package com.pnotes.ui.icons

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Complete 15-Tool Dynamic Icon System for PNotes Toolbar.
 * Automatically provides Dual-State rendering: Outline (Rest/Inactive) vs Colorful Luminous Glyph (Active).
 */
object PnotesDynamicIcons {

    private fun buildIcon(
        viewportWidth: Float = 26f,
        viewportHeight: Float = 26f,
        translationX: Float = 1f,
        translationY: Float = 1f,
        builderAction: ImageVector.Builder.() -> Unit
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = "dynamic_icon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        )
        builder.addGroup(
            name = "pad",
            rotate = 0f,
            pivotX = 0f,
            pivotY = 0f,
            scaleX = 1f,
            scaleY = 1f,
            translationX = translationX,
            translationY = translationY,
            clipPathData = emptyList(),
        )
        builder.builderAction()
        return builder.build()
    }

    private fun parseNodes(d: String) = PathParser().parsePathString(d).toNodes()

    // === 1. Regular Pen: INACTIVE ===
    val penInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 18.08,0.57 L 23.44,5.93 C 24.19,6.68 24.19,7.9 23.44,8.65 L 8.64,23.44 C 8.28,23.8 7.79,24.0 7.28,24.0 L 1.92,24.0 C 0.86,24.0 0.0,23.15 0.0,22.09 L 0.0,16.72 C 0.0,16.21 0.2,15.73 0.56,15.37 L 15.36,0.57 C 16.11,-0.18 17.33,-0.18 18.08,0.57 Z M 7.28,22.09 L 1.92,22.09 L 1.92,16.72 L 12.48,6.16 L 17.84,11.53 L 7.28,22.09 Z M 19.2,10.17 L 13.84,4.81 L 16.72,1.93 L 22.08,7.29 L 19.2,10.17 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 1. Regular Pen: ACTIVE ===
    val penActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 18.08,0.57 L 23.44,5.93 C 24.19,6.68 24.19,7.9 23.44,8.65 L 13.9,18.19 L 8.64,23.44 C 8.28,23.8 7.79,24.0 7.28,24.0 L 1.92,24.0 C 0.86,24.0 0.0,23.15 0.0,22.09 L 0.0,16.72 C 0.0,16.21 0.2,15.73 0.56,15.37 L 15.36,0.57 C 16.11,-0.18 17.33,-0.18 18.08,0.57 Z M 19.2,10.17 L 13.84,4.81 L 16.72,1.93 L 22.08,7.29 L 19.2,10.17 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1))),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 2. Fountain Pen: INACTIVE ===
    val fountainPenInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 23.48,8.16 C 23.81,8.49 24.0,8.94 24.0,9.41 C 24.0,9.88 23.82,10.34 23.48,10.67 L 20.32,13.84 L 17.9,20.28 C 17.68,20.87 17.16,21.31 16.53,21.41 L 1.04,23.99 C 0.99,24.0 0.94,24.0 0.89,24.0 C 0.63,24.0 0.38,23.89 0.21,23.69 C 0.04,23.49 -0.03,23.23 0.01,22.97 L 2.59,7.48 C 2.7,6.85 3.13,6.32 3.73,6.1 L 10.17,3.68 L 13.33,0.53 C 14.02,-0.16 15.16,-0.16 15.85,0.53 L 23.48,8.16 Z M 16.24,19.66 L 3.48,21.78 L 8.74,16.52 C 10.1,17.25 11.8,16.88 12.72,15.64 C 13.64,14.4 13.51,12.67 12.42,11.58 C 11.33,10.49 9.6,10.37 8.36,11.29 C 7.12,12.21 6.76,13.9 7.49,15.26 L 2.22,20.53 L 4.35,7.77 L 10.44,5.48 L 18.52,13.56 L 16.24,19.66 Z M 8.89,13.78 C 8.89,13.04 9.48,12.45 10.22,12.45 C 10.96,12.45 11.56,13.04 11.56,13.78 C 11.56,14.52 10.96,15.12 10.22,15.12 C 9.48,15.12 8.89,14.52 8.89,13.78 Z M 19.56,12.08 L 11.92,4.45 L 14.59,1.78 L 22.22,9.41 L 19.56,12.08 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 2. Fountain Pen: ACTIVE ===
    val fountainPenActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 15.75,0.53 L 23.47,8.25 L 23.47,8.25 C 24.17,8.95 24.17,10.1 23.47,10.8 L 20.28,13.99 L 17.83,20.52 C 17.6,21.12 17.07,21.55 16.44,21.66 L 2.43,24.0 C 2.24,24.03 2.05,23.93 1.96,23.76 C 1.87,23.59 1.9,23.38 2.04,23.24 L 8.45,16.83 C 9.52,17.34 10.79,17.09 11.59,16.22 C 12.39,15.35 12.53,14.06 11.94,13.04 C 11.35,12.02 10.15,11.5 9.0,11.77 C 7.85,12.04 7.01,13.02 6.92,14.2 C 6.89,14.67 6.98,15.14 7.18,15.56 L 0.77,21.97 C 0.63,22.11 0.41,22.14 0.24,22.05 C 0.07,21.96 -0.02,21.76 0.01,21.57 L 2.34,7.56 C 2.45,6.93 2.89,6.4 3.49,6.18 L 10.01,3.73 L 13.21,0.53 C 13.91,-0.17 15.05,-0.17 15.75,0.53 Z M 19.5,12.22 L 11.78,4.5 L 14.48,1.8 L 22.2,9.53 L 19.5,12.22 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFC084FC))),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 3. Brush: INACTIVE ===
    val brushInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 22.21,2.18 C 22.64,2.18 22.99,2.53 22.99,2.96 C 22.99,7.29 18.11,11.73 14.88,14.2 C 15.43,15.99 15.09,17.93 13.98,19.43 C 12.87,20.93 11.11,21.82 9.24,21.82 L 1.78,21.82 C 1.43,21.82 1.13,21.6 1.03,21.27 C 0.93,20.94 1.05,20.57 1.34,20.38 C 1.43,20.32 3.35,18.96 3.35,15.93 C 3.35,14.06 4.24,12.3 5.74,11.19 C 7.24,10.08 9.18,9.74 10.97,10.29 C 13.44,7.06 17.88,2.18 22.21,2.18 Z M 9.24,20.25 L 3.61,20.25 C 4.27,19.31 4.92,17.89 4.92,15.93 C 4.92,13.54 6.85,11.61 9.24,11.61 C 11.63,11.61 13.57,13.54 13.57,15.93 C 13.57,18.32 11.63,20.25 9.24,20.25 Z M 12.43,10.97 C 12.77,10.53 13.1,10.13 13.42,9.74 C 14.21,10.28 14.89,10.96 15.43,11.75 C 15.04,12.07 14.64,12.4 14.2,12.74 C 13.74,12.03 13.14,11.43 12.43,10.97 Z M 16.62,10.71 C 16.03,9.87 15.3,9.14 14.46,8.55 C 17.58,5.16 19.94,4.13 21.33,3.84 C 21.05,5.23 20.01,7.59 16.62,10.71 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 3. Brush: ACTIVE ===
    val brushActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 22.21,2.18 C 22.64,2.18 22.99,2.53 22.99,2.96 C 22.99,7.29 18.11,11.73 14.88,14.2 C 15.43,15.99 15.09,17.93 13.98,19.43 C 12.87,20.93 11.11,21.82 9.24,21.82 L 1.78,21.82 C 1.43,21.82 1.13,21.6 1.03,21.27 C 0.93,20.94 1.05,20.57 1.34,20.38 C 1.43,20.32 3.35,18.96 3.35,15.93 C 3.35,14.06 4.24,12.3 5.74,11.19 C 7.24,10.08 9.18,9.74 10.97,10.29 C 13.44,7.06 17.88,2.18 22.21,2.18 Z M 9.24,20.25 L 3.61,20.25 C 4.27,19.31 4.92,17.89 4.92,15.93 C 4.92,13.54 6.85,11.61 9.24,11.61 C 11.63,11.61 13.57,13.54 13.57,15.93 C 13.57,18.32 11.63,20.25 9.24,20.25 Z M 12.43,10.97 C 12.77,10.53 13.1,10.13 13.42,9.74 C 14.21,10.28 14.89,10.96 15.43,11.75 C 15.04,12.07 14.64,12.4 14.2,12.74 C 13.74,12.03 13.14,11.43 12.43,10.97 Z M 16.62,10.71 C 16.03,9.87 15.3,9.14 14.46,8.55 C 17.58,5.16 19.94,4.13 21.33,3.84 C 21.05,5.23 20.01,7.59 16.62,10.71 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFFDA4AF))),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 4. Highlighter: INACTIVE ===
    val highlighterInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 23.2,10.79 C 23.41,10.79 23.62,10.88 23.77,11.03 C 23.92,11.18 24.0,11.39 24.0,11.6 C 24.0,11.81 23.92,12.01 23.77,12.16 L 18.73,17.2 C 18.23,17.7 17.47,17.81 16.85,17.48 L 14.73,19.59 C 14.11,20.21 13.09,20.21 12.47,19.59 L 12.0,19.13 L 8.57,22.56 C 8.42,22.71 8.21,22.79 8.0,22.79 C 7.91,22.79 7.83,22.78 7.75,22.75 L 0.55,20.35 C 0.29,20.26 0.08,20.05 0.02,19.78 C -0.04,19.51 0.03,19.23 0.23,19.03 L 6.07,13.2 L 5.6,12.73 C 4.98,12.11 4.98,11.08 5.6,10.46 L 7.72,8.35 C 7.39,7.73 7.5,6.96 8.0,6.46 L 13.03,1.43 C 13.34,1.12 13.86,1.12 14.17,1.43 C 14.48,1.74 14.48,2.25 14.17,2.56 L 9.13,7.6 L 17.6,16.06 L 22.63,11.03 C 22.78,10.88 22.99,10.79 23.2,10.79 Z M 7.78,21.08 L 2.28,19.24 L 7.2,14.33 L 10.87,17.99 L 7.78,21.08 Z M 13.6,18.46 L 6.73,11.6 L 8.8,9.53 L 15.67,16.4 L 13.6,18.46 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 4. Highlighter: ACTIVE ===
    val highlighterActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 23.2,10.79 C 23.41,10.79 23.62,10.88 23.77,11.03 C 23.92,11.18 24.0,11.39 24.0,11.6 C 24.0,11.81 23.92,12.01 23.77,12.16 L 18.73,17.2 C 18.23,17.7 17.47,17.81 16.85,17.48 L 14.73,19.59 C 14.11,20.21 13.09,20.21 12.47,19.59 L 12.0,19.13 L 8.57,22.56 C 8.42,22.71 8.21,22.79 8.0,22.79 C 7.91,22.79 7.83,22.78 7.75,22.75 L 0.55,20.35 C 0.29,20.26 0.08,20.05 0.02,19.78 C -0.04,19.51 0.03,19.23 0.23,19.03 L 6.07,13.2 L 5.6,12.73 C 4.98,12.11 4.98,11.08 5.6,10.46 L 7.72,8.35 C 7.39,7.73 7.5,6.96 8.0,6.46 L 13.03,1.43 C 13.34,1.12 13.86,1.12 14.17,1.43 C 14.48,1.74 14.48,2.25 14.17,2.56 L 9.13,7.6 L 17.6,16.06 L 22.63,11.03 C 22.78,10.88 22.99,10.79 23.2,10.79 Z M 7.78,21.08 L 2.28,19.24 L 7.2,14.33 L 10.87,17.99 L 7.78,21.08 Z M 13.6,18.46 L 6.73,11.6 L 8.8,9.53 L 15.67,16.4 L 13.6,18.46 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFFFACC15), Color(0xFFEAB308))),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 5. Eraser: INACTIVE ===
    val eraserInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 18.41,0.88 L 23.19,6.06 C 24.27,7.23 24.27,9.12 23.19,10.29 L 12.38,22.0 L 22.15,22.0 C 22.66,22.0 23.07,22.45 23.07,23.0 C 23.07,23.55 22.66,24.0 22.15,24.0 L 4.93,24.0 C 4.68,24.0 4.45,23.89 4.28,23.71 L 0.81,19.95 C -0.27,18.78 -0.27,16.88 0.81,15.71 L 14.5,0.88 C 15.58,-0.29 17.33,-0.29 18.41,0.88 Z M 9.77,22.0 L 5.32,22.0 L 2.12,18.54 C 1.76,18.15 1.76,17.51 2.12,17.12 L 8.31,10.41 L 14.39,17.0 L 9.77,22.0 Z M 21.88,8.87 L 15.69,15.59 L 9.61,9.0 L 15.81,2.3 C 15.98,2.12 16.22,2.0 16.46,2.0 C 16.7,2.0 16.94,2.12 17.11,2.3 L 21.89,7.47 C 22.25,7.86 22.25,8.49 21.89,8.88 L 21.88,8.87 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 5. Eraser: ACTIVE ===
    val eraserActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 18.41,0.88 L 23.19,6.06 C 24.27,7.23 24.27,9.12 23.19,10.29 L 12.38,22.0 L 22.15,22.0 C 22.66,22.0 23.07,22.45 23.07,23.0 C 23.07,23.55 22.66,24.0 22.15,24.0 L 4.93,24.0 C 4.68,24.0 4.45,23.89 4.28,23.71 L 0.81,19.95 C -0.27,18.78 -0.27,16.88 0.81,15.71 L 14.5,0.88 C 15.58,-0.29 17.33,-0.29 18.41,0.88 Z M 21.88,8.87 L 15.69,15.59 L 9.61,9.0 L 15.81,2.3 C 15.98,2.12 16.22,2.0 16.46,2.0 C 16.7,2.0 16.94,2.12 17.11,2.3 L 21.89,7.47 C 22.25,7.86 22.25,8.49 21.89,8.88 L 21.88,8.87 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFA855F7))),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 6. Lasso: INACTIVE ===
    val lassoInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 15.0,1.0 C 15.0,1.55 14.55,2.0 14.0,2.0 L 10.0,2.0 C 9.45,2.0 9.0,1.55 9.0,1.0 C 9.0,0.45 9.45,0.0 10.0,0.0 L 14.0,0.0 C 14.55,0.0 15.0,0.45 15.0,1.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 10.0,22.0 L 14.0,22.0 C 14.55,22.0 15.0,22.45 15.0,23.0 C 15.0,23.55 14.55,24.0 14.0,24.0 L 10.0,24.0 C 9.45,24.0 9.0,23.55 9.0,23.0 C 9.0,22.45 9.45,22.0 10.0,22.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 19.0,0.0 L 22.0,0.0 C 23.11,0.0 24.0,0.89 24.0,2.0 L 24.0,5.0 C 24.0,5.55 23.55,6.0 23.0,6.0 C 22.45,6.0 22.0,5.55 22.0,5.0 L 22.0,2.0 L 19.0,2.0 C 18.45,2.0 18.0,1.55 18.0,1.0 C 18.0,0.45 18.45,0.0 19.0,0.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 22.0,10.0 C 22.0,9.45 22.45,9.0 23.0,9.0 C 23.55,9.0 24.0,9.45 24.0,10.0 L 24.0,14.0 C 24.0,14.55 23.55,15.0 23.0,15.0 C 22.45,15.0 22.0,14.55 22.0,14.0 L 22.0,10.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 22.0,19.0 C 22.0,18.45 22.45,18.0 23.0,18.0 C 23.55,18.0 24.0,18.45 24.0,19.0 L 24.0,22.0 C 24.0,23.11 23.11,24.0 22.0,24.0 L 19.0,24.0 C 18.45,24.0 18.0,23.55 18.0,23.0 C 18.0,22.45 18.45,22.0 19.0,22.0 L 22.0,22.0 L 22.0,19.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 2.0,14.0 C 2.0,14.55 1.55,15.0 1.0,15.0 C 0.45,15.0 0.0,14.55 0.0,14.0 L 0.0,10.0 C 0.0,9.45 0.45,9.0 1.0,9.0 C 1.55,9.0 2.0,9.45 2.0,10.0 L 2.0,14.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 2.0,22.0 L 5.0,22.0 C 5.55,22.0 6.0,22.45 6.0,23.0 C 6.0,23.55 5.55,24.0 5.0,24.0 L 2.0,24.0 C 0.89,24.0 0.0,23.11 0.0,22.0 L 0.0,19.0 C 0.0,18.45 0.45,18.0 1.0,18.0 C 1.55,18.0 2.0,18.45 2.0,19.0 L 2.0,22.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 2.0,0.0 L 5.0,0.0 C 5.55,0.0 6.0,0.45 6.0,1.0 C 6.0,1.55 5.55,2.0 5.0,2.0 L 2.0,2.0 L 2.0,5.0 C 2.0,5.55 1.55,6.0 1.0,6.0 C 0.45,6.0 0.0,5.55 0.0,5.0 L 0.0,2.0 C 0.0,0.89 0.89,0.0 2.0,0.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
    }

    // === 6. Lasso: ACTIVE ===
    val lassoActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 13.33,0.89 C 13.33,1.38 12.93,1.78 12.44,1.78 L 8.89,1.78 C 8.4,1.78 8.0,1.38 8.0,0.89 C 8.0,0.4 8.4,0.0 8.89,0.0 L 12.44,0.0 C 12.93,0.0 13.33,0.4 13.33,0.89 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399))),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 8.89,19.56 L 12.44,19.56 C 12.93,19.56 13.33,19.95 13.33,20.44 C 13.33,20.93 12.93,21.33 12.44,21.33 L 8.89,21.33 C 8.4,21.33 8.0,20.93 8.0,20.44 C 8.0,19.95 8.4,19.56 8.89,19.56 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399))),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 19.56,4.44 L 19.56,1.78 L 16.89,1.78 C 16.4,1.78 16.0,1.38 16.0,0.89 C 16.0,0.4 16.4,0.0 16.89,0.0 L 19.56,0.0 C 20.54,0.0 21.33,0.8 21.33,1.78 L 21.33,4.44 C 21.33,4.93 20.93,5.33 20.44,5.33 C 19.95,5.33 19.56,4.93 19.56,4.44 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399))),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 19.56,8.89 C 19.56,8.4 19.95,8.0 20.44,8.0 C 20.93,8.0 21.33,8.4 21.33,8.89 L 21.33,12.44 C 21.33,12.93 20.93,13.33 20.44,13.33 C 19.95,13.33 19.56,12.93 19.56,12.44 L 19.56,8.89 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399))),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 1.78,12.44 C 1.78,12.93 1.38,13.33 0.89,13.33 C 0.4,13.33 0.0,12.93 0.0,12.44 L 0.0,8.89 C 0.0,8.4 0.4,8.0 0.89,8.0 C 1.38,8.0 1.78,8.4 1.78,8.89 L 1.78,12.44 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399))),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 1.78,19.56 L 4.44,19.56 C 4.93,19.56 5.33,19.95 5.33,20.44 C 5.33,20.93 4.93,21.33 4.44,21.33 L 1.78,21.33 C 0.8,21.33 0.0,20.54 0.0,19.56 L 0.0,16.89 C 0.0,16.4 0.4,16.0 0.89,16.0 C 1.38,16.0 1.78,16.4 1.78,16.89 L 1.78,19.56 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399))),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 1.78,0.0 L 4.44,0.0 C 4.93,0.0 5.33,0.4 5.33,0.89 C 5.33,1.38 4.93,1.78 4.44,1.78 L 1.78,1.78 L 1.78,4.44 C 1.78,4.93 1.38,5.33 0.89,5.33 C 0.4,5.33 0.0,4.93 0.0,4.44 L 0.0,1.78 C 0.0,0.8 0.8,0.0 1.78,0.0 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399))),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 21.33,19.56 L 23.11,19.56 C 23.6,19.56 24.0,19.95 24.0,20.44 C 24.0,20.93 23.6,21.33 23.11,21.33 L 21.33,21.33 L 21.33,23.11 C 21.33,23.6 20.93,24.0 20.44,24.0 C 19.95,24.0 19.56,23.6 19.56,23.11 L 19.56,21.33 L 17.78,21.33 C 17.29,21.33 16.89,20.93 16.89,20.44 C 16.89,19.95 17.29,19.56 17.78,19.56 L 19.56,19.56 L 19.56,17.78 C 19.56,17.29 19.95,16.89 20.44,16.89 C 20.93,16.89 21.33,17.29 21.33,17.78 L 21.33,19.56 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399))),
            pathFillType = PathFillType.NonZero,
        )
    }

    // === 7. Magic Wand: INACTIVE ===
    val magicWandInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 1.77,4.0 C 1.77,3.45 2.17,3.0 2.66,3.0 L 4.44,3.0 L 4.44,1.0 C 4.44,0.45 4.84,0.0 5.33,0.0 C 5.82,0.0 6.22,0.45 6.22,1.0 L 6.22,3.0 L 8.0,3.0 C 8.49,3.0 8.88,3.45 8.88,4.0 C 8.88,4.55 8.49,5.0 8.0,5.0 L 6.22,5.0 L 6.22,7.01 C 6.22,7.56 5.82,8.0 5.33,8.0 C 4.84,8.0 4.44,7.56 4.44,7.01 L 4.44,5.0 L 2.66,5.0 C 2.17,5.0 1.77,4.55 1.77,4.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 16.01,20.0 L 16.89,20.0 C 17.38,20.0 17.78,20.45 17.78,21.0 C 17.78,21.55 17.38,22.0 16.89,22.0 L 16.01,22.0 L 16.01,23.0 C 16.01,23.55 15.61,24.0 15.12,24.0 C 14.63,24.0 14.23,23.55 14.23,23.0 L 14.23,22.0 L 13.34,22.0 C 12.85,22.0 12.45,21.55 12.45,21.0 C 12.45,20.45 12.85,20.0 13.34,20.0 L 14.23,20.0 L 14.23,19.0 C 14.23,18.45 14.63,17.99 15.12,17.99 C 15.61,17.99 16.01,18.45 16.01,19.0 L 16.01,20.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 21.34,14.0 L 23.12,14.0 C 23.61,14.0 24.0,14.45 24.0,15.0 C 24.0,15.55 23.61,16.0 23.12,16.0 L 21.34,16.0 L 21.34,17.99 C 21.34,18.54 20.94,19.0 20.45,19.0 C 19.96,19.0 19.56,18.54 19.56,17.99 L 19.56,16.0 L 17.78,16.0 C 17.29,16.0 16.89,15.55 16.89,15.0 C 16.89,14.45 17.29,14.0 17.78,14.0 L 19.56,14.0 L 19.56,12.0 C 19.56,11.45 19.96,11.0 20.45,11.0 C 20.94,11.0 21.34,11.45 21.34,12.0 L 21.34,14.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 20.82,6.01 L 5.33,23.42 C 4.64,24.2 3.51,24.2 2.82,23.42 L 0.52,20.83 C -0.17,20.05 -0.17,18.77 0.52,17.99 L 16.01,0.58 C 16.7,-0.2 17.83,-0.2 18.52,0.58 L 20.82,3.17 C 21.51,3.95 21.51,5.23 20.82,6.01 Z M 12.45,7.41 L 14.75,10.0 L 4.07,22.0 L 1.77,19.41 L 12.45,7.41 Z M 17.26,2.0 L 19.56,4.59 L 16.01,8.59 L 13.71,6.01 L 17.26,2.0 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 7. Magic Wand: ACTIVE ===
    val magicWandActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 24.0,15.0 C 24.0,15.55 23.61,16.0 23.12,16.0 L 21.34,16.0 L 21.34,17.99 C 21.34,18.54 20.94,19.0 20.45,19.0 C 19.96,19.0 19.56,18.54 19.56,17.99 L 19.56,16.0 L 17.78,16.0 C 17.29,16.0 16.89,15.55 16.89,15.0 C 16.89,14.45 17.29,14.0 17.78,14.0 L 19.56,14.0 L 19.56,12.0 C 19.56,11.45 19.96,11.0 20.45,11.0 C 20.94,11.0 21.34,11.45 21.34,12.0 L 21.34,14.0 L 23.12,14.0 C 23.61,14.0 24.0,14.45 24.0,15.0 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF8B5CF6))),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 4.44,5.0 L 2.66,5.0 C 2.17,5.0 1.77,4.55 1.77,4.0 C 1.77,3.45 2.17,3.0 2.66,3.0 L 4.44,3.0 L 4.44,1.0 C 4.44,0.45 4.84,0.0 5.33,0.0 C 5.82,0.0 6.22,0.45 6.22,1.0 L 6.22,3.0 L 8.0,3.0 C 8.49,3.0 8.88,3.45 8.88,4.0 C 8.88,4.55 8.49,5.0 8.0,5.0 L 6.22,5.0 L 6.22,7.01 C 6.22,7.56 5.82,8.0 5.33,8.0 C 4.84,8.0 4.44,7.56 4.44,7.01 L 4.44,5.0 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF8B5CF6))),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 16.01,20.0 L 16.89,20.0 C 17.38,20.0 17.78,20.45 17.78,21.0 C 17.78,21.55 17.38,22.0 16.89,22.0 L 16.01,22.0 L 16.01,23.0 C 16.01,23.55 15.61,24.0 15.12,24.0 C 14.63,24.0 14.23,23.55 14.23,23.0 L 14.23,22.0 L 13.34,22.0 C 12.85,22.0 12.45,21.55 12.45,21.0 C 12.45,20.45 12.85,20.0 13.34,20.0 L 14.23,20.0 L 14.23,19.0 C 14.23,18.45 14.63,17.99 15.12,17.99 C 15.61,17.99 16.01,18.45 16.01,19.0 L 16.01,20.0 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF8B5CF6))),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 20.82,6.01 L 5.33,23.42 C 4.64,24.2 3.51,24.2 2.82,23.42 L 0.52,20.83 C -0.17,20.05 -0.17,18.77 0.52,17.99 L 16.01,0.58 C 16.7,-0.2 17.83,-0.2 18.52,0.58 L 20.82,3.17 C 21.51,3.95 21.51,5.23 20.82,6.01 Z M 17.26,2.0 L 19.56,4.59 L 16.01,8.59 L 13.71,6.01 L 17.26,2.0 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF8B5CF6))),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 8. Shapes: INACTIVE (Clean Overlapping Outline Shapes) ===
    val shapesInactive: ImageVector = buildIcon {
        // Rounded Square Outline
        addPath(
            pathData = parseNodes("M 4.5,7.0 L 11.5,7.0 C 13.43,7.0 15.0,8.57 15.0,10.5 L 15.0,17.5 C 15.0,19.43 13.43,21.0 11.5,21.0 L 4.5,21.0 C 2.57,21.0 1.0,19.43 1.0,17.5 L 1.0,10.5 C 1.0,8.57 2.57,7.0 4.5,7.0 Z"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Overlapping Circle Outline
        addPath(
            pathData = parseNodes("M 9.0,8.0 A 7.0,7.0 0.0 1,0 23.0,8.0 A 7.0,7.0 0.0 1,0 9.0,8.0 Z"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    // === 8. Shapes: ACTIVE ===
    val shapesActive: ImageVector = buildIcon {
        // Rounded Square (Indigo/Purple Gradient Fill)
        addPath(
            pathData = parseNodes("M 4.5,7.0 L 11.5,7.0 C 13.43,7.0 15.0,8.57 15.0,10.5 L 15.0,17.5 C 15.0,19.43 13.43,21.0 11.5,21.0 L 4.5,21.0 C 2.57,21.0 1.0,19.43 1.0,17.5 L 1.0,10.5 C 1.0,8.57 2.57,7.0 4.5,7.0 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
        )
        // Overlapping Circle (Pink/Cyan Gradient Fill + soft white stroke)
        addPath(
            pathData = parseNodes("M 9.0,8.0 A 7.0,7.0 0.0 1,0 23.0,8.0 A 7.0,7.0 0.0 1,0 9.0,8.0 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF06B6D4))),
            stroke = SolidColor(Color.White.copy(alpha = 0.5f)),
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    // === 9. Image: INACTIVE ===
    val imageInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 1.85,0.0 L 22.15,0.0 C 23.17,0.0 24.0,0.97 24.0,2.18 L 24.0,21.82 C 24.0,23.03 23.17,24.0 22.15,24.0 L 1.85,24.0 C 0.83,24.0 0.0,23.03 0.0,21.82 L 0.0,2.18 C 0.0,0.97 0.83,0.0 1.85,0.0 Z M 22.15,2.18 L 22.15,16.2 L 19.15,12.64 C 18.43,11.79 17.25,11.79 16.53,12.64 L 14.23,15.37 L 9.15,9.36 C 8.43,8.51 7.26,8.51 6.54,9.36 L 1.85,14.92 L 1.85,2.18 L 22.15,2.18 Z M 1.85,18.01 L 7.85,10.91 L 17.08,21.82 L 1.85,21.82 L 1.85,18.01 Z M 22.15,21.82 L 19.69,21.82 L 15.53,16.91 L 17.84,14.19 L 22.15,19.28 L 22.15,21.82 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
        addPath(
            pathData = parseNodes("M 13.85,8.18 C 13.85,7.27 14.46,6.54 15.23,6.54 C 16.0,6.54 16.62,7.27 16.62,8.18 C 16.62,9.09 16.0,9.81 15.23,9.81 C 14.46,9.81 13.85,9.09 13.85,8.18 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
    }

    // === 9. Image: ACTIVE ===
    val imageActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 1.85,0.0 L 22.15,0.0 C 23.17,0.0 24.0,0.97 24.0,2.18 L 24.0,21.82 C 24.0,23.03 23.17,24.0 22.15,24.0 L 1.85,24.0 C 0.83,24.0 0.0,23.03 0.0,21.82 L 0.0,2.18 C 0.0,0.97 0.83,0.0 1.85,0.0 Z M 15.23,6.54 C 16.0,6.54 16.62,7.27 16.62,8.18 C 16.62,9.09 16.0,9.81 15.23,9.81 C 14.46,9.81 13.85,9.09 13.85,8.18 C 13.85,7.27 14.46,6.54 15.23,6.54 Z M 22.15,21.82 L 1.85,21.82 L 1.85,16.46 L 7.19,10.13 C 7.36,9.93 7.61,9.81 7.85,9.81 C 8.09,9.81 8.33,9.93 8.5,10.13 L 16.27,19.32 C 16.63,19.75 17.22,19.75 17.58,19.32 C 17.94,18.89 17.94,18.21 17.58,17.78 L 15.54,15.37 L 17.19,13.41 C 17.55,12.98 18.14,12.98 18.5,13.41 L 22.15,17.73 L 22.15,21.82 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFFB923C), Color(0xFFFBBF24))),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 10. Table: INACTIVE ===
    val tableInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 0.92,0.0 L 23.08,0.0 C 23.59,0.0 24.0,0.54 24.0,1.2 L 24.0,21.59 C 24.0,22.92 23.17,24.0 22.15,24.0 L 1.85,24.0 C 0.83,24.0 0.0,22.92 0.0,21.59 L 0.0,1.2 C 0.0,0.54 0.41,0.0 0.92,0.0 Z M 1.85,9.59 L 6.46,9.59 L 6.46,14.41 L 1.85,14.41 L 1.85,9.59 Z M 8.31,9.59 L 22.15,9.59 L 22.15,14.41 L 8.31,14.41 L 8.31,9.59 Z M 22.15,2.41 L 22.15,7.2 L 1.85,7.2 L 1.85,2.41 L 22.15,2.41 Z M 1.85,16.8 L 6.46,16.8 L 6.46,21.59 L 1.85,21.59 L 1.85,16.8 Z M 22.15,21.59 L 8.31,21.59 L 8.31,16.8 L 22.15,16.8 L 22.15,21.59 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 10. Table: ACTIVE ===
    val tableActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 0.92,0.0 L 23.08,0.0 C 23.59,0.0 24.0,0.54 24.0,1.2 L 24.0,21.59 C 24.0,22.92 23.17,24.0 22.15,24.0 L 1.85,24.0 C 0.83,24.0 0.0,22.92 0.0,21.59 L 0.0,1.2 C 0.0,0.54 0.41,0.0 0.92,0.0 Z M 1.85,9.59 L 6.46,9.59 L 6.46,14.41 L 1.85,14.41 L 1.85,9.59 Z M 8.31,9.59 L 22.15,9.59 L 22.15,14.41 L 8.31,14.41 L 8.31,9.59 Z M 22.15,2.41 L 22.15,7.2 L 1.85,7.2 L 1.85,2.41 L 22.15,2.41 Z M 1.85,16.8 L 6.46,16.8 L 6.46,21.59 L 1.85,21.59 L 1.85,16.8 Z M 22.15,21.59 L 8.31,21.59 L 8.31,16.8 L 22.15,16.8 L 22.15,21.59 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5), Color(0xFF06B6D4))),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 11. Text Tool: INACTIVE ===
    val textInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 24.0,1.2 L 24.0,6.0 C 24.0,6.66 23.46,7.2 22.8,7.2 C 22.14,7.2 21.6,6.66 21.6,6.0 L 21.6,2.4 L 13.2,2.4 L 13.2,21.6 L 16.8,21.6 C 17.46,21.6 18.0,22.14 18.0,22.8 C 18.0,23.46 17.46,24.0 16.8,24.0 L 7.2,24.0 C 6.54,24.0 6.0,23.46 6.0,22.8 C 6.0,22.14 6.54,21.6 7.2,21.6 L 10.8,21.6 L 10.8,2.4 L 2.4,2.4 L 2.4,6.0 C 2.4,6.66 1.86,7.2 1.2,7.2 C 0.54,7.2 0.0,6.66 0.0,6.0 L 0.0,1.2 C 0.0,0.54 0.54,0.0 1.2,0.0 L 22.8,0.0 C 23.46,0.0 24.0,0.54 24.0,1.2 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
    }

    // === 11. Text Tool: ACTIVE ===
    val textActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 24.0,1.2 L 24.0,6.0 C 24.0,6.66 23.46,7.2 22.8,7.2 C 22.14,7.2 21.6,6.66 21.6,6.0 L 21.6,2.4 L 13.2,2.4 L 13.2,21.6 L 16.8,21.6 C 17.46,21.6 18.0,22.14 18.0,22.8 C 18.0,23.46 17.46,24.0 16.8,24.0 L 7.2,24.0 C 6.54,24.0 6.0,23.46 6.0,22.8 C 6.0,22.14 6.54,21.6 7.2,21.6 L 10.8,21.6 L 10.8,2.4 L 2.4,2.4 L 2.4,6.0 C 2.4,6.66 1.86,7.2 1.2,7.2 C 0.54,7.2 0.0,6.66 0.0,6.0 L 0.0,1.2 C 0.0,0.54 0.54,0.0 1.2,0.0 L 22.8,0.0 C 23.46,0.0 24.0,0.54 24.0,1.2 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFF8B5CF6), Color(0xFF3B82F6))),
            pathFillType = PathFillType.NonZero,
        )
    }

    // === 12. Camera: INACTIVE ===
    val cameraInactive: ImageVector = buildIcon {
        // Top flash mount (stroke)
        addPath(
            pathData = parseNodes("M 9.6,1.2 L 14.4,1.2 C 15.72,1.2 16.8,2.28 16.8,3.6 L 16.8,5.4 L 7.2,5.4 L 7.2,3.6 C 7.2,2.28 8.28,1.2 9.6,1.2 Z"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Camera body (stroke)
        addPath(
            pathData = parseNodes("M 4.8,4.8 L 19.2,4.8 C 21.84,4.8 24.0,6.96 24.0,9.6 L 24.0,17.4 C 24.0,20.04 21.84,22.2 19.2,22.2 L 4.8,22.2 C 2.16,22.2 0.0,20.04 0.0,17.4 L 0.0,9.6 C 0.0,6.96 2.16,4.8 4.8,4.8 Z"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Lens outer ring (stroke)
        addPath(
            pathData = parseNodes("M 6.0,13.5 A 6.0,6.0 0 1,0 18.0,13.5 A 6.0,6.0 0 1,0 6.0,13.5 Z"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Lens center pupil (fill)
        addPath(
            pathData = parseNodes("M 9.6,13.5 A 2.4,2.4 0 1,0 14.4,13.5 A 2.4,2.4 0 1,0 9.6,13.5 Z"),
            fill = SolidColor(Color.White),
        )
        // Flash / sensor dot (fill)
        addPath(
            pathData = parseNodes("M 18.6,9.0 A 1.2,1.2 0 1,0 21.0,9.0 A 1.2,1.2 0 1,0 18.6,9.0 Z"),
            fill = SolidColor(Color.White),
        )
    }

    // === 12. Camera: ACTIVE ===
    val cameraActive: ImageVector = buildIcon {
        // Top flash mount filled
        addPath(
            pathData = parseNodes("M 9.6,1.2 L 14.4,1.2 C 15.72,1.2 16.8,2.28 16.8,3.6 L 16.8,5.4 L 7.2,5.4 L 7.2,3.6 C 7.2,2.28 8.28,1.2 9.6,1.2 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFFDA4AF), Color(0xFFE11D48))),
        )
        // Camera body filled with Rose/Crimson gradient
        addPath(
            pathData = parseNodes("M 4.8,4.8 L 19.2,4.8 C 21.84,4.8 24.0,6.96 24.0,9.6 L 24.0,17.4 C 24.0,20.04 21.84,22.2 19.2,22.2 L 4.8,22.2 C 2.16,22.2 0.0,20.04 0.0,17.4 L 0.0,9.6 C 0.0,6.96 2.16,4.8 4.8,4.8 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFFB7185), Color(0xFFE11D48))),
        )
        // Dark lens backing
        addPath(
            pathData = parseNodes("M 6.0,13.5 A 6.0,6.0 0 1,0 18.0,13.5 A 6.0,6.0 0 1,0 6.0,13.5 Z"),
            fill = SolidColor(Color(0xFF0B1220)),
        )
        // Lens outer ring stroke (Luminous Rose gradient)
        addPath(
            pathData = parseNodes("M 6.0,13.5 A 6.0,6.0 0 1,0 18.0,13.5 A 6.0,6.0 0 1,0 6.0,13.5 Z"),
            stroke = Brush.linearGradient(listOf(Color(0xFFFDA4AF), Color(0xFFFB7185))),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Lens center pupil (Luminous gradient)
        addPath(
            pathData = parseNodes("M 9.6,13.5 A 2.4,2.4 0 1,0 14.4,13.5 A 2.4,2.4 0 1,0 9.6,13.5 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFFDA4AF), Color(0xFFFB7185))),
        )
        // Flash dot
        addPath(
            pathData = parseNodes("M 18.6,9.0 A 1.2,1.2 0 1,0 21.0,9.0 A 1.2,1.2 0 1,0 18.6,9.0 Z"),
            fill = SolidColor(Color(0xFFFFE4E6)),
        )
    }

    // === 13. Lock: INACTIVE ===
    val lockInactive: ImageVector = buildIcon {
        // Shackle loop (stroke)
        addPath(
            pathData = parseNodes("M 6.5,10.5 L 6.5,6.5 C 6.5,3.46 8.96,1.0 12.0,1.0 C 15.04,1.0 17.5,3.46 17.5,6.5 L 17.5,10.5"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Body (stroke outline)
        addPath(
            pathData = parseNodes("M 4.0,10.0 L 20.0,10.0 C 21.1,10.0 22.0,10.9 22.0,12.0 L 22.0,21.0 C 22.0,22.1 21.1,23.0 20.0,23.0 L 4.0,23.0 C 2.9,23.0 2.0,22.1 2.0,21.0 L 2.0,12.0 C 2.0,10.9 2.9,10.0 4.0,10.0 Z"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Keyhole
        addPath(
            pathData = parseNodes("M 12.0,14.0 C 11.17,14.0 10.5,14.67 10.5,15.5 C 10.5,16.1 10.86,16.62 11.38,16.87 L 11.0,19.2 L 13.0,19.2 L 12.62,16.87 C 13.14,16.62 13.5,16.1 13.5,15.5 C 13.5,14.67 12.83,14.0 12.0,14.0 Z"),
            fill = SolidColor(Color.White),
        )
    }

    // === 13. Lock: ACTIVE ===
    val lockActive: ImageVector = buildIcon {
        // Golden Shackle loop (stroke)
        addPath(
            pathData = parseNodes("M 6.5,10.5 L 6.5,6.5 C 6.5,3.46 8.96,1.0 12.0,1.0 C 15.04,1.0 17.5,3.46 17.5,6.5 L 17.5,10.5"),
            stroke = Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFFF59E0B))),
            strokeLineWidth = 2.4f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Filled Golden Body
        addPath(
            pathData = parseNodes("M 4.0,10.0 L 20.0,10.0 C 21.1,10.0 22.0,10.9 22.0,12.0 L 22.0,21.0 C 22.0,22.1 21.1,23.0 20.0,23.0 L 4.0,23.0 C 2.9,23.0 2.0,22.1 2.0,21.0 L 2.0,12.0 C 2.0,10.9 2.9,10.0 4.0,10.0 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFFF59E0B))),
        )
        // Dark Keyhole Cutout inside body
        addPath(
            pathData = parseNodes("M 12.0,14.0 C 11.17,14.0 10.5,14.67 10.5,15.5 C 10.5,16.1 10.86,16.62 11.38,16.87 L 11.0,19.2 L 13.0,19.2 L 12.62,16.87 C 13.14,16.62 13.5,16.1 13.5,15.5 C 13.5,14.67 12.83,14.0 12.0,14.0 Z"),
            fill = SolidColor(Color(0xFF0B1220)),
        )
    }

    // === 14. Page Sidebar: INACTIVE ===
    val sidebarInactive: ImageVector = buildIcon {
        // Outer rounded frame
        addPath(
            pathData = parseNodes("M 6.0,2.0 L 18.0,2.0 C 20.2,2.0 22.0,3.8 22.0,6.0 L 22.0,18.0 C 22.0,20.2 20.2,22.0 18.0,22.0 L 6.0,22.0 C 3.8,22.0 2.0,20.2 2.0,18.0 L 2.0,6.0 C 2.0,3.8 3.8,2.0 6.0,2.0 Z"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Vertical divider separating sidebar from page
        addPath(
            pathData = parseNodes("M 9.0,2.0 L 9.0,22.0"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Thumbnail page markers on left sidebar
        addPath(
            pathData = parseNodes("M 4.75,5.5 L 6.25,5.5 C 6.66,5.5 7.0,5.84 7.0,6.25 L 7.0,6.5 C 7.0,6.91 6.66,7.25 6.25,7.25 L 4.75,7.25 C 4.34,7.25 4.0,6.91 4.0,6.5 L 4.0,6.25 C 4.0,5.84 4.34,5.5 4.75,5.5 Z M 4.75,8.5 L 6.25,8.5 C 6.66,8.5 7.0,8.84 7.0,9.25 L 7.0,9.5 C 7.0,9.91 6.66,10.25 6.25,10.25 L 4.75,10.25 C 4.34,10.25 4.0,9.91 4.0,9.5 L 4.0,9.25 C 4.0,8.84 4.34,8.5 4.75,8.5 Z M 4.75,11.5 L 6.25,11.5 C 6.66,11.5 7.0,11.84 7.0,12.25 L 7.0,12.5 C 7.0,12.91 6.66,13.25 6.25,13.25 L 4.75,13.25 C 4.34,13.25 4.0,12.91 4.0,12.5 L 4.0,12.25 C 4.0,11.84 4.34,11.5 4.75,11.5 Z"),
            fill = SolidColor(Color.White),
        )
        // Content lines in right page
        addPath(
            pathData = parseNodes("M 12.5,5.5 L 18.5,5.5 C 19.05,5.5 19.5,5.95 19.5,6.5 L 19.5,6.5 C 19.5,7.05 19.05,7.5 18.5,7.5 L 12.5,7.5 C 11.95,7.5 11.5,7.05 11.5,6.5 L 11.5,6.5 C 11.5,5.95 11.95,5.5 12.5,5.5 Z M 12.25,9.5 L 17.25,9.5 C 17.66,9.5 18.0,9.84 18.0,10.25 L 18.0,10.25 C 18.0,10.66 17.66,11.0 17.25,11.0 L 12.25,11.0 C 11.84,11.0 11.5,10.66 11.5,10.25 L 11.5,10.25 C 11.5,9.84 11.84,9.5 12.25,9.5 Z M 12.25,12.5 L 18.25,12.5 C 18.66,12.5 19.0,12.84 19.0,13.25 L 19.0,13.25 C 19.0,13.66 18.66,14.0 18.25,14.0 L 12.25,14.0 C 11.84,14.0 11.5,13.66 11.5,13.25 L 11.5,13.25 C 11.5,12.84 11.84,12.5 12.25,12.5 Z M 12.25,15.5 L 15.75,15.5 C 16.16,15.5 16.5,15.84 16.5,16.25 L 16.5,16.25 C 16.5,16.66 16.16,17.0 15.75,17.0 L 12.25,17.0 C 11.84,17.0 11.5,16.66 11.5,16.25 L 11.5,16.25 C 11.5,15.84 11.84,15.5 12.25,15.5 Z"),
            fill = SolidColor(Color.White),
        )
    }

    // === 14. Page Sidebar: ACTIVE ===
    val sidebarActive: ImageVector = buildIcon {
        // Outer rounded frame (Cyan/Blue Gradient Stroke)
        addPath(
            pathData = parseNodes("M 6.0,2.0 L 18.0,2.0 C 20.2,2.0 22.0,3.8 22.0,6.0 L 22.0,18.0 C 22.0,20.2 20.2,22.0 18.0,22.0 L 6.0,22.0 C 3.8,22.0 2.0,20.2 2.0,18.0 L 2.0,6.0 C 2.0,3.8 3.8,2.0 6.0,2.0 Z"),
            stroke = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Filled Left Sidebar Panel (Cyan/Blue Gradient)
        addPath(
            pathData = parseNodes("M 5.5,2.0 L 9.0,2.0 L 9.0,22.0 L 5.5,22.0 C 3.57,22.0 2.0,20.44 2.0,18.5 L 2.0,5.5 C 2.0,3.56 3.57,2.0 5.5,2.0 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
        )
        // Vertical divider
        addPath(
            pathData = parseNodes("M 9.0,2.0 L 9.0,22.0"),
            stroke = SolidColor(Color(0xFF06B6D4)),
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Crisp White thumbnail dots on the left sidebar
        addPath(
            pathData = parseNodes("M 4.75,5.5 L 6.25,5.5 C 6.66,5.5 7.0,5.84 7.0,6.25 L 7.0,6.5 C 7.0,6.91 6.66,7.25 6.25,7.25 L 4.75,7.25 C 4.34,7.25 4.0,6.91 4.0,6.5 L 4.0,6.25 C 4.0,5.84 4.34,5.5 4.75,5.5 Z M 4.75,8.5 L 6.25,8.5 C 6.66,8.5 7.0,8.84 7.0,9.25 L 7.0,9.5 C 7.0,9.91 6.66,10.25 6.25,10.25 L 4.75,10.25 C 4.34,10.25 4.0,9.91 4.0,9.5 L 4.0,9.25 C 4.0,8.84 4.34,8.5 4.75,8.5 Z M 4.75,11.5 L 6.25,11.5 C 6.66,11.5 7.0,11.84 7.0,12.25 L 7.0,12.5 C 7.0,12.91 6.66,13.25 6.25,13.25 L 4.75,13.25 C 4.34,13.25 4.0,12.91 4.0,12.5 L 4.0,12.25 C 4.0,11.84 4.34,11.5 4.75,11.5 Z"),
            fill = SolidColor(Color.White),
        )
        // Top content headline in right page (Sky/Indigo Gradient)
        addPath(
            pathData = parseNodes("M 12.5,5.5 L 18.5,5.5 C 19.05,5.5 19.5,5.95 19.5,6.5 L 19.5,6.5 C 19.5,7.05 19.05,7.5 18.5,7.5 L 12.5,7.5 C 11.95,7.5 11.5,7.05 11.5,6.5 L 11.5,6.5 C 11.5,5.95 11.95,5.5 12.5,5.5 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8))),
        )
        // Right page text lines
        addPath(
            pathData = parseNodes("M 12.25,9.5 L 17.25,9.5 C 17.66,9.5 18.0,9.84 18.0,10.25 L 18.0,10.25 C 18.0,10.66 17.66,11.0 17.25,11.0 L 12.25,11.0 C 11.84,11.0 11.5,10.66 11.5,10.25 L 11.5,10.25 C 11.5,9.84 11.84,9.5 12.25,9.5 Z M 12.25,12.5 L 18.25,12.5 C 18.66,12.5 19.0,12.84 19.0,13.25 L 19.0,13.25 C 19.0,13.66 18.66,14.0 18.25,14.0 L 12.25,14.0 C 11.84,14.0 11.5,13.66 11.5,13.25 L 11.5,13.25 C 11.5,12.84 11.84,12.5 12.25,12.5 Z M 12.25,15.5 L 15.75,15.5 C 16.16,15.5 16.5,15.84 16.5,16.25 L 16.5,16.25 C 16.5,16.66 16.16,17.0 15.75,17.0 L 12.25,17.0 C 11.84,17.0 11.5,16.66 11.5,16.25 L 11.5,16.25 C 11.5,15.84 11.84,15.5 12.25,15.5 Z"),
            fill = SolidColor(Color(0xFF38BDF8)),
        )
    }

    // === 15. Voice Note: INACTIVE ===
    val voiceInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 16.55,11.62 C 16.55,14.13 14.51,16.17 12.0,16.17 C 9.49,16.17 7.45,14.13 7.45,11.62 L 7.45,5.55 C 7.45,3.04 9.49,1.0 12.0,1.0 C 14.51,1.0 16.55,3.04 16.55,5.55 L 16.55,11.62 Z M 8.97,5.55 C 8.97,3.87 10.32,2.52 12.0,2.52 C 13.68,2.52 15.03,3.87 15.03,5.55 L 15.03,11.62 C 15.03,13.3 13.68,14.66 12.0,14.66 C 10.32,14.66 8.97,13.3 8.97,11.62 L 8.97,5.55 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
        addPath(
            pathData = parseNodes("M 12.76,19.17 L 12.76,22.24 C 12.76,22.66 12.42,23.0 12.0,23.0 C 11.58,23.0 11.24,22.66 11.24,22.24 L 11.24,19.17 C 7.37,18.78 4.41,15.51 4.41,11.62 C 4.41,11.2 4.75,10.86 5.17,10.86 C 5.59,10.86 5.93,11.2 5.93,11.62 C 5.93,14.97 8.65,17.69 12.0,17.69 C 15.35,17.69 18.07,14.97 18.07,11.62 C 18.07,11.2 18.41,10.86 18.83,10.86 C 19.25,10.86 19.59,11.2 19.59,11.62 C 19.59,15.51 16.63,18.78 12.76,19.17 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        )
    }

    // === 15. Voice Note: ACTIVE ===
    val voiceActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 7.45,11.62 L 7.45,5.55 C 7.45,3.04 9.49,1.0 12.0,1.0 C 14.51,1.0 16.55,3.04 16.55,5.55 L 16.55,11.62 C 16.55,14.13 14.51,16.17 12.0,16.17 C 9.49,16.17 7.45,14.13 7.45,11.62 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFFF2D55), Color(0xFFAF52DE))),
            pathFillType = PathFillType.NonZero,
        )
        addPath(
            pathData = parseNodes("M 18.83,10.86 C 19.25,10.86 19.59,11.2 19.59,11.62 C 19.59,15.51 16.63,18.78 12.76,19.17 L 12.76,22.24 C 12.76,22.66 12.42,23.0 12.0,23.0 C 11.58,23.0 11.24,22.66 11.24,22.24 L 11.24,19.17 C 7.37,18.78 4.41,15.51 4.41,11.62 C 4.41,11.2 4.75,10.86 5.17,10.86 C 5.59,10.86 5.93,11.2 5.93,11.62 C 5.93,14.97 8.65,17.69 12.0,17.69 C 15.35,17.69 18.07,14.97 18.07,11.62 C 18.07,11.2 18.41,10.86 18.83,10.86 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFFFF2D55), Color(0xFFAF52DE))),
            pathFillType = PathFillType.NonZero,
        )
    }

    // === 16. Share: INACTIVE ===
    val shareInactive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 15.03,16.73 C 15.83,15.9 16.94,15.43 18.09,15.43 C 19.97,15.43 21.63,16.66 22.19,18.45 C 22.75,20.24 22.07,22.19 20.52,23.25 C 18.97,24.31 16.91,24.24 15.44,23.07 C 13.97,21.9 13.42,19.92 14.1,18.17 L 9.16,14.99 C 7.94,16.24 6.1,16.63 4.48,15.97 C 2.86,15.31 1.81,13.74 1.81,12.0 C 1.81,10.26 2.86,8.69 4.48,8.03 C 6.1,7.37 7.94,7.76 9.16,9.01 L 14.1,5.84 C 13.3,3.79 14.19,1.47 16.15,0.47 C 18.11,-0.53 20.51,0.12 21.7,1.97 C 22.89,3.82 22.49,6.27 20.77,7.64 C 19.05,9.01 16.57,8.85 15.03,7.28 L 10.09,10.45 C 10.48,11.45 10.48,12.55 10.09,13.55 L 15.03,16.73 Z M 18.09,1.72 C 19.51,1.72 20.67,2.87 20.67,4.29 C 20.67,5.71 19.51,6.86 18.09,6.86 C 16.67,6.86 15.52,5.71 15.52,4.29 C 15.52,2.87 16.67,1.72 18.09,1.72 Z M 6.1,14.57 C 4.68,14.57 3.53,13.42 3.53,12.0 C 3.53,10.58 4.68,9.43 6.1,9.43 C 7.52,9.43 8.67,10.58 8.67,12.0 C 8.67,13.42 7.52,14.57 6.1,14.57 Z M 18.09,22.29 C 16.67,22.29 15.52,21.14 15.52,19.72 C 15.52,18.3 16.67,17.14 18.09,17.14 C 19.51,17.14 20.67,18.3 20.67,19.72 C 20.67,21.14 19.51,22.29 18.09,22.29 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 16. Share: ACTIVE ===
    val shareActive: ImageVector = buildIcon {
        addPath(
            pathData = parseNodes("M 22.31,20.01 C 22.31,21.6 21.36,23.05 19.9,23.68 C 18.44,24.31 16.73,24.01 15.57,22.92 C 14.41,21.83 14.01,20.15 14.55,18.65 L 8.67,14.87 C 7.1,16.41 4.58,16.39 3.03,14.83 C 1.48,13.27 1.48,10.76 3.03,9.2 C 4.58,7.64 7.1,7.62 8.67,9.16 L 14.55,5.38 C 13.85,3.46 14.71,1.33 16.54,0.42 C 18.37,-0.49 20.58,0.13 21.69,1.84 C 22.8,3.55 22.43,5.83 20.85,7.12 C 19.27,8.41 16.98,8.31 15.52,6.88 L 9.63,10.66 C 9.95,11.54 9.95,12.5 9.63,13.38 L 15.52,17.16 C 16.67,16.04 18.38,15.71 19.86,16.33 C 21.34,16.95 22.31,18.4 22.31,20.01 Z"),
            fill = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF06B6D4), Color(0xFF6366F1))),
            pathFillType = PathFillType.EvenOdd,
        )
    }

    // === 17. Reading Mode (Book with page lines): INACTIVE ===
    val readingModeInactive: ImageVector = buildIcon(viewportWidth = 20.0f, viewportHeight = 17.14f, translationX = -41.75f, translationY = -11.93f) {
        addPath(
            pathData = parseNodes("M 54.61,11.93 L 61.04,11.93 C 61.43,11.93 61.75,12.25 61.75,12.64 L 61.75,25.5 C 61.75,25.89 61.43,26.21 61.04,26.21 L 54.61,26.21 C 53.43,26.21 52.46,27.18 52.46,28.36 C 52.46,28.75 52.14,29.07 51.75,29.07 C 51.36,29.07 51.04,28.75 51.04,28.36 C 51.04,27.18 50.07,26.21 48.89,26.21 L 42.46,26.21 C 42.07,26.21 41.75,25.89 41.75,25.5 L 41.75,12.64 C 41.75,12.25 42.07,11.93 42.46,11.93 L 48.89,11.93 C 50.01,11.93 51.08,12.46 51.75,13.36 C 52.42,12.46 53.49,11.93 54.61,11.93 Z M 48.89,24.79 L 43.18,24.79 L 43.18,13.36 L 48.89,13.36 C 50.07,13.36 51.04,14.32 51.04,15.5 L 51.04,25.5 C 50.42,25.03 49.66,24.79 48.89,24.79 Z M 60.32,24.79 L 54.61,24.79 C 53.84,24.79 53.08,25.03 52.46,25.5 L 52.46,15.5 C 52.46,14.32 53.43,13.36 54.61,13.36 L 60.32,13.36 L 60.32,24.79 Z"),
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
        addPath(
            pathData = parseNodes("M 54.61,15.5 L 58.18,15.5 C 58.57,15.5 58.89,15.82 58.89,16.21 C 58.89,16.6 58.57,16.93 58.18,16.93 L 54.61,16.93 C 54.22,16.93 53.89,16.6 53.89,16.21 C 53.89,15.82 54.22,15.5 54.61,15.5 Z"),
            fill = SolidColor(Color.White),
        )
        addPath(
            pathData = parseNodes("M 58.89,19.07 C 58.89,19.46 58.57,19.79 58.18,19.79 L 54.61,19.79 C 54.22,19.79 53.89,19.46 53.89,19.07 C 53.89,18.68 54.22,18.36 54.61,18.36 L 58.18,18.36 C 58.57,18.36 58.89,18.68 58.89,19.07 Z"),
            fill = SolidColor(Color.White),
        )
        addPath(
            pathData = parseNodes("M 58.89,21.93 C 58.89,22.32 58.57,22.64 58.18,22.64 L 54.61,22.64 C 54.22,22.64 53.89,22.32 53.89,21.93 C 53.89,21.54 54.22,21.21 54.61,21.21 L 58.18,21.21 C 58.57,21.21 58.89,21.54 58.89,21.93 Z"),
            fill = SolidColor(Color.White),
        )
    }

    // === 17. Reading Mode (Book with page lines): ACTIVE ===
    val readingModeActive: ImageVector = buildIcon(viewportWidth = 20.0f, viewportHeight = 17.14f, translationX = -41.75f, translationY = -11.93f) {
        val tealCyanGrad = Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF38BDF8)))
        addPath(
            pathData = parseNodes("M 54.61,11.93 L 61.04,11.93 C 61.43,11.93 61.75,12.25 61.75,12.64 L 61.75,25.5 C 61.75,25.89 61.43,26.21 61.04,26.21 L 54.61,26.21 C 53.43,26.21 52.46,27.18 52.46,28.36 C 52.46,28.75 52.14,29.07 51.75,29.07 C 51.36,29.07 51.04,28.75 51.04,28.36 C 51.04,27.18 50.07,26.21 48.89,26.21 L 42.46,26.21 C 42.07,26.21 41.75,25.89 41.75,25.5 L 41.75,12.64 C 41.75,12.25 42.07,11.93 42.46,11.93 L 48.89,11.93 C 50.01,11.93 51.08,12.46 51.75,13.36 C 52.42,12.46 53.49,11.93 54.61,11.93 Z M 48.89,24.79 L 43.18,24.79 L 43.18,13.36 L 48.89,13.36 C 50.07,13.36 51.04,14.32 51.04,15.5 L 51.04,25.5 C 50.42,25.03 49.66,24.79 48.89,24.79 Z M 60.32,24.79 L 54.61,24.79 C 53.84,24.79 53.08,25.03 52.46,25.5 L 52.46,15.5 C 52.46,14.32 53.43,13.36 54.61,13.36 L 60.32,13.36 L 60.32,24.79 Z"),
            fill = tealCyanGrad,
            pathFillType = PathFillType.EvenOdd,
        )
        addPath(
            pathData = parseNodes("M 54.61,15.5 L 58.18,15.5 C 58.57,15.5 58.89,15.82 58.89,16.21 C 58.89,16.6 58.57,16.93 58.18,16.93 L 54.61,16.93 C 54.22,16.93 53.89,16.6 53.89,16.21 C 53.89,15.82 54.22,15.5 54.61,15.5 Z"),
            fill = tealCyanGrad,
        )
        addPath(
            pathData = parseNodes("M 58.89,19.07 C 58.89,19.46 58.57,19.79 58.18,19.79 L 54.61,19.79 C 54.22,19.79 53.89,19.46 53.89,19.07 C 53.89,18.68 54.22,18.36 54.61,18.36 L 58.18,18.36 C 58.57,18.36 58.89,18.68 58.89,19.07 Z"),
            fill = tealCyanGrad,
        )
        addPath(
            pathData = parseNodes("M 58.89,21.93 C 58.89,22.32 58.57,22.64 58.18,22.64 L 54.61,22.64 C 54.22,22.64 53.89,22.32 53.89,21.93 C 53.89,21.54 54.22,21.21 54.61,21.21 L 58.18,21.21 C 58.57,21.21 58.89,21.54 58.89,21.93 Z"),
            fill = tealCyanGrad,
        )
    }
}