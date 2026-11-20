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
        createdAt = safeTimestampFromMillis(createdAtMillis),
        updatedAt = safeTimestampFromMillis(updatedAtMillis)
    )
}

private fun safeTimestampFromMillis(millis: Long): Timestamp {
    val safeMillis = millis.coerceAtLeast(0L)
    val seconds = safeMillis / 1000L
    val nanos = ((safeMillis % 1000L) * 1_000_000L).toInt().coerceIn(0, 999_999_999)
    return Timestamp(seconds, nanos)
}

const val LOCAL_GUEST_USER_ID = "guest_local"
