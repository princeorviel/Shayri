package com.example.ui.screens

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Shadow
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
import com.example.ui.components.resolveDiversePhotoForCard
import com.example.ui.theme.LocalShayariReadingStyle
import com.example.util.ReelsStorage
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
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
    val categoryId: String,
    val tags: List<String>,
    val videoUri: String? = null,
    val photoUri: String? = null,
    val backgroundDrawableName: String? = null,
    val voicePersonaId: String? = null,
    val bgmTrackId: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

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
    return ShayariEntity(
        id = deterministicId,
        couplet = couplet.ifBlank { "🎬 मेरी शायरी Reel" },
        poet = poet.ifBlank { "शायर दोस्त" },
        categoryId = categoryId,
        moodTag = tags.joinToString(", ")
    )
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
    onDeleteUserReel: ((UserCreatedReel) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ReelsFeedTab.FOR_YOU) }
    var activeTagFilter by remember { mutableStateOf<String?>(null) }
    var isAudioEnabled by remember { mutableStateOf(true) }
    var isCurrentReelPaused by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showSoundMixerSheet by remember { mutableStateOf(false) }

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
    val latestIsPaused by rememberUpdatedState(isCurrentReelPaused)

    fun releaseVoicePlayer() {
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

    val reelsList: List<ReelEntry> = remember(allShayari, popularShayari, userReels, selectedTab, activeTagFilter) {
        val base: List<ReelEntry> = when (selectedTab) {
            ReelsFeedTab.MY_REELS -> userReels.map { ReelEntry.CustomReel(it) }
            ReelsFeedTab.TRENDING -> {
                val custom = userReels.map { ReelEntry.CustomReel(it) }
                val pool = popularShayari.ifEmpty { allShayari.take(120) }
                    .filterNot { ShayariCategory.fromId(it.categoryId).isJokeCategory }
                    .map { ReelEntry.ShayariReel(it) }
                custom + pool
            }
            ReelsFeedTab.FOR_YOU -> {
                val custom = userReels.map { ReelEntry.CustomReel(it) }
                val curated = buildList {
                    addAll(popularShayari.take(40))
                    val seenIds = popularShayari.take(40).mapTo(HashSet()) { it.id }
                    var step = 0
                    for (item in allShayari) {
                        if (size >= 240) break
                        if (item.id !in seenIds && !ShayariCategory.fromId(item.categoryId).isJokeCategory) {
                            if (step % 7 == 0) add(item)
                            step++
                        }
                    }
                }.map { ReelEntry.ShayariReel(it) }
                custom + curated
            }
        }

        val tagQuery = activeTagFilter?.trim()
        if (tagQuery.isNullOrEmpty()) {
            base
        } else {
            val spec = ShayariTagMatcher.buildSearchSpec(tagQuery)
            base.filter { entry ->
                ShayariTagMatcher.scoreMatch(
                    spec = spec,
                    primaryCategoryId = entry.categoryId,
                    multiCategoryIds = null,
                    normCouplet = ShayariTagMatcher.normalizeForSearch(entry.couplet),
                    normPoet = ShayariTagMatcher.normalizeForSearch(entry.poet),
                    normMoodTag = ShayariTagMatcher.normalizeForSearch(entry.tags.joinToString(", "))
                ) > 0
            }.ifEmpty { base }
        }
    }

    val pagerState = rememberPagerState(pageCount = { reelsList.size.coerceAtLeast(1) })

    LaunchedEffect(selectedTab, activeTagFilter, userReels.size) {
        isCurrentReelPaused = false
        if (pagerState.currentPage != 0 && reelsList.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentReelPaused = false
    }

    val currentEntry = reelsList.getOrNull(pagerState.currentPage)
    val currentHasVideoAudio = currentEntry is ReelEntry.CustomReel && currentEntry.reel.videoUri != null

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
            ?: ReelBgmTrack.TIKTOK_SAD_GUITAR
    }

    // 1. Instant Zero-Noise TikTok Background Music sync
    LaunchedEffect(isAudioEnabled, isCurrentReelPaused, showCreateSheet, effectiveBgmTrack, currentHasVideoAudio, currentEntry?.stableKey) {
        bgmPlayer.setTrack(effectiveBgmTrack)
        val shouldPlayBgm = isAudioEnabled &&
            !isCurrentReelPaused &&
            !showCreateSheet &&
            currentEntry != null &&
            !currentHasVideoAudio &&
            effectiveBgmTrack != ReelBgmTrack.OFF
        bgmPlayer.setPlaying(shouldPlayBgm)
    }

    // 2. Sync Pause/Resume on currently loaded Shayar voice MediaPlayer & Local Fallback
    LaunchedEffect(isCurrentReelPaused, showCreateSheet, isAudioEnabled) {
        val shouldPause = isCurrentReelPaused || showCreateSheet || !isAudioEnabled
        if (shouldPause) {
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
        } else if (!shouldPause && voicePlaybackState is ShayarVoicePlaybackState.QuotaFallback) {
            val entry = currentEntry
            if (entry != null && entry.couplet.isNotBlank() && !currentHasVideoAudio) {
                localFallbackSpeaker.speak(
                    couplet = entry.couplet,
                    persona = effectivePersona,
                    onSpeakingChanged = { speaking ->
                        bgmPlayer.setDucked(speaking)
                    }
                )
            }
        }
    }

    // 3. Expressive Human-Like Shayar Voice via Gemini Flash TTS (quota-safe: single active reel only,
    // durable disk cache, automatic local Hindi neural voice fallback if Gemini quota/App Check/network fails)
    LaunchedEffect(currentEntry?.stableKey, effectivePersona, isAudioEnabled, showCreateSheet, voiceRetryTick) {
        releaseVoicePlayer()
        val entry = currentEntry
        if (!isAudioEnabled || showCreateSheet || entry == null || entry.couplet.isBlank() || currentHasVideoAudio) {
            voicePlaybackState = ShayarVoicePlaybackState.Idle
            return@LaunchedEffect
        }

        // Fast-path: if already cached in durable filesDir, play immediately with ZERO delay & ZERO quota use!
        val cachedFile = ShayarVoiceEngine.getCachedVoiceOrNull(
            context = context,
            couplet = entry.couplet,
            categoryId = entry.categoryId,
            persona = effectivePersona
        )

        val result: ShayarVoiceResult = if (cachedFile != null) {
            ShayarVoiceResult.Ready(cachedFile)
        } else {
            // Wait 380ms so rapid vertical scrolling through Reels never wastes Gemini TTS quota!
            delay(380L)
            voicePlaybackState = ShayarVoicePlaybackState.Loading
            ShayarVoiceEngine.getOrGenerateVoice(
                context = context,
                couplet = entry.couplet,
                categoryId = entry.categoryId,
                persona = effectivePersona
            )
        }

        val activeReelKey = entry.stableKey
        val activePersonaSnapshot = effectivePersona

        fun startLocalRecitationWithReplay(reasonMessage: String) {
            voicePlaybackState = ShayarVoicePlaybackState.QuotaFallback(activePersonaSnapshot, reasonMessage)
            if (latestIsPaused || !isAudioEnabled || showCreateSheet) return

            fun speakOnce() {
                if (latestIsPaused || !isAudioEnabled || showCreateSheet || currentEntry?.stableKey != activeReelKey) {
                    return
                }
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
                                currentEntry?.stableKey == activeReelKey &&
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

        when (result) {
            is ShayarVoiceResult.FallbackRequired -> {
                // Transparently track AI status (Quota / App Check) while immediately reciting aloud in Hindi!
                startLocalRecitationWithReplay(result.message)
            }
            is ShayarVoiceResult.Error -> {
                // Even on transient network/timeout errors, never leave the Reel silent — recite with local speaker!
                startLocalRecitationWithReplay(result.message)
            }
            is ShayarVoiceResult.Ready -> {
                val mp = MediaPlayer()
                val prepared = try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        mp.setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        mp.setDataSource(result.audioFile.absolutePath)
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

                if (!prepared) {
                    startLocalRecitationWithReplay("ऑडियो प्लेयर रीकवरी — लोकल आवाज़ चालू")
                    return@LaunchedEffect
                }

                voicePlayerRef[0] = mp

                mp.setOnErrorListener { _, _, _ ->
                    releaseVoicePlayer()
                    startLocalRecitationWithReplay("ऑडियो प्लेयर एरर — लोकल आवाज़ चालू")
                    true
                }

                mp.setOnCompletionListener { completedPlayer ->
                    bgmPlayer.setDucked(false)
                    voicePlaybackState = ShayarVoicePlaybackState.Idle
                    scope.launch {
                        // Let the background music swell gracefully before softly reciting again
                        delay(2_600L)
                        if (voicePlayerRef[0] === completedPlayer && !latestIsPaused && isAudioEnabled && !showCreateSheet) {
                            runCatching {
                                completedPlayer.seekTo(0)
                                completedPlayer.start()
                                bgmPlayer.setDucked(true)
                                voicePlaybackState = ShayarVoicePlaybackState.Speaking(effectivePersona)
                            }
                        }
                    }
                }

                if (!latestIsPaused && isAudioEnabled && !showCreateSheet) {
                    runCatching {
                        mp.start()
                        bgmPlayer.setDucked(true)
                        voicePlaybackState = ShayarVoicePlaybackState.Speaking(effectivePersona)
                    }
                } else {
                    voicePlaybackState = ShayarVoicePlaybackState.Idle
                }
            }
        }
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
                val isCurrentPage = pagerState.currentPage == page && !showCreateSheet
                val isLiked = entry.stableKey in likedReelKeys
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
                val isFollowed = entry.poet in followedPoets

                ReelPageCard(
                    entry = entry,
                    isActive = isCurrentPage,
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
                    canDelete = selectedTab == ReelsFeedTab.MY_REELS && entry is ReelEntry.CustomReel && onDeleteUserReel != null,
                    onToggleLike = {
                        val next = if (isLiked) likedReelKeys - entry.stableKey else likedReelKeys + entry.stableKey
                        updateLikedKeys(next)
                    },
                    onLikeOnly = {
                        if (!isLiked) {
                            updateLikedKeys(likedReelKeys + entry.stableKey)
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
                        if (entry !is ReelEntry.CustomReel || entry.reel.videoUri == null) {
                            onShowFeedback(
                                if (isAudioEnabled) "🎙️ शायर आवाज़ व म्यूज़िक चालू" else "🔇 आवाज़ व म्यूज़िक बंद"
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
                            updateFollowedPoets(followedPoets - entry.poet)
                        } else {
                            updateFollowedPoets(followedPoets + entry.poet)
                            onShowFeedback("${entry.poet} को फॉलो किया ✨")
                        }
                    },
                    onTagClick = { clickedTag ->
                        activeTagFilter = if (activeTagFilter == clickedTag) null else clickedTag
                        scope.launch { pagerState.scrollToPage(0) }
                    },
                    onDeleteClick = if (entry is ReelEntry.CustomReel && onDeleteUserReel != null) {
                        {
                            onDeleteUserReel(entry.reel)
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
                                    selectedTab = tab
                                    activeTagFilter = null
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
                onDismiss = { showCreateSheet = false },
                onShowFeedback = onShowFeedback,
                onPublishReel = { newReel ->
                    onCreateUserReel(newReel)
                    showCreateSheet = false
                    selectedTab = ReelsFeedTab.FOR_YOU
                    activeTagFilter = null
                    isAudioEnabled = true
                    scope.launch { pagerState.scrollToPage(0) }
                    onShowFeedback("आपकी नई Reel शायर आवाज़ व म्यूज़िक के साथ लाइव हो गई ✨")
                }
            )
        }
    }
}

@Composable
private fun ReelPageCard(
    entry: ReelEntry,
    isActive: Boolean,
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
    val category = remember(entry.categoryId) { ShayariCategory.fromId(entry.categoryId) }
    val readingStyle = LocalShayariReadingStyle.current
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    var showPausePulse by remember { mutableStateOf(false) }
    var reelProgress by remember(entry.stableKey) { mutableFloatStateOf(0f) }
    var videoPlaybackFailed by remember(entry.stableKey) { mutableStateOf(false) }

    val customReel = entry as? ReelEntry.CustomReel
    val videoUri = customReel?.reel?.videoUri?.takeUnless { videoPlaybackFailed }
    val photoUri = customReel?.reel?.photoUri
    val fallbackDrawable = remember(entry.stableKey) {
        when (entry) {
            is ReelEntry.ShayariReel -> resolveDiversePhotoForCard(entry.shayari, category)
            is ReelEntry.CustomReel -> resolvePresetBackdropRes(entry.reel.backgroundDrawableName)
                ?: category.backgroundResId
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
        // 1. Background Media Layer: Real Looping Video OR Cinematic Ken Burns Photo
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
        } else {
            OptimizedDrawableImage(
                resId = fallbackDrawable,
                contentDescription = category.titleHi,
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

        // 2. Cinema Scrim + Subtle Floating Golden Dust Bokeh
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.44f),
                        0.30f to Color.Black.copy(alpha = if (videoUri != null) 0.14f else 0.36f),
                        0.68f to Color.Black.copy(alpha = if (videoUri != null) 0.28f else 0.56f),
                        1.0f to Color.Black.copy(alpha = 0.88f)
                    )
                )
        )

        if (videoUri == null && isActive && !isPaused) {
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

        // 3. Center Shayari Typography Showcase (clean & luminous, never obscured)
        if (entry.couplet.isNotBlank()) {
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
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.40f),
                    border = BorderStroke(0.8.dp, Color(0xFFFFD580).copy(alpha = 0.52f))
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
                    textAlign = TextAlign.Center
                )

                if (videoUri == null && isActive) {
                    ReelVoiceStatusPill(
                        voiceState = voiceState,
                        bgmTrack = activeBgmTrack,
                        isAudioEnabled = isAudioEnabled,
                        onOpenSoundMixer = {
                            if (voiceState is ShayarVoicePlaybackState.Error ||
                                voiceState is ShayarVoicePlaybackState.QuotaFallback
                            ) {
                                onRetryVoice()
                            } else {
                                onOpenSoundMixer()
                            }
                        }
                    )
                }
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
            ReelRailAction(
                icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = formatCompactCount(simulatedLikesCount + if (isLiked) 1 else 0),
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
                .padding(start = 16.dp, end = 78.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = category.darkAccent.copy(alpha = 0.28f),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = entry.poet.trim().take(1).uppercase().ifBlank { "श" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = entry.poet,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(Color.Black, Offset(0f, 1.5f), 6f)
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
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
            }

            // Clickable #Hashtags Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(entry.tags.take(4)) { tag ->
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

            // Tappable Audio Attribution Line — opens Shayar Voice & BGM Mixer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = videoUri == null, onClick = onOpenSoundMixer)
                    .padding(vertical = 2.dp)
                    .testTag("reel_audio_attribution_row")
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFFFD580),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (videoUri != null) {
                        "Original Video Audio • ${entry.poet}"
                    } else {
                        "${activeBgmTrack.emoji} ${activeBgmTrack.labelHi} • ${activePersona.emoji} ${activePersona.labelHi}"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.90f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    onDismiss: () -> Unit,
    onShowFeedback: (String) -> Unit,
    onPublishReel: (UserCreatedReel) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var couplet by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(ShayariCategory.MOHABBAT.id) }
    var selectedTags by remember(selectedCategoryId) {
        mutableStateOf(ShayariTagMatcher.autoTagsForCategory(selectedCategoryId))
    }
    var pickedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBackdropKey by remember { mutableStateOf<String?>(BUILT_IN_REEL_BACKDROPS.first().key) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var isPublishing by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedVideoUri = uri
            pickedPhotoUri = null
            selectedBackdropKey = null
        }
    }

    // Storage Access Framework picker: finds videos in Downloads, WhatsApp, Telegram, Drive, Internal Storage (zero permissions)
    val videoFileBrowser = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pickedVideoUri = uri
            pickedPhotoUri = null
            selectedBackdropKey = null
        }
    }

    // System Camera Video Recorder via FileProvider (zero dangerous permissions; also works with Emulator virtual camera!)
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

    val suggestedTags = remember(selectedCategoryId) {
        ShayariTagMatcher.suggestedTagsForCategory(selectedCategoryId).take(10)
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
                text = "🎬 नई शायरी Reel बनाएँ",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Row 1: Gallery Video + All Files / Downloads Video Browser
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

            // Row 2: Record Live Video with Camera + Gallery Photo
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

            // Helpful tip for virtual preview emulator vs real phone gallery
            Text(
                text = "💡 गैलरी खाली दिखने पर 'फ़ाइल / WhatsApp', 'कैमरा रिकॉर्ड', या नीचे से HD बैकग्राउंड चुनें (असली फ़ोन APK में आपकी पूरी गैलरी दिखेगी)।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Picked Media Preview Strip with Remove Button
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
                                text = if (pickedVideoUri != null) "वीडियो Reel तैयार है ✓" else "फ़ोटो Reel तैयार है ✓",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (pickedVideoUri != null) {
                                    "फ़ुल-स्क्रीन लूपिंग वीडियो Reel बनेगी"
                                } else {
                                    "Ken Burns सिनेमैटिक मोशन Reel बनेगी"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                pickedVideoUri = null
                                pickedPhotoUri = null
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
            } else {
                // Built-in HD Scenic Backgrounds Carousel (instant Reels even without gallery media!)
                Text(
                    text = "या HD सिनेमैटिक बैकग्राउंड चुनें",
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
                                .width(76.dp)
                                .height(102.dp)
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

            OutlinedTextField(
                value = couplet,
                onValueChange = { if (it.length <= 280) couplet = it },
                enabled = !isPublishing,
                label = { Text("Reel पर दिखने वाली शायरी / कैप्शन") },
                placeholder = { Text("यहाँ अपनी 2 या 4 लाइनें लिखें…") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_reel_couplet_input")
            )

            Text(
                text = "कैटेगरी चुनें",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ShayariCategory.entries.filterNot { it.isJokeCategory }, key = { it.id }) { cat ->
                    val selected = cat.id == selectedCategoryId
                    Surface(
                        onClick = { if (!isPublishing) selectedCategoryId = cat.id },
                        shape = RoundedCornerShape(50),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = "${cat.emoji} ${cat.titleHi}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Text(
                text = "ऑटो-टैग्स (टैप करके जोड़ें/हटाएँ)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestedTags, key = { it }) { tag ->
                    val active = selectedTags.any { it.equals(tag, ignoreCase = true) }
                    Surface(
                        onClick = {
                            if (!isPublishing) {
                                selectedTags = if (active) {
                                    selectedTags.filterNot { it.equals(tag, ignoreCase = true) }
                                } else {
                                    selectedTags + tag
                                }
                            }
                        },
                        shape = RoundedCornerShape(50),
                        color = if (active) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = if (active) "✓ #$tag" else "+ #$tag",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            var selectedVoicePersona by remember(selectedCategoryId) {
                mutableStateOf(ShayarVoicePersona.recommendedForCategory(selectedCategoryId))
            }
            var selectedBgmTrack by remember(selectedCategoryId) {
                mutableStateOf(ReelBgmTrack.recommendedForCategory(selectedCategoryId))
            }

            if (pickedVideoUri == null) {
                Text(
                    text = "🎙️ शायर की आवाज़ चुनें",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ShayarVoicePersona.entries, key = { it.id }) { persona ->
                        val active = selectedVoicePersona == persona
                        Surface(
                            onClick = { if (!isPublishing) selectedVoicePersona = persona },
                            shape = RoundedCornerShape(50),
                            color = if (active) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                1.dp,
                                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                text = "${persona.emoji} ${persona.labelHi}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (active) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "🎵 बैकग्राउंड म्यूज़िक चुनें",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReelBgmTrack.entries, key = { it.id }) { track ->
                        val active = selectedBgmTrack == track
                        Surface(
                            onClick = { if (!isPublishing) selectedBgmTrack = track },
                            shape = RoundedCornerShape(50),
                            color = if (active) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                1.dp,
                                if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                text = "${track.emoji} ${track.labelHi}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (active) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            val canPublish = !isPublishing && (couplet.trim().length >= 4 || pickedVideoUri != null || pickedPhotoUri != null)
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
                        val newReel = UserCreatedReel(
                            id = "reel_${System.currentTimeMillis()}",
                            couplet = couplet.trim(),
                            poet = defaultAuthor,
                            categoryId = selectedCategoryId,
                            tags = selectedTags.ifEmpty { ShayariTagMatcher.autoTagsForCategory(selectedCategoryId) },
                            videoUri = savedVideoUri,
                            photoUri = savedPhotoUri,
                            backgroundDrawableName = if (savedVideoUri == null && savedPhotoUri == null) {
                                selectedBackdropKey
                            } else {
                                null
                            },
                            voicePersonaId = if (savedVideoUri == null) selectedVoicePersona.id else null,
                            bgmTrackId = if (savedVideoUri == null) selectedBgmTrack.id else null
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
                    Text("Reel तैयार हो रही है…")
                } else {
                    Text(
                        text = "Reel पोस्ट करें ✨",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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
