package com.pnotes.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pnotes.ui.theme.LocalMotion
import com.pnotes.ui.theme.LocalReducedMotion
import com.pnotes.ui.theme.Motion
import com.pnotes.ui.theme.PurpleTheme
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Multiplier on the stretch envelope: distance-driven stretch can be braver than velocity-driven,
 * because it is already gone by the time the box arrives instead of fading with the tail. */
private const val SegmentStretchScale = 1.25f

/** How much the box stretches along its travel, and how much it gives up across. */
private const val SegmentStretchAlong = 0.07f
private const val SegmentStretchAcross = 0.03f

/** One segment of a segmented control, as the indicator needs to see it: where it is, and whether
 *  it is the chosen one. */
@Immutable
data class SegmentReport(val chosen: Boolean, val bounds: Rect)

/**
 * A rectangle as four independent animated components.
 *
 * Springs on a single 2D distance desync when X and Y move at different speeds, and a box's four
 * edges are four independent quantities anyway — so they are animated as four, inside one value.
 */
internal val RectVectorConverter = TwoWayConverter<Rect, AnimationVector4D>(
    convertToVector = { AnimationVector4D(it.left, it.top, it.right, it.bottom) },
    convertFromVector = { Rect(it.v1, it.v2, it.v3, it.v4) },
)

/**
 * Where a segmented control's options are, and how much of each the travelling surface currently
 * covers.
 *
 * Segments report themselves from the layout phase and the indicator reads them back. Keeping this
 * in one small holder is what lets the chosen surface be a *single* element that travels between
 * options instead of a layer per option fading in and out — the cross-fade that made a selection
 * blink away the instant another option was picked. First built for the dock's tool slots; any
 * mutually-exclusive row or rail of options can reuse it.
 *
 * Rectangles are all in root coordinates, so a segment never has to know which container it lives in.
 */
@Stable
class SegmentTracker {
    private val segments = mutableStateMapOf<String, SegmentReport>()

    /**
     * How much of each segment the surface covers. One state per segment, so that a travel
     * invalidates the two segments it is actually crossing rather than every option in the control.
     */
    private val coverage = HashMap<String, MutableState<Float>>()

    /**
     * Which segments the surface is under, as a yes or no. A glyph has to *invert*: a coverage
     * fraction would slide every glyph through every shade on the way past, and there is no shade
     * of a colour that reads well on both the surface and the container behind it.
     */
    private val lit = mutableStateMapOf<String, Boolean>()

    /** The chosen segment, as reported by the segments themselves. */
    val activeSlotId: String?
        get() = segments.entries.firstOrNull { it.value.chosen }?.key

    /**
     * The chosen segment's rectangle in root coordinates, or null when nothing is chosen here or
     * its bounds have not been measured yet.
     */
    val activeBounds: Rect?
        get() = activeSlotId?.let { segments[it]?.bounds }

    fun boundsOf(id: String): Rect? = segments[id]?.bounds

    internal fun report(id: String, chosen: Boolean, bounds: Rect) {
        val report = SegmentReport(chosen, bounds)
        // A write to a snapshot map invalidates every reader, and segments write on every layout
        // pass of an animating container, so only write when the segment actually moved.
        if (segments[id] != report) segments[id] = report
        if (!coverage.containsKey(id)) coverage[id] = mutableStateOf(0f)
    }

    /**
     * The share of [id] the surface covers, 0..1, or [fallback] before the first frame has been
     * measured — which is what keeps the chosen option's label showing during the frame its own
     * surface is placed.
     */
    fun coverageOf(id: String, fallback: Float): Float = coverage[id]?.value ?: fallback

    /** Whether the surface is under [id], with [fallback] standing in before anything is measured. */
    fun litOf(id: String, fallback: Boolean): Boolean = lit[id] ?: fallback

    /**
     * Re-measure what the surface covers, given its rectangle in root coordinates and its opacity.
     * Coverage is written continuously, because a label climbs in as the surface arrives and
     * evaporates as it leaves; the lit flag is only written when it flips.
     */
    internal fun publish(rect: Rect?, alpha: Float) {
        for ((id, report) in segments) {
            val covered = if (rect == null) 0f else segmentCoverage(rect, report.bounds) * alpha
            val state = coverage[id]
            if (state != null && abs(state.value - covered) > 0.002f) state.value = covered
            val was = lit[id] ?: false
            val now = segmentLatched(was, covered)
            if (was != now) lit[id] = now
        }
    }

    /**
     * Forget every segment. Called when a control's item layer is collapsed away, so that the
     * surface places itself the next time the options appear rather than flying in from where it
     * used to be.
     */
    fun clear() {
        if (segments.isNotEmpty()) segments.clear()
        if (coverage.isNotEmpty()) coverage.clear()
        if (lit.isNotEmpty()) lit.clear()
    }
}

/** The control a segment belongs to, so a segment can report itself without being told where it is. */
val LocalSegmentTracker = staticCompositionLocalOf<SegmentTracker?> { null }

/** Report a segment's on-screen rectangle and chosen state to the control's indicator. */
internal fun Modifier.reportSegmentBounds(
    tracker: SegmentTracker?,
    id: String,
    chosen: Boolean,
): Modifier = onGloballyPositioned { coordinates ->
    tracker?.report(id, chosen, coordinates.boundsInRoot())
}

/**
 * How much of the way from the surface's current rectangle to its destination it is, 0..1 — by the
 * along-axis distance, the axis it visibly moves on, so a long sweep starts braver than a short hop
 * even though the spring is identical. 2000px is roughly the widest sweep the control is designed
 * for; on far larger containers the stretch simply saturates, which is the safe direction to err.
 */
internal fun segmentTravelProgress(current: Rect, destination: Rect): Float {
    val distance = maxOf(
        abs(destination.left - current.left),
        abs(destination.right - current.right),
        abs(destination.top - current.top),
        abs(destination.bottom - current.bottom),
    )
    if (distance <= 0.5f) return 1f
    return (1f - distance / 2000f).coerceIn(0f, 1f)
}

/**
 * Whether the container's own *shape* changed between two destination emissions — a flip from a
 * horizontal bar to a vertical rail, or back. That is the only move that invalidates a travel: the
 * segments' local geometry is rebuilt, so the surface's old rectangle points somewhere the new
 * container may not even contain, and animating would fly it in from outside the clip. A pure
 * translation — dragging the container, moving it between edges of the same orientation — never
 * qualifies: the surface is drawn in the container's local space, it rides along, and translations
 * are conflated away by the flow before this is asked anyway.
 */
internal fun segmentContainerReshaped(previous: Rect?, current: Rect?): Boolean =
    previous != null && current != null &&
        (abs(previous.width - current.width) > 0.5f || abs(previous.height - current.height) > 0.5f)

/**
 * The coverage at which a glyph inverts, with hysteresis.
 *
 * A glyph is drawn in the middle of its segment, so the honest threshold is the surface reaching
 * its centre — but a single threshold would flicker whenever the surface rested right on it. The
 * lower release value means a glyph lights once and stays lit until the surface has genuinely left.
 */
internal fun segmentLatched(wasLit: Boolean, coverage: Float): Boolean =
    if (wasLit) coverage > 0.34f else coverage > 0.5f

/**
 * How visible an option's name is, given how much of its segment the surface covers.
 *
 * Names materialise *under* the surface as it arrives and evaporate as it leaves, which is the
 * whole point of a travelling selection: the label you read is the label of the option the surface
 * is on. Keyed on coverage rather than on which option is chosen, so a name can never appear on
 * bare container ahead of the surface — and so the handover is robust to however far behind the
 * surface's spring happens to be.
 */
internal fun segmentLabelReveal(coverage: Float): Float =
    ((coverage - 0.35f) / 0.5f).coerceIn(0f, 1f)

/** Overlap of two rectangles as a fraction of the segment's area, clamped to 0..1. */
internal fun segmentCoverage(indicator: Rect, segment: Rect): Float {
    if (segment.width <= 0f || segment.height <= 0f) return 0f
    val overlapWidth = min(indicator.right, segment.right) - max(indicator.left, segment.left)
    val overlapHeight = min(indicator.bottom, segment.bottom) - max(indicator.top, segment.top)
    if (overlapWidth <= 0f || overlapHeight <= 0f) return 0f
    return ((overlapWidth * overlapHeight) / (segment.width * segment.height)).coerceIn(0f, 1f)
}

/**
 * A segmented control's one chosen surface: a box that travels from the option it was on to the
 * option that was just picked — a segmented control's selection, generalised. The dock's tool bar
 * is the first user; page tabs, pen swatches and any mutually-exclusive row or rail can host it.
 *
 * Host it inside the container's Box, above the material and below the buttons, and provide a
 * [SegmentTracker] to the segments through [LocalSegmentTracker]. Styling defaults to the app's
 * active-surface treatment; pass [surface], [glare], [border] or [corner] to restyle for other
 * chrome without touching the motion.
 *
 * The rectangle itself is the animated thing, not a 0..1 progress towards one. That is what makes
 * an interrupted travel continue from the rectangle currently on screen, carrying its velocity,
 * instead of restarting or jumping — and it is why there is no separate "presentation vs. target"
 * state to get out of step on the frame the choice changes.
 *
 * The surface carries no fixed flourish. It stretches along the direction it is moving by an amount
 * taken from how far it still has to go — bravest at launch, gone by the time it settles — which is
 * both what makes a long sweep read as fast and what keeps a short hop between neighbours quiet.
 * A reshaped container re-places the surface instead of travelling it: the selection's meaning did
 * not change, so there is nothing to aim at, and animating would fly it in from outside the clip.
 */
@Composable
internal fun BoxScope.SegmentedIndicator(
    tracker: SegmentTracker,
    containerBounds: Rect?,
    isVertical: Boolean,
    modifier: Modifier = Modifier,
    collapseProgress: Float = 0f,
    surface: Brush? = null,
    glare: Brush? = null,
    border: Brush? = null,
    corner: Dp? = null,
) {
    val isDark = isGlassDark()
    val motion = LocalMotion.current
    val reduced = LocalReducedMotion.current
    val density = LocalDensity.current

    val activeId = tracker.activeSlotId
    // The destination is *re-read* from layout rather than keyed into an effect: keying the
    // destination into LaunchedEffect tore the animation down and relaunched it on every layout
    // pass — the travel restarted once a frame and took an extra frame to start at all, which is
    // exactly what a delay on every choice change feels like. Only the chosen segment decides the
    // effect runs; the container's own rectangle is read through a state so the calculation also
    // re-runs if the container itself moves — an edge change, a rotation — rather than translating
    // with a stale origin. Rectangles are local to the container, so a pure translation conflates
    // away in the flow.
    val currentContainerBounds by rememberUpdatedState(containerBounds)
    val destination by remember(activeId) {
        derivedStateOf {
            val o = currentContainerBounds?.topLeft
            tracker.activeBounds
                ?.let { if (o == null) null else it.translate(-o.x, -o.y) }
        }
    }

    val rect = remember {
        Animatable(Rect.Zero, RectVectorConverter, visibilityThreshold = Rect(0.5f, 0.5f, 0.5f, 0.5f))
    }
    var placed by remember { mutableStateOf(false) }
    // The container rectangle the box's current value lives in — remembered across effect
    // relaunches, unlike anything local to the collector. A container flip can relaunch this
    // effect and re-report its segments in either order; comparing against a remembered space is
    // what makes the re-place decision independent of that race. Null until the box is first put
    // down.
    var rectSpace by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(activeId) {
        if (activeId == null) {
            // Nothing chosen in this control, or its options are collapsed away. Forget the
            // placement so the next arrival is put down rather than flown in from where the box
            // happened to die.
            placed = false
            return@LaunchedEffect
        }
        // One collector for the whole choosing. The first emission is already the destination the
        // travel will settle on, so the box launches on the frame the tap lands; collectLatest
        // keeps a re-chosen option preempting whatever flight is still in progress rather than
        // queueing behind it.
        snapshotFlow { destination }.collectLatest { dest ->
            if (dest == null) return@collectLatest
            val containerNow = currentContainerBounds
            val reshaped = segmentContainerReshaped(rectSpace, containerNow)
            if (!placed || reshaped || !Motion.usesSpring(reduced)) {
                // First placement (and reduced motion): the box is simply there. The fade carries
                // the change instead, which still says which option was chosen rather than hiding
                // it. A reshaped container re-places too: the selection's meaning did not change,
                // so there is nothing to travel; animating would fly the box across the container
                // from stale geometry, entering from outside its clip. A moving container carries
                // its selection; it does not aim it.
                rect.snapTo(dest)
                placed = true
                rectSpace = containerNow
                return@collectLatest
            }
            // Re-targeted rather than restarted. A spring starts from the value on screen and keeps
            // the velocity it already had, so a second tap before the first travel has landed rides
            // through the interruption instead of waiting its turn or snapping back to the start.
            rect.animateTo(dest, motion.travel())
            rectSpace = containerNow
        }
    }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val presence by animateFloatAsState(
        targetValue = if (appeared && destination != null) 1f else 0f,
        animationSpec = motion.quick(),
        label = "segmentIndicatorPresence",
    )

    // A host that is collapsing away (the dock's puck, a bar sliding out) owns the space once its
    // options are gone, and the box leaves with them.
    val collapse = androidx.compose.ui.util.lerp(1f, 0.85f, collapseProgress.coerceIn(0f, 1f))
    val alpha = presence * (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)

    val drawn = rect.value
    // Speed, from the distance left to the destination rather than the spring's own velocity:
    // velocity only builds a frame or two after launch, so even a fast spring reads as a still box
    // for a beat. Proximity can't: distance is known the frame the choice is made. Exactly the
    // one-shot-easing tail problem inverted — the beginning of a spring is slow-looking, the end of
    // a tween is slow, and the middle of both is where they read fast — but a spring is kept because
    // it survives interruption.
    val progress = destination?.let { segmentTravelProgress(drawn, it) } ?: 1f

    // Told per frame but written per segment, so a travel invalidates the two buttons it is
    // crossing. The origin comes from the updated state, not from composition: on the frame the
    // container itself moves, the composition-time value is the previous position and the coverage
    // would be measured against where the segments *were*.
    SideEffect {
        val o = currentContainerBounds?.topLeft
        tracker.publish(
            rect = Rect(
                drawn.left + (o?.x ?: 0f),
                drawn.top + (o?.y ?: 0f),
                drawn.right + (o?.x ?: 0f),
                drawn.bottom + (o?.y ?: 0f),
            ),
            alpha = alpha,
        )
    }

    if (currentContainerBounds == null || (destination == null && alpha <= 0.01f)) return

    val destCenter = destination?.center
    val alongX = if (destCenter == null) true else
        abs(destCenter.x - drawn.center.x) >= abs(destCenter.y - drawn.center.y)
    val stretch = SegmentStretchAlong * (1f - progress) * SegmentStretchScale
    val squash = SegmentStretchAcross * (1f - progress) * SegmentStretchScale
    // Anchored on the trailing edge of the travel, so the stretch reaches forward — towards the
    // option the box is heading for — instead of swelling backwards away from it.
    val transformOrigin = if (alongX) {
        TransformOrigin(if (drawn.center.x <= (destCenter?.x ?: drawn.center.x)) 0f else 1f, 0.5f)
    } else {
        TransformOrigin(0.5f, if (drawn.center.y <= (destCenter?.y ?: drawn.center.y)) 0f else 1f)
    }

    val shape = RoundedCornerShape(corner ?: if (isVertical) 10.5.dp else 11.dp)
    val elevation = androidx.compose.ui.unit.lerp(6.dp, 12.dp, 1f - progress)

    Box(
        modifier = modifier
            .align(Alignment.TopStart)
            .size(
                width = with(density) { drawn.width.toDp() },
                height = with(density) { drawn.height.toDp() },
            )
            .offset { IntOffset(drawn.left.roundToInt(), drawn.top.roundToInt()) }
            .graphicsLayer {
                this.alpha = alpha
                scaleX = (if (alongX) 1f + stretch else 1f - squash) * collapse
                scaleY = (if (alongX) 1f - squash else 1f + stretch) * collapse
                this.transformOrigin = transformOrigin
            }
            .shadow(
                elevation = if (isDark) elevation else 0.dp,
                shape = shape,
                clip = false,
                spotColor = if (isDark) Color(0x40000000) else Color.Transparent,
                ambientColor = if (isDark) Color(0x20000000) else Color.Transparent,
            )
            .clip(shape)
            .background(surface ?: PurpleTheme.activeGradient(isDark)),
    ) {
        if (isDark) {
            Box(Modifier.matchParentSize().background(glare ?: ActiveGlassSpecularGlare))
        }
        Box(Modifier.matchParentSize().border(if (isDark) 1.1.dp else 0.8.dp, border ?: PurpleTheme.activeBorder(isDark), shape))
    }
}
