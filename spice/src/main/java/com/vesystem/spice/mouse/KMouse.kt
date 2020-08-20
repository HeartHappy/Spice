package com.vesystem.spice.mouse

import android.app.Service
import android.content.Context
import android.os.Vibrator
import android.view.MotionEvent


/**
 * Created Date 2020/7/24.
 * @author ChenRui
 * ClassDescription:
 */
abstract class KMouse(
    val context: Context,
    val mouseOption: IMouseOperation
) {
    protected var vibrator: Vibrator? = null
    protected var isZoom: Boolean = false//是否为手势缩放
    protected var pressedX: Int = 0 //首次按下的点x坐标
    protected var pressedY: Int = 0//首次按下的点y坐标
    protected var isDoubleDown = false//判断双指按下和双指松开
    private var mouseMaxX: Int = context.resources.displayMetrics.widthPixels
    private var mouseMaxY: Int = context.resources.displayMetrics.heightPixels

    var mouseX: Int = 0
        //鼠标的绝对X坐标
        get() = if (field <= 0) 0 else if (field >= mouseMaxX) mouseMaxX - 1 else field
    var mouseY: Int = 0
        //鼠标的绝对Y坐标
        get() = if (field <= 0) 0 else if (field >= mouseMaxY) mouseMaxY - 1 else field


    init {
        vibrator = context.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
    }


    fun downMouseRightButton() {
        mouseOption.handlerMouseEvent(mouseX, mouseY, 0, SPICE_MOUSE_BUTTON_RIGHT, false)
    }

    fun upMouseRightButton() {
        mouseOption.releaseMouseEvent(mouseX, mouseY, 0, SPICE_MOUSE_BUTTON_RIGHT, false)
    }

    fun downMouseRightButton(mouseX: Int, mouseY: Int) {
        mouseOption.handlerMouseEvent(mouseX, mouseY, 0, SPICE_MOUSE_BUTTON_RIGHT, false)
    }

    fun upMouseRightButton(mouseX: Int, mouseY: Int) {
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