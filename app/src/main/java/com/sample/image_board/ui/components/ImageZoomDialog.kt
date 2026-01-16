package com.sample.image_board.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

@Composable
fun ImageZoomDialog(imageUrl: String, onDismiss: () -> Unit) {
    // State untuk zoom dan pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom saat dialog dibuka
    LaunchedEffect(imageUrl) {
        scale = 1f
        offset = Offset.Zero
    }

    Dialog(
            onDismissRequest = onDismiss,
            properties =
                    DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = false
                    )
    ) {
        Box(
                modifier =
                        Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
                            // Tap background untuk dismiss
                            detectTapGestures(
                                    onTap = {
                                        if (scale <= 1f) {
                                            onDismiss()
                                        }
                                    },
                                    onDoubleTap = { tapOffset ->
                                        // Double tap untuk toggle zoom
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                            // Center zoom pada posisi tap (optional)
                                        }
                                    }
                            )
                        }
        ) {
            // Close Button
            IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                )
            }

            // Transformable State untuk pinch zoom dan pan
            val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 5f)

                // Hanya bisa pan kalau sudah di-zoom
                if (scale > 1f) {
                    offset += offsetChange
                } else {
                    offset = Offset.Zero
                }
            }

            // Image dengan transform
            AsyncImage(
                    model = imageUrl,
                    contentDescription = "Zoomed Image",
                    modifier =
                            Modifier.fillMaxSize()
                                    .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                    )
                                    .transformable(state = state),
                    contentScale = ContentScale.Fit
            )
        }
    }
}
