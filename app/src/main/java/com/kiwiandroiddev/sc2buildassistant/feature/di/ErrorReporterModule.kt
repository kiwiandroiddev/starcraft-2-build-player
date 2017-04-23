package com.kiwiandroiddev.sc2buildassistant.feature.di

import android.content.Context
import com.kiwiandroiddev.sc2buildassistant.feature.di.qualifiers.ApplicationContext
import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.impl.GoogleAnalyticsErrorReporter
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ErrorReporterModule {

    @Provides
    @Singleton
    fun provideErrorReporter(@ApplicationContext appContext: Context): ErrorReporter =
            GoogleAnalyticsErrorReporter(appContext)

}