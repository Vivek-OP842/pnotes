package com.pnotes.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pnotes.R
import com.pnotes.ui.theme.LocalPalette

/**
 * Dynamic 3D Stationery Illustration switching seamlessly between Light and Dark mode.
 * Contains:
 * - Notebook + layered pages
 * - 3D Pencil
 * - Coral decorative leaves / botanical shapes
 * - Integrated soft coral glow
 * - 100% transparent background
 */
@Composable
fun StationeryDecoration(
    modifier: Modifier = Modifier,
) {
    val isDark = LocalPalette.current.isDark
    val illustrationRes = if (isDark) {
        R.drawable.stationery_illustration_dark
    } else {
        R.drawable.stationery_illustration_light
    }

    Image(
        painter = painterResource(id = illustrationRes),
        contentDescription = "Stationery illustration",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}


