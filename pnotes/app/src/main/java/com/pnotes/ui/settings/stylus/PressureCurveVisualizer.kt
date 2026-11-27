package com.pnotes.ui.settings.stylus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import com.pnotes.ui.PnotesTokens
import com.pnotes.ui.theme.LocalPalette

// Stylus visualizer monochrome theme
@Composable
private fun getVisualizerMonochromeAccent(isDark: Boolean): Color =
    if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)

/**
 * High-performance 2D transfer function graph visualizer rendering $y = x^\gamma$ in real-time.
 *
 * @param gamma Transfer exponent ($\gamma \in [0.5, 2.5]$).
 * @param livePressure Current normalized raw stylus pressure [0.0f .. 1.0f].
 * @param modifier Canvas modifier.
 */
@Composable
fun PressureCurveVisualizer(
    gamma: Float,
    livePressure: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalPalette.current.isDark
    val animatedLivePressure by animateFloatAsState(
        targetValue = livePressure.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 800f),
        label = "pressure_dot"
    )

    val graphBg = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val gridLineColor = if (isDark) Color(0x18FFFFFF) else Color(0x14000000)
    val referenceLineColor = if (isDark) Color(0x3588A0C0) else Color(0x3564748B)
    val themeAccent = getVisualizerMonochromeAccent(isDark)
    val curveSecondaryColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A)

    val curveDescription = remember(gamma) {
        when {
            gamma < 0.95f -> "Soft Touch — Quick wide strokes with light force"
            gamma in 0.95f..1.05f -> "Linear — 1:1 Direct proportional response"
            gamma in 1.06f..1.75f -> "Balanced Dynamic — Natural handwriting response"
            else -> "Firm Press — Deliberate pressure needed for broad lines"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .shadow(elevation = if (isDark) 0.dp else 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x20000000))
            .clip(RoundedCornerShape(16.dp))
            .background(graphBg)
            .border(1.dp, PnotesTokens.cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padL = 36f
            val padB = 30f
            val padR = 16f
            val padT = 16f

            val graphW = size.width - padL - padR
            val graphH = size.height - padT - padB

            if (graphW <= 0f || graphH <= 0f) return@Canvas

            // 1. Draw Grid Lines (0%, 25%, 50%, 75%, 100%)
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val fraction = i.toFloat() / gridSteps
                val x = padL + fraction * graphW
                val y = padT + (1f - fraction) * graphH

                // Vertical grid lines
                drawLine(
                    color = gridLineColor,
                    start = Offset(x, padT),
                    end = Offset(x, padT + graphH),
                    strokeWidth = 1f
                )

                // Horizontal grid lines
                drawLine(
                    color = gridLineColor,
                    start = Offset(padL, y),
                    end = Offset(padL + graphW, y),
                    strokeWidth = 1f
                )
            }

            // 2. Draw Linear Reference (Dashed diagonal y = x)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            drawLine(
                color = referenceLineColor,
                start = Offset(padL, padT + graphH),
                end = Offset(padL + graphW, padT),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )

            // 3. Construct the non-linear Gamma curve: y = x^gamma
            val curvePath = Path()
            val fillPath = Path()

            val numSamples = 60
            fillPath.moveTo(padL, padT + graphH)

            for (step in 0..numSamples) {
                val inputX = step.toFloat() / numSamples
                val outputY = inputX.toDouble().pow(gamma.toDouble()).toFloat().coerceIn(0f, 1f)

                val plotX = padL + inputX * graphW
                val plotY = padT + (1f - outputY) * graphH

                if (step == 0) {
                    curvePath.moveTo(plotX, plotY)
                    fillPath.lineTo(plotX, plotY)
                } else {
                    curvePath.lineTo(plotX, plotY)
                    fillPath.lineTo(plotX, plotY)
                }
            }

            fillPath.lineTo(padL + graphW, padT + graphH)
            fillPath.close()

            // 4. Shaded Area Under the Curve (Gradient Fill)
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        themeAccent.copy(alpha = if (isDark) 0.22f else 0.16f),
                        curveSecondaryColor.copy(alpha = if (isDark) 0.08f else 0.05f),
                        Color.Transparent
                    ),
                    startY = padT,
                    endY = padT + graphH
                )
            )

            // 5. Draw Glowing Gradient Curve Stroke
            drawPath(
                path = curvePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(curveSecondaryColor, themeAccent),
                    startX = padL,
                    endX = padL + graphW
                ),
                style = Stroke(width = 3.5f)
            )

            // 6. Live Stylus Tracking Point
            if (animatedLivePressure > 0.005f) {
                val dotX = animatedLivePressure.coerceIn(0f, 1f)
                val dotY = dotX.toDouble().pow(gamma.toDouble()).toFloat().coerceIn(0f, 1f)

                val center = Offset(padL + dotX * graphW, padT + (1f - dotY) * graphH)
                val liveDotColor = if (isDark) Color.White else Color(0xFF09090B)

                // Outer ambient glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(liveDotColor.copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = 24f
                    ),
                    radius = 24f,
                    center = center
                )

                // Inner bright pulse core
                drawCircle(
                    color = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5),
                    radius = 5.5f,
                    center = center
                )
                drawCircle(
                    color = liveDotColor,
                    radius = 4f,
                    center = center
                )

                // Crosshair coordinate lines to axes
                drawLine(
                    color = liveDotColor.copy(alpha = 0.35f),
                    start = Offset(center.x, padT + graphH),
                    end = center,
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = liveDotColor.copy(alpha = 0.35f),
                    start = Offset(padL, center.y),
                    end = center,
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
            }
        }

        // Top-Right Formula & Description Badge
        Text(
            text = "y = x^${String.format(java.util.Locale.US, "%.2f", gamma)}",
            color = themeAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Bottom-Left Axis Description
        Text(
            text = curveDescription,
            color = PnotesTokens.textSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
