package com.pnotes.core.inking

/**
 * Represents a single high-frequency digitizer sample captured from an S-Pen,
 * active stylus, or touch surface.
 *
 * @property x Horizontal coordinate in content/canvas space.
 * @property y Vertical coordinate in content/canvas space.
 * @property pressure Normalized stylus pressure from 0.0f (no contact/hover) to 1.0f (maximum force).
 * @property tilt Stylus tilt angle relative to the screen surface in radians (0 = perpendicular).
 * @property orientation Stylus azimuth orientation angle in radians.
 * @property timestamp Monotonic or hardware event timestamp in milliseconds.
 */
data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val tilt: Float = 0f,
    val orientation: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Distance in pixels to another point. */
    fun distanceTo(other: InkPoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /** Midpoint between this point and [other]. */
    fun midpoint(other: InkPoint): InkPoint {
        return InkPoint(
            x = (x + other.x) * 0.5f,
            y = (y + other.y) * 0.5f,
            pressure = (pressure + other.pressure) * 0.5f,
            tilt = (tilt + other.tilt) * 0.5f,
            orientation = (orientation + other.orientation) * 0.5f,
            timestamp = (timestamp + other.timestamp) / 2
        )
    }
}
