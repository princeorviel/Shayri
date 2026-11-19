package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shayari")
data class ShayariEntity(
    @PrimaryKey val id: Int,
    val couplet: String,
    val poet: String,
    val categoryId: String,
    val moodTag: String,
    val isFavorite: Boolean = false,
    val isFeatured: Boolean = false
)
