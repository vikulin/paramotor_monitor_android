package org.rivchain.paramotor_monitor

import android.bluetooth.BluetoothDevice

class BluetoothDeviceData {

    var isConnected: Boolean = false

    var mBluetoothDevice: BluetoothDevice? = null

    var deviceData: DeviceData = DeviceData()

    var availableSensorId = setOf<Int>()

    companion object {
        var sensorId = linkedSetOf(
            "459e",//RPM
            "2a1c",//tc
            "8fcc"//fl
        )
        var label = linkedSetOf(
            "RPM",//RPM
            "°C",//tc
            "%"//fl
        )
        var descryption = linkedSetOf(
            "rotations per minute",//RPM
            "temperature in °C",//tc
            "fuel level in %"//fl
        )
    }
}