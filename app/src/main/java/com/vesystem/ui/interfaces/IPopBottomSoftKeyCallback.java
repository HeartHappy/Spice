package com.vesystem.ui.interfaces;


import android.view.MotionEvent;

/**
 * Created Date 2020-01-16.
 *
 * @author ChenRui
 * ClassDescription：底部自定义软键盘触摸事件接口回调
 */
public interface IPopBottomSoftKeyCallback {
    //自定义底部软键盘按下回调
    void onTouchDown(MotionEvent event, int keyCode);

    //自定义底部软键盘松开回调
    void onTouchUp(MotionEvent event, int keyCode);

    //自定义底部软键盘鼠标右键回调
    void onMouseEvent(int mouseType);
}
