package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.data.model.LocalShayarAccount
import com.example.data.model.SocialLinks
import com.example.ui.components.ProfileManagementSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.ShayariTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthAndProfileFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `AuthScreen provides unified Google Sign-In, registration with social links, and login mode`() {
        var googleClicks = 0
        var registeredName = ""
        var registeredLinks = SocialLinks()
        var loggedInIdentifier = ""

        composeRule.setContent {
            ShayariTheme {
                AuthScreen(
                    localAccount = LocalShayarAccount(),
                    showGoogleSignIn = true,
                    onRegisterSubmit = { fullName, _, _, _, _, links ->
                        registeredName = fullName
                        registeredLinks = links
                    },
                    onLoginSubmit = { identifier, _ ->
                        loggedInIdentifier = identifier
                    },
                    onGoogleAuthSuccess = {},
                    onGoogleAuthError = {},
                    onGoogleSignInOverride = { googleClicks++ },
                    onBack = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("auth_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("auth_google_signin_button").performScrollTo().performClick()
        assertEquals(1, googleClicks)

        // Register flow with optional social link expanded
        composeRule.onNodeWithTag("auth_register_name_input").performScrollTo().performTextInput("Prince Shayar")
        composeRule.onNodeWithTag("auth_register_password_input").performScrollTo().performTextInput("1234")
        composeRule.onNodeWithTag("auth_toggle_optional_socials").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("social_input_instagram").performScrollTo().performTextInput("@prince_shayar")
        composeRule.onNodeWithTag("auth_register_submit_button").performScrollTo().performClick()

        assertEquals("Prince Shayar", registeredName)
        assertEquals("https://instagram.com/prince_shayar", registeredLinks.instagramUrl)

        // Switch to Login tab and submit
        composeRule.onNodeWithTag("auth_tab_login").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("auth_login_identifier_input").performScrollTo().performTextInput("prince_shayar")
        composeRule.onNodeWithTag("auth_login_password_input").performScrollTo().performTextInput("1234")
        composeRule.onNodeWithTag("auth_login_submit_button").performScrollTo().performClick()

        assertEquals("prince_shayar", loggedInIdentifier)
    }

    @Test
    fun `ProfileManagementSheet saves updated pen name, bio, and normalized social media links`() {
        var savedName = ""
        var savedBio = ""
        var savedLinks = SocialLinks()

        composeRule.setContent {
            ShayariTheme {
                ProfileManagementSheet(
                    initialPenName = "Prince",
                    initialBio = "दिल की बात",
                    initialSocialLinks = SocialLinks(),
                    onDismiss = {},
                    onSave = { name, bio, links ->
                        savedName = name
                        savedBio = bio
                        savedLinks = links
                    }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_management_sheet").assertIsDisplayed()

        composeRule.onNodeWithTag("edit_poet_pen_name_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("edit_poet_pen_name_input").performTextInput("प्रिंस शायर")
        composeRule.onNodeWithTag("edit_poet_bio_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("edit_poet_bio_input").performTextInput("इश्क़ और दर्द की शायरी")

        composeRule.onNodeWithTag("social_input_facebook").performScrollTo().performTextInput("prince.fb")
        composeRule.onNodeWithTag("social_input_instagram").performScrollTo().performTextInput("@prince_ig")
        composeRule.onNodeWithTag("social_input_whatsapp").performScrollTo().performTextInput("9876543210")

        composeRule.onNodeWithTag("save_poet_profile_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("प्रिंस शायर", savedName)
        assertEquals("इश्क़ और दर्द की शायरी", savedBio)
        assertEquals("https://facebook.com/prince.fb", savedLinks.facebookUrl)
        assertEquals("https://instagram.com/prince_ig", savedLinks.instagramUrl)
        assertTrue(savedLinks.whatsappUrl.contains("wa.me/919876543210"))
    }
}
