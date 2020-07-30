package com.vesystem.spice.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.widget.FrameLayout

/**
 * Created Date 2020/7/29.
 * @author ChenRui
 * ClassDescription:
 */
class KEventFragment(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        Log.i("KEventFragment", "dispatchKeyEvent: ")
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchKeyEventPreIme(event: KeyEvent?): Boolean {
        Log.i("KEventFragment", "dispatchKeyEventPreIme: ")
        return super.dispatchKeyEventPreIme(event)
    }

    override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
        Log.i("KEventFragment", "dispatchKeyShortcutEvent: ")
        return super.dispatchKeyShortcutEvent(event)
    }
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        Log.i("KEventFragment", "onKeyPreIme: ")
        return super.onKeyPreIme(keyCode, event)
    }
}