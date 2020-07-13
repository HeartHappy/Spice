package com.vesystem.spice.bitmap

import android.graphics.PixelFormat
import android.graphics.drawable.DrawableContainer

/**
 * Created Date 2020/7/8.
 * @author ChenRui
 * ClassDescription:自定义Drawable属性
 */
class KCanvasDrawable(var width: Int, var height: Int) : DrawableContainer() {

    override fun getIntrinsicHeight(): Int {
        return height
    }

    override fun getIntrinsicWidth(): Int {
        return width
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun isStateful(): Boolean {
        return false
    }
}