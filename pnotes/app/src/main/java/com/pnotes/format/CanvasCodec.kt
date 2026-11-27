package com.pnotes.format

import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.infinite.CanvasBackground
import com.pnotes.core.infinite.InfiniteDocument
import com.pnotes.core.infinite.Waypoint
import com.pnotes.core.model.CanvasItem
import com.pnotes.core.model.ImageData
import com.pnotes.core.model.ImageItem
import com.pnotes.core.model.PagePattern
import com.pnotes.core.model.PageSize
import com.pnotes.core.model.PageStyle
import com.pnotes.core.model.Rgba
import com.pnotes.core.model.ShapeItem
import com.pnotes.core.model.Stroke
import com.pnotes.core.model.TableItem
import com.pnotes.core.pal.ImageCodec
import com.pnotes.core.stroke.Sample
import com.pnotes.core.tools.ShapeKind
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolConfig
import com.pnotes.core.tools.ToolDefaults
import com.pnotes.core.util.Svg
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Thrown when a file is not a valid `.xcanvas` bundle. */
class XCanvasFormatException(message: String) : Exception(message)

/**
 * Reads and writes the `.xcanvas` bundle: the same shape as `.pnote` (a ZIP with a deflated
 * `manifest.json` plus stored binary assets under `assets/`), but with no page array. The manifest
 * holds one flat item list in content space plus the canvas metadata a page has no place for: the
 * background ruling, the view the canvas was left at, and the named waypoints.
 *
 * This is a sibling of [DocumentCodec], not a mode of it. The two document models are different
 * enough that one codec serving both would branch in every method, and `DocumentCodec`'s emitted
 * bytes are pinned by a test, so its writers must not be refactored into shared helpers. The item
 * serialization here is deliberately identical to `.pnote`'s, so ink written by either format reads
 * the same way and a converter would be a straight copy.
 *
 * Loading is forgiving, exactly like `.pnote`: unknown item kinds are skipped, missing fields take
 * model defaults, and new optional fields are written only when set so older readers stay
 * compatible. Strokes stay editable vector samples; nothing is flattened.
 */
class CanvasCodec(private val imageCodec: ImageCodec) {

    /** Thrown out of [write] when [isCancelled] turns true mid-copy, so the caller can discard the partial file. */
    class WriteCancelled : Exception()

    fun write(doc: InfiniteDocument, out: OutputStream, isCancelled: () -> Boolean = { false }) {
        val assets = ArrayList<Pair<String, File>>()
        ZipOutputStream(out).use { zos ->
            // The manifest streams straight into the deflater, so a dense canvas's JSON is never
            // materialized as a DOM, a String, or a byte[].
            zos.putNextEntry(ZipEntry("manifest.json").apply { method = ZipEntry.DEFLATED })
            val w = java.io.BufferedWriter(java.io.OutputStreamWriter(zos, Charsets.UTF_8), 32 * 1024)
            writeManifest(JsonWrite(w), doc, assets)
            w.flush()
            zos.closeEntry()
            // Each image streams straight from its temp file into the bundle, never as a byte[].
            for ((name, file) in assets) zos.putStored(name, file, isCancelled)
        }
    }

    // --- model -> streaming json ---

    private fun writeManifest(j: JsonWrite, doc: InfiniteDocument, assets: MutableList<Pair<String, File>>) {
        j.beginObject()
        j.name("format").value(FORMAT)
        j.name("version").value(VERSION)
        j.name("writer").value(WRITER)
        j.name("dpi").value(doc.dpi)
        writeBackground(j, doc.background)
        // The last view and the waypoints are written only when there is something to say.
        doc.lastView?.let {
            j.name("view")
            writeView(j, it)
        }
        if (doc.waypoints.isNotEmpty()) {
            j.name("waypoints").beginArray()
            for (wp in doc.waypoints) writeWaypoint(j, wp)
            j.endArray()
        }
        j.name("items").beginArray()
        for (item in doc.items) writeItem(j, item, assets)
        j.endArray()
        j.endObject()
    }

    private fun writeItem(j: JsonWrite, item: CanvasItem, assets: MutableList<Pair<String, File>>) {
        when (item) {
            is Stroke -> writeStroke(j, item)
            is ImageItem -> {
                // Readers match assets by manifest name (any extension); .svg keeps the bundle
                // honest and older readers skip the item they can't decode.
                val ext = if (Svg.isSvgFile(item.image.file)) "svg" else "png"
                val name = "assets/image-%03d.%s".format(assets.size, ext)
                assets.add(name to item.image.file)
                writeImage(j, item, name)
            }
            is ShapeItem -> writeShape(j, item)
            is TableItem -> writeTable(j, item)
            else -> {} // text and any unrecognized kind: not written, the canvas has none
        }
    }

    /**
     * The background ruling. Written in full rather than as a sparse override set: unlike a page,
     * a canvas has no level below it to inherit from, so every field carries a real value and
     * spelling them out keeps a reopened canvas immune to a change of defaults later.
     */
    private fun writeBackground(j: JsonWrite, bg: CanvasBackground) {
        j.name("background").beginObject()
        j.name("pattern").value(bg.pattern.id)
        j.name("pattern_color")
        writeRgba(j, bg.patternColor)
        j.name("spacing").value(bg.spacing)
        // Additive: written only when the canvas overrides the theme paper.
        bg.paperColor?.let {
            j.name("paper_color")
            writeRgba(j, it)
        }
        j.endObject()
    }

    private fun writeView(j: JsonWrite, v: Waypoint) {
        j.beginObject()
        j.name("cx").value(v.cx)
        j.name("cy").value(v.cy)
        j.name("zoom").value(v.zoom)
        j.endObject()
    }

    private fun writeWaypoint(j: JsonWrite, w: Waypoint) {
        j.beginObject()
        j.name("name").value(w.name)
        j.name("cx").value(w.cx)
        j.name("cy").value(w.cy)
        j.name("zoom").value(w.zoom)
        j.endObject()
    }

    private fun writeStroke(j: JsonWrite, s: Stroke) {
        // Per-sample time is only meaningful to the speed pen, so it's written as an optional
        // 4th element only then; every other stroke serializes without it.
        val withTime = s.config.speedStrength > 0.0
        j.beginObject()
        j.name("kind").value(Stroke.KIND)
        j.name("tool").value(s.tool.id)
        j.name("config").beginObject()
        j.name("base_width").value(s.config.baseWidth)
        j.name("pressure_enabled").value(s.config.pressureEnabled)
        j.name("pressure_min_factor").value(s.config.pressureMinFactor)
        j.name("direction_strength").value(s.config.directionStrength)
        j.name("rgba")
        writeRgba(j, s.config.rgba)
        if (s.config.pressureGamma != 1.0) j.name("pressure_gamma").value(s.config.pressureGamma)
        // New style fields are written only when set, so a plain pen stroke's config stays minimal.
        if (s.config.speedStrength != 0.0) j.name("speed_strength").value(s.config.speedStrength)
        if (s.config.taperEnabled) {
            j.name("taper_enabled").value(true)
            j.name("taper_min_factor").value(s.config.taperMinFactor)
        }
        if (s.config.neon) {
            j.name("neon").value(true)
            j.name("neon_strength").value(s.config.neonStrength)
        }
        if (s.tool == Tool.DASHED) {
            j.name("dash_length").value(s.config.dashLength)
            j.name("dash_gap").value(s.config.dashGap)
        }
        if (s.tool == Tool.HIGHLIGHTER) {
            j.name("highlighter_alpha").value(s.config.highlighterAlpha)
            if (s.config.highlighterInverse) j.name("highlighter_inverse").value(true)
        }
        if (s.config.texture != com.pnotes.core.tools.StrokeTexture.SMOOTH) {
            j.name("texture").value(s.config.texture.id)
        }
        // The nib's grind turns the ink, so it has to survive a reload: written only when the nib is
        // not square, which keeps every italic stroke's config exactly as it was.
        if (s.config.nibCut != 0.0) j.name("nib_cut").value(s.config.nibCut)
        // Star Notes physics parameters: written only when they differ from default
        if (s.config.velocityLowerBound != 0.0) j.name("vel_lo").value(s.config.velocityLowerBound)
        if (s.config.velocityUpperBound != 0.6) j.name("vel_hi").value(s.config.velocityUpperBound)
        if (s.config.velocitySensitivity != 0.5) j.name("vel_sens").value(s.config.velocitySensitivity)
        if (s.config.velocityAmplifier != 1.0) j.name("vel_amp").value(s.config.velocityAmplifier)
        // Preserve the historical compact form for the legacy .4 default. New ordinary pens use
        // zero, so that value is written explicitly and does not reload as the legacy moving tail.
        if (s.config.endVelocitySensitivity != 0.4) j.name("end_vel_sens").value(s.config.endVelocitySensitivity)
        if (s.config.curvatureDecay != 0.0) j.name("curvature_decay").value(s.config.curvatureDecay)
        if (s.config.brushRatio != 1.0) j.name("brush_ratio").value(s.config.brushRatio)
        if (s.config.brushAngle != 0.0) j.name("brush_angle").value(s.config.brushAngle)
        if (s.config.adaptiveSpline) j.name("adaptive_spline").value(true)
        j.endObject()
        // Samples are almost all of a dense manifest's bytes, so they serialize rounded: 0.01
        // content px and 0.001 pressure are far below anything visible. Rounding is idempotent,
        // so re-saving an untouched canvas stays byte-stable.
        j.name("samples").beginArray()
        for (sm in s.samples) {
            j.beginArray().value(round2(sm.x)).value(round2(sm.y)).value(round3(sm.pressure))
            if (withTime) j.value(sm.t)
            j.endArray()
        }
        j.endArray()
        // Stylus orientation rides its own optional channels rather than a 5th element of every
        // sample: only a stylus reports it, so ink drawn with a finger keeps the samples array it has
        // always written.
        if (s.hasTilt) {
            j.name("tilts").beginArray()
            for (i in 0 until s.sampleCount) j.value(round3(s.tiltAt(i)))
            j.endArray()
        }
        if (s.hasAzimuth) {
            j.name("azimuths").beginArray()
            for (i in 0 until s.sampleCount) j.value(round3(s.azimuthAt(i)))
            j.endArray()
        }
        if (withTime) j.name("speed_scale").value(s.speedScale)
        // The zoom the stroke was drawn at, as the scale on the ink low-pass lengths, so it
        // re-smooths on load exactly as it did under the pen.
        if (s.smoothScale != 1.0) j.name("smooth_scale").value(s.smoothScale)
        // Star Notes lag compensation (see StrokeEngine.emaStep): written only when on, so older
        // readers ignore it and pre-lagComp ink reloads on the legacy filter it drew with.
        if (s.lagComp != 0.0) j.name("lag_comp").value(s.lagComp)
        // The predicted tip: the samples the committed geometry must not smooth away. Written only
        // when the stroke carries one, so ordinary ink serializes exactly as it always has.
        if (s.tipCount > 0) j.name("tip_count").value(s.tipCount)
        // Straight-line strokes must reload un-smoothed, else the EMA pulls their far end inward.
        if (s.straight) j.name("straight").value(true)
        if (s.locked) j.name("locked").value(true)
        j.endObject()
    }

    private fun writeImage(j: JsonWrite, item: ImageItem, assetName: String) {
        j.beginObject()
        j.name("kind").value(ImageItem.KIND)
        j.name("asset").value(assetName)
        j.name("rect").beginArray().value(item.rect.x).value(item.rect.y).value(item.rect.w).value(item.rect.h).endArray()
        j.name("src_w").value(item.image.width)
        j.name("src_h").value(item.image.height)
        if (item.orientation != 0) j.name("orientation").value(item.orientation)
        if (item.angle != 0.0) j.name("angle").value(item.angle)
        if (item.locked) j.name("locked").value(true)
        j.endObject()
    }

    private fun writeShape(j: JsonWrite, s: ShapeItem) {
        j.beginObject()
        j.name("kind").value(ShapeItem.KIND)
        j.name("shape").value(s.shape.id)
        j.name("start").beginArray().value(s.start.x).value(s.start.y).endArray()
        j.name("end").beginArray().value(s.end.x).value(s.end.y).endArray()
        j.name("stroke_rgba")
        writeRgba(j, s.strokeRgba)
        j.name("stroke_width").value(s.strokeWidth)
        j.name("fill_rgba")
        s.fillRgba?.let { writeRgba(j, it) } ?: j.nullValue()
        // Polygon/polyline carry their vertices (absolute content px); other kinds omit them.
        s.vertices()?.let { verts ->
            j.name("points").beginArray()
            for (p in verts) j.beginArray().value(p.x).value(p.y).endArray()
            j.endArray()
        }
        if (s.neon) {
            j.name("neon").value(true)
            j.name("neon_strength").value(s.neonStrength)
        }
        if (s.dashed) {
            j.name("dashed").value(true)
            j.name("dash_length").value(s.dashLength)
            j.name("dash_gap").value(s.dashGap)
        }
        if (s.locked) j.name("locked").value(true)
        j.endObject()
    }

    private fun writeTable(j: JsonWrite, t: TableItem) {
        j.beginObject()
        j.name("kind").value(TableItem.KIND)
        j.name("pos").beginArray().value(round2(t.pos.x)).value(round2(t.pos.y)).endArray()
        j.name("rows").value(t.rowCount)
        j.name("cols").value(t.colCount)
        j.name("row_heights").beginArray()
        for (h in t.rowHeights) j.value(round2(h))
        j.endArray()
        j.name("col_widths").beginArray()
        for (w in t.colWidths) j.value(round2(w))
        j.endArray()
        j.name("stroke_rgba")
        writeRgba(j, t.strokeRgba)
        j.name("stroke_width").value(t.strokeWidth)
        j.name("header_row").value(t.hasHeaderRow)
        j.name("header_col").value(t.hasHeaderCol)
        if (t.fillRgba != null) {
            j.name("fill_rgba")
            writeRgba(j, t.fillRgba!!)
        }
        if (t.cellTexts.isNotEmpty()) {
            j.name("cells").beginArray()
            for ((cell, text) in t.cellTexts) {
                if (text.isNotEmpty()) {
                    j.beginObject()
                    j.name("r").value(cell.first)
                    j.name("c").value(cell.second)
                    j.name("text").value(text)
                    j.endObject()
                }
            }
            j.endArray()
        }
        if (t.locked) j.name("locked").value(true)
        j.endObject()
    }

    private fun writeRgba(j: JsonWrite, c: Rgba) {
        j.beginArray().value(c.r).value(c.g).value(c.b).value(c.a).endArray()
    }

    private fun round2(v: Double): Double = if (v.isFinite()) Math.round(v * 100.0) / 100.0 else v

    private fun round3(v: Double): Double = if (v.isFinite()) Math.round(v * 1000.0) / 1000.0 else v

    // --- streaming json -> model ---

    /**
     * Read a `.xcanvas` from [input]. When [imageDir] is non-null the inserted images are streamed
     * out to fresh temp files there (never held in RAM) and the caller owns their lifetime; a null
     * dir skips them, which is what a validation-only read wants.
     */
    fun read(input: InputStream, imageDir: File? = null): InfiniteDocument {
        var manifest: ParsedManifest? = null
        val imageFiles = HashMap<String, File>()
        ZipInputStream(input).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name
                    if (name == "manifest.json") {
                        // Parsed straight off the zip stream, so a dense canvas's manifest is never
                        // materialized as bytes, a String, or a DOM.
                        if (manifest == null) {
                            manifest = try {
                                parseManifest(JsonPull(InputStreamReader(zis, Charsets.UTF_8)))
                            } catch (_: JsonPullException) {
                                throw XCanvasFormatException(NOT_XCANVAS)
                            }
                        }
                    } else if (name.startsWith("assets/image-")) {
                        if (imageDir != null) {
                            val f = File.createTempFile("img", null, imageDir)
                            FileOutputStream(f).use { zis.copyTo(it) }
                            imageFiles[name] = f
                        }
                    }
                    // Anything else is an asset from a newer version: skipped, never buffered.
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val m = manifest ?: throw XCanvasFormatException(NOT_XCANVAS)
        if (!m.formatOk) throw XCanvasFormatException(NOT_XCANVAS)

        val doc = InfiniteDocument(dpi = m.dpi)
        doc.background = m.background
        doc.lastView = m.view
        doc.waypoints.addAll(m.waypoints)

        // Image entries stream out of the zip after the manifest, so image items materialize only
        // now that their files exist; the recorded slot restores each one's z-order position.
        val items = ArrayList<CanvasItem>(m.items.size + m.images.size)
        items.addAll(m.items)
        var dropped = 0
        for (spec in m.images) {
            val item = materializeImage(spec, imageFiles)
            if (item == null) dropped++ else items.add(spec.index - dropped, item)
        }
        doc.addAll(items)
        return doc
    }

    private class ParsedManifest {
        var formatOk = false
        var writer = 0
        var dpi = PageSize.DEFAULT_DPI
        var background = CanvasBackground()
        var view: Waypoint? = null
        val waypoints = ArrayList<Waypoint>()
        val items = ArrayList<CanvasItem>()
        val images = ArrayList<PendingImage>()
    }

    /** An image item parsed before its asset entry has streamed out of the zip. */
    private class PendingImage(
        val index: Int,
        val asset: String,
        val rect: Rect?,
        val srcW: Int,
        val srcH: Int,
        val orientation: Int,
        val angle: Double,
        val locked: Boolean,
    )

    private fun parseManifest(p: JsonPull): ParsedManifest {
        val m = ParsedManifest()
        p.beginObject()
        while (p.hasNext()) {
            when (p.nextName()) {
                "format" -> {
                    if (stringOr(p, "") != FORMAT) throw XCanvasFormatException(NOT_XCANVAS)
                    m.formatOk = true
                }
                "writer" -> m.writer = intOr(p, 0)
                "dpi" -> m.dpi = intOr(p, PageSize.DEFAULT_DPI)
                "background" -> m.background = parseBackground(p)
                "view" -> m.view = parseWaypoint(p, named = false)
                "waypoints" -> parseWaypoints(p, m.waypoints)
                "items" -> parseItems(p, m.items, m.images)
                else -> p.skipValue()
            }
        }
        p.endObject()
        return m
    }

    private fun parseBackground(p: JsonPull): CanvasBackground {
        if (p.peek() != JsonPull.Token.BEGIN_OBJECT) {
            p.skipValue()
            return CanvasBackground()
        }
        val def = CanvasBackground()
        var pattern = def.pattern
        var patternColor = def.patternColor
        var spacing = def.spacing
        var paperColor: Rgba? = null
        p.beginObject()
        while (p.hasNext()) {
            when (p.nextName()) {
                "pattern" -> pattern = PagePattern.fromId(stringOrNull(p)) ?: def.pattern
                "pattern_color" -> patternColor = rgbaOrNull(p) ?: def.patternColor
                "spacing" -> spacing = doubleOr(p, def.spacing)
                "paper_color" -> paperColor = rgbaOrNull(p)
                else -> p.skipValue()
            }
        }
        p.endObject()
        return CanvasBackground(pattern, patternColor, spacing, paperColor)
    }

    private fun parseWaypoints(p: JsonPull, out: MutableList<Waypoint>) {
        if (p.peek() != JsonPull.Token.BEGIN_ARRAY) return p.skipValue()
        p.beginArray()
        while (p.hasNext()) parseWaypoint(p, named = true)?.let { out.add(it) }
        p.endArray()
    }

    /** One saved view. A malformed or non-object entry is skipped rather than failing the load. */
    private fun parseWaypoint(p: JsonPull, named: Boolean): Waypoint? {
        if (p.peek() != JsonPull.Token.BEGIN_OBJECT) {
            p.skipValue()
            return null
        }
        var name = ""
        var cx = 0.0
        var cy = 0.0
        var zoom = 1.0
        p.beginObject()
        while (p.hasNext()) {
            when (p.nextName()) {
                "name" -> name = stringOr(p, "")
                "cx" -> cx = doubleOr(p, 0.0)
                "cy" -> cy = doubleOr(p, 0.0)
                "zoom" -> zoom = doubleOr(p, 1.0)
                else -> p.skipValue()
            }
        }
        p.endObject()
        if (!cx.isFinite() || !cy.isFinite() || !zoom.isFinite() || zoom <= 0.0) return null
        return Waypoint(if (named) Waypoint.sanitizeName(name) else "", cx, cy, zoom)
    }

    private fun parseItems(p: JsonPull, items: MutableList<CanvasItem>, pending: MutableList<PendingImage>) {
        if (p.peek() != JsonPull.Token.BEGIN_ARRAY) return p.skipValue()
        p.beginArray()
        while (p.hasNext()) parseItem(p, items, pending)
        p.endArray()
    }

    /** Union of every kind's fields, so an item parses in one pass whatever its key order. */
    private class ItemScratch {
        var locked = false
        var kind: String? = null
        var tool: String? = null
        var config: ConfigScratch? = null
        var samples: MutableList<Sample>? = null
        var tilts: List<Double>? = null
        var azimuths: List<Double>? = null
        var speedScale = 1.0
        var smoothScale = 1.0
        var lagComp = 0.0
        var tipCount = 0
        var straight = false
        var asset: String? = null
        var rect: Rect? = null
        var srcW = 0
        var srcH = 0
        var orientation = 0
        var angle = 0.0
        var pos: Pt? = null
        var rows = TableItem.DEFAULT_ROWS
        var cols = TableItem.DEFAULT_COLS
        var rowHeights: MutableList<Double>? = null
        var colWidths: MutableList<Double>? = null
        var headerRow = true
        var headerCol = false
        var cells: MutableMap<Pair<Int, Int>, String>? = null
        var shape: String? = null
        var start: Pt? = null
        var end: Pt? = null
        var strokeRgba: Rgba? = null
        var strokeWidth = 3.0
        var fillRgba: Rgba? = null
        var points: List<Pt>? = null
        var neon = false
        var neonStrength = 0.6
        var dashed = false
        var dashLength = 10.0
        var dashGap = 8.0
    }

    /** Stroke config fields as written; null = absent, so defaults resolve exactly as before. */
    private class ConfigScratch {
        var baseWidth: Double? = null
        var pressureEnabled: Boolean? = null
        var pressureMinFactor: Double? = null
        var pressureGamma: Double? = null
        var directionStrength: Double? = null
        var rgba: Rgba? = null
        var speedStrength: Double? = null
        var taperEnabled: Boolean? = null
        var taperMinFactor: Double? = null
        var neon: Boolean? = null
        var neonStrength: Double? = null
        var dashLength: Double? = null
        var dashGap: Double? = null
        var highlighterAlpha: Double? = null
        var highlighterInverse: Boolean? = null
        var texture: String? = null
        var nibCut: Double? = null
        var velLo: Double? = null
        var velHi: Double? = null
        var velSens: Double? = null
        var velAmp: Double? = null
        var endVelSens: Double? = null
        var curvatureDecay: Double? = null
        var brushRatio: Double? = null
        var brushAngle: Double? = null
        var adaptiveSpline: Boolean? = null
    }

    private fun parseItem(p: JsonPull, items: MutableList<CanvasItem>, pending: MutableList<PendingImage>) {
        if (p.peek() != JsonPull.Token.BEGIN_OBJECT) return p.skipValue()
        val s = ItemScratch()
        p.beginObject()
        while (p.hasNext()) {
            when (p.nextName()) {
                "kind" -> s.kind = stringOr(p, "")
                "tool" -> s.tool = stringOr(p, "")
                "config" -> s.config = parseConfig(p)
                "samples" -> s.samples = parseSamples(p)
                "tilts" -> s.tilts = parseDoubles(p)
                "azimuths" -> s.azimuths = parseDoubles(p)
                "speed_scale" -> s.speedScale = doubleOr(p, 1.0)
                "smooth_scale" -> s.smoothScale = doubleOr(p, 1.0)
                "lag_comp" -> s.lagComp = doubleOr(p, 0.0)
                "tip_count" -> s.tipCount = intOr(p, 0)
                "straight" -> s.straight = boolOr(p, false)
                "asset" -> s.asset = stringOr(p, "")
                "rect" -> s.rect = rectOrNull(p)
                "src_w" -> s.srcW = intOr(p, 0)
                "src_h" -> s.srcH = intOr(p, 0)
                "orientation" -> s.orientation = intOr(p, 0)
                "angle" -> s.angle = doubleOr(p, 0.0)
                "pos" -> s.pos = ptOrNull(p)
                "rows" -> s.rows = intOr(p, TableItem.DEFAULT_ROWS)
                "cols" -> s.cols = intOr(p, TableItem.DEFAULT_COLS)
                "row_heights" -> s.rowHeights = parseDoubles(p)?.toMutableList()
                "col_widths" -> s.colWidths = parseDoubles(p)?.toMutableList()
                "header_row" -> s.headerRow = boolOr(p, true)
                "header_col" -> s.headerCol = boolOr(p, false)
                "cells" -> s.cells = parseCells(p)
                "shape" -> s.shape = stringOr(p, "")
                "start" -> s.start = ptOrNull(p)
                "end" -> s.end = ptOrNull(p)
                "stroke_rgba" -> s.strokeRgba = rgbaOrNull(p)
                "stroke_width" -> s.strokeWidth = doubleOr(p, 3.0)
                "fill_rgba" -> s.fillRgba = rgbaOrNull(p)
                "points" -> s.points = pointsOrNull(p)
                "neon" -> s.neon = boolOr(p, false)
                "neon_strength" -> s.neonStrength = doubleOr(p, 0.6)
                "dashed" -> s.dashed = boolOr(p, false)
                "dash_length" -> s.dashLength = doubleOr(p, 10.0)
                "dash_gap" -> s.dashGap = doubleOr(p, 8.0)
                "locked" -> s.locked = boolOr(p, false)
                else -> p.skipValue()
            }
        }
        p.endObject()
        val before = items.size
        when (s.kind) {
            Stroke.KIND -> items.add(buildStroke(s))
            ImageItem.KIND -> {
                val asset = s.asset
                if (!asset.isNullOrEmpty()) {
                    pending.add(
                        PendingImage(
                            items.size + pending.size, asset, s.rect, s.srcW, s.srcH,
                            s.orientation, s.angle, s.locked,
                        ),
                    )
                }
            }
            ShapeItem.KIND -> items.add(buildShape(s))
            TableItem.KIND -> items.add(buildTable(s))
            else -> {} // text and any unrecognized kind: skipped (forgiving)
        }
        // Absent on every canvas written before locking existed, which reads back as unlocked.
        if (s.locked && items.size > before) items[before].locked = true
    }

    private fun buildStroke(s: ItemScratch): Stroke {
        val tool = Tool.fromId(s.tool) ?: Tool.PEN
        val c = s.config
        val def = ToolConfig()
        val config = ToolConfig(
            baseWidth = c?.baseWidth ?: def.baseWidth,
            pressureEnabled = c?.pressureEnabled ?: def.pressureEnabled,
            pressureMinFactor = c?.pressureMinFactor ?: def.pressureMinFactor,
            pressureGamma = c?.pressureGamma ?: def.pressureGamma,
            directionStrength = c?.directionStrength ?: def.directionStrength,
            rgba = c?.rgba ?: def.rgba,
            speedStrength = c?.speedStrength ?: def.speedStrength,
            taperEnabled = c?.taperEnabled ?: def.taperEnabled,
            taperMinFactor = c?.taperMinFactor ?: ToolDefaults.DEFAULT_TAPER_TIP,
            neon = c?.neon ?: def.neon,
            neonStrength = c?.neonStrength ?: def.neonStrength,
            dashLength = c?.dashLength ?: def.dashLength,
            dashGap = c?.dashGap ?: def.dashGap,
            highlighterAlpha = c?.highlighterAlpha ?: def.highlighterAlpha,
            highlighterInverse = c?.highlighterInverse ?: def.highlighterInverse,
            texture = com.pnotes.core.tools.StrokeTexture.fromId(c?.texture),
            // Absent on every stroke drawn before the grind was a setting -> square, its old ink.
            nibCut = c?.nibCut ?: def.nibCut,
            velocityLowerBound = c?.velLo ?: def.velocityLowerBound,
            velocityUpperBound = c?.velHi ?: def.velocityUpperBound,
            velocitySensitivity = c?.velSens ?: def.velocitySensitivity,
            velocityAmplifier = c?.velAmp ?: def.velocityAmplifier,
            endVelocitySensitivity = c?.endVelSens ?: def.endVelocitySensitivity,
            curvatureDecay = c?.curvatureDecay ?: def.curvatureDecay,
            brushRatio = c?.brushRatio ?: def.brushRatio,
            brushAngle = c?.brushAngle ?: def.brushAngle,
            adaptiveSpline = c?.adaptiveSpline ?: def.adaptiveSpline,
        )
        return Stroke(tool, config, s.samples ?: mutableListOf(), s.speedScale, s.straight, s.smoothScale, s.lagComp)
            .also {
                it.setOrientation(s.tilts, s.azimuths)
                // The predicted tip survives the round trip: without it the reloaded stroke's tail
                // would be smoothed short of the ink the writer drew.
                it.tipCount = s.tipCount
            }
    }

    private fun buildShape(s: ItemScratch): ShapeItem {
        val kind = ShapeKind.fromId(s.shape)
        val strokeRgba = s.strokeRgba ?: DEFAULT_SHAPE_STROKE
        s.points?.let { verts ->
            return ShapeItem.poly(
                kind, verts, strokeRgba, s.strokeWidth, s.fillRgba, s.neon, s.neonStrength,
                s.dashed, s.dashLength, s.dashGap,
            )
        }
        return ShapeItem(
            shape = kind,
            start = s.start ?: Pt.ZERO,
            end = s.end ?: Pt.ZERO,
            strokeRgba = strokeRgba,
            strokeWidth = s.strokeWidth,
            fillRgba = s.fillRgba,
            neon = s.neon,
            neonStrength = s.neonStrength,
            dashed = s.dashed,
            dashLength = s.dashLength,
            dashGap = s.dashGap,
        )
    }

    private fun buildTable(s: ItemScratch): TableItem {
        val rows = s.rows.coerceIn(TableItem.MIN_ROWS, TableItem.MAX_ROWS)
        val cols = s.cols.coerceIn(TableItem.MIN_COLS, TableItem.MAX_COLS)
        val rowHeights = s.rowHeights ?: MutableList(rows) { TableItem.DEFAULT_ROW_HEIGHT }
        val colWidths = s.colWidths ?: MutableList(cols) { TableItem.DEFAULT_COL_WIDTH }
        return TableItem(
            pos = s.pos ?: Pt.ZERO,
            rowCount = rows,
            colCount = cols,
            rowHeights = rowHeights.toMutableList(),
            colWidths = colWidths.toMutableList(),
            strokeRgba = s.strokeRgba ?: TableItem.DEFAULT_STROKE_COLOR,
            strokeWidth = s.strokeWidth,
            hasHeaderRow = s.headerRow,
            hasHeaderCol = s.headerCol,
            fillRgba = s.fillRgba,
            cellTexts = s.cells ?: mutableMapOf(),
        )
    }

    private fun parseCells(p: JsonPull): MutableMap<Pair<Int, Int>, String>? {
        if (p.peek() != JsonPull.Token.BEGIN_ARRAY) { p.skipValue(); return null }
        val map = mutableMapOf<Pair<Int, Int>, String>()
        p.beginArray()
        while (p.hasNext()) {
            if (p.peek() != JsonPull.Token.BEGIN_OBJECT) { p.skipValue(); continue }
            var r = 0
            var c = 0
            var text = ""
            p.beginObject()
            while (p.hasNext()) {
                when (p.nextName()) {
                    "r" -> r = intOr(p, 0)
                    "c" -> c = intOr(p, 0)
                    "text" -> text = stringOr(p, "")
                    else -> p.skipValue()
                }
            }
            p.endObject()
            if (text.isNotEmpty()) {
                map[r to c] = text
            }
        }
        p.endArray()
        return map
    }

    private fun parseConfig(p: JsonPull): ConfigScratch? {
        if (p.peek() != JsonPull.Token.BEGIN_OBJECT) {
            p.skipValue()
            return null
        }
        val c = ConfigScratch()
        p.beginObject()
        while (p.hasNext()) {
            when (p.nextName()) {
                "base_width" -> c.baseWidth = doubleOrNull(p)
                "pressure_enabled" -> c.pressureEnabled = boolOrNull(p)
                "pressure_min_factor" -> c.pressureMinFactor = doubleOrNull(p)
                "pressure_gamma" -> c.pressureGamma = doubleOrNull(p)
                "direction_strength" -> c.directionStrength = doubleOrNull(p)
                "rgba" -> c.rgba = rgbaOrNull(p)
                "speed_strength" -> c.speedStrength = doubleOrNull(p)
                "taper_enabled" -> c.taperEnabled = boolOrNull(p)
                "taper_min_factor" -> c.taperMinFactor = doubleOrNull(p)
                "neon" -> c.neon = boolOrNull(p)
                "neon_strength" -> c.neonStrength = doubleOrNull(p)
                "dash_length" -> c.dashLength = doubleOrNull(p)
                "dash_gap" -> c.dashGap = doubleOrNull(p)
                "highlighter_alpha" -> c.highlighterAlpha = doubleOrNull(p)
                "highlighter_inverse" -> c.highlighterInverse = boolOrNull(p)
                "texture" -> c.texture = stringOrNull(p)
                "nib_cut" -> c.nibCut = doubleOrNull(p)
                "vel_lo" -> c.velLo = doubleOrNull(p)
                "vel_hi" -> c.velHi = doubleOrNull(p)
                "vel_sens" -> c.velSens = doubleOrNull(p)
                "vel_amp" -> c.velAmp = doubleOrNull(p)
                "end_vel_sens" -> c.endVelSens = doubleOrNull(p)
                "curvature_decay" -> c.curvatureDecay = doubleOrNull(p)
                "brush_ratio" -> c.brushRatio = doubleOrNull(p)
                "brush_angle" -> c.brushAngle = doubleOrNull(p)
                "adaptive_spline" -> c.adaptiveSpline = boolOrNull(p)
                else -> p.skipValue()
            }
        }
        p.endObject()
        return c
    }

    /** A flat JSON array of doubles, or null when the value is absent or not an array. */
    private fun parseDoubles(p: JsonPull): List<Double>? {
        if (p.peek() != JsonPull.Token.BEGIN_ARRAY) {
            p.skipValue()
            return null
        }
        val out = ArrayList<Double>()
        p.beginArray()
        while (p.hasNext()) out.add(doubleOr(p, 0.0))
        p.endArray()
        return out
    }

    private fun parseSamples(p: JsonPull): MutableList<Sample>? {
        if (p.peek() != JsonPull.Token.BEGIN_ARRAY) {
            p.skipValue()
            return null
        }
        val out = ArrayList<Sample>()
        p.beginArray()
        while (p.hasNext()) {
            if (p.peek() != JsonPull.Token.BEGIN_ARRAY) {
                p.skipValue()
                continue
            }
            p.beginArray()
            val x = if (p.hasNext()) doubleOr(p, 0.0) else 0.0
            val y = if (p.hasNext()) doubleOr(p, 0.0) else 0.0
            val pressure = if (p.hasNext()) doubleOr(p, 1.0) else 1.0
            // 4th element (relative ms) is present only for speed-pen strokes; absent means 0.
            val t = if (p.hasNext()) doubleOr(p, 0.0) else 0.0
            while (p.hasNext()) p.skipValue()
            p.endArray()
            out.add(Sample(x, y, pressure, t))
        }
        p.endArray()
        return out
    }

    private fun materializeImage(spec: PendingImage, imageFiles: Map<String, File>): ImageItem? {
        val file = imageFiles[spec.asset] ?: return null
        var w = spec.srcW
        var h = spec.srcH
        if (w <= 0 || h <= 0) {
            val probed = imageCodec.probeFile(file.path) ?: return null
            w = probed.width
            h = probed.height
        }
        val rect = spec.rect ?: Rect(0.0, 0.0, w.toDouble(), h.toDouble())
        return ImageItem(ImageData(file, w, h), rect, spec.orientation, spec.angle)
            .also { it.locked = spec.locked }
    }

    // --- streaming value helpers (mirroring org.json's forgiving opt* coercions) ---

    private fun doubleOr(p: JsonPull, def: Double): Double = doubleOrNull(p) ?: def

    private fun doubleOrNull(p: JsonPull): Double? = when (p.peek()) {
        JsonPull.Token.NUMBER -> p.nextDouble()
        JsonPull.Token.STRING -> p.nextString().toDoubleOrNull()
        else -> {
            p.skipValue()
            null
        }
    }

    private fun intOr(p: JsonPull, def: Int): Int = intOrNull(p) ?: def

    private fun intOrNull(p: JsonPull): Int? = when (p.peek()) {
        JsonPull.Token.NUMBER -> p.nextInt()
        JsonPull.Token.STRING -> p.nextString().let { it.toIntOrNull() ?: it.toDoubleOrNull()?.toInt() }
        else -> {
            p.skipValue()
            null
        }
    }

    private fun boolOr(p: JsonPull, def: Boolean): Boolean = boolOrNull(p) ?: def

    private fun boolOrNull(p: JsonPull): Boolean? = when (p.peek()) {
        JsonPull.Token.BOOLEAN -> p.nextBoolean()
        JsonPull.Token.STRING -> when (p.nextString().lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
        else -> {
            p.skipValue()
            null
        }
    }

    private fun stringOr(p: JsonPull, def: String): String = stringOrNull(p) ?: def

    private fun stringOrNull(p: JsonPull): String? = when (p.peek()) {
        JsonPull.Token.STRING -> p.nextString()
        else -> {
            p.skipValue()
            null
        }
    }

    private fun rgbaOrNull(p: JsonPull): Rgba? {
        if (p.peek() != JsonPull.Token.BEGIN_ARRAY) {
            p.skipValue()
            return null
        }
        val channels = ArrayList<Int>(4)
        p.beginArray()
        while (p.hasNext()) channels.add(intOr(p, 0))
        p.endArray()
        return Rgba.fromList(channels)
    }

    private fun ptOrNull(p: JsonPull): Pt? {
        if (p.peek() != JsonPull.Token.BEGIN_ARRAY) {
            p.skipValue()
            return null
        }
        var count = 0
        var x = 0.0
        var y = 0.0
        p.beginArray()
        while (p.hasNext()) {
            when (count) {
                0 -> x = doubleOr(p, 0.0)
                1 -> y = doubleOr(p, 0.0)
                else -> p.skipValue()
            }
            count++
        }
        p.endArray()
        return if (count >= 2) Pt(x, y) else null
    }

    private fun rectOrNull(p: JsonPull): Rect? {
        if (p.peek() != JsonPull.Token.BEGIN_ARRAY) {
            p.skipValue()
            return null
        }
        val v = DoubleArray(4)
        var count = 0
        p.beginArray()
        while (p.hasNext()) {
            if (count < 4) v[count] = doubleOr(p, 0.0) else p.skipValue()
            count++
        }
        p.endArray()
        return if (count >= 4) Rect(v[0], v[1], v[2], v[3]) else null
    }

    private fun pointsOrNull(p: JsonPull): List<Pt>? {
        if (p.peek() != JsonPull.Token.BEGIN_ARRAY) {
            p.skipValue()
            return null
        }
        val out = ArrayList<Pt>()
        p.beginArray()
        while (p.hasNext()) ptOrNull(p)?.let { out.add(it) }
        p.endArray()
        return if (out.size >= 2) out else null
    }

    companion object {
        const val FORMAT = "xcanvas"
        const val VERSION = 1

        /** File extension of the bundle, without the dot. */
        const val EXTENSION = InfiniteDocument.EXTENSION

        /** The com.pnotes versionCode stamped into manifests this build writes. Old readers
         *  ignore the unknown key; new readers use it to date a file's conventions. */
        const val WRITER = 47

        /** Default shape outline colour for a shape whose stroke colour did not survive. */
        private val DEFAULT_SHAPE_STROKE = Rgba(0, 230, 118, 255)

        private const val NOT_XCANVAS = "Not a pnotes canvas"
    }
}

private fun ZipOutputStream.putStored(name: String, file: File, isCancelled: () -> Boolean) {
    val crc = CRC32()
    val buf = ByteArray(64 * 1024)
    FileInputStream(file).use { input ->
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            crc.update(buf, 0, n)
        }
    }
    val size = file.length()
    val entry = ZipEntry(name).apply {
        method = ZipEntry.STORED
        this.size = size
        compressedSize = size
        this.crc = crc.value
    }
    putNextEntry(entry)
    FileInputStream(file).use { input ->
        while (true) {
            if (isCancelled()) throw CanvasCodec.WriteCancelled()
            val n = input.read(buf)
            if (n < 0) break
            write(buf, 0, n)
        }
    }
    closeEntry()
}
