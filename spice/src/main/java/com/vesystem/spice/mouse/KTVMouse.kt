package com.vesystem.spice.mouse

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent

/**
 * Created Date 2020/7/13.
 * @author ChenRui
 * ClassDescription:TV端得鼠标操作
 */
class KTVMouse(val context: Context, private val mouseOption: IMouseOperation) :
    GestureDetector.SimpleOnGestureListener() {

    private val TAG = "KTVPointer"
    var gestureDetector: GestureDetector? = null
    var signMouseOperation = 0
    var isDouble = false
    //TODO 区分鼠标的移动是否按下和鼠标的hover移动，还存在问题
    var isDown = false

    init {
        gestureDetector = GestureDetector(context, this)
    }


    fun downMouseRightButton(x: Int, y: Int) {
        signMouseOperation = SPICE_MOUSE_BUTTON_RIGHT
        mouseOption.rightButtonDown(x, y, 0, SPICE_MOUSE_BUTTON_RIGHT)
    }

    fun upMouseRightButton(mouseX: Int, mouseY: Int) {
        mouseOption.rightButtonUp(mouseX, mouseY, 0, SPICE_MOUSE_BUTTON_RIGHT)
    }


    override fun onSingleTapUp(e: MotionEvent): Boolean {
        Log.i(TAG, "onSingleTapUp: ")
        signMouseOperation = 0
        mouseOption.leftButtonUp(e.x.toInt(), e.y.toInt(), e.metaState, SPICE_MOUSE_BUTTON_LEFT)
        isDown = false
        return true
    }

    override fun onDown(e: MotionEvent): Boolean {
        if (!isDouble) {
            isDown = true
            signMouseOperation = SPICE_MOUSE_BUTTON_LEFT
            mouseOption.leftButtonDown(
                e.x.toInt(),
                e.y.toInt(),
                e.metaState,
                SPICE_MOUSE_BUTTON_LEFT
            )
            Log.i(TAG, "onDown: ")
        } else {
            isDouble = false
        }
        return true
    }


    override fun onDoubleTap(e: MotionEvent): Boolean {
        signMouseOperation = SPICE_MOUSE_BUTTON_LEFT
        mouseOption.leftButtonDown(e.x.toInt(), e.y.toInt(), e.metaState, SPICE_MOUSE_BUTTON_LEFT)
        mouseOption.leftButtonUp(e.x.toInt(), e.y.toInt(), e.metaState, SPICE_MOUSE_BUTTON_LEFT)
        signMouseOperation = 0
        isDouble = true
        Log.i(TAG, "onDoubleTap: ")
        return false
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
//        Log.i(TAG, "onFling: ")
        return true
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
//        Log.i(TAG, "onScroll: ")
        /*mouseOption.leftButtonDown(
            e1.x.toInt(),
            e1.y.toInt(),
            e1.metaState,
            SPICE_MOUSE_BUTTON_LEFT
        )*/
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
        Log.i(TAG, "onLongPress: ")
    }


    companion object {
        const val MOUSE_BUTTON_MOVE = 0x0800
        const val POINTER_DOWN_MASK = 0x8000
        const val SPICE_MOUSE_BUTTON_MOVE = 0
        const val SPICE_MOUSE_BUTTON_LEFT = 1
        const val SPICE_MOUSE_BUTTON_MIDDLE = 2
        const val SPICE_MOUSE_BUTTON_RIGHT = 3
        const val SPICE_MOUSE_BUTTON_UP = 4
        const val SPICE_MOUSE_BUTTON_DOWN = 5
    }
}