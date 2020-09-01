package com.vesystem.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private val connectReceiver = ConnectReceiver()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imm()
        setContentView(R.layout.activity_main)

        val intent = IntentFilter()
        intent.addAction(KSpice.ACTION_SPICE_CONNECT_SUCCEED)
        registerReceiver(connectReceiver, intent)
        connectDesktop()
    }

    private fun connectDesktop() {
        btnConnect.setOnClickListener {
            KSpice
                .connect(
                    etConnIp.text.toString(),
                    etConnPort.text.toString(),
                    etConnPwd.text.toString()
                )
                .sound(true)
                .isAdjust(false)
                .mouseMode(KSpice.MouseMode.MODE_CLICK)
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


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(connectReceiver)
    }


    class ConnectReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == KSpice.ACTION_SPICE_CONNECT_SUCCEED) {
                Log.i("ConnectReceiver", "onReceive: 连接成功")
            }
        }
    }
}