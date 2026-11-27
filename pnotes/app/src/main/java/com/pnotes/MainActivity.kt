package com.pnotes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.zIndex
import com.pnotes.account.AccountStore
import com.pnotes.account.LocalAccountStore
import com.pnotes.account.LocalOpenPremium
import com.pnotes.account.LocalOpenSignIn
import com.pnotes.core.tools.Tool
import com.pnotes.ui.auth.LoginHost
import com.pnotes.ui.premium.PremiumHost
import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.IntentCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pnotes.ui.Editor
import com.pnotes.ui.FloatingPanel
import com.pnotes.ui.PanelAnchor
import com.pnotes.ui.PnotesAdaptiveQuickRail
import com.pnotes.ui.rememberLastPresent
import com.pnotes.ui.RailDockEdge
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.LocalMotion
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.LocalReducedMotion
import com.pnotes.ui.theme.PnotesTheme
import com.pnotes.ui.theme.rememberMotionSpecs
import com.pnotes.ui.theme.toComposeColor
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Minimum time the launch loader stays up, so its animation is briefly seen even
 *  when the session restores instantly. */
private const val MIN_LOADER_MS = 600L

/** Whether the display has a camera cutout (notch/hole-punch). False below API 29, which has no
 *  cutout API; such devices fall back to fullscreen by default. Also false for a context with no
 *  display of its own, which cannot answer the question. */
internal fun deviceHasDisplayCutout(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val display = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.display
        else @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    }.getOrNull()
    return display?.cutout != null
}

/** The standard touch action an old One UI S-Pen-button code stands in for, or -1 for anything else. */
private fun standardPenAction(action: Int): Int = when (action) {
    211 -> MotionEvent.ACTION_DOWN
    212 -> MotionEvent.ACTION_UP
    213 -> MotionEvent.ACTION_MOVE
    else -> -1
}

class MainActivity : ComponentActivity() {

    // The editor owns the fullscreen state (persisted preference, default depends on the display
    // cutout); this activity just applies it to the window. Fullscreen draws edge to edge and lets
    // the swipe-in transient bars overlay (no resize), non-fullscreen insets under the bars.
    private var editor: Editor? = null
    // A PDF handed to us by another app ("Open with" / Share); consumed once the editor is ready.
    private var pendingPdfImport by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableHighRefreshRate()
        com.pnotes.platform.FontCatalog.init(this)
        com.pnotes.platform.AndroidRenderer.initTextures(this)
        setTheme(com.pnotes.R.style.Theme_PNotes) // leave the dark launch/splash theme behind
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        applyFullscreen(true) // Fullscreen edge-to-edge mode (status bars hidden)
        pendingPdfImport = pdfImportUri(intent)
        setContent {
            val context = LocalContext.current
            val ed = remember { Editor(context).also { editor = it } }
            LaunchedEffect(ed.fullscreen) { applyFullscreen(ed.fullscreen) }
            var ready by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                ed.restoreSession()
                ready = true
                ed.prewarmBackstage() // warm recents/explorer caches so the first backstage open is instant
            }
            // One motion vocabulary for the whole app, resolved once. The editor's surfaces read it
            // from here, so they honour the same reduced-motion setting Login and Premium already do.
            val motion = rememberMotionSpecs()
            val baseDensity = LocalDensity.current
            val uiScale = run {
                ed.prefsVersion
                ed.preferences.uiScale.coerceIn(
                    com.pnotes.settings.Preferences.UI_SCALE_MIN,
                    com.pnotes.settings.Preferences.UI_SCALE_MAX
                )
            }
            val scaledDensity = remember(baseDensity.density, baseDensity.fontScale, uiScale) {
                Density(
                    density = baseDensity.density * uiScale,
                    fontScale = baseDensity.fontScale * uiScale,
                )
            }
            CompositionLocalProvider(
                LocalDensity provides scaledDensity,
                LocalMotion provides motion,
                LocalReducedMotion provides motion.reduced,
            ) {
            PnotesTheme(ed.palette) {
                val accountStore = remember { AccountStore.get(context) }
                var showSignIn by remember { mutableStateOf(false) }
                var showPremium by remember { mutableStateOf(false) }
                CompositionLocalProvider(
                    LocalAccountStore provides accountStore,
                    LocalOpenSignIn provides { showSignIn = true },
                    LocalOpenPremium provides { showPremium = true },
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (ready) {
                            EditorScreen(
                                ed,
                                fullscreen = ed.fullscreen,
                                onToggleFullscreen = ed::toggleFullscreen,
                                importPdfUri = pendingPdfImport,
                                onImportConsumed = { pendingPdfImport = null },
                            )
                        }
                        // The sign-in surface is hosted here, above everything else, so the account
                        // entry points can open it without threading a callback down through the
                        // explorer's nesting. It covers the app only while it is open.
                        if (showSignIn) {
                            // Opaque to touch as well as to sight. The artwork is mostly empty
                            // space, and Compose only stops hit-testing lower siblings once a hit
                            // node exists up here — without this the taps that miss a control
                            // reach the Home screen underneath and open notes. It never consumes,
                            // so the login controls still see every event.
                            Box(Modifier.fillMaxSize().then(SwallowTouches)) {
                                LoginHost(
                                    onBack = { showSignIn = false },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        // Hosted the same way, and for the same reasons: the account entry points
                        // open it from anywhere, and its artwork is mostly empty space, so the taps
                        // that miss a control must not reach the screen underneath.
                        if (showPremium) {
                            Box(Modifier.fillMaxSize().then(SwallowTouches)) {
                                PremiumHost(
                                    onBack = { showPremium = false },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    // Stylus side-button keys (Bluetooth/USI pens report the button only this way) are caught here,
    // before Compose focus routing, so the controller sees both press and release regardless of
    // which view holds focus. Other keys fall through to the normal dispatch.
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // The pen writes in whichever pane has the focus, so its button belongs to that pane too.
        if (editor?.active?.onStylusButtonKey(event) == true) return true
        return super.dispatchKeyEvent(event)
    }

    // Older Samsung builds (seen on a Tab S6 Lite, Android 13; gone by Android 15) tag a stylus
    // stroke made with the S-Pen button held with proprietary action codes instead of DOWN/MOVE/UP,
    // so the view tree never opens a touch target and the whole stroke is dropped. Rewrite them
    // here, the last point they are intact, and dispatch normally. A no-op on every other device.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val standard = standardPenAction(ev.actionMasked)
        if (standard < 0 || ev.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            return super.dispatchTouchEvent(ev)
        }
        val rewritten = MotionEvent.obtain(ev)
        rewritten.action = standard
        try {
            return super.dispatchTouchEvent(rewritten)
        } finally {
            rewritten.recycle()
        }
    }

    // A PDF arriving while we're already running: singleTask reuses this instance via onNewIntent.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pdfImportUri(intent)?.let { pendingPdfImport = it }
    }

    // The PDF uri carried by an inbound VIEW ("Open with") or SEND ("Share") intent, else null.
    private fun pdfImportUri(intent: Intent?): Uri? = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND ->
            if (intent.type == "application/pdf")
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else null
        else -> null
    }

    // uiMode is in configChanges, so a system dark/light flip lands here instead of recreating us.
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val night = newConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        editor?.onSystemDarkModeChanged(night == android.content.res.Configuration.UI_MODE_NIGHT_YES)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && editor?.fullscreen == true) applyFullscreen(true) // re-hide transient bars after they swipe in
    }

    /**
     * Request the highest supported display refresh rate (e.g. 120Hz/144Hz) from the window manager.
     * Stylus inking latency drops from ~16.6ms per frame to ~8.3ms or ~6.9ms.
     */
    private fun enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val d = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            }
            d?.let {
                val modes = it.supportedModes
                val highestMode = modes.maxByOrNull { m -> m.refreshRate }
                if (highestMode != null && highestMode.refreshRate > 60f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highestMode.modeId
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        params.preferredRefreshRate = highestMode.refreshRate
                    }
                    window.attributes = params
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        editor?.persist()
    }

    override fun onStop() {
        super.onStop()
        editor?.persist()
    }

    override fun onDestroy() {
        super.onDestroy()
        editor?.persist()
        editor?.stopPresentation()
    }

    private fun applyFullscreen(fullscreen: Boolean = true) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

@Composable
private fun EditorScreen(
    editor: Editor,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    importPdfUri: Uri? = null,
    onImportConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var presentPane by remember { mutableStateOf<Editor?>(null) }
    // Backstage is the root of the stack; the editor is pushed on top only when a note is open
    // (editor.noteOpen). Every launch starts on backstage.
    var backstageView by remember { mutableStateOf(com.pnotes.ui.BackstageView.HOME) }
    var showShareChooser by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var guardAction by remember { mutableStateOf<GuardRequest?>(null) }
    var pendingAfterSave by remember { mutableStateOf<(() -> Unit)?>(null) }
    // The pane an image was picked for, and where a long-press menu asked it to land. Held together
    // so the picker's result lands in the pane that opened it, whatever the focus does meanwhile.
    var pendingInsert by remember { mutableStateOf<PendingInsert?>(null) }
    // The same, for the infinite canvas.
    var pendingCanvasInsert by remember { mutableStateOf<PendingInsert?>(null) }
    var pendingShareUri by remember { mutableStateOf<String?>(null) }
    var pendingSaveCopyUri by remember { mutableStateOf<String?>(null) }
    // A finished PDF render awaiting a SAF "Save as" destination (open-note / file / pages export).
    var pendingExportTemp by remember { mutableStateOf<java.io.File?>(null) }
    // In-flight PDF render: (pagesDone, totalPages) drives the progress dialog; null hides it.
    var exportProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // The running export's coroutine and its own cancel flag. Each export gets a fresh flag so a new
    // export can abort the previous one (set its flag, then join it) without un-cancelling itself.
    var exportJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var exportCancel by remember { mutableStateOf<java.util.concurrent.atomic.AtomicBoolean?>(null) }
    // The pane whose page indices await a SAF "Save as" destination (side-panel page export).
    var pendingExportPages by remember { mutableStateOf<PendingPages?>(null) }
    val scope = rememberCoroutineScope()
    val resolver = context.contentResolver
    val rwFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    // "Open…" remembers the picked .pnote and shows the name dialog at once; it's copied into the folder at Save.
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { u ->
            val stem = com.pnotes.core.util.Paths.stem(displayNameOf(resolver, u) ?: "Note")
            editor.requestImport(com.pnotes.ui.ImportKind.OPEN, stem, u.toString())
            backstageView = com.pnotes.ui.BackstageView.HOME
            editor.goHomeAll() // land on backstage to name/place the pending import
        }
    }
    // Which pane a "Save as" writes: the focused one at the moment the picker opened.
    var savePane by remember { mutableStateOf(editor) }
    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        uri?.let {
            runCatching { resolver.takePersistableUriPermission(it, rwFlags) }
            val name = displayNameOf(resolver, it)
            runCatching { resolver.openOutputStream(it, "wt")?.use { o -> savePane.save(o, it.toString(), name) } }
                .onSuccess { val p = pendingAfterSave; pendingAfterSave = null; p?.invoke() }
                .onFailure { editor.message = "Could not save the note."; pendingAfterSave = null }
        }
    }

    // "Import PDF" remembers the picked PDF and shows the name dialog at once; the (possibly large) copy
    // into the .pnote happens at Save, under the "Importing PDF…" loader, so the dialog isn't delayed.
    val importPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { u ->
            val stem = com.pnotes.core.util.Paths.stem(displayNameOf(resolver, u) ?: "Document")
            editor.requestImport(com.pnotes.ui.ImportKind.PDF, stem, u.toString())
            backstageView = com.pnotes.ui.BackstageView.HOME
            editor.goHomeAll() // land on backstage to name/place the pending import
        }
    }
    // A PDF "Save as" destination. The note is already rendered into [pendingExportTemp]
    // (off-thread, behind the progress dialog), so the picker just chooses where to copy it —
    // shared by the open-note export, the explorer-file export, and side-panel page saves.
    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        val temp = pendingExportTemp; pendingExportTemp = null
        if (temp != null) {
            if (uri != null) {
                val ok = runCatching { resolver.openOutputStream(uri)?.use { o -> temp.inputStream().use { it.copyTo(o) } } != null }.getOrDefault(false)
                editor.message = if (ok) "Exported to PDF." else "Could not export to PDF."
            }
            temp.delete() // discard the temp whether saved or the picker was dismissed
        }
    }

    // Save a copy of an explorer file (.pnote) elsewhere.
    val saveCopyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val src = pendingSaveCopyUri; pendingSaveCopyUri = null
        if (uri != null && src != null) {
            runCatching { resolver.openOutputStream(uri)?.use { o -> editor.copyFileTo(src, o) } }
                .onFailure { editor.message = "Could not save a copy." }
        }
    }

    // Save a single selected page as a PNG.
    val savePageImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri ->
        val pending = pendingExportPages; pendingExportPages = null
        val index = pending?.pages?.firstOrNull()
        if (uri != null && pending != null && index != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val ok = runCatching {
                    val png = pending.editor.pageImagePng(index) ?: return@runCatching false
                    resolver.openOutputStream(uri)?.use { it.write(png) } != null
                }.getOrDefault(false)
                if (!ok) editor.message = "Could not save the image."
            }
        }
    }

    // Save several selected pages as individual PNGs into a folder the user picks.
    val savePagesImagesTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        val pending = pendingExportPages; pendingExportPages = null
        if (treeUri != null && pending != null && pending.pages.isNotEmpty()) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val stem = pending.editor.title
                val saved = runCatching {
                    val parent = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                        treeUri, android.provider.DocumentsContract.getTreeDocumentId(treeUri),
                    )
                    var n = 0
                    for (index in pending.pages) {
                        val png = pending.editor.pageImagePng(index) ?: continue
                        val name = "%s-p%02d.png".format(stem, index + 1)
                        val file = android.provider.DocumentsContract.createDocument(resolver, parent, "image/png", name) ?: continue
                        resolver.openOutputStream(file)?.use { it.write(png) }
                        n++
                    }
                    n
                }.getOrDefault(0)
                editor.message = if (saved > 0) "Saved $saved image${if (saved == 1) "" else "s"}." else "Could not save the images."
            }
        }
    }

    val insertImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val pending = pendingInsert; pendingInsert = null
        if (uri != null && pending != null) {
            runCatching {
                resolver.openInputStream(uri)?.use { s -> pending.editor.insertImageAt(s.readBytes(), pending.at) }
            }.onFailure { editor.message = "Could not read the image." }
        }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val pending = pendingInsert; pendingInsert = null
        if (bitmap != null) {
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()
            if (pending != null) {
                pending.editor.insertImageAt(bytes, pending.at)
            } else {
                editor.active.insertImageAt(bytes, null)
            }
        }
    }

    // Images picked for the sticker library (multi-select), stored on disk by the editor.
    val addStickersLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            runCatching { resolver.openInputStream(uri)?.use { s -> editor.addSticker(s.readBytes()) } }
                .onFailure { editor.message = "Could not read the image." }
        }
    }

    // A user Helix code theme (.toml), parsed + stored by the editor.
    val importCodeThemeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }
                .getOrNull()
                ?.let { editor.importCodeTheme(it, displayNameOf(resolver, uri)) }
                ?: run { editor.message = "Could not read the file." }
        }
    }

    // A user font (.ttf/.otf), stored + registered by the editor.
    val importFontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }
                .getOrNull()
                ?.let { editor.importFont(it, displayNameOf(resolver, uri)) }
                ?: run { editor.message = "Could not read the file." }
        }
    }

    // Grant a folder for the in-app explorer (a one-time system folder picker).
    val pickRootLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching { resolver.takePersistableUriPermission(it, rwFlags) }
            editor.updateBrowseRoot(it.toString())
        }
    }

    /** Open the "Save as" picker for [target], remembering which pane its result belongs to. */
    fun launchSaveAs(target: Editor) {
        savePane = target
        createLauncher.launch(com.pnotes.core.util.DocumentKind.withSuffix(target.title, com.pnotes.core.util.DocumentKind.NOTE))
    }

    fun saveOrPrompt() {
        val target = editor.active
        val uri = target.currentUri
        if (uri == null || !target.saveTo(uri)) launchSaveAs(target)
    }

    // The prompt is about one pane's note, so it asks about — and saves — the pane being acted on.
    fun guarded(target: Editor = editor.active, action: () -> Unit) {
        when {
            // A canvas keeps its own autosave, and dirty/autosaveUri describe the paged buffer
            // underneath it, so asking about those here would prompt over a note nobody is looking at.
            target.canvasOpen -> action()
            target.autosaveUri != null -> action() // autosaved notes are flushed on doc-swap; no prompt
            target.dirty -> guardAction = GuardRequest(target, action)
            else -> action()
        }
    }

    /** Run [action] once every open pane has settled its unsaved changes, prompting one at a time. */
    fun guardedAll(action: () -> Unit) {
        val panes = editor.openPanes
        fun step(i: Int) {
            if (i >= panes.size) action() else guarded(panes[i]) { step(i + 1) }
        }
        step(0)
    }

    // An image picked for the infinite canvas: read the bytes and hand them straight over.
    val insertCanvasImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val pending = pendingCanvasInsert; pendingCanvasInsert = null
        if (uri == null || pending == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { resolver.openInputStream(uri)?.use { input -> input.readBytes() } }.getOrNull()
            }
            if (bytes != null) pending.editor.infinite.insertImage(bytes, pending.at)
        }
    }

    /** Read the file at [uriStr] into [target]. The extension picks the surface: the two document
     *  types share the explorer but not much else. Reads off-thread behind the "Opening note…"
     *  spinner so a big embedded PDF doesn't freeze the UI. */
    suspend fun openInto(target: Editor, uriStr: String, knownName: String? = null) {
        val name = knownName ?: kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            displayNameOf(resolver, Uri.parse(uriStr))
        }
        val isCanvas = com.pnotes.core.util.DocumentKind.ofName(name ?: "") == com.pnotes.core.util.DocumentKind.CANVAS
        val cleanName = com.pnotes.core.util.DocumentKind.stripSuffix(name ?: "")
        target.openInNewTab(uriStr, cleanName, isCanvas)
    }

    fun openTreeFile(uriStr: String, knownName: String? = null) {
        scope.launch { openInto(editor, uriStr, knownName) }
    }

    /** Open two picked files together, one per pane, and start the split focused on the first. */
    fun openSplit(firstUri: String, secondUri: String) {
        editor.splitRatio = 0.5f
        editor.focusPane(com.pnotes.ui.Pane.PRIMARY)
        val second = editor.secondaryPane()
        scope.launch {
            openInto(editor, firstUri)
            openInto(second, secondUri)
            // A file that would not open leaves no half-built pane behind; the other stays as it is.
            if (!second.noteOpen) {
                editor.abandonSecondary()
                editor.message = "Could not open the second note."
            }
        }
    }

    fun stemOf(uriStr: String): String =
        com.pnotes.core.util.Paths.stem(displayNameOf(resolver, Uri.parse(uriStr)) ?: "Note")

    // Render a PDF off the main thread into a temp file behind a cancellable progress dialog;
    // only once it finishes does [onReady] run — opening the SAF picker or a share sheet.
    // Dismissing the dialog flips this export's cancel flag, which aborts the page loop AND the
    // write stream, so the half-written temp is discarded. A fresh export first aborts and joins any
    // previous one, so they never overlap. [shareDir] picks the cache subdir: FileProvider only
    // exposes cache/share, so shares render there; plain "save" exports use cache/export.
    fun runPdfExport(
        stem: String,
        shareDir: Boolean,
        render: (java.io.OutputStream, (Int, Int) -> Unit, () -> Boolean) -> Unit,
        onReady: (java.io.File) -> Unit,
    ) {
        val prevJob = exportJob
        val prevCancel = exportCancel
        val cancel = java.util.concurrent.atomic.AtomicBoolean(false)
        exportCancel = cancel // the dialog's Cancel flips THIS export's flag
        exportProgress = 0 to 0 // show the dialog at once; the render fills in the real total
        exportJob = scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Abort any still-running previous export (its write stream throws on the next buffer) and
            // wait for it to fully unwind before we touch the shared temp dir — no overlapping saves.
            prevCancel?.set(true)
            prevJob?.join()
            val dir = java.io.File(context.cacheDir, if (shareDir) "share" else "export").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // keep only the file this export produces
            val temp = java.io.File(dir, "$stem.pdf")
            val ok = runCatching {
                java.io.FileOutputStream(temp).use { fo ->
                    // Abort the (otherwise uninterruptible) PdfBox save the moment Cancel is tapped.
                    val o = CancellableOutputStream(fo) { cancel.get() }
                    render(o, { done, total -> if (!cancel.get()) exportProgress = done to total }, { cancel.get() })
                }
            }.isSuccess
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (exportCancel === cancel) exportProgress = null // only the latest export owns the dialog
                when {
                    cancel.get() -> temp.delete()
                    ok -> onReady(temp)
                    else -> { temp.delete(); if (exportCancel === cancel) editor.message = "Could not export to PDF." }
                }
            }
        }
    }

    fun launchShare(file: java.io.File, stem: String, mime: String) {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share $stem"))
    }

    fun shareFile(uriStr: String, asPdf: Boolean) {
        val stem = stemOf(uriStr)
        if (asPdf) {
            // Render with progress, then share the finished PDF (writes into cache/share for FileProvider).
            runPdfExport(stem, shareDir = true,
                render = { o, prog, cancel -> editor.exportFileToPdf(uriStr, o, prog, cancel) },
                onReady = { temp -> runCatching { launchShare(temp, stem, "application/pdf") }.onFailure { editor.message = "Could not share the note." } })
        } else {
            // A plain .pnote share is just a fast byte copy — no render, no dialog needed.
            runCatching {
                val dir = java.io.File(context.cacheDir, "share").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() } // keep only the file we're about to share
                val file = java.io.File(dir, "$stem.pnote")
                java.io.FileOutputStream(file).use { o -> editor.copyFileTo(uriStr, o) }
                launchShare(file, stem, "application/octet-stream")
            }.onFailure { editor.message = "Could not share the note." }
        }
    }

    // Share the selected side-panel pages: as one PDF, or as one/many PNGs (ACTION_SEND_MULTIPLE).
    fun sharePages(from: Editor, pages: List<Int>, asPdf: Boolean) {
        if (pages.isEmpty()) return
        val stem = from.title
        if (asPdf) {
            runPdfExport(stem, shareDir = true,
                render = { o, prog, cancel -> from.exportPagesToPdf(pages, o, prog, cancel) },
                onReady = { temp -> runCatching { launchShare(temp, stem, "application/pdf") }.onFailure { editor.message = "Could not share the pages." } })
            return
        }
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val auth = "${context.packageName}.fileprovider"
            val intent = runCatching {
                val dir = java.io.File(context.cacheDir, "share").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val uris = ArrayList<Uri>()
                for (index in pages) {
                    val png = from.pageImagePng(index) ?: continue
                    val file = java.io.File(dir, "%s-p%02d.png".format(stem, index + 1))
                    java.io.FileOutputStream(file).use { it.write(png) }
                    uris.add(androidx.core.content.FileProvider.getUriForFile(context, auth, file))
                }
                when {
                    uris.isEmpty() -> null
                    uris.size == 1 -> Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uris[0]); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    else -> Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = "image/png"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                }
            }.getOrNull()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (intent != null) context.startActivity(Intent.createChooser(intent, "Share $stem")) else editor.message = "Could not share the pages."
            }
        }
    }

    fun savePagesAsPdf(from: Editor, pages: List<Int>) {
        if (pages.isEmpty()) return
        runPdfExport(from.title, shareDir = false,
            render = { o, prog, cancel -> from.exportPagesToPdf(pages, o, prog, cancel) },
            onReady = { temp -> pendingExportTemp = temp; savePdfLauncher.launch("${from.title}.pdf") })
    }

    // One page -> a single PNG (CreateDocument); several -> a folder the user picks (one PNG per page).
    fun savePagesAsImages(from: Editor, pages: List<Int>) {
        if (pages.isEmpty()) return
        pendingExportPages = PendingPages(from, pages)
        if (pages.size == 1) savePageImageLauncher.launch("%s-p%02d.png".format(from.title, pages[0] + 1))
        else savePagesImagesTreeLauncher.launch(null)
    }

    // Every shortcut acts on the focused pane, so the same KeyActions serve both of them.
    editor.keyActions = remember {
        Editor.KeyActions(
            newNote = { guarded { editor.active.newNote() } },
            open = {
                if (editor.browseRoot != null) openLauncher.launch(arrayOf("*/*"))
                else { backstageView = com.pnotes.ui.BackstageView.HOME; guardedAll { editor.goHomeAll() } }
            },
            save = { saveOrPrompt() },
            saveAs = { launchSaveAs(editor.active) },
            exportPdf = {
                val from = editor.active
                runPdfExport(from.title, shareDir = false,
                    render = { o, prog, cancel -> from.exportPdf(o, prog, cancel) },
                    onReady = { temp -> pendingExportTemp = temp; savePdfLauncher.launch("${from.title}.pdf") })
            },
            preferences = { backstageView = com.pnotes.ui.BackstageView.PREFERENCES; guardedAll { editor.goHomeAll() } },
            fullscreen = onToggleFullscreen,
            newTab = { guarded { editor.active.newNote() } },
            closeTab = {
                val target = editor.active
                target.tabManager.activeTab?.let { curTab ->
                    guarded(target) { target.tabManager.closeTab(curTab.id) }
                }
            },
            nextTab = {
                val target = editor.active
                val tabs = target.tabManager.tabs
                val curIdx = tabs.indexOfFirst { it.id == target.tabManager.activeTabId }
                if (curIdx >= 0 && tabs.size > 1) {
                    val nextIdx = (curIdx + 1) % tabs.size
                    target.tabManager.selectTab(tabs[nextIdx].id)
                }
            },
            prevTab = {
                val target = editor.active
                val tabs = target.tabManager.tabs
                val curIdx = tabs.indexOfFirst { it.id == target.tabManager.activeTabId }
                if (curIdx >= 0 && tabs.size > 1) {
                    val prevIdx = (curIdx - 1 + tabs.size) % tabs.size
                    target.tabManager.selectTab(tabs[prevIdx].id)
                }
            },
        )
    }

    LaunchedEffect(editor.message) {
        editor.message?.let {
            snackbar.showSnackbar(it)
            editor.message = null
        }
    }
    // The second pane raises its own messages; surface them through the same snackbar.
    val secondaryMessage = editor.secondary?.message
    LaunchedEffect(secondaryMessage) {
        if (secondaryMessage != null) {
            snackbar.showSnackbar(secondaryMessage)
            editor.secondary?.message = null
        }
    }

    // A PDF opened from another app ("Open with"/Share): set up a pending import and land on the
    // backstage so its name dialog appears, exactly like the in-app "Import PDF" button. With no
    // folder chosen yet, fall back to App storage so the explorer renders and the note autosaves.
    LaunchedEffect(importPdfUri) {
        val src = importPdfUri ?: return@LaunchedEffect
        onImportConsumed() // consume once; the body has no suspend point so it runs to completion
        val stem = com.pnotes.core.util.Paths.stem(displayNameOf(resolver, src) ?: "Document")
        guardedAll {
            if (editor.browseRoot == null) editor.useInternalStorage()
            editor.requestImport(com.pnotes.ui.ImportKind.PDF, stem, src.toString())
            backstageView = com.pnotes.ui.BackstageView.HOME
            editor.goHomeAll()
        }
    }

    // In fullscreen the window runs edge to edge and the swipe-in system bars are transient, so
    // zero the content insets: otherwise their inset animates 0 -> N -> 0 and resizes the canvas,
    // forcing a re-render every time the bars hide. Non-fullscreen keeps the normal bar insets.
    // The IME inset joins both cases so a DOCKED keyboard lifts the content (the bottom format
    // bar rides right above it); a floating keyboard reports no inset and the bar stays at the
    // bottom. Insets are consumed below so inner imePadding fields don't pad a second time.
    val contentInsets = if (fullscreen) WindowInsets.ime else WindowInsets.systemBars.union(WindowInsets.ime)
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = contentInsets) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner).consumeWindowInsets(contentInsets)) {
            // BASE LAYER: backstage is the root of the stack — always present underneath.
            com.pnotes.ui.Backstage(
                editor = editor,
                view = backstageView,
                onSelectView = { backstageView = it },
                onExitApp = { showExitConfirm = true },
                onImportCodeTheme = { importCodeThemeLauncher.launch(arrayOf("*/*")) },
                onImportFont = { importFontLauncher.launch(arrayOf("*/*")) },
                onOpenSystem = { openLauncher.launch(arrayOf("*/*")) },
                onImportPdf = { importPdfLauncher.launch(arrayOf("application/pdf")) },
                onOpenFile = { uri, name -> guarded(editor) { openTreeFile(uri, name) } },
                onPickRoot = { pickRootLauncher.launch(null) },
                onShareFile = { uri -> pendingShareUri = uri; showShareChooser = true },
                onSaveCopyFile = { uri -> pendingSaveCopyUri = uri; saveCopyLauncher.launch(com.pnotes.core.util.DocumentKind.withSuffix(stemOf(uri), com.pnotes.core.util.DocumentKind.NOTE)) },
                onExportFilePdf = { uri ->
                    runPdfExport(stemOf(uri), shareDir = false,
                        render = { o, prog, cancel -> editor.exportFileToPdf(uri, o, prog, cancel) },
                        onReady = { temp -> pendingExportTemp = temp; savePdfLauncher.launch("${stemOf(uri)}.pdf") })
                },
                onOpenSplit = { first, second -> guardedAll { openSplit(first, second) } },
            )

            // TOP LAYER: the open panes (toolbar + canvas each), pushed over backstage. Back acts on
            // the focused pane; its handlers live here so — composed after backstage — they win while
            // a note is open.
            val focused = editor.active
            if (focused.noteOpen) {
                // While a text box is open, Back commits-or-dismisses it (and hides the keyboard).
                BackHandler(enabled = focused.editingField != null) { focused.commitText() }
                // A live flow caret session ends first (flushing its typing burst).
                BackHandler(enabled = focused.flowEditingActive) { focused.flowText.endSession() }
                // Otherwise Back closes that pane (guarded for unsaved edits); the other one stays.
                BackHandler(enabled = focused.editingField == null && !focused.flowEditingActive) {
                    guarded(focused) { focused.goHome() }
                }
            }

            val actions = PaneActions(
                onToggleFullscreen = onToggleFullscreen,
                onOpenBackstage = {
                    backstageView = com.pnotes.ui.BackstageView.HOME
                    guardedAll { editor.goHomeAll() }
                },
                onClosePane = { pane -> guarded(pane) { pane.goHome() } },
                onInsertImage = { pane, at ->
                    pendingInsert = PendingInsert(pane, at)
                    insertImageLauncher.launch(arrayOf("image/*"))
                },
                onInsertCanvasImage = { pane, at ->
                    pendingCanvasInsert = PendingInsert(pane, at)
                    insertCanvasImageLauncher.launch(arrayOf("image/*"))
                },
                onTakePhoto = { pane ->
                    pendingInsert = PendingInsert(pane, null)
                    takePhotoLauncher.launch(null)
                },
                onAddStickers = { addStickersLauncher.launch(arrayOf("image/*")) },
                onPresent = { presentPane = editor.active },
                onSharePages = { pane, pages, asPdf -> sharePages(pane, pages, asPdf) },
                onSavePagesAsPdf = { pane, pages -> savePagesAsPdf(pane, pages) },
                onSavePagesAsImages = { pane, pages -> savePagesAsImages(pane, pages) },
                onOpenInSplitTab = { pane, tab ->
                    val uri = tab.uri
                    if (uri != null) {
                        editor.splitRatio = 0.5f
                        val second = editor.secondaryPane()
                        scope.launch { openInto(second, uri, tab.title) }
                    }
                },
                onImportPdf = { importPdfLauncher.launch(arrayOf("application/pdf")) },
                guarded = { target, act -> guarded(target, act) },
                openNoteInto = { target, uri -> scope.launch { openInto(target, uri) } },
            )
            val isNoteOpen = editor.noteOpen || editor.secondary?.noteOpen == true
            androidx.compose.animation.AnimatedVisibility(
                visible = isNoteOpen,
                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) +
                    androidx.compose.animation.scaleIn(
                        initialScale = 0.94f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                        ),
                    ),
                exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(180)) +
                    androidx.compose.animation.scaleOut(
                        targetScale = 0.96f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                        ),
                    ),
            ) {
                SplitHost(editor, actions)
            }
        }
    }
    // ── Exit confirmation dialog ──
    if (showExitConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { androidx.compose.material3.Text("Exit PNotes?") },
            text = { androidx.compose.material3.Text("Do you really want to exit the app?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showExitConfirm = false
                    (context as? android.app.Activity)?.finish()
                }) { androidx.compose.material3.Text("Exit") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExitConfirm = false }) {
                    androidx.compose.material3.Text("Cancel")
                }
            },
        )
    }
    if (showShareChooser) {
        val shareUri = pendingShareUri
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showShareChooser = false; pendingShareUri = null },
            title = { androidx.compose.material3.Text("Share note") },
            text = { androidx.compose.material3.Text("Share “${shareUri?.let { stemOf(it) } ?: ""}” as:") },
            confirmButton = {
                androidx.compose.foundation.layout.Row {
                    androidx.compose.material3.TextButton(onClick = { showShareChooser = false; pendingShareUri = null; shareUri?.let { shareFile(it, asPdf = false) } }) {
                        androidx.compose.material3.Text("PNotes file")
                    }
                    androidx.compose.material3.TextButton(onClick = { showShareChooser = false; pendingShareUri = null; shareUri?.let { shareFile(it, asPdf = true) } }) {
                        androidx.compose.material3.Text("PDF")
                    }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showShareChooser = false; pendingShareUri = null }) {
                    androidx.compose.material3.Text("Cancel")
                }
            },
        )
    }
    presentPane?.let { pane ->
        com.pnotes.ui.PresentationDialog(editor = pane, onDismiss = { presentPane = null })
    }
    guardAction?.let { request ->
        val guarded = request.editor
        val action = request.action
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { guardAction = null },
            title = { androidx.compose.material3.Text("Unsaved changes") },
            text = { androidx.compose.material3.Text("Save changes to “${guarded.title}” before continuing?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    guardAction = null
                    val uri = guarded.currentUri
                    if (uri != null) {
                        guarded.saveTo(uri)
                        action()
                    } else {
                        pendingAfterSave = action
                        launchSaveAs(guarded)
                    }
                }) { androidx.compose.material3.Text("Save") }
            },
            dismissButton = {
                androidx.compose.foundation.layout.Row {
                    androidx.compose.material3.TextButton(onClick = { guardAction = null; action() }) {
                        androidx.compose.material3.Text("Discard")
                    }
                    androidx.compose.material3.TextButton(onClick = { guardAction = null }) {
                        androidx.compose.material3.Text("Cancel")
                    }
                }
            },
        )
    }
    exportProgress?.let { (done, total) ->
        PdfExportDialog(done = done, total = total, onCancel = {
            exportCancel?.set(true) // abort this export's page loop AND its write stream
            exportProgress = null   // hide at once; the job then discards the half-written temp
        })
    }
    if (editor.importing) {
        // OPEN imports an .pnote; everything else is the PDF import the loader is named for.
        PdfImportDialog(
            isPdf = editor.pendingImport?.kind != com.pnotes.ui.ImportKind.OPEN,
            onCancel = { editor.cancelImportInProgress() }, // the stream-copy stops at its next buffer
        )
    }
    // Tapping a note reads it off-thread (editor.opening). Only show the spinner once the read has run
    // long enough to matter, so opening a small note never flashes a dialog; a big PDF gets the loader.
    var showOpening by remember { mutableStateOf(false) }
    val opening = editor.opening || editor.secondary?.opening == true
    LaunchedEffect(opening) {
        if (opening) { delay(160); showOpening = opening } else showOpening = false
    }
    if (showOpening && opening) {
        SpinnerDialog("Opening note…", onCancel = {
            // Discard whichever pane's note is still being read when it returns.
            editor.cancelOpenInProgress()
            editor.secondary?.cancelOpenInProgress()
            showOpening = false // dismiss at once; opening clears as the reads unwind
        })
    }
    // A dirty note is flushed off-thread on close/pause; show the saving overlay only once the write is
    // slow enough to matter, so closing a small note never flashes a dialog.
    var showSaving by remember { mutableStateOf(false) }
    val saving = editor.savingNote || editor.secondary?.savingNote == true
    LaunchedEffect(saving) {
        if (saving) { delay(160); showSaving = saving } else showSaving = false
    }
    if (showSaving && saving) SavingDialog()
}

/** A pending "unsaved changes" prompt: the pane it asks about, and what to run once it's settled. */
private class GuardRequest(val editor: Editor, val action: () -> Unit)

/** A picked image on its way into [editor], at [at] when a long-press chose the spot. */
private class PendingInsert(val editor: Editor, val at: com.pnotes.core.geometry.Pt?)

/** Side-panel pages of [editor] waiting on a SAF "Save as" destination. */
private class PendingPages(val editor: Editor, val pages: List<Int>)

/**
 * What a pane can ask the screen around it to do: open a SAF picker, run an export, or change the
 * window. Each callback takes the pane it is acting for, because in a split there are two of them
 * and a picker's result has to come back to the one that opened it.
 */
private class PaneActions(
    val onToggleFullscreen: () -> Unit,
    /** Leave for the backstage, closing every open pane. */
    val onOpenBackstage: () -> Unit,
    /** Close just this pane, leaving the other one to fill the window. */
    val onClosePane: (Editor) -> Unit,
    val onInsertImage: (Editor, com.pnotes.core.geometry.Pt?) -> Unit,
    val onInsertCanvasImage: (Editor, com.pnotes.core.geometry.Pt?) -> Unit,
    val onTakePhoto: (Editor) -> Unit = {},
    val onAddStickers: () -> Unit,
    val onPresent: () -> Unit,
    val onSharePages: (Editor, List<Int>, Boolean) -> Unit,
    val onSavePagesAsPdf: (Editor, List<Int>) -> Unit,
    val onSavePagesAsImages: (Editor, List<Int>) -> Unit,
    val onOpenInSplitTab: (Editor, com.pnotes.tabs.NoteTab) -> Unit = { _, _ -> },
    val onImportPdf: () -> Unit = {},
    val guarded: (Editor, () -> Unit) -> Unit = { _, action -> action() },
    val openNoteInto: (Editor, String) -> Unit = { _, _ -> },
)

/** Neither pane of a split may be squeezed below this share of the split axis. */
private const val MIN_PANE_RATIO = 0.18f

/** How wide the divider is to a finger. Mostly empty around the line, so it is easy to catch. */
private val DIVIDER = 16.dp

/** The accent line drawn down the middle of the divider, the same weight as a pane's focus line. */
private val DIVIDER_LINE = 2.dp

/**
 * Makes the open panes opaque to touch. Compose stops hit-testing lower siblings once a higher one
 * is hit, so this node — hit anywhere over the panes — keeps a tap that no child handled from
 * reaching the backstage composed underneath. It never consumes, so children still see every event.
 */
private val SwallowTouches = Modifier.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Initial)
    }
}

/**
 * Lays the open panes over the backstage. Both open is a split: side by side in landscape, stacked
 * in portrait, with a draggable divider between them. One open is that pane full-screen, and none
 * leaves the backstage showing.
 *
 * The panes are placed by offset and size inside one Box rather than by a Row that becomes a Column,
 * so a pane keeps its composition node — and with it its canvas view, raster caches and GL context —
 * when the split opens, closes or the device turns.
 */
@Composable
private fun SplitHost(editor: Editor, actions: PaneActions) {
    val second = editor.secondary
    val split = editor.noteOpen && second?.noteOpen == true
    // A pane that closed on its own leaves the other one full-screen; once neither is open the
    // second editor is released, freeing its canvas and GL surfaces.
    LaunchedEffect(editor.noteOpen, second?.noteOpen) {
        if (second != null && !second.noteOpen) editor.releaseClosedSecondary()
    }
    if (!editor.noteOpen && second?.noteOpen != true) return

    val sideBySide = LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val ratio = editor.splitRatio.coerceIn(MIN_PANE_RATIO, 1f - MIN_PANE_RATIO)
    BoxWithConstraints(Modifier.fillMaxSize().then(SwallowTouches)) {
        val fullW = maxWidth
        val fullH = maxHeight
        val full = if (sideBySide) fullW else fullH
        // The panes meet along the split axis and take all of it; the divider is not a gap between
        // them but a handle floating over the seam, so there is no strip of its own colour to show
        // beside the toolbars. Across the axis both run full.
        val firstExtent = if (split) full * ratio else full
        val secondExtent = if (split) full - firstExtent else full

        /** Sizes a pane to [extent] along the split axis and the whole window across it. */
        fun paneSize(extent: Dp) = Modifier.size(
            if (sideBySide) extent else fullW,
            if (sideBySide) fullH else extent,
        )

        /** Offsets a pane [along] the split axis from the top-left of the editor area. */
        fun paneOffset(along: Dp) = Modifier.offset(
            x = if (sideBySide) along else 0.dp,
            y = if (sideBySide) 0.dp else along,
        )

        if (editor.noteOpen) {
            EditorPane(
                editor = editor,
                app = editor,
                actions = actions,
                closable = split,
                modifier = paneSize(firstExtent),
            )
        }
        if (second?.noteOpen == true) {
            EditorPane(
                editor = second,
                app = editor,
                actions = actions,
                closable = split,
                modifier = paneOffset(if (split) firstExtent else 0.dp).then(paneSize(secondExtent)),
            )
        }
        // Composed last so it sits over both panes and catches the drag before either of them.
        if (split) {
            SplitDivider(
                sideBySide = sideBySide,
                extentPx = with(LocalDensity.current) { full.toPx() },
                ratio = editor.splitRatio,
                onRatio = { editor.splitRatio = it },
                modifier = paneOffset((firstExtent - DIVIDER / 2).coerceAtLeast(0.dp))
                    .then(paneSize(DIVIDER)),
            )
        }
    }
}

/**
 * Which way a panel grows out of the edge its rail is docked to: a card beside a left-docked rail
 * opens towards the middle of the screen, one beside a bottom-docked rail opens upwards.
 */
private fun com.pnotes.ui.RailDockEdge.panelAnchor(): PanelAnchor = when (this) {
    com.pnotes.ui.RailDockEdge.TOP -> PanelAnchor.TOP
    com.pnotes.ui.RailDockEdge.BOTTOM -> PanelAnchor.BOTTOM
    com.pnotes.ui.RailDockEdge.LEFT -> PanelAnchor.START
    com.pnotes.ui.RailDockEdge.RIGHT -> PanelAnchor.END
}

private fun clampTopLeft(
    rawTopLeft: Float,
    itemSize: Float,
    parentSize: Float,
    margin: Float,
    minOffset: Float = margin,
): Float {
    val minVal = minOffset
    val maxVal = (parentSize - margin - itemSize).coerceAtLeast(minVal)
    return if (minVal >= maxVal) minVal else rawTopLeft.coerceIn(minVal, maxVal)
}

private fun safeDockCenter(
    edge: com.pnotes.ui.RailDockEdge,
    ratio: Float,
    dockW: Float,
    dockH: Float,
    parentW: Float,
    parentH: Float,
    marginPx: Float,
    thicknessPx: Float,
    topHeaderHeightPx: Float = 0f,
): Pair<Float, Float> {
    val isVertical = edge == com.pnotes.ui.RailDockEdge.LEFT || edge == com.pnotes.ui.RailDockEdge.RIGHT
    val cx: Float
    val cy: Float
    if (isVertical) {
        cx = if (edge == com.pnotes.ui.RailDockEdge.LEFT) marginPx + thicknessPx / 2f
             else parentW - marginPx - thicknessPx / 2f
        val minCY = topHeaderHeightPx + marginPx + dockH / 2f
        val maxCY = (parentH - marginPx - dockH / 2f).coerceAtLeast(minCY)
        cy = if (minCY >= maxCY) (topHeaderHeightPx + parentH) / 2f else minCY + ratio * (maxCY - minCY)
    } else {
        cy = if (edge == com.pnotes.ui.RailDockEdge.TOP) topHeaderHeightPx + marginPx + thicknessPx / 2f
             else parentH - marginPx - thicknessPx / 2f
        val minCX = marginPx + dockW / 2f
        val maxCX = (parentW - marginPx - dockW / 2f).coerceAtLeast(minCX)
        cx = if (minCX >= maxCX) parentW / 2f else minCX + ratio * (maxCX - minCX)
    }
    return Pair(cx, cy)
}

/**
 * One pane: its toolbar over its canvas, with the menus and overlays that belong to that document.
 * A paged note and an infinite canvas get different chrome, since the paged toolbar is mostly pages,
 * viewing modes and text, none of which mean anything on a canvas.
 *
 * [app] is the primary editor, which owns the split; [editor] is this pane's own. Touching anywhere
 * in the pane gives it the focus, so the keyboard and the file actions follow the pen.
 */
@Composable
private fun EditorPane(
    editor: Editor,
    app: Editor,
    actions: PaneActions,
    closable: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val focusRequester = remember { FocusRequester() }
    val focused = app.active === editor
    // This pane owns the keyboard while it is the focused one; (re)grab it as that changes.
    LaunchedEffect(focused, editor.noteOpen) {
        if (focused && editor.noteOpen) runCatching { focusRequester.requestFocus() }
    }
    val takeFocus = Modifier.pointerInput(editor) {
        awaitPointerEventScope {
            while (true) {
                // Initial pass and never consumed: the pane notices the touch, the canvas still gets it.
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press) app.focusPane(editor.pane)
            }
        }
    }
    Column(
        modifier = modifier
            .background(palette.bg.toComposeColor())
            .then(takeFocus)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { ke ->
                ke.type == KeyEventType.KeyDown && editor.handleKeyDown(ke.nativeKeyEvent)
            },
    ) {
        val onClose = if (closable) ({ actions.onClosePane(editor) }) else null
        // In a split, a hairline over the toolbar marks the pane the pen and the keyboard are in.
        if (closable) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background((if (focused) palette.accent else palette.border).toComposeColor()),
            )
        }
        if (editor.canvasOpen) {
            val canvas = editor.infinite
            val context = androidx.compose.ui.platform.LocalContext.current
            var showCanvasBrowseDrawer by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                    AndroidView(
                        factory = { detached(canvas.view) },
                        modifier = Modifier
                            .fillMaxSize(),
                        update = { it.publish() },
                    )
                    com.pnotes.ui.SelectionMenu(canvas)
                    com.pnotes.ui.LongPressMenu(canvas, onInsertImageAt = { c -> actions.onInsertCanvasImage(editor, c) })
                    com.pnotes.ui.CanvasDebugOverlay(canvas)
                    com.pnotes.ui.FloatingSideRails(
                        editor = canvas,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }
                com.pnotes.ui.PnotesCanvasTopBar(
                    editor = canvas,
                    isHeaderShrunk = editor.headerShrunk,
                    onToggleHeaderShrunk = { editor.toggleHeaderShrunk() },
                    onOpenBackstage = actions.onOpenBackstage,
                    onInsertImage = { actions.onInsertCanvasImage(editor, null) },
                    onShare = { actions.onSharePages(editor, listOf(editor.pageIndex), false) },
                    onToggleFullscreen = actions.onToggleFullscreen,
                    onClosePane = onClose,
                    isTabBarVisible = editor.tabManager.tabs.size > 1 && editor.tabBarVisible,
                    onNewTab = { actions.guarded(editor) { editor.newNote() } },
                    onNewCanvasTab = { actions.guarded(editor) { editor.newCanvas() } },
                    onImportPdf = actions.onImportPdf,
                    onOpenBrowseDrawer = { showCanvasBrowseDrawer = true },
                    modifier = Modifier.align(Alignment.TopCenter).zIndex(if (editor.headerShrunk) 40f else 20f),
                )

                // Scrim for Canvas Browse Drawer (Dims whole screen including TopBar)
                val canvasBrowseScrimAlpha by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (showCanvasBrowseDrawer) 1f else 0f,
                    animationSpec = com.pnotes.ui.theme.LocalMotion.current.effect(),
                    label = "canvasBrowseScrimAlpha",
                )
                if (canvasBrowseScrimAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = canvasBrowseScrimAlpha }
                            .background(Color.Black.copy(alpha = if (com.pnotes.ui.isGlassDark()) 0.5f else 0.35f))
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null,
                                onClick = { showCanvasBrowseDrawer = false }
                            )
                            .zIndex(45f)
                    )
                }

                com.pnotes.ui.PnotesNotesSideDrawer(
                    visible = showCanvasBrowseDrawer,
                    editor = editor,
                    onClose = { showCanvasBrowseDrawer = false },
                    onOpenNote = { entry ->
                        showCanvasBrowseDrawer = false
                        val isCanvas = com.pnotes.core.util.DocumentKind.ofName(entry.name) == com.pnotes.core.util.DocumentKind.CANVAS
                        editor.openInNewTab(entry.documentUri, com.pnotes.core.util.DocumentKind.stripSuffix(entry.name), isCanvas)
                    },
                    onOpenInNewTab = { entry ->
                        showCanvasBrowseDrawer = false
                        val isCanvas = com.pnotes.core.util.DocumentKind.ofName(entry.name) == com.pnotes.core.util.DocumentKind.CANVAS
                        editor.openInNewTab(entry.documentUri, com.pnotes.core.util.DocumentKind.stripSuffix(entry.name), isCanvas)
                    },
                    onOpenInSplit = { entry ->
                        showCanvasBrowseDrawer = false
                        actions.onOpenInSplitTab(editor, com.pnotes.tabs.NoteTab(
                            uri = entry.documentUri,
                            title = com.pnotes.core.util.DocumentKind.stripSuffix(entry.name),
                            kind = if (com.pnotes.core.util.DocumentKind.ofName(entry.name) == com.pnotes.core.util.DocumentKind.CANVAS) com.pnotes.core.util.DocumentKind.CANVAS else com.pnotes.core.util.DocumentKind.NOTE
                        ))
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .zIndex(50f),
                )

                BackHandler(enabled = showCanvasBrowseDrawer) {
                    showCanvasBrowseDrawer = false
                }
            }
        } else {
            val context = androidx.compose.ui.platform.LocalContext.current
            val density = androidx.compose.ui.platform.LocalDensity.current
            var showStrokeWidthSlider by remember { mutableStateOf(false) }
            var showEraserSettings by remember { mutableStateOf(false) }
            var showPageStyles by remember { mutableStateOf(false) }
            var showPageAngle by remember { mutableStateOf(false) }
            var showColorPicker by remember { mutableStateOf(false) }
            var isAddingNewColor by remember { mutableStateOf(false) }
            var showMargins by remember { mutableStateOf(false) }
            var showViewOptions by remember { mutableStateOf(false) }
            var showPageJump by remember { mutableStateOf(false) }
            var showBrowseNotesDrawer by remember { mutableStateOf(false) }
            var showImageGalleryDrawer by remember { mutableStateOf(false) }
            var activePopupTool by remember { mutableStateOf<Tool?>(null) }

            fun dismissAllPopovers(closeDrawers: Boolean = false) {
                activePopupTool = null
                showStrokeWidthSlider = false
                showEraserSettings = false
                showPageStyles = false
                showPageAngle = false
                showColorPicker = false
                showMargins = false
                showViewOptions = false
                showPageJump = false
                if (closeDrawers) {
                    showBrowseNotesDrawer = false
                    showImageGalleryDrawer = false
                }
            }

            var topHeaderOffsetPx by remember { mutableFloatStateOf(if (editor.headerShrunk) 0f else with(density) { 58.dp.toPx() }) }

	            val savedDockEdge = remember { com.pnotes.ui.ToolbarPreferences.getDockEdge(context) }
	            val savedRailEdge = remember { com.pnotes.ui.ToolbarPreferences.getRailEdge(context) }
	            val savedIsCombined = remember { com.pnotes.ui.ToolbarPreferences.getIsCombined(context) }
	            val savedIsFullLength = remember { com.pnotes.ui.ToolbarPreferences.getIsFullLength(context) }

	            var railDockEdge by rememberSaveable { mutableStateOf(if (savedIsCombined) savedDockEdge else savedRailEdge) }
	            var isRailDragging by remember { mutableStateOf(false) }
	            val railCenterX = remember { Animatable(0f) }
	            val railCenterY = remember { Animatable(0f) }
	            var railWidthPx by remember { mutableIntStateOf(0) }
	            var railHeightPx by remember { mutableIntStateOf(0) }
	            var isRailPositioned by remember { mutableStateOf(false) }
	            val railMorphProgress = remember { Animatable(0f) }
	            var railGrabOffsetX by remember { mutableFloatStateOf(0f) }
	            var railGrabOffsetY by remember { mutableFloatStateOf(0f) }
	            var railFingerX by remember { mutableFloatStateOf(0f) }
	            var railFingerY by remember { mutableFloatStateOf(0f) }
	            var railHorizontalLength by remember { mutableFloatStateOf(0f) }
	            var railVerticalLength by remember { mutableFloatStateOf(0f) }

	            // Bottom Dock Draggable & Magnetic Edge-Snapping States
	            val coroutineScope = rememberCoroutineScope()
	            var dockDockEdge by rememberSaveable { mutableStateOf(savedDockEdge) }
	            var isDockDragging by remember { mutableStateOf(false) }
	            val dockCenterX = remember { Animatable(0f) }
	            val dockCenterY = remember { Animatable(0f) }
	            var dockWidthPx by remember { mutableIntStateOf(0) }
	            var dockHeightPx by remember { mutableIntStateOf(0) }
	            var isDockPositioned by remember { mutableStateOf(false) }
	            val dockMorphProgress = remember { Animatable(0f) }
	            var dockGrabOffsetX by remember { mutableFloatStateOf(0f) }
	            var dockGrabOffsetY by remember { mutableFloatStateOf(0f) }
	            var dockFingerX by remember { mutableFloatStateOf(0f) }
	            var dockFingerY by remember { mutableFloatStateOf(0f) }
	            var dockHorizontalLength by remember { mutableFloatStateOf(0f) }
	            var dockVerticalLength by remember { mutableFloatStateOf(0f) }
	            var isFullLengthToolbar by rememberSaveable { mutableStateOf(savedIsFullLength) }

            androidx.compose.runtime.LaunchedEffect(editor.tool) {
                if (!editor.tool.isStroke) showStrokeWidthSlider = false
                if (editor.tool != Tool.ERASER) showEraserSettings = false
                if (activePopupTool != null && activePopupTool != editor.tool) {
                    activePopupTool = null
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // 1. WORKSPACE ROW (Canvas Area + Right Page Manager)
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // CANVAS AREA (Takes full available space, zero artificial margin/padding clipping)
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clipToBounds()
                    ) {
                    val parentW = constraints.maxWidth.toFloat()
                    val parentH = constraints.maxHeight.toFloat()
                    val marginPx = with(density) { 10.dp.toPx() }

                    AndroidView(
                        factory = { detached(editor.view) },
                        modifier = Modifier
                            .fillMaxSize(),
                        update = { it.requestRender() }, // repaint on (re)attach so a push never flashes blank
                    )
                    editor.editingField?.let { field ->
                        com.pnotes.ui.TextEditorOverlay(editor, field)
                    }
                    com.pnotes.ui.SelectionMenu(editor)
                    editor.tableStyleTarget?.let { target ->
                        androidx.compose.ui.window.Dialog(onDismissRequest = { editor.dismissTableStyle() }) {
                            com.pnotes.ui.TableAdjustStylePopup(
                                table = target.table,
                                onTableModified = {
                                    editor.state.invalidateAllCaches()
                                    editor.view.requestRender()
                                },
                                onDeleteTable = {
                                    editor.deleteSelection()
                                    editor.dismissTableStyle()
                                },
                                onDismiss = { editor.dismissTableStyle() },
                            )
                        }
                    }
                    editor.tableCellTarget?.let { target ->
                        com.pnotes.ui.TableCellTextDialog(
                            row = target.row,
                            col = target.col,
                            initialText = target.currentText,
                            onConfirm = { newText ->
                                if (newText.isEmpty()) {
                                    target.table.cellTexts.remove(target.row to target.col)
                                } else {
                                    target.table.cellTexts[target.row to target.col] = newText
                                }
                                val page = editor.state.document.pages.getOrNull(editor.pageIndex)
                                if (page != null) editor.state.invalidatePage(page)
                                editor.state.document.dirty = true
                                editor.state.invalidateAllCaches()
                                editor.view.requestRender()
                                editor.dismissTableCell()
                            },
                            onDismiss = { editor.dismissTableCell() },
                        )
                    }
                    com.pnotes.ui.ScreenshotMenu(editor)
                    com.pnotes.ui.TextStyleBar(editor)
                    com.pnotes.ui.LongPressMenu(editor, onInsertImageAt = { c -> actions.onInsertImage(editor, c) })
                    com.pnotes.ui.FlowEditMenu(editor)
                    ZoomLockHint(editor)
                    RefiningPdfHint(editor)

                    val circleSizePx = with(density) { 56.dp.toPx() }
                    val circleRadius = circleSizePx / 2f
                    val dockThicknessPx = with(density) { 44.dp.toPx() }

                    val isFullLengthActive = isFullLengthToolbar && !isDockDragging && dockMorphProgress.value <= 0.05f
                    val effectiveDockThickness = (if (isFullLengthToolbar) dockThicknessPx else (dockThicknessPx + marginPx)).toDouble()
                    val isCombined = dockDockEdge == railDockEdge
                    androidx.compose.runtime.LaunchedEffect(editor.view.state, isFullLengthActive, dockDockEdge, railDockEdge, isCombined, dockThicknessPx, marginPx) {
                        val st = editor.view.state
                        if (isFullLengthActive) {
                            when (dockDockEdge) {
                                com.pnotes.ui.RailDockEdge.LEFT -> st?.setViewportInsets(dockThicknessPx.toDouble(), 0.0, 0.0, 0.0)
                                com.pnotes.ui.RailDockEdge.RIGHT -> st?.setViewportInsets(0.0, 0.0, dockThicknessPx.toDouble(), 0.0)
                                com.pnotes.ui.RailDockEdge.TOP -> st?.setViewportInsets(0.0, dockThicknessPx.toDouble(), 0.0, 0.0)
                                com.pnotes.ui.RailDockEdge.BOTTOM -> st?.setViewportInsets(0.0, 0.0, 0.0, dockThicknessPx.toDouble())
                            }
                        } else {
                            st?.setViewportInsets(0.0, 0.0, 0.0, 0.0)
                        }
                        val leftInset = if (dockDockEdge == com.pnotes.ui.RailDockEdge.LEFT || (!isCombined && railDockEdge == com.pnotes.ui.RailDockEdge.LEFT)) effectiveDockThickness else 0.0
                        val rightInset = if (dockDockEdge == com.pnotes.ui.RailDockEdge.RIGHT || (!isCombined && railDockEdge == com.pnotes.ui.RailDockEdge.RIGHT)) effectiveDockThickness else 0.0
                        val topInset = if (dockDockEdge == com.pnotes.ui.RailDockEdge.TOP || (!isCombined && railDockEdge == com.pnotes.ui.RailDockEdge.TOP)) effectiveDockThickness else 0.0
                        val bottomInset = if (dockDockEdge == com.pnotes.ui.RailDockEdge.BOTTOM || (!isCombined && railDockEdge == com.pnotes.ui.RailDockEdge.BOTTOM)) effectiveDockThickness else 0.0
                        st?.setToolbarInsets(leftInset, topInset, rightInset, bottomInset)
                        editor.view.invalidate()
                        editor.view.requestRender()
                    }

                    // Fresh Adaptive Quick-Tool Rail (Draggable & Magnetic Edge-Snapping to Left, Top, Right, or Bottom edge; morphs to circle on drag)
                    androidx.compose.runtime.LaunchedEffect(parentW, parentH) {
                        if (parentW > 0 && parentH > 0) {
                            val isTop = railDockEdge == com.pnotes.ui.RailDockEdge.TOP || railDockEdge == com.pnotes.ui.RailDockEdge.BOTTOM
                            val defaultRailLen = with(density) { 300.dp.toPx() }
                            val curRailW = if (isTop) (if (railHorizontalLength > 0f) railHorizontalLength else defaultRailLen) else dockThicknessPx
                            val curRailH = if (isTop) dockThicknessPx else (if (railVerticalLength > 0f) railVerticalLength else defaultRailLen)
                            val (cx, cy) = safeDockCenter(
                                edge = railDockEdge,
                                ratio = com.pnotes.ui.ToolbarPreferences.getRailRatio(context),
                                dockW = curRailW,
                                dockH = curRailH,
                                parentW = parentW,
                                parentH = parentH,
                                marginPx = marginPx,
                                thicknessPx = dockThicknessPx,
                                topHeaderHeightPx = topHeaderOffsetPx,
                            )
                            if (!isRailPositioned) {
                                railCenterX.snapTo(cx)
                                railCenterY.snapTo(cy)
                                isRailPositioned = true
                            } else if (!isRailDragging) {
                                railCenterX.snapTo(cx)
                                railCenterY.snapTo(cy)
                            }
                        }
                    }

                    val railDragModifier = if (isFullLengthToolbar) Modifier else Modifier.pointerInput(parentW, parentH) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                var isDragActive = false
                                var accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                val pointerId = down.id

                                val touchGrabX = down.position.x
                                val touchGrabY = down.position.y
                                railGrabOffsetX = touchGrabX
                                railGrabOffsetY = touchGrabY

                                val isTop = railDockEdge == com.pnotes.ui.RailDockEdge.TOP || railDockEdge == com.pnotes.ui.RailDockEdge.BOTTOM
                                val defaultRailLen = with(density) { 300.dp.toPx() }
                                val curRailW = if (isTop) (if (railHorizontalLength > 0f) railHorizontalLength else defaultRailLen) else dockThicknessPx
                                val curRailH = if (isTop) dockThicknessPx else (if (railVerticalLength > 0f) railVerticalLength else defaultRailLen)

                                val startRailLeft = railCenterX.value - curRailW / 2f
                                val startRailTop = railCenterY.value - curRailH / 2f

                                railFingerX = startRailLeft + touchGrabX
                                railFingerY = startRailTop + touchGrabY

                                val initialRailEdge = railDockEdge
                                val oneCmPx = with(density) { 64.dp.toPx() }

                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                    if (!change.pressed) {
                                        if (isDragActive) {
                                            change.consume()
                                            isRailDragging = false

                                            val morphT = railMorphProgress.value.coerceIn(0f, 1f)
                                            val curW = androidx.compose.ui.util.lerp(curRailW, circleSizePx, morphT)
                                            val curH = androidx.compose.ui.util.lerp(curRailH, circleSizePx, morphT)
                                            val curAnchorX = androidx.compose.ui.util.lerp(railGrabOffsetX, circleSizePx / 2f, morphT)
                                            val curAnchorY = androidx.compose.ui.util.lerp(railGrabOffsetY, circleSizePx / 2f, morphT)
                                            val curTopLeftX = railFingerX - curAnchorX
                                            val curTopLeftY = railFingerY - curAnchorY
                                            val currCX = curTopLeftX + curW / 2f
                                            val currCY = curTopLeftY + curH / 2f

                                            val distLeft = currCX.coerceAtLeast(0f)
                                            val distRight = (parentW - currCX).coerceAtLeast(0f)
                                            val distTop = (currCY - topHeaderOffsetPx).coerceAtLeast(0f)
                                            val distBottom = (parentH - currCY).coerceAtLeast(0f)

                                            val minDist = minOf(distLeft, distRight, distTop, distBottom)

                                            val nextRailEdge = when (minDist) {
                                                distTop -> com.pnotes.ui.RailDockEdge.TOP
                                                distBottom -> com.pnotes.ui.RailDockEdge.BOTTOM
                                                distLeft -> com.pnotes.ui.RailDockEdge.LEFT
                                                else -> com.pnotes.ui.RailDockEdge.RIGHT
                                            }

                                            val nextIsTop = nextRailEdge == com.pnotes.ui.RailDockEdge.TOP || nextRailEdge == com.pnotes.ui.RailDockEdge.BOTTOM
                                            val nextRailLen = if (nextIsTop) {
                                                if (railHorizontalLength > 0f) railHorizontalLength else defaultRailLen
                                            } else {
                                                if (railVerticalLength > 0f) railVerticalLength else defaultRailLen
                                            }

                                            val nextRailW = if (nextIsTop) nextRailLen else dockThicknessPx
                                            val nextRailH = if (nextIsTop) dockThicknessPx else nextRailLen

                                            val dropRatio: Float
                                            if (nextIsTop) {
                                                val minCX = marginPx + nextRailW / 2f
                                                val maxCX = (parentW - marginPx - nextRailW / 2f).coerceAtLeast(minCX)
                                                dropRatio = if (maxCX > minCX) ((currCX - minCX) / (maxCX - minCX)).coerceIn(0f, 1f) else 0.5f
                                            } else {
                                                val minCY = topHeaderOffsetPx + marginPx + nextRailH / 2f
                                                val maxCY = (parentH - marginPx - nextRailH / 2f).coerceAtLeast(minCY)
                                                dropRatio = if (maxCY > minCY) ((currCY - minCY) / (maxCY - minCY)).coerceIn(0f, 1f) else 0.25f
                                            }

                                            val (targetCX, targetCY) = safeDockCenter(
                                                edge = nextRailEdge,
                                                ratio = dropRatio,
                                                dockW = nextRailW,
                                                dockH = nextRailH,
                                                parentW = parentW,
                                                parentH = parentH,
                                                marginPx = marginPx,
                                                thicknessPx = dockThicknessPx,
                                                topHeaderHeightPx = topHeaderOffsetPx,
                                            )

                                            railDockEdge = nextRailEdge
                                            com.pnotes.ui.ToolbarPreferences.setRailEdge(context, nextRailEdge)
                                            com.pnotes.ui.ToolbarPreferences.setRailRatio(context, dropRatio)
                                            if (dockDockEdge == nextRailEdge) {
                                                com.pnotes.ui.ToolbarPreferences.setIsCombined(context, true)
                                            } else {
                                                com.pnotes.ui.ToolbarPreferences.setIsCombined(context, false)
                                            }

                                            coroutineScope.launch {
                                                launch {
                                                    railMorphProgress.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(
                                                            dampingRatio = 0.82f,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                                launch {
                                                    railCenterX.animateTo(
                                                        targetValue = targetCX,
                                                        animationSpec = spring(
                                                            dampingRatio = 0.82f,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                                launch {
                                                    railCenterY.animateTo(
                                                        targetValue = targetCY,
                                                        animationSpec = spring(
                                                            dampingRatio = 0.82f,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        break
                                    }

                                    val delta = change.positionChange()
                                    accumulatedDrag += delta

                                    if (!isDragActive && accumulatedDrag.getDistance() > viewConfiguration.touchSlop) {
                                        isDragActive = true
                                        isRailDragging = true
                                        coroutineScope.launch {
                                            railCenterX.stop()
                                            railCenterY.stop()
                                        }
                                    }

                                    if (isDragActive) {
                                        change.consume()
                                        railFingerX = (railFingerX + delta.x).coerceIn(0f, parentW.toFloat())
                                        railFingerY = (railFingerY + delta.y).coerceIn(0f, parentH.toFloat())

                                        val isCurrentlyBall = railMorphProgress.value > 0.5f
                                        val shouldMorphToBall = if (!isCurrentlyBall) {
                                            val currentLeft = railFingerX - railGrabOffsetX
                                            val currentTop = railFingerY - railGrabOffsetY
                                            val currentRight = currentLeft + curRailW
                                            val currentBottom = currentTop + curRailH
                                            val pulledAwayFromDockedEdge = when (initialRailEdge) {
                                                com.pnotes.ui.RailDockEdge.BOTTOM -> (parentH - marginPx - currentBottom).coerceAtLeast(0f)
                                                com.pnotes.ui.RailDockEdge.TOP -> (currentTop - (topHeaderOffsetPx + marginPx)).coerceAtLeast(0f)
                                                com.pnotes.ui.RailDockEdge.LEFT -> (currentLeft - marginPx).coerceAtLeast(0f)
                                                com.pnotes.ui.RailDockEdge.RIGHT -> (parentW - marginPx - currentRight).coerceAtLeast(0f)
                                            }
                                            pulledAwayFromDockedEdge > oneCmPx
                                        } else {
                                            val ballDistLeft = (railFingerX - circleRadius - marginPx).coerceAtLeast(0f)
                                            val ballDistRight = (parentW - marginPx - (railFingerX + circleRadius)).coerceAtLeast(0f)
                                            val ballDistTop = (railFingerY - circleRadius - (topHeaderOffsetPx + marginPx)).coerceAtLeast(0f)
                                            val ballDistBottom = (parentH - marginPx - (railFingerY + circleRadius)).coerceAtLeast(0f)
                                            val minBallDistToAnyEdge = minOf(ballDistLeft, ballDistRight, ballDistTop, ballDistBottom)
                                            minBallDistToAnyEdge > oneCmPx
                                        }

                                        coroutineScope.launch {
                                            if (shouldMorphToBall) {
                                                railMorphProgress.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.85f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            } else {
                                                railMorphProgress.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.85f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }

                                        val morphT = railMorphProgress.value.coerceIn(0f, 1f)
                                        val curW = androidx.compose.ui.util.lerp(curRailW, circleSizePx, morphT)
                                        val curH = androidx.compose.ui.util.lerp(curRailH, circleSizePx, morphT)
                                        val curAnchorX = androidx.compose.ui.util.lerp(railGrabOffsetX, circleSizePx / 2f, morphT)
                                        val curAnchorY = androidx.compose.ui.util.lerp(railGrabOffsetY, circleSizePx / 2f, morphT)
                                        val curTopLeftX = railFingerX - curAnchorX
                                        val curTopLeftY = railFingerY - curAnchorY
                                        val newCX = curTopLeftX + curW / 2f
                                        val newCY = curTopLeftY + curH / 2f

                                        coroutineScope.launch {
                                            railCenterX.snapTo(newCX)
                                            railCenterY.snapTo(newCY)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val railPositionModifier = if (!isRailPositioned) {
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 14.dp, top = 16.dp)
                    } else {
                        val maxAllowedRailW = with(density) { (parentW - 2 * marginPx).coerceAtLeast(circleSizePx).toDp() }
                        val maxAllowedRailH = with(density) { (parentH - 2 * marginPx).coerceAtLeast(circleSizePx).toDp() }
                        Modifier
                            .align(Alignment.TopStart)
                            .widthIn(max = maxAllowedRailW)
                            .heightIn(max = maxAllowedRailH)
                            .offset {
                                val morphT = railMorphProgress.value.coerceIn(0f, 1f)
                                val isTop = railDockEdge == com.pnotes.ui.RailDockEdge.TOP || railDockEdge == com.pnotes.ui.RailDockEdge.BOTTOM
                                val defaultRailLen = with(density) { 300.dp.toPx() }
                                val curRailW = if (isTop) (if (railHorizontalLength > 0f) railHorizontalLength else defaultRailLen) else dockThicknessPx
                                val curRailH = if (isTop) dockThicknessPx else (if (railVerticalLength > 0f) railVerticalLength else defaultRailLen)
                                val curW = androidx.compose.ui.util.lerp(curRailW, circleSizePx, morphT)
                                val curH = androidx.compose.ui.util.lerp(curRailH, circleSizePx, morphT)

                                val rawTopLeftX: Float
                                val rawTopLeftY: Float
                                if (isRailDragging) {
                                    val curAnchorX = androidx.compose.ui.util.lerp(railGrabOffsetX, circleSizePx / 2f, morphT)
                                    val curAnchorY = androidx.compose.ui.util.lerp(railGrabOffsetY, circleSizePx / 2f, morphT)
                                    rawTopLeftX = railFingerX - curAnchorX
                                    rawTopLeftY = railFingerY - curAnchorY
                                } else {
                                    rawTopLeftX = railCenterX.value - curW / 2f
                                    rawTopLeftY = railCenterY.value - curH / 2f
                                }

                                val finalX = clampTopLeft(rawTopLeftX, curW, parentW, marginPx)
                                val finalY = clampTopLeft(rawTopLeftY, curH, parentH, marginPx, minOffset = topHeaderOffsetPx + marginPx)
                                IntOffset(finalX.roundToInt(), finalY.roundToInt())
                            }
                            .then(
                                if (railMorphProgress.value > 0f) {
                                    val morphT = railMorphProgress.value.coerceIn(0f, 1f)
                                    val isTop = railDockEdge == com.pnotes.ui.RailDockEdge.TOP || railDockEdge == com.pnotes.ui.RailDockEdge.BOTTOM
                                    val defaultRailLen = with(density) { 300.dp.toPx() }
                                    val curRailW = if (isTop) (if (railHorizontalLength > 0f) railHorizontalLength else defaultRailLen) else dockThicknessPx
                                    val curRailH = if (isTop) dockThicknessPx else (if (railVerticalLength > 0f) railVerticalLength else defaultRailLen)
                                    val curW = androidx.compose.ui.util.lerp(curRailW, circleSizePx, morphT)
                                    val curH = androidx.compose.ui.util.lerp(curRailH, circleSizePx, morphT)
                                    Modifier.size(with(density) { curW.toDp() }, with(density) { curH.toDp() })
                                } else Modifier
                            )
                    }

                    androidx.compose.runtime.LaunchedEffect(parentW, parentH) {
                        if (parentW > 0 && parentH > 0) {
                            val isVertical = dockDockEdge == com.pnotes.ui.RailDockEdge.LEFT || dockDockEdge == com.pnotes.ui.RailDockEdge.RIGHT
                            val defaultDockLen = with(density) { 664.dp.toPx() }
                            val curDockW = if (isVertical) dockThicknessPx else (if (dockHorizontalLength > 0f) dockHorizontalLength else defaultDockLen)
                            val curDockH = if (isVertical) (if (dockVerticalLength > 0f) dockVerticalLength else defaultDockLen) else dockThicknessPx
                            val (cx, cy) = safeDockCenter(
                                edge = dockDockEdge,
                                ratio = com.pnotes.ui.ToolbarPreferences.getDockRatio(context),
                                dockW = curDockW,
                                dockH = curDockH,
                                parentW = parentW,
                                parentH = parentH,
                                marginPx = marginPx,
                                thicknessPx = dockThicknessPx,
                                topHeaderHeightPx = topHeaderOffsetPx,
                            )
                            if (!isDockPositioned) {
                                dockCenterX.snapTo(cx)
                                dockCenterY.snapTo(cy)
                                isDockPositioned = true
                            } else if (!isDockDragging) {
                                dockCenterX.snapTo(cx)
                                dockCenterY.snapTo(cy)
                            }
                        }
                    }

                    val dockDragModifier = if (isFullLengthToolbar) Modifier else Modifier.pointerInput(parentW, parentH) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                var isDragActive = false
                                var accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                val pointerId = down.id

                                val touchGrabX = down.position.x
                                val touchGrabY = down.position.y
                                dockGrabOffsetX = touchGrabX
                                dockGrabOffsetY = touchGrabY

                                val isVertical = dockDockEdge == com.pnotes.ui.RailDockEdge.LEFT || dockDockEdge == com.pnotes.ui.RailDockEdge.RIGHT
                                val defaultDockLen = with(density) { 664.dp.toPx() }
                                val curDockW = if (isVertical) dockThicknessPx else (if (dockHorizontalLength > 0f) dockHorizontalLength else defaultDockLen)
                                val curDockH = if (isVertical) (if (dockVerticalLength > 0f) dockVerticalLength else defaultDockLen) else dockThicknessPx

                                val startDockLeft = dockCenterX.value - curDockW / 2f
                                val startDockTop = dockCenterY.value - curDockH / 2f

                                dockFingerX = startDockLeft + touchGrabX
                                dockFingerY = startDockTop + touchGrabY

                                val initialDockEdge = dockDockEdge
                                val oneCmPx = with(density) { 64.dp.toPx() }

                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                    if (!change.pressed) {
                                        if (isDragActive) {
                                            change.consume()
                                            isDockDragging = false

                                            val morphT = dockMorphProgress.value.coerceIn(0f, 1f)
                                            val curW = androidx.compose.ui.util.lerp(curDockW, circleSizePx, morphT)
                                            val curH = androidx.compose.ui.util.lerp(curDockH, circleSizePx, morphT)
                                            val curAnchorX = androidx.compose.ui.util.lerp(dockGrabOffsetX, circleSizePx / 2f, morphT)
                                            val curAnchorY = androidx.compose.ui.util.lerp(dockGrabOffsetY, circleSizePx / 2f, morphT)
                                            val curTopLeftX = dockFingerX - curAnchorX
                                            val curTopLeftY = dockFingerY - curAnchorY
                                            val currCX = curTopLeftX + curW / 2f
                                            val currCY = curTopLeftY + curH / 2f

                                            val distLeft = currCX.coerceAtLeast(0f)
                                            val distRight = (parentW - currCX).coerceAtLeast(0f)
                                            val distTop = (currCY - topHeaderOffsetPx).coerceAtLeast(0f)
                                            val distBottom = (parentH - currCY).coerceAtLeast(0f)

                                             val minDist = minOf(distLeft, distRight, distTop, distBottom)

                                             val nextDockEdge = when (minDist) {
                                                 distLeft -> com.pnotes.ui.RailDockEdge.LEFT
                                                 distRight -> com.pnotes.ui.RailDockEdge.RIGHT
                                                 distTop -> com.pnotes.ui.RailDockEdge.TOP
                                                 else -> com.pnotes.ui.RailDockEdge.BOTTOM
                                             }

                                             val nextIsVertical = nextDockEdge == com.pnotes.ui.RailDockEdge.LEFT || nextDockEdge == com.pnotes.ui.RailDockEdge.RIGHT
                                             val nextDefaultLen = with(density) { (if (nextDockEdge == railDockEdge) 720.dp else 540.dp).toPx() }
                                             val nextDockLen = if (nextIsVertical) {
                                                 if (dockVerticalLength > 0f) dockVerticalLength else nextDefaultLen
                                             } else {
                                                 if (dockHorizontalLength > 0f) dockHorizontalLength else nextDefaultLen
                                             }

                                             val nextDockW = if (nextIsVertical) dockThicknessPx else nextDockLen
                                             val nextDockH = if (nextIsVertical) nextDockLen else dockThicknessPx

                                             val dropRatio: Float
                                             if (nextIsVertical) {
                                                 val minCY = topHeaderOffsetPx + marginPx + nextDockH / 2f
                                                 val maxCY = (parentH - marginPx - nextDockH / 2f).coerceAtLeast(minCY)
                                                 dropRatio = if (maxCY > minCY) ((currCY - minCY) / (maxCY - minCY)).coerceIn(0f, 1f) else 0.5f
                                             } else {
                                                 val minCX = marginPx + nextDockW / 2f
                                                 val maxCX = (parentW - marginPx - nextDockW / 2f).coerceAtLeast(minCX)
                                                 dropRatio = if (maxCX > minCX) ((currCX - minCX) / (maxCX - minCX)).coerceIn(0f, 1f) else 0.5f
                                             }

                                             val (targetCX, targetCY) = safeDockCenter(
                                                 edge = nextDockEdge,
                                                 ratio = dropRatio,
                                                 dockW = nextDockW,
                                                 dockH = nextDockH,
                                                 parentW = parentW,
                                                 parentH = parentH,
                                                 marginPx = marginPx,
                                                 thicknessPx = dockThicknessPx,
                                                 topHeaderHeightPx = topHeaderOffsetPx,
                                             )

                                             val currentlyCombined = dockDockEdge == railDockEdge
                                             dockDockEdge = nextDockEdge
                                             com.pnotes.ui.ToolbarPreferences.setDockEdge(context, nextDockEdge)
                                             com.pnotes.ui.ToolbarPreferences.setDockRatio(context, dropRatio)
                                             if (currentlyCombined) {
                                                 railDockEdge = nextDockEdge
                                                 com.pnotes.ui.ToolbarPreferences.setRailEdge(context, nextDockEdge)
                                                 com.pnotes.ui.ToolbarPreferences.setRailRatio(context, dropRatio)
                                                 com.pnotes.ui.ToolbarPreferences.setIsCombined(context, true)
                                             } else if (nextDockEdge == railDockEdge) {
                                                 com.pnotes.ui.ToolbarPreferences.setIsCombined(context, true)
                                             } else {
                                                 com.pnotes.ui.ToolbarPreferences.setIsCombined(context, false)
                                             }

                                            coroutineScope.launch {
                                                launch {
                                                    dockMorphProgress.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(
                                                            dampingRatio = 0.82f,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                                if (currentlyCombined) {
                                                    launch { railMorphProgress.animateTo(0f, spring(0.82f, Spring.StiffnessMediumLow)) }
                                                    launch { railCenterX.snapTo(targetCX) }
                                                    launch { railCenterY.snapTo(targetCY) }
                                                }
                                                launch {
                                                    dockCenterX.animateTo(
                                                        targetValue = targetCX,
                                                        animationSpec = spring(
                                                            dampingRatio = 0.82f,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                                launch {
                                                    dockCenterY.animateTo(
                                                        targetValue = targetCY,
                                                        animationSpec = spring(
                                                            dampingRatio = 0.82f,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        break
                                    }

                                    val delta = change.positionChange()
                                    accumulatedDrag += delta

                                    if (!isDragActive && accumulatedDrag.getDistance() > viewConfiguration.touchSlop) {
                                        isDragActive = true
                                        isDockDragging = true
                                        coroutineScope.launch {
                                            dockCenterX.stop()
                                            dockCenterY.stop()
                                        }
                                    }

                                    if (isDragActive) {
                                        change.consume()
                                        dockFingerX = (dockFingerX + delta.x).coerceIn(0f, parentW.toFloat())
                                        dockFingerY = (dockFingerY + delta.y).coerceIn(0f, parentH.toFloat())

                                        val isCurrentlyBall = dockMorphProgress.value > 0.5f
                                        val shouldMorphToBall = if (!isCurrentlyBall) {
                                            val currentLeft = dockFingerX - dockGrabOffsetX
                                            val currentTop = dockFingerY - dockGrabOffsetY
                                            val currentRight = currentLeft + curDockW
                                            val currentBottom = currentTop + curDockH
                                            val pulledAwayFromDockedEdge = when (initialDockEdge) {
                                                com.pnotes.ui.RailDockEdge.BOTTOM -> (parentH - marginPx - currentBottom).coerceAtLeast(0f)
                                                com.pnotes.ui.RailDockEdge.TOP -> (currentTop - (topHeaderOffsetPx + marginPx)).coerceAtLeast(0f)
                                                com.pnotes.ui.RailDockEdge.LEFT -> (currentLeft - marginPx).coerceAtLeast(0f)
                                                com.pnotes.ui.RailDockEdge.RIGHT -> (parentW - marginPx - currentRight).coerceAtLeast(0f)
                                            }
                                            pulledAwayFromDockedEdge > oneCmPx
                                        } else {
                                            val ballDistLeft = (dockFingerX - circleRadius - marginPx).coerceAtLeast(0f)
                                            val ballDistRight = (parentW - marginPx - (dockFingerX + circleRadius)).coerceAtLeast(0f)
                                            val ballDistTop = (dockFingerY - circleRadius - (topHeaderOffsetPx + marginPx)).coerceAtLeast(0f)
                                            val ballDistBottom = (parentH - marginPx - (dockFingerY + circleRadius)).coerceAtLeast(0f)
                                            val minBallDistToAnyEdge = minOf(ballDistLeft, ballDistRight, ballDistTop, ballDistBottom)
                                            minBallDistToAnyEdge > oneCmPx
                                        }

                                        coroutineScope.launch {
                                            if (shouldMorphToBall) {
                                                dockMorphProgress.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.85f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                                if (dockDockEdge == railDockEdge) {
                                                    railMorphProgress.animateTo(
                                                        targetValue = 1f,
                                                        animationSpec = spring(
                                                            dampingRatio = 0.85f,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                            } else {
                                                dockMorphProgress.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.85f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                                if (dockDockEdge == railDockEdge) {
                                                    railMorphProgress.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(
                                                            dampingRatio = 0.85f,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        val morphT = dockMorphProgress.value.coerceIn(0f, 1f)
                                        val curW = androidx.compose.ui.util.lerp(curDockW, circleSizePx, morphT)
                                        val curH = androidx.compose.ui.util.lerp(curDockH, circleSizePx, morphT)
                                        val curAnchorX = androidx.compose.ui.util.lerp(dockGrabOffsetX, circleSizePx / 2f, morphT)
                                        val curAnchorY = androidx.compose.ui.util.lerp(dockGrabOffsetY, circleSizePx / 2f, morphT)
                                        val curTopLeftX = dockFingerX - curAnchorX
                                        val curTopLeftY = dockFingerY - curAnchorY
                                        val newCX = curTopLeftX + curW / 2f
                                        val newCY = curTopLeftY + curH / 2f

                                        coroutineScope.launch {
                                            dockCenterX.snapTo(newCX)
                                            dockCenterY.snapTo(newCY)
                                            if (dockDockEdge == railDockEdge) {
                                                railCenterX.snapTo(newCX)
                                                railCenterY.snapTo(newCY)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

	                    val dockPositionModifier = if (!isDockPositioned) {
	                        when (dockDockEdge) {
	                            com.pnotes.ui.RailDockEdge.LEFT -> Modifier.align(Alignment.CenterStart).then(if (!isFullLengthToolbar) Modifier.padding(start = 16.dp) else Modifier)
	                            com.pnotes.ui.RailDockEdge.RIGHT -> Modifier.align(Alignment.CenterEnd).then(if (!isFullLengthToolbar) Modifier.padding(end = 16.dp) else Modifier)
	                            com.pnotes.ui.RailDockEdge.TOP -> Modifier.align(Alignment.TopCenter).then(if (!isFullLengthToolbar) Modifier.padding(top = 16.dp) else Modifier)
	                            com.pnotes.ui.RailDockEdge.BOTTOM -> Modifier.align(Alignment.BottomCenter).then(if (!isFullLengthToolbar) Modifier.padding(bottom = 16.dp) else Modifier)
	                        }
	                    } else if (isFullLengthToolbar && !isDockDragging && dockMorphProgress.value == 0f) {
	                        when (dockDockEdge) {
	                            com.pnotes.ui.RailDockEdge.LEFT -> Modifier.align(Alignment.CenterStart)
	                            com.pnotes.ui.RailDockEdge.RIGHT -> Modifier.align(Alignment.CenterEnd)
	                            com.pnotes.ui.RailDockEdge.TOP -> Modifier.align(Alignment.TopCenter)
	                            com.pnotes.ui.RailDockEdge.BOTTOM -> Modifier.align(Alignment.BottomCenter)
	                        }
	                    } else {
                        val maxAllowedDockW = with(density) { (parentW - 2 * marginPx).coerceAtLeast(circleSizePx).toDp() }
                        val maxAllowedDockH = with(density) { (parentH - 2 * marginPx).coerceAtLeast(circleSizePx).toDp() }
                        Modifier
                            .align(Alignment.TopStart)
                            .widthIn(max = maxAllowedDockW)
                            .heightIn(max = maxAllowedDockH)
                            .offset {
                                val morphT = dockMorphProgress.value.coerceIn(0f, 1f)
                                val isVertical = dockDockEdge == com.pnotes.ui.RailDockEdge.LEFT || dockDockEdge == com.pnotes.ui.RailDockEdge.RIGHT
                                val defaultDockLen = with(density) { 664.dp.toPx() }
                                val curDockW = if (isVertical) dockThicknessPx else (if (dockHorizontalLength > 0f) dockHorizontalLength else defaultDockLen)
                                val curDockH = if (isVertical) (if (dockVerticalLength > 0f) dockVerticalLength else defaultDockLen) else dockThicknessPx
                                val curW = androidx.compose.ui.util.lerp(curDockW, circleSizePx, morphT)
                                val curH = androidx.compose.ui.util.lerp(curDockH, circleSizePx, morphT)

                                val rawTopLeftX: Float
                                val rawTopLeftY: Float
                                if (isDockDragging) {
                                    val curAnchorX = androidx.compose.ui.util.lerp(dockGrabOffsetX, circleSizePx / 2f, morphT)
                                    val curAnchorY = androidx.compose.ui.util.lerp(dockGrabOffsetY, circleSizePx / 2f, morphT)
                                    rawTopLeftX = dockFingerX - curAnchorX
                                    rawTopLeftY = dockFingerY - curAnchorY
                                } else {
                                    rawTopLeftX = dockCenterX.value - curW / 2f
                                    rawTopLeftY = dockCenterY.value - curH / 2f
                                }

                                val finalX = clampTopLeft(rawTopLeftX, curW, parentW, marginPx)
                                val finalY = clampTopLeft(rawTopLeftY, curH, parentH, marginPx, minOffset = topHeaderOffsetPx + marginPx)
                                IntOffset(finalX.roundToInt(), finalY.roundToInt())
                            }
                            .then(
                                if (dockMorphProgress.value > 0f) {
                                    val morphT = dockMorphProgress.value.coerceIn(0f, 1f)
                                    val isVertical = dockDockEdge == com.pnotes.ui.RailDockEdge.LEFT || dockDockEdge == com.pnotes.ui.RailDockEdge.RIGHT
                                    val defaultDockLen = with(density) { 664.dp.toPx() }
                                    val curDockW = if (isVertical) dockThicknessPx else (if (dockHorizontalLength > 0f) dockHorizontalLength else defaultDockLen)
                                    val curDockH = if (isVertical) (if (dockVerticalLength > 0f) dockVerticalLength else defaultDockLen) else dockThicknessPx
                                    val curW = androidx.compose.ui.util.lerp(curDockW, circleSizePx, morphT)
                                    val curH = androidx.compose.ui.util.lerp(curDockH, circleSizePx, morphT)
                                    Modifier.size(with(density) { curW.toDp() }, with(density) { curH.toDp() })
                                } else Modifier
                            )
                    }

                    // UNIFIED CONSOLIDATED TOOLBAR (Always single consolidated bar matching the prototype; hidden in Reading Mode)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !editor.isReadingMode,
                        enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) +
                            androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(220)),
                        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(180)) +
                            androidx.compose.animation.scaleOut(targetScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(180)),
                        modifier = dockPositionModifier.zIndex(10f),
                    ) {
                        com.pnotes.ui.PnotesCombinedDock(
                            editor = editor,
                            dockEdge = dockDockEdge,
                            isDragging = isDockDragging,
                            morphProgress = dockMorphProgress.value.coerceIn(0f, 1f),
                            isFullLength = isFullLengthToolbar,
                            isPageStylesOpen = showPageStyles,
                            activePopupTool = activePopupTool,
                            onActivePopupToolChange = { tool ->
                                if (tool != null) {
                                    dismissAllPopovers()
                                    activePopupTool = tool
                                } else {
                                    activePopupTool = null
                                }
                            },
                            onOpenPageStyles = {
                                val next = !showPageStyles
                                dismissAllPopovers()
                                showPageStyles = next
                            },
                            onToggleStrokeWidthSlider = {
                                val next = !showStrokeWidthSlider
                                dismissAllPopovers()
                                showStrokeWidthSlider = next
                            },
                            onToggleEraserSettings = {
                                val next = !showEraserSettings
                                dismissAllPopovers()
                                showEraserSettings = next
                            },
                            onOpenColorPicker = { isAdd ->
                                dismissAllPopovers()
                                isAddingNewColor = isAdd
                                showColorPicker = true
                            },
                            onInsertImage = {
                                dismissAllPopovers()
                                showImageGalleryDrawer = true
                            },
                            onUndo = { editor.undo() },
                            onRedo = { editor.redo() },
                            onDetach = { },
                            modifier = Modifier
                                .onSizeChanged { size ->
                                    val isVertical = dockDockEdge == com.pnotes.ui.RailDockEdge.LEFT || dockDockEdge == com.pnotes.ui.RailDockEdge.RIGHT
                                    if (!isDockDragging && dockMorphProgress.value == 0f && size.width > 0 && size.height > 0) {
                                        if (isVertical) {
                                            dockVerticalLength = size.height.toFloat()
                                        } else {
                                            dockHorizontalLength = size.width.toFloat()
                                        }
                                    }
                                    dockWidthPx = size.width
                                    dockHeightPx = size.height
                                }
                                .then(dockDragModifier),
                        )
                    }

                    // Scrim to tap-dismiss stroke width slider or eraser settings popovers
                    if ((showStrokeWidthSlider && editor.tool.isStroke) || (showEraserSettings && editor.tool == Tool.ERASER) || showColorPicker) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        dismissAllPopovers()
                                    }
                                )
                        )
                    }

                    // Stroke Width Slider Card (Anchored dynamically adjacent to the Adaptive Rail or Combined Dock).
                    // The cards below stay composed and take their visibility as a parameter, so the
                    // tool change that closes one is a transition rather than a disappearance.
                    val panelDockEdge = if (isCombined) dockDockEdge else railDockEdge
                    val sliderCardWidthPx = with(density) { 180.dp.toPx() }
                    val cardGapPx = with(density) { 10.dp.toPx() }
                    val cardCX = if (isCombined) dockCenterX.value else railCenterX.value
                    val cardCY = if (isCombined) dockCenterY.value else railCenterY.value
                    val cardW = if (isCombined) (if (isDockDragging) circleSizePx else dockWidthPx.toFloat())
                                else (if (isRailDragging) circleSizePx else railWidthPx.toFloat())
                    val cardH = if (isCombined) (if (isDockDragging) circleSizePx else dockHeightPx.toFloat())
                                else (if (isRailDragging) circleSizePx else railHeightPx.toFloat())
                    val sliderX = when (panelDockEdge) {
                        com.pnotes.ui.RailDockEdge.LEFT -> (cardCX + cardW / 2f + cardGapPx)
                            .coerceAtMost((parentW - sliderCardWidthPx - marginPx).coerceAtLeast(marginPx))
                        com.pnotes.ui.RailDockEdge.RIGHT -> (cardCX - cardW / 2f - sliderCardWidthPx - cardGapPx)
                            .coerceAtLeast(marginPx)
                        com.pnotes.ui.RailDockEdge.TOP, com.pnotes.ui.RailDockEdge.BOTTOM -> (cardCX - sliderCardWidthPx / 2f)
                            .coerceIn(marginPx, (parentW - sliderCardWidthPx - marginPx).coerceAtLeast(marginPx))
                    }
                    val sliderY = when (panelDockEdge) {
                        com.pnotes.ui.RailDockEdge.TOP -> (cardCY + cardH / 2f + cardGapPx)
                            .coerceAtMost(parentH - with(density) { 200.dp.toPx() })
                        com.pnotes.ui.RailDockEdge.BOTTOM -> (cardCY - cardH / 2f - with(density) { 150.dp.toPx() } - cardGapPx)
                            .coerceAtLeast(marginPx)
                        else -> (cardCY - with(density) { 60.dp.toPx() })
                            .coerceIn(marginPx, parentH - with(density) { 200.dp.toPx() })
                    }

                    FloatingPanel(
                        visible = showStrokeWidthSlider && editor.tool.isStroke && !showPageStyles,
                        anchor = panelDockEdge.panelAnchor(),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(sliderX.roundToInt(), sliderY.roundToInt()) }
                            .zIndex(15f),
                    ) {
                        com.pnotes.ui.StrokeWidthSliderCard(
                            editor = editor,
                            onDismiss = { showStrokeWidthSlider = false },
                        )
                    }

                    // Eraser Settings Card (Anchored dynamically relative to Dock & Eraser tool position)
                    val isVerticalDock = panelDockEdge == com.pnotes.ui.RailDockEdge.LEFT || panelDockEdge == com.pnotes.ui.RailDockEdge.RIGHT
                    val dockLeft = cardCX - cardW / 2f
                    val dockTop = cardCY - cardH / 2f
                    val eraserCenterXPx = if (!isVerticalDock) dockLeft + with(density) { 70.5.dp.toPx() } else cardCX
                    val eraserCenterYPx = if (isVerticalDock) dockTop + with(density) { 54.dp.toPx() } else cardCY

                    val eraserCardWidthPx = with(density) { 280.dp.toPx() }
                    val eraserCardHeightPx = with(density) { 480.dp.toPx() }
                    val eraserGapPx = with(density) { 10.dp.toPx() }

                    val eraserCardX = when (panelDockEdge) {
                        com.pnotes.ui.RailDockEdge.LEFT -> (cardCX + cardW / 2f + eraserGapPx)
                            .coerceAtMost((parentW - eraserCardWidthPx - marginPx).coerceAtLeast(marginPx))
                        com.pnotes.ui.RailDockEdge.RIGHT -> (cardCX - cardW / 2f - eraserCardWidthPx - eraserGapPx)
                            .coerceAtLeast(marginPx)
                        com.pnotes.ui.RailDockEdge.TOP, com.pnotes.ui.RailDockEdge.BOTTOM -> (eraserCenterXPx - eraserCardWidthPx / 2f)
                            .coerceIn(marginPx, (parentW - eraserCardWidthPx - marginPx).coerceAtLeast(marginPx))
                    }

                    val eraserCardY = when (panelDockEdge) {
                        com.pnotes.ui.RailDockEdge.TOP -> (cardCY + cardH / 2f + eraserGapPx)
                            .coerceAtMost((parentH - eraserCardHeightPx - marginPx).coerceAtLeast(marginPx))
                        com.pnotes.ui.RailDockEdge.BOTTOM -> (cardCY - cardH / 2f - eraserCardHeightPx - eraserGapPx)
                            .coerceAtLeast(marginPx)
                        else -> (eraserCenterYPx - with(density) { 60.dp.toPx() })
                            .coerceIn(marginPx, (parentH - eraserCardHeightPx - marginPx).coerceAtLeast(marginPx))
                    }

                    FloatingPanel(
                        visible = showEraserSettings && editor.tool == Tool.ERASER && !showPageStyles,
                        anchor = panelDockEdge.panelAnchor(),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(eraserCardX.roundToInt(), eraserCardY.roundToInt()) }
                            .zIndex(15f),
                    ) {
                        com.pnotes.ui.EraserSettingsCard(
                            editor = editor,
                            onDismiss = { showEraserSettings = false },
                        )
                    }

                    if (showColorPicker) {
                        val colorPickerWidthPx = with(density) { 280.dp.toPx() }
                        val colorAnchorXPx = if (!isVerticalDock) dockLeft + with(density) { 560.dp.toPx() } else cardCX
                        val colorAnchorYPx = if (isVerticalDock) dockTop + with(density) { 560.dp.toPx() } else cardCY

                        val colorBoxX = when (panelDockEdge) {
                            com.pnotes.ui.RailDockEdge.LEFT -> (cardCX + cardW / 2f + with(density) { 8.dp.toPx() })
                                .coerceAtMost((parentW - colorPickerWidthPx - marginPx).coerceAtLeast(marginPx))
                            com.pnotes.ui.RailDockEdge.RIGHT -> (cardCX - cardW / 2f - colorPickerWidthPx - with(density) { 8.dp.toPx() })
                                .coerceAtLeast(marginPx)
                            com.pnotes.ui.RailDockEdge.TOP, com.pnotes.ui.RailDockEdge.BOTTOM -> (colorAnchorXPx - colorPickerWidthPx / 2f)
                                .coerceIn(marginPx, (parentW - colorPickerWidthPx - marginPx).coerceAtLeast(marginPx))
                        }
                        val colorBoxY = when (panelDockEdge) {
                            com.pnotes.ui.RailDockEdge.TOP -> (cardCY + cardH / 2f)
                            com.pnotes.ui.RailDockEdge.BOTTOM -> (cardCY - cardH / 2f)
                            else -> (colorAnchorYPx - with(density) { 150.dp.toPx() }).coerceIn(marginPx, (parentH - with(density) { 350.dp.toPx() }).coerceAtLeast(marginPx))
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset { IntOffset(colorBoxX.roundToInt(), colorBoxY.roundToInt()) }
                                .size(280.dp, 1.dp)
                                .zIndex(15f),
                        ) {
                            com.pnotes.ui.ColorPickerPopup(
                                initial = if (isAddingNewColor) editor.controller.inkColor else (editor.toolbarColors.getOrNull(editor.activeColorIndex) ?: editor.controller.inkColor),
                                recents = editor.hostRecentColors,
                                onDismiss = {
                                    showColorPicker = false
                                    editor.rememberSwatchColor(editor.activeColorIndex)
                                },
                                onPick = { chosenColor ->
                                    if (isAddingNewColor) {
                                        editor.addToolbarColor(chosenColor)
                                    } else {
                                        editor.setSwatchColor(editor.activeColorIndex, chosenColor)
                                        editor.rememberSwatchColor(editor.activeColorIndex)
                                    }
                                    showColorPicker = false
                                },
                                onDelete = if (editor.toolbarColors.size > 1) {
                                    {
                                        editor.removeToolbarColor(editor.activeColorIndex)
                                        showColorPicker = false
                                    }
                                } else null
                            )
                        }
                    }

                    val showModalScrim = showPageStyles || showPageAngle || showImageGalleryDrawer
                    val modalScrimAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (showModalScrim) 1f else 0f,
                        animationSpec = com.pnotes.ui.theme.LocalMotion.current.effect(),
                        label = "modalScrimAlpha",
                    )
                    if (modalScrimAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = modalScrimAlpha }
                                .background(Color.Black.copy(alpha = if (com.pnotes.ui.isGlassDark()) 0.45f else 0.25f))
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        showPageStyles = false
                                        showPageAngle = false
                                        showImageGalleryDrawer = false
                                    }
                                )
                                .zIndex(18f)
                        )
                    }

                    com.pnotes.ui.FloatingPanel(
                        visible = showPageStyles,
                        anchor = com.pnotes.ui.PanelAnchor.CENTER,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-20).dp)
                            .zIndex(20f),
                    ) {
                        com.pnotes.ui.PageStylesDialog(
                            editor = editor,
                            onDismiss = { showPageStyles = false },
                        )
                    }

                    com.pnotes.ui.FloatingPanel(
                        visible = showPageAngle,
                        anchor = com.pnotes.ui.PanelAnchor.CENTER,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-20).dp)
                            .zIndex(20f),
                    ) {
                        com.pnotes.ui.PageAnglePopover(
                            editor = editor,
                            onDismiss = { showPageAngle = false },
                        )
                    }

                    if (showMargins) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-16).dp, y = 64.dp)
                                .zIndex(20f),
                        ) {
                            com.pnotes.ui.MarginsPopup(editor) { showMargins = false }
                        }
                    }

                    if (showViewOptions) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-16).dp, y = 64.dp)
                                .zIndex(20f),
                        ) {
                            com.pnotes.ui.ViewMenuPopup(editor) { showViewOptions = false }
                        }
                    }

                    if (showPageJump) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-16).dp, y = 64.dp)
                                .zIndex(20f),
                        ) {
                            com.pnotes.ui.PageJumpPopup(editor) { showPageJump = false }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = editor.sidebarVisible,
                        enter = androidx.compose.animation.slideInHorizontally(
                            animationSpec = androidx.compose.animation.core.tween(250),
                            initialOffsetX = { it },
                        ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
                        exit = androidx.compose.animation.slideOutHorizontally(
                            animationSpec = androidx.compose.animation.core.tween(200),
                            targetOffsetX = { it },
                        ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)),
                        modifier = Modifier.align(Alignment.CenterEnd).zIndex(30f),
                    ) {
                        com.pnotes.ui.PnotesPageManager(
                            editor = editor,
                            onClose = { editor.sidebarVisible = false },
                        )
                    }

                    // 4. Side Vertical Gallery Bar (Image Picker Drawer)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showImageGalleryDrawer,
                        enter = androidx.compose.animation.slideInHorizontally(
                            animationSpec = androidx.compose.animation.core.tween(250),
                            initialOffsetX = { it },
                        ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
                        exit = androidx.compose.animation.slideOutHorizontally(
                            animationSpec = androidx.compose.animation.core.tween(200),
                            targetOffsetX = { it },
                        ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)),
                        modifier = Modifier.align(Alignment.CenterEnd).zIndex(35f),
                    ) {
                        com.pnotes.ui.PnotesImageGalleryDrawer(
                            onSelectImageBytes = { bytes ->
                                editor.insertImageAt(bytes, null)
                                showImageGalleryDrawer = false
                            },
                            onLaunchFilePicker = {
                                showImageGalleryDrawer = false
                                actions.onInsertImage(editor, null)
                            },
                            onLaunchCamera = {
                                showImageGalleryDrawer = false
                                actions.onTakePhoto(editor)
                            },
                            onClose = { showImageGalleryDrawer = false },
                        )
                    }

                    // 5. Floating Edge Page Scroll Indicator (Appears when scrolling / panning)
                    com.pnotes.ui.PnotesFloatingPageScrollIndicator(
                        pageIndex = editor.pageIndex,
                        pageCount = editor.pageCount,
                        scrollFraction = editor.scrollFraction,
                        isScrolling = editor.isScrolling,
                        onScrubToPage = { page -> editor.goToPage(page) },
                        edge = com.pnotes.ui.ScrollIndicatorEdge.RIGHT,
                        isDark = palette.isDark,
                        topInsetDp = if (editor.headerShrunk) 20f else 64f,
                        bottomInsetDp = 80f,
                    )
                }
            }

            LaunchedEffect(editor.headerShrunk) {
                if (editor.headerShrunk) {
                    topHeaderOffsetPx = 0f
                }
            }

            // 2. FRESH REACT PROTOTYPE TOP BAR (Overlay on top of workspace)
            com.pnotes.ui.PnotesTopBar(
                editor = editor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { size ->
                        if (size.height > 0) {
                            topHeaderOffsetPx = if (editor.headerShrunk) 0f else size.height.toFloat()
                        }
                    }
                    .zIndex(if (editor.headerShrunk) 40f else 20f),
                onOpenBackstage = actions.onOpenBackstage,
                onShare = {
                    actions.onSharePages(editor, listOf(editor.pageIndex), false)
                },
                onOpenMargins = {
                    dismissAllPopovers()
                    showMargins = true
                },
                onOpenViewOptions = {
                    dismissAllPopovers()
                    showViewOptions = true
                },
                onOpenPageJump = {
                    dismissAllPopovers()
                    showPageJump = true
                },
                onTogglePresentation = actions.onPresent,
                onToggleFullscreen = actions.onToggleFullscreen,
                onToggleHeaderShrunk = { editor.toggleHeaderShrunk() },
                onInsertImage = {
                    dismissAllPopovers()
                    showImageGalleryDrawer = true
                },
                onTogglePageStyles = {
                    val next = !showPageStyles
                    dismissAllPopovers()
                    showPageStyles = next
                },
                onTogglePageAngle = {
                    val next = !showPageAngle
                    dismissAllPopovers()
                    showPageAngle = next
                },
                onToggleStrokeWidthSlider = {
                    val next = !showStrokeWidthSlider
                    dismissAllPopovers()
                    showStrokeWidthSlider = next
                },
                onToggleEraserSettings = {
                    val next = !showEraserSettings
                    dismissAllPopovers()
                    showEraserSettings = next
                },
                onTogglePageManager = { editor.sidebarVisible = !editor.sidebarVisible },
	                onToggleFullLengthToolbar = {
	                    val next = !isFullLengthToolbar
	                    isFullLengthToolbar = next
	                    com.pnotes.ui.ToolbarPreferences.setIsFullLength(context, next)
	                },
	                isFullLengthToolbar = isFullLengthToolbar,
                isPageStylesOpen = showPageStyles,
                isPageAngleOpen = showPageAngle,
                onApplyPageStyles = { showPageStyles = false },
                onNewTab = { actions.guarded(editor) { editor.newNote() } },
                onNewCanvasTab = { actions.guarded(editor) { editor.newCanvas() } },
                onImportPdf = actions.onImportPdf,
                onOpenBrowseDrawer = { showBrowseNotesDrawer = true },
            )

            // 2b. VERTICAL SEPARATOR LINE (Between TopBar and Full-Length Left Toolbar)
            val isFullLengthOnLeft = isFullLengthToolbar && !isDockDragging && dockMorphProgress.value <= 0.05f && dockDockEdge == com.pnotes.ui.RailDockEdge.LEFT
            if (isFullLengthOnLeft && !editor.isReadingMode) {
                val isDark = com.pnotes.ui.isGlassDark()
                val strokeCol = if (isDark) Color(0xFF3F3F48) else Color(0xFFD4D4DC)
                val topHeaderDp = with(density) { topHeaderOffsetPx.toDp() }
                val hasHeader = !editor.headerShrunk && topHeaderOffsetPx > 0f

                Box(
                    modifier = Modifier
                        .offset(x = 44.dp, y = if (hasHeader) topHeaderDp else 0.dp)
                        .width(0.9.dp)
                        .fillMaxHeight()
                        .background(strokeCol)
                        .zIndex(25f)
                )
            }

            // 3. FULL-SCREEN SCRIM FOR BROWSE NOTES DRAWER (Dims whole screen including TopBar)
            val browseScrimAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (showBrowseNotesDrawer) 1f else 0f,
                animationSpec = com.pnotes.ui.theme.LocalMotion.current.effect(),
                label = "browseScrimAlpha",
            )
            if (browseScrimAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = browseScrimAlpha }
                        .background(Color.Black.copy(alpha = if (com.pnotes.ui.isGlassDark()) 0.5f else 0.35f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { showBrowseNotesDrawer = false }
                        )
                        .zIndex(45f)
                )
            }

            // 4. MODAL SLIDE-OVER DRAWER: BROWSE ALL DOCUMENTS (Elevated ABOVE TopBar)
            com.pnotes.ui.PnotesNotesSideDrawer(
                visible = showBrowseNotesDrawer,
                editor = editor,
                onClose = { showBrowseNotesDrawer = false },
                onOpenNote = { entry ->
                    showBrowseNotesDrawer = false
                    val isCanvas = com.pnotes.core.util.DocumentKind.ofName(entry.name) == com.pnotes.core.util.DocumentKind.CANVAS
                    editor.openInNewTab(entry.documentUri, com.pnotes.core.util.DocumentKind.stripSuffix(entry.name), isCanvas)
                },
                onOpenInNewTab = { entry ->
                    showBrowseNotesDrawer = false
                    val isCanvas = com.pnotes.core.util.DocumentKind.ofName(entry.name) == com.pnotes.core.util.DocumentKind.CANVAS
                    editor.openInNewTab(entry.documentUri, com.pnotes.core.util.DocumentKind.stripSuffix(entry.name), isCanvas)
                },
                onOpenInSplit = { entry ->
                    showBrowseNotesDrawer = false
                    actions.onOpenInSplitTab(editor, com.pnotes.tabs.NoteTab(
                        uri = entry.documentUri,
                        title = com.pnotes.core.util.DocumentKind.stripSuffix(entry.name),
                        kind = if (com.pnotes.core.util.DocumentKind.ofName(entry.name) == com.pnotes.core.util.DocumentKind.CANVAS) com.pnotes.core.util.DocumentKind.CANVAS else com.pnotes.core.util.DocumentKind.NOTE
                    ))
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(50f),
            )

            BackHandler(enabled = showBrowseNotesDrawer) {
                showBrowseNotesDrawer = false
            }
        }
            // Last child of the resized column: rides directly above the soft keyboard.
            // TextFormatBar is replaced by in-place TextStyleBar for text boxes.
        }
    }
}

/**
 * The handle over the seam between two split panes. Dragging it moves the boundary along the split
 * axis, keeping both panes at least [MIN_PANE_RATIO] of it; double-tapping puts it back in the
 * middle. Only the line is painted — the margin either side of it is what the finger catches, and it
 * stays transparent so the panes it lies over show through.
 */
@Composable
private fun SplitDivider(
    sideBySide: Boolean,
    extentPx: Float,
    ratio: Float,
    onRatio: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    // Read inside the long-lived drag gesture, which does not restart as the ratio moves.
    val ratioNow = rememberUpdatedState(ratio)
    val onRatioNow = rememberUpdatedState(onRatio)
    var dragged by remember { mutableStateOf(0f) }
    Box(
        modifier = modifier
            .pointerInput(sideBySide, extentPx) {
                detectDragGestures(
                    onDragStart = { dragged = ratioNow.value },
                    onDrag = { change, drag ->
                        change.consume()
                        if (extentPx <= 0f) return@detectDragGestures
                        val along = if (sideBySide) drag.x else drag.y
                        dragged = (dragged + along / extentPx).coerceIn(MIN_PANE_RATIO, 1f - MIN_PANE_RATIO)
                        onRatioNow.value(dragged)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onRatioNow.value(0.5f) })
            },
        contentAlignment = Alignment.Center,
    ) {
        // The line itself: thin, accent, and running the whole way, so it closes the frame the two
        // panes' focus lines start along their toolbars. The empty margin either side of it is what
        // the finger actually catches.
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (sideBySide) (DIVIDER - DIVIDER_LINE) / 2 else 0.dp,
                    vertical = if (sideBySide) 0.dp else (DIVIDER - DIVIDER_LINE) / 2,
                )
                .clip(RoundedCornerShape(DIVIDER_LINE / 2))
                .background(palette.accent.toComposeColor()),
        )
    }
}

/**
 * Hands a reused View back to [AndroidView]. A pane's canvas can move to a fresh composition node
 * when the split opens or closes, and the new node parents the view itself, so it has to leave the
 * old parent first rather than throw.
 */
private fun <T : android.view.View> detached(view: T): T =
    view.also { (it.parent as? android.view.ViewGroup)?.removeView(it) }

/**
 * Wraps a PDF export's output stream and throws the instant [cancelled] turns true, so PdfBox's
 * otherwise-uninterruptible `save()` (which writes the whole document in one call) aborts promptly
 * when the user taps Cancel, instead of running to completion on a background thread.
 */
private class CancellableOutputStream(
    private val out: java.io.OutputStream,
    private val cancelled: () -> Boolean,
) : java.io.OutputStream() {
    override fun write(b: Int) { if (cancelled()) throw java.io.InterruptedIOException(); out.write(b) }
    override fun write(b: ByteArray, off: Int, len: Int) {
        if (cancelled()) throw java.io.InterruptedIOException()
        out.write(b, off, len)
    }
    override fun flush() { out.flush() }
    override fun close() { out.close() }
}

/**
 * "Exporting to PDF…" dialog shown while a (possibly large) note is flattened to a PDF off the main
 * thread. An animated spinner runs through the preparing/page phases (where there's no meaningful
 * percentage), then a determinate ring fills 0→100% during the final write. Dismissing it (Cancel,
 * back, or tapping outside) aborts the export via [onCancel]. Styled to match the monospace surfaces.
 */
@Composable
private fun PdfExportDialog(done: Int, total: Int, onCancel: () -> Unit) {
    val palette = LocalPalette.current
    // Only the final PDF write has a meaningful, moving percentage (reported as byte progress with
    // total = -1, done a 0..1000 permille). Preparing and the page phase sit at 0% (the page loop is
    // near-instant on the common path), so show an animated spinner there instead of a frozen 0% ring,
    // and switch to the determinate ring + percentage once writing begins.
    val writing = total < 0
    val fraction = if (writing) (done / 1000f).coerceIn(0f, 1f) else 0f
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surface.toComposeColor())
                .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(14.dp))
                .padding(horizontal = 32.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                if (writing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxSize(),
                        color = palette.accent.toComposeColor(),
                        trackColor = palette.border.toComposeColor(),
                        strokeWidth = 4.dp,
                    )
                    Text(
                        "${(fraction * 100).roundToInt()}%",
                        color = palette.text.toComposeColor(),
                        fontSize = 15.sp,
                    )
                } else {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = palette.accent.toComposeColor(),
                        strokeWidth = 4.dp,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Exporting to PDF…",
                color = palette.text.toComposeColor(),
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    writing -> "Writing the PDF…"
                    total == 0 -> "Preparing…"
                    done < total -> "page $done / $total"
                    else -> "Writing $total page${if (total == 1) "" else "s"}…"
                },
                color = palette.textDim.toComposeColor(),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(14.dp))
            androidx.compose.material3.TextButton(onClick = onCancel) {
                Text("Cancel", color = palette.accent.toComposeColor())
            }
        }
    }
}

/**
 * Indeterminate spinner dialog: a styled card with an animated [CircularProgressIndicator], a
 * [title], a "This may take a moment." line, and a Cancel button. Shared by the "Importing…"
 * (PDF/note copy) and "Opening note…" (off-thread read) flows, neither of which has a meaningful
 * percentage. Dismissing it (Cancel, back, or tapping outside) calls [onCancel]. Styled to match
 * [PdfExportDialog].
 */
@Composable
private fun SpinnerDialog(title: String, onCancel: () -> Unit) {
    val palette = LocalPalette.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surface.toComposeColor())
                .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(14.dp))
                .padding(horizontal = 32.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = palette.accent.toComposeColor(),
                    strokeWidth = 4.dp,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(title, color = palette.text.toComposeColor(), fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text("This may take a moment.", color = palette.textDim.toComposeColor(), fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            androidx.compose.material3.TextButton(onClick = onCancel) {
                Text("Cancel", color = palette.accent.toComposeColor())
            }
        }
    }
}

/**
 * Non-cancelable "Saving your notes…" dialog shown while a dirty note is flushed off the main thread
 * on close or pause, so quitting a large note shows progress instead of a frozen (ANR) screen.
 */
@Composable
private fun SavingDialog() {
    val palette = LocalPalette.current
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surface.toComposeColor())
                .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(14.dp))
                .padding(horizontal = 32.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = palette.accent.toComposeColor(),
                    strokeWidth = 4.dp,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text("Saving your notes…", color = palette.text.toComposeColor(), fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text("This may take a moment.", color = palette.textDim.toComposeColor(), fontSize = 13.sp)
        }
    }
}

/**
 * Indeterminate "Importing PDF…"/"Importing note…" dialog shown while a picked PDF (or `.pnote`) is
 * streamed into a new note off the main thread. Import has no natural page-by-page progress — the
 * cost is copying the (possibly large) source bytes — so it shows an animated spinner.
 */
@Composable
private fun PdfImportDialog(isPdf: Boolean, onCancel: () -> Unit) =
    SpinnerDialog(if (isPdf) "Importing PDF…" else "Importing note…", onCancel)

/**
 * Subtle, non-blocking hint shown bottom-right while a dark-mode PDF's embedded-image colours are
 * still being parsed by the up-front background sweep ([Editor.isRefiningPdf]). The pages are already
 * visible; this just shows a `k/N` progress bar so the user can keep scrolling and drawing while the
 * remaining pages' images snap to their true colours.
 */
@Composable
private fun BoxScope.RefiningPdfHint(editor: Editor) {
    val palette = LocalPalette.current
    AnimatedVisibility(
        visible = editor.isRefiningPdf,
        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val total = editor.refiningTotal
        val done = editor.refiningDone
        val fraction = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
        Column(
            // Fixed width so the bar's fillMaxWidth tracks the chip (not the whole screen); the label
            // wraps to two tidy lines under it.
            modifier = Modifier
                .width(190.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(palette.surface.toComposeColor())
                .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = palette.accent.toComposeColor(),
                trackColor = palette.border.toComposeColor(),
                drawStopIndicator = {}, // no trailing dot
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Refining PDF colours $done/$total pages…",
                color = palette.textDim.toComposeColor(),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
        }
    }
}

/**
 * Transient zoom-lock affordance, centred just below the toolbar. Appears when a pinch snaps to
 * fit-to-width ([Editor.zoomLockHint] bumps) and auto-dismisses after a moment. Tapping toggles the
 * zoom lock, so the same chip both locks and unlocks; each tap re-arms the dismiss timer so it
 * lingers long enough to tap again.
 */
@Composable
private fun BoxScope.ZoomLockHint(editor: Editor) {
    val palette = LocalPalette.current
    var visible by remember { mutableStateOf(false) }
    // Bumped on the initial fit-width snap and on every tap; (re)starts the auto-dismiss timer below.
    var armToken by remember { mutableStateOf(0) }
    LaunchedEffect(editor.zoomLockHint) {
        if (editor.zoomLockHint > 0) {
            visible = true
            armToken++
        }
    }
    LaunchedEffect(armToken) {
        if (armToken > 0) {
            delay(2500)
            visible = false
        }
    }
    // Breaking past the fit-width magnet dismisses the hint at once.
    LaunchedEffect(editor.zoomLockHintDismiss) {
        if (editor.zoomLockHintDismiss > 0) visible = false
    }
    val locked = editor.zoomLocked
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(palette.surface.toComposeColor())
                .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(6.dp))
                .clickable { editor.toggleZoomLock(); armToken++ }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (locked) PnotesIcons.lock else PnotesIcons.unlock,
                contentDescription = if (locked) "Unlock zoom" else "Lock zoom at fit width",
                tint = (if (locked) palette.accent else palette.textDim).toComposeColor(),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (locked) "Unlock zoom" else "Lock zoom",
                color = palette.text.toComposeColor(),
                fontSize = 13.sp,
            )
        }
    }
}

/** Queries the storage provider for a document's user-visible file name, if available. */
private fun displayNameOf(resolver: android.content.ContentResolver, uri: Uri): String? = runCatching {
    resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (i >= 0) c.getString(i) else null
        } else null
    }
}.getOrNull()
