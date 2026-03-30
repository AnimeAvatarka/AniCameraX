package com.cmf.anicamerax

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.cmf.anicamerax.ui.theme.AniCameraXTheme
import com.cmf.anicamerax.CameraScreen
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private var imageCapture: ImageCapture? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var outputDirectory: File
    private var photoMode: PhotoMode = PhotoMode.Light
    private var expanded: Boolean = false
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            AniCameraXTheme {
                CameraScreen(
                    onTakePhoto = { takePhoto() },
                    onSwitchCamera = { switchCamera() },
                    selectedMode = photoMode,
                    onModeChange = { photoMode = it },
                    isDropdownExpanded = expanded,
                    onDropdownExpand = { expanded = it },
                    onPreviewViewReady = { previewView ->
                        startCamera(previewView, this@MainActivity, cameraExecutor)
                    }
                )
            }
        }

        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Разрешение на камеру необходимо",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        executor: ExecutorService
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val builder = ImageCapture.Builder()

                when (photoMode) {
                    PhotoMode.High ->
                        builder.setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    PhotoMode.Human ->
                        builder.setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    else -> Unit
                }

                val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display?.rotation ?: Surface.ROTATION_0
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.rotation
                }
                builder.setTargetRotation(rotation)

                imageCapture = builder.build()

                val preview = Preview.Builder().build()

                runOnUiThread {
                    previewView.surfaceProvider?.let {
                        preview.setSurfaceProvider(it)
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture!!)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка при инициализации камеры", e)
            }
        }, executor)
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: run {
            Log.w("MainActivity", "takePhoto: imageCapture is null")
            return
        }

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${getString(R.string.app_name)}")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        Toast.makeText(baseContext, "Фото сохранено", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("MainActivity", "Ошибка при съёмке", exception)
                    runOnUiThread {
                        Toast.makeText(
                            baseContext,
                            "Ошибка: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    private fun getOutputDirectory(): File {
        val appName = getString(R.string.app_name)
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, appName).apply {
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}