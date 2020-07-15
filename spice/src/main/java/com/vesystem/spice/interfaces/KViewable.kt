package com.vesystem.spice.interfaces


interface KViewable {
    fun reallocateDrawable(width: Int, height: Int)
    fun reDraw(x: Int, y: Int, width: Int, height: Int)
    fun setMousePointerPosition(x: Int, y: Int)
    fun mouseMode(relative: Boolean)
}