package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SocialLinks
import com.example.ui.components.AccountAvatar
import com.example.ui.components.SocialLinksEditorFields
import com.example.ui.theme.RoseVioletGradientEnd
import com.example.ui.theme.RoseVioletGradientStart
import com.example.util.ProfilePhotoStorage
import kotlinx.coroutines.launch

private val ACCOUNT_GRADIENT = Brush.horizontalGradient(
    colors = listOf(RoseVioletGradientStart, RoseVioletGradientEnd)
)

@Composable
fun EasyProfileEditorScreen(
    initialName: String,
    initialBio: String,
    initialStoredPhotoUri: String,
    fallbackRemotePhotoUri: Uri? = null,
    initialSocialLinks: SocialLinks = SocialLinks(),
    isSaving: Boolean = false,
    onBack: () -> Unit,
    onFeedback: (String) -> Unit = {},
    onSave: (name: String, bio: String, storedPhotoUri: String, socialLinks: SocialLinks) -> Unit,
    onPickPhotoClickOverride: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !isSaving, onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(initialName) { mutableStateOf(initialName) }
    var bio by remember(initialBio) { mutableStateOf(initialBio) }
    var storedPhotoUri by remember(initialStoredPhotoUri) { mutableStateOf(initialStoredPhotoUri) }
    var socialLinks by remember(initialSocialLinks) { mutableStateOf(initialSocialLinks) }
    var showSocialLinks by remember { mutableStateOf(false) }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri ->
        if (pickedUri != null) {
            scope.launch {
                isProcessingPhoto = true
                val savedPath = ProfilePhotoStorage.savePickedProfilePhoto(context, pickedUri)
                isProcessingPhoto = false
                if (savedPath != null) {
                    storedPhotoUri = savedPath
                } else {
                    onFeedback(context.getString(R.string.profile_photo_error_snackbar))
                }
            }
        }
    }

    fun launchPhotoPicker() {
        if (onPickPhotoClickOverride != null) {
            onPickPhotoClickOverride()
        } else {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    val previewPhotoUri = remember(storedPhotoUri, fallbackRemotePhotoUri) {
        ProfilePhotoStorage.resolveDisplayUri(
            storedPhotoUri = storedPhotoUri,
            fallbackRemoteUri = fallbackRemotePhotoUri
        )
    }
    val hasCustomPhoto = storedPhotoUri.isNotBlank()
    val cleanName = name.trim()
    val canSave = !isSaving && !isProcessingPhoto

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("easy_profile_editor_screen")
    ) {
        // Vibrant Pink-to-Purple Gradient Header Bar ("←  Account")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ACCOUNT_GRADIENT)
                .statusBarsPadding()
                .defaultMinSize(minHeight = 60.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = { if (!isSaving) onBack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .minimumInteractiveComponentSize()
                    .testTag("easy_profile_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = Color.White
                )
            }

            Text(
                text = stringResource(R.string.profile_editor_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("easy_profile_title")
            )
        }

        // Scrollable, Airy Form Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Profile Photo Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Circular Photo Preview (clickable to pick photo)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable(
                                    enabled = !isProcessingPhoto && !isSaving,
                                    role = Role.Button,
                                    onClick = ::launchPhotoPicker
                                )
                                .testTag("easy_profile_avatar_preview")
                        ) {
                            AccountAvatar(
                                photoUrl = previewPhotoUri,
                                displayName = cleanName.ifBlank { "शायर" },
                                isSignedIn = true,
                                isSelected = true,
                                size = 68.dp,
                                contentDescription = stringResource(R.string.profile_photo_heading)
                            )
                            if (isProcessingPhoto) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.profile_photo_heading),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                                        ),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable(
                                                enabled = !isProcessingPhoto && !isSaving,
                                                role = Role.Button,
                                                onClick = ::launchPhotoPicker
                                            )
                                            .testTag("easy_profile_choose_photo_button")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.profile_photo_choose),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (previewPhotoUri != null) {
                                            stringResource(R.string.profile_photo_selected)
                                        } else {
                                            stringResource(R.string.profile_photo_none)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (previewPhotoUri != null) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (hasCustomPhoto) {
                                        IconButton(
                                            onClick = {
                                                storedPhotoUri = ""
                                                scope.launch {
                                                    ProfilePhotoStorage.clearSavedProfilePhotos(context)
                                                }
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("easy_profile_remove_photo_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.profile_photo_remove_cd),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Clean Rounded Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.profile_name_placeholder),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = RoseVioletGradientStart,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("easy_profile_name_input")
                )

                // 3. Clean Rounded Bio / Mood Input
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 160) bio = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.profile_bio_placeholder),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    minLines = 1,
                    maxLines = 3,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = RoseVioletGradientEnd,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("easy_profile_bio_input")
                )

                // 4. Full-Width Pink-to-Purple Gradient Save Button ("प्रोफ़ाइल सेव करें")
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            enabled = canSave,
                            role = Role.Button
                        ) {
                            val resolvedName = cleanName.ifBlank {
                                initialName.trim().ifBlank { "शायर दोस्त" }
                            }
                            onSave(
                                resolvedName,
                                bio.trim(),
                                storedPhotoUri,
                                socialLinks.normalized()
                            )
                        }
                        .testTag("easy_profile_save_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (canSave) {
                                    ACCOUNT_GRADIENT
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profile_save_cta),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }

                // 5. Optional Social Links Expander (keeps default UI super clean like the screenshot)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable(role = Role.Button) {
                                    showSocialLinks = !showSocialLinks
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("easy_profile_toggle_socials"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.profile_social_links_optional),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (showSocialLinks) {
                                    Icons.Rounded.ExpandLess
                                } else {
                                    Icons.Rounded.ExpandMore
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = showSocialLinks,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            SocialLinksEditorFields(
                                socialLinks = socialLinks,
                                onSocialLinksChange = { socialLinks = it },
                                collapseSecondaryByDefault = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PaddingValues(start = 14.dp, end = 14.dp, bottom = 14.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
