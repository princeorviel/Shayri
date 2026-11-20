package com.example.ui.studio

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import com.example.ai.ShayariAssistMode
import com.example.data.model.ShayariCategory
import com.example.ui.components.ShayariAiAssistPanel
import com.example.ui.theme.LocalShayariReadingStyle
import com.example.ui.viewmodel.PersonalShayariEvent
import com.example.ui.viewmodel.PersonalShayariUiState
import com.example.ui.viewmodel.PersonalShayariViewModel
import com.example.util.HinglishTransliterator
import com.example.util.ShayariShareHelper
import kotlinx.coroutines.launch

@Composable
fun ShayariWriteStudioScreen(
    viewModel: PersonalShayariViewModel,
    authorName: String,
    initialStep: StudioStep = StudioStep.WRITE,
    onAuthorNameChanged: (String) -> Unit = {},
    onClose: () -> Unit,
    onSaved: (wasUpdate: Boolean) -> Unit,
    onFeedback: (String) -> Unit,
    onPostSavedToCatalog: ((personalId: String, couplet: String, authorName: String, title: String, themeTag: String, wasUpdate: Boolean) -> Unit)? = null,
    onRequestShareDesignedCard: ((couplet: String, authorName: String, title: String, tag: String, design: ShayariDesignState) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val latestOnSaved by rememberUpdatedState(onSaved)
    val latestOnFeedback by rememberUpdatedState(onFeedback)
    val latestOnClose by rememberUpdatedState(onClose)
    val latestOnPostSavedToCatalog by rememberUpdatedState(onPostSavedToCatalog)
    var localAuthorName by remember(authorName) { mutableStateOf(authorName.trim()) }
    val latestAuthorName by rememberUpdatedState(localAuthorName.trim())

    var activeSubmissionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var hasDispatchedSaveNavigation by remember { mutableStateOf(false) }

    var currentStep by remember { mutableStateOf(initialStep) }
    var activeDesignTab by remember { mutableStateOf(DesignTab.WALLPAPER) }
    val initialFontStyle = LocalShayariReadingStyle.current.fontStyle
    var designState by remember {
        mutableStateOf(
            ShayariDesignState(
                fontStyle = initialFontStyle
            )
        )
    }
    var isSharingImage by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is PersonalShayariEvent.SaveSucceeded -> {
                        if (activeSubmissionId == event.submissionId && !hasDispatchedSaveNavigation) {
                            activeSubmissionId = null
                            hasDispatchedSaveNavigation = true
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            if (event.savedId.isNotBlank() && event.couplet.isNotBlank()) {
                                latestOnPostSavedToCatalog?.invoke(
                                    event.savedId,
                                    event.couplet,
                                    latestAuthorName,
                                    event.title,
                                    event.themeTag,
                                    event.wasUpdate
                                )
                            }
                            latestOnSaved(event.wasUpdate)
                        }
                    }

                    is PersonalShayariEvent.SaveFailed -> {
                        if (activeSubmissionId == event.submissionId) {
                            activeSubmissionId = null
                            latestOnFeedback(event.message)
                        }
                    }
                }
            }
        }
    }

    val submitPost: () -> Unit = {
        if (!hasDispatchedSaveNavigation && !uiState.isSaving && activeSubmissionId == null) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            val submissionId = viewModel.nextSubmissionId()
            activeSubmissionId = submissionId
            val accepted = viewModel.submitShayari(submissionId)
            if (!accepted) {
                activeSubmissionId = null
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            designState = designState.copy(
                wallpaper = StudioWallpaper.GalleryPhoto(uri),
                scrimDimness = 0.35f,
                textColor = Color.White
            )
        }
    }

    val handleBack: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (currentStep == StudioStep.DESIGN) {
            currentStep = StudioStep.WRITE
        } else {
            latestOnClose()
        }
    }
    BackHandler(onBack = handleBack)

    val isEditing = uiState.editingItem != null
    val trimmedCouplet = uiState.coupletInput.trim()
    val canProceed = trimmedCouplet.length >= 4 && !uiState.isSaving && !hasDispatchedSaveNavigation
    val signature = localAuthorName.trim()

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Studio Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = handleBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(
                            if (currentStep == StudioStep.DESIGN) {
                                "studio_design_back_button"
                            } else {
                                "studio_close_button"
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }

                Text(
                    text = when {
                        currentStep == StudioStep.DESIGN -> stringResource(R.string.studio_design_title)
                        isEditing -> stringResource(R.string.studio_edit_title)
                        else -> stringResource(R.string.studio_write_title)
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                if (currentStep == StudioStep.WRITE) {
                    Button(
                        onClick = { currentStep = StudioStep.DESIGN },
                        enabled = canProceed,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .testTag("studio_next_design_button")
                    ) {
                        Text(
                            text = stringResource(R.string.studio_next_design),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = submitPost,
                        enabled = canProceed,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .testTag("studio_publish_button")
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkAdded,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEditing) "अपडेट करें" else "पोस्ट करें",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "studio_step_transition",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { step ->
                when (step) {
                    StudioStep.WRITE -> {
                        StudioWriteStepContent(
                            uiState = uiState,
                            signature = signature,
                            onAuthorNameChanged = { updated ->
                                val clipped = updated.take(50)
                                localAuthorName = clipped
                                onAuthorNameChanged(clipped)
                            },
                            onCoupletChanged = viewModel::onCoupletChanged,
                            onTitleChanged = viewModel::onTitleChanged,
                            onThemeTagChanged = viewModel::onThemeTagChanged,
                            onTogglePresetTag = viewModel::togglePresetThemeTag,
                            onRequestAiAssist = viewModel::requestAiAssist,
                            onInsertRhymingWord = viewModel::insertRhymingWord,
                            onApplyLineCompletion = viewModel::applyLineCompletion,
                            onDismissAi = viewModel::dismissAiSuggestions,
                            onQuickSave = submitPost
                        )
                    }

                    StudioStep.DESIGN -> {
                        StudioDesignStepContent(
                            couplet = uiState.coupletInput.trim(),
                            authorName = signature,
                            title = uiState.titleInput,
                            tag = uiState.themeTagInput,
                            design = designState,
                            activeTab = activeDesignTab,
                            canSavePost = canProceed,
                            isSaving = uiState.isSaving,
                            isSharingImage = isSharingImage,
                            onTabSelected = { activeDesignTab = it },
                            onSelectWallpaper = { wp ->
                                designState = when (wp) {
                                    is StudioWallpaper.SolidOrGradient -> designState.copy(
                                        wallpaper = wp,
                                        scrimDimness = 0f,
                                        textColor = wp.defaultTextColor,
                                        accentColor = wp.defaultAccentColor
                                    )
                                    is StudioWallpaper.DrawableArt -> designState.copy(
                                        wallpaper = wp,
                                        scrimDimness = 0.35f,
                                        textColor = Color.White,
                                        accentColor = Color(0xFFE6BE78)
                                    )
                                    is StudioWallpaper.GalleryPhoto -> designState.copy(
                                        wallpaper = wp,
                                        scrimDimness = 0.35f,
                                        textColor = Color.White
                                    )
                                }
                            },
                            onPickGalleryPhoto = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onCycleDimness = {
                                val nextDim = when {
                                    designState.scrimDimness < 0.12f -> 0.25f
                                    designState.scrimDimness < 0.35f -> 0.45f
                                    designState.scrimDimness < 0.58f -> 0.65f
                                    else -> 0.05f
                                }
                                designState = designState.copy(scrimDimness = nextDim)
                            },
                            onDesignUpdated = { designState = it },
                            onSavePost = submitPost,
                            onShareCard = {
                                if (onRequestShareDesignedCard != null) {
                                    onRequestShareDesignedCard(
                                        uiState.coupletInput.trim(),
                                        signature,
                                        uiState.titleInput,
                                        uiState.themeTagInput,
                                        designState
                                    )
                                } else if (!isSharingImage) {
                                    scope.launch {
                                        isSharingImage = true
                                        val ok = try {
                                            ShayariShareHelper.shareDesignedCard(
                                                context = context,
                                                couplet = uiState.coupletInput.trim(),
                                                authorName = signature,
                                                title = uiState.titleInput,
                                                tag = uiState.themeTagInput,
                                                design = designState
                                            )
                                        } finally {
                                            isSharingImage = false
                                        }
                                        if (!ok) {
                                            onFeedback(context.getString(R.string.share_error_snackbar))
                                        }
                                    }
                                }
                            },
                            onDownloadCard = {
                                scope.launch {
                                    val result = ShayariShareHelper.saveDesignedCardToGallery(
                                        context = context,
                                        couplet = uiState.coupletInput.trim(),
                                        authorName = signature,
                                        title = uiState.titleInput,
                                        tag = uiState.themeTagInput,
                                        design = designState
                                    )
                                    if (result.isSuccess) {
                                        onFeedback(context.getString(R.string.download_image_success))
                                    } else {
                                        onFeedback(context.getString(R.string.download_image_error))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioWriteStepContent(
    uiState: PersonalShayariUiState,
    signature: String,
    onAuthorNameChanged: (String) -> Unit,
    onCoupletChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onThemeTagChanged: (String) -> Unit,
    onTogglePresetTag: (String) -> Unit,
    onRequestAiAssist: (ShayariAssistMode) -> Unit,
    onInsertRhymingWord: (String) -> Unit,
    onApplyLineCompletion: (String, Boolean) -> Unit,
    onDismissAi: () -> Unit,
    onQuickSave: () -> Unit
) {
    var showOptionalDetails by remember {
        mutableStateOf(uiState.titleInput.isNotBlank() || uiState.themeTagInput.isNotBlank())
    }
    var showAuthorEditor by remember { mutableStateOf(signature.isBlank()) }
    var showAiHelper by remember { mutableStateOf(false) }
    var coupletFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = uiState.coupletInput,
                selection = TextRange(uiState.coupletInput.length)
            )
        )
    }
    LaunchedEffect(uiState.externalDraftVersion) {
        if (coupletFieldValue.text != uiState.coupletInput) {
            coupletFieldValue = TextFieldValue(
                text = uiState.coupletInput,
                selection = TextRange(uiState.coupletInput.length)
            )
        }
    }
    val readingStyle = LocalShayariReadingStyle.current
    val trimmedLength = coupletFieldValue.text.trim().length

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Optional Details Row (Title + Category Tag) — unobtrusive pills
            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (showOptionalDetails) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showOptionalDetails = !showOptionalDetails }
                        .testTag("studio_toggle_details_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = uiState.themeTagInput.trim().ifBlank { "टॉपिक / शीर्षक (वैकल्पिक)" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showOptionalDetails) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ShayariCategory.entries, key = { it.id }) { category ->
                            val selected = uiState.themeTagInput.trim() == category.titleHi
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onTogglePresetTag(category.titleHi) }
                                    .testTag("personal_preset_tag_${category.id}")
                            ) {
                                Text(
                                    text = "${category.titleHi} ${category.emoji}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.titleInput,
                            onValueChange = onTitleChanged,
                            label = { Text("शीर्षक (वैकल्पिक)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("personal_title_input")
                        )
                        OutlinedTextField(
                            value = uiState.themeTagInput,
                            onValueChange = onThemeTagChanged,
                            label = { Text("टैग (वैकल्पिक)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("personal_theme_tag_input")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clean distraction-free paper canvas (YourQuote-style)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 250.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 180.dp)
                    ) {
                        if (coupletFieldValue.text.isEmpty()) {
                            Text(
                                text = "यहाँ अपनी शायरी लिखें…\n(Hindi, Hinglish या English — अपने कीबोर्ड से लिखें)",
                                style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.titleLarge).copy(
                                    fontSize = 20.sp,
                                    lineHeight = 32.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                        BasicTextField(
                            value = coupletFieldValue,
                            onValueChange = { newValue ->
                                val boundedValue = if (newValue.text.length <= 600) {
                                    newValue
                                } else {
                                    val clipped = newValue.text.take(600)
                                    TextFieldValue(
                                        text = clipped,
                                        selection = TextRange(
                                            newValue.selection.start.coerceAtMost(clipped.length),
                                            newValue.selection.end.coerceAtMost(clipped.length)
                                        )
                                    )
                                }
                                coupletFieldValue = boundedValue
                                onCoupletChanged(boundedValue.text)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrectEnabled = true
                            ),
                            textStyle = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.titleLarge).copy(
                                fontSize = 20.sp,
                                lineHeight = 32.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 180.dp)
                                .testTag("personal_couplet_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (showAuthorEditor) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                1.dp,
                                if (showAuthorEditor) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showAuthorEditor = !showAuthorEditor }
                                .testTag("studio_edit_author_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = if (signature.isNotBlank()) {
                                        "— $signature"
                                    } else {
                                        "— अपना नाम लिखें"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = readingStyle.fontStyle.fontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "नाम बदलें",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = "$trimmedLength / 600",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showAuthorEditor) {
                        OutlinedTextField(
                            value = signature,
                            onValueChange = onAuthorNameChanged,
                            label = { Text("आपका नाम (कार्ड पर दिखेगा)") },
                            placeholder = { Text("अपना नाम या तख़ल्लुस लिखें") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .testTag("studio_author_name_input")
                        )
                    }
                }
            }

            // 1-tap Starter Lines when canvas is blank
            AnimatedVisibility(visible = coupletFieldValue.text.isBlank()) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "शुरुआत करने के लिए एक लाइन चुनें:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StudioPromptStarters.starters) { starter ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        coupletFieldValue = TextFieldValue(
                                            text = starter,
                                            selection = TextRange(starter.length)
                                        )
                                        onCoupletChanged(starter)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EditNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = starter.trim(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showAiHelper) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    ShayariAiAssistPanel(
                        aiState = uiState.aiUiState,
                        onRequestRhymes = { onRequestAiAssist(ShayariAssistMode.RHYMES) },
                        onRequestCompletions = { onRequestAiAssist(ShayariAssistMode.COMPLETE_LINES) },
                        onInsertWord = onInsertRhymingWord,
                        onAppendLine = { line -> onApplyLineCompletion(line, false) },
                        onReplaceWithLine = { line -> onApplyLineCompletion(line, true) },
                        onDismiss = {
                            onDismissAi()
                            showAiHelper = false
                        }
                    )
                }
            }
        }

        // Lightweight Quick-Format Bar (New Line, Purnviram, Em-dash, Mood words)
        ShayariQuickFormatBar(
            onInsertSnippet = { snippet, padSpace ->
                val updated = HinglishTransliterator.insertAtCursor(
                    fieldValue = coupletFieldValue,
                    snippet = snippet,
                    padWithSpace = padSpace
                )
                if (updated.text.length <= 600) {
                    coupletFieldValue = updated
                    onCoupletChanged(updated.text)
                }
            }
        )

        // Clean Bottom Write Toolbar (AI Assist toggle + Direct Save)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { showAiHelper = !showAiHelper },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("studio_ai_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.ai_assist_title),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            TextButton(
                onClick = onQuickSave,
                enabled = trimmedLength >= 4 && !uiState.isSaving,
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("studio_quick_save_button")
            ) {
                Text(
                    text = stringResource(R.string.studio_save_post),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun StudioDesignStepContent(
    couplet: String,
    authorName: String,
    title: String,
    tag: String,
    design: ShayariDesignState,
    activeTab: DesignTab,
    canSavePost: Boolean,
    isSaving: Boolean,
    isSharingImage: Boolean,
    onTabSelected: (DesignTab) -> Unit,
    onSelectWallpaper: (StudioWallpaper) -> Unit,
    onPickGalleryPhoto: () -> Unit,
    onCycleDimness: () -> Unit,
    onDesignUpdated: (ShayariDesignState) -> Unit,
    onSavePost: () -> Unit,
    onShareCard: () -> Unit,
    onDownloadCard: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Live Card Preview Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.46f)
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            WriteStudioDesignCardPreview(
                couplet = couplet,
                authorName = authorName,
                title = title,
                tag = tag,
                design = design,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. YourQuote-style WALLPAPER | TEXT TabRow
        TabRow(
            selectedTabIndex = if (activeTab == DesignTab.WALLPAPER) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = activeTab == DesignTab.WALLPAPER,
                onClick = { onTabSelected(DesignTab.WALLPAPER) },
                modifier = Modifier.testTag("studio_tab_wallpaper"),
                text = {
                    Text(
                        text = stringResource(R.string.studio_tab_wallpaper),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            )
            Tab(
                selected = activeTab == DesignTab.TEXT,
                onClick = { onTabSelected(DesignTab.TEXT) },
                modifier = Modifier.testTag("studio_tab_text"),
                text = {
                    Text(
                        text = stringResource(R.string.studio_tab_text),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            )
        }

        // 3. Controls area (Wallpaper grid OR Text controls)
        Box(
            modifier = Modifier
                .weight(0.54f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                DesignTab.WALLPAPER -> {
                    StudioWallpaperPickerGrid(
                        selectedWallpaper = design.wallpaper,
                        scrimDimness = design.scrimDimness,
                        onSelectWallpaper = onSelectWallpaper,
                        onPickGalleryPhoto = onPickGalleryPhoto,
                        onCycleDimness = onCycleDimness
                    )
                }

                DesignTab.TEXT -> {
                    StudioTextControlsPanel(
                        design = design,
                        onSelectFont = { onDesignUpdated(design.copy(fontStyle = it)) },
                        onSelectColor = { onDesignUpdated(design.copy(textColor = it)) },
                        onSelectTextAlign = { onDesignUpdated(design.copy(textAlign = it)) },
                        onSelectVerticalAlign = { onDesignUpdated(design.copy(verticalAlign = it)) },
                        onFontSizeChanged = { onDesignUpdated(design.copy(fontSizeSp = it)) },
                        onScrimChanged = { onDesignUpdated(design.copy(scrimDimness = it)) }
                    )
                }
            }
        }

        // 4. Bottom Share / Save / Download Bar
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDownloadCard,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 46.dp)
                    .testTag("studio_download_card_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = stringResource(R.string.studio_download_card),
                    modifier = Modifier.size(18.dp)
                )
            }

            FilledTonalButton(
                onClick = onShareCard,
                enabled = !isSharingImage,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 46.dp)
                    .testTag("studio_share_card_button")
            ) {
                if (isSharingImage) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.studio_share_card),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onSavePost,
                enabled = canSavePost && !isSaving,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 46.dp)
                    .testTag("studio_save_post_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkAdded,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.studio_save_post),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
