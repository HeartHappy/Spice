package com.vesystem.spice.mouse

/**
 * Created Date 2020/7/15.
 * @author ChenRui
 * ClassDescription:TV端鼠标操作：1、左按钮单击、双击   2、中间按钮滚动   3、右侧按钮     4、上滚动   5、下滚动   6、重置
 */
interface IMouseOperation {

    fun leftButtonDown(x: Int, y: Int, metaState: Int, mouseType: Int)

    fun leftButtonUp(x: Int, y: Int, metaState: Int, mouseType: Int)

    fun middleButtonDown(x: Int, y: Int, metaState: Int)

    fun rightButtonDown(x: Int, y: Int, metaState: Int, mouseType: Int)

    fun rightButtonUp(x: Int, y: Int, metaState: Int, mouseType: Int)

    fun scrollUp(x: Int, y: Int, metaState: Int)

    fun scrollDown(x: Int, y: Int, metaState: Int)

    fun releaseButton(x: Int, y: Int, metaState: Int)
}