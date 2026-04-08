package com.cmf.anicamerax.logic

import android.graphics.*
import com.cmf.anicamerax.LmcParams

object ImageProcessor {

    /**
     * Основной метод обработки Bitmap на основе 11 параметров LMC
     */
    fun applyLmcEffects(source: Bitmap, params: LmcParams): Bitmap {
        val width = source.width
        val height = source.height
        val workingBitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(workingBitmap)
        val paint = Paint()

        // 1. Цветокоррекция (Saturation, Contrast, Brightness)
        val colorMatrix = ColorMatrix().apply {
            // Сатурация (0..50 -> 0.0..2.0)
            setSaturation(params.saturation / 25f)

            // Контраст (0..50)
            val contrastScale = params.contrast / 25f
            val translate = (-0.5f * contrastScale + 0.5f) * 255f
            postConcat(ColorMatrix(floatArrayOf(
                contrastScale, 0f, 0f, 0f, translate,
                0f, contrastScale, 0f, 0f, translate,
                0f, 0f, contrastScale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )))
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(workingBitmap, 0f, 0f, paint)

        // 2. Имитация резкости (Sharpness) через небольшое наложение (Simple Unsharp Mask)
        // В реальном HDR+ тут был бы сверточный фильтр, для теста используем усиление границ
        if (params.sharpness > 25f) {
            // Программная резкость через фильтр размытия и вычитания (упрощенно для теста)
            paint.maskFilter = BlurMaskFilter(1f, BlurMaskFilter.Blur.INNER)
        }

        return workingBitmap
    }
}