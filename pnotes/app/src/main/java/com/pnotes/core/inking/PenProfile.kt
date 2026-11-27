package com.pnotes.core.inking

import androidx.compose.ui.graphics.BlendMode

/**
 * Enumeration of supported drawing instruments with distinct physical inking characteristics.
 */
enum class PenType(
    val displayName: String,
    val description: String,
    val defaultBaseWidth: Float,
) {
    BALLPOINT(
        displayName = "Ballpoint",
        description = "Uniform, reliable stroke with subtle natural pressure modulation.",
        defaultBaseWidth = 2.0f
    ),
    FOUNTAIN(
        displayName = "Fountain Pen",
        description = "Responsive dynamic tapering, expressive downstrokes and fine flourishes.",
        defaultBaseWidth = 2.0f
    ),
    BRUSH(
        displayName = "Calligraphy / Brush",
        description = "Wide dynamic range spanning from hairline flourishes to broad sweeps.",
        defaultBaseWidth = 4.0f
    ),
    PENCIL(
        displayName = "Pencil",
        description = "Graphite texture simulation with pressure-to-opacity alpha modulation.",
        defaultBaseWidth = 2.0f
    ),
    HIGHLIGHTER(
        displayName = "Highlighter",
        description = "Fixed width semi-transparent ink that multiplies with existing text.",
        defaultBaseWidth = 25.0f
    );
}

/**
 * Defines the mathematical and visual response configuration for a pen profile.
 *
 * @property penType The associated [PenType].
 * @property minWidthFactor Minimum stroke width multiplier relative to base width.
 * @property maxWidthFactor Maximum stroke width multiplier relative to base width.
 * @property gamma Non-linear response curve exponent ($f(p) = p^\gamma$).
 * @property velocityDamping Damping factor applied when drawing at high speeds.
 * @property velocityThreshold Speed threshold in px/ms for maximum velocity damping.
 * @property isPressureEnabled Whether stylus pressure alters stroke geometry.
 * @property isAlphaModulated Whether stylus pressure modulates stroke opacity (e.g. pencil).
 * @property minAlpha Minimum alpha opacity when [isAlphaModulated] is true.
 * @property maxAlpha Maximum alpha opacity when [isAlphaModulated] is true.
 * @property blendMode Composition blend mode (e.g. [BlendMode.Multiply] for highlighters).
 */
data class PenProfile(
    val penType: PenType,
    val minWidthFactor: Float,
    val maxWidthFactor: Float,
    val gamma: Float,
    val velocityDamping: Float,
    val velocityThreshold: Float = 2.5f,
    val isPressureEnabled: Boolean = true,
    val isAlphaModulated: Boolean = false,
    val minAlpha: Float = 0.25f,
    val maxAlpha: Float = 0.95f,
    val blendMode: BlendMode = BlendMode.SrcOver,
) {
    companion object {
        /** Ballpoint: 0.80x to 1.80x, Gamma 0.95, subtle velocity damping 0.05. */
        val BALLPOINT = PenProfile(
            penType = PenType.BALLPOINT,
            minWidthFactor = 0.80f,
            maxWidthFactor = 1.80f,
            gamma = 0.95f,
            velocityDamping = 0.05f,
            velocityThreshold = 3.0f,
            isPressureEnabled = true,
            isAlphaModulated = false,
        )

        /** Fountain: 0.35x to 2.80x, Gamma 1.0, velocity damping 0.20. */
        val FOUNTAIN = PenProfile(
            penType = PenType.FOUNTAIN,
            minWidthFactor = 0.35f,
            maxWidthFactor = 2.80f,
            gamma = 1.0f,
            velocityDamping = 0.20f,
            velocityThreshold = 2.5f,
            isPressureEnabled = true,
            isAlphaModulated = false,
        )

        /** Calligraphy / Brush: 0.15x to 3.60x, Gamma 1.1, velocity damping 0.30. */
        val BRUSH = PenProfile(
            penType = PenType.BRUSH,
            minWidthFactor = 0.15f,
            maxWidthFactor = 3.60f,
            gamma = 1.1f,
            velocityDamping = 0.30f,
            velocityThreshold = 2.0f,
            isPressureEnabled = true,
            isAlphaModulated = false,
        )

        /** Pencil: 0.60x to 1.80x, Gamma 0.95, pressure maps to opacity (0.30 to 0.95). */
        val PENCIL = PenProfile(
            penType = PenType.PENCIL,
            minWidthFactor = 0.60f,
            maxWidthFactor = 1.80f,
            gamma = 0.95f,
            velocityDamping = 0.10f,
            velocityThreshold = 3.0f,
            isPressureEnabled = true,
            isAlphaModulated = true,
            minAlpha = 0.30f,
            maxAlpha = 0.95f,
        )

        /** Highlighter: Fixed width 1.0x to 1.0x, BlendMode.Multiply, pressure inactive. */
        val HIGHLIGHTER = PenProfile(
            penType = PenType.HIGHLIGHTER,
            minWidthFactor = 1.0f,
            maxWidthFactor = 1.0f,
            gamma = 1.0f,
            velocityDamping = 0.0f,
            velocityThreshold = 1.0f,
            isPressureEnabled = false,
            isAlphaModulated = false,
            blendMode = BlendMode.Multiply,
        )

        /**
         * Resolves the default [PenProfile] for the given [PenType].
         */
        fun forType(type: PenType): PenProfile = when (type) {
            PenType.BALLPOINT -> BALLPOINT
            PenType.FOUNTAIN -> FOUNTAIN
            PenType.BRUSH -> BRUSH
            PenType.PENCIL -> PENCIL
            PenType.HIGHLIGHTER -> HIGHLIGHTER
        }
    }
}
