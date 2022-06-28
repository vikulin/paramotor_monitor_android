package org.rivchain.paramotor_monitor

import android.bluetooth.BluetoothDevice

class BluetoothDeviceData {

    var isConnected: Boolean = false

    var mBluetoothDevice: BluetoothDevice? = null

    var deviceData: DeviceData = DeviceData()

    var availableSensorId = setOf<Int>()

    companion object {
        var sensorProfile = listOf(
            arrayOf("459e", "RPM", "RPM", "rotations per minute", 1),//RPM
            arrayOf("2a1c", "°C", "tc_256x256", "temperature in °C", 1),//tc
            arrayOf("8fcc", "%", "%", "fuel level in %", 1),//fl
            arrayOf("ed11", "h", "h", "engine hours", 3600)//eh
        )
    }
}