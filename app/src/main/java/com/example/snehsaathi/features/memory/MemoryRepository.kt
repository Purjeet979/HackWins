package com.example.snehsaathi.features.memory

import com.google.firebase.firestore.FirebaseFirestore

class MemoryRepository {

    private val db = FirebaseFirestore.getInstance()

    fun saveMemory(text: String) {
        val memory = hashMapOf(
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("memories")
            .add(memory)
    }

    fun fetchRecentMemories(
        onResult: (List<String>) -> Unit
    ) {
        db.collection("memories")
            .orderBy("timestamp")
            .limitToLast(5)
            .get()
            .addOnSuccessListener { snapshot ->
                val memories =
                    snapshot.documents.mapNotNull {
                        it.getString("text")
                    }
                onResult(memories)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}
