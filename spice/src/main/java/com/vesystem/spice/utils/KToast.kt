package com.vesystem.spice.utils

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast

/**
 * Created Date 2020/8/11.
 * @author ChenRui
 * ClassDescription:单例toast工具类
 */
object KToast {

    private var toast: Toast? = null

    /**
     * 长吐司
     *
     * @param message 文字
     */
    @SuppressLint("ShowToast")
    fun show(context: Context, message: String) {
        if (toast == null) {
            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        } else {
            toast?.setText(message)
        }
        toast?.show()
    }
}