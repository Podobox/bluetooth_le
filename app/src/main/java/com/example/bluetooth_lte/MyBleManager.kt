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

    private var modelNumberCharacteristic: BluetoothGattCharacteristic? = null
    private val defaultScope = CoroutineScope(Dispatchers.Main)

    companion object {
        // private val SERVICE_UUID: UUID = UUID.fromString("00001523-1212-efde-1523-785feabcd123")
        // private val CHARACTERISTIC_UUID: UUID = UUID.fromString("80323644-3537-4F0B-A53B-CF494ECEAAB3")
        private const val BLE_APP = "BLE_APP"
    }

    // Canal pour gérer les notifications reçues
    private val notificationChannel = Channel<String>()

    override fun getGattCallback(): BleManagerGattCallback {
        return MyGattCallback()
    }

    private inner class MyGattCallback : BleManagerGattCallback() {
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            // Afficher tous les services pour debugging
            Log.d(BLE_APP, "Services disponibles :")
            for (service in gatt.services) {
                Log.d(BLE_APP, "Service UUID : ${service.uuid}")
            }

            if (modelNumberCharacteristic == null) {
                Log.e(BLE_APP, "Erreur : Caractéristique non trouvée !")
                return true
            }

            val properties = modelNumberCharacteristic!!.properties
            val readSupport = properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
            val notifySupport = properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

            return readSupport && notifySupport
        }

        override fun initialize() {
            Log.d(BLE_APP, "Initialisation de la connexion BLE")

            setNotificationCallback(modelNumberCharacteristic).with { _, data ->
                if (data.value != null) {
                    val value = String(data.value!!, Charsets.UTF_8)
                    defaultScope.launch {
                        notificationChannel.send(value)
                    }
                }
            }

            beginAtomicRequestQueue()
                .add(enableNotifications(modelNumberCharacteristic)
                    .fail { _: BluetoothDevice, status: Int ->
                        Log.e(BLE_APP, "Erreur d'abonnement aux notifications : $status")
                        disconnect().enqueue()
                    }
                )
                .done {
                    Log.d(BLE_APP, "Notifications activées avec succès")
                }
                .enqueue()
        }

        override fun onServicesInvalidated() {
            modelNumberCharacteristic = null
        }
    }

    fun readModelNumber(callback: (String) -> Unit) {
        modelNumberCharacteristic?.let {
            readCharacteristic(it)
                .with { _, data -> callback(data.getStringValue(0) ?: "Inconnu") }
                .fail { _, status -> Log.e(BLE_APP, "Erreur de lecture : $status") }
                .enqueue()
        } ?: callback("Caractéristique non disponible")
    }

    fun enableNotifications() {
        if (modelNumberCharacteristic == null) {
            Log.e(BLE_APP, "Caractéristique de notification non disponible !")
            return
        }

        enableNotifications(modelNumberCharacteristic)
            .fail { _: BluetoothDevice, status: Int ->
                Log.e(BLE_APP, "Erreur d'abonnement aux notifications: $status")
            }
            .done {
                Log.d(BLE_APP, "Abonnement aux notifications réussi")
            }
            .enqueue()
    }
}
