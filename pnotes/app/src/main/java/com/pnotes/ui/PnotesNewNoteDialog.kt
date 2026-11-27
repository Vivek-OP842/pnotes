package com.pnotes.ui

import com.pnotes.ui.theme.PurpleTheme
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.pnotes.ui.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pnotes.core.model.Orientation
import com.pnotes.core.model.PageSize
import com.pnotes.ui.covers.CoverRepository
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.model.CoverItem
import com.pnotes.ui.model.MarginOption
import com.pnotes.ui.model.NewNoteConfiguration
import com.pnotes.ui.model.NoteCreationType
import com.pnotes.ui.model.PaperColorItem
import com.pnotes.ui.model.PaperColorPalette
import com.pnotes.ui.model.PaperStyleOption
import com.pnotes.ui.model.ProceduralCoverType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PrimaryMonochrome: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
private val PrimaryYellow: Color @Composable get() = PrimaryMonochrome
private val PrimaryOrange: Color @Composable get() = PrimaryMonochrome
private val DarkDialogBgStart = Color(0xFF151A22)
private val DarkDialogBgEnd = Color(0xFF10141A)
private val LightDialogBgStart = Color(0xFFFFFFFF)
private val LightDialogBgEnd = Color(0xFFFAFAFA)
private val SelectedBorderColor: Color @Composable get() = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)

@Composable
fun NewNoteDialog(
    visible: Boolean,
    currentFolderDocId: String,
    currentFolderName: String,
    availableFolders: List<Pair<String, String>>,
    initialStyle: PaperStyleOption = PaperStyleOption.PLAIN,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        type: NoteCreationType,
        style: PaperStyleOption,
        color: PaperColorItem,
        targetFolderDocId: String,
        cover: CoverItem,
        orientation: Orientation,
        margins: MarginOption,
        pageSize: PageSize,
        showPageNumber: Boolean,
        showDate: Boolean,
    ) -> Unit,
) {
    if (!visible) return

    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val context = LocalContext.current
    var config by remember(visible) {
        mutableStateOf(
            NewNoteConfiguration(
                title = "",
                noteType = NoteCreationType.HANDWRITTEN,
                cover = CoverRepository.defaultFeaturedCover,
                paperStyle = initialStyle,
                paperColor = PaperColorPalette.items[0],
                pageSize = PageSize.A4,
                orientation = Orientation.PORTRAIT,
                margins = MarginOption.NORMAL,
                showPageNumber = false,
                showDate = true,
                targetFolderDocId = currentFolderDocId,
                targetFolderName = if (currentFolderName.isNotEmpty()) currentFolderName else "Pari Notes",
            )
        )
    }

    var availableCovers by remember { mutableStateOf(CoverRepository.loadAllCovers(context)) }
    LaunchedEffect(Unit) {
        availableCovers = CoverRepository.loadAllCovers(context)
    }

    var showCustomColorPicker by remember { mutableStateOf(false) }
    var showAllCoversModal by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            if (showAllCoversModal) showAllCoversModal = false else onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
    ) {
        BackHandler {
            if (showAllCoversModal) showAllCoversModal = false else onDismiss()
        }

        var dialogCardVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            dialogCardVisible = true
        }

        val scrimAlpha by animateFloatAsState(
            targetValue = if (dialogCardVisible) 0.45f else 0f,
            animationSpec = tween(180),
            label = "newNoteScrimAlpha",
        )
        val cardScale by animateFloatAsState(
            targetValue = if (dialogCardVisible) 1.0f else 0.92f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "newNoteCardScale",
        )
        val cardAlpha by animateFloatAsState(
            targetValue = if (dialogCardVisible) 1.0f else 0f,
            animationSpec = tween(180),
            label = "newNoteCardAlpha",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 940.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .shadow(elevation = 28.dp, shape = RoundedCornerShape(28.dp), spotColor = Color(0x66000000))
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            if (isDark) listOf(DarkDialogBgStart, DarkDialogBgEnd) else listOf(LightDialogBgStart, LightDialogBgEnd)
                        )
                    )
                    .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // === FIXED HEADER ===
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 26.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = "New note",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFF5F7FA) else Color(0xFF151922),
                            modifier = Modifier.align(Alignment.Center),
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFF4F4F5))
                                .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), CircleShape)
                                .clickable(onClick = onDismiss)
                                .align(Alignment.CenterEnd),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.close,
                                contentDescription = "Close",
                                tint = if (isDark) Color(0xFFE8ECF2) else Color(0xFF151922),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    // === SCROLLABLE BODY ===
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 26.dp, vertical = 6.dp),
                    ) {
                        // === SECTION 1: NOTE TITLE ===
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Note title",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFE9EDF2) else Color(0xFF151922),
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
                            .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        BasicTextField(
                            value = config.title,
                            onValueChange = { if (it.length <= 80) config = config.copy(title = it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = if (isDark) Color(0xFFE8EDF5) else Color(0xFF151922),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            cursorBrush = SolidColor(PrimaryYellow),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (config.title.isEmpty()) {
                                    Text(
                                        text = "Untitled note",
                                        color = if (isDark) Color(0xFF5F6E82) else Color(0xFFA1A1AA),
                                        fontSize = 15.sp,
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Text(
                            text = "${config.title.length}/80",
                            color = if (isDark) Color(0xFF7A889B) else Color(0xFF71717A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }

                    // === SECTION 2: TOP LIVE PREVIEW (Cover + Page + Note Type) ===
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // 1. Cover Preview
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(235.dp),
                        ) {
                            CoverPreviewCard(
                                cover = config.cover,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(310.dp)
                                    .clickable { showAllCoversModal = true },
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAllCoversModal = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "Cover",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFE8ECF2) else Color(0xFF151922),
                                )
                                Text(
                                    text = "(Tap to view all)",
                                    fontSize = 12.sp,
                                    color = PrimaryYellow,
                                )
                            }
                        }

                        // 2. Page Preview
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(220.dp),
                        ) {
                            PagePreviewCard(
                                config = config,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(310.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Page",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFE8ECF2) else Color(0xFF151922),
                            )
                        }

                        // 3. Note Type Selection Cards
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            // Handwritten note card
                            NoteTypeCard(
                                title = "Handwritten note",
                                description = "Write and draw freely\non a blank page",
                                isSelected = config.noteType == NoteCreationType.HANDWRITTEN,
                                accentColor = PrimaryMonochrome,
                                iconBadge = {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    if (isDark) listOf(Color(0xFF27272A), Color(0xFF18181B))
                                                    else listOf(Color(0xFFF4F4F5), Color(0xFFE4E4E7))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = PnotesIcons.pen,
                                            contentDescription = "Handwritten",
                                            tint = if (isDark) Color.White else Color(0xFF09090B),
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                },
                                onClick = { config = config.copy(noteType = NoteCreationType.HANDWRITTEN) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(148.dp),
                            )

                            // New canvas card
                            NoteTypeCard(
                                title = "New canvas",
                                description = "Create a blank canvas for\nsketching and drawing",
                                isSelected = config.noteType == NoteCreationType.CANVAS,
                                accentColor = PrimaryMonochrome,
                                iconBadge = {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    if (isDark) listOf(Color(0xFF27272A), Color(0xFF18181B))
                                                    else listOf(Color(0xFFF4F4F5), Color(0xFFE4E4E7))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = PnotesIcons.grid,
                                            contentDescription = "Canvas",
                                            tint = if (isDark) Color.White else Color(0xFF09090B),
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                },
                                onClick = { config = config.copy(noteType = NoteCreationType.CANVAS) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(148.dp),
                            )
                        }
                    }

                    // === SECTION 4: CHOOSE PAPER STYLE ===
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Choose paper style",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFF3F5F8) else Color(0xFF151922),
                    )
                    Spacer(Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(PaperStyleOption.allOptions) { style ->
                            val isSelected = config.paperStyle == style
                            PaperStyleCard(
                                style = style,
                                isSelected = isSelected,
                                onClick = { config = config.copy(paperStyle = style) },
                            )
                        }
                    }

                    // === SECTION 5: CHOOSE PAPER COLOUR ===
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Choose paper colour",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFF3F5F8) else Color(0xFF151922),
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PaperColorPalette.items.forEach { colorItem ->
                            val isSelected = config.paperColor.id == colorItem.id
                            PaperColorSwatch(
                                item = colorItem,
                                isSelected = isSelected,
                                onClick = { config = config.copy(paperColor = colorItem) },
                            )
                        }

                        // Custom "+" Color Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), CircleShape)
                                .clickable { showCustomColorPicker = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+",
                                color = if (isDark) Color(0xFFBAC7D5) else Color(0xFF71717A),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Light,
                            )
                        }
                    }

                    if (showCustomColorPicker) {
                        ColorPickerPopup(
                            initial = config.paperColor.rgba,
                            recents = emptyList(),
                            onDismiss = { showCustomColorPicker = false },
                            onPick = { picked ->
                                val customItem = PaperColorItem(
                                    id = "custom_${picked.r}_${picked.g}_${picked.b}",
                                    name = "Custom",
                                    color = Color(picked.r, picked.g, picked.b, picked.a),
                                    rgba = picked,
                                    isDark = (picked.r * 0.299 + picked.g * 0.587 + picked.b * 0.114) < 128
                                )
                                config = config.copy(paperColor = customItem)
                                showCustomColorPicker = false
                            }
                        )
                    }

                    // === SECTION 6: CHOOSE COVER ===
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Choose cover",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFF3F5F8) else Color(0xFF151922),
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${availableCovers.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
                                )
                            }
                        }

                        // "View all covers" button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5))
                                .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(20.dp))
                                .clickable { showAllCoversModal = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = PnotesIcons.layoutGrid,
                                contentDescription = "View all",
                                tint = PrimaryYellow,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "View all covers",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryYellow
                            )
                            Icon(
                                imageVector = PnotesIcons.chevronRight,
                                contentDescription = null,
                                tint = PrimaryYellow,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Quick "View all covers" tile as first card
                        item {
                            Box(
                                modifier = Modifier
                                    .size(width = 92.dp, height = 125.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                    .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(8.dp))
                                    .clickable { showAllCoversModal = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryYellow.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = PnotesIcons.layoutGrid,
                                            contentDescription = "View all",
                                            tint = PrimaryYellow,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "View all",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryYellow
                                    )
                                    Text(
                                        text = "${availableCovers.size} covers",
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A)
                                    )
                                }
                            }
                        }

                        items(availableCovers) { coverItem ->
                            val isSelected = config.cover.id == coverItem.id
                            CoverThumbnailCard(
                                cover = coverItem,
                                isSelected = isSelected,
                                onClick = { config = config.copy(cover = coverItem) },
                            )
                        }
                    }

                    // === SECTION 7: PAGE SETTINGS ===
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Page settings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFF3F5F8) else Color(0xFF151922),
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 1. Page Size
                        var sizeDropdownOpen by remember { mutableStateOf(false) }
                        Column {
                            Text("Page size", fontSize = 12.sp, color = if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A))
                            Spacer(Modifier.height(4.dp))
                            Box {
                                Row(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                        .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                                        .clickable { sizeDropdownOpen = true }
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = "${config.pageSize.displayName} (210 × 297 mm)",
                                        fontSize = 13.sp,
                                        color = if (isDark) Color(0xFFE8EDF5) else Color(0xFF151922)
                                    )
                                    Icon(
                                        imageVector = PnotesIcons.chevronDown,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = sizeDropdownOpen,
                                    onDismissRequest = { sizeDropdownOpen = false }
                                ) {
                                    listOf(PageSize.A4, PageSize.A5, PageSize.LETTER, PageSize.LEGAL).forEach { size ->
                                        DropdownMenuItem(
                                            text = { Text(size.displayName) },
                                            onClick = {
                                                config = config.copy(pageSize = size)
                                                sizeDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Orientation
                        Column {
                            Text("Orientation", fontSize = 12.sp, color = if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A))
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OrientationButton(
                                    label = "Portrait",
                                    icon = PnotesIcons.portrait,
                                    isSelected = config.orientation == Orientation.PORTRAIT,
                                    onClick = { config = config.copy(orientation = Orientation.PORTRAIT) }
                                )
                                OrientationButton(
                                    label = "Landscape",
                                    icon = PnotesIcons.landscape,
                                    isSelected = config.orientation == Orientation.LANDSCAPE,
                                    onClick = { config = config.copy(orientation = Orientation.LANDSCAPE) }
                                )
                            }
                        }

                        // 3. Margins
                        var marginDropdownOpen by remember { mutableStateOf(false) }
                        Column {
                            Text("Margins", fontSize = 12.sp, color = if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A))
                            Spacer(Modifier.height(4.dp))
                            Box {
                                Row(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                        .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                                        .clickable { marginDropdownOpen = true }
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = config.margins.title,
                                        fontSize = 13.sp,
                                        color = if (isDark) Color(0xFFE8EDF5) else Color(0xFF151922)
                                    )
                                    Icon(
                                        imageVector = PnotesIcons.chevronDown,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = marginDropdownOpen,
                                    onDismissRequest = { marginDropdownOpen = false }
                                ) {
                                    MarginOption.allOptions.forEach { margin ->
                                        DropdownMenuItem(
                                            text = { Text(margin.title) },
                                            onClick = {
                                                config = config.copy(margins = margin)
                                                marginDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Page number switch
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Page number", fontSize = 12.sp, color = if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A))
                            Spacer(Modifier.height(2.dp))
                            Switch(
                                checked = config.showPageNumber,
                                onCheckedChange = { config = config.copy(showPageNumber = it) },
                                colors = PurpleTheme.switchColors(isDark)
                            )
                        }

                        // 5. Date switch
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Date", fontSize = 12.sp, color = if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A))
                            Spacer(Modifier.height(2.dp))
                            Switch(
                                checked = config.showDate,
                                onCheckedChange = { config = config.copy(showDate = it) },
                                colors = PurpleTheme.switchColors(isDark)
                            )
                        }
                    }

                    // === SECTION 9: CHOOSE NOTEBOOK ===
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Choose notebook",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFF3F5F8) else Color(0xFF151922),
                    )
                    Spacer(Modifier.height(10.dp))

                    var notebookDropdownOpen by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
                                .clickable { notebookDropdownOpen = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = PnotesIcons.folder,
                                    contentDescription = "Folder",
                                    tint = PrimaryYellow,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = config.targetFolderName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFF3F5F8) else Color(0xFF151922),
                                )
                            }

                            Icon(
                                imageVector = PnotesIcons.chevronDown,
                                contentDescription = "Dropdown",
                                tint = if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = notebookDropdownOpen,
                            onDismissRequest = { notebookDropdownOpen = false },
                            modifier = Modifier.widthIn(min = 280.dp)
                        ) {
                            availableFolders.forEach { (docId, folderName) ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = PnotesIcons.folder,
                                            contentDescription = null,
                                            tint = PrimaryYellow,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = { Text(folderName) },
                                    onClick = {
                                        config = config.copy(
                                            targetFolderDocId = docId,
                                            targetFolderName = folderName,
                                        )
                                        notebookDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    }

                    // === FIXED FOOTER (Cancel + Create) ===
                    Surface(
                        color = if (isDark) Color(0xFF09090B) else Color(0xFFFAFAFA),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, if (isDark) Color(0x18FFFFFF) else Color(0xFFE4E4E7)),
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 26.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Cancel Button
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF1A222E) else Color.White,
                                    contentColor = if (isDark) Color(0xFFE9EDF2) else Color(0xFF151922)
                                ),
                                border = BorderStroke(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7)),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(0.38f)
                                    .height(54.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            // Create Button
                            Button(
                                onClick = {
                                    val finalTitle = config.title.ifBlank { "Untitled note" }
                                    onCreate(
                                        finalTitle,
                                        config.noteType,
                                        config.paperStyle,
                                        config.paperColor,
                                        config.targetFolderDocId,
                                        config.cover,
                                        config.orientation,
                                        config.margins,
                                        config.pageSize,
                                        config.showPageNumber,
                                        config.showDate,
                                    )
                                },
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF0B0D10)
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(0.62f)
                                    .height(54.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            if (isDark) listOf(Color(0xFFFFFFFF), Color(0xFFF4F4F5))
                                            else listOf(Color(0xFF09090B), Color(0xFF18181B))
                                        ),
                                        RoundedCornerShape(27.dp)
                                    )
                            ) {
                                Text(
                                    text = "Create",
                                    color = if (isDark) Color(0xFF09090B) else Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // === ALL COVERS GALLERY OVERLAY ===
                AnimatedVisibility(
                    visible = showAllCoversModal,
                    enter = fadeIn(tween(180)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.96f),
                    exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.96f),
                ) {
                    AllCoversGalleryOverlay(
                        covers = availableCovers,
                        selectedCover = config.cover,
                        onSelectCover = { chosen ->
                            config = config.copy(cover = chosen)
                            showAllCoversModal = false
                        },
                        onDismiss = { showAllCoversModal = false },
                        isDark = isDark,
                    )
                }
            }
        }
    }
}

// ============================================================================
// PREVIEWS AND CARDS IMPLEMENTATION
// ============================================================================

@Composable
private fun CoverPreviewCard(cover: CoverItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val assetBitmap = remember(cover.assetPath) {
        cover.assetPath?.let { CoverRepository.loadAssetBitmap(context, it, 460, 620) }
    }

    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0x88000000))
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (assetBitmap != null) {
            Image(
                bitmap = assetBitmap,
                contentDescription = cover.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Procedural Cover Rendering
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                when (cover.type) {
                    ProceduralCoverType.NOTES, ProceduralCoverType.DARK -> {
                        // Deep textured leather
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF22201E), Color(0xFF141312), Color(0xFF0D0D0D))
                            )
                        )

                        // Subtle spine highlight
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0x33FFFFFF), Color.Transparent),
                                startX = 0f,
                                endX = w * 0.08f,
                            )
                        )

                        // Gold border line
                        drawRoundRect(
                            color = Color(0x44D4AF37),
                            topLeft = Offset(w * 0.08f, h * 0.06f),
                            size = Size(w * 0.84f, h * 0.88f),
                            cornerRadius = CornerRadius(8f, 8f),
                            style = Stroke(width = 2f)
                        )

                        // Gold horizontal accent lines
                        val lineY = h * 0.65f
                        for (i in 0..3) {
                            drawLine(
                                color = Color(0x66D4AF37),
                                start = Offset(w * 0.25f, lineY + (i * 18f)),
                                end = Offset(w * 0.75f, lineY + (i * 18f)),
                                strokeWidth = 2f,
                            )
                        }

                        // Right snap leather clasp
                        drawRoundRect(
                            color = Color(0xFF332F2A),
                            topLeft = Offset(w - 24f, h * 0.45f),
                            size = Size(28f, 44f),
                            cornerRadius = CornerRadius(6f, 6f),
                        )
                        drawCircle(
                            color = Color(0xFFD4AF37),
                            radius = 4f,
                            center = Offset(w - 12f, h * 0.45f + 22f)
                        )
                    }

                    ProceduralCoverType.CLASSIC -> {
                        // Rich brown leather
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF8B4513), Color(0xFF5C2C16), Color(0xFF3D1C06))
                            )
                        )
                        // Stitch line
                        drawRoundRect(
                            color = Color(0x77FFFFFF),
                            topLeft = Offset(w * 0.06f, h * 0.05f),
                            size = Size(w * 0.88f, h * 0.90f),
                            cornerRadius = CornerRadius(8f, 8f),
                            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                        )
                    }

                    ProceduralCoverType.DIARY -> {
                        // Royal navy blue leather
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF1E3A5F), Color(0xFF152840), Color(0xFF0F1B2D))
                            )
                        )
                    }

                    ProceduralCoverType.VINTAGE -> {
                        // Aged parchment
                        drawRect(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFFE8D3A2), Color(0xFFC7A76D), Color(0xFF8B6C38))
                            )
                        )
                    }

                    ProceduralCoverType.MARBLE -> {
                        // Carrara marble white with veins
                        drawRect(Color(0xFFF3F4F6))
                        drawLine(Color(0x33A0AEC0), Offset(w * 0.2f, 0f), Offset(w * 0.8f, h), strokeWidth = 3f)
                        drawLine(Color(0x22A0AEC0), Offset(w * 0.1f, h * 0.4f), Offset(w * 0.9f, h * 0.6f), strokeWidth = 2f)
                    }

                    ProceduralCoverType.GRADIENT -> {
                        // Holographic gradient
                        drawRect(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFFFBE18), Color(0xFFF43F5E), Color(0xFF38BDF8), Color(0xFFF472B6))
                            )
                        )
                    }

                    ProceduralCoverType.MINIMAL -> {
                        drawRect(Color(0xFF1E242B))
                        drawLine(Color(0x33D4AF37), Offset(w * 0.15f, h * 0.7f), Offset(w * 0.85f, h * 0.7f), strokeWidth = 2f)
                    }

                    ProceduralCoverType.NONE -> {
                        drawRect(Color(0xFF151D28))
                        drawCircle(
                            color = Color(0x4488A0C0),
                            radius = 28f,
                            center = Offset(w / 2, h / 2),
                            style = Stroke(width = 3f)
                        )
                        drawLine(
                            color = Color(0x4488A0C0),
                            start = Offset(w / 2 - 20f, h / 2 - 20f),
                            end = Offset(w / 2 + 20f, h / 2 + 20f),
                            strokeWidth = 3f,
                        )
                    }

                    else -> {
                        drawRect(Color(0xFF18202A))
                    }
                }
            }

            if (cover.type == ProceduralCoverType.NOTES) {
                Text(
                    text = "Notes",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFFE5C158),
                    modifier = Modifier.padding(bottom = 60.dp)
                )
            }
        }
    }
}

@Composable
private fun PagePreviewCard(config: NewNoteConfiguration, modifier: Modifier = Modifier) {
    val dateText = remember {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
    }

    Box(
        modifier = modifier
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(10.dp), spotColor = Color(0x66000000))
            .clip(RoundedCornerShape(10.dp))
            .background(config.paperColor.color)
            .border(1.dp, Color(0x26000000), RoundedCornerShape(10.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val isDark = config.paperColor.isDark
            val lineColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)
            val marginLineColor = if (isDark) Color(0x44F87171) else Color(0x44EF4444)

            val marginInset = when (config.margins) {
                MarginOption.NONE -> 0f
                MarginOption.NARROW -> w * 0.06f
                MarginOption.NORMAL -> w * 0.12f
                MarginOption.WIDE -> w * 0.18f
            }

            val contentLeft = marginInset
            val contentRight = w - marginInset

            when (config.paperStyle) {
                PaperStyleOption.RULED -> {
                    val lineSpacing = 16f
                    var y = h * 0.15f
                    while (y < h * 0.92f) {
                        drawLine(lineColor, Offset(contentLeft, y), Offset(contentRight, y), strokeWidth = 1.5f)
                        y += lineSpacing
                    }
                }

                PaperStyleOption.LINED_WIDE -> {
                    val lineSpacing = 22f
                    var y = h * 0.15f
                    while (y < h * 0.92f) {
                        drawLine(lineColor, Offset(contentLeft, y), Offset(contentRight, y), strokeWidth = 1.5f)
                        y += lineSpacing
                    }
                }

                PaperStyleOption.LINED_NARROW -> {
                    val lineSpacing = 11f
                    var y = h * 0.15f
                    while (y < h * 0.92f) {
                        drawLine(lineColor, Offset(contentLeft, y), Offset(contentRight, y), strokeWidth = 1.2f)
                        y += lineSpacing
                    }
                }

                PaperStyleOption.GRID -> {
                    val gridSpacing = 14f
                    var x = contentLeft
                    while (x <= contentRight) {
                        drawLine(lineColor, Offset(x, h * 0.1f), Offset(x, h * 0.9f), strokeWidth = 1f)
                        x += gridSpacing
                    }
                    var y = h * 0.1f
                    while (y <= h * 0.9f) {
                        drawLine(lineColor, Offset(contentLeft, y), Offset(contentRight, y), strokeWidth = 1f)
                        y += gridSpacing
                    }
                }

                PaperStyleOption.DOTTED -> {
                    val dotSpacing = 16f
                    var x = contentLeft + 8f
                    while (x < contentRight) {
                        var y = h * 0.12f
                        while (y < h * 0.9f) {
                            drawCircle(lineColor, radius = 1.2f, center = Offset(x, y))
                            y += dotSpacing
                        }
                        x += dotSpacing
                    }
                }

                PaperStyleOption.CORNELL -> {
                    val leftCue = w * 0.32f
                    val summaryTop = h * 0.78f
                    drawLine(marginLineColor, Offset(leftCue, h * 0.1f), Offset(leftCue, summaryTop), strokeWidth = 2f)
                    drawLine(marginLineColor, Offset(contentLeft, summaryTop), Offset(contentRight, summaryTop), strokeWidth = 2f)
                    var y = h * 0.15f
                    while (y < summaryTop) {
                        drawLine(lineColor, Offset(leftCue, y), Offset(contentRight, y), strokeWidth = 1.5f)
                        y += 16f
                    }
                }

                PaperStyleOption.TWO_COLUMN -> {
                    val mid = w / 2
                    drawLine(lineColor, Offset(mid, h * 0.1f), Offset(mid, h * 0.9f), strokeWidth = 1.5f)
                    var y = h * 0.15f
                    while (y < h * 0.9f) {
                        drawLine(lineColor, Offset(contentLeft, y), Offset(mid - 6f, y), strokeWidth = 1f)
                        drawLine(lineColor, Offset(mid + 6f, y), Offset(contentRight, y), strokeWidth = 1f)
                        y += 16f
                    }
                }

                PaperStyleOption.CHECKLIST -> {
                    var y = h * 0.15f
                    while (y < h * 0.9f) {
                        drawRoundRect(
                            color = lineColor,
                            topLeft = Offset(contentLeft + 4f, y - 8f),
                            size = Size(10f, 10f),
                            cornerRadius = CornerRadius(2f, 2f),
                            style = Stroke(width = 1.5f)
                        )
                        drawLine(lineColor, Offset(contentLeft + 22f, y), Offset(contentRight, y), strokeWidth = 1.5f)
                        y += 20f
                    }
                }

                PaperStyleOption.MUSIC -> {
                    for (staff in 0..3) {
                        val startY = h * 0.14f + (staff * 38f)
                        if (startY + 20f < h * 0.92f) {
                            for (l in 0..4) {
                                drawLine(lineColor, Offset(contentLeft, startY + (l * 4.5f)), Offset(contentRight, startY + (l * 4.5f)), strokeWidth = 1.2f)
                            }
                        }
                    }
                }

                PaperStyleOption.STORYBOARD -> {
                    val boxW = (contentRight - contentLeft - 12f) / 2
                    val boxH = h * 0.28f
                    val y1 = h * 0.12f
                    val y2 = h * 0.52f

                    drawRoundRect(lineColor, Offset(contentLeft, y1), Size(boxW, boxH), CornerRadius(4f, 4f), style = Stroke(1.5f))
                    drawRoundRect(lineColor, Offset(contentLeft + boxW + 12f, y1), Size(boxW, boxH), CornerRadius(4f, 4f), style = Stroke(1.5f))
                    drawRoundRect(lineColor, Offset(contentLeft, y2), Size(boxW, boxH), CornerRadius(4f, 4f), style = Stroke(1.5f))
                    drawRoundRect(lineColor, Offset(contentLeft + boxW + 12f, y2), Size(boxW, boxH), CornerRadius(4f, 4f), style = Stroke(1.5f))
                }

                PaperStyleOption.PLAIN -> {
                    // Blank paper
                }
            }
        }

        // Overlay Date if enabled
        if (config.showDate) {
            Text(
                text = dateText,
                fontSize = 9.sp,
                color = if (config.paperColor.isDark) Color(0x88FFFFFF) else Color(0x88000000),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 12.dp)
            )
        }

        // Overlay Page number if enabled
        if (config.showPageNumber) {
            Text(
                text = "1",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (config.paperColor.isDark) Color(0x88FFFFFF) else Color(0x88000000),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 8.dp, end = 12.dp)
            )
        }
    }
}

@Composable
private fun NoteTypeCard(
    title: String,
    description: String,
    isSelected: Boolean,
    accentColor: Color,
    iconBadge: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSelected) (if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5)) else (if (isDark) Color(0xFF18181B) else Color.White)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7)),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        // Selection Checkmark Badge
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White else Color(0xFF09090B))
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.check,
                    contentDescription = "Selected",
                    tint = if (isDark) Color(0xFF09090B) else Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            iconBadge()
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFF5F7FA) else Color(0xFF151922),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A),
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun PaperStyleCard(
    style: PaperStyleOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(width = 92.dp, height = 110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0xFF18181B) else Color.White)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) SelectedBorderColor else (if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7)),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Miniature pattern drawing
            Canvas(modifier = Modifier.size(width = 66.dp, height = 80.dp)) {
                val w = size.width
                val h = size.height
                val lc = if (isDark) Color(0x4488A0C0) else Color(0x44A1A1AA)

                when (style) {
                    PaperStyleOption.PLAIN -> {
                        drawRoundRect(if (isDark) Color(0x15FFFFFF) else Color(0x0A000000), Offset.Zero, size, CornerRadius(4f, 4f))
                    }
                    PaperStyleOption.RULED -> {
                        var y = 10f
                        while (y < h - 8f) {
                            drawLine(lc, Offset(4f, y), Offset(w - 4f, y), strokeWidth = 1.5f)
                            y += 12f
                        }
                    }
                    PaperStyleOption.LINED_WIDE -> {
                        var y = 12f
                        while (y < h - 8f) {
                            drawLine(lc, Offset(4f, y), Offset(w - 4f, y), strokeWidth = 1.5f)
                            y += 16f
                        }
                    }
                    PaperStyleOption.LINED_NARROW -> {
                        var y = 8f
                        while (y < h - 8f) {
                            drawLine(lc, Offset(4f, y), Offset(w - 4f, y), strokeWidth = 1.2f)
                            y += 8f
                        }
                    }
                    PaperStyleOption.GRID -> {
                        var x = 6f
                        while (x < w) {
                            drawLine(lc, Offset(x, 6f), Offset(x, h - 6f), strokeWidth = 1f)
                            x += 10f
                        }
                        var y = 6f
                        while (y < h) {
                            drawLine(lc, Offset(6f, y), Offset(w - 6f, y), strokeWidth = 1f)
                            y += 10f
                        }
                    }
                    PaperStyleOption.DOTTED -> {
                        var x = 10f
                        while (x < w - 6f) {
                            var y = 10f
                            while (y < h - 6f) {
                                drawCircle(lc, radius = 1.2f, center = Offset(x, y))
                                y += 12f
                            }
                            x += 12f
                        }
                    }
                    PaperStyleOption.CORNELL -> {
                        drawLine(Color(0x66EF4444), Offset(w * 0.35f, 6f), Offset(w * 0.35f, h - 6f), strokeWidth = 1.5f)
                        var y = 12f
                        while (y < h - 8f) {
                            drawLine(lc, Offset(w * 0.35f, y), Offset(w - 4f, y), strokeWidth = 1.2f)
                            y += 12f
                        }
                    }
                    PaperStyleOption.TWO_COLUMN -> {
                        val mid = w / 2
                        drawLine(lc, Offset(mid, 6f), Offset(mid, h - 6f), strokeWidth = 1.5f)
                        var y = 12f
                        while (y < h - 8f) {
                            drawLine(lc, Offset(4f, y), Offset(mid - 3f, y), strokeWidth = 1f)
                            drawLine(lc, Offset(mid + 3f, y), Offset(w - 4f, y), strokeWidth = 1f)
                            y += 12f
                        }
                    }
                    PaperStyleOption.CHECKLIST -> {
                        var y = 14f
                        while (y < h - 8f) {
                            drawRoundRect(lc, Offset(4f, y - 5f), Size(7f, 7f), CornerRadius(1.5f, 1.5f), style = Stroke(1.2f))
                            drawLine(lc, Offset(14f, y), Offset(w - 4f, y), strokeWidth = 1.2f)
                            y += 14f
                        }
                    }
                    PaperStyleOption.MUSIC -> {
                        for (staff in 0..1) {
                            val startY = 16f + (staff * 32f)
                            for (l in 0..4) {
                                drawLine(lc, Offset(4f, startY + (l * 4f)), Offset(w - 4f, startY + (l * 4f)), strokeWidth = 1f)
                            }
                        }
                    }
                    PaperStyleOption.STORYBOARD -> {
                        val boxW = (w - 10f) / 2
                        val boxH = h * 0.35f
                        drawRoundRect(lc, Offset(2f, 8f), Size(boxW, boxH), CornerRadius(3f, 3f), style = Stroke(1.2f))
                        drawRoundRect(lc, Offset(6f + boxW, 8f), Size(boxW, boxH), CornerRadius(3f, 3f), style = Stroke(1.2f))
                        drawRoundRect(lc, Offset(2f, 16f + boxH), Size(boxW, boxH), CornerRadius(3f, 3f), style = Stroke(1.2f))
                        drawRoundRect(lc, Offset(6f + boxW, 16f + boxH), Size(boxW, boxH), CornerRadius(3f, 3f), style = Stroke(1.2f))
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange)
                        .align(Alignment.TopEnd)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.check,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF09090B) else Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = style.title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) PrimaryYellow else (if (isDark) Color(0xFFBAC7D5) else Color(0xFF71717A)),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PaperColorSwatch(
    item: PaperColorItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(item.color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) SelectedBorderColor else Color(0x38000000),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(PrimaryOrange),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.check,
                    contentDescription = null,
                    tint = if (com.pnotes.ui.theme.LocalPalette.current.isDark) Color(0xFF09090B) else Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
private fun CoverThumbnailCard(
    cover: CoverItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    val context = LocalContext.current
    val assetBitmap = remember(cover.assetPath) {
        cover.assetPath?.let { CoverRepository.loadAssetBitmap(context, it, 180, 240) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(width = 92.dp, height = 125.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) SelectedBorderColor else (if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7)),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (assetBitmap != null) {
                Image(
                    bitmap = assetBitmap,
                    contentDescription = cover.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CoverPreviewCard(cover = cover, modifier = Modifier.fillMaxSize())
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.check,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF09090B) else Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = cover.name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) PrimaryYellow else (if (isDark) Color(0xFFBAC7D5) else Color(0xFF71717A)),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 92.dp)
        )
    }
}

@Composable
private fun OrientationButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) (if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5)) else (if (isDark) Color(0xFF18181B) else Color.White))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7)),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) PrimaryYellow else (if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A)),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) PrimaryYellow else (if (isDark) Color(0xFF9CA6B5) else Color(0xFF71717A)),
        )
    }
}

// ============================================================================
// ALL COVERS GALLERY OVERLAY & CATEGORIES
// ============================================================================

private val CoverCategories = listOf(
    "All",
    "Botanical & Floral",
    "Animals & Wildlife",
    "Pop & Retro",
    "Cute & Cozy",
    "Woodland & Nature",
    "Fruits & Fresh",
    "Safari & Wild",
    "Pixel & Play",
    "Design & Layouts",
    "Classic & Textures",
)

private fun getCoverCategory(cover: CoverItem): String {
    val id = cover.id.lowercase()
    val name = cover.name.lowercase()
    return when {
        id == "none" -> "All"
        id in listOf("minimal", "notes", "classic", "dark", "diary", "vintage", "marble", "gradient") -> "Classic & Textures"
        id.contains("daisy") || id.contains("matisse") || id.contains("sakura") || id.contains("monstera") || id.contains("flora") || name.contains("botanical") || name.contains("floral") -> "Botanical & Floral"
        id.contains("tiger") || id.contains("cat") || id.contains("toucan") || id.contains("whale") || id.contains("fox") -> "Animals & Wildlife"
        id.contains("y2k") || id.contains("memphis") || id.contains("sticker") || id.contains("groovy") || id.contains("comic") -> "Pop & Retro"
        id.contains("dino") || id.contains("bear") || id.contains("croissant") || id.contains("dog") || id.contains("mountain") -> "Cute & Cozy"
        id.contains("mushroom") || id.contains("owl") || id.contains("bee") || id.contains("otter") || id.contains("hedgehog") -> "Woodland & Nature"
        id.contains("watermelon") || id.contains("lemon") || id.contains("strawberry") || id.contains("avocado") || id.contains("peach") -> "Fruits & Fresh"
        id.contains("flamingo") || id.contains("cactus") || id.contains("giraffe") || id.contains("panda") || id.contains("sloth") -> "Safari & Wild"
        id.contains("pixel") || id.contains("alien") || id.contains("monster") || id.contains("balloon") || id.contains("plane") -> "Pixel & Play"
        id.contains("frame") || id.contains("masthead") || id.contains("pattern") || id.contains("lens") || id.contains("grid") || id.contains("wedge") || id.contains("stage") || id.contains("dunes") || id.contains("column") || id.contains("polaroid") || id.contains("bezel") || id.contains("beam") || id.contains("trajectory") -> "Design & Layouts"
        else -> "Classic & Textures"
    }
}

@Composable
private fun AllCoversGalleryOverlay(
    covers: List<CoverItem>,
    selectedCover: CoverItem,
    onSelectCover: (CoverItem) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredCovers = remember(covers, selectedCategory, searchQuery) {
        covers.filter { cover ->
            val matchesCategory = if (selectedCategory == "All") {
                true
            } else {
                getCoverCategory(cover) == selectedCategory
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim()
                cover.name.contains(q, ignoreCase = true) ||
                getCoverCategory(cover).contains(q, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    if (isDark) listOf(Color(0xFF14171E), Color(0xFF0D0F14))
                    else listOf(Color(0xFFFFFFFF), Color(0xFFF8F9FA))
                )
            )
            .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // === 1. TOP HEADER ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5))
                            .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PnotesIcons.layoutGrid,
                            contentDescription = null,
                            tint = if (isDark) Color.White else Color(0xFF09090B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Choose a cover",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF5F7FA) else Color(0xFF151922)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${filteredCovers.size} of ${covers.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
                                )
                            }
                        }
                        Text(
                            text = "Tap any cover to select it immediately",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF8E9BAE) else Color(0xFF71717A)
                        )
                    }
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFF4F4F5))
                        .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PnotesIcons.close,
                        contentDescription = "Close",
                        tint = if (isDark) Color(0xFFE8ECF2) else Color(0xFF151922),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // === 2. SEARCH BAR ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0xFF1C212B) else Color(0xFFF4F4F6))
                    .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = PnotesIcons.search,
                    contentDescription = "Search",
                    tint = if (isDark) Color(0xFF8E9BAE) else Color(0xFF71717A),
                    modifier = Modifier.size(18.dp)
                )

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = if (isDark) Color(0xFFF5F7FA) else Color(0xFF151922),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    cursorBrush = SolidColor(if (isDark) Color.White else Color(0xFF09090B)),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search covers (e.g. Tiger, Daisy, Vintage, Pop)...",
                                color = if (isDark) Color(0xFF5F6E82) else Color(0xFFA1A1AA),
                                fontSize = 14.sp,
                            )
                        }
                        innerTextField()
                    }
                )

                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2E3544) else Color(0xFFE4E4E7))
                            .clickable { searchQuery = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PnotesIcons.close,
                            contentDescription = "Clear",
                            tint = if (isDark) Color(0xFFF5F7FA) else Color(0xFF151922),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // === 3. CATEGORY PILLS ===
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CoverCategories) { category ->
                    val isCatSelected = selectedCategory == category
                    val count = if (category == "All") {
                        covers.size
                    } else {
                        covers.count { getCoverCategory(it) == category }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isCatSelected) {
                                    if (isDark) Color.White else Color(0xFF09090B)
                                } else {
                                    if (isDark) Color(0xFF1B202A) else Color(0xFFF4F4F6)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isCatSelected) {
                                    Color.Transparent
                                } else {
                                    if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7)
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 12.5.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCatSelected) {
                                if (isDark) Color(0xFF09090B) else Color.White
                            } else {
                                if (isDark) Color(0xFFBAC7D5) else Color(0xFF71717A)
                            }
                        )
                        Text(
                            text = "$count",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCatSelected) {
                                if (isDark) Color(0xFF52525B) else Color(0xFFA1A1AA)
                            } else {
                                if (isDark) Color(0xFF71717A) else Color(0xFFA1A1AA)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // === 4. COVERS GRID ===
            if (filteredCovers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1C212B) else Color(0xFFF4F4F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PnotesIcons.search,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF71717A) else Color(0xFFA1A1AA),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "No covers found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFF5F7FA) else Color(0xFF151922)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Try searching with a different keyword or selecting 'All'",
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF8E9BAE) else Color(0xFF71717A)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                searchQuery = ""
                                selectedCategory = "All"
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7),
                                contentColor = if (isDark) Color.White else Color(0xFF09090B)
                            )
                        ) {
                            Text("Reset filters", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 8.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredCovers, key = { it.id }) { coverItem ->
                        val isSelected = coverItem.id == selectedCover.id
                        GalleryCoverGridCard(
                            cover = coverItem,
                            isSelected = isSelected,
                            isDark = isDark,
                            onClick = {
                                onSelectCover(coverItem)
                            }
                        )
                    }
                }
            }

            // === 5. BOTTOM BAR ===
            Surface(
                color = if (isDark) Color(0xFF0E1117) else Color(0xFFF4F4F6),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, if (isDark) Color(0x18FFFFFF) else Color(0xFFE4E4E7)),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Selected cover:",
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF8E9BAE) else Color(0xFF71717A)
                        )
                        Text(
                            text = selectedCover.name,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF09090B)
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color.White else Color(0xFF09090B),
                            contentColor = if (isDark) Color(0xFF09090B) else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryCoverGridCard(
    cover: CoverItem,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assetBitmap = remember(cover.assetPath) {
        cover.assetPath?.let { CoverRepository.loadAssetBitmap(context, it, 260, 360) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .shadow(
                    elevation = if (isSelected) 12.dp else 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = if (isSelected) Color(0x88000000) else Color(0x44000000)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0xFF18181B) else Color.White)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) {
                        if (isDark) Color.White else Color(0xFF09090B)
                    } else {
                        if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7)
                    },
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (assetBitmap != null) {
                Image(
                    bitmap = assetBitmap,
                    contentDescription = cover.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CoverPreviewCard(cover = cover, modifier = Modifier.fillMaxSize())
            }

            // Top-Right Selected Checkmark Badge
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White else Color(0xFF09090B))
                        .align(Alignment.TopEnd)
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PnotesIcons.check,
                        contentDescription = "Selected",
                        tint = if (isDark) Color(0xFF09090B) else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = cover.name,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) {
                if (isDark) Color.White else Color(0xFF09090B)
            } else {
                if (isDark) Color(0xFFCAD5E2) else Color(0xFF52525B)
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

