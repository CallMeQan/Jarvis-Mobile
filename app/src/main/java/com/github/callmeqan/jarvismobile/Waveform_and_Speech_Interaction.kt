package com.github.callmeqan.jarvismobile

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.github.callmeqan.jarvismobile.WaveformView // Add this import

class YourFragmentOrActivity : Fragment() {
    private lateinit var waveformView: WaveformView
    private lateinit var speechRecognizerHelper: SpeechRecognizerHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        waveformView = view.findViewById(R.id.waveformView) // Corrected ID
        speechRecognizerHelper = SpeechRecognizerHelper(
            this,
            onVoiceCommand = { result -> Log.d("YourFragmentOrActivity", "Speech result: $result") },
            onRmsChanged = { rms -> waveformView.updateAmplitude(rms) } // Update the waveform view with RMS changes
        )
    }

    override fun onStart() {
        super.onStart()
        // Removed automatic call to startListening()
    }

    override fun onStop() {
        super.onStop()
        speechRecognizerHelper.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizerHelper.destroy()
    }
}