package com.pnotes.ui.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared studio primitives styled after the final React workspace. Screens own data/state; this
 * file owns chrome only.
 */

/** The React primary CTA gradient: linear-gradient(124.64deg, #38bdf8 0%, … #0284c7 100%). */
@Composable
internal fun primaryBrush(t: StudioTokens): Brush = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFF38BDF8),
        0.3438f to Color(0xFF2FA9F0),
        0.6656f to Color(0xFF01A6F5),
        1.0f to Color(0xFF0284C7),
    ),
    start = Offset.Zero,
    end = Offset.Infinite,
)

/** Soft secondary button — header "Select" / "Import PDF". */
@Composable
internal fun StudioSoftButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 34.dp,
    iconSize: Dp = 14.dp,
    fontSize: Int = 13,
    fontWeight: FontWeight = FontWeight.SemiBold,
    enabled: Boolean = true,
    radius: Dp = 20.dp,
) {
    val t = studioTokens()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "studioSoftButton",
    )
    Row(
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(radius))
            .background(t.iconButton)
            .border(1.dp, t.iconButtonBorder, RoundedCornerShape(radius))
            .alphaIf(!enabled, 0.5f)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = t.textSecondary, modifier = Modifier.size(iconSize))
        Text(
            label,
            color = t.textSecondary,
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Filled accent button — "New Notebook", "Create Folder", "Open Canvas". */
@Composable
internal fun StudioPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 44.dp,
    iconSize: Dp = 16.dp,
    fontSize: Int = 14,
    shortcut: String? = null,
    enabled: Boolean = true,
    radius: Dp = 12.dp,
    foreground: Color? = null,
) {
    val t = studioTokens()
    val fg = foreground ?: t.onPrimary
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "studioPrimaryButton",
    )
    Row(
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(radius))
            .background(primaryBrush(t))
            .alphaIf(!enabled, 0.55f)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(7.dp))
        }
        Text(
            label,
            color = fg,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (shortcut != null) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(shortcut, color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Circular header icon button (36dp) / view-toggle half / mini card actions. */
@Composable
internal fun StudioIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color,
    bg: Color,
    size: Dp = 36.dp,
    iconSize: Dp = 17.dp,
    radius: Dp = size / 2,
    borderColor: Color? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(radius)) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/**
 * Filter tab — "All (28)", "Notes", "Favorites": 32dp tall, radius 16, count pill on the right.
 */
@Composable
internal fun StudioFilterTab(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = studioTokens()
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) t.primary else t.chip)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, t.chipBorder, RoundedCornerShape(16.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            color = if (selected) t.onPrimary else t.textMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
        if (count != null) {
            Box(
                Modifier
                    .height(18.dp)
                    .widthIn(min = 18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) Color.Black.copy(alpha = 0.16f)
                        else t.folderCountBg
                    )
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    count.toString(),
                    color = if (selected) t.onPrimary else t.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeight = 10.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}

/** Uppercase micro label — "NOTEBOOK FOLDERS", "PINNED & RECENT". */
@Composable
internal fun StudioSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val t = studioTokens()
    Text(
        text,
        color = color ?: t.textTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        maxLines = 1,
        modifier = modifier,
    )
}

/** Tiny tinted pill — "LIVE", "10 NOTEBOOKS", "ACTIVE FOLDER". */
@Composable
internal fun StudioTag(
    text: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    corner: Dp = 5.dp,
    fontSize: Int = 9,
    fontWeight: FontWeight = FontWeight.Bold,
    horizontalPadding: Dp = 6.dp,
    verticalPadding: Dp = 2.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Text(text, color = fg, fontSize = fontSize.sp, fontWeight = fontWeight, maxLines = 1)
    }
}

/** Count pill used on sidebar rows (18dp tall). */
@Composable
internal fun StudioCountChip(
    text: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(18.dp)
            .widthIn(min = 18.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeight = 10.5.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

internal fun Modifier.alphaIf(condition: Boolean, alpha: Float): Modifier =
    if (condition) this.then(Modifier.graphicsLayer { this.alpha = alpha }) else this

/** What the little paper tag next to the title says, and in what colour. */
internal data class StudioBadgeSpec(val label: String, val bg: Color)

/** A one-line "42 pgs • CS Folder" style line built from whatever facts are actually known. */
internal fun metaLine(vararg parts: String?): String =
    parts.filterNotNull().filter { it.isNotBlank() }.joinToString(" • ")

/** Centre-aligned label used by empty states in the notebook grid. */
@Composable
internal fun StudioEmptyState(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val t = studioTokens()
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, color = t.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            action()
        }
    }
}

/** Circular avatar with initials. */
@Composable
internal fun StudioAvatar(
    initials: String,
    size: Dp,
    radius: Dp,
    gradient: Brush,
    fontSize: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = Color.White, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

internal val studioBlueGradient = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))
internal val studioAmberGradient = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
internal val studioLogoTileGradient = Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF0369A1)))

/** Two-letter initials. */
internal fun initialsOf(name: String): String {
    val words = name.trim().split(' ', '\t', '_', '-').filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "PN"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

/** Soft card chrome shared by notebook / folder / new tiles. */
@Composable
internal fun StudioCardSurface(
    modifier: Modifier = Modifier,
    bg: Color? = null,
    radius: Dp = 14.dp,
    border: Color? = null,
    content: @Composable () -> Unit,
) {
    val t = studioTokens()
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(bg ?: t.card)
            .then(if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(radius)) else Modifier),
    ) {
        content()
    }
}

/** Small round press-feedback target for star / dots / close affordances. */
@Composable
internal fun StudioMiniIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    bg: Color = Color.Unspecified,
    size: Dp = 24.dp,
    iconSize: Dp = 14.dp,
    radius: Dp = size / 2,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .then(if (bg != Color.Unspecified) Modifier.background(bg) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Floating menu surface (card context menus, sort dropdown, move popover). */
@Composable
internal fun StudioMenuSurface(
    modifier: Modifier = Modifier,
    minWidth: Dp = 190.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = studioTokens()
    Column(
        modifier
            // Popup content is measured with the whole window as max width; without an intrinsic
            // width the fillMaxWidth menu rows would stretch the surface across the screen.
            .widthIn(min = minWidth)
            .width(IntrinsicSize.Max)
            .then(
                if (t.isDark) {
                    Modifier.figcraftDarkShadow(shapeRadius = 16.dp, offsetY = 6.dp, spread = 2.dp)
                } else {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.22f),
                    )
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .background(t.card)
            .border(1.dp, t.cardBorder, RoundedCornerShape(16.dp))
            .padding(6.dp),
        content = content,
    )
}

/** One row inside a [StudioMenuSurface] with Apple fluid press response and hover glow. */
@Composable
internal fun StudioMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    trailingText: String? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val t = studioTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "menuItemScale",
    )

    val targetBg = when {
        !enabled -> Color.Transparent
        danger && (isHovered || isPressed) -> t.red.copy(alpha = 0.16f)
        isHovered || isPressed -> (if (t.isDark) Color(0xFF222838) else Color(0xFFEDF2F7))
        else -> Color.Transparent
    }

    val bg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "menuItemBg",
    )

    val fg = when {
        !enabled -> t.textTertiary.copy(alpha = 0.45f)
        danger -> t.red
        isHovered || isPressed -> (if (t.isDark) Color.White else t.textPrimary)
        else -> t.textPrimary
    }

    Row(
        modifier
            .fillMaxWidth()
            .height(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .alphaIf(!enabled, 0.55f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
        } else if (iconRes != null) {
            Icon(painter = painterResource(iconRes), contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = label,
            color = fg,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.W500,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (trailingText != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = trailingText,
                color = t.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

/** Divider line inside [StudioMenuSurface]. */
@Composable
internal fun StudioMenuDivider() {
    val t = studioTokens()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(1.dp)
            .background(t.divider)
    )
}

/** Soft drop shadow used by cards and floating surfaces. */
internal fun Modifier.studioCardShadow(radius: Dp = 14.dp): Modifier =
    this.shadow(
        elevation = 2.dp,
        shape = RoundedCornerShape(radius),
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.10f),
        spotColor = Color.Black.copy(alpha = 0.16f),
    )
