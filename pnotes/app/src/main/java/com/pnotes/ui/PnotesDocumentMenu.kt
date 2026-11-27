package com.pnotes.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.NormalModeDarkPill
import com.pnotes.ui.theme.NormalModePillBorder
import com.pnotes.ui.theme.NormalModeSolidColor

/**
 * Pixel-perfect Document Popover Menu for PNotes (3-dots).
 * Faithfully implements the 2-tier prototype workflow:
 * 1. Opens as the compact **Stage Anchored Popover** (Quick Action Popover).
 * 2. Clicking **"Show All Options" / "Full Menu"** seamlessly expands to the **Full Menu**.
 * Styled in the Slate Zinc / Titanium Charcoal design system (Linear & Raycast aesthetic).
 */
@Composable
fun PnotesDocumentMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onOpenPageStyles: () -> Unit,
    onOpenMargins: () -> Unit,
    onOpenPageAngle: () -> Unit = {},
    onOpenViewOptions: () -> Unit = {},
    onOpenPageJump: () -> Unit = {},
    onTogglePresentation: () -> Unit = {},
    isPresentationActive: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    isFullscreenActive: Boolean = false,
    onToggleZoomLock: () -> Unit = {},
    isZoomLocked: Boolean = false,
    onToggleFullLengthToolbar: () -> Unit = {},
    isFullLengthToolbar: Boolean = false,
    onShareCurrentPage: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onDuplicatePage: () -> Unit = {},
    onToggleTheme: ((Boolean) -> Unit)? = null,
    pageIndex: Int = 0,
    pageCount: Int = 1,
    currentMarginsLabel: String = "16mm",
    currentPageAngle: Float = 0f,
    offset: DpOffset = DpOffset(0.dp, 8.dp),
) {
    val isDark = isGlassDark()
    var isFullMenu by remember { mutableStateOf(false) }

    // Reset to compact quick popover whenever the menu is closed
    LaunchedEffect(expanded) {
        if (!expanded) {
            isFullMenu = false
        }
    }

    // Apple 2C2C2C / Clean Solid Light Tokens
    val cardBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFFFF)
    val cardBorderBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.10f)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE4E4E7), Color(0xFFD4D4D8)))
    }
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE4E4E7)
    val insetRowBg = if (isDark) Color(0xFF383838) else Color(0xFFF4F4F5)
    val dividerCol = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE4E4E7)

    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF18181B)
    val textMuted = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
    val textSubtle = if (isDark) Color(0xFF71717A) else Color(0xFFA1A1AA)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.Transparent,
        border = null,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        AnimatedContent(
            targetState = isFullMenu,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(150))
            },
            label = "3dotMenuTransition"
        ) { showFull ->
            if (!showFull) {
                // =========================================================================
                // TIER 1: STAGE ANCHORED POPOVER (Compact Quick Action Popover - 310dp)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .width(310.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(width = 1.dp, brush = cardBorderBrush, shape = RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 1. Hero Pill: Page Styles & Studio
                    val heroBg = PnotesPrimary
                    val heroFg = Color.White

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(heroBg)
                            .clickable {
                                onDismissRequest()
                                onOpenPageStyles()
                            }
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = PnotesIcons.pageStyles,
                                contentDescription = "Page Styles",
                                tint = heroFg,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Page Styles & Studio",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = heroFg,
                            )
                        }
                        Icon(
                            imageVector = PnotesIcons.chevronRight,
                            contentDescription = null,
                            tint = heroFg,
                            modifier = Modifier.size(12.dp),
                        )
                    }

                    // 2. Page Margins
                    QuickPopoverRow(
                        icon = PnotesIcons.margins,
                        title = "Page Margins",
                        textPrimary = textPrimary,
                        onClick = {
                            onDismissRequest()
                            onOpenMargins()
                        },
                        trailing = {
                            Text(
                                text = currentMarginsLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = textMuted,
                            )
                        }
                    )

                    // 3. Jump to Page
                    QuickPopoverRow(
                        icon = PnotesIcons.thumbnails,
                        title = "Jump to Page",
                        textPrimary = textPrimary,
                        onClick = {
                            onDismissRequest()
                            onOpenPageJump()
                        },
                        trailing = {
                            Text(
                                text = "${pageIndex + 1}/$pageCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                            )
                        }
                    )

                    // Hairline Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(dividerCol)
                    )

                    // 4. Lock Zoom (100%)
                    QuickPopoverRow(
                        icon = PnotesIcons.lock,
                        title = "Lock Zoom (100%)",
                        textPrimary = textPrimary,
                        onClick = onToggleZoomLock,
                        trailing = {
                            MiniTactileSwitch(
                                checked = isZoomLocked,
                                isDark = isDark,
                                onCheckedChange = { onToggleZoomLock() }
                            )
                        }
                    )

                    // 5. Full Screen Focus
                    QuickPopoverRow(
                        icon = PnotesIcons.fullscreen,
                        title = "Full Screen Focus",
                        textPrimary = textPrimary,
                        onClick = {
                            onDismissRequest()
                            onToggleFullscreen()
                        },
                        trailing = {
                            MiniTactileSwitch(
                                checked = isFullscreenActive,
                                isDark = isDark,
                                onCheckedChange = {
                                    onDismissRequest()
                                    onToggleFullscreen()
                                }
                            )
                        }
                    )

                    // 6. Show All Options -> EXPANDS TO FULL MENU
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(insetRowBg)
                            .border(width = 0.5.dp, color = cardBorder, shape = RoundedCornerShape(8.dp))
                            .clickable { isFullMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = PnotesIcons.sliders,
                                contentDescription = "Options",
                                tint = textPrimary,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "Show All Options",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Full Menu",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = textMuted,
                            )
                            Icon(
                                imageVector = PnotesIcons.chevronRight,
                                contentDescription = null,
                                tint = textMuted,
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }

                    // 7. Bottom Action Row: [ Share ] & [ Export PDF ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Share Button
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(insetRowBg)
                                .border(width = 0.5.dp, color = cardBorder, shape = RoundedCornerShape(8.dp))
                                .clickable {
                                    onDismissRequest()
                                    onShareCurrentPage()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.share,
                                contentDescription = "Share",
                                tint = textPrimary,
                                modifier = Modifier.size(11.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Share",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                            )
                        }

                        // Export PDF Button (Inverted Primary)
                        val exportBtnBg = if (isDark) Color(0xFFF4F4F6) else Color(0xFF0F0F12)
                        val exportBtnFg = if (isDark) Color(0xFF0F0F12) else Color(0xFFF4F4F6)

                        Row(
                            modifier = Modifier
                                .weight(1.2f)
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    spotColor = if (isDark) Color(0x35000000) else Color(0x1F0F0F12)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(exportBtnBg)
                                .clickable {
                                    onDismissRequest()
                                    onExportPdf()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.file,
                                contentDescription = "Export PDF",
                                tint = exportBtnFg,
                                modifier = Modifier.size(11.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Export PDF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = exportBtnFg,
                            )
                        }
                    }
                }
            } else {
                // =========================================================================
                // TIER 2: FULL MENU BODY CONTENT (Expanded Full Menu - 360dp)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .heightIn(max = 620.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(width = 1.dp, brush = cardBorderBrush, shape = RoundedCornerShape(16.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Quick Options Back / Collapse Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isFullMenu = false }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = PnotesIcons.pillChevronLeft,
                            contentDescription = "Back",
                            tint = textMuted,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "Quick Options",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMuted,
                        )
                    }

                    // --- SECTION 1: PAPER & CANVAS STUDIO ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MenuHeaderLabel(text = "PAPER & CANVAS STUDIO", color = textMuted)

                        // 1.1 Hero Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(insetRowBg)
                                .border(width = 0.5.dp, color = cardBorder, shape = RoundedCornerShape(12.dp))
                                .clickable {
                                    onDismissRequest()
                                    onOpenPageStyles()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0xFFF4F4F6) else Color(0xFF0F0F12)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = PnotesIcons.pageStyles,
                                        contentDescription = "Page Styles",
                                        tint = if (isDark) Color(0xFF0F0F12) else Color(0xFFF4F4F6),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    text = "Page Styles & Paper Studio",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDark) Color(0xFFF4F4F6) else Color(0xFF0F0F12))
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Studio",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFF0F0F12) else Color(0xFFF4F4F6),
                                    )
                                }
                                Icon(
                                    imageVector = PnotesIcons.chevronRight,
                                    contentDescription = null,
                                    tint = textMuted,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }

                        // 1.2 Page Margins
                        MenuFullRow(
                            icon = PnotesIcons.margins,
                            title = "Page Margins",
                            textPrimary = textPrimary,
                            iconBg = insetRowBg,
                            onClick = {
                                onDismissRequest()
                                onOpenMargins()
                            },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "Normal ($currentMarginsLabel)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textMuted,
                                    )
                                    Icon(
                                        imageVector = PnotesIcons.chevronRight,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF636370) else Color(0xFFD4D4D8),
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        )

                        // 1.3 Page Angle & Orientation
                        MenuFullRow(
                            icon = PnotesIcons.pageAngleCompass,
                            title = "Page Angle & Orientation",
                            textPrimary = textPrimary,
                            iconBg = insetRowBg,
                            onClick = {
                                onDismissRequest()
                                onOpenPageAngle()
                            },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(insetRowBg)
                                        .border(width = 0.5.dp, color = cardBorder, shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val angleText = if (currentPageAngle != 0f) "${currentPageAngle.toInt()}°" else "0°"
                                    Text(
                                        text = "$angleText Portrait",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSubtle,
                                    )
                                }
                            }
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(dividerCol))

                    // --- SECTION 2: PAGE MANAGEMENT ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MenuHeaderLabel(text = "PAGE MANAGEMENT", color = textMuted)

                        // 2.1 Jump to Page
                        MenuFullRow(
                            icon = PnotesIcons.thumbnails,
                            title = "Jump to Page",
                            textPrimary = textPrimary,
                            iconBg = insetRowBg,
                            onClick = {
                                onDismissRequest()
                                onOpenPageJump()
                            },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(insetRowBg)
                                        .border(width = 0.5.dp, color = cardBorder, shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Page ${pageIndex + 1}/$pageCount",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSubtle,
                                    )
                                }
                            }
                        )

                        // 2.2 Duplicate Page
                        MenuFullRow(
                            icon = PnotesIcons.duplicate,
                            title = "Duplicate Page",
                            textPrimary = textPrimary,
                            iconBg = insetRowBg,
                            onClick = {
                                onDismissRequest()
                                onDuplicatePage()
                            },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(insetRowBg)
                                        .border(width = 0.5.dp, color = cardBorder, shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "+ Clone",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSubtle,
                                    )
                                }
                            }
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(dividerCol))

                    // --- SECTION 3: VIEW MODES & ACCESSIBILITY ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MenuHeaderLabel(text = "VIEW MODES & ACCESSIBILITY", color = textMuted)

                        // 3.1 Paper View Tone
                        MenuFullRow(
                            icon = PnotesIcons.contrast,
                            title = "Paper View Tone",
                            textPrimary = textPrimary,
                            iconBg = insetRowBg,
                            onClick = {
                                onDismissRequest()
                                onOpenViewOptions()
                            },
                            trailingContent = {
                                MenuSegmentedTwoPill(
                                    option1 = "Light",
                                    option2 = "Dark",
                                    selectedOption = if (isDark) "Dark" else "Light",
                                    isDark = isDark,
                                    onSelect = { option ->
                                        if (onToggleTheme != null) {
                                            onToggleTheme(option == "Dark")
                                        } else {
                                            onDismissRequest()
                                            onOpenViewOptions()
                                        }
                                    }
                                )
                            }
                        )

                        // 3.2 Lock Zoom Level
                        MenuFullRow(
                            icon = PnotesIcons.lock,
                            title = "Lock Zoom Level",
                            textPrimary = textPrimary,
                            iconBg = insetRowBg,
                            onClick = onToggleZoomLock,
                            trailingContent = {
                                MenuFullTactileSwitch(
                                    checked = isZoomLocked,
                                    isDark = isDark,
                                    onCheckedChange = { onToggleZoomLock() }
                                )
                            }
                        )

                        // 3.3 Full Screen Focus
                        MenuFullRow(
                            icon = PnotesIcons.fullscreen,
                            title = "Full Screen Focus",
                            textPrimary = textPrimary,
                            iconBg = insetRowBg,
                            onClick = {
                                onDismissRequest()
                                onToggleFullscreen()
                            },
                            trailingContent = {
                                MenuFullTactileSwitch(
                                    checked = isFullscreenActive,
                                    isDark = isDark,
                                    onCheckedChange = {
                                        onDismissRequest()
                                        onToggleFullscreen()
                                    }
                                )
                            }
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(dividerCol))

                    // --- SECTION 4: PRESENTATION & WORKSPACE ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MenuHeaderLabel(text = "PRESENTATION & WORKSPACE", color = textMuted)

                        // 4.1 Start/Stop Presentation
                        MenuFullRow(
                            icon = PnotesIcons.present,
                            title = if (isPresentationActive) "Stop Presentation" else "Start Presentation",
                            textPrimary = textPrimary,
                            iconBg = insetRowBg,
                            onClick = {
                                onDismissRequest()
                                onTogglePresentation()
                            },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(insetRowBg)
                                        .border(width = 0.5.dp, color = cardBorder, shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (isPresentationActive) "Live" else "AirPlay",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSubtle,
                                    )
                                }
                            }
                        )

                        // 4.2 Floating Toolbar Dock
                        MenuFullRow(
                            icon = PnotesIcons.sidebar,
                            title = "Floating Toolbar Dock",
                            textPrimary = textPrimary,
                            iconBg = insetRowBg,
                            onClick = onToggleFullLengthToolbar,
                            trailingContent = {
                                MenuFullTactileSwitch(
                                    checked = !isFullLengthToolbar,
                                    isDark = isDark,
                                    onCheckedChange = { onToggleFullLengthToolbar() }
                                )
                            }
                        )


                    }

                    // --- SECTION 5: BOTTOM ACTIONS ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Share Page
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(insetRowBg)
                                .border(width = 0.8.dp, color = cardBorder, shape = RoundedCornerShape(12.dp))
                                .clickable {
                                    onDismissRequest()
                                    onShareCurrentPage()
                                }
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.share,
                                contentDescription = "Share",
                                tint = textPrimary,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Share Page",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                            )
                        }

                        // Export PDF
                        val exportBtnBg = if (isDark) Color(0xFFF4F4F6) else Color(0xFF0F0F12)
                        val exportBtnFg = if (isDark) Color(0xFF0F0F12) else Color(0xFFF4F4F6)

                        Row(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(42.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    spotColor = if (isDark) Color(0x35000000) else Color(0x1F0F0F12)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(exportBtnBg)
                                .clickable {
                                    onDismissRequest()
                                    onExportPdf()
                                }
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.file,
                                contentDescription = "Export PDF",
                                tint = exportBtnFg,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Export PDF (${pageCount}p)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = exportBtnFg,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact row item in the Quick Action Popover (Stage-Anchored).
 */
@Composable
private fun QuickPopoverRow(
    icon: ImageVector,
    title: String,
    textPrimary: Color,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = textPrimary,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
            )
        }

        trailing()
    }
}

/**
 * Full expanded row item in the Full Menu.
 */
@Composable
private fun MenuFullRow(
    icon: ImageVector,
    title: String,
    textPrimary: Color,
    iconBg: Color,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = textPrimary,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
            )
        }

        trailingContent()
    }
}

/**
 * Mini tactile switch used in the Quick Popover (28x16dp with 12dp knob).
 */
@Composable
private fun MiniTactileSwitch(
    checked: Boolean,
    isDark: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val trackBg = if (checked) {
        Color(0xFF34C759) // Apple Green
    } else {
        if (isDark) Color(0xFF39393D) else Color(0xFFE5E5EA)
    }

    val knobOffset by animateDpAsState(
        targetValue = if (checked) 14.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 1800f),
        label = "miniSwitchKnob",
    )

    Box(
        modifier = Modifier
            .width(28.dp)
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(trackBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(12.dp)
                .shadow(elevation = 1.dp, shape = CircleShape, spotColor = Color(0x33000000))
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * Full tactile pill switch used in the Full Menu (36x20dp with 16dp knob).
 */
@Composable
private fun MenuFullTactileSwitch(
    checked: Boolean,
    isDark: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val trackBg = if (checked) {
        Color(0xFF34C759) // Apple Green
    } else {
        if (isDark) Color(0xFF39393D) else Color(0xFFE5E5EA)
    }

    val knobOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 1800f),
        label = "fullSwitchKnob",
    )

    Box(
        modifier = Modifier
            .width(36.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(trackBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(16.dp)
                .shadow(elevation = 2.dp, shape = CircleShape, spotColor = Color(0x33000000))
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * Clean Section Header Typography (9.5sp Bold, letter-spaced).
 */
@Composable
private fun MenuHeaderLabel(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.72.sp,
        color = color,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

/**
 * Compact two-option segmented pill widget (e.g. [ Light | Dark ] or [ Off | On ]).
 */
@Composable
private fun MenuSegmentedTwoPill(
    option1: String,
    option2: String,
    selectedOption: String,
    isDark: Boolean,
    onSelect: (String) -> Unit,
) {
    val containerBg = if (isDark) Color(0xFF222228) else Color(0xFFF4F4F5)
    val containerBorder = if (isDark) Color(0xFF2E2E36) else Color(0xFFE4E4E7)

    val activeBg = if (isDark) Color(0xFFF4F4F6) else Color(0xFF0F0F12)
    val activeFg = if (isDark) Color(0xFF0F0F12) else Color(0xFFF4F4F6)
    val inactiveFg = if (isDark) Color(0xFFD4D4D8) else Color(0xFF52525B)

    Row(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(containerBg)
            .border(width = 0.6.dp, color = containerBorder, shape = RoundedCornerShape(9.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(option1, option2).forEach { opt ->
            val isSelected = opt.equals(selectedOption, ignoreCase = true)
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(2.dp, RoundedCornerShape(7.dp), spotColor = Color(0x22000000))
                                .background(activeBg)
                        } else Modifier
                    )
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = opt,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) activeFg else inactiveFg,
                )
            }
        }
    }
}
