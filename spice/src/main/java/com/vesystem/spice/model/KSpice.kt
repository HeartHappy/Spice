package com.vesystem.spice.model

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import com.vesystem.spice.ui.KRemoteCanvasActivity
import com.vesystem.spice.utils.SystemRunEnvUtil
import java.io.File
import kotlin.properties.Delegates

/**
 * Created Date 2020/7/15.
 * @author ChenRui
 * ClassDescription:连接bean
 */
class KSpice {

    companion object {
        private var ip: String by Delegates.notNull()
        private var port: String by Delegates.notNull()
        private const val tPort = "-1"
        private var password: String by Delegates.notNull()
        private var cf: String by Delegates.notNull()

        //        internal val ca: String? = null
//        internal const val cs: String = ""
        private var sound: Boolean = false
        private var resolutionWidth = 0//分辨率宽
        private var resolutionHeight = 0//分辨率高
        private var sysRunEnv = false
        private var mouseMode = MouseMode.MODE_CLICK//默认操作模式，点击


        internal const val SPICE_CONFIG = "SpiceConfig"
        internal const val IP = "IP"
        internal const val PORT = "PORT"
        internal const val TPort = "TPort"
        internal const val PASSWORD = "PASSWORD"
        internal const val CF = "CF"
        internal const val SOUND = "SOUND"
        internal const val MOUSE_MODE = "MOUSE_MODE"
        internal const val SYSTEM_RUN_ENV = "SYSTEM_RUN_ENV"
        internal const val RESOLUTION_WIDTH = "RESOLUTION_WIDTH"
        internal const val RESOLUTION_HEIGHT = "RESOLUTION_HEIGHT"

        const val ACTION_SPICE_CONNECT_SUCCEED = "ACTION_SPICE_CONNECT_SUCCEED"//spice连接成功通知Action

        fun connect(ip: String, port: String, password: String): Companion {
            this.ip = ip
            this.port = port
            this.password = password
            return this
        }

        fun sound(sound: Boolean): Companion {
            this.sound = sound
            return this
        }


        fun resolution(width: Int, height: Int): Companion {
            this.resolutionWidth = width
            this.resolutionHeight = height
            return this
        }

        fun mouseMode(mode: MouseMode): Companion {
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
            whiteSharedPreferences(sp)

            System.gc()
            val intent = Intent(context, KRemoteCanvasActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }


        fun getSpiceConfigSP(context: Context): SharedPreferences {
            return context.getSharedPreferences(SPICE_CONFIG, Context.MODE_PRIVATE)
        }


        private fun whiteSharedPreferences(sp: SharedPreferences) {
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
            edit.apply()
        }


        enum class MouseMode {
            MODE_CLICK,
            MODE_TOUCH
        }
    }
}