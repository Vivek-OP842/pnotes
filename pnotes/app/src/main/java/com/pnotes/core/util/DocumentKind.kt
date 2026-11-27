package com.pnotes.core.util

/**
 * The document types the app stores, and the file extension each uses.
 *
 * The extension is spelled once here rather than at the two dozen sites that used to hard-code
 * `.pnote`. That matters now there are two: every place that lists, names, renames or opens a file
 * has to agree on which extensions are documents, and a place that forgets one silently hides the
 * user's files from the explorer.
 */
enum class DocumentKind(val extension: String) {
    /** The paged notebook. */
    NOTE("pnotes"),

    /** The infinite canvas. */
    CANVAS("xcanvas"),
    ;

    /** The extension with its dot, as it appears at the end of a file name. */
    val suffix: String get() = ".$extension"

    companion object {
        private val NOTE_SUFFIXES = listOf(".pnotes", ".pnote", ".xnotes", ".xnote")
        private val CANVAS_SUFFIXES = listOf(".pcanvas", ".xcanvas")

        /** The kind [name] denotes, or null when it is not a document at all. */
        fun ofName(name: String): DocumentKind? {
            val lower = name.lowercase()
            return when {
                NOTE_SUFFIXES.any { lower.endsWith(it) } -> NOTE
                CANVAS_SUFFIXES.any { lower.endsWith(it) } -> CANVAS
                else -> null
            }
        }

        fun isDocument(name: String): Boolean = ofName(name) != null

        /** [name] without its document extension, or unchanged when it has none. */
        fun stripSuffix(name: String): String {
            val lower = name.lowercase()
            for (s in NOTE_SUFFIXES + CANVAS_SUFFIXES) {
                if (lower.endsWith(s)) return name.dropLast(s.length)
            }
            return name
        }

        /** [name] with [kind]'s extension, added only if it is not already the right one. */
        fun withSuffix(name: String, kind: DocumentKind): String {
            val current = ofName(name)
            return if (current == kind) name else "$name${kind.suffix}"
        }
    }
}
