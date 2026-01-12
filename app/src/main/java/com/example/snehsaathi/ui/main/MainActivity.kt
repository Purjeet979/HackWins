package com.example.snehsaathi.ui.main

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
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.snehsaathi.R
import com.example.snehsaathi.core.*
import com.example.snehsaathi.features.medication.MedicationReminderWorker
import com.example.snehsaathi.features.memory.*
import com.example.snehsaathi.features.nostalgia.NostalgiaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private val aiService: AIService = GroqClient()
    private val featureManager = FeatureManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("VOICE_DEBUG", "Speech available: ${SpeechRecognizer.isRecognitionAvailable(this)}")

        tts = TextToSpeech(this, this)

        if (FeatureFlags.MEDICATION) {
            val delay = calculateInitialDelay(8, 0)
            val work =
                PeriodicWorkRequestBuilder<MedicationReminderWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "MEDICATION_REMINDER",
                ExistingPeriodicWorkPolicy.REPLACE,
                work
            )
        }

        setContent {
            AppUI(
                aiService = aiService,
                featureManager = featureManager,
                tts = tts,
                onSaveDailySummary = { saveDailySummary(it) }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("hi", "IN")
        }
    }

    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }

    private fun saveDailySummary(summary: String) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("daily_summaries")
            .document(date)
            .set(
                mapOf(
                    "text" to summary,
                    "timestamp" to System.currentTimeMillis()
                )
            )
    }
}

@Composable
fun AppUI(
    aiService: AIService,
    featureManager: FeatureManager,
    tts: TextToSpeech,
    onSaveDailySummary: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var responseText by remember {
        mutableStateOf("Dadi, namaste. Main yahin hoon. Aap jab chaahen bol sakti hain.")
    }

    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var rmsLevel by remember { mutableStateOf(0f) }
    var showRipple by remember { mutableStateOf(false) }

    val memoryRepository = remember { MemoryRepository() }
    val memoryManager = remember { MemoryManager(memoryRepository) }

    // 🌸 High accessibility background
    val backgroundBrush = Brush.radialGradient(
        listOf(
            Color(0xFFFFFDD0), // Very light cream
            Color(0xFFFFF9C4), // Pale Yellow
            Color(0xFFFFECB3)  // Amber Light
        ),
        center = Offset(0.5f, 0.4f),
        radius = 1200f
    )

    val textColor = Color(0xFF4A3B32) // Dark Brown

    // 🎤 Mic state glow + pulse
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening || isSpeaking) 1.12f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val volumeScale = (1f + (rmsLevel / 20f)).coerceIn(1f, 1.2f)

    val rippleScale by animateFloatAsState(
        targetValue = if (showRipple) 1.6f else 1f,
        animationSpec = tween(600),
        finishedListener = { showRipple = false },
        label = "ripple"
    )

    // Glow color logic
    val glowColor = when {
        isListening -> Color(0xFFB39DDB)   // soft purple (listening)
        isSpeaking -> Color(0xFFFFE0B2)   // warm cream (AI speaking)
        else -> Color.White.copy(alpha = 0.35f)
    }

    val voiceHelper = remember {
        VoiceInputHelper(
            context = context,
            onResult = { spokenText ->
                FirebaseLogger.logConversation(spokenText, "USER")

                scope.launch(Dispatchers.Main) {
                    when (val result = featureManager.processUserText(spokenText)) {

                        is FeatureResult.Block -> {
                            responseText = result.message
                            isSpeaking = true
                            tts.speak(result.message, TextToSpeech.QUEUE_FLUSH, null, "BLOCK")
                        }

                        is FeatureResult.Pass -> {
                            responseText = "Soch rahi hoon…"

                            val prompt =
                                if (NostalgiaEngine.detect(result.userText) != null)
                                    "Aaj achanak kaunsa pal yaad aa gaya?\n${result.userText}"
                                else result.userText

                            memoryRepository.fetchRecentMemories { memories ->
                                scope.launch {
                                    val finalPrompt =
                                        memoryManager.enrichPrompt(prompt, memories)

                                    val reply = aiService.reply(finalPrompt)
                                    responseText = reply
                                    onSaveDailySummary(reply)
                                    isSpeaking = true

                                    if (memoryManager.shouldSave(result.userText)) {
                                        memoryRepository.saveMemory(result.userText)
                                    }

                                    tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "AI")
                                }
                            }
                        }
                    }
                }
            },
            onListeningStart = {
                isListening = true
                isSpeaking = false
            },
            onListeningStop = {
                isListening = false
                rmsLevel = 0f
            },
            onRmsLevel = null
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showRipple = true
            voiceHelper.startListening()
        }
    }

    fun startListening() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showRipple = true
            voiceHelper.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // 🖥️ UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Image(
            painter = painterResource(id = R.drawable.sneh_saathi_logo),
            contentDescription = "Sneh Saathi Logo",
            modifier = Modifier.size(260.dp)
        )

        // Response Card with Border and Replay Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFD7CCC8)), // Subtle brown border
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = responseText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 28.sp, // Increased for Devanagari readability
                        letterSpacing = 0.5.sp
                    ),
                    color = textColor
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Replay Button
                IconButton(
                    onClick = {
                        if (responseText.isNotEmpty()) {
                            isSpeaking = true
                            tts.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, "REPLAY")
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Replay",
                        tint = Color(0xFF00897B) // High contrast Teal
                    )
                }
            }
        }

        // 🎤 MIC — DIFFERENT GLOW FOR LISTENING vs AI SPEAKING
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(170.dp)
        ) {
            // Ripple effect when mic tapped
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = rippleScale
                        scaleY = rippleScale
                    }
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(100.dp)
                    )
            )

            // Glow + pulse
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = pulseScale * (if (isListening) volumeScale else 1f)
                        scaleY = pulseScale * (if (isListening) volumeScale else 1f)
                    }
                    .background(
                        color = glowColor,
                        shape = RoundedCornerShape(100.dp)
                    )
            )

            // 👂 Ear icon when listening, 🎤 Mic when speaking
            Icon(
                imageVector = when {
                    isListening -> Icons.Filled.Hearing
                    else -> Icons.Filled.Mic
                },
                contentDescription = "Voice state",
                tint = Color(0xFF00897B), // High contrast Teal
                modifier = Modifier.size(56.dp)
            )
        }

        // Action Button
        Button(
            onClick = { startListening() },
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp), // Taller for easier tapping
            shape = RoundedCornerShape(35.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE64A19)) // High contrast Burnt Orange
        ) {
            Text(
                text = if (isListening) "Sun rahi hoon…" else "Boliyé",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
