package com.github.callmeqan.jarvismobile

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.callmeqan.jarvismobile.databinding.FragmentSettingBinding

class SettingFragment : Fragment() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device?.name
                val deviceAddress = device?.address // MAC address of the device
                println("Found device: $deviceName, $deviceAddress")
            }
        }
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val requestPermissionCode = 1001

    private var isReceiverRegistered = false // Flag to check receiver registration status

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toTypedArray(), requestPermissionCode)
        } else {
            setupBluetooth() // If permissions are granted, proceed to setup Bluetooth
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentSettingBinding.inflate(inflater, container, false)

        // Check and request Bluetooth permissions
        checkAndRequestPermissions()

        return binding.root
    }

    private fun setupBluetooth() {
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireContext().registerReceiver(bluetoothReceiver, filter)

        bluetoothAdapter.startDiscovery()
        isReceiverRegistered = true // Set flag to true when receiver is registered
    }
    // Handle permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted, setup Bluetooth
                setupBluetooth()
            } else {
                // Permissions denied
                println("Bluetooth permissions denied")
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()

        // Only unregister the receiver if it's registered
        if (isReceiverRegistered) {
            requireContext().unregisterReceiver(bluetoothReceiver)
            isReceiverRegistered = false // Reset the flag
        }
    }


}
