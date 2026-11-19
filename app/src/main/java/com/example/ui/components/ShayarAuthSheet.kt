package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LocalShayarAccount
import com.example.data.model.SocialLinks

private enum class ShayarAuthTab {
    REGISTER,
    LOGIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShayarAuthSheet(
    localAccount: LocalShayarAccount,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    onRegisterSubmit: (
        fullName: String,
        username: String,
        contact: String,
        password: String,
        bio: String,
        socialLinks: SocialLinks
    ) -> Unit,
    onLoginSubmit: (identifier: String, password: String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember(localAccount.isRegistered) {
        mutableStateOf(
            if (localAccount.isRegistered) ShayarAuthTab.LOGIN else ShayarAuthTab.REGISTER
        )
    }

    var fullName by remember(localAccount.fullName) { mutableStateOf(localAccount.fullName) }
    var username by remember(localAccount.username) { mutableStateOf(localAccount.username) }
    var contact by remember(localAccount.contact) { mutableStateOf(localAccount.contact) }
    var registerPassword by remember { mutableStateOf("") }
    var bio by remember(localAccount.bio) { mutableStateOf(localAccount.bio) }
    var socialLinks by remember(localAccount.socialLinks) { mutableStateOf(localAccount.socialLinks) }
    var showRegisterPassword by remember { mutableStateOf(false) }

    var loginIdentifier by remember(localAccount.username, localAccount.contact) {
        mutableStateOf(
            localAccount.username.ifBlank { localAccount.contact.ifBlank { localAccount.fullName } }
        )
    }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    var localValidationError by remember { mutableStateOf<String?>(null) }
    val activeError = localValidationError ?: errorMessage

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("shayar_auth_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.auth_sheet_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.auth_sheet_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Segmented Register / Login Switch
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AuthTabPill(
                        label = stringResource(R.string.auth_tab_register),
                        selected = selectedTab == ShayarAuthTab.REGISTER,
                        onClick = {
                            selectedTab = ShayarAuthTab.REGISTER
                            localValidationError = null
                            onClearError()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_tab_register")
                    )
                    AuthTabPill(
                        label = stringResource(R.string.auth_tab_login),
                        selected = selectedTab == ShayarAuthTab.LOGIN,
                        onClick = {
                            selectedTab = ShayarAuthTab.LOGIN
                            localValidationError = null
                            onClearError()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_tab_login")
                    )
                }
            }

            if (!activeError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_error_banner")
                ) {
                    Text(
                        text = activeError,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            if (selectedTab == ShayarAuthTab.REGISTER) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        if (it.length <= 40) fullName = it
                        localValidationError = null
                        onClearError()
                    },
                    label = { Text(stringResource(R.string.auth_full_name_label)) },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_register_name_input")
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        val cleaned = it.replace(" ", "").take(30)
                        username = cleaned
                        localValidationError = null
                        onClearError()
                    },
                    label = { Text(stringResource(R.string.auth_username_label)) },
                    placeholder = { Text("जैसे: prince_shayar") },
                    leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_register_username_input")
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = {
                        if (it.length <= 80) contact = it
                        localValidationError = null
                        onClearError()
                    },
                    label = { Text(stringResource(R.string.auth_contact_label)) },
                    leadingIcon = { Icon(Icons.Rounded.ContactPhone, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_register_contact_input")
                )

                OutlinedTextField(
                    value = registerPassword,
                    onValueChange = {
                        if (it.length <= 40) registerPassword = it
                        localValidationError = null
                        onClearError()
                    },
                    label = { Text(stringResource(R.string.auth_password_label)) },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showRegisterPassword = !showRegisterPassword }) {
                            Icon(
                                imageVector = if (showRegisterPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showRegisterPassword) "पासवर्ड छुपाएँ" else "पासवर्ड दिखाएँ"
                            )
                        }
                    },
                    visualTransformation = if (showRegisterPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_register_password_input")
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 160) bio = it },
                    label = { Text(stringResource(R.string.poet_bio_label)) },
                    placeholder = { Text(stringResource(R.string.poet_bio_hint)) },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_register_bio_input")
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SocialLinksEditorFields(
                    socialLinks = socialLinks,
                    onSocialLinksChange = { socialLinks = it },
                    collapseSecondaryByDefault = true
                )

                Button(
                    onClick = {
                        val trimmedName = fullName.trim()
                        val trimmedPass = registerPassword.trim()
                        when {
                            trimmedName.length < 2 -> {
                                localValidationError = "कृपया कम से कम 2 अक्षरों का शायर नाम लिखें।"
                            }
                            trimmedPass.length < 4 -> {
                                localValidationError = "कृपया कम से कम 4 अक्षरों का पासवर्ड सेट करें।"
                            }
                            else -> {
                                localValidationError = null
                                onRegisterSubmit(
                                    trimmedName,
                                    username.trim(),
                                    contact.trim(),
                                    trimmedPass,
                                    bio.trim(),
                                    socialLinks
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_register_submit_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auth_register_cta),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                OutlinedTextField(
                    value = loginIdentifier,
                    onValueChange = {
                        loginIdentifier = it
                        localValidationError = null
                        onClearError()
                    },
                    label = { Text(stringResource(R.string.auth_identifier_label)) },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_login_identifier_input")
                )

                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = {
                        loginPassword = it
                        localValidationError = null
                        onClearError()
                    },
                    label = { Text(stringResource(R.string.auth_password_label)) },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                            Icon(
                                imageVector = if (showLoginPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showLoginPassword) "पासवर्ड छुपाएँ" else "पासवर्ड दिखाएँ"
                            )
                        }
                    },
                    visualTransformation = if (showLoginPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_login_password_input")
                )

                Button(
                    onClick = {
                        when {
                            loginIdentifier.trim().isBlank() -> {
                                localValidationError = "कृपया अपना यूज़रनेम, ईमेल या मोबाइल नंबर लिखें।"
                            }
                            loginPassword.trim().length < 4 -> {
                                localValidationError = "कृपया अपना पासवर्ड लिखें।"
                            }
                            else -> {
                                localValidationError = null
                                onLoginSubmit(loginIdentifier.trim(), loginPassword.trim())
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_login_submit_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auth_login_cta),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (!localAccount.isRegistered) {
                    Text(
                        text = "नए यूज़र हैं? ऊपर 'नया रजिस्टर' टैब पर टैप करके तुरंत अकाउंट बनाएँ।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = stringResource(R.string.auth_or_divider),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            OutlinedButton(
                onClick = onGoogleSignInClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_google_signin_button")
            ) {
                Text(
                    text = stringResource(R.string.auth_google_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun AuthTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1
            )
        }
    }
}
