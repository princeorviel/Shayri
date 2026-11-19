package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SocialLinkPlatform
import com.example.data.model.SocialLinks

@Composable
fun SocialLinksChipsRow(
    socialLinks: SocialLinks,
    onOpenLink: (SocialLinkPlatform, String) -> Unit,
    modifier: Modifier = Modifier,
    onDarkSurface: Boolean = false,
    onAddLinksClick: (() -> Unit)? = null
) {
    val activeEntries = remember(socialLinks) { socialLinks.activeEntries() }
    if (activeEntries.isEmpty() && onAddLinksClick == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("social_links_chips_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (activeEntries.isEmpty() && onAddLinksClick != null) {
            val containerColor = if (onDarkSurface) {
                Color.White.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            }
            val borderColor = if (onDarkSurface) {
                Color(0xFFE6BE78).copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
            }
            val contentColor = if (onDarkSurface) {
                Color(0xFFF3D59B)
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAddLinksClick)
                    .testTag("social_links_add_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddLink,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.social_links_add_cta),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        } else {
            activeEntries.forEach { (platform, rawValue) ->
                val chipBg = if (onDarkSurface) {
                    Color.White.copy(alpha = 0.14f)
                } else {
                    platform.brandColor.copy(alpha = 0.11f)
                }
                val chipBorder = if (onDarkSurface) {
                    Color.White.copy(alpha = 0.28f)
                } else {
                    platform.brandColor.copy(alpha = 0.32f)
                }
                val labelColor = if (onDarkSurface) {
                    Color(0xFFFFF8F0)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = chipBg,
                    border = BorderStroke(1.dp, chipBorder),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onOpenLink(platform, rawValue) }
                        .testTag("social_link_chip_${platform.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SocialPlatformBadge(platform = platform, size = 24)
                        Text(
                            text = platform.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = labelColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = labelColor.copy(alpha = 0.72f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            if (onAddLinksClick != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (onDarkSurface) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        1.dp,
                        if (onDarkSurface) Color(0xFFE6BE78).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onAddLinksClick)
                        .testTag("social_links_edit_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.social_links_section_title),
                            tint = if (onDarkSurface) Color(0xFFF3D59B) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "लिंक एडिट",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (onDarkSurface) Color(0xFFF3D59B) else MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SocialPlatformBadge(
    platform: SocialLinkPlatform,
    size: Int = 28,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(platform.brandColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = platform.badgeText,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (size <= 24) 10.sp else 11.sp,
            maxLines = 1
        )
    }
}

@Composable
fun SocialLinksEditorFields(
    socialLinks: SocialLinks,
    onSocialLinksChange: (SocialLinks) -> Unit,
    modifier: Modifier = Modifier,
    collapseSecondaryByDefault: Boolean = false
) {
    val hasSecondaryInitial = socialLinks.twitterUrl.isNotBlank() ||
        socialLinks.telegramUrl.isNotBlank() ||
        socialLinks.youtubeUrl.isNotBlank()
    var showMorePlatforms by remember(collapseSecondaryByDefault) {
        mutableStateOf(!collapseSecondaryByDefault || hasSecondaryInitial)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.social_links_section_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.social_links_section_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SocialLinkField(
            platform = SocialLinkPlatform.FACEBOOK,
            value = socialLinks.facebookUrl,
            onValueChange = { onSocialLinksChange(socialLinks.copy(facebookUrl = it.take(160))) }
        )
        SocialLinkField(
            platform = SocialLinkPlatform.INSTAGRAM,
            value = socialLinks.instagramUrl,
            onValueChange = { onSocialLinksChange(socialLinks.copy(instagramUrl = it.take(160))) }
        )
        SocialLinkField(
            platform = SocialLinkPlatform.WHATSAPP,
            value = socialLinks.whatsappUrl,
            onValueChange = { onSocialLinksChange(socialLinks.copy(whatsappUrl = it.take(160))) }
        )

        if (collapseSecondaryByDefault) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showMorePlatforms = !showMorePlatforms }
                    .testTag("social_links_toggle_more")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (showMorePlatforms) {
                            "X, Telegram, YouTube छुपाएँ"
                        } else {
                            "+ X (Twitter), Telegram, YouTube भी जोड़ें"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (showMorePlatforms) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (showMorePlatforms) {
            SocialLinkField(
                platform = SocialLinkPlatform.TWITTER,
                value = socialLinks.twitterUrl,
                onValueChange = { onSocialLinksChange(socialLinks.copy(twitterUrl = it.take(160))) }
            )
            SocialLinkField(
                platform = SocialLinkPlatform.TELEGRAM,
                value = socialLinks.telegramUrl,
                onValueChange = { onSocialLinksChange(socialLinks.copy(telegramUrl = it.take(160))) }
            )
            SocialLinkField(
                platform = SocialLinkPlatform.YOUTUBE,
                value = socialLinks.youtubeUrl,
                onValueChange = { onSocialLinksChange(socialLinks.copy(youtubeUrl = it.take(160))) }
            )
        }
    }
}

@Composable
private fun SocialLinkField(
    platform: SocialLinkPlatform,
    value: String,
    onValueChange: (String) -> Unit
) {
    val trimmed = value.trim()
    val previewUrl = remember(platform, trimmed) {
        if (trimmed.isBlank()) null else platform.normalizeUrl(trimmed)?.removePrefix("https://")
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(platform.label) },
        placeholder = { Text(platform.hint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            SocialPlatformBadge(platform = platform, size = 26)
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                androidx.compose.material3.IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.testTag("social_clear_${platform.id}")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Close,
                        contentDescription = "${platform.label} लिंक हटाएँ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else null,
        supportingText = if (!previewUrl.isNullOrBlank()) {
            {
                Text(
                    text = "लिंक: $previewUrl",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else null,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (platform == SocialLinkPlatform.WHATSAPP) {
                androidx.compose.ui.text.input.KeyboardType.Text
            } else {
                androidx.compose.ui.text.input.KeyboardType.Uri
            },
            imeAction = androidx.compose.ui.text.input.ImeAction.Next
        ),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("social_input_${platform.id}")
    )
}
