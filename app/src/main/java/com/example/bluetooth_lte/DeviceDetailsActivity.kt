package com.example.bluetooth_lte

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class DeviceDetailsActivity : AppCompatActivity() {

    private var bluetoothGatt: BluetoothGatt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_details)

        val deviceName = intent.getStringExtra("deviceName") ?: "Inconnu"
        val deviceAddress = intent.getStringExtra("deviceAddress") ?: "Inconnu"

        findViewById<TextView>(R.id.deviceNameTextView).text = "Nom : $deviceName"
        findViewById<TextView>(R.id.deviceAddressTextView).text = "Adresse MAC : $deviceAddress"

        bluetoothGatt = BluetoothManagerSingleton.bluetoothGatt

        findViewById<Button>(R.id.disconnectButton).setOnClickListener {
            disconnectDevice()
        }

    }

    private fun disconnectDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_CONNECT_PERMISSION
                )
                return
            }
        }

        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null

        Toast.makeText(this, "Déconnecté", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT_PERMISSION = 2
    }


}
