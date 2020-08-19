/*
 * Copyright (C) 2013- Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */
package com.vesystem.opaque

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.vesystem.spice.interfaces.ISpiceConnect
import com.vesystem.spice.model.KMessageEvent
import org.freedesktop.gstreamer.GStreamer
import org.greenrobot.eventbus.EventBus
import java.lang.ref.WeakReference

/**
 * Created Date: 2020/7/7
 *
 * @author ChenRui
 * ClassDescription：该类的 包名、类名不能改动
 */
class SpiceCommunicator(context: Context) {
    private external fun SpiceClientConnect(
        ip: String?,
        port: String?,
        tport: String?,
        password: String?,
        ca_file: String?,
        ca_cert: String?,
        cert_subj: String?,
        sound: Boolean
    ): Int

    private external fun SpiceClientDisconnect()
    private external fun SpiceButtonEvent(
        x: Int,
        y: Int,
        metaState: Int,
        pointerMask: Int,
        rel: Boolean
    )

    private external fun SpiceKeyEvent(keyDown: Boolean, virtualKeyCode: Int)
    external fun UpdateBitmap(bitmap: Bitmap?, x: Int, y: Int, w: Int, h: Int)
    external fun SpiceRequestResolution(x: Int, y: Int)


    var isConnectSucceed = false
    var isClickDisconnect = false

    private var bitmap: Bitmap? = null
    private var spiceConnect: ISpiceConnect? = null

    fun setBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap
    }

    fun setSpiceConnect(spiceConnect: ISpiceConnect?) {
        this.spiceConnect = spiceConnect
    }

    init {
        val wrContext = WeakReference(context)
        myself = WeakReference(this)
        try {
            GStreamer(wrContext.get())
        } catch (e: Exception) {
            e.printStackTrace()
            EventBus.getDefault()
                .post(KMessageEvent(KMessageEvent.SPICE_CONNECT_FAILURE, e.message))
        }
    }

    /**
     * Launches a new thread which performs a plain SPICE connection.
     */
    fun connectSpice(
        ip: String?,
        port: String?,
        tport: String?,
        password: String?,
        cf: String?,
        ca: String?,
        cs: String?,
        sound: Boolean
    ) {
//        Log.e(TAG, "connectSpice:ip: " + ip + ", port:" + port + ", password:" + password + ",tport:" + tport + ", " + cf + ", " + cs);
        SpiceClientConnect(ip, port, tport, password, cf, ca, cs, sound)
        //退出时被释放内存，需要空判
        spiceConnect?.onConnectFail()
    }

    fun disconnect() {
        SpiceClientDisconnect()
        Log.i(TAG, "disconnect: spiceConnect 断开 Spice连接，并滞空")
        myself?.clear()
    }

    fun sendSpiceKeyEvent(keyDown: Boolean, virtualKeyCode: Int) {
        Log.i(TAG, "sendSpiceKeyEvent: down: $keyDown code: $virtualKeyCode")
        SpiceKeyEvent(keyDown, virtualKeyCode)
    }

    fun writePointerEvent(
        x: Int,
        y: Int,
        metaState: Int,
        pointerMask: Int,
        rel: Boolean
    ) {
        Log.d(
            TAG,
            "sendMouseEvent: " + x + "x" + y + "," + "metaState: " + metaState + ", pointerMask: " + pointerMask + ",rel:" + rel
        )
        SpiceButtonEvent(x, y, metaState, pointerMask, rel)
        spiceConnect?.onMouseUpdate(x, y)
    }

    fun onSettingsChanged(width: Int, height: Int) {
//        Log.i(TAG, "onSettingsChanged called, wxh: " + width + "x" + height);
        spiceConnect?.let {
            if (!isConnectSucceed) {
                isConnectSucceed = true
                it.onConnectSucceed()
            }
            it.onUpdateBitmapWH(width, height)
        }
    }


    companion object {
        private const val TAG = "SpiceCommunicator"
        private var myself: WeakReference<SpiceCommunicator>? = null

        init {
            System.loadLibrary("gstreamer_android")
            System.loadLibrary("spice")
        }

        @JvmStatic
        fun sendMessage(message: Int) {
//        Log.i(TAG, "sendMessage called with message: " + message);
            EventBus.getDefault().post(KMessageEvent(message))
        }

        @JvmStatic
        fun sendMessageWithText(message: Int, messageText: String?) {
//        Log.i(TAG, "sendMessageWithText: " + messageText);
            EventBus.getDefault().post(KMessageEvent(message))
        }

        @JvmStatic
        private fun OnSettingsChanged(inst: Int, width: Int, height: Int, bpp: Int) {
//        Log.i(TAG, "OnSettingsChanged: inst:" + inst);
            myself?.get()?.onSettingsChanged(width, height)
        }

        @JvmStatic
        private fun OnGraphicsUpdate(
            inst: Int,
            x: Int,
            y: Int,
            width: Int,
            height: Int
        ) {
            //android.util.Log.i(TAG, "OnGraphicsUpdate called: " + x +", " + y + " + " + width + "x" + height );
            myself?.get()?.bitmap?.let {
                synchronized(it) {
//            Log.i(TAG, "OnGraphicsUpdate: 更新Bitmap");
                    myself?.get()?.UpdateBitmap(
                        it,
                        x,
                        y,
                        width,
                        height
                    )
                }
                myself?.get()?.spiceConnect?.onUpdateBitmap(
                    x,
                    y,
                    width,
                    height
                )
            }
        }

        @JvmStatic
        private fun OnMouseUpdate(x: Int, y: Int) {
            Log.d(TAG, "OnMouseUpdate: X:$x,Y:$y");
            //android.util.Log.i(TAG, "OnMouseUpdate called: " + x +", " + y);
            myself?.get()?.spiceConnect?.onMouseUpdate(x, y)
        }

        @JvmStatic
        private fun OnMouseMode(relative: Boolean) {
            //        Log.i(TAG, "OnMouseMode called, relative: " + relative);
//            myself?.get()?.spiceConnect?.onMouseMode(relative)
        }
    }
}