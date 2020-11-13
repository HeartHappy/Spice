package com.vesystem.spicedemo

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.github.jokar.multilanguages.library.MultiLanguage
import com.vesystem.spice.model.KSpice
import com.vesystem.spicedemo.LocalManageUtil.saveSelectLanguage
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val connectReceiver = ConnectReceiver()

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(MultiLanguage.setLocal(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imm()
        setContentView(R.layout.activity_main)
//        readSP()
        KSpice.registerSpiceReceiver(this, connectReceiver)
        connectDesktop()

        btnChina.setOnClickListener {
            selectLanguage(1)
        }
        btnEnglish.setOnClickListener {
            selectLanguage(3)
        }
    }

    private fun readSP() {
        val sp = getSharedPreferences("SpiceConnectInfo", Context.MODE_PRIVATE)
        val ip = sp.getString("IP", null)
        val port = sp.getString("PORT", null)
        val pwd = sp.getString("PWD", null)
        ip?.let { etConnIp.setText(it) }
        port?.let { etConnPort.setText(it) }
        pwd?.let { etConnPwd.setText(it) }
    }


    private fun selectLanguage(select: Int) {
        saveSelectLanguage(this, select)
        //重启APP到主页面
        startActivity(Intent.makeRestartActivityTask(ComponentName(this, MainActivity::class.java)))
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


    @SuppressLint("CommitPrefEdits")
    private fun connectDesktop() {
        saveSP()
        btnConnect.setOnClickListener {
            KSpice
                .connect(
                    etConnIp.text.toString(),
                    etConnPort.text.toString(),
                    etConnPwd.text.toString()
                )
                .runtimeToPhone(true)
                .sound(true)
                .isAdjust(false)
                .mouseMode(KSpice.MouseMode.MODE_CLICK)
                .start(this)
        }
    }

    private fun saveSP() {
        val sp = getSharedPreferences("SpiceConnectInfo", Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putString("IP", etConnIp.text.toString())
        edit.putString("PORT", etConnPort.text.toString())
        edit.putString("PWD", etConnPwd.text.toString())
        edit.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        KSpice.unregisterSpiceReceiver(this, connectReceiver)
    }
}