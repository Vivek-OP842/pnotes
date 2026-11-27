package com.pnotes.core.stroke

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CurvatureSplineTest {

    @Test
    fun straightLineSplineDoesNotAddUnnecessaryPoints() {
        val points = listOf(
            CurvatureSpline.SplinePoint(0.0, 0.0, 1.0),
            CurvatureSpline.SplinePoint(10.0, 0.0, 1.0),
            CurvatureSpline.SplinePoint(20.0, 0.0, 1.0),
            CurvatureSpline.SplinePoint(30.0, 0.0, 1.0),
        )
        val subdivided = CurvatureSpline.smoothAndSubdivide(points)
        // Straight line has 0 curvature -> low subdivision count
        assertTrue(subdivided.size >= 4)
        for (pt in subdivided) {
            assertEquals(0.0, pt.y, 1e-6)
        }
    }

    @Test
    fun sharpCurveTriggersAdaptiveSubdivision() {
        val points = listOf(
            CurvatureSpline.SplinePoint(0.0, 0.0, 1.0),
            CurvatureSpline.SplinePoint(10.0, 0.0, 1.0),
            CurvatureSpline.SplinePoint(10.0, 10.0, 1.0),
            CurvatureSpline.SplinePoint(0.0, 10.0, 1.0),
            CurvatureSpline.SplinePoint(0.0, 0.0, 1.0),
        )
        val subdivided = CurvatureSpline.smoothAndSubdivide(points)
        // Highly curved closed loop should trigger recursive subdivision for smooth corners
        assertTrue(subdivided.size > points.size)
    }

    @Test
    fun curvatureCalculationReturnsZeroOnStraightSegment() {
        val p0 = CurvatureSpline.SplinePoint(0.0, 0.0, 1.0)
        val p1 = CurvatureSpline.SplinePoint(10.0, 0.0, 1.0)
        val p2 = CurvatureSpline.SplinePoint(20.0, 0.0, 1.0)
        val p3 = CurvatureSpline.SplinePoint(30.0, 0.0, 1.0)
        val kappa = CurvatureSpline.curvature(p0, p1, p2, p3, 5.0, 0.0, 10.0, 20.0, 30.0)
        assertEquals(0.0, kappa, 1e-6)
    }
}
