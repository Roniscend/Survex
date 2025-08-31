package com.example.survey.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.example.survey.data.CropData
import com.example.survey.data.MediaItem
import com.example.survey.data.Session
import com.example.survey.data.repository.SurveyRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CameraViewModel : ViewModel() {

    private lateinit var repository: SurveyRepository

    // FIXED: Use mutableStateOf for single state object
    private val _uiState = mutableStateOf(CameraUiState())
    val uiState: State<CameraUiState> = _uiState

    // FIXED: Use mutableStateListOf for immediate UI updates on lists
    private val _capturedMedia = mutableStateListOf<MediaItem>()
    val capturedMedia: List<MediaItem> = _capturedMedia

    // Sessions list
    private val _allSessions = mutableStateListOf<Session>()
    val allSessions: List<Session> = _allSessions

    fun initializeRepository(context: Context) {
        repository = SurveyRepository(context)
        loadSessionsFromDatabase()
    }

    private fun loadSessionsFromDatabase() {
        viewModelScope.launch {
            repository.getAllSessions().collect { sessions ->
                _allSessions.clear()
                _allSessions.addAll(sessions)
            }
        }
    }

    fun setSessionName(name: String) {
        _uiState.value = _uiState.value.copy(sessionName = name)
    }

    // FIXED: This will immediately trigger UI recomposition for Save button
    fun addMediaItem(mediaItem: MediaItem) {
        _capturedMedia.add(mediaItem) // Immediate UI update

        // Save to database asynchronously (doesn't block UI)
        viewModelScope.launch {
            try {
                repository.insertMediaWithSession(mediaItem)
            } catch (e: Exception) {
                e.printStackTrace()
                // Revert UI state on database error
                _capturedMedia.remove(mediaItem)
            }
        }
    }

    fun removeMediaItem(mediaItem: MediaItem) {
        _capturedMedia.remove(mediaItem)

        viewModelScope.launch {
            try {
                repository.deleteMedia(mediaItem.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleMediaSelection(mediaItem: MediaItem) {
        val index = _capturedMedia.indexOfFirst { it.id == mediaItem.id }
        if (index != -1) {
            _capturedMedia[index] = _capturedMedia[index].copy(
                isSelected = !_capturedMedia[index].isSelected
            )
        }
    }

    fun updateCropData(mediaItem: MediaItem, cropData: CropData) {
        val index = _capturedMedia.indexOfFirst { it.id == mediaItem.id }
        if (index != -1) {
            _capturedMedia[index] = _capturedMedia[index].copy(cropData = cropData)
        }
    }

    fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
    }

    fun clearCapturedMedia() {
        _capturedMedia.clear()
    }

    fun getSelectedMedia(): List<MediaItem> {
        return _capturedMedia.filter { it.isSelected }
    }

    fun saveSession() {
        viewModelScope.launch {
            val sessionName = _uiState.value.sessionName
            val selectedMedia = getSelectedMedia()

            if (sessionName.isNotEmpty() && selectedMedia.isNotEmpty()) {
                val session = Session(
                    name = sessionName,
                    mediaItems = selectedMedia.toMutableList()
                )
                repository.insertSession(session)

                selectedMedia.forEach { mediaItem ->
                    repository.insertMedia(mediaItem)
                }

                clearCapturedMedia()
            }
        }
    }

    fun deleteSessionWithFiles(context: Context, sessionName: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionName)
            _allSessions.removeAll { it.name == sessionName }
        }
    }

    fun getSessionByName(name: String): Session? {
        return _allSessions.find { it.name == name }
    }
}

data class CameraUiState(
    val sessionName: String = "",
    val isRecording: Boolean = false,
    val showPreview: Boolean = false
)
