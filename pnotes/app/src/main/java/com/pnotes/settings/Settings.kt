package com.pnotes.settings

import com.pnotes.core.model.PagePattern
import com.pnotes.core.model.PageStyle
import com.pnotes.core.model.Rgba
import com.pnotes.core.pal.FontFace
import com.pnotes.core.text.FlowDefaults
import com.pnotes.core.text.FlowMargins
import com.pnotes.core.tools.EraseMode
import com.pnotes.core.tools.InkPalette
import com.pnotes.core.tools.ShapeConfig
import com.pnotes.core.tools.ShapeKind
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolConfig
import com.pnotes.core.tools.ToolDefaults
import com.pnotes.core.tools.ToolbarLayout
import org.json.JSONArray
import org.json.JSONObject

/** Presentation-server defaults (spec 09 §5 / 12 §10). */
data class PresentationSettings(
    val port: Int = 8000,
    val scope: String = "localhost", // "localhost" | "lan"
    val mode: String = "page", // "page" | "follow"
    val quality: String = "medium", // "low" | "medium" | "high"
    val maxFps: Int = 30,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("port", port).put("scope", scope).put("mode", mode)
        .put("quality", quality).put("max_fps", maxFps)

    companion object {
        fun fromJson(o: JSONObject?): PresentationSettings {
            if (o == null) return PresentationSettings()
            return PresentationSettings(
                port = o.optInt("port", 8000),
                scope = if (o.optString("scope", "localhost") == "lan") "lan" else "localhost",
                mode = if (o.optString("mode", "page") == "follow") "follow" else "page",
                quality = o.optString("quality", "medium").let { if (it in setOf("low", "medium", "high")) it else "medium" },
                maxFps = o.optInt("max_fps", 30),
            )
        }
    }
}

/**
 * How the in-app explorer orders entries. The chosen key sorts within each group (folders first,
 * then files); [Settings.explorerSortDescending] flips the direction.
 */
enum class ExplorerSortKey(val id: String) {
    NAME("name"),
    MODIFIED("modified"),
    SIZE("size");

    companion object {
        fun fromId(id: String): ExplorerSortKey = entries.firstOrNull { it.id == id } ?: MODIFIED
    }
}

/** All persistent non-document state (spec 09 §2). */
data class Settings(
    /** Global View-menu defaults; per-note [com.pnotes.canvas.ViewOverrides] shadow them. */
    val viewDefaults: com.pnotes.canvas.ViewSettings = com.pnotes.canvas.ViewSettings(),
    val tools: Map<Tool, ToolConfig> = emptyMap(),
    val shapeConfig: ShapeConfig = ShapeConfig(),
    val toolbarColors: List<Rgba> = InkPalette.presets,
    val toolbarColorCount: Int = 5,
    val toolbarLayout: ToolbarLayout = ToolbarLayout.DEFAULT,
    /** The infinite canvas's own bar. A separate layout because the two hold different items. */
    val canvasToolbarLayout: ToolbarLayout = ToolbarLayout.CANVAS_DEFAULT,
    val activeColor: Int = 0,
    /** The tool armed when the app was last paused, re-armed on the next launch. */
    val lastTool: Tool = Tool.DEFAULT,
    val recentColors: List<Rgba> = emptyList(),
    /** Persisted SAF tree URI for the in-app file explorer's root folder, or null. */
    val browseRoot: String? = null,
    /** Whether the next launch opens the home screen (true) or the last-open note (false). */
    val startOnHome: Boolean = true,
    val sidebarVisible: Boolean = false,
    /** Explorer grid sort: which field orders entries, and whether it's reversed. */
    val explorerSortKey: ExplorerSortKey = ExplorerSortKey.MODIFIED,
    val explorerSortDescending: Boolean = true,
    val renderScale: Double = 1.0,
    val presentation: PresentationSettings = PresentationSettings(),
    /** All Pages style stamped onto every newly created note; empty ⇒ none saved. */
    val newNoteStyle: PageStyle = PageStyle(),
    /** Flow (text tool) defaults stamped onto every newly created note; empty ⇒ none saved. */
    val newNoteFlow: FlowDefaults = FlowDefaults(),
    val prefs: Preferences = Preferences(),
    /** One-shot flag: the first-run stylus check (which may auto-enable finger-draw) has run. */
    val fingerDrawAutoChecked: Boolean = false,
    /** Quick stroke width presets (default 3: 4.0, 7.0, 10.0, max 5). */
    val widthPresets: List<Double> = listOf(4.0, 7.0, 10.0),
    /** Document URIs of user's favorited / starred notes. */
    val favorites: Set<String> = emptySet(),
    /** Document URIs of user's pinned notes. */
    val pinned: Set<String> = emptySet(),
) {
    fun configFor(tool: Tool): ToolConfig = tools[tool] ?: ToolDefaults.configFor(tool)

    /** Push a colour to the front of recent colours, de-duped, capped at 24. */
    fun rememberColor(c: Rgba): Settings =
        copy(recentColors = (listOf(c) + recentColors.filter { it != c }).take(24))

    fun toJson(): JSONObject {
        val toolsObj = JSONObject()
        for (tool in ToolDefaults.persistedTools) {
            toolsObj.put(tool.id, toolConfigJson(configFor(tool)))
        }
        toolsObj.put("shape", shapeConfigJson(shapeConfig))
        return JSONObject()
            .put("tools", toolsObj)
            .put("toolbar_colors", JSONArray().apply { toolbarColors.forEach { put(rgbaArr(it)) } })
            .put("toolbar_color_count", toolbarColorCount)
            .put("toolbar_layout", toolbarLayoutJson(toolbarLayout))
            .put("canvas_toolbar_layout", toolbarLayoutJson(canvasToolbarLayout))
            .put("active_color", activeColor)
            .put("last_tool", lastTool.id)
            .put("recent_colors", JSONArray().apply { recentColors.forEach { put(rgbaArr(it)) } })
            .apply { browseRoot?.let { put("browse_root", it) } }
            .put("start_on_home", startOnHome)
            .put("sidebar_visible", sidebarVisible)
            .put("explorer_sort_key", explorerSortKey.id)
            .put("explorer_sort_descending", explorerSortDescending)
            .put("render_scale", renderScale)
            .put("presentation", presentation.toJson())
            .apply { if (!newNoteStyle.isEmpty) put("new_note_style", pageStyleJson(newNoteStyle)) }
            .apply { if (!newNoteFlow.isEmpty) put("new_note_flow", flowDefaultsJson(newNoteFlow)) }
            .put("prefs", prefs.toJson())
            .put("view_defaults", com.pnotes.platform.ViewSettingsJson.write(JSONObject(), viewDefaults))
            .put("finger_draw_auto_checked", fingerDrawAutoChecked)
            .put("width_presets", org.json.JSONArray(widthPresets))
            .put("favorites", org.json.JSONArray(favorites))
            .put("pinned", org.json.JSONArray(pinned))
    }

    companion object {
        fun fromJson(o: JSONObject): Settings {
            val toolsObj = o.optJSONObject("tools")
            val tools = HashMap<Tool, ToolConfig>()
            if (toolsObj != null) {
                for (tool in ToolDefaults.persistedTools) {
                    toolsObj.optJSONObject(tool.id)?.let { tools[tool] = toolConfig(it, tool) }
                }
            }
            val shape = toolsObj?.optJSONObject("shape")?.let { shapeConfig(it) } ?: ShapeConfig()

            val colors = rgbaList(o.optJSONArray("toolbar_colors")).toMutableList()
            while (colors.size < 7) colors.add(InkPalette.presets[colors.size])

            return Settings(
                viewDefaults = o.optJSONObject("view_defaults")
                    ?.let { com.pnotes.platform.ViewSettingsJson.read(it) }
                    ?: legacyViewDefaults(o.optJSONObject("prefs")),
                tools = tools,
                shapeConfig = shape,
                toolbarColors = colors.take(7),
                toolbarColorCount = o.optInt("toolbar_color_count", 5).coerceIn(1, 7),
                toolbarLayout = toolbarLayout(o.optJSONObject("toolbar_layout")),
                canvasToolbarLayout = toolbarLayout(
                    o.optJSONObject("canvas_toolbar_layout"),
                    ToolbarLayout.CANVAS_ITEMS,
                    ToolbarLayout.CANVAS_DEFAULT,
                ),
                activeColor = o.optInt("active_color", 0).coerceIn(0, 6),
                // Only tools the toolbar can arm come back; a transient one (e.g. TEXT_BOX) would
                // leave the bar showing a tool the user never picked.
                lastTool = Tool.fromId(o.optString("last_tool", ""))
                    ?.takeIf { it in Tool.wheelOrder } ?: Tool.DEFAULT,
                recentColors = rgbaList(o.optJSONArray("recent_colors")).take(24),
                browseRoot = o.optString("browse_root", "").ifEmpty { null },
                startOnHome = o.optBoolean("start_on_home", true),
                sidebarVisible = o.optBoolean("sidebar_visible", false),
                explorerSortKey = ExplorerSortKey.fromId(o.optString("explorer_sort_key", "modified")),
                explorerSortDescending = o.optBoolean("explorer_sort_descending", true),
                renderScale = o.optDouble("render_scale", 1.0),
                presentation = PresentationSettings.fromJson(o.optJSONObject("presentation")),
                newNoteStyle = pageStyle(o.optJSONObject("new_note_style")),
                newNoteFlow = flowDefaults(o.optJSONObject("new_note_flow")),
                prefs = Preferences.fromJson(o.optJSONObject("prefs")),
                fingerDrawAutoChecked = o.optBoolean("finger_draw_auto_checked", false),
                widthPresets = o.optJSONArray("width_presets")?.let { a ->
                    (0 until a.length()).map { a.optDouble(it, 4.0) }
                }?.takeIf { it.isNotEmpty() } ?: listOf(4.0, 7.0, 10.0),
                favorites = o.optJSONArray("favorites")?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optString(it, null) }.toSet()
                } ?: emptySet(),
                pinned = o.optJSONArray("pinned")?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optString(it, null) }.toSet()
                } ?: emptySet(),
            )
        }

        /** Settings written before the View menu's Global tab: the old PDF dark-mode
         *  checkboxes seed the global invert / keep-images defaults. */
        private fun legacyViewDefaults(prefs: JSONObject?): com.pnotes.canvas.ViewSettings {
            if (prefs == null) return com.pnotes.canvas.ViewSettings()
            return com.pnotes.canvas.ViewSettings(
                invert = if (prefs.optBoolean("pdf_dark_mode", false)) 100 else 0,
                keepImages = prefs.optBoolean("pdf_keep_image_colors", false),
            )
        }

        private fun rgbaArr(c: Rgba) = JSONArray().put(c.r).put(c.g).put(c.b).put(c.a)

        private fun rgba(a: JSONArray?): Rgba? =
            a?.let { Rgba.fromList((0 until it.length()).map { i -> it.optInt(i, 0) }) }

        private fun pageStyleJson(s: PageStyle) = JSONObject()
            .apply { s.pageColor?.let { put("page_color", rgbaArr(it)) } }
            .apply { s.pattern?.let { put("pattern", it.id) } }
            .apply { s.patternColor?.let { put("pattern_color", rgbaArr(it)) } }
            .apply { s.spacing?.let { put("spacing", it) } }

        private fun pageStyle(o: JSONObject?): PageStyle {
            if (o == null) return PageStyle()
            return PageStyle(
                pageColor = rgba(o.optJSONArray("page_color")),
                pattern = PagePattern.fromId(o.optString("pattern", "")),
                patternColor = rgba(o.optJSONArray("pattern_color")),
                spacing = if (o.has("spacing")) o.optDouble("spacing") else null,
            )
        }

        private fun flowDefaultsJson(d: FlowDefaults) = JSONObject()
            .put("face", d.face.id)
            .put("mono_face", d.monoFace.id)
            .put("size_pt", d.sizePt)
            .apply { d.color?.let { put("color", rgbaArr(it)) } }
            .put("margin_left_mm", d.margins.leftMm)
            .put("margin_top_mm", d.margins.topMm)
            .put("margin_right_mm", d.margins.rightMm)
            .put("margin_bottom_mm", d.margins.bottomMm)

        private fun flowDefaults(o: JSONObject?): FlowDefaults {
            if (o == null) return FlowDefaults()
            val d = FlowDefaults()
            return FlowDefaults(
                face = FontFace(o.optString("face", d.face.id)),
                monoFace = FontFace(o.optString("mono_face", d.monoFace.id)),
                sizePt = o.optDouble("size_pt", d.sizePt),
                color = rgba(o.optJSONArray("color")),
                margins = FlowMargins(
                    leftMm = o.optDouble("margin_left_mm", FlowMargins.DEFAULT_MM),
                    topMm = o.optDouble("margin_top_mm", FlowMargins.DEFAULT_MM),
                    rightMm = o.optDouble("margin_right_mm", FlowMargins.DEFAULT_MM),
                    bottomMm = o.optDouble("margin_bottom_mm", FlowMargins.DEFAULT_MM),
                ),
            )
        }

        private fun rgbaList(arr: JSONArray?): List<Rgba> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                val a = arr.optJSONArray(i) ?: return@mapNotNull null
                Rgba.fromList((0 until a.length()).map { a.optInt(it, 0) })
            }
        }

        private fun toolConfigJson(c: ToolConfig) = JSONObject()
            .put("base_width", c.baseWidth)
            .put("pressure_enabled", c.pressureEnabled)
            .put("pressure_min_factor", c.pressureMinFactor)
            .put("pressure_gamma", c.pressureGamma)
            .put("direction_strength", c.directionStrength)
            .put("speed_strength", c.speedStrength)
            .put("taper_enabled", c.taperEnabled)
            .put("taper_min_factor", c.taperMinFactor)
            .put("neon", c.neon)
            .put("neon_strength", c.neonStrength)
            .put("dash_length", c.dashLength)
            .put("dash_gap", c.dashGap)
            .put("erase_mode", c.eraseMode.id)
            .put("switch_back_after_erase", c.switchBackAfterErase)
            .put("switch_back_after_select", c.switchBackAfterSelect)
            .put("straight_line", c.straightLine)
            .put("scale", c.scale)
            .put("highlighter_alpha", c.highlighterAlpha)
            .put("highlighter_inverse", c.highlighterInverse)
            .put("stability", c.stability)
            .put("two_finger_tap_toggle", c.twoFingerTapToggle)
            .put("erase_highlighter_only", c.eraseHighlighterOnly)
            .put("erase_shapes", c.eraseShapes)
            .put("texture", c.texture.id)
            .put("nib_cut", c.nibCut)
            .put("velocity_lower_bound", c.velocityLowerBound)
            .put("velocity_upper_bound", c.velocityUpperBound)
            .put("velocity_sensitivity", c.velocitySensitivity)
            .put("velocity_amplifier", c.velocityAmplifier)
            .put("end_velocity_sensitivity", c.endVelocitySensitivity)
            .put("curvature_decay", c.curvatureDecay)
            .put("brush_ratio", c.brushRatio)
            .put("brush_angle", c.brushAngle)
            .put("adaptive_spline", c.adaptiveSpline)
            .put("rgba", rgbaArr(c.rgba))
            .apply { c.colorOverride?.let { put("color_override", rgbaArr(it)) } }

        private fun toolConfig(o: JSONObject, tool: Tool): ToolConfig {
            val d = ToolDefaults.configFor(tool)
            val savedMinFactor = o.optDouble("pressure_min_factor", d.pressureMinFactor)
            // Migrate legacy 0.35 default to Star Notes 0.18 hairline dynamic range
            val minFactor = if (savedMinFactor == 0.35 && d.pressureMinFactor != 0.35) d.pressureMinFactor else savedMinFactor
            val savedGamma = o.optDouble("pressure_gamma", d.pressureGamma)
            // Migrate legacy 1.0 linear default to Star Notes 1.45 balanced power curve
            val gamma = if (savedGamma == 1.0 && d.pressureGamma != 1.0) d.pressureGamma else (savedGamma.takeIf { it.isFinite() }?.coerceIn(0.5, 2.5) ?: d.pressureGamma)
            return ToolConfig(
                baseWidth = o.optDouble("base_width", d.baseWidth),
                pressureEnabled = o.optBoolean("pressure_enabled", d.pressureEnabled),
                pressureMinFactor = minFactor,
                pressureGamma = gamma,
                directionStrength = o.optDouble("direction_strength", d.directionStrength),
                rgba = Rgba.fromList(o.optJSONArray("rgba")?.let { a -> (0 until a.length()).map { a.optInt(it, 0) } }) ?: d.rgba,
                speedStrength = o.optDouble("speed_strength", d.speedStrength),
                taperEnabled = o.optBoolean("taper_enabled", d.taperEnabled),
                taperMinFactor = o.optDouble("taper_min_factor", d.taperMinFactor),
                neon = o.optBoolean("neon", d.neon),
                neonStrength = o.optDouble("neon_strength", d.neonStrength),
                dashLength = o.optDouble("dash_length", d.dashLength),
                dashGap = o.optDouble("dash_gap", d.dashGap),
                eraseMode = EraseMode.fromId(o.optString("erase_mode", d.eraseMode.id)),
                switchBackAfterErase = o.optBoolean("switch_back_after_erase", d.switchBackAfterErase),
                switchBackAfterSelect = o.optBoolean("switch_back_after_select", d.switchBackAfterSelect),
                straightLine = o.optBoolean("straight_line", d.straightLine),
                scale = o.optBoolean("scale", d.scale),
                highlighterAlpha = o.optDouble("highlighter_alpha", d.highlighterAlpha),
                highlighterInverse = o.optBoolean("highlighter_inverse", d.highlighterInverse),
                stability = o.optDouble("stability", d.stability),
                twoFingerTapToggle = o.optBoolean("two_finger_tap_toggle", d.twoFingerTapToggle),
                eraseHighlighterOnly = o.optBoolean("erase_highlighter_only", d.eraseHighlighterOnly),
                eraseShapes = o.optBoolean("erase_shapes", d.eraseShapes),
                texture = com.pnotes.core.tools.StrokeTexture.fromId(o.optString("texture", d.texture.id)),
                nibCut = o.optDouble("nib_cut", d.nibCut),
                velocityLowerBound = o.optDouble("velocity_lower_bound", d.velocityLowerBound),
                velocityUpperBound = o.optDouble("velocity_upper_bound", d.velocityUpperBound),
                velocitySensitivity = o.optDouble("velocity_sensitivity", d.velocitySensitivity),
                velocityAmplifier = o.optDouble("velocity_amplifier", d.velocityAmplifier),
                endVelocitySensitivity = o.optDouble("end_velocity_sensitivity", d.endVelocitySensitivity),
                curvatureDecay = o.optDouble("curvature_decay", d.curvatureDecay),
                brushRatio = o.optDouble("brush_ratio", d.brushRatio),
                brushAngle = o.optDouble("brush_angle", d.brushAngle),
                adaptiveSpline = false, // always off: CurvatureSpline causes zigzag on raw digitizer input
                colorOverride = o.optJSONArray("color_override")
                    ?.let { a -> Rgba.fromList((0 until a.length()).map { i -> a.optInt(i, 0) }) }
                    ?: d.colorOverride,
            )
        }

        private fun shapeConfigJson(c: ShapeConfig) = JSONObject()
            .put("shape", c.shape.id).put("stroke_width", c.strokeWidth).put("fill", c.fill)
            .put("fill_alpha", c.fillAlpha)
            .put("neon", c.neon).put("neon_strength", c.neonStrength)
            .put("dashed", c.dashed).put("dash_length", c.dashLength).put("dash_gap", c.dashGap)

        private fun shapeConfig(o: JSONObject) = ShapeConfig(
            shape = ShapeKind.fromId(o.optString("shape", "rectangle")),
            strokeWidth = o.optDouble("stroke_width", 3.0),
            fill = o.optBoolean("fill", false),
            fillAlpha = o.optDouble("fill_alpha", ShapeConfig.FILL_ALPHA)
                .coerceIn(ShapeConfig.FILL_ALPHA_MIN, ShapeConfig.FILL_ALPHA_MAX),
            neon = o.optBoolean("neon", false),
            neonStrength = o.optDouble("neon_strength", 0.6),
            dashed = o.optBoolean("dashed", false),
            dashLength = o.optDouble("dash_length", 10.0),
            dashGap = o.optDouble("dash_gap", 8.0),
        )

        private fun toolbarLayoutJson(layout: ToolbarLayout): JSONObject {
            val secArr = JSONArray()
            for (sec in layout.sections) {
                val entryArr = JSONArray()
                for (e in sec.entries) {
                    entryArr.put(JSONObject().put("id", e.item.id).put("visible", e.visible))
                }
                secArr.put(entryArr)
            }
            return JSONObject().put("sections", secArr)
        }

        private fun toolbarLayout(
            o: JSONObject?,
            among: Set<com.pnotes.core.tools.ToolbarItem> = ToolbarLayout.NOTE_ITEMS,
            fallback: ToolbarLayout = ToolbarLayout.DEFAULT,
        ): ToolbarLayout {
            if (o == null) return fallback
            val secArr = o.optJSONArray("sections") ?: return fallback
            val raw = ArrayList<List<Pair<String, Boolean>>>()
            for (i in 0 until secArr.length()) {
                val entryArr = secArr.optJSONArray(i) ?: continue
                val entries = ArrayList<Pair<String, Boolean>>()
                for (j in 0 until entryArr.length()) {
                    val e = entryArr.optJSONObject(j) ?: continue
                    entries.add(e.optString("id") to e.optBoolean("visible", true))
                }
                raw.add(entries)
            }
            return ToolbarLayout.fromRaw(raw, among, fallback)
        }
    }
}
