package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PersonalShayari(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val themeTag: String = "",
    val couplet: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun PersonalShayari.toShayariEntity(authorName: String = ""): ShayariEntity {
    val matchedCategory = ShayariCategory.entries.firstOrNull {
        it.titleHi.equals(themeTag.trim(), ignoreCase = true) ||
            it.id.equals(themeTag.trim(), ignoreCase = true)
    } ?: ShayariCategory.ZINDAGI

    val displayTag = when {
        title.isNotBlank() && themeTag.isNotBlank() -> "$title • $themeTag"
        title.isNotBlank() -> title
        themeTag.isNotBlank() -> themeTag
        else -> "मेरी शायरी"
    }

    val cleanAuthor = authorName.trim()
    return ShayariEntity(
        id = ("personal_$id").hashCode(),
        couplet = couplet,
        poet = cleanAuthor.ifBlank { title.trim() },
        categoryId = matchedCategory.id,
        moodTag = displayTag,
        isFavorite = false,
        isFeatured = false
    )
}
