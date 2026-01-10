package com.example.snehsaathi

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GroqClient {

    private val client = HttpClient(CIO)

    suspend fun sendMessage(userText: String): String {
        Log.d("GROQ_KEY", BuildConfig.GROQ_API_KEY)

        return withContext(Dispatchers.IO) {
            try {
                val messages = JSONArray().apply {
                    put(
                        JSONObject().put("role", "system")
                            .put("content", "Reply in Hinglish. Keep it supportive and short.")
                    )
                    put(
                        JSONObject().put("role", "user")
                            .put("content", userText)
                    )
                }

                val body = JSONObject().apply {
                    put("model", "llama-3.1-8b-instant")

                    put("messages", messages)
                    put("temperature", 0.7)
                }

                val response: String =
                    client.post("https://api.groq.com/openai/v1/chat/completions") {
                        header("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                        contentType(ContentType.Application.Json)
                        setBody(body.toString())
                    }.body()

                Log.d("GROQ_RAW", response)

                JSONObject(response)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

            } catch (e: Exception) {
                Log.e("GROQ_ERROR", e.toString())
                "I’m here for you. Want to try again?"
            }
        }
    }
}
