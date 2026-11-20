package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ShayariCategory
import com.example.ui.theme.RoseVioletGradientEnd
import com.example.ui.theme.RoseVioletGradientStart

private val PILL_CHIP_SHAPE = RoundedCornerShape(50)
private val INLINE_CARD_SHAPE = RoundedCornerShape(22.dp)
private val INLINE_CHIP_SHAPE = RoundedCornerShape(16.dp)

private val ROSE_VIOLET_GRADIENT = Brush.horizontalGradient(
    colors = listOf(RoseVioletGradientStart, RoseVioletGradientEnd)
)

private val QUICK_INLINE_CATEGORIES: List<ShayariCategory> = listOf(
    ShayariCategory.ROMANTIC,
    ShayariCategory.DOSTI,
    ShayariCategory.ZINDAGI,
    ShayariCategory.DARD,
    ShayariCategory.BEWAFAI,
    ShayariCategory.SHARABI,
    ShayariCategory.MOHABBAT,
    ShayariCategory.PRAKRITI,
    ShayariCategory.HAUSLA,
    ShayariCategory.ATTITUDE,
    ShayariCategory.TANHAI
)

@Composable
fun CategoryChipStrip(
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier,
    categoryCounts: Map<String, Int> = emptyMap(),
    showCounts: Boolean = false
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val totalCount = if (showCounts) categoryCounts.values.sum() else 0
    val coreCategories = ShayariCategory.CORE_FEED_CATEGORIES
    val extraCategories = ShayariCategory.FEED_ORDERED_CATEGORIES.drop(coreCategories.size)
    val rowState = rememberLazyListState()

    LaunchedEffect(selectedCategoryId) {
        if (selectedCategoryId == null) {
            rowState.animateScrollToItem(0)
        } else {
            val coreIdx = coreCategories.indexOfFirst { it.id == selectedCategoryId }
            if (coreIdx >= 0) {
                rowState.animateScrollToItem((coreIdx).coerceAtLeast(0))
            } else {
                val extraIdx = extraCategories.indexOfFirst { it.id == selectedCategoryId }
                if (extraIdx >= 0) {
                    // 1 ("all") + coreCategories.size + 1 (divider) + extraIdx
                    rowState.animateScrollToItem(1 + coreCategories.size + 1 + extraIdx)
                }
            }
        }
    }

    LazyRow(
        state = rowState,
        modifier = modifier.testTag("category_filter_strip"),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "all") {
            val allTitle = stringResource(R.string.all_categories)
            val label = if (showCounts && totalCount > 0) "$allTitle ✨ ($totalCount)" else "$allTitle ✨"
            EmojiCategoryChip(
                label = label,
                selected = selectedCategoryId == null,
                useGradientWhenSelected = true,
                onClick = { onSelectCategory(null) },
                testTag = "category_chip_all"
            )
        }

        // Featured 5 core moods: Mohabbat, Dard, Zindagi, Dosti, Hausla
        items(coreCategories, key = { it.id }) { category ->
            val count = if (showCounts) categoryCounts[category.id] else null
            val accent = if (isDarkTheme) category.darkAccent else category.lightAccent
            val secondaryText = if (showCounts && count != null && count > 0) {
                "${category.titleHi} ($count)"
            } else {
                category.titleHi
            }
            CoreMoodCategoryChip(
                emoji = category.emoji,
                primaryLabel = category.labelEn,
                secondaryLabel = secondaryText,
                selected = selectedCategoryId == category.id,
                accentColor = accent,
                onClick = {
                    if (selectedCategoryId == category.id) {
                        onSelectCategory(null)
                    } else {
                        onSelectCategory(category.id)
                    }
                },
                testTag = "category_chip_${category.id}"
            )
        }

        if (extraCategories.isNotEmpty()) {
            item(key = "core_divider") {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(1.dp)
                        .height(26.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                )
            }

            items(extraCategories, key = { it.id }) { category ->
                val count = if (showCounts) categoryCounts[category.id] else null
                val accent = if (isDarkTheme) category.darkAccent else category.lightAccent
                val label = if (showCounts && count != null && count > 0) {
                    "${category.emoji} ${category.titleHi} ($count)"
                } else {
                    "${category.emoji} ${category.titleHi}"
                }
                EmojiCategoryChip(
                    label = label,
                    selected = selectedCategoryId == category.id,
                    accentColor = accent,
                    onClick = {
                        if (selectedCategoryId == category.id) {
                            onSelectCategory(null)
                        } else {
                            onSelectCategory(category.id)
                        }
                    },
                    testTag = "category_chip_${category.id}"
                )
            }
        }
    }
}

@Composable
fun InlineCategoryPickerCard(
    selectedCategoryId: String?,
    categoryCounts: Map<String, Int>,
    showCounts: Boolean,
    onSelectCategory: (String?) -> Unit,
    onClose: () -> Unit,
    isOwner: Boolean = false,
    onOpenAdminDashboard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val allOrderedCategories = remember {
        QUICK_INLINE_CATEGORIES + ShayariCategory.entries.filter { it !in QUICK_INLINE_CATEGORIES }
    }
    var showAllCategories by remember { mutableStateOf(false) }

    val visibleCategories = remember(showAllCategories, allOrderedCategories, selectedCategoryId) {
        if (showAllCategories) {
            allOrderedCategories
        } else {
            val base = QUICK_INLINE_CATEGORIES
            val activeCategory = selectedCategoryId?.let { ShayariCategory.fromId(it) }
            if (activeCategory != null && activeCategory !in base) {
                listOf(activeCategory) + base.dropLast(1)
            } else {
                base
            }
        }
    }
    val remainingCount = (allOrderedCategories.size - QUICK_INLINE_CATEGORIES.size).coerceAtLeast(0)
    val totalCount = if (showCounts) categoryCounts.values.sum() else 0

    // Pair of (categoryId or null for "All", display label)
    val allTitle = stringResource(R.string.all_categories)
    val chipEntries = remember(visibleCategories, categoryCounts, showCounts, totalCount, allTitle) {
        buildList {
            val allLabel = if (showCounts && totalCount > 0) {
                "✨ $allTitle ($totalCount)"
            } else {
                "✨ $allTitle"
            }
            add(null to allLabel)
            visibleCategories.forEach { cat ->
                val count = categoryCounts[cat.id]
                val label = if (showCounts && count != null && count > 0) {
                    "${cat.emoji} ${cat.titleHi} ($count)"
                } else {
                    "${cat.emoji} ${cat.titleHi}"
                }
                add(cat.id to label)
            }
        }
    }
    val rows = remember(chipEntries) { chipEntries.chunked(3) }

    Surface(
        shape = INLINE_CARD_SHAPE,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("inline_categories_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row: "सभी श्रेणियाँ चुनें" + "बंद करें ×"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.inline_categories_heading),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onClose)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("inline_categories_close_button")
                ) {
                    Text(
                        text = stringResource(R.string.inline_categories_close),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.inline_categories_close),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Clean 3-column grid of rounded category pills
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (catId, label) ->
                            val isSelected = selectedCategoryId == catId
                            InlineGridCategoryPill(
                                label = label,
                                selected = isSelected,
                                onClick = {
                                    onSelectCategory(catId)
                                    onClose()
                                },
                                testTag = if (catId == null) {
                                    "inline_category_chip_all"
                                } else {
                                    "inline_category_chip_$catId"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining slots in the last row so pills keep uniform 3-column width
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Footer actions: Expand all 32 categories + Owner Admin Dashboard shortcut
            if (remainingCount > 0 || (isOwner && onOpenAdminDashboard != null)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (remainingCount > 0) {
                        Surface(
                            shape = PILL_CHIP_SHAPE,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(PILL_CHIP_SHAPE)
                                .clickable(role = Role.Button) {
                                    showAllCategories = !showAllCategories
                                }
                                .testTag("inline_categories_toggle_all_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showAllCategories) {
                                        stringResource(R.string.inline_categories_show_less)
                                    } else {
                                        stringResource(R.string.inline_categories_show_more, remainingCount)
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (showAllCategories) {
                                        Icons.Rounded.ExpandLess
                                    } else {
                                        Icons.Rounded.ExpandMore
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (isOwner && onOpenAdminDashboard != null) {
                        Surface(
                            shape = PILL_CHIP_SHAPE,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(PILL_CHIP_SHAPE)
                                .clickable(role = Role.Button) {
                                    onClose()
                                    onOpenAdminDashboard()
                                }
                                .testTag("inline_categories_admin_dashboard_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = stringResource(R.string.admin_dashboard_title),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Dashboard",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineGridCategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val unselectedBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    val borderStroke = if (selected) {
        null
    } else {
        BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }

    Surface(
        shape = INLINE_CHIP_SHAPE,
        color = if (selected) Color.Transparent else unselectedBg,
        border = borderStroke,
        shadowElevation = if (selected) 3.dp else 0.dp,
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(INLINE_CHIP_SHAPE)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (selected) {
                        Modifier.background(ROSE_VIOLET_GRADIENT)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CoreMoodCategoryChip(
    emoji: String,
    primaryLabel: String,
    secondaryLabel: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val activeFg = if (accentColor.luminance() > 0.48f) {
        Color(0xFF18181F)
    } else {
        Color.White
    }
    val containerColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "coreChipBg"
    )
    val primaryContentColor by animateColorAsState(
        targetValue = if (selected) activeFg else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "coreChipPrimaryFg"
    )
    val secondaryContentColor by animateColorAsState(
        targetValue = if (selected) activeFg.copy(alpha = 0.86f) else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "coreChipSecondaryFg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200),
        label = "coreChipBorder"
    )

    Surface(
        shape = PILL_CHIP_SHAPE,
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 4.dp else 1.5.dp,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 48.dp)
            .clip(PILL_CHIP_SHAPE)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(7.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = primaryLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = primaryContentColor,
                    maxLines = 1
                )
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = secondaryContentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EmojiCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    useGradientWhenSelected: Boolean = false
) {
    val activeBg = accentColor ?: MaterialTheme.colorScheme.primary
    val activeFg = if (useGradientWhenSelected) {
        Color.White
    } else if (activeBg.luminance() > 0.48f) {
        Color(0xFF18181F)
    } else {
        Color.White
    }
    val containerColor by animateColorAsState(
        targetValue = when {
            selected && useGradientWhenSelected -> Color.Transparent
            selected -> activeBg
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(200),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) activeFg else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "chipFg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected && useGradientWhenSelected -> Color.Transparent
            selected -> activeBg
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(200),
        label = "chipBorder"
    )

    Surface(
        shape = PILL_CHIP_SHAPE,
        color = containerColor,
        border = if (selected && useGradientWhenSelected) null else BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 4.dp else 1.5.dp,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 48.dp)
            .clip(PILL_CHIP_SHAPE)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .then(
                    if (selected && useGradientWhenSelected) {
                        Modifier.background(ROSE_VIOLET_GRADIENT)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 15.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
