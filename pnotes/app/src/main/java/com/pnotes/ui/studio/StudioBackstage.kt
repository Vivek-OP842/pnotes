package com.pnotes.ui.studio

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pnotes.R
import com.pnotes.ui.BackstageView
import com.pnotes.ui.Editor
import com.pnotes.ui.NewNoteDialog
import com.pnotes.ui.RenameDialog
import com.pnotes.ui.model.NoteCreationType
import com.pnotes.ui.model.PaperStyleOption
import com.pnotes.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Width of the permanent sidebar, straight from the reference frame. */
private const val SIDEBAR_WIDTH_DP = 312

/**
 * The studio home: the app's library, laid out like the supplied reference — one full-width header,
 * a sidebar that is always there, and the notebook/folder content beside it.
 *
 * Everything it shows comes from the real library. The folder list is the actual folder tree under
 * the user's chosen root (with real counts), the cards show each note's own rendered page, and the
 * numbers in the headers are counted from those same notes. Where the reference names something
 * this app does not have, the control is presented disabled and says so rather than pretending.
 */
@Composable
internal fun StudioBackstage(
    editor: Editor,
    onOpenFile: (String, String?) -> Unit,
    onImportPdf: () -> Unit,
    onOpenSystem: () -> Unit,
    onPickRoot: () -> Unit,
    onSelectView: (BackstageView) -> Unit,
    onExitApp: () -> Unit,
    onShareFile: (String) -> Unit,
    onSaveCopyFile: (String) -> Unit,
    onExportFilePdf: (String) -> Unit,
    onOpenSplit: (String, String) -> Unit,
) {
    val t = studioTokens()
    val root = editor.browseRoot
    if (root == null) {
        StudioRootPrompt(onPickRoot = onPickRoot, onUseAppStorage = { editor.useInternalStorage() })
        return
    }
    StudioLibraryHost(
        editor = editor,
        root = root,
        onOpenFile = onOpenFile,
        onImportPdf = onImportPdf,
        onOpenSystem = onOpenSystem,
        onPickRoot = onPickRoot,
        onSelectView = onSelectView,
        onExitApp = onExitApp,
        onShareFile = onShareFile,
        onSaveCopyFile = onSaveCopyFile,
        onExportFilePdf = onExportFilePdf,
        onOpenSplit = onOpenSplit,
    )
}

/**
 * The state the app lands in when its granted folder can no longer be read: the notes are still on
 * disk, but Android has dropped this app's permission to the tree (a reinstall, a moved folder, a
 * removed card). It says exactly that and offers the one action that fixes it.
 */
@Composable
private fun StudioAccessLost(onPickRoot: () -> Unit, onUseAppStorage: () -> Unit) {
    val t = studioTokens()
    Column(
        Modifier.fillMaxSize().background(t.bg).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("This folder can no longer be read", color = t.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your notes are still on the device — Android just dropped this app's permission to the folder. " +
                "Grant it again, pick another folder, or keep notes in app storage.",
            color = t.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StudioPrimaryButton("Grant access again", icon = StudioIcons.folder, onClick = onPickRoot, modifier = Modifier.width(210.dp))
            StudioSoftButton("Use App Storage", StudioIcons.download, onUseAppStorage, height = 44.dp)
        }
    }
}

/** The first-run state, before a folder has been granted. */
@Composable
private fun StudioRootPrompt(onPickRoot: () -> Unit, onUseAppStorage: () -> Unit) {
    val t = studioTokens()
    Column(
        Modifier.fillMaxSize().background(t.bg).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Choose where your notes live",
            color = t.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "PNotes browses a folder you grant it, so your notes stay in your own storage.",
            color = t.textSecondary,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StudioPrimaryButton("Choose folder", icon = StudioIcons.folder, onClick = onPickRoot, modifier = Modifier.width(180.dp))
            StudioSoftButton("Use App Storage", StudioIcons.download, onUseAppStorage, height = 44.dp)
        }
    }
}

@Composable
private fun StudioLibraryHost(
    editor: Editor,
    root: String,
    onOpenFile: (String, String?) -> Unit,
    onImportPdf: () -> Unit,
    onOpenSystem: () -> Unit,
    onPickRoot: () -> Unit,
    onSelectView: (BackstageView) -> Unit,
    onExitApp: () -> Unit,
    onShareFile: (String) -> Unit,
    onSaveCopyFile: (String) -> Unit,
    onExportFilePdf: (String) -> Unit,
    onOpenSplit: (String, String) -> Unit,
) {
    val t = studioTokens()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Navigation & view state ───────────────────────────────────────────────────────────────
    var query by rememberSaveable { mutableStateOf("") }
    var library by rememberSaveable { mutableStateOf(StudioLibrary.ALL) }
    var folderDocId by rememberSaveable { mutableStateOf<String?>(null) }
    var subFolderDocId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedFolderIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    val folderSubFolders = remember { mutableStateMapOf<String, List<StudioFolder>>() }
    var filter by rememberSaveable { mutableStateOf(StudioFilter.ALL) }
    var sortName by rememberSaveable { mutableStateOf(StudioSort.MODIFIED.name) }
    var isGrid by rememberSaveable { mutableStateOf(true) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var showAccountPopover by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val selection = remember { mutableStateListOf<String>() }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastNonce by remember { mutableStateOf(0) }
    // The reference is a wide workspace: below 600dp the sidebar becomes a drawer.
    val compact = LocalConfiguration.current.screenWidthDp < 600
    var sidebarOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(compact, folderDocId, subFolderDocId, library, filter) {
        if (compact) sidebarOpen = false
    }

    LaunchedEffect(toastNonce) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(1800)
            toastMessage = null
        }
    }

    // ── Dialog state ──────────────────────────────────────────────────────────────────────────
    var showNewNote by remember { mutableStateOf(false) }
    var newNoteStyle by remember { mutableStateOf(PaperStyleOption.PLAIN) }
    var newNoteParentDocId by remember { mutableStateOf<String?>(null) }
    var newFolderParent by remember { mutableStateOf<String?>(null) }
    var showNewFolder by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<StudioItem?>(null) }
    var renameFolderTarget by remember { mutableStateOf<StudioFolder?>(null) }
    var deleteTarget by remember { mutableStateOf<StudioItem?>(null) }
    var deleteFolderTarget by remember { mutableStateOf<StudioFolder?>(null) }
    var moveTarget by remember { mutableStateOf<StudioItem?>(null) }
    var pathTarget by remember { mutableStateOf<StudioItem?>(null) }
    var fieldError by remember { mutableStateOf<String?>(null) }
    val sort = StudioSort.valueOf(sortName)

    val rootDocId = remember(root) { editor.browseRootDocId(root) }
    val rootName by produceState(editor.cachedRootName(root) ?: "Workspace", root) {
        value = withContext(Dispatchers.IO) { editor.browseRootName(root) } ?: "Workspace"
    }
    // A granted folder can stop being readable — a machine-level reinstall revokes the grant, the
    // folder can be moved, the SD card pulled. Detect that instead of showing an empty library.
    val rootAccessible by produceState(true, root, refreshKey) {
        value = withContext(Dispatchers.IO) { runCatching { editor.browseRootName(root) != null }.getOrDefault(false) }
    }

    // ── Library data (all on IO; the provider queries are cached by the editor) ──────────────
    val folders by produceState(emptyList(), root, refreshKey, editor.noteOpen) {
        value = loadStudioFolders(editor, root, rootDocId)
    }
    val folderNames by produceState(emptyMap<String, String>(), root, refreshKey) {
        value = withContext(Dispatchers.IO) { folderNameIndex(editor, root, rootDocId) }
    }
    val vaultItems by produceState(emptyList(), root, refreshKey, editor.noteOpen, folderNames) {
        value = loadStudioItems(editor, root, rootDocId, recursive = true, folderNames = folderNames)
    }

    // Load subfolders for any expanded folder, active folder, or subfolder
    LaunchedEffect(expandedFolderIds, folders, folderDocId, subFolderDocId, refreshKey) {
        val toLoad = (expandedFolderIds + listOfNotNull(folderDocId, subFolderDocId)).toSet()
        for (fId in toLoad) {
            val subs = withContext(Dispatchers.IO) { loadStudioFolders(editor, root, fId) }
            folderSubFolders[fId] = subs
        }
    }

    LaunchedEffect(folderDocId) {
        val id = folderDocId
        if (id != null && !expandedFolderIds.contains(id)) {
            expandedFolderIds = expandedFolderIds + id
        }
    }

    val openFolder = folders.firstOrNull { it.docId == folderDocId }
    val shownFolderDocId = subFolderDocId ?: folderDocId
    val currentSubFolders = shownFolderDocId?.let { folderSubFolders[it] } ?: emptyList()
    val subFolders = currentSubFolders
    val activeDocId = subFolderDocId ?: folderDocId
    // A picked PDF (or .pnote) waiting for its name. The picker remembers the file and the copy
    // only happens at Save, so this prompt can appear immediately.
    val pendingImport = editor.pendingImport
    // Where an import lands: the folder the user is browsing, or the library root when they are
    // looking at everything.
    val importParentDocId = com.pnotes.ui.ImportPrompt.targetFolder(activeDocId, rootDocId)
    val folderItems by produceState(emptyList(), root, activeDocId, refreshKey, editor.noteOpen, folderNames) {
        val id = activeDocId
        value = if (id == null) emptyList() else loadStudioItems(editor, root, id, recursive = false, folderNames = folderNames)
    }
    val searchResults by produceState<List<StudioItem>?>(null, root, query, activeDocId, refreshKey, folderNames) {
        val q = query.trim()
        if (q.isEmpty()) {
            value = null
            return@produceState
        }
        kotlinx.coroutines.delay(220)
        val scopeDoc = activeDocId ?: rootDocId
        val found = withContext(Dispatchers.IO) { editor.searchNotes(root, scopeDoc, q) }
        value = found.map { entry ->
            val meta = runCatching { editor.noteMeta(entry.documentUri) }.getOrNull()
            StudioItem(
                entry = entry,
                kind = studioKindOf(entry.name),
                folderDocId = entry.parentDocId,
                folderName = folderNames[entry.parentDocId],
                pages = meta?.pages,
                pattern = meta?.pattern,
                pdfBacked = meta?.pdfBacked ?: false,
            )
        }
    }
    val storage by produceState(StudioStorage(0L, 0L, "On-device storage"), refreshKey) {
        value = withContext(Dispatchers.IO) { deviceStorage(context) }
    }

    // Live previews: each note's own rendered tile, loaded a few at a time so the grid fills in
    // rather than blocking on the first frame.
    val thumbs = remember { mutableStateMapOf<String, ImageBitmap>() }
    val thumbQueue = remember(folderItems, vaultItems, searchResults) {
        (searchResults ?: if (folderDocId != null) folderItems else vaultItems).map { it.uri }.take(16)
    }
    LaunchedEffect(thumbQueue) {
        for (uri in thumbQueue) {
            if (thumbs[uri] == null) {
                val item = (searchResults ?: if (folderDocId != null) folderItems else vaultItems).firstOrNull { it.uri == uri } ?: continue
                editor.tileThumbnail(uri, item.name)?.let { thumbs[uri] = it }
            }
        }
    }

    // ── Derived content ───────────────────────────────────────────────────────────────────────
    val rootNotes = remember(vaultItems, rootDocId) {
        vaultItems.filter { it.folderDocId == rootDocId || it.folderName.isNullOrBlank() }
    }
    val recentVaultNotes = remember(vaultItems) {
        vaultItems.sortedByDescending { it.modified }.take(10)
    }
    val baseItems = when {
        searchResults != null -> searchResults.orEmpty()
        folderDocId != null -> folderItems
        else -> rootNotes
    }
    val libraryItems = when (library) {
        StudioLibrary.ALL -> baseItems
        StudioLibrary.NOTES -> baseItems.filter { it.kind == StudioKind.NOTE }
        StudioLibrary.CANVAS -> baseItems.filter { it.kind == StudioKind.CANVAS }
        StudioLibrary.FAVORITES -> baseItems.filter { it.isFavorite }
        StudioLibrary.PDFS -> baseItems.filter { it.kind == StudioKind.PDF || it.pdfBacked }
        StudioLibrary.SAVED -> baseItems.filter { it.isPinned }
    }
    val visibleItems = sortItems(libraryItems.filter { matches(it, filter) }, sort)
    val quickFilters = remember(libraryItems, t.isDark) {
        listOf(
            Triple("Grid Paper", StudioFilter.GRID, Color(0xFF2563EB)),
            Triple("Ruled Paper", StudioFilter.RULED, Color(0xFF16A34A)),
            Triple("Dot Grid", StudioFilter.DOTS, Color(0xFF7C3AED)),
            Triple("Cornell", StudioFilter.CORNELL, Color(0xFFEA580C)),
        ).filter { (_, f, _) -> countFor(libraryItems, f) > 0 }
            .map { (label, f, color) -> StudioQuickFilter(label, color, f) }
    }
    val libraryCounts = mapOf(
        StudioLibrary.ALL to vaultItems.size,
        StudioLibrary.NOTES to vaultItems.count { it.kind == StudioKind.NOTE },
        StudioLibrary.CANVAS to vaultItems.count { it.kind == StudioKind.CANVAS },
        StudioLibrary.FAVORITES to vaultItems.count { it.isFavorite },
        StudioLibrary.PDFS to vaultItems.count { it.kind == StudioKind.PDF || it.pdfBacked },
        StudioLibrary.SAVED to vaultItems.count { it.isPinned },
    )
    val screenCounts = remember(baseItems) {
        mapOf(
            StudioLibrary.ALL to baseItems.size,
            StudioLibrary.NOTES to baseItems.count { it.kind == StudioKind.NOTE },
            StudioLibrary.CANVAS to baseItems.count { it.kind == StudioKind.CANVAS },
            StudioLibrary.FAVORITES to baseItems.count { it.isFavorite },
            StudioLibrary.PDFS to baseItems.count { it.kind == StudioKind.PDF || it.pdfBacked },
            StudioLibrary.SAVED to baseItems.count { it.isPinned },
        )
    }
    val libraryTitle = when (library) {
        StudioLibrary.ALL -> "All Handwritten Notes"
        StudioLibrary.NOTES -> "All Notes"
        StudioLibrary.CANVAS -> "All Freeform Canvases"
        StudioLibrary.FAVORITES -> "Favorite Notes"
        StudioLibrary.PDFS -> "PDFs & Annotated Documents"
        StudioLibrary.SAVED -> "Saved & Pinned"
    }
    val notesByFolder = remember(visibleItems) { visibleItems.groupBy { it.folderDocId } }
    val accountState by com.pnotes.account.AccountStore.get(context).state.collectAsState()

    fun rescan() {
        refreshKey++
    }

    fun openItem(item: StudioItem) {
        if (selectionMode) {
            if (selection.contains(item.uri)) selection.remove(item.uri) else selection.add(item.uri)
            return
        }
        onOpenFile(item.uri, item.name)
    }

    fun toggleSelection(item: StudioItem) {
        if (selection.contains(item.uri)) selection.remove(item.uri) else selection.add(item.uri)
    }

    fun startNewNote(folder: String?, styleId: String?) {
        newNoteParentDocId = folder
        newNoteStyle = when (styleId?.lowercase()) {
            "grid" -> PaperStyleOption.GRID
            "cornell" -> PaperStyleOption.CORNELL
            "dotted", "dot grid", "dots" -> PaperStyleOption.DOTTED
            "ruled" -> PaperStyleOption.RULED
            else -> PaperStyleOption.PLAIN
        }
        showNewNote = true
    }

    fun toggleTheme() {
        editor.applyPreferences(
            editor.preferences.copy(uiAppearance = if (t.isDark) "light" else "dark"),
        )
    }

    fun showToast(message: String) {
        toastMessage = message
        toastNonce++
    }

    // ── Back: leave the account popover, then selection, then the folder, then the app ────────
    BackHandler {
        when {
            sidebarOpen -> sidebarOpen = false
            showAccountPopover -> showAccountPopover = false
            selectionMode -> { selectionMode = false; selection.clear() }
            subFolderDocId != null -> subFolderDocId = null
            folderDocId != null -> { folderDocId = null; filter = StudioFilter.ALL }
            else -> onExitApp()
        }
    }

    val actions = StudioNoteActions(
        thumbnails = { thumbs[it.uri] },
        onClick = { openItem(it) },
        onLongClick = { item ->
            selectionMode = true
            if (!selection.contains(item.uri)) {
                selection.add(item.uri)
            }
        },
        onToggleStar = {
            editor.toggleFavorite(it.uri)
            rescan()
        },
        onTogglePin = {
            editor.togglePinned(it.uri)
            rescan()
        },
        onSelect = { toggleSelection(it) },
        selectionMode = selectionMode,
        selection = selection.toSet(),
        onMove = { moveTarget = it },
        onRename = { renameTarget = it },
        onShare = { onShareFile(it.uri) },
        onExportPdf = { onExportFilePdf(it.uri) },
        onShowPath = { pathTarget = it },
        onDelete = { deleteTarget = it },
    )

    // One sidebar definition, shown inline on wide screens and as a drawer on phones.
    val sidebarContent: @Composable (Modifier) -> Unit = { sidebarModifier ->
        StudioSidebar(
            onNewNote = { startNewNote(activeDocId, null) },
            counts = libraryCounts,
            activeLibrary = if (folderDocId == null) library else null,
            onLibrary = { picked ->
                library = picked
                folderDocId = null
                subFolderDocId = null
                filter = StudioFilter.ALL
                query = ""
            },
            folders = folders,
            allFoldersSelected = folderDocId == null,
            onAllFolders = {
                folderDocId = null
                subFolderDocId = null
                library = StudioLibrary.ALL
                query = ""
            },
            selectedFolderDocId = folderDocId,
            selectedSubFolderDocId = subFolderDocId,
            expandedFolderIds = expandedFolderIds,
            subFoldersMap = folderSubFolders,
            onOpenFolder = { picked ->
                folderDocId = picked.docId
                subFolderDocId = null
                filter = StudioFilter.ALL
                query = ""
                expandedFolderIds = expandedFolderIds + picked.docId
            },
            onToggleFolder = { folder ->
                expandedFolderIds = if (expandedFolderIds.contains(folder.docId)) {
                    expandedFolderIds - folder.docId
                } else {
                    expandedFolderIds + folder.docId
                }
            },
            onNewFolder = { newFolderParent = rootDocId; showNewFolder = true },
            onRenameFolder = { renameFolderTarget = it },
            onDeleteFolder = { deleteFolderTarget = it },
            onAddSubfolder = { folder ->
                newFolderParent = folder.docId
                showNewFolder = true
            },
            onOpenSubFolder = {
                subFolderDocId = it.docId
                filter = StudioFilter.ALL
            },
            smartTags = quickFilters,
            activeFilter = filter,
            onSmartTag = { tag ->
                filter = if (filter == tag.filter) StudioFilter.ALL else tag.filter
            },
            storage = storage,
            onStorageClick = {
                editor.message = "${storage.label} used on this device, and your notes are in ${rootName ?: "your folder"}."
            },
            modifier = sidebarModifier,
        )
    }

    // ── Layout: header, then the permanent sidebar beside the content ──────────────────────────
    if (!rootAccessible) {
        StudioAccessLost(onPickRoot = onPickRoot, onUseAppStorage = { editor.useInternalStorage() })
        return
    }
    Box(Modifier.fillMaxSize().background(t.bgBrush)) {
        Row(Modifier.fillMaxSize()) {
            if (!compact) {
                sidebarContent(Modifier.width(SIDEBAR_WIDTH_DP.dp).fillMaxHeight())
                Box(Modifier.width(1.dp).fillMaxHeight().background(t.sidebarBorder))
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                StudioTopBar(
                    query = query,
                    onQueryChange = { query = it },
                    hint = if (openFolder != null) {
                        "Search notes, formulas & jots in ${openFolder.name}..."
                    } else {
                        "Search notes, tags, or notebooks..."
                    },
                    isGrid = isGrid,
                    onToggleGrid = { isGrid = !isGrid },
                    storageLabel = "Stored on this device · ${storage.label} used",
                    onStorageClick = {
                        editor.message = "Notes live in ${rootName ?: "your folder"}. ${storage.label} used on this device."
                    },
                    onAccountClick = { showAccountPopover = !showAccountPopover },
                    onToggleTheme = { toggleTheme() },
                    onBrandClick = {
                        folderDocId = null
                        subFolderDocId = null
                        library = StudioLibrary.ALL
                        filter = StudioFilter.ALL
                        query = ""
                    },
                    onOpenSidebar = if (compact) ({ sidebarOpen = true }) else null,
                )

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (openFolder != null) {
                        val currentSub = subFolderDocId?.let { subId ->
                            currentSubFolders.firstOrNull { it.docId == subId }
                        }
                        val shownFolder = currentSub ?: openFolder
                        StudioFolderScreen(
                            title = shownFolder.name,
                            badge = if (currentSub != null) "Subfolder" else "Active Folder",
                            backLabel = if (currentSub != null) openFolder.name else "All Folders",
                            items = visibleItems,
                            totalCount = folderItems.size,
                            pages = folderItems.mapNotNull { it.pages }.takeIf { it.isNotEmpty() }?.sum(),
                            lastTouched = folderItems.maxOfOrNull { it.modified } ?: shownFolder.modified,
                            subFolders = currentSubFolders,
                            onNewSubfolder = {
                                newFolderParent = shownFolder.docId
                                showNewFolder = true
                            },
                            activeLibrary = library,
                            onLibrary = { picked ->
                                library = picked
                                filter = StudioFilter.ALL
                            },
                            libraryCounts = screenCounts,
                            sort = sort,
                            onSort = { sortName = it.name },
                            isGrid = isGrid,
                            onViewMode = { isGrid = it },
                            selectionMode = selectionMode,
                            actions = actions,
                            onSelectClick = {
                                selectionMode = !selectionMode
                                if (!selectionMode) selection.clear()
                            },
                            onImportPdf = onImportPdf,
                            onNewNotebook = { folder, style -> startNewNote(folder, style) },
                            folderDocId = shownFolder.docId,
                            onBack = {
                                if (currentSub != null) {
                                    subFolderDocId = null
                                } else {
                                    folderDocId = null
                                    filter = StudioFilter.ALL
                                }
                            },
                            onOpenSubFolder = {
                                subFolderDocId = it.docId
                                filter = StudioFilter.ALL
                            },
                            onRenameFolder = { renameFolderTarget = it },
                            onDeleteFolder = { deleteFolderTarget = it },
                            onAddSubfolder = { folder ->
                                newFolderParent = folder.docId
                                showNewFolder = true
                            },
                        )
                    } else {
                        StudioOverviewScreen(
                            query = query,
                            title = libraryTitle,
                            items = visibleItems,
                            folders = folders,
                            notesInFolder = { folder -> notesByFolder[folder.docId].orEmpty() },
                            activeFilter = filter,
                            onClearFilter = { filter = StudioFilter.ALL },
                            activeLibrary = library,
                            onLibrary = { picked ->
                                library = picked
                                filter = StudioFilter.ALL
                                query = ""
                            },
                            libraryCounts = screenCounts,
                            recentNotes = recentVaultNotes,
                            sort = sort,
                            onSort = { sortName = it.name },
                            isGrid = isGrid,
                            onViewMode = { isGrid = it },
                            selectionMode = selectionMode,
                            actions = actions,
                            onSelectClick = {
                                selectionMode = !selectionMode
                                if (!selectionMode) selection.clear()
                            },
                            onImportPdf = onImportPdf,
                            onNewNotebook = { folder, style -> startNewNote(folder, style) },
                            onNewFolder = { newFolderParent = rootDocId; showNewFolder = true },
                            onAddSubfolder = { folder ->
                                newFolderParent = folder.docId
                                showNewFolder = true
                            },
                            onOpenFolder = { folder ->
                                folderDocId = folder.docId
                                subFolderDocId = null
                                filter = StudioFilter.ALL
                                query = ""
                                expandedFolderIds = expandedFolderIds + folder.docId
                            },
                            onRenameFolder = { renameFolderTarget = it },
                            onDeleteFolder = { deleteFolderTarget = it },
                        )
                    }

                    if (selectionMode && selection.isNotEmpty()) {
                        val moveDestinations = remember(folderNames, rootName, rootDocId) {
                            listOf(rootDocId to (rootName ?: "Workspace")) +
                                folderNames.map { (id, name) -> id to name }
                        }
                        StudioSelectionBar(
                            count = selection.size,
                            totalCount = visibleItems.size,
                            canSplit = selection.size >= 2,
                            destinations = moveDestinations,
                            onSelectAll = {
                                selection.clear()
                                selection.addAll(visibleItems.map { it.uri })
                            },
                            onOpenSplit = {
                                val picked = selection.toList()
                                onOpenSplit(picked[0], picked[1])
                                selectionMode = false
                                selection.clear()
                            },
                            onFavorite = {
                                val picked = selection.toList()
                                val n = picked.size
                                val allFav = picked.isNotEmpty() && picked.all { editor.isFavorite(it) }
                                picked.forEach { uri -> if (editor.isFavorite(uri) == allFav) editor.toggleFavorite(uri) }
                                rescan()
                                showToast("Updated favorites for $n note${if (n > 1) "s" else ""}")
                            },
                            onPin = {
                                val picked = selection.toList()
                                val n = picked.size
                                picked.forEach { if (!editor.isPinned(it)) editor.togglePinned(it) }
                                rescan()
                                showToast("Pinned $n note${if (n > 1) "s" else ""} to top")
                            },
                            onMoveTo = { destDocId ->
                                val picked = selection.toList()
                                val n = picked.size
                                val destName = if (destDocId == rootDocId) "All Folders"
                                else moveDestinations.firstOrNull { it.first == destDocId }?.second ?: "All Folders"
                                selection.clear()
                                selectionMode = false
                                showToast("Moved $n note${if (n > 1) "s" else ""} to $destName")
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        for (uri in picked) {
                                            val item = baseItems.firstOrNull { it.uri == uri }
                                            editor.moveDocumentInto(root, uri, item?.folderDocId ?: rootDocId, destDocId)
                                        }
                                    }
                                    rescan()
                                }
                            },
                            onDelete = {
                                val picked = selection.toList()
                                val n = picked.size
                                selection.clear()
                                selectionMode = false
                                showToast("Deleted $n note${if (n > 1) "s" else ""}")
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        for (uri in picked) {
                                            editor.deleteDocument(uri)
                                        }
                                    }
                                    rescan()
                                }
                            },
                            onClear = {
                                selection.clear()
                                selectionMode = false
                            },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }

                    if (!selectionMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 24.dp, bottom = 24.dp)
                                .size(48.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = CircleShape,
                                    spotColor = Color(0x33000000)
                                )
                                .clip(CircleShape)
                                .background(t.fabBg)
                                .clickable {
                                    startNewNote(activeDocId, null)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.fig_plus),
                                contentDescription = "New Notebook",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
        }

        // The reference's floating confirmation toast, above the selection bar when one is up.
        toastMessage?.let { message ->
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (selectionMode && selection.isNotEmpty()) 92.dp else 26.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp), spotColor = Color(0x66000000))
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (t.isDark) Color(0xFF1E293B) else Color(0xFF0F172A))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✓", color = Color(0xFF38BDF8), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(message, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (compact && sidebarOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { sidebarOpen = false },
            )
            Box(Modifier.align(Alignment.CenterStart).fillMaxHeight()) {
                sidebarContent(Modifier.width(SIDEBAR_WIDTH_DP.dp).fillMaxHeight())
            }
        }

        if (showAccountPopover) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showAccountPopover = false },
            )
            Box(Modifier.align(Alignment.TopEnd).padding(top = 60.dp, end = 16.dp)) {
                StudioUserPopoverCard(
                    displayName = accountState.account?.displayName?.takeIf { it.isNotBlank() } ?: "Local workspace",
                    email = accountState.account?.email ?: "Not signed in — notes stay on this device",
                    avatarLabel = accountState.avatarLabel,
                    signedIn = accountState.signedIn,
                    premium = accountState.premium,
                    storage = storage,
                    notesCount = vaultItems.size,
                    foldersCount = folders.size,
                    onClose = { showAccountPopover = false },
                    onToggleTheme = { toggleTheme() },
                    onOpenPreferences = {
                        showAccountPopover = false
                        onSelectView(BackstageView.PREFERENCES)
                    },
                    onSync = {
                        rescan()
                        editor.message = "Library re-scanned from storage."
                    },
                    onUnavailable = { editor.message = it },
                    onSignOut = {
                        com.pnotes.account.AccountStore.get(context).signOut()
                        editor.message = "Signed out. Your notes stay on this device."
                    },
                    onSignIn = {
                        showAccountPopover = false
                        onSelectView(BackstageView.LOGIN)
                    },
                )
            }
        }
    }

    // ── Dialogs (all of them real operations on the real library) ──────────────────────────────
    if (showNewNote) {
        val parentId = newNoteParentDocId ?: activeDocId ?: rootDocId
        val parentName = if (parentId == rootDocId) (rootName ?: "Workspace") else {
            (folders + subFolders).firstOrNull { it.docId == parentId }?.name ?: "Workspace"
        }
        NewNoteDialog(
            visible = true,
            currentFolderDocId = parentId,
            currentFolderName = parentName,
            availableFolders = listOf(rootDocId to (rootName ?: "Workspace")) +
                folders.map { it.docId to it.name } +
                subFolders.map { it.docId to it.name },
            initialStyle = newNoteStyle,
            onDismiss = { showNewNote = false },
            onCreate = { title, type, style, color, targetFolderDocId, cover, orientation, margins, pageSize, showPageNumber, showDate ->
                showNewNote = false
                val targetDoc = targetFolderDocId.ifEmpty { parentId }
                scope.launch {
                    val uri = withContext(Dispatchers.IO) {
                        when (type) {
                            NoteCreationType.HANDWRITTEN -> editor.createConfiguredNoteFile(
                                treeUri = root,
                                parentDocId = targetDoc,
                                rawName = title,
                                style = com.pnotes.core.model.PageStyle(
                                    pageColor = color.rgba,
                                    pattern = style.pattern,
                                ),
                                pageSize = pageSize,
                                orientation = orientation,
                                margins = margins.toPageMargins(),
                                cover = cover,
                                showDate = showDate,
                                showPageNumber = showPageNumber,
                            )
                            NoteCreationType.CANVAS -> editor.createBlankCanvasFile(root, targetDoc, title)
                        }
                    }
                    if (uri != null) {
                        rescan()
                        onOpenFile(uri, null)
                    } else {
                        editor.message = "Couldn’t create that note."
                    }
                }
            },
        )
    }

    if (showNewFolder) {
        StudioNameDialog(
            title = "New Notebook Folder",
            initial = "",
            confirmLabel = "Create",
            placeholder = "e.g. Distributed Databases",
            error = fieldError,
            onSubmit = { name ->
                val parent = newFolderParent ?: rootDocId
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { editor.createFolder(root, parent, name) }
                    if (ok) {
                        fieldError = null
                        showNewFolder = false
                        rescan()
                    } else {
                        fieldError = "Couldn’t create that folder."
                    }
                }
            },
            onDismiss = { fieldError = null; showNewFolder = false },
        )
    }

    // A picked PDF (or .pnote) awaiting its name. The picker only *remembers* the file; nothing is
    // copied until the name is confirmed, so this prompt can appear at once instead of after a
    // big copy. Without it the pick silently does nothing — the file is remembered but never
    // committed, and the user is left with no note and no explanation.
    pendingImport?.let { pending ->
        StudioNameDialog(
            title = com.pnotes.ui.ImportPrompt.title(pending.kind),
            initial = pending.defaultName,
            confirmLabel = com.pnotes.ui.ImportPrompt.CONFIRM_LABEL,
            placeholder = com.pnotes.ui.ImportPrompt.PLACEHOLDER,
            error = fieldError,
            onSubmit = { name ->
                scope.launch {
                    val uri = editor.commitImportAsync(root, importParentDocId, name)
                    when {
                        uri != null -> {
                            fieldError = null
                            rescan()
                            onOpenFile(uri, null)
                        }
                        // Still pending: a genuine failure (the copy or the render failed), so keep
                        // the prompt up with the error and let the user retry. Cancelled imports
                        // clear the pending request and fall through to the dismiss branch.
                        editor.pendingImport != null -> fieldError = "Couldn’t import that file."
                    }
                }
            },
            onDismiss = {
                fieldError = null
                editor.cancelImport()
            },
        )
    }

    renameTarget?.let { item ->
        StudioNameDialog(
            title = "Rename Note",
            initial = item.title,
            confirmLabel = "Rename",
            placeholder = "Enter note title",
            onSubmit = { name ->
                renameTarget = null
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        editor.renameDocument(item.uri, com.pnotes.core.util.DocumentKind.withSuffix(name, com.pnotes.core.util.DocumentKind.NOTE))
                    }
                    if (ok) {
                        rescan()
                        showToast("Renamed note to \"$name\"")
                    } else {
                        editor.message = "Couldn’t rename that note."
                    }
                }
            },
            onDismiss = { renameTarget = null },
        )
    }

    renameFolderTarget?.let { folder ->
        StudioNameDialog(
            title = "Rename Folder",
            initial = folder.name,
            confirmLabel = "Rename",
            onSubmit = { name ->
                renameFolderTarget = null
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { editor.renameDocument(folder.uri, name) }
                    if (ok) {
                        rescan()
                        showToast("Renamed folder to \"$name\"")
                    } else {
                        editor.message = "Couldn’t rename that folder."
                    }
                }
            },
            onDismiss = { renameFolderTarget = null },
        )
    }

    deleteTarget?.let { item ->
        ConfirmDialog(
            title = "Delete “${item.title}”?",
            body = "This removes the file from storage. It cannot be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { editor.deleteDocument(item.uri) }
                    if (ok) rescan() else editor.message = "Couldn’t delete that note."
                }
            },
            onDismiss = { deleteTarget = null },
        )
    }

    deleteFolderTarget?.let { folder ->
        ConfirmDialog(
            title = "Delete “${folder.name}”?",
            body = if (folder.notes > 0 || folder.folders > 0) {
                "This folder still holds ${folder.notes} note${if (folder.notes == 1) "" else "s"} and ${folder.folders} sub-folder${if (folder.folders == 1) "" else "s"}. Empty it first — nothing is deleted recursively."
            } else {
                "The folder is empty, so this only removes the folder itself."
            },
            confirmLabel = "Delete folder",
            enabled = folder.notes == 0 && folder.folders == 0,
            destructive = true,
            onConfirm = {
                deleteFolderTarget = null
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { editor.deleteDocument(folder.uri) }
                    if (ok) {
                        rescan()
                        showToast("Deleted folder \"${folder.name}\"")
                    } else {
                        editor.message = "Couldn’t delete that folder."
                    }
                }
            },
            onDismiss = { deleteFolderTarget = null },
        )
    }

    moveTarget?.let { item ->
        StudioMoveDialog(
            destinations = listOf(rootDocId to (rootName ?: "Workspace")) +
                allFolders(editor, root, rootDocId).map { it.first to it.second },
            currentParent = item.folderDocId,
            onPick = { destDocId ->
                moveTarget = null
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        editor.moveDocumentInto(root, item.uri, item.folderDocId, destDocId)
                    }
                    if (ok) rescan() else editor.message = "Couldn’t move that note."
                }
            },
            onDismiss = { moveTarget = null },
        )
    }

    pathTarget?.let { item ->
        val path = remember(item.uri) {
            com.pnotes.ui.resolveRealPath(context, item.uri, item.folderDocId, item.name)
        }
        ConfirmDialog(
            title = "Storage location",
            body = "${item.name}\n\n${path.ifBlank { "Resolved by the storage provider — no direct path is exposed." }}",
            confirmLabel = "Done",
            hideDismiss = true,
            onConfirm = { pathTarget = null },
            onDismiss = { pathTarget = null },
        )
    }

}

/** Every folder under the root, as (docId, name) pairs — the destinations a note can move to. */
private fun allFolders(editor: Editor, root: String, rootDocId: String): List<Pair<String, String>> {
    val out = ArrayList<Pair<String, String>>()
    val seen = HashSet<String>()
    val stack = ArrayDeque(listOf(rootDocId))
    while (stack.isNotEmpty()) {
        val docId = stack.removeLast()
        if (!seen.add(docId)) continue
        for (e in editor.browseChildren(root, docId)) {
            if (!e.isDir) continue
            val childId = editor.browseDocId(e.documentUri)
            out.add(childId to e.name)
            stack.addLast(childId)
        }
    }
    return out
}

/** docId -> folder name for every folder under the root, so cards can name their parent folder. */
private fun folderNameIndex(editor: Editor, root: String, rootDocId: String): Map<String, String> =
    allFolders(editor, root, rootDocId).toMap()

/** The device's real used/total bytes, for the sidebar's storage card. */
private fun deviceStorage(context: android.content.Context): StudioStorage = runCatching {
    val stat = android.os.StatFs(context.filesDir.path)
    val total = stat.blockCountLong * stat.blockSizeLong
    val free = stat.availableBlocksLong * stat.blockSizeLong
    StudioStorage(usedBytes = (total - free).coerceAtLeast(0L), totalBytes = total, where = "On-device storage")
}.getOrDefault(StudioStorage(0L, 0L, "On-device storage"))

/** The floating bar shown while notes are selected — the actions multi-select actually supports. */
@Composable
private fun StudioSelectionBar(
    count: Int,
    totalCount: Int,
    canSplit: Boolean,
    destinations: List<Pair<String, String>>,
    onSelectAll: () -> Unit,
    onOpenSplit: () -> Unit,
    onFavorite: () -> Unit,
    onPin: () -> Unit,
    onMoveTo: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = studioTokens()
    var moveOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Column(modifier.padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (moveOpen) {
            StudioSelectionMoveMenu(
                count = count,
                destinations = destinations,
                onPick = {
                    moveOpen = false
                    onMoveTo(it)
                },
            )
            Spacer(Modifier.height(8.dp))
        }
        // On phones the full action set is wider than the screen, so the pill caps at the viewport
        // and its contents scroll sideways rather than spilling past both edges.
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .height(54.dp)
                .shadow(elevation = 18.dp, shape = RoundedCornerShape(27.dp), spotColor = Color(0x55000000))
                .clip(RoundedCornerShape(27.dp))
                .background(if (t.isDark) Color(0xF212141C) else Color(0xF7FFFFFF))
                .border(1.dp, if (t.isDark) Color(0x4738BDF8) else Color(0x380284C7), RoundedCornerShape(27.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Count pill + select-all + dismiss
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (t.isDark) Color(0xFF1E283D) else Color(0xFFE0F2FE))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (t.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$count Selected",
                        color = if (t.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }

                if (count < totalCount) {
                    Text(
                        "Select All",
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onSelectAll)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = t.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }

                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (t.isDark) Color(0x25FFFFFF) else Color(0x15000000))
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        StudioIcons.close,
                        contentDescription = "Clear selection",
                        tint = t.textSecondary,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }

            Box(Modifier.width(1.dp).height(24.dp).background(t.divider))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Move — opens the folder picker above the bar
                Row(
                    Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            if (moveOpen) {
                                Modifier.background(if (t.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            } else Modifier
                        )
                        .clickable { moveOpen = !moveOpen }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(StudioIcons.folder, null, tint = t.textPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Move", color = t.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }

                // Favorite
                Row(
                    Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onFavorite)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(StudioIcons.star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Favorite", color = t.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }

                // Pin
                Row(
                    Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onPin)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(StudioIcons.pin, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pin", color = t.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }

                // Open in Split
                Row(
                    Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            if (canSplit) Modifier
                                .background(if (t.isDark) Color(0x2638BDF8) else Color(0xFFE0F2FE))
                                .border(1.dp, if (t.isDark) Color(0x4D38BDF8) else Color(0x4D0284C7), RoundedCornerShape(18.dp))
                            else Modifier
                        )
                        .clickable(enabled = canSplit, onClick = onOpenSplit)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        StudioIcons.splitView,
                        null,
                        tint = if (canSplit) (if (t.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) else t.textMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Open in Split",
                        color = if (canSplit) (if (t.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) else t.textMuted,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (count == 2) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(if (t.isDark) Color(0xFF0284C7) else Color(0xFF0EA5E9))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("2", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Delete — confirms inline, the way the reference does
                if (confirmDelete) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (t.isDark) Color(0x26EF4444) else Color(0xFFFEE2E2))
                            .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Delete $count?",
                            color = Color(0xFFEF4444),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444))
                                .clickable(onClick = onDelete)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text("Yes", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { confirmDelete = false }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        ) {
                            Text("Cancel", color = t.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Row(
                        Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { confirmDelete = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(StudioIcons.trash, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete", color = Color(0xFFEF4444), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** The folder picker that opens above the selection bar's Move action. */
@Composable
private fun StudioSelectionMoveMenu(
    count: Int,
    destinations: List<Pair<String, String>>,
    onPick: (String) -> Unit,
) {
    val t = studioTokens()
    Column(
        Modifier
            .width(280.dp)
            .heightIn(max = 320.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0x88000000))
            .clip(RoundedCornerShape(14.dp))
            .background(if (t.isDark) Color(0xFF161924) else Color.White)
            .border(1.dp, if (t.isDark) Color(0xFF283149) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(8.dp),
    ) {
        Text(
            "Move $count note${if (count == 1) "" else "s"} to:",
            color = t.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
        Column(Modifier.verticalScroll(rememberScrollState())) {
            destinations.forEachIndexed { index, (docId, name) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPick(docId) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        StudioIcons.folder,
                        null,
                        tint = t.folderAccents[index % t.folderAccents.size],
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        name,
                        color = t.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** A studio-styled name prompt (new folder, new canvas, rename folder). */
@Composable
private fun StudioModal(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val t = studioTokens()
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.92f)
                .then(
                    if (t.isDark) {
                        Modifier.figcraftDarkShadow(shapeRadius = 20.dp, offsetY = 8.dp, spread = 3.dp)
                    } else {
                        Modifier.shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(20.dp),
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.15f),
                            spotColor = Color.Black.copy(alpha = 0.25f),
                        )
                    }
                )
                .clip(RoundedCornerShape(20.dp))
                .background(t.card)
                .border(1.dp, t.cardBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(t.primary)
                        )
                        Text(
                            "PNOTES WORKSPACE",
                            color = t.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(title, color = t.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(t.chip)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(StudioIcons.close, "Close", tint = t.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            content()
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
private fun StudioModalCancel(label: String = "Cancel", onClick: () -> Unit) {
    val t = studioTokens()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "cancelScale",
    )
    Box(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(10.dp))
            .background(t.searchField)
            .border(1.dp, t.searchBorder, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = t.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StudioModalConfirm(
    label: String,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val t = studioTokens()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "confirmScale",
    )
    val gradient = if (destructive) {
        listOf(Color(0xFFEF4444), Color(0xFFDC2626))
    } else {
        listOf(t.primary, t.primaryDeep)
    }
    Box(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) Brush.horizontalGradient(gradient) else SolidColor(t.chip))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) Color.White else t.textMuted,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StudioNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    placeholder: String? = null,
    error: String? = null,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val t = studioTokens()
    var text by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    val fieldTag = when {
        title.contains("Folder", ignoreCase = true) -> "FOLDER NAME"
        title.contains("Note", ignoreCase = true) -> "NOTE NAME"
        title.contains("Canvas", ignoreCase = true) -> "CANVAS NAME"
        else -> "NAME"
    }
    StudioModal(
        title = title,
        onDismiss = onDismiss,
        content = {
            Text(
                fieldTag,
                color = t.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(12.dp),
                placeholder = placeholder?.let { { Text(it, color = t.textMuted, fontSize = 13.5.sp) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = t.textPrimary,
                    unfocusedTextColor = t.textPrimary,
                    focusedContainerColor = t.searchField,
                    unfocusedContainerColor = t.searchField,
                    focusedBorderColor = t.primary,
                    unfocusedBorderColor = t.searchBorder,
                    cursorColor = t.primary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onSubmit(text.trim()) }),
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
        },
        actions = {
            StudioModalCancel(onClick = onDismiss)
            Spacer(Modifier.width(10.dp))
            StudioModalConfirm(
                label = confirmLabel,
                enabled = text.isNotBlank(),
                onClick = { if (text.isNotBlank()) onSubmit(text.trim()) },
            )
        },
    )
}

/** A small confirm/notice dialog used for deletes and the storage-path readout. */
@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    enabled: Boolean = true,
    hideDismiss: Boolean = false,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val t = studioTokens()
    StudioModal(
        title = title,
        onDismiss = onDismiss,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (destructive) Color(0xFFEF4444).copy(alpha = 0.15f)
                            else t.primary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (destructive) StudioIcons.trash else StudioIcons.cloudCheck,
                        contentDescription = null,
                        tint = if (destructive) Color(0xFFEF4444) else t.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    body,
                    color = t.textSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        },
        actions = {
            if (!hideDismiss) {
                StudioModalCancel(onClick = onDismiss)
                Spacer(Modifier.width(10.dp))
            }
            StudioModalConfirm(
                label = confirmLabel,
                enabled = enabled,
                destructive = destructive,
                onClick = onConfirm,
            )
        },
    )
}

/** Destination picker for "Move to folder". */
@Composable
private fun StudioMoveDialog(
    destinations: List<Pair<String, String>>,
    currentParent: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val t = studioTokens()
    StudioModal(
        title = "Move to folder",
        onDismiss = onDismiss,
        content = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                destinations.forEachIndexed { index, (docId, name) ->
                    val active = docId == currentParent
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) t.selectedRow else Color.Transparent)
                            .border(
                                1.dp,
                                if (active) t.primary.copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = !active) { onPick(docId) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.fig_folder),
                            contentDescription = null,
                            tint = if (active) t.textMuted else t.folderAccents[index % t.folderAccents.size],
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            name,
                            color = if (active) t.textMuted else t.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (active) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(t.chip)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Current", color = t.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        actions = {
            StudioModalCancel(onClick = onDismiss)
        },
    )
}
