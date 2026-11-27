package com.pnotes.core.stroke

import com.pnotes.core.geometry.Pt
import com.pnotes.core.infinite.MeshData
import com.pnotes.core.infinite.StrokeTessellator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * The predicted tail is the part of a live stroke that is drawn but not yet real. It is appended to
 * the ribbon as a continuation of the last real point, so it has to behave like more ribbon: same
 * half-width, rails of its own, and a hard stop on anything a predictor glitch might invent.
 */
class LiveTailRibbonTest {

    /** A straight diagonal run of [count] points, [step] apart, half-width `1.5`. */
    private class LineRibbon(count: Int, step: Double = 4.0) : RibbonPoints {
        private val px = DoubleArray(count) { it * step }
        private val py = DoubleArray(count) { it * step * 0.5 }
        override val pointCount = count
        override val hasRails = true
        override fun cx(i: Int) = px[i]
        override fun cy(i: Int) = py[i]
        override fun hw(i: Int) = 1.5
        override fun leftX(i: Int) = px[i] - 1.0
        override fun leftY(i: Int) = py[i] + 1.0
        override fun rightX(i: Int) = px[i] + 1.0
        override fun rightY(i: Int) = py[i] - 1.0
    }

    private fun tail(vararg xy: Double): List<Pt> =
        (0 until xy.size / 2).map { Pt(xy[2 * it], xy[2 * it + 1]) }

    @Test fun theTailIsCountedAfterTheRealPoints() {
        val base = LineRibbon(4)
        val live = LiveTailRibbon(base, from = 0, tail = tail(18.0, 9.0, 22.0, 11.0))
        assertEquals(6, live.pointCount)
        assertEquals(2, live.tailPoints().size)
        // The real points are delegated unchanged.
        for (i in 0 until 4) {
            assertEquals(base.cx(i), live.cx(i), 0.0)
            assertEquals(base.cy(i), live.cy(i), 0.0)
            assertEquals(base.hw(i), live.hw(i), 0.0)
        }
    }

    @Test fun fromLeavesTheSettledHeadOut() {
        val base = LineRibbon(6)
        val live = LiveTailRibbon(base, from = 4, tail = tail(18.0, 9.0, 22.0, 11.0))
        assertEquals(4, live.pointCount)
        assertEquals(base.cx(4), live.cx(0), 0.0)
        assertEquals(base.cx(5), live.cx(1), 0.0)
        // The appended points start where the view starts, not at index 0 of the base.
        assertEquals(18.0, live.cx(2), 0.0)
    }

    @Test fun aTailPointOnTopOfTheTipIsNotProgress() {
        val base = LineRibbon(4)
        val lastX = base.cx(3)
        val lastY = base.cy(3)
        // The first point is the anchor itself; the second is within the 0.2px step floor.
        val live = LiveTailRibbon(
            base,
            from = 0,
            tail = tail(lastX, lastY, lastX + 0.1, lastY, 18.0, 9.0),
        )
        assertEquals(listOf(Pt(18.0, 9.0)), live.tailPoints())
        assertEquals(5, live.pointCount)
    }

    @Test fun aGlitchBeyondTheCapEndsTheTail() {
        val base = LineRibbon(4)
        val tipX = base.cx(3)
        val tipY = base.cy(3)
        // 10px ahead is fine, the 200px jump is not, and nothing after the jump is drawn: a
        // predictor that overshoots must not leave a line across the page.
        val live = LiveTailRibbon(
            base,
            from = 0,
            tail = tail(tipX + 10.0, tipY, tipX + 200.0, tipY, tipX + 15.0, tipY),
            maxTailDist = 64.0,
        )
        assertEquals(1, live.tailPoints().size)
        assertEquals(tipX + 10.0, live.cx(live.pointCount - 1), 0.0)
    }

    @Test fun aNonFiniteTailPointIsSkippedNotFatal() {
        val base = LineRibbon(4)
        val tipX = base.cx(3)
        val tipY = base.cy(3)
        val live = LiveTailRibbon(
            base,
            from = 0,
            tail = listOf(Pt(Double.NaN, tipY), Pt(tipX + 8.0, tipY)),
        )
        assertEquals(listOf(Pt(tipX + 8.0, tipY)), live.tailPoints())
    }

    @Test fun theTailKeepsTheLastRealHalfWidth() {
        val base = LineRibbon(4)
        val live = LiveTailRibbon(base, from = 0, tail = tail(18.0, 9.0))
        assertEquals(1.5, live.hw(4), 0.0)
        assertEquals(1.5, live.hw(0), 0.0)
    }

    @Test fun railsArePerpendicularToTheTailDirection() {
        val base = LineRibbon(4) // heading right and down
        val live = LiveTailRibbon(base, from = 0, tail = tail(18.0, 9.0, 22.0, 11.0))
        for (i in 0 until live.pointCount) {
            val railDx = live.leftX(i) - live.cx(i)
            val railDy = live.leftY(i) - live.cy(i)
            assertEquals("rail length at $i", live.hw(i), hypot(railDx, railDy), 1e-9)
            // A straight diagonal has one direction, so every segment normal is the same up to sign.
            if (i > 0) {
                val dx = live.cx(i) - live.cx(i - 1)
                val dy = live.cy(i) - live.cy(i - 1)
                assertEquals("rail is perpendicular at $i", 0.0, railDx * dx + railDy * dy, 1e-6)
            }
        }
    }

    @Test fun aSingleRealPointWithATailIsARibbonNotADot() {
        val base = LineRibbon(1)
        assertFalse(LiveTailRibbon(base, from = 0, tail = emptyList()).hasRails)
        assertTrue(LiveTailRibbon(base, from = 0, tail = tail(4.0, 2.0)).hasRails)
    }

    @Test fun theExtendedRibbonMeshesAndReachesThePredictedTip() {
        val base = LineRibbon(8)
        val tipX = base.cx(7)
        val tipY = base.cy(7)
        val live = LiveTailRibbon(base, from = 0, tail = tail(tipX + 4.0, tipY + 2.0, tipX + 8.0, tipY + 4.0))

        val mesh = StrokeTessellator.tessellate(live, StrokeTessellator.DEFAULT_TOLERANCE)
        assertTrue("the extended tail produced no triangles", mesh.triangleCount > 0)

        // The end cap is drawn about the predicted tip, so a point on its centreline must be covered.
        val last = live.pointCount - 1
        assertTrue(covers(mesh, live.cx(last), live.cy(last)))
        // And the join is continuous: the midpoint between the last real and first predicted point.
        assertTrue(covers(mesh, (base.cx(7) + tipX + 4.0) / 2, (base.cy(7) + tipY + 2.0) / 2))
    }

    @Test fun theCapIsMeasuredFromTheLastRealSampleNotTheStartOfTheView() {
        val base = LineRibbon(10)
        val tipX = base.cx(9)
        val tipY = base.cy(9)
        // from=8 puts two real points in the view; the distance budget still starts at the tip.
        val live = LiveTailRibbon(
            base,
            from = 8,
            tail = tail(tipX + 40.0, tipY, tipX + 80.0, tipY),
            maxTailDist = 64.0,
        )
        assertEquals(listOf(Pt(tipX + 40.0, tipY)), live.tailPoints())
        assertTrue(abs(live.tailPoints().first().x - tipX) < 41.0)
    }

    /** Even-odd triangle containment, enough for the thin ribbon meshes here. */
    private fun covers(mesh: MeshData, px: Double, py: Double): Boolean {
        for (t in 0 until mesh.triangleCount) {
            val ia = mesh.indices[3 * t]
            val ib = mesh.indices[3 * t + 1]
            val ic = mesh.indices[3 * t + 2]
            val ax = mesh.positions[2 * ia]
            val ay = mesh.positions[2 * ia + 1]
            val bx = mesh.positions[2 * ib]
            val by = mesh.positions[2 * ib + 1]
            val cx = mesh.positions[2 * ic]
            val cy = mesh.positions[2 * ic + 1]
            val d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by)
            val d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy)
            val d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay)
            val neg = d1 < 0 || d2 < 0 || d3 < 0
            val pos = d1 > 0 || d2 > 0 || d3 > 0
            if (!(neg && pos)) return true
        }
        return false
    }
}
