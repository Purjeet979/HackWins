package com.example.snehsaathi

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.wss
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class GeminiLiveClient {

    // --- PASTE YOUR API KEY HERE ---
    private val apiKey = "AIzaSyDYs_6-JX7xmvhE5AmdVh8NDTm1fkx_bNs"
    // -------------------------------

    // We create the client ONCE and never close it until the app dies.
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000 // Keep connection alive
        }
    }

    private var isRunning = false

    // --- EMULATOR SETTINGS (16000Hz is safer) ---
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    // Use a larger buffer to prevent "Channel Cancelled" errors
    private val bufferSize = minBufferSize * 4

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    @SuppressLint("MissingPermission")
    suspend fun startSession() {
        if (isRunning) return // Prevent double clicks
        isRunning = true
        setupAudio()

        val hostUrl = "generativelanguage.googleapis.com"
        val pathUrl = "/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"

        try {
            Log.d("SnehSaathi", "Attempting connection...")
            client.wss(method = HttpMethod.Get, host = hostUrl, path = pathUrl) {
                Log.d("SnehSaathi", "Connected to Gemini! Initializing...")

                // 1. Send Setup Message
                send(Frame.Text(getSetupJson().toString()))

                // --- STABILITY FIX: Wait 1s for server to handshake ---
                delay(1000)

                // 2. Start Microphone Stream
                val session = this
                launch(Dispatchers.IO) {
                    streamMicrophone(session)
                }

                // 3. Receive Audio
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        handleResponse(frame.readText())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SnehSaathi", "Connection Error: ${e.message}")
        } finally {
            // Only stop recording, DO NOT close the client
            disconnect()
        }
    }

    private fun getSetupJson(): JSONObject {
        return JSONObject("""
            {
              "setup": {
                "model": "models/gemini-2.0-flash-exp",
                "generationConfig": {
                  "responseModalities": ["AUDIO", "TEXT"],
                  "speechConfig": {
                    "voiceConfig": { "prebuiltVoiceConfig": { "voiceName": "Aoede" } }
                  }
                },
                "systemInstruction": {
                  "parts": [ { "text": "You are Suman, a kind companion for an Indian grandmother. Speak in Hinglish. Keep responses short, warm, and respectful." } ]
                }
              }
            }
        """.trimIndent())
    }

    @SuppressLint("MissingPermission")
    private suspend fun streamMicrophone(session: DefaultClientWebSocketSession) {
        val data = ByteArray(minBufferSize)

        try {
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                Log.d("SnehSaathi", "Microphone streaming started...")
            } else {
                Log.e("SnehSaathi", "Audio Record failed to initialize")
                return
            }

            while (isRunning && session.isActive) {
                val read = audioRecord?.read(data, 0, data.size) ?: 0
                if (read > 0) {
                    val base64Audio = Base64.encodeToString(data, 0, read, Base64.NO_WRAP)

                    val msg = JSONObject()
                        .put("realtimeInput", JSONObject()
                            .put("mediaChunks", JSONArray()
                                .put(JSONObject()
                                    .put("mimeType", "audio/pcm")
                                    .put("data", base64Audio))))

                    session.send(Frame.Text(msg.toString()))
                }
            }
        } catch (e: Exception) {
            Log.e("SnehSaathi", "Mic Stream Error: ${e.message}")
        }
    }

    private fun handleResponse(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            if (json.has("serverContent")) {
                val content = json.getJSONObject("serverContent")
                if (content.has("modelTurn")) {
                    val parts = content.getJSONObject("modelTurn").getJSONArray("parts")
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            val audioData = part.getJSONObject("inlineData").getString("data")
                            val decoded = Base64.decode(audioData, Base64.NO_WRAP)
                            audioTrack?.write(decoded, 0, decoded.size)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SnehSaathi", "Response Parse Error: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupAudio() {
        try {
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(audioFormat).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("SnehSaathi", "Audio Init Error: ${e.message}")
        }
    }

    fun disconnect() {
        isRunning = false
        Log.d("SnehSaathi", "Disconnecting audio...")
        try {
            // STOP AUDIO ONLY - DO NOT CLOSE CLIENT
            audioRecord?.stop()
            audioRecord?.release()
            audioTrack?.stop()
            audioTrack?.release()

            // audioRecord = null
            // audioTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
