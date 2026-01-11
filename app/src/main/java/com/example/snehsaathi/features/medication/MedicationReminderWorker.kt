package com.example.snehsaathi.features.medication

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Locale

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun doWork(): Result {
        tts = TextToSpeech(applicationContext, this)
        return Result.success()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
            tts?.speak(
                "Dadi, dawa lene ka samay ho gaya hai. Main yahin hoon.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "MEDICATION"
            )
        }
    }
}
