package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CommunityShayari(
    @DocumentId val id: String = "",
    val userId: String = "",
    val penName: String = "",
    val couplet: String = "",
    val categoryId: String = ShayariCategory.MOHABBAT.id,
    val promptTitle: String = "",
    val status: String = "published",
    val wahCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun CommunityShayari.toShayariEntity(): ShayariEntity {
    return ShayariEntity(
        id = id.hashCode(),
        couplet = couplet,
        poet = penName,
        categoryId = categoryId,
        moodTag = if (promptTitle.isNotBlank()) promptTitle else "कवि मंच",
        isFavorite = false,
        isFeatured = false
    )
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val penName: String = "",
    val bio: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PoetProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val penName: String = "",
    val bio: String = "",
    @field:JvmField val isPublic: Boolean = true,
    val followerCount: Long = 0L,
    val followingCount: Long = 0L,
    val postCount: Long = 0L,
    val facebookUrl: String = "",
    val instagramUrl: String = "",
    val whatsappUrl: String = "",
    val twitterUrl: String = "",
    val telegramUrl: String = "",
    val youtubeUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toSocialLinks(): SocialLinks = SocialLinks(
        facebookUrl = facebookUrl,
        instagramUrl = instagramUrl,
        whatsappUrl = whatsappUrl,
        twitterUrl = twitterUrl,
        telegramUrl = telegramUrl,
        youtubeUrl = youtubeUrl
    )
}

@Keep
@IgnoreExtraProperties
data class PoetFollower(
    @DocumentId val id: String = "",
    val followerUserId: String = "",
    val followerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class WritingPrompt(
    val title: String,
    val description: String,
    val suggestedCategoryId: String,
    val starters: List<String>
)

object WritingPromptsCatalog {
    val prompts: List<WritingPrompt> = listOf(
        WritingPrompt(
            title = "बारिश और यादें",
            description = "पहली बारिश की ख़ुशबू, भीगी सड़कें या किसी की याद — अपने लफ़्ज़ों में ढालें।",
            suggestedCategoryId = ShayariCategory.PRAKRITI.id,
            starters = listOf(
                "बारिश की इन बूँदों ने आज फिर,\n",
                "मिट्टी की ख़ुशबू में बसी है तेरी याद,\n",
                "बादल बरसे तो दिल ने कहा चुपके से,\n"
            )
        ),
        WritingPrompt(
            title = "हौसले की उड़ान",
            description = "मुश्किलों से लड़कर आगे बढ़ने का जज़्बा 2 से 4 पंक्तियों में लिखें।",
            suggestedCategoryId = ShayariCategory.HAUSLA.id,
            starters = listOf(
                "आसमाँ चाहे जितना ऊँचा सही,\n",
                "गिरकर संभलने का हुनर सीखा है हमने,\n",
                "अँधेरी रात भी ढल जाएगी एक दिन,\n"
            )
        ),
        WritingPrompt(
            title = "यारों की महफ़िल",
            description = "चाय की टपरी, पुरानी हँसी और सच्ची दोस्ती के नाम एक शेर लिखें।",
            suggestedCategoryId = ShayariCategory.DOSTI.id,
            starters = listOf(
                "वक़्त बदला मगर यारों की महफ़िल वही रही,\n",
                "एक सच्चा दोस्त जब साथ खड़ा होता है,\n",
                "बचपन की वो हँसी आज भी ज़िंदा है,\n"
            )
        ),
        WritingPrompt(
            title = "ख़ामोश प्यार",
            description = "दिल की अनकही बातें या मोहब्बत का सबसे कोमल एहसास शब्दों में पिरोएँ।",
            suggestedCategoryId = ShayariCategory.MOHABBAT.id,
            starters = listOf(
                "लब ख़ामोश रहे और आँखें सब कह गईं,\n",
                "तेरा नाम जब भी दिल पर दस्तक देता है,\n",
                "इश्क़ लफ़्ज़ों का मोहताज नहीं होता,\n"
            )
        ),
        WritingPrompt(
            title = "चाँदनी रात और सुकून",
            description = "ठंडी हवा, चाँद की रोशनी और मन की शांति पर कुछ नया रचें।",
            suggestedCategoryId = ShayariCategory.WAQT.id,
            starters = listOf(
                "शोर की दुनिया से दूर एक कोना मिला,\n",
                "चाँद ने खिड़की पर आकर पूछा आज,\n",
                "वक़्त के साथ समझ आया सुकून कहाँ है,\n"
            )
        )
    )
}
