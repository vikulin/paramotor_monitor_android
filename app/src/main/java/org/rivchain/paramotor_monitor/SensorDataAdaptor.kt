package org.rivchain.paramotor_monitor

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rivchain.paramotor_monitor.R
import java.lang.Double
import kotlin.Any
import kotlin.Array
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.Number

/**
 * Created by Vadym Vikulin
 */
class SensorDataAdapter(
    var mContext: Context?,
    var mSensorData: MutableList<Any>,
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
        val profile = BluetoothDeviceData.sensorProfile[mSensorId[position]]
        //exponent
        holder.data.text = cast(data, profile).toString()
        holder.dataLabel.text = profile[2].toString()
    }

    private fun cast(value: Any, profile: Array<Any>): Any {
        if(value == null){
            return "E"
        }
        return when (profile[1]) {
            Int -> (Double.valueOf(value.toString()) / Integer.parseInt(profile[5].toString())).toInt()
            else -> throw IllegalArgumentException("Unsupported Cast")
        }
    }

    fun updateAvailableSensors(availableSensors: Set<Int>) {
        mSensorId = availableSensors.toMutableList()
        if(mSensorData.size == 0){
            mSensorData = arrayOfNulls<Any>(availableSensors.size).toMutableList() as MutableList<Any>
        }
    }

    fun updateSensorData(sensorData: Map<Int, Any>) {
        for (s in sensorData) {
            mSensorData[s.key] = s.value
        }
    }

    override fun getItemCount(): Int {
        return mSensorData.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var data: TextView
        var dataLabel: TextView

        init {
            data = view.findViewById<View>(R.id.data) as TextView
            dataLabel = view.findViewById<View>(R.id.data_label) as TextView
        }
    }
}