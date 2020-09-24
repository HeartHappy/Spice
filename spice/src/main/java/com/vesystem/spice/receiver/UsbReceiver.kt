package com.vesystem.spice.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log


class UsbReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        val action = intent.action
        if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
            Log.d("UsbReceiver", "拔出usb")
            val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice
            Log.d("UsbReceiver", "设备的ProductId值为：" + device.productId)
            Log.d("UsbReceiver", "设备的VendorId值为：" + device.vendorId)
        } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
            Log.d("UsbReceiver", "插入usb")
        }
    }
}
