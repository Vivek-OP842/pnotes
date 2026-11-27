package com.pnotes.ui.auth

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.ui.prototype.LoginObsidianMonochrome
import com.pnotes.ui.prototype.ProtoTextStyle
import com.pnotes.ui.theme.LocalReducedMotion
import com.pnotes.ui.theme.rememberReducedMotion

/** Everything the login surface needs to know about the world; it holds no behaviour itself. */
@Immutable
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val rememberFor30Days: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
) {
    /** Submitting demands both credentials, and never races an in-flight attempt. */
    val canSubmit: Boolean get() = !busy && email.isNotBlank() && password.isNotEmpty()
}

/** The prototype's own status colour for the muted explanatory line. */
private val LoginStatus = Color(0xFF94A3B8)
private val LoginErrorColor = Color(0xFFF87171)

private const val STATUS_IDLE = "Enter your Obsidian ID credentials to access your notes."
private const val STATUS_BUSY = "Checking your Obsidian ID\u2026"
private const val SUBMIT_IDLE = "Sign In to Workspace"
private const val SUBMIT_BUSY = "Signing in\u2026"

/**
 * The Login Page, reproduced from the prototype's Obsidian Monochrome frame.
 *
 * The artwork is the generated frame, untouched; every control above it is positioned in the
 * prototype's own design pixels so it lands exactly where the design drew it. The frame declares no
 * motion of its own, so feedback follows Apple's fluid-interface rules: press acknowledgement on
 * pointer-down, critically damped springs (a tap carries no momentum, so it must not overshoot), and
 * an instant equivalent when the user has asked the system for reduced motion.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRememberChange: (Boolean) -> Unit,
    onGoogleSignIn: () -> Unit,
    onPasskeySignIn: () -> Unit,
    onAppleSignIn: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReducedMotion()
    val focus = LocalFocusManager.current
    val busy = state.busy

    CompositionLocalProvider(LocalReducedMotion provides reduceMotion) {
        LoginObsidianMonochrome(
            modifier = modifier,
            email = state.email,
            // The frame's sample value is a literal bullet string; keep the same glyph so the
            // masked field looks exactly as drawn at any length.
            password = if (state.password.isEmpty()) {
                ""
            } else {
                "\u2022".repeat(state.password.length)
            },
            submitLabel = if (busy) SUBMIT_BUSY else SUBMIT_IDLE,
            statusText = when {
                // An error takes the status line's place rather than adding a row, so the layout of
                // every other state stays byte-for-byte what the prototype drew.
                state.error != null -> ""
                busy -> STATUS_BUSY
                else -> STATUS_IDLE
            },
        ) {
            state.error?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.offset(x = 912.dp, y = 238.5.dp).size(408.dp, 16.dp),
                    style = ProtoTextStyle(
                        fontSize = 13.sp,
                        color = LoginErrorColor,
                        lineHeight = 15.99.sp,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    color = LoginErrorColor,
                )
            }

            ProtoHit(912f, 278f, 199f, 44f, onGoogleSignIn, enabled = !busy)
            ProtoHit(1121f, 278f, 199f, 44f, onPasskeySignIn, enabled = !busy)
            ProtoHit(912f, 332f, 408f, 46f, onAppleSignIn, enabled = !busy)

            ProtoField(
                x = 912f,
                y = 465f,
                width = 408f,
                height = 46f,
                value = state.email,
                onValueChange = onEmailChange,
                enabled = !busy,
                leadingInset = 40f,
            )
            ProtoField(
                x = 912f,
                y = 545f,
                width = 408f,
                height = 46f,
                value = state.password,
                onValueChange = onPasswordChange,
                enabled = !busy,
                isPassword = true,
                leadingInset = 38.22f,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focus.clearFocus()
                    onSubmit()
                },
            )

            ProtoHit(1277f, 520f, 43f, 24f, onForgotPassword, cornerRadius = 6f, haptic = false, enabled = !busy)
            ProtoHit(
                912f, 606f, 207f, 22f,
                onClick = { onRememberChange(!state.rememberFor30Days) },
                cornerRadius = 6f,
                enabled = !busy,
            )
            ProtoHit(912f, 652f, 408f, 24f, onCreateAccount, cornerRadius = 6f, haptic = false, enabled = !busy)

            ProtoHit(
                x = 912f,
                y = 700f,
                width = 408f,
                height = 48f,
                onClick = {
                    focus.clearFocus()
                    onSubmit()
                },
                enabled = state.canSubmit,
                dimmed = !state.canSubmit,
                // The submit bar is the light element on this screen, so a press must darken it.
                pressTint = Color(0xFF050508),
            )
        }
    }
}
