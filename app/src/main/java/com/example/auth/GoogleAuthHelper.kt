package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import java.security.MessageDigest
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

const val OWNER_EMAIL = "princeorviel@gmail.com"
const val OWNER_USERNAME = "princeorviel"

fun isOwnerEmail(email: String?): Boolean {
    return email?.trim()?.equals(OWNER_EMAIL, ignoreCase = true) == true
}

fun isAppOwner(user: FirebaseUser?): Boolean {
    return isOwnerEmail(user?.email)
}

fun isLocalOwnerAccount(account: com.example.data.model.LocalShayarAccount?): Boolean {
    if (account == null || !account.isLoggedIn) return false
    if (isOwnerEmail(account.contact)) return true
    val cleanUsername = account.username.trim().removePrefix("@")
    return cleanUsername.equals(OWNER_USERNAME, ignoreCase = true)
}

fun isFirebaseAvailable(context: Context): Boolean {
    return runCatching {
        val appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isNotEmpty()) {
            true
        } else {
            FirebaseApp.initializeApp(appContext) != null
        }
    }.getOrDefault(false)
}

fun safeCurrentUser(context: Context): FirebaseUser? {
    if (!isFirebaseAvailable(context)) return null
    return runCatching { Firebase.auth.currentUser }.getOrNull()
}

fun safeAuthStateFlow(context: Context): Flow<FirebaseUser?> {
    if (!isFirebaseAvailable(context)) return flowOf(null)
    return runCatching { Firebase.auth.authStateFlow() }.getOrElse { flowOf(null) }
}

private fun resolveDefaultWebClientId(context: Context): String? {
    val resId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    if (resId == 0) return null
    return runCatching { context.getString(resId) }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit = {},
    onUnauthenticated: () -> Unit = {}
) {
    if (!isFirebaseAvailable(context)) {
        onUnauthenticated()
        return
    }
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

private val EXPECTED_FIREBASE_SHA1S = setOf(
    "D6:68:6A:1E:B0:A6:49:D0:37:FD:58:CD:AE:88:A3:00:C2:AB:B1:DA",
    "0F:BA:D1:CF:A4:3F:AE:DF:03:CF:54:37:4D:E0:49:A8:B3:2A:AF:CF"
)

private fun isKnownFirebaseSha1(sha1: String?): Boolean {
    return sha1 != null && EXPECTED_FIREBASE_SHA1S.any { it.equals(sha1, ignoreCase = true) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
private fun getAppSigningSha1(context: Context): String? {
    return runCatching {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo ?: return null
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return null
        val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
        digest.joinToString(":") { byte -> "%02X".format(byte) }
    }.getOrNull()
}

private fun isPlayServicesConfigCancellation(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    val lower = message.lowercase()
    return lower.contains("28444") ||
        lower.contains("developer") ||
        lower.contains("not set up") ||
        lower.contains("caller") ||
        lower.contains("reauth") ||
        lower.contains("signature") ||
        lower.contains("10:") ||
        lower.contains("16:")
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: () -> Unit = {}
) {
    if (!isFirebaseAvailable(context)) {
        onAuthError("क्लाउड लॉगिन कॉन्फ़िगर नहीं है — GitHub Secrets में GOOGLE_SERVICES_JSON जोड़कर नई APK बनाएँ।")
        return
    }
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        onAuthError("Google Sign-In कॉन्फ़िग अधूरा है (default_web_client_id नहीं मिला)।")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        val startedAtMs = System.currentTimeMillis()
        try {
            val activity = context.findActivity()
                ?: throw IllegalStateException("Activity context required for Google Sign-In")
            val result = try {
                credentialManager.getCredential(activity, request)
            } catch (firstError: Exception) {
                val fastAbort = firstError is GetCredentialCancellationException &&
                    (System.currentTimeMillis() - startedAtMs) < 1500L
                if (firstError is NoCredentialException || fastAbort) {
                    val fallbackOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(clientId)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(fallbackOption)
                        .build()
                    credentialManager.getCredential(activity, fallbackRequest)
                } else {
                    throw firstError
                }
            }
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("अमान्य Google क्रेडेंशियल मिला (${credential.type})।")
            }
        } catch (e: GetCredentialCancellationException) {
            val elapsedMs = System.currentTimeMillis() - startedAtMs
            val rawMessage = e.errorMessage?.toString() ?: e.message
            val actualSha1 = getAppSigningSha1(context)
            Log.w("Auth", "Google Sign-In cancelled (${elapsedMs}ms): $rawMessage | SHA1=$actualSha1", e)
            when {
                actualSha1 != null && !isKnownFirebaseSha1(actualSha1) -> {
                    onAuthError("APK का SHA-1 मैच नहीं हुआ ($actualSha1) — Firebase में यह SHA-1 जोड़ें।")
                }
                isPlayServicesConfigCancellation(rawMessage) || elapsedMs < 1500L -> {
                    onAuthError("Google लॉगिन शीट खुल नहीं सकी (${rawMessage ?: "FastCancel"} | SHA1: ${actualSha1?.take(11) ?: "?"})")
                }
                else -> {
                    onAuthCancelled()
                }
            }
        } catch (e: NoCredentialException) {
            val rawMessage = e.errorMessage?.toString() ?: e.message
            val actualSha1 = getAppSigningSha1(context)
            Log.e("Auth", "No Google credential available: $rawMessage | SHA1=$actualSha1", e)
            if (actualSha1 != null && !isKnownFirebaseSha1(actualSha1)) {
                onAuthError("APK का SHA-1 मैच नहीं हुआ ($actualSha1) — Firebase में यह SHA-1 जोड़ें।")
            } else {
                onAuthError("फ़ोन में Google अकाउंट नहीं मिला या लॉगिन ब्लॉक है (${rawMessage ?: "NoCredential"})।")
            }
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: e.javaClass.simpleName)
        }
    }
}

fun signOutFromGoogle(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                runCatching { Firebase.auth.signOut() }
                onSignOutComplete()
            }
        }
    }
}

@Stable
class GoogleSignInLauncher(
    val isAvailable: Boolean,
    private val isLoadingState: () -> Boolean,
    private val triggerLaunch: () -> Unit
) {
    val isLoading: Boolean
        get() = isLoadingState()

    fun launch() {
        if (!isLoading) {
            triggerLaunch()
        }
    }
}

@Composable
fun rememberGoogleSignInLauncher(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: () -> Unit = {},
    onClickOverride: (() -> Unit)? = null
): GoogleSignInLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val firebaseAvailable = remember(context) { isFirebaseAvailable(context) }
    var isLoading by remember { mutableStateOf(false) }

    val latestSuccess by rememberUpdatedState(onAuthSuccess)
    val latestError by rememberUpdatedState(onAuthError)
    val latestCancelled by rememberUpdatedState(onAuthCancelled)
    val latestOverride by rememberUpdatedState(onClickOverride)

    return remember(context, credentialManager, scope, firebaseAvailable) {
        GoogleSignInLauncher(
            isAvailable = firebaseAvailable,
            isLoadingState = { isLoading },
            triggerLaunch = {
                val override = latestOverride
                if (override != null) {
                    override()
                    return@GoogleSignInLauncher
                }
                isLoading = true
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    scope = scope,
                    onAuthSuccess = {
                        isLoading = false
                        latestSuccess()
                    },
                    onAuthError = { message ->
                        isLoading = false
                        latestError(message)
                    },
                    onAuthCancelled = {
                        isLoading = false
                        latestCancelled()
                    }
                )
            }
        )
    }
}
