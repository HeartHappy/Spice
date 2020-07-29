package com.vesystem.test

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.vesystem.spice.model.KSpice
import com.vesystem.spice.zoom.GestureViewBinder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imm()
        setContentView(R.layout.activity_main)

        zc.setOnZoomInClickListener {
            ziv.canvasZoomIn()
        }
        zc.setOnZoomOutClickListener {
            ziv.canvasZoomOut()
        }
        val bind = GestureViewBinder.bind(this, rl, ziv)
        bind.isFullGroup=true
        connectDesktop()
    }

    private fun connectDesktop() {
        btnConnect.setOnClickListener {
            KSpice.connect("192.168.30.62", "5903", "lsmyc3qnx4")
                .sound(true)
//                .resolution(720,1080)
                .mouseMode(KSpice.Companion.MouseMode.MODE_CLICK)
                .listener(object : KSpice.Companion.ISpiceListener {
                    override fun onSucceed() {
                        Log.i("MainActivity", "onSucceed: 连接成功")
                    }

                    override fun onFail(message: String) {
                        Log.i("MainActivity", "onFail: $message")
                    }
                })
                .start(this.applicationContext)
            //            startActivity(Intent(this, RemoteCanvasActivity::class.java))
        }

        btnConnectByTouch.setOnClickListener {
            KSpice.connect("192.168.30.62", "5903", "lsmyc3qnx4")
                .sound(true)
//                .resolution(720,1080)
                .mouseMode(KSpice.Companion.MouseMode.MODE_TOUCH)
                .listener(object : KSpice.Companion.ISpiceListener {
                    override fun onSucceed() {
                        Log.i("MainActivity", "onSucceed: 连接成功")
                    }

                    override fun onFail(message: String) {
                        Log.i("MainActivity", "onFail: $message")
                    }
                })
                .start(this.applicationContext)

        }
    }


    /**
     * 沉浸式
     */
    private fun imm() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }
    }

   /* override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        Log.i("TAG", "dispatchTouchEvent: ${ev.actionMasked}")
        return true
    }*/

}