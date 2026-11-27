package com.pnotes.core.cache

import com.pnotes.core.infinite.InfiniteDocument
import com.pnotes.core.model.Document
import com.pnotes.core.model.deepCopy
import com.pnotes.core.pal.TextMeasurer
import java.util.LinkedHashMap

/**
 * Fast, thread-safe in-memory cache for parsed notes and infinite canvases.
 * Avoids redundant SAF disk I/O, zip decompression, and JSON parsing when
 * opening or switching between recent documents.
 */
object DocumentCache {
    private const val MAX_ENTRIES = 16

    private data class CachedPaged(
        val doc: Document,
        val fileBytes: Long,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private data class CachedCanvas(
        val doc: InfiniteDocument,
        val fileBytes: Long,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val pagedCache = object : LinkedHashMap<String, CachedPaged>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedPaged>?): Boolean = size > MAX_ENTRIES
    }

    private val canvasCache = object : LinkedHashMap<String, CachedCanvas>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedCanvas>?): Boolean = size > MAX_ENTRIES
    }

    fun getPaged(uri: String, fileBytes: Long, measurer: TextMeasurer): Document? {
        synchronized(pagedCache) {
            val entry = pagedCache[uri] ?: return null
            if (fileBytes > 0 && entry.fileBytes > 0 && entry.fileBytes != fileBytes) {
                pagedCache.remove(uri)
                return null
            }
            return entry.doc.deepCopy(measurer)
        }
    }

    fun putPaged(uri: String, doc: Document, fileBytes: Long, measurer: TextMeasurer? = null) {
        synchronized(pagedCache) {
            val toStore = if (measurer != null) doc.deepCopy(measurer) else doc
            pagedCache[uri] = CachedPaged(toStore, fileBytes)
        }
    }

    fun getCanvas(uri: String, fileBytes: Long): InfiniteDocument? {
        synchronized(canvasCache) {
            val entry = canvasCache[uri] ?: return null
            if (fileBytes > 0 && entry.fileBytes > 0 && entry.fileBytes != fileBytes) {
                canvasCache.remove(uri)
                return null
            }
            return entry.doc
        }
    }

    fun putCanvas(uri: String, doc: InfiniteDocument, fileBytes: Long) {
        synchronized(canvasCache) {
            canvasCache[uri] = CachedCanvas(doc, fileBytes)
        }
    }

    fun evict(uri: String) {
        synchronized(pagedCache) { pagedCache.remove(uri) }
        synchronized(canvasCache) { canvasCache.remove(uri) }
    }

    fun clear() {
        synchronized(pagedCache) { pagedCache.clear() }
        synchronized(canvasCache) { canvasCache.clear() }
    }
}
