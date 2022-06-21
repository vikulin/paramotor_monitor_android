package org.rivchain.paramotor_monitor

import android.app.Application
import android.content.Context
import android.os.Process

/**
 * Created by WGH on 2017/4/10.
 */
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        mPId = Process.myPid()
    }

    companion object {

        private var context: Context? = null

        private var mPId = 0

        fun context(): Context? {
            return context
        }

        fun getmPId(): Int {
            return mPId
        }
    }
}