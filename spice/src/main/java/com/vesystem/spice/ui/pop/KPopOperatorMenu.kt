package com.vesystem.spice.ui.pop

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import com.vesystem.spice.R
import com.vesystem.spice.ui.interfaces.KIOperatorMenu

/**
 * Created Date 2020-01-13.
 *
 * @author ChenRui
 * ClassDescription：操作菜单
 */
class KPopOperatorMenu(context: Context) : PopupWindow(context),
    View.OnClickListener {
    private var mIOperatorMenu: KIOperatorMenu? = null
    fun setIOperatorMenu(IOperatorMenu: KIOperatorMenu?) {
        mIOperatorMenu = IOperatorMenu
    }

    private fun initPopProperty(
        view: View,
        context: Context
    ) {
        this.contentView = view
        // 设置弹出窗体的宽
        this.width = context.resources.getDimensionPixelSize(R.dimen.dp_80)
        // 设置弹出窗体的高
        this.height = context.resources.getDimensionPixelSize(R.dimen.dp_120)
        // 设置弹出窗体可点击()
        this.isFocusable = true
        this.isOutsideTouchable = true
        this.inputMethodMode = INPUT_METHOD_NEEDED
        //设置SelectPicPopupWindow弹出窗体动画效果
//        this.setAnimationStyle(R.style.mypopwindow_anim_style);
        // 实例化一个ColorDrawable颜色为半透明
        val dw = ColorDrawable(0x00FFFFFF)
        //设置弹出窗体的背景
        setBackgroundDrawable(dw)
    }

    private fun initView(view: View) {
        val tvTouchClick = view.findViewById<TextView>(R.id.tvTouchClick)
        val tvTouchMove = view.findViewById<TextView>(R.id.tvTouchMove)
        val tvDragSelect = view.findViewById<TextView>(R.id.tvDragSelect)
        tvTouchClick.setOnClickListener(this)
        tvTouchMove.setOnClickListener(this)
        tvDragSelect.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        dismiss()
        val id = v.id
        if (id == R.id.tvTouchClick) {
            if (mIOperatorMenu != null) {
                mIOperatorMenu!!.touchClick(v)
            }
        } else if (id == R.id.tvTouchMove) {
            if (mIOperatorMenu != null) {
                mIOperatorMenu!!.touchMove(v)
            }
        } else if (id == R.id.tvDragSelect) {
            if (mIOperatorMenu != null) {
                mIOperatorMenu!!.dragSelect(v)
            }
        }
    }

    init {
        @SuppressLint("InflateParams") val view =
            LayoutInflater.from(context).inflate(R.layout.pop_operator_menu, null, false)
        initView(view)
        initPopProperty(view, context)
    }
}