package com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface

import io.reactivex.Single

/**
 * Created by matthome on 15/07/17.
 */
interface TranslationAgent {

    fun getTranslation(fromLanguageCode: String,
                       toLanguageCode: String,
                       sourceText: String): Single<String>

}