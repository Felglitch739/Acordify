package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSynthesizer
import com.example.model.Song
import com.example.model.SongStyle
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val message: String? = null) : UiState
    data class Error(val message: String) : UiState
}

import com.example.BuildConfig

val API_KEY = BuildConfig.GEMINI_API_KEY

class SongViewModel : ViewModel() {
    private val _selectedStyle = MutableStateFlow<SongStyle>(SongStyle.INDIE)
    val selectedStyle: StateFlow<SongStyle> = _selectedStyle.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Success())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isLocalMode = MutableStateFlow(false)
    val isLocalMode: StateFlow<Boolean> = _isLocalMode.asStateFlow()

    private val _waveform = MutableStateFlow("SINE")
    val waveform: StateFlow<String> = _waveform.asStateFlow()

    private val _accentColorTheme = MutableStateFlow("TERRACOTTA")
    val accentColorTheme: StateFlow<String> = _accentColorTheme.asStateFlow()

    private val _history = MutableStateFlow<List<Song>>(emptyList())
    val history: StateFlow<List<Song>> = _history.asStateFlow()

    val isPlaying: StateFlow<Boolean> = AudioSynthesizer.isPlaying
    val isLooping: StateFlow<Boolean> = AudioSynthesizer.isLooping

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
        AudioSynthesizer.startPlayingProgression(song.chords)
    }

    private fun getStyleForSong(song: Song): SongStyle {
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
            AudioSynthesizer.stopPlaying()
            _uiState.value = UiState.Loading

            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = API_KEY,
                    generationConfig = generationConfig {
                        temperature = 1.0f
                        responseMimeType = "application/json"
                    }
                )

                val prompt = """
                    Generate a song matching the style: ${_selectedStyle.value.name}.
                    Return EXACTLY a raw JSON object with this shape:
                    {
                      "title": "A short title in Spanish",
                      "tempo": "Tempo in BPM and description",
                      "chords": "A 4-chord progression separated by hyphens (e.g., Am - F - C - G)",
                      "lyrics": "Exactly a 4-line verse in Spanish",
                      "soloScale": "The exact Minor Pentatonic or Major scale to use",
                      "soloTip": "A brief tip for playing a solo"
                    }
                    Do not wrap the response in markdown blocks. Just output the raw JSON.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val rawText = response.text

                if (rawText != null) {
                    val cleanJson = cleanJsonString(rawText)
                    val jsonObject = JSONObject(cleanJson)
                    
                    val song = Song(
                        title = jsonObject.getString("title"),
                        tempo = jsonObject.getString("tempo"),
                        chords = jsonObject.getString("chords"),
                        lyrics = jsonObject.getString("lyrics"),
                        soloScale = jsonObject.getString("soloScale"),
                        soloTip = jsonObject.getString("soloTip"),
                        isAiGenerated = true
                    )
                    
                    _currentSong.value = song
                    _uiState.value = UiState.Success("AI Generated an original song!")

                    val currentList = _history.value.toMutableList()
                    if (!currentList.any { it.title == song.title }) {
                        currentList.add(0, song)
                        _history.value = currentList
                    }

                    AudioSynthesizer.startPlayingProgression(song.chords)
                } else {
                    _uiState.value = UiState.Error("Respuesta vacía de la IA.")
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "Error generating song", e)
                _uiState.value = UiState.Error("Error: ${e.message}")
            }
        }
    }

    private fun cleanJsonString(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            val firstLineEnd = text.indexOf('\n')
            if (firstLineEnd != -1) {
                text = text.substring(firstLineEnd).trim()
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length - 3).trim()
            }
        }
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            text = text.substring(start, end + 1)
        }
        return text
    }

    fun playAccompaniment() {
        _currentSong.value?.let { song ->
            AudioSynthesizer.startPlayingProgression(song.chords)
        }
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
