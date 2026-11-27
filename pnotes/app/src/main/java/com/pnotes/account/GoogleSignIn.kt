package com.pnotes.account

import android.app.Activity
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.pnotes.BuildConfig
import org.json.JSONObject

/** The signed-in identity, as far as the app needs to know it. */
data class Account(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
)

/** Every way a sign-in attempt can end. Nothing here reports success on the user's behalf. */
sealed interface SignInOutcome {
    data class Success(val account: Account) : SignInOutcome
    data object Cancelled : SignInOutcome

    /** No OAuth client id was supplied, so no attempt was made. */
    data object NotConfigured : SignInOutcome
    data class Failed(val message: String) : SignInOutcome
}

private const val TAG = "GoogleSignIn"

/**
 * Google Sign-In over Credential Manager.
 *
 * The OAuth client id comes from the gitignored `auth.properties`; when it is absent this returns
 * [SignInOutcome.NotConfigured] and never invents a session, because a login screen that reports
 * success without an identity is worse than one that reports it cannot work yet.
 */
object GoogleSignIn {

    suspend fun signIn(activity: Activity): SignInOutcome {
        if (BuildConfig.OAUTH_CLIENT_ID.isBlank()) return SignInOutcome.NotConfigured

        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.OAUTH_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            val credential = CredentialManager.create(activity)
                .getCredential(activity, request)
                .credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                SignInOutcome.Success(accountOf(GoogleIdTokenCredential.createFrom(credential.data)))
            } else {
                SignInOutcome.Failed("Google returned an unexpected credential type.")
            }
        } catch (e: GetCredentialCancellationException) {
            SignInOutcome.Cancelled
        } catch (e: NoCredentialException) {
            SignInOutcome.Failed("No Google account is available on this device.")
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager failed", e)
            SignInOutcome.Failed(e.message ?: "Google sign-in failed.")
        } catch (e: Exception) {
            Log.w(TAG, "Sign-in failed", e)
            SignInOutcome.Failed(e.message ?: "Google sign-in failed.")
        }
    }

    private fun accountOf(credential: GoogleIdTokenCredential): Account {
        // The id token's claims are the only place the email lives; GoogleIdTokenCredential itself
        // exposes the display name and picture but not the address.
        val claims = decodeClaims(credential.idToken)
        return Account(
            id = claims?.optString("sub").orEmpty().ifEmpty { credential.id },
            email = claims?.optString("email").orEmpty(),
            displayName = claims?.optString("name").orEmpty()
                .ifEmpty { credential.displayName.orEmpty() },
            photoUrl = claims?.optString("picture").orEmpty()
                .ifEmpty { credential.profilePictureUri?.toString().orEmpty() }
                .ifEmpty { null },
        )
    }

    private fun decodeClaims(idToken: String): JSONObject? = runCatching {
        val payload = idToken.split('.').getOrNull(1) ?: return null
        val json = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        JSONObject(String(json, Charsets.UTF_8))
    }.getOrNull()
}
