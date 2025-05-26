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

import androidx.lifecycle.ViewModelProvider

import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeFragment : Fragment() {

    private lateinit var micButton: ImageButton
    private lateinit var sendButton: ImageButton
    private lateinit var commandInput: EditText
    private lateinit var chatLog: RecyclerView
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

    private fun sendMessageToLog(message: String, role: String) {
        messages.add(role + ": $message")
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
                sendMessageToLog(message, role = "user")
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
                sendMessageToLog(message, role = "user") // Log the message in the chat
                sendMessageToESP32(message) // Send the message to ESP32
                sendMessage2Server(message = message, role = "user")
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
                    Toast.makeText(requireContext(),"Message sent successfully",Toast.LENGTH_SHORT).show()
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
        bluetoothSocket?.close()
        speechRecognizerHelper.destroy()
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

    private fun sendMessage2Server(message: String, role: String = "user") {
        // On below line we are creating a retrofit
        // Builder and passing our base url
        val retrofit = Retrofit.Builder()
            .baseUrl("https://fd57-2405-4802-a61a-8d0-2191-a0e4-4b6b-c899.ngrok-free.app/")

            // As we are sending data in json format so we have to add Gson converter factory
            .addConverterFactory(GsonConverterFactory.create())

            // At last we are building our retrofit builder.
            .build()

        // Below line is to create an instance for our retrofit api class.
        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

        // Passing data from our text fields to our modal class.
        val chatMessage = ChatMessage(message, role)

        // Calling a method to create a post and passing our modal class.
        Toast.makeText(requireContext(), "Data: " + chatMessage.message, Toast.LENGTH_SHORT).show()
        val call: Call<ChatMessage?>? = retrofitAPI.sendMessage2Server(chatMessage)

        // On below line we are executing our method.
        call!!.enqueue(object : Callback<ChatMessage?> {
            override fun onResponse(call: Call<ChatMessage?>?, response: Response<ChatMessage?>) {
                // This method is called when we get response from our api.
                Toast.makeText(requireContext(), "Message sent to API server", Toast.LENGTH_SHORT).show()

                // We are getting response from our body and passing it to our modal class.
                val response: ChatMessage? = response.body()

                // On below line we are getting our data from modal class and adding it to our string.
                val responseString = "Response Code : " + "201" + "\n" + "message : " +response!!.message + "\n" + "role : " + response!!.role
                sendMessageToLog(response!!.message, role = "assistant")

                // Below line we are setting our string to our text view.
                Toast.makeText(requireContext(), responseString, Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(call: Call<ChatMessage?>?, t: Throwable) {

                // Setting text to our text view when we get error response from API.
                Toast.makeText(requireContext(), "Error found : " + t.message, Toast.LENGTH_SHORT).show()
            }
        })
}}
