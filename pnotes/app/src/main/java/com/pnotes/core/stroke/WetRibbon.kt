package com.pnotes.core.stroke

import com.pnotes.core.geometry.Rect
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The ribbon of a stroke still under the pen, grown a sample at a time instead of rebuilt.
 *
 * [StrokeEngine.build] is a batch: every sample re-runs a dozen passes over the whole stroke and
 * allocates a dozen arrays to hold them, so the cost of one frame of ink is proportional to how
 * much ink is already down. Written slowly, a long stroke ends up re-deriving thousands of points
 * to draw the one under the nib, and the line starts trailing the hand.
 *
 * It does not have to. Almost every rule the engine applies is **causal**: the arc-length low-pass
 * ([StrokeEngine.emaByArc]) and the nib's slew limiter ([StrokeEngine.nibStep]) each read one
 * sample and their own previous output, so their answer for a point is fixed the moment that point
 * arrives. What is left looks a bounded distance ahead, and that distance is what this class
 * tracks: [settledCount] is how many leading points can no longer change, whatever the writer does
 * next. Points behind it are computed once and kept. Points in front are the only ones recomputed,
 * and there are never many of them.
 *
 * What holds a point back:
 *  - its **tangent**, a central difference, so the last point is always provisional;
 *  - the **end-pressure hold** ([StrokeEngine.heldPressureAt]), whose window sits over the last
 *    [StrokeEngine.CAP_HOLD_SAMPLES] points and moves with them;
 *  - the **speed** pen's centred window, which reads [StrokeEngine.SPEED_WINDOW_MS] ahead in time;
 *  - the **calligraphy** head, which pins every point flat until the stroke first fills
 *    [StrokeEngine.HEAD_LEN] and then rewrites them once — unless the stylus reported its own nib
 *    axis, in which case the head is known from the first sample and nothing is held back ([NibAxis]);
 *  - the **taper** pen's tail window, [StrokeEngine.TAPER_TAIL_LEN] of arc back from the nib, which
 *    eases the width to the tip as it advances — everything farther back is full width for good.
 *
 * The output is exactly what [StrokeEngine.build] returns for the same samples with
 * `finished = false`, point for point, which the tests hold it to. Lift-time rules (the calligraphy
 * dot and its tail cap) are not modelled here at all: they only fire once the pen is up, and the
 * stroke rebuilds through [StrokeEngine.build] then.
 */
class WetRibbon(
    private val baseWidth: Double,
    private val pressureEnabled: Boolean,
    private val m: Double,
    private val ds: Double,
    private val speedStrength: Double,
    private val speedScale: Double,
    private val smooth: Boolean,
    private val holdEnds: Boolean,
    private val taperEnabled: Boolean,
    private val taperMinFactor: Double,
    /** The writer's own nib axis ([NibAxis.of]), or null to measure the direction channel on the
     *  page's vertical as every stroke was before a stylus could report one. Not to be confused with
     *  [nib], the per-point channel the limiter carries along that axis. */
    private val nibAxis: NibAxis.Axis? = null,
    private val smoothScale: Double,
    private val velLo: Double = StrokeEngine.SPEED_LO,
    private val velHi: Double = StrokeEngine.SPEED_HI,
    private val velAmp: Double = 1.0,
    private val brushRatio: Double = 1.0,
    private val brushAngle: Double = 0.0,
    /** Star Notes lag compensation (StrokeEngine.emaStep); the batch build must receive the same
     *  value at commit or the ink shifts on pen-up. 0.0 = legacy filter. */
    private val lagComp: Double = 0.0,
    /** End-velocity flick sensitivity; the batch build must receive the same value at commit.
     *  The needle itself only forms once the ribbon is told the pen is up ([finishFlick]). */
    private val endVelocitySensitivity: Double = 0.0,
    /** When true the caller feeds this ribbon curvature-spline points instead of raw samples, so
     *  the centreline it draws is the smooth curve rather than the digitizer's polyline. The
     *  committed stroke must subdivide its input too ([StrokeEngine.build]'s adaptiveSpline), and
     *  the raw sample arrays the stroke keeps are then for storage, undo and reload only. */
    val splineFed: Boolean = false,
    private val pressureGamma: Double = 1.45,
) : RibbonPoints {
    private val pressureCurve = pressureGamma.takeIf { it.isFinite() }?.coerceIn(0.5, 2.5) ?: 1.0
    /** Samples taken so far; one ribbon point each. */
    override var pointCount = 0
        private set

    /**
     * Leading points whose centre and half-width are final. Everything below this index can be
     * baked into a raster or uploaded to the GPU once and never revisited; everything at or above
     * it is redrawn each frame. 0 while the stroke is too young for anything to have settled.
     */
    var settledCount = 0
        private set

    // --- the causal channels, one entry per sample, grown by doubling ---

    private var sx = EMPTY_D
    private var sy = EMPTY_D
    private var sp = EMPTY_D
    private var tx = EMPTY_D
    private var ty = EMPTY_D

    /** Arc from each point back to the tip. The taper pen and the end-velocity needle both read it,
     *  and only over the live tail, which is where a frame's work already is — so the tail can slide
     *  with the nib without a pass over the stroke behind it. */
    private var tipDist = EMPTY_D

    /** Elapsed ms and cumulative *raw* travel: only the speed pen's window reads them. */
    private var times = EMPTY_D
    private var rawCum = EMPTY_D

    /** The nib's slew-limited direction, final up to [nibDone]; calligraphy only. */
    private var nib = EMPTY_D

    // --- the packed output, over-allocated; only [0, pointCount) is meaningful ---

    private var centerline = EMPTY_F
    private var halfWidths = EMPTY_F
    private var leftRail = EMPTY_F
    private var rightRail = EMPTY_F

    // --- carried state, so no pass ever restarts at the head of the stroke ---

    private var lastRawX = 0.0
    private var lastRawY = 0.0
    private var lastRawP = 0.0
    private var prevRawX = 0.0
    private var prevRawY = 0.0
    private var prevDx = 0.0
    private var prevDy = 0.0

    /** Cumulative travel along the *smoothed* centreline: what the nib's head window is measured in. */
    private var arc = 0.0

    private val smoothLen = StrokeEngine.SMOOTH_LEN * max(smoothScale, 0.0)
    private val pressureSmoothLen = (StrokeEngine.PRESSURE_SMOOTH_LEN * max(smoothScale, 0.0)).coerceIn(0.8, 3.0)
    private val headLen = StrokeEngine.headLen(smoothScale)
    private val openLen = StrokeEngine.openLen(smoothScale)
    private val closeLen = StrokeEngine.closeLen(smoothScale)

    /** The taper pen's tail window and its too-short-to-taper floor, both at this stroke's draw zoom;
     *  0 while the pen does not taper, so nothing below allocates or runs for the other pens. */
    private val taperTailLen = if (taperEnabled) StrokeEngine.TAPER_TAIL_LEN * max(smoothScale, 0.0) else 0.0
    private val taperMinLen = StrokeEngine.TAPER_MIN_LEN * max(smoothScale, 0.0)

    /** Tangents are final below this; the last one is always a backward difference and provisional. */
    private var tanDone = 0
    private var carryTx = 1.0
    private var carryTy = 0.0

    /** The sample the calligraphy head window first filled at, or -1 while it is still short. */
    private var headK = -1
    private var headDir = -1.0
    private var nibValue = 0.0
    private var nibDone = 0

    /** The speed window's two sample cursors as they stand entering point [settledCount]. */
    private val speedCursor = IntArray(2)
    private val speedScratch = IntArray(2)

    /** Disc box of the settled points; [bounds] unions the live tail into it. */
    private var minX = Double.POSITIVE_INFINITY
    private var minY = Double.POSITIVE_INFINITY
    private var maxX = Double.NEGATIVE_INFINITY
    private var maxY = Double.NEGATIVE_INFINITY

    @Volatile
    private var snapshot: StrokeGeometry? = null

    /** Whether this pen records timing at all; the channel is allocated for it, used once it fills. */
    private val timed = speedStrength > 0.0

    /** True once the times channel actually carries usable stamps (some sample had t != 0).
     *  The end-velocity pen allocates the channel but without stamps it has no timing rules to
     *  wait out — tests and untimed inputs feed t = 0 and must settle like the speedless pens. */
    private val timesUsable: Boolean
        get() = anyTime && pointCount >= 2 && times[pointCount - 1] - times[0] > 0.0

    /** Set by the first sample carrying a real elapsed time, mirroring how [com.pnotes.core.model.Stroke]
     *  only allocates its time channel once one arrives. Without it the engine reads no speed. */
    private var anyTime = false

    /** Whether the speed pen's width factor applies yet: the same three gates [StrokeEngine.speedFactors]
     *  falls out of, so a stroke too young or too brief to time draws at plain pressure width. */
    private fun speedActive(n: Int): Boolean =
        speedStrength > 0.0 && timed && anyTime && n >= 2 && times[n - 1] - times[0] > 0.0

    // --- reading the ribbon ---

    override fun cx(i: Int): Double = centerline[2 * i].toDouble()
    override fun cy(i: Int): Double = centerline[2 * i + 1].toDouble()
    override fun hw(i: Int): Double = halfWidths[i].toDouble()

    /** The packed centres, over-allocated: read `2 * pointCount` floats. */
    fun centerlineArray(): FloatArray = centerline

    /** The packed radii, over-allocated: read [pointCount] floats. */
    fun halfWidthArray(): FloatArray = halfWidths

    override fun leftX(i: Int): Double = leftRail[2 * i].toDouble()
    override fun leftY(i: Int): Double = leftRail[2 * i + 1].toDouble()
    override fun rightX(i: Int): Double = rightRail[2 * i].toDouble()
    override fun rightY(i: Int): Double = rightRail[2 * i + 1].toDouble()

    /** A ribbon of two points or more has a body; a lone sample is a dot with no rails. */
    override val hasRails get() = pointCount >= 2

    /**
     * The ribbon as a standalone [StrokeGeometry], copied out at its exact length. Kept until the
     * next sample lands, so the cold call sites that want a whole geometry (bounds for a neon
     * halo, a hit test, the pen-up reduction) can have one without forcing the ribbon back into a
     * batch rebuild.
     */
    fun geometry(): StrokeGeometry {
        snapshot?.let { return it }
        val n = pointCount
        val pts = pendingTail
        if (n == 0 && pts.isEmpty()) return StrokeGeometry.EMPTY.also { snapshot = it }
        if (pts.isNotEmpty()) {
            // Drain pending lookahead tail so snapshot geometry matches the visual live ink
            flushPending()
            return geometry()
        }
        val c = centerline.copyOf(2 * n)
        val h = halfWidths.copyOf(n)
        // A single sample is a dot: one swept disc, no rails, exactly as the batch build returns it.
        val g = if (n == 1) {
            StrokeGeometry(c, h)
        } else {
            StrokeGeometry(c, h, leftRail.copyOf(2 * n), rightRail.copyOf(2 * n))
        }
        return g.also { snapshot = it }
    }

    /** Disc box of the whole ribbon: every centre grown by its own half-width. The pump's queued
     *  tail is not ribbon geometry yet, but a frame that ignores it would clip the very tip the
     *  writer sees, so its bounding box is unioned in here. */
    fun bounds(): Rect {
        if (pointCount == 0 && pendingTail.isEmpty()) return Rect(0.0, 0.0, 0.0, 0.0)
        var lo0 = minX
        var lo1 = minY
        var hi0 = maxX
        var hi1 = maxY
        for (i in settledCount until pointCount) {
            val x = cx(i)
            val y = cy(i)
            val r = hw(i)
            if (x - r < lo0) lo0 = x - r
            if (x + r > hi0) hi0 = x + r
            if (y - r < lo1) lo1 = y - r
            if (y + r > hi1) hi1 = y + r
        }
        for (p in pendingTail) {
            if (p.x < lo0) lo0 = p.x
            if (p.x > hi0) hi0 = p.x
            if (p.y < lo1) lo1 = p.y
            if (p.y > hi1) hi1 = p.y
        }
        return Rect(lo0, lo1, hi0 - lo0, hi1 - lo1)
    }

    // --- growing the ribbon ---

    /**
     * The needle is part of the signature, not a lift-time event: it eases the live tip towards a
     * point while the pen is still down, so what the writer watched grow is exactly what lands at
     * commit — the batch build ([StrokeEngine.flickFactorAt]) runs the same always-on ease over the
     * same clamped window, and there is nothing left to latch.
     */
    fun finishFlick() {
        // Kept for call-site symmetry with the lift-time rules; a no-op now that the needle is live.
    }

    /** The pump's not-yet-appended spline points: they extend the current tail and are part of what
     *  a frame must draw, but the settled rules never see them (the next append or the commit
     *  flushes them in order). */
    private var pendingTail: List<CurvatureSpline.SplinePoint> = emptyList()

    /** Queue the pump's lookahead points; drained in order by the next [append] or [flushPending].
     *  Replaces any previous tail — the pump re-fences its last segment each time it advances. */
    fun setPendingTail(pts: List<CurvatureSpline.SplinePoint>) {
        pendingTail = pts
        snapshot = null
    }

    /** Drain the queued points into the ribbon in order. */
    fun flushPending() {
        if (pendingTail.isEmpty()) return
        val pts = pendingTail
        pendingTail = emptyList()
        for (p in pts) append(p.x, p.y, p.pressure, p.time)
    }

    /** Take one more sample: extend every causal channel by a step, then redraw the live tail. */
    fun append(x: Double, y: Double, pressure: Double, t: Double) {
        val i = pointCount
        ensure(i + 1)
        val shapedPressure = pressure.coerceIn(0.0, 1.0).pow(pressureCurve)
        if (t != 0.0) anyTime = true
        val step = if (i == 0) 0.0 else hypot(x - lastRawX, y - lastRawY)
        if (i == 0) {
            sx[0] = x
            sy[0] = y
            sp[0] = shapedPressure
            prevDx = 0.0
            prevDy = 0.0
            if (timed) {
                times[0] = t
                rawCum[0] = 0.0
            }
        } else {
            val curDx = x - lastRawX
            val curDy = y - lastRawY
            val curLen = hypot(curDx, curDy)
            val prevLen = hypot(prevDx, prevDy)
            val dot = if (i > 1 && prevLen > 1e-4 && curLen > 1e-4) {
                (prevDx * curDx + prevDy * curDy) / (prevLen * curLen)
            } else 1.0

            // Layer 2: 1€ Kinematic Curvature Adaptive Filter (Apple PencilKit / GoodNotes model):
            // On sharp peaks/valleys ("mm", "ww", cursive loops where dot < 0.75), smoothing length
            // drops to 0.0 so the apex reaches 100% of the true physical peak without cutting corners or becoming `~~~`.
            // On straight lines (dot ~ 1.0), smoothing stays at 100% for jitter-free stability.
            val cornerFactor = if (dot < 0.75) {
                ((dot + 0.25) / 1.0).coerceIn(0.0, 1.0).let { it * it }
            } else {
                1.0
            }
            val effectiveSmoothLen = smoothLen * cornerFactor

            sx[i] = if (smooth && effectiveSmoothLen > 0.0) StrokeEngine.emaStep(sx[i - 1], lastRawX, x, step, effectiveSmoothLen, lagComp) else x
            sy[i] = if (smooth && effectiveSmoothLen > 0.0) StrokeEngine.emaStep(sy[i - 1], lastRawY, y, step, effectiveSmoothLen, lagComp) else y
            sp[i] = StrokeEngine.emaStep(sp[i - 1], lastRawP, shapedPressure, step, pressureSmoothLen)
            if (timed) {
                times[i] = t
                rawCum[i] = rawCum[i - 1] + step
            }
            arc += hypot(sx[i] - sx[i - 1], sy[i] - sy[i - 1])
            prevDx = curDx
            prevDy = curDy
        }
        lastRawX = x
        lastRawY = y
        lastRawP = shapedPressure
        pointCount = i + 1
        snapshot = null

        extendTangents()
        startNib()
        extendNib()
        rebuildTail()
    }

    /** The direction channel at point [i]: the tangent's component along the nib's broad axis. With no
     *  axis from the stylus this is the tangent's y-component — the same doubles the direction term has
     *  always been measured on. */
    private fun axisAt(i: Int): Double {
        val nb = nibAxis ?: return ty[i]
        return tx[i] * nb.x + ty[i] * nb.y
    }

    /**
     * Tangents by finite difference, exactly as the batch build takes them. Only the last one is a
     * one-sided difference, so each new sample turns the previous last into an interior point and
     * fixes it for good. [carryTx]/[carryTy] hold the last usable direction entering the first
     * point still in play, which is what a run of coincident samples falls back on.
     */
    private fun extendTangents() {
        val n = pointCount
        if (n < 2) return
        var lx = carryTx
        var ly = carryTy
        for (i in tanDone until n) {
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
            if (len < StrokeEngine.MIN_TANGENT_LEN) {
                tx[i] = lx
                ty[i] = ly
            } else {
                tx[i] = dx / len
                ty[i] = dy / len
                lx = tx[i]
                ly = ty[i]
            }
            if (i == n - 2) {
                carryTx = lx
                carryTy = ly
            }
        }
        tanDone = n - 1
    }

    /**
     * The calligraphy direction channel, started once — the ribbon's form of the engine's two ways of
     * getting it.
     *
     * With the stylus's own axis ([nib]) the answer is in hand at pen-down: the first sample's
     * direction along that axis *is* the head, so the limiter starts on it and nothing is pinned. That
     * is what lets the head settle immediately instead of waiting [headLen] for a chord, and what
     * removes the rewrite a live stroke used to get when the window filled.
     *
     * Without one the head is decided at the sample the smoothed travel first reaches [headLen], from
     * the chord across the window: until then there is no heading to read and every point draws thin,
     * so nothing settles.
     */
    private fun startNib() {
        if (ds <= 0.0 || headK >= 0) return
        val nb = nibAxis
        if (nb != null) {
            if (pointCount < 2) return // one sample has no direction to project
            val seed = axisAt(0).coerceIn(-1.0, StrokeEngine.DOT_DIR_Y)
            headK = 0
            headDir = seed
            nib[0] = seed
            nibValue = seed
            nibDone = 1
            return
        }
        if (arc < headLen) return
        val k = pointCount - 1
        headK = k
        headDir = StrokeEngine.headDirectionAt(sx, sy, k)
        for (i in 0..k) nib[i] = headDir
        nibValue = headDir
        nibDone = k + 1
    }

    /** Carry the nib's slew limiter forward over every point whose tangent has stopped moving. */
    private fun extendNib() {
        if (ds <= 0.0 || headK < 0) return
        for (i in nibDone until pointCount - 1) {
            nibValue = StrokeEngine.nibStep(nibValue, axisAt(i), stepAlong(i), openLen, closeLen)
            nib[i] = nibValue
            nibDone = i + 1
        }
    }

    /** Travel along the smoothed centreline into point [i]; what the nib measures its rate against. */
    private fun stepAlong(i: Int): Double =
        if (i == 0) 0.0 else hypot(sx[i] - sx[i - 1], sy[i] - sy[i - 1])

    /**
     * Recompute every point from [settledCount] to the nib, then move the settled mark up to
     * whatever the rules now allow. This is the only loop that runs per frame, and its length is
     * bounded by the lookahead the pen needs rather than by the stroke.
     */
    private fun rebuildTail() {
        val n = pointCount
        val from = settledCount
        val target = settleTarget()

        // Arc back to the tip for the points in play, accumulated from the nib forwards-in-reverse.
        // The flick needle reads it too (it is measured over the same kind of window as the taper).
        if (taperTailLen > 0.0) {
            tipDist[n - 1] = 0.0
            for (i in n - 2 downTo from) tipDist[i] = tipDist[i + 1] + stepAlong(i + 1)
        }

        // The last point's nib value rides on a tangent that is still moving, so it is computed
        // fresh here rather than committed to [nib].
        val liveNib = if (ds <= 0.0 || headK < 0) {
            -1.0
        } else if (n - 1 < nibDone) {
            nib[n - 1]
        } else {
            StrokeEngine.nibStep(nibValue, axisAt(n - 1), stepAlong(n - 1), openLen, closeLen)
        }

        val speed = speedActive(n)
        val held = holdEnds && pressureEnabled
        speedScratch[0] = speedCursor[0]
        speedScratch[1] = speedCursor[1]
        for (i in from until n) {
            if (i == target) {
                speedCursor[0] = speedScratch[0]
                speedCursor[1] = speedScratch[1]
            }
            // A lone sample is a dot: no tangent, no nib, and the pure-pressure half-width.
            val dir = when {
                n == 1 -> 0.0
                ds <= 0.0 -> ty[i]
                headK < 0 -> -1.0
                else -> (if (i < nibDone) nib[i] else liveNib).coerceIn(-1.0, StrokeEngine.DOT_DIR_Y)
            }
            val pressure = if (held) StrokeEngine.heldPressureAt(sp, n, i) else sp[i]
            var h = StrokeEngine.halfWidth(baseWidth, pressureEnabled, m, ds, pressure, dir)
            if (speed) {
                h *= StrokeEngine.speedFactorAt(times, rawCum, n, i, speedScratch, speedStrength, speedScale)
            }
            // A lone sample is a dot, untapered, exactly as the batch build returns it.
            if (taperTailLen > 0.0 && n > 1) {
                h *= StrokeEngine.taperFactorAt(tipDist[i], arc, taperMinFactor, smoothScale)
            }
            // Brush ratio / aspect angle adjustment
            if (brushRatio != 1.0) {
                val cosA = tx[i] * kotlin.math.cos(brushAngle) - ty[i] * kotlin.math.sin(brushAngle)
                val aspectFactor = kotlin.math.sqrt(cosA * cosA * (brushRatio * brushRatio - 1.0) + 1.0)
                h *= aspectFactor
            }

            if (n == 1) {
                halfWidths[0] = h.toFloat()
                centerline[0] = sx[0].toFloat()
                centerline[1] = sy[0].toFloat()
            } else {
                StrokeEngine.writePoint(
                    centerline, halfWidths, leftRail, rightRail, i, sx[i], sy[i], tx[i], ty[i], h,
                )
            }
        }
        for (i in from until target) {
            val x = cx(i)
            val y = cy(i)
            val r = hw(i)
            if (x - r < minX) minX = x - r
            if (x + r > maxX) maxX = x + r
            if (y - r < minY) minY = y - r
            if (y + r > maxY) maxY = y + r
        }
        settledCount = target
    }

    /**
     * How many leading points can no longer change, as the tightest of the pen's lookaheads. Each
     * rule is stated as the first index it still has a claim on; the settled mark is the smallest.
     */
    private fun settleTarget(): Int {
        val n = pointCount
        if (n < 2) return 0
        // The newest point's tangent is one-sided, and the next sample will rewrite it.
        var s = n - 1
        if (holdEnds && pressureEnabled) {
            // The hold's window is min(CAP_HOLD_SAMPLES, (n-1)/2) wide at each end, so until it
            // reaches full width the head it covers is still moving too.
            if (n < 2 * StrokeEngine.CAP_HOLD_SAMPLES + 1) return 0
            s = min(s, n - StrokeEngine.CAP_HOLD_SAMPLES)
        }
        // Calligraphy pins every point to the head until the head exists, then rewrites them once.
        if (ds > 0.0 && headK < 0) return 0
        if (speedActive(n)) {
            val t0 = times[0]
            val tN = times[n - 1]
            // Strictly past the window on both sides: a sample landing on its edge would still be
            // inside it, and would move the arc the window averages over. The left clamp slides the
            // window right for early points, which is why the head waits out a whole span.
            if (tN <= t0 + 2.0 * StrokeEngine.SPEED_WINDOW_MS) return 0
            var k = settledCount
            while (k < n && times[k] + StrokeEngine.SPEED_WINDOW_MS < tN) k++
            s = min(s, k)
        } else if (speedStrength > 0.0 && !timesUsable) {
            // The speed pen's widths ride the times channel; until it carries something usable one
            // timed sample would put every width back in play, so nothing settles until it does.
            return 0
        }
        if (taperTailLen > 0.0) {
            // Below the too-short floor nothing tapers, and crossing it re-shapes the whole stroke at
            // once, so no point is final until the stroke is past it.
            if (arc < taperMinLen) return 0
            // Walk back from the nib until the tail window is spent; the point we stop on is the
            // oldest one the window still reaches, and it has to be redrawn for as long as it moves.
            var d = 0.0
            var k = n - 1
            while (k > 0 && d < taperTailLen) {
                d += stepAlong(k)
                k--
            }
            s = min(s, if (d >= taperTailLen) k + 1 else 0)
        }
        return max(s, 0)
    }

    private fun ensure(capacity: Int) {
        if (sx.size >= capacity) return
        var c = if (sx.isEmpty()) INITIAL_CAPACITY else sx.size
        while (c < capacity) c *= 2
        sx = sx.copyOf(c)
        sy = sy.copyOf(c)
        sp = sp.copyOf(c)
        tx = tx.copyOf(c)
        ty = ty.copyOf(c)
        if (timed) {
            times = times.copyOf(c)
            rawCum = rawCum.copyOf(c)
        }
        if (ds > 0.0) nib = nib.copyOf(c)
        if (taperTailLen > 0.0) tipDist = tipDist.copyOf(c)
        centerline = centerline.copyOf(2 * c)
        halfWidths = halfWidths.copyOf(c)
        leftRail = leftRail.copyOf(2 * c)
        rightRail = rightRail.copyOf(2 * c)
    }

    companion object {
        private val EMPTY_D = DoubleArray(0)
        private val EMPTY_F = FloatArray(0)

        private const val INITIAL_CAPACITY = 64

        /**
         * Whether a stroke of this style can be grown rather than rebuilt. The taper pen can, now
         * that its profile is a rate of travel rather than a fraction of the total arc: a point's
         * distance from the nib only grows, so once the tail window has passed it its width is final
         * (see [StrokeEngine.taperFactorAt]). A straight-line tool cannot — it holds two samples and
         * replaces the second, so there is nothing to grow and nothing to gain.
         */
        fun supports(straight: Boolean): Boolean = !straight
    }
}
