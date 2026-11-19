package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariComment
import com.example.data.model.ShayariEntity
import com.example.ui.components.FloatingScrollButtons
import com.example.ui.components.ShayariCardItem
import com.example.ui.screens.CommentTarget
import com.example.ui.screens.ShayariCommentsSheet
import com.example.ui.theme.ShayariTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ShayariCardItemTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleShayari = ShayariEntity(
        id = 7,
        couplet = "इश्क़ ने 'ग़ालिब' निकम्मा कर दिया,\nवरना हम भी आदमी थे काम के।",
        poet = "मिर्ज़ा ग़ालिब",
        categoryId = ShayariCategory.MOHABBAT.id,
        moodTag = "प्यार"
    )

    @Test
    fun `ShayariCardItem hides copy button and triggers Like Comment Share actions`() {
        var liked = false
        var commented = false
        var shared = false

        composeRule.setContent {
            ShayariTheme {
                ShayariCardItem(
                    shayari = sampleShayari,
                    commentCount = 3,
                    onOpenReader = {},
                    onToggleFavorite = { liked = true },
                    onComment = { commented = true },
                    onShare = { shared = true }
                )
            }
        }

        composeRule.waitForIdle()

        // Copy button is removed
        composeRule.onNodeWithTag("copy_button_${sampleShayari.id}").assertDoesNotExist()

        // Like, Comment (with count), and Share are displayed and interactive
        composeRule.onNodeWithTag("favorite_button_${sampleShayari.id}").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("comment_button_${sampleShayari.id}").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("3").assertIsDisplayed()
        composeRule.onNodeWithTag("share_button_${sampleShayari.id}").assertIsDisplayed().performClick()

        assertTrue(liked)
        assertTrue(commented)
        assertTrue(shared)
    }

    @Test
    fun `ShayariCardItem displays unobstructed joke illustration banner for comedy categories`() {
        val joke = ShayariEntity(
            id = 88,
            couplet = "मुल्ला नसरुद्दीन नदी किनारे पानी में दही मिला रहे थे...",
            poet = "मुल्ला नसरुद्दीन",
            categoryId = ShayariCategory.MULLA_NASRUDDIN.id,
            moodTag = "हाज़िरजवाबी"
        )

        composeRule.setContent {
            ShayariTheme {
                ShayariCardItem(
                    shayari = joke,
                    onOpenReader = {},
                    onToggleFavorite = {},
                    onComment = {},
                    onShare = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("joke_banner_image_${joke.id}", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(joke.couplet).assertIsDisplayed()
    }

    @Test
    fun `ShayariCategory backgroundFor matches joke categories and shayari verse themes`() {
        assertEquals(
            com.example.R.drawable.img_joke_mulla_nasruddin,
            ShayariCategory.MULLA_NASRUDDIN.backgroundFor(
                ShayariEntity(1, "मुल्ला जी बाजार में गए", "मुल्ला नसरुद्दीन", ShayariCategory.MULLA_NASRUDDIN.id, "मज़ाक")
            )
        )
        assertEquals(
            com.example.R.drawable.img_joke_santa_banta,
            ShayariCategory.SANTA_BANTA.backgroundFor(
                ShayariEntity(2, "संता और बंता चाय पी रहे थे", "संता-बंता", ShayariCategory.SANTA_BANTA.id, "हँसी")
            )
        )
        assertEquals(
            com.example.R.drawable.img_bg_sharabi,
            ShayariCategory.ZINDAGI.backgroundFor(
                ShayariEntity(3, "साक़ी ने जब से मयख़ाना खोला है, जाम में हर दर्द घुल गया है", "_ शायरी", ShayariCategory.ZINDAGI.id, "साक़ी")
            )
        )
        assertEquals(
            com.example.R.drawable.img_bg_maut,
            ShayariCategory.MOHABBAT.backgroundFor(
                ShayariEntity(4, "मेरे जनाज़े पर भी कफ़न उठाकर न देखना सनम", "_ शायरी", ShayariCategory.MOHABBAT.id, "जनाज़ा")
            )
        )
    }

    @Test
    fun `ShayariCommentsSheet shows Google sign in gate when signed out`() {
        var signInTriggered = false

        composeRule.setContent {
            ShayariTheme {
                ShayariCommentsSheet(
                    target = CommentTarget(
                        shayariKey = "seed_7",
                        couplet = sampleShayari.couplet,
                        poet = sampleShayari.poet
                    ),
                    currentUserId = null,
                    comments = emptyList(),
                    isLoading = false,
                    isSubmitting = false,
                    errorMessage = null,
                    onAddComment = {},
                    onDeleteComment = {},
                    onAuthSuccess = {},
                    onAuthError = {},
                    onDismiss = {},
                    onSignInClickOverride = { signInTriggered = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("comments_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("comments_auth_gate").assertIsDisplayed()
        composeRule.onNodeWithTag("comment_input").assertDoesNotExist()
        composeRule.onNodeWithTag("comments_google_sign_in_button").performClick()

        assertTrue(signInTriggered)
    }

    @Test
    fun `ShayariCommentsSheet lists comments allows adding and owner-only deleting when signed in`() {
        var addedText: String? = null
        var deletedId: String? = null

        val ownComment = ShayariComment(
            id = "c1",
            shayariKey = "seed_7",
            userId = "user_alice",
            authorName = "अमन",
            text = "लाजवाब शायरी!"
        )
        val otherComment = ShayariComment(
            id = "c2",
            shayariKey = "seed_7",
            userId = "user_bob",
            authorName = "सहर",
            text = "बहुत बढ़िया!"
        )

        composeRule.setContent {
            ShayariTheme {
                ShayariCommentsSheet(
                    target = CommentTarget(
                        shayariKey = "seed_7",
                        couplet = sampleShayari.couplet,
                        poet = sampleShayari.poet
                    ),
                    currentUserId = "user_alice",
                    comments = listOf(ownComment, otherComment),
                    isLoading = false,
                    isSubmitting = false,
                    errorMessage = null,
                    onAddComment = { addedText = it },
                    onDeleteComment = { deletedId = it },
                    onAuthSuccess = {},
                    onAuthError = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("लाजवाब शायरी!").assertIsDisplayed()
        composeRule.onNodeWithText("बहुत बढ़िया!").assertIsDisplayed()

        // Delete shown only for owner
        composeRule.onNodeWithTag("delete_comment_button_c1").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("delete_comment_button_c2").assertDoesNotExist()
        assertEquals("c1", deletedId)

        // Add comment
        composeRule.onNodeWithTag("comment_input").performTextInput("दिल खुश हो गया")
        composeRule.onNodeWithTag("comment_send_button").performClick()
        assertEquals("दिल खुश हो गया", addedText)
    }

    @Test
    fun `FloatingScrollButtons scroll down and scroll back to top`() {
        lateinit var listStateRef: androidx.compose.foundation.lazy.LazyListState

        composeRule.setContent {
            ShayariTheme {
                val listState = rememberLazyListState()
                listStateRef = listState
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(30) { idx ->
                            Text(
                                text = "Item $idx",
                                modifier = Modifier.height(120.dp)
                            )
                        }
                    }
                    FloatingScrollButtons(listState = listState)
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scroll_up_button").assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithTag("scroll_down_button").assertIsDisplayed().assertIsEnabled().performClick()
        composeRule.mainClock.advanceTimeBy(1200L)
        composeRule.waitForIdle()

        assertFalse(listStateRef.canScrollForward)
        assertTrue(listStateRef.firstVisibleItemIndex >= 20)
        composeRule.onNodeWithTag("scroll_up_button").assertIsEnabled().performClick()
        composeRule.mainClock.advanceTimeBy(1200L)
        composeRule.waitForIdle()

        assertEquals(0, listStateRef.firstVisibleItemIndex)
        assertEquals(0, listStateRef.firstVisibleItemScrollOffset)
    }

    @Test
    fun `MehfilScreen top title click triggers onRefreshApp`() {
        var refreshCount = 0

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.MehfilScreen(
                    uiState = com.example.ui.viewmodel.ShayariUiState(
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
                    onShare = {},
                    onRefreshApp = { refreshCount++ }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("header_title_refresh_button").assertIsDisplayed().performClick()
        assertEquals(1, refreshCount)
    }

    @Test
    fun `MehfilScreen top right categories button is icon-only and triggers navigation`() {
        var openedCategories = false

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.MehfilScreen(
                    uiState = com.example.ui.viewmodel.ShayariUiState(
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
                    onShare = {},
                    onOpenCategories = { openedCategories = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("कैटेगरी").assertDoesNotExist()
        composeRule.onNodeWithTag("top_categories_button").assertIsDisplayed().performClick()
        assertTrue(openedCategories)
    }

    @Test
    fun `CategoriesScreen hides counts for non-owners and shows emoji and count for owner`() {
        var clickedCategory: ShayariCategory? = null

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.CategoriesScreen(
                    categoryCounts = mapOf(
                        ShayariCategory.MOHABBAT.id to 42,
                        ShayariCategory.ATTITUDE.id to 30
                    ),
                    onCategoryClick = { clickedCategory = it },
                    isOwner = true
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_grid_card_mohabbat").assertIsDisplayed()
        composeRule.onNodeWithTag("category_grid_card_adultery").assertIsDisplayed()
        composeRule.onNodeWithText("❤️").assertIsDisplayed()
        composeRule.onNodeWithText("42").assertIsDisplayed()
        composeRule.onNodeWithTag("category_grid_card_attitude").assertIsDisplayed().performClick()
        assertEquals(ShayariCategory.ATTITUDE, clickedCategory)
    }

    @Test
    fun `CategoriesScreen omits shayari count badges and total count when isOwner is false`() {
        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.CategoriesScreen(
                    categoryCounts = mapOf(
                        ShayariCategory.MOHABBAT.id to 42,
                        ShayariCategory.ATTITUDE.id to 30
                    ),
                    onCategoryClick = {},
                    isOwner = false
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_grid_card_mohabbat").assertIsDisplayed()
        composeRule.onNodeWithTag("category_count_badge_mohabbat").assertDoesNotExist()
        composeRule.onNodeWithText("42").assertDoesNotExist()
        composeRule.onNodeWithText("72 शायरी", substring = true).assertDoesNotExist()
    }

    @Test
    fun `MehfilScreen hides shayari counts for non-owners and shows them only to owner`() {
        val sampleCounts = mapOf(ShayariCategory.MOHABBAT.id to 1315)
        val state = com.example.ui.viewmodel.ShayariUiState(
            isLoading = false,
            allShayari = listOf(sampleShayari),
            filteredShayari = listOf(sampleShayari),
            featuredShayari = sampleShayari,
            categoryCounts = sampleCounts
        )

        composeRule.setContent {
            ShayariTheme {
                androidx.compose.foundation.layout.Column {
                    com.example.ui.screens.MehfilScreen(
                        uiState = state,
                        isDarkTheme = false,
                        onToggleTheme = {},
                        onSearchQueryChanged = {},
                        onCategorySelected = {},
                        onOpenReader = { _, _ -> },
                        onToggleFavorite = {},
                        onComment = {},
                        onShare = {},
                        isOwner = false
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("सभी ✨ (1315)").assertDoesNotExist()
        composeRule.onNodeWithText("सभी • 1 शायरी").assertDoesNotExist()
        composeRule.onNodeWithText("सभी ✨").assertIsDisplayed()
        composeRule.onNodeWithText("सभी").assertIsDisplayed()
    }

    @Test
    fun `MehfilScreen displays shayari counts when isOwner is true`() {
        val sampleCounts = mapOf(ShayariCategory.MOHABBAT.id to 1315)
        val state = com.example.ui.viewmodel.ShayariUiState(
            isLoading = false,
            allShayari = listOf(sampleShayari),
            filteredShayari = listOf(sampleShayari),
            featuredShayari = sampleShayari,
            categoryCounts = sampleCounts
        )

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.MehfilScreen(
                    uiState = state,
                    isDarkTheme = false,
                    onToggleTheme = {},
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onOpenReader = { _, _ -> },
                    onToggleFavorite = {},
                    onComment = {},
                    onShare = {},
                    isOwner = true
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("सभी ✨ (1315)").assertIsDisplayed()
        composeRule.onNodeWithText("सभी • 1 शायरी").assertIsDisplayed()
    }

    @Test
    fun `isOwnerEmail grants access only to configured owner email`() {
        assertTrue(com.example.auth.isOwnerEmail("princeorviel@gmail.com"))
        assertTrue(com.example.auth.isOwnerEmail("  PrinceOrviel@Gmail.com "))
        assertFalse(com.example.auth.isOwnerEmail("reader@gmail.com"))
        assertFalse(com.example.auth.isOwnerEmail(null))
        assertFalse(com.example.auth.isAppOwner(null))
    }

    @Test
    fun `AccountProfileHeader shows poet identity post count without split tabs and triggers sign out`() {
        var signedOut = false

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.AccountProfileHeader(
                    photoUrl = null,
                    displayName = "राहत साहब",
                    email = "rahat@example.com",
                    postCount = 6,
                    onSignOut = { signedOut = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("account_profile_avatar").assertIsDisplayed()
        composeRule.onNodeWithText("राहत साहब").assertIsDisplayed()
        composeRule.onNodeWithText("🆔 राहत साहब").assertDoesNotExist()
        composeRule.onNodeWithText("rahat@example.com").assertIsDisplayed()
        composeRule.onNodeWithTag("account_stat_posts").assertIsDisplayed()
        composeRule.onNodeWithText("मेरी पोस्ट: 6").assertIsDisplayed()
        composeRule.onNodeWithTag("account_tab_personal").assertDoesNotExist()
        composeRule.onNodeWithTag("account_tab_kavi_manch").assertDoesNotExist()

        composeRule.onNodeWithTag("sign_out_button").performClick()
        assertTrue(signedOut)
    }

    @Test
    fun `ShayariBottomBar renders 4 options without search or bell and triggers callbacks`() {
        var homeClicks = 0
        var favoritesClicks = 0
        var createClicks = 0
        var accountClicks = 0

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.components.ShayariBottomBar(
                    isHomeSelected = true,
                    isFavoritesSelected = false,
                    isAccountSelected = false,
                    currentUserPhotoUrl = null,
                    currentUserDisplayName = null,
                    isSignedIn = false,
                    onHomeClick = { homeClicks++ },
                    onFavoritesClick = { favoritesClicks++ },
                    onCreateClick = { createClicks++ },
                    onAccountClick = { accountClicks++ }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_search").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_notifications").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_mehfil").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("nav_tab_favorites").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("nav_tab_create").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("nav_tab_account").assertIsDisplayed().performClick()

        assertEquals(1, homeClicks)
        assertEquals(1, favoritesClicks)
        assertEquals(1, createClicks)
        assertEquals(1, accountClicks)
    }

    @Test
    fun `ShayariBottomBar displays Favourites label when selected`() {
        composeRule.setContent {
            ShayariTheme {
                com.example.ui.components.ShayariBottomBar(
                    isHomeSelected = false,
                    isFavoritesSelected = true,
                    isAccountSelected = false,
                    currentUserPhotoUrl = null,
                    currentUserDisplayName = null,
                    isSignedIn = false,
                    onHomeClick = {},
                    onFavoritesClick = {},
                    onCreateClick = {},
                    onAccountClick = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Favourites").assertIsDisplayed()
        composeRule.onNodeWithText("पसंद").assertDoesNotExist()
    }

    @Test
    fun `ShayariBottomBar displays My account label without ID emoji when selected`() {
        composeRule.setContent {
            ShayariTheme {
                com.example.ui.components.ShayariBottomBar(
                    isHomeSelected = false,
                    isFavoritesSelected = false,
                    isAccountSelected = true,
                    currentUserPhotoUrl = null,
                    currentUserDisplayName = null,
                    isSignedIn = false,
                    onHomeClick = {},
                    onFavoritesClick = {},
                    onCreateClick = {},
                    onAccountClick = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("My account").assertIsDisplayed()
        composeRule.onNodeWithText("🆔", substring = true).assertDoesNotExist()
    }

    @Test
    fun `MehfilScreen top header hosts search and notification buttons`() {
        var searchToggled = false
        var notificationsOpened = false

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.MehfilScreen(
                    uiState = com.example.ui.viewmodel.ShayariUiState(
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
                    onShare = {},
                    onToggleSearch = { searchToggled = true },
                    onOpenNotificationSettings = { notificationsOpened = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_toggle_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("notification_settings_button").assertIsDisplayed().performClick()
        assertTrue(searchToggled)
        assertTrue(notificationsOpened)
    }

    @Test
    fun `PostNotificationsSheet shows Google sign in gate when signed out and omits daily time controls`() {
        var signInTriggered = false

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.PostNotificationsSheet(
                    currentUserId = null,
                    notifications = emptyList(),
                    isLoading = false,
                    errorMessage = null,
                    onNotificationClick = {},
                    onMarkAllRead = {},
                    onDeleteNotification = {},
                    onAuthSuccess = {},
                    onAuthError = {},
                    onDismiss = {},
                    onSignInClickOverride = { signInTriggered = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("post_notifications_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("notifications_auth_gate").assertIsDisplayed()
        composeRule.onNodeWithText("सुबह", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("टाइम बदलें", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("अभी टेस्ट करके देखें", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("notifications_google_sign_in_button").performClick()

        assertTrue(signInTriggered)
    }

    @Test
    fun `PostNotificationsSheet displays like comment share items and triggers mark all read click and delete`() {
        var clickedId: String? = null
        var markedAllRead = false
        var deletedId: String? = null

        val items = listOf(
            com.example.data.model.PostNotification(
                id = "n_like",
                recipientUserId = "author_1",
                actorUserId = "reader_1",
                actorName = "राहुल",
                type = com.example.data.model.PostNotificationType.LIKE.value,
                postId = "p1",
                postExcerpt = "चाँद भी शर्मा गया...",
                isRead = false
            ),
            com.example.data.model.PostNotification(
                id = "n_comment",
                recipientUserId = "author_1",
                actorUserId = "reader_2",
                actorName = "सहर",
                type = com.example.data.model.PostNotificationType.COMMENT.value,
                postId = "p1",
                postExcerpt = "चाँद भी शर्मा गया...",
                commentText = "वाह बहुत उम्दा!",
                isRead = false
            ),
            com.example.data.model.PostNotification(
                id = "n_share",
                recipientUserId = "author_1",
                actorUserId = "reader_3",
                actorName = "कबीर",
                type = com.example.data.model.PostNotificationType.SHARE.value,
                postId = "p1",
                postExcerpt = "चाँद भी शर्मा गया...",
                isRead = true
            )
        )

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.PostNotificationsSheet(
                    currentUserId = "author_1",
                    notifications = items,
                    isLoading = false,
                    errorMessage = null,
                    onNotificationClick = { clickedId = it.id },
                    onMarkAllRead = { markedAllRead = true },
                    onDeleteNotification = { deletedId = it },
                    onAuthSuccess = {},
                    onAuthError = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notifications_list").assertIsDisplayed()
        composeRule.onNodeWithTag("notification_unread_dot_n_like", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("notification_unread_dot_n_comment", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("notification_unread_dot_n_share", useUnmergedTree = true).assertDoesNotExist()

        composeRule.onNodeWithText("“वाह बहुत उम्दा!”").assertIsDisplayed()
        composeRule.onNodeWithTag("mark_all_notifications_read_button").assertIsDisplayed().performClick()
        assertTrue(markedAllRead)

        composeRule.onNodeWithTag("notification_item_n_like").performClick()
        assertEquals("n_like", clickedId)

        composeRule.onNodeWithTag("delete_notification_button_n_share").performClick()
        assertEquals("n_share", deletedId)
    }

    @Test
    fun `MehfilScreen shows unread notification badge count when greater than zero`() {
        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.MehfilScreen(
                    uiState = com.example.ui.viewmodel.ShayariUiState(
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
                    onShare = {},
                    unreadNotificationCount = 4
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notification_unread_badge", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("4").assertIsDisplayed()
    }

    @Test
    fun `FeaturedHeroCard hides shuffle and full screen buttons and opens reader when card is tapped`() {
        var openedShayariId: Int? = null

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.MehfilScreen(
                    uiState = com.example.ui.viewmodel.ShayariUiState(
                        isLoading = false,
                        allShayari = listOf(sampleShayari),
                        filteredShayari = listOf(sampleShayari),
                        featuredShayari = sampleShayari,
                        popularShayari = listOf(sampleShayari)
                    ),
                    isDarkTheme = false,
                    onToggleTheme = {},
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onOpenReader = { shayari, _ -> openedShayariId = shayari.id },
                    onToggleFavorite = {},
                    onComment = {},
                    onShare = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("featured_open_studio_button").assertDoesNotExist()
        composeRule.onNodeWithText("फ़ुल स्क्रीन में पढ़ें").assertDoesNotExist()
        composeRule.onNodeWithTag("shuffle_shayari_button").assertDoesNotExist()
        composeRule.onNodeWithText("कोई भी शायरी दिखाएँ").assertDoesNotExist()

        composeRule.onNodeWithTag("featured_hero_card").assertIsDisplayed().performClick()
        assertEquals(sampleShayari.id, openedShayariId)
    }

    @Test
    fun `FeaturedHeroCard dynamically rotates through popularShayari automatically`() {
        val secondPopular = ShayariEntity(
            id = 88,
            couplet = "खुदी को कर बुलंद इतना कि हर तक़दीर से पहले,\nखुदा बंदे से खुद पूछे बता तेरी रज़ा क्या है।",
            poet = "अल्लामा इक़बाल",
            categoryId = ShayariCategory.KHUDA_RAB.id,
            moodTag = "जज़्बा",
            isFeatured = true
        )
        var openedId: Int? = null

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.MehfilScreen(
                    uiState = com.example.ui.viewmodel.ShayariUiState(
                        isLoading = false,
                        allShayari = listOf(sampleShayari, secondPopular),
                        filteredShayari = emptyList(),
                        featuredShayari = sampleShayari,
                        popularShayari = listOf(sampleShayari, secondPopular)
                    ),
                    isDarkTheme = false,
                    onToggleTheme = {},
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onOpenReader = { shayari, _ -> openedId = shayari.id },
                    onToggleFavorite = {},
                    onComment = {},
                    onShare = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("— मिर्ज़ा ग़ालिब").assertIsDisplayed()

        // Advance past the 5s auto-rotation interval
        composeRule.mainClock.advanceTimeBy(5600L)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("— अल्लामा इक़बाल").assertIsDisplayed()
        composeRule.onNodeWithTag("featured_hero_card").performClick()
        assertEquals(88, openedId)
    }

    @Test
    fun `AccountScreen allows guest users to launch Write Studio and displays saved shayari`() {
        val guestItems = kotlinx.coroutines.flow.MutableStateFlow(
            listOf(
                com.example.data.model.PersonalShayari(
                    id = "local_1",
                    userId = com.example.data.model.LOCAL_GUEST_USER_ID,
                    title = "सफर की बात",
                    themeTag = "सुकून",
                    couplet = "रास्ते खुद ब खुद बनते जाते हैं,\nजब इरादे दिल से सच्चे होते हैं।"
                )
            )
        )
        val inMemoryStore = object : com.example.data.remote.PersonalShayariStore {
            override fun observePersonalShayari(userId: String) = guestItems
            override suspend fun savePersonalShayari(title: String, themeTag: String, couplet: String) =
                Result.success("local_new")
            override suspend fun updatePersonalShayari(shayariId: String, title: String, themeTag: String, couplet: String) =
                Result.success(Unit)
            override suspend fun deletePersonalShayari(shayariId: String) = Result.success(Unit)
        }
        val personalVm = com.example.ui.viewmodel.PersonalShayariViewModel(
            repository = inMemoryStore,
            currentUserId = com.example.data.model.LOCAL_GUEST_USER_ID
        )
        var signInPrompted = false
        var studioOpened = false

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.screens.AccountScreen(
                    currentUser = null,
                    personalViewModel = personalVm,
                    communityViewModel = null,
                    onSignOut = {},
                    onOpenPersonalCard = {},
                    onCopyPersonal = {},
                    onSharePersonal = {},
                    onFeedback = {},
                    onOpenWriteStudio = { studioOpened = true },
                    signInClickOverride = { signInPrompted = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("अतिथि शायर").assertIsDisplayed()
        composeRule.onNodeWithTag("account_tab_personal").assertDoesNotExist()
        composeRule.onNodeWithTag("account_tab_kavi_manch").assertDoesNotExist()
        composeRule.onNodeWithTag("account_guest_sign_in_button").assertIsDisplayed()
        composeRule.onNodeWithText("मेरी पोस्ट: 1").assertIsDisplayed()
        composeRule.onNodeWithTag("personal_shayari_list").performScrollToIndex(2)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("सफर की बात").assertIsDisplayed()
        composeRule.onNodeWithText("फ़ोन में सेव").assertIsDisplayed()

        composeRule.onNodeWithTag("studio_launcher_write_button").performScrollTo().performClick()
        assertTrue(studioOpened)

        composeRule.onNodeWithTag("account_guest_sign_in_button").performClick()
        assertTrue(signInPrompted)
    }

    @Test
    fun `ShayariWriteStudioScreen writes without mandatory title or tag transitions to Design and saves`() {
        val guestItems = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.data.model.PersonalShayari>>(emptyList())
        val inMemoryStore = object : com.example.data.remote.PersonalShayariStore {
            override fun observePersonalShayari(userId: String) = guestItems
            override suspend fun savePersonalShayari(
                title: String,
                themeTag: String,
                couplet: String
            ): Result<String> {
                val created = com.example.data.model.PersonalShayari(
                    id = "local_${guestItems.value.size + 1}",
                    userId = com.example.data.model.LOCAL_GUEST_USER_ID,
                    title = title,
                    themeTag = themeTag,
                    couplet = couplet
                )
                guestItems.value = listOf(created) + guestItems.value
                return Result.success(created.id)
            }

            override suspend fun updatePersonalShayari(shayariId: String, title: String, themeTag: String, couplet: String) =
                Result.success(Unit)

            override suspend fun deletePersonalShayari(shayariId: String) = Result.success(Unit)
        }
        val personalVm = com.example.ui.viewmodel.PersonalShayariViewModel(
            repository = inMemoryStore,
            currentUserId = com.example.data.model.LOCAL_GUEST_USER_ID
        )
        var savedTriggered = false

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.studio.ShayariWriteStudioScreen(
                    viewModel = personalVm,
                    authorName = "Prince Orviel",
                    onClose = {},
                    onSaved = { savedTriggered = true },
                    onFeedback = {}
                )
            }
        }

        composeRule.waitForIdle()
        // Minimal clean canvas — next button disabled until text entered
        composeRule.onNodeWithTag("studio_next_design_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("personal_couplet_input")
            .performTextInput("कमबख़्त ये इश्क़ ने नकारा कर दिया,\nवरना हम भी इंसान थे काम के")
        composeRule.onNodeWithTag("studio_next_design_button").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        // Design step (YourQuote-style): live preview, Wallpaper & Text tabs
        composeRule.onNodeWithTag("studio_live_card_preview").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_tab_wallpaper").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_wallpaper_search").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_pick_gallery_button").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_wallpaper_paper_ivory").performClick()

        // Switch to TEXT tab & customize
        composeRule.onNodeWithTag("studio_tab_text").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("studio_halign_CENTER").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_valign_TOP").assertIsDisplayed().performClick()

        // Save directly from Design Studio without needing any title/tag
        composeRule.onNodeWithTag("studio_save_post_button").performClick()
        composeRule.waitForIdle()

        assertTrue(savedTriggered)
        assertEquals(1, guestItems.value.size)
        assertTrue(guestItems.value.first().couplet.contains("कमबख़्त ये इश्क़"))
    }

    @Test
    fun `ShareShayariSheet offers text and image share modes and triggers callbacks`() {
        var textShared = false
        var imageShared = false
        var dismissed = false

        composeRule.setContent {
            ShayariTheme {
                com.example.ui.components.ShareShayariSheet(
                    target = com.example.ui.components.ShareTarget.Catalog(sampleShayari),
                    isPreparingImage = false,
                    onShareText = { textShared = true },
                    onShareImage = { imageShared = true },
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("share_options_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("share_option_text").assertIsDisplayed().performClick()
        assertTrue(textShared)

        composeRule.onNodeWithTag("share_option_image").assertIsDisplayed().performClick()
        assertTrue(imageShared)

        composeRule.onNodeWithTag("share_sheet_close_button").assertIsDisplayed().performClick()
        assertTrue(dismissed)
    }

    @Test
    fun `MehfilScreen and AccountProfileHeader expose App Share buttons and trigger callbacks`() {
        var mehfilShareAppClicks = 0
        var accountShareAppClicks = 0

        composeRule.setContent {
            ShayariTheme {
                androidx.compose.foundation.layout.Column {
                    com.example.ui.screens.AccountProfileHeader(
                        photoUrl = null,
                        displayName = "राहत साहब",
                        email = "rahat@example.com",
                        postCount = 2,
                        onSignOut = {},
                        onShareApp = { accountShareAppClicks++ }
                    )
                    com.example.ui.screens.MehfilScreen(
                        uiState = com.example.ui.viewmodel.ShayariUiState(
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
                        onShare = {},
                        onShareApp = { mehfilShareAppClicks++ }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("account_share_app_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("mehfil_share_app_button").assertIsDisplayed().performClick()

        assertEquals(1, accountShareAppClicks)
        assertEquals(1, mehfilShareAppClicks)
    }
}

