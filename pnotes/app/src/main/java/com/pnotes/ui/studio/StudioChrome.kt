package com.pnotes.ui.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pnotes.R

/** What the library rows in the sidebar switch between, matching the final workspace. */
enum class StudioLibrary { ALL, NOTES, CANVAS, FAVORITES, PDFS, SAVED }

/** How much room is actually in use, for the sidebar's storage card. */
data class StudioStorage(val usedBytes: Long, val totalBytes: Long, val where: String) {
    /** The "1.8 GB / 5 GB" line, from the real numbers. */
    val label: String get() = "${formatBytes(usedBytes)} / ${formatBytes(totalBytes)}"
    val fraction: Float get() = if (totalBytes <= 0L) 0f else (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
}

internal fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (value >= 100 || unit == 0) "${value.toInt()} ${units[unit]}" else "%.1f %s".format(value, units[unit])
}

/**
 * The studio header matching the Figcraft prototype: 600x48dp search field with Ctrl K badge,
 * 84x44dp theme toggle with sliding thumb, notification bell with unread indicator, and profile button.
 */
@Composable
internal fun StudioTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    isGrid: Boolean,
    onToggleGrid: () -> Unit,
    storageLabel: String,
    onStorageClick: () -> Unit,
    onAccountClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onBrandClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSidebar: (() -> Unit)? = null,
) {
    val t = studioTokens()
    val compact = onOpenSidebar != null

    Box(
        modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(t.topBar),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 16.dp else 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onOpenSidebar != null) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(t.searchField)
                        .border(1.dp, t.searchBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenSidebar),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fig_list_nav),
                        contentDescription = "Open sidebar",
                        tint = t.textPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
            }

            if (!compact) {
                Spacer(Modifier.weight(1f))
            }

            // Search bar: 600x48dp in Figcraft prototype
            Box(
                Modifier
                    .then(if (compact) Modifier.weight(1f) else Modifier.width(600.dp))
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(t.searchField)
                    .border(1.dp, t.searchBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fig_search),
                        contentDescription = null,
                        tint = t.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "Search notes, tags, or notebooks...",
                                color = t.textSecondary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.W400,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = t.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.W400,
                            ),
                            cursorBrush = SolidColor(t.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {}),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onQueryChange("") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(StudioIcons.close, "Clear search", tint = t.textSecondary, modifier = Modifier.size(12.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (!compact) {
                        Box(
                            Modifier
                                .size(width = 64.dp, height = 28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(t.searchChip)
                                .border(1.dp, t.searchBorder, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Ctrl K",
                                color = t.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.W600,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Right Cluster: Theme toggle, Bell, Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Theme toggle: 84x44dp with sliding thumb
                val thumbOffset by animateDpAsState(
                    targetValue = if (t.isDark) 44.dp else 4.dp,
                    label = "themeThumb",
                )
                Box(
                    Modifier
                        .size(width = 84.dp, height = 44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(t.searchField)
                        .then(if (!t.isDark) Modifier.border(1.dp, t.searchBorder, RoundedCornerShape(22.dp)) else Modifier)
                        .clickable(onClick = onToggleTheme),
                ) {
                    Box(
                        Modifier
                            .offset(x = thumbOffset, y = 4.dp)
                            .size(36.dp)
                            .then(if (!t.isDark) Modifier.shadow(2.dp, RoundedCornerShape(18.dp)) else Modifier)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (t.isDark) t.primary else t.accentAmber),
                    )
                    Box(
                        Modifier
                            .offset(x = 4.dp, y = 4.dp)
                            .size(36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.fig_sun),
                            contentDescription = "Light mode",
                            tint = if (t.isDark) t.textSecondary else Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Box(
                        Modifier
                            .offset(x = 44.dp, y = 4.dp)
                            .size(36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.fig_moon),
                            contentDescription = "Dark mode",
                            tint = if (t.isDark) Color.White else t.textSecondary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }

                // Bell button: 40x40dp
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(t.searchField)
                        .then(if (!t.isDark) Modifier.border(1.dp, t.searchBorder, RoundedCornerShape(20.dp)) else Modifier)
                        .clickable(onClick = onStorageClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fig_bell),
                        contentDescription = "Notifications",
                        tint = t.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 7.dp, end = 10.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(t.accentAmber),
                    )
                }

                // Profile button: 40x40dp
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(t.searchField)
                        .then(if (!t.isDark) Modifier.border(1.dp, t.searchBorder, RoundedCornerShape(20.dp)) else Modifier)
                        .clickable(onClick = onAccountClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fig_user),
                        contentDescription = "Account profile",
                        tint = t.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * The permanent 312dp sidebar matching Figcraft: Pnotes logo & PRO badge,
 * 260x52dp "New Notebook" pill, four navigation rows (Library, Favorites, PDFs & Slides, Saved),
 * Folders section with circular add button, and 260x80dp storage status card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudioSidebar(
    onNewNote: () -> Unit,
    counts: Map<StudioLibrary, Int>,
    activeLibrary: StudioLibrary?,
    onLibrary: (StudioLibrary) -> Unit,
    folders: List<StudioFolder>,
    allFoldersSelected: Boolean,
    onAllFolders: () -> Unit,
    selectedFolderDocId: String?,
    selectedSubFolderDocId: String?,
    expandedFolderIds: Set<String>,
    subFoldersMap: Map<String, List<StudioFolder>>,
    onOpenFolder: (StudioFolder) -> Unit,
    onToggleFolder: (StudioFolder) -> Unit,
    onNewFolder: () -> Unit,
    onRenameFolder: (StudioFolder) -> Unit,
    onDeleteFolder: (StudioFolder) -> Unit,
    onOpenSubFolder: (StudioFolder) -> Unit,
    smartTags: List<StudioQuickFilter>,
    activeFilter: StudioFilter,
    onSmartTag: (StudioQuickFilter) -> Unit,
    storage: StudioStorage,
    onStorageClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddSubfolder: ((StudioFolder) -> Unit)? = null,
) {
    val t = studioTokens()
    Box(
        modifier
            .width(312.dp)
            .fillMaxHeight()
            .background(t.sidebarBrush),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp, vertical = 24.dp),
        ) {
            // Top: Pnotes Logo (36x36dp) + "Pnotes" (24sp W700 with grid pattern) + "PRO" badge (48x22dp)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.fig_logo),
                    contentDescription = "Pnotes Logo",
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.width(10.dp))
                PnotesWordmark(isDark = t.isDark)
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(width = 48.dp, height = 22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (t.isDark) Color(0xFF102844) else t.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "PRO",
                        color = Color(0xFF0080FF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.W700,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Navigation items: Library, Favorites, PDFs & Slides, Saved (each 260x42dp)
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                SidebarNavRow(
                    iconRes = R.drawable.fig_nav_grid,
                    label = "Library",
                    count = counts[StudioLibrary.ALL] ?: 0,
                    selected = activeLibrary == StudioLibrary.ALL && selectedFolderDocId == null,
                    onClick = { onAllFolders(); onLibrary(StudioLibrary.ALL) },
                )
                SidebarNavRow(
                    iconRes = R.drawable.fig_star,
                    label = "Favorites",
                    count = counts[StudioLibrary.FAVORITES] ?: 0,
                    selected = activeLibrary == StudioLibrary.FAVORITES,
                    iconTint = Color(0xFFFFB200),
                    onClick = { onLibrary(StudioLibrary.FAVORITES) },
                )
                SidebarNavRow(
                    iconRes = R.drawable.fig_pdf,
                    label = "PDFs & Slides",
                    count = counts[StudioLibrary.PDFS] ?: 0,
                    selected = activeLibrary == StudioLibrary.PDFS,
                    iconTint = Color(0xFFE81E21),
                    onClick = { onLibrary(StudioLibrary.PDFS) },
                )
                SidebarNavRow(
                    iconRes = R.drawable.fig_bookmark,
                    label = "Saved",
                    count = counts[StudioLibrary.SAVED] ?: 0,
                    selected = activeLibrary == StudioLibrary.SAVED,
                    onClick = { onLibrary(StudioLibrary.SAVED) },
                )
            }

            Spacer(Modifier.height(20.dp))
            SidebarDivider()
            Spacer(Modifier.height(20.dp))

            // "Folders" section header: 16sp W600 + circular plus button (24x24dp)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Folders",
                    color = t.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(t.searchChip)
                        .clickable(onClick = onNewFolder),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fig_plus),
                        contentDescription = "New folder",
                        tint = t.textPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Scrollable Folders list
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                folders.forEach { folder ->
                    val isExpanded = expandedFolderIds.contains(folder.docId)
                    val subs = subFoldersMap[folder.docId].orEmpty()
                    SidebarFolderRow(
                        folder = folder,
                        selected = folder.docId == selectedFolderDocId && selectedSubFolderDocId == null,
                        expanded = isExpanded,
                        onClick = { onOpenFolder(folder) },
                        onToggleExpand = { onToggleFolder(folder) },
                        onRename = { onRenameFolder(folder) },
                        onDelete = { onDeleteFolder(folder) },
                        onAddSubfolder = { onAddSubfolder?.invoke(folder) },
                    )
                    if (isExpanded && subs.isNotEmpty()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            subs.forEach { sub ->
                                SidebarSubFolderRow(
                                    folder = sub,
                                    selected = sub.docId == selectedSubFolderDocId,
                                    onClick = { onOpenSubFolder(sub) },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Storage card at bottom: 260x80dp, rounded 16dp
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(t.sidebarCard)
                    .border(1.dp, t.sidebarCardBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onStorageClick)
                    .padding(16.dp),
            ) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.fig_cloud),
                            contentDescription = null,
                            tint = t.textPrimary,
                            modifier = Modifier.size(20.dp, 16.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Storage",
                            color = t.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W600,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            storage.label,
                            color = t.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W400,
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(t.syncTrack),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(storage.fraction.coerceIn(0.05f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(t.primary),
                        )
                    }
                }
            }
        }
    }
}

/** A sidebar chip that narrows the library to a paper style (the reference's smart tags). */
data class StudioQuickFilter(val label: String, val color: Color, val filter: StudioFilter)

@Composable
private fun SidebarDivider() {
    val t = studioTokens()
    Box(Modifier.fillMaxWidth().height(1.dp).background(t.sidebarDivider))
}

/**
 * Wordmark rendering "Pnotes" with a subtle graph-paper blueprint grid pattern inside the glyphs,
 * matching Figcraft / Apple design specifications exactly.
 */
@Composable
internal fun PnotesWordmark(
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val gridBrush = remember(isDark, density) {
        val tileSize = with(density) { 5.dp.roundToPx() }.coerceAtLeast(3)
        val bitmap = android.graphics.Bitmap.createBitmap(tileSize, tileSize, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val bgPaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#0F172A")
        }
        val linePaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }
        canvas.drawRect(0f, 0f, tileSize.toFloat(), tileSize.toFloat(), bgPaint)
        canvas.drawLine(0f, (tileSize - 1).toFloat(), tileSize.toFloat(), (tileSize - 1).toFloat(), linePaint)
        canvas.drawLine((tileSize - 1).toFloat(), 0f, (tileSize - 1).toFloat(), tileSize.toFloat(), linePaint)
        ShaderBrush(android.graphics.BitmapShader(bitmap, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT))
    }

    Text(
        text = "Pnotes",
        style = TextStyle(
            brush = gridBrush,
            fontSize = 24.sp,
            fontWeight = FontWeight.W700,
            letterSpacing = (-0.5).sp,
        ),
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun SidebarNavRow(
    iconRes: Int,
    label: String,
    count: Int,
    selected: Boolean,
    iconTint: Color? = null,
    onClick: () -> Unit,
) {
    val t = studioTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "navScale",
    )
    val bgAnim by animateColorAsState(
        targetValue = if (selected) t.primary else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "navBg",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(21.dp))
            .background(bgAnim)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (selected) Color.White else (iconTint ?: t.textSecondary),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                label,
                color = if (selected) Color.White else t.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Box(
                    Modifier
                        .height(20.dp)
                        .widthIn(min = 22.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        count.toString(),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.W600,
                        color = Color.White,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            } else if (count > 0) {
                Text(
                    count.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    color = t.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SidebarCount(text: String, active: Boolean) {
    val t = studioTokens()
    Box(
        Modifier
            .height(20.dp)
            .widthIn(min = 22.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) (if (t.isDark) Color(0xFF102844) else t.primary.copy(alpha = 0.18f)) else Color.Transparent)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (active) (if (t.isDark) Color(0xFF0080FF) else t.primary) else Color(0xFF64748B),
            fontSize = 11.5.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun SidebarAllFoldersRow(
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    val t = studioTokens()
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) t.selectedRow else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.fig_folder),
            contentDescription = null,
            tint = t.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "All Folders",
            color = if (selected) (if (t.isDark) Color.White else t.primary) else t.textPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.W600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        SidebarCount(text = count.toString(), active = selected)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SidebarFolderRow(
    folder: StudioFolder,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    onToggleExpand: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddSubfolder: (() -> Unit)? = null,
) {
    val t = studioTokens()
    val menuOffset = with(androidx.compose.ui.platform.LocalDensity.current) { 26.dp.roundToPx() }
    var menuOpen by remember(folder.docId) { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) t.selectedRow else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuOpen = true },
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.fig_folder),
            contentDescription = null,
            tint = t.primary,
            modifier = Modifier.size(width = 18.dp, height = 16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            folder.name,
            color = t.textPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.W600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            folder.notes.toString(),
            color = t.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.W400,
            maxLines = 1,
        )
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { menuOpen = !menuOpen },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.fig_dots_vertical),
                contentDescription = "Folder menu",
                tint = t.textSecondary.copy(alpha = 0.65f),
                modifier = Modifier.size(14.dp),
            )
        }
        if (menuOpen) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, menuOffset),
                onDismissRequest = { menuOpen = false },
                properties = PopupProperties(focusable = true),
            ) {
                StudioMenuSurface(minWidth = 170.dp) {
                    if (onAddSubfolder != null) {
                        StudioMenuItem(
                            label = "Add Subfolder",
                            icon = StudioIcons.folderPlus,
                            onClick = { menuOpen = false; onAddSubfolder() },
                        )
                    }
                    StudioMenuItem(
                        label = "Rename",
                        icon = StudioIcons.pencilWrite,
                        onClick = { menuOpen = false; onRename() },
                    )
                    StudioMenuDivider()
                    StudioMenuItem(
                        label = "Delete",
                        icon = StudioIcons.trash,
                        danger = true,
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarSubFolderRow(
    folder: StudioFolder,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val t = studioTokens()
    Row(
        Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) {
                    if (t.isDark) Color(0x1F0EA5E9) else Color(0xFFE0F2FE)
                } else {
                    Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            StudioIcons.folder,
            null,
            tint = t.primary,
            modifier = Modifier.size(width = 13.dp, height = 11.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            folder.name,
            color = when {
                selected -> t.primary
                t.isDark -> Color(0xFF94A3B8)
                else -> Color(0xFF475569)
            },
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            folder.notes.toString(),
            color = if (selected) t.primary else Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * The 350dp account popover opened from the settings and profile buttons: the real signed-in
 * identity, the real device storage, the entitlement state, and rows wired to the real theme and
 * Preferences surfaces.
 */
@Composable
internal fun StudioUserPopoverCard(
    displayName: String,
    email: String,
    avatarLabel: String,
    signedIn: Boolean,
    premium: Boolean,
    storage: StudioStorage,
    notesCount: Int,
    foldersCount: Int,
    onClose: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenPreferences: () -> Unit,
    onSync: () -> Unit,
    onUnavailable: (String) -> Unit,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = studioTokens()
    Column(
        modifier
            .width(350.dp)
            .shadow(24.dp, RoundedCornerShape(16.dp), clip = false, spotColor = Color.Black.copy(alpha = 0.7f))
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (t.isDark) {
                    Brush.verticalGradient(listOf(Color(0xFF131317), Color(0xFF0D0D10)))
                } else {
                    Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFC)))
                },
            )
            .border(1.dp, if (t.isDark) Color(0xFF23232C) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Identity + entitlement
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (t.isDark) {
                                    Brush.linearGradient(listOf(Color(0xFF2E2E3A), Color(0xFF161620)))
                                } else {
                                    Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(avatarLabel, color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                            .border(1.dp, if (t.isDark) Color(0xFF131317) else Color.White, CircleShape),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        displayName,
                        color = t.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        email,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (premium) {
                            Brush.linearGradient(listOf(Color(0xFF2D220E), Color(0xFF1B1406)))
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    if (t.isDark) Color(0xFF1C1C26) else Color(0xFFF1F5F9),
                                    if (t.isDark) Color(0xFF14141B) else Color(0xFFE2E8F0),
                                ),
                            )
                        },
                    )
                    .border(
                        0.5.dp,
                        if (premium) Color(0xFFD97706) else if (t.isDark) Color(0xFF30303E) else Color(0xFFCBD5E1),
                        RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    StudioIcons.sparkle,
                    null,
                    tint = if (premium) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                    modifier = Modifier.size(10.dp),
                )
                Text(
                    if (premium) "LIFETIME" else "LOCAL",
                    color = if (premium) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                )
            }
        }

        // Real storage + sync state
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (t.isDark) Color(0xFF181822) else Color(0xFFF1F5F9))
                .border(0.5.dp, if (t.isDark) Color(0xFF262634) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                Spacer(Modifier.width(7.dp))
                Text(
                    "Stored on this device · just now",
                    color = if (t.isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StudioMiniIconButton(
                    icon = StudioIcons.refresh,
                    contentDescription = "Rescan library",
                    onClick = onSync,
                    tint = Color(0xFF94A3B8),
                    bg = if (t.isDark) Color(0xFF20202C) else Color.White,
                    size = 20.dp,
                    iconSize = 11.dp,
                    radius = 5.dp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (t.isDark) Color(0xFF23232C) else Color(0xFFE2E8F0)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(storage.fraction.coerceAtLeast(0.08f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9)))),
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$notesCount notebooks • $foldersCount folders (${formatBytes(storage.usedBytes)} used)",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${(storage.fraction * 100).toInt()}%",
                        color = t.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }

        // Entitlement banner
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (t.isDark) {
                        Brush.linearGradient(listOf(Color(0xFF1A1A28), Color(0xFF11111A)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7)))
                    },
                )
                .border(
                    1.dp,
                    if (t.isDark) Color(0xFF2A2A3A) else Color(0xFFBBF7D0),
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(StudioIcons.sparkle, null, tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (premium) "PNotes Lifetime Suite" else "PNotes Local Suite",
                        color = if (t.isDark) Color(0xFFF8FAFC) else Color(0xFF14532D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (premium) {
                                Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF475569)))
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        if (premium) "Active" else "Device",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
            Text(
                if (premium) {
                    "Lifetime access on this account — unlimited notes and low-latency inking."
                } else {
                    "Notes are stored locally and stay fully usable without an account."
                },
                color = if (t.isDark) Color(0xFF94A3B8) else Color(0xFF166534),
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        PopoverDivider()

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PopoverRow(
                icon = StudioIcons.pencilOutline,
                label = "Stylus & Inking Studio",
                trailing = "Pressure, Palm",
                onClick = onOpenPreferences,
            )
            PopoverRow(
                icon = StudioIcons.gridSquares,
                label = "Default Paper & Templates",
                trailing = "Paper, Theme",
                onClick = onOpenPreferences,
            )
            PopoverRow(
                icon = if (t.isDark) StudioIcons.sun else StudioIcons.moon,
                label = "App Settings & Theme",
                trailing = if (t.isDark) "Dark Mode" else "Light Mode",
                trailingColor = t.primary,
                onClick = onToggleTheme,
            )
            PopoverRow(
                icon = StudioIcons.download,
                label = "Export Full Vault",
                trailing = "↓",
                trailingColor = t.primary,
                onClick = {
                    onUnavailable("This build exports notes individually — open a note's ⋯ menu to export it.")
                    onClose()
                },
            )
        }

        PopoverDivider()

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (signedIn) (if (t.isDark) Color(0xFF1F1316) else Color(0xFFFFF1F2)) else t.card)
                .border(
                    0.5.dp,
                    if (signedIn) (if (t.isDark) Color(0xFF3B181E) else Color(0xFFFECDD3)) else t.cardBorder,
                    RoundedCornerShape(8.dp),
                )
                .clickable {
                    if (signedIn) onSignOut() else onSignIn()
                    onClose()
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (signedIn) StudioIcons.logout else StudioIcons.user,
                null,
                tint = if (signedIn) Color(0xFFF87171) else t.textSecondary,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (signedIn) "Sign Out" else "Sign In",
                color = if (signedIn) Color(0xFFF87171) else t.textSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }

        Text(
            "PNotes Studio · On-device inking",
            color = Color(0xFF475569),
            fontSize = 9.5.sp,
            letterSpacing = 0.3.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PopoverDivider() {
    val t = studioTokens()
    Box(Modifier.fillMaxWidth().height(1.dp).background(if (t.isDark) Color(0xFF20202A) else Color(0xFFE2E8F0)))
}

@Composable
private fun PopoverRow(
    icon: ImageVector,
    label: String,
    trailing: String,
    onClick: () -> Unit,
    trailingColor: Color = Color(0xFF64748B),
) {
    val t = studioTokens()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (t.isDark) Color(0xFF1C1C26) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(9.dp))
        Text(
            label,
            color = if (t.isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(trailing, color = trailingColor, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Text("›", color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 1)
    }
}
