package com.example.ui.screens

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.ShayarVoiceEngine
import com.example.ai.ShayarVoicePersona
import com.example.ai.ShayarVoiceResult
import com.example.ai.VideoSingerDetector
import com.example.audio.LocalShayarFallbackSpeaker
import com.example.audio.ReelBgmTrack
import com.example.audio.ShayariBgmPlayer
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariTagMatcher
import com.example.data.model.shayariCommentKey
import com.example.ui.components.OptimizedDrawableImage
import com.example.ui.components.ReelSoundMixerSheet
import com.example.ui.components.ReelVoiceStatusPill
import com.example.ui.components.ShayarVoicePlaybackState
import com.example.ui.components.rememberPagerRevealProgress
import com.example.ui.components.resolveDiversePhotoForCard
import com.example.ui.theme.LocalShayariReadingStyle
import com.example.util.ReelsStorage
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ReelsFeedTab(val labelHi: String) {
    FOR_YOU("For You"),
    TRENDING("ट्रेंडिंग"),
    MY_REELS("मेरी Reels")
}

data class UserCreatedReel(
    val id: String,
    val couplet: String,
    val poet: String,
    val categoryId: String = "",
    val tags: List<String> = emptyList(),
    val videoUri: String? = null,
    val photoUri: String? = null,
    val backgroundDrawableName: String? = null,
    val voicePersonaId: String? = null,
    val bgmTrackId: String? = null,
    val ownerUserId: String? = null,
    val mediaType: String = "text",
    val thumbnailBase64: String? = null,
    val singerName: String? = null,
    val songTitle: String? = null,
    val singerChecked: Boolean = false,
    val likeCount: Int = 0,
    val isOnlineSynced: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)

private val USER_HASHTAG_REGEX = Regex("#([\\p{L}\\p{M}\\p{N}_]{1,28})")

fun extractUserHashtags(rawText: String): List<String> {
    if (rawText.isBlank()) return emptyList()
    return USER_HASHTAG_REGEX.findAll(rawText)
        .map { it.groupValues[1].trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .take(10)
        .toList()
}

private data class ReelBackdropPreset(
    val key: String,
    val labelHi: String,
    val resId: Int
)

private val BUILT_IN_REEL_BACKDROPS = listOf(
    ReelBackdropPreset("moonlit_boat", "चाँदनी रात", R.drawable.img_pic_moonlit_boat),
    ReelBackdropPreset("rainy_window", "बारिश खिड़की", R.drawable.img_pic_rainy_window),
    ReelBackdropPreset("red_roses", "गुलाब रात", R.drawable.img_pic_red_roses_night),
    ReelBackdropPreset("sunset_silhouettes", "ढलती शाम", R.drawable.img_pic_sunset_silhouettes),
    ReelBackdropPreset("autumn_road", "पतझड़ राहें", R.drawable.img_pic_autumn_road),
    ReelBackdropPreset("candle_lake", "दीया झील", R.drawable.img_bg_candle_lake),
    ReelBackdropPreset("romantic", "मोहब्बत", R.drawable.img_bg_romantic),
    ReelBackdropPreset("prakriti_rain", "बरसात", R.drawable.img_bg_prakriti_rain),
    ReelBackdropPreset("bewafai", "तन्हाई", R.drawable.img_bg_bewafai),
    ReelBackdropPreset("zindagi", "ज़िंदगी", R.drawable.img_bg_zindagi),
    ReelBackdropPreset("dosti", "दोस्ती", R.drawable.img_bg_dosti),
    ReelBackdropPreset("motivation", "हौसला", R.drawable.img_bg_motivation)
)

private fun resolvePresetBackdropRes(name: String?): Int? {
    if (name.isNullOrBlank()) return null
    return BUILT_IN_REEL_BACKDROPS.firstOrNull { it.key == name }?.resId
}

private sealed interface ReelEntry {
    val stableKey: String
    val couplet: String
    val poet: String
    val categoryId: String
    val tags: List<String>

    data class ShayariReel(val shayari: ShayariEntity) : ReelEntry {
        override val stableKey: String = "shayari_${shayari.id}"
        override val couplet: String = shayari.couplet
        override val poet: String = shayari.poet
        override val categoryId: String = shayari.categoryId
        override val tags: List<String> = ShayariTagMatcher.parseTags(shayari.moodTag)
            .ifEmpty { ShayariTagMatcher.autoTagsForCategory(shayari.categoryId).take(3) }
    }

    data class CustomReel(val reel: UserCreatedReel) : ReelEntry {
        override val stableKey: String = "custom_${reel.id}"
        override val couplet: String = reel.couplet
        override val poet: String = reel.poet
        override val categoryId: String = reel.categoryId
        override val tags: List<String> = reel.tags
    }
}

private fun UserCreatedReel.toSyntheticShayariEntity(): ShayariEntity {
    val deterministicId = 9_000_000 + (id.hashCode() and 0x7FFFF)
    val effectiveCredit = singerName?.trim()?.takeIf { it.isNotBlank() }
        ?: if (videoUri != null || mediaType == "video") {
            VideoSingerDetector.extractKnownSingerFromText(couplet, poet)
        } else {
            null
        }
        ?: poet.ifBlank { "शायर दोस्त" }
    return ShayariEntity(
        id = deterministicId,
        couplet = couplet.ifBlank { "🎬 मेरी Reel" },
        poet = effectiveCredit,
        categoryId = categoryId.ifBlank { ShayariCategory.MOHABBAT.id },
        moodTag = tags.joinToString(", ")
    )
}

internal fun buildRandomizedShayariReelPool(
    allShayari: List<ShayariEntity>,
    popularShayari: List<ShayariEntity>,
    maxCount: Int = 240,
    random: Random = Random.Default
): List<ShayariEntity> {
    if (maxCount <= 0) return emptyList()
    val result = ArrayList<ShayariEntity>(maxCount)
    val seenIds = HashSet<Int>(maxCount * 2)

    val popularEligible = popularShayari
        .filterNot { ShayariCategory.fromId(it.categoryId).isJokeCategory }
        .shuffled(random)
    val popularQuota = minOf(popularEligible.size, (maxCount / 4).coerceAtLeast(16))
    for (i in 0 until popularQuota) {
        val item = popularEligible[i]
        if (seenIds.add(item.id)) {
            result.add(item)
        }
    }

    if (allShayari.isNotEmpty() && result.size < maxCount) {
        if (allShayari.size <= maxCount * 2) {
            val shuffledAll = allShayari.shuffled(random)
            for (item in shuffledAll) {
                if (result.size >= maxCount) break
                if (!ShayariCategory.fromId(item.categoryId).isJokeCategory && seenIds.add(item.id)) {
                    result.add(item)
                }
            }
        } else {
            val maxProbes = maxCount * 6
            var probes = 0
            while (result.size < maxCount && probes < maxProbes) {
                probes++
                val item = allShayari[random.nextInt(allShayari.size)]
                if (!ShayariCategory.fromId(item.categoryId).isJokeCategory && seenIds.add(item.id)) {
                    result.add(item)
                }
            }
            if (result.size < maxCount) {
                val startOffset = random.nextInt(allShayari.size)
                for (offset in allShayari.indices) {
                    if (result.size >= maxCount) break
                    val item = allShayari[(startOffset + offset) % allShayari.size]
                    if (!ShayariCategory.fromId(item.categoryId).isJokeCategory && seenIds.add(item.id)) {
                        result.add(item)
                    }
                }
            }
        }
    }

    result.shuffle(random)
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    allShayari: List<ShayariEntity>,
    popularShayari: List<ShayariEntity>,
    userReels: List<UserCreatedReel>,
    commentCounts: Map<String, Int>,
    authorName: String,
    onToggleFavorite: (ShayariEntity) -> Unit,
    onComment: (ShayariEntity) -> Unit,
    onShare: (ShayariEntity) -> Unit,
    onCreateUserReel: (UserCreatedReel) -> Unit,
    onShowFeedback: (String) -> Unit,
    modifier: Modifier = Modifier,
    createSheetTrigger: Int = 0,
    refreshTrigger: Int = 0,
    onDeleteUserReel: ((UserCreatedReel) -> Unit)? = null,
    onUpdateUserReelSinger: ((reel: UserCreatedReel, singerName: String?, songTitle: String?) -> Unit)? = null,
    currentUserId: String? = null,
    onlineLikedReelIds: Set<String> = emptySet(),
    isSignedInOnline: Boolean = false,
    onToggleOnlineReelLike: ((reelId: String, currentlyLiked: Boolean) -> Unit)? = null,
    onRequestGoogleSignIn: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ReelsFeedTab.FOR_YOU) }
    var activeTagFilter by remember { mutableStateOf<String?>(null) }
    var isAudioEnabled by remember { mutableStateOf(true) }
    var isCurrentReelPaused by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showSoundMixerSheet by remember { mutableStateOf(false) }
    var detectingSingerReelIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editingSingerReel by remember { mutableStateOf<UserCreatedReel?>(null) }
    var shuffleNonce by remember { mutableIntStateOf(0) }
    var shuffleSeed by remember { mutableLongStateOf(System.nanoTime()) }
    var lastTopReelKey by remember { mutableStateOf<String?>(null) }
    var pinLatestUserReel by remember { mutableStateOf(false) }
    var lastHandledExternalRefresh by remember { mutableIntStateOf(refreshTrigger) }

    // Per-Reel Sound Mixer overrides keyed by entry.stableKey (changing one shayari NEVER changes others!)
    var perReelPersonaOverrides by remember(context) {
        mutableStateOf(
            ReelsStorage.loadPerReelVoiceOverrides(context).mapValues { (_, v) ->
                ShayarVoicePersona.fromId(v)
            }
        )
    }
    var perReelBgmOverrides by remember(context) {
        mutableStateOf(
            ReelsStorage.loadPerReelBgmOverrides(context).mapValues { (_, v) ->
                ReelBgmTrack.fromId(v)
            }
        )
    }
    var voicePlaybackState by remember { mutableStateOf<ShayarVoicePlaybackState>(ShayarVoicePlaybackState.Idle) }
    var voiceRetryTick by remember { mutableIntStateOf(0) }
    var allowLocalTtsFallback by remember(context) {
        mutableStateOf(ReelsStorage.loadAllowLocalTtsFallback(context))
    }

    var likedReelKeys by remember(context) {
        mutableStateOf(ReelsStorage.loadLikedReelKeys(context))
    }
    var savedCustomIds by remember(context) {
        mutableStateOf(ReelsStorage.loadSavedCustomReelIds(context))
    }
    var followedPoets by remember(context) {
        mutableStateOf(ReelsStorage.loadFollowedPoets(context))
    }

    LaunchedEffect(createSheetTrigger) {
        if (createSheetTrigger > 0) {
            showCreateSheet = true
        }
    }

    // Zero-Noise Studio-Normalized TikTok BGM Player + Human-like Shayar Voice MediaPlayer
    // + Transparent Local Backup Speaker when Gemini free-tier quota is temporarily exhausted
    val bgmPlayer = remember(context) { ShayariBgmPlayer(context) }
    val localFallbackSpeaker = remember(context) { LocalShayarFallbackSpeaker(context) }
    val voicePlayerRef = remember { arrayOf<MediaPlayer?>(null) }
    val prewarmJobRef = remember { arrayOf<Job?>(null) }
    val wasPausedRef = remember { booleanArrayOf(false) }
    val hasSwipedOnceRef = remember { booleanArrayOf(false) }
    val latestIsPaused by rememberUpdatedState(isCurrentReelPaused)

    fun releaseVoicePlayer() {
        prewarmJobRef[0]?.cancel()
        prewarmJobRef[0] = null
        localFallbackSpeaker.stop()
        runCatching {
            voicePlayerRef[0]?.setOnCompletionListener(null)
            voicePlayerRef[0]?.stop()
            voicePlayerRef[0]?.release()
        }
        voicePlayerRef[0] = null
        bgmPlayer.setDucked(false)
    }

    DisposableEffect(bgmPlayer, localFallbackSpeaker) {
        onDispose {
            releaseVoicePlayer()
            bgmPlayer.release()
            localFallbackSpeaker.release()
        }
    }

    val reelsList: List<ReelEntry> = remember(
        allShayari,
        popularShayari,
        userReels,
        selectedTab,
        activeTagFilter,
        currentUserId,
        shuffleSeed,
        shuffleNonce,
        pinLatestUserReel
    ) {
        val rng = Random(shuffleSeed)
        val base: List<ReelEntry> = when (selectedTab) {
            ReelsFeedTab.MY_REELS -> userReels
                .filter { it.ownerUserId == null || it.ownerUserId == currentUserId }
                .map { ReelEntry.CustomReel(it) }
            ReelsFeedTab.TRENDING -> {
                val custom = userReels
                    .sortedByDescending { it.likeCount }
                    .map { ReelEntry.CustomReel(it) }
                val pool = popularShayari.ifEmpty { allShayari.take(160) }
                    .filterNot { ShayariCategory.fromId(it.categoryId).isJokeCategory }
                    .map { ReelEntry.ShayariReel(it) }
                val combined = (custom + pool).toMutableList()
                if (shuffleNonce > 0 && combined.size > 1) {
                    combined.shuffle(rng)
                }
                combined
            }
            ReelsFeedTab.FOR_YOU -> {
                val customEntries = userReels.map { ReelEntry.CustomReel(it) }
                val curatedShayari = buildRandomizedShayariReelPool(
                    allShayari = allShayari,
                    popularShayari = popularShayari,
                    maxCount = 240,
                    random = rng
                ).map { ReelEntry.ShayariReel(it) }

                if (pinLatestUserReel && customEntries.isNotEmpty()) {
                    val pinnedFirst = customEntries.first()
                    val rest = (customEntries.drop(1) + curatedShayari).shuffled(rng)
                    listOf(pinnedFirst) + rest
                } else {
                    (customEntries + curatedShayari).shuffled(rng)
                }
            }
        }

        val tagQuery = activeTagFilter?.trim()
        val filtered = if (tagQuery.isNullOrEmpty()) {
            base
        } else {
            val spec = ShayariTagMatcher.buildSearchSpec(tagQuery)
            base.filter { entry ->
                entry.tags.any { it.equals(tagQuery, ignoreCase = true) } ||
                    entry.couplet.contains(tagQuery, ignoreCase = true) ||
                    ShayariTagMatcher.scoreMatch(
                        spec = spec,
                        primaryCategoryId = entry.categoryId.ifBlank { ShayariCategory.MOHABBAT.id },
                        multiCategoryIds = null,
                        normCouplet = ShayariTagMatcher.normalizeForSearch(entry.couplet),
                        normPoet = ShayariTagMatcher.normalizeForSearch(entry.poet),
                        normMoodTag = ShayariTagMatcher.normalizeForSearch(entry.tags.joinToString(", "))
                    ) > 0
            }.ifEmpty { base }
        }

        // Guarantee a genuinely fresh top reel on random refresh whenever multiple reels exist
        if (!pinLatestUserReel &&
            selectedTab != ReelsFeedTab.MY_REELS &&
            lastTopReelKey != null &&
            filtered.size > 1 &&
            filtered.first().stableKey == lastTopReelKey
        ) {
            val mutable = filtered.toMutableList()
            val swapIndex = 1 + rng.nextInt(mutable.size - 1)
            val first = mutable[0]
            mutable[0] = mutable[swapIndex]
            mutable[swapIndex] = first
            mutable
        } else {
            filtered
        }
    }

    val pagerState = rememberPagerState(pageCount = { reelsList.size.coerceAtLeast(1) })

    fun refreshRandomReels(switchToHomeTab: Boolean, notifyUser: Boolean) {
        lastTopReelKey = reelsList.getOrNull(pagerState.currentPage)?.stableKey
        pinLatestUserReel = false
        activeTagFilter = null
        isCurrentReelPaused = false
        if (switchToHomeTab) {
            selectedTab = ReelsFeedTab.FOR_YOU
        }
        val nextNonce = shuffleNonce + 1
        shuffleNonce = nextNonce
        shuffleSeed = System.nanoTime() xor (nextNonce.toLong() * -7046029254386353131L)
        if (notifyUser) {
            onShowFeedback("नई रील्स रिफ्रेश हो गईं ✨")
        }
        scope.launch {
            pagerState.scrollToPage(0)
        }
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > lastHandledExternalRefresh) {
            lastHandledExternalRefresh = refreshTrigger
            refreshRandomReels(switchToHomeTab = true, notifyUser = false)
        }
    }

    LaunchedEffect(selectedTab, activeTagFilter, userReels.size, shuffleNonce) {
        isCurrentReelPaused = false
        if (pagerState.currentPage != 0 && reelsList.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentReelPaused = false
    }

    val currentEntry = reelsList.getOrNull(pagerState.currentPage)
    val latestActiveKey by rememberUpdatedState(currentEntry?.stableKey)
    val currentHasVideoAudio = currentEntry is ReelEntry.CustomReel &&
        (currentEntry.reel.videoUri != null || currentEntry.reel.mediaType == "video")

    // Auto-identify the playback singer (e.g. Kishore Kumar, Lata Mangeshkar, Arijit Singh)
    // on any video Reel that has not been checked yet, so singer name replaces uploader name.
    LaunchedEffect(currentEntry?.stableKey, currentHasVideoAudio) {
        val custom = currentEntry as? ReelEntry.CustomReel ?: return@LaunchedEffect
        val reel = custom.reel
        val isVideo = reel.videoUri != null || reel.mediaType == "video"
        if (!isVideo || !reel.singerName.isNullOrBlank() || reel.singerChecked) return@LaunchedEffect
        if (onUpdateUserReelSinger == null || reel.id in detectingSingerReelIds) return@LaunchedEffect
        val isOwnerOrLocal = reel.ownerUserId == null || reel.ownerUserId == currentUserId
        if (!isOwnerOrLocal) return@LaunchedEffect

        detectingSingerReelIds = detectingSingerReelIds + reel.id
        try {
            val detected = VideoSingerDetector.detectSinger(
                context = context,
                videoUri = reel.videoUri?.let { Uri.parse(it) },
                caption = reel.couplet,
                uploaderName = reel.poet
            )
            onUpdateUserReelSinger(reel, detected.singerName, detected.songTitle)
        } finally {
            detectingSingerReelIds = detectingSingerReelIds - reel.id
        }
    }

    fun resolvePersonaFor(entry: ReelEntry, pageIdx: Int): ShayarVoicePersona {
        return perReelPersonaOverrides[entry.stableKey]
            ?: (entry as? ReelEntry.CustomReel)?.reel?.voicePersonaId?.let { ShayarVoicePersona.fromId(it) }
            ?: ShayarVoicePersona.recommendedForReel(
                categoryId = entry.categoryId,
                couplet = entry.couplet,
                pageIndex = pageIdx,
                stableKey = entry.stableKey
            )
    }

    fun resolveBgmFor(entry: ReelEntry, pageIdx: Int): ReelBgmTrack {
        return perReelBgmOverrides[entry.stableKey]
            ?: (entry as? ReelEntry.CustomReel)?.reel?.bgmTrackId?.let { ReelBgmTrack.fromId(it) }
            ?: ReelBgmTrack.recommendedForReel(
                categoryId = entry.categoryId,
                pageIndex = pageIdx,
                stableKey = entry.stableKey
            )
    }

    val currentSelectedPersonaOverride = currentEntry?.stableKey?.let { perReelPersonaOverrides[it] }
    val currentSelectedBgmOverride = currentEntry?.stableKey?.let { perReelBgmOverrides[it] }

    val effectivePersona = remember(currentEntry?.stableKey, pagerState.currentPage, currentSelectedPersonaOverride) {
        currentEntry?.let { resolvePersonaFor(it, pagerState.currentPage) }
            ?: ShayarVoicePersona.SUREELI_SHAYARA
    }

    val effectiveBgmTrack = remember(currentEntry?.stableKey, pagerState.currentPage, currentSelectedBgmOverride) {
        currentEntry?.let { resolveBgmFor(it, pagerState.currentPage) }
            ?: ReelBgmTrack.SUFI_BANSURI_MEHFIL
    }

    // 1. Instant Zero-Noise Multi-Genre Background Music sync + Next-Reel BGM Pre-warm
    LaunchedEffect(isAudioEnabled, isCurrentReelPaused, showCreateSheet, effectiveBgmTrack, currentHasVideoAudio, currentEntry?.stableKey, pagerState.currentPage) {
        bgmPlayer.setTrack(effectiveBgmTrack)
        val shouldPlayBgm = isAudioEnabled &&
            !isCurrentReelPaused &&
            !showCreateSheet &&
            currentEntry != null &&
            !currentHasVideoAudio &&
            effectiveBgmTrack != ReelBgmTrack.OFF
        bgmPlayer.setPlaying(shouldPlayBgm)

        // Silently pre-render the next Reel's distinct musical loop so swiping has 0ms BGM lag
        if (shouldPlayBgm) {
            val nextIdx = pagerState.currentPage + 1
            val nextEntry = reelsList.getOrNull(nextIdx)
            if (nextEntry != null) {
                val nextHasVideo = nextEntry is ReelEntry.CustomReel &&
                    (nextEntry.reel.videoUri != null || nextEntry.reel.mediaType == "video")
                if (!nextHasVideo) {
                    val nextBgm = resolveBgmFor(nextEntry, nextIdx)
                    if (nextBgm != ReelBgmTrack.OFF && nextBgm != effectiveBgmTrack) {
                        bgmPlayer.prewarmTrack(nextBgm)
                    }
                }
            }
        }
    }

    // 2. Sync Pause/Resume on currently loaded Shayar Voice MediaPlayer & live fallback speaker
    LaunchedEffect(isCurrentReelPaused, showCreateSheet, isAudioEnabled) {
        val shouldPause = isCurrentReelPaused || showCreateSheet || !isAudioEnabled
        val wasPaused = wasPausedRef[0]
        wasPausedRef[0] = shouldPause

        if (shouldPause) {
            prewarmJobRef[0]?.cancel()
            prewarmJobRef[0] = null
            localFallbackSpeaker.stop()
            bgmPlayer.setDucked(false)
        }
        val player = voicePlayerRef[0]
        if (player != null) {
            runCatching {
                if (shouldPause) {
                    if (player.isPlaying) player.pause()
                } else if (!player.isPlaying) {
                    player.start()
                    bgmPlayer.setDucked(true)
                }
            }
        } else if (!shouldPause && wasPaused) {
            val entry = currentEntry
            if (entry != null && entry.couplet.isNotBlank() && !currentHasVideoAudio) {
                when (voicePlaybackState) {
                    is ShayarVoicePlaybackState.Speaking -> {
                        // Replay cached Gemini human audio cleanly via MediaPlayer
                        voiceRetryTick++
                    }
                    is ShayarVoicePlaybackState.QuotaFallback -> {
                        if (allowLocalTtsFallback) {
                            localFallbackSpeaker.speak(
                                couplet = entry.couplet,
                                persona = effectivePersona,
                                onSpeakingChanged = { speaking ->
                                    bgmPlayer.setDucked(speaking)
                                }
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun scheduleNextReelPrewarm(nextPageIndex: Int) {
        prewarmJobRef[0]?.cancel()
        prewarmJobRef[0] = null
        val nextEntry = reelsList.getOrNull(nextPageIndex) ?: return
        if (nextEntry.couplet.isBlank()) return
        val nextHasVideo = nextEntry is ReelEntry.CustomReel &&
            (nextEntry.reel.videoUri != null || nextEntry.reel.mediaType == "video")
        if (nextHasVideo) return

        val nextPersona = resolvePersonaFor(nextEntry, nextPageIndex)
        val nextBgm = resolveBgmFor(nextEntry, nextPageIndex)

        // Sequential look-ahead: wait 1s after current Reel starts playing, then quietly cache
        // ONLY the single next Reel (page + 1) so the user's next swipe plays in 0ms without quota bursts.
        prewarmJobRef[0] = scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            delay(1_000L)
            if (latestIsPaused || !isAudioEnabled || showCreateSheet) return@launch
            if (nextBgm != ReelBgmTrack.OFF) {
                bgmPlayer.prewarmTrack(nextBgm)
            }
            if (!ShayarVoiceEngine.isInFallbackCooldown()) {
                ShayarVoiceEngine.prewarmVoiceIfSafe(
                    context = context,
                    couplet = nextEntry.couplet,
                    categoryId = nextEntry.categoryId,
                    persona = nextPersona
                )
            } else if (allowLocalTtsFallback) {
                val alreadyLocal = localFallbackSpeaker.getCachedMasteredVoiceOrNull(
                    couplet = nextEntry.couplet,
                    persona = nextPersona
                )
                if (alreadyLocal == null) {
                    localFallbackSpeaker.synthesizeMasteredVoice(
                        couplet = nextEntry.couplet,
                        persona = nextPersona,
                        isPrewarm = true
                    )
                }
            }
        }
    }

    // 3. Expressive Human Shayar Voice via Gemini 3.1 Flash TTS (0ms cache + next-Reel pre-warm + graceful offline fallback)
    LaunchedEffect(
        currentEntry?.stableKey,
        effectivePersona,
        isAudioEnabled,
        showCreateSheet,
        allowLocalTtsFallback,
        voiceRetryTick
    ) {
        releaseVoicePlayer()
        val entry = currentEntry
        if (!isAudioEnabled || showCreateSheet || entry == null || entry.couplet.isBlank() || currentHasVideoAudio) {
            voicePlaybackState = ShayarVoicePlaybackState.Idle
            return@LaunchedEffect
        }

        val activeReelKey = entry.stableKey
        val activePageSnapshot = pagerState.currentPage
        val activePersonaSnapshot = effectivePersona
        val speakingState = ShayarVoicePlaybackState.Speaking(activePersonaSnapshot)

        suspend fun playWavFileInMediaPlayer(
            audioFile: File,
            targetPlaybackState: ShayarVoicePlaybackState
        ): Boolean {
            val mp = MediaPlayer()
            val prepared = try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    mp.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    mp.setDataSource(audioFile.absolutePath)
                    mp.prepare()
                    mp.setVolume(1.0f, 1.0f)
                    true
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                runCatching { mp.release() }
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ReelsScreen", "MediaPlayer prepare failed", e)
                runCatching { mp.release() }
                false
            }

            if (!prepared) return false

            voicePlayerRef[0] = mp

            mp.setOnErrorListener { _, _, _ ->
                releaseVoicePlayer()
                true
            }

            mp.setOnCompletionListener { completedPlayer ->
                bgmPlayer.setDucked(false)
                scope.launch {
                    // Natural breathing pause while emotional music swells softly before reciting again
                    delay(3_200L)
                    if (voicePlayerRef[0] === completedPlayer && !latestIsPaused && isAudioEnabled && !showCreateSheet) {
                        runCatching {
                            completedPlayer.seekTo(0)
                            completedPlayer.start()
                            bgmPlayer.setDucked(true)
                            voicePlaybackState = targetPlaybackState
                        }
                    }
                }
            }

            if (!latestIsPaused && isAudioEnabled && !showCreateSheet) {
                runCatching {
                    mp.start()
                    bgmPlayer.setDucked(true)
                    voicePlaybackState = targetPlaybackState
                }
            }
            return true
        }

        // Fast-path: 0ms instant playback if Gemini human Shayar audio is already cached on disk!
        val cachedGeminiAudio = ShayarVoiceEngine.getCachedVoiceOrNull(
            context = context,
            couplet = entry.couplet,
            categoryId = entry.categoryId,
            persona = activePersonaSnapshot
        )

        val geminiResult: ShayarVoiceResult = if (cachedGeminiAudio != null) {
            hasSwipedOnceRef[0] = true
            ShayarVoiceResult.Ready(cachedGeminiAudio)
        } else {
            // Brief 120ms settle delay on swipe so flick-scrolling skips wasted calls while staying snappy
            val settleMs = if (!hasSwipedOnceRef[0]) 0L else 120L
            hasSwipedOnceRef[0] = true
            if (settleMs > 0L) delay(settleMs)
            if (latestActiveKey != activeReelKey || !isAudioEnabled || showCreateSheet) {
                return@LaunchedEffect
            }
            voicePlaybackState = ShayarVoicePlaybackState.Loading
            ShayarVoiceEngine.getOrGenerateVoice(
                context = context,
                couplet = entry.couplet,
                categoryId = entry.categoryId,
                persona = activePersonaSnapshot
            )
        }

        if (latestActiveKey != activeReelKey || !isAudioEnabled || showCreateSheet) {
            return@LaunchedEffect
        }

        if (geminiResult is ShayarVoiceResult.Ready) {
            if (!latestIsPaused) {
                val started = playWavFileInMediaPlayer(geminiResult.audioFile, speakingState)
                if (started) {
                    scheduleNextReelPrewarm(activePageSnapshot + 1)
                    return@LaunchedEffect
                }
            } else {
                voicePlaybackState = speakingState
                return@LaunchedEffect
            }
        }

        val fallbackDetail = when (geminiResult) {
            is ShayarVoiceResult.FallbackRequired -> geminiResult.message
            is ShayarVoiceResult.Error -> geminiResult.message
            is ShayarVoiceResult.Ready -> "ऑफ़लाइन आवाज़ सक्रिय"
        }

        if (!allowLocalTtsFallback) {
            // User prefers pure emotional BGM instead of phone TTS when Gemini voice isn't available
            voicePlaybackState = ShayarVoicePlaybackState.Error(fallbackDetail)
            return@LaunchedEffect
        }

        // Graceful offline/quota fallback using mastered Android Hindi voice
        val fallbackState = ShayarVoicePlaybackState.QuotaFallback(
            persona = activePersonaSnapshot,
            detail = fallbackDetail
        )
        val cachedLocalWav = localFallbackSpeaker.getCachedMasteredVoiceOrNull(
            couplet = entry.couplet,
            persona = activePersonaSnapshot
        )
        val readyLocalWav = cachedLocalWav ?: localFallbackSpeaker.synthesizeMasteredVoice(
            couplet = entry.couplet,
            persona = activePersonaSnapshot,
            isPrewarm = false
        )

        if (readyLocalWav != null &&
            !latestIsPaused &&
            isAudioEnabled &&
            !showCreateSheet &&
            latestActiveKey == activeReelKey
        ) {
            val started = playWavFileInMediaPlayer(readyLocalWav, fallbackState)
            if (started) {
                scheduleNextReelPrewarm(activePageSnapshot + 1)
                return@LaunchedEffect
            }
        }

        // Final zero-silence live TTS backup
        fun speakOnce() {
            if (latestIsPaused || !isAudioEnabled || showCreateSheet || latestActiveKey != activeReelKey) {
                return
            }
            voicePlaybackState = fallbackState
            localFallbackSpeaker.speak(
                couplet = entry.couplet,
                persona = activePersonaSnapshot,
                onSpeakingChanged = { speaking ->
                    bgmPlayer.setDucked(speaking)
                },
                onCompletedAll = {
                    scope.launch {
                        delay(3_200L)
                        if (!latestIsPaused && isAudioEnabled && !showCreateSheet &&
                            latestActiveKey == activeReelKey &&
                            voicePlayerRef[0] == null
                        ) {
                            speakOnce()
                        }
                    }
                }
            )
        }
        speakOnce()
    }

    fun updateLikedKeys(next: Set<String>) {
        likedReelKeys = next
        ReelsStorage.saveLikedReelKeys(context, next)
    }

    fun updateSavedCustomIds(next: Set<String>) {
        savedCustomIds = next
        ReelsStorage.saveSavedCustomReelIds(context, next)
    }

    fun updateFollowedPoets(next: Set<String>) {
        followedPoets = next
        ReelsStorage.saveFollowedPoets(context, next)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_screen_root")
    ) {
        if (reelsList.isEmpty()) {
            EmptyReelsPlaceholder(
                isMyReelsTab = selectedTab == ReelsFeedTab.MY_REELS,
                onCreateClick = { showCreateSheet = true }
            )
        } else {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                key = { idx -> reelsList.getOrNull(idx)?.stableKey ?: "empty_$idx" },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reels_vertical_pager")
            ) { page ->
                val entry = reelsList.getOrNull(page) ?: return@VerticalPager
                val isSelectedPage = pagerState.currentPage == page
                val isCurrentPage = isSelectedPage && !showCreateSheet
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    .absoluteValue
                    .coerceIn(0f, 1f)
                val isOnlineCustomReel = entry is ReelEntry.CustomReel && entry.reel.isOnlineSynced
                val isLiked = if (isOnlineCustomReel && onToggleOnlineReelLike != null && isSignedInOnline) {
                    (entry as ReelEntry.CustomReel).reel.id in onlineLikedReelIds
                } else {
                    entry.stableKey in likedReelKeys
                }
                val isSaved = when (entry) {
                    is ReelEntry.ShayariReel -> entry.shayari.isFavorite
                    is ReelEntry.CustomReel -> entry.reel.id in savedCustomIds
                }
                val commentEntity = remember(entry.stableKey) {
                    when (entry) {
                        is ReelEntry.ShayariReel -> entry.shayari
                        is ReelEntry.CustomReel -> entry.reel.toSyntheticShayariEntity()
                    }
                }
                val commentCount = commentCounts[shayariCommentKey(commentEntity.id)] ?: 0
                val isOwnCustomReel = entry is ReelEntry.CustomReel &&
                    (entry.reel.ownerUserId == null || entry.reel.ownerUserId == currentUserId)
                val entryIsVideo = entry is ReelEntry.CustomReel &&
                    (entry.reel.videoUri != null || entry.reel.mediaType == "video")
                val entrySingerName = if (entry is ReelEntry.CustomReel && entryIsVideo) {
                    entry.reel.singerName?.trim()?.takeIf { it.isNotBlank() }
                        ?: VideoSingerDetector.extractKnownSingerFromText(entry.reel.couplet, entry.reel.poet)
                } else {
                    null
                }
                val followTargetName = entrySingerName ?: entry.poet
                val isFollowed = followTargetName in followedPoets

                ReelPageCard(
                    entry = entry,
                    isActive = isCurrentPage,
                    isVisuallyFocused = isSelectedPage,
                    pageOffset = pageOffset,
                    revealEpoch = shuffleNonce,
                    isPaused = if (isCurrentPage) isCurrentReelPaused else false,
                    onTogglePause = {
                        if (isCurrentPage) {
                            isCurrentReelPaused = !isCurrentReelPaused
                        }
                    },
                    isLiked = isLiked,
                    isSaved = isSaved,
                    isFollowed = isFollowed,
                    commentCount = commentCount,
                    isAudioEnabled = isAudioEnabled,
                    voiceState = if (isCurrentPage) voicePlaybackState else ShayarVoicePlaybackState.Idle,
                    activePersona = effectivePersona,
                    activeBgmTrack = effectiveBgmTrack,
                    canDelete = isOwnCustomReel && onDeleteUserReel != null,
                    isDetectingSinger = entry is ReelEntry.CustomReel && entry.reel.id in detectingSingerReelIds,
                    onEditSingerClick = if (isOwnCustomReel && entryIsVideo && onUpdateUserReelSinger != null) {
                        { editingSingerReel = (entry as ReelEntry.CustomReel).reel }
                    } else {
                        null
                    },
                    onToggleLike = {
                        if (isOnlineCustomReel && onToggleOnlineReelLike != null && isSignedInOnline) {
                            onToggleOnlineReelLike((entry as ReelEntry.CustomReel).reel.id, isLiked)
                        } else {
                            val next = if (isLiked) likedReelKeys - entry.stableKey else likedReelKeys + entry.stableKey
                            updateLikedKeys(next)
                        }
                    },
                    onLikeOnly = {
                        if (!isLiked) {
                            if (isOnlineCustomReel && onToggleOnlineReelLike != null && isSignedInOnline) {
                                onToggleOnlineReelLike((entry as ReelEntry.CustomReel).reel.id, false)
                            } else {
                                updateLikedKeys(likedReelKeys + entry.stableKey)
                            }
                        }
                    },
                    onToggleSave = {
                        when (entry) {
                            is ReelEntry.ShayariReel -> onToggleFavorite(entry.shayari)
                            is ReelEntry.CustomReel -> {
                                val willSave = entry.reel.id !in savedCustomIds
                                updateSavedCustomIds(
                                    if (willSave) savedCustomIds + entry.reel.id else savedCustomIds - entry.reel.id
                                )
                                onShowFeedback(if (willSave) "Reel सेव की गई 🔖" else "Reel सेव से हटाई गई")
                            }
                        }
                    },
                    onCommentClick = {
                        onComment(commentEntity)
                    },
                    onShareClick = {
                        onShare(commentEntity)
                    },
                    onToggleAudio = {
                        isAudioEnabled = !isAudioEnabled
                        if (entry !is ReelEntry.CustomReel || (entry.reel.videoUri == null && entry.reel.mediaType != "video")) {
                            onShowFeedback(
                                if (isAudioEnabled) "🎙️ आवाज़ व म्यूज़िक चालू" else "🔇 आवाज़ व म्यूज़िक बंद"
                            )
                        }
                    },
                    onOpenSoundMixer = {
                        showSoundMixerSheet = true
                    },
                    onRetryVoice = {
                        ShayarVoiceEngine.clearQuotaCooldown()
                        voiceRetryTick++
                    },
                    onToggleFollow = {
                        if (isFollowed) {
                            updateFollowedPoets(followedPoets - followTargetName)
                        } else {
                            updateFollowedPoets(followedPoets + followTargetName)
                            onShowFeedback("$followTargetName को फॉलो किया ✨")
                        }
                    },
                    onTagClick = { clickedTag ->
                        activeTagFilter = if (activeTagFilter == clickedTag) null else clickedTag
                        scope.launch { pagerState.scrollToPage(0) }
                    },
                    onDeleteClick = if (isOwnCustomReel && onDeleteUserReel != null) {
                        {
                            onDeleteUserReel((entry as ReelEntry.CustomReel).reel)
                            onShowFeedback("Reel हटा दी गई")
                        }
                    } else {
                        null
                    }
                )
            }
        }

        // Transparent Instagram / TikTok Top Bar Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.76f),
                            Color.Black.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ReelsFeedTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (selectedTab == tab && tab != ReelsFeedTab.MY_REELS) {
                                        refreshRandomReels(
                                            switchToHomeTab = tab == ReelsFeedTab.FOR_YOU,
                                            notifyUser = true
                                        )
                                    } else {
                                        selectedTab = tab
                                        activeTagFilter = null
                                        if (tab == ReelsFeedTab.FOR_YOU) {
                                            refreshRandomReels(switchToHomeTab = true, notifyUser = false)
                                        }
                                    }
                                }
                                .padding(horizontal = 5.dp, vertical = 4.dp)
                                .testTag("reels_tab_${tab.name}")
                        ) {
                            Text(
                                text = tab.labelHi,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = if (isSelected) 15.sp else 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        offset = Offset(0f, 1.5f),
                                        blurRadius = 6f
                                    )
                                ),
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.68f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(if (isSelected) 22.dp else 0.dp)
                                    .height(2.5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF4D6D))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // + Create Reel Button (Gallery Video or Photo Motion Reel)
                Surface(
                    onClick = { showCreateSheet = true },
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.48f)),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .height(36.dp)
                        .testTag("reels_create_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoCall,
                            contentDescription = "नई Reel बनाएँ",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "बनाएँ",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (activeTagFilter != null) {
                Surface(
                    onClick = { activeTagFilter = null },
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFF4D6D).copy(alpha = 0.90f),
                    modifier = Modifier.testTag("reels_active_tag_filter")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "#$activeTagFilter Reels",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "टैग फ़िल्टर हटाएँ",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        if (showSoundMixerSheet && currentEntry != null) {
            val activeKey = currentEntry.stableKey
            ReelSoundMixerSheet(
                selectedPersona = currentSelectedPersonaOverride,
                effectivePersona = effectivePersona,
                selectedBgmTrack = currentSelectedBgmOverride,
                effectiveBgmTrack = effectiveBgmTrack,
                voiceState = voicePlaybackState,
                allowLocalTtsFallback = allowLocalTtsFallback,
                onToggleLocalTtsFallback = { enabled ->
                    allowLocalTtsFallback = enabled
                    ReelsStorage.saveAllowLocalTtsFallback(context, enabled)
                },
                onRetryVoice = {
                    ShayarVoiceEngine.clearQuotaCooldown()
                    voiceRetryTick++
                },
                onSelectPersona = { persona ->
                    val nextMap = if (persona == null) {
                        perReelPersonaOverrides - activeKey
                    } else {
                        perReelPersonaOverrides + (activeKey to persona)
                    }
                    perReelPersonaOverrides = nextMap
                    ReelsStorage.savePerReelVoiceOverrides(
                        context,
                        nextMap.mapValues { (_, v) -> v.id }
                    )
                    if (!isAudioEnabled) isAudioEnabled = true
                },
                onSelectBgmTrack = { track ->
                    val nextMap = if (track == null) {
                        perReelBgmOverrides - activeKey
                    } else {
                        perReelBgmOverrides + (activeKey to track)
                    }
                    perReelBgmOverrides = nextMap
                    ReelsStorage.savePerReelBgmOverrides(
                        context,
                        nextMap.mapValues { (_, v) -> v.id }
                    )
                    if (!isAudioEnabled && track != ReelBgmTrack.OFF) isAudioEnabled = true
                },
                onDismiss = { showSoundMixerSheet = false }
            )
        }

        if (showCreateSheet) {
            CreateReelBottomSheet(
                defaultAuthor = authorName.ifBlank { "शायर दोस्त" },
                isSignedInOnline = isSignedInOnline,
                onRequestGoogleSignIn = onRequestGoogleSignIn,
                onDismiss = { showCreateSheet = false },
                onShowFeedback = onShowFeedback,
                onPublishReel = { newReel ->
                    onCreateUserReel(newReel)
                    showCreateSheet = false
                    pinLatestUserReel = true
                    selectedTab = ReelsFeedTab.FOR_YOU
                    activeTagFilter = null
                    isAudioEnabled = true
                    scope.launch { pagerState.scrollToPage(0) }
                    onShowFeedback(
                        if (isSignedInOnline) {
                            "आपकी नई Reel ऑनलाइन लाइव हो गई 🌐✨"
                        } else {
                            "Reel पोस्ट हो गई ✨ (ऑनलाइन सिंक के लिए Google से जुड़ें)"
                        }
                    )
                }
            )
        }

        editingSingerReel?.let { targetReel ->
            EditVideoSingerSheet(
                reel = targetReel,
                onDismiss = { editingSingerReel = null },
                onSave = { newSinger, newSong ->
                    onUpdateUserReelSinger?.invoke(targetReel, newSinger, newSong)
                    editingSingerReel = null
                    onShowFeedback(
                        if (!newSinger.isNullOrBlank()) {
                            "🎤 सिंगर का नाम सेव हुआ: $newSinger"
                        } else {
                            "सिंगर का नाम हटा दिया गया"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun ReelPageCard(
    entry: ReelEntry,
    isActive: Boolean,
    isVisuallyFocused: Boolean,
    pageOffset: Float,
    revealEpoch: Int,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowed: Boolean,
    commentCount: Int,
    isAudioEnabled: Boolean,
    voiceState: ShayarVoicePlaybackState,
    activePersona: ShayarVoicePersona,
    activeBgmTrack: ReelBgmTrack,
    canDelete: Boolean,
    isDetectingSinger: Boolean = false,
    onEditSingerClick: (() -> Unit)? = null,
    onToggleLike: () -> Unit,
    onLikeOnly: () -> Unit,
    onToggleSave: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleAudio: () -> Unit,
    onOpenSoundMixer: () -> Unit,
    onRetryVoice: () -> Unit,
    onToggleFollow: () -> Unit,
    onTagClick: (String) -> Unit,
    onDeleteClick: (() -> Unit)?
) {
    val category = remember(entry.categoryId) {
        ShayariCategory.fromId(entry.categoryId.ifBlank { ShayariCategory.MOHABBAT.id })
    }
    val readingStyle = LocalShayariReadingStyle.current
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    var showPausePulse by remember { mutableStateOf(false) }
    var reelProgress by remember(entry.stableKey) { mutableFloatStateOf(0f) }
    var videoPlaybackFailed by remember(entry.stableKey) { mutableStateOf(false) }

    val customReel = entry as? ReelEntry.CustomReel
    val videoUri = customReel?.reel?.videoUri?.takeUnless { videoPlaybackFailed }
    val photoUri = customReel?.reel?.photoUri
    val thumbnailBase64 = customReel?.reel?.thumbnailBase64?.takeIf { it.isNotBlank() }
    val decodedThumbBitmap: ImageBitmap? = remember(thumbnailBase64) {
        if (thumbnailBase64 == null) {
            null
        } else {
            runCatching {
                val bytes = Base64.decode(thumbnailBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    val isVideoReel = videoUri != null || customReel?.reel?.mediaType == "video"
    val isCustomMediaReel = customReel != null &&
        (videoUri != null || photoUri != null || decodedThumbBitmap != null || customReel.reel.mediaType == "video" || customReel.reel.mediaType == "photo")
    val resolvedSingerName = remember(customReel?.reel?.singerName, entry.couplet, entry.poet, isVideoReel) {
        if (!isVideoReel) {
            null
        } else {
            customReel?.reel?.singerName?.trim()?.takeIf { it.isNotBlank() }
                ?: VideoSingerDetector.extractKnownSingerFromText(entry.couplet, entry.poet)
        }
    }
    val resolvedSongTitle = remember(customReel?.reel?.songTitle, isVideoReel) {
        if (!isVideoReel) null else customReel?.reel?.songTitle?.trim()?.takeIf { it.isNotBlank() }
    }
    val hasSingerCredit = isVideoReel && !resolvedSingerName.isNullOrBlank()
    val fallbackDrawable = remember(entry.stableKey) {
        when (entry) {
            is ReelEntry.ShayariReel -> resolveDiversePhotoForCard(entry.shayari, category)
            is ReelEntry.CustomReel -> resolvePresetBackdropRes(entry.reel.backgroundDrawableName)
                ?: BUILT_IN_REEL_BACKDROPS.first().resId
        }
    }

    // Smooth auto-progress bar for Motion Photo Reels (real MediaPlayer drives video Reels)
    LaunchedEffect(isActive, isPaused, entry.stableKey, videoUri) {
        if (!isActive) {
            reelProgress = 0f
            return@LaunchedEffect
        }
        if (videoUri != null) return@LaunchedEffect
        while (true) {
            delay(50L)
            if (!isPaused) {
                reelProgress = (reelProgress + 0.0042f).let { if (it >= 1f) 0f else it }
            }
        }
    }

    LaunchedEffect(showDoubleTapHeart) {
        if (showDoubleTapHeart) {
            delay(650L)
            showDoubleTapHeart = false
        }
    }

    LaunchedEffect(showPausePulse) {
        if (showPausePulse) {
            delay(550L)
            showPausePulse = false
        }
    }

    // Cinematic Ken Burns slow zoom & pan for Motion Photo Reels
    val motionTransition = rememberInfiniteTransition(label = "reel_ken_burns")
    val kenBurnsScale by motionTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.13f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ken_burns_scale"
    )
    val kenBurnsShiftX by motionTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ken_burns_x"
    )
    val particlePhase by motionTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ember_phase"
    )
    val vinylRotation by motionTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_spin"
    )

    val likeHeartScale by animateFloatAsState(
        targetValue = if (isLiked) 1.16f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "like_bounce"
    )

    val simulatedLikesCount = remember(entry.stableKey) {
        val seed = (entry.stableKey.hashCode() and 0x7FFFFFFF)
        120 + (seed % 2400)
    }

    val latestTogglePause by rememberUpdatedState(onTogglePause)
    val latestLikeOnly by rememberUpdatedState(onLikeOnly)

    val reelVisualReveal = rememberPagerRevealProgress(
        isCurrentPage = isVisuallyFocused,
        contentKey = Triple(entry.stableKey, revealEpoch, "reel_visual"),
        durationMillis = 380,
        delayMillis = 0
    )
    val textReveal = rememberPagerRevealProgress(
        isCurrentPage = isVisuallyFocused,
        contentKey = Triple(entry.stableKey, revealEpoch, "reel_text"),
        durationMillis = 480,
        delayMillis = 65
    )
    val reelSwipeVisibility = (1f - 0.30f * pageOffset).coerceIn(0f, 1f)
    val textSwipeVisibility = (1f - 0.60f * pageOffset).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(entry.stableKey) {
                detectTapGestures(
                    onDoubleTap = {
                        latestLikeOnly()
                        showDoubleTapHeart = true
                    },
                    onTap = {
                        latestTogglePause()
                        showPausePulse = true
                    }
                )
            }
    ) {
        // 1. Background Media Layer with smooth Reel fade-in on load/swipe/refresh
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = (0.22f + 0.78f * reelVisualReveal) * reelSwipeVisibility
                    val settleScale = 0.975f + (0.025f * reelVisualReveal)
                    scaleX = settleScale
                    scaleY = settleScale
                }
        ) {
        if (videoUri != null) {
            LoopingTextureVideoBackground(
                uriString = videoUri,
                isActive = isActive && !isPaused,
                isMuted = !isAudioEnabled,
                onProgressUpdate = { progress -> reelProgress = progress },
                onPlaybackError = { videoPlaybackFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        } else if (photoUri != null) {
            AsyncImage(
                model = Uri.parse(photoUri),
                contentDescription = entry.poet,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (isActive && !isPaused) {
                            scaleX = kenBurnsScale
                            scaleY = kenBurnsScale
                            translationX = kenBurnsShiftX
                        }
                    }
            )
        } else if (decodedThumbBitmap != null) {
            Image(
                bitmap = decodedThumbBitmap,
                contentDescription = entry.poet,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (isActive && !isPaused) {
                            scaleX = kenBurnsScale
                            scaleY = kenBurnsScale
                            translationX = kenBurnsShiftX
                        }
                    }
            )
        } else {
            OptimizedDrawableImage(
                resId = fallbackDrawable,
                contentDescription = entry.poet,
                widthPx = 1080,
                heightPx = 1920,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (isActive && !isPaused) {
                            scaleX = kenBurnsScale
                            scaleY = kenBurnsScale
                            translationX = kenBurnsShiftX
                        }
                    }
            )
        }

        // 2. Cinema Scrim + Subtle Floating Golden Dust Bokeh (clean light scrim on video/photo so media shines)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = if (isCustomMediaReel) 0.34f else 0.44f),
                        0.30f to Color.Black.copy(alpha = if (isCustomMediaReel) 0.06f else 0.36f),
                        0.68f to Color.Black.copy(alpha = if (isCustomMediaReel) 0.24f else 0.56f),
                        1.0f to Color.Black.copy(alpha = 0.88f)
                    )
                )
        )
        }

        if (!isCustomMediaReel && isActive && !isPaused) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                for (i in 0 until 12) {
                    val xFrac = ((i * 73 + 19) % 100) / 100f
                    val yBase = ((i * 41 + 11) % 100) / 100f
                    val drift = sin(particlePhase + i * 0.7f) * 18f
                    val yPos = (h * yBase - (particlePhase / (2f * PI.toFloat())) * 90f + h) % h
                    val radius = (2.5f + (i % 4) * 1.4f).dp.toPx()
                    drawCircle(
                        color = Color(0xFFFFD580).copy(alpha = 0.16f + (i % 3) * 0.06f),
                        radius = radius,
                        center = Offset(w * xFrac + drift, yPos)
                    )
                }
            }
        }

        // 3. Center Typography Showcase — ONLY for pure text Reels (never blocks user Video/Photo Reels!)
        if (!isCustomMediaReel && entry.couplet.isNotBlank()) {
            val shayariFontSize = when {
                entry.couplet.length > 140 -> 20.sp
                entry.couplet.length > 90 -> 23.sp
                else -> 26.sp
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 74.dp, top = 36.dp, bottom = 78.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show Shayari category pill ONLY on built-in curated Shayari Reels, never forced on user Reels
                if (entry is ReelEntry.ShayariReel) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.40f),
                        border = BorderStroke(0.8.dp, Color(0xFFFFD580).copy(alpha = 0.52f)),
                        modifier = Modifier.graphicsLayer {
                            alpha = textReveal * textSwipeVisibility
                            translationY = (1f - textReveal) * 12.dp.toPx()
                        }
                    ) {
                        Text(
                            text = "${category.emoji} ${category.titleHi}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.4.sp
                            ),
                            color = Color(0xFFFFE0A3),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Text(
                    text = entry.couplet,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = readingStyle.fontStyle.fontFamily,
                        fontSize = shayariFontSize,
                        lineHeight = (shayariFontSize.value * 1.52f).sp,
                        fontWeight = FontWeight.SemiBold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.92f),
                            offset = Offset(0f, 3f),
                            blurRadius = 14f
                        )
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = textReveal * textSwipeVisibility
                        translationY = (1f - textReveal) * 20.dp.toPx()
                    }
                )
            }
        }

        // 4. Double-Tap Big Red Heart Burst Animation
        AnimatedVisibility(
            visible = showDoubleTapHeart,
            enter = scaleIn(
                initialScale = 0.4f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeIn(),
            exit = scaleOut(targetScale = 1.4f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF3057),
                modifier = Modifier.size(104.dp)
            )
        }

        // 5. Single-Tap Play/Pause Pulse Indicator
        AnimatedVisibility(
            visible = showPausePulse || isPaused,
            enter = scaleIn(initialScale = 0.7f) + fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.54f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = if (isPaused) "Play" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // 6. Right-Side TikTok / Instagram Vertical Action Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val displayLikesCount = when {
                customReel?.reel?.isOnlineSynced == true -> customReel.reel.likeCount.coerceAtLeast(0)
                customReel != null -> customReel.reel.likeCount + if (isLiked) 1 else 0
                else -> simulatedLikesCount + if (isLiked) 1 else 0
            }
            ReelRailAction(
                icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = formatCompactCount(displayLikesCount),
                tint = if (isLiked) Color(0xFFFF3057) else Color.White,
                iconModifier = Modifier.scale(likeHeartScale),
                testTag = "reel_like_button",
                onClick = onToggleLike
            )

            ReelRailAction(
                icon = Icons.Rounded.ChatBubbleOutline,
                label = if (commentCount > 0) commentCount.toString() else "कमेंट",
                tint = Color.White,
                testTag = "reel_comment_button",
                onClick = onCommentClick
            )

            ReelRailAction(
                icon = Icons.Rounded.Share,
                label = "शेयर",
                tint = Color.White,
                testTag = "reel_share_button",
                onClick = onShareClick
            )

            ReelRailAction(
                icon = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                label = if (isSaved) "सेव्ड" else "सेव",
                tint = if (isSaved) Color(0xFFFFD166) else Color.White,
                testTag = "reel_save_button",
                onClick = onToggleSave
            )

            ReelRailAction(
                icon = if (isAudioEnabled) {
                    Icons.AutoMirrored.Rounded.VolumeUp
                } else {
                    Icons.AutoMirrored.Rounded.VolumeOff
                },
                label = if (videoUri != null) {
                    if (isAudioEnabled) "साउंड" else "म्यूट"
                } else {
                    if (isAudioEnabled) "सुनें" else "आवाज़"
                },
                tint = if (isAudioEnabled) Color(0xFF7CE8A6) else Color.White,
                testTag = "reel_audio_button",
                onClick = onToggleAudio
            )

            if (canDelete && onDeleteClick != null) {
                ReelRailAction(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "हटाएँ",
                    tint = Color(0xFFFF8A80),
                    testTag = "reel_delete_button",
                    onClick = onDeleteClick
                )
            }

            // Spinning Vinyl Audio Disc — Tap to change Shayar Voice & Background Music!
            Surface(
                onClick = onOpenSoundMixer,
                shape = CircleShape,
                color = Color(0xFF1A1A22),
                border = BorderStroke(2.dp, Color(0xFFFFB74D).copy(alpha = 0.85f)),
                modifier = Modifier
                    .size(42.dp)
                    .rotate(if (isActive && !isPaused && isAudioEnabled) vinylRotation else 0f)
                    .testTag("reel_sound_mixer_disc")
            ) {
                OptimizedDrawableImage(
                    resId = R.drawable.shayari_heartbreak_logo_1795079888557,
                    contentDescription = "शायर आवाज़ और बैकग्राउंड म्यूज़िक बदलें",
                    widthPx = 120,
                    heightPx = 120,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 7. Bottom-Left Creator Info, Follow, Clickable Hashtags & Audio Strip
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 78.dp, bottom = 18.dp)
                .graphicsLayer {
                    val captionReveal = ((textReveal - 0.12f) / 0.88f).coerceIn(0f, 1f)
                    alpha = (0.20f + 0.80f * captionReveal) * textSwipeVisibility
                    translationY = (1f - captionReveal) * 12.dp.toPx()
                },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val cleanPoetName = remember(entry.poet) {
                val trimmed = entry.poet.trim().trimStart('_', '-', '•', ' ')
                when {
                    trimmed.isBlank() || trimmed == "शायरी" -> "शायरी महफ़िल"
                    else -> trimmed
                }
            }
            val displayHeaderName = if (hasSingerCredit) resolvedSingerName!! else cleanPoetName
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (hasSingerCredit) {
                        Color(0xFFFFB74D).copy(alpha = 0.24f)
                    } else {
                        category.darkAccent.copy(alpha = 0.28f)
                    },
                    border = BorderStroke(
                        1.5.dp,
                        if (hasSingerCredit) Color(0xFFFFD580) else Color.White.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (hasSingerCredit) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = resolvedSingerName,
                                tint = Color(0xFFFFE0A3),
                                modifier = Modifier.size(19.dp)
                            )
                        } else {
                            Text(
                                text = displayHeaderName.take(1).uppercase().ifBlank { "श" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                Text(
                    text = displayHeaderName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(Color.Black, Offset(0f, 1.5f), 6f)
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .testTag("reel_creator_or_singer_name")
                )

                Surface(
                    onClick = onToggleFollow,
                    shape = RoundedCornerShape(8.dp),
                    color = if (isFollowed) Color.White.copy(alpha = 0.22f) else Color.Transparent,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                    modifier = Modifier.testTag("reel_follow_button")
                ) {
                    Text(
                        text = if (isFollowed) "Following" else "Follow",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (customReel?.reel?.isOnlineSynced == true) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1DB954).copy(alpha = 0.28f),
                        border = BorderStroke(0.8.dp, Color(0xFF7CE8A6).copy(alpha = 0.72f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = Color(0xFF9DF5BD),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "ऑनलाइन",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // TikTok-style bottom-left free-form caption for Video / Photo Reels
            if (isCustomMediaReel && entry.couplet.isNotBlank()) {
                Text(
                    text = entry.couplet,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.90f),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 7f
                        )
                    ),
                    color = Color.White,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Clickable #Hashtags Row (ONLY shown if user/reel actually has tags — never forced!)
            if (entry.tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(entry.tags.take(5)) { tag ->
                        Surface(
                            onClick = { onTagClick(tag) },
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White.copy(alpha = 0.94f),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Tappable Audio Attribution Line — shows Singer name (never uploader name!) for video Reels,
            // or opens Voice & BGM Mixer for non-video Reels
            if (!isVideoReel && isAudioEnabled && voiceState !is ShayarVoicePlaybackState.Idle) {
                ReelVoiceStatusPill(
                    voiceState = voiceState,
                    bgmTrack = activeBgmTrack,
                    isAudioEnabled = isAudioEnabled,
                    onOpenSoundMixer = {
                        if (voiceState is ShayarVoicePlaybackState.Error) {
                            onRetryVoice()
                        } else {
                            onOpenSoundMixer()
                        }
                    }
                )
            } else {
                val videoAudioLabel = when {
                    resolvedSingerName != null && resolvedSongTitle != null ->
                        "सिंगर: $resolvedSingerName • $resolvedSongTitle"
                    resolvedSingerName != null ->
                        "सिंगर: $resolvedSingerName"
                    isDetectingSinger ->
                        "सिंगर पहचाना जा रहा है…"
                    else ->
                        "Original Audio"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            enabled = (!isVideoReel) || (onEditSingerClick != null),
                            onClick = {
                                if (isVideoReel) {
                                    onEditSingerClick?.invoke()
                                } else {
                                    onOpenSoundMixer()
                                }
                            }
                        )
                        .padding(vertical = 2.dp)
                        .testTag("reel_audio_attribution_row")
                ) {
                    Icon(
                        imageVector = if (hasSingerCredit) Icons.Rounded.Mic else Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFFFFD580),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isVideoReel) {
                            videoAudioLabel
                        } else {
                            "${activeBgmTrack.emoji} ${activeBgmTrack.labelHi} • ${activePersona.emoji} ${activePersona.labelHi}"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isVideoReel && onEditSingerClick != null) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "सिंगर का नाम बदलें",
                            tint = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // 8. Sleek Bottom Reel Progress Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.5.dp)
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(reelProgress.coerceIn(0f, 1f))
                    .height(2.5.dp)
                    .background(Color.White.copy(alpha = 0.92f))
            )
        }
    }
}

@Composable
private fun ReelRailAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit,
    iconModifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = iconModifier.size(30.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                shadow = Shadow(Color.Black, Offset(0f, 1f), 5f)
            ),
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun LoopingTextureVideoBackground(
    uriString: String,
    isActive: Boolean,
    isMuted: Boolean,
    onProgressUpdate: (Float) -> Unit,
    onPlaybackError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaPlayerRef = remember(uriString) { arrayOf<MediaPlayer?>(null) }
    val surfaceRef = remember(uriString) { arrayOf<Surface?>(null) }
    val isPreparedRef = remember(uriString) { booleanArrayOf(false) }

    val latestIsActive by rememberUpdatedState(isActive)
    val latestIsMuted by rememberUpdatedState(isMuted)
    val latestOnProgressUpdate by rememberUpdatedState(onProgressUpdate)
    val latestOnPlaybackError by rememberUpdatedState(onPlaybackError)

    LaunchedEffect(isActive, uriString) {
        val player = mediaPlayerRef[0] ?: return@LaunchedEffect
        if (!isPreparedRef[0]) return@LaunchedEffect
        runCatching {
            if (isActive && !player.isPlaying) {
                player.start()
            } else if (!isActive && player.isPlaying) {
                player.pause()
            }
        }
    }

    LaunchedEffect(isMuted, uriString) {
        val player = mediaPlayerRef[0] ?: return@LaunchedEffect
        val vol = if (isMuted) 0f else 1f
        runCatching { player.setVolume(vol, vol) }
    }

    // Track real video playback progress for the bottom bar
    LaunchedEffect(isActive, uriString) {
        if (!isActive) return@LaunchedEffect
        while (true) {
            delay(100L)
            val player = mediaPlayerRef[0]
            if (player != null && isPreparedRef[0]) {
                runCatching {
                    val dur = player.duration
                    if (dur > 0) {
                        latestOnProgressUpdate((player.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }
        }
    }

    DisposableEffect(uriString) {
        onDispose {
            isPreparedRef[0] = false
            runCatching {
                mediaPlayerRef[0]?.stop()
                mediaPlayerRef[0]?.release()
            }
            mediaPlayerRef[0] = null
            runCatching { surfaceRef[0]?.release() }
            surfaceRef[0] = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        isPreparedRef[0] = false
                        val surface = Surface(st)
                        surfaceRef[0] = surface
                        val player = MediaPlayer()
                        mediaPlayerRef[0] = player
                        runCatching {
                            player.setDataSource(context, Uri.parse(uriString))
                            player.setSurface(surface)
                            player.isLooping = true
                            val vol = if (latestIsMuted) 0f else 1f
                            player.setVolume(vol, vol)
                            player.setOnVideoSizeChangedListener { _, videoW, videoH ->
                                if (videoW > 0 && videoH > 0 && width > 0 && height > 0) {
                                    applyCenterCropMatrix(this@apply, width, height, videoW, videoH)
                                }
                            }
                            player.setOnErrorListener { _, _, _ ->
                                isPreparedRef[0] = false
                                latestOnPlaybackError()
                                true
                            }
                            player.setOnPreparedListener { mp ->
                                isPreparedRef[0] = true
                                val currentVol = if (latestIsMuted) 0f else 1f
                                runCatching { mp.setVolume(currentVol, currentVol) }
                                if (latestIsActive) {
                                    runCatching { mp.start() }
                                }
                            }
                            player.prepareAsync()
                        }.onFailure {
                            latestOnPlaybackError()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                        val mp = mediaPlayerRef[0] ?: return
                        if (isPreparedRef[0] && mp.videoWidth > 0 && mp.videoHeight > 0) {
                            applyCenterCropMatrix(this@apply, w, h, mp.videoWidth, mp.videoHeight)
                        }
                    }

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        isPreparedRef[0] = false
                        runCatching {
                            mediaPlayerRef[0]?.stop()
                            mediaPlayerRef[0]?.release()
                        }
                        mediaPlayerRef[0] = null
                        runCatching { surfaceRef[0]?.release() }
                        surfaceRef[0] = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                }
            }
        }
    )
}

private fun applyCenterCropMatrix(
    textureView: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    videoWidth: Int,
    videoHeight: Int
) {
    val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
    val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
    val scaleX: Float
    val scaleY: Float
    if (videoRatio > viewRatio) {
        scaleX = videoRatio / viewRatio
        scaleY = 1f
    } else {
        scaleX = 1f
        scaleY = viewRatio / videoRatio
    }
    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}

@Composable
private fun EmptyReelsPlaceholder(
    isMyReelsTab: Boolean,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.VideoLibrary,
            contentDescription = null,
            tint = Color(0xFFFF4D6D),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (isMyReelsTab) "आपने अभी कोई Reel नहीं बनाई है" else "Reels लोड हो रही हैं…",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "गैलरी से कोई भी Video या Photo चुनें और अपनी शायरी के साथ Instagram जैसी Reel बनाएँ।",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onCreateClick,
            shape = RoundedCornerShape(50)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("पहली Reel बनाएँ")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateReelBottomSheet(
    defaultAuthor: String,
    isSignedInOnline: Boolean,
    onRequestGoogleSignIn: (() -> Unit)?,
    onDismiss: () -> Unit,
    onShowFeedback: (String) -> Unit,
    onPublishReel: (UserCreatedReel) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var caption by remember { mutableStateOf("") }
    var pickedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBackdropKey by remember { mutableStateOf<String?>(BUILT_IN_REEL_BACKDROPS.first().key) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var isPublishing by remember { mutableStateOf(false) }
    var singerNameInput by remember { mutableStateOf("") }
    var detectedSongTitle by remember { mutableStateOf<String?>(null) }
    var userEditedSinger by remember { mutableStateOf(false) }
    var isDetectingSinger by remember { mutableStateOf(false) }
    var singerDetectTrigger by remember { mutableIntStateOf(0) }

    val parsedHashtags = remember(caption) { extractUserHashtags(caption) }

    // Automatically identify the singer whenever a video is picked
    LaunchedEffect(pickedVideoUri, singerDetectTrigger) {
        val uri = pickedVideoUri
        if (uri == null) {
            isDetectingSinger = false
            return@LaunchedEffect
        }
        if (singerDetectTrigger == 0) {
            userEditedSinger = false
            singerNameInput = ""
            detectedSongTitle = null
        }
        isDetectingSinger = true
        val detected = VideoSingerDetector.detectSinger(
            context = context,
            videoUri = uri,
            caption = caption,
            uploaderName = defaultAuthor,
            forceRefresh = singerDetectTrigger > 0
        )
        if (!userEditedSinger && !detected.singerName.isNullOrBlank()) {
            singerNameInput = detected.singerName
        }
        if (!detected.songTitle.isNullOrBlank()) {
            detectedSongTitle = detected.songTitle
        }
        isDetectingSinger = false
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedVideoUri = uri
            pickedPhotoUri = null
            selectedBackdropKey = null
        }
    }

    val videoFileBrowser = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pickedVideoUri = uri
            pickedPhotoUri = null
            selectedBackdropKey = null
        }
    }

    val cameraVideoRecorder = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (success && file != null && file.exists() && file.length() > 0L) {
            pickedVideoUri = Uri.fromFile(file)
            pickedPhotoUri = null
            selectedBackdropKey = null
        } else {
            runCatching { file?.delete() }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri
            pickedVideoUri = null
            selectedBackdropKey = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isPublishing) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🎬 नई Reel बनाएँ",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Row 1: Gallery Video + All Files / WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        runCatching {
                            videoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }.onFailure {
                            videoFileBrowser.launch(arrayOf("video/*"))
                        }
                    },
                    enabled = !isPublishing,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_reel_pick_video_button")
                ) {
                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pickedVideoUri != null) "वीडियो चुना ✓" else "गैलरी वीडियो",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = {
                        runCatching {
                            videoFileBrowser.launch(arrayOf("video/*"))
                        }.onFailure {
                            onShowFeedback("वीडियो फ़ाइल पिकर उपलब्ध नहीं है")
                        }
                    },
                    enabled = !isPublishing,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_reel_browse_files_button")
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "फ़ाइल / WhatsApp",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Row 2: Camera Record + Gallery Photo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val target = ReelsStorage.createCameraVideoTarget(context)
                        if (target == null) {
                            onShowFeedback("कैमरा वीडियो तैयार नहीं हो सका")
                        } else {
                            pendingCameraFile = target.first
                            runCatching {
                                cameraVideoRecorder.launch(target.second)
                            }.onFailure {
                                pendingCameraFile = null
                                runCatching { target.first.delete() }
                                onShowFeedback("इस डिवाइस में कैमरा ऐप उपलब्ध नहीं है")
                            }
                        }
                    },
                    enabled = !isPublishing,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_reel_record_camera_button")
                ) {
                    Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "कैमरा रिकॉर्ड",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = {
                        runCatching {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }.onFailure {
                            onShowFeedback("फ़ोटो पिकर उपलब्ध नहीं है")
                        }
                    },
                    enabled = !isPublishing,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_reel_pick_photo_button")
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pickedPhotoUri != null) "फ़ोटो चुना ✓" else "गैलरी फ़ोटो",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Picked Media Preview Strip OR Optional Backdrop Carousel
            if (pickedPhotoUri != null || pickedVideoUri != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pickedPhotoUri != null) {
                            AsyncImage(
                                model = pickedPhotoUri,
                                contentDescription = "चुनी गई फ़ोटो",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFF4D6D).copy(alpha = 0.18f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Videocam,
                                        contentDescription = "चुना गया वीडियो",
                                        tint = Color(0xFFFF4D6D),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (pickedVideoUri != null) "वीडियो तैयार है ✓" else "फ़ोटो तैयार है ✓",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (pickedVideoUri != null) {
                                    "फ़ुल-स्क्रीन वीडियो Reel"
                                } else {
                                    "फ़ुल-स्क्रीन फ़ोटो Reel"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                pickedVideoUri = null
                                pickedPhotoUri = null
                                singerNameInput = ""
                                detectedSongTitle = null
                                userEditedSinger = false
                                selectedBackdropKey = BUILT_IN_REEL_BACKDROPS.first().key
                            },
                            enabled = !isPublishing
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "मीडिया हटाएँ"
                            )
                        }
                    }
                }

                if (pickedVideoUri != null) {
                    OutlinedTextField(
                        value = singerNameInput,
                        onValueChange = {
                            if (it.length <= 60) {
                                singerNameInput = it
                                userEditedSinger = true
                            }
                        },
                        enabled = !isPublishing,
                        singleLine = true,
                        label = { Text("सिंगर का नाम (गायक)") },
                        placeholder = { Text("जैसे: किशोर कुमार, अरिजीत सिंह, लता मंगेशकर") },
                        supportingText = {
                            Text(
                                text = when {
                                    isDetectingSinger -> "🎵 वीडियो के गाने से सिंगर पहचाना जा रहा है…"
                                    singerNameInput.isNotBlank() && !detectedSongTitle.isNullOrBlank() ->
                                        "गीत: $detectedSongTitle • वीडियो पर सिंगर का नाम दिखेगा"
                                    singerNameInput.isNotBlank() ->
                                        "वीडियो पर यही सिंगर का नाम दिखेगा"
                                    else ->
                                        "अगर वीडियो में किसी सिंगर ने गाया है तो उसका नाम अपने आप आएगा या आप लिख सकते हैं"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = Color(0xFFFF4D6D)
                            )
                        },
                        trailingIcon = {
                            if (isDetectingSinger) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        userEditedSinger = false
                                        singerDetectTrigger++
                                    },
                                    enabled = !isPublishing
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "AI से सिंगर पहचानें"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_reel_singer_input")
                    )
                }
            } else {
                Text(
                    text = "बैकग्राउंड चुनें (या ऊपर से वीडियो/फ़ोटो लें)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(BUILT_IN_REEL_BACKDROPS, key = { it.key }) { preset ->
                        val isSelected = selectedBackdropKey == preset.key
                        Surface(
                            onClick = {
                                if (!isPublishing) {
                                    selectedBackdropKey = preset.key
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFFF4D6D) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .width(72.dp)
                                .height(94.dp)
                                .testTag("reel_backdrop_${preset.key}")
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                OptimizedDrawableImage(
                                    resId = preset.resId,
                                    contentDescription = preset.labelHi,
                                    widthPx = 220,
                                    heightPx = 300,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.72f)
                                                )
                                            )
                                        )
                                )
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFF4D6D),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                            .size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = preset.labelHi,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 4.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // TikTok-Simple Free-Form Caption Input (write ANYTHING — funny video, शायरी, #tags — NO forced categories/tags!)
            OutlinedTextField(
                value = caption,
                onValueChange = { if (it.length <= 360) caption = it },
                enabled = !isPublishing,
                label = { Text("कैप्शन लिखें") },
                placeholder = { Text("कुछ भी लिखें… जैसे: funny video, शायरी, #funny #viral") },
                supportingText = {
                    Text("अपने हिसाब से कोई भी शब्द या #हैशटैग लिखें (वैकल्पिक)")
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_reel_couplet_input")
            )

            // Show live preview chips ONLY for #hashtags that the user themselves typed
            if (parsedHashtags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(parsedHashtags, key = { it }) { tag ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Online Publish Status Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSignedInOnline) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isSignedInOnline) Icons.Rounded.CloudDone else Icons.Rounded.Public,
                        contentDescription = null,
                        tint = if (isSignedInOnline) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isSignedInOnline) {
                            "ऑनलाइन पब्लिश होगी — सभी यूज़र्स देख सकेंगे"
                        } else {
                            "ऑनलाइन पब्लिश के लिए Google से जुड़ें"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isSignedInOnline && onRequestGoogleSignIn != null) {
                        TextButton(
                            onClick = onRequestGoogleSignIn,
                            enabled = !isPublishing
                        ) {
                            Text("लॉगिन", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val canPublish = !isPublishing && (caption.trim().isNotEmpty() || pickedVideoUri != null || pickedPhotoUri != null)
            Button(
                onClick = {
                    if (isPublishing) return@Button
                    isPublishing = true
                    scope.launch {
                        val savedVideoUri = pickedVideoUri?.let {
                            ReelsStorage.persistPickedMedia(context, it, isVideo = true)
                        }
                        val savedPhotoUri = pickedPhotoUri?.let {
                            ReelsStorage.persistPickedMedia(context, it, isVideo = false)
                        }
                        val mediaType = when {
                            savedVideoUri != null -> "video"
                            savedPhotoUri != null -> "photo"
                            else -> "text"
                        }
                        val thumbBase64 = when (mediaType) {
                            "video" -> ReelsStorage.extractCompressedThumbnailBase64(
                                context = context,
                                uriString = savedVideoUri,
                                isVideo = true
                            )
                            "photo" -> ReelsStorage.extractCompressedThumbnailBase64(
                                context = context,
                                uriString = savedPhotoUri,
                                isVideo = false
                            )
                            else -> null
                        }
                        val cleanCaption = caption.trim()
                        val resolvedSinger = if (mediaType == "video") {
                            singerNameInput.trim().take(60).takeIf { it.isNotBlank() }
                                ?: VideoSingerDetector.extractKnownSingerFromText(cleanCaption, defaultAuthor)
                        } else {
                            null
                        }
                        val newReel = UserCreatedReel(
                            id = "reel_${System.currentTimeMillis()}",
                            couplet = cleanCaption,
                            poet = defaultAuthor,
                            categoryId = "",
                            tags = extractUserHashtags(cleanCaption),
                            videoUri = savedVideoUri,
                            photoUri = savedPhotoUri,
                            backgroundDrawableName = if (mediaType == "text") {
                                selectedBackdropKey
                            } else {
                                null
                            },
                            voicePersonaId = null,
                            bgmTrackId = null,
                            mediaType = mediaType,
                            thumbnailBase64 = thumbBase64,
                            singerName = resolvedSinger,
                            songTitle = if (mediaType == "video") detectedSongTitle else null,
                            singerChecked = mediaType == "video" && !isDetectingSinger
                        )
                        isPublishing = false
                        onPublishReel(newReel)
                    }
                },
                enabled = canPublish,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_reel_publish_button")
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reel पोस्ट हो रही है…")
                } else {
                    Text(
                        text = if (isSignedInOnline) "ऑनलाइन पोस्ट करें ✨" else "Reel पोस्ट करें ✨",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditVideoSingerSheet(
    reel: UserCreatedReel,
    onDismiss: () -> Unit,
    onSave: (singerName: String?, songTitle: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var singerInput by remember(reel.id) { mutableStateOf(reel.singerName.orEmpty()) }
    var songInput by remember(reel.id) { mutableStateOf(reel.songTitle.orEmpty()) }
    var isDetecting by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { if (!isDetecting) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🎤 वीडियो सिंगर का नाम",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "इस वीडियो में जिस सिंगर ने गाया है (जैसे किशोर कुमार, अरिजीत सिंह, लता मंगेशकर), उसका नाम लिखें या AI से पहचानें।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = singerInput,
                onValueChange = { if (it.length <= 60) singerInput = it },
                enabled = !isDetecting,
                singleLine = true,
                label = { Text("सिंगर का नाम") },
                placeholder = { Text("जैसे: किशोर कुमार") },
                leadingIcon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_reel_singer_input")
            )

            OutlinedTextField(
                value = songInput,
                onValueChange = { if (it.length <= 80) songInput = it },
                enabled = !isDetecting,
                singleLine = true,
                label = { Text("गाने का नाम (वैकल्पिक)") },
                placeholder = { Text("जैसे: पल पल दिल के पास") },
                leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (!reel.videoUri.isNullOrBlank()) {
                FilledTonalButton(
                    onClick = {
                        if (isDetecting) return@FilledTonalButton
                        isDetecting = true
                        scope.launch {
                            val detected = VideoSingerDetector.detectSinger(
                                context = context,
                                videoUri = Uri.parse(reel.videoUri),
                                caption = reel.couplet,
                                uploaderName = reel.poet,
                                forceRefresh = true
                            )
                            if (!detected.singerName.isNullOrBlank()) {
                                singerInput = detected.singerName
                            }
                            if (!detected.songTitle.isNullOrBlank()) {
                                songInput = detected.songTitle
                            }
                            isDetecting = false
                        }
                    },
                    enabled = !isDetecting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDetecting) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("वीडियो सुनकर सिंगर पहचाना जा रहा है…")
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI से सिंगर पहचानें")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isDetecting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("रद्द करें")
                }
                Button(
                    onClick = {
                        val cleanSinger = singerInput.trim().take(60).takeIf { it.isNotBlank() }
                        val cleanSong = songInput.trim().take(80).takeIf { it.isNotBlank() }
                        onSave(cleanSinger, cleanSong)
                    },
                    enabled = !isDetecting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_reel_singer_save_button")
                ) {
                    Text("सेव करें", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private fun formatCompactCount(count: Int): String {
    return if (count >= 1000) {
        val k = count / 1000.0
        String.format(Locale.US, "%.1fK", k)
    } else {
        count.toString()
    }
}
