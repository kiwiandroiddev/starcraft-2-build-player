package com.kiwiandroiddev.sc2buildassistant.di

import android.content.Context
import com.kiwiandroiddev.sc2buildassistant.di.qualifiers.ApplicationContext
import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.data.LoadStandardBuildsIntoDatabaseUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.LoadStandardBuildsIntoDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.datainterface.ClearDatabaseAgent
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl.ResetDatabaseUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsPresenter
import dagger.Module
import dagger.Provides
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Singleton

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
                    executionScheduler = Schedulers.io(),
                    viewResultScheduler = AndroidSchedulers.mainThread())

    @Provides
    @Singleton
    fun provideResetDatabaseUseCase(clearDatabaseAgent: ClearDatabaseAgent,
                                    loadStandardBuildsIntoDatabaseUseCase: LoadStandardBuildsIntoDatabaseUseCase): ResetDatabaseUseCase =
            ResetDatabaseUseCaseImpl(clearDatabaseAgent, loadStandardBuildsIntoDatabaseUseCase)

    @Provides
    @Singleton
    fun provideLoadStandardBuildsIntoDatabaseUseCase(@ApplicationContext appContext: Context): LoadStandardBuildsIntoDatabaseUseCase =
            LoadStandardBuildsIntoDatabaseUseCaseImpl(appContext)

}