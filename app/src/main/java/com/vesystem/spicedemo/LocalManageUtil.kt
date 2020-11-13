package com.vesystem.spicedemo

import android.content.Context
import android.content.res.Configuration
import com.github.jokar.multilanguages.library.MultiLanguage
import java.util.*


/**
 * Created Date 2020/9/11.
 * @author ChenRui
 * ClassDescription:
 */
object LocalManageUtil {

    /**
     * 获取系统的locale
     *
     * @return Locale对象
     */
    fun getSystemLocale(context: Context?): Locale? {
        return SPUtil.getInstance(context).systemCurrentLocal
    }

    fun getSelectLanguage(context: Context): String? {
        return when (SPUtil.getInstance(context).selectLanguage) {
            1 -> context.getString(R.string.chinese)
            3 -> context.getString(R.string.english)
            else -> context.getString(R.string.chinese)
        }
    }

    /**
     * 获取选择的语言设置
     *
     * @param context
     * @return
     */
    fun getSetLanguageLocale(context: Context?): Locale? {
        return when (SPUtil.getInstance(context).selectLanguage) {
            0 -> getSystemLocale(context)
            1 -> Locale.CHINA
            2 -> Locale.TAIWAN
            3 -> Locale.ENGLISH
            else -> Locale.CHINA
        }
    }


    fun saveSystemCurrentLanguage(context: Context?) {
        SPUtil.getInstance(context).systemCurrentLocal = MultiLanguage.getSystemLocal(context)
    }

    /**
     * 保存系统语言
     * @param context
     * @param newConfig
     */
    fun saveSystemCurrentLanguage(context: Context?, newConfig: Configuration?) {
        SPUtil.getInstance(context).systemCurrentLocal = MultiLanguage.getSystemLocal(newConfig)
    }

    fun saveSelectLanguage(context: Context?, select: Int) {
        SPUtil.getInstance(context).saveLanguage(select)
        MultiLanguage.setApplicationLanguage(context)
    }
}