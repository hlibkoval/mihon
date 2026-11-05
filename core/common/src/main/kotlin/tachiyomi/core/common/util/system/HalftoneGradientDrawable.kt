package tachiyomi.core.common.util.system

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.core.graphics.createBitmap
import kotlin.math.PI
import kotlin.math.cos

/**
 * A top-to-bottom gradient from [startColor] to [endColor] drawn as halftone dots using only
 * those two colors, so it doesn't band, even on e-ink screens with few grey levels. Solid top
 * and bottom thirds, with the dots shrinking across the middle third.
 *
 * @param dotPitch the period of the dot pattern in pixels.
 */
class HalftoneGradientDrawable(
    @ColorInt private val startColor: Int,
    @ColorInt private val endColor: Int,
    dotPitch: Int,
) : Drawable() {

    private val pitch = dotPitch.coerceAtLeast(MIN_DOT_PITCH)

    /** Per pixel of one pattern cell, the coverage at which it turns [startColor]. */
    private val thresholds: FloatArray = buildThresholds(pitch)

    private val paint = Paint().apply { isFilterBitmap = false }
    private var drawAlpha = 255

    private val shaderMatrix = Matrix()
    private var shader: BitmapShader? = null
    private var bandTop = 0
    private var bandBottom = 0

    override fun onBoundsChange(bounds: Rect) {
        bandTop = bounds.top + bounds.height() / 3
        bandBottom = bounds.top + bounds.height() * 2 / 3
        val bandHeight = bandBottom - bandTop
        shader = if (bandHeight > 0) {
            BitmapShader(buildBand(bandHeight), Shader.TileMode.REPEAT, Shader.TileMode.CLAMP).apply {
                shaderMatrix.setTranslate(bounds.left.toFloat(), bandTop.toFloat())
                setLocalMatrix(shaderMatrix)
            }
        } else {
            null
        }
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        val left = bounds.left.toFloat()
        val right = bounds.right.toFloat()

        paint.shader = null
        paint.setColorWithAlpha(startColor)
        canvas.drawRect(left, bounds.top.toFloat(), right, bandTop.toFloat(), paint)
        paint.setColorWithAlpha(endColor)
        canvas.drawRect(left, bandBottom.toFloat(), right, bounds.bottom.toFloat(), paint)

        val shader = shader ?: return
        // The shader supplies the colors, the paint only the alpha
        paint.setColorWithAlpha(0xFF000000.toInt())
        paint.shader = shader
        canvas.drawRect(left, bandTop.toFloat(), right, bandBottom.toFloat(), paint)
    }

    /** The transition band, one pattern cell wide, repeated horizontally. */
    private fun buildBand(height: Int): Bitmap {
        val pixels = IntArray(pitch * height)
        for (y in 0..<height) {
            val coverage = 1f - (y + 0.5f) / height
            val row = (y % pitch) * pitch
            for (x in 0..<pitch) {
                pixels[y * pitch + x] = if (thresholds[row + x] < coverage) startColor else endColor
            }
        }
        return createBitmap(pitch, height).apply { setPixels(pixels, 0, pitch, 0, 0, pitch, height) }
    }

    private fun Paint.setColorWithAlpha(@ColorInt color: Int) {
        this.color = color
        this.alpha = color.alpha * drawAlpha / 255
    }

    override fun setAlpha(alpha: Int) {
        if (drawAlpha != alpha) {
            drawAlpha = alpha
            invalidateSelf()
        }
    }

    override fun getAlpha(): Int = drawAlpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int =
        if (drawAlpha == 255 && startColor.alpha == 255 && endColor.alpha == 255) {
            PixelFormat.OPAQUE
        } else {
            PixelFormat.TRANSLUCENT
        }

    private companion object {
        const val MIN_DOT_PITCH = 4

        /** Ranks the pixels of a cell by the spot function cos(x)·cos(y), dot centers first. */
        fun buildThresholds(pitch: Int): FloatArray {
            val size = pitch * pitch
            val spot = FloatArray(size) { i ->
                val x = i % pitch + 0.5
                val y = i / pitch + 0.5
                (cos(2 * PI * x / pitch) * cos(2 * PI * y / pitch)).toFloat()
            }
            val thresholds = FloatArray(size)
            (0..<size).sortedByDescending { spot[it] }
                .forEachIndexed { rank, i -> thresholds[i] = (rank + 0.5f) / size }
            return thresholds
        }
    }
}
