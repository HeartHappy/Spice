package com.vesystem.spice.mouse

import android.content.Context
import android.util.Log
import android.view.MotionEvent

/**
 * Created Date 2020/7/13.
 * @author ChenRui
 * ClassDescription:TV端得鼠标操作
 */
class KTVMouse(val context: Context, private val mouseOption: IMouseOperation) {
    private var mouseX: Int = 0
    private var mouseY: Int = 0

    fun downMouseRightButton() {
        mouseOption.handlerMouseEvent(mouseX, mouseY, 0, SPICE_MOUSE_BUTTON_RIGHT, false)
    }

    fun upMouseRightButton() {
        mouseOption.releaseMouseEvent(mouseX, mouseY, 0, SPICE_MOUSE_BUTTON_RIGHT, false)
    }


    fun onTouchEvent(event: MotionEvent, dx: Int, dy: Int): Boolean {
//        Log.i(TAG, "onTouchEvent 参数: ${event.action},${event.actionMasked},${event.buttonState}")
        try {
            mouseX = if ((event.x.toInt() - dx) > 0) event.x.toInt() - dx else event.x.toInt()
            mouseY = if ((event.y.toInt() - dy) > 0) event.y.toInt() - dy else event.y.toInt()
            val metaState = event.metaState
            when (event.actionMasked) {
                MotionEvent.ACTION_SCROLL -> {
//                Log.i(TAG, "onTouchEvent: 中间键滚动")
                    val axisValue = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    val signMouseOperation: Int
                    if (axisValue < 0) {
                        //下滑
                        signMouseOperation = SPICE_MOUSE_MIDDLE_SCROLL_DOWN
                        mouseOption.handlerMouseEvent(
                            mouseX,
                            mouseY,
                            metaState,
                            SPICE_MOUSE_MIDDLE_SCROLL_DOWN,
                            false
                        )
                    } else {
                        //上滑
                        signMouseOperation = SPICE_MOUSE_MIDDLE_SCROLL_UP
                        mouseOption.handlerMouseEvent(
                            mouseX,
                            mouseY,
                            metaState,
                            SPICE_MOUSE_MIDDLE_SCROLL_UP,
                            false
                        )
                    }
                    mouseOption.releaseMouseEvent(mouseX, mouseY, 0, signMouseOperation, false)
                    return true
                }
                /*MotionEvent.ACTION_BUTTON_PRESS -> {
                    Log.i(TAG, "onTouchEvent: 按下")
                    mouseOption.handlerMouseEvent(
                        mouseX,
                        mouseY,
                        metaState,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                }*/
                MotionEvent.ACTION_MOVE -> {
                Log.i(TAG, "onTouchEvent: 按下移动：X:${event.x},Y:${event.y}")
                    mouseOption.mouseDownMove(
                        mouseX,
                        mouseY,
                        metaState,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    return true
                }
                /*MotionEvent.ACTION_BUTTON_RELEASE -> {
                Log.i(TAG, "onTouchEvent: 松开")
                    mouseOption.releaseMouseEvent(
                        mouseX,
                        mouseY,
                        0,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    return true
                }*/
                MotionEvent.ACTION_DOWN->{
                    Log.i(TAG, "onTouchEvent: 按下")
                    mouseOption.handlerMouseEvent(
                        mouseX,
                        mouseY,
                        metaState,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    return true
                }
                MotionEvent.ACTION_UP->{
                    Log.i(TAG, "onTouchEvent: 松开")
                    mouseOption.releaseMouseEvent(
                        mouseX,
                        mouseY,
                        0,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    return true
                }
                MotionEvent.ACTION_HOVER_MOVE -> {
                Log.i(TAG, "onTouchEvent: 鼠标移动X:${event.x},Y:${event.y}")
                    mouseOption.mouseMove(
                        mouseX,
                        mouseY,
                        metaState,
                        SPICE_MOUSE_BUTTON_MOVE,
                        false
                    )
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }


    companion object {
        //        const val MOUSE_BUTTON_MOVE = 0x0800
        const val POINTER_DOWN_MASK = 0x8000
        const val SPICE_MOUSE_BUTTON_MOVE = 0
        const val SPICE_MOUSE_BUTTON_LEFT = 1

        //        const val SPICE_MOUSE_BUTTON_MIDDLE = 2
        const val SPICE_MOUSE_BUTTON_RIGHT = 3
        const val SPICE_MOUSE_MIDDLE_SCROLL_UP = 4
        const val SPICE_MOUSE_MIDDLE_SCROLL_DOWN = 5
        private const val TAG = "KTVMouse"
    }

}