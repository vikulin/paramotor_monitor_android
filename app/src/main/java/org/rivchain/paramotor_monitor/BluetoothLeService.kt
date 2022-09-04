package org.rivchain.paramotor_monitor

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.*

class BluetoothLeService : Service() {
    var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private val mBinder: IBinder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onUnbind(intent: Intent): Boolean {
        return super.onUnbind(intent)
    }

    fun initialize(): Boolean {
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e("BluetoothLeService", "Unable to initialize BluetoothManager.")
                return false
            }
        }
        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            Log.e("BluetoothLeService", "Unable to obtain a BluetoothAdapter.")
            return false
        }
        Log.i("BluetoothLeService", "Initialize BluetoothLeService success!")
        return true
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            Log.e("BluetoothLeService", "BluetoothAdapter not initialized or unspecified address.")
            return false
        }
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress && mBluetoothGatt != null) {
            Log.w("BluetoothLeService", "Trying to use an existing mBluetoothGatt for connection.")
            return mBluetoothGatt!!.connect()
        }
        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.e("BluetoothLeService", "Device not found,Unable to connect.")
            return false
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        Log.w("BluetoothLeService", "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        /**
         * fix for BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED is never called in MainActivity.java
         * see https://stackoverflow.com/questions/25848764/onservicesdiscoveredbluetoothgatt-gatt-int-status-is-never-called
         */
        mBluetoothGatt?.connect()
        return true
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e("BluetoothLeService", "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.disconnect()
    }

    @SuppressLint("MissingPermission")
    fun close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt!!.close()
            mBluetoothGatt = null
        }
    }

    @SuppressLint("MissingPermission")
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                broadcastUpdate(ACTION_GATT_CONNECTED, gatt.device.address)
                Log.i("BluetoothLeService", "Connected to GATT server.")
                mBluetoothGatt!!.discoverServices()
                Log.i("BluetoothLeService", "Attempting to start service discovery:")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastUpdate(ACTION_GATT_DISCONNECTED, gatt.device.address)
                Log.w("BluetoothLeService", "Disconnected from GATT server.")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val gattServices = gatt.services
                var uuidServiceList = mutableListOf<String>()
                for (gattService in gattServices) {
                    val serviceUuid = gattService.uuid
                    Log.i("BluetoothLeService", "service: $serviceUuid")
                    uuidServiceList.add(serviceUuid.toString())
                }
                broadcastUpdate(
                    ACTION_GATT_SERVICES_DISCOVERED,
                    gatt.device.address,
                    uuidServiceList
                )
            } else {
                Log.e("BluetoothLeService", "onServicesDiscovered received : $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BluetoothLeService", "onCharacteristicRead()")
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, gatt.device.address)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.i("BluetoothLeService", "onCharacteristicChanged()")
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, gatt.device.address)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            Log.i("BluetoothLeService", "onCharacteristicWrite()")
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BluetoothLeService", "onMtuChanged() success. New MTU: $mtu")
            } else {
                Log.i("BluetoothLeService", "onMtuChanged() failed. MTU: $mtu")
            }
            super.onMtuChanged(gatt, mtu, status)
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, address: String?) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, address: String?, uuidService: List<String>?) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address)
        intent.putExtra(EXTRA_DEVICE_SERVICE_UUID, uuidService?.toTypedArray())
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(
        action: String,
        characteristic: BluetoothGattCharacteristic, address: String?
    ) {
        val intent = Intent(action)
        val data = characteristic.value
        intent.putExtra(EXTRA_DATA, data)
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address)
        sendBroadcast(intent)
    }

    @get:SuppressLint("MissingPermission")
    val supportedGattServices: List<BluetoothGattService>?
        get() {
            if (mBluetoothGatt == null) {
                return null
            }
            val gattServices = mBluetoothGatt!!.services
            for (gattService in gattServices) {
                val gattCharacteristics = gattService.characteristics
                for (gattCharacteristic in gattCharacteristics) {
                    val serviceUuid = gattService.uuid
                    val uuid = gattCharacteristic.uuid.toString()
                    Log.i("BluetoothLeService", "uuid : $uuid/service: $serviceUuid")
                    if (uuid.equals(UUID_NOTIFY.toString(), ignoreCase = true)) {
                        mNotifyCharacteristic = gattCharacteristic
                        mBluetoothGatt!!.setCharacteristicNotification(gattCharacteristic, true)
                        Log.i(
                            "BluetoothLeService",
                            "setCharacteristicNotification : $uuid"
                        )
                        val magic_uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        val descriptor = gattCharacteristic.getDescriptor(magic_uuid)
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        mBluetoothGatt!!.writeDescriptor(descriptor)
                    }
                }
            }
            return gattServices
        }

    @SuppressLint("MissingPermission")
    fun writeCharacteristic(data: ByteArray?) {
        mNotifyCharacteristic!!.value = data
        mBluetoothGatt!!.writeCharacteristic(mNotifyCharacteristic)
    }

    companion object {
        const val ACTION_GATT_CONNECTING = "action_gatt_connecting"
        const val ACTION_GATT_CONNECTED = "action_gatt_connected"
        const val ACTION_GATT_DISCONNECTED = "action_gatt_disconnected"
        const val ACTION_GATT_SERVICES_DISCOVERED = "action_gatt_services_discovered"
        const val ACTION_DATA_AVAILABLE = "action_data_available"
        const val EXTRA_DATA = "extra_data"
        const val EXTRA_DEVICE_ADDRESS = "extra_device_address"
        const val EXTRA_DEVICE_SERVICE_UUID = "extra_service_uuid"
        val UUID_NOTIFY = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    }
}