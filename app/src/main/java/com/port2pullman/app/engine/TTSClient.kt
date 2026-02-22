package com.port2pullman.app.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.port2pullman.app.BuildConfig
import com.port2pullman.app.debug.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * ElevenLabs Text-to-Speech client.
 *
 * Calls the ElevenLabs REST API to synthesize speech from text,
 * saves the returned audio to a cache file, and plays it via
 * [MediaPlayer] with alarm-stream audio attributes so it's audible
 * even in Do Not Disturb mode.
 *
 * The API key is read from `BuildConfig.ELEVENLABS_API_KEY` which
 * is loaded from `secrets.properties` at build time.
 */
class TTSClient(private val context: Context) {

    companion object {
        private const val TAG = "TTSClient"
        private const val BASE_URL = "https://api.elevenlabs.io/v1"
        private const val DEFAULT_VOICE_ID = "JBFqnCBsd6RMkjVDRZzb"  // "George"
        private const val MODEL_ID = "eleven_flash_v2_5"               // ~75ms latency
        private const val OUTPUT_FORMAT = "mp3_44100_128"
    }

    private var mediaPlayer: MediaPlayer? = null

    /** Whether a valid API key is configured. */
    val isAvailable: Boolean
        get() = BuildConfig.ELEVENLABS_API_KEY.isNotBlank() &&
                BuildConfig.ELEVENLABS_API_KEY != "YOUR_ELEVENLABS_KEY_HERE"

    /**
     * Synthesize [text] to speech and play it immediately.
     *
     * Must be called from a coroutine scope.  The network call runs on
     * [Dispatchers.IO] and playback switches to [Dispatchers.Main].
     */
    suspend fun speak(text: String, voiceId: String = DEFAULT_VOICE_ID) {
        if (!isAvailable) {
            DebugLog.w(TAG, "No ElevenLabs API key — skipping TTS")
            return
        }

        val audioFile = withContext(Dispatchers.IO) {
            synthesize(text, voiceId)
        }

        if (audioFile != null) {
            withContext(Dispatchers.Main) {
                playAudio(audioFile)
            }
        }
    }

    /**
     * Synthesize [text] and save the audio to [outputFile] without
     * playing.  Useful for pre-generating alarm audio at creation time
     * so playback is instant when the condition fires.
     */
    suspend fun synthesizeToFile(
        text: String,
        outputFile: File,
        voiceId: String = DEFAULT_VOICE_ID
    ) {
        if (!isAvailable) {
            DebugLog.w(TAG, "No ElevenLabs API key — skipping TTS file synthesis")
            return
        }

        withContext(Dispatchers.IO) {
            val tempFile = synthesize(text, voiceId)
            if (tempFile != null) {
                tempFile.renameTo(outputFile)
                DebugLog.d(TAG, "Saved TTS audio to ${outputFile.absolutePath}")
            }
        }
    }

    /** Stop any currently playing audio and release resources. */
    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    // ── Internal ────────────────────────────────────────────────────

    /**
     * Call the ElevenLabs TTS endpoint and save the returned audio
     * bytes to a temp file in the app cache directory.
     */
    private fun synthesize(text: String, voiceId: String): File? {
        val apiKey = BuildConfig.ELEVENLABS_API_KEY
        val url = URL("$BASE_URL/text-to-speech/$voiceId")

        DebugLog.d(TAG, "Synthesizing TTS: '${text.take(60)}…' (voice=$voiceId)")

        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("xi-api-key", apiKey)
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.doOutput = true

            // Build JSON payload — escape quotes in text
            val escapedText = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")

            val body = """
                {
                    "text": "$escapedText",
                    "model_id": "$MODEL_ID",
                    "output_format": "$OUTPUT_FORMAT"
                }
            """.trimIndent()

            connection.outputStream.use { it.write(body.toByteArray()) }

            if (connection.responseCode != 200) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                DebugLog.e(TAG, "ElevenLabs API error ${connection.responseCode}: $error")
                connection.disconnect()
                return null
            }

            // Save audio to temp file
            val file = File(context.cacheDir, "tts_${System.currentTimeMillis()}.mp3")
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()

            DebugLog.i(TAG, "TTS audio saved (${file.length()} bytes)")
            file
        } catch (e: Exception) {
            DebugLog.e(TAG, "TTS synthesis failed: ${e.message}")
            null
        }
    }

    /**
     * Play an audio file through [MediaPlayer] with alarm-level audio
     * attributes so it plays over Do-Not-Disturb and other audio.
     */
    private fun playAudio(file: File) {
        stop() // Release any previous player

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    DebugLog.d(TAG, "TTS playback completed")
                    it.release()
                    file.delete() // Clean up temp file
                }
                setOnErrorListener { _, what, extra ->
                    DebugLog.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    file.delete()
                    true
                }
            }
            DebugLog.d(TAG, "TTS playback started")
        } catch (e: Exception) {
            DebugLog.e(TAG, "Failed to play TTS audio: ${e.message}")
            file.delete()
        }
    }
}
