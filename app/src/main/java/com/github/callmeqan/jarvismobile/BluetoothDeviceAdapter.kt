package com.github.callmeqan.jarvismobile

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.recyclerview.widget.RecyclerView
import com.github.callmeqan.jarvismobile.R

class BluetoothDeviceAdapter(
    private val devices: List<BluetoothDevice>,
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bluetooth_device, parent, false)
        return DeviceViewHolder(view, onDeviceClick)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int = devices.size

    class DeviceViewHolder(itemView: View, private val onDeviceClick: (BluetoothDevice) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val deviceName: TextView = itemView.findViewById(R.id.device_name)
        private val deviceAddress: TextView = itemView.findViewById(R.id.device_address)

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        fun bind(device: BluetoothDevice) {
            deviceName.text = device.name ?: "Unknown Device"
            deviceAddress.text = device.address

            itemView.setOnClickListener {
                itemView.alpha = 0.5f // Add a fade effect to indicate selection
                onDeviceClick(device)
                itemView.postDelayed({ itemView.alpha = 1.0f }, 200) // Restore original appearance after a short delay
            }
        }
    }
}
