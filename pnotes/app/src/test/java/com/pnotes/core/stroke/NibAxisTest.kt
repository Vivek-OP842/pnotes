package com.pnotes.core.stroke

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.hypot

/**
 * The nib axis a stylus's tilt and azimuth name. The load-bearing cases are the two that decide
 * whether ink changes at all: an orientation that cannot be read (nothing changes), and the natural
 * hold at azimuth 0 (which has to come out as the vertical axis the direction term was always
 * measured on, or every calligraphy stroke drawn with a tilted stylus would change).
 */
class NibAxisTest {

    @Test fun anUprightStylusHasNoAxisToRead() {
        // Below the threshold the barrel's screen projection has no direction left in it, and the
        // caller falls back to the fixed vertical.
        assertNull(NibAxis.of(0.0, 1.2))
        assertNull(NibAxis.of(NibAxis.MIN_TILT - 1e-3, 1.2))
        assertNull(NibAxis.of(Double.NaN, 1.2))
    }

    @Test fun theNaturalHoldNamesTheVerticalAxis() {
        // Azimuth 0 is the barrel leaning toward the writer, and that is where the fixed vertical axis
        // came from: the broad face on downstrokes, the thin one on upstrokes.
        val axis = NibAxis.of(0.7, 0.0)!!
        assertEquals(NibAxis.DEFAULT_X, axis.x, 1e-12)
        assertEquals(NibAxis.DEFAULT_Y, axis.y, 1e-12)
    }

    @Test fun turningThePenTurnsTheNib() {
        val quarter = NibAxis.of(0.7, PI / 2)!!
        assertEquals(1.0, quarter.x, 1e-12)
        assertEquals(0.0, quarter.y, 1e-12)
        // Half a turn puts the broad face on upstrokes instead — the mirror of the default.
        val half = NibAxis.of(0.7, PI)!!
        assertEquals(0.0, half.x, 1e-12)
        assertEquals(-1.0, half.y, 1e-12)
    }

    @Test fun theAxisIsAlwaysAUnitVector() {
        for (deg in -180..180 step 7) {
            for (tilt in listOf(0.2, 0.6, 1.2, 1.5)) {
                val axis = NibAxis.of(tilt, deg * PI / 180.0)!!
                assertEquals("tilt $tilt at $deg°", 1.0, hypot(axis.x, axis.y), 1e-12)
            }
        }
    }

    @Test fun theGrindTurnsTheFaceOffTheLean() {
        // A square-cut nib (the default) squares the face to the lean; an oblique is ground a few
        // degrees off it, which is the only part of the axis a stylus cannot report.
        val cut = -0.35
        for (a in listOf(0.0, 0.7, 2.2, -1.4)) {
            // Grinding the nib by `cut` and turning the wrist by `cut` name the same axis, so the cut
            // is a rotation of the one thing rather than a second, separate effect.
            val ground = NibAxis.of(0.7, a, cut)!!
            val wrist = NibAxis.of(0.7, a + cut)!!
            assertEquals("cut $cut at azimuth $a, x", wrist.x, ground.x, 1e-12)
            assertEquals("cut $cut at azimuth $a, y", wrist.y, ground.y, 1e-12)
            // And the square grind is exactly what the stylus alone names: nobody who leaves the
            // setting alone sees a single number of their ink move.
            val square = NibAxis.of(0.7, a)!!
            assertEquals("square at azimuth $a, x", square.x, NibAxis.of(0.7, a, NibAxis.CUT)!!.x, 0.0)
            assertEquals("square at azimuth $a, y", square.y, NibAxis.of(0.7, a, NibAxis.CUT)!!.y, 0.0)
        }
        // A quarter turn of the grind swaps the face onto the axis the lean is across.
        val quarter = NibAxis.of(0.7, 0.3, PI / 2)!!
        val straight = NibAxis.of(0.7, 0.3)!!
        assertEquals(straight.y, quarter.x, 1e-12)
        assertEquals(-straight.x, quarter.y, 1e-12)
    }

    @Test fun aGrindPastHalfATurnIsClampedAndGarbageReadsAsSquare() {
        val most = NibAxis.of(0.7, 0.3, NibAxis.MAX_CUT)!!
        assertEquals(most.x, NibAxis.of(0.7, 0.3, 9.0)!!.x, 1e-12)
        assertEquals(most.y, NibAxis.of(0.7, 0.3, 9.0)!!.y, 1e-12)
        val least = NibAxis.of(0.7, 0.3, -NibAxis.MAX_CUT)!!
        assertEquals(least.x, NibAxis.of(0.7, 0.3, -9.0)!!.x, 1e-12)
        assertEquals(least.y, NibAxis.of(0.7, 0.3, -9.0)!!.y, 1e-12)
        // A cut that is not a number cannot turn a nib, so it reads as the square grind.
        val square = NibAxis.of(0.7, 0.3)!!
        assertEquals(square.x, NibAxis.of(0.7, 0.3, Double.NaN)!!.x, 0.0)
        assertEquals(square.y, NibAxis.of(0.7, 0.3, Double.POSITIVE_INFINITY)!!.y, 0.0)
    }

    @Test fun aGrindNeedsAnOrientationToTurn() {
        // With no usable tilt the axis is the page's, and the page does not have a grind: there is no
        // second axis to rotate, so the cut is inert rather than quietly turning the vertical.
        assertNull(NibAxis.of(0.0, 1.2, 0.6))
        assertNull(NibAxis.of(NibAxis.MIN_TILT - 1e-3, 1.2, 0.6))
    }

    @Test fun theAxisIsPerSampleNotPerStroke() {
        // Two samples of the same stroke with different orientations name different axes: the model
        // reads the axis once, at pen-down, so this is about the mapping being a pure function of the
        // pair rather than remembering anything.
        val a = NibAxis.of(0.6, 0.3)!!
        val b = NibAxis.of(0.6, 0.3)!!
        assertEquals(a.x, b.x, 0.0)
        assertEquals(a.y, b.y, 0.0)
        assertTrue(NibAxis.of(0.6, 0.9)!!.x != a.x)
    }
}
