package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CommunityShayari
import com.example.data.model.PersonalShayari
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.toShayariEntity
import com.example.ui.theme.LocalShayariReadingStyle
import com.example.ui.viewmodel.PersonalCollectionState
import com.example.ui.viewmodel.PersonalShayariViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PersonalShayariScreen(
    currentUser: FirebaseUser?,
    viewModel: PersonalShayariViewModel,
    onSignOut: () -> Unit,
    onOpenAsCard: (ShayariEntity) -> Unit,
    onCopy: (PersonalShayari) -> Unit,
    onShare: (PersonalShayari) -> Unit,
    onFeedback: (String) -> Unit,
    myCommunityPosts: List<CommunityShayari> = emptyList(),
    onOpenCommunityCard: (CommunityShayari) -> Unit = {},
    onShareCommunity: (CommunityShayari) -> Unit = {},
    onDeleteCommunityPost: (String) -> Unit = {},
    onPersonalPostDeleted: ((String) -> Unit)? = null,
    onOpenWriteStudio: () -> Unit = {},
    authorPenName: String = "",
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var deleteCandidate by remember { mutableStateOf<PersonalShayari?>(null) }
    var deleteCommunityId by remember { mutableStateOf<String?>(null) }

    val isSignedIn = currentUser != null
    val effectiveAuthorName = authorPenName.trim().ifBlank {
        currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: ""
    }
    val authorDisplay = effectiveAuthorName.ifBlank { "मेरी डायरी" }
    val filteredCommunityPosts = remember(myCommunityPosts, uiState.searchQuery, uiState.selectedFilterTag) {
        val q = uiState.searchQuery.trim().lowercase()
        val tag = uiState.selectedFilterTag
        myCommunityPosts.filter { post ->
            val catTitle = ShayariCategory.fromId(post.categoryId).titleHi
            val matchesTag = tag == null || catTitle.equals(tag, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                post.couplet.lowercase().contains(q) ||
                post.promptTitle.lowercase().contains(q) ||
                catTitle.lowercase().contains(q)
            matchesTag && matchesQuery
        }
    }
    val totalAllPostsCount = uiState.allItems.size + myCommunityPosts.size
    val totalVisiblePostsCount = uiState.visibleItems.size + filteredCommunityPosts.size

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("personal_shayari_list"),
            contentPadding = PaddingValues(top = if (showHeader) 0.dp else 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showHeader) {
                // Editorial Header
                item(key = "ps_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 12.dp, top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.personal_studio_title),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$authorDisplay • ${uiState.allItems.size} सेव शायरी",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("personal_sign_out_button")
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

            // Clean Write Studio Launcher Card
            item(key = "ps_composer") {
                StudioWriteLauncherCard(
                    isSignedIn = isSignedIn,
                    onStartWriting = {
                        viewModel.cancelEditing()
                        onOpenWriteStudio()
                    },
                    onStarterSelected = { starter ->
                        viewModel.cancelEditing()
                        viewModel.onCoupletChanged(starter)
                        onOpenWriteStudio()
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Personal Collection Header & Search/Filter
            item(key = "ps_collection_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.personal_collection_heading),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "$totalVisiblePostsCount पोस्ट",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = totalAllPostsCount >= 3 || uiState.searchQuery.isNotEmpty()) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("टाइटल, टैग या शायरी खोजें…") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.clear_search)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .testTag("personal_search_input")
                        )
                    }

                    if (uiState.availableThemeTags.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item(key = "filter_all") {
                                PersonalTagFilterChip(
                                    label = stringResource(R.string.all_categories),
                                    selected = uiState.selectedFilterTag == null,
                                    onClick = { viewModel.selectFilterTag(null) },
                                    testTag = "personal_filter_all"
                                )
                            }
                            items(uiState.availableThemeTags, key = { it }) { tag ->
                                PersonalTagFilterChip(
                                    label = tag,
                                    selected = uiState.selectedFilterTag == tag,
                                    onClick = {
                                        val next = if (uiState.selectedFilterTag == tag) null else tag
                                        viewModel.selectFilterTag(next)
                                    },
                                    testTag = "personal_filter_tag_$tag"
                                )
                            }
                        }
                    }
                }
            }

            // Personal Collection Feed
            when (val state = uiState.collectionState) {
                is PersonalCollectionState.Loading -> {
                    item(key = "ps_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is PersonalCollectionState.Error -> {
                    item(key = "ps_error") {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is PersonalCollectionState.Success -> {
                    if (totalVisiblePostsCount == 0) {
                        item(key = "ps_empty") {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.HistoryEdu,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (totalAllPostsCount == 0) {
                                            stringResource(R.string.personal_empty_title)
                                        } else {
                                            stringResource(R.string.empty_search_title)
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (totalAllPostsCount == 0) {
                                            stringResource(R.string.personal_empty_subtitle)
                                        } else {
                                            stringResource(R.string.empty_search_subtitle)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.visibleItems, key = { "personal_${it.id}" }) { item ->
                            val entity = item.toShayariEntity(effectiveAuthorName)
                            PersonalShayariCard(
                                item = item,
                                authorSignature = effectiveAuthorName,
                                isSignedIn = isSignedIn,
                                onEdit = {
                                    viewModel.startEditing(item)
                                    onOpenWriteStudio()
                                },
                                onDelete = { deleteCandidate = item },
                                onCopy = { onCopy(item) },
                                onShare = { onShare(item) },
                                onOpenAsCard = { onOpenAsCard(entity) },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }

                        items(filteredCommunityPosts, key = { "community_${it.id}" }) { post ->
                            val mapped = PersonalShayari(
                                id = post.id,
                                userId = post.userId,
                                title = post.promptTitle.ifBlank { post.penName },
                                themeTag = ShayariCategory.fromId(post.categoryId).titleHi,
                                couplet = post.couplet,
                                createdAt = post.createdAt,
                                updatedAt = post.updatedAt
                            )
                            PersonalShayariCard(
                                item = mapped,
                                authorSignature = post.penName.ifBlank { effectiveAuthorName },
                                isSignedIn = isSignedIn,
                                onEdit = null,
                                onDelete = { deleteCommunityId = post.id },
                                onCopy = { onCopy(mapped) },
                                onShare = { onShareCommunity(post) },
                                onOpenAsCard = { onOpenCommunityCard(post) },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    val toDelete = deleteCandidate
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("शायरी हटाएँ?") },
            text = { Text("क्या आप पक्का यह शायरी अपने अकाउंट से हटाना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = toDelete.id
                        deleteCandidate = null
                        viewModel.deleteShayari(
                            shayariId = id,
                            onSuccess = {
                                onPersonalPostDeleted?.invoke(id)
                                onFeedback("शायरी हटा दी गई")
                            },
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

    val communityIdToDelete = deleteCommunityId
    if (communityIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteCommunityId = null },
            title = { Text("पोस्ट हटाएँ?") },
            text = { Text("क्या आप पक्का यह पोस्ट हटाना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCommunityId = null
                        onDeleteCommunityPost(communityIdToDelete)
                    }
                ) {
                    Text("हटाएँ", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCommunityId = null }) {
                    Text("कैंसल")
                }
            }
        )
    }
}

@Composable
private fun StudioWriteLauncherCard(
    isSignedIn: Boolean,
    onStartWriting: () -> Unit,
    onStarterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val cardGradient = if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceContainerLowest,
                MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onStartWriting)
            .testTag("studio_launcher_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.HistoryEdu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.studio_account_cta_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isSignedIn) {
                            stringResource(R.string.studio_account_cta_subtitle)
                        } else {
                            "बगैर लॉगिन भी लिखें और वॉलपेपर कार्ड बनाएँ"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(com.example.ui.studio.StudioPromptStarters.starters.take(3)) { starter ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onStarterSelected(starter) }
                    ) {
                        Text(
                            text = "“${starter.trim()}…”",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onStartWriting,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("studio_launcher_write_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.studio_account_cta_button),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun PersonalTagFilterChip(
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
            .defaultMinSize(minHeight = 40.dp)
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
private fun PersonalShayariCard(
    item: PersonalShayari,
    authorSignature: String,
    isSignedIn: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpenAsCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val matchedCat = ShayariCategory.entries.firstOrNull {
        it.titleHi.equals(item.themeTag.trim(), ignoreCase = true)
    }
    val accent = when {
        matchedCat != null -> if (isDarkTheme) matchedCat.darkAccent else matchedCat.lightAccent
        else -> MaterialTheme.colorScheme.primary
    }

    val formattedDate = remember(item.updatedAt, item.createdAt) {
        formatTimestamp(item.updatedAt ?: item.createdAt)
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpenAsCard)
            .testTag("personal_shayari_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.title.isNotBlank()) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item.themeTag.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = accent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "# ${item.themeTag}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (formattedDate.isNotEmpty()) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("personal_edit_button_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "एडिट करें",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("personal_delete_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "हटाएँ",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val readingStyle = LocalShayariReadingStyle.current
            Text(
                text = item.couplet,
                style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (authorSignature.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "— $authorSignature",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = readingStyle.fontStyle.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSignedIn) "मेरी पोस्ट" else "फ़ोन में सेव",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.action_copy),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("personal_share_button_${item.id}")
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

private fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    return runCatching {
        val formatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("hi-IN"))
        formatter.format(timestamp.toDate())
    }.getOrDefault("")
}
