/*
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2009 Michael A. MacDonald
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */
package com.vesystem.opaque.bitmap

import android.graphics.*
import android.graphics.drawable.DrawableContainer

/**
 * @author Michael A. MacDonald
 */
open class KAbstractBitmapDrawable internal constructor(var data: KAbstractBitmapData) :
    DrawableContainer() {
    @JvmField
    var cursorRect: RectF?
    var hotX = 0
    var hotY = 0
    @JvmField
    var softCursor: Bitmap?
    var softCursorInit: Boolean
    var clipRect: Rect?
    var toDraw: Rect? = null
    var drawing = false
    @JvmField
    var _defaultPaint: Paint
    var _whitePaint: Paint
    var _blackPaint: Paint
    fun draw(canvas: Canvas, xoff: Int, yoff: Int) {
        try {
            canvas.drawBitmap(data.mbitmap!!, xoff.toFloat(), yoff.toFloat(), _defaultPaint)
            canvas.drawBitmap(softCursor!!, cursorRect!!.left, cursorRect!!.top, _defaultPaint)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun setCursorRect(
        x: Int,
        y: Int,
        w: Float,
        h: Float,
        hX: Int,
        hY: Int
    ) {
        hotX = hX
        hotY = hY
        cursorRect!!.left = x - hotX.toFloat()
        cursorRect!!.right = cursorRect!!.left + w
        cursorRect!!.top = y - hotY.toFloat()
        cursorRect!!.bottom = cursorRect!!.top + h
    }

    fun moveCursorRect(x: Int, y: Int) {
        setCursorRect(x, y, cursorRect!!.width(), cursorRect!!.height(), hotX, hotY)
    }

    fun setSoftCursor(newSoftCursorPixels: IntArray?) {
        val oldSoftCursor = softCursor
        softCursor = Bitmap.createBitmap(
            newSoftCursorPixels!!, cursorRect!!.width().toInt(),
            cursorRect!!.height().toInt(), Bitmap.Config.ARGB_8888
        )
        softCursorInit = true
        oldSoftCursor!!.recycle()
    }

    /* (non-Javadoc)
     * @see android.graphics.drawable.DrawableContainer#getIntrinsicHeight()
     */
    override fun getIntrinsicHeight(): Int {
        return data.framebufferheight
    }

    /* (non-Javadoc)
     * @see android.graphics.drawable.DrawableContainer#getIntrinsicWidth()
     */
    override fun getIntrinsicWidth(): Int {
        return data.framebufferwidth
    }

    /* (non-Javadoc)
     * @see android.graphics.drawable.DrawableContainer#getOpacity()
     */
    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    /* (non-Javadoc)
     * @see android.graphics.drawable.DrawableContainer#isStateful()
     */
    override fun isStateful(): Boolean {
        return false
    }

    fun dispose() {
        drawing = false
        if (softCursor != null) softCursor!!.recycle()
        softCursor = null
        cursorRect = null
        clipRect = null
        toDraw = null
    }

    fun startDrawing() {
        drawing = true
    }

    init {
        cursorRect = RectF()
        clipRect = Rect()
        // Try to free up some memory.
        System.gc()
        softCursor = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        softCursorInit = false
        _defaultPaint = Paint()
        _defaultPaint.isFilterBitmap = true
        _whitePaint = Paint()
        _whitePaint.color = -0x1
        _blackPaint = Paint()
        _blackPaint.color = -0x1000000
    }
}