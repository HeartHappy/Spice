package com.vesystem.spice.model

/**
 * Created Date:2019-11-25
 *
 * @author ChenRui
 * ClassDescription:eventbus消息类
 */
class KMessageEvent {
    var requestCode //request code
            = 0
        private set
    var `object`: Any? = null
        private set
    var msg: String? = null
        private set
    var position = 0
    var isSign = false

    constructor(requestCode: Int) {
        this.requestCode = requestCode
    }

    constructor(`object`: Any?) {
        this.`object` = `object`
    }

    constructor(msg: String?) {
        this.msg = msg
    }

    constructor(requestCode: Int, `object`: Any?) {
        this.requestCode = requestCode
        this.`object` = `object`
    }

    constructor(requestCode: Int, msg: String?) {
        this.requestCode = requestCode
        this.msg = msg
    }

    constructor(requestCode: Int, position: Int) {
        this.requestCode = requestCode
        this.position = position
    }

    constructor(requestCode: Int, sign: Boolean) {
        this.requestCode = requestCode
        isSign = sign
    }

    companion object {
        const val SPICE_CONNECT_SUCCESS = 4
        const val SPICE_CONNECT_FAILURE = 5
        const val SPICE_CONNECT_TIMEOUT = 6 //连接超时
        const val SPICE_GET_W_H = 10 //获取到了bitmap宽高
        const val SPICE_BITMAP_UPDATE = 11 //bitmap更新
        const val SPICE_MOUSE_UPDATE = 12 //鼠标更新
        const val SPICE_MOUSE_MODE_UPDATE = 13 //鼠标模式更新
    }
}