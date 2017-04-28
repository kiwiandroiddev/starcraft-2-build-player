package com.kiwiandroiddev.sc2buildassistant.di

import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetSettingsUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenter
import dagger.Module
import dagger.Provides
import io.reactivex.Observable
import javax.inject.Singleton

/**
 * Created by Matt Clarke on 28/04/17.
 */
@Module
class BriefModule {

    @Provides
    @Singleton
    fun provideBriefPresenter(getSettingsUseCase: GetSettingsUseCase,
                              navigator: BriefNavigator) = BriefPresenter(getSettingsUseCase, navigator)

    @Provides
    @Singleton
    fun provideGetSettingsUseCase(): GetSettingsUseCase =
            object : GetSettingsUseCase {
                override fun showAds(): Observable<Boolean> =
                        Observable.just(true)
            }

}