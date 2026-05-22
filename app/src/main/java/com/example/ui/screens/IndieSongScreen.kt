package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.model.SongStyle
import com.example.viewmodel.SongViewModel
import com.example.viewmodel.UiState

// Database of standard guitar chord fingerings (6th string to 1st string)
private val chordFingerings = mapOf(
    "C" to intArrayOf(-1, 3, 2, 0, 1, 0),
    "Cmaj7" to intArrayOf(-1, 3, 2, 0, 0, 0),
    "CM7" to intArrayOf(-1, 3, 2, 0, 0, 0),
    "C7" to intArrayOf(-1, 3, 2, 3, 1, 0),
    "Am" to intArrayOf(-1, 0, 2, 2, 1, 0),
    "Am7" to intArrayOf(-1, 0, 2, 0, 1, 0),
    "Dm" to intArrayOf(-1, -1, 0, 2, 3, 1),
    "Dm7" to intArrayOf(-1, -1, 0, 2, 1, 1),
    "G" to intArrayOf(3, 2, 0, 0, 0, 3),
    "G7" to intArrayOf(3, 2, 0, 0, 0, 1),
    "F" to intArrayOf(1, 3, 3, 2, 1, 1),
    "Fmaj7" to intArrayOf(-1, 3, 3, 2, 1, 0),
    "E" to intArrayOf(0, 2, 2, 1, 0, 0),
    "Em" to intArrayOf(0, 2, 2, 0, 0, 0),
    "Em7" to intArrayOf(0, 2, 0, 0, 0, 0),
    "E5" to intArrayOf(0, 2, 2, -1, -1, -1),
    "G5" to intArrayOf(3, 5, 5, -1, -1, -1),
    "A5" to intArrayOf(-1, 0, 2, 2, -1, -1),
    "C5" to intArrayOf(-1, 3, 5, 5, -1, -1),
    "D5" to intArrayOf(-1, -1, 0, 2, 3, -1),
    "A" to intArrayOf(-1, 0, 2, 2, 2, 0),
    "D" to intArrayOf(-1, -1, 0, 2, 3, 2),
    "D7" to intArrayOf(-1, -1, 0, 2, 1, 2),
    "B5" to intArrayOf(-1, 2, 4, 4, -1, -1),
    "F#m" to intArrayOf(2, 4, 4, 2, 2, 2),
    "C#m" to intArrayOf(-1, 4, 6, 6, 5, 4),
    "Bm" to intArrayOf(-1, 2, 4, 4, 3, 2),
    "Dm9" to intArrayOf(-1, 5, 3, 5, 5, -1),
    "G13" to intArrayOf(3, -1, 3, 4, 5, -1),
    "Cmaj9" to intArrayOf(-1, 3, 2, 4, 3, -1)
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IndieSongScreen(
    viewModel: SongViewModel,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // Moody Musician Premium Theme Colors (Spotify/GuitarTuna vibe)
    val bgDark = Color(0xFF000000) // Pitch Black Background
    val cardDark = Color(0xCC121212) // Semi-transparent dark grey (80% opacity)
    val textLight = Color(0xFFFFFFFF) // Pure White
    val accentGreen = Color(0xFF1DB954) // Vibrant GuitarTuna Green
    val borderDark = Color(0x22FFFFFF) // Faint White Border for glassmorphism look
    val labelColor = Color(0xB3FFFFFF) // Faint label text

    val sansFont = FontFamily.SansSerif

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as UiState.Error).message,
                duration = SnackbarDuration.Long
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
            .drawBehind {
                // Subtle guitar fretboard background drawing (3-5% opacity)
                val width = size.width
                val height = size.height
                val stringCount = 6
                val fretCount = 10
                val fretColor = Color.White.copy(alpha = 0.03f)
                val stringColor = Color.White.copy(alpha = 0.04f)

                // Draw frets (horizontal lines)
                for (i in 0..fretCount) {
                    val y = (height / fretCount) * i
                    drawLine(
                        color = fretColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 2f
                    )

                    // Fret marker dots (single dot at 3rd, 5th, 7th, 9th)
                    if (i == 3 || i == 5 || i == 7 || i == 9) {
                        val centerY = (height / fretCount) * (i - 0.5f)
                        drawCircle(
                            color = fretColor,
                            radius = 12f,
                            center = Offset(width / 2, centerY)
                        )
                    }
                }

                // Draw strings (vertical lines)
                for (i in 0 until stringCount) {
                    val x = (width / (stringCount + 1)) * (i + 1)
                    drawLine(
                        color = stringColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f + (i * 0.5f) // strings get thicker
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardDark, RoundedCornerShape(12.dp))
                    .border(1.dp, borderDark, RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "ACORDIFY",
                        color = textLight,
                        fontSize = 20.sp,
                        fontFamily = sansFont,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generador de Progresiones y Letras",
                        color = labelColor,
                        fontSize = 12.sp,
                        fontFamily = sansFont,
                        fontWeight = FontWeight.Medium
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
                    val borderStroke = if (isSelected) accentGreen else borderDark

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .border(1.dp, borderStroke, RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectStyle(style) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.displayName,
                            color = textColor,
                            fontSize = 11.sp,
                            fontFamily = sansFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Main Content Area
            if (uiState == UiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingGuitarPick(accentColor = accentGreen)
                }
            } else {
                AnimatedContent(
                    targetState = currentSong,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                    },
                    label = "SongContent"
                ) { song ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        // Chord Display - HERO ELEMENT
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardDark, RoundedCornerShape(12.dp))
                                .border(1.dp, borderDark, RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "PROGRESIÓN",
                                    color = accentGreen,
                                    fontSize = 11.sp,
                                    fontFamily = sansFont,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // LED Glow style text
                                Text(
                                    text = song.chords,
                                    color = textLight,
                                    fontSize = 36.sp,
                                    fontFamily = sansFont,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 1.sp,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = accentGreen.copy(alpha = 0.8f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 15f
                                        )
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Minimal Guitar Fingering Charts
                                val chordList = song.chords.split("-").map { it.trim() }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    chordList.forEach { chordName ->
                                        if (chordName.isNotEmpty()) {
                                            ChordDiagram(chordName = chordName, accentColor = accentGreen)
                                        }
                                    }
                                }
                            }
                        }

                        // Info Panels (Lyric & Solo Guide)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(cardDark, RoundedCornerShape(12.dp))
                                    .border(1.dp, borderDark, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "PISTA",
                                        color = accentGreen,
                                        fontSize = 10.sp,
                                        fontFamily = sansFont,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
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
                                        color = labelColor,
                                        fontSize = 11.sp,
                                        fontFamily = sansFont,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(cardDark, RoundedCornerShape(12.dp))
                                    .border(1.dp, borderDark, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "ESCALA SOLISTA",
                                        color = accentGreen,
                                        fontSize = 10.sp,
                                        fontFamily = sansFont,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
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

                        // Lyrics
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardDark, RoundedCornerShape(12.dp))
                                .border(1.dp, borderDark, RoundedCornerShape(12.dp))
                                .padding(20.dp)
                        ) {
                            Column {
                                Text(
                                    text = "VERSOS COMPATIBLES",
                                    color = accentGreen,
                                    fontSize = 10.sp,
                                    fontFamily = sansFont,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = song.lyrics,
                                    color = textLight,
                                    fontSize = 14.sp,
                                    fontFamily = sansFont,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Playback Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardDark, RoundedCornerShape(12.dp))
                    .border(1.dp, if (isPlaying) accentGreen else borderDark, RoundedCornerShape(12.dp))
                    .clickable {
                        if (isPlaying) viewModel.stopAccompaniment() else viewModel.playAccompaniment()
                    }
                    .padding(vertical = 14.dp)
                    .testTag("playback_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaying) "DETENER REPRODUCCIÓN (■)" else "REPRODUCIR ACORDES (▶)",
                    color = if (isPlaying) accentGreen else textLight,
                    fontSize = 12.sp,
                    fontFamily = sansFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Generate Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (uiState == UiState.Loading) borderDark else accentGreen, RoundedCornerShape(12.dp))
                    .clickable(enabled = uiState != UiState.Loading) {
                        viewModel.generateNewSong()
                    }
                    .padding(vertical = 16.dp)
                    .testTag("generate_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Generate",
                        tint = bgDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GENERAR",
                        color = bgDark,
                        fontSize = 15.sp,
                        fontFamily = sansFont,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Custom Snackbar Host for clean error Snackbars
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            Snackbar(
                modifier = Modifier.padding(8.dp),
                containerColor = Color(0xFF121212),
                contentColor = Color.White,
                actionColor = accentGreen,
                shape = RoundedCornerShape(8.dp),
                action = {
                    Text(
                        text = "OK",
                        color = accentGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = sansFont,
                        modifier = Modifier
                            .clickable { data.dismiss() }
                            .padding(8.dp)
                    )
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color.Red
                    )
                    Text(
                        text = data.visuals.message,
                        color = textLight,
                        fontFamily = sansFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingGuitarPick(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PickAnimation")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinRotation"
    )

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(50.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
        ) {
            val path = androidx.compose.ui.graphics.Path().apply {
                // Top center
                moveTo(size.width / 2f, 0f)
                // Curve to right shoulder
                cubicTo(
                    size.width * 0.9f, 0f,
                    size.width, size.height * 0.3f,
                    size.width * 0.8f, size.height * 0.7f
                )
                // Down to bottom tip
                lineTo(size.width / 2f, size.height)
                // Left shoulder curve
                lineTo(size.width * 0.2f, size.height * 0.7f)
                cubicTo(
                    0f, size.height * 0.3f,
                    size.width * 0.1f, 0f,
                    size.width / 2f, 0f
                )
            }
            drawPath(
                path = path,
                color = accentColor
            )
        }
    }
}

@Composable
fun ChordDiagram(
    chordName: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF1DB954)
) {
    val cleanName = chordName.trim()
    val fingering = chordFingerings[cleanName]
        ?: chordFingerings[cleanName.replace("m7", "7")]
        ?: chordFingerings[cleanName.take(2)]
        ?: chordFingerings[cleanName.take(1)]
        ?: intArrayOf(-1, -1, -1, -1, -1, -1)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(55.dp)
    ) {
        Text(
            text = cleanName,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            val width = size.width
            val height = size.height

            val strings = 6
            val frets = 4

            val stringSpacing = width / (strings - 1)
            val fretSpacing = height / frets

            // Draw nut (slightly thicker)
            drawLine(
                color = Color.Gray,
                start = Offset(0f, 0f),
                end = Offset(width, 0f),
                strokeWidth = 3f
            )

            // Draw frets
            for (f in 1..frets) {
                val y = f * fretSpacing
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Draw strings
            for (s in 0 until strings) {
                val x = s * stringSpacing
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }

            // Draw fret dots and muted/open marks
            for (s in 0 until strings) {
                val fret = fingering[s]
                val x = s * stringSpacing

                if (fret == -1) {
                    // Draw "X" above nut
                    val sz = 4f
                    val y = -fretSpacing / 2
                    drawLine(
                        color = Color.Red.copy(alpha = 0.7f),
                        start = Offset(x - sz, y - sz),
                        end = Offset(x + sz, y + sz),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color.Red.copy(alpha = 0.7f),
                        start = Offset(x + sz, y - sz),
                        end = Offset(x - sz, y + sz),
                        strokeWidth = 1.5f
                    )
                } else if (fret == 0) {
                    // Draw "O" above nut
                    drawCircle(
                        color = Color.LightGray,
                        radius = 3.5f,
                        center = Offset(x, -fretSpacing / 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                } else {
                    // Draw finger dot
                    val y = (fret - 0.5f) * fretSpacing
                    drawCircle(
                        color = accentColor,
                        radius = 5f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
