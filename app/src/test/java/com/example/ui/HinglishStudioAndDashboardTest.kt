package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.model.PersonalShayari
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.remote.PersonalShayariStore
import com.example.ui.screens.MehfilScreen
import com.example.ui.studio.ShayariWriteStudioScreen
import com.example.ui.theme.ShayariTheme
import com.example.ui.viewmodel.PersonalShayariViewModel
import com.example.ui.viewmodel.ShayariUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class HinglishStudioAndDashboardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class InMemoryStore : PersonalShayariStore {
        private val items = MutableStateFlow<List<PersonalShayari>>(emptyList())
        override fun observePersonalShayari(userId: String): Flow<List<PersonalShayari>> = items
        override suspend fun savePersonalShayari(title: String, themeTag: String, couplet: String): Result<String> =
            Result.success("id_1")
        override suspend fun updatePersonalShayari(shayariId: String, title: String, themeTag: String, couplet: String): Result<Unit> =
            Result.success(Unit)
        override suspend fun deletePersonalShayari(shayariId: String): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun `ShayariWriteStudioScreen preserves user keyboard text and offers quick format bar`() {
        val vm = PersonalShayariViewModel(InMemoryStore(), "guest_1")

        composeRule.setContent {
            ShayariTheme {
                ShayariWriteStudioScreen(
                    viewModel = vm,
                    authorName = "अतिथि",
                    onClose = {},
                    onSaved = {},
                    onFeedback = {}
                )
            }
        }

        composeRule.onNodeWithTag("studio_quick_format_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_newline_key").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_purnviram_key").assertIsDisplayed()

        // User's own keyboard text (Hinglish or Hindi) stays untouched without forced conversion
        composeRule.onNodeWithTag("personal_couplet_input").performTextInput("Dil ki baat ")
        composeRule.waitForIdle()
        assertEquals("Dil ki baat ", vm.uiState.value.coupletInput)

        // Quick newline + mood chip insertion works cleanly at cursor
        composeRule.onNodeWithTag("studio_newline_key").performClick()
        composeRule.onNodeWithTag("mood_word_chip_दिल").performClick()
        composeRule.waitForIdle()
        assertEquals("Dil ki baat \nदिल ", vm.uiState.value.coupletInput)
    }

    @Test
    fun `ShayariWriteStudioScreen displays signed-in author name and never shows मेरी कलम`() {
        val vm = PersonalShayariViewModel(InMemoryStore(), "user_1")

        composeRule.setContent {
            ShayariTheme {
                ShayariWriteStudioScreen(
                    viewModel = vm,
                    authorName = "Prince Orviel",
                    onClose = {},
                    onSaved = {},
                    onFeedback = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("studio_edit_author_chip").assertIsDisplayed()
        composeRule.onNodeWithText("— Prince Orviel").assertIsDisplayed()
        composeRule.onNodeWithText("— मेरी कलम").assertDoesNotExist()
    }

    @Test
    fun `ShayariWriteStudioScreen allows inline author name entry when authorName is blank`() {
        val vm = PersonalShayariViewModel(InMemoryStore(), "guest_1")
        var updatedAuthor = ""

        composeRule.setContent {
            ShayariTheme {
                ShayariWriteStudioScreen(
                    viewModel = vm,
                    authorName = "",
                    onAuthorNameChanged = { updatedAuthor = it },
                    onClose = {},
                    onSaved = {},
                    onFeedback = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("— अपना नाम लिखें").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_edit_author_chip").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("studio_author_name_input")
            .assertIsDisplayed()
            .performTextInput("अमन शर्मा")
        composeRule.waitForIdle()

        assertEquals("अमन शर्मा", updatedAuthor)
        composeRule.onNodeWithText("— अमन शर्मा").assertIsDisplayed()
    }

    @Test
    fun `MehfilScreen quick read, quick write, and search mood pills trigger expected callbacks`() {
        val sample = ShayariEntity(
            id = 11,
            couplet = "दिल से लिखी हर बात असर करती है,\nसच्ची दुआ हर सफ़र आसान करती है।",
            poet = "अज्ञात शायर",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "प्यार",
            isFeatured = true
        )
        var quickReadOpened = false
        var quickWriteOpened = false
        var lastSearch = ""

        composeRule.setContent {
            ShayariTheme {
                MehfilScreen(
                    uiState = ShayariUiState(
                        isLoading = false,
                        allShayari = listOf(sample),
                        filteredShayari = listOf(sample),
                        featuredShayari = sample,
                        popularShayari = listOf(sample)
                    ),
                    isDarkTheme = false,
                    onToggleTheme = {},
                    onSearchQueryChanged = { lastSearch = it },
                    onCategorySelected = {},
                    onOpenReader = { _, _ -> quickReadOpened = true },
                    onToggleFavorite = {},
                    onComment = {},
                    onShare = {},
                    onOpenKaviManch = { quickWriteOpened = true },
                    searchExpanded = true
                )
            }
        }

        composeRule.onNodeWithTag("quick_search_chip_प्यार").assertIsDisplayed().performClick()
        assertEquals("प्यार", lastSearch)

        composeRule.onNodeWithTag("mehfil_surprise_read_button").assertIsDisplayed().performClick()
        assertTrue(quickReadOpened)

        composeRule.onNodeWithTag("mehfil_hinglish_write_button").assertIsDisplayed().performClick()
        assertTrue(quickWriteOpened)
    }
}
