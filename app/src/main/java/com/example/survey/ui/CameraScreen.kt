package com.example.survey.ui

import android.Manifest
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.survey.utils.CameraController
import com.example.survey.utils.LocationHelper
import com.example.survey.viewmodel.CameraViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToPreview: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // FIXED: Proper observation for State and List types
    val uiState by viewModel.uiState  // Use 'by' for State<T>
    val capturedMedia = viewModel.capturedMedia  // Direct access for List<T>

    // Handle back press to clear captured media
    BackHandler {
        viewModel.clearCapturedMedia()
        onBack()
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    var cameraController by remember { mutableStateOf<CameraController?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    val locationHelper = remember { LocationHelper(context) }

    if (!permissionsState.allPermissionsGranted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Camera and location permissions are required")
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant Permissions")
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }.also { previewView ->
                    val controller = CameraController(ctx, lifecycleOwner)
                    controller.initialize(previewView.surfaceProvider)
                    cameraController = controller
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = {
                    viewModel.clearCapturedMedia()
                    onBack()
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            // FIXED: Save button will now appear immediately after first photo
            if (capturedMedia.isNotEmpty()) {
                Button(
                    onClick = onNavigateToPreview,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save (${capturedMedia.size})")
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Switch Camera
            IconButton(
                onClick = { cameraController?.switchCamera() },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.FlipCameraAndroid,
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }

            // Capture Photo
            IconButton(
                onClick = {
                    locationHelper.getCurrentLocation { location ->
                        cameraController?.capturePhoto(
                            sessionName = uiState.sessionName,
                            timestamp = viewModel.getCurrentTimestamp(),
                            location = location,
                            onSuccess = { mediaItem ->
                                viewModel.addMediaItem(mediaItem)
                            },
                            onError = { error ->
                                error.printStackTrace()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Take Photo",
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Record Video
            IconButton(
                onClick = {
                    if (!isRecording) {
                        locationHelper.getCurrentLocation { location ->
                            val started = cameraController?.startVideoRecording(
                                sessionName = uiState.sessionName,
                                timestamp = viewModel.getCurrentTimestamp(),
                                location = location,
                                onSuccess = { mediaItem ->
                                    viewModel.addMediaItem(mediaItem)
                                    isRecording = false
                                },
                                onError = { error ->
                                    error.printStackTrace()
                                    isRecording = false
                                }
                            ) ?: false
                            if (started) isRecording = true
                        }
                    } else {
                        cameraController?.stopVideoRecording()
                        isRecording = false
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (isRecording) Color.Red else Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                    contentDescription = if (isRecording) "Stop Recording" else "Record Video",
                    tint = Color.White
                )
            }
        }

        // Recording indicator
        if (isRecording) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .background(Color.Red, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recording", color = Color.White)
            }
        }

        // Photo count indicator
        if (capturedMedia.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = CircleShape
            ) {
                Text(
                    text = "${capturedMedia.size}",
                    color = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
