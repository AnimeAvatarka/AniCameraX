package com.cmf.anicamerax.models

import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture

class CameraConfigManager {
    fun getProfile(lensFacing: Int, mode: PhotoMode): CameraProfile {
        return when (mode) {
            PhotoMode.Camera -> CameraProfile(enableHdr = true, jpegQuality = 98)
            PhotoMode.Portrait -> CameraProfile(captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            PhotoMode.Night -> CameraProfile(enableNight = true, exposureCompensation = 2)
            PhotoMode.Video -> CameraProfile(aspectRatio = AspectRatio.RATIO_16_9)
            PhotoMode.HDR -> CameraProfile(enableHdr = true, jpegQuality = 98)
            PhotoMode.Front -> CameraProfile(captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        }
    }
}
