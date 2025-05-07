package com.github.callmeqan.jarvismobile

import com.github.callmeqan.jarvismobile.databinding.FragmentHomeBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment() {

    private lateinit var speechRecognizerHelper: SpeechRecognizerHelper
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private val messages = mutableListOf<String>()  // List to hold chat messages

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Set up the RecyclerView
        recyclerView = binding.root.findViewById(R.id.chat_log)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize the adapter
        chatAdapter = ChatAdapter(messages)
        recyclerView.adapter = chatAdapter

        // Mic button
        val micButton: Button = binding.root.findViewById(R.id.mic_button)

        // Initialize the SpeechRecognizer
        speechRecognizerHelper = SpeechRecognizerHelper(this) { recognizedText ->
            // Handle the recognized text (e.g., display in chat log)
            handleSpeechRecognitionResult(recognizedText)
        }

        // Start listening on mic button click
        micButton.setOnClickListener {
            speechRecognizerHelper.startListening()
        }

        return binding.root
    }

    private fun handleSpeechRecognitionResult(result: String) {
        // Add recognized text to the RecyclerView
        messages.add("You said: $result")  // Add the new message to the list
        chatAdapter.addMessage("You said: $result")  // Update the RecyclerView with the new message
        recyclerView.scrollToPosition(messages.size - 1)  // Scroll to the latest message
    }


    override fun onResume() {
        super.onResume()
        println("HomeFragment onResume called") // Add log
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizerHelper.destroy()
    }
}
