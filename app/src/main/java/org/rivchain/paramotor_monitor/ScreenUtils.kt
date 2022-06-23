package org.rivchain.paramotor_monitor

import android.content.Context
import android.util.DisplayMetrics

/**
 * Created by Vadym Vikulin on 6/23/22.
 */
object ScreenUtils {
    var height = 0
    var width = 0
    fun convertDpToPx(context: Context, dp: Int): Int {
        return Math.round(dp * (context.resources.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }
}