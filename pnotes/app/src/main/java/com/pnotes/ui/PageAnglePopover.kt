package com.pnotes.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.canvas.ViewSettings
import com.pnotes.ui.icons.PnotesIcons
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Solid Dark Obsidian Page Angle & Orientation Popover matching the prototype UI.
 * Glass effect removed as requested for crisp contrast and zero opacity artifacts.
 * Provides immediate live canvas rotation, quick presets, rotary angle dial,
 * step nudge buttons, precision slider, handwriting slant presets, canvas behavior toggles,
 * and scope management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageAnglePopover(
    editor: Editor,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialRotation = editor.pageTiltAngle

    var currentAngle by remember { mutableFloatStateOf(initialRotation) }
    var lockCanvasRotation by remember { mutableStateOf(false) }
    var keepToolbarsLevel by remember { mutableStateOf(true) }
    var scopeAllPages by remember { mutableStateOf(false) }

    fun normalizeAngle(deg: Float): Float {
        var a = deg % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return (a * 10f).roundToInt() / 10f
    }

    fun applyRotationToCanvas(angleDeg: Float, saveDefault: Boolean = false) {
        val normalized = normalizeAngle(angleDeg)
        currentAngle = normalized

        // Rotate canvas with full continuous sub-degree tilt support
        editor.setPageTilt(normalized)

        if (scopeAllPages || saveDefault) {
            val newDefaults = editor.viewDefaults.copy(rotation = normalized.roundToInt())
            editor.updateViewDefaults(newDefaults)
        }
    }

    val popoverShape = RoundedCornerShape(16.dp)
    val formattedAngle = if (currentAngle >= 0) "+${String.format(java.util.Locale.US, "%.1f", currentAngle)}°" else "${String.format(java.util.Locale.US, "%.1f", currentAngle)}°"

    val isDark = isGlassDark()

    Box(
        modifier = modifier
            .width(370.dp)
            .wrapContentHeight()
            .shadow(
                elevation = 24.dp,
                shape = popoverShape,
                spotColor = if (isDark) Color(0x66000000) else Color(0x18000000),
                ambientColor = if (isDark) Color(0x33000000) else Color(0x0A000000),
            )
            .clip(popoverShape)
            .background(if (isDark) PnotesDarkCard else PnotesLightCard)
            .border(
                width = 1.dp,
                color = if (isDark) PnotesDarkBorder else PnotesLightBorder,
                shape = popoverShape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            // =========================================================================
            // 1. HEADER: Icon + Title + Subtitle | Reset 0° + Close (X)
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Compass Icon Box (26x26 rd(7))
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isDark) PnotesDarkSurface2 else PnotesLightSurface2)
                            .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.pageAngleCompass,
                            contentDescription = "Page Angle",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(15.dp),
                        )
                    }

                    Column {
                        Text(
                            text = "Page Angle & Orientation",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) PnotesDarkText1 else PnotesLightText1,
                        )
                        Text(
                            text = "Rotate canvas & handwriting slant",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (isDark) PnotesDarkText3 else PnotesLightText3,
                        )
                    }
                }

                // Right Actions: Reset 0° + Close X
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    // Reset 0° button
                    Row(
                        modifier = Modifier
                            .height(25.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) PnotesDarkSurface2 else PnotesLightSurface2)
                            .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(6.dp))
                            .bouncyClickable {
                                applyRotationToCanvas(0f)
                            }
                            .padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = PnotesIcons.angleReset,
                            contentDescription = "Reset",
                            tint = if (isDark) PnotesDarkText2 else PnotesLightText2,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = "Reset 0°",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) PnotesDarkText1 else PnotesLightText1,
                        )
                    }

                    // Close (X) button
                    Box(
                        modifier = Modifier
                            .size(25.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) PnotesDarkSurface2 else PnotesLightSurface2)
                            .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(6.dp))
                            .bouncyClickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.close,
                            contentDescription = "Close",
                            tint = if (isDark) PnotesDarkText2 else PnotesLightText2,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }

            // Divider Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isDark) PnotesDarkBorder else PnotesLightBorder)
            )

            // =========================================================================
            // 2. QUICK ROTATION PRESETS (0°, 90°, 45°, 30°)
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "QUICK ROTATION PRESETS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71717A),
                    letterSpacing = 0.5.sp,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    PresetCard(
                        title = "Default",
                        degree = "0°",
                        isSelected = currentAngle == 0f,
                        modifier = Modifier.weight(1f),
                        onClick = { applyRotationToCanvas(0f) },
                    ) { isSel ->
                        // Portrait mini page illustration
                        Box(
                            modifier = Modifier
                                .size(16.dp, 21.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSel) Color.White else Color(0xFF1F1F26))
                                .border(0.5.dp, if (isSel) Color(0xFFD4D4D8) else Color(0xFF3F3F48), RoundedCornerShape(2.dp))
                                .padding(horizontal = 2.dp, vertical = 3.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(modifier = Modifier.size(11.dp, 1.5.dp).background(if (isSel) Color(0xFFA1A1AA) else Color(0xFF71717A), RoundedCornerShape(1.dp)))
                                Box(modifier = Modifier.size(7.dp, 1.5.dp).background(if (isSel) Color(0xFFD4D4D8) else Color(0xFF52525B), RoundedCornerShape(1.dp)))
                            }
                        }
                    }

                    PresetCard(
                        title = "90°",
                        degree = "Landscape",
                        isSelected = currentAngle == 90f || currentAngle == -270f,
                        modifier = Modifier.weight(1f),
                        onClick = { applyRotationToCanvas(90f) },
                    ) { isSel ->
                        // Landscape mini page illustration
                        Box(
                            modifier = Modifier
                                .size(21.dp, 15.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSel) Color.White else Color(0xFF1F1F26))
                                .border(0.5.dp, if (isSel) Color(0xFFD4D4D8) else Color(0xFF3F3F48), RoundedCornerShape(2.dp))
                                .padding(horizontal = 3.dp, vertical = 2.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(modifier = Modifier.size(14.dp, 1.5.dp).background(if (isSel) Color(0xFFA1A1AA) else Color(0xFF71717A), RoundedCornerShape(1.dp)))
                                Box(modifier = Modifier.size(9.dp, 1.5.dp).background(if (isSel) Color(0xFFD4D4D8) else Color(0xFF52525B), RoundedCornerShape(1.dp)))
                            }
                        }
                    }

                    PresetCard(
                        title = "45°",
                        degree = "Diagonal",
                        isSelected = currentAngle == 45f,
                        modifier = Modifier.weight(1f),
                        onClick = { applyRotationToCanvas(45f) },
                    ) { isSel ->
                        // 45° Diagonal mini page illustration
                        Box(
                            modifier = Modifier
                                .size(13.dp, 18.dp)
                                .rotate(45f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSel) Color.White else Color(0xFF1F1F26))
                                .border(0.5.dp, if (isSel) Color(0xFFD4D4D8) else Color(0xFF3F3F48), RoundedCornerShape(2.dp))
                                .padding(horizontal = 2.dp, vertical = 3.dp),
                        ) {
                            Box(modifier = Modifier.size(9.dp, 1.5.dp).background(if (isSel) Color(0xFFA1A1AA) else Color(0xFF71717A), RoundedCornerShape(1.dp)))
                        }
                    }

                    PresetCard(
                        title = "30°",
                        degree = "Drafting",
                        isSelected = currentAngle == 30f,
                        modifier = Modifier.weight(1f),
                        onClick = { applyRotationToCanvas(30f) },
                    ) { isSel ->
                        // 30° Drafting mini page illustration
                        Box(
                            modifier = Modifier
                                .size(13.dp, 18.dp)
                                .rotate(30f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSel) Color.White else Color(0xFF1F1F26))
                                .border(0.5.dp, if (isSel) Color(0xFFD4D4D8) else Color(0xFF3F3F48), RoundedCornerShape(2.dp))
                                .padding(horizontal = 2.dp, vertical = 3.dp),
                        ) {
                            Box(modifier = Modifier.size(9.dp, 1.5.dp).background(if (isSel) Color(0xFFA1A1AA) else Color(0xFF71717A), RoundedCornerShape(1.dp)))
                        }
                    }
                }
            }

            // =========================================================================
            // 3. FREE ANGLE DIAL & FINE-TUNING CONTROLS
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF101014))
                    .border(0.5.dp, Color(0xFF222228), RoundedCornerShape(9.dp))
                    .padding(8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Header with title and active badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "FREE ANGLE DIAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF71717A),
                            letterSpacing = 0.5.sp,
                        )

                        // Angle Readout Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isDark) PnotesDarkElevated else PnotesLightSurface2)
                                .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(5.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = formattedAngle,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007AFF),
                            )
                        }
                    }

                    // Rotary Dial + Step Buttons & Slider Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Interactive Rotary Dial (78x78)
                        RotaryDial(
                            angle = currentAngle,
                            onAngleChange = { deg ->
                                applyRotationToCanvas(deg)
                            },
                            modifier = Modifier.size(78.dp),
                        )

                        // Nudge Buttons & Slider
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            // 4 Nudge Step Buttons: [-5°], [-1°], [+1°], [+5°]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                NudgeButton(label = "-5°", modifier = Modifier.weight(1f)) {
                                    applyRotationToCanvas(currentAngle - 5f)
                                }
                                NudgeButton(label = "-1°", modifier = Modifier.weight(1f)) {
                                    applyRotationToCanvas(currentAngle - 1f)
                                }
                                NudgeButton(label = "+1°", modifier = Modifier.weight(1f)) {
                                    applyRotationToCanvas(currentAngle + 1f)
                                }
                                NudgeButton(label = "+5°", modifier = Modifier.weight(1f)) {
                                    applyRotationToCanvas(currentAngle + 5f)
                                }
                            }

                            // Continuous Range Slider (-180° to +180°)
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Slider(
                                    value = currentAngle.coerceIn(-180f, 180f),
                                    onValueChange = { deg ->
                                        currentAngle = normalizeAngle(deg)
                                    },
                                    onValueChangeFinished = {
                                        applyRotationToCanvas(currentAngle)
                                    },
                                    valueRange = -180f..180f,
                                    colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF007AFF),
                                    activeTrackColor = Color(0xFF007AFF),
                                    inactiveTrackColor = if (isDark) PnotesDarkTrack else PnotesLightTrack,
                                ),
                                    modifier = Modifier.fillMaxWidth().height(16.dp),
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(text = "-180°", fontSize = 8.sp, color = Color(0xFFA1A1AA))
                                    Text(text = "0°", fontSize = 8.sp, color = Color(0xFFA1A1AA))
                                    Text(text = "+180°", fontSize = 8.sp, color = Color(0xFFA1A1AA))
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 4. NATURAL HANDWRITING SLANTS (Right +15°, Left -15°, Draft +30°)
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "NATURAL HANDWRITING SLANTS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71717A),
                    letterSpacing = 0.5.sp,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    SlantCard(
                        icon = PnotesIcons.handSlantRight,
                        label = "Right (+15°)",
                        isSelected = currentAngle == 15f,
                        modifier = Modifier.weight(1f),
                        onClick = { applyRotationToCanvas(15f) },
                    )

                    SlantCard(
                        icon = PnotesIcons.handSlantLeft,
                        label = "Left (-15°)",
                        isSelected = currentAngle == -15f,
                        modifier = Modifier.weight(1f),
                        onClick = { applyRotationToCanvas(-15f) },
                    )

                    SlantCard(
                        icon = PnotesIcons.draftingAngle,
                        label = "Draft (+30°)",
                        isSelected = currentAngle == 30f,
                        modifier = Modifier.weight(1f),
                        onClick = { applyRotationToCanvas(30f) },
                    )
                }
            }

            // =========================================================================
            // 5. CANVAS BEHAVIOR TOGGLES
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) PnotesDarkSurface2 else PnotesLightSurface2)
                    .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(8.dp))
                    .padding(7.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Toggle 1: Lock Canvas Rotation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Lock Canvas Rotation",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) PnotesDarkText1 else PnotesLightText1,
                            )
                            Text(
                                text = "Disable accidental 2-finger twist",
                                fontSize = 8.5.sp,
                                color = if (isDark) PnotesDarkText3 else PnotesLightText3,
                            )
                        }

                        SolidAppleSwitch(
                            checked = lockCanvasRotation,
                            onCheckedChange = { lockCanvasRotation = it },
                        )
                    }

                    // Toggle 2: Keep Toolbars Level
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Keep Toolbars Level",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) PnotesDarkText1 else PnotesLightText1,
                            )
                            Text(
                                text = "UI stays horizontal as page tilts",
                                fontSize = 8.5.sp,
                                color = if (isDark) PnotesDarkText3 else PnotesLightText3,
                            )
                        }

                        SolidAppleSwitch(
                            checked = keepToolbarsLevel,
                            onCheckedChange = { keepToolbarsLevel = it },
                        )
                    }
                }
            }

            // =========================================================================
            // 6. PAGE SCOPE SELECTOR & ACTION BUTTONS
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                // Segmented Scope Switcher
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isDark) PnotesDarkBg else PnotesLightBg)
                        .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(7.dp))
                        .padding(2.5.dp),
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Scope: This Page Only
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (!scopeAllPages) Color(0xFF007AFF) else Color.Transparent)
                                .border(
                                    0.5.dp,
                                    if (!scopeAllPages) Color(0xFF007AFF) else Color.Transparent,
                                    RoundedCornerShape(5.dp)
                                )
                                .bouncyClickable { scopeAllPages = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "This Page Only (p. ${editor.pageIndex + 1})",
                                fontSize = 10.sp,
                                fontWeight = if (!scopeAllPages) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (!scopeAllPages) Color.White else (if (isDark) PnotesDarkText2 else PnotesLightText2),
                            )
                        }

                        // Scope: All Pages
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (scopeAllPages) Color(0xFF007AFF) else Color.Transparent)
                                .border(
                                    0.5.dp,
                                    if (scopeAllPages) Color(0xFF007AFF) else Color.Transparent,
                                    RoundedCornerShape(5.dp)
                                )
                                .bouncyClickable { scopeAllPages = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "All Pages",
                                fontSize = 10.sp,
                                fontWeight = if (scopeAllPages) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (scopeAllPages) Color.White else (if (isDark) PnotesDarkText2 else PnotesLightText2),
                            )
                        }
                    }
                }

                // Bottom Action Buttons: Save as Default + Apply Angle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Secondary: Save as Default
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) PnotesDarkSurface2 else PnotesLightSurface2)
                            .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(8.dp))
                            .bouncyClickable {
                                applyRotationToCanvas(currentAngle, saveDefault = true)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Save as Default",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) PnotesDarkText1 else PnotesLightText1,
                        )
                    }

                    // Primary CTA: Apply Angle
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(8.dp),
                                spotColor = Color(0x33007AFF),
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF007AFF))
                            .border(1.dp, Color(0xFF007AFF), RoundedCornerShape(8.dp))
                            .bouncyClickable {
                                applyRotationToCanvas(currentAngle, saveDefault = false)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = PnotesIcons.pageAngleCheck,
                                contentDescription = "Apply",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "Apply Angle ($formattedAngle)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick Rotation Preset Card item.
 */
@Composable
private fun PresetCard(
    title: String,
    degree: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    iconContent: @Composable (Boolean) -> Unit,
) {
    val isDark = isGlassDark()
    val cardShape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(cardShape)
            .background(if (isSelected) (if (isDark) PnotesDarkElevated else Color(0xFFF0F4FA)) else (if (isDark) PnotesDarkSurface2 else PnotesLightSurface2))
            .border(
                width = if (isSelected) 1.dp else 0.5.dp,
                color = if (isSelected) Color(0xFF007AFF) else (if (isDark) PnotesDarkBorder else PnotesLightBorder),
                shape = cardShape,
            )
            .bouncyClickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                iconContent(isSelected)
            }

            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color(0xFF007AFF) else (if (isDark) PnotesDarkText1 else PnotesLightText1),
            )

            Text(
                text = degree,
                fontSize = 8.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) PnotesDarkText3 else PnotesLightText3,
            )
        }
    }
}

/**
 * Interactive Rotary Angle Dial with cardinal tick marks and glowing pointer needle.
 */
@Composable
private fun RotaryDial(
    angle: Float,
    onAngleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun calculateAngle(pointerOffset: Offset, center: Offset): Float {
        val dx = pointerOffset.x - center.x
        val dy = pointerOffset.y - center.y
        var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
        if (deg > 180f) deg -= 360f
        return deg
    }

    Box(
        modifier = modifier
            .shadow(6.dp, CircleShape, spotColor = Color(0xFF000000))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0B0B0E), Color(0xFF14141A)),
                    start = Offset(0f, 0f),
                    end = Offset(78f, 78f),
                )
            )
            .border(1.dp, Color(0xFF2A2A34), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    onAngleChange(calculateAngle(change.position, center))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    onAngleChange(calculateAngle(offset, center))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Top 0°
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 2.5.dp)
                .size(3.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
        Text(
            text = "0°",
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = 6.dp),
        )

        // Right 90°
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-2.5).dp)
                .size(3.dp)
                .clip(CircleShape)
                .background(Color(0xFF52525B))
        )
        Text(
            text = "90°",
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF71717A),
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-8.5).dp),
        )

        // Bottom 180°
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-2.5).dp)
                .size(3.dp)
                .clip(CircleShape)
                .background(Color(0xFF52525B))
        )
        Text(
            text = "180°",
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF71717A),
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-8.5).dp),
        )

        // Left 270°
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 2.5.dp)
                .size(3.dp)
                .clip(CircleShape)
                .background(Color(0xFF52525B))
        )
        Text(
            text = "270°",
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF71717A),
            modifier = Modifier.align(Alignment.CenterStart).offset(x = 7.5.dp),
        )

        // Inner circular track (36dp)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(0.5.dp, Color(0xFF2A2A34), CircleShape)
        )

        // Rotating Pointer Needle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(angle),
            contentAlignment = Alignment.Center,
        ) {
            // Needle body
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 10.dp)
                    .size(2.dp, 26.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White, Color(0xFFA1A1AA))
                        ),
                        RoundedCornerShape(1.dp)
                    )
            )

            // Glowing Needle Tip Bead
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 6.5.dp)
                    .size(4.dp)
                    .shadow(3.dp, CircleShape, spotColor = Color.White)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        // Center Metallic Pivot Cap (9dp)
        Box(
            modifier = Modifier
                .size(9.dp)
                .shadow(2.dp, CircleShape, spotColor = Color(0xFF000000))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFF71717A))
                    )
                )
                .border(0.75.dp, Color(0xFF000000), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111115))
            )
        }
    }
}

/**
 * Granular +/- Nudge button for angle dial.
 */
@Composable
private fun NudgeButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isDark = isGlassDark()
    Box(
        modifier = modifier
            .height(25.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDark) PnotesDarkElevated else PnotesLightSurface2)
            .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(6.dp))
            .bouncyClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) PnotesDarkText1 else PnotesLightText1,
        )
    }
}

/**
 * Natural Handwriting Slant Card.
 */
@Composable
private fun SlantCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isDark = isGlassDark()
    val cardShape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .height(28.dp)
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(5.dp, cardShape, spotColor = Color(0x33007AFF))
                        .background(Color(0xFF007AFF), cardShape)
                        .border(1.dp, Color(0xFF007AFF), cardShape)
                } else {
                    Modifier
                        .background(if (isDark) PnotesDarkElevated else PnotesLightSurface2, cardShape)
                        .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, cardShape)
                }
            )
            .clip(cardShape)
            .bouncyClickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else (if (isDark) PnotesDarkText2 else PnotesLightText2),
                modifier = Modifier.size(11.dp),
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color.White else (if (isDark) PnotesDarkText2 else PnotesLightText2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Apple-style minimalist switch component with crisp solid contrast.
 */
@Composable
private fun SolidAppleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 12f else 2f,
        animationSpec = tween(durationMillis = 180),
        label = "switchThumb",
    )

    Box(
        modifier = modifier
            .size(30.dp, 18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (checked) Color(0xFF007AFF) else Color(0xFF2A3342))
            .border(
                0.5.dp,
                if (checked) Color(0xFF007AFF) else Color(0xFF3F4B5E),
                RoundedCornerShape(9.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x = thumbOffset.dp.roundToPx(), y = 0) }
                .size(14.dp)
                .shadow(2.dp, CircleShape, spotColor = Color(0x44000000))
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
