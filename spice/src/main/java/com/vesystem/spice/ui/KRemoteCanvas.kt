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
import com.vesystem.spice.model.KMessageEvent.Companion.SPICE_CONNECT_FAILURE
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
    var spiceCommunicator: SpiceCommunicator? = null//远程连接
    private var drawable: WeakReference<Drawable>? = null//渲染bitmap
    var scope: WeakReference<Job>? = null
    private var canvasBitmap: Bitmap? = null
    var myHandler: Handler? = null
    var ktvMouse: KTVMouse? = null
    var isSetCanvasMatrix = false

    init {
        setBackgroundColor(Color.BLACK)
        myHandler = Handler(Handler.Callback {
            when (it.what) {
                SPICE_CONNECT_TIMEOUT -> {
                    EventBus.getDefault().post(KMessageEvent(SPICE_CONNECT_TIMEOUT))
                }
                SPICE_CONNECT_FAILURE -> {
                    EventBus.getDefault().post(KMessageEvent(SPICE_CONNECT_FAILURE))
                }
            }
            false
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        spiceCommunicator = SpiceCommunicator(context.applicationContext)


        /**
         * 原生方法得接口回调
         */
        spiceCommunicator?.setSpiceConnect(object :
            KSpiceConnect {
            override fun onUpdateBitmapWH(width: Int, height: Int) {
//                Log.i(TAG, "onUpdateBitmapWH: $width,$height")
                reallocateDrawable(width, height)
                if (!isSetCanvasMatrix) {
                    spiceRequestResolution(width, height)
                }
            }

            override fun onUpdateBitmap(x: Int, y: Int, width: Int, height: Int) {
//                Log.i(TAG, "onUpdateBitmap: x:$x,Y:$y,W:$width,H:$height")
                postInvalidate()
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
//                Log.i(TAG, "onConnectSucceed: 连接成功")
                myHandler?.removeMessages(SPICE_CONNECT_TIMEOUT)
                myHandler?.removeMessages(SPICE_CONNECT_FAILURE)
                EventBus.getDefault().post(
                    KMessageEvent(
                        KMessageEvent.SPICE_CONNECT_SUCCESS
                    )
                )
                myHandler?.post {
                    ktvMouse = KTVMouse(context.applicationContext, this@KRemoteCanvas)
                }
            }

            override fun onConnectFail() {
//                Log.i(TAG, "onConnectFail: 连接失败")
                myHandler?.removeMessages(SPICE_CONNECT_TIMEOUT)
                myHandler?.sendEmptyMessageDelayed(SPICE_CONNECT_FAILURE, 3 * 1000)
            }
        })


        scope = WeakReference(GlobalScope.launch {
            myHandler?.sendEmptyMessageDelayed(SPICE_CONNECT_TIMEOUT, 5 * 1000)
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
        spiceCommunicator?.connectSpice(
            Spice.ip,
            Spice.port,
            Spice.tPort,
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
    fun spiceRequestResolution(width: Int, height: Int) {
        if (width != this.width || height != this.height) {
            spiceCommunicator?.SpiceRequestResolution(this.width, this.height)
        }
    }

    fun updateSpiceResolvingPower(width: Int, height: Int) {
        spiceCommunicator?.isClickDisconnect?.let {
            if(it){
                isSetCanvasMatrix = true
                spiceCommunicator?.SpiceRequestResolution(width, height)
            }
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
        spiceCommunicator?.setBitmap(canvasBitmap)
        post(drawableRunnable)
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
     * 监听鼠标按下时得移动
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        ktvMouse?.onTouchEvent(event)
        return true
    }

    /**
     * 鼠标移动、按下、松开、中间键滚动
     */
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        ktvMouse?.let {
            return it.onTouchEvent(event)
        }
        return super.onGenericMotionEvent(event)
    }


    /**
     * 处理鼠标按下事件
     */
    override fun handlerMouseEvent(
        x: Int,
        y: Int,
        metaState: Int,
        mouseType: Int,
        isMove: Boolean
    ) {
        spiceCommunicator?.writePointerEvent(
            x,
            y,
            metaState,
            mouseType and POINTER_DOWN_MASK.inv(),
            isMove
        )
        spiceCommunicator?.writePointerEvent(
            x,
            y,
            metaState,
            mouseType or POINTER_DOWN_MASK,
            isMove
        )
    }

    override fun mouseDownMove(x: Int, y: Int, metaState: Int, mouseType: Int, isMove: Boolean) {
        spiceCommunicator?.writePointerEvent(
            x,
            y,
            metaState,
            mouseType or POINTER_DOWN_MASK,
            isMove
        )
    }


    override fun mouseMove(x: Int, y: Int, metaState: Int, mouseType: Int, isMove: Boolean) {
        spiceCommunicator?.writePointerEvent(
            x, y, metaState,
            mouseType, isMove
        )
        postInvalidate()
//        reDraw(x, y, width, height)
    }


    /**
     * 处理鼠标松开事件、重置鼠标
     */
    override fun releaseMouseEvent(
        x: Int,
        y: Int,
        metaState: Int,
        mouseType: Int,
        isMove: Boolean
    ) {
        spiceCommunicator?.writePointerEvent(
            x,
            y,
            metaState,
            mouseType and POINTER_DOWN_MASK.inv(),
            isMove
        )
        spiceCommunicator?.writePointerEvent(x, y, metaState, 0, isMove)
    }


    //鼠标右键的按下、松开操作
    fun rightMouseButton(isDown: Boolean) {
        if (isDown) {
            ktvMouse?.downMouseRightButton()
        } else {
            ktvMouse?.upMouseRightButton()
        }
    }


    companion object {
        private const val TAG = "KRemoteCanvas"
    }

}