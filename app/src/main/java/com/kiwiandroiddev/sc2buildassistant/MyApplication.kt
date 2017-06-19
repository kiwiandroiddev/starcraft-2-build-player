package com.kiwiandroiddev.sc2buildassistant


import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.analytics.tracking.android.GoogleAnalytics
import com.google.analytics.tracking.android.Logger
import com.karumi.dexter.Dexter
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter
import com.kiwiandroiddev.sc2buildassistant.di.AndroidModule
import com.kiwiandroiddev.sc2buildassistant.di.ApplicationComponent
import com.kiwiandroiddev.sc2buildassistant.di.ApplicationModule
import com.kiwiandroiddev.sc2buildassistant.di.DaggerApplicationComponent
import com.kiwiandroiddev.sc2buildassistant.feature.brief.view.BriefActivity
import com.kiwiandroiddev.sc2buildassistant.feature.brief.view.BriefViewModel
import com.kiwiandroiddev.sc2buildassistant.feature.navigation.RegisteredActivityNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.datainterface.ClearDatabaseAgent
import com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsFragment
import com.kiwiandroiddev.sc2buildassistant.service.JsonBuildService
import io.reactivex.Completable
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

/**
 * Makes a database instance visible across the application, to prevent frequent opening
 * and closing of a new database in every class

 * @author matt
 */
class MyApplication : Application(), ClearDatabaseAgent {
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
            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                registeredActivityNavigator.registerCurrentActivity(activity)
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                registeredActivityNavigator.unregisterCurrentActivity(activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle?) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivityCreated(activity: Activity, p1: Bundle?) {
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
        graph = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(app = this))
                .androidModule(AndroidModule(this))
                .build()
        graph.inject(this)
    }

    private fun initRuntimePermissionsHelper() {
        Dexter.initialize(this)
    }

    val db: DbAdapter
        get() = synchronized(this) {
            if (mDb == null)
                mDb = DbAdapter(applicationContext)

            mDb!!
        }

    fun inject(target: SettingsFragment) =
            graph.inject(target)

    fun inject(target: BriefActivity) =
            graph.inject(target)

    fun inject(target: BriefViewModel) =
            graph.inject(target)

    override fun clear(): Completable =
            Completable.fromAction {
                db.clear()
                JsonBuildService.notifyBuildProviderObservers(this)
            }

}
