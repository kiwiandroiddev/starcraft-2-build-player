package com.kiwiandroiddev.sc2buildassistant.di

import com.kiwiandroiddev.sc2buildassistant.MyApplication
import com.kiwiandroiddev.sc2buildassistant.feature.brief.view.BriefActivity
import com.kiwiandroiddev.sc2buildassistant.feature.brief.view.BriefViewModel
import com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        ApplicationModule::class,
        AndroidModule::class,
        NavigationModule::class,
        ErrorReporterModule::class,
        SettingsModule::class,
        BriefModule::class,
        TranslationModule::class))
interface ApplicationComponent {
    fun inject(target: MyApplication)
    fun inject(target: SettingsFragment)
    fun inject(target: BriefActivity)
    fun inject(target: BriefViewModel)
}