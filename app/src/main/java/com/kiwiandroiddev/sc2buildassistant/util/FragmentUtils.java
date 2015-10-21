package com.kiwiandroiddev.sc2buildassistant.util;

/**
 * Created by matt on 22/10/15.
 */
public class FragmentUtils {

    private FragmentUtils() {}

    /**
     * Helper for getting Fragments inside the tab pager
     */
    public static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }
}
