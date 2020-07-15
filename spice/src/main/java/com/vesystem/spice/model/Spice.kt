package com.vesystem.spice.model

import android.content.Context
import android.content.Intent
import android.util.Log
import com.vesystem.spice.ui.KRemoteCanvasActivity
import java.io.File
import kotlin.properties.Delegates

/**
 * Created Date 2020/7/15.
 * @author ChenRui
 * ClassDescription:连接bean
 */
class Spice {

    companion object {
        var ip: String by Delegates.notNull()
        var port: String by Delegates.notNull()
        val tport = "-1"
        var password: String by Delegates.notNull()
        var cf: String by Delegates.notNull()
        val ca: String? = null
        val cs: String = ""
        var sound: Boolean by Delegates.notNull()

        fun connect(ip: String, port: String, password: String, sound: Boolean):Companion {
            this.ip = ip
            this.port = port
            this.password = password
            this.sound = sound
            return this
        }

        fun start(context: Context) {
            cf = context.filesDir.path + File.separator + "ca0.pem"
            Log.e("Spice,", "start: $cf")
            context.startActivity(Intent(context, KRemoteCanvasActivity::class.java))
        }
    }
}