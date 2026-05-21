package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.model.SongStyle
import com.example.viewmodel.SongViewModel
import com.example.viewmodel.UiState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IndieSongScreen(
    viewModel: SongViewModel,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()

    // Everforest Inspired Palette
    val bgDark = Color(0xFF2D353B) // Dark earthy charcoal green
    val cardDark = Color(0xFF343F44) // Slightly lighter for structural cards
    val textLight = Color(0xFFD3C6AA) // Soft off-white
    val accentGreen = Color(0xFFA7C080) // Soft green
    val accentOrange = Color(0xFFE69875) // Warm orange
    val borderDark = Color(0xFF475258)

    val sansFont = FontFamily.SansSerif
    val monoFont = FontFamily.Monospace

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardDark, RoundedCornerShape(4.dp))
                    .border(1.dp, borderDark, RoundedCornerShape(4.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "chordweaver".uppercase(),
                        color = accentOrange,
                        fontSize = 14.sp,
                        fontFamily = monoFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generador de Progresiones y Letras",
                        color = textLight,
                        fontSize = 12.sp,
                        fontFamily = monoFont
                    )
                }
            }

            // Genre Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SongStyle.values().forEach { style ->
                    val isSelected = style == selectedStyle
                    val bgColor = if (isSelected) accentGreen else cardDark
                    val textColor = if (isSelected) bgDark else textLight
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(bgColor, RoundedCornerShape(4.dp))
                            .border(1.dp, if (isSelected) accentGreen else borderDark, RoundedCornerShape(4.dp))
                            .clickable { viewModel.selectStyle(style) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.displayName,
                            color = textColor,
                            fontSize = 11.sp,
                            fontFamily = monoFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Main Content Area with state transitions
            AnimatedContent(
                targetState = currentSong,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "SongContent"
            ) { song ->
                if (song != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        
                        // Chord Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardDark, RoundedCornerShape(4.dp))
                                .border(1.dp, borderDark, RoundedCornerShape(4.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "PROGRESIÓN",
                                    color = accentGreen,
                                    fontSize = 10.sp,
                                    fontFamily = monoFont,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = song.chords,
                                    color = textLight,
                                    fontSize = 32.sp,
                                    fontFamily = sansFont,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        // Theory Guide
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(cardDark, RoundedCornerShape(4.dp))
                                    .border(1.dp, borderDark, RoundedCornerShape(4.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "INFO. PISTA",
                                        color = accentOrange,
                                        fontSize = 10.sp,
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = song.title,
                                        color = textLight,
                                        fontSize = 14.sp,
                                        fontFamily = sansFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = song.tempo,
                                        color = textLight.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        fontFamily = monoFont
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(cardDark, RoundedCornerShape(4.dp))
                                    .border(1.dp, borderDark, RoundedCornerShape(4.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "ESCALA RECOMENDADA",
                                        color = accentOrange,
                                        fontSize = 10.sp,
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = song.soloScale,
                                        color = textLight,
                                        fontSize = 14.sp,
                                        fontFamily = sansFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Lyric Prompt
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardDark, RoundedCornerShape(4.dp))
                                .border(1.dp, borderDark, RoundedCornerShape(4.dp))
                                .padding(24.dp)
                        ) {
                            Column {
                                Text(
                                    text = "VERSOS COMPATIBLES",
                                    color = accentGreen,
                                    fontSize = 10.sp,
                                    fontFamily = monoFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = song.lyrics,
                                    color = textLight,
                                    fontSize = 16.sp,
                                    fontFamily = monoFont,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardDark, RoundedCornerShape(4.dp))
                            .border(1.dp, borderDark, RoundedCornerShape(4.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Presiona GENERAR para invocar a la IA.",
                            color = textLight,
                            fontSize = 14.sp,
                            fontFamily = monoFont,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Generate Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (uiState == UiState.Loading) borderDark else accentOrange, RoundedCornerShape(4.dp))
                    .clickable(enabled = uiState != UiState.Loading) {
                        viewModel.generateNewSong()
                    }
                    .padding(vertical = 18.dp)
                    .testTag("generate_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState == UiState.Loading) "GENERANDO..." else "GENERAR",
                    color = bgDark,
                    fontSize = 14.sp,
                    fontFamily = monoFont,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            val isPlaying by viewModel.isPlaying.collectAsState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardDark, RoundedCornerShape(4.dp))
                    .border(1.dp, if (isPlaying) accentGreen else borderDark, RoundedCornerShape(4.dp))
                    .clickable {
                        if (isPlaying) viewModel.stopAccompaniment() else viewModel.playAccompaniment()
                    }
                    .padding(vertical = 12.dp)
                    .testTag("playback_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaying) "DETENER REPRODUCCIÓN (■)" else "REPRODUCIR ACORDES (▶)",
                    color = if (isPlaying) accentGreen else textLight,
                    fontSize = 12.sp,
                    fontFamily = monoFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status/State Message
            val statusText = when (uiState) {
                is UiState.Loading -> "Analizando armonías..."
                is UiState.Success -> (uiState as UiState.Success).message ?: "Listo."
                is UiState.Error -> (uiState as UiState.Error).message
                else -> ""
            }
            
            if (statusText.isNotEmpty()) {
                Text(
                    text = statusText,
                    color = textLight.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontFamily = monoFont,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
