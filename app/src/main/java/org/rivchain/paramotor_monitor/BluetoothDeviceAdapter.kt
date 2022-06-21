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
import kotlin.collections.ArrayList

/**
 * Created by WGH on 2017/4/10.
 */
class BluetoothDeviceAdapter(
    private val mBluetoothDeviceList: List<BluetoothDeviceData>,
    private val mBluetoothClickListener: OnBluetoothDeviceClickedListener
) :    RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder>() {

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
            if (holder.absoluteAdapterPosition < 0) {
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
            holder.deviceName.text = "device unknown"
        } else {
            holder.deviceName.text = device.mBluetoothDevice!!.name
        }
        if(device.isConnected){
            holder.deviceImage.setImageResource(R.drawable.bluetoothf_connected)
        } else {
            holder.deviceImage.setImageResource(R.drawable.bluetoothf)
        }
    }

    override fun getItemCount(): Int {
        return mBluetoothDeviceList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var cardView: CardView
        var deviceImage: ImageView
        var deviceName: TextView

        init {
            cardView = view as CardView
            deviceImage = view.findViewById<View>(R.id.device_image) as ImageView
            deviceName = view.findViewById<View>(R.id.device_name) as TextView
        }
    }

    init {
        initDrawableList()
    }
}