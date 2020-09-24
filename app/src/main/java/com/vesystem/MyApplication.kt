package com.vesystem

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Process
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import com.github.jokar.multilanguages.library.MultiLanguage
import com.vesystem.test.LocalManageUtil


/**
 * Created Date 2020/7/6.
 *
 * @author ChenRui
 * ClassDescription:
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        MultiLanguage.init { context -> //return your local settings
            LocalManageUtil.getSetLanguageLocale(context)
        }
        MultiLanguage.setApplicationLanguage(this)

        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectAll() //检测Activity泄露
                .penaltyLog() //在Logcat中打印违规日志
                .build()
        )
    }

    private fun currentProcessName(context: Context): String {
        val manager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (runningAppProcess in manager.runningAppProcesses) {
            if (runningAppProcess.pid == Process.myPid()) {
                return runningAppProcess.processName
            }
        }
        return ""
    }

    override fun attachBaseContext(base: Context?) {
        //Save the system language selection when entering the app for the first time.
        LocalManageUtil.saveSystemCurrentLanguage(base)
        super.attachBaseContext(MultiLanguage.setLocal(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        /**
         * The user saves the system selection language when switching languages on the system settings page (in order to select when the system language is used, if it is not saved, it will not be available after switching languages)
         */
        LocalManageUtil.saveSystemCurrentLanguage(applicationContext, newConfig)
        MultiLanguage.onConfigurationChanged(applicationContext)
    }

    companion object {
        private const val TAG = "MyApplication"
        lateinit var INSTANCE: MyApplication
    }
}