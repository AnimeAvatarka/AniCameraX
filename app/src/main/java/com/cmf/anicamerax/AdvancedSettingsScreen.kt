package com.cmf.anicamerax

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    profile: CameraProfile,
    onProfileChange: (CameraProfile) -> Unit,
    onBack: () -> Unit
) {
    var currentProfile by remember { mutableStateOf(profile) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тонкая настройка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingSlider(
                label = "Качество JPEG",
                value = currentProfile.jpegQuality.toFloat(),
                range = 50f..100f,
                onValueChange = { quality ->
                    currentProfile = currentProfile.copy(jpegQuality = quality.toInt())
                }
            )

            SettingSlider(
                label = "Экспокоррекция",
                value = currentProfile.exposureCompensation.toFloat(),
                range = -3f..3f,
                steps = 6,
                onValueChange = { exp ->
                    currentProfile = currentProfile.copy(exposureCompensation = exp.toInt())
                }
            )

            SettingSlider(
                label = "Насыщенность",
                value = currentProfile.saturation,
                range = 0.5f..2f,
                onValueChange = { sat ->
                    currentProfile = currentProfile.copy(saturation = sat)
                }
            )

            SettingSlider(
                label = "Контраст",
                value = currentProfile.contrast,
                range = 0.5f..2f,
                onValueChange = { con ->
                    currentProfile = currentProfile.copy(contrast = con)
                }
            )

            SettingSlider(
                label = "Резкость",
                value = currentProfile.sharpness,
                range = 0.5f..2f,
                onValueChange = { sharp ->
                    currentProfile = currentProfile.copy(sharpness = sharp)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("HDR", fontSize = 16.sp)
                Switch(
                    checked = currentProfile.enableHdr,
                    onCheckedChange = { hdr ->
                        currentProfile = currentProfile.copy(enableHdr = hdr)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ночной режим", fontSize = 16.sp)
                Switch(
                    checked = currentProfile.enableNight,
                    onCheckedChange = { night ->
                        currentProfile = currentProfile.copy(enableNight = night)
                    }
                )
            }

            Button(
                onClick = {
                    onProfileChange(currentProfile)
                    onBack()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Сохранить")
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = (range.endInclusive - range.start).toInt() * 5,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 16.sp)
            Text(String.format("%.1f", value))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps
        )
    }
}