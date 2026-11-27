package com.pnotes.format

import com.pnotes.core.FakeImageCodec
import com.pnotes.core.FakeTextMeasurer
import com.pnotes.core.geometry.Pt
import com.pnotes.core.model.Document
import com.pnotes.core.model.Page
import com.pnotes.core.model.Rgba
import com.pnotes.core.model.TableItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class TableCodecTest {

    private val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())

    @Test
    fun tableRoundTripsThroughDocumentCodec() {
        val doc = Document(dpi = 150)
        val page = Page(800.0, 1000.0)
        val table = TableItem(
            pos = Pt(45.0, 60.0),
            rowCount = 3,
            colCount = 4,
            rowHeights = mutableListOf(40.0, 50.0, 60.0),
            colWidths = mutableListOf(100.0, 120.0, 140.0, 160.0),
            strokeRgba = Rgba(50, 60, 70, 255),
            strokeWidth = 2.5,
            hasHeaderRow = true,
            hasHeaderCol = true,
            fillRgba = Rgba(255, 240, 220, 200),
        )
        table.cellTexts[Pair(0, 0)] = "Header"
        table.cellTexts[Pair(1, 2)] = "Row1Col2"
        table.cellTexts[Pair(2, 3)] = "Total: 100"
        table.locked = true

        page.items.add(table)
        doc.pages.add(page)

        val out = ByteArrayOutputStream()
        codec.write(doc, out)

        val back = codec.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(1, back.pages.size)
        assertEquals(1, back.pages[0].items.size)

        val restored = back.pages[0].items[0] as? TableItem
        assertNotNull(restored)
        restored!!

        assertEquals(45.0, restored.pos.x, 0.01)
        assertEquals(60.0, restored.pos.y, 0.01)
        assertEquals(3, restored.rowCount)
        assertEquals(4, restored.colCount)
        assertEquals(3, restored.rowHeights.size)
        assertEquals(4, restored.colWidths.size)
        assertEquals(40.0, restored.rowHeights[0], 0.01)
        assertEquals(50.0, restored.rowHeights[1], 0.01)
        assertEquals(60.0, restored.rowHeights[2], 0.01)
        assertEquals(100.0, restored.colWidths[0], 0.01)
        assertEquals(120.0, restored.colWidths[1], 0.01)
        assertEquals(140.0, restored.colWidths[2], 0.01)
        assertEquals(160.0, restored.colWidths[3], 0.01)

        assertEquals(Rgba(50, 60, 70, 255), restored.strokeRgba)
        assertEquals(2.5, restored.strokeWidth, 0.01)
        assertTrue(restored.hasHeaderRow)
        assertTrue(restored.hasHeaderCol)
        assertEquals(Rgba(255, 240, 220, 200), restored.fillRgba)

        assertEquals("Header", restored.cellTexts[Pair(0, 0)])
        assertEquals("Row1Col2", restored.cellTexts[Pair(1, 2)])
        assertEquals("Total: 100", restored.cellTexts[Pair(2, 3)])
        assertTrue(restored.locked)
    }

    @Test
    fun tableWithoutFillRoundTrips() {
        val doc = Document(dpi = 150)
        val page = Page(500.0, 500.0)
        val table = TableItem(
            pos = Pt(10.0, 10.0),
            rowCount = 2,
            colCount = 2,
            fillRgba = null,
        )
        page.items.add(table)
        doc.pages.add(page)

        val out = ByteArrayOutputStream()
        codec.write(doc, out)

        val back = codec.read(ByteArrayInputStream(out.toByteArray()))
        val restored = back.pages[0].items[0] as TableItem
        assertEquals(null, restored.fillRgba)
        assertEquals(2, restored.rowCount)
        assertEquals(2, restored.colCount)
    }
}
