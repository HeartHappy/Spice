package com.vesystem.spice.ui

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.gordonwong.materialsheetfab.MaterialSheetFab
import com.vesystem.spice.R
import com.vesystem.spice.keyboard.KeyBoard
import com.vesystem.spice.keyboard.KeyBoard.Companion.KEY_WIN_CENTER_ENTER
import com.vesystem.spice.keyboard.KeyBoard.Companion.KEY_WIN_SHIFT
import com.vesystem.spice.keyboard.KeyBoard.Companion.SCANCODE_SHIFT_MASK
import com.vesystem.spice.keyboard.KeyBoard.Companion.UNICODE_MASK
import com.vesystem.spice.keyboard.KeyBoard.Companion.UNICODE_META_MASK
import com.vesystem.spice.model.KMessageEvent
import com.vesystem.spice.ui.widget.Fab
import kotlinx.android.synthetic.main.activity_remote_canvas.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.properties.Delegates

/**
 * Created Date 2020/7/6.
 * @author ChenRui
 * ClassDescription:远程界面
 * 负责处理：
 * 1、加载自定义View
 * 2、处理其他view的事件
 * 3、键盘的交互
 */
class KRemoteCanvasActivity : Activity(), View.OnClickListener {
    private var materialSheetFab by Delegates.notNull<MaterialSheetFab<Fab>>()
    private var keyBoard: KeyBoard? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_remote_canvas)

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        initView()

        initSoftKeyBoard()
    }


    @Suppress("DEPRECATION")
    private fun initView() {

        // Initialize and define actions for on-screen keys.
        val overlay = findViewById<View>(R.id.overlay)
        val sheetColor = resources.getColor(R.color.col_white)
        val fabColor = resources.getColor(R.color.col_white)
        materialSheetFab = MaterialSheetFab(
            floatingActionButton, fab_sheet, overlay,
            sheetColor, fabColor
        )
        findViewById<View>(R.id.session_sys_keyboard).setOnClickListener(this)
        findViewById<View>(R.id.session_disconnect).setOnClickListener(this)
    }


    private fun showKeyboard() {
        val inputMgr: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        //解决自定义键盘显示时太快，导致的移动问题
        inputMgr.toggleSoftInputFromWindow(
            canvas.windowToken,
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.SHOW_FORCED
        )
        canvas.postDelayed({
            if (!isSoftShowing()) {
                Toast.makeText(
                    canvas.context,
                    "系统没有自带软键盘，请插入物理键盘操作！",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, 500)
    }


    /**
     * 判断软键盘是否弹出
     *
     * @return
     */
    private fun isSoftShowing(): Boolean {
        //获取当前屏幕内容的高度
        val screenHeight = window.decorView.height
        //获取View可见区域的bottom
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        return screenHeight - rect.bottom > 200
    }

    /**
     * 获取软键盘高度
     *
     * @return
     */
    /*@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun getSoftButtonsBarHeight(): Int {
        val metrics = DisplayMetrics()
        //这个方法获取可能不是真实屏幕的高度
        windowManager.defaultDisplay.getMetrics(metrics)
        val usableHeight = metrics.heightPixels
        //获取当前屏幕的真实高度
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val realHeight = metrics.heightPixels
        return if (realHeight > usableHeight) {
            realHeight - usableHeight
        } else {
            0
        }
    }*/

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun eventBus(messageEvent: KMessageEvent) {
        Log.i("MessageEvent", "eventBus: ${messageEvent.requestCode}")
        when (messageEvent.requestCode) {
            KMessageEvent.SPICE_CONNECT_SUCCESS -> Log.i(
                "MessageEvent",
                "eventBus: 连接成功"
            )
            KMessageEvent.SPICE_CONNECT_TIMEOUT -> {
                Log.i("MessageEvent", "eventBus: 连接超时")
            }
            //失败原因：1、连接失败   2、连接超时  3、远程断开
            KMessageEvent.SPICE_CONNECT_FAILURE -> {
                val sc = canvas.spiceCommunicator?.get()
                sc?.isConnectSucceed?.let {
                    //如果时连接情况下，被断开，视为其他设备占用，导致断开连接
                    when {
                        sc.isConnectSucceed -> {
                            Log.i("MessageEvent", "eventBus: 远程连接断开")
                            sc.disconnect()
                            //如果是点击时，断开得连接
                        }
                        sc.isClickDisconnect -> {
                            Log.i("MessageEvent", "eventBus:点击导致得断开连接，返回得失败 ")
                            //1、主动点击断开时，返回得连接失败
                        }
                        else -> {
                            //2、连接时，返回得连接失败
                            canvas.myHandler?.removeMessages(KMessageEvent.SPICE_CONNECT_TIMEOUT)
                            Log.i("MessageEvent", "eventBus: 连接失败,无法连接或认证ca主题")
                        }
                    }
                }
            }

            else -> Log.i("MessageEvent", "eventBus: 其他")
        }
    }

    override fun onClick(v: View?) {
        v?.let {
            if (it.id == R.id.session_sys_keyboard) {
                materialSheetFab.hideSheet()
                showKeyboard()
            } else if (it.id == R.id.session_disconnect) {
                close()
                finish()
            }
        }
    }

    private fun initSoftKeyBoard() {
        keyBoard = KeyBoard(resources)
    }


    private var tabSign: Boolean = false//解决，tab第一次按下时，只返回一次事件松开事件
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isDown = event.action == KeyEvent.ACTION_DOWN

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
            sendKey(KEY_WIN_CENTER_ENTER, isDown, event)
            return true
        }

        val unicodeChar =
            event.getUnicodeChar(event.metaState and UNICODE_META_MASK.inv() and KeyEvent.META_ALT_MASK.inv())
//        Log.i(TAG, "dispatchKeyEvent unicodeChar: $unicodeChar,keycode:${event.keyCode},${event.isSystem}")
        val code: Int
        code = if (unicodeChar > 0) {
            unicodeChar or UNICODE_MASK
        } else {
            event.keyCode
        }
        val codeList = keyBoard?.keyCode?.get(code)
        codeList?.forEach {
            it?.let { code ->
                var sendCode = code
                //处理shift+字母、符号、数字切换
                if (code and SCANCODE_SHIFT_MASK != 0) {
//                    Log.i(TAG, "dispatchKeyEvent Found Shift mask. code:$code")
                    sendCode = code and SCANCODE_SHIFT_MASK.inv()
                    //解决右侧*、-、+输入不正确问题
                    if (event.keyCode == KeyEvent.KEYCODE_NUMPAD_MULTIPLY || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ADD) {
                        sendKey(KEY_WIN_SHIFT, isDown, event)
                        sendKey(sendCode, isDown, event)
//                        Log.i(TAG, "dispatchKeyEvent: 为右侧*、+号键")
                        return true
                    }
                }

                //处理alt事件
                val altPressed = event.isAltPressed
                if (altPressed && event.repeatCount == 0 && isDown && (event.keyCode == KeyEvent.KEYCODE_ALT_LEFT || event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT)) {
//                        Log.i(TAG, "dispatchKeyEvent:left or right alt true")
                    sendKey(KeyBoard.KEY_WIN_ALT, isDown, event)
                    return true
                } else if (altPressed && (event.keyCode == KeyEvent.KEYCODE_ALT_LEFT || event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT) && !isDown) {
//                    Log.i(TAG, "dispatchKeyEvent: alt+tab组合按键时，alt按下未松开，却响应了松开事件")
                    return true
                } else if (!altPressed && (event.keyCode == KeyEvent.KEYCODE_ALT_LEFT || event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT) && !isDown) {
//                    Log.i(TAG, "dispatchKeyEvent:left or right alt false")
                    sendKey(KeyBoard.KEY_WIN_ALT, isDown, event)
                    tabSign = false
                    return true
                }

                //处理alt+tab事件
                if (altPressed && event.keyCode == KeyEvent.KEYCODE_TAB) {
                    return if (!tabSign) {
                        //Log.i(TAG, "dispatchKeyEvent: alt+tab组合按键时，首次只响应一次tab松开事件")
                        sendKey(sendCode, true, event)
                        sendKey(sendCode, false, event)
                        tabSign = true
                        true
                    } else {
                        //Log.i(TAG, "dispatchKeyEvent: alt+tab组合按键时，tab $isDown")
                        sendKey(sendCode, isDown, event)
                        true
                    }
                }

                if (event.keyCode == KeyEvent.KEYCODE_CTRL_RIGHT || event.keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
                    sendKey(KeyBoard.KEY_WIN_CTRL, isDown, event)
                    return true
                }
                sendKey(sendCode, isDown, event)
            }
        }
        return true
    }


    private fun sendKey(code: Int, down: Boolean, event: KeyEvent) {
        Log.i(TAG, "sendKey: $code,$down,${event.repeatCount},${event.isAltPressed}")
        canvas.spiceCommunicator?.get()?.sendSpiceKeyEvent(down, code)
    }


    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        System.gc()
    }


    /**
     * 断开连接，并取消协程
     */
    private fun close() {
        val sc = canvas.spiceCommunicator?.get()
        sc?.isConnectSucceed?.let {
            if (sc.isConnectSucceed) {
                sc.isConnectSucceed = false
                sc.isClickDisconnect = true
                sc.disconnect()
            }
        }
        canvas.scope?.get()?.cancel()
    }

    companion object {
        private const val TAG = "KRemoteCanvasActivity"
    }
}