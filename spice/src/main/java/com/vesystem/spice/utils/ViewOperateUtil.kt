package com.vesystem.spice.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.vesystem.spice.R
import com.vesystem.spice.mouse.KMouse.Companion.TAG
import com.vesystem.spice.ui.interfaces.SoftKeyBoardListener
import java.lang.reflect.Method


/**
 * Created Date 2019-07-30.
 *
 * @author RayChen
 * ClassDescription：View操作工具类
 */
class ViewOperateUtil {

    companion object {
        private var gestureDetector: GestureDetector? = null

        /**
         * (x,y)是否在view的区域内
         *
         * @param view 控件范围
         * @param x    x坐标
         * @param y    y坐标
         * @return 返回true，代表在范围内
         */
        fun isTouchPointInView(view: View?, x: Int, y: Int): Boolean {
            if (view == null) {
                return false
            }
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val rectF = RectF(
                location[0].toFloat(), location[1]
                    .toFloat(), (location[0] + view.width).toFloat(),
                (location[1] + view.height).toFloat()
            )
            return rectF.contains(x.toFloat(), y.toFloat())
        }


        /**
         * 查找View在窗孔中所在位置
         */
        fun findViewLocation(view: View): RectF {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            return RectF(
                location[0].toFloat(), location[1]
                    .toFloat(), (location[0] + view.width).toFloat(),
                (location[1] + view.height).toFloat()
            )
        }


        /**
         * view随触摸、鼠标事件移动
         */
        @Suppress("DEPRECATION")
        fun moveView(view: View, event: MotionEvent): Boolean {
            gestureDetector?.let {
                return it.onTouchEvent(event)
            } ?: let {
                gestureDetector = GestureDetector(object :
                    GestureDetector.SimpleOnGestureListener() {

                    override fun onSingleTapUp(e: MotionEvent?): Boolean {
                        Log.i("GestureDetector", "onSingleTapUp: ")
                        return true
                    }

                    override fun onDown(e: MotionEvent?): Boolean {
                        Log.i("GestureDetector", "onDown: ")
                        view.alpha = 1f
                        return true
                    }

                    override fun onScroll(
                        e1: MotionEvent,
                        e2: MotionEvent,
                        distanceX: Float,
                        distanceY: Float
                    ): Boolean {
                        val moveX = e2.x - e1.x
                        val moveY = e2.y - e1.y
                        setFathersMeasureChildLocation(
                            view,
                            view.left + moveX.toInt(),
                            view.top + moveY.toInt(),
                            view.right + moveX.toInt(),
                            view.bottom + moveY.toInt()
                        )
//                      view.layout(l,t,r,b)  该方法只更新了孩子得位置，但是父view绘制孩子得时候，还是原有得，一旦父view发生刷新，孩子就会出现还原得问题
                        return true
                    }

                    override fun onLongPress(e: MotionEvent?) {
                    }
                })
                gestureDetector?.onTouchEvent(event)
                return true
            }
        }


        /**
         * 设置父亲测量孩子得位置，解决孩子view发生位置改变，父view刷新时，孩子view被还原问题
         */
        fun setFathersMeasureChildLocation(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int
        ) {
            val params = FrameLayout.LayoutParams(right - left, bottom - top)
            val parent = view.parent
            val p = parent as View
            val marginRight = p.width - right
            val marginBottom = p.height - bottom
            params.setMargins(left, top, marginRight, marginBottom)
            view.layoutParams = params
        }


        /**
         * 创建Activity揭露动画(注意：该Activity主题样式要设置为window背景透明，theme:WindowTransparentTheme)
         *
         * @param activity 需要执行动画的Activity
         * @param duration 动画时长
         * @param centerX  圆心X坐标
         * @param centerY  圆心Y坐标
         */
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        fun createActivityCircularReveal(
            activity: Activity,
            duration: Int,
            centerX: Int,
            centerY: Int
        ) {
            //设置window背景透明
            //decorView执行动画
            val decorView = activity.window.decorView
            val viewById =
                decorView.findViewById<View>(R.id.content)
            decorView.post {
                val widthPixels = activity.resources.displayMetrics.widthPixels
                val heightPixels = activity.resources.displayMetrics.heightPixels
                val hypot = Math.hypot(
                    widthPixels.toDouble(),
                    heightPixels.toDouble()
                ).toFloat()
                val circularReveal =
                    ViewAnimationUtils.createCircularReveal(
                        viewById,
                        centerX,
                        centerY,
                        0f,
                        hypot
                    )
                circularReveal.setDuration(duration.toLong()).start()
            }
        }

        /**
         * Activity消失动画
         *
         * @param context  上下文
         * @param duration 时长
         */
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        fun disapperCircularReveal(context: Context, duration: Int) {
            val widthPixels = context.resources.displayMetrics.widthPixels
            val heightPixels = context.resources.displayMetrics.heightPixels
            val hypot =
                Math.hypot(widthPixels.toDouble(), heightPixels.toDouble()).toFloat()
            val decorView = (context as Activity).window.decorView
            val circularReveal =
                ViewAnimationUtils.createCircularReveal(
                    decorView.rootView,
                    widthPixels / 2,
                    heightPixels / 2,
                    hypot,
                    0f
                )
            circularReveal.duration = duration.toLong()
            circularReveal.start()
            //出现监听，没有kotlin提示时，直接复制java代码，自动转换
            circularReveal.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    decorView.rootView.visibility = View.GONE
                    context.finish()
                }
            })
        }

        var isVisibleForLast = false


        /**
         * 软键盘显示、隐藏监听，并返回键盘高度
         */
        fun setSoftKeyBoardListener(
            activity: Activity,
            softKeyBoardListener: SoftKeyBoardListener
        ) {
            val rootView = activity.window.decorView.rootView
            //        final View rootView = activity.findViewById(android.R.id.content);
            rootView.viewTreeObserver.addOnGlobalLayoutListener {
                Log.i(TAG, "setSoftKeyBoardListener: ")
                val r = Rect()
                rootView.getWindowVisibleDisplayFrame(r)
                //计算出可见屏幕的高度
                val displayHeight: Int = r.height()
                //获得屏幕整体的高度
                val height: Int = rootView.height
                //获得键盘高度
                val keyboardHeight = height - displayHeight
                val visible = displayHeight.toDouble() / height < 0.8
                if (visible != isVisibleForLast) {
                    if (visible) {
                        softKeyBoardListener.showKeyBoard(keyboardHeight)
                    } else {
                        softKeyBoardListener.hideKeyBoard(keyboardHeight)
                    }
                }
                isVisibleForLast = visible
            }
        }


        /**
         * 获取底部导航栏高度
         */
        fun getNavigationBarHeight(context: Context): Int {
            val resources = context.resources
            val resourceId =
                resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                //判断底部导航栏是否为显示状态
                val navigationBarShowing = isNavigationBarShowing(context)
                if (navigationBarShowing) {
                    return resources.getDimensionPixelSize(resourceId)
                }
            }
            return 0
        }


        /**
         * 检测设备是否存在底部导航栏
         */
        @SuppressLint("PrivateApi")
        private fun checkDeviceHasNavigationBar(context: Context): Boolean {
            var hasNavigationBar = false
            val rs: Resources = context.resources
            val id: Int = rs.getIdentifier("config_showNavigationBar", "bool", "android")
            if (id > 0) {
                hasNavigationBar = rs.getBoolean(id)
            }
            try {
                val systemPropertiesClass =
                    Class.forName("android.os.SystemProperties")
                val m: Method = systemPropertiesClass.getMethod("get", String::class.java)
                val navBarOverride =
                    m.invoke(systemPropertiesClass, "qemu.hw.mainkeys") as String
                if ("1" == navBarOverride) {
                    hasNavigationBar = false
                } else if ("0" == navBarOverride) {
                    hasNavigationBar = true
                }
            } catch (e: Exception) {
            }
            return hasNavigationBar
        }


        /**
         * 检测是否显示底部导航栏
         */
        private fun isNavigationBarShowing(context: Context): Boolean {
            //判断手机底部是否支持导航栏显示
            val haveNavigationBar = checkDeviceHasNavigationBar(context)
            if (haveNavigationBar) {
                val brand = Build.BRAND
                val mDeviceInfo: String
                mDeviceInfo = if (brand.equals("HUAWEI", ignoreCase = true)) {
                    "navigationbar_is_min"
                } else if (brand.equals("XIAOMI", ignoreCase = true)) {
                    "force_fsg_nav_bar"
                } else if (brand.equals("VIVO", ignoreCase = true)) {
                    "navigation_gesture_on"
                } else if (brand.equals("OPPO", ignoreCase = true)) {
                    "navigation_gesture_on"
                } else {
                    "navigationbar_is_min"
                }
                if (Settings.Global.getInt(context.contentResolver, mDeviceInfo, 0) == 0) {
                    return true
                }
            }
            return false
        }


    }
}