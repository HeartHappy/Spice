package com.vesystem.test

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.vesystem.spice.zoom.ScaleGestureBinder
import com.vesystem.spice.zoom.ScaleGestureListener
import kotlinx.android.synthetic.main.activity_zoom.*


class ZoomActivity : AppCompatActivity() {
    private var scaleGestureListener: ScaleGestureListener? = null
    private var scaleGestureBinder: ScaleGestureBinder? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zoom)
        scaleGestureListener = ScaleGestureListener(ziv)
        scaleGestureListener?.isFullGroup = true
        scaleGestureListener?.setOnScaleListener { Log.i(TAG, "onCreate: $it") }
        scaleGestureBinder = ScaleGestureBinder(this, scaleGestureListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("LeaksActivity", "onDestroy: 退出了")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureBinder?.onTouchEvent(event)
        if(event.action==MotionEvent.ACTION_DOWN){
            Log.d("ZoomActivity", "onTouchEvent: X:${event.x},rawX:${event.rawX}")
        }
        return super.onTouchEvent(event)
    }
    companion object {
        private const val TAG = "ZoomActivity"
    }
}