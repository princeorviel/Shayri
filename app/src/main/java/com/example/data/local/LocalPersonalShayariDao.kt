package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LocalPersonalShayariEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalPersonalShayariDao {
    @Query("SELECT * FROM local_personal_shayari ORDER BY updatedAtMillis DESC, createdAtMillis DESC")
    fun observeAll(): Flow<List<LocalPersonalShayariEntity>>

    @Query("SELECT * FROM local_personal_shayari ORDER BY createdAtMillis ASC")
    suspend fun getAllOnce(): List<LocalPersonalShayariEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LocalPersonalShayariEntity)

    @Query("UPDATE local_personal_shayari SET title = :title, themeTag = :themeTag, couplet = :couplet, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateById(
        id: String,
        title: String,
        themeTag: String,
        couplet: String,
        updatedAtMillis: Long
    ): Int

    @Query("DELETE FROM local_personal_shayari WHERE id = :id")
    suspend fun deleteById(id: String)
}
