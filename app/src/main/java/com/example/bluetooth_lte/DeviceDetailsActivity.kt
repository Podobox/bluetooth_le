package com.example.bluetooth_lte

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DeviceDetailsActivity() : AppCompatActivity() {

    private lateinit var bleManager: MyBleManager
    private lateinit var buttonValueTextView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_details)

        val deviceName = intent.getStringExtra("deviceName") ?: "Inconnu"
        val deviceAddress = intent.getStringExtra("deviceAddress") ?: "Inconnu"

        findViewById<TextView>(R.id.deviceNameTextView).text = "Nom : $deviceName"
        findViewById<TextView>(R.id.deviceAddressTextView).text = "Adresse MAC : $deviceAddress"
        buttonValueTextView = findViewById(R.id.buttonValueTextView)


        bleManager = BleManagerSingleton.getInstance(this)

        findViewById<Button>(R.id.readButtonValue).setOnClickListener {
            readButtonValue()
        }

        findViewById<Button>(R.id.disconnectButton).setOnClickListener {
            disconnectDevice()
        }

        listenForButtonNotifications()

    }

    private fun readButtonValue() {
        bleManager.readButtonValue { value ->
            runOnUiThread {
                buttonValueTextView.text = "État du bouton : ${if (value == "1") "ON" else "OFF"}"
            }
        }
    }

    private fun listenForButtonNotifications() {
        lifecycleScope.launch {
            for (value in bleManager.getNotificationChannel()) {
                runOnUiThread {
                    buttonValueTextView.text = "État du bouton : ${if (value == "1") "Enfoncé" else "Relâché"}"
                }
            }
        }
    }

    private fun disconnectDevice() {
        bleManager.disconnect().enqueue()
        Toast.makeText(this, "Déconnecté", Toast.LENGTH_SHORT).show()
        finish()
    }

}