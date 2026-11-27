package com.pnotes.canvas

import com.pnotes.core.geometry.Geometry
import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.model.CanvasItem
import com.pnotes.core.model.Page

/** A selected item plus the index of the page it lives on. */
data class Selected(val pageIndex: Int, val item: CanvasItem)

/**
 * Pure selection-membership tests (spec 06 §6–7).
 *
 * A locked item is not a member of anything, whichever way the selection was drawn. Putting that
 * here rather than at each call site is what makes it true of the band and the lasso on both
 * canvases at once.
 */
object SelectionMath {

    /**
     * Band selection over a flat item list, for a canvas with no pages: every item whose bounds
     * meet [band]. The paged overloads below are the same test with a page-space translation in
     * front of it; sharing the rule is the point, so the two canvases select alike.
     */
    fun bandMembers(items: List<CanvasItem>, band: Rect): List<CanvasItem> =
        items.filter { !it.locked && it.bounds().intersects(band) }

    /** Lasso selection over a flat item list: every item whose geometry or bounds lies inside [polygon]. */
    fun lassoMembers(items: List<CanvasItem>, polygon: List<Pt>): List<CanvasItem> {
        if (polygon.size < 3) return emptyList()
        val polyBounds = Rect.bounding(polygon)
        if (!polyBounds.left.isFinite() || !polyBounds.top.isFinite() || !polyBounds.w.isFinite() || !polyBounds.h.isFinite()) return emptyList()
        return items.filter {
            !it.locked &&
                it.bounds().outset(24.0).intersects(polyBounds) &&
                itemHitsLasso(it, polygon) { p -> p }
        }
    }

    /**
     * Internal hit-test for lasso containment: checks centroid, center of bounds, bounds corners,
     * bounds containment, and stroke sample points.
     */
    fun itemHitsLasso(item: CanvasItem, polygon: List<Pt>, toContent: (Pt) -> Pt): Boolean {
        val b = item.bounds()
        // 1. Centroid
        if (Geometry.pointInPolygon(polygon, toContent(item.centroid()))) return true
        // 2. Bounds center
        if (Geometry.pointInPolygon(polygon, toContent(b.center))) return true
        // 3. Four corners of bounds
        if (Geometry.pointInPolygon(polygon, toContent(b.topLeft)) ||
            Geometry.pointInPolygon(polygon, toContent(Pt(b.right, b.bottom))) ||
            Geometry.pointInPolygon(polygon, toContent(Pt(b.right, b.top))) ||
            Geometry.pointInPolygon(polygon, toContent(Pt(b.left, b.bottom)))
        ) return true

        // 4. Polygon bounds containment (lasso drawn inside large item or item fully inside lasso)
        val polyBounds = Rect.bounding(polygon)
        val p0 = toContent(Pt(b.left, b.top))
        val p1 = toContent(Pt(b.right, b.bottom))
        val itemRect = Rect.fromPoints(p0, p1)
        if (itemRect.contains(polyBounds.topLeft) && itemRect.contains(Pt(polyBounds.right, polyBounds.bottom))) return true

        // 5. Stroke sample points for handwritten ink
        if (item is com.pnotes.core.model.Stroke) {
            val count = item.sampleCount
            val step = maxOf(1, count / 30)
            for (k in 0 until count step step) {
                val pt = toContent(Pt(item.xAt(k), item.yAt(k)))
                if (Geometry.pointInPolygon(polygon, pt)) return true
            }
            if (count > 0) {
                if (Geometry.pointInPolygon(polygon, toContent(Pt(item.xAt(0), item.yAt(0))))) return true
                if (Geometry.pointInPolygon(polygon, toContent(Pt(item.xAt(count - 1), item.yAt(count - 1))))) return true
            }
        }
        return false
    }

    /**
     * Band selection: every item whose content-space bounds intersect [band]. [toContentRect]
     * maps an item's page-space bounds on page i into content space (a plain page-rect
     * translation, plus the display rotation when the view is rotated).
     */
    fun bandMembers(
        pages: List<Page>,
        pageRects: List<Rect>,
        band: Rect,
        toContentRect: (Int, Rect) -> Rect = { i, r -> r.translate(pageRects[i].left, pageRects[i].top) },
    ): List<Selected> {
        val out = ArrayList<Selected>()
        for (i in pages.indices) {
            val pr = pageRects.getOrNull(i) ?: continue
            // Skip a page the band cannot reach, before touching its items. item.bounds() builds the
            // stroke's ribbon, so without this one small drag rebuilds every stroke in the document
            // and undoes the canvas's geometry eviction. Outset by OFF_PAGE_SLACK because an item
            // may paint past its page edge (a neon glow, ink drawn over the margin).
            if (!pr.outset(OFF_PAGE_SLACK).intersects(band)) continue
            for (item in pages[i].items) {
                if (item.locked) continue
                if (toContentRect(i, item.bounds()).intersects(band)) out.add(Selected(i, item))
            }
        }
        return out
    }

    /** How far past its page edge an item may reach and still be reachable by a band that misses
     *  the page rect. Covers neon bloom and ink drawn over the margin. */
    const val OFF_PAGE_SLACK = 64.0

    /**
     * Lasso selection: every item whose content-space centroid lies inside [polygon] (even-odd).
     * [toContent] maps a page-space point on page i into content space, like [bandMembers].
     */
    fun lassoMembers(
        pages: List<Page>,
        pageRects: List<Rect>,
        polygon: List<Pt>,
        toContent: (Int, Pt) -> Pt = { i, p -> Pt(p.x + pageRects[i].left, p.y + pageRects[i].top) },
    ): List<Selected> {
        if (polygon.size < 3) return emptyList()
        val polyBounds = Rect.bounding(polygon)
        if (!polyBounds.left.isFinite() || !polyBounds.top.isFinite() || !polyBounds.w.isFinite() || !polyBounds.h.isFinite()) return emptyList()
        val out = ArrayList<Selected>()
        for (i in pages.indices) {
            val pr = pageRects.getOrNull(i) ?: continue
            // Skip pages that do not intersect the polygon bounding box!
            if (!pr.outset(OFF_PAGE_SLACK).intersects(polyBounds)) continue
            for (item in pages[i].items) {
                if (item.locked) continue
                val b = item.bounds()
                val p0 = toContent(i, Pt(b.left, b.top))
                val p1 = toContent(i, Pt(b.right, b.bottom))
                val itemContentRect = Rect.fromPoints(p0, p1)
                if (!itemContentRect.outset(24.0).intersects(polyBounds)) continue

                if (itemHitsLasso(item, polygon) { p -> toContent(i, p) }) {
                    out.add(Selected(i, item))
                }
            }
        }
        return out
    }
}
