package org.rivchain.paramotor_monitor

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.rivchain.paramotor_monitor.R

/**
 * Created by Vadym Vikulin
 */
class BluetoothDeviceAdapter(
    val mBluetoothDeviceList: MutableList<BluetoothDeviceData>,
    private val mBluetoothClickListener: OnBluetoothDeviceClickedListener
) : RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder>() {

    private var mContext: Context? = null
    private val mDrawableList: ArrayList<Int> = ArrayList()

    private fun initDrawableList() {
        if (mDrawableList.size != 0) {
            mDrawableList.clear()
        }
        mDrawableList.add(R.drawable.bluetoothf)
    }

    @SuppressLint("MissingPermission")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (mContext == null) {
            mContext = parent.context
        }
        val view =
            LayoutInflater.from(mContext).inflate(R.layout.bt_device_item, parent, false)
        val holder = ViewHolder(view)
        holder.cardView.setOnClickListener(View.OnClickListener {
            if (holder.bindingAdapterPosition < 0 || mBluetoothDeviceList.size == 0) {
                Log.e(
                    "MyBluetoothDeviceAd",
                    "holder.getAdapterPosition() : " + holder.bindingAdapterPosition
                )
                return@OnClickListener
            }
            BluetoothScan.stopScan()
            val device = mBluetoothDeviceList[holder.bindingAdapterPosition]
            mBluetoothClickListener.onBluetoothDeviceClicked(device)
        })
        holder.cardView.setOnLongClickListener {
            Log.e("MyBluetoothDeviceAd", "LongClick :　" + holder.absoluteAdapterPosition)
            true
        }
        return holder
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = mBluetoothDeviceList[position]
        if (TextUtils.isEmpty(device.mBluetoothDevice!!.name)) {
            holder.deviceName.text = "unknown device"
        } else {
            holder.deviceName.text = device.mBluetoothDevice!!.name
        }
        if (device.isConnected) {
            holder.deviceImage.setImageResource(R.drawable.bluetoothf_connected)
            holder.deviceName.visibility = View.GONE
            holder.sensorData.visibility = View.VISIBLE
        } else {
            holder.deviceImage.setImageResource(R.drawable.bluetoothf)
            holder.deviceName.visibility = View.VISIBLE
            holder.sensorData.visibility = View.GONE
        }
        if(device.availableSensorId.isNotEmpty()){
            var sensorData = (device.deviceData.sensorData as Array<Any>)
            if(sensorData.isNotEmpty()) {
                (holder.sensorData.adapter as SensorDataAdapter).updateSensorData(sensorData)
                (holder.sensorData.adapter as SensorDataAdapter).updateAvailableSensors(device.availableSensorId)
                (holder.sensorData.adapter as SensorDataAdapter).notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return mBluetoothDeviceList.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var cardView: CardView
        var deviceImage: ImageView
        var deviceName: TextView
        var sensorData: RecyclerView

        init {
            cardView = view as CardView
            deviceImage = view.findViewById<View>(R.id.device_image) as ImageView
            deviceName = view.findViewById<View>(R.id.device_name) as TextView
            sensorData = view.findViewById<View>(R.id.sensor_data) as RecyclerView
            sensorData.adapter = SensorDataAdapter(this@BluetoothDeviceAdapter.mContext, mutableListOf(), mutableListOf())
        }
    }

    init {
        initDrawableList()
    }
}