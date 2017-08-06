package com.kiwiandroiddev.sc2buildassistant.di

import android.content.Context
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter
import com.kiwiandroiddev.sc2buildassistant.di.qualifiers.ApplicationContext
import com.kiwiandroiddev.sc2buildassistant.feature.brief.data.GetCurrentSystemLanguageAgent
import com.kiwiandroiddev.sc2buildassistant.feature.brief.data.SqliteBuildRepository
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetCurrentLanguageUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.ShouldTranslateBuildByDefaultUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.BuildRepository
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.GetCurrentLanguageAgent
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl.GetBuildUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl.GetCurrentLanguageUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenter
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenterImpl
import com.kiwiandroiddev.sc2buildassistant.feature.common.androidview.AndroidStringResolver
import com.kiwiandroiddev.sc2buildassistant.feature.common.presentation.StringResolver
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.CheckTranslationPossibleUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.GetTranslationUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.GetSettingsUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.TranslationAgent
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.impl.GetTranslationUseCaseImpl
import dagger.Module
import dagger.Provides
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
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
                              shouldTranslateBuildByDefaultUseCase: ShouldTranslateBuildByDefaultUseCase,
                              getTranslationUseCase: GetTranslationUseCase,
                              stringResolver: StringResolver,
                              errorReporter: ErrorReporter,
                              navigator: BriefNavigator): BriefPresenter =
            BriefPresenterImpl(
                    getBuildUseCase,
                    getSettingsUseCase,
                    getCurrentLanguageUseCase,
                    checkTranslationPossibleUseCase,
                    shouldTranslateBuildByDefaultUseCase,
                    getTranslationUseCase,
                    navigator,
                    errorReporter,
                    stringResolver,
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
    fun provideShouldTranslateBuildByDefaultUseCase(): ShouldTranslateBuildByDefaultUseCase =
            object : ShouldTranslateBuildByDefaultUseCase {

                private val preferenceMap: MutableMap<Long, Boolean> = HashMap()

                override fun setTranslateByDefaultPreference(buildId: Long): Completable {
                    return Completable.fromAction { preferenceMap[buildId] = true }
                }

                override fun clearTranslateByDefaultPreference(buildId: Long): Completable {
                    return Completable.fromAction { preferenceMap[buildId] = false }
                }

                override fun shouldTranslateByDefault(buildId: Long): Single<Boolean> {
                    val translateNow = preferenceMap[buildId] ?: false
                    return Single.just(translateNow)
                }
            }

    @Provides
    @Singleton
    fun provideStringResolver(@ApplicationContext context: Context): StringResolver =
            AndroidStringResolver(context)

    @Provides
    @Singleton
    fun provideBuildRepository(dbAdapter: DbAdapter): BuildRepository =
            SqliteBuildRepository(dbAdapter)

    @Provides
    @Singleton
    fun provideGetCurrentLanguageAgent(@ApplicationContext context: Context): GetCurrentLanguageAgent =
            GetCurrentSystemLanguageAgent(context)

}

