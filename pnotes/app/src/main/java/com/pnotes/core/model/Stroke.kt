package com.pnotes.core.model

import com.pnotes.core.geometry.Affine
import com.pnotes.core.geometry.Geometry
import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.pal.BlendMode
import com.pnotes.core.pal.FillRule
import com.pnotes.core.pal.Pen
import com.pnotes.core.pal.Renderer
import com.pnotes.core.stroke.CurvatureSpline
import com.pnotes.core.stroke.NibAxis
import com.pnotes.core.stroke.Sample
import com.pnotes.core.stroke.StrokeEngine
import com.pnotes.core.stroke.StrokeGeometry
import com.pnotes.core.stroke.WetRibbon
import com.pnotes.core.tools.StrokeTexture
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolConfig

/**
 * The unit of inking (spec 02 §5.1). Holds the raw samples plus a snapshot of
 * the tool style; the ribbon geometry is derived from the samples on demand and
 * cached until the samples change.
 *
 * Samples live in parallel primitive arrays, not a `List<Sample>`: a dense note holds millions of
 * them, and one boxed 4-double object per sample costs ~60 bytes against the 12 the numbers need.
 * Positions are **offsets from [ox]/[oy]**, a per-stroke double origin taken at pen-down, so a
 * float never has to carry a large absolute coordinate — the infinite canvas has no coordinate
 * bound, and float32 spacing reaches a whole pixel around 1e7. An offset spans one stroke, a few
 * hundred px at most, where float resolves to ~1e-5 px. [samples] is a read-only view over the
 * arrays for the cold call sites; the hot loops read [xAt]/[yAt]/[pAt] and allocate nothing.
 */
class Stroke(
    val tool: Tool,
    /** Scaled in place when the stroke is resized (var so [applyTransform] can swap a width-scaled
     *  copy); otherwise the immutable style snapshot captured at pen-down. */
    var config: ToolConfig,
    samples: List<Sample> = emptyList(),
    /** Content-px → dp scale captured at pen-down (zoom ÷ display density), so the speed
     *  pen judges gesture speed in zoom- and device-independent units. 1.0 = unscaled. */
    val speedScale: Double = 1.0,
    /** Straight-line mode: render as a raw segment (no position smoothing) so the line reaches
     *  exactly from the first sample to the last — the EMA low-pass otherwise pulls a 2-point
     *  stroke's far end to the midpoint, lagging it behind the stylus. */
    val straight: Boolean = false,
    /** Scale on every arc-length constant the engine measures the hand against: the low-pass
     *  lengths, the nib's head window and its widen/thin rates. Captured at pen-down
     *  as 1 ÷ zoom and capped at 1, so they are lengths of *gesture* rather than of page, and
     *  writing small at high zoom behaves as writing at 100% does. 1.0 = drawn at 100% zoom, and
     *  the default for anything built without a pen. */
    val smoothScale: Double = 1.0,
    /** Star Notes lag compensation for the position low-pass (StrokeEngine.emaStep). Captured at
     *  pen-down from the pen's stability setting, so re-tuning a tool never restyles existing
     *  strokes. 0.0 (the default, and every stroke persisted before the field existed) = the
     *  legacy filter exactly; ~0.45 = Star Notes' tip-anchored feel. */
    val lagComp: Double = 0.0,
) : CanvasItem {

    override val kind = KIND
    override val resizable = false
    override var locked = false

    /** Double origin the float offsets hang off; the first sample's position. */
    private var ox = 0.0
    private var oy = 0.0
    private var xa = EMPTY_F
    private var ya = EMPTY_F
    private var pa = EMPTY_F
    /** Milliseconds since the first sample, allocated only once a sample carries a non-zero time
     *  (only the speed pen records any), so ordinary ink pays nothing for the channel. */
    private var ta: FloatArray? = null
    /** Radians of stylus tilt (`AXIS_TILT`) per sample, allocated only once a sample carries one, so
     *  ink drawn with a finger (or a stylus held flat) pays nothing for the channel. Stored and
     *  persisted so a gesture's tilt is not thrown away at capture: it is the input the calligraphy
     *  nib needs to know which way its edge is turned, which the tangent can only guess at. */
    private var tta: FloatArray? = null
    /** Radians of stylus azimuth (`AXIS_ORIENTATION`) per sample, allocated on the same terms as
     *  [tta]. With the tilt this is what turns the calligraphy nib: together they name the direction
     *  the writer is holding the pen in, which the stroke's own tangent cannot know ([NibAxis]). */
    private var aza: FloatArray? = null
    private var n = 0

    /** The ribbon cache. Volatile because cache builds read it off the UI thread while
     *  [releaseGeometry] may be nulling it on the UI thread; [StrokeGeometry] is immutable, so a
     *  race costs at worst one redundant rebuild. */
    @Volatile
    private var cachedGeometry: StrokeGeometry? = null
    private var cachedRawBounds: Rect? = null
    private var cachedBounds: Rect? = null

    /**
     * The ribbon while the pen is still down, grown a sample at a time rather than rebuilt (see
     * [WetRibbon]). Present only for a live stroke whose pen settles as it is drawn; the straight-line
     * tools have none, and neither does a finished stroke, which goes back through
     * [StrokeEngine.build] so the lift-time rules can fire.
     */
    @Volatile
    private var wet: WetRibbon? = null

    /** The live ribbon, for renderers that want to draw only the part of it still moving. */
    val wetRibbon: WetRibbon? get() = wet

    /** The adaptive-spline pump's in-flight samples, when the pen's spline is on. The wet ribbon is
     *  fed spline points instead of raw samples (the raw polyline the digitizer reports is what
     *  scallops the ink), and the commit finishes the tail through [finishWet]. When the pump is
     *  off this stays empty and the ribbon is fed exactly as before. */
    private var splineBuf = ArrayList<CurvatureSpline.SplinePoint>(16)

    /** Spline points the pump still owes the ribbon (the final segment waits for the pen-up).
     *  Reserved for a future lookahead tail; the exact pump emits each segment the moment it
     *  becomes final, so this stays empty and the ribbon never carries unbaked points. */
    private var splinePending: List<CurvatureSpline.SplinePoint> = emptyList()

    /** True when the pen's spline smoothing is active for this stroke (wet pump only; the commit
     *  decides independently so it can pump a shorter buffer). */
    private val splineActive get() = config.adaptiveSpline && !straight && smoothScale > 0.0

    init {
        if (samples.isNotEmpty()) setSamples(samples)
    }

    /** Copy constructor: duplicates the sample arrays without materializing a single [Sample]. */
    constructor(src: Stroke) : this(src.tool, src.config, emptyList(), src.speedScale, src.straight, src.smoothScale, src.lagComp) {
        ox = src.ox
        oy = src.oy
        n = src.n
        xa = src.xa.copyOf(n)
        ya = src.ya.copyOf(n)
        pa = src.pa.copyOf(n)
        ta = src.ta?.copyOf(n)
        tta = src.tta?.copyOf(n)
        aza = src.aza?.copyOf(n)
        cutStart = src.cutStart
        cutEnd = src.cutEnd
        tipCount = src.tipCount
    }

    /** Whether the start of this stroke is a cut/erased edge (flat butt cap instead of round). */
    var cutStart: Boolean = false

    /** Whether the end of this stroke is a cut/erased edge (flat butt cap instead of round). */
    var cutEnd: Boolean = false

    /**
     * How many of this stroke's trailing samples are the predictor's look-ahead tail; 0 when the
     * stroke carries none.
     *
     * Those samples were drawn raw, ahead of the nib, and they are the last ink the writer saw
     * before the pen came up. They stay ordinary samples — so the eraser, the selection, the undo
     * stack and the file format all treat them as ink — but the committed geometry pins them to
     * their raw positions instead of running them through the causal low-pass (see
     * [StrokeEngine.build]). Left to the filter they would come out about one smoothing length
     * short, and the line would visibly retract under the hand at pen-up.
     *
     * A count, not an index: the tail is always the tail — the last samples of the stroke — so a
     * count survives the edits that shift indices (the pen-up reduction, an eraser split).
     */
    var tipCount: Int = 0

    /** Index of the first sample that is the look-ahead tail, or -1 when there is none. */
    val tipFrom: Int get() = if (tipCount > 0) (n - tipCount).coerceAtLeast(0) else -1

    // --- sample access ---

    /** Number of raw samples. */
    val sampleCount get() = n

    fun xAt(i: Int): Double = ox + xa[i]
    fun yAt(i: Int): Double = oy + ya[i]
    fun pAt(i: Int): Double = pa[i].toDouble()
    fun tAt(i: Int): Double = ta?.get(i)?.toDouble() ?: 0.0

    /** Stylus tilt (radians) at sample [i]; 0 when the input reported none. */
    fun tiltAt(i: Int): Double = tta?.get(i)?.toDouble() ?: 0.0

    /** Stylus azimuth (radians) at sample [i]; 0 when the input reported none. */
    fun azimuthAt(i: Int): Double = aza?.get(i)?.toDouble() ?: 0.0

    /** Whether this stroke carries a tilt channel at all: false for anything drawn without a stylus,
     *  or with one held flat, which is most ink. */
    val hasTilt get() = tta != null

    /** Whether this stroke carries an azimuth channel at all; see [hasTilt]. */
    val hasAzimuth get() = aza != null

    /**
     * The nib the writer was holding, read once from the stroke's first sample, or null when the
     * stylus reported no usable orientation there (a finger, a stylus that reports none, or one held
     * too upright to read). [StrokeEngine] and [WetRibbon] are handed this and measure the direction
     * term on it instead of on the page's vertical.
     *
     * The first sample rather than an average over the stroke, because the axis has to be known at
     * pen-down: it is the one value a growing ribbon can read once and keep, and a stylus that reports
     * nothing at pen-down reports nothing worth turning a nib by either.
     *
     * The nib's grind ([ToolConfig.nibCut]) rides the config, not this object: it is a property of the
     * tool the writer picked, so two presets on one stylus draw different nibs, and a stroke keeps the
     * nib it was drawn with even if the tool is retuned afterwards.
     */
    private fun nibAxis(): NibAxis.Axis? = NibAxis.of(tiltAt(0), azimuthAt(0), config.nibCut)

    fun sampleAt(i: Int): Sample = Sample(xAt(i), yAt(i), pAt(i), tAt(i), tiltAt(i), azimuthAt(i))

    /** Read-only `List<Sample>` view for the call sites that still speak in [Sample]s. Each `get`
     *  builds a short-lived value; the hot loops use the primitive accessors instead. */
    val samples: List<Sample> get() = SampleView(this)

    private class SampleView(private val s: Stroke) : AbstractList<Sample>() {
        override val size get() = s.n
        override fun get(index: Int): Sample = s.sampleAt(index)
    }

    /** Replace every sample (pen-up reduction, undo restore, legacy compaction). Allocates exactly
     *  [list].size: a finished stroke never grows again, and rounding up to the growth step would
     *  waste most of a byte budget where the median stroke is ~30 samples. */
    fun setSamples(list: List<Sample>) {
        val m = list.size
        n = m
        // The tail is the stroke's last samples, so a shorter list simply has fewer of them.
        if (tipCount > m) tipCount = m
        if (m == 0) {
            xa = EMPTY_F
            ya = EMPTY_F
            pa = EMPTY_F
            ta = null
            tta = null
            aza = null
            invalidate()
            return
        }
        val first = list[0]
        ox = first.x
        oy = first.y
        var timed = false
        var tilted = false
        var oriented = false
        for (i in 0 until m) {
            if (list[i].t != 0.0) timed = true
            if (list[i].tilt != 0.0) tilted = true
            if (list[i].azimuth != 0.0) oriented = true
        }
        xa = FloatArray(m)
        ya = FloatArray(m)
        pa = FloatArray(m)
        ta = if (timed) FloatArray(m) else null
        tta = if (tilted) FloatArray(m) else null
        aza = if (oriented) FloatArray(m) else null
        for (i in 0 until m) {
            val s = list[i]
            xa[i] = (s.x - ox).toFloat()
            ya[i] = (s.y - oy).toFloat()
            pa[i] = s.pressure.toFloat()
            ta?.set(i, s.t.toFloat())
            tta?.set(i, s.tilt.toFloat())
            aza?.set(i, s.azimuth.toFloat())
        }
        invalidate()
    }

    /**
     * Hand back the slack [addSample]'s doubling left over. A live stroke grows by doubling, so at
     * pen-up it holds up to twice the arrays it needs, and it never grows again. Called once the
     * stroke is committed.
     */
    fun trimToSize() {
        if (xa.size == n) return
        xa = xa.copyOf(n)
        ya = ya.copyOf(n)
        pa = pa.copyOf(n)
        ta = ta?.copyOf(n)
        tta = tta?.copyOf(n)
        aza = aza?.copyOf(n)
    }

    private fun ensure(capacity: Int) {
        if (xa.size >= capacity) return
        var c = if (xa.isEmpty()) INITIAL_CAPACITY else xa.size
        while (c < capacity) c *= 2
        xa = xa.copyOf(c)
        ya = ya.copyOf(c)
        pa = pa.copyOf(c)
        ta = ta?.copyOf(c)
        tta = tta?.copyOf(c)
        aza = aza?.copyOf(c)
    }

    /** False only while the pen is still down on this stroke: lift-time rules (the calligraphy
     *  dot swell, a stroke that never fills [StrokeEngine.HEAD_LEN]) are held off so the live
     *  preview can't open thick at every pen-down. Loaded, cloned and eraser-split strokes are
     *  complete. */
    var finished = true
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /** Ink colour with the tool's alpha scale applied (the highlighter uses its configurable
     *  [ToolConfig.highlighterAlpha]; every other tool is opaque, scale 1.0). */
    val renderColor get() = config.rgba.scaleAlpha(
        if (tool == Tool.HIGHLIGHTER) config.highlighterAlpha else tool.alphaScale,
    )

    /** How this stroke's translucent ink composites: standard alpha blending like Star Notes marker. */
    val blendMode: BlendMode get() = BlendMode.SRC_OVER

    val isEmpty get() = n == 0

    /** Calligraphy pen holds its start/end pressure ([StrokeEngine.CAP_HOLD_SAMPLES]) out to the broad width.
     *  Ballpoint/Fountain pen follows physical stylus pressure naturally from initial touch. */
    internal val holdsEnds: Boolean get() = tool == Tool.PEN || tool == Tool.HIGHLIGHTER

    /** Lazily-built ribbon geometry; rebuilt only when samples change. */
    fun geometry(): StrokeGeometry {
        wet?.let { return it.geometry() }
        cachedGeometry?.let { return it }
        // Unpack to the doubles the engine works in. It allocates these three arrays anyway, so
        // reading the float storage here costs nothing over passing a list of boxed samples.
        val rx = DoubleArray(n)
        val ry = DoubleArray(n)
        val rp = DoubleArray(n)
        for (i in 0 until n) {
            rx[i] = ox + xa[i]
            ry[i] = oy + ya[i]
            rp[i] = pa[i].toDouble()
        }
        val rt = ta?.let { src -> DoubleArray(n) { src[it].toDouble() } }
        return StrokeEngine.build(
            rx, ry, rp, rt,
            config.baseWidth,
            config.pressureEnabled,
            config.pressureMinFactor,
            config.directionStrength,
            config.speedStrength,
            config.taperEnabled,
            config.taperMinFactor,
            speedScale,
            smooth = !straight,
            holdEnds = holdsEnds,
            finished = finished,
            smoothScale = smoothScale,
            nib = nibAxis(),
            velLo = config.velocityLowerBound,
            velHi = config.velocityUpperBound,
            velAmp = config.velocityAmplifier,
            endVelocitySensitivity = config.endVelocitySensitivity,
            curvatureDecay = config.curvatureDecay,
            brushRatio = config.brushRatio,
            brushAngle = config.brushAngle,
            lagComp = lagComp,
            adaptiveSpline = config.adaptiveSpline,
            pressureGamma = config.pressureGamma,
            tipFrom = tipFrom,
        ).also { cachedGeometry = it }
    }

    /** Pre-compiled hardware Path object for zero-allocation 120 FPS GPU vector rendering. */
    var cachedNativePath: Any? = null

    fun invalidate() {
        wet = null
        cachedGeometry = null
        cachedRawBounds = null
        cachedBounds = null
        cachedNativePath = null
        // The spline pump's window belongs to the ribbon it fed; a wholesale sample edit rebuilds
        // both together (wetOrStart re-runs the pump over the new samples).
        splineBuf.clear()
        splinePending = emptyList()
    }

    /**
     * Drop the ribbon but keep the bounds. Used by the canvas to reclaim geometry for pages that
     * scrolled out of the cached band. The two [Rect]s stay because [bounds] is what selection
     * reads, and rebuilding the whole ribbon to hand back a rectangle would cost more than the
     * ribbon saved. Not [invalidate]: nothing about the samples changed.
     */
    fun releaseGeometry() {
        // A live ribbon holds the bounds too, so bank them before letting it go.
        wet?.let {
            cachedBounds = it.bounds()
            wet = null
        }
        cachedGeometry = null
        cachedNativePath = null
    }

    /** True once [geometry] has been built and not since released; lets the canvas skip pages
     *  that never rendered instead of walking their items. */
    val hasGeometry get() = wet != null || cachedGeometry != null

    fun addSample(s: Sample) {
        if (n == 0) {
            ox = s.x
            oy = s.y
        }
        ensure(n + 1)
        if (s.t != 0.0 && ta == null) ta = FloatArray(xa.size)
        if (s.tilt != 0.0 && tta == null) tta = FloatArray(xa.size)
        if (s.azimuth != 0.0 && aza == null) aza = FloatArray(xa.size)
        xa[n] = (s.x - ox).toFloat()
        ya[n] = (s.y - oy).toFloat()
        pa[n] = s.pressure.toFloat()
        ta?.set(n, s.t.toFloat())
        tta?.set(n, s.tilt.toFloat())
        aza?.set(n, s.azimuth.toFloat())
        n++
        val ribbon = wetOrStart()
        if (ribbon == null) {
            invalidate()
            return
        }
        // Fed from the packed arrays, not from [s]: the samples are stored as floats, and the
        // ribbon has to see the same rounded numbers a rebuild would, or the ink would shift a
        // hair as the stroke crosses from one to the other.
        val i = n - 1
        if (splineActive) {
            pumpSpline(ribbon, i)
        } else {
            ribbon.append(xAt(i), yAt(i), pAt(i), tAt(i))
        }
        cachedGeometry = null
        cachedRawBounds = null
        cachedBounds = null
    }

    /**
     * The exact causal spline pump: feeds sample [i] into the sliding window and, once its segment
     * became final (lookahead landed), appends that segment's subdivision to [ribbon]. See the
     * steady-state window comment in [addSample] for why this is segment-for-segment identical to
     * the committed whole-stroke pass. Split out so [wetOrStart]'s backfill runs the same code the
     * live path runs.
     */
    private fun pumpSpline(ribbon: WetRibbon, i: Int) {
        splineBuf.add(CurvatureSpline.SplinePoint(xAt(i), yAt(i), pAt(i), tAt(i)))
        val m = splineBuf.size
        if (m == 1) {
            // The stroke's very first knot: the segment subdividers emit points AFTER their start
            // knot, so the head knot itself rides out here or the ribbon would begin one point in.
            val p = splineBuf[0]
            ribbon.append(p.x, p.y, p.pressure, p.time)
            return
        }
        if (m >= 4) {
            // Steady state: buffer [s-2, s-1, s, s+1]. Segment s-1→s just became final (its
            // lookahead s+1 is real) — emit it with p0 = s-2 behind it. The whole-stroke pass
            // computes this same segment with the same 4 knots, so the polylines match exactly.
            val p0 = splineBuf[m - 4]
            val p1 = splineBuf[m - 3]
            val p2 = splineBuf[m - 2]
            val p3 = splineBuf[m - 1]
            for (p in CurvatureSpline.subdivideSegmentPoints(p0, p1, p2, p3)) {
                ribbon.append(p.x, p.y, p.pressure, p.time)
            }
            // The final segment's subdivision includes p3 clamped when finished; live lookahead tail
            // is the end of segment s→s+1 computed with p3 clamped as lookahead.
            ribbon.setPendingTail(CurvatureSpline.subdivideSegmentPoints(p1, p2, p3, p3))
            val kept = ArrayList<CurvatureSpline.SplinePoint>(3)
            kept.add(splineBuf[m - 3])
            kept.add(splineBuf[m - 2])
            kept.add(splineBuf[m - 1])
            splineBuf = kept
        } else if (m == 3) {
            // The stroke's first segment (0→1) becomes final the moment sample 2 lands: the
            // whole-stroke pass runs it with p0 clamped onto p1 and the REAL sample 2 as p3.
            val head = splineBuf[0]
            val p2 = splineBuf[1]
            val p3 = splineBuf[2]
            for (p in CurvatureSpline.subdivideSegmentPoints(head, head, p2, p3)) {
                ribbon.append(p.x, p.y, p.pressure, p.time)
            }
            // Pending tail for segment 1->2 with tail clamp
            ribbon.setPendingTail(CurvatureSpline.subdivideSegmentPoints(head, p2, p3, p3))
        } else if (m == 2) {
            val p0 = splineBuf[0]
            val p1 = splineBuf[1]
            ribbon.setPendingTail(CurvatureSpline.subdivideSegmentPoints(p0, p0, p1, p1))
        }
    }

    /**
     * Pen-up: flush the spline pump's lookahead into the ribbon, spline-subdividing the held tail
     * with end-fenced knots (Clamp hold-and-extend in [CurvatureSpline.smoothAndSubdivide] already
     * fences the ends), so the committed ink and the wet ink share one continuous point sequence.
     * With the pump off this is a no-op — the ribbon already saw every raw sample.
     */
    fun finishWet() {
        val ribbon = wet ?: return
        if (!splineActive) return
        // The buffer holds the last segment's three knots [s-1, s, s+1]: the final segment s→s+1
        // with p3 clamped onto p2 — the same fence the committed whole-stroke pass applies at its
        // tail. This also replaces the pending lookahead tail (the ribbon appends over it), and
        // after it the ribbon has seen every spline point the committed stroke will subdivide.
        splinePending = emptyList()
        if (splineBuf.isEmpty()) return
        val m = splineBuf.size
        if (m >= 3) {
            // Buffer [a, b, c]: every segment up to a→b has been emitted; the pending one is
            // b→c, with p0 = a behind it and p3 clamped onto c — the whole-stroke pass's tail fence.
            val a = splineBuf[m - 3]
            val b = splineBuf[m - 2]
            val c = splineBuf[m - 1]
            ribbon.setPendingTail(emptyList())
            for (p in CurvatureSpline.subdivideSegmentPoints(a, b, c, c)) {
                ribbon.append(p.x, p.y, p.pressure, p.time)
            }
        } else if (m == 2) {
            // Two knots and no third: emit the raw segment (a 2-point spline is the polyline).
            ribbon.setPendingTail(emptyList())
            ribbon.append(splineBuf[1].x, splineBuf[1].y, splineBuf[1].pressure, splineBuf[1].time)
        }
        splineBuf.clear()
    }

    /**
     * The live ribbon, started on the first sample of a stroke whose pen settles as it is drawn.
     * Anything that edits the samples wholesale drops it; the next sample builds a fresh one over
     * what is already there and carries on from the end, so a stroke never falls back to rebuilding
     * per frame for good.
     */
    private fun wetOrStart(): WetRibbon? {
        wet?.let { return it }
        if (finished || !WetRibbon.supports(straight)) return null
        val w = WetRibbon(
            baseWidth = config.baseWidth,
            pressureEnabled = config.pressureEnabled,
            m = config.pressureMinFactor,
            ds = config.directionStrength,
            pressureGamma = config.pressureGamma,
            speedStrength = config.speedStrength,
            speedScale = speedScale,
            smooth = !straight,
            holdEnds = holdsEnds,
            taperEnabled = config.taperEnabled,
            taperMinFactor = config.taperMinFactor,
            nibAxis = nibAxis(),
            smoothScale = smoothScale,
            velLo = config.velocityLowerBound,
            velHi = config.velocityUpperBound,
            velAmp = config.velocityAmplifier,
            brushRatio = config.brushRatio,
            brushAngle = config.brushAngle,
            lagComp = lagComp,
            endVelocitySensitivity = config.endVelocitySensitivity,
            splineFed = config.adaptiveSpline,
        )
        // Everything before the sample being added; the caller appends that one itself. With the
        // spline pump on, the backfill runs the same pump over the existing samples so the ribbon
        // carries exactly the spline points the live frames drew — not raw backfill the pump would
        // later duplicate.
        if (splineActive) {
            splineBuf.clear()
            for (i in 0 until n - 1) pumpSpline(w, i)
        } else {
            for (i in 0 until n - 1) w.append(xAt(i), yAt(i), pAt(i), tAt(i))
        }
        return w.also { wet = it }
    }

    /**
     * Straight-line tools: collapse to a single segment from the pen-down sample to [end],
     * replacing any prior moving endpoint. The first sample's origin stays fixed while the far
     * end tracks the pointer, so the live preview and committed stroke are one straight ribbon.
     */
    fun setStraightEnd(end: Sample) {
        if (n > 1) n = 1
        addSample(end)
    }

    override fun paint(r: Renderer) {
        // Zoomed-out ink is a hairline: below a device pixel the ribbon has nothing to show that a
        // line cannot, and the ribbon is the expensive half of painting a dense page — a path of two
        // shapes per sample, built and scan-converted. The stroke itself is untouched (its geometry is
        // still built on demand for bounds, selection and erasing); only the mark it leaves changes.
        if (paintsAsHairline(r)) {
            paintHairline(r)
            return
        }
        val g = geometry()
        val color = renderColor
        when {
            // The dashed pen draws its (uniform-width) centreline as a dashed, round-capped
            // line rather than a solid ribbon; the full ribbon geometry is still used for
            // bounds/hit-testing/erasing, so the whole line stays selectable through the gaps.
            tool == Tool.DASHED || config.texture == StrokeTexture.DASHED -> paintDashed(r, g, color)
            config.texture == StrokeTexture.DOTTED -> paintDotted(r, g, color)
            // The highlighter never glows (a translucent marker; glow is meaningless there).
            (config.neon || config.texture == StrokeTexture.NEON) && tool != Tool.HIGHLIGHTER -> paintNeon(r, g, color)
            else -> {
                // Paint the C1-smooth ribbon directly with its exact renderColor (preserves identical color on live vs committed ink)
                paintFills(r, g, color)
            }
        }
    }

    /**
     * Whether this stroke's ink is too thin on [r]'s target to be worth a ribbon: the **widest** the
     * pen's own shaping could ever draw it is under [HAIRLINE_MAX_DEVICE_PX] device pixels, so a
     * hairline is what comes out either way.
     *
     * Only the plain smooth pens qualify. A texture needs the ribbon to carry the grain, neon needs it
     * for the halo, a translucent stroke needs the layer it composites in, the highlighter is wide by
     * definition, and a one-sample stroke is a single disc that costs nothing to build.
     */
    private fun paintsAsHairline(r: Renderer): Boolean {
        if (n < 2 || straight) return false
        if (config.texture != StrokeTexture.SMOOTH) return false
        if (config.neon || tool == Tool.HIGHLIGHTER) return false
        if (renderColor.a < 255) return false
        // Measured on the widest possible mark, a dot's DOT_DIR_Y included, so the ribbon is never
        // dropped while the ink could still be visibly thick.
        val widest = config.baseWidth * (1.0 + StrokeEngine.DOT_DIR_Y * config.directionStrength) * r.deviceScale
        return widest < HAIRLINE_MAX_DEVICE_PX
    }

    /** The stroke as one cosmetic hairline down its raw samples — a device-pixel line that stays
     *  readable at any zoom, standing in for the ribbon the page is too small to show. */
    private fun paintHairline(r: Renderer) {
        val pts = FloatArray(2 * n)
        for (i in 0 until n) {
            pts[2 * i] = xAt(i).toFloat()
            pts[2 * i + 1] = yAt(i).toFloat()
        }
        r.strokePolyline(pts, Pen(color = renderColor, width = HAIRLINE_DEVICE_PX))
    }

    // The ribbon is a circular brush disc swept down the centreline: a disc at every sample plus
    // the body bridging them. Round caps and joins come for free on every pen, and the union of
    // convex pieces can't hole the way one self-overlapping outline does at a sharp turn.
    private fun paintFills(r: Renderer, g: StrokeGeometry, color: Rgba) {
        if (r is com.pnotes.platform.AndroidRenderer && wet == null) {
            val path = (cachedNativePath as? android.graphics.Path) ?: run {
                com.pnotes.platform.AndroidRenderer.buildStrokePath(
                    g.centerline, g.halfWidths, !cutStart, !cutEnd, config.texture,
                ).also { cachedNativePath = it }
            }
            r.drawCachedPath(path, color, config.texture)
            return
        }
        r.fillDiskRibbon(g.centerline, g.halfWidths, color, capStart = !cutStart, capEnd = !cutEnd, texture = config.texture)
    }

    /**
     * Paint just the opaque highlighter ribbon (full alpha, no blend) into [r]. The canvas
     * pre-renders this once into a bitmap and composites it live at [renderColor]'s alpha with
     * MULTIPLY, so a self-overlapping highlight isn't re-tessellated every frame.
     */
    fun paintHighlighterRibbon(r: Renderer) {
        paintFills(r, geometry(), renderColor.withAlpha(255))
    }

    /**
     * Paint [count] points of the live ribbon starting at [from]. The wet cache draws a stroke in
     * two runs — the settled one baked into a raster once, the moving one over it every frame — and
     * a growing ribbon's arrays are over-allocated, so neither run is describable by array length.
     *
     * Only for opaque, non-neon ink, which is the only kind the cache takes: overlapping runs of a
     * solid colour union to the same colour, while a translucent one would darken where they meet
     * and a bloom would compound. [dashPhase] is how far into the dash pattern this run starts, so
     * the dashed pen's rhythm carries across the seam instead of restarting at it.
     */
    fun paintRun(r: Renderer, ribbon: WetRibbon, from: Int, count: Int, dashPhase: Double) {
        if (count <= 0) return
        val color = renderColor
        val centers = ribbon.centerlineArray()
        if ((tool == Tool.DASHED || config.texture == StrokeTexture.DASHED) && ribbon.pointCount >= 2) {
            r.strokePolyline(
                centers, from, count,
                Pen(
                    color = color,
                    width = config.baseWidth,
                    cosmetic = false,
                    dashed = true,
                    dashOn = config.dashLength,
                    dashGap = config.dashGap,
                    dashPhase = dashPhase,
                ),
            )
        } else if (config.texture == StrokeTexture.DOTTED && ribbon.pointCount >= 2) {
            r.strokePolyline(
                centers, from, count,
                Pen(
                    color = color,
                    width = config.baseWidth,
                    cosmetic = false,
                    dashed = true,
                    dashOn = 0.1,
                    dashGap = (config.baseWidth * 1.8).coerceAtLeast(4.0),
                    dashPhase = dashPhase,
                ),
            )
        } else {
            r.fillDiskRibbon(centers, ribbon.halfWidthArray(), from, count, color, texture = config.texture)
        }
    }

    /** Whether the wet cache may bake this stroke's settled ink into a raster. Neon stacks blurred
     *  layers composited at an alpha, and translucent ink compounds where it overlaps itself, so
     *  neither survives being painted in two runs; both keep the plain every-frame redraw. */
    val wetCacheable: Boolean
        get() = !config.neon && renderColor.a >= 255 && config.texture == StrokeTexture.SMOOTH

    /** Dotted pen: round circular dots spaced evenly along the stroke path. */
    private fun paintDotted(r: Renderer, g: StrokeGeometry, color: Rgba) {
        if (g.pointCount >= 2) {
            r.strokePolyline(
                g.centerline,
                Pen(
                    color = color,
                    width = config.baseWidth,
                    cosmetic = false,
                    dashed = true,
                    dashOn = 0.1,
                    dashGap = (config.baseWidth * 1.8).coerceAtLeast(4.0),
                ),
            )
        } else {
            r.fillDiskRibbon(g.centerline, g.halfWidths, color, texture = StrokeTexture.DOTTED)
        }
    }

    /**
     * The dashed pen: a constant-width, round-capped dashed line traced down the smoothed
     * centreline (so its rounded dashes match the tool's icon). Dash/gap runs are in content
     * px so they scale with zoom like the ink. A single tap (no line) is drawn as a dot.
     */
    private fun paintDashed(r: Renderer, g: StrokeGeometry, color: Rgba) {
        if (g.pointCount >= 2) {
            r.strokePolyline(
                g.centerline,
                Pen(
                    color = color,
                    width = config.baseWidth,
                    cosmetic = false,
                    dashed = true,
                    dashOn = config.dashLength,
                    dashGap = config.dashGap,
                ),
            )
        } else {
            r.fillDiskRibbon(g.centerline, g.halfWidths, color)
        }
    }

    /**
     * Neon as a laser trail. Four layers, back to front:
     *   1. a wide, faint outer bloom (the blurred ribbon in the ink colour);
     *   2. a tighter, brighter bloom that keeps the colour saturated at the line;
     *   3. the tube body: the saturated colour, lifted slightly toward white so it
     *      reads as lit;
     *   4. the white-hot core: a thin *solid* white inner ribbon (no blur, so it
     *      reaches full white even on a 1px line instead of smudging out).
     * Each bloom composites once at a glow-intensity alpha so a self-overlapping
     * scribble can't compound into a darker blob. [config.neonStrength] scales the
     * bloom's size and brightness; it overrides the translucent path, so neon works
     * on any stroke tool.
     */
    internal fun neonGlowRadius(): Double {
        val s = config.neonStrength.coerceIn(0.0, 1.0)
        return (config.baseWidth * (NEON_BLOOM_WIDE_FACTOR_MIN + NEON_BLOOM_WIDE_FACTOR_SPAN * s))
            .coerceAtLeast(NEON_BLOOM_WIDE_MIN)
    }

    override fun paintBounds(): Rect =
        if ((config.neon || config.texture == StrokeTexture.NEON) && tool != Tool.HIGHLIGHTER) bounds().outset(neonGlowRadius() * 2 + 4)
        else if (config.texture == StrokeTexture.CHALK || config.texture == StrokeTexture.CHARCOAL) bounds().outset(12.0)
        else bounds()

    private fun paintNeon(r: Renderer, g: StrokeGeometry, color: Rgba) {
        val s = config.neonStrength.coerceIn(0.0, 1.0)
        val body = color.withAlpha(255)

        // 1) Wide, faint outer bloom.
        paintBloom(r, g, body, neonGlowRadius(), NEON_BLOOM_WIDE_ALPHA_MIN + NEON_BLOOM_WIDE_ALPHA_SPAN * s)

        // 2) Tight, brighter bloom: saturated colour hugging the line.
        val tightR = (config.baseWidth * (NEON_BLOOM_TIGHT_FACTOR_MIN + NEON_BLOOM_TIGHT_FACTOR_SPAN * s))
            .coerceAtLeast(NEON_BLOOM_TIGHT_MIN)
        paintBloom(r, g, body, tightR, NEON_BLOOM_TIGHT_ALPHA_MIN + NEON_BLOOM_TIGHT_ALPHA_SPAN * s)

        // 3) Tube body, lifted slightly toward white so it reads as lit.
        paintFills(r, g, lighten(body, NEON_BODY_LIGHTEN))

        // 4) Solid white-hot core (no blur, so thin lines still read pure white): the same swept
        //    disc at a fraction of the width, so it rounds with the body on every pen.
        val core = FloatArray(g.halfWidths.size) { (g.halfWidths[it] * NEON_CORE_FRAC).toFloat() }
        r.fillDiskRibbon(g.centerline, core, Rgba(255, 255, 255, 255))
    }

    /** One bloom pass: the blurred ribbon body plus its two rounded ends, composited once at [alpha]. */
    private fun paintBloom(r: Renderer, g: StrokeGeometry, color: Rgba, radius: Double, alpha: Double) {
        r.saveLayerAlpha(paintBounds(), alpha)
        if (g.outlineCount >= 3) r.fillPolygonGlow(g.outline, color, FillRule.NONZERO, radius)
        val n = g.pointCount
        if (n > 0) {
            if (g.hw(0) > 0.0) r.fillCircleGlow(Pt(g.cx(0), g.cy(0)), g.hw(0), color, radius)
            if (n > 1 && g.hw(n - 1) > 0.0) r.fillCircleGlow(Pt(g.cx(n - 1), g.cy(n - 1)), g.hw(n - 1), color, radius)
        }
        r.restore()
    }

    /** [c] lerped a fraction [t] toward white (alpha preserved). */


    override fun bounds(): Rect {
        // A live ribbon keeps a running box over its settled points, so a growing stroke's bounds
        // cost what its moving tail costs rather than a walk of every point it has laid down.
        wet?.let { return it.bounds() }
        cachedBounds?.let { return it }
        val g = geometry()
        if (g.pointCount == 0) {
            val b = if (n == 0) Rect(0.0, 0.0, 0.0, 0.0) else rawBounds()
            return b.also { cachedBounds = it }
        }
        // Bound the swept-disc ribbon by the discs themselves: each centre +/- its half-width holds
        // that sample's disc, and the bridging body never reaches past the discs it joins, so this
        // covers the round caps every pen now grows (the old outline box clipped them flat).
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for (i in 0 until g.pointCount) {
            val cx = g.cx(i)
            val cy = g.cy(i)
            val h = g.hw(i)
            if (cx - h < minX) minX = cx - h
            if (cx + h > maxX) maxX = cx + h
            if (cy - h < minY) minY = cy - h
            if (cy + h > maxY) maxY = cy + h
        }
        return Rect(minX, minY, maxX - minX, maxY - minY).also { cachedBounds = it }
    }

    /** O(1): the samples are offsets, so moving the stroke only moves the origin they hang off. */
    override fun translate(dx: Double, dy: Double) {
        ox += dx
        oy += dy
        wet = null
        cachedGeometry = null
        cachedNativePath = null
        cachedRawBounds = cachedRawBounds?.translate(dx, dy)
        cachedBounds = cachedBounds?.translate(dx, dy)
    }

    /** Install the stylus orientation channels wholesale, one value per sample (codec load), or null
     *  to clear them. A channel of all zeros is dropped rather than kept: a stroke whose ink carries no
     *  orientation is one the channel was never allocated for, and it should not start paying for it
     *  after a reload. */
    fun setOrientation(tilts: List<Double>?, azimuths: List<Double>?) {
        tta = packChannel(tilts)
        aza = packChannel(azimuths)
        // The channels feed the nib's broad axis, so replacing them rewrites the ink: everything
        // derived from the old ones has to go, exactly as a wholesale sample edit would.
        invalidate()
    }

    /** One sample channel packed from a codec list; null when the list is absent, the wrong length for
     *  the samples, or carries nothing but zeros. */
    private fun packChannel(values: List<Double>?): FloatArray? {
        if (values == null || values.size != n || n == 0 || values.all { it == 0.0 }) return null
        return FloatArray(n) { values[it].toFloat() }
    }

    override fun snapshotGeometry(): GeometrySnapshot =
        StrokeSnapshot(
            ox, oy, xa.copyOf(n), ya.copyOf(n), pa.copyOf(n), ta?.copyOf(n), tta?.copyOf(n),
            aza?.copyOf(n), config,
        )

    override fun restoreGeometry(snap: GeometrySnapshot) {
        if (snap !is StrokeSnapshot) return
        ox = snap.ox
        oy = snap.oy
        n = snap.xs.size
        xa = snap.xs.copyOf()
        ya = snap.ys.copyOf()
        pa = snap.ps.copyOf()
        ta = snap.ts?.copyOf()
        tta = snap.tilts?.copyOf()
        aza = snap.azimuths?.copyOf()
        config = snap.config
        invalidate()
    }

    /** Transform every sample and scale the width-bearing style fields (base width, taper, dash
     *  runs) by the transform's linear factor, so a resized stroke looks zoomed; a pure rotation
     *  (factor 1) leaves the width untouched. */
    override fun applyTransform(t: Affine) {
        if (n > 0) {
            // Re-origin on the transformed first sample so the offsets stay stroke-local; a rotation
            // about a far-away pivot would otherwise push them out to the pivot's magnitude.
            val first = t.apply(Pt(ox + xa[0], oy + ya[0]))
            for (i in 0 until n) {
                val p = t.apply(Pt(ox + xa[i], oy + ya[i]))
                xa[i] = (p.x - first.x).toFloat()
                ya[i] = (p.y - first.y).toFloat()
            }
            ox = first.x
            oy = first.y
        }
        val k = t.linearScale
        if (k != 1.0) {
            config = config.copy(
                baseWidth = config.baseWidth * k,
                dashLength = config.dashLength * k,
                dashGap = config.dashGap * k,
            )
        }
        invalidate()
    }

    /** `p` within the swept disc: inside any sample's disc (catching the dot, ends and joins) or
     *  inside the ribbon body. */
    override fun contains(p: Pt): Boolean {
        // Fast AABB rejection: if p is outside paintBounds (with 2px tolerance for brush cap), reject in O(1)
        if (!paintBounds().outset(2.0).contains(p)) return false
        val g = geometry()
        for (i in 0 until g.pointCount) {
            val h = g.hw(i)
            if (h <= 0.0) continue
            val dx = p.x - g.cx(i)
            val dy = p.y - g.cy(i)
            if (dx * dx + dy * dy <= h * h) return true
        }
        return g.bodyContains(p)
    }

    /** Mean of the sample positions. */
    override fun centroid(): Pt {
        if (n == 0) return Pt.ZERO
        var sx = 0.0
        var sy = 0.0
        for (i in 0 until n) {
            sx += xa[i]
            sy += ya[i]
        }
        return Pt(ox + sx / n, oy + sy / n)
    }

    /**
     * AABB of the *raw* input samples, cached and invalidated with the geometry.
     * Built in one allocation-free pass (no `samples.map { it.pos }`), so an eraser
     * sweep that bbox-rejects thousands of strokes per move stays cheap.
     */
    private fun rawBounds(): Rect {
        cachedRawBounds?.let { return it }
        var minX = xa[0]
        var minY = ya[0]
        var maxX = minX
        var maxY = minY
        for (i in 1 until n) {
            val x = xa[i]
            val y = ya[i]
            if (x < minX) minX = x else if (x > maxX) maxX = x
            if (y < minY) minY = y else if (y > maxY) maxY = y
        }
        return Rect(ox + minX, oy + minY, (maxX - minX).toDouble(), (maxY - minY).toDouble())
            .also { cachedRawBounds = it }
    }

    /**
     * Whether this stroke's rendered half-width differs from the raw `baseWidth · pressure` radius
     * the cheap eraser math assumes: the calligraphic nib multiplies by its direction term, the
     * speed pen thins with velocity, and the taper pen eases its tail.
     *
     * The plain pen is the common case and keeps the allocation-free per-sample path. The others do
     * not, because measuring them at their nominal width deletes ink that is plainly visible (a nib
     * on its thin face renders at a fraction of it) and spares ink that is plainly not (the same nib
     * opened past its base width, or a dot built at [StrokeEngine.DOT_DIR_Y]).
     */
    private val widthIsShapedByPen: Boolean
        get() = config.directionStrength != 0.0 || config.speedStrength > 0.0 || config.taperEnabled

    /**
     * The ribbon the eraser and the hit test measure the ink against, or null when the rendered
     * width *is* the raw pressure radius and a full build would cost a pass to learn nothing.
     */
    private fun eraseGeometry(): StrokeGeometry? {
        if (!widthIsShapedByPen) return null
        val g = geometry()
        return if (g.pointCount >= n) g else null // one geometry point per sample, or fall back
    }

    /** Half-width the eraser and the hit test measure against at sample [i] — the rendered radius for
     *  a stroke a pen shapes, the raw pressure radius otherwise — floored so a hairline still has a
     *  grabbable edge. Both callers use this one value, which is what keeps them agreeing on where
     *  the ink ends (a tap that selects a stroke must be one the eraser would touch). */
    private fun eraseHalfWidth(i: Int, baseHw: Double, g: StrokeGeometry?): Double {
        val hw = if (g != null) g.hw(i) else baseHw * (if (i < pa.size) pa[i].toDouble() else 1.0)
        return hw.coerceAtLeast(MIN_ERASE_HW)
    }

    /**
     * Cheap sample test after a bounding-box reject (spec 02 §5.1).
     *
     * The comparisons are **strict**, which is what makes this the exact complement of [erasedBy]'s
     * keep test (`dist >= radius + hw`): a circle the eraser leaves untouched must not be a circle a
     * tap selects. At the boundary a shared width widens the ink to a closed disc, and a disc that
     * is merely tangent overlaps nothing — there is no area to shave, so calling it a hit would have
     * the eraser hand back a "modified" fragment identical to the stroke it came from.
     */
    override fun intersectsCircle(cx: Double, cy: Double, radius: Double): Boolean {
        if (n == 0) return false
        val baseHw = config.baseWidth * 0.5
        val g = eraseGeometry()
        // Reject against the box the ink actually covers: the rendered disc box for a pen that shapes
        // the width, the raw sample box (the smoothed geometry lags inward) otherwise. Both are
        // measured against the widest radius a sample may end up with, floor included, so the reject
        // can never cut off a sample the loop below would have kept.
        if (g != null) {
            if (!bounds().outset(radius + MIN_ERASE_HW).contains(Pt(cx, cy))) return false
        } else if (rawBounds().distanceTo(Pt(cx, cy)) > radius + maxOf(baseHw, MIN_ERASE_HW)) {
            return false
        }
        // A straight stroke carries only its two endpoints, so a mid-line tap falls between
        // samples; test the segments they span (point-to-line) instead of the sample points.
        if (straight) {
            val maxR = radius + maxOf(baseHw, MIN_ERASE_HW)
            val c = Pt(cx, cy)
            if (n == 1) return c.distanceTo(Pt(xAt(0), yAt(0))) < maxR
            for (i in 1 until n) {
                val a = Pt(xAt(i - 1), yAt(i - 1))
                val b = Pt(xAt(i), yAt(i))
                if (Geometry.distancePointToSegment(c, a, b) < maxR) return true
            }
            return false
        }
        if (g != null) {
            for (i in 0 until n) {
                val dx = g.cx(i) - cx
                val dy = g.cy(i) - cy
                val touchR = radius + eraseHalfWidth(i, baseHw, g)
                if (dx * dx + dy * dy < touchR * touchR) return true
            }
            return false
        }
        // Compare in offset space so the eraser sweep does no per-sample origin arithmetic.
        val lx = cx - ox
        val ly = cy - oy
        for (i in 0 until n) {
            val dx = xa[i] - lx
            val dy = ya[i] - ly
            val hw = eraseHalfWidth(i, baseHw, null)
            val touchR = radius + hw
            if (dx * dx + dy * dy < touchR * touchR) return true
        }
        return false
    }

    /**
     * AREA-erase: the surviving fragments after an eraser circle (page-local [cx], [cy], [radius])
     * passes over this stroke. Supports 2D edge shaving / carving:
     *  - Touching only the outer edge / half-width shaves the stroke thinner rather than deleting it.
     *  - Cutting through the middle splits the stroke into fragments with sharp, flat cuts.
     *  - Covering the whole stroke removes it completely.
     */
    fun erasedBy(cx: Double, cy: Double, radius: Double): List<Stroke>? {
        if (n == 0) return null
        val baseHw = config.baseWidth * 0.5
        val g = eraseGeometry()
        // Same reject as [intersectsCircle], and on the same width, so the two agree on the whole
        // plane rather than only near the samples.
        if (g != null) {
            if (!bounds().outset(radius + MIN_ERASE_HW).contains(Pt(cx, cy))) return null
        } else if (rawBounds().distanceTo(Pt(cx, cy)) > radius + maxOf(baseHw, MIN_ERASE_HW)) {
            return null
        }
        if (straight) return if (intersectsCircle(cx, cy, radius)) emptyList() else null

        val lx = cx - ox
        val ly = cy - oy
        var anyErased = false
        var anyModified = false

        val outX = FloatArray(n)
        val outY = FloatArray(n)
        val outP = FloatArray(n)
        val outT = if (ta != null) FloatArray(n) else null
        val outTilt = if (tta != null) FloatArray(n) else null
        val outAzi = if (aza != null) FloatArray(n) else null
        val keep = BooleanArray(n)

        for (i in 0 until n) {
            val dx = if (g != null) g.cx(i) - cx else xa[i] - lx
            val dy = if (g != null) g.cy(i) - cy else ya[i] - ly
            val dist = kotlin.math.hypot(dx, dy)
            val pVal = if (i < pa.size) pa[i].toDouble() else 1.0
            val hw = eraseHalfWidth(i, baseHw, g)

            if (dist >= radius + hw) {
                // Completely outside eraser
                outX[i] = xa[i]
                outY[i] = ya[i]
                outP[i] = pa[i]
                if (outT != null && ta != null && i < ta!!.size) outT[i] = ta!![i]
                if (outTilt != null && tta != null && i < tta!!.size) outTilt[i] = tta!![i]
                if (outAzi != null && aza != null && i < aza!!.size) outAzi[i] = aza!![i]
                keep[i] = true
            } else if (dist <= radius && radius >= hw * 0.7) {
                // Eraser covers the centerline and most of the width -> cut through
                anyErased = true
                keep[i] = false
            } else {
                // Partial / half-width carving (shave the side of the stroke without bending the centerline)
                anyErased = true
                anyModified = true
                val overlap = (radius + hw) - dist
                val newHw = (hw - overlap).coerceAtLeast(0.5)

                outX[i] = xa[i]
                outY[i] = ya[i]
                outP[i] = (pVal * (newHw / hw)).coerceIn(0.05, 2.0).toFloat()
                if (outT != null && ta != null && i < ta!!.size) outT[i] = ta!![i]
                if (outTilt != null && tta != null && i < tta!!.size) outTilt[i] = tta!![i]
                if (outAzi != null && aza != null && i < aza!!.size) outAzi[i] = aza!![i]
                keep[i] = true
            }
        }

        if (!anyErased && !anyModified) return null

        val fragments = mutableListOf<Stroke>()
        var runStart = -1
        for (i in 0 until n) {
            if (keep[i]) {
                if (runStart < 0) runStart = i
            } else {
                if (runStart >= 0) {
                    fragments.add(
                        buildFragment(
                            runStart, i, outX, outY, outP, outT, outTilt, outAzi,
                            isCutStart = runStart > 0 || cutStart, isCutEnd = true,
                        ),
                    )
                    runStart = -1
                }
            }
        }
        if (runStart >= 0) {
            fragments.add(
                buildFragment(
                    runStart, n, outX, outY, outP, outT, outTilt, outAzi,
                    isCutStart = runStart > 0 || cutStart, isCutEnd = cutEnd,
                ),
            )
        }

        return fragments
    }

    /** A new stroke from samples `[from, to)` with modified coordinates and pressures. */
    private fun buildFragment(
        from: Int,
        to: Int,
        srcX: FloatArray,
        srcY: FloatArray,
        srcP: FloatArray,
        srcT: FloatArray?,
        srcTilt: FloatArray?,
        srcAzi: FloatArray?,
        isCutStart: Boolean,
        isCutEnd: Boolean,
    ): Stroke {
        val m = to - from
        val s = Stroke(tool, config, emptyList(), speedScale, straight, smoothScale, lagComp)
        s.ox = ox + srcX[from]
        s.oy = oy + srcY[from]
        s.n = m
        s.xa = FloatArray(m) { srcX[from + it] - srcX[from] }
        s.ya = FloatArray(m) { srcY[from + it] - srcY[from] }
        s.pa = FloatArray(m) { srcP[from + it] }
        s.ta = srcT?.let { FloatArray(m) { srcT[from + it] } }
        s.tta = srcTilt?.let { FloatArray(m) { srcTilt[from + it] } }
        s.aza = srcAzi?.let { FloatArray(m) { srcAzi[from + it] } }
        s.cutStart = isCutStart
        s.cutEnd = isCutEnd
        // The tail is the stroke's last samples, so a fragment keeps the part of it that fell
        // inside the fragment. A fragment that ends before the tail started carries none.
        val tailStart = tipFrom
        s.tipCount = if (tailStart < 0) 0 else (minOf(to, tailStart + tipCount) - maxOf(from, tailStart)).coerceAtLeast(0)
        return s
    }

    companion object {
        const val KIND = "stroke"

        private val EMPTY_F = FloatArray(0)

        /** First allocation for a live stroke; grows by doubling from here. */
        private const val INITIAL_CAPACITY = 32

        /** Zoomed-out LOD: a stroke whose widest possible rendered mark is under this many device
         *  pixels is painted as a hairline instead of a ribbon. */
        private const val HAIRLINE_MAX_DEVICE_PX = 1.6

        /** The hairline's own width, in device pixels — cosmetic, so it does not scale with zoom. */
        private const val HAIRLINE_DEVICE_PX = 1.0

        /** Floor on the half-width the eraser and the hit test measure (content px): a hairline still
         *  has a grabbable edge, and a single shared floor is what keeps the two testers consistent
         *  on the width they read. */
        private const val MIN_ERASE_HW = 1.0

        /** [c] lerped a fraction [t] toward white (alpha preserved). */
        internal fun lighten(c: Rgba, t: Double): Rgba {
            val f = t.coerceIn(0.0, 1.0)
            return Rgba(
                (c.r + (255 - c.r) * f).toInt(),
                (c.g + (255 - c.g) * f).toInt(),
                (c.b + (255 - c.b) * f).toInt(),
                c.a,
            )
        }

        /** Neon bloom: two stacked NORMAL-blur passes, a wide faint halo under a
         *  tighter brighter one. Radius = base_width * (MIN + SPAN * neonStrength),
         *  floored in page px; alpha = MIN + SPAN * neonStrength. */
        internal const val NEON_BLOOM_WIDE_FACTOR_MIN = 1.8
        internal const val NEON_BLOOM_WIDE_FACTOR_SPAN = 5.0
        internal const val NEON_BLOOM_WIDE_MIN = 6.0
        internal const val NEON_BLOOM_WIDE_ALPHA_MIN = 0.0
        internal const val NEON_BLOOM_WIDE_ALPHA_SPAN = 0.42

        internal const val NEON_BLOOM_TIGHT_FACTOR_MIN = 0.7
        internal const val NEON_BLOOM_TIGHT_FACTOR_SPAN = 1.8
        internal const val NEON_BLOOM_TIGHT_MIN = 2.5
        internal const val NEON_BLOOM_TIGHT_ALPHA_MIN = 0.0
        internal const val NEON_BLOOM_TIGHT_ALPHA_SPAN = 0.85

        /** Fraction of the tube width filled by the solid white-hot core. */
        internal const val NEON_CORE_FRAC = 0.3

        /** Body colour lifted this fraction toward white so the tube reads as lit. */
        internal const val NEON_BODY_LIGHTEN = 0.10
    }
}

/** Snapshot of a stroke's transformable geometry: the sample path and the width-bearing style.
 *  Holds the packed arrays rather than a list of [Sample]s, so a resize drag snapshotting a big
 *  selection every frame does not allocate one object per sample. */
private class StrokeSnapshot(
    val ox: Double,
    val oy: Double,
    val xs: FloatArray,
    val ys: FloatArray,
    val ps: FloatArray,
    val ts: FloatArray?,
    val tilts: FloatArray?,
    val azimuths: FloatArray?,
    val config: ToolConfig,
) : GeometrySnapshot
