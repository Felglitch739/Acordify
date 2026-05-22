package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSynthesizer
import com.example.data.LocalSongs
import com.example.model.Song
import com.example.model.SongStyle
import com.example.network.GeminiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val message: String? = null) : UiState
    data class Error(val message: String) : UiState
}

class SongViewModel : ViewModel() {
    private val _selectedStyle = MutableStateFlow<SongStyle>(SongStyle.INDIE)
    val selectedStyle: StateFlow<SongStyle> = _selectedStyle.asStateFlow()

    private val _currentSong = MutableStateFlow<Song>(LocalSongs.playlist[0])
    val currentSong: StateFlow<Song> = _currentSong.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Success())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isLocalMode = MutableStateFlow(!GeminiManager.hasValidApiKey())
    val isLocalMode: StateFlow<Boolean> = _isLocalMode.asStateFlow()

    private val _waveform = MutableStateFlow("SINE")
    val waveform: StateFlow<String> = _waveform.asStateFlow()

    private val _accentColorTheme = MutableStateFlow("TERRACOTTA")
    val accentColorTheme: StateFlow<String> = _accentColorTheme.asStateFlow()

    private val _history = MutableStateFlow<List<Song>>(emptyList())
    val history: StateFlow<List<Song>> = _history.asStateFlow()

    val isPlaying: StateFlow<Boolean> = AudioSynthesizer.isPlaying
    val isLooping: StateFlow<Boolean> = AudioSynthesizer.isLooping

    init {
        // Initial setup matching the initial style
        val initialSong = LocalSongs.getRandom(SongStyle.INDIE)
        _currentSong.value = initialSong
        _history.value = listOf(initialSong)
    }

    fun selectStyle(style: SongStyle) {
        _selectedStyle.value = style
    }

    fun setWaveform(type: String) {
        _waveform.value = type
        AudioSynthesizer.currentWaveform = type
    }

    fun setAccentColorTheme(theme: String) {
        _accentColorTheme.value = theme
    }

    fun loadSongFromHistory(song: Song) {
        AudioSynthesizer.stopPlaying()
        _currentSong.value = song
        _selectedStyle.value = getStyleForSong(song)
        // Automatically start playing accompaniment for immediate visual-sound satisfaction
        AudioSynthesizer.startPlayingProgression(song.chords)
    }

    private fun getStyleForSong(song: Song): SongStyle {
        // Match style by chords or title
        return when {
            song.chords.contains("maj7") || song.chords.contains("CM7") -> SongStyle.JAZZ
            song.chords.contains("M7") -> SongStyle.JAZZ
            song.tempo.contains("Rock") -> SongStyle.ROCK
            song.tempo.contains("Pop") -> SongStyle.POP
            song.tempo.contains("Soul") -> SongStyle.R_AND_B
            else -> SongStyle.INDIE
        }
    }

    fun generateNewSong() {
        viewModelScope.launch {
            // Stop current playback before generating a new song
            AudioSynthesizer.stopPlaying()
            _uiState.value = UiState.Loading
            
            // Check API is configured and call it
            val hasKey = GeminiManager.hasValidApiKey()
            _isLocalMode.value = !hasKey

            if (!hasKey) {
                _uiState.value = UiState.Error("Error de Conexión: Verifica tu API Key o Internet")
                return@launch
            }

            var song: Song? = null
            try {
                song = GeminiManager.generateIndieSong(_selectedStyle.value)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error de Conexión: Verifica tu API Key o Internet")
                return@launch
            }

            if (song != null) {
                _currentSong.value = song
                _uiState.value = UiState.Success("AI Generated an original song!")
            } else {
                _uiState.value = UiState.Error("Error de Conexión: Verifica tu API Key o Internet")
                return@launch
            }

            // Append the new song to our session history list
            val currentList = _history.value.toMutableList()
            if (!currentList.any { it.title == _currentSong.value.title }) {
                currentList.add(0, _currentSong.value) // insert at start
                _history.value = currentList
            }

            // Play the chord progression accompaniment for the newly generated song
            AudioSynthesizer.startPlayingProgression(_currentSong.value.chords)
        }
    }

    fun playAccompaniment() {
        AudioSynthesizer.startPlayingProgression(_currentSong.value.chords)
    }

    fun stopAccompaniment() {
        AudioSynthesizer.stopPlaying()
    }

    fun toggleLoopAccompaniment() {
        AudioSynthesizer.toggleLooping()
    }

    override fun onCleared() {
        super.onCleared()
        AudioSynthesizer.stopPlaying()
    }
}
