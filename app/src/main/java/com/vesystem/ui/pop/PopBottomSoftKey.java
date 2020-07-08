package com.vesystem.ui.pop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.vesystem.spice.R;
import com.vesystem.ui.interfaces.IPopBottomSoftKeyCallback;


/**
 * Created Date 2020-01-16.
 *
 * @author ChenRui
 * ClassDescription：底部软键盘
 */
public class PopBottomSoftKey extends PopupWindow implements View.OnTouchListener {

    private IPopBottomSoftKeyCallback mIPopBottomSoftKeyCallback;

    public void setIPopBottomSoftKeyCallback(IPopBottomSoftKeyCallback IPopBottomSoftKeyCallback) {
        mIPopBottomSoftKeyCallback = IPopBottomSoftKeyCallback;
    }

    public PopBottomSoftKey(Context context, int softKeyBoardHeight) {
        super(context);

        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.pop_bottom_soft_key, null, false);
        initPopProperty(view, softKeyBoardHeight);
        initView(view);
    }

    private void initPopProperty(View view, int softKeyBoardHeight) {
        Log.i("PopBottomSoftKey", "initPopProperty: 创建软键盘窗体高度：" + softKeyBoardHeight);
        setContentView(view);
        setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        setHeight(softKeyBoardHeight);
//        this.setInputMethodMode(android.widget.PopupWindow.INPUT_METHOD_NEEDED);  //设置Pop不压键盘，默认是会压住键盘
        //设置setBackgroundDrawable才会全屏，默认不全屏
        setBackgroundDrawable(new BitmapDrawable());
        setOutsideTouchable(false);

    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView(View view) {

        Button btnF1 = view.findViewById(R.id.btnF1);
        Button btnF2 = view.findViewById(R.id.btnF2);
        Button btnF3 = view.findViewById(R.id.btnF3);
        Button btnF4 = view.findViewById(R.id.btnF4);
        Button btnF5 = view.findViewById(R.id.btnF5);
        Button btnF6 = view.findViewById(R.id.btnF6);
        Button btnF7 = view.findViewById(R.id.btnF7);
        Button btnF8 = view.findViewById(R.id.btnF8);
        Button btnF9 = view.findViewById(R.id.btnF9);
        Button btnF10 = view.findViewById(R.id.btnF10);
        Button btnF11 = view.findViewById(R.id.btnF11);
        Button btnF12 = view.findViewById(R.id.btnF12);

        LinearLayout btnRightMouse = view.findViewById(R.id.btnRightMouse);
        LinearLayout btnDelText = view.findViewById(R.id.btnDelText);
        LinearLayout btnEnter = view.findViewById(R.id.btnEnter);

        Button btnIns = view.findViewById(R.id.btnIns);
        Button btnDel = view.findViewById(R.id.btnDel);

        Button btnPageUp = view.findViewById(R.id.btnPageUp);
        Button btnPageDown = view.findViewById(R.id.btnPageDown);

        LinearLayout btnScreenShot = view.findViewById(R.id.btnScreenShot);


        LinearLayout btnKeyUp = view.findViewById(R.id.btnKeyUp);
        LinearLayout btnKeyDown = view.findViewById(R.id.btnKeyDown);
        LinearLayout btnKeyLeft = view.findViewById(R.id.btnKeyLeft);
        LinearLayout btnKeyRight = view.findViewById(R.id.btnKeyRight);

        btnKeyUp.setOnTouchListener(this);
        btnKeyDown.setOnTouchListener(this);
        btnKeyLeft.setOnTouchListener(this);
        btnKeyRight.setOnTouchListener(this);

        btnF1.setOnTouchListener(this);
        btnF2.setOnTouchListener(this);
        btnF3.setOnTouchListener(this);
        btnF4.setOnTouchListener(this);
        btnF5.setOnTouchListener(this);
        btnF6.setOnTouchListener(this);
        btnF7.setOnTouchListener(this);
        btnF8.setOnTouchListener(this);
        btnF9.setOnTouchListener(this);
        btnF10.setOnTouchListener(this);
        btnF11.setOnTouchListener(this);
        btnF12.setOnTouchListener(this);

        btnRightMouse.setOnTouchListener(this);
        btnDelText.setOnTouchListener(this);
        btnEnter.setOnTouchListener(this);

        btnIns.setOnTouchListener(this);
        btnDel.setOnTouchListener(this);

        btnPageUp.setOnTouchListener(this);
        btnPageDown.setOnTouchListener(this);

        btnScreenShot.setOnTouchListener(this);
    }

    //TODO 需优化为点击事件
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mIPopBottomSoftKeyCallback != null) {
            int id = v.getId();
            if (id == R.id.btnKeyUp) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_DPAD_UP, v);
            } else if (id == R.id.btnKeyDown) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_DPAD_DOWN, v);
            } else if (id == R.id.btnKeyLeft) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_DPAD_LEFT, v);
            } else if (id == R.id.btnKeyRight) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_DPAD_RIGHT, v);


            } else if (id == R.id.btnF1) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F1, v);
            } else if (id == R.id.btnF2) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F2, v);
            } else if (id == R.id.btnF3) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F3, v);
            } else if (id == R.id.btnF4) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F4, v);
            } else if (id == R.id.btnF5) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F5, v);
//                mIPopBottomSoftKeyCallback.onTouch(event, KeyEvent.KEYCODE_F5, event.getAction());
            } else if (id == R.id.btnF6) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F6, v);
            } else if (id == R.id.btnF7) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F7, v);
            } else if (id == R.id.btnF8) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F8, v);
            } else if (id == R.id.btnF9) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F9, v);
            } else if (id == R.id.btnF10) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F10, v);
            } else if (id == R.id.btnF11) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F11, v);
            } else if (id == R.id.btnF12) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_F12, v);


            } else if (id == R.id.btnRightMouse) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    mIPopBottomSoftKeyCallback.onMouseEvent(4);
                    v.setPressed(true);
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    mIPopBottomSoftKeyCallback.onMouseEvent(4);
                    v.setPressed(false);
                }
                return true;
            } else if (id == R.id.btnDelText) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_DEL, v);
            } else if (id == R.id.btnIns) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_INSERT, v);
            } else if (id == R.id.btnDel) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_FORWARD_DEL, v);
            } else if (id == R.id.btnEnter) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_ENTER, v);
            } else if (id == R.id.btnScreenShot) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_SYSRQ, v);
            } else if (id == R.id.btnPageUp) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_PAGE_UP, v);
            } else if (id == R.id.btnPageDown) {
                return handlerTouchEvent(event, KeyEvent.KEYCODE_PAGE_DOWN, v);
            }
        }
        return false;
    }


    /**
     * 处理按下和松开事件
     *
     * @param event
     * @param keycodeDpadUp 键值
     * @param v
     */
    private boolean handlerTouchEvent(MotionEvent event, int keycodeDpadUp, View v) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            mIPopBottomSoftKeyCallback.onTouchDown(event, keycodeDpadUp);
            v.setPressed(true);
            return true;
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            mIPopBottomSoftKeyCallback.onTouchUp(event, keycodeDpadUp);
            v.setPressed(false);
            return true;
        }
        return false;
    }
}
