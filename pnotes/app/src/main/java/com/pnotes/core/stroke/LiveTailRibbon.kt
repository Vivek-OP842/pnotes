package com.pnotes.core.stroke

import com.pnotes.core.geometry.Pt
import kotlin.math.hypot

/**
 * The moving end of a live stroke, extended by the predictor's look-ahead points.
 *
 * The ribbon under the pen is smoothed, and a causal filter always trails the nib by roughly one
 * filter length. The predictor answers where the pen will be a frame or two from now; drawn as a
 * continuation of the ribbon, that tail puts ink back under the nib without un-smoothing the body
 * behind it. On the infinite canvas it also means the tip the writer watches is the tip that is
 * adopted at pen-up (see `InfiniteInteraction.adoptPredictedTail`).
 *
 * This is a read-only view over an existing ribbon: points `[from, pointCount)` are delegated to
 * it unchanged, and the predicted points are appended in the same coordinate space. The tail is
 * trimmed so a predictor glitch cannot draw a line across the page:
 *  - a point closer than [minTailStep] to the previous one is a duplicate, not progress;
 *  - the tail stops at the first point farther than [maxTailDist] from the last real sample.
 *
 * The predicted points get the last real half-width, matching the paged canvas's live tail, and
 * rails of their own computed from the same central-difference tangents the ribbon uses. That is
 * all a [RibbonPoints] needs to be tessellated into one continuous run, so the join has no gap and
 * no second alpha layer.
 */
class LiveTailRibbon(
    private val base: RibbonPoints,
    from: Int,
    tail: List<Pt>,
    private val halfWidth: Double = base.hw(base.pointCount - 1),
    private val maxTailDist: Double = DEFAULT_MAX_TAIL_PX,
    private val minTailStep: Double = DEFAULT_MIN_TAIL_STEP_PX,
) : RibbonPoints {

    private val start = from.coerceIn(0, base.pointCount)
    private val realCount = base.pointCount - start

    /** Predicted points that survived the trim, in order after the last real one. */
    private val predicted: List<Pt>

    init {
        require(realCount >= 1) { "a live tail needs at least one real point" }
        val anchorX = base.cx(base.pointCount - 1)
        val anchorY = base.cy(base.pointCount - 1)
        val kept = ArrayList<Pt>(tail.size)
        var prevX = anchorX
        var prevY = anchorY
        for (p in tail) {
            if (!p.x.isFinite() || !p.y.isFinite()) continue
            if (hypot(p.x - prevX, p.y - prevY) < minTailStep) continue
            if (hypot(p.x - anchorX, p.y - anchorY) > maxTailDist) break
            kept += p
            prevX = p.x
            prevY = p.y
        }
        predicted = kept
    }

    override val pointCount: Int = realCount + predicted.size

    /** A single real sample is still a dot; any tail point gives the ribbon a body to sweep. */
    override val hasRails: Boolean get() = pointCount >= 2

    // Packed once per frame rather than computed per getter: the tessellator reads each rail
    // several times (body quads, end caps, join discs).
    private val centersX = DoubleArray(pointCount)
    private val centersY = DoubleArray(pointCount)
    private val widths = DoubleArray(pointCount)
    private val leftXArr = DoubleArray(pointCount)
    private val leftYArr = DoubleArray(pointCount)
    private val rightXArr = DoubleArray(pointCount)
    private val rightYArr = DoubleArray(pointCount)

    init {
        for (i in 0 until pointCount) {
            if (i < realCount) {
                val k = start + i
                centersX[i] = base.cx(k)
                centersY[i] = base.cy(k)
                widths[i] = base.hw(k)
            } else {
                val p = predicted[i - realCount]
                centersX[i] = p.x
                centersY[i] = p.y
                widths[i] = halfWidth
            }
        }
        val n = pointCount
        for (i in 0 until n) {
            val dx: Double
            val dy: Double
            when {
                n < 2 -> {
                    dx = 1.0
                    dy = 0.0
                }
                i == 0 -> {
                    dx = centersX[1] - centersX[0]
                    dy = centersY[1] - centersY[0]
                }
                i == n - 1 -> {
                    dx = centersX[n - 1] - centersX[n - 2]
                    dy = centersY[n - 1] - centersY[n - 2]
                }
                else -> {
                    dx = (centersX[i + 1] - centersX[i - 1]) * 0.5
                    dy = (centersY[i + 1] - centersY[i - 1]) * 0.5
                }
            }
            val len = hypot(dx, dy)
            val nx = if (len > 1e-6) -dy / len else 0.0
            val ny = if (len > 1e-6) dx / len else 1.0
            val r = widths[i]
            leftXArr[i] = centersX[i] + nx * r
            leftYArr[i] = centersY[i] + ny * r
            rightXArr[i] = centersX[i] - nx * r
            rightYArr[i] = centersY[i] - ny * r
        }
    }

    /** The accepted predicted points, newest last; empty when nothing is worth drawing. */
    fun tailPoints(): List<Pt> = predicted

    override fun cx(i: Int): Double = centersX[i]
    override fun cy(i: Int): Double = centersY[i]
    override fun hw(i: Int): Double = widths[i]
    override fun leftX(i: Int): Double = leftXArr[i]
    override fun leftY(i: Int): Double = leftYArr[i]
    override fun rightX(i: Int): Double = rightXArr[i]
    override fun rightY(i: Int): Double = rightYArr[i]

    companion object {
        /** Longest extension (content px) the live tail may draw past the last real sample. */
        const val DEFAULT_MAX_TAIL_PX = 64.0

        /** Closer than this and a predicted point is the previous one, not progress. */
        const val DEFAULT_MIN_TAIL_STEP_PX = 0.2
    }
}
