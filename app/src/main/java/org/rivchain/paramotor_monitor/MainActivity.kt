package org.rivchain.paramotor_monitor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.rivchain.paramotor_monitor.R
import org.rivchain.paramotor_monitor.db.Database

class MainActivity : AppCompatActivity(), OnBluetoothDeviceClickedListener {

    private var mConnectionState = BluetoothLeService.ACTION_GATT_DISCONNECTED
    private var swipeRefresh: SwipeRefreshLayout? = null
    private var recyclerView: RecyclerView? = null
    private var mBluetoothDeviceAdapter: BluetoothDeviceAdapter? = null
    private var mBluetoothDeviceList: MutableList<BluetoothDeviceData> = ArrayList()
    private val mBluetoothScanCallBack = MyBluetoothScanCallBack()
    private var mHandler: Handler? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mOverlayService: OverlayService? = null
    private var gson = Gson()

    /*Overlay infrastructure*/
    private var service: Intent? = null
    private var foreground = false
    private val statusHandler = Handler(Looper.getMainLooper())

    private val statusChecker: Runnable = object : Runnable {
        override fun run() {
            statusHandler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        initData()
        initService()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("MissingPermission")
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        initData()
        //initService()
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val connectedDevices =
            bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)
        if (connectedDevices.size > 0) {
            for (cd in connectedDevices) {
                if (!contains(cd)) {
                    val bluetoothDeviceData = BluetoothDeviceData()
                    bluetoothDeviceData.mBluetoothDevice = cd
                    bluetoothDeviceData.isConnected = true
                    //bluetoothDeviceData.deviceData = DeviceData()
                    mBluetoothDeviceList.add(bluetoothDeviceData)
                }
            }
            mBluetoothDeviceAdapter!!.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        initReceiver()
        //Toast.makeText(this@MainActivity, "Permission Granted!", Toast.LENGTH_SHORT).show()
        //check overlay permissions

        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !Settings.canDrawOverlays(this)
            } else {
                false
            }
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION)
        } else {
            //changeStatus(true)
            //finish()
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            this.requestPermissions()
            return
        }
        scanLeDevice(true)
        mOverlayService?.hide()
    }

    public override fun onPause() {
        super.onPause()
        Log.i("MainActivity", "unregisterReceiver()")
        unregisterReceiver(mGattUpdateReceiver)
        mOverlayService?.show()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    PERMISSIONS_REQUEST
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
            PERMISSIONS_REQUEST -> if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                //Toast.makeText(this@MainActivity, "Permission Granted!", Toast.LENGTH_SHORT).show()
                //check overlay permissions

                //Check if the application has draw over other apps permission or not?
                //This permission is by default available for API<23. But for API > 23
                //you have to ask for the permission in runtime.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION)
                } else {
                    changeStatus(true)
                    //finish()
                }
            } else {
                Toast.makeText(this@MainActivity, "Permission Denied!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun changeStatus(status: Boolean) {
        if (status) {
            //startForegroundService(service)
            startService(service)
        } else {
            stopService(service)
        }
    }

    private fun initView() {
        recyclerView = findViewById(R.id.recycler_view)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        hideActionBar(recyclerView)
    }

    fun hideActionBar(view: View?) {
        if (view != null) {
            val params: WindowManager.LayoutParams = window.attributes
            params.flags = params.flags or (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
            view.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_FULLSCREEN
            window.attributes = params
        }
    }

    private fun initService() {
        Log.i("MainActivity", "initService()")
        if (mBluetoothLeService == null) {
            val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
            bindService(gattServiceIntent, mBtServiceConnection, BIND_AUTO_CREATE)
        }
        if (mOverlayService == null) {
            service = Intent(this, OverlayService::class.java)
            service!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            bindService(service, mOverlayServiceConnection, BIND_AUTO_CREATE)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initData() {
        mHandler = Handler(Looper.getMainLooper())
        val layoutManager = GridLayoutManager(this, 1)
        recyclerView!!.layoutManager = layoutManager
        mBluetoothDeviceAdapter = BluetoothDeviceAdapter(mBluetoothDeviceList, this)
        recyclerView!!.adapter = mBluetoothDeviceAdapter
        swipeRefresh!!.setOnRefreshListener {
            //scan result does not return connected devices. save connected in list
            var connectedDeviceList =
                mBluetoothDeviceList.filter { key: BluetoothDeviceData -> key.isConnected } as MutableList<BluetoothDeviceData>
            connectedDeviceList.sortBy { it.mBluetoothDevice?.name }
            mBluetoothDeviceList.clear()
            mBluetoothDeviceList.addAll(connectedDeviceList)
            scanLeDevice(true)
        }
    }

    private val mBtServiceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Log.e("MainActivity", "Unable to initialize Bluetooth")
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.i("MainActivity", "BtService disconnected")
            mBluetoothLeService = null
        }
    }

    private val mOverlayServiceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mOverlayService = (service as OverlayService.LocalBinder).service
            //mOverlayService!!.mBluetoothDeviceList = mBluetoothDeviceList
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mOverlayService = null
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

        override fun onReceive(c: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED == action) {
                val address = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_ADDRESS)
                Log.i("MainActivity", "ACTION_GATT_CONNECTED!!!")
                showMsg("Connected device $address")
                mConnectionState = BluetoothLeService.ACTION_GATT_CONNECTED
                swipeRefresh!!.isRefreshing = false
                //inputMessage()
                var d =
                    mBluetoothDeviceList.firstOrNull { it.mBluetoothDevice!!.address.equals(address) }
                if (d != null && address != null) {
                    setStatusConnected(address, true)
                    mBluetoothDeviceAdapter?.notifyDataSetChanged()
                    var db = Database.load(this@MainActivity)
                    d.mBluetoothDevice?.let {
                        db.addDeviceInfo(it)
                        Database.store(db, this@MainActivity)
                    }
                    mOverlayService?.addDevice(d)
                    mOverlayService?.notifyDataSetChanged()
                    //findViewById<View>(R.id.root).rootView.visibility = View.GONE
                    Thread.sleep(1000);
                    startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
                val address = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_ADDRESS)
                Log.i("MainActivity", "ACTION_GATT_DISCONNECTED!!!")
                showMsg("Disconnected device $address")
                mConnectionState = BluetoothLeService.ACTION_GATT_DISCONNECTED
                swipeRefresh!!.isRefreshing = false
                if (address != null) {
                    setStatusConnected(address, false)
                    mBluetoothDeviceAdapter?.notifyDataSetChanged()
                    mOverlayService?.notifyDataSetChanged()
                }
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                mBluetoothLeService!!.supportedGattServices
                val address = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_ADDRESS)
                val serviceUuid =
                    intent.getStringArrayExtra(BluetoothLeService.EXTRA_DEVICE_SERVICE_UUID)
                //update serviceUuid to available sensor list
                if (address != null && serviceUuid!!.isNotEmpty()) {
                    var sensors = LinkedHashSet<String>()
                    for (uuid in serviceUuid) {
                        val u = uuid.replace("-", "", ignoreCase = true)
                        var sensorIdList = u.chunked(4)
                        sensors.addAll(sensorIdList)
                    }
                    var availableSensorId = linkedSetOf<Int>()
                    for ((index, sensorId) in BluetoothDeviceData.sensorProfile.withIndex()) {
                        if (sensors.indexOf(sensorId[0]) >= 0) {
                            availableSensorId.add(index)
                        }
                    }
                    setSensorsId(address, availableSensorId)
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
                val data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)
                val address = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_ADDRESS)
                //showMsg("Got string : " + data?.let { String(it) })
                if (data != null && data.isNotEmpty() && address != null) {
                    val stringBuilder = StringBuilder(data.size)
                    for (byteChar in data) {
                        val b = Char(byteChar.toUShort())
                        stringBuilder.append(b)
                    }
                    Log.i("MainActivity", "Get string : $stringBuilder")
                    if (mBluetoothDeviceList.size > 0) {
                        try {
                            setData(address, stringBuilder.toString())
                            mBluetoothDeviceAdapter?.notifyDataSetChanged()
                            mOverlayService?.notifyDataSetChanged()
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                            println(stringBuilder.toString())
                        }
                    }
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
            if (!contains(device) && device?.name != null) {
                var bluetoothDeviceData = BluetoothDeviceData()
                bluetoothDeviceData.mBluetoothDevice = device
                mBluetoothDeviceList.add(bluetoothDeviceData)
                mBluetoothDeviceAdapter?.notifyDataSetChanged()
            }
        }
    }

    fun contains(device: BluetoothDevice?): Boolean {
        for (bluetoothDeviceData in mBluetoothDeviceList) {
            if (bluetoothDeviceData.mBluetoothDevice!!.address.equals(
                    device!!.address,
                    ignoreCase = true
                )
            ) {
                return true
            }
        }
        return false
    }

    fun setSensorsId(address: String, availableSensorId: Set<Int>) {
        var device = mBluetoothDeviceList.firstOrNull {
            it.mBluetoothDevice?.address.equals(
                address,
                ignoreCase = true
            )
        }
        device?.availableSensorId = availableSensorId
    }

    fun setStatusConnected(address: String, status: Boolean) {
        mBluetoothDeviceList.firstOrNull {
            it.mBluetoothDevice!!.address.equals(
                address,
                ignoreCase = true
            )
        }?.isConnected = status
    }

    @SuppressLint("MissingPermission")
    fun setData(address: String, data: String) {
        //TODO replace Array<Any>::class.java to explicit class type definition
        val mapType = object : TypeToken<Map<Int, Any>>() {}.type
        val sensorData: Map<Int, Any> = gson.fromJson(data, mapType)
        var device = mBluetoothDeviceList.firstOrNull {
            it.mBluetoothDevice!!.address.equals(
                address,
                ignoreCase = true
            )
        }
        device?.deviceData?.putAll(sensorData)
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
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            //Check if the permission is granted or not.
            // Settings activity never returns proper value so instead check with following method
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(this)
                } else {
                    true
                }
            ) {
                changeStatus(true)
                //finish()
            } else { //Permission is not available
                Toast.makeText(
                    this,
                    "Draw over other app permission not available. Closing the application",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {

        private const val PERMISSIONS_REQUEST = 2
        private const val CODE_DRAW_OVER_OTHER_APP_PERMISSION = 1404
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
        //mLastConnectedDevice = indexOf(bluetoothDeviceData.mBluetoothDevice)
        val mDeviceName = bluetoothDeviceData.mBluetoothDevice!!.name
        val mDeviceAddress = bluetoothDeviceData.mBluetoothDevice!!.address
        Log.i("MainActivity", "connecting device: $mDeviceName($mDeviceAddress)")
        if (mBluetoothLeService != null) {
            if (!bluetoothDeviceData.isConnected) {
                mBluetoothLeService!!.connect(mDeviceAddress)
                showMsg("connecting device : $mDeviceName")
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