package com.example.data

import com.example.model.Song
import com.example.model.SongStyle

object LocalSongs {
    private val indiePlaylist = listOf(
        Song(
            title = "Ecos de Lluvia",
            tempo = "75 BPM - Slow Acoustic Vibe",
            chords = "Am - F - C - G",
            lyrics = "Las luces se apagan despacio en la ciudad,\ntu sombra se borra en la fría marea,\nguardo los versos que no te quise cantar,\nbajo este cielo que ya no nos planea.",
            soloScale = "A Minor Pentatonic",
            soloTip = "Focus on the root note A (5th fret low E string) and play with slow, expressive finger vibratos and subtle bends."
        )
    )

    private val rockPlaylist = listOf(
        Song(
            title = "Carretera Libre",
            tempo = "120 BPM - High Energy Surf Rock",
            chords = "E5 - G5 - A5 - C5",
            lyrics = "Subimos al coche con velocidad,\nla radio tocando estruendo y de rock,\nno importan las horas ni la oscuridad,\nque tiemblen las calles y rompa el reloj.",
            soloScale = "E Minor Pentatonic",
            soloTip = "Play bouncy, double-time staccato lines using short muted strokes on high frets for absolute energy."
        )
    )

    private val jazzPlaylist = listOf(
        Song(
            title = "Café Nocturno",
            tempo = "60 BPM - Slow Jazz Ballad",
            chords = "Am7 - Dm7 - G7 - Cmaj7",
            lyrics = "La noche cobija este viejo rincón,\nun piano cansado suspira su son,\ntu taza vacía me habla de adiós,\nmientras el humo dibuja el dolor.",
            soloScale = "A Dorian Mode",
            soloTip = "Highlight the flat 5th (Eb note) to spice your lines with a classic, blues-scale nocturnal melancholy."
        )
    )

    private val popPlaylist = listOf(
        Song(
            title = "Baile en la Cocina",
            tempo = "110 BPM - Sweet Upbeat Pop",
            chords = "D - G - A - D",
            lyrics = "Giremos tocando silvidos al son,\nde tazas que vibran cantando a la par,\ndibujas sonrisas de melocotón,\ny el viento se une queriendo bailar.",
            soloScale = "D Major Pentatonic",
            soloTip = "Play playful, major-leaning pentatonic lines with rapid hammer-ons and slide up high."
        )
    )

    private val rbPlaylist = listOf(
        Song(
            title = "Faros y Niebla",
            tempo = "65 BPM - Late Night Smooth Soul",
            chords = "Dm9 - G13 - Cmaj9 - Fmaj7",
            lyrics = "La niebla de vela suspende la luz,\nel río dibuja tu fiel silueta,\nel faro distante me dicta tu cruz,\ny el saxo llorando silencia al poeta.",
            soloScale = "D Minor Pentatonic / Blues",
            soloTip = "Play with swing phrasing, delaying your note entry slightly behind the beat for that laidback pocket."
        )
    )

    val playlist = indiePlaylist + rockPlaylist + jazzPlaylist + popPlaylist + rbPlaylist

    fun getRandom(style: SongStyle, currentSong: Song? = null): Song {
        val targets = when (style) {
            SongStyle.INDIE -> indiePlaylist
            SongStyle.ROCK -> rockPlaylist
            SongStyle.JAZZ -> jazzPlaylist
            SongStyle.POP -> popPlaylist
            SongStyle.R_AND_B -> rbPlaylist
        }
        val filtered = targets.filter { it.title != currentSong?.title }
        return if (filtered.isEmpty()) {
            targets.random()
        } else {
            filtered.random()
        }
    }
}
