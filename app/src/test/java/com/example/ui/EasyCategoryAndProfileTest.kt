package com.example.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.SocialLinks
import com.example.ui.screens.EasyProfileEditorScreen
import com.example.ui.screens.MehfilScreen
import com.example.ui.theme.ShayariTheme
import com.example.ui.viewmodel.ShayariUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class EasyCategoryAndProfileTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleShayari = ShayariEntity(
        id = 701,
        couplet = "हौसले बुलंद हों तो राहें ख़ुद बन जाती हैं,\nअँधेरों में भी रोशनी की किरणें आती हैं।",
        poet = "शायर दोस्त",
        categoryId = ShayariCategory.HAUSLA.id,
        moodTag = "मोटिवेशनल",
        isFeatured = true
    )

    @Test
    fun `MehfilScreen toggles inline 3-column category picker and selects category in one tap`() {
        var selectedCategoryId: String? = null

        composeRule.setContent {
            ShayariTheme {
                MehfilScreen(
                    uiState = ShayariUiState(
                        isLoading = false,
                        allShayari = listOf(sampleShayari),
                        filteredShayari = listOf(sampleShayari),
                        featuredShayari = sampleShayari,
                        categoryCounts = mapOf(
                            ShayariCategory.ROMANTIC.id to 202,
                            ShayariCategory.DOSTI.id to 200,
                            ShayariCategory.HAUSLA.id to 150
                        )
                    ),
                    isDarkTheme = false,
                    onToggleTheme = {},
                    onSearchQueryChanged = {},
                    onCategorySelected = { selectedCategoryId = it },
                    onOpenReader = { _, _ -> },
                    onToggleFavorite = {},
                    onComment = {},
                    onShare = {},
                    isOwner = false
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inline_categories_panel").assertDoesNotExist()

        // Tap grid icon button to open inline "सभी श्रेणियाँ चुनें" card
        composeRule.onNodeWithTag("top_categories_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("inline_categories_panel").assertIsDisplayed()
        composeRule.onNodeWithText("सभी श्रेणियाँ चुनें").assertIsDisplayed()
        composeRule.onNodeWithTag("inline_categories_close_button").assertIsDisplayed()
        composeRule.onNodeWithTag("inline_category_chip_all").assertIsDisplayed()
        composeRule.onNodeWithTag("inline_category_chip_romantic").assertIsDisplayed()

        // Tap "दोस्ती" directly — selects it and auto-collapses the panel
        composeRule.onNodeWithTag("inline_category_chip_dosti").performClick()
        composeRule.waitForIdle()

        assertEquals(ShayariCategory.DOSTI.id, selectedCategoryId)
        composeRule.onNodeWithTag("inline_categories_panel").assertDoesNotExist()

        // Re-open and close explicitly via "बंद करें"
        composeRule.onNodeWithTag("top_categories_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inline_categories_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("inline_categories_close_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inline_categories_panel").assertDoesNotExist()
    }

    @Test
    fun `EasyProfileEditorScreen lets user pick photo, edit name and bio, and save cleanly`() {
        var photoPickerTriggered = false
        var savedName = ""
        var savedBio = ""
        var savedPhoto = ""
        var savedLinks = SocialLinks()

        composeRule.setContent {
            ShayariTheme {
                EasyProfileEditorScreen(
                    initialName = "Prince",
                    initialBio = "Sensitive",
                    initialStoredPhotoUri = "",
                    onBack = {},
                    onPickPhotoClickOverride = { photoPickerTriggered = true },
                    onSave = { name, bio, photoUri, links ->
                        savedName = name
                        savedBio = bio
                        savedPhoto = photoUri
                        savedLinks = links
                    }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("easy_profile_editor_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Account").assertIsDisplayed()
        composeRule.onNodeWithText("Profile Photo").assertIsDisplayed()

        composeRule.onNodeWithTag("easy_profile_choose_photo_button").assertIsDisplayed().performClick()
        assertTrue(photoPickerTriggered)

        composeRule.onNodeWithTag("easy_profile_name_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("easy_profile_name_input").performTextInput("प्रिंस शायर")
        composeRule.onNodeWithTag("easy_profile_bio_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("easy_profile_bio_input").performTextInput("दिल से लिखने वाला")

        composeRule.onNodeWithTag("easy_profile_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("प्रिंस शायर", savedName)
        assertEquals("दिल से लिखने वाला", savedBio)
        assertEquals("", savedPhoto)
        assertEquals(SocialLinks(), savedLinks)
    }

    @Test
    fun `UserPreferencesRepository updateEasyProfile persists name, bio, and photo URI across restarts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo1 = UserPreferencesRepository(context)

        runBlocking {
            repo1.updateEasyProfile(
                penName = "Prince",
                bio = "Sensitive",
                profilePhotoUri = "file:///data/user/0/com.example/files/profile_avatar_1.jpg"
            )
        }

        // Fresh repository instance simulates app restart
        val repo2 = UserPreferencesRepository(context)
        val cached = repo2.readCachedPreferencesSync()
        assertEquals("Prince", cached.authorPenName)
        assertEquals("Prince", cached.localAccount.fullName)
        assertEquals("Sensitive", cached.localAccount.bio)
        assertEquals("file:///data/user/0/com.example/files/profile_avatar_1.jpg", cached.localAccount.profilePhotoUri)
    }
}
