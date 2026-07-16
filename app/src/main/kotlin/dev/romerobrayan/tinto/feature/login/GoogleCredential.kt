package dev.romerobrayan.tinto.feature.login

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Opens the Credential Manager account picker and returns a Google ID token.
 * Lives in the UI layer on purpose: the picker needs an Activity context,
 * which must not leak into repositories or view models.
 *
 * @param context the Activity-scoped context hosting the picker UI.
 * @param serverClientId the Firebase *web* OAuth client id (`default_web_client_id`).
 * @throws androidx.credentials.exceptions.GetCredentialException when the user
 *   cancels, no Google account is available, or Play Services fails.
 */
internal suspend fun fetchGoogleIdToken(context: Context, serverClientId: String): String {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(serverClientId)
        // Show every Google account on the device, not only ones that have
        // already authorized the app — first sign-in is the common case here.
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(false)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val credential = CredentialManager.create(context)
        .getCredential(context, request)
        .credential
    check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        "Unexpected credential type: ${credential.type}"
    }
    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}
