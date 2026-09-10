package id.app.education.ui.screens.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import id.app.education.BuildConfig
import timber.log.Timber

/**
 * Google sign-in via Credential Manager (replaces the legacy `GoogleSignInClient` from the
 * deleted `LoginActivity`) — only the credential-retrieval half; the backend `login-sso` call
 * that consumes the resulting ID token is not yet wired (see [SsoScreen]'s note).
 */
suspend fun signInWithGoogle(context: Context): GoogleAccountInfo? {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.WEB_CLIENT_ID)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val result = CredentialManager.create(context).getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleAccountInfo(
                idToken = googleIdTokenCredential.idToken,
                displayName = googleIdTokenCredential.displayName.orEmpty(),
                email = googleIdTokenCredential.id,
            )
        } else {
            null
        }
    } catch (e: GetCredentialException) {
        Timber.e(e, "Google sign-in failed")
        null
    }
}
