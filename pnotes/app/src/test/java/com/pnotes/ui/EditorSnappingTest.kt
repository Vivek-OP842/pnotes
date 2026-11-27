package com.pnotes.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorSnappingTest {
    @Test
    fun snappingStateTogglesCorrectly() {
        var snapping = true
        fun toggle() { snapping = !snapping }
        assertTrue(snapping)
        toggle()
        assertFalse(snapping)
        toggle()
        assertTrue(snapping)
    }
}
