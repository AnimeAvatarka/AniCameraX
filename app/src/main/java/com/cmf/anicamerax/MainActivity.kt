package com.cmf.anicamerax

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmf.anicamerax.ui.theme.AniCameraXTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (!cameraGranted) {
            Toast.makeText(this, "Разрешение на камеру необходимо!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )

        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            AniCameraXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    CameraScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

class CameraViewModel : ViewModel() {
    var lastBitmap by mutableStateOf<ImageBitmap?>(null)
        private set
    var rollByDegrees by mutableFloatStateOf(0f)
        private set

    fun updateBitmap(bitmap: ImageBitmap?) {
        lastBitmap = bitmap
    }

    fun updateRoll(degrees: Float) {
        rollByDegrees = degrees
    }
}

enum class PhotoMode(val title: String, val icon: ImageVector) {
    Camera("Фото", Icons.Rounded.CameraAlt),
    Night("Ночь", Icons.Rounded.Nightlight),
    Portrait("Портрет", Icons.Rounded.Portrait),
    HDR("HDR", Icons.Rounded.HdrOn),
    Video("Видео", Icons.Rounded.Videocam),
    Front("Фронт", Icons.Rounded.Face)
}

suspend fun ProcessCameraProvider.Companion.await(context: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val instanceFuture = ProcessCameraProvider.getInstance(context)
        instanceFuture.addListener(
            {
                try {
                    cont.resume(instanceFuture.get())
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var isVideoMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var selectedMode by remember { mutableStateOf(PhotoMode.Camera) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var exposureValue by remember { mutableFloatStateOf(0f) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val videoCapture = remember {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        VideoCapture.withOutput(recorder)
    }

    LaunchedEffect(selectedMode, isVideoMode) {
        val cameraProvider = ProcessCameraProvider.await(context)
        val cameraSelector = if (selectedMode == PhotoMode.Front) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        try {
            cameraProvider.unbindAll()
            camera = if (isVideoMode) {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
            } else {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            }
        } catch (e: Exception) {
            Log.e("Camera", "Binding failed", e)
        }
    }

    // Управление вспышкой отдельно
    LaunchedEffect(isFlashOn) {
        imageCapture.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        selectedMode = if (selectedMode == PhotoMode.Front) PhotoMode.Camera else PhotoMode.Front
                    }) {
                        Icon(Icons.Rounded.FlipCameraAndroid, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { isFlashOn = !isFlashOn }) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isFlashOn) Color.Yellow else Color.White
                        )
                    }
                    IconButton(onClick = { isSettingsOpen = true }) {
                        Icon(Icons.Rounded.Settings, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            ModeSelector(
                selectedMode = selectedMode,
                onModeSelected = { mode ->
                    selectedMode = mode
                    isVideoMode = mode == PhotoMode.Video
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                focusPoint = offset
                                val factory = previewView.meteringPointFactory
                                val point = factory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                    .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                    .build()
                                camera?.cameraControl?.startFocusAndMetering(action)
                            }
                        )
                    }
            )

            Leveler(viewModel.rollByDegrees, modifier = Modifier.align(Alignment.Center))

            focusPoint?.let { offset ->
                FocusControl(
                    offset = offset,
                    exposure = exposureValue,
                    onExposureChange = {
                        exposureValue = it
                        camera?.cameraControl?.setExposureCompensationIndex(it.roundToInt())
                    },
                    onDismiss = { focusPoint = null }
                )
            }

            AnimatedVisibility(
                visible = isRecording,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                RecordingIndicator()
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LastPhotoThumbnail(viewModel.lastBitmap) {
                    Toast.makeText(context, "Галерея", Toast.LENGTH_SHORT).show()
                }

                ShutterButton(
                    isVideo = isVideoMode,
                    isRecording = isRecording,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isVideoMode) {
                            if (isRecording) {
                                recording?.stop()
                                isRecording = false
                            } else {
                                recording = startVideoRecording(context, videoCapture) {
                                    isRecording = true
                                }
                            }
                        } else {
                            takePhoto(context, imageCapture) { bitmap ->
                                viewModel.updateBitmap(bitmap)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.size(64.dp)) // Заглушка для симметрии
            }
        }
    }

    if (isSettingsOpen) {
        CameraSettingsSheet(onDismiss = { isSettingsOpen = false })
    }
}

@Composable
fun ShutterButton(isVideo: Boolean, isRecording: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "")
    val density = LocalDensity.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White,
                style = Stroke(width = density.run { 4.dp.toPx() }),
                radius = size.minDimension / 2.2f
            )
        }
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(if (isVideo && isRecording) RoundedCornerShape(8.dp) else CircleShape)
                .background(if (isVideo) Color.Red else Color.White)
                .animateContentSize()
        )
    }
}

@Composable
fun FocusControl(offset: Offset, exposure: Float, onExposureChange: (Float) -> Unit, onDismiss: () -> Unit) {
    LaunchedEffect(offset) {
        delay(3000L)
        onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.roundToInt() - 40.dp.roundToPx(), offset.y.roundToInt() - 40.dp.roundToPx()) }
                .size(80.dp)
                .border(2.dp, Color.Yellow, CircleShape)
        )

        Slider(
            value = exposure,
            onValueChange = onExposureChange,
            valueRange = -2f..2f,
            modifier = Modifier
                .width(150.dp)
                .graphicsLayer {
                    rotationZ = -90f
                    translationX = offset.x + 100f
                    translationY = offset.y - 75f
                },
            colors = SliderDefaults.colors(thumbColor = Color.Yellow, activeTrackColor = Color.Yellow)
        )
    }
}

@Composable
fun Leveler(rotationDegrees: Float, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val isLevel = abs(rotationDegrees) < 1.5f

    Canvas(modifier = modifier.size(200.dp, 40.dp)) {
        val middle = size.height / 2
        rotate(rotationDegrees) {
            drawLine(
                color = if (isLevel) Color.Green else Color.White.copy(alpha = 0.6f),
                start = Offset(0f, middle),
                end = Offset(size.width, middle),
                strokeWidth = density.run { 2.dp.toPx() }
            )
        }
    }
}

@Composable
fun RecordingIndicator() {
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            seconds++
        }
    }

    Surface(
        color = Color.Red.copy(alpha = 0.8f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text(
            text = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun LastPhotoThumbnail(bitmap: ImageBitmap?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.DarkGray)
            .border(2.dp, Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Rounded.PhotoLibrary, null, tint = Color.White)
        }
    }
}

@Composable
fun ModeSelector(selectedMode: PhotoMode, onModeSelected: (PhotoMode) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        items(PhotoMode.entries) { mode ->
            val isSelected = mode == selectedMode
            Text(
                text = mode.title.uppercase(),
                color = if (isSelected) Color.Yellow else Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable { onModeSelected(mode) }
            )
        }
    }
}

fun startVideoRecording(context: Context, videoCapture: VideoCapture<Recorder>, onStarted: () -> Unit): Recording {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "VID_${System.currentTimeMillis()}")
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AniCameraX")
    }

    val outputOptions = MediaStoreOutputOptions.Builder(
        context.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).setContentValues(contentValues).build()

    return videoCapture.output
        .prepareRecording(context, outputOptions)
        .apply {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                withAudioEnabled()
            }
        }
        .start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Start) onStarted()
        }
}

fun takePhoto(context: Context, imageCapture: ImageCapture, onResult: (ImageBitmap?) -> Unit) {
    val photoFile = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                onResult(bitmap?.asImageBitmap())
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "Photo capture failed", exception)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSettingsSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text("Настройки камеры", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            ListItem(
                headlineContent = { Text("Сетка") },
                trailingContent = { Switch(checked = false, onCheckedChange = {}) }
            )
        }
    }
}