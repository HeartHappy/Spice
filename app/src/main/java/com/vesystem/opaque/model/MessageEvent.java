package com.vesystem.opaque.model;


/**
 * Created Date:2019-11-25
 *
 * @author ChenRui
 * ClassDescription:eventbus消息类
 */
public class MessageEvent {

    public static final int SPICE_CONNECT_SUCCESS = 4;
    public static final int SPICE_CONNECT_FAILURE = 5;
    public static final int SPICE_GET_W_H = 10;//获取到了bitmap宽高
    public static final int SPICE_BITMAP_UPDATE = 11;//bitmap更新
    public static final int SPICE_MOUSE_UPDATE = 12;//鼠标更新
    public static final int SPICE_MOUSE_MODE_UPDATE = 13;//鼠标模式更新

    private int requestCode; //request code
    private Object object;
    private String msg;
    private int position;
    private boolean sign;



    public MessageEvent(int requestCode) {
        this.requestCode = requestCode;
    }


    public MessageEvent(Object object) {
        this.object = object;
    }

    public MessageEvent(String msg) {
        this.msg = msg;
    }

    public MessageEvent(int requestCode, Object object) {
        this.requestCode = requestCode;
        this.object = object;
    }


    public MessageEvent(int requestCode, String msg) {
        this.requestCode = requestCode;
        this.msg = msg;
    }

    public MessageEvent(int requestCode, int position) {
        this.requestCode = requestCode;
        this.position = position;
    }

    public MessageEvent(int requestCode, boolean sign) {
        this.requestCode = requestCode;
        this.sign = sign;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public Object getObject() {
        return object;
    }

    public String getMsg() {
        return msg;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isSign() {
        return sign;
    }

    public void setSign(boolean sign) {
        this.sign = sign;
    }
}
