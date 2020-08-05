@file:Suppress("DEPRECATION")

package com.vesystem.spice.mouse

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import com.vesystem.spice.zoom.IZoom

/**
 * Created Date 2020/7/24.
 * @author ChenRui
 * ClassDescription:触摸移动鼠标处理（手机端使用）
 */
class KMobileMouse(
    context: Context,
    mouseOption: IMouseOperation,
    private val iZoom: IZoom
) :
    KMouse(context, mouseOption) {
    private var gd: GestureDetector? = null

    //    private var sgd: ScaleGestureDetector? = null

    private var relativeX: Int = 0//原鼠标位置，绝对X坐标
    private var relativeY: Int = 0//原鼠标位置，绝对Y坐标
    private var isLongPress: Boolean = false//是否长按
    private var dx = 0 //桌面偏移屏幕距离
    private var dy = 0


    init {

        gd = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(event: MotionEvent) {
                //单指长按
                if (event.pointerCount == 1 && !isTranslation) {
//                    Log.i(TAG, "onLongPress: 长按")
                    vibrator?.vibrate(100)
                    isLongPress = true
                    mouseOption.handlerMouseEvent(
                        mouseX - dx,
                        mouseY - dy,
                        event.metaState,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                }
            }
        })
    }

    /**
     * dx、dy：画面距离屏幕边缘的偏移量
     */
    override fun onTouchEvent(event: MotionEvent, dx: Int, dy: Int): Boolean {
        this.dx = dx
        this.dy = dy
//        Log.i(TAG, "onTouchEvent: ${event.actionMasked},${event.buttonState}")

//        if (event.pointerCount == 1) {
        gd?.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                this.pressedX = event.x.toInt()
                this.pressedY = event.y.toInt()
            }
            MotionEvent.ACTION_MOVE -> {

                val pdx = event.x - this.pressedX
                val pdy = event.y - this.pressedY
                if (event.pointerCount == 2 && kotlin.math.abs(pdx) > 5 && kotlin.math.abs(pdy) > 5) {
//                    Log.i(TAG, "onTouchEvent: 双指触摸平移画面$pdx，$pdy")
                    isTranslation = true
                    iZoom.translation(pdx.toInt(), pdy.toInt())
                    return true
                }
                if (isDoubleDown) {
//                    Log.i(TAG, "onTouchEvent: 双指触摸平移，只松开了一个手指")
                    return true
                }

                mouseX = (event.x - this.pressedX + relativeX).toInt()
                mouseY = (event.y - this.pressedY + relativeY).toInt()
                if (isLongPress) {
                    mouseOption.mouseDownMove(
                        mouseX - dx,
                        mouseY - dy,
                        event.metaState,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    /* Log.i(
                         TAG,
                         "onTouchEvent: 按下移动,鼠标位置：$mouseX,$mouseY,画布偏移屏幕距离：$dx,$dy"
                     )*/
                } else {
                    mouseOption.mouseMove(
                        mouseX - dx,
                        mouseY - dy,
                        event.metaState,
                        SPICE_MOUSE_BUTTON_MOVE,
                        false
                    )
                    /*Log.i(
                        TAG,
                        "onTouchEvent: 移动,鼠标位置：$mouseX,$mouseY,画布偏移屏幕距离：$dx,$dy,相对屏幕：${event.rawX},${event.rawY}"
                    )*/
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                isDoubleDown = false//双指都松开了
                relativeX = mouseX
                relativeY = mouseY
//                Log.i(TAG, "onTouchEvent: 单指松开$isTranslation")
                if (isLongPress) {
                    mouseOption.releaseMouseEvent(
                        mouseX - dx,
                        mouseY - dy,
                        0,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    isLongPress = false
//                    Log.i(TAG, "onTouchEvent: 单指长按松开")
                    return true
                }
                //松开时小于5px代表是按下未移动
                if (kotlin.math.abs((event.x - this.pressedX)) < 5 && kotlin.math.abs((event.y - this.pressedY)) < 5) {
                    mouseOption.handlerMouseEvent(
                        mouseX - dx,
                        mouseY - dy,
                        event.metaState,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    mouseOption.releaseMouseEvent(
                        mouseX - dx,
                        mouseY - dy,
                        0,
                        SPICE_MOUSE_BUTTON_LEFT,
                        false
                    )
                    /* Log.i(
                         TAG,
                         "onTouchEvent: 单击${event.x - this.pressedX},${event.y - this.pressedY}"
                     )*/
                }

                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                isDoubleDown = true
            }
            //双指松开，没有移动则是右键、移动则是缩放操作
            MotionEvent.ACTION_POINTER_UP -> {
                //如果是双指拖放松开
                if (isTranslation) {
                    isTranslation = false
                    iZoom.translationAfterLimit()
//                    Log.i(TAG, "onTouchEvent: 双指触摸平移松开")
                    return true
                }
                vibrator?.vibrate(100)
                downMouseRightButton(mouseX - dx, mouseY - dy)
                upMouseRightButton(mouseX - dx, mouseY - dy)
//                Log.i(TAG, "onTouchEvent: 双指屏幕固定位置松开，即右键弹出菜单")
                return true
            }
        }
        return false
    }
}