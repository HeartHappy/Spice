package com.vesystem.spice.ui.interfaces

import android.view.MotionEvent

/**
 * Created Date 2020-01-16.
 *
 * @author ChenRui
 * ClassDescription：底部自定义软键盘触摸事件接口回调
 */
interface KIPopBottomSoftKeyCallback {
    //自定义底部软键盘按下回调
    fun onTouchDown(event: MotionEvent, keyCode: Int)

    //自定义底部软键盘松开回调
    fun onTouchUp(event: MotionEvent, keyCode: Int)

    //自定义底部软键盘鼠标右键回调
    fun onMouseEvent(mouseType: Int)
}