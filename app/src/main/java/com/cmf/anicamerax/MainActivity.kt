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
import androidx.compose.ui.Modifier // Добавлен этот импорт
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner // Обновленный импорт для Lifecycle 2.7.0+
import com.cmf.anicamerax.ui.theme.AniCameraXTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Разрешение на камеру необходимо", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Запрос разрешения при запуске
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
}

@Composable
fun MainCameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Используем remember для создания экзекутора один раз
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    val configManager = remember { CameraConfigManager() }
    var photoMode by remember { mutableStateOf(PhotoMode.Light) }
    var expanded by remember { mutableStateOf(false) }

    // Завершаем работу экзекутора при выходе из экрана
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    CameraScreenWithPreview(
        imageCapture = imageCapture,
        onImageCaptureChange = { imageCapture = it },
        cameraSelector = cameraSelector,
        onCameraSelectorChange = { cameraSelector = it },
        lifecycleOwner = lifecycleOwner,
        cameraExecutor = cameraExecutor,
        configManager = configManager,
        photoMode = photoMode,
        expanded = expanded,
        onTakePhoto = {
            // Захватываем текущий imageCapture локально
            val capture = imageCapture
            if (capture == null) {
                Toast.makeText(context, "Камера еще не готова", Toast.LENGTH_SHORT).show()
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
                        Log.e("Camera", "Ошибка съёмки: ${exception.message}", exception)
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
        onDropdownExpand = { expanded = it }
    )
}
