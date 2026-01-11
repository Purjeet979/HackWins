package com.example.snehsaathi.features.memory

import com.google.firebase.firestore.FirebaseFirestore

object FirebaseLogger {

    private val db = FirebaseFirestore.getInstance()

    fun logConversation(text: String, type: String) {
        val data = hashMapOf(
            "text" to text,
            "type" to type, // USER / AI / SCAM
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("snehsaathi_logs")
            .add(data)
    }
}
