package com.pnotes.canvas

import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.model.Page
import com.pnotes.core.model.Stroke
import com.pnotes.core.stroke.Sample
import com.pnotes.core.tools.Tool
import com.pnotes.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionMathTest {

    private fun dot(x: Double, y: Double): Stroke =
        Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), mutableListOf(Sample(x, y, 1.0)))

    // Two A4-ish pages stacked; page rects in content space.
    private val pages = listOf(Page(100.0, 100.0), Page(100.0, 100.0))
    private val rects = listOf(Rect(48.0, 48.0, 100.0, 100.0), Rect(48.0, 186.0, 100.0, 100.0))

    @Test fun bandSelectsOnlyIntersecting() {
        val a = dot(10.0, 10.0) // page 0, content ~ (58,58)
        val b = dot(90.0, 90.0) // page 0, content ~ (138,138)
        pages[0].items.addAll(listOf(a, b))
        // Band over the top-left corner of page 0 only.
        val members = SelectionMath.bandMembers(pages, rects, Rect(50.0, 50.0, 30.0, 30.0))
        assertEquals(1, members.size)
        assertSame(a, members[0].item)
        assertEquals(0, members[0].pageIndex)
    }

    @Test fun lassoSelectsByCentroidInsidePolygon() {
        val a = dot(10.0, 10.0)
        val b = dot(90.0, 90.0)
        pages[0].items.addAll(listOf(a, b))
        // Polygon (content space) around a's centroid (~58,58).
        val poly = listOf(Pt(50.0, 50.0), Pt(75.0, 50.0), Pt(75.0, 75.0), Pt(50.0, 75.0))
        val members = SelectionMath.lassoMembers(pages, rects, poly)
        assertEquals(1, members.size)
        assertSame(a, members[0].item)
    }

    @Test fun bandSpansPages() {
        val a = dot(50.0, 50.0) // page 0
        val b = dot(50.0, 50.0) // page 1
        pages[0].items.add(a)
        pages[1].items.add(b)
        val members = SelectionMath.bandMembers(pages, rects, Rect(0.0, 0.0, 300.0, 400.0))
        assertEquals(2, members.size)
        assertTrue(members.any { it.pageIndex == 0 } && members.any { it.pageIndex == 1 })
    }

    @Test fun degenerateLassoSelectsNothing() {
        pages[0].items.add(dot(50.0, 50.0))
        assertTrue(SelectionMath.lassoMembers(pages, rects, listOf(Pt(0.0, 0.0), Pt(1.0, 1.0))).isEmpty())
    }

    @Test fun lassoHandlesNonFiniteGracefully() {
        pages[0].items.add(dot(50.0, 50.0))
        val nanPoly = listOf(Pt(Double.NaN, 0.0), Pt(10.0, 10.0), Pt(20.0, 20.0))
        assertTrue(SelectionMath.lassoMembers(pages, rects, nanPoly).isEmpty())
        val infPoly = listOf(Pt(Double.POSITIVE_INFINITY, 0.0), Pt(10.0, 10.0), Pt(20.0, 20.0))
        assertTrue(SelectionMath.lassoMembers(pages, rects, infPoly).isEmpty())
    }

    @Test fun strokeContainsFastRejection() {
        val s = dot(50.0, 50.0)
        // Point far outside bounds returns false immediately
        org.junit.Assert.assertFalse(s.contains(Pt(500.0, 500.0)))
        // Point within dot disc returns true
        assertTrue(s.contains(Pt(50.0, 50.0)))
    }

    @Test fun lassoSelectsStrokeWhenCentroidOutside() {
        // A U-shaped stroke where samples are at (10, 10), (10, 50), (50, 50), (50, 10).
        // The centroid is in the center of the U around (30, 30).
        // If the lasso polygon encircles the bottom bend at (10..50, 40..60),
        // the centroid is NOT inside the polygon, but the stroke samples ARE!
        val uStroke = Stroke(
            Tool.PEN,
            ToolDefaults.configFor(Tool.PEN),
            mutableListOf(
                Sample(10.0, 10.0, 1.0),
                Sample(10.0, 50.0, 1.0),
                Sample(50.0, 50.0, 1.0),
                Sample(50.0, 10.0, 1.0),
            )
        )
        val items = listOf(uStroke)
        // Polygon encircling the bottom part only (x: 5..55, y: 45..55)
        val poly = listOf(Pt(5.0, 45.0), Pt(55.0, 45.0), Pt(55.0, 55.0), Pt(5.0, 55.0))
        val selected = SelectionMath.lassoMembers(items, poly)
        assertEquals(1, selected.size)
        assertSame(uStroke, selected[0])
    }

    @Test fun lassoFlatAndPagedConsistency() {
        val s = dot(20.0, 20.0)
        val flatSelected = SelectionMath.lassoMembers(listOf(s), listOf(Pt(15.0, 15.0), Pt(25.0, 15.0), Pt(25.0, 25.0), Pt(15.0, 25.0)))
        assertEquals(1, flatSelected.size)
    }
}
