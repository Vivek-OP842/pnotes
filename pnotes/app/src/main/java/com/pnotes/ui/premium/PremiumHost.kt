package com.pnotes.ui.premium

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.pnotes.account.AccountState
import com.pnotes.account.LocalAccountStore
import com.pnotes.billing.Billing
import com.pnotes.billing.PREMIUM_PRODUCT_ID
import com.pnotes.billing.PurchaseResult
import com.pnotes.ui.theme.LocalPalette
import kotlinx.coroutines.launch

/** A line for the status row: what to say, and whether it is a problem rather than a status. */
private data class Notice(val text: String, val isError: Boolean = false)

/**
 * Owns the premium surface's state and turns its intents into real work.
 *
 * Entitlement is only ever recorded from a reported outcome — an attempt that was not reported as
 * owned grants nothing, and a restore that finds nothing takes nothing away. When the purchase went
 * through the local test harness with nobody signed in, the entitlement is recorded as device-scoped
 * and flagged, so the surface can never present it as a real sale.
 */
@Composable
fun PremiumHost(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = LocalAccountStore.current
    val accountState = store?.state?.collectAsState()?.value ?: AccountState()
    val paletteIsDark = LocalPalette.current.isDark
    val provider = remember(context) { Billing.provider(context) }
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<Notice?>(null) }

    // Registered while this surface is up, so back closes it rather than reaching the backstage's
    // handler, which would otherwise offer to exit the app.
    BackHandler { onBack() }

    /** Records a reported purchase against the right owner. Grants on no other outcome. */
    fun recordEntitlement() {
        when {
            accountState.signedIn -> store?.grantPremium()
            // Only the local harness may hand out an entitlement with no account behind it.
            provider.testMode -> store?.grantLocalEntitlement()
        }
    }

    fun attempt(body: suspend () -> Notice) {
        if (busy) return
        busy = true
        notice = null
        scope.launch {
            notice = body()
            busy = false
        }
    }

    PremiumScreen(
        state = PremiumUiState(
            upgraded = accountState.premium,
            dark = paletteIsDark,
            email = accountState.account?.email?.takeIf { it.isNotEmpty() },
            busy = busy,
            message = notice?.text,
            messageIsError = notice?.isError == true,
            testMode = provider.testMode,
        ),
        modifier = modifier,
        onBack = onBack,
        onPurchase = {
            // A Play purchase always belongs to an account, so there is nothing to buy while signed
            // out. The local harness is the one exception, and the surface says so.
            if (!accountState.signedIn && !provider.testMode) {
                notice = Notice("Sign in first \u2014 a purchase belongs to your account.", isError = true)
            } else {
                attempt {
                    when (val result = provider.purchase(PREMIUM_PRODUCT_ID)) {
                        is PurchaseResult.Purchased -> {
                            recordEntitlement()
                            Notice("Lifetime access unlocked. Thank you.")
                        }
                        // Declining is not a failure: nothing was charged, so nothing is wrong.
                        PurchaseResult.Cancelled -> Notice("Purchase cancelled \u2014 nothing was charged.")
                        PurchaseResult.NothingToRestore -> Notice("Nothing to restore.")
                        is PurchaseResult.Failed -> Notice(result.message, isError = true)
                    }
                }
            }
        },
        onRestore = {
            attempt {
                when (val result = provider.restore(PREMIUM_PRODUCT_ID)) {
                    is PurchaseResult.Purchased -> {
                        recordEntitlement()
                        Notice("Restored \u2014 lifetime access is now active.")
                    }
                    PurchaseResult.NothingToRestore ->
                        Notice("No lifetime purchase found for this account.", isError = true)
                    PurchaseResult.Cancelled -> Notice("Restore cancelled.")
                    is PurchaseResult.Failed -> Notice(result.message, isError = true)
                }
            }
        },
    )
}
