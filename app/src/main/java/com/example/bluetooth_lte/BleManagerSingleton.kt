package com.example.bluetooth_lte

import android.annotation.SuppressLint
import android.content.Context

object BleManagerSingleton {
    @SuppressLint("StaticFieldLeak")
    private var bleManager: MyBleManager? = null

    fun getInstance(context: Context): MyBleManager {
        if (bleManager == null) {
            bleManager = MyBleManager(context.applicationContext)
        }
        return bleManager!!
    }
}
