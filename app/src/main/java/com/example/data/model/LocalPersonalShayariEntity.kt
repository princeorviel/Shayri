package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Entity(tableName = "local_personal_shayari")
data class LocalPersonalShayariEntity(
    @PrimaryKey val id: String,
    val title: String,
    val themeTag: String,
    val couplet: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

fun LocalPersonalShayariEntity.toPersonalShayari(userId: String = LOCAL_GUEST_USER_ID): PersonalShayari {
    return PersonalShayari(
        id = id,
        userId = userId,
        title = title,
        themeTag = themeTag,
        couplet = couplet,
        createdAt = Timestamp(createdAtMillis / 1000L, ((createdAtMillis % 1000L) * 1_000_000L).toInt()),
        updatedAt = Timestamp(updatedAtMillis / 1000L, ((updatedAtMillis % 1000L) * 1_000_000L).toInt())
    )
}

const val LOCAL_GUEST_USER_ID = "guest_local"
