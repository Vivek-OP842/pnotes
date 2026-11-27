package com.pnotes.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import com.pnotes.core.geometry.Pt
import com.pnotes.core.geometry.Rect
import com.pnotes.core.model.ImageData
import com.pnotes.core.model.Rgba
import com.pnotes.core.pal.BlendMode
import com.pnotes.core.pal.FillRule
import com.pnotes.core.pal.FontSpec
import com.pnotes.core.pal.Pen
import com.pnotes.core.pal.RasterSurface
import com.pnotes.core.pal.Renderer
import com.pnotes.core.pal.TextFlags
import com.pnotes.core.stroke.TangentHull
import com.pnotes.core.tools.StrokeTexture

/**
 * A [Renderer] backed by an [android.graphics.Canvas]. The same implementation
 * draws to the on-screen view, into page-cache/thumbnail bitmaps, to PDF export
 * and to presentation frames.
 *
 * Only translation and uniform scaling are used; the cumulative scale is tracked
 * so a cosmetic pen's width stays constant in device pixels regardless of zoom.
 */
class AndroidRenderer(canvas: Canvas = Canvas()) : Renderer {

    var canvas: Canvas = canvas
        private set

    fun bind(c: Canvas): AndroidRenderer {
        this.canvas = c
        scaleStack.clear()
        scaleX = 1f
        scaleY = 1f
        return this
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { isDither = true }
    private val rasterBlendPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { isDither = true }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val neonGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = android.graphics.Color.WHITE }
    private val layerPaint = Paint()

    // --- live-stroke settled head, built once and drawn as immutable batches ---
    private val liveBatches = ArrayList<Path>(8)
    private var liveCacheKey: Any? = null
    private var liveBakedPoints = 0

    // --- Shaders for textured drawing ---
    private val defaultPencilShader by lazy {
        createNoiseBitmapShader(density = 0.40f, minAlpha = 30, maxAlpha = 240, seed = 1337L)
    }
    private var cachedPaperGrainBmp: Bitmap? = null
    private var cachedPaperGrainShader: BitmapShader? = null
    private fun getPencilShader(): BitmapShader {
        val bmp = paperGrainBitmap
        if (bmp != null) {
            if (cachedPaperGrainShader == null || cachedPaperGrainBmp != bmp) {
                cachedPaperGrainBmp = bmp
                cachedPaperGrainShader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            }
            return cachedPaperGrainShader!!
        }
        return defaultPencilShader
    }

    private var cachedPencilStampBmp: Bitmap? = null
    private var cachedPencilStampShader: BitmapShader? = null
    private fun getPencilStampShader(): BitmapShader? {
        val bmp = pencilBitmap ?: return null
        if (cachedPencilStampShader == null || cachedPencilStampBmp != bmp) {
            cachedPencilStampBmp = bmp
            cachedPencilStampShader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
        return cachedPencilStampShader
    }

    // --- Clip Studio Paint Shaders ---
    private var cachedCspPenBmp: Bitmap? = null
    private var cachedCspPenShader: BitmapShader? = null
    private fun getCspPenShader(): BitmapShader? {
        val bmp = cspPenBitmap ?: return null
        if (cachedCspPenShader == null || cachedCspPenBmp != bmp) {
            cachedCspPenBmp = bmp
            cachedCspPenShader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
        return cachedCspPenShader
    }

    private var cachedCspPencilBmp: Bitmap? = null
    private var cachedCspPencilShader: BitmapShader? = null
    private fun getCspPencilShader(): BitmapShader {
        val bmp = cspPencilBitmap ?: paperGrainBitmap
        if (bmp != null) {
            if (cachedCspPencilShader == null || cachedCspPencilBmp != bmp) {
                cachedCspPencilBmp = bmp
                cachedCspPencilShader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            }
            return cachedCspPencilShader!!
        }
        return defaultPencilShader
    }

    private var cachedCspBrushBmp: Bitmap? = null
    private var cachedCspBrushShader: BitmapShader? = null
    private fun getCspBrushShader(): BitmapShader? {
        val bmp = cspBrushBitmap ?: return null
        if (cachedCspBrushShader == null || cachedCspBrushBmp != bmp) {
            cachedCspBrushBmp = bmp
            cachedCspBrushShader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
        return cachedCspBrushShader
    }
    private val chalkShader by lazy {
        createNoiseBitmapShader(density = 0.50f, minAlpha = 20, maxAlpha = 230, seed = 777L)
    }
    private val charcoalShader by lazy {
        createNoiseBitmapShader(density = 0.60f, minAlpha = 40, maxAlpha = 255, seed = 999L)
    }
    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private fun createNoiseBitmapShader(density: Float, minAlpha: Int, maxAlpha: Int, seed: Long): BitmapShader {
        val size = 64
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rand = java.util.Random(seed)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val v = rand.nextFloat()
                val a = if (v > density) {
                    (minAlpha + (v - density) / (1f - density) * (maxAlpha - minAlpha)).toInt().coerceIn(0, 255)
                } else {
                    (minAlpha * 0.35f * (v / density)).toInt().coerceIn(0, 255)
                }
                bmp.setPixel(x, y, android.graphics.Color.argb(a, 255, 255, 255))
            }
        }
        return BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    /** The platform blend for [blend], or null when it has none (SRC_OVER, or below API 29). */
    private fun nativeBlend(blend: BlendMode): android.graphics.BlendMode? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return when (blend) {
            BlendMode.MULTIPLY -> android.graphics.BlendMode.MULTIPLY
            BlendMode.SCREEN -> android.graphics.BlendMode.SCREEN
            BlendMode.DARKEN -> android.graphics.BlendMode.DARKEN
            BlendMode.SRC_OVER -> null
        }
    }

    private val clipBounds = android.graphics.Rect()
    private val scaleStack = ArrayDeque<Float>()
    private var scaleX = 1f
    private var scaleY = 1f
    private val avgScale get() = ((scaleX + scaleY) / 2f).coerceAtLeast(1e-4f)

    /** [Renderer.deviceScale]: the transform's cumulative scale — the same figure a cosmetic pen's
     *  width is divided by to stay constant on screen. */
    override val deviceScale get() = avgScale.toDouble()

    override fun save() {
        canvas.save()
        scaleStack.addLast(scaleX)
        scaleStack.addLast(scaleY)
    }

    override fun restore() {
        canvas.restore()
        if (scaleStack.isNotEmpty()) {
            scaleY = scaleStack.removeLast()
            scaleX = scaleStack.removeLast()
        }
    }

    override fun saveLayerAlpha(bounds: Rect, alpha: Double) {
        canvas.saveLayerAlpha(
            bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(),
            (alpha.coerceIn(0.0, 1.0) * 255).toInt(),
        )
        scaleStack.addLast(scaleX)
        scaleStack.addLast(scaleY)
    }

    // MULTIPLY and SCREEN use the W3C separable blends (BlendMode, API 29+), which over a
    // transparent backdrop *deposit* the source and over ink blend — so they
    // work the same in the transparent ink cache and on the composed screen. Below
    // API 29 (and for SRC_OVER) we just use plain alpha compositing.
    override fun saveLayerBlended(bounds: Rect, alpha: Double, blend: BlendMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val native = nativeBlend(blend) ?: return saveLayerAlpha(bounds, alpha)
            layerPaint.reset()
            layerPaint.alpha = (alpha.coerceIn(0.0, 1.0) * 255).toInt()
            layerPaint.blendMode = native
            canvas.saveLayer(
                bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(),
                layerPaint,
            )
            scaleStack.addLast(scaleX)
            scaleStack.addLast(scaleY)
        } else {
            saveLayerAlpha(bounds, alpha)
        }
    }

    override fun translate(dx: Double, dy: Double) {
        canvas.translate(dx.toFloat(), dy.toFloat())
    }

    override fun rotate(degrees: Double) {
        canvas.rotate(degrees.toFloat())
        // A quarter turn swaps which axis each scale factor acts on (cosmetic pen math).
        val d = ((degrees % 360.0) + 360.0) % 360.0
        if (d == 90.0 || d == 270.0) {
            val t = scaleX
            scaleX = scaleY
            scaleY = t
        }
    }

    override fun scale(sx: Double, sy: Double) {
        canvas.scale(sx.toFloat(), sy.toFloat())
        scaleX *= sx.toFloat()
        scaleY *= sy.toFloat()
    }

    override fun clipRect(rect: Rect) {
        canvas.clipRect(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat())
    }

    override fun clear() {
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
    }

    override fun fillBackground(rect: Rect, color: Rgba) = fillRect(rect, color)

    override fun fillRect(rect: Rect, color: Rgba) {
        fillPaint.color = color.toArgb()
        canvas.drawRect(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(), fillPaint)
    }

    override fun fillPolygon(points: List<Pt>, color: Rgba, rule: FillRule) {
        if (points.size < 3) return
        fillPaint.color = color.toArgb()
        canvas.drawPath(buildPath(points, close = true, rule), fillPaint)
    }

    override fun fillCircle(center: Pt, radius: Double, color: Rgba) {
        fillPaint.color = color.toArgb()
        canvas.drawCircle(center.x.toFloat(), center.y.toFloat(), radius.toFloat(), fillPaint)
    }

    // The whole swept-disc ribbon as one path: every disc and bridging quad is wound the same way
    // under WINDING, so overlaps union into solid ink (anti-aliased only along the true outer
    // silhouette, no interior seams, no winding-cancelled gap at a sharp turn). One draw call keeps
    // the repainted-every-frame live stroke cheap. Built straight from the packed float geometry
    // (the [Geometry.ribbonQuad] math inlined) so a dense page allocates nothing per point.
    override fun fillDiskRibbon(
        centers: FloatArray,
        radii: FloatArray,
        color: Rgba,
        capStart: Boolean,
        capEnd: Boolean,
        texture: StrokeTexture,
    ) = fillDiskRibbon(centers, radii, 0, minOf(centers.size / 2, radii.size), color, capStart, capEnd, texture)

    /**
     * Pass 1: High-speed direct hardware live inking.
     * Generates a continuous C1-smooth envelope extending seamlessly to the predicted tip
     * without any self-cancelling interior winding holes, dot-dot artifacts, or gaps under the nib.
     */
    fun drawLiveCapsules(
        centers: FloatArray,
        radii: FloatArray,
        from: Int,
        count: Int,
        color: Rgba,
        texture: StrokeTexture = StrokeTexture.SMOOTH,
        isNeon: Boolean = false,
        neonStrength: Double = 0.6,
        predictedTail: List<Pt>? = null,
        predictedRadius: Float = 0f,
        settled: Int = 0,
        cacheKey: Any? = null,
    ) {
        if (count <= 0) return
        val argb = color.toArgb()
        fillPaint.color = argb

        // Single-point: draw nothing while pen is still down — avoids the nib dot artifact.
        // A lone first sample with no predicted tail is invisible until the stroke has >= 2 points.
        if (count == 1 && (predictedTail == null || predictedTail.isEmpty())) return

        val isChalk = texture == StrokeTexture.CHALK
        val isPencil = texture == StrokeTexture.PENCIL
        val isCharcoal = texture == StrokeTexture.CHARCOAL
        val isCalligraphy = texture == StrokeTexture.CALLIGRAPHY
        val neonActive = isNeon || texture == StrokeTexture.NEON

        // The settled head of a long stroke does not have to be re-splined every frame. Built once
        // in batches, it stays a set of immutable paths; only the tail under the nib is rebuilt.
        // Opaque smooth ink only: two paths of the same colour union without a seam, while a
        // translucent or textured ink would composite the overlap twice.
        if (from == 0 && cacheKey != null && settled >= LIVE_CACHE_BATCH &&
            count > settled && texture == StrokeTexture.SMOOTH && !neonActive && color.a >= 255
        ) {
            drawLiveInBatches(centers, radii, count, argb, predictedTail, settled, cacheKey)
            return
        }

        // Build continuous C1-smooth spline envelope extending to the predicted pen tip
        val extCenters: FloatArray
        val extRadii: FloatArray
        val totalCount: Int
        val path = if (predictedTail != null && predictedTail.isNotEmpty()) {
            totalCount = count + predictedTail.size
            extCenters = FloatArray(2 * totalCount)
            extRadii = FloatArray(totalCount)
            System.arraycopy(centers, 2 * from, extCenters, 0, 2 * count)
            System.arraycopy(radii, from, extRadii, 0, count)
            val lastR = if (count > 0) radii[from + count - 1] else 2f

            for (j in predictedTail.indices) {
                val pt = predictedTail[j]
                extCenters[2 * (count + j)] = pt.x.toFloat()
                extCenters[2 * (count + j) + 1] = pt.y.toFloat()
                extRadii[count + j] = lastR
            }
            buildStrokePath(extCenters, extRadii, 0, totalCount, capStart = true, capEnd = true, texture = texture)
        } else {
            totalCount = count
            extCenters = FloatArray(2 * count)
            extRadii = FloatArray(count)
            System.arraycopy(centers, 2 * from, extCenters, 0, 2 * count)
            System.arraycopy(radii, from, extRadii, 0, count)
            buildStrokePath(centers, radii, from, count, capStart = true, capEnd = true, texture = texture)
        }

        when {
            neonActive -> {
                val s = neonStrength.coerceIn(0.0, 1.0).toFloat()
                val lastR = if (count > 0) radii[from + count - 1] else 3f
                val glowRadius = (lastR * (2.2f + 2.8f * s)).coerceIn(8f, 60f)

                // 1. Wide Vibrant Neon Glow Halo (Blurred)
                neonGlowPaint.color = color.withAlpha(255).toArgb()
                neonGlowPaint.maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
                canvas.drawPath(path, neonGlowPaint)

                // 2. Saturated Tube Body
                fillPaint.color = color.withAlpha(255).toArgb()
                canvas.drawPath(path, fillPaint)

                // 3. Ultra Crisp White-Hot Laser Core
                val coreRadii = FloatArray(totalCount) { (extRadii[it] * 0.40f).coerceAtLeast(1.0f) }
                val corePath = buildStrokePath(extCenters, coreRadii, 0, totalCount, capStart = true, capEnd = true, texture = StrokeTexture.SMOOTH)
                corePaint.color = android.graphics.Color.WHITE
                canvas.drawPath(corePath, corePaint)
            }
            isPencil -> {
                fillPaint.color = color.copy(a = (color.a * 0.35).toInt().coerceIn(10, 255)).toArgb()
                canvas.drawPath(path, fillPaint)
                texturePaint.shader = getCspPencilShader()
                texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                texturePaint.alpha = (color.a * 0.95).toInt().coerceIn(0, 255)
                canvas.drawPath(path, texturePaint)
            }
            isCalligraphy -> {
                fillPaint.color = argb
                canvas.drawPath(path, fillPaint)
                getCspPenShader()?.let { penShader ->
                    texturePaint.shader = penShader
                    texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                    texturePaint.alpha = (color.a * 0.90).toInt().coerceIn(0, 255)
                    canvas.drawPath(path, texturePaint)
                }
            }
            isChalk -> {
                fillPaint.color = color.copy(a = (color.a * 0.82).toInt().coerceIn(20, 255)).toArgb()
                canvas.drawPath(path, fillPaint)
                texturePaint.shader = chalkShader
                texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                canvas.drawPath(path, texturePaint)
            }
            isCharcoal -> {
                fillPaint.color = color.copy(a = (color.a * 0.90).toInt().coerceIn(30, 255)).toArgb()
                canvas.drawPath(path, fillPaint)
                texturePaint.shader = charcoalShader
                texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                canvas.drawPath(path, texturePaint)
            }
            else -> {
                fillPaint.color = argb
                canvas.drawPath(path, fillPaint)
            }
        }
    }

    /**
     * Draws a live stroke whose settled head is cached and whose tail is rebuilt every frame.
     *
     * A stroke written slowly is thousands of points long, and the live path used to be re-splined
     * from the first point every frame, so one frame's work grew with the ink already down. Here
     * the head is appended in [LIVE_CACHE_BATCH]-point batches once and kept as immutable paths;
     * a frame rebuilds only the run from the last baked point to the predicted tip.
     *
     * Batches overlap by one point, and every flat end gets the disc a round nib would have left
     * there. Without it, two splines whose end tangents are one-sided (each batch derives its own)
     * could open a sub-pixel wedge at the joint. The discs also restore the round start cap that
     * `capStart = false` would otherwise cut flat.
     */
    private fun drawLiveInBatches(
        centers: FloatArray,
        radii: FloatArray,
        count: Int,
        argb: Int,
        predictedTail: List<Pt>?,
        settled: Int,
        cacheKey: Any,
    ) {
        if (cacheKey !== liveCacheKey) {
            liveCacheKey = cacheKey
            liveBatches.clear()
            liveBakedPoints = 0
        }
        while (settled - liveBakedPoints >= LIVE_CACHE_BATCH) {
            val next = liveBakedPoints + LIVE_CACHE_BATCH
            val from = (liveBakedPoints - 1).coerceAtLeast(0)
            val batch = buildStrokePath(centers, radii, from, next - from, capStart = false, capEnd = false)
                .apply { addJointDisc(centers, radii, from) }
            liveBatches += batch
            liveBakedPoints = next
        }

        fillPaint.color = argb
        for (batch in liveBatches) canvas.drawPath(batch, fillPaint)

        val from = (liveBakedPoints - 1).coerceAtLeast(0)
        val liveCount = count - from
        val live = if (predictedTail != null && predictedTail.isNotEmpty()) {
            val total = liveCount + predictedTail.size
            val extCenters = FloatArray(2 * total)
            val extRadii = FloatArray(total)
            System.arraycopy(centers, 2 * from, extCenters, 0, 2 * liveCount)
            System.arraycopy(radii, from, extRadii, 0, liveCount)
            val lastR = radii[from + liveCount - 1]
            for (j in predictedTail.indices) {
                val pt = predictedTail[j]
                extCenters[2 * (liveCount + j)] = pt.x.toFloat()
                extCenters[2 * (liveCount + j) + 1] = pt.y.toFloat()
                extRadii[liveCount + j] = lastR
            }
            buildStrokePath(extCenters, extRadii, 0, total, capStart = false, capEnd = true)
        } else {
            buildStrokePath(centers, radii, from, liveCount, capStart = false, capEnd = true)
        }.apply { addJointDisc(centers, radii, from) }
        canvas.drawPath(live, fillPaint)
    }

    /**
     * A round nib leaves a disc wherever two swept runs meet. Drawn counter-clockwise to match the
     * ribbon perimeter's own direction, so the non-zero fill unions the two instead of cancelling
     * the overlap into a hole.
     */
    private fun Path.addJointDisc(centers: FloatArray, radii: FloatArray, i: Int) {
        val r = radii[i]
        if (r.isFinite() && r > 0f) {
            addCircle(centers[2 * i], centers[2 * i + 1], r, Path.Direction.CCW)
        }
    }

    override fun fillDiskRibbon(
        centers: FloatArray,
        radii: FloatArray,
        from: Int,
        count: Int,
        color: Rgba,
        capStart: Boolean,
        capEnd: Boolean,
        texture: StrokeTexture,
    ) {
        if (count <= 0) return
        val end = from + count
        val argb = color.toArgb()

        // 1. Dotted brush: trace centerline as clean circular dots
        if (texture == StrokeTexture.DOTTED) {
            if (count >= 2) {
                val dotPath = Path()
                dotPath.moveTo(centers[2 * from], centers[2 * from + 1])
                for (i in from + 1 until end) dotPath.lineTo(centers[2 * i], centers[2 * i + 1])
                strokePaint.color = argb
                val avgR = if (radii.isNotEmpty()) radii[from] else 3f
                val dotGap = (avgR * 2.8f).coerceAtLeast(6f)
                strokePaint.strokeWidth = (avgR * 2f).coerceAtLeast(1.5f)
                strokePaint.pathEffect = DashPathEffect(floatArrayOf(0.1f, dotGap), 0f)
                strokePaint.strokeCap = Paint.Cap.ROUND
                canvas.drawPath(dotPath, strokePaint)
                strokePaint.pathEffect = null
                return
            }
        }

        val isChalk = texture == StrokeTexture.CHALK
        val isPencil = texture == StrokeTexture.PENCIL
        val isCharcoal = texture == StrokeTexture.CHARCOAL
        val isWatercolor = texture == StrokeTexture.WATERCOLOR
        val isCalligraphy = texture == StrokeTexture.CALLIGRAPHY

        // Star Notes continuous C1-smooth spline ribbon outline
        val path = buildStrokePath(centers, radii, from, count, capStart, capEnd, texture)

        when {
            isPencil -> {
                // 1. Soft graphite body: base translucent wash so paper tooth can peak through
                fillPaint.color = color.copy(a = (color.a * 0.35).toInt().coerceIn(10, 255)).toArgb()
                canvas.drawPath(path, fillPaint)

                // 2. Authentic Clip Studio Paint sketch paper tooth grain overlay
                texturePaint.shader = getCspPencilShader()
                texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                texturePaint.alpha = (color.a * 0.95).toInt().coerceIn(0, 255)
                canvas.drawPath(path, texturePaint)

                // 3. Authentic graphite particle texture stamp overlay (MaLiang pencil texture)
                getPencilStampShader()?.let { stampShader ->
                    texturePaint.shader = stampShader
                    texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                    texturePaint.alpha = (color.a * 0.85).toInt().coerceIn(0, 255)
                    canvas.drawPath(path, texturePaint)
                }
            }
            isCalligraphy -> {
                // 1. Solid deep ink core
                fillPaint.color = argb
                canvas.drawPath(path, fillPaint)

                // 2. Clip Studio Paint Textured G-Pen ink overlay (sumi ink texture & paper friction)
                getCspPenShader()?.let { penShader ->
                    texturePaint.shader = penShader
                    texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                    texturePaint.alpha = (color.a * 0.90).toInt().coerceIn(0, 255)
                    canvas.drawPath(path, texturePaint)
                }
            }
            isChalk -> {
                // Powdery chalk body
                fillPaint.color = color.copy(a = (color.a * 0.82).toInt().coerceIn(20, 255)).toArgb()
                canvas.drawPath(path, fillPaint)
                // Chalk texture shader
                texturePaint.shader = chalkShader
                texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                canvas.drawPath(path, texturePaint)
                // Scatter tiny chalk dust specks
                if (count > 2) {
                    for (i in from until end step 3) {
                        val x = centers[2 * i]
                        val y = centers[2 * i + 1]
                        val hash = kotlin.math.sin(x.toDouble() * 1.7 + y.toDouble() * 2.3).toFloat()
                        if (hash > 0.4f) {
                            val r = radii[i]
                            val dustX = x + hash * r * 1.22f
                            val dustY = y - hash * r * 1.22f
                            canvas.drawCircle(dustX, dustY, (0.8f / avgScale).coerceIn(0.5f, 2f), fillPaint)
                        }
                    }
                }
            }
            isCharcoal -> {
                // Deep organic charcoal body
                fillPaint.color = color.copy(a = (color.a * 0.90).toInt().coerceIn(30, 255)).toArgb()
                canvas.drawPath(path, fillPaint)
                // Fibrous charcoal texture shader
                texturePaint.shader = charcoalShader
                texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                canvas.drawPath(path, texturePaint)
            }
            isWatercolor -> {
                // 1. Translucent soft wash interior
                fillPaint.color = color.copy(a = (color.a * 0.30).toInt().coerceIn(10, 255)).toArgb()
                canvas.drawPath(path, fillPaint)

                // 2. Clip Studio Paint Watercolor / Gouache Wash granulation texture
                getCspBrushShader()?.let { brushShader ->
                    texturePaint.shader = brushShader
                    texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                    texturePaint.alpha = (color.a * 0.70).toInt().coerceIn(0, 255)
                    canvas.drawPath(path, texturePaint)
                }

                // 3. Darker pooled wet-edge contour
                strokePaint.color = color.copy(a = (color.a * 0.65).toInt().coerceIn(20, 255)).toArgb()
                strokePaint.strokeWidth = (1.5f / avgScale).coerceIn(1f, 3.5f)
                strokePaint.pathEffect = null
                strokePaint.strokeCap = Paint.Cap.ROUND
                canvas.drawPath(path, strokePaint)
            }
            else -> {
                // Smooth anti-aliased ribbon
                fillPaint.color = argb
                canvas.drawPath(path, fillPaint)
            }
        }
    }

    /** Ultra-fast 0ms cached path rendering for smooth 120 FPS panning and zero-blur vector scaling. */
    fun drawCachedPath(path: Path, color: Rgba, texture: StrokeTexture) {
        val argb = color.toArgb()
        val isPencil = texture == StrokeTexture.PENCIL
        val isCalligraphy = texture == StrokeTexture.CALLIGRAPHY
        val isChalk = texture == StrokeTexture.CHALK
        val isCharcoal = texture == StrokeTexture.CHARCOAL

        when {
            isPencil -> {
                fillPaint.color = color.copy(a = (color.a * 0.35).toInt().coerceIn(10, 255)).toArgb()
                canvas.drawPath(path, fillPaint)
                texturePaint.shader = getCspPencilShader()
                texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                texturePaint.alpha = (color.a * 0.95).toInt().coerceIn(0, 255)
                canvas.drawPath(path, texturePaint)
                getPencilStampShader()?.let { stampShader ->
                    texturePaint.shader = stampShader
                    texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                    texturePaint.alpha = (color.a * 0.85).toInt().coerceIn(0, 255)
                    canvas.drawPath(path, texturePaint)
                }
            }
            isCalligraphy -> {
                fillPaint.color = argb
                canvas.drawPath(path, fillPaint)
                getCspPenShader()?.let { penShader ->
                    texturePaint.shader = penShader
                    texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                    texturePaint.alpha = (color.a * 0.90).toInt().coerceIn(0, 255)
                    canvas.drawPath(path, texturePaint)
                }
            }
            isChalk -> {
                fillPaint.color = color.copy(a = (color.a * 0.82).toInt().coerceIn(20, 255)).toArgb()
                canvas.drawPath(path, fillPaint)
                texturePaint.shader = chalkShader
                texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                canvas.drawPath(path, texturePaint)
            }
            isCharcoal -> {
                fillPaint.color = color.copy(a = (color.a * 0.90).toInt().coerceIn(30, 255)).toArgb()
                canvas.drawPath(path, fillPaint)
                texturePaint.shader = charcoalShader
                texturePaint.colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
                canvas.drawPath(path, texturePaint)
            }
            else -> {
                fillPaint.color = argb
                canvas.drawPath(path, fillPaint)
            }
        }
    }

    // The blur radius is a page-space length: the canvas scale grows it with zoom,
    // so the halo tracks the ink (exactly like a non-cosmetic pen width). INNER blur
    // keeps the soft fill inside the shape (the white core); NORMAL spreads it both
    // ways (the outer halo).
    override fun fillPolygonGlow(pts: FloatArray, color: Rgba, rule: FillRule, blurRadius: Double, inner: Boolean) {
        if (pts.size < 6) return
        val path = buildPath(pts, close = true, rule)
        if (blurRadius <= 0.0) {
            fillPaint.color = color.toArgb()
            canvas.drawPath(path, fillPaint)
            return
        }
        glowPaint.color = color.toArgb()
        glowPaint.maskFilter = BlurMaskFilter(blurRadius.toFloat().coerceAtLeast(0.1f), blurStyle(inner))
        canvas.drawPath(path, glowPaint)
        glowPaint.maskFilter = null
    }

    override fun fillCircleGlow(center: Pt, radius: Double, color: Rgba, blurRadius: Double, inner: Boolean) {
        if (blurRadius <= 0.0) return fillCircle(center, radius, color)
        glowPaint.color = color.toArgb()
        glowPaint.maskFilter = BlurMaskFilter(blurRadius.toFloat().coerceAtLeast(0.1f), blurStyle(inner))
        canvas.drawCircle(center.x.toFloat(), center.y.toFloat(), radius.toFloat(), glowPaint)
        glowPaint.maskFilter = null
    }

    private fun blurStyle(inner: Boolean) =
        if (inner) BlurMaskFilter.Blur.INNER else BlurMaskFilter.Blur.NORMAL

    override fun fillEllipse(center: Pt, rx: Double, ry: Double, color: Rgba) {
        fillPaint.color = color.toArgb()
        canvas.drawOval(
            (center.x - rx).toFloat(), (center.y - ry).toFloat(),
            (center.x + rx).toFloat(), (center.y + ry).toFloat(),
            fillPaint,
        )
    }

    override fun strokeRect(rect: Rect, pen: Pen) {
        applyPen(pen)
        canvas.drawRect(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(), strokePaint)
    }

    override fun strokePolyline(points: List<Pt>, pen: Pen) {
        if (points.size < 2) return
        applyPen(pen)
        canvas.drawPath(buildPath(points, close = false, FillRule.NONZERO), strokePaint)
    }

    override fun strokePolyline(pts: FloatArray, pen: Pen) = strokePolyline(pts, 0, pts.size / 2, pen)

    override fun strokePolyline(pts: FloatArray, from: Int, count: Int, pen: Pen) {
        if (count < 2) return
        applyPen(pen)
        val path = Path()
        path.moveTo(pts[2 * from], pts[2 * from + 1])
        for (i in 1 until count) path.lineTo(pts[2 * (from + i)], pts[2 * (from + i) + 1])
        canvas.drawPath(path, strokePaint)
    }

    override fun strokePolygon(points: List<Pt>, pen: Pen) {
        if (points.size < 2) return
        applyPen(pen)
        canvas.drawPath(buildPath(points, close = true, FillRule.NONZERO), strokePaint)
    }

    override fun strokeEllipse(center: Pt, rx: Double, ry: Double, pen: Pen) {
        applyPen(pen)
        canvas.drawOval(
            (center.x - rx).toFloat(), (center.y - ry).toFloat(),
            (center.x + rx).toFloat(), (center.y + ry).toFloat(),
            strokePaint,
        )
    }

    override fun drawRaster(raster: RasterSurface, dest: Rect, src: Rect?) {
        val bmp = (raster as? AndroidRasterSurface)?.bitmap ?: return
        if (bmp.isRecycled) return
        val srcRect = src?.let {
            android.graphics.Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt())
        }
        val destRect = RectF(dest.left.toFloat(), dest.top.toFloat(), dest.right.toFloat(), dest.bottom.toFloat())
        canvas.drawBitmap(bmp, srcRect, destRect, bitmapPaint)
    }

    // MULTIPLY uses the W3C separable blend (API 29+); below that (and for SRC_OVER) it falls back
    // to plain alpha compositing, matching saveLayerBlended. Composites a pre-rendered highlighter
    // ribbon onto the live page each frame in one blit, no per-frame ribbon tessellation.
    override fun drawRasterBlended(raster: RasterSurface, dest: Rect, alpha: Double, blend: BlendMode, src: Rect?) {
        val bmp = (raster as? AndroidRasterSurface)?.bitmap ?: return
        if (bmp.isRecycled) return
        rasterBlendPaint.reset()
        rasterBlendPaint.isFilterBitmap = true
        rasterBlendPaint.isDither = true
        rasterBlendPaint.alpha = (alpha.coerceIn(0.0, 1.0) * 255).toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            nativeBlend(blend)?.let { rasterBlendPaint.blendMode = it }
        } else {
            val pdMode = when (blend) {
                BlendMode.MULTIPLY -> android.graphics.PorterDuff.Mode.MULTIPLY
                BlendMode.SCREEN -> android.graphics.PorterDuff.Mode.SCREEN
                BlendMode.DARKEN -> android.graphics.PorterDuff.Mode.DARKEN
                BlendMode.SRC_OVER -> null
            }
            if (pdMode != null) {
                rasterBlendPaint.xfermode = android.graphics.PorterDuffXfermode(pdMode)
            }
        }
        val srcRect = src?.let {
            android.graphics.Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt())
        }
        val destRect = RectF(dest.left.toFloat(), dest.top.toFloat(), dest.right.toFloat(), dest.bottom.toFloat())
        canvas.drawBitmap(bmp, srcRect, destRect, rasterBlendPaint)
    }

    // Decode the source only to the destination's device-pixel size (capped, never upscaled past the
    // source) so a huge photo never fully decodes; the quarter turn and the free angle are applied
    // as one canvas rotation about the destination centre (they share it, so they simply add), the
    // rect already carrying the quarter-turned (w/h-swapped) box.
    override fun drawImage(image: ImageData, dest: Rect, orientation: Int, angle: Double) {
        if (dest.w <= 0.0 || dest.h <= 0.0) return
        val devW = (dest.w * scaleX).toInt()
        val devH = (dest.h * scaleY).toInt()
        val o = ((orientation % 360) + 360) % 360
        if (o == 0 && angle == 0.0 && drawVectorSlice(image, dest, devW, devH)) return
        val turned = o == 90 || o == 270
        val reqW = (if (turned) devH else devW).coerceIn(1, DECODE_CAP_PX)
        val reqH = (if (turned) devW else devH).coerceIn(1, DECODE_CAP_PX)
        val bmp = ImageDecoder.decodeSampledFile(image.file.path, reqW, reqH) ?: return
        val uw = (if (turned) dest.h else dest.w).toFloat()
        val uh = (if (turned) dest.w else dest.h).toFloat()
        val degrees = o + Math.toDegrees(angle)
        canvas.save()
        canvas.translate(((dest.left + dest.right) / 2.0).toFloat(), ((dest.top + dest.bottom) / 2.0).toFloat())
        if (degrees != 0.0) canvas.rotate(degrees.toFloat())
        canvas.drawBitmap(bmp, null, RectF(-uw / 2f, -uh / 2f, uw / 2f, uh / 2f), bitmapPaint)
        canvas.restore()
    }

    /**
     * Draw an unrotated vector image by rasterizing only the part the clip can show. The clip is
     * this canvas's own, so the destination surface bounds it: the sharp viewport asks for the
     * visible sliver instead of the whole document at a 4096 px bucket. Returns false when the
     * image isn't a vector (or the slice failed) so the caller falls back to the raster path.
     */
    private fun drawVectorSlice(image: ImageData, dest: Rect, devW: Int, devH: Int): Boolean {
        val path = image.file.path
        if (!ImageDecoder.isVector(path)) return false
        if (!canvas.getClipBounds(clipBounds)) return true // clipped away entirely
        val l = maxOf(dest.left, clipBounds.left.toDouble())
        val t = maxOf(dest.top, clipBounds.top.toDouble())
        val r = minOf(dest.right, clipBounds.right.toDouble())
        val b = minOf(dest.bottom, clipBounds.bottom.toDouble())
        if (r <= l || b <= t) return true
        val slice = ImageDecoder.renderVectorSlice(
            path,
            (l - dest.left) / dest.w, (t - dest.top) / dest.h,
            (r - dest.left) / dest.w, (b - dest.top) / dest.h,
            devW, devH,
        ) ?: return false
        canvas.drawBitmap(
            slice.bitmap,
            null,
            RectF(
                (dest.left + slice.u0 * dest.w).toFloat(),
                (dest.top + slice.v0 * dest.h).toFloat(),
                (dest.left + slice.u1 * dest.w).toFloat(),
                (dest.top + slice.v1 * dest.h).toFloat(),
            ),
            bitmapPaint,
        )
        return true
    }

    override fun drawText(text: String, rect: Rect, font: FontSpec, color: Rgba, flags: TextFlags) {
        if (text.isEmpty()) return
        val paint = AndroidText.textPaint(font, color.toArgb())
        val innerWidth = (rect.w - 12.0).toInt().coerceAtLeast(1)
        val layout = AndroidText.layout(text, innerWidth, paint)
        canvas.save()
        canvas.translate((rect.left + 6.0).toFloat(), (rect.top + 4.0).toFloat())
        layout.draw(canvas)
        canvas.restore()
    }

    override fun drawTextRun(text: String, x: Double, baseline: Double, font: FontSpec, color: Rgba) {
        if (text.isEmpty()) return
        val paint = AndroidText.textPaint(font, color.toArgb())
        canvas.drawText(text, x.toFloat(), baseline.toFloat(), paint)
    }

    private fun applyPen(pen: Pen) {
        strokePaint.color = pen.color.toArgb()
        val width = if (pen.cosmetic) (pen.width / avgScale) else pen.width
        strokePaint.strokeWidth = width.toFloat()
        // A page-space outward blur for the neon halo on shape outlines (NORMAL = both sides).
        strokePaint.maskFilter = if (pen.glowRadius > 0.0) {
            BlurMaskFilter(pen.glowRadius.toFloat().coerceAtLeast(0.1f), BlurMaskFilter.Blur.NORMAL)
        } else {
            null
        }
        strokePaint.pathEffect = if (pen.dashed) {
            // Cosmetic dashes (chrome) keep a fixed device-pixel rhythm; content dashes (the
            // dashed pen) are specified in content px and so scale with zoom like the ink.
            val s = if (pen.cosmetic) 1.0 / avgScale else 1.0
            val on = (pen.dashOn * s).toFloat().coerceAtLeast(0.1f)
            val off = (pen.dashGap * s).toFloat().coerceAtLeast(0.1f)
            DashPathEffect(floatArrayOf(on, off), (pen.dashPhase * s).toFloat())
        } else {
            null
        }
    }

    private fun buildPath(points: List<Pt>, close: Boolean, rule: FillRule): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.fillType = if (rule == FillRule.EVEN_ODD) Path.FillType.EVEN_ODD else Path.FillType.WINDING
        path.moveTo(points[0].x.toFloat(), points[0].y.toFloat())
        for (i in 1 until points.size) path.lineTo(points[i].x.toFloat(), points[i].y.toFloat())
        if (close) path.close()
        return path
    }

    private fun buildPath(pts: FloatArray, close: Boolean, rule: FillRule): Path {
        val path = Path()
        if (pts.size < 2) return path
        path.fillType = if (rule == FillRule.EVEN_ODD) Path.FillType.EVEN_ODD else Path.FillType.WINDING
        path.moveTo(pts[0], pts[1])
        for (i in 1 until pts.size / 2) path.lineTo(pts[2 * i], pts[2 * i + 1])
        if (close) path.close()
        return path
    }

    companion object {
        /** Long-edge cap (device px) for an on-screen image decode, so extreme zoom can't OOM. */
        private const val DECODE_CAP_PX = 4096

        /**
         * Ribbon points baked into one immutable path before the live head is cached again.
         * It trades draw calls for path-building: a stroke of n points is drawn as about n / this
         * paths, but no frame ever re-splines more than this plus the pen's lookahead.
         */
        const val LIVE_CACHE_BATCH = 128

        /** Pre-builds a complete Hardware GPU vector path for a stroke geometry once. */
        fun buildStrokePath(
            centers: FloatArray,
            radii: FloatArray,
            capStart: Boolean = true,
            capEnd: Boolean = true,
            texture: StrokeTexture = StrokeTexture.SMOOTH,
        ): Path = buildStrokePath(centers, radii, 0, minOf(centers.size / 2, radii.size), capStart, capEnd, texture)

        /**
         * Pure Pixel-Level Swept-Disc Capsule Inking Engine.
         * Generates continuous sub-pixel hardware capsule & circular disc stamps (0.4-0.8px spacing):
         *   1. Every segment is stamped in sub-pixel increments with exact Clockwise bitangent capsules.
         *   2. Every node is stamped with an exact Clockwise circular disc.
         *
         * Guarantees:
         * 1. Direct pixel-level ink flow with zero polygon/lineTo faceting or straight line chords.
         * 2. 100% round circular domes on start, end, 180° turns, loops, and corners.
         * 3. Completely solid Skia WINDING fill without any white holes, notches, or caterpillar ridges.
         */
        fun buildStrokePath(
            centers: FloatArray,
            radii: FloatArray,
            from: Int,
            count: Int,
            capStart: Boolean = true,
            capEnd: Boolean = true,
            texture: StrokeTexture = StrokeTexture.SMOOTH,
        ): Path {
            val path = Path().apply { fillType = Path.FillType.WINDING }
            if (count <= 0) return path
            if (count == 1) {
                val cx = centers[2 * from]
                val cy = centers[2 * from + 1]
                val cr = radii[from].coerceAtLeast(0.05f)
                if (cx.isFinite() && cy.isFinite() && cr.isFinite() && cr > 0f) {
                    path.addCircle(cx, cy, cr, Path.Direction.CW)
                }
                return path
            }

            val end = from + count

            // 1. Evaluate smooth continuous cubic Hermite spline centerline & radius samples
            var maxEstimatedPoints = count * 32
            var xPts = DoubleArray(maxEstimatedPoints)
            var yPts = DoubleArray(maxEstimatedPoints)
            var rPts = DoubleArray(maxEstimatedPoints)
            var totalPts = 0

            fun addPt(x: Double, y: Double, r: Double) {
                if (totalPts >= xPts.size) {
                    val newCap = xPts.size * 2
                    xPts = xPts.copyOf(newCap)
                    yPts = yPts.copyOf(newCap)
                    rPts = rPts.copyOf(newCap)
                }
                xPts[totalPts] = x
                yPts[totalPts] = y
                rPts[totalPts] = r
                totalPts++
            }

            for (i in from until end - 1) {
                val x1 = centers[2 * i].toDouble()
                val y1 = centers[2 * i + 1].toDouble()
                val r1 = radii[i].toDouble().coerceAtLeast(0.05)
                val x2 = centers[2 * (i + 1)].toDouble()
                val y2 = centers[2 * (i + 1) + 1].toDouble()
                val r2 = radii[i + 1].toDouble().coerceAtLeast(0.05)

                if (x1.isNaN() || y1.isNaN() || x2.isNaN() || y2.isNaN()) continue

                val dx1 = x2 - x1
                val dy1 = y2 - y1
                val len1 = kotlin.math.hypot(dx1, dy1)
                if (len1 < 1e-4) continue

                val (dx0, dy0, len0) = if (i > from) {
                    val px = centers[2 * (i - 1)].toDouble()
                    val py = centers[2 * (i - 1) + 1].toDouble()
                    val vx = x1 - px; val vy = y1 - py
                    Triple(vx, vy, kotlin.math.hypot(vx, vy))
                } else {
                    Triple(dx1, dy1, len1)
                }

                val (dx2, dy2, len2) = if (i + 2 < end) {
                    val nx = centers[2 * (i + 2)].toDouble()
                    val ny = centers[2 * (i + 2) + 1].toDouble()
                    val vx = nx - x2; val vy = ny - y2
                    Triple(vx, vy, kotlin.math.hypot(vx, vy))
                } else {
                    Triple(dx1, dy1, len1)
                }

                val dir1x = (dx0 / len0.coerceAtLeast(1e-4)) + (dx1 / len1)
                val dir1y = (dy0 / len0.coerceAtLeast(1e-4)) + (dy1 / len1)
                val dir1Len = kotlin.math.hypot(dir1x, dir1y).coerceAtLeast(1e-4)
                val mag1 = len1 * (if (len0 < len1) 0.75 else 0.90)
                val t1x = (dir1x / dir1Len) * mag1
                val t1y = (dir1y / dir1Len) * mag1

                val dir2x = (dx1 / len1) + (dx2 / len2.coerceAtLeast(1e-4))
                val dir2y = (dy1 / len1) + (dy2 / len2.coerceAtLeast(1e-4))
                val dir2Len = kotlin.math.hypot(dir2x, dir2y).coerceAtLeast(1e-4)
                val mag2 = len1 * (if (len2 < len1) 0.75 else 0.90)
                val t2x = (dir2x / dir2Len) * mag2
                val t2y = (dir2y / dir2Len) * mag2

                val minR = kotlin.math.min(r1, r2)
                val stepLen = kotlin.math.max(0.40, minR * 0.25)
                val steps = kotlin.math.max(1, kotlin.math.ceil(len1 / stepLen).toInt()).coerceAtMost(48)

                val startS = if (totalPts == 0) 0 else 1
                for (s in startS..steps) {
                    val u = s.toDouble() / steps
                    val u2 = u * u; val u3 = u2 * u
                    val h00 = 2.0 * u3 - 3.0 * u2 + 1.0
                    val h10 = u3 - 2.0 * u2 + u
                    val h01 = -2.0 * u3 + 3.0 * u2
                    val h11 = u3 - u2
                    val curX = h00 * x1 + h10 * t1x + h01 * x2 + h11 * t2x
                    val curY = h00 * y1 + h10 * t1y + h01 * y2 + h11 * t2y
                    val curR = (r1 + (r2 - r1) * (3.0 * u2 - 2.0 * u3)).coerceAtLeast(0.05)
                    addPt(curX, curY, curR)
                }
            }

            if (totalPts < 2) return path

            // 2. Compute smooth left and right rail envelope coordinates
            val lx = FloatArray(totalPts)
            val ly = FloatArray(totalPts)
            val rx = FloatArray(totalPts)
            val ry = FloatArray(totalPts)

            for (k in 0 until totalPts) {
                val x = xPts[k]
                val y = yPts[k]
                val r = rPts[k]

                val tx = when {
                    k == 0 -> xPts[1] - xPts[0]
                    k == totalPts - 1 -> xPts[totalPts - 1] - xPts[totalPts - 2]
                    else -> (xPts[k + 1] - xPts[k - 1]) * 0.5
                }
                val ty = when {
                    k == 0 -> yPts[1] - yPts[0]
                    k == totalPts - 1 -> yPts[totalPts - 1] - yPts[totalPts - 2]
                    else -> (yPts[k + 1] - yPts[k - 1]) * 0.5
                }
                val tLen = kotlin.math.hypot(tx, ty)
                val nx = if (tLen > 1e-4) -ty / tLen else 0.0
                val ny = if (tLen > 1e-4) tx / tLen else 1.0

                lx[k] = (x + nx * r).toFloat()
                ly[k] = (y + ny * r).toFloat()
                rx[k] = (x - nx * r).toFloat()
                ry[k] = (y - ny * r).toFloat()
            }

            // 3. Build single continuous outer perimeter (Zero interior quad slits / zero serration / 100% clean solid ink)
            path.moveTo(lx[0], ly[0])

            // Left rail forward
            for (k in 1 until totalPts) {
                path.lineTo(lx[k], ly[k])
            }

            // Round end-cap dome from Left[last] to Right[last]
            val lastIdx = totalPts - 1
            if (capEnd && rPts[lastIdx] > 0.1) {
                val tipX = xPts[lastIdx]
                val tipY = yPts[lastIdx]
                val tipR = rPts[lastIdx]
                val tMx = xPts[lastIdx] - xPts[lastIdx - 1]
                val tMy = yPts[lastIdx] - yPts[lastIdx - 1]
                val tLen = kotlin.math.hypot(tMx, tMy)
                val normTx = if (tLen > 1e-4) tMx / tLen else 1.0
                val normTy = if (tLen > 1e-4) tMy / tLen else 0.0

                val domeFwd = tipR * 1.3333333333333333
                val c1x = (lx[lastIdx] + normTx * domeFwd).toFloat()
                val c1y = (ly[lastIdx] + normTy * domeFwd).toFloat()
                val c2x = (rx[lastIdx] + normTx * domeFwd).toFloat()
                val c2y = (ry[lastIdx] + normTy * domeFwd).toFloat()
                path.cubicTo(c1x, c1y, c2x, c2y, rx[lastIdx], ry[lastIdx])
            } else {
                path.lineTo(rx[lastIdx], ry[lastIdx])
            }

            // Right rail backward
            for (k in lastIdx - 1 downTo 0) {
                path.lineTo(rx[k], ry[k])
            }

            // Round start-cap dome from Right[0] to Left[0]
            if (capStart && rPts[0] > 0.1) {
                val startX = xPts[0]
                val startY = yPts[0]
                val startR = rPts[0]
                val t0x = xPts[1] - xPts[0]
                val t0y = yPts[1] - yPts[0]
                val tLen = kotlin.math.hypot(t0x, t0y)
                val normTx = if (tLen > 1e-4) t0x / tLen else 1.0
                val normTy = if (tLen > 1e-4) t0y / tLen else 0.0

                val domeBack = startR * 1.3333333333333333
                val c1x = (rx[0] - normTx * domeBack).toFloat()
                val c1y = (ry[0] - normTy * domeBack).toFloat()
                val c2x = (lx[0] - normTx * domeBack).toFloat()
                val c2y = (ly[0] - normTy * domeBack).toFloat()
                path.cubicTo(c1x, c1y, c2x, c2y, lx[0], ly[0])
            }

            path.close()
            return path

            return path
        }

        @Volatile
        var pencilBitmap: Bitmap? = null
        @Volatile
        var paperGrainBitmap: Bitmap? = null
        @Volatile
        var cspPenBitmap: Bitmap? = null
        @Volatile
        var cspPencilBitmap: Bitmap? = null
        @Volatile
        var cspBrushBitmap: Bitmap? = null

        fun initTextures(context: Context) {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            if (pencilBitmap == null) {
                try {
                    pencilBitmap = BitmapFactory.decodeResource(context.resources, com.pnotes.R.drawable.texture_pencil, opts)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            if (paperGrainBitmap == null) {
                try {
                    paperGrainBitmap = BitmapFactory.decodeResource(context.resources, com.pnotes.R.drawable.texture_paper_grain, opts)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            if (cspPenBitmap == null) {
                try {
                    cspPenBitmap = BitmapFactory.decodeResource(context.resources, com.pnotes.R.drawable.texture_csp_pen, opts)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            if (cspPencilBitmap == null) {
                try {
                    cspPencilBitmap = BitmapFactory.decodeResource(context.resources, com.pnotes.R.drawable.texture_csp_pencil, opts)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            if (cspBrushBitmap == null) {
                try {
                    cspBrushBitmap = BitmapFactory.decodeResource(context.resources, com.pnotes.R.drawable.texture_csp_brush, opts)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }
}
