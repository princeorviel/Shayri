package com.example.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ShayariDatabase
import com.example.data.local.ShayariRepository
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.ui.components.CategoryChipStrip
import com.example.ui.screens.MehfilScreen
import com.example.ui.theme.ShayariTheme
import com.example.ui.viewmodel.ShayariUiState
import com.example.ui.viewmodel.ShayariViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class CategoryFilterFeedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val coreSampleShayari = listOf(
        ShayariEntity(
            id = 101,
            couplet = "इश्क़ की बारिश में भीगते रहे हम,\nतेरी यादों के शहर में बसते रहे हम।",
            poet = "मिर्ज़ा ग़ालिब",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "मोहब्बत",
            isFeatured = true
        ),
        ShayariEntity(
            id = 102,
            couplet = "दर्द इतना था कि लफ़्ज़ ख़ामोश हो गए,\nआँसू बहते रहे और हम बेहोश हो गए।",
            poet = "अहमद फ़राज़",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "दर्द"
        ),
        ShayariEntity(
            id = 103,
            couplet = "ज़िंदगी एक सफ़र है सुहाना,\nहर मोड़ पर मिलता है नया फ़साना।",
            poet = "गुलज़ार",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "ज़िंदगी"
        ),
        ShayariEntity(
            id = 104,
            couplet = "दोस्ती का रिश्ता सबसे अनमोल होता है,\nसच्चा यार हर ग़म का मोल होता है।",
            poet = "बशीर बद्र",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "दोस्ती"
        ),
        ShayariEntity(
            id = 105,
            couplet = "हौसला रख मंज़िल ज़रूर मिलेगी,\nमेहनत की कली एक दिन ज़रूर खिलेगी।",
            poet = "दुष्यंत कुमार",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "हौसला"
        )
    )

    @Test
    fun `CategoryChipStrip spotlights Mohabbat Dard Zindagi Dosti and Hausla and switches selection cleanly`() {
        var selectedId: String? = null

        composeRule.setContent {
            ShayariTheme {
                CategoryChipStrip(
                    selectedCategoryId = null,
                    onSelectCategory = { selectedId = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_chip_all").assertIsDisplayed().assertIsSelected()

        val expectedCore = listOf(
            ShayariCategory.MOHABBAT to "Mohabbat",
            ShayariCategory.DARD to "Dard",
            ShayariCategory.ZINDAGI to "Zindagi",
            ShayariCategory.DOSTI to "Dosti",
            ShayariCategory.HAUSLA to "Hausla"
        )
        expectedCore.forEach { (category, labelEn) ->
            composeRule.onNodeWithTag("category_filter_strip").performScrollToKey(category.id)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("category_chip_${category.id}").assertIsDisplayed().performClick()
            assertEquals(category.id, selectedId)
            composeRule.onNodeWithText(labelEn).assertIsDisplayed()
        }
    }

    @Test
    fun `MehfilScreen shows sticky category filter bar and active category banner when filtered`() {
        var latestSelectedCategory: String? = ShayariCategory.DARD.id
        val dardOnly = coreSampleShayari.filter { it.categoryId == ShayariCategory.DARD.id }

        composeRule.setContent {
            ShayariTheme {
                MehfilScreen(
                    uiState = ShayariUiState(
                        isLoading = false,
                        allShayari = coreSampleShayari,
                        filteredShayari = dardOnly,
                        featuredShayari = coreSampleShayari.first(),
                        selectedCategoryId = ShayariCategory.DARD.id
                    ),
                    isDarkTheme = false,
                    onToggleTheme = {},
                    onSearchQueryChanged = {},
                    onCategorySelected = { latestSelectedCategory = it },
                    onOpenReader = { _, _ -> },
                    onToggleFavorite = {},
                    onComment = {},
                    onShare = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_filter_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("category_chip_dard").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("active_category_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Dard • दर्द").assertIsDisplayed()
        composeRule.onNodeWithTag("shayari_card_102").assertIsDisplayed()
        composeRule.onNodeWithTag("shayari_card_101").assertDoesNotExist()

        // Tapping active category clear button resets filter to null (All)
        composeRule.onNodeWithTag("active_category_clear_button").performClick()
        assertNull(latestSelectedCategory)
    }

    @Test
    fun `MehfilScreen wired to ShayariViewModel switches live between Mohabbat Dard Zindagi Dosti Hausla and All`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                db.shayariDao().insertAll(coreSampleShayari)
            }
            val vm = ShayariViewModel(
                repository = ShayariRepository(db.shayariDao()),
                preferencesRepository = UserPreferencesRepository(context),
                autoSeed = false
            )

            composeRule.setContent {
                val state by vm.uiState.collectAsStateWithLifecycle()
                ShayariTheme {
                    MehfilScreen(
                        uiState = state,
                        isDarkTheme = false,
                        onToggleTheme = {},
                        onSearchQueryChanged = vm::onSearchQueryChanged,
                        onCategorySelected = vm::onCategorySelected,
                        onOpenReader = { _, _ -> },
                        onToggleFavorite = {},
                        onComment = {},
                        onShare = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5_000L) {
                ShadowLooper.idleMainLooper()
                !vm.uiState.value.isLoading && vm.uiState.value.allShayari.size == coreSampleShayari.size
            }

            val checks = listOf(
                ShayariCategory.MOHABBAT to 101,
                ShayariCategory.DARD to 102,
                ShayariCategory.ZINDAGI to 103,
                ShayariCategory.DOSTI to 104,
                ShayariCategory.HAUSLA to 105
            )
            for ((category, expectedCardId) in checks) {
                composeRule.onNodeWithTag("category_filter_strip").performScrollToKey(category.id)
                composeRule.waitForIdle()
                composeRule.onNodeWithTag("category_chip_${category.id}").performClick()
                composeRule.waitUntil(timeoutMillis = 5_000L) {
                    ShadowLooper.idleMainLooper()
                    vm.uiState.value.selectedCategoryId == category.id &&
                        vm.uiState.value.filteredShayari.size == 1
                }
                composeRule.waitForIdle()
                composeRule.onNodeWithTag("shayari_card_$expectedCardId").assertIsDisplayed()
                composeRule.onNodeWithText("${category.labelEn} • ${category.titleHi}").assertIsDisplayed()
            }

            // Reset back to All
            composeRule.onNodeWithTag("category_filter_strip").performScrollToKey("all")
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("category_chip_all").performClick()
            composeRule.waitUntil(timeoutMillis = 5_000L) {
                ShadowLooper.idleMainLooper()
                vm.uiState.value.selectedCategoryId == null &&
                    vm.uiState.value.filteredShayari.size == coreSampleShayari.size
            }
            assertEquals(coreSampleShayari.size, vm.uiState.value.filteredShayari.size)
        } finally {
            db.close()
        }
    }
}
