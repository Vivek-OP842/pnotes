package com.pnotes.core.stroke

import com.pnotes.core.geometry.Pt
import com.pnotes.core.model.Stroke
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The rule that keeps ink from disappearing: **what the writer watched is what lands.**
 *
 * The live ink is the wet ribbon — a causal low-pass, so its tip trails the nib — plus the
 * predictor's look-ahead tail, drawn raw so the ink reaches ahead of the hand. The committed ink
 * is the stroke's geometry, rebuilt from the samples.
 *
 * These tests pin the two halves together: the committed stroke must end exactly where the live
 * ink ended, for every tail size, and never behind it. Before the tip was pinned to its raw
 * positions, the causal low-pass dragged the committed tip back by roughly one smoothing length
 * (about 4 px at the default stability) — so the line visibly shrank at pen-up, and a tail of a
 * few pixels was swallowed whole.
 */
class TipContinuityTest {

    /** The pen at the default stability: smoothScale 3.0 (stability 0.5 at 100% zoom), lagComp 0.45. */
    private fun buildStroke(): Stroke {
        val s = Stroke(
            Tool.PEN,
            ToolDefaults.configFor(Tool.PEN),
            smoothScale = 3.0,
            lagComp = 0.45,
        )
        s.finished = false
        return s
    }

    /** A pen moving at [speedPxPerMs], sampled every [dtMs], on a long shallow arc. */
    private fun feed(stroke: Stroke, samples: Int, speedPxPerMs: Double, dtMs: Double) {
        for (i in 0 until samples) {
            val t = i * dtMs
            stroke.addSample(Sample(100.0 + speedPxPerMs * t, 300.0 + sin(t * 0.004) * 40.0, 0.6, t))
        }
    }

    /** The live tip the writer sees: the wet ribbon plus the raw look-ahead tail. */
    private fun liveTip(stroke: Stroke, tail: List<Pt>): Pt {
        val ribbon = stroke.wetRibbon!!
        val live = LiveTailRibbon(
            base = ribbon,
            from = 0,
            tail = tail,
            halfWidth = ribbon.hw(ribbon.pointCount - 1),
            maxTailDist = LiveTailRibbon.DEFAULT_MAX_TAIL_PX,
            minTailStep = LiveTailRibbon.DEFAULT_MIN_TAIL_STEP_PX,
        )
        return Pt(live.cx(live.pointCount - 1), live.cy(live.pointCount - 1))
    }

    /** The pen-up the editors perform, in the order they perform it. */
    private fun commit(stroke: Stroke): Pt {
        stroke.finished = true
        val g = stroke.geometry()
        val cl = g.centerline
        return Pt(cl[cl.size - 2].toDouble(), cl[cl.size - 1].toDouble())
    }

    @Test fun theCommittedTipIsTheLiveTipForEveryTailSize() {
        // The tail the writer watched, from a hair to a whole letter: 0.5 px to 20 px.
        for (tailPx in listOf(0.5, 1.0, 2.0, 3.0, 5.0, 8.0, 12.0, 20.0)) {
            val stroke = buildStroke()
            feed(stroke, 160, 1.4, 8.0)
            val nib = Pt(stroke.xAt(stroke.sampleCount - 1), stroke.yAt(stroke.sampleCount - 1))

            // The raw look-ahead, ahead of the nib along +x.
            val rawTail = listOf(Pt(nib.x + tailPx * 0.4, nib.y), Pt(nib.x + tailPx, nib.y))
            val ribbon = stroke.wetRibbon!!
            val anchor = Pt(ribbon.cx(ribbon.pointCount - 1), ribbon.cy(ribbon.pointCount - 1))
            val tail = PredictionAdopt.trim(rawTail, anchor.x, anchor.y)
            assertTrue("the ${tailPx}px tail was trimmed away", tail.isNotEmpty())

            val live = liveTip(stroke, tail)

            // Pen up: the UP event lands on the nib (the pen did not move between the last MOVE and
            // the UP), then the tail is committed verbatim, as the editors do.
            stroke.addSample(Sample(nib.x, nib.y, 0.6, 160 * 8.0))
            val n = stroke.sampleCount
            val samples = PredictionAdopt.adopt(tail, 0.6, stroke.tAt(n - 1))
            stroke.tipCount = samples.size
            for (s in samples) stroke.addSample(s)

            val committed = commit(stroke)
            val gap = hypot(live.x - committed.x, live.y - committed.y)
            assertTrue(
                "a ${tailPx}px tail left the committed tip $gap px behind the live tip",
                gap < 0.01,
            )
        }
    }

    @Test fun aTinyTailIsNotSwallowedByTheCommit() {
        // A 0.5 mm look-ahead: the committed stroke must reach it, not stop at the nib.
        val stroke = buildStroke()
        feed(stroke, 160, 1.4, 8.0)
        val nib = Pt(stroke.xAt(stroke.sampleCount - 1), stroke.yAt(stroke.sampleCount - 1))
        val rawTail = listOf(Pt(nib.x + 2.0, nib.y), Pt(nib.x + 5.0, nib.y))
        val ribbon = stroke.wetRibbon!!
        val anchor = Pt(ribbon.cx(ribbon.pointCount - 1), ribbon.cy(ribbon.pointCount - 1))
        val tail = PredictionAdopt.trim(rawTail, anchor.x, anchor.y)
        val live = liveTip(stroke, tail)

        stroke.addSample(Sample(nib.x, nib.y, 0.6, 160 * 8.0))
        val n = stroke.sampleCount
        val samples = PredictionAdopt.adopt(tail, 0.6, stroke.tAt(n - 1))
        stroke.tipCount = samples.size
        for (s in samples) stroke.addSample(s)

        val committed = commit(stroke)
        assertEquals("the committed tip is not the tail's end", live.x, committed.x, 0.01)
        assertTrue(
            "the committed tip fell back to the nib",
            hypot(committed.x - nib.x, committed.y - nib.y) > 1.0,
        )
    }

    @Test fun theBodyIsUnchangedByThePin() {
        // The pin must move the tail and nothing else: the stroke's body is the same ink it was.
        val pinned = buildStroke()
        feed(pinned, 160, 1.4, 8.0)
        val nib = Pt(pinned.xAt(pinned.sampleCount - 1), pinned.yAt(pinned.sampleCount - 1))
        pinned.addSample(Sample(nib.x, nib.y, 0.6, 160 * 8.0))
        val n = pinned.sampleCount
        val tail = listOf(Pt(nib.x + 3.0, nib.y), Pt(nib.x + 7.0, nib.y))
        val samples = PredictionAdopt.adopt(tail, 0.6, pinned.tAt(n - 1))
        pinned.tipCount = samples.size
        for (s in samples) pinned.addSample(s)
        pinned.finished = true
        val pinnedGeom = pinned.geometry()

        val plain = buildStroke()
        feed(plain, 160, 1.4, 8.0)
        plain.addSample(Sample(nib.x, nib.y, 0.6, 160 * 8.0))
        plain.finished = true
        val plainGeom = plain.geometry()

        val body = n - 1
        for (i in 0 until body) {
            assertEquals("the body moved at $i", plainGeom.cx(i), pinnedGeom.cx(i), 1e-4)
            assertEquals("the body moved at $i", plainGeom.cy(i), pinnedGeom.cy(i), 1e-4)
        }
    }

    @Test fun aStrokeWithoutATailIsUnaffected() {
        // The common case must not change: no tail, no pin, the same ink as before.
        val stroke = buildStroke()
        feed(stroke, 160, 1.4, 8.0)
        stroke.finished = true
        val g = stroke.geometry()
        val cl = g.centerline
        val tip = Pt(cl[cl.size - 2].toDouble(), cl[cl.size - 1].toDouble())
        val nib = Pt(stroke.xAt(stroke.sampleCount - 1), stroke.yAt(stroke.sampleCount - 1))
        // The low-pass still trails the nib; the tail is what closed that gap, and there is none.
        assertTrue(
            "the tip should still trail the nib when there is no look-ahead",
            hypot(nib.x - tip.x, nib.y - tip.y) > 0.5,
        )
    }
}
