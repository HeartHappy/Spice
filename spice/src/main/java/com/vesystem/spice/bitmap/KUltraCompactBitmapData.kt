/*
 * Copyright (C) 2019 Iordan Iordanov
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
package com.vesystem.spice.bitmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.vesystem.spice.interfaces.KSpiceConnectable

class KUltraCompactBitmapData(
    rfb: KSpiceConnectable?,
    trueColor: Boolean,
    width: Int,
    height: Int
) : KAbstractBitmapData(rfb!!) {
    var cfg = Bitmap.Config.RGB_565

    internal inner class CompactBitmapDrawable :
        KAbstractBitmapDrawable(this@KUltraCompactBitmapData) {
        override fun draw(canvas: Canvas) {
            try {
                synchronized(mbitmap!!) {
                    canvas.drawBitmap(data.mbitmap!!, 0.0f, 0.0f, _defaultPaint)
                    canvas.drawBitmap(
                        softCursor!!,
                        cursorRect!!.left,
                        cursorRect!!.top,
                        _defaultPaint
                    )
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun validDraw(x: Int, y: Int, w: Int, h: Int): Boolean {
        return true
    }

    override fun offset(x: Int, y: Int): Int {
        return y * bitmapwidth + x
    }

    override fun createDrawable(): KAbstractBitmapDrawable {
        return CompactBitmapDrawable()
    }

    override fun updateBitmap(x: Int, y: Int, w: Int, h: Int) {
        // Not used
    }

    override fun updateBitmap(b: Bitmap?, x: Int, y: Int, w: Int, h: Int) {
        synchronized(mbitmap!!) {
            memGraphics!!.drawBitmap(b!!, x.toFloat(), y.toFloat(), null)
        }
    }

    override fun copyRect(sx: Int, sy: Int, dx: Int, dy: Int, w: Int, h: Int) {
        var srcOffset: Int
        val startSrcY: Int
        val endSrcY: Int
        var dstY: Int
        val deltaY: Int
        if (sy > dy) {
            startSrcY = sy
            endSrcY = sy + h
            dstY = dy
            deltaY = +1
        } else {
            startSrcY = sy + h - 1
            endSrcY = sy - 1
            dstY = dy + h - 1
            deltaY = -1
        }
        var y = startSrcY
        while (y != endSrcY) {
            srcOffset = offset(sx, y)
            try {
                val bitmapPixels = IntArray(w * h)
                synchronized(mbitmap!!) {
                    mbitmap!!.getPixels(
                        bitmapPixels,
                        srcOffset,
                        bitmapwidth,
                        sx - xoffset,
                        y - yoffset,
                        w,
                        1
                    )
                    mbitmap!!.setPixels(bitmapPixels, offset(dx, dy), bitmapwidth, dx, dy, w, h)
                }
            } catch (e: Exception) {
                // There was an index out of bounds exception, but we continue copying what we can.
                e.printStackTrace()
            }
            dstY += deltaY
            y += deltaY
        }
    }

    override fun drawRect(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        paint: Paint?
    ) {
        synchronized(
            mbitmap!!
        ) {
            memGraphics!!.drawRect(
                x.toFloat(),
                y.toFloat(),
                x + w.toFloat(),
                y + h.toFloat(),
                paint!!
            )
        }
    }

    override fun scrollChanged(newx: Int, newy: Int) {
        // Don't need to do anything here
    }

    override fun frameBufferSizeChanged() {
        framebufferwidth = rfb.framebufferWidth()
        framebufferheight = rfb.framebufferHeight()
        if (bitmapwidth < framebufferwidth || bitmapheight < framebufferheight) {
            Log.i(
                TAG,
                "One or more bitmap dimensions increased, realloc = ("
                        + framebufferwidth + "," + framebufferheight + ")"
            )
            dispose()
            // Try to free up some memory.
//            System.gc();
            bitmapwidth = framebufferwidth
            bitmapheight = framebufferheight
            mbitmap = Bitmap.createBitmap(bitmapwidth, bitmapheight, cfg)
            mbitmap?.let {
                memGraphics = Canvas(it)
            }

            drawable = createDrawable()
            drawable!!.startDrawing()
        } else {
            Log.i(
                TAG,
                "Both bitmap dimensions same or smaller, no realloc = ("
                        + framebufferwidth + "," + framebufferheight + ")"
            )
        }
    }

    override fun syncScroll() {
        // Don't need anything here either
    }

    companion object {
        private const val TAG = "UltraCompactBitmapData"

        /**
         * Multiply this times total number of pixels to get estimate of process size with all buffers plus
         * safety factor
         */
//        const val CAPACITY_MULTIPLIER = 4
    }

    init {
        bitmapwidth = framebufferwidth
        bitmapheight = framebufferheight

        // To please createBitmap, we ensure the size it at least 1x1.
        if (bitmapwidth == 0) bitmapwidth = 1
        if (bitmapheight == 0) bitmapheight = 1
        if (trueColor) {
            cfg = Bitmap.Config.ARGB_8888
        }
        mbitmap = Bitmap.createBitmap(bitmapwidth, bitmapheight, cfg)
        Log.i(
            TAG,
            "bitmapsize = ($bitmapwidth,$bitmapheight)"
        )
        mbitmap?.let {
            it.setHasAlpha(false)
            memGraphics = Canvas(it)
            drawable?.startDrawing()
        }
    }
}