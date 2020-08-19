package com.vesystem.test

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import com.vesystem.spice.mouse.KMouse

/**
 * Created Date 2020/7/23.
 * @author ChenRui
 * ClassDescription:
 */
class ZoomImageView(context: Context?, attrs: AttributeSet?) :
    AppCompatImageView(context!!, attrs) {
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action==MotionEvent.ACTION_DOWN){
            Log.d("ZoomImageView", "onTouchEvent: X:${event.x},rawX:${event.rawX}")
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private const val TAG = "ZoomImageView"
    }
}