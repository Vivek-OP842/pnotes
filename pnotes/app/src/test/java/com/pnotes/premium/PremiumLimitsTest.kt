package com.pnotes.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The free/premium boundary. These assertions are the acceptance criteria for gating, so the numbers
 * come from the prototype's comparison table rather than from whatever the code happens to do.
 */
class PremiumLimitsTest {

    @Test
    fun `free plan allows the tenth note and refuses the eleventh`() {
        assertTrue(PremiumGate.notes(used = 9, premium = false).allowsOneMore())
        assertFalse(PremiumGate.notes(used = 10, premium = false).allowsOneMore())
        // The boundary is inclusive of the last allowed item, not off by one in either direction.
        assertEquals(0, PremiumGate.notes(used = 10, premium = false).remaining)
    }

    @Test
    fun `free plan counts two folders and ten templates`() {
        assertTrue(PremiumGate.folders(used = 1, premium = false).allowsOneMore())
        assertFalse(PremiumGate.folders(used = 2, premium = false).allowsOneMore())
        assertTrue(PremiumGate.templates(used = 9, premium = false).allowsOneMore())
        assertFalse(PremiumGate.templates(used = 10, premium = false).allowsOneMore())
    }

    @Test
    fun `pages are unlimited on both plans`() {
        assertTrue(PremiumGate.pages(used = 9_999, premium = false).unlimited)
        assertTrue(PremiumGate.pages(used = 9_999, premium = false).allowsOneMore())
        assertTrue(PremiumGate.pages(used = 9_999, premium = true).allowsOneMore())
    }

    @Test
    fun `premium lifts every limit`() {
        val heavilyUsed = 100_000
        for (allowance in listOf(
            PremiumGate.notes(heavilyUsed, premium = true),
            PremiumGate.folders(heavilyUsed, premium = true),
            PremiumGate.templates(heavilyUsed, premium = true),
        )) {
            assertTrue(allowance.allowsOneMore())
            assertTrue(allowance.unlimited)
        }
    }

    @Test
    fun `watermark is the free plan's and is lifted by premium`() {
        assertTrue(PremiumGate.exportsAreWatermarked(premium = false))
        assertFalse(PremiumGate.exportsAreWatermarked(premium = true))
    }

    @Test
    fun `remaining never goes negative when a limit is already exceeded`() {
        // An existing library can be over the line — e.g. files created before a limit existed.
        assertEquals(0, PremiumGate.notes(used = 25, premium = false).remaining)
        assertFalse(PremiumGate.notes(used = 25, premium = false).allowsOneMore())
    }
}
