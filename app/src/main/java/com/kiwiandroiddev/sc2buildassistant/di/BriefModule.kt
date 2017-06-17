package com.kiwiandroiddev.sc2buildassistant.di

import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter
import com.kiwiandroiddev.sc2buildassistant.feature.brief.data.SqliteBuildRepository
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.BuildRepository
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl.GetBuildUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.GetSettingsUseCase
import dagger.Module
import dagger.Provides
import io.reactivex.android.schedulers.AndroidSchedulers
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
                              navigator: BriefNavigator) =
            BriefPresenter(getBuildUseCase, getSettingsUseCase, navigator, AndroidSchedulers.mainThread())

    @Provides
    @Singleton
    fun provideGetBuildUseCase(buildRepository: BuildRepository): GetBuildUseCase =
            GetBuildUseCaseImpl(buildRepository)

    @Provides
    @Singleton
    fun provideBuildRepository(dbAdapter: DbAdapter): BuildRepository =
            SqliteBuildRepository(dbAdapter)

}

