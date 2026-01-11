package com.example.snehsaathi.features.memory

class MemoryManager(
    private val repository: MemoryRepository
) {

    fun shouldSave(text: String): Boolean {
        val keywords = listOf(
            "mera",
            "meri",
            "naam",
            "hai",
            "tha",
            "thi",
            "rehta",
            "rehti",
            "bimari",
            "problem"
        )

        val lower = text.lowercase()
        return keywords.any { lower.contains(it) }
    }

    fun enrichPrompt(
        userText: String,
        memories: List<String>
    ): String {
        if (memories.isEmpty()) return userText

        return """
        Known information about the user:
        ${memories.joinToString("\n")}

        Respond kindly and remember these facts.

        User says: $userText
        """.trimIndent()
    }
}
