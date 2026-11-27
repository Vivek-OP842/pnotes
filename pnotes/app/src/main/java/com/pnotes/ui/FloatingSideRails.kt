package com.pnotes.ui

import com.pnotes.ui.theme.PurpleTheme
import com.pnotes.ui.theme.shadowPaint
import com.pnotes.core.tools.StrokeTexture
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pnotes.core.model.Rgba
import com.pnotes.core.tools.EraseMode
import com.pnotes.core.tools.Tool
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.toComposeColor
import kotlin.math.roundToInt

/** Which screen edge the floating controls rail is snapped to. */
private enum class RailDock { LEFT, RIGHT, TOP, BOTTOM }

// --- Flat matte surface kit (reference redesign): solid fills only. No gradients, no
// --- specular rims, no glossy highlights anywhere on the rail — just flat colour fields
// --- with a soft matte drop shadow.

/** Toolbar surface: opaque off-white matching the reference mock. */
private val RailSurfaceDark = Color(0xFF1C1C22)
private val RailSurfaceLight = Color(0xFFF4F4F5)

/** Raised neumorphic button surface. */
private val ButtonSurface = Color(0xFFEEEEF0)

/** Recessed capsule surface (slightly darker than toolbar). */
private val CapsuleSurface = Color(0xFFE2E2E5)

/** Neumorphic shadow colours — light from top-left. */
private val NeumorphLight = Color(0xFFFFFFFF)
private val NeumorphDark = Color(0xFFBEBEC2)

/** Slate glyph/outline colour that reads on the light control surface. */
private val ControlGlyph = Color(0xFF6B6B74)

/** Dynamic monochrome selection ring for the active colour swatch. */
private val SelectionPurple: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)

/** Exactly five color swatches matching the reference in order: Black, White, Yellow, Green, Pink. */
private val ReferenceColors = listOf(
    Color(0xFF1F2024), // 1. Black
    Color(0xFFFFFFFF), // 2. White
    Color(0xFFFFE500), // 3. Yellow
    Color(0xFF2DD455), // 4. Green
    Color(0xFFF43BE0), // 5. Pink
)

private val ReferenceRgba = listOf(
    Rgba(0x1F, 0x20, 0x24, 255),
    Rgba(0xFF, 0xFF, 0xFF, 255),
    Rgba(0xFF, 0xE5, 0x00, 255),
    Rgba(0x2D, 0xD4, 0x55, 255),
    Rgba(0xF4, 0x3B, 0xE0, 255),
)

/**
 * Custom toolbar shape: sharp diagonal peaks on the dock edge,
 * smoothly rounded corners on the outer side.
 */
private class RailEdgeShape(
    private val dock: RailDock = RailDock.LEFT,
    private val cutHDp: Dp = 68.dp,
    private val cornerRadiusDp: Dp = 28.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val W = size.width
        val H = size.height
        val cutH = with(density) { cutHDp.toPx() }
        val r = with(density) { cornerRadiusDp.toPx() }

        val path = Path().apply {
            if (dock == RailDock.RIGHT) {
                moveTo(W, 0f)
                val diagRatio = cutH / W
                val rDiag = (r * 0.65f).coerceAtMost(W * 0.45f)
                val rVert = r.coerceAtMost(cutH * 0.55f)

                lineTo(rDiag, cutH - rDiag * diagRatio)
                quadraticTo(0f, cutH, 0f, cutH + rVert)

                lineTo(0f, H - cutH - rVert)
                quadraticTo(0f, H - cutH, rDiag, H - cutH + rDiag * diagRatio)

                lineTo(W, H)
                lineTo(W, 0f)
                close()
            } else {
                moveTo(0f, 0f)
                val diagRatio = cutH / W
                val rDiag = (r * 0.65f).coerceAtMost(W * 0.45f)
                val rVert = r.coerceAtMost(cutH * 0.55f)

                lineTo(W - rDiag, cutH - rDiag * diagRatio)
                quadraticTo(W, cutH, W, cutH + rVert)

                lineTo(W, H - cutH - rVert)
                quadraticTo(W, H - cutH, W - rDiag, H - cutH + rDiag * diagRatio)

                lineTo(0f, H)
                lineTo(0f, 0f)
                close()
            }
        }
        return Outline.Generic(path)
    }
}

/**
 * Flat matte drop shadow: one soft dark layer, no key-light gradient. Painted behind the
 * shape so the rail reads as lifted without any specular or reflection effect.
 */
private fun Modifier.matteShadow(
    isDark: Boolean,
    elevation: Dp,
    shape: Shape,
): Modifier = drawBehind {
    drawIntoCanvas { canvas ->
        synchronized(shadowPaint) {
            shadowPaint.setShadowLayer(
                elevation.toPx() * 1.6f,
                0f,
                elevation.toPx() * 0.7f,
                android.graphics.Color.argb(if (isDark) 70 else 46, 0, 0, 0),
            )
            canvas.nativeCanvas.drawPath(
                shape.createOutline(size, layoutDirection, this).toPath().asAndroidPath(),
                shadowPaint,
            )
        }
    }
}

/** Any [Outline] as a [Path], so the shadow paints rounded pills and circles alike. */
private fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRect(rect) }
}

/**
 * Edge-Snapping Floating Controls Rail (Stroke Width + Color Swatches / Eraser Types + Options Menu).
 * Always stays docked flush touching the screen edge (left/right vertically, top/bottom horizontally)
 * with smooth sliding along that edge. When ERASER is selected, color swatches are dynamically
 * replaced with Eraser Types / Mode controls.
 */
@Composable
fun FloatingSideRails(
    editor: Editor,
    modifier: Modifier = Modifier,
) {
    UnifiedSideRailContent(
        currentTool = editor.tool,
        currentWidth = editor.activeToolWidth,
        widthPresets = editor.widthPresets,
        toolbarColors = editor.toolbarColors,
        activeColorIndex = editor.activeColorIndex,
        onSelectWidth = { editor.selectWidthPreset(it) },
        onAddWidth = { editor.addWidthPreset(it) },
        onRemoveWidth = { editor.removeWidthPreset(it) },
        onPickColor = { editor.pickColor(it) },
        onToggleEraser = {
            if (editor.tool == Tool.ERASER) {
                editor.toggleToPreviousTool()
            } else {
                editor.selectTool(Tool.ERASER)
            }
        },
        onErasePage = { editor.erasePage(editor.state.currentPageIndex()) },
        onSelectTool = { editor.selectTool(it) },
        zoomLocked = editor.zoomLocked,
        onToggleZoomLock = { editor.toggleZoomLock() },
        host = editor,
        modifier = modifier,
    )
}

/**
 * Edge-Snapping Vertical Floating Controls Rail for Infinite Canvas.
 */
@Composable
fun FloatingSideRails(
    editor: InfiniteEditor,
    modifier: Modifier = Modifier,
) {
    UnifiedSideRailContent(
        currentTool = editor.tool,
        currentWidth = editor.activeToolWidth,
        widthPresets = editor.widthPresets,
        toolbarColors = editor.toolbarColors,
        activeColorIndex = editor.activeColorIndex,
        onSelectWidth = { editor.selectWidthPreset(it) },
        onAddWidth = { editor.addWidthPreset(it) },
        onRemoveWidth = { editor.removeWidthPreset(it) },
        onPickColor = { editor.pickColor(it) },
        onToggleEraser = {
            if (editor.tool == Tool.ERASER) {
                editor.toggleToPreviousTool()
            } else {
                editor.toggleTool(Tool.ERASER)
            }
        },
        onErasePage = { editor.clearCanvas() },
        onSelectTool = { editor.armTool(it) },
        zoomLocked = editor.zoomLocked,
        onToggleZoomLock = { editor.toggleZoomLock() },
        host = editor,
        modifier = modifier,
    )
}

/**
 * Minimal diagonal outline eraser icon matching reference:
 * thin, gray, outline style, ~45-degree angle, dividing cap line, no filled background.
 */
@Composable
private fun ReferenceEraserIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF1F2024),
) {
    Canvas(modifier = modifier) {
        val strokeW = 1.5.dp.toPx()
        val eraserW = 18.dp.toPx()
        val eraserH = 10.dp.toPx()
        val cr = 2.dp.toPx()

        withTransform({
            rotate(degrees = -45f, pivot = center)
        }) {
            val left = center.x - eraserW / 2f
            val top = center.y - eraserH / 2f

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(eraserW, eraserH),
                cornerRadius = CornerRadius(cr, cr),
                style = Stroke(width = strokeW),
            )

            val dividerX = left + eraserW * 0.38f
            drawLine(
                color = color,
                start = Offset(dividerX, top),
                end = Offset(dividerX, top + eraserH),
                strokeWidth = strokeW,
            )
        }
    }
}

/**
 * Swatch dot inside the capsule:
 * Whichever swatch is active has a prominent purple outer selection ring (#A855F7, 5px thick)
 * with a clean 2.5dp gap and thin dark boundary.
 * Inactive swatches are clean circular swatches (30dp).
 */
@Composable
private fun UnifiedColorDot(
    color: Color,
    active: Boolean,
    isWhite: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> com.pnotes.ui.theme.Motion.BUTTON_PRESS_SCALE
            active -> 1.06f
            isHovered -> 1.04f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "unifiedColorScale",
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (active) 25.dp else 28.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isWhite) {
                        Modifier.border(0.8.dp, Color(0x28000000), CircleShape)
                    } else {
                        Modifier
                    }
                )
                .drawBehind {
                    // Subtle soft shadow
                    drawCircle(
                        Color(0x18000000),
                        radius = size.minDimension / 2,
                        center = Offset(size.width / 2, size.height / 2 + 0.6.dp.toPx()),
                    )
                },
        )
    }
}

/**
 * Eraser Type / Mode Item inside the vertical capsule when Eraser tool is active.
 */
@Composable
private fun UnifiedEraserModeItem(
    icon: ImageVector,
    active: Boolean,
    tooltip: String,
    isDark: Boolean,
    isDockedOnRight: Boolean,
    isPrecisionDot: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> com.pnotes.ui.theme.Motion.BUTTON_PRESS_SCALE
            active -> 1.12f
            isHovered -> 1.08f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "eraserModeScale",
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "eraserBorderAlpha",
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (active) 0.22f else 0.08f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "eraserBgAlpha",
    )

    AppleTooltip(
        tooltip = tooltip,
        position = if (isDockedOnRight) TooltipPosition.LEFT else TooltipPosition.RIGHT,
        interactionSource = interactionSource,
    ) {
        val squircleShape = RoundedCornerShape(11.dp)
        Box(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .border(2.dp, SelectionPurple.copy(alpha = borderAlpha), squircleShape)
                .padding(2.dp)
                .clip(squircleShape)
                .background(SelectionPurple.copy(alpha = bgAlpha))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isPrecisionDot) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(
                            1.3.dp,
                            if (active) SelectionPurple else Color(0xFF4A4A52),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) SelectionPurple else Color(0xFF4A4A52),
                            ),
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = tooltip,
                    tint = if (active) SelectionPurple else Color(0xFF4A4A52),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun UnifiedSideRailContent(
    currentTool: Tool,
    currentWidth: Double,
    widthPresets: List<Double>,
    toolbarColors: List<Rgba>,
    activeColorIndex: Int,
    onSelectWidth: (Double) -> Unit,
    onAddWidth: (Double) -> Unit,
    onRemoveWidth: (Int) -> Unit,
    onPickColor: (Int) -> Unit,
    onToggleEraser: () -> Unit,
    onErasePage: (() -> Unit)?,
    onSelectTool: ((Tool) -> Unit)? = null,
    host: ToolPopupHost,
    zoomLocked: Boolean = false,
    onToggleZoomLock: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showWidthPopup by remember { mutableStateOf(false) }
    var showActiveToolPopup by remember { mutableStateOf(false) }
    var switcherIndex by remember { mutableStateOf<Int?>(null) }
    val palette = LocalPalette.current
    val isDark = palette.isDark

    val isEraserActive = currentTool == Tool.ERASER

    var railDock by remember { mutableStateOf(RailDock.LEFT) }
    var isDragging by remember { mutableStateOf(false) }
    var startedOnVerticalEdge by remember { mutableStateOf(true) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var railOffsetY by remember { mutableFloatStateOf(0f) }
    var railOffsetX by remember { mutableFloatStateOf(0f) }
    var railSizeW by remember { mutableFloatStateOf(0f) }
    var railSizeH by remember { mutableFloatStateOf(0f) }

    val isHorizontalDock = railDock == RailDock.TOP || railDock == RailDock.BOTTOM
    val isDockedOnRight = railDock == RailDock.RIGHT || isHorizontalDock
    val tooltipSide = when (railDock) {
        RailDock.LEFT -> TooltipPosition.RIGHT
        RailDock.RIGHT -> TooltipPosition.LEFT
        RailDock.TOP -> TooltipPosition.BOTTOM
        RailDock.BOTTOM -> TooltipPosition.TOP
    }

    val topInteraction = remember { MutableInteractionSource() }
    val topPressed by topInteraction.collectIsPressedAsState()
    val topHovered by topInteraction.collectIsHoveredAsState()
    val topScale by animateFloatAsState(
        targetValue = if (topPressed) 0.88f else (if (topHovered) 1.08f else 1.0f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "topScale",
    )

    val eraserInteraction = remember { MutableInteractionSource() }
    val eraserPressed by eraserInteraction.collectIsPressedAsState()
    val eraserHovered by eraserInteraction.collectIsHoveredAsState()
    val eraserScale by animateFloatAsState(
        targetValue = if (eraserPressed) com.pnotes.ui.theme.Motion.BUTTON_PRESS_SCALE else (if (eraserHovered) 1.08f else 1.0f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "eraserScale",
    )

    val moreInteraction = remember { MutableInteractionSource() }
    val morePressed by moreInteraction.collectIsPressedAsState()
    val moreHovered by moreInteraction.collectIsHoveredAsState()
    val moreScale by animateFloatAsState(
        targetValue = if (morePressed) com.pnotes.ui.theme.Motion.BUTTON_PRESS_SCALE else (if (moreHovered) 1.06f else 1.0f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "moreScale",
    )

    val zoomLockInteraction = remember { MutableInteractionSource() }
    val zoomLockPressed by zoomLockInteraction.collectIsPressedAsState()
    val zoomLockHovered by zoomLockInteraction.collectIsHoveredAsState()
    val zoomLockScale by animateFloatAsState(
        targetValue = if (zoomLockPressed) com.pnotes.ui.theme.Motion.BUTTON_PRESS_SCALE else (if (zoomLockHovered) 1.08f else 1.0f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "zoomLockScale",
    )

    val railShape: Shape = when (railDock) {
        RailDock.LEFT -> RailEdgeShape(RailDock.LEFT)
        RailDock.RIGHT -> RailEdgeShape(RailDock.RIGHT)
        RailDock.TOP -> RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = 6.dp, topEnd = 6.dp)
        RailDock.BOTTOM -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxDragWidth = constraints.maxWidth.toFloat()
        val maxDragHeight = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        val railShortPx = with(density) { 80.dp.toPx() }
        val railLongFallbackPx = with(density) { 570.dp.toPx() }

        // Horizontal position: smoothly snaps flush to left (0) or right edge when released
        val animatedOffsetX by animateFloatAsState(
            targetValue = if (isDragging) {
                dragOffsetX.coerceIn(0f, (maxDragWidth - with(density) { 50.dp.toPx() }).coerceAtLeast(0f))
            } else {
                if (railDock == RailDock.RIGHT) {
                    (maxDragWidth - with(density) { 80.dp.toPx() }).coerceAtLeast(0f)
                } else {
                    0f // Flush touching the left edge (dormant for top/bottom docks)
                }
            },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
            label = "railEdgeSnapX",
        )

        val halfSpanY = ((maxDragHeight - railShortPx) / 2f).coerceAtLeast(0f)
        val animatedSnapOffsetY by animateFloatAsState(
            targetValue = (if (isDragging) {
                dragOffsetY
            } else {
                if (railDock == RailDock.BOTTOM) halfSpanY else -halfSpanY
            }).coerceIn(-halfSpanY, halfSpanY),
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
            label = "railEdgeSnapY",
        )

        val measuredW = if (railSizeW > 0f) railSizeW else (if (!isHorizontalDock) railShortPx else railLongFallbackPx)
        val measuredH = if (railSizeH > 0f) railSizeH else (if (!isHorizontalDock) railLongFallbackPx else railShortPx)

        fun travelYPx(): Float =
            ((maxDragHeight - maxOf(measuredH, railShortPx)) / 2f).coerceAtLeast(0f)

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .onSizeChanged {
                    railSizeW = it.width.toFloat()
                    railSizeH = it.height.toFloat()
                }
                .offset {
                    val draggingOnSide = isDragging && startedOnVerticalEdge
                    IntOffset(
                        x = (if (draggingOnSide || !isHorizontalDock) animatedOffsetX else railOffsetX).roundToInt(),
                        y = (if (!draggingOnSide && isHorizontalDock) animatedSnapOffsetY else railOffsetY).roundToInt(),
                    )
                }
                .then(
                    if (!isHorizontalDock) Modifier.width(80.dp).height(570.dp)
                    else Modifier.height(80.dp).width(570.dp)
                )
                .matteShadow(isDark = isDark, elevation = 4.dp, shape = railShape)
                .clip(railShape)
                .background(RailSurfaceLight)
                .animateContentSize(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                )
                .padding(
                    if (isHorizontalDock) PaddingValues(horizontal = 58.dp, vertical = 10.dp)
                    else PaddingValues(horizontal = 10.dp, vertical = 58.dp)
                ),
            contentAlignment = Alignment.Center,
        ) {
            val strokeWidthButton: @Composable () -> Unit = {
                // ==========================================
                // 1. TOP ELEMENT: Concentric Target / Radar Button
                // ==========================================
                Box {
                    AppleTooltip(
                        tooltip = if (isEraserActive) "Eraser Size: ${String.format(java.util.Locale.US, "%.1f", currentWidth)} pt" else "Stroke Width: ${String.format(java.util.Locale.US, "%.1f", currentWidth)} pt",
                        position = tooltipSide,
                        interactionSource = topInteraction,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer {
                                    scaleX = topScale
                                    scaleY = topScale
                                }
                                .clip(CircleShape)
                                .background(ButtonSurface)
                                .border(0.8.dp, Color(0x18000000), CircleShape)
                                .drawBehind {
                                    drawCircle(NeumorphDark.copy(alpha = 0.35f), radius = size.minDimension / 2, center = Offset(size.width / 2 + 1.2.dp.toPx(), size.height / 2 + 1.2.dp.toPx()))
                                    drawCircle(NeumorphLight, radius = size.minDimension / 2, center = Offset(size.width / 2 - 1.2.dp.toPx(), size.height / 2 - 1.2.dp.toPx()))
                                }
                                .combinedClickable(
                                    interactionSource = topInteraction,
                                    indication = null,
                                    onClick = { showWidthPopup = !showWidthPopup },
                                    onLongClick = { showWidthPopup = true },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Target / Radar Icon: outer thin circle, inner thin circle, dynamic center dot
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .border(1.3.dp, Color(0xFF1F2024), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(17.dp)
                                        .border(1.0.dp, Color(0xFF6B6B74), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val dotSize = (currentWidth * 0.45 + 3.0).coerceIn(3.5, 11.0).dp
                                    Box(
                                        modifier = Modifier
                                            .size(dotSize)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1F2024)),
                                    )
                                }
                            }
                        }
                    }

                    // The width/texture card is a raw popup window and the platform owns its exit,
                    // so it is given an enter animation; the window itself is still torn down on
                    // dismissal, which is why this one opens visibly and closes instantly.
                    if (showWidthPopup) {
                        val popupOffsetX = when (railDock) {
                            RailDock.RIGHT -> with(density) { (-338).dp.roundToPx() }
                            RailDock.TOP, RailDock.BOTTOM -> with(density) { (-122).dp.roundToPx() }
                            RailDock.LEFT -> with(density) { 86.dp.roundToPx() }
                        }
                        val popupOffsetY = when (railDock) {
                            RailDock.TOP -> with(density) { 42.dp.roundToPx() }
                            RailDock.BOTTOM -> with(density) { (-208).dp.roundToPx() }
                            else -> with(density) { (-4).dp.roundToPx() }
                        }

                        Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(x = popupOffsetX, y = popupOffsetY),
                            properties = PopupProperties(
                                focusable = false,
                                dismissOnClickOutside = true,
                                dismissOnBackPress = true,
                            ),
                            onDismissRequest = { showWidthPopup = false },
                        ) {
                            PanelEnter(anchor = PanelAnchor.BOTTOM) {
                                WidthAndTexturePopupCard(
                                    currentWidth = currentWidth,
                                    currentTool = currentTool,
                                    isDark = isDark,
                                    host = host,
                                    onWidthChange = onSelectWidth,
                                    onSelectTool = onSelectTool,
                                )
                            }
                        }
                    }
                }
            }

            val eraserButton: @Composable () -> Unit = {
                // ==========================================
                // 2. ERASER BUTTON: Minimal Outline Diagonal Eraser
                // ==========================================
                Box {
                    AppleTooltip(
                        tooltip = if (isEraserActive) "Eraser Active (Tap to return to Pen)" else "Eraser (Tap to toggle)",
                        position = tooltipSide,
                        interactionSource = eraserInteraction,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer {
                                    scaleX = eraserScale
                                    scaleY = eraserScale
                                }
                                .clip(CircleShape)
                                .background(
                                    if (isEraserActive) SelectionPurple.copy(alpha = 0.15f)
                                    else ButtonSurface
                                )
                                .then(
                                    if (isEraserActive) {
                                        Modifier.border(2.dp, SelectionPurple, CircleShape)
                                    } else {
                                        Modifier.border(0.8.dp, Color(0x18000000), CircleShape)
                                    }
                                )
                                .drawBehind {
                                    if (!isEraserActive) {
                                        drawCircle(NeumorphDark.copy(alpha = 0.35f), radius = size.minDimension / 2, center = Offset(size.width / 2 + 1.2.dp.toPx(), size.height / 2 + 1.2.dp.toPx()))
                                        drawCircle(NeumorphLight, radius = size.minDimension / 2, center = Offset(size.width / 2 - 1.2.dp.toPx(), size.height / 2 - 1.2.dp.toPx()))
                                    }
                                }
                                .combinedClickable(
                                    interactionSource = eraserInteraction,
                                    indication = null,
                                    onClick = onToggleEraser,
                                    onLongClick = { showActiveToolPopup = true },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            ReferenceEraserIcon(
                                modifier = Modifier.size(24.dp),
                                color = if (isEraserActive) SelectionPurple else Color(0xFF1F2024),
                            )
                        }
                    }
                }
            }

            val zoomLockButton: @Composable () -> Unit = {
                // ==========================================
                // 2b. ZOOM LOCK BUTTON: keep the canvas at its current zoom
                // ==========================================
                if (onToggleZoomLock != null) {
                    AppleTooltip(
                        tooltip = if (zoomLocked) "Zoom Locked (Tap to unlock)" else "Lock Zoom",
                        position = tooltipSide,
                        interactionSource = zoomLockInteraction,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer {
                                    scaleX = zoomLockScale
                                    scaleY = zoomLockScale
                                }
                                .clip(CircleShape)
                                .background(if (zoomLocked) SelectionPurple.copy(alpha = 0.15f) else ButtonSurface)
                                .then(
                                    if (zoomLocked) {
                                        Modifier.border(2.dp, SelectionPurple, CircleShape)
                                    } else {
                                        Modifier.border(0.8.dp, Color(0x18000000), CircleShape)
                                    }
                                )
                                .drawBehind {
                                    if (!zoomLocked) {
                                        drawCircle(NeumorphDark.copy(alpha = 0.35f), radius = size.minDimension / 2, center = Offset(size.width / 2 + 1.2.dp.toPx(), size.height / 2 + 1.2.dp.toPx()))
                                        drawCircle(NeumorphLight, radius = size.minDimension / 2, center = Offset(size.width / 2 - 1.2.dp.toPx(), size.height / 2 - 1.2.dp.toPx()))
                                    }
                                }
                                .clickable(
                                    interactionSource = zoomLockInteraction,
                                    indication = null,
                                    onClick = onToggleZoomLock,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (zoomLocked) PnotesIcons.lock else PnotesIcons.unlock,
                                contentDescription = if (zoomLocked) "Zoom Locked" else "Lock Zoom",
                                tint = if (zoomLocked) SelectionPurple else Color(0xFF1F2024),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            val middleSegment: @Composable () -> Unit = {
                // ==========================================
                // 3. COLOR PALETTE CAPSULE OR ERASER TYPES
                // ==========================================
                Box(
                    modifier = Modifier
                        .then(
                            if (!isHorizontalDock) Modifier.width(48.dp).height(238.dp)
                            else Modifier.height(48.dp).width(238.dp)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(CapsuleSurface)
                        .border(0.6.dp, Color(0x12000000), RoundedCornerShape(24.dp))
                        .drawBehind {
                            drawRoundRect(
                                NeumorphDark.copy(alpha = 0.35f),
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                            )
                            drawRoundRect(
                                CapsuleSurface,
                                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                                size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                                cornerRadius = CornerRadius(23.dp.toPx(), 23.dp.toPx()),
                            )
                        }
                        .padding(
                            if (!isHorizontalDock) PaddingValues(vertical = 8.dp)
                            else PaddingValues(horizontal = 8.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val middleItems: @Composable () -> Unit = {
                        if (isEraserActive) {
                            var currentEraseMode by remember(host.toolConfig(Tool.ERASER).eraseMode) {
                                mutableStateOf(host.toolConfig(Tool.ERASER).eraseMode)
                            }

                            // 1. Precision Eraser (Area Mode)
                            val isPrecision = currentEraseMode == EraseMode.AREA
                            UnifiedEraserModeItem(
                                icon = PnotesIcons.pan,
                                isPrecisionDot = true,
                                active = isPrecision,
                                tooltip = "Precision Eraser",
                                isDark = isDark,
                                isDockedOnRight = isDockedOnRight,
                                onClick = {
                                    currentEraseMode = EraseMode.AREA
                                    val cfg = host.toolConfig(Tool.ERASER)
                                    host.updateToolConfig(Tool.ERASER, cfg.copy(eraseMode = EraseMode.AREA))
                                },
                            )

                            // 2. Stroke Eraser (Whole Stroke Mode)
                            val isStroke = currentEraseMode == EraseMode.STROKE
                            UnifiedEraserModeItem(
                                icon = PnotesIcons.eraser,
                                active = isStroke,
                                tooltip = "Stroke Eraser",
                                isDark = isDark,
                                isDockedOnRight = isDockedOnRight,
                                onClick = {
                                    currentEraseMode = EraseMode.STROKE
                                    val cfg = host.toolConfig(Tool.ERASER)
                                    host.updateToolConfig(Tool.ERASER, cfg.copy(eraseMode = EraseMode.STROKE))
                                },
                            )

                            // 3. Highlighter Only Toggle
                            val eraserCfg = host.toolConfig(Tool.ERASER)
                            val isHighlighterOnly = eraserCfg.eraseHighlighterOnly
                            UnifiedEraserModeItem(
                                icon = PnotesIcons.highlighter,
                                active = isHighlighterOnly,
                                tooltip = if (isHighlighterOnly) "Highlighter Only: ON" else "Highlighter Only: OFF",
                                isDark = isDark,
                                isDockedOnRight = isDockedOnRight,
                                onClick = {
                                    val next = !eraserCfg.eraseHighlighterOnly
                                    host.updateToolConfig(Tool.ERASER, eraserCfg.copy(eraseHighlighterOnly = next))
                                },
                            )

                            // 4. Clear Page
                            if (onErasePage != null) {
                                UnifiedEraserModeItem(
                                    icon = PnotesIcons.trash,
                                    active = false,
                                    tooltip = "Clear Page",
                                    isDark = isDark,
                                    isDockedOnRight = isDockedOnRight,
                                    onClick = onErasePage,
                                )
                            }
                        } else {
                            val displayColors = if (toolbarColors.size >= 5) toolbarColors.take(5) else (toolbarColors + ReferenceRgba).take(5)
                            displayColors.forEachIndexed { i, rgba ->
                                val isActive = i == activeColorIndex
                                val isWhite = rgba.r > 240 && rgba.g > 240 && rgba.b > 240
                                Box {
                                    UnifiedColorDot(
                                        color = rgba.toComposeColor(),
                                        active = isActive,
                                        isWhite = isWhite,
                                        onClick = {
                                            if (isActive) {
                                                switcherIndex = i
                                            } else {
                                                onPickColor(i)
                                            }
                                        },
                                    )

                                    if (switcherIndex == i) {
                                        ColorSwitcherPopup(host, i) { switcherIndex = null }
                                    }
                                }
                            }
                        }
                    }

                    if (!isHorizontalDock) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            middleItems()
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            middleItems()
                        }
                    }

                    // Traveling selection ring for quick colors
                    if (!isEraserActive && activeColorIndex in 0..4) {
                        val colorRingTargetOffset = (activeColorIndex * 46).dp
                        val colorRingOffset by animateDpAsState(
                            targetValue = colorRingTargetOffset,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            label = "travelingColorRing",
                        )
                        Box(
                            modifier = Modifier
                                .then(
                                    if (!isHorizontalDock) {
                                        Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = colorRingOffset)
                                    } else {
                                        Modifier
                                            .align(Alignment.CenterStart)
                                            .offset(x = colorRingOffset)
                                    }
                                )
                                .size(38.dp)
                                .border(4.dp, SelectionPurple, CircleShape)
                        )
                    }
                }
            }

            val dragHandleButton: @Composable () -> Unit = {
                // ==========================================
                // 4. BOTTOM ELEMENT: 3-Bar Hamburger Menu / Drag Handle Button
                // ==========================================
                Box {
                    val toolDisplayName = when (currentTool) {
                        Tool.ERASER -> "Eraser"
                        Tool.PEN -> "Pen"
                        Tool.CALLIGRAPHY -> "Calligraphy Pen"
                        Tool.HIGHLIGHTER -> "Highlighter"
                        Tool.DASHED -> "Dashed Pen"
                        Tool.SPEED -> "Speed Pen"
                        Tool.TAPER -> "Taper Pen"
                        Tool.SHAPE -> "Shape"
                        Tool.TABLE -> "Table"
                        Tool.SELECT -> "Select"
                        Tool.LASSO -> "Lasso"
                        Tool.TEXT, Tool.TEXT_BOX -> "Text"
                        Tool.IMAGE -> "Image"
                        Tool.PAN -> "Pan"
                        Tool.SCREENSHOT -> "Screenshot"
                    }
                    AppleTooltip(
                        tooltip = "$toolDisplayName Settings / Drag to Move",
                        position = tooltipSide,
                        interactionSource = moreInteraction,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer {
                                    scaleX = moreScale
                                    scaleY = moreScale
                                }
                                .clip(CircleShape)
                                .background(ButtonSurface)
                                .border(0.8.dp, Color(0x18000000), CircleShape)
                                .drawBehind {
                                    drawCircle(NeumorphDark.copy(alpha = 0.35f), radius = size.minDimension / 2, center = Offset(size.width / 2 + 1.2.dp.toPx(), size.height / 2 + 1.2.dp.toPx()))
                                    drawCircle(NeumorphLight, radius = size.minDimension / 2, center = Offset(size.width / 2 - 1.2.dp.toPx(), size.height / 2 - 1.2.dp.toPx()))
                                }
                                .pointerInput(railSizeW > 0f && railSizeH > 0f) {
                                    fun finishDrag() {
                                        val relLeft = if (startedOnVerticalEdge) dragOffsetX else railOffsetX
                                        val relTop = (maxDragHeight - measuredH) / 2f +
                                            (if (startedOnVerticalEdge) railOffsetY else dragOffsetY)
                                        val centerX = relLeft + measuredW / 2f
                                        val centerY = relTop + measuredH / 2f
                                        val dLeft = centerX.coerceAtLeast(1f)
                                        val dRight = (maxDragWidth - centerX).coerceAtLeast(1f)
                                        val dTop = centerY.coerceAtLeast(1f)
                                        val dBottom = (maxDragHeight - centerY).coerceAtLeast(1f)

                                        val orientBias = 1.45f
                                        val nearest = if (startedOnVerticalEdge) {
                                            if (minOf(dTop, dBottom) * orientBias < minOf(dLeft, dRight)) {
                                                if (dTop < dBottom) RailDock.TOP else RailDock.BOTTOM
                                            } else {
                                                if (dLeft < dRight) RailDock.LEFT else RailDock.RIGHT
                                            }
                                        } else {
                                            if (minOf(dLeft, dRight) * orientBias < minOf(dTop, dBottom)) {
                                                if (dLeft < dRight) RailDock.LEFT else RailDock.RIGHT
                                            } else {
                                                if (dTop < dBottom) RailDock.TOP else RailDock.BOTTOM
                                            }
                                        }
                                        val wentHorizontal = nearest == RailDock.TOP || nearest == RailDock.BOTTOM
                                        if (!wentHorizontal) {
                                            railOffsetY = (centerY - maxDragHeight / 2f)
                                                .coerceIn(-travelYPx(), travelYPx())
                                            dragOffsetX = relLeft.coerceIn(0f, (maxDragWidth - measuredW).coerceAtLeast(0f))
                                            dragOffsetY = railOffsetY
                                        } else {
                                            railOffsetX = relLeft
                                                .coerceIn(0f, (maxDragWidth - railLongFallbackPx).coerceAtLeast(0f))
                                            dragOffsetY = relTop - (maxDragHeight - railShortPx) / 2f
                                        }
                                        railDock = nearest
                                        isDragging = false
                                    }

                                    detectDragGestures(
                                        onDragStart = {
                                            isDragging = true
                                            startedOnVerticalEdge = !isHorizontalDock
                                            dragOffsetX = when {
                                                railDock == RailDock.RIGHT ->
                                                    (maxDragWidth - measuredW).coerceAtLeast(0f)
                                                isHorizontalDock ->
                                                    railOffsetX.coerceIn(0f, (maxDragWidth - measuredW).coerceAtLeast(0f))
                                                else -> 0f
                                            }
                                            dragOffsetY = when {
                                                isHorizontalDock && railDock == RailDock.BOTTOM -> halfSpanY
                                                isHorizontalDock -> -halfSpanY
                                                else -> railOffsetY.coerceIn(-travelYPx(), travelYPx())
                                            }
                                        },
                                        onDragEnd = { finishDrag() },
                                        onDragCancel = { finishDrag() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (startedOnVerticalEdge) {
                                                dragOffsetX += dragAmount.x
                                                railOffsetY = (railOffsetY + dragAmount.y)
                                                    .coerceIn(-travelYPx(), travelYPx())
                                            } else {
                                                dragOffsetY = (dragOffsetY + dragAmount.y)
                                                    .coerceIn(-halfSpanY, halfSpanY)
                                                railOffsetX = (railOffsetX + dragAmount.x)
                                                    .coerceIn(0f, (maxDragWidth - measuredW).coerceAtLeast(0f))
                                            }
                                        }
                                    )
                                }
                                .clickable(
                                    interactionSource = moreInteraction,
                                    indication = null,
                                    onClick = {
                                        showActiveToolPopup = true
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(3.5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(Modifier.size(width = 18.dp, height = 2.2.dp).clip(RoundedCornerShape(1.1.dp)).background(Color(0xFF4A4A52)))
                                Box(Modifier.size(width = 18.dp, height = 2.2.dp).clip(RoundedCornerShape(1.1.dp)).background(Color(0xFF4A4A52)))
                                Box(Modifier.size(width = 18.dp, height = 2.2.dp).clip(RoundedCornerShape(1.1.dp)).background(Color(0xFF4A4A52)))
                            }
                        }
                    }

                    // The panel a tool opens keeps its slot once opened, so clearing the flag plays
                    // its exit instead of tearing the menu down in the frame the tap landed.
                    val openedPopupTool = rememberLastPresent(if (showActiveToolPopup) currentTool else null)
                    if (openedPopupTool != null) {
                        val popupVisible = showActiveToolPopup && openedPopupTool == currentTool
                        when (openedPopupTool) {
                            Tool.ERASER -> EraserConfigPopup(host, onDismiss = { showActiveToolPopup = false }, visible = popupVisible)
                            Tool.SHAPE -> ShapeConfigPopup(host, onDismiss = { showActiveToolPopup = false }, visible = popupVisible)
                            Tool.SELECT, Tool.LASSO -> SelectConfigPopup(host, onDismiss = { showActiveToolPopup = false }, visible = popupVisible)
                            else -> ToolConfigPopup(host, openedPopupTool, onDismiss = { showActiveToolPopup = false }, visible = popupVisible)
                        }
                    }
                }
            }

            if (!isHorizontalDock) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    strokeWidthButton()
                    eraserButton()
                    zoomLockButton()
                    middleSegment()
                    dragHandleButton()
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    strokeWidthButton()
                    eraserButton()
                    zoomLockButton()
                    middleSegment()
                    dragHandleButton()
                }
            }
        }
    }
}

private data class BrushTexture(
    val id: String,
    val label: String,
    val textureType: StrokeTexture,
    val tool: Tool,
    val configure: (com.pnotes.core.tools.ToolConfig) -> com.pnotes.core.tools.ToolConfig,
    val drawPreview: androidx.compose.ui.graphics.drawscope.DrawScope.(Color, Boolean) -> Unit,
)

/**
 * Redesigned Floating Toolbar Stroke Width & Textures Card.
 * Replaces the slider and Add/Delete action buttons with:
 * 1. Fixed Pen Width Section: 10 proportional preset dots in 2 rows of 5 (1, 2, 4, 8, 12 px & 20, 30, 50, 60, 80 px).
 * 2. Textures Section: distinct brush presets and textures (Smooth, Pencil, G-Pen, Italic, Oblique, Taper, Speed, Marker, Chalk, Neon, Dashed, Watercolor, Charcoal, Dotted)
 *    with custom Canvas-rendered stroke previews and labels laid out evenly in a 4-column grid.
 */
@Composable
private fun WidthAndTexturePopupCard(
    currentWidth: Double,
    currentTool: Tool,
    isDark: Boolean,
    host: ToolPopupHost,
    onWidthChange: (Double) -> Unit,
    onSelectTool: ((Tool) -> Unit)?,
) {
    val penWidthRow1 = listOf(1.0, 2.0, 4.0, 8.0, 12.0)
    val penWidthRow2 = listOf(20.0, 30.0, 50.0, 60.0, 80.0)

    val textures = remember {
        listOf(
            BrushTexture(
                id = "smooth",
                label = "Smooth",
                textureType = StrokeTexture.SMOOTH,
                tool = Tool.PEN,
                configure = { it.copy(texture = StrokeTexture.SMOOTH, pressureEnabled = true, pressureMinFactor = 0.35, neon = false, directionStrength = 0.0, speedStrength = 0.0, taperEnabled = false) },
                drawPreview = { color, _ ->
                    val path = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.75f)
                        cubicTo(
                            size.width * 0.40f, size.height * 0.85f,
                            size.width * 0.60f, size.height * 0.15f,
                            size.width * 0.85f, size.height * 0.25f,
                        )
                    }
                    drawPath(path, color, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
                }
            ),
            BrushTexture(
                id = "pencil",
                label = "Pencil",
                textureType = StrokeTexture.PENCIL,
                tool = Tool.PEN,
                configure = { it.copy(texture = StrokeTexture.PENCIL, pressureEnabled = true, pressureMinFactor = 0.20, neon = false, directionStrength = 0.0, speedStrength = 0.0, taperEnabled = false) },
                drawPreview = { color, _ ->
                    val path = Path().apply {
                        moveTo(size.width * 0.18f, size.height * 0.72f)
                        cubicTo(
                            size.width * 0.45f, size.height * 0.80f,
                            size.width * 0.55f, size.height * 0.20f,
                            size.width * 0.82f, size.height * 0.28f,
                        )
                    }
                    drawPath(path, color.copy(alpha = 0.85f), style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round))
                    drawCircle(color.copy(alpha = 0.6f), radius = 1.0.dp.toPx(), center = Offset(size.width * 0.35f, size.height * 0.60f))
                    drawCircle(color.copy(alpha = 0.6f), radius = 0.9.dp.toPx(), center = Offset(size.width * 0.65f, size.height * 0.40f))
                }
            ),
            BrushTexture(
                id = "calligraphy",
                label = "G-Pen",
                textureType = StrokeTexture.CALLIGRAPHY,
                tool = Tool.CALLIGRAPHY,
                configure = { it.copy(texture = StrokeTexture.CALLIGRAPHY, directionStrength = 0.85, nibCut = 0.0, brushRatio = 0.12, brushAngle = Math.toRadians(90.0), neon = false) },
                drawPreview = { color, _ ->
                    val path = Path().apply {
                        moveTo(size.width * 0.20f, size.height * 0.80f)
                        lineTo(size.width * 0.28f, size.height * 0.84f)
                        lineTo(size.width * 0.84f, size.height * 0.24f)
                        lineTo(size.width * 0.76f, size.height * 0.18f)
                        close()
                    }
                    drawPath(path, color)
                }
            ),
            BrushTexture(
                id = "italic",
                label = "Italic",
                textureType = StrokeTexture.CALLIGRAPHY,
                tool = Tool.CALLIGRAPHY,
                configure = { it.copy(texture = StrokeTexture.CALLIGRAPHY, directionStrength = 0.80, nibCut = 0.0, brushRatio = 0.14, brushAngle = Math.toRadians(45.0), neon = false) },
                drawPreview = { color, _ ->
                    val path = Path().apply {
                        moveTo(size.width * 0.18f, size.height * 0.82f)
                        lineTo(size.width * 0.30f, size.height * 0.82f)
                        lineTo(size.width * 0.82f, size.height * 0.20f)
                        lineTo(size.width * 0.70f, size.height * 0.20f)
                        close()
                    }
                    drawPath(path, color)
                }
            ),
            BrushTexture(
                id = "oblique",
                label = "Oblique",
                textureType = StrokeTexture.CALLIGRAPHY,
                tool = Tool.CALLIGRAPHY,
                configure = { it.copy(texture = StrokeTexture.CALLIGRAPHY, directionStrength = 0.80, nibCut = Math.toRadians(-15.0), brushRatio = 0.14, brushAngle = Math.toRadians(35.0), neon = false) },
                drawPreview = { color, _ ->
                    val path = Path().apply {
                        moveTo(size.width * 0.22f, size.height * 0.85f)
                        lineTo(size.width * 0.36f, size.height * 0.79f)
                        lineTo(size.width * 0.82f, size.height * 0.25f)
                        lineTo(size.width * 0.68f, size.height * 0.31f)
                        close()
                    }
                    drawPath(path, color)
                }
            ),
            BrushTexture(
                id = "taper",
                label = "Taper",
                textureType = StrokeTexture.TAPER,
                tool = Tool.TAPER,
                configure = { it.copy(texture = StrokeTexture.TAPER, taperEnabled = true, taperMinFactor = 0.05, neon = false) },
                drawPreview = { color, _ ->
                    val path = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.70f)
                        quadraticTo(size.width * 0.45f, size.height * 0.70f, size.width * 0.85f, size.height * 0.25f)
                        quadraticTo(size.width * 0.50f, size.height * 0.60f, size.width * 0.15f, size.height * 0.70f)
                    }
                    drawPath(path, color)
                    drawCircle(color, radius = 3.2.dp.toPx(), center = Offset(size.width * 0.18f, size.height * 0.70f))
                }
            ),
            BrushTexture(
                id = "speed",
                label = "Speed",
                textureType = StrokeTexture.SPEED,
                tool = Tool.SPEED,
                configure = { it.copy(texture = StrokeTexture.SPEED, speedStrength = 0.85, neon = false) },
                drawPreview = { color, _ ->
                    val path = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.80f)
                        cubicTo(
                            size.width * 0.35f, size.height * 0.50f,
                            size.width * 0.60f, size.height * 0.30f,
                            size.width * 0.88f, size.height * 0.20f,
                        )
                    }
                    drawPath(path, color, style = Stroke(width = 3.8.dp.toPx(), cap = StrokeCap.Round))
                }
            ),
            BrushTexture(
                id = "marker",
                label = "Marker",
                textureType = StrokeTexture.MARKER,
                tool = Tool.HIGHLIGHTER,
                configure = { it.copy(texture = StrokeTexture.MARKER, highlighterAlpha = 0.55, neon = false) },
                drawPreview = { color, _ ->
                    drawLine(
                        color = color.copy(alpha = 0.55f),
                        start = Offset(size.width * 0.18f, size.height * 0.72f),
                        end = Offset(size.width * 0.82f, size.height * 0.28f),
                        strokeWidth = 7.0.dp.toPx(),
                        cap = StrokeCap.Square,
                    )
                }
            ),
            BrushTexture(
                id = "chalk",
                label = "Chalk",
                textureType = StrokeTexture.CHALK,
                tool = Tool.CALLIGRAPHY,
                configure = { it.copy(texture = StrokeTexture.CHALK, directionStrength = 0.25, neon = false) },
                drawPreview = { color, _ ->
                    val step = size.width * 0.10f
                    var curX = size.width * 0.18f
                    var curY = size.height * 0.75f
                    for (i in 0..5) {
                        val nxtX = curX + step
                        val nxtY = curY - step * 0.85f
                        val jitter = (if (i % 2 == 0) 1.2f else -1.2f) * 1.5.dp.toPx()
                        drawLine(
                            color = color.copy(alpha = 0.80f),
                            start = Offset(curX, curY + jitter),
                            end = Offset(nxtX, nxtY - jitter),
                            strokeWidth = 3.2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        drawCircle(color.copy(alpha = 0.5f), radius = 1.0.dp.toPx(), center = Offset(curX + 1.dp.toPx(), curY - 2.dp.toPx()))
                        curX = nxtX
                        curY = nxtY
                    }
                }
            ),
            BrushTexture(
                id = "neon",
                label = "Neon",
                textureType = StrokeTexture.NEON,
                tool = Tool.PEN,
                configure = { it.copy(texture = StrokeTexture.NEON, neon = true, neonStrength = 0.80) },
                drawPreview = { _, _ ->
                    val glowColor = Color(0xFF00E5FF)
                    val p1 = Offset(size.width * 0.20f, size.height * 0.75f)
                    val p2 = Offset(size.width * 0.80f, size.height * 0.25f)
                    drawLine(color = glowColor.copy(alpha = 0.35f), start = p1, end = p2, strokeWidth = 8.0.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = glowColor.copy(alpha = 0.70f), start = p1, end = p2, strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = Color.White, start = p1, end = p2, strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                }
            ),
            BrushTexture(
                id = "dashed",
                label = "Dashed",
                textureType = StrokeTexture.DASHED,
                tool = Tool.DASHED,
                configure = { it.copy(texture = StrokeTexture.DASHED, dashLength = 12.0, dashGap = 8.0, neon = false) },
                drawPreview = { color, _ ->
                    drawLine(
                        color = color,
                        start = Offset(size.width * 0.16f, size.height * 0.74f),
                        end = Offset(size.width * 0.84f, size.height * 0.26f),
                        strokeWidth = 2.8.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                            0f
                        ),
                        cap = StrokeCap.Round,
                    )
                }
            ),
            BrushTexture(
                id = "watercolor",
                label = "Brush",
                textureType = StrokeTexture.WATERCOLOR,
                tool = Tool.HIGHLIGHTER,
                configure = { it.copy(texture = StrokeTexture.WATERCOLOR, highlighterAlpha = 0.30, neon = false) },
                drawPreview = { _, _ ->
                    drawCircle(
                        color = Color(0xFF60A5FA).copy(alpha = 0.35f),
                        radius = 8.dp.toPx(),
                        center = Offset(size.width * 0.42f, size.height * 0.55f),
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                        radius = 9.dp.toPx(),
                        center = Offset(size.width * 0.58f, size.height * 0.45f),
                    )
                }
            ),
            BrushTexture(
                id = "charcoal",
                label = "Charc",
                textureType = StrokeTexture.CHARCOAL,
                tool = Tool.SPEED,
                configure = { it.copy(texture = StrokeTexture.CHARCOAL, speedStrength = 0.70, neon = false) },
                drawPreview = { color, _ ->
                    drawLine(
                        color = color.copy(alpha = 0.90f),
                        start = Offset(size.width * 0.18f, size.height * 0.72f),
                        end = Offset(size.width * 0.82f, size.height * 0.28f),
                        strokeWidth = 4.2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(color.copy(alpha = 0.7f), radius = 1.2.dp.toPx(), center = Offset(size.width * 0.30f, size.height * 0.70f))
                    drawCircle(color.copy(alpha = 0.7f), radius = 1.4.dp.toPx(), center = Offset(size.width * 0.70f, size.height * 0.30f))
                }
            ),
            BrushTexture(
                id = "dotted",
                label = "Dotted",
                textureType = StrokeTexture.DOTTED,
                tool = Tool.DASHED,
                configure = { it.copy(texture = StrokeTexture.DOTTED, dashLength = 0.1, dashGap = 8.0, neon = false) },
                drawPreview = { color, _ ->
                    val pts = listOf(
                        Offset(size.width * 0.22f, size.height * 0.72f),
                        Offset(size.width * 0.40f, size.height * 0.56f),
                        Offset(size.width * 0.58f, size.height * 0.40f),
                        Offset(size.width * 0.76f, size.height * 0.24f),
                    )
                    pts.forEach { pt ->
                        drawCircle(color = color, radius = 2.2.dp.toPx(), center = pt)
                    }
                }
            ),
        )
    }

    val currentConfig = host.toolConfig(currentTool)

    Box(
        modifier = Modifier
            .width(330.dp)
            .matteShadow(isDark = isDark, elevation = 10.dp, shape = RoundedCornerShape(24.dp))
            .background(if (isDark) PnotesDarkCard else PnotesLightCard)
            .border(0.8.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ==================================================
            // 1. PEN WIDTH SECTION HEADER
            // ==================================================
            Text(
                text = "Pen Width",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) PnotesDarkText1 else PnotesLightText1,
            )

            // ROW 1: 1 px, 2 px, 4 px, 8 px, 12 px
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                penWidthRow1.forEach { preset ->
                    PenWidthPresetItem(
                        preset = preset,
                        isSelected = kotlin.math.abs(currentWidth - preset) < 0.5,
                        isDark = isDark,
                        onClick = { onWidthChange(preset) },
                    )
                }
            }

            // ROW 2: 20 px, 30 px, 50 px, 60 px, 80 px
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                penWidthRow2.forEach { preset ->
                    PenWidthPresetItem(
                        preset = preset,
                        isSelected = kotlin.math.abs(currentWidth - preset) < 0.5,
                        isDark = isDark,
                        onClick = { onWidthChange(preset) },
                    )
                }
            }

            // ==================================================
            // 2. HORIZONTAL DIVIDER
            // ==================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isDark) PnotesDarkBorder else PnotesLightBorder)
            )

            // ==================================================
            // 3. TEXTURES SECTION HEADER
            // ==================================================
            Text(
                text = "Textures",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) PnotesDarkText1 else PnotesLightText1,
            )

            // PRESETS / TEXTURES GRID (4 per row)
            textures.chunked(4).forEach { rowTextures ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rowTextures.forEach { texture ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            val isActive = (currentConfig.texture == texture.textureType) && (currentTool == texture.tool)
                            TextureGridItem(
                                texture = texture,
                                isActive = isActive,
                                isDark = isDark,
                                onClick = {
                                    onSelectTool?.invoke(texture.tool)
                                    val currentCfg = host.toolConfig(texture.tool)
                                    val updatedCfg = texture.configure(currentCfg).copy(
                                        texture = texture.textureType,
                                        baseWidth = currentWidth,
                                    )
                                    host.updateToolConfig(texture.tool, updatedCfg)
                                },
                            )
                        }
                    }
                    // If row is not full, fill remaining slots with empty weighted spacers
                    if (rowTextures.size < 4) {
                        repeat(4 - rowTextures.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PenWidthPresetItem(
    preset: Double,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val dotDp = when (preset) {
        1.0 -> 3.5.dp
        2.0 -> 5.5.dp
        4.0 -> 8.5.dp
        8.0 -> 12.5.dp
        12.0 -> 16.5.dp
        20.0 -> 21.dp
        30.0 -> 25.dp
        50.0 -> 29.dp
        60.0 -> 33.dp
        else -> 37.dp
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) com.pnotes.ui.theme.Motion.BUTTON_PRESS_SCALE else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "penWidthScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(2.5.dp, SelectionPurple, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(dotDp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White else Color(0xFF1F2024)),
            )
        }

        Text(
            text = "${preset.toInt()} px",
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) {
                if (isDark) Color.White else Color(0xFF18181B)
            } else {
                if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
            },
        )
    }
}

@Composable
private fun TextureGridItem(
    texture: BrushTexture,
    isActive: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) com.pnotes.ui.theme.Motion.BUTTON_PRESS_SCALE else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "textureScale",
    )

    val strokeColor = if (isDark) Color.White else Color(0xFF1F2024)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(
                    if (isActive) {
                        Modifier.border(2.5.dp, SelectionPurple, CircleShape)
                    } else {
                        Modifier.border(
                            0.8.dp,
                            if (isDark) Color(0x28FFFFFF) else Color(0x18000000),
                            CircleShape
                        )
                    }
                )
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF262633) else Color(0xFFFAFAFA)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                texture.drawPreview(this, strokeColor, isDark)
            }
        }

        Text(
            text = texture.label,
            fontSize = 10.5.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) {
                if (isDark) Color.White else Color(0xFF18181B)
            } else {
                if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
            },
        )
    }
}

/**
 * High-contrast colorful Eraser icon for active Floating Rail state using new SVG asset.
 */
@Composable
private fun ColorfulEraserIcon(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
) {
    Icon(
        painter = androidx.compose.ui.res.painterResource(com.pnotes.R.drawable.ic_tool_eraser_active),
        contentDescription = "Eraser Active",
        tint = Color.Unspecified,
        modifier = modifier.size(20.dp),
    )
}

