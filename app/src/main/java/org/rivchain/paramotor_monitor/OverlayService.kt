package org.rivchain.paramotor_monitor

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.widget.RelativeLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rivchain.paramotor_monitor.R
import java.lang.NumberFormatException

/**
 * Created by Vadym Vikulin on 6/23/22.
 */
class OverlayService : Service() {

    private var mBluetoothDeviceList: MutableList<BluetoothDeviceData> = ArrayList()
    private var mConnectedDeviceAdapter: BluetoothDeviceAdapter? = null
    private var topParams: WindowManager.LayoutParams? = null
    private var windowManager: WindowManager? = null
    private var topView: RelativeLayout? = null
    private var deviceListView: RecyclerView? = null
    //private var topGrab: View? = null
    private val mBinder: IBinder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        val service: OverlayService
            get() = this@OverlayService
    }

    override fun onUnbind(intent: Intent): Boolean {
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        isRunning = true
        initScreenUtils()
        initViews()
        //initOnClicks()
        initOnTouches()
        initReceiver()
    }

    private fun initViews() {
        topView = LayoutInflater.from(this).inflate(R.layout.top, null) as RelativeLayout
        val LAYOUT_FLAG: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        topParams = WindowManager.LayoutParams(
            ScreenUtils.width,
            0,//ScreenUtils.convertDpToPx(this@OverlayService, 150),
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        deviceListView = topView!!.findViewById(R.id.recycler_view)
        val layoutManager = GridLayoutManager(this, 1)
        deviceListView!!.layoutManager = layoutManager
        mConnectedDeviceAdapter = BluetoothDeviceAdapter(mBluetoothDeviceList,
            object : OnBluetoothDeviceClickedListener {
                override fun onBluetoothDeviceClicked(bluetoothDeviceData: BluetoothDeviceData) {

                }
            })
        deviceListView!!.adapter = mConnectedDeviceAdapter
        topParams!!.x = 0
        topParams!!.y = 0
        topParams!!.gravity = Gravity.TOP or Gravity.RIGHT
        windowManager!!.addView(topView, topParams)
    }

    private fun initReceiver() {
        Log.i("OverlayService", "initReceiver()")
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
                Log.i("OverlayService", "ACTION_GATT_CONNECTED!!!")
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
                Log.i("OverlayService", "ACTION_GATT_DISCONNECTED!!!")
                if(mBluetoothDeviceList.size > 0) {
                    mBluetoothDeviceList.removeAt(0)
                    notifyDataSetChanged()
                }
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                //nothing todo
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
                val data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)
                //showMsg("Got string : " + data?.let { String(it) })
                Log.i("OverlayService", "Data!")
                if (data != null && data.size > 0) {
                    val stringBuilder = StringBuilder(data.size)
                    for (byteChar in data) {
                        val b = Char(byteChar.toUShort())
                        stringBuilder.append(b)
                    }
                    Log.i("OverlayService", "Get string : $stringBuilder")
                    if(mBluetoothDeviceList.size > 0) {
                        try {
                            setData(0, stringBuilder.toString())
                            notifyDataSetChanged()
                        } catch (e: NumberFormatException){
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun setData(mDevice: Int, data: String) {
        var newData = DeviceData()
        var sensorData  = data.split("|")
        newData.rpm = Integer.parseInt(sensorData[0])
        newData.temp = Integer.parseInt(sensorData[1])
        mBluetoothDeviceList[mDevice].deviceData = newData
    }

    fun addDevice(device: BluetoothDeviceData){
        if(mBluetoothDeviceList.indexOf(device) < 0) {
            mBluetoothDeviceList.add(0, device)
            topParams!!.height = mBluetoothDeviceList.size * ScreenUtils.convertDpToPx(this@OverlayService, 85)
            windowManager!!.updateViewLayout(topView, topParams)
        }
    }

    fun notifyDataSetChanged(){
        mConnectedDeviceAdapter?.notifyDataSetChanged()
    }

    private fun initScreenUtils() {
        var statusBarHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        val metrics = this.resources.displayMetrics
        ScreenUtils.width = metrics.widthPixels
        ScreenUtils.height = metrics.heightPixels - statusBarHeight
    }

    //TODO implement
    /**
    private fun initOnClicks() {
        topView!!.findViewById<View>(R.id.webButton).setOnLongClickListener {
            stopSelf()
            true
        }
    }
    **/

    private fun initOnTouches() {
        deviceListView!!.setOnTouchListener(OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> return@OnTouchListener true
                MotionEvent.ACTION_MOVE -> {
                    topParams!!.y = Math.max(
                        motionEvent.rawY.toInt(),
                        ScreenUtils.convertDpToPx(this@OverlayService, 50)
                    )
                    windowManager!!.updateViewLayout(topView, topParams)
                    return@OnTouchListener true
                }
                MotionEvent.ACTION_UP -> return@OnTouchListener true
            }
            true
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (topView != null) windowManager!!.removeView(topView)
    }

    companion object {
        var isRunning = false
    }
}