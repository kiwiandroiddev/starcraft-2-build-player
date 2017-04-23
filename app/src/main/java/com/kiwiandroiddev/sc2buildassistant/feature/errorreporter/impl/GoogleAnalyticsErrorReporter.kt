package com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.impl

import android.content.Context
import com.kiwiandroiddev.sc2buildassistant.feature.di.qualifiers.ApplicationContext
import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.util.EasyTrackerUtils

class GoogleAnalyticsErrorReporter(@ApplicationContext val appContext: Context) : ErrorReporter {

    override fun trackNonFatalError(error: Throwable) {
        EasyTrackerUtils.sendNonFatalException(appContext, error)
    }

}