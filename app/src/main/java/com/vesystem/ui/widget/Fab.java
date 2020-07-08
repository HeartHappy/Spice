package com.vesystem.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.gordonwong.materialsheetfab.AnimatedFab;


/**
 * Created Date 2019-12-09.
 *
 * @author ChenRui
 * ClassDescription：
 */
public class Fab extends FloatingActionButton implements AnimatedFab {

    public Fab(Context context) {
        this(context, null);
    }

    public Fab(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Fab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Shows the FAB.
     */
    @Override
    public void show() {
        show(0, 0);
    }

    @Override
    public void show(float translationX, float translationY) {
        this.setTranslationY(translationY);
        setVisibility(VISIBLE);
    }


    /**
     * Hides the FAB.
     */
    @Override
    public void hide() {
        // NOTE: This immediately hides the FAB. An animation can
        // be used instead - see the sample app.

        setVisibility(View.INVISIBLE);
    }
}
