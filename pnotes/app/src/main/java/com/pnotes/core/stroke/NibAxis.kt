package com.pnotes.core.stroke

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The direction a calligraphy nib lays its broad face down in, read from the stylus's own tilt and
 * azimuth (`MotionEvent.AXIS_TILT` / `AXIS_ORIENTATION`).
 *
 * [StrokeEngine] measures the pen's direction term as the stroke direction's component along a fixed
 * axis — vertical, so a downstroke earns the broad face and an upstroke the thin one. That axis is a
 * stand-in for something the tangent cannot supply: which way the nib is actually turned. A stylus
 * reports it, so the axis can be the *pen's* rather than the page's, and the nib turns with the
 * writer instead of with the stroke.
 *
 * The second half of this pays off at pen-down. [StrokeEngine]'s head rule existed because the first
 * samples of a stroke could not be trusted to say which way it was going: it read a chord across the
 * first 8 px of travel, pinned every sample inside that window to it, and drew a live stroke thin
 * until the window filled — then rewrote it. Handed an axis, the first sample's direction is real, so
 * there is no chord, no pin and no rewrite.
 *
 * Nothing here is guessed. [of] returns null whenever there is no usable orientation — a finger, a
 * stylus that reports none, or one held so upright that its azimuth is only sensor noise — and every
 * caller then falls back to [DEFAULT_X]/[DEFAULT_Y], the vertical axis ink has always been measured
 * on. A pen held the way the reference below assumes therefore draws *exactly* what it drew before
 * tilt existed, and only a pen the writer turns away from that draws differently.
 */
object NibAxis {

    /** The broad axis of a nib with no stylus orientation to go on: straight down the page. That is
     *  the axis the direction term has always measured itself against (the tangent's y-component), so
     *  ink from a finger — or from any stylus that reports no tilt — is drawn exactly as it was. */
    const val DEFAULT_X = 0.0
    const val DEFAULT_Y = 1.0

    /** Tilt (`AXIS_TILT`, radians from the screen normal) below which the azimuth is not worth
     *  reading: the barrel's screen projection has no direction left in it and the sensor's noise
     *  dominates what little is there. About 6°. */
    const val MIN_TILT = 0.1

    /**
     * Which way the platform's azimuth turns the nib. +1 assumes the platform reports 0 for a stylus
     * lying toward the bottom of the screen — the natural hold, the barrel pointing back at the
     * writer — which puts the broad face on downstrokes and so agrees with [DEFAULT_Y]. A device that
     * measures the other way (or a writer who holds the pen mirrored) wants this flipped: it is the
     * one constant here that a device has to confirm.
     */
    const val AZIMUTH_SIGN = 1.0

    /** The nib's cut, in radians: how far the broad face is turned from the direction the barrel
     *  leans, which is a property of how the nib is ground. 0 squares it to the lean — a straight
     *  italic. A negative cut turns the face clockwise (an oblique), which is what a right-handed
     *  writer wants from a nib held the way the reference below assumes. */
    const val CUT = 0.0

    /** Largest cut worth reading: a half turn of the face, past which the axis is the same axis
     *  again (`Axis(x, y)` and `Axis(-x, -y)` describe one nib), so anything further is clamped
     *  rather than allowed to fold the nib back on itself. */
    const val MAX_CUT = PI / 2.0

    /** A nib's broad axis in page space — the stroke direction that earns the full width. Unit
     *  length. Its perpendicular is the nib's edge. */
    data class Axis(val x: Double, val y: Double)

    /**
     * The broad axis of the nib on a stylus at [tilt] radians from the screen normal, whose barrel
     * leans toward azimuth [azimuth] radians (`AXIS_TILT` / `AXIS_ORIENTATION`), or null when there is
     * no usable orientation to read.
     *
     * The barrel's screen projection is the direction the pen leans, and the broad face lies along it
     * — a chisel earns its full width on the stroke that runs *across* its edge, which is the stroke
     * running along the lean. At azimuth 0 that is [DEFAULT_Y], straight down the page, which is where
     * the fixed vertical axis came from.
     *
     * [cut] is the one part of that the stylus cannot report: how the nib itself is ground, turned
     * away from the lean by [CUT] for a square-cut italic and further for an oblique. It rides the
     * caller's style ([com.pnotes.core.tools.ToolConfig.nibCut]) rather than a constant here, so a
     * preset can be an oblique without a code change. A non-finite cut reads as square, and one past
     * [MAX_CUT] is clamped.
     *
     * Only meaningful when a tilt is: with no orientation to read the nib falls back to
     * [DEFAULT_X]/[DEFAULT_Y], which is the page's axis, not the writer's, so there is nothing to
     * turn. A cut therefore changes nothing for a finger or a stylus that reports no tilt, which is
     * the honest answer — there is no second axis to rotate in that case.
     */
    fun of(tilt: Double, azimuth: Double, cut: Double = CUT): Axis? {
        if (!(tilt >= MIN_TILT)) return null // !(>=) so a NaN tilt falls back rather than propagating
        val ground = if (cut.isFinite()) cut.coerceIn(-MAX_CUT, MAX_CUT) else CUT
        val a = AZIMUTH_SIGN * azimuth + ground
        return Axis(sin(a), cos(a))
    }
}
