package com.vesystem.spice.zoom

/**
 * Created Date 2020/7/27.
 * @author ChenRui
 * ClassDescription:
 */
interface IZoom {
    fun zoom(scale: Float)
    //平移
    fun translation(dx: Int, dy: Int)
    //平移之前校验
    fun translationBeforeLimit(): Boolean

    //平移结束之后校验
    fun translationAfterLimit()

}