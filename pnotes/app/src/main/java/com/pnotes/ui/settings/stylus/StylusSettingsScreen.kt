package com.pnotes.ui.settings.stylus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pnotes.settings.stylus.StylusSettingsViewModel
import com.pnotes.ui.PnotesTokens
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.PurpleTheme
import com.pnotes.ui.theme.toComposeColor


/**
 * Full-featured Stylus Inking Engine & Pressure Sensitivity Calibration Settings Screen.
 * Fully styled to seamlessly match the PNotes visual identity and dynamic Light/Dark theme.
 */
@Composable
fun StylusSettingsScreen(
    onBack: () -> Unit,
    compact: Boolean = false,
    sidebarOpen: Boolean = true,
    onShowSidebar: () -> Unit = {},
    viewModel: StylusSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val settings by viewModel.settings.collectAsState()
    val liveRawPressure by viewModel.liveRawPressure.collectAsState()
    val liveCalibratedPressure by viewModel.liveCalibratedPressure.collectAsState()
    val liveVelocity by viewModel.liveVelocity.collectAsState()
    val liveStrokeWidth by viewModel.liveStrokeWidth.collectAsState()
    val clearSignalKey by viewModel.scratchpadClearKey.collectAsState()

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg.toComposeColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // === 1. TOP HEADER (Unified with Preferences & Backstage style) ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (compact) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = PnotesIcons.prev,
                            contentDescription = "Back",
                            tint = PnotesTokens.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                } else if (!sidebarOpen) {
                    IconButton(onClick = onShowSidebar) {
                        Icon(
                            imageVector = PnotesIcons.menu,
                            contentDescription = "Show sidebar",
                            tint = PnotesTokens.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                } else {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = PnotesIcons.prev,
                            contentDescription = "Back",
                            tint = PnotesTokens.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }

                Column {
                    Text(
                        text = "Stylus & Pressure Calibration",
                        color = PnotesTokens.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Fine-tune S-Pen & active stylus inking dynamics",
                        color = PnotesTokens.textSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                // Reset to Defaults Button
                TextButton(
                    onClick = { viewModel.resetToDefaults() },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = "Reset to defaults",
                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // === SCROLLABLE SETTINGS CONTENT ===
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // === 2. TRANSFER FUNCTION & PRESSURE CALIBRATION SECTION ===
                SectionCard(
                    title = "Pressure Transfer Curve (Gamma Calibration)",
                    subtitle = "Adjust response curve between light feather strokes and heavy force"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // 2D Live Curve Visualizer
                        PressureCurveVisualizer(
                            gamma = settings.pressureGamma,
                            livePressure = liveRawPressure,
                        )

                        // Gamma Sensitivity Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pressure Gamma: ${String.format(java.util.Locale.US, "%.2f", settings.pressureGamma)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PnotesTokens.textPrimary
                                )
                                Text(
                                    text = when {
                                        settings.pressureGamma < 0.95f -> "Soft (Light Touch)"
                                        settings.pressureGamma in 0.95f..1.05f -> "Linear (1:1)"
                                        settings.pressureGamma in 1.06f..1.75f -> "Balanced"
                                        else -> "Firm (Heavy Press)"
                                    },
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF09090B),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            Slider(
                                value = settings.pressureGamma,
                                onValueChange = { viewModel.setPressureGamma(it) },
                                valueRange = 0.5f..2.5f,
                                colors = PurpleTheme.sliderColors(isDark),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Quick Presets Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PresetChip("Soft (0.8)", isSelected = kotlin.math.abs(settings.pressureGamma - 0.8f) < 0.05f, modifier = Modifier.weight(1f)) {
                                    viewModel.setPressureGamma(0.8f)
                                }
                                PresetChip("Linear (1.0)", isSelected = kotlin.math.abs(settings.pressureGamma - 1.0f) < 0.05f, modifier = Modifier.weight(1f)) {
                                    viewModel.setPressureGamma(1.0f)
                                }
                                PresetChip("StarNote (1.45)", isSelected = kotlin.math.abs(settings.pressureGamma - 1.45f) < 0.05f, modifier = Modifier.weight(1f)) {
                                    viewModel.setPressureGamma(1.45f)
                                }
                                PresetChip("Firm (2.2)", isSelected = kotlin.math.abs(settings.pressureGamma - 2.2f) < 0.05f, modifier = Modifier.weight(1f)) {
                                    viewModel.setPressureGamma(2.2f)
                                }
                            }
                        }

                        // Live Stylus Pressure Gauge Meter
                        PressureGauge(
                            rawPressure = liveRawPressure,
                            calibratedPressure = liveCalibratedPressure,
                        )
                    }
                }

                // === 3. INTERACTIVE INKING TEST SCRATCHPAD ===
                SectionCard(
                    title = "Interactive Test Scratchpad",
                    subtitle = "Test your stylus inking, handwriting response, and line dynamics directly"
                ) {
                    StylusTestCanvas(
                        settings = settings,
                        clearSignalKey = clearSignalKey,
                        onLiveTelemetry = { raw, cal, vel, width ->
                            viewModel.updateLiveTelemetry(raw, cal, vel, width)
                        },
                        onPenSelected = { viewModel.setPenType(it) },
                        onBaseWidthChanged = { viewModel.setBaseStrokeWidth(it) },
                        onColorChanged = { viewModel.setStrokeColor(it) },
                        onClearClicked = { viewModel.clearScratchpad() }
                    )
                }

                // === 4. HARDWARE & INKING ENGINE TOGGLES ===
                SectionCard(
                    title = "Digitizer & Motion Processing",
                    subtitle = "Configure hardware touch handling, speed tapering, and anti-jitter filters"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Stylus Only Mode (Palm Rejection)
                        ToggleRow(
                            title = "Stylus Only Mode (Hardware Palm Rejection)",
                            subtitle = "Reject all finger & palm touches while inking with an active stylus",
                            checked = settings.stylusOnlyMode,
                            onCheckedChange = { viewModel.setStylusOnlyMode(it) }
                        )

                        // Velocity Tapering
                        ToggleRow(
                            title = "Velocity Tapering",
                            subtitle = "Dynamically thin strokes drawn at higher handwriting speeds for natural flourishes",
                            checked = settings.velocityTaperingEnabled,
                            onCheckedChange = { viewModel.setVelocityTapering(it) }
                        )

                        // Stroke Stabilization (Anti-Jitter) Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                    Text(
                                        text = "Stroke Stabilization (Anti-Jitter)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PnotesTokens.textPrimary
                                    )
                                    Text(
                                        text = "Low-pass exponential moving average filter on digitizer coordinates",
                                        fontSize = 11.5.sp,
                                        color = PnotesTokens.textSecondary
                                    )
                                }
                                Text(
                                    text = "${(settings.strokeStabilization * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
                                )
                            }

                            Slider(
                                value = settings.strokeStabilization,
                                onValueChange = { viewModel.setStrokeStabilization(it) },
                                valueRange = 0.0f..1.0f,
                                colors = PurpleTheme.sliderColors(isDark),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // === 5. LIVE TELEMETRY HUD READOUT ===
                SectionCard(
                    title = "Hardware Digitizer Telemetry",
                    subtitle = "Real-time hardware sensor readings from your tablet digitizer"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryStat("Raw Pressure", "${(liveRawPressure * 100).toInt()}%")
                        TelemetryStat("Calibrated Width", "${String.format(java.util.Locale.US, "%.1f", liveStrokeWidth)} px")
                        TelemetryStat("Velocity", "${String.format(java.util.Locale.US, "%.2f", liveVelocity)} px/ms")
                        TelemetryStat("Tool Instrument", settings.selectedPenType.displayName)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    val isDark = LocalPalette.current.isDark
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isDark) 0.dp else 2.dp, RoundedCornerShape(18.dp), spotColor = Color(0x20000000))
            .clip(RoundedCornerShape(18.dp))
            .background(PnotesTokens.cardBackground)
            .border(1.dp, PnotesTokens.cardBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PnotesTokens.textPrimary,
                    letterSpacing = (-0.3).sp
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = PnotesTokens.textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isDark = LocalPalette.current.isDark
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) (if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5)) else (if (isDark) Color(0xFF18181B) else Color.White),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0x28FFFFFF) else Color(0xFFE2E8F0))
        ),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) (if (isDark) Color.White else Color(0xFF09090B)) else PnotesTokens.textSecondary,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val isDark = LocalPalette.current.isDark
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = PnotesTokens.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.5.sp, color = PnotesTokens.textSecondary, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = PurpleTheme.switchColors(isDark)
        )
    }
}

@Composable
private fun TelemetryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = PnotesTokens.textSecondary)
        Spacer(Modifier.height(3.dp))
        Text(text = value, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = PnotesTokens.textPrimary)
    }
}
