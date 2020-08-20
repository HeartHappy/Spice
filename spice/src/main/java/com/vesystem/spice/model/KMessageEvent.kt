package com.vesystem.spice.model

/**
 * Created Date:2019-11-25
 *
 * @author ChenRui
 * ClassDescription:eventbus消息类
 */
class KMessageEvent {
    var requestCode = 0//request code
    var msg: String? = null

    constructor(requestCode: Int) {
        this.requestCode = requestCode
    }

    constructor(requestCode: Int, msg: String?) {
        this.requestCode = requestCode
        this.msg = msg
    }


    companion object {
        const val SPICE_CONNECT_SUCCESS = 4
        const val SPICE_CONNECT_FAILURE = 5
        const val SPICE_CONNECT_TIMEOUT = 6 //连接超时
        const val SPICE_ADJUST_RESOLVING = 7 //调整分辨率成功
        const val SPICE_ADJUST_RESOLVING_SUCCEED = 8 //调整分辨率成功
        const val SPICE_ADJUST_RESOLVING_TIMEOUT = 9 //调整分辨率超时，win10中遇到
    }
}