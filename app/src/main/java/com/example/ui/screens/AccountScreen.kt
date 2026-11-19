package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.onGoogleSignInClicked
import com.example.data.model.CommunityShayari
import com.example.data.model.LocalShayarAccount
import com.example.data.model.PersonalShayari
import com.example.data.model.ShayariEntity
import com.example.data.model.SocialLinkPlatform
import com.example.data.model.SocialLinks
import com.example.data.model.openSocialProfileUrl
import com.example.ui.components.AccountAvatar
import com.example.ui.components.SocialLinksChipsRow
import com.example.ui.viewmodel.CommunityFeedState
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.PersonalShayariViewModel
import com.google.firebase.auth.FirebaseUser

@Composable
fun AccountScreen(
    currentUser: FirebaseUser?,
    personalViewModel: PersonalShayariViewModel,
    communityViewModel: CommunityViewModel?,
    onSignOut: () -> Unit,
    onOpenPersonalCard: (ShayariEntity) -> Unit,
    onCopyPersonal: (PersonalShayari) -> Unit,
    onSharePersonal: (PersonalShayari) -> Unit,
    onOpenCommunityCard: (CommunityShayari) -> Unit = {},
    onShareCommunity: (CommunityShayari) -> Unit = {},
    onFeedback: (String) -> Unit,
    onShareApp: () -> Unit = {},
    onRateApp: (() -> Unit)? = null,
    onAuthSuccess: () -> Unit = {},
    onAuthError: (String) -> Unit = onFeedback,
    onOpenWriteStudio: () -> Unit = {},
    onOpenKaviManch: () -> Unit = {},
    onOpenMyPoetProfile: () -> Unit = {},
    onAuthorNameChanged: (String) -> Unit = {},
    authorPenName: String = "",
    localAccount: LocalShayarAccount = LocalShayarAccount(),
    onOpenAuthSheet: (() -> Unit)? = null,
    onUpdateLocalProfile: ((penName: String, bio: String, socialLinks: SocialLinks) -> Unit)? = null,
    signInClickOverride: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val personalUiState by personalViewModel.uiState.collectAsStateWithLifecycle()
    val communityUiState = communityViewModel?.uiState?.collectAsStateWithLifecycle()?.value

    val allCommunityPosts = (communityUiState?.feedState as? CommunityFeedState.Success)?.posts.orEmpty()
    val isFirebaseSignedIn = currentUser != null
    val isLocalSignedIn = !isFirebaseSignedIn && localAccount.isLoggedIn
    val isSignedIn = isFirebaseSignedIn || isLocalSignedIn

    val myCommunityPosts = remember(allCommunityPosts, currentUser?.uid) {
        val uid = currentUser?.uid ?: return@remember emptyList()
        allCommunityPosts.filter { it.userId == uid }
    }
    val totalPostCount = personalUiState.allItems.size + myCommunityPosts.size

    val myPoetProfile = remember(communityUiState?.poetProfiles, currentUser?.uid) {
        val uid = currentUser?.uid ?: return@remember null
        communityUiState?.profileFor(uid)
    }
    val followerCount = myPoetProfile?.followerCount ?: 0L
    val followingCount = communityUiState?.followingIds?.size?.toLong()
        ?: myPoetProfile?.followingCount
        ?: 0L
    val currentBio = if (isFirebaseSignedIn) {
        communityUiState?.userProfile?.bio?.takeIf { it.isNotBlank() }
            ?: myPoetProfile?.bio?.takeIf { it.isNotBlank() }
            ?: localAccount.bio
    } else if (isLocalSignedIn) {
        localAccount.bio
    } else {
        ""
    }
    val activeSocialLinks = remember(myPoetProfile, localAccount.socialLinks, isFirebaseSignedIn) {
        val remoteLinks = myPoetProfile?.toSocialLinks()
        if (isFirebaseSignedIn && remoteLinks != null && remoteLinks.hasAny()) {
            remoteLinks
        } else {
            localAccount.socialLinks
        }
    }

    val displayName = when {
        isFirebaseSignedIn -> {
            communityUiState?.userProfile?.penName?.takeIf { it.isNotBlank() }
                ?: authorPenName.trim().takeIf { it.isNotBlank() }
                ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: "शायर दोस्त"
        }
        isLocalSignedIn -> {
            localAccount.fullName.trim().takeIf { it.isNotBlank() }
                ?: authorPenName.trim().takeIf { it.isNotBlank() }
                ?: "शायर दोस्त"
        }
        else -> {
            authorPenName.trim().ifBlank { stringResource(R.string.guest_writer_title) }
        }
    }

    val accountSubtitle = when {
        isFirebaseSignedIn -> currentUser?.email ?: stringResource(R.string.guest_writer_subtitle)
        isLocalSignedIn -> {
            listOfNotNull(
                localAccount.handleDisplay.takeIf { it.isNotBlank() },
                localAccount.contact.takeIf { it.isNotBlank() }
            ).joinToString(" • ").ifBlank { "रजिस्टर्ड शायर अकाउंट" }
        }
        else -> stringResource(R.string.guest_writer_subtitle)
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    fun triggerGoogleSignIn() {
        val override = signInClickOverride
        if (override != null) {
            override()
            return
        }
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onAuthError = onAuthError,
            scope = scope
        )
    }

    val canEditProfile = (isFirebaseSignedIn && communityViewModel != null) || isLocalSignedIn

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AccountProfileHeader(
            photoUrl = currentUser?.photoUrl,
            displayName = displayName,
            email = accountSubtitle,
            postCount = totalPostCount,
            onSignOut = onSignOut,
            isSignedIn = isSignedIn,
            onSignInClick = {
                if (onOpenAuthSheet != null) {
                    onOpenAuthSheet()
                } else {
                    triggerGoogleSignIn()
                }
            },
            onShareApp = onShareApp,
            onRateApp = onRateApp,
            bio = currentBio,
            socialLinks = activeSocialLinks,
            onOpenSocialLink = { platform, rawValue ->
                val opened = openSocialProfileUrl(context, platform, rawValue)
                if (!opened) {
                    onFeedback(context.getString(R.string.social_link_open_error))
                }
            },
            followerCount = followerCount,
            followingCount = followingCount,
            showSocialStats = isSignedIn,
            onEditProfile = if (canEditProfile) {
                { showEditProfileDialog = true }
            } else null,
            onOpenMyPoetProfile = if (isSignedIn) onOpenMyPoetProfile else null,
            onOpenKaviManch = onOpenKaviManch,
            modifier = Modifier.widthIn(max = 760.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PersonalShayariScreen(
                currentUser = currentUser,
                viewModel = personalViewModel,
                onSignOut = onSignOut,
                onOpenAsCard = onOpenPersonalCard,
                onCopy = onCopyPersonal,
                onShare = onSharePersonal,
                onFeedback = onFeedback,
                myCommunityPosts = myCommunityPosts,
                onOpenCommunityCard = onOpenCommunityCard,
                onShareCommunity = onShareCommunity,
                onDeleteCommunityPost = { postId ->
                    communityViewModel?.deleteShayari(
                        shayariId = postId,
                        onSuccess = { onFeedback("शायरी हटा दी गई") },
                        onError = onFeedback
                    )
                },
                onOpenWriteStudio = onOpenWriteStudio,
                authorPenName = displayName,
                showHeader = false
            )
        }
    }

    if (showEditProfileDialog && canEditProfile) {
        EditPoetProfileDialog(
            initialPenName = displayName,
            initialBio = currentBio,
            initialSocialLinks = activeSocialLinks,
            isSaving = communityUiState?.isSubmitting == true,
            onDismiss = { showEditProfileDialog = false },
            onSaveWithSocials = { newPenName, newBio, newSocialLinks ->
                if (isFirebaseSignedIn && communityViewModel != null) {
                    communityViewModel.updatePoetProfile(
                        penName = newPenName,
                        bio = newBio,
                        socialLinks = newSocialLinks,
                        onSuccess = {
                            onUpdateLocalProfile?.invoke(newPenName, newBio, newSocialLinks)
                            onAuthorNameChanged(newPenName)
                            showEditProfileDialog = false
                            onFeedback(context.getString(R.string.poet_profile_saved_snackbar))
                        },
                        onError = onFeedback
                    )
                } else {
                    onUpdateLocalProfile?.invoke(newPenName, newBio, newSocialLinks)
                    onAuthorNameChanged(newPenName)
                    showEditProfileDialog = false
                    onFeedback(context.getString(R.string.poet_profile_saved_snackbar))
                }
            }
        )
    }
}

@Composable
fun AccountProfileHeader(
    photoUrl: Uri?,
    displayName: String?,
    email: String?,
    postCount: Int,
    onSignOut: () -> Unit,
    isSignedIn: Boolean = true,
    onSignInClick: () -> Unit = {},
    onShareApp: () -> Unit = {},
    onRateApp: (() -> Unit)? = null,
    bio: String = "",
    socialLinks: SocialLinks = SocialLinks(),
    onOpenSocialLink: (SocialLinkPlatform, String) -> Unit = { _, _ -> },
    followerCount: Long = 0L,
    followingCount: Long = 0L,
    showSocialStats: Boolean = false,
    onEditProfile: (() -> Unit)? = null,
    onOpenMyPoetProfile: (() -> Unit)? = null,
    onOpenKaviManch: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val resolvedName = displayName?.takeIf { it.isNotBlank() }
        ?: if (isSignedIn) "शायर दोस्त" else stringResource(R.string.guest_writer_title)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountAvatar(
                photoUrl = photoUrl,
                displayName = resolvedName,
                isSignedIn = isSignedIn,
                isSelected = true,
                size = 52.dp,
                contentDescription = resolvedName,
                modifier = Modifier.testTag("account_profile_avatar")
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolvedName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("account_display_name")
                )
                if (!email.isNullOrBlank()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("account_poet_bio")
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                        modifier = Modifier.testTag("account_stat_posts")
                    ) {
                        Text(
                            text = "मेरी पोस्ट: $postCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (showSocialStats || followerCount > 0L || followingCount > 0L) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)),
                            modifier = Modifier.testTag("account_stat_followers")
                        ) {
                            Text(
                                text = "फ़ॉलोअर्स: $followerCount",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)),
                            modifier = Modifier.testTag("account_stat_following")
                        ) {
                            Text(
                                text = "फ़ॉलोइंग: $followingCount",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (isSignedIn) {
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = stringResource(R.string.sign_out),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onSignInClick,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("account_guest_sign_in_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Login,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.guest_sign_in_cta),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
            }
        }

        if (socialLinks.hasAny() || (isSignedIn && onEditProfile != null)) {
            Spacer(modifier = Modifier.height(10.dp))
            SocialLinksChipsRow(
                socialLinks = socialLinks,
                onOpenLink = onOpenSocialLink,
                onDarkSurface = false,
                onAddLinksClick = onEditProfile
            )
        }

        if (isSignedIn && (onEditProfile != null || onOpenMyPoetProfile != null)) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onEditProfile != null) {
                    OutlinedButton(
                        onClick = onEditProfile,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("account_edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.poet_edit_profile_cta),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }
                if (onOpenMyPoetProfile != null) {
                    FilledTonalButton(
                        onClick = onOpenMyPoetProfile,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("account_my_poet_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.account_my_public_profile),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (onOpenKaviManch != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenKaviManch)
                    .testTag("account_open_kavi_manch_card")
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.account_open_kavi_manch_title),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.account_open_kavi_manch_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.26f)),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onShareApp)
                    .testTag("account_share_app_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_share_app),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onRateApp != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onRateApp)
                        .testTag("account_rate_app_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.StarRate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_rate_app),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
