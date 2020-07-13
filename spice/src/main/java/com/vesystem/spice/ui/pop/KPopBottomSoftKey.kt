package com.vesystem.spice.ui.pop

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.vesystem.spice.R
import com.vesystem.spice.ui.interfaces.KIPopBottomSoftKeyCallback

/**
 * Created Date 2020-01-16.
 *
 * @author ChenRui
 * ClassDescription：底部软键盘
 */
class KPopBottomSoftKey(context: Context?, softKeyBoardHeight: Int) :
    PopupWindow(context), OnTouchListener {
    private var mIPopBottomSoftKeyCallback: KIPopBottomSoftKeyCallback? =
        null

    fun setIPopBottomSoftKeyCallback(IPopBottomSoftKeyCallback: KIPopBottomSoftKeyCallback?) {
        mIPopBottomSoftKeyCallback = IPopBottomSoftKeyCallback
    }

    private fun initPopProperty(view: View, softKeyBoardHeight: Int) {
        Log.i("PopBottomSoftKey", "initPopProperty: 创建软键盘窗体高度：$softKeyBoardHeight")
        contentView = view
        width = LinearLayout.LayoutParams.MATCH_PARENT
        height = softKeyBoardHeight
        //        this.setInputMethodMode(android.widget.PopupWindow.INPUT_METHOD_NEEDED);  //设置Pop不压键盘，默认是会压住键盘
        //设置setBackgroundDrawable才会全屏，默认不全屏
        setBackgroundDrawable(BitmapDrawable())
        isOutsideTouchable = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView(view: View) {
        val btnF1 = view.findViewById<Button>(R.id.btnF1)
        val btnF2 = view.findViewById<Button>(R.id.btnF2)
        val btnF3 = view.findViewById<Button>(R.id.btnF3)
        val btnF4 = view.findViewById<Button>(R.id.btnF4)
        val btnF5 = view.findViewById<Button>(R.id.btnF5)
        val btnF6 = view.findViewById<Button>(R.id.btnF6)
        val btnF7 = view.findViewById<Button>(R.id.btnF7)
        val btnF8 = view.findViewById<Button>(R.id.btnF8)
        val btnF9 = view.findViewById<Button>(R.id.btnF9)
        val btnF10 = view.findViewById<Button>(R.id.btnF10)
        val btnF11 = view.findViewById<Button>(R.id.btnF11)
        val btnF12 = view.findViewById<Button>(R.id.btnF12)
        val btnRightMouse = view.findViewById<LinearLayout>(R.id.btnRightMouse)
        val btnDelText = view.findViewById<LinearLayout>(R.id.btnDelText)
        val btnEnter = view.findViewById<LinearLayout>(R.id.btnEnter)
        val btnIns = view.findViewById<Button>(R.id.btnIns)
        val btnDel = view.findViewById<Button>(R.id.btnDel)
        val btnPageUp =
            view.findViewById<Button>(R.id.btnPageUp)
        val btnPageDown =
            view.findViewById<Button>(R.id.btnPageDown)
        val btnScreenShot = view.findViewById<LinearLayout>(R.id.btnScreenShot)
        val btnKeyUp = view.findViewById<LinearLayout>(R.id.btnKeyUp)
        val btnKeyDown = view.findViewById<LinearLayout>(R.id.btnKeyDown)
        val btnKeyLeft = view.findViewById<LinearLayout>(R.id.btnKeyLeft)
        val btnKeyRight = view.findViewById<LinearLayout>(R.id.btnKeyRight)
        btnKeyUp.setOnTouchListener(this)
        btnKeyDown.setOnTouchListener(this)
        btnKeyLeft.setOnTouchListener(this)
        btnKeyRight.setOnTouchListener(this)
        btnF1.setOnTouchListener(this)
        btnF2.setOnTouchListener(this)
        btnF3.setOnTouchListener(this)
        btnF4.setOnTouchListener(this)
        btnF5.setOnTouchListener(this)
        btnF6.setOnTouchListener(this)
        btnF7.setOnTouchListener(this)
        btnF8.setOnTouchListener(this)
        btnF9.setOnTouchListener(this)
        btnF10.setOnTouchListener(this)
        btnF11.setOnTouchListener(this)
        btnF12.setOnTouchListener(this)
        btnRightMouse.setOnTouchListener(this)
        btnDelText.setOnTouchListener(this)
        btnEnter.setOnTouchListener(this)
        btnIns.setOnTouchListener(this)
        btnDel.setOnTouchListener(this)
        btnPageUp.setOnTouchListener(this)
        btnPageDown.setOnTouchListener(this)
        btnScreenShot.setOnTouchListener(this)
    }

    //TODO 需优化为点击事件
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (mIPopBottomSoftKeyCallback != null) {
            val id = v.id
            when (id) {
                R.id.btnKeyUp -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_DPAD_UP, v)
                }
                R.id.btnKeyDown -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_DPAD_DOWN, v)
                }
                R.id.btnKeyLeft -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_DPAD_LEFT, v)
                }
                R.id.btnKeyRight -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_DPAD_RIGHT, v)
                }
                R.id.btnF1 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F1, v)
                }
                R.id.btnF2 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F2, v)
                }
                R.id.btnF3 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F3, v)
                }
                R.id.btnF4 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F4, v)
                }
                R.id.btnF5 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F5, v)
                    //                mIPopBottomSoftKeyCallback.onTouch(event, KeyEvent.KEYCODE_F5, event.getAction());
                }
                R.id.btnF6 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F6, v)
                }
                R.id.btnF7 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F7, v)
                }
                R.id.btnF8 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F8, v)
                }
                R.id.btnF9 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F9, v)
                }
                R.id.btnF10 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F10, v)
                }
                R.id.btnF11 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F11, v)
                }
                R.id.btnF12 -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_F12, v)
                }
                R.id.btnRightMouse -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        mIPopBottomSoftKeyCallback!!.onMouseEvent(4)
                        v.isPressed = true
                    } else if (event.action == KeyEvent.ACTION_UP) {
                        mIPopBottomSoftKeyCallback!!.onMouseEvent(4)
                        v.isPressed = false
                    }
                    return true
                }
                R.id.btnDelText -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_DEL, v)
                }
                R.id.btnIns -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_INSERT, v)
                }
                R.id.btnDel -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_FORWARD_DEL, v)
                }
                R.id.btnEnter -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_ENTER, v)
                }
                R.id.btnScreenShot -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_SYSRQ, v)
                }
                R.id.btnPageUp -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_PAGE_UP, v)
                }
                R.id.btnPageDown -> {
                    return handlerTouchEvent(event, KeyEvent.KEYCODE_PAGE_DOWN, v)
                }
            }
        }
        return false
    }

    /**
     * 处理按下和松开事件
     *
     * @param event
     * @param keycodeDpadUp 键值
     * @param v
     */
    private fun handlerTouchEvent(
        event: MotionEvent,
        keycodeDpadUp: Int,
        v: View
    ): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            mIPopBottomSoftKeyCallback!!.onTouchDown(event, keycodeDpadUp)
            v.isPressed = true
            return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            mIPopBottomSoftKeyCallback!!.onTouchUp(event, keycodeDpadUp)
            v.isPressed = false
            return true
        }
        return false
    }

    init {
        @SuppressLint("InflateParams") val view =
            LayoutInflater.from(context).inflate(R.layout.pop_bottom_soft_key, null, false)
        initPopProperty(view, softKeyBoardHeight)
        initView(view)
    }
}