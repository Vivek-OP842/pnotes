package com.pnotes.core.model

import com.pnotes.core.stroke.NibAxis
import com.pnotes.core.stroke.Sample
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The calligraphy nib's broad axis taken from the stylus's own tilt and azimuth ([NibAxis]) rather
 * than from the page's vertical.
 *
 * [com.pnotes.core.stroke.StrokeEngine] used to stand in for the missing information with the page:
 * its direction term was the stroke's own y-component, so a downstroke earned the broad face whether
 * or not the writer's nib was turned that way. Everything here goes through [Stroke] — the sample
 * channels as a stylus fills them, the axis the first sample yields, and the build each consumer gets
 * handed — so it pins the seam rather than the engine's arithmetic, which
 * [com.pnotes.core.stroke.WetRibbonTest] and [com.pnotes.core.stroke.NibAxisTest] already hold down.
 *
 * The last two tests are the promise the rest of the app leans on: ink from a stylus that reports
 * nothing — a finger, or one held too upright to read — is the ink it has always been.
 */
class StrokeNibAxisTest {

    private val calligraphy = ToolDefaults.configFor(Tool.CALLIGRAPHY)

    /** A straight run of [count] samples along the unit direction ([dx], [dy]), [step] px apart. */
    private fun line(count: Int, dx: Double, dy: Double, step: Double = 3.0): List<Sample> =
        (0 until count).map { Sample(it * step * dx, it * step * dy, 1.0) }

    /** A run of samples along the broad axis of a nib held at [azimuth] and ground by [cut], or
     *  across it when [across]. The axis a stroke has to run along to earn the full face. */
    private fun along(azimuth: Double, cut: Double, across: Boolean = false, count: Int = 60): List<Sample> {
        val t = azimuth + cut
        return if (across) line(count, cos(t), -sin(t)) else line(count, sin(t), cos(t))
    }

    /** A finished calligraphy stroke of [samples], the stylus held at [tilt]/[azimuth] throughout,
     *  through a nib ground [cut] radians off the lean. */
    private fun drawn(samples: List<Sample>, tilt: Double = 0.0, azimuth: Double = 0.0, cut: Double = 0.0): Stroke =
        Stroke(Tool.CALLIGRAPHY, if (cut == 0.0) calligraphy else calligraphy.copy(nibCut = cut), samples).apply {
            if (tilt != 0.0 || azimuth != 0.0) {
                setOrientation(List(samples.size) { tilt }, List(samples.size) { azimuth })
            }
            finished = true
        }

    /** A stroke still under the pen, fed the way the capture path feeds one: orientation in the
     *  samples themselves, so the ribbon is built knowing which way the nib is turned. */
    private fun live(samples: List<Sample>, cut: Double = 0.0): Stroke =
        Stroke(Tool.CALLIGRAPHY, if (cut == 0.0) calligraphy else calligraphy.copy(nibCut = cut)).apply {
            finished = false
            for (s in samples) addSample(s)
        }

    /** Half-width a third of the way in: clear of both the head and the lift-time tail. */
    private fun midWidth(s: Stroke): Double {
        val g = s.geometry()
        return g.hw(g.pointCount / 3)
    }

    private fun assertSamePoints(a: Stroke, b: Stroke, what: String) {
        val ga = a.geometry()
        val gb = b.geometry()
        assertEquals("$what: point count", ga.pointCount, gb.pointCount)
        for (i in 0 until minOf(ga.pointCount, gb.pointCount)) {
            assertEquals("$what: cx[$i]", ga.cx(i), gb.cx(i), TOL)
            assertEquals("$what: cy[$i]", ga.cy(i), gb.cy(i), TOL)
            assertEquals("$what: hw[$i]", ga.hw(i), gb.hw(i), TOL)
        }
    }

    // Pressure is pinned at 1.0 in every path below, so the pressure term is 1.0 and the half-width
    // is baseWidth * (1 + ds * dir) / 2 for a direction term of dir: the full face when the stroke
    // runs along the nib's broad axis, the naked edge when it runs across it.
    private val broad = calligraphy.baseWidth * (1.0 + calligraphy.directionStrength) / 2.0
    private val edge = calligraphy.baseWidth / 2.0

    @Test fun theWritersNibDecidesTheBroadFaceNotThePagesVertical() {
        // No orientation reported: the page stands in for the nib, so a downstroke is broad and a
        // stroke across it is the edge — the ink calligraphy has always drawn.
        assertEquals("downstroke without an axis", broad, midWidth(drawn(line(60, 0.0, 1.0))), TOL)
        assertEquals("crosswise without an axis", edge, midWidth(drawn(line(60, 1.0, 0.0))), TOL)
        // The same two strokes drawn with the nib turned a quarter turn: the faces swap, because the
        // nib turned with the writer and the page did not.
        val quarter = PI / 2
        assertEquals("downstroke with the nib turned", edge, midWidth(drawn(line(60, 0.0, 1.0), TILT, quarter)), TOL)
        assertEquals("crosswise with the nib turned", broad, midWidth(drawn(line(60, 1.0, 0.0), TILT, quarter)), TOL)
    }

    @Test fun everyAzimuthPutsTheBroadFaceOnTheLeaningBarrel() {
        // The barrel's screen projection is (sin a, cos a) and the broad face lies along it, so a
        // stroke down the lean is the full face and one across it is the edge — at any azimuth.
        for (a in listOf(0.0, 0.4, 1.1, 2.4, -0.9, PI)) {
            val lean = line(60, sin(a), cos(a))
            val across = line(60, cos(a), -sin(a))
            assertEquals("azimuth $a along the lean", broad, midWidth(drawn(lean, TILT, a)), TOL)
            assertEquals("azimuth $a across it", edge, midWidth(drawn(across, TILT, a)), TOL)
        }
    }

    @Test fun azimuthZeroDrawsTheInkTheFixedVerticalAlwaysDrew() {
        // A stylus leaning straight back at the writer is the hold the old fixed axis described. Its
        // ink has to come out the same through the axis path, or every turned-nib change would also
        // have moved ink that never asked for one.
        for (s in listOf(line(60, 0.0, 1.0), line(60, 1.0, 0.0), line(60, 0.6, 0.8))) {
            assertSamePoints(drawn(s), drawn(s, TILT, 0.0), "azimuth 0")
        }
    }

    @Test fun anUnreadableTiltFallsBackAndTheFloorIsTheLastTiltThatIsRead() {
        val samples = line(60, sin(0.7), cos(0.7))
        // Below the floor the axis is dropped entirely: no orientation at all, so the page's
        // vertical is back in charge and not one number of the ink moves.
        assertSamePoints(
            drawn(samples),
            drawn(samples, tilt = NibAxis.MIN_TILT / 2, azimuth = 2.2),
            "below the tilt floor",
        )
        // At the floor it is read, and the axes disagree by enough to show.
        val atFloor = drawn(samples, tilt = NibAxis.MIN_TILT, azimuth = 2.2)
        assertTrue(
            "the floor must be the last tilt that is read",
            abs(midWidth(atFloor) - midWidth(drawn(samples))) > 0.1,
        )
    }

    @Test fun theGrindTurnsTheBroadFaceWhereverItIsSet() {
        // The cut rides the stroke's config, so a preset (not a constant) decides which stroke earns
        // the full face: square (0) is the ink calligraphy always drew, and an oblique moves it.
        for (cut in listOf(0.0, -0.35, 0.9, PI / 2)) {
            for (a in listOf(0.0, 0.7, 2.1)) {
                assertEquals(
                    "cut $cut at azimuth $a, along the ground edge",
                    broad,
                    midWidth(drawn(along(a, cut), TILT, a, cut)),
                    TOL,
                )
                assertEquals(
                    "cut $cut at azimuth $a, across it",
                    edge,
                    midWidth(drawn(along(a, cut, across = true), TILT, a, cut)),
                    TOL,
                )
            }
        }
    }

    @Test fun aGrindIsInertWithoutAnOrientationToTurn() {
        // A finger, or a stylus held too upright to read, leaves the axis on the page's vertical, and
        // the page has no grind. The cut must change nothing there rather than quietly turning it.
        for (s in listOf(line(60, 0.0, 1.0), line(60, 1.0, 0.0), line(60, 0.6, 0.8))) {
            assertSamePoints(drawn(s, cut = PI / 3), drawn(s), "a grind with no tilt")
        }
    }

    @Test fun theLiveRibbonDrawsTheGroundNibTheWayTheEngineDoes() {
        // The live ribbon reads the same config at pen-down, so an oblique has to come out oblique
        // while the pen is still down and not merely once the stroke is rebuilt at lift.
        for (cut in listOf(-0.4, 0.6)) {
            for (a in listOf(0.0, 1.2, -0.8)) {
                assertEquals(
                    "live, cut $cut at azimuth $a, along the ground edge",
                    broad,
                    midWidth(live(along(a, cut, count = 30).map { it.copy(tilt = TILT, azimuth = a) }, cut)),
                    TOL,
                )
                assertEquals(
                    "live, cut $cut at azimuth $a, across it",
                    edge,
                    midWidth(live(along(a, cut, across = true, count = 30).map { it.copy(tilt = TILT, azimuth = a) }, cut)),
                    TOL,
                )
            }
        }
    }

    @Test fun anAxisLetsALiveHeadSettleBeforeTheChordWindowFills() {
        // The pen-down head rule existed because the first samples of a stroke could not say which
        // way it was going: it pinned a live stroke's ink flat until its first window of travel had
        // filled, then rewrote it once. A stylus that reports its axis hands that answer over with
        // the first sample, so nothing is pinned and the ink behind the nib is final a sample earlier.
        val short = line(3, sin(0.4), cos(0.4), step = 2.0)
        val plain = live(short)
        val turned = live(short.map { it.copy(tilt = TILT, azimuth = 1.1) })
        assertEquals("the chord has nothing to settle yet", 0, plain.wetRibbon!!.settledCount)
        assertTrue("an axis settles points earlier", turned.wetRibbon!!.settledCount > 0)
    }

    @Test fun theLiveRibbonDrawsTheTurnedNibTheWayTheEngineDoes() {
        // The live path is a separate implementation reading the same axis off the same first
        // sample, so it has to agree about the face the nib lays down and not merely about the
        // direction of travel.
        for (a in listOf(0.0, 0.9, 2.4, -0.7)) {
            val lean = line(30, sin(a), cos(a), step = 2.5).map { it.copy(tilt = TILT, azimuth = a) }
            val across = line(30, cos(a), -sin(a), step = 2.5).map { it.copy(tilt = TILT, azimuth = a) }
            assertEquals("live, azimuth $a along the lean", broad, midWidth(live(lean)), TOL)
            assertEquals("live, azimuth $a across it", edge, midWidth(live(across)), TOL)
        }
    }

    private companion object {
        /** The tilt a writer holds a stylus at: well past [NibAxis.MIN_TILT]. */
        const val TILT = 0.8

        /** Half-widths are stored as floats, so exactness is out of reach past a few digits. */
        const val TOL = 1e-6
    }
}
