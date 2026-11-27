package com.pnotes.ui.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.pnotes.core.model.Orientation
import com.pnotes.core.model.PageMargins
import com.pnotes.core.model.PagePattern
import com.pnotes.core.model.PageSize
import com.pnotes.core.model.Rgba

enum class NoteCreationType {
    HANDWRITTEN,
    CANVAS,
}

enum class PaperStyleOption(val title: String, val pattern: PagePattern) {
    PLAIN("Plain", PagePattern.NONE),
    RULED("Ruled", PagePattern.LINES),
    LINED_WIDE("Lined (Wide)", PagePattern.LINED_WIDE),
    LINED_NARROW("Lined (Narrow)", PagePattern.LINED_NARROW),
    GRID("Grid", PagePattern.GRID),
    DOTTED("Dotted", PagePattern.DOTS),
    CORNELL("Cornell", PagePattern.CORNELL),
    TWO_COLUMN("Two column", PagePattern.TWO_COLUMN),
    CHECKLIST("Checklist", PagePattern.CHECKLIST),
    MUSIC("Music", PagePattern.MUSIC),
    STORYBOARD("Storyboard", PagePattern.STORYBOARD);

    companion object {
        val allOptions = entries.toList()
    }
}

enum class MarginOption(val title: String, val fraction: Double) {
    NONE("None", 0.0),
    NARROW("Narrow", 0.03),
    NORMAL("Normal", 0.05),
    WIDE("Wide", 0.08);

    fun toPageMargins(): PageMargins = when (this) {
        NONE -> PageMargins(0.0, 0.0, 0.0, 0.0)
        NARROW -> PageMargins(0.03, 0.03, 0.03, 0.03)
        NORMAL -> PageMargins(0.05, 0.05, 0.05, 0.05)
        WIDE -> PageMargins(0.08, 0.08, 0.08, 0.08)
    }

    companion object {
        val allOptions = entries.toList()
    }
}

data class PaperColorItem(
    val id: String,
    val name: String,
    val color: Color,
    val rgba: Rgba,
    val isDark: Boolean = false,
)

object PaperColorPalette {
    val items = listOf(
        PaperColorItem("warm_cream", "Warm Cream", Color(0xFFFFFCEB), Rgba(255, 252, 235, 255)),
        PaperColorItem("white", "Pure White", Color(0xFFFFFFFF), Rgba(255, 255, 255, 255)),
        PaperColorItem("light_gray", "Light Gray", Color(0xFFF4F6F8), Rgba(244, 246, 248, 255)),
        PaperColorItem("sand", "Sand", Color(0xFFF5EBE1), Rgba(245, 235, 225, 255)),
        PaperColorItem("soft_yellow", "Soft Yellow", Color(0xFFFEF9C3), Rgba(254, 249, 195, 255)),
        PaperColorItem("light_green", "Light Green", Color(0xFFE2F8D8), Rgba(226, 248, 216, 255)),
        PaperColorItem("mint", "Mint", Color(0xFFD1FAE5), Rgba(209, 250, 229, 255)),
        PaperColorItem("light_blue", "Light Blue", Color(0xFFE0F2FE), Rgba(224, 242, 254, 255)),
        PaperColorItem("lavender", "Lavender", Color(0xFFEDE9FE), Rgba(237, 233, 254, 255)),
        PaperColorItem("soft_pink", "Soft Pink", Color(0xFFFCE7F3), Rgba(252, 231, 243, 255)),
        PaperColorItem("dark_charcoal", "Dark Charcoal", Color(0xFF2B2B2B), Rgba(0x2B, 0x2B, 0x2B, 255), isDark = true),
        PaperColorItem("dark_navy", "Dark Navy", Color(0xFF283548), Rgba(40, 53, 72, 255), isDark = true),
    )
}

enum class ProceduralCoverType {
    NONE,
    MINIMAL,
    NOTES,
    CLASSIC,
    DARK,
    DIARY,
    VINTAGE,
    MARBLE,
    GRADIENT,
    ASSET_IMAGE,
}

data class CoverItem(
    val id: String,
    val name: String,
    val type: ProceduralCoverType,
    val assetPath: String? = null,
    val horizontalAssetPath: String? = null,
)

data class NewNoteConfiguration(
    val title: String = "",
    val noteType: NoteCreationType = NoteCreationType.HANDWRITTEN,
    val cover: CoverItem = com.pnotes.ui.covers.CoverRepository.defaultFeaturedCover,
    val paperStyle: PaperStyleOption = PaperStyleOption.PLAIN,
    val paperColor: PaperColorItem = PaperColorPalette.items[0],
    val pageSize: PageSize = PageSize.A4,
    val orientation: Orientation = Orientation.PORTRAIT,
    val margins: MarginOption = MarginOption.NONE,
    val showPageNumber: Boolean = false,
    val showDate: Boolean = true,
    val targetFolderDocId: String = "",
    val targetFolderName: String = "Pari Notes",
)
