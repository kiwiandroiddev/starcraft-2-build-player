package com.kiwiandroiddev.sc2buildassistant.feature.di

import android.content.Context
import com.kiwiandroiddev.sc2buildassistant.MyApplication
import com.kiwiandroiddev.sc2buildassistant.feature.di.qualifiers.ApplicationContext
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.datainterface.ClearDatabaseAgent
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApplicationModule(val app: MyApplication) {

    @Provides
    @ApplicationContext
    fun provideApplicationContext(): Context = app

    @Provides
    @Singleton
    fun provideClearDatabaseAgent(): ClearDatabaseAgent = app

}