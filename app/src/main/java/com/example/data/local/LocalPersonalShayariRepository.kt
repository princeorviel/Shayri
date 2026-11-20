package com.example.data.local

import com.example.data.model.LOCAL_GUEST_USER_ID
import com.example.data.model.LocalPersonalShayariEntity
import com.example.data.model.PersonalShayari
import com.example.data.model.toPersonalShayari
import com.example.data.remote.PersonalShayariStore
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalPersonalShayariRepository(
    private val dao: LocalPersonalShayariDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PersonalShayariStore {

    override fun observePersonalShayari(userId: String): Flow<List<PersonalShayari>> {
        return dao.observeAll()
            .map { entities ->
                entities.map { it.toPersonalShayari(userId = LOCAL_GUEST_USER_ID) }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun savePersonalShayari(
        title: String,
        themeTag: String,
        couplet: String
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val cleanTitle = title.trim().take(80)
            val rawThemeTag = themeTag.trim().take(40)
            val effectiveThemeTag = if (cleanTitle.isEmpty() && rawThemeTag.isEmpty()) {
                DEFAULT_THEME_TAG
            } else {
                rawThemeTag
            }
            val cleanCouplet = couplet.trim().take(600)

            require(cleanCouplet.length >= MIN_LOCAL_COUPLET_LENGTH) {
                "कृपया कम से कम $MIN_LOCAL_COUPLET_LENGTH अक्षर लिखें।"
            }

            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            dao.upsert(
                LocalPersonalShayariEntity(
                    id = id,
                    title = cleanTitle,
                    themeTag = effectiveThemeTag,
                    couplet = cleanCouplet,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePersonalShayari(
        shayariId: String,
        title: String,
        themeTag: String,
        couplet: String
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            require(shayariId.isNotBlank()) { "शायरी अपडेट करने के लिए सही ID नहीं मिली।" }
            val cleanTitle = title.trim().take(80)
            val rawThemeTag = themeTag.trim().take(40)
            val effectiveThemeTag = if (cleanTitle.isEmpty() && rawThemeTag.isEmpty()) {
                DEFAULT_THEME_TAG
            } else {
                rawThemeTag
            }
            val cleanCouplet = couplet.trim().take(600)

            require(cleanCouplet.length >= MIN_LOCAL_COUPLET_LENGTH) {
                "कृपया कम से कम $MIN_LOCAL_COUPLET_LENGTH अक्षर लिखें।"
            }

            val now = System.currentTimeMillis()
            val updatedRows = dao.updateById(
                id = shayariId,
                title = cleanTitle,
                themeTag = effectiveThemeTag,
                couplet = cleanCouplet,
                updatedAtMillis = now
            )
            if (updatedRows == 0) {
                throw NoSuchElementException("शायरी नहीं मिली")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePersonalShayari(shayariId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                dao.deleteById(shayariId)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun migrateAllToCloud(cloudStore: PersonalShayariStore): Int =
        migrateAllToCloudReturningItems(cloudStore).size

    suspend fun migrateAllToCloudReturningItems(
        cloudStore: PersonalShayariStore
    ): List<LocalPersonalShayariEntity> =
        withContext(ioDispatcher) {
            val localItems = try {
                dao.getAllOnce()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            val migrated = mutableListOf<LocalPersonalShayariEntity>()
            for (item in localItems) {
                val result = try {
                    cloudStore.savePersonalShayari(
                        title = item.title,
                        themeTag = item.themeTag.ifBlank { DEFAULT_THEME_TAG },
                        couplet = item.couplet
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
                if (result.isSuccess) {
                    try {
                        dao.deleteById(item.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Keep going if local cleanup fails
                    }
                    migrated.add(item)
                }
            }
            migrated
        }

    companion object {
        const val DEFAULT_THEME_TAG = "मेरी शायरी"
        const val MIN_LOCAL_COUPLET_LENGTH = 4
    }
}
