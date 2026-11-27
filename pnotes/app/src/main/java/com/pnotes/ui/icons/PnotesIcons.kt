package com.pnotes.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Cohesive Hugeicons Stroke-Rounded Icon System for PNotes Toolbar & Canvas.
 * Supports both clean inactive outline states (1.5px rounded stroke) and
 * high-contrast active filled states for Apple System Blue active tool states.
 */
typealias PNotesIcons = PnotesIcons

object PnotesIcons {

    private fun strokeIcon(strokeWidth: Float = 1.5f, vararg pathData: String): ImageVector =
        strokeIconCustomViewport(strokeWidth, 24f, 24f, 0f, 0f, *pathData)

    private fun strokeIconCustomViewport(
        strokeWidth: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        translationX: Float = 0f,
        translationY: Float = 0f,
        vararg pathData: String
    ): ImageVector {
        val nodes = PathParser().parsePathString(pathData.joinToString(" ")).toNodes()
        val builder = ImageVector.Builder(
            name = "xn_stroke",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        )
        if (translationX != 0f || translationY != 0f) {
            builder.addGroup(
                name = "g",
                translationX = translationX,
                translationY = translationY,
            )
        }
        builder.addPath(
            pathData = nodes,
            stroke = SolidColor(Color.White),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        return builder.build()
    }

    private fun filledIcon(vararg pathData: String): ImageVector =
        filledIconCustomViewport(24f, 24f, 0f, 0f, *pathData)

    private fun filledIconCustomViewport(
        viewportWidth: Float,
        viewportHeight: Float,
        translationX: Float = 0f,
        translationY: Float = 0f,
        vararg pathData: String
    ): ImageVector {
        val nodes = PathParser().parsePathString(pathData.joinToString(" ")).toNodes()
        val builder = ImageVector.Builder(
            name = "xn_fill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        )
        if (translationX != 0f || translationY != 0f) {
            builder.addGroup(
                name = "g",
                translationX = translationX,
                translationY = translationY,
            )
        }
        builder.addPath(
            pathData = nodes,
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        )
        return builder.build()
    }

    private fun combinedIcon(
        strokePaths: List<String> = emptyList(),
        fillPaths: List<String> = emptyList(),
        strokeWidth: Float = 1.5f,
        viewportWidth: Float = 24f,
        viewportHeight: Float = 24f,
        translationX: Float = 0f,
        translationY: Float = 0f,
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = "xn_combo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        )
        if (translationX != 0f || translationY != 0f) {
            builder.addGroup(
                name = "g",
                translationX = translationX,
                translationY = translationY,
            )
        }
        if (fillPaths.isNotEmpty()) {
            val fillNodes = PathParser().parsePathString(fillPaths.joinToString(" ")).toNodes()
            builder.addPath(
                pathData = fillNodes,
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd,
            )
        }
        if (strokePaths.isNotEmpty()) {
            val strokeNodes = PathParser().parsePathString(strokePaths.joinToString(" ")).toNodes()
            builder.addPath(
                pathData = strokeNodes,
                stroke = SolidColor(Color.White),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }

    private fun circle(cx: Double, cy: Double, r: Double): String =
        "M ${cx - r} $cy a $r $r 0 1 0 ${2 * r} 0 a $r $r 0 1 0 ${-2 * r} 0"

    private fun rect(x: Double, y: Double, w: Double, h: Double): String =
        "M $x $y h $w v $h h ${-w} Z"

    private fun roundRect(x: Double, y: Double, w: Double, h: Double, r: Double): String =
        "M ${x + r} $y h ${w - 2 * r} a $r $r 0 0 1 $r $r v ${h - 2 * r} " +
            "a $r $r 0 0 1 ${-r} $r h ${-(w - 2 * r)} a $r $r 0 0 1 ${-r} ${-r} " +
            "v ${-(h - 2 * r)} a $r $r 0 0 1 $r ${-r} Z"

    private fun ellipse(cx: Double, cy: Double, rx: Double, ry: Double): String =
        "M ${cx - rx} $cy a $rx $ry 0 1 0 ${2 * rx} 0 a $rx $ry 0 1 0 ${-2 * rx} 0"

    // === 1. SELECT (Cursor Rectangle Selection - Hugeicons cursor-rectangle-selection-02) ===
    private const val SELECT_CURSOR_PATH =
        "M14.5352 11.0865L18.5575 12.6605C20.8775 13.5683 22.0375 14.0222 21.9991 14.7422C21.9606 15.4622 20.75 15.7924 18.3288 16.4527C17.6079 16.6493 17.2475 16.7476 16.9976 16.9976C16.7476 17.2475 16.6493 17.6079 16.4527 18.3288C15.7924 20.75 15.4622 21.9606 14.7422 21.9991C14.0222 22.0375 13.5683 20.8775 12.6605 18.5575L11.0865 14.5352C10.136 12.1062 9.6608 10.8918 10.2763 10.2763C10.8918 9.6608 12.1062 10.136 14.5352 11.0865Z"
    private const val SELECT_BOX_CORNERS =
        "M2 8.5V11.5M11.5 2H8.5M8.5 18H9M18 9V8.5M4.5 18C3.11929 18 2 16.8807 2 15.5M2 4.5C2 3.11929 3.11929 2 4.5 2M18 4.5C18 3.11929 16.8807 2 15.5 2"

    val select = strokeIcon(1.5f, SELECT_CURSOR_PATH, SELECT_BOX_CORNERS)
    val selectFilled = combinedIcon(
        strokePaths = listOf(SELECT_BOX_CORNERS),
        fillPaths = listOf(SELECT_CURSOR_PATH),
        strokeWidth = 1.8f,
    )

    // === 2. PAN (Canvas Hand - Hugeicons hold-03) ===
    private const val PAN_HAND_PATH =
        "M20.5 9.5V14.1667C20.5 16.34 20.5 17.4267 20.1689 18.2918C19.6627 19.6148 18.6207 20.6601 17.3019 21.1679C16.4395 21.5 15.3562 21.5 13.1896 21.5C12.0534 21.5 11.4853 21.5 10.9566 21.3834C10.1499 21.2056 9.40001 20.8294 8.77419 20.2888C8.36398 19.9344 8.02311 19.4785 7.34137 18.5667L4.33738 14.5487C3.8758 13.9314 3.88907 13.0789 4.36965 12.4763C4.99772 11.6888 6.16877 11.6237 6.8797 12.3369L8.5011 13.9634V10.5V6C8.5011 5.17157 9.17267 4.5 10.0011 4.5C10.8295 4.5 11.5011 5.17157 11.5011 6M11.5011 6V4C11.5011 3.17157 12.1727 2.5 13.0011 2.5C13.8295 2.5 14.5011 3.17157 14.5011 4V6M11.5011 6V10.5M14.5011 6C14.5011 5.17157 15.1727 4.5 16.0011 4.5C16.8295 4.5 17.5011 5.17157 17.5011 6V8M14.5011 6V10.5M20.5011 10.5V8C20.5011 7.17157 19.8295 6.5 19.0011 6.5C18.1727 6.5 17.5011 7.17157 17.5011 8M17.5011 8V10.5"

    val pan = strokeIcon(1.5f, PAN_HAND_PATH)
    val panFilled = filledIcon(PAN_HAND_PATH + "Z")

    // === 3. ERASER (Updated from Change Icon / eraser inactive.svg) ===
    val eraser = strokeIconCustomViewport(
        44f,
        709f,
        709f,
        0f,
        20f,
        "M51.95 363.28L196.315 240.2L360.606 432.903L216.241 555.983C198.504 571.106 171.865 568.986 156.743 551.248L47.2148 422.779C32.0923 405.041 34.2123 378.403 51.95 363.28Z",
        "M422.673 47.2154C440.411 32.0928 467.05 34.2129 482.172 51.9506L591.7 180.42C606.822 198.157 604.703 224.796 586.965 239.918L361.708 431.964L197.417 239.261L422.673 47.2154Z",
    )
    val eraserFilled = filledIconCustomViewport(
        709f,
        709f,
        0f,
        20f,
        "M36.5085 431.907C16.345 408.256 19.1718 372.738 42.8222 352.575L187.196 229.487L369.742 443.601L276.144 523.4L240.163 554.076C233.18 560.029 224.304 563.299 215.128 563.299L174.354 563.299C157.839 563.299 142.158 556.044 131.466 543.457L91.2723 496.141L36.5085 431.907Z",
        "M214.115 206.538L413.547 36.5091C437.198 16.3457 472.716 19.1724 492.88 42.8228L602.407 171.291C622.571 194.942 619.744 230.46 596.093 250.623L396.661 420.652L214.115 206.538Z",
    )

    // === 4. REGULAR PEN / PENCIL ===
    val pen = strokeIcon(
        2.0f,
        "M12 20H21",
        "M3.00003 20H4.67457C5.16376 20 5.40835 20 5.63852 19.9447C5.84259 19.8957 6.03768 19.8149 6.21663 19.7053C6.41846 19.5816 6.59141 19.4086 6.93732 19.0627L19.5001 6.49998C20.3285 5.67156 20.3285 4.32841 19.5001 3.49998C18.6716 2.67156 17.3285 2.67156 16.5001 3.49998L3.93729 16.0627C3.59139 16.4086 3.41843 16.5816 3.29475 16.7834C3.18509 16.9624 3.10428 17.1574 3.05529 17.3615C3.00003 17.5917 3.00003 17.8363 3.00003 18.3255V20Z",
    )
    val penFilled = pen

    // === 5. FOUNTAIN / CALLIGRAPHY PEN ===
    val fountainPen = filledIconCustomViewport(
        viewportWidth = 64f,
        viewportHeight = 64f,
        pathData = arrayOf(
            "M54.97,24.31 C55.64,24.97 56.01,25.88 56.01,26.82 C56.01,27.76 55.64,28.68 54.97,29.34 L48.65,35.67 L43.81,48.56 C43.37,49.75 42.32,50.61 41.07,50.82 L10.08,55.98 C9.98,56 9.89,56 9.79,56 C9.27,56 8.77,55.78 8.43,55.38 C8.09,54.98 7.95,54.45 8.04,53.93 L13.2,22.95 C13.41,21.7 14.28,20.64 15.47,20.2 L28.36,15.36 L34.68,9.05 C36.07,7.66 38.32,7.66 39.71,9.05 L54.97,24.31 Z M40.48,47.31 L14.97,51.56 L25.5,41.03 C28.22,42.5 31.59,41.76 33.44,39.28 C35.29,36.8 35.03,33.34 32.85,31.16 C30.67,28.98 27.22,28.72 24.74,30.57 C22.26,32.42 21.51,35.8 22.98,38.52 L12.46,49.05 L16.71,23.53 L28.9,18.96 L45.05,35.12 L40.48,47.31 Z M25.79,35.56 C25.79,34.09 26.98,32.89 28.45,32.89 C29.92,32.89 31.12,34.09 31.12,35.56 C31.12,37.03 29.92,38.23 28.45,38.23 C26.98,38.23 25.79,37.03 25.79,35.56 Z M47.12,32.16 L31.86,16.9 L37.19,11.56 L52.45,26.82 L47.12,32.16 Z"
        )
    )
    val fountainPenFilled = fountainPen

    // === 6. HIGHLIGHTER (Updated from Change Icon / Highlighter Inactive.svg) ===
    val highlighter = combinedIcon(
        strokePaths = listOf(
            "M164.169 290.504C155.161 278.488 158.436 261.329 171.236 253.475L481.842 62.8746C492.988 56.035 507.502 58.7363 515.441 69.1279L649.351 244.394C657.412 254.945 656.028 269.916 646.17 278.81L374.134 524.226C363 534.271 345.644 532.587 336.649 520.588L164.169 290.504Z",
            "M319.293 520.583L259.654 576.898C257.679 578.763 255.001 579.693 252.295 579.454L80.6465 564.269L69.9521 552.046L69.9199 552.01L69.8877 551.973L53.9316 534.022L51.3428 531.035L80.1182 358.846C80.5971 355.98 82.3609 353.49 84.9062 352.089L161.029 310.18L319.293 520.583Z",
        ),
        fillPaths = listOf("M0.546455 565.277L38.0448 534.778L74.545 576.277L38.0448 586.277L0.546455 565.277Z"),
        strokeWidth = 44f,
        viewportWidth = 705f,
        viewportHeight = 705f,
        translationX = 0f,
        translationY = 48f,
    )
    val highlighterFilled = highlighter

    // === 7. DASHED PEN (Updated from Change Icon / Dash Pen Inactive.svg) ===
    val dashedPen = combinedIcon(
        strokePaths = listOf(
            "M135.739 582.742L233.328 270.618L240.458 274.111L250.602 317.327L252.609 325.875L261.312 326.893L291.311 330.402L291.454 330.419L291.596 330.432L298.024 331.036L298.941 336.533L299.087 337.41L299.356 338.257L311.859 377.74L314.166 385.026L321.69 386.295L360.864 392.904L373.133 400.276L377.569 409.489L384.784 433.996L386.421 439.556L391.713 441.9L431.328 459.455C434.428 460.828 436.874 461.919 438.837 462.834C436.84 463.672 434.353 464.665 431.203 465.916L136.136 583.11C135.937 583.189 135.743 583.267 135.553 583.342C135.614 583.146 135.675 582.946 135.739 582.742Z",
            "M402.866 82.5045C419.061 65.0113 430.374 52.8305 440.134 44.7674C449.614 36.9354 456.201 34.2261 462.702 33.9629C469.204 33.6997 475.987 35.8676 486.07 42.9075C496.45 50.1552 508.711 61.382 526.267 77.509L529.357 80.3477L296.879 333.429L253.577 333.162L238.194 260.376L402.866 82.5045Z",
            "M529.354 80.2596L600.305 145.435L368.588 397.687L314.966 387.136L297.776 332.36L529.354 80.2596Z",
            "M600.008 145.097L645.251 186.657C660.511 200.675 661.51 224.413 647.482 239.663L449.351 455.044L385.584 438.861L368.251 397.393L600.008 145.097Z",
        ),
        fillPaths = listOf(
            "M156.958 588.387C140.622 594.794 124.117 579.472 129.248 562.662L155.316 477.25L239.964 555.833L156.958 588.387Z",
            "M259.357 589.956h51.75v22h-51.75Z M331.412 589.993h51.87v22h-51.87Z M403.594 590.03h51.87v22h-51.87Z M475.773 590.067h51.87v22h-51.87Z M547.953 590.104h52.46v22h-52.46Z",
        ),
        strokeWidth = 45f,
        viewportWidth = 717f,
        viewportHeight = 717f,
        translationX = 13f,
        translationY = 0f,
    )
    val dashedPenFilled = dashedPen

    // === 8. SPEED & TAPER PENS ===
    val speedPen = combinedIcon(
        strokePaths = listOf("M6.5,41 L10.5,41 M14.5,41 L18.5,41 M22.5,41 L26.5,41"),
        fillPaths = listOf("M38.6,5.7 L45.3,12.41 C46.24,13.35 46.24,14.86 45.3,15.8 L41.33,19.77 L42.37,23.91 C42.47,24.32 42.35,24.75 42.05,25.05 L36.05,31.05 C35.58,31.52 34.83,31.52 34.36,31.05 C33.89,30.58 33.89,29.82 34.36,29.35 L39.88,23.83 L39.35,21.75 L26.8,34.3 C26.35,34.75 25.75,35 25.11,35 L18.4,35 C17.07,35 16,33.93 16,32.6 L16,25.9 C16,25.26 16.26,24.65 16.71,24.2 L25.96,14.95 L35.2,5.7 C36.14,4.76 37.66,4.76 38.6,5.7 Z M18.4,28.3 L22.71,32.6 L18.4,32.6 L18.4,28.3 Z M25.6,32.1 L18.9,25.4 L31.6,12.7 L38.31,19.4 L25.6,32.1 Z M40,17.7 L33.3,11 L36.9,7.4 L43.6,14.1 L40,17.7 Z"),
        strokeWidth = 2.5f,
        viewportWidth = 48f,
        viewportHeight = 48f,
        translationX = 0f,
        translationY = 0f,
    )
    val speedPenFilled = speedPen

    val taperPen = filledIconCustomViewport(
        viewportWidth = 38f,
        viewportHeight = 32.08f,
        pathData = arrayOf(
            "M27.36,8 C27.72,8 28,8.29 28,8.65 C28,12.19 24.01,15.82 21.36,17.84 C21.81,19.3 21.54,20.89 20.63,22.12 C19.72,23.35 18.28,24.08 16.75,24.08 L10.65,24.08 C10.37,24.08 10.11,23.89 10.03,23.62 C9.95,23.35 10.06,23.06 10.29,22.9 C10.36,22.85 11.93,21.73 11.93,19.25 C11.93,17.72 12.66,16.29 13.89,15.38 C15.12,14.47 16.71,14.19 18.17,14.64 C20.19,11.99 23.82,8 27.36,8 Z M16.75,22.79 L12.14,22.79 C12.68,22.02 13.22,20.85 13.22,19.25 C13.22,17.3 14.8,15.72 16.75,15.72 C18.7,15.72 20.29,17.3 20.29,19.25 C20.29,21.2 18.7,22.79 16.75,22.79 Z M19.36,15.2 C19.64,14.84 19.9,14.51 20.17,14.19 C20.82,14.63 21.38,15.19 21.82,15.84 C21.5,16.1 21.17,16.37 20.81,16.65 C20.44,16.07 19.94,15.57 19.36,15.2 Z M22.79,14.98 C22.3,14.3 21.7,13.71 21.02,13.22 C23.57,10.45 25.5,9.6 26.64,9.37 C26.41,10.51 25.56,12.43 22.79,14.98 Z"
        )
    )
    val taperPenFilled = taperPen
    val brush = taperPen
    val brushFilled = taperPenFilled

    // === 9. SHAPES (Hugeicons continuous rounded geometry) ===
    private const val RECTANGLE_PATH =
        "M1.99219 12C1.99219 9.19974 1.99219 7.79961 2.53715 6.73005C3.01652 5.78924 3.78142 5.02433 4.72223 4.54497C5.79179 4 7.19192 4 9.99219 4H13.9922C16.7924 4 18.1926 4 19.2621 4.54497C20.203 5.02433 20.9679 5.78924 21.4472 6.73005C21.9922 7.79961 21.9922 9.19974 21.9922 12C21.9922 14.8003 21.9922 16.2004 21.4472 17.27C20.9679 18.2108 20.203 18.9757 19.2621 19.455C18.1926 20 16.7925 20 13.9922 20H9.99219C7.19192 20 5.79179 20 4.72223 19.455C3.78142 18.9757 3.01652 18.2108 2.53715 17.27C1.99219 16.2004 1.99219 14.8003 1.99219 12Z"

    val shapeRect = strokeIcon(1.5f, RECTANGLE_PATH)
    val shapeRectFilled = filledIcon(RECTANGLE_PATH)
    val shape = shapeRect
    val shapeFilled = shapeRectFilled

    val shapeCircle = strokeIcon(1.5f, circle(12.0, 12.0, 8.5))
    val shapeCircleFilled = filledIcon(circle(12.0, 12.0, 8.5))

    val shapeLine = strokeIcon(1.5f, "M3 20L21 4")
    val shapeLineFilled = strokeIcon(2.8f, "M3 20L21 4")

    private const val ARROW_SHAFT = "M12.9997 11L5.99969 18"
    private const val ARROW_HEAD =
        "M15.5407 6.08278L14.2989 6.19567C12.287 6.37857 11.2811 6.47002 11.0444 7.12388C10.8076 7.77774 11.5219 8.49198 12.9504 9.92046L14.0792 11.0493C15.5077 12.4778 16.222 13.1921 16.8758 12.9553C17.5297 12.7186 17.6211 11.7127 17.804 9.70078L17.9169 8.45902C18.027 7.24766 18.0821 6.64198 17.7199 6.27979C17.3577 5.9176 16.752 5.97266 15.5407 6.08278Z"

    val shapeArrow = strokeIcon(1.5f, ARROW_SHAFT, ARROW_HEAD)
    val shapeArrowFilled = combinedIcon(
        strokePaths = listOf(ARROW_SHAFT),
        fillPaths = listOf(ARROW_HEAD),
        strokeWidth = 2.2f,
    )

    val shapeEllipse = strokeIcon(1.5f, ellipse(12.0, 12.0, 9.5, 6.5))
    val shapeEllipseFilled = filledIcon(ellipse(12.0, 12.0, 9.5, 6.5))

    val shapeTriangle = strokeIcon(1.5f, "M12 4 21 19 3 19 Z")
    val shapeTriangleFilled = filledIcon("M12 4 21 19 3 19 Z")

    // === 10. TEXT (Hugeicons text-stroke-rounded) ===
    private const val TEXT_TOP_BAR = "M4 7V4.5C4 4.22386 4.22386 4 4.5 4H19.5C19.7761 4 20 4.22386 20 4.5V7"
    private const val TEXT_STEM = "M12 4V20"
    private const val TEXT_BASE = "M9 20H15"

    val text = strokeIcon(1.5f, TEXT_TOP_BAR, TEXT_STEM, TEXT_BASE)
    val textFilled = strokeIcon(2.5f, TEXT_TOP_BAR, TEXT_STEM, TEXT_BASE)
    val textBox = strokeIcon(1.5f, rect(3.0, 3.0, 18.0, 18.0), "M7 8V6h10v2", "M12 6v12", "M9.5 18h5")

    // === 11. INSERT IMAGE (Hugeicons image-plus-stroke-rounded) ===
    private const val IMAGE_FRAME_PATH =
        "M11.5 21.5C7.35301 21.4981 5.22972 21.4473 3.89124 20.1088C2.5 18.7176 2.5 16.4784 2.5 12C2.5 7.52168 2.5 5.2825 3.89124 3.89125C5.28249 2.5 7.52166 2.5 12 2.5C16.4783 2.5 18.7175 2.5 20.1088 3.89125C21.5 5.2825 21.5 7.52168 21.5 12"
    private const val IMAGE_PLUS_BADGE =
        "M21.5 18.5H18.5M18.5 18.5H15.5M18.5 18.5V15.5M18.5 18.5V21.5"
    private const val IMAGE_MOUNTAIN_PATH = "M13.0018 13.0616C10.1493 14.6467 7.70477 17.7414 5.41797 20.5"

    val image = strokeIcon(1.5f, IMAGE_FRAME_PATH, IMAGE_PLUS_BADGE, circle(7.5, 7.5, 1.5), IMAGE_MOUNTAIN_PATH)
    val imageFilled = combinedIcon(
        strokePaths = listOf(IMAGE_PLUS_BADGE),
        fillPaths = listOf(IMAGE_FRAME_PATH + "Z"),
        strokeWidth = 2.0f,
    )

    // === 12. STICKY NOTE / STICKERS (Hugeicons book-open-01) ===
    private const val BOOK_LEFT_PAGE =
        "M7.99978 3.5H6.60021C4.43183 3.5 3.34764 3.5 2.67399 4.17362C2.00034 4.84724 2.00029 5.93144 2.00021 8.09982L2 13.3998C1.99992 15.5684 1.99987 16.6526 2.67353 17.3263C3.34719 18 4.43146 18 6.6 18H8.95042C10.4329 18 11.7092 19.0464 11.9999 20.5V5.5C11.0556 4.24097 9.99989 3.5 7.99978 3.5Z"
    private const val BOOK_RIGHT_PAGE =
        "M16.0001 3.5H17.3997C19.5681 3.5 20.6523 3.5 21.3259 4.17362C21.9996 4.84724 21.9996 5.93144 21.9997 8.09982L21.9999 13.3998C22 15.5684 22 16.6526 21.3264 17.3263C20.6527 18 19.5684 18 17.3999 18H15.0495C13.567 18 12.2907 19.0464 12 20.5V5.5C12.9443 4.24097 14 3.5 16.0001 3.5Z"

    val stickyNote = strokeIcon(1.5f, BOOK_LEFT_PAGE, BOOK_RIGHT_PAGE)
    val stickyNoteFilled = filledIcon(BOOK_LEFT_PAGE, BOOK_RIGHT_PAGE)

    // === 13. UNDO & REDO (Hugeicons undo-03 & redo-01) ===
    val undo = strokeIcon(
        1.5f,
        "M3 8H15C18.3137 8 21 10.6863 21 14C21 17.3137 18.3137 20 15 20H11",
        "M7 4L5.8462 4.87652C3.94873 6.31801 3 7.03875 3 8C3 8.96125 3.94873 9.68199 5.8462 11.1235L7 12",
    )
    val redo = strokeIcon(
        1.5f,
        "M20.9922 8H8.99219C5.67848 8 2.99219 10.6863 2.99219 14C2.99219 17.3137 5.67848 20 8.99219 20H12.9922",
        "M16.9922 4L18.146 4.87652C20.0435 6.31801 20.9922 7.03875 20.9922 8C20.9922 8.96125 20.0435 9.68199 18.146 11.1235L16.9922 12",
    )

    // === 14. LASSO SELECT (Hugeicons lasso-select-stroke-rounded) ===
    private const val LASSO_POINTER =
        "M16.4014 12.8149L19.4182 13.9953C21.1582 14.6762 22.0282 15.0166 21.9993 15.5567C21.9705 16.0967 21.0625 16.3443 19.2466 16.8395L19.2466 16.8395C18.7059 16.987 18.4356 17.0607 18.2482 17.2482C18.0607 17.4356 17.987 17.7059 17.8395 18.2466L17.8395 18.2466C17.3443 20.0625 17.0967 20.9705 16.5567 20.9993C16.0166 21.0282 15.6762 20.1582 14.9953 18.4182L13.8149 15.4014C13.102 13.5797 12.7456 12.6688 13.2072 12.2072C13.6688 11.7456 14.5797 12.102 16.4014 12.8149Z"
    private const val LASSO_LOOP =
        "M5 17C3.89543 17 3 16.1046 3 15C3 13.8954 3.89543 13 5 13C6.10457 13 7 13.8954 7 15C7 16.1046 6.10457 17 5 17ZM5 17C5 18.6 6.33333 19.6667 7 20M21.8987 11C21.9655 10.6734 22 10.3395 22 10C22 6.13401 17.5228 3 12 3C6.47715 3 2 6.13401 2 10C2 11.3199 2.52185 12.5544 3.42901 13.6081M6.84631 16C7.80456 16.4042 8.86753 16.6991 10 16.86"

    val lasso = strokeIcon(1.5f, LASSO_POINTER, LASSO_LOOP)
    val lassoFilled = combinedIcon(
        strokePaths = listOf(LASSO_LOOP),
        fillPaths = listOf(LASSO_POINTER),
        strokeWidth = 1.8f,
    )

    // === 15. MAGNET / SNAPPING (Hugeicons magnet-stroke-rounded) ===
    private const val MAGNET_BODY =
        "M3.925 2.5H9.15C9.15 2.5 8.2 7.7 8.2 10.1C8.2 13.1 10.1 15.5 12 15.5C13.9 15.5 15.8 13.1 15.8 10.1C15.8 7.7 14.85 2.5 14.85 2.5H20.075C20.075 2.5 21.5 7.13842 21.5 11.5C21.5 17 17.7 21.5 12 21.5C6.3 21.5 2.5 17 2.5 11.5C2.5 7.13842 3.925 2.5 3.925 2.5Z"
    private const val MAGNET_POLES = "M2.97266 7.25H8.19766M15.7977 7.25L21.0227 7.25"

    val magnet = strokeIcon(1.5f, MAGNET_BODY, MAGNET_POLES)
    val magnetFilled = combinedIcon(
        strokePaths = listOf(MAGNET_POLES),
        fillPaths = listOf(MAGNET_BODY),
        strokeWidth = 1.5f,
    )

    // === 16. TABLE (Star Notes 3x3 Table with Header Separators) ===
    private const val TABLE_OUTER_PATH =
        "M4 4H20C20.5523 4 21 4.44772 21 5V19C21 19.5523 20.5523 20 20 20H4C3.44772 20 3 19.5523 3 19V5C3 4.44772 3.44772 4 4 4Z"
    private const val TABLE_DIVIDERS_PATH =
        "M3 9H21M3 15H21M9 4V20M15 4V20"
    private const val TABLE_HEADER_FILL_PATH =
        "M4 4H20C20.5523 4 21 4.44772 21 5V9H3V5C3 4.44772 3.44772 4 4 4Z"

    val table = strokeIcon(1.5f, TABLE_OUTER_PATH, TABLE_DIVIDERS_PATH)
    val tableFilled = combinedIcon(
        strokePaths = listOf(TABLE_OUTER_PATH, TABLE_DIVIDERS_PATH),
        fillPaths = listOf(TABLE_HEADER_FILL_PATH),
        strokeWidth = 1.5f,
    )

    // === TABLE OVERLAY HANDLES (Star Notes handle icons) ===
    val tableSelectAll = combinedIcon(
        strokePaths = listOf("M5 9.5H15M5 13.5H15"),
        fillPaths = listOf("M3 6C3 4.34315 4.34315 3 6 3H14C15.6569 3 17 4.34315 17 6V17C17 18.6569 15.6569 20 14 20H6C4.34315 20 3 18.6569 3 17V6Z"),
        strokeWidth = 1.8f,
    )

    val tableIncreaseRow = strokeIcon(
        1.8f,
        "M6 7C6 4.79086 7.79086 3 10 3H14C16.2091 3 18 4.79086 18 7V17C18 19.2091 16.2091 21 14 21H10C7.79086 21 6 19.2091 6 17V7Z",
        "M12 8V16M8 12H16",
    )

    val tableIncreaseCol = strokeIcon(
        1.8f,
        "M7 6C4.79086 6 3 7.79086 3 10V14C3 16.2091 4.79086 18 7 18H17C19.2091 18 21 16.2091 21 14V10C21 7.79086 19.2091 6 17 6H7Z",
        "M12 8V16M8 12H16",
    )

    // === 17. PAGE STYLES & FILTERS (Hugeicons filter-vertical-stroke-rounded) ===
    val pageStyles = strokeIcon(
        1.5f,
        "M7 21L7 18", "M17 21L17 15", "M17 6L17 3", "M7 9L7 3",
        "M7 18C6.06812 18 5.60218 18 5.23463 17.8478C4.74458 17.6448 4.35523 17.2554 4.15224 16.7654C4 16.3978 4 15.9319 4 15C4 14.0681 4 13.6022 4.15224 13.2346C4.35523 12.7446 4.74458 12.3552 5.23463 12.1522C5.60218 12 6.06812 12 7 12C7.93188 12 8.39782 12 8.76537 12.1522C9.25542 12.3552 9.64477 12.7446 9.84776 13.2346C10 13.6022 10 14.0681 10 15C10 15.9319 10 16.3978 9.84776 16.7654C9.64477 17.2554 9.25542 17.6448 8.76537 17.8478C8.39782 18 7.93188 18 7 18Z",
        "M17 12C16.0681 12 15.6022 12 15.2346 11.8478C14.7446 11.6448 14.3552 11.2554 14.1522 10.7654C14 10.3978 14 9.93188 14 9C14 8.06812 14 7.60218 14.1522 7.23463C14.3552 6.74458 14.7446 6.35523 15.2346 6.15224C15.6022 6 16.0681 6 17 6C17.9319 6 18.3978 6 18.7654 6.15224C19.2554 6.35523 19.6448 6.74458 19.8478 7.23463C20 7.60218 20 8.06812 20 9C20 9.93188 20 10.3978 19.8478 10.7654C19.6448 11.2554 19.2554 11.6448 18.7654 11.8478C18.3978 12 17.9319 12 17 12Z",
    )

    // === 18. MORE MENU (Hugeicons ellipsis-vertical-stroke-rounded) ===
    val more = strokeIcon(
        2.0f,
        "M12 12V11.5M12 5V4.5M12 19V18.5M13 12C13 11.4477 12.5523 11 12 11C11.4477 11 11 11.4477 11 12C11 12.5523 11.4477 13 12 13C12.5523 13 13 12.5523 13 12ZM13 5C13 4.44772 12.5523 4 12 4C11.4477 4 11 4.44772 11 5C11 5.55228 11.4477 6 12 6C12.5523 6 13 5.55228 13 5ZM13 19C13 18.4477 12.5523 18 12 18C11.4477 18 11 18.4477 11 19C11 19.5523 11.4477 20 12 20C12.5523 20 13 19.5523 13 19Z",
    )
    val moreHorizontal = strokeIcon(1.5f, circle(5.0, 12.0, 1.5), circle(12.0, 12.0, 1.5), circle(19.0, 12.0, 1.5))

    // === 19. MAGIC WAND (Dedicated Magic Wand / Laser Wand) ===
    val magicWand = strokeIcon(
        2.0f,
        "M13.0001 14L10.0001 11M15.0104 3.5V2M18.9498 5.06066L20.0104 4M18.9498 13L20.0104 14.0607M11.0104 5.06066L9.94979 4M20.5104 9H22.0104M6.13146 20.8686L15.3687 11.6314C15.7647 11.2354 15.9627 11.0373 16.0369 10.809C16.1022 10.6082 16.1022 10.3918 16.0369 10.191C15.9627 9.96265 15.7647 9.76465 15.3687 9.36863L14.6315 8.63137C14.2354 8.23535 14.0374 8.03735 13.8091 7.96316C13.6083 7.8979 13.3919 7.8979 13.1911 7.96316C12.9627 8.03735 12.7647 8.23535 12.3687 8.63137L3.13146 17.8686C2.73545 18.2646 2.53744 18.4627 2.46325 18.691C2.39799 18.8918 2.39799 19.1082 2.46325 19.309C2.53744 19.5373 2.73545 19.7354 3.13146 20.1314L3.86872 20.8686C4.26474 21.2646 4.46275 21.4627 4.69108 21.5368C4.89192 21.6021 5.10827 21.6021 5.30911 21.5368C5.53744 21.4627 5.73545 21.2646 6.13146 20.8686Z",
    )
    val magicWandFilled = magicWand

    // === 20. CONNECTORS ===
    val connectorElbow = strokeIcon(1.5f, "M5 19h7v-14h7")
    val connectorCurved = strokeIcon(1.5f, "M5 19c6 0 8-14 14-14")

    // === 21. NAVIGATION & PILL BUTTONS (Hugeicons circle-arrow-left-01 & circle-arrow-right-01) ===
    val circleArrowLeft = strokeIcon(
        1.5f,
        circle(12.0, 12.0, 10.0),
        "M13.5 16C13.5 16 10.5 13.054 10.5 12C10.5 10.946 13.5 8 13.5 8",
    )
    val circleArrowRight = strokeIcon(
        1.5f,
        circle(12.0, 12.0, 10.0),
        "M10.5 8C10.5 8 13.5 10.946 13.5 12C13.5 13.0541 10.5 16 10.5 16",
    )
    val pillChevronLeft = strokeIcon(1.8f, "M15 4.5 L8 12 L15 19.5")
    val pillChevronRight = strokeIcon(1.8f, "M9 4.5 L16 12 L9 19.5")
    val pillMenu = strokeIcon(1.8f, "M4.5 5.5 L19.5 5.5", "M4.5 12 L19.5 12", "M4.5 18.5 L19.5 18.5")
    val unmerge = strokeIcon(1.8f, "M8 7 L3 12 L8 17", "M16 7 L21 12 L16 17")

    // === 22. GENERAL UTILITY & FLOW BAR ICONS ===
    val bold = strokeIcon(1.5f, "M6 12h9a4 4 0 0 1 0 8H7a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h7a4 4 0 0 1 0 8")
    val italic = strokeIcon(1.5f, "M19 4h-9", "M14 20H5", "M15 4 9 20")
    val underline = strokeIcon(1.5f, "M6 4v6a6 6 0 0 0 12 0V4", "M4 20h16")
    val strikethrough = strokeIcon(1.5f, "M16 4H9a3 3 0 0 0-2.83 4", "M14 12a4 4 0 0 1 0 8H6", "M4 12h16")
    val listBullet = strokeIcon(1.5f, "M3 12h.01", "M3 18h.01", "M3 6h.01", "M8 12h13", "M8 18h13", "M8 6h13")
    val listOrdered = strokeIcon(1.5f, "M10 12h11", "M10 18h11", "M10 6h11", "M4 10h2", "M4 6h1v4", "M6 18H4c0-1 2-2 2-3s-1-1.5-2-1")
    val checkboxItem = strokeIcon(1.5f, rect(4.0, 4.0, 16.0, 16.0), "m9 12 2 2 4-4")
    val alignLeft = strokeIcon(1.5f, "M15 12H3", "M17 18H3", "M21 6H3")
    val alignCenter = strokeIcon(1.5f, "M17 12H7", "M19 18H5", "M21 6H3")
    val alignRight = strokeIcon(1.5f, "M21 12H9", "M21 18H7", "M21 6H3")
    val alignJustify = strokeIcon(1.5f, "M3 12h18", "M3 18h18", "M3 6h18")
    val indentIncrease = strokeIcon(1.5f, "M21 12H11", "M21 18H11", "M21 6H11", "m3 8 4 4-4 4")
    val indentDecrease = strokeIcon(1.5f, "M21 12H11", "M21 18H11", "M21 6H11", "m7 8-4 4 4 4")
    val scissors = strokeIcon(1.5f, circle(6.0, 6.0, 3.0), circle(6.0, 18.0, 3.0), "M20 4 8.12 15.88", "M14.47 14.48 20 20", "M8.12 8.12 12 12")
    val screenshot = strokeIcon(
        1.5f,
        "M6 2v14a2 2 0 0 0 2 2h14",
        "M18 22V8a2 2 0 0 0-2-2H2",
    )
    val zoomIn = strokeIcon(1.5f, circle(11.0, 11.0, 8.0), "M21 21l-4.35-4.35", "M11 8v6", "M8 11h6")
    val zoomOut = strokeIcon(1.5f, circle(11.0, 11.0, 8.0), "M21 21l-4.35-4.35", "M8 11h6")
    val fit = strokeIcon(1.5f, "M8 3H5a2 2 0 0 0-2 2v3", "M21 8V5a2 2 0 0 0-2-2h-3", "M3 16v3a2 2 0 0 0 2 2h3", "M16 21h3a2 2 0 0 0 2-2v-3")
    val page = strokeIcon(1.5f, rect(3.0, 3.0, 18.0, 18.0), "M12 8v8", "M8 12h8")
    val canvas = strokeIcon(1.5f, rect(3.0, 3.0, 18.0, 18.0), "M3 9h18", "M3 15h18", "M9 3v18", "M15 3v18")
    val map = strokeIcon(1.5f, "M21 9V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h4", roundRect(12.0, 13.0, 10.0, 7.0, 2.0))
    val mapFilled = combinedIcon(
        strokePaths = listOf("M21 9V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h4"),
        fillPaths = listOf(roundRect(12.0, 13.0, 10.0, 7.0, 2.0)),
        strokeWidth = 1.5f,
    )
    val prev = strokeIcon(2.4f, "M15 18l-6-6 6-6")
    val next = strokeIcon(2.4f, "M9 18l6-6-6-6")
    val file = strokeIcon(1.5f, "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z", "M14 2v6h6")
    val edit = strokeIcon(1.5f, "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7", "M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z")
    val sidebar = strokeIcon(1.5f, rect(3.0, 3.0, 18.0, 18.0), "M9 3v18")
    val fullscreen = strokeIcon(1.5f, "M15 3h6v6", "M9 21H3v-6", "M21 3l-7 7", "M3 21l7-7")
    val present = strokeIcon(1.5f, "M2 8V6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6", "M2 12a9 9 0 0 1 8 8", "M2 16a5 5 0 0 1 4 4", "M2 20h.01")
    val plus = strokeIcon(2.4f, "M12 5v14", "M5 12h14")
    val minus = strokeIcon(2.4f, "M5 12h14")
    val fontFace = strokeIcon(1.5f, "M12 4v16", "M4 7V5a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v2", "M9 20h6")
    val trash = strokeIcon(1.5f, "M3 6h18", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2")
    val close = strokeIcon(2.2f, "M18 6 6 18", "M6 6l12 12")
    val bookmark = strokeIcon(1.5f, "M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2Z")
    val bookmarkFilled = filledIcon("M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2Z")
    val contents = strokeIcon(1.5f, "M8 6h13", "M8 12h13", "M8 18h13", "M3 6h.01", "M3 12h.01", "M3 18h.01")
    val thumbnails = strokeIcon(1.5f, rect(3.0, 3.0, 7.0, 7.0), rect(14.0, 3.0, 7.0, 7.0), rect(3.0, 14.0, 7.0, 7.0), rect(14.0, 14.0, 7.0, 7.0))
    val lock = strokeIcon(1.5f, rect(3.0, 11.0, 18.0, 11.0), "M7 11V7a5 5 0 0 1 10 0v4")
    val lockFilled = combinedIcon(
        strokePaths = listOf("M7 11V7a5 5 0 0 1 10 0v4"),
        fillPaths = listOf(rect(3.0, 11.0, 18.0, 11.0)),
        strokeWidth = 1.5f,
    )
    val unlock = strokeIcon(1.5f, rect(3.0, 11.0, 18.0, 11.0), "M7 11V7a5 5 0 0 1 9.9-1")
    private const val COPY_BACK_SHEET = "M4 16a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2"
    val copy = strokeIcon(1.5f, roundRect(8.0, 8.0, 14.0, 14.0, 2.0), COPY_BACK_SHEET)
    val cut = scissors
    val duplicate = strokeIcon(1.5f, roundRect(8.0, 8.0, 14.0, 14.0, 2.0), COPY_BACK_SHEET, "M15 12v6", "M12 15h6")
    val front = strokeIcon(1.5f, "M12 2.5 22 7.5 12 12.5 2 7.5Z", "M22 12.5 12 17.5 2 12.5", "M22 17.5 12 22.5 2 17.5")
    val split = strokeIcon(1.5f, rect(3.0, 3.0, 18.0, 18.0), "M12 3v18")
    val folder = strokeIcon(1.5f, "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2Z")
    val save = strokeIcon(1.5f, "M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2Z", "M17 21v-8H7v8", "M7 3v5h8")
    val database = strokeIcon(1.5f, ellipse(12.0, 5.0, 9.0, 3.0), "M3 5V19A9 3 0 0 0 21 19V5", "M3 12A9 3 0 0 0 21 12")
    val importDoc = strokeIcon(1.5f, "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "M7 10l5 5 5-5", "M12 15V3")
    val exportDoc = strokeIcon(1.5f, "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "M17 8l-5-5-5 5", "M12 3v12")
    val download = importDoc
    val sliders = strokeIcon(1.5f, "M4 21v-7", "M4 10V3", "M12 21v-9", "M12 8V3", "M20 21v-5", "M20 12V3", "M1 14h6", "M9 8h6", "M17 16h6")
    val margins = strokeIcon(1.5f, rect(3.0, 3.0, 18.0, 18.0), "M16 3v13", "M3 16h13")
    val view = strokeIcon(1.5f, "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z", circle(12.0, 12.0, 3.0))
    val home = strokeIcon(1.5f, "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z", "M9 22V12h6v10")
    val share = strokeIcon(1.5f, "M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8", "M16 6l-4-4-4 4", "M12 2v13")
    val check = strokeIcon(1.5f, "M20 6 9 17l-5-5")
    val arrowUp = strokeIcon(1.5f, "M12 19V5", "M5 12l7-7 7 7")
    val arrowDown = strokeIcon(1.5f, "M12 5v14", "M19 12l-7 7-7-7")
    val newFolder = strokeIcon(1.5f, "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2Z", "M12 11v6", "M9 14h6")
    val paste = strokeIcon(1.5f, "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2", "M9 2h6a1 1 0 0 1 1 1v1a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1Z")
    val menu = strokeIcon(1.5f, "M4 6h16", "M4 12h16", "M4 18h16")
    val search = strokeIcon(1.5f, circle(11.0, 11.0, 8.0), "M21 21l-4.35-4.35")
    val settings = strokeIcon(
        1.5f,
        circle(12.0, 12.0, 3.0),
        "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"
    )
    val contrast = strokeIcon(
        1.5f,
        circle(12.0, 12.0, 10.0),
        "M12 2v20a10 10 0 0 0 0-20"
    )
    val blur = strokeIcon(
        1.5f,
        circle(12.0, 12.0, 10.0),
        "M12 2v20",
        "M16 4v16",
        "M20 7v10",
        "M8 4v16",
        "M4 7v10"
    )
    val blurOff = strokeIcon(
        1.5f,
        "M12 2.69l5.66 5.66a8 8 0 0 1 1.95 3.09",
        "M4.93 4.93l14.14 14.14",
        "M6.34 8.35A8 8 0 0 0 12 22a8 8 0 0 0 5.66-2.34"
    )
    val info = strokeIcon(1.5f, circle(12.0, 12.0, 10.0), "M12 16v-4", "M12 8h.01")
    val bug = strokeIcon(
        1.5f,
        "M8 2l1.88 1.88", "M14.12 3.88 16 2",
        "M9 7.13v-1a3.003 3.003 0 1 1 6 0v1",
        "M12 20c-3.3 0-6-2.7-6-6v-3a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v3c0 3.3-2.7 6-6 6",
        "M12 20v-9", "M6.53 9C4.6 8.8 3 7.1 3 5", "M6 13H2",
        "M3 21c0-2.1 1.7-3.9 3.8-4", "M20.97 5c0 2.1-1.6 3.8-3.5 4",
        "M22 13h-4", "M17.2 17c2.1.1 3.8 1.9 3.8 4",
    )
    val idea = strokeIcon(
        1.5f,
        "M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5",
        "M9 18h6", "M10 22h4",
    )
    val github = strokeIcon(
        1.5f,
        "M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22",
    )
    val heart = strokeIcon(
        1.5f,
        "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78Z",
    )
    val arrowLeft = strokeIcon(1.5f, "M19 12H5", "M12 19l-7-7 7-7")
    val user = strokeIcon(1.5f, circle(12.0, 8.0, 4.0), "M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2")
    val grid = strokeIcon(1.5f, rect(3.0, 3.0, 7.0, 7.0), rect(14.0, 3.0, 7.0, 7.0), rect(3.0, 14.0, 7.0, 7.0), rect(14.0, 14.0, 7.0, 7.0))
    val chevronDown = strokeIcon(2.2f, "M6 9l6 6 6-6")
    val chevronUp = strokeIcon(2.2f, "M18 15l-6-6-6 6")
    val cursorPointer = strokeIcon(1.5f, "M3 3l7.07 16.97 2.51-7.39 7.39-2.51Z")
    val hand = pan
    val star = strokeIcon(1.5f, "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z")
    val starFilled = filledIcon("M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z")
    val clock = strokeIcon(1.5f, circle(12.0, 12.0, 10.0), "M12 6v6l4 2")
    val portrait = strokeIcon(1.5f, roundRect(6.0, 3.0, 12.0, 18.0, 2.0))
    val landscape = strokeIcon(1.5f, roundRect(3.0, 6.0, 18.0, 12.0, 2.0))
    val palette = strokeIcon(
        1.5f,
        "M12 2C6.48 2 2 6.48 2 12c0 4.42 3.58 8 8 8 1.1 0 2-.9 2-2 0-.55-.22-1.05-.59-1.41-.36-.36-.59-.86-.59-1.41 0-1.1.9-2 2-2h2.41c3.08 0 5.59-2.51 5.59-5.59C21.41 5.06 17.18 2 12 2Z",
        circle(6.5, 11.5, 1.0),
        circle(9.5, 7.5, 1.0),
        circle(14.5, 7.5, 1.0),
        circle(17.5, 11.5, 1.0),
    )
    val paletteFilled = combinedIcon(
        strokePaths = listOf(circle(6.5, 11.5, 1.0), circle(9.5, 7.5, 1.0), circle(14.5, 7.5, 1.0), circle(17.5, 11.5, 1.0)),
        fillPaths = listOf("M12 2C6.48 2 2 6.48 2 12c0 4.42 3.58 8 8 8 1.1 0 2-.9 2-2 0-.55-.22-1.05-.59-1.41-.36-.36-.59-.86-.59-1.41 0-1.1.9-2 2-2h2.41c3.08 0 5.59-2.51 5.59-5.59C21.41 5.06 17.18 2 12 2Z"),
        strokeWidth = 1.5f,
    )
    val singleStickPen = strokeIcon(
        2.0f,
        "M17.5 3.5a2.121 2.121 0 0 1 3 3L8 19l-4.5 1 1-4.5L17.5 3.5z",
        "M14.5 6.5l3 3",
    )

    // === 23. BLUEPRINT EXACT COMPONENT ICONS ===
    val cards = strokeIcon(
        1.5f,
        "M7 3h12a2 2 0 0 1 2 2v10",
        roundRect(3.0, 7.0, 14.0, 14.0, 2.0),
    )
    val sidebarSimple = strokeIcon(
        1.5f,
        roundRect(3.0, 3.0, 18.0, 18.0, 2.0),
        "M15 3v18",
    )
    val crosshair = strokeIcon(
        1.5f,
        circle(12.0, 12.0, 8.0),
        "M12 2v4", "M12 18v4", "M2 12h4", "M18 12h4",
        circle(12.0, 12.0, 1.5),
    )
    val scribbleLoop = strokeIcon(
        1.5f,
        "M4 14c1.5-3 3-4 5-4s3 2 4 4c1 2 2 4 4 4s3-1 4-4",
    )
    val selectionBackground = strokeIcon(
        1.5f,
        roundRect(4.0, 4.0, 16.0, 16.0, 2.0),
        "M4 10h16", "M10 4v16",
    )
    val paintBucket = strokeIcon(
        1.5f,
        "M19 11L13 5L4 14c-1 1-1 3 0 4s3 1 4 0l9-9",
        "M19 18c0 1.1-.9 2-2 2s-2-.9-2-2c0-1.5 2-4 2-4s2 2.5 2 4z",
    )
    val arrowsHorizontal = strokeIcon(
        1.5f,
        "M8 7L3 12l5 5", "M16 7l5 5-5 5", "M3 12h18",
    )
    val exportShare = strokeIcon(
        1.5f,
        "M4 13v6a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-6",
        "M12 15V3", "M8 7l4-4 4 4",
    )

    // === PNotes Prototype Brand & Layout Icons ===
    val pnotesPenNib: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 36f,
            viewportHeight = 36f,
            pathData = arrayOf(
                "M25.85,16.95 C25.86,16.96 25.85,16.98 25.86,16.99 C26.08,17.44 26.04,17.97 25.74,18.37 L25.73,18.38 L18.53,27.74 C18.4,27.9 18.21,28 18,28 C17.79,28 17.6,27.9 17.47,27.74 L10.27,18.38 L10.26,18.37 C9.96,17.97 9.92,17.44 10.14,16.99 C10.15,16.98 10.14,16.96 10.15,16.95 L12.67,12.49 L12.67,9.33 C12.67,8.59 13.26,8 14,8 L22,8 C22.74,8 23.33,8.59 23.33,9.33 L23.33,12.49 L25.85,16.95 Z M22,9.33 L22,12 L14,12 L14,9.33 L22,9.33 Z M18,18.66 C17.45,18.66 17,18.21 17,17.66 C17,17.11 17.45,16.66 18,16.66 C18.55,16.66 19,17.11 19,17.66 C19,18.21 18.55,18.66 18,18.66 Z M18.67,25.37 L18.67,19.9 C19.78,19.57 20.48,18.47 20.31,17.32 C20.14,16.17 19.16,15.33 18,15.33 C16.84,15.33 15.86,16.17 15.69,17.32 C15.52,18.47 16.22,19.57 17.33,19.9 L17.33,25.37 L11.33,17.58 L13.72,13.33 L22.28,13.33 L24.67,17.58 L18.67,25.37 Z"
            )
        )
    }

    val quickInkingPen: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 24f,
            viewportHeight = 24f,
            translationX = -16f,
            translationY = -12f,
            pathData = arrayOf(
                "M35.65,21.44 C35.87,21.66 36,21.97 36,22.28 C36,22.59 35.87,22.9 35.65,23.12 L33.55,25.23 L31.93,29.52 C31.78,29.92 31.44,30.21 31.02,30.28 L20.69,32 C20.66,32.01 20.62,32 20.59,32 C20.42,32 20.25,31.93 20.14,31.8 C20.03,31.67 19.98,31.48 20.01,31.31 L21.72,20.99 C21.79,20.57 22.09,20.22 22.49,20.07 L26.78,18.46 L28.89,16.35 C29.35,15.89 30.11,15.89 30.57,16.35 L35.65,21.44 Z M30.82,29.11 L22.32,30.52 L25.83,27.01 C26.74,27.5 27.85,27.26 28.47,26.43 C29.09,25.6 29.01,24.45 28.28,23.72 C27.55,22.99 26.41,22.91 25.58,23.53 C24.75,24.15 24.5,25.27 24.99,26.18 L21.47,29.69 L22.9,21.18 L26.96,19.66 L32.35,25.04 L30.82,29.11 Z M25.93,25.19 C25.93,24.7 26.31,24.3 26.8,24.3 C27.29,24.3 27.69,24.7 27.69,25.19 C27.69,25.68 27.29,26.08 26.8,26.08 C26.31,26.08 25.93,25.68 25.93,25.19 Z M33.04,24.06 L27.94,18.97 L29.72,17.19 L34.81,22.28 L33.04,24.06 Z"
            )
        )
    }

    val pnotesFolder: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 44f,
            viewportHeight = 44f,
            pathData = arrayOf(
                "M31.43,19.25 C31.94,19.25 32.41,19.49 32.71,19.9 C33,20.31 33.08,20.84 32.92,21.32 L30.12,29.71 C30.01,30.03 29.72,30.25 29.38,30.25 L11.79,30.25 C11.36,30.25 11,29.89 11,29.46 L11,15.32 C11,14.45 11.7,13.75 12.57,13.75 L17.81,13.75 C18.15,13.75 18.48,13.86 18.75,14.06 L21.48,16.11 L28.29,16.11 C29.16,16.11 29.86,16.81 29.86,17.68 L29.86,19.25 L31.43,19.25 Z M17.81,15.32 L20.74,17.52 C20.88,17.62 21.04,17.68 21.21,17.68 L28.29,17.68 L28.29,19.25 L15.5,19.25 C14.82,19.25 14.21,19.68 14,20.32 L12.57,24.62 L12.57,15.32 L17.81,15.32 Z M28.81,28.68 L12.88,28.68 L15.5,20.82 L31.43,20.82 L28.81,28.68 Z"
            )
        )
    }

    val layoutGrid = strokeIcon(
        1.5f,
        roundRect(3.0, 3.0, 7.0, 7.0, 1.5),
        roundRect(14.0, 3.0, 7.0, 7.0, 1.5),
        roundRect(3.0, 14.0, 7.0, 7.0, 1.5),
        roundRect(14.0, 14.0, 7.0, 7.0, 1.5),
    )

    val layoutList = strokeIcon(
        1.5f,
        "M4 6h16", "M4 12h16", "M4 18h16",
    )

    val chevronRight = strokeIcon(2.2f, "M9 18l6-6-6-6")
    val sortVertical = strokeIcon(1.5f, "M7 15l5 5 5-5", "M7 9l5-5 5 5")
    val moreVertical = filledIcon(circle(12.0, 12.0, 1.4), circle(12.0, 5.5, 1.4), circle(12.0, 18.5, 1.4))

    // === Page Angle & Orientation Popover Icons ===
    val pageAngleCompass: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 18f,
            viewportHeight = 18f,
            translationX = -6f,
            translationY = -6f,
            pathData = arrayOf(
                "M6,15 C6,10.03 10.03,6 15,6 C19.97,6.01 23.99,10.03 24,15 C24,19.97 19.97,24 15,24 C10.03,24 6,19.97 6,15 Z M15,22.62 C10.79,22.62 7.38,19.21 7.38,15 C7.38,10.79 10.79,7.38 15,7.38 C19.21,7.38 22.62,10.79 22.62,15 C22.61,19.2 19.2,22.61 15,22.62 Z",
                "M13.31,13 L18.84,10.23 C19.11,10.1 19.43,10.15 19.64,10.36 C19.85,10.57 19.9,10.89 19.77,11.16 L17,16.69 C16.93,16.82 16.82,16.93 16.69,17 L11.16,19.77 C11.06,19.82 10.96,19.85 10.85,19.85 C10.61,19.85 10.39,19.72 10.26,19.52 C10.13,19.32 10.12,19.05 10.23,18.84 L13,13.31 C13.07,13.18 13.18,13.07 13.31,13 Z M15.87,15.87 L12.39,17.61 L14.13,14.13 L17.61,12.4 L15.87,15.87 Z"
            )
        )
    }

    val angleReset: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 13f,
            viewportHeight = 13.01f,
            translationX = -12f,
            translationY = -8.49f,
            pathData = arrayOf(
                "M25,15 C25,18.56 22.36,21.45 19.08,21.5 L19,21.5 C17.47,21.5 16,20.87 14.88,19.73 C14.67,19.52 14.67,19.19 14.86,18.97 C15.05,18.75 15.35,18.73 15.56,18.94 C17.55,20.99 20.7,20.9 22.61,18.76 C24.52,16.62 24.46,13.21 22.5,11.13 C20.54,9.05 17.41,9.07 15.47,11.17 C15.47,11.18 15.46,11.18 15.45,11.18 L13.79,12.84 L15.5,12.84 C15.78,12.84 16,13.04 16,13.35 C16,13.66 15.78,13.91 15.5,13.91 L12.5,13.91 C12.22,13.91 12,13.66 12,13.35 L12,10.12 C12,9.81 12.22,9.58 12.5,9.58 C12.78,9.58 13,9.81 13,10.12 L13,12.14 L14.76,10.39 C16.48,8.53 19.07,7.98 21.31,8.99 C23.55,10 25,12.37 25,15 Z"
            )
        )
    }

    val handSlantRight: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 14f,
            viewportHeight = 13.99f,
            translationX = -14f,
            translationY = -11.01f,
            pathData = arrayOf(
                "M25.1,13.17 C25.37,13.06 25.66,13.01 25.96,13.01 C27.09,13.01 28,13.79 28,14.76 L28,19.51 C28,22.55 25.14,25 21.6,25 C19.79,25 18.53,24.6 17.45,23.65 C16.38,22.71 15.44,21.22 14.27,19.11 C13.72,18.27 14.05,17.21 15.03,16.73 C16.01,16.25 17.25,16.54 17.81,17.37 L18.1,17.76 L18.1,13.76 C18.1,13.14 18.46,12.57 19.08,12.25 C19.7,11.93 20.47,11.93 21.1,12.22 C21.4,11.4 22.37,10.9 23.36,11.03 C24.35,11.16 25.1,11.9 25.1,12.76 L25.1,13.17 Z M26.83,19.51 C26.83,21.99 24.49,24.01 21.6,24.01 C20.12,24.01 19.14,23.7 18.28,22.95 C17.31,22.11 16.44,20.7 15.32,18.67 C15.31,18.66 15.3,18.64 15.29,18.63 C15.05,18.27 15.19,17.82 15.61,17.61 C16.03,17.4 16.57,17.52 16.81,17.88 C16.81,17.88 16.81,17.89 16.81,17.89 L18.17,19.77 C18.31,19.96 18.59,20.05 18.83,19.99 C19.07,19.93 19.26,19.73 19.26,19.51 L19.26,13.76 C19.26,13.35 19.65,13.01 20.13,13.01 C20.61,13.01 21.01,13.35 21.01,13.76 L21.01,17.51 C21.01,17.79 21.27,18.01 21.6,18.01 C21.93,18.01 22.17,17.79 22.17,17.51 L22.17,12.76 C22.17,12.35 22.57,12.01 23.05,12.01 C23.53,12.01 23.92,12.35 23.92,12.76 L23.92,17.51 C23.92,17.79 24.17,18.01 24.5,18.01 C24.83,18.01 25.1,17.79 25.1,17.51 L25.1,14.76 C25.1,14.35 25.48,14.01 25.96,14.01 C26.44,14.01 26.83,14.35 26.83,14.76 L26.83,19.51 Z"
            )
        )
    }

    val handSlantLeft: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 13.99f,
            viewportHeight = 13.99f,
            translationX = -19.35f,
            translationY = -11.01f,
            pathData = arrayOf(
                "M30.43,13.17 C30.7,13.06 31,13.01 31.3,13.01 C32.43,13.01 33.34,13.79 33.34,14.76 L33.34,19.51 C33.34,22.55 30.47,25 26.93,25 C25.12,25 23.87,24.6 22.79,23.65 C21.72,22.71 20.78,21.22 19.62,19.11 C19.07,18.27 19.38,17.21 20.36,16.73 C21.34,16.25 22.59,16.54 23.15,17.37 L23.44,17.76 L23.44,13.76 C23.44,13.14 23.8,12.57 24.42,12.25 C25.04,11.93 25.81,11.93 26.44,12.22 C26.74,11.4 27.71,10.9 28.7,11.03 C29.69,11.16 30.43,11.9 30.43,12.76 L30.43,13.17 Z M32.18,19.51 C32.18,21.99 29.82,24.01 26.93,24.01 C25.45,24.01 24.49,23.7 23.63,22.95 C22.66,22.11 21.78,20.7 20.66,18.67 C20.65,18.66 20.64,18.64 20.63,18.63 C20.39,18.27 20.53,17.82 20.95,17.61 C21.37,17.4 21.91,17.52 22.15,17.88 C22.15,17.88 22.15,17.89 22.15,17.89 L23.51,19.77 C23.65,19.96 23.93,20.05 24.17,19.99 C24.41,19.93 24.6,19.73 24.6,19.51 L24.6,13.76 C24.6,13.35 25,13.01 25.48,13.01 C25.96,13.01 26.35,13.35 26.35,13.76 L26.35,17.51 C26.35,17.79 26.6,18.01 26.93,18.01 C27.26,18.01 27.52,17.79 27.52,17.51 L27.52,12.76 C27.52,12.35 27.91,12.01 28.39,12.01 C28.87,12.01 29.26,12.35 29.26,12.76 L29.26,17.51 C29.26,17.79 29.52,18.01 29.85,18.01 C30.18,18.01 30.43,17.79 30.43,17.51 L30.43,14.76 C30.43,14.35 30.82,14.01 31.3,14.01 C31.78,14.01 32.18,14.35 32.18,14.76 L32.18,19.51 Z"
            )
        )
    }

    val draftingAngle: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 14f,
            viewportHeight = 14f,
            translationX = -13.52f,
            translationY = -11f,
            pathData = arrayOf(
                "M23.93,11.29 L27.23,14.59 C27.62,14.98 27.62,15.61 27.23,16 L18.52,24.71 C18.13,25.1 17.5,25.1 17.11,24.71 L13.81,21.41 C13.42,21.02 13.42,20.39 13.81,20 L22.52,11.29 C22.91,10.9 23.54,10.9 23.93,11.29 Z M17.81,24 L14.52,20.71 L16.52,18.71 L18.17,20.35 C18.37,20.54 18.66,20.55 18.86,20.35 C19.06,20.15 19.05,19.85 18.86,19.65 L17.23,18 L18.52,16.71 L20.17,18.35 C20.37,18.54 20.67,18.55 20.87,18.35 C21.07,18.15 21.06,17.85 20.87,17.65 L19.23,16 L20.52,14.71 L22.17,16.35 C22.37,16.54 22.67,16.55 22.87,16.35 C23.07,16.15 23.06,15.85 22.87,15.65 L21.23,14 L23.23,12 L26.52,15.29 L17.81,24 Z"
            )
        )
    }

    val pageAngleCheck: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 15.01f,
            viewportHeight = 15f,
            translationX = -25.47f,
            translationY = -12.5f,
            pathData = arrayOf(
                "M40.3,13.93 L30.7,27.25 C30.58,27.41 30.43,27.5 30.27,27.5 C30.11,27.5 29.96,27.41 29.84,27.25 L25.65,21.4 C25.41,21.07 25.41,20.58 25.65,20.25 C25.89,19.92 26.26,19.92 26.5,20.25 L30.27,25.48 L39.44,12.75 C39.68,12.42 40.06,12.42 40.3,12.75 C40.54,13.08 40.54,13.6 40.3,13.93 Z"
            )
        )
    }

    // === EXACT PROTOTYPE ICONS (Matching prototype/toolbar SVGs 100%) ===
    val protoSelect: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 16f,
            viewportHeight = 16f,
            translationX = -7f,
            translationY = -7f,
            pathData = arrayOf(
                "M12.93,8.19 L12.93,7.59 C12.93,7.26 13.19,7 13.52,7 C13.85,7 14.11,7.26 14.11,7.59 L14.11,8.19 C14.11,8.52 13.85,8.78 13.52,8.78 C13.19,8.78 12.93,8.52 12.93,8.19 Z",
                "M8.19,14.11 L7.59,14.11 C7.26,14.11 7,13.85 7,13.52 C7,13.19 7.26,12.93 7.59,12.93 L8.19,12.93 C8.52,12.93 8.78,13.19 8.78,13.52 C8.78,13.85 8.52,14.11 8.19,14.11 Z",
                "M16.08,9.34 C15.93,9.39 15.76,9.38 15.62,9.31 C15.33,9.16 15.21,8.8 15.36,8.51 L15.95,7.33 C16.1,7.04 16.46,6.91 16.75,7.06 C17.04,7.21 17.16,7.57 17.01,7.86 L16.42,9.04 C16.35,9.18 16.23,9.29 16.08,9.34 Z",
                "M7.33,15.95 L8.51,15.36 C8.8,15.21 9.16,15.33 9.31,15.62 C9.46,15.91 9.33,16.27 9.04,16.42 L7.86,17.01 C7.57,17.16 7.21,17.04 7.06,16.75 C6.91,16.46 7.04,16.1 7.33,15.95 Z",
                "M22.65,20.04 C23.11,20.5 23.11,21.25 22.65,21.71 L21.71,22.65 C21.25,23.11 20.5,23.11 20.04,22.65 L16.24,18.85 L14.93,22.27 C14.93,22.28 14.92,22.28 14.92,22.29 C14.73,22.72 14.3,23 13.83,23 L13.78,23 C13.29,22.98 12.86,22.66 12.71,22.19 L8.84,10.33 C8.7,9.91 8.82,9.44 9.13,9.13 C9.44,8.82 9.91,8.7 10.33,8.84 L22.19,12.71 C22.65,12.86 22.97,13.29 22.99,13.78 C23.01,14.27 22.74,14.72 22.29,14.92 L22.27,14.93 L18.85,16.24 L22.65,20.04 Z M18.01,17.07 L21.81,20.87 L20.87,21.81 L17.07,18.01 C16.85,17.79 16.54,17.66 16.23,17.66 C16.16,17.66 16.08,17.67 16.01,17.68 C15.62,17.75 15.31,18.01 15.15,18.37 C15.15,18.38 15.14,18.39 15.14,18.4 L13.83,21.8 L9.96,9.96 L21.81,13.83 L18.4,15.14 L18.38,15.15 C18.02,15.31 17.76,15.62 17.69,16.01 C17.62,16.4 17.73,16.79 18.01,17.07 Z"
            )
        )
    }

    val protoEraser: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 16f,
            viewportHeight = 14.78f,
            translationX = -7f,
            translationY = -7.61f,
            pathData = arrayOf(
                "M19.28,8.15 L22.46,11.35 C23.18,12.07 23.18,13.24 22.46,13.96 L15.25,21.16 L21.77,21.16 C22.11,21.16 22.38,21.43 22.38,21.77 C22.38,22.11 22.11,22.39 21.77,22.39 L10.29,22.39 C10.13,22.39 9.97,22.33 9.85,22.21 L7.54,19.89 C6.82,19.17 6.82,18 7.54,17.28 L16.67,8.15 C17.39,7.43 18.56,7.43 19.28,8.15 Z M13.51,21.16 L10.54,21.16 L8.41,19.03 C8.17,18.79 8.17,18.4 8.41,18.16 L12.54,14.02 L16.59,18.07 L13.51,21.16 Z M21.59,13.08 L17.46,17.2 L13.41,13.16 L17.54,9.02 C17.66,8.9 17.81,8.85 17.97,8.85 C18.13,8.85 18.29,8.9 18.41,9.02 L21.59,12.22 C21.83,12.46 21.83,12.85 21.59,13.09 L21.59,13.08 Z"
            )
        )
    }

    val protoPen: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 15.99f,
            viewportHeight = 16f,
            translationX = -10f,
            translationY = -7f,
            pathData = arrayOf(
                "M17.9,8.39 C19.75,6.54 22.76,6.54 24.61,8.39 C25.5,9.28 25.99,10.49 25.99,11.75 C25.99,13.01 25.49,14.21 24.6,15.1 L19.48,20.29 C19.26,20.51 18.95,20.63 18.64,20.63 L13.2,20.63 L11.01,22.83 C10.78,23.06 10.4,23.06 10.17,22.83 C9.94,22.6 9.94,22.22 10.17,21.99 L12.37,19.8 L12.37,14.36 C12.37,14.05 12.49,13.74 12.71,13.52 L17.9,8.39 Z M18.74,9.23 C20.1,7.87 22.3,7.84 23.7,9.16 C25.1,10.48 25.18,12.69 23.9,14.12 L19.72,14.12 L22.27,11.57 C22.5,11.34 22.5,10.96 22.27,10.73 C22.04,10.5 21.66,10.5 21.43,10.73 L17.11,15.06 L17.11,10.85 L18.74,9.23 Z M13.55,14.36 L15.92,12.02 L15.92,16.24 L13.55,18.61 L13.55,14.36 Z M18.64,19.45 L14.39,19.45 L18.54,15.3 L22.74,15.3 L18.64,19.45 Z"
            )
        )
    }

    val protoBrush: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 15.98f,
            viewportHeight = 14.28f,
            translationX = -7.01f,
            translationY = -7.86f,
            pathData = arrayOf(
                "M22.43,7.86 C22.75,7.86 22.99,8.11 22.99,8.43 C22.99,11.58 19.46,14.81 17.11,16.6 C17.51,17.9 17.26,19.32 16.45,20.41 C15.64,21.5 14.37,22.14 13.01,22.14 L7.58,22.14 C7.33,22.14 7.1,21.98 7.03,21.74 C6.96,21.5 7.05,21.24 7.26,21.1 C7.33,21.05 8.73,20.07 8.73,17.86 C8.73,16.5 9.37,15.22 10.46,14.41 C11.55,13.6 12.97,13.36 14.27,13.76 C16.06,11.41 19.28,7.86 22.43,7.86 Z M13.01,21 L8.92,21 C9.4,20.31 9.86,19.28 9.86,17.86 C9.86,16.12 11.27,14.71 13.01,14.71 C14.75,14.71 16.16,16.12 16.16,17.86 C16.16,19.6 14.75,21 13.01,21 Z M15.31,14.25 C15.56,13.93 15.8,13.64 16.04,13.36 C16.62,13.75 17.1,14.24 17.49,14.82 C17.21,15.06 16.93,15.29 16.61,15.54 C16.28,15.02 15.83,14.58 15.31,14.25 Z M18.37,14.06 C17.94,13.45 17.41,12.92 16.8,12.49 C19.07,10.02 20.78,9.28 21.79,9.07 C21.59,10.08 20.84,11.79 18.37,14.06 Z"
            )
        )
    }

    val protoHighlighter: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 24f,
            viewportHeight = 24f,
            translationX = 0f,
            translationY = 0f,
            pathData = arrayOf(
                // Cap / Top barrel
                "M18.8,4.2 L19.8,5.2 C20.6,6.0 20.6,7.2 19.8,8.0 L18.0,9.8 L14.2,6.0 L16.0,4.2 C16.8,3.4 18.0,3.4 18.8,4.2 Z",
                // Main wide highlighter barrel
                "M13.2,7.0 L17.0,10.8 L10.5,17.3 L6.7,13.5 L13.2,7.0 Z",
                // Signature angled chisel nib
                "M5.7,14.5 L9.5,18.3 L5.5,20.0 L4.0,18.5 L5.7,14.5 Z"
            )
        )
    }

    val protoText: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 16f,
            viewportHeight = 16f,
            translationX = -7f,
            translationY = -7f,
            pathData = arrayOf(
                "M23,7.8 L23,11 C23,11.44 22.64,11.8 22.2,11.8 C21.76,11.8 21.4,11.44 21.4,11 L21.4,8.6 L15.8,8.6 L15.8,21.4 L18.2,21.4 C18.64,21.4 19,21.76 19,22.2 C19,22.64 18.64,23 18.2,23 L11.8,23 C11.36,23 11,22.64 11,22.2 C11,21.76 11.36,21.4 11.8,21.4 L14.2,21.4 L14.2,8.6 L8.6,8.6 L8.6,11 C8.6,11.44 8.24,11.8 7.8,11.8 C7.36,11.8 7,11.44 7,11 L7,7.8 C7,7.36 7.36,7 7.8,7 L22.2,7 C22.64,7 23,7.36 23,7.8 Z"
            )
        )
    }

    val protoImage: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 16f,
            viewportHeight = 13.54f,
            translationX = -7f,
            translationY = -8.23f,
            pathData = arrayOf(
                "M8.23,8.23 L21.77,8.23 C22.45,8.23 23,8.78 23,9.46 L23,20.54 C23,21.22 22.45,21.77 21.77,21.77 L8.23,21.77 C7.55,21.77 7,21.22 7,20.54 L7,9.46 C7,8.78 7.55,8.23 8.23,8.23 Z M21.77,9.46 L21.77,17.37 L19.76,15.36 C19.28,14.88 18.5,14.88 18.02,15.36 L16.48,16.9 L13.1,13.51 C12.62,13.03 11.84,13.03 11.36,13.51 L8.23,16.64 L8.23,9.46 L21.77,9.46 Z M8.23,18.38 L12.23,14.38 L18.38,20.54 L8.23,20.54 L8.23,18.38 Z M21.77,20.54 L20.13,20.54 L17.36,17.77 L18.89,16.23 L21.77,19.11 L21.77,20.54 Z",
                "M16.23,12.85 C16.23,12.34 16.64,11.92 17.15,11.92 C17.66,11.92 18.08,12.34 18.08,12.85 C18.08,13.36 17.66,13.77 17.15,13.77 C16.64,13.77 16.23,13.36 16.23,12.85 Z"
            )
        )
    }

    val protoMagicWand: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 24f,
            viewportHeight = 24f,
            translationX = 0f,
            translationY = 0f,
            pathData = arrayOf(
                // Wand stick
                "M19.7,4.3 C20.1,4.7 20.1,5.3 19.7,5.7 L6.7,18.7 C6.3,19.1 5.7,19.1 5.3,18.7 L4.3,17.7 C3.9,17.3 3.9,16.7 4.3,16.3 L17.3,3.3 C17.7,2.9 18.3,2.9 18.7,3.3 L19.7,4.3 Z",
                // Wand tip accent band
                "M14.5,4.5 L18.5,8.5 L17.0,10.0 L13.0,6.0 Z",
                // Star 1 (Top left sparkle)
                "M8.5,2.0 L9.2,4.3 L11.5,5.0 L9.2,5.7 L8.5,8.0 L7.8,5.7 L5.5,5.0 L7.8,4.3 Z",
                // Star 2 (Right sparkle)
                "M19.5,10.0 L20.0,11.8 L21.8,12.3 L20.0,12.8 L19.5,14.6 L19.0,12.8 L17.2,12.3 L19.0,11.8 Z",
                // Star 3 (Bottom sparkle)
                "M14.5,15.0 L15.1,16.9 L17.0,17.5 L15.1,18.1 L14.5,20.0 L13.9,18.1 L12.0,17.5 L13.9,16.9 Z"
            )
        )
    }

    val protoChevronDown: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 10.01f,
            viewportHeight = 5.46f,
            translationX = -54.1f,
            translationY = -11.27f,
            pathData = arrayOf(
                "M63.97,12.05 L59.42,16.6 C59.33,16.69 59.22,16.73 59.1,16.73 C58.98,16.73 58.87,16.69 58.78,16.6 L54.23,12.05 C54.05,11.87 54.05,11.59 54.23,11.41 C54.41,11.23 54.7,11.23 54.88,11.41 L59.1,15.63 L63.32,11.41 C63.5,11.23 63.79,11.23 63.97,11.41 C64.15,11.59 64.15,11.87 63.97,12.05 Z"
            )
        )
    }

    val protoPalette: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 14f,
            viewportHeight = 13.46f,
            translationX = -5f,
            translationY = -5.27f,
            pathData = arrayOf(
                "M12,5.27 C13.83,5.27 15.6,6 16.9,7.29 C18.23,8.59 18.99,10.36 19,12.22 C19,12.76 18.95,13.3 18.83,13.83 C18.61,14.81 17.74,15.51 16.73,15.51 L13.62,15.51 C13.03,15.51 12.54,15.99 12.54,16.58 C12.54,17.28 12.2,17.94 11.63,18.34 C11.06,18.74 10.33,18.84 9.67,18.61 C6.79,17.59 5,15.16 5,12.27 C5,8.43 8.09,5.31 11.93,5.27 L12,5.27 Z M17.77,13.59 C17.66,14.08 17.23,14.43 16.73,14.43 L13.62,14.43 C12.43,14.43 11.46,15.39 11.46,16.58 C11.46,16.93 11.29,17.26 11.01,17.46 C10.73,17.66 10.36,17.72 10.03,17.6 C7.59,16.74 6.08,14.69 6.08,12.27 C6.08,9.02 8.69,6.38 11.94,6.35 L12,6.35 C15.25,6.36 17.88,8.97 17.92,12.22 C17.92,12.68 17.88,13.14 17.78,13.59 L17.77,13.59 Z",
                "M12.81,8.77 C12.81,9.22 12.45,9.58 12,9.58 C11.55,9.58 11.19,9.22 11.19,8.77 C11.19,8.32 11.55,7.97 12,7.97 C12.45,7.97 12.81,8.32 12.81,8.77 Z",
                "M9.85,10.39 C9.85,10.84 9.49,11.2 9.04,11.2 C8.59,11.2 8.23,10.84 8.23,10.39 C8.23,9.94 8.59,9.58 9.04,9.58 C9.49,9.58 9.85,9.94 9.85,10.39 Z",
                "M9.85,14.16 C9.85,14.61 9.49,14.97 9.04,14.97 C8.59,14.97 8.23,14.61 8.23,14.16 C8.23,13.71 8.59,13.35 9.04,13.35 C9.49,13.35 9.85,13.71 9.85,14.16 Z",
                "M15.77,10.39 C15.77,10.84 15.41,11.2 14.96,11.2 C14.51,11.2 14.15,10.84 14.15,10.39 C14.15,9.94 14.51,9.58 14.96,9.58 C15.41,9.58 15.77,9.94 15.77,10.39 Z"
            )
        )
    }

    val protoUndo: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 15f,
            viewportHeight = 12.7f,
            translationX = -6.5f,
            translationY = -7.65f,
            pathData = arrayOf(
                "M21.5,15.73 C21.5,18.28 19.44,20.35 16.89,20.35 L10.54,20.35 C10.22,20.35 9.97,20.09 9.97,19.77 C9.97,19.45 10.22,19.19 10.54,19.19 L16.89,19.19 C18.8,19.19 20.35,17.64 20.35,15.73 C20.35,13.82 18.8,12.27 16.89,12.27 L8.48,12.27 L10.95,14.75 C11.17,14.98 11.18,15.33 10.95,15.56 C10.72,15.79 10.37,15.78 10.14,15.56 L6.67,12.1 C6.56,11.99 6.5,11.84 6.5,11.69 C6.5,11.54 6.56,11.39 6.67,11.28 L10.14,7.82 C10.37,7.6 10.72,7.59 10.95,7.82 C11.18,8.05 11.17,8.41 10.95,8.64 L8.48,11.12 L16.89,11.12 C19.44,11.12 21.5,13.18 21.5,15.73 Z"
            )
        )
    }

    val protoRedo: ImageVector by lazy {
        filledIconCustomViewport(
            viewportWidth = 15f,
            viewportHeight = 12.7f,
            translationX = -6.5f,
            translationY = -7.65f,
            pathData = arrayOf(
                "M17.06,14.75 L19.53,12.27 L11.12,12.27 C9.21,12.27 7.66,13.82 7.66,15.73 C7.66,17.64 9.21,19.19 11.12,19.19 L17.47,19.19 C17.79,19.19 18.04,19.45 18.04,19.77 C18.04,20.09 17.79,20.35 17.47,20.35 L11.12,20.35 C8.57,20.35 6.5,18.28 6.5,15.73 C6.5,13.18 8.57,11.12 11.12,11.12 L19.53,11.12 L17.06,8.64 C16.84,8.41 16.83,8.05 17.06,7.82 C17.29,7.59 17.64,7.6 17.87,7.82 L21.34,11.28 C21.45,11.39 21.5,11.54 21.5,11.69 C21.5,11.84 21.45,11.99 21.34,12.1 L17.87,15.56 C17.64,15.78 17.29,15.79 17.06,15.56 C16.83,15.33 16.84,14.98 17.06,14.75 Z"
            )
        )
    }
}
