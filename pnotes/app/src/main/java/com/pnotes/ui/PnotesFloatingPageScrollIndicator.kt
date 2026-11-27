package com.pnotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

enum class ScrollIndicatorEdge {
    LEFT,
    RIGHT,
}

/**
 * Smart edge-anchored Floating Page Indicator for PNotes.
 *
 * Appears dynamically when the document is being scrolled or panned, displaying the
 * current page number in a clean floating capsule anchored flush to the screen edge.
 * Automatically fades out smoothly when scrolling pauses.
 * Also supports vertical dragging / scrubbing to jump across pages rapidly.
 */
@Composable
fun PnotesFloatingPageScrollIndicator(
    pageIndex: Int,
    pageCount: Int,
    scrollFraction: Float,
    isScrolling: Boolean,
    onScrubToPage: (Int) -> Unit = {},
    edge: ScrollIndicatorEdge = ScrollIndicatorEdge.LEFT,
    isDark: Boolean = false,
    topInsetDp: Float = 60f,
    bottomInsetDp: Float = 80f,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 1) return

    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val activeFraction = if (isDragging) dragFraction else scrollFraction.coerceIn(0f, 1f)
    val displayPage = if (isDragging) {
        (dragFraction * (pageCount - 1)).roundToInt().coerceIn(0, pageCount - 1) + 1
    } else {
        (pageIndex + 1).coerceIn(1, pageCount)
    }

    val visible = isScrolling || isDragging

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .zIndex(25f),
    ) {
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val topPaddingPx = with(density) { topInsetDp.dp.toPx() }
        val bottomPaddingPx = with(density) { bottomInsetDp.dp.toPx() }
        val travelTrackPx = (totalHeightPx - topPaddingPx - bottomPaddingPx).coerceAtLeast(100f)

        val targetY = topPaddingPx + (activeFraction * travelTrackPx)

        val animatedY by animateFloatAsState(
            targetValue = targetY,
            animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh) else spring(stiffness = Spring.StiffnessMediumLow),
            label = "scrollIndicatorY",
        )

        val isLeft = edge == ScrollIndicatorEdge.LEFT
        val pillShape = if (isLeft) {
            RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 0.dp, bottomEnd = 0.dp)
        }

        val pillBg = if (isDark) Color(0xFF1E2128) else Color.White
        val textColor = if (isDark) Color.White else Color(0xFF09090B)
        val borderColor = if (isDark) Color.White.copy(alpha = 0.20f) else Color(0x18000000)

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { if (isLeft) -it else it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutHorizontally(
                targetOffsetX = { if (isLeft) -it else it },
                animationSpec = tween(280),
            ) + fadeOut(animationSpec = tween(280)),
            modifier = Modifier
                .align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
                .offset { IntOffset(0, animatedY.roundToInt()) },
        ) {
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isDark) 8.dp else 6.dp,
                        shape = pillShape,
                        spotColor = Color.Black.copy(alpha = 0.25f),
                        ambientColor = Color.Black.copy(alpha = 0.15f),
                    )
                    .clip(pillShape)
                    .background(pillBg)
                    .border(1.dp, borderColor, pillShape)
                    .pointerInput(pageCount) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragFraction = activeFraction
                            },
                            onDragEnd = {
                                isDragging = false
                                val targetPage = (dragFraction * (pageCount - 1)).roundToInt().coerceIn(0, pageCount - 1)
                                onScrubToPage(targetPage)
                            },
                            onDragCancel = {
                                isDragging = false
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragFraction = (dragFraction + dragAmount / travelTrackPx).coerceIn(0f, 1f)
                                val targetPage = (dragFraction * (pageCount - 1)).roundToInt().coerceIn(0, pageCount - 1)
                                onScrubToPage(targetPage)
                            },
                        )
                    }
                    .padding(
                        start = if (isLeft) 10.dp else 14.dp,
                        end = if (isLeft) 14.dp else 10.dp,
                        top = 6.dp,
                        bottom = 6.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$displayPage",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeight = 13.5.sp,
                    ),
                )
            }
        }
    }
}
