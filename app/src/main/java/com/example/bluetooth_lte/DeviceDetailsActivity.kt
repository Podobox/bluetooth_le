package com.example.bluetooth_lte

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DeviceDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_details)

        val deviceName = intent.getStringExtra("deviceName") ?: "Inconnu"
        val deviceAddress = intent.getStringExtra("deviceAddress") ?: "Inconnu"

        findViewById<TextView>(R.id.deviceNameTextView).text = "Nom : $deviceName"
        findViewById<TextView>(R.id.deviceAddressTextView).text = "Adresse MAC : $deviceAddress"


        findViewById<Button>(R.id.disconnectButton).setOnClickListener {
            disconnectDevice()
        }

    }

    private fun disconnectDevice() {
        TODO("Not yet implemented")
    }
}
