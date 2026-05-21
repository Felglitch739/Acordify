package com.example.model

data class Song(
    val title: String,
    val tempo: String,
    val chords: String,
    val lyrics: String,
    val soloScale: String,
    val soloTip: String,
    val isAiGenerated: Boolean = false
)
