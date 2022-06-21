package org.rivchain.paramotor_monitor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
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
import java.util.*

/**
 * Created by WGH on 2017/4/10.
 */
class MyBluetoothDeviceAdapter(
    private val mBluetoothDeviceList: List<BluetoothDevice>,
    private val mBluetoothClickListener: OnBluetoothDeviceClickedListener
) :    RecyclerView.Adapter<MyBluetoothDeviceAdapter.ViewHolder>() {
    private var mContext: Context? = null
    private val mDrawableList: ArrayList<Int> = ArrayList()
    private var mRandomInt = 0
    private fun initDrawableList() {
        if (mDrawableList.size != 0) {
            mDrawableList.clear()
        }
        mDrawableList.add(R.drawable.bluetoothf)
        val mRandom = Random()
        mRandomInt = mRandom.nextInt(mDrawableList.size)
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
            if (holder.adapterPosition < 0) {
                Log.e(
                    "MyBluetoothDeviceAd",
                    "holder.getAdapterPosition() : " + holder.adapterPosition
                )
                return@OnClickListener
            }
            val device = mBluetoothDeviceList[holder.adapterPosition]
                ?: return@OnClickListener
            BluetoothScan.stopScan()
            mBluetoothClickListener.onBluetoothDeviceClicked(device.name, device.address)
        })
        holder.cardView.setOnLongClickListener {
            Log.e("MyBluetoothDeviceAd", "LongClick :　" + holder.adapterPosition)
            true
        }
        return holder
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = mBluetoothDeviceList[position]
        if (TextUtils.isEmpty(device.name)) {
            holder.deviceName.text = "device unknown"
        } else {
            holder.deviceName.text = device.name
        }
        holder.deviceImage.setImageResource(
            mDrawableList[((position + mRandomInt) %
                    mDrawableList.size + mDrawableList.size) % mDrawableList.size]
        )
        val layoutParams = holder.deviceImage.layoutParams
        holder.deviceImage.layoutParams = layoutParams
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