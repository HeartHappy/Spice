package com.vesystem.spice.inputmethod

import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection


/**
 * Created Date 2020/11/12.
 * @author ChenRui
 * ClassDescription:继承BaseInputConnection方法，重写deleteSurroundingText，拦截删除事件
 */
class InterceptInputConnection(target: View, mutable: Boolean) :
    BaseInputConnection(target, mutable) {
    private var mBackspaceListener: BackspaceListener? = null

    fun setBackspaceListener(backspaceListener: BackspaceListener?) {
        mBackspaceListener = backspaceListener
    }

    /**
     * 当软键盘删除文本之前，会调用这个方法通知输入框，我们可以重写这个方法并判断是否要拦截这个删除事件。
     * 在谷歌输入法上，点击退格键的时候不会调用{@link #sendKeyEvent(KeyEvent event)}，
     * 而是直接回调这个方法，所以也要在这个方法上做拦截；
     * */
    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
//        Log.d("Intercept", "deleteSurroundingText: ")
        mBackspaceListener?.let {
            it.onBackspace()
            return true
        }
        return super.deleteSurroundingText(beforeLength, afterLength)
    }


    override fun sendKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
//            Log.d("Intercept", "sendKeyEvent: 拦截到了删除按键：${event.keyCode}")
            mBackspaceListener?.let {
                it.onBackspace()
                return true
            }
        }
        return super.sendKeyEvent(event)
    }

    interface BackspaceListener {
        /**
         * @return true 代表消费了这个事件
         * */
        fun onBackspace(): Boolean
    }
}