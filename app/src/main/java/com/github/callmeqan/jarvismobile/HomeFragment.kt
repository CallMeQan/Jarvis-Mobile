package com.github.callmeqan.jarvismobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.callmeqan.jarvismobile.databinding.FragmentHomeBinding
import java.util.Locale
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import java.util.UUID
import android.speech.SpeechRecognizer
import com.airbnb.lottie.LottieAnimationView
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import java.io.IOException
import kotlin.concurrent.thread
import com.github.callmeqan.jarvismobile.BluetoothConnectionManager

class HomeFragment : Fragment() {

    private lateinit var micButton: ImageButton
    private lateinit var sendButton: ImageButton
    private lateinit var commandInput: EditText
    private lateinit var chatLog: RecyclerView
    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var speechRecognizerHelper: SpeechRecognizerHelper
    private val messages = mutableListOf<String>() // List to store chat messages
    private var bluetoothSocket: BluetoothSocket? = null
    private val sppUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Ensure consistent scrolling behavior for both chat and voice logs
    private fun scrollToBottom() {
        chatLog.post {
            chatLog.scrollToPosition(messages.size - 1)
        }
    }

    private fun sendMessageToLog(message: String) {
        messages.add("You: $message")
        chatLog.adapter?.notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize UI elements
        micButton = view.findViewById(R.id.mic_button)
        sendButton = view.findViewById(R.id.send_button)
        commandInput = view.findViewById(R.id.command_input)
        chatLog = view.findViewById(R.id.chat_log)
        lottieAnimationView = view.findViewById(R.id.waveformAnimation) // Initialize LottieAnimationView

        // Ensure only the waveform visibility is toggled without affecting the chat log
        lottieAnimationView.visibility = View.INVISIBLE // Use INVISIBLE instead of GONE to avoid layout shifts

        micButton.setOnClickListener {
            if (checkAudioPermission()) {
                lottieAnimationView.visibility = View.VISIBLE // Show waveform when starting voice recognition
                lottieAnimationView.progress = 0f // Reset animation
                lottieAnimationView.playAnimation()
                speechRecognizerHelper.startListening()
            } else {
                requestAudioPermission()
            }
        }

        speechRecognizerHelper = SpeechRecognizerHelper(
            this,
            onVoiceCommand = { message ->
                sendMessageToLog(message)
                lottieAnimationView.pauseAnimation() // Stop animation after processing
                lottieAnimationView.visibility = View.INVISIBLE // Hide waveform after processing
                speechRecognizerHelper.stopListening() // Stop listening after processing
            },
            onRmsChanged = { rms ->
                lottieAnimationView.progress = (rms / 10).coerceIn(0f, 1f) // Update Lottie animation
            }
        )

        // Set up RecyclerView with a fixed size and visible logs
        chatLog.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true // Ensure new messages appear at the bottom
        }
        chatLog.setHasFixedSize(true)
        chatLog.adapter = ChatAdapter(messages)

        // Preload 5 logs at a time
        chatLog.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (messages.size > 5) {
                chatLog.scrollToPosition(messages.size - 5)
            }
        }

        // Handle send button click
        sendButton.setOnClickListener {
            val message = commandInput.text.toString()
            if (message.isNotBlank()) {
                sendMessageToLog(message) // Log the message in the chat
                sendMessageToESP32(message) // Send the message to ESP32
                commandInput.text.clear()
            } else {
                Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun connectToClassicDevice(deviceAddress: String) {
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        thread {
            try {
                val socket = device.createRfcommSocketToServiceRecord(sppUUID)
                bluetoothSocket?.close() // Close any existing socket
                bluetoothSocket = socket
                socket.connect()

                BluetoothConnectionManager.bluetoothSocket = socket // Update shared BluetoothSocket

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(requireContext(), "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(requireContext(), "Failed to connect: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                bluetoothSocket?.close()
                bluetoothSocket = null
            }
        }
    }

    private fun sendMessageToESP32(message: String) {
        if (BluetoothConnectionManager.bluetoothSocket == null || !BluetoothConnectionManager.bluetoothSocket!!.isConnected) {
            val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val deviceAddress = sharedPreferences.getString("paired_device", null)

            if (deviceAddress == null) {
                Toast.makeText(requireContext(), "No device paired. Please pair a device in Settings.", Toast.LENGTH_SHORT).show()
                return
            }

            connectToClassicDevice(deviceAddress)
        } else {
            bluetoothSocket = BluetoothConnectionManager.bluetoothSocket
        }

        thread {
            try {
                val outputStream = bluetoothSocket?.outputStream
                outputStream?.write(message.toByteArray())

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(requireContext(),
                }
            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(requireContext(), "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO_PERMISSION
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bluetoothGatt.isInitialized) {
            bluetoothGatt.close()
        }
        bluetoothSocket?.close()
        speechRecognizerHelper.destroy()
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}