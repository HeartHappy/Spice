package com.vesystem.spice.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.vesystem.opaque.SpiceCommunicator
import com.vesystem.spice.interfaces.KSpiceConnect
import com.vesystem.spice.interfaces.KViewable
import com.vesystem.spice.model.KMessageEvent
import com.vesystem.spice.model.KMessageEvent.Companion.SPICE_CONNECT_TIMEOUT
import com.vesystem.spice.model.Spice
import com.vesystem.spice.mouse.IMouseOperation
import com.vesystem.spice.mouse.KTVMouse
import com.vesystem.spice.mouse.KTVMouse.Companion.POINTER_DOWN_MASK
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.lang.ref.WeakReference

/**
 * Created Date 2020/7/6.
 * @author ChenRui
 * ClassDescription:自定义图形渲染View
 * 负责处理：
 * 1、创建连接
 * 2、连接后的画面实时绘制
 * 3、TV端鼠标的交互操作
 * 4、界面销毁断开连接、主动断开连接
 */
class KRemoteCanvas(context: Context?, attrs: AttributeSet?) : AppCompatImageView(context, attrs),
    KViewable, IMouseOperation {
    private var shiftY: Int = 0
    private var shiftX: Int = 0
    var spiceCommunicator: WeakReference<SpiceCommunicator>? = null//远程连接
    private var drawable: WeakReference<Drawable>? = null//渲染bitmap
    var scope: WeakReference<Job>? = null
    private var canvasBitmap: Bitmap? = null
    var myHandler: Handler? = null
    var ktvMouse: KTVMouse? = null
    private var mouseX: Int = 0
    private var mouseY: Int = 0

    init {
        setBackgroundColor(Color.BLACK)
        myHandler = Handler(Handler.Callback {
            when (it.what) {
                SPICE_CONNECT_TIMEOUT -> EventBus.getDefault()
                    .post(
                        KMessageEvent(
                            SPICE_CONNECT_TIMEOUT
                        )
                    )
            }
            false
        })

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        spiceCommunicator = WeakReference(
            SpiceCommunicator(
                context.applicationContext
            )
        )

        /**
         * 原生方法得接口回调
         */
        spiceCommunicator?.get()?.setSpiceConnect(object :
            KSpiceConnect {
            override fun onUpdateBitmapWH(width: Int, height: Int) {
//                Log.i(TAG, "onUpdateBitmapWH: $width,$height")
                computeShiftFromFullToView(width, height)
                reallocateDrawable(width, height)
                spiceRequestResolution(width, height)
            }

            override fun onUpdateBitmap(x: Int, y: Int, width: Int, height: Int) {
//                Log.i(TAG, "onUpdateBitmap: x:$x,Y:$y,W:$width,H:$height")
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
                EventBus.getDefault().post(
                    KMessageEvent(
                        KMessageEvent.SPICE_CONNECT_SUCCESS
                    )
                )
                myHandler?.post {
                    ktvMouse = KTVMouse(context.applicationContext, this@KRemoteCanvas)
                }
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
            Spice.ip,
            Spice.port,
            Spice.tport,
            Spice.password,
            Spice.cf,
            Spice.ca,
            Spice.cs,
            Spice.sound
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


    /**
     * 重绘界面得bitmap
     */
    override fun onDraw(canvas: Canvas) {
        canvasBitmap?.let {
//            Log.i(TAG, "onDraw: 更新")
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    /**
     * 更新Bitmap和Drawable，并设置新得Drawable
     */
    override fun reallocateDrawable(width: Int, height: Int) {
//        Log.i(TAG, "reallocateDrawable: $width,$height")
        //创建bitmap
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvasBitmap?.setHasAlpha(false)
        drawable = WeakReference(
            com.vesystem.spice.bitmap.KCanvasDrawable(
                width,
                height
            )
        )
        //TODO 初始化鼠标
        spiceCommunicator?.get()?.setBitmap(canvasBitmap)
        post(drawableRunnable)
    }


    fun computeShiftFromFullToView(framebufferWidth: Int, framebufferHeight: Int) {
        shiftX = (framebufferWidth - width) / 2
        shiftY = (framebufferHeight - height) / 2
    }

    override fun reDraw(x: Int, y: Int, width: Int, height: Int) {
//        Log.i(TAG, "reDraw: X:$x,Y:$y,W:$width,H:$height")

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


    /**
     *
     *
     *  TV端的鼠标操作
     *
     *
     */

    override fun setMousePointerPosition(x: Int, y: Int) {
        Log.i(TAG, "setMousePointerPosition: X:$x,Y:$y")
    }

    override fun mouseMode(relative: Boolean) {
        Log.i(TAG, "mouseMode: $relative")
    }


    /**
     * 鼠标的操作，单击、双击、滚动
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.i(TAG, "onTouchEvent: ")
        ktvMouse?.gestureDetector?.onTouchEvent(event)
        return true
    }


    /**
     * TV端鼠标移动坐标
     */
    override fun onHoverEvent(event: MotionEvent): Boolean {
        ktvMouse?.isDown?.let {
            if(!it){
                mouseX = event.x.toInt()
                mouseY = event.y.toInt()
                spiceCommunicator?.get()?.writePointerEvent(
                    mouseX, mouseY, 0,
                    0, false
                )
                reDraw(mouseX, mouseY, width, height)
                Log.i(TAG, "KTVPointer: X:${event.x},Y:${event.y}")
            }
        }
        return super.onHoverEvent(event)
    }

    override fun leftButtonDown(x: Int, y: Int, metaState: Int, mouseType: Int) {
        handlerMouseDownEvent(x, y, metaState, mouseType)
    }

    override fun leftButtonUp(x: Int, y: Int, metaState: Int, mouseType: Int) {
        handlerMouseUpEvent(x, y, metaState, mouseType)
    }


    override fun middleButtonDown(x: Int, y: Int, metaState: Int) {
        TODO("Not yet implemented")
    }

    override fun rightButtonDown(x: Int, y: Int, metaState: Int, mouseType: Int) {
        handlerMouseDownEvent(x, y, metaState, mouseType)
    }

    override fun rightButtonUp(x: Int, y: Int, metaState: Int, mouseType: Int) {
        handlerMouseUpEvent(x,y,metaState,mouseType)
    }


    override fun scrollUp(x: Int, y: Int, metaState: Int) {
        TODO("Not yet implemented")
    }

    override fun scrollDown(x: Int, y: Int, metaState: Int) {
        TODO("Not yet implemented")
    }

    override fun releaseButton(x: Int, y: Int, metaState: Int) {
        TODO("Not yet implemented")
    }


    //鼠标右键的按下、松开操作
    fun rightMouseButton(isDown: Boolean) {
        if (isDown) {
            ktvMouse?.downMouseRightButton(mouseX, mouseY)
        } else {
            ktvMouse?.upMouseRightButton(mouseX, mouseY)
        }
    }


    /**
     * 处理鼠标按下事件
     */
    private fun handlerMouseDownEvent(
        x: Int,
        y: Int,
        metaState: Int,
        mouseType: Int
    ) {
        spiceCommunicator?.get()
            ?.writePointerEvent(
                x,
                y,
                metaState,
                mouseType and POINTER_DOWN_MASK.inv(),
                false
            )
        spiceCommunicator?.get()
            ?.writePointerEvent(
                x,
                y,
                metaState,
                mouseType or POINTER_DOWN_MASK,
                false
            )

    }


    /**
     * 处理鼠标松开事件
     */
    private fun handlerMouseUpEvent(
        x: Int,
        y: Int,
        metaState: Int,
        mouseType: Int
    ) {
        spiceCommunicator?.get()
            ?.writePointerEvent(
                x,
                y,
                metaState,
                mouseType and POINTER_DOWN_MASK.inv(),
                false
            )
        spiceCommunicator?.get()
            ?.writePointerEvent(x, y, metaState, 0, false)
    }


    companion object {
        private const val TAG = "KRemoteCanvas"
    }

}