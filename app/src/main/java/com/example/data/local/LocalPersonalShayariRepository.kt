package com.example.data.local

import com.example.data.model.LOCAL_GUEST_USER_ID
import com.example.data.model.LocalPersonalShayariEntity
import com.example.data.model.PersonalShayari
import com.example.data.model.toPersonalShayari
import com.example.data.remote.PersonalShayariStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalPersonalShayariRepository(
    private val dao: LocalPersonalShayariDao
) : PersonalShayariStore {

    override fun observePersonalShayari(userId: String): Flow<List<PersonalShayari>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toPersonalShayari(userId = LOCAL_GUEST_USER_ID) }
        }
    }

    override suspend fun savePersonalShayari(
        title: String,
        themeTag: String,
        couplet: String
    ): Result<String> {
        return runCatching {
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
            id
        }
    }

    override suspend fun updatePersonalShayari(
        shayariId: String,
        title: String,
        themeTag: String,
        couplet: String
    ): Result<Unit> {
        return runCatching {
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
        }
    }

    override suspend fun deletePersonalShayari(shayariId: String): Result<Unit> {
        return runCatching {
            dao.deleteById(shayariId)
        }
    }

    suspend fun migrateAllToCloud(cloudStore: PersonalShayariStore): Int {
        val localItems = runCatching { dao.getAllOnce() }.getOrDefault(emptyList())
        var migrated = 0
        for (item in localItems) {
            val result = runCatching {
                cloudStore.savePersonalShayari(
                    title = item.title,
                    themeTag = item.themeTag.ifBlank { DEFAULT_THEME_TAG },
                    couplet = item.couplet
                )
            }.getOrElse { Result.failure(it) }
            if (result.isSuccess) {
                runCatching { dao.deleteById(item.id) }
                migrated++
            }
        }
        return migrated
    }

    companion object {
        const val DEFAULT_THEME_TAG = "मेरी शायरी"
        const val MIN_LOCAL_COUPLET_LENGTH = 4
    }
}
