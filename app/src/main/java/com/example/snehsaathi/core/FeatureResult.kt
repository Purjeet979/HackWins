package com.example.snehsaathi.core

sealed class FeatureResult {

    data class Block(
        val message: String
    ) : FeatureResult()

    data class Pass(
        val userText: String
    ) : FeatureResult()
}
