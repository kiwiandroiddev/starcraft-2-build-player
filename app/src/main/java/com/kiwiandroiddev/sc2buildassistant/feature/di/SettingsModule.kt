package com.kiwiandroiddev.sc2buildassistant.feature.di

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsPresenter
import dagger.Module
import dagger.Provides
import rx.Observable
import javax.inject.Singleton

/**
 * Copyright © 2017. Orion Health. All rights reserved.
 */
@Module
class SettingsModule {

    @Provides
    @Singleton
    fun provideSettingsPresenter(resetDatabaseUseCase: ResetDatabaseUseCase,
                                 navigator: SettingsNavigator): SettingsPresenter =
            SettingsPresenter(resetDatabaseUseCase, navigator)

    // TODO stub
    @Provides
    @Singleton
    fun provideResetDatabaseUseCase(): ResetDatabaseUseCase =
            object: ResetDatabaseUseCase {
                override fun resetDatabase(): Observable<Void> {
                    return Observable.just(null)
                }
            }

}