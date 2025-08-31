package com.example.survey.utils

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

class GalleryManager(private val context: Context) {

    companion object {
        private const val APP_FOLDER_NAME = "SurveyApp"
    }

    // Create session folder in Pictures directory
    fun createSessionFolder(sessionName: String): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val sessionDir = File(picturesDir, "$APP_FOLDER_NAME${File.separator}$sessionName")

        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        return sessionDir
    }

    // Get session folder path
    fun getSessionFolder(sessionName: String): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File(picturesDir, "$APP_FOLDER_NAME${File.separator}$sessionName")
    }

    // Open session folder in default gallery app
    fun openSessionInGallery(sessionName: String) {
        val sessionFolder = getSessionFolder(sessionName)

        if (!sessionFolder.exists() || sessionFolder.listFiles()?.isEmpty() == true) {
            return // No files to show
        }

        try {
            // Method 1: Try to open folder directly
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        sessionFolder
                    ),
                    "image/*"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Method 2: Fallback - open first image in folder
                openFirstImageInGallery(sessionFolder)
            }
        } catch (e: Exception) {
            // Method 3: Alternative approach - use ACTION_GET_CONTENT
            openGalleryWithChooser(sessionFolder)
        }
    }

    private fun openFirstImageInGallery(sessionFolder: File) {
        val files = sessionFolder.listFiles { file ->
            file.extension.lowercase() in listOf("jpg", "jpeg", "png", "mp4", "avi", "mov")
        }

        if (files != null && files.isNotEmpty()) {
            val firstFile = files.first()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                firstFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(firstFile))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        }
    }

    private fun openGalleryWithChooser(sessionFolder: File) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, "Open Gallery")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4" -> "video/mp4"
            "avi" -> "video/avi"
            "mov" -> "video/quicktime"
            else -> "image/*"
        }
    }

    // Scan all files in session folder for gallery visibility
    fun scanSessionFolder(sessionName: String) {
        val sessionFolder = getSessionFolder(sessionName)
        val files = sessionFolder.listFiles()

        if (files != null && files.isNotEmpty()) {
            val filePaths = files.map { it.absolutePath }.toTypedArray()
            MediaScannerConnection.scanFile(
                context,
                filePaths,
                null
            ) { path, uri ->
                // Files scanned successfully
            }
        }
    }
}
