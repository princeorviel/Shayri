package com.example.data.local

import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ShayariRepository(private val dao: ShayariDao) {
    private val seedMutex = Mutex()
    @Volatile
    private var isCatalogVerified = false

    val allShayari: Flow<List<ShayariEntity>> = dao.observeAllShayari()
    val favoriteShayari: Flow<List<ShayariEntity>> = dao.observeFavoriteShayari()

    suspend fun ensureSeeded() {
        if (isCatalogVerified) return
        seedMutex.withLock {
            if (isCatalogVerified) return
            withContext(Dispatchers.IO) {
                val existingSeedCount = dao.getSeedCount()
                if (existingSeedCount < ShayariSeedData.EXPECTED_CATALOG_SIZE) {
                    val fullCatalog = ShayariSeedData.allShayari
                    if (existingSeedCount < fullCatalog.size) {
                        dao.insertAllChunked(fullCatalog)
                    }
                }
                val sample = ShayariSeedData.boldCategorySample()
                if (dao.countMatchingCouplet(sample.id, sample.couplet) == 0) {
                    dao.syncSeedContent(ShayariSeedData.boldCategoryItems())
                }
                if (dao.countLegacyPoetNames() > 0) {
                    dao.normalizeLegacyPoetNames()
                }
                isCatalogVerified = true
            }
        }
    }

    suspend fun toggleFavorite(id: Int, currentlyFavorite: Boolean) {
        dao.updateFavorite(id = id, isFavorite = !currentlyFavorite)
    }

    suspend fun upsertUserPostToCatalog(
        personalId: String,
        couplet: String,
        authorName: String,
        title: String = "",
        themeTag: String = "",
        primaryCategoryId: String? = null
    ): ShayariEntity = withContext(Dispatchers.IO) {
        val cleanCouplet = couplet.trim().take(600)
        val cleanAuthor = authorName.trim().ifBlank { "शायर दोस्त" }.take(40)
        val cleanTag = themeTag.trim()
        val cleanTitle = title.trim()

        val matchedCategory = com.example.data.model.ShayariTagMatcher.resolvePrimaryCategory(
            rawTag = cleanTag,
            explicitCategoryId = primaryCategoryId,
            fallback = ShayariCategory.ZINDAGI
        )

        val parsedTags = com.example.data.model.ShayariTagMatcher.parseTags(cleanTag)
        val formattedTags = com.example.data.model.ShayariTagMatcher.formatTagsStorage(
            tags = if (parsedTags.isEmpty()) listOf(matchedCategory.titleHi) else parsedTags,
            maxLen = 110
        )

        val displayTag = when {
            cleanTitle.isNotBlank() && formattedTags.isNotBlank() -> "$cleanTitle • $formattedTags"
            formattedTags.isNotBlank() -> formattedTags
            cleanTitle.isNotBlank() -> cleanTitle
            else -> matchedCategory.titleHi
        }

        val entity = ShayariEntity(
            id = userPostCatalogId(personalId),
            couplet = cleanCouplet,
            poet = cleanAuthor,
            categoryId = matchedCategory.id,
            moodTag = displayTag,
            isFavorite = false,
            isFeatured = false
        )
        dao.upsertUserPost(entity)
        entity
    }

    suspend fun removeUserPostFromCatalog(personalId: String) = withContext(Dispatchers.IO) {
        if (personalId.isBlank()) return@withContext
        dao.deleteById(userPostCatalogId(personalId))
    }

    companion object {
        const val USER_POST_ID_BASE = 1_000_000

        fun userPostCatalogId(personalId: String): Int {
            val positiveHash = (personalId.hashCode() and 0x3FFFFFFF) % 900_000_000
            return USER_POST_ID_BASE + positiveHash
        }
    }
}
