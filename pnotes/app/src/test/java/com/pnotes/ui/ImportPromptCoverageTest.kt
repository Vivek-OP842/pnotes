package com.pnotes.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The import prompt must exist on **every** backstage surface that can be handed a picked file.
 *
 * The bug this guards against: the studio library (the home screen) and the classic explorer are
 * two different surfaces, and the system picker hands the picked file to whichever is showing. The
 * prompt used to live only on the explorer, so picking a PDF from the studio remembered the file
 * and then did nothing at all — no prompt, no note, no error.
 *
 * So this is a check on the *sources*: every surface that can receive a pending import must
 * render it. It reads the files rather than the running UI, because that is the one thing that
 * can be checked on every build without a device.
 */
class ImportPromptCoverageTest {

    /** The backstage surfaces that render the library / explorer the picker returns to. */
    private val backstages = listOf(
        "src/main/java/com/pnotes/ui/studio/StudioBackstage.kt",
        "src/main/java/com/pnotes/ui/Backstage.kt",
    )

    private fun source(rel: String): String {
        // The test runs from the module directory (app/), so the paths above resolve directly.
        val f = File(rel)
        val file = if (f.exists()) f else File("..", rel)
        assertTrue("cannot find $rel (looked in ${f.absolutePath} and ${file.absolutePath})", file.exists())
        return file.readText()
    }

    @Test fun everyBackstageSurfaceRendersThePendingImportPrompt() {
        for (rel in backstages) {
            val text = source(rel)
            assertTrue(
                "$rel reads editor.pendingImport but never renders it — a picked file handed to this " +
                    "surface would be remembered and then silently ignored.",
                text.contains("pendingImport"),
            )
            assertTrue(
                "$rel does not commit the import (no commitImportAsync) — the prompt would ask for a " +
                    "name and then drop the file.",
                text.contains("commitImportAsync"),
            )
        }
    }

    @Test fun theTitleDistinguishesAPdfFromAnOpenedNote() {
        assertEquals("Import PDF", ImportPrompt.title(ImportKind.PDF))
        assertEquals("Import Note", ImportPrompt.title(ImportKind.OPEN))
    }

    @Test fun theImportLandsInTheFolderBeingBrowsed() {
        assertEquals("folder-1", ImportPrompt.targetFolder("folder-1", "root"))
        // Looking at everything: the library root is the target.
        assertEquals("root", ImportPrompt.targetFolder(null, "root"))
        assertEquals("root", ImportPrompt.targetFolder("", "root"))
    }

    @Test fun thePromptSavesRatherThanCreates() {
        assertEquals("Save", ImportPrompt.CONFIRM_LABEL)
    }
}
