package com.pnotes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.PurpleTheme
import com.pnotes.ui.theme.NormalModeSolidColor
import com.pnotes.ui.theme.NormalModeDarkPill
import com.pnotes.ui.theme.NormalModePillBorder
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import com.pnotes.ui.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.painterResource
import com.pnotes.R
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pnotes.core.model.Page
import com.pnotes.core.model.PagePattern
import com.pnotes.core.model.PageStyle
import com.pnotes.core.model.Rgba
import com.pnotes.core.tools.EraseMode
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolConversions
import com.pnotes.core.util.DocumentKind
import com.pnotes.tabs.NoteTab
import com.pnotes.tabs.TabManager
import com.pnotes.ui.icons.PnotesDynamicIcons
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.LocalMotion
import com.pnotes.ui.theme.toComposeColor
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

// === MONOCHROME DESIGN TOKENS (LIGHT: DEEP OBSIDIAN #09090B | DARK: PURE WHITE #FFFFFF) ===
private fun monochromeAccent(isDark: Boolean): Color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
private fun monochromeAccentFg(isDark: Boolean): Color = if (isDark) Color(0xFF09090B) else Color(0xFFFFFFFF)
private fun monochromePillBg(isDark: Boolean): Color = if (isDark) Color(0x2EFFFFFF) else Color(0xFFF4F4F5)
private fun monochromePillBorder(isDark: Boolean): Color = if (isDark) Color(0x66FFFFFF) else Color(0xFF09090B)

// Fallback constants for any remaining static accesses
private val PrimaryPurple = Color(0xFF09090B)
private val SecondaryIndigo = Color(0xFF09090B)
private val LightPurpleBg = Color(0xFFF4F4F5)
private val PurpleBorder = Color(0xFFE4E4E7)
private val SlateDark = Color(0xFF09090B)
private val SlateText = Color(0xFF09090B)
private val SlateBody = Color(0xFF27272A)
private val SlateSub = Color(0xFF52525B)
private val SlateMuted = Color(0xFF71717A)
private val GlassBorder = Color(0xFFE4E4E7)
private val SavedGreen = Color(0xFF09090B)
private val SavedGreenBg = Color(0xFFF4F4F5)
private val SavedGreenBorder = Color(0xFFE4E4E7)
private val SavedGreenText = Color(0xFF09090B)

private fun activePillGradient(isDark: Boolean): Brush = if (isDark) {
    Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE4E4E7)))
} else {
    Brush.linearGradient(listOf(Color(0xFF09090B), Color(0xFF18181B)))
}

private val ActivePillGradient = Brush.linearGradient(
    listOf(Color(0xFF09090B), Color(0xFF18181B))
)

fun activeGlassSurface(isDark: Boolean): Brush = if (isDark) {
    SolidColor(Color(0xFF27272A))
} else {
    SolidColor(Color(0xFF09090B))
}

fun activeGlassLuminousBorder(isDark: Boolean): Brush = if (isDark) {
    SolidColor(Color(0xFF3F3F46))
} else {
    SolidColor(Color(0xFF18181B))
}

val ActiveGlassSpecularGlare = SolidColor(Color.Transparent)

// =========================================================================
// === SOLID CRISP DESIGN SYSTEM (100% OPAQUE, NO BLUR, NO GLASS) ===
// =========================================================================

// --- LIGHT SOLID TOKENS ---
val TrueGlassSurface = SolidColor(Color(0xFFFFFFFF))
val TrueGlassSpecularGlare = SolidColor(Color.Transparent)
val TrueGlassLuminousBorder = SolidColor(Color(0xFFE4E4E7))
val TrueGlassInnerPill = SolidColor(Color(0xFFF4F4F5))

// --- PAGE 1 DESIGN SYSTEM TOKENS (prototype/New UI/pnotes-new-ui.figcraft) ---
val PnotesDarkBg = Color(0xFF2C2C2C)
val PnotesDarkSidebar = Color(0xFF242424)
val PnotesDarkCard = Color(0xFF2C2C2C)
val PnotesDarkSurface2 = Color(0xFF222222)
val PnotesDarkElevated = Color(0xFF383838)
val PnotesDarkBorder = Color(0xFF3F3F46)
val PnotesDarkBorderSubtle = Color(0xFF2E2E32)
val PnotesDarkText1 = Color(0xFFF5F7FA)
val PnotesDarkText2 = Color(0xFFA1A1AA)
val PnotesDarkText3 = Color(0xFF71717A)
val PnotesPrimary = Color(0xFF007AFF)
val PnotesDanger = Color(0xFFE81E21)
val PnotesDarkDanger = PnotesDanger
val PnotesHighlight = Color(0xFFFFB200)
val PnotesDarkTrack = Color(0xFF3E3E3E)

val PnotesLightBg = Color(0xFFF7F9FC)
val PnotesLightCard = Color(0xFFFFFFFF)
val PnotesLightSurface2 = Color(0xFFF3F6FA)
val PnotesLightElevated = Color(0xFFFFFFFF)
val PnotesLightBorder = Color(0xFFE1E6EF)
val PnotesLightText1 = Color(0xFF151923)
val PnotesLightText2 = Color(0xFF525D70)
val PnotesLightText3 = Color(0xFF8993A3)
val PnotesLightTrack = Color(0xFFDCE2EB)

// --- DARK SOLID TOKENS ---
val TrueGlassDarkSurface = SolidColor(Color(0xFF202024))
val TrueGlassDarkSpecularGlare = SolidColor(Color.Transparent)
val TrueGlassDarkLuminousBorder = SolidColor(Color(0xFF38383E))
val TrueGlassDarkInnerPill = SolidColor(Color(0xFF27272A))

/** Returns true if the active note palette / theme is dark. */
@Composable
fun isGlassDark(): Boolean =
    LocalPalette.current.isDark

fun Modifier.glassmorphic(
    shape: Shape,
    elevation: androidx.compose.ui.unit.Dp = 8.dp,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    borderBrush: Brush? = null,
    isDark: Boolean = false,
    spotColor: Color? = null,
    ambientColor: Color? = null,
): Modifier {
    val actualBorder = borderBrush ?: if (isDark) {
        SolidColor(Color(0xFF38383E))
    } else {
        SolidColor(Color(0xFFE2E8F0))
    }

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
        )
        .clip(shape)
        .background(
            if (isDark) Color(0xFF202024) else Color(0xFFFFFFFF)
        )
        .border(width = borderWidth, brush = actualBorder, shape = shape)
}

/**
 * Ultra-responsive tactile bouncy feedback modifier for buttons, icons, and interactive pills.
 *
 * Implements Apple's WWDC fluid interface principle:
 * - Instant response on pointer-down with high-stiffness tactile spring
 * - Bouncy spring recovery on release for authentic physical button feel
 */
@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1.0f,
        animationSpec = if (isPressed) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 4200f)
        } else {
            spring(dampingRatio = 0.74f, stiffness = 1900f)
        },
        label = "bouncyClickScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/** Docking edge for the adaptive quick tool rail. */
enum class RailDockEdge {
    LEFT, TOP, RIGHT, BOTTOM
}

/**
 * 1. FLOATING GLASS TOP BAR (Header 1)
 * Blueprint: s(fill, 64) | pad(10, 20) | f[rgba(255,255,255,0.85), blur(24)]
 * st[1.dp bottom #FFFFFF] | shadow(#0F172A, 4%, y=3, blur=12)
 */
@Composable
fun PnotesTopBar(
    editor: Editor,
    onOpenBackstage: () -> Unit,
    onShare: () -> Unit = {},
    onOpenMenu: (() -> Unit)? = null,
    onTogglePageStyles: () -> Unit,
    onTogglePageAngle: () -> Unit = {},
    onToggleStrokeWidthSlider: () -> Unit,
    onToggleEraserSettings: () -> Unit,
    onTogglePageManager: () -> Unit,
    onOpenMargins: () -> Unit = {},
    onOpenViewOptions: () -> Unit = {},
    onOpenPageJump: () -> Unit = {},
    onTogglePresentation: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    onToggleFullLengthToolbar: () -> Unit = {},
    isFullLengthToolbar: Boolean = false,
    onToggleHeaderShrunk: () -> Unit = { editor.toggleHeaderShrunk() },
    onInsertImage: () -> Unit = {},
    isPageStylesOpen: Boolean = false,
    isPageAngleOpen: Boolean = false,
    onApplyPageStyles: () -> Unit = {},
    isTabBarVisible: Boolean = editor.tabManager.tabs.size > 1 && editor.tabBarVisible,
    onToggleTabBarVisible: () -> Unit = { editor.toggleTabBarVisible() },
    onNewTab: () -> Unit = { editor.newNote() },
    onNewCanvasTab: () -> Unit = { editor.newCanvas() },
    onImportPdf: () -> Unit = {},
    onOpenBrowseDrawer: () -> Unit = {},
    tabContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var renaming by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var cameraFlash by remember { mutableStateOf(false) }
    var shareFlash by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currentTool = editor.tool
    val activeInkRgba = editor.controller.inkColor
    val isArea = if (currentTool == Tool.ERASER) {
        editor.toolConfig(Tool.ERASER).eraseMode == EraseMode.AREA
    } else false

    val patName = when (editor.currentPageStyle.pattern) {
        PagePattern.DOTS -> "Dotted Grid"
        PagePattern.LINES -> "Ruled Lines"
        PagePattern.GRID -> "Square Grid"
        PagePattern.CORNELL -> "Cornell Notes"
        else -> "Blank Page"
    }

    PnotesTopBarContent(
        title = editor.title.ifBlank { "Architecture Masterplan & Sketches 2025" },
        currentTool = currentTool,
        isWandActive = editor.wandEnabled,
        isPresentationActive = editor.presentationRunning,
        isFullscreenActive = editor.fullscreen,
        isHeaderShrunk = editor.headerShrunk,
        activeToolWidth = editor.activeToolWidth,
        activeInkColor = activeInkRgba.toComposeColor(),
        pageIndex = editor.pageIndex,
        pageCount = editor.pageCount,
        sidebarActive = editor.sidebarVisible,
        isPageStylesOpen = isPageStylesOpen,
        isPageAngleOpen = isPageAngleOpen,
        pagePatternName = patName,
        isAreaEraser = isArea,
        hasSelection = editor.hasSelection,
        zoomLocked = editor.zoomLocked,
        isReadingMode = editor.isReadingMode,
        onToggleReadingMode = { editor.toggleReadingMode() },
        onOpenBackstage = onOpenBackstage,
        onPrevPage = { editor.prevPage() },
        onNextPage = { editor.nextPage() },
        onTogglePageManager = onTogglePageManager,
        onToggleZoomLock = { editor.toggleZoomLock() },
        onTogglePageAngle = onTogglePageAngle,
        onToggleWand = { editor.toggleWand() },
        onTogglePresentation = onTogglePresentation,
        onToggleFullscreen = onToggleFullscreen,
        onToggleHeaderShrunk = onToggleHeaderShrunk,
        onOpenMenu = onOpenMenu ?: { menuExpanded = true },
        onToggleStrokeWidthSlider = onToggleStrokeWidthSlider,
        onToggleEraserSettings = onToggleEraserSettings,
        onTogglePageStyles = onTogglePageStyles,
        onApplyPageStyles = onApplyPageStyles,
        onClearPage = { editor.erasePage(editor.pageIndex) },
        onRename = { renaming = true },
        onShare = onShare,
        isShareActive = shareFlash,
        onToggleShare = {
            coroutineScope.launch {
                shareFlash = true
                onShare()
                delay(1200)
                shareFlash = false
            }
        },
        isScreenshotActive = cameraFlash || editor.tool == Tool.SCREENSHOT,
        onToggleScreenshot = {
            coroutineScope.launch {
                cameraFlash = true
                editor.captureCurrentPageScreenshot()
                delay(1200)
                cameraFlash = false
            }
        },
        tabManager = editor.tabManager,
        onCloseTab = { tab ->
            editor.closeTabDirectly(tab.id)
        },
        isTabBarVisible = isTabBarVisible,
        onToggleTabBarVisible = onToggleTabBarVisible,
        onNewTab = onNewTab,
        onNewCanvasTab = onNewCanvasTab,
        onImportPdf = onImportPdf,
        onOpenBrowseDrawer = onOpenBrowseDrawer,
        tabContent = tabContent,
        isFullLengthToolbar = isFullLengthToolbar,
        menuContent = {
            PnotesDocumentMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                onOpenPageStyles = onTogglePageStyles,
                onOpenMargins = onOpenMargins,
                onOpenPageAngle = onTogglePageAngle,
                onOpenViewOptions = onOpenViewOptions,
                onOpenPageJump = onOpenPageJump,
                onTogglePresentation = onTogglePresentation,
                isPresentationActive = editor.presentationRunning,
                onToggleFullscreen = onToggleHeaderShrunk,
                isFullscreenActive = editor.fullscreen || editor.headerShrunk,
                onToggleZoomLock = { editor.toggleZoomLock() },
                isZoomLocked = editor.zoomLocked,
                onToggleFullLengthToolbar = onToggleFullLengthToolbar,
                isFullLengthToolbar = isFullLengthToolbar,
                onShareCurrentPage = onShare,
                onExportPdf = onShare,
                onDuplicatePage = {
                    editor.insertPageAfter(editor.pageIndex)
                },
                onToggleTheme = { dark ->
                    editor.applyPreferences(
                        editor.preferences.copy(uiAppearance = if (dark) "dark" else "light")
                    )
                },
                pageIndex = editor.pageIndex,
                pageCount = editor.pageCount,
                currentPageAngle = editor.pageTiltAngle,
            )
        },
        modifier = modifier,
    )

    if (renaming) {
        RenameDialog(
            initial = editor.title,
            onConfirm = { name ->
                renaming = false
                if (!editor.renameCurrentDocument(name)) editor.message = "Couldn’t rename the note."
            },
            onDismiss = { renaming = false },
        )
    }
}

/**
 * Pure, Preview-compatible version of PnotesTopBar.
 * Has ZERO dependency on Android runtime or Canvas / Editor internals.
 * Can be previewed directly in Android Studio's Preview tab!
 */
@Composable
fun PnotesTopBarContent(
    title: String,
    currentTool: Tool = Tool.PEN,
    isWandActive: Boolean = false,
    isPresentationActive: Boolean = false,
    isFullscreenActive: Boolean = false,
    isHeaderShrunk: Boolean = false,
    activeToolWidth: Double = 8.0,
    activeInkColor: Color = Color(0xFF38BDF8),
    pageIndex: Int = 1,
    pageCount: Int = 3,
    sidebarActive: Boolean = false,
    isPageStylesOpen: Boolean = false,
    isPageAngleOpen: Boolean = false,
    pagePatternName: String = "Blank Page",
    isAreaEraser: Boolean = false,
    hasSelection: Boolean = false,
    zoomLocked: Boolean = false,
    isReadingMode: Boolean = false,
    onToggleReadingMode: () -> Unit = {},
    onOpenBackstage: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onTogglePageManager: () -> Unit = {},
    onToggleZoomLock: () -> Unit = {},
    onTogglePageAngle: () -> Unit = {},
    onToggleWand: () -> Unit = {},
    onTogglePresentation: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    onToggleHeaderShrunk: () -> Unit = onToggleFullscreen,
    onOpenMenu: () -> Unit = {},
    onToggleStrokeWidthSlider: () -> Unit = {},
    onToggleEraserSettings: () -> Unit = {},
    onTogglePageStyles: () -> Unit = {},
    onApplyPageStyles: () -> Unit = {},
    onClearPage: () -> Unit = {},
    onRename: () -> Unit = {},
    onShare: () -> Unit = {},
    isShareActive: Boolean = false,
    onToggleShare: () -> Unit = onShare,
    isScreenshotActive: Boolean = false,
    onToggleScreenshot: () -> Unit = {},
    tabManager: TabManager? = null,
    onCloseTab: (NoteTab) -> Unit = {},
    isTabBarVisible: Boolean = true,
    onToggleTabBarVisible: () -> Unit = {},
    onNewTab: () -> Unit = {},
    onNewCanvasTab: () -> Unit = {},
    onImportPdf: () -> Unit = {},
    onOpenBrowseDrawer: () -> Unit = {},
    tabContent: (@Composable () -> Unit)? = null,
    menuContent: (@Composable () -> Unit)? = null,
    isFullLengthToolbar: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isDark = isGlassDark()

    if (isHeaderShrunk) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, end = 16.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        spotColor = if (isDark) Color(0x500F0F12) else Color(0x1F0F0F12),
                    )
                    .clip(CircleShape)
                    .background(if (isDark) NormalModeSolidColor else Color(0xFFFFFFFF))
                    .border(
                        width = 1.2.dp,
                        color = if (isDark) NormalModePillBorder else Color(0xFFD4D4D8),
                        shape = CircleShape,
                    )
                    .bouncyClickable(onClick = onToggleHeaderShrunk),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.fullscreen,
                    contentDescription = "Expand Header",
                    tint = if (isDark) Color.White else Color(0xFF0F0F12),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        return
    }

    val solidBg = if (isDark) Color(0xFF2B2B2B) else Color(0xFFF0F0F2)
    val textPrimary = if (isDark) Color(0xFFF4F4F6) else Color(0xFF0F0F12)
    val textBody = if (isDark) Color(0xFFF4F4F6) else Color(0xFF18181C)
    val textSub = if (isDark) Color(0xFFD4D4D8) else Color(0xFF52525B)
    val textMuted = if (isDark) Color(0xFF9494A0) else Color(0xFF71717A)
    val innerPillBg = if (isDark) Color(0xFF383838) else Color(0xFFFFFFFF)
    val innerPillBorder = if (isDark) Color(0xFF484848) else Color(0xFFD4D4D8)
    val topBorder = if (isDark) Brush.verticalGradient(listOf(Color(0xFF484848), Color(0xFF383838))) else Brush.verticalGradient(listOf(Color(0xFFE4E4E7), Color(0xFFD4D4D8)))
    val dividerColor = if (isDark) Color(0xFF484848) else Color(0xFFD4D4D8)

    val isEraser = currentTool == Tool.ERASER
    val isLasso = currentTool == Tool.LASSO || currentTool == Tool.SELECT
    val strokeWidthFormatted = ((activeToolWidth * 10.0).roundToInt() / 10.0)

    val topBarModifier = modifier
        .fillMaxWidth()
        .shadow(4.dp, RoundedCornerShape(0.dp), spotColor = if (isDark) Color(0x500F0F12) else Color(0x120F0F12))
        .background(Brush.linearGradient(listOf(solidBg, solidBg)))
        .statusBarsPadding()

    val chromeActiveBg = if (isDark) Color(0xFF1E1E24) else Color(0xFFFFFFFF)
    val chromeTabShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp)

    Column(
        modifier = topBarModifier,
    ) {
        // HEADER TOOLS/NAVIGATION ROW (Single unified 38dp height matching Chrome bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // === LEFT GROUP: Back (32x32) + Embedded Chrome Tabs Strip flush on bottom ===
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Back Chevron Button (Flat Icon, vertically centered)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(32.dp)
                        .bouncyClickable(onClick = onOpenBackstage),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.prev,
                        contentDescription = "Back",
                        tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Embedded Tabs in Header
                if (tabManager != null && tabManager.tabs.isNotEmpty()) {
                    PnotesEmbeddedHeaderTabs(
                        tabManager = tabManager,
                        title = title,
                        onNewTab = onNewTab,
                        onNewCanvasTab = onNewCanvasTab,
                        onImportPdf = onImportPdf,
                        onOpenBrowseDrawer = onOpenBrowseDrawer,
                        onRename = onRename,
                        onCloseTab = onCloseTab,
                        isDark = isDark,
                        innerPillBg = innerPillBg,
                        topBorder = topBorder,
                        textPrimary = textPrimary,
                        textBody = textBody,
                        textMuted = textMuted,
                        modifier = Modifier.fillMaxHeight(),
                    )
                } else {
                    // Single Note Active Chrome Tab (flush at bottom)
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(chromeTabShape)
                            .background(chromeActiveBg)
                            .border(1.dp, if (isDark) Color(0xFF38383E) else Color(0xFFCBD5E1), chromeTabShape)
                            .bouncyClickable { onRename() }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title.ifBlank { "Note 1" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeight = 13.sp,
                            ),
                        )
                    }

                    var showSingleTabMenu by remember { mutableStateOf(false) }

                    // "+" New Tab Button (Chrome style small rounded square)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .bouncyClickable { showSingleTabMenu = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.plus,
                                contentDescription = "New Tab Options",
                                tint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = showSingleTabMenu,
                            onDismissRequest = { showSingleTabMenu = false },
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Note Page", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White else Color(0xFF09090B)) },
                                leadingIcon = { Icon(PnotesIcons.file, null, tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A), modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showSingleTabMenu = false
                                    onNewTab()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Infinite Canvas", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White else Color(0xFF09090B)) },
                                leadingIcon = { Icon(PnotesIcons.grid, null, tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A), modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showSingleTabMenu = false
                                    onNewCanvasTab()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import PDF Document", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White else Color(0xFF09090B)) },
                                leadingIcon = { Icon(PnotesIcons.copy, null, tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A), modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showSingleTabMenu = false
                                    onImportPdf()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Browse All Documents", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White else Color(0xFF09090B)) },
                                leadingIcon = { Icon(PnotesIcons.folder, null, tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A), modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showSingleTabMenu = false
                                    onOpenBrowseDrawer()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // === RIGHT GROUP ===
            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Contextual Action buttons: Apply Style / Clear Page (when active)
                if (isPageStylesOpen) {
                    // Apply Style Button (hug x 32 rd(10))
                    val btnShape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .shadow(4.dp, btnShape, spotColor = if (isDark) Color(0x3518181B) else Color(0x18000000))
                            .clip(btnShape)
                            .background(activeGlassSurface(isDark))
                            .border(1.2.dp, activeGlassLuminousBorder(isDark), btnShape)
                            .bouncyClickable(onClick = onApplyPageStyles),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(modifier = Modifier.matchParentSize().background(ActiveGlassSpecularGlare))
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = PnotesIcons.check,
                                contentDescription = "Apply Style",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Apply Style",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeight = 12.sp,
                                ),
                            )
                        }
                    }
                }

                // Dynamic Active Chip: Magic Wand (when wand is enabled) - Glowing Green (32x32 rd(9))
                if (isWandActive) {
                    val wandGreen = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                    val wandGreenBg = if (isDark) Color(0x3822C55E) else Color(0x2816A34A)
                    val wandGreenBorder = if (isDark) Color(0xCC4ADE80) else Color(0xE616A34A)
                    val wandShape = RoundedCornerShape(9.dp)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = wandShape,
                                spotColor = if (isDark) Color(0x8822C55E) else Color(0x5516A34A),
                                ambientColor = if (isDark) Color(0x4422C55E) else Color(0x3316A34A),
                            )
                            .clip(wandShape)
                            .background(wandGreenBg)
                            .border(1.2.dp, wandGreenBorder, wandShape)
                            .bouncyClickable(onClick = onToggleWand),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.magicWand,
                            contentDescription = "Magic Wand Active (tap to turn off)",
                            tint = wandGreen,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                // Dynamic Active Chip: Live Presentation (when presentation is active)
                if (isPresentationActive) {
                    val presShape = RoundedCornerShape(9.dp)
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(presShape)
                            .background(if (isDark) Color(0x33EF4444) else Color(0x22EF4444))
                            .border(1.2.dp, if (isDark) Color(0xAAEF4444) else Color(0xFFDC2626), presShape)
                            .bouncyClickable(onClick = onTogglePresentation)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = PnotesIcons.present,
                            contentDescription = "Presentation Active (Tap to stop)",
                            tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Live",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeight = 12.sp,
                            ),
                        )
                    }
                }

                // Dynamic Active Chip: Page Angle & Orientation Toggle Button (Shown ONLY when active/open)
                if (isPageAngleOpen) {
                    val pageAngleActiveBg = if (isDark) Color.White.copy(alpha = 0.18f) else Color(0x10000000)
                    val pageAngleActiveBorder = if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF09090B)
                    val angleShape = RoundedCornerShape(9.dp)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(angleShape)
                            .background(pageAngleActiveBg)
                            .border(1.2.dp, pageAngleActiveBorder, angleShape)
                            .bouncyClickable(onClick = onTogglePageAngle),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.pageAngleCompass,
                            contentDescription = "Page Angle & Orientation",
                            tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                // 1. Lock Button (Flat Icon)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .bouncyClickable(onClick = onToggleZoomLock),
                    contentAlignment = Alignment.Center,
                ) {
                    if (zoomLocked) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFBBF24).copy(alpha = 0.50f),
                                            Color(0xFFFBBF24).copy(alpha = 0.15f),
                                            Color.Transparent,
                                        ),
                                    ),
                                    shape = CircleShape,
                                )
                        )
                    }
                    Icon(
                        imageVector = if (zoomLocked) PnotesDynamicIcons.lockActive else PnotesDynamicIcons.lockInactive,
                        contentDescription = if (zoomLocked) "Zoom Locked (Tap to unlock)" else "Lock Zoom",
                        tint = if (zoomLocked) Color.Unspecified else textBody,
                        modifier = Modifier.size(20.5.dp),
                    )
                }

                // 2. Camera / Screenshot Button (Flat Icon)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .bouncyClickable(onClick = onToggleScreenshot),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isScreenshotActive) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFF43F5E).copy(alpha = 0.50f),
                                            Color(0xFFF43F5E).copy(alpha = 0.15f),
                                            Color.Transparent,
                                        ),
                                    ),
                                    shape = CircleShape,
                                )
                        )
                    }
                    Icon(
                        imageVector = if (isScreenshotActive) PnotesDynamicIcons.cameraActive else PnotesDynamicIcons.cameraInactive,
                        contentDescription = "Take Screenshot",
                        tint = if (isScreenshotActive) Color.Unspecified else textBody,
                        modifier = Modifier.size(20.5.dp),
                    )
                }

                // 3. Share Button (Flat Icon with active glow from user SVG)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .bouncyClickable(onClick = onToggleShare),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isShareActive) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF38BDF8).copy(alpha = 0.50f),
                                            Color(0xFF38BDF8).copy(alpha = 0.15f),
                                            Color.Transparent,
                                        ),
                                    ),
                                    shape = CircleShape,
                                )
                        )
                    }
                    Icon(
                        imageVector = if (isShareActive) PnotesDynamicIcons.shareActive else PnotesDynamicIcons.shareInactive,
                        contentDescription = "Share",
                        tint = if (isShareActive) Color.Unspecified else textBody,
                        modifier = Modifier.size(20.5.dp),
                    )
                }

                // 4. Reading Mode (Book) Button (Between Share and Sidebar)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .bouncyClickable(onClick = onToggleReadingMode),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isReadingMode) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF34D399).copy(alpha = 0.50f),
                                            Color(0xFF38BDF8).copy(alpha = 0.15f),
                                            Color.Transparent,
                                        ),
                                    ),
                                    shape = CircleShape,
                                )
                        )
                    }
                    Icon(
                        imageVector = if (isReadingMode) PnotesDynamicIcons.readingModeActive else PnotesDynamicIcons.readingModeInactive,
                        contentDescription = if (isReadingMode) "Reading Mode Active (Pan Only - Tap to exit)" else "Reading Mode (Pan Only)",
                        tint = if (isReadingMode) Color.Unspecified else textBody,
                        modifier = Modifier.size(width = 20.5.dp, height = 18.45.dp),
                    )
                }

                // 5. Sidebar Toggle Button (Flat Icon)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .bouncyClickable(onClick = onTogglePageManager),
                    contentAlignment = Alignment.Center,
                ) {
                    if (sidebarActive) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF06B6D4).copy(alpha = 0.50f),
                                            Color(0xFF06B6D4).copy(alpha = 0.15f),
                                            Color.Transparent,
                                        ),
                                    ),
                                    shape = CircleShape,
                                )
                        )
                    }
                    Icon(
                        imageVector = if (sidebarActive) PnotesDynamicIcons.sidebarActive else PnotesDynamicIcons.sidebarInactive,
                        contentDescription = "Toggle Page Manager",
                        tint = if (sidebarActive) Color.Unspecified else textBody,
                        modifier = Modifier.size(20.5.dp),
                    )
                }

                // 5. More Menu Button (Flat Icon)
                Box {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .bouncyClickable(onClick = onOpenMenu),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.more,
                            contentDescription = "More",
                            tint = textBody,
                            modifier = Modifier.size(20.5.dp),
                        )
                    }
                    menuContent?.invoke()
                }
            }
        }

        // 3. Page-Facing Bottom Separation Rim Stroke
        val isLeftDock = isFullLengthToolbar && !isHeaderShrunk
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (isLeftDock) 44.dp else 0.dp)
                .height(1.dp)
                .background(
                    Color(0xFF555555)
                )
        )
    }
}

/**
 * 2. ADAPTIVE QUICK-TOOL RAIL
 * Can dock to Left, Right, or Top edge.
 * When docked to Top edge, dynamically morphs into a horizontal layout bar.
 * Built with solid surfaces (specular gradient, luminous border, ambient shadow).
 */
@Composable
fun PnotesAdaptiveQuickRail(
    editor: Editor,
    dockEdge: RailDockEdge = RailDockEdge.LEFT,
    isDragging: Boolean = false,
    morphProgress: Float = 0f,
    onToggleStrokeWidthSlider: () -> Unit,
    onToggleEraserSettings: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onDragDelta: ((Float, Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = isGlassDark()
    // Read once here: the rail's cross-fades are configured inside lambdas that are not composable.
    val motion = LocalMotion.current
    val currentTool = editor.tool
    val isEraser = currentTool == Tool.ERASER
    val widthFormatted = ((editor.activeToolWidth * 10.0).roundToInt() / 10.0)
    val isTop = dockEdge == RailDockEdge.TOP || dockEdge == RailDockEdge.BOTTOM

    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val dragModifier = if (onDragDelta != null) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { },
                onDragEnd = { currentOnDragEnd?.invoke() },
                onDragCancel = { currentOnDragEnd?.invoke() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    currentOnDragDelta?.invoke(dragAmount.x, dragAmount.y)
                }
            )
        }
    } else Modifier

    val clampedProgress = morphProgress.coerceIn(0f, 1f)
    val solidBg = if (isDark) Color(0xFF2B2B2B) else Color(0xFFF0F0F2)
    val dividerColor = if (isDark) Color(0xFF404040) else Color(0xFFD4D4D8)
    val gripBg = if (isDark) Color(0xFF383838) else Color(0xFFFFFFFF)
    val gripNotch = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
    val currentCorner = androidx.compose.ui.unit.lerp(26.dp, 28.dp, clampedProgress)
    val railShape = RoundedCornerShape(currentCorner)
    val currentElevation = androidx.compose.ui.unit.lerp(12.dp, 16.dp, clampedProgress)

    Box(
        modifier = modifier
            .glassmorphic(
                shape = railShape,
                elevation = currentElevation,
                borderBrush = SolidColor(if (isDark) Color(0xFF484848) else Color(0xFFD4D4D8)),
                isDark = isDark,
            )
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(
                horizontal = androidx.compose.ui.unit.lerp(if (isTop) 14.dp else 8.dp, 0.dp, clampedProgress).coerceAtLeast(0.dp),
                vertical = androidx.compose.ui.unit.lerp(if (isTop) 7.dp else 10.dp, 0.dp, clampedProgress).coerceAtLeast(0.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (clampedProgress < 0.95f) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = (1f - clampedProgress * 1.5f).coerceIn(0f, 1f)
                    scaleX = androidx.compose.ui.util.lerp(1f, 0.85f, clampedProgress)
                    scaleY = androidx.compose.ui.util.lerp(1f, 0.85f, clampedProgress)
                }
            ) {
                if (isTop) {
                    // =========================================================
                    // === HORIZONTAL MORPHED LAYOUT (DOCKED AT TOP EDGE) ===
                    // =========================================================
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Arming the eraser replaces this entire run of controls with a different
                        // one. The two cross-fade, so the change is seen rather than blinked at.
                        AnimatedContent(
                            targetState = isEraser,
                            transitionSpec = {
                                fadeIn(animationSpec = motion.quick()) togetherWith
                                    fadeOut(animationSpec = motion.quick())
                            },
                            label = "railControlsHorizontal",
                        ) { eraserActive ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                        if (eraserActive) {
                            // Eraser Thickness Pill
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.14f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.28f) else Color(0x18000000), RoundedCornerShape(10.dp))
                            .bouncyClickable(onClick = onToggleEraserSettings)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${editor.activeToolWidth.toInt()} px",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeight = 11.sp,
                            ),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    // Quick Tool Switcher Squircle: When Eraser is active, shows Pen in DEACTIVATED (idle) state
                    val targetPenTool = editor.controller.previousTool?.takeIf { it != Tool.ERASER } ?: Tool.PEN
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.16f) else Color(0x14000000), RoundedCornerShape(10.dp))
                            .bouncyClickable { editor.selectTool(targetPenTool) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(PnotesIcons.pen, "Switch to Pen", tint = if (isDark) Color(0xFFD4D4D8) else SlateSub, modifier = Modifier.size(16.dp))
                    }

                    // Vertical Divider
                    Box(modifier = Modifier.size(1.dp, 20.dp).background(dividerColor))

                    // Eraser Modes Row
                    val cfg = editor.toolConfig(Tool.ERASER)
                    val modeShape = RoundedCornerShape(10.dp)
                    val isArea = cfg.eraseMode == EraseMode.AREA
                    val isStroke = cfg.eraseMode == EraseMode.STROKE
                    // Precision
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .then(
                                if (isArea) {
                                    Modifier
                                        .shadow(3.dp, modeShape, spotColor = if (isDark) Color(0x33000000) else Color(0x14000000))
                                        .clip(modeShape)
                                        .background(activeGlassSurface(isDark))
                                        .border(1.dp, activeGlassLuminousBorder(isDark), modeShape)
                                } else {
                                    Modifier.clip(modeShape)
                                }
                            )
                            .bouncyClickable { editor.updateToolConfig(Tool.ERASER, cfg.copy(eraseMode = EraseMode.AREA)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isArea) {
                            Box(modifier = Modifier.matchParentSize().background(ActiveGlassSpecularGlare))
                        }
                        Icon(PnotesIcons.crosshair, "Precision", tint = if (isArea) Color.White else (if (isDark) Color(0xFFD4D4D8) else SlateSub), modifier = Modifier.size(16.dp))
                    }
                    // Stroke
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .then(
                                if (isStroke) {
                                    Modifier
                                        .shadow(3.dp, modeShape, spotColor = if (isDark) Color(0x33000000) else Color(0x14000000))
                                        .clip(modeShape)
                                        .background(activeGlassSurface(isDark))
                                        .border(1.dp, activeGlassLuminousBorder(isDark), modeShape)
                                } else {
                                    Modifier.clip(modeShape)
                                }
                            )
                            .bouncyClickable { editor.updateToolConfig(Tool.ERASER, cfg.copy(eraseMode = EraseMode.STROKE)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isStroke) {
                            Box(modifier = Modifier.matchParentSize().background(ActiveGlassSpecularGlare))
                        }
                        Icon(PnotesIcons.scribbleLoop, "Stroke", tint = if (isStroke) Color.White else (if (isDark) Color(0xFFD4D4D8) else SlateSub), modifier = Modifier.size(16.dp))
                    }
                } else {
                    // Inking Mode
                    // Thickness Pill
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.14f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.28f) else Color(0x18000000), RoundedCornerShape(10.dp))
                            .bouncyClickable(onClick = onToggleStrokeWidthSlider)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$widthFormatted pt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeight = 11.sp,
                            ),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    // Quick Tool Switcher Squircle: When Pen is active, shows Eraser in DEACTIVATED (idle) state
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.16f) else Color(0x14000000), RoundedCornerShape(10.dp))
                            .bouncyClickable { editor.selectTool(Tool.ERASER) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(PnotesIcons.eraser, "Switch to Eraser", tint = if (isDark) Color(0xFFD4D4D8) else SlateSub, modifier = Modifier.size(16.dp))
                    }

                    // Vertical Divider
                    Box(modifier = Modifier.size(1.dp, 20.dp).background(dividerColor))

                    // Swatches
                    val swatches = editor.toolbarColors
                    val activeIdx = editor.activeColorIndex
                    swatches.forEachIndexed { index, rgba ->
                        val isWhite = rgba.r > 240 && rgba.g > 240 && rgba.b > 240
                        val isSelected = index == activeIdx
                        SwatchCircle(
                            color = rgba.toComposeColor(),
                            borderColor = if (isWhite) (if (isDark) Color(0xFF71717A) else Color(0xFFD4D4D8)) else null,
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    onOpenColorPicker()
                                } else {
                                    editor.pickColor(index)
                                }
                            }
                        )
                    }

                    // Add Color "+" Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(2.dp, CircleShape, spotColor = if (isDark) Color(0x33000000) else Color(0x14000000))
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.14f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.28f) else Color(0xFFE4E4E7), CircleShape)
                            .bouncyClickable(onClick = onOpenColorPicker),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(PnotesIcons.plus, "Add Color", tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B), modifier = Modifier.size(16.dp))
                    }
                }
                            }
                        }
            }
        } else {
            // ===============================================================
            // === VERTICAL STANDARD LAYOUT (DOCKED AT LEFT / RIGHT EDGE) ===
            // ===============================================================
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Same cross-fade as the horizontal rail: arming the eraser swaps the whole run of
                // controls, so the change is seen rather than blinked at.
                AnimatedContent(
                    targetState = isEraser,
                    transitionSpec = {
                        fadeIn(animationSpec = motion.quick()) togetherWith
                            fadeOut(animationSpec = motion.quick())
                    },
                    label = "railControlsVertical",
                ) { eraserActive ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                if (eraserActive) {
                    // Eraser Thickness Badge
                    Box(
                        modifier = Modifier
                            .size(36.dp, 32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.14f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.28f) else Color(0x18000000), RoundedCornerShape(10.dp))
                            .bouncyClickable(onClick = onToggleEraserSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${editor.activeToolWidth.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeight = 11.sp,
                            ),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    // Quick Tool Switcher Squircle: When Eraser is active, shows Pen in DEACTIVATED (idle) state
                    val targetPenTool = editor.controller.previousTool?.takeIf { it != Tool.ERASER } ?: Tool.PEN
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.16f) else Color(0x14000000), RoundedCornerShape(10.dp))
                            .bouncyClickable { editor.selectTool(targetPenTool) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(PnotesIcons.pen, "Switch to Pen", tint = if (isDark) Color(0xFFD4D4D8) else SlateSub, modifier = Modifier.size(17.dp))
                    }

                    Box(modifier = Modifier.size(24.dp, 1.dp).background(dividerColor))

                    val cfg = editor.toolConfig(Tool.ERASER)
                    val modeShape = RoundedCornerShape(10.dp)
                    val isArea = cfg.eraseMode == EraseMode.AREA
                    val isStroke = cfg.eraseMode == EraseMode.STROKE
                    // Precision
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .then(
                                if (isArea) {
                                    Modifier
                                        .shadow(3.dp, modeShape, spotColor = if (isDark) Color(0x33000000) else Color(0x14000000))
                                        .clip(modeShape)
                                        .background(activeGlassSurface(isDark))
                                        .border(1.dp, activeGlassLuminousBorder(isDark), modeShape)
                                } else {
                                    Modifier.clip(modeShape)
                                }
                            )
                            .bouncyClickable { editor.updateToolConfig(Tool.ERASER, cfg.copy(eraseMode = EraseMode.AREA)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isArea) {
                            Box(modifier = Modifier.matchParentSize().background(ActiveGlassSpecularGlare))
                        }
                        Icon(PnotesIcons.crosshair, "Precision", tint = if (isArea) Color.White else (if (isDark) Color(0xFFD4D4D8) else SlateSub), modifier = Modifier.size(16.dp))
                    }
                    // Stroke
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .then(
                                if (isStroke) {
                                    Modifier
                                        .shadow(3.dp, modeShape, spotColor = if (isDark) Color(0x33000000) else Color(0x14000000))
                                        .clip(modeShape)
                                        .background(activeGlassSurface(isDark))
                                        .border(1.dp, activeGlassLuminousBorder(isDark), modeShape)
                                } else {
                                    Modifier.clip(modeShape)
                                }
                            )
                            .bouncyClickable { editor.updateToolConfig(Tool.ERASER, cfg.copy(eraseMode = EraseMode.STROKE)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isStroke) {
                            Box(modifier = Modifier.matchParentSize().background(ActiveGlassSpecularGlare))
                        }
                        Icon(PnotesIcons.scribbleLoop, "Stroke", tint = if (isStroke) Color.White else (if (isDark) Color(0xFFD4D4D8) else SlateSub), modifier = Modifier.size(16.dp))
                    }
                } else {
                    // Inking Mode
                    // Thickness Badge
                    Box(
                        modifier = Modifier
                            .size(36.dp, 32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.14f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.28f) else Color(0x18000000), RoundedCornerShape(10.dp))
                            .bouncyClickable(onClick = onToggleStrokeWidthSlider),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$widthFormatted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeight = 11.sp,
                            ),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    // Quick Tool Switcher Squircle: When Pen is active, shows Eraser in DEACTIVATED (idle) state
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.16f) else Color(0x14000000), RoundedCornerShape(10.dp))
                            .bouncyClickable { editor.selectTool(Tool.ERASER) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(PnotesIcons.eraser, "Switch to Eraser", tint = if (isDark) Color(0xFFD4D4D8) else SlateSub, modifier = Modifier.size(17.dp))
                    }

                    Box(modifier = Modifier.size(24.dp, 1.dp).background(dividerColor))

                    // Swatches
                    val swatches = editor.toolbarColors
                    val activeIdx = editor.activeColorIndex
                    swatches.forEachIndexed { index, rgba ->
                        val isWhite = rgba.r > 240 && rgba.g > 240 && rgba.b > 240
                        val isSelected = index == activeIdx
                        SwatchCircle(
                            color = rgba.toComposeColor(),
                            borderColor = if (isWhite) (if (isDark) Color(0xFF71717A) else Color(0xFFD4D4D8)) else null,
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    onOpenColorPicker()
                                } else {
                                    editor.pickColor(index)
                                }
                            }
                        )
                    }

                    // Add Color "+" Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .shadow(2.dp, CircleShape, spotColor = if (isDark) Color(0x33000000) else Color(0x14000000))
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.14f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.28f) else Color(0xFFE4E4E7), CircleShape)
                            .bouncyClickable(onClick = onOpenColorPicker),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(PnotesIcons.plus, "Add Color", tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B), modifier = Modifier.size(15.dp))
                    }
                }
                    }
                }
            }
        }
            }
        }

        if (clampedProgress > 0.05f) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = ((clampedProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                    scaleX = androidx.compose.ui.util.lerp(0.65f, 1f, clampedProgress)
                    scaleY = androidx.compose.ui.util.lerp(0.65f, 1f, clampedProgress)
                },
                contentAlignment = Alignment.Center,
            ) {
                if (isEraser) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                spotColor = if (isDark) Color(0x40000000) else Color(0x14000000),
                                ambientColor = if (isDark) Color(0x20000000) else Color(0x0A000000),
                            )
                            .clip(CircleShape)
                            .background(PurpleTheme.activeGradient(isDark))
                            .border(
                                width = 1.dp,
                                brush = PurpleTheme.activeBorder(isDark),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.eraser,
                            contentDescription = null,
                            tint = PurpleTheme.accentFg(isDark),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else {
                    val activeRgba = editor.toolbarColors.getOrNull(editor.activeColorIndex) ?: editor.toolbarColors.firstOrNull()
                    val activeColor = activeRgba?.toComposeColor() ?: (if (isDark) Color.White else Color(0xFF09090B))
                    val isWhite = activeRgba?.let { it.r > 240 && it.g > 240 && it.b > 240 } ?: false
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                spotColor = activeColor.copy(alpha = 0.60f),
                                ambientColor = Color(0x40000000),
                            )
                            .clip(CircleShape)
                            .background(activeColor)
                            .border(
                                width = 2.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = 0.25f)
                                    )
                                ),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                            .background(if (isWhite) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.40f))
                        )
                    }
                }
            }
        }
    }
}

/** Backward compatibility alias for any existing callers */
@Composable
fun PnotesLeftQuickRail(
    editor: Editor,
    onToggleStrokeWidthSlider: () -> Unit,
    onToggleEraserSettings: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onDrag: ((Float) -> Unit)? = null,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier,
) {
    PnotesAdaptiveQuickRail(
        editor = editor,
        dockEdge = RailDockEdge.LEFT,
        isDragging = isDragging,
        onToggleStrokeWidthSlider = onToggleStrokeWidthSlider,
        onToggleEraserSettings = onToggleEraserSettings,
        onOpenColorPicker = onOpenColorPicker,
        onDragDelta = if (onDrag != null) { { _, dy -> onDrag(dy) } } else null,
        modifier = modifier,
    )
}

@Composable
private fun SwatchCircle(
    color: Color,
    selected: Boolean,
    borderColor: Color? = null,
    onClick: () -> Unit,
) {
    val isDark = isGlassDark()
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.border(2.dp, if (isDark) Color.White else Color(0xFF09090B), CircleShape).padding(2.dp)
                else Modifier.padding(2.dp)
            )
            .bouncyClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(if (selected) 2.dp else 1.dp, CircleShape, spotColor = if (isDark) Color(0x66000000) else Color(0x20000000))
                .clip(CircleShape)
                .then(
                    if (borderColor != null) Modifier.border(1.2.dp, borderColor, CircleShape)
                    else Modifier
                )
                .background(color)
        )
    }
}

/**
 * 3. BOTTOM FLOATING DOCK
 * Blueprint: s(hug, 56) | p(429, 734) | rd(22) | f[rgba(255,255,255,0.9), blur(28)]
 * st[1.5.dp #FFFFFF] | shadow(#0F172A, 14%, y=12, blur=36)
 */
@Composable
fun PnotesBottomDock(
    editor: Editor,
    onOpenPageStyles: () -> Unit,
    onToggleStrokeWidthSlider: () -> Unit = {},
    onToggleEraserSettings: () -> Unit = {},
    isPageStylesOpen: Boolean = false,
    dockEdge: RailDockEdge = RailDockEdge.BOTTOM,
    isDragging: Boolean = false,
    morphProgress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    var activePopupTool by remember { mutableStateOf<Tool?>(null) }

    LaunchedEffect(editor.tool) {
        if (activePopupTool != null && activePopupTool != editor.tool) {
            activePopupTool = null
        }
    }

    PnotesBottomDockContent(
        currentTool = editor.tool,
        isWandActive = editor.wandEnabled,
        isPageStylesOpen = isPageStylesOpen,
        activePopupTool = activePopupTool,
        dockEdge = dockEdge,
        isDragging = isDragging,
        morphProgress = morphProgress,
        onSelectTool = { tool ->
            activePopupTool = null
            editor.selectTool(tool)
        },
        onToolClick = { clickedTool ->
            when (clickedTool) {
                Tool.SELECT, Tool.LASSO -> {
                    activePopupTool = null
                    if (editor.tool == Tool.SELECT) {
                        editor.selectTool(Tool.LASSO)
                    } else if (editor.tool == Tool.LASSO) {
                        editor.selectTool(Tool.SELECT)
                    } else {
                        editor.selectTool(Tool.SELECT)
                    }
                }
                Tool.PEN -> {
                    if (editor.tool == Tool.PEN) {
                        activePopupTool = if (activePopupTool != null) null else Tool.PEN
                    } else {
                        activePopupTool = null
                        editor.selectTool(Tool.PEN)
                    }
                }
                Tool.CALLIGRAPHY -> {
                    if (editor.tool == Tool.CALLIGRAPHY) {
                        activePopupTool = if (activePopupTool != null) null else Tool.CALLIGRAPHY
                    } else {
                        activePopupTool = null
                        editor.selectTool(Tool.CALLIGRAPHY)
                    }
                }
                Tool.DASHED -> {
                    if (editor.tool == Tool.DASHED) {
                        activePopupTool = if (activePopupTool != null) null else Tool.DASHED
                    } else {
                        activePopupTool = null
                        editor.selectTool(Tool.DASHED)
                    }
                }
                Tool.TAPER, Tool.SPEED -> {
                    val isBrushActive = editor.tool == Tool.TAPER || editor.tool == Tool.SPEED
                    if (isBrushActive) {
                        activePopupTool = if (activePopupTool != null) null else editor.tool
                    } else {
                        activePopupTool = null
                        editor.selectTool(Tool.TAPER)
                    }
                }
                Tool.HIGHLIGHTER -> {
                    if (editor.tool == Tool.HIGHLIGHTER) {
                        activePopupTool = if (activePopupTool != null) null else Tool.HIGHLIGHTER
                    } else {
                        activePopupTool = null
                        editor.selectTool(Tool.HIGHLIGHTER)
                    }
                }
                Tool.ERASER -> {
                    if (editor.tool == Tool.ERASER) {
                        activePopupTool = if (activePopupTool != null) null else Tool.ERASER
                    } else {
                        activePopupTool = null
                        editor.selectTool(Tool.ERASER)
                    }
                }
                Tool.SHAPE -> {
                    if (editor.tool == Tool.SHAPE) {
                        activePopupTool = if (activePopupTool != null) null else Tool.SHAPE
                    } else {
                        activePopupTool = null
                        editor.selectTool(Tool.SHAPE)
                    }
                }
                Tool.TEXT, Tool.TEXT_BOX -> {
                    if (editor.tool == Tool.TEXT || editor.tool == Tool.TEXT_BOX) {
                        activePopupTool = if (activePopupTool != null) null else Tool.TEXT
                    } else {
                        activePopupTool = null
                        editor.selectTool(Tool.TEXT)
                    }
                }
                Tool.TABLE -> {
                    if (editor.tool == Tool.TABLE) {
                        activePopupTool = if (activePopupTool != null) null else Tool.TABLE
                    } else {
                        activePopupTool = null
                        editor.selectTool(Tool.TABLE)
                    }
                }
                else -> {
                    if (editor.tool == clickedTool) {
                        activePopupTool = if (activePopupTool != null) null else clickedTool
                    } else {
                        activePopupTool = null
                        editor.selectTool(clickedTool)
                    }
                }
            }
        },
        onToggleWand = {
            activePopupTool = null
            editor.toggleWand()
        },
        onOpenPageStyles = {
            activePopupTool = null
            onOpenPageStyles()
        },
        onToggleStrokeWidthSlider = onToggleStrokeWidthSlider,
        onToggleEraserSettings = onToggleEraserSettings,
        onToggleLassoSelection = { editor.hasSelection = !editor.hasSelection },
        toolPopup = { tool, visible ->
            when (tool) {
                Tool.ERASER -> EraserConfigPopup(editor, onDismiss = { activePopupTool = null }, visible = visible)
                Tool.SHAPE -> ShapeConfigPopup(editor, onDismiss = { activePopupTool = null }, visible = visible)
                Tool.TABLE -> TableConfigPopup(
                    config = editor.tableConfig,
                    onConfigChange = { editor.updateTableConfig(it) },
                    onInsertTable = { editor.insertTable(it) },
                    onDismiss = { activePopupTool = null },
                    visible = visible,
                )
                Tool.SELECT, Tool.LASSO -> SelectConfigPopup(editor, onDismiss = { activePopupTool = null }, visible = visible)
                Tool.TEXT -> TextToolConfigPopup(editor, onDismiss = { activePopupTool = null }, visible = visible)
                else -> ToolConfigPopup(editor, editor.tool, onDismiss = { activePopupTool = null }, visible = visible)
            }
        },
        modifier = modifier,
    )
}

/**
 * Pure, Preview-compatible bottom dock for instant Android Studio Preview tab rendering.
 * Supports both horizontal (BOTTOM/TOP) and vertical (LEFT/RIGHT) adaptive edge docking,
 * and morphs into a compact circular puck during active dragging.
 */
@Composable
fun PnotesBottomDockContent(
    currentTool: Tool = Tool.PEN,
    isWandActive: Boolean = false,
    isPageStylesOpen: Boolean = false,
    activePopupTool: Tool? = null,
    dockEdge: RailDockEdge = RailDockEdge.BOTTOM,
    isDragging: Boolean = false,
    morphProgress: Float = 0f,
    onSelectTool: (Tool) -> Unit = {},
    onToolClick: (Tool) -> Unit = onSelectTool,
    onToggleWand: () -> Unit = {},
    onOpenPageStyles: () -> Unit = {},
    onToggleStrokeWidthSlider: () -> Unit = {},
    onToggleEraserSettings: () -> Unit = {},
    onToggleLassoSelection: () -> Unit = {},
    toolPopup: @Composable (Tool, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val isDark = isGlassDark()
    val isVertical = dockEdge == RailDockEdge.LEFT || dockEdge == RailDockEdge.RIGHT
    val clampedProgress = morphProgress.coerceIn(0f, 1f)
    val currentCorner = androidx.compose.ui.unit.lerp(26.dp, 28.dp, clampedProgress)
    val dockShape = RoundedCornerShape(currentCorner)
    val currentElevation = androidx.compose.ui.unit.lerp(12.dp, 16.dp, clampedProgress)
    val solidBg = if (isDark) Color(0xFF2B2B2B) else Color(0xFFF0F0F2)
    val dividerColor = if (isDark) Color(0xFF404040) else Color(0xFFD4D4D8)
    val gripBg = if (isDark) Color(0xFF383838) else Color(0xFFFFFFFF)
    val gripNotch = if (isDark) Color(0xFF9494A0) else Color(0xFF71717A)

    val slotBounds = remember { DockSlotBounds() }
    var dockRect by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier
            .then(
                if (clampedProgress > 0f) Modifier
                else if (isVertical) Modifier.width(44.dp)
                else Modifier.height(44.dp)
            )
            .onGloballyPositioned { dockRect = it.boundsInRoot() }
            .shadow(
                elevation = currentElevation,
                shape = dockShape,
                clip = false,
                spotColor = if (isDark) Color(0x45000000) else Color(0x1A000000),
                ambientColor = if (isDark) Color(0x20000000) else Color(0x0A000000),
            )
            .graphicsLayer {
                this.shape = dockShape
                this.clip = true
            }
            .clip(dockShape)
            .drawWithContent {
                val outline = dockShape.createOutline(size, layoutDirection, this)
                val path = Path().apply { addOutline(outline) }
                clipPath(path) {
                    this@drawWithContent.drawContent()
                }
            }
            .background(solidBg)
            .clip(dockShape),
        contentAlignment = Alignment.Center,
    ) {
        // Base layer (Solid #2B2B2B in Dark, #F0F0F2 in Light)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(dockShape)
                .background(solidBg)
        )

        val isSelect = currentTool == Tool.SELECT
        val isEraser = currentTool == Tool.ERASER
        val isLasso = currentTool == Tool.LASSO
        val isText = currentTool == Tool.TEXT || currentTool == Tool.TEXT_BOX
        val isHighlighter = currentTool == Tool.HIGHLIGHTER
        val isBrush = currentTool == Tool.TAPER || currentTool == Tool.SPEED
        val isPen = currentTool.isStroke && !isHighlighter && !isBrush
        val isShape = currentTool == Tool.SHAPE
        val isTable = currentTool == Tool.TABLE
        val isImage = currentTool == Tool.IMAGE
        // The tool whose panel is (still) on screen: it outlives the tap that closed it so the menu
        // can animate away instead of being torn down with its window.
        val openedPopup = rememberLastPresent(activePopupTool)

        val itemsContent: @Composable () -> Unit = {
            // 1. Select/Lasso Tool (single slot — popup toggles between modes)
            Box {
                DockToolSlot(
                    icon = PnotesDynamicIcons.lassoInactive,
                    activeIcon = PnotesDynamicIcons.lassoActive,
                    label = if (isLasso) "Lasso" else "Select",
                    active = (isSelect || isLasso) && !isPageStylesOpen,
                    slotId = "select",
                    isVertical = isVertical,
                    onClick = { onToolClick(if (isLasso) Tool.LASSO else Tool.SELECT) },
                )
                if (openedPopup == Tool.SELECT || openedPopup == Tool.LASSO) {
                    toolPopup(openedPopup ?: Tool.SELECT, activePopupTool == Tool.SELECT || activePopupTool == Tool.LASSO)
                }
            }

            // 2. Eraser Tool
            Box {
                DockToolSlot(
                    icon = PnotesDynamicIcons.eraserInactive,
                    activeIcon = PnotesDynamicIcons.eraserActive,
                    label = "Eraser",
                    active = isEraser && !isPageStylesOpen,
                    slotId = "eraser",
                    isVertical = isVertical,
                    onClick = { onToolClick(Tool.ERASER) },
                )
                if (openedPopup == Tool.ERASER) {
                    toolPopup(Tool.ERASER, activePopupTool == Tool.ERASER)
                }
            }

            // Divider 1
            Box(
                modifier = if (isVertical) {
                    Modifier
                        .height(1.dp)
                        .width(22.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, dividerColor, Color.Transparent)
                            )
                        )
                } else {
                    Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, dividerColor, Color.Transparent)
                            )
                        )
                }
            )

            // 3. Active Pen Tool
            val currentPen = if (currentTool.isStroke && !isHighlighter && !isBrush) currentTool else Tool.PEN
            val (penInactive, penActive) = if (currentPen == Tool.CALLIGRAPHY) {
                PnotesDynamicIcons.fountainPenInactive to PnotesDynamicIcons.fountainPenActive
            } else if (currentPen == Tool.TAPER || currentPen == Tool.SPEED) {
                PnotesDynamicIcons.brushInactive to PnotesDynamicIcons.brushActive
            } else {
                PnotesDynamicIcons.penInactive to PnotesDynamicIcons.penActive
            }
            Box {
                DockToolSlot(
                    icon = penInactive,
                    activeIcon = penActive,
                    label = penToolLabel(currentPen),
                    active = isPen && !isPageStylesOpen,
                    slotId = "pen",
                    isVertical = isVertical,
                    onClick = { onToolClick(currentPen) },
                )
                if (openedPopup != null && openedPopup.isStroke && openedPopup != Tool.HIGHLIGHTER) {
                    toolPopup(openedPopup, activePopupTool == openedPopup)
                }
            }

            // 4. Highlighter Tool
            Box {
                DockToolSlot(
                    icon = PnotesDynamicIcons.highlighterInactive,
                    activeIcon = PnotesDynamicIcons.highlighterActive,
                    label = "Highlighter",
                    active = isHighlighter && !isPageStylesOpen,
                    slotId = "highlighter",
                    isVertical = isVertical,
                    onClick = { onToolClick(Tool.HIGHLIGHTER) },
                )
                if (openedPopup == Tool.HIGHLIGHTER) {
                    toolPopup(Tool.HIGHLIGHTER, activePopupTool == Tool.HIGHLIGHTER)
                }
            }

            // 5. Shapes Tool (Toggle)
            Box {
                DockToolSlot(
                    icon = PnotesDynamicIcons.shapesInactive,
                    activeIcon = PnotesDynamicIcons.shapesActive,
                    label = "Shapes",
                    active = isShape && !isPageStylesOpen,
                    slotId = "shapes",
                    isVertical = isVertical,
                    onClick = { onToolClick(Tool.SHAPE) },
                )
                if (openedPopup == Tool.SHAPE) {
                    toolPopup(Tool.SHAPE, activePopupTool == Tool.SHAPE)
                }
            }

            // Divider 2
            Box(
                modifier = if (isVertical) {
                    Modifier
                        .height(1.dp)
                        .width(22.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, dividerColor, Color.Transparent)
                            )
                        )
                } else {
                    Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, dividerColor, Color.Transparent)
                            )
                        )
                }
            )

            // 5. Text Tool
            Box {
                DockToolSlot(
                    icon = PnotesDynamicIcons.textInactive,
                    activeIcon = PnotesDynamicIcons.textActive,
                    label = "Text",
                    active = isText && !isPageStylesOpen,
                    slotId = "text",
                    isVertical = isVertical,
                    onClick = { onToolClick(Tool.TEXT) },
                )
                if (openedPopup == Tool.TEXT) {
                    toolPopup(Tool.TEXT, activePopupTool == Tool.TEXT)
                }
            }

            // 6. Magic Wand Tool (Disappearing Ink) - Independent Glowing Green Toggle
            Box {
                DockWandSlot(
                    icon = PnotesDynamicIcons.magicWandInactive,
                    activeIcon = PnotesDynamicIcons.magicWandActive,
                    label = "Wand",
                    active = isWandActive,
                    isVertical = isVertical,
                    onClick = onToggleWand,
                )
            }

            // 7. Styles Pill (Palette)
            Box {
                DockToolSlot(
                    icon = PnotesIcons.palette,
                    label = "Styles",
                    active = isPageStylesOpen,
                    slotId = "styles",
                    isVertical = isVertical,
                    onClick = onOpenPageStyles,
                )
            }
        }

        if (morphProgress < 0.95f) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = (1f - morphProgress * 1.5f).coerceIn(0f, 1f)
                    scaleX = androidx.compose.ui.util.lerp(1f, 0.85f, morphProgress)
                    scaleY = androidx.compose.ui.util.lerp(1f, 0.85f, morphProgress)
                }
            ) {
                // The slots report themselves to the dock's indicator from inside this layer.
                CompositionLocalProvider(LocalDockSlotBounds provides slotBounds) {
                    if (isVertical) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 6.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsContent()
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsContent()
                        }
                    }
                }
            }
        } else {
            // Fully collapsed: the tools are gone, so forget where they were. The pill would
            // otherwise fly in from its old rectangle when the dock opens again.
            DisposableEffect(Unit) {
                slotBounds.clear()
                onDispose { }
            }
        }

        if (morphProgress > 0.05f) {
            val activeIcon = when {
                isPageStylesOpen -> PnotesIcons.palette
                isSelect -> PnotesIcons.protoSelect
                isEraser -> PnotesIcons.protoEraser
                isPen -> PnotesIcons.protoPen
                isBrush -> PnotesIcons.protoBrush
                isHighlighter -> PnotesIcons.protoHighlighter
                isShape -> PnotesIcons.shape
                isText -> PnotesIcons.protoText
                isLasso -> PnotesIcons.lasso
                isWandActive -> PnotesIcons.protoMagicWand
                else -> PnotesIcons.protoPen
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = ((morphProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        scaleX = androidx.compose.ui.util.lerp(0.65f, 1f, morphProgress)
                        scaleY = androidx.compose.ui.util.lerp(0.65f, 1f, morphProgress)
                    },
                contentAlignment = Alignment.Center,
            ) {
                val collapsedSquircle = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = collapsedSquircle,
                            spotColor = if (isDark) Color(0x40000000) else Color(0x14000000),
                            ambientColor = if (isDark) Color(0x20000000) else Color(0x0A000000),
                        )
                        .clip(collapsedSquircle)
                        .background(PurpleTheme.activeGradient(isDark))
                        .border(
                            width = 1.dp,
                            brush = PurpleTheme.activeBorder(isDark),
                            shape = collapsedSquircle,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = activeIcon,
                        contentDescription = null,
                        tint = PurpleTheme.accentFg(isDark),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/**
 * UNIFIED COMBINED DOCK
 * Formed dynamically when both the Main Dock and the Quick Rail snap to the same edge.
 * Exact prototype unified toolbar layout:
 * [9 Tools: Select/Lasso, Eraser, Pen, Brush, Highlighter, Shape, Text, Image, Wand]
 * | [Divider 1] | [Width Pill + 3 Quick Preset Dots]
 * | [Divider 2] | [5 Color Swatches + Palette Picker]
 * | [Divider 3] | [Undo, Redo]
 */
@Composable
fun PnotesCombinedDock(
    editor: Editor,
    dockEdge: RailDockEdge = RailDockEdge.BOTTOM,
    isDragging: Boolean = false,
    morphProgress: Float = 0f,
    isFullLength: Boolean = false,
    isPageStylesOpen: Boolean = false,
    activePopupTool: Tool? = null,
    onActivePopupToolChange: ((Tool?) -> Unit)? = null,
    onOpenPageStyles: () -> Unit = {},
    onToggleStrokeWidthSlider: () -> Unit = {},
    onToggleEraserSettings: () -> Unit = {},
    onOpenColorPicker: (Boolean) -> Unit = {},
    onInsertImage: () -> Unit = {},
    onUndo: () -> Unit = { editor.undo() },
    onRedo: () -> Unit = { editor.redo() },
    onDetach: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var localActivePopupTool by remember { mutableStateOf<Tool?>(null) }
    val effectiveActivePopupTool = if (onActivePopupToolChange != null) activePopupTool else localActivePopupTool
    val updatePopupTool: (Tool?) -> Unit = { tool ->
        if (onActivePopupToolChange != null) {
            onActivePopupToolChange(tool)
        } else {
            localActivePopupTool = tool
        }
    }

    LaunchedEffect(editor.tool) {
        if (effectiveActivePopupTool != null && effectiveActivePopupTool != editor.tool) {
            updatePopupTool(null)
        }
    }

    PnotesCombinedDockContent(
        editor = editor,
        currentTool = editor.tool,
        isWandActive = editor.wandEnabled,
        isPageStylesOpen = isPageStylesOpen,
        activePopupTool = effectiveActivePopupTool,
        dockEdge = dockEdge,
        isDragging = isDragging,
        morphProgress = morphProgress,
        isFullLength = isFullLength,
        onSelectTool = { tool ->
            updatePopupTool(null)
            if (editor.wandEnabled) editor.toggleWand()
            editor.selectTool(tool)
        },
        onToolClick = { clickedTool ->
            if (editor.wandEnabled && clickedTool != Tool.PAN) {
                editor.toggleWand()
            }
            when (clickedTool) {
                Tool.SELECT, Tool.LASSO -> {
                    if (editor.tool == Tool.LASSO || editor.tool == Tool.SELECT) {
                        updatePopupTool(if (effectiveActivePopupTool != null) null else clickedTool)
                    } else {
                        updatePopupTool(null)
                        editor.selectTool(Tool.LASSO)
                    }
                }
                Tool.PEN -> {
                    if (editor.tool == Tool.PEN) {
                        updatePopupTool(if (effectiveActivePopupTool != null) null else Tool.PEN)
                    } else {
                        updatePopupTool(null)
                        editor.selectTool(Tool.PEN)
                    }
                }
                Tool.CALLIGRAPHY -> {
                    if (editor.tool == Tool.CALLIGRAPHY) {
                        updatePopupTool(if (effectiveActivePopupTool != null) null else Tool.CALLIGRAPHY)
                    } else {
                        updatePopupTool(null)
                        editor.selectTool(Tool.CALLIGRAPHY)
                    }
                }
                Tool.DASHED -> {
                    if (editor.tool == Tool.DASHED) {
                        updatePopupTool(if (effectiveActivePopupTool != null) null else Tool.DASHED)
                    } else {
                        updatePopupTool(null)
                        editor.selectTool(Tool.DASHED)
                    }
                }
                Tool.TAPER, Tool.SPEED -> {
                    val isBrushActive = editor.tool == Tool.TAPER || editor.tool == Tool.SPEED
                    if (isBrushActive) {
                        updatePopupTool(if (effectiveActivePopupTool != null) null else editor.tool)
                    } else {
                        updatePopupTool(null)
                        editor.selectTool(Tool.TAPER)
                    }
                }
                Tool.HIGHLIGHTER -> {
                    if (editor.tool == Tool.HIGHLIGHTER) {
                        updatePopupTool(if (effectiveActivePopupTool != null) null else Tool.HIGHLIGHTER)
                    } else {
                        updatePopupTool(null)
                        editor.selectTool(Tool.HIGHLIGHTER)
                    }
                }
                Tool.ERASER -> {
                    if (editor.tool == Tool.ERASER) {
                        onToggleEraserSettings()
                    } else {
                        updatePopupTool(null)
                        editor.selectTool(Tool.ERASER)
                    }
                }
                Tool.SHAPE -> {
                    editor.selectTool(Tool.SHAPE)
                    updatePopupTool(if (effectiveActivePopupTool == Tool.SHAPE) null else Tool.SHAPE)
                }
                Tool.TEXT, Tool.TEXT_BOX -> {
                    if (editor.tool == Tool.TEXT || editor.tool == Tool.TEXT_BOX) {
                        updatePopupTool(if (effectiveActivePopupTool != null) null else Tool.TEXT)
                    } else {
                        updatePopupTool(null)
                        editor.selectTool(Tool.TEXT)
                    }
                }
                Tool.TABLE -> {
                    editor.selectTool(Tool.TABLE)
                    updatePopupTool(if (effectiveActivePopupTool == Tool.TABLE) null else Tool.TABLE)
                }
                else -> {
                    if (editor.tool == clickedTool) {
                        updatePopupTool(if (effectiveActivePopupTool != null) null else clickedTool)
                    } else {
                        updatePopupTool(null)
                        editor.selectTool(clickedTool)
                    }
                }
            }
        },
        onToggleWand = {
            updatePopupTool(null)
            editor.toggleWand()
        },
        onOpenPageStyles = {
            updatePopupTool(null)
            onOpenPageStyles()
        },
        onToggleStrokeWidthSlider = onToggleStrokeWidthSlider,
        onToggleEraserSettings = onToggleEraserSettings,
        onOpenColorPicker = onOpenColorPicker,
        onInsertImage = onInsertImage,
        onUndo = onUndo,
        onRedo = onRedo,
        onDetach = onDetach,
        toolPopup = { tool, visible ->
            when (tool) {
                Tool.ERASER -> EraserConfigPopup(editor, onDismiss = { updatePopupTool(null) }, visible = visible)
                Tool.SHAPE -> ShapeConfigPopup(editor, onDismiss = { updatePopupTool(null) }, visible = visible)
                Tool.TABLE -> TableConfigPopup(
                    config = editor.tableConfig,
                    onConfigChange = { editor.updateTableConfig(it) },
                    onInsertTable = { editor.insertTable(it) },
                    onDismiss = { updatePopupTool(null) },
                    visible = visible,
                )
                Tool.SELECT, Tool.LASSO -> SelectConfigPopup(editor, onDismiss = { updatePopupTool(null) }, visible = visible)
                Tool.TEXT, Tool.TEXT_BOX -> TextToolConfigPopup(editor, onDismiss = { updatePopupTool(null) }, visible = visible)
                else -> ToolConfigPopup(editor, tool, onDismiss = { updatePopupTool(null) }, visible = visible)
            }
        },
        modifier = modifier,
    )
}

private val FigPenLeftClipShape = GenericShape { size, _ ->
    addRect(
        Rect(
            left = 0f,
            top = -100f,
            right = size.width + 2000f,
            bottom = size.height + 100f,
        )
    )
}

private val FigPenTopClipShape = GenericShape { size, _ ->
    addRect(
        Rect(
            left = -100f,
            top = 0f,
            right = size.width + 100f,
            bottom = size.height + 2000f,
        )
    )
}

@Composable
private fun FigStylusPenItem(
    drawableRes: Int,
    contentDescription: String,
    yOffset: Dp,
    isActive: Boolean,
    width: Dp,
    height: Dp,
    restingX: Dp,
    activeX: Dp,
    onClick: () -> Unit,
) {
    val animatedX by animateDpAsState(
        targetValue = if (isActive) activeX else restingX,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 650f),
        label = "${contentDescription}_x",
    )

    Box(
        modifier = Modifier
            .offset(x = 0.dp, y = yOffset)
            .fillMaxWidth()
            .height(32.dp)
            .clip(FigPenLeftClipShape)
            .zIndex(if (isActive) 10f else 1f)
            .bouncyClickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .offset(x = animatedX)
                .requiredSize(width = width, height = height),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun FigStylusPenItemHorizontal(
    drawableRes: Int,
    contentDescription: String,
    xOffset: Dp,
    isActive: Boolean,
    width: Dp,
    height: Dp,
    restingY: Dp,
    activeY: Dp,
    onClick: () -> Unit,
) {
    val animatedY by animateDpAsState(
        targetValue = if (isActive) activeY else restingY,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 650f),
        label = "${contentDescription}_y",
    )

    Box(
        modifier = Modifier
            .offset(x = xOffset, y = 0.dp)
            .width(32.dp)
            .fillMaxHeight()
            .clip(FigPenTopClipShape)
            .zIndex(if (isActive) 10f else 1f)
            .bouncyClickable(onClick = onClick),
        contentAlignment = Alignment.TopCenter,
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .offset(y = animatedY)
                .requiredSize(width = width, height = height),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun PnotesCombinedDockContent(
    editor: Editor,
    currentTool: Tool = Tool.PEN,
    isWandActive: Boolean = false,
    isPageStylesOpen: Boolean = false,
    activePopupTool: Tool? = null,
    dockEdge: RailDockEdge = RailDockEdge.LEFT,
    isDragging: Boolean = false,
    morphProgress: Float = 0f,
    isFullLength: Boolean = false,
    onSelectTool: (Tool) -> Unit = {},
    onToolClick: (Tool) -> Unit = onSelectTool,
    onToggleWand: () -> Unit = {},
    onOpenPageStyles: () -> Unit = {},
    onToggleStrokeWidthSlider: () -> Unit = {},
    onToggleEraserSettings: () -> Unit = {},
    onOpenColorPicker: (Boolean) -> Unit = {},
    onInsertImage: () -> Unit = {},
    onUndo: () -> Unit = { editor.undo() },
    onRedo: () -> Unit = { editor.redo() },
    onDetach: () -> Unit = {},
    toolPopup: @Composable (Tool, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val isVertical = dockEdge == RailDockEdge.LEFT || dockEdge == RailDockEdge.RIGHT
    val clampedProgress = morphProgress.coerceIn(0f, 1f)

    // Morph animation when dragged across edges (balanced 44dp profile)
    val morphW = if (isVertical) 44.dp else 664.dp
    val morphH = if (isVertical) 664.dp else 44.dp
    val circleSize = 56.dp
    val targetW = androidx.compose.ui.unit.lerp(morphW, circleSize, clampedProgress)
    val targetH = androidx.compose.ui.unit.lerp(morphH, circleSize, clampedProgress)
    val targetCorner = androidx.compose.ui.unit.lerp(24.dp, circleSize / 2f, clampedProgress)
    val dockShape = RoundedCornerShape(targetCorner)

    val currentElevation = androidx.compose.ui.unit.lerp(8.dp, 12.dp, clampedProgress)
    val isDark = isGlassDark()
    val dockBorderColor = if (isDark) Color(0xFF38383E) else Color(0xFFE2E8F0)
    val dockBackgroundColor = if (isDark) Color(0xFF202024) else Color(0xFFFFFFFF)
    val dividerColor = if (isDark) Color(0xFF38383E) else Color(0xFFE2E8F0)
    val actionIconColor = if (isDark) Color(0xFFD9D9D9) else Color(0xFF1E293B)
    val swatchBorderColor = if (isDark) Color.White else Color(0xFF1E293B)
    val strokeDotActiveColor = if (isDark) Color.White else Color(0xFF1E293B)
    val strokeDotInactiveColor = if (isDark) Color(0xFF9AA0A6) else Color(0xFF94A3B8)

    Box(modifier = modifier) {
        // 1. DOCK SHELL: Solid Background, Shadow & Outer Border (drawn BEHIND the pens so the border never cuts through them)
        Box(
            modifier = Modifier
                .size(targetW, targetH)
                .shadow(
                    elevation = currentElevation,
                    shape = dockShape,
                    clip = false,
                )
                .background(dockBackgroundColor, dockShape)
                .border(1.dp, dockBorderColor, dockShape)
        )

        // 2. DOCK CONTENT: Tools, Stylus Pens, Swatches, Action Buttons (drawn ON TOP of the dock shell)
        Box(
            modifier = Modifier.size(targetW, targetH),
            contentAlignment = Alignment.TopStart,
        ) {
            if (clampedProgress < 0.6f) {
                if (isVertical) {
                    // ==================== VERTICAL ORIENTATION (38dp x 664dp) ====================
                    // 1. TOOL SETTINGS (y = 10.dp)
                    Box(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 10.dp)
                            .fillMaxWidth()
                            .height(34.dp)
                            .bouncyClickable(onClick = onOpenPageStyles),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fig_tool_settings),
                            contentDescription = "Tool Settings",
                            colorFilter = ColorFilter.tint(actionIconColor),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // 2. GROUP 3: 7 STYLUS PENS (Full large size, resting tips aligned with 2-3px gap to boundary, active protruding 14dp into canvas)
                    // Pen 0: Eraser (y = 44.dp)
                    FigStylusPenItem(
                        drawableRes = if (isDark) R.drawable.fig_tool_eraser else R.drawable.fig_tool_eraser_light,
                        contentDescription = "Eraser",
                        yOffset = 44.dp,
                        isActive = (currentTool == Tool.ERASER),
                        width = 53.dp,
                        height = 20.dp,
                        restingX = (-10.4).dp,
                        activeX = 5.dp,
                        onClick = { onToolClick(Tool.ERASER) },
                    )

                    // Pen 1: Fineliner (y = 76.dp)
                    val isFinelinerActive = (currentTool == Tool.PEN && !isWandActive) ||
                        (currentTool.isStroke && currentTool != Tool.HIGHLIGHTER && currentTool != Tool.CALLIGRAPHY && currentTool != Tool.DASHED && !isWandActive)
                    FigStylusPenItem(
                        drawableRes = if (isDark) R.drawable.fig_tool_fineliner else R.drawable.fig_tool_fineliner_light,
                        contentDescription = "Fineliner",
                        yOffset = 76.dp,
                        isActive = isFinelinerActive,
                        width = 57.5.dp,
                        height = 19.dp,
                        restingX = (-13.84).dp,
                        activeX = 1.56.dp,
                        onClick = { onToolClick(Tool.PEN) },
                    )

                    // Pen 2: Fountain Pen (y = 107.dp)
                    FigStylusPenItem(
                        drawableRes = if (isDark) R.drawable.fig_tool_fountain_pen else R.drawable.fig_tool_fountain_pen_light,
                        contentDescription = "Fountain Pen",
                        yOffset = 107.dp,
                        isActive = (currentTool == Tool.CALLIGRAPHY && !isWandActive),
                        width = 60.dp,
                        height = 20.dp,
                        restingX = (-13.12).dp,
                        activeX = 2.28.dp,
                        onClick = { onToolClick(Tool.CALLIGRAPHY) },
                    )

                    // Pen 3: Highlighter (y = 140.dp)
                    FigStylusPenItem(
                        drawableRes = if (isDark) R.drawable.fig_tool_highlighter else R.drawable.fig_tool_highlighter_light,
                        contentDescription = "Highlighter",
                        yOffset = 140.dp,
                        isActive = (currentTool == Tool.HIGHLIGHTER && !isWandActive),
                        width = 56.dp,
                        height = 17.dp,
                        restingX = (-13.4).dp,
                        activeX = 2.dp,
                        onClick = { onToolClick(Tool.HIGHLIGHTER) },
                    )

                    // Pen 4: Red Nib (y = 172.dp)
                    FigStylusPenItem(
                        drawableRes = if (isDark) R.drawable.fig_tool_red_nib else R.drawable.fig_tool_red_nib_light,
                        contentDescription = "Red Nib",
                        yOffset = 172.dp,
                        isActive = (currentTool == Tool.DASHED && !isWandActive),
                        width = 53.5.dp,
                        height = 21.dp,
                        restingX = (-6.67).dp,
                        activeX = 8.73.dp,
                        onClick = { onToolClick(Tool.DASHED) },
                    )

                    // Pen 5: Lasso (y = 203.dp)
                    FigStylusPenItem(
                        drawableRes = if (isDark) R.drawable.fig_tool_lasso else R.drawable.fig_tool_lasso_light,
                        contentDescription = "Lasso",
                        yOffset = 203.dp,
                        isActive = ((currentTool == Tool.LASSO || currentTool == Tool.SELECT) && !isWandActive),
                        width = 57.dp,
                        height = 15.5.dp,
                        restingX = (-9.78).dp,
                        activeX = 5.62.dp,
                        onClick = { onToolClick(Tool.LASSO) },
                    )

                    // Pen 6: Laser (y = 235.dp)
                    FigStylusPenItem(
                        drawableRes = if (isDark) R.drawable.fig_tool_laser else R.drawable.fig_tool_laser_light,
                        contentDescription = "Laser",
                        yOffset = 235.dp,
                        isActive = isWandActive,
                        width = 54.dp,
                        height = 16.dp,
                        restingX = (-8.83).dp,
                        activeX = 6.57.dp,
                        onClick = { onToggleWand() },
                    )

                    // 3. DIVIDER 1 (y = 274.dp)
                    Box(
                        modifier = Modifier
                            .offset(x = 8.dp, y = 274.dp)
                            .size(width = 28.dp, height = 1.5.dp)
                            .background(dividerColor, RoundedCornerShape(0.75.dp)),
                    )

                    // 4. STROKE PRESETS
                    val presets = editor.widthPresets
                    val p1 = presets.getOrElse(0) { 4.0 }
                    val p2 = presets.getOrElse(1) { 7.0 }
                    val p3 = presets.getOrElse(2) { 10.0 }
                    val currentW = editor.activeToolWidth
                    val isStroke1 = kotlin.math.abs(currentW - p1) < 0.35
                    val isStroke2 = kotlin.math.abs(currentW - p2) < 0.35
                    val isStroke3 = kotlin.math.abs(currentW - p3) < 0.35

                    // Dot 1 (Small)
                    Box(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 286.dp)
                            .fillMaxWidth()
                            .height(20.dp)
                            .bouncyClickable {
                                if (isStroke1) onToggleStrokeWidthSlider()
                                else editor.selectWidthPreset(p1)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isStroke1) 4.5.dp else 3.5.dp)
                                .background(if (isStroke1) strokeDotActiveColor else strokeDotInactiveColor, CircleShape),
                        )
                    }

                    // Dot 2 (Medium)
                    Box(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 308.dp)
                            .fillMaxWidth()
                            .height(22.dp)
                            .bouncyClickable {
                                if (isStroke2) onToggleStrokeWidthSlider()
                                else editor.selectWidthPreset(p2)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isStroke2) 6.5.dp else 5.5.dp)
                                .background(if (isStroke2) strokeDotActiveColor else strokeDotInactiveColor, CircleShape),
                        )
                    }

                    // Dot 3 (Large)
                    Box(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 332.dp)
                            .fillMaxWidth()
                            .height(24.dp)
                            .bouncyClickable {
                                if (isStroke3) onToggleStrokeWidthSlider()
                                else editor.selectWidthPreset(p3)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isStroke3) 8.5.dp else 7.5.dp)
                                .background(if (isStroke3) strokeDotActiveColor else strokeDotInactiveColor, CircleShape),
                        )
                    }

                    // 5. DIVIDER 2 (y = 363.dp)
                    Box(
                        modifier = Modifier
                            .offset(x = 8.dp, y = 363.dp)
                            .size(width = 28.dp, height = 1.5.dp)
                            .background(dividerColor, RoundedCornerShape(0.75.dp)),
                    )

                    // 6. UTILITY TOOLS
                    // Import image
                    Box(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 378.dp)
                            .fillMaxWidth()
                            .height(32.dp)
                            .bouncyClickable(onClick = onInsertImage),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fig_action_import_image),
                            contentDescription = "Import Image",
                            colorFilter = ColorFilter.tint(actionIconColor),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Table
                    Box(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 414.dp)
                            .fillMaxWidth()
                            .height(32.dp)
                            .bouncyClickable(onClick = { onToolClick(Tool.TABLE) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fig_action_table),
                            contentDescription = "Table",
                            colorFilter = ColorFilter.tint(actionIconColor),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Shapes
                    Box(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 450.dp)
                            .fillMaxWidth()
                            .height(32.dp)
                            .bouncyClickable(onClick = { onToolClick(Tool.SHAPE) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fig_action_shapes),
                            contentDescription = "Shapes",
                            colorFilter = ColorFilter.tint(actionIconColor),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // 7. DIVIDER 3 (y = 491.dp)
                    Box(
                        modifier = Modifier
                            .offset(x = 8.dp, y = 491.dp)
                            .size(width = 28.dp, height = 1.5.dp)
                            .background(dividerColor, RoundedCornerShape(0.75.dp)),
                    )

                    // 8. COLOR SWATCHES (5 dots)
                    val colors = editor.toolbarColors
                    val activeColorIdx = editor.activeColorIndex.coerceIn(0, 4)
                    val defaultColors = listOf(
                        Color(0xFF000000), // Black
                        Color(0xFF3B82F6), // Blue
                        Color(0xFFEF4444), // Red
                        Color(0xFF10B981), // Green
                        Color(0xFFF59E0B), // Amber
                    )
                    val swatchYPositions = listOf(510.dp, 535.dp, 560.dp, 585.dp, 610.dp)

                    swatchYPositions.forEachIndexed { i, yPos ->
                        val col = if (i < colors.size) colors[i].toComposeColor() else defaultColors.getOrElse(i) { Color(0xFFD9D9D9) }
                        val isSelected = (i == activeColorIdx)
                        Box(
                            modifier = Modifier
                                .offset(x = 0.dp, y = yPos - 3.dp)
                                .fillMaxWidth()
                                .height(24.dp)
                                .bouncyClickable {
                                    if (i < colors.size) {
                                        if (isSelected) onOpenColorPicker(false)
                                        else editor.pickColor(i)
                                    } else {
                                        onOpenColorPicker(true)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, swatchBorderColor, CircleShape)
                                        else if (!isDark && (col.red > 0.85f && col.green > 0.85f && col.blue > 0.85f)) Modifier.border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                        else Modifier
                                    ),
                            )
                        }
                    }

                    // 9. ADD ITEM
                    Box(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 626.dp)
                            .fillMaxWidth()
                            .height(32.dp)
                            .bouncyClickable(onClick = { onOpenColorPicker(true) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fig_action_add_item),
                            contentDescription = "Add Item",
                            colorFilter = ColorFilter.tint(actionIconColor),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else {
                    // ==================== HORIZONTAL ORIENTATION (664dp x 48dp) ====================
                    // 1. TOOL SETTINGS
                    Box(
                        modifier = Modifier
                            .offset(x = 10.dp, y = 0.dp)
                            .width(34.dp)
                            .fillMaxHeight()
                            .bouncyClickable(onClick = onOpenPageStyles),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fig_tool_settings),
                            contentDescription = "Tool Settings",
                            colorFilter = ColorFilter.tint(actionIconColor),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // 2. GROUP 3: 7 STYLUS PENS
                    // Pen 0: Eraser
                    FigStylusPenItemHorizontal(
                        drawableRes = if (isDark) R.drawable.fig_tool_eraser else R.drawable.fig_tool_eraser_light,
                        contentDescription = "Eraser",
                        xOffset = 44.dp,
                        isActive = (currentTool == Tool.ERASER),
                        width = 53.dp,
                        height = 20.dp,
                        restingY = (-12.51).dp,
                        activeY = 0.49.dp,
                        onClick = { onToolClick(Tool.ERASER) },
                    )

                    // Pen 1: Fineliner
                    val isFinelinerActiveH = (currentTool == Tool.PEN && !isWandActive) ||
                        (currentTool.isStroke && currentTool != Tool.HIGHLIGHTER && currentTool != Tool.CALLIGRAPHY && currentTool != Tool.DASHED && !isWandActive)
                    FigStylusPenItemHorizontal(
                        drawableRes = if (isDark) R.drawable.fig_tool_fineliner else R.drawable.fig_tool_fineliner_light,
                        contentDescription = "Fineliner",
                        xOffset = 76.dp,
                        isActive = isFinelinerActiveH,
                        width = 57.5.dp,
                        height = 19.dp,
                        restingY = (-13.77).dp,
                        activeY = (-0.77).dp,
                        onClick = { onToolClick(Tool.PEN) },
                    )

                    // Pen 2: Fountain Pen
                    FigStylusPenItemHorizontal(
                        drawableRes = if (isDark) R.drawable.fig_tool_fountain_pen else R.drawable.fig_tool_fountain_pen_light,
                        contentDescription = "Fountain Pen",
                        xOffset = 107.dp,
                        isActive = (currentTool == Tool.CALLIGRAPHY && !isWandActive),
                        width = 60.dp,
                        height = 20.dp,
                        restingY = (-11.45).dp,
                        activeY = 1.55.dp,
                        onClick = { onToolClick(Tool.CALLIGRAPHY) },
                    )

                    // Pen 3: Highlighter
                    FigStylusPenItemHorizontal(
                        drawableRes = if (isDark) R.drawable.fig_tool_highlighter else R.drawable.fig_tool_highlighter_light,
                        contentDescription = "Highlighter",
                        xOffset = 140.dp,
                        isActive = (currentTool == Tool.HIGHLIGHTER && !isWandActive),
                        width = 56.dp,
                        height = 17.dp,
                        restingY = (-13.91).dp,
                        activeY = (-0.91).dp,
                        onClick = { onToolClick(Tool.HIGHLIGHTER) },
                    )

                    // Pen 4: Red Nib
                    FigStylusPenItemHorizontal(
                        drawableRes = if (isDark) R.drawable.fig_tool_red_nib else R.drawable.fig_tool_red_nib_light,
                        contentDescription = "Red Nib",
                        xOffset = 172.dp,
                        isActive = (currentTool == Tool.DASHED && !isWandActive),
                        width = 53.5.dp,
                        height = 21.dp,
                        restingY = (-8.27).dp,
                        activeY = 4.73.dp,
                        onClick = { onToolClick(Tool.DASHED) },
                    )

                    // Pen 5: Lasso
                    FigStylusPenItemHorizontal(
                        drawableRes = if (isDark) R.drawable.fig_tool_lasso else R.drawable.fig_tool_lasso_light,
                        contentDescription = "Lasso",
                        xOffset = 203.dp,
                        isActive = ((currentTool == Tool.LASSO || currentTool == Tool.SELECT) && !isWandActive),
                        width = 57.dp,
                        height = 15.5.dp,
                        restingY = (-9.74).dp,
                        activeY = 3.26.dp,
                        onClick = { onToolClick(Tool.LASSO) },
                    )

                    // Pen 6: Laser
                    FigStylusPenItemHorizontal(
                        drawableRes = if (isDark) R.drawable.fig_tool_laser else R.drawable.fig_tool_laser_light,
                        contentDescription = "Laser",
                        xOffset = 235.dp,
                        isActive = isWandActive,
                        width = 54.dp,
                        height = 16.dp,
                        restingY = (-10.43).dp,
                        activeY = 2.57.dp,
                        onClick = { onToggleWand() },
                    )

                    // 3. DIVIDER 1
                    Box(
                        modifier = Modifier
                            .offset(x = 274.dp, y = 6.5.dp)
                            .size(width = 1.5.dp, height = 25.dp)
                            .background(dividerColor, RoundedCornerShape(0.75.dp)),
                    )

                    // 4. STROKE PRESETS
                    val presetsH = editor.widthPresets
                    val p1H = presetsH.getOrElse(0) { 4.0 }
                    val p2H = presetsH.getOrElse(1) { 7.0 }
                    val p3H = presetsH.getOrElse(2) { 10.0 }
                    val currentWH = editor.activeToolWidth
                    val isStroke1H = kotlin.math.abs(currentWH - p1H) < 0.35
                    val isStroke2H = kotlin.math.abs(currentWH - p2H) < 0.35
                    val isStroke3H = kotlin.math.abs(currentWH - p3H) < 0.35

                    Box(
                        modifier = Modifier
                            .offset(x = 286.dp, y = 0.dp)
                            .width(20.dp)
                            .fillMaxHeight()
                            .bouncyClickable {
                                if (isStroke1H) onToggleStrokeWidthSlider()
                                else editor.selectWidthPreset(p1H)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isStroke1H) 4.5.dp else 3.5.dp)
                                .background(if (isStroke1H) strokeDotActiveColor else strokeDotInactiveColor, CircleShape),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = 308.dp, y = 0.dp)
                            .width(22.dp)
                            .fillMaxHeight()
                            .bouncyClickable {
                                if (isStroke2H) onToggleStrokeWidthSlider()
                                else editor.selectWidthPreset(p2H)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isStroke2H) 6.5.dp else 5.5.dp)
                                .background(if (isStroke2H) strokeDotActiveColor else strokeDotInactiveColor, CircleShape),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = 332.dp, y = 0.dp)
                            .width(24.dp)
                            .fillMaxHeight()
                            .bouncyClickable {
                                if (isStroke3H) onToggleStrokeWidthSlider()
                                else editor.selectWidthPreset(p3H)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isStroke3H) 8.5.dp else 7.5.dp)
                                .background(if (isStroke3H) strokeDotActiveColor else strokeDotInactiveColor, CircleShape),
                        )
                    }

                    // 5. DIVIDER 2
                    Box(
                        modifier = Modifier
                            .offset(x = 363.dp, y = 6.5.dp)
                            .size(width = 1.5.dp, height = 25.dp)
                            .background(dividerColor, RoundedCornerShape(0.75.dp)),
                    )

                    // 6. UTILITY TOOLS
                    Box(
                        modifier = Modifier
                            .offset(x = 378.dp, y = 0.dp)
                            .width(32.dp)
                            .fillMaxHeight()
                            .bouncyClickable(onClick = onInsertImage),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fig_action_import_image),
                            contentDescription = "Import Image",
                            colorFilter = ColorFilter.tint(actionIconColor),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = 414.dp, y = 0.dp)
                            .width(32.dp)
                            .fillMaxHeight()
                            .bouncyClickable(onClick = { onToolClick(Tool.TABLE) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fig_action_table),
                            contentDescription = "Table",
                            colorFilter = ColorFilter.tint(actionIconColor),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = 450.dp, y = 0.dp)
                            .width(32.dp)
                            .fillMaxHeight()
                            .bouncyClickable(onClick = { onToolClick(Tool.SHAPE) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fig_action_shapes),
                            contentDescription = "Shapes",
                            colorFilter = ColorFilter.tint(actionIconColor),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // 7. DIVIDER 3
                    Box(
                        modifier = Modifier
                            .offset(x = 491.dp, y = 6.5.dp)
                            .size(width = 1.5.dp, height = 25.dp)
                            .background(dividerColor, RoundedCornerShape(0.75.dp)),
                    )

                    // 8. COLOR SWATCHES
                    val colorsH = editor.toolbarColors
                    val activeColorIdxH = editor.activeColorIndex.coerceIn(0, 4)
                    val defaultColorsH = listOf(
                        Color(0xFF000000),
                        Color(0xFF3B82F6),
                        Color(0xFFEF4444),
                        Color(0xFF10B981),
                        Color(0xFFF59E0B),
                    )
                    val swatchXPositions = listOf(510.dp, 535.dp, 560.dp, 585.dp, 610.dp)

                    swatchXPositions.forEachIndexed { i, xPos ->
                        val col = if (i < colorsH.size) colorsH[i].toComposeColor() else defaultColorsH.getOrElse(i) { Color(0xFFD9D9D9) }
                        val isSelected = (i == activeColorIdxH)
                        Box(
                            modifier = Modifier
                                .offset(x = xPos - 3.dp, y = 0.dp)
                                .width(24.dp)
                                .fillMaxHeight()
                                .bouncyClickable {
                                    if (i < colorsH.size) {
                                        if (isSelected) onOpenColorPicker(false)
                                        else editor.pickColor(i)
                                    } else {
                                        onOpenColorPicker(true)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, swatchBorderColor, CircleShape)
                                        else if (!isDark && (col.red > 0.85f && col.green > 0.85f && col.blue > 0.85f)) Modifier.border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                        else Modifier
                                    ),
                            )
                        }
                    }

                    // 9. ADD ITEM
                    Box(
                        modifier = Modifier
                            .offset(x = 626.dp, y = 0.dp)
                            .width(32.dp)
                            .fillMaxHeight()
                            .bouncyClickable(onClick = { onOpenColorPicker(true) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fig_action_add_item),
                            contentDescription = "Add Item",
                            colorFilter = ColorFilter.tint(actionIconColor),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // Tool Popup overlay (anchored dynamically to the active tool on the toolbar)
        if (activePopupTool != null) {
            val popupWidth = 360.dp
            val toolCenterX = when (activePopupTool) {
                Tool.ERASER -> 70.5.dp
                Tool.PEN -> 104.75.dp
                Tool.CALLIGRAPHY -> 137.dp
                Tool.HIGHLIGHTER -> 168.dp
                Tool.DASHED -> 198.75.dp
                Tool.LASSO, Tool.SELECT -> 231.5.dp
                Tool.TABLE -> 430.dp
                Tool.SHAPE -> 466.dp
                Tool.TEXT, Tool.TEXT_BOX -> 466.dp
                else -> 104.75.dp
            }
            val toolCenterY = when (activePopupTool) {
                Tool.ERASER -> 54.dp
                Tool.PEN -> 85.5.dp
                Tool.CALLIGRAPHY -> 117.dp
                Tool.HIGHLIGHTER -> 148.5.dp
                Tool.DASHED -> 182.5.dp
                Tool.LASSO, Tool.SELECT -> 211.dp
                Tool.TABLE -> 430.dp
                Tool.SHAPE -> 466.dp
                Tool.TEXT, Tool.TEXT_BOX -> 466.dp
                else -> 85.5.dp
            }

            val popupOffsetModifier = if (isVertical) {
                val anchorY = (toolCenterY - 180.dp).coerceIn(0.dp, (targetH - 360.dp).coerceAtLeast(0.dp))
                if (dockEdge == RailDockEdge.RIGHT) {
                    Modifier.offset(x = (-popupWidth - 8.dp), y = anchorY)
                } else {
                    Modifier.offset(x = targetW + 8.dp, y = anchorY)
                }
            } else {
                val anchorX = (toolCenterX - popupWidth / 2f).coerceIn(0.dp, (targetW - popupWidth).coerceAtLeast(0.dp))
                if (dockEdge == RailDockEdge.BOTTOM) {
                    Modifier.offset(x = anchorX, y = (-4).dp)
                } else {
                    Modifier.offset(x = anchorX, y = targetH + 4.dp)
                }
            }
            Box(
                modifier = popupOffsetModifier.size(width = popupWidth, height = 1.dp).zIndex(100f),
            ) {
                toolPopup(activePopupTool, true)
            }
        }
    }
}

/** The fixed cell the icon sits in, so revealing a label never moves the glyph. */
private val DockToolSlotSize = 32.dp

/** The gap between the icon and a revealed label, and the pill's trailing padding. */
private val DockLabelGap = 4.dp
private val DockLabelPadding = 10.dp

fun toolGlowColor(slotId: String): Color = when (slotId) {
    "pen" -> Color(0xFF38BDF8)
    "brush" -> Color(0xFFF43F5E)
    "highlighter" -> Color(0xFFFBBF24)
    "eraser" -> Color(0xFFEC4899)
    "select", "lasso" -> Color(0xFF10B981)
    "shapes" -> Color(0xFF8B5CF6)
    "text" -> Color(0xFFA855F7)
    "image" -> Color(0xFFF97316)
    "table" -> Color(0xFF6366F1)
    "wand" -> Color(0xFF06B6D4)
    "lock" -> Color(0xFFFBBF24)
    "camera" -> Color(0xFFF43F5E)
    "share" -> Color(0xFF38BDF8)
    "sidebar" -> Color(0xFF06B6D4)
    else -> Color(0xFF38BDF8)
}

/**
 * A dock tool button.
 */
@Composable
fun DockToolSlot(
    icon: ImageVector,
    activeIcon: ImageVector? = null,
    label: String,
    active: Boolean,
    slotId: String,
    isVertical: Boolean = false,
    glowColor: Color = toolGlowColor(slotId),
    onClick: () -> Unit,
) {
    val isDark = isGlassDark()
    val motion = LocalMotion.current
    val dock = LocalDockSlotBounds.current
    val expanded = active && !isVertical
    val density = LocalDensity.current
    val resolvedLabelStyle = LocalTextStyle.current.merge(
        TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
    )
    val measurer = rememberTextMeasurer()
    val labelWidthPx = remember(label, resolvedLabelStyle) {
        measurer.measure(AnnotatedString(label), resolvedLabelStyle).size.width
    }
    val targetWidth = if (expanded) {
        DockToolSlotSize + DockLabelGap + with(density) { labelWidthPx.toDp() } + DockLabelPadding
    } else {
        DockToolSlotSize
    }
    val width = targetWidth
    val corner by animateDpAsState(
        targetValue = if (active && isVertical) 10.5.dp else if (active) 11.dp else 9.dp,
        animationSpec = motion.travelSettle(),
        label = "dockSlotCorner",
    )
    val shape = RoundedCornerShape(corner)
    val lit = dock?.litOf(slotId, active) ?: active
    val covered = dock?.coverageOf(slotId, if (active) 1f else 0f) ?: if (active) 1f else 0f
    val accentFg = if (isDark) Color(0xFF09090B) else Color(0xFFFFFFFF)
    val unlitTint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val tint by animateColorAsState(
        targetValue = if (lit) accentFg else unlitTint,
        animationSpec = motion.quick(),
        label = "dockSlotTint",
    )
    val labelReveal = dockLabelReveal(covered)
    val labelTint = androidx.compose.ui.graphics.lerp(
        unlitTint,
        accentFg,
        labelReveal,
    )

    val currentIcon = if (active && activeIcon != null) activeIcon else icon
    val isDynamicActive = active && activeIcon != null

    Box(
        modifier = Modifier
            .height(DockToolSlotSize)
            .width(width)
            .clip(shape)
            .reportDockSlotBounds(dock, slotId, active)
            .bouncyClickable(onClick = onClick),
    ) {
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(DockToolSlotSize), contentAlignment = Alignment.Center) {
                AnimatedGlyph(
                    icon = currentIcon,
                    tint = tint,
                    contentDescription = if (expanded) null else label,
                    size = 19.5.dp,
                    isDynamicActive = isDynamicActive,
                    glowColor = glowColor,
                )
            }
            if (!isVertical) {
                Box(
                    Modifier
                        .graphicsLayer { alpha = labelReveal }
                        .padding(start = DockLabelGap, end = DockLabelPadding),
                ) {
                    Text(
                        text = label,
                        color = labelTint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/**
 * A dedicated dock button for Magic Wand (Disappearing Ink) mode.
 *
 * Magic Wand is an independent modifier/mode that operates on top of active inking tools
 * (Pen, Brush, Highlighter, etc.), rather than a mutually exclusive tool slot.
 *
 * When enabled:
 * - It displays a vibrant glowing green aesthetic (green background, green border, shadow glow, and green icon/label).
 * - It does NOT report active to [SegmentTracker], allowing the primary drawing tool (Pen, etc.)
 *   to remain visibly active and selected simultaneously.
 */
@Composable
fun DockWandSlot(
    icon: ImageVector = PnotesDynamicIcons.magicWandInactive,
    activeIcon: ImageVector? = PnotesDynamicIcons.magicWandActive,
    label: String = "Wand",
    active: Boolean,
    isVertical: Boolean = false,
    onClick: () -> Unit,
) {
    val isDark = isGlassDark()
    val motion = LocalMotion.current
    val density = LocalDensity.current
    val expanded = active && !isVertical
    val resolvedLabelStyle = LocalTextStyle.current.merge(
        TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
    )
    val measurer = rememberTextMeasurer()
    val labelWidthPx = remember(label, resolvedLabelStyle) {
        measurer.measure(AnnotatedString(label), resolvedLabelStyle).size.width
    }
    val targetWidth = if (expanded) {
        DockToolSlotSize + DockLabelGap + with(density) { labelWidthPx.toDp() } + DockLabelPadding
    } else {
        DockToolSlotSize
    }
    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = motion.travelSettle(),
        label = "dockWandWidth",
    )
    val corner by animateDpAsState(
        targetValue = if (active) 11.dp else 9.dp,
        animationSpec = motion.travelSettle(),
        label = "dockWandCorner",
    )
    val shape = RoundedCornerShape(corner)

    val wandGreen = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
    val wandGreenBg = if (isDark) Color(0x3822C55E) else Color(0x2816A34A)
    val wandGreenBorder = if (isDark) Color(0xCC4ADE80) else Color(0xE616A34A)
    val unlitTint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)

    val tint by animateColorAsState(
        targetValue = if (active) wandGreen else unlitTint,
        animationSpec = motion.quick(),
        label = "dockWandTint",
    )

    val currentIcon = if (active && activeIcon != null) activeIcon else icon
    val isDynamicActive = active && activeIcon != null

    Box(
        modifier = Modifier
            .height(DockToolSlotSize)
            .width(width)
            .clip(shape)
            .bouncyClickable(onClick = onClick),
    ) {
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(DockToolSlotSize), contentAlignment = Alignment.Center) {
                AnimatedGlyph(
                    icon = currentIcon,
                    tint = tint,
                    contentDescription = if (expanded) null else label,
                    size = 19.5.dp,
                    isDynamicActive = isDynamicActive,
                    glowColor = Color(0xFF4ADE80),
                )
            }
            if (!isVertical && active) {
                Box(
                    Modifier.padding(start = DockLabelGap, end = DockLabelPadding),
                ) {
                    Text(
                        text = label,
                        color = wandGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/**
 * A glyph that changes identity in place — the dock's pen slot turns from a pen into a fountain pen
 * when another pen is picked — cross-fades instead of flickering to the next icon.
 */
@Composable
private fun AnimatedGlyph(
    icon: ImageVector,
    tint: Color,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp,
    isDynamicActive: Boolean = false,
    glowColor: Color = Color.White,
) {
    val motion = LocalMotion.current
    AnimatedContent(
        targetState = icon,
        transitionSpec = {
            (fadeIn(animationSpec = motion.quick()) + scaleIn(
                initialScale = 0.85f,
                animationSpec = motion.quick(),
            )) togetherWith (fadeOut(animationSpec = motion.quick()) + scaleOut(
                targetScale = 0.85f,
                animationSpec = motion.quick(),
            ))
        },
        label = "dockToolGlyph",
    ) { glyph ->
        Box(
            modifier = Modifier.size(size + 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isDynamicActive) {
                // Radiant Colored Luminous Glow Halo behind active glyph
                Box(
                    modifier = Modifier
                        .size(size + 10.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = 0.45f),
                                    glowColor.copy(alpha = 0.15f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = CircleShape,
                        )
                )
            }
            Icon(
                imageVector = glyph,
                contentDescription = contentDescription,
                tint = if (isDynamicActive) Color.Unspecified else tint,
                modifier = Modifier
                    .size(size)
                    .then(
                        if (isDynamicActive) {
                            Modifier.graphicsLayer {
                                scaleX = 1.08f
                                scaleY = 1.08f
                            }
                        } else Modifier
                    ),
            )
        }
    }
}

/**
 * 4. STROKE WIDTH SLIDER CARD
 * Blueprint: s(180, hug) | p(74, 140) | rd(16) | f[rgba(255,255,255,0.96), blur(20)]
 * st[1.dp #FFFFFF] | shadow(#0F172A, 14%, y=8, blur=28)
 */
@Composable
fun StrokeWidthSliderCard(
    editor: Editor,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isGlassDark()
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val range = remember(editor.tool) { ToolConversions.widthRange(editor.tool) }
    val minVal = range.start.toFloat()
    val maxVal = range.endInclusive.toFloat()
    var width by remember(editor.activeToolWidth, editor.tool) {
        mutableFloatStateOf(editor.activeToolWidth.toFloat().coerceIn(minVal, maxVal))
    }
    val widthFormatted = ((width * 10.0).roundToInt() / 10.0)

    val activeInkRgba = editor.controller.inkColor
    val activeColor = activeInkRgba.toComposeColor()

    val presets = remember(editor.tool, minVal, maxVal) {
        if (editor.tool == Tool.HIGHLIGHTER) {
            listOf(10.0f, 16.0f, 24.0f, 32.0f, 48.0f).filter { it in minVal..maxVal }
        } else {
            listOf(1.0f, 2.0f, 4.0f, 8.0f, 14.0f, 20.0f).filter { it in minVal..maxVal }
        }
    }

    val midVal = remember(minVal, maxVal) {
        if (minVal < 1f && maxVal >= 40f) 6.0f else (((minVal + maxVal) / 2f * 10.0).roundToInt() / 10.0f)
    }
    val minFormatted = ((minVal * 10.0).roundToInt() / 10.0)
    val midFormatted = ((midVal * 10.0).roundToInt() / 10.0)
    val maxFormatted = ((maxVal * 10.0).roundToInt() / 10.0)

    Box(
        modifier = modifier
            .width(290.dp)
            .glassmorphic(
                shape = RoundedCornerShape(16.dp),
                elevation = 14.dp,
                borderWidth = 1.2.dp,
                isDark = isDark,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Quick Pen Switcher Row (Directly switch between all 6 pen types)
            if (editor.tool.isStroke) {
                val pens = listOf(
                    Tool.PEN to PnotesIcons.pen,
                    Tool.CALLIGRAPHY to PnotesIcons.fountainPen,
                    Tool.HIGHLIGHTER to PnotesIcons.highlighter,
                    Tool.DASHED to PnotesIcons.dashedPen,
                    Tool.SPEED to PnotesIcons.speedPen,
                    Tool.TAPER to PnotesIcons.taperPen,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    pens.forEach { (penTool, penIcon) ->
                        val isCurrent = editor.tool == penTool
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (isCurrent) Color(0xFF007AFF)
                                    else if (isDark) PnotesDarkSurface2
                                    else PnotesLightSurface2
                                )
                                .border(
                                    1.dp,
                                    if (isCurrent) Color(0xFF007AFF) else if (isDark) PnotesDarkBorder else PnotesLightBorder,
                                    RoundedCornerShape(9.dp)
                                )
                                .bouncyClickable { editor.selectTool(penTool) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = penIcon,
                                contentDescription = penTool.name,
                                tint = if (isCurrent) Color.White else if (isDark) PnotesDarkText2 else PnotesLightText2,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(if (isDark) PnotesDarkBorder else PnotesLightBorder)
                )
            }

            // Live Stroke Preview Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) PnotesDarkSurface2 else PnotesLightSurface2)
                    .border(1.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(30.dp)
                ) {
                    val strokePx = (width * density).coerceIn(1.5f, size.height - 2f)
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Presets Header & Row (Presets visually proportional to actual stroke width)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Quick Presets",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) PnotesDarkText2 else PnotesLightText2,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    presets.forEach { preset ->
                        val isSelected = kotlin.math.abs(width - preset) < 0.25f
                        val dotSize = preset.dp.coerceIn(2.5.dp, 20.dp)
                        val label = if (preset % 1f == 0f) preset.toInt().toString() else preset.toString()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (isSelected) (if (isDark) PnotesDarkElevated else Color(0xFFF0F4FA))
                                    else if (isDark) PnotesDarkSurface2
                                    else PnotesLightSurface2
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF007AFF) else if (isDark) PnotesDarkBorder else PnotesLightBorder,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .bouncyClickable {
                                    width = preset
                                    editor.selectWidthPreset(preset.toDouble())
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier.size(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(dotSize)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF007AFF)
                                            else activeColor
                                        )
                                )
                            }
                            Text(
                                text = label,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) (if (isDark) PnotesDarkText1 else PnotesLightText1) else (if (isDark) PnotesDarkText3 else PnotesLightText3),
                            )
                        }
                    }
                }
            }

            // Header Row: Stroke Width & value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Stroke Width",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) PnotesDarkText1 else PnotesLightText1,
                )
                Text(
                    text = "$widthFormatted pt",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007AFF),
                )
            }

            // Slider
            Slider(
                value = width.coerceIn(minVal, maxVal),
                onValueChange = {
                    val clamped = it.coerceIn(minVal, maxVal)
                    width = clamped
                    editor.selectWidthPreset(clamped.toDouble())
                },
                valueRange = minVal..maxVal,
                modifier = Modifier.height(24.dp),
                colors = PurpleTheme.sliderColors(isDark),
            )

            // Ticks: Min | Mid | Max (Clickable to jump directly to preset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "$minFormatted",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) PnotesDarkText3 else PnotesLightText3,
                    modifier = Modifier.bouncyClickable { width = minVal; editor.selectWidthPreset(minVal.toDouble()) },
                )
                Text(
                    text = "$midFormatted",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) PnotesDarkText1 else PnotesLightText1,
                    modifier = Modifier.bouncyClickable { width = midVal; editor.selectWidthPreset(midVal.toDouble()) },
                )
                Text(
                    text = "$maxFormatted",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) PnotesDarkText3 else PnotesLightText3,
                    modifier = Modifier.bouncyClickable { width = maxVal; editor.selectWidthPreset(maxVal.toDouble()) },
                )
            }
        }
    }
}

/**
 * 5. ERASER SETTINGS CARD
 * Styled to Page 1 Design Tokens
 */
@Composable
fun EraserSettingsCard(
    editor: Editor,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isGlassDark()
    val cfg = editor.toolConfig(Tool.ERASER)
    val isArea = cfg.eraseMode == EraseMode.AREA
    val currentWidth = cfg.baseWidth

    Box(
        modifier = modifier
            .width(280.dp)
            .glassmorphic(
                shape = RoundedCornerShape(16.dp),
                elevation = 14.dp,
                borderWidth = 1.dp,
                isDark = isDark,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Eraser Settings",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) PnotesDarkText1 else PnotesLightText1,
            )

            // Precision Eraser (Area mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isArea) (if (isDark) PnotesDarkElevated else Color(0xFFF0F4FA)) else (if (isDark) PnotesDarkSurface2 else PnotesLightSurface2))
                    .border(
                        1.dp,
                        if (isArea) Color(0xFF007AFF) else (if (isDark) PnotesDarkBorder else PnotesLightBorder),
                        RoundedCornerShape(10.dp)
                    )
                    .bouncyClickable {
                        editor.updateToolConfig(Tool.ERASER, cfg.copy(eraseMode = EraseMode.AREA))
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isArea) {
                    Icon(
                        imageVector = PnotesIcons.check,
                        contentDescription = "Selected",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.5.dp, if (isDark) PnotesDarkText3 else PnotesLightText3, CircleShape)
                    )
                }
                Text(
                    text = "Precision Eraser",
                    fontSize = 12.5.sp,
                    fontWeight = if (isArea) FontWeight.Bold else FontWeight.Medium,
                    color = if (isArea) (if (isDark) PnotesDarkText1 else PnotesLightText1) else (if (isDark) PnotesDarkText2 else PnotesLightText2),
                )
            }

            // Stroke Eraser (Whole)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isArea) (if (isDark) PnotesDarkElevated else Color(0xFFF0F4FA)) else (if (isDark) PnotesDarkSurface2 else PnotesLightSurface2))
                    .border(
                        1.dp,
                        if (!isArea) Color(0xFF007AFF) else (if (isDark) PnotesDarkBorder else PnotesLightBorder),
                        RoundedCornerShape(10.dp)
                    )
                    .bouncyClickable {
                        editor.updateToolConfig(Tool.ERASER, cfg.copy(eraseMode = EraseMode.STROKE))
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!isArea) {
                    Icon(
                        imageVector = PnotesIcons.check,
                        contentDescription = "Selected",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.5.dp, if (isDark) PnotesDarkText3 else PnotesLightText3, CircleShape)
                    )
                }
                Text(
                    text = "Stroke Eraser (Whole)",
                    fontSize = 12.5.sp,
                    fontWeight = if (!isArea) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isArea) (if (isDark) PnotesDarkText1 else PnotesLightText1) else (if (isDark) PnotesDarkText2 else PnotesLightText2),
                )
            }

            // Quick Size Presets (8px, 24px, 64px)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val presets = listOf(8.0 to "8px", 24.0 to "24px", 64.0 to "64px")
                presets.forEach { (presetVal, label) ->
                    val isSelected = kotlin.math.abs(currentWidth - presetVal) < 1.0
                    val presetShape = RoundedCornerShape(9.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(presetShape)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(if (isDark) Color(0xFF1C3A60) else Color(0xFFE1EDFF))
                                        .border(1.dp, Color(0xFF007AFF), presetShape)
                                } else {
                                    Modifier
                                        .background(if (isDark) PnotesDarkSurface2 else PnotesLightSurface2)
                                        .border(0.75.dp, if (isDark) PnotesDarkBorder else PnotesLightBorder, presetShape)
                                }
                            )
                            .bouncyClickable {
                                editor.updateToolConfig(Tool.ERASER, cfg.copy(baseWidth = presetVal))
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF007AFF) else (if (isDark) PnotesDarkText1 else PnotesLightText1),
                        )
                    }
                }
            }

            // 1. Erase Highlighter Only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Highlighter Only",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) PnotesDarkText1 else PnotesLightText1,
                    )
                    Text(
                        text = "Leave handwriting untouched",
                        fontSize = 10.sp,
                        color = if (isDark) PnotesDarkText2 else PnotesLightText2,
                    )
                }
                Switch(
                    checked = cfg.eraseHighlighterOnly,
                    onCheckedChange = { checked ->
                        editor.updateToolConfig(Tool.ERASER, cfg.copy(eraseHighlighterOnly = checked))
                    },
                    colors = PurpleTheme.switchColors(isDark),
                )
            }

            // 2. Erase Shapes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Erase Shapes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) PnotesDarkText1 else PnotesLightText1,
                    )
                    Text(
                        text = "Erase recognized shapes and lines",
                        fontSize = 10.sp,
                        color = if (isDark) PnotesDarkText2 else PnotesLightText2,
                    )
                }
                Switch(
                    checked = cfg.eraseShapes,
                    onCheckedChange = { checked ->
                        editor.updateToolConfig(Tool.ERASER, cfg.copy(eraseShapes = checked))
                    },
                    colors = PurpleTheme.switchColors(isDark),
                )
            }

            // 3. Auto-switch back
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Auto-Switch Back",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) PnotesDarkText1 else PnotesLightText1,
                    )
                    Text(
                        text = "Return to pen after lifting",
                        fontSize = 10.sp,
                        color = if (isDark) PnotesDarkText2 else PnotesLightText2,
                    )
                }
                Switch(
                    checked = cfg.switchBackAfterErase,
                    onCheckedChange = { checked ->
                        editor.updateToolConfig(Tool.ERASER, cfg.copy(switchBackAfterErase = checked))
                    },
                    colors = PurpleTheme.switchColors(isDark),
                )
            }

            // 4. Clear Page Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0x33FF453A) else Color(0x18FF3B30))
                    .border(1.dp, Color(0x40FF453A), RoundedCornerShape(8.dp))
                    .bouncyClickable {
                        editor.clearActivePage()
                        onDismiss()
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = PnotesIcons.trash,
                    contentDescription = "Clear Page",
                    tint = Color(0xFFFF453A),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Clear Current Page",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF453A),
                )
            }
        }
    }
}

/**
 * 6. FLOATING LASSO ACTION BAR (State 3)
 * Blueprint: Frame 106 | s(hug, 29) | p(120, -50) | rd(12) | f[rgba(15,23,42,0.9), blur(16)]
 * Duplicate, Recolor, Flip, Delete
 */
@Composable
fun FloatingLassoActionBar(
    onDuplicate: () -> Unit,
    onRecolor: () -> Unit,
    onFlip: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .shadow(10.dp, RoundedCornerShape(12.dp), spotColor = Color(0x25000000))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE609090B))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Duplicate
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .bouncyClickable(onClick = onDuplicate)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = PnotesIcons.copy,
                contentDescription = "Duplicate",
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "Duplicate",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }

        // Recolor
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .bouncyClickable(onClick = onRecolor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = PnotesIcons.paintBucket,
                contentDescription = "Recolor",
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "Recolor",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }

        // Flip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .bouncyClickable(onClick = onFlip)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = PnotesIcons.arrowsHorizontal,
                contentDescription = "Flip",
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "Flip",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }

        // Delete (Red Trash)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x4DEF4444))
                .bouncyClickable(onClick = onDelete)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PnotesIcons.trash,
                contentDescription = "Delete",
                tint = Color(0xFFF87171),
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/**
 * 7. PAGE STYLE POPOVER — redesigned from FigCraft 'pnotes-new-ui-live' (Page 1, dark).
 * Compact 400dp popover: 11 paper-style swatches (4 per row, live pattern previews),
 * Narrow/Standard/Wide segmented spacing, Faint/Medium/Bold ruling contrast,
 * 10 paper-tone dots. Every tap applies live to the current page; the footer holds
 * only "Set as Notebook Default". Orientation lives in the Page Angle dialog,
 * and the old fine-tune sliders are replaced by segmented tracks.
 */
@Composable
fun PageStylesDialog(
    editor: Editor,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isGlassDark()

    // Design tokens — dark panel #171D28, chips #222A37, accent #007AFF.
    val panelColor = if (isDark) Color(0xFF171D28) else Color(0xFFFFFFFF)
    val chipColor = if (isDark) Color(0xFF222A37) else Color(0xFFF3F6FA)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.13f) else Color(0xFFE1E6EF)
    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF151923)
    val textSecondary = if (isDark) Color(0xFFA6B0C0) else Color(0xFF525D70)
    val textFaint = if (isDark) Color(0xFF788497) else Color(0xFF8993A3)
    val accent = Color(0xFF007AFF)

    val initialStyle = editor.currentPageStyle
    var selectedPattern by remember { mutableStateOf(initialStyle.pattern ?: PagePattern.NONE) }
    var selectedColor by remember { mutableStateOf(initialStyle.pageColor ?: Rgba(250, 246, 238)) }
    var selectedSpacing by remember { mutableDoubleStateOf(initialStyle.spacing ?: PageStyle.DEFAULT_SPACING) }
    var patternOpacity by remember {
        mutableFloatStateOf((initialStyle.patternColor?.a ?: PageStyle.DEFAULT_PATTERN_COLOR.a) / 255f)
    }

    /** Live-apply: every tap lands on the current page immediately (each tap = one undo step). */
    fun applyStyle() {
        editor.setCurrentPageStyle(
            PageStyle(
                pageColor = selectedColor,
                pattern = selectedPattern,
                patternColor = PageStyle.DEFAULT_PATTERN_COLOR.copy(
                    a = (patternOpacity * 255f).roundToInt().coerceIn(0, 255)
                ),
                spacing = selectedSpacing,
            )
        )
    }

    val patterns = remember {
        listOf(
            "Plain" to PagePattern.NONE,
            "Ruled" to PagePattern.LINES,
            "Wide" to PagePattern.LINED_WIDE,
            "Narrow" to PagePattern.LINED_NARROW,
            "Grid" to PagePattern.GRID,
            "Dots" to PagePattern.DOTS,
            "Cornell" to PagePattern.CORNELL,
            "Two Column" to PagePattern.TWO_COLUMN,
            "Checklist" to PagePattern.CHECKLIST,
            "Music" to PagePattern.MUSIC,
            "Storyboard" to PagePattern.STORYBOARD,
        )
    }

    val paperTones = remember {
        listOf(
            TonePreset("Pure White", "#FFFFFF", Rgba(255, 255, 255), Color(0xFFFFFFFF)),
            TonePreset("Warm Ivory", "#FAF6EE", Rgba(250, 246, 238), Color(0xFFFAF6EE)),
            TonePreset("Amber Sand", "#F5EBE1", Rgba(245, 235, 225), Color(0xFFF5EBE1)),
            TonePreset("Sage Green", "#E2F8D8", Rgba(226, 248, 216), Color(0xFFE2F8D8)),
            TonePreset("Ice Blue", "#E0F2FE", Rgba(224, 242, 254), Color(0xFFE0F2FE)),
            TonePreset("Soft Rose", "#FCE7F3", Rgba(252, 231, 243), Color(0xFFFCE7F3)),
            TonePreset("Lilac Frost", "#EDE9FE", Rgba(237, 233, 254), Color(0xFFEDE9FE)),
            TonePreset("Linen Gray", "#F4F6F8", Rgba(244, 246, 248), Color(0xFFF4F6F8)),
            TonePreset("Dark Charcoal", "#2E2E32", Rgba(46, 46, 50), Color(0xFF2E2E32), isDark = true),
            TonePreset("Pure OLED", "#000000", Rgba(0, 0, 0), Color(0xFF000000), isDark = true),
        )
    }
    val activeTone = paperTones.firstOrNull { it.rgba == selectedColor } ?: paperTones[1]

    // Segmented presets; the segment nearest the current value is highlighted, so a page
    // that inherited a custom spacing/opacity still shows a sensible selection.
    val spacingOptions = remember { listOf("Narrow" to 45.0, "Standard" to 64.0, "Wide" to 86.0) }
    val contrastOptions = remember { listOf("Faint" to 0.25f, "Medium" to 0.45f, "Bold" to 0.75f) }
    val spacingIndex = spacingOptions.indices.minByOrNull {
        kotlin.math.abs(spacingOptions[it].second - selectedSpacing)
    } ?: 1
    val contrastIndex = contrastOptions.indices.minByOrNull {
        kotlin.math.abs(contrastOptions[it].second - patternOpacity)
    } ?: 1

    val panelShape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .width(400.dp)
            .heightIn(max = 640.dp)
            .shadow(
                elevation = 20.dp,
                shape = panelShape,
                spotColor = Color(0x66000000),
                ambientColor = if (isDark) Color(0x33000000) else Color(0x14000000),
            )
            .clip(panelShape)
            .background(panelColor)
            .border(1.dp, borderColor, panelShape)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // ============================================================
            // HEADER: Title + Close
            // ============================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Page Style",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(chipColor)
                        .border(0.8.dp, borderColor, CircleShape)
                        .bouncyClickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.close,
                        contentDescription = "Close",
                        tint = textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Scrollable middle — caps gracefully on short screens.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ============================================================
                // PAPER STYLE: 11 pattern swatches, 4 per row
                // ============================================================
                Text(
                    text = "PAPER STYLE",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(9.dp))

                patterns.chunked(4).forEach { rowPatterns ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowPatterns.forEach { (title, pattern) ->
                            val isSelected = selectedPattern == pattern
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(86.dp)
                                    .bouncyClickable {
                                        selectedPattern = pattern
                                        applyStyle()
                                    },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .then(
                                            if (isSelected) {
                                                Modifier.shadow(
                                                    elevation = 8.dp,
                                                    shape = RoundedCornerShape(10.dp),
                                                    ambientColor = Color.Transparent,
                                                    spotColor = Color(0x4D007AFF),
                                                )
                                            } else Modifier
                                        )
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(activeTone.composeColor)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(2.5.dp, accent, RoundedCornerShape(10.dp))
                                            } else {
                                                Modifier.border(
                                                    0.8.dp,
                                                    if (activeTone.isDark) Color.White.copy(alpha = 0.20f) else Color(0x24000000),
                                                    RoundedCornerShape(10.dp),
                                                )
                                            }
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    PatternPreviewGraphic(
                                        pattern = pattern,
                                        selected = isSelected,
                                        paperColor = activeTone.composeColor,
                                        opacity = patternOpacity,
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = title,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) textPrimary else textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(8.dp))

                // ============================================================
                // GRID / LINE SPACING: 3-segment track
                // ============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "GRID / LINE SPACING",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary,
                        letterSpacing = 0.8.sp,
                    )
                    Text(
                        text = "${((selectedSpacing / 9.1) * 10).roundToInt() / 10.0} mm",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
                Spacer(Modifier.height(9.dp))
                StyleSegmentedTrack(
                    options = spacingOptions.map { it.first },
                    selectedIndex = spacingIndex,
                    isDark = isDark,
                    onSelect = { index ->
                        selectedSpacing = spacingOptions[index].second
                        applyStyle()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(18.dp))

                // ============================================================
                // RULING CONTRAST: Faint / Medium / Bold segmented track
                // ============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "RULING CONTRAST",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary,
                        letterSpacing = 0.8.sp,
                    )
                    Text(
                        text = "${(patternOpacity * 100).roundToInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
                Spacer(Modifier.height(9.dp))
                StyleSegmentedTrack(
                    options = contrastOptions.map { it.first },
                    selectedIndex = contrastIndex,
                    isDark = isDark,
                    onSelect = { index ->
                        patternOpacity = contrastOptions[index].second
                        applyStyle()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(18.dp))

                // ============================================================
                // PAPER TONE: 10 dots in one row
                // ============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "PAPER TONE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary,
                        letterSpacing = 0.8.sp,
                    )
                    Text(
                        text = activeTone.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = textFaint,
                    )
                }
                Spacer(Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    paperTones.forEach { tone ->
                        val isSelected = selectedColor == tone.rgba
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.shadow(
                                            elevation = 6.dp,
                                            shape = CircleShape,
                                            ambientColor = Color.Transparent,
                                            spotColor = Color(0x47007AFF),
                                        )
                                    } else Modifier
                                )
                                .clip(CircleShape)
                                .background(tone.composeColor)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, accent, CircleShape)
                                    } else {
                                        Modifier.border(
                                            0.8.dp,
                                            if (tone.isDark) Color.White.copy(alpha = 0.35f) else Color(0x26000000),
                                            CircleShape,
                                        )
                                    }
                                )
                                .bouncyClickable {
                                    selectedColor = tone.rgba
                                    applyStyle()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Canvas(modifier = Modifier.size(14.dp)) {
                                    val tickColor = if (tone.isDark) Color.White else accent
                                    val stroke = 2.dp.toPx()
                                    drawLine(
                                        color = tickColor,
                                        start = Offset(size.width * 0.20f, size.height * 0.55f),
                                        end = Offset(size.width * 0.42f, size.height * 0.76f),
                                        strokeWidth = stroke,
                                        cap = StrokeCap.Round,
                                    )
                                    drawLine(
                                        color = tickColor,
                                        start = Offset(size.width * 0.42f, size.height * 0.76f),
                                        end = Offset(size.width * 0.82f, size.height * 0.26f),
                                        strokeWidth = stroke,
                                        cap = StrokeCap.Round,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ============================================================
            // FOOTER: single right-aligned default action
            // ============================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(chipColor)
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .bouncyClickable {
                            val newStyle = PageStyle(
                                pageColor = selectedColor,
                                pattern = selectedPattern,
                                patternColor = PageStyle.DEFAULT_PATTERN_COLOR.copy(
                                    a = (patternOpacity * 255f).roundToInt().coerceIn(0, 255)
                                ),
                                spacing = selectedSpacing,
                            )
                            editor.setCurrentPageStyle(newStyle)
                            editor.setDocumentStyle(newStyle)
                            editor.saveNewNoteStyle(newStyle)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Set as Notebook Default",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
            }
        }
    }
}

/** Full-width 3-segment track (h36, r18) with an accent active pill (r15) — redesign token style. */
@Composable
private fun StyleSegmentedTrack(
    options: List<String>,
    selectedIndex: Int,
    isDark: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackShape = RoundedCornerShape(18.dp)
    val accent = Color(0xFF007AFF)
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(trackShape)
            .background(if (isDark) Color(0xFF222A37) else Color(0xFFF3F6FA))
            .border(0.8.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE1E6EF), trackShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (active) {
                            Modifier.shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(15.dp),
                                ambientColor = Color.Transparent,
                                spotColor = Color(0x4D007AFF),
                            )
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(15.dp))
                    .then(if (active) Modifier.background(accent) else Modifier)
                    .bouncyClickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) Color.White else (if (isDark) Color(0xFFA6B0C0) else Color(0xFF525D70)),
                    maxLines = 1,
                )
            }
        }
    }
}

private data class TonePreset(
    val name: String,
    val hexLabel: String,
    val rgba: Rgba,
    val composeColor: Color,
    val isDark: Boolean = false,
)

@Composable
private fun PatternPreviewGraphic(
    pattern: PagePattern,
    selected: Boolean,
    paperColor: Color,
    opacity: Float,
) {
    val isDarkPaper = (paperColor.red * 0.299 + paperColor.green * 0.587 + paperColor.blue * 0.114) < 0.45
    val baseLineColor = if (isDarkPaper) Color.White else Color(0xFF27272A)
    val tint = baseLineColor.copy(alpha = (opacity * if (selected) 1.0f else 0.75f).coerceIn(0.20f, 0.95f))

    Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
        val w = size.width
        val h = size.height
        when (pattern) {
            PagePattern.NONE -> {
                // Blank clean paper
            }
            PagePattern.LINES -> {
                val step = h / 5f
                for (i in 1..4) {
                    drawLine(tint, Offset(2f, i * step), Offset(w - 2f, i * step), strokeWidth = 1f)
                }
            }
            PagePattern.LINED_WIDE -> {
                val step = h / 4f
                for (i in 1..3) {
                    drawLine(tint, Offset(2f, i * step), Offset(w - 2f, i * step), strokeWidth = 1f)
                }
            }
            PagePattern.LINED_NARROW -> {
                val step = h / 7f
                for (i in 1..6) {
                    drawLine(tint, Offset(2f, i * step), Offset(w - 2f, i * step), strokeWidth = 0.85f)
                }
            }
            PagePattern.GRID -> {
                val stepX = w / 5f
                val stepY = h / 4f
                for (i in 1..4) {
                    drawLine(tint, Offset(i * stepX, 2f), Offset(i * stepX, h - 2f), strokeWidth = 0.8f)
                }
                for (j in 1..3) {
                    drawLine(tint, Offset(2f, j * stepY), Offset(w - 2f, j * stepY), strokeWidth = 0.8f)
                }
            }
            PagePattern.DOTS -> {
                val stepX = w / 5f
                val stepY = h / 4f
                for (x in 1..4) {
                    for (y in 1..3) {
                        drawCircle(tint, radius = 1.2f, center = Offset(x * stepX, y * stepY))
                    }
                }
            }
            PagePattern.CORNELL -> {
                val cueX = w * 0.30f
                val summaryY = h * 0.76f
                val headerY = h * 0.20f
                drawLine(tint, Offset(2f, headerY), Offset(w - 2f, headerY), strokeWidth = 1.1f)
                drawLine(tint, Offset(cueX, headerY), Offset(cueX, summaryY), strokeWidth = 1.1f)
                drawLine(tint, Offset(2f, summaryY), Offset(w - 2f, summaryY), strokeWidth = 1.1f)
                val step = (summaryY - headerY) / 3f
                for (i in 1..2) {
                    val y = headerY + i * step
                    drawLine(tint, Offset(cueX, y), Offset(w - 2f, y), strokeWidth = 0.8f)
                }
            }
            PagePattern.TWO_COLUMN -> {
                val midX = w * 0.5f
                drawLine(tint, Offset(midX, 2f), Offset(midX, h - 2f), strokeWidth = 1f)
                val step = h / 5f
                for (i in 1..4) {
                    val y = i * step
                    drawLine(tint, Offset(2f, y), Offset(midX - 3f, y), strokeWidth = 0.8f)
                    drawLine(tint, Offset(midX + 3f, y), Offset(w - 2f, y), strokeWidth = 0.8f)
                }
            }
            PagePattern.CHECKLIST -> {
                val step = h / 4f
                for (i in 1..3) {
                    val y = i * step
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(3f, y - 3.5f),
                        size = androidx.compose.ui.geometry.Size(7f, 7f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(0.9f)
                    )
                    drawLine(tint, Offset(14f, y), Offset(w - 2f, y), strokeWidth = 0.9f)
                }
            }
            PagePattern.MUSIC -> {
                val staveCenter = h * 0.5f
                val spacing = 2.5f
                for (i in -2..2) {
                    val y = staveCenter + i * spacing
                    drawLine(tint, Offset(2f, y), Offset(w - 2f, y), strokeWidth = 0.8f)
                }
            }
            PagePattern.STORYBOARD -> {
                val frameW = (w - 10f) / 2f
                val frameH = h * 0.65f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(3f, 3f),
                    size = androidx.compose.ui.geometry.Size(frameW, frameH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f, 2.5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(0.9f)
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(7f + frameW, 3f),
                    size = androidx.compose.ui.geometry.Size(frameW, frameH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f, 2.5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(0.9f)
                )
                drawLine(tint, Offset(3f, h - 5f), Offset(3f + frameW, h - 5f), strokeWidth = 0.8f)
                drawLine(tint, Offset(7f + frameW, h - 5f), Offset(w - 3f, h - 5f), strokeWidth = 0.8f)
            }
        }
    }
}

/**
 * 8. RIGHT-SIDE PAGE MANAGER DRAWER
 * Blueprint: s(220, fill) | rd(0) | f[rgba(248,250,252,0.75), blur(20)]
 * st[left 1.dp #FFFFFF] | shadow(#0F172A, 3%, x=-2, blur=8)
 */
@Composable
fun PnotesPageManager(
    editor: Editor,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isGlassDark()
    val listState = rememberLazyListState()
    val pages = remember(editor.contentVersion) { editor.pagesSnapshot() }
    var selectedTab by remember { mutableStateOf(0) } // 0 = All Pages, 1 = Bookmarks

    LaunchedEffect(editor.pageIndex) {
        listState.scrollToItem(editor.pageIndex)
    }

    val settled by produceState(initialValue = true, listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .drop(1)
            .collectLatest {
                value = false
                delay(150)
                value = true
            }
    }

    val displayedPages = remember(pages, selectedTab, editor.bookmarks) {
        if (selectedTab == 1) {
            pages.mapIndexed { idx, page -> idx to page }.filter { (idx, _) -> editor.bookmarks.any { it.page == idx } }
        } else {
            pages.mapIndexed { idx, page -> idx to page }
        }
    }

    Box(
        modifier = modifier
            .width(210.dp)
            .fillMaxHeight()
            .glassmorphic(
                shape = androidx.compose.ui.graphics.RectangleShape,
                elevation = 16.dp,
                borderWidth = 1.2.dp,
                isDark = isDark,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header: Icon cards + "PAGE MANAGER" + Add Page Button + Close Button
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
                        imageVector = PnotesIcons.cards,
                        contentDescription = "Page Manager",
                        tint = if (isDark) Color.White else Color(0xFF09090B),
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "PAGE MANAGER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFA1A1AA) else SlateMuted,
                        letterSpacing = 0.55.sp,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // "+" Add Page Button (26x26)
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE4E4E7), RoundedCornerShape(6.dp))
                            .bouncyClickable { editor.addPage() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.plus,
                            contentDescription = "Add Page",
                            tint = if (isDark) Color.White else Color(0xFF09090B),
                            modifier = Modifier.size(13.dp),
                        )
                    }

                    // "X" Close Button (26x26)
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x0A000000))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE4E4E7), RoundedCornerShape(6.dp))
                            .bouncyClickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFFD4D4D8) else SlateDark,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }

            // Top Segmented Filter: [ All Pages | Bookmarks ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isDark) Color(0xFF1B1B20) else Color(0xFFF4F4F5))
                    .border(0.6.dp, if (isDark) Color(0xFF2E2E36) else Color(0xFFE4E4E7), RoundedCornerShape(9.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val bookmarkedCount = pages.indices.count { idx -> editor.bookmarks.any { it.page == idx } }

                // Tab 0: All Pages
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .then(
                            if (selectedTab == 0) {
                                Modifier
                                    .shadow(2.dp, RoundedCornerShape(7.dp), spotColor = Color(0x25000000))
                                    .background(if (isDark) Color(0xFF2A2A32) else Color.White)
                            } else Modifier
                        )
                        .clickable { selectedTab = 0 },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "All (${pages.size})",
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)),
                    )
                }

                // Tab 1: Bookmarks
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .then(
                            if (selectedTab == 1) {
                                Modifier
                                    .shadow(2.dp, RoundedCornerShape(7.dp), spotColor = Color(0x25000000))
                                    .background(if (isDark) Color(0xFF2A2A32) else Color.White)
                            } else Modifier
                        )
                        .clickable { selectedTab = 1 },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = PnotesIcons.bookmarkFilled,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = if (selectedTab == 1) (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)) else (if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)),
                        )
                        Text(
                            text = "Bookmarks ($bookmarkedCount)",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)),
                        )
                    }
                }
            }

            // LazyColumn of Page Cards (fitting full card width!)
            if (displayedPages.isEmpty() && selectedTab == 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = PnotesIcons.bookmark,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF52525B) else Color(0xFFA1A1AA),
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = "No Bookmarks Yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) PnotesDarkText2 else Color(0xFF71717A),
                        )
                        Text(
                            text = "Tap the bookmark icon on any page card to pin it here.",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF71717A) else Color(0xFFA1A1AA),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(displayedPages, key = { (_, page) -> page.uid }) { (index, page) ->
                        val isCurrent = index == editor.pageIndex
                        val bm = editor.bookmarks.firstOrNull { it.page == index }
                        val hasBookmark = bm != null
                        val customTitle = bm?.label?.takeIf { it.isNotBlank() && !it.startsWith("Page ", ignoreCase = true) }
                        val pageTitle = customTitle ?: "Page ${index + 1}"
                        var showMenu by remember { mutableStateOf(false) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = if (isCurrent) 6.dp else 2.dp,
                                    shape = RoundedCornerShape(10.dp),
                                    spotColor = if (isCurrent) (if (isDark) Color(0x40000000) else Color(0x1F000000)) else Color(0x1A000000),
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE4E4E7)),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .bouncyClickable { editor.goToPage(index) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) {
                                if (isDark) Color(0xFF18181B) else Color.White
                            } else {
                                if (isDark) Color(0xFF18181B).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.75f)
                            },
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                // Top row: Page number pill + Title + Bookmark + Options menu
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f, fill = false),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrent) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color.White.copy(alpha = 0.12f) else Color(0x10000000))),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) (if (isDark) Color(0xFF09090B) else Color.White) else (if (isDark) Color(0xFFD4D4D8) else SlateDark),
                                            )
                                        }
                                        Text(
                                            text = pageTitle,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isCurrent) (if (isDark) Color(0xFFFAFAFA) else SlateDark) else (if (isDark) Color(0xFFA1A1AA) else SlateSub),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (hasBookmark) PnotesIcons.bookmarkFilled else PnotesIcons.bookmark,
                                            contentDescription = "Bookmark",
                                            tint = if (hasBookmark) (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)) else (if (isDark) Color(0xFFA1A1AA) else SlateMuted),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .bouncyClickable {
                                                    if (hasBookmark) editor.removeBookmarkAtPage(index)
                                                    else editor.addBookmarkAt(index, "Page ${index + 1}")
                                                },
                                        )
                                        Box {
                                            Icon(
                                                imageVector = PnotesIcons.moreHorizontal,
                                                contentDescription = "Options",
                                                tint = if (isDark) Color(0xFFA1A1AA) else SlateMuted,
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .bouncyClickable { showMenu = true },
                                            )
                                            PageCardDropdownMenu(
                                                expanded = showMenu,
                                                onDismiss = { showMenu = false },
                                                index = index,
                                                hasBookmark = hasBookmark,
                                                pageCount = pages.size,
                                                editor = editor,
                                            )
                                        }
                                    }
                                }

                                // True Page Thumbnail fitting full card width cleanly!
                                val thumb by produceState<ImageBitmap?>(editor.cachedPageThumbnail(page), page, editor.contentVersion, settled) {
                                    val cached = editor.cachedPageThumbnail(page)
                                    if (cached != null) {
                                        value = cached
                                    } else if (settled) {
                                        value = editor.pageThumbnail(page, 300)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f / 1.4142f)
                                        .shadow(
                                            elevation = if (isCurrent) 4.dp else 1.dp,
                                            shape = RoundedCornerShape(6.dp),
                                            spotColor = if (isDark) Color.Black else Color(0x22000000),
                                        )
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDark) Color(0xFF09090B) else Color.White)
                                        .border(
                                            width = 1.dp,
                                            color = if (isCurrent) (if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF09090B).copy(alpha = 0.5f)) else (if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFE4E4E7)),
                                            shape = RoundedCornerShape(6.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (thumb != null) {
                                        Image(
                                            bitmap = thumb!!,
                                            contentDescription = "Thumbnail for page ${index + 1}",
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        // Realistic paper placeholder with subtle header & ruled lines
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp, 4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(if (isDark) Color(0xFF38BDF8) else Color(0xFF09090B).copy(alpha = 0.7f))
                                            )
                                            repeat(6) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(2.dp)
                                                        .clip(RoundedCornerShape(1.dp))
                                                        .background(if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageCardDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    index: Int,
    hasBookmark: Boolean,
    pageCount: Int,
    editor: Editor,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text("Insert Blank Page After") },
            onClick = {
                onDismiss()
                editor.insertPageAfter(index)
            }
        )
        DropdownMenuItem(
            text = { Text("Duplicate Page") },
            onClick = {
                onDismiss()
                editor.copyPages(listOf(index))
                editor.pastePagesAfter(index)
            }
        )
        DropdownMenuItem(
            text = { Text(if (hasBookmark) "Remove Bookmark" else "Bookmark Page") },
            onClick = {
                onDismiss()
                if (hasBookmark) editor.removeBookmarkAtPage(index)
                else editor.addBookmarkAt(index, "Page ${index + 1}")
            }
        )
        DropdownMenuItem(
            text = { Text("Clear Page") },
            onClick = {
                onDismiss()
                editor.erasePage(index)
            }
        )
        if (pageCount > 1) {
            DropdownMenuItem(
                text = { Text("Delete Page", color = Color(0xFFEF4444)) },
                onClick = {
                    onDismiss()
                    editor.deletePages(listOf(index))
                }
            )
        }
    }
}

@Composable
fun PnotesEmbeddedHeaderTabs(
    tabManager: TabManager,
    title: String,
    onNewTab: () -> Unit,
    onNewCanvasTab: () -> Unit,
    onImportPdf: () -> Unit,
    onOpenBrowseDrawer: () -> Unit,
    onRename: () -> Unit,
    onCloseTab: (NoteTab) -> Unit,
    isDark: Boolean,
    innerPillBg: Color,
    topBorder: Brush,
    textPrimary: Color,
    textBody: Color,
    textMuted: Color,
    modifier: Modifier = Modifier,
) {
    val tabs = tabManager.tabs
    val activeTabId = tabManager.activeTabId
    var showNewTabPopup by remember { mutableStateOf(false) }

    val chromeActiveBg = if (isDark) Color(0xFF1E1E24) else Color(0xFFFFFFFF)
    val chromeTabShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    val activeBorderColor = if (isDark) Color(0xFF38383E) else Color(0xFFCBD5E1)
    val separatorColor = if (isDark) Color(0xFF38383E) else Color(0xFFCBD5E1)
    val plusTint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)

    Row(
        modifier = modifier.height(38.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val isActive = tab.id == activeTabId
            val tabTitle = tab.title.ifBlank { "Untitled Note" }
            val isDirty = tab.isDirty

            // Subtle vertical separator between inactive tabs
            if (index > 0 && !isActive && (index - 1 >= 0 && tabs[index - 1].id != activeTabId)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .width(1.dp)
                        .height(14.dp)
                        .background(separatorColor)
                )
            }

            val tabModifier = Modifier
                .height(34.dp)
                .clip(chromeTabShape)
                .background(if (isActive) chromeActiveBg else Color.Transparent)
                .then(
                    if (isActive) Modifier.border(1.dp, activeBorderColor, chromeTabShape) else Modifier
                )
                .bouncyClickable {
                    if (!isActive) {
                        tabManager.selectTab(tab.id)
                    } else {
                        onRename()
                    }
                }
                .padding(start = 14.dp, end = if (tabs.size > 1) 8.dp else 14.dp)

            Row(
                modifier = tabModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = tabTitle,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isActive) {
                        if (isDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
                    } else {
                        if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeight = 13.sp,
                    ),
                    modifier = Modifier.widthIn(max = 160.dp),
                )
                if (isDirty) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                    )
                }
                if (tabs.size > 1) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .bouncyClickable {
                                onCloseTab(tab)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PnotesIcons.close,
                            contentDescription = "Close Tab",
                            tint = if (isActive) {
                                if (isDark) Color(0xFFCBD5E1) else Color(0xFF0F172A)
                            } else {
                                if (isDark) Color(0x80A1A1AA) else Color(0xFF64748B)
                            },
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }
        }

        // "+" New Tab Button (Chrome style small rounded square right next to tabs)
        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .bouncyClickable { showNewTabPopup = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PnotesIcons.plus,
                    contentDescription = "New Tab Options",
                    tint = plusTint,
                    modifier = Modifier.size(16.dp),
                )
            }

            DropdownMenu(
                expanded = showNewTabPopup,
                onDismissRequest = { showNewTabPopup = false },
                shape = RoundedCornerShape(14.dp),
            ) {
                DropdownMenuItem(
                    text = { Text("New Note Page", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White else Color(0xFF09090B)) },
                    leadingIcon = { Icon(PnotesIcons.file, null, tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A), modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showNewTabPopup = false
                        onNewTab()
                    }
                )
                DropdownMenuItem(
                    text = { Text("New Infinite Canvas", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White else Color(0xFF09090B)) },
                    leadingIcon = { Icon(PnotesIcons.grid, null, tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A), modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showNewTabPopup = false
                        onNewCanvasTab()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Import PDF Document", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White else Color(0xFF09090B)) },
                    leadingIcon = { Icon(PnotesIcons.copy, null, tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A), modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showNewTabPopup = false
                        onImportPdf()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Browse All Documents", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White else Color(0xFF09090B)) },
                    leadingIcon = { Icon(PnotesIcons.folder, null, tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A), modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showNewTabPopup = false
                        onOpenBrowseDrawer()
                    }
                )
            }
        }
    }
}

// =========================================================================
// === ANDROID STUDIO COMPOSE PREVIEWS (INSTANT PREVIEW WITHOUT BUILD) ===
// =========================================================================

@Preview(
    name = "1. Top Bar Refined (No Share, No Saved)",
    showBackground = true,
    backgroundColor = 0xFF1E2430,
    widthDp = 1200,
    heightDp = 76,
)
@Composable
fun PnotesTopBarPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E2430))
    ) {
        PnotesTopBarContent(
            title = "Untitled note_2",
            currentTool = Tool.PEN,
            activeToolWidth = 8.0,
            activeInkColor = Color(0xFF38BDF8),
            pageIndex = 1,
            pageCount = 3,
            sidebarActive = false,
        )
    }
}

@Preview(
    name = "2. Top Bar - Eraser Context",
    showBackground = true,
    backgroundColor = 0xFF1E2430,
    widthDp = 1200,
    heightDp = 76,
)
@Composable
fun PnotesTopBarEraserPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E2430))
    ) {
        PnotesTopBarContent(
            title = "Untitled note_2",
            currentTool = Tool.ERASER,
            activeToolWidth = 16.0,
            activeInkColor = Color(0xFF38BDF8),
            pageIndex = 1,
            pageCount = 3,
            sidebarActive = false,
            isAreaEraser = false,
        )
    }
}

@Preview(
    name = "3. Bottom Dock",
    showBackground = true,
    backgroundColor = 0xFF1E2430,
    widthDp = 600,
    heightDp = 80,
)
@Composable
fun PnotesBottomDockPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E2430))
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        PnotesBottomDockContent(
            currentTool = Tool.PEN,
            isPageStylesOpen = false,
        )
    }
}

@Preview(
    name = "4. Tablet Workspace Full Screen",
    showBackground = true,
    widthDp = 1280,
    heightDp = 800,
)
@Composable
fun PnotesTabletFullPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E2430))
    ) {
        // Top Bar
        PnotesTopBarContent(
            title = "Untitled note_2",
            currentTool = Tool.PEN,
            activeToolWidth = 8.0,
            activeInkColor = Color(0xFF38BDF8),
            pageIndex = 1,
            pageCount = 3,
            sidebarActive = false,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // Bottom Dock
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        ) {
            PnotesBottomDockContent(
                currentTool = Tool.PEN,
                isPageStylesOpen = false,
            )
        }
    }
}

