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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ShayariCategory

private val CHIP_SHAPE = RoundedCornerShape(14.dp)

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
                    "${category.titleHi} ${category.emoji} ($count)"
                } else {
                    "${category.titleHi} ${category.emoji}"
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
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.surfaceContainerLow,
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
        targetValue = if (selected) accentColor else accentColor.copy(alpha = 0.34f),
        animationSpec = tween(200),
        label = "coreChipBorder"
    )

    Surface(
        shape = CHIP_SHAPE,
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 3.dp else 1.dp,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 48.dp)
            .clip(CHIP_SHAPE)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
    accentColor: Color? = null
) {
    val activeBg = accentColor ?: MaterialTheme.colorScheme.primary
    val activeFg = if (activeBg.luminance() > 0.48f) {
        Color(0xFF18181F)
    } else {
        Color.White
    }
    val containerColor by animateColorAsState(
        targetValue = if (selected) activeBg else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(200),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) activeFg else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "chipFg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) activeBg else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200),
        label = "chipBorder"
    )

    Surface(
        shape = CHIP_SHAPE,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 2.dp else 1.dp,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 48.dp)
            .clip(CHIP_SHAPE)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
