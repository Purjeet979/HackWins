package com.example.snehsaathi.features.nostalgia

import com.example.snehsaathi.core.FeatureFlags

data class NostalgiaContext(
    val mood: String
)

object NostalgiaEngine {

    fun detect(text: String): NostalgiaContext? {
        if (!FeatureFlags.NOSTALGIA) return null

        val lower = text.lowercase()

        val nostalgiaKeywords = listOf(
            "pehle",
            "bachpan",
            "purane din",
            "yaad hai",
            "jawani",
            "school ke din"
        )

        return if (nostalgiaKeywords.any { lower.contains(it) }) {
            NostalgiaContext(mood = "nostalgic")
        } else {
            null
        }
    }
}
