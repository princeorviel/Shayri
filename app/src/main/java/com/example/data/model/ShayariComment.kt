package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ShayariComment(
    @DocumentId val id: String = "",
    val shayariKey: String = "",
    val userId: String = "",
    val authorName: String = "",
    val text: String = "",
    val status: String = "published",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun shayariCommentKey(shayariId: Int): String = "seed_$shayariId"
