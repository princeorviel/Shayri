package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
        mutableStateOf(FeedCardStyleMode.MIX_YOURQUOTE.name)
    }
    val selectedCardStyleMode = remember(selectedCardStyleModeName) {
        FeedCardStyleMode.entries.firstOrNull { it.name == selectedCardStyleModeName }
            ?: FeedCardStyleMode.MIX_YOURQUOTE
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

    LaunchedEffect(searchFocusTrigger) {
        if (searchFocusTrigger > 0 && searchExpanded) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(uiState.selectedCategoryId) {
        if (listState.firstVisibleItemIndex > 2) {
            listState.animateScrollToItem(2)
        }
    }

    val refreshScope = rememberCoroutineScope()
    var isPullRefreshing by remember { mutableStateOf(false) }

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
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Editorial Header
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastTapTimeMs = remember { longArrayOf(0L) }
                    val tapCount = remember { intArrayOf(0) }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
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
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .testTag("header_title_refresh_button")
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 30.sp,
                                lineHeight = 40.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        IconButton(
                            onClick = onToggleSearch,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = stringResource(R.string.search_hint),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onOpenNotificationSettings,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("notification_settings_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
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
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = onShareApp,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("mehfil_share_app_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.action_share_app),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = stringResource(R.string.toggle_theme),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            border = if (isCategoryGridExpanded) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                            } else {
                                null
                            },
                            shadowElevation = if (isCategoryGridExpanded) 6.dp else 3.dp,
                            modifier = Modifier
                                .padding(start = 4.dp, end = 4.dp)
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    isCategoryGridExpanded = !isCategoryGridExpanded
                                    onOpenCategories()
                                }
                                .testTag("top_categories_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.GridView,
                                    contentDescription = stringResource(R.string.action_categories),
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Animated Search Bar + Quick Hinglish Mood Chips
            item(key = "search_bar") {
                AnimatedVisibility(
                    visible = searchExpanded || uiState.searchQuery.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChanged,
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
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchQueryChanged("") },
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
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .testTag("search_input")
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(QUICK_SEARCH_MOODS) { (label, queryTerm) ->
                                val isSelected = uiState.searchQuery.equals(queryTerm, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            onSearchQueryChanged(if (isSelected) "" else queryTerm)
                                        }
                                        .testTag("quick_search_chip_$queryTerm")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2.5 Sticky Category Filter Bar (Mohabbat, Dard, Zindagi, Dosti, Hausla + all moods)
            stickyHeader(key = "category_chips") {
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
                        modifier = Modifier.padding(horizontal = 20.dp)
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
                        text = if (showCounts) {
                            "$activeCategoryTitle • ${uiState.filteredShayari.size} शायरी"
                        } else {
                            activeCategoryTitle
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.selectedCategoryId != null || uiState.searchQuery.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.reset_category_filter),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
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

            // 5. Shayari Feed or Empty Search State
            if (uiState.filteredShayari.isEmpty() && !uiState.isLoading) {
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

    LaunchedEffect(popularPool, refreshTrigger) {
        activeIndexRef[0] = 0
        if (popularPool.size > 1) {
            while (true) {
                delay(5000L)
                val next = (activePopularIndex + 1) % popularPool.size
                activePopularIndex = next
                activeIndexRef[0] = next
            }
        }
    }

    val activeShayari = popularPool.getOrNull(activePopularIndex) ?: popularPool.firstOrNull() ?: return
    FeaturedHeroCard(
        shayari = activeShayari,
        onOpen = { onOpen(activeShayari) },
        onToggleFavorite = { onToggleFavorite(activeShayari) },
        onShare = { onShare(activeShayari) },
        modifier = modifier
    )
}

@Composable
private fun FeaturedHeroCard(
    shayari: ShayariEntity,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .testTag("featured_hero_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 196.dp)
        ) {
            Crossfade(
                targetState = shayari,
                animationSpec = tween(durationMillis = 550),
                label = "featured_hero_bg_crossfade",
                modifier = Modifier.matchParentSize()
            ) { currentShayari ->
                val heroCategory = ShayariCategory.fromId(currentShayari.categoryId)
                OptimizedDrawableImage(
                    resId = heroCategory.backgroundFor(currentShayari),
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    widthPx = 800,
                    heightPx = 480,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Balanced twilight scrim for crisp couplet readability
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x9E190C18),
                                Color(0xD4120914)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFE6BE78).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFE6BE78).copy(alpha = 0.45f))
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
                                text = stringResource(R.string.featured_badge),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFFF3D59B)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("featured_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.action_share_short),
                                tint = Color(0xFFF7EDE2)
                            )
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("featured_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (shayari.isFavorite) {
                                    Icons.Rounded.Favorite
                                } else {
                                    Icons.Rounded.FavoriteBorder
                                },
                                contentDescription = stringResource(R.string.action_favorite),
                                tint = if (shayari.isFavorite) Color(0xFFFF8FA3) else Color(0xFFF7EDE2)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val readingStyle = LocalShayariReadingStyle.current
                AnimatedContent(
                    targetState = shayari,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(420)) +
                            slideInVertically(animationSpec = tween(420)) { it / 10 }) togetherWith
                            fadeOut(animationSpec = tween(260))
                    },
                    label = "featured_hero_text_transition"
                ) { currentShayari ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = currentShayari.couplet,
                            style = readingStyle.heroCoupletTextStyle(MaterialTheme.typography.titleLarge),
                            color = Color(0xFFFFF8F0)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "— ${currentShayari.poet}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = readingStyle.fontStyle.fontFamily
                                ),
                                color = Color(0xFFE6BE78),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.hero_read_full_cta),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color(0xFFFFF8F0)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFFFFF8F0),
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
    "प्यार • Pyar" to "प्यार",
    "इश्क़ • Ishq" to "इश्क़",
    "ज़िंदगी • Zindagi" to "ज़िंदगी",
    "तन्हाई • Tanhai" to "तन्हाई",
    "दर्द • Dard" to "दर्द",
    "ख़ुदा • Khuda" to "ख़ुदा",
    "दोस्ती • Dosti" to "दोस्ती",
    "बेवफ़ा • Bewafa" to "बेवफ़ा"
)
