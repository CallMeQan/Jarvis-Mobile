package com.github.callmeqan.jarvismobile

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomnavigation.BottomNavigationView

import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.github.callmeqan.jarvismobile.databinding.ActivityMainBinding
import androidx.lifecycle.ViewModelProvider

import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var lottieAnimationView: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set content view first
        setContentView(R.layout.activity_main)

        enableEdgeToEdge()

        // Initialize LottieAnimationView
        lottieAnimationView = findViewById(R.id.waveformAnimation)

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                lottieAnimationView.progress = 0f // Reset animation progress
                lottieAnimationView.playAnimation()
            }

            override fun onBeginningOfSpeech() {
                lottieAnimationView.speed = 1.5f // Speed up animation
            }

            override fun onRmsChanged(rmsdB: Float) {
                lottieAnimationView.progress = (rmsdB / 10).coerceIn(0f, 1f) // Adjust animation based on voice amplitude
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                lottieAnimationView.pauseAnimation()
            }

            override fun onError(error: Int) {
                Log.e("SpeechRecognizer", "Error: $error") // Log error instead of showing a toast
                restartSpeechRecognizer(intent) // Restart SpeechRecognizer on error
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.i("SpeechRecognizer", "You said: ${matches?.get(0)}") // Log results instead of showing a toast
                restartSpeechRecognizer(intent) // Restart SpeechRecognizer after results
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // ViewCompat padding fix (optional but nice)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Hook up nav controller with bottom nav
        val navController = findNavController(R.id.nav_host_fragment)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            println("Navigating to: ${destination.label}")
        }
    }

    private fun restartSpeechRecognizer(intent: Intent) {
        speechRecognizer.stopListening()
        speechRecognizer.startListening(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}