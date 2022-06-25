package org.rivchain.paramotor_monitor

import android.app.Application
import android.content.Context
import android.os.Process
import com.rivchain.paramotor_monitor.BuildConfig
import com.rivchain.paramotor_monitor.R
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender

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

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            //each plugin you chose above can be configured in a block like this:
            httpSender {
                uri = "https://acrarium.rivchain.org/acrarium/report"
                basicAuthLogin = "xxx"
                basicAuthPassword = "xxx"
                httpMethod = HttpSender.Method.POST
            }
            dialog {
                //required
                text = getString(R.string.report_dialog_text)
                //optional, enables the dialog title
                title = getString(R.string.app_name)
                //defaults to android.R.string.ok
                positiveButtonText = getString(android.R.string.ok)
                //defaults to android.R.string.cancel
                negativeButtonText = getString(android.R.string.cancel)
                //optional, enables the comment input
                commentPrompt = getString(R.string.report_dialog_comment)
                //optional, enables the email input
                //emailPrompt = getString(R.string.report_dialog_email)
                //defaults to android.R.drawable.ic_dialog_alert
                resIcon = android.R.drawable.ic_dialog_alert
                //optional, defaults to @android:style/Theme.Dialog
                resTheme = R.style.AppTheme
            }
        }
    }
}