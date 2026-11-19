package com.example.ui.components

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CommunityShayari
import com.example.data.model.PersonalShayari
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.toShayariEntity
import com.example.ui.studio.ShayariDesignState
import com.example.ui.theme.LocalShayariReadingStyle
import com.example.util.ShayariShareHelper
import com.example.util.SocialShareLauncher
import com.example.util.SocialSharePlatform

enum class ShareContentMode {
    TEXT,
    IMAGE
}

sealed class ShareTarget {
    data class Catalog(val shayari: ShayariEntity) : ShareTarget()
    data class Personal(val item: PersonalShayari, val authorFallback: String? = null) : ShareTarget()
    data class Community(val post: CommunityShayari) : ShareTarget()
    data class DesignedCard(
        val couplet: String,
        val authorName: String,
        val title: String,
        val tag: String,
        val design: ShayariDesignState
    ) : ShareTarget()
    data object App : ShareTarget()
}

fun ShareTarget.toCardEntity(): ShayariEntity? = when (this) {
    is ShareTarget.Catalog -> shayari
    is ShareTarget.Personal -> item.toShayariEntity(authorFallback.orEmpty())
    is ShareTarget.Community -> post.toShayariEntity()
    is ShareTarget.DesignedCard -> null
    ShareTarget.App -> null
}

fun ShareTarget.toShareText(context: Context): String = when (this) {
    is ShareTarget.Catalog -> ShayariShareHelper.formatShareText(shayari)
    is ShareTarget.Personal -> ShayariShareHelper.formatPersonalShareText(item, authorFallback)
    is ShareTarget.Community -> ShayariShareHelper.formatShareText(post.toShayariEntity())
    is ShareTarget.DesignedCard -> buildString {
        if (title.isNotBlank()) append("✦ ").append(title.trim()).append("\n\n")
        append(couplet.trim())
        if (authorName.isNotBlank()) append("\n\n— ").append(authorName.trim())
        if (tag.isNotBlank()) append("\n#").append(tag.trim().replace(Regex("\\s+"), "_"))
        append(" #शायरी")
    }
    ShareTarget.App -> ShayariShareHelper.formatAppShareText(context)
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
    is ShareTarget.DesignedCard -> {
        title.trim().ifBlank {
            if (authorName.isNotBlank()) "${authorName.trim()} • शायरी" else defaultTitle
        }
    }
    ShareTarget.App -> defaultTitle
}

fun ShareTarget.toCustomBadgeLabel(): String? = when (this) {
    is ShareTarget.Catalog -> null
    is ShareTarget.Personal -> item.themeTag.trim().takeIf { it.isNotEmpty() } ?: "मेरी शायरी"
    is ShareTarget.Community -> null
    is ShareTarget.DesignedCard -> tag.trim().takeIf { it.isNotEmpty() } ?: "मेरी शायरी"
    ShareTarget.App -> null
}

private object SocialBrandColors {
    val WhatsApp = Color(0xFF25D366)
    val Facebook = Color(0xFF1877F2)
    val TwitterX = Color(0xFF0F1419)
    val Threads = Color(0xFF1C1C20)
    val Telegram = Color(0xFF229ED9)
    val Signal = Color(0xFF3A76F0)
    val InstagramStops = listOf(
        Color(0xFFFEDA75),
        Color(0xFFFA7E1E),
        Color(0xFFD62976),
        Color(0xFF962FBF),
        Color(0xFF4F5BD5)
    )
}

@Composable
fun ShareShayariSheet(
    target: ShareTarget,
    isPreparingImage: Boolean,
    onShareToPlatform: (ShareTarget, SocialSharePlatform, ShareContentMode) -> Unit,
    onShareMore: (ShareTarget, ShareContentMode) -> Unit,
    onCopy: (ShareTarget) -> Unit,
    onDownload: (ShareTarget) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !isPreparingImage, onBack = onDismiss)

    val initialMode = remember(target) {
        if (target is ShareTarget.DesignedCard) ShareContentMode.IMAGE else ShareContentMode.TEXT
    }
    var selectedMode by remember(target) { mutableStateOf(initialMode) }

    val readingStyle = LocalShayariReadingStyle.current
    val sheetTitle = if (target is ShareTarget.App) {
        stringResource(R.string.share_app_sheet_title)
    } else {
        stringResource(R.string.share_sheet_title)
    }
    val previewPrimaryText = when (target) {
        is ShareTarget.Catalog -> target.shayari.couplet
        is ShareTarget.Personal -> target.item.couplet
        is ShareTarget.Community -> target.post.couplet
        is ShareTarget.DesignedCard -> target.couplet
        ShareTarget.App -> "✦ ${stringResource(R.string.app_name)} — ${stringResource(R.string.app_subtitle)}"
    }
    val previewSecondaryText = when (target) {
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
        is ShareTarget.DesignedCard -> {
            listOfNotNull(
                target.authorName.takeIf { it.isNotBlank() }?.let { "— $it" },
                target.tag.takeIf { it.isNotBlank() }?.let { "#$it" }
            ).joinToString(" • ").ifBlank { "मेरी शायरी" }
        }
        ShareTarget.App -> SocialShareLauncher.LIVE_WEBSITE_URL
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f),
                            shape = CircleShape
                        )
                )

                // Header
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
                            text = sheetTitle,
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

                // Compact preview card
                Surface(
                    shape = RoundedCornerShape(16.dp),
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
                            text = previewPrimaryText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = readingStyle.fontStyle.fontFamily,
                                fontWeight = if (target is ShareTarget.App) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = previewSecondaryText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Format Mode Toggle (Text vs Photo Card)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShareModePill(
                        label = stringResource(R.string.share_mode_text_pill),
                        icon = Icons.Rounded.Description,
                        selected = selectedMode == ShareContentMode.TEXT,
                        enabled = !isPreparingImage,
                        onClick = { selectedMode = ShareContentMode.TEXT },
                        testTag = "share_option_text",
                        modifier = Modifier.weight(1f)
                    )
                    ShareModePill(
                        label = stringResource(R.string.share_mode_image_pill),
                        icon = Icons.Rounded.Image,
                        selected = selectedMode == ShareContentMode.IMAGE,
                        enabled = !isPreparingImage,
                        onClick = { selectedMode = ShareContentMode.IMAGE },
                        testTag = "share_option_image",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isPreparingImage) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.share_image_generating),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Section Heading
                Text(
                    text = stringResource(R.string.share_trending_heading),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Row 1: WhatsApp, Instagram, Facebook, Twitter (X)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SocialPlatformItem(
                        platform = SocialSharePlatform.WHATSAPP,
                        enabled = !isPreparingImage,
                        onClick = { onShareToPlatform(target, SocialSharePlatform.WHATSAPP, selectedMode) },
                        modifier = Modifier.weight(1f)
                    )
                    SocialPlatformItem(
                        platform = SocialSharePlatform.INSTAGRAM,
                        enabled = !isPreparingImage,
                        onClick = { onShareToPlatform(target, SocialSharePlatform.INSTAGRAM, selectedMode) },
                        modifier = Modifier.weight(1f)
                    )
                    SocialPlatformItem(
                        platform = SocialSharePlatform.FACEBOOK,
                        enabled = !isPreparingImage,
                        onClick = { onShareToPlatform(target, SocialSharePlatform.FACEBOOK, selectedMode) },
                        modifier = Modifier.weight(1f)
                    )
                    SocialPlatformItem(
                        platform = SocialSharePlatform.TWITTER,
                        enabled = !isPreparingImage,
                        onClick = { onShareToPlatform(target, SocialSharePlatform.TWITTER, selectedMode) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Threads, Telegram, Signal, More
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SocialPlatformItem(
                        platform = SocialSharePlatform.THREADS,
                        enabled = !isPreparingImage,
                        onClick = { onShareToPlatform(target, SocialSharePlatform.THREADS, selectedMode) },
                        modifier = Modifier.weight(1f)
                    )
                    SocialPlatformItem(
                        platform = SocialSharePlatform.TELEGRAM,
                        enabled = !isPreparingImage,
                        onClick = { onShareToPlatform(target, SocialSharePlatform.TELEGRAM, selectedMode) },
                        modifier = Modifier.weight(1f)
                    )
                    SocialPlatformItem(
                        platform = SocialSharePlatform.SIGNAL,
                        enabled = !isPreparingImage,
                        onClick = { onShareToPlatform(target, SocialSharePlatform.SIGNAL, selectedMode) },
                        modifier = Modifier.weight(1f)
                    )
                    MoreSharePlatformItem(
                        enabled = !isPreparingImage,
                        onClick = { onShareMore(target, selectedMode) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Utility Actions Row: Copy & Download
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShareUtilityActionButton(
                        icon = Icons.Rounded.ContentCopy,
                        label = stringResource(R.string.action_copy_short),
                        enabled = !isPreparingImage,
                        onClick = { onCopy(target) },
                        testTag = "share_action_copy",
                        modifier = Modifier.weight(1f)
                    )
                    ShareUtilityActionButton(
                        icon = Icons.Rounded.Download,
                        label = stringResource(R.string.action_download_short),
                        enabled = !isPreparingImage,
                        isAccent = true,
                        onClick = { onDownload(target) },
                        testTag = "share_action_download",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ShareModePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = shape,
        color = bgColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SocialPlatformItem(
    platform: SocialSharePlatform,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(platform.labelRes)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp)
            .testTag("share_platform_${platform.id}")
    ) {
        SocialBrandCircleIcon(platform = platform)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MoreSharePlatformItem(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(R.string.share_platform_more)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp)
            .testTag("share_platform_more")
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SocialBrandCircleIcon(platform: SocialSharePlatform) {
    val circleModifier = Modifier
        .size(52.dp)
        .clip(CircleShape)

    when (platform) {
        SocialSharePlatform.WHATSAPP -> {
            Box(
                modifier = circleModifier.background(SocialBrandColors.WhatsApp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(26.dp)) {
                    val w = size.width
                    val h = size.height
                    val strokeW = w * 0.10f
                    // Speech bubble outline + tail
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.42f,
                        center = Offset(w * 0.52f, h * 0.47f),
                        style = Stroke(width = strokeW)
                    )
                    val tail = Path().apply {
                        moveTo(w * 0.22f, h * 0.72f)
                        lineTo(w * 0.10f, h * 0.90f)
                        lineTo(w * 0.34f, h * 0.82f)
                        close()
                    }
                    drawPath(tail, color = Color.White)
                    // Minimal phone handset arc inside
                    val handset = Path().apply {
                        moveTo(w * 0.38f, h * 0.34f)
                        quadraticTo(w * 0.42f, h * 0.58f, w * 0.66f, h * 0.61f)
                    }
                    drawPath(
                        handset,
                        color = Color.White,
                        style = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
                    )
                }
            }
        }

        SocialSharePlatform.INSTAGRAM -> {
            Box(
                modifier = circleModifier.background(
                    Brush.linearGradient(
                        colors = SocialBrandColors.InstagramStops,
                        start = Offset(0f, 140f),
                        end = Offset(140f, 0f)
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    val w = size.width
                    val h = size.height
                    val strokeW = w * 0.095f
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(strokeW, strokeW),
                        size = Size(w - strokeW * 2, h - strokeW * 2),
                        cornerRadius = CornerRadius(w * 0.28f, w * 0.28f),
                        style = Stroke(width = strokeW)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.22f,
                        center = Offset(w * 0.5f, h * 0.5f),
                        style = Stroke(width = strokeW)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.065f,
                        center = Offset(w * 0.74f, h * 0.26f)
                    )
                }
            }
        }

        SocialSharePlatform.FACEBOOK -> {
            Box(
                modifier = circleModifier.background(SocialBrandColors.Facebook),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "f",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        SocialSharePlatform.TWITTER -> {
            Box(
                modifier = circleModifier.background(SocialBrandColors.TwitterX),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(22.dp)) {
                    val w = size.width
                    val h = size.height
                    // Crisp X mark
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.14f, h * 0.12f),
                        end = Offset(w * 0.86f, h * 0.88f),
                        strokeWidth = w * 0.14f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.84f, h * 0.12f),
                        end = Offset(w * 0.16f, h * 0.88f),
                        strokeWidth = w * 0.10f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        SocialSharePlatform.THREADS -> {
            Box(
                modifier = circleModifier.background(SocialBrandColors.Threads),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "@",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        SocialSharePlatform.TELEGRAM -> {
            Box(
                modifier = circleModifier.background(SocialBrandColors.Telegram),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    val w = size.width
                    val h = size.height
                    val plane = Path().apply {
                        moveTo(w * 0.90f, h * 0.14f)
                        lineTo(w * 0.08f, h * 0.46f)
                        lineTo(w * 0.40f, h * 0.58f)
                        lineTo(w * 0.78f, h * 0.26f)
                        lineTo(w * 0.48f, h * 0.64f)
                        lineTo(w * 0.72f, h * 0.84f)
                        close()
                    }
                    drawPath(plane, color = Color.White)
                }
            }
        }

        SocialSharePlatform.SIGNAL -> {
            Box(
                modifier = circleModifier.background(SocialBrandColors.Signal),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(26.dp)) {
                    val w = size.width
                    val h = size.height
                    // Inner solid speech bubble
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.30f,
                        center = Offset(w * 0.5f, h * 0.48f)
                    )
                    val tail = Path().apply {
                        moveTo(w * 0.26f, h * 0.65f)
                        lineTo(w * 0.19f, h * 0.82f)
                        lineTo(w * 0.40f, h * 0.74f)
                        close()
                    }
                    drawPath(tail, color = Color.White)
                    // Outer subtle ring accent
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = w * 0.44f,
                        center = Offset(w * 0.5f, h * 0.5f),
                        style = Stroke(width = w * 0.07f, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareUtilityActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    val bgColor = if (isAccent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isAccent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val iconTint = if (isAccent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = shape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
