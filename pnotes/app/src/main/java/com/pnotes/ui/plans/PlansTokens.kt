package com.pnotes.ui.plans

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.pnotes.ui.theme.LocalPalette

object PlansTokens {
    val CoralAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)

    val CoralGlow: Color
        @Composable
        @ReadOnlyComposable
        get() = CoralAccent.copy(alpha = 0.22f)

    val CoralGlowLight: Color
        @Composable
        @ReadOnlyComposable
        get() = CoralAccent.copy(alpha = 0.14f)

    // Dark mode colors
    val DarkBaseBg = Color(0xFF000000)
    val DarkCardBg = Color(0xFF0C0C0C)
    val DarkCardBorder = Color(0xFF1E1E1E)
    val DarkTextPrimary = Color(0xFFF5F6F8)
    val DarkTextSecondary = Color(0xFFB5BBC5)
    val DarkTextMuted = Color(0xFFA7ADB8)
    val DarkButtonBg = Color(0xFF161616)
    val DarkButtonBorder = Color(0x33FFFFFF)
    val DarkReassuranceBg = Color(0xFF0A0A0A)

    // Light mode colors
    val LightBaseBg = Color(0xFFF8F9FA)
    val LightCardBg = Color(0xFFFFFFFF)
    val LightCardBorder = Color(0xFFE2E8F0)
    val LightTextPrimary = Color(0xFF151922)
    val LightTextSecondary = Color(0xFF64748B)
    val LightTextMuted = Color(0xFF94A3B8)
    val LightButtonBg = Color(0xFFF4F4F5)
    val LightButtonBorder = Color(0xFFE4E4E7)
    val LightReassuranceBg = Color(0xFFFFFFFF).copy(alpha = 0.92f)
}
