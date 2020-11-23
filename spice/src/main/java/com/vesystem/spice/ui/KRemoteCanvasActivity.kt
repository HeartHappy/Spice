@file:Suppress("DEPRECATION")

package com.vesystem.spice.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.PopupMenu
import com.vesystem.spice.R
import com.vesystem.spice.keyboard.KeyBoard
import com.vesystem.spice.keyboard.KeyBoard.Companion.KEY_WIN
import com.vesystem.spice.keyboard.KeyBoard.Companion.KEY_WIN_ALT
import com.vesystem.spice.keyboard.KeyBoard.Companion.KEY_WIN_CENTER_ENTER
import com.vesystem.spice.keyboard.KeyBoard.Companion.KEY_WIN_CTRL
import com.vesystem.spice.keyboard.KeyBoard.Companion.KEY_WIN_ESC
import com.vesystem.spice.keyboard.KeyBoard.Companion.KEY_WIN_SHIFT
import com.vesystem.spice.keyboard.KeyBoard.Companion.KEY_WIN_TAB
import com.vesystem.spice.keyboard.KeyBoard.Companion.SCANCODE_SHIFT_MASK
import com.vesystem.spice.keyboard.KeyBoard.Companion.UNICODE_MASK
import com.vesystem.spice.keyboard.KeyBoard.Companion.UNICODE_META_MASK
import com.vesystem.spice.model.KMessageEvent
import com.vesystem.spice.model.KSpice
import com.vesystem.spice.ui.interfaces.IPopBottomSoftKeyCallback
import com.vesystem.spice.ui.interfaces.SoftKeyBoardListener
import com.vesystem.spice.ui.pop.KPopBottomSoftKey
import com.vesystem.spice.utils.ViewOperateUtil
import com.vesystem.spice.zoom.ScaleGestureBinder
import com.vesystem.spice.zoom.ScaleGestureListener
import kotlinx.android.synthetic.main.activity_remote_canvas.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


/**
 * Created Date 2020/7/6.
 * @author ChenRui
 * ClassDescription:远程界面
 * 负责处理：
 * 1、加载自定义View
 * 2、处理其他view的事件
 * 3、键盘的交互
 * 4、鼠标的处理
 * 5、缩放
 * 6、系统软键盘+特殊键盘+自定义键盘
 */
class KRemoteCanvasActivity : Activity() {
    private var keyBoard: KeyBoard? = null //自定义物理键盘
    private var dialog: ProgressDialog? = null //连接时的加载dialog
    private var alertDialog: AlertDialog? = null //连接信息提示
    private var popupMenu: PopupMenu? = null//右下角菜单弹出列表Pop
    private var popBottomSoftKey: KPopBottomSoftKey? = null//自定义键盘Pop
    private var keyBoardHeight: Int = 0
    private var scaleGestureListener: ScaleGestureListener? = null
    private var scaleGestureBinder: ScaleGestureBinder? = null
    var systemRunningEnv = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_remote_canvas)

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        systemRunningEnv = KSpice.readKeyInInt(
            applicationContext,
            KSpice.SYSTEM_RUN_ENV
        ) == KSpice.PHONE || KSpice.readKeyInInt(
            applicationContext,
            KSpice.SYSTEM_RUN_ENV
        ) == KSpice.FLAT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            initBroadcast()
            initMenuEvent()
            initZoomEvent()
            initSoftKeyboardEvent()
            initSpecialKeyboardEvent()
            createLoading()
        } else {
            dialogHint(getString(R.string.Your_current_system_version_is_too_low_please_upgrade))
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun initMenuEvent() {
        //菜单按钮相关事件
        flRemoteMenu.postDelayed({ moveMenuButton() }, 3000)
        flRemoteMenu.setOnTouchListener { v, event ->
            val moveView = ViewOperateUtil.moveView(v, event)
            if (moveView && event.action == MotionEvent.ACTION_UP) {
                showDetailListMenu(v)
            }
            true
        }
    }

    /**
     * 初始化缩放事件
     */
    private fun initZoomEvent() {
        //初始化TV、手机不同系统下的UI
        scaleGestureListener = ScaleGestureListener(canvas)
        scaleGestureListener?.isFullGroup = true
        scaleGestureListener?.setOnScaleListener { canvas.scaleFactor = it }
        scaleGestureBinder = ScaleGestureBinder(this, scaleGestureListener)
    }

    /**
     * 系统键盘相关事件
     */
    private fun initSoftKeyboardEvent() {
        ViewOperateUtil.setSoftKeyBoardListener(this, object : SoftKeyBoardListener {

            override fun showKeyBoard(keyboardHeight: Int) {
                //获取底部软键盘高度
                val navigationBarHeight =
                    ViewOperateUtil.getNavigationBarHeight(context = this@KRemoteCanvasActivity)
                keyBoardHeight = keyboardHeight - navigationBarHeight
                flRemoteMenu.visibility = View.GONE
                //如果是手机端、显示特殊键盘
                if (systemRunningEnv) {
                    llSpecialKeyboard.translationY = -keyBoardHeight.toFloat()
                    val animator = ObjectAnimator.ofFloat(llSpecialKeyboard, "alpha", 0f, 1f)
                    animator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            //解决调整分辨率失败后，调整更加分辨率时，切换横屏后界面重绘问题
                            if (keyBoardHeight > 0) {
                                canvas.updateKeyboardHeight(keyBoardHeight + llSpecialKeyboard.height)
                            }
                        }
                    })
                    animator.setDuration(500).start()
                    llSpecialKeyboard.visibility = View.VISIBLE
                } else {
                    canvas.updateKeyboardHeight(keyBoardHeight)
                }
            }

            override fun hideKeyBoard(keyboardHeight: Int) {
                keyBoardHeight = keyboardHeight
                flRemoteMenu.visibility = View.VISIBLE
                if (systemRunningEnv) {
                    llSpecialKeyboard.translationY = 0f
                    llSpecialKeyboard.visibility = View.GONE
                }
                canvas.updateKeyboardHeight(0)
            }
        })
    }

    /**
     * 特殊键盘相关事件
     */
    private fun initSpecialKeyboardEvent() {
        //单独发送按下、松开事件
        btnTab.setOnClickListener {
            sendKey(KEY_WIN_TAB, true)
            sendKey(KEY_WIN_TAB, false)
        }
        btnEsc.setOnClickListener {
            sendKey(KEY_WIN_ESC, true)
            sendKey(KEY_WIN_ESC, false)
            resetSpecialKeyboard()
        }
        //点击只发送按下，再次点击才松开
        btnSuper.setOnClickListener {
            selectOrReleaseSpecialKeyboard(it, KEY_WIN)
        }
        btnShift.setOnClickListener {
            selectOrReleaseSpecialKeyboard(it, KEY_WIN_SHIFT)
        }
        btnCtrl.setOnClickListener {
            selectOrReleaseSpecialKeyboard(it, KEY_WIN_CTRL)
        }
        btnAlt.setOnClickListener {
            selectOrReleaseSpecialKeyboard(it, KEY_WIN_ALT)
        }

        //点击打开更多选项、自定义键盘
        btnShowMore.setOnClickListener {
            it.isSelected = !it.isSelected
            if (it.isSelected) {
                Log.d(TAG, "initSpecialKeyboardEvent: 显示更多")
                popBottomSoftKey ?: let {
                    popBottomSoftKey = KPopBottomSoftKey(this, keyBoardHeight)
                    initBottomSoftKeyCallback()
                }
                popBottomSoftKey?.showAtLocation(canvas, Gravity.BOTTOM, 0, 0)
            } else {
                Log.d(TAG, "initSpecialKeyboardEvent: 隐藏更多")
                popBottomSoftKey?.dismiss()
            }
        }
    }

    /**
     * 底部软键盘回调
     */
    private fun initBottomSoftKeyCallback() {
        popBottomSoftKey?.setIPopBottomSoftKeyCallback(object : IPopBottomSoftKeyCallback {
            override fun onTouchDown(event: MotionEvent, keyCode: Int) {
                androidKeycodeToWinCode(keyCode, true)
            }

            override fun onTouchUp(event: MotionEvent, keyCode: Int) {
                androidKeycodeToWinCode(keyCode, false)
            }

            override fun onMouseEvent(isDown: Boolean) {
                canvas.rightMouseButton(isDown)
            }
        })
    }

    private fun createLoading() {
        dialog = ProgressDialog.show(
            this,
            getString(R.string.info_progress_dialog_connecting),
            getString(R.string.info_progress_dialog_establishing),
            true,
            true
        )
        dialog?.setCancelable(false)
    }

    /**
     * 显示系统软键盘
     */
    private fun showKeyboard() {
        val inputMgr: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMgr.toggleSoftInputFromWindow(
            window.decorView.windowToken,
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.SHOW_FORCED
        )
        //检测系统是否有键盘
//        canvas.postDelayed(runnable, 1000)
    }

    //监听是否有键盘
    /*val runnable = {
        if (!keyBoardIsShow) {
            Toast.makeText(
                canvas.context,
                getString(R.string.the_system_has_no_keyboard),
                Toast.LENGTH_SHORT
            ).show()
        }
    }*/

    private fun hideKeyboard() {
        val inputMgr: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMgr.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    private fun sendDelayNotice() {
        canvas.postDelayed(connectSucceedRunnable, 60 * 1000)
    }

    private val connectSucceedRunnable = {
        sendBroadcast(Intent(KSpice.ACTION_SPICE_CONNECT_SUCCEED))
        sendDelayNotice()
    }

    /**
     * Spice连接时和调整分辨率的相关事件处理
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun eventBus(messageEvent: KMessageEvent) {
        //Log.d("MessageEvent", "eventBus: ${messageEvent.requestCode}")
        dialog?.dismiss()
        when (messageEvent.requestCode) {
            KMessageEvent.SPICE_CONNECT_SUCCESS -> {
                sendBroadcast(Intent(KSpice.ACTION_SPICE_CONNECT_SUCCEED))
                sendDelayNotice()
                //初始化键盘拦截器
                keyBoard = KeyBoard(resources)
            }
            KMessageEvent.SPICE_CONNECT_TIMEOUT -> {
                messageEvent.msg?.let {
                    canvas.close()
                    dialogHint(it)
                }
            }
            //失败原因：1、连接失败   2、连接超时  3、远程被断开
            KMessageEvent.SPICE_CONNECT_FAILURE -> {
                messageEvent.msg?.let {
                    canvas.close()
                    dialogHint(it)
                }
            }
            KMessageEvent.SPICE_ADJUST_RESOLVING -> {
                canvas.visibility = View.INVISIBLE
                tvAdjustHint.visibility = View.VISIBLE
            }
            KMessageEvent.SPICE_ADJUST_RESOLVING_TIMEOUT -> {
                if (systemRunningEnv) {
                    requestedOrientation = if (messageEvent.isVertical) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                }
            }
            KMessageEvent.SPICE_ADJUST_RESOLVING_SUCCEED -> {
                tvAdjustHint.visibility = View.GONE
                canvas.visibility = View.VISIBLE
            }
            else -> Log.d("MessageEvent", "eventBus: 其他")
        }
    }

    /**
     * Spice连接时的dialog提示
     */
    private fun dialogHint(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.error))
        builder.setMessage(message)
        builder.setCancelable(false)
        builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        if (!(alertDialog != null && alertDialog?.isShowing!!)) {
            alertDialog = builder.create()
            alertDialog?.show()
        }
    }

    /**
     * 键盘的相关事件处理
     */
    private var tabSign: Boolean = false  //解决，tab第一次按下时，只返回一次事件松开事件
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        keyBoard ?: let {
            return false
        }

        val isDown = event.action == KeyEvent.ACTION_DOWN
        //处理键盘菜单按钮
        if (event.keyCode == KeyEvent.KEYCODE_MENU && !isDown && event.source == InputDevice.SOURCE_KEYBOARD) {
            Log.d(TAG, "dispatchKeyEvent: 键盘显示显示菜单${event.source}")
            showDetailListMenu(flRemoteMenu)
            return true
        }
        //处理鼠标中间键
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.source == InputDevice.SOURCE_MOUSE) {
            Log.d(TAG, "dispatchKeyEvent: 鼠标中间键:${event.keyCode},$isDown")
            canvas.middleMouseButton(isDown)
            return true
        }


        //解决右键弹出菜单问题
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.source == InputDevice.SOURCE_MOUSE) {
            canvas.rightMouseButton(isDown)
            return true
        }

        //解决按下num lock按键后与上下左右冲突问题
        if (event.keyCode == KeyEvent.KEYCODE_NUM_LOCK) {
            return true
        }

        //解决左侧Enter按键无效问题
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            sendKey(KEY_WIN_CENTER_ENTER, isDown)
            return true
        }

        return androidKeyCodeToWinCode(event, isDown)
    }

    /**
     * 监听鼠标按下时得移动
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        //处理双指时使用,手势缩放
        onScaleEvent(event)
        return true
    }

    /**
     * 处理缩放事件，目前只支持触摸模式
     */
    private fun onScaleEvent(event: MotionEvent) {
        //1、画面没全屏不支持缩放  2、调屏模式下并且打开键盘也不支持   3、TV环境不支持 4、被调整分辨率情况不支持
        canvas.canvasBitmap?.let {
            if (KSpice.readKeyInBoolean(
                    context = this,
                    key = KSpice.IS_ADJUST
                ) && keyBoardHeight > 0 && systemRunningEnv || canvas.isAdjustFromPassive
            ) {
                canvas.updateScaleToDefault()
                //调整全屏成功才支持缩放
            } else if (canvas.isAdjustFullSucceed) {
                scaleGestureBinder?.onTouchEvent(event)
            }
            /*if (it.width != canvas.width || it.height != canvas.height || (KSpice.readKeyInBoolean(context = this, key = KSpice.IS_ADJUST) && keyBoardHeight > 0)) {
                canvas.updateScaleToDefault()
                *//*KToast.show(
                    applicationContext,
                    getString(R.string.In_this_operation_mode_zooming_is_not_supported)
                )*//*
            } else if (KSpice.readKeyInBoolean(context = this, key = KSpice.SYSTEM_RUN_ENV) && it.width == canvas.width && it.height == canvas.height) {
                scaleGestureBinder?.onTouchEvent(event)
            }*/
        }
    }

    /**
     * android 系统键盘 keycode转为win 对应的键盘码
     */
    private fun androidKeyCodeToWinCode(event: KeyEvent, isDown: Boolean): Boolean {
        val unicodeChar =
            event.getUnicodeChar(event.metaState and UNICODE_META_MASK.inv() and KeyEvent.META_ALT_MASK.inv())
        val code: Int
        code = if (unicodeChar > 0) unicodeChar or UNICODE_MASK else event.keyCode
        val codeList = keyBoard?.keyCode?.get(code)
        codeList?.forEach {
            it?.let { code ->
                var sendCode = code
                //处理shift+字母、符号、数字切换
                if (code and SCANCODE_SHIFT_MASK != 0) {
                    //Log.d(TAG, "dispatchKeyEvent Found Shift mask. code:$code")
                    sendCode = code and SCANCODE_SHIFT_MASK.inv()
                    //解决右侧*、-、+输入不正确问题
                    if (event.keyCode == KeyEvent.KEYCODE_NUMPAD_MULTIPLY || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ADD) {
                        sendKey(KEY_WIN_SHIFT, isDown)
                        sendKey(sendCode, isDown)
                        //Log.d(TAG, "dispatchKeyEvent: 为右侧*、+号键")
                        return true
                    }
                }

                //处理alt事件
                val altPressed = event.isAltPressed
                if (altPressed && event.repeatCount == 0 && isDown && (event.keyCode == KeyEvent.KEYCODE_ALT_LEFT || event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT)) {
                    //Log.d(TAG, "dispatchKeyEvent:left or right alt true")
                    sendKey(KEY_WIN_ALT, isDown)
                    return true
                } else if (altPressed && (event.keyCode == KeyEvent.KEYCODE_ALT_LEFT || event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT) && !isDown) {
                    //Log.d(TAG, "dispatchKeyEvent: alt+tab组合按键时，alt按下未松开，却响应了松开事件")
                    return true
                } else if (!altPressed && (event.keyCode == KeyEvent.KEYCODE_ALT_LEFT || event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT) && !isDown) {
                    //Log.d(TAG, "dispatchKeyEvent:left or right alt false")
                    sendKey(KEY_WIN_ALT, isDown)
                    tabSign = false
                    return true
                }
                //处理alt+tab事件
                if (altPressed && event.keyCode == KeyEvent.KEYCODE_TAB) {
                    return if (!tabSign) {
                        //Log.d(TAG, "dispatchKeyEvent: alt+tab组合按键时，首次只响应一次tab松开事件")
                        sendKey(sendCode, true)
                        sendKey(sendCode, false)
                        tabSign = true
                        true
                    } else {
                        //Log.d(TAG, "dispatchKeyEvent: alt+tab组合按键时，tab $isDown")
                        sendKey(sendCode, isDown)
                        true
                    }
                }
                //处理左右两侧ctrl不一致问题
                if (event.keyCode == KeyEvent.KEYCODE_CTRL_RIGHT || event.keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
                    sendKey(KEY_WIN_CTRL, isDown)
                    return true
                }
                sendKey(sendCode, isDown)
                if (!isDown) {
                    //重置特殊键盘按下的特殊键
                    resetSpecialKeyboard()
                }
            }
        }
        return true
    }

    /**
     * 自定义特殊键盘 keycode转win对应的键盘码
     */
    private fun androidKeycodeToWinCode(keyCode: Int, isDown: Boolean) {
        keyBoard?.keyCode?.get(keyCode)?.forEach { it?.let { code -> sendKey(code, isDown) } }
    }

    /**
     * 发送键盘key指令
     */
    private fun sendKey(code: Int, down: Boolean) {
        canvas.sendKey(code, down)
    }

    /**
     * 选中或释放特殊键盘win、shift、ctrl、alt
     */
    private fun selectOrReleaseSpecialKeyboard(view: View, keyCode: Int) {
        view.isSelected = !view.isSelected
        sendKey(keyCode, view.isSelected)
    }

    /**
     * 重置所有已经被选中的特殊键盘
     */
    private fun resetSpecialKeyboard() {
        resetSpecialKeyboardByViewId(btnSuper, KEY_WIN)
        resetSpecialKeyboardByViewId(btnCtrl, KEY_WIN_CTRL)
        resetSpecialKeyboardByViewId(btnShift, KEY_WIN_SHIFT)
        resetSpecialKeyboardByViewId(btnAlt, KEY_WIN_ALT)
    }

    /**
     * 重置特殊键盘根据view的id
     */
    private fun resetSpecialKeyboardByViewId(linearLayout: View, keyCode: Int) {
        if (linearLayout.isSelected) {
            linearLayout.isSelected = !linearLayout.isSelected
            sendKey(keyCode, false)
        }
    }

    /**
     * 显示详细菜单列表
     */
    private fun showDetailListMenu(v: View) {
        showOrHideMenuButton(v, 0f)
        //val wrapper: Context =ContextThemeWrapper(this, R.style.MyPopupStyle)
        //1.实例化PopupMenu
        popupMenu ?: let {
            popupMenu = PopupMenu(this, v)
            //2、加载xml
            popupMenu?.inflate(R.menu.desktop_menu)
            //3.为弹出菜单设置点击监听
            popupMenu?.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.disconnect -> {
                        finish()
                        true
                    }
                    R.id.system_keyboard -> {
                        showKeyboard()
                        true
                    }
                    R.id.is_adjust -> {
                        KSpice.writeValueToKey(this, KSpice.IS_ADJUST, true)
                        true
                    }
                    R.id.no_adjust -> {
                        KSpice.writeValueToKey(this, KSpice.IS_ADJUST, false)
                        true
                    }
                    R.id.touch_mode -> {
                        KSpice.writeValueToKey(
                            this,
                            KSpice.MOUSE_MODE,
                            KSpice.MouseMode.MODE_TOUCH.toString()
                        )
                        canvas.updateMouseMode()
                        true
                    }
                    R.id.click_mode -> {
                        KSpice.writeValueToKey(
                            this,
                            KSpice.MOUSE_MODE,
                            KSpice.MouseMode.MODE_CLICK.toString()
                        )
                        canvas.updateMouseMode()
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
            //4、设置关闭监听
            popupMenu?.setOnDismissListener { showOrHideMenuButton(v, 1f) }
        }
        //5、读取本地配置属性，并显示UI
        if (KSpice.readKeyInBoolean(this, KSpice.IS_ADJUST)) {
            popupMenu?.menu?.findItem(R.id.is_adjust)?.isChecked = true
        } else {
            popupMenu?.menu?.findItem(R.id.no_adjust)?.isChecked = true
        }
        //6、读取本地运行环境配置，显示不同UI
        if (systemRunningEnv) {
            val itemInputMode = popupMenu?.menu?.findItem(R.id.operator_mode)
            itemInputMode?.isVisible = true
            if (KSpice.readKeyInString(
                    this,
                    KSpice.MOUSE_MODE
                ) == KSpice.MouseMode.MODE_CLICK.toString()
            ) {
                popupMenu?.menu?.findItem(R.id.click_mode)?.isChecked = true
            } else {
                popupMenu?.menu?.findItem(R.id.touch_mode)?.isChecked = true
            }
        }
        //7.显示弹出菜单
        popupMenu?.show()
    }

    /**
     * 移动菜单按钮
     */
    private fun moveMenuButton() {
        flRemoteMenu.animate()
            .translationX((resources.getDimensionPixelSize(R.dimen.dp_40) + flRemoteMenu.width / 2).toFloat())
            .start()
    }

    /**
     * 显示或隐藏菜单按钮
     */
    private fun showOrHideMenuButton(v: View, value: Float) {
        v.animate().alpha(value).start()
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
        llSpecialKeyboard.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        sendBroadcast(Intent(KSpice.ACTION_SPICE_CONNECT_DISCONNECT))
        Log.d(TAG, "onDestroy: 销毁并断开连接")
        canvas.removeCallbacks(connectSucceedRunnable)
        canvas.close()
        android.os.Process.killProcess(android.os.Process.myPid())
        System.gc()
    }

    companion object {
        private const val TAG = "KRemoteCanvasActivity"
    }
}