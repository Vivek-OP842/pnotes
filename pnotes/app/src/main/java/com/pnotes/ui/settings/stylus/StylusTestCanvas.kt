package com.pnotes.ui.settings.stylus

import com.pnotes.ui.theme.PurpleTheme
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import com.pnotes.core.inking.InkPoint
import com.pnotes.core.inking.InkingInputListener
import com.pnotes.core.inking.InkingInputProcessor
import com.pnotes.core.inking.InkingStroke
import com.pnotes.core.inking.InkingStrokeSegment
import com.pnotes.core.inking.NeoPenConfig
import com.pnotes.core.inking.NeoPenStrokeEngine
import com.pnotes.core.inking.PenProfile
import com.pnotes.core.inking.PenType
import com.pnotes.settings.stylus.StylusSettings
import com.pnotes.ui.PnotesTokens
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.LocalPalette


/**
 * Interactive, zero-latency scratchpad canvas for real-time stylus pressure and stroke testing.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StylusTestCanvas(
    settings: StylusSettings,
    clearSignalKey: Int,
    onLiveTelemetry: (raw: Float, calibrated: Float, velocity: Float, width: Float) -> Unit,
    onPenSelected: (PenType) -> Unit,
    onBaseWidthChanged: (Float) -> Unit,
    onColorChanged: (Long) -> Unit,
    onClearClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalPalette.current.isDark
    val completedStrokes = remember { mutableStateListOf<InkingStroke>() }
    val currentStrokeSegments = remember { mutableStateListOf<InkingStrokeSegment>() }

    val testColors = remember(isDark) {
        listOf(
            if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A), // Neutral Charcoal
            if (isDark) Color(0xFFFFFFFF) else Color(0xFF151922), // Ink Black / Crisp White
            Color(0xFF8B5CF6), // Royal Purple
            Color(0xFFF43F5E), // Rose Red
            Color(0xFF10B981), // Emerald Green
            Color(0xFFFFE600), // Highlighter Yellow
        )
    }

    var activeColor by remember(settings.strokeColorHex) {
        mutableStateOf(Color(settings.strokeColorHex))
    }

    val penProfile = remember(settings.selectedPenType) {
        PenProfile.forType(settings.selectedPenType)
    }

    // Star Notes (Onyx neo3pen native engine) exact pipeline port:
    // StrokePointProducer -> SmoothFilter(smoothLevel) -> SplineProducer -> width calc.
    val strokeEngine = remember(settings.selectedPenType, settings.pressureGamma, settings.velocityTaperingEnabled, settings.strokeStabilization, activeColor, settings.baseStrokeWidth) {
        NeoPenStrokeEngine(
            config = NeoPenConfig.fromStylusSettings(
                profile = penProfile,
                baseWidth = settings.baseStrokeWidth,
                stabilization = settings.strokeStabilization,
                velocityTaperingEnabled = settings.velocityTaperingEnabled,
                gamma = settings.pressureGamma,
            ),
        ).apply { color = activeColor }
    }

    // Clear canvas when signal triggers
    LaunchedEffect(clearSignalKey) {
        completedStrokes.clear()
        currentStrokeSegments.clear()
    }

    val inputProcessor = remember(settings.stylusOnlyMode, settings.pressureGamma) {
        InkingInputProcessor(
            stylusOnlyMode = settings.stylusOnlyMode,
            gamma = settings.pressureGamma,
            listener = object : InkingInputListener {
                override fun onStrokeStart(point: InkPoint) {
                    currentStrokeSegments.clear()
                    val seg = strokeEngine.startStroke(point)
                    if (seg != null) currentStrokeSegments.add(seg)
                    val width = strokeEngine.computeStrokeWidth(point.pressure, 0f)
                    onLiveTelemetry(point.pressure, point.pressure, 0f, width)
                }

                override fun onStrokePoint(point: InkPoint, velocity: Float) {
                    val seg = strokeEngine.addPoint(point, velocity)
                    if (seg != null) {
                        currentStrokeSegments.add(seg)
                    }
                    val width = strokeEngine.computeStrokeWidth(point.pressure, velocity)
                    val calibrated = point.pressure.toDouble().pow(settings.pressureGamma.toDouble()).toFloat()
                    onLiveTelemetry(point.pressure, calibrated, velocity, width)
                }

                override fun onStrokeEnd(point: InkPoint) {
                    val finished = strokeEngine.endStroke(point)
                    if (finished.segments.isNotEmpty()) {
                        completedStrokes.add(finished)
                    }
                    currentStrokeSegments.clear()
                    onLiveTelemetry(0f, 0f, 0f, 0f)
                }

                override fun onStrokeCancel() {
                    currentStrokeSegments.clear()
                    onLiveTelemetry(0f, 0f, 0f, 0f)
                }

                override fun onLivePressure(rawPressure: Float, calibratedPressure: Float) {
                    // Passed directly through stroke points
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Pen Profile Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PenType.values().forEach { type ->
                val isSelected = settings.selectedPenType == type
                Surface(
                    onClick = { onPenSelected(type) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) (if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5)) else (if (isDark) Color(0xFF141A24) else Color(0xFFF5F0E6)),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0x2888A0C0) else Color(0xFFDED6C7))
                    ),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = type.displayName,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) (if (isDark) Color.White else Color(0xFF09090B)) else PnotesTokens.textSecondary
                        )
                    }
                }
            }
        }

        // 2. Scratchpad Drawing Area (Authentic Note Paper Feeling)
        val scratchpadBg = if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .shadow(if (isDark) 0.dp else 2.dp, RoundedCornerShape(18.dp), spotColor = Color(0x20000000))
                .clip(RoundedCornerShape(18.dp))
                .background(scratchpadBg)
                .border(1.dp, PnotesTokens.cardBorder, RoundedCornerShape(18.dp))
                .pointerInput(strokeEngine, settings.stylusOnlyMode, settings.pressureGamma) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        val isStylus = down.type == PointerType.Stylus || down.type == PointerType.Eraser

                        if (settings.stylusOnlyMode && !isStylus) {
                            return@awaitEachGesture
                        }

                        val rawP = if (isStylus || down.pressure > 0f) down.pressure.coerceIn(0.01f, 1f) else 0.5f
                        val downPoint = InkPoint(
                            x = down.position.x,
                            y = down.position.y,
                            pressure = rawP,
                            timestamp = System.currentTimeMillis()
                        )

                        currentStrokeSegments.clear()
                        val startSeg = strokeEngine.startStroke(downPoint)
                        if (startSeg != null) currentStrokeSegments.add(startSeg)
                        val initialWidth = strokeEngine.computeStrokeWidth(downPoint.pressure, 0f)
                        val calDown = downPoint.pressure.toDouble().pow(settings.pressureGamma.toDouble()).toFloat()
                        onLiveTelemetry(downPoint.pressure, calDown, 0f, initialWidth)

                        var lastPos = down.position
                        var lastTime = System.currentTimeMillis()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                change.consume()
                                val upPoint = InkPoint(
                                    x = change.position.x,
                                    y = change.position.y,
                                    pressure = rawP,
                                    timestamp = System.currentTimeMillis()
                                )
                                val finished = strokeEngine.endStroke(upPoint)
                                if (finished.segments.isNotEmpty()) {
                                    completedStrokes.add(finished)
                                }
                                currentStrokeSegments.clear()
                                onLiveTelemetry(0f, 0f, 0f, 0f)
                                break
                            }

                            change.consume()

                            val now = System.currentTimeMillis()
                            val dt = (now - lastTime).coerceAtLeast(1L)
                            val dx = change.position.x - lastPos.x
                            val dy = change.position.y - lastPos.y
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            val velocity = dist / dt.toFloat()

                            lastPos = change.position
                            lastTime = now

                            val moveRawP = if (isStylus || change.pressure > 0f) change.pressure.coerceIn(0.01f, 1f) else 0.5f
                            val movePoint = InkPoint(
                                x = change.position.x,
                                y = change.position.y,
                                pressure = moveRawP,
                                timestamp = now
                            )

                            // Harvest historical high-frequency points (120Hz/240Hz)
                            for (h in change.historical) {
                                val hPoint = InkPoint(
                                    x = h.position.x,
                                    y = h.position.y,
                                    pressure = moveRawP,
                                    timestamp = h.uptimeMillis
                                )
                                val hSeg = strokeEngine.addPoint(hPoint, velocity)
                                if (hSeg != null) {
                                    currentStrokeSegments.add(hSeg)
                                }
                            }

                            val seg = strokeEngine.addPoint(movePoint, velocity)
                            if (seg != null) {
                                currentStrokeSegments.add(seg)
                            }

                            val curWidth = strokeEngine.computeStrokeWidth(movePoint.pressure, velocity)
                            val calP = movePoint.pressure.toDouble().pow(settings.pressureGamma.toDouble()).toFloat()
                            onLiveTelemetry(movePoint.pressure, calP, velocity, curWidth)
                        }
                    }
                }
        ) {
            // Subtle dot grid background on scratchpad
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 24.dp.toPx()
                val dotColor = if (isDark) Color(0x15FFFFFF) else Color(0x15000000)

                var x = gridSpacing
                while (x < size.width) {
                    var y = gridSpacing
                    while (y < size.height) {
                        drawCircle(dotColor, radius = 1.5f, center = Offset(x, y))
                        y += gridSpacing
                    }
                    x += gridSpacing
                }

                // Render Completed Strokes
                for (stroke in completedStrokes) {
                    val isHighlighter = stroke.profile.penType == PenType.HIGHLIGHTER
                    val blend = if (isHighlighter) BlendMode.SrcOver else BlendMode.SrcOver

                    for (seg in stroke.segments) {
                        val path = Path().apply {
                            moveTo(seg.startX, seg.startY)
                            quadraticTo(seg.controlX, seg.controlY, seg.endX, seg.endY)
                        }
                        val strokeColor = seg.color.copy(alpha = seg.alpha)
                        val avgWidth = (seg.startWidth + seg.endWidth) * 0.5f

                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = avgWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            ),
                            blendMode = blend
                        )
                    }
                }

                // Render Active Drawing Stroke
                for (seg in currentStrokeSegments) {
                    val path = Path().apply {
                        moveTo(seg.startX, seg.startY)
                        quadraticTo(seg.controlX, seg.controlY, seg.endX, seg.endY)
                    }
                    val strokeColor = seg.color.copy(alpha = seg.alpha)
                    val avgWidth = (seg.startWidth + seg.endWidth) * 0.5f

                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = avgWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Scratchpad Hint Watermark when empty
            if (completedStrokes.isEmpty() && currentStrokeSegments.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Stylus Test Scratchpad",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0x35FFFFFF) else Color(0x35000000)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Write or press with stylus to test pressure calibration",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0x22FFFFFF) else Color(0x22000000)
                    )
                }
            }

            // Top-Right "Clear Test Area" Action Button
            Surface(
                onClick = onClearClicked,
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Color(0xFF1A222F) else Color(0xFFF1EDE4),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x3588A0C0) else Color(0xFFDED6C7)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = PnotesIcons.close,
                        contentDescription = "Clear",
                        tint = PnotesTokens.textSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text("Clear Area", fontSize = 11.5.sp, color = PnotesTokens.textSecondary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 3. Quick Color Swatches & Stroke Width Slider Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Swatches
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                testColors.forEach { c ->
                    val isSelected = activeColor.value == c.value
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0x35000000) else Color(0x35000000)),
                                shape = CircleShape
                            )
                            .clickable {
                                activeColor = c
                                onColorChanged(c.value.toLong())
                            }
                    )
                }
            }

            // Stroke Base Width Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.width(180.dp)
            ) {
                Text(
                    text = "${settings.baseStrokeWidth.toInt()}px",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PnotesTokens.textPrimary
                )
                Slider(
                    value = settings.baseStrokeWidth,
                    onValueChange = onBaseWidthChanged,
                    valueRange = 1.0f..30.0f,
                    colors = PurpleTheme.sliderColors(isDark),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
