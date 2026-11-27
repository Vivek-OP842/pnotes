package com.pnotes.core.inking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Conformance tests for the Kotlin port of the Star Notes native neo3pen engine:
 * SmoothFilter (smoothLevel stabilization), SplineProducer (Catmull-Rom -> cubic Bézier,
 * adaptive subdivision, arc-length resampling) and NeoPenStrokeEngine (full pipeline).
 */
class NeoPenEngineTest {

    private fun pt(x: Float, y: Float, pressure: Float = 0.5f, t: Long = 0L) =
        InkPoint(x = x, y = y, pressure = pressure, timestamp = t)

    // --- SmoothFilter ---

    @Test fun smoothFilter_firstSamplePassesThrough() {
        val f = SmoothFilter(0.5f)
        val out = f.filter(pt(10f, 20f), 0f)
        assertEquals(10f, out.x, 1e-4f)
        assertEquals(20f, out.y, 1e-4f)
    }

    @Test fun smoothFilter_deadZoneCollapsesJitter() {
        val f = SmoothFilter(0.8f) // large noise gate
        f.filter(pt(0f, 0f), 0f)
        val out = f.filter(pt(0.3f, 0f), 0.1f) // below gate (~1.68px)
        assertEquals(0f, out.x, 1e-4f)
        assertEquals(0f, out.y, 1e-4f)
    }

    @Test fun smoothFilter_convergesToTarget() {
        val f = SmoothFilter(0.4f)
        f.filter(pt(0f, 0f), 0f)
        var out = f.filter(pt(0f, 0f), 0f)
        repeat(200) { out = f.filter(pt(100f, 0f), 0.2f) }
        assertEquals(100f, out.x, 0.5f)
    }

    @Test fun smoothFilter_lagCompensationKeepsTipClose() {
        val f = SmoothFilter(0.9f)
        f.filter(pt(0f, 0f), 0f)
        // Steady fast diagonal motion: with lag compensation the tip must not trail far.
        var p = pt(0f, 0f)
        var t = 0L
        for (i in 1..200) {
            t += 8
            p = f.filter(pt(i * 2f, i * 2f, t = t), 0.25f * 4f)
        }
        val trail = hypot(p.x - 400f, p.y - 400f)
        assertTrue("trail too large: $trail", trail < 8f)
    }

    @Test fun smoothFilter_fastMotionRelaxesSmoothing() {
        val heavy = SmoothFilter(1.0f)
        val light = SmoothFilter(1.0f)
        heavy.filter(pt(0f, 0f), 0f)
        light.filter(pt(0f, 0f), 0f)
        // Same jump: one arrives at high speed (relaxed alpha), one at low speed.
        val fast = heavy.filter(pt(50f, 0f), 10f)
        val slow = light.filter(pt(50f, 0f), 0.01f)
        assertTrue(fast.x > slow.x)
    }

    @Test fun smoothFilter_pressureChannelSmooths() {
        val f = SmoothFilter(0.5f)
        f.filter(pt(0f, 0f, pressure = 0.2f), 0f)
        val out = f.filter(pt(1f, 0f, pressure = 0.8f), 0.2f)
        assertTrue("pressure should be between samples", out.pressure in 0.2f..0.8f)
    }

    // --- SplineProducer ---

    @Test fun spline_catmullRomEndpointsPassThrough() {
        val seg = SplineProducer.catmullRomToBezier(
            pt(0f, 0f), pt(10f, 0f), pt(20f, 5f), pt(30f, 20f),
        )
        assertEquals(10f, seg.startX, 1e-4f)
        assertEquals(0f, seg.startY, 1e-4f)
        assertEquals(20f, seg.endX, 1e-4f)
        assertEquals(5f, seg.endY, 1e-4f)
    }

    @Test fun spline_collinearPointsStayFlat() {
        val seg = SplineProducer.catmullRomToBezier(
            pt(0f, 0f), pt(10f, 0f), pt(20f, 0f), pt(30f, 0f),
        )
        assertEquals(0f, seg.control1Y, 1e-4f)
        assertEquals(0f, seg.control2Y, 1e-4f)
    }

    @Test fun spline_adaptiveSubdivisionHitsEndpoint() {
        val out = SplineProducer.flattenCubic(0f, 0f, 10f, 40f, 20f, -40f, 30f, 0f)
        val last = out.last()
        assertEquals(30f, last[0], 1e-4f)
        assertEquals(0f, last[1], 1e-4f)
        assertTrue("subdivision should emit multiple points", out.size > 2)
    }

    @Test fun spline_resampleByDistance_uniform() {
        val line = listOf(pt(0f, 0f), pt(10f, 0f), pt(20f, 0f), pt(100f, 0f))
        val resampled = SplineProducer.resampleByDistance(line, stepPx = 10f)
        // 100px line at 10px step -> 10 interior samples + endpoint.
        assertTrue(resampled.size in 10..12)
        for (i in 1 until resampled.size) {
            val d = hypot(resampled[i].x - resampled[i - 1].x, resampled[i].y - resampled[i - 1].y)
            assertTrue("gap too large: $d", d <= 10.01f + 1e-3f)
        }
        assertEquals(100f, resampled.last().x, 1e-4f)
    }

    // --- NeoPenStrokeEngine pipeline ---

    private fun engine(smooth: Float = 0.35f) = NeoPenStrokeEngine(
        NeoPenConfig(penType = PenType.FOUNTAIN, smoothLevel = smooth, penWidth = 5f, penMinWidth = 1.2f),
    )

    @Test fun engine_startThenTap_producesDot() {
        val e = engine()
        e.startStroke(pt(5f, 5f))
        val stroke = e.endStroke(pt(5f, 5f))
        assertEquals(1, stroke.segments.size)
        assertEquals(true, stroke.isComplete)
    }

    @Test fun engine_singleSwipe_producesContinuousSegments() {
        val e = engine(0.2f)
        e.startStroke(pt(0f, 0f, t = 0))
        // Window starts padded with the pen-down point; the first emission happens on
        // the second point and ink begins exactly at pen-down.
        val seg1 = e.addPoint(pt(10f, 0f, t = 8), 1.25f)
        assertEquals(null, seg1)
        val seg2 = e.addPoint(pt(20f, 0f, t = 16), 1.25f)
        assertTrue("4-point window should emit a segment", seg2 != null)
        assertEquals(0f, seg2!!.startX, 0.01f)
        val seg3 = e.addPoint(pt(30f, 0f, t = 24), 1.25f)
        assertTrue(seg3 != null)
        val stroke = e.endStroke(pt(40f, 0f, t = 32))
        assertTrue(stroke.segments.isNotEmpty())
        // Continuity: every segment starts where the previous one ended.
        for (i in 1 until stroke.segments.size) {
            assertEquals(stroke.segments[i - 1].endX, stroke.segments[i].startX, 0.01f)
            assertEquals(stroke.segments[i - 1].endY, stroke.segments[i].startY, 0.01f)
        }
        // Endpoint fidelity: ink ends where the pen ended.
        val last = stroke.segments.last()
        assertEquals(40f, last.endX, 2.0f)
    }

    @Test fun engine_widthRespectsNativeKnobs() {
        val e = NeoPenStrokeEngine(
            NeoPenConfig(
                penType = PenType.FOUNTAIN,
                penWidth = 10f,
                penMinWidth = 2f,
                pressureSensitivity = 1f,
                ignorePressure = false,
                velocitySensitivity = 0f,
            ),
        )
        val light = e.computeStrokeWidth(0.1f, 0f)
        val heavy = e.computeStrokeWidth(1.0f, 0f)
        assertTrue(light < heavy)
        assertTrue(heavy <= 10f + 1e-3f)
        assertTrue(light >= 2f - 1e-3f)
    }

    @Test fun engine_ignorePressureGivesFixedWidth() {
        val e = NeoPenStrokeEngine(
            NeoPenConfig(penType = PenType.HIGHLIGHTER, ignorePressure = true, penWidth = 24f),
        )
        assertEquals(24f, e.computeStrokeWidth(0.1f, 0f), 1e-4f)
        assertEquals(24f, e.computeStrokeWidth(1.0f, 0f), 1e-4f)
    }

    @Test fun engine_velocityThinningRespectsBounds() {
        val e = NeoPenStrokeEngine(
            NeoPenConfig(
                penType = PenType.FOUNTAIN,
                penWidth = 10f,
                penMinWidth = 1f,
                pressureSensitivity = 0f,
                velocitySensitivity = 1f,
                velocityLowerBound = 0.5f,
                velocityUpperBound = 2.5f,
                velocityIgnoreThreshold = 0.05f,
            ),
        )
        val slow = e.computeStrokeWidth(0.5f, 0.4f)  // below lower bound -> full width
        val fast = e.computeStrokeWidth(0.5f, 3f)    // above upper bound -> max thinning
        assertEquals(10f, slow, 1e-3f)
        assertTrue(fast < slow)
    }

    @Test fun engine_endVelocityTaperThinsTail() {
        val e = engine(0.0f)
        e.startStroke(pt(0f, 0f, t = 0))
        e.addPoint(pt(10f, 0f, t = 8), 1f)
        e.addPoint(pt(20f, 0f, t = 16), 1f)
        e.addPoint(pt(30f, 0f, t = 24), 1f)
        val stroke = e.endStroke(pt(40f, 0f, t = 32))
        val last = stroke.segments.last()
        assertTrue(
            "tail should taper below nominal width",
            last.endWidth < e.computeStrokeWidth(0.5f, 0f),
        )
    }

    @Test fun engine_stabilizationReducesJitter() {
        // Simulate a noisy hand: straight-line intent + per-sample tremor.
        val noisy = mutableListOf<InkPoint>()
        var t = 0L
        for (i in 0..60) {
            t += 8
            noisy.add(pt(i * 2f + (if (i % 2 == 0) 1.5f else -1.5f), (if (i % 3 == 0) 2f else -2f), t = t))
        }
        val rawDeviations = noisy.map { kotlin.math.abs(it.y - 0f) }.average()

        val e = engine(0.7f)
        e.startStroke(noisy.first())
        val xs = mutableListOf(noisy.first().x)
        for (i in 1 until noisy.size) {
            e.addPoint(noisy[i], 0.25f)
        }
        val stroke = e.endStroke(noisy.last())
        val smoothDeviations = stroke.segments.flatMap { listOf(it.startY, it.endY) }
            .map { kotlin.math.abs(it) }.average()

        assertTrue(
            "stabilized deviation ($smoothDeviations) should be below raw ($rawDeviations)",
            smoothDeviations < rawDeviations,
        )
    }
}
