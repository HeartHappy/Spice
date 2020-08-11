package com.vesystem.test

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.app.AppCompatActivity
import com.jarvislau.destureviewbinder.GestureViewBinder
import com.vesystem.spice.mouse.KMouse
import kotlinx.android.synthetic.main.activity_zoom.*
import kotlin.properties.Delegates

class ZoomActivity : AppCompatActivity() {
    private var sgd: ScaleGestureDetector by Delegates.notNull()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zoom)
        GestureViewBinder.bind(this, flParent, ziv)
        sgd = ScaleGestureDetector(this, object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {

                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector?) {

            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                Log.i(
                    KMouse.TAG,
                    "onScale: ${scaleFactor},伸缩比率：${detector.currentSpan / detector.previousSpan},${detector.currentSpan},${detector.previousSpan}"
                )
                ziv.scaleX = scaleFactor
                ziv.scaleY = scaleFactor
                return false
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

        if (ev.pointerCount == 2) {
            Log.i(TAG, "dispatchTouchEvent: 缩放")
            sgd.onTouchEvent(ev)
        }
        Log.i(TAG, "dispatchTouchEvent: 父view")
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.i(TAG, "onTouchEvent: 父View")
        return super.onTouchEvent(event)
    }

    companion object {
        private const val TAG = "ZoomActivity"
    }
}