package com.pnotes.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import com.pnotes.core.tools.Tool

/**
 * The dock's segments and their travelling surface: a typealias onto the generic [SegmentTracker].
 * The dock was the first host of the segmented indicator, and its call sites read better in dock
 * vocabulary — slots, armed, coverage — than in the generic one.
 */
typealias DockSlotBounds = SegmentTracker

/** The dock's tracker local, as the segments report themselves through. */
val LocalDockSlotBounds = LocalSegmentTracker

/** Report a tool slot's on-screen rectangle and armed state to the dock's indicator. */
internal fun Modifier.reportDockSlotBounds(
    bounds: DockSlotBounds?,
    id: String,
    active: Boolean,
): Modifier = reportSegmentBounds(bounds, id, active)

/** How visible a tool's name is, given how much of its slot the travelling box covers. */
internal fun dockLabelReveal(coverage: Float): Float = segmentLabelReveal(coverage)

/**
 * The dock's one active surface: a box that travels from the tool it was on to the tool that was
 * just armed.
 *
 * A thin dock-flavoured wrapper over [SegmentedIndicator] — the dock contributes only its tool-slot
 * mapping ([dockSlotToolFor]) and the app's active-surface styling, which is the indicator's
 * default. Everything else — the churn-free collector, the velocity-preserving retarget, the
 * re-place on a reshaped dock, the coverage handout that lights glyphs and reveals labels as the
 * box passes under them — lives in the generic component, where page tabs and pen swatches can
 * reuse it.
 *
 * Placed in the dock's Box above the material and below the buttons, so it slides beneath the
 * glyphs the way a segmented control's selection does, and inside the dock's own rounded clip,
 * because the dock is the container and a selection that escapes its container stops reading as a
 * selection.
 */
@Composable
internal fun BoxScope.DockActiveIndicator(
    bounds: DockSlotBounds,
    dockBounds: Rect?,
    isVertical: Boolean,
    morphProgress: Float,
) = SegmentedIndicator(
    tracker = bounds,
    containerBounds = dockBounds,
    isVertical = isVertical,
    collapseProgress = morphProgress,
)

/**
 * The armed tool a dock slot shows. Each slot covers a *family* — the pen slot fronts every stroke
 * pen, the select slot both select and lasso — so an armed tool that arrived from another surface's
 * state still lands somewhere honest instead of lighting nothing.
 */
internal fun dockSlotToolFor(tool: Tool): Tool = when (tool) {
    // The pen slot fronts every stroke pen: pen, dashed, speed, calligraphy and taper are all
    // armed from the one dock slot (the dock itself has no per-pen slots — pens live in the
    // pen wheel), so an armed brush or calligraphy pen still lights the pen slot.
    Tool.PEN, Tool.DASHED, Tool.SPEED, Tool.CALLIGRAPHY, Tool.TAPER -> Tool.PEN
    Tool.HIGHLIGHTER -> Tool.HIGHLIGHTER
    Tool.ERASER -> Tool.ERASER
    Tool.SELECT, Tool.LASSO -> Tool.SELECT
    Tool.SHAPE -> Tool.SHAPE
    Tool.TABLE -> Tool.TABLE
    Tool.TEXT, Tool.TEXT_BOX -> Tool.TEXT
    Tool.PAN -> Tool.PAN
    Tool.SCREENSHOT, Tool.IMAGE -> Tool.PEN
}
