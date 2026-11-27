package com.pnotes.core.cache

import com.pnotes.core.FakeTextMeasurer
import com.pnotes.core.infinite.InfiniteDocument
import com.pnotes.core.model.Document
import com.pnotes.core.model.Page
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DocumentCacheTest {

    private val fakeMeasurer = FakeTextMeasurer()

    @Before
    fun setUp() {
        DocumentCache.clear()
    }

    @After
    fun tearDown() {
        DocumentCache.clear()
    }

    @Test
    fun pagedCacheStoreAndRetrieveHit() {
        val doc = Document()
        doc.pages.add(Page(800.0, 1000.0))
        val uri = "content://com.pnotes.provider/document/123"

        DocumentCache.putPaged(uri, doc, 5000L, fakeMeasurer)

        val retrieved = DocumentCache.getPaged(uri, 5000L, fakeMeasurer)
        assertNotNull(retrieved)
        assertEquals(1, retrieved!!.pages.size)
        // Ensure defensive copy was made so modifying retrieved doesn't mutate cache
        assertNotSame(doc, retrieved)
    }

    @Test
    fun pagedCacheInvalidatesOnFileSizeBytesMismatch() {
        val doc = Document()
        val uri = "content://com.pnotes.provider/document/123"

        DocumentCache.putPaged(uri, doc, 5000L, fakeMeasurer)

        // Requesting with changed file size should evict and return null
        val retrieved = DocumentCache.getPaged(uri, 6000L, fakeMeasurer)
        assertNull(retrieved)

        // Subsequent get with original size is also gone
        assertNull(DocumentCache.getPaged(uri, 5000L, fakeMeasurer))
    }

    @Test
    fun canvasCacheStoreAndRetrieve() {
        val doc = InfiniteDocument()
        val uri = "content://com.pnotes.provider/document/canvas_456"

        DocumentCache.putCanvas(uri, doc, 3500L)

        val retrieved = DocumentCache.getCanvas(uri, 3500L)
        assertNotNull(retrieved)
    }

    @Test
    fun evictRemovesSpecificUri() {
        val doc = Document()
        val uri = "content://com.pnotes.provider/document/to_delete"

        DocumentCache.putPaged(uri, doc, 2000L, fakeMeasurer)
        assertNotNull(DocumentCache.getPaged(uri, 2000L, fakeMeasurer))

        DocumentCache.evict(uri)
        assertNull(DocumentCache.getPaged(uri, 2000L, fakeMeasurer))
    }
}
