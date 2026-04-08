package com.cmf.anicamerax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import com.cmf.anicamerax.logic.CameraManager

class MainActivity : ComponentActivity() {
    private val cameraManager by lazy { CameraManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Тип Float везде: и в состоянии, и в модели данных
            var sharpness by remember { mutableFloatStateOf(1f) }
            var denoise by remember { mutableFloatStateOf(1f) }
            var showSettings by remember { mutableStateOf(false) }

            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val previewView = remember { PreviewView(context) }

            // Теперь здесь Float и Float — ошибок не будет
            LaunchedEffect(Unit) {
                cameraManager.startCamera(lifecycleOwner, previewView, LmcParams(sharpness, denoise))
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = { showSettings = true }) { Text("Настройки") }
                    Spacer(modifier = Modifier.height(16.dp))
                    IconButton(
                        onClick = { cameraManager.takePhoto { /* ... */ } },
                        modifier = Modifier.size(70.dp).background(Color.White, CircleShape)
                    ) { }
                }

                if (showSettings) {
                    CameraSettingsSheet(
                        onDismiss = { showSettings = false },
                        sharpness = sharpness,
                        onSharpnessChange = { sharpness = it },
                        onApply = {
                            // Передаем Float напрямую
                            cameraManager.startCamera(lifecycleOwner, previewView, LmcParams(sharpness, denoise))
                        }
                    )
                }
            }
        }
    }
}