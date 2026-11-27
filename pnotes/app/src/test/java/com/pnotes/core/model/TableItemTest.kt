package com.pnotes.core.model

import com.pnotes.core.geometry.Affine
import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableItemTest {

    @Test
    fun defaultTableDimensionsAndBounds() {
        val table = TableItem(pos = Pt(100.0, 200.0), rowCount = 3, colCount = 4)
        assertEquals(3, table.rowCount)
        assertEquals(4, table.colCount)
        assertEquals(3, table.rowHeights.size)
        assertEquals(4, table.colWidths.size)

        val totalW = 4 * TableItem.DEFAULT_COL_WIDTH
        val totalH = 3 * TableItem.DEFAULT_ROW_HEIGHT
        assertEquals(totalW, table.totalWidth, 1e-9)
        assertEquals(totalH, table.totalHeight, 1e-9)
        assertEquals(Rect(100.0, 200.0, totalW, totalH), table.bounds())
    }

    @Test
    fun cellBoundsCalculation() {
        val table = TableItem(
            pos = Pt(10.0, 20.0),
            rowCount = 2,
            colCount = 2,
            rowHeights = mutableListOf(50.0, 60.0),
            colWidths = mutableListOf(100.0, 150.0),
        )

        assertEquals(Rect(10.0, 20.0, 100.0, 50.0), table.cellBounds(0, 0))
        assertEquals(Rect(110.0, 20.0, 150.0, 50.0), table.cellBounds(0, 1))
        assertEquals(Rect(10.0, 70.0, 100.0, 60.0), table.cellBounds(1, 0))
        assertEquals(Rect(110.0, 70.0, 150.0, 60.0), table.cellBounds(1, 1))
    }

    @Test
    fun cellAtHitTesting() {
        val table = TableItem(
            pos = Pt(0.0, 0.0),
            rowCount = 2,
            colCount = 2,
            rowHeights = mutableListOf(100.0, 100.0),
            colWidths = mutableListOf(100.0, 100.0),
        )

        assertEquals(Pair(0, 0), table.cellAt(Pt(50.0, 50.0)))
        assertEquals(Pair(0, 1), table.cellAt(Pt(150.0, 50.0)))
        assertEquals(Pair(1, 0), table.cellAt(Pt(50.0, 150.0)))
        assertEquals(Pair(1, 1), table.cellAt(Pt(150.0, 150.0)))
        assertNull(table.cellAt(Pt(250.0, 50.0)))
        assertNull(table.cellAt(Pt(-10.0, 50.0)))
    }

    @Test
    fun addAndRemoveRowsAndColumns() {
        val table = TableItem(pos = Pt(0.0, 0.0), rowCount = 2, colCount = 2)
        table.cellTexts[Pair(1, 1)] = "BottomRight"
        table.cellTexts[Pair(2, 2)] = "WillBePruned"

        assertTrue(table.addRow(80.0))
        assertEquals(3, table.rowCount)
        assertEquals(80.0, table.rowHeights[2], 1e-9)

        assertTrue(table.addCol(140.0))
        assertEquals(3, table.colCount)
        assertEquals(140.0, table.colWidths[2], 1e-9)

        table.cellTexts[Pair(2, 2)] = "Row2Col2"

        assertTrue(table.removeRow())
        assertEquals(2, table.rowCount)
        assertFalse(table.cellTexts.containsKey(Pair(2, 2)))
        assertTrue(table.cellTexts.containsKey(Pair(1, 1)))

        assertTrue(table.removeCol())
        assertEquals(2, table.colCount)

        // Try removing down to minimum
        assertTrue(table.removeRow())
        assertEquals(1, table.rowCount)
        assertFalse(table.removeRow()) // Cannot remove below 1 row
        assertEquals(1, table.rowCount)
    }

    @Test
    fun setEqualDimensions() {
        val table = TableItem(
            pos = Pt(0.0, 0.0),
            rowCount = 3,
            colCount = 2,
            rowHeights = mutableListOf(30.0, 60.0, 90.0),
            colWidths = mutableListOf(100.0, 200.0),
        )

        table.setEqualRowHeights()
        for (h in table.rowHeights) {
            assertEquals(60.0, h, 1e-9)
        }

        table.setEqualColWidths()
        for (w in table.colWidths) {
            assertEquals(150.0, w, 1e-9)
        }
    }

    @Test
    fun deepCopyPreservesAllFields() {
        val orig = TableItem(
            pos = Pt(15.0, 25.0),
            rowCount = 2,
            colCount = 3,
            strokeRgba = Rgba(10, 20, 30, 255),
            strokeWidth = 3.5,
            hasHeaderRow = false,
            hasHeaderCol = true,
            fillRgba = Rgba(100, 150, 200, 180),
        )
        orig.cellTexts[Pair(0, 1)] = "CellData"
        orig.locked = true

        val copy = orig.deepCopy()
        assertEquals(orig.pos, copy.pos)
        assertEquals(orig.rowCount, copy.rowCount)
        assertEquals(orig.colCount, copy.colCount)
        assertEquals(orig.rowHeights, copy.rowHeights)
        assertEquals(orig.colWidths, copy.colWidths)
        assertEquals(orig.strokeRgba, copy.strokeRgba)
        assertEquals(orig.strokeWidth, copy.strokeWidth, 1e-9)
        assertEquals(orig.hasHeaderRow, copy.hasHeaderRow)
        assertEquals(orig.hasHeaderCol, copy.hasHeaderCol)
        assertEquals(orig.fillRgba, copy.fillRgba)
        assertEquals(orig.cellTexts, copy.cellTexts)
        assertEquals(orig.locked, copy.locked)

        // Verify copy is deep
        copy.translate(10.0, 10.0)
        assertEquals(Pt(25.0, 35.0), copy.pos)
        assertEquals(Pt(15.0, 25.0), orig.pos)
    }

    @Test
    fun snapshotAndRestoreGeometry() {
        val table = TableItem(pos = Pt(50.0, 50.0), rowCount = 2, colCount = 2)
        val snap = table.snapshotGeometry()

        table.translate(100.0, 100.0)
        table.addRow(99.0)
        table.strokeWidth = 8.0

        table.restoreGeometry(snap)
        assertEquals(Pt(50.0, 50.0), table.pos)
        assertEquals(2, table.rowCount)
        assertEquals(2.0, table.strokeWidth, 1e-9)
    }

    @Test
    fun applyTransformScalesDimensions() {
        val table = TableItem(
            pos = Pt(10.0, 20.0),
            rowCount = 2,
            colCount = 2,
            rowHeights = mutableListOf(50.0, 50.0),
            colWidths = mutableListOf(100.0, 100.0),
            strokeWidth = 2.0,
        )

        val affine = Affine(2.0, 0.0, 0.0, 3.0, 0.0, 0.0)
        table.applyTransform(affine)

        assertEquals(20.0, table.pos.x, 1e-9)
        assertEquals(60.0, table.pos.y, 1e-9)
        assertEquals(200.0, table.colWidths[0], 1e-9)
        assertEquals(150.0, table.rowHeights[0], 1e-9)
    }
}
