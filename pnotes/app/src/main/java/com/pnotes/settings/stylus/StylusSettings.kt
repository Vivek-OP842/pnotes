package com.pnotes.settings.stylus

import com.pnotes.core.inking.PenType
import org.json.JSONObject

/**
 * User-calibrated stylus and pressure sensitivity settings model.
 *
 * @property pressureGamma Power-law exponent for the non-linear transfer function ($y = x^\gamma$).
 *   $\gamma < 1.0$: Soft / Light touch curve (reaches maximum width with minimal force).
 *   $\gamma = 1.0$: Linear direct mapping.
 *   $\gamma > 1.0$: Firm / Deliberate press curve (preserves fine hairlines during gentle writing).
 * @property stylusOnlyMode Rejects finger and capacitive touch while inking (Hardware Palm Rejection).
 * @property velocityTaperingEnabled Dynamically thins strokes drawn at higher speeds.
 * @property strokeStabilization Anti-jitter smoothing factor between 0.0 (raw) and 1.0 (heavy smoothing).
 * @property selectedPenType Active testing pen instrument profile.
 * @property baseStrokeWidth Base nominal stroke width in pixels.
 * @property strokeColorHex Test stroke color ARGB hex.
 */
data class StylusSettings(
    val pressureGamma: Float = 1.45f,
    val stylusOnlyMode: Boolean = false,
    val velocityTaperingEnabled: Boolean = true,
    val strokeStabilization: Float = 0.45f,
    val selectedPenType: PenType = PenType.FOUNTAIN,
    val baseStrokeWidth: Float = 2.0f,
    val strokeColorHex: Long = 0xFFFFAA00,
    val minPressureThreshold: Float = 0.01f,
    val maxPressureThreshold: Float = 1.0f,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("pressureGamma", pressureGamma.toDouble())
        put("stylusOnlyMode", stylusOnlyMode)
        put("velocityTaperingEnabled", velocityTaperingEnabled)
        put("strokeStabilization", strokeStabilization.toDouble())
        put("selectedPenType", selectedPenType.name)
        put("baseStrokeWidth", baseStrokeWidth.toDouble())
        put("strokeColorHex", strokeColorHex)
        put("minPressureThreshold", minPressureThreshold.toDouble())
        put("maxPressureThreshold", maxPressureThreshold.toDouble())
    }

    companion object {
        val DEFAULT = StylusSettings()

        fun fromJson(json: JSONObject?): StylusSettings {
            if (json == null) return DEFAULT
            val penTypeName = json.optString("selectedPenType", PenType.FOUNTAIN.name)
            val penType = runCatching { PenType.valueOf(penTypeName) }.getOrDefault(PenType.FOUNTAIN)

            return StylusSettings(
                pressureGamma = json.optDouble("pressureGamma", 1.45).takeIf { it.isFinite() }?.toFloat()?.coerceIn(0.5f, 2.5f) ?: 1.45f,
                stylusOnlyMode = json.optBoolean("stylusOnlyMode", false),
                velocityTaperingEnabled = json.optBoolean("velocityTaperingEnabled", true),
                strokeStabilization = json.optDouble("strokeStabilization", 0.45).toFloat().coerceIn(0.0f, 1.0f),
                selectedPenType = penType,
                baseStrokeWidth = json.optDouble("baseStrokeWidth", 2.0).toFloat().coerceIn(1.0f, 40.0f),
                strokeColorHex = json.optLong("strokeColorHex", 0xFFFFAA00),
                minPressureThreshold = json.optDouble("minPressureThreshold", 0.01).toFloat(),
                maxPressureThreshold = json.optDouble("maxPressureThreshold", 1.0).toFloat(),
            )
        }
    }
}
