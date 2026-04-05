package com.cmf.anicamerax

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cmf.anicamerax.models.CameraConfigManager
import com.cmf.anicamerax.models.PhotoMode
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreenWithPreview(
    imageCapture: ImageCapture? = null,
    onImageCaptureChange: (ImageCapture?) -> Unit = {},
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    photoMode: PhotoMode = PhotoMode.Camera,
    onTakePhoto: () -> Unit = {},
    onSwitchCamera: () -> Unit = {},
    onModeChange: (PhotoMode) -> Unit = {},
    onOpenAdvanced: () -> Unit = {},
    isHdrEnabled: Boolean = false,
    onHdrToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var showZoomBar by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }

    // Привязка CameraX
    LaunchedEffect(cameraSelector, previewView) {
        val currentPreviewView = previewView ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Получаем профиль
                val configManager = CameraConfigManager()
                val lensFacing = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                val profile = configManager.getProfile(lensFacing, photoMode)

                // Настройка захвата изображения
                val newImageCapture = ImageCapture.Builder()
                    .setCaptureMode(profile.captureMode)
                    .setJpegQuality(profile.jpegQuality)
                    .setTargetRotation(currentPreviewView.display.rotation)
                    .setFlashMode(flashMode)
                    .build()

                onImageCaptureChange(newImageCapture)

                // Настройка превью
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(currentPreviewView.surfaceProvider)
                }

                // Отвязка и новая привязка
                cameraProvider.unbindAll()
                val newCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    newImageCapture
                )
                camera = newCamera

                // Наблюдение за зумом
                newCamera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                    zoomLevel = zoomState?.zoomRatio ?: 1f
                }

            } catch (e: Exception) {
                Log.e("CameraScreen", "Ошибка привязки камеры", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Обновление вспышки
    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Превью камеры
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    keepScreenOn = true
                }
            },
            update = { previewView = it },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val factory = previewView?.meteringPointFactory ?: return@detectTapGestures
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(
                            point,
                            FocusMeteringAction.FLAG_AF
                        ).disableAutoCancel().build()
                        camera?.cameraControl?.startFocusAndMetering(action)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val cam = camera ?: return@detectTransformGestures
                        val zoomState = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures
                        val newZoom = (zoomState.zoomRatio * zoom).coerceIn(
                            zoomState.minZoomRatio,
                            zoomState.maxZoomRatio
                        )
                        cam.cameraControl.setZoomRatio(newZoom)
                        zoomLevel = newZoom
                    }
                }
        )

        // Верхняя панель с HDR
        TopAppBar(
            title = {
                Text(
                    text = photoMode.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                }) {
                    Icon(Icons.Rounded.FlashOff, contentDescription = "Вспышка", tint = Color.White)
                }
            },
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HDR",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = isHdrEnabled,
                        onCheckedChange = { onHdrToggle() },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
                IconButton(onClick = { showZoomBar = !showZoomBar }) {
                    Text("🔍", color = Color.White, fontSize = 20.sp)
                }
                IconButton(onClick = onOpenAdvanced) {
                    Text("⚙️", color = Color.White, fontSize = 20.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.4f)),
            modifier = Modifier.padding(8.dp)
        )

        // Режимы снизу
        PrimaryTabRow(
            selectedTabIndex = PhotoMode.entries.indexOf(photoMode),
            containerColor = Color.Transparent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 120.dp)
        ) {
            PhotoMode.entries.forEach { mode ->
                Tab(
                    selected = photoMode == mode,
                    onClick = { onModeChange(mode) },
                    text = { Text(mode.title, fontSize = 12.sp) }
                )
            }
        }

        // Кнопка съёмки
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { onTakePhoto() },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(50.dp).clip(CircleShape).background(Color.White))
        }

        // Переключение камеры
        FloatingActionButton(
            onClick = onSwitchCamera,
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(50.dp)
        ) {
            Text("🔄", fontSize = 20.sp)
        }

        // Полоса масштабирования
        if (showZoomBar) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .height(150.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Zoom", color = Color.White, fontSize = 12.sp)
                Canvas(modifier = Modifier.size(width = 120.dp, height = 6.dp)) {
                    drawRect(color = Color.Gray.copy(alpha = 0.5f))
                    val ratio = (zoomLevel.coerceAtMost(5f) / 5f).coerceAtLeast(0.2f)
                    drawRect(
                        color = Color.White,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width * ratio, size.height)
                    )
                }
                Text(
                    String.format(Locale.US, "%.1fx", zoomLevel.coerceAtMost(5f)),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}