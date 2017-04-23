package com.kiwiandroiddev.sc2buildassistant.feature.di

import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.LoadStandardBuildsIntoDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.datainterface.ClearDatabaseAgent
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl.ResetDatabaseUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsPresenter
import dagger.Module
import dagger.Provides
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Singleton

/**
 * Copyright © 2017. Orion Health. All rights reserved.
 */
@Module
class SettingsModule {

    @Provides
    @Singleton
    fun provideSettingsPresenter(resetDatabaseUseCase: ResetDatabaseUseCase,
                                 navigator: SettingsNavigator,
                                 errorReporter: ErrorReporter): SettingsPresenter =
            SettingsPresenter(
                    resetDatabaseUseCase = resetDatabaseUseCase,
                    navigator = navigator,
                    errorReporter = errorReporter,
                    viewResultScheduler = AndroidSchedulers.mainThread(), executionScheduler = Schedulers.io())

    @Provides
    @Singleton
    fun provideResetDatabaseUseCase(clearDatabaseAgent: ClearDatabaseAgent,
                                    loadStandardBuildsIntoDatabaseUseCase: LoadStandardBuildsIntoDatabaseUseCase): ResetDatabaseUseCase =
            ResetDatabaseUseCaseImpl(clearDatabaseAgent, loadStandardBuildsIntoDatabaseUseCase)

    @Provides
    @Singleton
    fun provideLoadStandardBuildsIntoDatabaseUseCase(): LoadStandardBuildsIntoDatabaseUseCase =
            object : LoadStandardBuildsIntoDatabaseUseCase {
                override fun loadBuilds(): Observable<Int> =
                        Observable.just(100)

            }

}