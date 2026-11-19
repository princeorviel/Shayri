package com.example.data.model

import com.google.firebase.Timestamp

enum class PostNotificationType(val value: String) {
    LIKE("like"),
    COMMENT("comment"),
    SHARE("share"),
    FOLLOW("follow");

    companion object {
        fun fromValue(raw: String?): PostNotificationType {
            return entries.firstOrNull { it.value == raw } ?: LIKE
        }
    }
}

data class PostNotification(
    val id: String = "",
    val recipientUserId: String = "",
    val actorUserId: String = "",
    val actorName: String = "",
    val type: String = PostNotificationType.LIKE.value,
    val postId: String = "",
    val postExcerpt: String = "",
    val commentText: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val notificationType: PostNotificationType
        get() = PostNotificationType.fromValue(type)
}
