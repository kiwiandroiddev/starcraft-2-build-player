package com.kiwiandroiddev.sc2buildassistant;


import android.app.Application;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;

import timber.log.Timber;
import timber.log.Timber.DebugTree;

/**
 * Makes a database instance visible across the application, to prevent frequent opening
 * and closing of a new database in every class
 * 
 * @author matt
 *
 */
public class MyApplication extends Application {
    private DbAdapter mDb;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new DebugTree());
        }

        // TODO execute only for debug build type

        // When dry run is set, hits will not be dispatched, but will still be logged as
        // though they were dispatched.
        GoogleAnalytics.getInstance(this).setDryRun(true);

        // Set the log level to verbose.
        GoogleAnalytics.getInstance(this).getLogger()
                .setLogLevel(Logger.LogLevel.VERBOSE);
    }

    public DbAdapter getDb() {
      if (mDb == null)
          mDb = new DbAdapter(getApplicationContext());
      return mDb;
    }
}
