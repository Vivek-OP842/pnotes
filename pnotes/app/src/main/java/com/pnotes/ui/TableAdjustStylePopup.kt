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
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
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
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.toComposeColor
import kotlin.math.roundToInt

/**
 * Contextual popup for styling and modifying an already-selected table on canvas (matches Star Notes `@layout/popup_table_adjust_style`).
 *
 * Allows modifying:
 * - Header Row toggle
 * - Header Column toggle
 * - Border stroke thickness slider
 * - Border stroke color swatches
 * - Cell/Header background fill toggle and presets
 * - Equal width / height actions
 * - Add/Remove rows and columns
 * - Delete table action
 */
@Composable
fun TableAdjustStylePopup(
    table: TableItem,
    onTableModified: () -> Unit,
    onDeleteTable: () -> Unit,
    onDismiss: () -> Unit,
) {
    var hasHeaderRow by remember(table) { mutableStateOf(table.hasHeaderRow) }
    var hasHeaderCol by remember(table) { mutableStateOf(table.hasHeaderCol) }
    var strokeWidth by remember(table) { mutableDoubleStateOf(table.strokeWidth) }
    var strokeRgba by remember(table) { mutableStateOf(table.strokeRgba) }
    var fillRgba by remember(table) { mutableStateOf(table.fillRgba) }
    var fillAlpha by remember(table) { mutableFloatStateOf(table.fillRgba?.let { (it.a / 255f).coerceIn(0.05f, 1f) } ?: 1.0f) }
    var rowCount by remember(table) { mutableStateOf(table.rowCount) }
    var colCount by remember(table) { mutableStateOf(table.colCount) }
    var showBorderCustomPicker by remember { mutableStateOf(false) }
    var showFillCustomPicker by remember { mutableStateOf(false) }

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
        Rgba(0x18, 0x18, 0x1B, 0xFF), // Black
        Rgba(0x71, 0x71, 0x7A, 0xFF), // Zinc Grey
        Rgba(0x3B, 0x82, 0xF6, 0xFF), // Blue
        Rgba(0xEF, 0x44, 0x44, 0xFF), // Red
        Rgba(0x10, 0xB9, 0x81, 0xFF), // Emerald Green
    )

    Column(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) TrueGlassDarkSurface else TrueGlassSurface)
            .border(1.dp, if (isDark) TrueGlassDarkLuminousBorder else TrueGlassLuminousBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Table Style", color = textCol, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${rowCount} × ${colCount}", color = textDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                checked = hasHeaderRow,
                onCheckedChange = {
                    hasHeaderRow = it
                    table.hasHeaderRow = it
                    onTableModified()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF34C759),
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                    uncheckedTrackColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                    uncheckedBorderColor = Color.Transparent,
                ),
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
                checked = hasHeaderCol,
                onCheckedChange = {
                    hasHeaderCol = it
                    table.hasHeaderCol = it
                    onTableModified()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF34C759),
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                    uncheckedTrackColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Stroke Width Slider
        Text(
            "Border thickness · ${strokeWidth.roundToInt()} pt",
            color = textDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Slider(
            value = strokeWidth.toFloat(),
            onValueChange = {
                strokeWidth = it.toDouble()
                table.strokeWidth = it.toDouble()
                onTableModified()
            },
            valueRange = 1f..8f,
            colors = SliderDefaults.colors(
                thumbColor = if (isDark) Color.White else Color(0xFF18181B),
                activeTrackColor = if (isDark) Color.White else Color(0xFF18181B),
            ),
        )

        Spacer(Modifier.height(8.dp))

        // Border Color
        Text("Border color", color = textDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            strokeColors.forEach { c ->
                val isSelected = strokeRgba == c
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
                        .clickable {
                            strokeRgba = c
                            table.strokeRgba = c
                            onTableModified()
                        },
                )
            }

            // Custom Border Color Picker Button
            val isCustomBorder = strokeRgba !in strokeColors
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isCustomBorder) strokeRgba.toComposeColor() else itemBg)
                    .border(
                        width = if (isCustomBorder) 2.dp else 0.5.dp,
                        color = if (isCustomBorder) (if (isDark) Color.White else Color(0xFF18181B)) else borderCol,
                        shape = CircleShape,
                    )
                    .clickable { showBorderCustomPicker = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.plus,
                    contentDescription = "Custom Border Color",
                    tint = if (isCustomBorder) (if (!strokeRgba.isDark) Color.Black else Color.White) else textDim,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Fill Color
        Text("Fill color", color = textDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // None / transparent
            val isNone = fillRgba == null
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(itemBg)
                    .border(
                        width = if (isNone) 2.dp else 0.5.dp,
                        color = if (isNone) (if (isDark) Color.White else Color(0xFF18181B)) else borderCol,
                        shape = CircleShape,
                    )
                    .clickable {
                        fillRgba = null
                        table.fillRgba = null
                        onTableModified()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Ø", color = textDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            fillPresets.forEach { c ->
                val baseMatches = fillRgba?.let { it.r == c.r && it.g == c.g && it.b == c.b } == true
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(c.toComposeColor())
                        .border(
                            width = if (baseMatches) 2.dp else 0.5.dp,
                            color = if (baseMatches) (if (isDark) Color.White else Color(0xFF18181B)) else borderCol,
                            shape = CircleShape,
                        )
                        .clickable {
                            val alphaInt = (fillAlpha * 255).roundToInt().coerceIn(1, 255)
                            val newColor = Rgba(c.r, c.g, c.b, alphaInt)
                            fillRgba = newColor
                            table.fillRgba = newColor
                            onTableModified()
                        },
                )
            }

            // Custom Fill Color Picker Button
            val isCustomFill = fillRgba != null && fillPresets.none { it.r == fillRgba!!.r && it.g == fillRgba!!.g && it.b == fillRgba!!.b }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isCustomFill) fillRgba!!.copy(a = 255).toComposeColor() else itemBg)
                    .border(
                        width = if (isCustomFill) 2.dp else 0.5.dp,
                        color = if (isCustomFill) (if (isDark) Color.White else Color(0xFF18181B)) else borderCol,
                        shape = CircleShape,
                    )
                    .clickable { showFillCustomPicker = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.plus,
                    contentDescription = "Custom Fill Color",
                    tint = if (isCustomFill) (if (!fillRgba!!.isDark) Color.Black else Color.White) else textDim,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        // Fill Opacity / Transparency Seekbar Slider (when fill is active)
        if (fillRgba != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Fill opacity · ${(fillAlpha * 100).roundToInt()}%",
                color = textDim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Slider(
                value = fillAlpha,
                onValueChange = {
                    fillAlpha = it
                    val base = fillRgba ?: Rgba(255, 255, 255, 255)
                    val alphaInt = (it * 255).roundToInt().coerceIn(1, 255)
                    val newColor = Rgba(base.r, base.g, base.b, alphaInt)
                    fillRgba = newColor
                    table.fillRgba = newColor
                    onTableModified()
                },
                valueRange = 0.05f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = if (isDark) Color.White else Color(0xFF18181B),
                    activeTrackColor = if (isDark) Color.White else Color(0xFF18181B),
                ),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Row/Col adjust actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(itemBg)
                    .clickable {
                        if (table.addRow()) {
                            rowCount = table.rowCount
                            onTableModified()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("+ Row", color = textCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(itemBg)
                    .clickable {
                        if (table.removeRow()) {
                            rowCount = table.rowCount
                            onTableModified()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("− Row", color = textCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(itemBg)
                    .clickable {
                        if (table.addCol()) {
                            colCount = table.colCount
                            onTableModified()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("+ Col", color = textCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(itemBg)
                    .clickable {
                        if (table.removeCol()) {
                            colCount = table.colCount
                            onTableModified()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("− Col", color = textCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Equalize dimensions actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(itemBg)
                    .clickable {
                        table.setEqualRowHeights()
                        onTableModified()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Equal Rows", color = textCol, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(itemBg)
                    .clickable {
                        table.setEqualColWidths()
                        onTableModified()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Equal Cols", color = textCol, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Delete Table Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDark) Color(0x33EF4444) else Color(0x18EF4444))
                .border(1.dp, if (isDark) Color(0x66EF4444) else Color(0x30EF4444), RoundedCornerShape(8.dp))
                .clickable {
                    onDeleteTable()
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = PnotesIcons.trash,
                    contentDescription = "Delete Table",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Delete Table",
                    color = Color(0xFFEF4444),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    if (showBorderCustomPicker) {
        ColorPickerPopup(
            initial = strokeRgba,
            recents = strokeColors,
            onDismiss = { showBorderCustomPicker = false },
            onPick = {
                strokeRgba = it
                table.strokeRgba = it
                onTableModified()
                showBorderCustomPicker = false
            },
        )
    }

    if (showFillCustomPicker) {
        ColorPickerPopup(
            initial = fillRgba ?: Rgba(255, 255, 255, (fillAlpha * 255).roundToInt()),
            recents = fillPresets,
            onDismiss = { showFillCustomPicker = false },
            onPick = {
                val alphaInt = (fillAlpha * 255).roundToInt().coerceIn(1, 255)
                val colorWithAlpha = Rgba(it.r, it.g, it.b, alphaInt)
                fillRgba = colorWithAlpha
                table.fillRgba = colorWithAlpha
                onTableModified()
                showFillCustomPicker = false
            },
        )
    }
}

/**
 * Dialog for typing/editing text inside a selected table cell (Star Notes style).
 */
@Composable
fun TableCellTextDialog(
    row: Int,
    col: Int,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    val isDark = LocalPalette.current.isDark
    val bg = if (isDark) Color(0xFF1E1E24) else Color.White
    val textCol = if (isDark) Color.White else Color(0xFF18181B)
    val borderCol = if (isDark) Color(0xFF3F3F46) else Color(0xFFE4E4E7)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Edit Cell (${row + 1}, ${col + 1})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textCol,
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0x10000000))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(PnotesIcons.close, contentDescription = "Close", tint = textCol, modifier = Modifier.size(12.dp))
                }
            }

            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter cell text...", fontSize = 13.sp) },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) Color.White else Color(0xFF09090B),
                    unfocusedBorderColor = borderCol,
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF4F4F5))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textCol)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color.White else Color(0xFF09090B))
                        .clickable {
                            onConfirm(text)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Done", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF09090B) else Color.White)
                }
            }
        }
    }
}
