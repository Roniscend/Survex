package com.example.survey.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {

    // Session operations
    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE name = :sessionName")
    suspend fun deleteSessionByName(sessionName: String)

    @Query("SELECT * FROM sessions WHERE name = :sessionName")
    suspend fun getSessionByName(sessionName: String): SessionEntity?

    // Media operations
    @Query("SELECT * FROM media_items WHERE sessionName = :sessionName ORDER BY timestamp DESC")
    fun getMediaForSession(sessionName: String): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity)

    @Delete
    suspend fun deleteMedia(media: MediaEntity)

    @Query("DELETE FROM media_items WHERE id = :mediaId")
    suspend fun deleteMediaById(mediaId: String)

    @Query("DELETE FROM media_items WHERE sessionName = :sessionName")
    suspend fun deleteAllMediaForSession(sessionName: String)

    @Query("SELECT COUNT(*) FROM media_items WHERE sessionName = :sessionName")
    suspend fun getMediaCountForSession(sessionName: String): Int

    // Batch operations using transactions
    @Transaction
    suspend fun insertSessionWithMedia(session: SessionEntity, mediaItems: List<MediaEntity>) {
        insertSession(session)
        mediaItems.forEach { insertMedia(it) }
    }

    @Transaction
    suspend fun deleteSessionWithAllMedia(sessionName: String) {
        deleteAllMediaForSession(sessionName)
        deleteSessionByName(sessionName)
    }
}
