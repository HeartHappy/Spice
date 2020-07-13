package com.vesystem.spice.interfaces

/**
 * Created Date 2020/7/7.
 * @author ChenRui
 * ClassDescription:
 */
interface KSpiceConnect {
    fun onUpdateBitmapWH(width: Int, height: Int)

    fun onUpdateBitmap(x: Int, y: Int, width: Int, height: Int)

    fun onMouseUpdate(x: Int, y: Int)

    fun onMouseMode(relative: Boolean)

    fun onConnectSucceed()
}