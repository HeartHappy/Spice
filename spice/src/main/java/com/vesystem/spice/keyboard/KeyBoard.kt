package com.vesystem.spice.keyboard

import android.content.res.Resources
import android.view.KeyEvent
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

/**
 * Created Date 2020/7/9.
 * @author ChenRui
 * ClassDescription:
 */
class KeyBoard(r: Resources) {
    var keyCode: HashMap<Int, Array<Int?>>? = null

    init {
        keyCode = loadKeyMap(r)
    }

    @Throws(IOException::class)
    fun loadKeyMap(r: Resources): HashMap<Int, Array<Int?>>? {
        val `is`: InputStream = try {
            r.assets.open(DEFAULT_LAYOUT_MAP)
        } catch (e: IOException) {
            // If layout map file was not found, load the default one.
            r.assets.open(DEFAULT_LAYOUT_MAP)
        }
        val `in` =
            BufferedReader(InputStreamReader(`is`))
        var line = `in`.readLine()
        val table =
            HashMap<Int, Array<Int?>>(500)
        while (line != null) {
            //android.util.Log.i (TAG, "Layout " + file + " " + line);
            val tokens = line.split(" ".toRegex()).toTypedArray()
            val scanCodes = arrayOfNulls<Int>(tokens.size - 1)
            for (i in 1 until tokens.size) {
                scanCodes[i - 1] = tokens[i].toInt()
            }
            table[tokens[0].toInt()] = scanCodes
            line = `in`.readLine()
        }
        return table
    }

    companion object {
        const val DEFAULT_LAYOUT_MAP = "layouts/English (US)"
        const val SCANCODE_SHIFT_MASK = 0x10000
        const val SCANCODE_ALTGR_MASK = 0x20000
        const val UNICODE_MASK = 0x100000
        const val UNICODE_META_MASK =
            KeyEvent.META_CTRL_MASK or KeyEvent.META_META_MASK or KeyEvent.META_CAPS_LOCK_ON

        //window键盘对应key
        val KEY_WIN_SHIFT = 42
        val KEY_WIN_ALT = 56
        val KEY_WIN_TAB = 15
        val KEY_WIN_CTRL = 29
        const val KEY_WIN_CENTER_ENTER = 28

    }
}