package com.pnotes.ui

import androidx.activity.compose.BackHandler
import com.pnotes.ui.theme.floatingPill3D
import com.pnotes.ui.theme.elevated3D
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.pnotes.platform.AppStorageDocumentsProvider
import java.io.File
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.R
import com.pnotes.account.AccountStore
import com.pnotes.account.LocalOpenPremium
import com.pnotes.account.LocalOpenSignIn
import com.pnotes.core.model.Rgba
import com.pnotes.settings.ExplorerSortKey
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.ColorMath
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.Palette
import com.pnotes.ui.theme.toComposeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.roundToInt

/** Which pane the backstage shows on the right. */
enum class BackstageView { HOME, PREFERENCES, ABOUT, STYLUS_CALIBRATION, LOGIN }

/** Whether the Home explorer is awaiting a new file/folder name. */
private enum class CreateMode { NONE, FILE, CANVAS, FOLDER }

/** Entries copied/cut in the explorer; [sourceParentDocId] is the folder they came from (for moves). */
private data class ClipItem(val entries: List<BrowseEntry>, val sourceParentDocId: String, val isCut: Boolean)

/** The stem (no extension) offered for a fresh note in [entries], from the filename template. */
private fun nextUntitled(editor: Editor, entries: List<BrowseEntry>?): String =
    editor.newNoteStem(entries.orEmpty().filter { !it.isDir }.map { it.name.lowercase() }.toSet())

/**
 * The full-screen "File" area (the home screen). Shows an in-app file explorer
 * rooted at a folder the user granted, with a command sidebar that is a
 * collapsible left pane on wide screens and a slide-over drawer on phones. "Open…" uses
 * the system picker; "New note" / "Import PDF" land a file in the current folder.
 */
@Composable
fun Backstage(
    editor: Editor,
    view: BackstageView,
    onSelectView: (BackstageView) -> Unit,
    onOpenSystem: () -> Unit,
    onImportPdf: () -> Unit,
    onOpenFile: (String, String?) -> Unit,
    onPickRoot: () -> Unit,
    onShareFile: (String) -> Unit,
    onSaveCopyFile: (String) -> Unit,
    onExportFilePdf: (String) -> Unit,
    /** Home is the app's root: back from here leaves the app rather than dropping into the editor. */
    onExitApp: () -> Unit,
    /** Preferences asked to import a Helix code theme. */
    onImportCodeTheme: () -> Unit = {},
    /** Preferences asked to import a font file. */
    onImportFont: () -> Unit = {},
    /** Two picked files are to be opened together, one per pane of a split view. */
    onOpenSplit: (String, String) -> Unit = { _, _ -> },
) {
    // Home is the studio layout: one full-width header, a permanent sidebar, and the library beside
    // it (see com.pnotes.ui.studio). The other backstage views keep the pane they always had.
    if (view == BackstageView.HOME) {
        com.pnotes.ui.studio.StudioBackstage(
            editor = editor,
            onOpenFile = onOpenFile,
            onImportPdf = onImportPdf,
            onOpenSystem = onOpenSystem,
            onPickRoot = onPickRoot,
            onSelectView = onSelectView,
            onExitApp = onExitApp,
            onShareFile = onShareFile,
            onSaveCopyFile = onSaveCopyFile,
            onExportFilePdf = onExportFilePdf,
            onOpenSplit = onOpenSplit,
        )
        return
    }
    // Below this width the sidebar becomes a slide-over drawer instead of a persistent pane.
    val compact = LocalConfiguration.current.screenWidthDp < COMPACT_WIDTH_DP
    // The backstage is the root of the stack — ordinary base content, not a dialog. The activity
    // window already runs edge-to-edge with the system bars hidden (MainActivity.applyFullscreen).
    BackstageContent(
        editor, compact, view, onSelectView, onOpenSystem, onImportPdf,
        onOpenFile, onPickRoot, onShareFile, onSaveCopyFile, onExportFilePdf, onExitApp, onImportCodeTheme, onImportFont,
        onOpenSplit,
    )
}

/** Width at or above which the sidebar is a persistent pane rather than a drawer. */
private const val COMPACT_WIDTH_DP = 600

/** Open/close animation duration for the sidebar drawer/pane and its scrim. */
private const val SIDEBAR_ANIM_MS = 150

/**
 * The home-first layout: the explorer (or Preferences) fills the screen, with a
 * command sidebar that's a collapsible left pane on wide screens and a slide-over drawer on
 * phones. A `<` collapses it; a hamburger (hidden while open) brings it back.
 */
@Composable
private fun BackstageContent(
    editor: Editor,
    compact: Boolean,
    view: BackstageView,
    onSelectView: (BackstageView) -> Unit,
    onOpenSystem: () -> Unit,
    onImportPdf: () -> Unit,
    onOpenFile: (String, String?) -> Unit,
    onPickRoot: () -> Unit,
    onShareFile: (String) -> Unit,
    onSaveCopyFile: (String) -> Unit,
    onExportFilePdf: (String) -> Unit,
    onExitApp: () -> Unit,
    onImportCodeTheme: () -> Unit,
    onImportFont: () -> Unit,
    onOpenSplit: (String, String) -> Unit,
) {
    val palette = LocalPalette.current
    var createMode by remember { mutableStateOf(CreateMode.NONE) }
    var sidebarOpen by remember { mutableStateOf(false) }
    // Close animates only on a true dismiss ("<", scrim, back); a command swaps the pane
    // already composed underneath, so it closes instantly.
    var animateClose by remember { mutableStateOf(true) }
    val dismissSidebar = { animateClose = true; sidebarOpen = false }

    // A folder is required for these actions; without one, send the user to pick a folder first.
    val selectView: (BackstageView) -> Unit = { v ->
        if (v == BackstageView.HOME) createMode = CreateMode.NONE
        onSelectView(v)
        if (compact) { animateClose = false; sidebarOpen = false }
    }
    val newNote: () -> Unit = {
        if (editor.browseRoot != null) { onSelectView(BackstageView.HOME); createMode = CreateMode.FILE } else onPickRoot()
        if (compact) { animateClose = false; sidebarOpen = false }
    }
    val newCanvas: () -> Unit = {
        if (editor.browseRoot != null) { onSelectView(BackstageView.HOME); createMode = CreateMode.CANVAS } else onPickRoot()
        if (compact) { animateClose = false; sidebarOpen = false }
    }
    val importPdf: () -> Unit = {
        if (editor.browseRoot != null) onImportPdf() else onPickRoot()
        if (compact) { animateClose = false; sidebarOpen = false }
    }
    val openSystem: () -> Unit = {
        if (editor.browseRoot != null) onOpenSystem() else onPickRoot()
        if (compact) { animateClose = false; sidebarOpen = false }
    }

    // Home is the app's root, so it owns every back press while it's up (the editor sits
    // underneath in the same activity — letting the dialog dismiss would just bounce back to
    // it, and the editor's own handler would re-open Home: an endless loop). Back peels off
    // one layer at a time — drawer, Preferences, an in-progress create — and once at the bare
    // Home screen it leaves the app instead. A deeper explorer folder is popped first by the
    // explorer's own (more-nested) handler before this one ever sees the press.
    BackHandler {
        when {
            compact && sidebarOpen -> dismissSidebar()
            // Preferences, About, and Stylus Calibration are sub-pages of Home: back lands on Home rather than leaving the app.
            view == BackstageView.PREFERENCES || view == BackstageView.ABOUT || view == BackstageView.STYLUS_CALIBRATION || view == BackstageView.LOGIN -> selectView(BackstageView.HOME)
            createMode != CreateMode.NONE -> createMode = CreateMode.NONE
            else -> onExitApp()
        }
    }

    // Overlay Layout: BackstageMain always fills full screen (zero layout shrinking/shifting)
    Box(Modifier.fillMaxSize().background(palette.bg.toComposeColor()).imePadding()) {
        BackstageMain(
            Modifier.fillMaxSize(), editor, view, compact, sidebarOpen, { animateClose = true; sidebarOpen = true }, { selectView(BackstageView.HOME) },
            onOpenFile, onPickRoot, importPdf, onShareFile, onSaveCopyFile, onExportFilePdf, createMode, { createMode = it }, onImportCodeTheme, onImportFont,
            onOpenSplit, selectView,
        )

        // Overlay Backdrop Scrim (fades in, tap to dismiss)
        AnimatedVisibility(
            visible = sidebarOpen,
            enter = fadeIn(animationSpec = tween(SIDEBAR_ANIM_MS)),
            exit = if (animateClose) fadeOut(animationSpec = tween(SIDEBAR_ANIM_MS)) else ExitTransition.None,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x77000000))
                    .clickable { dismissSidebar() }
            )
        }

        // Overlay Slide-Over Sidebar (slides over the screen with depth shadow)
        AnimatedVisibility(
            visible = sidebarOpen,
            enter = slideInHorizontally(animationSpec = tween(SIDEBAR_ANIM_MS), initialOffsetX = { -it }),
            exit = if (animateClose) slideOutHorizontally(animationSpec = tween(SIDEBAR_ANIM_MS), targetOffsetX = { -it }) else ExitTransition.None,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            BackstageSidebar(
                Modifier
                    .width(if (compact) 296.dp else 280.dp)
                    .fillMaxHeight(),
                view,
                dismissSidebar,
                selectView,
                newNote,
                newCanvas,
                importPdf,
                openSystem,
            )
        }
    }
}

/** The command sidebar (slide-over drawer over the screen). */
@Composable
private fun BackstageSidebar(
    modifier: Modifier,
    view: BackstageView,
    onCollapse: () -> Unit,
    onSelectView: (BackstageView) -> Unit,
    onNewNote: () -> Unit,
    onNewCanvas: () -> Unit,
    onImportPdf: () -> Unit,
    onOpenSystem: () -> Unit,
) {
    val isDark = LocalPalette.current.isDark
    Column(
        modifier
            .fillMaxHeight()
            .background(PnotesTokens.sidebarBg)
            .border(width = 1.dp, color = PnotesTokens.sidebarBorder, shape = RectangleShape)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_notes_app_logo),
                contentDescription = "PNotes logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Text("PNotes", color = PnotesTokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.5).sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onCollapse, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_sidebar_collapse),
                    contentDescription = "Collapse sidebar",
                    tint = if (isDark) Color.White else Color(0xFF09090B),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Command(
            drawableRes = R.drawable.ic_action_dashboard,
            label = "Home",
            selected = view == BackstageView.HOME,
        ) { onSelectView(BackstageView.HOME) }

        Command(
            icon = PnotesIcons.singleStickPen,
            label = "New note",
        ) { onNewNote() }

        Command(
            drawableRes = R.drawable.ic_action_canvas,
            label = "New canvas",
        ) { onNewCanvas() }

        Command(
            drawableRes = R.drawable.ic_action_import_pdf,
            label = "Import PDF…",
        ) { onImportPdf() }

        Command(
            drawableRes = R.drawable.ic_action_documents,
            label = "Open…",
        ) { onOpenSystem() }

        HorizontalDivider(
            color = PnotesTokens.sidebarBorder,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        )

        Command(
            drawableRes = R.drawable.ic_action_settings,
            label = "Preferences",
            selected = view == BackstageView.PREFERENCES,
        ) { onSelectView(BackstageView.PREFERENCES) }
    }
}

/** The main pane (the explorer, or Preferences); shows a hamburger when the sidebar is hidden. */
@Composable
private fun BackstageMain(
    modifier: Modifier,
    editor: Editor,
    view: BackstageView,
    compact: Boolean,
    sidebarOpen: Boolean,
    onShowSidebar: () -> Unit,
    onBackToHome: () -> Unit,
    onOpenFile: (String, String?) -> Unit,
    onPickRoot: () -> Unit,
    onImportPdf: () -> Unit,
    onShareFile: (String) -> Unit,
    onSaveCopyFile: (String) -> Unit,
    onExportFilePdf: (String) -> Unit,
    createMode: CreateMode,
    onCreateMode: (CreateMode) -> Unit,
    onImportCodeTheme: () -> Unit,
    onImportFont: () -> Unit,
    onOpenSplit: (String, String) -> Unit,
    onSelectView: (BackstageView) -> Unit = {},
) {
    PnotesBackground(modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
            if (view == BackstageView.ABOUT) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 6.dp, end = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (compact) {
                        IconButton(onClick = onBackToHome) {
                            Icon(painterResource(R.drawable.ic_action_sidebar_collapse), "Back to home", tint = PnotesTokens.textPrimary, modifier = Modifier.size(24.dp))
                        }
                    } else if (!sidebarOpen) {
                        IconButton(onClick = onShowSidebar) {
                            Icon(painterResource(R.drawable.ic_action_sidebar_menu), "Show sidebar", tint = PnotesTokens.textPrimary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = view,
                    transitionSpec = {
                        ContentTransform(
                            targetContentEnter = fadeIn(animationSpec = tween(200)) +
                                scaleIn(initialScale = 0.98f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)),
                            initialContentExit = fadeOut(animationSpec = tween(150)) +
                                scaleOut(targetScale = 0.99f, animationSpec = tween(150)),
                        )
                    },
                    label = "backstageViewTransition",
                ) { currentView ->
                    when (currentView) {
                        BackstageView.HOME -> HomePane(
                            editor, onOpenFile, onPickRoot, onImportPdf,
                            onShareFile, onSaveCopyFile, onExportFilePdf, createMode, onCreateMode, sidebarOpen, onShowSidebar,
                            onOpenSplit,
                            onSelectView = { onSelectView(it) },
                        )
                        BackstageView.PREFERENCES -> PreferencesPane(
                            editor, compact, sidebarOpen, onShowSidebar, onBackToHome, onImportCodeTheme, onImportFont,
                            onOpenStylusCalibration = { onSelectView(BackstageView.STYLUS_CALIBRATION) }
                        )
                        BackstageView.STYLUS_CALIBRATION -> com.pnotes.ui.settings.stylus.StylusSettingsScreen(
                            compact = compact,
                            sidebarOpen = sidebarOpen,
                            onShowSidebar = onShowSidebar,
                            onBack = onBackToHome,
                        )
                        BackstageView.ABOUT -> AboutPane()
                        BackstageView.LOGIN -> com.pnotes.ui.auth.LoginHost(
                            onBack = onBackToHome,
                        )
                    }
                }
            }
        }
    }
}

// --- left rail ---

@Composable
private fun Command(
    icon: ImageVector? = null,
    drawableRes: Int? = null,
    iconTint: Color? = null,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val isDark = LocalPalette.current.isDark
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "commandScale",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (selected) {
                    Modifier
                        .clip(shape)
                        .background(PnotesTokens.sidebarSelectedBg)
                        .border(1.dp, PnotesTokens.sidebarSelectedBorder, shape)
                } else {
                    Modifier.clip(shape)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (drawableRes != null) {
            Icon(
                painter = painterResource(drawableRes),
                contentDescription = label,
                tint = iconTint ?: (if (selected) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFFA7ADB8) else Color(0xFF6B7785))),
                modifier = Modifier.size(22.dp),
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint ?: (if (selected) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFFA7ADB8) else Color(0xFF6B7785))),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = if (selected) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFFA7ADB8) else Color(0xFF20242A)),
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun RailDivider() {
    val isDark = LocalPalette.current.isDark
    HorizontalDivider(color = if (isDark) Color(0x18FFFFFF) else Color(0xFFE1E6EB), modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
}

/** One row of the explorer's "Sort by" menu: a check and direction arrow mark the active field. */
@Composable
private fun SortOption(
    label: String,
    key: ExplorerSortKey,
    activeKey: ExplorerSortKey,
    descending: Boolean,
    onPick: (ExplorerSortKey, Boolean) -> Unit,
) {
    val isDark = LocalPalette.current.isDark
    val active = key == activeKey
    val tint = if (active) (if (isDark) Color.White else Color(0xFF09090B)) else (if (isDark) Color(0xFFE4E4E7) else Color(0xFF20242A))
    DropdownMenuItem(
        text = { Text(label, color = tint) },
        leadingIcon = {
            if (active) Icon(PnotesIcons.check, null, tint = tint, modifier = Modifier.size(18.dp))
            else Spacer(Modifier.size(18.dp))
        },
        trailingIcon = if (active) {
            {
                Icon(
                    if (descending) PnotesIcons.arrowDown else PnotesIcons.arrowUp,
                    if (descending) "Descending" else "Ascending",
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else null,
        onClick = { if (active) onPick(key, !descending) else onPick(key, key != ExplorerSortKey.NAME) },
    )
}

// --- home pane: the folder explorer ---

@Composable
private fun HomePane(
    editor: Editor,
    onOpenFile: (String, String?) -> Unit,
    onPickRoot: () -> Unit,
    onImportPdf: () -> Unit,
    onShareFile: (String) -> Unit,
    onSaveCopyFile: (String) -> Unit,
    onExportFilePdf: (String) -> Unit,
    createMode: CreateMode,
    onCreateMode: (CreateMode) -> Unit,
    sidebarOpen: Boolean,
    onShowSidebar: () -> Unit,
    onOpenSplit: (String, String) -> Unit,
    onSelectView: (BackstageView) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        ExplorerSection(
            editor = editor,
            onOpenFile = onOpenFile,
            onPickRoot = onPickRoot,
            onImportPdf = onImportPdf,
            onShareFile = onShareFile,
            onSaveCopyFile = onSaveCopyFile,
            onExportFilePdf = onExportFilePdf,
            createMode = createMode,
            onCreateMode = onCreateMode,
            sidebarOpen = sidebarOpen,
            onShowSidebar = onShowSidebar,
            searchQuery = query,
            onSearchChange = { query = it },
            onOpenSplit = onOpenSplit,
            onSelectView = onSelectView,
        )
    }
}

private val timeFormatLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
}
private val dateFormatLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
}
private val calendarLocal = object : ThreadLocal<Calendar>() {
    override fun initialValue(): Calendar = Calendar.getInstance()
}

private fun formatNoteDate(entry: BrowseEntry): String {
    if (entry.modified <= 0) return "Today, 10:30 AM"
    val cal = calendarLocal.get() ?: Calendar.getInstance()
    cal.timeInMillis = System.currentTimeMillis()
    val nowDay = cal.get(Calendar.DAY_OF_YEAR)
    val nowYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = entry.modified
    val modDay = cal.get(Calendar.DAY_OF_YEAR)
    val modYear = cal.get(Calendar.YEAR)
    val timeFormat = timeFormatLocal.get() ?: SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(entry.modified))
    return when {
        nowYear == modYear && nowDay == modDay -> "Today, $timeStr"
        nowYear == modYear && nowDay - modDay == 1 -> "Yesterday, $timeStr"
        else -> {
            val dateFormat = dateFormatLocal.get() ?: SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
            dateFormat.format(Date(entry.modified))
        }
    }
}

private fun formatRelativeTime(timeMillis: Long): String {
    if (timeMillis <= 0) return "2 min ago"
    val diffSec = (System.currentTimeMillis() - timeMillis) / 1000L
    return when {
        diffSec < 60 -> "Just now"
        diffSec < 3600 -> "${diffSec / 60} min ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        diffSec < 604800 -> "${diffSec / 86400}d ago"
        else -> "Earlier"
    }
}

// --- explorer section ---

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ExplorerSection(
    editor: Editor,
    onOpenFile: (String, String?) -> Unit,
    onPickRoot: () -> Unit,
    onImportPdf: () -> Unit,
    onShareFile: (String) -> Unit,
    onSaveCopyFile: (String) -> Unit,
    onExportFilePdf: (String) -> Unit,
    createMode: CreateMode,
    onCreateMode: (CreateMode) -> Unit,
    sidebarOpen: Boolean = true,
    onShowSidebar: () -> Unit = {},
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    onOpenSplit: (String, String) -> Unit = { _, _ -> },
    onSelectView: (BackstageView) -> Unit = {},
) {
    val palette = LocalPalette.current
    val root = editor.browseRoot
    if (root == null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Text("Choose a folder to keep and browse your notes in.", color = Color(0xFF71717A), fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(PnotesIcons.folder, "Choose folder", Modifier.fillMaxHeight(), onPickRoot)
                PrimaryButton(PnotesIcons.database, "Use App Storage", Modifier.fillMaxHeight()) { editor.useInternalStorage() }
            }
            Spacer(Modifier.weight(1f))
        }
        return
    }

    val scope = rememberCoroutineScope()
    val rootDocId = remember(root) { editor.browseRootDocId(root) }
    val stack = remember(root) { mutableStateListOf<Pair<String, String>>() }
    val currentDocId = if (stack.isEmpty()) rootDocId else stack.last().first
    var refreshKey by remember(root) { mutableStateOf(0) }
    var fieldError by remember(root) { mutableStateOf<String?>(null) }
    var renaming by remember(root) { mutableStateOf<BrowseEntry?>(null) }
    val selection = remember(root) { mutableStateListOf<BrowseEntry>() }
    var clipboard by remember(root) { mutableStateOf<ClipItem?>(null) }
    var pendingDelete by remember(root) { mutableStateOf<List<BrowseEntry>?>(null) }
    var pendingDeleteFolder by remember(root) { mutableStateOf<BrowseEntry?>(null) }
    var showFolderPickerDialog by remember(root) { mutableStateOf(false) }
    var showBatchColorDialog by remember(root) { mutableStateOf(false) }
    var batchColorTargets by remember(root) { mutableStateOf<List<BrowseEntry>>(emptyList()) }
    var folderMenuTarget by remember(root) { mutableStateOf<BrowseEntry?>(null) }
    var noteMenuTarget by remember(root) { mutableStateOf<BrowseEntry?>(null) }
    var pathInfoEntry by remember(root) { mutableStateOf<BrowseEntry?>(null) }
    var pathInfoCustom by remember(root) { mutableStateOf<Pair<String, String>?>(null) }
    var opError by remember(root) { mutableStateOf<String?>(null) }
    var menuOpen by remember(root) { mutableStateOf(false) }
    var sortSubmenu by remember(root) { mutableStateOf(false) }
    LaunchedEffect(menuOpen) { if (!menuOpen) sortSubmenu = false }
    var newMenuOpen by remember(root) { mutableStateOf(false) }
    val rootName by produceState(editor.cachedRootName(root), root) { value = withContext(Dispatchers.IO) { editor.browseRootName(root) } }
    val dismissInteraction = remember { MutableInteractionSource() }
    val pendingImport = editor.pendingImport
    var showCreateMenuDialog by rememberSaveable { mutableStateOf(false) }
    var showNewNoteDialog by rememberSaveable { mutableStateOf(false) }
    val isNewNoteOpen = showNewNoteDialog || createMode == CreateMode.FILE

    val folderBounds = remember(root) { mutableStateMapOf<String, Rect>() }
    val fileBounds = remember(root) { mutableStateMapOf<String, Rect>() }
    var dragItems by remember(root) { mutableStateOf<List<BrowseEntry>>(emptyList()) }
    var dragPos by remember(root) { mutableStateOf<Offset?>(null) }
    var dropTargetUri by remember(root) { mutableStateOf<String?>(null) }
    var pulseUri by remember(root) { mutableStateOf<String?>(null) }
    var boxCoords by remember(root) { mutableStateOf<LayoutCoordinates?>(null) }
    var dragCardSize by remember(root) { mutableStateOf<IntSize?>(null) }
    val gridState = rememberLazyGridState()

    BackHandler(enabled = stack.isNotEmpty()) {
        stack.removeAt(stack.lastIndex)
        selection.clear()
        opError = null
    }
    fun toggleSelect(e: BrowseEntry) {
        val i = selection.indexOfFirst { it.documentUri == e.documentUri }
        if (i >= 0) selection.removeAt(i) else selection.add(e)
    }
    fun updateDropTarget(pos: Offset) {
        dropTargetUri = folderBounds.entries
            .firstOrNull { (uri, r) -> r.contains(pos) && dragItems.none { it.documentUri == uri } }?.key
    }
    val autoScrollBand = with(LocalDensity.current) { 72.dp.toPx() }
    val autoScrollMax = with(LocalDensity.current) { 72.dp.toPx() }
    LaunchedEffect(dragPos != null) {
        while (dragPos != null) {
            val pos = dragPos
            val bc = boxCoords
            if (pos != null && bc != null) {
                val b = bc.boundsInWindow()
                val delta = when {
                    pos.y < b.top + autoScrollBand ->
                        -((b.top + autoScrollBand - pos.y) / autoScrollBand).coerceIn(0f, 1f) * autoScrollMax
                    pos.y > b.bottom - autoScrollBand ->
                        ((pos.y - (b.bottom - autoScrollBand)) / autoScrollBand).coerceIn(0f, 1f) * autoScrollMax
                    else -> 0f
                }
                if (delta != 0f) {
                    gridState.scrollBy(delta)
                    updateDropTarget(pos)
                }
            }
            delay(16L)
        }
    }

    // Listing & All Notes State
    var showAllNotes by rememberSaveable { mutableStateOf(false) }

    val entries by produceState(editor.cachedChildren(root, currentDocId), root, currentDocId, refreshKey, editor.noteOpen) {
        value = withContext(Dispatchers.IO) { editor.browseChildren(root, currentDocId) }
    }
    val vaultAllNotes by produceState<List<BrowseEntry>?>(null, root, rootDocId, refreshKey, editor.noteOpen) {
        value = withContext(Dispatchers.IO) { editor.listAllNotes(root, rootDocId) }
    }
    LaunchedEffect(currentDocId) { onSearchChange("") }
    val query = searchQuery.trim()
    val searching = query.isNotEmpty()
    val searchScopeDocId = if (showAllNotes) rootDocId else currentDocId
    val results by produceState<List<BrowseEntry>?>(emptyList(), root, searchScopeDocId, query, refreshKey, editor.noteOpen) {
        if (!searching) { value = emptyList(); return@produceState }
        value = null
        delay(250)
        value = withContext(Dispatchers.IO) { editor.searchNotes(root, searchScopeDocId, query) }
    }
    val folders = remember(searching, showAllNotes, entries) {
        if (searching || showAllNotes) emptyList() else entries?.filter { it.isDir }.orEmpty()
    }

    // Reactive note counts for each folder
    val folderCounts by produceState(
        emptyMap<String, Editor.FolderCountInfo>(),
        root,
        folders,
        vaultAllNotes,
        refreshKey
    ) {
        value = withContext(Dispatchers.IO) {
            folders.associate { folder ->
                val folderDocId = editor.browseDocId(folder.documentUri)
                folder.documentUri to editor.countFolderContents(root, folderDocId)
            }
        }
    }

    val files = remember(searching, results, showAllNotes, vaultAllNotes, entries, stack) {
        when {
            searching -> results.orEmpty()
            showAllNotes -> vaultAllNotes.orEmpty()
            stack.isEmpty() -> {
                val rootFiles = entries?.filterNot { it.isDir }.orEmpty()
                if (rootFiles.isNotEmpty()) rootFiles else vaultAllNotes.orEmpty()
            }
            else -> entries?.filterNot { it.isDir }.orEmpty()
        }
    }

    var activeFilter by rememberSaveable { mutableStateOf(HomeFilter.ALL) }

    // Source list for categories: use all documents across the vault for accurate category counts
    val allVaultDocuments = remember(vaultAllNotes, files) {
        vaultAllNotes ?: files
    }

    val allCount = allVaultDocuments.size
    val notesCount = allVaultDocuments.count { isNoteDocument(it.name) }
    val canvasesCount = allVaultDocuments.count { isCanvasDocument(it.name) }
    val pdfsCount = allVaultDocuments.count { isPdfDocument(it.name) }
    val favCount = allVaultDocuments.count { editor.isFavorite(it.documentUri) }
    val recentNotesCount = files.size
    val oneWeekAgo = remember { System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L }

    val displayFiles = remember(files, allVaultDocuments, activeFilter, stack, refreshKey) {
        val sourceFiles = if (activeFilter != HomeFilter.ALL && stack.isEmpty()) {
            allVaultDocuments
        } else {
            files
        }
        when (activeFilter) {
            HomeFilter.ALL -> files
            HomeFilter.NOTES -> sourceFiles.filter { isNoteDocument(it.name) }
            HomeFilter.CANVASES -> sourceFiles.filter { isCanvasDocument(it.name) }
            HomeFilter.PDFS -> sourceFiles.filter { isPdfDocument(it.name) }
            HomeFilter.FAVORITES -> sourceFiles.filter { editor.isFavorite(it.documentUri) }
            HomeFilter.RECENT -> sourceFiles.sortedByDescending { it.modified }
        }
    }
    val filesNow = rememberUpdatedState(displayFiles)
    val sourceDocId = rememberUpdatedState(currentDocId)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { boxCoords = it }
            .pointerInput(root) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = try {
                        withTimeout(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val e = awaitPointerEvent()
                                val c = e.changes.firstOrNull { it.id == down.id }
                                if (c == null || !c.pressed || c.isConsumed) return@withTimeout false
                                if ((c.position - down.position).getDistance() > viewConfiguration.touchSlop) return@withTimeout false
                            }
                            @Suppress("UNREACHABLE_CODE") false
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        true
                    }
                    if (!longPress) return@awaitEachGesture
                    val winDown = boxCoords?.localToWindow(down.position) ?: return@awaitEachGesture
                    val hitUri = fileBounds.entries.firstOrNull { it.value.contains(winDown) }?.key
                    if (hitUri == null) return@awaitEachGesture
                    if (selection.none { it.documentUri == hitUri }) {
                        filesNow.value.firstOrNull { it.documentUri == hitUri }?.let {
                            renaming = null; opError = null; selection.add(it)
                        }
                        do {
                            val e = awaitPointerEvent(PointerEventPass.Initial)
                            e.changes.forEach { it.consume() }
                        } while (e.changes.any { it.id == down.id && it.pressed })
                        return@awaitEachGesture
                    }
                    if (selection.any { it.isDir }) {
                        do {
                            val e = awaitPointerEvent(PointerEventPass.Initial)
                            e.changes.forEach { it.consume() }
                        } while (e.changes.any { it.id == down.id && it.pressed })
                        return@awaitEachGesture
                    }
                    val rect = fileBounds[hitUri]
                    dragItems = selection.toList()
                    dragCardSize = rect?.let { IntSize(it.width.roundToInt(), it.height.roundToInt()) }
                    var pos = winDown
                    dragPos = pos
                    updateDropTarget(pos)
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                        val delta = c.positionChange()
                        c.consume()
                        if (!c.pressed) break
                        pos += delta
                        dragPos = pos
                        updateDropTarget(pos)
                    }
                    val target = dropTargetUri
                    if (target != null && target != sourceDocId.value) {
                        val items = dragItems
                        dragItems = emptyList(); dragPos = null; dropTargetUri = null
                        selection.clear(); opError = null
                        val srcParent = sourceDocId.value
                        scope.launch {
                            val targetId = editor.browseDocId(target)
                            val ok = withContext(Dispatchers.IO) {
                                var allOk = true
                                items.forEach { item ->
                                    if (item.documentUri != target &&
                                        !editor.moveDocumentInto(root, item.documentUri, srcParent, targetId)) allOk = false
                                }
                                allOk
                            }
                            if (ok) { pulseUri = target; refreshKey++ } else opError = "Couldn’t move all items."
                        }
                    } else {
                        dragItems = emptyList(); dragPos = null; dropTargetUri = null
                    }
                }
            }
            .then(
                if (selection.isNotEmpty()) Modifier.clickable(interactionSource = dismissInteraction, indication = null) { selection.clear() } else Modifier,
            ),
    ) {
        val availableWidth = maxWidth
        val isPortrait = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        val gridColumns = when {
            isPortrait -> when {
                availableWidth >= 800.dp -> 6
                availableWidth >= 620.dp -> 5
                availableWidth >= 440.dp -> 4
                else -> 3
            }
            else -> when {
                availableWidth >= 1200.dp -> 8
                availableWidth >= 950.dp -> 7
                availableWidth >= 750.dp -> 6
                availableWidth >= 550.dp -> 5
                else -> 4
            }
        }
        val isCompactWidth = availableWidth < 600.dp

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // 1. TOP APP BAR (PNotes Prototype Top Bar with Branding, Search, New Note, Import, Grid/List Toggle, and Profile Menu)
            var workspaceMenuOpen by rememberSaveable { mutableStateOf(false) }
            var userPopoverOpen by rememberSaveable { mutableStateOf(false) }
            val ctx = LocalContext.current
            val isDark = LocalPalette.current.isDark
            val menuBg = if (isDark) Color(0xFF454549) else Color(0xFFFFFFFF)
            val menuBorder = if (isDark) Color(0xFF58585E) else Color(0xFFE4E4E7)
            var isGridView by rememberSaveable { mutableStateOf(true) }
            // The avatar's own section of this menu: who is signed in, and the way in or out.
            val accountStore = remember { AccountStore.get(ctx) }
            val accountState by accountStore.state.collectAsState()
            val openSignIn = LocalOpenSignIn.current
            val openPremium = LocalOpenPremium.current

            Box(Modifier.fillMaxWidth()) {
                PNotesPrototypeTopBar(
                    searchQuery = searchQuery,
                    onSearchChange = onSearchChange,
                    onShowSidebar = onShowSidebar,
                    onNewNote = {
                        if (editor.preferences.dontShowCreateDialog) {
                            showNewNoteDialog = true
                        } else {
                            showCreateMenuDialog = true
                        }
                    },
                    onImport = onImportPdf,
                    isGridView = isGridView,
                    onToggleGridView = { isGridView = !isGridView },
                    workspaceName = rootName ?: "Vivek's Workspace",
                    onWorkspaceClick = { workspaceMenuOpen = true },
                    onAvatarClick = { userPopoverOpen = true },
                    avatarLabel = accountState.avatarLabel,
                    compact = isCompactWidth,
                    sidebarOpen = sidebarOpen,
                )

                // Workspace Folder Navigation Dropdown
                DropdownMenu(
                    expanded = workspaceMenuOpen,
                    onDismissRequest = { workspaceMenuOpen = false },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = if (isCompactWidth) 40.dp else 120.dp)
                        .widthIn(min = 200.dp),
                ) {
                    DropdownMenuItem(
                        text = { Text("Show real path", fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_action_info), null, modifier = Modifier.size(18.dp), tint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF52525B)) },
                        onClick = {
                            workspaceMenuOpen = false
                            val resolved = resolveRealPath(ctx, root, rootDocId)
                            pathInfoCustom = Pair(rootName ?: "Root Folder", resolved)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Change folder", fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_action_edit), null, modifier = Modifier.size(18.dp), tint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF52525B)) },
                        onClick = { workspaceMenuOpen = false; onPickRoot() },
                    )
                    DropdownMenuItem(
                        text = { Text("Forget folder", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFFE5534B)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_action_trash), null, tint = Color(0xFFE5534B), modifier = Modifier.size(18.dp)) },
                        onClick = { workspaceMenuOpen = false; editor.clearBrowseRoot() },
                    )
                }

                // User Popover Menu Card matching prototype exactly (Guest, Signed-In Free, Lifetime Pro)
                UserPopoverMenu(
                    expanded = userPopoverOpen,
                    onDismissRequest = { userPopoverOpen = false },
                    accountState = accountState,
                    notesCount = files.size,
                    maxFreeNotes = 10,
                    onSignIn = {
                        userPopoverOpen = false
                        openSignIn()
                    },
                    onUpgrade = {
                        userPopoverOpen = false
                        openPremium()
                    },
                    onRestorePurchase = {
                        userPopoverOpen = false
                        openPremium()
                    },
                    onStylusStudio = {
                        userPopoverOpen = false
                        onSelectView(BackstageView.STYLUS_CALIBRATION)
                    },
                    onPaperDefaults = {
                        userPopoverOpen = false
                        showNewNoteDialog = true
                    },
                    onCanvasPresentation = {
                        userPopoverOpen = false
                        android.widget.Toast.makeText(ctx, "Canvas Presentation available inside canvas", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onAppSettings = {
                        userPopoverOpen = false
                        onSelectView(BackstageView.PREFERENCES)
                    },
                    onExportVault = {
                        userPopoverOpen = false
                        android.widget.Toast.makeText(ctx, "Exporting full vault backup (.pnote)...", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onManageAccount = {
                        userPopoverOpen = false
                        openSignIn()
                    },
                    onSignOut = {
                        userPopoverOpen = false
                        accountStore.signOut()
                    },
                )
            }

            var sortMenuOpen by remember { mutableStateOf(false) }

            val currentSortKey = editor.explorerSortKey
            val sortLabel = when (currentSortKey) {
                ExplorerSortKey.NAME -> "Name"
                ExplorerSortKey.SIZE -> "Size"
                ExplorerSortKey.MODIFIED -> "Last modified"
            }

            // 2. UNIFIED SCROLLABLE HOME CONTENT (Filter Tabs, Folders, and Notes)
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isGridView) gridColumns else 1),
                state = gridState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = if (isCompactWidth) 16.dp else 24.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isGridView) 10.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(if (isGridView) 10.dp else 6.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 96.dp),
            ) {
                // ==========================================
                // 2A. FILTER TABS ROW & SORT DROPDOWN
                // ==========================================
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        PNotesPrototypeFilterTabsRow(
                            activeTab = activeFilter,
                            onTabSelect = { filter ->
                                activeFilter = if (activeFilter == filter && filter != HomeFilter.ALL) HomeFilter.ALL else filter
                            },
                            allCount = allCount,
                            notesCount = notesCount,
                            canvasesCount = canvasesCount,
                            docsCount = pdfsCount,
                            favoritesCount = favCount,
                            sortKey = currentSortKey,
                            sortAscending = !editor.explorerSortDescending,
                            onSortChange = { key, ascending ->
                                editor.setExplorerSort(key, !ascending)
                                refreshKey++
                            },
                            sortLabel = sortLabel,
                            onSortClick = { sortMenuOpen = true },
                        )

                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd),
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Last modified", fontWeight = if (currentSortKey == ExplorerSortKey.MODIFIED) FontWeight.Bold else FontWeight.Normal)
                                        if (currentSortKey == ExplorerSortKey.MODIFIED) {
                                            Spacer(Modifier.width(8.dp))
                                            Icon(PnotesIcons.check, null, tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    sortMenuOpen = false
                                    editor.setExplorerSort(ExplorerSortKey.MODIFIED, true)
                                    refreshKey++
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Name", fontWeight = if (currentSortKey == ExplorerSortKey.NAME) FontWeight.Bold else FontWeight.Normal)
                                        if (currentSortKey == ExplorerSortKey.NAME) {
                                            Spacer(Modifier.width(8.dp))
                                            Icon(PnotesIcons.check, null, tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    sortMenuOpen = false
                                    editor.setExplorerSort(ExplorerSortKey.NAME, false)
                                    refreshKey++
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Size", fontWeight = if (currentSortKey == ExplorerSortKey.SIZE) FontWeight.Bold else FontWeight.Normal)
                                        if (currentSortKey == ExplorerSortKey.SIZE) {
                                            Spacer(Modifier.width(8.dp))
                                            Icon(PnotesIcons.check, null, tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    sortMenuOpen = false
                                    editor.setExplorerSort(ExplorerSortKey.SIZE, true)
                                    refreshKey++
                                },
                            )
                        }
                    }
                }

                if (opError != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(opError!!, color = Color(0xFFE5534B), fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }

                // ==========================================
                // 2B. FOLDERS SECTION
                // ==========================================
                if (!searching && !showAllNotes && folders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                            PNotesFoldersHeader(
                                folderCount = folders.size,
                                onNewFolder = {
                                    if (editor.preferences.dontShowCreateDialog) {
                                        onCreateMode(CreateMode.FOLDER)
                                    } else {
                                        showCreateMenuDialog = true
                                    }
                                },
                                isGridView = isGridView,
                                onToggleGridView = { isGridView = !isGridView },
                                showViewToggle = false,
                                modifier = Modifier.padding(bottom = 10.dp),
                            )

                            val folderCols = if (isCompactWidth) 1 else if (availableWidth >= 900.dp) 4 else if (availableWidth >= 600.dp) 3 else 2
                            val chunkedFolders = folders.chunked(folderCols)

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                chunkedFolders.forEach { rowFolders ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        rowFolders.forEach { entry ->
                                            val isMenuOpen = folderMenuTarget?.documentUri == entry.documentUri
                                            val countInfo = folderCounts[entry.documentUri]
                                                ?: editor.cachedChildren(root, editor.browseDocId(entry.documentUri))?.let { ch ->
                                                    val n = ch.count { !it.isDir && (com.pnotes.core.util.DocumentKind.isDocument(it.name) || it.name.endsWith(".pdf", ignoreCase = true)) }
                                                    val f = ch.count { it.isDir }
                                                    Editor.FolderCountInfo(n, f)
                                                } ?: Editor.FolderCountInfo(0, 0)
                                            Box(modifier = Modifier.weight(1f)) {
                                                PNotesPrototypeFolderCard(
                                                    folderName = entryLabel(entry),
                                                    noteCount = countInfo.notes,
                                                    countLabel = countInfo.label,
                                                    timeAgo = "",
                                                    colorTag = entry.color?.let { codeOutline(it, isDark).toComposeColor() },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .onGloballyPositioned { r -> folderBounds[entry.documentUri] = r.boundsInWindow() },
                                                    onClick = {
                                                        opError = null
                                                        if (selection.isNotEmpty()) toggleSelect(entry)
                                                        else stack.add(editor.browseDocId(entry.documentUri) to entry.name)
                                                    },
                                                    onMenuClick = { folderMenuTarget = entry },
                                                    menuContent = {
                                                        DropdownMenu(
                                                            expanded = isMenuOpen,
                                                            onDismissRequest = { folderMenuTarget = null },
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier
                                                                .widthIn(min = 180.dp),
                                                        ) {
                                                            DropdownMenuItem(
                                                                text = { Text("Show real path", fontSize = 13.sp) },
                                                                leadingIcon = { Icon(painterResource(R.drawable.ic_action_info), null, modifier = Modifier.size(16.dp), tint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF52525B)) },
                                                                onClick = {
                                                                    folderMenuTarget = null
                                                                    pathInfoEntry = entry
                                                                },
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("Rename", fontSize = 13.sp) },
                                                                leadingIcon = { Icon(painterResource(R.drawable.ic_action_edit), null, modifier = Modifier.size(16.dp), tint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF52525B)) },
                                                                onClick = {
                                                                    folderMenuTarget = null
                                                                    renaming = entry
                                                                },
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("Delete folder", fontSize = 13.sp, color = Color(0xFFE5534B)) },
                                                                leadingIcon = { Icon(painterResource(R.drawable.ic_action_trash), null, tint = Color(0xFFE5534B), modifier = Modifier.size(16.dp)) },
                                                                onClick = {
                                                                    folderMenuTarget = null
                                                                    pendingDeleteFolder = entry
                                                                },
                                                            )
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                        // Fill remaining slots in row with empty spacers so card sizes stay uniform
                                        repeat(folderCols - rowFolders.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 2C. NOTES SECTION HEADER (Recent Notes & Canvases)
                // ==========================================
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val mostRecentMod = displayFiles.maxOfOrNull { it.modified }
                    val lastUpdatedStr = mostRecentMod?.let { formatRelativeTime(it) }

                    val breadcrumbs: @Composable (() -> Unit)? = if (stack.isNotEmpty()) {
                        {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    text = "Home",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF71717A),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            stack.clear()
                                            selection.clear()
                                            opError = null
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                )
                                Text(
                                    text = "/",
                                    fontSize = 13.sp,
                                    color = Color(0xFF71717A),
                                )
                                stack.forEachIndexed { i, (_, name) ->
                                    val isCurrent = i == stack.lastIndex
                                    Text(
                                        text = name,
                                        fontSize = 15.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isCurrent) (if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)) else Color(0xFF71717A),
                                        modifier = if (!isCurrent) {
                                            Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    while (stack.size > i + 1) stack.removeAt(stack.lastIndex)
                                                    selection.clear()
                                                    opError = null
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        } else Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    )
                                    if (!isCurrent) {
                                        Text(
                                            text = "/",
                                            fontSize = 13.sp,
                                            color = Color(0xFF71717A),
                                        )
                                    }
                                }
                            }
                        }
                    } else null

                    val isAllSelected = selection.isNotEmpty() && displayFiles.isNotEmpty() && selection.size >= displayFiles.size
                    PNotesRecentSectionHeader(
                        totalCount = displayFiles.size,
                        lastUpdatedText = lastUpdatedStr,
                        isSelectionMode = selection.isNotEmpty(),
                        selectedCount = selection.size,
                        isAllSelected = isAllSelected,
                        onSelectAll = {
                            if (isAllSelected) {
                                selection.clear()
                            } else {
                                selection.clear()
                                selection.addAll(displayFiles)
                            }
                        },
                        onToggleSelectionMode = {
                            if (selection.isNotEmpty()) {
                                selection.clear()
                            } else {
                                displayFiles.firstOrNull()?.let { selection.add(it) }
                            }
                        },
                        breadcrumbsContent = breadcrumbs,
                        showViewToggle = showAllNotes || searching || folders.isEmpty(),
                        isGridView = isGridView,
                        onToggleGridView = { isGridView = !isGridView },
                        modifier = Modifier.padding(bottom = 14.dp),
                    )
                }

                // ==========================================
                // 2D. NOTES GRID / LIST ITEMS OR EMPTY STATE
                // ==========================================
                when {
                    searching && results == null -> {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptyPane("Searching…") }
                    }
                    searching && results!!.isEmpty() -> {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptyPane("No notes match “$query”.") }
                    }
                    !searching && !showAllNotes && entries == null -> {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptyPane("Loading…") }
                    }
                    !searching && showAllNotes && vaultAllNotes == null -> {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptyPane("Loading all notes…") }
                    }
                    displayFiles.isEmpty() -> {
                        val emptyMsg = when {
                            activeFilter == HomeFilter.FAVORITES -> "No favorite notes yet. Tap the star on any note to add it here."
                            activeFilter == HomeFilter.CANVASES -> "No canvas notes found. Create a new canvas to get started!"
                            activeFilter == HomeFilter.PDFS -> "No imported PDFs found. Tap 'Import' to add documents."
                            activeFilter == HomeFilter.NOTES -> "No note documents found in this folder."
                            showAllNotes -> "No notes found in storage."
                            else -> "This folder has no notes."
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyPane(emptyMsg)
                        }
                    }
                    else -> {
                        itemsIndexed(
                            items = displayFiles,
                            key = { _, it -> it.documentUri },
                            span = { _, _ -> GridItemSpan(if (isGridView) 1 else maxLineSpan) },
                        ) { idx, entry ->
                            val thumb by produceState<ImageBitmap?>(editor.cachedNoteTile(entry.documentUri), entry.documentUri, entry.modified, entry.name) {
                                value = editor.tileThumbnail(entry.documentUri, entry.name)
                            }
                            val isNoteMenuOpen = noteMenuTarget?.documentUri == entry.documentUri
                            val isStarred = editor.isFavorite(entry.documentUri)
                            val isSelected = selection.any { it.documentUri == entry.documentUri }

                            val isNote = !isPdfDocument(entry.name) && !isCanvasDocument(entry.name)
                            val docKind = when {
                                isCanvasDocument(entry.name) -> "Canvas"
                                isPdfDocument(entry.name) -> "PDF"
                                stack.isNotEmpty() -> stack.last().second
                                else -> "Note"
                            }
                            val subtitleText = "$docKind · ${formatRelativeTime(entry.modified)}"
                            val colorTag = entry.color?.let { codeOutline(it, isDark).toComposeColor() }

                            if (isGridView) {
                                PNotesPrototypeNoteCard(
                                    title = entryLabel(entry),
                                    subtitleText = subtitleText,
                                    thumbnail = thumb,
                                    colorTag = colorTag,
                                    styleIndex = idx,
                                    selected = isSelected,
                                    isSelectionMode = selection.isNotEmpty(),
                                    isStarred = isStarred,
                                    isNoteDocument = isNote,
                                    onClick = {
                                        opError = null
                                        if (selection.isNotEmpty()) toggleSelect(entry) else onOpenFile(entry.documentUri, entry.name)
                                    },
                                    onMenuClick = { noteMenuTarget = entry },
                                    onStarClick = {
                                        editor.toggleFavorite(entry.documentUri)
                                        refreshKey++
                                    },
                                    menuContent = {
                                        EntryMenu(
                                            expanded = isNoteMenuOpen,
                                            onDismiss = { noteMenuTarget = null },
                                            onRename = { renaming = entry },
                                            onCopy = { clipboard = ClipItem(listOf(entry), currentDocId, false) },
                                            onCut = { clipboard = ClipItem(listOf(entry), currentDocId, true) },
                                            onDelete = { pendingDelete = listOf(entry) },
                                            onShare = { onShareFile(entry.documentUri) },
                                            onSaveCopy = { onSaveCopyFile(entry.documentUri) },
                                            onExportPdf = { onExportFilePdf(entry.documentUri) },
                                            onColor = { c ->
                                                scope.launch {
                                                    val parentDoc = if (entry.parentDocId.isNotEmpty()) entry.parentDocId else currentDocId
                                                    withContext(Dispatchers.IO) { editor.setItemColor(root, parentDoc, entry.name, c) }
                                                    refreshKey++
                                                }
                                            },
                                            onShowPath = { pathInfoEntry = entry },
                                        )
                                    },
                                    modifier = Modifier.onGloballyPositioned { r -> fileBounds[entry.documentUri] = r.boundsInWindow() },
                                )
                            } else {
                                PNotesPrototypeNoteListRow(
                                    title = entryLabel(entry),
                                    subtitleText = subtitleText,
                                    thumbnail = thumb,
                                    colorTag = colorTag,
                                    styleIndex = idx,
                                    selected = isSelected,
                                    isSelectionMode = selection.isNotEmpty(),
                                    isStarred = isStarred,
                                    isNoteDocument = isNote,
                                    onClick = {
                                        opError = null
                                        if (selection.isNotEmpty()) toggleSelect(entry) else onOpenFile(entry.documentUri, entry.name)
                                    },
                                    onMenuClick = { noteMenuTarget = entry },
                                    onStarClick = {
                                        editor.toggleFavorite(entry.documentUri)
                                        refreshKey++
                                    },
                                    menuContent = {
                                        EntryMenu(
                                            expanded = isNoteMenuOpen,
                                            onDismiss = { noteMenuTarget = null },
                                            onRename = { renaming = entry },
                                            onCopy = { clipboard = ClipItem(listOf(entry), currentDocId, false) },
                                            onCut = { clipboard = ClipItem(listOf(entry), currentDocId, true) },
                                            onDelete = { pendingDelete = listOf(entry) },
                                            onShare = { onShareFile(entry.documentUri) },
                                            onSaveCopy = { onSaveCopyFile(entry.documentUri) },
                                            onExportPdf = { onExportFilePdf(entry.documentUri) },
                                            onColor = { c ->
                                                scope.launch {
                                                    val parentDoc = if (entry.parentDocId.isNotEmpty()) entry.parentDocId else currentDocId
                                                    withContext(Dispatchers.IO) { editor.setItemColor(root, parentDoc, entry.name, c) }
                                                    refreshKey++
                                                }
                                            },
                                            onShowPath = { pathInfoEntry = entry },
                                        )
                                    },
                                    modifier = Modifier.onGloballyPositioned { r -> fileBounds[entry.documentUri] = r.boundsInWindow() },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button pinned at bottom-right corner of screen (Quick Inking) - hidden when in selection mode
        if (selection.isEmpty()) {
            PNotesQuickInkingFab(
                onQuickInking = {
                    val quickStem = nextUntitled(editor, entries)
                    val pageStyle = com.pnotes.core.model.PageStyle(
                        pageColor = Rgba(0x2B, 0x2B, 0x2B, 255),
                    )
                    val limeCover = com.pnotes.ui.model.CoverItem(
                        id = "asset_cover_fruit_explosive_lime",
                        name = "Explosive Lime",
                        type = com.pnotes.ui.model.ProceduralCoverType.ASSET_IMAGE,
                        assetPath = "covers/cover_fruit_explosive_lime.png",
                        horizontalAssetPath = "covers/cover_fruit_explosive_lime_horizontal.png",
                    )
                    scope.launch {
                        val uri = withContext(Dispatchers.IO) {
                            editor.createConfiguredNoteFile(
                                treeUri = root,
                                parentDocId = currentDocId,
                                rawName = quickStem,
                                style = pageStyle,
                                pageSize = com.pnotes.core.model.PageSize.A4,
                                orientation = com.pnotes.core.model.Orientation.PORTRAIT,
                                margins = com.pnotes.core.model.PageMargins(),
                                cover = limeCover,
                            )
                        }
                        if (uri != null) {
                            refreshKey++
                            onOpenFile(uri, null)
                        } else {
                            opError = "Couldn’t create quick note."
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(28.dp)
                    .zIndex(10f),
            )
        }

        // Floating Selection Action Dock pinned at bottom center
        val isAllStarred = selection.isNotEmpty() && selection.all { editor.isFavorite(it.documentUri) }
        PnotesSelectionActionDock(
            visible = selection.isNotEmpty(),
            selectedCount = selection.size,
            isAllStarred = isAllStarred,
            onMove = { showFolderPickerDialog = true },
            onDuplicate = {
                val itemsToCopy = selection.toList()
                selection.clear()
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        var allOk = true
                        itemsToCopy.forEach { item ->
                            if (!editor.copyDocumentInto(root, item.documentUri, currentDocId)) {
                                allOk = false
                            }
                        }
                        allOk
                    }
                    if (ok) refreshKey++ else opError = "Couldn’t duplicate some items."
                }
            },
            onToggleStar = {
                val items = selection.toList()
                val allStarred = items.all { editor.isFavorite(it.documentUri) }
                items.forEach { item ->
                    val isFav = editor.isFavorite(item.documentUri)
                    if (allStarred == isFav) {
                        editor.toggleFavorite(item.documentUri)
                    }
                }
                refreshKey++
            },
            onColorTag = {
                batchColorTargets = selection.toList()
                showBatchColorDialog = true
            },
            onExportPdf = {
                val items = selection.toList()
                selection.clear()
                items.forEach { item ->
                    onExportFilePdf(item.documentUri)
                }
            },
            onDelete = {
                pendingDelete = selection.toList()
            },
            onDismiss = { selection.clear() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .zIndex(15f),
        )

        // Floating stack of dragged notes
        val pos = dragPos
        val bc = boxCoords
        val sz = dragCardSize
        if (pos != null && bc != null && sz != null && sz.width > 0) {
            val origin = bc.positionInWindow()
            val lift = with(LocalDensity.current) { 24.dp.toPx() }
            val dx = pos.x - origin.x - sz.width / 2f
            val dy = pos.y - origin.y - sz.height / 2f - lift
            DragPreview(editor, dragItems, sz, Modifier.offset { IntOffset(dx.roundToInt(), dy.roundToInt()) })
        }
    }

    // Name entry for new folder, new canvas, or a pending PDF/Open import.
    if ((createMode == CreateMode.FOLDER || createMode == CreateMode.CANVAS || pendingImport != null) && !editor.importing) {
        val isFolder = pendingImport == null && createMode == CreateMode.FOLDER
        val default = when {
            pendingImport != null -> pendingImport.defaultName // import names default to the source file
            createMode == CreateMode.CANVAS -> nextUntitled(editor, editor.cachedChildren(root, currentDocId))
            else -> "" // new folder
        }
        NameDialog(
            title = when {
                pendingImport != null -> com.pnotes.ui.ImportPrompt.title(pendingImport.kind)
                isFolder -> "New folder"
                createMode == CreateMode.CANVAS -> "New canvas"
                else -> "New note"
            },
            initial = default,
            confirmLabel = if (pendingImport != null) com.pnotes.ui.ImportPrompt.CONFIRM_LABEL else "Create",
            placeholder = if (isFolder) "Folder name" else null,
            allowEmpty = !isFolder, // a folder needs a name
            error = fieldError,
            onConfirm = { n ->
                when {
                    pendingImport != null -> scope.launch {
                        // Land the import in the current folder; it opens only when the user taps it.
                        // commitImportAsync drives the "Importing…" dialog and runs the copy off-thread.
                        val uri = editor.commitImportAsync(root, currentDocId, n)
                        when {
                            uri != null -> refreshKey++
                            editor.pendingImport != null -> fieldError = "Couldn’t save that note." // genuine failure; keep the prompt
                            // else: cancelled — the prompt already dismissed (pendingImport cleared)
                        }
                    }
                    isFolder -> scope.launch {
                        val ok = withContext(Dispatchers.IO) { editor.createFolder(root, currentDocId, n) }
                        if (ok) { onCreateMode(CreateMode.NONE); refreshKey++ } else fieldError = "Couldn’t create that folder."
                    }
                    createMode == CreateMode.CANVAS -> scope.launch {
                        val uri = withContext(Dispatchers.IO) { editor.createBlankCanvasFile(root, currentDocId, n) }
                        if (uri != null) {
                            onCreateMode(CreateMode.NONE)
                            refreshKey++
                            onOpenFile(uri, null)
                        } else fieldError = "Couldn’t create the canvas."
                    }
                    else -> Unit
                }
            },
            onDismiss = { fieldError = null; if (pendingImport != null) editor.cancelImport() else onCreateMode(CreateMode.NONE) },
        )
    }

    renaming?.let { entry ->
        NameDialog(
            title = if (entry.isDir) "Rename folder" else "Rename note",
            initial = entryLabel(entry),
            confirmLabel = "Rename",
            allowEmpty = false,
            onConfirm = { raw ->
                val kind = com.pnotes.core.util.DocumentKind.ofName(entry.name)
                val newName = if (entry.isDir || kind == null) raw
                    else com.pnotes.core.util.DocumentKind.withSuffix(
                        com.pnotes.core.util.DocumentKind.stripSuffix(raw), kind,
                    )
                // Renames touch the open-note binding (Compose state) so run on the main thread.
                val ok = editor.renameDocument(entry.documentUri, newName)
                renaming = null
                if (ok) {
                    // Carry the colour code to the new name (sidecar is keyed by name), then re-list.
                    if (entry.color != null) {
                        scope.launch {
                            withContext(Dispatchers.IO) { editor.moveItemColor(root, entry.parentDocId, entry.name, newName) }
                            refreshKey++
                        }
                    } else {
                        refreshKey++
                    }
                }
            },
            onDismiss = { renaming = null },
        )
    }

    pendingDelete?.let { targets ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete?", color = palette.text.toComposeColor()) },
            text = {
                Text(
                    text = if (targets.size == 1) "Delete “${entryLabel(targets.first())}”? This can’t be undone."
                    else "Delete ${targets.size} items? This can’t be undone.",
                    color = palette.textDim.toComposeColor(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val items = targets.toList()
                    pendingDelete = null; selection.clear(); opError = null
                    scope.launch {
                        val allOk = withContext(Dispatchers.IO) {
                            var ok = true
                            items.forEach { e ->
                                if (editor.deleteDocument(e.documentUri)) {
                                    // Drop its colour entry from the parent sidecar (a deleted folder's
                                    // own sidecar goes with it).
                                    if (e.color != null) editor.setItemColor(root, e.parentDocId, e.name, null)
                                } else {
                                    ok = false
                                }
                            }
                            ok
                        }
                        refreshKey++
                        if (!allOk) opError = "Couldn’t delete some items."
                    }
                }) { Text("Delete", color = Color(0xFFE5534B), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = palette.textDim.toComposeColor()) } },
            containerColor = palette.menuBg.toComposeColor(),
            titleContentColor = palette.text.toComposeColor(),
            textContentColor = palette.text.toComposeColor(),
        )
    }

    if (showFolderPickerDialog) {
        val count = selection.size
        val targetFolders = if (folders.isNotEmpty()) folders else runCatching { editor.browseChildren(root, rootDocId).filter { it.isDir } }.getOrDefault(emptyList())
        PnotesFolderPickerDialog(
            folders = targetFolders,
            currentDocId = currentDocId,
            rootDocId = rootDocId,
            itemCount = count,
            onSelectFolder = { targetParentDocId ->
                val itemsToMove = selection.toList()
                selection.clear()
                showFolderPickerDialog = false
                val srcParent = currentDocId
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        var allOk = true
                        itemsToMove.forEach { item ->
                            if (item.documentUri != targetParentDocId &&
                                !editor.moveDocumentInto(root, item.documentUri, srcParent, targetParentDocId)) {
                                allOk = false
                            }
                        }
                        allOk
                    }
                    if (ok) refreshKey++ else opError = "Couldn’t move some items."
                }
            },
            onDismiss = { showFolderPickerDialog = false },
        )
    }

    if (showBatchColorDialog) {
        val targets = batchColorTargets
        val count = targets.size
        PnotesBatchColorDialog(
            itemCount = count,
            onSelectColor = { c ->
                showBatchColorDialog = false
                selection.clear()
                scope.launch {
                    withContext(Dispatchers.IO) {
                        targets.forEach { item ->
                            val parentDoc = if (item.parentDocId.isNotEmpty()) item.parentDocId else currentDocId
                            editor.setItemColor(root, parentDoc, item.name, c)
                        }
                    }
                    refreshKey++
                }
            },
            onDismiss = { showBatchColorDialog = false },
        )
    }

    pendingDeleteFolder?.let { folderEntry ->
        val childCount = editor.cachedChildren(root, editor.browseDocId(folderEntry.documentUri))?.size ?: 0
        AlertDialog(
            onDismissRequest = { pendingDeleteFolder = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_trash),
                        contentDescription = null,
                        tint = Color(0xFFE5534B),
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (childCount > 0) "Delete Folder and Notes?" else "Delete Folder?",
                        fontWeight = FontWeight.Bold,
                        color = PnotesTokens.textPrimary,
                        fontSize = 18.sp,
                    )
                }
            },
            text = {
                Text(
                    text = if (childCount > 0) {
                        "The folder “${folderEntry.name}” contains $childCount ${if (childCount == 1) "note" else "notes"}. Deleting this folder will permanently delete all notes inside it. This action cannot be undone."
                    } else {
                        "Are you sure you want to delete the folder “${folderEntry.name}”? This action cannot be undone."
                    },
                    fontSize = 14.sp,
                    color = PnotesTokens.textSecondary,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = folderEntry
                        pendingDeleteFolder = null
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                editor.deleteDocument(target.documentUri)
                            }
                            if (ok) {
                                if (target.color != null) {
                                    withContext(Dispatchers.IO) {
                                        editor.setItemColor(root, target.parentDocId, target.name, null)
                                    }
                                }
                                refreshKey++
                            } else {
                                opError = "Couldn’t delete folder."
                            }
                        }
                    },
                ) {
                    Text("Delete", color = Color(0xFFE5534B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteFolder = null },
                ) {
                    Text("Cancel", color = PnotesTokens.textSecondary)
                }
            },
            containerColor = PnotesTokens.dialogBg,
        )
    }

    val ctx = LocalContext.current
    pathInfoEntry?.let { entry ->
        val realPath = resolveRealPath(ctx, entry.documentUri, entry.parentDocId, entry.name)
        RealPathDialog(
            title = if (entry.isDir) "Folder Location" else "File Location",
            itemName = entryLabel(entry),
            isDirectory = entry.isDir,
            realPath = realPath,
            onDismiss = { pathInfoEntry = null },
        )
    }

    pathInfoCustom?.let { (customTitle, customPath) ->
        RealPathDialog(
            title = "Folder Location",
            itemName = customTitle,
            isDirectory = true,
            realPath = customPath,
            onDismiss = { pathInfoCustom = null },
        )
    }

    // Step 1: Create Menu Action Dialog (What would you like to create?)
    CreateActionDialog(
        visible = showCreateMenuDialog,
        onDismiss = { showCreateMenuDialog = false },
        onNewNote = {
            showCreateMenuDialog = false
            showNewNoteDialog = true
        },
        onNewCanvas = {
            showCreateMenuDialog = false
            onCreateMode(CreateMode.CANVAS)
        },
        onImportPdf = {
            showCreateMenuDialog = false
            onImportPdf()
        },
        onNewFolder = {
            showCreateMenuDialog = false
            onCreateMode(CreateMode.FOLDER)
        },
        dontShowAgain = editor.preferences.dontShowCreateDialog,
        onDontShowAgainChange = { checked ->
            editor.applyPreferences(editor.preferences.copy(dontShowCreateDialog = checked))
        },
    )

    // Step 2: New Note Customization Dialog (matching reference)
    val currentFolderDisplay = when {
        rootName == null || rootName == "App storage" -> "Vivek NOTES"
        stack.isNotEmpty() -> stack.last().second
        else -> rootName!!
    }
    val currentSubFolders = entries?.filter { it.isDir }.orEmpty()
    val folderChoices = remember(currentSubFolders, rootName, stack) {
        val list = mutableListOf<Pair<String, String>>()
        list.add(rootDocId to (if (rootName == null || rootName == "App storage") "Vivek NOTES" else rootName!!))
        currentSubFolders.forEach { f ->
            list.add(editor.browseDocId(f.documentUri) to f.name)
        }
        list
    }

    NewNoteDialog(
        visible = isNewNoteOpen,
        currentFolderDocId = currentDocId,
        currentFolderName = currentFolderDisplay,
        availableFolders = folderChoices,
        onDismiss = {
            showNewNoteDialog = false
            if (createMode == CreateMode.FILE) onCreateMode(CreateMode.NONE)
        },
        onCreate = { title, type, style, color, targetFolderDocId, cover, orientation, margins, pageSize, showPageNumber, showDate ->
            showNewNoteDialog = false
            if (createMode == CreateMode.FILE) onCreateMode(CreateMode.NONE)
            val targetDoc = targetFolderDocId.ifEmpty { currentDocId }
            when (type) {
                com.pnotes.ui.model.NoteCreationType.HANDWRITTEN -> {
                    val pageStyle = com.pnotes.core.model.PageStyle(
                        pageColor = color.rgba,
                        pattern = style.pattern,
                    )
                    val pageMargins = margins.toPageMargins()
                    scope.launch {
                        val uri = withContext(Dispatchers.IO) {
                            editor.createConfiguredNoteFile(
                                treeUri = root,
                                parentDocId = targetDoc,
                                rawName = title,
                                style = pageStyle,
                                pageSize = pageSize,
                                orientation = orientation,
                                margins = pageMargins,
                                cover = cover,
                                showDate = showDate,
                                showPageNumber = showPageNumber,
                            )
                        }
                        if (uri != null) {
                            refreshKey++
                            onOpenFile(uri, null)
                        } else {
                            opError = "Couldn’t create note."
                        }
                    }
                }
                com.pnotes.ui.model.NoteCreationType.CANVAS -> {
                    scope.launch {
                        val uri = withContext(Dispatchers.IO) {
                            editor.createBlankCanvasFile(root, targetDoc, title)
                        }
                        if (uri != null) {
                            refreshKey++
                            onOpenFile(uri, null)
                        } else {
                            opError = "Couldn’t create canvas."
                        }
                    }
                }
            }
        },
    )
}

/** Height of the search pill (also its rounded-end radius via CircleShape). */
private val SEARCH_HEIGHT = 40.dp

/**
 * The explorer's recursive name filter, shaped as a rounded pill with a left-aligned magnifier (no
 * placeholder text). Idle it's a shorter pill; tapped, it eases out to [expandedWidth] and stops
 * (no overshoot), revealing the field. Tapping outside drops focus, which clears the query and lets
 * it ease back. A trailing ✕ does the same by hand.
 */
@Composable
private fun ExplorerSearchField(query: String, onQueryChange: (String) -> Unit, expandedWidth: Dp) {
    val palette = LocalPalette.current
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // `active` bootstraps the field into composition on tap so its FocusRequester can grab focus;
    // `focused` is the real focus state. Either keeps the pill expanded.
    var active by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val expanded = active || focused
    // Idle pill is a little shorter than the active one (not a circle) so the grow stays subtle.
    val collapsedWidth = (expandedWidth * 0.7f).coerceAtLeast(150.dp)
    val width by animateDpAsState(
        if (expanded) expandedWidth else collapsedWidth,
        // Eased grow that decelerates into its target and holds — no spring overshoot or rebound.
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "searchWidth",
    )
    LaunchedEffect(active) { if (active) runCatching { focus.requestFocus() } }
    Row(
        Modifier
            .width(width)
            .height(SEARCH_HEIGHT)
            .clip(CircleShape)
            .background(palette.surface.toComposeColor())
            .border(1.dp, palette.border.toComposeColor(), CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { active = true }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter = painterResource(R.drawable.ic_action_search), contentDescription = "Search notes", tint = palette.textDim.toComposeColor(), modifier = Modifier.size(18.dp))
        if (expanded) {
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = palette.text.toComposeColor(), fontSize = 14.sp),
                cursorBrush = SolidColor(palette.accent.toComposeColor()),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.weight(1f)
                    .focusRequester(focus)
                    .onFocusChanged {
                        if (it.isFocused) {
                            focused = true
                            active = false // bootstrap done; `focused` holds it open now
                        } else {
                            if (focused) onQueryChange("") // a real blur (tap outside) ends the search
                            focused = false
                        }
                    },
            )
            if (query.isNotEmpty()) {
                Icon(
                    PnotesIcons.close, "Clear search",
                    tint = palette.textDim.toComposeColor(),
                    modifier = Modifier.size(16.dp).clip(CircleShape)
                        .clickable { onQueryChange(""); focusManager.clearFocus() },
                )
            }
        }
    }
}

@Composable
private fun Crumb(text: String, current: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Text(
        text,
        color = (if (current) palette.text else palette.textDim).toComposeColor(),
        fontSize = 14.sp,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 2.dp),
    )
}

@Composable
private fun IconAction(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, desc, tint = palette.accent.toComposeColor(), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun IconAction(drawableRes: Int, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(painter = painterResource(drawableRes), contentDescription = desc, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
    }
}

private fun entryLabel(entry: BrowseEntry): String =
    if (entry.isDir) entry.name else com.pnotes.core.util.DocumentKind.stripSuffix(entry.name)

/** A file's last-edited date for the line beneath its tile (relative, e.g. "2 days ago"). */
private fun entryDate(entry: BrowseEntry): String =
    if (entry.modified > 0)
        android.text.format.DateUtils.getRelativeTimeSpanString(
            entry.modified, System.currentTimeMillis(), android.text.format.DateUtils.DAY_IN_MILLIS,
        ).toString()
    else ""

/** A colour-coded outline, deepened in the light theme like the accent (dark/oled keep it as stored). */
private fun codeOutline(c: Rgba, isDark: Boolean): Rgba = if (isDark) c else ColorMath.darkenForLight(c)

@Composable
internal fun EntryMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: (() -> Unit)?,
    onCopy: (() -> Unit)?,
    onCut: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onShare: (() -> Unit)? = null,
    onSaveCopy: (() -> Unit)? = null,
    onExportPdf: (() -> Unit)? = null,
    onColor: ((Rgba?) -> Unit)? = null,
    onShowPath: (() -> Unit)? = null,
) {
    val isDark = LocalPalette.current.isDark
    val menuBg = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF)
    val menuBorder = if (isDark) Color(0x35FFFFFF) else Color(0xFFE4E4E7)
    val iconTint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF52525B)

    var showColors by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) { if (!expanded) showColors = false }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .widthIn(min = 185.dp, max = 210.dp),
    ) {
        if (showColors) {
            ColorCodeMenuContent { c -> onDismiss(); onColor?.invoke(c) }
        } else {
            if (onShowPath != null) {
                DropdownMenuItem(
                    text = { Text("Show real path", fontWeight = FontWeight.Medium, fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_action_info), null, modifier = Modifier.size(16.dp), tint = iconTint)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    onClick = { onDismiss(); onShowPath() },
                )
            }
            DropdownMenuItem(
                text = { Text("Rename", fontWeight = FontWeight.Medium, fontSize = 13.5.sp) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_action_edit), null, modifier = Modifier.size(16.dp), tint = iconTint)
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                onClick = { onDismiss(); onRename?.invoke() },
            )
            DropdownMenuItem(
                text = { Text("Copy", fontWeight = FontWeight.Medium, fontSize = 13.5.sp) },
                leadingIcon = {
                    Icon(PnotesIcons.copy, null, modifier = Modifier.size(16.dp), tint = iconTint)
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                onClick = { onDismiss(); onCopy?.invoke() },
            )
            DropdownMenuItem(
                text = { Text("Cut", fontWeight = FontWeight.Medium, fontSize = 13.5.sp) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_action_cut), null, modifier = Modifier.size(16.dp), tint = iconTint)
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                onClick = { onDismiss(); onCut?.invoke() },
            )
            if (onColor != null) {
                DropdownMenuItem(
                    text = { Text("Color code", fontWeight = FontWeight.Medium, fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_tool_palette), null, modifier = Modifier.size(16.dp), tint = if (isDark) Color.White else Color(0xFF09090B))
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    onClick = { showColors = true },
                )
            }
            DropdownMenuItem(
                text = { Text("Delete", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Color(0xFFE5534B)) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_action_trash), null, modifier = Modifier.size(16.dp), tint = Color(0xFFE5534B))
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                onClick = { onDismiss(); onDelete?.invoke() },
            )
            if (onShare != null) {
                HorizontalDivider(
                    color = if (isDark) Color(0x18FFFFFF) else Color(0xFFE4E4E7),
                    modifier = Modifier.padding(vertical = 3.dp, horizontal = 10.dp),
                )
                DropdownMenuItem(
                    text = { Text("Share", fontWeight = FontWeight.Medium, fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(PnotesIcons.share, null, modifier = Modifier.size(16.dp), tint = iconTint)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    onClick = { onDismiss(); onShare() },
                )
                DropdownMenuItem(
                    text = { Text("Save a copy…", fontWeight = FontWeight.Medium, fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(PnotesIcons.save, null, modifier = Modifier.size(16.dp), tint = iconTint)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    onClick = { onDismiss(); onSaveCopy?.invoke() },
                )
                DropdownMenuItem(
                    text = { Text("Export to PDF", fontWeight = FontWeight.Medium, fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_action_import_pdf), null, modifier = Modifier.size(16.dp), tint = iconTint)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    onClick = { onDismiss(); onExportPdf?.invoke() },
                )
            }
        }
    }
}

/** Slanted parallel lines that shade a colour-coded card without hiding what's on it. */
private fun Modifier.colorHatch(color: Color): Modifier = drawBehind {
    val step = 10.dp.toPx()
    val stroke = 1.5.dp.toPx()
    val faint = color.copy(alpha = 0.28f)
    var x = -size.height
    while (x < size.width) {
        drawLine(faint, Offset(x, size.height), Offset(x + size.height, 0f), stroke)
        x += step
    }
}

/** Material chrome rounds the backstage cards; the classic accent chrome keeps them squared. */
private fun cardShape(palette: Palette): Shape =
    if (palette.isMaterial) RoundedCornerShape(12.dp) else RectangleShape

/** Slightly tighter rounding for the compact folder chips. */
private fun chipShape(palette: Palette): Shape =
    if (palette.isMaterial) RoundedCornerShape(8.dp) else RectangleShape

/** Step each deeper card down-and-right by this much so the stack reads as a tidy pile. */
private val DRAG_STACK_STEP = 8.dp

/**
 * A neat stack of the dragged notes drawn as full-size tile cards (same size as the grid tiles, taken
 * from the picked-up tile), trailing the finger while they're moved onto a folder.
 */
@Composable
private fun DragPreview(editor: Editor, items: List<BrowseEntry>, sizePx: IntSize, modifier: Modifier) {
    val density = LocalDensity.current
    val cardW = with(density) { sizePx.width.toDp() }
    val cardH = with(density) { sizePx.height.toDp() }
    // At most three cards; the primary note sits on top of the pile, the rest peek out behind it.
    val shown = items.take(3)
    val spread = DRAG_STACK_STEP * shown.lastIndex.coerceAtLeast(0)
    Box(modifier.size(cardW + spread, cardH + spread)) {
        for (i in shown.indices.reversed()) {
            val shift = DRAG_STACK_STEP * i
            StackedNoteCard(
                editor, shown[i],
                Modifier.offset(shift, shift).size(cardW, cardH).alpha(if (i == 0) 1f else 0.97f),
            )
        }
    }
}

/** One opaque card mirroring a file tile (thumbnail + name + date), for the dragged stack. */
@Composable
private fun StackedNoteCard(editor: Editor, entry: BrowseEntry, modifier: Modifier) {
    val palette = LocalPalette.current
    val thumb = editor.cachedNoteTile(entry.documentUri)
    val shape = cardShape(palette)
    Column(modifier.clip(shape).background(palette.bg.toComposeColor()).border(1.dp, palette.accent.toComposeColor(), shape)) {
        Box(Modifier.fillMaxWidth().weight(1f).background(palette.paper.toComposeColor())) {
            if (thumb != null) {
                Image(thumb, null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.matchParentSize())
            } else {
                Icon(PnotesIcons.file, null, tint = palette.textDim.toComposeColor(), modifier = Modifier.size(32.dp).align(Alignment.Center))
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(entryLabel(entry), color = palette.text.toComposeColor(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val date = entryDate(entry)
            if (date.isNotEmpty()) {
                Text(date, color = palette.textDim.toComposeColor(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A compact folder chip (small icon + name) for the wrapping row above the file tiles. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderChip(
    entry: BrowseEntry,
    selected: Boolean,
    dimmed: Boolean,
    inSelectMode: Boolean,
    onRename: (() -> Unit)?,
    onCopy: (() -> Unit)?,
    onCut: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onColor: ((Rgba?) -> Unit)?,
    onDismissSelection: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isDropTarget: Boolean,
    pulsing: Boolean,
    onPulseDone: () -> Unit,
    onBounds: (Rect?) -> Unit,
) {
    val palette = LocalPalette.current
    var menuOpen by remember { mutableStateOf(false) }
    val accent = palette.accent.toComposeColor()
    val onAccent = palette.bg.toComposeColor()
    val codeColor = entry.color?.let { codeOutline(it, palette.isDark).toComposeColor() }
    // A dropped move flicks the target chip up and back; the hover state tints it with the accent veil.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(pulsing) {
        if (pulsing) {
            pulse.animateTo(1.08f, tween(110))
            pulse.animateTo(1f, tween(160))
            onPulseDone()
        }
    }
    DisposableEffect(Unit) { onDispose { onBounds(null) } }
    // A hovering drag fills the chip exactly like a selection, so the drop target reads the same.
    val active = selected || isDropTarget
    val shape = chipShape(palette)
    Row(
        Modifier
            .fillMaxWidth()
            .scale(pulse.value)
            .onGloballyPositioned { onBounds(it.boundsInWindow()) }
            // accent-fill toggle: active fills solid accent with content flipped to bg, otherwise a thin
            // bordered transparent box. No tap ripple — the colour invert is the only cue.
            .clip(shape)
            .background(if (active) accent else Color.Transparent)
            .then(if (!active && codeColor != null) Modifier.colorHatch(codeColor) else Modifier)
            .border(1.dp, if (active) accent else (codeColor ?: palette.border.toComposeColor()), shape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .alpha(if (dimmed) 0.4f else 1f)
            .padding(start = 10.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PnotesIcons.folder, null, tint = if (active) onAccent else palette.textDim.toComposeColor(), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            entryLabel(entry), color = if (active) onAccent else palette.text.toComposeColor(), fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        // The overflow button is kept in select mode too, so the chip width (and the row layout) never
        // shifts when selection starts; there a tap dismisses the selection instead of opening the menu.
        Box {
            IconButton(
                onClick = { if (inSelectMode) onDismissSelection() else menuOpen = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(painter = painterResource(R.drawable.ic_action_more), contentDescription = "More", tint = palette.textDim.toComposeColor(), modifier = Modifier.size(16.dp))
            }
            if (!inSelectMode) EntryMenu(menuOpen, { menuOpen = false }, onRename, onCopy, onCut, onDelete, onColor = onColor)
        }
    }
}

/** A big square note tile: first-page thumbnail (cropped to the page top) + name + date. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTile(
    editor: Editor,
    entry: BrowseEntry,
    selected: Boolean,
    dimmed: Boolean,
    inSelectMode: Boolean,
    onShare: (() -> Unit)?,
    onSaveCopy: (() -> Unit)?,
    onExportPdf: (() -> Unit)?,
    onRename: (() -> Unit)?,
    onCopy: (() -> Unit)?,
    onCut: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onColor: ((Rgba?) -> Unit)?,
    onClick: () -> Unit,
    onBounds: (Rect?) -> Unit,
) {
    val palette = LocalPalette.current
    var menuOpen by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { onBounds(null) } }
    // Seed from the in-memory cache for an instant paint, then load/render off-thread; re-keying on
    // the file's mtime re-renders the tile after the note is edited.
    val thumb by produceState<ImageBitmap?>(editor.cachedNoteTile(entry.documentUri), entry.documentUri, entry.modified) {
        value = editor.tileThumbnail(entry.documentUri, entry.name)
    }
    val accent = palette.accent.toComposeColor()
    val onAccent = palette.bg.toComposeColor()
    val codeColor = entry.color?.let { codeOutline(it, palette.isDark).toComposeColor() }
    val shape = cardShape(palette)
    Column(
        Modifier
            // Square the whole card; the thumbnail shrinks vertically to leave room for the label strip.
            .aspectRatio(1f)
            .alpha(if (dimmed) 0.4f else 1f)
            // accent-fill family: one outline wraps the thumbnail and the label strip. border draws
            // over its children, so it stays crisp on top of the full-bleed thumbnail.
            .clip(shape)
            .border(1.dp, if (selected) accent else (codeColor ?: palette.border.toComposeColor()), shape)
            // No tap ripple — the accent border + fill is the only selection cue. The tile owns only tap
            // (open / toggle in select mode); long-press to select and the drag-to-move gesture both live
            // on the grid container, so they survive this tile scrolling out from under the finger.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .onGloballyPositioned { onBounds(it.boundsInWindow()) },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(palette.paper.toComposeColor()),
        ) {
            val img = thumb
            if (img != null) {
                Image(img, entryLabel(entry), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.matchParentSize())
            } else {
                Icon(PnotesIcons.file, null, tint = palette.textDim.toComposeColor(), modifier = Modifier.size(32.dp).align(Alignment.Center))
            }
            // Selected: a translucent accent veil over the thumbnail, tying it to the accent label strip.
            if (selected) Box(Modifier.matchParentSize().background(palette.accentAlpha(38).toComposeColor()))
            if (!inSelectMode && onRename != null) {
                Box(Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                        Icon(painter = painterResource(R.drawable.ic_action_more), contentDescription = "More", tint = palette.textDim.toComposeColor(), modifier = Modifier.size(18.dp))
                    }
                    EntryMenu(menuOpen, { menuOpen = false }, onRename, onCopy, onCut, onDelete, onShare, onSaveCopy, onExportPdf, onColor = onColor)
                }
            }
        }
        // Label strip inside the outline; selected fills accent with the text flipped to bg.
        // A colour code hatches the strip only, never the thumbnail above it.
        Column(
            Modifier
                .fillMaxWidth()
                .background(if (selected) accent else Color.Transparent)
                .then(if (!selected && codeColor != null) Modifier.colorHatch(codeColor) else Modifier)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                entryLabel(entry), color = if (selected) onAccent else palette.text.toComposeColor(), fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val date = entryDate(entry)
            if (date.isNotEmpty()) {
                Text(
                    date, color = if (selected) onAccent else palette.textDim.toComposeColor(), fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// --- shared bits ---

/**
 * A small modal that asks for a single name, used for new notes, new folders, renames, and
 * naming a pending import. Pre-fills [initial] (fully selected so typing replaces it), confirms
 * on the keyboard's Done action or hardware Enter, and dismisses on Cancel, the scrim, or Esc.
 * When [allowEmpty] is false the confirm button stays disabled until something is typed; a
 * non-null [error] shows under the field and keeps the dialog open after a failed operation.
 */
@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    placeholder: String? = null,
    allowEmpty: Boolean,
    error: String? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var text by remember { mutableStateOf(TextFieldValue(initial, selection = TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val confirm = {
        val n = text.text.trim()
        if (allowEmpty || n.isNotEmpty()) onConfirm(n)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = palette.text.toComposeColor()) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                isError = error != null,
                placeholder = placeholder?.let { { Text(it, color = palette.textDim.toComposeColor()) } },
                supportingText = error?.let { { Text(it, color = Color(0xFFE5534B)) } },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.text.toComposeColor(),
                    unfocusedTextColor = palette.text.toComposeColor(),
                    focusedBorderColor = palette.accent.toComposeColor(),
                    unfocusedBorderColor = palette.border.toComposeColor(),
                    cursorColor = palette.accent.toComposeColor(),
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier
                    .focusRequester(focus)
                    .onPreviewKeyEvent { ev ->
                        when {
                            ev.type != KeyEventType.KeyDown -> false
                            ev.key == Key.Enter || ev.key == Key.NumPadEnter -> { confirm(); true }
                            ev.key == Key.Escape -> { onDismiss(); true }
                            else -> false
                        }
                    },
            )
        },
        confirmButton = {
            TextButton(onClick = { confirm() }, enabled = allowEmpty || text.text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = palette.textDim.toComposeColor()) } },
        containerColor = palette.menuBg.toComposeColor(),
        titleContentColor = palette.text.toComposeColor(),
        textContentColor = palette.text.toComposeColor(),
    )
}

private const val PRESS_FILL_MS = 120L

/** Line glyph above a label in a bordered box; inverts to the accent while pressed. Matches the About pane buttons. */
@Composable
private fun PrimaryButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    // Keep the accent fill visible for a minimum time so even a millisecond tap registers.
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(interaction) {
        val scope = this
        var pressedAt = 0L
        var clearJob: Job? = null
        interaction.interactions.collect { event ->
            when (event) {
                is PressInteraction.Press -> {
                    clearJob?.cancel()
                    pressedAt = System.currentTimeMillis()
                    pressed = true
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    val remaining = PRESS_FILL_MS - (System.currentTimeMillis() - pressedAt)
                    clearJob?.cancel()
                    clearJob = scope.launch {
                        if (remaining > 0) delay(remaining)
                        pressed = false
                    }
                }
            }
        }
    }
    val accent = palette.accent.toComposeColor()
    val onAccent = palette.bg.toComposeColor()
    val shape = cardShape(palette)
    Column(
        modifier
            .clip(shape)
            .background(if (pressed) accent else Color.Transparent)
            .border(1.dp, if (pressed) accent else palette.border.toComposeColor(), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (pressed) onAccent else accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = if (pressed) onAccent else palette.text.toComposeColor(),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyPane(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = LocalPalette.current.textDim.toComposeColor(), fontSize = 14.sp)
    }
}

/**
 * Manages fetching, caching, and serving inspiring daily study and productivity quotes.
 * Supports live auto-update from internet with curated instant fallbacks.
 */
object StudyQuoteManager {
    private val curatedStudyQuotes = listOf(
        "“Success is the sum of small efforts, repeated day in and day out.”" to "Robert Collier",
        "“The secret of getting ahead is getting started.”" to "Mark Twain",
        "“Focus on progress, not perfection. Every page written is a step forward.”" to "Bill Phillips",
        "“Small disciplines repeated with consistency every day lead to great achievements.”" to "John C. Maxwell",
        "“Live as if you were to die tomorrow. Learn as if you were to live forever.”" to "Mahatma Gandhi",
        "“The capacity to learn is a gift; the ability to learn is a skill; the willingness to learn is a choice.”" to "Brian Herbert",
        "“Study while others are sleeping; work while others are loafing; prepare while others are playing.”" to "William Arthur Ward",
        "“Knowledge is having the right answer. Intelligence is asking the right question.”" to "Anonymous",
        "“There is no substitute for hard work and persistent curiosity.”" to "Thomas Edison",
        "“Energy and persistence conquer all things.”" to "Benjamin Franklin",
        "“It always seems impossible until it's done.”" to "Nelson Mandela",
        "“You don’t have to be great to start, but you have to start to be great.”" to "Zig Ziglar",
    )

    var currentQuote by mutableStateOf(curatedStudyQuotes.random())
    var isLoading by mutableStateOf(false)
    private var hasFetchedOnline = false

    fun fetchOnlineQuote(scope: kotlinx.coroutines.CoroutineScope, force: Boolean = false) {
        if (hasFetchedOnline && !force) return
        scope.launch {
            isLoading = true
            val onlineQuote = withContext(Dispatchers.IO) {
                var conn: java.net.HttpURLConnection? = null
                try {
                    val url = java.net.URL("https://dummyjson.com/quotes/random")
                    conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 3500
                    conn.readTimeout = 3500
                    conn.requestMethod = "GET"
                    if (conn.responseCode == 200) {
                        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = org.json.JSONObject(responseText)
                        val quote = json.optString("quote", "")
                        val author = json.optString("author", "")
                        if (quote.isNotBlank()) {
                            "“$quote”" to author
                        } else null
                    } else null
                } catch (e: Exception) {
                    null
                } finally {
                    conn?.disconnect()
                }
            }
            if (onlineQuote != null) {
                currentQuote = onlineQuote
                hasFetchedOnline = true
            } else if (force) {
                // Pick a new curated quote on manual refresh if offline
                currentQuote = curatedStudyQuotes.random()
            }
            isLoading = false
        }
    }
}

/**
 * Sleek glassmorphic Daily Study & Productivity Inspiration Quote Banner.
 * Perfectly fills the header space and automatically updates from the internet.
 */
@Composable
fun DailyStudyQuoteCard(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val isDark = LocalPalette.current.isDark

    LaunchedEffect(Unit) {
        StudyQuoteManager.fetchOnlineQuote(scope, force = false)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else (if (isHovered) 1.01f else 1.0f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "quoteScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isDark) Color(0xFF1E212A).copy(alpha = 0.85f)
                else Color(0xFFFFFFFF)
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x1FFFFFFF) else Color(0xFFE4E7EB),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { StudyQuoteManager.fetchOnlineQuote(scope, force = true) },
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            // Sparkle / Lightbulb Accent Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5))
                    .border(1.dp, if (isDark) Color(0x35FFFFFF) else Color(0xFFE4E4E7), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "💡",
                    fontSize = 18.sp,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "DAILY STUDY INSPIRATION",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF09090B),
                        letterSpacing = 0.6.sp,
                    )
                    if (StudyQuoteManager.isLoading) {
                        Text(
                            text = "• updating…",
                            fontSize = 10.sp,
                            color = PnotesTokens.textMuted,
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = StudyQuoteManager.currentQuote.first,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = PnotesTokens.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.5.sp,
                )

                if (StudyQuoteManager.currentQuote.second.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "— ${StudyQuoteManager.currentQuote.second}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF09090B),
                    )
                }
            }
        }
    }
}

/**
 * Classifies document entries into Canvases, PDFs, and Notes.
 */
fun isCanvasDocument(name: String): Boolean {
    val lower = name.lowercase()
    return lower.endsWith(".pcanvas") || lower.endsWith(".xcanvas")
        || com.pnotes.core.util.DocumentKind.ofName(name) == com.pnotes.core.util.DocumentKind.CANVAS
}

fun isPdfDocument(name: String): Boolean {
    return name.endsWith(".pdf", ignoreCase = true)
}

fun isNoteDocument(name: String): Boolean {
    val lower = name.lowercase()
    return lower.endsWith(".pnote") || lower.endsWith(".pnotes")
        || lower.endsWith(".xnote") || lower.endsWith(".xnotes")
        || (!isCanvasDocument(name) && !isPdfDocument(name))
}

/**
 * Dynamic Home Filter Categories for Quick Navigation
 */
enum class HomeFilter(val label: String) {
    ALL("All"),
    NOTES("Notes"),
    CANVASES("Canvases"),
    PDFS("PDFs"),
    FAVORITES("Favorites"),
    RECENT("Recent"),
}

/**
 * Sleek Dynamic Filter Chip Pill with Live Count Badge
 */
@Composable
fun FilterChipPill(
    filter: HomeFilter = HomeFilter.ALL,
    label: String,
    count: Int,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else (if (isHovered) 1.04f else 1.0f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "filterChipScale",
    )

    val shape = RoundedCornerShape(20.dp)

    val backgroundBrush = when {
        isSelected -> {
            if (isDark) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFE4E4E7),
                    ),
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF09090B),
                        Color(0xFF18181B),
                    ),
                )
            }
        }
        else -> {
            if (isDark) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF27272A),
                        Color(0xFF18181B),
                    ),
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFFFFFFF),
                    ),
                )
            }
        }
    }

    val borderBrush = when {
        isSelected -> {
            Brush.verticalGradient(
                if (isDark) {
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFE4E4E7),
                    )
                } else {
                    listOf(
                        Color(0xFF09090B),
                        Color(0xFF27272A),
                    )
                }
            )
        }
        else -> {
            if (isDark) {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f),
                    ),
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFE4E4E7),
                        Color(0xFFE4E4E7),
                    ),
                )
            }
        }
    }

    val textColor = if (isSelected) {
        if (isDark) Color(0xFF09090B) else Color.White
    } else {
        if (isDark) Color(0xFFE4E4E7) else Color(0xFF374151)
    }

    val badgeBg = if (isSelected) {
        if (isDark) Color(0x22000000) else Color(0x28FFFFFF)
    } else {
        if (isDark) Color(0x35FFFFFF) else Color(0xFFF4F4F5)
    }

    val badgeTextColor = if (isSelected) {
        if (isDark) Color(0xFF09090B) else Color.White
    } else {
        if (isDark) Color(0xFFA1A1AA) else Color(0xFF8A96A3)
    }

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                val shadowColor = if (isSelected) {
                    if (isDark) android.graphics.Color.argb(80, 255, 255, 255)
                    else android.graphics.Color.argb(60, 0, 0, 0)
                } else {
                    if (isDark) android.graphics.Color.argb(75, 0, 0, 0)
                    else android.graphics.Color.argb(12, 0, 0, 0)
                }
                val blurRadius = if (isSelected) 8.dp.toPx() else 3.dp.toPx()
                val yOffset = if (isSelected) 3.dp.toPx() else 1.dp.toPx()

                drawIntoCanvas { canvas ->
                    val paint = com.pnotes.ui.theme.toolbarShadowPaint
                    synchronized(paint) {
                        paint.setShadowLayer(blurRadius, 0f, yOffset, shadowColor)
                        canvas.nativeCanvas.drawRoundRect(
                            0f,
                            0f,
                            size.width,
                            size.height,
                            20.dp.toPx(),
                            20.dp.toPx(),
                            paint,
                        )
                    }
                }
            }
            .clip(shape)
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Category icon box when unselected
            if (!isSelected) {
                when (filter) {
                    HomeFilter.NOTES -> {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF16243A) else Color(0xFFEAF5FF)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.stickyNote,
                                contentDescription = null,
                                tint = Color(0xFF3B9FE8),
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                    HomeFilter.CANVASES -> {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.canvas,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                    HomeFilter.PDFS -> {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.download,
                                contentDescription = null,
                                tint = if (isDark) Color.White else Color(0xFF09090B),
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                    HomeFilter.FAVORITES -> {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF142B1D) else Color(0xFFE8F7E9)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.star,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                    HomeFilter.RECENT -> {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.clock,
                                contentDescription = null,
                                tint = if (isDark) Color.White else Color(0xFF09090B),
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                    HomeFilter.ALL -> Unit
                }
            }

            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor,
            )

            // Circular Badge count
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                )
            }
        }
    }
}

/** Resolves a human-readable, exact absolute filesystem path from a SAF Document or Tree URI. */
internal fun resolveRealPath(
    context: Context,
    documentUriString: String?,
    parentDocId: String? = null,
    entryName: String? = null,
): String {
    if (documentUriString.isNullOrBlank()) return ""
    return runCatching {
        val uri = Uri.parse(documentUriString)
        val auth = uri.authority ?: ""

        if (auth.endsWith(".documents") || auth.contains("pnotes") || auth.contains("pnotes")) {
            var docId: String? = null
            if (DocumentsContract.isDocumentUri(context, uri)) {
                docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            }
            if (docId.isNullOrBlank()) {
                docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            }
            if (docId.isNullOrBlank() && !parentDocId.isNullOrBlank() && !entryName.isNullOrBlank()) {
                docId = "$parentDocId/$entryName"
            }
            val base = AppStorageDocumentsProvider.rootDir(context)
            if (docId.isNullOrBlank() || docId == "root") {
                base.absolutePath
            } else {
                val rel = docId.removePrefix("root/").removePrefix("root")
                if (rel.isEmpty() || rel == "/") base.absolutePath
                else File(base, rel.trimStart('/')).absolutePath
            }
        } else if (auth == "com.android.externalstorage.documents") {
            var docId: String? = null
            if (DocumentsContract.isDocumentUri(context, uri)) {
                docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            }
            if (docId.isNullOrBlank()) {
                docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            }
            if (docId.isNullOrBlank() && !parentDocId.isNullOrBlank() && !entryName.isNullOrBlank()) {
                docId = "$parentDocId/$entryName"
            }
            if (!docId.isNullOrBlank()) {
                val split = docId.split(":")
                val type = split.getOrNull(0) ?: ""
                val path = if (split.size > 1) split[1] else ""
                if (type.equals("primary", ignoreCase = true)) {
                    val ext = android.os.Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
                    if (path.isEmpty()) ext else "$ext/${path.trimStart('/')}"
                } else if (type.isNotEmpty()) {
                    if (path.isEmpty()) "/storage/$type" else "/storage/$type/${path.trimStart('/')}"
                } else {
                    docId
                }
            } else {
                Uri.decode(documentUriString)
            }
        } else if (uri.scheme.equals("file", ignoreCase = true)) {
            uri.path ?: documentUriString
        } else {
            var resolvedPath: String? = null
            runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.MediaStore.MediaColumns.DATA),
                    null,
                    null,
                    null,
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                        if (idx >= 0) resolvedPath = c.getString(idx)
                    }
                }
            }
            resolvedPath ?: Uri.decode(documentUriString)
        }
    }.getOrElse {
        Uri.decode(documentUriString)
    }
}

/** Sleek popup dialog displaying the exact real filesystem path with a one-tap copy button. */
@Composable
private fun RealPathDialog(
    title: String,
    itemName: String,
    isDirectory: Boolean,
    realPath: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isDark = LocalPalette.current.isDark
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (isDirectory) (if (isDark) Color(0x28FFFFFF) else Color(0xFFF4F4F5))
                            else (if (isDark) Color(0xFF1E2C44) else Color(0xFFEDF4FF)),
                            RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (isDirectory) R.drawable.ic_action_folder else R.drawable.ic_action_documents),
                        contentDescription = null,
                        tint = if (isDirectory) (if (isDark) Color.White else Color(0xFF09090B))
                               else (if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF09090B),
                    )
                    Text(
                        text = itemName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = "EXACT REAL PATH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF12151C) else Color(0xFFFAFAFA),
                    border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else Color(0xFFE4E4E7)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = realPath,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF18181B),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Real Path", realPath)
                        clipboard?.setPrimaryClip(clip)
                        copied = true
                        Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (copied) {
                            if (isDark) Color(0xFF163E2B) else Color(0xFFDCFCE7)
                        } else {
                            if (isDark) Color(0xFF28303F) else Color(0xFFF4F4F5)
                        },
                        contentColor = if (copied) {
                            if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                        } else {
                            if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A)
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                ) {
                    if (copied) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_done),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(
                            imageVector = PnotesIcons.copy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (copied) "Path Copied!" else "Copy Real Path",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = "Close",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else Color(0xFF09090B),
                )
            }
        },
    )
}



