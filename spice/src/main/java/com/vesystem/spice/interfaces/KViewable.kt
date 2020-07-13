package com.vesystem.spice.interfaces

import android.graphics.Bitmap

interface KViewable {
    //    void waitUntilInflated();
    val desiredWidth: Int
    val desiredHeight: Int
    fun reallocateDrawable(width: Int, height: Int)
    val bitmap: Bitmap?
    fun reDraw(x: Int, y: Int, width: Int, height: Int)
    fun setMousePointerPosition(x: Int, y: Int)
    fun mouseMode(relative: Boolean)
}