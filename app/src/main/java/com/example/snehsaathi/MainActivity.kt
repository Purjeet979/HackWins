package com.example.snehsaathi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
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
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            AppUI(tts)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("hi", "IN")
        }
    }
}

@Composable
fun AppUI(tts: TextToSpeech) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    val chatClient = GroqClient()

    var responseText by remember { mutableStateOf("Press button and speak") }
    var isListening by remember { mutableStateOf(false) }

    val voiceHelper = remember {
        VoiceInputHelper(context) { spokenText ->

            // ✅ CORRECT: suspend function inside coroutine
            scope.launch {
                responseText = "Thinking..."
                val reply = chatClient.sendMessage(spokenText)
                responseText = reply
                tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    fun startListening() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            isListening = true
            voiceHelper.startListening()
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
            isListening = false
            startListening()
        }) {
            Text("Speak")
        }
    }
}
