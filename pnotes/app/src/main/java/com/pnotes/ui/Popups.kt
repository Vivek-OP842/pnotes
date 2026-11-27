package com.pnotes.ui

import android.os.Build
import android.view.View
import android.view.ViewParent
import android.view.WindowManager
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import com.pnotes.ui.theme.PurpleTheme
import com.pnotes.ui.theme.floatingPill3D
import com.pnotes.ui.theme.elevated3D
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.pnotes.R
import com.pnotes.canvas.ViewOverrides
import com.pnotes.canvas.ViewSettings
import com.pnotes.canvas.ViewingMode
import com.pnotes.core.model.PageEdge
import com.pnotes.core.model.PageMargins
import com.pnotes.core.model.PagePattern
import com.pnotes.core.model.PageStyle
import com.pnotes.core.model.Rgba
import com.pnotes.core.pal.FontFace
import com.pnotes.core.tools.EraseMode
import com.pnotes.core.tools.ShapeConfig
import com.pnotes.core.tools.ShapeKind
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolConversions
import com.pnotes.core.tools.ToolDefaults
import com.pnotes.platform.FontCatalog
import com.pnotes.settings.Preferences
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.LocalMotion
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.Motion
import com.pnotes.ui.theme.toComposeColor
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The "Pen Dynamics" popup (the Pen Popover redesign): a themed header with Reset and close, a
 * segmented tab row across the six stroke tools, then per-tool dynamics — live stroke preview,
 * ink colour, stroke-thickness presets, pressure, signature controls, smoothing, and Scale/Neon
 * pills. Every colour resolves through [LocalPalette], so one layout serves dark and light.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolConfigPopup(
    editor: ToolPopupHost,
    tool: Tool,
    onDismiss: () -> Unit,
    visible: Boolean = true,
) {
    var selected by remember { mutableStateOf(tool) }
    var resetEpoch by remember { mutableStateOf(0) }
    val motion = LocalMotion.current
    val isDark = isGlassDark()

    androidx.compose.runtime.LaunchedEffect(tool) {
        selected = tool
    }

    DropdownMenu(
        expanded = visible,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            Modifier
                .width(360.dp)
                .heightIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            PopupHeaderBar(
                title = if (selected.isStroke) "Pen Dynamics · ${penToolLabel(selected)}" else selected.name.lowercase().replaceFirstChar { it.uppercase() },
                icon = if (selected.isStroke) penToolIcon(selected) else popupGlyphIcon(selected),
                onReset = if (selected.isStroke) {
                    {
                        editor.updateToolConfig(selected, ToolDefaults.configFor(selected))
                        resetEpoch++
                    }
                } else {
                    null
                },
                onClose = onDismiss,
            )

            if (selected.isStroke) {
                Spacer(Modifier.height(6.dp))
                PenTypeTabs(selected) { t ->
                    selected = t
                    editor.armPopupTool(t)
                }
            }

            Spacer(Modifier.height(8.dp))
            // The panel body changes identity when another pen is picked: cross-fading it keeps the
            // frame still and shows the swap, instead of repainting 360dp of controls at once.
            AnimatedContent(
                targetState = selected to resetEpoch,
                transitionSpec = {
                    fadeIn(animationSpec = motion.quick()) togetherWith
                        fadeOut(animationSpec = motion.quick())
                },
                label = "penDynamicsBody",
            ) { (bodyTool, _) ->
                PenDynamicsContent(editor, bodyTool)
            }
        }
    }
}

internal fun penToolIcon(tool: Tool): ImageVector = when (tool) {
    Tool.CALLIGRAPHY -> PnotesIcons.fountainPen
    Tool.SPEED -> PnotesIcons.speedPen
    Tool.TAPER -> PnotesIcons.taperPen
    Tool.DASHED -> PnotesIcons.dashedPen
    Tool.HIGHLIGHTER -> PnotesIcons.highlighter
    else -> PnotesIcons.pen
}

internal fun penToolLabel(tool: Tool): String = when (tool) {
    Tool.CALLIGRAPHY -> "Calligraphy"
    Tool.SPEED -> "Speed"
    Tool.TAPER -> "Brush"
    Tool.DASHED -> "Dashed"
    Tool.HIGHLIGHTER -> "Highlighter"
    else -> "Fountain Pen"
}

private fun popupGlyphIcon(tool: Tool): ImageVector = when (tool) {
    Tool.ERASER -> PnotesIcons.eraser
    Tool.SHAPE -> PnotesIcons.shape
    Tool.TABLE -> PnotesIcons.table
    Tool.SELECT, Tool.LASSO -> PnotesIcons.select
    Tool.TEXT, Tool.TEXT_BOX -> PnotesIcons.text
    Tool.IMAGE -> PnotesIcons.image
    Tool.PAN -> PnotesIcons.pan
    else -> penToolIcon(tool)
}

/** The dynamics content for one stroke tool; keyed by tool, so state resets on a tab switch. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PenDynamicsContent(editor: ToolPopupHost, tool: Tool) {
    val base = remember { editor.toolConfig(tool) }
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val accent = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    val green = accent

    var pressure by remember { mutableStateOf(base.pressureEnabled) }
    var sensitivity by remember { mutableStateOf(ToolConversions.minFactorToSensitivity(base.pressureMinFactor).toFloat()) }
    var multiplier by remember { mutableStateOf(ToolConversions.directionStrengthToMultiplier(base.directionStrength).toFloat()) }
    var nibCut by remember { mutableStateOf(ToolConversions.nibCutToDegrees(base.nibCut).toFloat()) }
    var speed by remember { mutableStateOf(ToolConversions.strengthToSpeed(base.speedStrength).toFloat()) }
    var taperTip by remember { mutableStateOf((base.taperMinFactor * 100).toFloat()) }
    var width by remember { mutableStateOf(base.baseWidth.toFloat()) }
    var glow by remember { mutableStateOf(base.neon) }
    var glowIntensity by remember { mutableStateOf(ToolConversions.neonStrengthToIntensity(base.neonStrength).toFloat()) }
    var dashLen by remember { mutableStateOf(base.dashLength.toFloat()) }
    var gapLen by remember { mutableStateOf(base.dashGap.toFloat()) }
    var straight by remember { mutableStateOf(base.straightLine) }
    var scale by remember { mutableStateOf(base.scale) }
    var intensity by remember { mutableStateOf(ToolConversions.highlighterAlphaToIntensity(base.highlighterAlpha).toFloat()) }
    var inverse by remember { mutableStateOf(base.highlighterInverse) }
    var stability by remember { mutableStateOf((base.stability * 100.0).toFloat().coerceIn(0f, 100f)) }
    var velAmp by remember { mutableStateOf(base.velocityAmplifier.toFloat()) }
    var endVelSens by remember { mutableStateOf((base.endVelocitySensitivity * 100.0).toFloat().coerceIn(0f, 100f)) }
    var brushRatio by remember { mutableStateOf(base.brushRatio.toFloat()) }
    var brushAngleDeg by remember { mutableStateOf(Math.toDegrees(base.brushAngle).toFloat()) }
    var adaptiveSpline by remember { mutableStateOf(base.adaptiveSpline) }
    var colorOverride by remember { mutableStateOf(base.colorOverride) }

    val activeColor = (colorOverride ?: editor.hostToolbarColors.getOrNull(editor.hostActiveColorIndex) ?: Rgba(10, 132, 255)).toComposeColor()

    fun emit() {
        val m = ToolConversions.sensitivityToMinFactor(sensitivity.toDouble())
        val ds = if (tool == Tool.CALLIGRAPHY) ToolConversions.multiplierToDirectionStrength(multiplier.toDouble()) else base.directionStrength
        val nc = if (tool == Tool.CALLIGRAPHY) ToolConversions.degreesToNibCut(nibCut.toDouble()) else base.nibCut
        val sp = if (tool == Tool.SPEED) ToolConversions.speedToStrength(speed.toDouble()) else base.speedStrength
        val tmf = if (tool == Tool.TAPER) taperTip.toDouble() / 100.0 else base.taperMinFactor
        val ha = if (tool == Tool.HIGHLIGHTER) ToolConversions.intensityToHighlighterAlpha(intensity.toDouble()) else base.highlighterAlpha
        editor.updateToolConfig(
            tool,
            base.copy(
                baseWidth = width.toDouble(),
                pressureEnabled = pressure,
                pressureMinFactor = m,
                directionStrength = ds,
                speedStrength = sp,
                taperMinFactor = tmf,
                neon = glow,
                neonStrength = ToolConversions.intensityToNeonStrength(glowIntensity.toDouble()),
                dashLength = dashLen.toDouble(),
                dashGap = gapLen.toDouble(),
                straightLine = straight,
                scale = scale,
                highlighterAlpha = ha,
                highlighterInverse = inverse,
                stability = (stability / 100.0).toDouble(),
                nibCut = nc,
                velocityAmplifier = velAmp.toDouble(),
                endVelocitySensitivity = (endVelSens / 100.0).toDouble(),
                brushRatio = brushRatio.toDouble(),
                brushAngle = Math.toRadians(brushAngleDeg.toDouble()),
                adaptiveSpline = adaptiveSpline,
                colorOverride = colorOverride,
            ),
        )
    }

    Column {
        // === 1. Live stroke preview ===
        LiveStrokePreviewBanner(
            tool = tool,
            width = width,
            neon = glow,
            neonIntensity = glowIntensity,
            dashLen = dashLen,
            gapLen = gapLen,
            color = activeColor,
            highlighterAlpha = intensity,
        )

        Spacer(Modifier.height(8.dp))

        // === 2. Colour & Ink Shade ===
        InkShadeRow(
            editor = editor,
            tool = tool,
            colorOverride = colorOverride,
            highlighterOpacity = intensity,
            onPick = { colorOverride = it; emit() },
        )

        Spacer(Modifier.height(8.dp))

        // === 3. Stroke thickness: presets, quick sizes, slider ===
        val range = ToolConversions.widthRange(tool)
        val widthRange = range.start.toFloat()..range.endInclusive.toFloat()
        SectionCard {
            SectionCardHeader(
                title = "Stroke Thickness",
                glyph = { ThicknessGlyph(accent) },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDark) Color(0xFF18181B) else Color(0xFFECECF1))
                                .border(0.5.dp, if (isDark) Color(0xFF27272A) else Color(0xFFE0E0E6), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "${fmtWidth(range.start)} – ${fmtWidth(range.endInclusive)} pt",
                                color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B),
                                fontSize = 9.sp,
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5))
                                .border(0.5.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFE4E4E7), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "${fmtWidth(width)} pt",
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            WidthPresetChips(width, widthRange) { width = it; emit() }
            Spacer(Modifier.height(6.dp))
            QuickSizeGrid(width, widthRange) { width = it; emit() }
            Spacer(Modifier.height(6.dp))
            MinMaxSlider(width, widthRange) { width = it; emit() }
        }

        Spacer(Modifier.height(8.dp))

        // === 4. Pressure sensitivity ===
        if (tool == Tool.PEN || tool == Tool.CALLIGRAPHY || tool == Tool.SPEED || tool == Tool.TAPER) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PressureGlyph(accent)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Pressure Sensitivity",
                        color = if (isDark) Color.White else Color(0xFF151922),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "$sensitivity%",
                        color = if (pressure) accent else (if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = pressure,
                        onCheckedChange = { pressure = it; emit() },
                        colors = PurpleTheme.switchColors(isDark),
                    )
                }
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it; emit() },
                    valueRange = 0f..100f,
                    enabled = pressure,
                    colors = PurpleTheme.sliderColors(isDark),
                    modifier = Modifier.height(26.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // === 5. Per-tool signature controls ===
        if (tool == Tool.CALLIGRAPHY || tool == Tool.SPEED || tool == Tool.TAPER || tool == Tool.DASHED || tool == Tool.HIGHLIGHTER) {
            SectionCard {
                SectionCardHeader(title = "Signature", glyph = { SlidersGlyph(accent) })
                if (tool == Tool.CALLIGRAPHY) {
                    SliderRow("MULTIPLIER", multiplier, 1f..5f, suffix = "x") { multiplier = it; emit() }
                    // The grind, not the geometry: 0 squares the face to the barrel's lean (a
                    // straight italic), and turning it either way makes the nib oblique. Only has
                    // an effect while the stylus reports a tilt — see NibAxis.
                    SliderRow(
                        "NIB CUT",
                        nibCut,
                        ToolConversions.NIB_CUT_RANGE_DEGREES.start.toFloat()..
                            ToolConversions.NIB_CUT_RANGE_DEGREES.endInclusive.toFloat(),
                        suffix = "°",
                    ) { nibCut = it; emit() }

                }
                if (tool == Tool.SPEED) {
                    SliderRow("SPEED", speed, 0f..100f, suffix = "%") { speed = it; emit() }
                }
                if (tool == Tool.TAPER) {
                    SliderRow("TIP WIDTH PERCENTAGE", taperTip, 0f..100f, suffix = "%") { taperTip = it; emit() }
                }
                if (tool == Tool.DASHED) {
                    SliderRow("DASH", dashLen, 2f..40f, suffix = "px") { dashLen = it; emit() }
                    SliderRow("GAP", gapLen, 2f..40f, suffix = "px") { gapLen = it; emit() }
                }
                if (tool == Tool.HIGHLIGHTER) {
                    SliderRow("OPACITY", intensity, 10f..100f, suffix = "%") { intensity = it; emit() }
                    ToggleRow("STRAIGHT LINE", straight) { straight = it; emit() }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // === 6. Stroke smoothing ===
        if (tool.isStroke) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WaveGlyph(green)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Stroke Smoothing",
                        color = if (isDark) Color.White else Color(0xFF151922),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text("$stability%", color = green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = stability,
                    onValueChange = { stability = it; emit() },
                    valueRange = 0f..100f,
                    colors = PurpleTheme.sliderColors(isDark),
                    modifier = Modifier.height(26.dp),
                )
                // Smoothing Presets
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val isStarNote = abs(stability - 45f) < 2f && abs(endVelSens - 40f) < 2f && adaptiveSpline
                    val presets = listOf(
                        "Raw" to 0f,
                        "Standard" to 25f,
                        "StarNote" to 45f,
                        "Silky" to 75f,
                    )
                    presets.forEach { (label, value) ->
                        val active = if (label == "StarNote") isStarNote else (abs(stability - value) < 2f && !isStarNote)
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) (if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)) else Color.Transparent)
                                .border(
                                    0.5.dp,
                                    if (active) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)),
                                    RoundedCornerShape(6.dp),
                                )
                                .clickable {
                                    stability = value
                                    if (label == "StarNote") {
                                        endVelSens = 40f
                                        adaptiveSpline = true
                                        if (tool == Tool.PEN) velAmp = 1.15f
                                    }
                                    emit()
                                }
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                color = if (active) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)),
                                fontSize = 8.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }
                if (tool == Tool.PEN || tool == Tool.CALLIGRAPHY || tool == Tool.SPEED || tool == Tool.TAPER) {
                    Spacer(Modifier.height(4.dp))
                    SliderRow("LIFT FLICK DECAY", endVelSens, 0f..100f, suffix = "%") { endVelSens = it; emit() }
                    ToggleRow("CURVATURE ADAPTIVE SPLINE", adaptiveSpline) { adaptiveSpline = it; emit() }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // === 7. Scale / Neon toggle pills ===
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TogglePill("Scale with Zoom", "Fixed canvas mm", scale, { scale = it; emit() }, Modifier.weight(1f))
            if (tool.isStroke && tool != Tool.HIGHLIGHTER && tool != Tool.DASHED) {
                TogglePill("Neon Glow", "Refractive aura", glow, { glow = it; emit() }, Modifier.weight(1f))
            }
        }
        if (glow && tool.isStroke && tool != Tool.HIGHLIGHTER && tool != Tool.DASHED) {
            Spacer(Modifier.height(8.dp))
            SectionCard {
                SliderRow("INTENSITY", glowIntensity, 0f..100f, suffix = "%") { glowIntensity = it; emit() }
            }
        }
    }
}

/** Live stroke preview rendered inside popup headers. */
@Composable
private fun LiveStrokePreviewBanner(
    tool: Tool,
    width: Float,
    neon: Boolean,
    neonIntensity: Float,
    dashLen: Float,
    gapLen: Float,
    color: Color,
    highlighterAlpha: Float,
) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0x40000000) else Color(0x33FFFFFF))
            .border(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x18000000), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.08f, h * 0.58f)
                cubicTo(
                    w * 0.35f, h * 0.15f,
                    w * 0.65f, h * 0.85f,
                    w * 0.92f, h * 0.42f,
                )
            }
            val strokeW = width.coerceIn(1.5f, 26f)
            val pathEffect = if (tool == Tool.DASHED) {
                PathEffect.dashPathEffect(floatArrayOf(dashLen.coerceAtLeast(2f), gapLen.coerceAtLeast(2f)), 0f)
            } else null

            if (neon) {
                val glowAlpha = (neonIntensity / 100f * 0.45f).coerceIn(0.1f, 0.6f)
                drawPath(
                    path = path,
                    color = color.copy(alpha = glowAlpha),
                    style = Stroke(
                        width = strokeW * 2.8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = pathEffect,
                    ),
                )
            }

            val coreAlpha = if (tool == Tool.HIGHLIGHTER) (highlighterAlpha / 100f).coerceIn(0.10f, 1.0f) else 1.0f
            drawPath(
                path = path,
                color = color.copy(alpha = coreAlpha),
                style = Stroke(
                    width = strokeW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = pathEffect,
                ),
            )
        }
    }
}

// ============================================================================
// Pen-popover shared components (theme-aware: one layout serves dark and light)
// ============================================================================

/** Widths formatted without a trailing .0 (6.0 -> "6", 6.5 -> "6.5"). */
private fun fmtWidth(v: Double): String {
    val r = (v * 10).roundToInt() / 10.0
    return if (abs(r - r.roundToInt()) < 0.05) r.roundToInt().toString() else r.toString()
}

private fun fmtWidth(v: Float): String = fmtWidth(v.toDouble())

/** The popup's header row: indigo icon tile, title, optional Reset pill, close button. */
@Composable
internal fun PopupHeaderBar(
    title: String,
    icon: ImageVector,
    onReset: (() -> Unit)?,
    onClose: () -> Unit,
) {
    val isDark = LocalPalette.current.isDark
    Row(verticalAlignment = Alignment.CenterVertically) {
        val motion = LocalMotion.current
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDark) PnotesDarkElevated else PnotesLightSurface2)
                .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = icon, animationSpec = motion.quick(), label = "popupHeaderIcon") { glyph ->
                Icon(glyph, contentDescription = null, tint = if (isDark) PnotesDarkText1 else PnotesLightText1, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Crossfade(
            targetState = title,
            modifier = Modifier.weight(1f),
            animationSpec = motion.quick(),
            label = "popupHeaderTitle",
        ) { heading ->
            Text(
                heading,
                color = if (isDark) PnotesDarkText1 else PnotesLightText1,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onReset != null) {
            Box(
                Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isDark) PnotesDarkElevated else PnotesLightSurface2)
                    .border(0.8.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(7.dp))
                    .bouncyClickable(onClick = onReset)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Reset",
                    color = if (isDark) PnotesDarkText2 else PnotesLightText2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (isDark) PnotesDarkElevated else PnotesLightSurface2)
                .border(0.8.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(7.dp))
                .bouncyClickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                PnotesIcons.close,
                contentDescription = "Close",
                tint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF52525B),
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/** The segmented pen-type tab row: one indigo pill for the active tool. */
@Composable
private fun PenTypeTabs(selected: Tool, onSelect: (Tool) -> Unit) {
    val isDark = LocalPalette.current.isDark
    val pens = listOf(
        Tool.PEN to PnotesIcons.pen,
        Tool.CALLIGRAPHY to PnotesIcons.fountainPen,
        Tool.HIGHLIGHTER to PnotesIcons.highlighter,
        Tool.DASHED to PnotesIcons.dashedPen,
        Tool.SPEED to PnotesIcons.speedPen,
        Tool.TAPER to PnotesIcons.taperPen,
    )
    val selectedIndex = pens.indexOfFirst { it.first == selected }.coerceAtLeast(0)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) PnotesDarkSurface2 else PnotesLightSurface2)
            .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(10.dp))
            .padding(3.dp),
    ) {
        val totalWidth = maxWidth
        val tabWidth = (totalWidth - (2.dp * (pens.size - 1))) / pens.size
        val targetX = (tabWidth + 2.dp) * selectedIndex
        val animatedX by animateDpAsState(
            targetValue = targetX,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "penTabIndicator",
        )

        // Sliding traveling indicator pill
        Box(
            Modifier
                .offset(x = animatedX)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF007AFF)),
        )

        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pens.forEach { (t, icon) ->
                val active = t == selected
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val tabScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.88f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "tabScale",
                )
                val tabTint by animateColorAsState(
                    targetValue = if (active) Color.White else (if (isDark) PnotesDarkText2 else PnotesLightText2),
                    animationSpec = LocalMotion.current.effect(),
                    label = "penTabTint",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = tabScale
                            scaleY = tabScale
                        }
                        .clip(RoundedCornerShape(7.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(t) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = t.name.lowercase().replaceFirstChar { it.uppercase() },
                        tint = tabTint,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** A rounded section card used by the pen/eraser/shape popovers. */
@Composable
internal fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val isDark = LocalPalette.current.isDark
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) PnotesDarkElevated else PnotesLightSurface2)
            .border(0.5.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        content = content,
    )
}

/** A section card's title row with an optional leading glyph and trailing content. */
@Composable
internal fun SectionCardHeader(
    title: String,
    glyph: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val isDark = LocalPalette.current.isDark
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (glyph != null) {
            glyph()
            Spacer(Modifier.width(6.dp))
        }
        Text(
            title,
            color = if (isDark) PnotesDarkText1 else PnotesLightText1,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/**"Colour & Ink Shade" row: preset swatches + custom wheel dot, optional opacity readout. */
@Composable
private fun InkShadeRow(
    editor: ToolPopupHost,
    tool: Tool,
    colorOverride: Rgba?,
    highlighterOpacity: Float,
    onPick: (Rgba?) -> Unit,
) {
    val isDark = LocalPalette.current.isDark
    val swatches = listOf(
        Rgba(79, 70, 229),
        Rgba(255, 255, 255),
        Rgba(56, 189, 248),
        Rgba(16, 185, 129),
        Rgba(245, 158, 11),
        Rgba(244, 63, 94),
    )
    val active = colorOverride ?: editor.hostToolbarColors.getOrNull(editor.hostActiveColorIndex)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Color & Ink Shade",
            color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF3F3F46),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (tool == Tool.HIGHLIGHTER) {
            Text(
                "Opacity ",
                color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B),
                fontSize = 10.sp,
            )
            Text(
                "${highlighterOpacity.roundToInt()}%",
                color = if (isDark) Color.White else Color(0xFF151922),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        swatches.forEach { c ->
            val isActive = active != null && c == active
            val isWhite = c.r > 240 && c.g > 240 && c.b > 240
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF0A0A0A) else Color.White)
                    .then(
                        if (isActive) {
                            Modifier.border(1.dp, if (isDark) Color.White else Color(0xFF09090B), CircleShape)
                        } else {
                            Modifier.border(0.5.dp, if (isDark) Color(0xFF262626) else Color(0xFFE4E4E7), CircleShape)
                        },
                    )
                    .padding(5.dp)
                    .clickable { onPick(if (isActive) null else c) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(c.toComposeColor())
                        .then(if (isWhite) Modifier.border(0.5.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFD4D4D8), CircleShape) else Modifier),
                )
            }
        }
        ColorPickerDot(
            colorOverride,
            custom = colorOverride != null && colorOverride !in swatches,
            onPick = onPick,
            dismissOnPick = false,
        ) { d, p ->
            ColorPickerPopup(
                initial = colorOverride ?: editor.hostToolbarColors.getOrNull(editor.hostActiveColorIndex),
                recents = editor.hostRecentColors,
                onDismiss = d,
                onPick = p,
            )
        }
    }
}

/**"Fine · 1 / Pen · 3 / Med · 6 …" preset chips; clamps each preset into the tool's range. */
@Composable
private fun WidthPresetChips(current: Float, range: ClosedFloatingPointRange<Float>, onPick: (Float) -> Unit) {
    val isDark = LocalPalette.current.isDark
    val presets = listOf("Fine" to 1f, "Pen" to 2f, "Med" to 5f, "Thick" to 12f, "Marker" to 25f, "Max" to 60f)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0xFF18181B) else Color(0xFFEFEFF2))
            .border(0.5.dp, if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7), RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        presets.forEach { (label, v) ->
            val clamped = v.coerceIn(range.start, range.endInclusive)
            val active = abs(current - clamped) < 0.05f
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (active) (if (isDark) Color.White else Color(0xFF09090B)) else Color.Transparent)
                    .clickable { onPick(clamped) }
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$label · ${clamped.roundToInt()}",
                    color = when {
                        active -> if (isDark) Color(0xFF09090B) else Color.White
                        else -> if (isDark) Color(0xFFB4B4BC) else Color(0xFF52525B)
                    },
                    fontSize = 8.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The 2×4 quick-size grid of dots sized by thickness. */
@Composable
private fun QuickSizeGrid(current: Float, range: ClosedFloatingPointRange<Float>, onPick: (Float) -> Unit) {
    val isDark = LocalPalette.current.isDark
    val sizes = listOf(0.5f, 2f, 6f, 12f, 20f, 32f, 45f, 60f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sizes.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { v ->
                    val clamped = v.coerceIn(range.start, range.endInclusive)
                    val active = abs(current - clamped) < 0.05f
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) (if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)) else (if (isDark) Color(0xFF171717) else Color(0xFFF4F4F6)))
                            .then(
                                if (active) {
                                    Modifier.border(0.75.dp, if (isDark) Color.White else Color(0xFF09090B), RoundedCornerShape(8.dp))
                                } else {
                                    Modifier.border(0.5.dp, if (isDark) Color(0xFF262626) else Color(0xFFE4E4E7), RoundedCornerShape(8.dp))
                                },
                            )
                            .clickable { onPick(clamped) }
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        val dot = (2 + (clamped / 60f) * 20).dp
                        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(dot)
                                    .clip(CircleShape)
                                    .background(if (active) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFFB4B4BC) else Color(0xFF52525B))),
                            )
                        }
                        Text(
                            fmtWidth(clamped),
                            color = if (active) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)),
                            fontSize = 9.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

/** A slider with "min pt / max pt" labels either side, white knob, indigo core. */
@Composable
private fun MinMaxSlider(value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val isDark = LocalPalette.current.isDark
    val dim = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${fmtWidth(range.start)} pt", color = dim, fontSize = 9.sp)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = PurpleTheme.sliderColors(isDark),
            modifier = Modifier.weight(1f).height(26.dp),
        )
        Text("${fmtWidth(range.endInclusive)} pt", color = dim, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** The bottom toggle pill: title + sub-caption left, switch right. */
@Composable
private fun TogglePill(title: String, caption: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val isDark = LocalPalette.current.isDark
    val trackBg = if (checked) {
        Color(0xFF34C759) // Apple Green
    } else {
        if (isDark) Color(0xFF39393D) else Color(0xFFE5E5EA)
    }
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 14.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 1800f),
        label = "pillSwitchKnob",
    )

    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF141416) else Color(0xFFF7F7F9))
            .border(0.6.dp, if (isDark) Color(0xFF28282C) else Color(0xFFE9E9EF), RoundedCornerShape(12.dp))
            .bouncyClickable { onChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = if (isDark) Color(0xFFF4F4F6) else Color(0xFF18181B), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(1.dp))
            Text(caption, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A), fontSize = 9.5.sp)
        }
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(trackBg),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(12.dp)
                    .shadow(elevation = 1.dp, shape = CircleShape, spotColor = Color(0x33000000))
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

// --- small vector glyphs for section headers ---------------------------------

@Composable
private fun ThicknessGlyph(accent: Color) {
    val isDark = LocalPalette.current.isDark
    Box(
        Modifier
            .size(13.dp)
            .clip(CircleShape)
            .background(if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5)),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(accent.copy(alpha = 0.75f)))
    }
}

@Composable
private fun SlidersGlyph(accent: Color) {
    Icon(PnotesIcons.sliders, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
}

@Composable
private fun PressureGlyph(accent: Color) {
    Icon(PnotesIcons.pan, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
}

@Composable
private fun WaveGlyph(green: Color) {
    Icon(PnotesIcons.highlighter, contentDescription = null, tint = green, modifier = Modifier.size(13.dp))
}

/**
 * Shape-tool configuration popup: kind picker, Live Shape Preview Banner, WIDTH, FILL, DASHED, NEON.
 */
@Composable
fun ShapeConfigPopup(editor: ToolPopupHost, onDismiss: () -> Unit, visible: Boolean = true) {
    var kind by remember { mutableStateOf(editor.hostShapeConfig.shape) }
    var width by remember { mutableStateOf(editor.hostShapeConfig.strokeWidth.toFloat()) }
    var fill by remember { mutableStateOf(editor.hostShapeConfig.fill) }
    var fillOpacity by remember { mutableStateOf((editor.hostShapeConfig.fillAlpha * 100).toFloat()) }
    var glow by remember { mutableStateOf(editor.hostShapeConfig.neon) }
    var glowIntensity by remember { mutableStateOf(ToolConversions.neonStrengthToIntensity(editor.hostShapeConfig.neonStrength).toFloat()) }
    var dashed by remember { mutableStateOf(editor.hostShapeConfig.dashed) }
    var dashLen by remember { mutableStateOf(editor.hostShapeConfig.dashLength.toFloat()) }
    var gapLen by remember { mutableStateOf(editor.hostShapeConfig.dashGap.toFloat()) }

    val activeColor = (editor.hostToolbarColors.getOrNull(editor.hostActiveColorIndex) ?: Rgba(10, 132, 255)).toComposeColor()

    fun emit() = editor.updateShapeConfig(
        ShapeConfig(
            shape = kind,
            strokeWidth = width.toDouble(),
            fill = fill,
            fillAlpha = (fillOpacity / 100.0).coerceIn(ShapeConfig.FILL_ALPHA_MIN, ShapeConfig.FILL_ALPHA_MAX),
            neon = glow,
            neonStrength = ToolConversions.intensityToNeonStrength(glowIntensity.toDouble()),
            dashed = dashed,
            dashLength = dashLen.toDouble(),
            dashGap = gapLen.toDouble(),
        ),
    )

    DropdownMenu(expanded = visible, onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(350.dp)
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            PopupHeaderBar(title = "Shape", icon = PnotesIcons.shape, onReset = null, onClose = onDismiss)

            Spacer(Modifier.height(10.dp))

            // === 1. Live Interactive Shape Preview Banner ===
            LiveShapePreviewBanner(
                kind = kind,
                width = width,
                fill = fill,
                fillAlpha = fillOpacity,
                neon = glow,
                neonIntensity = glowIntensity,
                dashed = dashed,
                dashLen = dashLen,
                gapLen = gapLen,
                color = activeColor,
            )

            Spacer(Modifier.height(10.dp))

            // === 2. Shape Kind Selection Chips ===
            SectionCard {
                SectionCardHeader(title = "Shape Type", glyph = { SlidersGlyph(if (isDarkTheme()) Color(0xFFFAFAFA) else Color(0xFF09090B)) })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShapeKind.DRAW_TOOL_KINDS.forEach { k ->
                        val isSelected = kind == k
                        KindChip(
                            icon = if (isSelected) shapeFilledIcon(k) else shapeIcon(k),
                            label = k.id,
                            selected = isSelected,
                        ) { kind = k; emit() }
                    }
                }
                Spacer(Modifier.height(10.dp))
                MinMaxSlider(width, 1f..50f) { width = it; emit() }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard {
                ToggleRow("FILL", fill) { fill = it; emit() }
                if (fill) {
                    val minPct = (ShapeConfig.FILL_ALPHA_MIN * 100).toFloat()
                    SliderRow("OPACITY", fillOpacity, minPct..100f, suffix = "%") { fillOpacity = it; emit() }
                }
                ToggleRow("DASHED", dashed) { dashed = it; emit() }
                if (dashed) {
                    SliderRow("DASH", dashLen, 2f..40f, suffix = "px") { dashLen = it; emit() }
                    SliderRow("GAP", gapLen, 2f..40f, suffix = "px") { gapLen = it; emit() }
                }
                ToggleRow("NEON", glow) { glow = it; emit() }
                if (glow) {
                    SliderRow("INTENSITY", glowIntensity, 0f..100f, suffix = "%") { glowIntensity = it; emit() }
                }
            }
        }
    }
}

/** Live shape preview rendered inside shape config popup. */
@Composable
private fun LiveShapePreviewBanner(
    kind: ShapeKind,
    width: Float,
    fill: Boolean,
    fillAlpha: Float,
    neon: Boolean,
    neonIntensity: Float,
    dashed: Boolean,
    dashLen: Float,
    gapLen: Float,
    color: Color,
) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0x40000000) else Color(0x33FFFFFF))
            .border(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x18000000), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeW = width.coerceIn(1.5f, 16f)
            val pathEffect = if (dashed) {
                PathEffect.dashPathEffect(floatArrayOf(dashLen.coerceAtLeast(2f), gapLen.coerceAtLeast(2f)), 0f)
            } else null

            val cx = w / 2f
            val cy = h / 2f
            val rx = w * 0.28f
            val ry = h * 0.34f

            when (kind) {
                ShapeKind.RECTANGLE -> {
                    val rectLeft = cx - rx
                    val rectTop = cy - ry
                    val rectWidth = rx * 2f
                    val rectHeight = ry * 2f
                    if (fill) {
                        drawRect(
                            color = color.copy(alpha = (fillAlpha / 100f).coerceIn(0.1f, 1.0f)),
                            topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                        )
                    }
                    if (neon) {
                        drawRect(
                            color = color.copy(alpha = (neonIntensity / 100f * 0.45f).coerceIn(0.1f, 0.6f)),
                            topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                            style = Stroke(width = strokeW * 2.5f, pathEffect = pathEffect),
                        )
                    }
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                        size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                        style = Stroke(width = strokeW, pathEffect = pathEffect),
                    )
                }
                ShapeKind.CIRCLE, ShapeKind.ELLIPSE -> {
                    if (fill) {
                        drawOval(
                            color = color.copy(alpha = (fillAlpha / 100f).coerceIn(0.1f, 1.0f)),
                            topLeft = androidx.compose.ui.geometry.Offset(cx - rx, cy - ry),
                            size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f),
                        )
                    }
                    if (neon) {
                        drawOval(
                            color = color.copy(alpha = (neonIntensity / 100f * 0.45f).coerceIn(0.1f, 0.6f)),
                            topLeft = androidx.compose.ui.geometry.Offset(cx - rx, cy - ry),
                            size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f),
                            style = Stroke(width = strokeW * 2.5f, pathEffect = pathEffect),
                        )
                    }
                    drawOval(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(cx - rx, cy - ry),
                        size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f),
                        style = Stroke(width = strokeW, pathEffect = pathEffect),
                    )
                }
                ShapeKind.LINE -> {
                    val p = Path().apply {
                        moveTo(cx - rx, cy + ry * 0.6f)
                        lineTo(cx + rx, cy - ry * 0.6f)
                    }
                    if (neon) {
                        drawPath(p, color.copy(alpha = (neonIntensity / 100f * 0.45f).coerceIn(0.1f, 0.6f)), style = Stroke(width = strokeW * 2.5f, cap = StrokeCap.Round, pathEffect = pathEffect))
                    }
                    drawPath(p, color, style = Stroke(width = strokeW, cap = StrokeCap.Round, pathEffect = pathEffect))
                }
                ShapeKind.POLYLINE -> {
                    val p = Path().apply {
                        moveTo(cx - rx, cy + ry * 0.5f)
                        lineTo(cx, cy + ry * 0.5f)
                        lineTo(cx, cy - ry * 0.5f)
                        lineTo(cx + rx, cy - ry * 0.5f)
                    }
                    if (neon) {
                        drawPath(p, color.copy(alpha = (neonIntensity / 100f * 0.45f).coerceIn(0.1f, 0.6f)), style = Stroke(width = strokeW * 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = pathEffect))
                    }
                    drawPath(p, color, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = pathEffect))
                }
                ShapeKind.ARROW -> {
                    val p = Path().apply {
                        moveTo(cx - rx, cy)
                        lineTo(cx + rx, cy)
                        moveTo(cx + rx - 14f, cy - 10f)
                        lineTo(cx + rx, cy)
                        lineTo(cx + rx - 14f, cy + 10f)
                    }
                    if (neon) {
                        drawPath(p, color.copy(alpha = (neonIntensity / 100f * 0.45f).coerceIn(0.1f, 0.6f)), style = Stroke(width = strokeW * 2.5f, cap = StrokeCap.Round, pathEffect = pathEffect))
                    }
                    drawPath(p, color, style = Stroke(width = strokeW, cap = StrokeCap.Round, pathEffect = pathEffect))
                }
                ShapeKind.TRIANGLE, ShapeKind.POLYGON -> {
                    val p = Path().apply {
                        moveTo(cx, cy - ry)
                        lineTo(cx + rx, cy + ry)
                        lineTo(cx - rx, cy + ry)
                        close()
                    }
                    if (fill) {
                        drawPath(p, color.copy(alpha = (fillAlpha / 100f).coerceIn(0.1f, 1.0f)))
                    }
                    if (neon) {
                        drawPath(p, color.copy(alpha = (neonIntensity / 100f * 0.45f).coerceIn(0.1f, 0.6f)), style = Stroke(width = strokeW * 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = pathEffect))
                    }
                    drawPath(p, color, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = pathEffect))
                }
                ShapeKind.CURVE -> {
                    val p = Path().apply {
                        moveTo(cx - rx, cy + ry * 0.5f)
                        cubicTo(cx - rx * 0.3f, cy - ry * 1.1f, cx + rx * 0.3f, cy + ry * 1.1f, cx + rx, cy - ry * 0.5f)
                    }
                    if (neon) {
                        drawPath(p, color.copy(alpha = (neonIntensity / 100f * 0.45f).coerceIn(0.1f, 0.6f)), style = Stroke(width = strokeW * 2.5f, cap = StrokeCap.Round, pathEffect = pathEffect))
                    }
                    drawPath(p, color, style = Stroke(width = strokeW, cap = StrokeCap.Round, pathEffect = pathEffect))
                }
            }
        }
    }
}

/** Eraser configuration popup: STROKE/PRECISION mode picker, live preview banner, quick presets, filtering, and radius slider. */
@Composable
fun EraserConfigPopup(editor: ToolPopupHost, onDismiss: () -> Unit, visible: Boolean = true) {
    val base = remember { editor.toolConfig(Tool.ERASER) }
    var area by remember { mutableStateOf(base.eraseMode == EraseMode.AREA) }
    var size by remember { mutableStateOf(base.baseWidth.toFloat()) }
    var switchBack by remember { mutableStateOf(base.switchBackAfterErase) }
    var twoFingerTap by remember { mutableStateOf(base.twoFingerTapToggle) }
    var scale by remember { mutableStateOf(base.scale) }
    var highlighterOnly by remember { mutableStateOf(base.eraseHighlighterOnly) }
    var eraseShapes by remember { mutableStateOf(base.eraseShapes) }

    fun emit() = editor.updateToolConfig(
        Tool.ERASER,
        base.copy(
            baseWidth = size.toDouble(),
            eraseMode = if (area) EraseMode.AREA else EraseMode.STROKE,
            switchBackAfterErase = switchBack,
            twoFingerTapToggle = twoFingerTap,
            scale = scale,
            eraseHighlighterOnly = highlighterOnly,
            eraseShapes = eraseShapes,
        ),
    )

    DropdownMenu(expanded = visible, onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(340.dp)
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            PopupHeaderBar(
                title = "Eraser Settings",
                icon = PnotesIcons.eraser,
                onReset = {
                    val defaults = ToolDefaults.configFor(Tool.ERASER)
                    editor.updateToolConfig(Tool.ERASER, defaults)
                    area = defaults.eraseMode == EraseMode.AREA
                    size = defaults.baseWidth.toFloat()
                    switchBack = defaults.switchBackAfterErase
                    twoFingerTap = defaults.twoFingerTapToggle
                    scale = defaults.scale
                    highlighterOnly = defaults.eraseHighlighterOnly
                    eraseShapes = defaults.eraseShapes
                },
                onClose = onDismiss,
            )

            Spacer(Modifier.height(10.dp))

            // Live Eraser Area Preview
            LiveEraserPreviewBanner(area = area, sizeRadius = size)

            Spacer(Modifier.height(10.dp))

            SectionCard {
                SectionCardHeader(title = "Eraser Mode & Size", glyph = { ThicknessGlyph(LocalPalette.current.let { if (it.isDark) Color(0xFFFAFAFA) else Color(0xFF09090B) }) })
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkTheme()) Color(0xFF18181B) else Color(0xFFEFEFF2))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    SegmentedTab("Precision", area, Modifier.weight(1f)) { area = true; emit() }
                    SegmentedTab("Stroke", !area, Modifier.weight(1f)) { area = false; emit() }
                }
                Spacer(Modifier.height(10.dp))

                // Quick Size Presets (Small / Medium / Large)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val presets = listOf(8f to "Small (8px)", 24f to "Medium (24px)", 64f to "Large (64px)")
                    presets.forEach { (presetVal, label) ->
                        val isSelected = kotlin.math.abs(size - presetVal) < 1.0f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) (if (isDarkTheme()) Color(0xFF2C3E50) else Color(0xFFD6E4FF))
                                    else (if (isDarkTheme()) Color(0xFF18181B) else Color(0xFFEFEFF2))
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF007AFF) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    size = presetVal
                                    emit()
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF007AFF) else (if (isDarkTheme()) PnotesDarkText1 else PnotesLightText1),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                val r = ToolConversions.widthRange(Tool.ERASER)
                MinMaxSlider(size, r.start.toFloat()..r.endInclusive.toFloat()) { size = it; emit() }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard {
                SectionCardHeader(title = "Target Filtering", glyph = { ThicknessGlyph(LocalPalette.current.let { if (it.isDark) Color(0xFFFAFAFA) else Color(0xFF09090B) }) })
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    label = "ERASE HIGHLIGHTER ONLY",
                    checked = highlighterOnly,
                    subtitle = "Only erase highlighter marks; handwriting ink remains protected",
                ) {
                    highlighterOnly = it
                    emit()
                }
                ToggleRow(
                    label = "ERASE SHAPES",
                    checked = eraseShapes,
                    subtitle = "Include recognized geometric shapes and diagram lines",
                ) {
                    eraseShapes = it
                    emit()
                }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard {
                SectionCardHeader(title = "Behavior & Gestures", glyph = { ThicknessGlyph(LocalPalette.current.let { if (it.isDark) Color(0xFFFAFAFA) else Color(0xFF09090B) }) })
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    label = "AUTO-SWITCH BACK",
                    checked = switchBack,
                    subtitle = "Return to previous drawing pen immediately after lifting eraser",
                ) {
                    switchBack = it
                    emit()
                }
                ToggleRow(
                    label = "SCALE WITH ZOOM",
                    checked = scale,
                    subtitle = "Eraser radius scales dynamically as canvas zooms",
                ) {
                    scale = it
                    emit()
                }
                ToggleRow(
                    label = "2-FINGER TAP TOGGLE",
                    checked = twoFingerTap,
                    subtitle = "Quick 2-finger tap on canvas instantly switches to eraser",
                ) {
                    twoFingerTap = it
                    emit()
                }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkTheme()) Color(0x33FF453A) else Color(0x18FF3B30))
                        .border(1.dp, Color(0x40FF453A), RoundedCornerShape(8.dp))
                        .clickable {
                            editor.clearActivePage()
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = PnotesIcons.trash,
                        contentDescription = "Clear Page",
                        tint = Color(0xFFFF453A),
                        modifier = Modifier.size(18.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clear Current Page",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF453A),
                        )
                        Text(
                            text = "Erase all handwriting and strokes on this page",
                            fontSize = 10.sp,
                            color = if (isDarkTheme()) PnotesDarkText2 else PnotesLightText2,
                        )
                    }
                }
            }
        }
    }
}

/** A text segmented tab used inside section cards with Apple elevated pill treatment. */
@Composable
private fun SegmentedTab(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isDark = isDarkTheme()
    val tabShape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .clip(tabShape)
            .then(
                if (active) {
                    Modifier
                        .background(Color(0xFF007AFF))
                } else {
                    Modifier
                        .background(if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5))
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) Color.White else (if (isDark) PnotesDarkText2 else PnotesLightText2),
            fontSize = 10.5.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun isDarkTheme(): Boolean = LocalPalette.current.isDark

/** Live eraser target preview with Apple fluid spring scaling. */
@Composable
private fun LiveEraserPreviewBanner(area: Boolean, sizeRadius: Float) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val animatedRadius by androidx.compose.animation.core.animateFloatAsState(
        targetValue = sizeRadius,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.78f, stiffness = 600f),
        label = "eraserPreviewRadius",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isDark) Color(0xFF18181B)
                else Color(0xFFF4F4F5)
            )
            .border(
                1.dp,
                if (isDark) Color(0xFF27272A)
                else Color(0xFFE4E4E7),
                RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = (animatedRadius * 0.7f).coerceIn(4f, size.height * 0.40f)

            drawLine(
                color = if (isDark) Color(0x33FFFFFF) else Color(0x22000000),
                start = androidx.compose.ui.geometry.Offset(cx - 20f, cy),
                end = androidx.compose.ui.geometry.Offset(cx + 20f, cy),
                strokeWidth = 1f,
            )
            drawLine(
                color = if (isDark) Color(0x33FFFFFF) else Color(0x22000000),
                start = androidx.compose.ui.geometry.Offset(cx, cy - 20f),
                end = androidx.compose.ui.geometry.Offset(cx, cy + 20f),
                strokeWidth = 1f,
            )

            drawCircle(
                color = if (area) Color(0x33FF453A) else (if (isDark) Color(0x33FFFFFF) else Color(0x22000000)),
                radius = r,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )
            drawCircle(
                color = if (area) Color(0xFFFF453A) else (if (isDark) Color.White else Color(0xFF1C1C1E)),
                radius = r,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = Stroke(width = 1.5f),
            )
        }
    }
}

/** Page-styles popup: "All Pages" vs "Current Page", paper color, pattern, spacing, opacity. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StylesPopup(editor: Editor, onDismiss: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var docStyle by remember { mutableStateOf(editor.documentStyle) }
    var pageStyle by remember { mutableStateOf(editor.currentPageStyle) }
    val style = if (tab == 0) docStyle else pageStyle
    var showNewNoteRow by remember { mutableStateOf(editor.documentStyle != editor.newNoteStyle) }
    fun apply(next: PageStyle) {
        if (tab == 0) {
            docStyle = next; editor.setDocumentStyle(next)
            if (next != editor.newNoteStyle) showNewNoteRow = true
        } else { pageStyle = next; editor.setCurrentPageStyle(next) }
    }

    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(310.dp)
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            PopupTitle("PAGE STYLES & PATTERN")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeChip("All Pages", tab == 0) { tab = 0 }
                ModeChip("Current Page", tab == 1) { tab = 1 }
            }

            Spacer(Modifier.height(8.dp))
            StyleCaption("PAGE COLOUR")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ModeChip("Default", style.pageColor == null) { apply(style.copy(pageColor = null)) }
                pageColorPresets.forEach { c ->
                    ColorDot(c.toComposeColor(), style.pageColor == c) { apply(style.copy(pageColor = c)) }
                }
                ColorPickerDot(
                    style.pageColor,
                    custom = style.pageColor != null && style.pageColor !in pageColorPresets,
                    onPick = { apply(style.copy(pageColor = it)) },
                    dismissOnPick = false,
                ) { d, p -> PageColorGridPopup(style.pageColor, d, p) }
            }

            Spacer(Modifier.height(8.dp))
            StyleCaption("PATTERN")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ModeChip("Default", style.pattern == null) { apply(style.copy(pattern = null)) }
                ModeChip("None", style.pattern == PagePattern.NONE) { apply(style.copy(pattern = PagePattern.NONE)) }
                ModeChip("Lines", style.pattern == PagePattern.LINES) { apply(style.copy(pattern = PagePattern.LINES)) }
                ModeChip("Dots", style.pattern == PagePattern.DOTS) { apply(style.copy(pattern = PagePattern.DOTS)) }
                ModeChip("Grid", style.pattern == PagePattern.GRID) { apply(style.copy(pattern = PagePattern.GRID)) }
            }

            Spacer(Modifier.height(8.dp))
            val spacing = style.spacing ?: PageStyle.DEFAULT_SPACING
            SliderRow(
                label = "SPACING" + if (style.spacing == null) " (default)" else "",
                value = spacing.toFloat().coerceIn(PageStyle.MIN_SPACING.toFloat(), PageStyle.MAX_SPACING.toFloat()),
                range = PageStyle.MIN_SPACING.toFloat()..PageStyle.MAX_SPACING.toFloat(),
                suffix = "px",
            ) { apply(style.copy(spacing = it.toDouble())) }

            Spacer(Modifier.height(8.dp))
            val effPatternColor = style.patternColor
                ?: (if (tab == 1) docStyle.patternColor else null)
                ?: PageStyle.DEFAULT_PATTERN_COLOR
            StyleCaption("PATTERN COLOUR")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeChip("Default", style.patternColor == null) { apply(style.copy(patternColor = null)) }
                ColorPickerDot(
                    style.patternColor?.copy(a = 255),
                    custom = style.patternColor != null,
                    onPick = { apply(style.copy(patternColor = it.copy(a = effPatternColor.a))) },
                    dismissOnPick = false,
                ) { d, p -> PageColorGridPopup(style.patternColor?.copy(a = 255), d, p) }
            }

            Spacer(Modifier.height(8.dp))
            val opacityPct = effPatternColor.a * 100f / 255f
            SliderRow("OPACITY", opacityPct, 0f..100f, suffix = "%") { pct ->
                apply(style.copy(patternColor = effPatternColor.copy(a = (pct / 100f * 255f).roundToInt().coerceIn(0, 255))))
            }

            if (tab == 0) {
                Spacer(Modifier.height(8.dp))
                if (showNewNoteRow && !docStyle.isEmpty) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().offset(x = (-6).dp)) {
                        Checkbox(
                            checked = !editor.newNoteStyle.isEmpty && docStyle == editor.newNoteStyle,
                            onCheckedChange = { on ->
                                editor.saveNewNoteStyle(if (on) docStyle else PageStyle())
                            },
                            colors = PurpleTheme.checkboxColors(),
                        )
                        Text(
                            "Default for new notes",
                            color = LocalPalette.current.text.toComposeColor(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ModeChip("Reset", false) { apply(PageStyle()) }
                }
            }
        }
    }
}

/** Page-margins popup: margins per edge. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarginsPopup(editor: Editor, onDismiss: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var edge by remember { mutableStateOf(PageEdge.LEFT) }
    var docMargins by remember { mutableStateOf(editor.documentMargins) }
    var pageMargins by remember { mutableStateOf(editor.currentPageMargins) }
    val margins = if (tab == 0) docMargins else pageMargins
    fun apply(next: PageMargins) {
        if (tab == 0) {
            docMargins = next; editor.setDocumentMargins(next)
        } else {
            pageMargins = next; editor.setCurrentPageMargins(next)
        }
    }

    val own = margins.edge(edge)
    val inherited = if (tab == 1) docMargins.edge(edge) else null
    val percent = ((own ?: inherited ?: 0.0) * 100).toFloat().coerceIn(0f, 100f)

    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(300.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            PopupTitle("PAGE MARGINS")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeChip("All Pages", tab == 0) { tab = 0 }
                ModeChip("Current Page", tab == 1) { tab = 1 }
            }

            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PageEdge.entries.forEach { e ->
                    ModeChip(e.label, edge == e) { edge = e }
                }
            }

            Spacer(Modifier.height(8.dp))
            SliderRow(
                label = "${edge.label.uppercase()}" + if (own == null) " (default)" else "",
                value = percent,
                range = 0f..100f,
                suffix = "%",
            ) { apply(margins.withEdge(edge, it.toDouble() / 100.0)) }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ModeChip("Reset", false) { apply(PageMargins()) }
            }
        }
    }
}

/** The toolbar's View menu popup. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViewMenuPopup(editor: Editor, onDismiss: () -> Unit) {
    val defaults = editor.viewDefaults
    val overrides = editor.viewOverrides
    val vs = editor.viewSettings
    var showDefaultRow by remember { mutableStateOf(editor.viewSettings != editor.viewDefaults) }

    fun apply(new: ViewOverrides) {
        editor.updateViewOverrides(new)
        if (editor.viewSettings != editor.viewDefaults) showDefaultRow = true
    }
    fun setMode(v: ViewingMode) = apply(overrides.copy(mode = v))
    fun setVerticalScroll(v: Boolean) = apply(overrides.copy(verticalScroll = v))
    fun setContrast(v: Int) = apply(overrides.copy(contrast = v))
    fun setInvert(v: Int) = apply(overrides.copy(invert = v))
    fun setBrightness(v: Int) = apply(overrides.copy(brightness = v))
    fun setSepia(v: Int) = apply(overrides.copy(sepia = v))
    fun setKeepImages(v: Boolean) = apply(overrides.copy(keepImages = v))
    fun setRotation(v: Int) = apply(overrides.copy(rotation = v))
    fun setScrollbar(v: Boolean) = apply(overrides.copy(scrollbar = v))

    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(310.dp)
                .heightIn(max = 540.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            PopupTitle("VIEW SETTINGS")
            StyleCaption("VIEWING MODE")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeChip("Single", vs.mode == ViewingMode.SINGLE) { setMode(ViewingMode.SINGLE) }
                ModeChip("Double", vs.mode == ViewingMode.DOUBLE) { setMode(ViewingMode.DOUBLE) }
                ModeChip("Cover", vs.mode == ViewingMode.COVER) { setMode(ViewingMode.COVER) }
            }

            Spacer(Modifier.height(8.dp))
            ToggleRow("VERTICAL SCROLLING", vs.verticalScroll) { setVerticalScroll(it) }

            Spacer(Modifier.height(8.dp))
            StyleCaption("PDF COLOUR FILTERS")
            FilterSpinRow("Contrast", vs.contrast, 0, 200) { setContrast(it) }
            FilterSpinRow("Invert", vs.invert, 0, 100) { setInvert(it) }
            FilterSpinRow("Brightness", vs.brightness, 0, 200) { setBrightness(it) }
            FilterSpinRow("Sepia", vs.sepia, 0, 100) { setSepia(it) }
            ToggleRow("DON'T FILTER IMAGES", vs.keepImages) { setKeepImages(it) }

            Spacer(Modifier.height(8.dp))
            SliderRow("ROTATE", vs.rotation.toFloat(), 0f..270f, suffix = "°") { setRotation((it / 90f).roundToInt() * 90) }

            ToggleRow("SCROLLBAR", vs.scrollbar) { setScrollbar(it) }

            Spacer(Modifier.height(8.dp))
            if (showDefaultRow && overrides != ViewOverrides()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().offset(x = (-6).dp)) {
                    Checkbox(
                        checked = defaults != ViewSettings() && vs == defaults,
                        onCheckedChange = { on ->
                            editor.updateViewDefaults(if (on) vs else ViewSettings())
                        },
                        colors = PurpleTheme.checkboxColors(),
                    )
                    Text(
                        "Default for all notes",
                        color = LocalPalette.current.text.toComposeColor(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ModeChip("Reset", false) { apply(ViewOverrides()) }
            }
        }
    }
}

@Composable
private fun FilterSpinRow(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Text(label, color = if (isDark) Color(0xFFEBEBF5) else Color(0xFF1C1C1E), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0x1EFFFFFF) else Color(0x0C000000))
                    .clickable { onChange((value - 5).coerceIn(min, max)) },
                contentAlignment = Alignment.Center,
            ) {
                Text("−", color = palette.text.toComposeColor(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier
                    .width(46.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0x28FFFFFF) else Color(0x12000000))
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$value%",
                    color = palette.accent.toComposeColor(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0x1EFFFFFF) else Color(0x0C000000))
                    .clickable { onChange((value + 5).coerceIn(min, max)) },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = palette.text.toComposeColor(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Page-nav popup: type a page number and jump to it. */
@Composable
fun PageJumpPopup(editor: Editor, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("${editor.pageIndex + 1}") }
    fun go() {
        val n = text.toIntOrNull() ?: return
        editor.goToPage(n - 1)
        onDismiss()
    }
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        Column(Modifier.width(230.dp).padding(horizontal = 16.dp, vertical = 14.dp)) {
            PopupTitle("GO TO PAGE")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldFrame(Modifier.width(72.dp)) {
                    NativeField(
                        value = text,
                        onText = { text = it },
                        modifier = Modifier.weight(1f),
                        numeric = true,
                        maxLen = 5,
                        endAlign = true,
                        autoFocus = true,
                        onDone = { go() },
                    )
                }
                Text(
                    "/ ${editor.pageCount}",
                    color = LocalPalette.current.textDim.toComposeColor(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
                ModeChip("GO", selected = true) { go() }
            }
        }
    }
}

/** Zoom limit popup. */
@Composable
fun ZoomMenuPopup(editor: Editor, onDismiss: () -> Unit) {
    val base = remember { editor.preferences }
    var minOn by remember { mutableStateOf(base.minZoomEnabled) }
    var minPct by remember { mutableStateOf(base.minZoomPercent) }
    var maxOn by remember { mutableStateOf(base.maxZoomEnabled) }
    var maxPct by remember { mutableStateOf(base.maxZoomPercent) }

    fun emit() = editor.applyPreferences(
        editor.preferences.copy(
            minZoomEnabled = minOn, minZoomPercent = minPct,
            maxZoomEnabled = maxOn, maxZoomPercent = maxPct,
        ),
    )

    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        Column(Modifier.width(300.dp).padding(horizontal = 16.dp, vertical = 14.dp)) {
            PopupTitle("ZOOM LIMITS")
            ZoomLimitRow(
                "MIN ZOOM", minOn, minPct,
                onToggle = {
                    minOn = it
                    if (minOn && maxOn && minPct > maxPct) minPct = maxPct
                    emit()
                },
                onValue = {
                    minPct = it.coerceIn(Preferences.ZOOM_LIMIT_MIN_PCT, Preferences.ZOOM_LIMIT_MAX_PCT)
                        .coerceAtMost(if (maxOn) maxPct else Preferences.ZOOM_LIMIT_MAX_PCT)
                    emit()
                },
            )
            ZoomLimitRow(
                "MAX ZOOM", maxOn, maxPct,
                onToggle = {
                    maxOn = it
                    if (maxOn && minOn && maxPct < minPct) maxPct = minPct
                    emit()
                },
                onValue = {
                    maxPct = it.coerceIn(Preferences.ZOOM_LIMIT_MIN_PCT, Preferences.ZOOM_LIMIT_MAX_PCT)
                        .coerceAtLeast(if (minOn) minPct else Preferences.ZOOM_LIMIT_MIN_PCT)
                    emit()
                },
            )
        }
    }
}

@Composable
private fun ZoomLimitRow(label: String, enabled: Boolean, value: Int, onToggle: (Boolean) -> Unit, onValue: (Int) -> Unit) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val color = (if (enabled) (if (isDark) Color(0xFFEBEBF5) else Color(0xFF1C1C1E)) else (if (isDark) Color(0xFF636366) else Color(0xFFAEAEC2)))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(76.dp))
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isDark) Color(0x1EFFFFFF) else Color(0x0C000000))
                .clickable(enabled = enabled) { onValue(value - 10) },
            contentAlignment = Alignment.Center,
        ) {
            Text("−", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .width(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isDark) Color(0x28FFFFFF) else Color(0x12000000))
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$value%",
                color = if (enabled) palette.accent.toComposeColor() else color,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isDark) Color(0x1EFFFFFF) else Color(0x0C000000))
                .clickable(enabled = enabled) { onValue(value + 10) },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = PurpleTheme.switchColors(isDark),
        )
    }
}

/** Select tool popup. */
@Composable
fun SelectConfigPopup(editor: ToolPopupHost, onDismiss: () -> Unit, visible: Boolean = true) {
    val base = remember { editor.toolConfig(Tool.SELECT) }
    var switchBack by remember { mutableStateOf(base.switchBackAfterSelect) }

    fun emit() = editor.updateToolConfig(Tool.SELECT, base.copy(switchBackAfterSelect = switchBack))

    DropdownMenu(
        expanded = visible,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
    ) {
        val palette = LocalPalette.current
        val isDark = palette.isDark
        Column(
            Modifier
                .width(280.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            PopupHeaderBar(title = "Select", icon = PnotesIcons.select, onReset = null, onClose = onDismiss)
            Spacer(Modifier.height(10.dp))
            SectionCard {
                ToggleRow("SWITCH BACK", switchBack) { switchBack = it; emit() }
            }
        }
    }
}

/** Color switcher popup. */
@Composable
fun ColorSwitcherPopup(host: ToolPopupHost, index: Int, onDismiss: () -> Unit) {
    ColorPickerPopup(
        initial = host.hostToolbarColors.getOrNull(index),
        recents = host.hostRecentColors,
        onDismiss = { host.rememberSwatchColor(index); onDismiss() },
        onPick = { host.setSwatchColor(index, it) },
    )
}

@Composable
internal fun PopupTitle(text: String) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val titleColor = if (isDark) Color(0xFFF5F7FA) else Color(0xFF151922)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 6.dp),
    ) {
        Box(
            Modifier
                .size(width = 3.5.dp, height = 13.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) Color.White else Color(0xFF09090B)),
        )
        Text(
            text = text.uppercase(),
            color = titleColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
internal fun StyleCaption(text: String) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    Text(
        text = text,
        color = if (isDark) Color(0xFFD4D4D8) else Color(0xFF71717A),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 3.dp),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = label,
                color = if (isDark) Color(0xFFF5F7FA) else Color(0xFF151922),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = if (isDark) PnotesDarkText2 else PnotesLightText2,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 13.sp,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = PurpleTheme.switchColors(isDark),
        )
    }
}

@Composable
internal fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    suffix: String = "pt",
    onChangeFinished: (() -> Unit)? = null,
    onChange: (Float) -> Unit,
) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = if (enabled) (if (isDark) Color(0xFFF5F7FA) else Color(0xFF151922)) else (if (isDark) Color(0xFF636366) else Color(0xFFAEAEC2)),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            )
            val badgeShape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .clip(badgeShape)
                    .background(if (isDark) Color(0x26FFFFFF) else Color(0x14000000))
                    .border(0.75.dp, if (isDark) Color(0x3DFFFFFF) else Color(0x24000000), badgeShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${value.roundToInt()} $suffix",
                    color = if (enabled) (if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)) else (if (isDark) Color(0xFF8E8E93) else Color(0xFFAEAEC2)),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onChangeFinished,
            valueRange = range,
            enabled = enabled,
            colors = PurpleTheme.sliderColors(isDark),
            modifier = Modifier.height(26.dp),
        )
    }
}

private fun shapeDrawable(kind: ShapeKind): Int? = when (kind) {
    ShapeKind.LINE, ShapeKind.POLYLINE -> R.drawable.ic_shape_line
    ShapeKind.CURVE -> R.drawable.ic_shape_curve
    ShapeKind.ARROW -> R.drawable.ic_shape_arrow
    ShapeKind.RECTANGLE -> R.drawable.ic_shape_rect
    ShapeKind.CIRCLE -> R.drawable.ic_shape_circle
    ShapeKind.ELLIPSE -> R.drawable.ic_shape_ellipse
    ShapeKind.TRIANGLE, ShapeKind.POLYGON -> R.drawable.ic_shape_triangle
}

private fun shapeIcon(kind: ShapeKind): ImageVector = when (kind) {
    ShapeKind.POLYLINE -> PnotesIcons.connectorElbow
    ShapeKind.CURVE -> PnotesIcons.connectorCurved
    ShapeKind.LINE -> PnotesIcons.shapeLine
    ShapeKind.ARROW -> PnotesIcons.shapeArrow
    ShapeKind.RECTANGLE -> PnotesIcons.shapeRect
    ShapeKind.ELLIPSE -> PnotesIcons.shapeEllipse
    ShapeKind.CIRCLE -> PnotesIcons.shapeCircle
    ShapeKind.TRIANGLE, ShapeKind.POLYGON -> PnotesIcons.shapeTriangle
}

private fun shapeFilledIcon(kind: ShapeKind): ImageVector = when (kind) {
    ShapeKind.POLYLINE -> PnotesIcons.connectorElbow
    ShapeKind.CURVE -> PnotesIcons.connectorCurved
    ShapeKind.LINE -> PnotesIcons.shapeLineFilled
    ShapeKind.ARROW -> PnotesIcons.shapeArrowFilled
    ShapeKind.RECTANGLE -> PnotesIcons.shapeRectFilled
    ShapeKind.ELLIPSE -> PnotesIcons.shapeEllipseFilled
    ShapeKind.CIRCLE -> PnotesIcons.shapeCircleFilled
    ShapeKind.TRIANGLE, ShapeKind.POLYGON -> PnotesIcons.shapeTriangleFilled
}

@Composable
private fun KindChip(
    icon: ImageVector? = null,
    drawableRes: Int? = null,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else (if (isHovered) 1.08f else 1.0f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "kindChipScale",
    )

    val chipShape = RoundedCornerShape(9.dp)
    val chipModifier = if (selected) {
        Modifier
            .shadow(2.dp, shape = chipShape, spotColor = Color(0x33007AFF))
            .clip(chipShape)
            .background(Color(0xFF007AFF))
            .border(1.dp, Color(0xFF007AFF), chipShape)
    } else {
        Modifier
            .clip(chipShape)
            .background(if (isDark) PnotesDarkElevated else PnotesLightSurface2)
            .border(0.8.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, chipShape)
    }

    AppleTooltip(
        tooltip = label,
        position = TooltipPosition.TOP,
        interactionSource = interactionSource,
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(chipModifier)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(8.dp),
        ) {
            val iconTint = if (selected) {
                Color.White
            } else {
                if (isDark) PnotesDarkText2 else PnotesLightText2
            }
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            } else if (drawableRes != null) {
                Icon(
                    painter = painterResource(drawableRes),
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * DropdownMenu matching Apple iPadOS modern translucent rounded styling.
 *
 * Every tool panel in the app opens through here, so the enter and exit animations live here too
 * instead of in each of the nine call sites.
 *
 * A menu is a popup window, and the platform destroys that window the moment `expanded` goes
 * false — so a panel closed by its caller would vanish in the frame it was dismissed, which is
 * exactly the blink this fixes. The window is therefore held open for one exit duration while its
 * contents fade and shrink, and only then unmounted. `expanded` stays the caller's business: this
 * composable keeps rendering while [alive] is true, which is strictly longer than the caller
 * asked for.
 */
@Composable
internal fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: androidx.compose.ui.unit.DpOffset = androidx.compose.ui.unit.DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(22.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val glassShape = shape
    val motion = LocalMotion.current

    val shown = remember { MutableTransitionState(false) }
    var alive by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (expanded) {
            alive = true
            shown.targetState = true
        } else {
            shown.targetState = false
            delay(Motion.exitMillis(motion.reduced).toLong())
            alive = false
        }
    }
    if (!alive) return

    androidx.compose.material3.DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        offset = offset,
        modifier = modifier,
        properties = properties,
        shape = glassShape,
        containerColor = Color.Transparent,
        border = null,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {

        AnimatedVisibility(
            visibleState = shown,
            enter = fadeIn(animationSpec = motion.dropdownEnter()) + scaleIn(
                initialScale = motion.enterScale,
                animationSpec = motion.dropdownEnter(),
            ),
            exit = fadeOut(animationSpec = motion.dropdownExit()) + scaleOut(
                targetScale = motion.exitScale,
                animationSpec = motion.dropdownExit(),
            ),
        ) {
            MenuGlassCard(glassShape = glassShape, isDark = isDark, content = content)
        }
    }
}

/** The solid opaque card a menu draws: standard shadow, 100% solid surface, crisp solid border, then [content]. */
@Composable
private fun MenuGlassCard(
    glassShape: androidx.compose.ui.graphics.Shape,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderColor = if (isDark) Color(0xFF38383E) else Color(0xFFE4E4E7)
    val bgColor = if (isDark) Color(0xFF202024) else Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .shadow(
                elevation = 10.dp,
                shape = glassShape,
            )
            .clip(glassShape)
            .background(bgColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = glassShape,
            ),
    ) {
        Column {
            content()
        }
    }
}

/** Modern tactile ModeChip. */
@Composable
internal fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "modeChipScale",
    )

    val chipShape = RoundedCornerShape(9.dp)
    val chipModifier = if (selected) {
        Modifier
            .shadow(2.dp, shape = chipShape, spotColor = Color(0x33007AFF))
            .clip(chipShape)
            .background(Color(0xFF007AFF))
            .border(1.dp, Color(0xFF007AFF), chipShape)
    } else {
        Modifier
            .clip(chipShape)
            .background(if (isDark) PnotesDarkElevated else PnotesLightSurface2)
            .border(0.8.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, chipShape)
    }

    val textColor = if (selected) {
        Color.White
    } else {
        if (isDark) PnotesDarkText2 else PnotesLightText2
    }

    Box(
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(chipModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun FontMenuItems(
    current: FontFace?,
    monoOnly: Boolean = false,
    withDefault: Boolean = false,
    onPick: (FontFace?) -> Unit,
) {
    val palette = LocalPalette.current
    if (withDefault) {
        DropdownMenuItem(
            text = {
                Text(
                    "Default",
                    color = (if (current == null) palette.accent else palette.text).toComposeColor(),
                    fontSize = 14.sp,
                )
            },
            onClick = { onPick(null) },
        )
    }
    for (choice in FontCatalog.choices()) {
        if (monoOnly && !choice.mono) continue
        DropdownMenuItem(
            text = {
                Text(
                    choice.label,
                    color = (if (choice.face == current) palette.accent else palette.text).toComposeColor(),
                    style = TextStyle(fontFamily = choice.face.toComposeFamily(), fontSize = 14.sp),
                )
            },
            onClick = { onPick(choice.face) },
        )
    }
}
