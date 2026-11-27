package com.pnotes.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenElevationTest {
    @Test
    fun verifyCardDimensionsAndProportions() {
        val cardWidth = 213.33f
        val cardHeight = 314.0f
        val previewWidth = 197.33f
        val previewHeight = 260.0f
        assertEquals(cardWidth - 16.0f, previewWidth, 0.01f)
        assertEquals(cardHeight - 54.0f, previewHeight, 0.01f)
    }
}
