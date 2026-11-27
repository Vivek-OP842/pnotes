package com.pnotes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.pnotes.core.model.Rgba

/** Convert a core [Rgba] to a Compose [Color]. */
fun Rgba.toComposeColor(): Color = Color(r, g, b, a)

/**
 * Page 1 solid opaque background color and surface tokens.
 * All UI surfaces are opaque; there is no blur or glass mode.
 */
val NormalModeSolidColorDark = Color(0xFF171D28)
val NormalModeSolidColorLight = Color(0xFFFFFFFF)

fun normalModeSolidColor(isDark: Boolean): Color = if (isDark) NormalModeSolidColorDark else NormalModeSolidColorLight
fun normalModePillBg(isDark: Boolean): Color = if (isDark) Color(0xFF222A37) else Color(0xFFFFFFFF)
fun normalModePillBorder(isDark: Boolean): Color = if (isDark) Color(0xFF94A3B8).copy(alpha = 0.16f) else Color(0xFFE1E6EF)

val NormalModeSolidColor = NormalModeSolidColorDark
val NormalModeSurface = Color(0xFF171D28)
val NormalModeSurfaceVariant = Color(0xFF1B2230)
val NormalModeSurfaceElevated = Color(0xFF222A37)
val NormalModeDarkPill = Color(0xFF222A37)
val NormalModePillBorder = Color(0xFF94A3B8).copy(alpha = 0.16f)
val NormalModeTextPrimary = Color(0xFFF5F7FA)
val NormalModeTextSecondary = Color(0xFFA6B0C0)
val NormalModeTextMuted = Color(0xFF788497)

/**
 * Slate Zinc / Titanium Charcoal Tokens (Linear & Raycast Style).
 * Replaces pure pitch-black (#000000 / #09090B / #18181B) with sophisticated charcoal and zinc tones.
 */
val SlateZincDarkPrimary = Color(0xFF0F0F12)
val SlateZincDarkSurface = Color(0xFF18181C)
val SlateZincSecondarySurface = Color(0xFF222228)
val SlateZincBorder = Color(0xFF2E2E36)
val SlateZincBorderLight = Color(0xFFE4E4E7)
val SlateZincTextPrimaryDark = Color(0xFFF4F4F6)
val SlateZincTextPrimaryLight = Color(0xFF0F0F12)
val SlateZincTextMuted = Color(0xFF9494A0)
val SlateZincTextSubtle = Color(0xFFA1A1AA)
val SlateZincCardBgLight = Color(0xFFF4F4F5)
val SlateZincKnobDark = Color(0xFF636370)

val LocalPalette = staticCompositionLocalOf { Palette.dark() }

@Composable
fun PNotesTheme(palette: Palette, content: @Composable () -> Unit) {
    PnotesTheme(palette, content)
}

@Composable
fun PnotesTheme(palette: Palette, content: @Composable () -> Unit) {
    val accent = palette.accent.toComposeColor()
    // The surfaceContainer* roles must come from the palette too: components read them
    // directly (menus draw surfaceContainer), and the darkColorScheme()/lightColorScheme()
    // baselines are purple-seeded constants that ignore the palette entirely.
    val scheme = if (palette.isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = palette.bg.toComposeColor(),
            secondary = accent,
            background = palette.bg.toComposeColor(),
            onBackground = palette.text.toComposeColor(),
            surface = palette.menuBg.toComposeColor(),
            onSurface = palette.text.toComposeColor(),
            surfaceVariant = palette.surface.toComposeColor(),
            onSurfaceVariant = palette.textDim.toComposeColor(),
            outline = palette.border.toComposeColor(),
            surfaceDim = palette.bg.toComposeColor(),
            surfaceBright = palette.surfaceHi.toComposeColor(),
            surfaceContainerLowest = palette.bg.toComposeColor(),
            surfaceContainerLow = palette.panel.toComposeColor(),
            surfaceContainer = palette.menuBg.toComposeColor(),
            surfaceContainerHigh = palette.surface.toComposeColor(),
            surfaceContainerHighest = palette.surfaceHi.toComposeColor(),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = accent,
            background = palette.bg.toComposeColor(),
            onBackground = palette.text.toComposeColor(),
            surface = palette.menuBg.toComposeColor(),
            onSurface = palette.text.toComposeColor(),
            surfaceVariant = palette.surface.toComposeColor(),
            onSurfaceVariant = palette.textDim.toComposeColor(),
            outline = palette.border.toComposeColor(),
            surfaceDim = palette.surface.toComposeColor(),
            surfaceBright = palette.paper.toComposeColor(),
            surfaceContainerLowest = palette.paper.toComposeColor(),
            surfaceContainerLow = palette.menuBg.toComposeColor(),
            surfaceContainer = palette.menuBg.toComposeColor(),
            surfaceContainerHigh = palette.panel.toComposeColor(),
            surfaceContainerHighest = palette.bg.toComposeColor(),
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
