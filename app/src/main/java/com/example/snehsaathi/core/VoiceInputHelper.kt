package com.example.snehsaathi.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class VoiceInputHelper(
    context: Context,
    private val onResult: (String) -> Unit,
    private val onListeningStart: () -> Unit,
    private val onListeningStop: () -> Unit,
    private val onRmsLevel: ((Float) -> Unit)? = null // ✅ OPTIONAL, SAFE
) {

    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)

    private var lastRmsUpdate = 0L

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VOICE_DEBUG", "Ready for speech")
                onListeningStart()
            }

            override fun onBeginningOfSpeech() {
                Log.d("VOICE_DEBUG", "Speech started")
            }

            override fun onResults(results: Bundle) {
                val text =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()

                Log.d("VOICE_DEBUG", "Speech results: $text")

                onListeningStop()

                if (!text.isNullOrBlank()) {
                    onResult(text)
                }

                speechRecognizer.cancel()
            }

            override fun onError(error: Int) {
                Log.e("VOICE_DEBUG", "Speech error code: $error")
                onListeningStop()
                speechRecognizer.cancel()
            }

            override fun onEndOfSpeech() {
                Log.d("VOICE_DEBUG", "Speech ended")
            }

            // 🔊 SAFE RMS HANDLING (THROTTLED, NO RECURSION)
            override fun onRmsChanged(rmsdB: Float) {
                val now = System.currentTimeMillis()
                if (now - lastRmsUpdate > 100) { // ~10 fps
                    lastRmsUpdate = now
                    onRmsLevel?.invoke(rmsdB.coerceIn(0f, 10f))
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        Log.d("VOICE_DEBUG", "startListening called")
        speechRecognizer.cancel()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("hi", "IN"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer.startListening(intent)
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
}
