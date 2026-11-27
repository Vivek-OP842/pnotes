package com.pnotes.core.stroke

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class TangentHullTest {

    @Test
    fun equalRadiiProduceOrthogonalTangents() {
        val quad = FloatArray(8)
        val success = TangentHull.computeBitangentQuad(0.0, 0.0, 5.0, 20.0, 0.0, 5.0, quad, 0)
        assertTrue(success)
        // With horizontal line and equal radii, tangents are horizontal offsets: y = +5 and y = -5
        assertEquals(0.0f, quad[0], 1e-4f)  // L1 x
        assertEquals(5.0f, quad[1], 1e-4f)  // L1 y
        assertEquals(20.0f, quad[2], 1e-4f) // L2 x
        assertEquals(5.0f, quad[3], 1e-4f)  // L2 y
        assertEquals(20.0f, quad[4], 1e-4f) // R2 x
        assertEquals(-5.0f, quad[5], 1e-4f) // R2 y
        assertEquals(0.0f, quad[6], 1e-4f)  // R1 x
        assertEquals(-5.0f, quad[7], 1e-4f) // R1 y
    }

    @Test
    fun unequalRadiiAngleInward() {
        val quad = FloatArray(8)
        // r1 = 10, r2 = 2 over distance 20: dr/dist = 8/20 = 0.4 = sin(alpha)
        val success = TangentHull.computeBitangentQuad(0.0, 0.0, 10.0, 20.0, 0.0, 2.0, quad, 0)
        assertTrue(success)
        // L2 y must be positive, R2 y must be negative
        assertTrue(quad[1] > 0)
        assertTrue(quad[3] > 0)
        assertTrue(quad[5] < 0)
        assertTrue(quad[7] < 0)
        // L1 y < r1 because alpha tilts the normal backward towards center 1
        assertTrue(quad[1] < 10.0f)
    }

    @Test
    fun computeRailsProducesMatchingSizes() {
        val centers = floatArrayOf(0f, 0f, 10f, 0f, 20f, 0f)
        val radii = floatArrayOf(2f, 3f, 4f)
        val left = FloatArray(6)
        val right = FloatArray(6)
        TangentHull.computeRails(centers, radii, 3, left, right)
        // Check rails exist and have non-zero separation
        for (i in 0 until 3) {
            val dist = hypot(left[2 * i] - right[2 * i], left[2 * i + 1] - right[2 * i + 1])
            assertTrue(dist > 0.0f)
        }
    }
}
