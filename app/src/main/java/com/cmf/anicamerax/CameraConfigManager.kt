package com.cmf.anicamerax

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture

class CameraConfigManager {
    suspend fun getProfile(lensFacing: Int): CameraProfile {
        return when (lensFacing) {
            CameraSelector.LENS_FACING_BACK -> CameraProfile(
                captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
                jpegQuality = 98
            )
            CameraSelector.LENS_FACING_FRONT -> CameraProfile(
                captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                jpegQuality = 95
            )
            else -> CameraProfile()
        }
    }
}