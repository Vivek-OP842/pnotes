package com.pnotes.core.stroke

import com.pnotes.core.geometry.Pt
import kotlin.math.hypot

/**
 * The predictor's look-ahead tail: the ink the writer sees ahead of the nib, and the ink that
 * lands at pen-up.
 *
 * **The rule this file exists to enforce: the tail the writer watched is the tail that is
 * committed, point for point.**
 *
 * The live renderer draws the tail raw. It is an estimate of where the nib will be a frame from
 * now, and running it through the stroke's causal low-pass would drag the ink back behind the
 * pen — the one thing a look-ahead must never do. So the tail must not go through that low-pass
 * on its way into the model either: a causal low-pass trails its input by about one smoothing
 * length (roughly 4 px at the default stability), so a committed tail comes out that much
 * shorter than the tail that was on screen and the line visibly retracts under the hand at
 * pen-up. The committed geometry therefore pins the tail samples to their raw positions (see
 * [StrokeEngine.build]'s `tipFrom`), and this file's job is only to guarantee that the tail
 * being pinned is the very tail that was drawn.
 *
 * That is why the trimming lives here and runs once, in the controller, before the tail is
 * handed to anybody: the renderer and the pen-up adoption then read the same list, and there is
 * no second set of rules that can quietly shorten it.
 */
object PredictionAdopt {

    /**
     * Closest a tail point may sit to the previous one (content px at 100% zoom). A point nearer
     * than this is the previous one again, not progress. The caller scales it by the zoom, so the
     * writer sees the same grain on the glass at every zoom.
     */
    const val MIN_TAIL_STEP_PX = 0.2

    /**
     * Furthest a tail point may sit from the anchor it grows out of (content px at 100% zoom).
     * A predictor glitch must not be able to draw a line across the page.
     */
    const val MAX_TAIL_PX = 64.0

    /** Elapsed-ms span the predicted points are given when they are timestamps (10 - 15 ms target window). */
    const val PREDICTED_SPAN_MS = 12.5

    /** Dynamic horizon bounds based on display refresh rate: clamped to 10 - 15 ms target window. */
    const val MIN_PREDICT_HORIZON_MS = 10.0
    const val MAX_PREDICT_HORIZON_MS = 15.0

    /**
     * The tail as it will be drawn and as it will be committed: no duplicate points, and nothing
     * past [maxDist] from the anchor.
     *
     * [anchorX]/[anchorY] is the point the tail grows out of — the live ribbon's tip, which is
     * what the tail is drawn from — so the trim is measured from the same place the renderer
     * measures it, and running it twice changes nothing.
     */
    fun trim(
        tail: List<Pt>,
        anchorX: Double,
        anchorY: Double,
        minStep: Double = MIN_TAIL_STEP_PX,
        maxDist: Double = MAX_TAIL_PX,
    ): List<Pt> {
        if (tail.isEmpty()) return emptyList()
        val kept = ArrayList<Pt>(tail.size)
        var prevX = anchorX
        var prevY = anchorY
        for (p in tail) {
            if (!p.x.isFinite() || !p.y.isFinite()) continue
            if (hypot(p.x - prevX, p.y - prevY) < minStep) continue
            if (hypot(p.x - anchorX, p.y - anchorY) > maxDist) break
            kept += p
            prevX = p.x
            prevY = p.y
        }
        return kept
    }

    /**
     * The samples that carry [tail] into the stroke.
     *
     * The tail is taken whole. Every point the writer saw is a point the stroke gets: nothing is
     * dropped for sitting behind the pen-up sample, for doubling back, or for being small. The
     * ink was on screen, and the rule is that ink never disappears — so the commit reproduces it
     * rather than second-guessing it.
     *
     * The times run forward from [lastT], because the tail stands for a stretch of the stroke
     * that has not been drawn yet.
     */
    fun adopt(tail: List<Pt>, pressure: Double, lastT: Double): List<Sample> {
        if (tail.isEmpty()) return emptyList()
        val p = pressure.coerceIn(0.0, 1.0)
        val stepMs = PREDICTED_SPAN_MS / tail.size
        return tail.mapIndexed { i, pt -> Sample(pt.x, pt.y, p, lastT + stepMs * (i + 1)) }
    }
}
