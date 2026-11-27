package com.pnotes.billing

import android.content.Context
import com.pnotes.BuildConfig

/**
 * The one thing this app sells, bought once and kept forever.
 *
 * Google Play hands back a product id per purchase, so the id lives on [BuildConfig] and is
 * overridable from the gitignored `auth.properties` rather than being compiled in.
 */
val PREMIUM_PRODUCT_ID: String get() = BuildConfig.PREMIUM_PRODUCT_ID

/**
 * What an attempt produced. Cancellation is a normal outcome rather than an error — the user chose
 * not to buy, and nothing was charged — so it is modelled separately from failure.
 */
sealed interface PurchaseResult {

    /** The purchase is owned. [orderId] is null when the entitlement came from a restore. */
    data class Purchased(val orderId: String?) : PurchaseResult

    data object Cancelled : PurchaseResult

    /** Asked to restore, and this account owns nothing to restore. */
    data object NothingToRestore : PurchaseResult

    data class Failed(val message: String) : PurchaseResult
}

/**
 * Where a purchase actually goes.
 *
 * The screen owns presentation and the store owns entitlement; this is the only piece that talks to
 * a payment system, so swapping the test harness for Google Play is a change in one factory.
 */
interface BillingProvider {

    /**
     * True when purchases are simulated locally and no money can change hands. The UI must say so
     * rather than presenting simulated success as a real sale.
     */
    val testMode: Boolean

    suspend fun purchase(productId: String): PurchaseResult

    suspend fun restore(productId: String): PurchaseResult
}

object Billing {

    /**
     * Debug builds get the local harness so the whole flow can be exercised without Play Console;
     * release builds must go through Google Play. They deliberately do not fall back: a purchase
     * that silently succeeds without a payment system would be worse than one that admits it cannot
     * take money yet.
     */
    fun provider(context: Context): BillingProvider =
        if (BuildConfig.DEBUG) {
            LocalTestBillingProvider(context)
        } else {
            UnwiredBillingProvider
        }
}

/** The release path before Play Billing is wired up: it refuses, loudly and honestly. */
private object UnwiredBillingProvider : BillingProvider {

    override val testMode = false

    private val message =
        "Purchases are not available in this build: Google Play Billing is not wired up yet."

    override suspend fun purchase(productId: String) = PurchaseResult.Failed(message)

    override suspend fun restore(productId: String) = PurchaseResult.Failed(message)
}
