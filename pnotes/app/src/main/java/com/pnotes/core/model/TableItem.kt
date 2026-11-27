package com.pnotes.core.model

import com.pnotes.core.geometry.Affine
import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.pal.FontSpec
import com.pnotes.core.pal.Pen
import com.pnotes.core.pal.Renderer
import com.pnotes.core.pal.TextMeasurer

/**
 * A structured table item placed on the canvas (imported from Star Notes architecture).
 *
 * Maintains a 2D grid with independent [colWidths] and [rowHeights], optional header highlights
 * ([hasHeaderRow], [hasHeaderCol]), background fills ([fillRgba]), border outline and grid lines
 * ([strokeRgba], [strokeWidth]), and optional text content per cell ([cellTexts]).
 */
class TableItem(
    var pos: Pt,
    var rowCount: Int = DEFAULT_ROWS,
    var colCount: Int = DEFAULT_COLS,
    val rowHeights: MutableList<Double> = MutableList(rowCount.coerceIn(MIN_ROWS, MAX_ROWS)) { DEFAULT_ROW_HEIGHT },
    val colWidths: MutableList<Double> = MutableList(colCount.coerceIn(MIN_COLS, MAX_COLS)) { DEFAULT_COL_WIDTH },
    var strokeRgba: Rgba = DEFAULT_STROKE_COLOR,
    var strokeWidth: Double = DEFAULT_STROKE_WIDTH,
    var hasHeaderRow: Boolean = true,
    var hasHeaderCol: Boolean = false,
    var fillRgba: Rgba? = null,
    val cellTexts: MutableMap<Pair<Int, Int>, String> = mutableMapOf(),
    private var measurer: TextMeasurer? = null,
) : CanvasItem, Resizable {

    init {
        ensureDimensions()
    }

    override val kind = KIND
    override val resizable = true
    override var locked = false

    fun setMeasurer(tm: TextMeasurer) {
        measurer = tm
    }

    private fun ensureDimensions() {
        rowCount = rowCount.coerceIn(MIN_ROWS, MAX_ROWS)
        colCount = colCount.coerceIn(MIN_COLS, MAX_COLS)
        while (rowHeights.size < rowCount) rowHeights.add(DEFAULT_ROW_HEIGHT)
        while (rowHeights.size > rowCount) rowHeights.removeAt(rowHeights.size - 1)
        while (colWidths.size < colCount) colWidths.add(DEFAULT_COL_WIDTH)
        while (colWidths.size > colCount) colWidths.removeAt(colWidths.size - 1)
    }

    val totalWidth: Double get() = colWidths.sum()
    val totalHeight: Double get() = rowHeights.sum()

    override fun bounds(): Rect =
        Rect(pos.x, pos.y, maxOf(totalWidth, MIN_CELL_WIDTH), maxOf(totalHeight, MIN_CELL_HEIGHT))

    /** Get bounding rectangle for cell at [row], [col] in content coordinates. */
    fun cellBounds(row: Int, col: Int): Rect {
        val r = row.coerceIn(0, (rowCount - 1).coerceAtLeast(0))
        val c = col.coerceIn(0, (colCount - 1).coerceAtLeast(0))
        var xOff = pos.x
        for (j in 0 until c) {
            xOff += colWidths.getOrElse(j) { DEFAULT_COL_WIDTH }
        }
        var yOff = pos.y
        for (i in 0 until r) {
            yOff += rowHeights.getOrElse(i) { DEFAULT_ROW_HEIGHT }
        }
        val w = colWidths.getOrElse(c) { DEFAULT_COL_WIDTH }
        val h = rowHeights.getOrElse(r) { DEFAULT_ROW_HEIGHT }
        return Rect(xOff, yOff, w, h)
    }

    /** Determine which cell (row, col) contains point [p], or null if outside bounds. */
    fun cellAt(p: Pt): Pair<Int, Int>? {
        if (!bounds().contains(p)) return null
        var curY = pos.y
        var targetRow = -1
        for (i in 0 until rowCount) {
            val h = rowHeights.getOrElse(i) { DEFAULT_ROW_HEIGHT }
            if (p.y >= curY && p.y <= curY + h) {
                targetRow = i
                break
            }
            curY += h
        }
        var curX = pos.x
        var targetCol = -1
        for (j in 0 until colCount) {
            val w = colWidths.getOrElse(j) { DEFAULT_COL_WIDTH }
            if (p.x >= curX && p.x <= curX + w) {
                targetCol = j
                break
            }
            curX += w
        }
        if (targetRow in 0 until rowCount && targetCol in 0 until colCount) {
            return targetRow to targetCol
        }
        return null
    }

    /** Add a new row to the bottom (clamped to [MAX_ROWS]). */
    fun addRow(height: Double = (rowHeights.lastOrNull() ?: DEFAULT_ROW_HEIGHT)): Boolean {
        if (rowCount >= MAX_ROWS) return false
        rowCount++
        rowHeights.add(height.coerceIn(MIN_CELL_HEIGHT, 500.0))
        return true
    }

    /** Remove bottom row (floored at [MIN_ROWS]). */
    fun removeRow(): Boolean {
        if (rowCount <= MIN_ROWS) return false
        rowCount--
        rowHeights.removeAt(rowHeights.size - 1)
        // Clean up texts in removed row
        val toRemove = cellTexts.keys.filter { it.first >= rowCount }
        for (k in toRemove) cellTexts.remove(k)
        return true
    }

    /** Add a new column to the right (clamped to [MAX_COLS]). */
    fun addCol(width: Double = (colWidths.lastOrNull() ?: DEFAULT_COL_WIDTH)): Boolean {
        if (colCount >= MAX_COLS) return false
        colCount++
        colWidths.add(width.coerceIn(MIN_CELL_WIDTH, 800.0))
        return true
    }

    /** Remove rightmost column (floored at [MIN_COLS]). */
    fun removeCol(): Boolean {
        if (colCount <= MIN_COLS) return false
        colCount--
        colWidths.removeAt(colWidths.size - 1)
        // Clean up texts in removed column
        val toRemove = cellTexts.keys.filter { it.second >= colCount }
        for (k in toRemove) cellTexts.remove(k)
        return true
    }

    /** Make all rows equal in height. */
    fun setEqualRowHeights() {
        val avg = totalHeight / maxOf(rowCount, 1)
        for (i in rowHeights.indices) rowHeights[i] = avg
    }

    /** Make all columns equal in width. */
    fun setEqualColWidths() {
        val avg = totalWidth / maxOf(colCount, 1)
        for (j in colWidths.indices) colWidths[j] = avg
    }

    /** Hit-test internal vertical column dividers. Returns col index (0..colCount-2) or null. */
    fun hitVerticalDivider(p: Pt, tolerance: Double): Int? {
        if (p.y < pos.y - tolerance || p.y > pos.y + totalHeight + tolerance) return null
        var curX = pos.x
        for (j in 0 until colCount - 1) {
            curX += colWidths.getOrElse(j) { DEFAULT_COL_WIDTH }
            if (kotlin.math.abs(p.x - curX) <= tolerance) {
                return j
            }
        }
        return null
    }

    /** Hit-test internal horizontal row dividers. Returns row index (0..rowCount-2) or null. */
    fun hitHorizontalDivider(p: Pt, tolerance: Double): Int? {
        if (p.x < pos.x - tolerance || p.x > pos.x + totalWidth + tolerance) return null
        var curY = pos.y
        for (i in 0 until rowCount - 1) {
            curY += rowHeights.getOrElse(i) { DEFAULT_ROW_HEIGHT }
            if (kotlin.math.abs(p.y - curY) <= tolerance) {
                return i
            }
        }
        return null
    }

    /** Resize specific column by index, clamped to [MIN_CELL_WIDTH]. */
    fun resizeCol(colIndex: Int, newWidth: Double) {
        if (colIndex in colWidths.indices) {
            colWidths[colIndex] = newWidth.coerceIn(MIN_CELL_WIDTH, 1200.0)
        }
    }

    /** Resize specific row by index, clamped to [MIN_CELL_HEIGHT]. */
    fun resizeRow(rowIndex: Int, newHeight: Double) {
        if (rowIndex in rowHeights.indices) {
            rowHeights[rowIndex] = newHeight.coerceIn(MIN_CELL_HEIGHT, 800.0)
        }
    }

    /** Auto-expand row heights if multi-line cell text exceeds current row height. */
    fun recomputeAutoRowHeights(tm: TextMeasurer? = measurer) {
        if (tm == null || cellTexts.isEmpty()) return
        val font = FontSpec(14.0)
        for (row in 0 until rowCount) {
            var maxRequiredH = DEFAULT_ROW_HEIGHT
            for (col in 0 until colCount) {
                val text = cellTexts[row to col] ?: continue
                if (text.isEmpty()) continue
                val cellW = colWidths.getOrElse(col) { DEFAULT_COL_WIDTH }
                val maxTextW = maxOf(cellW - 16.0, 10.0)
                val measured = tm.measure(text, font, maxTextW)
                val reqH = measured.h + 14.0
                if (reqH > maxRequiredH) maxRequiredH = reqH
            }
            rowHeights[row] = maxOf(rowHeights.getOrElse(row) { DEFAULT_ROW_HEIGHT }, maxRequiredH)
        }
    }

    override fun paint(r: Renderer) {
        val b = bounds()
        val fill = fillRgba
        if (fill != null && fill.a > 0) {
            // Fill header row if enabled
            if (hasHeaderRow && rowHeights.isNotEmpty()) {
                val headerRowH = rowHeights[0]
                r.fillRect(Rect(pos.x, pos.y, totalWidth, headerRowH), fill)
            }
            // Fill header col if enabled
            if (hasHeaderCol && colWidths.isNotEmpty()) {
                val headerColW = colWidths[0]
                r.fillRect(Rect(pos.x, pos.y, headerColW, totalHeight), fill)
            }
            // If neither header is on, fill the entire table surface
            if (!hasHeaderRow && !hasHeaderCol) {
                r.fillRect(b, fill)
            }
        }

        // Draw grid lines
        val pen = Pen(strokeRgba, strokeWidth, cosmetic = false)
        // Outer boundary
        r.strokeRect(b, pen)

        // Horizontal dividers
        var curY = pos.y
        for (i in 0 until rowCount - 1) {
            curY += rowHeights.getOrElse(i) { DEFAULT_ROW_HEIGHT }
            r.strokePolyline(listOf(Pt(pos.x, curY), Pt(pos.x + totalWidth, curY)), pen)
        }

        // Vertical dividers
        var curX = pos.x
        for (j in 0 until colCount - 1) {
            curX += colWidths.getOrElse(j) { DEFAULT_COL_WIDTH }
            r.strokePolyline(listOf(Pt(curX, pos.y), Pt(curX, pos.y + totalHeight)), pen)
        }

        // Render text content in cells
        if (cellTexts.isNotEmpty()) {
            val font = FontSpec(14.0)
            for ((cell, text) in cellTexts) {
                if (text.isEmpty()) continue
                val cb = cellBounds(cell.first, cell.second)
                val padX = 8.0
                val padY = 6.0
                val textRect = Rect(cb.x + padX, cb.y + padY, maxOf(cb.w - padX * 2, 1.0), maxOf(cb.h - padY * 2, 1.0))
                r.drawText(text, textRect, font, strokeRgba)
            }
        }
    }

    override fun translate(dx: Double, dy: Double) {
        pos = Pt(pos.x + dx, pos.y + dy)
    }

    override fun contains(p: Pt): Boolean = bounds().contains(p)

    override fun centroid(): Pt = bounds().center

    override fun intersectsCircle(cx: Double, cy: Double, radius: Double): Boolean {
        val b = bounds()
        val closestX = cx.coerceIn(b.left, b.right)
        val closestY = cy.coerceIn(b.top, b.bottom)
        val distSq = (cx - closestX) * (cx - closestX) + (cy - closestY) * (cy - closestY)
        return distSq <= radius * radius
    }

    override fun snapshotGeometry(): GeometrySnapshot =
        TableSnapshot(
            pos = pos,
            rowCount = rowCount,
            colCount = colCount,
            rowHeights = rowHeights.toList(),
            colWidths = colWidths.toList(),
            strokeWidth = strokeWidth,
        )

    override fun restoreGeometry(snap: GeometrySnapshot) {
        if (snap !is TableSnapshot) return
        pos = snap.pos
        rowCount = snap.rowCount
        colCount = snap.colCount
        rowHeights.clear()
        rowHeights.addAll(snap.rowHeights)
        colWidths.clear()
        colWidths.addAll(snap.colWidths)
        strokeWidth = snap.strokeWidth
        ensureDimensions()
    }

    override fun applyTransform(t: Affine) {
        pos = t.apply(pos)
        val sx = t.scaleX
        val sy = t.scaleY
        for (j in colWidths.indices) {
            colWidths[j] = (colWidths[j] * sx).coerceIn(MIN_CELL_WIDTH, 2000.0)
        }
        for (i in rowHeights.indices) {
            rowHeights[i] = (rowHeights[i] * sy).coerceIn(MIN_CELL_HEIGHT, 2000.0)
        }
        strokeWidth = (strokeWidth * t.linearScale).coerceIn(0.5, 30.0)
    }

    override fun geometry(): GeoHandle =
        TableHandle(pos = pos, colWidths = colWidths.toList(), rowHeights = rowHeights.toList())

    override fun setGeometry(handle: GeoHandle) {
        if (handle !is TableHandle) return
        pos = handle.pos
        colWidths.clear()
        colWidths.addAll(handle.colWidths)
        rowHeights.clear()
        rowHeights.addAll(handle.rowHeights)
        rowCount = rowHeights.size
        colCount = colWidths.size
        ensureDimensions()
    }

    fun deepCopy(measurer: TextMeasurer? = this.measurer): TableItem {
        val copy = TableItem(
            pos = pos,
            rowCount = rowCount,
            colCount = colCount,
            rowHeights = rowHeights.toMutableList(),
            colWidths = colWidths.toMutableList(),
            strokeRgba = strokeRgba,
            strokeWidth = strokeWidth,
            hasHeaderRow = hasHeaderRow,
            hasHeaderCol = hasHeaderCol,
            fillRgba = fillRgba,
            cellTexts = cellTexts.toMutableMap(),
            measurer = measurer,
        )
        copy.locked = locked
        return copy
    }

    companion object {
        const val KIND = "table"
        const val DEFAULT_ROWS = 3
        const val DEFAULT_COLS = 3
        const val DEFAULT_ROW_HEIGHT = 48.0
        const val DEFAULT_COL_WIDTH = 96.0
        const val MIN_ROWS = 1
        const val MAX_ROWS = 20
        const val MIN_COLS = 1
        const val MAX_COLS = 20
        const val MIN_CELL_WIDTH = 24.0
        const val MIN_CELL_HEIGHT = 20.0
        val DEFAULT_STROKE_COLOR = Rgba(0xA1, 0xA1, 0xAA, 0xFF) // Zinc 400
        const val DEFAULT_STROKE_WIDTH = 2.0
    }
}
