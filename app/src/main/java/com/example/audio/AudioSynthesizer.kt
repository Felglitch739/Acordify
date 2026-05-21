package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

object AudioSynthesizer {
    private const val TAG = "AudioSynthesizer"
    private const val SAMPLE_RATE = 22050 // light sample rate, responsive and CPU efficient
    
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val synthScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _isLooping = MutableStateFlow(true)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    var currentWaveform = "SINE"

    private fun generateWaveValue(angle: Double, type: String): Double {
        return when (type) {
            "TRIANGLE" -> {
                val phase = (angle % (2.0 * Math.PI)) / (2.0 * Math.PI)
                val normalizedPhase = if (phase < 0.0) phase + 1.0 else phase
                if (normalizedPhase < 0.25) {
                    4.0 * normalizedPhase
                } else if (normalizedPhase < 0.75) {
                    2.0 - 4.0 * normalizedPhase
                } else {
                    4.0 * normalizedPhase - 4.0
                }
            }
            "SAWTOOTH" -> {
                val phase = (angle % (2.0 * Math.PI)) / (2.0 * Math.PI)
                val normalizedPhase = if (phase < 0.0) phase + 1.0 else phase
                2.0 * normalizedPhase - 1.0
            }
            "SQUARE" -> {
                if (sin(angle) >= 0.0) 0.5 else -0.5
            }
            else -> sin(angle) // SINE
        }
    }

    // Note frequencies map (A4 = 440Hz base semitones)
    private val frequencies = mapOf(
        "C" to 261.63, "C#" to 277.18, "D" to 293.66, "D#" to 311.13, "Eb" to 311.13,
        "E" to 329.63, "F" to 349.23, "F#" to 369.99, "G" to 392.00, "G#" to 415.30, "Ab" to 415.30,
        "A" to 440.00, "A#" to 466.16, "Bb" to 466.16, "B" to 493.88
    )

    fun toggleLooping() {
        _isLooping.value = !_isLooping.value
    }

    fun startPlayingProgression(chordsString: String) {
        stopPlaying()
        _isPlaying.value = true

        synthJob = synthScope.launch {
            try {
                // Initialize AudioTrack
                val bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_8BIT
                )
                
                audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_8BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                audioTrack?.play()

                // Parse chords (e.g. "Am - F - C - G")
                val chords = chordsString.split("-").map { it.trim() }.filter { it.isNotEmpty() }
                if (chords.isEmpty()) {
                    _isPlaying.value = false
                    return@launch
                }

                do {
                    for (chord in chords) {
                        if (!isActive || !_isPlaying.value) break
                        
                        // play each chord for 2.0 seconds for an atmospheric acoustic synth progress
                        val chordDurationMs = 2000
                        val samplesCount = (SAMPLE_RATE * (chordDurationMs / 1000.0)).toInt()
                        
                        // Get note frequencies for the chord components
                        val chordFreqs = getChordFrequencies(chord)
                        
                        val pcmBuffer = ByteArray(samplesCount)
                        for (i in 0 until samplesCount) {
                            if (!isActive || !_isPlaying.value) break
                            val time = i.toDouble() / SAMPLE_RATE
                            
                            var valSum = 0.0
                            val ampDivider = chordFreqs.size.coerceAtLeast(1) + 1.2
                            
                            // Rich Sub-bass root note (down 1 octave)
                            if (chordFreqs.isNotEmpty()) {
                                val rootFreq = chordFreqs[0] / 2.0
                                valSum += 0.85 * generateWaveValue(2.0 * Math.PI * rootFreq * time, currentWaveform)
                            }
                            
                            // Triad notes
                            for (freq in chordFreqs) {
                                // Fundamental wave
                                valSum += 0.60 * generateWaveValue(2.0 * Math.PI * freq * time, currentWaveform)
                                // Warm second harmonic
                                valSum += 0.18 * generateWaveValue(2.0 * Math.PI * (freq * 1.5) * time, currentWaveform)
                            }
                            
                            var amplitude = valSum / ampDivider
                            
                            // ADSR soft attack and decay to prevent popping/clicking audio issues
                            val progress = i.toDouble() / samplesCount
                            val envelope = when {
                                progress < 0.10 -> progress / 0.10 // Attack over first 10%
                                progress > 0.80 -> (1.0 - progress) / 0.20 // Decaying fadeout over last 20%
                                else -> 1.0 // Sustain
                            }
                            amplitude *= envelope

                            // Pack code into unsigned 8-bit byte representation
                            val pcmByte = (amplitude * 127.0 + 128.0).toInt().coerceIn(0, 255).toByte()
                            pcmBuffer[i] = pcmByte
                        }

                        // Write buffer to stream
                        audioTrack?.write(pcmBuffer, 0, pcmBuffer.size)
                    }
                } while (_isLooping.value && isActive && _isPlaying.value)

                if (isActive) {
                    _isPlaying.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Synth exception during playback", e)
            } finally {
                cleanUpAudio()
            }
        }
    }

    fun stopPlaying() {
        _isPlaying.value = false
        synthJob?.cancel()
        synthJob = null
        cleanUpAudio()
    }

    private fun cleanUpAudio() {
        try {
            audioTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    try {
                        stop()
                    } catch (e: Exception) {}
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup exception", e)
        } finally {
            audioTrack = null
        }
    }

    private fun getChordFrequencies(chordName: String): List<Double> {
        val clean = chordName.trim()
        
        var isMinor = false
        var is7th = false
        var isMaj7 = false
        var rootNote = clean
        
        if (clean.endsWith("maj7") || clean.endsWith("M7")) {
            isMaj7 = true
            rootNote = clean.removeSuffix("maj7").removeSuffix("M7")
        } else if (clean.endsWith("min7") || clean.endsWith("m7")) {
            isMinor = true
            is7th = true
            rootNote = clean.removeSuffix("min7").removeSuffix("m7")
        } else if (clean.endsWith("min") || clean.endsWith("m")) {
            isMinor = true
            rootNote = clean.removeSuffix("min").removeSuffix("m")
        } else if (clean.endsWith("7")) {
            is7th = true
            rootNote = clean.removeSuffix("7")
        } else if (clean.endsWith("maj")) {
            rootNote = clean.removeSuffix("maj")
        }
        
        val baseFreq = frequencies[rootNote] ?: return emptyList()
        val semitones = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B")
        val rootIndex = semitones.indexOf(rootNote).takeIf { it != -1 } ?: 0
        
        fun freqForSemitoneOffset(offset: Int): Double {
            return baseFreq * Math.pow(2.0, offset.toDouble() / 12.0)
        }
        
        val notes = mutableListOf<Double>()
        notes.add(baseFreq)
        
        val thirdOffset = if (isMinor) 3 else 4
        notes.add(freqForSemitoneOffset(thirdOffset))
        notes.add(freqForSemitoneOffset(7))
        
        if (is7th) {
            notes.add(freqForSemitoneOffset(10))
        } else if (isMaj7) {
            notes.add(freqForSemitoneOffset(11))
        }
        
        return notes
    }
}
