package com.pnotes.core.stroke

import com.pnotes.core.geometry.Pt
import com.pnotes.core.model.Stroke
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolDefaults
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Where the live ink sits relative to the nib, on the infinite canvas.
 *
 * The wet ribbon is a causal low-pass, so its last point is always a little behind the pen: the
 * filter needs the next sample to know where the curve is going. That residual is the rubber band
 * the writer sees between the ink and the nib.
 *
 * The infinite canvas answers it with the predictor's look-ahead, laid into the same ribbon as a
 * continuation of the live tail ([LiveTailRibbon]). The tail points *ahead* of the nib on purpose:
 * by the time the frame reaches the screen the pen has moved on, so ink drawn at the nib is already
 * stale, and ink drawn at the predicted position is where the pen actually is.
 *
 * These tests pin that behaviour down as numbers rather than by eye: the ink must not trail the
 * nib, must not be thrown far ahead of it, and the join must be continuous.
 */
class LiveTipLagTest {

    /**
     * A pen moving at [speedPxPerMs] content px per millisecond, sampled every [dtMs].
     * The path is a long shallow arc, so the filter has real curvature to smooth but the heading
     * never changes fast enough for the predictor's direction gate to drop the tail.
     */
    private fun feedStroke(stroke: Stroke, samples: Int, speedPxPerMs: Double, dtMs: Double) {
        for (i in 0 until samples) {
            val t = i * dtMs
            stroke.addSample(Sample(100.0 + speedPxPerMs * t, 300.0 + sin(t * 0.004) * 40.0, 0.6, t))
        }
    }

    /** The pen's true position after the last sample of [stroke]. */
    private fun nibAt(stroke: Stroke): Pt {
        val i = stroke.sampleCount - 1
        return Pt(stroke.xAt(i), stroke.yAt(i))
    }

    /**
     * The predictor's look-ahead for a pen moving at a constant [speedPxPerMs]: the AndroidX
     * predictor's 12 ms window, taken at its middle and at its far edge. This is the shape the real
     * predictor returns — points ahead of the last sample along the direction of travel.
     */
    private fun lookAhead(stroke: Stroke, speedPxPerMs: Double): List<Pt> {
        val n = stroke.sampleCount
        val last = Pt(stroke.xAt(n - 1), stroke.yAt(n - 1))
        val prev = Pt(stroke.xAt(n - 2), stroke.yAt(n - 2))
        val dx = last.x - prev.x
        val dy = last.y - prev.y
        val len = hypot(dx, dy)
        if (len < 1e-6) return emptyList()
        val travel = speedPxPerMs * LOOKAHEAD_MS
        return listOf(
            Pt(last.x + dx / len * travel, last.y + dy / len * travel),
            Pt(last.x + dx / len * travel * 1.6, last.y + dy / len * travel * 1.6),
        )
    }

    @Test fun theRibbonTipTrailsTheNibSoThereIsSomethingToClose() {
        // The premise of the whole fix: without a tail the live ink sits behind the pen. If this
        // ever stops being true the tail has nothing left to do.
        val speed = 1.4
        val stroke = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), smoothScale = 3.0, lagComp = 0.45)
        stroke.finished = false
        feedStroke(stroke, samples = 160, speedPxPerMs = speed, dtMs = 8.0)
        val ribbon = stroke.wetRibbon!!
        val nib = nibAt(stroke)
        val tip = Pt(ribbon.cx(ribbon.pointCount - 1), ribbon.cy(ribbon.pointCount - 1))
        val trail = hypot(nib.x - tip.x, nib.y - tip.y)
        assertTrue("the ribbon tip does not trail the nib at all ($trail px)", trail > 0.5)
        println("plain ribbon tip trails the nib by ${"%.2f".format(trail)}px at ${speed}px/ms")
    }

    @Test fun thePredictedTailBringsTheLiveInkUpToTheNib() {
        // The default pen at the default stability (0.5 -> 3x streamline, 0.45 lag compensation),
        // written at a normal 1.4 px/ms.
        val speed = 1.4
        val stroke = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), smoothScale = 3.0, lagComp = 0.45)
        stroke.finished = false
        feedStroke(stroke, samples = 160, speedPxPerMs = speed, dtMs = 8.0)
        val ribbon = stroke.wetRibbon!!

        val nib = nibAt(stroke)
        val plainTip = Pt(ribbon.cx(ribbon.pointCount - 1), ribbon.cy(ribbon.pointCount - 1))
        val plainTrail = hypot(nib.x - plainTip.x, nib.y - plainTip.y)

        val live = LiveTailRibbon(
            base = ribbon,
            from = 0,
            tail = lookAhead(stroke, speed),
            halfWidth = ribbon.hw(ribbon.pointCount - 1),
        )
        assertTrue("the look-ahead was trimmed away", live.tailPoints().isNotEmpty())
        val tailTip = Pt(live.cx(live.pointCount - 1), live.cy(live.pointCount - 1))

        // The ink must reach the nib: the tail's tip is at or past the pen, so what the writer sees
        // at the tip is the pen's own position, not a point behind it.
        val ahead = (tailTip.x - nib.x) * (nib.x - plainTip.x) + (tailTip.y - nib.y) * (nib.y - plainTip.y)
        assertTrue("the ink still trails the nib with the tail drawn", ahead >= 0.0)

        // And it must not be thrown far ahead of it: the tail is a look-ahead, not a leap.
        val overshoot = hypot(tailTip.x - nib.x, tailTip.y - nib.y)
        val travel = speed * LOOKAHEAD_MS * 1.6
        assertTrue(
            "the tail overshot the nib by $overshoot px (look-ahead is $travel px)",
            overshoot <= travel + 1.0,
        )
        println(
            "live ink: plain trail ${"%.2f".format(plainTrail)}px, " +
                "with tail it reaches ${"%.2f".format(overshoot)}px past the nib",
        )
    }

    @Test fun theTailKeepsUpAtFastWritingSpeed() {
        // Fast writing is where the residual is worst: the filter's length is fixed in arc length,
        // so the faster the nib moves the further the ink trails it. 3 px/ms is a brisk signature.
        val speed = 3.0
        val stroke = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), smoothScale = 3.0, lagComp = 0.45)
        stroke.finished = false
        feedStroke(stroke, samples = 200, speedPxPerMs = speed, dtMs = 8.0)
        val ribbon = stroke.wetRibbon!!

        val nib = nibAt(stroke)
        val plainTip = Pt(ribbon.cx(ribbon.pointCount - 1), ribbon.cy(ribbon.pointCount - 1))
        val plainTrail = hypot(nib.x - plainTip.x, nib.y - plainTip.y)

        val live = LiveTailRibbon(
            base = ribbon,
            from = 0,
            tail = lookAhead(stroke, speed),
            halfWidth = ribbon.hw(ribbon.pointCount - 1),
        )
        assertTrue("the look-ahead was trimmed away", live.tailPoints().isNotEmpty())
        val tailTip = Pt(live.cx(live.pointCount - 1), live.cy(live.pointCount - 1))
        val ahead = (tailTip.x - nib.x) * (nib.x - plainTip.x) + (tailTip.y - nib.y) * (nib.y - plainTip.y)
        assertTrue("the ink still trails the nib at ${speed}px/ms", ahead >= 0.0)
        println("fast live ink: plain trail ${"%.2f".format(plainTrail)}px, tail reaches the nib")
    }

    @Test fun aLookAheadLongerThanTheCapIsTrimmedRatherThanDrawn() {
        // The cap exists so a fast stroke's own look-ahead cannot be thrown off the nib by more
        // than the writer can account for. 6 px/ms over the 12 ms window is 72 px, past the 64 px
        // cap, so the tail is cut short — the ink still reaches the nib, just not the far edge.
        val speed = 6.0
        val stroke = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), smoothScale = 3.0, lagComp = 0.45)
        stroke.finished = false
        feedStroke(stroke, samples = 200, speedPxPerMs = speed, dtMs = 8.0)
        val ribbon = stroke.wetRibbon!!
        val live = LiveTailRibbon(
            base = ribbon,
            from = 0,
            tail = lookAhead(stroke, speed),
            halfWidth = ribbon.hw(ribbon.pointCount - 1),
            maxTailDist = LiveTailRibbon.DEFAULT_MAX_TAIL_PX,
        )
        val tip = Pt(ribbon.cx(ribbon.pointCount - 1), ribbon.cy(ribbon.pointCount - 1))
        for (p in live.tailPoints()) {
            assertTrue("a tail point is past the cap", hypot(p.x - tip.x, p.y - tip.y) <= 64.0)
        }
        println("at ${speed}px/ms the tail kept ${live.tailPoints().size} of 2 points")
    }

    @Test fun theTailIsCappedSoAPredictorGlitchCannotThrowInkAcrossThePage() {
        val speed = 1.4
        val stroke = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), smoothScale = 3.0, lagComp = 0.45)
        stroke.finished = false
        feedStroke(stroke, samples = 120, speedPxPerMs = speed, dtMs = 8.0)
        val ribbon = stroke.wetRibbon!!
        val tip = Pt(ribbon.cx(ribbon.pointCount - 1), ribbon.cy(ribbon.pointCount - 1))

        // A predictor that suddenly reports a point 400px away: the tail must stop before it.
        val live = LiveTailRibbon(
            base = ribbon,
            from = 0,
            tail = listOf(Pt(tip.x + 400.0, tip.y), Pt(tip.x + 410.0, tip.y)),
            maxTailDist = LiveTailRibbon.DEFAULT_MAX_TAIL_PX,
        )
        assertTrue("the glitch was drawn", live.tailPoints().isEmpty())
    }

    @Test fun theTailIsDrawnInTheSameMeshSoTheJoinHasNoSecondAlphaLayer() {
        val speed = 1.4
        val stroke = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), smoothScale = 3.0, lagComp = 0.45)
        stroke.finished = false
        feedStroke(stroke, samples = 120, speedPxPerMs = speed, dtMs = 8.0)
        val ribbon = stroke.wetRibbon!!
        val live = LiveTailRibbon(
            base = ribbon,
            from = 0,
            tail = lookAhead(stroke, speed),
            halfWidth = ribbon.hw(ribbon.pointCount - 1),
        )
        // One mesh for the whole extended run: the join is inside it, so nothing composites twice.
        val mesh = com.pnotes.core.infinite.StrokeTessellator.tessellate(
            live,
            com.pnotes.core.infinite.StrokeTessellator.DEFAULT_TOLERANCE,
        )
        assertTrue("the extended run produced no triangles", mesh.triangleCount > 0)
        val last = live.pointCount - 1
        assertTrue("the mesh does not reach the predicted tip", covers(mesh, live.cx(last), live.cy(last)))
        // And the join between the last real point and the first predicted one is covered too.
        val real = live.pointCount - live.tailPoints().size - 1
        val joinX = (live.cx(real) + live.cx(real + 1)) / 2.0
        val joinY = (live.cy(real) + live.cy(real + 1)) / 2.0
        assertTrue("the join has a hole", covers(mesh, joinX, joinY))
    }

    /** Even-odd triangle containment, enough for the thin ribbon meshes here. */
    private fun covers(mesh: com.pnotes.core.infinite.MeshData, px: Double, py: Double): Boolean {
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

    private companion object {
        /** The AndroidX predictor's window, in ms. */
        const val LOOKAHEAD_MS = 12.0
    }
}
