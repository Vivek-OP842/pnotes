package com.pnotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.core.util.DocumentKind
import com.pnotes.ui.BrowseEntry
import com.pnotes.ui.Editor
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.NormalModeDarkPill
import com.pnotes.ui.theme.NormalModePillBorder
import com.pnotes.ui.theme.NormalModeSolidColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Slide-out solid side drawer to browse, search, and navigate workspace documents
 * in a hierarchical Folder Tree structure (folders, subfolders, unfiled notes).
 */
@Composable
fun PnotesNotesSideDrawer(
    visible: Boolean,
    editor: Editor,
    onClose: () -> Unit,
    onOpenNote: (BrowseEntry) -> Unit,
    onOpenInNewTab: (BrowseEntry) -> Unit,
    onOpenInSplit: ((BrowseEntry) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        val isDark = isGlassDark()
        val root = editor.browseRoot
        val rootDocId = remember(root) { editor.browseRootDocId(root ?: "") }

        var searchQuery by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf<DocumentKind?>(null) }

        // Fetch hierarchical tree data from workspace
        val treeData by produceState(Editor.WorkspaceTreeData(), root, visible) {
            if (root != null) {
                value = withContext(Dispatchers.IO) {
                    editor.loadWorkspaceTree(root, rootDocId)
                }
            }
        }

        // Expanded folder IDs tracking
        var expandedFolderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
        var initializedExpanded by remember { mutableStateOf(false) }

        // Auto-expand all folders upon first load
        LaunchedEffect(treeData) {
            if (!initializedExpanded && (treeData.folders.isNotEmpty() || treeData.rootNotes.isNotEmpty())) {
                val allIds = HashSet<String>()
                allIds.add("ROOT_NOTES")
                fun collect(nodes: List<Editor.FolderNode>) {
                    for (n in nodes) {
                        allIds.add(n.docId)
                        collect(n.subfolders)
                    }
                }
                collect(treeData.folders)
                expandedFolderIds = allIds
                initializedExpanded = true
            }
        }

        // Helper to collect all folder IDs for Expand/Collapse All
        val allFolderIds = remember(treeData) {
            val allIds = HashSet<String>()
            allIds.add("ROOT_NOTES")
            fun collect(nodes: List<Editor.FolderNode>) {
                for (n in nodes) {
                    allIds.add(n.docId)
                    collect(n.subfolders)
                }
            }
            collect(treeData.folders)
            allIds
        }

        val isAllExpanded = expandedFolderIds.containsAll(allFolderIds) && allFolderIds.isNotEmpty()

        val toggleFolder: (String) -> Unit = { docId ->
            expandedFolderIds = if (docId in expandedFolderIds) {
                expandedFolderIds - docId
            } else {
                expandedFolderIds + docId
            }
        }

        // Filter folders and root notes according to search and kind filter
        val filteredFolders = remember(treeData.folders, searchQuery, selectedFilter) {
            treeData.folders.mapNotNull { filterFolderNode(it, searchQuery, selectedFilter) }
        }

        val filteredRootNotes = remember(treeData.rootNotes, searchQuery, selectedFilter) {
            treeData.rootNotes.filter { entry ->
                val matchesQuery = searchQuery.isBlank() || entry.name.contains(searchQuery, ignoreCase = true)
                val matchesKind = when (selectedFilter) {
                    DocumentKind.NOTE -> DocumentKind.ofName(entry.name) == DocumentKind.NOTE
                    DocumentKind.CANVAS -> DocumentKind.ofName(entry.name) == DocumentKind.CANVAS
                    null -> true
                }
                matchesQuery && matchesKind
            }
        }

        val totalVisibleNotes = remember(filteredFolders, filteredRootNotes) {
            filteredRootNotes.size + filteredFolders.sumOf { it.totalNoteCount }
        }

        val textPrimary = if (isDark) Color(0xFFF4F4F6) else Color(0xFF18181B)
        val textBody = if (isDark) Color(0xFFF4F4F6) else Color(0xFF27272A)
        val textMuted = if (isDark) Color(0xFF9494A0) else Color(0xFF71717A)
        val innerPillBg = if (isDark) NormalModeDarkPill else Color(0xFFF4F4F5)
        val drawerBg = if (isDark) NormalModeSolidColor else Color(0xFFFFFFFF)
        val drawerBorder = if (isDark) NormalModePillBorder else Color(0xFFE4E4E7)

        Box(
            modifier = Modifier
                .width(345.dp)
                .fillMaxHeight()
                .shadow(elevation = 24.dp, shape = RectangleShape, spotColor = if (isDark) Color.Black else Color(0x33000000))
                .background(drawerBg)
                .border(
                    width = 1.dp,
                    color = drawerBorder,
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.folder,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Workspace Notes",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                            )
                            Text(
                                text = "$totalVisibleNotes document${if (totalVisibleNotes == 1) "" else "s"} • ${filteredFolders.size} folder${if (filteredFolders.size == 1) "" else "s"}",
                                fontSize = 11.5.sp,
                                color = textMuted,
                            )
                        }
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(innerPillBg)
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFD4D4D8), CircleShape)
                            .bouncyClickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.close,
                            contentDescription = "Close Drawer",
                            tint = textBody,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar with High-Contrast Solid Background & Clear Text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.18f) else Color(0xFFD4D4D8), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = PnotesIcons.file,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp),
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search existing notes...",
                                    fontSize = 13.sp,
                                    color = textMuted,
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                cursorBrush = SolidColor(if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PnotesIcons.close,
                                    contentDescription = "Clear search",
                                    tint = textMuted,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterPill(
                        label = "All",
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        isDark = isDark,
                    )
                    FilterPill(
                        label = "Notebooks",
                        selected = selectedFilter == DocumentKind.NOTE,
                        onClick = { selectedFilter = DocumentKind.NOTE },
                        isDark = isDark,
                    )
                    FilterPill(
                        label = "Canvases",
                        selected = selectedFilter == DocumentKind.CANVAS,
                        onClick = { selectedFilter = DocumentKind.CANVAS },
                        isDark = isDark,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sub-header with section title and Expand All / Collapse All toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "FOLDER HIERARCHY",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = textMuted,
                    )
                    Text(
                        text = if (isAllExpanded) "Collapse all" else "Expand all",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                expandedFolderIds = if (isAllExpanded) emptySet() else allFolderIds
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Tree Structure List
                if (filteredFolders.isEmpty() && filteredRootNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching notes or folders found" else "No notes in workspace yet",
                            fontSize = 13.sp,
                            color = textMuted,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        // Folders
                        items(filteredFolders, key = { it.docId }) { folderNode ->
                            FolderTreeNodeView(
                                node = folderNode,
                                level = 0,
                                isDark = isDark,
                                textPrimary = textPrimary,
                                textMuted = textMuted,
                                expandedFolderIds = expandedFolderIds,
                                isSearching = searchQuery.isNotBlank(),
                                onToggleExpand = toggleFolder,
                                onOpenNote = onOpenNote,
                                onOpenInNewTab = onOpenInNewTab,
                                onOpenInSplit = onOpenInSplit,
                            )
                        }

                        // Root / Unfiled Notes
                        if (filteredRootNotes.isNotEmpty()) {
                            item(key = "ROOT_NOTES") {
                                RootNotesSectionView(
                                    rootNotes = filteredRootNotes,
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textMuted = textMuted,
                                    isExpanded = searchQuery.isNotBlank() || ("ROOT_NOTES" in expandedFolderIds),
                                    onToggleExpand = { toggleFolder("ROOT_NOTES") },
                                    onOpenNote = onOpenNote,
                                    onOpenInNewTab = onOpenInNewTab,
                                    onOpenInSplit = onOpenInSplit,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recursively filters a FolderNode for matching query or document kind.
 */
private fun filterFolderNode(
    node: Editor.FolderNode,
    query: String,
    kindFilter: DocumentKind?
): Editor.FolderNode? {
    val matchesFolderQuery = query.isNotBlank() && node.name.contains(query, ignoreCase = true)

    val filteredNotes = node.notes.filter { entry ->
        val matchesQuery = query.isBlank() || matchesFolderQuery || entry.name.contains(query, ignoreCase = true)
        val matchesKind = when (kindFilter) {
            DocumentKind.NOTE -> DocumentKind.ofName(entry.name) == DocumentKind.NOTE
            DocumentKind.CANVAS -> DocumentKind.ofName(entry.name) == DocumentKind.CANVAS
            null -> true
        }
        matchesQuery && matchesKind
    }

    val filteredSubfolders = node.subfolders.mapNotNull { filterFolderNode(it, query, kindFilter) }

    val totalCount = filteredNotes.size + filteredSubfolders.sumOf { it.totalNoteCount }

    if (query.isNotBlank()) {
        if (matchesFolderQuery || totalCount > 0) {
            return node.copy(
                subfolders = filteredSubfolders,
                notes = filteredNotes,
                totalNoteCount = totalCount
            )
        }
        return null
    }

    if (kindFilter != null && totalCount == 0) {
        return null
    }

    return node.copy(
        subfolders = filteredSubfolders,
        notes = filteredNotes,
        totalNoteCount = totalCount
    )
}

/**
 * Collapsible Folder Tree Node item with amber folder icon, note count badge,
 * and animated children guide line.
 */
@Composable
private fun FolderTreeNodeView(
    node: Editor.FolderNode,
    level: Int,
    isDark: Boolean,
    textPrimary: Color,
    textMuted: Color,
    expandedFolderIds: Set<String>,
    isSearching: Boolean,
    onToggleExpand: (String) -> Unit,
    onOpenNote: (BrowseEntry) -> Unit,
    onOpenInNewTab: (BrowseEntry) -> Unit,
    onOpenInSplit: ((BrowseEntry) -> Unit)?,
) {
    val isExpanded = isSearching || (node.docId in expandedFolderIds)
    val folderBg = if (isDark) Color(0xFF27272A) else Color(0xFFF8FAFC)
    val folderBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE4E4E7)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Folder Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(folderBg)
                .border(1.dp, folderBorder, RoundedCornerShape(10.dp))
                .clickable { onToggleExpand(node.docId) }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                // Chevron icon
                Icon(
                    imageVector = if (isExpanded) PnotesIcons.chevronDown else PnotesIcons.chevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = textMuted,
                    modifier = Modifier.size(14.dp),
                )

                // Amber Folder Icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isDark) Color(0xFFF59E0B).copy(alpha = 0.18f) else Color(0xFFFEF3C7)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.folder,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                        modifier = Modifier.size(16.dp),
                    )
                }

                // Folder Name
                Text(
                    text = node.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Note count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFE4E4E7))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${node.totalNoteCount}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMuted,
                )
            }
        }

        // Children (Subfolders & Notes) with left guide line
        if (isExpanded && (node.subfolders.isNotEmpty() || node.notes.isNotEmpty())) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp)
                    .drawBehind {
                        val strokeWidth = 1.5.dp.toPx()
                        val lineColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFD4D4D8)
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                    .padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Subfolders
                for (subfolder in node.subfolders) {
                    FolderTreeNodeView(
                        node = subfolder,
                        level = level + 1,
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textMuted = textMuted,
                        expandedFolderIds = expandedFolderIds,
                        isSearching = isSearching,
                        onToggleExpand = onToggleExpand,
                        onOpenNote = onOpenNote,
                        onOpenInNewTab = onOpenInNewTab,
                        onOpenInSplit = onOpenInSplit,
                    )
                }

                // Notes inside this folder
                for (note in node.notes) {
                    DrawerNoteItem(
                        entry = note,
                        isDark = isDark,
                        onOpen = { onOpenNote(note) },
                        onOpenInNewTab = { onOpenInNewTab(note) },
                        onOpenInSplit = onOpenInSplit?.let { { it(note) } },
                    )
                }
            }
        }
    }
}

/**
 * Section for notes residing in the root workspace directory.
 */
@Composable
private fun RootNotesSectionView(
    rootNotes: List<BrowseEntry>,
    isDark: Boolean,
    textPrimary: Color,
    textMuted: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenNote: (BrowseEntry) -> Unit,
    onOpenInNewTab: (BrowseEntry) -> Unit,
    onOpenInSplit: ((BrowseEntry) -> Unit)?,
) {
    val folderBg = if (isDark) Color(0xFF27272A) else Color(0xFFF8FAFC)
    val folderBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE4E4E7)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(folderBg)
                .border(1.dp, folderBorder, RoundedCornerShape(10.dp))
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = if (isExpanded) PnotesIcons.chevronDown else PnotesIcons.chevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = textMuted,
                    modifier = Modifier.size(14.dp),
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isDark) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFFE0F2FE)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.file,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        modifier = Modifier.size(16.dp),
                    )
                }

                Text(
                    text = "Root / Unfiled Notes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFE4E4E7))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${rootNotes.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMuted,
                )
            }
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp)
                    .drawBehind {
                        val strokeWidth = 1.5.dp.toPx()
                        val lineColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFD4D4D8)
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                    .padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (note in rootNotes) {
                    DrawerNoteItem(
                        entry = note,
                        isDark = isDark,
                        onOpen = { onOpenNote(note) },
                        onOpenInNewTab = { onOpenInNewTab(note) },
                        onOpenInSplit = onOpenInSplit?.let { { it(note) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
) {
    val activeBg = if (isDark) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFFE0F2FE)
    val inactiveBg = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF4F4F5)
    val activeText = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val inactiveText = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)

    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) activeBg else inactiveBg)
            .border(
                width = 1.dp,
                color = if (selected) activeText.copy(alpha = 0.4f) else (if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE4E4E7)),
                shape = RoundedCornerShape(14.dp)
            )
            .bouncyClickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) activeText else inactiveText,
        )
    }
}

@Composable
private fun DrawerNoteItem(
    entry: BrowseEntry,
    isDark: Boolean,
    onOpen: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onOpenInSplit: (() -> Unit)?,
) {
    val isCanvas = DocumentKind.ofName(entry.name) == DocumentKind.CANVAS
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
    val textMuted = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
    val cardBg = if (isDark) Color(0xFF1E1E22) else Color(0xFFF1F5F9)
    val borderCol = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (isCanvas) {
                                if (isDark) Color(0xFFA855F7).copy(alpha = 0.2f) else Color(0xFFF3E8FF)
                            } else {
                                if (isDark) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFFE0F2FE)
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isCanvas) PnotesIcons.grid else PnotesIcons.file,
                        contentDescription = null,
                        tint = if (isCanvas) (if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA))
                               else (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)),
                        modifier = Modifier.size(16.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DocumentKind.stripSuffix(entry.name),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isCanvas) "Infinite Canvas" else "Paged Note",
                        fontSize = 10.5.sp,
                        color = textMuted,
                    )
                }
            }

            // Quick Open in New Tab Action Button
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                        .bouncyClickable(onClick = onOpenInNewTab),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.plus,
                        contentDescription = "Open in New Tab",
                        tint = textPrimary,
                        modifier = Modifier.size(13.dp),
                    )
                }

                if (onOpenInSplit != null) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                            .bouncyClickable(onClick = onOpenInSplit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.split,
                            contentDescription = "Open in Split",
                            tint = textPrimary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
}
