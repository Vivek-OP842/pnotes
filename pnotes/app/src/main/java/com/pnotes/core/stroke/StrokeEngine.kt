package com.pnotes.core.stroke

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Turns raw stylus samples into a smooth, variable-width ink ribbon (spec 03).
 * Pure, deterministic and unit-tested against the spec's conformance vectors.
 * All math runs in doubles; only the packed [StrokeGeometry] output is floats.
 */
object StrokeEngine {
    /** EMA low-pass smoothing factor (spec 03 conformance). */
    const val ALPHA = 0.50

    /** Below this difference length a sample is degenerate; reuse last tangent. */
    const val MIN_TANGENT_LEN = 1e-6

    /** Below this travel a step carries no arc for [emaByArc] to integrate over. */
    const val MIN_STEP = 1e-9

    /** Floor on the calligraphic direction term so width stays positive. */
    const val MIN_DIRECTION = 0.1

    /** Steepness of the pressure response S-curve (see [logisticEase]). */
    const val PRESSURE_CURVE_K = 8.0

    /** Sample spacing (content px) the smoothing lengths below are tuned at (Star Notes reference). */
    const val REFERENCE_SPACING = 2.4

    /** Low-pass length (content px) for position: filters out digitizer micro-jitter. */
    val SMOOTH_LEN = REFERENCE_SPACING * (1.0 - ALPHA) / ALPHA

    /** Low-pass length (content px) for pressure: calibrated for buttery, instant thick-to-thin response without lag (Star Notes reference). */
    const val PRESSURE_SMOOTH_LEN = 1.8

    /** Calligraphy pen: the travel that decides the stroke's **head**, the one stretch no causal
     *  rule can judge, because at pen-down the samples that would tell a jitter from a genuinely
     *  broad opening have not arrived yet. Every sample inside it is drawn at the direction of the
     *  chord across it (see [headDirection]). Long enough to outvote a pen-down flick, short enough
     *  that a stroke which really starts broad opens broad. It is the dot threshold too: a finished
     *  stroke that never fills the window is a tap. The tail borrows it as well, as the travel a
     *  widening has to hold before the pen lifts to count (see [capTail]). */
    const val HEAD_LEN = 8.0

    /** Calligraphy pen: the travel the nib takes to widen across its whole range, thin face to broad
     *  (see [nibDirection]). A rate, not a threshold: a stray sample only buys its own fraction of
     *  it, while a real downstroke reaches full width within a letter's height. */
    const val OPEN_LEN = 8.0

    /** [OPEN_LEN]'s counterpart for thinning, so the two directions can be tuned apart: ink that
     *  swells too eagerly reads as a blot, ink that thins too slowly reads as a smear. */
    const val CLOSE_LEN = 8.0

    /** The nib's arc constants are hand gestures, so they are quoted at 100% zoom and scaled by the
     *  stroke's [smoothScale] to the page. Without it a nib drawn at 4x had to be dragged four times
     *  as far across the glass before it would thicken, since the page it was writing on was a
     *  quarter the size. */
    fun headLen(smoothScale: Double): Double = HEAD_LEN * max(smoothScale, 0.0)

    /** [OPEN_LEN] at the stroke's draw zoom; see [headLen]. */
    fun openLen(smoothScale: Double): Double = OPEN_LEN * max(smoothScale, 0.0)

    /** [CLOSE_LEN] at the stroke's draw zoom; see [headLen]. */
    fun closeLen(smoothScale: Double): Double = CLOSE_LEN * max(smoothScale, 0.0)

    /** Calligraphy pen: the direction-y a dot is built at. Past the broad face's 1.0 on purpose,
     *  so a dot lands slightly bigger than the thickest line and reads as a deliberate mark. */
    const val DOT_DIR_Y = 1.5

    /** Speed pen: dp/ms at/below which the line stays full width, and the speed
     *  at/above which it reaches its thinnest (0 and ≈3.75 in/s of hand travel).
     *  Measuring in dp — not page pixels — makes the effect independent of both zoom
     *  and screen density; see [speedFactors] and the per-stroke speed scale. */
    const val SPEED_LO = 0.0
    const val SPEED_HI = 0.6

    /** Speed pen: half the duration (ms) of the centred window the nib's speed is measured over.
     *  Speed is the arc length covered across `±this` ms divided by that span. A fixed *time*
     *  base (not a fixed sample count, which collapses to a point where the pen crawls and the
     *  distance-gated samples bunch up) keeps the estimate steady and lets the faster ink on
     *  either side of a brief corner pause dilute it, instead of the width ballooning into a
     *  blob there. The window slides inward at the stroke's ends so its first and last points
     *  still average a full span rather than the at-rest tip. */
    const val SPEED_WINDOW_MS = 40.0

    /** Speed pen: minimum per-segment dt (ms) so a duplicate-timestamp pair can't
     *  divide by ~zero and spike the speed. */
    const val MIN_DT = 1.0

    /** Taper pen: strokes shorter than this arc are left un-tapered, so a quick tick doesn't
     *  collapse to nothing. Quoted at 100% zoom and scaled by the stroke's draw zoom, since what
     *  makes a tick a tick is how far the hand went, not how much page it landed on. */
    const val TAPER_MIN_LEN = 8.0

    /** Taper pen: the arc (content px, quoted at 100% zoom and scaled by the stroke's draw zoom) over
     *  which the nib eases from full width down to `taperMinFactor` at the tip. A rate of hand travel
     *  rather than a fraction of the stroke, which is what makes the pen's profile the same on a
     *  letter and on a ruled line — a long stroke keeps its body instead of thinning across its whole
     *  back half — and, the reason it has to be absolute, why the pen can be grown: a point's distance
     *  from the nib only ever grows, so ink farther back than this window is final ([WetRibbon]). */
    const val TAPER_TAIL_LEN = 24.0

    /** Pens that hold their ends ([holdEndPressure]) do so over this many samples at each end,
     *  enough to cover the EMA pressure ramp so the swept end disc meets the line at the body width. */
    const val CAP_HOLD_SAMPLES = 4

    /** Taper falloff shape: the tail ease is the [logisticEase] sigmoid clipped to its
     *  `[TAPER_TAIL, 1 - TAPER_TAIL]` band, since a true sigmoid only reaches 0 and 1 at +/-inf;
     *  the clipped band is then stretched back to a real point and full width. A smaller tail
     *  hugs the rails harder: a longer thin hold near the tip, then a quicker opening, than the
     *  old cubic smoothstep. [TAPER_CURVE_K] is the logistic steepness that spans exactly that
     *  band (sigma(+-k/2) = 1 - TAPER_TAIL / TAPER_TAIL). */
    const val TAPER_TAIL = 0.01
    val TAPER_CURVE_K = 2.0 * ln((1.0 - TAPER_TAIL) / TAPER_TAIL)

    /** The regular pen and highlighter hold their ends to the body width so the swept end discs
     *  meet the line at full width (no thin-end pinch from light pen-down/pen-up pressure).
     *  Replaces the first/last [CAP_HOLD_SAMPLES] with the body floor if they fall below it. */
    private fun holdEndPressure(p: DoubleArray) {
        val n = p.size
        val w = min(CAP_HOLD_SAMPLES, (n - 1) / 2)
        if (w < 1) return
        val headFloor = p[w]
        for (i in 0 until w) if (p[i] < headFloor) p[i] = headFloor
        val tailFloor = p[n - 1 - w]
        for (i in n - w until n) if (p[i] < tailFloor) p[i] = tailFloor
    }

    /** One-pole IIR low-pass (exponential moving average). */
    fun ema(values: DoubleArray, alpha: Double = ALPHA): DoubleArray {
        if (values.isEmpty()) return values
        val out = DoubleArray(values.size)
        out[0] = values[0]
        for (i in 1 until values.size) {
            out[i] = alpha * values[i] + (1 - alpha) * out[i - 1]
        }
        return out
    }

    /** [ema] over a boxed list — the spec-vector form; [build] runs on the array one. */
    fun ema(values: List<Double>, alpha: Double = ALPHA): List<Double> =
        ema(values.toDoubleArray(), alpha).asList()

    /**
     * The same one-pole low-pass as [ema], measured in travel rather than in samples: the
     * continuous filter `dy/ds = (x(s) - y) / lambda` over arc length `s`, integrated exactly
     * across each step with the input read as a straight line between the two samples. [steps]
     * holds each sample's distance from the one before it; `steps[0]` is unused.
     *
     * This is what lets a committed stroke match the wet one. Pen-up sample reduction
     * ([StrokeSimplify]) changes the spacing, and a fixed per-sample factor turns that into a
     * different curve: its lag is one sample spacing, so a thinned stroke trails less and cuts its
     * corners deeper than the one that was drawn. Here the lag is [lambda] whatever the spacing,
     * so dropping a sample leaves the curve where it was. It also makes the ink independent of the
     * pen's report rate, and of how fast the hand was moving when the samples were spaced out.
     *
     * Reading the input as a line across the step rather than as a constant is what buys the last
     * of it. A constant would hold each sample over the whole step it ends, which biases the lag by
     * half a spacing, and half a spacing is exactly the quantity the reduction changes.
     */
    fun emaByArc(values: DoubleArray, steps: DoubleArray, lambda: Double, lagComp: Double = 0.0): DoubleArray {
        if (values.isEmpty()) return values
        if (lambda <= 0.0) return values.copyOf() // no smoothing length: pass the samples through
        val out = DoubleArray(values.size)
        out[0] = values[0]
        for (i in 1 until values.size) {
            out[i] = emaStep(out[i - 1], values[i - 1], values[i], steps[i], lambda, lagComp)
        }
        return out
    }

    /**
     * Position-specific arc-EMA with Curvature-Adaptive Corner Preservation:
     * When the writer executes a sharp corner or U-turn (direction reversal), the filter
     * automatically relaxes smoothing so the apex of the U-turn reaches 100% of the true pen position
     * and is never cut short or shrunk.
     */
    fun emaPositionByArc(
        rawX: DoubleArray,
        rawY: DoubleArray,
        steps: DoubleArray,
        lambda: Double,
        lagComp: Double = 0.0,
    ): Pair<DoubleArray, DoubleArray> {
        val n = rawX.size
        if (n == 0) return DoubleArray(0) to DoubleArray(0)
        if (lambda <= 0.0) return rawX.copyOf() to rawY.copyOf()
        val outX = DoubleArray(n)
        val outY = DoubleArray(n)
        outX[0] = rawX[0]
        outY[0] = rawY[0]
        for (i in 1 until n) {
            val curDx = rawX[i] - rawX[i - 1]
            val curDy = rawY[i] - rawY[i - 1]
            val prevDx = if (i > 1) rawX[i - 1] - rawX[i - 2] else curDx
            val prevDy = if (i > 1) rawY[i - 1] - rawY[i - 2] else curDy
            val prevLen = hypot(prevDx, prevDy)
            val curLen = hypot(curDx, curDy)
            val dot = if (prevLen > 1e-4 && curLen > 1e-4) {
                (prevDx * curDx + prevDy * curDy) / (prevLen * curLen)
            } else 1.0

            // Layer 2: 1€ Kinematic Curvature Adaptive Filter (Casiez et al. / Apple PencilKit model):
            // On cursive peaks / inflections (e.g. "mm", "ww", loops):
            // As deflection increases (dot drops below 0.75), smoothing length drops to 0.0,
            // locking the smoothed coordinate 100% to the exact physical apex of the letter without shortcutting.
            // On smooth straight strokes (dot ~ 1.0): Full silky-smooth filtering.
            val cornerFactor = if (dot < 0.75) {
                ((dot + 0.25) / 1.0).coerceIn(0.0, 1.0).let { it * it }
            } else {
                1.0
            }
            val effectiveLambda = lambda * cornerFactor

            outX[i] = if (effectiveLambda > 0.0) emaStep(outX[i - 1], rawX[i - 1], rawX[i], steps[i], effectiveLambda, lagComp) else rawX[i]
            outY[i] = if (effectiveLambda > 0.0) emaStep(outY[i - 1], rawY[i - 1], rawY[i], steps[i], effectiveLambda, lagComp) else rawY[i]
        }
        return outX to outY
    }

    /**
     * One step of [emaByArc]: the filter carried across a step of [d] travel. The whole filter is
     * this line applied in order, which is what lets [WetRibbon] extend it a sample at a time.
     *
     * [lagComp] is the Star Notes `SmoothFilter` lag-compensation term (NeoPen port, Phase 1 of
     * the smoothness plan): a causal low-pass always trails the pen by roughly one smoothing
     * length, which reads as a rubber-band between ink and nib. The fraction [lagComp] of the
     * filter's own residual `(curIn - prevOut)` is folded back into the output, pulling the tip
     * onto the true pen position without un-smoothing the curve. 0.0 (the default) reproduces the
     * legacy filter bit-for-bit; values around 0.35–0.6 match Star Notes' feel. The residual is
     * scaled by the same one-step decay the filter itself uses, so the compensation — like the
     * filter — is arc-length based: it is independent of report rate and of zoom.
     */
    internal fun emaStep(
        prevOut: Double,
        prevIn: Double,
        curIn: Double,
        d: Double,
        lambda: Double,
        lagComp: Double = 0.0,
    ): Double {
        if (lambda <= 0.0) return curIn
        if (d <= MIN_STEP) return prevOut // the pen did not move: no arc to integrate over
        val decay = exp(-d / lambda)
        val slope = (curIn - prevIn) * lambda * (1.0 - decay) / d
        val out = decay * prevOut + curIn - decay * prevIn - slope
        if (lagComp <= 0.0) return out
        // Fold a decaying slice of the residual back in. The (1 - decay) weight means a long
        // step (fast motion) is corrected almost fully within the step, and a short step (slow,
        // jittery motion) barely at all — exactly where the raw sample is least trustworthy.
        return out + (curIn - out) * (1.0 - decay) * lagComp
    }

    /**
     * Half-width at a point (spec 03 §3.2 with Star Notes Directional Chisel Projection).
     *
     * Given smoothed [pressure] in `[0, 1]` and the directional factor [ty] in `[-1, 1]`:
     * When [ds] > 0 (calligraphy), [ty] represents the projected blade factor or angle.
     * ```
     *   w = baseWidth · (m + (1 − m) · pressure) · max(1 + ds · ty, MIN_DIRECTION)
     * ```
     * Returns half the width (the radius of the swept brush disc).
     */
    fun halfWidth(
        baseWidth: Double,
        pressureEnabled: Boolean,
        m: Double,
        ds: Double,
        pressure: Double,
        ty: Double,
    ): Double {
        val p = if (pressureEnabled) pressure.coerceIn(0.0, 1.0) else 1.0
        val pFactor = if (pressureEnabled) m + (1.0 - m) * p else 1.0
        val direction = max(1.0 + ds * ty, MIN_DIRECTION)
        return baseWidth * pFactor * direction / 2.0
    }

    /**
     * Normalized logistic S-curve on `[0, 1]`, centred at 0.5 and rescaled so the endpoints
     * are exact (`0 -> 0`, `1 -> 1`) while only the middle bends. [k] sets the steepness: the
     * curve spans the logistic's `[sigma(-k/2), sigma(k/2)]` band, so a larger [k] both steepens
     * the middle and clips the rails nearer 0 and 1. `k <= 0` is the identity (a linear ramp).
     * Shared by the pressure response ([PRESSURE_CURVE_K]) and the taper ease ([TAPER_CURVE_K]).
     */
    fun logisticEase(x: Double, k: Double): Double {
        if (k <= 0.0) return x
        val lo = 1.0 / (1.0 + exp(k * 0.5))
        val hi = 1.0 / (1.0 + exp(-k * 0.5))
        val raw = 1.0 / (1.0 + exp(-k * (x - 0.5)))
        return (raw - lo) / (hi - lo)
    }

    /** Hermite smoothstep: 0 below [lo], 1 above [hi], an S-curve between. */
    internal fun smoothstep(lo: Double, hi: Double, x: Double): Double {
        if (hi <= lo) return if (x >= hi) 1.0 else 0.0
        val t = ((x - lo) / (hi - lo)).coerceIn(0.0, 1.0)
        return t * t * (3 - 2 * t)
    }

    /**
     * Per-point width multipliers in `[1 − speedStrength, 1]` for the **speed pen**:
     * the faster the nib travels across the page, the thinner the line (ink has less
     * time to lay down). Speed at point `i` is the **arc length of the raw samples over a
     * centred time window** of `±[SPEED_WINDOW_MS]` ms divided by that span, in dp/ms, where
     * [speedScale] (zoom ÷ density, captured at pen-down) converts page pixels to dp so the
     * effect is zoom- and device-independent. It reads the raw sample motion, not the smoothed
     * centerline, so the position low-pass can't compress the start or cut a corner short and
     * read a false slow-down there. Summing distance and time over a fixed *time* span (not a
     * fixed sample count) rejects per-sample jitter and keeps slow corners and ends from
     * collapsing the window onto themselves and ballooning the width. Returns all-`1.0` when off
     * or the samples carry no usable timing. [steps] is [build]'s raw per-sample travel.
     */
    fun speedFactors(
        samples: List<Sample>,
        steps: DoubleArray,
        speedStrength: Double,
        speedScale: Double,
    ): DoubleArray = speedFactors(
        DoubleArray(samples.size) { samples[it].t }, steps, speedStrength, speedScale,
    )

    /** [speedFactors] over the elapsed-ms channel alone; [times] holds one entry per sample. */
    fun speedFactors(
        times: DoubleArray,
        steps: DoubleArray,
        speedStrength: Double,
        speedScale: Double,
        velLo: Double = SPEED_LO,
        velHi: Double = SPEED_HI,
        velAmp: Double = 1.0,
    ): DoubleArray {
        val n = times.size
        val out = DoubleArray(n) { 1.0 }
        if (speedStrength <= 0.0 || n < 2) return out
        val t0 = times[0]
        val tN = times[n - 1]
        if (tN - t0 <= 0.0) return out
        val cum = DoubleArray(n)
        for (i in 1 until n) cum[i] = cum[i - 1] + steps[i]
        val cursor = IntArray(2)
        for (i in 0 until n) {
            out[i] = speedFactorAt(times, cum, n, i, cursor, speedStrength, speedScale, velLo, velHi, velAmp)
        }
        return out
    }

    /**
     * One point's speed width factor, and the body of [speedFactors]. [cum] is cumulative *raw*
     * travel and [cursor] holds the window's two sample indices, which only ever move forward, so
     * walking the points in order costs one pass over the stroke rather than a search per point.
     *
     * [WetRibbon] calls this directly for the handful of points still inside the window. Restarting
     * the scan from an older [cursor] is safe: both ends of the window only slide forward as the
     * stroke grows, so a stale cursor is behind the true one and the scan simply runs further.
     */
    internal fun speedFactorAt(
        times: DoubleArray,
        cum: DoubleArray,
        n: Int,
        i: Int,
        cursor: IntArray,
        speedStrength: Double,
        speedScale: Double,
        velLo: Double = SPEED_LO,
        velHi: Double = SPEED_HI,
        velAmp: Double = 1.0,
    ): Double {
        val t0 = times[0]
        val tN = times[n - 1]
        val half = SPEED_WINDOW_MS
        // Centre a fixed-duration window on this sample's time; if it runs past either end of
        // the stroke, slide it inward so the span stays ~2·half rather than shrinking to a point.
        var a = times[i] - half
        var b = times[i] + half
        if (a < t0) { b += t0 - a; a = t0 }
        if (b > tN) { a -= b - tN; b = tN; if (a < t0) a = t0 }
        var lo = cursor[0]
        var hi = cursor[1]
        while (lo < i && times[lo] < a) lo++
        while (hi < n - 1 && times[hi + 1] <= b) hi++
        cursor[0] = lo
        cursor[1] = hi
        // Always span at least one segment so a window that falls between two far-apart slow
        // samples reads a real speed instead of a zero-length divide.
        var l = lo
        var h = hi
        if (h <= l) { if (h < n - 1) h++ else l-- }
        val dist = (cum[h] - cum[l]) * speedScale
        val dt = max(times[h] - times[l], MIN_DT)
        val v = (dist / dt) * velAmp
        val loB = min(velLo, velHi)
        val hiB = max(velLo, velHi)
        return (1.0 - speedStrength * smoothstep(loB, hiB, v)).coerceIn(0.05, 1.0)
    }

    /**
     * Per-point width multipliers in `[taperMinFactor, 1]` for the **taper pen**: full width along
     * the body, easing down over the last [TAPER_TAIL_LEN] of arc to [taperMinFactor] of full at the
     * tip (a sharp point when that is 0). Returns all-`1.0` when the stroke is too short
     * ([TAPER_MIN_LEN]), where the whole stroke is a tick and a taper would erase it. [cum] is
     * cumulative arc along the smoothed centreline, from [build].
     */
    fun taperFactors(
        cum: DoubleArray,
        taperMinFactor: Double,
        smoothScale: Double = 1.0,
    ): DoubleArray {
        val n = cum.size
        val out = DoubleArray(n) { 1.0 }
        if (n < 2) return out
        val total = cum[n - 1]
        for (i in 0 until n) {
            out[i] = taperFactorAt(total - cum[i], total, taperMinFactor, smoothScale)
        }
        return out
    }

    /**
     * [taperFactors] read at one point: the width multiplier of a point [fromTip] of arc back from
     * the stroke's tip, on a stroke [total] of arc long. Shared with [WetRibbon], which carries it a
     * point at a time as the stroke grows — the tail window rides along with the nib, so the points
     * inside it are asked again on every sample while everything behind them is settled for good.
     */
    internal fun taperFactorAt(
        fromTip: Double,
        total: Double,
        taperMinFactor: Double,
        smoothScale: Double,
    ): Double {
        val tailLen = TAPER_TAIL_LEN * max(smoothScale, 0.0)
        if (tailLen <= 0.0) return 1.0
        if (total < TAPER_MIN_LEN * max(smoothScale, 0.0)) return 1.0
        val u = (fromTip / tailLen).coerceIn(0.0, 1.0)
        val ease = u * u * (3.0 - 2.0 * u)
        return taperMinFactor + (1.0 - taperMinFactor) * ease
    }

    /**
     * The calligraphy nib's **head**: the direction of the straight line from where the pen landed to
     * where the stroke first passes [headLen] of travel, paired with the last sample inside that
     * window. Every sample in the window takes that one value.
     *
     * A chord and not a statistic over the samples, because it asks the only question the head has to
     * answer, which way the stroke actually went, and answers it in the units that matter. A wobble
     * at pen-down displaces the far end by exactly its own half-pixel and no more. A per-sample rule
     * cannot do that: a tangent is only as trustworthy as the distance it was measured over, and at
     * pen-down the smoothed centreline is still catching up, so its first steps run a tenth as long
     * as the ones just after and their tangents are mostly digitiser noise.
     *
     * What it gives up is the deliberate flick that goes out and comes back. The chord reads only the
     * net displacement, so a pen-down flick longer than half the window leaves a net heading its own
     * way and opens broader than it should. Under half the window, which is the ordinary case, the
     * run that follows still wins the net.
     *
     * Until the window fills there is no answer, so a live stroke draws the safe value (thin) and is
     * rewritten once, when it fills. A *finished* stroke that never fills it is a dot and takes
     * [DOT_DIR_Y]. [sx], [sy] and [cum] are the smoothed centreline and its cumulative arc;
     * [headLen] is already scaled to the stroke's draw zoom.
     */
    private fun headDirection(
        sx: DoubleArray,
        sy: DoubleArray,
        cum: DoubleArray,
        headLen: Double,
        finished: Boolean,
    ): Pair<Double, Int> {
        val k = cum.indexOfFirst { it >= headLen }
        if (k < 0) return (if (finished) DOT_DIR_Y else -1.0) to cum.lastIndex
        val dx = sx[k] - sx[0]
        val dy = sy[k] - sy[0]
        val len = hypot(dx, dy)
        // A stroke that came back to where it started has no net direction to read, so take the safe
        // value rather than a heading made of rounding.
        return (if (len < MIN_TANGENT_LEN) -1.0 else dy / len) to k
    }

    /**
     * The calligraphy nib's direction channel: a slew limiter over the raw heading. Widening is
     * earned over [openLen] of travel and thinning is spent over [closeLen], so the width can never
     * change faster than the writer has earned. This bounds the *rate*, not the duration: a short
     * heading change is flattened in proportion to how sharp it is, and a gentle one passes through.
     *
     * The channel spans 2.0 (thin face -1 to broad face +1), so the per-sample cap is
     * `step · 2 / rate`, and travelling exactly [openLen] at full cap carries the width from one
     * extreme to the other. Nothing here reads time: a fast stroke reports sparser samples, each
     * step is longer, and each gets a proportionally larger allowance, so the same path draws the
     * same widths however fast it was written.
     *
     * Samples up to [holdUntil] are pinned to [seed], which is the head: a rate cannot decide the
     * start, having no travel behind it to measure. [steps] is travel along the smoothed centreline,
     * and both lengths are already scaled to the stroke's draw zoom.
     */
    /**
     * [headDirection]'s mirror at the other end, run once the pen has lifted. Across the stroke's
     * last [window] of travel the nib may thin but not widen: a lift-off skid and a downstroke that
     * began one sample ago are the same signal, and the stroke ended before either could hold it.
     * The last sample is the worst of it, since its tangent is one-sided and nothing follows to spend
     * the width back. Everything before the window keeps whatever it earned.
     *
     * A ceiling rather than the head's flat pin, because the tail is not short of information the way
     * the start is. Only the widening is unconfirmable. The thinning either side of it was measured
     * against real travel, so a stroke that curves out of the broad face still thins across the tail
     * exactly as it did before, and one that was already broad the whole way is untouched.
     *
     * Only for a *finished* stroke: while the pen is down the last sample is the pen itself, and
     * capping it would stop the ink widening at all.
     *
     * The ceiling is read at exactly [window] back from the end, between samples rather than at one,
     * so where the window opens does not depend on how densely the pen happened to report. Anchored
     * at a sample instead, the same gesture drawn faster would cap at a different width.
     */
    /**
     * The width multiplier of the end-velocity flick at [fromTip] px back from the tip, over a
     * [flickWindow] px tail. Eased progressively and gently across the tail window so that the stroke
     * transitions smoothly without sudden pinching or collapsing. The tip factor is reduced by 20%
     * to keep the tail fuller and more natural.
     */
    internal fun flickFactorAt(fromTip: Double, flickWindow: Double, endVelocitySensitivity: Double, liftPressure: Double = Double.NaN): Double {
        if (flickWindow <= 0.0 || fromTip >= flickWindow) return 1.0
        val u = (fromTip / flickWindow).coerceIn(0.0, 1.0)
        // Smooth gradual S-curve easing (smoothstep: u * u * (3.0 - 2.0 * u))
        // Transitions gently and progressively along the tail rather than a sudden steep snap
        val ease = u * u * (3.0 - 2.0 * u)
        // Depth of the needle: 20% less pinching as requested ("pahle se 20% kam pichakna chahiye")
        val baseDepth = if (liftPressure.isNaN()) endVelocitySensitivity.coerceIn(0.0, 1.0)
        else (endVelocitySensitivity * (1.0 - liftPressure.coerceIn(0.0, 1.0))).coerceIn(0.0, 1.0)
        val depth = (baseDepth * 0.80).coerceIn(0.0, 0.80)
        val minTipFactor = (1.0 - depth).coerceIn(0.20, 1.0)
        return minTipFactor + (1.0 - minTipFactor) * ease
    }

    /**
     * The end-velocity flick's needle is part of every stroke's signature — the tail the writer
     * watched grow is the tail that lands — so the ease below is applied continuously from the
     * live tip, never gated on how the pen lifted. The window is clamped to a fraction of the
     * stroke's own length so a short dash gets a proportionate tail, not a full-window needle on
     * two dots. The [rawT] parameter is retained for API stability; the gate itself is gone.
     */
    internal fun flickGate(
        steps: DoubleArray,
        rawT: DoubleArray?,
        n: Int,
        speedScale: Double,
        liftPressure: Double,
    ): Boolean {
        if (n < 2 || rawT == null || !liftPressure.isFinite() || liftPressure > FLICK_LOW_PRESSURE) return false
        val first = max(1, n - FLICK_GATE_SAMPLES)
        var distance = 0.0
        var elapsed = 0.0
        for (i in first until n) {
            val dt = rawT[i] - rawT[i - 1]
            if (dt > 0.0) {
                distance += steps[i] * speedScale.coerceAtLeast(0.0)
                elapsed += dt
            }
        }
        return elapsed > 0.0 && distance / elapsed >= FLICK_SPEED_THRESHOLD
    }

    /** Samples of travel the flick gate reads back from the tip. */
    const val FLICK_GATE_SAMPLES = 5
    const val FLICK_SPEED_THRESHOLD = 0.6
    const val FLICK_LOW_PRESSURE = 0.45

    /** The flick needle's tail window, in content px at 100% zoom (scaled by smoothScale), and
     *  the fraction of a stroke's own length it is clamped to for short strokes. Kept short so
     *  handwriting strokes keep most of their body — the tail is an ending, not a diet. Below
     *  [FLICK_MIN_ARC] the stroke is a dot or a hairline dash, and a needle would only deform it. */
    const val FLICK_WINDOW = 20.0
    const val FLICK_WINDOW_FRACTION = 0.6
    const val FLICK_MIN_ARC = 3.0

    private fun capTail(dir: DoubleArray, cum: DoubleArray, window: Double) {
        val n = dir.size
        val start = cum[n - 1] - window
        var i = 1
        while (i < n - 1 && cum[i] < start) i++
        var ceiling = dir[i - 1]
        val span = cum[i] - cum[i - 1]
        if (start > cum[i - 1] && span > MIN_STEP) {
            ceiling += (dir[i] - dir[i - 1]) * (start - cum[i - 1]) / span
        }
        for (j in i until n) {
            if (dir[j] > ceiling) dir[j] = ceiling else ceiling = dir[j]
        }
    }

    private fun nibDirection(
        channel: DoubleArray,
        steps: DoubleArray,
        openLen: Double,
        closeLen: Double,
        seed: Double,
        holdUntil: Int,
    ): DoubleArray {
        val out = DoubleArray(channel.size)
        var d = seed
        for (i in channel.indices) {
            if (i <= holdUntil) {
                out[i] = seed
                continue
            }
            d = nibStep(d, channel[i], if (i == 0) 0.0 else steps[i], openLen, closeLen)
            out[i] = d
        }
        return out
    }

    /** One step of [nibDirection]'s slew limiter: [d] moved toward [target] by at most what [step]
     *  of travel has earned. Shared with [WetRibbon], which carries [d] forward sample by sample. */
    internal fun nibStep(
        d: Double,
        target: Double,
        step: Double,
        openLen: Double,
        closeLen: Double,
    ): Double {
        val rate = if (target > d) openLen else closeLen
        val limit = if (rate > 0.0) step * 2.0 / rate else Double.MAX_VALUE
        return d + (target - d).coerceIn(-limit, limit)
    }

    /**
     * The direction the calligraphy head is pinned to, and the last sample it covers, read off a
     * centreline that already fills [headLen]. [WetRibbon]'s incremental form of [headDirection]:
     * it asks the same question of a live stroke once, at the sample the window first fills, and
     * keeps the answer. Returns null while the window is still short.
     */
    internal fun headDirectionAt(
        sx: DoubleArray,
        sy: DoubleArray,
        k: Int,
    ): Double {
        val dx = sx[k] - sx[0]
        val dy = sy[k] - sy[0]
        val len = hypot(dx, dy)
        // A stroke that came back to where it started has no net direction to read, so take the safe
        // value rather than a heading made of rounding.
        return if (len < MIN_TANGENT_LEN) -1.0 else dy / len
    }

    /**
     * The end-pressure hold ([holdEndPressure]) read at one point instead of applied to the array:
     * the pressure a stroke of [n] samples draws point [i] at, given the smoothed channel [p].
     * [WetRibbon] uses it because the hold's tail window moves as the stroke grows, so the last few
     * points have to be asked again every sample while the ones before them stay put.
     */
    internal fun heldPressureAt(p: DoubleArray, n: Int, i: Int): Double {
        val w = min(CAP_HOLD_SAMPLES, (n - 1) / 2)
        if (w < 1) return p[i]
        if (i < w) return max(p[i], p[w])
        if (i >= n - w) return max(p[i], p[n - 1 - w])
        return p[i]
    }

    /**
     * Builds [StrokeGeometry] from [samples] and the style fields. [speedStrength]
     * and [taperEnabled] default to off, in which case the output is identical to
     * the four-field pen/calligraphy pipeline (spec 03 conformance).
     */
    /**
     * Builds [StrokeGeometry] from [samples] and the style fields. [speedStrength]
     * and [taperEnabled] default to off, in which case the output is identical to
     * the four-field pen/calligraphy pipeline (spec 03 conformance).
     */
    fun build(
        samples: List<Sample>,
        baseWidth: Double,
        pressureEnabled: Boolean,
        m: Double,
        ds: Double,
        speedStrength: Double = 0.0,
        taperEnabled: Boolean = false,
        taperMinFactor: Double = 0.0,
        speedScale: Double = 1.0,
        smooth: Boolean = true,
        holdEnds: Boolean = false,
        finished: Boolean = true,
        smoothScale: Double = 1.0,
        nib: NibAxis.Axis? = null,
        velLo: Double = SPEED_LO,
        velHi: Double = SPEED_HI,
        velAmp: Double = 1.0,
        endVelocitySensitivity: Double = 0.0,
        curvatureDecay: Double = 0.0,
        brushRatio: Double = 1.0,
        brushAngle: Double = 0.0,
        adaptiveSpline: Boolean = false,
        lagComp: Double = 0.0,
        pressureGamma: Double = 1.45,
        tipFrom: Int = -1,
    ): StrokeGeometry {
        val n = samples.size
        if (n == 0) return StrokeGeometry.EMPTY

        // The adaptive-spline subdivision happens in the primitive overload (over doubles, the
        // same pass the committed stroke runs); this overload only forwards the flag.
        val workSamples = samples

        val wn = workSamples.size
        val rawX = DoubleArray(wn)
        val rawY = DoubleArray(wn)
        val rawP = DoubleArray(wn)
        var timed = false
        for (i in 0 until wn) {
            val s = workSamples[i]
            rawX[i] = s.x
            rawY[i] = s.y
            rawP[i] = s.pressure
            if (s.t != 0.0) timed = true
        }
        val rawT = if (timed) DoubleArray(wn) { workSamples[it].t } else null
        return build(
            rawX, rawY, rawP, rawT, baseWidth, pressureEnabled, m, ds, speedStrength,
            taperEnabled, taperMinFactor, speedScale, smooth, holdEnds, finished, smoothScale,
            nib = nib, velLo = velLo, velHi = velHi, velAmp = velAmp,
            endVelocitySensitivity = endVelocitySensitivity, curvatureDecay = curvatureDecay,
            brushRatio = brushRatio, brushAngle = brushAngle,
            lagComp = lagComp,
            adaptiveSpline = adaptiveSpline,
            pressureGamma = pressureGamma,
            tipFrom = tipFrom,
        )
    }

    /**
     * [build] over the caller's own primitive channels: [rawX]/[rawY]/[rawP] are one entry per
     * sample and [rawT] is the elapsed-ms channel, null when the ink carries no timing (everything
     * but the speed pen). [Stroke] stores its samples packed and calls this directly, so painting a
     * dense page never materializes a `Sample` per point.
     */
    fun build(
        rawX: DoubleArray,
        rawY: DoubleArray,
        rawP: DoubleArray,
        rawT: DoubleArray?,
        baseWidth: Double,
        pressureEnabled: Boolean,
        m: Double,
        ds: Double,
        speedStrength: Double = 0.0,
        taperEnabled: Boolean = false,
        taperMinFactor: Double = 0.0,
        speedScale: Double = 1.0,
        smooth: Boolean = true,
        holdEnds: Boolean = false,
        finished: Boolean = true,
        smoothScale: Double = 1.0,
        /** The writer's own nib axis ([NibAxis.of]), or null to measure the direction term on the
         *  page's vertical as every stroke was measured before a stylus could report one. */
        nib: NibAxis.Axis? = null,
        velLo: Double = SPEED_LO,
        velHi: Double = SPEED_HI,
        velAmp: Double = 1.0,
        endVelocitySensitivity: Double = 0.0,
        curvatureDecay: Double = 0.0,
        brushRatio: Double = 1.0,
        brushAngle: Double = 0.0,
        /** Star Notes lag compensation (see [emaStep]): 0.0 = legacy filter, ~0.45 = Star Notes. */
        lagComp: Double = 0.0,
        /** Curvature-adaptive Catmull-Rom subdivision of the input before filtering — the smoothing
         *  that makes cursive silk. The committed stroke runs it so the ink the writer watched (the
         *  wet ribbon pumps the same spline) is the ink that lands. */
        adaptiveSpline: Boolean = false,
        pressureGamma: Double = 1.45,
        /** Index of the first sample that is the predictor's look-ahead tail, or -1 when the stroke
         *  carries none. Those samples were drawn raw, ahead of the nib, and are pinned to their raw
         *  positions instead of being smoothed: a causal low-pass trails its input by about one
         *  smoothing length, and a smoothed tail comes out that much shorter than the tail the
         *  writer watched, so the line would visibly retract at pen-up. */
        tipFrom: Int = -1,
    ): StrokeGeometry {
        var n = rawX.size
        if (n == 0) return StrokeGeometry.EMPTY

        // 1. Shape pressure and filter out digitizer micro-jitter with curvature-adaptive EMA:
        val rawSteps = DoubleArray(n)
        for (i in 1 until n) rawSteps[i] = hypot(rawX[i] - rawX[i - 1], rawY[i] - rawY[i - 1])
        val smoothLen = SMOOTH_LEN * max(smoothScale, 0.0)

        val filtered = if (smooth) {
            emaPositionByArc(rawX, rawY, rawSteps, smoothLen, lagComp)
        } else {
            rawX to rawY
        }
        // The predicted tip is the ink the writer watched: it was drawn raw, ahead of the nib, so
        // the causal low-pass must not touch it. Left to the filter it would come out about one
        // smoothing length short — the line would visibly retract under the hand at pen-up, which
        // is the one thing a look-ahead is never allowed to do. Pinned to its raw positions, the
        // committed ink is the ink that was on screen, point for point.
        //
        // The pin is applied before the spline, and the centripetal spline interpolates its knots
        // exactly, so the subdivided curve still passes through the pinned tip.
        val (fx, fy) = if (tipFrom in 0 until n) {
            val px = filtered.first.copyOf()
            val py = filtered.second.copyOf()
            for (i in tipFrom until n) {
                px[i] = rawX[i]
                py[i] = rawY[i]
            }
            px to py
        } else {
            filtered
        }

        val gamma = pressureGamma.takeIf { it.isFinite() }?.coerceIn(0.5, 2.5) ?: 1.0
        val fp = if (pressureEnabled && gamma != 1.0) {
            DoubleArray(n) { rawP[it].coerceIn(0.0, 1.0).pow(gamma) }
        } else rawP

        val pressureSmoothLen = (PRESSURE_SMOOTH_LEN * max(smoothScale, 0.0)).coerceIn(0.8, 3.0)
        var sp = emaByArc(fp, rawSteps, pressureSmoothLen)
        if (holdEnds && pressureEnabled) holdEndPressure(sp)

        // 2. Centripetal Catmull-Rom Spline Curve Interpolation for silky smooth organic loops at any zoom:
        var sx = fx
        var sy = fy
        var ft = rawT
        if (adaptiveSpline && n >= 3 && smooth) {
            val pts = ArrayList<CurvatureSpline.SplinePoint>(n)
            for (i in 0 until n) {
                pts.add(CurvatureSpline.SplinePoint(fx[i], fy[i], sp[i], rawT?.get(i) ?: 0.0))
            }
            val sub = CurvatureSpline.smoothAndSubdivide(pts)
            if (sub.size > n) {
                val sx2 = DoubleArray(sub.size)
                val sy2 = DoubleArray(sub.size)
                val sp2 = DoubleArray(sub.size)
                val st2 = if (rawT != null) DoubleArray(sub.size) else null
                for (i in sub.indices) {
                    sx2[i] = sub[i].x; sy2[i] = sub[i].y; sp2[i] = sub[i].pressure
                    st2?.set(i, sub[i].time)
                }
                sx = sx2; sy = sy2; sp = sp2; ft = st2
                n = sub.size
            }
        }

        val steps = DoubleArray(n)
        for (i in 1 until n) steps[i] = hypot(sx[i] - sx[i - 1], sy[i] - sy[i - 1])

        fun hw(i: Int, ty: Double) = halfWidth(baseWidth, pressureEnabled, m, ds, sp[i], ty)

        // 3. Single sample -> a filled dot: one swept disc at the pure-pressure half-width. A
        //    finished calligraphy tap takes the dot width (past the broad face) so it stays visible.
        if (n == 1) {
            val h = hw(0, if (finished && ds > 0.0) DOT_DIR_Y else 0.0)
            return StrokeGeometry(
                floatArrayOf(sx[0].toFloat(), sy[0].toFloat()),
                floatArrayOf(h.toFloat()),
            )
        }

        // 4. Per-point unit tangent: central difference at the interior samples, forward/backward at
        //    the ends. A coincident sample pair (e.g. zero travel at pen-down before the first move)
        //    keeps the last valid tangent rather than falling to NaN.
        var lastTx = 1.0
        var lastTy = 0.0
        val tx = DoubleArray(n)
        val ty = DoubleArray(n)
        for (i in 0 until n) {
            val dx = when {
                n <= 1 -> 1.0
                i == 0 -> sx[1] - sx[0]
                i == n - 1 -> sx[n - 1] - sx[n - 2]
                else -> (sx[i + 1] - sx[i - 1]) * 0.5
            }
            val dy = when {
                n <= 1 -> 0.0
                i == 0 -> sy[1] - sy[0]
                i == n - 1 -> sy[n - 1] - sy[n - 2]
                else -> (sy[i + 1] - sy[i - 1]) * 0.5
            }
            val len = hypot(dx, dy)
            if (len < MIN_TANGENT_LEN) {
                tx[i] = lastTx
                ty[i] = lastTy
            } else {
                tx[i] = dx / len
                ty[i] = dy / len
                lastTx = tx[i]
                lastTy = ty[i]
            }
        }

        // Travel along the smoothed centreline and its running total: the path the ink follows, and
        // what every arc-length rule below is measured in.
        val needArc = ds > 0.0 || taperEnabled || endVelocitySensitivity > 0.0 || curvatureDecay > 0.0
        val dirSteps = DoubleArray(if (needArc) n else 0)
        val cum = DoubleArray(dirSteps.size)
        if (needArc) {
            for (i in 1 until n) {
                dirSteps[i] = hypot(sx[i] - sx[i - 1], sy[i] - sy[i - 1])
                cum[i] = cum[i - 1] + dirSteps[i]
            }
        }

        // Optional width multipliers: speed thins fast travel, taper points the ends.
        val sf = if (speedStrength > 0.0 && ft != null) {
            speedFactors(ft, if (n == rawSteps.size) rawSteps else steps, speedStrength, speedScale, velLo, velHi, velAmp)
        } else {
            null
        }
        val tf = if (taperEnabled) taperFactors(cum, taperMinFactor, smoothScale) else null

        // Star Notes lift-off tail (snake tail release): the needle's depth reads the LIFT's own
        // pressure — a firm stroke releases blunt and full of ink, a light one to a hairline —
        // over a window the End-velocity slider scales. Pressure and speed shape it together.
        val liftPressure = if (pressureEnabled) sp[n - 1] else Double.NaN
        val flickDecay = if (finished && endVelocitySensitivity > 0.0 &&
            flickGate(steps, ft, n, speedScale, liftPressure) &&
            cum.isNotEmpty() && cum[n - 1] > FLICK_MIN_ARC
        ) {
            val totalArc = cum[n - 1]
            // Tail window: scaled by zoom/smoothScale, clamped to a fraction of the stroke's own
            // length so a short dash grows a proportionate tail instead of a full-window needle.
            val flickWindow = min(FLICK_WINDOW * max(smoothScale, 0.0), totalArc * FLICK_WINDOW_FRACTION)
            DoubleArray(n) { idx ->
                flickFactorAt(totalArc - cum[idx], flickWindow, endVelocitySensitivity, liftPressure)
            }
        } else null

        // Calligraphy / direction channel:
        // By default, downstroke (+y) is the broad face (+1.0) and upstroke (-y) is thin (-1.0).
        // With tilt/azimuth NibAxis, it aligns to the user's stylus orientation and nib cut.
        val axisDir = if (nib == null) ty else DoubleArray(n) { tx[it] * nib.x + ty[it] * nib.y }
        val dirY = if (ds > 0.0) {
            val window = headLen(smoothScale)
            if (nib != null) {
                if (finished && cum[n - 1] < window) {
                    val d = nibDirection(
                        axisDir, dirSteps, openLen(smoothScale), closeLen(smoothScale),
                        DOT_DIR_Y, cum.lastIndex,
                    )
                    capTail(d, cum, window)
                    d
                } else {
                    val seed = axisDir[0].coerceIn(-1.0, DOT_DIR_Y)
                    val d = nibDirection(
                        axisDir, dirSteps, openLen(smoothScale), closeLen(smoothScale), seed, -1,
                    )
                    if (finished) capTail(d, cum, window)
                    d
                }
            } else {
                val (head, holdUntil) = headDirection(sx, sy, cum, window, finished)
                val d = nibDirection(
                    axisDir, dirSteps, openLen(smoothScale), closeLen(smoothScale), head, holdUntil,
                )
                if (finished) capTail(d, cum, window)
                d
            }
        } else null

        val centerline = FloatArray(2 * n)
        val halfWidths = FloatArray(n)
        val leftRail = FloatArray(2 * n)
        val rightRail = FloatArray(2 * n)

        for (i in 0 until n) {
            val dir = if (dirY != null) dirY[i].coerceIn(-1.0, DOT_DIR_Y) else ty[i]
            var h = hw(i, dir)
            if (sf != null) h *= sf[i]
            if (tf != null) h *= tf[i]
            if (flickDecay != null) h *= flickDecay[i]

            // Brush ratio / aspect angle adjustment
            if (brushRatio != 1.0) {
                val cosA = tx[i] * kotlin.math.cos(brushAngle) - ty[i] * kotlin.math.sin(brushAngle)
                val aspectFactor = kotlin.math.sqrt(cosA * cosA * (brushRatio * brushRatio - 1.0) + 1.0)
                h *= aspectFactor
            }

            writePoint(centerline, halfWidths, leftRail, rightRail, i, sx[i], sy[i], tx[i], ty[i], h)
        }

        return StrokeGeometry(centerline, halfWidths, leftRail, rightRail)
    }

    /**
     * One ribbon point written into the packed output arrays: its centre, its half-width, and the
     * rail vertex either side of it along the normal. Shared with [WetRibbon] so a live stroke's
     * growing ribbon and a finished stroke's rebuilt one are laid out by the same code.
     */
    internal fun writePoint(
        centerline: FloatArray,
        halfWidths: FloatArray,
        leftRail: FloatArray,
        rightRail: FloatArray,
        i: Int,
        x: Double,
        y: Double,
        tx: Double,
        ty: Double,
        h: Double,
    ) {
        halfWidths[i] = h.toFloat()
        centerline[2 * i] = x.toFloat()
        centerline[2 * i + 1] = y.toFloat()
        val nx = -ty // tangent rotated 90°, already unit length
        val ny = tx
        leftRail[2 * i] = (x + nx * h).toFloat()
        leftRail[2 * i + 1] = (y + ny * h).toFloat()
        rightRail[2 * i] = (x - nx * h).toFloat()
        rightRail[2 * i + 1] = (y - ny * h).toFloat()
    }
}
