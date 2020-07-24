package com.vesystem.spice.model

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.util.Log
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
        internal var ip: String by Delegates.notNull()
        internal var port: String by Delegates.notNull()
        internal const val tPort = "-1"
        internal var password: String by Delegates.notNull()
        internal var cf: String by Delegates.notNull()
        internal val ca: String? = null
        internal const val cs: String = ""
        internal var sound: Boolean = false
        internal var resolutionWidth = 0//分辨率宽
        internal var resolutionheight = 0//分辨率高
        internal var spiceListener: ISpiceListener? = null
        internal var sysRunEnv = false
        internal var mouseMode = MouseMode.MODE_CLICK//默认操作模式，点击

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

        fun listener(spiceListener: ISpiceListener): Companion {
            this.spiceListener = spiceListener
            return this
        }

        fun resolution(width: Int, height: Int): Companion {
            this.resolutionWidth = width
            this.resolutionheight = height
            return this
        }

        fun mouseMode(mode: MouseMode): Companion {
            this.mouseMode = mode
            return this
        }

        fun start(context: Context) {
            cf = context.filesDir.path + File.separator + "ca0.pem"
            sysRunEnv = SystemRunEnvUtil.comprehensiveCheckSystemEnv(context)
            Log.i("KSpice", "start: 系统运行环境：$sysRunEnv")
            val intent = Intent(context, KRemoteCanvasActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }


        interface ISpiceListener {
            fun onSucceed()
            fun onFail(message: String)
        }

        enum class MouseMode {
            MODE_CLICK,
            MODE_TOUCH
        }
    }
}