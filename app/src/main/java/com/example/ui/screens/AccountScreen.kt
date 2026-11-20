package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.BuildConfig
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.rememberGoogleSignInLauncher
import com.example.data.model.CommunityShayari
import com.example.data.model.LocalShayarAccount
import com.example.data.model.PersonalShayari
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariTagMatcher
import com.example.data.model.SocialLinkPlatform
import com.example.data.model.SocialLinks
import com.example.data.model.openSocialProfileUrl
import com.example.ui.components.AccountAvatar
import com.example.ui.components.ProfileManagementSheet
import com.example.ui.components.SocialLinksChipsRow
import com.example.ui.theme.RoseVioletGradientEnd
import com.example.ui.theme.RoseVioletGradientStart
import com.example.ui.viewmodel.CommunityFeedState
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.PersonalShayariViewModel
import com.example.util.ProfilePhotoStorage
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
    profilePhotoUri: Uri? = null,
    onOpenAuthSheet: (() -> Unit)? = null,
    onUpdateLocalProfile: ((penName: String, bio: String, socialLinks: SocialLinks) -> Unit)? = null,
    onOpenEasyProfile: (() -> Unit)? = null,
    onSaveEasyProfile: ((penName: String, bio: String, storedPhotoUri: String, socialLinks: SocialLinks) -> Unit)? = null,
    onQuickCreateNameAccount: ((String) -> Unit)? = null,
    onPersonalPostDeleted: ((String) -> Unit)? = null,
    isOwner: Boolean = false,
    totalCatalogShayariCount: Int = 0,
    categoryCount: Int = 0,
    onOpenOwnerCategoryStats: (() -> Unit)? = null,
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
    } else {
        localAccount.bio
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

    val effectivePhotoUri = remember(profilePhotoUri, localAccount.profilePhotoUri, currentUser?.photoUrl) {
        profilePhotoUri ?: ProfilePhotoStorage.resolveDisplayUri(
            storedPhotoUri = localAccount.profilePhotoUri,
            fallbackRemoteUri = currentUser?.photoUrl
        )
    }

    var showInlineEasyProfile by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val googleSignIn = rememberGoogleSignInLauncher(
        onAuthSuccess = onAuthSuccess,
        onAuthError = onAuthError,
        onClickOverride = signInClickOverride
    )

    val triggerOpenEasyProfile: () -> Unit = {
        if (onOpenEasyProfile != null) {
            onOpenEasyProfile()
        } else {
            showInlineEasyProfile = true
        }
    }

    if (showInlineEasyProfile) {
        EasyProfileEditorScreen(
            initialName = if (displayName == stringResource(R.string.guest_writer_title)) "" else displayName,
            initialBio = currentBio,
            initialStoredPhotoUri = localAccount.profilePhotoUri,
            fallbackRemotePhotoUri = currentUser?.photoUrl,
            initialSocialLinks = activeSocialLinks,
            isSaving = communityUiState?.isSubmitting == true,
            onBack = { showInlineEasyProfile = false },
            onFeedback = onFeedback,
            onSave = { newName, newBio, newPhotoUri, newSocialLinks ->
                if (isFirebaseSignedIn && communityViewModel != null) {
                    communityViewModel.updatePoetProfile(
                        penName = newName,
                        bio = newBio,
                        socialLinks = newSocialLinks,
                        onSuccess = {
                            if (onSaveEasyProfile != null) {
                                onSaveEasyProfile(newName, newBio, newPhotoUri, newSocialLinks)
                            } else {
                                onUpdateLocalProfile?.invoke(newName, newBio, newSocialLinks)
                                onAuthorNameChanged(newName)
                            }
                            showInlineEasyProfile = false
                            onFeedback(context.getString(R.string.profile_saved_snackbar))
                        },
                        onError = onFeedback
                    )
                } else {
                    if (onSaveEasyProfile != null) {
                        onSaveEasyProfile(newName, newBio, newPhotoUri, newSocialLinks)
                    } else {
                        onUpdateLocalProfile?.invoke(newName, newBio, newSocialLinks)
                        onAuthorNameChanged(newName)
                    }
                    showInlineEasyProfile = false
                    onFeedback(context.getString(R.string.profile_saved_snackbar))
                }
            },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AccountProfileHeader(
            photoUrl = effectivePhotoUri,
            displayName = displayName,
            email = accountSubtitle,
            postCount = totalPostCount,
            onSignOut = onSignOut,
            isSignedIn = isSignedIn,
            isOnlineGoogleAccount = isFirebaseSignedIn,
            onQuickGoogleConnect = if (!isFirebaseSignedIn) {
                { googleSignIn.launch() }
            } else {
                null
            },
            onSignInClick = {
                if (onOpenAuthSheet != null) {
                    onOpenAuthSheet()
                } else {
                    googleSignIn.launch()
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
            onEditProfile = triggerOpenEasyProfile,
            onQuickCreateNameAccount = if (!isSignedIn) onQuickCreateNameAccount else null,
            onOpenMyPoetProfile = if (isSignedIn) onOpenMyPoetProfile else null,
            onOpenKaviManch = onOpenKaviManch,
            isOwner = isOwner,
            totalCatalogShayariCount = totalCatalogShayariCount,
            categoryCount = categoryCount,
            onOpenOwnerCategoryStats = onOpenOwnerCategoryStats,
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
                onPersonalPostDeleted = onPersonalPostDeleted,
                onRequestOnlineSync = if (!isFirebaseSignedIn) {
                    { googleSignIn.launch() }
                } else {
                    null
                },
                onPublishPersonalOnline = if (isFirebaseSignedIn && communityViewModel != null) {
                    { item ->
                        val primaryCatId = ShayariTagMatcher.resolvePrimaryCategory(
                            rawTag = item.themeTag
                        ).id
                        val hashTagsStr = ShayariTagMatcher.parseTags(item.themeTag)
                            .take(5)
                            .joinToString(" ") { "#$it" }
                        val promptTitle = listOf(item.title.trim(), hashTagsStr)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                            .take(120)
                        communityViewModel.publishOrUpdateShayari(
                            editingId = null,
                            penName = displayName.ifBlank { "शायर दोस्त" },
                            couplet = item.couplet.trim(),
                            categoryId = primaryCatId,
                            promptTitle = promptTitle,
                            onSuccess = {
                                onFeedback("आपकी शायरी अब सभी ऑनलाइन यूज़र्स के लिए लाइव है 🌐✨")
                            },
                            onError = onFeedback
                        )
                    }
                } else {
                    null
                },
                onOpenWriteStudio = onOpenWriteStudio,
                authorPenName = displayName,
                showHeader = false
            )
        }
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
    isOnlineGoogleAccount: Boolean = isSignedIn,
    onQuickGoogleConnect: (() -> Unit)? = null,
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
    onQuickCreateNameAccount: ((String) -> Unit)? = null,
    onOpenMyPoetProfile: (() -> Unit)? = null,
    onOpenKaviManch: (() -> Unit)? = null,
    isOwner: Boolean = false,
    totalCatalogShayariCount: Int = 0,
    categoryCount: Int = 0,
    onOpenOwnerCategoryStats: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val resolvedName = displayName?.takeIf { it.isNotBlank() }
        ?: if (isSignedIn) "शायर दोस्त" else stringResource(R.string.guest_writer_title)

    val headerGradient = Brush.horizontalGradient(
        colors = listOf(RoseVioletGradientStart, RoseVioletGradientEnd)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .then(
                                if (onEditProfile != null) {
                                    Modifier.clickable(onClick = onEditProfile)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        AccountAvatar(
                            photoUrl = photoUrl,
                            displayName = resolvedName,
                            isSignedIn = isSignedIn,
                            isSelected = true,
                            size = 56.dp,
                            contentDescription = resolvedName,
                            modifier = Modifier.testTag("account_profile_avatar")
                        )
                        if (onEditProfile != null) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(1.5.dp, RoseVioletGradientStart),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(21.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = null,
                                        tint = RoseVioletGradientStart,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = resolvedName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("account_display_name")
                        )
                        if (!email.isNullOrBlank()) {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = bio,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.96f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("account_poet_bio")
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                modifier = Modifier.testTag("account_stat_posts")
                            ) {
                                Text(
                                    text = "मेरी पोस्ट: $postCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (showSocialStats || followerCount > 0L || followingCount > 0L) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.22f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    modifier = Modifier.testTag("account_stat_followers")
                                ) {
                                    Text(
                                        text = "फ़ॉलोअर्स: $followerCount",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.22f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    modifier = Modifier.testTag("account_stat_following")
                                ) {
                                    Text(
                                        text = "फ़ॉलोइंग: $followingCount",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier.testTag("account_version_badge")
                            ) {
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White.copy(alpha = 0.92f),
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (isSignedIn) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!isOnlineGoogleAccount && onQuickGoogleConnect != null) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = onQuickGoogleConnect)
                                        .testTag("account_go_online_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Login,
                                            contentDescription = null,
                                            tint = RoseVioletGradientStart,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "ऑनलाइन जुड़ें",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = RoseVioletGradientStart,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = stringResource(R.string.sign_out),
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onSignInClick)
                                .testTag("account_guest_sign_in_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Login,
                                    contentDescription = null,
                                    tint = RoseVioletGradientStart,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = stringResource(R.string.guest_sign_in_cta),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = RoseVioletGradientStart,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isSignedIn && onQuickCreateNameAccount != null) {
            Spacer(modifier = Modifier.height(8.dp))
            var quickName by remember { mutableStateOf("") }
            val trimmedQuickName = quickName.trim()
            val canQuickCreate = trimmedQuickName.length >= 2

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, RoseVioletGradientStart.copy(alpha = 0.35f)),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_quick_name_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickName,
                        onValueChange = { if (it.length <= 40) quickName = it },
                        placeholder = {
                            Text(
                                text = "बस अपना नाम लिखें…",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (canQuickCreate) {
                                    onQuickCreateNameAccount(trimmedQuickName)
                                    quickName = ""
                                }
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoseVioletGradientStart,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("account_quick_name_input")
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = if (canQuickCreate) {
                                    headerGradient
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            RoseVioletGradientStart.copy(alpha = 0.45f),
                                            RoseVioletGradientEnd.copy(alpha = 0.45f)
                                        )
                                    )
                                }
                            )
                            .clickable(enabled = canQuickCreate) {
                                onQuickCreateNameAccount(trimmedQuickName)
                                quickName = ""
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("account_quick_create_button")
                    ) {
                        Text(
                            text = "अकाउंट बनाएँ",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (socialLinks.hasAny() || (isSignedIn && onEditProfile != null)) {
            Spacer(modifier = Modifier.height(8.dp))
            SocialLinksChipsRow(
                socialLinks = socialLinks,
                onOpenLink = onOpenSocialLink,
                onDarkSurface = false,
                onAddLinksClick = onEditProfile
            )
        }

        if (onEditProfile != null || (isSignedIn && onOpenMyPoetProfile != null)) {
            Spacer(modifier = Modifier.height(8.dp))
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

        if (isOwner && onOpenOwnerCategoryStats != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenOwnerCategoryStats)
                    .testTag("account_owner_stats_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "👑 Owner रिपोर्ट • कुल $totalCatalogShayariCount शायरी",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$categoryCount कैटेगरी में किसमें कितनी शायरी है, यहाँ देखें",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
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
