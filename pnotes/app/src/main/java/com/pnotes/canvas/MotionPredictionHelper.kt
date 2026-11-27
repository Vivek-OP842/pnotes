package com.pnotes.canvas

import android.os.Build
import android.view.MotionEvent
import android.view.View

/**
 * Thin wrapper around `androidx.input.motionprediction.MotionEventPredictor` that predicts
 * where the stylus will be ~16 ms from now, eliminating perceived ink lag.
 *
 * **Stylus only** — finger touch is scrolling/gestures, not ink, so prediction is skipped.
 * **API 31+** — the predictor requires the view system's input pipeline introduced in Android 12;
 * on older devices [predict] returns null and the caller draws without prediction (no regression).
 *
 * Lifecycle: create once per drawing surface and hold it as long as the surface lives. Feed every
 * raw stylus [MotionEvent] through [record]; call [predict] in the ACTION_MOVE handler *after*
 * all real samples have been consumed, and draw the returned points as an ephemeral tail.
 */
class MotionPredictionHelper(view: View) {

    /**
     * A single predicted sample: viewport-space coordinates, pressure, and optional predicted timestamp.
     */
    data class PredictedPoint(val x: Double, val y: Double, val pressure: Double, val timeMs: Long = 0L)

    companion object {
        /** Minimum lookahead horizon in milliseconds. */
        const val MIN_LOOKAHEAD_MS = 10.0

        /** Maximum lookahead horizon in milliseconds (capped at 15 ms). */
        const val MAX_LOOKAHEAD_MS = 15.0
    }

    // The predictor is only available on API 31+.  On older devices the backing field stays null
    // and every public method is a no-op, so callers don't need their own version checks.
    private val predictor: androidx.input.motionprediction.MotionEventPredictor? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { androidx.input.motionprediction.MotionEventPredictor.newInstance(view) }.getOrNull()
        } else {
            null
        }

    private var lastRecordedEventTime: Long = 0L
    private var lastRecordedX: Double = 0.0
    private var lastRecordedY: Double = 0.0
    private var lastRecordedPressure: Double = 1.0

    /**
     * Feed a raw [MotionEvent] into the predictor's Kalman filter.
     * Call this for **every** stylus event (DOWN, MOVE, UP, CANCEL) **before** calling [predict].
     */
    fun record(event: MotionEvent) {
        lastRecordedEventTime = event.eventTime
        lastRecordedX = event.x.toDouble()
        lastRecordedY = event.y.toDouble()
        lastRecordedPressure = event.pressure.toDouble().coerceIn(0.0, 1.0)
        runCatching { predictor?.record(event) }
    }

    /**
     * Ask the predictor for where the stylus will be in the 10-15 ms window.
     * Uses AndroidX's Kalman filter motion predictor clamped/interpolated to the 10-15 ms horizon
     * for low-latency stylus tracking without overshooting.
     *
     * @param isStylusEvent true when the active pointer is a stylus (TOOL_TYPE_STYLUS).
     */
    fun predict(isStylusEvent: Boolean): List<PredictedPoint>? {
        if (!isStylusEvent) return null

        val pred = predictor ?: return null
        val predEvent = runCatching { pred.predict() }.getOrNull() ?: return null

        val points = mutableListOf<PredictedPoint>()
        try {
            val totalRawSamples = predEvent.historySize + 1
            var prevX = lastRecordedX
            var prevY = lastRecordedY
            var prevP = lastRecordedPressure
            var prevDt = 0.0

            for (i in 0 until totalRawSamples) {
                val isLast = (i == predEvent.historySize)
                val curTime = if (isLast) predEvent.eventTime else predEvent.getHistoricalEventTime(i)
                val curX = if (isLast) predEvent.x.toDouble() else predEvent.getHistoricalX(0, i).toDouble()
                val curY = if (isLast) predEvent.y.toDouble() else predEvent.getHistoricalY(0, i).toDouble()
                val curP = (if (isLast) predEvent.pressure.toDouble() else predEvent.getHistoricalPressure(0, i).toDouble()).coerceIn(0.0, 1.0)

                // Calculate lookahead delta in ms relative to the real event
                val dt = if (lastRecordedEventTime > 0L && curTime > lastRecordedEventTime) {
                    (curTime - lastRecordedEventTime).toDouble()
                } else {
                    // Fallback if timestamps are not offset: scale uniformly across the 10-15ms window
                    (i + 1) * (MAX_LOOKAHEAD_MS / totalRawSamples)
                }

                if (dt <= MAX_LOOKAHEAD_MS) {
                    points.add(PredictedPoint(curX, curY, curP, curTime))
                    prevX = curX
                    prevY = curY
                    prevP = curP
                    prevDt = dt
                } else {
                    // Capped at MAX_LOOKAHEAD_MS (15 ms): interpolate the boundary point at exactly 15 ms
                    if (prevDt < MAX_LOOKAHEAD_MS && dt > prevDt) {
                        val factor = ((MAX_LOOKAHEAD_MS - prevDt) / (dt - prevDt)).coerceIn(0.0, 1.0)
                        val interX = prevX + (curX - prevX) * factor
                        val interY = prevY + (curY - prevY) * factor
                        val interP = prevP + (curP - prevP) * factor
                        val interTime = (lastRecordedEventTime + MAX_LOOKAHEAD_MS).toLong()
                        points.add(PredictedPoint(interX, interY, interP, interTime))
                    }
                    break
                }
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { predEvent.recycle() }
        }

        return if (points.isEmpty()) null else points
    }

    /**
     * Reset the predictor state. Call on stroke end (ACTION_UP) or cancellation.
     * The library handles this internally via record(UP/CANCEL), but an explicit reset
     * after our own stroke bookkeeping ensures no stale state leaks across strokes.
     */
    fun reset() {
        lastRecordedEventTime = 0L
    }
}
