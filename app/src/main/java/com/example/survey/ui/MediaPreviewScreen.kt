package com.example.survey.ui

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.survey.data.CropData
import com.example.survey.data.MediaItem
import com.example.survey.viewmodel.CameraViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    // FIXED: Direct access to mutableStateListOf - no collectAsState needed
    val capturedMedia = viewModel.capturedMedia
    val uiState by viewModel.uiState
    var showCropDialog by remember { mutableStateOf<MediaItem?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    "Preview - ${uiState.sessionName}",
                    fontWeight = FontWeight.Medium
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        showSaveDialog = true
                    },
                    enabled = viewModel.getSelectedMedia().isNotEmpty()
                ) {
                    Text("Save Selected (${viewModel.getSelectedMedia().size})", color = MaterialTheme.colorScheme.primary)
                }
            }
        )

        if (capturedMedia.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No media captured yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Go back and capture some photos or videos",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column {
                // Selection Summary
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "${capturedMedia.size} items captured",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "${viewModel.getSelectedMedia().size} selected for saving",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        // Select All / Deselect All
                        TextButton(
                            onClick = {
                                val allSelected = capturedMedia.all { it.isSelected }
                                capturedMedia.forEach { mediaItem ->
                                    if (allSelected && mediaItem.isSelected) {
                                        viewModel.toggleMediaSelection(mediaItem)
                                    } else if (!allSelected && !mediaItem.isSelected) {
                                        viewModel.toggleMediaSelection(mediaItem)
                                    }
                                }
                            }
                        ) {
                            Text(
                                if (capturedMedia.all { it.isSelected }) "Deselect All" else "Select All",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(capturedMedia) { mediaItem ->
                        MediaPreviewItem(
                            mediaItem = mediaItem,
                            onToggleSelection = { viewModel.toggleMediaSelection(mediaItem) },
                            onCrop = if (!mediaItem.isVideo) { { showCropDialog = mediaItem } } else null,
                            onDelete = { viewModel.removeMediaItem(mediaItem) }
                        )
                    }

                    // Bottom spacer for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Save Confirmation Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Session") },
            text = {
                Column {
                    Text("Save ${viewModel.getSelectedMedia().size} selected items to session '${uiState.sessionName}'?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Items will be saved to your device gallery in a dedicated folder.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        // Save media to gallery
                        saveMediaToGallery(context, viewModel.getSelectedMedia(), uiState.sessionName)
                        // Save session to ViewModel
                        viewModel.saveSession()
                        // Navigate back to home
                        onSave()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Crop Dialog
    showCropDialog?.let { mediaItem ->
        CropDialog(
            mediaItem = mediaItem,
            onDismiss = { showCropDialog = null },
            onCrop = { cropData ->
                viewModel.updateCropData(mediaItem, cropData)
                showCropDialog = null
            }
        )
    }
}

@Composable
fun MediaPreviewItem(
    mediaItem: MediaItem,
    onToggleSelection: () -> Unit,
    onCrop: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (mediaItem.isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (mediaItem.isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Media preview with overlay
                Box {
                    AsyncImage(
                        model = mediaItem.uri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Video indicator
                    if (mediaItem.isVideo) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Selection indicator
                    if (mediaItem.isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(bottomStart = 8.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onCrop != null) {
                        IconButton(onClick = onCrop) {
                            Icon(
                                Icons.Default.Crop,
                                contentDescription = "Crop",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    }
                    Checkbox(
                        checked = mediaItem.isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Media info with icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (mediaItem.isVideo) Icons.Default.Videocam else Icons.Default.Photo,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (mediaItem.isVideo)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (mediaItem.isVideo) "Video" else "Photo",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                // FIXED: Replace Chip with simple Text badge
                if (mediaItem.cropData != null) {
                    Text(
                        text = "Cropped",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timestamp and location
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = mediaItem.timestamp,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = mediaItem.location,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CropDialog(
    mediaItem: MediaItem,
    onDismiss: () -> Unit,
    onCrop: (CropData) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Crop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crop Image")
            }
        },
        text = {
            Text("Image cropping functionality will be implemented here. For now, this will apply basic crop settings.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // For now, just apply a dummy crop
                    onCrop(CropData(0f, 0f, 1f, 1f))
                }
            ) {
                Text("Apply Crop")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// FIXED: Updated to use Pictures/SurveyApp directory to match CameraController
private fun saveMediaToGallery(
    context: Context,
    mediaItems: List<MediaItem>,
    sessionName: String
) {
    try {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val sessionDir = File(picturesDir, "SurveyApp${File.separator}$sessionName")

        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        mediaItems.forEach { mediaItem ->
            val fileName = if (mediaItem.isVideo) {
                "video_${System.currentTimeMillis()}.mp4"
            } else {
                "photo_${System.currentTimeMillis()}.jpg"
            }

            val destFile = File(sessionDir, fileName)
            mediaItem.file.copyTo(destFile, overwrite = true)

            // Scan the file so it appears in gallery
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                null,
                null
            )

            // Clean up temp file
            try {
                mediaItem.file.delete()
            } catch (e: Exception) {
                // Handle cleanup error silently
            }
        }
    } catch (e: Exception) {
        // Handle save error - you might want to show a toast or error dialog
    }
}
