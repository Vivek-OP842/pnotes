package com.pnotes.platform

import com.pnotes.core.geometry.Rect
import com.pnotes.core.pal.FontSpec
import com.pnotes.core.pal.LineMetrics
import com.pnotes.core.pal.TextFlags
import com.pnotes.core.pal.TextMeasurer

/** Measures text with the same layout pipeline the renderer draws with. */
class AndroidTextMeasurer : TextMeasurer {

    override fun measure(text: String, font: FontSpec, wrapWidth: Double, flags: TextFlags): Rect {
        val probe = text.ifEmpty { " " }
        val paint = AndroidText.textPaint(font)
        val innerWidth = (wrapWidth - 12.0).toInt().coerceAtLeast(1)
        val layout = AndroidText.layout(probe, innerWidth, paint)
        // Width is fixed to the wrap width; height follows the text + vertical padding.
        return Rect(0.0, 0.0, wrapWidth, layout.height.toDouble() + 8.0)
    }

    override fun lineHeight(font: FontSpec): Double = AndroidText.lineHeight(font)

    override fun metrics(font: FontSpec): LineMetrics {
        val fm = AndroidText.textPaint(font).fontMetrics
        // Paint ascent is negative (above baseline); expose both as positive distances.
        return LineMetrics(ascent = -fm.ascent.toDouble(), descent = fm.descent.toDouble())
    }

    override fun advances(text: String, font: FontSpec): DoubleArray {
        if (text.isEmpty()) return DoubleArray(0)
        val widths = FloatArray(text.length)
        AndroidText.textPaint(font).getTextWidths(text, widths)
        return DoubleArray(text.length) { widths[it].toDouble() }
    }
}
