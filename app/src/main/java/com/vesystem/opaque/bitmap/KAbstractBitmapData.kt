/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2009 Michael A. MacDonald
 *
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */
package com.vesystem.opaque.bitmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.ImageView
import com.vesystem.opaque.interfaces.KSpiceConnectable

/**
 * Abstract interface between the VncCanvas and the bitmap and pixel data buffers that actually contain
 * the data.
 * This allows for implementations that use smaller bitmaps or buffers to save memory.
 *
 * @author Michael A. MacDonald
 */
abstract class KAbstractBitmapData internal constructor(
    var rfb: KSpiceConnectable
) {
    @JvmField
    var framebufferwidth: Int

    @JvmField
    var framebufferheight: Int

    @JvmField
    var bitmapwidth = 0

    @JvmField
    var bitmapheight = 0

    var mbitmap: Bitmap? = null
    var bitmapPixels: IntArray? = null

    @JvmField
    var memGraphics: Canvas? = null
    var waitingForInput = false

    //    RemoteCanvas vncCanvas;
    @JvmField
    var drawable: KAbstractBitmapDrawable? = null
    var paint: Paint

    @JvmField
    var xoffset = 0

    @JvmField
    var yoffset = 0

    @Synchronized
    fun doneWaiting() {
        waitingForInput = false
    }

    fun setCursorRect(x: Int, y: Int, w: Int, h: Int, hX: Int, hY: Int) {
        if (drawable != null) drawable!!.setCursorRect(x, y, w.toFloat(), h.toFloat(), hX, hY)
    }

    fun moveCursorRect(x: Int, y: Int) {
        if (drawable != null) drawable!!.moveCursorRect(x, y)
    }

    fun setSoftCursor(newSoftCursorPixels: IntArray?) {
        if (drawable != null) drawable!!.setSoftCursor(newSoftCursorPixels)
    }

    // Return an empty new rectangle if drawable is null.
    val cursorRect: RectF
        get() = (if (drawable != null) drawable!!.cursorRect else  // Return an empty new rectangle if drawable is null.
            RectF()) as RectF

    val isNotInitSoftCursor: Boolean
        get() = if (drawable != null) drawable!!.softCursorInit == false else false

    /**
     * @return The smallest scale supported by the implementation; the scale at which
     * the bitmap would be smaller than the screen
     */
    /* val minimumScale: Float
         get() = Math.min(
             vncCanvas.get()!!.width.toFloat() / framebufferwidth,
             vncCanvas.get()!!.height.toFloat() / framebufferheight
         )

     fun widthRatioLessThanHeightRatio(): Boolean {
         return vncCanvas.get()?.width?.toFloat()!! / framebufferwidth < vncCanvas.get()
             ?.getHeight()!! / framebufferheight
     }
 */
    /**
     * Send a request through the protocol to get the data for the currently held bitmap
     *
     * @param incremental True if we want incremental update; false for full update
     */
    fun prepareFullUpdateRequest(incremental: Boolean) {}

    /**
     * Determine if a rectangle in full-frame coordinates can be drawn in the existing buffer
     *
     * @param x Top left x
     * @param y Top left y
     * @param w width (pixels)
     * @param h height (pixels)
     * @return True if entire rectangle fits into current screen buffer, false otherwise
     */
    abstract fun validDraw(x: Int, y: Int, w: Int, h: Int): Boolean

    /**
     * Return an offset in the bitmapPixels array of a point in full-frame coordinates
     *
     * @param x
     * @param y
     * @return Offset in bitmapPixels array of color data for that point
     */
    abstract fun offset(x: Int, y: Int): Int

    /**
     * Update pixels in the bitmap with data from the bitmapPixels array, positioned
     * in full-frame coordinates
     *
     * @param x Top left x
     * @param y Top left y
     * @param w width (pixels)
     * @param h height (pixels)
     */
    abstract fun updateBitmap(x: Int, y: Int, w: Int, h: Int)

    /**
     * Update pixels in the bitmap with data from the given bitmap, positioned
     * in full-frame coordinates
     *
     * @param b The bitmap to copy from.
     * @param x Top left x
     * @param y Top left y
     * @param w width (pixels)
     * @param h height (pixels)
     */
    abstract fun updateBitmap(b: Bitmap?, x: Int, y: Int, w: Int, h: Int)

    /**
     * Create drawable appropriate for this data
     *
     * @return drawable
     */
    abstract fun createDrawable(): KAbstractBitmapDrawable

    /**
     * Sets the canvas's drawable
     *
     * @param v ImageView displaying bitmap data
     */
    fun setImageDrawable(v: ImageView) {
        v.setImageDrawable(drawable)
    }

    /**
     * Call in UI thread; tell ImageView we've changed
     *
     * @param v ImageView displaying bitmap data
     */
    fun updateView(v: ImageView) {
        v.invalidate()
    }

    /**
     * Copy a rectangle from one part of the bitmap to another
     */
    abstract fun copyRect(sx: Int, sy: Int, dx: Int, dy: Int, w: Int, h: Int)
    fun fillRect(x: Int, y: Int, w: Int, h: Int, pix: Int) {
        paint.color = pix
        drawRect(x, y, w, h, paint)
    }
    /* public void imageRect(int x, int y, int w, int h, int[] pix) {
        for (int j = 0; j < h; j++) {
            try {
                synchronized (mbitmap) {
                    System.arraycopy(pix, (w * j), bitmapPixels, offset(x, y + j), w);
                }
                //System.arraycopy(pix, (w * j), bitmapPixels, bitmapwidth * (y + j) + x, w);
            } catch (ArrayIndexOutOfBoundsException e) {
                // An index is out of bounds for some reason, but we try to continue.
                e.printStackTrace();
            }

        }
        updateBitmap(x, y, w, h);
    }*/
    /**
     * Draw a rectangle in the bitmap with coordinates given in full frame
     *
     * @param x     Top left x
     * @param y     Top left y
     * @param w     width (pixels)
     * @param h     height (pixels)
     * @param paint How to draw
     */
    abstract fun drawRect(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        paint: Paint?
    )

    /**
     * Scroll position has changed.
     *
     *
     * This method is called in the UI thread-- it updates internal status, but does
     * not change the bitmap data or send a network request until syncScroll is called
     *
     * @param newx Position of left edge of visible part in full-frame coordinates
     * @param newy Position of top edge of visible part in full-frame coordinates
     */
    abstract fun scrollChanged(newx: Int, newy: Int)

    /**
     * Remote framebuffer size has changed.
     *
     *
     * This method is called when the framebuffer has changed size and reinitializes the
     * necessary data structures to support that change.
     */
    abstract fun frameBufferSizeChanged()

    /**
     * Sync scroll -- called from network thread; copies scroll changes from UI to network state
     */
    abstract fun syncScroll()

    /**
     * Release resources
     */
    fun dispose() {
        if (drawable != null) drawable!!.dispose()
        drawable = null
        if (mbitmap != null) mbitmap!!.recycle()
        mbitmap = null
        memGraphics = null
        bitmapPixels = null
    }

    fun fbWidth(): Int {
        return framebufferwidth
    }

    fun fbHeight(): Int {
        return framebufferheight
    }

    fun bmWidth(): Int {
        return bitmapwidth
    }

    fun bmHeight(): Int {
        return bitmapheight
    }

    init {
        framebufferwidth = rfb.framebufferWidth()
        framebufferheight = rfb.framebufferHeight()
        drawable = this.createDrawable()
        paint = Paint()
    }
}