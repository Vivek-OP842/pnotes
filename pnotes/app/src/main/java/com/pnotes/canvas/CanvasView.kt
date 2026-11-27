package com.pnotes.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.model.Page
import com.pnotes.core.model.Stroke
import com.pnotes.core.model.resolvedPageColor
import com.pnotes.core.pal.BlendMode
import com.pnotes.core.pal.Pen
import com.pnotes.core.tools.Tool
import com.pnotes.platform.AndroidRasterSurface
import com.pnotes.platform.AndroidRenderer

/**
 * The on-screen canvas. Draws the document in immediate mode each frame
 * (spec 05 §6): window background, then visible pages (paper + hairline border +
 * cached background layer + cached ink). Selection overlay, the live stroke and
 * the eraser cursor are layered on top by later interaction code.
 */
class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    init {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            pointerIcon = android.view.PointerIcon.getSystemIcon(context, android.view.PointerIcon.TYPE_NULL)
        }
    }

    override fun onResolvePointerIcon(event: MotionEvent, pointerIndex: Int): android.view.PointerIcon? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.view.PointerIcon.getSystemIcon(context, android.view.PointerIcon.TYPE_NULL)
        } else {
            super.onResolvePointerIcon(event, pointerIndex)
        }
    }

    var state: CanvasState? = null
        set(value) {
            field = value
            value?.let { st ->
                st.devicePxPerDp = resources.displayMetrics.density.toDouble()
                // Mirror current drawing active state into the new CanvasState immediately.
                st.isDrawingActive = isDrawingActive
                // Rasterize newly visible pages off the UI thread so scrolling never
                // stalls while a page is built; publish the surface back on the main
                // thread and ask for a repaint.
                st.runAsync = { work ->
                    val ex = cacheExecutor
                    if (ex != null && !ex.isShutdown) ex.execute(work) else work()
                }
                st.postToMain = { work -> mainHandler.post(work) }
                st.onCacheReady = { requestRender() }
            }
            invalidate()
        }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val viewRenderer = AndroidRenderer()

    /** Single background thread that builds page caches; lives while the view is attached. */
    private var cacheExecutor: ExecutorService? = null

    /** Hook for overlay drawing (selection/live-stroke/eraser), set by the interaction layer. */
    var drawOverlay: ((renderer: AndroidRenderer, canvas: Canvas) -> Unit)? = null

    /** Pointer handler installed by the interaction layer. */
    var input: ((MotionEvent) -> Boolean)? = null

    /** Clean two-finger tap (finger-only, brief, near-stationary), for the configurable gesture. */
    var onTwoFingerTap: (() -> Unit)? = null

    /** Clean three-finger tap (finger-only, brief, near-stationary), for the configurable gesture. */
    var onThreeFingerTap: (() -> Unit)? = null

    /** Invoked after the viewport is (re)laid out and the initial fit applied. */
    var afterLayout: (() -> Unit)? = null

    /** Hover handler (stylus/mouse hover) for the eraser cursor. */
    var hover: ((MotionEvent) -> Boolean)? = null

    /** Stylus side-button presses (generic-motion stream), for pens that don't put it in the touch buttonState. */
    var genericMotion: ((MotionEvent) -> Unit)? = null

    /** The inline flow text input surface (IME mirror), installed by the Editor. */
    var flowInput: FlowInput? = null

    /** Hardware keys arriving while this view holds focus (flow text sessions), fed to the Editor. */
    var onKey: ((android.view.KeyEvent) -> Boolean)? = null

    /** When true (active drawing in progress), repaints are driven directly on hardware vsync via Choreographer. */
    var isDrawingActive: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            // Mirror into CanvasState so scheduleInk can defer background cache builds during
            // an active stroke — a build completing mid-stroke blits committed-only ink and
            // creates a one-frame visual gap/cut in the live drawn stroke.
            state?.isDrawingActive = value
            if (value) {
                startFrameLoop()
            } else {
                // Ensure immediate repaint on pen-up so the committed vector stroke replaces the live overlay atomically
                invalidate()
                stopFrameLoop()
                mainHandler.removeCallbacks(sharpDebounce)
                mainHandler.postDelayed(sharpDebounce, SHARP_SETTLE_MS)
            }
        }

    private var frameLoopRunning = false
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (isDrawingActive && isAttachedToWindow) {
                invalidate()
                Choreographer.getInstance().postFrameCallback(this)
            } else {
                frameLoopRunning = false
            }
        }
    }

    private fun startFrameLoop() {
        if (!isAttachedToWindow) return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startFrameLoop() }
            return
        }
        if (!frameLoopRunning) {
            frameLoopRunning = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    private fun stopFrameLoop() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stopFrameLoop() }
            return
        }
        if (frameLoopRunning) {
            frameLoopRunning = false
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }
    }

    override fun onCheckIsTextEditor(): Boolean = flowInput?.sessionActive == true

    override fun onCreateInputConnection(outAttrs: android.view.inputmethod.EditorInfo): android.view.inputmethod.InputConnection? {
        val input = flowInput
        if (input == null || !input.sessionActive) return null
        return input.createInputConnection(outAttrs)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean =
        onKey?.invoke(event) == true || super.onKeyDown(keyCode, event)

    /** Transparent debug HUD (frame rate / cache / heap), toggled by a four-finger tap. */
    val debugOverlay = DebugOverlay()

    /** Whether the canvas scrollbar is shown (the View menu's per-note toggle). */
    var scrollbarEnabled = false

    /** Invoked after a scrollbar drag moved the view, so the host can refresh (page indicator). */
    var onScrollbarScrolled: (() -> Unit)? = null

    // Scrollbar drag state; the thumb is tracked as a fraction so fast drags don't drift.
    private var scrollbarDragging = false
    private var scrollbarFrac = 0.0
    private var scrollbarLastY = 0f
    private val scrollbarPaint = Paint()

    // Reused paints for the elastic "pull to add page" badge, so onDraw allocates nothing.
    private val overscrollStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val overscrollText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val overscrollArc = RectF()
    private val paperShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paperShadowRect = RectF()
    private val sharpBlitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val sharpBlitMatrix = Matrix()

    /**
     * Low-rate repaint while the HUD is visible. The canvas only repaints on interaction,
     * so without this the frame-rate line would freeze at its last value when idle instead
     * of falling to 0. Runs only while [DebugOverlay.enabled]; costs nothing otherwise.
     */
    private val debugTick = object : Runnable {
        override fun run() {
            if (!debugOverlay.enabled) return
            requestRender() // vsync-aligned repaint, like scroll/cache-ready; a plain invalidate() does
                            // not reliably repaint this Compose-hosted view while idle, which froze the HUD
            mainHandler.postDelayed(this, DEBUG_TICK_MS)
        }
    }

    /** (Re)start the HUD's idle repaint loop when it's showing; safe to call repeatedly. */
    private fun startDebugTick() {
        mainHandler.removeCallbacks(debugTick)
        if (debugOverlay.enabled) mainHandler.postDelayed(debugTick, DEBUG_TICK_MS)
    }

    // --- four-finger-tap recognition (toggles the debug HUD) ---
    private var gestureDownMs = 0L
    private var gestureMaxPointers = 0
    private var fourFingerActive = false
    private var fourCx = 0f
    private var fourCy = 0f
    private var fourMoved = false

    // --- two/three-finger-tap recognition (configurable gesture actions) ---
    private val tapDownX = HashMap<Int, Float>()
    private val tapDownY = HashMap<Int, Float>()
    private var tapMoved = false
    private var tapAllFingers = true

    init {
        isFocusableInTouchMode = true
        setWillNotDraw(false)
    }

    private var lastStylusEventMs: Long = 0L

    /**
     * True when [e] holds a deliberate bare-hand multi-touch gesture (two or more fingers, no
     * pen tip, eraser end or hardware-palm contact): those events are never palms, so a clean
     * two-finger scroll/tap is recognized on the first attempt.
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

    private fun isPalmPointer(e: MotionEvent, index: Int): Boolean {
        if (isMultiFingerGesture(e)) return false
        val toolType = e.getToolType(index)
        if (toolType == 5) return true // TOOL_TYPE_PALM
        if (toolType != MotionEvent.TOOL_TYPE_FINGER) return false
        val d = resources.displayMetrics.density
        val major = e.getTouchMajor(index)
        val minor = e.getTouchMinor(index)
        val size = e.getSize(index)
        if (major > 40f * d || (major > 30f * d && minor > 25f * d) || (size > 0.8f && major > 30f * d)) return true
        return false
    }

    private val tiltMatrix = android.graphics.Matrix()

    private fun transformTiltEvent(event: MotionEvent, angleDeg: Float): MotionEvent {
        tiltMatrix.reset()
        val cx = width / 2f
        val cy = height / 2f
        tiltMatrix.postRotate(angleDeg, cx, cy)
        val copy = MotionEvent.obtain(event)
        copy.transform(tiltMatrix)
        return copy
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Request unbuffered dispatch for stylus AND finger events while drawing is active —
        // bypasses vsync batching so digitizer samples reach the app immediately, cutting ~16ms
        // of pen latency. Called on every event (not just DOWN) so ACTION_MOVE is also unbuffered.
        val tt0 = event.getToolType(0)
        if (tt0 == MotionEvent.TOOL_TYPE_STYLUS || tt0 == MotionEvent.TOOL_TYPE_ERASER) {
            lastStylusEventMs = android.os.SystemClock.uptimeMillis()
            requestUnbufferedDispatch(event)
        } else if (isDrawingActive && tt0 == MotionEvent.TOOL_TYPE_FINGER) {
            // Unbuffered dispatch for finger drawing too: prevents frame-aligned batching from
            // creating 1-frame sample gaps that appear as stroke cuts at the render boundary.
            requestUnbufferedDispatch(event)
        }
        if (trackScrollbar(event)) return true
        if (trackFourFingerTap(event)) return true
        if (trackMultiFingerTap(event)) return true

        val tilt = state?.tiltAngleDeg ?: 0f
        return if (tilt != 0f) {
            val transformed = transformTiltEvent(event, -tilt)
            val handled = input?.invoke(transformed) ?: super.onTouchEvent(transformed)
            transformed.recycle()
            handled
        } else {
            input?.invoke(event) ?: super.onTouchEvent(event)
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val tilt = state?.tiltAngleDeg ?: 0f
        val e = if (tilt != 0f) transformTiltEvent(event, -tilt) else event
        val handled = genericMotion?.let { it(e); true } ?: super.onGenericMotionEvent(e)
        if (tilt != 0f && e !== event) e.recycle()
        return handled
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        val tilt = state?.tiltAngleDeg ?: 0f
        val e = if (tilt != 0f) transformTiltEvent(event, -tilt) else event
        val handled = hover?.invoke(e) ?: super.onHoverEvent(e)
        if (tilt != 0f && e !== event) e.recycle()
        return handled
    }

    // --- canvas scrollbar (View menu toggle; drawn in [onDraw], dragged like the sidebar's) ---
    // Continuous mode: a vertical bar at the right edge mapping the scroll. Paginated mode: a
    // horizontal scrubber along the bottom whose thumb is the current row; dragging snaps per row.

    /** True when the scrollbar has anything to scroll for the current view. */
    private fun scrollbarActive(st: CanvasState): Boolean = scrollbarEnabled &&
        if (st.verticalScroll) st.maxScrollY() > 0.0 else st.rowRanges().size > 1

    /** Thumb length (viewport px): the viewport's share of the content, floored for grabbability. */
    private fun scrollbarThumb(st: CanvasState): Float {
        val minThumb = SCROLLBAR_MIN_THUMB_DP * resources.displayMetrics.density
        if (!st.verticalScroll) {
            val track = st.viewportW.toFloat()
            val rows = st.rowRanges().size.coerceAtLeast(1)
            return (track / rows).coerceAtLeast(minThumb).coerceAtMost(track)
        }
        val track = st.viewportH.toFloat()
        val share = (st.viewportH / (st.contentH * st.zoom)).toFloat().coerceAtMost(1f)
        return (track * share).coerceAtLeast(minThumb).coerceAtMost(track)
    }

    /** The thumb's 0..1 position for the current view. */
    private fun scrollbarFraction(st: CanvasState): Double =
        if (st.verticalScroll) {
            (st.scrollY / st.maxScrollY()).coerceIn(0.0, 1.0)
        } else {
            val last = (st.rowRanges().size - 1).coerceAtLeast(1)
            st.currentRow.toDouble() / last
        }

    /**
     * Capture and drive a drag that starts on the scrollbar's band (right edge, or the bottom
     * edge in paginated mode). Deltas accumulate into a locally tracked fraction (seeded from
     * the current position) and map back onto the scroll — or snap per row in paginated mode —
     * exactly like the side panel's scrollbar; once a drag is live every event is consumed
     * until the finger lifts so no ink is drawn under the bar.
     */
    private fun trackScrollbar(e: MotionEvent): Boolean {
        val st = state ?: return false
        if (!scrollbarActive(st)) return false
        val toolType = e.getToolType(0)
        if (toolType == 5 || toolType == MotionEvent.TOOL_TYPE_STYLUS) return false
        if (isPalmPointer(e, 0)) return false
        if (android.os.SystemClock.uptimeMillis() - lastStylusEventMs < 2500L) return false

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val d = resources.displayMetrics.density
                val band = SCROLLBAR_TOUCH_DP * d
                if (st.verticalScroll) {
                    if (e.x < st.viewportW - band) return false
                } else {
                    // In paginated/horizontal mode, only capture near the bottom AND directly on the thumb
                    if (e.y < st.viewportH - band) return false
                    val thumbW = scrollbarThumb(st)
                    val thumbX = (st.viewportW - thumbW) * scrollbarFraction(st).toFloat()
                    val touchSlop = 28 * d
                    if (e.x < thumbX - touchSlop || e.x > thumbX + thumbW + touchSlop) return false
                }
                scrollbarDragging = true
                scrollbarLastY = if (st.verticalScroll) e.y else e.x
                scrollbarFrac = scrollbarFraction(st)
                requestRender()
                return true
            }
            MotionEvent.ACTION_MOVE -> if (scrollbarDragging) {
                val pos = if (st.verticalScroll) e.y else e.x
                val delta = pos - scrollbarLastY
                scrollbarLastY = pos
                val track = if (st.verticalScroll) st.viewportH else st.viewportW
                val travel = track - scrollbarThumb(st)
                if (delta != 0f && travel > 0f) {
                    scrollbarFrac = (scrollbarFrac + delta / travel).coerceIn(0.0, 1.0)
                    if (st.verticalScroll) {
                        st.scrollY = scrollbarFrac * st.maxScrollY()
                        st.clampScroll()
                        onScrollbarScrolled?.invoke()
                    } else {
                        val rows = st.rowRanges()
                        val target = (scrollbarFrac * (rows.size - 1)).toInt().coerceIn(0, rows.lastIndex)
                        if (target != st.currentRow) {
                            st.goToPage(rows[target].first)
                            onScrollbarScrolled?.invoke()
                        }
                    }
                    requestRender()
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP ->
                if (scrollbarDragging) return true // extra fingers never reach the canvas mid-drag
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (scrollbarDragging) {
                scrollbarDragging = false
                requestRender()
                return true
            }
        }
        return false
    }

    /** Draw the scrollbar thumb (square, dim; accent while dragged). */
    private fun drawScrollbar(canvas: Canvas, st: CanvasState) {
        if (!scrollbarActive(st)) return
        val d = resources.displayMetrics.density
        val barW = SCROLLBAR_WIDTH_DP * d
        val thumb = scrollbarThumb(st)
        val frac = scrollbarFraction(st).toFloat()
        scrollbarPaint.color = (if (scrollbarDragging) st.palette.accent else st.palette.textDim).toArgb()
        if (st.verticalScroll) {
            val top = (st.viewportH - thumb) * frac
            canvas.drawRect(st.viewportW - barW, top, st.viewportW.toFloat(), top + thumb, scrollbarPaint)
        } else {
            val left = (st.viewportW - thumb) * frac
            canvas.drawRect(left, st.viewportH - barW, left + thumb, st.viewportH.toFloat(), scrollbarPaint)
        }
    }

    /**
     * Watch the touch stream for a clean four-finger tap. Once a 4th finger lands we
     * cancel whatever gesture the interaction layer began (e.g. a pinch) and swallow the
     * rest of the gesture; on a quick, near-stationary release with exactly four fingers
     * we toggle [debugOverlay]. Returns true when the event was consumed here (so the
     * caller must not forward it to the interaction layer).
     */
    private fun trackFourFingerTap(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureDownMs = e.eventTime
                gestureMaxPointers = 1
                fourFingerActive = false
                fourMoved = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                gestureMaxPointers = maxOf(gestureMaxPointers, e.pointerCount)
                if (!fourFingerActive && e.pointerCount >= 4) {
                    fourFingerActive = true
                    val (cx, cy) = centroid(e)
                    fourCx = cx; fourCy = cy
                    cancelInteraction(e) // abort the pinch the controller already started
                }
                if (fourFingerActive) return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (fourFingerActive) {
                    val (cx, cy) = centroid(e)
                    if (kotlin.math.hypot((cx - fourCx).toDouble(), (cy - fourCy).toDouble()) > TAP_SLOP) {
                        fourMoved = true
                    }
                    return true
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (fourFingerActive) return true // wait for the last finger to lift
            }
            MotionEvent.ACTION_UP -> {
                if (fourFingerActive) {
                    fourFingerActive = false
                    val quick = e.eventTime - gestureDownMs <= TAP_TIMEOUT_MS
                    if (quick && !fourMoved && gestureMaxPointers == 4) {
                        debugOverlay.toggle()
                        startDebugTick()
                        requestRender()
                    }
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (fourFingerActive) {
                    fourFingerActive = false
                    return true
                }
            }
        }
        return false
    }

    private fun centroid(e: MotionEvent): Pair<Float, Float> {
        var sx = 0f; var sy = 0f
        for (i in 0 until e.pointerCount) { sx += e.getX(i); sy += e.getY(i) }
        return sx / e.pointerCount to sy / e.pointerCount
    }

    /** Forward a synthetic CANCEL so the interaction layer abandons its in-flight gesture. */
    private fun cancelInteraction(e: MotionEvent) {
        val cancel = MotionEvent.obtain(e)
        cancel.action = MotionEvent.ACTION_CANCEL
        input?.invoke(cancel)
        cancel.recycle()
    }

    /**
     * Recognize a clean two- or three-finger tap (finger-only, brief, near-stationary) and fire the
     * matching callback. Unlike the four-finger tap this never swallows the gesture mid-flight, so
     * pinch-zoom keeps working; only on a recognized tap do we cancel the (already-ended) pinch and
     * consume the terminal UP. A moving or slow gesture falls through untouched. Reuses [gestureDownMs]
     * and [gestureMaxPointers], which [trackFourFingerTap] (run first) keeps current.
     */
    private fun trackMultiFingerTap(e: MotionEvent): Boolean {
        val d = resources.displayMetrics.density
        val slopPx = 28.0 * d
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tapDownX.clear(); tapDownY.clear()
                tapMoved = false
                val tt = e.getToolType(0)
                tapAllFingers = (tt == MotionEvent.TOOL_TYPE_FINGER || tt == MotionEvent.TOOL_TYPE_UNKNOWN)
                recordTapDown(e, 0)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val tt = e.getToolType(e.actionIndex)
                if (tt != MotionEvent.TOOL_TYPE_FINGER && tt != MotionEvent.TOOL_TYPE_UNKNOWN) {
                    tapAllFingers = false
                }
                recordTapDown(e, e.actionIndex)
            }
            MotionEvent.ACTION_MOVE -> if (!tapMoved) {
                for (i in 0 until e.pointerCount) {
                    val tt = e.getToolType(i)
                    if (tt != MotionEvent.TOOL_TYPE_FINGER && tt != MotionEvent.TOOL_TYPE_UNKNOWN) {
                        tapAllFingers = false
                    }
                    val dx = e.getX(i) - (tapDownX[e.getPointerId(i)] ?: e.getX(i))
                    val dy = e.getY(i) - (tapDownY[e.getPointerId(i)] ?: e.getY(i))
                    if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) > slopPx) { tapMoved = true; break }
                }
            }
            MotionEvent.ACTION_UP -> {
                val cb = when (gestureMaxPointers) {
                    2 -> onTwoFingerTap
                    3 -> onThreeFingerTap
                    else -> null
                }
                val duration = e.eventTime - gestureDownMs
                val quick = duration in 25..650
                val stylusQuiet = android.os.SystemClock.uptimeMillis() - lastStylusEventMs >= 400L
                if (cb != null && quick && !tapMoved && tapAllFingers && stylusQuiet) {
                    if (gestureMaxPointers == 2 && tapDownX.size >= 2) {
                        val ids = tapDownX.keys.toList()
                        val x1 = tapDownX[ids[0]] ?: 0f
                        val y1 = tapDownY[ids[0]] ?: 0f
                        val x2 = tapDownX[ids[1]] ?: 0f
                        val y2 = tapDownY[ids[1]] ?: 0f
                        val dist = kotlin.math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble())
                        if (dist < 8f * d || dist > 280f * d) {
                            return false
                        }
                    }
                    cancelInteraction(e)
                    cb()
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> tapMoved = true
        }
        return false
    }

    private fun recordTapDown(e: MotionEvent, index: Int) {
        tapDownX[e.getPointerId(index)] = e.getX(index)
        tapDownY[e.getPointerId(index)] = e.getY(index)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (cacheExecutor?.isShutdown != false) {
            val cores = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            cacheExecutor = Executors.newFixedThreadPool(cores) { r ->
                Thread(r, "pnotes-cache").apply { isDaemon = true }
            }
        }
        startDebugTick() // resume the HUD ticker if it was left enabled across a detach/reattach
        if (isDrawingActive) startFrameLoop()
    }

    override fun onDetachedFromWindow() {
        stopFrameLoop()
        cacheExecutor?.shutdown()
        mainHandler.removeCallbacks(debugTick)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val st = state ?: return
        st.viewportW = w
        st.viewportH = h
        if (w > 0 && h > 0) {
            st.maxCachePx = maxOf(st.maxCachePx, maxOf(w, h).toDouble() * 1.5)
        }
        st.relayout()
        if (!st.didInitialFit && w > 0 && h > 0) {
            st.establishInitialView()
        } else if (w != oldw && w > 0) {
            st.reflowFitWidthForResize() // sidebar opened/closed: re-fit to the new width
        }
        st.clampScroll()
        updateSystemGestureExclusion(w, h)
        afterLayout?.invoke()
        invalidate()
    }

    /** Exclude bottom edge and lower corners from Android system back / home gestures to protect palm contact. */
    private fun updateSystemGestureExclusion(w: Int, h: Int) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && w > 0 && h > 0) {
            val d = resources.displayMetrics.density
            val bottomH = (56 * d).toInt()
            val sideW = (36 * d).toInt()
            val sideH = (140 * d).toInt()
            val rects = listOf(
                // Bottom navigation / palm rest band
                android.graphics.Rect(0, (h - bottomH).coerceAtLeast(0), w, h),
                // Bottom-left corner (back swipe zone)
                android.graphics.Rect(0, (h - sideH).coerceAtLeast(0), sideW.coerceAtMost(w), h),
                // Bottom-right corner (back swipe zone)
                android.graphics.Rect((w - sideW).coerceAtLeast(0), (h - sideH).coerceAtLeast(0), w, h),
            )
            systemGestureExclusionRects = rects
        }
    }

    override fun onDraw(canvas: Canvas) {
        val st = state ?: return
        canvas.drawColor(st.palette.bg.toArgb())

        val tilt = st.tiltAngleDeg
        if (tilt != 0f) {
            canvas.save()
            canvas.rotate(tilt, width / 2f, height / 2f)
        }

        val r = viewRenderer.bind(canvas)
        val origin = st.origin()
        r.save()
        r.translate(origin.x, origin.y)
        r.scale(st.zoom, st.zoom)

        val visible = st.visibleContentRect()
        val border = Pen(st.palette.paperBorder, 1.0, cosmetic = true)
        val cachedPages = HashSet<Page>()
        // Paginated mode shows only the current row; neighbours never draw.
        val drawable = st.drawablePageRange()

        val sharpBlit = if (!st.zoomingInProgress) st.sharpViewportBlit() else null

        for (i in st.document.pages.indices) {
            if (i !in drawable) continue
            val pr = st.pageRects.getOrNull(i) ?: continue
            if (!pr.intersects(visible)) continue
            val page = st.document.pages[i]
            cachedPages.add(page)

            if (st.pageBorders) {
                val d = resources.displayMetrics.density
                val z = st.zoom.toFloat().coerceAtLeast(0.1f)
                val dpInContent = d / z

                // Layer 1: Ambient soft shadow around page sheet
                paperShadowPaint.color = if (st.palette.isDark) 0x33000000 else 0x140F172A
                paperShadowRect.set(
                    (pr.left - 2.0 * dpInContent).toFloat(),
                    (pr.top + 2.0 * dpInContent).toFloat(),
                    (pr.right + 2.0 * dpInContent).toFloat(),
                    (pr.bottom + 10.0 * dpInContent).toFloat()
                )
                canvas.drawRoundRect(paperShadowRect, 6f * dpInContent, 6f * dpInContent, paperShadowPaint)

                // Layer 2: Deep contact shadow under bottom edge of paper sheet
                paperShadowPaint.color = if (st.palette.isDark) 0x4D000000 else 0x180F172A
                paperShadowRect.set(
                    pr.left.toFloat(),
                    (pr.top + dpInContent).toFloat(),
                    pr.right.toFloat(),
                    (pr.bottom + 4.0 * dpInContent).toFloat()
                )
                canvas.drawRoundRect(paperShadowRect, 4f * dpInContent, 4f * dpInContent, paperShadowPaint)
            }

            r.fillRect(pr, st.paperColor(page))
            if (st.pageBorders) r.strokeRect(pr, border)
            st.backgroundForOrSchedule(page)?.let { blitPageSurface(r, st, page, pr, it.surface) }

            // Ink layer: when sharp viewport overlay is active, it renders the razor-sharp ink across
            // the viewport at native screen DPI. We skip blitting the stale-scaled low-res page cache
            // to eliminate blurry halo/ghosting artifacts underneath the sharp vectors.
            if (sharpBlit == null) {
                val inkEntry = st.cacheForOrSchedule(page)
                val targetRes = st.clampedRes(page)
                if (inkEntry != null && st.isCacheUsable(inkEntry, page, targetRes)) {
                    blitPageSurface(r, st, page, pr, inkEntry.surface)
                } else {
                    // Direct vector fallback at full native display resolution — guarantees strokes are NEVER blurry
                    r.withSave {
                        r.clipRect(pr)
                        r.translate(pr.left, pr.top)
                        st.applyPageTransform(r, page)

                        val pageVisible = st.displayRectToPage(page, visible.translate(-pr.left, -pr.top))

                        if (st.flowLifted || !st.document.flow.isEmpty) {
                            st.paintFlow?.invoke(page, r, pageVisible)
                        }

                        for (item in page.items) {
                            if (!st.isLiftedItem(item)) {
                                if (item.paintBounds().intersects(pageVisible)) {
                                    item.paint(r)
                                }
                            }
                        }
                    }
                }
            } else {
                // When sharp blit is active, always draw image items (photos, stickers, cover art)
                // in page space so they never flicker, lag, or disappear during sharp frame generation.
                r.withSave {
                    r.clipRect(pr)
                    r.translate(pr.left, pr.top)
                    st.applyPageTransform(r, page)
                    val pageVisible = st.displayRectToPage(page, visible.translate(-pr.left, -pr.top))
                    for (item in page.items) {
                        if (item is com.pnotes.core.model.ImageItem && !st.isLiftedItem(item)) {
                            if (item.paintBounds().intersects(pageVisible)) {
                                item.paint(r)
                            }
                        }
                    }
                }
            }
        }
        r.restore()

        // Sharp viewport overlay: razor-sharp ink at native screen DPI
        if (sharpBlit != null) {
            val bmp = (sharpBlit.ink as? AndroidRasterSurface)?.bitmap
            if (bmp != null && !bmp.isRecycled) {
                sharpBlitMatrix.reset()
                sharpBlitMatrix.setScale(sharpBlit.scale.toFloat(), sharpBlit.scale.toFloat())
                sharpBlitMatrix.postTranslate(sharpBlit.dx.toFloat(), sharpBlit.dy.toFloat())
                canvas.drawBitmap(bmp, sharpBlitMatrix, sharpBlitPaint)
            }
        }

        // Prefetch the N pages before and after the visible band (N = visible count) into the
        // page background cache so scrolling lands on already-rasterized backgrounds.
        if (!st.zoomingInProgress && !isDrawingActive) {
            st.visiblePageRange()?.let { vis ->
                val n = vis.last - vis.first + 1
                for (d in 1..n) for (j in intArrayOf(vis.last + d, vis.first - d)) {
                    val page = st.document.pages.getOrNull(j) ?: continue
                    if (!cachedPages.add(page)) continue
                    st.backgroundForOrSchedule(page)
                }
            }
        }

        drawOverlay?.invoke(r, canvas)

        // Elastic "pull past the end to add a page" affordance, on top of everything (viewport space).
        if (st.overscrollY > 1.0) {
            drawOverscrollIndicator(canvas, st)
        } else if (!st.verticalScroll && st.flipOffsetX > 1.0 && st.currentRow >= st.rowRanges().size - 1) {
            drawFlipAddPageIndicator(canvas, st)
        }

        drawScrollbar(canvas, st)

        if (tilt != 0f) {
            canvas.restore()
        }

        st.dropCachesExcept(cachedPages)

        // Debug HUD on top, reading the just-pruned cache state (viewport space).
        debugOverlay.sampleFrame(System.nanoTime())
        debugOverlay.draw(r, st)
    }

    /** Fires once the view has been still for 0.25s ([SHARP_SETTLE_MS]), rendering the on-screen sharp viewport. */
    private val sharpDebounce = Runnable { state?.requestSharpViewport() }

    /** Blit a page-space cache surface into the page's display rect, rotated per the view. The
     *  surface covers the page's whole footprint (margins included), so it blits at [CanvasState.footprint]. */
    private fun blitPageSurface(r: AndroidRenderer, st: CanvasState, page: Page, pr: Rect, surface: com.pnotes.core.pal.RasterSurface) {
        if (st.rotationDeg == 0) {
            r.drawRaster(surface, pr)
            return
        }
        r.withSave {
            r.translate(pr.left, pr.top)
            st.applyPageTransform(r, page)
            r.drawRaster(surface, st.footprint(page))
        }
    }

    /**
     * Draw the elastic add-page badge in the gap the pull opens below the last page: an accent
     * progress ring with a "+" that closes to a full circle once the pull is far enough to release.
     * Everything fades in with the stretch so a small accidental tug shows almost nothing.
     */
    private fun drawOverscrollIndicator(canvas: Canvas, st: CanvasState) {
        val over = st.overscrollY
        if (st.pageRects.isEmpty()) return
        val d = resources.displayMetrics.density
        val progress = (over / InteractionController.OVERSCROLL_TRIGGER).coerceIn(0.0, 1.0).toFloat()
        val radius = (16f * d) * (0.8f + 0.2f * progress)

        val last = st.pageRects.last()
        val anchor = st.contentToViewport(Pt(last.centerX, last.bottom))
        val cx = anchor.x.toFloat().coerceIn(0f, st.viewportW.toFloat())
        // Pin the badge just below the page's bottom edge so it rises into the gap, never over the page.
        val cy = anchor.y.toFloat() + 12f * d + radius
        drawAddPageBadge(canvas, st, cx, cy, radius, progress, over / (InteractionController.OVERSCROLL_TRIGGER * 0.4))
    }

    /**
     * The same badge for the paginated strip, in the gap the edge-pull opens past the last page.
     * Only the last row shows it — an earlier row's pull flips to the next page instead.
     */
    private fun drawFlipAddPageIndicator(canvas: Canvas, st: CanvasState) {
        val pull = st.flipOffsetX
        val rows = st.rowRanges()
        val lastPage = rows.lastOrNull()?.last ?: return
        val rect = st.pageRects.getOrNull(lastPage) ?: return
        val d = resources.displayMetrics.density
        val progress = (pull / InteractionController.FLIP_TRIGGER).coerceIn(0.0, 1.0).toFloat()
        val radius = (16f * d) * (0.8f + 0.2f * progress)

        val anchor = st.contentToViewport(Pt(rect.right, rect.centerY))
        val cx = anchor.x.toFloat() + 12f * d + radius
        val cy = anchor.y.toFloat().coerceIn(radius + 8f * d, st.viewportH.toFloat() - radius - 24f * d)
        drawAddPageBadge(canvas, st, cx, cy, radius, progress, pull / (InteractionController.FLIP_TRIGGER * 0.4))
    }

    /** The shared add-page badge: progress ring, "+", and the pull/release caption under it. */
    private fun drawAddPageBadge(
        canvas: Canvas,
        st: CanvasState,
        cx: Float,
        cy: Float,
        radius: Float,
        progress: Float,
        rawAlpha: Double,
    ) {
        val d = resources.displayMetrics.density
        val alpha = rawAlpha.coerceIn(0.0, 1.0).toFloat()
        val ready = progress >= 1f
        val accent = st.palette.accent.toArgb()
        val dim = st.palette.textDim.toArgb()

        // Faint full track behind the progress.
        overscrollStroke.color = dim
        overscrollStroke.alpha = (45 * alpha).toInt()
        overscrollStroke.strokeWidth = 2.5f * d
        canvas.drawCircle(cx, cy, radius, overscrollStroke)

        // Accent progress ring (closes to a full circle once armed) with a "+", never filled solid.
        overscrollStroke.color = accent
        overscrollStroke.alpha = (255 * alpha).toInt()
        overscrollStroke.strokeWidth = 2.8f * d
        overscrollArc.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(overscrollArc, -90f, 360f * progress, false, overscrollStroke)
        drawPlus(canvas, cx, cy, radius * 0.46f, accent, (255 * alpha).toInt(), 2.4f * d)

        overscrollText.color = if (ready) accent else dim
        overscrollText.alpha = (235 * alpha).toInt()
        overscrollText.textSize = 11f * d
        canvas.drawText(if (ready) "Release to add page" else "Pull to add page", cx, cy + radius + 16f * d, overscrollText)
    }

    /** A centred "+" glyph (two rounded strokes) for the overscroll badge. */
    private fun drawPlus(canvas: Canvas, cx: Float, cy: Float, half: Float, color: Int, a: Int, w: Float) {
        overscrollStroke.color = color
        overscrollStroke.alpha = a
        overscrollStroke.strokeWidth = w
        canvas.drawLine(cx - half, cy, cx + half, cy, overscrollStroke)
        canvas.drawLine(cx, cy - half, cx, cy + half, overscrollStroke)
    }

    /** Request an immediate repaint on main thread (or vsync-aligned from background threads). */
    fun requestRender() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate()
            mainHandler.removeCallbacks(sharpDebounce)
            if (!isDrawingActive) {
                mainHandler.postDelayed(sharpDebounce, SHARP_SETTLE_MS)
            }
        } else {
            // Always post to main thread for both the invalidate and the sharp-debounce logic.
            // Using postInvalidateOnAnimation() here could skip a vsync and cause a 1-frame visual
            // gap mid-stroke when pnotes-cache threads signal onCacheReady during active drawing.
            mainHandler.post {
                invalidate()
                mainHandler.removeCallbacks(sharpDebounce)
                if (!isDrawingActive) {
                    mainHandler.postDelayed(sharpDebounce, SHARP_SETTLE_MS)
                }
            }
        }
    }

    companion object {
        /** Max gesture duration (ms) still counted as a tap. */
        private const val TAP_TIMEOUT_MS = 500L

        /** Max centroid drift (viewport px) the four fingers may wander and still tap. */
        private const val TAP_SLOP = 40.0

        /** How long the view must be still before on-screen sharp viewport is rendered (0.25 sec). */
        private const val SHARP_SETTLE_MS = 250L

        /** Idle repaint interval (ms) while the debug HUD is visible, so its FPS falls to 0. */
        private const val DEBUG_TICK_MS = 250L

        /** Drawn scrollbar thumb width (dp), matching the side panel's. */
        private const val SCROLLBAR_WIDTH_DP = 8f

        /** Touch band width (dp) that grabs the scrollbar — wider than the drawn bar for fingers. */
        private const val SCROLLBAR_TOUCH_DP = 16f

        /** Minimum thumb length (dp), matching the side panel's. */
        private const val SCROLLBAR_MIN_THUMB_DP = 28f
    }
}
