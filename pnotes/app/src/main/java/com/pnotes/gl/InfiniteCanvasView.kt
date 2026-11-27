package com.pnotes.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import com.pnotes.core.infinite.CanvasBackground
import com.pnotes.core.infinite.CanvasViewport
import com.pnotes.core.model.Rgba

/**
 * The on-screen infinite canvas: a [GLSurfaceView] driving [GlRenderer].
 *
 * Nothing here is rasterized at a fixed resolution, so there is no cache to be at the wrong scale
 * and no settle-and-rebuild after a pinch. Zoom and scroll reach the shader as uniforms, which is
 * what makes the view sharp during a gesture rather than after it.
 *
 * Rendering is [RENDERMODE_WHEN_DIRTY]: the canvas repaints on interaction, like the paged one,
 * not at the display refresh rate. If stylus latency proves worse than the paged `View` path, the
 * swap to an owned EGL context on a dedicated render thread is confined to this file.
 *
 * The input hooks deliberately mirror `canvas.CanvasView`'s, so the same interaction layer and the
 * same hard-won device-specific stylus handling drive either canvas.
 */
class InfiniteCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    private val configChooser = MsaaConfigChooser()
    private val glRenderer = GlRenderer()

    /** The view onto the canvas. Main-thread owned; snapshotted for the GL thread by [publish]. */
    val viewport = CanvasViewport()

    var background: CanvasBackground = CanvasBackground()
        set(value) {
            field = value
            publish()
        }

    /** Paper colour behind the ruling, resolved from the canvas or the theme by the host. */
    var paperColor: Rgba = Rgba(255, 255, 255, 255)
        set(value) {
            field = value
            publish()
        }

    /** Content drawn over the ruling; installed by the editor once geometry exists. */
    var scene: GlScene?
        get() = glRenderer.scene
        set(value) {
            glRenderer.scene = value
        }

    /** Pointer handler installed by the interaction layer. */
    var input: ((MotionEvent) -> Boolean)? = null

    /** Hover handler (stylus/mouse hover), for the eraser cursor. */
    var hover: ((MotionEvent) -> Boolean)? = null

    /** Stylus side-button presses on the generic-motion stream, for pens that omit them from touch. */
    var genericMotion: ((MotionEvent) -> Unit)? = null

    /** Hardware keys arriving while this view holds focus. */
    var onKey: ((KeyEvent) -> Boolean)? = null

    /** Invoked after the viewport is laid out, so the host can apply an initial fit. */
    var afterLayout: (() -> Unit)? = null

    /** Invoked on the GL thread once a context exists or has been rebuilt after being lost. */
    var onContextReady: ((Int) -> Unit)?
        get() = glRenderer.onContextReady
        set(value) {
            glRenderer.onContextReady = value
        }

    /** Multisample count actually granted, for the debug readout. 0 or 1 means no MSAA. */
    val msaaSamples: Int get() = configChooser.chosenSamples

    /** What the last frame did. Read on the main thread; written whole on the GL thread. */
    val stats: GlStats get() = glRenderer.stats.copy(msaaSamples = configChooser.chosenSamples)

    /** Fired on a clean four-finger tap, the same gesture that toggles the paged canvas's HUD. */
    var onFourFingerTap: (() -> Unit)? = null

    /** Clean two-finger tap (finger-only, brief, near-stationary), for the configurable gesture. */
    var onTwoFingerTap: (() -> Unit)? = null

    /** Non-null when a shader would not build; the host shows a message instead of a black view. */
    val failure: String? get() = glRenderer.failure

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(configChooser)
        glRenderer.msaaSamples = { configChooser.chosenSamples }
        setRenderer(glRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        // Only a hint, and routinely ignored: onSurfaceCreated still has to rebuild everything.
        preserveEGLContextOnPause = true
        isFocusableInTouchMode = true
    }

    private var cursorAt: com.pnotes.core.geometry.Pt? = null
    private var cursorRadius = 0.0

    /** Whether the minimap is shown, and the extent of what has been drawn. */
    var minimapVisible: Boolean = false
        set(value) {
            field = value
            publish()
        }

    var contentBounds: com.pnotes.core.geometry.Rect? = null
        set(value) {
            field = value
            publish()
        }

    /** Accent colour for the minimap's markers. */
    var accent: Rgba = Rgba(0, 230, 118, 255)
        set(value) {
            field = value
            publish()
        }

    /** Show the eraser cursor at [at] in viewport pixels with [radiusPx], or hide it when null. */
    fun setEraserCursor(at: com.pnotes.core.geometry.Pt?, radiusPx: Double) {
        cursorAt = at
        cursorRadius = radiusPx
        publish()
    }

    /** Publish calls so far; the renderer differences them per frame for the debug readout. */
    private var publishRequests = 0

    private var interactive = false
    private val idleRunnable = Runnable { stopInteractive() }

    /**
     * Hand the render thread the newest view. Called after anything that changes what a frame would
     * draw, and cheap enough to call per pointer event: it is one store of an immutable record, so
     * a frame can never catch a gesture's update half applied.
     *
     * The state is written straight away rather than at the next frame callback. During a gesture
     * the render thread is already running, and it should draw the freshest pointer position that
     * has landed rather than one sampled a frame earlier.
     */
    fun publish() {
        publishRequests++
        glRenderer.frame = FrameState(
            zoom = viewport.zoom,
            scrollX = viewport.scrollX,
            scrollY = viewport.scrollY,
            widthPx = viewport.widthPx,
            heightPx = viewport.heightPx,
            background = background,
            paper = paperColor,
            cursorX = cursorAt?.x ?: 0.0,
            cursorY = cursorAt?.y ?: 0.0,
            cursorRadius = cursorRadius,
            cursorVisible = cursorAt != null && cursorRadius > 0.0,
            minimapVisible = minimapVisible,
            contentBounds = contentBounds,
            accent = accent,
        )
        glRenderer.publishRequests = publishRequests
        // In continuous mode the render thread is already drawing every refresh; otherwise ask for
        // a frame now. GLSurfaceView collapses a burst of requests into one draw by itself.
        if (!interactive) requestRender()
    }

    /**
     * Draw every refresh while the view is being moved, and only on demand otherwise.
     *
     * This is a deliberate trade, measured on the target tablet. Drawing on demand wakes the render
     * thread from the main thread once per event, and the resulting frames land unevenly: intervals
     * scattered by about 1.4 ms with occasional dropped frames, which is exactly what a pan that
     * "does not feel smooth" is made of. Leaving the render thread running removes the hop, and
     * `eglSwapBuffers` then paces it against the display: intervals tighten to about 0.07 ms with
     * no drops at all.
     *
     * The cost is latency. A continuously running thread keeps the buffer queue full, which pushed
     * the time from a finished frame to it being shown from about 15 ms to about 26 ms. That is a
     * good trade while panning, where evenness is the whole of the feel, and a bad one while
     * inking, where the gap between the nib and the line is. So this is turned on for pan, pinch
     * and the glide after them, and left off while a stroke is being drawn.
     */
    fun setInteractive(active: Boolean, linger: Boolean = true) {
        removeCallbacks(idleRunnable)
        if (active) {
            if (!interactive) {
                interactive = true
                renderMode = RENDERMODE_CONTINUOUSLY
            }
            return
        }
        // Normally linger, so a drag that pauses mid-gesture does not pay the wake cost to resume.
        // Inking asks for it at once, because every frame it stays up is a frame of lag under the pen.
        if (linger) postDelayed(idleRunnable, IDLE_LINGER_MS) else stopInteractive()
    }

    /** Stop drawing every refresh. Called by [idleRunnable] once the canvas has been still. */
    private fun stopInteractive() {
        if (!interactive) return
        interactive = false
        renderMode = RENDERMODE_WHEN_DIRTY
        requestRender()
    }

    /** Run [work] on the GL thread, for GPU state that must be touched with the context current. */
    fun onGlThread(work: () -> Unit) = queueEvent(work)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // A frame count only means something next to the rate the panel can actually show.
        glRenderer.displayHz = (display?.refreshRate ?: 60f).toDouble()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(idleRunnable)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewport.widthPx = w
        viewport.heightPx = h
        afterLayout?.invoke()
        publish()
    }

    // --- touch / gesture handling ---

    private val tapDownX = HashMap<Int, Float>()
    private val tapDownY = HashMap<Int, Float>()
    private var tapMoved = false
    private var tapAllFingers = true
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
        if (major > 22f * d || (major > 18f * d && minor > 14f * d) || size > 0.14f) return true
        if (android.os.SystemClock.uptimeMillis() - lastStylusEventMs < 2500L) {
            return true
        }
        return false
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Request unbuffered dispatch for stylus events — bypasses vsync batching so the
        // digitizer's samples reach the app as soon as they arrive, cutting pen latency ~16ms.
        val tt0 = event.getToolType(0)
        if (tt0 == MotionEvent.TOOL_TYPE_STYLUS || tt0 == MotionEvent.TOOL_TYPE_ERASER) {
            lastStylusEventMs = android.os.SystemClock.uptimeMillis()
            requestUnbufferedDispatch(event)
        }
        if (trackFourFingerTap(event)) return true
        if (trackMultiFingerTap(event)) return true
        return input?.invoke(event) ?: super.onTouchEvent(event)
    }

    // --- four-finger-tap recognition (toggles the debug HUD, as on the paged canvas) ---

    private var gestureDownMs = 0L
    private var gestureMaxPointers = 0
    private var fourFingerActive = false
    private var fourCx = 0f
    private var fourCy = 0f
    private var fourMoved = false

    /**
     * Watch for a clean four-finger tap. Once a fourth finger lands the in-flight gesture is
     * cancelled and the rest of it swallowed, so the HUD can never be toggled by something that
     * also drew or panned.
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
                    fourCx = cx
                    fourCy = cy
                    cancelInteraction(e)
                }
                if (fourFingerActive) return true
            }
            MotionEvent.ACTION_MOVE -> if (fourFingerActive) {
                val (cx, cy) = centroid(e)
                if (kotlin.math.hypot((cx - fourCx).toDouble(), (cy - fourCy).toDouble()) > TAP_SLOP) {
                    fourMoved = true
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> if (fourFingerActive) return true
            MotionEvent.ACTION_UP -> if (fourFingerActive) {
                fourFingerActive = false
                val quick = e.eventTime - gestureDownMs <= TAP_TIMEOUT_MS
                if (quick && !fourMoved && gestureMaxPointers == 4) onFourFingerTap?.invoke()
                return true
            }
            MotionEvent.ACTION_CANCEL -> if (fourFingerActive) {
                fourFingerActive = false
                return true
            }
        }
        return false
    }

    /** Recognize clean two-finger tap for tool toggle / gesture. */
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

    private fun centroid(e: MotionEvent): Pair<Float, Float> {
        var sx = 0f
        var sy = 0f
        for (i in 0 until e.pointerCount) {
            sx += e.getX(i)
            sy += e.getY(i)
        }
        return sx / e.pointerCount to sy / e.pointerCount
    }

    /** Forward a synthetic CANCEL so the interaction layer abandons whatever it had begun. */
    private fun cancelInteraction(e: MotionEvent) {
        val cancel = MotionEvent.obtain(e)
        cancel.action = MotionEvent.ACTION_CANCEL
        input?.invoke(cancel)
        cancel.recycle()
    }

    override fun onHoverEvent(event: MotionEvent): Boolean =
        hover?.invoke(event) ?: super.onHoverEvent(event)

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        genericMotion?.invoke(event)
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        onKey?.invoke(event) == true || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        onKey?.invoke(event) == true || super.onKeyUp(keyCode, event)

    companion object {
        /** Longest gesture still counted as a tap. */
        private const val TAP_TIMEOUT_MS = 500L

        /** Furthest the four fingers may drift and still count as a tap, in viewport px. */
        private const val TAP_SLOP = 40.0

        /** How long the render thread keeps running after a gesture ends. */
        private const val IDLE_LINGER_MS = 250L
    }
}
