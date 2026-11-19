package com.example.data.local

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
                val existingCount = dao.getCount()
                if (existingCount < ShayariSeedData.EXPECTED_CATALOG_SIZE) {
                    val fullCatalog = ShayariSeedData.allShayari
                    if (existingCount < fullCatalog.size) {
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
}
