package com.pnotes.ui

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.VelocityTracker
import com.pnotes.canvas.InteractionController
import com.pnotes.canvas.MotionPredictionHelper
import com.pnotes.core.stroke.PredictionAdopt
import com.pnotes.canvas.StylusButtonLatch
import com.pnotes.core.geometry.Pt
import com.pnotes.core.infinite.CanvasViewport
import com.pnotes.canvas.HandleId
import com.pnotes.canvas.ResizeMath
import com.pnotes.canvas.SelectionMath
import com.pnotes.core.geometry.Rect
import com.pnotes.core.infinite.CanvasSelection
import com.pnotes.core.infinite.EraseSession
import com.pnotes.core.infinite.LiftTransform
import com.pnotes.core.infinite.OverlayTessellator
import com.pnotes.core.model.CanvasItem
import com.pnotes.core.model.ImageItem
import com.pnotes.core.model.Rgba
import com.pnotes.core.model.ShapeItem
import com.pnotes.core.model.Stroke
import com.pnotes.core.stroke.Sample
import com.pnotes.core.stroke.ShapeRecognizer
import com.pnotes.core.stroke.StrokeSimplify
import com.pnotes.core.tools.EraseMode
import com.pnotes.core.tools.InkPalette
import com.pnotes.core.tools.ShapeConfig
import com.pnotes.core.tools.ShapeKind
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolConfig
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.max

/** What the current gesture is doing. */
enum class CanvasPointerMode { IDLE, PAN, PINCH, DRAW, ERASE, SHAPE, BAND, LASSO, MOVE, RESIZE, ROTATE }

/**
 * Gestures on the infinite canvas.
 *
 * The navigation half is what an infinite canvas needs before anything else, and it is
 * deliberately the paged canvas's feel rather than a second one: the velocity smoothing, fling
 * friction and start/stop thresholds are read from [InteractionController]'s constants rather than
 * copied, so tuning either canvas tunes both.
 *
 * Everything a pinch changes is two numbers on [CanvasViewport], and the renderer takes them as
 * shader uniforms. There is no cache to invalidate, no settle debounce, and no zoom clamp against
 * a page: zoom runs the full configured range and scroll runs forever in all four directions.
 */
class InfiniteInteraction(
    private val viewport: CanvasViewport,
    private val requestRender: () -> Unit,
    /** Called whenever the view moved, so the host can refresh a zoom readout or schedule a save. */
    private val onViewChanged: () -> Unit = {},
    /** True while a gesture or a glide is live, so the renderer can keep drawing every refresh. */
    private val setInteractive: (Boolean, Boolean) -> Unit = { _, _ -> },
    /** The style the armed tool draws with, including the toolbar's active ink colour. */
    private val configFor: (Tool) -> ToolConfig = { ToolConfig() },
    /** The wet stroke changed: re-tessellate it into the dynamic buffer, or clear it when null. */
    private val onWetStroke: (Stroke?) -> Unit = {},
    /** Pen up on a finished stroke: add it to the document and push the undo command. */
    private val onCommitStroke: (Stroke) -> Unit = {},
    /** Begin an eraser drag; the host owns the document and the undo stack. */
    private val onEraseBegin: () -> EraseSession? = { null },
    /** The eraser drag ended: push its single undo command. */
    private val onEraseEnd: (EraseSession) -> Unit = {},
    /** Where the eraser cursor sits in viewport pixels, and how wide, or null to hide it. */
    private val onEraserCursor: (Pt?, Double) -> Unit = { _, _ -> },
    /** The shape being dragged out, re-tessellated as it grows, or null to clear the preview. */
    private val onPendingShape: (ShapeItem?) -> Unit = {},
    /** A finished shape: add it to the document and push the undo command. */
    private val onCommitShape: (ShapeItem) -> Unit = {},
    /** The shape tool's style, and the active ink colour it draws in. */
    private val shapeConfig: () -> ShapeConfig = { ShapeConfig() },
    private val inkColor: () -> Rgba = { InkPalette.DEFAULT },
    /** Whether a held pen stroke may snap to a recognized shape. */
    private val detectShapes: () -> Boolean = { true },
    /** The selection, owned by the host so the chrome can read it. */
    private val selection: () -> CanvasSelection? = { null },
    /** Items the selection may be drawn from, culled to a rect. */
    private val itemsIn: (Rect) -> List<CanvasItem> = { emptyList() },
    /** The selection or its overlay changed: rebuild the chrome geometry. */
    private val onSelectionChanged: () -> Unit = {},
    /** A finished selection drag: push its single undo command. */
    private val onCommitSelection: (com.pnotes.core.history.Command?) -> Unit = {},
    /** Draw [items] displaced by the drag so far, rather than moving them. An empty list ends it. */
    private val onLiftSelection: (List<CanvasItem>, LiftTransform) -> Unit = { _, _ -> },
    /** Content pixels per dp, so the speed pen judges gesture speed independently of zoom. */
    private val devicePxPerDp: () -> Double = { 1.0 },
    /** A press that landed on the minimap; returns true when it was consumed as navigation. */
    private val onMinimapPress: (Double, Double) -> Boolean = { _, _ -> false },
    /**
     * A finger held still: open the paste menu at this viewport and content point, or offer to
     * release the locked item it landed on, which arrives as the third argument.
     */
    private val onContextMenu: (Pt, Pt, CanvasItem?) -> Unit = { _, _, _ -> },
    /** A tool this layer armed by itself, so the chrome can follow: a long-press grab and its end. */
    private val onToolChanged: (Tool) -> Unit = {},
) {

    private val choreographer = Choreographer.getInstance()

    companion object {
        /** How near a handle a press has to land to grab it, in device pixels. */
        const val HANDLE_TOUCH_PX = 22.0

        /** Least on-screen distance between lasso vertices, so a slow drag stays cheap. */
        const val LASSO_MIN_STEP_PX = 3.0

        /** How far a press may wander and still count as a tap, in device pixels. */
        const val TAP_SLOP_PX = 12.0
    }

    /** The armed tool. */
    var tool: Tool = Tool.PEN

    /** Whether a finger draws, or pans instead. Mirrors the paged canvas's preference. */
    var fingerDraws: Boolean = false

    /** Tool the stylus side button arms while it is held; null leaves the button alone. */
    var penButtonTool: Tool? = Tool.ERASER

    /** Zoom lock: a pinch pans without changing the zoom, mirroring the paged canvas. */
    var zoomLocked: Boolean = false

    /** Disappearing ink is armed, so a held stroke must not become a committed shape. */
    var wandEnabled: Boolean = false

    /** Which pans still move a locked view: "single" (default) | "double" | "none". */
    var zoomLockPan: String = "single"

    private val stylusButtons = StylusButtonLatch()

    private var eraseSession: EraseSession? = null

    // The shape being dragged out with the shape tool.
    private var pendingShape: ShapeItem? = null

    // Band and lasso, in content space, live only while their gesture runs.
    var bandRect: Rect? = null
        private set
    val lassoPoints = ArrayList<Pt>()
    private var bandAnchor = Pt.ZERO

    // The live transform drag.
    private var grabHandle: HandleId? = null
    private var moveAnchor = Pt.ZERO
    private var movedBy = Pt.ZERO

    // Where a handle or the rotate grip is being dragged to, and whether the renderer is doing the
    // work. Ink, shapes and placed SVGs map faithfully in the shader, since all three are triangles.
    // A photo or a text box is rebuilt by the model rather than mapped, so a selection holding one is
    // transformed the slow way and stays honest.
    private var transformPointer = Pt.ZERO
    private var liftedTransform = false

    // A finger press landed off a live selection and became a pan; a tap there dismisses it, a
    // real drag pans and leaves it alone.
    private var panMayDismiss = false
    private var panDownAt = Pt.ZERO

    // A finger held still grabs the item under it, or opens the paste menu on empty canvas.
    private var longPressRunnable: Runnable? = null
    private var longPressAt = Pt.ZERO
    private var longPressCandidate: CanvasItem? = null
    private var longPressLocked: CanvasItem? = null

    // The tool a long-press grab borrowed the canvas from, given back when the selection goes.
    private var longPressPrevTool: Tool? = null

    // Hold-still-to-snap: a freehand stroke that stops moving becomes the shape it looks like.
    private val handler = Handler(Looper.getMainLooper())
    private var dwellRunnable: Runnable? = null
    private var dwellEligible = false
    private var dwellAnchor = Pt.ZERO
    private var lastStrokeMoveTimeMs = 0L

    var mode = CanvasPointerMode.IDLE
        private set

    // The stroke being drawn, live until the pen lifts.
    private var liveStroke: Stroke? = null
    private var drawingPointerId = -1
    private var drawingIsStylus = false
    private var strokeStartTimeMs = 0L

    // --- MOTION PREDICTION (Nebo-style 16 ms look-ahead) ---
    var motionPredictor: MotionPredictionHelper? = null

    // Pan and inertial fling, in viewport px and viewport px/s.
    private var lastPan = Pt.ZERO
    private var lastMoveMs = 0L
    private var panVel = Pt.ZERO
    private var velocityTracker: VelocityTracker? = null
    private var flinging = false
    private var flingVel = Pt.ZERO
    private var lastFlingMs = 0L
    private val flingFrame = Choreographer.FrameCallback { stepFling(it) }

    // Pinch.
    private var pinchInitDist = 1.0
    private var pinchInitZoom = 1.0
    private var pinchAnchorContent = Pt.ZERO
    private var panSlopPassed = false
    var lastStylusEventMs = 0L

    // The predictor's look-ahead tail, in content px (see [generatePredictedTail]).
    private var predictedTail: List<Pt>? = null
    private var predictedPressure = 1.0

    /**
     * The look-ahead the renderer draws past the smoothed ribbon, in content px, newest last.
     * Empty when the predictor is unavailable or has nothing coherent to add. The editor meshes
     * this as a continuation of the wet tail so the ink reaches the nib instead of trailing it
     * by the smoothing filter's residual.
     */
    val predictedTailPoints: List<Pt> get() = predictedTail ?: emptyList()

    fun onTouch(e: MotionEvent): Boolean {
        for (i in 0 until e.pointerCount) {
            val tt = e.getToolType(i)
            if (tt == MotionEvent.TOOL_TYPE_STYLUS || tt == MotionEvent.TOOL_TYPE_ERASER) {
                lastStylusEventMs = android.os.SystemClock.uptimeMillis()
                break
            }
        }
        motionPredictor?.record(e)
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDown(e)
            MotionEvent.ACTION_POINTER_DOWN -> handlePointerDown(e)
            MotionEvent.ACTION_MOVE -> handleMove(e)
            MotionEvent.ACTION_POINTER_UP -> handlePointerUp(e)
            MotionEvent.ACTION_UP -> handleUp(e)
            MotionEvent.ACTION_CANCEL -> abortGesture()
        }
        return true
    }

    /**
     * Latch a side button reported only on the hovering generic-motion stream, which is the only
     * place some pens put it.
     */
    fun onGenericMotion(e: MotionEvent) {
        if (e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS || e.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) {
            lastStylusEventMs = android.os.SystemClock.uptimeMillis()
        }
        stylusButtons.onGenericMotion(e)
    }

    /** Latch a side button delivered as a key event, which is all Bluetooth and USI pens send. */
    fun onStylusButtonKey(keyCode: Int, down: Boolean): Boolean = stylusButtons.onKey(keyCode, down)

    /** Drop any in-flight gesture and stop a glide, so a document swap cannot bleed into the next. */
    fun resetGestureState() {
        stopFling()
        cancelLongPress()
        longPressPrevTool = null
        mode = CanvasPointerMode.IDLE
        panVel = Pt.ZERO
        liveStroke = null
        liftedTransform = false
        predictedTail = null
        onWetStroke(null)
        onLiftSelection(emptyList(), LiftTransform.NONE)
        stylusButtons.reset()
    }

    // --- pointer handling ---

    private fun handleDown(e: MotionEvent) {
        stopFling() // a new touch halts any in-progress glide
        val toolType = e.getToolType(0)
        // 1. Hardware palm tool type rejection (5 is standard TOOL_TYPE_PALM)
        if (toolType == 5) {
            mode = CanvasPointerMode.IDLE
            return
        }
        // 2. Reject accidental large palm contact and stylus writing proximity
        if (toolType == MotionEvent.TOOL_TYPE_FINGER) {
            val d = (devicePxPerDp()).toFloat()
            val major = e.touchMajor
            val minor = e.touchMinor
            val size = e.size
            if (major > 24f * d || (major > 18f * d && minor > 14f * d) || (size > 0.45f && major > 16f * d)) {
                mode = CanvasPointerMode.IDLE
                return
            }
            if ((tool.isStroke || tool == Tool.ERASER || tool == Tool.SHAPE) &&
                android.os.SystemClock.uptimeMillis() - lastStylusEventMs < 1200L
            ) {
                mode = CanvasPointerMode.IDLE
                return
            }
        }
        // The minimap sits over the canvas, so a press on it navigates rather than draws.
        if (onMinimapPress(e.getX(0).toDouble(), e.getY(0).toDouble())) {
            mode = CanvasPointerMode.IDLE
            return
        }
        drawingPointerId = e.getPointerId(0)
        drawingIsStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS
        val vx = e.getX(0).toDouble()
        val vy = e.getY(0).toDouble()

        // Which tool this pointer actually drives: the pen's eraser end and its held side button
        // both override the armed tool, and a finger pans unless finger-draw is on. This mirrors
        // the paged canvas so a pen behaves the same on either surface.
        val buttonHeld = stylusButtons.heldFor(e)
        val onSelection = hitsSelection(viewport.viewportToContent(Pt(vx, vy)))
        val effective: Tool = when {
            toolType == MotionEvent.TOOL_TYPE_ERASER -> Tool.ERASER
            buttonHeld && penButtonTool != null -> penButtonTool!!
            // While something is selected, a press on it grabs it rather than inking through it or restarting lasso,
            // and a finger may grab it even with finger-draw off.
            onSelection && (tool.isStroke || tool == Tool.SHAPE || tool == Tool.LASSO) &&
                toolType != MotionEvent.TOOL_TYPE_FINGER -> Tool.SELECT
            toolType == MotionEvent.TOOL_TYPE_FINGER && onSelection -> Tool.SELECT
            toolType == MotionEvent.TOOL_TYPE_FINGER && !fingerDraws && tool.fingerPansWhenOff -> Tool.PAN
            else -> tool
        }
        // A press that lands off the selection with anything but the selection tools dismisses it,
        // which is what makes an empty tap put the chrome away.
        panMayDismiss = false
        if (!onSelection && effective != Tool.SELECT && effective != Tool.LASSO) {
            if (effective == Tool.PAN) panMayDismiss = hasSelection() else clearSelection()
        }

        armLongPress(Pt(vx, vy), onSelection, toolType == MotionEvent.TOOL_TYPE_FINGER)

        when {
            effective.isStroke -> beginDraw(vx, vy, effective, e)
            effective == Tool.ERASER -> beginErase(vx, vy)
            effective == Tool.SHAPE -> beginShape(vx, vy)
            effective == Tool.SELECT -> beginSelect(vx, vy)
            effective == Tool.LASSO -> beginLasso(vx, vy)
            else -> beginPan(vx, vy)
        }
    }

    // --- long press: grab an item, or the paste menu on empty canvas ---

    /**
     * Arm what a finger held still does: pick up the item under it, or open the paste menu when
     * there is nothing there.
     *
     * Finger only, like the paged canvas: the stylus always draws, so resting it never grabs or pops
     * a menu. A press on the live selection is left alone, because that already means something.
     */
    private fun armLongPress(at: Pt, onSelection: Boolean, isFinger: Boolean) {
        cancelLongPress()
        if (!isFinger || onSelection) return
        val hit = itemAt(viewport.viewportToContent(at))
        // The eraser is the one tool a grab would fight with, so it is the one that leaves an item
        // untouched on a held press. Every other tool (pen/highlighter/shape/pan/select/lasso) picks
        // it up so a user does not have to switch tools just to move something they drew.
        if (hit != null && tool != Tool.ERASER) {
            longPressAt = at
            longPressRunnable = Runnable { grabItem(hit) }
            handler.postDelayed(longPressRunnable!!, InteractionController.LONG_PRESS_MS)
        } else if (hit == null) {
            longPressAt = at
            longPressRunnable = Runnable {
                onContextMenu(at, viewport.viewportToContent(at), null)
                cancelLongPress()
            }
            handler.postDelayed(longPressRunnable!!, InteractionController.LONG_PRESS_MS)
        }
    }

    private fun cancelLongPress() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }

    /**
     * Long press on an item selected it and converted the current gesture into a move; end of
     * gesture will restore the previous tool if this was armed from something else.
     */
    private fun grabItem(item: CanvasItem) {
        val sel = selection() ?: return
        if (tool != Tool.SELECT) {
            longPressPrevTool = tool
            tool = Tool.SELECT
            onToolChanged(Tool.SELECT)
        }
        sel.select(listOf(item))
        setInteractive(false, false)
        beginMoveAt(sel, viewport.viewportToContent(longPressAt))
        onSelectionChanged()
    }

    /**
     * Whether [e] carries a deliberate bare-hand multi-touch gesture: two or more fingers and
     * no pen tip, eraser end or hardware-palm contact among its pointers. Such an event is
     * never a palm, so it must not be dropped by the single-contact heuristics — this is what
     * lets a two-finger scroll or zoom start on the very first attempt.
     */
    private fun isMultiFingerGesture(e: MotionEvent): Boolean {
        var fingers = 0
        for (i in 0 until e.pointerCount) {
            when (e.getToolType(i)) {
                MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_ERASER -> return false
                MotionEvent.TOOL_TYPE_FINGER -> fingers++
            }
        }
        return fingers >= 2
    }

    private fun handlePointerDown(e: MotionEvent) {
        cancelLongPress() // a second finger is a pinch, never a held press
        val actionIdx = e.actionIndex
        val toolType = e.getToolType(actionIdx)
        if (toolType == 5) return

        // Reject secondary palm pointer contacts — but never a deliberate bare-hand
        // multi-finger gesture: two real fingers together must pan/zoom immediately.
        if (toolType == MotionEvent.TOOL_TYPE_FINGER && !isMultiFingerGesture(e)) {
            val d = (devicePxPerDp()).toFloat()
            val major = e.getTouchMajor(actionIdx)
            val minor = e.getTouchMinor(actionIdx)
            val size = e.getSize(actionIdx)
            if (major > 24f * d || (major > 18f * d && minor > 14f * d) || (size > 0.45f && major > 16f * d)) {
                return
            }
            if (android.os.SystemClock.uptimeMillis() - lastStylusEventMs < 1200L) {
                return
            }
        }

        // Stylus preemption: if palm touched first and started a finger pan or pinch,
        // and the stylus tip now lands, preempt and abort the pan/pinch immediately to ink!
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            if (mode == CanvasPointerMode.PAN || mode == CanvasPointerMode.PINCH) {
                abortGesture()
                val vx = e.getX(actionIdx).toDouble()
                val vy = e.getY(actionIdx).toDouble()
                drawingPointerId = e.getPointerId(actionIdx)
                drawingIsStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS
                beginDraw(vx, vy, tool, e)
                return
            }
        }

        // A stylus stroke ignores an incidental palm or second finger; a finger stroke yields to a
        // pinch, since two fingers can only mean a zoom.
        if (mode == CanvasPointerMode.DRAW && drawingIsStylus) return
        if (mode == CanvasPointerMode.ERASE && drawingIsStylus) return
        if (e.pointerCount >= 2) {
            if (mode == CanvasPointerMode.DRAW) abandonStroke()
            // A finger erase yields to a pinch: commit what it already removed rather than lose it.
            if (mode == CanvasPointerMode.ERASE) endErase()
            beginPinch(e)
        }
    }

    private fun handleMove(e: MotionEvent) {
        // A finger that wanders is panning or drawing, not holding still for the menu.
        if (longPressRunnable != null) {
            val moved = Pt(e.getX(0).toDouble(), e.getY(0).toDouble()).distanceTo(longPressAt)
            if (moved > InteractionController.LONG_PRESS_SLOP) cancelLongPress()
        }
        when (mode) {
            CanvasPointerMode.PAN -> extendPan(e.getX(0).toDouble(), e.getY(0).toDouble())
            CanvasPointerMode.PINCH -> updatePinch(e)
            CanvasPointerMode.DRAW -> extendDraw(e)
            CanvasPointerMode.ERASE -> extendErase(e)
            CanvasPointerMode.SHAPE -> extendShape(e.getX(0).toDouble(), e.getY(0).toDouble())
            CanvasPointerMode.BAND -> extendBand(e.getX(0).toDouble(), e.getY(0).toDouble())
            CanvasPointerMode.LASSO -> extendLasso(e.getX(0).toDouble(), e.getY(0).toDouble())
            CanvasPointerMode.MOVE -> extendMove(e.getX(0).toDouble(), e.getY(0).toDouble())
            CanvasPointerMode.RESIZE -> extendResize(e.getX(0).toDouble(), e.getY(0).toDouble())
            CanvasPointerMode.ROTATE -> extendRotate(e.getX(0).toDouble(), e.getY(0).toDouble())
            CanvasPointerMode.IDLE -> Unit
        }
    }

    private fun handlePointerUp(e: MotionEvent) {
        val upIdx = e.actionIndex
        val upId = e.getPointerId(upIdx)
        if (mode == CanvasPointerMode.DRAW && upId == drawingPointerId) {
            endDraw(e)
            mode = CanvasPointerMode.IDLE
            drawingPointerId = -1
            return
        }
        if (mode == CanvasPointerMode.ERASE && upId == drawingPointerId) {
            endErase()
            mode = CanvasPointerMode.IDLE
            drawingPointerId = -1
            return
        }
        if (mode == CanvasPointerMode.PAN && upId == drawingPointerId) {
            mode = CanvasPointerMode.IDLE
            drawingPointerId = -1
            return
        }
        if (mode == CanvasPointerMode.PINCH) {
            mode = CanvasPointerMode.IDLE
        }
    }

    private fun handleUp(e: MotionEvent) {
        cancelLongPress()
        val wasMoving = (mode == CanvasPointerMode.PAN && singleFingerPanAllowed()) ||
            (mode == CanvasPointerMode.PINCH && pinchPanAllowed())
        // A finger tap off the selection puts it away; a tap is the only way to say so with a
        // finger, since a drag there is a pan.
        if (mode == CanvasPointerMode.PAN && panMayDismiss &&
            panWasTap(e.getX(0).toDouble(), e.getY(0).toDouble())
        ) {
            clearSelection()
        }
        panMayDismiss = false
        if (mode == CanvasPointerMode.DRAW) endDraw(e)
        if (mode == CanvasPointerMode.ERASE) endErase()
        if (mode == CanvasPointerMode.SHAPE) endShape()
        when (mode) {
            CanvasPointerMode.BAND -> endBand()
            CanvasPointerMode.LASSO -> endLasso()
            CanvasPointerMode.MOVE -> endMove()
            CanvasPointerMode.RESIZE, CanvasPointerMode.ROTATE -> endTransform()
            else -> Unit
        }
        mode = CanvasPointerMode.IDLE
        setInteractive(false, true)
        val now = System.nanoTime() / 1_000_000L
        val tracker = velocityTracker
        val (trackVx, trackVy) = if (tracker != null) {
            tracker.computeCurrentVelocity(1000, 16000f)
            val vx = tracker.getXVelocity(0).toDouble()
            val vy = tracker.getYVelocity(0).toDouble()
            tracker.recycle()
            velocityTracker = null
            Pair(vx, vy)
        } else Pair(0.0, 0.0)

        val trackerVel = Pt(trackVx, trackVy)
        val velocity = if (panMayDismiss && panWasTap(e.getX(0).toDouble(), e.getY(0).toDouble())) {
            Pt.ZERO
        } else if (trackerVel.length() >= panVel.length()) {
            trackerVel
        } else if (now - lastMoveMs < 250L) {
            panVel
        } else {
            trackerVel
        }
        if (wasMoving) startFling(velocity)
        onViewChanged()
        // Once more now the gesture has settled, so chrome that only shows on a still selection
        // gets its chance; during the drag the mode was still MOVE.
        onSelectionChanged()
        requestRender()
    }

    /** Commit any in-flight live stroke (e.g. on app close, task swipe, or lifecycle pause). */
    fun commitInFlightDrawing() {
        if (mode == CanvasPointerMode.DRAW) {
            val stroke = liveStroke
            liveStroke = null
            predictedTail = null
            onWetStroke(null)
            if (stroke != null && !stroke.isEmpty) {
                stroke.finishWet()
                stroke.wetRibbon?.finishFlick()
                stroke.finished = true
                simplifyForCommit(stroke)
                onCommitStroke(stroke)
            }
            mode = CanvasPointerMode.IDLE
        }
    }

    private fun abortGesture() {
        cancelLongPress()
        velocityTracker?.recycle()
        velocityTracker = null
        if (mode == CanvasPointerMode.DRAW) commitInFlightDrawing()
        if (mode == CanvasPointerMode.ERASE) endErase()
        if (mode == CanvasPointerMode.SHAPE) abandonShape()
        // A cancelled drag never happened: the model was never touched, so putting the box back and
        // dropping the lift is the whole undo.
        if (mode == CanvasPointerMode.MOVE) {
            selection()?.previewMove(0.0, 0.0)
            onLiftSelection(emptyList(), LiftTransform.NONE)
            onSelectionChanged()
        }
        if ((mode == CanvasPointerMode.ROTATE || mode == CanvasPointerMode.RESIZE) && liftedTransform) {
            selection()?.previewBack()
            onLiftSelection(emptyList(), LiftTransform.NONE)
            liftedTransform = false
            onSelectionChanged()
        }
        mode = CanvasPointerMode.IDLE
        setInteractive(false, true)
        stopFling()
        requestRender()
    }

    // --- drawing ---

    private fun beginDraw(vx: Double, vy: Double, drawTool: Tool, e: MotionEvent) {
        val base = configFor(drawTool)
        // SCALE off: divide the width by the draw-time zoom so the stroke keeps a constant
        // on-screen thickness whatever zoom it was drawn at. Baked in, so it is ordinary ink after.
        val z = viewport.zoom
        val config = if (base.scale) {
            base
        } else {
            base.copy(
                baseWidth = base.baseWidth / z,
                dashLength = base.dashLength / z,
                dashGap = base.dashGap / z,
                scale = true,
            )
        }
        val straight = drawTool == Tool.HIGHLIGHTER && config.straightLine
        val stabScale = InteractionController.stabilityScale(config.stability)
        val stroke = Stroke(
            drawTool, config,
            speedScale = z / devicePxPerDp().coerceAtLeast(1e-9),
            straight = straight,
            smoothScale = InteractionController.smoothScaleFor(z) * stabScale,
            lagComp = InteractionController.lagCompensation(config.stability),
        )
        // Live until the pen lifts, so lift-time rules cannot fire mid-draw.
        stroke.finished = false
        // Inking wants the shortest path from nib to pixel, so it draws on demand: a render thread
        // left running keeps the buffer queue full and puts a frame of lag under the pen.
        setInteractive(false, false)
        strokeStartTimeMs = e.eventTime
        val p = viewport.viewportToContent(Pt(vx, vy))
        stroke.addSample(Sample(p.x, p.y, pressureOf(e, 0)))
        liveStroke = stroke
        predictedTail = null
        mode = CanvasPointerMode.DRAW
        // Only solid pens arm the snap: a highlighter or a straight-line drag never becomes a shape.
        dwellEligible = detectShapes() && drawTool.isStroke && drawTool != Tool.HIGHLIGHTER &&
            !straight && !wandEnabled
        if (dwellEligible) {
            lastStrokeMoveTimeMs = android.os.SystemClock.uptimeMillis()
            armDwell(Pt(vx, vy))
        }
        onWetStroke(stroke)
        requestRender()
    }

    private fun extendDraw(e: MotionEvent) {
        val idx = e.findPointerIndex(drawingPointerId)
        if (idx < 0) return
        // Historical points first: the digitizer batches several samples into one event, and
        // dropping them coarsens a fast stroke into visible chords.
        for (h in 0 until e.historySize) {
            addStrokePoint(
                e.getHistoricalX(idx, h).toDouble(),
                e.getHistoricalY(idx, h).toDouble(),
                if (drawingIsStylus) e.getHistoricalPressure(idx, h).toDouble() else 1.0,
                stylusAxis(e, MotionEvent.AXIS_TILT, idx, h),
                stylusAxis(e, MotionEvent.AXIS_ORIENTATION, idx, h),
                e.getHistoricalEventTime(h),
                force = false,
            )
        }
        addStrokePoint(
            e.getX(idx).toDouble(), e.getY(idx).toDouble(),
            pressureOf(e, idx),
            stylusAxis(e, MotionEvent.AXIS_TILT, idx, -1), stylusAxis(e, MotionEvent.AXIS_ORIENTATION, idx, -1),
            e.eventTime, force = false,
        )
        // Prediction first: the publish below meshes the tail extension, so asking for it after
        // the publish would draw this frame with the previous frame's look-ahead.
        generatePredictedTail()
        onWetStroke(liveStroke)
        // Real movement restarts the clock; holding within the slop lets it mature, so the snap
        // fires only once the pen has actually come to rest.
        if (dwellEligible) {
            val here = Pt(e.getX(idx).toDouble(), e.getY(idx).toDouble())
            if (here.distanceTo(dwellAnchor) > 3.0) {
                lastStrokeMoveTimeMs = android.os.SystemClock.uptimeMillis()
                armDwell(here)
            }
        }
        requestRender()
    }

    /**
     * Ask the motion predictor for 1-2 frames of look-ahead and keep it in viewport space as the
     * ephemeral tail the renderer draws ahead of the wet ink. Ported from InteractionController
     * (Phase 4 of the smoothness plan): without this the recorded predictor was never queried on
     * this editor, so the tip lags the hand by the whole input-to-render pipeline.
     */
    private fun generatePredictedTail() {
        val stroke = liveStroke ?: return
        if (stroke.straight) return // straight snap; prediction would fight it
        val n = stroke.sampleCount
        if (n < 1) {
            predictedTail = null
            return
        }

        var predicted = motionPredictor?.predict(drawingIsStylus)

        if ((predicted == null || predicted.isEmpty()) && n >= 2) {
            val lastX = stroke.xAt(n - 1)
            val lastY = stroke.yAt(n - 1)
            val prevX = stroke.xAt(n - 2)
            val prevY = stroke.yAt(n - 2)
            val dx = lastX - prevX
            val dy = lastY - prevY
            val d = kotlin.math.hypot(dx, dy)
            if (d > (0.1 / viewport.zoom)) {
                val step = (d * 0.8).coerceIn(0.5 / viewport.zoom, 15.0 / viewport.zoom)
                val dirX = dx / d
                val dirY = dy / d
                val p1 = Pt(lastX + dirX * step, lastY + dirY * step)
                val p2 = Pt(lastX + dirX * (step * 1.8), lastY + dirY * (step * 1.8))
                setPredictedTail(stroke, listOf(p1, p2), 0.0)
                return
            }
        }

        if (predicted.isNullOrEmpty()) {
            predictedTail = null
            return
        }
        val points = predicted.map { viewport.viewportToContent(Pt(it.x, it.y)) }
        // Direction sanity check, as on the paged canvas: a sharp turn or U-turn makes a
        // straight-line look-ahead cut the corner or overshoot the apex, and the pen-up adoption
        // would then keep the overshoot as ink. Drop the tail for that frame instead.
        if (!continuesStroke(stroke, points)) {
            predictedTail = null
            return
        }
        setPredictedTail(stroke, points, predicted.last().pressure)
    }

    /**
     * The look-ahead tail, trimmed once here and then shared by everybody who needs it.
     *
     * The editor meshes it and the pen-up commits it, so the two must read the same list: a tail
     * that is drawn and then quietly shortened on the way into the model is ink that disappears at
     * the moment the pen lifts. The trim is measured from the live ribbon's tip — the point the
     * tail is actually drawn from — and it is the same rule the mesh's own trim uses, so running
     * it twice changes nothing.
     */
    private fun setPredictedTail(stroke: Stroke, points: List<Pt>, lastPressure: Double) {
        val ribbon = stroke.wetRibbon
        var anchorX = 0.0
        var anchorY = 0.0
        if (ribbon != null && ribbon.pointCount > 0) {
            anchorX = ribbon.cx(ribbon.pointCount - 1)
            anchorY = ribbon.cy(ribbon.pointCount - 1)
        } else {
            val n = stroke.sampleCount
            if (n > 0) {
                anchorX = stroke.xAt(n - 1)
                anchorY = stroke.yAt(n - 1)
            }
        }
        val trimmed = PredictionAdopt.trim(
            points, anchorX, anchorY,
            minStep = PredictionAdopt.MIN_TAIL_STEP_PX / viewport.zoom,
            maxDist = PredictionAdopt.MAX_TAIL_PX / viewport.zoom,
        )
        if (trimmed.isEmpty()) {
            predictedTail = null
            return
        }
        predictedTail = trimmed
        val lastIdx = stroke.sampleCount - 1
        predictedPressure =
            if (lastPressure > 0.05) lastPressure else (if (lastIdx >= 0) stroke.pAt(lastIdx) else 1.0)
    }

    /**
     * Whether [tail] still travels the way the stroke was going. The first predicted point is
     * compared with the stroke's last chord; [minDot] is looser for a chord shorter than ~12 screen
     * px, where the heading is mostly digitizer noise rather than a real direction.
     */
    private fun continuesStroke(stroke: Stroke, tail: List<Pt>): Boolean {
        val n = stroke.sampleCount
        if (n < 2 || tail.isEmpty()) return true
        val lastX = stroke.xAt(n - 1)
        val lastY = stroke.yAt(n - 1)
        val curDx = lastX - stroke.xAt(n - 2)
        val curDy = lastY - stroke.yAt(n - 2)
        val curLen = kotlin.math.hypot(curDx, curDy)
        val predDx = tail.first().x - lastX
        val predDy = tail.first().y - lastY
        val predLen = kotlin.math.hypot(predDx, predDy)
        if (curLen <= 1e-4 || predLen <= 1e-4) return true
        val dot = (curDx * predDx + curDy * predDy) / (curLen * predLen)
        val minDot = if (curLen < 12.0 / viewport.zoom) -0.2 else 0.1
        return dot >= minDot
    }

    /**
     * Commit the predictor's look-ahead tail into [stroke] at pen-up: the overlay drew that tip
     * every frame, so discarding it on lift retracts the ink to the last real sample in front of
     * the writer.
     *
     * It is committed **verbatim**: the tail that is drawn and the tail that lands are the same
     * points, so none of the ink the writer watched can go missing on the way into the note. The
     * tail's samples are ordinary ink from here on — the eraser, the selection, the undo stack and
     * the file format all see them like any other sample — but they are marked as the tip
     * ([Stroke.tipFrom]) so the committed geometry pins them to their raw positions instead of
     * smoothing them away. A causal low-pass trails its input by about one smoothing length, and a
     * smoothed tail comes out that much shorter than the tail on screen: the line would retract by
     * roughly 4 px at the default stability, which is the very thing this whole path exists to stop.
     */
    private fun adoptPredictedTail(stroke: Stroke, up: Pt) {
        if (stroke.straight) return
        val tail = predictedTail
        predictedTail = null
        val n = stroke.sampleCount
        if (n < 1 || tail.isNullOrEmpty()) return

        val samples = PredictionAdopt.adopt(tail, predictedPressure, stroke.tAt(n - 1))
        if (samples.isEmpty()) return
        stroke.tipCount = samples.size
        // PredictionAdopt returns Samples already in stroke space; add them verbatim.
        for (s in samples) stroke.addSample(s)
    }

    /**
     * One stylus axis read at pointer [index] (axis [h] of the historical batch when [h] is not
     * negative), or 0 when the input reports none. [axis] is `AXIS_TILT` or `AXIS_ORIENTATION`; both
     * say which way the writer holds the pen, and both are gone if they are not read here.
     */
    private fun stylusAxis(e: MotionEvent, axis: Int, index: Int, h: Int): Double {
        if (!drawingIsStylus) return 0.0
        return runCatching {
            if (h >= 0) {
                e.getHistoricalAxisValue(axis, index, h).toDouble()
            } else {
                e.getAxisValue(axis, index).toDouble()
            }
        }.getOrDefault(0.0)
    }

    private fun addStrokePoint(
        vx: Double,
        vy: Double,
        pressure: Double,
        tilt: Double,
        azimuth: Double,
        timeMs: Long,
        force: Boolean,
    ) {
        val stroke = liveStroke ?: return
        val p = viewport.viewportToContent(Pt(vx, vy))
        val t = (timeMs - strokeStartTimeMs).toDouble()
        if (stroke.straight) {
            stroke.setStraightEnd(Sample(p.x, p.y, pressure.coerceIn(0.0, 1.0), t, tilt, azimuth))
            return
        }
        val last = stroke.samples.lastOrNull()
        val count = stroke.sampleCount
        val isDirectionalTurn = if (count >= 2) {
            val v0x = stroke.xAt(count - 1) - stroke.xAt(count - 2)
            val v0y = stroke.yAt(count - 1) - stroke.yAt(count - 2)
            val v1x = p.x - stroke.xAt(count - 1)
            val v1y = p.y - stroke.yAt(count - 1)
            val l0 = kotlin.math.hypot(v0x, v0y)
            val l1 = kotlin.math.hypot(v1x, v1y)
            if (l0 > 1e-4 && l1 > 1e-4) {
                val cosAngle = (v0x * v1x + v0y * v1y) / (l0 * l1)
                cosAngle < 0.90 // Directional turn > 25°
            } else false
        } else false

        // Decimate by on-screen spacing rather than content spacing, so drawing while zoomed in
        // keeps its detail instead of faceting into chords a zoom factor long.
        val gate = if (isDirectionalTurn) 0.0 else maxOf(
            (InteractionController.MIN_SAMPLE_DIST / viewport.zoom)
                .coerceAtMost(InteractionController.MIN_SAMPLE_DIST),
            InteractionController.noiseGatePx(stroke.config.stability) / viewport.zoom,
        )
        if (force || isDirectionalTurn || last == null || Pt(last.x, last.y).manhattanTo(p) >= gate) {
            stroke.addSample(Sample(p.x, p.y, pressure.coerceIn(0.0, 1.0), t, tilt, azimuth))
        }
    }

    private fun endDraw(e: MotionEvent) {
        cancelDwell()
        dwellEligible = false
        val idx = e.findPointerIndex(drawingPointerId).coerceAtLeast(0)
        // Digitizers often report a pressure dip (or an outright 0) on the UP event itself. The
        // tip's real release pressure is the last MOVE's, so floor the forced pen-up sample at it —
        // otherwise the EMA drags a narrow spike into the last few widths and the ink pinches at
        // the exact moment the writer judges their line ending (Star Notes rejects the same dip).
        val stroke0 = liveStroke
        val lastMoveP = stroke0?.takeIf { it.sampleCount > 0 }?.pAt(stroke0.sampleCount - 1)
        val upPressure = if (drawingIsStylus) {
            maxOf(pressureOf(e, idx), lastMoveP ?: 0.0)
        } else {
            1.0
        }
        addStrokePoint(
            e.getX(idx).toDouble(), e.getY(idx).toDouble(),
            upPressure,
            stylusAxis(e, MotionEvent.AXIS_TILT, idx, -1), stylusAxis(e, MotionEvent.AXIS_ORIENTATION, idx, -1),
            e.eventTime, force = true,
        )
        val stroke = liveStroke
        liveStroke = null
        if (stroke == null || stroke.isEmpty) {
            predictedTail = null
            onWetStroke(null)
            return
        }
        // Keep the tip the writer was already looking at: the predicted tail becomes real ink
        // before the commit, so the line never retracts on lift.
        adoptPredictedTail(stroke, Pt(e.getX(idx).toDouble(), e.getY(idx).toDouble()))
        // The ribbon still holds the ink the writer saw: flush the spline pump's last segment
        // into it and latch the end-velocity flick BEFORE the stroke is finished. Finishing a
        // stroke invalidates it, and an invalidated stroke has no ribbon left to write into —
        // so a flush after that point is a no-op on a buffer that is already gone.
        stroke.finishWet()
        stroke.wetRibbon?.finishFlick()
        // The pen is up: rebuild with lift-time rules on before the stroke is committed. The wet
        // buffer is deliberately not cleared here: the commit releases it in the same step, so no
        // frame can land between the two and blink.
        stroke.finished = true
        simplifyForCommit(stroke)
        onCommitStroke(stroke)
    }

    // --- selection ---

    fun hasSelection(): Boolean = selection()?.isEmpty == false

    /**
     * Whether [at] grabs the settled selection: its rotate grip, one of its handles, or its body.
     * A press that does is routed to the selection whatever tool is armed and whatever is touching,
     * which is what lets a finger drag a selection while finger-draw is off.
     */
    private fun hitsSelection(at: Pt): Boolean {
        val sel = selection() ?: return false
        if (sel.isEmpty) return false
        val tolerance = HANDLE_TOUCH_PX / viewport.zoom
        val grip = sel.rotateGrip(OverlayTessellator.GRIP_ARM_PX / viewport.zoom)
        if (grip != null && at.distanceTo(grip) <= tolerance) return true
        if (sel.hitHandle(at, tolerance) != null) return true
        if (sel.contains(at)) return true
        val b = sel.box
        if (b != null) {
            val local = b.worldToLocal(at)
            if (kotlin.math.abs(local.x) <= b.halfW + tolerance && kotlin.math.abs(local.y) <= b.halfH + tolerance) {
                return true
            }
        }
        for (item in sel.items) {
            if (item.contains(at) || item.bounds().outset(tolerance).contains(at)) return true
        }
        return false
    }

    /** Clear the selection and its chrome, for a tool change or a document swap. */
    fun clearSelection() {
        // A long-press grab only borrowed the selection tool; putting the selection away returns it.
        longPressPrevTool?.let {
            longPressPrevTool = null
            tool = it
            onToolChanged(it)
        }
        selection()?.clear()
        bandRect = null
        lassoPoints.clear()
        onLiftSelection(emptyList(), LiftTransform.NONE)
        onSelectionChanged()
        requestRender()
    }

    private fun beginSelect(vx: Double, vy: Double) {
        val sel = selection() ?: return
        val at = viewport.viewportToContent(Pt(vx, vy))
        setInteractive(false, false)
        // A press on a handle or the grip transforms; inside the box it moves; anywhere else
        // starts a fresh band, which is what makes an empty patch of canvas deselect.
        if (!sel.isEmpty) {
            val tolerance = HANDLE_TOUCH_PX / viewport.zoom
            val grip = sel.rotateGrip(OverlayTessellator.GRIP_ARM_PX / viewport.zoom)
            if (grip != null && at.distanceTo(grip) <= tolerance) {
                sel.beginTransform(at)
                beginLiftedTransform(sel, at)
                mode = CanvasPointerMode.ROTATE
                return
            }
            val handle = sel.hitHandle(at, tolerance)
            if (handle != null) {
                grabHandle = handle
                sel.beginTransform()
                beginLiftedTransform(sel, at)
                mode = CanvasPointerMode.RESIZE
                return
            }
            if (sel.contains(at)) {
                beginMoveAt(sel, at)
                return
            }
        }
        // A press that lands on an item picks that item up, as it does on a note. A band is what
        // the empty canvas between items starts, not what selecting anything at all takes. A locked
        // item is not there as far as this is concerned, so a band can be started over one.
        val hit = grabbableAt(at)
        if (hit != null) {
            if (sel.items.none { it === hit }) sel.select(listOf(hit))
            beginMoveAt(sel, at)
            onSelectionChanged()
            return
        }
        sel.clear()
        bandAnchor = at
        bandRect = Rect(at.x, at.y, 0.0, 0.0)
        mode = CanvasPointerMode.BAND
        onSelectionChanged()
    }

    /** Start dragging the selection from [at], the tail of every press that grabs one. */
    private fun beginMoveAt(sel: CanvasSelection, at: Pt) {
        sel.beginTransform()
        moveAnchor = at
        movedBy = Pt.ZERO
        mode = CanvasPointerMode.MOVE
        onLiftSelection(sel.items, LiftTransform.NONE)
    }

    /**
     * The topmost item under [content], or null. The index is queried with a slop-sized rect, but
     * whether the press really landed on something is the item's own answer.
     */
    private fun itemAt(content: Pt): CanvasItem? {
        val pad = maxOf(8.0, TAP_SLOP_PX / viewport.zoom)
        val near = Rect(content.x - pad, content.y - pad, pad * 2, pad * 2)
        // Last, not first: the index comes back in z-order, so the topmost item is the one hit.
        return itemsIn(near).lastOrNull { it.contains(content) || it.intersectsCircle(content.x, content.y, pad) }
    }

    /** [itemAt] but skipping anything pinned, which is what a tap or a drag may actually grab. */
    private fun grabbableAt(content: Pt): CanvasItem? = itemAt(content)?.takeUnless { it.locked }

    private fun extendBand(vx: Double, vy: Double) {
        val at = viewport.viewportToContent(Pt(vx, vy))
        bandRect = Rect.fromPoints(bandAnchor, at)
        onSelectionChanged()
        requestRender()
    }

    private fun endBand() {
        val sel = selection()
        val rect = bandRect
        bandRect = null
        if (sel != null && rect != null && (rect.w > 1e-6 || rect.h > 1e-6)) {
            sel.select(SelectionMath.bandMembers(itemsIn(rect), rect))
        }
        onSelectionChanged()
        requestRender()
    }

    private fun beginLasso(vx: Double, vy: Double) {
        val sel = selection() ?: return
        val at = viewport.viewportToContent(Pt(vx, vy))
        if (hitsSelection(at)) {
            beginMoveAt(sel, at)
            return
        }
        setInteractive(false, false)
        sel.clear()
        lassoPoints.clear()
        lassoPoints.add(at)
        mode = CanvasPointerMode.LASSO
        onSelectionChanged()
    }

    private fun extendLasso(vx: Double, vy: Double) {
        val at = viewport.viewportToContent(Pt(vx, vy))
        val last = lassoPoints.lastOrNull()
        // Decimate by on-screen spacing, so a slow drag does not pile up thousands of vertices.
        if (last == null || last.distanceTo(at) * viewport.zoom >= LASSO_MIN_STEP_PX) lassoPoints.add(at)
        onSelectionChanged()
        requestRender()
    }

    private fun endLasso() {
        val sel = selection()
        if (sel != null) {
            if (lassoPoints.size >= 3) {
                val bounds = Rect.bounding(lassoPoints)
                sel.select(SelectionMath.lassoMembers(itemsIn(bounds), lassoPoints.toList()))
            } else if (lassoPoints.isNotEmpty()) {
                val hit = grabbableAt(lassoPoints.first())
                if (hit != null) {
                    sel.select(listOf(hit))
                } else {
                    sel.clear()
                }
            }
        }
        lassoPoints.clear()
        onSelectionChanged()
        requestRender()
    }

    /**
     * A drag in progress. The model is left exactly where it was and the renderer is told to draw
     * the selection offset, so a drag costs one uniform however much is selected. The box follows
     * so the chrome tracks the finger.
     */
    private fun extendMove(vx: Double, vy: Double) {
        val sel = selection() ?: return
        val at = viewport.viewportToContent(Pt(vx, vy))
        movedBy = Pt(at.x - moveAnchor.x, at.y - moveAnchor.y)
        sel.previewMove(movedBy.x, movedBy.y)
        onLiftSelection(sel.items, LiftTransform.shift(movedBy.x, movedBy.y))
        onSelectionChanged()
        requestRender()
    }

    /** Finger up: apply the whole move to the model once, then hand the drawing back to it. */
    private fun endMove() {
        val sel = selection() ?: return
        sel.moveLive(movedBy.x, movedBy.y)
        onLiftSelection(emptyList(), LiftTransform.NONE)
        onCommitSelection(sel.buildCommand(movedOnly = true, dx = movedBy.x, dy = movedBy.y))
        onSelectionChanged()
        requestRender()
    }

    /**
     * Arm a handle or grip drag to be drawn by the renderer rather than baked by the model, when
     * everything selected maps faithfully. Ink and shapes do: their geometry is mapped through the
     * transform, which is exactly what the shader does. A placed SVG does too, since it is drawn
     * from triangles rather than from a texture, and it has the most to gain: baking would re-mesh
     * the whole drawing on every pointer sample. A photo or a text box is rebuilt instead of mapped,
     * so those keep the slow path and stay honest.
     */
    private fun beginLiftedTransform(sel: CanvasSelection, at: Pt) {
        transformPointer = at
        liftedTransform = sel.items.all { it is Stroke || it is ShapeItem || isVectorImage(it) }
        if (liftedTransform) onLiftSelection(sel.items, LiftTransform.NONE)
    }

    private fun isVectorImage(item: CanvasItem): Boolean =
        item is ImageItem && com.pnotes.platform.ImageDecoder.isVector(item.image.file.path)

    /**
     * A resize in progress. The model is left alone and the renderer is handed the map, so a handle
     * drag costs a few uniforms however much is selected.
     */
    private fun extendResize(vx: Double, vy: Double) {
        val sel = selection() ?: return
        val handle = grabHandle ?: return
        val at = viewport.viewportToContent(Pt(vx, vy))
        transformPointer = at
        if (liftedTransform) {
            val map = sel.previewResize(handle, at) ?: return
            onLiftSelection(sel.items, LiftTransform.of(map, sel.transformPivot ?: Pt.ZERO))
        } else {
            sel.resizeLive(handle, at)
        }
        onSelectionChanged()
        requestRender()
    }

    /** A turn in progress, drawn the same way a resize is. */
    private fun extendRotate(vx: Double, vy: Double) {
        val sel = selection() ?: return
        val at = viewport.viewportToContent(Pt(vx, vy))
        transformPointer = at
        if (liftedTransform) {
            val swept = sel.previewRotate(at)
            onLiftSelection(sel.items, LiftTransform.turn(sel.transformPivot ?: Pt.ZERO, swept))
        } else {
            sel.rotateLive(at)
        }
        onSelectionChanged()
        requestRender()
    }

    private fun endTransform() {
        val sel = selection() ?: return
        val handle = grabHandle
        val wasResize = handle != null
        grabHandle = null
        // Finger up on a lifted drag: apply the whole thing to the model once, then hand the drawing
        // back to it.
        if (liftedTransform) {
            if (handle != null) sel.resizeLive(handle, transformPointer)
            else sel.rotateLive(transformPointer)
            onLiftSelection(emptyList(), LiftTransform.NONE)
            liftedTransform = false
        }
        onCommitSelection(sel.buildCommand(movedOnly = false))
        // A resize of an upright box snaps onto the real bounds, which is what keeps the chrome
        // honest around a text box that refused to shrink past its own text. A turned box cannot be
        // re-derived at all: item bounds are axis aligned, so measuring one and then tilting the
        // result grew the box by its own rotation and made every release jump.
        val upright = sel.box?.let { kotlin.math.abs(it.angle) < 1e-9 } == true
        if (wasResize && upright) sel.refreshBox()
        onSelectionChanged()
        requestRender()
    }

    // --- shapes ---

    private fun beginShape(vx: Double, vy: Double) {
        val cfg = shapeConfig()
        val at = viewport.viewportToContent(Pt(vx, vy))
        val ink = inkColor()
        val fill = if (cfg.fill && cfg.shape.isClosed) ink.scaleAlpha(cfg.fillAlpha) else null
        pendingShape = ShapeItem(
            cfg.shape, at, at, ink, cfg.strokeWidth * InteractionController.SHAPE_PEN_PARITY, fill,
            cfg.neon, cfg.neonStrength,
            dashed = cfg.dashed, dashLength = cfg.dashLength, dashGap = cfg.dashGap,
        )
        mode = CanvasPointerMode.SHAPE
        setInteractive(false, false)
        onPendingShape(pendingShape)
        requestRender()
    }

    private fun extendShape(vx: Double, vy: Double) {
        val shape = pendingShape ?: return
        val raw = viewport.viewportToContent(Pt(vx, vy))
        shape.end = when {
            // Line and arrow pin flat when the dragged end lands near an axis.
            shape.shape == ShapeKind.LINE || shape.shape == ShapeKind.ARROW -> snapAxisEndpoint(shape.start, raw)
            // Circle keeps its box square, so it stays a circle rather than becoming an ellipse.
            shape.shape == ShapeKind.CIRCLE -> squareCorner(shape.start, raw)
            else -> raw
        }
        if (shape.shape == ShapeKind.POLYLINE || shape.shape == ShapeKind.CURVE) {
            val pts = when (shape.shape) {
                ShapeKind.POLYLINE -> ShapeItem.generatePolylinePoints(shape.start, shape.end)
                ShapeKind.CURVE -> ShapeItem.generateCurvePoints(shape.start, shape.end)
                else -> null
            }
            if (pts != null) {
                shape.points = ShapeItem.normalize(pts, shape.box)
            }
        }
        onPendingShape(shape)
        requestRender()
    }

    private fun endShape() {
        val shape = pendingShape
        pendingShape = null
        onPendingShape(null)
        // A tap makes no shape; only a real drag commits one.
        if (shape != null && shape.start.distanceTo(shape.end) > InteractionController.SHAPE_MIN_DRAG) {
            if (shape.shape == ShapeKind.POLYLINE || shape.shape == ShapeKind.CURVE) {
                val pts = when (shape.shape) {
                    ShapeKind.POLYLINE -> ShapeItem.generatePolylinePoints(shape.start, shape.end)
                    ShapeKind.CURVE -> ShapeItem.generateCurvePoints(shape.start, shape.end)
                    else -> null
                }
                if (pts != null) {
                    shape.points = ShapeItem.normalize(pts, shape.box)
                }
            }
            onCommitShape(shape)
        }
        requestRender()
    }

    private fun abandonShape() {
        pendingShape = null
        onPendingShape(null)
    }

    /** Constrain a dragged corner to a square box anchored at [anchor], for the perfect circle. */
    private fun squareCorner(anchor: Pt, p: Pt): Pt {
        val side = max(abs(p.x - anchor.x), abs(p.y - anchor.y))
        val sx = if (p.x >= anchor.x) 1.0 else -1.0
        val sy = if (p.y >= anchor.y) 1.0 else -1.0
        return Pt(anchor.x + sx * side, anchor.y + sy * side)
    }

    /** Snap a line or arrow's dragged end to an exact horizontal or vertical run from [anchor]. */
    private fun snapAxisEndpoint(anchor: Pt, p: Pt): Pt {
        val dx = p.x - anchor.x
        val dy = p.y - anchor.y
        if (dx == 0.0 && dy == 0.0) return p
        val snap = Math.toRadians(InteractionController.SHAPE_AXIS_SNAP_DEG)
        val fromHoriz = atan2(abs(dy), abs(dx)) // 0 is horizontal, PI/2 is vertical
        return when {
            fromHoriz <= snap -> Pt(p.x, anchor.y)
            fromHoriz >= Math.PI / 2.0 - snap -> Pt(anchor.x, p.y)
            else -> p
        }
    }

    // --- hold still to snap a freehand stroke into a shape ---

    private fun armDwell(at: Pt) {
        cancelDwell()
        dwellAnchor = at
        val r = Runnable { onDwellElapsed() }
        dwellRunnable = r
        handler.postDelayed(r, InteractionController.SHAPE_DWELL_MS)
    }

    private fun cancelDwell() {
        dwellRunnable?.let { handler.removeCallbacks(it) }
        dwellRunnable = null
    }

    /** The pen has held still: if what it drew reads as a shape, swap the stroke for that shape. */
    private fun onDwellElapsed() {
        dwellRunnable = null
        if (!dwellEligible) return
        val stroke = liveStroke ?: return
        if (stroke.samples.size < InteractionController.SHAPE_MIN_SAMPLES) return

        // Verify that the pen has genuinely been held stationary for at least 500ms
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastStrokeMoveTimeMs < 500L) return

        // Verify spatial stability: the last few samples must all be tightly clustered
        if (stroke.samples.size >= 5) {
            val lastPos = stroke.samples.last().pos
            val tailSamples = stroke.samples.takeLast(5)
            val maxTailDrift = tailSamples.maxOf { it.pos.distanceTo(lastPos) }
            val maxAllowedDrift = (14.0 / viewport.zoom).coerceAtLeast(6.0)
            if (maxTailDrift > maxAllowedDrift) return
        }

        val rec = ShapeRecognizer.recognize(stroke.samples) ?: return
        val width = stroke.config.baseWidth * InteractionController.SHAPE_PEN_PARITY
        val color = stroke.config.rgba // the as-drawn colour, not the alpha-scaled render one
        val dashed = stroke.tool == Tool.DASHED // a dashed pen snaps to a dashed shape
        val verts = rec.vertices
        val shape = if (verts != null) {
            ShapeItem.poly(
                rec.kind, verts, color, width, null, stroke.config.neon, stroke.config.neonStrength,
                dashed, stroke.config.dashLength, stroke.config.dashGap,
            )
        } else {
            ShapeItem(
                shape = rec.kind,
                start = rec.start,
                end = rec.end,
                strokeRgba = color,
                strokeWidth = width,
                fillRgba = null,
                neon = stroke.config.neon,
                neonStrength = stroke.config.neonStrength,
                dashed = dashed,
                dashLength = stroke.config.dashLength,
                dashGap = stroke.config.dashGap,
            )
        }
        // The stroke was never committed, so dropping it makes the wet ink vanish the moment it
        // snaps; the eventual pen up then commits nothing.
        liveStroke = null
        predictedTail = null
        onWetStroke(null)
        dwellEligible = false
        cancelDwell()
        onCommitShape(shape)
        mode = CanvasPointerMode.IDLE
        // Leave the new shape selected, as a note does, so it can be resized or turned straight
        // away. The chrome only shows over a settled selection, so the handles arrive at pen up.
        selection()?.select(listOf(shape))
        onSelectionChanged()
        requestRender()
    }

    /** Smooth a freehand curvy stroke path into a sleek, jitter-free organic curve on hold-and-dwell. */
    private fun smoothCurvyStroke(stroke: Stroke) {
        if (stroke.samples.size < 4) return
        val smoothed = com.pnotes.core.stroke.CurveSmoother.smooth(stroke.samples, passes = 8)
        val next = Stroke(
            stroke.tool,
            stroke.config,
            speedScale = stroke.speedScale,
            straight = stroke.straight,
            smoothScale = stroke.smoothScale,
        )
        next.finished = false
        for (s in smoothed) {
            next.addSample(s)
        }
        liveStroke = next
        onWetStroke(next)
        dwellEligible = false
        cancelDwell()
        requestRender()
    }

    // --- erasing ---

    /** Eraser radius in content pixels: the tool's width, or a constant on-screen size when off. */
    fun eraserRadius(): Double {
        val cfg = configFor(Tool.ERASER)
        return if (cfg.scale) cfg.baseWidth else cfg.baseWidth / viewport.zoom
    }

    private fun areaErase(): Boolean = configFor(Tool.ERASER).eraseMode == EraseMode.AREA

    private fun beginErase(vx: Double, vy: Double) {
        eraseSession = onEraseBegin() ?: return
        mode = CanvasPointerMode.ERASE
        eraseAt(vx, vy)
    }

    private fun extendErase(e: MotionEvent) {
        val idx = e.findPointerIndex(drawingPointerId)
        if (idx < 0) return
        // The historical points matter here as much as when drawing: a fast sweep that only sampled
        // the newest event would skip over strokes between one frame and the next.
        for (h in 0 until e.historySize) {
            eraseAt(e.getHistoricalX(idx, h).toDouble(), e.getHistoricalY(idx, h).toDouble())
        }
        eraseAt(e.getX(idx).toDouble(), e.getY(idx).toDouble())
    }

    private fun eraseAt(vx: Double, vy: Double) {
        val session = eraseSession ?: return
        onEraserCursor(Pt(vx, vy), eraserRadius() * viewport.zoom)
        val content = viewport.viewportToContent(Pt(vx, vy))
        val cfg = configFor(Tool.ERASER)
        session.erase(content.x, content.y, eraserRadius(), areaErase(), cfg.eraseHighlighterOnly, cfg.eraseShapes)
        requestRender()
    }

    private fun endErase() {
        val session = eraseSession ?: return
        eraseSession = null
        onEraserCursor(null, 0.0)
        onEraseEnd(session)
        requestRender()
    }

    /** Drop the wet stroke without committing it, when a second finger turns the gesture into a zoom. */
    private fun abandonStroke() {
        liveStroke = null
        predictedTail = null
        onWetStroke(null)
    }

    /** Shed the samples the ribbon does not need, at the tolerance the draw zoom justifies. */
    private fun simplifyForCommit(stroke: Stroke) {
        if (StrokeSimplify.enabled && !stroke.straight) {
            val eps = (InteractionController.SIMPLIFY_EPS / viewport.zoom)
                .coerceAtMost(InteractionController.SIMPLIFY_EPS)
            val slim = StrokeSimplify.simplify(
                stroke.samples, stroke.geometry().halfWidths, eps,
                stroke.smoothScale, stroke.config.directionStrength,
            )
            if (slim.size != stroke.sampleCount) {
                stroke.setSamples(slim) // allocates exactly, so no trim needed
                stroke.invalidate()
                return
            }
        }
        // Nothing was dropped, so the stroke still carries the slack capture doubling left behind.
        stroke.trimToSize()
    }

    private fun pressureOf(e: MotionEvent, index: Int): Double =
        if (drawingIsStylus) e.getPressure(index).toDouble() else 1.0

    // --- pan ---

    private fun beginPan(vx: Double, vy: Double) {
        mode = CanvasPointerMode.PAN
        panDownAt = Pt(vx, vy)
        panSlopPassed = false
        // Moving the view is paced by the display, so the render thread stays up for it.
        setInteractive(true, true)
        startTrackingVelocity(vx, vy)
    }

    /** True when the pan never really moved, so it reads as a tap rather than a scroll. */
    private fun panWasTap(vx: Double, vy: Double): Boolean =
        Pt(vx, vy).distanceTo(panDownAt) <= TAP_SLOP_PX

    private fun extendPan(vx: Double, vy: Double) {
        trackVelocity(vx, vy)
        if (!panSlopPassed) {
            val d = devicePxPerDp()
            if (Pt(vx, vy).distanceTo(panDownAt) < 8.0 * d) {
                return
            }
            panSlopPassed = true
        }
        if (singleFingerPanAllowed()) viewport.panByViewport(vx - lastPan.x, vy - lastPan.y)
        lastPan = Pt(vx, vy)
        onViewChanged()
        requestRender()
    }

    // Once zoom is locked the zoomLockPan preference decides which pans still move the view; an
    // unlocked view always pans. Mirrors the paged canvas so one preference governs both surfaces.
    private fun singleFingerPanAllowed(): Boolean = !(zoomLocked && zoomLockPan != "single")

    private fun pinchPanAllowed(): Boolean = !(zoomLocked && zoomLockPan == "none")

    // --- pinch ---

    private fun beginPinch(e: MotionEvent) {
        mode = CanvasPointerMode.PINCH
        setInteractive(true, true)
        val a = Pt(e.getX(0).toDouble(), e.getY(0).toDouble())
        val b = Pt(e.getX(1).toDouble(), e.getY(1).toDouble())
        val mid = (a + b) * 0.5
        pinchInitDist = a.distanceTo(b).coerceAtLeast(1.0)
        pinchInitZoom = viewport.zoom
        pinchAnchorContent = viewport.viewportToContent(mid)
        startTrackingVelocity(mid.x, mid.y)
    }

    private fun updatePinch(e: MotionEvent) {
        if (e.pointerCount < 2) return
        val a = Pt(e.getX(0).toDouble(), e.getY(0).toDouble())
        val b = Pt(e.getX(1).toDouble(), e.getY(1).toDouble())
        val dist = a.distanceTo(b)
        if (dist < 1e-3) return
        val mid = (a + b) * 0.5
        trackVelocity(mid.x, mid.y)
        // Zoom about the pinch's own midpoint, and let that midpoint drag the canvas at the same
        // time: the content under the fingers stays under the fingers whether they spread or slide.
        // A locked zoom keeps the zoom it started at, so the pinch is a pan and nothing else.
        viewport.zoom = if (zoomLocked) pinchInitZoom else pinchInitZoom * (dist / pinchInitDist)
        if (pinchPanAllowed()) {
            viewport.scrollX = pinchAnchorContent.x - mid.x / viewport.zoom
            viewport.scrollY = pinchAnchorContent.y - mid.y / viewport.zoom
        }
        lastPan = mid
        onViewChanged()
        requestRender()
    }

    // --- velocity and fling ---

    private fun startTrackingVelocity(vx: Double, vy: Double) {
        stopFling()
        lastPan = Pt(vx, vy)
        lastMoveMs = System.nanoTime() / 1_000_000L
        panVel = Pt.ZERO
    }

    private fun trackVelocity(vx: Double, vy: Double) {
        val now = System.nanoTime() / 1_000_000L
        val dt = ((now - lastMoveMs).coerceAtLeast(1L)) / 1000.0
        val inst = Pt((vx - lastPan.x) / dt, (vy - lastPan.y) / dt)
        val k = InteractionController.VEL_SMOOTH
        panVel = Pt(panVel.x * k + inst.x * (1 - k), panVel.y * k + inst.y * (1 - k))
        lastMoveMs = now
    }

    private fun startFling(fingerVel: Pt) {
        val len = fingerVel.length()
        if (len < InteractionController.FLING_MIN_START) return
        val maxFlingSpeed = 16000.0
        val clampedVel = if (len > maxFlingSpeed) Pt(fingerVel.x / len * maxFlingSpeed, fingerVel.y / len * maxFlingSpeed) else fingerVel
        setInteractive(true, true) // the glide is still motion, so the render thread stays up for it
        flingVel = clampedVel
        flinging = true
        lastFlingMs = System.nanoTime() / 1_000_000L
        choreographer.postFrameCallback(flingFrame)
    }

    fun stopFling() {
        flinging = false
    }

    private fun stepFling(frameTimeNanos: Long) {
        if (!flinging) return
        val now = frameTimeNanos / 1_000_000L
        val dt = ((now - lastFlingMs).coerceIn(1L, 40L)) / 1000.0
        lastFlingMs = now
        viewport.panByViewport(flingVel.x * dt, flingVel.y * dt)
        val decay = exp(-InteractionController.FLING_FRICTION * dt)
        flingVel = Pt(flingVel.x * decay, flingVel.y * decay)
        onViewChanged()
        requestRender()
        // Nothing bounds an infinite canvas, so a glide only ever ends by running out of speed.
        if (flingVel.length() < InteractionController.FLING_MIN_STOP) {
            flinging = false
            setInteractive(false, true)
        } else {
            choreographer.postFrameCallback(flingFrame)
        }
    }
}
