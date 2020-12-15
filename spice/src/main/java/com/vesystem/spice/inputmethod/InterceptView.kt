package com.vesystem.spice.inputmethod

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

/**
 * Created Date 2020/11/12.
 * @author ChenRui
 * ClassDescription:拦截EditText输入法，可在InterceptInputConnection中做任意拦截
 */
class InterceptView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var deleteListener: InterceptInputConnection.BackspaceListener? = null

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val myInputConnection = InterceptInputConnection(this, false)
        myInputConnection.setBackspaceListener(object : InterceptInputConnection.BackspaceListener {
            override fun onBackspace(): Boolean {
                deleteListener?.onBackspace()
                return true
            }
        })
        return myInputConnection
    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    fun initInputConnection(deleteListener: InterceptInputConnection.BackspaceListener) {
        //1、自定义如果没有自带焦点（例如：EditText），否则需要请求获取焦点requestFocus()
        requestFocus()
        //2、显示软键盘
        showSysKeyboard()
        //3、再onCreateConnect连接后，设置回调监听
        this.deleteListener = deleteListener
    }

    /**
     * 显示、隐藏系统键盘
     */
    private fun showSysKeyboard() {
        val mgr =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mgr.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
}