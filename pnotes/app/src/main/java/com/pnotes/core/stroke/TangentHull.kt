package com.pnotes.core.stroke

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Exact 2-Circle Bitangent Convex Hull Generator (Star Notes / Onyx NeoPen reference).
 *
 * Given two variable-radius circles C1(x1, y1, r1) and C2(x2, y2, r2), evaluates the exact outer
 * common tangents rather than simple orthogonal normal offsets:
 *   sin(alpha) = (r1 - r2) / distance
 *   theta = atan2(y2 - y1, x2 - x1)
 *
 * Left tangent angles: theta + PI/2 + alpha
 * Right tangent angles: theta - PI/2 - alpha
 *
 * This completely prevents inner self-intersections and eliminate scalloping/beading artifacts
 * when radius changes rapidly along dynamic pressure/velocity transitions.
 */
object TangentHull {

    /**
     * Evaluates the 4 outer tangent contact vertices between C1 and C2.
     * Output float array of size 8:
     * [L1x, L1y, L2x, L2y, R2x, R2y, R1x, R1y] defining a clockwise or counter-clockwise convex quad.
     */
    fun computeBitangentQuad(
        x1: Double,
        y1: Double,
        r1: Double,
        x2: Double,
        y2: Double,
        r2: Double,
        out: FloatArray,
        outOffset: Int = 0,
    ): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val dist = hypot(dx, dy)

        val dr = r1 - r2
        if (dist < 1e-4) {
            // Degenerate / coincident points -> use default orthogonal offsets
            out[outOffset + 0] = (x1 - r1).toFloat()
            out[outOffset + 1] = (y1).toFloat()
            out[outOffset + 2] = (x2 - r2).toFloat()
            out[outOffset + 3] = (y2).toFloat()
            out[outOffset + 4] = (x2 + r2).toFloat()
            out[outOffset + 5] = (y2).toFloat()
            out[outOffset + 6] = (x1 + r1).toFloat()
            out[outOffset + 7] = (y1).toFloat()
            return false
        }

        val theta = atan2(dy, dx)
        val sinAlpha = (dr / dist).coerceIn(-1.0, 1.0)
        val alpha = asin(sinAlpha)

        // In screen coordinates (+y is down):
        // Left side is theta - PI/2 + alpha (screen top)
        // Right side is theta + PI/2 - alpha (screen bottom)
        val leftAngle = theta + Math.PI / 2.0 - alpha
        val rightAngle = theta - Math.PI / 2.0 + alpha

        val cosL = cos(leftAngle)
        val sinL = sin(leftAngle)
        val cosR = cos(rightAngle)
        val sinR = sin(rightAngle)

        // L1 on Circle 1
        out[outOffset + 0] = (x1 + r1 * cosL).toFloat()
        out[outOffset + 1] = (y1 + r1 * sinL).toFloat()
        // L2 on Circle 2
        out[outOffset + 2] = (x2 + r2 * cosL).toFloat()
        out[outOffset + 3] = (y2 + r2 * sinL).toFloat()
        // R2 on Circle 2
        out[outOffset + 4] = (x2 + r2 * cosR).toFloat()
        out[outOffset + 5] = (y2 + r2 * sinR).toFloat()
        // R1 on Circle 1
        out[outOffset + 6] = (x1 + r1 * cosR).toFloat()
        out[outOffset + 7] = (y1 + r1 * sinR).toFloat()

        return true
    }

    /**
     * Builds continuous left and right rails using exact bitangent contact points across an entire centerline.
     */
    fun computeRails(
        centers: FloatArray,
        radii: FloatArray,
        count: Int,
        outLeft: FloatArray,
        outRight: FloatArray,
    ) {
        if (count <= 0) return
        if (count == 1) {
            val x = centers[0].toDouble()
            val y = centers[1].toDouble()
            val r = radii[0].toDouble()
            outLeft[0] = (x - r).toFloat()
            outLeft[1] = y.toFloat()
            outRight[0] = (x + r).toFloat()
            outRight[1] = y.toFloat()
            return
        }

        val quad = FloatArray(8)
        for (i in 0 until count - 1) {
            val x1 = centers[2 * i].toDouble()
            val y1 = centers[2 * i + 1].toDouble()
            val r1 = radii[i].toDouble()

            val x2 = centers[2 * (i + 1)].toDouble()
            val y2 = centers[2 * (i + 1) + 1].toDouble()
            val r2 = radii[i + 1].toDouble()

            computeBitangentQuad(x1, y1, r1, x2, y2, r2, quad, 0)

            if (i == 0) {
                outLeft[0] = quad[0]
                outLeft[1] = quad[1]
                outRight[0] = quad[6]
                outRight[1] = quad[7]
            }

            outLeft[2 * (i + 1)] = quad[2]
            outLeft[2 * (i + 1) + 1] = quad[3]
            outRight[2 * (i + 1)] = quad[4]
            outRight[2 * (i + 1) + 1] = quad[5]
        }
    }
}
