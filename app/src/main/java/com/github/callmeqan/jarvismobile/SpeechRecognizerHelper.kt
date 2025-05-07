package com.github.callmeqan.jarvismobile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.util.Locale

class SpeechRecognizerHelper(
    private val fragment: Fragment,
    private val onResult: (String) -> Unit
) {
    private val recognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(fragment.requireContext())

    private val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    init {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(fragment.requireContext(), "Listening...", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    if (it.isNotEmpty()) {
                        onResult(it[0])
                    }
                }
            }

            override fun onError(error: Int) {
                Toast.makeText(fragment.requireContext(), "Error recognizing speech", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })
    }

    fun startListening() {
        recognizer.startListening(recognizerIntent)
    }

    fun destroy() {
        recognizer.destroy()
    }
}
