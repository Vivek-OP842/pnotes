package com.pnotes.billing

import android.content.Context
import kotlinx.coroutines.delay

/**
 * Simulates the purchase flow on a debug build so it can be exercised end to end without a Play
 * Console listing.
 *
 * It is a test harness, and it says so: [testMode] is true, the premium screen labels the purchase
 * as simulated, and what it records is ownership *of this device* — there is no Play account behind
 * it, so nothing here is presented as a real sale. Ownership is persisted in its own preferences
 * rather than in the account store, so a simulated purchase can never be mistaken for an entitlement
 * the account actually paid for.
 */
class LocalTestBillingProvider(context: Context) : BillingProvider {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override val testMode = true

    override suspend fun purchase(productId: String): PurchaseResult {
        // A purchase is never instant. The delay is here so the screen's busy state has to survive a
        // real wait rather than only ever being drawn for one frame.
        delay(SIMULATED_LATENCY_MS)
        prefs.edit().putBoolean(KEY_OWNED, true).apply()
        return PurchaseResult.Purchased(orderId = "TEST-${System.currentTimeMillis()}")
    }

    override suspend fun restore(productId: String): PurchaseResult {
        delay(SIMULATED_LATENCY_MS)
        return if (ownsLocally()) PurchaseResult.Purchased(orderId = null) else PurchaseResult.NothingToRestore
    }

    /** Whether this device has been "bought for" by the test harness. */
    fun ownsLocally(): Boolean = prefs.getBoolean(KEY_OWNED, false)

    private companion object {
        const val PREFS = "pnotes.billing.test"
        const val KEY_OWNED = "owned"
        const val SIMULATED_LATENCY_MS = 700L
    }
}
