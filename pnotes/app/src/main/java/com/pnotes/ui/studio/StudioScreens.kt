package com.pnotes.ui.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pnotes.R

/**
 * The workspace's two content surfaces, measured against the final React frames:
 * [StudioOverviewScreen] is the multi-folder home (Artboards 1–3) and [StudioFolderScreen] is the
 * folder detail (Artboard 4). The host owns all state; these are presentational.
 */


/**
 * Everything a card can do, in one place, so both screens stay presentational while the host keeps
 * the real behaviour (open, select, star, move, rename, share, export, delete).
 */
internal class StudioNoteActions(
    val thumbnails: (StudioItem) -> ImageBitmap?,
    val onClick: (StudioItem) -> Unit,
    val onLongClick: (StudioItem) -> Unit,
    val onToggleStar: (StudioItem) -> Unit,
    val onTogglePin: (StudioItem) -> Unit = {},
    val onSelect: (StudioItem) -> Unit,
    val selectionMode: Boolean,
    val selection: Set<String>,
    val onMove: (StudioItem) -> Unit,
    val onRename: (StudioItem) -> Unit,
    val onShare: (StudioItem) -> Unit,
    val onExportPdf: (StudioItem) -> Unit,
    val onShowPath: (StudioItem) -> Unit,
    val onDelete: (StudioItem) -> Unit,
)

// ── Overview (home) ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudioOverviewScreen(
    query: String,
    title: String,
    items: List<StudioItem>,
    folders: List<StudioFolder>,
    notesInFolder: (StudioFolder) -> List<StudioItem>,
    activeFilter: StudioFilter,
    onClearFilter: () -> Unit,
    activeLibrary: StudioLibrary,
    onLibrary: (StudioLibrary) -> Unit,
    libraryCounts: Map<StudioLibrary, Int>,
    sort: StudioSort,
    onSort: (StudioSort) -> Unit,
    isGrid: Boolean,
    onViewMode: (Boolean) -> Unit,
    selectionMode: Boolean,
    actions: StudioNoteActions,
    onSelectClick: () -> Unit,
    onImportPdf: () -> Unit,
    onNewNotebook: (folderDocId: String?, styleId: String?) -> Unit,
    onNewFolder: () -> Unit,
    onOpenFolder: (StudioFolder) -> Unit,
    onRenameFolder: (StudioFolder) -> Unit,
    onDeleteFolder: (StudioFolder) -> Unit,
    modifier: Modifier = Modifier,
    onAddSubfolder: ((StudioFolder) -> Unit)? = null,
    recentNotes: List<StudioItem> = emptyList(),
) {
    val t = studioTokens()
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val hPad = if (compact) 16.dp else 32.dp
    val lastTouched = items.maxOfOrNull { it.modified } ?: 0L
    val searching = query.trim().isNotEmpty()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = hPad, end = hPad, top = 20.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // Heading row: "All Handwritten Notes" (30sp W700) + Subtitle + Action buttons
        StudioScreenHeading(
            title = title,
            meta = metaLine(
                "${items.size} ${if (items.size == 1) "notebook" else "notebooks"}",
                if (lastTouched > 0) "Last saved ${relativeLong(lastTouched)}" else null,
            ),
            compact = compact,
        ) {
            StudioHeaderActions(
                selectionMode = selectionMode,
                onSelectClick = onSelectClick,
                onImportPdf = onImportPdf,
                onNewNotebook = null,
            )
        }

        // Filter tabs row with Sort dropdown and View switch
        StudioLibraryTabsRow(
            activeLibrary = activeLibrary,
            onLibrary = onLibrary,
            counts = libraryCounts,
            sort = sort,
            onSort = onSort,
            isGrid = isGrid,
            onViewMode = onViewMode,
        )

        if (searching) {
            Text(
                "Search Results for \"${query.trim()}\" (${items.size})",
                color = t.textSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (items.isEmpty()) {
                StudioSearchEmpty(query.trim())
            } else {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items.forEach { item ->
                        StudioNotebookCardFor(item, actions)
                    }
                }
            }
            return@Column
        }

        if (activeLibrary == StudioLibrary.ALL) {
            // 1. Notebook cards row directly under filter tabs (Figcraft Artboard 1/2)
            val cardsToShow = if (recentNotes.isNotEmpty()) recentNotes else items
            if (isGrid && cardsToShow.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    cardsToShow.forEach { note ->
                        StudioNotebookCardFor(note, actions)
                    }
                }
            }

            // 2. Notebook Folders Section (Figcraft Artboard 1/2: Notebook Folders header + 55dp cards)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Notebook Folders",
                        color = t.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W600,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${folders.size} folders",
                        color = t.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W400,
                    )
                }

                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    val folderCardModifier = if (compact) Modifier.fillMaxWidth() else Modifier.wrapContentWidth()
                    folders.forEachIndexed { index, folder ->
                        StudioFolderCard(
                            folder = folder,
                            accent = t.folderAccents[index % t.folderAccents.size],
                            onClick = { onOpenFolder(folder) },
                            modifier = folderCardModifier,
                            onRename = { onRenameFolder(folder) },
                            onDelete = { onDeleteFolder(folder) },
                            onAddSubfolder = { onAddSubfolder?.invoke(folder) },
                        )
                    }
                    StudioNewFolderCard(
                        onClick = onNewFolder,
                        modifier = folderCardModifier,
                    )
                }
            }

            // 3. Additional notes if there are more notes than the recents row
            val remainingNotes = remember(items, cardsToShow) {
                val shownUris = cardsToShow.map { it.uri }.toSet()
                items.filter { !shownUris.contains(it.uri) }
            }
            if (!isGrid) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items.forEach { note ->
                        StudioNoteRow(
                            item = note,
                            thumbnail = actions.thumbnails(note),
                            selected = actions.selection.contains(note.uri),
                            selectionMode = actions.selectionMode,
                            onClick = { actions.onClick(note) },
                            onLongClick = { actions.onLongClick(note) },
                            onSelect = { actions.onSelect(note) },
                            onToggleStar = { actions.onToggleStar(note) },
                            onDelete = { actions.onDelete(note) },
                            onMove = { actions.onMove(note) },
                            onRename = { actions.onRename(note) },
                            onTogglePin = { actions.onTogglePin(note) },
                            onExportPdf = { actions.onExportPdf(note) },
                            onShowPath = { actions.onShowPath(note) },
                        )
                    }
                }
            } else if (remainingNotes.isNotEmpty()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "All Notes",
                            color = t.textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.W600,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${remainingNotes.size} more notes",
                            color = t.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W400,
                        )
                    }
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        remainingNotes.forEach { note ->
                            StudioNotebookCardFor(note, actions)
                        }
                    }
                }
            }
        } else {
            // Filtered view (Notes, Canvas, Favorites, PDFs, Saved)
            if (isGrid) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items.forEach { note ->
                        StudioNotebookCardFor(note, actions)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items.forEach { note ->
                        StudioNoteRow(
                            item = note,
                            thumbnail = actions.thumbnails(note),
                            selected = actions.selection.contains(note.uri),
                            selectionMode = actions.selectionMode,
                            onClick = { actions.onClick(note) },
                            onLongClick = { actions.onLongClick(note) },
                            onSelect = { actions.onSelect(note) },
                            onToggleStar = { actions.onToggleStar(note) },
                            onDelete = { actions.onDelete(note) },
                            onMove = { actions.onMove(note) },
                            onRename = { actions.onRename(note) },
                            onTogglePin = { actions.onTogglePin(note) },
                            onExportPdf = { actions.onExportPdf(note) },
                            onShowPath = { actions.onShowPath(note) },
                        )
                    }
                }
            }
        }
    }
}

// ── Folder detail ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudioFolderScreen(
    title: String,
    badge: String,
    backLabel: String,
    items: List<StudioItem>,
    totalCount: Int,
    pages: Int?,
    lastTouched: Long,
    subFolders: List<StudioFolder>,
    onNewSubfolder: () -> Unit,
    activeLibrary: StudioLibrary,
    onLibrary: (StudioLibrary) -> Unit,
    libraryCounts: Map<StudioLibrary, Int>,
    sort: StudioSort,
    onSort: (StudioSort) -> Unit,
    isGrid: Boolean,
    onViewMode: (Boolean) -> Unit,
    selectionMode: Boolean,
    actions: StudioNoteActions,
    onSelectClick: () -> Unit,
    onImportPdf: () -> Unit,
    onNewNotebook: (folderDocId: String?, styleId: String?) -> Unit,
    folderDocId: String,
    onBack: () -> Unit,
    onOpenSubFolder: (StudioFolder) -> Unit,
    onRenameFolder: ((StudioFolder) -> Unit)? = null,
    onDeleteFolder: ((StudioFolder) -> Unit)? = null,
    onAddSubfolder: ((StudioFolder) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val t = studioTokens()
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val hPad = if (compact) 16.dp else 32.dp

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = hPad, end = hPad, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Breadcrumb: back chip, slash, current folder.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(t.card)
                    .border(1.dp, t.cardBorder, RoundedCornerShape(6.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(StudioIcons.arrowLeft, null, tint = t.primary, modifier = Modifier.size(width = 12.dp, height = 10.dp))
                Text(backLabel, color = t.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            Text("/", color = Color(0xFF64748B), fontSize = 13.sp, maxLines = 1)
            Text(
                title,
                color = t.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Title, badge, real counts, and the header actions.
        StudioScreenHeading(
            title = title,
            badge = badge,
            meta = metaLine(
                "$totalCount ${if (totalCount == 1) "notebook" else "notebooks"}",
                pages?.let { "$it pages" },
                "Last updated ${relativeLong(lastTouched)}",
            ),
            compact = compact,
        ) {
            StudioHeaderActions(
                selectionMode = selectionMode,
                onSelectClick = onSelectClick,
                onImportPdf = onImportPdf,
                onNewNotebook = { onNewNotebook(folderDocId, null) },
            )
        }

        // Filter tabs row with Sort dropdown and View switch
        StudioLibraryTabsRow(
            activeLibrary = activeLibrary,
            onLibrary = onLibrary,
            counts = libraryCounts,
            sort = sort,
            onSort = onSort,
            isGrid = isGrid,
            onViewMode = onViewMode,
        )

        // Notebook Folders Section (matching Figcraft design photo: header + adaptive cards grid)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Notebook Folders",
                    color = t.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${subFolders.size} folders",
                    color = t.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W400,
                )
            }

            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val folderCardModifier = if (compact) Modifier.fillMaxWidth() else Modifier.wrapContentWidth()
                subFolders.forEachIndexed { index, sub ->
                    StudioFolderCard(
                        folder = sub,
                        accent = t.folderAccents[index % t.folderAccents.size],
                        onClick = { onOpenSubFolder(sub) },
                        modifier = folderCardModifier,
                        onRename = { onRenameFolder?.invoke(sub) },
                        onDelete = { onDeleteFolder?.invoke(sub) },
                        onAddSubfolder = { onAddSubfolder?.invoke(sub) },
                    )
                }
                StudioNewFolderCard(
                    onClick = onNewSubfolder,
                    modifier = folderCardModifier,
                )
            }
        }

        // Subfolder ke notes section
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Notes",
                    color = t.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${items.size} notes",
                    color = t.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W400,
                )
            }

            if (items.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        StudioIcons.folder,
                        null,
                        tint = t.textMuted,
                        modifier = Modifier.size(width = 44.dp, height = 36.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No notes in this folder yet",
                        color = t.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    StudioPrimaryButton(
                        label = "+ Create Notebook",
                        onClick = { onNewNotebook(folderDocId, null) },
                        height = 38.dp,
                        fontSize = 13,
                        radius = 19.dp,
                    )
                }
            } else if (isGrid) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items.forEach { note -> StudioNotebookCardFor(note, actions) }
                    StudioNewNotebookCard(
                        onClick = { style -> onNewNotebook(folderDocId, style) },
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items.forEach { note ->
                        StudioNoteRow(
                            item = note,
                            thumbnail = actions.thumbnails(note),
                            selected = actions.selection.contains(note.uri),
                            selectionMode = actions.selectionMode,
                            onClick = { actions.onClick(note) },
                            onLongClick = { actions.onLongClick(note) },
                            onSelect = { actions.onSelect(note) },
                            onToggleStar = { actions.onToggleStar(note) },
                            onDelete = { actions.onDelete(note) },
                            onMove = { actions.onMove(note) },
                            onRename = { actions.onRename(note) },
                            onTogglePin = { actions.onTogglePin(note) },
                            onExportPdf = { actions.onExportPdf(note) },
                            onShowPath = { actions.onShowPath(note) },
                        )
                    }
                }
            }
        }
    }
}

// ── Shared header pieces ─────────────────────────────────────────────────────────────────────

/**
 * The screen title, optional badge and real meta line, with the header actions beside it on wide
 * screens and underneath it on phones so the three buttons never squeeze the heading to nothing.
 */
@Composable
private fun StudioScreenHeading(
    title: String,
    meta: String,
    compact: Boolean,
    badge: String? = null,
    actions: @Composable () -> Unit,
) {
    val t = studioTokens()
    val heading: @Composable () -> Unit = {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    title,
                    color = t.textPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (badge != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(t.primary.copy(alpha = 0.12f))
                            .border(0.5.dp, t.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(badge, color = t.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                meta,
                color = t.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (compact) {
        Column(Modifier.fillMaxWidth()) {
            heading()
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
        }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { heading() }
            Spacer(Modifier.width(16.dp))
            actions()
        }
    }
}

@Composable
private fun StudioHeaderActions(
    selectionMode: Boolean,
    onSelectClick: () -> Unit,
    onImportPdf: () -> Unit,
    onNewNotebook: (() -> Unit)? = null,
    onNewSubfolder: (() -> Unit)? = null,
) {
    val t = studioTokens()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectionMode) {
            Row(
                Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(t.searchField)
                    .border(1.dp, t.searchBorder, RoundedCornerShape(24.dp))
                    .clickable(onClick = onSelectClick)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Cancel Select",
                    color = t.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.W600,
                )
            }
        }
        if (onNewSubfolder != null) {
            Row(
                Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(t.searchField)
                    .border(1.dp, t.searchBorder, RoundedCornerShape(24.dp))
                    .clickable(onClick = onNewSubfolder)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Icon(
                    StudioIcons.folderPlus,
                    contentDescription = "New Subfolder",
                    tint = t.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "+ Subfolder",
                    color = t.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W600,
                )
            }
        }
        // Import PDF button: 148x48dp, rounded 24dp, bg t.searchField, border 1dp t.searchBorder
        Row(
            Modifier
                .size(width = 148.dp, height = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(t.searchField)
                .border(1.dp, t.searchBorder, RoundedCornerShape(24.dp))
                .clickable(onClick = onImportPdf),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                painter = painterResource(R.drawable.fig_download),
                contentDescription = "Import PDF",
                tint = t.textPrimary,
                modifier = Modifier.size(16.dp, 18.dp),
            )
            Text(
                "Import PDF",
                color = t.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.W600,
            )
        }
        if (onNewNotebook != null) {
            // New Notebook button: 182x48dp, rounded 24dp, shadow 3dp, bg t.fabBg (Dark: 0xFF998BEA, Light: 0xFFFFB200)
            Row(
                Modifier
                    .size(width = 182.dp, height = 48.dp)
                    .shadow(3.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(t.fabBg)
                    .clickable(onClick = onNewNotebook),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                Icon(
                    painter = painterResource(R.drawable.fig_plus),
                    contentDescription = "New Notebook",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "New Notebook",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.W600,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudioLibraryTabsRow(
    activeLibrary: StudioLibrary,
    onLibrary: (StudioLibrary) -> Unit,
    counts: Map<StudioLibrary, Int>,
    sort: StudioSort,
    onSort: (StudioSort) -> Unit,
    isGrid: Boolean,
    onViewMode: (Boolean) -> Unit,
) {
    val t = studioTokens()
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val chips: @Composable () -> Unit = {
        StudioLibraryTabChip(
            label = "All",
            count = counts[StudioLibrary.ALL] ?: 0,
            iconRes = R.drawable.fig_grid,
            width = 100.dp,
            selected = activeLibrary == StudioLibrary.ALL,
        ) { onLibrary(StudioLibrary.ALL) }

        StudioLibraryTabChip(
            label = "Notes",
            count = counts[StudioLibrary.NOTES] ?: 0,
            iconRes = R.drawable.fig_notes,
            width = 121.dp,
            selected = activeLibrary == StudioLibrary.NOTES,
        ) { onLibrary(StudioLibrary.NOTES) }

        StudioLibraryTabChip(
            label = "Canvas",
            count = counts[StudioLibrary.CANVAS] ?: 0,
            iconRes = R.drawable.fig_nav_grid,
            width = 131.dp,
            selected = activeLibrary == StudioLibrary.CANVAS,
        ) { onLibrary(StudioLibrary.CANVAS) }
    }

    val plateShape = RoundedCornerShape(26.dp)
    val plateBg = if (t.isDark) Color(0xFF15151A) else Color(0xFFEFF2F7)
    val plateBorder = if (t.isDark) Color(0x2194A3B8) else Color(0xFFE1E6EF)

    val plate = @Composable {
        Box(
            modifier = Modifier
                .height(52.dp)
                .then(
                    if (t.isDark) {
                        Modifier.figcraftDarkShadow(shapeRadius = 26.dp, offsetY = 6.dp, spread = 2.dp)
                    } else Modifier
                )
                .clip(plateShape)
                .background(plateBg)
                .border(1.dp, plateBorder, plateShape)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                chips()
            }
        }
    }

    if (compact) {
        Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                contentAlignment = Alignment.CenterStart,
            ) {
                plate()
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                StudioSortControl(sort = sort, onSort = onSort)
                Spacer(Modifier.width(16.dp))
                StudioViewSwitch(isGrid = isGrid, onChange = onViewMode)
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                contentAlignment = Alignment.CenterStart,
            ) {
                plate()
            }
            Spacer(Modifier.width(16.dp))
            StudioSortControl(sort = sort, onSort = onSort)
            Spacer(Modifier.width(16.dp))
            StudioViewSwitch(isGrid = isGrid, onChange = onViewMode)
        }
    }
}

@Composable
private fun StudioLibraryTabChip(
    label: String,
    count: Int,
    iconRes: Int,
    width: Dp,
    selected: Boolean,
    isPdfBadge: Boolean = false,
    iconTint: Color? = null,
    onClick: () -> Unit,
) {
    val t = studioTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "tabChipPress",
    )
    val bgAnim by animateColorAsState(
        targetValue = if (selected) {
            if (t.isDark) Color(0xFF1D3054) else t.primary
        } else {
            if (t.isDark) Color(0xFF15151A) else Color.White
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tabChipBg",
    )

    Row(
        Modifier
            .size(width = width, height = 40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(bgAnim)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, if (t.isDark) Color(0x2194A3B8) else Color(0xFFE1E6EF), RoundedCornerShape(20.dp))
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = if (selected) Color.White else (iconTint ?: (if (t.isDark) Color(0xFFF5F7FA) else t.textPrimary)),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = if (selected) Color.White else (if (t.isDark) Color(0xFFF5F7FA) else t.textPrimary),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.W600,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .height(20.dp)
                .widthIn(min = 22.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        selected -> if (t.isDark) Color(0xFF222A37).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.22f)
                        isPdfBadge -> Color(0xFFE81E21).copy(alpha = 0.18f)
                        t.isDark -> Color(0xFF222A37).copy(alpha = 0.25f)
                        else -> Color(0xFFF1F5F9)
                    }
                )
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                count.toString(),
                color = when {
                    selected -> Color.White
                    isPdfBadge -> Color(0xFFE81E21)
                    t.isDark -> Color(0xFFA6B0C0)
                    else -> t.textSecondary
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun StudioSortControl(
    sort: StudioSort,
    onSort: (StudioSort) -> Unit,
) {
    val t = studioTokens()
    var open by remember { mutableStateOf(false) }
    val offset = with(LocalDensity.current) { 48.dp.roundToPx() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val sortScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "sortControlPress",
    )
    Box {
        Row(
            Modifier
                .size(width = 150.dp, height = 44.dp)
                .graphicsLayer {
                    scaleX = sortScale
                    scaleY = sortScale
                }
                .then(
                    if (t.isDark) {
                        Modifier.figcraftDarkShadow(shapeRadius = 22.dp, offsetY = 6.dp, spread = 2.dp)
                    } else Modifier
                )
                .clip(RoundedCornerShape(22.dp))
                .background(t.searchField)
                .border(1.dp, t.searchBorder, RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) { open = !open }
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Sort: ${sort.label}",
                color = t.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.W600,
                maxLines = 1,
            )
            Icon(
                painter = painterResource(R.drawable.fig_chevron_down),
                contentDescription = "Sort options",
                tint = t.textSecondary,
                modifier = Modifier.size(width = 12.dp, height = 7.dp),
            )
        }
        if (open) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, offset),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                StudioMenuSurface(minWidth = 150.dp) {
                    StudioSort.entries.forEach { option ->
                        val active = option == sort
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) t.selectedRow else Color.Transparent)
                                .clickable {
                                    open = false
                                    onSort(option)
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                option.label,
                                color = if (active) t.primary else t.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Small common pieces ─────────────────────────────────────────────────────────────────────

@Composable
private fun StudioNotebookCardFor(item: StudioItem, actions: StudioNoteActions) {
    StudioNotebookCard(
        item = item,
        thumbnail = actions.thumbnails(item),
        selected = actions.selection.contains(item.uri),
        selectionMode = actions.selectionMode,
        onClick = { actions.onClick(item) },
        onLongClick = { actions.onLongClick(item) },
        onSelect = { actions.onSelect(item) },
        onToggleStar = { actions.onToggleStar(item) },
        onTogglePin = { actions.onTogglePin(item) },
        modifier = Modifier,
        onMove = { actions.onMove(item) },
        onRename = { actions.onRename(item) },
        onShare = { actions.onShare(item) },
        onExportPdf = { actions.onExportPdf(item) },
        onShowPath = { actions.onShowPath(item) },
        onDelete = { actions.onDelete(item) },
    )
}

@Composable
private fun StudioSearchEmpty(query: String) {
    val t = studioTokens()
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(StudioIcons.search, null, tint = Color(0xFF64748B), modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            "No notes matching \"$query\"",
            color = t.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Try searching by note title, folder, or paper style.",
            color = Color(0xFF64748B),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}
