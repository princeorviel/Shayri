package com.example.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.PersonalShayari
import com.example.data.remote.PersonalShayariStore
import com.example.ui.components.ShayariAiAssistPanel
import com.example.ui.theme.ShayariTheme
import com.example.ui.viewmodel.PersonalShayariViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShayariAiAssistantTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `parseShayariSuggestionsJson parses, trims, and deduplicates suggestions`() {
        val raw = """
            {
              "hint": "  ‘दिल’ के लिए हल्की और ग़ज़ल-नुमा क़ाफ़िया ध्वनि चुनें।  ",
              "rhymes": [" महफ़िल ", "साहिल", "महफ़िल", "", "क़ाबिल"],
              "completions": ["तेरे बिना सूनी है हर महफ़िल", "तेरे बिना सूनी है हर महफ़िल", "दिल की हर धड़कन है तेरे क़ाबिल"]
            }
        """.trimIndent()

        val parsed = parseShayariSuggestionsJson(raw)

        assertEquals("‘दिल’ के लिए हल्की और ग़ज़ल-नुमा क़ाफ़िया ध्वनि चुनें।", parsed.hint)
        assertEquals(listOf("महफ़िल", "साहिल", "क़ाबिल"), parsed.rhymes)
        assertEquals(
            listOf(
                "तेरे बिना सूनी है हर महफ़िल",
                "दिल की हर धड़कन है तेरे क़ाबिल"
            ),
            parsed.completions
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `parseShayariSuggestionsJson throws when both rhymes and completions are empty`() {
        parseShayariSuggestionsJson("""{"hint":"केवल सुझाव","rhymes":[],"completions":[]}""")
    }

    @Test
    fun `buildShayariPrompt formats rhyme and completion prompts with draft and theme`() {
        val rhymePrompt = buildShayariPrompt(
            mode = ShayariAssistMode.RHYMES,
            draft = "चाँदनी रात में तेरा नाम",
            themeOrTitle = "मोहब्बत"
        )
        assertTrue(rhymePrompt.contains("चाँदनी रात में तेरा नाम"))
        assertTrue(rhymePrompt.contains("मोहब्बत"))
        assertTrue(rhymePrompt.contains("तुकबंदी"))

        val completionPrompt = buildShayariPrompt(
            mode = ShayariAssistMode.COMPLETE_LINES,
            draft = "हवा के झोंके से फिर याद आया",
            themeOrTitle = "यादें"
        )
        assertTrue(completionPrompt.contains("हवा के झोंके से फिर याद आया"))
        assertTrue(completionPrompt.contains("यादें"))
        assertTrue(completionPrompt.contains("अगली पंक्तियाँ"))
    }

    @Test
    fun `ShayariDraftEditorHelper appends words and lines cleanly within maxLength`() {
        assertEquals("महफ़िल", ShayariDraftEditorHelper.appendRhymingWord("", " महफ़िल ", 60))
        assertEquals(
            "दिल की महफ़िल",
            ShayariDraftEditorHelper.appendRhymingWord("दिल की", "महफ़िल", 60)
        )
        assertEquals(
            "दिल की महफ़िल",
            ShayariDraftEditorHelper.appendRhymingWord("दिल की ", "महफ़िल", 60)
        )

        assertEquals(
            "पहली पंक्ति",
            ShayariDraftEditorHelper.appendLine("   ", " पहली पंक्ति ", 60)
        )
        assertEquals(
            "पहली पंक्ति\nदूसरी पंक्ति",
            ShayariDraftEditorHelper.appendLine("पहली पंक्ति", "दूसरी पंक्ति", 60)
        )
        assertEquals(
            "पहली पंक्ति\nदूसरी पंक्ति",
            ShayariDraftEditorHelper.appendLine("पहली पंक्ति\n", "दूसरी पंक्ति", 60)
        )
        assertEquals(
            "पहली",
            ShayariDraftEditorHelper.appendLine("", "पहली पंक्ति", 4)
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `PersonalShayariViewModel handles AI validation, success, insertion, dismiss, and errors`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val fakeClient = FakeShayariAiClient()
            val viewModel = PersonalShayariViewModel(
                repository = FakePersonalShayariStore(),
                currentUserId = "user-1",
                aiClient = fakeClient
            )
            val collectJob = backgroundScope.launch(dispatcher) { viewModel.uiState.collect() }

            // Blank draft & theme returns Hindi validation error without calling client
            viewModel.requestAiAssist(ShayariAssistMode.RHYMES)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.aiUiState is ShayariAiUiState.Error)
            assertEquals(0, fakeClient.callCount)

            // Valid draft & theme gets suggestions
            viewModel.onThemeTagChanged("प्रेम")
            viewModel.onCoupletChanged("दिल में बसी है")
            viewModel.requestAiAssist(ShayariAssistMode.RHYMES)
            advanceUntilIdle()

            val successState = viewModel.uiState.value.aiUiState as ShayariAiUiState.Success
            assertEquals(ShayariAssistMode.RHYMES, successState.mode)
            assertEquals(listOf("तस्वीर", "तक़दीर"), successState.suggestions.rhymes)

            // Insert rhyme and append line
            viewModel.insertRhymingWord("तस्वीर")
            advanceUntilIdle()
            assertEquals("दिल में बसी है तस्वीर", viewModel.uiState.value.coupletInput)

            viewModel.applyLineCompletion("धड़कन भी लिखती है तक़दीर", replaceExisting = false)
            advanceUntilIdle()
            assertEquals(
                "दिल में बसी है तस्वीर\nधड़कन भी लिखती है तक़दीर",
                viewModel.uiState.value.coupletInput
            )

            // Dismiss resets to Idle
            viewModel.dismissAiSuggestions()
            advanceUntilIdle()
            assertEquals(ShayariAiUiState.Idle, viewModel.uiState.value.aiUiState)

            // Client exception surfaces error message
            fakeClient.nextError = IllegalStateException("नेटवर्क त्रुटि")
            viewModel.requestAiAssist(ShayariAssistMode.COMPLETE_LINES)
            advanceUntilIdle()
            val errorState = viewModel.uiState.value.aiUiState as ShayariAiUiState.Error
            assertEquals("नेटवर्क त्रुटि", errorState.message)

            collectJob.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `ShayariAiAssistPanel triggers request, insertion, completion, retry, and dismiss callbacks`() {
        var aiState by mutableStateOf<ShayariAiUiState>(ShayariAiUiState.Idle)
        var requestedMode: ShayariAssistMode? = null
        var insertedWord: String? = null
        var appendedLine: String? = null
        var replacedLine: String? = null
        var dismissed = false

        val sampleSuggestions = ShayariAiSuggestions(
            hint = "क़ाफ़िया: रात / बात",
            rhymes = listOf("बरसात", "सौग़ात"),
            completions = listOf("चाँदनी ने छेड़ी दिल की बात")
        )

        composeRule.setContent {
            ShayariTheme {
                ShayariAiAssistPanel(
                    aiState = aiState,
                    onRequestRhymes = {
                        requestedMode = ShayariAssistMode.RHYMES
                        aiState = ShayariAiUiState.Success(ShayariAssistMode.RHYMES, sampleSuggestions)
                    },
                    onRequestCompletions = {
                        requestedMode = ShayariAssistMode.COMPLETE_LINES
                        aiState = ShayariAiUiState.Success(
                            ShayariAssistMode.COMPLETE_LINES,
                            sampleSuggestions
                        )
                    },
                    onInsertWord = { insertedWord = it },
                    onAppendLine = { appendedLine = it },
                    onReplaceWithLine = { replacedLine = it },
                    onDismiss = {
                        dismissed = true
                        aiState = ShayariAiUiState.Idle
                    }
                )
            }
        }

        composeRule.onNodeWithTag("ai_suggest_rhymes_button").performClick()
        assertEquals(ShayariAssistMode.RHYMES, requestedMode)
        composeRule.onNodeWithText("बरसात").assertIsDisplayed()

        composeRule.onNodeWithTag("ai_rhyme_chip_0").performClick()
        assertEquals("बरसात", insertedWord)

        composeRule.onNodeWithTag("ai_complete_line_button").performClick()
        assertEquals(ShayariAssistMode.COMPLETE_LINES, requestedMode)

        composeRule.onNodeWithTag("ai_append_line_0").performClick()
        assertEquals("चाँदनी ने छेड़ी दिल की बात", appendedLine)

        composeRule.onNodeWithTag("ai_replace_line_0").performClick()
        assertEquals("चाँदनी ने छेड़ी दिल की बात", replacedLine)

        composeRule.onNodeWithTag("ai_dismiss_button").performClick()
        assertTrue(dismissed)
        assertEquals(ShayariAiUiState.Idle, aiState)

        // Verify error & retry UI
        aiState = ShayariAiUiState.Error(ShayariAssistMode.RHYMES, "कनेक्शन असफल रहा")
        composeRule.onNodeWithTag("ai_error_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_retry_button").performClick()
        assertEquals(ShayariAssistMode.RHYMES, requestedMode)
        assertTrue(aiState is ShayariAiUiState.Success)
    }

    private class FakeShayariAiClient : ShayariAiClient {
        var callCount = 0
        var nextError: Throwable? = null

        override suspend fun suggest(
            mode: ShayariAssistMode,
            draft: String,
            themeOrTitle: String
        ): ShayariAiSuggestions {
            callCount++
            nextError?.let {
                nextError = null
                throw it
            }
            return ShayariAiSuggestions(
                hint = "सटीक क़ाफ़िया सुझाव",
                rhymes = listOf("तस्वीर", "तक़दीर"),
                completions = listOf("धड़कन भी लिखती है तक़दीर")
            )
        }
    }

    private class FakePersonalShayariStore : PersonalShayariStore {
        private val flow = MutableStateFlow<List<PersonalShayari>>(emptyList())

        override fun observePersonalShayari(userId: String): Flow<List<PersonalShayari>> = flow

        override suspend fun savePersonalShayari(
            title: String,
            themeTag: String,
            couplet: String
        ): Result<String> = Result.success("doc-1")

        override suspend fun updatePersonalShayari(
            shayariId: String,
            title: String,
            themeTag: String,
            couplet: String
        ): Result<Unit> = Result.success(Unit)

        override suspend fun deletePersonalShayari(shayariId: String): Result<Unit> = Result.success(Unit)
    }
}
