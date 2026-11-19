package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.ThumbUpOffAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CommunityShayari
import com.example.data.model.PoetFollower
import com.example.data.model.PoetProfile
import com.example.data.model.ShayariCategory
import com.example.ui.components.AccountAvatar
import com.example.ui.theme.LocalShayariReadingStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetProfileScreen(
    poetProfile: PoetProfile,
    poetPosts: List<CommunityShayari>,
    followers: List<PoetFollower>,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    userWahIds: Set<String>,
    ownPhotoUrl: Uri? = null,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onEditProfile: () -> Unit,
    onToggleWah: (CommunityShayari) -> Unit,
    onOpenPostAsCard: (CommunityShayari) -> Unit,
    onCommentPost: (CommunityShayari) -> Unit,
    onSharePost: (CommunityShayari) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showFollowersSheet by remember { mutableStateOf(false) }

    val resolvedName = poetProfile.penName.trim().ifBlank { "शायर दोस्त" }
    val resolvedBio = poetProfile.bio.trim().ifBlank { stringResource(R.string.poet_bio_default) }
    val displayedFollowerCount = maxOf(poetProfile.followerCount, followers.size.toLong())
    val displayedPostCount = maxOf(poetProfile.postCount, poetPosts.size.toLong())

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("poet_profile_list"),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top App Header & Hero Poet Card
            item(key = "poet_hero") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("poet_profile_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.poet_profile_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF561928), Color(0xFF281018))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isOwnProfile && ownPhotoUrl != null) {
                                    AccountAvatar(
                                        photoUrl = ownPhotoUrl,
                                        displayName = resolvedName,
                                        isSignedIn = true,
                                        isSelected = true,
                                        size = 76.dp,
                                        contentDescription = resolvedName
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE6BE78).copy(alpha = 0.22f),
                                        border = BorderStroke(2.dp, Color(0xFFF3D59B).copy(alpha = 0.7f)),
                                        modifier = Modifier.size(76.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = resolvedName.firstOrNull()?.toString() ?: "श",
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = Color(0xFFFFF8F0)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = resolvedName,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFFFF8F0),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("poet_profile_name")
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = resolvedBio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE9DAD0),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .testTag("poet_profile_bio")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 3-Column Metrics Bar
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, Color(0xFFE6BE78).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PoetMetricColumn(
                                            count = displayedPostCount,
                                            label = stringResource(R.string.poet_stat_posts),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("poet_stat_posts")
                                        )
                                        PoetMetricColumn(
                                            count = displayedFollowerCount,
                                            label = stringResource(R.string.poet_stat_followers),
                                            onClick = if (followers.isNotEmpty()) {
                                                { showFollowersSheet = true }
                                            } else null,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("poet_stat_followers")
                                        )
                                        PoetMetricColumn(
                                            count = poetProfile.followingCount,
                                            label = stringResource(R.string.poet_stat_following),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("poet_stat_following")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (isOwnProfile) {
                                    FilledTonalButton(
                                        onClick = onEditProfile,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFFE6BE78),
                                            contentColor = Color(0xFF281709)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("poet_edit_profile_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.poet_edit_profile_cta),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                } else {
                                    val followContainer by animateColorAsState(
                                        targetValue = if (isFollowing) {
                                            Color.White.copy(alpha = 0.16f)
                                        } else {
                                            Color(0xFFE6BE78)
                                        },
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "followBtnBg"
                                    )
                                    val followContent by animateColorAsState(
                                        targetValue = if (isFollowing) {
                                            Color(0xFFFFF8F0)
                                        } else {
                                            Color(0xFF281709)
                                        },
                                        label = "followBtnFg"
                                    )

                                    if (isFollowing) {
                                        OutlinedButton(
                                            onClick = onToggleFollow,
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = followContainer,
                                                contentColor = followContent
                                            ),
                                            border = BorderStroke(1.5.dp, Color(0xFFF3D59B)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("poet_follow_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.poet_following_cta),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = onToggleFollow,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = followContainer,
                                                contentColor = followContent
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("poet_follow_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PersonAdd,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.poet_follow_cta),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (followers.isNotEmpty()) {
                item(key = "followers_preview_bar") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showFollowersSheet = true }
                            .testTag("poet_followers_preview_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.poet_followers_heading, followers.size),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = followers.take(3).joinToString(", ") { it.followerName },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Published Shayari Section Header
            item(key = "poet_posts_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.poet_posts_heading, poetPosts.size),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (poetPosts.isEmpty()) {
                item(key = "poet_posts_empty") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Create,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = stringResource(R.string.poet_posts_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(poetPosts, key = { it.id }) { post ->
                    PoetShayariPostCard(
                        post = post,
                        isWahed = userWahIds.contains(post.id),
                        onToggleWah = { onToggleWah(post) },
                        onComment = { onCommentPost(post) },
                        onShare = { onSharePost(post) },
                        onOpen = { onOpenPostAsCard(post) },
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }
        }
    }

    if (showFollowersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFollowersSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.poet_followers_heading, followers.size),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(followers, key = { it.followerUserId.ifBlank { it.id } }) { follower ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = follower.followerName.firstOrNull()?.toString() ?: "श",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Text(
                                text = follower.followerName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PoetMetricColumn(
    count: Long,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    } else {
        modifier
    }
    Column(
        modifier = clickableModifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFF3D59B)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFE9DAD0)
        )
    }
}

@Composable
private fun PoetShayariPostCard(
    post: CommunityShayari,
    isWahed: Boolean,
    onToggleWah: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = ShayariCategory.fromId(post.categoryId)
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val accent = if (isDarkTheme) category.darkAccent else category.lightAccent
    val readingStyle = LocalShayariReadingStyle.current

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpen)
            .testTag("poet_post_card_${post.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${category.emoji} ${category.titleHi}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (post.promptTitle.isNotBlank()) {
                    Text(
                        text = post.promptTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = post.couplet,
                style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isWahed) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isWahed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onToggleWah)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isWahed) Icons.Rounded.ThumbUp else Icons.Rounded.ThumbUpOffAlt,
                            contentDescription = "वाह!",
                            tint = if (isWahed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "वाह! • ${post.wahCount}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isWahed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onComment,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = stringResource(R.string.action_comment),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.action_share_text),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditPoetProfileDialog(
    initialPenName: String,
    initialBio: String,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (penName: String, bio: String) -> Unit
) {
    var penName by remember(initialPenName) { mutableStateOf(initialPenName) }
    var bio by remember(initialBio) { mutableStateOf(initialBio) }
    val cleanPenName = penName.trim()
    val canSave = cleanPenName.length >= 2 && !isSaving

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.poet_edit_profile_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            EditPoetProfileFields(
                penName = penName,
                onPenNameChange = { if (it.length <= 40) penName = it },
                bio = bio,
                onBioChange = { if (it.length <= 160) bio = it }
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(cleanPenName, bio.trim()) },
                enabled = canSave,
                modifier = Modifier.testTag("save_poet_profile_button")
            ) {
                Text(stringResource(R.string.poet_save_profile_cta))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
internal fun EditPoetProfileFields(
    penName: String,
    onPenNameChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = penName,
            onValueChange = onPenNameChange,
            label = { Text(stringResource(R.string.poet_pen_name_label)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_poet_pen_name_input")
        )
        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            label = { Text(stringResource(R.string.poet_bio_label)) },
            placeholder = { Text(stringResource(R.string.poet_bio_hint)) },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_poet_bio_input")
        )
    }
}

@Composable
internal fun EditPoetProfileForm(
    initialPenName: String,
    initialBio: String,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (penName: String, bio: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var penName by remember(initialPenName) { mutableStateOf(initialPenName) }
    var bio by remember(initialBio) { mutableStateOf(initialBio) }
    val cleanPenName = penName.trim()
    val canSave = cleanPenName.length >= 2 && !isSaving

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.poet_edit_profile_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        EditPoetProfileFields(
            penName = penName,
            onPenNameChange = { if (it.length <= 40) penName = it },
            bio = bio,
            onBioChange = { if (it.length <= 160) bio = it }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSave(cleanPenName, bio.trim()) },
                enabled = canSave,
                modifier = Modifier.testTag("save_poet_profile_button")
            ) {
                Text(stringResource(R.string.poet_save_profile_cta))
            }
        }
    }
}
