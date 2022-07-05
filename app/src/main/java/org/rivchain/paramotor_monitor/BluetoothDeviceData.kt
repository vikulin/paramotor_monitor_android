package org.rivchain.paramotor_monitor

import android.bluetooth.BluetoothDevice

class BluetoothDeviceData {

    var isConnected: Boolean = false

    var mBluetoothDevice: BluetoothDevice? = null

    var deviceData: HashMap<Int, Any> = HashMap()

    var availableSensorId = setOf<Int>()

    companion object {
        var sensorProfile = listOf(
            //UUID code, data type, label, icon, description, nominator, error value
            arrayOf("459e", Int, "RPM", "RPM", "rotations per minute", 1),//RPM
            arrayOf("2a1c", Int, "°C", "tc_256x256", "temperature in °C", 1),//tc
            arrayOf("8fcc", Int, "%", "%", "fuel level in %", 1),//fl
            arrayOf("ed11", Int, "h", "h", "engine hours", 3600)//eh
        )
    }
}