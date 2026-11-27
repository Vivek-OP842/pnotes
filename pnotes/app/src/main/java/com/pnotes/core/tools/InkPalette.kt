package com.pnotes.core.tools

import com.pnotes.core.model.Rgba

/** The preset ink colours (Apple System Palette). Ink colour is global across stroke tools. */
object InkPalette {
    val BLACK = Rgba(0x1F, 0x20, 0x24, 255)      // #1F2024
    val WHITE = Rgba(255, 255, 255, 255)         // #FFFFFF
    val YELLOW = Rgba(255, 229, 0, 255)          // #FFE500
    val GREEN = Rgba(45, 212, 85, 255)           // #2DD455
    val PINK = Rgba(244, 59, 224, 255)           // #F43BE0
    val BLUE = Rgba(10, 132, 255, 255)           // Apple System Blue #0A84FF
    val RED = Rgba(255, 69, 58, 255)             // Apple System Red #FF453A
    val PURPLE = Rgba(191, 90, 242, 255)         // Apple System Purple #BF5AF2
    val ORANGE = Rgba(255, 159, 10, 255)         // Apple System Orange #FF9F0A

    val DEFAULT = BLACK

    /** Full preset palette offered by the colour controls; also the default toolbar swatches. */
    val presets = listOf(BLACK, WHITE, YELLOW, GREEN, PINK, BLUE, RED, PURPLE, ORANGE)
}
