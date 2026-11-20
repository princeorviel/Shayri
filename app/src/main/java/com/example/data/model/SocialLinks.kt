package com.example.data.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.security.MessageDigest

enum class SocialLinkPlatform(
    val id: String,
    val label: String,
    val badgeText: String,
    val brandColor: Color,
    val hint: String
) {
    FACEBOOK(
        id = "facebook",
        label = "Facebook",
        badgeText = "f",
        brandColor = Color(0xFF1877F2),
        hint = "username या facebook.com/..."
    ),
    INSTAGRAM(
        id = "instagram",
        label = "Instagram",
        badgeText = "IG",
        brandColor = Color(0xFFE1306C),
        hint = "@username या instagram.com/..."
    ),
    WHATSAPP(
        id = "whatsapp",
        label = "WhatsApp",
        badgeText = "WA",
        brandColor = Color(0xFF25D366),
        hint = "मोबाइल नंबर या wa.me/..."
    ),
    TWITTER(
        id = "twitter",
        label = "X (Twitter)",
        badgeText = "X",
        brandColor = Color(0xFF0F1419),
        hint = "@handle या x.com/..."
    ),
    TELEGRAM(
        id = "telegram",
        label = "Telegram",
        badgeText = "TG",
        brandColor = Color(0xFF229ED9),
        hint = "@username या t.me/..."
    ),
    YOUTUBE(
        id = "youtube",
        label = "YouTube",
        badgeText = "YT",
        brandColor = Color(0xFFFF0000),
        hint = "@channel या youtube.com/..."
    );

    fun normalizeUrl(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed.take(160)
        }
        val cleanHandle = trimmed.removePrefix("@").trim()
        return when (this) {
            FACEBOOK -> {
                if (cleanHandle.contains("facebook.com", ignoreCase = true) ||
                    cleanHandle.contains("fb.com", ignoreCase = true)
                ) {
                    "https://$cleanHandle"
                } else {
                    "https://facebook.com/$cleanHandle"
                }
            }
            INSTAGRAM -> {
                if (cleanHandle.contains("instagram.com", ignoreCase = true)) {
                    "https://$cleanHandle"
                } else {
                    "https://instagram.com/$cleanHandle"
                }
            }
            WHATSAPP -> {
                if (cleanHandle.contains("wa.me", ignoreCase = true) ||
                    cleanHandle.contains("whatsapp.com", ignoreCase = true)
                ) {
                    "https://$cleanHandle"
                } else {
                    val digits = cleanHandle.filter { it.isDigit() }
                    when {
                        digits.length == 10 -> "https://wa.me/91$digits"
                        digits.length in 11..15 -> "https://wa.me/$digits"
                        else -> "https://wa.me/$cleanHandle"
                    }
                }
            }
            TWITTER -> {
                if (cleanHandle.contains("x.com", ignoreCase = true) ||
                    cleanHandle.contains("twitter.com", ignoreCase = true)
                ) {
                    "https://$cleanHandle"
                } else {
                    "https://x.com/$cleanHandle"
                }
            }
            TELEGRAM -> {
                if (cleanHandle.contains("t.me", ignoreCase = true) ||
                    cleanHandle.contains("telegram.me", ignoreCase = true)
                ) {
                    "https://$cleanHandle"
                } else {
                    "https://t.me/$cleanHandle"
                }
            }
            YOUTUBE -> {
                if (cleanHandle.contains("youtube.com", ignoreCase = true) ||
                    cleanHandle.contains("youtu.be", ignoreCase = true)
                ) {
                    "https://$cleanHandle"
                } else {
                    "https://youtube.com/@$cleanHandle"
                }
            }
        }.take(160)
    }
}

data class SocialLinks(
    val facebookUrl: String = "",
    val instagramUrl: String = "",
    val whatsappUrl: String = "",
    val twitterUrl: String = "",
    val telegramUrl: String = "",
    val youtubeUrl: String = ""
) {
    fun valueFor(platform: SocialLinkPlatform): String = when (platform) {
        SocialLinkPlatform.FACEBOOK -> facebookUrl
        SocialLinkPlatform.INSTAGRAM -> instagramUrl
        SocialLinkPlatform.WHATSAPP -> whatsappUrl
        SocialLinkPlatform.TWITTER -> twitterUrl
        SocialLinkPlatform.TELEGRAM -> telegramUrl
        SocialLinkPlatform.YOUTUBE -> youtubeUrl
    }

    fun activeEntries(): List<Pair<SocialLinkPlatform, String>> {
        return SocialLinkPlatform.entries.mapNotNull { platform ->
            val value = valueFor(platform).trim()
            if (value.isNotBlank()) platform to value else null
        }
    }

    fun hasAny(): Boolean = activeEntries().isNotEmpty()

    fun normalized(): SocialLinks = SocialLinks(
        facebookUrl = SocialLinkPlatform.FACEBOOK.normalizeUrl(facebookUrl),
        instagramUrl = SocialLinkPlatform.INSTAGRAM.normalizeUrl(instagramUrl),
        whatsappUrl = SocialLinkPlatform.WHATSAPP.normalizeUrl(whatsappUrl),
        twitterUrl = SocialLinkPlatform.TWITTER.normalizeUrl(twitterUrl),
        telegramUrl = SocialLinkPlatform.TELEGRAM.normalizeUrl(telegramUrl),
        youtubeUrl = SocialLinkPlatform.YOUTUBE.normalizeUrl(youtubeUrl)
    )
}

data class LocalShayarAccount(
    val isRegistered: Boolean = false,
    val isLoggedIn: Boolean = false,
    val fullName: String = "",
    val username: String = "",
    val contact: String = "",
    val bio: String = "",
    val passwordHash: String = "",
    val socialLinks: SocialLinks = SocialLinks(),
    val profilePhotoUri: String = ""
) {
    val handleDisplay: String
        get() = username.trim().removePrefix("@").takeIf { it.isNotBlank() }?.let { "@$it" }.orEmpty()
}

fun hashLocalPassword(rawPassword: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(rawPassword.trim().toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

fun openSocialProfileUrl(
    context: Context,
    platform: SocialLinkPlatform,
    rawValue: String
): Boolean {
    val normalized = platform.normalizeUrl(rawValue)
    if (normalized.isBlank()) return false
    return runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
