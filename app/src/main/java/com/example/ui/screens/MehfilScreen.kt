package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.shayariCommentKey
import com.example.ui.components.CategoryChipStrip
import com.example.ui.components.FeedCardStyleMode
import com.example.ui.components.FloatingScrollButtons
import com.example.ui.components.InlineCategoryPickerCard
import com.example.ui.components.OptimizedDrawableImage
import com.example.ui.components.ShayariCardItem
import com.example.ui.theme.LocalShayariReadingStyle
import com.example.ui.viewmodel.ShayariUiState
import com.example.ui.viewmodel.ShayariViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MehfilScreen(
    uiState: ShayariUiState,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onOpenReader: (ShayariEntity, List<ShayariEntity>) -> Unit,
    onToggleFavorite: (ShayariEntity) -> Unit,
    onComment: (ShayariEntity) -> Unit,
    onShare: (ShayariEntity) -> Unit,
    commentCounts: Map<String, Int> = emptyMap(),
    onOpenKaviManch: () -> Unit = {},
    onOpenPoetCommunity: (() -> Unit)? = null,
    onOpenCategories: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    onOpenNotificationSettings: () -> Unit = {},
    searchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    searchFocusTrigger: Int = 0,
    refreshTrigger: Int = 0,
    onRefreshApp: () -> Unit = {},
    onShareApp: () -> Unit = {},
    isOwner: Boolean = false,
    showCounts: Boolean = isOwner,
    onTripleTapOwnerToggle: (() -> Unit)? = null,
    onOpenAdminDashboard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var isCategoryGridExpanded by remember { mutableStateOf(false) }
    var selectedCardStyleModeName by remember {
        mutableStateOf(FeedCardStyleMode.PHOTO_CINEMA.name)
    }
    val selectedCardStyleMode = remember(selectedCardStyleModeName) {
        FeedCardStyleMode.entries.firstOrNull { it.name == selectedCardStyleModeName }
            ?: FeedCardStyleMode.PHOTO_CINEMA
    }
    val popularPool = remember(uiState.popularShayari, uiState.featuredShayari) {
        uiState.popularShayari.ifEmpty { listOfNotNull(uiState.featuredShayari) }
    }
    // Track active hero index in a non-state holder so the 5s carousel tick never recomposes MehfilScreen.
    val activePopularIndexRef = remember(popularPool, refreshTrigger) { IntArray(1) }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            isCategoryGridExpanded = false
            listState.animateScrollToItem(0)
        }
    }

    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchInputText by remember { mutableStateOf(uiState.searchQuery) }
    var lastDispatchedSearchText by remember { mutableStateOf(uiState.searchQuery) }
    LaunchedEffect(uiState.searchQuery) {
        if (uiState.searchQuery != lastDispatchedSearchText) {
            searchInputText = uiState.searchQuery
            lastDispatchedSearchText = uiState.searchQuery
        }
    }
    val isSearchActive = searchExpanded || searchInputText.isNotEmpty() || uiState.searchQuery.isNotEmpty()
    var pinnedBarHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(searchFocusTrigger, searchExpanded) {
        if (searchExpanded) {
            listState.animateScrollToItem(0)
            if (searchFocusTrigger > 0) {
                delay(120L)
                runCatching { searchFocusRequester.requestFocus() }
                keyboardController?.show()
            }
        }
    }

    LaunchedEffect(uiState.selectedCategoryId) {
        if (listState.firstVisibleItemIndex > 2) {
            listState.animateScrollToItem(2)
        }
    }

    val refreshScope = rememberCoroutineScope()
    var isPullRefreshing by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val collapseRangePx = remember(density) { with(density) { 156.dp.toPx() } }
    val collapseProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / collapseRangePx).coerceIn(0f, 1f)
            }
        }
    }
    val lastTapTimeMs = remember { longArrayOf(0L) }
    val tapCount = remember { intArrayOf(0) }
    val handleTitleTap: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastTapTimeMs[0] <= 900L) {
            tapCount[0] += 1
        } else {
            tapCount[0] = 1
        }
        lastTapTimeMs[0] = now
        if (tapCount[0] >= 3 && onTripleTapOwnerToggle != null) {
            tapCount[0] = 0
            onTripleTapOwnerToggle()
        } else {
            onRefreshApp()
            refreshScope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isPullRefreshing,
        onRefresh = {
            if (!isPullRefreshing) {
                isPullRefreshing = true
                onRefreshApp()
                refreshScope.launch {
                    delay(450L)
                    isPullRefreshing = false
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("mehfil_feed_list"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Collapsing Parallax Cover Photo (morphs smoothly into pinned top bar on scroll;
            //    yields exact dynamic top spacing when the pinned Search drawer is active so results show immediately)
            item(key = "header") {
                if (isSearchActive) {
                    val spacerHeight = if (pinnedBarHeightPx > 0) {
                        with(density) { pinnedBarHeightPx.toDp() }
                    } else {
                        178.dp
                    }
                    Spacer(modifier = Modifier.height(spacerHeight))
                } else {
                    val isBewafaiSelected = uiState.selectedCategoryId == "bewafai"
                    val headerShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)

                    Surface(
                        shape = headerShape,
                        color = Color.Black,
                        shadowElevation = 6.dp,
                        border = if (isBewafaiSelected) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(headerShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clickable {
                                    onCategorySelected(if (isBewafaiSelected) null else "bewafai")
                                }
                                .testTag("home_top_cover_banner")
                        ) {
                            OptimizedDrawableImage(
                                resId = R.drawable.img_home_cover_banner,
                                contentDescription = stringResource(R.string.cd_home_cover_photo),
                                widthPx = 1080,
                                heightPx = 608,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val scrollPx = if (listState.firstVisibleItemIndex == 0) {
                                            listState.firstVisibleItemScrollOffset.toFloat()
                                        } else {
                                            collapseRangePx
                                        }
                                        translationY = scrollPx * 0.42f
                                        alpha = (1f - collapseProgress * 0.55f).coerceIn(0.45f, 1f)
                                    }
                            )

                            // Light top scrim behind floating icons (fades away as solid top bar takes over)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .align(Alignment.TopCenter)
                                    .graphicsLayer { alpha = (1f - collapseProgress).coerceIn(0f, 1f) }
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.48f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // 2.5 Category Filter Bar (scrolls cleanly beneath the pinned collapsing top bar)
            item(key = "category_chips") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
                ) {
                    CategoryChipStrip(
                        selectedCategoryId = uiState.selectedCategoryId,
                        onSelectCategory = onCategorySelected,
                        categoryCounts = uiState.categoryCounts,
                        showCounts = showCounts,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )
                }
            }

            // 2.6 Easy Inline Expandable "सभी श्रेणियाँ चुनें" 3-column Grid Panel
            item(key = "inline_categories_grid") {
                AnimatedVisibility(
                    visible = isCategoryGridExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    InlineCategoryPickerCard(
                        selectedCategoryId = uiState.selectedCategoryId,
                        categoryCounts = uiState.categoryCounts,
                        showCounts = showCounts,
                        onSelectCategory = { catId ->
                            onCategorySelected(catId)
                            isCategoryGridExpanded = false
                        },
                        onClose = { isCategoryGridExpanded = false },
                        isOwner = isOwner,
                        onOpenAdminDashboard = onOpenAdminDashboard,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // 3. Featured Daily Shayari Hero Card + Mobile Read/Write Dashboard Strip (or Active Category Banner)
            val activeSelectedCategory = uiState.selectedCategoryId?.let { ShayariCategory.fromId(it) }
            if (activeSelectedCategory != null) {
                item(key = "active_category_banner") {
                    ActiveCategoryBanner(
                        category = activeSelectedCategory,
                        isDarkTheme = isDarkTheme,
                        itemCount = if (showCounts) uiState.filteredShayari.size else null,
                        onClearFilter = {
                            onCategorySelected(null)
                            if (uiState.searchQuery.isNotBlank()) {
                                onSearchQueryChanged("")
                            }
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else if (uiState.searchQuery.isBlank() && popularPool.isNotEmpty()) {
                item(key = "featured_hero") {
                    FeaturedHeroCarousel(
                        popularPool = popularPool,
                        refreshTrigger = refreshTrigger,
                        activeIndexRef = activePopularIndexRef,
                        onOpen = { shayari ->
                            onOpenReader(shayari, uiState.allShayari)
                        },
                        onToggleFavorite = onToggleFavorite,
                        onShare = onShare,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }

                item(key = "kavi_manch_invite") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KaviManchInviteBanner(
                            onClick = onOpenKaviManch
                        )

                        MobileQuickActionStrip(
                            onQuickRead = {
                                val nextIndex = (activePopularIndexRef[0] + 1) % popularPool.size.coerceAtLeast(1)
                                val target = popularPool.getOrNull(nextIndex) ?: popularPool.first()
                                onOpenReader(target, popularPool.ifEmpty { uiState.allShayari })
                            },
                            onQuickWrite = onOpenKaviManch
                        )

                        if (onOpenPoetCommunity != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onOpenPoetCommunity)
                                    .testTag("mehfil_poet_community_button")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.account_open_kavi_manch_title),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stringResource(R.string.account_open_kavi_manch_subtitle),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Active Collection Count Bar
            item(key = "feed_count_bar") {
                val activeCategoryTitle = activeSelectedCategory?.titleHi
                    ?: stringResource(R.string.all_categories)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            uiState.searchQuery.isNotBlank() ->
                                "“${uiState.searchQuery.trim()}” • ${uiState.filteredShayari.size} शायरियाँ मिलीं"
                            showCounts ->
                                "$activeCategoryTitle • ${uiState.filteredShayari.size} शायरी"
                            else ->
                                activeCategoryTitle
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.selectedCategoryId != null || uiState.searchQuery.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.reset_category_filter),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    searchInputText = ""
                                    lastDispatchedSearchText = ""
                                    onCategorySelected(null)
                                    onSearchQueryChanged("")
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("reset_filters_button")
                        )
                    }
                }
            }

            // 4.5 YourQuote-style Feed Card Look Selector Strip
            item(key = "feed_card_style_strip") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feed_card_style_strip")
                ) {
                    items(FeedCardStyleMode.entries, key = { it.name }) { mode ->
                        val isSelected = selectedCardStyleMode == mode
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.4.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                }
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { selectedCardStyleModeName = mode.name }
                                .testTag("feed_card_style_${mode.name}")
                        ) {
                            Text(
                                text = "${mode.emoji} ${mode.labelHi}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // 5. Shayari Feed, Loading State, or Empty Search State
            if (uiState.filteredShayari.isEmpty() && uiState.isLoading) {
                item(key = "feed_loading_card") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "शायरियाँ लोड हो रही हैं…",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (uiState.filteredShayari.isEmpty() && !uiState.isLoading) {
                item(key = "empty_search") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.empty_search_title),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.empty_search_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.filteredShayari,
                    key = { it.id },
                    contentType = {
                        if (ShayariCategory.fromId(it.categoryId).isJokeCategory) "joke_card" else "shayari_card"
                    }
                ) { shayari ->
                    ShayariCardItem(
                        shayari = shayari,
                        commentCount = commentCounts[shayariCommentKey(shayari.id)] ?: 0,
                        cardStyleMode = selectedCardStyleMode,
                        isOnlinePost = shayari.id >= ShayariViewModel.ONLINE_COMMUNITY_ID_BASE,
                        onOpenReader = { onOpenReader(shayari, uiState.filteredShayari) },
                        onToggleFavorite = { onToggleFavorite(shayari) },
                        onComment = { onComment(shayari) },
                        onShare = { onShare(shayari) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }

        // Pinned Collapsing Top Bar: transparent floating icons over cover -> solid elevated header bar on scroll or active Search
        val effectiveBarProgress = if (isSearchActive) 1f else collapseProgress
        val surfaceBg = MaterialTheme.colorScheme.surface
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        val primaryColor = MaterialTheme.colorScheme.primary
        val outlineColor = MaterialTheme.colorScheme.outlineVariant
        val barBgColor = surfaceBg.copy(alpha = effectiveBarProgress * 0.98f)
        val iconBgColor = Color.Black.copy(alpha = 0.44f * (1f - effectiveBarProgress))
        val iconBorderColor = lerp(
            Color.White.copy(alpha = 0.28f),
            outlineColor.copy(alpha = 0.45f),
            effectiveBarProgress
        )
        val iconTint = lerp(Color.White, onSurfaceColor, effectiveBarProgress)
        val titleAlpha = ((effectiveBarProgress - 0.3f) / 0.7f).coerceIn(0f, 1f)
        val searchPillBgColor = if (isSearchActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            lerp(
                Color.Black.copy(alpha = 0.56f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                effectiveBarProgress
            )
        }
        val searchPillContentColor = if (isSearchActive) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            lerp(Color.White, MaterialTheme.colorScheme.onPrimaryContainer, effectiveBarProgress)
        }
        val searchPillBorderColor = if (isSearchActive) {
            primaryColor.copy(alpha = 0.75f)
        } else {
            lerp(
                Color.White.copy(alpha = 0.48f),
                primaryColor.copy(alpha = 0.55f),
                effectiveBarProgress
            )
        }

        Surface(
            color = barBgColor,
            shadowElevation = (4f * effectiveBarProgress).dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .onSizeChanged { pinnedBarHeightPx = it.height }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Real Circular Shayari Logo + morphing "शायरी" title when collapsed
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(onClick = handleTitleTap)
                            .padding(end = 6.dp)
                            .testTag("header_title_refresh_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            border = BorderStroke(1.5.dp, Color(0xFFFFB74D).copy(alpha = 0.9f)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                        ) {
                            OptimizedDrawableImage(
                                resId = R.drawable.shayari_heartbreak_logo_1795079888557,
                                contentDescription = stringResource(R.string.app_name),
                                widthPx = 160,
                                heightPx = 160,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (titleAlpha > 0.02f) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = primaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.graphicsLayer {
                                    alpha = titleAlpha
                                    translationX = -10f * (1f - titleAlpha)
                                }
                            )
                        }
                    }

                    // Action Buttons: Larger prominent Search pill + clean toolbar icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (isSearchActive) {
                                    searchInputText = ""
                                    lastDispatchedSearchText = ""
                                    keyboardController?.hide()
                                    if (!searchExpanded) {
                                        onSearchQueryChanged("")
                                    } else {
                                        onToggleSearch()
                                    }
                                } else {
                                    onToggleSearch()
                                    refreshScope.launch { listState.animateScrollToItem(0) }
                                }
                            },
                            shape = CircleShape,
                            color = searchPillBgColor,
                            border = BorderStroke(1.2.dp, searchPillBorderColor),
                            shadowElevation = if (isSearchActive) 2.dp else 4.dp,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .testTag("search_toggle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                    contentDescription = stringResource(R.string.search_hint),
                                    tint = searchPillContentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onOpenNotificationSettings,
                            shape = CircleShape,
                            color = iconBgColor,
                            border = BorderStroke(0.75.dp, iconBorderColor),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(36.dp)
                                .testTag("notification_settings_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotificationCount > 0) {
                                            Badge(
                                                containerColor = primaryColor,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.testTag("notification_unread_badge")
                                            ) {
                                                Text(
                                                    text = if (unreadNotificationCount > 99) {
                                                        "99+"
                                                    } else {
                                                        unreadNotificationCount.toString()
                                                    },
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (unreadNotificationCount > 0) {
                                            Icons.Rounded.NotificationsActive
                                        } else {
                                            Icons.Outlined.Notifications
                                        },
                                        contentDescription = stringResource(R.string.notification_settings_cd),
                                        tint = if (unreadNotificationCount > 0) {
                                            lerp(Color(0xFFFFB74D), primaryColor, effectiveBarProgress)
                                        } else {
                                            iconTint
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = onShareApp,
                            shape = CircleShape,
                            color = iconBgColor,
                            border = BorderStroke(0.75.dp, iconBorderColor),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(36.dp)
                                .testTag("mehfil_share_app_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = stringResource(R.string.action_share_app),
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onToggleTheme,
                            shape = CircleShape,
                            color = iconBgColor,
                            border = BorderStroke(0.75.dp, iconBorderColor),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(36.dp)
                                .testTag("theme_toggle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = stringResource(R.string.toggle_theme),
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                val willExpand = !isCategoryGridExpanded
                                isCategoryGridExpanded = willExpand
                                onOpenCategories()
                                if (willExpand && listState.firstVisibleItemIndex > 0) {
                                    refreshScope.launch { listState.animateScrollToItem(0) }
                                }
                            },
                            shape = CircleShape,
                            color = primaryColor.copy(alpha = 0.90f),
                            border = if (isCategoryGridExpanded) {
                                BorderStroke(2.dp, Color.White)
                            } else {
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                            },
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(36.dp)
                                .testTag("top_categories_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.GridView,
                                    contentDescription = stringResource(R.string.action_categories),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Pinned Live Search Input + Bilingual Quick Suggestion Chips (always accessible while scrolling!)
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchInputText,
                            onValueChange = { updated ->
                                searchInputText = updated
                                lastDispatchedSearchText = updated
                                onSearchQueryChanged(updated)
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            },
                            trailingIcon = {
                                if (searchInputText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            searchInputText = ""
                                            lastDispatchedSearchText = ""
                                            onSearchQueryChanged("")
                                        },
                                        modifier = Modifier.testTag("clear_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.clear_search)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboardController?.hide() }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = outlineColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .focusRequester(searchFocusRequester)
                                .testTag("search_input")
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(QUICK_SEARCH_MOODS) { (label, queryTerm) ->
                                val isSelected = searchInputText.equals(queryTerm, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) primaryColor else outlineColor
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            val nextQuery = if (isSelected) "" else queryTerm
                                            searchInputText = nextQuery
                                            lastDispatchedSearchText = nextQuery
                                            onSearchQueryChanged(nextQuery)
                                        }
                                        .testTag("quick_search_chip_$queryTerm")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            onSurfaceColor
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (effectiveBarProgress > 0.05f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(outlineColor.copy(alpha = 0.45f * effectiveBarProgress))
                    )
                }
            }
        }

        if (uiState.filteredShayari.isNotEmpty()) {
            FloatingScrollButtons(
                listState = listState,
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias = 1f, verticalBias = 0.86f))
                    .padding(end = 10.dp)
            )
        }
    }
}

@Composable
private fun ActiveCategoryBanner(
    category: ShayariCategory,
    isDarkTheme: Boolean,
    itemCount: Int?,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (isDarkTheme) category.darkAccent else category.lightAccent
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = if (isDarkTheme) 0.16f else 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_category_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.22f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = category.emoji, fontSize = 20.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${category.labelEn} • ${category.titleHi}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (itemCount != null) {
                        "${category.taglineHi} • $itemCount शायरी"
                    } else {
                        category.taglineHi
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onClearFilter,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("active_category_clear_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.reset_category_filter),
                    tint = accent
                )
            }
        }
    }
}

private val DYNAMIC_HERO_COVER_PICS = intArrayOf(
    R.drawable.img_pic_moonlit_boat,
    R.drawable.img_pic_rainy_window,
    R.drawable.img_pic_sunset_silhouettes,
    R.drawable.img_pic_red_roses_night,
    R.drawable.img_pic_autumn_road,
    R.drawable.img_bg_candle_lake,
    R.drawable.img_bg_romantic,
    R.drawable.img_bg_prakriti_rain,
    R.drawable.img_hero_mehfil,
    R.drawable.img_bg_bewafai,
    R.drawable.img_bg_zindagi,
    R.drawable.img_bg_musafir,
    R.drawable.img_bg_secret_love,
    R.drawable.img_bg_aansu,
    R.drawable.img_bg_khuda,
    R.drawable.img_bg_dosti,
    R.drawable.img_bg_motivation,
    R.drawable.img_bg_tareef,
    R.drawable.img_card_mushaira
)

@Composable
private fun FeaturedHeroCarousel(
    popularPool: List<ShayariEntity>,
    refreshTrigger: Int,
    activeIndexRef: IntArray,
    onOpen: (ShayariEntity) -> Unit,
    onToggleFavorite: (ShayariEntity) -> Unit,
    onShare: (ShayariEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var activePopularIndex by remember(popularPool, refreshTrigger) {
        mutableIntStateOf(0)
    }
    var manualAdvanceCounter by remember(popularPool, refreshTrigger) {
        mutableIntStateOf(0)
    }

    LaunchedEffect(popularPool, refreshTrigger, manualAdvanceCounter) {
        if (popularPool.size > 1) {
            while (true) {
                delay(4500L)
                val next = (activePopularIndex + 1) % popularPool.size
                activePopularIndex = next
                activeIndexRef[0] = next
            }
        }
    }

    val activeShayari = popularPool.getOrNull(activePopularIndex) ?: popularPool.firstOrNull() ?: return
    val coverResId = remember(activeShayari.id, activePopularIndex) {
        val categoryPic = ShayariCategory.fromId(activeShayari.categoryId).backgroundFor(activeShayari)
        val curatedPic = DYNAMIC_HERO_COVER_PICS[activePopularIndex % DYNAMIC_HERO_COVER_PICS.size]
        if (activePopularIndex % 2 == 0) categoryPic else curatedPic
    }

    FeaturedHeroCard(
        shayari = activeShayari,
        coverResId = coverResId,
        slideIndex = activePopularIndex,
        totalSlides = popularPool.size,
        onOpen = { onOpen(activeShayari) },
        onNextSlide = {
            if (popularPool.size > 1) {
                val next = (activePopularIndex + 1) % popularPool.size
                activePopularIndex = next
                activeIndexRef[0] = next
                manualAdvanceCounter += 1
            }
        },
        onToggleFavorite = { onToggleFavorite(activeShayari) },
        onShare = { onShare(activeShayari) },
        modifier = modifier
    )
}

@Composable
private fun FeaturedHeroCard(
    shayari: ShayariEntity,
    coverResId: Int,
    slideIndex: Int,
    totalSlides: Int,
    onOpen: () -> Unit,
    onNextSlide: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heroCategory = remember(shayari.categoryId) { ShayariCategory.fromId(shayari.categoryId) }
    val cardShape = RoundedCornerShape(24.dp)
    Surface(
        shape = cardShape,
        color = Color(0xFF0E0912),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFE6BE78).copy(alpha = 0.42f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(onClick = onOpen)
            .testTag("featured_hero_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 276.dp)
        ) {
            // Vivid full-bleed rotating photographic Cover Background
            Crossfade(
                targetState = coverResId,
                animationSpec = tween(durationMillis = 700),
                label = "featured_hero_bg_crossfade",
                modifier = Modifier.matchParentSize()
            ) { currentCoverResId ->
                OptimizedDrawableImage(
                    resId = currentCoverResId,
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    widthPx = 1080,
                    heightPx = 720,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Editorial Cover-Photo scrim: keeps upper & middle photo bright and vibrant, darkens cleanly behind shayari
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x5A0A060E),
                                0.28f to Color(0x1A0A060E),
                                0.56f to Color(0x780D0712),
                                1.0f to Color(0xDE09050D)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 276.dp)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top floating row: Dynamic Mood Pill + Frosted Share/Like Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x99140C18),
                        border = BorderStroke(1.dp, Color(0xFFE6BE78).copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFF3D59B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${stringResource(R.string.featured_badge)} • ${heroCategory.titleHi}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFFFFE5B0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = onShare,
                            shape = CircleShape,
                            color = Color(0x88120A16),
                            border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .testTag("featured_share_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = stringResource(R.string.action_share_short),
                                    tint = Color(0xFFFFF5EB),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onToggleFavorite,
                            shape = CircleShape,
                            color = Color(0x88120A16),
                            border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .testTag("featured_favorite_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (shayari.isFavorite) {
                                        Icons.Rounded.Favorite
                                    } else {
                                        Icons.Rounded.FavoriteBorder
                                    },
                                    contentDescription = stringResource(R.string.action_favorite),
                                    tint = if (shayari.isFavorite) Color(0xFFFF6B8B) else Color(0xFFFFF5EB),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                // Open breathing window so the vibrant cover artwork shows through clearly
                Spacer(modifier = Modifier.height(34.dp))

                // Lower Editorial Shayari Showcase Zone
                val readingStyle = LocalShayariReadingStyle.current
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AnimatedContent(
                        targetState = shayari,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(460)) +
                                slideInVertically(animationSpec = tween(460)) { it / 8 }) togetherWith
                                fadeOut(animationSpec = tween(260))
                        },
                        label = "featured_hero_text_transition"
                    ) { currentShayari ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = currentShayari.couplet,
                                style = readingStyle.heroCoupletTextStyle(MaterialTheme.typography.titleLarge).copy(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.72f),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 6f
                                    )
                                ),
                                color = Color(0xFFFFFAF4)
                            )

                            Text(
                                text = "— ${currentShayari.poet}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = readingStyle.fontStyle.fontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        offset = Offset(0f, 1.5f),
                                        blurRadius = 4f
                                    )
                                ),
                                color = Color(0xFFF3D59B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Bottom bar: Animated Dynamic Indicator Dots + Next Slide + Read Full CTA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val visibleDots = totalSlides.coerceIn(1, 6)
                        val activeDot = slideIndex % visibleDots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(visibleDots) { idx ->
                                val isCurrent = idx == activeDot
                                val dotWidth by animateDpAsState(
                                    targetValue = if (isCurrent) 20.dp else 6.dp,
                                    animationSpec = tween(300),
                                    label = "dynamic_hero_dot_width"
                                )
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(dotWidth)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (isCurrent) {
                                                Color(0xFFF3D59B)
                                            } else {
                                                Color.White.copy(alpha = 0.35f)
                                            }
                                        )
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (totalSlides > 1) {
                                Surface(
                                    onClick = onNextSlide,
                                    shape = RoundedCornerShape(50),
                                    color = Color(0x66000000),
                                    border = BorderStroke(1.dp, Color(0xFFE6BE78).copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("featured_next_slide_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.hero_next_slide_label),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = Color(0xFFFFE5B0)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = stringResource(R.string.hero_next_slide_cd),
                                            tint = Color(0xFFFFE5B0),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.hero_read_full_cta),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color(0xFFFFFAF4)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFFFFFAF4),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KaviManchInviteBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("mehfil_write_invite_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.HistoryEdu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.mehfil_write_invite_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.mehfil_write_invite_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.mehfil_write_invite_cta),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun MobileQuickActionStrip(
    onQuickRead: () -> Unit,
    onQuickWrite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onQuickRead)
                .testTag("mehfil_surprise_read_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.mehfil_quick_read_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onQuickWrite)
                .testTag("mehfil_hinglish_write_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.mehfil_quick_write_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val QUICK_SEARCH_MOODS: List<Pair<String, String>> = listOf(
    "प्यार • Love" to "प्यार",
    "इश्क़ • Ishq" to "इश्क़",
    "बेवफ़ाई • Bewafa" to "बेवफ़ा",
    "दर्द • Sad" to "दर्द",
    "रोमांटिक • Romantic" to "Romantic",
    "Attitude • रॉयल" to "Attitude",
    "दोस्ती • Dosti" to "दोस्ती",
    "ज़िंदगी • Life" to "ज़िंदगी",
    "तन्हाई • Alone" to "तन्हाई",
    "चुटकुले • Funny" to "Funny",
    "ख़ुदा • Khuda" to "ख़ुदा"
)
