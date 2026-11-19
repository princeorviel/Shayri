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
        val cleanTitle = title.trim().take(80)
        val cleanThemeTag = themeTag.trim().take(40)
        val cleanCouplet = couplet.trim().take(600)

        require(cleanTitle.isNotEmpty() || cleanThemeTag.isNotEmpty()) {
            "Title or theme tag is required."
        }
        require(cleanCouplet.length >= 10) {
            "Couplet must be at least 10 characters."
        }

        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        return runCatching {
            dao.upsert(
                LocalPersonalShayariEntity(
                    id = id,
                    title = cleanTitle,
                    themeTag = cleanThemeTag,
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
        require(shayariId.isNotBlank()) { "Shayari ID must not be blank for update." }
        val cleanTitle = title.trim().take(80)
        val cleanThemeTag = themeTag.trim().take(40)
        val cleanCouplet = couplet.trim().take(600)

        require(cleanTitle.isNotEmpty() || cleanThemeTag.isNotEmpty()) {
            "Title or theme tag is required."
        }
        require(cleanCouplet.length >= 10) {
            "Couplet must be at least 10 characters."
        }

        val now = System.currentTimeMillis()
        return runCatching {
            val updatedRows = dao.updateById(
                id = shayariId,
                title = cleanTitle,
                themeTag = cleanThemeTag,
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
        val localItems = dao.getAllOnce()
        var migrated = 0
        for (item in localItems) {
            val result = cloudStore.savePersonalShayari(
                title = item.title,
                themeTag = item.themeTag,
                couplet = item.couplet
            )
            if (result.isSuccess) {
                dao.deleteById(item.id)
                migrated++
            }
        }
        return migrated
    }
}
