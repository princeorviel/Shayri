package com.example.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalPersonalShayariRepository
import com.example.data.local.ShayariDatabase
import com.example.data.local.ShayariRepository
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.CommunityShayari
import com.example.data.model.LocalShayarAccount
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariTagMatcher
import com.example.data.model.SocialLinks
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EasyProfileEditorScreen
import com.example.ui.screens.MehfilScreen
import com.example.ui.studio.ShayariWriteStudioScreen
import com.example.ui.theme.ShayariTheme
import com.example.ui.viewmodel.PersonalShayariViewModel
import com.example.ui.viewmodel.ShayariUiState
import com.example.ui.viewmodel.ShayariViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

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
        assertTrue(cached.localAccount.isRegistered)
        assertTrue(cached.localAccount.isLoggedIn)
    }

    @Test
    fun `UserPreferencesRepository quickCreateNameAccount creates permanent logged-in account with name only`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo1 = UserPreferencesRepository(context)

        runBlocking {
            repo1.quickCreateNameAccount("अमन शायर")
        }

        // Fresh repository simulates reopening app
        val repo2 = UserPreferencesRepository(context)
        val cached = repo2.readCachedPreferencesSync()
        assertEquals("अमन शायर", cached.authorPenName)
        assertEquals("अमन शायर", cached.localAccount.fullName)
        assertTrue(cached.localAccount.isRegistered)
        assertTrue(cached.localAccount.isLoggedIn)
        assertTrue(cached.localAccount.username.isNotBlank())
    }

    @Test
    fun `AccountScreen displays 1-step name account box for guests and submits name directly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val personalVm = PersonalShayariViewModel(
                repository = LocalPersonalShayariRepository(db.localPersonalShayariDao()),
                currentUserId = "local_guest"
            )
            var createdName = ""

            composeRule.setContent {
                ShayariTheme {
                    AccountScreen(
                        currentUser = null,
                        personalViewModel = personalVm,
                        communityViewModel = null,
                        onSignOut = {},
                        onOpenPersonalCard = {},
                        onCopyPersonal = {},
                        onSharePersonal = {},
                        onFeedback = {},
                        localAccount = LocalShayarAccount(),
                        onQuickCreateNameAccount = { createdName = it }
                    )
                }
            }

            composeRule.waitForIdle()
            composeRule.onNodeWithTag("account_version_badge").assertIsDisplayed()
            composeRule.onNodeWithTag("account_quick_name_card").assertIsDisplayed()
            composeRule.onNodeWithTag("account_quick_name_input").performTextInput("रवि शायर")
            composeRule.onNodeWithTag("account_quick_create_button").performClick()
            composeRule.waitForIdle()

            assertEquals("रवि शायर", createdName)
        } finally {
            db.close()
        }
    }

    @Test
    fun `AuthScreen register tab accepts name only without requiring password`() {
        var submittedName = ""
        var submittedPassword = "unset"

        composeRule.setContent {
            ShayariTheme {
                AuthScreen(
                    localAccount = LocalShayarAccount(),
                    onRegisterSubmit = { fullName, _, _, password, _, _ ->
                        submittedName = fullName
                        submittedPassword = password
                    },
                    onLoginSubmit = { _, _ -> },
                    onGoogleAuthSuccess = {},
                    onGoogleAuthError = {},
                    onBack = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("auth_register_name_input").performScrollTo().performTextInput("राहुल शायर")
        composeRule.onNodeWithTag("auth_register_submit_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("राहुल शायर", submittedName)
        assertEquals("", submittedPassword)
    }

    @Test
    fun `ShayariRepository upsertUserPostToCatalog adds post to top of total posts and updates or deletes cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.shayariDao()
            val repo = ShayariRepository(dao)

            // Seed 2 normal catalog items (ids < 1_000_000)
            dao.insertAll(
                listOf(
                    ShayariEntity(
                        id = 1,
                        couplet = "पहली सीड शायरी",
                        poet = "कवि एक",
                        categoryId = ShayariCategory.MOHABBAT.id,
                        moodTag = "मोहब्बत"
                    ),
                    ShayariEntity(
                        id = 2,
                        couplet = "दूसरी सीड शायरी",
                        poet = "कवि दो",
                        categoryId = ShayariCategory.DOSTI.id,
                        moodTag = "दोस्ती"
                    )
                )
            )
            assertEquals(2, dao.getSeedCount())
            assertEquals(2, repo.allShayari.first().size)

            // User posts a new shayari -> added to total posts at the top & matched to category
            val inserted = repo.upsertUserPostToCatalog(
                personalId = "post-abc-123",
                couplet = "दिल की हर धड़कन में तेरा नाम लिखा है,\nचाँदनी रातों में तेरा पैग़ाम लिखा है।",
                authorName = "अमन",
                title = "दिल की बात",
                themeTag = "मोहब्बत"
            )
            assertTrue(inserted.id >= ShayariRepository.USER_POST_ID_BASE)
            assertEquals(ShayariCategory.MOHABBAT.id, inserted.categoryId)

            val afterInsert = repo.allShayari.first()
            assertEquals(3, afterInsert.size)
            assertEquals(2, dao.getSeedCount())
            // User post appears FIRST ahead of seed items
            assertEquals(inserted.id, afterInsert.first().id)
            assertEquals("अमन", afterInsert.first().poet)
            assertEquals(2, afterInsert.count { it.categoryId == ShayariCategory.MOHABBAT.id })

            // Re-upserting same personalId updates in place without duplicating
            repo.upsertUserPostToCatalog(
                personalId = "post-abc-123",
                couplet = "संपादित शायरी की दो पंक्तियाँ।",
                authorName = "अमन",
                title = "नया शीर्षक",
                themeTag = "दोस्ती"
            )
            val afterUpdate = repo.allShayari.first()
            assertEquals(3, afterUpdate.size)
            assertEquals("संपादित शायरी की दो पंक्तियाँ।", afterUpdate.first().couplet)
            assertEquals(ShayariCategory.DOSTI.id, afterUpdate.first().categoryId)

            // Removing user post deletes only the user post and restores total count
            repo.removeUserPostFromCatalog("post-abc-123")
            val afterDelete = repo.allShayari.first()
            assertEquals(2, afterDelete.size)
            assertFalse(afterDelete.any { it.id >= ShayariRepository.USER_POST_ID_BASE })
        } finally {
            db.close()
        }
    }

    @Test
    fun `ShayariTagMatcher maps multiple tags to all related categories and safely trims cloud tags`() {
        val rawTags = "दर्द, बेवफ़ाई, जख्मी दिल"
        val parsed = ShayariTagMatcher.parseTags(rawTags)
        assertEquals(listOf("दर्द", "बेवफ़ाई", "जख्मी दिल"), parsed)

        val matchedCats = ShayariTagMatcher.matchingCategoryIds(
            primaryCategoryId = ShayariCategory.DARD.id,
            moodTag = rawTags
        )
        assertTrue(matchedCats.contains(ShayariCategory.DARD.id))
        assertTrue(matchedCats.contains(ShayariCategory.BEWAFAI.id))
        assertTrue(matchedCats.contains(ShayariCategory.AANSU.id))

        val trimmed = ShayariTagMatcher.trimTagsForCloudPersonal(
            "दर्द, बेवफ़ाई, जख्मी दिल, तन्हाई, अधूरा प्यार, मोहब्बत, यादें",
            maxLen = 40
        )
        assertTrue("Trimmed cloud tag length must satisfy Firestore <= 40 rule", trimmed.length <= 40)
        assertTrue(trimmed.contains("दर्द"))
    }

    @Test
    fun `ShayariViewModel merges live online community posts into Home feed, total counts, and multi-tag categories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.shayariDao()
            val repo = ShayariRepository(dao)
            runBlocking {
                dao.insertAll(
                    listOf(
                        ShayariEntity(
                            id = 1,
                            couplet = "पहली सीड शायरी मोहब्बत पर",
                            poet = "कवि एक",
                            categoryId = ShayariCategory.MOHABBAT.id,
                            moodTag = "मोहब्बत"
                        )
                    )
                )
                // Also insert local copy of same couplet to verify deduplication against online version
                repo.upsertUserPostToCatalog(
                    personalId = "local-1",
                    couplet = "खंजर से करो बात न तलवार से पूछो,\nमैं क़त्ल हुआ कैसे मेरे यार से पूछो।",
                    authorName = "Prince Orviel",
                    title = "",
                    themeTag = "दर्द, बेवफ़ाई, जख्मी दिल",
                    primaryCategoryId = ShayariCategory.DARD.id
                )
            }
            val prefs = UserPreferencesRepository(context)
            val vm = ShayariViewModel(repo, prefs, autoSeed = false)

            // Push live online Firestore posts from multiple online users
            vm.updateOnlineCommunityPosts(
                listOf(
                    CommunityShayari(
                        id = "cloud_post_1",
                        userId = "user_prince",
                        penName = "Prince Orviel",
                        couplet = "खंजर से करो बात न तलवार से पूछो,\nमैं क़त्ल हुआ कैसे मेरे यार से पूछो।",
                        categoryId = ShayariCategory.DARD.id,
                        promptTitle = "#दर्द #बेवफ़ाई #जख्मी दिल"
                    ),
                    CommunityShayari(
                        id = "cloud_post_2",
                        userId = "user_other",
                        penName = "अमन शायर",
                        couplet = "दोस्ती की मिठास हर दर्द को कम कर देती है,\nसच्चा दोस्त ज़िंदगी को जन्नत बना देता है।",
                        categoryId = ShayariCategory.DOSTI.id,
                        promptTitle = "#दोस्ती #यारी"
                    )
                )
            )

            composeRule.waitUntil(timeoutMillis = 5_000L) {
                ShadowLooper.idleMainLooper()
                val s = vm.uiState.value
                !s.isLoading && s.allShayari.size == 3
            }
            val stateAll = vm.uiState.value
            // 1 seed + 2 online posts (local duplicate deduplicated!)
            assertEquals(3, stateAll.allShayari.size)
            assertTrue(stateAll.allShayari.first().id >= ShayariViewModel.ONLINE_COMMUNITY_ID_BASE)

            // Multi-tagged post appears in BOTH Dard AND Bewafai categories
            vm.onCategorySelected(ShayariCategory.BEWAFAI.id)
            composeRule.waitUntil(timeoutMillis = 5_000L) {
                ShadowLooper.idleMainLooper()
                val s = vm.uiState.value
                s.selectedCategoryId == ShayariCategory.BEWAFAI.id &&
                    s.filteredShayari.any { it.couplet.contains("खंजर से करो बात") }
            }
            val stateBewafai = vm.uiState.value
            assertTrue(stateBewafai.filteredShayari.any { it.couplet.contains("खंजर से करो बात") })
            assertTrue((stateBewafai.categoryCounts[ShayariCategory.DARD.id] ?: 0) >= 1)
            assertTrue((stateBewafai.categoryCounts[ShayariCategory.BEWAFAI.id] ?: 0) >= 1)
        } finally {
            db.close()
        }
    }

    @Test
    fun `MehfilScreen shows YourQuote feed card style switcher and switches card style cleanly`() {
        composeRule.setContent {
            ShayariTheme {
                MehfilScreen(
                    uiState = ShayariUiState(
                        isLoading = false,
                        allShayari = listOf(sampleShayari),
                        filteredShayari = listOf(sampleShayari),
                        featuredShayari = sampleShayari
                    ),
                    isDarkTheme = false,
                    onToggleTheme = {},
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onOpenReader = { _, _ -> },
                    onToggleFavorite = {},
                    onComment = {},
                    onShare = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mehfil_feed_list").performScrollToKey("feed_card_style_strip")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("feed_card_style_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("feed_card_style_PHOTO_CINEMA").assertIsDisplayed()
        composeRule.onNodeWithTag("feed_card_style_PAPER_DIARY").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("feed_card_style_POLAROID").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `ShayariWriteStudioScreen supports 1 primary category, multiple tags, online CTA, and YourQuote templates`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val personalVm = PersonalShayariViewModel(
                repository = LocalPersonalShayariRepository(db.localPersonalShayariDao()),
                currentUserId = "local_guest"
            )
            var googleSignInRequested = false
            var publishedPrimaryCat = ""
            var publishedTags = ""

            composeRule.setContent {
                ShayariTheme {
                    ShayariWriteStudioScreen(
                        viewModel = personalVm,
                        authorName = "Prince",
                        isOnlineAccount = false,
                        onRequestGoogleSignIn = { googleSignInRequested = true },
                        onClose = {},
                        onSaved = {},
                        onFeedback = {},
                        onPostPublishedWithCategory = { _, _, _, _, primaryCat, tagsCsv, _ ->
                            publishedPrimaryCat = primaryCat
                            publishedTags = tagsCsv
                        }
                    )
                }
            }

            composeRule.waitForIdle()
            // Local account sees clickable Google sync CTA
            composeRule.onNodeWithTag("studio_google_sync_cta").assertIsDisplayed().performClick()
            assertTrue(googleSignInRequested)

            // Type couplet + select 1 primary category + add multiple tags
            composeRule.onNodeWithTag("personal_couplet_input")
                .performTextInput("खंजर से करो बात न तलवार से पूछो,\nमैं क़त्ल हुआ कैसे मेरे यार से पूछो।")
            composeRule.onNodeWithTag("studio_category_and_tags_card").performScrollTo()
            composeRule.onNodeWithTag("studio_primary_category_row").performScrollToKey(ShayariCategory.DARD.id)
            composeRule.onNodeWithTag("studio_primary_cat_dard").performClick()
            composeRule.onNodeWithTag("studio_suggested_tags_row").performScrollToKey("बेवफ़ाई")
            composeRule.onNodeWithTag("studio_multi_tag_बेवफ़ाई").performClick()
            composeRule.onNodeWithTag("studio_custom_tag_input").performScrollTo().performTextInput("जख्मी दिल")
            composeRule.onNodeWithTag("studio_add_custom_tag_button").performScrollTo().performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("studio_matching_categories_hint").performScrollTo().assertIsDisplayed()

            // Go to Design step -> YourQuote templates tab is default
            composeRule.onNodeWithTag("studio_next_design_button").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("studio_tab_templates").assertIsDisplayed()
            composeRule.onNodeWithTag("studio_template_frames_row").performScrollToKey("polaroid")
            composeRule.onNodeWithTag("studio_frame_polaroid").assertIsDisplayed().performClick()
            composeRule.onNodeWithTag("studio_save_post_button").performClick()
            composeRule.waitForIdle()

            assertEquals(ShayariCategory.DARD.id, publishedPrimaryCat)
            assertTrue(publishedTags.contains("बेवफ़ाई"))
            assertTrue(publishedTags.contains("जख्मी दिल"))
        } finally {
            db.close()
        }
    }

    @Test
    fun `ShayariViewModel universal search matches Hindi English Hinglish tags categories poets and nukta variants`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val catalog = listOf(
                ShayariEntity(
                    id = 801,
                    couplet = "इश्क़ में तेरे हर शाम चाँदनी लगती है,\nमोहब्बत की दुनिया कितनी प्यारी लगती है।",
                    poet = "मिर्ज़ा ग़ालिब",
                    categoryId = ShayariCategory.MOHABBAT.id,
                    moodTag = "मोहब्बत, love, ishq shayri"
                ),
                ShayariEntity(
                    id = 802,
                    couplet = "बेवफ़ाई का दर्द आँसुओं में बह गया,\nज़िंदगी का सबसे बड़ा सबक मिल गया।",
                    poet = "जौन एलिया",
                    categoryId = ShayariCategory.BEWAFAI.id,
                    moodTag = "बेवफ़ाई, broken heart"
                ),
                ShayariEntity(
                    id = 803,
                    couplet = "संता ने बंता से पूछा इतना क्यों हँस रहे हो,\nबोला आज़ादी से जीने का मज़ा ही अलग है।",
                    poet = "कॉमेडी मंच",
                    categoryId = ShayariCategory.SANTA_BANTA.id,
                    moodTag = "चुटकुले"
                )
            )
            runBlocking { db.shayariDao().insertAll(catalog) }
            val vm = ShayariViewModel(
                repository = ShayariRepository(db.shayariDao()),
                preferencesRepository = UserPreferencesRepository(context),
                autoSeed = false
            )

            fun searchAndWait(query: String, check: (List<ShayariEntity>) -> Boolean) {
                vm.onSearchQueryChanged(query)
                composeRule.waitUntil(timeoutMillis = 5_000L) {
                    ShadowLooper.idleMainLooper()
                    val s = vm.uiState.value
                    s.searchQuery == query && check(s.filteredShayari)
                }
            }

            // Even after selecting a different category, searching finds global matches
            vm.onCategorySelected(ShayariCategory.SANTA_BANTA.id)
            searchAndWait("love shayari") { list -> list.any { it.id == 801 } }
            searchAndWait("romantic") { list -> list.any { it.id == 801 } }
            searchAndWait("moon") { list -> list.any { it.id == 801 } }
            searchAndWait("galib") { list -> list.any { it.id == 801 } }
            searchAndWait("बेवफाई") { list -> list.any { it.id == 802 } } // without nukta
            searchAndWait("जिंदगी") { list -> list.any { it.id == 802 } } // without nukta
            searchAndWait("#breakup") { list -> list.any { it.id == 802 } }
            searchAndWait("funny jokes") { list -> list.any { it.id == 803 } }
        } finally {
            db.close()
        }
    }

    @Test
    fun `ShayariWriteStudioScreen auto populates related bilingual tags when Mohabbat category is selected`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val personalVm = PersonalShayariViewModel(
                repository = LocalPersonalShayariRepository(db.localPersonalShayariDao()),
                currentUserId = "local_guest"
            )

            composeRule.setContent {
                ShayariTheme {
                    ShayariWriteStudioScreen(
                        viewModel = personalVm,
                        authorName = "Prince",
                        isOnlineAccount = false,
                        onRequestGoogleSignIn = {},
                        onClose = {},
                        onSaved = {},
                        onFeedback = {}
                    )
                }
            }

            composeRule.waitForIdle()
            composeRule.onNodeWithTag("studio_category_and_tags_card").performScrollTo()
            composeRule.onNodeWithTag("studio_primary_category_row").performScrollToKey(ShayariCategory.MOHABBAT.id)
            composeRule.onNodeWithTag("studio_primary_cat_mohabbat").performClick()
            composeRule.waitForIdle()

            val activeTags = ShayariTagMatcher.parseTags(personalVm.uiState.value.themeTagInput)
            assertTrue(activeTags.contains("मोहब्बत"))
            assertTrue(activeTags.contains("romantic शायरी"))
            assertTrue(activeTags.contains("ishq shayri"))
            assertTrue(activeTags.contains("pyar"))
            assertTrue(activeTags.contains("love"))
        } finally {
            db.close()
        }
    }
}
