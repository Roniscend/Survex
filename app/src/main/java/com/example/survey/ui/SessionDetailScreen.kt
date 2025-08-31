package com.example.survey.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.survey.viewmodel.CameraViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionName: String,
    viewModel: CameraViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var mediaFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedMedia by remember { mutableStateOf<File?>(null) }
    var showFullScreen by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<File?>(null) }
    var showDeleteSessionDialog by remember { mutableStateOf(false) }
    var showMenuDropdown by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Launcher for picking images from gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val sessionDir = File(picturesDir, "SurveyApp${File.separator}$sessionName")
            if (!sessionDir.exists()) sessionDir.mkdirs()

            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val destFile = File(sessionDir, fileName)

            try {
                context.contentResolver.openInputStream(selectedUri).use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream?.copyTo(outputStream)
                    }
                }
                mediaFiles = loadSessionMedia(context, sessionName)
            } catch (e: Exception) {
                Log.e("SessionDetail", "Error copying image", e)
            }
        }
    }

    // Load media files from Pictures/SurveyApp/SessionName directory
    LaunchedEffect(sessionName) {
        isLoading = true
        try {
            mediaFiles = loadSessionMedia(context, sessionName)
            Log.d("SessionDetail", "Found ${mediaFiles.size} media files")
        } catch (e: Exception) {
            Log.e("SessionDetail", "Error loading media files", e)
            mediaFiles = emptyList()
        } finally {
            isLoading = false
        }
    }

    if (showFullScreen && selectedMedia != null) {
        FullScreenMediaViewer(
            mediaFile = selectedMedia!!,
            onClose = {
                showFullScreen = false
                selectedMedia = null
            }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        "Session: $sessionName",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Gallery Button
                    IconButton(
                        onClick = {
                            openSessionInGallery(context, sessionName)
                        }
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Open in Gallery",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Add Photo Button
                    IconButton(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Photo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Menu Button
                    Box {
                        IconButton(onClick = { showMenuDropdown = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menu"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Session") },
                                onClick = {
                                    showMenuDropdown = false
                                    showDeleteSessionDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                }
                            )
                        }
                    }
                }
            )

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (mediaFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No media found in this session",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Capture photos or tap + to add from gallery",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                Column {
                    Text(
                        text = "${mediaFiles.size} items",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mediaFiles, key = { it.absolutePath }) { file ->
                            MediaGridItemWithDelete(
                                mediaFile = file,
                                onClick = {
                                    selectedMedia = file
                                    showFullScreen = true
                                },
                                onDelete = {
                                    showDeleteDialog = file
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete individual media confirmation dialog
    showDeleteDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Media") },
            text = {
                Text("Are you sure you want to delete this ${if (file.extension.lowercase() in listOf("mp4", "avi", "mov", "mkv")) "video" else "photo"}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            file.delete()
                            mediaFiles = loadSessionMedia(context, sessionName)
                            showDeleteDialog = null
                        } catch (e: Exception) {
                            Log.e("SessionDetail", "Error deleting file", e)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete session confirmation dialog
    if (showDeleteSessionDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSessionDialog = false },
            title = { Text("Delete Session") },
            text = {
                Column {
                    Text("Are you sure you want to delete the entire session '$sessionName'?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This will permanently delete all ${mediaFiles.size} media files in this session. This action cannot be undone.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            // Delete from database
                            viewModel.deleteSessionWithFiles(context, sessionName)

                            // Delete session folder and all contents
                            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            val sessionDir = File(picturesDir, "SurveyApp${File.separator}$sessionName")
                            sessionDir.deleteRecursively()

                            showDeleteSessionDialog = false
                            onBack()
                        } catch (e: Exception) {
                            Log.e("SessionDetail", "Error deleting session", e)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSessionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MediaGridItemWithDelete(
    mediaFile: File,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isVideo = mediaFile.extension.lowercase() in listOf("mp4", "avi", "mov", "mkv")

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                AsyncImage(
                    model = mediaFile,
                    contentDescription = "Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(android.R.drawable.ic_menu_camera),
                    error = painterResource(android.R.drawable.ic_menu_report_image)
                )
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // File info overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        .format(Date(mediaFile.lastModified())),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun FullScreenMediaViewer(
    mediaFile: File,
    onClose: () -> Unit
) {
    val isVideo = mediaFile.extension.lowercase() in listOf("mp4", "avi", "mov", "mkv")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isVideo) {
            VideoPlayer(
                videoFile = mediaFile,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ZoomableImage(
                imageFile = mediaFile,
                modifier = Modifier.fillMaxSize()
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

@Composable
fun VideoPlayer(
    videoFile: File,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = ExoPlayer.Builder(ctx).build().apply {
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile))
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
                useController = true
            }
        },
        modifier = modifier
    )
}

@Composable
fun ZoomableImage(
    imageFile: File,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    AsyncImage(
        model = imageFile,
        contentDescription = null,
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentScale = ContentScale.Fit
    )
}

// Helper function to load media files from Pictures/SurveyApp/SessionName directory
private fun loadSessionMedia(context: Context, sessionName: String): List<File> {
    return try {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val sessionDir = File(picturesDir, "SurveyApp${File.separator}$sessionName")

        if (sessionDir.exists() && sessionDir.isDirectory) {
            val files = sessionDir.listFiles { file ->
                file.extension.lowercase() in listOf("jpg", "jpeg", "png", "mp4", "avi", "mov", "mkv")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            Log.d("SessionDetail", "Loaded ${files.size} media files from ${sessionDir.absolutePath}")
            files
        } else {
            Log.w("SessionDetail", "Session directory doesn't exist: ${sessionDir.absolutePath}")
            emptyList()
        }
    } catch (e: Exception) {
        Log.e("SessionDetail", "Error loading session media", e)
        emptyList()
    }
}

// FIXED: Helper function to open session in gallery with correct authority
private fun openSessionInGallery(context: Context, sessionName: String) {
    try {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val sessionDir = File(picturesDir, "SurveyApp${File.separator}$sessionName")

        // Check if directory exists and has files
        if (!sessionDir.exists() || sessionDir.listFiles()?.isEmpty() == true) {
            Log.e("SessionDetail", "No files found in directory: ${sessionDir.absolutePath}")
            return
        }

        // Get the first media file to open gallery with
        val mediaFiles = sessionDir.listFiles { file ->
            val extension = file.extension.lowercase()
            extension in listOf("jpg", "jpeg", "png", "mp4", "avi", "mov")
        }

        if (mediaFiles?.isNotEmpty() == true) {
            val firstFile = mediaFiles[0]
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",  // FIXED: Match manifest authority
                firstFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, if (firstFile.extension.lowercase() in listOf("mp4", "avi", "mov")) "video/*" else "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Log.e("SessionDetail", "No app found to open media file")
            }
        }
    } catch (e: Exception) {
        Log.e("SessionDetail", "Error opening gallery", e)
    }
}
