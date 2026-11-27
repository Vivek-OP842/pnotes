package com.pnotes.ui.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pnotes.ui.prototype.ProtoTextStyle
import com.pnotes.ui.theme.LocalReducedMotion

/**
 * Interaction primitives for the prototype-derived screens.
 *
 * The prototype is a static Figma artboard: it specifies no motion at all, so behaviour comes from
 * Apple's fluid-interface rules — feedback on pointer-down, springs rather than fixed-duration
 * curves, and a gentler equivalent whenever the user has asked the system for reduced motion.
 */

/**
 * A press carries no momentum, so its spring is critically damped (damping 1.0): it acknowledges
 * instantly and settles without overshoot. Bounce is reserved for gestures that were thrown.
 */
@Composable
private fun pressSpring(reduceMotion: Boolean) =
    if (reduceMotion) {
        snap<Float>()
    } else {
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
    }

/**
 * An interactive region expressed in the prototype's own design pixels, laid directly over the
 * artwork that [com.pnotes.ui.prototype.ProtoFrame] already drew.
 *
 * `indication = null` on purpose: the frame supplies the real chrome, and a Material ripple would
 * repaint it. Feedback is a light lift plus a press-in instead, applied on pointer-down so the
 * control answers the moment it is touched rather than when it is released.
 */
@Composable
fun ProtoHit(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Float = 12f,
    enabled: Boolean = true,
    dimmed: Boolean = false,
    haptic: Boolean = true,
    pressTint: Color = Color.White,
    pressAlpha: Float = 0.10f,
    reduceMotion: Boolean = LocalReducedMotion.current,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val press by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = pressSpring(reduceMotion),
        label = "protoPress",
    )

    Box(
        modifier = modifier
            .offset(x = x.dp, y = y.dp)
            .size(width.dp, height.dp)
            .graphicsLayer {
                val s = 1f - 0.006f * press
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(pressTint.copy(alpha = pressAlpha * press))
            .then(
                if (dimmed) Modifier.background(Color.Black.copy(alpha = 0.45f)) else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
            ) {
                // Causality and harmony: the haptic belongs to the commit, and fires on the same
                // frame as the state change rather than after it.
                if (haptic) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    )
}

/**
 * A real text input sitting on the field chrome the prototype already drew.
 *
 * No placeholder is rendered: the design already labels both fields directly above, and repeating
 * that label inside the box would be noise rather than guidance.
 */
@Composable
fun ProtoField(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    leadingInset: Float = 40f,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    Box(
        modifier = modifier.offset(x = x.dp, y = y.dp).size(width.dp, height.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = ProtoTextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = textColor,
                lineHeight = 16.94.sp,
            ),
            cursorBrush = SolidColor(textColor),
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onNext = { onImeAction() },
            ),
            modifier = Modifier.fillMaxSize().padding(start = leadingInset.dp, end = 16.dp),
        )
    }
}
