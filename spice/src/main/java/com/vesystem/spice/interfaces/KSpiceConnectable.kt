/**
 * Copyright (C) 2012 Iordan Iordanov
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
package com.vesystem.spice.interfaces

interface KSpiceConnectable {
    fun framebufferWidth(): Int
    fun framebufferHeight(): Int
    fun desktopName(): String?
    fun requestUpdate(incremental: Boolean)

    @Throws(Exception::class)
    fun requestResolution(x: Int, y: Int)
    fun writeClientCutText(text: String?)

    //    public void setIsInNormalProtocol(boolean state);
    //    boolean isInNormalProtocol();
    val encoding: String?
    fun writePointerEvent(
        x: Int,
        y: Int,
        metaState: Int,
        pointerMask: Int,
        relative: Boolean
    )

    fun writeKeyEvent(key: Int, metaState: Int, down: Boolean)
    fun writeSetPixelFormat(
        bitsPerPixel: Int, depth: Int, bigEndian: Boolean,
        trueColour: Boolean, redMax: Int, greenMax: Int, blueMax: Int,
        redShift: Int, greenShift: Int, blueShift: Int, fGreyScale: Boolean
    )

    fun writeFramebufferUpdateRequest(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        b: Boolean
    )

    fun close()
    var isCertificateAccepted: Boolean
}