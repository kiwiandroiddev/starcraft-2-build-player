package com.kiwiandroiddev.sc2buildassistant.feature.di

import com.kiwiandroiddev.sc2buildassistant.MyApplication
import com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        ApplicationModule::class,
        NavigationModule::class,
        SettingsModule::class))
interface ApplicationComponent {
    fun inject(target: MyApplication)
    fun inject(target: SettingsFragment)
}