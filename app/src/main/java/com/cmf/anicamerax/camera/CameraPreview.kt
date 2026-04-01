package com.cmf.anicamerax

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier.fillMaxSize(),
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    onPreviewAvailable: (PreviewView) -> Unit
) {
    AndroidView(
        factory = { ctx: android.content.Context ->
            PreviewView(ctx).apply {
                this.scaleType = scaleType
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }.also(onPreviewAvailable)
        },
        update = { previewView: PreviewView ->
            previewView.scaleType = scaleType
        },
        modifier = modifier
    )
}