package com.cmf.anicamerax

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.cmf.anicamerax.models.PhotoMode
import com.cmf.anicamerax.ui.theme.AniCameraXTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Разрешение на камеру необходимо", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        setContent {
            AniCameraXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainCameraScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun MainCameraScreen() {
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var photoMode by remember { mutableStateOf(PhotoMode.Camera) }
    var showAdvanced by remember { mutableStateOf(false) }
    var isHdrEnabled by remember { mutableStateOf(true) }

    if (showAdvanced) {
        AdvancedSettingsScreen(onBack = { showAdvanced = false })
    } else {
        CameraScreenWithPreview(
            imageCapture = imageCapture,
            onImageCaptureChange = { imageCapture = it },
            cameraSelector = cameraSelector,
            photoMode = photoMode,
            onTakePhoto = {
                val capture = imageCapture ?: run {
                    Toast.makeText(context, "Камера не готова", Toast.LENGTH_SHORT).show()
                    return@CameraScreenWithPreview
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AniCameraX")
                    }
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
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Toast.makeText(context, "✅ Фото сохранено", Toast.LENGTH_SHORT).show()
                        }
                        override fun onError(exception: ImageCaptureException) {
                            Log.e("Camera", "Ошибка: ${exception.message}", exception)
                            Toast.makeText(context, "❌ Ошибка: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            },
            onSwitchCamera = {
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            },
            onModeChange = { photoMode = it },
            onOpenAdvanced = { showAdvanced = true },
            isHdrEnabled = isHdrEnabled,
            onHdrToggle = { isHdrEnabled = !isHdrEnabled }
        )
    }
}