package com.kiwiandroiddev.sc2buildassistant.feature.di

import com.kiwiandroiddev.sc2buildassistant.MyApplication
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.datainterface.ClearDatabaseAgent
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApplicationModule(val app: MyApplication) {

    @Provides
    @Singleton
    fun provideClearDatabaseAgent(): ClearDatabaseAgent = app

}