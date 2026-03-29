package com.cmf.anicamerax

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.cmf.anicamerax.ui.theme.AniCameraXTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color.Companion.Gray
import kotlinx.coroutines.flow.collect
import android.media.SoundPool
import android.widget.Toast

class MainActivity : ComponentActivity() {

    private var imageCapture: ImageCapture? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var outputDirectory: File
    private var photoMode by mutableStateOf(PhotoMode.Light)
    private var expanded by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        outputDirectory = getOutputDirectory()

        setContent {
            AniCameraXTheme {
                CameraScreen(
                    onTakePhoto = { takePhoto() },
                    onSwitchCamera = { switchCamera() },
                    selectedMode = photoMode,
                    onModeChange = { photoMode = it },
                    isDropdownExpanded = expanded,
                    onDropdownExpand = { expanded = it }
                )
            }
        }

        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Разрешение на камеру необходимо", Toast.LENGTH_SHORT).show()
            }
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(when (photoMode) {
                    PhotoMode.High -> ImageCapture.CAPTURE_MODE_HIGH_QUALITY
                    PhotoMode.Human -> ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY // можно улучшить позже
                    else -> ImageCapture.CAPTURE_MODE_BALANCED
                })
                .build()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findPreviewView().surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun findPreviewView(): PreviewView {
        return findViewById(R.id.preview_view)
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${resources.getString(R.string.app_name)}")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        val soundPool = SoundPool.Builder().setMaxStreams(1).build()
        val soundId = soundPool.load(this, R.raw.shutter_sound, 1)

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
                    runOnUiThread {
                        Toast.makeText(baseContext, "Фото сохранено", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(baseContext, "Ошибка: ${exception.message}", Toast.LENGTH_LONG).show()
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
        startCamera()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }
}

enum class PhotoMode(val title: String) {
    Light("Свет"),
    Human("Человек"),
    High("Ночь"),
    Pro("Про"),
    NightPro("Ночь+"),
    HDR("HDR")
}

@Composable
fun CameraScreen(
    onTakePhoto: () -> Unit,
    onSwitchCamera: () -> Unit,
    selectedMode: PhotoMode,
    onModeChange: (PhotoMode) -> Unit,
    isDropdownExpanded: Boolean,
    onDropdownExpand: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    id = R.id.preview_view
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.matchParentSize(),
            update = {}
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = selectedMode.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Button(
                onClick = onSwitchCamera,
                modifier = Modifier.padding(bottom = 16.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(12.dp)
            ) {
                Text("🔄")
            }

            Box {
                androidx.compose.material.Button(
                    onClick = onTakePhoto,
                    onClickLabel = "Сделать фото",
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .scale(scale)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                }
                            )
                        },
                    shape = CircleShape,
                    contentPadding = PaddingValues(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    elevation = ButtonDefaults.elevation(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Gray.copy(alpha = 0.3f),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { onDropdownExpand(false) },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.9f))
                ) {
                    PhotoMode.values().filter { it != selectedMode && it != PhotoMode.Light }
                        .forEach { mode ->
                            DropdownMenuItem(
                                onClick = {
                                    onModeChange(mode)
                                    onDropdownExpand(false)
                                }
                            ) {
                                Text(text = mode.title, color = Color.White)
                            }
                        }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// Уникальный ID для PreviewView
val R = object {
    val id = object {
        val preview_view = 1001
    }
    val raw = object {
        val shutter_sound = 0x7f060000
    }
}