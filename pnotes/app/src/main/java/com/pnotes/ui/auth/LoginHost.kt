package com.pnotes.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.pnotes.account.Account
import com.pnotes.account.GoogleSignIn
import com.pnotes.account.SignInOutcome
import kotlinx.coroutines.launch

/**
 * Owns the login surface's state and turns its intents into real work.
 *
 * Email/password submission deliberately does not succeed: this app has no credential backend, and
 * the prototype's "Obsidian ID" form has nothing behind it. Rather than fake a session it says so,
 * and points at the provider that does work.
 */
@Composable
fun LoginHost(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSignedIn: (Account) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(LoginUiState()) }

    // The surface is dismissed by the system back gesture. Registered while this composable is up,
    // so it wins over the backstage's own handler, which would otherwise offer to exit the app.
    BackHandler { onBack() }

    fun signInWithGoogle() {
        val activity = context.findActivity()
        if (activity == null) {
            state = state.copy(error = "Sign-in is unavailable in this window.")
            return
        }
        state = state.copy(busy = true, error = null)
        scope.launch {
            state = when (val outcome = GoogleSignIn.signIn(activity)) {
                is SignInOutcome.Success -> {
                    onSignedIn(outcome.account)
                    onBack()
                    LoginUiState()
                }
                // A cancelled attempt is not an error: leave the form exactly as the user left it.
                SignInOutcome.Cancelled -> state.copy(busy = false)
                SignInOutcome.NotConfigured -> state.copy(
                    busy = false,
                    error = "Google sign-in is not configured on this build yet.",
                )
                is SignInOutcome.Failed -> state.copy(busy = false, error = outcome.message)
            }
        }
    }

    LoginScreen(
        state = state,
        modifier = modifier,
        onEmailChange = { state = state.copy(email = it, error = null) },
        onPasswordChange = { state = state.copy(password = it, error = null) },
        onRememberChange = { state = state.copy(rememberFor30Days = it) },
        onGoogleSignIn = { signInWithGoogle() },
        onPasskeySignIn = {
            state = state.copy(error = "Passkeys are not available in this build yet.")
        },
        onAppleSignIn = {
            state = state.copy(error = "Sign in with Apple is not available in this build yet.")
        },
        onSubmit = {
            state = state.copy(
                error = "Email sign-in needs a PNotes account server. Use Google for now."
            )
        },
        onForgotPassword = {
            state = state.copy(error = "Password recovery arrives with the account server.")
        },
        onCreateAccount = {
            state = state.copy(error = "Creating an Obsidian ID arrives with the account server.")
        },
    )
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
