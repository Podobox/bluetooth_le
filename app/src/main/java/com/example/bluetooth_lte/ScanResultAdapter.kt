package com.example.bluetooth_lte

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ScanResultAdapter(private val scanResults: List<ScanResult>,
                        private val onDeviceClick: (ScanResult) -> Unit
) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.device_name)
        val deviceAddress: TextView = view.findViewById(R.id.device_address)
        val deviceRssi: TextView = view.findViewById(R.id.device_rssi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = scanResults[position]
        val context = holder.itemView.context
        val deviceAddress = result.device.address
        val rssi = "${result.rssi} dBm"

        val deviceName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                result.device.name ?: "Inconnu"
            } else {
                "Permission requise"
            }
        } else {
            result.device.name ?: "Inconnu"
        }

        holder.deviceName.text = deviceName
        holder.deviceAddress.text = deviceAddress
        holder.deviceRssi.text = rssi

        holder.itemView.setOnClickListener {
            onDeviceClick(result)
        }
    }

    override fun getItemCount(): Int = scanResults.size
}
