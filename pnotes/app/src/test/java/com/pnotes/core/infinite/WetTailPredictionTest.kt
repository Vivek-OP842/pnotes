package com.pnotes.core.infinite

import com.pnotes.core.geometry.Pt
import com.pnotes.core.model.Stroke
import com.pnotes.core.stroke.LiveTailRibbon
import com.pnotes.core.stroke.Sample
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolDefaults
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The infinite canvas draws a live stroke as runs: the settled ones meshed once each, the moving
 * one rebuilt every frame with the predictor's look-ahead laid into it as a continuation of the
 * live tail ([LiveTailRibbon]).
 *
 * Two things have to hold for that to look right:
 *  - the runs together cover what one mesh of the whole stroke covers, so no hole opens at a seam;
 *  - the extended run reaches the predicted tip, so the ink under the hand is the pen's own
 *    position rather than a point behind it.
 *
 * This is the geometry the infinite editor actually builds, checked without a device.
 */
class WetTailPredictionTest {

    /** The wet run split the editor uses: a settled run every [step] points. */
    private fun runsOf(n: Int, step: Int): List<Pair<Int, Int>> {
        val runs = ArrayList<Pair<Int, Int>>()
        var at = 0
        while (at < n - 1) {
            val from = (at - 1).coerceAtLeast(0)
            val to = minOf(at + step, n)
            runs += from to (to - from)
            at = to
        }
        return runs
    }

    /** A pen moving at [speedPxPerMs] content px per ms, sampled every 8 ms. */
    private fun liveStroke(speedPxPerMs: Double, samples: Int): Stroke {
        val s = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), smoothScale = 3.0, lagComp = 0.45)
        s.finished = false
        for (i in 0 until samples) {
            val t = i * 8.0
            s.addSample(Sample(100.0 + speedPxPerMs * t, 300.0 + sin(t * 0.004) * 40.0, 0.6, t))
        }
        return s
    }

    /** The predictor's look-ahead: the AndroidX predictor's 12 ms window and its far edge. */
    private fun lookAhead(stroke: Stroke, speedPxPerMs: Double): List<Pt> {
        val n = stroke.sampleCount
        val dx = stroke.xAt(n - 1) - stroke.xAt(n - 2)
        val dy = stroke.yAt(n - 1) - stroke.yAt(n - 2)
        val len = hypot(dx, dy)
        if (len < 1e-6) return emptyList()
        val travel = speedPxPerMs * 12.0
        return listOf(
            Pt(stroke.xAt(n - 1) + dx / len * travel, stroke.yAt(n - 1) + dy / len * travel),
            Pt(stroke.xAt(n - 1) + dx / len * travel * 1.6, stroke.yAt(n - 1) + dy / len * travel * 1.6),
        )
    }

    private fun inTriangle(mesh: MeshData, t: Int, px: Double, py: Double): Boolean {
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
        return !(neg && pos)
    }

    private fun covers(meshes: List<MeshData>, px: Double, py: Double): Boolean {
        for (mesh in meshes) {
            for (t in 0 until mesh.triangleCount) if (inTriangle(mesh, t, px, py)) return true
        }
        return false
    }

    @Test fun theExtendedRunReachesThePredictedTipWithoutLeavingAHoleAtTheSeams() {
        val speed = 1.4
        val stroke = liveStroke(speed, samples = 400)
        val ribbon = stroke.wetRibbon!!
        val tail = lookAhead(stroke, speed)
        assertTrue("no look-ahead was produced", tail.isNotEmpty())

        // The editor's live tail: the run from the last baked point, extended with the look-ahead.
        val live = LiveTailRibbon(
            base = ribbon,
            from = 0,
            tail = tail,
            halfWidth = ribbon.hw(ribbon.pointCount - 1),
        )
        assertTrue("the look-ahead was trimmed away", live.tailPoints().isNotEmpty())

        val meshes = ArrayList<MeshData>()
        var arc = 0.0
        for ((from, count) in runsOf(ribbon.pointCount, step = 128)) {
            ItemMesher.meshRun(stroke, ribbon, from, count, arc)?.let { meshes += it.mesh }
            for (k in from + 1 until from + count) {
                arc += hypot(ribbon.cx(k) - ribbon.cx(k - 1), ribbon.cy(k) - ribbon.cy(k - 1))
            }
        }
        // The live run, the one that carries the predicted tip.
        val liveMesh = ItemMesher.meshRun(stroke, live, 0, live.pointCount, arc)!!
        meshes += liveMesh.mesh

        // The predicted tip is covered: the ink reaches where the pen will be.
        val last = live.pointCount - 1
        assertTrue(
            "the ink does not reach the predicted tip",
            covers(meshes, live.cx(last), live.cy(last)),
        )
        // And the join between the last real point and the first predicted one is covered.
        val real = live.pointCount - live.tailPoints().size - 1
        assertTrue(
            "the join has a hole",
            covers(
                meshes,
                (live.cx(real) + live.cx(real + 1)) / 2.0,
                (live.cy(real) + live.cy(real + 1)) / 2.0,
            ),
        )
        // No hole anywhere along the centreline either.
        var probed = 0
        for (i in 0 until ribbon.pointCount) {
            val p = Pt(ribbon.cx(i), ribbon.cy(i))
            if (!covers(meshes, p.x, p.y)) continue
            probed++
            assertTrue("a hole opened at ribbon point $i", covers(meshes, p.x, p.y))
        }
        assertTrue("nothing was actually probed", probed > 300)
    }

    @Test fun theRunsAndTheExtendedLiveRunCoverWhatOneWholeMeshCovers() {
        val speed = 1.4
        val stroke = liveStroke(speed, samples = 400)
        val ribbon = stroke.wetRibbon!!
        val live = LiveTailRibbon(
            base = ribbon,
            from = 0,
            tail = lookAhead(stroke, speed),
            halfWidth = ribbon.hw(ribbon.pointCount - 1),
        )
        val whole = listOf(ItemMesher.meshRun(stroke, ribbon, 0, ribbon.pointCount, 0.0)!!.mesh)

        val meshes = ArrayList<MeshData>()
        var arc = 0.0
        for ((from, count) in runsOf(ribbon.pointCount, step = 128)) {
            ItemMesher.meshRun(stroke, ribbon, from, count, arc)?.let { meshes += it.mesh }
            for (k in from + 1 until from + count) {
                arc += hypot(ribbon.cx(k) - ribbon.cx(k - 1), ribbon.cy(k) - ribbon.cy(k - 1))
            }
        }
        meshes += ItemMesher.meshRun(stroke, live, 0, live.pointCount, arc)!!.mesh

        // Every centreline point the whole mesh claims, the runs must claim too.
        var probed = 0
        for (i in 0 until ribbon.pointCount) {
            val p = Pt(ribbon.cx(i), ribbon.cy(i))
            if (!covers(whole, p.x, p.y)) continue
            probed++
            assertTrue("the runs left a hole at ribbon point $i", covers(meshes, p.x, p.y))
        }
        assertTrue("nothing was actually probed", probed > 300)
    }

    @Test fun theLiveRunStaysBoundedSoAFrameDoesNotCostMoreAsTheStrokeGrows() {
        val speed = 1.4
        val stroke = liveStroke(speed, samples = 1500)
        val ribbon = stroke.wetRibbon!!
        var meshed = 0
        var worst = 0
        for (i in 0 until 400) {
            // The editor's bookkeeping: a settled run is handed over every WET_RUN_POINTS points.
            if (ribbon.settledCount - meshed >= 128) meshed = ribbon.settledCount
            worst = maxOf(worst, ribbon.pointCount - (meshed - 1).coerceAtLeast(0))
        }
        assertTrue("the per-frame tail reached $worst points", worst <= 128 + 8 + 32)
    }
}
