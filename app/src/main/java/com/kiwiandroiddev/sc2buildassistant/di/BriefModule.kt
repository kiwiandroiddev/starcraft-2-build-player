package com.kiwiandroiddev.sc2buildassistant.di

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.GetSettingsUseCase
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
    fun provideBriefPresenter(getBuildUseCase: GetBuildUseCase,
                              getSettingsUseCase: GetSettingsUseCase,
                              navigator: BriefNavigator) = BriefPresenter(getBuildUseCase, getSettingsUseCase, navigator)

    @Provides
    @Singleton
    fun provideGetBuildUseCase(): GetBuildUseCase =
        object : GetBuildUseCase {
            override fun getBuild(buildId: Long): Observable<Build> =
                    Observable.error(NotImplementedError())
        }

}