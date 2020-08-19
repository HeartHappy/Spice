package com.vesystem.spice.mouse

import android.content.Context
import android.util.Log
import android.view.MotionEvent

/**
 * Created Date 2020/7/13.
 * @author ChenRui
 * ClassDescription:TV端得鼠标操作
 */
class KTVMouse(
    context: Context,
    mouseOption: IMouseOperation
) :
    KMouse(context, mouseOption) {


    @Suppress("DEPRECATION")
    override fun onTouchEvent(event: MotionEvent, dx: Int, dy: Int): Boolean {

        try {
            mouseX = if ((event.x.toInt() - dx) > 0) event.x.toInt() - dx else event.x.toInt()
            mouseY = if ((event.y.toInt() - dy) > 0) event.y.toInt() - dy else event.y.toInt()
            Log.d(
                TAG,
                "onTouchEvent:x: ${event.x},Y:${event.y},mouseX:${mouseX},mouseY:${mouseY}，$dx,$dy"
            )
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
                MotionEvent.ACTION_DOWN -> {
//                    Log.i(TAG, "onTouchEvent: 按下")
                    this.pressedX = event.x.toInt()
                    this.pressedY = event.y.toInt()
                    Log.i(TAG, "onTouchEvent: $mouseX,$mouseY,$dx,$dy,按下点$pressedX,$pressedY")
                    mouseOption.handlerMouseEvent(
                        mouseX-dx,
                        mouseY-dy,
                        metaState,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
//                    Log.i(TAG, "onTouchEvent: 按下移动：X:${event.x},Y:${event.y}")
                    val pdx = event.x - this.pressedX
                    val pdy = event.y - this.pressedY
                    if(isDoubleDown && (event.pointerCount == 2 && kotlin.math.abs(pdx) > 5 && kotlin.math.abs(pdy) > 5)){
                        //双指按下并且移动了
                        isTranslation = true
                        return false
                    }
                   /*
                    if (isDoubleDown) {
//                        Log.i(TAG, "onTouchEvent: 双指触摸平移，只松开了一个手指")
                        return true
                    }*/
                    mouseOption.mouseDownMove(
                        mouseX-dx,
                        mouseY-dy,
                        metaState,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    return true
                }

                MotionEvent.ACTION_UP -> {
//                    Log.i(TAG, "onTouchEvent: 松开")
                    isDoubleDown = false//双指都松开了
                    mouseOption.releaseMouseEvent(
                        mouseX-dx,
                        mouseY-dy,
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
                MotionEvent.ACTION_POINTER_DOWN->{
                    isDoubleDown = true
                    return false
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (isTranslation) {
                        isTranslation = false
//                        Log.i(TAG, "onTouchEvent: 双指触摸平移松开")
                        return true
                    }
                    vibrator?.vibrate(100)
                    downMouseRightButton()
                    upMouseRightButton()
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

}