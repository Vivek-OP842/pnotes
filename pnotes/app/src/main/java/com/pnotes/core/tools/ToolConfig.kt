package com.pnotes.core.tools

import com.pnotes.core.model.Rgba

/**
 * Eraser behaviour (spec 04 §2): STROKE removes a whole stroke on contact; AREA removes only the
 * portion of a stroke the eraser passes over, splitting it into fragments. Persisted as [id].
 */
enum class EraseMode(val id: String) {
    STROKE("stroke"),
    AREA("area");

    companion object {
        fun fromId(id: String?): EraseMode = entries.firstOrNull { it.id == id } ?: AREA
    }
}

/**
 * The style record carried by stroke tools and used as the eraser/lasso size
 * carrier (spec 04 §2). A [com.pnotes.core.model.Stroke] stores a **copy** at
 * pen-down so re-tuning a tool never restyles existing strokes.
 *
 * The default `ToolConfig()` is `(3.0, on, 0.35, 0.0)` (spec 04 §3).
 */
data class ToolConfig(
    val baseWidth: Double = 3.0,
    val pressureEnabled: Boolean = true,
    /** `m` — width fraction at zero pressure (0.18 allows thin hairlines like Star Notes). */
    val pressureMinFactor: Double = 0.18,
    /** `ds` — calligraphic direction effect (0 = none). */
    val directionStrength: Double = 0.0,
    val rgba: Rgba = InkPalette.DEFAULT,
    // New fields go *after* `rgba` so the positional constructor stays stable.
    /** Velocity thinning (the speed pen): 0 = none; up the line thins as it moves faster. */
    val speedStrength: Double = 0.0,
    /** Taper pen: when true the width eases across the whole stroke, full at the head down to
     *  [taperMinFactor] of full width at the tip. */
    val taperEnabled: Boolean = false,
    /** Neon glow: a soft luminous halo under a bright core. Composable onto any stroke tool. */
    val neon: Boolean = false,
    /** Glow intensity (the neon halo): 0 = faint & tight, 1 = bright & wide. Only used when [neon]. */
    val neonStrength: Double = 0.6,
    /** Dashed pen: length of each dash, in content px. Only used by [Tool.DASHED]. */
    val dashLength: Double = 10.0,
    /** Dashed pen: gap between dashes, in content px. Only used by [Tool.DASHED]. */
    val dashGap: Double = 8.0,
    /** Eraser behaviour: whole-stroke (STROKE) or partial (AREA). Only used by [Tool.ERASER]. */
    val eraseMode: EraseMode = EraseMode.AREA,
    /** Eraser: re-arm the pen/highlighter used before the eraser once an erase lifts. Only [Tool.ERASER]. */
    val switchBackAfterErase: Boolean = false,
    /** Select: re-arm the pen/highlighter used before the select tool once a selection action completes. Only [Tool.SELECT]. */
    val switchBackAfterSelect: Boolean = false,
    /** Highlighter: commit each drag as a single straight segment (start → release). Only [Tool.HIGHLIGHTER]. */
    val straightLine: Boolean = false,
    /** When false, the pen draws at a constant on-screen size: at pen-down the width (and the
     *  taper/dash extents) are divided by the current zoom, so the stroke looks the same
     *  thickness whatever zoom you draw at. The eraser uses it to size its radius. */
    val scale: Boolean = true,
    /** Highlighter translucency: render-time alpha scale, default 0.35 (the historical value, so
     *  legacy strokes that lack the field reload unchanged). Capped to [0.10, 0.90] by the UI so it
     *  stays a MULTIPLY-blended marker. Only used by [Tool.HIGHLIGHTER]. */
    val highlighterAlpha: Double = 0.35,
    /** Per-tool colour override: when set, new strokes from this tool use this colour instead of
     *  the toolbar's active ink colour. null = follow the toolbar (the default for every tool). */
    val colorOverride: Rgba? = null,
    /** Taper pen: width fraction the tapered end keeps, in `[0, 1]` (the floor of the tail ease).
     *  0 = the tail comes to a sharp point; 0.1 = it bottoms out at a tenth of full width. Only
     *  used when [taperEnabled]. */
    val taperMinFactor: Double = 0.0,
    /** Inverse highlighter: composite with SCREEN instead of MULTIPLY, so the stroke lightens what
     *  is under it. A multiply has nothing to darken on a dark page, where it ends up tinting the
     *  light ink instead of the paper. Only used by [Tool.HIGHLIGHTER]. */
    val highlighterInverse: Boolean = false,
    /** Handwriting stability / streamline factor: 0.0 = raw / no smoothing, 0.5 = standard (50%), 1.0 = maximum stability (100%). */
    val stability: Double = 0.5,
    /** Two-finger tap on canvas shifts to eraser and second tap switches back to previous tool. Only used by [Tool.ERASER]. */
    val twoFingerTapToggle: Boolean = false,
    /** Eraser: only erase highlighter strokes; handwriting and shapes remain untouched. Only [Tool.ERASER]. */
    val eraseHighlighterOnly: Boolean = false,
    /** Eraser: include recognized geometric shapes when erasing. When false, shapes are protected. Only [Tool.ERASER]. */
    val eraseShapes: Boolean = true,
    /** Active brush texture type for specialized texture rendering (paper grain, chalk, charcoal, etc.). */
    val texture: StrokeTexture = StrokeTexture.SMOOTH,
    /**
     * The calligraphy nib's grind, in radians: how far the broad face is turned from the direction
     * the barrel leans, `[com.pnotes.core.stroke.NibAxis.of]`'s cut. 0 is a square-cut italic, the
     * ink every calligraphy stroke drew before this setting existed; a negative angle is an oblique,
     * the grind a right-handed writer usually wants. Clamped to ±90° by the engine, and ignored
     * unless the stylus reports a tilt — with no orientation to read there is no second axis to turn.
     */
    val nibCut: Double = 0.0,
    // --- Star Notes Physics Engine Parameters ---
    /** Lower bound for velocity thinning (dp/ms) */
    val velocityLowerBound: Double = 0.0,
    /** Upper bound for velocity thinning (dp/ms) */
    val velocityUpperBound: Double = 0.6,
    /** Dynamic velocity sensitivity factor (0.0 = none, 1.0 = standard Star Notes response) */
    val velocitySensitivity: Double = 0.5,
    /** Dynamic velocity thinning amplifier (0.0 = none, 1.0 = standard, 2.0 = strong speed pen) */
    val velocityAmplifier: Double = 1.0,
    /** End-velocity lift-off sensitivity for natural cursive flick / tapered hairline exit (0.0 to 1.0) */
    val endVelocitySensitivity: Double = 0.4,
    /** Curvature width decay factor: dynamic compression/expansion on sharp corners (0.0 = none) */
    val curvatureDecay: Double = 0.0,
    /** Brush footprint aspect ratio (width:height ratio; 1.0 = circular disc, 0.2 = thin chisel) */
    val brushRatio: Double = 1.0,
    /** Brush footprint angle relative to stroke trajectory in radians */
    val brushAngle: Double = 0.0,
    /** Adaptive centripetal Catmull-Rom spline subdivision for silky smooth cursive loops, spirals & organic curves at any zoom. */
    val adaptiveSpline: Boolean = false,
    /** Non-linear stylus pressure response (gamma 1.45 matches Star Notes balanced handwriting curve). */
    val pressureGamma: Double = 1.45,
)

/**
 * Brush texture types for rich and realistic canvas rendering.
 */
enum class StrokeTexture(val id: String, val label: String) {
    SMOOTH("smooth", "Smooth"),
    PENCIL("pencil", "Pencil"),
    CALLIGRAPHY("calligraphy", "Callig"),
    TAPER("taper", "Taper"),
    SPEED("speed", "Speed"),
    MARKER("marker", "Marker"),
    CHALK("chalk", "Chalk"),
    NEON("neon", "Neon"),
    DASHED("dashed", "Dashed"),
    WATERCOLOR("watercolor", "Waterc"),
    CHARCOAL("charcoal", "Charc"),
    DOTTED("dotted", "Dotted");

    companion object {
        fun fromId(id: String?): StrokeTexture = entries.firstOrNull { it.id == id } ?: SMOOTH
    }
}

/** Factory defaults per tool (spec 04 §3). */
object ToolDefaults {
    /** Tip width the taper pen eases down to, and the value assumed for legacy taper strokes that
     *  predate the setting, so they reload tapered rather than as a sharp point. */
    const val DEFAULT_TAPER_TIP = 0.30

    fun configFor(tool: Tool): ToolConfig = when (tool) {
        // Ordinary writing with Star Notes balanced pressure curve and responsive dynamic range
        Tool.PEN -> ToolConfig(baseWidth = 3.0, rgba = InkPalette.presets[0], pressureEnabled = true, pressureMinFactor = 0.18, directionStrength = 0.0, pressureGamma = 1.45, endVelocitySensitivity = 0.0)
        // Dashed pen: uniform-width (pressure off), so its dashes stay even; dash/gap set the rhythm.
        Tool.DASHED -> ToolConfig(baseWidth = 3.0, rgba = InkPalette.presets[0], pressureEnabled = false, pressureMinFactor = 1.0, directionStrength = 0.0, dashLength = 10.0, dashGap = 8.0)
        Tool.CALLIGRAPHY -> ToolConfig(
            baseWidth = 6.0,
            rgba = InkPalette.presets[0],
            pressureEnabled = true,
            pressureMinFactor = 0.22,
            directionStrength = 0.60,
            pressureGamma = 1.45,
        )
        Tool.SPEED -> ToolConfig(baseWidth = 3.0, rgba = InkPalette.presets[0], pressureEnabled = true, pressureMinFactor = 0.18, directionStrength = 0.0, speedStrength = 0.80, pressureGamma = 1.45)
        Tool.TAPER -> ToolConfig(baseWidth = 3.0, rgba = InkPalette.presets[0], pressureEnabled = true, pressureMinFactor = 0.18, directionStrength = 0.0, taperEnabled = true, taperMinFactor = DEFAULT_TAPER_TIP, pressureGamma = 1.45)
        Tool.HIGHLIGHTER -> ToolConfig(baseWidth = 16.0, rgba = InkPalette.presets[2], pressureEnabled = false, pressureMinFactor = 1.0, directionStrength = 0.0, highlighterAlpha = 0.50, colorOverride = null)
        Tool.ERASER -> ToolConfig(baseWidth = 24.0, pressureEnabled = false, pressureMinFactor = 1.0, directionStrength = 0.0, eraseMode = EraseMode.AREA)
        Tool.LASSO -> ToolConfig(baseWidth = 2.0, pressureEnabled = false, pressureMinFactor = 1.0, directionStrength = 0.0)
        else -> ToolConfig()
    }

    /** Tools whose config is persisted in settings (spec 09 §2). */
    val persistedTools = listOf(Tool.PEN, Tool.DASHED, Tool.CALLIGRAPHY, Tool.SPEED, Tool.TAPER, Tool.HIGHLIGHTER, Tool.ERASER, Tool.SELECT, Tool.LASSO)
}

/**
 * Conversions between the friendly popup controls and the internal style fields
 * (spec 04 §5). All are exact inverses on their valid ranges.
 */
object ToolConversions {
    /**
     * The NIB CUT slider's bounds, in degrees. A writer names a grind in degrees of slant and buys
     * them that way, so the popup speaks degrees while [ToolConfig.nibCut] carries radians: 0 is a
     * square-cut italic and either side of it is an oblique. Past ±45° a nib is turned further than
     * any real grind, and the face is close to lying along the stroke rather than across it.
     */
    val NIB_CUT_RANGE_DEGREES = -45.0..45.0

    /** NIB CUT (degrees, 0 = square italic) -> `nibCut` radians. Out-of-range input clamps, so a
     *  hand-edited preset cannot ask for a nib turned past the range the slider offers. */
    fun degreesToNibCut(degrees: Double): Double = Math.toRadians(
        degrees.coerceIn(NIB_CUT_RANGE_DEGREES.start, NIB_CUT_RANGE_DEGREES.endInclusive),
    )

    fun nibCutToDegrees(radians: Double): Double = Math.toDegrees(radians)

    /** SENSITIVITY (0..100, higher = thinner light strokes) -> `m` in [0.1, 1.0]. */
    fun sensitivityToMinFactor(sensitivity: Double): Double =
        1.0 - (sensitivity.coerceIn(0.0, 100.0) / 100.0) * 0.9

    fun minFactorToSensitivity(m: Double): Double = (1.0 - m) / 0.9 * 100.0

    /** MULTIPLIER (thick:thin ratio) -> `ds`, clamped to [0, 0.95]. */
    fun multiplierToDirectionStrength(multiplier: Double): Double =
        ((multiplier - 1.0) / (multiplier + 1.0)).coerceIn(0.0, 0.95)

    fun directionStrengthToMultiplier(ds: Double): Double = (1.0 + ds) / (1.0 - ds)

    /** SPEED (0..100, higher = stronger thinning at speed) -> `speedStrength` in [0, 0.92]. */
    fun speedToStrength(speed: Double): Double = speed.coerceIn(0.0, 100.0) / 100.0 * 0.92

    fun strengthToSpeed(s: Double): Double = (s / 0.92) * 100.0

    /** INTENSITY (0..100, higher = brighter/wider halo) -> `neonStrength` in [0, 1]. */
    fun intensityToNeonStrength(intensity: Double): Double = intensity.coerceIn(0.0, 100.0) / 100.0

    fun neonStrengthToIntensity(s: Double): Double = s * 100.0

    /** Highlighter OPACITY (10..100 percent opacity) -> alpha in [0.10, 1.0]. */
    fun intensityToHighlighterAlpha(intensity: Double): Double = intensity.coerceIn(10.0, 100.0) / 100.0

    fun highlighterAlphaToIntensity(a: Double): Double = (a * 100.0).coerceIn(10.0, 100.0)

    /** WIDTH slider range per tool (spec 04 §5): 4..80 for the highlighter, 1..120 for the eraser
     *  (its radius, default 24), else 1..50. */
    fun widthRange(tool: Tool): ClosedFloatingPointRange<Double> = when (tool) {
        Tool.HIGHLIGHTER -> 4.0..80.0
        Tool.ERASER -> 1.0..120.0
        else -> 1.0..50.0
    }
}
