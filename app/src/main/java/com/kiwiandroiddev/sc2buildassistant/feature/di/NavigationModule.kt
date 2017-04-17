package com.kiwiandroiddev.sc2buildassistant.feature.di

import com.kiwiandroiddev.sc2buildassistant.feature.navigation.RegisteredActivityNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsNavigator
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Copyright © 2017. Orion Health. All rights reserved.
 */
@Module
class NavigationModule {

    @Provides
    @Singleton
    fun provideRegisteredActivityNavigator() = RegisteredActivityNavigator()

    @Provides
    @Singleton
    fun provideSettingsNavigator(registeredActivityNavigator: RegisteredActivityNavigator): SettingsNavigator =
            registeredActivityNavigator

}

