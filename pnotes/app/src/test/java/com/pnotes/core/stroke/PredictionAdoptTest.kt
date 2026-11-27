package com.pnotes.core.stroke

import com.pnotes.core.geometry.Pt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pen-up adoption of the predictor's look-ahead tail.
 *
 * The rule the whole path exists to keep: **the tail the writer watched is the tail that lands,
 * point for point.** A tail that is drawn and then shortened on the way into the model is ink
 * that disappears at the exact moment the pen comes up, which is the one thing a look-ahead must
 * never cause.
 */
class PredictionAdoptTest {

    private fun tail(vararg xy: Pair<Double, Double>): List<Pt> = xy.map { Pt(it.first, it.second) }

    // --- trim ---

    @Test fun nothingToTrimFromAnEmptyTail() {
        assertTrue(PredictionAdopt.trim(emptyList(), 0.0, 0.0).isEmpty())
    }

    @Test fun aTailIsTrimmedToThePointsThatAreProgress() {
        // The anchor is where the tail grows out of; a point on it is the anchor again, not progress.
        val got = PredictionAdopt.trim(
            tail(0.0 to 0.0, 5.0 to 0.0, 10.0 to 0.0),
            anchorX = 0.0, anchorY = 0.0,
        )
        assertEquals(2, got.size)
        assertEquals(5.0, got[0].x, 1e-12)
        assertEquals(10.0, got[1].x, 1e-12)
    }

    @Test fun repeatedPointsAreSkippedNotRepeated() {
        val got = PredictionAdopt.trim(
            tail(5.0 to 0.0, 5.0 to 0.0, 10.0 to 0.0),
            anchorX = 0.0, anchorY = 0.0,
        )
        assertEquals(2, got.size)
        assertEquals(10.0, got.last().x, 1e-12)
    }

    @Test fun aTailThatRunsAwayIsCappedAtTheTrimDistance() {
        val got = PredictionAdopt.trim(
            tail(5.0 to 0.0, 10.0 to 0.0, 90.0 to 0.0),
            anchorX = 0.0, anchorY = 0.0,
            maxDist = 20.0,
        )
        assertEquals(2, got.size)
        assertEquals(10.0, got.last().x, 1e-12)
    }

    @Test fun aNonFinitePointIsDropped() {
        val got = PredictionAdopt.trim(
            listOf(Pt(5.0, 0.0), Pt(Double.NaN, 1.0), Pt(9.0, 0.0)),
            anchorX = 0.0, anchorY = 0.0,
        )
        assertEquals(2, got.size)
        assertEquals(9.0, got.last().x, 1e-12)
    }

    @Test fun trimmingIsIdempotentSoTheRendererAndTheAdopterCanBothRunIt() {
        val once = PredictionAdopt.trim(
            tail(0.5 to 0.0, 5.0 to 0.0, 10.0 to 0.0),
            anchorX = 0.0, anchorY = 0.0,
        )
        val twice = PredictionAdopt.trim(once, anchorX = 0.0, anchorY = 0.0)
        assertEquals(once, twice)
    }

    // --- adopt ---

    @Test fun nothingToAdoptFromAnEmptyTail() {
        assertTrue(PredictionAdopt.adopt(emptyList(), 1.0, 0.0).isEmpty())
    }

    @Test fun theTailIsAdoptedWhole() {
        val got = PredictionAdopt.adopt(tail(4.0 to 0.0, 8.0 to 0.0, 12.0 to 0.0), pressure = 0.5, lastT = 100.0)
        assertEquals(3, got.size)
        assertEquals(4.0, got[0].x, 1e-12)
        assertEquals(12.0, got[2].x, 1e-12)
        assertTrue("pressure was not carried", got.all { it.pressure == 0.5 })
    }

    @Test fun aTailBehindThePenUpPointIsStillAdopted() {
        // The writer saw this ink on screen, so it lands. Dropping it here is how a stroke loses
        // its last few pixels at the moment the pen comes up.
        val got = PredictionAdopt.adopt(tail(-1.0 to 0.0, -2.5 to 0.0), pressure = 1.0, lastT = 0.0)
        assertEquals(2, got.size)
        assertEquals(-2.5, got.last().x, 1e-12)
    }

    @Test fun aTinyTailIsAdoptedToo() {
        // A 0.5 mm look-ahead is still ink the writer watched.
        val got = PredictionAdopt.adopt(tail(0.4 to 0.0), pressure = 1.0, lastT = 0.0)
        assertEquals(1, got.size)
        assertEquals(0.4, got.single().x, 1e-12)
    }

    @Test fun timesRunForwardFromTheLastRealSample() {
        val got = PredictionAdopt.adopt(tail(4.0 to 0.0, 8.0 to 0.0, 12.0 to 0.0, 16.0 to 0.0), pressure = 1.0, lastT = 250.0)
        assertTrue("first adopted sample must be after the last real one", got[0].t > 250.0)
        for (i in 1 until got.size) {
            assertTrue("time went backwards at $i", got[i].t > got[i - 1].t)
        }
    }

    @Test fun pressureIsClampedIntoRange() {
        val high = PredictionAdopt.adopt(tail(5.0 to 0.0), pressure = 4.0, lastT = 0.0)
        val low = PredictionAdopt.adopt(tail(5.0 to 0.0), pressure = -3.0, lastT = 0.0)
        assertEquals(1.0, high.single().pressure, 1e-12)
        assertEquals(0.0, low.single().pressure, 1e-12)
    }
}
