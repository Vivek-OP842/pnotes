package com.pnotes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pnotes.R
import com.pnotes.ui.icons.PnotesIcons

// Cached static brushes and color palettes to avoid GC allocations during recomposition
private val DarkDialogBgBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF454549), Color(0xFF3F3F43))
)
private val LightDialogBgBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFFAFAFA), Color(0xFFF4F4F5))
)

private val DarkNoteCardColors = listOf(Color(0xFF4C4C50), Color(0xFF444448))
private val LightNoteCardColors = listOf(Color(0xFFFFFFFF), Color(0xFFF9F9FA))
private val DarkCanvasCardColors = listOf(Color(0xFF4C4C50), Color(0xFF444448))
private val LightCanvasCardColors = listOf(Color(0xFFFFFFFF), Color(0xFFF9F9FA))
private val DarkPdfCardColors = listOf(Color(0xFF4C4C50), Color(0xFF444448))
private val LightPdfCardColors = listOf(Color(0xFFFFFFFF), Color(0xFFF9F9FA))
private val DarkFolderCardColors = listOf(Color(0xFF4C4C50), Color(0xFF444448))
private val LightFolderCardColors = listOf(Color(0xFFFFFFFF), Color(0xFFF9F9FA))

/**
 * Step 1: "What would you like to create?" Modal Dialog matching reference design.
 * Offers 4 options:
 * 1. Handwritten note -> Opens Step 2 (PnotesNewNoteDialog with cover & paper customization)
 * 2. New canvas -> Creates and launches infinite drawing canvas
 * 3. Import PDF -> Opens system file picker for PDF documents
 * 4. New folder -> Creates a subfolder in the current workspace directory
 */
@Composable
fun CreateActionDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onNewNote: () -> Unit,
    onNewCanvas: () -> Unit,
    onImportPdf: () -> Unit,
    onNewFolder: () -> Unit,
    dontShowAgain: Boolean = false,
    onDontShowAgainChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
    ) {
        BackHandler { onDismiss() }

        var dialogCardVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            dialogCardVisible = true
        }

        val scrimAlpha by animateFloatAsState(
            targetValue = if (dialogCardVisible) 0.45f else 0f,
            animationSpec = tween(180),
            label = "createScrimAlpha",
        )
        val cardScale by animateFloatAsState(
            targetValue = if (dialogCardVisible) 1.0f else 0.92f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "createCardScale",
        )
        val cardAlpha by animateFloatAsState(
            targetValue = if (dialogCardVisible) 1.0f else 0f,
            animationSpec = tween(180),
            label = "createCardAlpha",
        )

        // Scrim backdrop
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = if (isDark) Color(0xFF454549) else Color.White,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF58585E) else Color(0xFFE4E4E7)),
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .widthIn(min = 520.dp, max = 580.dp)
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* prevent click dismissal */ },
                    ),
            ) {
                // Warm/dark subtle gradient background for the dialog surface
                Box(
                    modifier = Modifier.background(
                        brush = if (isDark) DarkDialogBgBrush else LightDialogBgBrush,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 26.dp, start = 26.dp, end = 26.dp, bottom = 18.dp),
                    ) {
                        // Header: "What would you like to create?" + Close Button
                        CreateDialogHeader(onClose = onDismiss)

                        Spacer(Modifier.height(24.dp))

                        // 2x2 Grid of Option Cards
                        CreateOptionGrid(
                            onNewNote = onNewNote,
                            onNewCanvas = onNewCanvas,
                            onImportPdf = onImportPdf,
                            onNewFolder = onNewFolder,
                        )

                        Spacer(Modifier.height(24.dp))

                        // Thin footer divider
                        HorizontalDivider(
                            color = if (isDark) Color(0xFF46464B) else Color(0xFFE4E4E7),
                            thickness = 1.dp,
                        )

                        Spacer(Modifier.height(14.dp))

                        // Footer: "Don't show again" checkbox & "Cancel" button
                        CreateDialogFooter(
                            dontShowAgain = dontShowAgain,
                            onDontShowAgainChange = onDontShowAgainChange,
                            onCancel = onDismiss,
                        )
                    }
                }
            }
        }
    }
}

/** Dialog Header with centered title and top-right close button */
@Composable
private fun CreateDialogHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "What would you like to create?",
            color = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0D1B36),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            textAlign = TextAlign.Center,
        )

        // Close X button at the top-right
        Surface(
            onClick = onClose,
            shape = CircleShape,
            color = if (isDark) Color(0xFF38383C) else Color(0xFFF4F4F5),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = PnotesIcons.close,
                    contentDescription = "Close dialog",
                    tint = if (isDark) Color(0xFFF5F7FA) else Color(0xFF151922),
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/** 2x2 Grid of creation cards */
@Composable
private fun CreateOptionGrid(
    onNewNote: () -> Unit,
    onNewCanvas: () -> Unit,
    onImportPdf: () -> Unit,
    onNewFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Row 1: New note & New canvas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CreateOptionCard(
                title = "New note",
                description = "Create a new handwritten\nnote",
                cardBgColors = if (isDark) DarkNoteCardColors else LightNoteCardColors,
                borderColor = if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7),
                iconComposable = { YellowPencilIcon() },
                onClick = onNewNote,
                modifier = Modifier.weight(1f),
            )

            CreateOptionCard(
                title = "New canvas",
                description = "Create a blank canvas for\nsketching and drawing",
                cardBgColors = if (isDark) DarkCanvasCardColors else LightCanvasCardColors,
                borderColor = if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7),
                iconComposable = { BlueDrawingIcon() },
                onClick = onNewCanvas,
                modifier = Modifier.weight(1f),
            )
        }

        // Row 2: Import PDF & New folder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CreateOptionCard(
                title = "Import PDF",
                description = "Annotate and study PDF\ndocuments",
                cardBgColors = if (isDark) DarkPdfCardColors else LightPdfCardColors,
                borderColor = if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7),
                iconComposable = { GreenImportIcon() },
                onClick = onImportPdf,
                modifier = Modifier.weight(1f),
            )

            CreateOptionCard(
                title = "New folder",
                description = "Organize notes into a new\nsubfolder",
                cardBgColors = if (isDark) DarkFolderCardColors else LightFolderCardColors,
                borderColor = if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7),
                iconComposable = { MonochromeFolderIcon() },
                onClick = onNewFolder,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Individual 3D Glassmorphic Option Card with glowing icon badge
 */
@Composable
private fun CreateOptionCard(
    title: String,
    description: String,
    cardBgColors: List<Color>,
    borderColor: Color,
    iconComposable: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
        label = "card_scale",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = if (isPressed) 1.dp else 2.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(210.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(cardBgColors))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Top Centered Icon with subtle float
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    iconComposable()
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = title,
                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF09090B),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = description,
                    color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Handwriting Pencil Icon matching Card 1 */
@Composable
private fun YellowPencilIcon() {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = com.pnotes.ui.icons.PnotesIcons.singleStickPen,
            contentDescription = "New note",
            tint = if (isDark) Color.White else Color(0xFF09090B),
            modifier = Modifier.size(52.dp),
        )
    }
}

/** Blue Drawing Nib / Stylus Icon matching Card 2 reference */
@Composable
private fun BlueDrawingIcon() {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_action_canvas),
            contentDescription = "New canvas",
            tint = if (isDark) Color.White else Color(0xFF09090B),
            modifier = Modifier.size(52.dp),
        )
    }
}

/** Mint Green PDF Upload Tray Icon matching Card 3 reference */
@Composable
private fun GreenImportIcon() {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_action_import_pdf),
            contentDescription = "Import PDF",
            tint = if (isDark) Color.White else Color(0xFF09090B),
            modifier = Modifier.size(52.dp),
        )
    }
}

/** Monochrome 3D Folder Icon matching Card 4 reference */
@Composable
private fun MonochromeFolderIcon() {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_action_add_folder),
            contentDescription = "New folder",
            tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
            modifier = Modifier.size(52.dp),
        )
    }
}

/** Dialog Footer with "Don't show again" checkbox and "Cancel" action */
@Composable
private fun CreateDialogFooter(
    dontShowAgain: Boolean,
    onDontShowAgainChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = com.pnotes.ui.theme.LocalPalette.current.isDark
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "Don't show again" Checkbox Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onDontShowAgainChange(!dontShowAgain) }
                .padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            // Custom rounded square checkbox matching the reference design
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (dontShowAgain) (if (isDark) Color.White else Color(0xFF09090B)) else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (dontShowAgain) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFF27272A) else Color(0xFFD4D4D8)),
                        shape = RoundedCornerShape(6.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (dontShowAgain) {
                    Icon(
                        imageVector = PnotesIcons.check,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF09090B) else Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Don't show again",
                color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF687083),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        // Cancel Text Button
        TextButton(
            onClick = onCancel,
            modifier = Modifier.height(36.dp),
        ) {
            Text(
                text = "Cancel",
                color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF687083),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
