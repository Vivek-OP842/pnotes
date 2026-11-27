package com.pnotes.ui.settings.stylus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.ui.PnotesTokens
import com.pnotes.ui.theme.LocalPalette

/**
 * Dual real-time gauge displaying raw hardware pressure and calibrated inking stroke width.
 */
@Composable
fun PressureGauge(
    rawPressure: Float,
    calibratedPressure: Float,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalPalette.current.isDark
    val themeAccent = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    val secondaryAccent = if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A)
    val animatedRaw by animateFloatAsState(
        targetValue = rawPressure.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 900f),
        label = "raw_pressure_bar"
    )
    val animatedCalibrated by animateFloatAsState(
        targetValue = calibratedPressure.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 900f),
        label = "calibrated_pressure_bar"
    )

    val rawPercent = (animatedRaw * 100).toInt()
    val calPercent = (animatedCalibrated * 100).toInt()

    val forceTier = when {
        rawPressure <= 0.01f -> "Idle / Hover"
        rawPressure < 0.25f -> "Light Touch"
        rawPressure < 0.65f -> "Moderate Force"
        else -> "Firm / Heavy Force"
    }

    val trackBg = if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
            .border(1.dp, PnotesTokens.cardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header with status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (rawPressure > 0.01f) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFF52525B) else Color(0xFF94A3B8)))
                    )
                    Text(
                        text = "Live Stylus Pressure Meter",
                        color = PnotesTokens.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = forceTier,
                    color = if (rawPressure > 0.01f) themeAccent else PnotesTokens.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 1. Raw Pressure Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Raw Sensor Pressure", fontSize = 11.5.sp, color = PnotesTokens.textSecondary)
                    Text("$rawPercent%", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = themeAccent)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(trackBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedRaw)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(secondaryAccent, themeAccent)
                                )
                            )
                    )
                }
            }

            // 2. Calibrated Stroke Width Output Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calibrated Stroke Output", fontSize = 11.5.sp, color = PnotesTokens.textSecondary)
                    Text("$calPercent%", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = themeAccent)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(trackBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedCalibrated)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(secondaryAccent, themeAccent)
                                )
                            )
                    )
                }
            }
        }
    }
}
