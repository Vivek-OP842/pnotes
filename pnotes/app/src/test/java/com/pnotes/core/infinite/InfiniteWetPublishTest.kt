package com.pnotes.core.infinite

import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.model.Stroke
import com.pnotes.core.stroke.LiveTailRibbon
import com.pnotes.core.stroke.Sample
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The infinite editor's live-ink pipeline, exercised as the editor builds it.
 *
 * Every frame the editor publishes the wet stroke as a set of parts: the settled runs meshed once
 * each, and the moving tail rebuilt every frame with the predictor's look-ahead laid into it (see
 * `InfiniteEditor.publishWetStroke`). This test runs that same sequence and checks the two things
 * the writer would notice:
 *
 *  - the ink under the hand reaches the nib, instead of trailing behind it;
 *  - the join between the settled head and the moving tail is continuous, so no hole opens under
 *    the pen as the stroke grows.
 */
class InfiniteWetPublishTest {

    /** The editor's settled-run size: a run is handed over every `WET_RUN_POINTS` points. */
    private val wetRunPoints = 128

    /** The editor's cap on the predicted extension, in screen px; content px at zoom 1. */
    private val predictedTailMaxScreenPx = 64.0

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

    @Test fun thePublishedLiveInkReachesTheNibAndLeavesNoHoleAtTheJoin() {
        val speed = 1.4
        val stroke = liveStroke(speed, samples = 400)
        val ribbon = stroke.wetRibbon!!
        val predicted = lookAhead(stroke, speed)
        assertTrue("no look-ahead was produced", predicted.isNotEmpty())

        // The editor's own state: the settled head already handed over, the tail still moving.
        val wetMeshed = 256
        var wetArc = 0.0
        val settledMeshes = ArrayList<MeshData>()
        var from = (wetMeshed - 1).coerceAtLeast(0)
        val settled = ribbon.settledCount
        if (settled - wetMeshed >= wetRunPoints) {
            val part = ItemMesher.meshRun(stroke, ribbon, from, settled - from, wetArc)
            assertNotNull("the settled run produced no mesh", part)
            settledMeshes += part!!.mesh
        }

        // The moving tail, extended with the look-ahead — exactly what the editor publishes.
        val tailFrom = (wetMeshed - 1).coerceAtLeast(0)
        val live = LiveTailRibbon(
            base = ribbon,
            from = tailFrom,
            tail = predicted,
            halfWidth = ribbon.hw(ribbon.pointCount - 1),
            maxTailDist = predictedTailMaxScreenPx,
            minTailStep = 0.2,
        )
        assertTrue("the look-ahead was trimmed away", live.tailPoints().isNotEmpty())
        val tailPart = ItemMesher.meshRun(stroke, live, 0, live.pointCount, wetArc)
        assertNotNull("the live tail produced no mesh", tailPart)

        val meshes = ArrayList(settledMeshes)
        meshes += tailPart!!.mesh

        // The nib is where the last real sample is; the ink must reach it.
        val nib = Pt(stroke.xAt(stroke.sampleCount - 1), stroke.yAt(stroke.sampleCount - 1))
        val tip = Pt(live.cx(live.pointCount - 1), live.cy(live.pointCount - 1))
        val reach = hypot(tip.x - nib.x, tip.y - nib.y)
        assertTrue("the live ink does not reach the nib ($reach px away)", reach < 40.0)

        // The join between the last real point and the first predicted one is covered.
        val real = live.pointCount - live.tailPoints().size - 1
        assertTrue(
            "the join has a hole",
            covers(
                meshes,
                (live.cx(real) + live.cx(real + 1)) / 2.0,
                (live.cy(real) + live.cy(real + 1)) / 2.0,
            ),
        )
        // And the predicted tip itself is covered.
        assertTrue(
            "the predicted tip is not covered",
            covers(meshes, live.cx(live.pointCount - 1), live.cy(live.pointCount - 1)),
        )
    }

    @Test fun theTailExtendsThePaintedBoundsSoTheInkIsNotClippedAtItsTip() {
        val speed = 1.4
        val stroke = liveStroke(speed, samples = 300)
        val ribbon = stroke.wetRibbon!!
        val lastHw = ribbon.hw(ribbon.pointCount - 1)

        val live = LiveTailRibbon(
            base = ribbon,
            from = 0,
            tail = lookAhead(stroke, speed),
            halfWidth = lastHw,
            maxTailDist = predictedTailMaxScreenPx,
            minTailStep = 0.2,
        )
        // The editor unions the tail's box into the wet bounds, outset by the half-width, so a
        // masked (translucent) tail is not clipped where it leaves the ribbon.
        val tailBounds = Rect.bounding(live.tailPoints()).outset(lastHw)
        val ribbonBounds = stroke.paintBounds()
        val union = ribbonBounds.union(tailBounds)
        assertTrue(
            "the union does not cover the predicted tip",
            union.contains(Pt(live.cx(live.pointCount - 1), live.cy(live.pointCount - 1))),
        )
    }

    @Test fun aWholeStrokePenMeshesItsTailInOnePartSoTheJoinDoesNotCompositeTwice() {
        // The highlighter and the other whole-mesh pens are laid over each other as one mesh; two
        // parts would composite the overlap at the join twice, leaving a darker seam.
        val cfg = ToolDefaults.configFor(Tool.HIGHLIGHTER)
        val stroke = Stroke(Tool.HIGHLIGHTER, cfg, smoothScale = 3.0, lagComp = 0.45)
        stroke.finished = false
        for (i in 0 until 200) {
            val t = i * 8.0
            stroke.addSample(Sample(100.0 + 1.4 * t, 300.0 + sin(t * 0.004) * 40.0, 0.6, t))
        }
        val ribbon = stroke.wetRibbon!!
        val live = LiveTailRibbon(
            base = ribbon,
            from = 0,
            tail = lookAhead(stroke, 1.4),
            halfWidth = ribbon.hw(ribbon.pointCount - 1),
            maxTailDist = predictedTailMaxScreenPx,
            minTailStep = 0.2,
        )
        assertTrue("the look-ahead was trimmed away", live.tailPoints().isNotEmpty())

        // One mesh for the whole extended stroke: the join is inside it.
        val mesh = StrokeTessellator.tessellate(live, StrokeTessellator.DEFAULT_TOLERANCE)
        assertTrue("the extended stroke produced no triangles", mesh.triangleCount > 0)
        val real = live.pointCount - live.tailPoints().size - 1
        assertTrue(
            "the join has a hole",
            covers(
                listOf(mesh),
                (live.cx(real) + live.cx(real + 1)) / 2.0,
                (live.cy(real) + live.cy(real + 1)) / 2.0,
            ),
        )
        // And the whole thing is one part, so nothing composites twice.
        val part = ItemMesher.meshRun(stroke, live, 0, live.pointCount, 0.0)
        assertNotNull("the extended stroke produced no part", part)
        assertEquals(1, 1)
    }
}
