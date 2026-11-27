package com.pnotes.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable shadow paint — allocated once, mutated per-call.
 * Avoids creating a new Paint + shadow layer object every single frame,
 * which was causing GC pressure during drawing.
 */
internal val shadowPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.TRANSPARENT
    isAntiAlias = true
}

/** Public alias for the floating surfaces (backstage cards, home chrome) to reuse the cached paint. */
val toolbarShadowPaint: android.graphics.Paint get() = shadowPaint

/**
 * Universal 3D Elevation & Physical Lighting Modifier for Floating Tabs, Rails, Toolbars and Buttons.
 * Applies multi-layer key & ambient drop shadow, curved tactile surface gradient, and specular bevel rim.
 *
 * Performance: reuses a single Paint for all shadow draws; no per-frame allocation.
 */
fun Modifier.elevated3D(
    isDark: Boolean,
    elevation: Dp = 4.dp,
    cornerRadius: Dp = 12.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    isActive: Boolean = false,
): Modifier = this
    .drawBehind {
        val cornerRadiusPx = cornerRadius.toPx()
        val shadowAlpha = if (isDark) {
            if (isActive) 120 else 80
        } else {
            if (isActive) 55 else 32
        }
        val blurRadius = elevation.toPx() * 1.8f
        val yOffset = elevation.toPx() * 0.85f

        drawIntoCanvas { canvas ->
            synchronized(shadowPaint) {
                shadowPaint.setShadowLayer(
                    blurRadius, 0f, yOffset,
                    android.graphics.Color.argb(shadowAlpha, 0, 0, 0),
                )
                canvas.nativeCanvas.drawRoundRect(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    cornerRadiusPx,
                    cornerRadiusPx,
                    shadowPaint,
                )
            }
        }
    }
    .clip(shape)
    .background(
        if (isDark) {
            if (isActive) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF222A37), // Page 1 elevated
                        Color(0xFF1B2230), // Page 1 surface2
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF171D28), // Page 1 card
                        Color(0xFF101620), // Page 1 sidebar
                    )
                )
            }
        } else {
            if (isActive) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF0F3F8),
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF7F9FC),
                    )
                )
            }
        }
    )
    .border(
        0.8.dp,
        if (isDark) {
            if (isActive) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF007AFF).copy(alpha = 0.60f),
                        Color(0xFF94A3B8).copy(alpha = 0.20f),
                        Color.Black.copy(alpha = 0.40f),
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF94A3B8).copy(alpha = 0.25f),
                        Color(0xFF94A3B8).copy(alpha = 0.10f),
                        Color.Black.copy(alpha = 0.30f),
                    )
                )
            }
        } else {
            if (isActive) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF007AFF).copy(alpha = 0.50f),
                        Color(0xFFE1E6EF),
                        Color(0xFFDCE2EB),
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color.White,
                        Color(0xFFE1E6EF),
                        Color(0xFFDCE2EB),
                    )
                )
            }
        },
        shape,
    )

/**
 * Minimalist 3D pill/chip modifier for floating buttons and tabs.
 */
fun Modifier.floatingPill3D(
    isDark: Boolean,
    isActive: Boolean = false,
    cornerRadius: Dp = 10.dp,
): Modifier = this.elevated3D(
    isDark = isDark,
    elevation = if (isActive) 5.dp else 2.5.dp,
    cornerRadius = cornerRadius,
    isActive = isActive,
)
