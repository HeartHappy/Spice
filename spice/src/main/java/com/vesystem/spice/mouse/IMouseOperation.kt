package com.vesystem.spice.mouse

/**
 * Created Date 2020/7/15.
 * @author ChenRui
 * ClassDescription:TV端鼠标操作：1、左按钮单击、双击   2、中间按钮滚动   3、右侧按钮     4、上滚动   5、下滚动   6、重置
 */
interface IMouseOperation {


    /**
     * 发送鼠标事件，包括按下左键、右键、中间键上滑动、下滑动、右键。发送 key和 mask
     * x、y:鼠标的绝对x轴坐标
     *
     */
    fun handlerMouseEvent(
        x: Int,
        y: Int,
        metaState: Int,
        mouseType: Int,
        isMove: Boolean
    )


    /**
     * 鼠标按下移动
     * x、y:鼠标的绝对x轴坐标
     */
    fun mouseDownMove(
        x: Int,
        y: Int,
        metaState: Int,
        mouseType: Int,
        isMove: Boolean
    )

    /**
     * 鼠标移动
     * x、y:鼠标的绝对x轴坐标
     */
    fun mouseMove(
        x: Int,
        y: Int,
        metaState: Int,
        mouseType: Int,
        isMove: Boolean
    )

    /**
     * 释放鼠标事件 重置key和 mask
     * x、y:鼠标的绝对x轴坐标
     */
    fun releaseMouseEvent(
        x: Int,
        y: Int,
        metaState: Int,
        mouseType: Int,
        isMove: Boolean
    )
}