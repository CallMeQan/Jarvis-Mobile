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

class HomeFragment : Fragment() {

    private lateinit var micButton: ImageButton
    private lateinit var sendButton: ImageButton
    private lateinit var commandInput: EditText
    private lateinit var chatLog: RecyclerView
    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var speechRecognizerHelper: SpeechRecognizerHelper
    private val messages = mutableListOf<String>() // List to store chat messages

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
                sendMessageToLog(message)
                commandInput.text.clear()
            }
        }

        return view
    }

    private fun sendMessageToESP32(message: String) {
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val deviceAddress = sharedPreferences.getString("paired_device", null)

        if (deviceAddress == null) {
            Toast.makeText(requireContext(), "No device paired. Please pair a device in Settings.", Toast.LENGTH_SHORT).show()
            return
        }

        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        bluetoothGatt = device.connectGatt(requireContext(), false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(UUID.fromString("YOUR_SERVICE_UUID"))
                    val characteristic = service.getCharacteristic(UUID.fromString("YOUR_CHARACTERISTIC_UUID"))
                    characteristic.value = message.toByteArray()
                    gatt.writeCharacteristic(characteristic)
                }
            }
        })
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
        speechRecognizerHelper.destroy()
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}