package com.kiwiandroiddev.sc2buildassistant.util;

import android.content.Context;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;

/**
 * Helper functions to bring some sanity to Google's hideous analytics APIs.
 *
 * Created by matt on 5/09/15.
 */
public class EasyTrackerUtils {
    public static void sendNonFatalException(Context c, Exception e) {
        EasyTracker.getInstance(c).send(
                MapBuilder.createException(
                        new StandardExceptionParser(c, null)
                                .getDescription(Thread.currentThread().getName(), e),
                        false)    // False indicates a nonfatal exception
                        .build());
    }

    public static void sendEvent(Context c, String category, String action, String label, Long value) {
        EasyTracker.getInstance(c).send(
                MapBuilder.createEvent(category, action, label, value)
                        .build());
    }
}
