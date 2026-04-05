package com.cmf.anicamerax.models

import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture

data class CameraProfile(
    val aspectRatio: Int = AspectRatio.RATIO_4_3,
    val jpegQuality: Int = 95,
    val captureMode: Int = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
    val exposureCompensation: Int = 0,
    val enableHdr: Boolean = false,
    val enableNight: Boolean = false,
    val saturation: Float = 1.0f,
    val contrast: Float = 1.0f,
    val sharpness: Float = 1.0f
)
