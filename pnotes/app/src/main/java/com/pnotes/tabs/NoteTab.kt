package com.pnotes.tabs

import com.pnotes.canvas.ViewOverrides
import com.pnotes.core.history.History
import com.pnotes.core.infinite.InfiniteDocument
import com.pnotes.core.model.Document
import com.pnotes.core.util.DocumentKind
import java.util.UUID

/**
 * Snapshot of a tab's view state (zoom, scroll, page) so switching between tabs
 * restores the exact view without resetting to fit-width or page 1.
 */
data class TabViewState(
    val zoom: Double = 1.0,
    val scrollX: Double = 0.0,
    val scrollY: Double = 0.0,
    val pageIndex: Int = 0,
    val zoomLocked: Boolean = false,
    val viewOverrides: ViewOverrides = ViewOverrides(),
)

/**
 * Representation of a single open tab in PNotes.
 */
data class NoteTab(
    val id: String = UUID.randomUUID().toString(),
    val uri: String? = null,
    val title: String = "Untitled Note",
    val kind: DocumentKind = DocumentKind.NOTE,
    val isDirty: Boolean = false,
    val isAutosaved: Boolean = false,
    val lastActiveTime: Long = System.currentTimeMillis(),
    val viewSnapshot: TabViewState? = null,
)

/**
 * Holds in-memory cached state for an open tab so switching tabs is instant (< 16ms)
 * without re-reading or re-decoding from disk, and preserves the active undo/redo history.
 */
sealed class TabDocumentHolder {
    data class PagedDoc(
        val document: Document,
        val history: History,
        var viewState: TabViewState,
    ) : TabDocumentHolder()

    data class CanvasDoc(
        val document: InfiniteDocument,
        var viewState: TabViewState,
    ) : TabDocumentHolder()
}
