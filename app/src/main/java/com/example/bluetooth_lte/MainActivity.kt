package com.example.bluetooth_lte

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val handler = Handler(Looper.getMainLooper())
    private val scanResults = mutableListOf<ScanResult>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScanResultAdapter
    private val seenDevices = mutableSetOf<String>()
    private var scanning = false
    private val scanPeriod: Long = 10000 // 10 secondes
    private var bluetoothGatt: BluetoothGatt? = null // Gestion de la connexion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        recyclerView = findViewById(R.id.recyclerView)
        adapter = ScanResultAdapter(scanResults) { selectedDevice ->
            connectToDevice(selectedDevice)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.scanButton).setOnClickListener {
            checkPermissionsAndScan()
        }
    }


    @SuppressLint("MissingPermission")
    private fun connectToDevice(scanResult: ScanResult) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_CONNECT_PERMISSION
                )
                return
            }
        }

        Toast.makeText(this, "Connexion à ${scanResult.device.address}...", Toast.LENGTH_SHORT)
            .show()

        bluetoothGatt =
            scanResult.device.connectGatt(this, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Connexion réussie !",
                                Toast.LENGTH_SHORT
                            ).show()
                            gatt.discoverServices() // Découverte des services GATT
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Déconnecté", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        runOnUiThread {
                            val intent =
                                Intent(this@MainActivity, DeviceDetailsActivity::class.java)
                            intent.putExtra("deviceName", gatt.device.name ?: "Inconnu")
                            intent.putExtra("deviceAddress", gatt.device.address)
                            startActivity(intent)
                        }
                    }
                }
            })
    }

    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT_PERMISSION = 2
    }


    private fun checkPermissionsAndScan() {
        val permissions = mutableListOf<String>()

        // Permissions pour API 29+
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) { // Android 12+
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
        } else {
            startScan()
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceAddress = result.device.address

            if (!seenDevices.contains(deviceAddress)) {
                seenDevices.add(deviceAddress)
                scanResults.add(result)

                // tri par rssi
                scanResults.sortWith(compareByDescending<ScanResult> {
                    val context = recyclerView.context
                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true // Pas nécessaire pour les versions < Android 12
                    }

                    if (hasPermission) {
                        it.device.name != null && it.device.name.isNotBlank()
                    } else {
                        false
                    }
                }.thenByDescending { it.rssi })


                adapter.notifyDataSetChanged()
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (scanning) {
            Toast.makeText(this, "Scan en cours", Toast.LENGTH_SHORT).show()
        } else if (bluetoothAdapter.isEnabled) {
            scanResults.clear()
            seenDevices.clear()
            adapter.notifyDataSetChanged()
            handler.postDelayed({ stopScan() }, scanPeriod)
            scanning = true
            bluetoothLeScanner.startScan(null, ScanSettings.Builder().build(), leScanCallback)
            Toast.makeText(this, "Scan en cours...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth désactivé", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (scanning) {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
            Toast.makeText(this, "Scan terminé", Toast.LENGTH_SHORT).show()
        }
    }
}
