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

    private val TAG = "KTVPointer"
    var signMouseOperation = 0
    private var mouseX: Int = 0
    private var mouseY: Int = 0


    //TODO 没有实时变化是没有reDraw更新UI
    fun downMouseRightButton() {
        signMouseOperation = SPICE_MOUSE_BUTTON_RIGHT
        mouseOption.handlerMouseEvent(mouseX, mouseY, 0, SPICE_MOUSE_BUTTON_RIGHT, false)
    }

    fun upMouseRightButton() {
        signMouseOperation = 0
        mouseOption.handlerMouseEvent(mouseX, mouseY, 0, SPICE_MOUSE_BUTTON_RIGHT, false)
    }


    fun onTouchEvent(event: MotionEvent): Boolean {
//        Log.i(TAG, "onTouchEvent: ${event.action}")
        mouseX = event.x.toInt()
        mouseY = event.y.toInt()
        val metaState = event.metaState
        when (event.action) {
            MotionEvent.ACTION_SCROLL -> {
                Log.i(TAG, "onTouchEvent: 中间键滚动")
                val axisValue = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
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
            MotionEvent.ACTION_BUTTON_PRESS -> {
                Log.i(TAG, "onTouchEvent: 按下")
                signMouseOperation = SPICE_MOUSE_BUTTON_LEFT
                mouseOption.handlerMouseEvent(
                    mouseX,
                    mouseY,
                    metaState,
                    SPICE_MOUSE_BUTTON_LEFT,
                    false
                )
            }
            MotionEvent.ACTION_BUTTON_RELEASE -> {
                Log.i(TAG, "onTouchEvent: 松开")
                signMouseOperation = 0
                mouseOption.handlerMouseEvent(
                    mouseX,
                    mouseY,
                    metaState,
                    SPICE_MOUSE_BUTTON_LEFT,
                    false
                )
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                Log.i(TAG, "onTouchEvent: 按下移动：X:${event.x},Y:${event.y}")
                /* signMouseOperation = SPICE_MOUSE_BUTTON_LEFT
                 mouseOption.handlerMouseEvent(
                     mouseX,
                     mouseY,
                     metaState,
                     SPICE_MOUSE_BUTTON_LEFT,
                     true
                 )*/
                return true
            }
            MotionEvent.ACTION_HOVER_MOVE -> {
                Log.i(TAG, "onTouchEvent: 鼠标移动X:${event.x},Y:${event.y}")
                signMouseOperation = SPICE_MOUSE_BUTTON_MOVE
                mouseOption.mouseMove(mouseX, mouseY, metaState, SPICE_MOUSE_BUTTON_MOVE, false)
                return true
            }
            /*MotionEvent.ACTION_HOVER_ENTER -> {
                signMouseOperation = 0
                mouseOption.releaseMouseEvent(mouseX, mouseY, metaState, signMouseOperation, false)
                Log.i(TAG, "onTouchEvent: hover进入释放之前移动鼠标")
            }*/

        }
        return false
    }


    companion object {
        const val MOUSE_BUTTON_MOVE = 0x0800
        const val POINTER_DOWN_MASK = 0x8000
        const val SPICE_MOUSE_BUTTON_MOVE = 0
        const val SPICE_MOUSE_BUTTON_LEFT = 1
        const val SPICE_MOUSE_BUTTON_MIDDLE = 2
        const val SPICE_MOUSE_BUTTON_RIGHT = 3
        const val SPICE_MOUSE_MIDDLE_SCROLL_UP = 4
        const val SPICE_MOUSE_MIDDLE_SCROLL_DOWN = 5
    }

}