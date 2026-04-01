package com.cmf.anicamerax

import android.content.Context
import android.util.Log
import android.view.Surface as AndroidSurface // Псевдоним, чтобы не путать с Composable Surface
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreenWithPreview(
    imageCapture: ImageCapture?, // Используется для передачи вовне через onImageCaptureChange
    onImageCaptureChange: (ImageCapture?) -> Unit,
    cameraSelector: CameraSelector,
    onCameraSelectorChange: (CameraSelector) -> Unit, // Если нужно менять селектор извне
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService, // Передано для операций с фото
    configManager: CameraConfigManager,
    photoMode: PhotoMode,
    expanded: Boolean,
    onTakePhoto: () -> Unit,
    onSwitchCamera: () -> Unit,
    onModeChange: (PhotoMode) -> Unit,
    onDropdownExpand: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var profile by remember { mutableStateOf<CameraProfile?>(null) }

    // 1. Загружаем профиль
    LaunchedEffect(cameraSelector) {
        val lensFacing = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        profile = configManager.getProfile(lensFacing)
    }

    // 2. Привязываем камеру (Логика отделена от UI)
    LaunchedEffect(profile, previewView, cameraSelector) {
        val currentProfile = profile ?: return@LaunchedEffect
        val currentPreviewView = previewView ?: return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val builder = ImageCapture.Builder()
                    .setCaptureMode(currentProfile.captureMode)
                    .setJpegQuality(currentProfile.jpegQuality)

                // Используем псевдоним AndroidSurface
                val rotation = currentPreviewView.display?.rotation ?: AndroidSurface.ROTATION_0
                builder.setTargetRotation(rotation)

                val newImageCapture = builder.build()
                onImageCaptureChange(newImageCapture)

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(currentPreviewView.surfaceProvider)
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    newImageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraScreen", "Ошибка привязки камеры", e)
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // UI Превью
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    keepScreenOn = true
                }.also { previewView = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Верхняя панель
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = photoMode.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            actions = {
                IconButton(onClick = { onDropdownExpand(!expanded) }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Меню")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onDropdownExpand(false) },
                    modifier = Modifier.background(
                        color = Color.Black.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    )
                ) {
                    PhotoMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.title, color = Color.White) },
                            onClick = {
                                onModeChange(mode)
                                onDropdownExpand(false)
                            }
                        )
                    }
                }
            }
        )

        // Элементы управления снизу
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Кнопка переключения камеры
            SmallFloatingActionButton(
                onClick = onSwitchCamera,
                containerColor = Color.White.copy(alpha = 0.8f),
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Text("↔️", fontSize = 20.sp)
            }

            // Кнопка затвора
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clickable { onTakePhoto() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(70.dp)) {
                    drawCircle(
                        color = Color.White,
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = size.minDimension / 3f
                    )
                }
            }

            // Индикатор режима
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "РЕЖИМ: ${photoMode.title.uppercase()}",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}
