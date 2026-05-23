package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.model.Song
import com.example.model.SongStyle
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>
)

data class ChatCompletionResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: ChatMessage?
)

data class SongResponseJson(
    val title: String,
    val tempo: String,
    val chords: String,
    val lyrics: String,
    val soloScale: String,
    val soloTip: String
)

object GeminiManager {
    private const val TAG = "OpenAIManager"
    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun hasValidApiKey(): Boolean {
        return try {
            val key = BuildConfig.OPENAI_API_KEY.trim()
            key.isNotEmpty() && 
            key != "MY_OPENAI_API_KEY" && 
            !key.contains("PLACEHOLDER") &&
            key.length >= 10
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generateIndieSong(style: SongStyle): Song? = withContext(Dispatchers.IO) {
        if (!hasValidApiKey()) {
            Log.d(TAG, "No valid API Key found. Falling back to local dataset.")
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

        val requestBodyData = ChatCompletionRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                ChatMessage(role = "system", content = "You are a helpful assistant that writes songs in JSON format."),
                ChatMessage(role = "user", content = prompt)
            )
        )

        val jsonBody = gson.toJson(requestBodyData)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toRequestBody(mediaType)
        
        var baseUrl = BuildConfig.OPENAI_BASE_URL.trim()
        if (baseUrl.isEmpty()) baseUrl = "https://api.openai.com/"
        if (!baseUrl.endsWith("/")) baseUrl += "/"

        val request = Request.Builder()
            .url("${baseUrl}v1/chat/completions")
            .post(body)
            .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY.trim()}")
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response: ${response.code}")
                    return@withContext null
                }
                
                val responseBodyStr = response.body?.string() ?: return@withContext null
                val completionResponse = gson.fromJson(responseBodyStr, ChatCompletionResponse::class.java)
                val rawText = completionResponse.choices?.firstOrNull()?.message?.content
                
                if (rawText != null) {
                    val cleanJson = cleanJsonString(rawText)
                    val parsed = gson.fromJson(cleanJson, SongResponseJson::class.java)
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
            }
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
