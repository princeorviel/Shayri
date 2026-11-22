package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
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
import com.example.ui.theme.LocalShayariReadingStyle

private val CARD_OUTER_SHAPE = RoundedCornerShape(18.dp)
private val CARD_IMAGE_SHAPE = RoundedCornerShape(14.dp)
private val CARD_INNER_BORDER_SHAPE = RoundedCornerShape(10.dp)
private val PILL_SHAPE = RoundedCornerShape(50)

private val SHAYARI_CARD_SCRIM = Brush.verticalGradient(
    colors = listOf(
        Color(0x330B0812),
        Color(0x7C0D0916),
        Color(0xB8090610)
    )
)
private val JOKE_BRUSH_MULLA = Brush.verticalGradient(listOf(Color(0xFF1F6E58), Color(0xFF154E3E)))
private val JOKE_BRUSH_SANTA = Brush.verticalGradient(listOf(Color(0xFF3E46B0), Color(0xFF2B328A)))
private val JOKE_BRUSH_MOTU = Brush.verticalGradient(listOf(Color(0xFFB4471E), Color(0xFF832E10)))
private val JOKE_BRUSH_DEFAULT = Brush.verticalGradient(listOf(Color(0xFF962559), Color(0xFF6B163E)))

private val JOKE_COUPLET_SHADOW = Shadow(
    color = Color.Black.copy(alpha = 0.45f),
    offset = Offset(0f, 1.5f),
    blurRadius = 3f
)
private val COUPLET_SHADOW = Shadow(
    color = Color.Black.copy(alpha = 0.80f),
    offset = Offset(0f, 1.5f),
    blurRadius = 4f
)
private val POET_SHADOW = Shadow(
    color = Color.Black.copy(alpha = 0.75f),
    offset = Offset(0f, 1f),
    blurRadius = 3f
)

@Composable
fun ShayariCardItem(
    shayari: ShayariEntity,
    onOpenReader: () -> Unit,
    onToggleFavorite: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    commentCount: Int = 0,
    cardStyleMode: FeedCardStyleMode = FeedCardStyleMode.PHOTO_CINEMA,
    isOnlinePost: Boolean = false
) {
    val category = ShayariCategory.fromId(shayari.categoryId)
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val moodAccent = if (isDarkTheme) category.darkAccent else category.lightAccent
    val readingStyle = LocalShayariReadingStyle.current
    val artworkResId = remember(
        shayari.id,
        shayari.categoryId,
        shayari.couplet,
        shayari.moodTag
    ) {
        category.backgroundFor(shayari)
    }

    val heartScale by animateFloatAsState(
        targetValue = if (shayari.isFavorite) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "heartBounce"
    )

    Surface(
        shape = CARD_OUTER_SHAPE,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(CARD_OUTER_SHAPE)
            .clickable(onClick = onOpenReader)
            .testTag("shayari_card_${shayari.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (category.isJokeCategory) {
                val panelBrush = when (category) {
                    ShayariCategory.MULLA_NASRUDDIN -> JOKE_BRUSH_MULLA
                    ShayariCategory.SANTA_BANTA -> JOKE_BRUSH_SANTA
                    ShayariCategory.MOTU_PATLU -> JOKE_BRUSH_MOTU
                    else -> JOKE_BRUSH_DEFAULT
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                        .clip(CARD_IMAGE_SHAPE)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(206.dp)
                            .testTag("joke_banner_image_${shayari.id}")
                    ) {
                        OptimizedDrawableImage(
                            resId = artworkResId,
                            contentDescription = category.titleHi,
                            widthPx = 720,
                            heightPx = 480,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(206.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(panelBrush)
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = shayari.couplet,
                            style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge).copy(
                                fontSize = (readingStyle.fontScale.cardSp + 0.5f).sp,
                                lineHeight = ((readingStyle.fontScale.cardSp + 0.5f) * readingStyle.fontStyle.lineHeightFactor).sp,
                                fontWeight = FontWeight.Medium,
                                shadow = JOKE_COUPLET_SHADOW
                            ),
                            color = Color(0xFFFFFBF4),
                            textAlign = TextAlign.Center
                        )

                        if (shayari.poet.isNotBlank() && !shayari.poet.equals(category.titleHi, ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "— ${shayari.poet}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = readingStyle.fontStyle.fontFamily
                                ),
                                color = Color(0xFFF6DFA9),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                YourQuoteCardCanvas(
                    shayari = shayari,
                    category = category,
                    styleMode = cardStyleMode,
                    isOnlinePost = isOnlinePost
                )
            }

            // Compact footer: Category + Emoji on left, Like / Comment / Share on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${category.titleHi} ${category.emoji}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = moodAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("favorite_button_${shayari.id}")
                    ) {
                        Icon(
                            imageVector = if (shayari.isFavorite) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.Rounded.FavoriteBorder
                            },
                            contentDescription = if (shayari.isFavorite) {
                                stringResource(R.string.action_unfavorite)
                            } else {
                                stringResource(R.string.action_favorite)
                            },
                            tint = if (shayari.isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .size(21.dp)
                                .scale(heartScale)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onComment)
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .testTag("comment_button_${shayari.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = stringResource(R.string.action_comment),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                        if (commentCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = commentCount.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_button_${shayari.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.action_share_short),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
