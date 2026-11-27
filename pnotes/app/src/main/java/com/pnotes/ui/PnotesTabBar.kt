package com.pnotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.core.util.DocumentKind
import com.pnotes.tabs.NoteTab
import com.pnotes.tabs.TabManager
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.NormalModeSolidColor
import com.pnotes.ui.theme.NormalModeDarkPill
import com.pnotes.ui.theme.NormalModePillBorder

/**
 * Multi-tabs bar for PNotes.
 *
 * Sits seamlessly beneath the PnotesTopBar / PnotesCanvasTopBar with solid surfaces,
 * active indicator highlight, spring animations, dirty indicators, and quick tab actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PnotesTabBar(
    tabManager: TabManager,
    onNewTab: () -> Unit,
    onNewCanvasTab: () -> Unit = {},
    onImportPdf: () -> Unit = {},
    onOpenBrowseDrawer: () -> Unit = {},
    onCloseTab: (NoteTab) -> Unit,
    onCloseOtherTabs: (NoteTab) -> Unit = {},
    onDuplicateTab: (NoteTab) -> Unit = {},
    onOpenInSplit: ((NoteTab) -> Unit)? = null,
    onRenameTab: ((NoteTab) -> Unit)? = null,
    onHideTabBar: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = isGlassDark()
    val tabs = tabManager.tabs
    val activeTabId = tabManager.activeTabId
    val listState = rememberLazyListState()

    // Automatically scroll to active tab when it changes
    LaunchedEffect(activeTabId) {
        val activeIndex = tabs.indexOfFirst { it.id == activeTabId }
        if (activeIndex >= 0) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    var showCreatePopup by remember { mutableStateOf(false) }

    val solidBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF7F7F9)
    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF18181B)
    val textBody = if (isDark) Color(0xFFF5F7FA) else Color(0xFF27272A)
    val textMuted = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
    val innerPillBg = if (isDark) Color(0xFF383838) else Color(0xFFECECEF)
    val innerPillBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0x1F000000)
    val topBorder = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.08f)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
    }

    val barModifier = modifier
        .fillMaxWidth()
        .height(44.dp)
        .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFFFF))

    Row(
        modifier = barModifier.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Tab list scrolling container
        LazyRow(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(
                items = tabs,
                key = { _, tab -> tab.id }
            ) { index, tab ->
                val isActive = tab.id == activeTabId
                PnotesTabItem(
                    tab = tab,
                    isActive = isActive,
                    showCloseButton = tabs.size > 1,
                    onSelect = { tabManager.selectTab(tab.id) },
                    onClose = { onCloseTab(tab) },
                    onCloseOthers = { onCloseOtherTabs(tab) },
                    onDuplicate = { onDuplicateTab(tab) },
                    onOpenInSplit = onOpenInSplit?.let { { it(tab) } },
                    onRename = onRenameTab?.let { { it(tab) } },
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        )
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Quick New Tab "+" Action Button (Exact glass design language as TopBar buttons: 30x30 rd(15))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(innerPillBg)
                .border(1.2.dp, topBorder, RoundedCornerShape(15.dp))
                .bouncyClickable { showCreatePopup = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PnotesIcons.plus,
                contentDescription = "New Tab Options",
                tint = textBody,
                modifier = Modifier.size(15.dp),
            )

            DropdownMenu(
                expanded = showCreatePopup,
                onDismissRequest = { showCreatePopup = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = if (isDark) Color(0xFF1E1E24) else Color(0xFFFFFFFF),
                border = BorderStroke(0.5.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0x29000000)),
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
            ) {
                val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
                val textMuted = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)

                DropdownMenuItem(
                    text = { Text("Browse Existing Notes...", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) },
                    leadingIcon = { Icon(PnotesIcons.folder, null, tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7), modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showCreatePopup = false
                        onOpenBrowseDrawer()
                    }
                )
                DropdownMenuItem(
                    text = { Text("New Note", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = textPrimary) },
                    leadingIcon = { Icon(PnotesIcons.file, null, tint = textMuted, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showCreatePopup = false
                        onNewTab()
                    }
                )
                DropdownMenuItem(
                    text = { Text("New Infinite Canvas", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = textPrimary) },
                    leadingIcon = { Icon(PnotesIcons.grid, null, tint = textMuted, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showCreatePopup = false
                        onNewCanvasTab()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Import PDF Document", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = textPrimary) },
                    leadingIcon = { Icon(PnotesIcons.copy, null, tint = textMuted, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showCreatePopup = false
                        onImportPdf()
                    }
                )
            }
        }

        // Hide Tab Bar Action Button (near "+" icon)
        if (onHideTabBar != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(innerPillBg)
                    .border(1.2.dp, topBorder, RoundedCornerShape(15.dp))
                    .bouncyClickable { onHideTabBar() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.chevronUp,
                    contentDescription = "Hide Tab Bar",
                    tint = textBody,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/**
 * Single tab item capsule with solid active styling matching the top buttons design language.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PnotesTabItem(
    tab: NoteTab,
    isActive: Boolean,
    showCloseButton: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onCloseOthers: () -> Unit,
    onDuplicate: () -> Unit,
    onOpenInSplit: (() -> Unit)?,
    onRename: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isDark = isGlassDark()
    var contextMenuExpanded by remember { mutableStateOf(false) }

    val innerPillBg = if (isDark) Color(0xFF15151A) else Color(0xFFFFFFFF)
    val activePillBg = if (isDark) Color(0xFF15151A) else Color(0xFFFFFFFF)
    val inactivePillBg = if (isDark) Color(0xFF242424) else Color(0xFFECECEF)

    val topBorder = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.08f)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFFCBD5E1)))
    }
    val inactiveBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x1F000000)

    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF18181B)
    val textBody = if (isDark) Color(0xFFF5F7FA) else Color(0xFF27272A)
    val textMuted = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)

    val targetBg by animateColorAsState(
        targetValue = if (isActive) activePillBg else inactivePillBg,
        animationSpec = tween(180),
        label = "tabBg"
    )

    val elevation by animateDpAsState(
        targetValue = if (isActive) 3.dp else 0.dp,
        animationSpec = tween(180),
        label = "tabElevation"
    )

    Box(
        modifier = modifier
            .height(30.dp)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(15.dp),
                spotColor = if (isDark) Color(0x350F0F12) else Color(0x1F0F0F12),
            )
            .clip(RoundedCornerShape(15.dp))
            .background(targetBg)
            .then(
                if (isActive) Modifier.border(1.2.dp, topBorder, RoundedCornerShape(15.dp))
                else Modifier.border(1.dp, inactiveBorder, RoundedCornerShape(15.dp))
            )
            .combinedClickable(
                onClick = onSelect,
                onLongClick = { contextMenuExpanded = true }
            )
            .padding(start = 10.dp, end = if (showCloseButton) 4.dp else 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Document Type Icon
            val docIcon = when (tab.kind) {
                DocumentKind.CANVAS -> PnotesIcons.grid
                else -> PnotesIcons.file
            }
            Icon(
                imageVector = docIcon,
                contentDescription = null,
                tint = if (isActive) textPrimary else textMuted,
                modifier = Modifier.size(13.dp),
            )

            // Tab Title
            Text(
                text = tab.title.ifBlank { "Untitled Note" },
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isActive) (if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)) else textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 150.dp),
            )

            // Dirty indicator dot
            if (tab.isDirty) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                )
            }

            // Close Tab Button ("×" Cut button)
            if (showCloseButton) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .bouncyClickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.close,
                        contentDescription = "Close Tab",
                        tint = if (isActive) textPrimary.copy(alpha = 0.8f) else textMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        // Long Press Context Menu
        DropdownMenu(
            expanded = contextMenuExpanded,
            onDismissRequest = { contextMenuExpanded = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = if (isDark) Color(0xFF1E1E24) else Color(0xFFFFFFFF),
            border = BorderStroke(0.5.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0x29000000)),
            shadowElevation = 8.dp,
            tonalElevation = 0.dp,
        ) {
            onRename?.let { renameFn ->
                DropdownMenuItem(
                    text = { Text("Rename Note", fontSize = 13.sp, color = textPrimary) },
                    leadingIcon = { Icon(PnotesIcons.file, null, tint = textMuted, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        contextMenuExpanded = false
                        renameFn()
                    }
                )
            }
            onDuplicate.let { dupFn ->
                DropdownMenuItem(
                    text = { Text("Duplicate Tab", fontSize = 13.sp, color = textPrimary) },
                    leadingIcon = { Icon(PnotesIcons.copy, null, tint = textMuted, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        contextMenuExpanded = false
                        dupFn()
                    }
                )
            }
            onOpenInSplit?.let { splitFn ->
                DropdownMenuItem(
                    text = { Text("Open in Split View", fontSize = 13.sp, color = textPrimary) },
                    leadingIcon = { Icon(PnotesIcons.split, null, tint = textMuted, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        contextMenuExpanded = false
                        splitFn()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Close Other Tabs", fontSize = 13.sp, color = textPrimary) },
                leadingIcon = { Icon(PnotesIcons.close, null, tint = textMuted, modifier = Modifier.size(16.dp)) },
                onClick = {
                    contextMenuExpanded = false
                    onCloseOthers()
                }
            )
            DropdownMenuItem(
                text = { Text("Close Tab", fontSize = 13.sp, color = Color(0xFFEF4444)) },
                leadingIcon = { Icon(PnotesIcons.close, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) },
                onClick = {
                    contextMenuExpanded = false
                    onClose()
                }
            )
        }
    }
}
