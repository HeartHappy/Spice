package com.vesystem.ui

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.gordonwong.materialsheetfab.MaterialSheetFab
import com.vesystem.opaque.model.MessageEvent
import com.vesystem.spice.R
import com.vesystem.ui.widget.Fab
import kotlinx.android.synthetic.main.activity_remote_canvas.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.properties.Delegates

/**
 * Created Date 2020/7/6.
 * @author ChenRui
 * ClassDescription:
 */
class KRemoteCanvasActivity : Activity(), View.OnClickListener {
    private var materialSheetFab by Delegates.notNull<MaterialSheetFab<Fab>>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_remote_canvas)

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        initView()
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
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
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
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun eventBus(messageEvent: MessageEvent) {
        Log.i("MessageEvent", "eventBus: ${messageEvent.requestCode}")
        when (messageEvent.requestCode) {
            //失败原因：1、连接失败   2、连接超时  3、远程断开
            MessageEvent.SPICE_CONNECT_FAILURE -> {
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
                            Log.i("MessageEvent", "eventBus: 连接失败")
                        }
                    }
                }
            }
            MessageEvent.SPICE_CONNECT_SUCCESS -> Log.i("MessageEvent", "eventBus: 连接成功")
            else -> Log.i("MessageEvent", "eventBus: 其他")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        System.gc()
    }

    override fun onClick(v: View?) {
        v?.let {
            if (it.id == R.id.session_sys_keyboard) {
                materialSheetFab.hideSheet()
                showKeyboard()
            } else if (it.id == R.id.session_disconnect) {
                val sc = canvas.spiceCommunicator?.get()
                sc?.isConnectSucceed?.let {
                    if (sc.isConnectSucceed) {
                        sc.isConnectSucceed = false
                        sc.isClickDisconnect = true
                        sc.disconnect()
                    }
                }
                canvas.scope?.get()?.cancel()
                finish()
            }
        }
    }
}