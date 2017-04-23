package com.kiwiandroiddev.sc2buildassistant.feature.di

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl.ClearDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl.ResetDatabaseUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsPresenter
import dagger.Module
import dagger.Provides
import io.reactivex.Completable
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

    @Provides
    @Singleton
    fun provideResetDatabaseUseCase(clearDatabaseUseCase: ClearDatabaseUseCase): ResetDatabaseUseCase =
            ResetDatabaseUseCaseImpl(clearDatabaseUseCase)

    // TODO stub
    @Provides
    @Singleton
    fun provideClearDatabaseUseCase(): ClearDatabaseUseCase =
            object : ClearDatabaseUseCase {
                override fun clear(): Completable = Completable.complete()
            }

}