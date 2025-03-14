package com.example.bluetooth_lte

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DeviceDetailsActivity() : AppCompatActivity() {

    private lateinit var bleManager: MyBleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_details)

        val deviceName = intent.getStringExtra("deviceName") ?: "Inconnu"
        val deviceAddress = intent.getStringExtra("deviceAddress") ?: "Inconnu"

        findViewById<TextView>(R.id.deviceNameTextView).text = "Nom : $deviceName"
        findViewById<TextView>(R.id.deviceAddressTextView).text = "Adresse MAC : $deviceAddress"

        bleManager = MyBleManager(this)

        /*bleManager.readModelNumber { modelNumber ->
            runOnUiThread {
                findViewById<TextView>(R.id.modelNumberTextView).text = "Modèle : $modelNumber"
            }
        }*/


        findViewById<Button>(R.id.disconnectButton).setOnClickListener {
            disconnectDevice()
        }

    }

    private fun disconnectDevice() {
        bleManager.disconnect().enqueue()
        Toast.makeText(this, "Déconnecté", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT_PERMISSION = 2
    }


}