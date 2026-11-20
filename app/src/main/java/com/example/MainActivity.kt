package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ai.configureAppCheck
import com.example.auth.attemptAutoSignIn
import com.example.auth.isAppOwner
import com.example.auth.isFirebaseAvailable
import com.example.auth.isLocalOwnerAccount
import com.example.auth.safeAuthStateFlow
import com.example.auth.safeCurrentUser
import com.example.auth.signOutFromGoogle
import com.example.data.local.LocalPersonalShayariRepository
import com.example.data.local.LoginLocalResult
import com.example.data.local.ShayariDatabase
import com.example.data.local.ShayariRepository
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.CommunityShayari
import com.example.data.model.LOCAL_GUEST_USER_ID
import com.example.data.model.PersonalShayari
import com.example.data.model.PoetProfile
import com.example.data.model.PostNotification
import com.example.data.model.PostNotificationType
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariComment
import com.example.data.model.ShayariEntity
import com.example.data.model.ThemeModeOption
import com.example.data.model.shayariCommentKey
import com.example.data.model.toShayariEntity
import com.example.data.remote.AdminDashboardRepository
import com.example.data.remote.CommunityShayariRepository
import com.example.data.remote.PersonalShayariRepository
import com.example.data.remote.PostNotificationsRepository
import com.example.data.remote.ShayariCommentsRepository
import com.example.notifications.DailyShayariNotificationHelper
import com.example.notifications.DailyShayariScheduler
import com.example.ui.components.AccountAvatar
import com.example.ui.components.ProfileManagementSheet
import com.example.ui.components.ShareContentMode
import com.example.ui.components.ShareShayariSheet
import com.example.ui.components.ShareTarget
import com.example.ui.components.ShayariBottomBar
import com.example.ui.components.toCardEntity
import com.example.ui.components.toCustomBadgeLabel
import com.example.ui.components.toPreviewTitle
import com.example.ui.components.toShareText
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CategoryDetailScreen
import com.example.ui.screens.CommentTarget
import com.example.ui.screens.EasyProfileEditorScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.KaviManchAuthGate
import com.example.ui.screens.KaviManchScreen
import com.example.ui.screens.MehfilScreen
import com.example.ui.screens.PoetProfileScreen
import com.example.ui.screens.PostNotificationsSheet
import com.example.ui.screens.ShayariCommentsSheet
import com.example.ui.screens.ShayariReaderScreen
import com.example.ui.theme.ShayariReadingStyle
import com.example.ui.theme.ShayariTheme
import com.example.ui.viewmodel.AdminDashboardUiState
import com.example.ui.viewmodel.AdminDashboardViewModel
import com.example.ui.viewmodel.CommunityFeedState
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.PersonalShayariViewModel
import com.example.ui.viewmodel.ShayariViewModel
import com.example.util.ProfilePhotoStorage
import com.example.util.ShayariShareHelper
import com.example.util.SocialShareLauncher
import com.example.util.SocialSharePlatform
import com.example.util.SocialShareResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

const val ROUTE_MEHFIL = "mehfil"
const val ROUTE_CATEGORIES = "categories"
const val ROUTE_CATEGORY_DETAIL = "category_detail/{categoryId}"
const val ROUTE_FAVORITES = "favorites"
const val ROUTE_ACCOUNT = "account"
const val ROUTE_EDIT_PROFILE = "edit_profile"
const val ROUTE_AUTH = "auth"
const val ROUTE_ADMIN_DASHBOARD = "admin_dashboard"
const val ROUTE_KAVI_MANCH = "kavi_manch"
const val ROUTE_POET_PROFILE = "poet_profile/{poetUserId}"
const val ROUTE_WRITE_STUDIO = "write_studio"
const val ROUTE_READER = "reader/{shayariId}/{scope}"

private enum class BottomTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    MEHFIL(
        route = ROUTE_MEHFIL,
        labelRes = R.string.nav_mehfil,
        selectedIcon = Icons.Rounded.AutoStories,
        unselectedIcon = Icons.Outlined.AutoStories,
        testTag = "nav_tab_mehfil"
    ),
    FAVORITES(
        route = ROUTE_FAVORITES,
        labelRes = R.string.nav_favorites,
        selectedIcon = Icons.Rounded.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        testTag = "nav_tab_favorites"
    ),
    ACCOUNT(
        route = ROUTE_ACCOUNT,
        labelRes = R.string.nav_account,
        selectedIcon = Icons.Rounded.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle,
        testTag = "nav_tab_account"
    )
}

class MainActivity : ComponentActivity() {
    private var pendingNotificationShayariId by mutableStateOf<Int?>(null)
    private var pendingDeepLinkTarget by mutableStateOf<ShayariShareHelper.DeepLinkTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        val database = ShayariDatabase.getInstance(applicationContext)
        val repository = ShayariRepository(database.shayariDao())
        val preferencesRepository = UserPreferencesRepository(applicationContext)

        setContent {
            val viewModel: ShayariViewModel = viewModel(
                factory = ShayariViewModel.Factory(repository, preferencesRepository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (uiState.preferences.themeMode) {
                ThemeModeOption.SYSTEM -> systemDark
                ThemeModeOption.LIGHT -> false
                ThemeModeOption.DARK -> true
            }

            val readingStyle = ShayariReadingStyle(
                fontScale = uiState.preferences.fontScale,
                fontStyle = uiState.preferences.fontStyle
            )

            ShayariTheme(
                darkTheme = isDarkTheme,
                readingStyle = readingStyle
            ) {
                ShayariAppRoot(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    systemDark = systemDark,
                    pendingNotificationShayariId = pendingNotificationShayariId,
                    onConsumeNotificationShayariId = { pendingNotificationShayariId = null },
                    pendingDeepLinkTarget = pendingDeepLinkTarget,
                    onConsumeDeepLinkTarget = { pendingDeepLinkTarget = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val notifId = extractNotificationShayariId(intent)
        if (notifId != null) {
            pendingNotificationShayariId = notifId
        }
        val deepLink = ShayariShareHelper.parseDeepLink(intent?.data)
        if (deepLink != null) {
            pendingDeepLinkTarget = deepLink
        }
    }

    private fun extractNotificationShayariId(intent: Intent?): Int? {
        val id = intent?.getIntExtra(
            DailyShayariNotificationHelper.EXTRA_NOTIFICATION_SHAYARI_ID,
            -1
        ) ?: -1
        return if (id > 0) id else null
    }
}

@Composable
private fun ShayariAppRoot(
    viewModel: ShayariViewModel,
    isDarkTheme: Boolean,
    systemDark: Boolean,
    pendingNotificationShayariId: Int?,
    onConsumeNotificationShayariId: () -> Unit,
    pendingDeepLinkTarget: ShayariShareHelper.DeepLinkTarget? = null,
    onConsumeDeepLinkTarget: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var previewCommunityEntity by remember { mutableStateOf<ShayariEntity?>(null) }
    var previewCommunityPost by remember { mutableStateOf<CommunityShayari?>(null) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    var authScreenError by remember { mutableStateOf<String?>(null) }
    var homeRefreshTick by remember { mutableIntStateOf(0) }
    var isHomeSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchFocusTrigger by remember { mutableIntStateOf(0) }

    val credentialManager = remember(context) { CredentialManager.create(context) }
    val firebaseReady = remember(context) { isFirebaseAvailable(context) }
    val currentUser by remember(firebaseReady) {
        safeAuthStateFlow(context)
    }.collectAsStateWithLifecycle(
        initialValue = safeCurrentUser(context)
    )
    val isOwner = remember(
        currentUser?.email,
        uiState.preferences.localAccount,
        uiState.preferences.ownerStatsEnabled
    ) {
        uiState.preferences.ownerStatsEnabled ||
            isAppOwner(currentUser) ||
            isLocalOwnerAccount(uiState.preferences.localAccount)
    }
    val commentsRepository = remember(context, firebaseReady) {
        if (firebaseReady) ShayariCommentsRepository(context.applicationContext) else null
    }
    val notificationsRepository = remember(context, firebaseReady) {
        if (firebaseReady) PostNotificationsRepository(context.applicationContext) else null
    }
    val localPersonalRepository = remember(context) {
        LocalPersonalShayariRepository(
            ShayariDatabase.getInstance(context.applicationContext).localPersonalShayariDao()
        )
    }
    val activeUid = currentUser?.uid ?: LOCAL_GUEST_USER_ID
    val activePersonalRepository = remember(activeUid, firebaseReady, context) {
        if (currentUser == null || !firebaseReady) {
            localPersonalRepository
        } else {
            PersonalShayariRepository(context.applicationContext)
        }
    }
    val activePersonalViewModel: PersonalShayariViewModel = viewModel(
        key = "personal_$activeUid",
        factory = PersonalShayariViewModel.Factory(
            repository = activePersonalRepository,
            currentUserId = activeUid
        )
    )
    val sharedCommunityViewModel: CommunityViewModel? = currentUser?.let { signedInUser ->
        key(signedInUser.uid) {
            val communityRepository = remember(context) {
                CommunityShayariRepository(context.applicationContext)
            }
            viewModel(
                key = "community_${signedInUser.uid}",
                factory = CommunityViewModel.Factory(
                    repository = communityRepository,
                    currentUserId = signedInUser.uid,
                    notificationsRepository = notificationsRepository
                )
            )
        }
    }
    var commentingTarget by remember { mutableStateOf<CommentTarget?>(null) }
    var commentCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var activeComments by remember { mutableStateOf<List<ShayariComment>>(emptyList()) }
    var commentsLoading by remember { mutableStateOf(false) }
    var commentsSubmitting by remember { mutableStateOf(false) }
    var commentsError by remember { mutableStateOf<String?>(null) }
    var postNotifications by remember { mutableStateOf<List<PostNotification>>(emptyList()) }
    var notificationsLoading by remember { mutableStateOf(false) }
    var notificationsError by remember { mutableStateOf<String?>(null) }
    var shareTarget by remember { mutableStateOf<ShareTarget?>(null) }
    var isPreparingShareImage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        DailyShayariScheduler.cancelDailyNotification(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope
        )
    }

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid == null) {
            commentCounts = emptyMap()
            postNotifications = emptyList()
            notificationsLoading = false
            notificationsError = null
        } else {
            launch {
                runCatching {
                    val cloudRepo = PersonalShayariRepository(context.applicationContext)
                    localPersonalRepository.migrateAllToCloud(cloudRepo)
                }
            }
            if (commentsRepository != null) {
                launch {
                    commentsRepository.observeCommentCounts()
                        .catch { /* Keep previous counts on transient failure */ }
                        .collect { commentCounts = it }
                }
            }
            if (notificationsRepository != null) {
                launch {
                    notificationsLoading = true
                    notificationsError = null
                    notificationsRepository.observeMyNotifications(uid)
                        .catch {
                            notificationsLoading = false
                            notificationsError = "नोटिफिकेशन लोड नहीं हो सके"
                        }
                        .collect { list ->
                            postNotifications = list
                            notificationsLoading = false
                        }
                }
            }
        }
    }

    LaunchedEffect(commentingTarget?.shayariKey, currentUser?.uid) {
        val target = commentingTarget
        if (target == null || currentUser == null || commentsRepository == null) {
            activeComments = emptyList()
            commentsLoading = false
            commentsError = null
        } else {
            commentsLoading = true
            commentsError = null
            commentsRepository.observeCommentsForShayari(target.shayariKey)
                .catch {
                    commentsLoading = false
                    commentsError = context.getString(R.string.comment_error_snackbar)
                }
                .collect { list ->
                    activeComments = list
                    commentsLoading = false
                }
        }
    }

    val showTopLevelNav = currentRoute in setOf(
        ROUTE_MEHFIL,
        ROUTE_FAVORITES,
        ROUTE_ACCOUNT
    )

    fun showFeedback(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun triggerAppRefresh() {
        isHomeSearchExpanded = false
        viewModel.refreshFeed()
        homeRefreshTick++
        showFeedback(context.getString(R.string.app_refreshed_snackbar))
    }

    fun handleTripleTapOwnerToggle() {
        viewModel.toggleOwnerStatsEnabled { enabled ->
            showFeedback(
                if (enabled) {
                    "👑 Owner मोड ऑन — अब सभी कैटेगरी की शायरी संख्या दिखेगी"
                } else {
                    "Owner मोड बंद — शायरी संख्या छुपा दी गई"
                }
            )
        }
    }

    fun handleHomeTabClick() {
        if (currentRoute == ROUTE_MEHFIL) {
            triggerAppRefresh()
        } else {
            navigateToTab(ROUTE_MEHFIL)
        }
    }

    fun handleSearchTabClick() {
        if (currentRoute != ROUTE_MEHFIL) {
            isHomeSearchExpanded = true
            searchFocusTrigger++
            navigateToTab(ROUTE_MEHFIL)
        } else {
            if (isHomeSearchExpanded && uiState.searchQuery.isNotEmpty()) {
                viewModel.onSearchQueryChanged("")
            }
            isHomeSearchExpanded = !isHomeSearchExpanded
            if (isHomeSearchExpanded) {
                searchFocusTrigger++
            }
        }
    }

    fun openWriteStudio(resetDraft: Boolean = true) {
        if (resetDraft) {
            activePersonalViewModel.cancelEditing()
        }
        navController.navigate(ROUTE_WRITE_STUDIO) {
            launchSingleTop = true
        }
    }

    fun handleCreateTabClick() {
        openWriteStudio(resetDraft = true)
    }

    fun handleAccountTabClick() {
        navigateToTab(ROUTE_ACCOUNT)
    }

    val unreadNotificationCount = remember(postNotifications) {
        postNotifications.count { !it.isRead }
    }

    LaunchedEffect(pendingNotificationShayariId, uiState.isLoading) {
        val targetId = pendingNotificationShayariId
        if (targetId != null && !uiState.isLoading) {
            onConsumeNotificationShayariId()
            navController.navigate("reader/$targetId/all") {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(pendingDeepLinkTarget, uiState.isLoading) {
        val target = pendingDeepLinkTarget
        if (target != null && !uiState.isLoading) {
            onConsumeDeepLinkTarget()
            when (target) {
                is ShayariShareHelper.DeepLinkTarget.ReadShayari -> {
                    navController.navigate("reader/${target.shayariId}/all") {
                        launchSingleTop = true
                    }
                }
                is ShayariShareHelper.DeepLinkTarget.OpenCategory -> {
                    viewModel.onCategorySelected(target.categoryId)
                    navController.navigate(ROUTE_MEHFIL) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                ShayariShareHelper.DeepLinkTarget.OpenHome -> {
                    viewModel.onCategorySelected(null)
                    navController.navigate(ROUTE_MEHFIL) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    fun openCommentsForShayari(shayari: ShayariEntity) {
        commentingTarget = CommentTarget(
            shayariKey = shayariCommentKey(shayari.id),
            couplet = shayari.couplet,
            poet = shayari.poet
        )
    }

    fun openCommentsForCommunityPost(post: CommunityShayari) {
        commentingTarget = CommentTarget(
            shayariKey = "community_${post.id}",
            couplet = post.couplet,
            poet = post.penName,
            communityPostId = post.id,
            recipientUserId = post.userId
        )
    }

    fun handleToggleFavorite(shayari: ShayariEntity) {
        val willBeFavorite = !shayari.isFavorite
        viewModel.toggleFavorite(shayari)
        showFeedback(
            if (willBeFavorite) {
                context.getString(R.string.added_favorite_snackbar)
            } else {
                context.getString(R.string.removed_favorite_snackbar)
            }
        )
    }

    fun notifyCommunityShareIfNeeded(post: CommunityShayari) {
        val activeUser = currentUser
        if (activeUser != null && post.userId != activeUser.uid && notificationsRepository != null) {
            val actorName = activeUser.displayName?.takeIf { it.isNotBlank() } ?: "शायर दोस्त"
            scope.launch {
                notificationsRepository.sendNotification(
                    recipientUserId = post.userId,
                    actorName = actorName,
                    type = PostNotificationType.SHARE,
                    postId = post.id,
                    postCouplet = post.couplet
                )
            }
        }
    }

    fun resolveShareTextWithPromo(target: ShareTarget): String {
        val raw = target.toShareText(context)
        return if (target is ShareTarget.App) {
            raw
        } else {
            ShayariShareHelper.appendAppPromoFooter(context, raw)
        }
    }

    suspend fun resolveShareImageUri(target: ShareTarget): Uri? {
        return when (target) {
            is ShareTarget.Catalog,
            is ShareTarget.Personal,
            is ShareTarget.Community -> {
                val entity = target.toCardEntity() ?: return null
                ShayariShareHelper.createShareImageUri(
                    context = context,
                    shayari = entity,
                    style = uiState.preferences.cardStyle,
                    fontStyle = uiState.preferences.fontStyle,
                    customBadgeLabel = target.toCustomBadgeLabel()
                )
            }
            is ShareTarget.DesignedCard -> {
                ShayariShareHelper.createDesignedCardImageUri(
                    context = context,
                    couplet = target.couplet,
                    authorName = target.authorName,
                    title = target.title,
                    tag = target.tag,
                    design = target.design
                )
            }
            ShareTarget.App -> {
                SocialShareLauncher.createAppPromoImageUri(context)
            }
        }
    }

    fun executeShareToPlatform(
        target: ShareTarget,
        platform: SocialSharePlatform,
        mode: ShareContentMode
    ) {
        if (isPreparingShareImage) return
        val defaultTitle = if (target is ShareTarget.App) {
            context.getString(R.string.share_app_subject, context.getString(R.string.app_name))
        } else {
            context.getString(R.string.share_chooser_title)
        }
        val previewTitle = target.toPreviewTitle(defaultTitle)
        val viralText = resolveShareTextWithPromo(target)
        val shouldAttachImage = mode == ShareContentMode.IMAGE || platform.prefersImageCard

        if (!shouldAttachImage) {
            shareTarget = null
            val result = SocialShareLauncher.shareToPlatform(
                context = context,
                platform = platform,
                text = viralText,
                previewTitle = previewTitle,
                imageUri = null
            )
            when (result) {
                SocialShareResult.LAUNCHED_NATIVE_APP,
                SocialShareResult.LAUNCHED_WEB_FALLBACK -> {
                    if (target is ShareTarget.Community) notifyCommunityShareIfNeeded(target.post)
                }
                SocialShareResult.LAUNCHED_CHOOSER_FALLBACK -> {
                    showFeedback(context.getString(R.string.share_platform_copied_fallback))
                    if (target is ShareTarget.Community) notifyCommunityShareIfNeeded(target.post)
                }
                SocialShareResult.FAILED -> {
                    showFeedback(context.getString(R.string.share_error_snackbar))
                }
            }
            return
        }

        scope.launch {
            isPreparingShareImage = true
            val imageUri = try {
                runCatching { resolveShareImageUri(target) }.getOrNull()
            } finally {
                isPreparingShareImage = false
            }
            shareTarget = null
            val result = SocialShareLauncher.shareToPlatform(
                context = context,
                platform = platform,
                text = viralText,
                previewTitle = previewTitle,
                imageUri = imageUri
            )
            when (result) {
                SocialShareResult.LAUNCHED_NATIVE_APP,
                SocialShareResult.LAUNCHED_WEB_FALLBACK -> {
                    if (target is ShareTarget.Community) notifyCommunityShareIfNeeded(target.post)
                }
                SocialShareResult.LAUNCHED_CHOOSER_FALLBACK -> {
                    showFeedback(context.getString(R.string.share_platform_copied_fallback))
                    if (target is ShareTarget.Community) notifyCommunityShareIfNeeded(target.post)
                }
                SocialShareResult.FAILED -> {
                    showFeedback(context.getString(R.string.share_error_snackbar))
                }
            }
        }
    }

    fun executeShareMore(target: ShareTarget, mode: ShareContentMode) {
        if (isPreparingShareImage) return
        val defaultTitle = if (target is ShareTarget.App) {
            context.getString(R.string.share_app_subject, context.getString(R.string.app_name))
        } else {
            context.getString(R.string.share_chooser_title)
        }
        val previewTitle = target.toPreviewTitle(defaultTitle)
        val viralText = resolveShareTextWithPromo(target)

        if (mode == ShareContentMode.TEXT) {
            shareTarget = null
            val ok = if (target is ShareTarget.App) {
                ShayariShareHelper.shareApp(context)
            } else {
                ShayariShareHelper.sharePlainText(
                    context = context,
                    text = viralText,
                    previewTitle = previewTitle
                )
            }
            if (!ok) {
                showFeedback(context.getString(R.string.share_error_snackbar))
            } else if (target is ShareTarget.Community) {
                notifyCommunityShareIfNeeded(target.post)
            }
            return
        }

        scope.launch {
            isPreparingShareImage = true
            val imageUri = try {
                runCatching { resolveShareImageUri(target) }.getOrNull()
            } finally {
                isPreparingShareImage = false
            }
            shareTarget = null
            val ok = if (imageUri != null) {
                runCatching {
                    context.startActivity(
                        ShayariShareHelper.buildShareImageChooserIntent(
                            context = context,
                            imageUri = imageUri,
                            text = viralText,
                            previewTitle = previewTitle
                        )
                    )
                    true
                }.getOrDefault(false)
            } else {
                false
            }
            if (ok) {
                if (target is ShareTarget.Community) notifyCommunityShareIfNeeded(target.post)
            } else {
                showFeedback(context.getString(R.string.share_card_error))
                ShayariShareHelper.sharePlainText(
                    context = context,
                    text = viralText,
                    previewTitle = previewTitle
                )
            }
        }
    }

    fun executeCopyShareTarget(target: ShareTarget) {
        val textToCopy = target.toShareText(context)
        ShayariShareHelper.copyPlainText(context, textToCopy)
        shareTarget = null
        showFeedback(
            if (target is ShareTarget.App) {
                context.getString(R.string.share_copy_app_snackbar)
            } else {
                context.getString(R.string.copied_snackbar)
            }
        )
    }

    fun executeDownloadShareTarget(target: ShareTarget) {
        if (isPreparingShareImage) return
        scope.launch {
            isPreparingShareImage = true
            val result = try {
                when (target) {
                    is ShareTarget.Catalog,
                    is ShareTarget.Personal,
                    is ShareTarget.Community -> {
                        val entity = target.toCardEntity()
                        if (entity != null) {
                            ShayariShareHelper.saveCardToGallery(
                                context = context,
                                shayari = entity,
                                style = uiState.preferences.cardStyle,
                                fontStyle = uiState.preferences.fontStyle
                            )
                        } else {
                            Result.failure(IllegalStateException("Missing card entity"))
                        }
                    }
                    is ShareTarget.DesignedCard -> {
                        ShayariShareHelper.saveDesignedCardToGallery(
                            context = context,
                            couplet = target.couplet,
                            authorName = target.authorName,
                            title = target.title,
                            tag = target.tag,
                            design = target.design
                        )
                    }
                    ShareTarget.App -> {
                        SocialShareLauncher.saveAppPromoCardToGallery(context)
                    }
                }
            } finally {
                isPreparingShareImage = false
            }
            shareTarget = null
            if (result.isSuccess) {
                showFeedback(context.getString(R.string.download_image_success))
            } else {
                showFeedback(context.getString(R.string.download_image_error))
            }
        }
    }

    fun handleShareShayari(shayari: ShayariEntity) {
        shareTarget = ShareTarget.Catalog(shayari)
    }

    fun handleShareCommunityPost(post: CommunityShayari) {
        shareTarget = ShareTarget.Community(post)
    }

    fun handleDownloadImage(shayari: ShayariEntity) {
        scope.launch {
            val result = ShayariShareHelper.saveCardToGallery(
                context = context,
                shayari = shayari,
                style = uiState.preferences.cardStyle,
                fontStyle = uiState.preferences.fontStyle
            )
            if (result.isSuccess) {
                showFeedback(context.getString(R.string.download_image_success))
            } else {
                showFeedback(context.getString(R.string.download_image_error))
            }
        }
    }

    val localAccount = uiState.preferences.localAccount
    val isAnySignedIn = currentUser != null || localAccount.isLoggedIn
    val resolvedProfilePhotoUri = remember(localAccount.profilePhotoUri, currentUser?.photoUrl) {
        ProfilePhotoStorage.resolveDisplayUri(
            storedPhotoUri = localAccount.profilePhotoUri,
            fallbackRemoteUri = currentUser?.photoUrl
        )
    }
    val navAvatarDisplayName = uiState.preferences.authorPenName.takeIf { it.isNotBlank() }
        ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: localAccount.fullName.takeIf { localAccount.isLoggedIn && it.isNotBlank() }

    val resolvedAuthorName = remember(uiState.preferences.authorPenName, localAccount, currentUser) {
        uiState.preferences.authorPenName.trim().ifBlank {
            localAccount.fullName.trim().takeIf { localAccount.isLoggedIn && it.isNotBlank() }
                ?: currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: ""
        }
    }

    LaunchedEffect(currentUser?.uid, uiState.isLoading) {
        val activeUser = currentUser ?: return@LaunchedEffect
        val communityVm = sharedCommunityViewModel ?: return@LaunchedEffect
        if (!uiState.isLoading) {
            val initialName = resolvedAuthorName.ifBlank {
                activeUser.displayName?.trim()?.takeIf { it.isNotBlank() } ?: "शायर दोस्त"
            }
            communityVm.ensurePublicPoetProfile(initialName)
        }
    }

    fun handleCopyPersonal(item: PersonalShayari) {
        ShayariShareHelper.copyPlainText(
            context,
            ShayariShareHelper.formatPersonalShareText(item, resolvedAuthorName)
        )
        showFeedback(context.getString(R.string.copied_snackbar))
    }

    fun handleSharePersonal(item: PersonalShayari) {
        shareTarget = ShareTarget.Personal(
            item = item,
            authorFallback = resolvedAuthorName
        )
    }

    fun handleShareApp() {
        shareTarget = ShareTarget.App
    }

    fun handleRateApp() {
        ShayariShareHelper.openPlayStoreForRating(context)
    }

    fun navigateToReader(shayariId: Int, readerScope: String) {
        navController.navigate("reader/$shayariId/$readerScope")
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        val isFullScreenStudioOrReader = currentRoute == ROUTE_READER ||
            currentRoute == ROUTE_WRITE_STUDIO ||
            currentRoute == ROUTE_EDIT_PROFILE

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = if (isFullScreenStudioOrReader) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useNavigationRail) {
                    ShayariBottomBar(
                        isHomeSelected = currentRoute == ROUTE_MEHFIL,
                        isFavoritesSelected = currentRoute == ROUTE_FAVORITES,
                        isAccountSelected = currentRoute == ROUTE_ACCOUNT,
                        currentUserPhotoUrl = resolvedProfilePhotoUri,
                        currentUserDisplayName = navAvatarDisplayName,
                        isSignedIn = isAnySignedIn,
                        onHomeClick = ::handleHomeTabClick,
                        onFavoritesClick = { navigateToTab(ROUTE_FAVORITES) },
                        onCreateClick = ::handleCreateTabClick,
                        onAccountClick = ::handleAccountTabClick
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        val railItemColors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        BottomTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (tab == BottomTab.MEHFIL) {
                                        handleHomeTabClick()
                                    } else {
                                        navigateToTab(tab.route)
                                    }
                                },
                                colors = railItemColors,
                                icon = {
                                    if (tab == BottomTab.ACCOUNT) {
                                        AccountAvatar(
                                            photoUrl = resolvedProfilePhotoUri,
                                            displayName = navAvatarDisplayName,
                                            isSignedIn = isAnySignedIn,
                                            isSelected = selected,
                                            size = 24.dp,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_MEHFIL
                    ) {
                        composable(ROUTE_MEHFIL) {
                            MehfilScreen(
                                uiState = uiState,
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { viewModel.cycleThemeMode(systemDark) },
                                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                                onCategorySelected = viewModel::onCategorySelected,
                                onOpenReader = { shayari, _ ->
                                    val scopeKey = if (uiState.selectedCategoryId == null && uiState.searchQuery.isBlank()) {
                                        "all"
                                    } else {
                                        "filtered"
                                    }
                                    navigateToReader(shayari.id, scopeKey)
                                },
                                onToggleFavorite = ::handleToggleFavorite,
                                onComment = ::openCommentsForShayari,
                                onShare = ::handleShareShayari,
                                commentCounts = commentCounts,
                                onOpenKaviManch = {
                                    openWriteStudio(resetDraft = true)
                                },
                                onOpenPoetCommunity = {
                                    navController.navigate(ROUTE_KAVI_MANCH) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenCategories = {},
                                unreadNotificationCount = unreadNotificationCount,
                                onOpenNotificationSettings = { showNotificationSheet = true },
                                searchExpanded = isHomeSearchExpanded,
                                onToggleSearch = ::handleSearchTabClick,
                                searchFocusTrigger = searchFocusTrigger,
                                refreshTrigger = homeRefreshTick,
                                onRefreshApp = ::triggerAppRefresh,
                                onShareApp = ::handleShareApp,
                                isOwner = isOwner,
                                showCounts = isOwner,
                                onTripleTapOwnerToggle = ::handleTripleTapOwnerToggle,
                                onOpenAdminDashboard = {
                                    navController.navigate(ROUTE_ADMIN_DASHBOARD) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(ROUTE_CATEGORIES) {
                            CategoriesScreen(
                                categoryCounts = uiState.categoryCounts,
                                onCategoryClick = { category ->
                                    navController.navigate("category_detail/${category.id}")
                                },
                                onBack = { navController.popBackStack() },
                                isOwner = isOwner,
                                showCounts = isOwner,
                                onTripleTapOwnerToggle = ::handleTripleTapOwnerToggle,
                                onOpenAdminDashboard = {
                                    navController.navigate(ROUTE_ADMIN_DASHBOARD) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(
                            route = ROUTE_CATEGORY_DETAIL,
                            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
                        ) { entry ->
                            val catId = entry.arguments?.getString("categoryId") ?: ShayariCategory.MOHABBAT.id
                            val category = ShayariCategory.fromId(catId)
                            val list = uiState.allShayari.filter { it.categoryId == category.id }
                            CategoryDetailScreen(
                                category = category,
                                shayariList = list,
                                onBack = { navController.popBackStack() },
                                onOpenReader = { shayari ->
                                    navigateToReader(shayari.id, "category_${category.id}")
                                },
                                onToggleFavorite = ::handleToggleFavorite,
                                onComment = ::openCommentsForShayari,
                                onShare = ::handleShareShayari,
                                commentCounts = commentCounts,
                                isOwner = isOwner,
                                showCounts = isOwner
                            )
                        }

                        composable(ROUTE_FAVORITES) {
                            FavoritesScreen(
                                favorites = uiState.favoriteShayari,
                                onExploreClick = { navigateToTab(ROUTE_MEHFIL) },
                                onOpenReader = { shayari ->
                                    navigateToReader(shayari.id, "favorites")
                                },
                                onToggleFavorite = ::handleToggleFavorite,
                                onComment = ::openCommentsForShayari,
                                onShare = ::handleShareShayari,
                                commentCounts = commentCounts
                            )
                        }

                        composable(ROUTE_ACCOUNT) {
                            val activeUser = currentUser
                            val activeCommunityVm = sharedCommunityViewModel
                            if (activeUser == null || activeCommunityVm == null) {
                                key(LOCAL_GUEST_USER_ID) {
                                    AccountScreen(
                                        currentUser = null,
                                        personalViewModel = activePersonalViewModel,
                                        communityViewModel = null,
                                        onSignOut = {
                                            viewModel.logoutLocalAccount {
                                                showFeedback("लॉगआउट हो गया")
                                            }
                                        },
                                        onOpenPersonalCard = { entity ->
                                            previewCommunityPost = null
                                            previewCommunityEntity = entity
                                            navigateToReader(entity.id, "personal")
                                        },
                                        onCopyPersonal = ::handleCopyPersonal,
                                        onSharePersonal = ::handleSharePersonal,
                                        onFeedback = ::showFeedback,
                                        onShareApp = ::handleShareApp,
                                        onRateApp = ::handleRateApp,
                                        onAuthSuccess = {
                                            showFeedback(context.getString(R.string.account_welcome_snackbar))
                                        },
                                        onAuthError = ::showFeedback,
                                        onOpenWriteStudio = { openWriteStudio(resetDraft = false) },
                                        onOpenKaviManch = {
                                            navController.navigate(ROUTE_KAVI_MANCH) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onOpenMyPoetProfile = {
                                            navController.navigate("poet_profile/${Uri.encode(LOCAL_GUEST_USER_ID)}") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onAuthorNameChanged = viewModel::setAuthorPenName,
                                        authorPenName = resolvedAuthorName,
                                        localAccount = localAccount,
                                        profilePhotoUri = resolvedProfilePhotoUri,
                                        onOpenAuthSheet = {
                                            authScreenError = null
                                            navController.navigate(ROUTE_AUTH) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onUpdateLocalProfile = { newName, newBio, newSocials ->
                                            viewModel.updateLocalProfileAndSocialLinks(
                                                penName = newName,
                                                bio = newBio,
                                                socialLinks = newSocials
                                            )
                                        },
                                        onOpenEasyProfile = {
                                            navController.navigate(ROUTE_EDIT_PROFILE) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onSaveEasyProfile = { newName, newBio, newPhotoUri, newSocials ->
                                            viewModel.updateEasyProfile(
                                                penName = newName,
                                                bio = newBio,
                                                profilePhotoUri = newPhotoUri,
                                                socialLinks = newSocials
                                            )
                                        },
                                        isOwner = isOwner,
                                        totalCatalogShayariCount = uiState.allShayari.size,
                                        categoryCount = ShayariCategory.entries.size,
                                        onOpenOwnerCategoryStats = {
                                            navController.navigate(ROUTE_ADMIN_DASHBOARD) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            } else {
                                key(activeUser.uid) {
                                    AccountScreen(
                                        currentUser = activeUser,
                                        personalViewModel = activePersonalViewModel,
                                        communityViewModel = activeCommunityVm,
                                        onSignOut = {
                                            viewModel.logoutLocalAccount()
                                            signOutFromGoogle(
                                                credentialManager = credentialManager,
                                                scope = scope,
                                                onSignOutComplete = {
                                                    showFeedback("लॉगआउट हो गया")
                                                }
                                            )
                                        },
                                        onOpenPersonalCard = { entity ->
                                            previewCommunityPost = null
                                            previewCommunityEntity = entity
                                            navigateToReader(entity.id, "personal")
                                        },
                                        onCopyPersonal = ::handleCopyPersonal,
                                        onSharePersonal = ::handleSharePersonal,
                                        onOpenCommunityCard = { post ->
                                            val entity = post.toShayariEntity()
                                            previewCommunityPost = post
                                            previewCommunityEntity = entity
                                            navigateToReader(entity.id, "community")
                                        },
                                        onShareCommunity = ::handleShareCommunityPost,
                                        onFeedback = ::showFeedback,
                                        onShareApp = ::handleShareApp,
                                        onRateApp = ::handleRateApp,
                                        onOpenWriteStudio = { openWriteStudio(resetDraft = false) },
                                        onOpenKaviManch = {
                                            navController.navigate(ROUTE_KAVI_MANCH) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onOpenMyPoetProfile = {
                                            navController.navigate("poet_profile/${Uri.encode(activeUser.uid)}") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onAuthorNameChanged = viewModel::setAuthorPenName,
                                        authorPenName = resolvedAuthorName,
                                        localAccount = localAccount,
                                        profilePhotoUri = resolvedProfilePhotoUri,
                                        onUpdateLocalProfile = { newName, newBio, newSocials ->
                                            viewModel.updateLocalProfileAndSocialLinks(
                                                penName = newName,
                                                bio = newBio,
                                                socialLinks = newSocials
                                            )
                                        },
                                        onOpenEasyProfile = {
                                            navController.navigate(ROUTE_EDIT_PROFILE) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onSaveEasyProfile = { newName, newBio, newPhotoUri, newSocials ->
                                            viewModel.updateEasyProfile(
                                                penName = newName,
                                                bio = newBio,
                                                profilePhotoUri = newPhotoUri,
                                                socialLinks = newSocials
                                            )
                                        },
                                        isOwner = isOwner,
                                        totalCatalogShayariCount = uiState.allShayari.size,
                                        categoryCount = ShayariCategory.entries.size,
                                        onOpenOwnerCategoryStats = {
                                            navController.navigate(ROUTE_ADMIN_DASHBOARD) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        composable(ROUTE_EDIT_PROFILE) {
                            val activeUser = currentUser
                            val activeCommunityVm = sharedCommunityViewModel
                            val communityUiState = activeCommunityVm?.uiState?.collectAsStateWithLifecycle()?.value
                            val myPoetProfile = remember(communityUiState?.poetProfiles, activeUser?.uid) {
                                val uid = activeUser?.uid ?: return@remember null
                                communityUiState?.profileFor(uid)
                            }
                            val initialBio = if (activeUser != null) {
                                communityUiState?.userProfile?.bio?.takeIf { it.isNotBlank() }
                                    ?: myPoetProfile?.bio?.takeIf { it.isNotBlank() }
                                    ?: localAccount.bio
                            } else {
                                localAccount.bio
                            }
                            val initialSocialLinks = remember(myPoetProfile, localAccount.socialLinks, activeUser) {
                                val remoteLinks = myPoetProfile?.toSocialLinks()
                                if (activeUser != null && remoteLinks != null && remoteLinks.hasAny()) {
                                    remoteLinks
                                } else {
                                    localAccount.socialLinks
                                }
                            }

                            EasyProfileEditorScreen(
                                initialName = resolvedAuthorName,
                                initialBio = initialBio,
                                initialStoredPhotoUri = localAccount.profilePhotoUri,
                                fallbackRemotePhotoUri = activeUser?.photoUrl,
                                initialSocialLinks = initialSocialLinks,
                                isSaving = communityUiState?.isSubmitting == true,
                                onBack = { navController.popBackStack() },
                                onFeedback = ::showFeedback,
                                onSave = { newName, newBio, newPhotoUri, newSocials ->
                                    if (activeUser != null && activeCommunityVm != null) {
                                        activeCommunityVm.updatePoetProfile(
                                            penName = newName,
                                            bio = newBio,
                                            socialLinks = newSocials,
                                            onSuccess = {
                                                viewModel.updateEasyProfile(
                                                    penName = newName,
                                                    bio = newBio,
                                                    profilePhotoUri = newPhotoUri,
                                                    socialLinks = newSocials
                                                ) {
                                                    navController.popBackStack()
                                                    showFeedback(context.getString(R.string.profile_saved_snackbar))
                                                }
                                            },
                                            onError = ::showFeedback
                                        )
                                    } else {
                                        viewModel.updateEasyProfile(
                                            penName = newName,
                                            bio = newBio,
                                            profilePhotoUri = newPhotoUri,
                                            socialLinks = newSocials
                                        ) {
                                            navController.popBackStack()
                                            showFeedback(context.getString(R.string.profile_saved_snackbar))
                                        }
                                    }
                                }
                            )
                        }

                        composable(ROUTE_AUTH) {
                            LaunchedEffect(currentUser?.uid) {
                                if (currentUser != null) {
                                    authScreenError = null
                                    navController.popBackStack()
                                }
                            }
                            AuthScreen(
                                localAccount = localAccount,
                                errorMessage = authScreenError,
                                showGoogleSignIn = firebaseReady,
                                onClearError = { authScreenError = null },
                                onRegisterSubmit = { fullName, username, contact, password, bio, socialLinks ->
                                    viewModel.registerLocalAccount(
                                        fullName = fullName,
                                        username = username,
                                        contact = contact,
                                        password = password,
                                        bio = bio,
                                        socialLinks = socialLinks
                                    ) {
                                        authScreenError = null
                                        navController.popBackStack()
                                        showFeedback(context.getString(R.string.auth_register_success))
                                    }
                                },
                                onLoginSubmit = { identifier, password ->
                                    viewModel.loginLocalAccount(identifier, password) { result ->
                                        when (result) {
                                            LoginLocalResult.Success -> {
                                                authScreenError = null
                                                navController.popBackStack()
                                                showFeedback(context.getString(R.string.auth_login_success))
                                            }
                                            LoginLocalResult.NoAccountRegistered -> {
                                                authScreenError = context.getString(R.string.auth_no_account_error)
                                            }
                                            LoginLocalResult.InvalidCredentials -> {
                                                authScreenError = context.getString(R.string.auth_invalid_credentials_error)
                                            }
                                        }
                                    }
                                },
                                onResetPasswordSubmit = { newPassword ->
                                    viewModel.resetLocalPasswordAndLogin(newPassword) { ok ->
                                        if (ok) {
                                            authScreenError = null
                                            navController.popBackStack()
                                            showFeedback("नया पासवर्ड सेट हो गया और लॉगिन सफल ✨")
                                        } else {
                                            authScreenError = "पासवर्ड अपडेट नहीं हो सका, कृपया फिर कोशिश करें।"
                                        }
                                    }
                                },
                                onGoogleAuthSuccess = {
                                    authScreenError = null
                                    navController.popBackStack()
                                    showFeedback(context.getString(R.string.account_welcome_snackbar))
                                },
                                onGoogleAuthError = { err ->
                                    authScreenError = err
                                    showFeedback(err)
                                },
                                onBack = {
                                    authScreenError = null
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(ROUTE_ADMIN_DASHBOARD) {
                            val activeUser = currentUser
                            if (activeUser == null || !firebaseReady) {
                                var localSearchQuery by remember { mutableStateOf("") }
                                var localSortOrder by remember {
                                    mutableStateOf(com.example.ui.viewmodel.AdminCategorySortOrder.HIGHEST_COUNT)
                                }
                                var localShowOnlyPopulated by remember { mutableStateOf(false) }

                                AdminDashboardScreen(
                                    isFirebaseSignedIn = false,
                                    isCloudConfigured = firebaseReady,
                                    uiState = AdminDashboardUiState(
                                        searchQuery = localSearchQuery,
                                        sortOrder = localSortOrder,
                                        showOnlyPopulated = localShowOnlyPopulated
                                    ),
                                    catalogCategoryCounts = uiState.categoryCounts,
                                    onSearchQueryChanged = { localSearchQuery = it.take(60) },
                                    onSortOrderChanged = { localSortOrder = it },
                                    onToggleOnlyPopulated = { localShowOnlyPopulated = !localShowOnlyPopulated },
                                    onRefresh = {},
                                    onCategoryClick = { category ->
                                        navController.navigate("category_detail/${category.id}")
                                    },
                                    onOpenKaviManch = {
                                        navController.navigate(ROUTE_KAVI_MANCH) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onAuthSuccess = {
                                        showFeedback(context.getString(R.string.account_welcome_snackbar))
                                    },
                                    onAuthError = ::showFeedback,
                                    onBack = { navController.popBackStack() }
                                )
                            } else {
                                key(activeUser.uid) {
                                    val adminRepository = remember(context) {
                                        AdminDashboardRepository(context.applicationContext)
                                    }
                                    val adminViewModel: AdminDashboardViewModel = viewModel(
                                        key = "admin_dashboard_${activeUser.uid}",
                                        factory = AdminDashboardViewModel.Factory(adminRepository)
                                    )
                                    val adminUiState by adminViewModel.uiState.collectAsStateWithLifecycle()

                                    AdminDashboardScreen(
                                        isFirebaseSignedIn = true,
                                        isCloudConfigured = true,
                                        uiState = adminUiState,
                                        catalogCategoryCounts = uiState.categoryCounts,
                                        onSearchQueryChanged = adminViewModel::onSearchQueryChanged,
                                        onSortOrderChanged = adminViewModel::onSortOrderChanged,
                                        onToggleOnlyPopulated = adminViewModel::onToggleOnlyPopulated,
                                        onRefresh = adminViewModel::refresh,
                                        onCategoryClick = { category ->
                                            navController.navigate("category_detail/${category.id}")
                                        },
                                        onOpenKaviManch = {
                                            navController.navigate(ROUTE_KAVI_MANCH) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }

                        composable(ROUTE_KAVI_MANCH) {
                            val activeUser = currentUser
                            val activeCommunityVm = sharedCommunityViewModel
                            if (activeUser == null || activeCommunityVm == null) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { navController.popBackStack() },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("kavi_manch_auth_back_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                contentDescription = stringResource(R.string.action_back),
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                    KaviManchAuthGate(
                                        onAuthSuccess = {
                                            showFeedback(context.getString(R.string.account_welcome_snackbar))
                                        },
                                        onAuthError = ::showFeedback,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                KaviManchScreen(
                                    currentUser = activeUser,
                                    viewModel = activeCommunityVm,
                                    onSignOut = {
                                        signOutFromGoogle(
                                            credentialManager = credentialManager,
                                            scope = scope,
                                            onSignOutComplete = {
                                                showFeedback("लॉगआउट हो गया")
                                                navController.popBackStack()
                                            }
                                        )
                                    },
                                    onOpenAsCard = { post ->
                                        val entity = post.toShayariEntity()
                                        previewCommunityPost = post
                                        previewCommunityEntity = entity
                                        navigateToReader(entity.id, "community")
                                    },
                                    onComment = ::openCommentsForCommunityPost,
                                    onShare = ::handleShareCommunityPost,
                                    onFeedback = ::showFeedback,
                                    onOpenPoetProfile = { poetUid ->
                                        if (poetUid.isNotBlank()) {
                                            navController.navigate("poet_profile/${Uri.encode(poetUid)}") {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    onBack = { navController.popBackStack() },
                                    commentCounts = commentCounts
                                )
                            }
                        }

                        composable(
                            route = ROUTE_POET_PROFILE,
                            arguments = listOf(navArgument("poetUserId") { type = NavType.StringType })
                        ) { entry ->
                            val poetUserId = Uri.decode(entry.arguments?.getString("poetUserId").orEmpty())
                            val activeUser = currentUser
                            val activeCommunityVm = sharedCommunityViewModel
                            if (poetUserId == LOCAL_GUEST_USER_ID && localAccount.isLoggedIn && activeUser == null) {
                                val personalUiState by activePersonalViewModel.uiState.collectAsStateWithLifecycle()
                                var showEditLocalProfileDialog by remember { mutableStateOf(false) }
                                val localPoetName = localAccount.fullName.trim().ifBlank {
                                    resolvedAuthorName.ifBlank { "शायर दोस्त" }
                                }
                                val localPoetPosts = remember(personalUiState.allItems, localPoetName) {
                                    personalUiState.allItems.map { item ->
                                        val mappedEntity = item.toShayariEntity(localPoetName)
                                        CommunityShayari(
                                            id = item.id,
                                            userId = LOCAL_GUEST_USER_ID,
                                            penName = localPoetName,
                                            couplet = item.couplet,
                                            categoryId = mappedEntity.categoryId,
                                            promptTitle = item.title
                                        )
                                    }
                                }
                                val localPoetProfile = remember(localAccount, localPoetName, localPoetPosts.size) {
                                    PoetProfile(
                                        id = LOCAL_GUEST_USER_ID,
                                        userId = LOCAL_GUEST_USER_ID,
                                        penName = localPoetName,
                                        bio = localAccount.bio,
                                        isPublic = true,
                                        followerCount = 0L,
                                        followingCount = 0L,
                                        postCount = localPoetPosts.size.toLong(),
                                        facebookUrl = localAccount.socialLinks.facebookUrl,
                                        instagramUrl = localAccount.socialLinks.instagramUrl,
                                        whatsappUrl = localAccount.socialLinks.whatsappUrl,
                                        twitterUrl = localAccount.socialLinks.twitterUrl,
                                        telegramUrl = localAccount.socialLinks.telegramUrl,
                                        youtubeUrl = localAccount.socialLinks.youtubeUrl
                                    )
                                }

                                PoetProfileScreen(
                                    poetProfile = localPoetProfile,
                                    poetPosts = localPoetPosts,
                                    followers = emptyList(),
                                    isOwnProfile = true,
                                    isFollowing = false,
                                    userWahIds = emptySet(),
                                    ownPhotoUrl = null,
                                    onBack = { navController.popBackStack() },
                                    onToggleFollow = {},
                                    onEditProfile = { showEditLocalProfileDialog = true },
                                    onToggleWah = {},
                                    onOpenPostAsCard = { post ->
                                        val matchedPersonal = personalUiState.allItems.firstOrNull { it.id == post.id }
                                        val entity = matchedPersonal?.toShayariEntity(localPoetName) ?: post.toShayariEntity()
                                        previewCommunityPost = null
                                        previewCommunityEntity = entity
                                        navigateToReader(entity.id, "personal")
                                    },
                                    onCommentPost = ::openCommentsForCommunityPost,
                                    onSharePost = ::handleShareCommunityPost,
                                    onFeedback = ::showFeedback
                                )

                                if (showEditLocalProfileDialog) {
                                    ProfileManagementSheet(
                                        initialPenName = localPoetProfile.penName,
                                        initialBio = localPoetProfile.bio,
                                        initialSocialLinks = localAccount.socialLinks,
                                        onDismiss = { showEditLocalProfileDialog = false },
                                        onSave = { newPenName, newBio, newSocialLinks ->
                                            viewModel.updateLocalProfileAndSocialLinks(
                                                penName = newPenName,
                                                bio = newBio,
                                                socialLinks = newSocialLinks
                                            ) {
                                                showEditLocalProfileDialog = false
                                                showFeedback(context.getString(R.string.poet_profile_saved_snackbar))
                                            }
                                        }
                                    )
                                }
                            } else if (activeUser == null || activeCommunityVm == null || poetUserId.isBlank()) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { navController.popBackStack() },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("poet_profile_auth_back_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                contentDescription = stringResource(R.string.action_back),
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                    KaviManchAuthGate(
                                        onAuthSuccess = {
                                            showFeedback(context.getString(R.string.account_welcome_snackbar))
                                        },
                                        onAuthError = ::showFeedback,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                val communityUiState by activeCommunityVm.uiState.collectAsStateWithLifecycle()
                                val followers by remember(poetUserId, activeCommunityVm) {
                                    activeCommunityVm.observeFollowersForPoet(poetUserId)
                                }.collectAsStateWithLifecycle(initialValue = emptyList())
                                var showEditMyProfileDialog by remember { mutableStateOf(false) }

                                val isOwnProfile = poetUserId == activeUser.uid
                                val allCommunityPosts = (communityUiState.feedState as? CommunityFeedState.Success)?.posts.orEmpty()
                                val poetPosts = remember(allCommunityPosts, poetUserId) {
                                    allCommunityPosts.filter { it.userId == poetUserId }
                                }
                                val actorDisplayName = resolvedAuthorName.ifBlank {
                                    activeUser.displayName?.trim()?.takeIf { it.isNotBlank() } ?: "शायर दोस्त"
                                }
                                val poetProfile = remember(
                                    communityUiState.poetProfiles,
                                    communityUiState.followingIds,
                                    poetPosts,
                                    poetUserId,
                                    isOwnProfile,
                                    actorDisplayName,
                                    localAccount.socialLinks
                                ) {
                                    val remote = communityUiState.profileFor(poetUserId)
                                    if (remote != null) {
                                        if (isOwnProfile && !remote.toSocialLinks().hasAny() && localAccount.socialLinks.hasAny()) {
                                            remote.copy(
                                                facebookUrl = localAccount.socialLinks.facebookUrl,
                                                instagramUrl = localAccount.socialLinks.instagramUrl,
                                                whatsappUrl = localAccount.socialLinks.whatsappUrl,
                                                twitterUrl = localAccount.socialLinks.twitterUrl,
                                                telegramUrl = localAccount.socialLinks.telegramUrl,
                                                youtubeUrl = localAccount.socialLinks.youtubeUrl
                                            )
                                        } else {
                                            remote
                                        }
                                    } else {
                                        PoetProfile(
                                            id = poetUserId,
                                            userId = poetUserId,
                                            penName = if (isOwnProfile) {
                                                actorDisplayName
                                            } else {
                                                poetPosts.firstOrNull()?.penName?.ifBlank { "शायर दोस्त" } ?: "शायर दोस्त"
                                            },
                                            bio = if (isOwnProfile) communityUiState.userProfile?.bio.orEmpty() else "",
                                            isPublic = true,
                                            followerCount = 0L,
                                            followingCount = if (isOwnProfile) communityUiState.followingIds.size.toLong() else 0L,
                                            postCount = poetPosts.size.toLong(),
                                            facebookUrl = if (isOwnProfile) localAccount.socialLinks.facebookUrl else "",
                                            instagramUrl = if (isOwnProfile) localAccount.socialLinks.instagramUrl else "",
                                            whatsappUrl = if (isOwnProfile) localAccount.socialLinks.whatsappUrl else "",
                                            twitterUrl = if (isOwnProfile) localAccount.socialLinks.twitterUrl else "",
                                            telegramUrl = if (isOwnProfile) localAccount.socialLinks.telegramUrl else "",
                                            youtubeUrl = if (isOwnProfile) localAccount.socialLinks.youtubeUrl else ""
                                        )
                                    }
                                }
                                val isFollowing = communityUiState.followingIds.contains(poetUserId)

                                PoetProfileScreen(
                                    poetProfile = poetProfile,
                                    poetPosts = poetPosts,
                                    followers = followers,
                                    isOwnProfile = isOwnProfile,
                                    isFollowing = isFollowing,
                                    userWahIds = communityUiState.userWahIds,
                                    ownPhotoUrl = if (isOwnProfile) activeUser.photoUrl else null,
                                    onBack = { navController.popBackStack() },
                                    onToggleFollow = {
                                        activeCommunityVm.toggleFollow(
                                            targetUserId = poetUserId,
                                            targetPenName = poetProfile.penName,
                                            actorName = actorDisplayName,
                                            onSuccess = { nowFollowing ->
                                                showFeedback(
                                                    if (nowFollowing) {
                                                        context.getString(R.string.poet_followed_snackbar, poetProfile.penName)
                                                    } else {
                                                        context.getString(R.string.poet_unfollowed_snackbar, poetProfile.penName)
                                                    }
                                                )
                                            },
                                            onError = ::showFeedback
                                        )
                                    },
                                    onEditProfile = { showEditMyProfileDialog = true },
                                    onToggleWah = { post ->
                                        activeCommunityVm.toggleWah(
                                            post = post,
                                            actorName = actorDisplayName,
                                            onError = ::showFeedback
                                        )
                                    },
                                    onOpenPostAsCard = { post ->
                                        val entity = post.toShayariEntity()
                                        previewCommunityPost = post
                                        previewCommunityEntity = entity
                                        navigateToReader(entity.id, "community")
                                    },
                                    onCommentPost = ::openCommentsForCommunityPost,
                                    onSharePost = ::handleShareCommunityPost,
                                    onFeedback = ::showFeedback
                                )

                                if (showEditMyProfileDialog && isOwnProfile) {
                                    ProfileManagementSheet(
                                        initialPenName = poetProfile.penName,
                                        initialBio = poetProfile.bio,
                                        initialSocialLinks = poetProfile.toSocialLinks(),
                                        isSaving = communityUiState.isSubmitting,
                                        onDismiss = { showEditMyProfileDialog = false },
                                        onSave = { newPenName, newBio, newSocialLinks ->
                                            activeCommunityVm.updatePoetProfile(
                                                penName = newPenName,
                                                bio = newBio,
                                                socialLinks = newSocialLinks,
                                                onSuccess = {
                                                    viewModel.updateLocalProfileAndSocialLinks(
                                                        penName = newPenName,
                                                        bio = newBio,
                                                        socialLinks = newSocialLinks
                                                    )
                                                    viewModel.setAuthorPenName(newPenName)
                                                    showEditMyProfileDialog = false
                                                    showFeedback(context.getString(R.string.poet_profile_saved_snackbar))
                                                },
                                                onError = ::showFeedback
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        composable(ROUTE_WRITE_STUDIO) {
                            com.example.ui.studio.ShayariWriteStudioScreen(
                                viewModel = activePersonalViewModel,
                                authorName = resolvedAuthorName,
                                onAuthorNameChanged = viewModel::setAuthorPenName,
                                onClose = { navController.popBackStack() },
                                onSaved = { wasUpdate ->
                                    showFeedback(
                                        if (wasUpdate) {
                                            "शायरी अपडेट हो गई ✨"
                                        } else {
                                            "आपकी शायरी सेव हो गई ✨"
                                        }
                                    )
                                    navController.popBackStack()
                                },
                                onFeedback = ::showFeedback,
                                onRequestShareDesignedCard = { couplet, author, title, tag, design ->
                                    shareTarget = ShareTarget.DesignedCard(
                                        couplet = couplet,
                                        authorName = author,
                                        title = title,
                                        tag = tag,
                                        design = design
                                    )
                                }
                            )
                        }

                        composable(
                            route = ROUTE_READER,
                            arguments = listOf(
                                navArgument("shayariId") { type = NavType.IntType },
                                navArgument("scope") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val shayariId = entry.arguments?.getInt("shayariId") ?: 1
                            val readerScope = entry.arguments?.getString("scope") ?: "all"
                            val isExternalShayari = readerScope == "community" || readerScope == "personal"

                            val personalUiState by activePersonalViewModel.uiState.collectAsStateWithLifecycle()
                            val communityPostsForReader = if (readerScope == "community" && sharedCommunityViewModel != null) {
                                val communityState by sharedCommunityViewModel.uiState.collectAsStateWithLifecycle()
                                (communityState.feedState as? CommunityFeedState.Success)?.posts.orEmpty()
                            } else {
                                emptyList()
                            }
                            val communityPostByEntityId = remember(communityPostsForReader) {
                                communityPostsForReader.associateBy { it.toShayariEntity().id }
                            }

                            val initialScopedIds = remember(readerScope, shayariId, uiState.isLoading) {
                                when {
                                    isExternalShayari -> emptyList()
                                    readerScope == "favorites" -> uiState.favoriteShayari.map { it.id }
                                    readerScope == "filtered" -> uiState.filteredShayari.map { it.id }
                                    readerScope.startsWith("category_") -> {
                                        val catId = readerScope.removePrefix("category_")
                                        uiState.allShayari.filter { it.categoryId == catId }.map { it.id }
                                    }
                                    else -> uiState.allShayari.map { it.id }
                                }.ifEmpty { listOf(shayariId) }
                            }
                            val allShayariById = remember(uiState.allShayari) {
                                uiState.allShayari.associateBy { it.id }
                            }
                            val readerList = remember(
                                initialScopedIds,
                                allShayariById,
                                previewCommunityEntity,
                                isExternalShayari,
                                readerScope,
                                personalUiState.allItems,
                                communityPostsForReader,
                                resolvedAuthorName
                            ) {
                                when (readerScope) {
                                    "personal" -> {
                                        personalUiState.allItems
                                            .map { it.toShayariEntity(resolvedAuthorName) }
                                            .ifEmpty { listOfNotNull(previewCommunityEntity ?: uiState.allShayari.firstOrNull()) }
                                    }
                                    "community" -> {
                                        communityPostsForReader
                                            .map { it.toShayariEntity() }
                                            .ifEmpty { listOfNotNull(previewCommunityEntity ?: uiState.allShayari.firstOrNull()) }
                                    }
                                    else -> {
                                        initialScopedIds
                                            .mapNotNull { allShayariById[it] }
                                            .ifEmpty { uiState.allShayari }
                                    }
                                }
                            }

                            ShayariReaderScreen(
                                shayariList = readerList,
                                initialShayariId = shayariId,
                                selectedCardStyle = uiState.preferences.cardStyle,
                                selectedFontScale = uiState.preferences.fontScale,
                                selectedFontStyle = uiState.preferences.fontStyle,
                                showFavoriteAction = !isExternalShayari,
                                onBack = { navController.popBackStack() },
                                onToggleFavorite = ::handleToggleFavorite,
                                onCopyText = { copiedEntity ->
                                    ShayariShareHelper.copyPlainText(
                                        context,
                                        ShayariShareHelper.formatShareText(copiedEntity)
                                    )
                                    showFeedback(context.getString(R.string.copied_snackbar))
                                },
                                onShareText = { sharedEntity ->
                                    val matchedCommunityPost = if (readerScope == "community") {
                                        communityPostByEntityId[sharedEntity.id]
                                            ?: previewCommunityPost?.takeIf { it.couplet == sharedEntity.couplet }
                                    } else {
                                        null
                                    }
                                    if (matchedCommunityPost != null) {
                                        handleShareCommunityPost(matchedCommunityPost)
                                    } else {
                                        handleShareShayari(sharedEntity)
                                    }
                                },
                                onDownloadImage = ::handleDownloadImage,
                                onCollab = { collabEntity ->
                                    val categoryTitle = ShayariCategory.fromId(collabEntity.categoryId).titleHi
                                    activePersonalViewModel.startCollabDraft(
                                        sourceCouplet = collabEntity.couplet,
                                        sourcePoet = collabEntity.poet,
                                        themeTag = categoryTitle
                                    )
                                    navController.navigate(ROUTE_WRITE_STUDIO) {
                                        launchSingleTop = true
                                    }
                                },
                                onWriteNew = { sourceEntity ->
                                    val categoryTitle = ShayariCategory.fromId(sourceEntity.categoryId).titleHi
                                    activePersonalViewModel.startWriteDraft(themeTag = categoryTitle)
                                    navController.navigate(ROUTE_WRITE_STUDIO) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showNotificationSheet) {
            PostNotificationsSheet(
                currentUserId = currentUser?.uid,
                notifications = postNotifications,
                isLoading = notificationsLoading,
                errorMessage = notificationsError,
                onNotificationClick = { notif ->
                    if (!notif.isRead && notificationsRepository != null) {
                        scope.launch {
                            notificationsRepository.markAsRead(notif.id)
                        }
                    }
                    showNotificationSheet = false
                    if (notif.notificationType == PostNotificationType.FOLLOW && notif.actorUserId.isNotBlank()) {
                        navController.navigate("poet_profile/${Uri.encode(notif.actorUserId)}") {
                            launchSingleTop = true
                        }
                    } else {
                        navigateToTab(ROUTE_ACCOUNT)
                    }
                },
                onMarkAllRead = {
                    val unreadIds = postNotifications.filter { !it.isRead }.map { it.id }
                    if (notificationsRepository != null) {
                        scope.launch {
                            val res = notificationsRepository.markAllAsRead(unreadIds)
                            if (res.isSuccess) {
                                showFeedback(context.getString(R.string.activity_notif_marked_all_read))
                            }
                        }
                    }
                },
                onDeleteNotification = { notifId ->
                    if (notificationsRepository != null) {
                        scope.launch {
                            notificationsRepository.deleteNotification(notifId)
                        }
                    }
                },
                onAuthSuccess = {
                    showFeedback(context.getString(R.string.activity_notif_auth_welcome))
                },
                onAuthError = ::showFeedback,
                onDismiss = { showNotificationSheet = false }
            )
        }

        val activeCommentTarget = commentingTarget
        if (activeCommentTarget != null) {
            ShayariCommentsSheet(
                target = activeCommentTarget,
                currentUserId = currentUser?.uid,
                comments = activeComments,
                isLoading = commentsLoading,
                isSubmitting = commentsSubmitting,
                errorMessage = commentsError,
                onAddComment = { commentText ->
                    val repo = commentsRepository ?: return@ShayariCommentsSheet
                    val authorName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "शायर दोस्त"
                    scope.launch {
                        commentsSubmitting = true
                        val result = repo.addComment(
                            shayariKey = activeCommentTarget.shayariKey,
                            authorName = authorName,
                            text = commentText
                        )
                        commentsSubmitting = false
                        if (result.isSuccess) {
                            val postId = activeCommentTarget.communityPostId
                            val recipientId = activeCommentTarget.recipientUserId
                            if (postId != null && recipientId != null && recipientId != currentUser?.uid && notificationsRepository != null) {
                                notificationsRepository.sendNotification(
                                    recipientUserId = recipientId,
                                    actorName = authorName,
                                    type = PostNotificationType.COMMENT,
                                    postId = postId,
                                    postCouplet = activeCommentTarget.couplet,
                                    commentText = commentText
                                )
                            }
                            showFeedback(context.getString(R.string.comment_added_snackbar))
                        } else {
                            showFeedback(context.getString(R.string.comment_error_snackbar))
                        }
                    }
                },
                onDeleteComment = { commentId ->
                    val repo = commentsRepository ?: return@ShayariCommentsSheet
                    scope.launch {
                        val result = repo.deleteComment(commentId)
                        if (result.isSuccess) {
                            showFeedback(context.getString(R.string.comment_deleted_snackbar))
                        } else {
                            showFeedback(context.getString(R.string.comment_error_snackbar))
                        }
                    }
                },
                onAuthSuccess = {
                    showFeedback("लॉगिन हो गया, अब आप कमेंट लिख सकते हैं!")
                },
                onAuthError = ::showFeedback,
                onDismiss = { commentingTarget = null }
            )
        }

        val pendingShareTarget = shareTarget
        if (pendingShareTarget != null) {
            ShareShayariSheet(
                target = pendingShareTarget,
                isPreparingImage = isPreparingShareImage,
                onShareToPlatform = ::executeShareToPlatform,
                onShareMore = ::executeShareMore,
                onCopy = ::executeCopyShareTarget,
                onDownload = ::executeDownloadShareTarget,
                onDismiss = {
                    if (!isPreparingShareImage) {
                        shareTarget = null
                    }
                }
            )
        }
    }
}
