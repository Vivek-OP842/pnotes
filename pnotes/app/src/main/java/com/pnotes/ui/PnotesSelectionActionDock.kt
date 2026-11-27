package com.pnotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.core.model.Rgba
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.toComposeColor

/**
 * Floating glass action dock pinned at bottom center when one or more notes are selected.
 * Provides batch actions: Move, Duplicate, Star/Unstar, Color Tag, Export PDF, and Delete.
 */
@Composable
fun PnotesSelectionActionDock(
    visible: Boolean,
    selectedCount: Int,
    isAllStarred: Boolean,
    onMove: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleStar: () -> Unit,
    onColorTag: () -> Unit,
    onExportPdf: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalPalette.current.isDark

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
        modifier = modifier,
    ) {
        val bgBrush = Brush.verticalGradient(
            listOf(
                if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFFFF),
                if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFFFF),
            )
        )

        val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE1E6EF)
        val shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0x20000000)
        val shape = RoundedCornerShape(20.dp)

        Surface(
            modifier = Modifier
                .shadow(elevation = 16.dp, shape = shape, spotColor = shadowColor, ambientColor = shadowColor)
                .clip(shape)
                .border(1.dp, borderColor, shape),
            shape = shape,
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier
                    .background(bgBrush)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}, // Consumes touches inside dock so they don't dismiss selection
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Count Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$selectedCount selected",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(1.dp)
                            .height(26.dp)
                            .background(if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7))
                    )

                    // Action 1: Move to Folder
                    DockActionButton(
                        icon = PnotesIcons.folder,
                        label = "Move",
                        isDark = isDark,
                        enabled = selectedCount > 0,
                        onClick = onMove,
                    )

                    // Action 2: Duplicate
                    DockActionButton(
                        icon = PnotesIcons.duplicate,
                        label = "Duplicate",
                        isDark = isDark,
                        enabled = selectedCount > 0,
                        onClick = onDuplicate,
                    )

                    // Action 3: Star / Unstar
                    DockActionButton(
                        icon = if (isAllStarred) PnotesIcons.starFilled else PnotesIcons.star,
                        label = if (isAllStarred) "Unstar" else "Star",
                        isDark = isDark,
                        enabled = selectedCount > 0,
                        tint = if (isAllStarred) Color(0xFFF59E0B) else null,
                        onClick = onToggleStar,
                    )

                    // Action 4: Color Tag
                    DockActionButton(
                        icon = PnotesIcons.palette,
                        label = "Color",
                        isDark = isDark,
                        enabled = selectedCount > 0,
                        onClick = onColorTag,
                    )

                    // Action 5: Export PDF
                    DockActionButton(
                        icon = PnotesIcons.exportDoc,
                        label = "PDF",
                        isDark = isDark,
                        enabled = selectedCount > 0,
                        onClick = onExportPdf,
                    )

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(1.dp)
                            .height(26.dp)
                            .background(if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7))
                    )

                    // Action 6: Delete (Danger Action)
                    DockActionButton(
                        icon = PnotesIcons.trash,
                        label = "Delete",
                        isDark = isDark,
                        enabled = selectedCount > 0,
                        tint = Color(0xFFEF4444),
                        onClick = onDelete,
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    // Close button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 16.dp),
                                onClick = onDismiss,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.close,
                            contentDescription = "Close selection",
                            tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockActionButton(
    icon: ImageVector,
    label: String,
    isDark: Boolean,
    enabled: Boolean,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val defaultIconTint = if (enabled) {
        tint ?: (if (isDark) Color(0xFFF4F4F5) else Color(0xFF09090B))
    } else {
        if (isDark) Color(0xFF52525B) else Color(0xFFA1A1AA)
    }

    val defaultTextTint = if (enabled) {
        if (isDark) Color(0xFFD4D4D8) else Color(0xFF27272A)
    } else {
        if (isDark) Color(0xFF52525B) else Color(0xFFA1A1AA)
    }

    val itemShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .clip(itemShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = defaultIconTint,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = defaultTextTint,
                maxLines = 1,
            )
        }
    }
}

/**
 * Modal dialog displaying candidate folders for moving selected notes.
 */
@Composable
fun PnotesFolderPickerDialog(
    folders: List<BrowseEntry>,
    currentDocId: String,
    rootDocId: String,
    itemCount: Int,
    onSelectFolder: (targetDocId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val isDark = palette.isDark

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Move $itemCount ${if (itemCount == 1) "item" else "items"}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.text.toComposeColor(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select destination folder",
                    fontSize = 12.sp,
                    color = palette.textDim.toComposeColor(),
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Option: Root / Home
                item {
                    val isCurrent = currentDocId == rootDocId
                    FolderDestinationItem(
                        name = "Home / Root Folder",
                        isCurrent = isCurrent,
                        isDark = isDark,
                        onClick = { if (!isCurrent) onSelectFolder(rootDocId) },
                    )
                }

                // Subfolders
                items(folders, key = { it.documentUri }) { folder ->
                    val folderDocId = runCatching {
                        android.provider.DocumentsContract.getDocumentId(android.net.Uri.parse(folder.documentUri))
                    }.getOrNull() ?: folder.documentUri.substringAfterLast("%2F").substringAfterLast("/")
                    val isCurrent = folderDocId == currentDocId || folder.name == currentDocId

                    FolderDestinationItem(
                        name = folder.name,
                        isCurrent = isCurrent,
                        isDark = isDark,
                        onClick = { if (!isCurrent) onSelectFolder(folderDocId) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textDim.toComposeColor())
            }
        },
        containerColor = palette.menuBg.toComposeColor(),
    )
}

@Composable
private fun FolderDestinationItem(
    name: String,
    isCurrent: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val bgColor = if (isCurrent) {
        if (isDark) Color(0xFF27272A).copy(alpha = 0.5f) else Color(0xFFF4F4F5).copy(alpha = 0.5f)
    } else {
        if (isDark) Color(0xFF1E1E24) else Color(0xFFFAFAFA)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .border(
                1.dp,
                if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7),
                shape,
            )
            .clickable(enabled = !isCurrent, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = PnotesIcons.folder,
            contentDescription = null,
            tint = if (isCurrent) Color(0xFFA1A1AA) else Color(0xFF3B82F6),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isCurrent) Color(0xFF71717A) else (if (isDark) Color(0xFFF4F4F5) else Color(0xFF09090B)),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isCurrent) {
            Text(
                text = "Current",
                fontSize = 11.sp,
                color = Color(0xFFA1A1AA),
            )
        }
    }
}

/**
 * Modal dialog for assigning color category tags to selected notes.
 */
@Composable
fun PnotesBatchColorDialog(
    itemCount: Int,
    onSelectColor: (Rgba?) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val isDark = palette.isDark

    val swatches = remember {
        listOf(
            Pair("Coral", Rgba(239, 68, 68, 255)),
            Pair("Amber", Rgba(245, 158, 11, 255)),
            Pair("Yellow", Rgba(234, 179, 8, 255)),
            Pair("Emerald", Rgba(16, 185, 129, 255)),
            Pair("Sky", Rgba(14, 165, 233, 255)),
            Pair("Indigo", Rgba(99, 102, 241, 255)),
            Pair("Purple", Rgba(168, 85, 247, 255)),
            Pair("Rose", Rgba(244, 63, 94, 255)),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Color tag for $itemCount ${if (itemCount == 1) "item" else "items"}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text.toComposeColor(),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Pick a label color to categorize your selected notes:",
                    fontSize = 13.sp,
                    color = palette.textDim.toComposeColor(),
                )

                // Color Swatches Grid / Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Option to clear color
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isDark) Color(0xFF52525B) else Color(0xFFD4D4D8), CircleShape)
                            .clickable {
                                onSelectColor(null)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.close,
                            contentDescription = "Clear color",
                            tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                            modifier = Modifier.size(14.dp),
                        )
                    }

                    // Color dots
                    swatches.take(7).forEach { (_, rgba) ->
                        val composeColor = Color(rgba.r, rgba.g, rgba.b, rgba.a)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .shadow(2.dp, CircleShape)
                                .clip(CircleShape)
                                .background(composeColor)
                                .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .clickable {
                                    onSelectColor(rgba)
                                    onDismiss()
                                },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textDim.toComposeColor())
            }
        },
        containerColor = palette.menuBg.toComposeColor(),
    )
}
