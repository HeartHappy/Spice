package com.vesystem.ui.interfaces;

import android.view.View;

/**
 * Created Date 2020-01-13.
 *
 * @author ChenRui
 * ClassDescription：
 */
public interface IOperatorMenu {

    //触屏点击
    void touchClick(View view);

    //触摸移动
    void touchMove(View view);

    //拖拽选择
    void dragSelect(View view);

}
