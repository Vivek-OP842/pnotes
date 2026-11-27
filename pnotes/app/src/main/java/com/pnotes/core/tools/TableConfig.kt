package com.pnotes.core.tools

import com.pnotes.core.model.Rgba

/**
 * Creation preferences for the Table tool (imported from Star Notes architecture).
 */
data class TableConfig(
    val rows: Int = 3,
    val cols: Int = 3,
    val strokeWidth: Double = 2.0,
    val strokeRgba: Rgba = DEFAULT_STROKE_COLOR,
    val hasHeaderRow: Boolean = true,
    val hasHeaderCol: Boolean = false,
    val fillEnabled: Boolean = false,
    val fillRgba: Rgba = DEFAULT_FILL_COLOR,
) {
    companion object {
        val DEFAULT_STROKE_COLOR = Rgba(0x27, 0x27, 0x2A, 0xFF) // Zinc Charcoal
        val DEFAULT_FILL_COLOR = Rgba(0xFF, 0xF9, 0xDA, 0xDF) // Soft pink default
    }
}
