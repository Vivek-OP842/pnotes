package com.pnotes.ui.studio

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioTokensTest {

    @Test
    fun verifyDarkModeTokensMatchFigcraft() {
        val dark = StudioTokens.Dark
        assertTrue("Tokens must be dark", dark.isDark)

        // Strict Dark Mode values from Figcraft Frame 104
        assertEquals(Color(0xFF000000), dark.bg)
        assertEquals(Color.Transparent, dark.topBar)
        assertEquals(Color(0xFF10001E), dark.sidebar)
        assertEquals(Color(0xFF15151A), dark.card)
        assertEquals(Color(0xFF1B2230), dark.cardSoft)
        assertEquals(Color(0xFF15151A), dark.searchField)
        assertEquals(Color(0xFF222A37), dark.searchChip)
        assertEquals(Color(0xFFF5F7FA), dark.textPrimary)
        assertEquals(Color(0xFFA6B0C0), dark.textSecondary)
        assertEquals(Color(0xFF788497), dark.textMuted)
        assertEquals(Color(0xFF788497), dark.textTertiary)
        assertEquals(Color(0x2194A3B8), dark.cardBorder)
        assertEquals(Color(0x2194A3B8), dark.divider)

        // Gradient brushes defined
        assertTrue("Dark must have a background gradient brush", dark.bgBrush != null)
        assertTrue("Dark must have a sidebar gradient brush", dark.sidebarBrush != null)

        // FAB is Lavender in Dark Mode (Node 196)
        assertEquals(Color(0xFF998BEA), dark.fabBg)
    }

    @Test
    fun verifyLightModeTokensMatchFigcraft() {
        val light = StudioTokens.Light
        assertTrue("Tokens must be light", !light.isDark)

        // Strict Light Mode values from Figcraft Frame 319
        assertEquals(Color(0xFFF7F9FC), light.bg)
        assertEquals(Color(0xFFF7F9FC), light.topBar)
        assertEquals(Color(0xFFFFFFFF), light.sidebar)
        assertEquals(Color(0xFFFFFFFF), light.card)
        assertEquals(Color(0xFFF3F6FA), light.cardSoft)
        assertEquals(Color(0xFFF3F6FA), light.searchField)
        assertEquals(Color(0xFFFFFFFF), light.searchChip)
        assertEquals(Color(0xFF151923), light.textPrimary)
        assertEquals(Color(0xFF525D70), light.textSecondary)
        assertEquals(Color(0xFF8993A3), light.textMuted)
        assertEquals(Color(0xFF8993A3), light.textTertiary)
        assertEquals(Color(0xFFE1E6EF), light.cardBorder)
        assertEquals(Color(0xFFE1E6EF), light.divider)

        // FAB is Amber in Light Mode (Node 1776)
        assertEquals(Color(0xFFFFB200), light.fabBg)
    }

    @Test
    fun verifyColorDifferentiationBetweenDarkAndLight() {
        val dark = StudioTokens.Dark
        val light = StudioTokens.Light

        // Strict enforcement: no lazy copying of colors between dark and light modes
        assertNotEquals("Backgrounds must differ", dark.bg, light.bg)
        assertNotEquals("Sidebars must differ", dark.sidebar, light.sidebar)
        assertNotEquals("Cards must differ", dark.card, light.card)
        assertNotEquals("Primary text must differ", dark.textPrimary, light.textPrimary)
        assertNotEquals("Secondary text must differ", dark.textSecondary, light.textSecondary)
        assertNotEquals("Borders must differ", dark.cardBorder, light.cardBorder)
        assertNotEquals("FAB colors must differ (Lavender vs Amber)", dark.fabBg, light.fabBg)
    }
}
