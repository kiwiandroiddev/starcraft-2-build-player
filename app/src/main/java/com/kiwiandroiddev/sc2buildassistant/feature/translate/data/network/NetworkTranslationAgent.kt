package com.kiwiandroiddev.sc2buildassistant.feature.translate.data.network

import com.kiwiandroiddev.sc2buildassistant.feature.translate.data.network.TranslationApi.*
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.TranslationAgent
import io.reactivex.Single

/**
 * Created by matthome on 15/07/17.
 */
class NetworkTranslationAgent(val translationApi: TranslationApi) : TranslationAgent {

    override fun getTranslation(fromLanguageCode: String,
                                toLanguageCode: String,
                                sourceText: String): Single<String> =
            translationApi.translate(TranslateQuery(fromLanguageCode, toLanguageCode, sourceText))

}