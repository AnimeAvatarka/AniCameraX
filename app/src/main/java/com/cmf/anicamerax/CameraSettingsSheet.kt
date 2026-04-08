package com.cmf.anicamerax

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSettingsSheet(
    onDismiss: () -> Unit,
    sharpness: Float, // Оставляем Float для слайдера
    onSharpnessChange: (Float) -> Unit,
    onApply: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Настройки", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Резкость: ${sharpness.toInt()}")
            Slider(
                value = sharpness,
                onValueChange = onSharpnessChange,
                valueRange = 0f..10f
            )

            Button(
                onClick = {
                    onApply()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Применить")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}