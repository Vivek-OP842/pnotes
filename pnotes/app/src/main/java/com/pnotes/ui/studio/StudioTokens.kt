package com.pnotes.ui.studio

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pnotes.ui.theme.LocalPalette

/**
 * Drop shadow matching the Figcraft prototype dark-mode physical drop shadow / backdrop effect:
 * offset [0, 6], spread 2dp, blur radius 6dp, color black with 25% opacity (0x40000000).
 */
fun Modifier.figcraftDarkShadow(
    shapeRadius: Dp,
    offsetY: Dp = 6.dp,
    spread: Dp = 2.dp,
    blurRadius: Dp = 6.dp,
    color: Color = Color(0x40000000),
): Modifier = this.drawBehind {
    val spreadPx = spread.toPx()
    val offsetYPx = offsetY.toPx()
    val cornerPx = shapeRadius.toPx()
    val blurPx = blurRadius.toPx()

    if (blurPx > 0f) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                this.color = color.toArgb()
                this.isAntiAlias = true
                this.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(
                -spreadPx,
                offsetYPx - spreadPx,
                size.width + spreadPx,
                size.height + offsetYPx + spreadPx,
                cornerPx + spreadPx,
                cornerPx + spreadPx,
                paint,
            )
        }
    } else {
        drawRoundRect(
            color = color,
            topLeft = Offset(-spreadPx, offsetYPx - spreadPx),
            size = Size(size.width + spreadPx * 2, size.height + spreadPx * 2),
            cornerRadius = CornerRadius(cornerPx + spreadPx, cornerPx + spreadPx),
        )
    }
}

/**
 * Colour tokens for the studio (home / folder) surfaces, matching the Figcraft prototype.
 * Dark and light frames and variables are both explicitly defined.
 */
@Immutable
data class StudioTokens(
    val bg: Color,
    val bgBrush: Brush,
    val topBar: Color,
    val topBarBorder: Color,
    val sidebar: Color,
    val sidebarBrush: Brush,
    val sidebarBorder: Color,
    val sidebarDivider: Color,
    val sidebarCard: Color,
    val sidebarCardBorder: Color,
    val mainDivider: Color,
    val card: Color,
    val cardBorder: Color,
    val cardHoverBorder: Color,
    val cardSoft: Color,
    val chip: Color,
    val chipText: Color,
    val chipBorder: Color,
    val searchField: Color,
    val searchBorder: Color,
    val searchChip: Color,
    val searchChipText: Color,
    val searchHint: Color,
    val iconButton: Color,
    val iconButtonBorder: Color,
    val segmentTrack: Color,
    val segmentThumb: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textTertiary: Color,
    val divider: Color,
    val primary: Color,
    val primaryDeep: Color,
    val primaryGlow: Color,
    val onPrimary: Color,
    val selectedRow: Color,
    val selectedRowText: Color,
    val selectedRing: Color,
    val selectedCountBg: Color,
    val selectedCountText: Color,
    val folderRowText: Color,
    val folderCountBg: Color,
    val folderCountText: Color,
    val folderAccents: List<Color>,
    val syncTrack: Color,
    val accentAmber: Color,
    val star: Color,
    val green: Color,
    val red: Color,
    val violet: Color,
    val badgeGrid: Color,
    val badgeRuled: Color,
    val badgeDots: Color,
    val badgeCornell: Color,
    val badgeHex: Color,
    val isDark: Boolean,
    val fabBg: Color,
) {
    val templateChip: Color get() = cardSoft
    val templateChipText: Color get() = textSecondary

    companion object {
        val Light = StudioTokens(
            bg = Color(0xFFF7F9FC),
            bgBrush = SolidColor(Color(0xFFF7F9FC)),
            topBar = Color(0xFFF7F9FC),
            topBarBorder = Color(0xFFE1E6EF),
            sidebar = Color(0xFFFFFFFF),
            sidebarBrush = SolidColor(Color(0xFFFFFFFF)),
            sidebarBorder = Color(0xFFE1E6EF),
            sidebarDivider = Color(0xFFE1E6EF),
            sidebarCard = Color(0xFFF3F6FA),
            sidebarCardBorder = Color(0xFFE1E6EF),
            mainDivider = Color(0xFFE1E6EF),
            card = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFE1E6EF),
            cardHoverBorder = Color(0xFFCBD5E1),
            cardSoft = Color(0xFFF3F6FA),
            chip = Color(0xFFF3F6FA),
            chipText = Color(0xFF525D70),
            chipBorder = Color(0xFFE1E6EF),
            searchField = Color(0xFFF3F6FA),
            searchBorder = Color(0xFFE1E6EF),
            searchChip = Color(0xFFFFFFFF),
            searchChipText = Color(0xFF151923),
            searchHint = Color(0xFF525D70),
            iconButton = Color(0xFFF3F6FA),
            iconButtonBorder = Color(0xFFE1E6EF),
            segmentTrack = Color(0xFFF3F6FA),
            segmentThumb = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF151923),
            textSecondary = Color(0xFF525D70),
            textMuted = Color(0xFF8993A3),
            textTertiary = Color(0xFF8993A3),
            divider = Color(0xFFE1E6EF),
            primary = Color(0xFF007AFF),
            primaryDeep = Color(0xFF0066D6),
            primaryGlow = Color(0x4D007AFF),
            onPrimary = Color(0xFFFFFFFF),
            selectedRow = Color(0x26007AFF),
            selectedRowText = Color(0xFF007AFF),
            selectedRing = Color(0x33007AFF),
            selectedCountBg = Color(0xFF007AFF),
            selectedCountText = Color(0xFFFFFFFF),
            folderRowText = Color(0xFF151923),
            folderCountBg = Color(0xFFF3F6FA),
            folderCountText = Color(0xFF525D70),
            folderAccents = listOf(
                Color(0xFFFFB200), Color(0xFF007AFF), Color(0xFFE81E21),
                Color(0xFF10B981), Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF38BDF8),
            ),
            syncTrack = Color(0xFFDCE2EB),
            accentAmber = Color(0xFFFFB200),
            star = Color(0xFFFFB200),
            green = Color(0xFF16A34A),
            red = Color(0xFFE81E21),
            violet = Color(0xFF8B5CF6),
            badgeGrid = Color(0xFF007AFF),
            badgeRuled = Color(0xFF059669),
            badgeDots = Color(0xFFFFB200),
            badgeCornell = Color(0xFF7C3AED),
            badgeHex = Color(0xFFE81E21),
            isDark = false,
            fabBg = Color(0xFFFFB200),
        )

        /**
         * Exact Figcraft dark-mode background gradient extracted from Frame 104 (#paint1):
         * Linear gradient from (614.054, 663.615) to (-420.598, 652.722) on 1536x1024 canvas.
         * Stop 0: #000000 (Pure Black), Stop 1: #8E11FF (Vibrant Electric Purple).
         */
        val DarkBgBrush: Brush = object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val w = if (size.width > 0f) size.width else 1536f
                val h = if (size.height > 0f) size.height else 1024f
                return LinearGradientShader(
                    from = Offset(614.054f / 1536f * w, 663.615f / 1024f * h),
                    to = Offset(-420.598f / 1536f * w, 652.722f / 1024f * h),
                    colors = listOf(Color(0xFF000000), Color(0xFF8E11FF)),
                    colorStops = listOf(0.0f, 1.0f),
                    tileMode = TileMode.Clamp,
                )
            }
        }

        /**
         * Exact Figcraft dark-mode sidebar gradient extracted from Frame 105 (#paint3):
         * Linear gradient from (76.738, 816.145) to (763.027, 845.172) on 312x1024 sidebar.
         * Stop 0: #10001E (Deep Midnight Violet), Stop 1: #0010001E (Transparent overlay).
         */
        val DarkSidebarBrush: Brush = object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val w = if (size.width > 0f) size.width else 312f
                val h = if (size.height > 0f) size.height else 1024f
                return LinearGradientShader(
                    from = Offset(76.738f / 312f * w, 816.145f / 1024f * h),
                    to = Offset(763.027f / 312f * w, 845.172f / 1024f * h),
                    colors = listOf(Color(0xFF10001E), Color(0x0010001E)),
                    colorStops = listOf(0.0f, 1.0f),
                    tileMode = TileMode.Clamp,
                )
            }
        }

        val Dark = StudioTokens(
            bg = Color(0xFF000000),
            bgBrush = DarkBgBrush,
            topBar = Color.Transparent,
            topBarBorder = Color(0x2194A3B8),
            sidebar = Color(0xFF10001E),
            sidebarBrush = DarkSidebarBrush,
            sidebarBorder = Color(0x33000000),
            sidebarDivider = Color(0x2194A3B8),
            sidebarCard = Color(0xFF15151A),
            sidebarCardBorder = Color(0x2194A3B8),
            mainDivider = Color(0x2194A3B8),
            card = Color(0xFF15151A),
            cardBorder = Color(0x2194A3B8),
            cardHoverBorder = Color(0x4094A3B8),
            cardSoft = Color(0xFF1B2230),
            chip = Color(0xFF15151A),
            chipText = Color(0xFFA6B0C0),
            chipBorder = Color(0x2194A3B8),
            searchField = Color(0xFF15151A),
            searchBorder = Color(0x2194A3B8),
            searchChip = Color(0xFF222A37),
            searchChipText = Color(0xFFF5F7FA),
            searchHint = Color(0xFFA6B0C0),
            iconButton = Color(0xFF1B2230),
            iconButtonBorder = Color(0x2194A3B8),
            segmentTrack = Color(0xFF1B2230),
            segmentThumb = Color(0xFF222A37),
            textPrimary = Color(0xFFF5F7FA),
            textSecondary = Color(0xFFA6B0C0),
            textMuted = Color(0xFF788497),
            textTertiary = Color(0xFF788497),
            divider = Color(0x2194A3B8),
            primary = Color(0xFF007AFF),
            primaryDeep = Color(0xFF0056B3),
            primaryGlow = Color(0x4D007AFF),
            onPrimary = Color(0xFFFFFFFF),
            selectedRow = Color(0x26007AFF),
            selectedRowText = Color(0xFFFFFFFF),
            selectedRing = Color(0x33007AFF),
            selectedCountBg = Color(0xFF007AFF),
            selectedCountText = Color(0xFFFFFFFF),
            folderRowText = Color(0xFFF5F7FA),
            folderCountBg = Color(0xFF1B2230),
            folderCountText = Color(0xFFA6B0C0),
            folderAccents = listOf(
                Color(0xFFFFB200), Color(0xFF007AFF), Color(0xFFE81E21),
                Color(0xFF10B981), Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF38BDF8),
            ),
            syncTrack = Color(0xFF2A3342),
            accentAmber = Color(0xFFFFB200),
            star = Color(0xFFFFB200),
            green = Color(0xFF22C55E),
            red = Color(0xFFE81E21),
            violet = Color(0xFFA78BFA),
            badgeGrid = Color(0xFF007AFF),
            badgeRuled = Color(0xFF10B981),
            badgeDots = Color(0xFFFFB200),
            badgeCornell = Color(0xFFA78BFA),
            badgeHex = Color(0xFFE81E21),
            isDark = true,
            fabBg = Color(0xFF998BEA),
        )
    }
}

@Composable
fun studioTokens(): StudioTokens {
    val dark = LocalPalette.current.isDark
    return remember(dark) { if (dark) StudioTokens.Dark else StudioTokens.Light }
}
