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

    /** 第一次时间  */
    private var oneTime: Long = 0

    /** 第二次时间  */
    private var twoTime: Long = 0
    private var toast: Toast? = null
    fun show(context: Context, msg: String) {
        toast ?: let {
            toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT)
            toast?.show()
            oneTime = System.currentTimeMillis()
        }.let {
            twoTime = System.currentTimeMillis()
            if (msg == oldMsg && twoTime - oneTime > Toast.LENGTH_SHORT) {
                toast?.show()
            } else {
                oldMsg = msg
                toast?.setText(msg)
                toast?.show()
            }
        }
    }
}