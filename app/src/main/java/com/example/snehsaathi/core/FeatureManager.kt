package com.example.snehsaathi.core

import com.example.snehsaathi.features.scamshield.ScamShield

class FeatureManager {

    suspend fun processUserText(text: String): FeatureResult {

        // 1️⃣ Scam Shield – STOP everything if scam
        if (FeatureFlags.SCAM_SHIELD && ScamShield.isScam(text)) {
            return FeatureResult.Block(
                ScamShield.warningMessage()
            )
        }

        // 2️⃣ No feature blocked → continue to AI
        return FeatureResult.Pass(text)
    }
}
