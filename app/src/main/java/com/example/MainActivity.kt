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
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.SmartDisplay
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.audio.ReadingAmbientPlayer
import com.example.audio.ReadingAmbientTrack
import com.example.audio.ReadingSleepTimerOption
import com.example.auth.attemptAutoSignIn
import com.example.auth.isAppOwner
import com.example.auth.isFirebaseAvailable
import com.example.auth.isLocalOwnerAccount
import com.example.auth.onGoogleSignInClicked
import com.example.auth.safeAuthStateFlow
import com.example.auth.safeCurrentUser
import com.example.auth.signOutFromGoogle
import com.example.data.local.LocalPersonalShayariRepository
import com.example.data.local.LoginLocalResult
import com.example.data.local.ShayariDatabase
import com.example.data.local.ShayariRepository
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.CommunityReel
import com.example.data.model.CommunityShayari
import com.example.data.model.LOCAL_GUEST_USER_ID
import com.example.data.model.PersonalShayari
import com.example.data.model.PoetProfile
import com.example.data.model.PostNotification
import com.example.data.model.PostNotificationType
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariComment
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariTagMatcher
import com.example.data.model.ThemeModeOption
import com.example.data.model.shayariCommentKey
import com.example.data.model.toShayariEntity
import com.example.data.remote.AdminDashboardRepository
import com.example.data.remote.CommunityReelsRepository
import com.example.data.remote.CommunityShayariRepository
import com.example.data.remote.PersonalShayariRepository
import com.example.data.remote.PostNotificationsRepository
import com.example.data.remote.ShayariCommentsRepository
import com.example.notifications.DailyShayariNotificationHelper
import com.example.notifications.DailyShayariScheduler
import com.example.ui.components.AccountAvatar
import com.example.ui.components.FloatingReadingMusicBar
import com.example.ui.components.ProfileManagementSheet
import com.example.ui.components.ReadingMusicSheet
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
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.ShayariCommentsSheet
import com.example.ui.screens.ShayariReaderScreen
import com.example.ui.screens.UserCreatedReel
import com.example.ui.theme.ShayariReadingStyle
import com.example.ui.theme.ShayariTheme
import com.example.ui.viewmodel.AdminDashboardUiState
import com.example.ui.viewmodel.AdminDashboardViewModel
import com.example.ui.viewmodel.CommunityFeedState
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.PersonalShayariViewModel
import com.example.ui.viewmodel.ShayariViewModel
import com.example.util.ProfilePhotoStorage
import com.example.util.ReelsStorage
import com.example.util.ShayariShareHelper
import com.example.util.SocialShareLauncher
import com.example.util.SocialSharePlatform
import com.example.util.SocialShareResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlin.math.ceil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

const val ROUTE_MEHFIL = "mehfil"
const val ROUTE_REELS = "reels"
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
    REELS(
        route = ROUTE_REELS,
        labelRes = R.string.nav_reels,
        selectedIcon = Icons.Rounded.SmartDisplay,
        unselectedIcon = Icons.Outlined.SmartDisplay,
        testTag = "nav_tab_reels"
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
    var userCreatedReels by remember { mutableStateOf<List<UserCreatedReel>>(emptyList()) }
    var onlineCommunityReels by remember { mutableStateOf<List<CommunityReel>>(emptyList()) }
    var onlineLikedReelIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var reelsCreateTrigger by remember { mutableIntStateOf(0) }
    var showReadingMusicSheet by remember { mutableStateOf(false) }
    var activeReaderCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedSleepTimer by remember { mutableStateOf(ReadingSleepTimerOption.CONTINUOUS) }
    var sleepTimerDeadlineMs by remember { mutableLongStateOf(0L) }
    var remainingSleepTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var isAppInForeground by remember { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val readingAmbientPlayer = remember(context) {
        ReadingAmbientPlayer(
            context = context,
            onAudioFocusLost = {
                viewModel.setReadingBgmEnabled(false)
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppInForeground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isAppInForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(readingAmbientPlayer) {
        onDispose {
            readingAmbientPlayer.release()
        }
    }

    val selectedReadingTrack = remember(uiState.preferences.readingBgmTrackId) {
        ReadingAmbientTrack.fromId(uiState.preferences.readingBgmTrackId)
    }
    val moodContextCategoryId = when (currentRoute) {
        ROUTE_READER -> activeReaderCategoryId ?: uiState.selectedCategoryId
        ROUTE_CATEGORY_DETAIL -> navBackStackEntry?.arguments?.getString("categoryId")
        else -> uiState.selectedCategoryId
    }
    val effectiveReadingTrack = remember(
        uiState.preferences.readingBgmAutoMood,
        selectedReadingTrack,
        moodContextCategoryId
    ) {
        if (uiState.preferences.readingBgmAutoMood && !moodContextCategoryId.isNullOrBlank()) {
            ReadingAmbientTrack.recommendedForCategory(moodContextCategoryId)
        } else {
            selectedReadingTrack
        }
    }
    val shouldPlayReadingMusic = uiState.preferences.readingBgmEnabled &&
        isAppInForeground &&
        currentRoute != ROUTE_REELS

    LaunchedEffect(
        shouldPlayReadingMusic,
        effectiveReadingTrack,
        uiState.preferences.readingBgmVolume
    ) {
        readingAmbientPlayer.updateState(
            shouldPlay = shouldPlayReadingMusic,
            track = effectiveReadingTrack,
            volume = uiState.preferences.readingBgmVolume
        )
    }

    LaunchedEffect(
        selectedSleepTimer,
        sleepTimerDeadlineMs,
        uiState.preferences.readingBgmEnabled
    ) {
        if (selectedSleepTimer == ReadingSleepTimerOption.CONTINUOUS ||
            sleepTimerDeadlineMs <= 0L ||
            !uiState.preferences.readingBgmEnabled
        ) {
            remainingSleepTimerMinutes = null
            return@LaunchedEffect
        }
        while (true) {
            val remainingMs = sleepTimerDeadlineMs - System.currentTimeMillis()
            if (remainingMs <= 0L) {
                remainingSleepTimerMinutes = null
                selectedSleepTimer = ReadingSleepTimerOption.CONTINUOUS
                sleepTimerDeadlineMs = 0L
                readingAmbientPlayer.fadeOutAndStop {
                    viewModel.setReadingBgmEnabled(false)
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.reading_music_timer_finished)
                        )
                    }
                }
                break
            }
            remainingSleepTimerMinutes = ceil(remainingMs / 60_000.0).toInt().coerceAtLeast(1)
            delay(minOf(10_000L, remainingMs))
        }
    }

    LaunchedEffect(context) {
        val persisted = ReelsStorage.loadUserReels(context)
        if (persisted.isNotEmpty()) {
            userCreatedReels = persisted
        }
    }

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
    val reelsRepository = remember(context, firebaseReady) {
        if (firebaseReady) CommunityReelsRepository(context.applicationContext) else null
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
    val sharedCommunityUiState = sharedCommunityViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val liveOnlineCommunityPosts = remember(sharedCommunityUiState?.feedState) {
        (sharedCommunityUiState?.feedState as? CommunityFeedState.Success)?.posts.orEmpty()
    }
    LaunchedEffect(liveOnlineCommunityPosts, currentUser?.uid) {
        if (currentUser != null) {
            viewModel.updateOnlineCommunityPosts(liveOnlineCommunityPosts)
        } else {
            viewModel.updateOnlineCommunityPosts(emptyList())
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
    var skippedGooglePostPrompt by rememberSaveable { mutableStateOf(false) }
    var isInstantPublishing by remember { mutableStateOf(false) }

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
            onlineCommunityReels = emptyList()
            onlineLikedReelIds = emptySet()
        } else {
            launch {
                runCatching {
                    val cloudRepo = PersonalShayariRepository(context.applicationContext)
                    val migratedItems = localPersonalRepository.migrateAllToCloudReturningItems(cloudRepo)
                    if (migratedItems.isNotEmpty()) {
                        val communityRepo = CommunityShayariRepository(context.applicationContext)
                        val migrationAuthor = uiState.preferences.authorPenName.trim().ifBlank {
                            uiState.preferences.localAccount.fullName.trim()
                                .takeIf { uiState.preferences.localAccount.isLoggedIn && it.isNotBlank() }
                                ?: currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() }
                                ?: "शायर दोस्त"
                        }
                        var syncedOnlineCount = 0
                        migratedItems.forEach { item ->
                            val cleanCouplet = item.couplet.trim()
                            if (cleanCouplet.length >= 10) {
                                val primaryCatId = ShayariTagMatcher.resolvePrimaryCategory(
                                    rawTag = item.themeTag
                                ).id
                                val hashTagsStr = ShayariTagMatcher.parseTags(item.themeTag)
                                    .take(5)
                                    .joinToString(" ") { "#$it" }
                                val promptTitle = listOf(item.title.trim(), hashTagsStr)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • ")
                                    .take(120)
                                val res = communityRepo.publishShayari(
                                    penName = migrationAuthor,
                                    couplet = cleanCouplet,
                                    categoryId = primaryCatId,
                                    promptTitle = promptTitle
                                )
                                if (res.isSuccess) {
                                    syncedOnlineCount++
                                }
                            }
                        }
                        if (syncedOnlineCount > 0) {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                "आपकी $syncedOnlineCount शायरी ऑनलाइन कुल पोस्ट में जुड़ गईं 🌐✨"
                            )
                        }
                    }
                }
            }
            if (reelsRepository != null) {
                launch {
                    reelsRepository.observePublishedReels()
                        .catch { /* Keep previous reels on transient failure */ }
                        .collect { onlineCommunityReels = it }
                }
                launch {
                    reelsRepository.observeUserLikedReelIds()
                        .catch { /* Keep previous likes on transient failure */ }
                        .collect { onlineLikedReelIds = it }
                }
                // Auto-sync all existing local Reels online so every user Reel goes online!
                launch {
                    runCatching {
                        val localReels = ReelsStorage.loadUserReels(context)
                        val unsynced = localReels.filter { !it.isOnlineSynced }
                        if (unsynced.isNotEmpty()) {
                            var changed = false
                            val updatedList = localReels.map { localReel ->
                                if (localReel.isOnlineSynced) {
                                    localReel
                                } else {
                                    val thumb = localReel.thumbnailBase64?.takeIf { it.isNotBlank() }
                                        ?: when {
                                            localReel.videoUri != null -> ReelsStorage.extractCompressedThumbnailBase64(
                                                context = context,
                                                uriString = localReel.videoUri,
                                                isVideo = true
                                            )
                                            localReel.photoUri != null -> ReelsStorage.extractCompressedThumbnailBase64(
                                                context = context,
                                                uriString = localReel.photoUri,
                                                isVideo = false
                                            )
                                            else -> null
                                        }
                                    val mediaType = when {
                                        localReel.videoUri != null -> "video"
                                        localReel.photoUri != null -> "photo"
                                        else -> localReel.mediaType.ifBlank { "text" }
                                    }
                                    val res = reelsRepository.publishReel(
                                        reelId = localReel.id,
                                        authorName = localReel.poet.ifBlank {
                                            currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() } ?: "शायर दोस्त"
                                        },
                                        caption = localReel.couplet,
                                        tags = localReel.tags,
                                        mediaType = mediaType,
                                        backgroundPreset = localReel.backgroundDrawableName.orEmpty(),
                                        thumbnailBase64 = thumb.orEmpty(),
                                        voicePersonaId = localReel.voicePersonaId.orEmpty(),
                                        bgmTrackId = localReel.bgmTrackId.orEmpty(),
                                        singerName = localReel.singerName.orEmpty(),
                                        songTitle = localReel.songTitle.orEmpty()
                                    )
                                    if (res.isSuccess) {
                                        changed = true
                                        localReel.copy(
                                            ownerUserId = uid,
                                            mediaType = mediaType,
                                            thumbnailBase64 = thumb,
                                            isOnlineSynced = true
                                        )
                                    } else {
                                        localReel
                                    }
                                }
                            }
                            if (changed) {
                                userCreatedReels = updatedList
                                ReelsStorage.saveUserReels(context, updatedList)
                            }
                        }
                    }
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
        ROUTE_REELS,
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
        if (currentRoute == ROUTE_REELS) {
            reelsCreateTrigger++
        } else {
            openWriteStudio(resetDraft = true)
        }
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
                SocialShareResult.LAUNCHED_NATIVE_APP -> {
                    if (platform == SocialSharePlatform.INSTAGRAM) {
                        ShayariShareHelper.copyPlainText(context, ShayariShareHelper.appDownloadUrl())
                        showFeedback("डाउनलोड लिंक कॉपी हो गया — Instagram Story के Link Sticker में पेस्ट करें 🔗")
                    }
                    if (target is ShareTarget.Community) notifyCommunityShareIfNeeded(target.post)
                }
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
        val textToCopy = if (target is ShareTarget.App) {
            ShayariShareHelper.appDownloadUrl()
        } else {
            target.toShareText(context)
        }
        ShayariShareHelper.copyPlainText(context, textToCopy)
        shareTarget = null
        showFeedback(
            if (target is ShareTarget.App) {
                "सीधा डाउनलोड लिंक कॉपी हो गया (कहीं भी पेस्ट करें) 🔗"
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

    val activePersonalUiState by activePersonalViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(activePersonalUiState.allItems, resolvedAuthorName) {
        val items = activePersonalUiState.allItems
        if (items.isNotEmpty()) {
            val author = resolvedAuthorName.ifBlank { "शायर दोस्त" }
            items.forEach { item ->
                if (item.id.isNotBlank() && item.couplet.isNotBlank()) {
                    viewModel.upsertUserPostToCatalog(
                        personalId = item.id,
                        couplet = item.couplet,
                        authorName = author,
                        title = item.title,
                        themeTag = item.themeTag
                    )
                }
            }
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

        val isReelsRoute = currentRoute == ROUTE_REELS
        val isFullScreenStudioOrReader = currentRoute == ROUTE_READER ||
            currentRoute == ROUTE_WRITE_STUDIO ||
            currentRoute == ROUTE_EDIT_PROFILE ||
            isReelsRoute

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isReelsRoute) Color.Black else MaterialTheme.colorScheme.background,
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
                        onAccountClick = ::handleAccountTabClick,
                        isReelsSelected = isReelsRoute,
                        onReelsClick = { navigateToTab(ROUTE_REELS) }
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
                        containerColor = if (isReelsRoute) Color(0xFF0A0A0E) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        val railItemColors = if (isReelsRoute) {
                            NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color.White.copy(alpha = 0.16f),
                                unselectedIconColor = Color.White.copy(alpha = 0.65f),
                                unselectedTextColor = Color.White.copy(alpha = 0.65f)
                            )
                        } else {
                            NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                                },
                                isReadingMusicPlaying = shouldPlayReadingMusic,
                                onOpenReadingMusic = { showReadingMusicSheet = true }
                            )
                        }

                        composable(ROUTE_REELS) {
                            val mergedOnlineAndLocalReels = remember(onlineCommunityReels, userCreatedReels) {
                                val localById = userCreatedReels.associateBy { it.id }
                                val onlineMapped = onlineCommunityReels.map { remote ->
                                    val localMatch = localById[remote.id]
                                    UserCreatedReel(
                                        id = remote.id,
                                        couplet = remote.caption,
                                        poet = remote.authorName.ifBlank { "शायर दोस्त" },
                                        categoryId = "",
                                        tags = remote.tags,
                                        videoUri = localMatch?.videoUri,
                                        photoUri = localMatch?.photoUri,
                                        backgroundDrawableName = remote.backgroundPreset.takeIf { it.isNotBlank() }
                                            ?: localMatch?.backgroundDrawableName,
                                        voicePersonaId = remote.voicePersonaId.takeIf { it.isNotBlank() }
                                            ?: localMatch?.voicePersonaId,
                                        bgmTrackId = remote.bgmTrackId.takeIf { it.isNotBlank() }
                                            ?: localMatch?.bgmTrackId,
                                        ownerUserId = remote.userId.takeIf { it.isNotBlank() },
                                        mediaType = remote.mediaType.ifBlank { localMatch?.mediaType ?: "text" },
                                        thumbnailBase64 = remote.thumbnailBase64.takeIf { it.isNotBlank() }
                                            ?: localMatch?.thumbnailBase64,
                                        singerName = remote.singerName.takeIf { it.isNotBlank() }
                                            ?: localMatch?.singerName,
                                        songTitle = remote.songTitle.takeIf { it.isNotBlank() }
                                            ?: localMatch?.songTitle,
                                        singerChecked = localMatch?.singerChecked
                                            ?: remote.singerName.isNotBlank(),
                                        likeCount = remote.likeCount,
                                        isOnlineSynced = true,
                                        createdAtMs = remote.createdAtMillis.takeIf { it > 0L }
                                            ?: localMatch?.createdAtMs
                                            ?: System.currentTimeMillis()
                                    )
                                }
                                val onlineIds = onlineMapped.mapTo(HashSet()) { it.id }
                                val localOnly = userCreatedReels.filterNot { it.id in onlineIds }
                                (onlineMapped + localOnly).sortedByDescending { it.createdAtMs }
                            }
                            ReelsScreen(
                                allShayari = uiState.allShayari,
                                popularShayari = uiState.popularShayari,
                                userReels = mergedOnlineAndLocalReels,
                                commentCounts = commentCounts,
                                authorName = resolvedAuthorName,
                                onToggleFavorite = ::handleToggleFavorite,
                                onComment = ::openCommentsForShayari,
                                onShare = ::handleShareShayari,
                                onCreateUserReel = { newReel ->
                                    val signedInUid = currentUser?.uid
                                    val enrichedReel = newReel.copy(
                                        ownerUserId = signedInUid,
                                        isOnlineSynced = signedInUid != null && reelsRepository != null
                                    )
                                    val updated = listOf(enrichedReel) + userCreatedReels.filterNot { it.id == enrichedReel.id }
                                    userCreatedReels = updated
                                    scope.launch {
                                        ReelsStorage.saveUserReels(context, updated)
                                        if (signedInUid != null && reelsRepository != null) {
                                            val result = reelsRepository.publishReel(
                                                reelId = enrichedReel.id,
                                                authorName = enrichedReel.poet.ifBlank { resolvedAuthorName },
                                                caption = enrichedReel.couplet,
                                                tags = enrichedReel.tags,
                                                mediaType = enrichedReel.mediaType,
                                                backgroundPreset = enrichedReel.backgroundDrawableName.orEmpty(),
                                                thumbnailBase64 = enrichedReel.thumbnailBase64.orEmpty(),
                                                voicePersonaId = enrichedReel.voicePersonaId.orEmpty(),
                                                bgmTrackId = enrichedReel.bgmTrackId.orEmpty(),
                                                singerName = enrichedReel.singerName.orEmpty(),
                                                songTitle = enrichedReel.songTitle.orEmpty()
                                            )
                                            if (result.isFailure) {
                                                val fallbackList = userCreatedReels.map {
                                                    if (it.id == enrichedReel.id) it.copy(isOnlineSynced = false) else it
                                                }
                                                userCreatedReels = fallbackList
                                                ReelsStorage.saveUserReels(context, fallbackList)
                                                showFeedback("ऑनलाइन सिंक में समस्या आई — लोकल सेव है, इंटरनेट पर फिर सिंक होगी")
                                            }
                                        }
                                    }
                                    if (enrichedReel.mediaType == "text" && enrichedReel.couplet.trim().length >= 8) {
                                        viewModel.upsertUserPostToCatalog(
                                            personalId = enrichedReel.id,
                                            couplet = enrichedReel.couplet.trim(),
                                            authorName = enrichedReel.poet.ifBlank { "शायर दोस्त" },
                                            title = "",
                                            themeTag = enrichedReel.tags.joinToString(" ")
                                        )
                                    }
                                },
                                onShowFeedback = ::showFeedback,
                                createSheetTrigger = reelsCreateTrigger,
                                onDeleteUserReel = { target ->
                                    val updated = userCreatedReels.filterNot { it.id == target.id }
                                    userCreatedReels = updated
                                    scope.launch {
                                        ReelsStorage.deleteLocalMediaIfOwned(context, target.videoUri)
                                        ReelsStorage.deleteLocalMediaIfOwned(context, target.photoUri)
                                        ReelsStorage.saveUserReels(context, updated)
                                        if (currentUser != null && reelsRepository != null &&
                                            (target.ownerUserId == null || target.ownerUserId == currentUser?.uid)
                                        ) {
                                            reelsRepository.deleteReel(target.id)
                                        }
                                    }
                                },
                                onUpdateUserReelSinger = { target, detectedSinger, detectedSong ->
                                    val cleanSinger = detectedSinger?.trim()?.take(60)?.takeIf { it.isNotBlank() }
                                    val cleanSong = detectedSong?.trim()?.take(80)?.takeIf { it.isNotBlank() }
                                    val existingInLocal = userCreatedReels.any { it.id == target.id }
                                    val updatedLocal = if (existingInLocal) {
                                        userCreatedReels.map {
                                            if (it.id == target.id) {
                                                it.copy(
                                                    singerName = cleanSinger,
                                                    songTitle = cleanSong,
                                                    singerChecked = true
                                                )
                                            } else {
                                                it
                                            }
                                        }
                                    } else {
                                        listOf(
                                            target.copy(
                                                singerName = cleanSinger,
                                                songTitle = cleanSong,
                                                singerChecked = true
                                            )
                                        ) + userCreatedReels
                                    }
                                    userCreatedReels = updatedLocal
                                    onlineCommunityReels = onlineCommunityReels.map { remote ->
                                        if (remote.id == target.id) {
                                            remote.copy(
                                                singerName = cleanSinger.orEmpty(),
                                                songTitle = cleanSong.orEmpty()
                                            )
                                        } else {
                                            remote
                                        }
                                    }
                                    scope.launch {
                                        ReelsStorage.saveUserReels(context, updatedLocal)
                                        if (currentUser != null && reelsRepository != null &&
                                            (target.ownerUserId == null || target.ownerUserId == currentUser?.uid)
                                        ) {
                                            reelsRepository.updateReelSinger(
                                                reelId = target.id,
                                                singerName = cleanSinger.orEmpty(),
                                                songTitle = cleanSong.orEmpty()
                                            )
                                        }
                                    }
                                },
                                currentUserId = currentUser?.uid,
                                onlineLikedReelIds = onlineLikedReelIds,
                                isSignedInOnline = currentUser != null && firebaseReady,
                                onToggleOnlineReelLike = { reelId, currentlyLiked ->
                                    scope.launch {
                                        reelsRepository?.toggleLike(reelId, currentlyLiked)
                                    }
                                },
                                onRequestGoogleSignIn = if (firebaseReady) {
                                    {
                                        onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            scope = scope,
                                            onAuthSuccess = {
                                                showFeedback("Google से जुड़ गए! अब आपकी सभी Reels ऑनलाइन होंगी 🌐✨")
                                            },
                                            onAuthError = { err ->
                                                showFeedback(err)
                                            },
                                            onAuthCancelled = {}
                                        )
                                    }
                                } else {
                                    null
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
                                        onQuickCreateNameAccount = { newName ->
                                            viewModel.quickCreateNameAccount(newName) {
                                                showFeedback("स्वागत है, $newName! आपका अकाउंट बन गया ✨")
                                            }
                                        },
                                        onPersonalPostDeleted = viewModel::removeUserPostFromCatalog,
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
                                        onPersonalPostDeleted = viewModel::removeUserPostFromCatalog,
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
                                isOnlineAccount = currentUser != null,
                                isConnectingGoogle = isInstantPublishing,
                                onRequestGoogleSignIn = {
                                    if (!isInstantPublishing) {
                                        isInstantPublishing = true
                                        onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            scope = scope,
                                            onAuthSuccess = {
                                                isInstantPublishing = false
                                                skippedGooglePostPrompt = false
                                                showFeedback("Google से जुड़ गए! अब आपकी शायरी ऑनलाइन पब्लिश होगी 🌐✨")
                                            },
                                            onAuthError = { err ->
                                                isInstantPublishing = false
                                                showFeedback(err)
                                            },
                                            onAuthCancelled = {
                                                isInstantPublishing = false
                                            }
                                        )
                                    }
                                },
                                onInstantOnlinePublish = { couplet, authorFromStudio, title, primaryCategoryId, tagsCsv, onFallbackLocal ->
                                    if (!firebaseReady || skippedGooglePostPrompt) {
                                        onFallbackLocal()
                                    } else if (!isInstantPublishing) {
                                        isInstantPublishing = true
                                        onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            scope = scope,
                                            onAuthSuccess = {
                                                skippedGooglePostPrompt = false
                                                scope.launch {
                                                    try {
                                                        val signedInUser = safeCurrentUser(context)
                                                        val effectiveAuthor = authorFromStudio.trim().ifBlank {
                                                            resolvedAuthorName.ifBlank {
                                                                signedInUser?.displayName?.trim()?.takeIf { it.isNotBlank() }
                                                                    ?: "शायर दोस्त"
                                                            }
                                                        }
                                                        if (authorFromStudio.trim().length >= 2) {
                                                            viewModel.setAuthorPenName(authorFromStudio.trim())
                                                        }
                                                        val cloudPersonalRepo = PersonalShayariRepository(context.applicationContext)
                                                        val savedPersonalId = cloudPersonalRepo.savePersonalShayari(
                                                            title = title.trim(),
                                                            themeTag = ShayariTagMatcher.trimTagsForCloudPersonal(tagsCsv),
                                                            couplet = couplet.trim()
                                                        ).getOrDefault("instant_${System.currentTimeMillis()}")

                                                        val hashTagsStr = ShayariTagMatcher.parseTags(tagsCsv)
                                                            .take(5)
                                                            .joinToString(" ") { "#$it" }
                                                        val communityPromptTitle = listOf(title.trim(), hashTagsStr)
                                                            .filter { it.isNotBlank() }
                                                            .joinToString(" • ")
                                                            .take(120)
                                                        val communityRepo = CommunityShayariRepository(context.applicationContext)
                                                        val publishRes = communityRepo.publishShayari(
                                                            penName = effectiveAuthor,
                                                            couplet = couplet.trim(),
                                                            categoryId = primaryCategoryId,
                                                            promptTitle = communityPromptTitle
                                                        )
                                                        viewModel.upsertUserPostToCatalog(
                                                            personalId = savedPersonalId,
                                                            couplet = couplet.trim(),
                                                            authorName = effectiveAuthor,
                                                            title = title.trim(),
                                                            themeTag = tagsCsv,
                                                            primaryCategoryId = primaryCategoryId
                                                        )
                                                        showFeedback(
                                                            if (publishRes.isSuccess) {
                                                                "आपकी शायरी ऑनलाइन पब्लिश होकर कुल पोस्ट में जुड़ गई 🌐✨"
                                                            } else {
                                                                "शायरी सेव हो गई ✨"
                                                            }
                                                        )
                                                        navController.popBackStack()
                                                    } finally {
                                                        isInstantPublishing = false
                                                    }
                                                }
                                            },
                                            onAuthCancelled = {
                                                isInstantPublishing = false
                                                skippedGooglePostPrompt = true
                                                onFallbackLocal()
                                            },
                                            onAuthError = { err ->
                                                isInstantPublishing = false
                                                onFallbackLocal()
                                                showFeedback("फ़ोन में सेव हुई — Google लॉगिन त्रुटि: $err")
                                            }
                                        )
                                    }
                                },
                                onAuthorNameChanged = viewModel::setAuthorPenName,
                                onClose = { navController.popBackStack() },
                                onPostPublishedWithCategory = { personalId, couplet, authorFromStudio, title, primaryCategoryId, tagsCsv, wasUpdate ->
                                    val effectiveAuthor = authorFromStudio.trim().ifBlank {
                                        resolvedAuthorName.ifBlank { "शायर दोस्त" }
                                    }
                                    if (!isAnySignedIn && authorFromStudio.trim().length >= 2) {
                                        viewModel.quickCreateNameAccount(authorFromStudio.trim())
                                    }
                                    viewModel.upsertUserPostToCatalog(
                                        personalId = personalId,
                                        couplet = couplet,
                                        authorName = effectiveAuthor,
                                        title = title,
                                        themeTag = tagsCsv,
                                        primaryCategoryId = primaryCategoryId
                                    )
                                    val activeCommunityVm = sharedCommunityViewModel
                                    if (!wasUpdate && currentUser != null && activeCommunityVm != null && couplet.trim().length >= 10) {
                                        val hashTagsStr = ShayariTagMatcher.parseTags(tagsCsv)
                                            .take(5)
                                            .joinToString(" ") { "#$it" }
                                        val communityPromptTitle = listOf(title.trim(), hashTagsStr)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" • ")
                                            .take(120)
                                        activeCommunityVm.publishOrUpdateShayari(
                                            editingId = null,
                                            penName = effectiveAuthor,
                                            couplet = couplet.trim(),
                                            categoryId = primaryCategoryId,
                                            promptTitle = communityPromptTitle,
                                            onSuccess = {},
                                            onError = ::showFeedback
                                        )
                                    }
                                },
                                onSaved = { wasUpdate ->
                                    showFeedback(
                                        when {
                                            wasUpdate -> "शायरी अपडेट हो गई ✨"
                                            currentUser != null -> "आपकी शायरी ऑनलाइन पब्लिश होकर कुल पोस्ट में जुड़ गई 🌐✨"
                                            else -> "शायरी फ़ोन में सेव हो गई ✨"
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
                                },
                                isAmbientMusicPlaying = shouldPlayReadingMusic,
                                activeAmbientTrack = effectiveReadingTrack,
                                onToggleAmbientMusic = {
                                    viewModel.setReadingBgmEnabled(!uiState.preferences.readingBgmEnabled)
                                },
                                onOpenAmbientMusicConfig = { showReadingMusicSheet = true },
                                onCurrentCategoryChanged = { catId ->
                                    activeReaderCategoryId = catId
                                }
                            )
                        }
                    }

                    val showFloatingMusicBar = uiState.preferences.readingBgmEnabled &&
                        currentRoute in setOf(
                            ROUTE_MEHFIL,
                            ROUTE_FAVORITES,
                            ROUTE_CATEGORY_DETAIL,
                            ROUTE_KAVI_MANCH,
                            ROUTE_POET_PROFILE
                        )
                    FloatingReadingMusicBar(
                        visible = showFloatingMusicBar,
                        isPlaying = shouldPlayReadingMusic,
                        activeTrack = effectiveReadingTrack,
                        remainingTimerMinutes = remainingSleepTimerMinutes,
                        onTogglePlay = {
                            viewModel.setReadingBgmEnabled(!uiState.preferences.readingBgmEnabled)
                        },
                        onOpenConfig = { showReadingMusicSheet = true },
                        onStopAndDismiss = {
                            selectedSleepTimer = ReadingSleepTimerOption.CONTINUOUS
                            sleepTimerDeadlineMs = 0L
                            remainingSleepTimerMinutes = null
                            viewModel.setReadingBgmEnabled(false)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
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

        if (showReadingMusicSheet) {
            ReadingMusicSheet(
                isEnabled = uiState.preferences.readingBgmEnabled,
                selectedTrack = selectedReadingTrack,
                effectiveTrack = effectiveReadingTrack,
                volume = uiState.preferences.readingBgmVolume,
                autoMoodEnabled = uiState.preferences.readingBgmAutoMood,
                selectedTimer = selectedSleepTimer,
                remainingTimerMinutes = remainingSleepTimerMinutes,
                onToggleEnabled = { enabled ->
                    viewModel.setReadingBgmEnabled(enabled)
                    if (enabled && selectedSleepTimer != ReadingSleepTimerOption.CONTINUOUS) {
                        sleepTimerDeadlineMs = System.currentTimeMillis() + selectedSleepTimer.minutes * 60_000L
                    } else if (!enabled) {
                        sleepTimerDeadlineMs = 0L
                        remainingSleepTimerMinutes = null
                    }
                },
                onSelectTrack = { track ->
                    viewModel.setReadingBgmTrack(track, enablePlayback = true)
                    if (selectedSleepTimer != ReadingSleepTimerOption.CONTINUOUS && sleepTimerDeadlineMs <= 0L) {
                        sleepTimerDeadlineMs = System.currentTimeMillis() + selectedSleepTimer.minutes * 60_000L
                    }
                },
                onVolumeChanged = { vol ->
                    viewModel.setReadingBgmVolume(vol)
                },
                onAutoMoodChanged = { autoMood ->
                    viewModel.setReadingBgmAutoMood(autoMood)
                },
                onSelectTimer = { option ->
                    selectedSleepTimer = option
                    if (option == ReadingSleepTimerOption.CONTINUOUS) {
                        sleepTimerDeadlineMs = 0L
                        remainingSleepTimerMinutes = null
                    } else {
                        sleepTimerDeadlineMs = System.currentTimeMillis() + option.minutes * 60_000L
                        if (!uiState.preferences.readingBgmEnabled) {
                            viewModel.setReadingBgmEnabled(true)
                        }
                    }
                },
                onDismiss = { showReadingMusicSheet = false }
            )
        }
    }
}
