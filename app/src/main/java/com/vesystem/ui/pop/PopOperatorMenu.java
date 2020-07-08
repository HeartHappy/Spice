package com.vesystem.ui.pop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.vesystem.spice.R;
import com.vesystem.ui.interfaces.IOperatorMenu;


/**
 * Created Date 2020-01-13.
 *
 * @author ChenRui
 * ClassDescription：操作菜单
 */
public class PopOperatorMenu extends PopupWindow implements View.OnClickListener {

    private IOperatorMenu mIOperatorMenu;

    public void setIOperatorMenu(IOperatorMenu IOperatorMenu) {
        mIOperatorMenu = IOperatorMenu;
    }

    public PopOperatorMenu(Context context) {
        super(context);
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.pop_operator_menu, null, false);
        initView(view);
        initPopProperty(view, context);
    }

    private void initPopProperty(View view, Context context) {
        this.setContentView(view);
        // 设置弹出窗体的宽
        this.setWidth(context.getResources().getDimensionPixelSize(R.dimen.dp_80));
        // 设置弹出窗体的高
        this.setHeight(context.getResources().getDimensionPixelSize(R.dimen.dp_120));
        // 设置弹出窗体可点击()
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        this.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        //设置SelectPicPopupWindow弹出窗体动画效果
//        this.setAnimationStyle(R.style.mypopwindow_anim_style);
        // 实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00FFFFFF);
        //设置弹出窗体的背景
        this.setBackgroundDrawable(dw);
    }

    private void initView(View view) {
        TextView tvTouchClick = view.findViewById(R.id.tvTouchClick);
        TextView tvTouchMove = view.findViewById(R.id.tvTouchMove);
        TextView tvDragSelect = view.findViewById(R.id.tvDragSelect);

        tvTouchClick.setOnClickListener(this);
        tvTouchMove.setOnClickListener(this);
        tvDragSelect.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        dismiss();
        int id = v.getId();
        if (id == R.id.tvTouchClick) {
            if (mIOperatorMenu != null) {
                mIOperatorMenu.touchClick(v);
            }
        } else if (id == R.id.tvTouchMove) {
            if (mIOperatorMenu != null) {
                mIOperatorMenu.touchMove(v);
            }
        } else if (id == R.id.tvDragSelect) {
            if (mIOperatorMenu != null) {
                mIOperatorMenu.dragSelect(v);
            }
        }
    }
}
