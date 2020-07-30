package com.vesystem.spice.ui

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
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
import com.vesystem.spice.mouse.KMobileMouse
import com.vesystem.spice.mouse.KMouse
import com.vesystem.spice.mouse.KMouse.Companion.POINTER_DOWN_MASK
import com.vesystem.spice.mouse.KTVMouse
import com.vesystem.spice.zoom.IZoom
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException

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
    IZoom, IViewable, IMouseOperation, ISpiceConnect {
    private var spiceCommunicator: SpiceCommunicator? = null//远程连接
    private var drawable: WeakReference<Drawable>? = null//渲染bitmap
    private var scope: WeakReference<Job>? = null
    private var canvasBitmap: Bitmap? = null
    private var cursorBitmap: Bitmap? = null
    private var myHandler: Handler? = null
    private var ktvMouse: KMouse? = null
    private var bitmapMatrix: Matrix = Matrix()
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var cursorMatrix: Matrix? = null
    private var isConnectFail = false
    private var connectFailCount = 0
    private var dx = 0//画布距离屏幕偏移量
    private var dy = 0
    private var scaleFactor: Float = 1f
    private var visibleRect = Rect()
    private var viewRect = Rect()


    init {
        setBackgroundColor(Color.BLACK)


        //计算bitmap在布局中位置
        myHandler = Handler(Handler.Callback {
            when (it.what) {
                SPICE_CONNECT_TIMEOUT -> {
                    EventBus.getDefault().post(
                        KMessageEvent(
                            SPICE_CONNECT_TIMEOUT,
                            resources.getString(R.string.error_connect_timeout)
                        )
                    )
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
                                EventBus.getDefault().post(
                                    KMessageEvent(
                                        SPICE_CONNECT_FAILURE,
                                        resources.getString(R.string.error_connection_interrupted)
                                    )
                                )
                            }
                            //点击断开连接，返回得失败
                            sc.isClickDisconnect -> {
//                            Log.i("MessageEvent", "eventBus:点击导致得断开连接，返回得失败 ")
                            }
                            //连接时，返回得连接失败,  注意：需要区分两次连接导致得连接失败，还是配置参数导致得失败
                            else -> {
//                            Log.i("MessageEvent", "eventBus: 连接失败,无法连接或认证ca主题")
                                if (connectFailCount == 3) {
                                    isConnectFail = true
                                }
                                if (connectFailCount < 3) {
                                    ++connectFailCount
                                    Log.i(TAG, "so库问题，连接时，偶发认证失败，失败后重连,重连次数$connectFailCount")
                                    enabledConnectSpice()
                                } else {
                                    connectFailCount = 0
                                    KSpice.spiceListener?.onFail(resources.getString(R.string.error_spice_unable_to_connect))
                                    EventBus.getDefault().post(
                                        KMessageEvent(
                                            SPICE_CONNECT_FAILURE,
                                            resources.getString(R.string.error_spice_unable_to_connect)
                                        )
                                    )
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
        spiceCommunicator?.setSpiceConnect(this)
        myHandler?.postDelayed({ enabledConnectSpice() }, 3000)
    }


    /**
     * 开始连接
     */
    private fun enabledConnectSpice() {
        //IO 线程里拉取数据
        myHandler?.sendEmptyMessageDelayed(SPICE_CONNECT_TIMEOUT, 5 * 1000)
        scope?.get()?.cancel(cause = CancellationException("取消发生异常"))
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
        bitmapWidth = width
        bitmapHeight = height
        setBitmapConfig(width, height)
    }


    /**
     * 恢复默认配置分辨率
     */
    fun recoverySpiceResolvingPower() {
        bitmapWidth = 0
        bitmapHeight = 0
        setBitmapConfig(KSpice.resolutionWidth, KSpice.resolutionheight)
    }


    /**
     * 设置bitmap显示宽高
     */
    private fun setBitmapConfig(width: Int, height: Int) {
        spiceCommunicator?.SpiceRequestResolution(width, height)
    }


    /**
     * 更新为配置的matrix
     */
    private fun configMatrix(sw: Int, sh: Int, pw: Int, ph: Int) {
        dx = (sw - pw) / 2
        dy = (sh - ph) / 2
        bitmapMatrix.setTranslate(dx.toFloat(), dy.toFloat())
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
//        Log.i(TAG, "onSizeChanged: $w,$h")
        if (KSpice.resolutionWidth == 0 || KSpice.resolutionheight == 0) {
            KSpice.resolutionWidth = w
            KSpice.resolutionheight = h
        }

    }

    /**
     * 重绘界面得bitmap
     */
    override fun onDraw(canvas: Canvas) {
        canvasBitmap?.let {
            canvas.drawBitmap(it, bitmapMatrix, null)
            cursorBitmap?.let { cursor ->
                cursorMatrix?.let { cm ->
                    ktvMouse?.let { mouse ->
                        cursorMatrix?.setTranslate(
                            mouse.mouseX.toFloat() + dx,
                            mouse.mouseY.toFloat() + dy
                        )
                    }
                    canvas.drawBitmap(cursor, cm, null)
                }
            }
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
        spiceCommunicator?.setBitmap(canvasBitmap)
        //如果键盘矩阵为0时，调整矩阵位置
        if (bitmapWidth == 0 && bitmapHeight == 0) {
            configMatrix(this.width, this.height, width, height)
        }

        //初始化鼠标
        initCursor()
        post { setImageDrawable(drawable?.get()) }
    }


    /**
     * 初始化鼠标
     */
    private fun initCursor() {
        if (KSpice.sysRunEnv) {
            cursorBitmap = BitmapFactory.decodeResource(resources, R.mipmap.cursor)
            cursorMatrix = Matrix()
            cursorBitmap?.let {
//                Log.i(TAG, "initCursor: 初始化鼠标")
                cursorMatrix?.setScale(1.8f, 1.8f, it.width / 2f, it.height / 2f)
                cursorBitmap =
                    Bitmap.createBitmap(it, 0, 0, it.width, it.height, cursorMatrix!!, false)
            }
        }
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


    fun canvasZoomIn() {
        scaleFactor += 0.5f
        animate().scaleX(scaleFactor).scaleY(scaleFactor).start()
    }


    fun canvasZoomOut() {
        scaleFactor -= 0.5f
        if (scaleFactor < 1) {
            scaleFactor = 1f
            Toast.makeText(context, "比例1：1", Toast.LENGTH_SHORT).show()
            return
        }
        animate().scaleX(scaleFactor).scaleY(scaleFactor)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    translationAfterLimit()
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            }).start()
        //解决缩小时，偏移屏幕中心问题
        /* val ofFloat = ValueAnimator.ofFloat(scaleFactor + 0.5f, scaleFactor)
         ofFloat.addUpdateListener { a ->
             val animatedValue = a.animatedValue as Float
             Log.i(TAG, "canvasZoomOut: $animatedValue")
             translationAfterLimit()
         }
         ofFloat.start()*/
    }


    override fun translation(dx: Int, dy: Int) {
        val isDefaultRect = translationBeforeLimit()
        if (!isDefaultRect) {
            offsetLeftAndRight(dx)
            offsetTopAndBottom(dy)
        }
    }

    override fun translationBeforeLimit(): Boolean {
        getHitRect(viewRect)
        Log.i(TAG, "translationBeforeLimit: 平移之前校验getHitRect：$viewRect")
        return viewRect.left == 0 && viewRect.top == 0 && viewRect.right == width && viewRect.bottom == height
    }

    override fun translationAfterLimit() {
        getGlobalVisibleRect(visibleRect)//在屏幕显示区域Rect
        Log.i(TAG, "translationLimit: 平移之后校验：$visibleRect")

        if (visibleRect.left > 0) {
            offsetLeftAndRight(-visibleRect.left)
        }
        if (visibleRect.top > 0) {
            offsetTopAndBottom(-visibleRect.top)
        }
        if (visibleRect.right < width) {
            offsetLeftAndRight((width - visibleRect.right))
        }
        if (visibleRect.bottom < height) {
            offsetTopAndBottom(height - visibleRect.bottom)
        }
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
        spiceCommunicator?.setSpiceConnect(null)
        spiceCommunicator = null
        myHandler?.removeCallbacksAndMessages(null)
        scope?.get()?.cancel()
        KSpice.resolutionWidth = 0
        KSpice.resolutionheight = 0
    }


    companion object {
        private const val TAG = "KRemoteCanvas"
    }

    override fun onUpdateBitmapWH(width: Int, height: Int) {
        Log.i(TAG, "onUpdateBitmapWH: ")
        if (width == KSpice.resolutionWidth && height == KSpice.resolutionheight || width == bitmapWidth && width == bitmapHeight) {
            reallocateDrawable(width, height)
        } else {
            reallocateDrawable(width, height)
            if (bitmapWidth != 0 && bitmapHeight != 0) {
                updateSpiceResolvingPower(bitmapWidth, bitmapHeight)
            } else {
                recoverySpiceResolvingPower()
            }
        }
    }

    override fun onUpdateBitmap(x: Int, y: Int, width: Int, height: Int) {
        Log.i(TAG, "onUpdateBitmap: ")
        postInvalidate()
    }

    override fun onMouseUpdate(x: Int, y: Int) {
        Log.i(TAG, "onMouseUpdate: ")
        postInvalidate()
    }

    override fun onMouseMode(relative: Boolean) {
        Log.i(TAG, "onMouseMode: ")
    }

    override fun onConnectSucceed() {
        Log.i(TAG, "onConnectSucceed: ")
        KSpice.spiceListener?.onSucceed()
        myHandler?.removeMessages(SPICE_CONNECT_TIMEOUT)
        myHandler?.removeMessages(SPICE_CONNECT_FAILURE)
        EventBus.getDefault().post(
            KMessageEvent(
                KMessageEvent.SPICE_CONNECT_SUCCESS
            )
        )
        myHandler?.post {
            if (KSpice.mouseMode == KSpice.Companion.MouseMode.MODE_CLICK) {
                ktvMouse = KTVMouse(
                    context.applicationContext,
                    this@KRemoteCanvas,
                    this@KRemoteCanvas
                )
            } else if (KSpice.mouseMode == KSpice.Companion.MouseMode.MODE_TOUCH) {
                ktvMouse = KMobileMouse(
                    context.applicationContext,
                    this@KRemoteCanvas,
                    this@KRemoteCanvas
                )
            }
        }
    }

    override fun onConnectFail() {
        Log.i(TAG, "onConnectFail: ")
        myHandler?.removeMessages(SPICE_CONNECT_TIMEOUT)
        myHandler?.sendEmptyMessage(SPICE_CONNECT_FAILURE)
    }


}