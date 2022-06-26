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
class SensorDataAdapter(
    var mContext: Context?,
    var mSensorData: MutableList<Int>,
    var mSensorId: MutableList<Int>
) : RecyclerView.Adapter<SensorDataAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(mContext).inflate(R.layout.sensor_data_item, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = mSensorData[position]
        holder.data.text = data.toString()
        holder.data_label.text = BluetoothDeviceData.label[mSensorId[position]]
    }

    fun updateAvailableSensors(availableSensors: Set<Int>){
        mSensorId = availableSensors.toMutableList()
    }

    fun updateSensorData(sensorData: Array<Int>){
        mSensorData = sensorData.toMutableList()
    }

    override fun getItemCount(): Int {
        return mSensorData.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var data: TextView
        var data_label: TextView

        init {
            data = view.findViewById<View>(R.id.data) as TextView
            data_label = view.findViewById<View>(R.id.data_label) as TextView
        }
    }
}