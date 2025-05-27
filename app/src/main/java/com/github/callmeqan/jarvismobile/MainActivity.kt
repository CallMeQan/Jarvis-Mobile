package com.github.callmeqan.jarvismobile

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisApp()
        }
    }
}

@Composable
fun JarvisApp() {
    // System bars edge-to-edge
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(color = MaterialTheme.colorScheme.background)
    }

    val navController = rememberNavController()
    val bottomNavItems = listOf("Home", "Settings") // replace with your destinations

    // Compose state for Lottie and SpeechRecognizer
    val context = LocalContext.current
    var lottieSpeed by remember { mutableFloatStateOf(1f) }
    var lottieIsPlaying by remember { mutableStateOf(false) }
    var saidText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Lottie composition
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("waveform.json"))
    val lottieAnimatable = animateLottieCompositionAsState(
        composition = composition,
        isPlaying = lottieIsPlaying,
        speed = lottieSpeed,
        iterations = LottieConstants.IterateForever
    )

    // SpeechRecognizer setup
    DisposableEffect(Unit) {
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                lottieIsPlaying = true
            }
            override fun onBeginningOfSpeech() {
                lottieSpeed = 1.5f
            }
            override fun onRmsChanged(rmsdB: Float) {
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                lottieIsPlaying = false
            }
            override fun onError(error: Int) {
                Log.e("SpeechRecognizer", "Error: $error")
                speechRecognizer.stopListening()
                speechRecognizer.startListening(intent)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                saidText = matches?.getOrNull(0)
                speechRecognizer.stopListening()
                speechRecognizer.startListening(intent)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(intent)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { label ->
                    NavigationBarItem(
                        selected = false, // You should wire selection state to navController
                        onClick = {
                            // navController.navigate(label)
                        },
                        label = { Text(label) },
                        icon = { /* Add icons if desired */ }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // Lottie Animation
            LottieAnimation(
                composition = composition,
                progress = { lottieAnimatable.progress },
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
            )

            // Optionally show recognized speech
            saidText?.let {
                Text(
                    text = "You said: $it",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                )
            }

            // Navigation host (replace with real destinations)
            NavHost(navController = navController, startDestination = "Home") {
                composable("Home") { /* HomeScreen() */ }
                composable("Settings") { /* SettingsScreen() */ }
            }
        }
    }
}