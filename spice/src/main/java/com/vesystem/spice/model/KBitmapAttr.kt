package com.vesystem.spice.model

import kotlin.properties.Delegates

/**
 * Created Date 2020/7/7.
 * @author ChenRui
 * ClassDescription:
 */
class KBitmapAttr {
    var x: Int by Delegates.notNull()
    var y: Int by Delegates.notNull()
    var width: Int by Delegates.notNull()
    var height: Int by Delegates.notNull()

    constructor(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    constructor(x: Int, y: Int, width: Int, height: Int) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }

}