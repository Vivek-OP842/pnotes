package com.pnotes.format

import com.pnotes.core.FakeImageCodec
import com.pnotes.core.FakeTextMeasurer
import com.pnotes.core.geometry.Pt
import com.pnotes.core.model.Document
import com.pnotes.core.model.Page
import com.pnotes.core.model.Stroke
import com.pnotes.core.stroke.PredictionAdopt
import com.pnotes.core.stroke.Sample
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import kotlin.math.hypot

/**
 * The predicted tip has to survive a save and a reload, or a note would come back with its last
 * few pixels shorter than the ink the writer drew: the tail's samples are in the file, but
 * without the marker that says "these are the tail", the reload smooths them like any other
 * sample and the line quietly shrinks.
 */
class TipRoundTripTest {

    private val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())

    private fun strokeWithTail(): Stroke {
        val stroke = Stroke(
            Tool.PEN,
            ToolDefaults.configFor(Tool.PEN),
            smoothScale = 3.0,
            lagComp = 0.45,
        )
        stroke.finished = false
        for (i in 0 until 160) {
            val t = i * 8.0
            stroke.addSample(Sample(100.0 + 1.4 * t, 300.0, 0.6, t))
        }
        val nib = Pt(stroke.xAt(stroke.sampleCount - 1), stroke.yAt(stroke.sampleCount - 1))
        // The pen-up event lands on the nib, then the look-ahead is committed verbatim.
        stroke.addSample(Sample(nib.x, nib.y, 0.6, 160 * 8.0))
        val tail = listOf(Pt(nib.x + 2.0, nib.y), Pt(nib.x + 5.0, nib.y))
        val samples = PredictionAdopt.adopt(tail, 0.6, stroke.tAt(stroke.sampleCount - 1))
        stroke.tipCount = samples.size
        for (s in samples) stroke.addSample(s)
        stroke.finished = true
        return stroke
    }

    private fun tipOf(stroke: Stroke): Pt {
        val cl = stroke.geometry().centerline
        return Pt(cl[cl.size - 2].toDouble(), cl[cl.size - 1].toDouble())
    }

    @Test fun theTipSurvivesASaveAndAReload() {
        val stroke = strokeWithTail()
        val drawnTip = tipOf(stroke)
        assertTrue("the tip was not committed as a tip", stroke.tipCount > 0)

        val doc = Document(dpi = 150)
        doc.pages.add(Page(1240.0, 1754.0).also { it.items.add(stroke) })
        val out = ByteArrayOutputStream()
        codec.write(doc, out)
        val imageDir = Files.createTempDirectory("pnotes-img").toFile()
        val loaded = codec.read(ByteArrayInputStream(out.toByteArray()), imageDir = imageDir)

        val back = loaded.pages[0].items.filterIsInstance<Stroke>().single()
        assertEquals("the tail marker was lost", stroke.tipCount, back.tipCount)
        val loadedTip = tipOf(back)
        assertEquals(
            "the reloaded stroke's tip moved",
            drawnTip.x, loadedTip.x, 0.02,
        )
        assertEquals(
            "the reloaded stroke's tip moved",
            drawnTip.y, loadedTip.y, 0.02,
        )
    }

    @Test fun anOrdinaryStrokeStillSerializesWithoutTheTipField() {
        // The field is additive: ink with no look-ahead must write exactly what it always wrote.
        val stroke = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN))
        stroke.finished = false
        stroke.addSample(Sample(10.0, 10.0, 0.5, 0.0))
        stroke.addSample(Sample(20.0, 14.0, 0.5, 8.0))
        stroke.finished = true

        val doc = Document(dpi = 150)
        doc.pages.add(Page(1240.0, 1754.0).also { it.items.add(stroke) })
        val out = ByteArrayOutputStream()
        codec.write(doc, out)
        val manifest = java.util.zip.ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                if (e.name == "manifest.json") return@use zis.readBytes().decodeToString()
                zis.closeEntry()
                e = zis.nextEntry
            }
            ""
        }
        assertTrue("the tip field leaked into ordinary ink", !manifest.contains("tip_count"))
    }

    @Test fun anEraserSplitKeepsTheTailOnTheFragmentThatHoldsIt() {
        val stroke = strokeWithTail()
        val tipBefore = tipOf(stroke)
        // Cut the stroke near its start: the fragment that keeps the tail must still carry it.
        val fragments = stroke.erasedBy(120.0, 300.0, 6.0)
        assertTrue("the stroke was not cut", fragments != null && fragments.isNotEmpty())
        val withTail = fragments!!.filter { it.tipCount > 0 }
        assertTrue("no fragment kept the tail marker", withTail.isNotEmpty())
        for (f in withTail) {
            val t = tipOf(f)
            assertEquals(
                "the fragment's tip moved ${hypot(t.x - tipBefore.x, t.y - tipBefore.y)} px",
                tipBefore.x, t.x, 0.5,
            )
        }
    }
}
