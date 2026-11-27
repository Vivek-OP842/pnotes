package com.pnotes.core.model

import com.pnotes.core.geometry.Affine
import com.pnotes.core.geometry.Geometry
import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.pal.Pen
import com.pnotes.core.pal.Renderer
import com.pnotes.core.tools.ShapeKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * An editable geometric shape (spec 02 §5.4): open (line/arrow, two endpoints)
 * or closed (rectangle/ellipse/triangle, drawn inside the normalized AABB of
 * start/end). Polygon/polyline additionally carry a vertex list in [points].
 * Closed shapes are stroked and optionally filled. Shapes erase like ink: whole
 * on contact in STROKE mode, and cut into outline fragments by the AREA eraser.
 */
class ShapeItem(
    var shape: ShapeKind,
    var start: Pt,
    var end: Pt,
    var strokeRgba: Rgba,
    var strokeWidth: Double = 3.0,
    var fillRgba: Rgba? = null,
    /** Render the outline as a glowing neon tube (halo + white-hot core). */
    var neon: Boolean = false,
    /** Glow intensity in [0, 1] (halo size + brightness); used only when [neon]. */
    var neonStrength: Double = 0.6,
    /**
     * Vertices for [ShapeKind.POLYGON]/[ShapeKind.POLYLINE], stored normalized to the
     * unit box so they scale with [start]/[end] for free; null for every other kind.
     */
    var points: List<Pt>? = null,
    /** Stroke the outline dashed (dash/gap run lengths in content px). */
    var dashed: Boolean = false,
    var dashLength: Double = 10.0,
    var dashGap: Double = 8.0,
) : CanvasItem, Resizable {

    override val kind = KIND
    override val resizable = true
    override var locked = false

    /** Normalized AABB of the two drag points. */
    val box: Rect get() = Rect.fromPoints(start, end)

    /** Absolute (content-space) key control points or vertices. */
    internal fun absControlPoints(): List<Pt> {
        val pts = points ?: return when (shape) {
            ShapeKind.POLYLINE -> generatePolylinePoints(start, end)
            ShapeKind.CURVE -> generateCurvePoints(start, end)
            else -> emptyList()
        }
        val b = box
        return pts.map { Pt(b.left + it.x * b.w, b.top + it.y * b.h) }
    }

    /** Polygon/polyline/curve vertices mapped from normalized storage into content space.
     *  For CURVE, samples cubic Bézier segments defined by the control point sequence. */
    internal fun absPoints(): List<Pt> {
        val ctrl = absControlPoints()
        if (shape == ShapeKind.CURVE) {
            val bez = toBezierPoints(ctrl)
            val numSegments = (bez.size - 1) / 3
            if (numSegments <= 0) return ctrl
            val result = ArrayList<Pt>(numSegments * 24 + 1)
            for (s in 0 until numSegments) {
                val p0 = bez[3 * s]
                val p1 = bez[3 * s + 1]
                val p2 = bez[3 * s + 2]
                val p3 = bez[3 * s + 3]
                val steps = 24
                for (step in 0 until steps) {
                    val t = step.toDouble() / steps
                    val u = 1.0 - t
                    val tt = t * t
                    val uu = u * u
                    val uuu = uu * u
                    val ttt = tt * t
                    val pt = Pt(
                        uuu * p0.x + 3.0 * uu * t * p1.x + 3.0 * u * tt * p2.x + ttt * p3.x,
                        uuu * p0.y + 3.0 * uu * t * p1.y + 3.0 * u * tt * p2.y + ttt * p3.y,
                    )
                    result.add(pt)
                }
            }
            result.add(bez.last())
            return result
        }
        return ctrl
    }

    /** Absolute (content-space) vertices for polygon/polyline kinds; null for the rest. */
    fun vertices(): List<Pt>? = if (points == null) null else absPoints()

    /**
     * Inserts a new control knot into the CURVE at content-space position [p].
     * Finds the closest segment on the Catmull-Rom spline, projects [p] onto it,
     * and inserts the new point between the surrounding control points.
     */
    fun insertCurvePoint(p: Pt): Boolean {
        if (shape != ShapeKind.CURVE) return false
        val ctrl = absControlPoints().toMutableList()
        if (ctrl.size < 2) return false

        val n = ctrl.size
        var bestDist = Double.MAX_VALUE
        var bestInsertIdx = 1
        var bestPointOnCurve = p

        for (i in 0 until n - 1) {
            val p0 = if (i > 0) ctrl[i - 1] else ctrl[0] * 3.0 - ctrl[1] * 3.0 + ctrl[2.coerceAtMost(n - 1)]
            val p1 = ctrl[i]
            val p2 = ctrl[i + 1]
            val p3 = if (i + 2 < n) ctrl[i + 2] else ctrl[n - 1] * 3.0 - ctrl[n - 2] * 3.0 + ctrl[(n - 3).coerceAtLeast(0)]

            val steps = 16
            var prevPt = p1
            for (s in 1..steps) {
                val t = s.toDouble() / steps
                val t2 = t * t
                val t3 = t2 * t
                val curPt = Pt(
                    0.5 * ((2.0 * p1.x) + (-p0.x + p2.x) * t + (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2 + (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * t3),
                    0.5 * ((2.0 * p1.y) + (-p0.y + p2.y) * t + (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2 + (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * t3)
                )
                val proj = closestPointOnSegment(p, prevPt, curPt)
                val d = p.distanceTo(proj)
                if (d < bestDist) {
                    bestDist = d
                    bestInsertIdx = i + 1
                    bestPointOnCurve = proj
                }
                prevPt = curPt
            }
        }

        ctrl.add(bestInsertIdx, bestPointOnCurve)

        val minX = ctrl.minOf { it.x }
        val minY = ctrl.minOf { it.y }
        val maxX = ctrl.maxOf { it.x }
        val maxY = ctrl.maxOf { it.y }
        start = Pt(minX, minY)
        end = Pt(maxX, maxY)
        val bw = max(1e-4, maxX - minX)
        val bh = max(1e-4, maxY - minY)
        points = ctrl.map { Pt((it.x - minX) / bw, (it.y - minY) / bh) }
        return true
    }

    private fun closestPointOnSegment(p: Pt, a: Pt, b: Pt): Pt {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val lenSq = abx * abx + aby * aby
        if (lenSq < 1e-12) return a
        val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / lenSq).coerceIn(0.0, 1.0)
        return Pt(a.x + t * abx, a.y + t * aby)
    }

    /** Returns list of (knotIndex, isRight, handlePoint) for all active tangent handles in page space. */
    fun tangentHandles(): List<Triple<Int, Boolean, Pt>> {
        if (shape != ShapeKind.CURVE) return emptyList()
        val bez = toBezierPoints(absControlPoints())
        val numKnots = (bez.size - 1) / 3 + 1
        val result = ArrayList<Triple<Int, Boolean, Pt>>()

        for (k in 0 until numKnots) {
            if (k > 0) {
                result.add(Triple(k, false, bez[3 * k - 1]))
            }
            if (k < numKnots - 1) {
                result.add(Triple(k, true, bez[3 * k + 1]))
            }
        }
        return result
    }

    /** Adjusts the curve's curvature by dragging the tangent handle at [index] (left or right).
     *  Keeps all other knots and handles 100% stationary. */
    fun moveTangent(index: Int, isRight: Boolean, newPos: Pt) {
        if (shape != ShapeKind.CURVE) return
        val bez = toBezierPoints(absControlPoints()).toMutableList()
        val numKnots = (bez.size - 1) / 3 + 1
        if (index !in 0 until numKnots) return

        val kIdx = 3 * index
        val knot = bez[kIdx]

        if (isRight && index < numKnots - 1) {
            bez[kIdx + 1] = newPos
            if (index > 0) {
                val v = newPos - knot
                if (v.length() > 1e-4) {
                    val leftArmLen = bez[kIdx - 1].distanceTo(knot)
                    bez[kIdx - 1] = knot - v.normalized() * leftArmLen
                }
            }
        } else if (!isRight && index > 0) {
            bez[kIdx - 1] = newPos
            if (index < numKnots - 1) {
                val v = newPos - knot
                if (v.length() > 1e-4) {
                    val rightArmLen = bez[kIdx + 1].distanceTo(knot)
                    bez[kIdx + 1] = knot - v.normalized() * rightArmLen
                }
            }
        }

        val minX = bez.minOf { it.x }
        val minY = bez.minOf { it.y }
        val maxX = bez.maxOf { it.x }
        val maxY = bez.maxOf { it.y }
        start = Pt(minX, minY)
        end = Pt(maxX, maxY)
        val bw = max(1e-4, maxX - minX)
        val bh = max(1e-4, maxY - minY)
        points = bez.map { Pt((it.x - minX) / bw, (it.y - minY) / bh) }
    }


    private fun pen() =
        Pen(color = strokeRgba, width = strokeWidth, cosmetic = false, dashed = dashed, dashOn = dashLength, dashGap = dashGap)

    /** Triangle vertices: apex at top-edge midpoint, base along the bottom edge, or custom 3 vertices if edited. */
    internal fun triangleVertices(): List<Pt> {
        val pts = points
        if (pts != null && pts.size >= 3) return absPoints()
        val b = box
        return listOf(Pt(b.centerX, b.top), Pt(b.left, b.bottom), Pt(b.right, b.bottom))
    }

    /** Interactive control points (handles) in content space. */
    fun controlPoints(): List<Pt> = when (shape) {
        ShapeKind.LINE, ShapeKind.ARROW -> listOf(start, end)
        ShapeKind.TRIANGLE -> triangleVertices()
        ShapeKind.RECTANGLE -> {
            val b = box
            listOf(
                b.topLeft,
                Pt(b.centerX, b.top),
                Pt(b.right, b.top),
                Pt(b.right, b.centerY),
                Pt(b.right, b.bottom),
                Pt(b.centerX, b.bottom),
                Pt(b.left, b.bottom),
                Pt(b.left, b.centerY),
            )
        }
        ShapeKind.ELLIPSE, ShapeKind.CIRCLE -> {
            val pts = points
            if (pts != null && pts.size == 4) {
                absControlPoints()
            } else {
                val b = box
                listOf(
                    Pt(b.centerX, b.top),
                    Pt(b.right, b.centerY),
                    Pt(b.centerX, b.bottom),
                    Pt(b.left, b.centerY),
                )
            }
        }
        ShapeKind.CURVE -> {
            val bez = toBezierPoints(absControlPoints())
            val numKnots = (bez.size - 1) / 3 + 1
            (0 until numKnots).map { bez[3 * it] }
        }
        ShapeKind.POLYLINE, ShapeKind.POLYGON -> {
            val pts = absControlPoints()
            if (pts.size > 8) com.pnotes.core.stroke.KeyPointExtractor.extractKeyPoints(pts, maxPoints = 8)
            else pts
        }
    }

    /** Move/stretch an individual vertex or control point by index, updating shape geometry. */
    fun moveVertex(index: Int, newPos: Pt) {
        when (shape) {
            ShapeKind.LINE, ShapeKind.ARROW -> {
                if (index == 0) start = newPos
                else if (index == 1) end = newPos
            }
            ShapeKind.TRIANGLE -> {
                val cur = triangleVertices().toMutableList()
                if (index in cur.indices) {
                    cur[index] = newPos
                    val minX = cur.minOf { it.x }
                    val minY = cur.minOf { it.y }
                    val maxX = cur.maxOf { it.x }
                    val maxY = cur.maxOf { it.y }
                    start = Pt(minX, minY)
                    end = Pt(maxX, maxY)
                    val bw = max(1e-4, maxX - minX)
                    val bh = max(1e-4, maxY - minY)
                    points = cur.map { Pt((it.x - minX) / bw, (it.y - minY) / bh) }
                }
            }
            ShapeKind.CURVE -> {
                val bez = toBezierPoints(absControlPoints()).toMutableList()
                val numKnots = (bez.size - 1) / 3 + 1
                if (index in 0 until numKnots) {
                    val kIdx = 3 * index
                    val delta = newPos - bez[kIdx]
                    bez[kIdx] = newPos
                    if (index > 0) bez[kIdx - 1] = bez[kIdx - 1] + delta
                    if (index < numKnots - 1) bez[kIdx + 1] = bez[kIdx + 1] + delta

                    val minX = bez.minOf { it.x }
                    val minY = bez.minOf { it.y }
                    val maxX = bez.maxOf { it.x }
                    val maxY = bez.maxOf { it.y }
                    start = Pt(minX, minY)
                    end = Pt(maxX, maxY)
                    val bw = max(1e-4, maxX - minX)
                    val bh = max(1e-4, maxY - minY)
                    points = bez.map { Pt((it.x - minX) / bw, (it.y - minY) / bh) }
                }
            }
            ShapeKind.POLYGON, ShapeKind.POLYLINE -> {
                val cur = absControlPoints().toMutableList()
                if (index in cur.indices) {
                    cur[index] = newPos
                    val minX = cur.minOf { it.x }
                    val minY = cur.minOf { it.y }
                    val maxX = cur.maxOf { it.x }
                    val maxY = cur.maxOf { it.y }
                    start = Pt(minX, minY)
                    end = Pt(maxX, maxY)
                    val bw = max(1e-4, maxX - minX)
                    val bh = max(1e-4, maxY - minY)
                    points = cur.map { Pt((it.x - minX) / bw, (it.y - minY) / bh) }
                }
            }
            ShapeKind.RECTANGLE -> {
                val b = box
                when (index) {
                    0 -> { start = newPos; end = Pt(max(newPos.x + 10.0, b.right), max(newPos.y + 10.0, b.bottom)) }
                    1 -> { start = Pt(b.left, newPos.y) }
                    2 -> { start = Pt(b.left, newPos.y); end = Pt(newPos.x, b.bottom) }
                    3 -> { end = Pt(newPos.x, b.bottom) }
                    4 -> { end = newPos }
                    5 -> { end = Pt(b.right, newPos.y) }
                    6 -> { start = Pt(newPos.x, b.top); end = Pt(b.right, newPos.y) }
                    7 -> { start = Pt(newPos.x, b.top) }
                }
            }
            ShapeKind.ELLIPSE, ShapeKind.CIRCLE -> {
                val pts = points
                if (pts != null) {
                    val cur = absControlPoints().toMutableList()
                    if (index in cur.indices) {
                        cur[index] = newPos
                        val minX = cur.minOf { it.x }
                        val minY = cur.minOf { it.y }
                        val maxX = cur.maxOf { it.x }
                        val maxY = cur.maxOf { it.y }
                        start = Pt(minX, minY)
                        end = Pt(maxX, maxY)
                        val bw = max(1e-4, maxX - minX)
                        val bh = max(1e-4, maxY - minY)
                        points = cur.map { Pt((it.x - minX) / bw, (it.y - minY) / bh) }
                    }
                } else {
                    val b = box
                    when (index) {
                        0 -> start = Pt(b.left, newPos.y)
                        1 -> end = Pt(newPos.x, b.bottom)
                        2 -> end = Pt(b.right, newPos.y)
                        3 -> start = Pt(newPos.x, b.top)
                    }
                }
            }
        }
    }

    /** Open ">" arrowhead as the chevron polyline barbLeft -> tip -> barbRight (stroked, not filled),
     *  sized from the stroke width. The tip sits just past [end] so the point clears the shaft's
     *  round cap and reads slightly forward instead of buried under the line end. */
    internal fun arrowHead(): List<Pt> {
        val dir = (end - start).normalized()
        if (dir.length() < 1e-9) return emptyList()
        val headLen = max(12.0, strokeWidth * 3.5)
        val tip = end + dir * (strokeWidth * 0.5)
        val back = tip - dir * headLen
        val perp = dir.perp() * (headLen * 0.5)
        return listOf(back + perp, tip, back - perp)
    }

    internal fun ellipsePolygon(segments: Int = 48): List<Pt> {
        val b = box
        val cx = b.centerX
        val cy = b.centerY
        val rx = b.w / 2.0
        val ry = b.h / 2.0
        return (0 until segments).map {
            val a = 2.0 * PI * it / segments
            Pt(cx + rx * cos(a), cy + ry * sin(a))
        }
    }

    override fun paint(r: Renderer) {
        if (neon) return paintNeon(r)
        fillRgba?.let { drawFill(r, it) }
        drawOutline(r, pen())
        if (shape == ShapeKind.ARROW) drawArrowHead(r, pen())
    }

    /** Fill the closed-shape interior (no-op for open line/arrow). */
    private fun drawFill(r: Renderer, fill: Rgba) {
        val b = box
        when (shape) {
            ShapeKind.RECTANGLE -> r.fillRect(b, fill)
            ShapeKind.ELLIPSE, ShapeKind.CIRCLE -> r.fillEllipse(b.center, b.w / 2.0, b.h / 2.0, fill)
            ShapeKind.TRIANGLE -> r.fillPolygon(triangleVertices(), fill)
            ShapeKind.POLYGON -> r.fillPolygon(absPoints(), fill)
            ShapeKind.LINE, ShapeKind.ARROW, ShapeKind.POLYLINE, ShapeKind.CURVE -> {}
        }
    }

    /** Stroke the shape's outline (the arrow's shaft for arrows) with [pen]. */
    private fun drawOutline(r: Renderer, pen: Pen) {
        val b = box
        when (shape) {
            ShapeKind.LINE, ShapeKind.ARROW -> r.strokePolyline(listOf(start, end), pen)
            ShapeKind.RECTANGLE -> r.strokeRect(b, pen)
            ShapeKind.ELLIPSE, ShapeKind.CIRCLE -> r.strokeEllipse(b.center, b.w / 2.0, b.h / 2.0, pen)
            ShapeKind.TRIANGLE -> r.strokePolygon(triangleVertices(), pen)
            ShapeKind.POLYGON -> r.strokePolygon(absPoints(), pen)
            ShapeKind.POLYLINE, ShapeKind.CURVE -> r.strokePolyline(absPoints(), pen)
        }
    }

    /** Stroke the open ">" arrowhead with [pen], mirroring the shaft so the head and shaft share one tube.
     *  The chevron is always solid: a dash break on the short barbs would maim the point. */
    private fun drawArrowHead(r: Renderer, pen: Pen) {
        val head = arrowHead()
        if (head.size == 3) r.strokePolyline(head, pen.copy(dashed = false))
    }

    /**
     * Neon: the outline as a glowing glass tube — a blurred ink-colour **halo**
     * (composited once at a glow-intensity alpha so corners don't blow out), the
     * opaque colour **body**, then a thinner **white-hot core** down the centre so
     * the colour reads at the tube's edges. [neonStrength] scales the halo only.
     */
    internal fun neonGlowRadius(): Double {
        val s = neonStrength.coerceIn(0.0, 1.0)
        return (strokeWidth * (GLOW_FACTOR_MIN + GLOW_FACTOR_SPAN * s)).coerceAtLeast(GLOW_MIN)
    }

    override fun paintBounds(): Rect =
        if (neon) bounds().outset(neonGlowRadius() * 2.0 + 4.0)
        else bounds()

    private fun paintNeon(r: Renderer) {
        val glowR = neonGlowRadius()
        val s = neonStrength.coerceIn(0.0, 1.0)
        val color = strokeRgba.withAlpha(255)
        val white = Rgba(255, 255, 255, 255)
        val glowAlpha = GLOW_ALPHA_MIN + GLOW_ALPHA_SPAN * s
        val coreW = (strokeWidth * CORE_WIDTH_FRAC).coerceAtLeast(CORE_WIDTH_MIN)

        fillRgba?.let { drawFill(r, it) }

        // 1) Outer halo (ink colour), bounded in its own glow-alpha layer.
        r.saveLayerAlpha(paintBounds(), glowAlpha)
        val haloPen = pen().copy(color = color, glowRadius = glowR)
        drawOutline(r, haloPen)
        if (shape == ShapeKind.ARROW) drawArrowHead(r, haloPen)
        r.restore()

        // 2) Tube body — saturated colour shows at the rim.
        drawOutline(r, pen())
        if (shape == ShapeKind.ARROW) drawArrowHead(r, pen())

        // 3) White-hot core — a thinner white line down the centre of the shaft and the chevron.
        val corePen = pen().copy(color = white, width = coreW)
        drawOutline(r, corePen)
        if (shape == ShapeKind.ARROW) drawArrowHead(r, corePen)
    }

    override fun bounds(): Rect {
        val pad = strokeWidth / 2.0 + 1.0
        return when (shape) {
            ShapeKind.LINE -> Rect.fromPoints(start, end).outset(pad)
            ShapeKind.ARROW -> Rect.bounding(listOf(start, end) + arrowHead()).outset(pad)
            ShapeKind.CURVE, ShapeKind.POLYGON, ShapeKind.POLYLINE -> {
                val pts = absPoints()
                if (pts.isNotEmpty()) Rect.bounding(pts).outset(pad)
                else box.outset(pad)
            }
            ShapeKind.TRIANGLE -> Rect.bounding(triangleVertices()).outset(pad)
            else -> box.outset(pad)
        }
    }

    override fun translate(dx: Double, dy: Double) {
        start = Pt(start.x + dx, start.y + dy)
        end = Pt(end.x + dx, end.y + dy)
    }

    override fun contains(p: Pt): Boolean {
        val tol = max(strokeWidth / 2.0, HIT_TOLERANCE)
        return when (shape) {
            ShapeKind.LINE, ShapeKind.ARROW -> Geometry.distancePointToSegment(p, start, end) <= tol
            ShapeKind.RECTANGLE -> if (fillRgba != null) box.contains(p) else nearRectOutline(p, tol)
            ShapeKind.TRIANGLE -> {
                val v = triangleVertices()
                if (fillRgba != null) Geometry.pointInPolygon(v, p) else nearPolyOutline(v, p, tol)
            }
            ShapeKind.ELLIPSE, ShapeKind.CIRCLE -> {
                val poly = ellipsePolygon()
                if (fillRgba != null) Geometry.pointInPolygon(poly, p) else nearPolyOutline(poly, p, tol)
            }
            ShapeKind.POLYGON -> {
                val v = absPoints()
                if (fillRgba != null) Geometry.pointInPolygon(v, p) else nearPolyOutline(v, p, tol)
            }
            ShapeKind.POLYLINE, ShapeKind.CURVE -> nearPolyOutline(absPoints(), p, tol, closed = false)
        }
    }

    private fun nearRectOutline(p: Pt, tol: Double): Boolean {
        val b = box
        val corners = listOf(
            Pt(b.left, b.top), Pt(b.right, b.top), Pt(b.right, b.bottom), Pt(b.left, b.bottom),
        )
        return nearPolyOutline(corners, p, tol)
    }

    private fun nearPolyOutline(verts: List<Pt>, p: Pt, tol: Double, closed: Boolean = true): Boolean {
        if (verts.isEmpty()) return false
        val edges = if (closed) verts.size else verts.size - 1
        for (i in 0 until edges) {
            val a = verts[i]
            val b = verts[(i + 1) % verts.size]
            if (Geometry.distancePointToSegment(p, a, b) <= tol) return true
        }
        return false
    }

    override fun centroid(): Pt = bounds().center

    /** True if an eraser circle of [radius] at (cx,cy) touches the shape's geometry. */
    override fun intersectsCircle(cx: Double, cy: Double, radius: Double): Boolean {
        val p = Pt(cx, cy)
        if (bounds().distanceTo(p) > radius) return false // cheap AABB reject
        val tol = radius + strokeWidth / 2.0
        return when (shape) {
            ShapeKind.LINE, ShapeKind.ARROW -> Geometry.distancePointToSegment(p, start, end) <= tol
            ShapeKind.RECTANGLE ->
                if (fillRgba != null && box.contains(p)) true else nearRectOutline(p, tol)
            ShapeKind.TRIANGLE -> {
                val v = triangleVertices()
                if (fillRgba != null && Geometry.pointInPolygon(v, p)) true else nearPolyOutline(v, p, tol)
            }
            ShapeKind.ELLIPSE, ShapeKind.CIRCLE -> {
                val poly = ellipsePolygon()
                if (fillRgba != null && Geometry.pointInPolygon(poly, p)) true else nearPolyOutline(poly, p, tol)
            }
            ShapeKind.POLYGON -> {
                val v = absPoints()
                if (fillRgba != null && Geometry.pointInPolygon(v, p)) true else nearPolyOutline(v, p, tol)
            }
            ShapeKind.POLYLINE, ShapeKind.CURVE -> nearPolyOutline(absPoints(), p, tol, closed = false)
        }
    }

    /**
     * AREA-erase: the outline parts that survive an eraser circle (page-local [cx], [cy],
     * [radius]), cut cleanly at the circle boundary — the same contract as [Stroke.erasedBy]:
     *  - `null`      — untouched (keep the original)
     *  - empty list  — remove the whole shape
     *  - fragments   — the surviving outline runs, as polyline shapes keeping this style
     * A filled shape erases whole on any hit: a cut outline cannot hold its fill.
     */
    fun erasedBy(cx: Double, cy: Double, radius: Double): List<ShapeItem>? {
        val c = Pt(cx, cy)
        if (bounds().distanceTo(c) > radius) return null
        if (fillRgba != null) return if (intersectsCircle(cx, cy, radius)) emptyList() else null
        val verts = when (shape) {
            // The arrow clips by its shaft, like its hit tests; a cut arrow loses its head.
            ShapeKind.LINE, ShapeKind.ARROW -> listOf(start, end)
            else -> currentOutline()
        }
        if (verts.size < 2) return if (intersectsCircle(cx, cy, radius)) emptyList() else null
        val closed = shape.isClosed
        val pts = if (closed) verts + verts.first() else verts

        var touched = false
        val runs = mutableListOf<MutableList<Pt>>()
        var current = mutableListOf<Pt>()
        fun flush() {
            if (current.size >= 2 && polylineLength(current) > 1e-6) runs.add(current)
            current = mutableListOf()
        }
        for (i in 0 until pts.size - 1) {
            val a = pts[i]
            val b = pts[i + 1]
            val outside = outsideSpans(a, b, c, radius)
            if (outside.size == 1 && outside[0].first == 0.0 && outside[0].second == 1.0) {
                if (current.isEmpty()) current.add(a)
                current.add(b)
                continue
            }
            touched = true
            if (outside.isEmpty()) { // the whole segment sits inside the circle
                flush()
                continue
            }
            for ((ta, tb) in outside) {
                if (ta == 0.0) {
                    if (current.isEmpty()) current.add(a)
                } else {
                    flush()
                    current.add(lerp(a, b, ta))
                }
                current.add(if (tb == 1.0) b else lerp(a, b, tb))
                if (tb < 1.0) flush()
            }
        }
        flush()
        if (!touched) return null

        // A closed outline cut away from the seam vertex leaves the seam intact in two runs;
        // join them so the survivor is one continuous polyline instead of splitting at v0.
        if (closed && runs.size >= 2) {
            val first = runs.first()
            val last = runs.last()
            if (first.first().distanceTo(pts.first()) < 1e-9 && last.last().distanceTo(pts.first()) < 1e-9) {
                runs.removeAt(runs.size - 1)
                runs[0] = (last + first.drop(1)).toMutableList()
            }
        }
        return runs.map {
            poly(
                ShapeKind.POLYLINE, it, strokeRgba, strokeWidth, null, neon, neonStrength,
                dashed, dashLength, dashGap,
            )
        }
    }

    /** Sub-intervals of the segment [a]→[b] (as fractions) lying outside the circle at [c]. */
    private fun outsideSpans(a: Pt, b: Pt, c: Pt, r: Double): List<Pair<Double, Double>> {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val fx = a.x - c.x
        val fy = a.y - c.y
        val qa = dx * dx + dy * dy
        if (qa < 1e-12) return if (fx * fx + fy * fy > r * r) listOf(0.0 to 1.0) else emptyList()
        val qb = 2.0 * (fx * dx + fy * dy)
        val qc = fx * fx + fy * fy - r * r
        val disc = qb * qb - 4.0 * qa * qc
        if (disc <= 0.0) return listOf(0.0 to 1.0) // misses (or grazes) the segment's line
        val sq = sqrt(disc)
        val t1 = (-qb - sq) / (2.0 * qa)
        val t2 = (-qb + sq) / (2.0 * qa)
        if (t2 <= 0.0 || t1 >= 1.0) return listOf(0.0 to 1.0) // chord lies beyond the segment span
        val spans = mutableListOf<Pair<Double, Double>>()
        if (t1 > 0.0) spans.add(0.0 to min(t1, 1.0))
        if (t2 < 1.0) spans.add(max(t2, 0.0) to 1.0)
        return spans
    }

    private fun lerp(a: Pt, b: Pt, t: Double) = Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

    private fun polylineLength(pts: List<Pt>): Double {
        var len = 0.0
        for (i in 0 until pts.size - 1) len += pts[i].distanceTo(pts[i + 1])
        return len
    }

    override fun geometry(): GeoHandle = ShapeHandle(start, end, points)

    override fun setGeometry(handle: GeoHandle) {
        if (handle is ShapeHandle) {
            start = handle.start
            end = handle.end
            points = handle.points
        }
    }

    override fun snapshotGeometry(): GeometrySnapshot = ShapeSnapshot(shape, start, end, points, strokeWidth)

    override fun restoreGeometry(snap: GeometrySnapshot) {
        if (snap !is ShapeSnapshot) return
        shape = snap.shape
        start = snap.start
        end = snap.end
        points = snap.points
        strokeWidth = snap.strokeWidth
    }

    /**
     * Bake a transform into the shape. A line/arrow keeps its kind (only its two endpoints move).
     * A pure scale keeps every kind parametric (the box scales; normalized vertices follow). A
     * rotation can't be held by an axis-aligned box, so a rotated rectangle/ellipse/triangle is
     * converted to a [ShapeKind.POLYGON] (closed) or [ShapeKind.POLYLINE] (open) of baked
     * vertices. The outline width scales by the transform's linear factor (1.0 for a rotation).
     */
    override fun applyTransform(t: Affine) {
        strokeWidth *= t.linearScale
        if (shape.isEndpointShape || t.isAxisAligned) {
            start = t.apply(start)
            end = t.apply(end)
            if (points != null) {
                val raw = absControlPoints()
                val rotated = raw.map { t.apply(it) }
                val bb = Rect.bounding(rotated)
                start = bb.topLeft
                end = Pt(bb.right, bb.bottom)
                points = normalize(rotated, bb)
            }
            return
        }
        if (shape == ShapeKind.CIRCLE || shape == ShapeKind.ELLIPSE) {
            val poly = ellipsePolygon()
            val rotated = poly.map { t.apply(it) }
            val bb = Rect.bounding(rotated)
            shape = ShapeKind.POLYGON
            start = bb.topLeft
            end = Pt(bb.right, bb.bottom)
            points = normalize(rotated, bb)
            return
        }
        if (shape == ShapeKind.RECTANGLE) {
            val b = box
            val corners = listOf(b.topLeft, Pt(b.right, b.top), Pt(b.right, b.bottom), Pt(b.left, b.bottom))
            val cur = if (points != null && points!!.size == 4) absControlPoints() else corners
            val rotatedCorners = cur.map { t.apply(it) }
            val bb = Rect.bounding(rotatedCorners)
            start = bb.topLeft
            end = Pt(bb.right, bb.bottom)
            shape = ShapeKind.POLYGON
            points = normalize(rotatedCorners, bb)
            return
        }
        if (shape == ShapeKind.TRIANGLE) {
            val rotatedVerts = triangleVertices().map { t.apply(it) }
            val bb = Rect.bounding(rotatedVerts)
            shape = ShapeKind.POLYGON
            start = bb.topLeft
            end = Pt(bb.right, bb.bottom)
            points = normalize(rotatedVerts, bb)
            return
        }
        if (shape == ShapeKind.CURVE) {
            val bez = toBezierPoints(absControlPoints())
            val rotated = bez.map { t.apply(it) }
            val bb = Rect.bounding(rotated)
            start = bb.topLeft
            end = Pt(bb.right, bb.bottom)
            points = normalize(rotated, bb)
            return
        }
        val verts = currentOutline().map { t.apply(it) }
        val bb = Rect.bounding(verts)
        shape = if (shape.isClosed) ShapeKind.POLYGON else ShapeKind.POLYLINE
        start = bb.topLeft
        end = Pt(bb.right, bb.bottom)
        points = normalize(verts, bb)
    }

    /** Content-space vertices of the current outline; used to bake a rotation into a vertex list. */
    private fun currentOutline(): List<Pt> = when (shape) {
        ShapeKind.POLYGON, ShapeKind.POLYLINE, ShapeKind.CURVE -> absPoints()
        ShapeKind.TRIANGLE -> triangleVertices()
        ShapeKind.ELLIPSE, ShapeKind.CIRCLE -> ellipsePolygon()
        else -> {
            val b = box
            listOf(Pt(b.left, b.top), Pt(b.right, b.top), Pt(b.right, b.bottom), Pt(b.left, b.bottom))
        }
    }

    companion object {
        const val KIND = "shape"
        const val HIT_TOLERANCE = 6.0

        /** Build a polygon/polyline from absolute [vertices], stored normalized to their box. */
        fun poly(
            shape: ShapeKind,
            vertices: List<Pt>,
            strokeRgba: Rgba,
            strokeWidth: Double = 3.0,
            fillRgba: Rgba? = null,
            neon: Boolean = false,
            neonStrength: Double = 0.6,
            dashed: Boolean = false,
            dashLength: Double = 10.0,
            dashGap: Double = 8.0,
        ): ShapeItem {
            val box = Rect.bounding(vertices)
            return ShapeItem(
                shape, box.topLeft, Pt(box.right, box.bottom), strokeRgba, strokeWidth,
                fillRgba, neon, neonStrength, normalize(vertices, box), dashed, dashLength, dashGap,
            )
        }

        fun generatePolylinePoints(start: Pt, end: Pt): List<Pt> {
            val dx = end.x - start.x
            val dy = end.y - start.y
            return if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                val midX = (start.x + end.x) / 2.0
                listOf(start, Pt(midX, start.y), Pt(midX, end.y), end)
            } else {
                val midY = (start.y + end.y) / 2.0
                listOf(start, Pt(start.x, midY), Pt(end.x, midY), end)
            }
        }

        fun generateCurvePoints(start: Pt, end: Pt): List<Pt> {
            val dx = end.x - start.x
            val dy = end.y - start.y
            val (p1, p2) = if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                val midX = (start.x + end.x) / 2.0
                Pt(midX, start.y) to Pt(midX, end.y)
            } else {
                val midY = (start.y + end.y) / 2.0
                Pt(start.x, midY) to Pt(end.x, midY)
            }
            return listOf(start, p1, p2, end)
        }

        /** Converts any list of points into a valid cubic Bézier control point sequence (3*(K-1) + 1 points). */
        fun toBezierPoints(pts: List<Pt>): List<Pt> {
            if (pts.size >= 4 && (pts.size - 1) % 3 == 0) return pts
            if (pts.size <= 2) {
                val p0 = pts.firstOrNull() ?: Pt(0.0, 0.0)
                val p1 = pts.lastOrNull() ?: Pt(10.0, 10.0)
                return generateCurvePoints(p0, p1)
            }
            val k = pts.size
            val tangents = (0 until k).map { i ->
                when (i) {
                    0 -> pts[1] - pts[0]
                    k - 1 -> pts[k - 1] - pts[k - 2]
                    else -> (pts[i + 1] - pts[i - 1]) * 0.5
                }
            }
            val bez = ArrayList<Pt>(3 * (k - 1) + 1)
            bez.add(pts[0])
            for (i in 0 until k - 1) {
                bez.add(pts[i] + tangents[i] / 3.0)
                bez.add(pts[i + 1] - tangents[i + 1] / 3.0)
                bez.add(pts[i + 1])
            }
            return bez
        }

        fun interpolateCatmullRom(controlPoints: List<Pt>, samplesPerSegment: Int = 20): List<Pt> {
            if (controlPoints.size < 3) return controlPoints
            val n = controlPoints.size
            val result = ArrayList<Pt>((n - 1) * samplesPerSegment + 1)

            // Natural curvature extrapolation: treats endpoints as part of a smooth continuous curve
            val pExt = ArrayList<Pt>(n + 2)
            val p0Ext = controlPoints[0] * 3.0 - controlPoints[1] * 3.0 + controlPoints[2]
            pExt.add(p0Ext)
            pExt.addAll(controlPoints)
            val pnExt = controlPoints[n - 1] * 3.0 - controlPoints[n - 2] * 3.0 + controlPoints[n - 3]
            pExt.add(pnExt)

            for (i in 0 until n - 1) {
                val p0 = pExt[i]
                val p1 = pExt[i + 1]
                val p2 = pExt[i + 2]
                val p3 = pExt[i + 3]

                for (s in 0 until samplesPerSegment) {
                    val t = s.toDouble() / samplesPerSegment
                    val t2 = t * t
                    val t3 = t2 * t
                    val x = 0.5 * (
                        (2.0 * p1.x) +
                        (-p0.x + p2.x) * t +
                        (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2 +
                        (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * t3
                    )
                    val y = 0.5 * (
                        (2.0 * p1.y) +
                        (-p0.y + p2.y) * t +
                        (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2 +
                        (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * t3
                    )
                    result.add(Pt(x, y))
                }
            }
            result.add(controlPoints.last())
            return result
        }

        internal fun normalize(vertices: List<Pt>, box: Rect): List<Pt> {
            val w = if (box.w > 1e-9) box.w else 1.0
            val h = if (box.h > 1e-9) box.h else 1.0
            return vertices.map { Pt((it.x - box.left) / w, (it.y - box.top) / h) }
        }

        /** Halo blur = stroke_width × (MIN + SPAN × neonStrength), floored in page px. */
        private const val GLOW_FACTOR_MIN = 1.2
        private const val GLOW_FACTOR_SPAN = 2.6
        private const val GLOW_MIN = 4.0

        /** Halo opacity = MIN + SPAN × neonStrength. */
        internal const val GLOW_ALPHA_MIN = 0.25
        internal const val GLOW_ALPHA_SPAN = 0.55

        /** White-hot core line width as a fraction of the stroke width, with a page-px floor. */
        internal const val CORE_WIDTH_FRAC = 0.4
        internal const val CORE_WIDTH_MIN = 1.0
    }
}

/** Snapshot of a shape's transformable geometry (kind can change when a box shape is rotated). */
private data class ShapeSnapshot(
    val shape: ShapeKind,
    val start: Pt,
    val end: Pt,
    val points: List<Pt>?,
    val strokeWidth: Double,
) : GeometrySnapshot
