package com.pnotes.ui.studio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.pnotes.ui.BrowseEntry
import com.pnotes.ui.Editor
import com.pnotes.ui.isCanvasDocument
import com.pnotes.ui.isPdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The studio's view of the library: real explorer entries plus the few facts the grid shows about
 * them (page count, paper style, whether a PDF backs them).
 *
 * Nothing here invents data. Page count and paper style come from what the note's own thumbnail
 * render recorded ([Editor.noteMeta]); when a note has never been rendered those stay null and the
 * card simply shows less.
 */
enum class StudioKind { NOTE, CANVAS, PDF }

/** One note/canvas/PDF as the library screens show it. */
data class StudioItem(
    val entry: BrowseEntry,
    val kind: StudioKind,
    val folderDocId: String,
    val folderName: String?,
    val pages: Int?,
    val pattern: String?,
    val pdfBacked: Boolean,
    val isPinned: Boolean = false,
) {
    val uri: String get() = entry.documentUri
    val name: String get() = entry.name
    val modified: Long get() = entry.modified
    val size: Long get() = entry.size
    val isFavorite: Boolean get() = entry.isFavorite

    /** The note's title without its document extension. */
    val title: String
        get() = when (kind) {
            StudioKind.CANVAS -> com.pnotes.core.util.DocumentKind.stripSuffix(name)
            StudioKind.PDF -> name.removeSuffix(".pdf").removeSuffix(".PDF")
            StudioKind.NOTE -> com.pnotes.core.util.DocumentKind.stripSuffix(name)
        }

    /** The paper tag on the cover: the note's real ruling, or its document kind. */
    val paperTag: String
        get() = when {
            kind == StudioKind.CANVAS -> "CANVAS"
            kind == StudioKind.PDF -> "PDF"
            pdfBacked -> "ANNOTATED PDF"
            else -> paperLabel(pattern)
        }

    /** "42 pgs" when the note has been rendered, else null — never a made-up number. */
    val pagesLabel: String?
        get() = pages?.let { "$it ${if (it == 1) "pg" else "pgs"}" }
}

/** The reference's four cover tags, plus the honest fallbacks for what it does not name. */
internal fun paperLabel(pattern: String?): String = when (pattern?.lowercase()) {
    "grid" -> "GRID"
    "lines" -> "RULED"
    "dots" -> "DOTS"
    "cornell" -> "CORNELL"
    "two_column" -> "2-COLUMN"
    "none" -> "BLANK"
    else -> "HANDWRITTEN"
}

/** Human wording for a paper style, used where a sentence reads better than a tag. */
internal fun paperWords(pattern: String?): String = when (pattern?.lowercase()) {
    "grid" -> "Grid paper"
    "lines" -> "Ruled paper"
    "dots" -> "Dot grid"
    "cornell" -> "Cornell paper"
    "two_column" -> "Two-column paper"
    "none" -> "Plain paper"
    else -> "Handwriting"
}

/** A folder as the sidebar and the folder screen show it, with its real contents counted. */
data class StudioFolder(
    val docId: String,
    val name: String,
    val uri: String,
    val notes: Int = 0,
    val folders: Int = 0,
    val pages: Int? = null,
    val modified: Long = 0,
) {
    val label: String get() = if (notes == 1) "1 note" else "$notes notes"
}

/** The bands the library can be narrowed to. Every one of them maps to something the app knows. */
enum class StudioFilter(val label: String) {
    ALL("All Types"),
    GRID("Grid Paper"),
    RULED("Ruled Paper"),
    DOTS("Dot Grid"),
    CORNELL("Cornell"),
    PDF("PDF Annotations"),
    CANVAS("Freeform Canvas"),
    FAVORITES("Favorites"),
}

/** How the grid orders notes — the reference's three sort fields. */
enum class StudioSort(val label: String) {
    MODIFIED("Modified"),
    TITLE("Title"),
    PAGES("Pages"),
}

internal fun studioKindOf(name: String): StudioKind = when {
    isCanvasDocument(name) -> StudioKind.CANVAS
    isPdfDocument(name) -> StudioKind.PDF
    else -> StudioKind.NOTE
}

/** How many entries of each kind/filter exist, for the chip labels. */
internal fun countFor(items: List<StudioItem>, filter: StudioFilter): Int = items.count { matches(it, filter) }

internal fun matches(item: StudioItem, filter: StudioFilter): Boolean = when (filter) {
    StudioFilter.ALL -> true
    StudioFilter.GRID -> item.pattern?.lowercase() == "grid"
    StudioFilter.RULED -> item.pattern?.lowercase() == "lines"
    StudioFilter.DOTS -> item.pattern?.lowercase() == "dots"
    StudioFilter.CORNELL -> item.pattern?.lowercase() == "cornell"
    StudioFilter.PDF -> item.kind == StudioKind.PDF || item.pdfBacked
    StudioFilter.CANVAS -> item.kind == StudioKind.CANVAS
    StudioFilter.FAVORITES -> item.isFavorite
}

internal fun sortItems(items: List<StudioItem>, sort: StudioSort, descending: Boolean = true): List<StudioItem> {
    val sorted = when (sort) {
        StudioSort.MODIFIED -> items.sortedWith(compareByDescending<StudioItem> { it.isPinned }.thenByDescending { it.modified })
        StudioSort.TITLE -> items.sortedWith(compareByDescending<StudioItem> { it.isPinned }.thenBy { it.title.lowercase() })
        StudioSort.PAGES -> items.sortedWith(compareByDescending<StudioItem> { it.isPinned }.thenByDescending { it.pages ?: -1 })
    }
    return if (descending) sorted else sorted.reversed()
}

/**
 * Load the notes directly inside [docId] (or the whole vault below it when [recursive]), tagged
 * with the folder they live in and whatever facts their cached tile recorded. Runs on IO: it
 * queries the storage provider per folder and reads a small sidecar per note.
 */
suspend fun loadStudioItems(
    editor: Editor,
    treeUri: String,
    docId: String,
    recursive: Boolean,
    folderNames: Map<String, String>,
): List<StudioItem> = withContext(Dispatchers.IO) {
    val entries = if (recursive) editor.listAllNotes(treeUri, docId) else editor.browseChildren(treeUri, docId)
        .filterNot { it.isDir }
    entries.map { entry ->
        val meta = runCatching { editor.noteMeta(entry.documentUri) }.getOrNull()
        StudioItem(
            entry = entry,
            kind = studioKindOf(entry.name),
            folderDocId = entry.parentDocId,
            folderName = folderNames[entry.parentDocId],
            pages = meta?.pages,
            pattern = meta?.pattern,
            pdfBacked = meta?.pdfBacked ?: false,
            isPinned = editor.isPinned(entry.documentUri),
        )
    }
}

/** The folders directly inside [docId], each with its real note/sub-folder counts. */
suspend fun loadStudioFolders(
    editor: Editor,
    treeUri: String,
    docId: String,
): List<StudioFolder> = withContext(Dispatchers.IO) {
    editor.browseChildren(treeUri, docId).filter { it.isDir }.map { dir ->
        val childDocId = editor.browseDocId(dir.documentUri)
        val counts = runCatching { editor.countFolderContents(treeUri, childDocId) }
            .getOrDefault(Editor.FolderCountInfo(0, 0))
        StudioFolder(
            docId = childDocId,
            name = dir.name,
            uri = dir.documentUri,
            notes = counts.notes,
            folders = counts.folders,
            pages = runCatching { pagesUnder(editor, treeUri, childDocId) }.getOrNull(),
            modified = dir.modified,
        )
    }
}

/** Total rendered page count beneath [docId], or null when nothing under it has been rendered. */
private fun pagesUnder(editor: Editor, treeUri: String, docId: String): Int? {
    var total = 0
    var known = false
    for (e in editor.listAllNotes(treeUri, docId)) {
        val meta = editor.noteMeta(e.documentUri) ?: continue
        total += meta.pages
        known = true
    }
    return if (known) total else null
}

/** The cover gradient a note falls back to before its real preview has been rendered. */
internal fun coverAccentFor(item: StudioItem, dark: Boolean): Color = when {
    item.kind == StudioKind.CANVAS -> if (dark) Color(0xFF065F46) else Color(0xFF065F46)
    item.kind == StudioKind.PDF || item.pdfBacked -> if (dark) Color(0xFF581C87) else Color(0xFF4C1D95)
    else -> when (item.pattern?.lowercase()) {
        "grid" -> if (dark) Color(0xFF27272A) else Color(0xFF1E293B)
        "dots" -> if (dark) Color(0xFF7C2D12) else Color(0xFF9A3412)
        "lines" -> if (dark) Color(0xFF065F46) else Color(0xFF065F46)
        "cornell" -> if (dark) Color(0xFF581C87) else Color(0xFF4C1D95)
        else -> if (dark) Color(0xFF27272A) else Color(0xFF334155)
    }
}

/** The colour of the paper tag on a cover. */
internal fun badgeFor(item: StudioItem, t: StudioTokens): StudioBadgeSpec {
    val label = item.paperTag
    val bg = when {
        item.kind == StudioKind.CANVAS -> if (t.isDark) Color(0xFF047857) else Color(0xFF059669)
        item.kind == StudioKind.PDF || item.pdfBacked -> if (t.isDark) Color(0xFF7C3AED) else Color(0xFF7C3AED)
        else -> when (item.pattern?.lowercase()) {
            "grid" -> t.badgeGrid
            "lines" -> t.badgeRuled
            "dots" -> t.badgeDots
            "cornell" -> t.badgeCornell
            else -> if (t.isDark) Color(0xFF334155) else Color(0xFF475569)
        }
    }
    return StudioBadgeSpec(label, bg)
}

/** "10m ago" / "Yesterday" / "Mar 4" — the relative stamp the cards carry. */
internal fun relativeStamp(timeMillis: Long): String {
    if (timeMillis <= 0) return "Not dated"
    val diff = System.currentTimeMillis() - timeMillis
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        diff < 60_000L -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(timeMillis))
    }
}

/** "2 minutes ago" / "10 minutes ago" for the header's last-updated note. */
internal fun relativeLong(timeMillis: Long): String {
    if (timeMillis <= 0) return "not yet saved"
    val diff = System.currentTimeMillis() - timeMillis
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        diff < 60_000L -> "just now"
        minutes < 60 -> "$minutes minute${if (minutes == 1L) "" else "s"} ago"
        hours < 24 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        days == 1L -> "yesterday"
        else -> "$days days ago"
    }
}

/** A weight shorthand for the reference's type ramp. */
internal val StudioSemiBold = FontWeight.SemiBold
internal val StudioBold = FontWeight.Bold
