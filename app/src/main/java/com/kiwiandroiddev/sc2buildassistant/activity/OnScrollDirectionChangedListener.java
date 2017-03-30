package com.kiwiandroiddev.sc2buildassistant.activity;

import android.support.v4.widget.NestedScrollView;

public abstract class OnScrollDirectionChangedListener implements NestedScrollView.OnScrollChangeListener {

    private int oldYDelta = 0;

    @Override
    public final void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        int yDelta = scrollY - oldScrollY;

        if (yDelta > 0 && oldYDelta <= 0) {
            onStartScrollingDown();
        } else if (yDelta < 0 && oldYDelta >= 0) {
            onStartScrollingUp();
        }

        oldYDelta = yDelta;
    }

    public void onStartScrollingDown() {}

    public void onStartScrollingUp() {}

}
