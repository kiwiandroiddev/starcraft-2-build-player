package com.kiwiandroiddev.sc2buildassistant.di

import com.kiwiandroiddev.sc2buildassistant.feature.cache.InMemoryCache
import com.kiwiandroiddev.sc2buildassistant.feature.translate.data.cache.CachedSupportedLanguagesAgent
import com.kiwiandroiddev.sc2buildassistant.feature.translate.data.cache.CachedTranslationAgent
import com.kiwiandroiddev.sc2buildassistant.feature.translate.data.network.NetworkSupportedLanguagesAgent
import com.kiwiandroiddev.sc2buildassistant.feature.translate.data.network.NetworkTranslationAgent
import com.kiwiandroiddev.sc2buildassistant.feature.translate.data.network.TranslationApi
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.CheckTranslationPossibleUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.GetTranslationUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.SupportedLanguagesAgent
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.TranslationAgent
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.impl.CheckTranslationPossibleUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.impl.GetTranslationUseCaseImpl
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by matthome on 15/07/17.
 */
@Module
class TranslationModule {

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Network

    companion object {
        const val TRANSLATION_API_BASE_URL = "https://sc2-cloud-translate.herokuapp.com/"
    }

    @Provides
    @Singleton
    fun provideCheckTranslationPossibleUseCase(supportedLanguagesAgent: SupportedLanguagesAgent): CheckTranslationPossibleUseCase =
            CheckTranslationPossibleUseCaseImpl(supportedLanguagesAgent)


    @Provides
    @Singleton
    fun provideGetTranslationUseCase(translationAgent: TranslationAgent): GetTranslationUseCase =
            GetTranslationUseCaseImpl(translationAgent)
//            object : GetTranslationUseCase {
//                override fun getTranslation(fromLanguageCode: String, toLanguageCode: String, sourceText: String): Single<String> =
//                        Observable.interval(5, TimeUnit.SECONDS).map { "Translated text here" }.firstOrError()
//            }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        return Retrofit.Builder()
                .baseUrl(TRANSLATION_API_BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
    }

    @Provides
    @Singleton
    fun provideTranslationApi(retrofit: Retrofit): TranslationApi =
            retrofit.create(TranslationApi::class.java)

    @Provides
    @Singleton
    @Network
    fun provideNetworkTranslationAgent(translationApi: TranslationApi): TranslationAgent =
            NetworkTranslationAgent(translationApi)

    @Provides
    @Singleton
    fun provideCachedTranslationAgent(
            @Network networkTranslationAgent: TranslationAgent
    ): TranslationAgent = CachedTranslationAgent(
            networkTranslationAgent,
            InMemoryCache()     // TODO stub
    )

    @Provides
    @Singleton
    @Network
    fun provideNetworkSupportedLanguagesAgent(translationApi: TranslationApi): SupportedLanguagesAgent =
            NetworkSupportedLanguagesAgent(translationApi)

    @Provides
    @Singleton
    fun provideCachedSupportedLanguagesAgent(
            @Network networkSupportedLanguagesAgent: SupportedLanguagesAgent
    ): SupportedLanguagesAgent =
            CachedSupportedLanguagesAgent(
                    networkSupportedLanguagesAgent,
                    InMemoryCache()     // TODO stub
            )

}