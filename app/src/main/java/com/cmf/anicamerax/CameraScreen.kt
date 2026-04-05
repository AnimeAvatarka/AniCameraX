package com.cmf.anicamerax

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.geometry.Offset
import android.content.Context
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import com.cmf.anicamerax.models.PhotoMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onTakePhoto: () -> Unit,
    onSwitchCamera: () -> Unit,
    selectedMode: PhotoMode,
    onModeChange: (PhotoMode) -> Unit,
    isDropdownExpanded: Boolean,
    onDropdownExpand: (Boolean) -> Unit,
    onPreviewViewReady: ((PreviewView) -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx: Context ->
                PreviewView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    previewView = this
                }
            },
            modifier = Modifier.matchParentSize(),
            update = {}
        )

        // Top Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = selectedMode.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                titleContentColor = Color.White
            ),
            actions = {
                IconButton(onClick = { onDropdownExpand(!isDropdownExpanded) }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Меню режимов",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { onDropdownExpand(false) },
                    modifier = Modifier.background(
                        color = Color.Black.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    )
                ) {
                    PhotoMode.entries
                        .filter { it != selectedMode }
                        .forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = mode.title,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    onModeChange(mode)
                                    onDropdownExpand(false)
                                }
                            )
                        }
                }
            }
        )

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = onSwitchCamera,
                modifier = Modifier.size(56.dp),
                containerColor = Color.White.copy(alpha = 0.9f),
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Text("↔️", fontSize = 24.sp)
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset: Offset ->
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { offset: Offset ->
                                onTakePhoto()
                            }
                        )
                    }
                    .clickable { onTakePhoto() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(60.dp)) {
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.3f),
                        radius = size.minDimension / 2.5f,
                        style = Stroke(width = 3f)
                    )
                }
            }

            Text(
                text = "Режим: ${selectedMode.title}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    DisposableEffect(previewView) {
        if (previewView != null) {
            onPreviewViewReady?.invoke(previewView!!)
        }
        onDispose {}
    }
}