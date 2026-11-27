package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.ReadingAmbientTrack
import com.example.data.model.CardStyle
import com.example.data.model.FontScaleOption
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariFontStyle
import com.example.ui.components.OptimizedDrawableImage
import com.example.ui.components.ReaderAmbientMusicPill
import com.example.ui.components.rememberPagerRevealProgress
import com.example.ui.theme.LocalShayariReadingStyle
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShayariReaderScreen(
    shayariList: List<ShayariEntity>,
    initialShayariId: Int,
    selectedCardStyle: CardStyle,
    selectedFontScale: FontScaleOption,
    selectedFontStyle: ShayariFontStyle = LocalShayariReadingStyle.current.fontStyle,
    showFavoriteAction: Boolean = true,
    onBack: () -> Unit,
    onToggleFavorite: (ShayariEntity) -> Unit,
    onCopyText: (ShayariEntity) -> Unit = {},
    onShareText: (ShayariEntity) -> Unit,
    onDownloadImage: (ShayariEntity) -> Unit,
    onCollab: (ShayariEntity) -> Unit = {},
    onWriteNew: (ShayariEntity) -> Unit = {},
    isAmbientMusicPlaying: Boolean = false,
    activeAmbientTrack: ReadingAmbientTrack = ReadingAmbientTrack.SOFT_PIANO,
    onToggleAmbientMusic: () -> Unit = {},
    onOpenAmbientMusicConfig: () -> Unit = {},
    onCurrentCategoryChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    if (shayariList.isEmpty()) {
        return
    }

    val initialIndex = remember(initialShayariId, shayariList.size) {
        shayariList.indexOfFirst { it.id == initialShayariId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { shayariList.size }
    )
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        snapPositionalThreshold = 0.22f
    )
    val currentShayari = shayariList[pagerState.currentPage.coerceIn(0, shayariList.lastIndex)]
    LaunchedEffect(currentShayari.categoryId) {
        onCurrentCategoryChanged(currentShayari.categoryId)
    }
    var actionMenuShayari by remember { mutableStateOf<ShayariEntity?>(null) }
    var doubleTapHeartTick by remember { mutableIntStateOf(0) }
    var showHeartBurst by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0.4f) }
    val longPressActionLabel = stringResource(R.string.reader_action_sheet_title)

    LaunchedEffect(doubleTapHeartTick) {
        if (doubleTapHeartTick > 0) {
            showHeartBurst = true
            heartScale.snapTo(0.45f)
            heartScale.animateTo(
                targetValue = 1.18f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            delay(360L)
            showHeartBurst = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(selectedCardStyle.startColor, selectedCardStyle.endColor)
                )
            )
    ) {
        // Full-bleed unobstructed Reels-style VerticalPager
        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            flingBehavior = flingBehavior,
            key = { index -> shayariList[index].id },
            modifier = Modifier
                .fillMaxSize()
                .testTag("reader_pager")
        ) { pageIndex ->
            val pageShayari = shayariList[pageIndex]
            val category = ShayariCategory.fromId(pageShayari.categoryId)
            val pageInteractionSource = remember { MutableInteractionSource() }
            val textScrollState = rememberScrollState()
            val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                .absoluteValue
                .coerceIn(0f, 1f)
            val isActivePage = pagerState.currentPage == pageIndex
            val textRevealProgress = rememberPagerRevealProgress(
                isCurrentPage = isActivePage,
                contentKey = pageShayari.id,
                durationMillis = 480,
                delayMillis = 45
            )
            val swipeVisibility = (1f - (0.60f * pageOffset)).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        interactionSource = pageInteractionSource,
                        indication = null,
                        onClick = {},
                        onDoubleClick = {
                            if (showFavoriteAction) {
                                if (!pageShayari.isFavorite) {
                                    onToggleFavorite(pageShayari)
                                }
                                doubleTapHeartTick += 1
                            }
                        },
                        onLongClickLabel = longPressActionLabel,
                        onLongClick = { actionMenuShayari = pageShayari }
                    )
                    .testTag(
                        if (pageIndex == pagerState.currentPage) {
                            "reader_image_area"
                        } else {
                            "reader_image_area_${pageShayari.id}"
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Per-Reel full-bleed background art
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(selectedCardStyle.startColor, selectedCardStyle.endColor)
                            )
                        )
                )

                if (selectedCardStyle.usesArtBackdrop) {
                    OptimizedDrawableImage(
                        resId = category.backgroundFor(pageShayari),
                        contentDescription = stringResource(R.string.cd_card_backdrop),
                        widthPx = 720,
                        heightPx = 1080,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.06f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xAA0E0916),
                                        Color(0x88140C1C),
                                        Color(0xCC0D0814)
                                    )
                                )
                            )
                    )
                }

                // Subtle ornamental frame inside system bars
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(14.dp)
                ) {
                    drawRoundRect(
                        color = selectedCardStyle.accentColor.copy(alpha = 0.28f),
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Centered poetry content — full vertical breathing room without bottom button obstruction
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(start = 32.dp, end = 32.dp, top = 84.dp, bottom = 40.dp)
                        .graphicsLayer {
                            val scaleFactor = 1f - (0.05f * pageOffset)
                            scaleX = scaleFactor
                            scaleY = scaleFactor
                            alpha = (0.18f + 0.82f * textRevealProgress) * swipeVisibility
                        }
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = selectedCardStyle.accentColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, selectedCardStyle.accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "✦  ${category.titleHi}  ✦",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = selectedCardStyle.accentColor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (category.isJokeCategory && selectedCardStyle.usesArtBackdrop) {
                        OptimizedDrawableImage(
                            resId = category.backgroundFor(pageShayari),
                            contentDescription = category.titleHi,
                            widthPx = 720,
                            heightPx = 400,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(185.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    } else {
                        Text(
                            text = "❝",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                            color = selectedCardStyle.accentColor.copy(alpha = 0.72f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(
                                state = textScrollState,
                                enabled = textScrollState.maxValue > 0
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = pageShayari.couplet,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = selectedFontStyle.fontFamily,
                                    fontSize = (selectedFontScale.readerSp + 3f).sp,
                                    lineHeight = ((selectedFontScale.readerSp + 3f) * selectedFontStyle.lineHeightFactor).sp,
                                    letterSpacing = selectedFontStyle.letterSpacingSp.sp
                                ),
                                color = selectedCardStyle.textColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.graphicsLayer {
                                    alpha = textRevealProgress * swipeVisibility
                                    translationY = (1f - textRevealProgress) * 18.dp.toPx()
                                }
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            val poetReveal = ((textRevealProgress - 0.16f) / 0.84f).coerceIn(0f, 1f)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.graphicsLayer {
                                    alpha = poetReveal * swipeVisibility
                                    translationY = (1f - poetReveal) * 10.dp.toPx()
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(1.5.dp)
                                        .background(selectedCardStyle.accentColor.copy(alpha = 0.65f))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = pageShayari.poet,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = selectedFontStyle.fontFamily,
                                        fontSize = 18.sp
                                    ),
                                    color = selectedCardStyle.accentColor
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(1.5.dp)
                                        .background(selectedCardStyle.accentColor.copy(alpha = 0.65f))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subtle right-edge vertical Reels rail indicator (purely visual, no text or numbers)
        if (shayariList.size > 1) {
            ReelsVerticalIndicator(
                currentIndex = pagerState.currentPage,
                totalCount = shayariList.size,
                accentColor = selectedCardStyle.accentColor,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 22.dp)
                    .testTag("reader_reels_indicator")
            )
        }

        // Double-tap floating heart burst overlay
        AnimatedVisibility(
            visible = showHeartBurst,
            exit = fadeOut(animationSpec = tween(220)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF758F).copy(alpha = 0.92f),
                modifier = Modifier
                    .size(96.dp)
                    .scale(heartScale.value)
                    .testTag("reader_double_tap_heart")
            )
        }

        // Minimal Pinned Top Overlay: Back, Subtle Ambient Music Pill, & Favorite (bottom stays 100% free for Reels scrolling)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.30f),
                border = BorderStroke(1.dp, selectedCardStyle.accentColor.copy(alpha = 0.28f))
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("reader_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = selectedCardStyle.textColor
                    )
                }
            }

            ReaderAmbientMusicPill(
                isPlaying = isAmbientMusicPlaying,
                activeTrack = activeAmbientTrack,
                accentColor = selectedCardStyle.accentColor,
                textColor = selectedCardStyle.textColor,
                onTogglePlay = onToggleAmbientMusic,
                onOpenConfig = onOpenAmbientMusicConfig,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (showFavoriteAction) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.30f),
                    border = BorderStroke(1.dp, selectedCardStyle.accentColor.copy(alpha = 0.28f))
                ) {
                    IconButton(
                        onClick = { onToggleFavorite(currentShayari) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reader_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (currentShayari.isFavorite) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.Rounded.FavoriteBorder
                            },
                            contentDescription = stringResource(R.string.action_favorite),
                            tint = if (currentShayari.isFavorite) {
                                Color(0xFFFF758F)
                            } else {
                                selectedCardStyle.textColor
                            }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Long-press Action Bottom Sheet: Copy, Download, Share, Collab, Write
        val activeMenuTarget = actionMenuShayari
        if (activeMenuTarget != null) {
            BackHandler { actionMenuShayari = null }

            ReaderLongPressActionSheet(
                shayari = activeMenuTarget,
                onDismiss = { actionMenuShayari = null },
                onCopy = {
                    actionMenuShayari = null
                    onCopyText(activeMenuTarget)
                },
                onDownload = {
                    actionMenuShayari = null
                    onDownloadImage(activeMenuTarget)
                },
                onShare = {
                    actionMenuShayari = null
                    onShareText(activeMenuTarget)
                },
                onCollab = {
                    actionMenuShayari = null
                    onCollab(activeMenuTarget)
                },
                onWriteNew = {
                    actionMenuShayari = null
                    onWriteNew(activeMenuTarget)
                }
            )
        }
    }
}

@Composable
private fun ReaderLongPressActionSheet(
    shayari: ShayariEntity,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onCollab: () -> Unit,
    onWriteNew: () -> Unit
) {
    val category = ShayariCategory.fromId(shayari.categoryId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .testTag("reader_action_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                )

                // Compact context header showing which shayari was long-pressed
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = shayari.couplet.replace("\n", " "),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${category.emoji} ${category.titleHi} • ${shayari.poet}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Primary Quick Actions Row: Copy, Download, Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Rounded.ContentCopy,
                        label = stringResource(R.string.action_copy_short),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = onCopy,
                        testTag = "reader_action_copy_button",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.Download,
                        label = stringResource(R.string.action_download_short),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onDownload,
                        testTag = "reader_action_download_button",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.Share,
                        label = stringResource(R.string.action_share_short),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = onShare,
                        testTag = "reader_action_share_button",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Creative Actions Row: Collab & Write
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CreativeActionTile(
                        icon = Icons.Rounded.Groups,
                        title = stringResource(R.string.action_collab),
                        subtitle = stringResource(R.string.action_collab_subtitle),
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = onCollab,
                        testTag = "reader_action_collab_button",
                        modifier = Modifier.weight(1f)
                    )
                    CreativeActionTile(
                        icon = Icons.Rounded.EditNote,
                        title = stringResource(R.string.action_write_new),
                        subtitle = stringResource(R.string.action_write_new_subtitle),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        onClick = onWriteNew,
                        testTag = "reader_action_write_button",
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reader_action_cancel_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        modifier = modifier
            .defaultMinSize(minHeight = 76.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CreativeActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = accentColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.34f)),
        modifier = modifier
            .defaultMinSize(minHeight = 74.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.18f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReelsVerticalIndicator(
    currentIndex: Int,
    totalCount: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val visibleSlots = totalCount.coerceAtMost(5)
    val activeSlot = when {
        totalCount <= visibleSlots -> currentIndex.coerceIn(0, visibleSlots - 1)
        currentIndex == 0 -> 0
        currentIndex == totalCount - 1 -> visibleSlots - 1
        else -> {
            val fraction = currentIndex.toFloat() / (totalCount - 1).coerceAtLeast(1).toFloat()
            (1 + (fraction * (visibleSlots - 3)).toInt()).coerceIn(1, visibleSlots - 2)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(visibleSlots) { slot ->
            val isActive = slot == activeSlot
            val dotHeight by animateDpAsState(
                targetValue = if (isActive) 18.dp else 5.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "reel_indicator_height"
            )
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(dotHeight)
                    .clip(CircleShape)
                    .background(
                        if (isActive) {
                            accentColor.copy(alpha = 0.88f)
                        } else {
                            Color.White.copy(alpha = 0.28f)
                        }
                    )
            )
        }
    }
}
