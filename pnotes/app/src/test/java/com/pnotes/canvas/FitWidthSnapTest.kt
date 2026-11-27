package com.pnotes.canvas

import com.pnotes.core.FakeSurfaceFactory
import com.pnotes.core.model.Document
import com.pnotes.core.model.Rgba
import com.pnotes.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the live magnetic fit-to-width snap and the sidebar-resize re-fit. */
class FitWidthSnapTest {

    private fun state(pages: Int = 1, viewportW: Int = 1000): CanvasState =
        CanvasState(Document.blank(pages), FakeSurfaceFactory(), Palette.forAppearance("dark", Rgba(0, 230, 118))).apply {
            this.viewportW = viewportW
            viewportH = 1400
            relayout()
        }

    @Test fun zoomWithinBandSticksToFitWidth() {
        val st = state()
        val fit = st.fitWidthZoom()
        assertFalse(st.fitWidthActive)

        val z = st.snapZoomToFit(fit * 1.03) // inside the ±5% band
        assertEquals(fit, z, 1e-9) // stuck exactly to fit-width
        assertTrue(st.fitWidthActive)
    }

    @Test fun zoomOutsideBandPassesThroughAndClearsFit() {
        val st = state()
        val fit = st.fitWidthZoom()
        st.fitWidthActive = true // pretend we were stuck; leaving the band must release

        val z = st.snapZoomToFit(fit * 1.3)
        assertEquals(fit * 1.3, z, 1e-9) // raw zoom passes through untouched
        assertFalse(st.fitWidthActive)
    }

    @Test fun noHeightMagnetInVerticalMode() {
        val st = state()
        assertEquals(0.0, st.fitHeightZoom(), 1e-9)
        val fitW = st.fitWidthZoom()
        st.snapZoomToFit(fitW)
        assertTrue(st.fitWidthActive)
        assertFalse(st.fitHeightActive)
    }

    @Test fun paginatedPinchSticksToFitHeight() {
        val st = state()
        st.verticalScroll = false
        st.relayout()
        val fit = st.fitHeightZoom()
        assertTrue(fit > 0.0)

        val z = st.snapZoomToFit(fit * 1.03) // inside the same ±5% band as fit-width
        assertEquals(fit, z, 1e-9)
        assertTrue(st.fitHeightActive)
        assertFalse(st.fitWidthActive)

        // Pinching past the band releases the magnet like fit-width does.
        val out = st.snapZoomToFit(fit * 1.3)
        assertEquals(fit * 1.3, out, 1e-9)
        assertFalse(st.fitHeightActive)
    }

    @Test fun nearerMagnetWinsWhenBothGrab() {
        val st = state()
        st.verticalScroll = false
        st.relayout()
        val fitW = st.fitWidthZoom()
        val fitH = st.fitHeightZoom()
        // A4 portrait in a 1000x1400 viewport: the two targets are distinct but close
        // (fitH sits ~1% above fitW), so approach each from its far side.
        assertTrue(fitW < fitH)
        st.snapZoomToFit(fitH * 1.01)
        assertTrue(st.fitHeightActive)
        st.snapZoomToFit(fitW * 0.99)
        assertTrue(st.fitWidthActive)
        assertFalse(st.fitHeightActive)
    }

    @Test fun resizeRefitsHeightWhileHeightFitActive() {
        val st = state()
        st.verticalScroll = false
        st.relayout()
        st.snapZoomToFit(st.fitHeightZoom())
        assertTrue(st.fitHeightActive)

        st.viewportH = 1000
        st.relayout()
        st.reflowFitWidthForResize()
        assertEquals(st.fitHeightZoom(), st.zoom, 1e-9)
    }

    @Test fun resizeRefitsWidthWhileFitActive() {
        val st = state(viewportW = 1000)
        st.fitWidth()
        val wide = st.zoom
        assertTrue(st.fitWidthActive)

        // Sidebar opens: narrower viewport, same content width.
        st.viewportW = 776
        st.relayout()
        st.reflowFitWidthForResize()

        assertEquals(st.fitWidthZoom(), st.zoom, 1e-9)
        assertTrue(st.zoom < wide)
    }

    @Test fun resizeRefitsEvenWhenZoomLocked() {
        val st = state(viewportW = 1000)
        st.fitWidth()
        st.zoomLocked = true // lock freezes user gestures, not the resize re-fit

        st.viewportW = 776
        st.relayout()
        st.reflowFitWidthForResize()

        assertEquals(st.fitWidthZoom(), st.zoom, 1e-9)
    }

    @Test fun resizeIsNoOpWhenNotFitToWidth() {
        val st = state(viewportW = 1000)
        st.zoom = 2.5
        st.fitWidthActive = false

        st.viewportW = 776
        st.relayout()
        st.reflowFitWidthForResize()

        assertEquals(2.5, st.zoom, 1e-9) // arbitrary zoom preserved
    }

    @Test fun dualWidthMagnetsWithToolbarInset() {
        val st = state(viewportW = 1000)
        st.setToolbarInsets(120.0, 0.0, 0.0, 0.0) // toolbar docked on left
        val full = st.fitWidthFullZoom()
        val inset = st.fitWidthInsetZoom()

        assertTrue("Inset fit should be smaller zoom than full fit", inset < full)
        assertTrue(inset > 0.0)
        assertTrue(full > 0.0)

        // 1. Pinch near full fit (screen edge to screen edge)
        val zFull = st.snapZoomToFit(full * 1.02)
        assertEquals(full, zFull, 1e-9)
        assertTrue(st.fitWidthActive)
        assertTrue(st.fitWidthFullActive)
        assertFalse(st.fitWidthInsetActive)

        // 2. Pinch near inset fit (toolbar edge to screen edge)
        val zInset = st.snapZoomToFit(inset * 0.98)
        assertEquals(inset, zInset, 1e-9)
        assertTrue(st.fitWidthActive)
        assertTrue(st.fitWidthInsetActive)
        assertFalse(st.fitWidthFullActive)

        // Origin should align with equal physical distance on both sides
        st.zoom = zInset
        st.scrollX = 0.0
        val ox = st.origin().x
        assertEquals(120.0, ox, 1e-9) // distance from left physical edge
        val rightEdge = ox + st.contentW * st.zoom
        assertEquals(1000.0 - 120.0, rightEdge, 1e-9) // distance from right physical edge: 1000 - 880 = 120.0

        // 3. Pinch far away releases both
        val zOut = st.snapZoomToFit(full * 1.3)
        assertEquals(full * 1.3, zOut, 1e-9)
        assertFalse(st.fitWidthActive)
        assertFalse(st.fitWidthFullActive)
        assertFalse(st.fitWidthInsetActive)
    }

    @Test fun fullWidthFitAlignsWithPhysicalScreenEdgesRegardlessOfInsets() {
        val st = state(viewportW = 1000)
        st.insetLeft = 48.0
        st.insetRight = 48.0
        st.relayout()

        val full = st.fitWidthFullZoom()
        val z = st.snapZoomToFit(full)
        st.zoom = z
        assertTrue(st.fitWidthFullActive)
        assertEquals(full, st.zoom, 1e-9)

        st.scrollX = 0.0
        val ox = st.origin().x
        assertEquals(0.0, ox, 1e-9) // exactly at left physical edge
        val rightEdge = ox + st.contentW * st.zoom
        assertEquals(1000.0, rightEdge, 1e-9) // exactly at right physical edge
        assertEquals(0.0, st.maxScrollX(), 1e-9) // locked, zero drift
    }
}
