package com.example.snehsaathi.features.scamshield

object ScamShield {

    fun isScam(text: String): Boolean {
        val lower = text.lowercase()
        return ScamKeywords.phrases.any { lower.contains(it) }
    }

    fun warningMessage(): String {
        return "Dadi, ruk jaiye. OTP ya number kisi ko mat batana. Yeh fraud ho sakta hai. Main yahin hoon."
    }
}
