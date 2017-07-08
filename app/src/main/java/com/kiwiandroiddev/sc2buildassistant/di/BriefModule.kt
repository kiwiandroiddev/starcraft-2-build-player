package com.kiwiandroiddev.sc2buildassistant.di

import android.content.Context
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter
import com.kiwiandroiddev.sc2buildassistant.di.qualifiers.ApplicationContext
import com.kiwiandroiddev.sc2buildassistant.feature.brief.data.GetCurrentSystemLanguageAgent
import com.kiwiandroiddev.sc2buildassistant.feature.brief.data.SqliteBuildRepository
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetCurrentLanguageUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.BuildRepository
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.GetCurrentLanguageAgent
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl.GetBuildUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl.GetCurrentLanguageUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenter
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenterImpl
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.CheckTranslationPossibleUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetTranslationUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.GetSettingsUseCase
import dagger.Module
import dagger.Provides
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Singleton

/**
 * Created by Matt Clarke on 28/04/17.
 */
@Module
class BriefModule {

    @Provides
    fun provideBriefPresenter(getBuildUseCase: GetBuildUseCase,
                              getSettingsUseCase: GetSettingsUseCase,
                              getCurrentLanguageUseCase: GetCurrentLanguageUseCase,
                              checkTranslationPossibleUseCase: CheckTranslationPossibleUseCase,
                              getTranslationUseCase: GetTranslationUseCase,
                              errorReporter: ErrorReporter,
                              navigator: BriefNavigator): BriefPresenter =
            BriefPresenterImpl(
                    getBuildUseCase,
                    getSettingsUseCase,
                    getCurrentLanguageUseCase,
                    checkTranslationPossibleUseCase,
                    getTranslationUseCase,
                    navigator,
                    errorReporter,
                    AndroidSchedulers.mainThread()
            )

    @Provides
    @Singleton
    fun provideGetBuildUseCase(buildRepository: BuildRepository): GetBuildUseCase =
            GetBuildUseCaseImpl(buildRepository)

    @Provides
    @Singleton
    fun provideGetCurrentLanguageUseCase(getCurrentLanguageAgent: GetCurrentLanguageAgent): GetCurrentLanguageUseCase =
            GetCurrentLanguageUseCaseImpl(getCurrentLanguageAgent)

    @Provides
    @Singleton
    fun provideCheckTranslationPossibleUseCase(): CheckTranslationPossibleUseCase =
            object : CheckTranslationPossibleUseCase {
                override fun canTranslateFromLanguage(fromLanguageCode: String, toLanguageCode: String): Single<Boolean> =
                        Single.just(true)   // TODO stub
            }

    @Provides
    @Singleton
    fun provideGetTranslationUseCase(): GetTranslationUseCase =
        object : GetTranslationUseCase {
            override fun getTranslation(fromLanguageCode: String, toLanguageCode: String, sourceText: String): Single<String> {
                return Single.error(NotImplementedError())
            }
        }

    @Provides
    @Singleton
    fun provideBuildRepository(dbAdapter: DbAdapter): BuildRepository =
            SqliteBuildRepository(dbAdapter)

    @Provides
    @Singleton
    fun provideGetCurrentLanguageAgent(@ApplicationContext context: Context): GetCurrentLanguageAgent =
            GetCurrentSystemLanguageAgent(context)

}

