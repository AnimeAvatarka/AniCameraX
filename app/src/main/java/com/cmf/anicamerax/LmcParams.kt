package com.cmf.anicamerax

data class LmcParams(
    val sharpness: Float,
    val denoise: Float,
    val saturation: Float = 1.0f, // Добавили новые поля
    val contrast: Float = 1.0f,
    val hdrBoost: Float = 1.0f
)