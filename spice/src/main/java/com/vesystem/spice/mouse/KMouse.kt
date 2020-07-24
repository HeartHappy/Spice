package com.vesystem.spice.mouse

import android.content.Context
import android.view.MotionEvent

/**
 * Created Date 2020/7/24.
 * @author ChenRui
 * ClassDescription:
 */
abstract class KMouse(val context: Context, val mouseOption: IMouseOperation) {
    var mouseX: Int = 0
        //鼠标的绝对X坐标
        get() = if (field <= 0) 0 else if (field >= context.resources.displayMetrics.widthPixels) context.resources.displayMetrics.widthPixels-1 else field
    var mouseY: Int = 0//鼠标的绝对Y坐标
        get() = if (field <= 0) 0 else if (field >= context.resources.displayMetrics.heightPixels) context.resources.displayMetrics.heightPixels-1 else field


    fun downMouseRightButton() {
        mouseOption.handlerMouseEvent(mouseX, mouseY, 0, SPICE_MOUSE_BUTTON_RIGHT, false)
    }

    fun upMouseRightButton() {
        mouseOption.releaseMouseEvent(mouseX, mouseY, 0, SPICE_MOUSE_BUTTON_RIGHT, false)
    }

    abstract fun onTouchEvent(event: MotionEvent, dx: Int, dy: Int): Boolean

    companion object {
        //        const val MOUSE_BUTTON_MOVE = 0x0800
        const val POINTER_DOWN_MASK = 0x8000
        const val SPICE_MOUSE_BUTTON_MOVE = 0
        const val SPICE_MOUSE_BUTTON_LEFT = 1

        //        const val SPICE_MOUSE_BUTTON_MIDDLE = 2
        const val SPICE_MOUSE_BUTTON_RIGHT = 3
        const val SPICE_MOUSE_MIDDLE_SCROLL_UP = 4
        const val SPICE_MOUSE_MIDDLE_SCROLL_DOWN = 5
        const val TAG = "KMouse"
    }

}