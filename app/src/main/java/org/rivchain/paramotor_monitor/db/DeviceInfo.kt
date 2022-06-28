package org.rivchain.paramotor_monitor.db

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import org.json.JSONException
import org.json.JSONObject

class DeviceInfo {

    constructor() {
        name = null
        address = null
    }

    @SuppressLint("MissingPermission")
    constructor(bluetoothDevice: BluetoothDevice) {
        name = bluetoothDevice.name
        address = bluetoothDevice.address
    }

    var name: String? = null
    var address: String? = null

    companion object {

        @Throws(JSONException::class)
        fun exportJSON(deviceInfo: DeviceInfo): JSONObject {
            val `object` = JSONObject()
            `object`.put("name", deviceInfo.name)
            `object`.put("address", deviceInfo.address)
            return `object`
        }

        @Throws(Exception::class)
        fun importJSON(`object`: JSONObject): DeviceInfo {
            val deviceInfo = DeviceInfo()
            deviceInfo.name = `object`.getString("name")
            deviceInfo.address = `object`.getString("address")
            return deviceInfo
        }
    }
}