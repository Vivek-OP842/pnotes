package com.pnotes.ui

import com.pnotes.settings.ExplorerSortKey
import com.pnotes.ui.theme.floatingPill3D
import com.pnotes.ui.theme.elevated3D
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.PathParser
import android.graphics.RectF
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.R
import com.pnotes.core.model.Rgba
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.toComposeColor

/** Design tokens matching the PNotes reference UI with dynamic Light & Dark mode support */
typealias PNotesTokens = PnotesTokens

object PnotesTokens {
    val textPrimary: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
    val textSecondary: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val textMuted: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF71717A) else Color(0xFFA1A1AA)
    val cardBackground: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF454549) else Color(0xFFFFFFFF)
    val cardBorder: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF56565C) else Color(0xFFE4E4E7)
    val folderCardBg: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF454549) else Color(0xFFFFFFFF)
    val folderCardBorder: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF56565C) else Color(0xFFE4E4E7)
    val searchBg: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF202024) else Color(0xFFFFFFFF)
    val searchBorder: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF3C3C42) else Color(0xFFE4E4E7)
    val sidebarBg: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF2A2A2E) else Color(0xFFFAFAFA)
    val sidebarBorder: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF3C3C42) else Color(0xFFE4E4E7)
    val sidebarSelectedBg: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0x2EFFFFFF) else Color(0xFFF4F4F5)
    val sidebarSelectedBorder: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0x4DFFFFFF) else Color(0xFF09090B)
    val dialogBg: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF454549) else Color(0xFFFFFFFF)
    val dialogBorder: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF58585E) else Color(0xFFE4E4E7)
    val badgeBg: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF38383C) else Color(0xFFF4F4F5)
    val badgeText: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
    val dividerColor: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF3C3C42) else Color(0xFFE4E4E7)

    val PrimaryAccent: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    val SecondaryYellow: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFE4E4E7) else Color(0xFF27272A)
    val SoftYellowBg: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0x18FFFFFF) else Color(0xFFF4F4F5)
    val CoralAccent: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    val SoftCoralBg: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0x18FFFFFF) else Color(0xFFF4F4F5)
    val GreenAccent: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    val SoftGreenBg: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0x18FFFFFF) else Color(0xFFF4F4F5)
    val BlueAccent: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    val SoftBlueBg: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0x18FFFFFF) else Color(0xFFF4F4F5)
    val PurpleAccent: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    val SoftPurpleBg: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0x18FFFFFF) else Color(0xFFF4F4F5)
    val YellowIndicator: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    val YellowGold: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    val CardRadius = 16.dp
    val FabGradient: List<Color> @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) listOf(Color(0xFFFFFFFF), Color(0xFFE4E4E7)) else listOf(Color(0xFF09090B), Color(0xFF18181B))

    // Static fallback constants
    val DarkNavy = Color(0xFF09090B)
    val SubtitleColor = Color(0xFF52525B)
    val TextMuted = Color(0xFFA1A1AA)
    val CardBackground = Color(0xFFFFFFFF)
    val CardBorder = Color(0xFFE4E4E7)
    val FolderCardBg = Color(0xFFFFFFFF)
    val FolderCardBorder = Color(0xFFE4E4E7)
    val SearchBg = Color(0xFFFFFFFF)
    val SearchBorder = Color(0xFFE4E4E7)
}

/**
 * 3D Physical Elevation & Tactile Draw Modifier for Prototype Note Cards.
 * Multi-layer shadow using native canvas shadow layer, surface gradient, and subtle border.
 */
fun Modifier.prototypeCard3D(
    isDark: Boolean,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 8.dp,
    tintColor: Color? = null,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val borderWidth = if (tintColor != null) 2.dp else 0.5.dp
    val borderColor = tintColor ?: (if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7))
    val bgBrush = if (tintColor != null) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    tintColor.copy(alpha = 0.35f),
                    tintColor.copy(alpha = 0.16f),
                    Color(0xFF131317),
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    tintColor.copy(alpha = 0.28f),
                    tintColor.copy(alpha = 0.12f),
                    Color(0xFFFAFBFE),
                )
            )
        }
    } else {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF48484C),
                    Color(0xFF424246),
                    Color(0xFF3D3D41),
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFFAFBFE),
                    Color(0xFFF4F4F5),
                )
            )
        }
    }

    return this
        .drawBehind {
            val cornerRadiusPx = cornerRadius.toPx()
            val blurRadius = elevation.toPx() * (if (tintColor != null) 2.2f else 1.8f)
            val yOffset = elevation.toPx() * 0.9f

            drawIntoCanvas { canvas ->
                val shadowColorInt = if (tintColor != null) {
                    val r = (tintColor.red * 255).toInt()
                    val g = (tintColor.green * 255).toInt()
                    val b = (tintColor.blue * 255).toInt()
                    val glowAlpha = if (isDark) 120 else 60
                    android.graphics.Color.argb(glowAlpha, r, g, b)
                } else {
                    val shadowAlpha = if (isDark) 110 else 30
                    android.graphics.Color.argb(shadowAlpha, 0, 0, 0)
                }

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.TRANSPARENT
                    isAntiAlias = true
                    setShadowLayer(
                        blurRadius,
                        0f,
                        yOffset,
                        shadowColorInt,
                    )
                }
                canvas.nativeCanvas.drawRoundRect(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    cornerRadiusPx,
                    cornerRadiusPx,
                    paint,
                )
            }
        }
        .background(bgBrush, shape)
        .border(
            width = borderWidth,
            color = borderColor,
            shape = shape,
        )
        .clip(shape)
}

/**
 * 3D Physical Elevation & Tactile Draw Modifier for Prototype Filter & Tab Pills.
 * Supports inactive tactile gradient pill vs active inverted pill with top specular highlights and subtle inset rim.
 */
fun Modifier.prototypePill3D(
    isDark: Boolean,
    isActive: Boolean = false,
    cornerRadius: Dp = 9.dp,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val backgroundBrush = if (isDark) {
        if (isActive) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFE4E4E7),
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF4C4C52),
                    Color(0xFF414146),
                )
            )
        }
    } else {
        if (isActive) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF27272A),
                    Color(0xFF09090B),
                    Color(0xFF020617),
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFFAFAFA),
                    Color(0xFFE4E4E7),
                )
            )
        }
    }

    val borderBrush = if (isDark) {
        if (isActive) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFD4D4D8),
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.18f),
                    Color(0xFF56565C),
                    Color.White.copy(alpha = 0.06f),
                )
            )
        }
    } else {
        if (isActive) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF52525B),
                    Color(0xFF09090B),
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFFE4E4E7),
                )
            )
        }
    }

    return this
        .drawBehind {
            val shadowAlpha = if (isDark) {
                if (isActive) 90 else 40
            } else {
                if (isActive) 50 else 20
            }
            val blurRadius = if (isActive) 6.dp.toPx() else 3.dp.toPx()
            val yOffset = if (isActive) 2.5.dp.toPx() else 1.dp.toPx()

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.TRANSPARENT
                    isAntiAlias = true
                    setShadowLayer(
                        blurRadius,
                        0f,
                        yOffset,
                        android.graphics.Color.argb(shadowAlpha, 0, 0, 0),
                    )
                }
                canvas.nativeCanvas.drawRoundRect(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    cornerRadius.toPx(),
                    cornerRadius.toPx(),
                    paint,
                )
            }
        }
        .clip(shape)
        .background(backgroundBrush)
        .border(
            width = 0.5.dp,
            brush = borderBrush,
            shape = shape,
        )
}

/**
 * 3D Physical Elevation & Tactile Draw Modifier for Prototype Search Bar.
 * Inner inset shadow styling with subtle outer border matching the prototype search bar.
 */
fun Modifier.prototypeSearch3D(
    isDark: Boolean,
    cornerRadius: Dp = 10.dp,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val bgColor = if (isDark) Color(0xFF202024) else Color(0xFFFAFAFA)
    val borderColor = if (isDark) Color(0xFF3C3C42) else Color(0xFFE4E4E7)

    return this
        .clip(shape)
        .background(bgColor)
        .border(
            width = 0.5.dp,
            color = borderColor,
            shape = shape,
        )
        .drawBehind {
            val cornerRadiusPx = cornerRadius.toPx()
            val topInsetAlpha = if (isDark) 0.35f else 0.08f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = topInsetAlpha),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = 6.dp.toPx(),
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, 6.dp.toPx()),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            )
        }
}

/**
 * Exact PNotes App Brand Logo from the prototype.
 * 36×36dp container with 10dp radius and 3D surface gradient (linear-gradient(145deg, #22222a 0%, #0e0e12 50%, #050508 100%)),
 * fountain pen nib emblem (18×18dp centered inside), and "PNotes" text in 20.sp Bold (-0.5.sp tracking).
 */
@Composable
fun PNotesBrandLogo(
    isDark: Boolean = com.pnotes.ui.theme.LocalPalette.current.isDark,
    showText: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_notes_app_logo),
            contentDescription = "PNotes Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        if (showText) {
            Text(
                text = "PNotes",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                letterSpacing = (-0.5).sp,
            )
        }
    }
}

/** Section Header with dynamic indicator pill */
@Composable
fun PnotesSectionHeader(
    icon: ImageVector? = null,
    drawableRes: Int? = null,
    title: String? = null,
    indicatorColor: Color = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color.White else Color(0xFF09090B),
    iconTint: Color? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Vertical indicator pill
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(indicatorColor),
        )
        Spacer(Modifier.width(8.dp))
        if (drawableRes != null) {
            if (iconTint != null) {
                Icon(
                    painter = painterResource(drawableRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Image(
                    painter = painterResource(drawableRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint ?: PnotesTokens.textPrimary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        if (content != null) {
            content()
        } else if (title != null) {
            Text(
                text = title,
                color = PnotesTokens.textPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

/** Search Bar with ⌘ K shortcut badge */
@Composable
fun PnotesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .shadow(
                elevation = if (isFocused) 4.dp else 1.5.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = if (isDark) Color(0x33000000) else Color(0x10000000),
                ambientColor = if (isDark) Color(0x22000000) else Color(0x06000000),
            )
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDark) Color(0xFF121212) else PnotesTokens.searchBg)
            .border(
                width = 1.dp,
                color = if (isFocused) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0x14FFFFFF) else PnotesTokens.searchBorder),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                runCatching { focus.requestFocus() }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_action_search),
            contentDescription = "Search",
            tint = if (isDark) (if (isFocused) Color.White else Color(0xFFA1A1AA)) else (if (isFocused) Color(0xFF09090B) else Color(0xFF7A8795)),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search notes",
                    color = if (isDark) Color(0xFF858C98) else Color(0xFF7A8795),
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = if (isDark) Color(0xFFF4F6F8) else PnotesTokens.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                ),
                cursorBrush = SolidColor(if (isDark) Color.White else Color(0xFF09090B)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .onFocusChanged { isFocused = it.isFocused },
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                imageVector = PnotesIcons.close,
                contentDescription = "Clear search",
                tint = PnotesTokens.textMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange(""); focusManager.clearFocus() },
            )
            Spacer(Modifier.width(8.dp))
        }
        // "⌘ K" Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isDark) Color(0xFF1E232B) else Color(0xFFF4F4F5))
                .border(0.5.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFDCE3EA), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "⌘ K",
                color = if (isDark) Color(0xFF858C98) else Color(0xFF6B7785),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Top Action Bar (Plus, Folder+, Menu buttons) */
@Composable
fun PnotesTopActions(
    onNewNote: () -> Unit,
    onNewFolder: () -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val btnBg = if (isDark) Color(0xFF161616) else Color.White
    val btnBorder = if (isDark) Color(0x14FFFFFF) else Color(0xFFE1E6EB)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val plusInteraction = remember { MutableInteractionSource() }
        val plusPressed by plusInteraction.collectIsPressedAsState()
        val plusScale by animateFloatAsState(
            targetValue = if (plusPressed) 0.88f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
            label = "plusScale",
        )

        // Plus action
        Surface(
            onClick = onNewNote,
            interactionSource = plusInteraction,
            shape = RoundedCornerShape(12.dp),
            color = btnBg,
            shadowElevation = 0.5.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, btnBorder),
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer {
                    scaleX = plusScale
                    scaleY = plusScale
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = PnotesIcons.plus,
                    contentDescription = "New note",
                    tint = if (isDark) Color.White else Color(0xFF09090B),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        val folderInteraction = remember { MutableInteractionSource() }
        val folderPressed by folderInteraction.collectIsPressedAsState()
        val folderScale by animateFloatAsState(
            targetValue = if (folderPressed) 0.88f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
            label = "folderScale",
        )

        // Folder action
        Surface(
            onClick = onNewFolder,
            interactionSource = folderInteraction,
            shape = RoundedCornerShape(12.dp),
            color = btnBg,
            shadowElevation = 0.5.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, btnBorder),
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer {
                    scaleX = folderScale
                    scaleY = folderScale
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = PnotesIcons.newFolder,
                    contentDescription = "New folder",
                    tint = if (isDark) Color.White else Color(0xFF09090B),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        val moreInteraction = remember { MutableInteractionSource() }
        val morePressed by moreInteraction.collectIsPressedAsState()
        val moreScale by animateFloatAsState(
            targetValue = if (morePressed) 0.88f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
            label = "moreScale",
        )

        // More options
        Surface(
            onClick = onOpenMore,
            interactionSource = moreInteraction,
            shape = RoundedCornerShape(12.dp),
            color = btnBg,
            shadowElevation = 0.5.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, btnBorder),
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer {
                    scaleX = moreScale
                    scaleY = moreScale
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_more_horizontal),
                    contentDescription = "More",
                    tint = if (isDark) Color(0xFFF4F5F7) else Color(0xFF20242A),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** 3D Warm Luminous Folder Icon matching reference */
@Composable
fun Folder3DIcon(
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Box(
        modifier = modifier.size(46.dp, 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Soft diffuse monochrome glow behind folder icon
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isDark) Color(0x24FFFFFF) else Color(0x0C000000),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.50f, h * 0.54f),
                    radius = w * 0.72f,
                ),
                topLeft = Offset(-w * 0.22f, -h * 0.22f),
                size = Size(w * 1.44f, h * 1.44f),
            )
        }

        Image(
            painter = painterResource(R.drawable.ic_folder_peach_3d),
            contentDescription = "Folder",
            modifier = Modifier.size(44.dp, 38.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

/** Glowing Folder Card matching reference */
@Composable
fun FolderCard(
    folderName: String,
    noteCount: Int,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuContent: @Composable (() -> Unit)? = null,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val cardBg = if (isDark) Color(0xFF454549) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF56565C) else Color(0xFFE4E4E7)

    val cardInteraction = remember { MutableInteractionSource() }
    val isCardPressed by cardInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
        label = "folderCardScale",
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.5.dp,
        modifier = modifier
            .width(220.dp)
            .height(84.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isDark) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x14FFFFFF),
                                Color(0x05FFFFFF),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.16f, size.height * 0.50f),
                            radius = size.width * 0.65f,
                        ),
                        topLeft = Offset(-size.width * 0.15f, -size.height * 0.30f),
                        size = Size(size.width * 0.95f, size.height * 1.60f),
                    )
                }
            } else {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x06000000),
                                Color(0x02000000),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.16f, size.height * 0.50f),
                            radius = size.width * 0.55f,
                        ),
                        topLeft = Offset(-size.width * 0.10f, -size.height * 0.20f),
                        size = Size(size.width * 0.85f, size.height * 1.40f),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = onClick,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Folder3DIcon(modifier = Modifier.size(46.dp, 38.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = folderName,
                            color = if (isDark) Color(0xFFF3F4F6) else PnotesTokens.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$noteCount notes",
                            color = if (isDark) Color(0xFF9BA1AD) else PnotesTokens.textSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
                // 3-dot options menu
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    val menuInteraction = remember { MutableInteractionSource() }
                    val isMenuPressed by menuInteraction.collectIsPressedAsState()
                    val menuScale by animateFloatAsState(
                        targetValue = if (isMenuPressed) 0.82f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
                        label = "folderMenuScale",
                    )
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                scaleX = menuScale
                                scaleY = menuScale
                            },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_more_horizontal),
                            contentDescription = "Folder options",
                            tint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF9AA5B1),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    menuContent?.invoke()
                }
            }
        }
    }
}

/** Quick Action Card to create a new subfolder in Folders row */
@Composable
fun NewFolderActionCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
        label = "newFolderCardScale",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF141A22) else Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, if (isDark) Color(0x18FFFFFF) else Color(0xFFE1E5E9)),
        shadowElevation = 0.5.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .width(200.dp)
            .height(82.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_add_folder),
                    contentDescription = "New folder",
                    tint = if (isDark) Color.White else Color(0xFF09090B),
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    text = "New folder",
                    color = if (isDark) Color(0xFFF3F4F6) else PnotesTokens.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Add subfolder",
                    color = if (isDark) Color(0xFF858D99) else PnotesTokens.textSecondary,
                    fontSize = 12.5.sp,
                )
            }
        }
    }
}

/** All Notes Capsule Pill Button beside Search Bar with glowing active state */
@Composable
fun AllNotesCapsuleButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.height(44.dp),
    ) {
        // Glowing halo aura when active
        if (isActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .scale(scaleX = 1.05f, scaleY = 1.15f)
                    .alpha(0.60f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (isDark) Color.White else Color(0xFF09090B)).copy(alpha = 0.20f),
                                Color.Transparent,
                            ),
                        ),
                        shape = RoundedCornerShape(26.dp),
                    )
            )
        }

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(22.dp),
            color = if (isActive) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF)),
            border = BorderStroke(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFF27272A) else Color(0xFFE1E6EB)),
            ),
            shadowElevation = if (isActive) 4.dp else 1.dp,
            modifier = Modifier.fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(
                        brush = if (isActive) {
                            Brush.horizontalGradient(
                                if (isDark) listOf(Color.White, Color(0xFFE4E4E7)) else listOf(Color(0xFF09090B), Color(0xFF18181B))
                            )
                        } else {
                            Brush.horizontalGradient(
                                if (isDark) listOf(Color(0xFF18181B), Color(0xFF131316)) else listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF)),
                            )
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(
                        imageVector = PnotesIcons.pen,
                        contentDescription = null,
                        tint = if (isActive) (if (isDark) Color(0xFF09090B) else Color.White) else PnotesTokens.textPrimary,
                        modifier = Modifier.size(if (isActive) 16.dp else 14.dp),
                    )
                    Text(
                        text = "All notes",
                        color = if (isActive) (if (isDark) Color(0xFF09090B) else Color.White) else PnotesTokens.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                        letterSpacing = if (isActive) 0.3.sp else 0.sp,
                    )
                }
            }
        }
    }
}

/** Sleek, compact Note List Row for List View */
@Composable
fun NoteListRow(
    title: String,
    dateText: String,
    thumbnail: ImageBitmap? = null,
    styleIndex: Int = 0,
    selected: Boolean = false,
    isStarred: Boolean = false,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    onStarClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    menuContent: @Composable (() -> Unit)? = null,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val rowInteraction = remember { MutableInteractionSource() }
    val isRowPressed by rowInteraction.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (isRowPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
        label = "rowScale",
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5)
        } else {
            PnotesTokens.cardBackground
        },
        shadowElevation = if (selected) 2.dp else 0.5.dp,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                if (isDark) Color.White else Color(0xFF09090B)
            } else {
                PnotesTokens.cardBorder
            },
        ),
        modifier = modifier
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .fillMaxWidth()
            .height(68.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Clickable note body (Mini Thumbnail + Title & Date Column)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = rowInteraction,
                        indication = null,
                        onClick = onClick,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Mini Thumbnail
                Box(
                    modifier = Modifier
                        .size(52.dp, 48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                ) {
                    StylizedNotePreview(
                        styleIndex = styleIndex,
                        thumbnail = thumbnail,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Title & Date Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = title,
                        color = PnotesTokens.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = dateText,
                        color = PnotesTokens.textSecondary,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                    )
                }
            }

            // Star / Favorite Button
            IconButton(
                onClick = onStarClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = PnotesIcons.star,
                    contentDescription = "Star",
                    tint = if (isStarred) PnotesTokens.YellowIndicator else if (isDark) Color(0xFF5A6372) else Color(0xFFD4D4D8),
                    modifier = Modifier.size(17.dp),
                )
            }

            // 3-dots Menu Button
            Box(
                modifier = Modifier.wrapContentSize(),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_more_horizontal),
                        contentDescription = "Note options",
                        tint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF71717A),
                        modifier = Modifier.size(18.dp),
                    )
                }
                menuContent?.invoke()
            }
        }
    }
}

/** Real Note Preview (renders live document/canvas/pdf thumbnail, or clean paper canvas fallback) */
@Composable
fun StylizedNotePreview(
    styleIndex: Int,
    thumbnail: ImageBitmap? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val paperColor = if (isDark) Color(0xFF000000) else Color(0xFFFAFAF9)

    Box(
        modifier = modifier
            .background(paperColor, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFECEAE4),
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp)),
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = "Note page preview",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val lineColor = if (isDark) Color(0x1E8899A6) else Color(0xFFE2DDD5)

                for (i in 1..4) {
                    val y = h * (0.24f + i * 0.15f)
                    drawLine(
                        color = lineColor,
                        start = Offset(w * 0.12f, y),
                        end = Offset(w * (if (i == 4) 0.55f else 0.88f), y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/** Note Card matching Reference Image */
@Composable
fun NoteCard(
    title: String,
    dateText: String,
    thumbnail: ImageBitmap? = null,
    styleIndex: Int = 0,
    selected: Boolean = false,
    isStarred: Boolean = false,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    onStarClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    menuContent: @Composable (() -> Unit)? = null,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val cardInteraction = remember { MutableInteractionSource() }
    val isCardPressed by cardInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
        label = "noteCardScale",
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5)
        } else {
            if (isDark) Color(0xFF14171C) else PnotesTokens.cardBackground
        },
        shadowElevation = if (selected) 4.dp else 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color.White.copy(alpha = 0.06f) else PnotesTokens.cardBorder),
        ),
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            // Note Preview Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.32f),
            ) {
                StylizedNotePreview(
                    styleIndex = styleIndex,
                    thumbnail = thumbnail,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = onClick,
                        ),
                )

                // Top right 3-dot menu button inside preview
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .wrapContentSize(),
                ) {
                    val menuInteraction = remember { MutableInteractionSource() }
                    val isMenuPressed by menuInteraction.collectIsPressedAsState()
                    val menuScale by animateFloatAsState(
                        targetValue = if (isMenuPressed) 0.86f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
                        label = "menuBtnScale",
                    )
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color(0xCC1A202C) else Color(0xDDFAFAFA),
                            )
                            .graphicsLayer {
                                scaleX = menuScale
                                scaleY = menuScale
                            },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_more_horizontal),
                            contentDescription = "Options",
                            tint = if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    if (menuContent != null) {
                        menuContent()
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title & Date Row + Star
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = PnotesTokens.textPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = dateText,
                        color = PnotesTokens.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
                val starScale by animateFloatAsState(
                    targetValue = if (isStarred) 1.25f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
                    label = "starScale",
                )
                IconButton(
                    onClick = onStarClick,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = PnotesIcons.star,
                        contentDescription = "Star",
                        tint = if (isStarred) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFF6B7280) else Color(0xFFD4D4D8)),
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                scaleX = starScale
                                scaleY = starScale
                            },
                    )
                }
            }
        }
    }
}

/** Premium Obsidian/White Floating Action Button with soft monochrome glow */
@Composable
fun CreateNoteFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "fab_scale",
    )

    val fabBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    val fabIconTint = if (isDark) Color(0xFF09090B) else Color.White
    val fabSpotColor = if (isDark) Color(0x35FFFFFF) else Color(0x35000000)
    val fabAmbientColor = if (isDark) Color(0x18FFFFFF) else Color(0x18000000)
    val fabGradient = if (isDark) listOf(Color(0xFFFFFFFF), Color(0xFFE4E4E7)) else listOf(Color(0xFF09090B), Color(0xFF18181B))

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = fabBg,
        shadowElevation = if (isPressed) 3.dp else 8.dp,
        modifier = modifier
            .scale(scale)
            .size(68.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                spotColor = fabSpotColor,
                ambientColor = fabAmbientColor,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = fabGradient,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = com.pnotes.ui.icons.PnotesIcons.singleStickPen,
                contentDescription = "Create note",
                tint = fabIconTint,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/**
 * Premium Overview & Quick Actions Dashboard Panel matching Reference
 */
@Composable
fun HomeOverviewPanel(
    totalNotesCount: Int,
    foldersCount: Int,
    recentNotesCount: Int,
    favoritesCount: Int,
    thisWeekCount: Int = 0,
    onTotalNotesClick: (() -> Unit)? = null,
    onFoldersClick: (() -> Unit)? = null,
    onRecentNotesClick: (() -> Unit)? = null,
    onFavoritesClick: (() -> Unit)? = null,
    onNewNote: () -> Unit,
    onNewCanvas: () -> Unit,
    onImportPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = if (isDark) Color(0x40000000) else Color(0x10000000),
                ambientColor = if (isDark) Color(0x20000000) else Color(0x06000000),
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = if (isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF14171D),
                            Color(0xFF161920),
                            Color(0xFF181C24),
                        ),
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFFFFFFF),
                        ),
                    )
                },
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFE4E7EB),
                shape = RoundedCornerShape(22.dp),
            ),
    ) {
        // Ambient soft warm golden glow on left of overview panel
        if (isDark) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x18FFC21F),
                            Color(0x08FFAA00),
                            Color(0x00000000),
                        ),
                        center = Offset(size.width * 0.10f, size.height * 0.50f),
                        radius = size.width * 0.40f,
                    ),
                    topLeft = Offset(-size.width * 0.15f, -size.height * 0.30f),
                    size = Size(size.width * 0.70f, size.height * 1.60f),
                )
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x1038BDF8),
                            Color(0x00000000),
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.50f),
                        radius = size.width * 0.35f,
                    ),
                    topLeft = Offset(size.width * 0.50f, -size.height * 0.20f),
                    size = Size(size.width * 0.60f, size.height * 1.40f),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: Overview Statistics (Total notes, Folders, Recent notes, Favorites)
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Overview",
                    color = if (isDark) Color(0xFFE9ECF0) else PnotesTokens.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Total notes
                    StatItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5))
                                    .border(1.dp, if (isDark) Color(0x35FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = com.pnotes.ui.icons.PnotesIcons.stickyNote,
                                    contentDescription = "Total notes",
                                    tint = if (isDark) Color.White else Color(0xFF09090B),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        label = "Total notes",
                        value = totalNotesCount.toString(),
                        subtitle = if (thisWeekCount > 0) "+$thisWeekCount this week" else "Active notes",
                        subtitleColor = if (isDark) Color.White else Color(0xFF09090B),
                        onClick = onTotalNotesClick,
                    )

                    // Folders
                    StatItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xFF132238) else Color(0xFFEAF5FF))
                                    .border(1.dp, if (isDark) Color(0x353B9FE8) else Color(0xFFBAE6FD), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = com.pnotes.ui.icons.PnotesIcons.folder,
                                    contentDescription = "Folders",
                                    tint = Color(0xFF3B9FE8),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        label = "Folders",
                        value = foldersCount.toString(),
                        subtitle = if (foldersCount > 0) "$foldersCount categories" else "Organized",
                        subtitleColor = if (isDark) Color(0xFF8B929E) else Color(0xFF9AA5B1),
                        onClick = onFoldersClick,
                    )

                    // Recent notes
                    StatItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5))
                                    .border(1.dp, if (isDark) Color(0x35FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = com.pnotes.ui.icons.PnotesIcons.clock,
                                    contentDescription = "Recent notes",
                                    tint = if (isDark) Color.White else Color(0xFF09090B),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        label = "Recent notes",
                        value = recentNotesCount.toString(),
                        subtitle = "Opened lately",
                        subtitleColor = if (isDark) Color(0xFF8B929E) else Color(0xFF9AA5B1),
                        onClick = onRecentNotesClick,
                    )

                    // Favorites
                    StatItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xFF14281C) else Color(0xFFE8F7E9))
                                    .border(1.dp, if (isDark) Color(0x354CAF50) else Color(0xFFBBF7D0), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = com.pnotes.ui.icons.PnotesIcons.star,
                                    contentDescription = "Favorites",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        label = "Favorites",
                        value = favoritesCount.toString(),
                        subtitle = if (favoritesCount > 0) "$favoritesCount starred" else "Quick access",
                        subtitleColor = if (favoritesCount > 0) Color(0xFF4CAF50) else (if (isDark) Color(0xFF8B929E) else Color(0xFF9AA5B1)),
                        onClick = onFavoritesClick,
                    )
                }
            }

            Spacer(Modifier.width(20.dp))

            // Subtle Vertical Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE4E7EB)),
            )

            Spacer(Modifier.width(20.dp))

            // Right: Quick Actions (New note, New canvas, Import PDF)
            Column {
                Text(
                    text = "Quick actions",
                    color = if (isDark) Color(0xFFE9ECF0) else PnotesTokens.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // New note
                    QuickActionItem(
                        label = "New note",
                        icon = {
                            Icon(
                                imageVector = com.pnotes.ui.icons.PnotesIcons.pen,
                                contentDescription = "New note",
                                tint = if (isDark) Color.White else Color(0xFF09090B),
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        onClick = onNewNote,
                    )

                    // New canvas
                    QuickActionItem(
                        label = "New canvas",
                        icon = {
                            Icon(
                                imageVector = com.pnotes.ui.icons.PnotesIcons.canvas,
                                contentDescription = "New canvas",
                                tint = PnotesTokens.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        onClick = onNewCanvas,
                    )

                    // Import PDF
                    QuickActionItem(
                        label = "Import PDF",
                        icon = {
                            Icon(
                                imageVector = com.pnotes.ui.icons.PnotesIcons.download,
                                contentDescription = "Import PDF",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        onClick = onImportPdf,
                    )
                }
            }
        }
    }
}

/** Individual Metric Stat Item in Overview Panel with Tactile Interaction */
@Composable
fun StatItem(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    subtitle: String? = null,
    subtitleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else (if (isHovered) 1.04f else 1.0f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "statItemScale",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .graphicsLayer { scaleX = itemScale; scaleY = itemScale }
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .padding(4.dp),
    ) {
        icon()
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                color = if (isDark) Color(0xFF8B929E) else Color(0xFF6B7785),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = value,
                color = if (isDark) Color(0xFFF4F6F8) else PnotesTokens.textPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = if (subtitleColor != Color.Unspecified) subtitleColor else (if (isDark) Color(0xFF8B929E) else Color(0xFF9AA5B1)),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Individual Quick Action Button matching Reference */
@Composable
fun QuickActionItem(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val actionScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
        label = "quickActionScale",
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF1E212A) else Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, if (isDark) Color(0x18FFFFFF) else Color(0xFFE2E6EA)),
        shadowElevation = 0.5.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = actionScale
                scaleY = actionScale
            }
            .width(96.dp)
            .height(76.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                color = if (isDark) Color(0xFFE4E4E7) else PnotesTokens.textPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

// =========================================================================================
// HOMESCREEN REDESIGN COMPONENTS (Matching 'Homescreen - Redesign +1 more.html' Prototype)
// =========================================================================================

/**
 * 1. PNotes Top Bar matching Prototype Layout:
 * - Height: 68dp (or adaptive)
 * - Left Group: PNotesBrandLogo, 1x20dp vertical divider line, Workspace switcher capsule (181.55x28dp, 8dp radius) with prototypePill3D
 * - Center Group: Centered Search Bar (480x40dp max, 10dp radius) with prototypeSearch3D, 16x16 search icon, 13.sp #94A3B8 placeholder, 30.5x19dp 6dp radius 11.sp Medium #64748B "⌘K" shortcut badge
 * - Right Actions Group: Grid/List View Toggle (64x34dp, 8dp radius) with prototypePill3D, Import Button (83.36x36dp, 9dp radius) with prototypePill3D, New Note Button (110.49x36dp, 9dp radius) with active prototypePill3D, User Avatar (34x34dp circle) with 3D gradient and initial "V"
 * - Bottom: 1dp divider line (#E2E8F0 in Light, #27272A in Dark)
 */
@Composable
fun PNotesPrototypeTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onShowSidebar: () -> Unit = {},
    onNewNote: () -> Unit,
    onImport: () -> Unit = {},
    isGridView: Boolean = true,
    onToggleGridView: () -> Unit = {},
    workspaceName: String = "Vivek's Workspace",
    onWorkspaceClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    /** Letter shown in the avatar control — the signed-in initial, or the workspace's when signed out. */
    avatarLabel: String = "V",
    compact: Boolean = false,
    sidebarOpen: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left Group: Logo + Vertical Divider + Workspace Switcher Capsule (All 36dp Height)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onShowSidebar),
                ) {
                    PNotesBrandLogo(isDark = isDark, showText = true)
                }

                if (!compact) {
                    // Vertical divider line (1dp wide, 20dp high, #E2E8F0 / #27272A)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .width(1.dp)
                            .height(20.dp)
                            .background(if (isDark) Color(0xFF3C3C42) else Color(0xFFE4E4E7)),
                    )

                    // Workspace switcher capsule (36dp height matching all buttons)
                    Box(
                        modifier = Modifier
                            .widthIn(max = 190.dp)
                            .height(36.dp)
                            .prototypePill3D(isDark = isDark, isActive = false, cornerRadius = 9.dp)
                            .clickable(onClick = onWorkspaceClick)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = PnotesIcons.user,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                                    modifier = Modifier.size(width = 14.dp, height = 13.5.dp),
                                )
                                Text(
                                    text = workspaceName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = PnotesIcons.chevronDown,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                                modifier = Modifier.size(width = 12.dp, height = 6.5.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            // Center Group: Centered Search Bar (Unified 36dp Height, 9dp radius)
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(min = 160.dp, max = 460.dp)
                    .height(36.dp)
                    .prototypeSearch3D(isDark = isDark, cornerRadius = 9.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = PnotesIcons.search,
                        contentDescription = "Search",
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    val searchTextStyle = TextStyle(
                        color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both,
                        ),
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            textStyle = searchTextStyle,
                            cursorBrush = SolidColor(if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search notes, handwritten text, folders...",
                                            style = searchTextStyle.copy(color = Color(0xFFA1A1AA)),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = PnotesIcons.close,
                            contentDescription = "Clear search",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { onSearchChange("") },
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    if (!compact) {
                        // ⌘K shortcut badge (30.5×19dp, 6dp radius, 11.sp Medium #64748B)
                        Box(
                            modifier = Modifier
                                .size(width = 30.5.dp, height = 19.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "⌘K",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF71717A),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            // Right Actions Group: All Unified 36dp Height (Import + New Note + Avatar)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Import Button (83.36×36dp, 9dp radius)
                if (!compact) {
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .prototypePill3D(isDark = isDark, isActive = false, cornerRadius = 9.dp)
                            .clickable(onClick = onImport)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = PnotesIcons.importDoc,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFE4E4E7) else Color(0xFF09090B),
                                modifier = Modifier.size(width = 12.7.dp, height = 15.dp),
                            )
                            Text(
                                text = "Import",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF09090B),
                            )
                        }
                    }
                }

                // New Note Button (36dp height, 9dp radius)
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .prototypePill3D(isDark = isDark, isActive = true, cornerRadius = 9.dp)
                        .clickable(onClick = onNewNote)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = PnotesIcons.plus,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF0B0F17) else Color(0xFFFFFFFF),
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "New Note",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF0B0F17) else Color(0xFFFFFFFF),
                        )
                    }
                }

                // User Avatar (36×36dp circle with 3D gradient matching 36dp button height)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) {
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF3F3F46),
                                        Color(0xFF27272A),
                                        Color(0xFF18181B),
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF27272A),
                                        Color(0xFF18181B),
                                        Color(0xFF09090B),
                                    )
                                )
                            }
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (isDark) Color(0xFF52525B) else Color(0xFFD4D4D8),
                            shape = CircleShape,
                        )
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = avatarLabel,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Full-width divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (isDark) Color(0xFF3C3C42) else Color(0xFFE4E4E7)),
        )
    }
}

/** Legacy / simple wrapper for PNotesTopBar */
@Composable
fun PNotesTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onShowSidebar: () -> Unit,
    onNewFolder: () -> Unit,
    onNewNote: () -> Unit,
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    PNotesPrototypeTopBar(
        searchQuery = searchQuery,
        onSearchChange = onSearchChange,
        onShowSidebar = onShowSidebar,
        onNewNote = onNewNote,
        onAvatarClick = onMoreClick,
        modifier = modifier,
    )
}

/**
 * 2. Greeting And Resume Card:
 * - Top: "Good evening, Vivek 👋", subtitle
 * - Bottom Container:
 *   - Left: "CONTINUE INKING", time ago, icon, title, subtitle, [Resume ->] button
 *   - Divider
 *   - Right: "TODAY'S STUDY GOAL", "3 of 4 Done" pill, task text, 75% progress bar
 */
@Composable
fun GreetingAndResumeCard(
    userName: String = "Vivek",
    recentNoteTitle: String = "Project Nova: Masterplan",
    recentNoteSubtitle: String = "Page 1 of 5 · Hybrid Event Mesh",
    timeAgoText: String = "2 min ago",
    todayGoalText: String = "Review CRDT sync & inking latency",
    goalProgress: Float = 0.75f,
    goalStatusText: String = "3 of 4 Done",
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingWord = when (currentHour) {
        in 4..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF454549) else Color(0xFFFFFFFF),
        border = BorderStroke(0.5.dp, if (isDark) Color(0xFF56565C) else Color(0xFFE4E4E7)),
        shadowElevation = 0.5.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            // Top Section: Greeting & Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "$greetingWord, $userName",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF09090B),
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = "👋",
                    fontSize = 22.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Capture ideas. Organize thoughts. Stay productive.",
                fontSize = 13.sp,
                color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
            )

            Spacer(Modifier.height(18.dp))

            // Bottom Inset Container: Continue Inking & Study Goal
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF121212) else Color(0xFFFAFAFA),
                border = BorderStroke(0.5.dp, if (isDark) Color(0xFF242424) else Color(0xFFE4E4E7)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left Column: Continue Inking
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)),
                                )
                                Text(
                                    text = "CONTINUE INKING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                                )
                            }
                            Text(
                                text = timeAgoText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF71717A),
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Icon Box
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5))
                                    .border(0.5.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFE4E4E7), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PnotesIcons.edit,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recentNoteTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isDark) Color.White else Color(0xFF09090B),
                                )
                                Text(
                                    text = recentNoteSubtitle,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            // Resume button
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B))
                                    .clickable(onClick = onResumeClick)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "Resume",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "→",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .width(1.dp)
                            .height(48.dp)
                            .background(if (isDark) Color(0xFF242424) else Color(0xFFE4E4E7)),
                    )

                    // Right Column: Study Goal
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = PnotesIcons.check,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "TODAY'S STUDY GOAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                )
                            }

                            // Done badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF0D2818) else Color(0xFFECFDF5))
                                    .border(
                                        0.5.dp,
                                        if (isDark) Color(0xFF10B981) else Color(0xFFA7F3D0),
                                        RoundedCornerShape(6.dp),
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = goalStatusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF10B981)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PnotesIcons.check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                            Text(
                                text = todayGoalText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A),
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Progress bar (4dp height)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFE4E4E7)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(goalProgress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Studio Canvas Streak Card:
 * - Gradient banner `#1E1B4B` -> `#0A0A0A`
 * - "🔥 14-Day Streak" pill + gold stars
 * - Title "PNotes Studio Canvas" + subtitle
 * - "Open Canvas Mode ↗" button
 */
@Composable
fun StudioCanvasStreakCard(
    streakDays: Int = 14,
    onOpenCanvas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(0.75.dp, if (isDark) Color(0xFF3F3F46) else Color(0x33FFFFFF)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isDark) {
                    Brush.linearGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0A0A0A)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFF09090B), Color(0xFF18181B)))
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            // Top Row: Streak Pill & Gold Sparkle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x26FFFFFF))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "🔥 $streakDays-Day Streak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }

                Icon(
                    imageVector = PnotesIcons.star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "PNotes Studio Canvas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ultra low-latency inking mesh with CRDT real-time sync.",
                fontSize = 12.sp,
                color = Color(0xFFD4D4D8),
                maxLines = 2,
            )

            Spacer(Modifier.height(18.dp))

            // Bottom action button: Open Canvas Mode ↗
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenCanvas)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = PnotesIcons.canvas,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Open Canvas Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    Text(
                        text = "↗",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/**
 * 4. PNotes Filter Tabs Row:
 * - Filter pills: All, Notes, Canvases, PDFs, Favorites, Recent (with counts)
 * - Trailing "Size" sort dropdown button + Grid/List layout toggle button
 */
@Composable
fun PNotesFilterTabsRow(
    activeFilter: HomeFilter,
    onFilterSelect: (HomeFilter) -> Unit,
    allCount: Int,
    notesCount: Int,
    canvasesCount: Int,
    pdfsCount: Int,
    favCount: Int,
    recentCount: Int,
    sortLabel: String = "Size",
    onSortClick: () -> Unit,
    isGridView: Boolean = true,
    onToggleGridView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Filter Pills Row
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeFilter.entries.forEach { filter ->
                val isSelected = activeFilter == filter
                val count = when (filter) {
                    HomeFilter.ALL -> allCount
                    HomeFilter.NOTES -> notesCount
                    HomeFilter.CANVASES -> canvasesCount
                    HomeFilter.PDFS -> pdfsCount
                    HomeFilter.FAVORITES -> favCount
                    HomeFilter.RECENT -> recentCount
                }

                val filterIcon: ImageVector? = when (filter) {
                    HomeFilter.ALL -> null
                    HomeFilter.NOTES -> PnotesIcons.file
                    HomeFilter.CANVASES -> PnotesIcons.canvas
                    HomeFilter.PDFS -> PnotesIcons.importDoc
                    HomeFilter.FAVORITES -> PnotesIcons.star
                    HomeFilter.RECENT -> PnotesIcons.clock
                }

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) (if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B))
                            else (if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF))
                        )
                        .border(
                            0.5.dp,
                            if (isSelected) Color.Transparent
                            else (if (isDark) Color(0xFF242424) else Color(0xFFE4E4E7)),
                            RoundedCornerShape(18.dp),
                        )
                        .clickable { onFilterSelect(filter) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (filterIcon != null) {
                            Icon(
                                imageVector = filterIcon,
                                contentDescription = null,
                                tint = if (isSelected) (if (isDark) Color(0xFF09090B) else Color.White) else Color(0xFFA1A1AA),
                                modifier = Modifier.size(13.dp),
                            )
                        }
                        Text(
                            text = filter.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) (if (isDark) Color(0xFF09090B) else Color.White) else (if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A)),
                        )
                        // Count badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) (if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7))
                                    else (if (isDark) Color(0xFF1E1E1E) else Color(0xFFF4F4F5))
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) (if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)) else Color(0xFF71717A),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Trailing controls: Sort button + Grid/List toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Sort Button
            Surface(
                onClick = onSortClick,
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF),
                border = BorderStroke(0.5.dp, if (isDark) Color(0xFF242424) else Color(0xFFE4E4E7)),
                shadowElevation = 0.5.dp,
                modifier = Modifier
                    .width(72.dp)
                    .height(34.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = sortLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PnotesTokens.textPrimary,
                    )
                    Icon(
                        imageVector = PnotesIcons.arrowDown,
                        contentDescription = "Sort",
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(11.dp),
                    )
                }
            }

            // Grid/List toggle button
            Surface(
                onClick = onToggleGridView,
                shape = RoundedCornerShape(8.dp),
                color = if (isGridView) (if (isDark) Color(0xFF27272A) else Color(0xFF09090B)) else (if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF)),
                border = BorderStroke(
                    0.5.dp,
                    if (isGridView) (if (isDark) Color(0xFF3F3F46) else Color(0xFF09090B)) else (if (isDark) Color(0xFF242424) else Color(0xFFE4E4E7)),
                ),
                shadowElevation = 0.5.dp,
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isGridView) PnotesIcons.thumbnails else PnotesIcons.contents,
                        contentDescription = "Toggle layout",
                        tint = if (isGridView) (if (isDark) Color(0xFFFAFAFA) else Color.White) else PnotesTokens.textPrimary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

/**
 * 5. PNotes Stat Summary Row:
 * - 4 Stat Cards: Total notes (+this week badge), Folders (Organized), Recent notes (Opened lately), Favorites (Quick access)
 * - Quick Action Buttons Card: New note, New canvas, Import PDF
 */
@Composable
fun PNotesStatSummaryRow(
    totalNotesCount: Int,
    foldersCount: Int,
    recentNotesCount: Int,
    favoritesCount: Int,
    thisWeekCount: Int,
    onTotalNotesClick: () -> Unit,
    onFoldersClick: () -> Unit,
    onRecentNotesClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onNewNote: () -> Unit,
    onNewCanvas: () -> Unit,
    onImportPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Total notes card
        PNotesStatCard(
            icon = PnotesIcons.file,
            iconBg = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5),
            iconTint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
            label = "Total notes",
            value = "$totalNotesCount",
            badgeText = "+$thisWeekCount this week",
            badgeColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF09090B),
            onClick = onTotalNotesClick,
            modifier = Modifier.width(220.dp),
        )

        // 2. Folders card
        PNotesStatCard(
            icon = PnotesIcons.folder,
            iconBg = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5),
            iconTint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
            label = "Folders",
            value = "$foldersCount",
            badgeText = "Organized",
            badgeColor = Color(0xFF71717A),
            onClick = onFoldersClick,
            modifier = Modifier.width(200.dp),
        )

        // 3. Recent notes card
        PNotesStatCard(
            icon = PnotesIcons.clock,
            iconBg = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5),
            iconTint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
            label = "Recent notes",
            value = "$recentNotesCount",
            badgeText = "Opened lately",
            badgeColor = Color(0xFF71717A),
            onClick = onRecentNotesClick,
            modifier = Modifier.width(210.dp),
        )

        // 4. Favorites card
        PNotesStatCard(
            icon = PnotesIcons.star,
            iconBg = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5),
            iconTint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
            label = "Favorites",
            value = "$favoritesCount",
            badgeText = "Quick access",
            badgeColor = Color(0xFF71717A),
            onClick = onFavoritesClick,
            modifier = Modifier.width(200.dp),
        )

        // 5. Quick Actions Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF454549) else Color(0xFFFFFFFF),
            border = BorderStroke(0.5.dp, if (isDark) Color(0xFF56565C) else Color(0xFFE1E6EB)),
            shadowElevation = 0.5.dp,
            modifier = Modifier.height(68.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // New note button
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5))
                        .border(0.5.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable(onClick = onNewNote)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(PnotesIcons.pen, null, tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.height(3.dp))
                        Text("New note", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B))
                    }
                }

                // New canvas button
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF141414) else Color(0xFFFAFAFA))
                        .border(0.5.dp, if (isDark) Color(0xFF262626) else Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable(onClick = onNewCanvas)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(PnotesIcons.canvas, null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.height(3.dp))
                        Text("New canvas", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A))
                    }
                }

                // Import PDF button
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF141414) else Color(0xFFFAFAFA))
                        .border(0.5.dp, if (isDark) Color(0xFF262626) else Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable(onClick = onImportPdf)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(PnotesIcons.importDoc, null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.height(3.dp))
                        Text("Import PDF", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A))
                    }
                }
            }
        }
    }
}

/** Individual Stat Card in the Summary Row */
@Composable
private fun PNotesStatCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF454549) else Color(0xFFFFFFFF),
        border = BorderStroke(0.5.dp, if (isDark) Color(0xFF56565C) else Color(0xFFE1E6EB)),
        shadowElevation = 0.5.dp,
        modifier = modifier.height(68.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA1A1AA),
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = value,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PnotesTokens.textPrimary,
                    )
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                    )
                }
            }
        }
    }
}

/**
 * Rich Prototype Vector Previews matching Homescreen prototype:
 * - Style 0: Canvas (Project Nova mesh diagram with spline curves)
 * - Style 1: Inking (Delta Stream v2.4 fluid waveforms in Gold, Cyan, Green)
 * - Style 2: Design (UI Guidelines & Tokens card preview)
 * - Style 3: Math (Catmull-Rom Tangent Matrix curves)
 */
@Composable
fun PNotesVectorPreview(
    styleIndex: Int,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val bg = if (isDark) Color(0xFF000000) else Color(0xFFFAFAFA)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(0.5.dp, if (isDark) Color(0xFF1E1E1E) else Color(0xFFE4E4E7), RoundedCornerShape(10.dp)),
    ) {
        when (styleIndex % 4) {
            0 -> {
                // Style 0: Canvas (Project Nova / Realtime Mesh)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0x3371717A),
                        radius = size.width * 0.35f,
                        center = Offset(size.width * 0.25f, size.height * 0.25f),
                    )
                    // Spline connecting curves
                    val path1 = Path().apply {
                        moveTo(size.width * 0.45f, size.height * 0.22f)
                        cubicTo(
                            size.width * 0.55f, size.height * 0.35f,
                            size.width * 0.60f, size.height * 0.40f,
                            size.width * 0.75f, size.height * 0.30f,
                        )
                    }
                    drawPath(path1, color = Color(0xFFA1A1AA), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                    val path2 = Path().apply {
                        moveTo(size.width * 0.45f, size.height * 0.50f)
                        cubicTo(
                            size.width * 0.52f, size.height * 0.60f,
                            size.width * 0.58f, size.height * 0.65f,
                            size.width * 0.68f, size.height * 0.55f,
                        )
                    }
                    drawPath(path2, color = Color(0xFFD4D4D8), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Tag: PROJECT NOVA
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141414))
                            .border(0.5.dp, Color(0xFF27272A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "PROJECT NOVA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4D4D8),
                        )
                    }

                    // Node 1: Realtime Mesh
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141414))
                            .border(0.5.dp, Color(0xFF262626), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Column {
                            Text("Realtime Mesh", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("WebSocket CRDT", fontSize = 7.sp, color = Color(0xFFA1A1AA))
                        }
                    }

                    // Node 2: Vector Inking Engine (aligned right)
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141414))
                            .border(0.5.dp, Color(0xFF27272A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Column {
                            Text("Vector Inking Engine", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFA1A1AA))
                            Text("Catmull-Rom Spline", fontSize = 7.sp, color = Color(0xFFD4D4D8))
                        }
                    }

                    // Node 3: Snapshot Sync
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141414))
                            .border(0.5.dp, Color(0xFF262626), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Column {
                            Text("Snapshot Sync", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("IndexedDB Cache", fontSize = 7.sp, color = Color(0xFF71717A))
                        }
                    }
                }
            }
            1 -> {
                // Style 1: Inking (Delta Stream waveforms)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Wave 1: Gold
                    val p1 = Path().apply {
                        moveTo(w * 0.08f, h * 0.50f)
                        cubicTo(w * 0.30f, h * 0.15f, w * 0.55f, h * 0.70f, w * 0.92f, h * 0.40f)
                    }
                    drawPath(p1, color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF18181B), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                    // Wave 2: Neutral Silver
                    val p2 = Path().apply {
                        moveTo(w * 0.10f, h * 0.35f)
                        cubicTo(w * 0.40f, h * 0.65f, w * 0.68f, h * 0.20f, w * 0.90f, h * 0.55f)
                    }
                    drawPath(p2, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))

                    // Wave 3: Subtle Gray
                    val p3 = Path().apply {
                        moveTo(w * 0.12f, h * 0.70f)
                        cubicTo(w * 0.35f, h * 0.85f, w * 0.65f, h * 0.60f, w * 0.88f, h * 0.75f)
                    }
                    drawPath(p3, color = if (isDark) Color(0xFF71717A) else Color(0xFF71717A), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                ) {
                    Text(
                        text = "DELTA STREAM v2.4",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF71717A),
                    )
                }
            }
            2 -> {
                // Style 2: Design (UI Guidelines & Tokens)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF0A0A0A) else Color(0xFFFFFFFF))
                        .border(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "UI GUIDELINES & TOKENS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = if (isDark) Color.White else Color(0xFF09090B),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(if (isDark) Color(0x18FFFFFF) else Color(0xFFE4E4E7)),
                        )
                        // 4 color token circles
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF09090B)))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF3F3F46)))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF71717A)))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFD4D4D8)))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Aa Display · Inter 28b", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF18181B))
                            Text("Bb Body · Inter 13r / lh 1.5", fontSize = 7.sp, color = Color(0xFFA1A1AA))
                            Text("Cc Caption · Inter 10sb / #71717A", fontSize = 6.5.sp, color = Color(0xFF71717A))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "\$color.surface.brand = #09090B",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color(0xFF09090B),
                            )
                        }
                    }
                }
            }
            else -> {
                // Style 3: Math (Catmull-Rom Tangent Matrix)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Subtle coordinate axes
                    drawLine(
                        color = Color(0x1871717A),
                        start = Offset(w * 0.1f, h * 0.8f),
                        end = Offset(w * 0.9f, h * 0.8f),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawLine(
                        color = Color(0x1871717A),
                        start = Offset(w * 0.1f, h * 0.8f),
                        end = Offset(w * 0.1f, h * 0.2f),
                        strokeWidth = 1.dp.toPx(),
                    )

                    // Spline curve
                    val path = Path().apply {
                        moveTo(w * 0.15f, h * 0.70f)
                        cubicTo(w * 0.35f, h * 0.25f, w * 0.65f, h * 0.85f, w * 0.85f, h * 0.35f)
                    }
                    drawPath(path, color = if (isDark) Color.White else Color(0xFF18181B), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                    // Tangent control points
                    drawCircle(if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B), radius = 3.dp.toPx(), center = Offset(w * 0.35f, h * 0.25f))
                    drawCircle(if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B), radius = 3.dp.toPx(), center = Offset(w * 0.65f, h * 0.85f))
                    drawCircle(if (isDark) Color.White else Color(0xFF09090B), radius = 4.dp.toPx(), center = Offset(w * 0.85f, h * 0.35f))
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                ) {
                    Text(
                        text = "Catmull-Rom Tangent Matrix P(t)",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF71717A),
                    )
                }
            }
        }
    }
}

/**
 * 6. PNotes Redesigned Note Card:
 * - 332dp × 312dp layout matching reference prototype
 * - 195dp preview box: if thumbnail != null display it, else rich vector preview
 * - Bottom info: Title, star button, 3-dot menu, subtitle, clock icon + date, and category badge
 */
@Composable
fun PNotesRedesignedNoteCard(
    title: String,
    dateText: String,
    thumbnail: ImageBitmap? = null,
    styleIndex: Int = 0,
    selected: Boolean = false,
    isStarred: Boolean = false,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    onStarClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    menuContent: @Composable (() -> Unit)? = null,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val cardInteraction = remember { MutableInteractionSource() }
    val isCardPressed by cardInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
        label = "redesignedNoteCardScale",
    )

    val subtitle = when (styleIndex % 4) {
        0 -> "Hybrid event mesh & inking stream architecture"
        1 -> "Delta CRDT snapshotting and ink stream reconciler"
        2 -> "UI/UX Design Guidelines & Token architecture"
        else -> "Catmull-Rom spline curves and stroke pressure dynamics"
    }

    val badgeLabel = when (styleIndex % 4) {
        0 -> "Canvas"
        1 -> "Inking"
        2 -> "Design"
        else -> "Math"
    }

    val badgeBg = when (styleIndex % 4) {
        0 -> if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)
        1 -> if (isDark) Color(0xFF181818) else Color(0xFFF4F4F5)
        2 -> if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)
        else -> if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)
    }

    val badgeBorder = when (styleIndex % 4) {
        0 -> if (isDark) Color(0xFF3F3F46) else Color(0xFFE4E4E7)
        1 -> if (isDark) Color(0xFF282828) else Color(0xFFE4E4E7)
        2 -> if (isDark) Color(0xFF3F3F46) else Color(0xFFE4E4E7)
        else -> if (isDark) Color(0xFF3F3F46) else Color(0xFFE4E4E7)
    }

    val badgeText = when (styleIndex % 4) {
        0 -> if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
        1 -> if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A)
        2 -> if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
        else -> if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
    }

    val badgeIcon: ImageVector = when (styleIndex % 4) {
        0 -> PnotesIcons.canvas
        1 -> PnotesIcons.pen
        2 -> PnotesIcons.palette
        else -> PnotesIcons.star
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            if (isDark) Color(0xFF525258) else Color(0xFFF4F4F5)
        } else {
            if (isDark) Color(0xFF454549) else Color(0xFFFFFFFF)
        },
        border = BorderStroke(
            0.75.dp,
            if (selected) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFF56565C) else Color(0xFFE1E6EB)),
        ),
        shadowElevation = if (selected) 4.dp else 0.5.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .fillMaxWidth()
            .clickable(
                interactionSource = cardInteraction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            // Preview Box (180dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp)),
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    PNotesVectorPreview(
                        styleIndex = styleIndex,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Row 1: Title + Star + 3-Dot Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PnotesTokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Star button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isStarred) {
                                    if (isDark) Color.White else Color(0xFF09090B)
                                } else {
                                    if (isDark) Color(0xFF161616) else Color(0xFFF4F4F5)
                                }
                            )
                            .border(
                                0.5.dp,
                                if (isStarred) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFF262626) else Color(0xFFE4E4E7)),
                                RoundedCornerShape(6.dp),
                            )
                            .clickable(onClick = onStarClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.star,
                            contentDescription = "Star note",
                            tint = if (isStarred) (if (isDark) Color(0xFF09090B) else Color.White) else Color(0xFF71717A),
                            modifier = Modifier.size(13.dp),
                        )
                    }

                    // 3-dot Menu button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF161616) else Color(0xFFF4F4F5))
                            .border(
                                0.5.dp,
                                if (isDark) Color(0xFF262626) else Color(0xFFE4E4E7),
                                RoundedCornerShape(6.dp),
                            )
                            .clickable(onClick = onMenuClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.moreHorizontal,
                            contentDescription = "More options",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(13.dp),
                        )
                        menuContent?.invoke()
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Row 2: Subtitle
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(10.dp))

            // Row 3: Clock + Date + Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = PnotesIcons.clock,
                        contentDescription = null,
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = dateText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF71717A),
                    )
                }

                // Category badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .border(0.5.dp, badgeBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeText,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            text = badgeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeText,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Exact Quick Inking FAB matching the tactile 3D floating pill specification.
 * 142.11dp × 48dp rounded 24dp pill button with specular top rim, deep multi-layer drop shadow,
 * and spring click/hover scaling.
 */
@Composable
fun PNotesQuickInkingFab(
    onClick: () -> Unit = {},
    onQuickInking: () -> Unit = onClick,
    modifier: Modifier = Modifier,
) {
    val effectiveClick = if (onQuickInking != onClick) onQuickInking else onClick
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val targetScale = when {
        isPressed -> 0.94f
        isHovered -> 1.04f
        else -> 1.0f
    }
    val fabScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "quickInkingFabScale",
    )

    val surfaceBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1E1E26),
                Color(0xFF141418),
                Color(0xFF0C0C0E),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF27272A),
                Color(0xFF09090B),
                Color(0xFF020617),
            )
        )
    }

    val rimBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.55f),
            Color.White.copy(alpha = 0.10f),
            Color.Black.copy(alpha = 0.85f),
        )
    )

    val pillShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = fabScale
                scaleY = fabScale
            }
            .width(142.11.dp)
            .height(48.dp)
            .drawBehind {
                val cornerRadiusPx = 24.dp.toPx()
                val blurRadius = 32.dp.toPx()
                val yOffset = 12.dp.toPx()
                val shadowColor = if (isDark) {
                    android.graphics.Color.argb((255 * 0.85f).toInt(), 0, 0, 0)
                } else {
                    android.graphics.Color.argb((255 * 0.48f).toInt(), 15, 23, 42)
                }

                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.TRANSPARENT
                        isAntiAlias = true
                        setShadowLayer(
                            blurRadius,
                            0f,
                            yOffset,
                            shadowColor,
                        )
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        cornerRadiusPx,
                        cornerRadiusPx,
                        paint,
                    )
                }
            }
            .clip(pillShape)
            .background(surfaceBrush)
            .border(
                width = 1.dp,
                brush = rimBorderBrush,
                shape = pillShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = effectiveClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = PnotesIcons.pnotesPenNib,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Quick Inking",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

/**
 * Exact Filter Tabs Row matching the prototype SVG.
 * - Outer container: fillMaxWidth(), padding horizontal = 40.dp, vertical = 8.dp (or adaptive).
 * - Left side: Segmented tab capsule:
 *   - All Notes (active state with prototypePill3D(isDark, true, 9.dp) and count badge pill)
 *   - Notes (icon 14×14dp, text "Notes" 13.sp Medium, count badge)
 *   - Canvases (icon 14×11.8dp, text "Canvases" 13.sp Medium, count badge)
 *   - PDFs & Docs (icon 13.4×14dp, text "PDFs & Docs" 13.sp Medium)
 *   - Favorites (icon 14×13.5dp, text "Favorites" 13.sp Medium)
 * - Right side:
 *   - Sort dropdown button (146.26dp × 30dp, 9dp radius) with prototypePill3D(isDark, false, 9.dp), sort arrows icon (14×14dp), label e.g. "Last modified" (13.sp Medium), and chevron (12×6.5dp).
 *   - Tapping opens sort menu with options: "Last modified", "Name", "Size" + order reversal.
 */
@Composable
fun PNotesPrototypeFilterTabsRow(
    activeFilter: HomeFilter = HomeFilter.ALL,
    activeTab: HomeFilter = activeFilter,
    onFilterSelect: (HomeFilter) -> Unit = {},
    onTabSelect: (HomeFilter) -> Unit = onFilterSelect,
    allCount: Int = 0,
    notesCount: Int = 0,
    canvasesCount: Int = 0,
    pdfsCount: Int = 0,
    docsCount: Int = pdfsCount,
    favCount: Int = 0,
    favoritesCount: Int = favCount,
    sortKey: ExplorerSortKey? = null,
    sortAscending: Boolean = false,
    onSortChange: ((ExplorerSortKey, Boolean) -> Unit)? = null,
    sortLabel: String = when (sortKey) {
        ExplorerSortKey.NAME -> "Name"
        ExplorerSortKey.SIZE -> "Size"
        ExplorerSortKey.MODIFIED -> "Last modified"
        null -> "Last modified"
    },
    onSortClick: (() -> Unit)? = null,
    onSortOptionSelect: ((String) -> Unit)? = null,
    isSortAscending: Boolean = sortAscending,
    onToggleSortOrder: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    var internalSortMenuOpen by remember { mutableStateOf(false) }
    val showInternalDropdown = onSortClick == null || onSortOptionSelect != null || onSortChange != null

    val currentFilter = if (activeTab != HomeFilter.ALL) activeTab else activeFilter
    val onSelect = { f: HomeFilter ->
        onTabSelect(f)
        onFilterSelect(f)
    }
    val effectivePdfsCount = if (docsCount != 0) docsCount else pdfsCount
    val effectiveFavCount = if (favoritesCount != 0) favoritesCount else favCount
    val effectiveAscending = isSortAscending || sortAscending

    val tabs = listOf(
        FilterTabSpec(HomeFilter.ALL, "All Notes", null, 0.dp, 0.dp, allCount),
        FilterTabSpec(HomeFilter.NOTES, "Notes", PnotesIcons.file, 14.dp, 14.dp, notesCount),
        FilterTabSpec(HomeFilter.CANVASES, "Canvases", PnotesIcons.canvas, 14.dp, 11.8.dp, canvasesCount),
        FilterTabSpec(HomeFilter.PDFS, "PDFs & Docs", PnotesIcons.importDoc, 13.4.dp, 14.dp, effectivePdfsCount.takeIf { it > 0 }),
        FilterTabSpec(HomeFilter.FAVORITES, "Favorites", PnotesIcons.star, 14.dp, 13.5.dp, effectiveFavCount.takeIf { it > 0 }),
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val isCompact = maxWidth < 640.dp
        val horizontalPad = 0.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPad, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left Side: Segmented Tab Capsule
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(39.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDark) {
                            Brush.verticalGradient(listOf(Color(0xFF202024), Color(0xFF1B1B1E)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFFE4E4E7), Color(0xFFF4F4F5)))
                        }
                    )
                    .border(
                        0.5.dp,
                        if (isDark) Color(0xFF38383E) else Color(0xFFD4D4D8),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentFilter == tab.filter

                        val contentColor = if (isSelected) {
                            if (isDark) Color(0xFF0B0F17) else Color(0xFFFFFFFF)
                        } else {
                            if (isDark) Color(0xFFE4E4E7) else Color(0xFF52525B)
                        }

                        val countTextColor = if (isSelected) {
                            if (isDark) Color(0xFF52525B) else Color(0xFFD4D4D8)
                        } else {
                            if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
                        }

                        Box(
                            modifier = Modifier
                                .height(31.dp)
                                .prototypePill3D(
                                    isDark = isDark,
                                    isActive = isSelected,
                                    cornerRadius = 9.dp,
                                )
                                .clickable { onSelect(tab.filter) }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (tab.icon != null) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(width = tab.iconWidth, height = tab.iconHeight),
                                    )
                                }
                                Text(
                                    text = tab.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = contentColor,
                                )
                                if (tab.count != null) {
                                    Text(
                                        text = "${tab.count}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = countTextColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Right Side: Sort Dropdown Button (146.26dp × 30dp, 9dp radius)
            Box(
                modifier = Modifier
                    .size(width = 146.26.dp, height = 30.dp)
                    .prototypePill3D(
                        isDark = isDark,
                        isActive = false,
                        cornerRadius = 9.dp,
                    )
                    .clickable {
                        if (onSortClick != null) {
                            onSortClick()
                        } else {
                            internalSortMenuOpen = true
                        }
                    }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = PnotesIcons.sortVertical,
                            contentDescription = "Sort",
                            tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = sortLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF27272A),
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = PnotesIcons.chevronDown,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF71717A) else Color(0xFFA1A1AA),
                        modifier = Modifier.size(width = 12.dp, height = 6.5.dp),
                    )
                }

                if (showInternalDropdown) {
                    DropdownMenu(
                        expanded = internalSortMenuOpen,
                        onDismissRequest = { internalSortMenuOpen = false },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        val options = listOf("Last modified", "Name", "Size")
                        options.forEach { option ->
                            val isSelectedOption = sortLabel.equals(option, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = option,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelectedOption) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                                        )
                                        if (isSelectedOption) {
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                imageVector = PnotesIcons.check,
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    internalSortMenuOpen = false
                                    if (onSortChange != null) {
                                        val key = when (option) {
                                            "Name" -> ExplorerSortKey.NAME
                                            "Size" -> ExplorerSortKey.SIZE
                                            else -> ExplorerSortKey.MODIFIED
                                        }
                                        onSortChange(key, key == ExplorerSortKey.NAME)
                                    } else {
                                        onSortOptionSelect?.invoke(option)
                                    }
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = if (effectiveAscending) PnotesIcons.arrowUp else PnotesIcons.arrowDown,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = if (effectiveAscending) "Ascending" else "Descending",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                                    )
                                }
                            },
                            onClick = {
                                internalSortMenuOpen = false
                                if (onSortChange != null) {
                                    val key = sortKey ?: ExplorerSortKey.MODIFIED
                                    onSortChange(key, !effectiveAscending)
                                } else {
                                    onToggleSortOrder?.invoke()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private data class FilterTabSpec(
    val filter: HomeFilter,
    val label: String,
    val icon: ImageVector?,
    val iconWidth: Dp,
    val iconHeight: Dp,
    val count: Int?,
)

/**
 * Grid / List View Toggle button capsule matching the prototype styling.
 * Used beside the "+ New Folder" action button and when viewing lists.
 */
@Composable
fun PNotesViewToggle(
    isGridView: Boolean,
    onToggleGridView: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 28.dp,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val width = (height.value * (64f / 28f)).dp
    val itemHeight = (height.value - 4f).dp
    val itemWidth = ((width.value - 6f) / 2f).dp
    val iconSize = (height.value * 0.5f).dp

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .prototypePill3D(isDark = isDark, isActive = false, cornerRadius = 8.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(width = itemWidth, height = itemHeight)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isGridView) (if (isDark) Color(0xFF27272A) else Color(0xFFFFFFFF))
                        else Color.Transparent
                    )
                    .clickable { if (!isGridView) onToggleGridView() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.layoutGrid,
                    contentDescription = "Grid view",
                    tint = if (isGridView) (if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)) else Color(0xFF71717A),
                    modifier = Modifier.size(iconSize),
                )
            }
            Box(
                modifier = Modifier
                    .size(width = itemWidth, height = itemHeight)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (!isGridView) (if (isDark) Color(0xFF27272A) else Color(0xFFFFFFFF))
                        else Color.Transparent
                    )
                    .clickable { if (isGridView) onToggleGridView() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.layoutList,
                    contentDescription = "List view",
                    tint = if (!isGridView) (if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)) else Color(0xFF71717A),
                    modifier = Modifier.size(width = iconSize, height = iconSize * 0.75f),
                )
            }
        }
    }
}

/**
 * Folders section header matching the prototype SVG.
 * Left: "Folders" (16sp SemiBold) + count badge pill (21.3×17dp, 10dp radius, 11sp SemiBold)
 * Right: View Toggle (64×28dp) + "+ New Folder" action button (111.05×28dp, 8dp radius with prototypePill3D)
 */
@Composable
fun PNotesFoldersHeader(
    folderCount: Int,
    onNewFolder: () -> Unit,
    isGridView: Boolean = true,
    onToggleGridView: () -> Unit = {},
    showViewToggle: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Folders",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                letterSpacing = (-0.3).sp,
            )
            Box(
                modifier = Modifier
                    .widthIn(min = 22.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0x33000000) else Color(0xFFF1F5F9))
                    .border(0.8.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$folderCount",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF64748B),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showViewToggle) {
                PNotesViewToggle(
                    isGridView = isGridView,
                    onToggleGridView = onToggleGridView,
                    height = 28.dp,
                )
            }

            Row(
                modifier = Modifier
                    .height(32.dp)
                    .prototypePill3D(isDark = isDark, isActive = false, cornerRadius = 8.dp)
                    .clickable(onClick = onNewFolder)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.plus,
                    contentDescription = "New Folder",
                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF0F172A),
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "New Folder",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF0F172A),
                )
            }
        }
    }
}

/**
 * Compact Folder Card matching reference design.
 * Outer card: fillMaxWidth(), 60dp height, 12dp corner radius.
 * Card padding: horizontal = 12.dp, vertical = 9.dp.
 * Left folder icon badge: 40×40dp box (9dp radius) with folder icon.
 * Center metadata: folder name (13.5sp SemiBold) + count pill ("$count notes", 10.5sp Medium).
 * No arrow, no clutter — whole card is tappable to open folder, long-press for menu.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PNotesPrototypeFolderCard(
    folderName: String,
    noteCount: Int = 0,
    countLabel: String? = null,
    timeAgo: String = "",
    colorTag: Color? = null,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuContent: @Composable (() -> Unit)? = null,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val cardInteraction = remember { MutableInteractionSource() }
    val isCardPressed by cardInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
        label = "folderCardScale",
    )

    val cardBgBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF4C4C52),
                Color(0xFF404045),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF9F9FB),
            )
        )
    }

    val cardBorderBrush = if (colorTag != null) {
        Brush.verticalGradient(listOf(colorTag.copy(alpha = 0.6f), colorTag.copy(alpha = 0.3f)))
    } else if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color(0xFF525258),
                Color.White.copy(alpha = 0.05f),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFE2E8F0),
                Color(0xFFCBD5E1),
            )
        )
    }

    val folderShape = RoundedCornerShape(13.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .drawBehind {
                val cornerRadiusPx = 13.dp.toPx()
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.TRANSPARENT
                        isAntiAlias = true
                        setShadowLayer(
                            if (isDark) 4.dp.toPx() else 2.dp.toPx(),
                            0f,
                            if (isDark) 1.8.dp.toPx() else 1.dp.toPx(),
                            if (isDark) android.graphics.Color.argb(70, 0, 0, 0) else android.graphics.Color.argb(20, 0, 0, 0),
                        )
                    }
                    canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cornerRadiusPx, cornerRadiusPx, paint)
                }
            }
            .clip(folderShape)
            .background(cardBgBrush)
            .border(0.75.dp, cardBorderBrush, folderShape)
            .combinedClickable(
                interactionSource = cardInteraction,
                indication = null,
                onClick = onClick,
                onLongClick = onMenuClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            // Left: Folder Icon Container (40x40dp compact box)
            val iconContainerBg = if (colorTag != null) {
                colorTag.copy(alpha = if (isDark) 0.22f else 0.12f)
            } else if (isDark) {
                Color(0x2EFBBF24)
            } else {
                Color(0xFFFEF3C7)
            }

            val iconContainerBorder = if (colorTag != null) {
                colorTag.copy(alpha = 0.6f)
            } else if (isDark) {
                Color(0x4DFBBF24)
            } else {
                Color(0xFFFDE68A)
            }

            val folderIconTint = colorTag ?: (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconContainerBg)
                    .border(0.8.dp, iconContainerBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.folder,
                    contentDescription = null,
                    tint = folderIconTint,
                    modifier = Modifier.size(width = 20.dp, height = 16.dp),
                )
            }

            // Middle: Folder Name + Note Count Pill
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = folderName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                    ),
                )

                Spacer(Modifier.height(4.dp))

                // Count Pill
                val displayCount = countLabel ?: "$noteCount ${if (noteCount == 1) "note" else "notes"}"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0x33000000) else Color(0xFFF1F5F9))
                        .border(
                            0.5.dp,
                            if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayCount,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                        ),
                        color = if (isDark) Color(0xFFD4D4D8) else Color(0xFF64748B),
                    )
                }
            }
        }

        // Anchor for context menu (rename, delete, etc.)
        menuContent?.invoke()
    }
}

/**
 * Recent Notes & Canvases section header matching the prototype SVG.
 * Left: "Recent Notes & Canvases" (16.sp SemiBold #0F172A / #F8FAFC) + 52.22×17dp count badge pill (10dp radius, #F1F5F9 / #27272A, 11.sp SemiBold #64748B / #F8FAFC)
 * Right: "Last updated: <time>" (12.sp #94A3B8) + 72.89×27dp "Select" pill button (8dp radius) with prototypePill3D(isDark, isSelectionMode, 8.dp), 13×13dp check icon, and 12.sp Medium label.
 */
@Composable
fun PNotesRecentSectionHeader(
    itemCount: Int = 0,
    totalCount: Int = itemCount,
    lastUpdatedText: String? = null,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    isAllSelected: Boolean = false,
    onSelectAll: () -> Unit = {},
    onToggleSelectMode: () -> Unit = {},
    onToggleSelectionMode: () -> Unit = onToggleSelectMode,
    breadcrumbsContent: @Composable (() -> Unit)? = null,
    showViewToggle: Boolean = false,
    isGridView: Boolean = true,
    onToggleGridView: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val count = if (totalCount != 0) totalCount else itemCount
    val onToggle = if (onToggleSelectionMode != onToggleSelectMode) onToggleSelectionMode else onToggleSelectMode

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left
        if (breadcrumbsContent != null) {
            breadcrumbsContent()
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (isSelectionMode) "Select Notes" else "Recent Notes & Canvases",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                    letterSpacing = (-0.2).sp,
                )
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelectionMode) {
                                if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                            } else {
                                if (isDark) Color(0xFF38383C) else Color(0xFFF4F4F5)
                            }
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isSelectionMode) "$selectedCount of $count selected" else "$count items",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelectionMode) {
                            if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                        } else {
                            if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
                        },
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                            lineHeight = 11.sp,
                        ),
                    )
                }
            }
        }

        // Right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showViewToggle) {
                PNotesViewToggle(
                    isGridView = isGridView,
                    onToggleGridView = onToggleGridView,
                    height = 27.dp,
                )
            }

            if (isSelectionMode) {
                // "Select All" / "Deselect All" button
                Box(
                    modifier = Modifier
                        .height(27.dp)
                        .prototypePill3D(
                            isDark = isDark,
                            isActive = isAllSelected,
                            cornerRadius = 8.dp,
                        )
                        .clickable(onClick = onSelectAll)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isAllSelected) "Deselect All" else "Select All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isAllSelected) {
                            if (isDark) Color(0xFF09090B) else Color(0xFFFFFFFF)
                        } else {
                            if (isDark) Color(0xFFE4E4E7) else Color(0xFF09090B)
                        },
                    )
                }
            } else if (lastUpdatedText != null) {
                val formattedTime = if (lastUpdatedText.startsWith("Last updated:", ignoreCase = true)) {
                    lastUpdatedText
                } else {
                    "Last updated: $lastUpdatedText"
                }
                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFA1A1AA),
                )
            }

            Box(
                modifier = Modifier
                    .size(width = 72.89.dp, height = 27.dp)
                    .prototypePill3D(
                        isDark = isDark,
                        isActive = isSelectionMode,
                        cornerRadius = 8.dp,
                    )
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (isSelectionMode) PnotesIcons.close else PnotesIcons.check,
                        contentDescription = null,
                        tint = if (isSelectionMode) {
                            if (isDark) Color(0xFF09090B) else Color(0xFFFFFFFF)
                        } else {
                            if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
                        },
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = if (isSelectionMode) "Done" else "Select",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelectionMode) {
                            if (isDark) Color(0xFF09090B) else Color(0xFFFFFFFF)
                        } else {
                            if (isDark) Color(0xFFE4E4E7) else Color(0xFF09090B)
                        },
                    )
                }
            }
        }
    }
}

private data class CoverPetalMatrix(
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val e: Float,
    val f: Float,
)

private val COVER_YELLOW_PETAL_MATRICES = listOf(
    CoverPetalMatrix(28.977775f, -7.764571f, 17.599695f, 65.682956f, 76.714295f, 25.03803f),
    CoverPetalMatrix(19.938771f, 9.297602f, -20.285677f, 43.502774f, 186.17078f, 27.59457f),
    CoverPetalMatrix(16.383041f, -11.471529f, 26.384516f, 37.680994f, 3.61538f, 94.8985f),
    CoverPetalMatrix(15.588457f, 9.0f, -21.0f, 36.373067f, 236.70548f, 108.31408f),
    CoverPetalMatrix(18.384776f, 18.384776f, -42.426407f, 42.426407f, 63.017357f, 229.59757f),
    CoverPetalMatrix(15.756924f, 2.778371f, -6.598631f, 37.422695f, 171.42152f, 213.8966f),
    CoverPetalMatrix(334.75203f, -83.463054f, 37.497894f, 150.39584f, -33.6226f, 249.03134f),
)

private const val COVER_PENCIL_PATH_DATA =
    "M204.42,149.49 L209.9,152.65 C210.66,153.09 210.92,154.08 210.48,154.84 " +
    "L201.76,169.94 C201.55,170.31 201.2,170.57 200.79,170.68 " +
    "L196.47,171.83 C195.62,172.06 194.75,171.56 194.52,170.71 " +
    "L193.36,166.39 C193.25,165.98 193.31,165.55 193.52,165.18 " +
    "L202.24,150.07 C202.68,149.31 203.66,149.05 204.42,149.49 Z " +
    "M200.38,169.14 L196.06,170.3 L194.91,165.98 L201.13,155.2 " +
    "L206.6,158.36 L200.38,169.14 Z " +
    "M207.4,156.97 L201.93,153.82 L203.62,150.88 L209.1,154.03 L207.4,156.97 Z"

private const val COVER_BOOKMARK_PATH_DATA =
    "M10.78,14 L23.22,14 C24.21,14 25,14.57 25,15.28 " +
    "L25,29.36 C25,29.59 24.82,29.81 24.54,29.92 " +
    "C24.26,30.03 23.92,30.02 23.64,29.9 " +
    "L17,26.91 L10.36,29.9 C10.08,30.02 9.74,30.03 9.46,29.92 " +
    "C9.18,29.81 9,29.59 9,29.36 L9,15.28 C9,14.57 9.79,14 10.78,14 Z"

private val coverPencilNativePath: android.graphics.Path by lazy {
    PathParser.createPathFromPathData(COVER_PENCIL_PATH_DATA).apply {
        fillType = android.graphics.Path.FillType.EVEN_ODD
    }
}

private val coverRibbonBodyPath: android.graphics.Path by lazy {
    android.graphics.Path().apply {
        val radii = floatArrayOf(
            0f, 0f, // top-left
            0f, 0f, // top-right
            8f, 8f, // bottom-right
            8f, 8f  // bottom-left
        )
        addRoundRect(
            RectF(212f, 0f, 246f, 46f),
            radii,
            android.graphics.Path.Direction.CW
        )
    }
}

private val coverRibbonEmblemPath: android.graphics.Path by lazy {
    val p = PathParser.createPathFromPathData(COVER_BOOKMARK_PATH_DATA)
    val m = android.graphics.Matrix().apply {
        setTranslate(212f, 0f)
    }
    p.transform(m)
    p
}

/**
 * Exact Live Vector Notebook Cover matching prototype/Notes preview/Cover.tsx and Cover.svg.
 * Dynamically displays the Save Mark (bookmark ribbon) when a color is chosen (colorTag != null).
 * When colorTag == null, the save mark is hidden.
 */
@Composable
fun PnotesLiveNotebookCover(
    colorTag: Color?,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val context = LocalContext.current
    val typeface = remember(context) {
        try {
            ResourcesCompat.getFont(context, com.pnotes.R.font.inter_700) ?: android.graphics.Typeface.DEFAULT_BOLD
        } catch (_: Exception) {
            android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    val yellowPaint = remember {
        android.graphics.Paint().apply {
            color = 0xFFE2E800.toInt()
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
    }

    val cyanDotPaint = remember {
        android.graphics.Paint().apply {
            color = 0xFF028090.toInt()
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
    }

    val pencilPaint = remember {
        android.graphics.Paint().apply {
            color = 0xFFB8F2E6.toInt()
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
    }

    val textPaint = remember(typeface) {
        android.graphics.Paint().apply {
            color = 0xFFB8F2E6.toInt()
            isAntiAlias = true
            textSize = 32f
            this.typeface = typeface
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(3f, 0f, 3f, android.graphics.Color.argb((0.35f * 255).toInt(), 1, 86, 97))
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val scaleX = width / 270f
        val scaleY = height / 295f

        // 1. Background gradient (150deg: #00A896 -> #028090)
        val bgBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFF00A896),
                1.0f to Color(0xFF028090),
            ),
            start = Offset(width * 0.25f, height * 0.067f),
            end = Offset(width * 0.75f, height * 0.933f),
        )
        // Draw full rect - top corners are clipped to 20dp, bottom corners are strictly flat/straight
        drawRect(brush = bgBrush)

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()
            nativeCanvas.scale(scaleX, scaleY)
            val cornerPath = android.graphics.Path().apply {
                // Top-left and top-right are rounded (20f), bottom-right and bottom-left are flat (0f)
                addRoundRect(
                    RectF(0f, 0f, 270f, 295f),
                    floatArrayOf(20f, 20f, 20f, 20f, 0f, 0f, 0f, 0f),
                    android.graphics.Path.Direction.CW,
                )
            }
            nativeCanvas.clipPath(cornerPath)

            // 2. Yellow organic petals (#E2E800)
            val matrix = android.graphics.Matrix()
            for (m in COVER_YELLOW_PETAL_MATRICES) {
                nativeCanvas.save()
                matrix.setValues(floatArrayOf(m.a, m.c, m.e, m.b, m.d, m.f, 0f, 0f, 1f))
                nativeCanvas.concat(matrix)
                nativeCanvas.drawOval(0f, 0f, 1f, 1f, yellowPaint)
                nativeCanvas.restore()
            }

            // 3. Three Cyan Dots (#028090)
            nativeCanvas.drawCircle(180f, 265f, 5f, cyanDotPaint)
            nativeCanvas.drawCircle(209f, 254f, 4f, cyanDotPaint)
            nativeCanvas.drawCircle(238f, 274f, 6f, cyanDotPaint)

            // 4. Typography ("NOTE BOOK ,") - Inter 700/Bold
            nativeCanvas.save()
            nativeCanvas.translate(28.79f, 125.225f)
            nativeCanvas.rotate(-14f)
            nativeCanvas.drawText("NOTE", 56.805f, 25.921f, textPaint)
            nativeCanvas.drawText("BOOK ,", 56.805f, 54.921f, textPaint)
            nativeCanvas.restore()

            // 5. Pencil Icon (#B8F2E6)
            nativeCanvas.drawPath(coverPencilNativePath, pencilPaint)

            // 6. Save Mark (Bookmark Ribbon hanging from top-right)
            // ONLY rendered if colorTag != null
            if (colorTag != null) {
                val topR = (colorTag.red * 255).toInt().coerceIn(0, 255)
                val topG = (colorTag.green * 255).toInt().coerceIn(0, 255)
                val topB = (colorTag.blue * 255).toInt().coerceIn(0, 255)
                val topColorInt = android.graphics.Color.argb(255, topR, topG, topB)

                val botR = (colorTag.red * 255 * 0.82f).toInt().coerceIn(0, 255)
                val botG = (colorTag.green * 255 * 0.82f).toInt().coerceIn(0, 255)
                val botB = (colorTag.blue * 255 * 0.82f).toInt().coerceIn(0, 255)
                val bottomColorInt = android.graphics.Color.argb(255, botR, botG, botB)

                val ribbonShader = android.graphics.LinearGradient(
                    212f, 0f, 212f, 46f,
                    topColorInt, bottomColorInt,
                    android.graphics.Shader.TileMode.CLAMP
                )

                // Soft tactile shadow under ribbon
                val shadowColorInt = android.graphics.Color.argb(100, topR, topG, topB)
                val shadowPaint = android.graphics.Paint().apply {
                    color = shadowColorInt
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.FILL
                }
                nativeCanvas.drawRoundRect(211f, 1f, 247f, 47.5f, 8f, 8f, shadowPaint)

                // Ribbon body
                val ribbonPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.FILL
                    shader = ribbonShader
                }
                nativeCanvas.drawPath(coverRibbonBodyPath, ribbonPaint)

                // Inner bookmark emblem cutout
                if (!isCompact) {
                    val lum = 0.299f * colorTag.red + 0.587f * colorTag.green + 0.114f * colorTag.blue
                    val emblemColorInt = if (lum > 0.40f) {
                        android.graphics.Color.argb(
                            255,
                            (colorTag.red * 255 * 0.35f).toInt().coerceIn(0, 255),
                            (colorTag.green * 255 * 0.35f).toInt().coerceIn(0, 255),
                            (colorTag.blue * 255 * 0.35f).toInt().coerceIn(0, 255)
                        )
                    } else {
                        android.graphics.Color.argb(240, 255, 255, 255)
                    }
                    val emblemPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                        color = emblemColorInt
                    }
                    nativeCanvas.drawPath(coverRibbonEmblemPath, emblemPaint)
                }
            }

            nativeCanvas.restore()
        }
    }
}

/**
 * Save Mark (Bookmark Ribbon hanging from top-right) matching prototype/Notes preview/Cover.tsx.
 * Renders dynamically with the chosen color (colorTag).
 */
@Composable
fun PnotesSaveMarkRibbon(
    colorTag: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val scaleX = width / 270f
        val scaleY = height / 295f

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()
            nativeCanvas.scale(scaleX, scaleY)

            val topR = (colorTag.red * 255).toInt().coerceIn(0, 255)
            val topG = (colorTag.green * 255).toInt().coerceIn(0, 255)
            val topB = (colorTag.blue * 255).toInt().coerceIn(0, 255)
            val topColorInt = android.graphics.Color.argb(255, topR, topG, topB)

            val botR = (colorTag.red * 255 * 0.82f).toInt().coerceIn(0, 255)
            val botG = (colorTag.green * 255 * 0.82f).toInt().coerceIn(0, 255)
            val botB = (colorTag.blue * 255 * 0.82f).toInt().coerceIn(0, 255)
            val bottomColorInt = android.graphics.Color.argb(255, botR, botG, botB)

            val ribbonShader = android.graphics.LinearGradient(
                212f, 0f, 212f, 46f,
                topColorInt, bottomColorInt,
                android.graphics.Shader.TileMode.CLAMP
            )

            // Soft tactile shadow under ribbon
            val shadowColorInt = android.graphics.Color.argb(100, topR, topG, topB)
            val shadowPaint = android.graphics.Paint().apply {
                color = shadowColorInt
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
            }
            nativeCanvas.drawRoundRect(211f, 1f, 247f, 47.5f, 8f, 8f, shadowPaint)

            // Ribbon body
            val ribbonPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                shader = ribbonShader
            }
            nativeCanvas.drawPath(coverRibbonBodyPath, ribbonPaint)

            // Inner bookmark emblem cutout
            val lum = 0.299f * colorTag.red + 0.587f * colorTag.green + 0.114f * colorTag.blue
            val emblemColorInt = if (lum > 0.40f) {
                android.graphics.Color.argb(
                    255,
                    (colorTag.red * 255 * 0.35f).toInt().coerceIn(0, 255),
                    (colorTag.green * 255 * 0.35f).toInt().coerceIn(0, 255),
                    (colorTag.blue * 255 * 0.35f).toInt().coerceIn(0, 255)
                )
            } else {
                android.graphics.Color.argb(240, 255, 255, 255)
            }
            val emblemPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                color = emblemColorInt
            }
            nativeCanvas.drawPath(coverRibbonEmblemPath, emblemPaint)

            nativeCanvas.restore()
        }
    }
}

/**
 * Exact Note Card matching the prototype SVG and user specifications:
 * - NO outer frame, NO border around the notebook cover.
 * - Exact 270x295 proportion (aspectRatio(270f / 295f)), height calculated naturally.
 * - 20dp corner radius (RoundedCornerShape(20.dp)).
 * - Dynamic Save Mark (bookmark ribbon) when a color is chosen.
 * - Action buttons row underneath: Title, Subtitle, Star toggle, 3-dots menu.
 */
@Composable
fun PNotesPrototypeNoteCard(
    title: String,
    subtitleText: String,
    thumbnail: ImageBitmap? = null,
    colorTag: Color? = null,
    styleIndex: Int = 0,
    selected: Boolean = false,
    isSelectionMode: Boolean = false,
    isStarred: Boolean = false,
    isNoteDocument: Boolean = true,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    onStarClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    menuContent: @Composable (() -> Unit)? = null,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val cardInteraction = remember { MutableInteractionSource() }
    val isCardPressed by cardInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
        label = "prototypeNoteCardScale",
    )

    val thumbAlpha by animateFloatAsState(
        targetValue = if (thumbnail != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "thumbFadeIn",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(10.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.25f),
            )
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (selected) {
                    Modifier.border(
                        width = 2.dp,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        shape = RoundedCornerShape(10.dp),
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = cardInteraction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        // 1. Notebook Cover (Exact 270:295 shape, flush to top, left, right edges - no surrounding frame)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(270f / 295f),
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = thumbAlpha },
                )
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.10f))
                    )
                }
                if (colorTag != null) {
                    PnotesSaveMarkRibbon(
                        colorTag = colorTag,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else if (isNoteDocument) {
                PnotesLiveNotebookCover(
                    colorTag = colorTag,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PNotesVectorPreview(
                    styleIndex = styleIndex,
                    modifier = Modifier.fillMaxSize(),
                )
                if (colorTag != null) {
                    PnotesSaveMarkRibbon(
                        colorTag = colorTag,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Selection Mode Checkbox Overlay (24dp circle) at TopStart
            if (isSelectionMode || selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) (if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B))
                            else Color(0x66000000)
                        )
                        .border(
                            1.5.dp,
                            if (selected) Color.Transparent else Color.White,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            imageVector = PnotesIcons.check,
                            contentDescription = "Selected",
                            tint = if (isDark) Color(0xFF09090B) else Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        // 2. Bottom Frame / Footer Panel (Matches Card 9.png exact UI)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF454549) else Color.White)
                .padding(horizontal = 7.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Title & Subtitle (Left side)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF09090B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(1.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        if (colorTag != null) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(colorTag)
                            )
                        }
                        Text(
                            text = subtitleText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Action Buttons Row (Star & Vertical 3-dots squircle buttons)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Star Button (22x22dp squircle matching Card 9.png)
                    val starBtnBg = if (isDark) Color(0xFF4E4E54) else Color(0xFFF4F4F5)
                    val starBtnBorder = if (isDark) Color(0xFF5A5A60) else Color(0xFFE4E4E7)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(starBtnBg)
                            .border(0.7.dp, starBtnBorder, RoundedCornerShape(5.dp))
                            .clickable(onClick = onStarClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isStarred) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x66EAB308),
                                            Color.Transparent,
                                        ),
                                    ),
                                    radius = size.width * 0.5f,
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isStarred) PnotesIcons.starFilled else PnotesIcons.star,
                            contentDescription = if (isStarred) "Unstar" else "Star",
                            tint = if (isStarred) {
                                Color(0xFFEAB308)
                            } else {
                                if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
                            },
                            modifier = Modifier.size(12.dp),
                        )
                    }

                    // Vertical 3-dots Menu Button (22x22dp squircle matching Card 9.png)
                    val moreBtnBg = if (isDark) Color(0xFF4E4E54) else Color(0xFFF4F4F5)
                    val moreBtnBorder = if (isDark) Color(0xFF5A5A60) else Color(0xFFE4E4E7)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(moreBtnBg)
                            .border(0.7.dp, moreBtnBorder, RoundedCornerShape(5.dp))
                            .clickable(onClick = onMenuClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.more,
                            contentDescription = "More options",
                            tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                            modifier = Modifier.size(12.dp),
                        )
                        menuContent?.invoke()
                    }
                }
            }
        }
    }
}

/**
 * Exact Note List Row matching the prototype styling.
 * Uses prototypeCard3D(isDark, 12.dp, 4.dp), 48×48dp thumbnail box with live thumbnail / vector preview,
 * title, category, timestamp, star toggle (12×11.57dp), and 3-dots menu (12×1.8dp).
 */
@Composable
fun PNotesPrototypeNoteListRow(
    title: String,
    subtitleText: String,
    thumbnail: ImageBitmap? = null,
    colorTag: Color? = null,
    styleIndex: Int = 0,
    selected: Boolean = false,
    isSelectionMode: Boolean = false,
    isStarred: Boolean = false,
    isNoteDocument: Boolean = true,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    onStarClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    menuContent: @Composable (() -> Unit)? = null,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val rowInteraction = remember { MutableInteractionSource() }
    val isRowPressed by rowInteraction.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (isRowPressed) 0.99f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
        label = "prototypeNoteListRowScale",
    )

    val thumbAlpha by animateFloatAsState(
        targetValue = if (thumbnail != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "listThumbFadeIn",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .prototypeCard3D(isDark = isDark, cornerRadius = 12.dp, elevation = 4.dp, tintColor = colorTag)
            .clickable(
                interactionSource = rowInteraction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Selection Mode Checkbox (if in selection mode)
                if (isSelectionMode || selected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) (if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B))
                                else Color(0x66000000)
                            )
                            .border(
                                1.5.dp,
                                if (selected) Color.Transparent else (if (isDark) Color(0xFF71717A) else Color(0xFFD4D4D8)),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = PnotesIcons.check,
                                contentDescription = "Selected",
                                tint = if (isDark) Color(0xFF09090B) else Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }

                // 48×48dp Thumbnail Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF121215) else Color(0xFFFFFFFF))
                        .border(
                            width = if (colorTag != null) 1.5.dp else 1.dp,
                            color = colorTag ?: (if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)),
                            shape = RoundedCornerShape(8.dp),
                        ),
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = thumbAlpha },
                        )
                    } else if (isNoteDocument) {
                        PnotesLiveNotebookCover(
                            colorTag = colorTag,
                            isCompact = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        PNotesVectorPreview(
                            styleIndex = styleIndex,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // Middle Text: Title + Category & Timestamp
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (colorTag != null) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colorTag)
                            )
                        }
                        Text(
                            text = subtitleText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right Actions: Star toggle + 3-dots menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Star Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onStarClick),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isStarred) {
                        Canvas(modifier = Modifier.size(22.dp)) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x66EAB308),
                                        Color.Transparent,
                                    ),
                                ),
                                radius = size.width * 0.5f,
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isStarred) PnotesIcons.starFilled else PnotesIcons.star,
                        contentDescription = if (isStarred) "Unstar" else "Star",
                        tint = if (isStarred) {
                            Color(0xFFEAB308)
                        } else {
                            if (isDark) Color(0xFF71717A) else Color(0xFFD4D4D8)
                        },
                        modifier = Modifier.size(width = 12.dp, height = 11.57.dp),
                    )
                }

                // 3-dots Menu Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onMenuClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.moreHorizontal,
                        contentDescription = "More options",
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(width = 12.dp, height = 1.8.dp),
                    )
                    menuContent?.invoke()
                }
            }
        }
    }
}
