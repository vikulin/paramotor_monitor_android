package org.rivchain.paramotor_monitor

import android.bluetooth.BluetoothDevice

class BluetoothDeviceData {

    var isConnected: Boolean = false

    var mBluetoothDevice: BluetoothDevice? = null

    var deviceData: DeviceData = DeviceData()

    var availableSensorId = setOf<Int>()

    companion object {
        var sensorProfile = listOf(
            arrayOf("459e", "RPM", "RPM", "rotations per minute"),//RPM
            arrayOf("2a1c", "°C", "tc_256x256", "temperature in °C"),//tc
            arrayOf("8fcc", "%", "%", "fuel level in %"),//fl
            arrayOf("ed11", "h", "h", "engine hours")//eh
        )
    }
}