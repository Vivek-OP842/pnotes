package com.pnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.toComposeColor

/**
 * Saved views. An infinite canvas has no pages to navigate by, so a named viewport is what stands
 * in for a bookmark: save where you are, jump back to it later.
 */
@Composable
fun CanvasWaypointsPopup(editor: InfiniteEditor, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    var name by remember { mutableStateOf("") }

    val isDark = isGlassDark()

    com.pnotes.ui.DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            Modifier
                .width(280.dp)
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            PopupTitle("WAYPOINTS")

            if (editor.waypoints.isEmpty()) {
                Text(
                    "No saved views yet.",
                    color = palette.textDim.toComposeColor(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            for (waypoint in editor.waypoints) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        waypoint.name,
                        color = palette.text.toComposeColor(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { editor.jumpTo(waypoint); onDismiss() }
                            .padding(vertical = 6.dp),
                    )
                    Text(
                        "${Math.round(waypoint.zoom * 100)}%",
                        color = palette.textDim.toComposeColor(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                    IconButton(onClick = { editor.removeWaypoint(waypoint) }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            PnotesIcons.close,
                            contentDescription = "Remove",
                            tint = palette.textDim.toComposeColor(),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.size(8.dp))
            StyleCaption("SAVE THIS VIEW")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                ModeChip("Save", selected = false) {
                    if (name.isNotBlank()) {
                        editor.saveWaypoint(name)
                        name = ""
                    }
                }
            }

            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeChip("Minimap", editor.minimapVisible) { editor.toggleMinimap() }
                ModeChip("Fit all", selected = false) { editor.zoomToFit(); onDismiss() }
            }
        }
    }
}
