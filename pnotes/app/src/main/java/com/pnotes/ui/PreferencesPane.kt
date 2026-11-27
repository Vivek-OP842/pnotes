package com.pnotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.pnotes.ui.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.core.model.Orientation
import com.pnotes.core.model.PageSize
import com.pnotes.core.model.Rgba
import com.pnotes.core.tools.ToolbarItem
import com.pnotes.core.tools.ToolbarLayout
import com.pnotes.settings.Preferences
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.ColorMath
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.PurpleTheme
import com.pnotes.ui.theme.elevated3D
import com.pnotes.ui.theme.toComposeColor
import kotlinx.coroutines.delay

private val accentPresets = listOf(
    Rgba(255, 255, 255), Rgba(9, 9, 11), Rgba(24, 24, 27), Rgba(39, 39, 42), Rgba(113, 113, 122), Rgba(212, 212, 216), Rgba(244, 244, 245),
)

private val materialSeedPresets = listOf(
    Rgba(244, 67, 54),   // red
    Rgba(233, 30, 99),   // pink
    Rgba(156, 39, 176),  // purple
    Rgba(63, 81, 181),   // indigo
    Rgba(33, 150, 243),  // blue
    Rgba(0, 150, 136),   // teal
    Rgba(76, 175, 80),   // green
    Rgba(255, 193, 7),   // amber
)

internal val pageColorPresets = listOf(
    Rgba(22, 22, 22), Rgba(13, 13, 13), Rgba(255, 255, 255), Rgba(247, 243, 233), Rgba(232, 232, 232),
)

private val penButtonOptions = listOf("eraser" to "Eraser", "pan" to "Pan", "select" to "Select", "none" to "None")

private val tapGestureOptions = listOf(
    "none" to "None",
    "undo" to "Undo",
    "redo" to "Redo",
    "toggle_pan" to "Toggle pan",
    "toggle_eraser" to "Toggle eraser",
    "toggle_previous" to "Toggle previous tool",
)

/**
 * Redesigned, 3D tactile Preferences page with clean symmetrical cards,
 * physical lighting, streamlined settings, and zero unnecessary clutter.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreferencesPane(
    editor: Editor,
    compact: Boolean,
    sidebarOpen: Boolean,
    onShowSidebar: () -> Unit,
    onBackToHome: () -> Unit,
    onImportCodeTheme: () -> Unit = {},
    onImportFont: () -> Unit = {},
    onOpenStylusCalibration: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val focusManager = LocalFocusManager.current
    var prefs by remember { mutableStateOf(editor.preferences) }
    var isFullLength by remember { mutableStateOf(ToolbarPreferences.getIsFullLength(context)) }

    fun update(p: Preferences) {
        prefs = p
        editor.applyPreferences(p)
    }

    LaunchedEffect(editor.prefsVersion) { prefs = editor.preferences }

    val scrollState = rememberScrollState()
    var canvasTab by remember { mutableStateOf(false) }
    val layout = if (canvasTab) editor.canvasToolbarLayout else editor.toolbarLayout

    var dragItem by remember { mutableStateOf<ToolbarItem?>(null) }
    var dragFinger by remember { mutableStateOf(Offset.Zero) }
    var dragGrab by remember { mutableStateOf(Offset.Zero) }
    var dropTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val chipBounds = remember { mutableMapOf<ToolbarItem, Rect>() }
    val sectionBounds = remember { mutableMapOf<Int, Rect>() }
    val sectionGrabBounds = remember { mutableMapOf<Int, Rect>() }

    var dragSection by remember { mutableStateOf<Int?>(null) }
    var sectionDropTarget by remember { mutableStateOf<Int?>(null) }
    var paneTopLeft by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(Rect.Zero) }

    fun editedLayout(): ToolbarLayout = if (canvasTab) editor.canvasToolbarLayout else editor.toolbarLayout
    fun applyEdited(next: ToolbarLayout) {
        if (canvasTab) editor.applyCanvasToolbarLayout(next) else editor.applyToolbarLayout(next)
    }
    fun retarget() {
        dropTarget = toolbarDropTarget(editedLayout(), dragFinger, chipBounds, sectionBounds)
    }
    fun retargetSection() {
        sectionDropTarget = toolbarSectionDropTarget(editedLayout(), dragFinger, sectionBounds)
    }
    fun showTab(canvas: Boolean) {
        if (canvasTab == canvas) return
        canvasTab = canvas
        chipBounds.clear()
        sectionBounds.clear()
        sectionGrabBounds.clear()
        dragItem = null
        dragSection = null
        dropTarget = null
        sectionDropTarget = null
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            .onGloballyPositioned { paneTopLeft = it.boundsInRoot().topLeft },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 14.dp else 24.dp),
        ) {
            // --- TOP HEADER BAR ---
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (compact) {
                    IconButton3D(
                        icon = PnotesIcons.prev,
                        contentDescription = "Back to home",
                        onClick = onBackToHome,
                        isDark = isDark,
                    )
                    Spacer(Modifier.width(10.dp))
                } else if (!sidebarOpen) {
                    IconButton3D(
                        icon = PnotesIcons.menu,
                        contentDescription = "Show sidebar",
                        onClick = onShowSidebar,
                        isDark = isDark,
                    )
                    Spacer(Modifier.width(10.dp))
                }

                Text(
                    text = "Preferences",
                    color = if (isDark) Color(0xFFF3F5F8) else Color(0xFF131720),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.4).sp,
                )

                Spacer(Modifier.weight(1f))

                // 3D Reset Button
                TactilePillButton(
                    text = "Reset to defaults",
                    isDark = isDark,
                    onClick = { update(Preferences()) },
                )
            }

            Spacer(Modifier.height(8.dp))

            // --- SCROLLABLE SETTINGS CONTENT ---
            Box(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { viewport = it.boundsInRoot() },
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ==========================================
                    // 1. HERO CARD: STYLUS & PRESSURE CALIBRATION
                    // ==========================================
                    Surface(
                        onClick = { onOpenStylusCalibration() },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) Color(0xFF131720) else Color(0xFFFFFFFF),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0x28FFFFFF) else Color(0xFFE4E4E7),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .elevated3D(
                                isDark = isDark,
                                elevation = if (isDark) 2.dp else 4.dp,
                                cornerRadius = 18.dp,
                                isActive = true,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
                                        )
                                        .shadow(4.dp, CircleShape, spotColor = if (isDark) Color(0x40FFFFFF) else Color(0x40000000)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = PnotesIcons.pen,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF09090B) else Color(0xFFFFFFFF),
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        "Stylus & Pressure Calibration",
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B),
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "Live curve visualizer, latency tuning, and palm rejection diagnostics",
                                        fontSize = 12.sp,
                                        color = PnotesTokens.textSecondary,
                                        lineHeight = 16.sp,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x18FFFFFF) else Color(0x0A000000)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PnotesIcons.chevronDown,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .rotate(-90f),
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 2. APPEARANCE & THEME CARD
                    // ==========================================
                    PrefCard3D(
                        title = "Appearance & Theme",
                        isDark = isDark,
                    ) {
                        FieldHeader3D("UI Theme", "Choose the visual atmosphere of the application")
                        SegmentedGroup3D(
                            options = listOf("System", "Dark", "Light", "OLED"),
                            selectedOption = when (prefs.uiAppearance) {
                                "dark" -> "Dark"
                                "light" -> "Light"
                                "oled" -> "OLED"
                                else -> "System"
                            },
                            isDark = isDark,
                            onSelect = {
                                update(prefs.copy(uiAppearance = it.lowercase()))
                            },
                        )

                        Spacer(Modifier.height(14.dp))

                        var localScale by remember(prefs.uiScale) { mutableStateOf(prefs.uiScale) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FieldHeader3D("App Display Scale", "Resize UI elements for small or large tablet screens")
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (kotlin.math.abs(localScale - Preferences.UI_SCALE_DEFAULT) > 0.01f) {
                                    TactilePillButton(
                                        text = "Reset",
                                        isDark = isDark,
                                        onClick = {
                                            localScale = Preferences.UI_SCALE_DEFAULT
                                            update(prefs.copy(uiScale = Preferences.UI_SCALE_DEFAULT))
                                        },
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0x24FFFFFF) else Color(0x12000000))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        "${kotlin.math.round(localScale * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B),
                                    )
                                }
                            }
                        }

                        Slider(
                            value = localScale,
                            onValueChange = {
                                localScale = (kotlin.math.round(it * 20f) / 20f).coerceIn(
                                    Preferences.UI_SCALE_MIN,
                                    Preferences.UI_SCALE_MAX
                                )
                            },
                            onValueChangeFinished = {
                                update(prefs.copy(uiScale = localScale))
                            },
                            valueRange = Preferences.UI_SCALE_MIN..Preferences.UI_SCALE_MAX,
                            steps = 15,
                            colors = PurpleTheme.sliderColors(palette.isDark),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(14.dp))

                        FieldHeader3D("Toolbar Dock Style", "Switch between floating island dock and full-length edge-attached dock")
                        SegmentedGroup3D(
                            options = listOf("Floating Dock", "Stick to Edge"),
                            selectedOption = if (isFullLength) "Stick to Edge" else "Floating Dock",
                            isDark = isDark,
                            onSelect = {
                                val full = it == "Stick to Edge"
                                isFullLength = full
                                ToolbarPreferences.setIsFullLength(context, full)
                            },
                        )

                        Spacer(Modifier.height(14.dp))

                        FieldHeader3D("Color Palette Style", "Classic vibrant accents or dynamic Material You")
                        SegmentedGroup3D(
                            options = listOf("Classic Accent", "Material You"),
                            selectedOption = if (prefs.paletteStyle == "material") "Material You" else "Classic Accent",
                            isDark = isDark,
                            onSelect = {
                                update(prefs.withPaletteStyle(if (it == "Material You") "material" else "classic"))
                            },
                        )

                        Spacer(Modifier.height(14.dp))

                        FieldHeader3D("Accent Color", "Primary highlight color for active tools and buttons")
                        if (prefs.paletteStyle == "material") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                DynamicSeedDot(prefs.materialSeed == null) { update(prefs.copy(materialSeed = null)) }
                                materialSeedPresets.forEach { c ->
                                    ColorDot(c.toComposeColor(), prefs.materialSeed == c) { update(prefs.copy(materialSeed = c)) }
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                accentPresets.forEach { c ->
                                    ColorDot(c.toComposeColor(), prefs.accentColor == c) { update(prefs.copy(accentColor = c)) }
                                }
                                ColorPickerDot(
                                    prefs.accentColor,
                                    custom = prefs.accentColor !in accentPresets,
                                    onPick = { update(prefs.copy(accentColor = it)) },
                                ) { onDismiss, onPick -> AccentColorGridPopup(onDismiss, onPick) }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0F000000))
                        Spacer(Modifier.height(14.dp))

                        PrefSwitchRow3D(
                            title = "Start in fullscreen",
                            subtitle = "Hide Android system status and navigation bars on open",
                            checked = editor.fullscreen,
                            isDark = isDark,
                            onCheckedChange = { editor.setFullscreenPref(it) },
                        )
                    }

                    // ==========================================
                    // 3. STYLUS & INKING CARD
                    // ==========================================
                    PrefCard3D(
                        title = "Stylus & Inking Behavior",
                        isDark = isDark,
                    ) {
                        PrefSwitchRow3D(
                            title = "Draw with finger",
                            subtitle = "When disabled, finger touches pan and scroll while only stylus inks",
                            checked = prefs.fingerDraws,
                            isDark = isDark,
                            onCheckedChange = { update(prefs.copy(fingerDraws = it)) },
                        )

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0F000000))
                        Spacer(Modifier.height(12.dp))

                        PrefSwitchRow3D(
                            title = "Hold still to snap shapes",
                            subtitle = "Rest the pen for a moment to convert hand-drawn lines, circles, and polygons",
                            checked = prefs.detectShapes,
                            isDark = isDark,
                            onCheckedChange = { update(prefs.copy(detectShapes = it)) },
                        )

                        Spacer(Modifier.height(14.dp))

                        FieldHeader3D("Stylus Side Button (Hold)", "Tool activated when pressing the barrel button")
                        SegmentedGroup3D(
                            options = penButtonOptions.map { it.second },
                            selectedOption = penButtonOptions.firstOrNull { it.first == prefs.penButtonTool }?.second ?: "Eraser",
                            isDark = isDark,
                            onSelect = { sel ->
                                val id = penButtonOptions.firstOrNull { it.second == sel }?.first ?: "eraser"
                                update(prefs.copy(penButtonTool = id))
                            },
                        )

                        if (prefs.penButtonTool == "eraser" || prefs.penButtonTool == "pan") {
                            Spacer(Modifier.height(12.dp))
                            PrefSwitchRow3D(
                                title = "Activate during hover",
                                subtitle = "Trigger side-button tool without touching the screen surface",
                                checked = prefs.penButtonHover,
                                isDark = isDark,
                                onCheckedChange = { update(prefs.copy(penButtonHover = it)) },
                            )
                        }
                    }

                    // ==========================================
                    // 4. GESTURES & SHORTCUTS CARD
                    // ==========================================
                    PrefCard3D(
                        title = "Gestures & Tap Shortcuts",
                        isDark = isDark,
                    ) {
                        PrefDropdownRow3D(
                            title = "Two-finger tap",
                            subtitle = "Quick tap with two fingers on canvas",
                            options = tapGestureOptions,
                            selectedId = prefs.twoFingerTap,
                            isDark = isDark,
                            onSelect = { update(prefs.copy(twoFingerTap = it)) },
                        )

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0F000000))
                        Spacer(Modifier.height(12.dp))

                        PrefDropdownRow3D(
                            title = "Three-finger tap",
                            subtitle = "Quick tap with three fingers on canvas",
                            options = tapGestureOptions,
                            selectedId = prefs.threeFingerTap,
                            isDark = isDark,
                            onSelect = { update(prefs.copy(threeFingerTap = it)) },
                        )

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0F000000))
                        Spacer(Modifier.height(12.dp))

                        PrefDropdownRow3D(
                            title = "Stylus double-tap",
                            subtitle = "Double tap on stylus barrel (supported pens)",
                            options = tapGestureOptions,
                            selectedId = prefs.stylusDoubleTap,
                            isDark = isDark,
                            onSelect = { update(prefs.copy(stylusDoubleTap = it)) },
                        )

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0F000000))
                        Spacer(Modifier.height(12.dp))

                        PrefDropdownRow3D(
                            title = "Stylus side button (Single tap)",
                            subtitle = "Quick click on side button",
                            options = tapGestureOptions,
                            selectedId = prefs.stylusButtonTap,
                            isDark = isDark,
                            onSelect = { update(prefs.copy(stylusButtonTap = it)) },
                        )
                    }

                    // ==========================================
                    // 5. PAGE & DOCUMENT DEFAULTS CARD
                    // ==========================================
                    PrefCard3D(
                        title = "Page & Document Defaults",
                        isDark = isDark,
                    ) {
                        PrefDropdownRow3D(
                            title = "Default Page Size",
                            subtitle = "Initial dimensions for newly created notebooks",
                            options = PageSize.entries.map { it.name to it.displayName },
                            selectedId = prefs.defaultPageSize.name,
                            isDark = isDark,
                            onSelect = { name ->
                                val sz = PageSize.entries.firstOrNull { it.name == name } ?: PageSize.A4
                                update(prefs.copy(defaultPageSize = sz))
                            },
                        )

                        if (prefs.defaultPageSize != PageSize.CUSTOM) {
                            Spacer(Modifier.height(14.dp))
                            FieldHeader3D("Default Orientation", "Canvas aspect layout")
                            SegmentedGroup3D(
                                options = listOf("Portrait", "Landscape"),
                                selectedOption = if (prefs.defaultPageOrientation == Orientation.LANDSCAPE) "Landscape" else "Portrait",
                                isDark = isDark,
                                onSelect = {
                                    update(prefs.copy(defaultPageOrientation = if (it == "Landscape") Orientation.LANDSCAPE else Orientation.PORTRAIT))
                                },
                            )
                        } else {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MillimetreField("Width (mm)", prefs.customPageWidthMm) {
                                    update(prefs.copy(customPageWidthMm = it))
                                }
                                MillimetreField("Height (mm)", prefs.customPageHeightMm) {
                                    update(prefs.copy(customPageHeightMm = it))
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0F000000))
                        Spacer(Modifier.height(14.dp))

                        FieldHeader3D("Default Page Color", "Background shade for paper surface")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            pageColorPresets.forEach { c ->
                                ColorDot(c.toComposeColor(), prefs.pageColor == c) { update(prefs.copy(pageColor = c)) }
                            }
                            ColorPickerDot(
                                prefs.pageColor,
                                custom = prefs.pageColor != null && prefs.pageColor !in pageColorPresets,
                                onPick = { update(prefs.copy(pageColor = it)) },
                                dismissOnPick = false,
                            ) { onDismiss, onPick -> PageColorGridPopup(prefs.pageColor, onDismiss, onPick) }
                        }

                        Spacer(Modifier.height(12.dp))

                        PrefSwitchRow3D(
                            title = "Page color follows theme",
                            subtitle = "Automatically match paper tone with dark or light mode",
                            checked = prefs.pageColor == null,
                            isDark = isDark,
                            onCheckedChange = {
                                update(prefs.copy(pageColor = if (it) null else pageColorPresets.first()))
                            },
                        )

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0F000000))
                        Spacer(Modifier.height(12.dp))

                        PrefSwitchRow3D(
                            title = "Hide page borders",
                            subtitle = "Remove subtle bounding hairline around pages",
                            checked = prefs.hidePageBorders,
                            isDark = isDark,
                            onCheckedChange = { update(prefs.copy(hidePageBorders = it)) },
                        )
                    }

                    // ==========================================
                    // 6. CUSTOM FONTS CARD
                    // ==========================================
                    PrefCard3D(
                        title = "Typography & Fonts",
                        isDark = isDark,
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Custom Typefaces",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFF3F5F8) else Color(0xFF18181B),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Import .ttf or .otf font files for text boxes and labels",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF8B98A8) else Color(0xFF71717A),
                                )
                            }
                            TactilePillButton(
                                text = "Import Font…",
                                isDark = isDark,
                                onClick = { onImportFont() },
                            )
                        }

                        if (editor.customFonts.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (font in editor.customFonts) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            font.label,
                                            color = palette.text.toComposeColor(),
                                            fontSize = 13.5.sp,
                                            style = TextStyle(fontFamily = font.face.toComposeFamily()),
                                        )
                                        if (font.mono) {
                                            Text("  mono", color = palette.accent.toComposeColor(), fontSize = 11.sp)
                                        }
                                        Spacer(Modifier.weight(1f))
                                        TextButton(onClick = { editor.removeCustomFont(font.face) }) {
                                            Text("Remove", fontSize = 12.sp, color = Color(0xFFEF4444))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 7. TOOLBAR CUSTOMIZATION CARD
                    // ==========================================
                    PrefCard3D(
                        title = "Toolbar Customization",
                        isDark = isDark,
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FieldHeader3D("Colors on Toolbar", "Quick-access color swatches")
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0x24FFFFFF) else Color(0x12000000))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "${editor.toolbarColorCount} colors",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B),
                                )
                            }
                        }

                        Slider(
                            value = editor.toolbarColorCount.toFloat(),
                            onValueChange = { editor.applyToolbarColorCount(Math.round(it)) },
                            valueRange = 1f..7f,
                            steps = 5,
                            colors = PurpleTheme.sliderColors(palette.isDark),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SegmentedGroup3D(
                                options = listOf("Notes Toolbar", "Canvas Toolbar"),
                                selectedOption = if (canvasTab) "Canvas Toolbar" else "Notes Toolbar",
                                isDark = isDark,
                                onSelect = { showTab(it == "Canvas Toolbar") },
                                modifier = Modifier.weight(1f),
                            )

                            TactilePillButton(
                                text = "Reset",
                                isDark = isDark,
                                onClick = {
                                    applyEdited(if (canvasTab) ToolbarLayout.CANVAS_DEFAULT else ToolbarLayout.DEFAULT)
                                },
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Tap tools to show/hide. Long-press chips or section headers to rearrange.",
                            color = if (isDark) Color(0xFF8B98A8) else Color(0xFF71717A),
                            fontSize = 12.sp,
                        )

                        Spacer(Modifier.height(10.dp))

                        ToolbarCustomizerBody(
                            layout = layout,
                            dragItem = dragItem,
                            dropTarget = dropTarget,
                            dragSection = dragSection,
                            sectionDropTarget = sectionDropTarget,
                            chipBounds = chipBounds,
                            sectionBounds = sectionBounds,
                            sectionGrabBounds = sectionGrabBounds,
                            onToggle = { s, i -> applyEdited(editedLayout().toggleVisible(s, i)) },
                            onDelete = { s -> applyEdited(editedLayout().removeSection(s)) },
                            onAdd = { applyEdited(editedLayout().addSection()) },
                            onDragStart = { item, local ->
                                dragItem = item
                                dragGrab = local
                                dragFinger = (chipBounds[item]?.topLeft ?: Offset.Zero) + local
                                retarget()
                            },
                            onDrag = { delta -> dragFinger += delta; retarget() },
                            onDrop = {
                                val target = dropTarget
                                val moving = dragItem
                                if (target != null && moving != null) {
                                    val cur = editedLayout()
                                    val fSec = cur.sections.indexOfFirst { s -> s.entries.any { it.item == moving } }
                                    val fIdx = if (fSec >= 0) cur.sections[fSec].entries.indexOfFirst { it.item == moving } else -1
                                    if (fSec >= 0 && fIdx >= 0) {
                                        applyEdited(cur.moveItem(fSec, fIdx, target.first, target.second))
                                    }
                                }
                                dragItem = null
                                dropTarget = null
                            },
                            onCancel = {
                                dragItem = null
                                dropTarget = null
                            },
                            onSectionDragStart = { sec, local ->
                                dragSection = sec
                                val grab = sectionGrabBounds[sec]?.topLeft ?: sectionBounds[sec]?.topLeft ?: Offset.Zero
                                val card = sectionBounds[sec]?.topLeft ?: Offset.Zero
                                dragFinger = grab + local
                                dragGrab = dragFinger - card
                                retargetSection()
                            },
                            onSectionDrag = { delta -> dragFinger += delta; retargetSection() },
                            onSectionDrop = {
                                val from = dragSection
                                val insertAt = sectionDropTarget
                                if (from != null && insertAt != null) {
                                    val cur = editedLayout()
                                    val to = (if (insertAt > from) insertAt - 1 else insertAt).coerceIn(0, cur.sections.lastIndex)
                                    if (to != from) applyEdited(cur.moveSection(from, to))
                                }
                                dragSection = null
                                sectionDropTarget = null
                            },
                            onSectionCancel = {
                                dragSection = null
                                sectionDropTarget = null
                            },
                        )
                    }
                }
            }
        }

        // Floating drag ghost for reordering toolbar items
        val gx = (dragFinger.x - dragGrab.x - paneTopLeft.x).toInt()
        val gy = (dragFinger.y - dragGrab.y - paneTopLeft.y).toInt()
        val ghostChip = dragItem
        val ghostSecIdx = dragSection
        if (ghostChip != null) {
            Box(Modifier.offset { IntOffset(gx, gy) }) { ToolbarDragGhost(ghostChip) }
        } else if (ghostSecIdx != null) {
            val sectionG = (if (canvasTab) editor.canvasToolbarLayout else editor.toolbarLayout)
                .sections.getOrNull(ghostSecIdx)
            val widthG = sectionBounds[ghostSecIdx]?.width
            if (sectionG != null && widthG != null) {
                Box(Modifier.offset { IntOffset(gx, gy) }) { SectionCardGhost(sectionG, widthG) }
            }
        }
    }

    LaunchedEffect(dragItem != null || dragSection != null) {
        while (dragItem != null || dragSection != null) {
            val band = 90f
            val y = dragFinger.y
            val delta = when {
                y < viewport.top + band -> -((viewport.top + band - y) / band) * 14f
                y > viewport.bottom - band -> ((y - (viewport.bottom - band)) / band) * 14f
                else -> 0f
            }
            if (delta != 0f) {
                scrollState.scrollBy(delta)
                if (dragItem != null) retarget() else retargetSection()
            }
            delay(16L)
        }
    }
}

// =========================================================
// 🌟 3D REUSABLE UI DESIGN SYSTEM COMPONENTS
// =========================================================

/**
 * Modern 3D Elevated Card Container with physical lighting and bevels.
 */
@Composable
private fun PrefCard3D(
    title: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardBg = if (isDark) Color(0xFF131720) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0x1CFFFFFF) else Color(0xFFE5E9F0)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .elevated3D(
                isDark = isDark,
                elevation = 4.dp,
                cornerRadius = 18.dp,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Text(
                text = title,
                color = if (isDark) Color(0xFFF3F5F8) else Color(0xFF09090B),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

/**
 * 3D Section Field Header with title and subtle subtitle.
 */
@Composable
private fun FieldHeader3D(title: String, subtitle: String? = null) {
    Column {
        Text(
            text = title,
            color = if (LocalPalette.current.isDark) Color(0xFFE4E4E7) else Color(0xFF18181B),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = if (LocalPalette.current.isDark) Color(0xFF828E9E) else Color(0xFF71717A),
                fontSize = 11.5.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * 3D Tactile Switch Row with Title, Subtitle, and Smooth Switch.
 */
@Composable
private fun PrefSwitchRow3D(
    title: String,
    subtitle: String,
    checked: Boolean,
    isDark: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF09090B),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = if (isDark) Color(0xFF828E9E) else Color(0xFF71717A),
                lineHeight = 15.sp,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = PurpleTheme.switchColors(isDark),
            modifier = Modifier.scale(0.88f),
        )
    }
}

/**
 * 3D Tactile Dropdown Row with Title, Subtitle, and Dropdown Trigger Chip.
 */
@Composable
private fun PrefDropdownRow3D(
    title: String,
    subtitle: String,
    options: List<Pair<String, String>>,
    selectedId: String,
    isDark: Boolean,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == selectedId }?.second ?: options.firstOrNull()?.second ?: "None"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF09090B),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = if (isDark) Color(0xFF828E9E) else Color(0xFF71717A),
            )
        }

        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Color(0xFF1B2230) else Color(0xFFF4F4F5),
                border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else Color(0xFFD4D4D8)),
                modifier = Modifier.elevated3D(isDark = isDark, elevation = 2.dp, cornerRadius = 10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B),
                    )
                    Icon(
                        imageVector = PnotesIcons.chevronDown,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (id, lbl) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                lbl,
                                color = if (id == selectedId) {
                                    if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
                                } else {
                                    if (isDark) Color(0xFFE4E4E7) else Color(0xFF18181B)
                                },
                                fontWeight = if (id == selectedId) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onSelect(id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

/**
 * 3D Tactile Segmented Choice Group (iOS/Material style).
 */
@Composable
private fun SegmentedGroup3D(
    options: List<String>,
    selectedOption: String,
    isDark: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackBg = if (isDark) Color(0xFF0C1017) else Color(0xFFEEF2F6)
    val trackBorder = if (isDark) Color(0x1FFFFFFF) else Color(0xFFE4E4E7)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = trackBg,
        border = BorderStroke(1.dp, trackBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val selected = option == selectedOption
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh),
                    label = "chip_scale",
                )

                Surface(
                    onClick = { onSelect(option) },
                    interactionSource = interactionSource,
                    shape = RoundedCornerShape(9.dp),
                    color = if (selected) {
                        if (isDark) Color(0xFF1F2937) else Color.White
                    } else {
                        Color.Transparent
                    },
                    border = if (selected) {
                        BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF09090B).copy(alpha = 0.3f),
                        )
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .then(
                            if (selected) {
                                Modifier.elevated3D(isDark = isDark, elevation = 3.dp, cornerRadius = 9.dp)
                            } else Modifier
                        ),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option,
                            fontSize = 12.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) {
                                if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
                            } else {
                                if (isDark) Color(0xFF828E9E) else Color(0xFF71717A)
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3D Tactile Action Button for Headers and secondary actions.
 */
@Composable
private fun TactilePillButton(
    text: String,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "pill_scale",
    )

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0xFF1A2230) else Color(0xFFF4F4F5),
        border = BorderStroke(1.dp, if (isDark) Color(0x2BFFFFFF) else Color(0xFFD4D4D8)),
        modifier = modifier
            .scale(scale)
            .elevated3D(isDark = isDark, elevation = if (isPressed) 1.dp else 3.5.dp, cornerRadius = 10.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF27272A),
            )
        }
    }
}

/**
 * 3D Circular Icon Button for Back and Sidebar triggers.
 */
@Composable
private fun IconButton3D(
    icon: ImageVector,
    contentDescription: String,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "icon_btn_scale",
    )

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF161C26) else Color(0xFFFAFAFA),
        border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else Color(0xFFE4E4E7)),
        modifier = Modifier
            .size(38.dp)
            .scale(scale)
            .elevated3D(isDark = isDark, elevation = 2.dp, cornerRadius = 12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isDark) Color(0xFFF3F5F8) else Color(0xFF18181B),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// =========================================================
// COLOR PICKERS & DOT HELPERS
// =========================================================

@Composable
internal fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    Box(
        Modifier
            .size(32.dp)
            .then(
                if (selected) {
                    Modifier.border(2.dp, if (isDark) Color.White else Color(0xFF09090B), CircleShape)
                } else Modifier
            )
            .padding(3.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x33000000), CircleShape)
            .clickable(onClick = onClick),
    )
}

private val materialSweepBrush = Brush.sweepGradient(
    listOf(
        Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFF44336),
        Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF2196F3),
    ),
)

@Composable
private fun DynamicSeedDot(selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    Box(
        Modifier
            .size(32.dp)
            .then(
                if (selected) {
                    Modifier.border(2.dp, if (isDark) Color.White else Color(0xFF09090B), CircleShape)
                } else Modifier
            )
            .padding(3.dp)
            .clip(CircleShape)
            .background(materialSweepBrush)
            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x33000000), CircleShape)
            .clickable(onClick = onClick),
    )
}

private val spectrumBrush = Brush.sweepGradient(
    listOf(
        Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
        Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000),
    ),
)

@Composable
internal fun ColorPickerDot(
    current: Rgba?,
    custom: Boolean,
    onPick: (Rgba) -> Unit,
    dismissOnPick: Boolean = true,
    grid: @Composable (onDismiss: () -> Unit, onPick: (Rgba) -> Unit) -> Unit,
) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(32.dp)
                .then(
                    if (custom) {
                        Modifier.border(2.dp, if (isDark) Color.White else Color(0xFF09090B), CircleShape)
                    } else Modifier
                )
                .padding(3.dp)
                .clip(CircleShape)
                .then(
                    if (custom && current != null) Modifier.background(current.toComposeColor())
                    else Modifier.background(spectrumBrush)
                )
                .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x33000000), CircleShape)
                .clickable { open = true },
        )
        if (open) grid({ open = false }, { onPick(it); if (dismissOnPick) open = false })
    }
}

@Composable
private fun Swatch(c: Rgba, onPick: (Rgba) -> Unit) {
    Box(
        Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(c.toComposeColor())
            .border(0.5.dp, LocalPalette.current.border.toComposeColor(), RoundedCornerShape(3.dp))
            .clickable { onPick(c) },
    )
}

@Composable
private fun AccentColorGridPopup(onDismiss: () -> Unit, onPick: (Rgba) -> Unit) {
    val hues = (0 until 12).map { it * 360.0 / 12.0 }
    val shades = listOf(1.0 to 1.0, 1.0 to 0.82, 0.78 to 1.0)
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            shades.forEach { (s, v) ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    hues.forEach { h -> Swatch(ColorMath.hsvToRgb(h, s, v), onPick) }
                }
            }
        }
    }
}

@Composable
internal fun PageColorGridPopup(initial: Rgba?, onDismiss: () -> Unit, onPick: (Rgba) -> Unit) {
    ColorPickerPopup(initial = initial, recents = emptyList(), onDismiss = onDismiss, onPick = onPick)
}

@Composable
private fun MillimetreField(label: String, value: Double, onChange: (Double) -> Unit) {
    var text by remember { mutableStateOf(formatMm(value)) }
    LaunchedEffect(value) { if (text.trim().toDoubleOrNull() != value) text = formatMm(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            raw.trim().toDoubleOrNull()?.let {
                if (it in Preferences.CUSTOM_PAGE_MIN_MM..Preferences.CUSTOM_PAGE_MAX_MM) onChange(it)
            }
        },
        singleLine = true,
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        modifier = Modifier.width(136.dp),
    )
}

private fun formatMm(v: Double): String =
    if (v == Math.floor(v) && !v.isInfinite()) v.toInt().toString() else v.toString()

/**
 * The colour-code picker shown inside a note/folder's overflow menu: a None row to clear the colour,
 * then the picker's full matrix (pale tints through near-black plus the greyscale row). [onPick] is
 * called with null for None. Rendered directly inside the menu's own dropdown column.
 */
@Composable
internal fun ColorCodeMenuContent(onPick: (Rgba?) -> Unit) {
    val palette = LocalPalette.current
    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.clip(RoundedCornerShape(4.dp)).clickable { onPick(null) }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(20.dp).clip(RoundedCornerShape(2.dp))
                    .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(2.dp)),
            )
            Text("None", color = palette.text.toComposeColor(), fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
        }
        FullSwatchGrid { onPick(it) }
    }
}
