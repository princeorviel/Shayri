package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ShayariSeedData
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.shayariCommentKey
import com.example.ui.components.FloatingScrollButtons
import com.example.ui.components.OptimizedDrawableImage
import com.example.ui.components.ShayariCardItem

private val CATEGORY_CARD_SHAPE = RoundedCornerShape(12.dp)

@Composable
fun CategoriesScreen(
    categoryCounts: Map<String, Int>,
    onCategoryClick: (ShayariCategory) -> Unit,
    onBack: () -> Unit = {},
    isOwner: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val totalShayariCount = categoryCounts.values.sum().takeIf { it > 0 }
        ?: ShayariSeedData.EXPECTED_CATALOG_SIZE

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 18.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("categories_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = stringResource(R.string.action_categories),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isOwner) {
                            "${ShayariCategory.entries.size} कैटेगरी में $totalShayariCount शायरी"
                        } else {
                            "${ShayariCategory.entries.size} दिलकश कैटेगरी"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ShayariCategory.entries, key = { it.id }) { category ->
                    val count = categoryCounts[category.id] ?: 0
                    CategoryImageGridCard(
                        category = category,
                        count = count,
                        showCount = isOwner,
                        onClick = { onCategoryClick(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryImageGridCard(
    category: ShayariCategory,
    count: Int,
    showCount: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CATEGORY_CARD_SHAPE,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(CATEGORY_CARD_SHAPE)
            .clickable(onClick = onClick)
            .testTag("category_grid_card_${category.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            OptimizedDrawableImage(
                resId = category.backgroundResId,
                contentDescription = null,
                widthPx = 320,
                heightPx = 220,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                category.lightAccent.copy(alpha = 0.30f),
                                Color(0xDC101018)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.42f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = category.emoji,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (showCount) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.48f),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.testTag("category_count_badge_${category.id}")
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = category.titleHi,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = category.taglineHi,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 9.5.sp,
                            lineHeight = 12.sp
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDetailScreen(
    category: ShayariCategory,
    shayariList: List<ShayariEntity>,
    onBack: () -> Unit,
    onOpenReader: (ShayariEntity) -> Unit,
    onToggleFavorite: (ShayariEntity) -> Unit,
    onComment: (ShayariEntity) -> Unit,
    onShare: (ShayariEntity) -> Unit,
    commentCounts: Map<String, Int> = emptyMap(),
    isOwner: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val accent = if (isDarkTheme) category.darkAccent else category.lightAccent
    val listState = rememberLazyListState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            // Top Header with Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("category_detail_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = category.emoji)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${category.titleHi} ${category.emoji}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isOwner) {
                            "${category.taglineHi} • ${shayariList.size} शायरी"
                        } else {
                            category.taglineHi
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("category_detail_subtitle")
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    items = shayariList,
                    key = { it.id },
                    contentType = { if (category.isJokeCategory) "joke_card" else "shayari_card" }
                ) { shayari ->
                    ShayariCardItem(
                        shayari = shayari,
                        commentCount = commentCounts[shayariCommentKey(shayari.id)] ?: 0,
                        onOpenReader = { onOpenReader(shayari) },
                        onToggleFavorite = { onToggleFavorite(shayari) },
                        onComment = { onComment(shayari) },
                        onShare = { onShare(shayari) }
                    )
                }
            }
        }

        if (shayariList.isNotEmpty()) {
            FloatingScrollButtons(
                listState = listState,
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias = 1f, verticalBias = 0.36f))
                    .padding(end = 12.dp)
            )
        }
    }
}
