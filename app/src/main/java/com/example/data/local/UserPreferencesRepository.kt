package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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
    val localAccount: LocalShayarAccount = LocalShayarAccount(),
    val ownerStatsEnabled: Boolean = false
)

class UserPreferencesRepository(context: Context) {
    private val appContext: Context = context.applicationContext
    private val syncPrefs: SharedPreferences = appContext.getSharedPreferences(
        SYNC_PREFS_NAME,
        Context.MODE_PRIVATE
    )

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
        val OWNER_STATS_ENABLED = booleanPreferencesKey("owner_stats_enabled")
    }

    /**
     * Reads the synchronously committed mirror of preferences & login session so UI has the
     * user's real logged-in state immediately on Frame 0 before DataStore/Room finish loading.
     */
    fun readCachedPreferencesSync(): UserPreferences {
        val socialLinks = SocialLinks(
            facebookUrl = syncPrefs.getString(SYNC_SOCIAL_FACEBOOK, "").orEmpty(),
            instagramUrl = syncPrefs.getString(SYNC_SOCIAL_INSTAGRAM, "").orEmpty(),
            whatsappUrl = syncPrefs.getString(SYNC_SOCIAL_WHATSAPP, "").orEmpty(),
            twitterUrl = syncPrefs.getString(SYNC_SOCIAL_TWITTER, "").orEmpty(),
            telegramUrl = syncPrefs.getString(SYNC_SOCIAL_TELEGRAM, "").orEmpty(),
            youtubeUrl = syncPrefs.getString(SYNC_SOCIAL_YOUTUBE, "").orEmpty()
        )
        val localAccount = LocalShayarAccount(
            isRegistered = syncPrefs.getBoolean(SYNC_LOCAL_REGISTERED, false),
            isLoggedIn = syncPrefs.getBoolean(SYNC_LOCAL_LOGGED_IN, false),
            fullName = syncPrefs.getString(SYNC_LOCAL_FULL_NAME, "").orEmpty(),
            username = syncPrefs.getString(SYNC_LOCAL_USERNAME, "").orEmpty(),
            contact = syncPrefs.getString(SYNC_LOCAL_CONTACT, "").orEmpty(),
            bio = syncPrefs.getString(SYNC_LOCAL_BIO, "").orEmpty(),
            passwordHash = syncPrefs.getString(SYNC_LOCAL_PASSWORD_HASH, "").orEmpty(),
            socialLinks = socialLinks
        )
        return UserPreferences(
            themeMode = ThemeModeOption.fromId(
                syncPrefs.getString(SYNC_THEME_MODE, ThemeModeOption.LIGHT.id)
                    ?: ThemeModeOption.LIGHT.id
            ),
            fontScale = FontScaleOption.fromId(
                syncPrefs.getString(SYNC_FONT_SCALE, FontScaleOption.MEDIUM.id)
                    ?: FontScaleOption.MEDIUM.id
            ),
            fontStyle = ShayariFontStyle.fromId(
                syncPrefs.getString(SYNC_FONT_STYLE, ShayariFontStyle.CLASSIC_SERIF.id)
                    ?: ShayariFontStyle.CLASSIC_SERIF.id
            ),
            cardStyle = CardStyle.fromId(
                syncPrefs.getString(SYNC_CARD_STYLE, CardStyle.NIGHT_MUSHAIRA.id)
                    ?: CardStyle.NIGHT_MUSHAIRA.id
            ),
            dailyNotificationEnabled = syncPrefs.getBoolean(SYNC_DAILY_NOTIF_ENABLED, false),
            dailyNotificationHour = syncPrefs.getInt(SYNC_DAILY_NOTIF_HOUR, 9).coerceIn(0, 23),
            dailyNotificationMinute = syncPrefs.getInt(SYNC_DAILY_NOTIF_MINUTE, 0).coerceIn(0, 59),
            authorPenName = syncPrefs.getString(SYNC_AUTHOR_PEN_NAME, "").orEmpty(),
            localAccount = localAccount,
            ownerStatsEnabled = syncPrefs.getBoolean(SYNC_OWNER_STATS_ENABLED, false)
        )
    }

    private fun mirrorAccountToSyncPrefs(
        account: LocalShayarAccount,
        authorPenName: String,
        commitImmediately: Boolean = false
    ) {
        val editor = syncPrefs.edit()
            .putBoolean(SYNC_LOCAL_REGISTERED, account.isRegistered)
            .putBoolean(SYNC_LOCAL_LOGGED_IN, account.isLoggedIn)
            .putString(SYNC_LOCAL_FULL_NAME, account.fullName)
            .putString(SYNC_LOCAL_USERNAME, account.username)
            .putString(SYNC_LOCAL_CONTACT, account.contact)
            .putString(SYNC_LOCAL_BIO, account.bio)
            .putString(SYNC_LOCAL_PASSWORD_HASH, account.passwordHash)
            .putString(SYNC_AUTHOR_PEN_NAME, authorPenName)
            .putString(SYNC_SOCIAL_FACEBOOK, account.socialLinks.facebookUrl)
            .putString(SYNC_SOCIAL_INSTAGRAM, account.socialLinks.instagramUrl)
            .putString(SYNC_SOCIAL_WHATSAPP, account.socialLinks.whatsappUrl)
            .putString(SYNC_SOCIAL_TWITTER, account.socialLinks.twitterUrl)
            .putString(SYNC_SOCIAL_TELEGRAM, account.socialLinks.telegramUrl)
            .putString(SYNC_SOCIAL_YOUTUBE, account.socialLinks.youtubeUrl)
        if (commitImmediately) {
            editor.commit()
        } else {
            editor.apply()
        }
    }

    val preferencesFlow: Flow<UserPreferences> = appContext.shayariDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs ->
            val cached = readCachedPreferencesSync()
            val dsHasRegistered = prefs[Keys.LOCAL_ACCOUNT_REGISTERED] == true
            val dsHasLoggedInKey = prefs.contains(Keys.LOCAL_ACCOUNT_LOGGED_IN)

            val localAccount = if (dsHasRegistered) {
                val socialLinks = SocialLinks(
                    facebookUrl = prefs[Keys.SOCIAL_FACEBOOK] ?: cached.localAccount.socialLinks.facebookUrl,
                    instagramUrl = prefs[Keys.SOCIAL_INSTAGRAM] ?: cached.localAccount.socialLinks.instagramUrl,
                    whatsappUrl = prefs[Keys.SOCIAL_WHATSAPP] ?: cached.localAccount.socialLinks.whatsappUrl,
                    twitterUrl = prefs[Keys.SOCIAL_TWITTER] ?: cached.localAccount.socialLinks.twitterUrl,
                    telegramUrl = prefs[Keys.SOCIAL_TELEGRAM] ?: cached.localAccount.socialLinks.telegramUrl,
                    youtubeUrl = prefs[Keys.SOCIAL_YOUTUBE] ?: cached.localAccount.socialLinks.youtubeUrl
                )
                val resolved = LocalShayarAccount(
                    isRegistered = true,
                    isLoggedIn = if (dsHasLoggedInKey) {
                        prefs[Keys.LOCAL_ACCOUNT_LOGGED_IN] ?: false
                    } else {
                        cached.localAccount.isLoggedIn
                    },
                    fullName = prefs[Keys.LOCAL_ACCOUNT_FULL_NAME] ?: cached.localAccount.fullName,
                    username = prefs[Keys.LOCAL_ACCOUNT_USERNAME] ?: cached.localAccount.username,
                    contact = prefs[Keys.LOCAL_ACCOUNT_CONTACT] ?: cached.localAccount.contact,
                    bio = prefs[Keys.LOCAL_ACCOUNT_BIO] ?: cached.localAccount.bio,
                    passwordHash = prefs[Keys.LOCAL_ACCOUNT_PASSWORD_HASH] ?: cached.localAccount.passwordHash,
                    socialLinks = socialLinks
                )
                if (!cached.localAccount.isRegistered) {
                    mirrorAccountToSyncPrefs(
                        account = resolved,
                        authorPenName = prefs[Keys.AUTHOR_PEN_NAME] ?: resolved.fullName,
                        commitImmediately = false
                    )
                }
                resolved
            } else if (cached.localAccount.isRegistered) {
                cached.localAccount
            } else {
                LocalShayarAccount(
                    socialLinks = SocialLinks(
                        facebookUrl = prefs[Keys.SOCIAL_FACEBOOK] ?: "",
                        instagramUrl = prefs[Keys.SOCIAL_INSTAGRAM] ?: "",
                        whatsappUrl = prefs[Keys.SOCIAL_WHATSAPP] ?: "",
                        twitterUrl = prefs[Keys.SOCIAL_TWITTER] ?: "",
                        telegramUrl = prefs[Keys.SOCIAL_TELEGRAM] ?: "",
                        youtubeUrl = prefs[Keys.SOCIAL_YOUTUBE] ?: ""
                    )
                )
            }

            UserPreferences(
                themeMode = ThemeModeOption.fromId(
                    prefs[Keys.THEME_MODE] ?: cached.themeMode.id
                ),
                fontScale = FontScaleOption.fromId(
                    prefs[Keys.FONT_SCALE] ?: cached.fontScale.id
                ),
                fontStyle = ShayariFontStyle.fromId(
                    prefs[Keys.FONT_STYLE] ?: cached.fontStyle.id
                ),
                cardStyle = CardStyle.fromId(
                    prefs[Keys.CARD_STYLE] ?: cached.cardStyle.id
                ),
                dailyNotificationEnabled = prefs[Keys.DAILY_NOTIF_ENABLED]
                    ?: cached.dailyNotificationEnabled,
                dailyNotificationHour = (prefs[Keys.DAILY_NOTIF_HOUR] ?: cached.dailyNotificationHour)
                    .coerceIn(0, 23),
                dailyNotificationMinute = (prefs[Keys.DAILY_NOTIF_MINUTE] ?: cached.dailyNotificationMinute)
                    .coerceIn(0, 59),
                authorPenName = prefs[Keys.AUTHOR_PEN_NAME]
                    ?.takeIf { it.isNotBlank() }
                    ?: cached.authorPenName.ifBlank {
                        if (localAccount.isLoggedIn) localAccount.fullName else ""
                    },
                localAccount = localAccount,
                ownerStatsEnabled = prefs[Keys.OWNER_STATS_ENABLED] ?: cached.ownerStatsEnabled
            )
        }

    suspend fun setThemeMode(mode: ThemeModeOption) {
        withContext(Dispatchers.IO) {
            syncPrefs.edit().putString(SYNC_THEME_MODE, mode.id).apply()
        }
        appContext.shayariDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.id
        }
    }

    suspend fun setFontScale(scale: FontScaleOption) {
        withContext(Dispatchers.IO) {
            syncPrefs.edit().putString(SYNC_FONT_SCALE, scale.id).apply()
        }
        appContext.shayariDataStore.edit { prefs ->
            prefs[Keys.FONT_SCALE] = scale.id
        }
    }

    suspend fun setFontStyle(style: ShayariFontStyle) {
        withContext(Dispatchers.IO) {
            syncPrefs.edit().putString(SYNC_FONT_STYLE, style.id).apply()
        }
        appContext.shayariDataStore.edit { prefs ->
            prefs[Keys.FONT_STYLE] = style.id
        }
    }

    suspend fun setCardStyle(style: CardStyle) {
        withContext(Dispatchers.IO) {
            syncPrefs.edit().putString(SYNC_CARD_STYLE, style.id).apply()
        }
        appContext.shayariDataStore.edit { prefs ->
            prefs[Keys.CARD_STYLE] = style.id
        }
    }

    suspend fun setDailyNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        withContext(Dispatchers.IO) {
            syncPrefs.edit()
                .putBoolean(SYNC_DAILY_NOTIF_ENABLED, enabled)
                .putInt(SYNC_DAILY_NOTIF_HOUR, safeHour)
                .putInt(SYNC_DAILY_NOTIF_MINUTE, safeMinute)
                .apply()
        }
        appContext.shayariDataStore.edit { prefs ->
            prefs[Keys.DAILY_NOTIF_ENABLED] = enabled
            prefs[Keys.DAILY_NOTIF_HOUR] = safeHour
            prefs[Keys.DAILY_NOTIF_MINUTE] = safeMinute
        }
    }

    suspend fun setAuthorPenName(name: String) {
        val clean = name.trim().take(50)
        withContext(Dispatchers.IO) {
            syncPrefs.edit().putString(SYNC_AUTHOR_PEN_NAME, clean).apply()
        }
        appContext.shayariDataStore.edit { prefs ->
            prefs[Keys.AUTHOR_PEN_NAME] = clean
        }
    }

    suspend fun toggleOwnerStatsEnabled(): Boolean {
        var newValue = false
        appContext.shayariDataStore.edit { prefs ->
            val current = prefs[Keys.OWNER_STATS_ENABLED]
                ?: syncPrefs.getBoolean(SYNC_OWNER_STATS_ENABLED, false)
            newValue = !current
            prefs[Keys.OWNER_STATS_ENABLED] = newValue
        }
        withContext(Dispatchers.IO) {
            syncPrefs.edit().putBoolean(SYNC_OWNER_STATS_ENABLED, newValue).commit()
        }
        return newValue
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
        val rawUsername = username.trim().removePrefix("@").replace("\\s+".toRegex(), "")
        val derivedUsername = if (rawUsername.isNotBlank()) {
            rawUsername
        } else {
            cleanName.lowercase()
                .replace("[^a-z0-9_]".toRegex(), "")
                .ifBlank { "shayar" }
        }.take(30)
        val cleanContact = contact.trim().take(80)
        val cleanBio = bio.trim().take(160)
        val normalizedLinks = socialLinks.normalized()
        val passwordHash = hashLocalPassword(password)

        val account = LocalShayarAccount(
            isRegistered = true,
            isLoggedIn = true,
            fullName = cleanName,
            username = derivedUsername,
            contact = cleanContact,
            bio = cleanBio,
            passwordHash = passwordHash,
            socialLinks = normalizedLinks
        )

        withContext(Dispatchers.IO) {
            mirrorAccountToSyncPrefs(
                account = account,
                authorPenName = cleanName,
                commitImmediately = true
            )
        }

        appContext.shayariDataStore.edit { prefs ->
            prefs[Keys.LOCAL_ACCOUNT_REGISTERED] = true
            prefs[Keys.LOCAL_ACCOUNT_LOGGED_IN] = true
            prefs[Keys.LOCAL_ACCOUNT_FULL_NAME] = cleanName
            prefs[Keys.LOCAL_ACCOUNT_USERNAME] = derivedUsername
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
        val updatedAccount = current.copy(isLoggedIn = true)
        withContext(Dispatchers.IO) {
            mirrorAccountToSyncPrefs(
                account = updatedAccount,
                authorPenName = current.fullName.ifBlank {
                    syncPrefs.getString(SYNC_AUTHOR_PEN_NAME, "").orEmpty()
                },
                commitImmediately = true
            )
        }
        appContext.shayariDataStore.edit { prefs ->
            prefs[Keys.LOCAL_ACCOUNT_REGISTERED] = true
            prefs[Keys.LOCAL_ACCOUNT_LOGGED_IN] = true
            prefs[Keys.LOCAL_ACCOUNT_FULL_NAME] = current.fullName
            prefs[Keys.LOCAL_ACCOUNT_USERNAME] = current.username
            prefs[Keys.LOCAL_ACCOUNT_CONTACT] = current.contact
            prefs[Keys.LOCAL_ACCOUNT_BIO] = current.bio
            prefs[Keys.LOCAL_ACCOUNT_PASSWORD_HASH] = current.passwordHash
            if (current.fullName.isNotBlank()) {
                prefs[Keys.AUTHOR_PEN_NAME] = current.fullName
            }
        }
        return LoginLocalResult.Success
    }

    suspend fun resetLocalPasswordAndLogin(newPassword: String): Boolean {
        val current = preferencesFlow.first().localAccount
        if (!current.isRegistered || newPassword.trim().length < 4) return false
        val newHash = hashLocalPassword(newPassword)
        val updatedAccount = current.copy(passwordHash = newHash, isLoggedIn = true)
        withContext(Dispatchers.IO) {
            mirrorAccountToSyncPrefs(
                account = updatedAccount,
                authorPenName = current.fullName.ifBlank {
                    syncPrefs.getString(SYNC_AUTHOR_PEN_NAME, "").orEmpty()
                },
                commitImmediately = true
            )
        }
        appContext.shayariDataStore.edit { prefs ->
            prefs[Keys.LOCAL_ACCOUNT_REGISTERED] = true
            prefs[Keys.LOCAL_ACCOUNT_PASSWORD_HASH] = newHash
            prefs[Keys.LOCAL_ACCOUNT_LOGGED_IN] = true
            prefs[Keys.LOCAL_ACCOUNT_FULL_NAME] = current.fullName
            prefs[Keys.LOCAL_ACCOUNT_USERNAME] = current.username
            prefs[Keys.LOCAL_ACCOUNT_CONTACT] = current.contact
            prefs[Keys.LOCAL_ACCOUNT_BIO] = current.bio
            if (current.fullName.isNotBlank()) {
                prefs[Keys.AUTHOR_PEN_NAME] = current.fullName
            }
        }
        return true
    }

    suspend fun logoutLocalAccount() {
        withContext(Dispatchers.IO) {
            syncPrefs.edit().putBoolean(SYNC_LOCAL_LOGGED_IN, false).commit()
        }
        appContext.shayariDataStore.edit { prefs ->
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
        withContext(Dispatchers.IO) {
            val editor = syncPrefs.edit()
            if (cleanName.isNotBlank()) {
                editor.putString(SYNC_AUTHOR_PEN_NAME, cleanName)
                editor.putString(SYNC_LOCAL_FULL_NAME, cleanName)
            }
            editor.putString(SYNC_LOCAL_BIO, cleanBio)
                .putString(SYNC_SOCIAL_FACEBOOK, normalizedLinks.facebookUrl)
                .putString(SYNC_SOCIAL_INSTAGRAM, normalizedLinks.instagramUrl)
                .putString(SYNC_SOCIAL_WHATSAPP, normalizedLinks.whatsappUrl)
                .putString(SYNC_SOCIAL_TWITTER, normalizedLinks.twitterUrl)
                .putString(SYNC_SOCIAL_TELEGRAM, normalizedLinks.telegramUrl)
                .putString(SYNC_SOCIAL_YOUTUBE, normalizedLinks.youtubeUrl)
                .commit()
        }
        appContext.shayariDataStore.edit { prefs ->
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

    private companion object {
        const val SYNC_PREFS_NAME = "shayari_account_session_sync"
        const val SYNC_THEME_MODE = "theme_mode"
        const val SYNC_FONT_SCALE = "font_scale"
        const val SYNC_FONT_STYLE = "font_style"
        const val SYNC_CARD_STYLE = "card_style"
        const val SYNC_DAILY_NOTIF_ENABLED = "daily_notif_enabled"
        const val SYNC_DAILY_NOTIF_HOUR = "daily_notif_hour"
        const val SYNC_DAILY_NOTIF_MINUTE = "daily_notif_minute"
        const val SYNC_AUTHOR_PEN_NAME = "author_pen_name"
        const val SYNC_OWNER_STATS_ENABLED = "owner_stats_enabled"

        const val SYNC_LOCAL_REGISTERED = "local_account_registered"
        const val SYNC_LOCAL_LOGGED_IN = "local_account_logged_in"
        const val SYNC_LOCAL_FULL_NAME = "local_account_full_name"
        const val SYNC_LOCAL_USERNAME = "local_account_username"
        const val SYNC_LOCAL_CONTACT = "local_account_contact"
        const val SYNC_LOCAL_BIO = "local_account_bio"
        const val SYNC_LOCAL_PASSWORD_HASH = "local_account_password_hash"
        const val SYNC_SOCIAL_FACEBOOK = "social_facebook"
        const val SYNC_SOCIAL_INSTAGRAM = "social_instagram"
        const val SYNC_SOCIAL_WHATSAPP = "social_whatsapp"
        const val SYNC_SOCIAL_TWITTER = "social_twitter"
        const val SYNC_SOCIAL_TELEGRAM = "social_telegram"
        const val SYNC_SOCIAL_YOUTUBE = "social_youtube"
    }
}

sealed interface LoginLocalResult {
    data object Success : LoginLocalResult
    data object NoAccountRegistered : LoginLocalResult
    data object InvalidCredentials : LoginLocalResult
}
