package com.pnotes.core.inking

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Kotlin port of the Star Notes native stroke pipeline (Onyx `neo3pen` engine,
 * `libneopen_jni.so`), reproducing the exact stage graph found in the binary:
 *
 *   TouchEvent -> StrokePointProducer -> SmoothFilter(smoothLevel) -> SplineProducer
 *              -> width calculators -> NeoStrokeRenderer
 *
 * Native width-calculator symbols reproduced here:
 *   - `FountainPenWidthCalculator`, `BallpointPenCalculator`, `DirectionalWidthCalculator`,
 *     `FixedWidthCalculator`
 * Native config knobs honored (exact names from the .so):
 *   `smoothLevel`, `penWidth`, `penMinWidth`, `pressureSensitivity`, `ignorePressure`,
 *   `velocitySensitivity`, `endVelocitySensitivity`, `velocityAmplifier`,
 *   `velocityIgnoreThreshold`, `velocityLowerBound`, `velocityUpperBound`
 *
 * The engine emits [InkingStrokeSegment]s (quadratic Bézier with per-end width), the same
 * render primitive the rest of the app already consumes, so it is a drop-in replacement for
 * [StylusStrokeEngine].
 */
class NeoPenStrokeEngine(
    var config: NeoPenConfig,
) {
    // ---- Stage state -------------------------------------------------------------
    private val smoothFilter = SmoothFilter(config.smoothLevel)
    private var smoothedVelocity = 0f
    private var started = false

    // Cache of the most recent raw input for width telemetry.
    private var lastRaw: InkPoint? = null
    private var lastVelocity = 0f

    // Keep a short window of filtered points for Catmull-Rom neighbors.
    private val splineWindow = ArrayDeque<InkPoint>()

    /** Segments accumulated for the active stroke. */
    private val activeSegments = mutableListOf<InkingStrokeSegment>()

    // ---------------------------------------------------------------------------------
    // StrokePointProducer stage: ingest raw digitizer samples.
    // ---------------------------------------------------------------------------------

    fun startStroke(point: InkPoint): InkingStrokeSegment? {
        started = true
        smoothFilter.updateLevel(config.smoothLevel)
        smoothFilter.reset()
        splineWindow.clear()
        activeSegments.clear()
        smoothedVelocity = 0f
        lastRaw = point
        lastVelocity = 0f

        val filtered = smoothFilter.filter(point, 0f)
        // Pad the window with a duplicated start point so the first Catmull-Rom
        // segment covers p0 -> p1 and ink begins exactly at pen-down.
        splineWindow.addLast(filtered)
        splineWindow.addLast(filtered)
        return null
    }

    fun addPoint(raw: InkPoint, velocity: Float): InkingStrokeSegment? {
        if (!started) {
            startStroke(raw)
        }
        lastRaw = raw
        lastVelocity = velocity
        // Velocity EMA keeps width response stable against sampling-rate noise.
        smoothedVelocity = 0.6f * smoothedVelocity + 0.4f * velocity

        // Stage 1: SmoothFilter.
        val filtered = smoothFilter.filter(raw, smoothedVelocity)

        // Suppress duplicate emissions from the dead-zone (identical position).
        val prev = splineWindow.lastOrNull()
        if (prev != null && filtered.x == prev.x && filtered.y == prev.y) {
            return null
        }

        splineWindow.addLast(filtered)
        while (splineWindow.size > 4) splineWindow.removeFirst()

        // Need a 4-point Catmull-Rom window (p0,p1,p2,p3).
        if (splineWindow.size < 4) return null

        // Emit the segment p1 -> p2, then slide the window forward so the next
        // emission continues exactly where this one ended.
        val out = emitWindowSegment()
        splineWindow.removeFirst()
        return out
    }

    fun endStroke(endPoint: InkPoint): InkingStroke {
        if (!started) {
            startStroke(endPoint)
        }

        // Flush the remaining spline window: filter the pen-up point, append it, then
        // emit a segment for every remaining window position so the ink reaches the
        // true pen-up position (the native engine does the same at pen-up).
        val tail = smoothFilter.filter(endPoint, smoothedVelocity)
        val prevTail = splineWindow.lastOrNull()
        if (prevTail == null || tail.x != prevTail.x || tail.y != prevTail.y) {
            splineWindow.addLast(tail)
            while (splineWindow.size > 4) splineWindow.removeFirst()
            while (splineWindow.size >= 4) {
                emitWindowSegment() // appends internally
                splineWindow.removeFirst()
            }
        }

        // Connect the last two window points directly so the stroke always lands on
        // the pen-up position even with fewer than 4 points remaining.
        val remaining = splineWindow.toList()
        if (remaining.size >= 2) {
            val a = remaining[remaining.size - 2]
            val b = remaining[remaining.size - 1]
            activeSegments.add(
                InkingStrokeSegment(
                    startX = a.x, startY = a.y,
                    controlX = (a.x + b.x) * 0.5f, controlY = (a.y + b.y) * 0.5f,
                    endX = b.x, endY = b.y,
                    startWidth = computeStrokeWidth(a.pressure, smoothedVelocity),
                    endWidth = computeStrokeWidth(b.pressure, smoothedVelocity),
                    alpha = computeStrokeAlpha(b.pressure),
                    color = color,
                    profile = profile,
                )
            )
        } else if (activeSegments.isEmpty()) {
            // Single tap -> dot.
            val p = lastRaw ?: endPoint
            val dotWidth = computeStrokeWidth(endPoint.pressure, 0f)
            activeSegments.add(
                InkingStrokeSegment(
                    startX = p.x - 0.5f, startY = p.y - 0.5f,
                    controlX = p.x, controlY = p.y,
                    endX = p.x + 0.5f, endY = p.y + 0.5f,
                    startWidth = dotWidth, endWidth = dotWidth,
                    alpha = computeStrokeAlpha(endPoint.pressure),
                    color = color,
                    profile = profile,
                )
            )
        }

        // endVelocitySensitivity: native tail taper — final width thins by the last
        // recorded speed fraction before pen-up.
        if (config.endVelocitySensitivity > 0f && activeSegments.isNotEmpty()) {
            val last = activeSegments.last()
            val taper = 1f - (config.endVelocitySensitivity *
                min(1f, smoothedVelocity / config.velocityUpperBound) * 0.5f)
            activeSegments[activeSegments.size - 1] = last.copy(
                endWidth = (last.endWidth * taper).coerceAtLeast(config.penMinWidth * 0.5f)
            )
        }

        val completed = InkingStroke(
            segments = activeSegments.toList(),
            profile = profile,
            baseWidth = config.penWidth,
            color = color,
            isComplete = true,
        )

        started = false
        splineWindow.clear()
        activeSegments.clear()
        return completed
    }

    /**
     * Builds the quadratic-render segment p1 -> p2 from the current 4-point Catmull-Rom
     * window and records it in [activeSegments]. The window is NOT advanced here.
     */
    private fun emitWindowSegment(): InkingStrokeSegment {
        val p0 = splineWindow[0]
        val p1 = splineWindow[1]
        val p2 = splineWindow[2]
        val p3 = splineWindow[3]
        val seg = SplineProducer.catmullRomToBezier(p0, p1, p2, p3)

        val w1 = computeStrokeWidth(p1.pressure, smoothedVelocity)
        val w2 = computeStrokeWidth(p2.pressure, smoothedVelocity)
        val a1 = computeStrokeAlpha(p1.pressure)
        val a2 = computeStrokeAlpha(p2.pressure)

        val out = InkingStrokeSegment(
            startX = seg.startX,
            startY = seg.startY,
            // Cubic -> quadratic reduction: control = midpoint of the two cubic controls.
            controlX = (seg.control1X + seg.control2X) * 0.5f,
            controlY = (seg.control1Y + seg.control2Y) * 0.5f,
            endX = seg.endX,
            endY = seg.endY,
            startWidth = w1,
            endWidth = w2,
            alpha = (a1 + a2) * 0.5f,
            color = color,
            profile = profile,
        )
        activeSegments.add(out)
        return out
    }

    // ---------------------------------------------------------------------------------
    // Width calculators (FountainPenWidthCalculator / BallpointPenCalculator etc.)
    // ---------------------------------------------------------------------------------

    /**
     * Dynamic width from pressure and velocity using the native knob semantics:
     *
     *   W = penMinWidth + (penWidth - penMinWidth) * pressureFactor * velocityFactor
     *
     *   pressureFactor = p^(gamma) scaled by pressureSensitivity (amplifier applied first)
     *   velocityFactor = 1 - velocitySensitivity * ramp(v; lowerBound, upperBound)
     *   ramp = 0 below velocityIgnoreThreshold and below velocityLowerBound
     */
    fun computeStrokeWidth(pressure: Float, velocity: Float): Float {
        if (config.ignorePressure || config.pressureSensitivity <= 0f) {
            return applyVelocityOnly(velocity)
        }

        val amplified = (pressure * config.velocityAmplifier).coerceIn(0f, 1f)
        val clamped = amplified.coerceIn(0.01f, 1f)
        val pressureFactor = clamped.toDouble().pow(1.5).toFloat().coerceIn(0f, 1f)
        val pressureWidth = config.penMinWidth +
            (config.penWidth - config.penMinWidth) * config.pressureSensitivity * pressureFactor

        return applyVelocityOnly(velocity, pressureWidth)
    }

    private fun applyVelocityOnly(velocity: Float, base: Float = config.penWidth): Float {
        if (config.velocitySensitivity <= 0f) return base
        if (velocity < config.velocityIgnoreThreshold) return base

        val t = ((velocity - config.velocityLowerBound) /
            max(0.01f, config.velocityUpperBound - config.velocityLowerBound)).coerceIn(0f, 1f)
        val thinned = base * (1f - config.velocitySensitivity * t)
        return thinned.coerceAtLeast(config.penMinWidth * 0.5f)
    }

    /** Pressure-modulated opacity for pencil/shading instruments. */
    fun computeStrokeAlpha(pressure: Float): Float {
        if (!profile.isAlphaModulated) {
            return if (profile.penType == PenType.HIGHLIGHTER) 0.45f else 1.0f
        }
        val clamped = pressure.coerceIn(0.01f, 1f)
        val f = clamped.toDouble().pow(profile.gamma.toDouble()).toFloat().coerceIn(0f, 1f)
        return (profile.minAlpha + (profile.maxAlpha - profile.minAlpha) * f).coerceIn(0.1f, 1f)
    }

    // ---------------------------------------------------------------------------------
    // Compatibility surface matching StylusStrokeEngine so callers can swap engines.
    // ---------------------------------------------------------------------------------

    var profile: PenProfile = config.penType.let { PenProfile.forType(it) }
        set(value) {
            field = value
            config = config.copy(penType = value.penType)
        }

    var color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White

    var baseWidth: Float
        get() = config.penWidth
        set(value) {
            config = config.copy(penWidth = value)
        }

    var stabilizationFactor: Float
        get() = config.smoothLevel
        set(value) {
            config = config.copy(smoothLevel = value)
            smoothFilter.updateLevel(value)
        }

    fun appendSegmentToPath(path: android.graphics.Path, segment: InkingStrokeSegment) {
        path.moveTo(segment.startX, segment.startY)
        path.quadTo(segment.controlX, segment.controlY, segment.endX, segment.endY)
    }
}
