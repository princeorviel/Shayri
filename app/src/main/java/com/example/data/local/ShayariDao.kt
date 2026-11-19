package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.ShayariEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShayariDao {
    @Query("SELECT * FROM shayari ORDER BY id ASC")
    fun observeAllShayari(): Flow<List<ShayariEntity>>

    @Query("SELECT * FROM shayari WHERE isFavorite = 1 ORDER BY id ASC")
    fun observeFavoriteShayari(): Flow<List<ShayariEntity>>

    @Query("SELECT COUNT(*) FROM shayari")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM shayari WHERE id = :id AND couplet = :couplet")
    suspend fun countMatchingCouplet(id: Int, couplet: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ShayariEntity>)

    @Transaction
    suspend fun insertAllChunked(items: List<ShayariEntity>, chunkSize: Int = 1000) {
        items.chunked(chunkSize).forEach { batch ->
            insertAll(batch)
        }
    }

    @Query("UPDATE shayari SET couplet = :couplet, poet = :poet, categoryId = :categoryId, moodTag = :moodTag WHERE id = :id")
    suspend fun updateSeedContent(id: Int, couplet: String, poet: String, categoryId: String, moodTag: String)

    @Query("SELECT COUNT(*) FROM shayari WHERE poet IN ('लोकप्रिय शायरी', 'अज्ञात शायर')")
    suspend fun countLegacyPoetNames(): Int

    @Query("UPDATE shayari SET poet = '_ शायरी' WHERE poet IN ('लोकप्रिय शायरी', 'अज्ञात शायर')")
    suspend fun normalizeLegacyPoetNames(): Int

    @Transaction
    suspend fun syncSeedContent(items: List<ShayariEntity>) {
        items.forEach { item ->
            updateSeedContent(item.id, item.couplet, item.poet, item.categoryId, item.moodTag)
        }
    }

    @Query("UPDATE shayari SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)
}
