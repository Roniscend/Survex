package com.example.survey.data

import android.net.Uri
import java.io.File

data class MediaItem(
    val id: String = System.currentTimeMillis().toString(),
    val uri: Uri,
    val file: File,
    val isVideo: Boolean,
    val timestamp: String,
    val location: String,
    val sessionName: String,
    var isSelected: Boolean = true,
    var cropData: CropData? = null
)

data class CropData(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class Session(
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val mediaItems: MutableList<MediaItem> = mutableListOf()
)
