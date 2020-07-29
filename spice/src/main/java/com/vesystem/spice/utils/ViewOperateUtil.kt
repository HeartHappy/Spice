package com.vesystem.spice.utils

import android.R
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.annotation.RequiresApi
import com.vesystem.spice.ui.interfaces.SoftKeyBoardListener

/**
 * Created Date 2019-07-30.
 *
 * @author RayChen
 * ClassDescription：View操作工具类
 */
class ViewOperateUtil private constructor() {

    companion object {
        private var rootViewVisibleHeight = 0 //纪录根视图的显示高度 = 0

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

        fun findViewLocation(view: View): RectF {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            return RectF(
                location[0].toFloat(), location[1]
                    .toFloat(), (location[0] + view.width).toFloat(),
                (location[1] + view.height).toFloat()
            )
        }

        fun softKeyBoardListener(
            activity: Activity,
            softKeyBoardListener: SoftKeyBoardListener
        ) {
            val rootView = activity.window.decorView.rootView
            //        final View rootView = activity.findViewById(android.R.id.content);
            rootView.viewTreeObserver.addOnGlobalLayoutListener(OnGlobalLayoutListener {
                val r = Rect()
                rootView.getWindowVisibleDisplayFrame(r)
                val visibleHeight = r.height()
                if (rootViewVisibleHeight == 0) {
                    rootViewVisibleHeight = visibleHeight
                    return@OnGlobalLayoutListener
                }

                //根视图显示高度没有变化，可以看作软键盘显示／隐藏状态没有改变
                if (rootViewVisibleHeight == visibleHeight) {
                    return@OnGlobalLayoutListener
                }

                //根视图显示高度变小超过300，可以看作软键盘显示了，该数值可根据需要自行调整
                if (rootViewVisibleHeight - visibleHeight > 200) {
                    softKeyBoardListener.showKeyBoard(rootViewVisibleHeight - visibleHeight)
                    rootViewVisibleHeight = visibleHeight
                    return@OnGlobalLayoutListener
                }

                //根视图显示高度变大超过300，可以看作软键盘隐藏了，该数值可根据需要自行调整
                if (visibleHeight - rootViewVisibleHeight > 200) {
                    softKeyBoardListener.hideKeyBoard(visibleHeight - rootViewVisibleHeight)
                    rootViewVisibleHeight = visibleHeight
                }
            })
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
            val intent = activity.intent
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

    }
}