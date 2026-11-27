package com.pnotes.ui.premium

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.ui.auth.ProtoHit
import com.pnotes.ui.theme.LocalReducedMotion
import com.pnotes.ui.theme.rememberReducedMotion
import com.pnotes.ui.prototype.PremiumActiveStateMonochrome
import com.pnotes.ui.prototype.PremiumDarkObsidianMonochrome
import com.pnotes.ui.prototype.PremiumLightMonochrome
import com.pnotes.ui.prototype.ProtoTextStyle

/** Everything the premium surface needs to know; it holds no behaviour itself. */
@Immutable
data class PremiumUiState(
    /** The lifetime entitlement is held, so the surface shows the Active frame instead of an offer. */
    val upgraded: Boolean = false,
    /** The app's own palette. The prototype ships a separate frame per mode; they are not inversions. */
    val dark: Boolean = false,
    /** The signed-in address, or null when signed out — in which case the identity pill is absent. */
    val email: String? = null,
    val busy: Boolean = false,
    /** A line for the status row. Null keeps the prototype's own copy. */
    val message: String? = null,
    val messageIsError: Boolean = false,
    /** Purchases are simulated on this build; the surface must say so. */
    val testMode: Boolean = false,
)

private val StatusMuted = Color(0xFF94A3B8)
private val ErrorColor = Color(0xFFF87171)

private const val CTA_IDLE = "Get Premium \u2014 \u20B9249"
private const val CTA_BUSY = "Processing\u2026"
private const val RESTORE_IDLE = "Restore Purchase"
private const val RESTORE_BUSY = "Restoring\u2026"
private const val STATUS_IDLE = "Already purchased PNotes Premium? Restore your purchase here."
private const val STATUS_TEST = "Test mode: purchases are simulated on this device. No payment is taken."

/**
 * The Premium Page, reproduced from the prototype's three frames.
 *
 * Which frame is drawn is a function of state, not a redraw of the design: the offer frames (light
 * and Obsidian) are the two the prototype provides, and the Active frame is its own third screen
 * rather than a variation, so it is used exactly as drawn once the entitlement is held.
 *
 * As on the login surface, motion comes from Apple's fluid-interface rules because the prototype
 * declares none: acknowledgement on pointer-down, critically damped springs for taps, and an instant
 * equivalent under reduced motion. Controls are laid over the artwork in the prototype's own design
 * pixels, so they land exactly where the design drew them.
 */
@Composable
fun PremiumScreen(
    state: PremiumUiState,
    onBack: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReducedMotion()
    val busy = state.busy

    val ctaLabel = if (busy) CTA_BUSY else CTA_IDLE
    val restoreLabel = if (busy) RESTORE_BUSY else RESTORE_IDLE
    val status = when {
        state.messageIsError -> ""
        state.message != null -> state.message
        state.testMode -> STATUS_TEST
        else -> STATUS_IDLE
    }
    val activeNotice = state.email
        ?.let { "Lifetime Access is tied to $it and this device." }
        ?: "Lifetime Access is tied to this device."

    CompositionLocalProvider(LocalReducedMotion provides reduceMotion) {
        if (state.upgraded) {
            PremiumActiveStateMonochrome(
                modifier = modifier,
                showAccount = state.email != null,
                signedInEmail = state.email.orEmpty(),
                activeNotice = activeNotice,
            ) {
                // The Active frame carries no offer and no restore: the purchase is already held, so
                // there is nothing left to buy or to look for. Only the way out remains.
                ProtoHit(
                    x = 48f, y = 32f, width = 81f, height = 32f,
                    onClick = onBack,
                    cornerRadius = 10f,
                    haptic = false,
                )
            }
            return@CompositionLocalProvider
        }

        val overlay: @Composable () -> Unit = {
            if (state.messageIsError) {
                state.message?.let { message ->
                    // An error takes the status row's place rather than adding a row, so every other
                    // state keeps the layout the prototype drew.
                    Text(
                        text = message,
                        modifier = Modifier.offset(x = 756.dp, y = 697.dp).size(612.dp, 13.dp),
                        style = ProtoTextStyle(
                            fontSize = 11.sp,
                            color = ErrorColor,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        softWrap = false,
                        color = ErrorColor,
                    )
                }
            }

            ProtoHit(
                x = 48f, y = 32f, width = 81f, height = 32f,
                onClick = onBack,
                cornerRadius = 10f,
                haptic = false,
                pressTint = if (state.dark) Color.White else Color.Black,
            )
            ProtoHit(
                x = 756f, y = 618f, width = 612f, height = 48f,
                onClick = onPurchase,
                cornerRadius = 12f,
                enabled = !busy,
                dimmed = busy,
                // The offer bar is dark on the light frame and light on the Obsidian frame, so the
                // press has to move away from the bar's own luminance in each.
                pressTint = if (state.dark) Color.Black else Color.White,
            )
            ProtoHit(
                x = 756f, y = 670f, width = 612f, height = 24f,
                onClick = onRestore,
                cornerRadius = 8f,
                enabled = !busy,
                haptic = false,
                pressTint = if (state.dark) Color.White else Color.Black,
            )
        }

        if (state.dark) {
            PremiumDarkObsidianMonochrome(
                modifier = modifier,
                showAccount = state.email != null,
                signedInEmail = state.email.orEmpty(),
                ctaLabel = ctaLabel,
                restoreLabel = restoreLabel,
                premiumStatus = status,
                overlay = overlay,
            )
        } else {
            PremiumLightMonochrome(
                modifier = modifier,
                showAccount = state.email != null,
                signedInEmail = state.email.orEmpty(),
                ctaLabel = ctaLabel,
                restoreLabel = restoreLabel,
                premiumStatus = status,
                overlay = overlay,
            )
        }
    }
}
