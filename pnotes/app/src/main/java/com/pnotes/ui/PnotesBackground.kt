package com.pnotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pnotes.ui.theme.toComposeColor

/**
 * Multi-layer atmospheric background.
 * Dark mode: charcoal base + GPU-efficient cached radial ambient glows (no per-pixel loops).
 * Light mode: pure clean white.
 *
 * Performance: all operations are GPU-accelerated drawRect/drawRect(brush) calls cached via
 * drawWithCache — zero per-frame allocations during animations or gestures.
 */
@Composable
fun PNotesBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PnotesBackground(modifier = modifier, content = content)
}

@Composable
fun PnotesBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = com.pnotes.ui.theme.LocalPalette.current
    val isDark = palette.isDark
    val accent = palette.accent.toComposeColor()
    val base = Color(0xFF2A2A2E)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                if (!isDark) {
                    onDrawBehind {
                        drawRect(color = Color.White)
                    }
                } else {
                    onDrawBehind {
                        drawRect(color = base)
                    }
                }
            },
    ) {
        content()
    }
}
