package com.pnotes.ui

/**
 * What the import prompt shows and does, in one place.
 *
 * The app has two backstage surfaces — the studio library (the home screen) and the classic
 * explorer — and either can be handed a picked file by the system picker. Both must then ask the
 * user for a name and commit it. When that prompt lived in only one of them, picking a PDF from
 * the other one silently did nothing: the file was remembered, but no prompt ever appeared and no
 * note was ever created.
 *
 * These helpers hold the shared parts — the title, the button label and the folder an import lands
 * in — so the two surfaces cannot drift apart, and so the behaviour is testable without a device.
 */
object ImportPrompt {

    /** The title of the name prompt for a pending import of [kind]. */
    fun title(kind: ImportKind): String = when (kind) {
        ImportKind.PDF -> "Import PDF"
        ImportKind.OPEN -> "Import Note"
    }

    /** The button that commits the import. */
    const val CONFIRM_LABEL = "Save"

    /** The placeholder in the name field. */
    const val PLACEHOLDER = "Note title"

    /**
     * The folder an import lands in: the folder the user is looking at, or the library root when
     * they are looking at everything.
     */
    fun targetFolder(activeFolderDocId: String?, rootDocId: String): String =
        activeFolderDocId?.takeIf { it.isNotBlank() } ?: rootDocId
}
