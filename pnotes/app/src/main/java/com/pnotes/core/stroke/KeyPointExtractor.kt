package com.pnotes.core.stroke

import com.pnotes.core.geometry.Geometry
import com.pnotes.core.geometry.Pt
import kotlin.math.acos
import kotlin.math.max

/**
 * Extracts key inflection and peak bending control points from a dense path.
 * Ensures that recognized curves and shapes have only a clean, minimal set of
 * control handles (typically 3 to 5 points: Start, Peak Bends, and End) instead
 * of dozens of cluttered dots.
 */
object KeyPointExtractor {

    /**
     * Extracts significant bending/turning points plus intermediate curve guides from a dense path.
     * @param points The full sampled point list.
     * @param maxPoints Maximum total control points to return (default 6).
     * @return Ordered list of key control points (5 to 6 points) from start to end.
     */
    fun extractKeyPoints(points: List<Pt>, maxPoints: Int = 6): List<Pt> {
        if (points.size <= maxPoints) return points
        val first = points.first()
        val last = points.last()
        val n = points.size

        // Calculate perpendicular distance to the start-end chord and local curvature
        val deviations = DoubleArray(n)
        for (i in 1 until n - 1) {
            val p = points[i]
            val chordDist = Geometry.distancePointToSegment(p, first, last)
            val prev = points[i - 1]
            val next = points[i + 1]
            val v1 = p - prev
            val v2 = next - p
            val l1 = v1.length()
            val l2 = v2.length()
            val angle = if (l1 > 1e-6 && l2 > 1e-6) {
                val cosA = (v1.x * v2.x + v1.y * v2.y) / (l1 * l2)
                acos(cosA.coerceIn(-1.0, 1.0))
            } else 0.0
            deviations[i] = chordDist + angle * 20.0
        }

        // Find prominent bending peaks
        val candidateIndices = mutableListOf<Int>()
        for (i in 2 until n - 2) {
            if (deviations[i] >= deviations[i - 1] && deviations[i] >= deviations[i + 1] &&
                deviations[i] >= deviations[i - 2] && deviations[i] >= deviations[i + 2] &&
                deviations[i] > 1.0) {
                candidateIndices.add(i)
            }
        }

        // Ensure at least 3-4 intermediate points across the curve length
        if (candidateIndices.size < 4) {
            val targetSteps = 5
            for (s in 1 until targetSteps) {
                val idx = (n * s) / targetSteps
                if (idx !in candidateIndices && idx in 1 until n - 1) {
                    candidateIndices.add(idx)
                }
            }
        }

        // Sort candidate peaks by bending prominence
        val sortedByBending = candidateIndices.sortedByDescending { deviations[it] }
        val keptIndices = mutableListOf(0, n - 1)
        val chordLen = first.distanceTo(last)
        val minSpacing = max(16.0, chordLen / (maxPoints + 1))

        for (idx in sortedByBending) {
            val p = points[idx]
            val tooClose = keptIndices.any { points[it].distanceTo(p) < minSpacing }
            if (!tooClose) {
                keptIndices.add(idx)
                if (keptIndices.size >= maxPoints) break
            }
        }

        keptIndices.sort()
        return keptIndices.map { points[it] }
    }
}
