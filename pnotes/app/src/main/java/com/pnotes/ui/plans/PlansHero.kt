package com.pnotes.ui.plans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.ui.StationeryDecoration
import com.pnotes.ui.theme.LocalPalette

@Composable
fun PlansHero(
    modifier: Modifier = Modifier,
    showIllustration: Boolean = true,
) {
    val isDark = LocalPalette.current.isDark
    val textPrimary = if (isDark) PlansTokens.DarkTextPrimary else PlansTokens.LightTextPrimary
    val textSecondary = if (isDark) PlansTokens.DarkTextSecondary else PlansTokens.LightTextSecondary
    val accent = PlansTokens.CoralAccent

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (showIllustration) {
            StationeryDecoration(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(width = 170.dp, height = 150.dp)
                    .offset(x = 12.dp, y = (-12).dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Choose your plan",
                color = textPrimary,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            // Glowing monochrome horizontal underline flare
            Canvas(modifier = Modifier.size(width = 140.dp, height = 8.dp)) {
                val w = size.width
                val h = size.height
                // Ambient oval glow
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.50f),
                            accent.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.45f,
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                )
                // Crisp central bright line
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accent,
                            Color.Transparent,
                        ),
                    ),
                    start = Offset(w * 0.08f, h * 0.5f),
                    end = Offset(w * 0.92f, h * 0.5f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round,
                )
            }

            Spacer(Modifier.height(14.dp))

            val subtitleText = buildAnnotatedString {
                append("At PNotes, you get ")
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
                    append("unlimited everything")
                }
                append(", even on the free plan.\nNo limits. No worries. Just your creativity.")
            }

            Text(
                text = subtitleText,
                color = textSecondary,
                fontSize = 15.5.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
