package com.cmf.anicamerax.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cmf.anicamerax.CameraViewModel

@Composable
fun ShutterButton(onClick: () -> Unit, isProcessing: Boolean) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(if (isProcessing) Color.Gray else Color.White, shape = CircleShape)
            .clickable(enabled = !isProcessing) { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black, shape = CircleShape).padding(2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White, shape = CircleShape))
        }
    }
}

@Composable
fun LmcPanel(vm: CameraViewModel) {
    val params = vm.mainParams
    Card(
        modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 350.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            LmcSlider("Sharpness", params.sharpness) { vm.updateParam("Sharpness", it) }
            LmcSlider("Saturation", params.saturation) { vm.updateParam("Saturation", it) }
            LmcSlider("Contrast", params.contrast) { vm.updateParam("Contrast", it) }
            LmcSlider("Denoise", params.denoise) { vm.updateParam("Denoise", it) }
            LmcSlider("HDR Boost", params.hdrBoost) { vm.updateParam("HDR Boost", it) }
        }
    }
}

@Composable
fun LmcSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = "$label: ${value.toInt()}", color = Color.White)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f
        )
    }
}