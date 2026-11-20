package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.auth.rememberGoogleSignInLauncher
import com.example.data.model.LocalShayarAccount
import com.example.data.model.SocialLinks
import com.example.ui.components.SocialLinksEditorFields

enum class ShayarAuthMode {
    REGISTER,
    LOGIN
}

@Composable
fun AuthScreen(
    localAccount: LocalShayarAccount,
    errorMessage: String? = null,
    showGoogleSignIn: Boolean = true,
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
    onResetPasswordSubmit: ((newPassword: String) -> Unit)? = null,
    onGoogleAuthSuccess: () -> Unit,
    onGoogleAuthError: (String) -> Unit,
    onGoogleSignInOverride: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val googleSignIn = rememberGoogleSignInLauncher(
        onAuthSuccess = onGoogleAuthSuccess,
        onAuthError = onGoogleAuthError,
        onClickOverride = onGoogleSignInOverride
    )
    val canShowGoogle = showGoogleSignIn && (googleSignIn.isAvailable || onGoogleSignInOverride != null)

    val heroGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .navigationBarsPadding()
            .testTag("auth_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Editorial Top Hero Band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(heroGradient)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("auth_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "शायर महफ़िल",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.auth_sheet_title),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.auth_sheet_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Streamlined Firebase Google Sign-In Primary Card
                if (canShowGoogle) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_firebase_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.CloudDone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "1-टैप क्लाउड लॉगिन (सुझाया गया)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "कवि मंच, फ़ॉलोअर्स, कमेंट और सोशल प्रोफ़ाइल सिंक करें",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onClearError()
                                    googleSignIn.launch()
                                },
                                enabled = !googleSignIn.isLoading,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("auth_google_signin_button")
                            ) {
                                if (googleSignIn.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "लॉगिन हो रहा है…",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.auth_google_cta),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "${stringResource(R.string.auth_or_divider)} शायर अकाउंट से",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                // Unified Local Register / Login Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShayarAuthFormContent(
                        localAccount = localAccount,
                        errorMessage = errorMessage,
                        showGoogleSignIn = false,
                        isGoogleLoading = googleSignIn.isLoading,
                        onClearError = onClearError,
                        onRegisterSubmit = onRegisterSubmit,
                        onLoginSubmit = onLoginSubmit,
                        onResetPasswordSubmit = onResetPasswordSubmit,
                        onGoogleSignInClick = { googleSignIn.launch() },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShayarAuthFormContent(
    localAccount: LocalShayarAccount,
    errorMessage: String? = null,
    showGoogleSignIn: Boolean = true,
    isGoogleLoading: Boolean = false,
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
    onResetPasswordSubmit: ((newPassword: String) -> Unit)? = null,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    var selectedTab by remember(localAccount.isRegistered) {
        mutableStateOf(
            if (localAccount.isRegistered) ShayarAuthMode.LOGIN else ShayarAuthMode.REGISTER
        )
    }

    var fullName by remember(localAccount.fullName) { mutableStateOf(localAccount.fullName) }
    var username by remember(localAccount.username) { mutableStateOf(localAccount.username) }
    var contact by remember(localAccount.contact) { mutableStateOf(localAccount.contact) }
    var registerPassword by remember { mutableStateOf("") }
    var bio by remember(localAccount.bio) { mutableStateOf(localAccount.bio) }
    var socialLinks by remember(localAccount.socialLinks) { mutableStateOf(localAccount.socialLinks) }
    var showRegisterPassword by remember { mutableStateOf(false) }
    var showOptionalProfileFields by remember { mutableStateOf(false) }

    var loginIdentifier by remember(localAccount.username, localAccount.contact, localAccount.fullName) {
        mutableStateOf(
            localAccount.username.ifBlank { localAccount.contact.ifBlank { localAccount.fullName } }
        )
    }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }
    var showResetPasswordBox by remember { mutableStateOf(false) }
    var newResetPassword by remember { mutableStateOf("") }

    var localValidationError by remember { mutableStateOf<String?>(null) }
    val activeError = localValidationError ?: errorMessage

    fun submitRegister() {
        val trimmedName = fullName.trim()
        val trimmedPass = registerPassword.trim()
        when {
            trimmedName.length < 2 -> {
                localValidationError = "कृपया अपना नाम लिखें (कम से कम 2 अक्षर)।"
            }
            trimmedPass.isNotEmpty() && trimmedPass.length < 4 -> {
                localValidationError = "पासवर्ड या तो खाली छोड़ें या कम से कम 4 अक्षर लिखें।"
            }
            else -> {
                focusManager.clearFocus()
                localValidationError = null
                val autoHandle = username.trim().ifBlank {
                    trimmedName.lowercase()
                        .replace("[^a-z0-9_]".toRegex(), "")
                        .ifBlank { "shayar" }
                }
                onRegisterSubmit(
                    trimmedName,
                    autoHandle,
                    contact.trim(),
                    trimmedPass,
                    bio.trim(),
                    socialLinks.normalized()
                )
            }
        }
    }

    fun submitLogin() {
        when {
            loginIdentifier.trim().isBlank() -> {
                localValidationError = "कृपया अपना नाम, यूज़रनेम, मोबाइल या ईमेल लिखें।"
            }
            loginPassword.trim().length < 4 -> {
                localValidationError = "कृपया अपना 4+ अक्षरों का पासवर्ड लिखें।"
            }
            else -> {
                focusManager.clearFocus()
                localValidationError = null
                onLoginSubmit(loginIdentifier.trim(), loginPassword.trim())
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Animated Segmented Mode Switcher
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
                AuthModePill(
                    label = stringResource(R.string.auth_tab_register),
                    selected = selectedTab == ShayarAuthMode.REGISTER,
                    onClick = {
                        selectedTab = ShayarAuthMode.REGISTER
                        localValidationError = null
                        showResetPasswordBox = false
                        onClearError()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("auth_tab_register")
                )
                AuthModePill(
                    label = stringResource(R.string.auth_tab_login),
                    selected = selectedTab == ShayarAuthMode.LOGIN,
                    onClick = {
                        selectedTab = ShayarAuthMode.LOGIN
                        localValidationError = null
                        onClearError()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("auth_tab_login")
                )
            }
        }

        AnimatedVisibility(
            visible = !activeError.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_error_banner")
            ) {
                Text(
                    text = activeError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(160))
            },
            label = "unifiedAuthTabContent"
        ) { tab ->
            if (tab == ShayarAuthMode.REGISTER) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            if (it.length <= 40) fullName = it
                            localValidationError = null
                            onClearError()
                        },
                        label = { Text("आपका शायर नाम *") },
                        placeholder = { Text("जैसे: प्रिंस, राहत या आपका नाम") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        trailingIcon = if (fullName.trim().length >= 2) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_register_name_input")
                    )

                    OutlinedTextField(
                        value = contact,
                        onValueChange = {
                            if (it.length <= 80) contact = it
                            localValidationError = null
                            onClearError()
                        },
                        label = { Text("मोबाइल नंबर या ईमेल (वैकल्पिक)") },
                        placeholder = { Text("जैसे: 98XXXXXXXX या email@gmail.com") },
                        leadingIcon = { Icon(Icons.Rounded.ContactPhone, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
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
                        label = { Text("पासवर्ड (वैकल्पिक — बिना पासवर्ड भी बनेगा)") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showRegisterPassword = !showRegisterPassword }) {
                                Icon(
                                    imageVector = if (showRegisterPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showRegisterPassword) "पासवर्ड छुपाएँ" else "पासवर्ड दिखाएँ"
                                )
                            }
                        },
                        supportingText = {
                            when {
                                registerPassword.trim().isEmpty() -> {
                                    Text(
                                        text = "✓ बस नाम लिखकर तुरंत अकाउंट बना सकते हैं",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                registerPassword.trim().length >= 4 -> {
                                    Text(
                                        text = "✓ पासवर्ड तैयार है",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                else -> {
                                    Text("पासवर्ड रखें तो 4+ अक्षर लिखें, या खाली छोड़ दें")
                                }
                            }
                        },
                        visualTransformation = if (showRegisterPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitRegister() }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_register_password_input")
                    )

                    // Collapsible Optional Social Links, @Username & Bio
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showOptionalProfileFields = !showOptionalProfileFields }
                            .testTag("auth_toggle_optional_socials")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "सोशल लिंक, @username व बायो जोड़ें (वैकल्पिक)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Facebook, Instagram, WhatsApp — प्रोफ़ाइल से बाद में भी जोड़ सकते हैं",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = if (showOptionalProfileFields) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showOptionalProfileFields,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it.replace(" ", "").take(30)
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

                            SocialLinksEditorFields(
                                socialLinks = socialLinks,
                                onSocialLinksChange = { socialLinks = it },
                                collapseSecondaryByDefault = true
                            )
                        }
                    }

                    Button(
                        onClick = ::submitRegister,
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
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (localAccount.isRegistered && localAccount.fullName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    loginIdentifier = localAccount.username
                                        .ifBlank { localAccount.contact.ifBlank { localAccount.fullName } }
                                    localValidationError = null
                                    onClearError()
                                }
                                .testTag("auth_saved_account_chip")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = localAccount.fullName.firstOrNull()?.uppercase() ?: "श",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = localAccount.fullName,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = listOfNotNull(
                                            localAccount.handleDisplay.takeIf { it.isNotBlank() },
                                            localAccount.contact.takeIf { it.isNotBlank() }
                                        ).joinToString(" • ").ifBlank { "टैप करके नाम भरें" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "चुनें",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = loginIdentifier,
                        onValueChange = {
                            loginIdentifier = it
                            localValidationError = null
                            onClearError()
                        },
                        label = { Text("नाम, यूज़रनेम, मोबाइल या ईमेल") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
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
                        label = { Text("पासवर्ड") },
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitLogin() }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_login_password_input")
                    )

                    Button(
                        onClick = ::submitLogin,
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (localAccount.isRegistered && onResetPasswordSubmit != null) {
                            TextButton(
                                onClick = {
                                    showResetPasswordBox = !showResetPasswordBox
                                    localValidationError = null
                                    onClearError()
                                },
                                modifier = Modifier.testTag("auth_forgot_password_button")
                            ) {
                                Text(
                                    text = "पासवर्ड भूल गए?",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        TextButton(
                            onClick = {
                                selectedTab = ShayarAuthMode.REGISTER
                                localValidationError = null
                                onClearError()
                            },
                            modifier = Modifier.testTag("auth_switch_to_register_button")
                        ) {
                            Text(
                                text = "नया अकाउंट बनाएँ →",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showResetPasswordBox && localAccount.isRegistered && onResetPasswordSubmit != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "नया पासवर्ड सेट करें और तुरंत लॉगिन करें",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = newResetPassword,
                                    onValueChange = { if (it.length <= 40) newResetPassword = it },
                                    label = { Text("नया पासवर्ड (4+ अक्षर)") },
                                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_reset_password_input")
                                )
                                FilledTonalButton(
                                    onClick = {
                                        if (newResetPassword.trim().length < 4) {
                                            localValidationError = "कृपया कम से कम 4 अक्षरों का नया पासवर्ड लिखें।"
                                        } else {
                                            focusManager.clearFocus()
                                            localValidationError = null
                                            onResetPasswordSubmit?.invoke(newResetPassword.trim())
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("auth_reset_password_submit")
                                ) {
                                    Text(
                                        text = "पासवर्ड बदलें व लॉगिन करें",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showGoogleSignIn) {
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
                enabled = !isGoogleLoading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_google_signin_button")
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.auth_google_cta),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "authModeBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "authModeFg"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
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
