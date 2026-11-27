package com.pnotes.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pnotes.core.util.DocumentKind

/**
 * TabManager manages the lifecycle of open tabs for an Editor instance.
 * Supports adding, closing, switching, reordering, and in-memory document caching.
 */
class TabManager(
    private val onSwitchTab: (targetTab: NoteTab, previousTab: NoteTab?) -> Unit,
    private val onCloseTabRequest: (tabToClose: NoteTab, remainingTabsCount: Int) -> Unit,
) {
    /** Reactive list of currently open tabs */
    val tabs = mutableStateListOf<NoteTab>()

    /** Currently active tab ID */
    var activeTabId by mutableStateOf<String?>(null)
        private set

    /** Fast in-memory document holder cache mapped by tab ID */
    private val documentCache = HashMap<String, TabDocumentHolder>()

    val activeTab: NoteTab?
        get() = tabs.find { it.id == activeTabId }

    val tabCount: Int
        get() = tabs.size

    val hasMultipleTabs: Boolean
        get() = tabs.size > 1

    /**
     * Cache an open document holder for a tab so switching away and back is instant.
     */
    fun cacheHolder(tabId: String, holder: TabDocumentHolder) {
        documentCache[tabId] = holder
    }

    /**
     * Retrieve a cached holder for a tab, if any.
     */
    fun getHolder(tabId: String): TabDocumentHolder? = documentCache[tabId]

    /**
     * Remove a cached holder when a tab is permanently closed.
     */
    fun removeHolder(tabId: String): TabDocumentHolder? = documentCache.remove(tabId)

    /**
     * Check if a tab with the given URI is already open.
     */
    fun findTabByUri(uri: String): NoteTab? {
        return tabs.find { it.uri != null && it.uri == uri }
    }

    /**
     * Register an existing or newly opened note as a tab and set it active.
     */
    fun registerTab(
        uri: String?,
        title: String,
        kind: DocumentKind,
        isDirty: Boolean = false,
        isAutosaved: Boolean = false,
        viewSnapshot: TabViewState? = null,
        tabId: String? = null,
    ): NoteTab {
        // If URI already exists, activate it instead of duplicating
        if (uri != null) {
            val existing = findTabByUri(uri)
            if (existing != null) {
                if (title.isNotBlank() && (existing.title.isBlank() || existing.title == "Untitled Note" || existing.title == "Untitled Canvas")) {
                    updateTab(existing.id, title = title)
                }
                selectTab(existing.id)
                return tabs.find { it.id == existing.id } ?: existing
            }
        }

        val newTab = NoteTab(
            id = tabId ?: java.util.UUID.randomUUID().toString(),
            uri = uri,
            title = title,
            kind = kind,
            isDirty = isDirty,
            isAutosaved = isAutosaved,
            viewSnapshot = viewSnapshot,
        )

        tabs.add(newTab)
        activeTabId = newTab.id
        return newTab
    }

    /**
     * Switch to a specific tab by its ID.
     */
    fun selectTab(tabId: String) {
        if (activeTabId == tabId) return
        val targetIndex = tabs.indexOfFirst { it.id == tabId }
        if (targetIndex < 0) return

        val prevTab = activeTab
        val targetTab = tabs[targetIndex]
        activeTabId = tabId

        onSwitchTab(targetTab, prevTab)
    }

    /**
     * Update dynamic state of a tab (e.g. title, dirty state, autosaved, view snapshot).
     */
    fun updateTab(
        tabId: String,
        title: String? = null,
        uri: String? = null,
        isDirty: Boolean? = null,
        isAutosaved: Boolean? = null,
        viewSnapshot: TabViewState? = null,
    ) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index >= 0) {
            val old = tabs[index]
            tabs[index] = old.copy(
                title = title ?: old.title,
                uri = if (uri != null) uri else old.uri,
                isDirty = isDirty ?: old.isDirty,
                isAutosaved = isAutosaved ?: old.isAutosaved,
                viewSnapshot = viewSnapshot ?: old.viewSnapshot,
                lastActiveTime = System.currentTimeMillis(),
            )
        }
    }

    /**
     * Close a tab by its ID. Triggers the onCloseTabRequest callback.
     */
    fun closeTab(tabId: String) {
        val tab = tabs.find { it.id == tabId } ?: return
        onCloseTabRequest(tab, tabs.size)
    }

    /**
     * Force remove a tab from the list and select the nearest adjacent tab.
     */
    fun removeTabDirect(tabId: String): NoteTab? {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index < 0) return null

        val wasActive = activeTabId == tabId
        val removed = tabs.removeAt(index)
        removeHolder(tabId)

        if (wasActive) {
            if (tabs.isNotEmpty()) {
                val nextIndex = (index - 1).coerceAtLeast(0).coerceAtMost(tabs.lastIndex)
                val nextTab = tabs[nextIndex]
                activeTabId = nextTab.id
                onSwitchTab(nextTab, removed)
            } else {
                activeTabId = null
            }
        }
        return removed
    }

    /**
     * Move / Reorder tabs (e.g., via drag and drop).
     */
    fun moveTab(fromIndex: Int, toIndex: Int) {
        if (fromIndex in tabs.indices && toIndex in tabs.indices && fromIndex != toIndex) {
            val item = tabs.removeAt(fromIndex)
            tabs.add(toIndex, item)
        }
    }

    /**
     * Close all other tabs except the specified one.
     */
    fun closeOtherTabs(tabId: String, canClose: (NoteTab) -> Boolean) {
        val toRemove = tabs.filter { it.id != tabId && canClose(it) }
        toRemove.forEach { closeTab(it.id) }
    }

    /**
     * Clear all tabs and cached documents.
     */
    fun clearAll() {
        tabs.clear()
        activeTabId = null
        documentCache.clear()
    }
}
