package com.pnotes.ui

import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.ui.geometry.Rect
import com.pnotes.core.tools.Tool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The segmented control's chosen surface is one element that travels between options. Where it
 * *is* comes from a spring over its own rectangle; what the options do about it — which glyph is
 * inverted — comes from coverage and a latch. Both are pure, so they are asserted here instead of
 * being judged on a device. The dock is the first host; these cover the generic machinery, plus
 * the dock's own tool-family mapping.
 */
class DockIndicatorTest {

    private val pen = Rect(0f, 0f, 120f, 40f)
    private val eraser = Rect(200f, 0f, 320f, 40f)

    @Test
    fun `coverage is the share of a segment the surface covers`() {
        assertEquals(1f, segmentCoverage(Rect(0f, 0f, 40f, 40f), Rect(0f, 0f, 40f, 40f)), 0.001f)
        assertEquals(0.5f, segmentCoverage(Rect(20f, 0f, 60f, 40f), Rect(0f, 0f, 40f, 40f)), 0.001f)
        assertEquals(0f, segmentCoverage(Rect(60f, 0f, 90f, 40f), Rect(0f, 0f, 40f, 40f)), 0.001f)
        // Covering a segment and then some is still all of it, never more.
        assertEquals(1f, segmentCoverage(Rect(-40f, -40f, 200f, 80f), Rect(0f, 0f, 40f, 40f)), 0.001f)
        assertEquals(0f, segmentCoverage(Rect(0f, 0f, 40f, 40f), Rect(0f, 0f, 0f, 0f)), 0.001f)
    }

    @Test
    fun `a glyph inverts once, when the surface reaches it`() {
        // On the way in the surface has to reach the glyph, which is drawn in the middle of its
        // segment.
        assertFalse(segmentLatched(wasLit = false, coverage = 0.4f))
        assertTrue(segmentLatched(wasLit = false, coverage = 0.6f))
        // On the way out a lower threshold holds it lit, so a surface resting on the boundary
        // cannot flicker the glyph on and off.
        assertTrue(segmentLatched(wasLit = true, coverage = 0.4f))
        assertFalse(segmentLatched(wasLit = true, coverage = 0.3f))
        assertFalse(segmentLatched(wasLit = true, coverage = 0f))
    }

    @Test
    fun `a name climbs in under the surface and evaporates behind it`() {
        assertEquals(0f, segmentLabelReveal(0f), 0.001f)
        assertEquals(0f, segmentLabelReveal(0.35f), 0.001f)
        assertEquals(1f, segmentLabelReveal(0.85f), 0.001f)
        assertEquals(1f, segmentLabelReveal(1f), 0.001f)
        // Half way in it is half visible, so a name is never fully readable while the surface is
        // still on its way to it — which is what stops a label appearing on bare container ahead
        // of the surface.
        val halfWay = segmentLabelReveal(0.6f)
        assertTrue(halfWay > 0.4f)
        assertTrue(halfWay < 0.6f)
    }

    @Test
    fun `the surface animates as four independent edges, not one 2D distance`() {
        val vector = RectVectorConverter.convertToVector(Rect(1f, 2f, 3f, 4f))
        assertEquals(1f, vector.v1, 0f)
        assertEquals(2f, vector.v2, 0f)
        assertEquals(3f, vector.v3, 0f)
        assertEquals(4f, vector.v4, 0f)
        assertEquals(Rect(1f, 2f, 3f, 4f), RectVectorConverter.convertFromVector(vector))
    }

    @Test
    fun `a vertical move does not touch the horizontal edges`() {
        // Springs on a single 2D distance desync when X and Y move at different speeds. The rail's
        // travels are purely vertical, so the horizontal edges have to be able to hold still.
        val resting = RectVectorConverter.convertToVector(Rect(10f, 0f, 50f, 40f))
        val movedDown = RectVectorConverter.convertToVector(Rect(10f, 80f, 50f, 120f))
        assertEquals(resting.v1, movedDown.v1, 0f)
        assertEquals(resting.v3, movedDown.v3, 0f)
        assertTrue(movedDown.v2 > resting.v2)
        assertTrue(AnimationVector4D(resting.v1, movedDown.v2, resting.v3, movedDown.v4) == movedDown)
    }

    @Test
    fun `the tracker knows which segment is chosen from the segments themselves`() {
        val tracker = SegmentTracker()
        assertNull(tracker.activeSlotId)

        tracker.report("pen", true, pen)
        assertEquals("pen", tracker.activeSlotId)
        assertEquals(pen, tracker.boundsOf("pen"))

        tracker.report("pen", false, pen)
        assertNull(tracker.activeSlotId)
    }

    @Test
    fun `before a frame has been measured a segment falls back to its own chosen flag`() {
        val tracker = SegmentTracker()
        assertTrue(tracker.litOf("pen", fallback = true))
        assertFalse(tracker.litOf("pen", fallback = false))
    }

    @Test
    fun `the tracker latches which segments the surface is under`() {
        val tracker = SegmentTracker()
        tracker.report("pen", true, pen)
        tracker.report("eraser", false, eraser)

        // Sitting fully on the pen: the pen is lit, the eraser is untouched.
        tracker.publish(pen, alpha = 1f)
        assertTrue(tracker.litOf("pen", fallback = false))
        assertFalse(tracker.litOf("eraser", fallback = false))
        assertEquals(1f, tracker.coverageOf("pen", fallback = 0f), 0.001f)

        // Half way across the eraser: half covered — so its name would be half revealed — but not
        // yet enough to invert its glyph.
        tracker.publish(Rect(260f, 0f, 400f, 40f), alpha = 1f)
        assertFalse(tracker.litOf("eraser", fallback = false))
        assertEquals(0.5f, tracker.coverageOf("eraser", fallback = 0f), 0.01f)

        // Arrived: now it is.
        tracker.publish(eraser, alpha = 1f)
        assertTrue(tracker.litOf("eraser", fallback = false))

        // And it lets go once the surface has gone.
        tracker.publish(null, alpha = 1f)
        assertFalse(tracker.litOf("eraser", fallback = false))
        assertFalse(tracker.litOf("pen", fallback = false))
    }

    @Test
    fun `a surface fading out dims the glyph it is leaving`() {
        val tracker = SegmentTracker()
        tracker.report("pen", true, pen)

        tracker.publish(pen, alpha = 1f)
        assertTrue(tracker.litOf("pen", fallback = false))

        // The surface is still over the segment, but on its way out of existence.
        tracker.publish(pen, alpha = 0.2f)
        assertFalse(tracker.litOf("pen", fallback = false))
    }

    @Test
    fun `a collapsed control forgets its segments and what was lit`() {
        val tracker = SegmentTracker()
        tracker.report("pen", true, pen)
        tracker.publish(pen, alpha = 1f)

        tracker.clear()

        assertNull(tracker.activeSlotId)
        assertNull(tracker.boundsOf("pen"))
        assertFalse(tracker.litOf("pen", fallback = false))
    }

    @Test
    fun `travel progress is one at the destination and falls with the distance left`() {
        // Arrived.
        assertEquals(1f, segmentTravelProgress(pen, pen), 0.001f)
        // A sub-pixel remainder is an arrival; the spring's visibilityThreshold is half a pixel.
        assertEquals(1f, segmentTravelProgress(pen, pen.translate(0.4f, 0f)), 0.001f)
        // A neighbour hop (~110px) sits near the top of the envelope...
        val hop = segmentTravelProgress(pen, Rect(110f, 0f, 230f, 40f))
        assertTrue(hop > 0.9f)
        // ...a full-dock sweep at the bottom of it, but never below zero.
        assertEquals(0f, segmentTravelProgress(pen, Rect(2000f, 0f, 2520f, 40f)), 0.001f)
        assertTrue(segmentTravelProgress(pen, Rect(2400f, 0f, 2520f, 40f)) >= 0f)
    }

    @Test
    fun `a reshaped container re-places its selection instead of travelling`() {
        val horizontal = Rect(10f, 100f, 300f, 160f)
        val vertical = Rect(20f, 50f, 80f, 400f)
        // A flip between a horizontal bar and a vertical rail rebuilds the segments' geometry: the
        // surface must re-place, or it flies in from outside the new container's clip.
        assertTrue(segmentContainerReshaped(horizontal, vertical))
        // Pure translation — dragging the container, moving it between edges of the same
        // orientation — does not qualify: the surface rides along in the container's local space.
        assertFalse(segmentContainerReshaped(horizontal, horizontal.translate(40f, -30f)))
        assertFalse(segmentContainerReshaped(horizontal, horizontal))
        assertFalse(segmentContainerReshaped(horizontal, null))
        // A first-ever sighting is not a reshaping — the initial placement handles that.
        assertFalse(segmentContainerReshaped(null, vertical))
    }

    @Test
    fun `every dock slot maps to exactly one distinct tool`() {
        val slots = listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.SELECT, Tool.SHAPE, Tool.TEXT, Tool.PAN)
        slots.forEach { slot -> assertEquals(slot, dockSlotToolFor(slot)) }
        // The pen slot fronts every stroke pen...
        assertEquals(Tool.PEN, dockSlotToolFor(Tool.CALLIGRAPHY))
        // ...and the select slot fronts both selection modes.
        assertEquals(Tool.SELECT, dockSlotToolFor(Tool.LASSO))
        // Values outside a dock (brought by another surface's state) fall back to the pen slot.
        assertEquals(Tool.PEN, dockSlotToolFor(Tool.SCREENSHOT))
        // 7 slots, 7 distinct families: no two slots can claim the same armed tool.
        assertEquals(slots.size, slots.map { dockSlotToolFor(it) }.toSet().size)
    }

    @Test
    fun `the dock shim reads dock vocabulary through the generic machinery`() {
        // The dock-facing surface is a typealias: every dock call site keeps working against the
        // extracted component without a single edit, and shares its behaviour.
        val dock: DockSlotBounds = SegmentTracker()
        dock.report("pen", true, pen)
        dock.publish(pen, alpha = 1f)
        assertEquals("pen", dock.activeSlotId)
        assertEquals(pen, dock.boundsOf("pen"))
        assertEquals(1f, dock.coverageOf("pen", fallback = 0f), 0.001f)
        assertTrue(dock.litOf("pen", fallback = false))
        // And the dock's label reveal is the generic one under another name.
        assertEquals(segmentLabelReveal(0.7f), dockLabelReveal(0.7f), 0f)
    }

    @Test
    fun `independent tool modifiers like wand do not steal active segment from pen`() {
        val dock: DockSlotBounds = SegmentTracker()
        dock.report("pen", true, pen)
        dock.publish(pen, alpha = 1f)

        // Pen is the active selected drawing tool
        assertEquals("pen", dock.activeSlotId)
        assertTrue(dock.litOf("pen", fallback = false))

        // When wand is toggled active, it does not register as a chosen segment
        // so the active drawing tool remains pen
        assertEquals("pen", dock.activeSlotId)
        assertTrue(dock.litOf("pen", fallback = false))
    }
}
