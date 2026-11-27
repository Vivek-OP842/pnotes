package com.pnotes.platform

import com.pnotes.canvas.ViewOverrides
import com.pnotes.core.model.Document
import com.pnotes.core.util.DocumentKind
import com.pnotes.format.DocumentCodec
import com.pnotes.tabs.NoteTab
import com.pnotes.tabs.TabViewState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Persists the working session — the open document (including unsaved edits),
 * all open tabs metadata, plus the view state (zoom/scroll) — under a private
 * directory, so relaunching reopens the last note and tabs where the user left off.
 */
class SessionStore(private val dir: File, private val codec: DocumentCodec, private val pdfDir: File, private val imageDir: File) {

    private val docFile = File(dir, "document.pnote")
    private val meta = JsonStore(File(dir, "session.json"))
    private val tabsMeta = JsonStore(File(dir, "session_tabs.json"))

    /** A restored session: the document plus the view state to reapply. */
    class Snapshot(
        val document: Document,
        val zoom: Double,
        val scrollX: Double,
        val scrollY: Double,
        val zoomLocked: Boolean,
        val viewOverrides: ViewOverrides,
        val tabs: List<NoteTab> = emptyList(),
        val activeTabId: String? = null,
    )

    /**
     * Save [document], view state, and all open [tabs].
     */
    fun save(
        document: Document,
        zoom: Double,
        scrollX: Double,
        scrollY: Double,
        zoomLocked: Boolean,
        viewOverrides: ViewOverrides,
        writeDocument: Boolean,
        tabs: List<NoteTab> = emptyList(),
        activeTabId: String? = null,
    ) {
        runCatching {
            dir.mkdirs()
            if (writeDocument || !docFile.exists()) {
                val tmp = File(dir, "document.pnote.tmp")
                FileOutputStream(tmp).use { codec.write(document, it) }
                if (!tmp.renameTo(docFile)) {
                    docFile.delete()
                    tmp.renameTo(docFile)
                }
            }
            val m = JSONObject()
                .put("dirty", document.dirty)
                .put("zoom", zoom)
                .put("scrollX", scrollX)
                .put("scrollY", scrollY)
                .put("zoomLocked", zoomLocked)
            document.path?.let { m.put("path", it) }
            document.displayName?.let { m.put("displayName", it) }
            meta.write(ViewOverridesJson.write(m, viewOverrides))

            // Save multi-tab session state
            if (tabs.isNotEmpty()) {
                val tabsObj = JSONObject()
                tabsObj.put("activeTabId", activeTabId)
                val arr = JSONArray()
                for (t in tabs) {
                    val tobj = JSONObject()
                        .put("id", t.id)
                        .put("title", t.title)
                        .put("kind", t.kind.name)
                        .put("isDirty", t.isDirty)
                        .put("isAutosaved", t.isAutosaved)
                        .put("lastActiveTime", t.lastActiveTime)
                    t.uri?.let { tobj.put("uri", it) }
                    t.viewSnapshot?.let { vs ->
                        val vsobj = JSONObject()
                            .put("zoom", vs.zoom)
                            .put("scrollX", vs.scrollX)
                            .put("scrollY", vs.scrollY)
                            .put("pageIndex", vs.pageIndex)
                            .put("zoomLocked", vs.zoomLocked)
                        tobj.put("viewSnapshot", vsobj)
                    }
                    arr.put(tobj)
                }
                tabsObj.put("tabs", arr)
                tabsMeta.write(tabsObj)
            } else {
                tabsMeta.write(JSONObject())
            }
        }
    }

    /** Discard any saved session (document + sidecar), so the next launch restores nothing. */
    fun clear() {
        runCatching {
            docFile.delete()
            File(dir, "session.json").delete()
            File(dir, "session_tabs.json").delete()
        }
    }

    /** Load the saved session, or null when there is none or it can't be read. */
    fun load(): Snapshot? {
        if (!docFile.exists()) return null
        val doc = runCatching { FileInputStream(docFile).use { codec.read(it, pdfDir, imageDir) } }.getOrNull() ?: return null
        val m = meta.read()
        doc.path = m.optString("path", "").ifEmpty { null }
        doc.displayName = m.optString("displayName", "").ifEmpty { null }
        doc.dirty = m.optBoolean("dirty", false)

        val restoredTabs = ArrayList<NoteTab>()
        var restoredActiveId: String? = null
        val tm = tabsMeta.read()
        if (tm.has("tabs")) {
            restoredActiveId = tm.optString("activeTabId", "").ifEmpty { null }
            val arr = tm.optJSONArray("tabs")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val tobj = arr.optJSONObject(i) ?: continue
                    val id = tobj.optString("id", "")
                    if (id.isEmpty()) continue
                    val title = tobj.optString("title", "Untitled Note")
                    val kindName = tobj.optString("kind", DocumentKind.NOTE.name)
                    val kind = runCatching { DocumentKind.valueOf(kindName) }.getOrDefault(DocumentKind.NOTE)
                    val isDirty = tobj.optBoolean("isDirty", false)
                    val isAutosaved = tobj.optBoolean("isAutosaved", false)
                    val lastActiveTime = tobj.optLong("lastActiveTime", System.currentTimeMillis())
                    val uri = tobj.optString("uri", "").ifEmpty { null }
                    val vsobj = tobj.optJSONObject("viewSnapshot")
                    val vs = if (vsobj != null) {
                        TabViewState(
                            zoom = vsobj.optDouble("zoom", 1.0),
                            scrollX = vsobj.optDouble("scrollX", 0.0),
                            scrollY = vsobj.optDouble("scrollY", 0.0),
                            pageIndex = vsobj.optInt("pageIndex", 0),
                            zoomLocked = vsobj.optBoolean("zoomLocked", false),
                        )
                    } else null
                    restoredTabs.add(
                        NoteTab(
                            id = id,
                            uri = uri,
                            title = title,
                            kind = kind,
                            isDirty = isDirty,
                            isAutosaved = isAutosaved,
                            lastActiveTime = lastActiveTime,
                            viewSnapshot = vs,
                        )
                    )
                }
            }
        }

        return Snapshot(
            doc,
            m.optDouble("zoom", 0.0),
            m.optDouble("scrollX", 0.0),
            m.optDouble("scrollY", 0.0),
            m.optBoolean("zoomLocked", false),
            ViewOverridesJson.read(m),
            tabs = restoredTabs,
            activeTabId = restoredActiveId,
        )
    }
}
