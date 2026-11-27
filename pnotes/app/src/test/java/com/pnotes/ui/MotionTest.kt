package com.pnotes.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import com.pnotes.ui.theme.Motion
import com.pnotes.ui.theme.MotionSpecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the 20 Motion Principles encoded in [Motion] and [MotionSpecs].
 * Asserted as pure mathematical and spec guarantees.
 */
class MotionTest {

    @Test
    fun `point 1 - entrance animations always use ease-out easing`() {
        val enterSpec = MotionSpecs(reduced = false).enter<Float>()
        assertTrue(enterSpec is TweenSpec<*>)
        assertEquals(LinearOutSlowInEasing, (enterSpec as TweenSpec<*>).easing)
        assertEquals(LinearOutSlowInEasing, Motion.EASE_OUT)
    }

    @Test
    fun `point 2 - dropdowns are between 150ms and 250ms`() {
        assertTrue("Dropdown duration must be >= 150ms", Motion.DROPDOWN_MS >= 150)
        assertTrue("Dropdown duration must be <= 250ms", Motion.DROPDOWN_MS <= 250)
        assertEquals(200, Motion.DROPDOWN_MS)

        val dropdownSpec = MotionSpecs(reduced = false).dropdownEnter<Float>()
        assertEquals(200, (dropdownSpec as TweenSpec<*>).durationMillis)
        assertEquals(LinearOutSlowInEasing, dropdownSpec.easing)
    }

    @Test
    fun `point 3 - list rows stagger by 30ms`() {
        assertEquals(30, Motion.LIST_STAGGER_MS)
        assertEquals(0, Motion.listStaggerDelay(0))
        assertEquals(30, Motion.listStaggerDelay(1))
        assertEquals(60, Motion.listStaggerDelay(2))
        assertEquals(150, Motion.listStaggerDelay(5))
    }

    @Test
    fun `point 4 - button press scales down to 0_97`() {
        assertEquals(0.97f, Motion.BUTTON_PRESS_SCALE, 0.0001f)
        assertEquals(0.97f, Motion.buttonScale(isPressed = true), 0.0001f)
        assertEquals(1.0f, Motion.buttonScale(isPressed = false), 0.0001f)
    }

    @Test
    fun `point 5 - moving knobs stretch by 12 percent along motion axis`() {
        assertEquals(1.12f, Motion.KNOB_STRETCH_FACTOR, 0.0001f)
        assertEquals(1.12f, Motion.knobStretch(isMoving = true), 0.0001f)
        assertEquals(1.0f, Motion.knobStretch(isMoving = false), 0.0001f)
    }

    @Test
    fun `point 6 - details settle with stagger`() {
        assertEquals(20, Motion.DETAIL_SETTLE_STAGGER_MS)
        assertEquals(0, Motion.detailSettleDelay(0))
        assertEquals(20, Motion.detailSettleDelay(1))
        assertEquals(40, Motion.detailSettleDelay(2))
    }

    @Test
    fun `point 7 - trajectory follows an arc curve rather than a rigid straight line`() {
        val startX = 0f
        val startY = 0f
        val endX = 100f
        val endY = 0f

        // At midpoint progress 0.5, a straight line would have Y = 0.
        // An arc must have perpendicular displacement (Y != 0).
        val (midX, midY) = Motion.arcTrajectory(0.5f, startX, startY, endX, endY, arcHeightFraction = 0.15f)
        assertEquals(50f, midX, 0.01f)
        assertTrue("Arc trajectory must displace perpendicularly at midpoint", kotlin.math.abs(midY) > 5f)

        // At start and end, trajectory hits endpoints exactly
        val (sX, sY) = Motion.arcTrajectory(0f, startX, startY, endX, endY)
        assertEquals(startX, sX, 0.001f)
        assertEquals(startY, sY, 0.001f)

        val (eX, eY) = Motion.arcTrajectory(1f, startX, startY, endX, endY)
        assertEquals(endX, eX, 0.001f)
        assertEquals(endY, eY, 0.001f)
    }

    @Test
    fun `point 8 - success confirmation uses pulse animation`() {
        assertEquals(1.08f, Motion.SUCCESS_PULSE_SCALE, 0.001f)
        assertEquals(240, Motion.SUCCESS_PULSE_MS)
        val pulse = MotionSpecs(reduced = false).successPulse<Float>()
        assertTrue(pulse is TweenSpec<*>)
        assertEquals(240, (pulse as TweenSpec<*>).durationMillis)
    }

    @Test
    fun `point 9 - on-screen movements use ease-in-out`() {
        val settleSpec = MotionSpecs(reduced = false).travelSettle<Float>()
        assertTrue(settleSpec is TweenSpec<*>)
        assertEquals(FastOutSlowInEasing, (settleSpec as TweenSpec<*>).easing)
        assertEquals(FastOutSlowInEasing, Motion.EASE_IN_OUT)
    }

    @Test
    fun `point 10 - popovers enter and exit at scale 0_95`() {
        assertEquals(0.95f, Motion.ENTER_SCALE, 0.0001f)
        assertEquals(0.95f, Motion.EXIT_SCALE, 0.0001f)
        assertEquals(0.95f, Motion.enterScale(reduced = false), 0.0001f)
        assertEquals(0.95f, Motion.exitScale(reduced = false), 0.0001f)
    }

    @Test
    fun `point 14 - small UI controls do not use bouncy springs`() {
        val effectSpec = MotionSpecs(reduced = false).effect<Float>()
        assertTrue(effectSpec is SpringSpec<*>)
        assertEquals(Spring.DampingRatioNoBouncy, (effectSpec as SpringSpec<*>).dampingRatio, 0.001f)

        val travelSpec = MotionSpecs(reduced = false).travel<Float>()
        assertTrue(travelSpec is SpringSpec<*>)
        assertEquals(Spring.DampingRatioNoBouncy, (travelSpec as SpringSpec<*>).dampingRatio, 0.001f)

        val pressSpec = MotionSpecs(reduced = false).press<Float>()
        assertTrue(pressSpec is SpringSpec<*>)
        assertEquals(Spring.DampingRatioNoBouncy, (pressSpec as SpringSpec<*>).dampingRatio, 0.001f)
    }

    @Test
    fun `point 16 - exits are strictly 20 percent shorter than entrances`() {
        val standardEnter = Motion.enterMillis(reduced = false)
        val standardExit = Motion.exitMillis(reduced = false)
        assertEquals(220, standardEnter)
        assertEquals(176, standardExit)
        val diff = (standardEnter - standardExit).toFloat() / standardEnter
        assertEquals(0.20f, diff, 0.01f)

        val reducedEnter = Motion.enterMillis(reduced = true)
        val reducedExit = Motion.exitMillis(reduced = true)
        assertEquals(140, reducedEnter)
        assertEquals(112, reducedExit)
        val diffReduced = (reducedEnter - reducedExit).toFloat() / reducedEnter
        assertEquals(0.20f, diffReduced, 0.01f)

        val dropdownEnter = Motion.dropdownEnterMillis(reduced = false)
        val dropdownExit = Motion.dropdownExitMillis(reduced = false)
        assertEquals(200, dropdownEnter)
        assertEquals(160, dropdownExit)
        val diffDropdown = (dropdownEnter - dropdownExit).toFloat() / dropdownEnter
        assertEquals(0.20f, diffDropdown, 0.01f)
    }

    @Test
    fun `point 20 - drag boundaries provide elastic rubber-band resistance without hard stop`() {
        val dimension = 1000f
        val d50 = Motion.rubberBandOffset(50f, dimension)
        val d100 = Motion.rubberBandOffset(100f, dimension)
        val d150 = Motion.rubberBandOffset(150f, dimension)

        val step1 = d50 - 0f
        val step2 = d100 - d50
        val step3 = d150 - d100

        // Sub-linear elastic damping: each equal step yields diminishing displacement
        assertTrue(d50 > 0f)
        assertTrue(step1 > step2)
        assertTrue(step2 > step3)
        assertTrue(d150 < 150f) // strictly compressed vs unconstrained delta
    }

    @Test
    fun `reduced motion keeps the fade and drops the scale`() {
        assertEquals(1f, Motion.enterScale(reduced = true), 0f)
        assertEquals(1f, Motion.exitScale(reduced = true), 0f)
        assertTrue(Motion.enterScale(reduced = false) < 1f)
        assertTrue(Motion.exitScale(reduced = false) < 1f)
    }

    @Test
    fun `reduced motion resolves movement specs to snaps`() {
        assertFalse(Motion.usesSpring(reduced = true))
        assertTrue(MotionSpecs(reduced = true).spatial<Float>() is SnapSpec<*>)
        assertTrue(MotionSpecs(reduced = true).press<Float>() is SnapSpec<*>)
        assertTrue(MotionSpecs(reduced = true).effect<Float>() is SnapSpec<*>)
    }

    @Test
    fun `the beats are ordered quick, standard, emphasized`() {
        assertTrue(Motion.QUICK_MS < Motion.STANDARD_MS)
        assertTrue(Motion.STANDARD_MS < Motion.EMPHASIZED_MS)
    }
}
