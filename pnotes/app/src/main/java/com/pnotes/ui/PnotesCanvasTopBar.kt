package com.pnotes.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.core.tools.Tool
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.NormalModeSolidColor
import com.pnotes.ui.theme.NormalModeDarkPill
import com.pnotes.ui.theme.NormalModePillBorder

/**
 * Modern monochrome top bar for Infinite Canvas matching the exact design language of PnotesTopBar.
 */
@Composable
fun PnotesCanvasTopBar(
    editor: InfiniteEditor,
    onOpenBackstage: () -> Unit,
    onInsertImage: () -> Unit,
    onShare: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    isHeaderShrunk: Boolean = false,
    onToggleHeaderShrunk: () -> Unit = onToggleFullscreen,
    onClosePane: (() -> Unit)? = null,
    isTabBarVisible: Boolean = true,
    onToggleTabBarVisible: () -> Unit = { },
    onNewTab: () -> Unit = {},
    onNewCanvasTab: () -> Unit = {},
    onImportPdf: () -> Unit = {},
    onOpenBrowseDrawer: () -> Unit = {},
    tabContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalPalette.current.isDark
    var renaming by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showStylesPopup by remember { mutableStateOf(false) }
    var showWaypointsPopup by remember { mutableStateOf(false) }

    if (isHeaderShrunk) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, end = 16.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        spotColor = if (isDark) Color(0x500F0F12) else Color(0x1F0F0F12),
                    )
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFFFF))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0x1F000000),
                    shape = CircleShape,
                )
                .bouncyClickable(onClick = onToggleHeaderShrunk),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PnotesIcons.fullscreen,
                contentDescription = "Expand Header",
                tint = if (isDark) Color.White else Color(0xFF18181B),
                modifier = Modifier.size(18.dp),
            )
        }
    }
    return
}

    val solidBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF7F7F9)
    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF18181B)
    val textBody = if (isDark) Color(0xFFF5F7FA) else Color(0xFF27272A)
    val textMuted = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
    val innerPillBg = if (isDark) Color(0xFF383838) else Color(0xFFECECEF)
    val innerPillBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0x1F000000)
    val barBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFFFF)
    val barBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x14000000)

    val topBarModifier = modifier
        .fillMaxWidth()
        .shadow(4.dp, RoundedCornerShape(0.dp), spotColor = if (isDark) Color(0x50000000) else Color(0x15000000))
        .background(barBg)

    Column(
        modifier = topBarModifier,
    ) {
        // 1. TABS BAR AT THE VERY TOP (when visible)
        if (isTabBarVisible) {
            tabContent?.invoke()
        }

        // 2. CANVAS HEADER TOOLS / NAVIGATION ROW (Exact unified 44dp height matching Toolbar and Tab Bar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
        // === 1. LEFT SECTION: Back + Canvas Icon + Editable Title + Saved Status ===
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(innerPillBg)
                    .border(1.2.dp, innerPillBorder, RoundedCornerShape(18.dp))
                    .bouncyClickable(onClick = onOpenBackstage),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.pillChevronLeft,
                    contentDescription = "Back",
                    tint = textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Canvas Icon & Title
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(innerPillBg)
                    .border(1.2.dp, innerPillBorder, RoundedCornerShape(18.dp))
                    .bouncyClickable(onClick = { renaming = true })
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = PnotesIcons.canvas,
                    contentDescription = "Canvas",
                    tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                    modifier = Modifier.size(16.dp),
                )

                Text(
                    text = editor.title.ifBlank { "Infinite Canvas" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp),
                )

                // Saved Status Dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                    )
                    Text(
                        text = "Saved",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textMuted,
                    )
                }
            }

            // Quick New Tab "+" Action Button in Second Bar (Shown ONLY when Tab Bar is hidden / single note)
            if (!isTabBarVisible) {
                var showNewTabPopup by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(innerPillBg)
                            .border(1.2.dp, innerPillBorder, RoundedCornerShape(18.dp))
                            .bouncyClickable { showNewTabPopup = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.plus,
                            contentDescription = "New Tab Options",
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = showNewTabPopup,
                        onDismissRequest = { showNewTabPopup = false },
                        shape = RoundedCornerShape(14.dp),
                        containerColor = if (isDark) Color(0xFF1E1E24) else Color(0xFFFFFFFF),
                        border = BorderStroke(0.5.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0x29000000)),
                        shadowElevation = 8.dp,
                        tonalElevation = 0.dp,
                    ) {
                        val textMenuPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
                        val textMenuMuted = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)

                        DropdownMenuItem(
                            text = { Text("Browse Existing Notes...", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) },
                            leadingIcon = { Icon(PnotesIcons.folder, null, tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7), modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showNewTabPopup = false
                                onOpenBrowseDrawer()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Note", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = textMenuPrimary) },
                            leadingIcon = { Icon(PnotesIcons.file, null, tint = textMenuMuted, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showNewTabPopup = false
                                onNewTab()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Infinite Canvas", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = textMenuPrimary) },
                            leadingIcon = { Icon(PnotesIcons.grid, null, tint = textMenuMuted, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showNewTabPopup = false
                                onNewCanvasTab()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import PDF Document", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = textMenuPrimary) },
                            leadingIcon = { Icon(PnotesIcons.copy, null, tint = textMenuMuted, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showNewTabPopup = false
                                onImportPdf()
                            }
                        )
                    }
                }
            }
        }

            // === 3. RIGHT SECTION: Undo/Redo + Zoom Pill + Lock + Share + Minimap + 3-Dots Menu ===
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Undo Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(innerPillBg)
                    .border(1.2.dp, innerPillBorder, RoundedCornerShape(18.dp))
                    .bouncyClickable(
                        enabled = editor.canUndo,
                        onClick = { editor.undo() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.undo,
                    contentDescription = "Undo",
                    tint = if (editor.canUndo) textPrimary else textMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp),
                )
            }

            // Redo Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(innerPillBg)
                    .border(1.2.dp, innerPillBorder, RoundedCornerShape(18.dp))
                    .bouncyClickable(
                        enabled = editor.canRedo,
                        onClick = { editor.redo() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.redo,
                    contentDescription = "Redo",
                    tint = if (editor.canRedo) textPrimary else textMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp),
                )
            }

            // Zoom Percentage Capsule (tap to reset / fit)
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(innerPillBg)
                    .border(1.2.dp, innerPillBorder, RoundedCornerShape(18.dp))
                    .bouncyClickable(onClick = { editor.zoomToFit() })
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${editor.zoomPercent}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )
                if (editor.zoomLocked) {
                    Icon(
                        imageVector = PnotesIcons.lock,
                        contentDescription = "Zoom Locked",
                        tint = textPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            // Lock Button (36x36 rd(18))
            val lockActive = editor.zoomLocked
            val lockBg = if (lockActive) (if (isDark) Color.White.copy(alpha = 0.18f) else Color(0x10000000)) else innerPillBg
            val lockBorder = if (lockActive) (if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF09090B)) else innerPillBorder
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(lockBg)
                    .border(1.2.dp, lockBorder, RoundedCornerShape(18.dp))
                    .bouncyClickable(onClick = { editor.toggleZoomLock() }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.lock,
                    contentDescription = "Lock Zoom",
                    tint = if (lockActive) (if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)) else textBody,
                    modifier = Modifier.size(16.dp),
                )
            }

            // Share Button (36x36 rd(18))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(innerPillBg)
                    .border(1.2.dp, innerPillBorder, RoundedCornerShape(18.dp))
                    .bouncyClickable(onClick = onShare),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.share,
                    contentDescription = "Share",
                    tint = textBody,
                    modifier = Modifier.size(16.dp),
                )
            }

            // Minimap Toggle Button
            val minimapActive = editor.minimapVisible
            val minimapBg = if (minimapActive) {
                if (isDark) Color.White.copy(alpha = 0.18f) else Color(0x10000000)
            } else innerPillBg
            val minimapBorder = if (minimapActive) {
                if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF09090B)
            } else innerPillBorder

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(minimapBg)
                    .border(1.2.dp, minimapBorder, RoundedCornerShape(18.dp))
                    .bouncyClickable(onClick = { editor.toggleMinimap() }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.contents,
                    contentDescription = "Toggle Minimap",
                    tint = if (minimapActive) (if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)) else textBody,
                    modifier = Modifier.size(17.dp),
                )
            }

                // 3-Dots More Options Menu Button
            Box {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(innerPillBg)
                        .border(1.2.dp, innerPillBorder, RoundedCornerShape(18.dp))
                        .bouncyClickable(onClick = {
                            showStylesPopup = false
                            showWaypointsPopup = false
                            showMoreMenu = true
                        }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.more,
                        contentDescription = "More Canvas Options",
                        tint = textBody,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Dropdown Menu positioned under the 3-dots button
                PnotesCanvasMoreMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    editor = editor,
                    onOpenStyles = {
                        showMoreMenu = false
                        showWaypointsPopup = false
                        showStylesPopup = true
                    },
                    onOpenWaypoints = {
                        showMoreMenu = false
                        showStylesPopup = false
                        showWaypointsPopup = true
                    },
                    onInsertImage = {
                        showMoreMenu = false
                        onInsertImage()
                    },
                    onToggleFullscreen = {
                        showMoreMenu = false
                        onToggleHeaderShrunk()
                    },
                )
            }

            onClosePane?.let { closeFn ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(innerPillBg)
                        .border(1.2.dp, innerPillBorder, RoundedCornerShape(18.dp))
                        .bouncyClickable(onClick = closeFn),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.close,
                        contentDescription = "Close Split Pane",
                        tint = textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        }

        // 3. Page-Facing Bottom Separation Rim Stroke
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.9.dp)
                .background(
                    if (isDark) Color(0xFF3F3F48) else Color(0xFFD4D4DC)
                )
        )
    }

    // Secondary Popups triggered from top bar
    if (renaming) {
        RenameDialog(
            initial = editor.title,
            onConfirm = { name ->
                renaming = false
                editor.rename(name)
            },
            onDismiss = { renaming = false },
        )
    }

    if (showStylesPopup) {
        CanvasStylesPopup(editor) { showStylesPopup = false }
    }

    if (showWaypointsPopup) {
        CanvasWaypointsPopup(editor) { showWaypointsPopup = false }
    }
}

/**
 * Modern monochrome popup menu for Canvas editor (3-dots).
 * Exposes all infinite canvas features (background, navigation, tools, AI wand, diagnostics).
 */
@Composable
fun PnotesCanvasMoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    editor: InfiniteEditor,
    onOpenStyles: () -> Unit,
    onOpenWaypoints: () -> Unit,
    onInsertImage: () -> Unit,
    onToggleFullscreen: () -> Unit,
    offset: DpOffset = DpOffset(0.dp, 8.dp),
) {
    val isDark = LocalPalette.current.isDark

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.Transparent,
        border = null,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .width(268.dp)
                .heightIn(max = 480.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) PnotesDarkCard else PnotesLightCard)
                .border(1.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(18.dp))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp, horizontal = 6.dp),
        ) {
            // Section 1: Canvas Background & Styles
            CanvasMenuSectionHeader(text = "CANVAS BACKGROUND & STYLES", isDark = isDark)

            CanvasMenuItemRow(
                icon = PnotesIcons.sliders,
                label = "Canvas Background & Styles",
                subtitle = "Grid, dots, lines, spacing, paper tint",
                isDark = isDark,
                onClick = {
                    onDismissRequest()
                    onOpenStyles()
                },
            )

            CanvasMenuDivider(isDark = isDark)

            // Section 2: View & Navigation
            CanvasMenuSectionHeader(text = "VIEW & NAVIGATION", isDark = isDark)

            CanvasMenuItemRow(
                icon = PnotesIcons.fullscreen,
                label = "Reset View (100%)",
                subtitle = "Reset zoom and center at origin",
                isDark = isDark,
                onClick = {
                    onDismissRequest()
                    editor.resetView()
                },
            )

            CanvasMenuItemRow(
                icon = PnotesIcons.cards,
                label = "Fit to Content",
                subtitle = "Frame all drawings on the canvas",
                isDark = isDark,
                onClick = {
                    onDismissRequest()
                    editor.zoomToFit()
                },
            )

            CanvasMenuItemRow(
                icon = PnotesIcons.zoomIn,
                label = "Zoom In (+25%)",
                subtitle = "Increase magnification level",
                isDark = isDark,
                onClick = {
                    onDismissRequest()
                    editor.zoomIn()
                },
            )

            CanvasMenuItemRow(
                icon = PnotesIcons.zoomOut,
                label = "Zoom Out (-25%)",
                subtitle = "Decrease magnification level",
                isDark = isDark,
                onClick = {
                    onDismissRequest()
                    editor.zoomOut()
                },
            )

            CanvasMenuItemRow(
                icon = if (editor.zoomLocked) PnotesIcons.lock else PnotesIcons.unlock,
                label = if (editor.zoomLocked) "Unlock Zoom" else "Lock Zoom",
                subtitle = if (editor.zoomLocked) "Allow gestures to scale canvas" else "Lock viewport scale firmly",
                isDark = isDark,
                badge = if (editor.zoomLocked) "Locked" else null,
                onClick = {
                    onDismissRequest()
                    editor.toggleZoomLock()
                },
            )

            CanvasMenuItemRow(
                icon = PnotesIcons.contents,
                label = "Bird's-Eye Minimap",
                subtitle = "Floating viewport navigator overlay",
                isDark = isDark,
                badge = if (editor.minimapVisible) "On" else "Off",
                onClick = {
                    onDismissRequest()
                    editor.toggleMinimap()
                },
            )

            CanvasMenuItemRow(
                icon = PnotesIcons.bookmark,
                label = "Waypoints & Camera Points",
                subtitle = "Save and jump to canvas locations",
                isDark = isDark,
                onClick = {
                    onDismissRequest()
                    onOpenWaypoints()
                },
            )

            CanvasMenuDivider(isDark = isDark)

            // Section 3: Canvas Tools & AI
            CanvasMenuSectionHeader(text = "CANVAS TOOLS & AI", isDark = isDark)

            CanvasMenuItemRow(
                icon = PnotesIcons.magicWand,
                label = "Magic Wand Mode",
                subtitle = "Smart stroke and region selection",
                isDark = isDark,
                badge = if (editor.wandEnabled) "Active" else null,
                onClick = {
                    onDismissRequest()
                    editor.toggleWand()
                },
            )

            CanvasMenuItemRow(
                icon = PnotesIcons.shape,
                label = "Auto Shape Recognition",
                subtitle = "Snap hand-drawn loops to geometric shapes",
                isDark = isDark,
                badge = if (editor.detectShapes) "On" else "Off",
                onClick = {
                    onDismissRequest()
                    editor.armDetectShapes(!editor.detectShapes)
                },
            )

            CanvasMenuItemRow(
                icon = PnotesIcons.image,
                label = "Insert Reference Image",
                subtitle = "Place photo or drawing onto canvas",
                isDark = isDark,
                onClick = {
                    onDismissRequest()
                    onInsertImage()
                },
            )

            CanvasMenuDivider(isDark = isDark)

            // Section 4: Canvas Actions & Diagnostics
            CanvasMenuSectionHeader(text = "CANVAS ACTIONS", isDark = isDark)

            CanvasMenuItemRow(
                icon = PnotesIcons.fullscreen,
                label = "Toggle Fullscreen",
                subtitle = "Expand canvas to entire tablet screen",
                isDark = isDark,
                onClick = {
                    onDismissRequest()
                    onToggleFullscreen()
                },
            )

            CanvasMenuItemRow(
                icon = PnotesIcons.info,
                label = "Diagnostics Overlay",
                subtitle = "Display viewport stats and frame rate",
                isDark = isDark,
                badge = if (editor.debugVisible) "On" else "Off",
                onClick = {
                    onDismissRequest()
                    editor.toggleDebug()
                },
            )

            CanvasMenuItemRow(
                icon = PnotesIcons.trash,
                label = "Clear Entire Canvas",
                subtitle = "Remove all strokes and items",
                isDestructive = true,
                isDark = isDark,
                onClick = {
                    onDismissRequest()
                    editor.clearCanvas()
                },
            )
        }
    }
}

@Composable
private fun CanvasMenuSectionHeader(text: String, isDark: Boolean) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = if (isDark) PnotesDarkText3 else PnotesLightText3,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun CanvasMenuDivider(isDark: Boolean) {
    Spacer(Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.6.dp)
            .background(if (isDark) PnotesDarkBorder else PnotesLightBorder),
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun CanvasMenuItemRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    isDark: Boolean,
    isDestructive: Boolean = false,
    badge: String? = null,
    onClick: () -> Unit,
) {
    val defaultIconColor = if (isDestructive) {
        PnotesDanger
    } else {
        if (isDark) PnotesDarkText1 else PnotesLightText1
    }

    val defaultTextColor = if (isDestructive) {
        PnotesDanger
    } else {
        if (isDark) PnotesDarkText1 else PnotesLightText1
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isDestructive) {
                        if (isDark) Color(0x33E81E21) else Color(0x18E81E21)
                    } else {
                        if (isDark) PnotesDarkElevated else PnotesLightSurface2
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = defaultIconColor,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = defaultTextColor,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (isDark) PnotesDarkText2 else PnotesLightText2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (badge != null) {
            val isActiveBadge = badge == "Active" || badge == "On" || badge == "Locked"
            val badgeBg = if (isActiveBadge) {
                if (isDark) Color(0x33007AFF) else Color(0x22007AFF)
            } else {
                if (isDark) PnotesDarkElevated else PnotesLightSurface2
            }
            val badgeFg = if (isActiveBadge) {
                PnotesPrimary
            } else {
                if (isDark) PnotesDarkText1 else PnotesLightText1
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .then(
                        if (isActiveBadge) {
                            Modifier.border(0.8.dp, PnotesPrimary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        } else Modifier
                    )
                    .padding(horizontal = 7.dp, vertical = 2.5.dp),
            ) {
                Text(
                    text = badge,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeFg,
                )
            }
        }
    }
}
