package com.vesystem.test

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity


class LeaksActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaks)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("LeaksActivity", "onDestroy: 退出了")
    }


}