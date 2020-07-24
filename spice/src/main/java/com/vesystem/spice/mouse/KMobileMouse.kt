package com.vesystem.spice.mouse

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent

/**
 * Created Date 2020/7/24.
 * @author ChenRui
 * ClassDescription:触摸移动鼠标处理（手机端使用）
 */
class KMobileMouse(context: Context, mouseOption: IMouseOperation) :
    KMouse(context, mouseOption) {
    var gd: GestureDetector? = null
    var dx: Int = 0
    var dy: Int = 0
    var relativeX: Int = 0//相对于鼠标的绝对坐标偏移量X
    var relativeY: Int = 0//相对于鼠标的绝对坐标偏移量Y
    var isLongPress: Boolean = false

    init {
        gd = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                Log.i(TAG, "onSingleTapUp: 抬起")
                return true
            }


            override fun onDown(e: MotionEvent?): Boolean {
                Log.i(TAG, "onDown: 按下")
                return true
            }


            override fun onLongPress(e: MotionEvent) {
                Log.i(TAG, "onLongPress: 长按")
                isLongPress = true
                mouseOption.handlerMouseEvent(
                    mouseX,
                    mouseY,
                    e.metaState,
                    SPICE_MOUSE_BUTTON_LEFT,
                    false
                )
            }

        })
    }

    override fun onTouchEvent(event: MotionEvent, dx: Int, dy: Int): Boolean {
        gd?.onTouchEvent(event)
        Log.i(TAG, "onTouchEvent: 触发长按了${event.actionMasked}")
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                this.dx = event.x.toInt()
                this.dy = event.y.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                mouseX = (event.x - this.dx + relativeX).toInt()
                mouseY = (event.y - this.dy + relativeY).toInt()
                if (isLongPress) {
                    mouseOption.mouseDownMove(
                        mouseX,
                        mouseY,
                        event.metaState,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                } else {
                    mouseOption.mouseMove(
                        mouseX,
                        mouseY,
                        event.metaState,
                        SPICE_MOUSE_BUTTON_MOVE,
                        false
                    )
                }

                return true
            }
            MotionEvent.ACTION_UP -> {
                relativeX = mouseX
                relativeY = mouseY
                //松开时小于5px代表是按下未移动
                if(isLongPress){
                    mouseOption.releaseMouseEvent(
                        mouseX,
                        mouseY,
                        0,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    isLongPress = false
                    return true
                }
                if ((event.x - this.dx) < 5 && (event.y - this.dy) < 5) {
                    mouseOption.handlerMouseEvent(
                        mouseX,
                        mouseY,
                        event.metaState,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    mouseOption.releaseMouseEvent(
                        mouseX,
                        mouseY,
                        0,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                }

                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                downMouseRightButton()
                upMouseRightButton()
                return true
            }
        }
        return false
    }
}