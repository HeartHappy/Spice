package com.vesystem.spice.ui.pop

import android.content.Context
import android.view.LayoutInflater
import android.widget.PopupWindow
import android.widget.TextView
import com.vesystem.spice.R
import com.vesystem.spice.ui.interfaces.IPopMenuItemListener

/**
 * Created Date 2020/7/22.
 * @author ChenRui
 * ClassDescription:
 */
class KPopMenuList(val context: Context, private val listener: IPopMenuItemListener) :
    PopupWindow() {
    init {
        val view =
            LayoutInflater.from(context).inflate(R.layout.pop_remote_menu_list, null, false)
        contentView = view
        width = context.resources.getDimensionPixelSize(R.dimen.dp_60)
        height = context.resources.getDimensionPixelSize(R.dimen.dp_60)
        isTouchable=true
        contentView.findViewById<TextView>(R.id.session_sys_keyboard).setOnClickListener {
            dismiss()
            listener.onClickSystemKeyboard()
        }
        contentView.findViewById<TextView>(R.id.session_disconnect).setOnClickListener {
            dismiss()
            listener.onClickDisconnect()
        }
        setOnDismissListener { listener.onDismiss() }
    }

}