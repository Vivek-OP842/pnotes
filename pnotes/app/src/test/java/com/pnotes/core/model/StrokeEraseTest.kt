package com.pnotes.core.model

import com.pnotes.core.stroke.Sample
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolConfig
import com.pnotes.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Area-erase split ([Stroke.erasedBy]): partitioning a stroke's samples around an eraser circle. */
class StrokeEraseTest {

    private fun stroke(vararg pts: Pair<Double, Double>): Stroke =
        Stroke(Tool.PEN, ToolConfig(), pts.map { Sample(it.first, it.second, 1.0) }.toMutableList())

    @Test fun untouchedReturnsNull() {
        val s = stroke(0.0 to 0.0, 10.0 to 0.0, 20.0 to 0.0)
        assertNull(s.erasedBy(100.0, 100.0, 5.0)) // far away -> bbox reject -> null
    }

    @Test fun emptyStrokeReturnsNull() {
        assertNull(Stroke(Tool.PEN, ToolConfig()).erasedBy(0.0, 0.0, 5.0))
    }

    @Test fun fullCoverReturnsEmpty() {
        val s = stroke(0.0 to 0.0, 1.0 to 0.0, 2.0 to 0.0)
        val frags = s.erasedBy(1.0, 0.0, 10.0) // every sample inside -> whole-removal signal
        assertNotNull(frags)
        assertTrue(frags!!.isEmpty())
    }

    @Test fun endTouchTrimsToOneFragment() {
        // x = 0,10,20,30,40,50; circle at (5,0) r=8 covers samples 0 and 1, the rest survive.
        val s = stroke(0.0 to 0.0, 10.0 to 0.0, 20.0 to 0.0, 30.0 to 0.0, 40.0 to 0.0, 50.0 to 0.0)
        val frags = s.erasedBy(5.0, 0.0, 8.0)!!
        assertEquals(1, frags.size)
        assertEquals(listOf(20.0, 30.0, 40.0, 50.0), frags[0].samples.map { it.x })
    }

    @Test fun midHoleSplitsInTwo() {
        // circle at (25,0) r=8 covers samples at x=20 and x=30 (indices 2,3) -> two fragments.
        val s = stroke(0.0 to 0.0, 10.0 to 0.0, 20.0 to 0.0, 30.0 to 0.0, 40.0 to 0.0, 50.0 to 0.0)
        val frags = s.erasedBy(25.0, 0.0, 8.0)!!
        assertEquals(2, frags.size)
        assertEquals(listOf(0.0, 10.0), frags[0].samples.map { it.x }) // order preserved
        assertEquals(listOf(40.0, 50.0), frags[1].samples.map { it.x })
    }

    @Test fun multipleHolesProduceThreeFragments() {
        // A path that crosses the eraser circle (origin, r=5) twice: samples 1 and 4 are inside.
        val s = stroke(
            10.0 to 0.0,   // out
            0.0 to 0.0,    // in  (erased)
            -10.0 to 0.0,  // out
            -10.0 to 1.0,  // out
            0.0 to 1.0,    // in  (erased)
            10.0 to 1.0,   // out
        )
        val frags = s.erasedBy(0.0, 0.0, 5.0)!!
        assertEquals(3, frags.size)
        assertEquals(listOf(1, 2, 1), frags.map { it.samples.size })
    }

    @Test fun singleSurvivingSamplesAreKept() {
        // Erase the middle of three collinear samples -> two single-sample dots survive (not dropped).
        val s = stroke(0.0 to 0.0, 10.0 to 0.0, 20.0 to 0.0)
        val frags = s.erasedBy(10.0, 0.0, 5.0)!!
        assertEquals(2, frags.size)
        assertTrue(frags.all { it.samples.size == 1 })
    }

    @Test fun fragmentsShareToolConfigAndSpeedScale() {
        val cfg = ToolConfig(baseWidth = 7.0)
        val orig = Stroke(
            Tool.HIGHLIGHTER, cfg,
            mutableListOf(Sample(0.0, 0.0, 1.0), Sample(10.0, 0.0, 1.0), Sample(20.0, 0.0, 1.0)),
            speedScale = 2.5,
        )
        for (frag in orig.erasedBy(0.0, 0.0, 5.0)!!) {
            assertEquals(Tool.HIGHLIGHTER, frag.tool)
            assertSame(cfg, frag.config) // same config reference, not a copy
            assertEquals(2.5, frag.speedScale, 1e-12)
        }
    }

    @Test fun fragmentsAreIndependentCopies() {
        val orig = stroke(0.0 to 0.0, 10.0 to 0.0, 20.0 to 0.0)
        val frag = orig.erasedBy(0.0, 0.0, 5.0)!!.first() // survivors: x=10,20
        val sizeBefore = frag.samples.size
        orig.setSamples(emptyList()) // mutating the original must not disturb the fragment's copy
        assertEquals(sizeBefore, frag.samples.size)
        assertEquals(10.0, frag.samples.first().x, 1e-12)
    }

    @Test fun agreesWithIntersectsCircle() {
        // The load-bearing invariant: erasedBy(..) != null  <=>  intersectsCircle(..) == true.
        val s = stroke(0.0 to 0.0, 10.0 to 0.0, 20.0 to 10.0, 30.0 to 10.0)
        val cases = listOf(
            Triple(5.0, 0.0, 6.0),     // hits sample 0
            Triple(100.0, 100.0, 5.0), // far miss (bbox reject)
            Triple(20.0, 10.0, 4.0),   // hits sample 2
            Triple(15.0, 5.0, 2.0),    // inside bbox but reaches no sample
            Triple(25.0, 10.0, 50.0),  // covers everything
        )
        for ((cx, cy, r) in cases) {
            assertEquals(
                "center=($cx,$cy) r=$r",
                s.intersectsCircle(cx, cy, r),
                s.erasedBy(cx, cy, r) != null,
            )
        }
    }

    // --- the eraser measures the ink that was drawn, not the ink the pen nominally lays down ---

    private fun shaped(tool: Tool, config: ToolConfig, points: List<Pair<Double, Double>>): Stroke =
        Stroke(tool, config, points.map { Sample(it.first, it.second, 1.0) }.toMutableList())

    @Test fun aCalligraphyThinFaceIsErasedWhereItIsDrawn() {
        // A nib drawn *up* the page rides its thin face, so the engine renders it at a fraction of
        // the width the pen nominally has. The eraser has to measure the rendered width: at the
        // nominal one it would carve the stroke from blank paper beside it.
        val cfg = ToolDefaults.configFor(Tool.CALLIGRAPHY)
        val s = shaped(Tool.CALLIGRAPHY, cfg, (0..8).map { 0.0 to -5.0 * it })
        val drawn = s.geometry().hw(4)
        val nominal = cfg.baseWidth * 0.5
        assertTrue("the thin face has to render thinner than nominal", drawn < nominal * 0.75)

        val r = 6.0
        val y = -20.0 // the middle sample
        // Reaching the ink it laid down: erased.
        assertNotNull(s.erasedBy(r + drawn - 0.5, y, r))
        // Short of it by more than the nominal radius: nothing to erase. This is the regression —
        // measured at `nominal` the circle landed on the stroke and carved it from empty paper.
        assertNull(s.erasedBy(r + nominal - 0.5, y, r))
        // And well clear of it, both from the sample loop and from the box reject.
        assertNull(s.erasedBy(r + drawn + 1.5, y, r))
        assertEquals(false, s.intersectsCircle(r + nominal - 0.5, y, r))
    }

    @Test fun hairlineAndShapedPensStillAgreeWithTheHitTest() {
        // A hairline narrower than the eraser's own floor, a nib that renders thinner than its base
        // width, and a taper whose tip is thinner still: all three have to keep the invariant a
        // selection depends on — a tap that picks a stroke is one the eraser would touch.
        val strokes = listOf(
            shaped(
                Tool.CALLIGRAPHY, ToolDefaults.configFor(Tool.CALLIGRAPHY),
                (0..12).map { it * 3.0 to it * 2.0 },
            ),
            shaped(Tool.PEN, ToolConfig(baseWidth = 0.6), (0..8).map { it * 2.0 to 0.0 }),
            shaped(Tool.TAPER, ToolDefaults.configFor(Tool.TAPER), (0..10).map { it * 3.0 to 0.0 }),
        )
        for ((k, s) in strokes.withIndex()) {
            for (cx in 0..60 step 3) {
                for (cy in -30..40 step 7) {
                    for (r in listOf(2.0, 7.0, 18.0)) {
                        assertEquals(
                            "stroke $k at ($cx,$cy) r=$r",
                            s.intersectsCircle(cx.toDouble(), cy.toDouble(), r),
                            s.erasedBy(cx.toDouble(), cy.toDouble(), r) != null,
                        )
                    }
                }
            }
        }
    }
}
