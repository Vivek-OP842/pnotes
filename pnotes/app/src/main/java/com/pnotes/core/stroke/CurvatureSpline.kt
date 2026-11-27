package com.pnotes.core.stroke

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Centripetal Catmull-Rom Curvature Spline & Adaptive Recursive Subdivider (Star Notes engine reference).
 *
 * Implements:
 * 1. Centripetal Catmull-Rom Spline with parameterization alpha = 0.5 (prevents cusps and self-intersections).
 * 2. Instantaneous Curvature Analysis: kappa = |x'y'' - y'x''| / (x'^2 + y'^2)^(3/2)
 * 3. Recursive Adaptive Subdivision: `subdivideRecursiveXY` dynamically adds micro-points where curvature is high,
 *    keeping straight segments minimal while guaranteeing silky smooth cursive loops and sharp hooks.
 */
object CurvatureSpline {

    /** Maximum subdivision recursion depth for ultra-smooth C1/C2 parametric spline interpolation */
    const val MAX_SUBDIVISION_DEPTH = 5

    /** Curvature threshold (radians of turning per pixel) to trigger adaptive binary subdivision */
    const val CURVATURE_TOLERANCE = 0.015

    /** Min step distance (pixels) below which subdivision stops */
    const val MIN_SUBDIVIDE_STEP = 0.35

    data class SplinePoint(
        val x: Double,
        val y: Double,
        val pressure: Double,
        val time: Double = 0.0,
        val tilt: Double = 0.0,
        val azimuth: Double = 0.0,
    )

    /**
     * Catmull-Rom knot interval calculation with centripetal parameterization (alpha = 0.5).
     */
    private fun knot(p0x: Double, p0y: Double, p1x: Double, p1y: Double, t0: Double, alpha: Double = 0.5): Double {
        val d = hypot(p1x - p0x, p1y - p0y)
        return t0 + if (d < 1e-6) 1e-3 else d.pow(alpha)
    }

    /**
     * Interpolates a point along the 4-point Catmull-Rom spline at parameter [t] in [t1, t2].
     */
    fun evaluateSpline(
        p0: SplinePoint,
        p1: SplinePoint,
        p2: SplinePoint,
        p3: SplinePoint,
        t: Double,
        t0: Double,
        t1: Double,
        t2: Double,
        t3: Double,
    ): SplinePoint {
        val a1x = (t1 - t) / (t1 - t0) * p0.x + (t - t0) / (t1 - t0) * p1.x
        val a1y = (t1 - t) / (t1 - t0) * p0.y + (t - t0) / (t1 - t0) * p1.y
        val a2x = (t2 - t) / (t2 - t1) * p1.x + (t - t1) / (t2 - t1) * p2.x
        val a2y = (t2 - t) / (t2 - t1) * p1.y + (t - t1) / (t2 - t1) * p2.y
        val a3x = (t3 - t) / (t3 - t2) * p2.x + (t - t2) / (t3 - t2) * p3.x
        val a3y = (t3 - t) / (t3 - t2) * p2.y + (t - t2) / (t3 - t2) * p3.y

        val b1x = (t2 - t) / (t2 - t0) * a1x + (t - t0) / (t2 - t0) * a2x
        val b1y = (t2 - t) / (t2 - t0) * a1y + (t - t0) / (t2 - t0) * a2y
        val b2x = (t3 - t) / (t3 - t1) * a2x + (t - t1) / (t3 - t1) * a3x
        val b2y = (t3 - t) / (t3 - t1) * a2y + (t - t1) / (t3 - t1) * a3y

        val cx = (t2 - t) / (t2 - t1) * b1x + (t - t1) / (t2 - t1) * b2x
        val cy = (t2 - t) / (t2 - t1) * b1y + (t - t1) / (t2 - t1) * b2y

        // Linear parameter blend for non-spatial channels
        val fraction = ((t - t1) / (t2 - t1)).coerceIn(0.0, 1.0)
        val cp = p1.pressure + (p2.pressure - p1.pressure) * fraction
        val ct = p1.time + (p2.time - p1.time) * fraction
        val cTilt = p1.tilt + (p2.tilt - p1.tilt) * fraction
        val cAzimuth = p1.azimuth + (p2.azimuth - p1.azimuth) * fraction

        return SplinePoint(cx, cy, cp, ct, cTilt, cAzimuth)
    }

    /**
     * Instantaneous curvature computation: kappa = |x' y'' - y' x''| / (x'^2 + y'^2)^(3/2)
     */
    fun curvature(
        p0: SplinePoint,
        p1: SplinePoint,
        p2: SplinePoint,
        p3: SplinePoint,
        t: Double,
        t0: Double,
        t1: Double,
        t2: Double,
        t3: Double,
    ): Double {
        val dt = 1e-3 * (t2 - t1)
        val tPrev = (t - dt).coerceAtLeast(t1)
        val tNext = (t + dt).coerceAtMost(t2)

        val pt = evaluateSpline(p0, p1, p2, p3, t, t0, t1, t2, t3)
        val ptPrev = evaluateSpline(p0, p1, p2, p3, tPrev, t0, t1, t2, t3)
        val ptNext = evaluateSpline(p0, p1, p2, p3, tNext, t0, t1, t2, t3)

        val dx1 = (pt.x - ptPrev.x) / dt
        val dy1 = (pt.y - ptPrev.y) / dt
        val dx2 = (ptNext.x - pt.x) / dt
        val dy2 = (ptNext.y - pt.y) / dt

        val vx = (dx1 + dx2) * 0.5
        val vy = (dy1 + dy2) * 0.5
        val ax = (dx2 - dx1) / dt
        val ay = (dy2 - dy1) / dt

        val speedSq = vx * vx + vy * vy
        if (speedSq < 1e-6) return 0.0
        val speed = sqrt(speedSq)
        val cross = abs(vx * ay - vy * ax)
        return cross / (speed * speedSq)
    }

    /**
     * Recursively subdivides segment [tA, tB] between P1 and P2 if curvature or deviation exceeds tolerance.
     */
    private fun subdivideRecursiveXY(
        p0: SplinePoint,
        p1: SplinePoint,
        p2: SplinePoint,
        p3: SplinePoint,
        tA: Double,
        tB: Double,
        t0: Double,
        t1: Double,
        t2: Double,
        t3: Double,
        depth: Int,
        out: MutableList<SplinePoint>,
    ) {
        val ptA = evaluateSpline(p0, p1, p2, p3, tA, t0, t1, t2, t3)
        val ptB = evaluateSpline(p0, p1, p2, p3, tB, t0, t1, t2, t3)
        val dist = hypot(ptB.x - ptA.x, ptB.y - ptA.y)

        if (depth >= MAX_SUBDIVISION_DEPTH || dist < MIN_SUBDIVIDE_STEP) {
            out.add(ptB)
            return
        }

        val tMid = 0.5 * (tA + tB)
        val ptMid = evaluateSpline(p0, p1, p2, p3, tMid, t0, t1, t2, t3)
        val kappaMid = curvature(p0, p1, p2, p3, tMid, t0, t1, t2, t3)

        // Flatness check: distance of midpoint from chord ptA -> ptB
        val chordLen = hypot(ptB.x - ptA.x, ptB.y - ptA.y)
        val dev = if (chordLen > 1e-4) {
            abs((ptB.y - ptA.y) * ptMid.x - (ptB.x - ptA.x) * ptMid.y + ptB.x * ptA.y - ptB.y * ptA.x) / chordLen
        } else {
            0.0
        }

        val dP = abs(ptB.pressure - ptA.pressure)

        if (kappaMid > CURVATURE_TOLERANCE || dev > 0.15 || dP > 0.02 || (dP > 0.008 && dist > 1.0)) {
            // High curvature / curved segment / rapid pressure taper: subdivide into two halves
            subdivideRecursiveXY(p0, p1, p2, p3, tA, tMid, t0, t1, t2, t3, depth + 1, out)
            subdivideRecursiveXY(p0, p1, p2, p3, tMid, tB, t0, t1, t2, t3, depth + 1, out)
        } else {
            out.add(ptB)
        }
    }

    /**
     * Transforms an input raw sample sequence into an adaptively curvature-subdivided spline point list.
     */
    fun smoothAndSubdivide(samples: List<SplinePoint>): List<SplinePoint> {
        val n = samples.size
        if (n < 3) return samples

        val result = mutableListOf<SplinePoint>()
        result.add(samples[0])

        for (i in 0 until n - 1) {
            result.addAll(subdivideSegmentPoints(p0At(samples, i), samples[i], samples[i + 1], p3At(samples, i)))
        }

        return result
    }

    /** Segment [i]'s leading knot: the previous sample, clamped at the stroke's head. */
    private fun p0At(samples: List<SplinePoint>, i: Int): SplinePoint =
        if (i > 0) samples[i - 1] else samples[0]

    /** Segment [i]'s trailing lookahead knot: two samples ahead, clamped at the stroke's tail. */
    private fun p3At(samples: List<SplinePoint>, i: Int): SplinePoint =
        if (i + 2 < samples.size) samples[i + 2] else samples[i + 1]

    /**
     * The subdivided polyline of the ONE segment [p1]→[p2], given its neighbouring knots [p0]/[p3],
     * EXCLUDING the segment's start knot (the previous segment's end already is that point) and
     * INCLUDING its end (the knot [p2], which the centripetal spline interpolates exactly). The
     * parameterization matches [smoothAndSubdivide] segment for segment, so running this per segment
     * as knots arrive — first segment with [p0] clamped onto [p1], last with [p3] clamped onto [p2] —
     * reproduces the whole-stroke polyline exactly, one final segment at a time. That equivalence is
     * what lets the live ribbon pump spline points causally while the committed stroke runs the
     * whole-stroke pass over the same raw samples.
     */
    fun subdivideSegmentPoints(p0: SplinePoint, p1: SplinePoint, p2: SplinePoint, p3: SplinePoint): List<SplinePoint> {
        val out = mutableListOf<SplinePoint>()
        val t0 = 0.0
        val t1 = knot(p0.x, p0.y, p1.x, p1.y, t0)
        val t2 = knot(p1.x, p1.y, p2.x, p2.y, t1)
        val t3 = knot(p2.x, p2.y, p3.x, p3.y, t2)
        subdivideRecursiveXY(p0, p1, p2, p3, t1, t2, t0, t1, t2, t3, 0, out)
        return out
    }
}
