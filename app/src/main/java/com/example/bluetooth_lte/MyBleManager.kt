package com.example.bluetooth_lte

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import no.nordicsemi.android.ble.BleManager
import java.util.UUID

class MyBleManager(context: Context) : BleManager(context) {

    private var buttonCharacteristic: BluetoothGattCharacteristic? = null
    private val defaultScope = CoroutineScope(Dispatchers.Main)
    private val notificationChannel = Channel<String>() // Canal pour gérer les notifications reçues

    companion object {
        private val SERVICE_UUID: UUID = UUID.fromString("00001523-1212-efde-1523-785feabcd123")
        private val CHARACTERISTIC_UUID: UUID = UUID.fromString("00001524-1212-efde-1523-785feabcd123")
        private const val BLE_APP = "BLE_APP"
    }

    fun getNotificationChannel(): Channel<String> {
        return notificationChannel
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return MyGattCallback()
    }

    private inner class MyGattCallback : BleManagerGattCallback() {
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            // Afficher tous les services pour debugging
            Log.d(BLE_APP, "Services disponibles :")
            for (service in gatt.services) {
                Log.d(BLE_APP, "Service UUID : ${service.uuid}")
                if (service.uuid == SERVICE_UUID) {
                    for (characteristic in service.characteristics) {
                        if (characteristic.uuid == CHARACTERISTIC_UUID) {
                            Log.d(BLE_APP, "Caractéristique UUID : ${characteristic.uuid}")
                        }
                    }
                    buttonCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                }
            }

            if (buttonCharacteristic == null) {
                Log.e(BLE_APP, "Erreur : Caractéristique non trouvée !")
                return false
            }

            val properties = buttonCharacteristic!!.properties
            val readSupport = properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
            val notifySupport = properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

            return readSupport && notifySupport
        }

        override fun initialize() {
            setNotificationCallback(buttonCharacteristic).with { _, data ->
                if (data.value != null) {
                    val value = String(data.value!!, Charsets.UTF_8)
                    defaultScope.launch {
                        notificationChannel.send(value)
                    }
                }
            }

            beginAtomicRequestQueue()
                .add(enableNotifications(buttonCharacteristic)
                    .fail { _: BluetoothDevice, status: Int ->
                        Log.e(BLE_APP, "Erreur notifications : $status")
                        disconnect().enqueue()
                    }
                )
                .done {
                    Log.d(BLE_APP, "Notifications activées avec succès")
                }
                .enqueue()
        }

        override fun onServicesInvalidated() {
            buttonCharacteristic = null
        }

    }


    fun readButtonValue(callback: (String) -> Unit) {
        if (!isConnected) {
            Log.e(BLE_APP, "Erreur : périphérique déconnecté")
            return
        }
        if (buttonCharacteristic == null) {
            Log.e("BLE_SERVICE", "Erreur lors de la lecture de la caractéristique")
            callback("Caractéristique non disponible")
            return
        }
        else if (!isReadSupported(buttonCharacteristic)){
            Log.e("BLE_SERVICE", "Lecture impossible")
            return
        }else {
            buttonCharacteristic.let {
                readCharacteristic(it)
                    .with { _, data ->
                        val rawBytes = data.value
                        if (rawBytes != null && rawBytes.isNotEmpty()) {
                            val intValue = rawBytes[0].toInt() // Lire le premier octet en entier
                            callback(intValue.toString())
                        } else {
                            callback("N/A")
                        }
                    }
                    .fail { _, status -> Log.e(BLE_APP, "Erreur de lecture : $status") }
                    .enqueue()
            }

        }
    }

    private fun isReadSupported(characteristic: BluetoothGattCharacteristic?): Boolean {
        return (characteristic!!.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0
    }
}
