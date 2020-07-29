@file:Suppress("DEPRECATION")

package com.vesystem.spice.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.vesystem.spice.R
import com.vesystem.spice.keyboard.KeyBoard
import com.vesystem.spice.keyboard.KeyBoard.Companion.KEY_WIN_CENTER_ENTER
import com.vesystem.spice.keyboard.KeyBoard.Companion.KEY_WIN_SHIFT
import com.vesystem.spice.keyboard.KeyBoard.Companion.SCANCODE_SHIFT_MASK
import com.vesystem.spice.keyboard.KeyBoard.Companion.UNICODE_MASK
import com.vesystem.spice.keyboard.KeyBoard.Companion.UNICODE_META_MASK
import com.vesystem.spice.model.KMessageEvent
import com.vesystem.spice.model.KSpice
import com.vesystem.spice.ui.interfaces.IPopMenuItemListener
import com.vesystem.spice.ui.interfaces.ISoftKeyboardListener
import com.vesystem.spice.ui.interfaces.ISoftKeyboardListener.OnSoftKeyBoardChangeListener
import com.vesystem.spice.ui.pop.KPopMenuList
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
 */
class KRemoteCanvasActivity : Activity(), View.OnClickListener {
    private var keyBoard: KeyBoard? = null //自定义物理键盘
    private var dialog: ProgressDialog? = null //连接时的加载dialog
    private var alertDialog: AlertDialog? = null //连接信息提示
    private var popMenuList: KPopMenuList? = null//菜单弹出列表Pop
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_remote_canvas)

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        initView()
        createLoading()
    }


    @Suppress("DEPRECATION")
    private fun initView() {
        flRemoteMenu.setOnClickListener(this)
        ISoftKeyboardListener.setSoftKeyBoardListener(this, object : OnSoftKeyBoardChangeListener {
            override fun keyBoardShow(height: Int) {
                Log.i(TAG, "keyBoardShow: $height")
                flRemoteMenu.visibility = View.GONE
                canvas.updateSpiceResolvingPower(canvas.width, canvas.height - height)
            }

            override fun keyBoardHide(height: Int) {
                Log.i(TAG, "keyBoardHide: $height")
                flRemoteMenu.visibility = View.VISIBLE
                canvas.recoverySpiceResolvingPower()
            }
        })
        if (KSpice.sysRunEnv) {
            zoomControls.setOnZoomInClickListener { canvas.canvasZoomIn() }
            zoomControls.setOnZoomOutClickListener { canvas.canvasZoomOut() }
        } else {
            zoomControls.hide()
        }

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
        val showSoftInput = inputMgr.showSoftInput(window.decorView, 0)
        if (!showSoftInput) {
            Toast.makeText(
                canvas.context,
                getString(R.string.the_system_has_no_keyboard),
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /**
     * Spice连接时的相关事件处理
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun eventBus(messageEvent: KMessageEvent) {
//        Log.i("MessageEvent", "eventBus: ${messageEvent.requestCode}")
        dialog?.dismiss()
        when (messageEvent.requestCode) {
            KMessageEvent.SPICE_CONNECT_SUCCESS -> {
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

            else -> Log.i("MessageEvent", "eventBus: 其他")
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
        builder.setPositiveButton(
            getString(R.string.confirm)
        ) { dialog, _ ->
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
    private var tabSign: Boolean = false//解决，tab第一次按下时，只返回一次事件松开事件
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        keyBoard ?: let {
            return false
        }

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
            sendKey(KEY_WIN_CENTER_ENTER, isDown)
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
                        sendKey(KEY_WIN_SHIFT, isDown)
                        sendKey(sendCode, isDown)
//                        Log.i(TAG, "dispatchKeyEvent: 为右侧*、+号键")
                        return true
                    }
                }

                //处理alt事件
                val altPressed = event.isAltPressed
                if (altPressed && event.repeatCount == 0 && isDown && (event.keyCode == KeyEvent.KEYCODE_ALT_LEFT || event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT)) {
//                        Log.i(TAG, "dispatchKeyEvent:left or right alt true")
                    sendKey(KeyBoard.KEY_WIN_ALT, isDown)
                    return true
                } else if (altPressed && (event.keyCode == KeyEvent.KEYCODE_ALT_LEFT || event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT) && !isDown) {
//                    Log.i(TAG, "dispatchKeyEvent: alt+tab组合按键时，alt按下未松开，却响应了松开事件")
                    return true
                } else if (!altPressed && (event.keyCode == KeyEvent.KEYCODE_ALT_LEFT || event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT) && !isDown) {
//                    Log.i(TAG, "dispatchKeyEvent:left or right alt false")
                    sendKey(KeyBoard.KEY_WIN_ALT, isDown)
                    tabSign = false
                    return true
                }

                //处理alt+tab事件
                if (altPressed && event.keyCode == KeyEvent.KEYCODE_TAB) {
                    return if (!tabSign) {
                        //Log.i(TAG, "dispatchKeyEvent: alt+tab组合按键时，首次只响应一次tab松开事件")
                        sendKey(sendCode, true)
                        sendKey(sendCode, false)
                        tabSign = true
                        true
                    } else {
                        //Log.i(TAG, "dispatchKeyEvent: alt+tab组合按键时，tab $isDown")
                        sendKey(sendCode, isDown)
                        true
                    }
                }

                if (event.keyCode == KeyEvent.KEYCODE_CTRL_RIGHT || event.keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
                    sendKey(KeyBoard.KEY_WIN_CTRL, isDown)
                    return true
                }
                sendKey(sendCode, isDown)
            }
        }
        return true
    }


    /**
     * 发送键盘key指令
     */
    private fun sendKey(code: Int, down: Boolean) {
        canvas.sendKey(code, down)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        Log.i(TAG, "onGenericMotionEvent: ${event.actionMasked}")
        return super.onGenericMotionEvent(event)
    }


    /**
     * 菜单按钮的点击事件
     */
    override fun onClick(v: View?) {
        v?.let { it ->
            if (it.id == R.id.flRemoteMenu) {
                showOrHide(v, 0f, View.GONE)
                popMenuList ?: let {
                    popMenuList = KPopMenuList(this, object : IPopMenuItemListener {
                        override fun onClickSystemKeyboard() {
                            showOrHide(v, 1f, View.VISIBLE)
                            showKeyboard()
                        }

                        override fun onClickDisconnect() {
                            showOrHide(v, 1f, View.VISIBLE)
                            finish()
                        }

                        override fun onDismiss() {
                            showOrHide(v, 1f, View.VISIBLE)
                        }
                    })
                }
                popMenuList?.let { pop ->
                    if (!pop.isShowing) {
                        pop.showAsDropDown(v, 0, 0)
                    }
                }
            }
        }
    }

    private fun showOrHide(v: View, value: Float, visibility: Int) {
        v.animate().alpha(value).start()
        v.visibility = visibility
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        Log.i(TAG, "onDestroy: 销毁并断开连接")
        canvas.close()
        System.gc()
    }

    companion object {
        private const val TAG = "KRemoteCanvasActivity"
    }
}