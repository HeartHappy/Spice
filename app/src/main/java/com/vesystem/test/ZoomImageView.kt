package com.vesystem.test

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatImageView

/**
 * Created Date 2020/7/23.
 * @author ChenRui
 * ClassDescription:
 */
class ZoomImageView(context: Context?, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {
    var bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_banner_foreground)
    private var bitmapMatrix: Matrix? = null

    init {

        setBackgroundColor(Color.BLUE)

        bitmapMatrix = Matrix()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.i(TAG, "onSizeChanged:$w,$h ")
    }

    override fun onDraw(canvas: Canvas) {
        bitmapMatrix?.let { matrix ->
            canvas.drawBitmap(bitmap, matrix, null)
        }
    }

    companion object {
        private const val TAG = "ZoomImageView"
    }
}