package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.model.Song
import com.example.model.SongStyle
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String = "application/json",
    @Json(name = "temperature") val temperature: Double = 1.0
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = GenerationConfig()
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

@JsonClass(generateAdapter = true)
data class SongResponseJson(
    val title: String,
    val tempo: String,
    val chords: String,
    val lyrics: String,
    val soloScale: String,
    val soloTip: String
)

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val songAdapter = moshi.adapter(SongResponseJson::class.java)
}

object GeminiManager {
    private const val TAG = "GeminiManager"

    fun hasValidApiKey(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY.trim()
            key.isNotEmpty() && 
            key != "MY_GEMINI_API_KEY" && 
            !key.contains("PLACEHOLDER") &&
            key.length >= 10
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generateIndieSong(style: SongStyle): Song? = withContext(Dispatchers.IO) {
        if (!hasValidApiKey()) {
            Log.d(TAG, "No valid API Gen key found. Falling back to local dataset.")
            return@withContext null
        }

        val styleInstruction = when (style) {
            SongStyle.INDIE -> """
                Style: Deeply melancholic acoustic indie-folk/rock song in Spanish.
                Themes: Sadness, nostalgia, cold rain, sea, memory, or silence.
                Chords: A melancholic 4-chord progression separated by hyphens (e.g., Am - F - C - G).
                Solo scale: A Minor Pentatonic, D Minor Pentatonic etc.
            """.trimIndent()
            
            SongStyle.ROCK -> """
                Style: High energy, driving rock or surf-rock song in Spanish.
                Themes: Road trips, rebellion, loud nights, speed, breaking free.
                Chords: Rock progressions separated by hyphens (e.g., E5 - G5 - A5 - C5).
                Solo scale: E Minor Pentatonic, A Blues Scale etc.
            """.trimIndent()
            
            SongStyle.JAZZ -> """
                Style: Slow, jazzy, melancholic indie-soul/jazz song in Spanish.
                Themes: Late-night reflections, lonely cafes, neon lights, or urban nostalgia.
                Chords: Progression featuring jazz extensions separated by hyphens (e.g., Am7 - Dm7 - G7 - Cmaj7).
                Solo scale: A Dorian Mode, D Minor Pentatonic etc.
            """.trimIndent()
            
            SongStyle.POP -> """
                Style: Upbeat, energetic, and happy pop song in Spanish.
                Themes: Summer, laughter, dancing, or youthful energy.
                Chords: Catchy, bright developments separated by hyphens (e.g., G - C - D - G).
                Solo scale: G Major Pentatonic, D Major Pentatonic etc.
            """.trimIndent()

            SongStyle.R_AND_B -> """
                Style: Smooth, late-night R&B or soul song in Spanish.
                Themes: Romance, smooth moves, city lights, feeling the groove.
                Chords: Soulful progressions with 9ths or 13ths separated by hyphens (e.g., Dm9 - G13 - Cmaj9 - Fmaj7).
                Solo scale: D Minor Pentatonic / Blues etc.
            """.trimIndent()
        }

        val prompt = """
            $styleInstruction

            You MUST return the result EXACTLY as a raw JSON object with this shape:
            {
              "title": "A beautiful, short title in Spanish matching the requested style",
              "tempo": "A specific tempo in BPM with a description matching the style (e.g. '75 BPM - Slow Acoustic Vibe' or '115 BPM - Upbeat Bedroom Pop')",
              "chords": "Refined and precise chords structure based on the style matching the guidelines above, separated by hyphens",
              "lyrics": "Exactly a 4-line verse of high-quality poetry in Spanish matching the semantic themes, split by newlines",
              "soloScale": "The specific Minor Pentatonic scale matching the chord key for playing lead guitars/licks (e.g., A Minor Pentatonic, D Minor Pentatonic, E Minor Pentatonic, B Minor Pentatonic, F# Minor Pentatonic, C# Minor Pentatonic)",
              "soloTip": "A brief, actionable tip for playing guitar solos over this specific progression using that scale."
            }
            Do not wrap the response in backticks or markdown formatting. Output only the raw valid JSON.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig()
        )

        try {
            val response = GeminiClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (rawText != null) {
                val cleanJson = cleanJsonString(rawText)
                val parsed = GeminiClient.songAdapter.fromJson(cleanJson)
                if (parsed != null) {
                    return@withContext Song(
                        title = parsed.title.trim(),
                        tempo = parsed.tempo.trim(),
                        chords = parsed.chords.trim(),
                        lyrics = parsed.lyrics.trim(),
                        soloScale = parsed.soloScale.trim(),
                        soloTip = parsed.soloTip.trim(),
                        isAiGenerated = true
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error generating indie song", e)
            null
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
}
