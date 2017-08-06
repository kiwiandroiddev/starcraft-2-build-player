package com.kiwiandroiddev.sc2buildassistant.di

import android.content.Context
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter
import com.kiwiandroiddev.sc2buildassistant.di.qualifiers.ApplicationContext
import com.kiwiandroiddev.sc2buildassistant.feature.brief.data.CachedTranslateByDefaultPreferenceAgent
import com.kiwiandroiddev.sc2buildassistant.feature.brief.data.GetCurrentSystemLanguageAgent
import com.kiwiandroiddev.sc2buildassistant.feature.brief.data.SqliteBuildRepository
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetCurrentLanguageUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.ShouldTranslateBuildByDefaultUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.BuildRepository
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.GetCurrentLanguageAgent
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.TranslateByDefaultPreferenceAgent
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl.GetBuildUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl.GetCurrentLanguageUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl.ShouldTranslateBuildByDefaultUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenter
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenterImpl
import com.kiwiandroiddev.sc2buildassistant.feature.cache.Cache
import com.kiwiandroiddev.sc2buildassistant.feature.cache.SharedPreferencesCache
import com.kiwiandroiddev.sc2buildassistant.feature.common.androidview.AndroidStringResolver
import com.kiwiandroiddev.sc2buildassistant.feature.common.presentation.StringResolver
import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.GetSettingsUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.CheckTranslationPossibleUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.GetTranslationUseCase
import dagger.Module
import dagger.Provides
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by Matt Clarke on 28/04/17.
 */
@Module
class BriefModule {

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class TranslatePreferenceCache

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
    fun provideShouldTranslateBuildByDefaultUseCase(translateByDefaultPreferenceAgent: TranslateByDefaultPreferenceAgent)
            : ShouldTranslateBuildByDefaultUseCase =
            ShouldTranslateBuildByDefaultUseCaseImpl(translateByDefaultPreferenceAgent)

    @Provides
    @Singleton
    fun provideTranslateByDefaultPreferenceAgent(@TranslatePreferenceCache cache: Cache<Boolean>)
            : TranslateByDefaultPreferenceAgent = CachedTranslateByDefaultPreferenceAgent(cache)

    @Provides
    @Singleton
    @TranslatePreferenceCache
    fun provideTranslatePreferenceCache(@ApplicationContext context: Context): Cache<Boolean> =
            SharedPreferencesCache<Boolean>(
                    context = context,
                    name = "com.kiwiandroiddev.sc2buildassistant.translatePreferenceCache",
                    classOfT = Boolean::class.java
            )

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

