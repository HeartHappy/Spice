package com.vesystem.test

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView

/**
 * Created Date 2020/7/23.
 * @author ChenRui
 * ClassDescription:
 */
class ZoomImageView(context: Context?, attrs: AttributeSet?) : AppCompatImageView(context!!, attrs) {
    var bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.bg)
    private var bitmapMatrix: Matrix? = null
    private var scaleFactor: Float = 1f

    init {

        setBackgroundColor(Color.BLUE)
        scaleType = ScaleType.MATRIX
        bitmapMatrix = Matrix()

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmapMatrix?.setTranslate((w - bitmap.width) / 2f, (h - bitmap.height) / 2f)
        Log.i(TAG, "onSizeChanged:$w,$h ")
    }

    override fun onDraw(canvas: Canvas) {
        bitmapMatrix?.let { matrix ->
            Log.i(TAG, "onDraw: ")
//            canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f) //缩放画布
            canvas.drawBitmap(bitmap, matrix, null)//绘制图片
        }
    }


    fun canvasZoomIn() {
        scaleFactor += 0.5f
//        postInvalidate()
        scaleX = scaleFactor
        scaleY = scaleFactor
//        animate().scaleX(scaleFactor).scaleY(scaleFactor).start()
    }


    fun canvasZoomOut() {
        scaleFactor -= 0.5f
        if (scaleFactor < 1) {
            scaleFactor = 1f
            Toast.makeText(context, "比例1：1", Toast.LENGTH_SHORT).show()
            return
        }

        scaleX = scaleFactor
        scaleY = scaleFactor
    }


    companion object {
        private const val TAG = "ZoomImageView"
    }
}