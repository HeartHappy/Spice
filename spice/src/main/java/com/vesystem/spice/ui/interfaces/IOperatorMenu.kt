package com.vesystem.spice.ui.interfaces

import android.view.View

/**
 * Created Date 2020-01-13.
 *
 * @author ChenRui
 * ClassDescription：
 */
interface IOperatorMenu {
    //触屏点击
    fun touchClick(view: View)

    //触摸移动
    fun touchMove(view: View)

    //拖拽选择
    fun dragSelect(view: View)
}