package com.pnotes.ui.theme

import com.pnotes.core.model.Rgba
import kotlin.math.abs

/**
 * The design tokens for one appearance (spec 11 §1). An [accent] overrides the
 * palette's shipped accent and survives a dark/light switch.
 */
data class Palette(
    val bg: Rgba,
    val panel: Rgba,
    val paper: Rgba,
    val paperBorder: Rgba,
    val accent: Rgba,
    val accentDim: Rgba,
    val border: Rgba,
    val text: Rgba,
    val textDim: Rgba,
    val surface: Rgba,
    val surfaceHi: Rgba,
    val menuBg: Rgba,
    val isDark: Boolean,
    /** True when the chrome is built from a Material 3 scheme (rounded card corners etc.). */
    val isMaterial: Boolean = false,
) {
    /** The accent lightened ~28% (hover highlights). */
    val accentLight: Rgba get() = ColorMath.lighten(accent, 0.28)

    fun accentAlpha(alpha: Int): Rgba = accent.withAlpha(alpha)

    companion object {
        val DEFAULT_ACCENT_DARK = Rgba(0, 122, 255)    // Page 1 Primary #007AFF
        val DEFAULT_ACCENT_LIGHT = Rgba(0, 122, 255)   // Page 1 Primary #007AFF
        val DEFAULT_ACCENT = DEFAULT_ACCENT_DARK
        val LEGACY_INDIGO = Rgba(99, 102, 241)
        val DISABLED_ICON = Rgba(120, 132, 151)        // Page 1 text3 #788497

        fun dark(accent: Rgba = DEFAULT_ACCENT_DARK): Palette {
            val effAccent = if (accent == LEGACY_INDIGO) DEFAULT_ACCENT_DARK else accent
            return Palette(
                bg = hex(0x15151A),          // Page 1 bg
                panel = hex(0x101620),       // Page 1 sidebar
                paper = hex(0x171D28),       // Page 1 card
                paperBorder = hex(0x2A3342), // Page 1 track
                accent = effAccent,
                accentDim = ColorMath.dim(effAccent),
                border = hex(0x283140),       // Page 1 border
                text = hex(0xF5F7FA),        // Page 1 text1
                textDim = hex(0xA6B0C0),     // Page 1 text2
                surface = hex(0x1B2230),     // Page 1 surface2
                surfaceHi = hex(0x222A37),   // Page 1 elevated / surface3
                menuBg = hex(0x171D28),      // Page 1 card
                isDark = true,
            )
        }

        fun light(accent: Rgba = DEFAULT_ACCENT_LIGHT): Palette {
            val effAccent = if (accent == DEFAULT_ACCENT || accent == DEFAULT_ACCENT_DARK || accent == LEGACY_INDIGO) {
                DEFAULT_ACCENT_LIGHT
            } else {
                accent
            }
            return Palette(
                bg = hex(0xF7F9FC),          // Page 1 bg light
                panel = hex(0xFFFFFF),       // Page 1 sidebar light
                paper = hex(0xFFFFFF),
                paperBorder = hex(0xE1E6EF), // Page 1 border light
                accent = effAccent,
                accentDim = ColorMath.dim(effAccent),
                border = hex(0xE1E6EF),
                text = hex(0x151923),        // Page 1 text1 light
                textDim = hex(0x525D70),     // Page 1 text2 light
                surface = hex(0xF3F6FA),     // Page 1 surface2 light
                surfaceHi = hex(0xFFFFFF),
                menuBg = hex(0xFFFFFF),
                isDark = false,
            )
        }

        /**
         * OLED: pitch-black ([bg]/[panel]/[paper]/[menuBg] = #000000) for power saving
         * and true black on OLED panels. Counts as a dark variant ([isDark] = true).
         * Big surfaces are pure black; small controls keep a micro-lift so tap targets
         * stay visible, and the page is located only by a faint [paperBorder].
         */
        fun oled(accent: Rgba = DEFAULT_ACCENT_DARK): Palette {
            val effAccent = if (accent == LEGACY_INDIGO) DEFAULT_ACCENT_DARK else accent
            return Palette(
                bg = hex(0x000000),
                panel = hex(0x000000),
                paper = hex(0x000000),
                paperBorder = hex(0x242424),
                accent = effAccent,
                accentDim = ColorMath.dim(effAccent),
                border = hex(0x1E1E1E),
                text = hex(0xc8c8c8),
                textDim = hex(0x6f6f6f),
                surface = hex(0x0C0C0C),
                surfaceHi = hex(0x121212),
                menuBg = hex(0x000000),
                isDark = true,
            )
        }

        fun forAppearance(appearance: String, accent: Rgba): Palette = when (appearance) {
            "light" -> light(accent)
            "oled" -> oled(accent)
            else -> dark(accent)
        }

        /** Build the full chrome from a Material 3 scheme's tonal surfaces. */
        fun material(appearance: String, m: MaterialColors): Palette = when (appearance) {
            "light" -> materialLight(m)
            "oled" -> materialOled(m)
            else -> materialDark(m)
        }

        /** Light sits a few tone steps below white so panels and the backstage keep visible
         *  depth against white paper instead of washing out near the top of the ladder. */
        fun materialLight(m: MaterialColors): Palette = Palette(
            bg = m.surfaceContainerHighest,
            panel = m.surfaceContainerHigh,
            paper = m.surfaceContainerLowest,
            paperBorder = m.outline,
            accent = m.primary,
            accentDim = ColorMath.dim(m.primary),
            // Raw outlineVariant reads too hard against the chrome; sink it a third of the way in.
            border = ColorMath.mix(m.outlineVariant, m.surfaceContainer, 0.35),
            text = m.onSurface,
            textDim = m.onSurfaceVariant,
            surface = m.surfaceDim,
            surfaceHi = ColorMath.darken(m.surfaceDim, 0.06),
            menuBg = m.surfaceContainer,
            isDark = false,
            isMaterial = true,
        )

        fun materialDark(m: MaterialColors): Palette = Palette(
            bg = m.surfaceContainerLowest,
            panel = m.surface,
            paper = m.surfaceContainer,
            paperBorder = m.outlineVariant,
            accent = m.primary,
            accentDim = ColorMath.dim(m.primary),
            // Raw outlineVariant reads too hard against the chrome; sink it a third of the way in.
            border = ColorMath.mix(m.outlineVariant, m.surfaceContainerLowest, 0.35),
            text = m.onSurface,
            textDim = m.onSurfaceVariant,
            surface = m.surfaceContainerHigh,
            surfaceHi = m.surfaceContainerHighest,
            menuBg = m.surfaceContainerLow,
            isDark = true,
            isMaterial = true,
        )

        fun materialOled(m: MaterialColors): Palette {
            val black = hex(0x000000)
            return materialDark(m).copy(
                bg = black,
                panel = black,
                paper = black,
                menuBg = black,
                surface = m.surfaceContainerLow,
                surfaceHi = m.surfaceContainer,
            )
        }

        /** Theme palette for pnotes. */
        fun ios(dark: Boolean): Palette = if (dark) dark() else light()

        private fun hex(rgb: Int): Rgba =
            Rgba((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, 255)
    }
}

/** Small pure colour math for accent derivations (spec 11 §1). */
object ColorMath {
    /** ACCENT_DIM = the accent's hue at 72% saturation and 55% value. */
    fun dim(c: Rgba): Rgba {
        val hsv = rgbToHsv(c)
        return hsvToRgb(hsv[0], 0.72, 0.55, c.a)
    }

    /**
     * Cap an accent's perceived luminance for the light theme. Colours brighter
     * than [maxLuminance] are scaled down in place (hue and saturation kept),
     * so a bright yellow deepens to gold; colours already dark enough pass through.
     */
    fun darkenForLight(c: Rgba, maxLuminance: Double = 0.35): Rgba {
        val lum = (0.299 * c.r + 0.587 * c.g + 0.114 * c.b) / 255.0
        if (lum <= maxLuminance) return c
        val k = maxLuminance / lum
        fun ch(v: Int) = (v * k).toInt().coerceIn(0, 255)
        return Rgba(ch(c.r), ch(c.g), ch(c.b), c.a)
    }

    /** Lighten toward white by [amount] (0..1). */
    fun lighten(c: Rgba, amount: Double): Rgba {
        val a = amount.coerceIn(0.0, 1.0)
        fun mix(v: Int) = (v + (255 - v) * a).toInt().coerceIn(0, 255)
        return Rgba(mix(c.r), mix(c.g), mix(c.b), c.a)
    }

    /** Darken toward black by [amount] (0..1). */
    fun darken(c: Rgba, amount: Double): Rgba {
        val a = amount.coerceIn(0.0, 1.0)
        fun mix(v: Int) = (v * (1 - a)).toInt().coerceIn(0, 255)
        return Rgba(mix(c.r), mix(c.g), mix(c.b), c.a)
    }

    /** Linear mix of [a] toward [b] by [t] (0..1). */
    fun mix(a: Rgba, b: Rgba, t: Double): Rgba {
        val k = t.coerceIn(0.0, 1.0)
        fun ch(x: Int, y: Int) = (x + (y - x) * k).toInt().coerceIn(0, 255)
        return Rgba(ch(a.r, b.r), ch(a.g, b.g), ch(a.b, b.b), ch(a.a, b.a))
    }

    fun rgbToHsv(c: Rgba): DoubleArray {
        val r = c.r / 255.0
        val g = c.g / 255.0
        val b = c.b / 255.0
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        var h = when {
            delta < 1e-9 -> 0.0
            max == r -> 60.0 * (((g - b) / delta) % 6.0)
            max == g -> 60.0 * (((b - r) / delta) + 2.0)
            else -> 60.0 * (((r - g) / delta) + 4.0)
        }
        if (h < 0) h += 360.0
        val s = if (max < 1e-9) 0.0 else delta / max
        return doubleArrayOf(h, s, max)
    }

    fun hsvToRgb(h: Double, s: Double, v: Double, alpha: Int = 255): Rgba {
        val c = v * s
        val x = c * (1 - abs((h / 60.0) % 2 - 1))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 60 -> Triple(c, x, 0.0)
            h < 120 -> Triple(x, c, 0.0)
            h < 180 -> Triple(0.0, c, x)
            h < 240 -> Triple(0.0, x, c)
            h < 300 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        fun ch(v0: Double) = ((v0 + m) * 255).toInt().coerceIn(0, 255)
        return Rgba(ch(r1), ch(g1), ch(b1), alpha)
    }
}
