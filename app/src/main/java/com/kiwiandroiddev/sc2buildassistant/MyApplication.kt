package com.kiwiandroiddev.sc2buildassistant


import android.app.Activity
import android.app.Application
import android.os.Bundle

import com.google.analytics.tracking.android.GoogleAnalytics
import com.google.analytics.tracking.android.Logger
import com.karumi.dexter.Dexter
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter
import com.kiwiandroiddev.sc2buildassistant.feature.di.ApplicationComponent
import com.kiwiandroiddev.sc2buildassistant.feature.di.DaggerApplicationComponent
import com.kiwiandroiddev.sc2buildassistant.feature.navigation.RegisteredActivityNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsFragment

import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

/**
 * Makes a database instance visible across the application, to prevent frequent opening
 * and closing of a new database in every class

 * @author matt
 */
class MyApplication : Application() {

    @Inject lateinit var registeredActivityNavigator: RegisteredActivityNavigator

    private lateinit var graph: ApplicationComponent
    private var mDb: DbAdapter? = null

    override fun onCreate() {
        super.onCreate()

        initDependencyInjection()
        initRuntimePermissionsHelper()
        initActivityLifecycleCallbacks()

        if (BuildConfig.DEBUG) {
            initDebugLogging()
            initDebugAnalytics()
        }
    }

    private fun initActivityLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityPaused(p0: Activity?) {
            }

            override fun onActivityResumed(p0: Activity) {
                registeredActivityNavigator.registerCurrentActivity(p0)
            }

            override fun onActivityStarted(p0: Activity?) {
            }

            override fun onActivityDestroyed(p0: Activity) {
                registeredActivityNavigator.unregisterCurrentActivity(p0)
            }

            override fun onActivitySaveInstanceState(p0: Activity?, p1: Bundle?) {
            }

            override fun onActivityStopped(p0: Activity?) {
            }

            override fun onActivityCreated(p0: Activity?, p1: Bundle?) {
            }

        })
    }

    private fun initDebugAnalytics() {
        GoogleAnalytics.getInstance(this)?.apply {
            setDryRun(true)
            logger.logLevel = Logger.LogLevel.VERBOSE
        }
    }

    private fun initDebugLogging() {
        Timber.plant(DebugTree())
    }

    private fun initDependencyInjection() {
        graph = DaggerApplicationComponent.builder().build()
        graph.inject(this)
    }

    private fun initRuntimePermissionsHelper() {
        Dexter.initialize(this)
    }

    val db: DbAdapter?
        get() {
            if (mDb == null)
                mDb = DbAdapter(applicationContext)
            return mDb
        }

    fun inject(target: SettingsFragment) =
            graph.inject(target)
}
