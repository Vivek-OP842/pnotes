package com.pnotes.core.inking

/**
 * Configuration for the NeoPen stroke pipeline — a 1:1 Kotlin port of the parameter set
 * exposed by Onyx's native `libneopen_jni.so` (Star Notes ink engine, namespace `neo3pen`).
 *
 * Pipeline architecture extracted from the native binary:
 *
 *   TouchEvent -> StrokePointProducer -> SmoothFilter(smoothLevel)
 *              -> SplineProducer -> SplineInterpolator / SplineDistanceInterpolator /
 *                 SplineCurvatureInterpolator -> StrokePointBlob -> NeoStrokeRenderer
 *
 * Native config symbols (exact names found in the .so):
 *   smoothLevel, penWidth, penMinWidth, pressureSensitivity, ignorePressure,
 *   velocitySensitivity, endVelocitySensitivity, velocityAmplifier,
 *   velocityIgnoreThreshold, velocityLowerBound, velocityUpperBound
 *
 * @property smoothLevel Anti-jitter stabilization in [0..1]. 0 = raw/snappy, 1 = heavy
 *   stabilization (Star Notes' default feel sits around 0.3–0.5).
 * @property penWidth Nominal maximum stroke width in px (native: penWidth).
 * @property penMinWidth Minimum stroke width in px at zero pressure (native: penMinWidth).
 * @property pressureSensitivity How strongly stylus pressure modulates width in [0..1]
 *   (native: pressureSensitivity). 0 = width ignores pressure.
 * @property ignorePressure When true, pressure input is ignored entirely and width is fixed
 *   (native: ignorePressure — used by marker/highlighter pens).
 * @property velocitySensitivity How strongly drawing speed thins the stroke in [0..1]
 *   (native: velocitySensitivity).
 * @property endVelocitySensitivity Extra taper applied to the stroke tail at pen-up
 *   (native: endVelocitySensitivity).
 * @property velocityAmplifier Boost factor for the pressure response on slow, deliberate
 *   downstrokes (native: velocityAmplifier — fountain-pen character).
 * @property velocityIgnoreThreshold Speed in px/ms below which velocity has no effect
 *   (native: velocityIgnoreThreshold).
 * @property velocityLowerBound Speed in px/ms where velocity thinning begins
 *   (native: velocityLowerBound).
 * @property velocityUpperBound Speed in px/ms where thinning reaches its maximum
 *   (native: velocityUpperBound).
 */
data class NeoPenConfig(
    val penType: PenType = PenType.FOUNTAIN,
    val smoothLevel: Float = 0.45f,
    val penWidth: Float = 2.0f,
    val penMinWidth: Float = 0.8f,
    val pressureSensitivity: Float = 0.8f,
    val ignorePressure: Boolean = false,
    val velocitySensitivity: Float = 0.6f,
    val endVelocitySensitivity: Float = 0.4f,
    val velocityAmplifier: Float = 1.15f,
    val velocityIgnoreThreshold: Float = 0.05f,
    val velocityLowerBound: Float = 0.05f,
    val velocityUpperBound: Float = 2.5f,
) {
    companion object {
        /**
         * Builds a NeoPenConfig from the app's stylus settings and active pen profile,
         * mapping the existing settings onto the exact native parameter names.
         */
        fun fromStylusSettings(
            profile: PenProfile,
            baseWidth: Float,
            stabilization: Float,
            velocityTaperingEnabled: Boolean,
            gamma: Float,
        ): NeoPenConfig {
            val isFixedWidth = profile.penType == PenType.HIGHLIGHTER
            return NeoPenConfig(
                penType = profile.penType,
                smoothLevel = stabilization.coerceIn(0.0f, 1.0f),
                penWidth = baseWidth * profile.maxWidthFactor,
                penMinWidth = baseWidth * profile.minWidthFactor,
                pressureSensitivity = if (profile.isPressureEnabled) 0.8f else 0.0f,
                ignorePressure = isFixedWidth || !profile.isPressureEnabled,
                velocitySensitivity = if (velocityTaperingEnabled && profile.velocityDamping > 0f) {
                    profile.velocityDamping.coerceIn(0.0f, 1.0f)
                } else 0.0f,
                endVelocitySensitivity = if (velocityTaperingEnabled) 0.4f else 0.0f,
                velocityAmplifier = if (profile.penType == PenType.FOUNTAIN) 1.15f else 1.0f,
                velocityIgnoreThreshold = 0.05f,
                velocityLowerBound = 0.05f,
                velocityUpperBound = maxOf(0.5f, profile.velocityThreshold),
            )
        }
    }
}
