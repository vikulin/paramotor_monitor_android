package org.rivchain.paramotor_monitor

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        var profile = BluetoothDeviceData.sensorProfile[mSensorId[position]]
        //exponent
        holder.data.text = data.div(Integer.parseInt(profile[4].toString())).toString()
        holder.data_label.text = profile[1].toString()
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