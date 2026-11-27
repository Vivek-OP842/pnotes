package com.pnotes.ui.theme

import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Universal Light Gradient Purple Palette for Sliders, SeekBars, and Toggle Switches.
 */
object PurpleTheme {
    // Clean monochrome neutral tones
    val LightLavender = Color(0xFFE4E4E7)
    val SoftPurple = Color(0xFFD4D4D8)
    val LightPurple = Color(0xFFA1A1AA)
    val MainPurple = Color(0xFF71717A)
    val DeepPurple = Color(0xFF3F3F46)

    val Gradient = listOf(Color(0xFFF4F4F5), Color(0xFFD4D4D8))
    val SoftGradient = listOf(Color(0xFFF4F4F5), Color(0xFFE4E4E7))
    val LightBarGradient = listOf(Color(0xFFFFFFFF), Color(0xFFE4E4E7), Color(0xFFD4D4D8))

    val BrushHorizontal = Brush.horizontalGradient(Gradient)
    val SoftBrushHorizontal = Brush.horizontalGradient(listOf(Color(0xFFF4F4F5), Color(0xFFE4E4E7)))

    fun accent(isDark: Boolean): Color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    fun accentFg(isDark: Boolean): Color = if (isDark) Color(0xFF09090B) else Color(0xFFFFFFFF)
    fun pillBg(isDark: Boolean): Color = if (isDark) Color(0x2EFFFFFF) else Color(0xFFF4F4F5)
    fun pillBorder(isDark: Boolean): Color = if (isDark) Color(0x66FFFFFF) else Color(0xFF09090B)

    fun activeGradient(isDark: Boolean): Brush = if (isDark) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.10f)))
    } else {
        // Apple PencilKit / iPadOS Native Soft-Tinted Matte Squircle
        Brush.linearGradient(listOf(Color(0xFFE5E5EA), Color(0xFFE2E2E8)))
    }

    fun activeBorder(isDark: Boolean): Brush = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.15f)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFD1D1D6), Color(0xFFC7C7CC)))
    }

    @Composable
    fun sliderColors(isDark: Boolean = LocalPalette.current.isDark): SliderColors {
        val accent = Color(0xFF007AFF) // Page 1 primary #007AFF
        val activeTrack = Color(0xFF007AFF)
        val thumb = Color(0xFF007AFF)
        val inactiveTrack = if (isDark) Color(0xFF2A3342) else Color(0xFFDCE2EB) // Page 1 track
        return SliderDefaults.colors(
            thumbColor = thumb,
            activeTrackColor = activeTrack,
            inactiveTrackColor = inactiveTrack,
            activeTickColor = accent,
            inactiveTickColor = inactiveTrack,
            disabledThumbColor = if (isDark) Color(0xFF48484A) else Color(0xFFC7C7CC),
            disabledActiveTrackColor = if (isDark) Color(0x22007AFF) else Color(0x10007AFF),
            disabledInactiveTrackColor = if (isDark) Color(0xFF1B2230) else Color(0xFFE1E6EF),
        )
    }

    @Composable
    fun switchColors(isDark: Boolean = LocalPalette.current.isDark): SwitchColors {
        val activeTrack = Color(0xFF007AFF) // Page 1 primary
        val activeThumb = Color.White
        val inactiveTrack = if (isDark) Color(0xFF2A3342) else Color(0xFFDCE2EB)
        return SwitchDefaults.colors(
            checkedThumbColor = activeThumb,
            checkedTrackColor = activeTrack,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = inactiveTrack,
            uncheckedBorderColor = Color.Transparent,
        )
    }

    @Composable
    fun checkboxColors(): androidx.compose.material3.CheckboxColors {
        val isDark = LocalPalette.current.isDark
        return androidx.compose.material3.CheckboxDefaults.colors(
            checkedColor = Color(0xFF007AFF),
            checkmarkColor = Color.White,
            uncheckedColor = if (isDark) Color(0xFF788497) else Color(0xFF8993A3),
        )
    }
}
