package com.pnotes.canvas

import com.pnotes.core.geometry.Pt
import com.pnotes.platform.SessionStore
import com.pnotes.format.DocumentCodec
import com.pnotes.core.FakeImageCodec
import com.pnotes.core.FakeTextMeasurer
import com.pnotes.core.model.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.math.exp

class FlingAndAutosaveTest {

    @Test
    fun flingConstantsProvideFluidMomentum() {
        // Friction must be tuned for natural glide (<= 1.5) rather than abrupt stop
        assertTrue(InteractionController.FLING_FRICTION <= 1.5)
        assertEquals(1.35, InteractionController.FLING_FRICTION, 1e-6)

        // Thresholds should start easily on quick flicks
        assertTrue(InteractionController.FLING_MIN_START <= 100.0)
        assertTrue(InteractionController.FLING_MIN_STOP <= 10.0)

        // Simulate a 4000 px/s fast flick
        val initialVel = 4000.0
        val dt = 1.0 // 1 second of gliding
        val remainingVel = initialVel * exp(-InteractionController.FLING_FRICTION * dt)

        // After 1 full second, it should still have > 1000 px/s velocity gliding smoothly
        assertTrue("Remaining velocity after 1s ($remainingVel) should exceed 1000 px/s", remainingVel > 1000.0)
    }

    @Test
    fun synchronousSessionSavePersistsEditsImmediately() {
        val dir = Files.createTempDirectory("pnotes-sync-session").toFile()
        val pdfDir = Files.createTempDirectory("pnotes-pdf").toFile()
        val imgDir = Files.createTempDirectory("pnotes-img").toFile()
        val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
        val sessionStore = SessionStore(dir, codec, pdfDir, imgDir)

        val doc = Document.blank(1).apply {
            path = "content://test/notes.pnote"
            displayName = "My Drawing"
            dirty = true
        }

        // Synchronous save
        sessionStore.save(
            doc,
            zoom = 1.0,
            scrollX = 0.0,
            scrollY = 250.0,
            zoomLocked = false,
            viewOverrides = ViewOverrides(),
            writeDocument = true
        )

        // Load immediately from disk
        val restored = sessionStore.load()
        org.junit.Assert.assertNotNull(restored)
        assertEquals("My Drawing", restored!!.document.displayName)
        assertEquals(250.0, restored.scrollY, 1e-6)
    }

    @Test
    fun inFlightDrawingIsCommittedWhenAppClosesOrCancels() {
        val doc = Document.blank(1)
        val state = CanvasState(doc, com.pnotes.core.FakeSurfaceFactory(), com.pnotes.ui.theme.Palette.forAppearance("dark", com.pnotes.core.model.Rgba(0, 230, 118))).apply {
            viewportW = 1000
            viewportH = 1400
            relayout()
        }
        val history = com.pnotes.core.history.History()
        var contentChangedCalled = false
        val controller = InteractionController(
            state, history, FakeTextMeasurer(),
            requestRender = {},
            onContentChanged = { contentChangedCalled = true },
        )

        // Simulate pen down at (100, 100)
        controller.startDrawingForTesting(Pt(100.0, 100.0))
        // Verify dirty set and contentChanged called immediately on pen down
        assertTrue(state.document.dirty)
        assertTrue(contentChangedCalled)
        assertTrue(controller.isDrawingActive())

        // Simulate direct app close / lifecycle abort before pen up
        controller.commitInFlightDrawing()
        org.junit.Assert.assertFalse(controller.isDrawingActive())
        assertEquals(1, state.document.pages[0].items.size)
        assertTrue(state.document.dirty)
    }
}
