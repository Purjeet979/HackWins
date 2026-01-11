package com.example.snehsaathi.core

import android.util.Log
import com.example.snehsaathi.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GroqClient : AIService {

    private val client = HttpClient(CIO)

    override suspend fun reply(userText: String): String =
        withContext(Dispatchers.IO) {

            Log.d("GROQ_DEBUG", "Sending text: $userText")

            try {
                val messages = JSONArray().apply {
                    put(
                        JSONObject()
                            .put("role", "system")
                            .put(
                                "content",
                                """
                                Role:
                                You are 'Suman', a warm, empathetic, and patient companion for an elderly Indian woman (Dadi).
                    
                                Tone:
                                Gentle, respectful (always use 'Ji'), slow-paced, and encouraging.
                                Never lecture. Never sound like a teacher, doctor, or chatbot.
                    
                                Language:
                                Hinglish — simple Hindi mixed with easy English.
                                Short sentences. Easy to listen to.
                    
                                Key Behaviors:
                                1. Active Listening:
                                   - If Dadi pauses or sounds unsure, wait.
                                   - Do not interrupt immediately.
                                   - Respond calmly.
                    
                                2. Memory Hooks:
                                   - Refer to her family naturally.
                                   - Say: "Rohan school gaya aaj?" instead of generic questions.
                                   - Assume emotional attachment to family.
                    
                                3. Health Nudges:
                                   - Gently remind about water or medicines.
                                   - Weave it into conversation.
                                   - Example: "Baaton baaton mein bhool na jana, paani piya aapne?"
                    
                                4. Emergency Detection:
                                   - If Dadi mentions severe pain, falling, dizziness, or extreme sadness:
                                     Say exactly:
                                     "Main abhi Rohan ko message karti hoon."
                                   - Then trigger the action: [ESCALATE_ACTION]
                    
                                Context (use naturally, do not repeat like a list):
                                - Grandson: Rohan (lives in Mumbai)
                                - Health: High BP, knee pain
                                - Hobbies: Old Bollywood songs, knitting
                    
                                Speaking Rules (VERY IMPORTANT):
                                - Always address her as "Dadi"
                                - Speak in 1–2 short sentences only
                                - Ask at most ONE question
                                - Never overload with advice
                                - Remember: Dadi is listening, not reading
                                """.trimIndent()
                            )


                    )
                    put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", userText)
                    )
                }

                val body = JSONObject().apply {
                    put("model", "llama-3.1-8b-instant")
                    put("messages", messages)
                    put("temperature", 0.6)
                }

                val rawResponse: String =
                    client.post("https://api.groq.com/openai/v1/chat/completions") {
                        header(
                            "Authorization",
                            "Bearer ${BuildConfig.GROQ_API_KEY}"
                        )
                        contentType(ContentType.Application.Json)
                        setBody(body.toString())
                    }.body()

                Log.d("GROQ_RAW", rawResponse)

                val json = JSONObject(rawResponse)

                // 🔴 IMPORTANT: Handle API error responses explicitly
                if (json.has("error")) {
                    val errorMsg =
                        json.getJSONObject("error").optString("message", "Unknown error")
                    Log.e("GROQ_ERROR", errorMsg)
                    return@withContext "Abhi thoda issue ho raha hai beta. Thodi der baad try karein."
                }

                val content =
                    json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .optString("content", "")

                if (content.isBlank()) {
                    Log.e("GROQ_ERROR", "Empty content from Groq")
                    return@withContext "Main yahin hoon. Aap dobara bolna chahenge?"
                }

                content

            } catch (e: Exception) {
                Log.e("GROQ_EXCEPTION", "Groq failed", e)
                "Main yahin hoon. Aap dobara bolna chahenge?"
            }
        }
}
