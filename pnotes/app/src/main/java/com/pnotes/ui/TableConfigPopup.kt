package com.pnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.core.model.Rgba
import com.pnotes.core.model.TableItem
import com.pnotes.core.tools.TableConfig
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.toComposeColor
import kotlin.math.roundToInt

/**
 * Table creation popup dialog matching Star Notes `@layout/popup_table`.
 *
 * Provides:
 * - Row / Column counters with `-` and `+` steppers
 * - Header Row and Header Column switches
 * - Stroke width slider (1.0..8.0 pt)
 * - Stroke border color swatches
 * - Fill color toggle with Star Notes 5 presets (#FFFFFF, #F9DADF, #EC93A9, #5F9EED, #F9D950)
 * - Action button to insert the configured table on canvas
 */
@Composable
fun TableConfigPopup(
    config: TableConfig,
    onConfigChange: (TableConfig) -> Unit,
    onInsertTable: (TableConfig) -> Unit,
    onDismiss: () -> Unit,
    visible: Boolean = true,
) {
    var stateConfig by remember(config, visible) { mutableStateOf(config) }

    val isDark = LocalPalette.current.isDark
    val bg = if (isDark) PnotesDarkCard else PnotesLightCard
    val textCol = if (isDark) PnotesDarkText1 else PnotesLightText1
    val textDim = if (isDark) PnotesDarkText2 else PnotesLightText2
    val borderCol = if (isDark) PnotesDarkBorder else PnotesLightBorder
    val itemBg = if (isDark) PnotesDarkElevated else PnotesLightSurface2

    val fillPresets = listOf(
        Rgba(0xFF, 0xFF, 0xFF, 0xFF), // White
        Rgba(0xFF, 0xF9, 0xDA, 0xDF), // Soft Pink
        Rgba(0xFF, 0xEC, 0x93, 0xA9), // Rose Pink
        Rgba(0xFF, 0x5F, 0x9E, 0xED), // Sky Blue
        Rgba(0xFF, 0xF9, 0xD9, 0x50), // Pastel Yellow
    )

    val strokeColors = listOf(
        Rgba(0x18, 0x18, 0x1B, 0xFF), // Black / Dark Charcoal
        Rgba(0x71, 0x71, 0x7A, 0xFF), // Zinc Grey
        Rgba(0x3B, 0x82, 0xF6, 0xFF), // Blue
        Rgba(0xEF, 0x44, 0x44, 0xFF), // Red
        Rgba(0x10, 0xB9, 0x81, 0xFF), // Emerald Green
    )

    fun update(newConfig: TableConfig) {
        stateConfig = newConfig
        onConfigChange(newConfig)
    }

    DropdownMenu(
        expanded = visible,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .padding(16.dp),
        ) {
            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Insert Table",
                    color = textCol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Row Count Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Rows", color = textCol, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Stepper(
                    value = stateConfig.rows,
                    min = TableItem.MIN_ROWS,
                    max = TableItem.MAX_ROWS,
                    onValueChange = { update(stateConfig.copy(rows = it)) },
                    itemBg = itemBg,
                    textCol = textCol,
                )
            }

            Spacer(Modifier.height(10.dp))

            // Column Count Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Columns", color = textCol, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Stepper(
                    value = stateConfig.cols,
                    min = TableItem.MIN_COLS,
                    max = TableItem.MAX_COLS,
                    onValueChange = { update(stateConfig.copy(cols = it)) },
                    itemBg = itemBg,
                    textCol = textCol,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Header Row Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Header row", color = textCol, fontSize = 14.sp)
                Switch(
                    checked = stateConfig.hasHeaderRow,
                    onCheckedChange = { update(stateConfig.copy(hasHeaderRow = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White),
                )
            }

            // Header Column Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Header column", color = textCol, fontSize = 14.sp)
                Switch(
                    checked = stateConfig.hasHeaderCol,
                    onCheckedChange = { update(stateConfig.copy(hasHeaderCol = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Stroke Width Slider
            Text(
                "Border thickness · ${stateConfig.strokeWidth.roundToInt()} pt",
                color = textDim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Slider(
                value = stateConfig.strokeWidth.toFloat(),
                onValueChange = { update(stateConfig.copy(strokeWidth = it.toDouble())) },
                valueRange = 1f..8f,
                colors = SliderDefaults.colors(
                    thumbColor = if (isDark) Color.White else Color(0xFF18181B),
                    activeTrackColor = if (isDark) Color.White else Color(0xFF18181B),
                ),
            )

            Spacer(Modifier.height(8.dp))

            // Stroke Color Swatches
            Text("Border color", color = textDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                strokeColors.forEach { c ->
                    val isSelected = stateConfig.strokeRgba == c
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c.toComposeColor())
                            .border(
                                width = if (isSelected) 2.dp else 0.5.dp,
                                color = if (isSelected) (if (isDark) Color.White else Color(0xFF18181B)) else borderCol,
                                shape = CircleShape,
                            )
                            .clickable { update(stateConfig.copy(strokeRgba = c)) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Fill Color Switch & Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Fill color", color = textCol, fontSize = 14.sp)
                Switch(
                    checked = stateConfig.fillEnabled,
                    onCheckedChange = { update(stateConfig.copy(fillEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White),
                )
            }

            if (stateConfig.fillEnabled) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fillPresets.forEach { c ->
                        val isSelected = stateConfig.fillRgba == c
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c.toComposeColor())
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) (if (isDark) Color.White else Color(0xFF18181B)) else borderCol,
                                    shape = CircleShape,
                                )
                                .clickable { update(stateConfig.copy(fillRgba = c)) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Insert Table Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color.White else Color(0xFF18181B))
                    .clickable {
                        onInsertTable(stateConfig)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Insert Table",
                    color = if (isDark) Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun Stepper(
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    itemBg: Color,
    textCol: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Decrease button
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(itemBg)
                .clickable(enabled = value > min) { onValueChange(value - 1) },
            contentAlignment = Alignment.Center,
        ) {
            Text("−", color = if (value > min) textCol else textCol.copy(alpha = 0.3f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        // Value text
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("$value", color = textCol, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        // Increase button
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(itemBg)
                .clickable(enabled = value < max) { onValueChange(value + 1) },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = if (value < max) textCol else textCol.copy(alpha = 0.3f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
