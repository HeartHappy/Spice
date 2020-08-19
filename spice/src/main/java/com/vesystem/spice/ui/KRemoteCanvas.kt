package com.vesystem.spice.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
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
import com.vesystem.spice.model.KSpice.ACTION_SPICE_CONNECT_SUCCEED
import com.vesystem.spice.model.KSpice.MOUSE_MODE
import com.vesystem.spice.model.KSpice.RESOLUTION_HEIGHT
import com.vesystem.spice.model.KSpice.RESOLUTION_WIDTH
import com.vesystem.spice.model.KSpice.SYSTEM_RUN_ENV
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
    internal var canvasBitmap: Bitmap? = null
    private var cursorBitmap: Bitmap? = null
    private var myHandler: Handler? = null
    internal var ktvMouse: KMouse? = null
    private var bitmapMatrix: Matrix = Matrix()
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var cursorMatrix: Matrix? = null
    private var isConnectFail = false
    private var connectFailCount = 0

    //    internal var dx = 0//画布距离屏幕偏移量
//    internal var dy = 0
    private var visibleRect = Rect()
    private var viewRect = Rect()
    private var keyboardHeight = 0
    private var singleOffset = 10//鼠标到达临界点时，view单次偏移量
    private var viewTranslationY = 0//view y轴 偏移量
    private var viewTranslationX = 0//view x轴 偏移量
    private var canvasTranslationX = 0 //canvas x轴 偏移
    private var canvasTranslationY = 0 //canvas y轴 偏移
    internal var scaleFactor: Float = 1f
        set(value) {
            if (value >= 1f) {
                Log.d(TAG, "缩放值:$value ")
                if (value == 1f && field > 1) {
                    KToast.show(context, "缩放比例1:1")
                }
                field = value
                //防止在1：1在缩放时，会检测
                if (field > 1) {
                    checkCanvasOutRange()
                }
            }
        }

    init {
//        setBackgroundColor(Color.BLUE)
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
                Log.d(TAG, "onUpdateBitmapWH: $width,$height")
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
                Log.d(TAG, "onMouseUpdate: $x,$y")
                postInvalidate()
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
            ) == KSpice.MouseMode.MODE_CLICK.toString()
        ) {
            ktvMouse = KTVMouse(context.applicationContext, this@KRemoteCanvas)
        } else if (KSpice.readKeyInString(
                context,
                MOUSE_MODE
            ) == KSpice.MouseMode.MODE_TOUCH.toString()
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
                recoveryDefaultScale()
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

    fun recoveryDefaultScale() {
        scaleX = 1f
        scaleY = 1f
        scaleFactor = 1f
        checkCanvasOutRange()
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
        //如果bitmap得画面宽大于屏幕宽，则计算比例，按照比例对画面进行缩放
        /*dx = (sw - pw) / 2
        dy = (sh - ph) / 2
        if (dx < 0) dx = 0
        if (dy < 0) dy = 0
        Log.d(
            TAG,
            "computeMatrixCenter: sw:$sw,sh:$sh,pw:$pw,ph:$ph,dx:$dx,dy:$dy，BW:$bitmapWidth,BH:$bitmapHeight"
        )
        bitmapMatrix.setTranslate(dx.toFloat(), dy.toFloat())*/
//        cursorMatrix?.postTranslate(dx.toFloat(), dy.toFloat())
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
                // TODO: 2020/8/14 鼠标临界点细节优化
                cursorMatrix?.let { cm ->
                    ktvMouse?.let { mouse ->
                        cursorMatrix?.setTranslate(
                            mouse.mouseX.toFloat(),
                            mouse.mouseY.toFloat()
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
        Log.i(TAG, "reallocateDrawable: 显示得$width,$height")
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
            //处理画面不为全屏，并且超出屏幕时需要处理鼠标范围
            /*canvasBitmap?.let {
                if (it.width > this.width || it.height > this.height) {
                    ktvMouse?.mouseMaxX = it.width
                    ktvMouse?.mouseMaxY = it.height
                }
            }*/
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
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val onTouchEvent = ktvMouse?.onTouchEvent(event, canvasTranslationX, canvasTranslationY)
        onTouchEvent?.let {
            return it
        }
        return super.onTouchEvent(event)
    }

    /**
     * 鼠标移动、按下、松开、中间键滚动
     */
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        ktvMouse?.let {
            return it.onTouchEvent(event, canvasTranslationX, canvasTranslationY)
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
     * 鼠标按下移动,TV的鼠标按下、手机端的点击模式，触屏按下移动时坐标
     */
    override fun mouseDownMove(x: Int, y: Int, metaState: Int, mouseType: Int, isMove: Boolean) {
        computeMouseBoundaryPoint(x, y)
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
        Log.d(TAG, "mouseMove: ${x},$y")
        computeMouseBoundaryPoint(x, y)
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
    }


    /**
     * 计算鼠标边界点和超出范围限制
     */
    @Suppress("DEPRECATION")
    private fun computeMouseBoundaryPoint(x: Int, y: Int) {
        getHitRect(viewRect)//缩放后或偏移后rect坐标
        canvasBitmap?.let {
            when {
                //如果画面与view宽高相同即全屏情况下。 并且缩放比例为1，键盘高度显示情况下进行偏移
                scaleFactor == 1f && width == it.width && height == it.height && keyboardHeight > 0 -> {
                    Log.d(TAG, "computeTvMouseBoundaryPoint: 全屏模式且画面等同与屏幕像素")
                    //画面全屏模式下，根据画面超出view范围和边界点进行偏移

                    when {
                        //view上移
                        y > height - keyboardHeight - 10 -> {
                            viewTranslationY = (translationY - singleOffset).toInt()
                            if (abs(viewTranslationY) > keyboardHeight) {
                                viewTranslationY = -keyboardHeight
                            }
                            translationY = viewTranslationY.toFloat()
                        }
                        //view往下移
                        it.height > height - keyboardHeight && y + viewTranslationY < 10 -> {
                            viewTranslationY = (translationY + singleOffset).toInt()
                            if (viewTranslationY > 0) {
                                viewTranslationY = 0
                            }
                            translationY = viewTranslationY.toFloat()
                        }
                        else -> {
                            Log.d(TAG, "computeTvMouseBoundaryPoint: 必须else")
                        }
                    }

                }
                //缩放情况下。根据View超出屏幕范围和边界点进行偏移，注意：+、-singleOffset在触屏点击模式下需要偏移画面时更加精确
                scaleFactor > 1f && width == it.width && height == it.height && viewRect.right - viewRect.left > width && viewRect.bottom - viewRect.top > height -> {
                    Log.d(TAG, "computeTvMouseBoundaryPoint: 缩放模式且画面等同与屏幕像素$x,$y,rect:$viewRect,scale:$scaleFactor")
                    //如果边缘出还有界面并且鼠标位置在边界处
                    when {
                        //左侧边界，往右移动
                        viewRect.left < 0 && x in 0..(abs(viewRect.left) / scaleFactor).toInt()+singleOffset -> {
                            Log.d(TAG, "computeTvMouseBoundaryPoint: 左侧边界，右移")
                            viewTranslationX = (translationX + singleOffset).toInt()
                            if (abs(viewTranslationX) > (viewRect.right - viewRect.left - width) / 2) {
                                viewTranslationX = (viewRect.right - viewRect.left - width) / 2
                            }
                            translationX = viewTranslationX.toFloat()
                        }
                        viewRect.top < 0 && y in 0..(abs(viewRect.top) / scaleFactor).toInt()+singleOffset -> {
                            Log.d(TAG, "computeTvMouseBoundaryPoint: 顶部边界，下移")
                            viewTranslationY = (translationY + singleOffset).toInt()
                            if (viewTranslationY > (viewRect.bottom - viewRect.top - height) / 2) {
                                viewTranslationY = (viewRect.bottom - viewRect.top - height) / 2
                            }
                            translationY = viewTranslationY.toFloat()
                        }
                        viewRect.right > width && x in (width - viewRect.left)/ scaleFactor-singleOffset..viewRect.right.toFloat() -> {
                            Log.d(TAG, "computeTvMouseBoundaryPoint: 右侧边界，左移")
                            viewTranslationX = (translationX - singleOffset).toInt()
                            if (abs(viewTranslationX) > (viewRect.right - viewRect.left - width) / 2) {
                                viewTranslationX = -(viewRect.right - viewRect.left - width) / 2
                            }
                            translationX = viewTranslationX.toFloat()
                        }
                        viewRect.bottom > height - keyboardHeight && y >= (height - keyboardHeight - viewRect.top)/ scaleFactor -singleOffset -> {
                            viewTranslationY = (translationY - singleOffset).toInt()
                            if (abs(viewTranslationY) > (viewRect.bottom - viewRect.top - height) / 2 + keyboardHeight) {
                                viewTranslationY =
                                    -((viewRect.bottom - viewRect.top - height) / 2 + keyboardHeight)
                                Log.i(TAG, "computeTvMouseBoundaryPoint: 底部越界值${viewTranslationY}")
                            }
                            translationY = viewTranslationY.toFloat()
                            Log.d(
                                TAG,
                                "computeTvMouseBoundaryPoint: 底部边界，键盘是否打开:${keyboardHeight > 0}，上移"
                            )
                        }
                        else -> {
                            Log.d(TAG, "computeTvMouseBoundaryPoint: 必须else")
                        }
                    }
                }
                width != it.width && height != it.height -> {
                    Log.i(TAG, "computeTvMouseBoundaryPoint: ${it.width},${it.height}")
                    when {
                        it.width > width && x >= width - 1 -> {
                            Log.d(TAG, "computeTvMouseBoundaryPoint: 屏幕右侧边界点")
                            canvasTranslationX -= singleOffset
                            if (abs(canvasTranslationX) > it.width - width) {
                                canvasTranslationX = -(it.width - width)
                            }
                            bitmapMatrix.setTranslate(canvasTranslationX.toFloat(), 0f)
                        }
                        it.width > width && x <= 1 -> {
                            canvasTranslationX += singleOffset
                            if (canvasTranslationX > 0) {
                                canvasTranslationX = 0
                            }
                            bitmapMatrix.setTranslate(canvasTranslationX.toFloat(), 0f)
                            Log.d(TAG, "computeTvMouseBoundaryPoint: 屏幕左侧边界点")
                        }
                        else -> Log.d(TAG, "computeTvMouseBoundaryPoint: ")
                    }
                }
                else -> {
                    Log.d(TAG, "computeTvMouseBoundaryPoint: 必须else")
                }
            }
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