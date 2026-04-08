package com.cmf.anicamerax.logic

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.cmf.anicamerax.LmcParams

// Используем псевдонимы, чтобы убрать "Redundant qualifier"
import android.hardware.camera2.CaptureRequest as Camera2Request
import android.hardware.camera2.CameraMetadata as Camera2Metadata

@Suppress("SpellCheckingInspection")
class CameraManager(private val context: Context) {
    private var imageCapture: ImageCapture? = null

    @OptIn(ExperimentalCamera2Interop::class, ExperimentalZeroShutterLag::class)
    private fun buildImageCapture(params: LmcParams): ImageCapture {
        val builder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)

        val extender = Camera2Interop.Extender(builder)

        // Работаем с Float из LmcParams
        if (params.sharpness > 0f) {
            extender.setCaptureRequestOption<Int>(
                Camera2Request.EDGE_MODE,
                Camera2Metadata.EDGE_MODE_HIGH_QUALITY
            )
        }

        if (params.denoise > 0f) {
            extender.setCaptureRequestOption<Int>(
                Camera2Request.NOISE_REDUCTION_MODE,
                Camera2Metadata.NOISE_REDUCTION_MODE_HIGH_QUALITY
            )
        }

        // ВАЖНО: сохраняем в переменную класса, чтобы takePhoto видел объект
        return builder.build().also { imageCapture = it }
    }

    @OptIn(ExperimentalCamera2Interop::class, ExperimentalZeroShutterLag::class)
    fun startCamera(
        owner: LifecycleOwner,
        previewView: PreviewView,
        params: LmcParams
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }

            val capture = buildImageCapture(params)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                provider.unbindAll()
                provider.bindToLifecycle(owner, cameraSelector, preview, capture)
            } catch (exception: Exception) {
                Log.e("AniCameraX", "Binding failed: ${exception.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(onPhotoCaptured: (Uri?) -> Unit) {
        val capture = imageCapture ?: run {
            Log.e("AniCameraX", "ImageCapture is null!")
            return
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "ANI_${System.currentTimeMillis()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/AniCameraX")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onPhotoCaptured(outputFileResults.savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("AniCameraX", "Capture error: ${exception.message}")
                    onPhotoCaptured(null)
                }
            }
        )
    }
}