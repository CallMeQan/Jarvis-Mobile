package com.github.callmeqan.jarvismobile

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.util.Locale

class SpeechRecognizerHelper(
    private val fragment: Fragment,
    private val onVoiceCommand: (String) -> Unit, // Callback for voice commands
    private val onRmsChanged: (Float) -> Unit // Callback for RMS updates
) {
    private val recognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(fragment.requireContext())
    private val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) // Default to system language
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false) // Disable partial results
    }

    private var lastRmsUpdateTime = 0L // Track the last update time
    private var isListening = false // Track if the recognizer is currently listening

    init {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.i("SpeechRecognizerHelper", "Listening...") // Replaced Toast with log message
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onVoiceCommandReceived(matches[0]) // Use the first result
                    Log.d("SpeechRecognizerHelper", "Final result: ${matches[0]}")
                } else {
                    onVoiceCommandReceived("No speech detected. Please try again.")
                    Log.d("SpeechRecognizerHelper", "No matches found.")
                }
                stopListening() // Stop listening after processing the results
            }

            override fun onError(error: Int) {
                isListening = false // Reset the listening state on error
                val errorMessage = getErrorText(error)
                Log.e("SpeechRecognizerHelper", "Error: $errorMessage (Code: $error)") // Replaced Toast with log message
            }

            override fun onRmsChanged(rmsdB: Float) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastRmsUpdateTime > 100) { // Throttle updates to 10 times per second
                    lastRmsUpdateTime = currentTime
                    val clampedRms = rmsdB.coerceIn(0f, 10f) // Clamp RMS value to a reasonable range
                    Log.d("SpeechRecognizerHelper", "RMS dB: $rmsdB, Clamped RMS: $clampedRms") // Log RMS values
                    onRmsChanged(clampedRms) // Call the lambda with the RMS value
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false // Reset the listening state
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
        })
    }

    fun startListening() {
        if (isListening) {
            Log.w("SpeechRecognizerHelper", "Recognizer is already listening. Ignoring start request.")
            return
        }
        isListening = true
        recognizer.startListening(recognizerIntent)
    }

    fun stopListening() {
        if (!isListening) {
            Log.w("SpeechRecognizerHelper", "Recognizer is not listening. Ignoring stop request.")
            return
        }
        isListening = false
        recognizer.stopListening()
    }

    fun destroy() {
        recognizer.destroy()
    }

    fun onRmsChanged(rms: Float) {
        onRmsChanged(rms) // Call the lambda with the RMS value
    }

    fun onVoiceCommandReceived(command: String) {
        onVoiceCommand(command) // Call the lambda with the voice command
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }
}