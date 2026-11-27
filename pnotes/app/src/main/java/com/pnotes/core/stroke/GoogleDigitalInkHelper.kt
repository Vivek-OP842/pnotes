package com.pnotes.core.stroke

import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionResult
import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.tools.ShapeKind
import kotlin.math.max
import kotlin.math.min

/**
 * Google ML Kit Digital Ink Recognition integration for on-device shape and gesture classification.
 */
object GoogleDigitalInkHelper {

    private var recognizer: DigitalInkRecognizer? = null
    private var model: DigitalInkRecognitionModel? = null
    private var isDownloading = false

    init {
        try {
            val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-autodraw")
                ?: DigitalInkRecognitionModelIdentifier.fromLanguageTag("autodraw")
            if (identifier != null) {
                val m = DigitalInkRecognitionModel.builder(identifier).build()
                model = m
                recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(m).build())
                
                // Trigger background download of the model if not already present
                com.google.mlkit.common.model.RemoteModelManager.getInstance()
                    .download(m, com.google.mlkit.common.model.DownloadConditions.Builder().build())
            }
        } catch (_: Throwable) {
            // Ignored on initialization if native binary or mock test environment
        }
    }

    /**
     * Builds a Google ML Kit [Ink] structure from standard stroke [Sample]s.
     */
    fun buildInk(samples: List<Sample>): Ink {
        val inkBuilder = Ink.builder()
        val strokeBuilder = Ink.Stroke.builder()
        val t0 = if (samples.isNotEmpty()) (samples[0].t * 1000.0).toLong() else 0L
        for (i in samples.indices) {
            val s = samples[i]
            val t = if (s.t > 0) (s.t * 1000.0).toLong() else (t0 + i * 16L)
            strokeBuilder.addPoint(Ink.Point.create(s.pos.x.toFloat(), s.pos.y.toFloat(), t))
        }
        inkBuilder.addStroke(strokeBuilder.build())
        return inkBuilder.build()
    }

    /**
     * Asynchronously recognizes an ink drawing and passes the top candidates to [onResult].
     */
    fun recognizeShapeAsync(samples: List<Sample>, onResult: (RecognizedShape?) -> Unit) {
        val rec = recognizer ?: run {
            onResult(null)
            return
        }
        val ink = buildInk(samples)
        rec.recognize(ink)
            .addOnSuccessListener { result: RecognitionResult ->
                val topCandidate = result.candidates.firstOrNull()?.text?.lowercase()
                onResult(mapPredictionToShape(topCandidate, samples))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    private fun mapPredictionToShape(label: String?, samples: List<Sample>): RecognizedShape? {
        if (label == null) return null
        
        val points = samples.map { it.pos }
        if (points.size < 4) return null
        val bbox = Rect.bounding(points)
        val diag = kotlin.math.hypot(bbox.w, bbox.h)
        val closed = points.first().distanceTo(points.last()) <= 0.35 * diag
        
        return when {
            closed && (label.contains("circle") || label.contains("ellipse") || label.contains("oval")) -> {
                // Determine if it should snap to a perfect circle
                val longAxis = max(bbox.w, bbox.h)
                val shortAxis = min(bbox.w, bbox.h)
                val aspect = if (longAxis > 1e-9) shortAxis / longAxis else 1.0
                if (aspect > 0.78 || label.contains("circle")) {
                    val r = (bbox.w + bbox.h) / 4.0
                    val cx = bbox.centerX
                    val cy = bbox.centerY
                    RecognizedShape(ShapeKind.ELLIPSE, Pt(cx - r, cy - r), Pt(cx + r, cy + r))
                } else {
                    RecognizedShape(ShapeKind.ELLIPSE, bbox.topLeft, Pt(bbox.right, bbox.bottom))
                }
            }
            closed && (label.contains("rectangle") || label.contains("square") || label.contains("box")) -> {
                RecognizedShape(ShapeKind.RECTANGLE, bbox.topLeft, Pt(bbox.right, bbox.bottom))
            }
            label.contains("line") -> {
                val chord = points.first().distanceTo(points.last())
                val maxDev = if (chord > 1e-6) points.maxOf { com.pnotes.core.geometry.Geometry.distancePointToSegment(it, points.first(), points.last()) } else Double.MAX_VALUE
                if (chord > 1e-6 && maxDev / chord <= 0.16) {
                    val ends = ShapeRecognizer.snapAxisAligned(listOf(points.first(), points.last()), closed = false)
                    RecognizedShape(ShapeKind.LINE, ends[0], ends[1])
                } else {
                    ShapeRecognizer.recognizePoints(points)
                }
            }
            // For triangle/polygon, ML Kit doesn't provide corners, so we rely on our geometric heuristic
            label.contains("triangle") || label.contains("polygon") -> {
                ShapeRecognizer.recognizePoints(points)
            }
            else -> {
                ShapeRecognizer.recognizePoints(points)
            }
        }
    }
}
