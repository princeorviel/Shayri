package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CardStyle
import com.example.data.model.FontScaleOption
import com.example.data.model.LocalShayarAccount
import com.example.data.model.ShayariFontStyle
import com.example.data.model.SocialLinks
import com.example.data.model.ThemeModeOption
import com.example.data.model.hashLocalPassword
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.shayariDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shayari_preferences"
)

data class UserPreferences(
    val themeMode: ThemeModeOption = ThemeModeOption.LIGHT,
    val fontScale: FontScaleOption = FontScaleOption.MEDIUM,
    val fontStyle: ShayariFontStyle = ShayariFontStyle.CLASSIC_SERIF,
    val cardStyle: CardStyle = CardStyle.NIGHT_MUSHAIRA,
    val dailyNotificationEnabled: Boolean = false,
    val dailyNotificationHour: Int = 9,
    val dailyNotificationMinute: Int = 0,
    val authorPenName: String = "",
    val localAccount: LocalShayarAccount = LocalShayarAccount()
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val CARD_STYLE = stringPreferencesKey("card_style")
        val DAILY_NOTIF_ENABLED = booleanPreferencesKey("daily_notif_enabled")
        val DAILY_NOTIF_HOUR = intPreferencesKey("daily_notif_hour")
        val DAILY_NOTIF_MINUTE = intPreferencesKey("daily_notif_minute")
        val AUTHOR_PEN_NAME = stringPreferencesKey("author_pen_name")

        val LOCAL_ACCOUNT_REGISTERED = booleanPreferencesKey("local_account_registered")
        val LOCAL_ACCOUNT_LOGGED_IN = booleanPreferencesKey("local_account_logged_in")
        val LOCAL_ACCOUNT_FULL_NAME = stringPreferencesKey("local_account_full_name")
        val LOCAL_ACCOUNT_USERNAME = stringPreferencesKey("local_account_username")
        val LOCAL_ACCOUNT_CONTACT = stringPreferencesKey("local_account_contact")
        val LOCAL_ACCOUNT_BIO = stringPreferencesKey("local_account_bio")
        val LOCAL_ACCOUNT_PASSWORD_HASH = stringPreferencesKey("local_account_password_hash")
        val SOCIAL_FACEBOOK = stringPreferencesKey("social_facebook")
        val SOCIAL_INSTAGRAM = stringPreferencesKey("social_instagram")
        val SOCIAL_WHATSAPP = stringPreferencesKey("social_whatsapp")
        val SOCIAL_TWITTER = stringPreferencesKey("social_twitter")
        val SOCIAL_TELEGRAM = stringPreferencesKey("social_telegram")
        val SOCIAL_YOUTUBE = stringPreferencesKey("social_youtube")
    }

    val preferencesFlow: Flow<UserPreferences> = context.shayariDataStore.data.map { prefs ->
        val socialLinks = SocialLinks(
            facebookUrl = prefs[Keys.SOCIAL_FACEBOOK] ?: "",
            instagramUrl = prefs[Keys.SOCIAL_INSTAGRAM] ?: "",
            whatsappUrl = prefs[Keys.SOCIAL_WHATSAPP] ?: "",
            twitterUrl = prefs[Keys.SOCIAL_TWITTER] ?: "",
            telegramUrl = prefs[Keys.SOCIAL_TELEGRAM] ?: "",
            youtubeUrl = prefs[Keys.SOCIAL_YOUTUBE] ?: ""
        )
        val localAccount = LocalShayarAccount(
            isRegistered = prefs[Keys.LOCAL_ACCOUNT_REGISTERED] ?: false,
            isLoggedIn = prefs[Keys.LOCAL_ACCOUNT_LOGGED_IN] ?: false,
            fullName = prefs[Keys.LOCAL_ACCOUNT_FULL_NAME] ?: "",
            username = prefs[Keys.LOCAL_ACCOUNT_USERNAME] ?: "",
            contact = prefs[Keys.LOCAL_ACCOUNT_CONTACT] ?: "",
            bio = prefs[Keys.LOCAL_ACCOUNT_BIO] ?: "",
            passwordHash = prefs[Keys.LOCAL_ACCOUNT_PASSWORD_HASH] ?: "",
            socialLinks = socialLinks
        )
        UserPreferences(
            themeMode = ThemeModeOption.fromId(prefs[Keys.THEME_MODE] ?: ThemeModeOption.LIGHT.id),
            fontScale = FontScaleOption.fromId(prefs[Keys.FONT_SCALE] ?: FontScaleOption.MEDIUM.id),
            fontStyle = ShayariFontStyle.fromId(
                prefs[Keys.FONT_STYLE] ?: ShayariFontStyle.CLASSIC_SERIF.id
            ),
            cardStyle = CardStyle.fromId(prefs[Keys.CARD_STYLE] ?: CardStyle.NIGHT_MUSHAIRA.id),
            dailyNotificationEnabled = prefs[Keys.DAILY_NOTIF_ENABLED] ?: false,
            dailyNotificationHour = (prefs[Keys.DAILY_NOTIF_HOUR] ?: 9).coerceIn(0, 23),
            dailyNotificationMinute = (prefs[Keys.DAILY_NOTIF_MINUTE] ?: 0).coerceIn(0, 59),
            authorPenName = prefs[Keys.AUTHOR_PEN_NAME] ?: "",
            localAccount = localAccount
        )
    }

    suspend fun setThemeMode(mode: ThemeModeOption) {
        context.shayariDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.id
        }
    }

    suspend fun setFontScale(scale: FontScaleOption) {
        context.shayariDataStore.edit { prefs ->
            prefs[Keys.FONT_SCALE] = scale.id
        }
    }

    suspend fun setFontStyle(style: ShayariFontStyle) {
        context.shayariDataStore.edit { prefs ->
            prefs[Keys.FONT_STYLE] = style.id
        }
    }

    suspend fun setCardStyle(style: CardStyle) {
        context.shayariDataStore.edit { prefs ->
            prefs[Keys.CARD_STYLE] = style.id
        }
    }

    suspend fun setDailyNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        context.shayariDataStore.edit { prefs ->
            prefs[Keys.DAILY_NOTIF_ENABLED] = enabled
            prefs[Keys.DAILY_NOTIF_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.DAILY_NOTIF_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setAuthorPenName(name: String) {
        context.shayariDataStore.edit { prefs ->
            prefs[Keys.AUTHOR_PEN_NAME] = name.trim().take(50)
        }
    }

    suspend fun registerLocalAccount(
        fullName: String,
        username: String,
        contact: String,
        password: String,
        bio: String,
        socialLinks: SocialLinks
    ) {
        val cleanName = fullName.trim().take(40)
        val cleanUsername = username.trim().removePrefix("@").replace("\\s+".toRegex(), "").take(30)
        val cleanContact = contact.trim().take(80)
        val cleanBio = bio.trim().take(160)
        val normalizedLinks = socialLinks.normalized()
        val passwordHash = hashLocalPassword(password)

        context.shayariDataStore.edit { prefs ->
            prefs[Keys.LOCAL_ACCOUNT_REGISTERED] = true
            prefs[Keys.LOCAL_ACCOUNT_LOGGED_IN] = true
            prefs[Keys.LOCAL_ACCOUNT_FULL_NAME] = cleanName
            prefs[Keys.LOCAL_ACCOUNT_USERNAME] = cleanUsername
            prefs[Keys.LOCAL_ACCOUNT_CONTACT] = cleanContact
            prefs[Keys.LOCAL_ACCOUNT_BIO] = cleanBio
            prefs[Keys.LOCAL_ACCOUNT_PASSWORD_HASH] = passwordHash
            prefs[Keys.AUTHOR_PEN_NAME] = cleanName
            prefs[Keys.SOCIAL_FACEBOOK] = normalizedLinks.facebookUrl
            prefs[Keys.SOCIAL_INSTAGRAM] = normalizedLinks.instagramUrl
            prefs[Keys.SOCIAL_WHATSAPP] = normalizedLinks.whatsappUrl
            prefs[Keys.SOCIAL_TWITTER] = normalizedLinks.twitterUrl
            prefs[Keys.SOCIAL_TELEGRAM] = normalizedLinks.telegramUrl
            prefs[Keys.SOCIAL_YOUTUBE] = normalizedLinks.youtubeUrl
        }
    }

    suspend fun loginLocalAccount(identifier: String, password: String): LoginLocalResult {
        val current = preferencesFlow.first().localAccount
        if (!current.isRegistered) {
            return LoginLocalResult.NoAccountRegistered
        }
        val cleanId = identifier.trim().removePrefix("@")
        val matchesIdentifier = cleanId.equals(current.username.removePrefix("@"), ignoreCase = true) ||
            (current.contact.isNotBlank() && cleanId.equals(current.contact, ignoreCase = true)) ||
            cleanId.equals(current.fullName, ignoreCase = true)
        val matchesPassword = hashLocalPassword(password) == current.passwordHash
        if (!matchesIdentifier || !matchesPassword) {
            return LoginLocalResult.InvalidCredentials
        }
        context.shayariDataStore.edit { prefs ->
            prefs[Keys.LOCAL_ACCOUNT_LOGGED_IN] = true
            if (current.fullName.isNotBlank()) {
                prefs[Keys.AUTHOR_PEN_NAME] = current.fullName
            }
        }
        return LoginLocalResult.Success
    }

    suspend fun logoutLocalAccount() {
        context.shayariDataStore.edit { prefs ->
            prefs[Keys.LOCAL_ACCOUNT_LOGGED_IN] = false
        }
    }

    suspend fun updateLocalProfileAndSocialLinks(
        penName: String,
        bio: String,
        socialLinks: SocialLinks
    ) {
        val cleanName = penName.trim().take(40)
        val cleanBio = bio.trim().take(160)
        val normalizedLinks = socialLinks.normalized()
        context.shayariDataStore.edit { prefs ->
            if (cleanName.isNotBlank()) {
                prefs[Keys.AUTHOR_PEN_NAME] = cleanName
                prefs[Keys.LOCAL_ACCOUNT_FULL_NAME] = cleanName
            }
            prefs[Keys.LOCAL_ACCOUNT_BIO] = cleanBio
            prefs[Keys.SOCIAL_FACEBOOK] = normalizedLinks.facebookUrl
            prefs[Keys.SOCIAL_INSTAGRAM] = normalizedLinks.instagramUrl
            prefs[Keys.SOCIAL_WHATSAPP] = normalizedLinks.whatsappUrl
            prefs[Keys.SOCIAL_TWITTER] = normalizedLinks.twitterUrl
            prefs[Keys.SOCIAL_TELEGRAM] = normalizedLinks.telegramUrl
            prefs[Keys.SOCIAL_YOUTUBE] = normalizedLinks.youtubeUrl
        }
    }
}

sealed interface LoginLocalResult {
    data object Success : LoginLocalResult
    data object NoAccountRegistered : LoginLocalResult
    data object InvalidCredentials : LoginLocalResult
}
