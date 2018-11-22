package com.github.anrimian.musicplayer.ui.utils.views.delegate;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;

import static androidx.core.view.ViewCompat.isLaidOut;
import static com.github.anrimian.musicplayer.utils.AndroidUtils.getColorFromAttr;

/**
 * Created on 21.01.2018.
 */

public class StatusBarColorDelegate implements SlideDelegate {

    private final int startColor;
    private final int endColor;

    private final Window window;

    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    public StatusBarColorDelegate(Window window,
                                  @ColorInt int startColor,
                                  @ColorInt int endColor) {
        this.window = window;
        this.startColor = startColor;
        this.endColor = endColor;
    }

    @Override
    public void onSlide(float slideOffset) {
        moveView(slideOffset);
    }

    private void moveView(float slideOffset) {
        int resultColor = (int) argbEvaluator.evaluate(slideOffset, startColor, endColor);
        if (Build.VERSION.SDK_INT >= 21) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(resultColor);
        }
    }
}
