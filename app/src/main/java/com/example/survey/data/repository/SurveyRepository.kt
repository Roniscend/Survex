package com.example.survey.data.repository

import android.content.Context
import android.net.Uri
import com.example.survey.data.MediaItem
import com.example.survey.data.Session
import com.example.survey.data.database.MediaEntity
import com.example.survey.data.database.SessionEntity
import com.example.survey.data.database.SurveyDao
import com.example.survey.data.database.SurveyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class SurveyRepository(context: Context) {

    private val surveyDao: SurveyDao = SurveyDatabase.getDatabase(context).surveyDao()
    private val context: Context = context.applicationContext

    // FIXED: Conversion functions with proper MediaItem constructor
    private fun MediaItem.toEntity(): MediaEntity {
        return MediaEntity(
            id = this.id,
            sessionName = this.sessionName,
            filePath = this.file.absolutePath,
            isVideo = this.isVideo,
            timestamp = this.timestamp,
            location = this.location
        )
    }

    private fun MediaEntity.toMediaItem(): MediaItem {
        return MediaItem(
            id = this.id,
            uri = Uri.fromFile(File(this.filePath)), // FIXED: Added uri parameter
            file = File(this.filePath),
            isVideo = this.isVideo,
            timestamp = this.timestamp,
            location = this.location,
            sessionName = this.sessionName
        )
    }

    // Session operations
    fun getAllSessions(): Flow<List<Session>> {
        return surveyDao.getAllSessions().map { sessionEntities ->
            sessionEntities.map { entity ->
                Session(
                    name = entity.name,
                    mediaItems = mutableListOf(),
                    createdAt = entity.createdAt
                )
            }
        }
    }

    suspend fun insertSession(session: Session) = withContext(Dispatchers.IO) {
        try {
            val entity = SessionEntity(
                name = session.name,
                createdAt = session.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            surveyDao.insertSession(entity)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun deleteSession(sessionName: String) = withContext(Dispatchers.IO) {
        try {
            surveyDao.deleteSessionByName(sessionName)

            val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
            val sessionDir = File(picturesDir, "SurveyApp${File.separator}$sessionName")
            if (sessionDir.exists()) {
                sessionDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getSessionByName(sessionName: String): Session? = withContext(Dispatchers.IO) {
        try {
            val entity = surveyDao.getSessionByName(sessionName)
            entity?.let {
                Session(
                    name = it.name,
                    mediaItems = mutableListOf(),
                    createdAt = it.createdAt
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun insertMediaWithSession(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        try {
            val existingSession = surveyDao.getSessionByName(mediaItem.sessionName)
            if (existingSession == null) {
                val newSession = SessionEntity(
                    name = mediaItem.sessionName,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                surveyDao.insertSession(newSession)
            }

            val entity = mediaItem.toEntity()
            surveyDao.insertMedia(entity)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun insertMedia(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        try {
            val entity = mediaItem.toEntity()
            surveyDao.insertMedia(entity)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun deleteMedia(mediaId: String) = withContext(Dispatchers.IO) {
        try {
            surveyDao.deleteMediaById(mediaId)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun deleteMediaItem(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        try {
            val entity = mediaItem.toEntity()
            surveyDao.deleteMedia(entity)

            if (mediaItem.file.exists()) {
                mediaItem.file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun getMediaForSession(sessionName: String): Flow<List<MediaItem>> {
        return surveyDao.getMediaForSession(sessionName).map { entities ->
            entities.map { entity -> entity.toMediaItem() }
        }
    }

    suspend fun insertSessionWithMedia(session: Session, mediaItems: List<MediaItem>) = withContext(Dispatchers.IO) {
        try {
            insertSession(session)
            mediaItems.forEach { mediaItem ->
                insertMedia(mediaItem)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun sessionExists(sessionName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            surveyDao.getSessionByName(sessionName) != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
