package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.ThumbUpOffAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import com.example.ai.ShayariAiUiState
import com.example.ai.ShayariAssistMode
import com.example.ai.ShayariDraftEditorHelper
import com.example.auth.rememberGoogleSignInLauncher
import android.net.Uri
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.CommunityShayari
import com.example.data.model.PoetProfile
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariTagMatcher
import com.example.data.model.WritingPrompt
import com.example.ui.components.AccountAvatar
import com.example.ui.components.CategoryChipStrip
import com.example.ui.components.ShayariAiAssistPanel
import com.example.ui.components.shayariTextFadeIn
import com.example.ui.theme.LocalShayariReadingStyle
import com.example.ui.viewmodel.CommunityFeedState
import com.example.ui.viewmodel.CommunityPostEvent
import com.example.ui.viewmodel.CommunityUiState
import com.example.ui.viewmodel.CommunityViewModel
import com.google.firebase.auth.FirebaseUser

@Composable
fun KaviManchAuthGate(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.kavi_manch_title),
    subtitle: String = stringResource(R.string.kavi_manch_auth_subtitle),
    highlights: List<String> = listOf(
        "✦  रोज़ नए टॉपिक और शुरुआत करने के लिए लाइनें",
        "✦  अपने नाम के साथ अपनी शायरी पोस्ट करें",
        "✦  दूसरों की शायरी पर 'वाह!' दें और शेयर करें"
    ),
    onSignInClickOverride: (() -> Unit)? = null
) {
    val googleSignIn = rememberGoogleSignInLauncher(
        onAuthSuccess = onAuthSuccess,
        onAuthError = onAuthError,
        onClickOverride = onSignInClickOverride
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    highlights.forEach { line ->
                        AuthHighlightRow(line)
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                if (googleSignIn.isAvailable || onSignInClickOverride != null) {
                    Button(
                        onClick = { googleSignIn.launch() },
                        enabled = !googleSignIn.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (googleSignIn.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.sign_in_with_google),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kavi_manch_cloud_unavailable_notice")
                    ) {
                        Text(
                            text = "इस APK बिल्ड में क्लाउड Firebase कनेक्ट नहीं है — आप ऑफलाइन शायरी, रील्स, कैटेगरी और कार्ड मेकर पूरी तरह इस्तेमाल कर सकते हैं।",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthHighlightRow(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun KaviManchScreen(
    currentUser: FirebaseUser,
    viewModel: CommunityViewModel,
    onSignOut: () -> Unit,
    onOpenAsCard: (CommunityShayari) -> Unit,
    onComment: (CommunityShayari) -> Unit,
    onShare: (CommunityShayari) -> Unit,
    onFeedback: (String) -> Unit,
    onOpenPoetProfile: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    commentCounts: Map<String, Int> = emptyMap(),
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnFeedback by rememberUpdatedState(onFeedback)
    var showComposer by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<CommunityShayari?>(null) }
    var prefillPromptTitle by remember { mutableStateOf("") }
    var prefillCategoryId by remember { mutableStateOf(ShayariCategory.MOHABBAT.id) }
    var deleteCandidate by remember { mutableStateOf<CommunityShayari?>(null) }
    var activeSubmissionId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.postEvents.collect { event ->
                when (event) {
                    is CommunityPostEvent.PublishSucceeded -> {
                        if (activeSubmissionId == event.submissionId) {
                            activeSubmissionId = null
                            viewModel.dismissAiSuggestions()
                            showComposer = false
                            editingPost = null
                            latestOnFeedback(
                                if (event.wasUpdate) {
                                    "शायरी अपडेट हो गई"
                                } else {
                                    "आपकी शायरी पोस्ट हो गई!"
                                }
                            )
                        }
                    }

                    is CommunityPostEvent.PublishFailed -> {
                        if (activeSubmissionId == event.submissionId) {
                            activeSubmissionId = null
                            latestOnFeedback(event.message)
                        }
                    }
                }
            }
        }
    }

    val defaultPenName = uiState.userProfile?.penName?.takeIf { it.isNotBlank() }
        ?: currentUser.displayName?.takeIf { it.isNotBlank() }
        ?: "शायर दोस्त"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(top = if (showHeader) 0.dp else 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (showHeader) {
                // Header
                item(key = "km_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = if (onBack != null) 8.dp else 20.dp, end = 12.dp, top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("kavi_manch_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenPoetProfile(currentUser.uid) }
                        ) {
                            Text(
                                text = stringResource(R.string.kavi_manch_title),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "नाम: $defaultPenName  •  प्रोफ़ाइल देखें",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Active Poets Directory (कवि प्रोफ़ाइल)
            if (uiState.poetProfiles.isNotEmpty()) {
                item(key = "km_poet_directory") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.poet_directory_heading),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.testTag("poet_directory_row")
                        ) {
                            items(uiState.poetProfiles, key = { it.userId }) { poet ->
                                val isSelf = poet.userId == currentUser.uid
                                val isFollowing = uiState.followingIds.contains(poet.userId)
                                PoetDirectoryCard(
                                    poet = poet,
                                    isSelf = isSelf,
                                    isFollowing = isFollowing,
                                    ownPhotoUrl = if (isSelf) currentUser.photoUrl else null,
                                    onOpenProfile = { onOpenPoetProfile(poet.userId) },
                                    onToggleFollow = {
                                        viewModel.toggleFollow(
                                            targetUserId = poet.userId,
                                            targetPenName = poet.penName,
                                            actorName = defaultPenName,
                                            onSuccess = { nowFollowing ->
                                                onFeedback(
                                                    if (nowFollowing) {
                                                        "अब आप ${poet.penName} को फ़ॉलो कर रहे हैं ✨"
                                                    } else {
                                                        "${poet.penName} को अनफ़ॉलो किया गया"
                                                    }
                                                )
                                            },
                                            onError = onFeedback
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Daily Writing Prompt Challenge Card
            item(key = "km_prompt") {
                DailyPromptCard(
                    prompt = uiState.activePrompt,
                    onCyclePrompt = viewModel::cyclePrompt,
                    onWriteForPrompt = {
                        editingPost = null
                        prefillPromptTitle = uiState.activePrompt.title
                        prefillCategoryId = uiState.activePrompt.suggestedCategoryId
                        showComposer = true
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Sort & Mine Filter Pills
            item(key = "km_controls") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "sort_latest") {
                        ModeChip(
                            label = "नई शायरी",
                            selected = !uiState.sortByTopWah && !uiState.showMyPostsOnly && !uiState.showFollowingOnly,
                            onClick = {
                                viewModel.setShowMyPostsOnly(false)
                                viewModel.setShowFollowingOnly(false)
                                viewModel.setSortByTopWah(false)
                            },
                            testTag = "community_filter_latest"
                        )
                    }
                    item(key = "sort_top") {
                        ModeChip(
                            label = "सबसे ज़्यादा वाह!",
                            selected = uiState.sortByTopWah && !uiState.showMyPostsOnly && !uiState.showFollowingOnly,
                            onClick = {
                                viewModel.setShowMyPostsOnly(false)
                                viewModel.setShowFollowingOnly(false)
                                viewModel.setSortByTopWah(true)
                            },
                            testTag = "community_filter_top"
                        )
                    }
                    item(key = "filter_following") {
                        ModeChip(
                            label = "फ़ॉलो किए गए",
                            selected = uiState.showFollowingOnly,
                            onClick = { viewModel.setShowFollowingOnly(!uiState.showFollowingOnly) },
                            testTag = "community_filter_following"
                        )
                    }
                    item(key = "filter_mine") {
                        ModeChip(
                            label = "मेरी शायरी",
                            selected = uiState.showMyPostsOnly,
                            onClick = { viewModel.setShowMyPostsOnly(!uiState.showMyPostsOnly) },
                            testTag = "community_filter_mine"
                        )
                    }
                }
            }

            // Topic Chips
            item(key = "km_topics") {
                CategoryChipStrip(
                    selectedCategoryId = uiState.selectedCategoryId,
                    onSelectCategory = viewModel::selectCategory
                )
            }

            // Feed states
            when (val feed = uiState.feedState) {
                is CommunityFeedState.Loading -> {
                    item(key = "km_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is CommunityFeedState.Error -> {
                    item(key = "km_error") {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text = feed.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is CommunityFeedState.Success -> {
                    if (uiState.visiblePosts.isEmpty()) {
                        item(key = "km_empty") {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Create,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "पहली शायरी आप लिखें!",
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "शुरुआत करें — नीचे 'लिखें' बटन दबाकर अपनी शायरी शेयर करें।",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            editingPost = null
                                            prefillPromptTitle = uiState.activePrompt.title
                                            prefillCategoryId = uiState.selectedCategoryId
                                                ?: uiState.activePrompt.suggestedCategoryId
                                            showComposer = true
                                        }
                                    ) {
                                        Text("शायरी लिखें")
                                    }
                                }
                            }
                        }
                    } else {
                        items(uiState.visiblePosts, key = { it.id }) { post ->
                            val isOwner = post.userId == currentUser.uid
                            val isWahed = uiState.userWahIds.contains(post.id)
                            val isFollowingAuthor = uiState.followingIds.contains(post.userId)

                            CommunityPostCard(
                                post = post,
                                isOwner = isOwner,
                                ownerPhotoUrl = if (isOwner) currentUser.photoUrl else null,
                                isWahed = isWahed,
                                isFollowingAuthor = isFollowingAuthor,
                                commentCount = commentCounts["community_${post.id}"] ?: 0,
                                onAuthorClick = { onOpenPoetProfile(post.userId) },
                                onToggleFollow = {
                                    viewModel.toggleFollow(
                                        targetUserId = post.userId,
                                        targetPenName = post.penName,
                                        actorName = defaultPenName,
                                        onSuccess = { nowFollowing ->
                                            onFeedback(
                                                if (nowFollowing) {
                                                    "अब आप ${post.penName} को फ़ॉलो कर रहे हैं ✨"
                                                } else {
                                                    "${post.penName} को अनफ़ॉलो किया गया"
                                                }
                                            )
                                        },
                                        onError = onFeedback
                                    )
                                },
                                onToggleWah = {
                                    viewModel.toggleWah(
                                        post = post,
                                        actorName = defaultPenName,
                                        onError = onFeedback
                                    )
                                },
                                onComment = { onComment(post) },
                                onShare = { onShare(post) },
                                onOpenAsCard = { onOpenAsCard(post) },
                                onEdit = {
                                    editingPost = post
                                    prefillPromptTitle = post.promptTitle
                                    prefillCategoryId = post.categoryId
                                    showComposer = true
                                },
                                onDelete = { deleteCandidate = post },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Write FAB
        ExtendedFloatingActionButton(
            onClick = {
                editingPost = null
                prefillPromptTitle = uiState.activePrompt.title
                prefillCategoryId = uiState.selectedCategoryId ?: uiState.activePrompt.suggestedCategoryId
                showComposer = true
            },
            icon = {
                Icon(imageVector = Icons.Rounded.Create, contentDescription = null)
            },
            text = {
                Text(
                    text = stringResource(R.string.action_write_shayari),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("write_shayari_fab")
        )
    }

    if (showComposer) {
        ComposeShayariSheet(
            initialPenName = editingPost?.penName ?: defaultPenName,
            initialCouplet = editingPost?.couplet ?: "",
            initialCategoryId = editingPost?.categoryId ?: prefillCategoryId,
            activePrompt = uiState.activePrompt,
            initialPromptTitle = editingPost?.promptTitle ?: prefillPromptTitle,
            isEditing = editingPost != null,
            isSubmitting = uiState.isSubmitting,
            aiUiState = uiState.aiUiState,
            onRequestAiAssist = { mode, draft, catId, promptTitle ->
                viewModel.requestAiAssist(
                    mode = mode,
                    draft = draft,
                    categoryTitle = ShayariCategory.fromId(catId).titleHi,
                    promptTitle = promptTitle
                )
            },
            onDismissAi = viewModel::dismissAiSuggestions,
            onDismiss = {
                viewModel.dismissAiSuggestions()
                showComposer = false
            },
            onSubmit = { penName, couplet, categoryId, promptTitle ->
                if (activeSubmissionId == null && !uiState.isSubmitting) {
                    val submissionId = viewModel.nextPostSubmissionId()
                    activeSubmissionId = submissionId
                    val accepted = viewModel.submitCommunityShayari(
                        editingId = editingPost?.id,
                        penName = penName,
                        couplet = couplet,
                        categoryId = categoryId,
                        promptTitle = promptTitle,
                        submissionId = submissionId
                    )
                    if (!accepted) {
                        activeSubmissionId = null
                    }
                }
            }
        )
    }

    val toDelete = deleteCandidate
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("शायरी हटाएँ?") },
            text = { Text("क्या आप पक्का अपनी यह शायरी हटाना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = toDelete.id
                        deleteCandidate = null
                        viewModel.deleteShayari(
                            shayariId = id,
                            onSuccess = { onFeedback("शायरी हटा दी गई") },
                            onError = onFeedback
                        )
                    }
                ) {
                    Text("हटाएँ", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("कैंसल")
                }
            }
        )
    }
}

@Composable
private fun DailyPromptCard(
    prompt: WritingPrompt,
    onCyclePrompt: () -> Unit,
    onWriteForPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
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
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFE6BE78).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFE6BE78).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFF3D59B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "आज का टॉपिक",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFFF3D59B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onCyclePrompt,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("cycle_prompt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "टॉपिक बदलें",
                            tint = Color(0xFFF3D59B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "“${prompt.title}”",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFFFF8F0)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = prompt.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE9DAD0)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onWriteForPrompt,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE6BE78),
                        contentColor = Color(0xFF281709)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("prompt_write_cta")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Create,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "इस टॉपिक पर लिखें",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .defaultMinSize(minHeight = 42.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun PoetDirectoryCard(
    poet: PoetProfile,
    isSelf: Boolean,
    isFollowing: Boolean,
    ownPhotoUrl: Uri?,
    onOpenProfile: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (isFollowing || isSelf) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenProfile)
            .testTag("poet_directory_card_${poet.userId}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelf && ownPhotoUrl != null) {
                AccountAvatar(
                    photoUrl = ownPhotoUrl,
                    displayName = poet.penName,
                    isSignedIn = true,
                    isSelected = true,
                    size = 44.dp
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = poet.penName.trim().firstOrNull()?.toString() ?: "श",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Text(
                text = poet.penName.ifBlank { "शायर दोस्त" },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = "${poet.followerCount} फ़ॉलोअर • ${poet.postCount} पोस्ट",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = when {
                    isSelf -> MaterialTheme.colorScheme.secondaryContainer
                    isFollowing -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 32.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = if (isSelf) onOpenProfile else onToggleFollow)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = when {
                            isSelf -> "मेरी प्रोफ़ाइल"
                            isFollowing -> "फ़ॉलोइंग ✓"
                            else -> "+ फ़ॉलो करें"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = when {
                            isSelf -> MaterialTheme.colorScheme.onSecondaryContainer
                            isFollowing -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onPrimary
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityShayari,
    isOwner: Boolean,
    ownerPhotoUrl: Uri?,
    isWahed: Boolean,
    isFollowingAuthor: Boolean,
    commentCount: Int,
    onAuthorClick: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleWah: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onOpenAsCard: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = ShayariCategory.fromId(post.categoryId)
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val accent = if (isDarkTheme) category.darkAccent else category.lightAccent

    val wahScale by animateFloatAsState(
        targetValue = if (isWahed) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "wahBounce"
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpenAsCard)
            .testTag("community_shayari_card_${post.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Author Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAuthorClick)
                        .padding(vertical = 2.dp)
                        .testTag("community_post_author_${post.id}")
                ) {
                    if (isOwner && ownerPhotoUrl != null) {
                        AccountAvatar(
                            photoUrl = ownerPhotoUrl,
                            displayName = post.penName,
                            isSignedIn = true,
                            isSelected = true,
                            size = 38.dp
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.18f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = post.penName.trim().firstOrNull()?.toString() ?: "श",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = accent
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = post.penName,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = category.titleHi,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                            if (post.promptTitle.isNotBlank()) {
                                Text(
                                    text = "  •  टॉपिक: ${post.promptTitle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (isOwner) {
                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "एडिट करें",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "हटाएँ",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isFollowingAuthor) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isFollowingAuthor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onToggleFollow)
                            .testTag("community_follow_button_${post.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isFollowingAuthor) Icons.Rounded.Check else Icons.Rounded.PersonAdd,
                                contentDescription = null,
                                tint = if (isFollowingAuthor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isFollowingAuthor) "फ़ॉलोइंग" else "फ़ॉलो",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isFollowingAuthor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val readingStyle = LocalShayariReadingStyle.current
            Text(
                text = post.couplet,
                style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.shayariTextFadeIn(
                    revealKey = post.id,
                    durationMillis = 400,
                    slideUpDp = 8.dp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

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
                        .defaultMinSize(minHeight = 42.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onToggleWah)
                        .testTag("wah_button_${post.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isWahed) Icons.Rounded.ThumbUp else Icons.Rounded.ThumbUpOffAlt,
                            contentDescription = "वाह!",
                            tint = if (isWahed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .scale(wahScale)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "वाह! • ${post.wahCount}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isWahed) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isWahed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onComment)
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .testTag("community_comment_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = stringResource(R.string.action_comment),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        if (commentCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
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
                            .testTag("community_share_button_${post.id}")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeShayariSheet(
    initialPenName: String,
    initialCouplet: String,
    initialCategoryId: String,
    activePrompt: WritingPrompt,
    initialPromptTitle: String,
    isEditing: Boolean,
    isSubmitting: Boolean,
    aiUiState: ShayariAiUiState,
    onRequestAiAssist: (mode: ShayariAssistMode, draft: String, categoryId: String, promptTitle: String) -> Unit,
    onDismissAi: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (penName: String, couplet: String, categoryId: String, promptTitle: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var penName by remember { mutableStateOf(initialPenName) }
    var couplet by remember { mutableStateOf(initialCouplet) }
    var selectedCategoryId by remember { mutableStateOf(initialCategoryId) }
    var attachToPrompt by remember { mutableStateOf(initialPromptTitle.isNotBlank()) }

    val trimmedCouplet = couplet.trim()
    val trimmedPenName = penName.trim()
    val canSubmit = trimmedPenName.length >= 2 && trimmedCouplet.length in 10..500 && !isSubmitting

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isEditing) "शायरी एडिट करें" else "अपनी शायरी लिखें",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = penName,
                onValueChange = { if (it.length <= 40) penName = it },
                label = { Text("आपका नाम") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("composer_pen_name_input")
            )

            // Inspiration Starters (only when creating or empty)
            if (!isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "शुरुआत के लिए कोई लाइन चुनें (टैप करें):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activePrompt.starters) { starter ->
                            FilledTonalButton(
                                onClick = {
                                    couplet = starter
                                    selectedCategoryId = activePrompt.suggestedCategoryId
                                    attachToPrompt = true
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = starter.trim(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = couplet,
                onValueChange = { if (it.length <= 500) couplet = it },
                label = { Text("आपकी शायरी (कम से कम 10 लेटर)") },
                placeholder = { Text("यहाँ अपनी 2 या 4 लाइनें लिखें…") },
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(16.dp),
                supportingText = {
                    Text("${trimmedCouplet.length} / 500 लेटर")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("composer_couplet_input")
            )

            ShayariAiAssistPanel(
                aiState = aiUiState,
                onRequestRhymes = {
                    val promptLabel = if (attachToPrompt) activePrompt.title else ""
                    onRequestAiAssist(
                        ShayariAssistMode.RHYMES,
                        couplet,
                        selectedCategoryId,
                        promptLabel
                    )
                },
                onRequestCompletions = {
                    val promptLabel = if (attachToPrompt) activePrompt.title else ""
                    onRequestAiAssist(
                        ShayariAssistMode.COMPLETE_LINES,
                        couplet,
                        selectedCategoryId,
                        promptLabel
                    )
                },
                onInsertWord = { word ->
                    couplet = ShayariDraftEditorHelper.appendRhymingWord(
                        currentDraft = couplet,
                        word = word,
                        maxLength = 500
                    )
                },
                onAppendLine = { line ->
                    couplet = ShayariDraftEditorHelper.appendLine(
                        currentDraft = couplet,
                        line = line,
                        maxLength = 500
                    )
                },
                onReplaceWithLine = { line ->
                    couplet = line.trim().take(500)
                },
                onDismiss = onDismissAi
            )

            // Category picker + automatic related bilingual tags
            var selectedAutoTags by remember(selectedCategoryId) {
                mutableStateOf(ShayariTagMatcher.autoTagsForCategory(selectedCategoryId))
            }
            val suggestedCategoryTags = remember(selectedCategoryId) {
                ShayariTagMatcher.suggestedTagsForCategory(selectedCategoryId).take(12)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "टॉपिक / कैटेगरी चुनें",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ShayariCategory.entries, key = { it.id }) { cat ->
                        val selected = cat.id == selectedCategoryId
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { selectedCategoryId = cat.id }
                        ) {
                            Text(
                                text = "${cat.emoji} ${cat.titleHi}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                val selectedCatObj = ShayariCategory.fromId(selectedCategoryId)
                Text(
                    text = "${selectedCatObj.emoji} ${selectedCatObj.titleHi} से जुड़े टैग्स (ऑटो-सुझाव)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("composer_related_tags_row")
                ) {
                    items(suggestedCategoryTags, key = { it }) { tag ->
                        val isTagActive = selectedAutoTags.any { it.equals(tag, ignoreCase = true) }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isTagActive) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isTagActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    selectedAutoTags = if (isTagActive) {
                                        selectedAutoTags.filterNot { it.equals(tag, ignoreCase = true) }
                                    } else {
                                        selectedAutoTags + tag
                                    }
                                }
                                .testTag("composer_tag_$tag")
                        ) {
                            Text(
                                text = if (isTagActive) "✓ #$tag" else "+ #$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isTagActive) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Challenge toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "आज के टॉपिक से जोड़ें",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "“${activePrompt.title}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = attachToPrompt,
                    onCheckedChange = { attachToPrompt = it }
                )
            }

            Button(
                onClick = {
                    val tagsJoined = selectedAutoTags.joinToString(" ") { "#$it" }
                    val promptLabel = when {
                        attachToPrompt && tagsJoined.isNotBlank() ->
                            "${activePrompt.title} • $tagsJoined".take(100)
                        attachToPrompt -> activePrompt.title.take(100)
                        tagsJoined.isNotBlank() -> tagsJoined.take(100)
                        else -> ""
                    }
                    onSubmit(trimmedPenName, trimmedCouplet, selectedCategoryId, promptLabel)
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("composer_publish_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isEditing) "बदलाव सेव करें" else "सबके सामने पोस्ट करें",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

