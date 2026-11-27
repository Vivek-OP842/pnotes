package com.pnotes.ui

import android.content.Context
import android.content.SharedPreferences

object ToolbarPreferences {
    private const val PREFS_NAME = "pnotes_toolbar_prefs"
    private const val KEY_DOCK_EDGE = "dock_edge"
    private const val KEY_RAIL_EDGE = "rail_edge"
    private const val KEY_DOCK_RATIO = "dock_ratio"
    private const val KEY_RAIL_RATIO = "rail_ratio"
    private const val KEY_IS_COMBINED = "is_combined"
    private const val KEY_IS_FULL_LENGTH = "is_full_length"


    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDockEdge(context: Context): RailDockEdge {
        val name = prefs(context).getString(KEY_DOCK_EDGE, RailDockEdge.LEFT.name)
        return try {
            RailDockEdge.valueOf(name ?: RailDockEdge.LEFT.name)
        } catch (_: Exception) {
            RailDockEdge.LEFT
        }
    }

    fun setDockEdge(context: Context, edge: RailDockEdge) {
        prefs(context).edit().putString(KEY_DOCK_EDGE, edge.name).apply()
    }

    fun getRailEdge(context: Context): RailDockEdge {
        val name = prefs(context).getString(KEY_RAIL_EDGE, RailDockEdge.LEFT.name)
        return try {
            RailDockEdge.valueOf(name ?: RailDockEdge.LEFT.name)
        } catch (_: Exception) {
            RailDockEdge.LEFT
        }
    }

    fun setRailEdge(context: Context, edge: RailDockEdge) {
        prefs(context).edit().putString(KEY_RAIL_EDGE, edge.name).apply()
    }

    fun getDockRatio(context: Context): Float =
        prefs(context).getFloat(KEY_DOCK_RATIO, 0.5f)

    fun setDockRatio(context: Context, ratio: Float) {
        prefs(context).edit().putFloat(KEY_DOCK_RATIO, ratio.coerceIn(0f, 1f)).apply()
    }

    fun getRailRatio(context: Context): Float =
        prefs(context).getFloat(KEY_RAIL_RATIO, 0.25f)

    fun setRailRatio(context: Context, ratio: Float) {
        prefs(context).edit().putFloat(KEY_RAIL_RATIO, ratio.coerceIn(0f, 1f)).apply()
    }

    fun getIsCombined(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_COMBINED, true)

    fun setIsCombined(context: Context, combined: Boolean) {
        prefs(context).edit().putBoolean(KEY_IS_COMBINED, combined).apply()
    }

    fun getIsFullLength(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_FULL_LENGTH, false)

    fun setIsFullLength(context: Context, fullLength: Boolean) {
        prefs(context).edit().putBoolean(KEY_IS_FULL_LENGTH, fullLength).apply()
    }
}
