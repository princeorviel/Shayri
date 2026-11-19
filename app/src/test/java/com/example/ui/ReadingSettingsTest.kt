package com.example.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextStyle
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.FontScaleOption
import com.example.data.model.ShayariFontStyle
import com.example.data.model.ThemeModeOption
import com.example.ui.screens.ReadingSettingsSheet
import com.example.ui.theme.ShayariReadingStyle
import com.example.ui.theme.ShayariTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReadingSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `ShayariFontStyle and FontScaleOption resolve ids cleanly and build scaled TextStyles`() {
        assertEquals(FontScaleOption.MEDIUM, FontScaleOption.fromId("unknown"))
        assertEquals(FontScaleOption.EXTRA_LARGE, FontScaleOption.fromId("extra_large"))
        assertEquals(ShayariFontStyle.CLASSIC_SERIF, ShayariFontStyle.fromId("unknown"))
        assertEquals(ShayariFontStyle.HANDWRITTEN, ShayariFontStyle.fromId("handwritten"))

        val style = ShayariReadingStyle(
            fontScale = FontScaleOption.LARGE,
            fontStyle = ShayariFontStyle.HANDWRITTEN
        )
        val cardTextStyle = style.cardCoupletTextStyle(TextStyle.Default)
        val heroTextStyle = style.heroCoupletTextStyle(TextStyle.Default)
        val readerTextStyle = style.readerCoupletTextStyle(TextStyle.Default)

        assertEquals(FontScaleOption.LARGE.cardSp, cardTextStyle.fontSize.value, 0.01f)
        assertEquals(FontScaleOption.LARGE.heroSp, heroTextStyle.fontSize.value, 0.01f)
        assertEquals(FontScaleOption.LARGE.readerSp, readerTextStyle.fontSize.value, 0.01f)
        assertNotNull(cardTextStyle.fontFamily)
    }

    @Test
    fun `UserPreferencesRepository persists font size and font style`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = UserPreferencesRepository(context)

        repo.setFontScale(FontScaleOption.EXTRA_LARGE)
        repo.setFontStyle(ShayariFontStyle.TRADITIONAL_PRINT)

        val prefs = repo.preferencesFlow.first()
        assertEquals(FontScaleOption.EXTRA_LARGE, prefs.fontScale)
        assertEquals(ShayariFontStyle.TRADITIONAL_PRINT, prefs.fontStyle)
    }

    @Test
    fun `ReadingSettingsSheet updates font size, font style, and reset controls`() {
        var currentScale by mutableStateOf(FontScaleOption.MEDIUM)
        var currentStyle by mutableStateOf(ShayariFontStyle.CLASSIC_SERIF)
        var currentTheme by mutableStateOf(ThemeModeOption.SYSTEM)
        var resetCalled = false

        composeRule.setContent {
            ShayariTheme {
                ReadingSettingsSheet(
                    selectedFontScale = currentScale,
                    selectedFontStyle = currentStyle,
                    selectedThemeMode = currentTheme,
                    dailyNotificationEnabled = false,
                    dailyNotificationHour = 9,
                    dailyNotificationMinute = 0,
                    onFontScaleSelected = { currentScale = it },
                    onFontStyleSelected = { currentStyle = it },
                    onThemeModeSelected = { currentTheme = it },
                    onResetTypography = {
                        resetCalled = true
                        currentScale = FontScaleOption.MEDIUM
                        currentStyle = ShayariFontStyle.CLASSIC_SERIF
                    },
                    onOpenNotificationSettings = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("reading_settings_preview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_font_size_large").performScrollTo().performClick()
        assertEquals(FontScaleOption.LARGE, currentScale)

        composeRule.onNodeWithTag("settings_font_style_row")
            .performScrollTo()
            .performScrollToNode(hasTestTag("settings_font_style_handwritten"))
        composeRule.onNodeWithTag("settings_font_style_handwritten").performClick()
        assertEquals(ShayariFontStyle.HANDWRITTEN, currentStyle)
        composeRule.onNodeWithText("हाथ की लिखावट").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_theme_dark").performScrollTo().performClick()
        assertEquals(ThemeModeOption.DARK, currentTheme)

        composeRule.onNodeWithTag("settings_reset_typography_button").performScrollTo().performClick()
        assertTrue(resetCalled)
        assertEquals(FontScaleOption.MEDIUM, currentScale)
        assertEquals(ShayariFontStyle.CLASSIC_SERIF, currentStyle)
    }
}
