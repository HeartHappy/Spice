package com.vesystem.spice.ui

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
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
import com.vesystem.spice.model.KSpice.Companion.ACTION_SPICE_CONNECT_SUCCEED
import com.vesystem.spice.model.KSpice.Companion.MOUSE_MODE
import com.vesystem.spice.model.KSpice.Companion.RESOLUTION_HEIGHT
import com.vesystem.spice.model.KSpice.Companion.RESOLUTION_WIDTH
import com.vesystem.spice.model.KSpice.Companion.SYSTEM_RUN_ENV
import com.vesystem.spice.mouse.IMouseOperation
import com.vesystem.spice.mouse.KMobileMouse
import com.vesystem.spice.mouse.KMouse
import com.vesystem.spice.mouse.KMouse.Companion.POINTER_DOWN_MASK
import com.vesystem.spice.mouse.KTVMouse
import com.vesystem.spice.utils.KToast
import com.vesystem.spice.zoom.IZoom
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.lang.ref.WeakReference
import kotlin.math.abs


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
        IZoom, IViewable, IMouseOperation {
    private var spiceCommunicator: SpiceCommunicator? = null//远程连接
    private var drawable: WeakReference<Drawable>? = null//渲染bitmap
    private var scope: WeakReference<Job>? = null
    private var canvasBitmap: Bitmap? = null
    private var cursorBitmap: Bitmap? = null
    private var myHandler: Handler? = null
    internal var ktvMouse: KMouse? = null
    private var bitmapMatrix: Matrix = Matrix()
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var cursorMatrix: Matrix? = null
    private var isConnectFail = false
    private var connectFailCount = 0
    internal var dx = 0//画布距离屏幕偏移量
    internal var dy = 0
    private var visibleRect = Rect()
    private var viewRect = Rect()
    private var keyboardHeight = 0
    private var canvasSingleDy = 10//鼠标到达临界点时，view单次偏移量
    private var canvasMaxDy = 0//最大偏移y轴
    internal var scaleFactor: Float = 1f
        set(value) {
            if (value > 1f) {
                field = value
                checkCanvasOutRange()
            } else {
                KToast.show(context, "缩放比例1:1")
            }
        }

    init {
        initHandler()
        initSpice(context)
    }


    private fun initHandler() {
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
                                //Log.d(TAG, "eventBus: 连接被断开")
                                EventBus.getDefault().post(
                                        KMessageEvent(
                                                SPICE_CONNECT_FAILURE,
                                                resources.getString(R.string.error_connection_interrupted)
                                        )
                                )
                            }
                            //点击断开连接，返回得失败
                            sc.isClickDisconnect -> {
                                //Log.d("MessageEvent", "eventBus:点击导致得断开连接，返回得失败 ")
                            }
                            //连接时，返回得连接失败,  注意：需要区分两次连接导致得连接失败，还是配置参数导致得失败
                            else -> {
                                //Log.d("MessageEvent", "eventBus: 连接失败,无法连接或认证ca主题")
                                if (connectFailCount == 3) {
                                    isConnectFail = true
                                }
                                if (connectFailCount < 3) {
                                    ++connectFailCount
                                    //Log.d(TAG, "so库问题，连接时，偶发认证失败，失败后重连,重连次数$connectFailCount")
                                    enabledConnectSpice()
                                } else {
                                    connectFailCount = 0
                                    EventBus.getDefault().post(
                                            KMessageEvent(
                                                    SPICE_CONNECT_FAILURE,
                                                    resources.getString(R.string.error_spice_unable_to_connect)
                                            )
                                    )
                                    //Log.d(TAG, "第二次连接失败: ")
                                }
                            }
                        }
                    }
                }
            }
            false
        })
    }

    private fun initSpice(context: Context) {
        spiceCommunicator = SpiceCommunicator(context.applicationContext)

        val isc = object : ISpiceConnect {
            override fun onUpdateBitmapWH(width: Int, height: Int) {
                //Log.d(TAG, "onUpdateBitmapWH: $width,$height")
                //返回宽等于配置宽
                if (width == KSpice.readKeyInInt(
                                context,
                                RESOLUTION_WIDTH
                        ) && height == KSpice.readKeyInInt(
                                context,
                                RESOLUTION_HEIGHT
                        ) || width == bitmapWidth && width == bitmapHeight
                ) {
                    reallocateDrawable(width, height)
                } else {
                    //返回宽为键盘宽度
                    reallocateDrawable(width, height)
                    if (bitmapWidth != 0 && bitmapHeight != 0) {
                        updateSpiceResolvingPower(bitmapWidth, bitmapHeight)
                    } else {
                        recoverySpiceResolvingPower()
                    }
                }
            }

            override fun onUpdateBitmap(x: Int, y: Int, width: Int, height: Int) {
                //Log.d(TAG, "onUpdateBitmap: $x,$y,$width,$height")
                postInvalidate()
            }

            override fun onMouseUpdate(x: Int, y: Int) {
                //Log.d(TAG, "onMouseUpdate: $x,$y")
                postInvalidate()
            }

            override fun onMouseMode(relative: Boolean) {
                //Log.d(TAG, "onMouseMode: ")
            }

            override fun onConnectSucceed() {
                //Log.d(TAG, "onConnectSucceed: ")
                context.sendBroadcast(Intent(ACTION_SPICE_CONNECT_SUCCEED))
                myHandler?.removeMessages(SPICE_CONNECT_TIMEOUT)
                myHandler?.removeMessages(SPICE_CONNECT_FAILURE)
                EventBus.getDefault().post(KMessageEvent(KMessageEvent.SPICE_CONNECT_SUCCESS))
                myHandler?.post { updateMouseMode() }
            }


            override fun onConnectFail() {
                Log.d(TAG, "onConnectFail: ")
                myHandler?.removeMessages(SPICE_CONNECT_TIMEOUT)
                myHandler?.sendEmptyMessage(SPICE_CONNECT_FAILURE)

            }
        }

        /**
         * 原生方法得接口回调
         */
        spiceCommunicator?.setSpiceConnect(isc)
        myHandler?.postDelayed({ enabledConnectSpice() }, 1000)
    }

    /**
     * 开始连接
     */
    private fun enabledConnectSpice() {
        //IO 线程里拉取数据
        myHandler?.sendEmptyMessageDelayed(SPICE_CONNECT_TIMEOUT, 5 * 1000)
        scope?.get()?.cancel()
        scope = WeakReference(GlobalScope.launch {
            Log.d(TAG, "启动Spice连接线程: ${Thread.currentThread().name}")
            spiceCommunicator?.connectSpice(
                    KSpice.readKeyInString(context, KSpice.IP),
                    KSpice.readKeyInString(context, KSpice.PORT),
                    KSpice.readKeyInString(context, KSpice.TPort),
                    KSpice.readKeyInString(context, KSpice.PASSWORD),
                    KSpice.readKeyInString(context, KSpice.CF),
                    null,
                    "",
                    KSpice.readKeyInBoolean(context, KSpice.SOUND)
            )
        })
    }

    /**
     * 更新鼠标操作模式
     */
    fun updateMouseMode() {
        if (KSpice.readKeyInString(
                        context,
                        MOUSE_MODE
                ) == KSpice.Companion.MouseMode.MODE_CLICK.toString()
        ) {
            ktvMouse = KTVMouse(context.applicationContext, this@KRemoteCanvas)
        } else if (KSpice.readKeyInString(
                        context,
                        MOUSE_MODE
                ) == KSpice.Companion.MouseMode.MODE_TOUCH.toString()
        ) {
            ktvMouse =
                    KMobileMouse(context.applicationContext, this@KRemoteCanvas, this@KRemoteCanvas)
        }
        invalidate()
    }

    /**
     * 调整为键盘显示时的分辨率
     */
    private fun updateSpiceResolvingPower(width: Int, height: Int) {
        bitmapWidth = width
        bitmapHeight = height
        setBitmapConfig(width, height)
    }

    /**
     * 恢复默认配置分辨率
     */
    private fun recoverySpiceResolvingPower() {
        bitmapWidth = 0
        bitmapHeight = 0
        setBitmapConfig(
                KSpice.readKeyInInt(context, RESOLUTION_WIDTH),
                KSpice.readKeyInInt(context, RESOLUTION_HEIGHT)
        )
    }

    /**
     * 更新软键盘高度
     */
    fun updateKeyboardHeight(keyboardHeight: Int) {
        if (KSpice.readKeyInBoolean(context, KSpice.IS_ADJUST)) {
            if (keyboardHeight == 0) {
                recoverySpiceResolvingPower()
            } else {
                updateSpiceResolvingPower(width, height - keyboardHeight)
            }
        } else {
            //恢复画布原有位置
            if (keyboardHeight == 0) {
                checkCanvasOutRange()
//                computeMatrixCenter(this.width, this.height, KSpice.readKeyInInt(context, RESOLUTION_WIDTH), KSpice.readKeyInInt(context, RESOLUTION_HEIGHT))
            }
            this.keyboardHeight = keyboardHeight
        }
    }

    /**
     * 设置bitmap显示宽高
     */
    private fun setBitmapConfig(width: Int, height: Int) {
        spiceCommunicator?.SpiceRequestResolution(width, height)
    }

    /**
     * 计算matrix居中
     */
    private fun computeMatrixCenter(sw: Int, sh: Int, pw: Int, ph: Int) {
        dx = (sw - pw) / 2
        dy = (sh - ph) / 2
        bitmapMatrix.setTranslate(dx.toFloat(), dy.toFloat())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
//        Log.d(TAG, "onSizeChanged: $w,$h")
        if (KSpice.readKeyInInt(context, RESOLUTION_WIDTH) == 0 || KSpice.readKeyInInt(
                        context,
                        RESOLUTION_HEIGHT
                ) == 0
        ) {
            KSpice.writeValueToKey(context, RESOLUTION_WIDTH, w)
            KSpice.writeValueToKey(context, RESOLUTION_HEIGHT, h)
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
        drawable = WeakReference(KCanvasDrawable(width, height))
        spiceCommunicator?.setBitmap(canvasBitmap)
        //如果键盘矩阵为0时，调整矩阵位置
        if (bitmapWidth == 0 && bitmapHeight == 0) {
            computeMatrixCenter(this.width, this.height, width, height)
        } else {
            computeMatrixCenter(bitmapWidth, bitmapHeight, width, height)
        }

        //初始化鼠标
        initCursor()
        post { setImageDrawable(drawable?.get()) }
    }

    /**
     * 初始化鼠标
     */
    private fun initCursor() {
        if (KSpice.readKeyInBoolean(context, SYSTEM_RUN_ENV)) {
            cursorBitmap = BitmapFactory.decodeResource(resources, R.mipmap.cursor)
            cursorMatrix = Matrix()
            cursorBitmap?.let {
//                Log.d(TAG, "initCursor: 初始化鼠标")
                cursorMatrix?.setScale(1.8f, 1.8f, it.width / 2f, it.height / 2f)
                cursorBitmap =
                        Bitmap.createBitmap(it, 0, 0, it.width, it.height, cursorMatrix, false)
            }
        }
    }

    /**
     * 监听鼠标按下时得移动
     */
    /* @SuppressLint("ClickableViewAccessibility")
     override fun onTouchEvent(event: MotionEvent): Boolean {
         ktvMouse?.onTouchEvent(event, dx, dy)
         return true
     }*/

    /**
     * 鼠标移动、按下、松开、中间键滚动
     */
    /*override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        ktvMouse?.let {
            return it.onTouchEvent(event, dx, dy)
        }
        return super.onGenericMotionEvent(event)
    }*/

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
     * 鼠标按下移动,TV的鼠标按下、手机端的点击模式，触屏按下移动时坐标
     */
    override fun mouseDownMove(x: Int, y: Int, metaState: Int, mouseType: Int, isMove: Boolean) {
        computeTvMouseBoundaryPoint(x, y)
//        computerMouseCriticalPoint(x, y)
//        checkCanvasOutOfRange(x, y)
        spiceCommunicator?.writePointerEvent(
                x,
                y,
                metaState,
                mouseType or POINTER_DOWN_MASK,
                isMove
        )
    }

    /**
     * 鼠标移动，TV端的鼠标直接移动坐标、手机端的触摸模式，即触摸移动坐标
     * 该x坐标时相对view得坐标
     */
    override fun mouseMove(x: Int, y: Int, metaState: Int, mouseType: Int, isMove: Boolean) {
        computeTvMouseBoundaryPoint(x, y)
//        computerMouseCriticalPoint(x, y)
//        checkCanvasOutOfRange(x, y)
        spiceCommunicator?.writePointerEvent(x, y, metaState, mouseType, isMove)
        postInvalidate()
    }


    /**
     * 隐藏软键盘或缩放时检测
     */
    private fun checkCanvasOutRange() {
        getHitRect(viewRect)
        if (viewRect.left > 0) {
            translationX -= viewRect.left
        }
        if (viewRect.top > 0) {
            translationY -= viewRect.top
        }
        if (viewRect.right < width) {
            translationX += width - viewRect.right
        }
        if (viewRect.bottom < height) {
            translationY += height - viewRect.bottom
        }
        //TV端偏移后，关闭软键盘需要重置
        dy = 0
    }


    /**
     * 检测画布是否超出范围，出现多余黑屏部分
     * 根据范围和鼠标坐标双重判断，增加精确性
     */
    private fun checkCanvasOutOfRange(x: Int, y: Int) {
        getHitRect(viewRect)
        if (viewRect.left > 0 && x * scaleFactor <= abs(viewRect.left)) {
            Log.d(TAG, "mouseMove: 左侧超出范围")
            translationX -= viewRect.left
        } else if (viewRect.right - viewRect.left > width * scaleFactor && x * scaleFactor > width - 1 + abs(viewRect.left)) {
            Log.d(TAG, "mouseMove: 右侧超出范围")
            translationX += viewRect.right - width * scaleFactor
        } else if (viewRect.top > 0 && y * scaleFactor <= abs(viewRect.top)) {
            Log.d(TAG, "mouseMove: 顶部超出范围")
            translationY -= viewRect.top
        } else if (viewRect.bottom < height - keyboardHeight && y == height - 1 - keyboardHeight) {
            Log.d(TAG, "mouseMove: 底部超出范围")
            translationY += height - viewRect.bottom
        }
    }


    /**
     * 计算鼠标临界点
     */
    private fun computerMouseCriticalPoint(x: Int, y: Int) {
        getHitRect(viewRect)
        // TODO: 2020/8/13 1、适配电视上的1：1缩放，打开键盘时的临界点，2、鼠标临界点细节优化
        /*
          var cursorWidth =0
          var cursorHeight=0
          cursorBitmap?.width?.let {
              cursorWidth=it
          }
          cursorBitmap?.height?.let {
              cursorHeight=it
          }*/
        if (viewRect.left < 0 && x * scaleFactor <= abs(viewRect.left) + 10 /*+ cursorWidth*/) {
            Log.d(TAG, "mouseMove: 在左侧边界范围")
            translationX += canvasSingleDy
        } else if (viewRect.right > width && x * scaleFactor >= width - 1 + abs(viewRect.left) - 10 /*- cursorWidth*/) {
            Log.d(TAG, "mouseMove: 在右侧边界范围")
            translationX -= canvasSingleDy
        } else if (viewRect.top < 0 && y * scaleFactor <= abs(viewRect.top) + 10/* + cursorHeight*/) {
            Log.d(TAG, "mouseMove: 在顶部边界范围")
            translationY += canvasSingleDy
        } else if (viewRect.bottom > height - keyboardHeight && y * scaleFactor >= height + abs(viewRect.top) - keyboardHeight - 10) {
            Log.d(TAG, "mouseMove: 在底部边界范围")
            translationY -= canvasSingleDy
        }
    }

    /**
     * 计算Tv端鼠标临界点
     */
    private fun computeTvMouseBoundaryPoint(x: Int, y: Int) {
        Log.i(TAG, "computeTvMouseBoundaryPoint: $x,$y,$dy,$canvasMaxDy")
        getHitRect(viewRect)
        if (keyboardHeight > 0 && y > height - keyboardHeight - 10) {
            canvasMaxDy = (translationY - canvasSingleDy).toInt()
            if (abs((canvasMaxDy)) > keyboardHeight) {
                canvasMaxDy = -keyboardHeight
            }
            dy = canvasMaxDy
            translationY = canvasMaxDy.toFloat()
        } else if (keyboardHeight > 0 && y + dy < 10) {
            canvasMaxDy = (translationY + canvasSingleDy).toInt()
            if (canvasMaxDy > 0) {
                canvasMaxDy = 0
                Log.i(TAG, "computeTvMouseBoundaryPoint: 超出了")
            }
            dy = canvasMaxDy
            translationY = canvasMaxDy.toFloat()
        }
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
        if (isDown) ktvMouse?.downMouseRightButton() else ktvMouse?.upMouseRightButton()
    }

    /**
     * 发送键盘key
     */
    fun sendKey(code: Int, down: Boolean) {
        spiceCommunicator?.sendSpiceKeyEvent(down, code)
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
        //Log.d(TAG, "translationBeforeLimit: 平移之前校验getHitRect：$viewRect")
        return viewRect.left == 0 && viewRect.top == 0 && viewRect.right == width && viewRect.bottom == height
    }

    override fun translationAfterLimit() {
        getGlobalVisibleRect(visibleRect)//在屏幕显示区域Rect
        //Log.d(TAG, "translationLimit: 平移之后校验：$visibleRect")
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
        KSpice.writeValueToKey(context, RESOLUTION_WIDTH, 0)
        KSpice.writeValueToKey(context, RESOLUTION_HEIGHT, 0)
    }

    companion object {
        private const val TAG = "KRemoteCanvas"
    }
}