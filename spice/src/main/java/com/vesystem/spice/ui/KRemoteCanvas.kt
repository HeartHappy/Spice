package com.vesystem.spice.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.vesystem.opaque.SpiceCommunicator
import com.vesystem.spice.R
import com.vesystem.spice.bitmap.KCanvasDrawable
import com.vesystem.spice.interfaces.ISpiceConnect
import com.vesystem.spice.interfaces.IViewable
import com.vesystem.spice.model.KMessageEvent
import com.vesystem.spice.model.KMessageEvent.Companion.SPICE_ADJUST_RESOLVING
import com.vesystem.spice.model.KMessageEvent.Companion.SPICE_ADJUST_RESOLVING_PASSIVE_TIMEOUT
import com.vesystem.spice.model.KMessageEvent.Companion.SPICE_ADJUST_RESOLVING_SUCCEED
import com.vesystem.spice.model.KMessageEvent.Companion.SPICE_ADJUST_RESOLVING_TIMEOUT
import com.vesystem.spice.model.KMessageEvent.Companion.SPICE_CONNECT_FAILURE
import com.vesystem.spice.model.KMessageEvent.Companion.SPICE_CONNECT_TIMEOUT
import com.vesystem.spice.model.KSpice
import com.vesystem.spice.model.KSpice.ACTION_SPICE_CONNECT_SUCCEED
import com.vesystem.spice.model.KSpice.MOUSE_MODE
import com.vesystem.spice.model.KSpice.SYSTEM_RUN_ENV
import com.vesystem.spice.mouse.IMouseOperation
import com.vesystem.spice.mouse.KMobileMouse
import com.vesystem.spice.mouse.KMouse
import com.vesystem.spice.mouse.KMouse.Companion.POINTER_DOWN_MASK
import com.vesystem.spice.mouse.KTVMouse
import com.vesystem.spice.utils.KToast
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
    IViewable, IMouseOperation {
    private var spiceCommunicator: SpiceCommunicator? = null//远程连接
    private var drawable: WeakReference<Drawable>? = null//渲染bitmap
    private var scope: WeakReference<Job>? = null
    internal var canvasBitmap: Bitmap? = null
    private var cursorBitmap: Bitmap? = null
    private var myHandler: Handler? = null
    private var ktvMouse: KMouse? = null
    private var bitmapMatrix: Matrix = Matrix()
    private var adjustWidth = 0 //调整为键盘显示时的宽度
    private var adjustHeight = 0
    internal var screenWidth = 0//屏幕宽或者为配置的宽高
    internal var screenHeight = 0
    private var responseWidth = 0 //响应的宽高
    private var responseHeight = 0
    private var cursorMatrix: Matrix? = null
    private var isConnectFail = false //是否连接失败
    private var isAdjustFullSucceed = false //是否已经调整到全屏
    private var connectFailCount = 0 //连接失败计数器，最大失败3次
    private var viewRect = Rect()
    private var keyboardHeight = 0
    private var singleOffset = 30//鼠标到达临界点时，view单次偏移量
    private var viewTranslationY = 0//view y轴 偏移量
    private var viewTranslationX = 0//view x轴 偏移量
    private var canvasTranslationX = 0 //canvas x轴 偏移
    private var canvasTranslationY = 0 //canvas y轴 偏移
    internal var scaleFactor: Float = 1f
        set(value) {
            if (value >= 1f) {
//                Log.d(TAG, "缩放值:$value ")
                if (value == 1f && field > 1) {
                    KToast.show(context, "缩放比例1:1")
                }
                field = value
                if (field > 1) {
                    checkCanvasOutRange()
                }
            }
        }

    init {
        setBackgroundColor(Color.BLACK)
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
                    sc?.let { sit ->
                        //如果时连接情况下，被断开，视为其他设备占用，导致断开连接
                        when {
                            //远程连接被断开
                            sit.isConnectSucceed -> {
                                //Log.d(TAG, "eventBus: 连接被断开")
                                EventBus.getDefault().post(
                                    KMessageEvent(
                                        SPICE_CONNECT_FAILURE,
                                        resources.getString(R.string.error_connection_interrupted)
                                    )
                                )
                            }
                            //点击断开连接，返回得失败
                            sit.isClickDisconnect -> {
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
                SPICE_ADJUST_RESOLVING_TIMEOUT -> {
                    //如果还没有切换到全屏，自动调整到适用分辨率
                    KToast.show(context, "系统为您调整到适用分辨率")
                    EventBus.getDefault().post(
                        KMessageEvent(
                            SPICE_ADJUST_RESOLVING_TIMEOUT,
                            false
                        )
                    )
                }
                SPICE_ADJUST_RESOLVING_PASSIVE_TIMEOUT -> {
                    //如果已经全屏过，但是被动调整分辨率，需要检测
                    startConnect()
                    checkResolvingIsFullScreenFromPassive(screenWidth, screenHeight)
                }
            }
            false
        })
    }

    private fun initSpice(context: Context) {
        spiceCommunicator = SpiceCommunicator(context.applicationContext)
        /**
         * 原生方法得接口回调
         */
        spiceCommunicator?.setSpiceConnect(object : ISpiceConnect {
            override fun onUpdateBitmapWH(width: Int, height: Int) {
                responseWidth = width
                responseHeight = height
                when {
                    //1、等同于配置宽高
                    width == screenWidth && height == screenHeight -> {
//                        Log.d(TAG, "onUpdateBitmapWH: 调屏为全屏宽高")
                        reallocateDrawable(width, height, false)
                    }
                    //2、等同于键盘宽高
                    width == adjustWidth && height == adjustHeight -> {
//                        Log.d(TAG, "onUpdateBitmapWH: 调屏为键盘显示宽高")
                        reallocateDrawable(width, height, false)
                    }
                    //3、都不是,直接获取的结果。自动调整为默认配置分辨率
                    else -> {
                        //被重启了，虚拟机导致的自动更新分辨率
                        if (isAdjustFullSucceed && width != screenWidth && height != screenHeight) {
                            /*Log.d(
                                TAG,
                                "onUpdateBitmapWH: 分辨率被动调整为不全屏了，调整分辨率，并且显示当前画面，不提示，不更新鼠标$screenWidth,$screenHeight"
                            )*/
                            reallocateDrawable(width, height, true)
                            updateResolutionToPassive(screenWidth, screenHeight)
                            updateMouseMode()
                        } else {
                            /*Log.d(
                                TAG,
                                "onUpdateBitmapWH: 都不是,直接获取的结果。自动调整为默认配置分辨率:请求得宽高：$screenWidth,$screenHeight,响应得宽高：${responseWidth},${responseHeight}"
                            )*/
                            EventBus.getDefault().post(KMessageEvent(SPICE_ADJUST_RESOLVING))
                            updateResolutionToDefault(screenWidth, screenHeight)
                        }
                    }
                }
            }

            override fun onUpdateBitmap(x: Int, y: Int, width: Int, height: Int) {
                postInvalidate()
            }

            override fun onMouseUpdate(x: Int, y: Int) {
                postInvalidate()
            }

            override fun onConnectSucceed() {
                myHandler?.post { updateMouseMode() }
                myHandler?.removeMessages(SPICE_CONNECT_TIMEOUT)
                myHandler?.removeMessages(SPICE_ADJUST_RESOLVING_TIMEOUT)
                myHandler?.sendEmptyMessage(SPICE_ADJUST_RESOLVING)
                context.sendBroadcast(Intent(ACTION_SPICE_CONNECT_SUCCEED))
                EventBus.getDefault().post(KMessageEvent(KMessageEvent.SPICE_CONNECT_SUCCESS))
            }

            override fun onConnectFail() {
                myHandler?.removeMessages(SPICE_CONNECT_TIMEOUT)
                myHandler?.sendEmptyMessage(SPICE_CONNECT_FAILURE)
            }
        })
        myHandler?.postDelayed({ enabledConnectSpice() }, 1000)
    }

    /**
     * 开始连接
     */
    private fun enabledConnectSpice() {
        //IO 线程里拉取数据
        myHandler?.removeMessages(SPICE_CONNECT_TIMEOUT)
        myHandler?.sendEmptyMessageDelayed(SPICE_CONNECT_TIMEOUT, 5 * 1000)
        startConnect()
    }

    private fun startConnect() {
        scope?.get()?.cancel()
        scope = WeakReference(GlobalScope.launch {
//            Log.d(TAG, "启动Spice连接线程: ${Thread.currentThread().name}")
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
        if (KSpice.readKeyInString(context, MOUSE_MODE) == KSpice.MouseMode.MODE_CLICK.toString()) {
            ktvMouse = KTVMouse(context.applicationContext, this@KRemoteCanvas, width, height)
        } else if (KSpice.readKeyInString(
                context,
                MOUSE_MODE
            ) == KSpice.MouseMode.MODE_TOUCH.toString()
        ) {
            ktvMouse = KMobileMouse(context.applicationContext, this@KRemoteCanvas, width, height)
        }
        postInvalidate()
    }

    /**
     * 更新分辨率为软键盘显示时调屏
     */
    private fun updateResolutionToKeyboard(width: Int, height: Int) {
        adjustWidth = width
        adjustHeight = height
        setBitmapConfig(width, height)
    }


    /**
     * 更新分辨率为默认配置
     */
    private fun updateResolutionToDefault(w: Int, h: Int) {
        /*Log.d(
            TAG,
            "updateResolutionToDefault: 更新为默认分辨率,请求宽高：$w,$h,响应宽高：$responseWidth,$responseHeight"
        )*/
        adjustWidth = 0
        adjustHeight = 0
        screenWidth = w
        screenHeight = h
        setBitmapConfig(w, h)
        //4、发送调整为默认配置，超时未响应
        myHandler?.removeMessages(SPICE_ADJUST_RESOLVING_TIMEOUT)
        myHandler?.sendEmptyMessageDelayed(SPICE_ADJUST_RESOLVING_TIMEOUT, 2000)
    }

    /**
     * 被动调整分辨率
     */
    private fun updateResolutionToPassive(w: Int, h: Int) {
        setBitmapConfig(w, h)
        myHandler?.removeMessages(SPICE_ADJUST_RESOLVING_PASSIVE_TIMEOUT)
        myHandler?.sendEmptyMessageDelayed(SPICE_ADJUST_RESOLVING_PASSIVE_TIMEOUT, 5000)
    }


    /**
     * 检测分辨率是否全屏，被动调整时调用
     */
    private fun checkResolvingIsFullScreenFromPassive(w: Int, h: Int) {
        if (w != responseWidth && h != responseHeight) {
            updateResolutionToPassive(screenWidth, screenHeight)
        }
    }


    /**
     * 检测分辨率是否全屏，切换横竖屏时调用
     */
    private fun checkResolvingIsFullScreen(w: Int, h: Int) {
        if (w == responseWidth && h == responseHeight) {
            reallocateDrawable(w, h, false)
            //解决直接显示时，画面没有出来，直接更新so库得画面
            spiceCommunicator?.UpdateBitmap(canvasBitmap, 0, 0, w, h)
        } else {
            //调整全屏分辨率没有成功，情况需要提示，成功后，在调整是被动调整。
            EventBus.getDefault().post(KMessageEvent(SPICE_ADJUST_RESOLVING))
            updateResolutionToDefault(w, h)
        }
        updateMouseMode()
    }

    /**
     * 更新软键盘高度
     */
    fun updateKeyboardHeight(keyboardHeight: Int) {
        if (KSpice.readKeyInBoolean(context, KSpice.IS_ADJUST)) {
            if (keyboardHeight == 0) {
                //恢复默认分辨率
                EventBus.getDefault().post(KMessageEvent(SPICE_ADJUST_RESOLVING))
                updateResolutionToDefault(screenWidth, screenHeight)
            } else {
                //更新为软键盘显示时的分辨率
                updateScaleToDefault()
                EventBus.getDefault().post(KMessageEvent(SPICE_ADJUST_RESOLVING))
                updateResolutionToKeyboard(width, height - keyboardHeight)
            }
        } else {
            //恢复画布原有位置
            if (keyboardHeight == 0) {
                checkCanvasOutRange()
            }
            this.keyboardHeight = keyboardHeight
        }
    }

    /**
     * 更新缩放为默认1：1
     */
    fun updateScaleToDefault() {
        scaleX = 1f
        scaleY = 1f
        scaleFactor = 1f
        checkCanvasOutRange()
    }

    /**
     * 设置bitmap显示宽高
     */
    private fun setBitmapConfig(width: Int, height: Int) {
//        enabledConnectSpice(20*1000)
//        EventBus.getDefault().post(KMessageEvent(SPICE_ADJUST_RESOLVING))
        spiceCommunicator?.SpiceRequestResolution(width, height)
    }

    /**
     * 计算matrix居中
     */
    private fun computeMatrixCenter(sw: Int, sh: Int, pw: Int, ph: Int) {
        //如果bitmap得画面宽大于屏幕宽，则计算比例，按照比例对画面进行缩放
        canvasTranslationX = (sw - pw) / 2
        canvasTranslationY = (sh - ph) / 2
        bitmapMatrix.setTranslate(canvasTranslationX.toFloat(), canvasTranslationY.toFloat())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        //切换横竖屏时，重置
        isAdjustFullSucceed = false
        spiceCommunicator?.isConnectSucceed?.let {
            if (it) {
                //解决切换横屏后更新分辨率时，当前屏幕宽高已经等同与响应得宽高，直接显示
                checkResolvingIsFullScreen(w, h)
            }
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
                            mouse.mouseX.toFloat() + canvasTranslationX,
                            mouse.mouseY.toFloat() + canvasTranslationY
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
    override fun reallocateDrawable(width: Int, height: Int, isCanvasCenter: Boolean) {
//        Log.d(TAG, "reallocateDrawable: 显示宽高$width,$height,响应宽高：$responseWidth,$responseHeight")
        if (responseWidth == screenWidth && responseHeight == screenHeight) {
            isAdjustFullSucceed = true
        }
        myHandler?.removeMessages(SPICE_ADJUST_RESOLVING_TIMEOUT)
        EventBus.getDefault().post(KMessageEvent(SPICE_ADJUST_RESOLVING_SUCCEED))
        //创建bitmap
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvasBitmap?.setHasAlpha(false)
        drawable = WeakReference(KCanvasDrawable(width, height))
        spiceCommunicator?.setBitmap(canvasBitmap)
        //设置画面居中
        if (isCanvasCenter) {
            computeMatrixCenter(this.width, this.height, width, height)
        } else {
            computeMatrixCenter(0, 0, 0, 0)
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
//        Log.d(TAG, "computeMouseBoundaryPoint: $viewRect,$x,$y,$width,$height")
        canvasBitmap?.let {
            when {
                //canvas画面与view宽高相同即全屏情况下。 并且缩放比例为1，键盘高度显示情况下进行偏移
                scaleFactor == 1f && width == it.width && height == it.height && keyboardHeight > 0 -> {
                    /*Log.d(
                        TAG,
                        "computeMouseBoundaryPoint: 全屏模式且画面等同与屏幕像素$y,$height,$keyboardHeight,$viewRect"
                    )*/
                    //画面全屏模式下，根据画面超出view范围和边界点进行偏移
                    when {
                        //view上移
                        //it.height > height - keyboardHeight&& y > height - keyboardHeight - 10
                        y > height - keyboardHeight - viewRect.top - singleOffset -> {
//                            Log.d(TAG, "computeMouseBoundaryPoint: 全屏模式，view上移")
                            viewTranslationY = (translationY - singleOffset).toInt()
                            if (abs(viewTranslationY) > keyboardHeight) {
                                viewTranslationY = -keyboardHeight
                            }
                            translationY = viewTranslationY.toFloat()
                        }
                        //view往下移
                        y + viewTranslationY < singleOffset -> {
//                            Log.d(TAG, "computeMouseBoundaryPoint: 全屏模式，view下移")
                            viewTranslationY = (translationY + singleOffset).toInt()
                            if (viewTranslationY > 0) {
                                viewTranslationY = 0
                            }
                            translationY = viewTranslationY.toFloat()
                        }
                        else -> {
                        }
                    }
                }
                //缩放情况下。根据View超出屏幕范围和边界点进行偏移，注意：+、-singleOffset在触屏点击模式下需要偏移画面时更加精确
                scaleFactor > 1f && width == it.width && height == it.height && viewRect.right - viewRect.left > width && viewRect.bottom - viewRect.top > height -> {
                    /*Log.d(
                        TAG,
                        "computeMouseBoundaryPoint: 缩放模式且画面等同与屏幕像素$x,$y,rect:$viewRect,scale:$scaleFactor"
                    )*/
                    //如果边缘出还有界面并且鼠标位置在边界处
                    when {
                        //左侧边界，往右移动
                        viewRect.left < 0 && x in 0..(abs(viewRect.left) / scaleFactor).toInt() + singleOffset -> {
//                            Log.d(TAG, "computeMouseBoundaryPoint: 左侧边界，右移")
                            viewTranslationX = (translationX + singleOffset).toInt()
                            if (abs(viewTranslationX) > (viewRect.right - viewRect.left - width) / 2) {
                                viewTranslationX = (viewRect.right - viewRect.left - width) / 2
                            }
                            translationX = viewTranslationX.toFloat()
                        }
                        viewRect.top < 0 && y in 0..(abs(viewRect.top) / scaleFactor).toInt() + singleOffset -> {
//                            Log.d(TAG, "computeMouseBoundaryPoint: 顶部边界，下移")
                            viewTranslationY = (translationY + singleOffset).toInt()
                            if (viewTranslationY > (viewRect.bottom - viewRect.top - height) / 2) {
                                viewTranslationY = (viewRect.bottom - viewRect.top - height) / 2
                            }
                            translationY = viewTranslationY.toFloat()
                        }
                        viewRect.right > width && x in (width - viewRect.left) / scaleFactor - singleOffset..viewRect.right.toFloat() -> {
//                            Log.d(TAG, "computeMouseBoundaryPoint: 右侧边界，左移")
                            viewTranslationX = (translationX - singleOffset).toInt()
                            if (abs(viewTranslationX) > (viewRect.right - viewRect.left - width) / 2) {
                                viewTranslationX = -(viewRect.right - viewRect.left - width) / 2
                            }
                            translationX = viewTranslationX.toFloat()
                        }
                        viewRect.bottom > height - keyboardHeight && y >= (height - keyboardHeight - viewRect.top) / scaleFactor - singleOffset -> {
                            viewTranslationY = (translationY - singleOffset).toInt()
                            if (abs(viewTranslationY) > (viewRect.bottom - viewRect.top - height) / 2 + keyboardHeight) {
                                viewTranslationY =
                                    -((viewRect.bottom - viewRect.top - height) / 2 + keyboardHeight)
//                                Log.d(TAG, "computeMouseBoundaryPoint: 底部越界值${viewTranslationY}")
                            }
                            translationY = viewTranslationY.toFloat()
                            /*Log.d(
                                TAG,
                                "computeMouseBoundaryPoint: 底部边界，键盘是否打开:${keyboardHeight > 0}，上移"
                            )*/
                        }
                        else -> {
                        }
                    }
                }
                else -> {
//                    Log.d(TAG, "computeMouseBoundaryPoint: 不是任何模式下")
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
    }

    /*companion object {
        private const val TAG = "KRemoteCanvas"
    }*/
}