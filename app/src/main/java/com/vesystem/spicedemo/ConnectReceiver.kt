package com.vesystem.spicedemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vesystem.spice.model.KSpice

class ConnectReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        if (intent.action == KSpice.ACTION_SPICE_CONNECT_SUCCEED) {
            Log.d("ConnectReceiver", "onReceive: 通知成功")
        } else if (intent.action == KSpice.ACTION_SPICE_CONNECT_DISCONNECT) {
            Log.d("ConnectReceiver", "onReceive: 通知断开")
        }
    }
}
