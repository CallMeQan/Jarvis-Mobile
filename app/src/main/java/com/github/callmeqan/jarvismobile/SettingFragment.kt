package com.github.callmeqan.jarvismobile

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.callmeqan.jarvismobile.databinding.FragmentSettingBinding
import com.github.callmeqan.jarvismobile.BluetoothConnectionManager
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread

class SettingFragment : Fragment() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var binding: FragmentSettingBinding
    private val devices = mutableListOf<BluetoothDevice>()
    private lateinit var deviceAdapter: BluetoothDeviceAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var isReceiverRegistered = false

    // Classic Bluetooth SPP UUID
    private val sppUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionRequestCode = 101

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && !devices.contains(device)) {
                    devices.add(device)
                    deviceAdapter.notifyItemInserted(devices.size - 1)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), "Bluetooth not supported", Toast.LENGTH_LONG).show()
            return binding.root
        }

        deviceAdapter = BluetoothDeviceAdapter(devices) { device ->
            connectToClassicDevice(device)
        }

        binding.deviceList.layoutManager = LinearLayoutManager(requireContext())
        binding.deviceList.adapter = deviceAdapter

        checkPermissions()

        return binding.root
    }

    private fun checkPermissions() {
        val toRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), toRequest.toTypedArray(), permissionRequestCode)
        } else {
            startDiscovery()
        }
    }

    private fun startDiscovery() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
            return
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireContext().registerReceiver(bluetoothReceiver, filter)
        isReceiverRegistered = true

        bluetoothAdapter.startDiscovery()
    }

    private fun connectToClassicDevice(device: BluetoothDevice) {
        bluetoothAdapter.cancelDiscovery()

        // Save the selected device address in SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("paired_device", device.address).apply()

        thread {
            try {
                val socket = device.createRfcommSocketToServiceRecord(sppUUID)
                bluetoothSocket?.close() // close any existing socket
                bluetoothSocket = socket
                BluetoothConnectionManager.bluetoothSocket = socket // Share the socket
                socket.connect()

                Handler(Looper.getMainLooper()).post {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                    }
                }

                // Optional: Read or write to the stream
                // val output = socket.outputStream
                // output.write("Hello ESP32".toByteArray())

            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to connect: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                bluetoothSocket?.close()
                bluetoothSocket = null
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionRequestCode && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startDiscovery()
        } else {
            Toast.makeText(requireContext(), "Bluetooth permissions denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Do not close the BluetoothSocket here to maintain the connection across fragments
        bluetoothSocket = null

        if (isReceiverRegistered) {
            requireContext().unregisterReceiver(bluetoothReceiver)
            isReceiverRegistered = false
        }
    }
}
