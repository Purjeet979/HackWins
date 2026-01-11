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
    private val onResult: (String) -> Unit
) {

    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VOICE_DEBUG", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("VOICE_DEBUG", "Speech started")
            }

            override fun onResults(results: Bundle) {
                val text =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()

                Log.d("VOICE_DEBUG", "Speech results: $text")

                if (!text.isNullOrBlank()) {
                    onResult(text)
                }
                speechRecognizer.stopListening()

            }

            override fun onError(error: Int) {
                Log.e("VOICE_DEBUG", "Speech error code: $error")

                if (error == SpeechRecognizer.ERROR_CLIENT) {
                    speechRecognizer.cancel()
                }
            }


            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer.startListening(intent)
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
}
