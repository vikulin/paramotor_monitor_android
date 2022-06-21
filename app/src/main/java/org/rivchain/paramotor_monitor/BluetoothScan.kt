package org.rivchain.paramotor_monitor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object BluetoothScan {
    const val SCAN_FEATURE_ERROR = 0x00
    const val SCAN_ADAPTER_ERROR = 0x01
    const val SCAN_NEED_ENADLE = 0x02
    const val SCAN_BEGIN_SCAN = 0x03
    const val AUTO_ENABLE_FAILURE = 0x04
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothScanCallBack: BluetoothScanCallBack? = null

    @SuppressLint("MissingPermission")
    fun startScan(autoEnable: Boolean, callBack: BluetoothScanCallBack?) {
        mBluetoothScanCallBack = callBack
        if (!isBluetoothSupport(autoEnable)) {
            return
        }
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter!!.startLeScan(mLeScanCallback)
        } else {
            Log.e("BluetoothScan", "mBluetoothAdapter is null.")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
        } else {
            Log.e("BluetoothScan", "mBluetoothAdapter is null.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun isBluetoothSupport(autoEnable: Boolean): Boolean {
        if (!MainApplication.context()!!.packageManager
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        ) {
            mBluetoothScanCallBack!!.onLeScanInitFailure(SCAN_FEATURE_ERROR)
            return false
        }
        val bluetoothManager =
            MainApplication.context()!!
                .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        return if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter!!.isEnabled) {
                if (autoEnable) {
                    if (mBluetoothAdapter!!.enable()) {
                        mBluetoothScanCallBack!!.onLeScanInitSuccess(SCAN_BEGIN_SCAN)
                        true
                    } else {
                        mBluetoothScanCallBack!!.onLeScanInitSuccess(AUTO_ENABLE_FAILURE)
                        false
                    }
                } else {
                    mBluetoothScanCallBack!!.onLeScanInitSuccess(SCAN_NEED_ENADLE)
                    false
                }
            } else {
                mBluetoothScanCallBack!!.onLeScanInitSuccess(SCAN_BEGIN_SCAN)
                true
            }
        } else {
            mBluetoothScanCallBack!!.onLeScanInitFailure(SCAN_ADAPTER_ERROR)
            false
        }
    }

    // Device scan callback.
    private val mLeScanCallback =
        LeScanCallback { device, rssi, scanRecord ->
            if (mBluetoothScanCallBack != null) {
                mBluetoothScanCallBack!!.onLeScanResult(device, rssi, scanRecord)
            } else {
                Log.e("BluetoothScan", "mBluetoothScanCallBack is null.")
            }
        }

    interface BluetoothScanCallBack {
        fun onLeScanInitFailure(failureCode: Int)
        fun onLeScanInitSuccess(successCode: Int)
        fun onLeScanResult(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?)
    }
}