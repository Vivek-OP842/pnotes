@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.pnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.canvas.EditingField
import com.pnotes.canvas.HandleId
import com.pnotes.core.pal.FontFace
import com.pnotes.platform.FontCatalog
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.toComposeColor
import kotlin.math.abs
import kotlin.math.roundToInt

private val composeFamilies = java.util.concurrent.ConcurrentHashMap<String, FontFamily>()

/** Drop memoized Compose families after a font import/removal changed resolution. */
internal fun invalidateComposeFamilies() = composeFamilies.clear()

/** Maps a [FontFace] to a Compose family: generic tokens directly, the rest via the catalog. */
internal fun FontFace.toComposeFamily(): FontFamily = when (this) {
    FontFace.SANS -> FontFamily.SansSerif
    FontFace.SERIF -> FontFamily.Serif
    FontFace.MONO -> FontFamily.Monospace
    FontFace.HAND -> FontFamily.Cursive
    else -> composeFamilies.getOrPut(id) {
        FontFamily(FontCatalog.resolve(this, bold = false, italic = false).typeface)
    }
}

/**
 * Swallows the text field's bring-into-view requests. On focus (and every caret move) Compose
 * asks the platform to reveal the caret via requestRectangleOnScreen; with the keyboard open in
 * an edge-to-edge window, ViewRootImpl answers by panning the whole window, which visibly lifts
 * a box taller than the viewport. Dispatch stops at the nearest [BringIntoViewModifierNode], so
 * a no-op ancestor keeps the request from ever reaching the platform: the page must never move
 * on its own during a box edit.
 */
private object NoBringIntoViewElement : ModifierNodeElement<NoBringIntoViewNode>() {
    override fun create() = NoBringIntoViewNode()
    override fun update(node: NoBringIntoViewNode) {}
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = other === this
}

private class NoBringIntoViewNode : Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(childCoordinates: LayoutCoordinates, boundsProvider: () -> Rect?) {}
}

/**
 * In-place text editor with 3D side-by-side Move Grip & Rotate Button and 8 interactive resizing handle points.
 */
@Composable
fun TextEditorOverlay(editor: Editor, field: EditingField) {
    val density = LocalDensity.current
    val palette = LocalPalette.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var value by remember { mutableStateOf(TextFieldValue(field.text, TextRange(field.text.length))) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(field.x, field.y) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(field.text) {
        if (field.text != value.text) {
            val cursor = value.selection.start.coerceIn(0, field.text.length)
            value = TextFieldValue(field.text, TextRange(cursor))
        }
    }

    val z = field.zoom.toFloat()
    val widthDp = with(density) { field.width.toInt().toDp() }
    val heightDp = with(density) { field.height.toFloat().toDp() }
    val fontSp = with(density) { field.fontPx.toFloat().toSp() }
    val accentColor = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)

    Layout(content = {
        Box(
            modifier = Modifier
                .then(NoBringIntoViewElement)
                .graphicsLayer(
                    scaleX = z,
                    scaleY = z,
                    rotationZ = field.rotation.toFloat(),
                    transformOrigin = TransformOrigin(0f, 0f),
                )
                .width(widthDp)
                .heightIn(min = heightDp)
        ) {
            // Text input area
            BasicTextField(
                value = value,
                onValueChange = {
                    value = it
                    editor.updateEditingText(it.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = heightDp)
                    .border(
                        width = Dp(1.8f / z),
                        color = accentColor.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(Dp(6f / z)),
                    )
                    .background(
                        color = accentColor.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(Dp(6f / z)),
                    )
                    .padding(
                        horizontal = Dp(8f / z),
                        vertical = Dp(6f / z),
                    )
                    .focusRequester(focusRequester)
                    .onGloballyPositioned { coords = it }
                    .pointerInput(Unit) {
                        val slop = viewConfiguration.touchSlop
                        val longPress = viewConfiguration.longPressTimeoutMillis
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            if (down.type != PointerType.Touch) return@awaitEachGesture
                            val lc = coords ?: return@awaitEachGesture
                            var lastRoot = lc.localToRoot(down.position)
                            var dragging = false
                            var yielded = false
                            var totalX = 0f
                            var totalY = 0f
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) { if (dragging) change.consume(); break }
                                if (yielded) continue
                                val cur = (coords ?: lc).localToRoot(change.position)
                                val dx = cur.x - lastRoot.x
                                val dy = cur.y - lastRoot.y
                                lastRoot = cur
                                totalX += dx
                                totalY += dy
                                if (!dragging) {
                                    if (abs(totalX) <= slop && abs(totalY) <= slop) continue
                                    if (change.uptimeMillis - down.uptimeMillis >= longPress) { yielded = true; continue }
                                    dragging = true
                                }
                                editor.panWhileEditing(dx, dy)
                                change.consume()
                            }
                        }
                    },
                textStyle = TextStyle(
                    color = field.rgba.toComposeColor(),
                    fontFamily = field.face.toComposeFamily(),
                    fontSize = fontSp,
                ),
                cursorBrush = SolidColor(accentColor),
            )

            // 3D Side-by-Side Control Bar (Move Grip + 3D Rotate Button)
            TextControlBar(
                zoom = z,
                accentColor = accentColor,
                onMove = { dx, dy ->
                    editor.moveEditingText(dx, dy)
                },
                onRotate = { deltaDeg ->
                    editor.rotateEditingText(deltaDeg)
                }
            )

            // 8 Resizing points (4 Corners: Scale font + box, 4 Edges: Wrap width / box height)
            TextResizeHandles(
                zoom = z,
                accentColor = accentColor,
                onResize = { handle, dx, dy ->
                    editor.resizeEditingText(handle, dx, dy)
                }
            )
        }
    }) { measurables, constraints ->
        val placeable = measurables[0].measure(Constraints())
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(field.x.roundToInt(), field.y.roundToInt())
        }
    }
}

/**
 * 3D Side-by-Side Control Bar:
 * Combines Move Grip pill and 3D Rotate Button side-by-side into a sleek floating capsule.
 * - Move Section: Drag with finger/stylus to reposition text box anywhere on page.
 * - 3D Rotate Section: Tap for instant 90° turn, or drag around center for ultra-smooth 360° rotation with angle snapping.
 */
@Composable
private fun BoxScope.TextControlBar(
    zoom: Float,
    accentColor: Color,
    onMove: (Double, Double) -> Unit,
    onRotate: (Double) -> Unit,
) {
    val barHeight = Dp(22f / zoom).coerceIn(18.dp, 28.dp)
    val floatOffset = Dp(-24f / zoom).coerceIn((-32).dp, (-16).dp)
    val cornerRadius = Dp(11f / zoom).coerceAtLeast(6.dp)
    val borderWidth = Dp(1.5f / zoom).coerceAtLeast(1.dp)
    val dotSize = Dp(2.6f / zoom).coerceIn(1.8.dp, 3.4.dp)
    val rotBtnSize = Dp(18f / zoom).coerceIn(15.dp, 24.dp)
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = floatOffset)
            .height(barHeight)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color(0x33000000),
                spotColor = Color(0x40000000),
            )
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White,
                        Color(0xFFF4F4F7),
                    )
                ),
                shape = RoundedCornerShape(cornerRadius),
            )
            .border(
                width = borderWidth,
                color = accentColor.copy(alpha = 0.85f),
                shape = RoundedCornerShape(cornerRadius),
            )
            .padding(horizontal = Dp(7f / zoom).coerceIn(5.dp, 10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dp(6f / zoom).coerceIn(4.dp, 8.dp)),
        ) {
            // --- 3D MOVE GRIP BUTTON ---
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onMove(dragAmount.x.toDouble(), dragAmount.y.toDouble())
                        }
                    }
                    .padding(vertical = 2.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dp(3.5f / zoom).coerceIn(2.dp, 4.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .shadow(0.5.dp, CircleShape)
                                .background(accentColor, CircleShape)
                        )
                    }
                }
            }

            // --- SUBTLE VERTICAL DIVIDER ---
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(Dp(12f / zoom).coerceIn(9.dp, 16.dp))
                    .background(accentColor.copy(alpha = 0.35f))
            )

            // --- 3D ROTATE BUTTON ---
            var dragPos by remember { mutableStateOf(Offset.Zero) }
            var lastAngle by remember { mutableStateOf(0.0) }
            var totalDragDist by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .size(rotBtnSize)
                    .shadow(elevation = 1.5.dp, shape = CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White,
                                accentColor.copy(alpha = 0.14f),
                            )
                        ),
                        shape = CircleShape,
                    )
                    .border(
                        width = Dp(1f / zoom).coerceAtLeast(0.8.dp),
                        color = accentColor.copy(alpha = 0.65f),
                        shape = CircleShape,
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                totalDragDist = 0f
                                dragPos = offset
                                val vecX = offset.x - (rotBtnSize.toPx() / 2f) + 20f
                                val vecY = offset.y - (rotBtnSize.toPx() / 2f) - 30f
                                lastAngle = Math.atan2(vecY.toDouble(), vecX.toDouble()) * 180.0 / Math.PI
                            },
                            onDragEnd = {
                                if (totalDragDist < 8f * density.density) {
                                    onRotate(90.0)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragDist += Math.abs(dragAmount.x) + Math.abs(dragAmount.y)
                                dragPos += dragAmount
                                val vecX = dragPos.x - (rotBtnSize.toPx() / 2f) + 20f
                                val vecY = dragPos.y - (rotBtnSize.toPx() / 2f) - 30f
                                val curAngle = Math.atan2(vecY.toDouble(), vecX.toDouble()) * 180.0 / Math.PI
                                var delta = curAngle - lastAngle
                                if (delta > 180.0) delta -= 360.0
                                if (delta < -180.0) delta += 360.0
                                lastAngle = curAngle
                                if (Math.abs(delta) > 0.05 && Math.abs(delta) < 45.0) {
                                    onRotate(delta)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text(
                    text = "↻",
                    color = accentColor,
                    style = TextStyle(
                        fontSize = with(LocalDensity.current) { Dp(11.5f / zoom).coerceIn(9.dp, 15.dp).toSp() },
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
        }
    }
}

/**
 * 8 Modern Resize Handles (Figma / GoodNotes / Canva style):
 * - 4 Corners: Circular dots (proportional scaling of font size + box)
 * - 2 Width Edges: Vertical capsule pills (text reflow / width adjustment)
 * - 2 Height Edges: Horizontal capsule pills (vertical height adjustment)
 */
@Composable
private fun BoxScope.TextResizeHandles(
    zoom: Float,
    accentColor: Color,
    onResize: (HandleId, Double, Double) -> Unit,
) {
    val cornerVisualSize = Dp(10f / zoom).coerceIn(7.dp, 14.dp)
    val pillLongSide = Dp(14f / zoom).coerceIn(10.dp, 18.dp)
    val pillShortSide = Dp(4.5f / zoom).coerceIn(3.dp, 6.dp)
    val touchSize = Dp(28f / zoom).coerceIn(22.dp, 38.dp)
    val borderWidth = Dp(1.6f / zoom).coerceAtLeast(1.dp)

    val handles = listOf(
        HandleId.TL to Alignment.TopStart,
        HandleId.T to Alignment.TopCenter,
        HandleId.TR to Alignment.TopEnd,
        HandleId.R to Alignment.CenterEnd,
        HandleId.BR to Alignment.BottomEnd,
        HandleId.B to Alignment.BottomCenter,
        HandleId.BL to Alignment.BottomStart,
        HandleId.L to Alignment.CenterStart,
    )

    for ((handleId, alignment) in handles) {
        val offsetX = when (alignment) {
            Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> -touchSize / 2
            Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd -> touchSize / 2
            else -> 0.dp
        }
        val offsetY = when (alignment) {
            Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd -> -touchSize / 2
            Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd -> touchSize / 2
            else -> 0.dp
        }

        val isCorner = handleId == HandleId.TL || handleId == HandleId.TR || handleId == HandleId.BL || handleId == HandleId.BR
        val isHorizontalPill = handleId == HandleId.T || handleId == HandleId.B
        val isVerticalPill = handleId == HandleId.L || handleId == HandleId.R

        Box(
            modifier = Modifier
                .align(alignment)
                .offset(x = offsetX, y = offsetY)
                .size(touchSize)
                .pointerInput(handleId) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onResize(handleId, dragAmount.x.toDouble(), dragAmount.y.toDouble())
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            when {
                isCorner -> {
                    // Crisp circular handle for proportional font/box scaling
                    Box(
                        modifier = Modifier
                            .size(cornerVisualSize)
                            .shadow(elevation = 2.dp, shape = CircleShape)
                            .background(Color.White, CircleShape)
                            .border(borderWidth, accentColor, CircleShape)
                    )
                }
                isVerticalPill -> {
                    // Vertical capsule pill for width reflow
                    Box(
                        modifier = Modifier
                            .size(width = pillShortSide, height = pillLongSide)
                            .shadow(elevation = 1.5.dp, shape = CircleShape)
                            .background(Color.White, CircleShape)
                            .border(borderWidth, accentColor, CircleShape)
                    )
                }
                isHorizontalPill -> {
                    // Horizontal capsule pill for height adjustment
                    Box(
                        modifier = Modifier
                            .size(width = pillLongSide, height = pillShortSide)
                            .shadow(elevation = 1.5.dp, shape = CircleShape)
                            .background(Color.White, CircleShape)
                            .border(borderWidth, accentColor, CircleShape)
                    )
                }
            }
        }
    }
}
