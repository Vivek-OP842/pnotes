package com.pnotes.core.stroke

import kotlin.math.hypot
import kotlin.math.max

/**
 * Curve smoothing engine for freeform strokes and curvy patterns.
 * When the user draws a curve or pattern and holds (dwell gesture), this engine
 * filters out micro-jitter, hand tremors, and rough bumps into a clean, smooth,
 * organic curve while preserving the drawn shape and endpoints.
 */
object CurveSmoother {

    /**
     * Smooths an arbitrary freehand stroke path to remove hand jitter and bumps
     * while strictly preserving the drawn shape, bends, and curvature.
     * @param samples The raw stroke samples.
     * @param passes Number of Gaussian/Laplacian smoothing iterations (default 6).
     * @return The smoothed list of samples.
     */
    fun smooth(samples: List<Sample>, passes: Int = 6): List<Sample> {
        if (samples.size < 4) return samples

        // 1. Resample uniformly along arc length for even smoothing
        val resampled = resampleEquidistant(samples, step = 4.0)
        if (resampled.size < 4) return samples

        // 2. Multi-pass weighted Laplacian smoothing
        var current = resampled
        val n = current.size
        for (p in 0 until passes) {
            val next = ArrayList<Sample>(n)
            next.add(current.first())
            for (i in 1 until n - 1) {
                val prev = current[i - 1]
                val curr = current[i]
                val post = current[i + 1]
                val smoothX = 0.25 * prev.x + 0.50 * curr.x + 0.25 * post.x
                val smoothY = 0.25 * prev.y + 0.50 * curr.y + 0.25 * post.y
                val smoothP = 0.25 * prev.pressure + 0.50 * curr.pressure + 0.25 * post.pressure
                next.add(Sample(smoothX, smoothY, smoothP, curr.t))
            }
            next.add(current.last())
            current = next
        }

        // 3. Smooth closed loops seam if endpoints meet
        val first = current.first()
        val last = current.last()
        val isClosed = hypot(first.x - last.x, first.y - last.y) < 24.0
        if (isClosed && current.size >= 6) {
            val midX = (first.x + last.x) / 2.0
            val midY = (first.y + last.y) / 2.0
            val midP = (first.pressure + last.pressure) / 2.0
            val closedList = ArrayList<Sample>(current.size)
            closedList.add(Sample(midX, midY, midP, first.t))
            for (i in 1 until current.size - 1) {
                closedList.add(current[i])
            }
            closedList.add(Sample(midX, midY, midP, last.t))
            current = closedList
        }

        // 4. Fine sub-division with Catmull-Rom spline interpolation for maximum smoothness
        return fineSubdivide(current)
    }

    private fun resampleEquidistant(samples: List<Sample>, step: Double): List<Sample> {
        val n = samples.size
        if (n < 2) return samples
        val cumDist = DoubleArray(n)
        for (i in 1 until n) {
            val d = hypot(samples[i].x - samples[i - 1].x, samples[i].y - samples[i - 1].y)
            cumDist[i] = cumDist[i - 1] + d
        }
        val totalLen = cumDist[n - 1]
        if (totalLen < 1e-4) return samples

        val count = max(8, (totalLen / step).toInt())
        val result = ArrayList<Sample>(count + 1)
        result.add(samples.first())

        var currIdx = 0
        for (k in 1 until count) {
            val targetDist = (k.toDouble() / count) * totalLen
            while (currIdx < n - 1 && cumDist[currIdx + 1] < targetDist) {
                currIdx++
            }
            if (currIdx >= n - 1) break
            val segLen = cumDist[currIdx + 1] - cumDist[currIdx]
            val frac = if (segLen > 1e-6) ((targetDist - cumDist[currIdx]) / segLen).coerceIn(0.0, 1.0) else 0.0
            val s0 = samples[currIdx]
            val s1 = samples[currIdx + 1]
            val x = s0.x + frac * (s1.x - s0.x)
            val y = s0.y + frac * (s1.y - s0.y)
            val p = s0.pressure + frac * (s1.pressure - s0.pressure)
            val t = s0.t + frac * (s1.t - s0.t)
            result.add(Sample(x, y, p, t))
        }
        result.add(samples.last())
        return result
    }

    private fun fineSubdivide(samples: List<Sample>): List<Sample> {
        val n = samples.size
        if (n < 4) return samples
        val result = ArrayList<Sample>(n * 2)

        val pExt = ArrayList<Sample>(n + 2)
        val s0 = samples[0]
        val s1 = samples[1]
        val s2 = samples[2]
        val p0Ext = Sample(s0.x * 3.0 - s1.x * 3.0 + s2.x, s0.y * 3.0 - s1.y * 3.0 + s2.y, s0.pressure, s0.t)
        pExt.add(p0Ext)
        pExt.addAll(samples)
        val sn = samples[n - 1]
        val sn1 = samples[n - 2]
        val sn2 = samples[n - 3]
        val pnExt = Sample(sn.x * 3.0 - sn1.x * 3.0 + sn2.x, sn.y * 3.0 - sn1.y * 3.0 + sn2.y, sn.pressure, sn.t)
        pExt.add(pnExt)

        val steps = 2
        for (i in 0 until n - 1) {
            val q0 = pExt[i]
            val q1 = pExt[i + 1]
            val q2 = pExt[i + 2]
            val q3 = pExt[i + 3]

            for (s in 0 until steps) {
                val t = s.toDouble() / steps
                val t2 = t * t
                val t3 = t2 * t
                val x = 0.5 * (
                    (2.0 * q1.x) +
                    (-q0.x + q2.x) * t +
                    (2.0 * q0.x - 5.0 * q1.x + 4.0 * q2.x - q3.x) * t2 +
                    (-q0.x + 3.0 * q1.x - 3.0 * q2.x + q3.x) * t3
                )
                val y = 0.5 * (
                    (2.0 * q1.y) +
                    (-q0.y + q2.y) * t +
                    (2.0 * q0.y - 5.0 * q1.y + 4.0 * q2.y - q3.y) * t2 +
                    (-q0.y + 3.0 * q1.y - 3.0 * q2.y + q3.y) * t3
                )
                val p = q1.pressure + t * (q2.pressure - q1.pressure)
                val time = q1.t + t * (q2.t - q1.t)
                result.add(Sample(x, y, p, time))
            }
        }
        result.add(samples.last())
        return result
    }
}
