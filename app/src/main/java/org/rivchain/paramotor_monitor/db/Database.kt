package org.rivchain.paramotor_monitor.db

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class Database {

    var settings: Settings = Settings()
    var deviceInfoList: ArrayList<DeviceInfo> = ArrayList()

    fun addDeviceInfo(deviceInfo: DeviceInfo) {
        val idx = findContact(deviceInfo.address)
        if (idx >= 0) {
            // contact exists - replace
            deviceInfoList[idx] = deviceInfo
        } else {
            deviceInfoList.add(deviceInfo)
        }
    }

    @SuppressLint("MissingPermission")
    fun addDeviceInfo(bluetoothDevice: BluetoothDevice) {
        addDeviceInfo(DeviceInfo(bluetoothDevice))
    }

    fun deleteDeviceInfo(address: String?) {
        val idx = findContact(address)
        if (idx >= 0) {
            deviceInfoList.removeAt(idx)
        }
    }

    private fun findContact(address: String?): Int {
        var i = 0
        while (i < deviceInfoList.size) {
            if (deviceInfoList[i].address.equals(address)) {
                return i
            }
            i += 1
        }
        return -1
    }

    companion object {

        var version = "1.0.0" // current version

        @Throws(JSONException::class)
        fun load(context: Context): Database {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            // read database
            val data = preferences.getString("db", null) ?: return Database()
            val obj = JSONObject(data)
            return fromJSON(obj)
        }

        @Throws(JSONException::class)
        fun store(db: Database, context: Context) {
            val obj = toJSON(db)
            // write database file
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.edit().putString("db", obj.toString()).apply()
        }

        @Throws(JSONException::class)
        fun toJSON(db: Database): JSONObject {
            val obj = JSONObject()
            obj.put("version", version)
            obj.put("settings", Settings.exportJSON(db.settings))
            val contacts = JSONArray()
            for (contact in db.deviceInfoList) {
                contacts.put(DeviceInfo.exportJSON(contact))
            }
            obj.put("devices", contacts)
            return obj
        }

        @Throws(JSONException::class)
        fun fromJSON(obj: JSONObject): Database {
            val db = Database()

            // import version
            version = obj.getString("version")

            // import contacts
            val array = obj.getJSONArray("devices")
            var i = 0
            while (i < array.length()) {
                db.deviceInfoList.add(
                    DeviceInfo.importJSON(array.getJSONObject(i))
                )
                i += 1
            }

            // import settings
            val settings = obj.getJSONObject("settings")
            db.settings = Settings.importJSON(settings)
            return db
        }
    }
}