package com.vesystem.test

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.vesystem.spice.model.KSpice
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        imm()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)
        /*zc.setOnZoomInClickListener {
            val content = window.decorView.findViewById<FrameLayout>(android.R.id.content)
            content.animate().scaleX(1.2f).scaleY(1.2f).start()
            Log.i(TAG, "onCreate:放大 ")

        }
        zc.setOnZoomOutClickListener {
            val content = window.decorView.findViewById<FrameLayout>(android.R.id.content)
            content.animate().scaleX(1.0f).scaleY(1.0f).start()
            Log.i(TAG, "onCreate: 缩小")
        }*/


        connectDesktop()
    }

    private fun connectDesktop() {
        btnConnect.setOnClickListener {
            KSpice.connect("192.168.30.61", "5901", "bgh3q1klcp")
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
            //            startActivity(Intent(this, RemoteCanvasActivity::class.java))
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

    companion object {
        private const val TAG = "MainActivity"
    }
}