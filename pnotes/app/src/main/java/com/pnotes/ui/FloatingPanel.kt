package com.pnotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import com.pnotes.ui.theme.LocalMotion
import com.pnotes.ui.theme.MotionSpecs

/**
 * Which edge of the screen a panel is standing on, so it grows out of that edge rather than out of
 * its own middle. A panel anchored above the dock grows upwards; one below the toolbar grows down.
 */
enum class PanelAnchor { TOP, BOTTOM, START, END, CENTER }

/**
 * The one way a floating panel arrives and leaves.
 *
 * The editor's panels used to be plain `if (open) { Card() }` branches: they appeared on the next
 * frame at full size, and a dismissal removed them in the same frame as the tap that caused it.
 * Neither reads as a transition, and the second one hides where the panel went. Wrapping every
 * panel in this composable gives them one shared contract — grow out of the anchor, fade, and on
 * the way out keep the panel composed until it has actually gone.
 *
 * Visibility is a parameter, not a `MutableTransitionState`, so callers keep expressing panels as
 * ordinary state (`showEraserSettings`) and the exit animation happens for free.
 */
@Composable
fun FloatingPanel(
    visible: Boolean,
    anchor: PanelAnchor = PanelAnchor.TOP,
    transformOrigin: TransformOrigin? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val motion = LocalMotion.current
    val origin = transformOrigin ?: anchor.origin()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enterTransition(motion, origin),
        exit = exitTransition(motion, origin),
        content = { content() },
    )
}

/**
 * The entering half of [FloatingPanel], for a surface whose exit is owned by someone else: a popup
 * window the platform tears down the instant it is dismissed, or a menu the caller closes by
 * clearing its `expanded` flag. There is nothing to animate out, so this animates in only.
 */
@Composable
fun PanelEnter(
    anchor: PanelAnchor = PanelAnchor.TOP,
    transformOrigin: TransformOrigin? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val motion = LocalMotion.current
    val origin = transformOrigin ?: anchor.origin()
    // Starts hidden so the very first composition animates; it never flips back, because the
    // window closing is what ends this content's life.
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = enterTransition(motion, origin),
        exit = ExitTransition.None,
        content = { content() },
    )
}

/**
 * The most recent non-null [value].
 *
 * A panel opened by setting a payload (`activePopupTool = Tool.ERASER`) and closed by clearing it
 * has nothing left to draw while it animates out. Holding the last payload keeps the panel on
 * screen for its exit, and keeps an open panel showing the current tool rather than the previous
 * one for a frame. The write lands during composition on purpose: it converges in that same pass,
 * because the next pass reads an equal value and writes nothing.
 */
@Composable
fun <T : Any> rememberLastPresent(value: T?): T? {
    var last by remember { mutableStateOf<T?>(null) }
    if (value != null && value != last) last = value
    return last ?: value
}

private fun PanelAnchor.origin(): TransformOrigin = when (this) {
    PanelAnchor.TOP -> TransformOrigin(0.5f, 0f)
    PanelAnchor.BOTTOM -> TransformOrigin(0.5f, 1f)
    PanelAnchor.START -> TransformOrigin(0f, 0.5f)
    PanelAnchor.END -> TransformOrigin(1f, 0.5f)
    PanelAnchor.CENTER -> TransformOrigin.Center
}

private fun enterTransition(motion: MotionSpecs, origin: TransformOrigin): EnterTransition =
    fadeIn(animationSpec = motion.enter()) +
        scaleIn(
            initialScale = motion.enterScale,
            transformOrigin = origin,
            animationSpec = motion.enter(),
        )

private fun exitTransition(motion: MotionSpecs, origin: TransformOrigin): ExitTransition =
    fadeOut(animationSpec = motion.exit()) +
        scaleOut(
            targetScale = motion.exitScale,
            transformOrigin = origin,
            animationSpec = motion.exit(),
        )
