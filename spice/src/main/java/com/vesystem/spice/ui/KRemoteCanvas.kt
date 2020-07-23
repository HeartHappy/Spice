package com.vesystem.spice.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.vesystem.opaque.SpiceCommunicator
import com.vesystem.spice.R
import com.vesystem.spice.bitmap.KCanvasDrawable
import com.vesystem.spice.interfaces.ISpiceConnect
import com.vesystem.spice.interfaces.IViewable
import com.vesystem.spice.model.KMessageEvent
import com.vesystem.spice.model.KMessageEvent.Companion.SPICE_CONNECT_FAILURE
import com.vesystem.spice.model.KMessageEvent.Companion.SPICE_CONNECT_TIMEOUT
import com.vesystem.spice.model.KSpice
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
 * 4、调整分辨率
 * 5、界面销毁断开连接、主动断开连接
 */
class KRemoteCanvas(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs),
    IViewable, IMouseOperation {
    var spiceCommunicator: SpiceCommunicator? = null//远程连接
    private var drawable: WeakReference<Drawable>? = null//渲染bitmap
    var scope: WeakReference<Job>? = null
    private var canvasBitmap: Bitmap? = null
    var myHandler: Handler? = null
    var ktvMouse: KTVMouse? = null
    var bitmapMatrix: Matrix = Matrix()
    var bitmapWidth = 0
    var bitmapHeight = 0
    var isConnectFail=false
    var dx = 0
    var dy = 0

    init {
        setBackgroundColor(Color.BLACK)

        //计算bitmap在布局中位置
        myHandler = Handler(Handler.Callback {
            when (it.what) {
                SPICE_CONNECT_TIMEOUT -> {
                    EventBus.getDefault().post(KMessageEvent(SPICE_CONNECT_TIMEOUT,resources.getString(R.string.error_connect_timeout)))
                }
                SPICE_CONNECT_FAILURE -> {
                    val sc = spiceCommunicator
                    sc?.isConnectSucceed?.let {
                        //如果时连接情况下，被断开，视为其他设备占用，导致断开连接
                        when {
                            //远程连接被断开
                            sc.isConnectSucceed -> {
//                            Log.i(TAG, "eventBus: 连接被断开")
                                KSpice.spiceListener?.onFail(resources.getString(R.string.error_connection_interrupted))
                                EventBus.getDefault().post(KMessageEvent(SPICE_CONNECT_FAILURE,resources.getString(R.string.error_connection_interrupted)))
                            }
                            //点击断开连接，返回得失败
                            sc.isClickDisconnect -> {
//                            Log.i("MessageEvent", "eventBus:点击导致得断开连接，返回得失败 ")
                            }
                            //连接时，返回得连接失败,  注意：需要区分两次连接导致得连接失败，还是配置参数导致得失败
                            else -> {
//                            Log.i("MessageEvent", "eventBus: 连接失败,无法连接或认证ca主题")
                                if(!isConnectFail){
                                    isConnectFail=true
                                    Log.i(TAG, "so库问题，连接时，偶发认证失败，失败后重连")
                                    enabledConnectSpice()
                                }else{
                                    KSpice.spiceListener?.onFail(resources.getString(R.string.error_spice_unable_to_connect))
                                    EventBus.getDefault().post(KMessageEvent(SPICE_CONNECT_FAILURE,resources.getString(R.string.error_spice_unable_to_connect)))
                                    Log.i(TAG, "第二次连接失败: ")
                                }
                            }
                        }
                    }
                }
            }
            false
        })


        spiceCommunicator = SpiceCommunicator(context.applicationContext)


        /**
         * 原生方法得接口回调
         */
        spiceCommunicator?.setSpiceConnect(object :
            ISpiceConnect {
            override fun onUpdateBitmapWH(width: Int, height: Int) {
                Log.i(TAG, "onUpdateBitmapWH: $width,$height，bw:$bitmapWidth,bh:$bitmapHeight")
                if (width != bitmapWidth || height != bitmapHeight) {
                    recoverySpiceResolvingPower()
                }
                reallocateDrawable(width, height)
               /* if(width==bitmapWidth&& height==bitmapHeight){
                    reallocateDrawable(width, height)
                }else{
                    recoverySpiceResolvingPower()
                }*/
            }

            override fun onUpdateBitmap(x: Int, y: Int, width: Int, height: Int) {
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
                KSpice.spiceListener?.onSucceed()
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
                myHandler?.sendEmptyMessage(SPICE_CONNECT_FAILURE)
            }
        })

        enabledConnectSpice()

    }

    /**
     * 开始连接
     */
    private fun enabledConnectSpice() {
        //IO 线程里拉取数据
        myHandler?.sendEmptyMessageDelayed(SPICE_CONNECT_TIMEOUT, 5 * 1000)
        scope = WeakReference(GlobalScope.launch {
            Log.i(TAG, "启动Spice连接线程: ")
            spiceCommunicator?.connectSpice(
                KSpice.ip,
                KSpice.port,
                KSpice.tPort,
                KSpice.password,
                KSpice.cf,
                KSpice.ca,
                KSpice.cs,
                KSpice.sound
            )
        })

    }

    /**
     * 调整为键盘显示时的分辨率
     */
    fun updateSpiceResolvingPower(width: Int, height: Int) {
        setBitmapConfig(width, height)
        configMatrix(this.width, this.height, this.width, this.height)
    }


    /**
     * 恢复默认配置分辨率
     */
    fun recoverySpiceResolvingPower() {
        setBitmapConfig(KSpice.resolutionWidth, KSpice.resolutionheight)
        Log.i(
            TAG,
            "recoverySpiceResolvingPower: 默认配置${KSpice.resolutionWidth},${KSpice.resolutionheight}"
        )
        configMatrix(this.width, this.height, KSpice.resolutionWidth, KSpice.resolutionheight)
    }


    /**
     * 设置bitmap显示宽高
     */
    private fun setBitmapConfig(width: Int, height: Int) {
        bitmapWidth = width
        bitmapHeight = height

        spiceCommunicator?.SpiceRequestResolution(bitmapWidth, bitmapHeight)
    }


    /**
     * 更新为配置的matrix
     */
    private fun configMatrix(sw: Int, sh: Int, pw: Int, ph: Int) {
        dx = (sw - pw) / 2
        dy = (sh - ph) / 2
        Log.i(TAG, "updateMatrix: $dx,$dy")
        bitmapMatrix.setTranslate(dx.toFloat(), dy.toFloat())
    }


    /**
     * 重绘界面得bitmap
     */
    override fun onDraw(canvas: Canvas) {
        canvasBitmap?.let {
            canvas.drawBitmap(it, bitmapMatrix, null)
        }
    }


    /**
     * 更新Bitmap和Drawable，并设置新得Drawable
     */
    override fun reallocateDrawable(width: Int, height: Int) {
        //创建bitmap
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvasBitmap?.setHasAlpha(false)
        drawable = WeakReference(
            KCanvasDrawable(
                width,
                height
            )
        )
        Log.i(TAG, "reallocateDrawable: $drawable")
        //TODO 初始化鼠标
        spiceCommunicator?.setBitmap(canvasBitmap)
        post { setImageDrawable(drawable?.get()) }
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
        ktvMouse?.onTouchEvent(event, dx, dy)
        return true
    }


    /**
     * 鼠标移动、按下、松开、中间键滚动
     */
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        ktvMouse?.let {
            return it.onTouchEvent(event, dx, dy)
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


    /**
     * 鼠标按下移动
     */
    override fun mouseDownMove(x: Int, y: Int, metaState: Int, mouseType: Int, isMove: Boolean) {
        spiceCommunicator?.writePointerEvent(
            x,
            y,
            metaState,
            mouseType or POINTER_DOWN_MASK,
            isMove
        )
    }


    /**
     * 鼠标移动
     */
    override fun mouseMove(x: Int, y: Int, metaState: Int, mouseType: Int, isMove: Boolean) {
        spiceCommunicator?.writePointerEvent(
            x, y, metaState,
            mouseType, isMove
        )
        postInvalidate()
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


    /**
     * 鼠标右键的按下、松开操作
     */
    fun rightMouseButton(isDown: Boolean) {
        if (isDown) {
            ktvMouse?.downMouseRightButton()
        } else {
            ktvMouse?.upMouseRightButton()
        }
    }


    /**
     * 发送键盘key
     */
    fun sendKey(code: Int, down: Boolean) {
        spiceCommunicator?.sendSpiceKeyEvent(down, code)
    }


    /**
     * 断开spice连接并取消协程
     */
    fun close() {
        val sc = spiceCommunicator
        sc?.isConnectSucceed?.let {
            if (sc.isConnectSucceed) {
                sc.isConnectSucceed = false
                sc.isClickDisconnect = true
                sc.disconnect()
            }
        }
        scope?.get()?.cancel()
    }


    companion object {
        private const val TAG = "KRemoteCanvas"
    }

}