package com.pnotes.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * The app's one motion vocabulary embodying the 20 Motion Principles:
 *
 * 1. Entrances always use ease-out (LinearOutSlowInEasing).
 * 2. Dropdowns are between 150ms and 250ms (canonical 200ms).
 * 3. List rows stagger by 30ms.
 * 4. Button press scales down to 0.97.
 * 5. Moving knobs stretch by 12% along movement axis.
 * 6. Details settle with stagger.
 * 7. Object movement follows Arcs, not straight lines.
 * 8. Success confirmation uses a pulse animation.
 * 9. On-screen movements use ease-in-out (FastOutSlowInEasing).
 * 10. Popovers enter and exit at scale 0.95.
 * 11. Popover enters from trigger anchor, not center.
 * 12. Popover exits towards trigger anchor.
 * 13. Popover avoids center fade.
 * 14. No bouncy springs on small UI controls (critical damping: DampingRatioNoBouncy).
 * 15. Held/dragged objects track pointers directly without tween delays.
 * 16. Exits are strictly 20% shorter than entrances.
 * 17. Old state exit and new state entrance overlap seamlessly.
 * 18. Reveals and color changes utilize masks/clipping bounds.
 * 19. Icon changes crossfade with scale rather than hard replacement.
 * 20. Drag boundaries provide rubber-band elastic resistance without hard stops.
 */
object Motion {
    /** A press settling, an icon swapping: the shortest beat. */
    const val QUICK_MS = 140

    /** Dropdown transition duration (Point 2: between 150ms and 250ms). */
    const val DROPDOWN_MS = 200

    /** A surface changing place or size: a tool slot growing, a panel opening. */
    const val STANDARD_MS = 220

    /** A whole surface arriving: the editor over the backstage. */
    const val EMPHASIZED_MS = 320

    /** The scale a popover enters from (Point 10: scale 0.95). */
    const val ENTER_SCALE = 0.95f

    /** The scale a popover leaves towards (Point 10: scale 0.95). */
    const val EXIT_SCALE = 0.95f

    /** Button press scale factor (Point 4: scale 0.97). */
    const val BUTTON_PRESS_SCALE = 0.97f

    /** Velocity/motion stretch for moving knobs along travel axis (Point 5: 12% stretch). */
    const val KNOB_STRETCH_FACTOR = 1.12f

    /** Stagger delay between list items (Point 3: 30ms stagger). */
    const val LIST_STAGGER_MS = 30

    /** Stagger interval when secondary details settle into place (Point 6: 20ms stagger). */
    const val DETAIL_SETTLE_STAGGER_MS = 20

    /** Success confirmation pulse scale and duration (Point 8). */
    const val SUCCESS_PULSE_SCALE = 1.08f
    const val SUCCESS_PULSE_MS = 240

    /** Standard entrance easing curve (Point 1: Ease-out). */
    val EASE_OUT = LinearOutSlowInEasing

    /** Standard on-screen movement easing curve (Point 9: Ease-in-out). */
    val EASE_IN_OUT = FastOutSlowInEasing

    /** Enter duration. Reduced motion keeps the fade but shortens it to [QUICK_MS]. */
    fun enterMillis(reduced: Boolean): Int = if (reduced) QUICK_MS else STANDARD_MS

    /**
     * Exit duration: strictly 20% shorter than arriving whenever motion is allowed (Point 16).
     * e.g., 220ms -> 176ms; 140ms -> 112ms.
     */
    fun exitMillis(reduced: Boolean): Int = (enterMillis(reduced) * 0.80f).toInt()

    /** Dropdown enter duration (Point 2). */
    fun dropdownEnterMillis(reduced: Boolean): Int = if (reduced) QUICK_MS else DROPDOWN_MS

    /** Dropdown exit duration: 20% shorter than entrance (Point 2, Point 16). */
    fun dropdownExitMillis(reduced: Boolean): Int = (dropdownEnterMillis(reduced) * 0.80f).toInt()

    /** Scale a surface scales up from; 1 = a pure fade, which is what reduced motion wants. */
    fun enterScale(reduced: Boolean): Float = if (reduced) 1f else ENTER_SCALE

    /** Scale a surface scales down towards; 1 = a pure fade. */
    fun exitScale(reduced: Boolean): Float = if (reduced) 1f else EXIT_SCALE

    /** Button press scale helper (Point 4). */
    fun buttonScale(isPressed: Boolean): Float = if (isPressed) BUTTON_PRESS_SCALE else 1.0f

    /** Knob stretch helper along movement axis (Point 5: 12% stretch). */
    fun knobStretch(isMoving: Boolean): Float = if (isMoving) KNOB_STRETCH_FACTOR else 1.0f

    /** List stagger delay calculation (Point 3: 30ms stagger). */
    fun listStaggerDelay(index: Int): Int = index * LIST_STAGGER_MS

    /** Detail settle stagger delay calculation (Point 6). */
    fun detailSettleDelay(index: Int): Int = index * DETAIL_SETTLE_STAGGER_MS

    /**
     * Calculate curved trajectory with natural arc offset (Point 7).
     * Prevents stiff straight-line mechanical trajectories.
     */
    fun arcTrajectory(
        progress: Float,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        arcHeightFraction: Float = 0.15f,
    ): Pair<Float, Float> {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val linearX = startX + (endX - startX) * clampedProgress
        val linearY = startY + (endY - startY) * clampedProgress
        val dx = endX - startX
        val dy = endY - startY
        // Perpendicular offset creates an organic arc path
        val normalX = -dy * arcHeightFraction
        val normalY = dx * arcHeightFraction
        val arcFactor = kotlin.math.sin(clampedProgress * Math.PI.toFloat())
        return Pair(linearX + normalX * arcFactor, linearY + normalY * arcFactor)
    }

    /**
     * Elastic rubber-band resistance formula for drag boundaries (Point 20).
     * Ensures fluid deceleration rather than an abrupt brick-wall hard stop.
     */
    fun rubberBandOffset(delta: Float, dimension: Float, factor: Float = 0.55f): Float {
        if (dimension <= 0f) return 0f
        return (delta * dimension * factor) / (dimension + factor * kotlin.math.abs(delta))
    }

    /** Whether an overshooting spring is used at all. False under reduced motion. */
    fun usesSpring(reduced: Boolean): Boolean = !reduced
}

/**
 * The specs [Motion] resolves to, ready to hand to an animation. Read once per screen from
 * [LocalMotion] instead of threading a `reduced` flag through every call site.
 */
@Immutable
class MotionSpecs(val reduced: Boolean) {
    val enterScale: Float get() = Motion.enterScale(reduced)
    val exitScale: Float get() = Motion.exitScale(reduced)

    /** Position, size, offset: a natural physics spring (Point 9). */
    fun <T> spatial(): FiniteAnimationSpec<T> =
        if (reduced) {
            snap()
        } else {
            spring<T>(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow,
            )
        }

    /** Opacity, tint, elevation: critically damped (Point 14: no bouncy spring on small UI). */
    fun <T> effect(): FiniteAnimationSpec<T> =
        if (reduced) {
            snap()
        } else {
            spring<T>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        }

    /** A pressed control springs back into place with Apple tactile response (Point 4: 0.97 scale). */
    fun <T> press(): FiniteAnimationSpec<T> =
        if (reduced) {
            snap()
        } else {
            spring<T>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 2200f,
            )
        }

    /**
     * A control changing place: a selection travelling between slots.
     * Critically damped on purpose (Point 14: no bouncy spring on small UI).
     */
    fun <T> travel(): FiniteAnimationSpec<T> =
        if (reduced) {
            snap()
        } else {
            spring<T>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 1600f,
            )
        }

    /**
     * On-screen movements settling: ease-in-out (Point 9).
     */
    fun <T> travelSettle(): FiniteAnimationSpec<T> =
        if (reduced) {
            snap()
        } else {
            tween(durationMillis = Motion.STANDARD_MS, easing = FastOutSlowInEasing)
        }

    /**
     * One piece of content replacing another in place — a title, a glyph, a swatch.
     */
    fun <T> quick(): FiniteAnimationSpec<T> = tween(
        durationMillis = Motion.QUICK_MS,
        easing = FastOutSlowInEasing,
    )

    /**
     * A surface arriving: always ease-out (Point 1: LinearOutSlowInEasing).
     */
    fun <T> enter(): FiniteAnimationSpec<T> = tween(
        durationMillis = Motion.enterMillis(reduced),
        easing = LinearOutSlowInEasing,
    )

    /**
     * A surface leaving: strictly 20% shorter than entrance (Point 16).
     */
    fun <T> exit(): FiniteAnimationSpec<T> = tween(
        durationMillis = Motion.exitMillis(reduced),
        easing = FastOutLinearInEasing,
    )

    /**
     * Dropdown menu arriving: between 150ms and 250ms (Point 2), ease-out (Point 1).
     */
    fun <T> dropdownEnter(): FiniteAnimationSpec<T> = tween(
        durationMillis = Motion.dropdownEnterMillis(reduced),
        easing = LinearOutSlowInEasing,
    )

    /**
     * Dropdown menu leaving: 20% shorter than entrance (Point 2, Point 16).
     */
    fun <T> dropdownExit(): FiniteAnimationSpec<T> = tween(
        durationMillis = Motion.dropdownExitMillis(reduced),
        easing = FastOutLinearInEasing,
    )

    /**
     * Success confirmation pulse animation (Point 8).
     */
    fun <T> successPulse(): FiniteAnimationSpec<T> = tween(
        durationMillis = Motion.SUCCESS_PULSE_MS,
        easing = FastOutSlowInEasing,
    )
}

private val DefaultMotionSpecs = MotionSpecs(reduced = false)

/** The active motion specs. Provided once at the app root; defaults to full motion. */
val LocalMotion = staticCompositionLocalOf { DefaultMotionSpecs }

/**
 * The Android equivalent of `prefers-reduced-motion`, as a `Boolean`. Android exposes it as the
 * global animator duration scale; when the user sets it to zero, transitions become fades rather
 * than being fast-forwarded.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { animatorDurationScale(context) == 0f }
}

/** Read the reduced-motion setting and resolve it into specs, once per screen. */
@Composable
fun rememberMotionSpecs(): MotionSpecs {
    val reduced = rememberReducedMotion()
    return remember(reduced) { MotionSpecs(reduced) }
}

private fun animatorDurationScale(context: Context): Float = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
}.getOrDefault(1f)
