package com.vesystem.spice.utils

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.vesystem.spice.model.KSpice

/**
 * Created Date 2020/4/16.
 *
 * @author ChenRui
 * ClassDescription：检测Android系统运行环境，TV或手机。true:手机 false：电视
 */
object SystemRunEnvUtil {
    /**
     * 根据屏幕的物理尺寸检测
     * 小于6.5则为手机，否则是电视
     * true:手机 false：电视
     *
     * @param context
     * @return
     */
    private fun checkScreenIsPhone(context: Context): Boolean {
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val defaultDisplay = windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        defaultDisplay.getMetrics(displayMetrics)
        val x =
            Math.pow(displayMetrics.widthPixels / displayMetrics.xdpi.toDouble(), 2.0)
        val y =
            Math.pow(displayMetrics.heightPixels / displayMetrics.ydpi.toDouble(), 2.0)
        val screenInches = Math.sqrt(x + y)
        return screenInches < 6.5
    }

    /**
     * 根据屏幕布局检测
     * true:手机 false：电视、平板
     *
     * @param context
     * @return
     */
    private fun checkScreenLayoutIsPhone(context: Context): Boolean {
        return context.resources
            .configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK <= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    /**
     * 根据SIM卡状态检测
     * PHONE_TYPE_NONE：代表不是手机类型
     * true:手机 false：电视
     *
     * @param context
     * @return
     */
    private fun checkSIMStateIsPhone(context: Context): Boolean {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.phoneType != TelephonyManager.PHONE_TYPE_NONE
    }

    /**
     * 根据电池当前状态检测，如果是满的就是电视，否则是手机
     *
     * @param context
     * @return
     */
    private fun checkBatteryIsPhone(context: Context): Boolean {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        //当前电池状态
        val status = batteryStatus!!.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_FULL

        //当前充电状态
        val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
        return !(isCharging && acCharge)
    }


    /**
     * 检查设备不为TV，可能是平板和手机或者其他设备
     */
    private fun checkDeviceIsTv(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /**
     * 综合检测，防止误判
     *
     * @param context
     * @return
     */
    /*fun comprehensiveCheckSystemEnv(context: Context): Boolean {
        Log.d("TAG", "comprehensiveCheckSystemEnv:${checkScreenIsPhone(context)},${checkScreenLayoutIsPhone(context)},${checkSIMStateIsPhone(context)},${checkBatteryIsPhone(context)},${checkDeviceIsPhone(context)} ")
        return checkScreenIsPhone(context) || checkScreenLayoutIsPhone(
            context
        ) || checkSIMStateIsPhone(context) || checkBatteryIsPhone(
            context
        )
    }*/


    /**
     * 综合检测，防止误判
     * PHONE:手机 FLAT：平板 PHONE：TV
     */
    fun comprehensiveCheckSystemEnv(context: Context): Int {
        /*Log.d(
            "SystemRunEnvUtil",
            "comprehensiveCheckSystemEnv:${checkScreenIsPhone(context)},${checkScreenLayoutIsPhone(
                context
            )},${checkDeviceIsTv(context)},${checkBatteryIsPhone(context)},${checkSIMStateIsPhone(
                context
            )} "
        )*/
        return if (checkScreenIsPhone(context) && checkScreenLayoutIsPhone(context) && !checkDeviceIsTv(
                context
            )
        ) {
            KSpice.PHONE
        } else if (checkSIMStateIsPhone(context) && !checkDeviceIsTv(context)) {
            KSpice.FLAT
        } else if (checkBatteryIsPhone(context) || checkDeviceIsTv(context) && !(checkScreenIsPhone(
                context
            ) && checkScreenLayoutIsPhone(context) && checkSIMStateIsPhone(context))
        ) {
            KSpice.TELEVISION
        } else {
            KSpice.PHONE
        }
    }
}