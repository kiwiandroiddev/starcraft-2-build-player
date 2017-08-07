package com.kiwiandroiddev.sc2buildassistant.di

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by Matt Clarke on 17/06/17.
 */
@Module
class AndroidModule(val appContext: Context) {

    @Provides
    @Singleton
    fun provideSharedPreferences(): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(appContext)

}