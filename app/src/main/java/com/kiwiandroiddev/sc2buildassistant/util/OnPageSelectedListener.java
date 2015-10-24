package com.kiwiandroiddev.sc2buildassistant.util;

import android.support.v4.view.ViewPager;

/**
 * Implementation of the OnPageChangeListener interface that provides default (empty)
 * callback definitions for everything except onPageSelected(). Reduces boilerplate.
 *
 * Created by matt on 22/10/15.
 */
public abstract class OnPageSelectedListener implements ViewPager.OnPageChangeListener {
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
