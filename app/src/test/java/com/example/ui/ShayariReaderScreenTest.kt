package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.example.data.model.CardStyle
import com.example.data.model.FontScaleOption
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariFontStyle
import com.example.ui.screens.ShayariReaderScreen
import com.example.ui.theme.ShayariTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ShayariReaderScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val firstShayari = ShayariEntity(
        id = 1,
        couplet = "कहाँ मैं और कहाँ ये नख़्वत-ए-दिल,\nमगर तेरी नज़र ने कर दिया क्या।",
        poet = "अमीर ख़ुसरो",
        categoryId = ShayariCategory.MOHABBAT.id,
        moodTag = "मोहब्बत"
    )

    private val secondShayari = ShayariEntity(
        id = 2,
        couplet = "इश्क़ ने 'ग़ालिब' निकम्मा कर दिया,\nवरना हम भी आदमी थे काम के।",
        poet = "मिर्ज़ा ग़ालिब",
        categoryId = ShayariCategory.DARD.id,
        moodTag = "दर्द"
    )

    private val thirdShayari = ShayariEntity(
        id = 3,
        couplet = "ज़िंदगी एक सफ़र है सुहाना,\nहर मोड़ पर मिलता है नया फ़साना।",
        poet = "गुलज़ार",
        categoryId = ShayariCategory.ZINDAGI.id,
        moodTag = "ज़िंदगी"
    )

    @Test
    fun `ShayariReaderScreen hides bottom share button for clean Reels vertical scrolling`() {
        var backClicked = false
        var favoritedId: Int? = null

        composeRule.setContent {
            ShayariTheme {
                ShayariReaderScreen(
                    shayariList = listOf(firstShayari, secondShayari, thirdShayari),
                    initialShayariId = firstShayari.id,
                    selectedCardStyle = CardStyle.NIGHT_MUSHAIRA,
                    selectedFontScale = FontScaleOption.MEDIUM,
                    selectedFontStyle = ShayariFontStyle.CLASSIC_SERIF,
                    onBack = { backClicked = true },
                    onToggleFavorite = { favoritedId = it.id },
                    onShareText = {},
                    onDownloadImage = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(firstShayari.couplet).assertIsDisplayed()
        composeRule.onNodeWithText("अमीर ख़ुसरो").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_reels_indicator").assertIsDisplayed()

        // Bottom Share button and clutter controls are absent so Reels scrolling is unobstructed
        composeRule.onNodeWithTag("reader_share_text_button").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_copy_button").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_customize_toggle").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_prev_button").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_next_button").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_settings_button").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_share_image_button").assertDoesNotExist()

        // Reels-style swipe UP advances to second shayari
        composeRule.onNodeWithTag("reader_pager").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(secondShayari.couplet).assertIsDisplayed()
        composeRule.onNodeWithText("मिर्ज़ा ग़ालिब").assertIsDisplayed()

        composeRule.onNodeWithTag("reader_favorite_button").performClick()
        assertEquals(secondShayari.id, favoritedId)

        // Swipe UP to third, then swipe DOWN back to second
        composeRule.onNodeWithTag("reader_pager").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(thirdShayari.couplet).assertIsDisplayed()

        composeRule.onNodeWithTag("reader_pager").performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(secondShayari.couplet).assertIsDisplayed()

        composeRule.onNodeWithTag("reader_back_button").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun `double tapping Reel likes current shayari`() {
        var favoritedId: Int? = null

        composeRule.setContent {
            ShayariTheme {
                ShayariReaderScreen(
                    shayariList = listOf(firstShayari, secondShayari),
                    initialShayariId = firstShayari.id,
                    selectedCardStyle = CardStyle.NIGHT_MUSHAIRA,
                    selectedFontScale = FontScaleOption.MEDIUM,
                    selectedFontStyle = ShayariFontStyle.CLASSIC_SERIF,
                    onBack = {},
                    onToggleFavorite = { favoritedId = it.id },
                    onShareText = {},
                    onDownloadImage = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_image_area").performTouchInput {
            doubleClick(position = Offset(24f, 160f))
        }
        composeRule.waitForIdle()
        assertEquals(firstShayari.id, favoritedId)
    }

    @Test
    fun `long pressing shayari opens action sheet with Copy Download Share Collab and Write`() {
        var copiedId: Int? = null
        var downloadedId: Int? = null
        var sharedId: Int? = null
        var collabId: Int? = null
        var writeNewId: Int? = null

        composeRule.setContent {
            ShayariTheme {
                ShayariReaderScreen(
                    shayariList = listOf(firstShayari, secondShayari),
                    initialShayariId = firstShayari.id,
                    selectedCardStyle = CardStyle.NIGHT_MUSHAIRA,
                    selectedFontScale = FontScaleOption.MEDIUM,
                    selectedFontStyle = ShayariFontStyle.CLASSIC_SERIF,
                    onBack = {},
                    onToggleFavorite = {},
                    onCopyText = { copiedId = it.id },
                    onShareText = { sharedId = it.id },
                    onDownloadImage = { downloadedId = it.id },
                    onCollab = { collabId = it.id },
                    onWriteNew = { writeNewId = it.id }
                )
            }
        }

        composeRule.waitForIdle()
        // Swipe to second shayari so we verify all 5 long-press actions target the active Reel
        composeRule.onNodeWithTag("reader_pager").performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        fun openLongPressMenuOnCouplet() {
            composeRule.onNodeWithText(secondShayari.couplet).performTouchInput {
                longClick(durationMillis = 800L)
            }
            composeRule.mainClock.advanceTimeBy(100L)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("reader_action_sheet").assertIsDisplayed()
        }

        // 1. Copy
        openLongPressMenuOnCouplet()
        composeRule.onNodeWithTag("reader_action_copy_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(secondShayari.id, copiedId)
        composeRule.onNodeWithTag("reader_action_sheet").assertDoesNotExist()

        // 2. Download
        openLongPressMenuOnCouplet()
        composeRule.onNodeWithTag("reader_action_download_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(secondShayari.id, downloadedId)

        // 3. Share
        openLongPressMenuOnCouplet()
        composeRule.onNodeWithTag("reader_action_share_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(secondShayari.id, sharedId)

        // 4. Collab
        openLongPressMenuOnCouplet()
        composeRule.onNodeWithTag("reader_action_collab_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(secondShayari.id, collabId)

        // 5. Write
        openLongPressMenuOnCouplet()
        composeRule.onNodeWithTag("reader_action_write_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(secondShayari.id, writeNewId)
    }
}
