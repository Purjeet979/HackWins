package com.example.snehsaathi.ui.main

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import java.util.Calendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.snehsaathi.core.*
import com.example.snehsaathi.features.memory.FirebaseLogger
import com.example.snehsaathi.features.memory.MemoryManager
import com.example.snehsaathi.features.memory.MemoryRepository
import com.example.snehsaathi.features.nostalgia.NostalgiaEngine
import com.example.snehsaathi.features.medication.MedicationReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    private val aiService: AIService = GroqClient()
    private val featureManager = FeatureManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(
            "VOICE_DEBUG",
            "Speech available: ${SpeechRecognizer.isRecognitionAvailable(this)}"
        )

        tts = TextToSpeech(this, this)

        // 🔔 MEDICATION REMINDER — DAILY AT FIXED TIME (8:00 AM)
        if (FeatureFlags.MEDICATION) {

            val initialDelayMillis =
                calculateInitialDelay(
                    hour = 8,
                    minute = 0
                )

            val medicationWork =
                PeriodicWorkRequestBuilder<MedicationReminderWorker>(
                    1,
                    TimeUnit.DAYS
                )
                    .setInitialDelay(
                        initialDelayMillis,
                        TimeUnit.MILLISECONDS
                    )
                    .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "MEDICATION_REMINDER",
                ExistingPeriodicWorkPolicy.REPLACE,
                medicationWork
            )
        }

        setContent {
            AppUI(
                aiService = aiService,
                featureManager = featureManager,
                tts = tts,
                onSaveDailySummary = { summary ->
                    saveDailySummary(summary)
                }
            )
        }

    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("hi", "IN")
        }
    }

    // ⏰ Calculate delay until next scheduled time
    private fun calculateInitialDelay(
        hour: Int,
        minute: Int
    ): Long {
        val now = Calendar.getInstance()

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }

    // 📅 API-24 safe daily date
    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // ✍️ Save daily summary for Ghostwriter
    private fun saveDailySummary(summary: String) {
        val today = getTodayDate()

        val data = mapOf(
            "text" to summary,
            "timestamp" to System.currentTimeMillis()
        )

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("daily_summaries")
            .document(today)
            .set(data)
    }
}

@Composable
fun AppUI(
    aiService: AIService,
    featureManager: FeatureManager,
    tts: TextToSpeech,
    onSaveDailySummary: (String) -> Unit
){
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var responseText by remember {
        mutableStateOf(
            "Dadi, namaste. Main yahin hoon. Aap jab chaahen bol sakti hain."
        )
    }

    var isListening by remember { mutableStateOf(false) }

    // 🔹 MEMORY (Firebase)
    val memoryRepository = remember { MemoryRepository() }
    val memoryManager = remember { MemoryManager(memoryRepository) }

    val voiceHelper = remember {
        VoiceInputHelper(context) { spokenText ->
            Log.d("VOICE_DEBUG", "Heard: $spokenText")

            FirebaseLogger.logConversation(
                text = spokenText,
                type = "USER"
            )

            scope.launch(Dispatchers.Main) {

                when (val result = featureManager.processUserText(spokenText)) {

                    // 🚨 SCAM BLOCK
                    is FeatureResult.Block -> {
                        responseText = result.message

                        FirebaseLogger.logConversation(
                            text = result.message,
                            type = "SCAM"
                        )

                        tts.speak(
                            result.message,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "BLOCK"
                        )
                    }

                    // 🤖 AI RESPONSE
                    is FeatureResult.Pass -> {

                        val nostalgiaContext =
                            NostalgiaEngine.detect(result.userText)

                        responseText = "Thinking..."

                        val nostalgiaPrompt =
                            if (nostalgiaContext != null) {
                                """
                                The user is feeling nostalgic.
                                Respond warmly, gently, and emotionally.
                                Use natural, respectful Hinglish.
                                Prefer phrases like:
                                "Aaj achanak kaunsa pal yaad aa gaya?"
                                Avoid asking "Kyun".

                                User says: ${result.userText}
                                """.trimIndent()
                            } else {
                                result.userText
                            }

                        memoryRepository.fetchRecentMemories { memories ->

                            scope.launch {

                                val finalPrompt =
                                    memoryManager.enrichPrompt(
                                        userText = nostalgiaPrompt,
                                        memories = memories
                                    )

                                val reply = aiService.reply(finalPrompt)
                                responseText = reply

                                onSaveDailySummary(reply)


                                if (memoryManager.shouldSave(result.userText)) {
                                    memoryRepository.saveMemory(result.userText)
                                }

                                FirebaseLogger.logConversation(
                                    text = reply,
                                    type = "AI"
                                )

                                tts.speak(
                                    reply,
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "AI"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isListening = true
            voiceHelper.startListening()
        }
    }

    fun startListening() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            isListening = true
            voiceHelper.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = responseText)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            Log.d("VOICE_DEBUG", "Speak button clicked")
            startListening()
        }) {
            Text("Speak")
        }
    }
}
