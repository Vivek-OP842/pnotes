package com.pnotes.core.model

import com.pnotes.core.FakeRenderer
import com.pnotes.core.FakeTextMeasurer
import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.stroke.Sample
import com.pnotes.core.tools.ShapeKind
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.StrokeTexture
import com.pnotes.core.tools.ToolConfig
import com.pnotes.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelItemsTest {

    private fun horizontalStroke(): Stroke {
        val s = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN))
        s.addSample(Sample(0.0, 0.0, 1.0))
        s.addSample(Sample(10.0, 0.0, 1.0))
        s.addSample(Sample(20.0, 0.0, 1.0))
        return s
    }

    @Test fun strokeCentroidIsMeanOfRawSamples() {
        assertEquals(Pt(10.0, 0.0), horizontalStroke().centroid())
    }

    @Test fun strokeContainsAlongRibbon() {
        val s = horizontalStroke()
        assertTrue(s.contains(Pt(5.0, 0.0)))
        assertFalse(s.contains(Pt(5.0, 6.0)))
    }

    @Test fun strokeEraserCircleUsesRawSamples() {
        val s = horizontalStroke()
        assertTrue(s.intersectsCircle(20.0, 0.0, 2.0)) // raw sample at (20,0)
        assertFalse(s.intersectsCircle(20.0, 40.0, 2.0))
    }

    @Test fun strokeTranslateShiftsSamples() {
        val s = horizontalStroke()
        s.translate(5.0, 5.0)
        assertEquals(Pt(15.0, 5.0), s.centroid())
    }

    @Test fun strokeHighlighterRenderAlpha() {
        val s = Stroke(Tool.HIGHLIGHTER, ToolConfig(rgba = Rgba(0, 230, 118, 255)))
        assertEquals(89, s.renderColor.a)
    }

    @Test fun everyPenStrokeGetsRoundCapsFromTheSweptDisc() {
        // Calligraphy ended flat (butt) before; the swept-disc brush stamps a disc at every sample,
        // so even a previously flat-capped pen now paints round caps/joins (fillCircle) plus the
        // bridging body (fillPolygon).
        val s = Stroke(Tool.CALLIGRAPHY, ToolDefaults.configFor(Tool.CALLIGRAPHY))
        s.addSample(Sample(0.0, 0.0, 1.0))
        s.addSample(Sample(10.0, 0.0, 1.0))
        s.addSample(Sample(20.0, 0.0, 1.0))
        val r = FakeRenderer()
        s.paint(r)
        assertTrue(r.ops.contains("fillCircle"))
        assertTrue(r.ops.contains("fillPolygon"))
    }

    @Test fun zoomedOutInkPaintsAsAHairlineInsteadOfARibbon() {
        val s = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN))
        s.addSample(Sample(0.0, 0.0, 1.0))
        s.addSample(Sample(10.0, 0.0, 1.0))
        s.addSample(Sample(20.0, 0.0, 1.0))
        // A 3 px pen at a tenth of a device pixel per page pixel: the page shows a hairline either
        // way, so a ribbon (two shapes per sample, built and scan-converted) is not worth painting.
        val far = FakeRenderer(deviceScale = 0.1)
        s.paint(far)
        assertTrue(far.ops.contains("strokePolyline"))
        assertFalse("it built a ribbon anyway", far.ops.contains("fillCircle") || far.ops.contains("fillPolygon"))

        // At reading zoom the same stroke is ink again.
        val near = FakeRenderer(deviceScale = 1.0)
        s.paint(near)
        assertTrue(near.ops.contains("fillCircle"))
        assertTrue(near.ops.contains("fillPolygon"))
    }

    @Test fun aNibThatCouldWidenKeepsItsRibbonWhileItsExtraWidthShows() {
        // The decision is made on the widest mark the pen could leave, so a nib that opens past its
        // base width is still ink at a zoom where a plain pen of the same nominal width is not.
        val nib = Stroke(Tool.CALLIGRAPHY, ToolDefaults.configFor(Tool.CALLIGRAPHY)) // base 6, ds 0.6
        nib.addSample(Sample(0.0, 0.0, 1.0))
        nib.addSample(Sample(10.0, 0.0, 1.0))
        // 6 px base × 1.9 of possible direction = 11.4 px of ink; a shade under 2 px of that on
        // screen is still a mark, so the ribbon stays.
        val r = FakeRenderer(deviceScale = 0.15)
        nib.paint(r)
        assertTrue("the nib could still be nearly two pixels wide", r.ops.contains("fillCircle"))

        // Deep enough and even that is a hairline.
        val deep = FakeRenderer(deviceScale = 0.05)
        nib.paint(deep)
        assertTrue(deep.ops.contains("strokePolyline"))
    }

    @Test fun texturedAndTranslucentInkIsNeverAHairline() {
        // The ribbon is what carries the grain, and what a translucent stroke composites in.
        for (cfg in listOf(
            ToolDefaults.configFor(Tool.PEN).copy(texture = StrokeTexture.CHALK),
            ToolDefaults.configFor(Tool.PEN).copy(rgba = Rgba(0, 0, 0, 80)),
        )) {
            val s = Stroke(Tool.PEN, cfg)
            s.addSample(Sample(0.0, 0.0, 1.0))
            s.addSample(Sample(10.0, 0.0, 1.0))
            val r = FakeRenderer(deviceScale = 0.02)
            s.paint(r)
            assertTrue("textured/translucent ink still needs its ribbon", r.ops.contains("fillCircle"))
        }
    }

    @Test fun singleSampleStrokePaintsJustADisc() {
        val s = Stroke(Tool.CALLIGRAPHY, ToolDefaults.configFor(Tool.CALLIGRAPHY))
        s.addSample(Sample(5.0, 5.0, 1.0))
        val r = FakeRenderer()
        s.paint(r)
        assertTrue(r.ops.contains("fillCircle"))
        assertFalse(r.ops.contains("fillPolygon"))
    }

    @Test fun imageItemGeometryAndHits() {
        val img = ImageItem(ImageData(java.io.File("test-image"),64, 48), Rect(10.0, 10.0, 64.0, 48.0))
        assertTrue(img.contains(Pt(20.0, 20.0)))
        assertFalse(img.contains(Pt(200.0, 200.0)))
        assertTrue(img.intersectsCircle(8.0, 12.0, 5.0)) // just left of the rect
        img.translate(5.0, 5.0)
        assertEquals(Rect(15.0, 15.0, 64.0, 48.0), img.geometry().let { (it as RectHandle).rect })
    }

    @Test fun textItemBoundsFollowLines() {
        val m = FakeTextMeasurer()
        val t = TextItem(Pt(5.0, 5.0), width = 300.0, text = "a\nb\nc", measurer = m)
        val lh = m.lineHeight(t.font)
        assertEquals(Rect(5.0, 5.0, 300.0, 3 * lh), t.bounds())
        // Empty text still measures one line tall.
        t.text = ""
        assertEquals(1 * lh, t.bounds().h, 1e-9)
    }

    @Test fun textItemEmptyPaintsNothing() {
        val r = FakeRenderer()
        TextItem(Pt(0.0, 0.0), measurer = FakeTextMeasurer()).paint(r)
        assertFalse(r.ops.contains("drawText"))
    }

    @Test fun filledRectangleContainsInterior() {
        val s = ShapeItem(ShapeKind.RECTANGLE, Pt(0.0, 0.0), Pt(100.0, 60.0), Rgba(0, 0, 0), 3.0, fillRgba = Rgba(0, 0, 0, 64))
        assertTrue(s.contains(Pt(50.0, 30.0)))
    }

    @Test fun unfilledRectangleOnlyHitsOutline() {
        val s = ShapeItem(ShapeKind.RECTANGLE, Pt(0.0, 0.0), Pt(100.0, 60.0), Rgba(0, 0, 0), 3.0, fillRgba = null)
        assertFalse(s.contains(Pt(50.0, 30.0)))    // interior misses
        assertTrue(s.contains(Pt(0.0, 30.0)))       // on the left edge
    }

    @Test fun lineShapeHitTolerance() {
        val s = ShapeItem(ShapeKind.LINE, Pt(0.0, 0.0), Pt(100.0, 0.0), Rgba(0, 0, 0), 3.0)
        assertTrue(s.contains(Pt(50.0, 2.0)))
        assertFalse(s.contains(Pt(50.0, 40.0)))
    }

    @Test fun shapePaintDispatch() {
        val r = FakeRenderer()
        ShapeItem(ShapeKind.RECTANGLE, Pt(0.0, 0.0), Pt(10.0, 10.0), Rgba(0, 0, 0), 3.0, fillRgba = Rgba(0, 0, 0, 64)).paint(r)
        assertTrue(r.ops.containsAll(listOf("fillRect", "strokeRect")))
    }

    @Test fun shapeHandleRoundTrip() {
        val s = ShapeItem(ShapeKind.ELLIPSE, Pt(0.0, 0.0), Pt(10.0, 10.0), Rgba(0, 0, 0))
        s.setGeometry(ShapeHandle(Pt(1.0, 2.0), Pt(3.0, 4.0)))
        assertEquals(Pt(1.0, 2.0), s.start)
        assertEquals(Pt(3.0, 4.0), s.end)
    }

    @Test fun pageBlankDimensions() {
        val p = Page.blank(PageSize.A4, Orientation.PORTRAIT, 150)
        assertEquals(1240.157, p.width, 0.01)
        assertEquals(1753.937, p.height, 0.01)
        assertTrue(p.items.isEmpty())
    }

    @Test fun documentBlankAndTitle() {
        val d = Document.blank()
        assertEquals(1, d.pages.size)
        assertEquals("Untitled", d.title)
        d.path = "/home/user/notes/Meeting.pnote"
        assertEquals("Meeting", d.title)
    }

    @Test fun documentAddPageMarksDirty() {
        val d = Document.blank(count = 1)
        d.dirty = false
        d.addPage(100.0, 200.0)
        assertEquals(2, d.pages.size)
        assertTrue(d.dirty)
    }
}
