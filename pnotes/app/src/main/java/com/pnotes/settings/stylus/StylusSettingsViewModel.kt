package com.pnotes.settings.stylus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pnotes.core.inking.PenProfile
import com.pnotes.core.inking.PenType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * ViewModel governing real-time stylus calibration, pressure meter updates, and scratchpad configuration.
 */
class StylusSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StylusSettingsRepository.getInstance(application)

    val settings: StateFlow<StylusSettings> = repository.settings

    // Live digitizer telemetry streams
    private val _liveRawPressure = MutableStateFlow(0f)
    val liveRawPressure: StateFlow<Float> = _liveRawPressure.asStateFlow()

    private val _liveCalibratedPressure = MutableStateFlow(0f)
    val liveCalibratedPressure: StateFlow<Float> = _liveCalibratedPressure.asStateFlow()

    private val _liveVelocity = MutableStateFlow(0f)
    val liveVelocity: StateFlow<Float> = _liveVelocity.asStateFlow()

    private val _liveStrokeWidth = MutableStateFlow(0f)
    val liveStrokeWidth: StateFlow<Float> = _liveStrokeWidth.asStateFlow()

    // Scratchpad clear signal counter
    private val _scratchpadClearKey = MutableStateFlow(0)
    val scratchpadClearKey: StateFlow<Int> = _scratchpadClearKey.asStateFlow()

    fun setPressureGamma(gamma: Float) {
        val clamped = gamma.coerceIn(0.5f, 2.5f)
        repository.updateSettings { it.copy(pressureGamma = clamped) }
    }

    fun setStylusOnlyMode(enabled: Boolean) {
        repository.updateSettings { it.copy(stylusOnlyMode = enabled) }
    }

    fun setVelocityTapering(enabled: Boolean) {
        repository.updateSettings { it.copy(velocityTaperingEnabled = enabled) }
    }

    fun setStrokeStabilization(factor: Float) {
        val clamped = factor.coerceIn(0.0f, 1.0f)
        repository.updateSettings { it.copy(strokeStabilization = clamped) }
    }

    fun setPenType(type: PenType) {
        val defaultWidth = type.defaultBaseWidth
        repository.updateSettings { it.copy(selectedPenType = type, baseStrokeWidth = defaultWidth) }
    }

    fun setBaseStrokeWidth(width: Float) {
        val clamped = width.coerceIn(1.0f, 40.0f)
        repository.updateSettings { it.copy(baseStrokeWidth = clamped) }
    }

    fun setStrokeColor(colorHex: Long) {
        repository.updateSettings { it.copy(strokeColorHex = colorHex) }
    }

    fun updateLiveTelemetry(rawPressure: Float, calibratedPressure: Float, velocity: Float = 0f, strokeWidth: Float = 0f) {
        _liveRawPressure.value = rawPressure
        _liveCalibratedPressure.value = calibratedPressure
        _liveVelocity.value = velocity
        _liveStrokeWidth.value = strokeWidth
    }

    fun clearScratchpad() {
        _scratchpadClearKey.value += 1
    }

    fun resetToDefaults() {
        repository.resetToDefaults()
    }
}
