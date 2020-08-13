package com.vesystem.spice.utils

import android.content.Context
import android.widget.Toast

/**
 * Created Date 2020/8/11.
 * @author ChenRui
 * ClassDescription:单例toast工具类
 */
object KToast {

    /** 之前显示的内容  */
    private var oldMsg: String? = null

    private var toast: Toast? = null
    fun show(context: Context, msg: String) {
        toast ?: let {
            toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT)
            toast?.show()
        }.let {
            if (msg == oldMsg) {
                toast?.show()
            } else {
                oldMsg = msg
                toast?.setText(msg)
                toast?.show()
            }
        }
    }
}