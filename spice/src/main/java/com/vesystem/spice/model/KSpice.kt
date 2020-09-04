package com.vesystem.spice.model

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.vesystem.spice.ui.KRemoteCanvasActivity
import com.vesystem.spice.utils.SystemRunEnvUtil
import java.io.File
import kotlin.properties.Delegates

/**
 * Created Date 2020/7/15.
 * @author ChenRui
 * ClassDescription:主要负责
 * 1、链式构建
 * 2、本地存储
 * 3、本地读取
 */
object KSpice {

    private var ip: String by Delegates.notNull()
    private var port: String by Delegates.notNull()
    private const val tPort = "-1"
    private var password: String by Delegates.notNull()
    private var cf: String by Delegates.notNull()
    private var sound: Boolean = false
    private var resolutionWidth = 0//分辨率宽
    private var resolutionHeight = 0//分辨率高
    private var sysRunEnv = false  //true :手机  false：TV
    private var mouseMode = MouseMode.MODE_CLICK//默认操作模式，点击
    private var isAdjust = true//默认弹出键盘调整分辨率


    internal const val SPICE_CONFIG = "SpiceConfig"

    //连接配置
    internal const val IP = "IP"
    internal const val PORT = "PORT"
    internal const val TPort = "TPort"
    internal const val PASSWORD = "PASSWORD"
    internal const val CF = "CF"
    internal const val SOUND = "SOUND"

    //设置相关配置
    internal const val MOUSE_MODE = "MOUSE_MODE"

    //系统配置
    internal const val SYSTEM_RUN_ENV = "SYSTEM_RUN_ENV"

    //分辨率配置
    internal const val RESOLUTION_WIDTH = "RESOLUTION_WIDTH"
    internal const val RESOLUTION_HEIGHT = "RESOLUTION_HEIGHT"

    //弹出键盘是否调整分辨率
    internal const val IS_ADJUST = "IS_ADJUST"

    const val ACTION_SPICE_CONNECT_SUCCEED = "ACTION_SPICE_CONNECT_SUCCEED"//spice连接成功通知Action

    fun connect(ip: String, port: String, password: String): KSpice {
        this.ip = ip
        this.port = port
        this.password = password
        return this
    }

    fun sound(sound: Boolean): KSpice {
        this.sound = sound
        return this
    }


    fun resolution(width: Int, height: Int): KSpice {
        this.resolutionWidth = width
        this.resolutionHeight = height
        return this
    }

    fun isAdjust(isAdjust: Boolean): KSpice {
        this.isAdjust = isAdjust
        return this
    }

    fun mouseMode(mode: MouseMode): KSpice {
        this.mouseMode = mode
        return this
    }

    fun start(context: Context) {
        cf = context.filesDir.path + File.separator + "ca0.pem"
        sysRunEnv = SystemRunEnvUtil.comprehensiveCheckSystemEnv(context)

        val widthPixels = context.resources.displayMetrics.widthPixels
        val heightPixels = context.resources.displayMetrics.heightPixels
        resolutionWidth =
            if (kotlin.math.abs(resolutionWidth) > widthPixels) widthPixels else kotlin.math.abs(
                resolutionWidth
            )
        resolutionHeight =
            if (kotlin.math.abs(resolutionHeight) > heightPixels) heightPixels else kotlin.math.abs(
                resolutionHeight
            )
        val sp = getSpiceConfigSP(context)
        writeSharedPreferences(sp)
        System.gc()
        val intent = Intent(context, KRemoteCanvasActivity::class.java)
        context.startActivity(intent)
    }


    private fun getSpiceConfigSP(context: Context): SharedPreferences {
        return context.getSharedPreferences(SPICE_CONFIG, Context.MODE_PRIVATE)
    }


    fun readKeyInInt(context: Context, key: String): Int {
        return getSpiceConfigSP(context).getInt(key, 0)

    }

    fun readKeyInString(context: Context, key: String): String? {
        return getSpiceConfigSP(context).getString(key, "")

    }

    fun readKeyInBoolean(context: Context, key: String): Boolean {
        return getSpiceConfigSP(context).getBoolean(key, false)
    }

    fun writeValueToKey(context: Context, key: String, value: Any) {
        val edit = getSpiceConfigSP(context).edit()
        when (value) {
            is String -> {
                edit.putString(key, value)
            }
            is Int -> {
                edit.putInt(key, value)
            }
            is Boolean -> {
                edit.putBoolean(key, value)
            }
        }
        edit.apply()
    }


    private fun writeSharedPreferences(sp: SharedPreferences) {
        val edit = sp.edit()
        edit.putString(IP, ip)
        edit.putString(PORT, port)
        edit.putString(TPort, tPort)
        edit.putString(PASSWORD, password)
        edit.putString(CF, cf)
        edit.putBoolean(SOUND, sound)
        edit.putBoolean(SYSTEM_RUN_ENV, sysRunEnv)

        edit.putInt(RESOLUTION_WIDTH, resolutionWidth)
        edit.putInt(RESOLUTION_HEIGHT, resolutionHeight)
        edit.putString(MOUSE_MODE, mouseMode.toString())
        edit.putBoolean(IS_ADJUST, isAdjust)
        edit.apply()
    }


    enum class MouseMode {
        MODE_CLICK,
        MODE_TOUCH
    }
}