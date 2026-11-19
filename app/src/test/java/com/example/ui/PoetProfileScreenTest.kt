package com.example.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.data.model.CommunityShayari
import com.example.data.model.PoetFollower
import com.example.data.model.PoetProfile
import com.example.data.model.ShayariCategory
import com.example.ui.screens.AccountProfileHeader
import com.example.ui.screens.EditPoetProfileForm
import com.example.ui.screens.PoetProfileScreen
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
class PoetProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val samplePoet = PoetProfile(
        id = "poet_1",
        userId = "poet_1",
        penName = "अमन शायराना",
        bio = "दिल के जज़्बात और ग़ज़लें लिखता हूँ",
        isPublic = true,
        followerCount = 12L,
        followingCount = 4L,
        postCount = 2L
    )

    private val samplePosts = listOf(
        CommunityShayari(
            id = "post_1",
            userId = "poet_1",
            penName = "अमन शायराना",
            couplet = "चाँद भी शर्मा जाए ऐसी मुस्कान तेरी,\nदिल में बसी है हर एक पहचान तेरी।",
            categoryId = ShayariCategory.MOHABBAT.id,
            promptTitle = "चाँदनी रात",
            wahCount = 18L
        )
    )

    private val sampleFollowers = listOf(
        PoetFollower(id = "f1", followerUserId = "f1", followerName = "राहुल शर्मा"),
        PoetFollower(id = "f2", followerUserId = "f2", followerName = "नेहा वर्मा")
    )

    @Test
    fun `PoetProfileScreen shows poet info, stats, follow button and triggers follow & back`() {
        var followClicked = false
        var backClicked = false

        composeRule.setContent {
            ShayariTheme {
                PoetProfileScreen(
                    poetProfile = samplePoet,
                    poetPosts = samplePosts,
                    followers = sampleFollowers,
                    isOwnProfile = false,
                    isFollowing = false,
                    userWahIds = emptySet(),
                    onBack = { backClicked = true },
                    onToggleFollow = { followClicked = true },
                    onEditProfile = {},
                    onToggleWah = {},
                    onOpenPostAsCard = {},
                    onCommentPost = {},
                    onSharePost = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("poet_profile_name").assertIsDisplayed()
        composeRule.onNodeWithText("अमन शायराना").assertIsDisplayed()
        composeRule.onNodeWithTag("poet_profile_bio").assertIsDisplayed()
        composeRule.onNodeWithTag("poet_stat_posts").assertIsDisplayed()
        composeRule.onNodeWithTag("poet_stat_followers").assertIsDisplayed()
        composeRule.onNodeWithTag("poet_stat_following").assertIsDisplayed()
        composeRule.onNodeWithTag("poet_follow_button").assertIsDisplayed()
        composeRule.onNodeWithText("फ़ॉलो करें").assertIsDisplayed()
        composeRule.onNodeWithTag("poet_followers_preview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("poet_post_card_post_1").assertIsDisplayed()

        composeRule.onNodeWithTag("poet_follow_button").performClick()
        assertTrue(followClicked)

        composeRule.onNodeWithTag("poet_profile_back_button").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun `PoetProfileScreen shows Following state and Edit CTA on own profile`() {
        var editClicked = false

        composeRule.setContent {
            ShayariTheme {
                PoetProfileScreen(
                    poetProfile = samplePoet,
                    poetPosts = samplePosts,
                    followers = emptyList(),
                    isOwnProfile = true,
                    isFollowing = true,
                    userWahIds = setOf("post_1"),
                    onBack = {},
                    onToggleFollow = {},
                    onEditProfile = { editClicked = true },
                    onToggleWah = {},
                    onOpenPostAsCard = {},
                    onCommentPost = {},
                    onSharePost = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("poet_edit_profile_button").assertIsDisplayed()
        composeRule.onNodeWithTag("poet_follow_button").assertDoesNotExist()

        composeRule.onNodeWithTag("poet_edit_profile_button").performClick()
        assertTrue(editClicked)
    }

    @Test
    fun `EditPoetProfileForm validates minimum pen name length and saves updated profile`() {
        var savedName = ""
        var savedBio = ""

        composeRule.setContent {
            ShayariTheme {
                Surface {
                    EditPoetProfileForm(
                        initialPenName = "अमन",
                        initialBio = "पुरानी बायो",
                        onDismiss = {},
                        onSave = { name, bio ->
                            savedName = name
                            savedBio = bio
                        }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("save_poet_profile_button").assertIsEnabled()

        composeRule.onNodeWithTag("edit_poet_pen_name_input").performTextClearance()
        composeRule.onNodeWithTag("edit_poet_pen_name_input").performTextInput("अ")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("save_poet_profile_button").assertIsNotEnabled()

        composeRule.onNodeWithTag("edit_poet_pen_name_input").performTextClearance()
        composeRule.onNodeWithTag("edit_poet_pen_name_input").performTextInput("कबीर दास")
        composeRule.onNodeWithTag("edit_poet_bio_input").performTextClearance()
        composeRule.onNodeWithTag("edit_poet_bio_input").performTextInput("नई शायरी की दुनिया")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("save_poet_profile_button").assertIsEnabled()
        composeRule.onNodeWithTag("save_poet_profile_button").performClick()
        composeRule.waitForIdle()

        assertEquals("कबीर दास", savedName)
        assertEquals("नई शायरी की दुनिया", savedBio)
    }

    @Test
    fun `AccountProfileHeader shows social stats, bio, edit actions and Kavi Manch card when enabled`() {
        var editClicked = false
        var profileClicked = false
        var kaviManchClicked = false

        composeRule.setContent {
            ShayariTheme {
                AccountProfileHeader(
                    photoUrl = null,
                    displayName = "अमन शायराना",
                    email = "aman@example.com",
                    postCount = 5,
                    onSignOut = {},
                    isSignedIn = true,
                    bio = "हर शाम एक नई ग़ज़ल",
                    followerCount = 9L,
                    followingCount = 3L,
                    showSocialStats = true,
                    onEditProfile = { editClicked = true },
                    onOpenMyPoetProfile = { profileClicked = true },
                    onOpenKaviManch = { kaviManchClicked = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("account_poet_bio").assertIsDisplayed()
        composeRule.onNodeWithTag("account_stat_followers").assertIsDisplayed()
        composeRule.onNodeWithText("फ़ॉलोअर्स: 9").assertIsDisplayed()
        composeRule.onNodeWithTag("account_stat_following").assertIsDisplayed()
        composeRule.onNodeWithText("फ़ॉलोइंग: 3").assertIsDisplayed()
        composeRule.onNodeWithTag("account_edit_profile_button").performClick()
        assertTrue(editClicked)
        composeRule.onNodeWithTag("account_my_poet_profile_button").performClick()
        assertTrue(profileClicked)
        composeRule.onNodeWithTag("account_open_kavi_manch_card").performClick()
        assertTrue(kaviManchClicked)
    }
}
