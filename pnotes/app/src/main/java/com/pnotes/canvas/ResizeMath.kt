package com.pnotes.canvas

import com.pnotes.core.geometry.Affine
import com.pnotes.core.geometry.Obb
import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.model.CanvasItem
import com.pnotes.core.model.ImageItem
import com.pnotes.core.model.ShapeItem
import com.pnotes.core.model.TableItem
import com.pnotes.core.model.TextItem
import kotlin.math.abs
import kotlin.math.max

/** Which resize handle is being dragged (spec 06 §8). T/B are the top/bottom side mid-handles. */
enum class HandleId {
    TL, TR, BL, BR, L, R, T, B, START, END,
    V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15,
    TAN_L0, TAN_R0, TAN_L1, TAN_R1, TAN_L2, TAN_R2, TAN_L3, TAN_R3,
    TAN_L4, TAN_R4, TAN_L5, TAN_R5, TAN_L6, TAN_R6, TAN_L7, TAN_R7,
}

fun HandleId.vertexIndex(): Int? = when (this) {
    HandleId.START, HandleId.V0 -> 0
    HandleId.END, HandleId.V1 -> 1
    HandleId.V2 -> 2
    HandleId.V3 -> 3
    HandleId.V4 -> 4
    HandleId.V5 -> 5
    HandleId.V6 -> 6
    HandleId.V7 -> 7
    HandleId.V8 -> 8
    HandleId.V9 -> 9
    HandleId.V10 -> 10
    HandleId.V11 -> 11
    HandleId.V12 -> 12
    HandleId.V13 -> 13
    HandleId.V14 -> 14
    HandleId.V15 -> 15
    else -> null
}

fun HandleId.tangentInfo(): Pair<Int, Boolean>? = when (this) {
    HandleId.TAN_L0 -> 0 to false
    HandleId.TAN_R0 -> 0 to true
    HandleId.TAN_L1 -> 1 to false
    HandleId.TAN_R1 -> 1 to true
    HandleId.TAN_L2 -> 2 to false
    HandleId.TAN_R2 -> 2 to true
    HandleId.TAN_L3 -> 3 to false
    HandleId.TAN_R3 -> 3 to true
    HandleId.TAN_L4 -> 4 to false
    HandleId.TAN_R4 -> 4 to true
    HandleId.TAN_L5 -> 5 to false
    HandleId.TAN_R5 -> 5 to true
    HandleId.TAN_L6 -> 6 to false
    HandleId.TAN_R6 -> 6 to true
    HandleId.TAN_L7 -> 7 to false
    HandleId.TAN_R7 -> 7 to true
    else -> null
}

data class ResizeHandle(val id: HandleId, val content: Pt)

/** Pure resize-handle geometry and per-type resize updates (spec 06 §8). */
object ResizeMath {
    const val MIN_SIZE = 24.0

    /** Lowest scale a generic box-handle drag yields, so a selection can't collapse to zero or
     *  flip. A ratio floor (vs the absolute [MIN_SIZE]) keeps thin selections from exploding. */
    const val MIN_SCALE = 0.05

    /** Handle positions in content space for [item] on a page at [pageTopLeft]. */
    fun handles(item: CanvasItem, pageTopLeft: Pt): List<ResizeHandle> = when (item) {
        is ImageItem -> rectCorners(item.rect, pageTopLeft)
        is TextItem -> {
            val b = item.bounds()
            val l = b.left + pageTopLeft.x
            val r = b.right + pageTopLeft.x
            val t = b.top + pageTopLeft.y
            val bot = b.bottom + pageTopLeft.y
            val cx = b.centerX + pageTopLeft.x
            val cy = b.centerY + pageTopLeft.y
            // Eight handles: four corners + four side midpoints.
            listOf(
                ResizeHandle(HandleId.TL, Pt(l, t)),
                ResizeHandle(HandleId.T, Pt(cx, t)),
                ResizeHandle(HandleId.TR, Pt(r, t)),
                ResizeHandle(HandleId.R, Pt(r, cy)),
                ResizeHandle(HandleId.BR, Pt(r, bot)),
                ResizeHandle(HandleId.B, Pt(cx, bot)),
                ResizeHandle(HandleId.BL, Pt(l, bot)),
                ResizeHandle(HandleId.L, Pt(l, cy)),
            )
        }
        is ShapeItem -> {
            val pts = item.controlPoints()
            pts.mapIndexed { idx, p ->
                val id = when (idx) {
                    0 -> if (item.shape.isEndpointShape) HandleId.START else HandleId.V0
                    1 -> if (item.shape.isEndpointShape) HandleId.END else HandleId.V1
                    2 -> HandleId.V2
                    3 -> HandleId.V3
                    4 -> HandleId.V4
                    5 -> HandleId.V5
                    6 -> HandleId.V6
                    7 -> HandleId.V7
                    8 -> HandleId.V8
                    9 -> HandleId.V9
                    10 -> HandleId.V10
                    11 -> HandleId.V11
                    12 -> HandleId.V12
                    13 -> HandleId.V13
                    14 -> HandleId.V14
                    else -> HandleId.V15
                }
                ResizeHandle(id, Pt(p.x + pageTopLeft.x, p.y + pageTopLeft.y))
            }
        }
        is TableItem -> {
            val b = item.bounds()
            val l = b.left + pageTopLeft.x
            val r = b.right + pageTopLeft.x
            val t = b.top + pageTopLeft.y
            val bot = b.bottom + pageTopLeft.y
            listOf(
                ResizeHandle(HandleId.TL, Pt(l, t)),
                ResizeHandle(HandleId.TR, Pt(r, t)),
                ResizeHandle(HandleId.BL, Pt(l, bot)),
                ResizeHandle(HandleId.BR, Pt(r, bot)),
            )
        }
        else -> emptyList()
    }

    private fun rectCorners(rect: Rect, ptl: Pt) = listOf(
        ResizeHandle(HandleId.TL, Pt(rect.left + ptl.x, rect.top + ptl.y)),
        ResizeHandle(HandleId.TR, Pt(rect.right + ptl.x, rect.top + ptl.y)),
        ResizeHandle(HandleId.BL, Pt(rect.left + ptl.x, rect.bottom + ptl.y)),
        ResizeHandle(HandleId.BR, Pt(rect.right + ptl.x, rect.bottom + ptl.y)),
    )

    fun hitHandle(handles: List<ResizeHandle>, content: Pt, tolerance: Double): HandleId? =
        handles.firstOrNull { it.content.distanceTo(content) <= tolerance }?.id

    /** The eight generic box handles (four corners + four edge midpoints) for [box], in box's
     *  own coordinate space. Used by the unified resize handles for any multi/mixed selection. */
    fun boxHandles(box: Rect): List<ResizeHandle> = listOf(
        ResizeHandle(HandleId.TL, box.topLeft),
        ResizeHandle(HandleId.T, Pt(box.centerX, box.top)),
        ResizeHandle(HandleId.TR, Pt(box.right, box.top)),
        ResizeHandle(HandleId.R, Pt(box.right, box.centerY)),
        ResizeHandle(HandleId.BR, Pt(box.right, box.bottom)),
        ResizeHandle(HandleId.B, Pt(box.centerX, box.bottom)),
        ResizeHandle(HandleId.BL, Pt(box.left, box.bottom)),
        ResizeHandle(HandleId.L, Pt(box.left, box.centerY)),
    )

    /** A scale about [anchor] by ([sx], [sy]) — the transform a box-handle drag maps to. */
    data class ScaleSpec(val anchor: Pt, val sx: Double, val sy: Double)

    /** The eight handles of an oriented box, in world space. */
    fun obbHandles(obb: Obb): List<ResizeHandle> = listOf(
        ResizeHandle(HandleId.TL, obb.localToWorld(Pt(-obb.halfW, -obb.halfH))),
        ResizeHandle(HandleId.T, obb.localToWorld(Pt(0.0, -obb.halfH))),
        ResizeHandle(HandleId.TR, obb.localToWorld(Pt(obb.halfW, -obb.halfH))),
        ResizeHandle(HandleId.R, obb.localToWorld(Pt(obb.halfW, 0.0))),
        ResizeHandle(HandleId.BR, obb.localToWorld(Pt(obb.halfW, obb.halfH))),
        ResizeHandle(HandleId.B, obb.localToWorld(Pt(0.0, obb.halfH))),
        ResizeHandle(HandleId.BL, obb.localToWorld(Pt(-obb.halfW, obb.halfH))),
        ResizeHandle(HandleId.L, obb.localToWorld(Pt(-obb.halfW, 0.0))),
    )

    /** Four corner handles for an oriented box in world space (TL, TR, BR, BL).
     *  Prevents edge-midpoint handles from intercepting move drags on horizontal text or narrow strokes. */
    fun obbCornerHandles(obb: Obb): List<ResizeHandle> = listOf(
        ResizeHandle(HandleId.TL, obb.localToWorld(Pt(-obb.halfW, -obb.halfH))),
        ResizeHandle(HandleId.TR, obb.localToWorld(Pt(obb.halfW, -obb.halfH))),
        ResizeHandle(HandleId.BR, obb.localToWorld(Pt(obb.halfW, obb.halfH))),
        ResizeHandle(HandleId.BL, obb.localToWorld(Pt(-obb.halfW, obb.halfH))),
    )

    /** Top-edge midpoint of an oriented box in world space (the rotate stem's base). */
    fun obbTopMid(obb: Obb): Pt = obb.localToWorld(Pt(0.0, -obb.halfH))

    /** The rotate-grip centre, [arm] (content px) out past the box's top edge along its local up. */
    fun obbRotateGrip(obb: Obb, arm: Double): Pt = obb.localToWorld(Pt(0.0, -obb.halfH - arm))

    /** The affine to bake into the items, plus the resulting oriented box. */
    data class ObbResize(val transform: Affine, val obb: Obb)

    /**
     * Drag a box handle of an oriented box [obb] to world [pointer]: scale in the box's own frame
     * (corner uniform, edge single-axis) about the opposite handle, returning the world-space affine
     * to bake into the items and the new oriented box. The two stay consistent — applying the affine
     * to [obb]'s corners reproduces the returned box's corners.
     */
    fun obbResize(obb: Obb, handle: HandleId, pointer: Pt): ObbResize {
        val local = obb.worldToLocal(pointer)
        val localBox = Rect(-obb.halfW, -obb.halfH, obb.halfW * 2.0, obb.halfH * 2.0)
        val sc = scaleForHandle(localBox, handle, local)
        val transform = Affine.scaleAlongAxes(obb.localToWorld(sc.anchor), obb.angle, sc.sx, sc.sy)
        // The local box was centred at the origin; scaling about sc.anchor moves that centre.
        val newCenterLocal = Pt(sc.anchor.x * (1.0 - sc.sx), sc.anchor.y * (1.0 - sc.sy))
        val newObb = Obb(obb.localToWorld(newCenterLocal), obb.halfW * sc.sx, obb.halfH * sc.sy, obb.angle)
        return ObbResize(transform, newObb)
    }

    /**
     * The scale a box-handle drag produces: a corner scales both axes by one factor (aspect-locked,
     * anchored at the opposite corner); an edge scales a single axis (anchored at the opposite
     * edge). Each axis is floored positive so the box can't shrink past [MIN_SIZE] or flip.
     */
    fun scaleForHandle(box: Rect, handle: HandleId, pointer: Pt): ScaleSpec = when (handle) {
        HandleId.L, HandleId.R -> {
            val anchorX = if (handle == HandleId.L) box.right else box.left
            val grabX = if (handle == HandleId.L) box.left else box.right
            ScaleSpec(Pt(anchorX, box.top), axisScale(pointer.x, anchorX, grabX), 1.0)
        }
        HandleId.T, HandleId.B -> {
            val anchorY = if (handle == HandleId.T) box.bottom else box.top
            val grabY = if (handle == HandleId.T) box.top else box.bottom
            ScaleSpec(Pt(box.left, anchorY), 1.0, axisScale(pointer.y, anchorY, grabY))
        }
        else -> {
            // A corner is aspect-locked: project the pointer onto the box diagonal from the opposite
            // (anchored) corner, so the one factor follows the drag and shrinks as well as grows.
            val anchor = cornerAnchor(box, handle)
            val s = diagonalScale(anchor, grabbedCorner(box, handle), pointer)
            ScaleSpec(anchor, s, s)
        }
    }

    private fun grabbedCorner(box: Rect, handle: HandleId): Pt = when (handle) {
        HandleId.TL -> box.topLeft
        HandleId.TR -> Pt(box.right, box.top)
        HandleId.BL -> Pt(box.left, box.bottom)
        else -> Pt(box.right, box.bottom) // BR
    }

    /** Single-axis scale from [anchor] toward [grab], floored at [MIN_SCALE] so the box can't
     *  collapse or flip. A ratio floor (not an absolute one) keeps thin selections from exploding. */
    private fun axisScale(pointer: Double, anchor: Double, grab: Double): Double {
        val denom = grab - anchor
        if (abs(denom) < 1e-9) return 1.0
        return max((pointer - anchor) / denom, MIN_SCALE)
    }

    /** Uniform scale from projecting [pointer] onto the [anchor]->[grab] diagonal, floored at
     *  [MIN_SCALE]. */
    private fun diagonalScale(anchor: Pt, grab: Pt, pointer: Pt): Double {
        val dx = grab.x - anchor.x
        val dy = grab.y - anchor.y
        val denom = dx * dx + dy * dy
        if (denom < 1e-9) return 1.0
        return max(((pointer.x - anchor.x) * dx + (pointer.y - anchor.y) * dy) / denom, MIN_SCALE)
    }

    private fun cornerAnchor(box: Rect, handle: HandleId): Pt = when (handle) {
        HandleId.TL -> Pt(box.right, box.bottom)
        HandleId.TR -> Pt(box.left, box.bottom)
        HandleId.BL -> Pt(box.right, box.top)
        else -> Pt(box.left, box.top) // BR
    }

    /** Aspect-locked image resize (page-local pointer). */
    fun resizeImage(old: Rect, handle: HandleId, pointer: Pt): Rect {
        val anchor = cornerAnchor(old, handle)
        val rawW = abs(pointer.x - anchor.x)
        val rawH = abs(pointer.y - anchor.y)
        var scale = max(rawW / old.w, rawH / old.h)
        scale = max(scale, max(MIN_SIZE / old.w, MIN_SIZE / old.h))
        val newW = old.w * scale
        val newH = old.h * scale
        val left = if (handle == HandleId.TL || handle == HandleId.BL) anchor.x - newW else anchor.x
        val top = if (handle == HandleId.TL || handle == HandleId.TR) anchor.y - newH else anchor.y
        return Rect(left, top, newW, newH)
    }

    /** Free (per-axis) closed-shape resize; returns new (start, end). */
    fun resizeClosedShape(start: Pt, end: Pt, handle: HandleId, pointer: Pt): Pair<Pt, Pt> {
        val anchor = cornerAnchor(Rect.fromPoints(start, end), handle)
        var px = pointer.x
        var py = pointer.y
        if (abs(px - anchor.x) < MIN_SIZE) px = anchor.x + if (px >= anchor.x) MIN_SIZE else -MIN_SIZE
        if (abs(py - anchor.y) < MIN_SIZE) py = anchor.y + if (py >= anchor.y) MIN_SIZE else -MIN_SIZE
        return anchor to Pt(px, py)
    }

    /** Open-shape resize: drag the grabbed endpoint; the other stays fixed. */
    fun resizeOpenShape(start: Pt, end: Pt, handle: HandleId, pointer: Pt): Pair<Pt, Pt> =
        if (handle == HandleId.START) pointer to end else start to pointer

    /** Square-locked closed-shape resize (the perfect circle): the box stays square, sized by the
     *  larger pointer delta from the anchored corner. Returns new (start, end). */
    fun resizeSquareShape(start: Pt, end: Pt, handle: HandleId, pointer: Pt): Pair<Pt, Pt> {
        val anchor = cornerAnchor(Rect.fromPoints(start, end), handle)
        val side = max(max(abs(pointer.x - anchor.x), abs(pointer.y - anchor.y)), MIN_SIZE)
        val sx = if (pointer.x >= anchor.x) 1.0 else -1.0
        val sy = if (pointer.y >= anchor.y) 1.0 else -1.0
        return anchor to Pt(anchor.x + sx * side, anchor.y + sy * side)
    }

    /**
     * Text box rect resize via any of the 8 handles (page-local pointer). [width] is
     * the wrap width; [height] is the reserved minimum (the box still grows to fit its
     * text). The dragged edge follows the pointer, the opposite edge stays fixed, and
     * both axes clamp to [MIN_SIZE]. Returns new (pos, width, height).
     */
    fun resizeText(pos: Pt, width: Double, height: Double, handle: HandleId, pointer: Pt): Triple<Pt, Double, Double> {
        var left = pos.x
        var top = pos.y
        var right = pos.x + width
        var bottom = pos.y + height
        val movesLeft = handle == HandleId.TL || handle == HandleId.BL || handle == HandleId.L
        val movesRight = handle == HandleId.TR || handle == HandleId.BR || handle == HandleId.R
        val movesTop = handle == HandleId.TL || handle == HandleId.TR || handle == HandleId.T
        val movesBottom = handle == HandleId.BL || handle == HandleId.BR || handle == HandleId.B
        if (movesLeft) left = pointer.x
        if (movesRight) right = pointer.x
        if (movesTop) top = pointer.y
        if (movesBottom) bottom = pointer.y
        if (right - left < MIN_SIZE) { if (movesLeft) left = right - MIN_SIZE else right = left + MIN_SIZE }
        if (bottom - top < MIN_SIZE) { if (movesTop) top = bottom - MIN_SIZE else bottom = top + MIN_SIZE }
        return Triple(Pt(left, top), right - left, bottom - top)
    }

    /**
     * Table rect resize via any of the 8 handles (page-local pointer).
     * Computes new position, individual colWidths and rowHeights proportionally.
     */
    fun resizeTable(
        pos: Pt,
        colWidths: List<Double>,
        rowHeights: List<Double>,
        handle: HandleId,
        pointer: Pt,
    ): Triple<Pt, List<Double>, List<Double>> {
        val totalW = colWidths.sum()
        val totalH = rowHeights.sum()
        var left = pos.x
        var top = pos.y
        var right = pos.x + totalW
        var bottom = pos.y + totalH

        val movesLeft = handle == HandleId.TL || handle == HandleId.BL || handle == HandleId.L
        val movesRight = handle == HandleId.TR || handle == HandleId.BR || handle == HandleId.R
        val movesTop = handle == HandleId.TL || handle == HandleId.TR || handle == HandleId.T
        val movesBottom = handle == HandleId.BL || handle == HandleId.BR || handle == HandleId.B

        if (movesLeft) left = pointer.x
        if (movesRight) right = pointer.x
        if (movesTop) top = pointer.y
        if (movesBottom) bottom = pointer.y

        val minTotalW = TableItem.MIN_CELL_WIDTH * colWidths.size
        val minTotalH = TableItem.MIN_CELL_HEIGHT * rowHeights.size

        if (right - left < minTotalW) {
            if (movesLeft) left = right - minTotalW else right = left + minTotalW
        }
        if (bottom - top < minTotalH) {
            if (movesTop) top = bottom - minTotalH else bottom = top + minTotalH
        }

        val newW = right - left
        val newH = bottom - top

        val scaleX = if (totalW > 0.0) newW / totalW else 1.0
        val scaleY = if (totalH > 0.0) newH / totalH else 1.0

        val newCols = colWidths.map { (it * scaleX).coerceIn(TableItem.MIN_CELL_WIDTH, 2000.0) }
        val newRows = rowHeights.map { (it * scaleY).coerceIn(TableItem.MIN_CELL_HEIGHT, 2000.0) }

        return Triple(Pt(left, top), newCols, newRows)
    }
}
