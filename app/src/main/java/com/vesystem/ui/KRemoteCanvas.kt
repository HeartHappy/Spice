package com.vesystem.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import com.vesystem.opaque.SpiceCommunicator
import com.vesystem.opaque.bitmap.KCanvasDrawable
import com.vesystem.opaque.interfaces.KSpiceConnect
import com.vesystem.opaque.interfaces.KViewable
import com.vesystem.opaque.model.MessageEvent
import com.vesystem.opaque.model.MessageEvent.SPICE_CONNECT_TIMEOUT
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.lang.ref.WeakReference

/**
 * Created Date 2020/7/6.
 * @author ChenRui
 * ClassDescription:
 */
class KRemoteCanvas(context: Context?, attrs: AttributeSet?) : AppCompatImageView(context, attrs),
    KViewable {
    private var shiftY: Int = 0
    private var shiftX: Int = 0
    var spiceCommunicator: WeakReference<SpiceCommunicator>? = null//远程连接
    var drawable: WeakReference<Drawable>? = null//渲染bitmap
    var scope: WeakReference<Job>? = null
    var canvasBitmap: Bitmap? = null
    var myHandler: Handler? = null

    init {
        setBackgroundColor(Color.BLACK)
        myHandler = Handler(Handler.Callback {
            when (it.what) {
                SPICE_CONNECT_TIMEOUT -> EventBus.getDefault()
                    .post(MessageEvent(SPICE_CONNECT_TIMEOUT))
            }
            false
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.i(TAG, "Spice初始化: ")

        spiceCommunicator = WeakReference(
            SpiceCommunicator(
                context.applicationContext
            )
        )

        /**
         * 原生方法得接口回调
         */
        spiceCommunicator?.get()?.setSpiceConnect(object : KSpiceConnect {
            override fun onUpdateBitmapWH(width: Int, height: Int) {
                Log.i(TAG, "onUpdateBitmapWH: $width,$height")
                computeShiftFromFullToView(width, height)
                reallocateDrawable(width, height)
                spiceRequestResolution(width, height)
            }

            override fun onUpdateBitmap(x: Int, y: Int, width: Int, height: Int) {
                Log.i(TAG, "onUpdateBitmap: x:$x,Y:$y,W:$width,H:$height")
                reDraw(x, y, width, height)
            }

            override fun onMouseUpdate(x: Int, y: Int) {
                Log.i(TAG, "onMouseUpdate: $x,$y")
                setMousePointerPosition(x, y)
            }

            override fun onMouseMode(relative: Boolean) {
                Log.i(TAG, "onMouseMode: $relative")
                mouseMode(relative)
            }

            override fun onConnectSucceed() {
                myHandler?.removeMessages(SPICE_CONNECT_TIMEOUT)
                EventBus.getDefault().post(MessageEvent(MessageEvent.SPICE_CONNECT_SUCCESS))
            }
        })


        scope = WeakReference(GlobalScope.launch {
            myHandler?.sendEmptyMessageDelayed(SPICE_CONNECT_TIMEOUT, 5000)
            enabledConnectSpice()
            enabledConnectSpice()
        })
    }


    /**
     * 开始连接
     */
    private fun enabledConnectSpice() {
        //IO 线程里拉取数据
        Log.i(TAG, "启动Spice连接线程: ")
        spiceCommunicator?.get()?.connectSpice(
            "192.168.30.61",
            5901.toString(),
            "-1",
            "gjc6qxl51z",
            "/data/user/0/com.vesystem.ngd/files/ca0.pem",
            null,
            "",
            true
        )
    }

    /**
     * 更新分辨率，如果当前给我得分辨率不是当前屏幕分辨率则，继续请求更新
     */
    private fun spiceRequestResolution(width: Int, height: Int) {
        if (width != this.width || height != this.height) {
            spiceCommunicator?.get()?.SpiceRequestResolution(this.width, this.height)
        }
    }

    override val desiredWidth: Int
        get() = width
    override val desiredHeight: Int
        get() = height
    override val bitmap: Bitmap?
        get() = canvasBitmap

    /*override fun onDraw(canvas: Canvas) {
        canvasBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }*/


    /**
     * 重绘界面得bitmap
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvasBitmap?.let {
            Log.i(TAG, "onDraw: 更新")
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    /**
     * 更新Bitmap和Drawable，并设置新得Drawable
     */
    override fun reallocateDrawable(width: Int, height: Int) {
        Log.i(TAG, "reallocateDrawable: $width,$height")
        //创建bitmap
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvasBitmap?.setHasAlpha(false)
        drawable = WeakReference(KCanvasDrawable(width, height))
//          drawable =   WeakReference(KUltraCompactBitmapData(spiceCommunicator?.get(), true, width, height))
        //TODO 初始化鼠标
        spiceCommunicator?.get()?.setBitmap(canvasBitmap)
        post(drawableRunnable)
//        drawable?.get()?.syncScroll()
    }


    override fun setMousePointerPosition(x: Int, y: Int) {
        Log.i(TAG, "setMousePointerPosition: X:$x,Y:$y")
    }

    override fun mouseMode(relative: Boolean) {
        Log.i(TAG, "mouseMode: $relative")
    }


    fun computeShiftFromFullToView(framebufferWidth: Int, framebufferHeight: Int) {
        shiftX = (framebufferWidth - width) / 2
        shiftY = (framebufferHeight - height) / 2
    }

    override fun reDraw(x: Int, y: Int, width: Int, height: Int) {
        Log.i(TAG, "reDraw: X:$x,Y:$y,W:$width,H:$height")

        val scale = 1
        val shiftedX = (x - shiftX).toFloat()
        val shiftedY = (y - shiftY).toFloat()
        val left = ((shiftedX - 1f) * scale).toInt()
        val top = ((shiftedY - 1f) * scale).toInt()
        val right = ((shiftedX + width + 1f) * scale).toInt()
        val bottom = ((shiftedY + height + 1f) * scale).toInt()
        Log.i(
            TAG,
            "reDraw: L:$left,T:$top,R:$right,B:$bottom,S:$scale"
        )
        // Make the box slightly larger to avoid artifacts due to truncation errors.
        // Make the box slightly larger to avoid artifacts due to truncation errors.
        postInvalidate(
            left, top,
            right, bottom
        )

    }

    private val drawableRunnable = Runnable {
        setImageDrawable(drawable?.get())
    }


    companion object {
        private const val TAG = "KRemoteCanvas"
    }

}