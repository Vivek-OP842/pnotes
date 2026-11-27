package com.pnotes.premium

/**
 * What the free plan includes.
 *
 * These numbers are not invented: they are the free column of the Premium page's own comparison
 * table ("10 Notes", "2 Folders", "10 Templates", "Export — Watermarked"), which is the same copy
 * the Free plan card states. Pages read "Unlimited" on *both* plans there, so they are deliberately
 * not limited here.
 */
object FreePlan {

    const val NOTES = 10
    const val FOLDERS = 2
    const val TEMPLATES = 10

    /** Pages are unlimited on the free plan too, per the prototype's table. */
    const val PAGES = Int.MAX_VALUE
}

/** How much of one thing is left, and whether one more is still allowed. */
data class Allowance(
    val used: Int,
    val limit: Int,
) {
    val unlimited: Boolean get() = limit == Int.MAX_VALUE

    val remaining: Int get() = if (unlimited) Int.MAX_VALUE else (limit - used).coerceAtLeast(0)

    fun allowsOneMore(): Boolean = unlimited || used < limit
}

/**
 * The free/premium boundary, expressed as pure questions about a count.
 *
 * Kept separate from the UI and from storage on purpose: the notes themselves stay local and
 * offline (§14), and entitlement lives in the account store, so this only has to answer "may I make
 * one more?" without knowing either. Every call site must supply its own count, which keeps the
 * numbers honest — there is no cached total here to drift out of sync with the files on disk.
 */
object PremiumGate {

    fun notes(used: Int, premium: Boolean): Allowance = allowance(used, FreePlan.NOTES, premium)

    fun folders(used: Int, premium: Boolean): Allowance = allowance(used, FreePlan.FOLDERS, premium)

    fun templates(used: Int, premium: Boolean): Allowance =
        allowance(used, FreePlan.TEMPLATES, premium)

    /** Pages are the one thing the prototype leaves unlimited on both plans. */
    fun pages(used: Int, premium: Boolean): Allowance = allowance(used, FreePlan.PAGES, premium)

    /** Free exports carry the watermark the prototype's table calls out; premium lifts it. */
    fun exportsAreWatermarked(premium: Boolean): Boolean = !premium

    private fun allowance(used: Int, freeLimit: Int, premium: Boolean): Allowance =
        Allowance(used = used, limit = if (premium) Int.MAX_VALUE else freeLimit)
}
