package com.example.snehsaathi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnehSaathiApp()
        }
    }
}

@Composable
fun SnehSaathiApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Create an instance of our Client
    val client = remember { GeminiLiveClient() }

    // State to track if we are currently talking
    var isConnected by remember { mutableStateOf(false) }

    // Permission Launcher (Asks user for Mic access)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                isConnected = true
                scope.launch { client.startSession() }
            } else {
                Toast.makeText(context, "Need Mic Permission to hear you!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // --- UI DESIGN ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)), // Light Grey Background
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Sneh Saathi",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A4A4A)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isConnected) "Suman is listening..." else "Tap to talk to Suman",
            fontSize = 18.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(50.dp))

        // THE BIG BUTTON
        Button(
            onClick = {
                if (isConnected) {
                    // Stop the call
                    client.disconnect()
                    isConnected = false
                } else {
                    // Start the call (Check permission first)
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        isConnected = true
                        scope.launch { client.startSession() }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            modifier = Modifier.size(200.dp), // Big size for seniors
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) Color(0xFFE57373) else Color(0xFF81C784) // Red for Stop, Green for Start
            )
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.Close else Icons.Default.Call,
                contentDescription = "Call",
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
        }

        if (isConnected) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Tip: Speak clearly in Hindi or English", color = Color.Gray)
        }
    }
}