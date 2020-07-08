package com.vesystem.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


/**
 * Created Date 2020-01-20.
 *
 * @author ChenRui
 * ClassDescription：
 */
public class FloatingBehavior extends FloatingActionButton.Behavior {

    public FloatingBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull FloatingActionButton child, @NonNull View dependency) {
        return super.layoutDependsOn(parent, child, dependency) || dependency instanceof LinearLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton fab, View moveView) {
        if (moveView instanceof NestedScrollView) {
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
            int fab_BM = lp.bottomMargin;
            int distance = fab.getHeight() + fab_BM;
            fab.setY(moveView.getY() - distance);
        }
        return super.onDependentViewChanged(parent, fab, moveView);

    }
}
