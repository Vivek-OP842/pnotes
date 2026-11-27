package com.pnotes.core.inking

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/**
 * Kotlin port of `neo3pen::SmoothFilter` from Onyx's native ink engine
 * (`libneopen_jni.so`, used by Star Notes).
 *
 * The native filter (symbols: `SmoothFilter`, `SmoothFilterE`, `smoothLevel`, `noiseSmooth`)
 * is a two-stage stabilization filter applied between the StrokePointProducer and
 * SplineProducer stages:
 *
 *  1. **Noise gate / dead-zone:** micro-movements below a distance threshold are treated as
 *     digitizer noise and collapsed onto the previous filtered point. Threshold scales with
 *     `smoothLevel` so heavy stabilization absorbs more jitter.
 *  2. **Position EMA with lag compensation:** an exponential moving average low-pass filter
 *     on x/y. A pure EMA trails the pen ("rubber-band" feel); the compensation term adds a
 *     fraction of the current motion vector back to the filtered point so the stroke tip
 *     still lands where the pen is, which is how the native engine keeps low latency at
 *     high smoothLevel.
 *  3. **Pressure EMA:** independent low-pass on the pressure channel (matches the
 *     `pressureSensitivity` config knob semantics).
 *
 * The filter is stateful per-stroke and must be reset via [reset] at pen-down.
 */
class SmoothFilter(
    private var smoothLevel: Float,
) {
    private var initialized = false
    private var fx = 0f
    private var fy = 0f
    private var fPressure = 0.5f

    /** Adaptive dead-zone radius in px; grows with smoothing level. */
    private val noiseGatePx: Float
        get() = 0.4f + smoothLevel * 1.6f

    /** EMA alpha for position: 1.0 = raw passthrough, lower = heavier smoothing. */
    private val positionAlpha: Float
        get() = (1.0f - smoothLevel * 0.85f).coerceIn(0.12f, 1.0f)

    /** EMA alpha for pressure channel. */
    private val pressureAlpha: Float
        get() = (1.0f - smoothLevel * 0.5f).coerceIn(0.25f, 1.0f)

    fun updateLevel(level: Float) {
        smoothLevel = level.coerceIn(0.0f, 1.0f)
    }

    fun reset() {
        initialized = false
        fx = 0f
        fy = 0f
        fPressure = 0.5f
    }

    /**
     * Filters one raw sample and returns the stabilized point.
     *
     * @param raw Incoming digitizer sample.
     * @param velocityPxMs Current stroke speed in px/ms, used to relax smoothing during fast
     *   motion (Star Notes keeps fast strokes snappy while slow strokes get full stabilization).
     */
    fun filter(raw: InkPoint, velocityPxMs: Float): InkPoint {
        if (!initialized) {
            initialized = true
            fx = raw.x
            fy = raw.y
            fPressure = raw.pressure
            return raw
        }

        val dist = hypot(raw.x - fx, raw.y - fy)

        // Stage 1: dead-zone noise gate — sub-pixel tremor is dropped entirely.
        if (dist < noiseGatePx) {
            return InkPoint(
                x = fx,
                y = fy,
                pressure = fPressure,
                tilt = raw.tilt,
                orientation = raw.orientation,
                timestamp = raw.timestamp,
            )
        }

        // Stage 2: position EMA.
        // Fast motion relaxes the filter (alpha -> 1) so stabilization never adds
        // perceptible lag while sprinting across the page.
        val speedRelax = (velocityPxMs / 3.0f).coerceIn(0.0f, 0.65f)
        val alpha = (positionAlpha + speedRelax).coerceAtMost(1.0f)

        val nx = fx + alpha * (raw.x - fx)
        val ny = fy + alpha * (raw.y - fy)

        // Lag compensation: re-inject a slice of the motion vector so the smoothed tip
        // converges on the true pen position instead of trailing behind it.
        val lagComp = smoothLevel * 0.25f
        val cx = nx + lagComp * (raw.x - nx)
        val cy = ny + lagComp * (raw.y - ny)

        // Stage 3: pressure EMA.
        fPressure += pressureAlpha * (raw.pressure - fPressure)

        fx = cx
        fy = cy

        return InkPoint(
            x = fx,
            y = fy,
            pressure = fPressure.coerceIn(0f, 1f),
            tilt = raw.tilt,
            orientation = raw.orientation,
            timestamp = raw.timestamp,
        )
    }

    /** Current filtered position (for tail flushing at pen-up). */
    val currentPosition: Pair<Float, Float>
        get() = fx to fy

    companion object {
        /** Convenience: filter a whole polyline (used by tests and vectorization paths). */
        fun filterAll(samples: List<InkPoint>, smoothLevel: Float): List<InkPoint> {
            val f = SmoothFilter(smoothLevel)
            return samples.mapIndexed { i, p ->
                val v = if (i == 0) 0f else {
                    val prev = samples[i - 1]
                    val dt = max(1L, p.timestamp - prev.timestamp)
                    hypot(p.x - prev.x, p.y - prev.y) / dt
                }
                f.filter(p, v)
            }
        }
    }
}
