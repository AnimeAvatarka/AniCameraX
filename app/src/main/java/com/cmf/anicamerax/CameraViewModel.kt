package com.cmf.anicamerax

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.cmf.anicamerax.models.PhotoMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// КЛАСС LmcParams ОТСЮДА УДАЛЕН.
// Он должен быть объявлен ТОЛЬКО в файле LmcParams.kt

class CameraViewModel : ViewModel() {
    private val _currentMode = MutableStateFlow(PhotoMode.Camera)
    val currentMode = _currentMode.asStateFlow()

    // Используем начальные значения прямо здесь
    var mainParams by mutableStateOf(
        LmcParams(
            sharpness = 25f,
            denoise = 20f,
            saturation = 30f,
            contrast = 25f,
            hdrBoost = 35f
        )
    )

    var isProcessing by mutableStateOf(false)

    fun updateParam(label: String, value: Float) {
        mainParams = when (label) {
            "Sharpness" -> mainParams.copy(sharpness = value)
            "Saturation" -> mainParams.copy(saturation = value)
            "Contrast" -> mainParams.copy(contrast = value)
            "Denoise" -> mainParams.copy(denoise = value)
            "HDR Boost" -> mainParams.copy(hdrBoost = value)
            else -> mainParams
        }
    }
}