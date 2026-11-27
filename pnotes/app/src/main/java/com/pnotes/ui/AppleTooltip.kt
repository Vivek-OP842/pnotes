package com.pnotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.pnotes.ui.theme.LocalPalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TooltipPosition {
    BOTTOM, // For top bar & toolbar (tooltip strictly below anchor)
    TOP,    // For bottom bar (tooltip strictly above anchor)
    RIGHT,  // For left side rails (tooltip strictly to the right of anchor)
    LEFT,   // For right side rails (tooltip strictly to the left of anchor)
}

/**
 * Accurate PopupPositionProvider that places tooltips strictly outside the anchor bounds,
 * ensuring icons/tools are never obscured, with window-edge clamping.
 */
class AppleTooltipPositionProvider(
    private val position: TooltipPosition,
    private val marginPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        return when (position) {
            TooltipPosition.BOTTOM -> {
                val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
                    .coerceIn(marginPx, (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx))
                val y = anchorBounds.bottom + marginPx
                IntOffset(x, y)
            }
            TooltipPosition.TOP -> {
                val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
                    .coerceIn(marginPx, (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx))
                val y = (anchorBounds.top - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
                IntOffset(x, y)
            }
            TooltipPosition.RIGHT -> {
                val x = anchorBounds.right + marginPx
                val y = (anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2)
                    .coerceIn(marginPx, (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx))
                IntOffset(x, y)
            }
            TooltipPosition.LEFT -> {
                val x = (anchorBounds.left - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
                val y = (anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2)
                    .coerceIn(marginPx, (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx))
                IntOffset(x, y)
            }
        }
    }
}

/**
 * Apple-style Glassmorphic Tooltip container for hover detection.
 * - Triggers only on mouse / stylus (S-Pen, Apple Pencil, Wacom) / pointer hover.
 * - Dismisses instantly on touch tap, click, or pointer exit (never gets stuck).
 * - Debounced (180ms) to prevent flickering when sweeping across toolbar.
 * - Positioned strictly outside anchor (below / right / above) with margin, so tool is never covered.
 */
@Composable
fun AppleTooltip(
    tooltip: String?,
    modifier: Modifier = Modifier,
    position: TooltipPosition = TooltipPosition.BOTTOM,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    if (tooltip.isNullOrBlank()) {
        content()
        return
    }

    var isHovered by remember { mutableStateOf(false) }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Dismiss immediately when clicked or pressed
    LaunchedEffect(isPressed) {
        if (isPressed) {
            isHovered = false
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val density = LocalDensity.current
    val marginPx = with(density) { 8.dp.roundToPx() }
    val positionProvider = remember(position, marginPx) {
        AppleTooltipPositionProvider(position, marginPx)
    }

    Box(
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(tooltip) {
                awaitPointerEventScope {
                    var hoverJob: Job? = null
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val isTouch = event.changes.any { it.type == PointerType.Touch }
                        val isHoverDevice = event.changes.any {
                            it.type == PointerType.Mouse || it.type == PointerType.Stylus || it.type == PointerType.Eraser
                        }

                        if (isTouch) {
                            // Touch events (finger taps/drags) should NEVER show hover tooltip or get stuck
                            hoverJob?.cancel()
                            isHovered = false
                            continue
                        }

                        when (event.type) {
                            PointerEventType.Enter -> {
                                if (isHoverDevice && !isPressed) {
                                    hoverJob?.cancel()
                                    hoverJob = coroutineScope.launch {
                                        delay(180)
                                        isHovered = true
                                    }
                                }
                            }
                            PointerEventType.Exit -> {
                                hoverJob?.cancel()
                                isHovered = false
                            }
                            PointerEventType.Move -> {
                                if (isHoverDevice && !isPressed && !isHovered && hoverJob?.isActive != true) {
                                    hoverJob = coroutineScope.launch {
                                        delay(180)
                                        isHovered = true
                                    }
                                }
                            }
                            PointerEventType.Press -> {
                                // Instantly dismiss when clicked
                                hoverJob?.cancel()
                                isHovered = false
                            }
                            PointerEventType.Release -> {
                                if (isTouch) {
                                    hoverJob?.cancel()
                                    isHovered = false
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content()

        if (isHovered && !isPressed) {
            Popup(
                popupPositionProvider = positionProvider,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    clippingEnabled = true,
                ),
            ) {
                AnimatedVisibility(
                    visible = isHovered && !isPressed,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(initialScale = com.pnotes.ui.theme.Motion.ENTER_SCALE, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)),
                    exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                            scaleOut(targetScale = com.pnotes.ui.theme.Motion.EXIT_SCALE, animationSpec = spring(stiffness = Spring.StiffnessHigh)),
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(elevation = 8.dp, shape = RoundedCornerShape(7.dp), spotColor = Color(0x55000000))
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isDark) Color(0xF51C1C1E) else Color(0xFAFFFFFF))
                            .border(
                                width = 0.5.dp,
                                color = if (isDark) Color(0x38FFFFFF) else Color(0x28000000),
                                shape = RoundedCornerShape(7.dp),
                            )
                            .padding(horizontal = 9.dp, vertical = 4.5.dp),
                    ) {
                        Text(
                            text = tooltip,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.01).sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
