package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class CommunityReel(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val caption: String = "",
    val tags: List<String> = emptyList(),
    val mediaType: String = "text",
    val backgroundPreset: String = "",
    val thumbnailBase64: String = "",
    val voicePersonaId: String = "",
    val bgmTrackId: String = "",
    val singerName: String = "",
    val songTitle: String = "",
    val likeCount: Int = 0,
    val status: String = "published",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val createdAtMillis: Long
        get() = createdAt?.toDate()?.time ?: 0L
}
