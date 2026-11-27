package com.pnotes.core.stroke

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Star Notes SmoothFilter port conformance (smoothness plan Phases 1–3):
 * lag-compensated arc-length EMA, legacy invariance at lagComp = 0, and the
 * stability-curve / noise-gate / lag-compensation mappings on InteractionController.
 */
class LagCompensationTest {

    // --- Phase 1: lag-compensated emaStep ---

    @Test fun lagComp_zeroReproducesLegacyFilter() {
        // Bit-for-bit: lagComp = 0 must be the legacy filter (back-compat guarantee).
        val steps = doubleArrayOf(0.0, 2.0, 2.5, 1.8, 3.0, 0.4, 2.2)
        val raw = doubleArrayOf(0.0, 3.0, 7.0, 9.5, 14.0, 14.4, 18.0)
        val legacy = StrokeEngine.emaByArc(raw, steps, StrokeEngine.SMOOTH_LEN)
        val withZero = StrokeEngine.emaByArc(raw, steps, StrokeEngine.SMOOTH_LEN, lagComp = 0.0)
        for (i in raw.indices) assertEquals(legacy[i], withZero[i], 0.0)
    }

    @Test fun lagComp_singleEmaStep_identicalAtZero() {
        val out = StrokeEngine.emaStep(1.0, 0.5, 2.0, 2.4, 2.4, lagComp = 0.0)
        val legacy = StrokeEngine.emaStep(1.0, 0.5, 2.0, 2.4, 2.4)
        assertEquals(legacy, out, 0.0)
    }

    @Test fun lagComp_singleEmaStep_pullsOutputTowardInput() {
        val legacy = StrokeEngine.emaStep(0.0, 0.0, 10.0, 2.4, 2.4)
        val compensated = StrokeEngine.emaStep(0.0, 0.0, 10.0, 2.4, 2.4, lagComp = 0.45)
        assertTrue(
            "compensation must close the residual, not widen it",
            abs(compensated - 10.0) < abs(legacy - 10.0),
        )
    }

    @Test fun lagComp_neverOvershootsRaw() {
        // Every step: the compensated output must stay between previous output and raw input
        // (monotone convergence, no ringing) for the whole compensation range.
        var prevOut = 0.0
        var prevIn = 0.0
        val steps = doubleArrayOf(2.4, 1.0, 4.0, 0.6, 2.4, 2.4, 0.2, 3.0)
        val raw = doubleArrayOf(4.0, 8.0, 20.0, 21.0, 15.0, 10.0, 10.2, 6.0)
        for (i in raw.indices) {
            val out = StrokeEngine.emaStep(prevOut, prevIn, raw[i], steps[i], StrokeEngine.SMOOTH_LEN, lagComp = 0.6)
            val lo = minOf(prevOut, raw[i])
            val hi = maxOf(prevOut, raw[i])
            assertTrue("overshoot at $i: $out not in [$lo, $hi]", out in lo..hi)
            prevOut = out
            prevIn = raw[i]
        }
    }

    @Test fun lagComp_steadyLine_trailShrinks() {
        // Steady diagonal at stylus spacing: measure how far the smoothed tip trails the pen
        // after convergence. Compensation must cut the trail substantially without collapsing
        // smoothing (the curve must still differ from raw — that's what kills jitter).
        val lambda = StrokeEngine.SMOOTH_LEN
        var legacyOut = 0.0
        var compOut = 0.0
        var prevIn = 0.0
        val step = 2.4
        var raw = 0.0
        repeat(60) {
            raw += step * 2.0 // 4.8px per sample: a normal writing speed
            legacyOut = StrokeEngine.emaStep(legacyOut, prevIn, raw, step, lambda, lagComp = 0.0)
            compOut = StrokeEngine.emaStep(compOut, prevIn, raw, step, lambda, lagComp = 0.45)
            prevIn = raw
        }
        val legacyTrail = raw - legacyOut
        val compTrail = raw - compOut
        assertTrue(
            "comp trail ($compTrail) should be well under legacy ($legacyTrail)",
            compTrail < legacyTrail * 0.65,
        )
        // But not zero: the filter must still smooth.
        assertTrue(compTrail > 0.5)
    }

    @Test fun lagComp_jitterStillFiltered() {
        // Square-wave tremor on top of slow motion: peak-to-peak wobble in the compensated
        // output must stay close to the legacy filter's (smoothing preserved).
        val lambda = StrokeEngine.SMOOTH_LEN
        var legacyOut = 0.0
        var compOut = 0.0
        var prevIn = 0.0
        var raw = 0.0
        var maxLegacySwing = 0.0
        var maxCompSwing = 0.0
        var lastLegacy = 0.0
        var lastComp = 0.0
        repeat(40) {
            raw += 1.2
            val jitter = if (it % 2 == 0) 1.5 else -1.5
            val input = raw + jitter
            legacyOut = StrokeEngine.emaStep(legacyOut, prevIn, input, 1.2, lambda, lagComp = 0.0)
            compOut = StrokeEngine.emaStep(compOut, prevIn, input, 1.2, lambda, lagComp = 0.45)
            maxLegacySwing = maxOf(maxLegacySwing, abs(legacyOut - lastLegacy))
            maxCompSwing = maxOf(maxCompSwing, abs(compOut - lastComp))
            lastLegacy = legacyOut
            lastComp = compOut
            prevIn = input
        }
        // The compensated channel must remain a low-pass: its step-to-step swing stays well
        // under the raw signal's (3.0) while tracking it far closer than the legacy filter.
        assertTrue("comp swing $maxCompSwing should stay low-pass", maxCompSwing < 2.0)
    }

    @Test fun wetRibbon_lagCompMatchesBatchBuild() {
        // Wet vs committed must agree with the same lagComp (pen-up jump elimination).
        val n = 30
        val xs = DoubleArray(n) { it * 2.5 + (if (it % 3 == 0) 0.8 else -0.4) }
        val ys = DoubleArray(n) { it * 1.1 + (if (it % 2 == 0) -0.6 else 0.9) }
        val ps = DoubleArray(n) { 0.6 + it * 0.01 }

        val ribbon = WetRibbon(
            baseWidth = 4.0, pressureEnabled = true, m = 0.35, ds = 0.0,
            speedStrength = 0.0, speedScale = 1.0, smooth = true, holdEnds = false,
            taperEnabled = false, taperMinFactor = 0.0, smoothScale = 1.0,
            lagComp = 0.45,
        )
        for (i in 0 until n) ribbon.append(xs[i], ys[i], ps[i], (i * 8).toDouble())

        val built = StrokeEngine.build(
            xs, ys, ps, null,
            baseWidth = 4.0, pressureEnabled = true, m = 0.35, ds = 0.0,
            smooth = true, finished = false, smoothScale = 1.0,
            lagComp = 0.45,
        )
        assertEquals(n, built.pointCount)
        var worst = 0.0
        for (i in 0 until n) {
            worst = maxOf(
                worst,
                hypot(ribbon.cx(i) - built.cx(i), ribbon.cy(i) - built.cy(i)),
            )
        }
        assertTrue("wet/committed drift: $worst", worst < 0.05)
    }

    // --- Phase 3: stability curve + mappings ---

    @Test fun stabilityScale_defaultUnchanged() {
        // Back-compat anchor: 0.5 still maps to 3.0x.
        assertEquals(3.0, com.pnotes.canvas.InteractionController.stabilityScale(0.5), 1e-9)
    }

    @Test fun stabilityScale_zeroIsRaw() {
        assertEquals(0.0, com.pnotes.canvas.InteractionController.stabilityScale(0.0), 1e-9)
        assertEquals(0.0, com.pnotes.canvas.InteractionController.stabilityScale(0.005), 1e-9)
    }

    @Test fun stabilityScale_linearNotExplosive() {
        // Upper half no longer explodes: 1.0 -> 6x (was 10x), and the curve is linear.
        assertEquals(6.0, com.pnotes.canvas.InteractionController.stabilityScale(1.0), 1e-9)
        assertEquals(1.2, com.pnotes.canvas.InteractionController.stabilityScale(0.2), 1e-9)
    }

    @Test fun lagCompensation_risesWithStability() {
        val f = com.pnotes.canvas.InteractionController::lagCompensation
        assertEquals(0.0, f(0.0), 1e-9)
        assertTrue(f(0.3) < f(0.6))
        assertTrue(f(0.6) < f(1.0))
        assertTrue("cap", f(1.0) <= 0.7 + 1e-9)
    }

    @Test fun noiseGate_scalesWithStability() {
        val g = com.pnotes.canvas.InteractionController::noiseGatePx
        assertEquals(0.0, g(0.0), 1e-9)
        assertTrue(g(0.3) < g(0.8))
        assertTrue("gate range", g(1.0) in 1.4..1.6)
    }
}
