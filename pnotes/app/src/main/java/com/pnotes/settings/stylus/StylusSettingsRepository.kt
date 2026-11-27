package com.pnotes.settings.stylus

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Reactive data repository for persistent stylus calibration and pressure settings.
 */
class StylusSettingsRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stylus_settings_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<StylusSettings> = _settings.asStateFlow()

    private fun loadSettings(): StylusSettings {
        val jsonStr = prefs.getString(KEY_STYLUS_SETTINGS, null) ?: return StylusSettings.DEFAULT
        return runCatching {
            StylusSettings.fromJson(JSONObject(jsonStr))
        }.getOrDefault(StylusSettings.DEFAULT)
    }

    /**
     * Updates and persists the stylus settings asynchronously.
     */
    fun updateSettings(transform: (StylusSettings) -> StylusSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        scope.launch {
            prefs.edit().putString(KEY_STYLUS_SETTINGS, updated.toJson().toString()).apply()
        }
    }

    /**
     * Resets stylus settings to factory defaults.
     */
    fun resetToDefaults() {
        updateSettings { StylusSettings.DEFAULT }
    }

    companion object {
        private const val KEY_STYLUS_SETTINGS = "key_stylus_settings_json"

        @Volatile
        private var instance: StylusSettingsRepository? = null

        fun getInstance(context: Context): StylusSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: StylusSettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
