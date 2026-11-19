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
import com.example.data.model.ShayariFontStyle
import com.example.data.model.ThemeModeOption
import kotlinx.coroutines.flow.Flow
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
    val authorPenName: String = ""
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
    }

    val preferencesFlow: Flow<UserPreferences> = context.shayariDataStore.data.map { prefs ->
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
            authorPenName = prefs[Keys.AUTHOR_PEN_NAME] ?: ""
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
}
