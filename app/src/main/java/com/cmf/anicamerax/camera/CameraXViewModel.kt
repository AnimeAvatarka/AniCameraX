package com.cmf.anicamerax.camera

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService

class CameraXViewModel : ViewModel() {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null

    val imageCapture = mutableStateOf<ImageCapture?>(null)
    val preview = mutableStateOf<Preview?>(null)

    fun initializeCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        executor: ExecutorService,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK
    ) {
        viewModelScope.launch {
            try {
                val provider = ProcessCameraProvider.getInstance(previewView.context).get()
                cameraProvider = provider

                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // Preview
                val previewBuilder = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                preview.value = previewBuilder  // ✅ Правильное присвоение

                // ImageCapture
                val imageCaptureBuilder = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setJpegQuality(98)
                    .build()
                imageCapture.value = imageCaptureBuilder  // ✅ Правильное присвоение

                camera = provider.bindToLifecycle(
                    lifecycleOwner,  // ✅ LifecycleOwner параметр
                    selector,
                    preview.value,
                    imageCapture.value
                )
            } catch (e: Exception) {
                android.util.Log.e("CameraXViewModel", "Ошибка инициализации", e)
            }
        }
    }

    fun takePhoto(
        context: Context,
        executor: ExecutorService,
        onSaved: () -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture.value ?: return

        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AniCameraX")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        // ✅ Правильный порядок параметров takePicture
        capture.takePicture(
            outputOptions,
            executor,  // Executor перед callback
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved()
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception.message ?: "Unknown error")
                }
            }
        )
    }

    fun switchCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        executor: ExecutorService
    ) {
        val currentLens = camera?.cameraInfo?.lensFacing ?: CameraSelector.LENS_FACING_BACK
        val newLens = if (currentLens == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        cameraProvider?.unbindAll()
        initializeCamera(previewView, lifecycleOwner, executor, newLens)
    }

    override fun onCleared() {
        super.onCleared()
        cameraProvider?.unbindAll()
    }
}