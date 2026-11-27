package com.pnotes.core.stroke

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-of-stroke conformance: the flick is a conditional pen-up effect. The live ribbon remains
 * pressure-only; a fast, light release gets a short snake tail, while a slow or firm release does
 * not.
 */
class EndOfStrokeTest {

    @Test fun flick_fastLightReleaseGrowsANeedle() {
        val n = 40
        val xs = DoubleArray(n) { it * 2.4 }
        val ys = DoubleArray(n) { 0.0 }
        val ps = DoubleArray(n) { 0.2 }
        val ts = DoubleArray(n) { it * 2.0 }
        val built = StrokeEngine.build(
            xs, ys, ps, ts,
            baseWidth = 6.0, pressureEnabled = true, m = 0.35, ds = 0.0,
            smooth = true, finished = true, smoothScale = 1.0,
            endVelocitySensitivity = 0.4,
        )
        val body = built.hw(n / 2)
        val tip = built.hw(n - 1)
        assertTrue(
            "the tip eases to a needle: body=$body tip=$tip",
            tip < body * 0.85,
        )
        // The ease is gradual across the tail, not a notch at the last point.
        val midTail = built.hw(n - 5)
        assertTrue(
            "the ease spreads across the window: tail=$midTail tip=$tip",
            midTail > tip && midTail < body,
        )
    }

    @Test fun flick_slowReleaseDoesNotThin() {
        val n = 40
        val xs = DoubleArray(n) { it * 2.4 }
        val ys = DoubleArray(n) { 0.0 }
        val ps = DoubleArray(n) { 0.2 }
        val ts = DoubleArray(n) { it * 50.0 }
        val built = StrokeEngine.build(
            xs, ys, ps, ts,
            baseWidth = 6.0, pressureEnabled = true, m = 0.35, ds = 0.0,
            smooth = true, finished = true, smoothScale = 1.0,
            endVelocitySensitivity = 0.4,
        )
        assertEquals(built.hw(n / 2), built.hw(n - 1), 0.15)
    }

    @Test fun flick_windowClampsForShortStrokes() {
        // A 10px dash must not grow a full 28px needle: the window is a fraction of the stroke's
        // own length, so the body keeps a body's share and the tail stays proportionate.
        val n = 6
        val xs = DoubleArray(n) { it * 2.0 }
        val ys = DoubleArray(n) { 0.0 }
        val ps = DoubleArray(n) { 0.2 }
        val ts = DoubleArray(n) { it * 2.0 }
        val built = StrokeEngine.build(
            xs, ys, ps, ts,
            baseWidth = 6.0, pressureEnabled = true, m = 0.35, ds = 0.0,
            smooth = true, finished = true, smoothScale = 1.0,
            endVelocitySensitivity = 0.4,
        )
        // The head end keeps most of its body: the tail is a fraction, not the whole stroke.
        val head = built.hw(0)
        val tip = built.hw(n - 1)
        assertTrue(
            "a short dash keeps its body: head=$head tip=$tip",
            tip < head * 0.95,
        )
        assertTrue(
            "a short dash keeps a body's share: head=$head tip=$tip",
            tip > head * 0.2,
        )
    }

    @Test fun flick_liveRibbonDoesNotGrowBehindTheNib() {
        val n = 40
        val xs = DoubleArray(n)
        var x = 0.0
        for (i in 0 until n) {
            x += if (i >= n - 5) 12.0 else 2.4
            xs[i] = x
        }
        val ys = DoubleArray(n) { it * 0.5 }
        val ps = DoubleArray(n) { 0.2 }
        val ts = DoubleArray(n) { it * 2.0 }

        val ribbon = WetRibbon(
            baseWidth = 6.0, pressureEnabled = true, m = 0.35, ds = 0.0,
            speedStrength = 0.0, speedScale = 1.0, smooth = true, holdEnds = false,
            taperEnabled = false, taperMinFactor = 0.0, smoothScale = 1.0,
            endVelocitySensitivity = 0.4,
        )
        for (i in 0 until n) ribbon.append(xs[i], ys[i], ps[i], ts[i])
        val unfinished = StrokeEngine.build(
            xs, ys, ps, ts,
            baseWidth = 6.0, pressureEnabled = true, m = 0.35, ds = 0.0,
            smooth = true, finished = false, smoothScale = 1.0,
            endVelocitySensitivity = 0.4,
        )
        var worst = 0.0
        for (i in 0 until n) {
            worst = maxOf(worst, kotlin.math.abs(ribbon.hw(i) - unfinished.hw(i)))
        }
        assertTrue("live pressure-only tail drift: $worst", worst < 0.05)
        assertEquals(ribbon.hw(n / 2), ribbon.hw(n - 1), 0.15)
    }

    @Test fun flick_liveRibbonStaysNormalUntilLift() {
        val n = 40
        val xs = DoubleArray(n) { it * 2.4 }
        val ys = DoubleArray(n) { 0.0 }
        val ps = DoubleArray(n) { 0.2 }
        val ts = DoubleArray(n) { it * 2.0 }

        val ribbon = WetRibbon(
            baseWidth = 6.0, pressureEnabled = true, m = 0.35, ds = 0.0,
            speedStrength = 0.0, speedScale = 1.0, smooth = true, holdEnds = false,
            taperEnabled = false, taperMinFactor = 0.0, smoothScale = 1.0,
            endVelocitySensitivity = 0.4,
        )
        for (i in 0 until n) ribbon.append(xs[i], ys[i], ps[i], ts[i])
        val body = ribbon.hw(n / 2)
        val tip = ribbon.hw(n - 1)
        assertEquals("live tail must not taper", body, tip, 0.15)

        // The live ribbon is unchanged by the compatibility hook.
        val before = DoubleArray(n) { ribbon.hw(it) }
        ribbon.finishFlick()
        for (i in 0 until n) {
            assertEquals("lift changes nothing at $i", before[i], ribbon.hw(i), 0.0)
        }
    }
}
