package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.model.ShayariCategory
import com.example.data.remote.AdminCategoryStatsSnapshot
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.theme.ShayariTheme
import com.example.ui.viewmodel.AdminDashboardUiState
import com.example.ui.viewmodel.AdminStatsLoadState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdminDashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `AdminDashboardScreen displays Google Sign-In gate when signed out`() {
        var signInClicks = 0

        composeRule.setContent {
            ShayariTheme {
                AdminDashboardScreen(
                    isFirebaseSignedIn = false,
                    uiState = AdminDashboardUiState(),
                    onSearchQueryChanged = {},
                    onSortOrderChanged = {},
                    onToggleOnlyPopulated = {},
                    onRefresh = {},
                    onSignInClickOverride = { signInClicks++ },
                    onBack = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_auth_gate").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_dashboard_google_signin").performClick()
        assertEquals(1, signInClicks)
    }

    @Test
    fun `AdminDashboardScreen displays Firestore shayari counts grouped by category and handles selection`() {
        val snapshot = AdminCategoryStatsSnapshot(
            countsByCategory = mapOf(
                ShayariCategory.MOHABBAT.id to 14,
                ShayariCategory.DARD.id to 9,
                ShayariCategory.DOSTI.id to 4
            ),
            totalShayariCount = 27,
            totalWahCount = 128L,
            activeCategoryCount = 3
        )
        var clickedCategory: ShayariCategory? = null
        var lastSearch = ""
        var filterToggled = false

        composeRule.setContent {
            ShayariTheme {
                AdminDashboardScreen(
                    isFirebaseSignedIn = true,
                    uiState = AdminDashboardUiState(
                        loadState = AdminStatsLoadState.Success(snapshot)
                    ),
                    catalogCategoryCounts = mapOf(ShayariCategory.MOHABBAT.id to 50),
                    onSearchQueryChanged = { lastSearch = it },
                    onSortOrderChanged = {},
                    onToggleOnlyPopulated = { filterToggled = true },
                    onRefresh = {},
                    onCategoryClick = { clickedCategory = it },
                    onBack = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_dashboard_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_summary_hero_card").assertIsDisplayed()
        composeRule.onNodeWithText("27").assertIsDisplayed()
        composeRule.onNodeWithText("128").assertIsDisplayed()

        composeRule.onNodeWithTag("admin_category_stats_list")
            .performScrollToNode(hasText("14 शायरी"))
        composeRule.onNodeWithTag("admin_category_count_mohabbat", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("admin_category_row_mohabbat").performClick()
        assertEquals(ShayariCategory.MOHABBAT, clickedCategory)

        composeRule.onNodeWithTag("admin_category_stats_list")
            .performScrollToNode(hasText("9 शायरी"))
        composeRule.onNodeWithTag("admin_category_count_dard", useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithTag("admin_category_stats_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("admin_dashboard_search_input"))
        composeRule.onNodeWithTag("admin_dashboard_search_input").performTextInput("Dosti")
        assertEquals("Dosti", lastSearch)

        composeRule.onNodeWithTag("admin_filter_populated").performClick()
        assertEquals(true, filterToggled)
    }

    @Test
    fun `AdminDashboardScreen shows error state with retry action`() {
        var retried = false

        composeRule.setContent {
            ShayariTheme {
                AdminDashboardScreen(
                    isFirebaseSignedIn = true,
                    uiState = AdminDashboardUiState(
                        loadState = AdminStatsLoadState.Error("नेटवर्क त्रुटि")
                    ),
                    onSearchQueryChanged = {},
                    onSortOrderChanged = {},
                    onToggleOnlyPopulated = {},
                    onRefresh = { retried = true },
                    onBack = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_dashboard_error").assertIsDisplayed()
        composeRule.onNodeWithText("नेटवर्क त्रुटि").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_dashboard_retry_button").performClick()
        assertEquals(true, retried)
    }

    @Test
    fun `AdminDashboardScreen shows catalog stats immediately and hides sign-in when cloud is unavailable`() {
        var clickedCategory: ShayariCategory? = null
        var lastSearch = ""

        composeRule.setContent {
            ShayariTheme {
                AdminDashboardScreen(
                    isFirebaseSignedIn = false,
                    isCloudConfigured = false,
                    uiState = AdminDashboardUiState(),
                    catalogCategoryCounts = mapOf(
                        ShayariCategory.MOHABBAT.id to 42,
                        ShayariCategory.DARD.id to 18
                    ),
                    onSearchQueryChanged = { lastSearch = it },
                    onSortOrderChanged = {},
                    onToggleOnlyPopulated = {},
                    onRefresh = {},
                    onCategoryClick = { clickedCategory = it },
                    onBack = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_dashboard_google_signin").assertDoesNotExist()
        composeRule.onNodeWithTag("admin_summary_hero_card").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_offline_cloud_notice").assertIsDisplayed()
        composeRule.onNodeWithText("60").assertIsDisplayed()

        composeRule.onNodeWithTag("admin_category_stats_list")
            .performScrollToNode(hasText("42 शायरी"))
        composeRule.onNodeWithTag("admin_category_count_mohabbat", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("admin_category_row_mohabbat").performClick()
        assertEquals(ShayariCategory.MOHABBAT, clickedCategory)

        composeRule.onNodeWithTag("admin_category_stats_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("admin_dashboard_search_input"))
        composeRule.onNodeWithTag("admin_dashboard_search_input").performTextInput("Dard")
        assertEquals("Dard", lastSearch)
    }
}
