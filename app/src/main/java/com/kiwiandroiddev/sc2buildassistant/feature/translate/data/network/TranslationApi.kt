package com.kiwiandroiddev.sc2buildassistant.feature.translate.data.network

import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Created by matthome on 15/07/17.
 */
interface TranslationApi {

    @POST("/")
    fun translate(@Body translateQuery: TranslateQuery): Single<String>

    @GET("/getLanguages")
    fun languageCodes(): Single<List<String>>

    data class TranslateQuery(val from: String? = null,
                              val to: String,
                              val text: String)

}

