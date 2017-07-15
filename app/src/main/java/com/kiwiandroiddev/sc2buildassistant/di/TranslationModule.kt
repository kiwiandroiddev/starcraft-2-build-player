package com.kiwiandroiddev.sc2buildassistant.di

import com.kiwiandroiddev.sc2buildassistant.feature.translate.data.network.NetworkTranslationAgent
import com.kiwiandroiddev.sc2buildassistant.feature.translate.data.network.TranslationApi
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.TranslationAgent
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Singleton

/**
 * Created by matthome on 15/07/17.
 */
@Module
class TranslationModule {

    companion object {
        const val TRANSLATION_API_BASE_URL = "https://sc2-cloud-translate.herokuapp.com/"
    }

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
    fun provideNetworkTranslationAgent(translationApi: TranslationApi): TranslationAgent =
            NetworkTranslationAgent(translationApi)

}