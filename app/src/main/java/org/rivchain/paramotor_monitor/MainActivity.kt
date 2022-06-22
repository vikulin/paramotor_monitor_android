package org.rivchain.paramotor_monitor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.rivchain.paramotor_monitor.R
import org.rivchain.paramotor_monitor.db.Database

class MainActivity : AppCompatActivity(), OnBluetoothDeviceClickedListener {
    private val REQUEST_PERMISSION_ACCESS_FINE_LOCATION = 1
    private var mConnectionState = BluetoothLeService.ACTION_GATT_DISCONNECTED
    private var swipeRefresh: SwipeRefreshLayout? = null
    private var recyclerView: RecyclerView? = null
    private var mBluetoothDeviceAdapter: BluetoothDeviceAdapter? = null
    private var mBluetoothDeviceList: MutableList<BluetoothDeviceData> = ArrayList()
    private val mBluetoothScanCallBack = MyBluetoothScanCallBack()
    private var mHandler: Handler? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private lateinit var mDevice: BluetoothDeviceData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        initData()
        initService()
    }

    override fun onResume() {
        super.onResume()
        initReceiver()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions()
            return
        }
        scanLeDevice(true)
    }

    public override fun onPause() {
        super.onPause()
        Log.i("MainActivity", "unregisterReceiver()")
        unregisterReceiver(mGattUpdateReceiver)
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_COARSE_LOCATION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_COARSE_LOCATION -> if (grantResults.size > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this@MainActivity, "Permission Granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initView() {
        recyclerView = findViewById(R.id.recycler_view)
        swipeRefresh = findViewById(R.id.swipe_refresh)
    }

    private fun initService() {
        Log.i("MainActivity", "initService()")
        if (mBluetoothLeService == null) {
            val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
        }
    }

    private fun initData() {
        mHandler = Handler()
        val layoutManager = GridLayoutManager(this, 1)
        recyclerView!!.layoutManager = layoutManager
        mBluetoothDeviceAdapter = BluetoothDeviceAdapter(mBluetoothDeviceList, this)
        recyclerView!!.setAdapter(mBluetoothDeviceAdapter)
        swipeRefresh!!.setOnRefreshListener {
            //scan result does not return connected devices. save connected in list
            var connectedDeviceList = mBluetoothDeviceList.filter { key: BluetoothDeviceData -> key.isConnected } as MutableList<BluetoothDeviceData>
            mBluetoothDeviceList.clear()
            mBluetoothDeviceList.addAll(connectedDeviceList)
            scanLeDevice(true)
        }
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Log.e("MainActivity", "Unable to initialize Bluetooth")
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            mHandler!!.postDelayed({
                swipeRefresh!!.isRefreshing = false
                BluetoothScan.stopScan()
            }, SCAN_PERIOD)
            swipeRefresh!!.isRefreshing = true
            BluetoothScan.startScan(true, mBluetoothScanCallBack)
        } else {
            swipeRefresh!!.isRefreshing = false
            BluetoothScan.stopScan()
        }
    }

    private fun initReceiver() {
        Log.i("MainActivity", "initReceiver()")
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        registerReceiver(mGattUpdateReceiver, intentFilter)
    }

    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED == action) {
                Log.i("MainActivity", "ACTION_GATT_CONNECTED!!!")
                showMsg("Connected device ..")
                mConnectionState = BluetoothLeService.ACTION_GATT_CONNECTED
                swipeRefresh!!.isRefreshing = false
                //inputMessage()
                setStatusConnected(mDevice, true)
                mBluetoothDeviceAdapter?.notifyDataSetChanged()
                var db = Database.load(this@MainActivity)
                mDevice.mBluetoothDevice?.let {
                    db.addDeviceInfo(it)
                    Database.store(db, this@MainActivity)
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
                Log.i("MainActivity", "ACTION_GATT_DISCONNECTED!!!")
                showMsg("disconnected")
                mConnectionState = BluetoothLeService.ACTION_GATT_DISCONNECTED
                swipeRefresh!!.isRefreshing = false
                setStatusConnected(mDevice, false)
                mBluetoothDeviceAdapter?.notifyDataSetChanged()
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                mBluetoothLeService!!.supportedGattServices
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
                val data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)
                //showMsg("Got string : " + data?.let { String(it) })
                if (data != null && data.size > 0) {
                    val stringBuilder = StringBuilder(data.size)
                    for (byteChar in data) {
                        val b = Char(byteChar.toUShort())
                        stringBuilder.append(b)
                    }
                    Log.i("MainActivity", "Get string : $stringBuilder")
                    setData(mDevice, stringBuilder.toString())
                    mBluetoothDeviceAdapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun inputMessage() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Message to send")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton(
            "OK"
        ) { dialog, which ->
            val text = input.text.toString()
            btSendBytes(text.toByteArray())
        }
        builder.show()
    }

    fun btSendBytes(data: ByteArray?) {
        if (mBluetoothLeService != null && mConnectionState == BluetoothLeService.ACTION_GATT_CONNECTED) {
            mBluetoothLeService!!.writeCharacteristic(data)
        }
    }

    private inner class MyBluetoothScanCallBack : BluetoothScan.BluetoothScanCallBack {

        override fun onLeScanInitFailure(failureCode: Int) {
            Log.i("MainActivity", "onLeScanInitFailure()")
            when (failureCode) {
                BluetoothScan.SCAN_FEATURE_ERROR -> showMsg("scan_feature_error")
                BluetoothScan.SCAN_ADAPTER_ERROR -> showMsg("scan_adapter_error")
                else -> showMsg("unKnow_error")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onLeScanInitSuccess(successCode: Int) {
            Log.i("MainActivity", "onLeScanInitSuccess()")
            when (successCode) {
                BluetoothScan.SCAN_BEGIN_SCAN -> Log.i(
                    "MainActivity",
                    "successCode : $successCode"
                )
                BluetoothScan.SCAN_NEED_ENADLE -> {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                }
                BluetoothScan.AUTO_ENABLE_FAILURE -> showMsg("auto_enable_bluetooth_error")
                else -> showMsg("unKnow_error")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onLeScanResult(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
            //CC:50:E3:B6:94:36
            if (!contains(device)) {
                var bluetoothDeviceData = BluetoothDeviceData()
                bluetoothDeviceData.mBluetoothDevice = device
                mBluetoothDeviceList.add(bluetoothDeviceData)
                mBluetoothDeviceAdapter?.notifyDataSetChanged()
            }
        }
    }

    fun contains(device: BluetoothDevice?): Boolean {
        for (bluetoothDeviceData in mBluetoothDeviceList) {
            if (bluetoothDeviceData.mBluetoothDevice!!.equals(device)) {
                return true
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun setStatusConnected(mDevice: BluetoothDeviceData, status: Boolean): Boolean {
        for (bluetoothDeviceData in mBluetoothDeviceList) {
            if (bluetoothDeviceData.mBluetoothDevice!!.address.equals(mDevice.mBluetoothDevice!!.address)) {
                bluetoothDeviceData.isConnected = status
                return true
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun setData(mDevice: BluetoothDeviceData, data: String): Boolean {
        for (bluetoothDeviceData in mBluetoothDeviceList) {
            if (bluetoothDeviceData.mBluetoothDevice!!.address.equals(mDevice.mBluetoothDevice!!.address)) {
                var newData = DeviceData()
                newData.data = data
                bluetoothDeviceData.deviceData = newData
                return true
            }
        }
        return false
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                showMsg("enable_bluetooth_error")
                return
            } else if (resultCode == RESULT_OK) {
                mBluetoothDeviceList.clear()
                scanLeDevice(true)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val REQUEST_CONNECT = 1
        private const val PERMISSION_REQUEST_BLUETOOTH = 0
        private const val PERMISSION_REQUEST_COARSE_LOCATION = 2
        const val EXTRAS_DEVICE_NAME = "extras_device_name"
        const val EXTRAS_DEVICE_ADDRESS = "extras_device_address"
        private const val REQUEST_ENABLE_BT = 1
        private const val SCAN_PERIOD = (1000 * 3).toLong()
        var toast: Toast? = null
        fun showMsg(msg: String?) {
            try {
                if (toast == null) {
                    toast = Toast.makeText(MainApplication.context(), msg, Toast.LENGTH_SHORT)
                } else {
                    toast!!.setText(msg)
                }
                toast!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onBluetoothDeviceClicked(bluetoothDeviceData: BluetoothDeviceData) {
        mDevice = bluetoothDeviceData
        val mDeviceName = mDevice.mBluetoothDevice!!.name
        val mDeviceAddress = mDevice.mBluetoothDevice!!.address
        Log.i("MainActivity", "connecting device: $mDeviceName($mDeviceAddress)")
        if (mBluetoothLeService != null) {
            if (!bluetoothDeviceData.isConnected) {
                mBluetoothLeService!!.connect(mDeviceAddress)
                showMsg("Attempt to connect device : $mDeviceName")
                mConnectionState = BluetoothLeService.ACTION_GATT_CONNECTING
                swipeRefresh!!.isRefreshing = true
            } else {
                mBluetoothLeService!!.disconnect()
                showMsg("Disconnecting device : $mDeviceName")
                mConnectionState = BluetoothLeService.ACTION_GATT_DISCONNECTED
                swipeRefresh!!.isRefreshing = true
            }
        }
    }
}