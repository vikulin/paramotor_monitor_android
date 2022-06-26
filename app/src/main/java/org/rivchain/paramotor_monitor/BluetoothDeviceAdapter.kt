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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.rivchain.paramotor_monitor.R

/**
 * Created by WGH on 2017/4/10.
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
            LayoutInflater.from(mContext).inflate(R.layout.bluetoothdevice_item, parent, false)
        val holder = ViewHolder(view)
        holder.cardView.setOnClickListener(View.OnClickListener {
            if (holder.absoluteAdapterPosition < 0 || mBluetoothDeviceList.size == 0) {
                Log.e(
                    "MyBluetoothDeviceAd",
                    "holder.getAdapterPosition() : " + holder.absoluteAdapterPosition
                )
                return@OnClickListener
            }
            val device = mBluetoothDeviceList[holder.absoluteAdapterPosition]
            BluetoothScan.stopScan()
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
            holder.deviceData.visibility = View.VISIBLE
        } else {
            holder.deviceImage.setImageResource(R.drawable.bluetoothf)
            holder.deviceName.visibility = View.VISIBLE
            holder.deviceData.visibility = View.GONE
        }
        if (device.deviceData.rpm > 0) {
            holder.deviceRpm.text = device.deviceData.rpm.toString()
        } else {
            holder.deviceRpm.text = ""
        }
        if (device.deviceData.tc > 0) {
            holder.deviceTemp.text = device.deviceData.tc.toString()
        } else {
            holder.deviceTemp.text = ""
        }
    }

    override fun getItemCount(): Int {
        return mBluetoothDeviceList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var cardView: CardView
        var deviceImage: ImageView
        var deviceName: TextView
        var deviceData: ConstraintLayout
        var deviceRpm: TextView
        var deviceTemp: TextView

        init {
            cardView = view as CardView
            deviceImage = view.findViewById<View>(R.id.device_image) as ImageView
            deviceName = view.findViewById<View>(R.id.device_name) as TextView
            deviceData = view.findViewById<View>(R.id.device_data) as ConstraintLayout
            deviceRpm = view.findViewById<View>(R.id.device_data_rpm) as TextView
            deviceTemp = view.findViewById<View>(R.id.device_data_temp) as TextView
        }
    }

    init {
        initDrawableList()
    }
}