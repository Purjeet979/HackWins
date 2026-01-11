package com.example.snehsaathi.core

interface AIService {
    suspend fun reply(userText: String): String
}
