package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CommunityShayari
import com.example.data.model.PersonalShayari
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.toShayariEntity
import com.example.ui.theme.LocalShayariReadingStyle
import com.example.util.ShayariShareHelper

sealed class ShareTarget {
    data class Catalog(val shayari: ShayariEntity) : ShareTarget()
    data class Personal(val item: PersonalShayari, val authorFallback: String? = null) : ShareTarget()
    data class Community(val post: CommunityShayari) : ShareTarget()
}

fun ShareTarget.toCardEntity(): ShayariEntity = when (this) {
    is ShareTarget.Catalog -> shayari
    is ShareTarget.Personal -> {
        item.toShayariEntity(authorFallback.orEmpty())
    }
    is ShareTarget.Community -> post.toShayariEntity()
}

fun ShareTarget.toShareText(): String = when (this) {
    is ShareTarget.Catalog -> ShayariShareHelper.formatShareText(shayari)
    is ShareTarget.Personal -> ShayariShareHelper.formatPersonalShareText(item, authorFallback)
    is ShareTarget.Community -> ShayariShareHelper.formatShareText(post.toShayariEntity())
}

fun ShareTarget.toPreviewTitle(defaultTitle: String): String = when (this) {
    is ShareTarget.Catalog -> {
        val cat = ShayariCategory.fromId(shayari.categoryId)
        "${shayari.poet} • ${cat.titleHi}"
    }
    is ShareTarget.Personal -> {
        item.title.ifBlank { item.themeTag.ifBlank { defaultTitle } }
    }
    is ShareTarget.Community -> {
        val cat = ShayariCategory.fromId(post.categoryId)
        "${post.penName} • ${cat.titleHi}"
    }
}

fun ShareTarget.toCustomBadgeLabel(): String? = when (this) {
    is ShareTarget.Catalog -> null
    is ShareTarget.Personal -> item.themeTag.trim().takeIf { it.isNotEmpty() } ?: "मेरी शायरी"
    is ShareTarget.Community -> null
}

@Composable
fun ShareShayariSheet(
    target: ShareTarget,
    isPreparingImage: Boolean,
    onShareText: (ShareTarget) -> Unit,
    onShareImage: (ShareTarget) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !isPreparingImage, onBack = onDismiss)

    val cardEntity = remember(target) { target.toCardEntity() }
    val readingStyle = LocalShayariReadingStyle.current
    val subtitleText = remember(target, cardEntity) {
        when (target) {
            is ShareTarget.Catalog -> {
                val cat = ShayariCategory.fromId(target.shayari.categoryId)
                "— ${target.shayari.poet} • ${cat.titleHi}"
            }
            is ShareTarget.Personal -> {
                val parts = listOfNotNull(
                    target.item.title.takeIf { it.isNotBlank() },
                    target.item.themeTag.takeIf { it.isNotBlank() }?.let { "#$it" }
                )
                parts.joinToString(" • ").ifBlank { "मेरी शायरी" }
            }
            is ShareTarget.Community -> {
                val cat = ShayariCategory.fromId(target.post.categoryId)
                "— ${target.post.penName} • ${cat.titleHi}"
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.54f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isPreparingImage,
                    onClick = onDismiss
                )
        )

        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .testTag("share_options_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f),
                            shape = CircleShape
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_sheet_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isPreparingImage,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_sheet_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Compact quote preview
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = cardEntity.couplet,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = readingStyle.fontStyle.fontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                ShareOptionTile(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.share_option_text_title),
                    subtitle = stringResource(R.string.share_option_text_subtitle),
                    enabled = !isPreparingImage,
                    isLoading = false,
                    onClick = { onShareText(target) },
                    testTag = "share_option_text"
                )

                ShareOptionTile(
                    icon = Icons.Rounded.Image,
                    title = stringResource(R.string.share_option_image_title),
                    subtitle = if (isPreparingImage) {
                        stringResource(R.string.share_image_generating)
                    } else {
                        stringResource(R.string.share_option_image_subtitle)
                    },
                    enabled = !isPreparingImage,
                    isLoading = isPreparingImage,
                    isPrimaryAccent = true,
                    onClick = { onShareImage(target) },
                    testTag = "share_option_image"
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ShareOptionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    testTag: String,
    isPrimaryAccent: Boolean = false
) {
    val containerColor = if (isPrimaryAccent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isPrimaryAccent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val iconContainerColor = if (isPrimaryAccent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val iconTint = if (isPrimaryAccent) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconContainerColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = iconTint,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
