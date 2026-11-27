package com.pnotes.ui.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Image
import com.pnotes.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * The studio's cards, measured against the final React components: the 208x260 duotone notebook
 * card with its embossed cover, foil label and emblem badge; the list row; the 376x74 folder card;
 * and the "new" tiles. All content is real — the live overlay is the note's own rendered page.
 */

// ── Cover art ────────────────────────────────────────────────────────────────────────────────

/** The eight duotone presets the React "Create Notebook" sheet offers. */
internal data class StudioCoverStyle(val top: Color, val bottom: Color, val emblem: String)

private val COVER_STYLES = listOf(
    StudioCoverStyle(Color(0xFF3B0764), Color(0xFF8B5CF6), "arrows"),
    StudioCoverStyle(Color(0xFF4338CA), Color(0xFF6366F1), "atom"),
    StudioCoverStyle(Color(0xFF047857), Color(0xFF10B981), "brackets"),
    StudioCoverStyle(Color(0xFFB45309), Color(0xFFD97706), "orbit"),
    StudioCoverStyle(Color(0xFF4A7C72), Color(0xFF82ADA4), "circuit"),
    StudioCoverStyle(Color(0xFF6C92AF), Color(0xFFA6C2D6), "pinwheel"),
    StudioCoverStyle(Color(0xFF991B1B), Color(0xFFDC2626), "mesh"),
    StudioCoverStyle(Color(0xFF0F172A), Color(0xFFF59E0B), "tree"),
)

/** Stable cover art per note: the same note always gets the same duotone + emblem. */
internal fun coverStyleFor(item: StudioItem): StudioCoverStyle {
    // Seeded by modification time, not the URI: SAF ids are paths, so renaming a folder would otherwise
    // repaint every note inside it.
    val seed = if (item.modified > 0) item.modified else (item.uri.hashCode().toLong() and Long.MAX_VALUE)
    val idx = (seed % COVER_STYLES.size).toInt()
    return COVER_STYLES[idx]
}

/** The small emblem stamped into the cover badge, transcribed from NotebookCard.tsx. */
@Composable
internal fun StudioEmblem(emblem: String, modifier: Modifier = Modifier) {
    when (emblem) {
        "pinwheel" -> Canvas(modifier.size(30.dp)) {
            val w = size.width
            val h = size.height
            fun tri(a: Offset, b: Offset, c: Offset, color: Color) {
                val p = Path().apply {
                    moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close()
                }
                drawPath(p, color)
            }
            tri(Offset(0f, h / 2), Offset(w / 2, 0f), Offset(w / 2, h / 2), Color(0xFFF1F5F9))
            tri(Offset(w, 0f), Offset(w / 2, h / 2), Offset(w, h / 2), Color(0xFFDCE5ED))
            tri(Offset(0f, h / 2), Offset(w / 2, h), Offset(0f, h), Color(0xFF577E9E))
            tri(Offset(w / 2, h / 2), Offset(w, h / 2), Offset(w / 2, h), Color.White)
        }
        "arrows" -> Icon(
            StudioIcons.emblemArrows,
            null,
            tint = Color.White,
            modifier = modifier.size(width = 19.dp, height = 14.dp),
        )
        else -> Canvas(modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val thin = Stroke(1.25.dp.toPx(), cap = StrokeCap.Round)
            when (emblem) {
                "atom" -> {
                    drawCircle(Color.White, radius = w * 0.125f)
                    listOf(30f, -30f, 90f).forEach { deg ->
                        withTransform({ rotate(deg) }) {
                            drawOval(
                                color = Color.White,
                                topLeft = Offset(w * 0.1f, h * 0.325f),
                                size = Size(w * 0.8f, h * 0.35f),
                                style = Stroke(1.dp.toPx()),
                            )
                        }
                    }
                }
                "circuit" -> {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.8f),
                        topLeft = Offset(w * 4f / 24f, h * 4f / 24f),
                        size = Size(w * 16f / 24f, h * 16f / 24f),
                        cornerRadius = CornerRadius(w * 2f / 24f),
                        style = thin,
                    )
                    drawRect(
                        color = Color.White.copy(alpha = 0.2f),
                        topLeft = Offset(w * 9f / 24f, h * 9f / 24f),
                        size = Size(w * 6f / 24f, h * 6f / 24f),
                    )
                    drawLine(Color.White, Offset(w * 4f / 24f, h * .5f), Offset(w * 9f / 24f, h * .5f), thin.width, StrokeCap.Round)
                    drawLine(Color.White, Offset(w * 15f / 24f, h * .5f), Offset(w * 20f / 24f, h * .5f), thin.width, StrokeCap.Round)
                    drawLine(Color.White, Offset(w * .5f, h * 4f / 24f), Offset(w * .5f, h * 9f / 24f), thin.width, StrokeCap.Round)
                    drawLine(Color.White, Offset(w * .5f, h * 15f / 24f), Offset(w * .5f, h * 20f / 24f), thin.width, StrokeCap.Round)
                }
                "orbit" -> {
                    drawCircle(Color.White, radius = w * 3f / 24f)
                    drawCircle(
                        Color.White,
                        radius = w * 8f / 24f,
                        style = Stroke(
                            1.25.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                        ),
                    )
                    drawCircle(Color(0xFFFEF08A), radius = w * 1.5f / 24f, center = Offset(w * 18f / 24f, h * 7f / 24f))
                }
                "tree" -> {
                    drawLine(Color.White, Offset(w * .5f, h * 7.5f / 24f), Offset(w * 7f / 24f, h * 9.8f / 24f), thin.width, StrokeCap.Round)
                    drawLine(Color.White, Offset(w * .5f, h * 7.5f / 24f), Offset(w * 17f / 24f, h * 9.8f / 24f), thin.width, StrokeCap.Round)
                    drawLine(Color.White, Offset(w * 7f / 24f, h * 14.2f / 24f), Offset(w * 4f / 24f, h * 17.2f / 24f), thin.width, StrokeCap.Round)
                    drawLine(Color.White, Offset(w * 7f / 24f, h * 14.2f / 24f), Offset(w * 10f / 24f, h * 17.2f / 24f), thin.width, StrokeCap.Round)
                    drawCircle(Color.White, w * 2.5f / 24f, Offset(w * .5f, h * 5f / 24f))
                    drawCircle(Color.White, w * 2.2f / 24f, Offset(w * 7f / 24f, h * .5f))
                    drawCircle(Color.White, w * 2.2f / 24f, Offset(w * 17f / 24f, h * .5f))
                    drawCircle(Color.White, w * 1.8f / 24f, Offset(w * 4f / 24f, h * 19f / 24f))
                    drawCircle(Color.White, w * 1.8f / 24f, Offset(w * 10f / 24f, h * 19f / 24f))
                }
                "mesh" -> {
                    val pts = listOf(
                        Offset(w * .5f, h * 3f / 24f), Offset(w * 20f / 24f, h * 7.5f / 24f),
                        Offset(w * 20f / 24f, h * 16.5f / 24f), Offset(w * .5f, h * 21f / 24f),
                        Offset(w * 4f / 24f, h * 16.5f / 24f), Offset(w * 4f / 24f, h * 7.5f / 24f),
                    )
                    val p = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                        close()
                    }
                    drawPath(p, Color.White.copy(alpha = 0.9f), style = thin)
                    drawLine(Color.White, pts[0], pts[3], thin.width, StrokeCap.Round)
                    drawLine(Color.White, pts[4], pts[1], thin.width, StrokeCap.Round)
                    drawLine(Color.White, pts[5], pts[2], thin.width, StrokeCap.Round)
                }
                else -> {
                    val p = Path().apply {
                        moveTo(w * .5f, h * 2f / 24f)
                        lineTo(w * 20f / 24f, h * .5f)
                        lineTo(w * .5f, h * 22f / 24f)
                        lineTo(w * 4f / 24f, h * .5f)
                        close()
                    }
                    drawPath(p, Color.White.copy(alpha = 0.2f))
                    drawPath(p, Color.White, style = thin)
                    drawCircle(Color.White, radius = w * 2.5f / 24f)
                }
            }
        }
    }
}

@Composable
private fun CoverFrame(
    item: StudioItem,
    thumbnail: ImageBitmap?,
    style: StudioCoverStyle,
    modifier: Modifier = Modifier,
) {
    val t = studioTokens()
    Box(
        modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(style.top),
    ) {
        // Dual-tone halves.
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(102f / 184f)
                .background(style.top),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(82f / 184f)
                .background(style.bottom),
        )

        // Spine ribbon + stitching.
        Box(
            Modifier
                .width(12.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.22f),
                        0.5f to Color.Black.copy(alpha = 0.08f),
                        1f to Color.Black.copy(alpha = 0.01f),
                    ),
                ),
        )
        Canvas(Modifier.offset(x = 8.dp).width(4.dp).fillMaxHeight()) {
            drawLine(
                color = Color.Black.copy(alpha = 0.25f),
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 0.84.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())),
            )
        }

        // Foil label.
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                item.title.take(15).uppercase(),
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 9.23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.68.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 160.dp),
            )
            Box(Modifier.width(80.dp).height(1.dp).background(Color.White.copy(alpha = 0.30f)))
        }

        // Emblem badge.
        Box(
            Modifier
                .offset(x = 77.dp, y = 90.dp)
                .size(36.9.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .border(0.63.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            StudioEmblem(style.emblem)
        }

        // Live preview of the note's real first page.
        if (thumbnail != null) {
            Box(
                Modifier
                    .padding(start = 14.dp, top = 40.dp, end = 10.dp, bottom = 12.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (t.isDark) Color(0xFF121214) else Color.White)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
            ) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 4.dp, bottom = 3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                ) {
                    Text(
                        "LIVE",
                        color = t.accentAmber,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}

// ── Notebook card ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StudioNotebookCard(
    item: StudioItem,
    thumbnail: ImageBitmap?,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelect: () -> Unit,
    onToggleStar: () -> Unit,
    modifier: Modifier = Modifier,
    onTogglePin: (() -> Unit)? = null,
    onMove: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onExportPdf: (() -> Unit)? = null,
    onShowPath: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val t = studioTokens()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "cardPressScale",
    )
    val hoverY by animateDpAsState(
        targetValue = if (hovered) (-3).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "cardHoverY",
    )
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier
            .size(width = 163.dp, height = 227.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = hoverY.toPx()
            }
            .then(
                if (t.isDark) {
                    Modifier.figcraftDarkShadow(shapeRadius = 16.dp, offsetY = 5.dp, spread = 2.dp)
                } else {
                    Modifier.shadow(
                        elevation = 11.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.20f),
                        spotColor = if (selected) t.primary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.45f),
                    )
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (t.isDark) t.card.copy(alpha = 0.85f) else t.card)
            .border(1.dp, if (selected) t.primary else t.cardBorder, RoundedCornerShape(16.dp))
            .hoverable(interaction)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        // Thumbnail box: 141x135dp at offset(11dp, 11dp)
        Box(
            Modifier
                .offset(x = 11.dp, y = 11.dp)
                .size(width = 141.dp, height = 135.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (t.isDark) t.cardSoft.copy(alpha = 0.90f) else t.cardSoft),
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(t.cardSoft, t.searchField),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fig_notes),
                        contentDescription = null,
                        tint = t.textTertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(29.dp),
                    )
                }
            }

            // Top-end badges: Pin (green) and Favorite (gold star) matching Figcraft placement
            if (item.isPinned && item.isFavorite) {
                Icon(
                    painter = painterResource(R.drawable.fig_pin),
                    contentDescription = "Pinned",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 7.dp, end = 7.dp)
                        .size(width = 14.dp, height = 18.dp),
                )
                Icon(
                    painter = painterResource(R.drawable.fig_favorite_gold),
                    contentDescription = "Favorite",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 7.dp, end = 25.dp)
                        .size(16.dp),
                )
            } else if (item.isPinned) {
                Icon(
                    painter = painterResource(R.drawable.fig_pin),
                    contentDescription = "Pinned",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 7.dp, end = 7.dp)
                        .size(width = 14.dp, height = 18.dp),
                )
            } else if (item.isFavorite) {
                Icon(
                    painter = painterResource(R.drawable.fig_favorite_gold),
                    contentDescription = "Favorite",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 7.dp, end = 7.dp)
                        .size(16.dp),
                )
            }

            // Selection checkbox
            if (selectionMode || selected) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 7.dp, top = 7.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (selected) t.primary else Color.Black.copy(alpha = 0.5f))
                        .clickable(onClick = onSelect),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(StudioIcons.checkSmall, "Selected", tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                }
            }
        }

        // Title at offset(11dp, 153dp), 2 lines permitted to wrap like Figcraft
        Text(
            text = item.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.W600,
            color = t.textPrimary,
            maxLines = 2,
            lineHeight = 17.sp,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .offset(x = 11.dp, y = 153.dp)
                .width(141.dp)
                .heightIn(max = 38.dp),
        )

        // Meta at offset(11dp, 201dp)
        val dateStr = cardDate(item.modified)
        val pagesStr = if (item.pages != null && item.pages > 0) "${item.pages} pages" else "Handwritten"
        val metaText = if (dateStr.isNotBlank() && dateStr != "—") "$dateStr • $pagesStr" else pagesStr
        Text(
            text = metaText,
            fontSize = 11.sp,
            fontWeight = FontWeight.W400,
            color = t.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .offset(x = 11.dp, y = 201.dp)
                .width(124.dp),
        )

        // 3-dots Menu icon at offset(137dp, 195dp)
        Box(
            Modifier
                .offset(x = 137.dp, y = 195.dp)
                .size(22.dp)
                .clickable { showMenu = !showMenu },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.fig_dots_vertical),
                contentDescription = "Notebook options",
                tint = t.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }

        // Context menu popup using floating window Popup to avoid clipping and event stealing
        if (showMenu) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(x = with(density) { (-10).dp.roundToPx() }, y = with(density) { 190.dp.roundToPx() }),
                onDismissRequest = { showMenu = false },
                properties = PopupProperties(focusable = true),
            ) {
                StudioMenuSurface(minWidth = 196.dp) {
                    val dismiss = { showMenu = false }
                    if (onTogglePin != null) {
                        StudioMenuItem(
                            label = if (item.isPinned) "Unpin Note" else "Pin to Top",
                            icon = StudioIcons.pin,
                            onClick = { dismiss(); onTogglePin() },
                        )
                    }
                    StudioMenuItem(
                        label = if (item.isFavorite) "Remove Favorite" else "Add to Favorites",
                        icon = if (item.isFavorite) StudioIcons.star else StudioIcons.starOutline,
                        onClick = { dismiss(); onToggleStar() },
                    )
                    StudioMenuDivider()
                    if (onRename != null) {
                        StudioMenuItem(
                            label = "Rename Note",
                            icon = StudioIcons.pencilWrite,
                            onClick = { dismiss(); onRename() },
                        )
                    }
                    if (onMove != null) {
                        StudioMenuItem(
                            label = "Move to Folder...",
                            icon = StudioIcons.moveToFolder,
                            onClick = { dismiss(); onMove() },
                        )
                    }
                    if (onExportPdf != null) {
                        StudioMenuItem(
                            label = "Export as PDF",
                            icon = StudioIcons.download,
                            onClick = { dismiss(); onExportPdf() },
                        )
                    }
                    if (onShowPath != null) {
                        StudioMenuItem(
                            label = "Storage Location",
                            icon = StudioIcons.cloudCheck,
                            onClick = { dismiss(); onShowPath() },
                        )
                    }
                    if (onDelete != null) {
                        StudioMenuDivider()
                        StudioMenuItem(
                            label = "Delete Note",
                            icon = StudioIcons.trash,
                            danger = true,
                            onClick = { dismiss(); onDelete() },
                        )
                    }
                }
            }
        }
    }
}

private fun cardDate(millis: Long): String {
    if (millis <= 0) return "—"
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
}

// ── List row ─────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StudioNoteRow(
    item: StudioItem,
    thumbnail: ImageBitmap?,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelect: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onMove: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onTogglePin: (() -> Unit)? = null,
    onExportPdf: (() -> Unit)? = null,
    onShowPath: (() -> Unit)? = null,
) {
    val t = studioTokens()
    val style = remember(item.uri) { coverStyleFor(item) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var showMenu by remember { mutableStateOf(false) }
    val stamp = remember(item.modified) {
        if (item.modified <= 0) "—"
        else SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.modified))
    }

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) t.selectedRow else t.card)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) t.primary else if (hovered) t.cardHoverBorder else t.cardBorder,
                RoundedCornerShape(12.dp),
            )
            .then(if (hovered && !selected) Modifier.graphicsLayer { translationY = -1.dp.toPx() } else Modifier)
            .hoverable(interaction)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (selectionMode || selected || hovered) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (selected) t.primary else Color.Transparent)
                        .border(
                            if (selected) 2.dp else 1.5.dp,
                            if (selected) t.primary else t.textMuted,
                            RoundedCornerShape(5.dp),
                        )
                        .clickable(onClick = onSelect),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            StudioIcons.checkSmall,
                            "Deselect note",
                            tint = if (t.isDark) Color(0xFF04131F) else Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }

            // Mini cover.
            Box(
                Modifier
                    .size(width = 42.dp, height = 54.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(style.top)
                    .shadow(2.dp, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .background(style.bottom),
                )
                Icon(
                    StudioIcons.pnotesLogo,
                    null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp),
                )
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        item.title,
                        color = t.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.folderName != null) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (t.isDark) t.primary.copy(alpha = 0.12f) else Color(0xFFE0F2FE))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                item.folderName,
                                color = t.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Text(
                    metaLine(
                        item.pages?.let { "$it pages" } ?: item.paperTag,
                        stamp,
                        item.isPinned.takeIf { it }?.let { "Pinned" },
                    ),
                    color = t.textTertiary,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (onMove != null && hovered) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(t.chip)
                        .border(1.dp, t.chipBorder, RoundedCornerShape(6.dp))
                        .clickable(onClick = onMove)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("Move...", color = t.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleStar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (item.isFavorite) StudioIcons.star else StudioIcons.starOutline,
                    "Star note",
                    tint = if (item.isFavorite) t.star else t.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showMenu = !showMenu },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.fig_dots_vertical),
                    contentDescription = "Note options",
                    tint = t.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (showMenu) {
                val density = LocalDensity.current
                Popup(
                    alignment = Alignment.TopEnd,
                    offset = IntOffset(x = with(density) { (-10).dp.roundToPx() }, y = with(density) { 36.dp.roundToPx() }),
                    onDismissRequest = { showMenu = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    StudioMenuSurface(minWidth = 196.dp) {
                        val dismiss = { showMenu = false }
                        if (onTogglePin != null) {
                            StudioMenuItem(
                                label = if (item.isPinned) "Unpin Note" else "Pin to Top",
                                icon = StudioIcons.pin,
                                onClick = { dismiss(); onTogglePin() },
                            )
                        }
                        StudioMenuItem(
                            label = if (item.isFavorite) "Remove Favorite" else "Add to Favorites",
                            icon = if (item.isFavorite) StudioIcons.star else StudioIcons.starOutline,
                            onClick = { dismiss(); onToggleStar() },
                        )
                        StudioMenuDivider()
                        if (onRename != null) {
                            StudioMenuItem(
                                label = "Rename Note",
                                icon = StudioIcons.pencilWrite,
                                onClick = { dismiss(); onRename() },
                            )
                        }
                        if (onMove != null) {
                            StudioMenuItem(
                                label = "Move to Folder...",
                                icon = StudioIcons.moveToFolder,
                                onClick = { dismiss(); onMove() },
                            )
                        }
                        if (onExportPdf != null) {
                            StudioMenuItem(
                                label = "Export as PDF",
                                icon = StudioIcons.download,
                                onClick = { dismiss(); onExportPdf() },
                            )
                        }
                        if (onShowPath != null) {
                            StudioMenuItem(
                                label = "Storage Location",
                                icon = StudioIcons.cloudCheck,
                                onClick = { dismiss(); onShowPath() },
                            )
                        }
                        StudioMenuDivider()
                        StudioMenuItem(
                            label = "Delete Note",
                            icon = StudioIcons.trash,
                            danger = true,
                            onClick = { dismiss(); onDelete() },
                        )
                    }
                }
            }
        }
    }
}

// ── Folder cards / tiles ────────────────────────────────────────────────────────────────────

@Composable
internal fun StudioFolderCard(
    folder: StudioFolder,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onAddSubfolder: (() -> Unit)? = null,
) {
    val t = studioTokens()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val isPressed by interaction.collectIsPressedAsState()
    val folderScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "folderScale",
    )
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier
            .height(55.dp)
            .wrapContentWidth()
            .graphicsLayer {
                scaleX = folderScale
                scaleY = folderScale
            }
            .then(
                if (t.isDark) {
                    Modifier.figcraftDarkShadow(shapeRadius = 17.dp, offsetY = 6.dp, spread = 2.dp)
                } else {
                    Modifier.shadow(
                        elevation = 5.dp,
                        shape = RoundedCornerShape(17.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.20f),
                        spotColor = Color.Black.copy(alpha = 0.35f),
                    )
                }
            )
            .clip(RoundedCornerShape(17.dp))
            .background(if (t.isDark) t.card.copy(alpha = 0.85f) else t.card)
            .border(1.dp, if (hovered) t.primary else t.cardBorder, RoundedCornerShape(17.dp))
            .hoverable(interaction)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .fillMaxHeight()
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                        onLongClick = { if (onAddSubfolder != null || onRename != null || onDelete != null) showMenu = true },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.fig_folder_card),
                    contentDescription = "Folder",
                    tint = t.primary,
                    modifier = Modifier.size(width = 25.dp, height = 21.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        folder.name,
                        color = t.textPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.W600,
                        maxLines = 1,
                    )
                    Text(
                        buildString {
                            append(if (folder.notes == 1) "1 notebook" else "${folder.notes} notebooks")
                            append(" • ")
                            append(if (folder.pages != null && folder.pages > 0) "${folder.pages} pages" else "${folder.folders} folders")
                        },
                        color = t.textSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.W400,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                painter = painterResource(R.drawable.fig_chevron_right),
                contentDescription = null,
                tint = t.textSecondary,
                modifier = Modifier.size(12.dp),
            )
            if (onAddSubfolder != null || onRename != null || onDelete != null) {
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (hovered || showMenu) t.chip else Color.Transparent)
                        .clickable { showMenu = !showMenu },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fig_dots_vertical),
                        contentDescription = "Folder options",
                        tint = if (hovered || showMenu) t.textPrimary else t.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        if (showMenu) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(x = with(density) { (-10).dp.roundToPx() }, y = with(density) { 50.dp.roundToPx() }),
                onDismissRequest = { showMenu = false },
                properties = PopupProperties(focusable = true),
            ) {
                StudioMenuSurface(minWidth = 180.dp) {
                    val dismiss = { showMenu = false }
                    if (onAddSubfolder != null) {
                        StudioMenuItem(
                            label = "Add Subfolder",
                            icon = StudioIcons.folderPlus,
                            onClick = { dismiss(); onAddSubfolder() },
                        )
                    }
                    if (onRename != null) {
                        StudioMenuItem(
                            label = "Rename Folder",
                            icon = StudioIcons.pencilWrite,
                            onClick = { dismiss(); onRename() },
                        )
                    }
                    if (onDelete != null) {
                        StudioMenuDivider()
                        StudioMenuItem(
                            label = "Delete Folder",
                            icon = StudioIcons.trash,
                            danger = true,
                            onClick = { dismiss(); onDelete() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun StudioNewFolderCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "New Folder",
    subtitle: String = "Group notebooks by subject",
) {
    val t = studioTokens()
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val newFolderScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "newFolderScale",
    )
    Box(
        modifier
            .height(55.dp)
            .wrapContentWidth()
            .graphicsLayer {
                scaleX = newFolderScale
                scaleY = newFolderScale
            }
            .then(
                if (t.isDark) {
                    Modifier.figcraftDarkShadow(shapeRadius = 17.dp, offsetY = 6.dp, spread = 2.dp)
                } else {
                    Modifier.shadow(
                        elevation = 5.dp,
                        shape = RoundedCornerShape(17.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.15f),
                        spotColor = Color.Black.copy(alpha = 0.25f),
                    )
                }
            )
            .clip(RoundedCornerShape(17.dp))
            .background(if (t.isDark) t.card.copy(alpha = 0.85f) else t.card)
            .border(1.dp, t.cardBorder, RoundedCornerShape(17.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
    ) {
        Row(
            Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (t.isDark) Color(0xFF222A37) else t.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.fig_plus),
                    contentDescription = null,
                    tint = if (t.isDark) Color(0xFFFFB200) else t.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    color = t.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    maxLines = 1,
                )
                Text(
                    subtitle,
                    color = t.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.W400,
                    maxLines = 1,
                )
            }
        }
    }
}

// ── New notebook tile ───────────────────────────────────────────────────────────────────────

@Composable
internal fun StudioNewNotebookCard(
    onClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = studioTokens()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "newCardPressScale",
    )
    val hoverY by animateDpAsState(
        targetValue = if (hovered) (-3).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "newCardHoverY",
    )

    Column(
        modifier
            .size(width = 163.dp, height = 227.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = hoverY.toPx()
            }
            .then(
                if (t.isDark) {
                    Modifier.figcraftDarkShadow(shapeRadius = 16.dp, offsetY = 5.dp, spread = 2.dp)
                } else {
                    Modifier.shadow(
                        elevation = 11.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.20f),
                        spotColor = Color.Black.copy(alpha = 0.45f),
                    )
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .background(t.card)
            .border(1.dp, t.cardBorder, RoundedCornerShape(16.dp))
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick(null) }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(t.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(StudioIcons.plus, null, tint = t.primary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("New Notebook", color = t.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Start writing or sketching a fresh page",
            color = t.textTertiary,
            fontSize = 9.5.sp,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Ruled" to "ruled", "Grid" to "grid", "Dots" to "dotted").forEach { (label, id) ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(t.chip)
                        .border(1.dp, t.chipBorder, RoundedCornerShape(6.dp))
                        .clickable { onClick(id) }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(label, color = t.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** Segmented grid/list switcher matching Figcraft: 104x44dp pill, 52x44dp sliding thumb. */
@Composable
internal fun StudioViewSwitch(
    isGrid: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = studioTokens()

    // Apple fluid spring motion: snappy response, physical momentum settle
    val thumbOffset by animateDpAsState(
        targetValue = if (isGrid) 0.dp else 52.dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 380f,
        ),
        label = "viewThumbOffset",
    )

    // Fluid morphing corner radii from left half-pill to right half-pill
    // DampingRatioNoBouncy + coerceAtLeast(0.dp) prevents Compose IllegalArgumentException on corner size
    val startRadius by animateDpAsState(
        targetValue = if (isGrid) 22.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f),
        label = "viewThumbStartRadius",
    )
    val endRadius by animateDpAsState(
        targetValue = if (isGrid) 0.dp else 22.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f),
        label = "viewThumbEndRadius",
    )

    // Apple tactile press interaction sources
    val gridInteractionSource = remember { MutableInteractionSource() }
    val isGridPressed by gridInteractionSource.collectIsPressedAsState()
    val gridScale by animateFloatAsState(
        targetValue = if (isGridPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "gridPressScale",
    )

    val listInteractionSource = remember { MutableInteractionSource() }
    val isListPressed by listInteractionSource.collectIsPressedAsState()
    val listScale by animateFloatAsState(
        targetValue = if (isListPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "listPressScale",
    )

    // Active & inactive icon tints: in Dark mode active is White on dark navy; in Light mode active is t.primary (#007AFF) on light thumb
    val activeTint = if (t.isDark) Color.White else t.primary
    val inactiveTint = if (t.isDark) Color(0xFF7E8D9E) else Color(0xFF64748B)

    val gridIconTint by animateColorAsState(
        targetValue = if (isGrid) activeTint else inactiveTint,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "gridIconTint",
    )
    val listIconTint by animateColorAsState(
        targetValue = if (!isGrid) activeTint else inactiveTint,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "listIconTint",
    )

    Box(
        modifier
            .size(width = 104.dp, height = 44.dp)
            .then(
                if (t.isDark) {
                    Modifier.figcraftDarkShadow(shapeRadius = 22.dp, offsetY = 6.dp, spread = 2.dp)
                } else Modifier
            )
            .clip(RoundedCornerShape(22.dp))
            .background(if (t.isDark) Color(0xFF15151A) else t.searchField)
            .border(1.dp, t.searchBorder, RoundedCornerShape(22.dp)),
    ) {
        // Sliding active thumb with fluid morphing corners
        Box(
            Modifier
                .offset(x = thumbOffset, y = 0.dp)
                .size(width = 52.dp, height = 44.dp)
                .then(if (!t.isDark) Modifier.shadow(2.dp, RoundedCornerShape(22.dp)) else Modifier)
                .clip(
                    RoundedCornerShape(
                        topStart = startRadius.coerceAtLeast(0.dp),
                        bottomStart = startRadius.coerceAtLeast(0.dp),
                        topEnd = endRadius.coerceAtLeast(0.dp),
                        bottomEnd = endRadius.coerceAtLeast(0.dp),
                    )
                )
                .background(if (t.isDark) Color(0xFF1D3054) else t.segmentThumb),
        )
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .size(width = 52.dp, height = 44.dp)
                    .graphicsLayer {
                        scaleX = gridScale
                        scaleY = gridScale
                    }
                    .clickable(
                        interactionSource = gridInteractionSource,
                        indication = null,
                    ) { if (!isGrid) onChange(true) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.fig_grid),
                    contentDescription = "Grid view",
                    tint = gridIconTint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(
                Modifier
                    .size(width = 52.dp, height = 44.dp)
                    .graphicsLayer {
                        scaleX = listScale
                        scaleY = listScale
                    }
                    .clickable(
                        interactionSource = listInteractionSource,
                        indication = null,
                    ) { if (isGrid) onChange(false) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.fig_list),
                    contentDescription = "List view",
                    tint = listIconTint,
                    modifier = Modifier.size(width = 16.dp, height = 11.dp),
                )
            }
        }
    }
}
