package com.pnotes.core.inking

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Kotlin port of the `neo3pen` spline family from Onyx's native ink engine
 * (`libneopen_jni.so`, Star Notes):
 *
 *  - `SplineProducer`          — turns filtered points into spline segments
 *  - `SplineInterpolator`      — Catmull-Rom -> cubic Bézier conversion
 *  - `SplineDistanceInterpolator`  — arc-length based evaluation
 *  - `SplineCurvatureInterpolator` — curvature-adaptive recursive subdivision
 *    (`subdivideRecursive`, `subdivideRecursiveXY` symbols in the .so)
 *
 * Native pipeline: SmoothFilter output -> SplineProducer -> SplinePointBlob ->
 * NeoStrokeRenderer. Here the cubic Bézier segments are appended to an Android
 * [android.graphics.Path]-compatible consumer, matching what the renderer draws.
 */
object SplineProducer {

    /**
     * A single cubic Bézier spline segment produced between two filtered input points.
     */
    data class SplineSegment(
        val startX: Float,
        val startY: Float,
        val control1X: Float,
        val control1Y: Float,
        val control2X: Float,
        val control2Y: Float,
        val endX: Float,
        val endY: Float,
        val startWidth: Float,
        val endWidth: Float,
        val alpha: Float,
        val point: InkPoint,
    )

    /**
     * Catmull-Rom to cubic Bézier conversion (uniform parameterization), the standard
     * construction the native `SplineInterpolator` uses: given window p0,p1,p2,p3 the
     * segment p1->p2 has control points offset along the tangents at p1 and p2.
     */
    fun catmullRomToBezier(
        p0: InkPoint,
        p1: InkPoint,
        p2: InkPoint,
        p3: InkPoint,
    ): SplineSegment {
        val c1x = p1.x + (p2.x - p0.x) / 6f
        val c1y = p1.y + (p2.y - p0.y) / 6f
        val c2x = p2.x - (p3.x - p1.x) / 6f
        val c2y = p2.y - (p3.y - p1.y) / 6f
        return SplineSegment(
            startX = p1.x, startY = p1.y,
            control1X = c1x, control1Y = c1y,
            control2X = c2x, control2Y = c2y,
            endX = p2.x, endY = p2.y,
            startWidth = p1.pressure, endWidth = p2.pressure, // widths set by caller
            alpha = 1f,
            point = p2,
        )
    }

    /**
     * Splits a cubic Bézier at parameter t=0.5 (de Casteljau). Used by the
     * curvature-adaptive subdivision pass.
     */
    private fun subdivide(
        p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
    ): Array<FloatArray> {
        val p01x = (p0[0] + p1[0]) * 0.5f; val p01y = (p0[1] + p1[1]) * 0.5f
        val p12x = (p1[0] + p2[0]) * 0.5f; val p12y = (p1[1] + p2[1]) * 0.5f
        val p23x = (p2[0] + p3[0]) * 0.5f; val p23y = (p2[1] + p3[1]) * 0.5f
        val p012x = (p01x + p12x) * 0.5f; val p012y = (p01y + p12y) * 0.5f
        val p123x = (p12x + p23x) * 0.5f; val p123y = (p12y + p23y) * 0.5f
        val pmx = (p012x + p123x) * 0.5f; val pmy = (p012y + p123y) * 0.5f
        return arrayOf(
            p0, floatArrayOf(p01x, p01y), floatArrayOf(p012x, p012y), floatArrayOf(pmx, pmy),
            floatArrayOf(pmx, pmy), floatArrayOf(p123x, p123y), floatArrayOf(p123x, p123y), p3,
        )
    }

    /** Flatness (control-polygon deviation) of a cubic segment — drives subdivision depth. */
    private fun flatness(p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray): Float {
        val ux = 3f * p1[0] - 2f * p0[0] - p3[0]; val uy = 3f * p1[1] - 2f * p0[1] - p3[1]
        val vx = 3f * p2[0] - 2f * p3[0] - p0[0]; val vy = 3f * p2[1] - 2f * p3[1] - p0[1]
        val u = maxOf(abs(ux), abs(uy))
        val v = maxOf(abs(vx), abs(vy))
        return maxOf(u, v)
    }

    /**
     * Curvature-adaptive recursive subdivision (`subdivideRecursiveXY` in the native lib):
     * subdivides until every segment is flat within [tolerance] px or max depth is hit.
     * Returns the flattened polyline for width-stable rendering.
     */
    fun subdivideAdaptive(
        p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
        tolerance: Float = 0.25f, depth: Int = 0, maxDepth: Int = 8,
        out: MutableList<FloatArray>,
    ) {
        if (depth >= maxDepth || flatness(p0, p1, p2, p3) <= tolerance) {
            out.add(p3)
            return
        }
        val halves = subdivide(p0, p1, p2, p3)
        subdivideAdaptive(halves[0], halves[1], halves[2], halves[3], tolerance, depth + 1, maxDepth, out)
        subdivideAdaptive(halves[4], halves[5], halves[6], halves[7], tolerance, depth + 1, maxDepth, out)
    }

    /**
     * Flattens a cubic segment into a polyline of points (first point excluded; caller
     * already holds it). Tolerance scales inversely with display density needs.
     */
    fun flattenCubic(
        startX: Float, startY: Float,
        c1x: Float, c1y: Float,
        c2x: Float, c2y: Float,
        endX: Float, endY: Float,
        tolerance: Float = 0.25f,
    ): List<FloatArray> {
        val out = mutableListOf<FloatArray>()
        subdivideAdaptive(
            floatArrayOf(startX, startY), floatArrayOf(c1x, c1y),
            floatArrayOf(c2x, c2y), floatArrayOf(endX, endY),
            tolerance, 0, 8, out,
        )
        return out
    }

    /**
     * Uniform Catmull-Rom evaluation of an open point list (padded with endpoint
     * duplicates), producing a cubic Bézier segment list — the [SplineProducer] stage.
     */
    fun produce(filtered: List<InkPoint>): List<SplineSegment> {
        if (filtered.size < 2) return emptyList()
        val pts = filtered
        val segments = mutableListOf<SplineSegment>()
        for (i in 0 until pts.size - 1) {
            val p0 = pts[if (i == 0) 0 else i - 1]
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val p3 = pts[if (i + 2 < pts.size) i + 2 else pts.size - 1]
            segments.add(catmullRomToBezier(p0, p1, p2, p3))
        }
        return segments
    }

    /** Total polyline length of filtered points, in px. */
    fun polylineLength(filtered: List<InkPoint>): Float {
        var len = 0f
        for (i in 1 until filtered.size) {
            len += hypot(filtered[i].x - filtered[i - 1].x, filtered[i].y - filtered[i - 1].y)
        }
        return len
    }

    /**
     * Resamples the filtered polyline at equal arc-length steps
     * (`SplineDistanceInterpolator` stage) — normalizes sampling density so width
     * modulation stays uniform regardless of digitizer event rate.
     */
    fun resampleByDistance(filtered: List<InkPoint>, stepPx: Float): List<InkPoint> {
        if (filtered.size < 2 || stepPx <= 0f) return filtered
        val out = mutableListOf<InkPoint>()
        var carry = 0f // distance already walked past the last emitted sample
        for (i in 1 until filtered.size) {
            val prev = filtered[i - 1]
            val curr = filtered[i]
            val segLen = hypot(curr.x - prev.x, curr.y - prev.y)
            if (segLen < 1e-6f) continue

            var d = carry
            while (d + stepPx <= segLen) {
                d += stepPx
                val f = d / segLen
                out.add(
                    InkPoint(
                        x = prev.x + (curr.x - prev.x) * f,
                        y = prev.y + (curr.y - prev.y) * f,
                        pressure = prev.pressure + (curr.pressure - prev.pressure) * f,
                        tilt = prev.tilt + (curr.tilt - prev.tilt) * f,
                        orientation = prev.orientation + (curr.orientation - prev.orientation) * f,
                        timestamp = prev.timestamp + ((curr.timestamp - prev.timestamp) * f).toLong(),
                    )
                )
            }
            carry = segLen - d
        }
        if (out.isEmpty()) return filtered
        out.add(filtered.last())
        return out
    }
}
