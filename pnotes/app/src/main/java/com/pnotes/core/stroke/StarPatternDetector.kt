package com.pnotes.core.stroke

import com.pnotes.core.geometry.Geometry
import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Result of star pattern analysis.
 * @param center The centroid of the star.
 * @param outerRadius The radius from center to the outer tips.
 * @param innerRadius The radius from center to the inner valleys.
 * @param rotationAngle The angle of the primary top tip in radians.
 * @param vertices The 10 ordered polygon vertices in content coordinates.
 */
data class StarMatch(
    val center: Pt,
    val outerRadius: Double,
    val innerRadius: Double,
    val rotationAngle: Double,
    val vertices: List<Pt>,
)

/**
 * High-precision detector for hand-drawn 5-pointed star gestures.
 * Recognizes:
 * 1. Classic continuous 5-line pentagrams (crossing strokes with 5 outer tips).
 * 2. Outline 10-vertex stars (radial polar signature with 5 outer tips and 5 inner valleys).
 */
object StarPatternDetector {

    private const val RESAMPLE_COUNT = 100
    private const val GOLDEN_STAR_RATIO = 0.382 // (3 - sqrt(5)) / 2
    private const val MIN_STAR_DIAGONAL = 24.0

    /**
     * Detects if the given sequence of points forms a 5-pointed star.
     * Returns a [StarMatch] with canonical 10-vertex polygon geometry if recognized, or null otherwise.
     */
    fun detectStar(points: List<Pt>): StarMatch? {
        if (points.size < 6) return null
        val bbox = Rect.bounding(points)
        val diag = hypot(bbox.w, bbox.h)
        if (diag < MIN_STAR_DIAGONAL) return null

        val path = resample(points, RESAMPLE_COUNT)
        val centroid = computeCentroid(path)

        // 1. Try continuous self-crossing pentagram star first
        detectPentagram(path, centroid, bbox)?.let { return it }

        // 2. Try outline 10-vertex star (polar radial profile)
        detectOutlineStar(path, centroid, bbox)?.let { return it }

        return null
    }

    // --- Method 1: Continuous Pentagram Star ---

    private fun detectPentagram(path: List<Pt>, centroid: Pt, bbox: Rect): StarMatch? {
        val n = path.size
        val k = 4
        val turns = DoubleArray(n)
        val sharpIndices = ArrayList<Int>()

        for (i in 0 until n) {
            val prev = path[(i - k + n) % n]
            val cur = path[i]
            val next = path[(i + k) % n]
            val v1 = cur - prev
            val v2 = next - cur
            val angle = angleBetween(v1, v2)
            turns[i] = angle
            if (angle >= 1.2) { // ~68 degrees+ turn
                sharpIndices.add(i)
            }
        }

        val peaks = clusterPeaks(turns, sharpIndices, n)
        if (peaks.size != 5) return null

        val tipPoints = peaks.map { path[it] }

        // Count segment crossings between the 5 tips
        var intersections = 0
        for (i in 0 until 5) {
            val a1 = tipPoints[i]
            val a2 = tipPoints[(i + 1) % 5]
            for (j in i + 2 until 5) {
                if (i == 0 && j == 4) continue
                val b1 = tipPoints[j]
                val b2 = tipPoints[(j + 1) % 5]
                if (segmentsIntersect(a1, a2, b1, b2)) {
                    intersections++
                }
            }
        }

        // A regular continuous pentagram has 5 self-crossings (permit 3+ for noisy hand drawings)
        if (intersections < 3) return null

        val outerRadius = tipPoints.map { it.distanceTo(centroid) }.average()
        val innerRadius = outerRadius * GOLDEN_STAR_RATIO

        // Find the top-most tip to align rotation
        val topTip = tipPoints.minByOrNull { it.y } ?: tipPoints[0]
        val rotationAngle = atan2(topTip.y - centroid.y, topTip.x - centroid.x)

        val vertices = generateStarVertices(centroid, outerRadius, innerRadius, rotationAngle)
        return StarMatch(centroid, outerRadius, innerRadius, rotationAngle, vertices)
    }

    // --- Method 2: Outline 10-Vertex Star ---

    private fun detectOutlineStar(path: List<Pt>, centroid: Pt, bbox: Rect): StarMatch? {
        val n = path.size
        val radii = DoubleArray(n) { path[it].distanceTo(centroid) }

        val window = 4
        val maxima = ArrayList<Int>()
        val minima = ArrayList<Int>()

        for (i in 0 until n) {
            var isMax = true
            var isMin = true
            val r = radii[i]
            for (w in 1..window) {
                val left = radii[(i - w + n) % n]
                val right = radii[(i + w) % n]
                if (left > r || right > r) isMax = false
                if (left < r || right < r) isMin = false
            }
            if (isMax) maxima.add(i)
            if (isMin) minima.add(i)
        }

        val clusteredMax = clusterIndices(maxima, radii, isMax = true, minSpacing = 6)
        val clusteredMin = clusterIndices(minima, radii, isMax = false, minSpacing = 6)

        if (clusteredMax.size != 5 || clusteredMin.size != 5) return null

        val meanMaxR = clusteredMax.map { radii[it] }.average()
        val meanMinR = clusteredMin.map { radii[it] }.average()
        val ratio = meanMinR / meanMaxR

        if (ratio !in 0.20..0.62) return null

        val topMaxIdx = clusteredMax.minByOrNull { path[it].y } ?: clusteredMax[0]
        val topTip = path[topMaxIdx]
        val rotationAngle = atan2(topTip.y - centroid.y, topTip.x - centroid.x)

        val vertices = generateStarVertices(centroid, meanMaxR, meanMinR, rotationAngle)
        return StarMatch(centroid, meanMaxR, meanMinR, rotationAngle, vertices)
    }

    /**
     * Generates a 10-vertex regular star polygon (alternating outer tips and inner valleys).
     */
    fun generateStarVertices(
        center: Pt,
        outerR: Double,
        innerR: Double,
        startAngleRad: Double = -PI / 2.0,
    ): List<Pt> {
        val verts = ArrayList<Pt>(10)
        val step = PI / 5.0 // 36 degrees per vertex step
        for (i in 0 until 10) {
            val angle = startAngleRad + i * step
            val r = if (i % 2 == 0) outerR else innerR
            verts.add(Pt(center.x + r * cos(angle), center.y + r * sin(angle)))
        }
        return verts
    }

    private fun computeCentroid(points: List<Pt>): Pt {
        var sx = 0.0
        var sy = 0.0
        for (p in points) {
            sx += p.x
            sy += p.y
        }
        return Pt(sx / points.size, sy / points.size)
    }

    private fun angleBetween(u: Pt, v: Pt): Double {
        val lu = hypot(u.x, u.y)
        val lv = hypot(v.x, v.y)
        if (lu < 1e-9 || lv < 1e-9) return 0.0
        val cosVal = ((u.x * v.x + u.y * v.y) / (lu * lv)).coerceIn(-1.0, 1.0)
        return acos(cosVal)
    }

    private fun segmentsIntersect(a1: Pt, a2: Pt, b1: Pt, b2: Pt): Boolean {
        fun ccw(p1: Pt, p2: Pt, p3: Pt): Double =
            (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x)

        val d1 = ccw(a1, a2, b1)
        val d2 = ccw(a1, a2, b2)
        val d3 = ccw(b1, b2, a1)
        val d4 = ccw(b1, b2, a2)

        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
    }

    private fun clusterPeaks(turns: DoubleArray, candidateIndices: List<Int>, n: Int): List<Int> {
        if (candidateIndices.isEmpty()) return emptyList()
        val peaks = ArrayList<Int>()
        var i = 0
        while (i < candidateIndices.size) {
            var bestIdx = candidateIndices[i]
            var bestTurn = turns[bestIdx]
            var j = i + 1
            while (j < candidateIndices.size && (candidateIndices[j] - candidateIndices[j - 1] <= 4)) {
                val idx = candidateIndices[j]
                if (turns[idx] > bestTurn) {
                    bestTurn = turns[idx]
                    bestIdx = idx
                }
                j++
            }
            peaks.add(bestIdx)
            i = j
        }
        return peaks
    }

    private fun clusterIndices(indices: List<Int>, values: DoubleArray, isMax: Boolean, minSpacing: Int): List<Int> {
        if (indices.isEmpty()) return emptyList()
        val clustered = ArrayList<Int>()
        var group = ArrayList<Int>()
        group.add(indices[0])

        for (i in 1 until indices.size) {
            val curr = indices[i]
            val prev = indices[i - 1]
            if (curr - prev <= minSpacing) {
                group.add(curr)
            } else {
                val best = if (isMax) group.maxByOrNull { values[it] }!! else group.minByOrNull { values[it] }!!
                clustered.add(best)
                group = ArrayList()
                group.add(curr)
            }
        }
        if (group.isNotEmpty()) {
            val best = if (isMax) group.maxByOrNull { values[it] }!! else group.minByOrNull { values[it] }!!
            clustered.add(best)
        }
        return clustered
    }

    private fun resample(points: List<Pt>, n: Int): List<Pt> {
        if (points.size <= 1 || n <= 1) return points
        var total = 0.0
        for (i in 1 until points.size) total += points[i - 1].distanceTo(points[i])
        if (total < 1e-9) return List(n) { points.first() }

        val step = total / (n - 1)
        val out = ArrayList<Pt>(n)
        out.add(points.first())
        var prev = points.first()
        var accum = 0.0
        var i = 1
        while (i < points.size && out.size < n) {
            val curr = points[i]
            val seg = prev.distanceTo(curr)
            if (seg < 1e-12) {
                i++
                continue
            }
            if (accum + seg >= step) {
                val t = (step - accum) / seg
                val np = Pt(prev.x + t * (curr.x - prev.x), prev.y + t * (curr.y - prev.y))
                out.add(np)
                prev = np
                accum = 0.0
            } else {
                accum += seg
                prev = curr
                i++
            }
        }
        while (out.size < n) out.add(points.last())
        return out
    }
}
