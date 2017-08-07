package com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface

import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.LanguageCode
import io.reactivex.Single

/**
 * Created by matthome on 15/07/17.
 */
interface TranslationAgent {

    fun getTranslation(fromLanguageCode: LanguageCode,
                       toLanguageCode: LanguageCode,
                       sourceText: String): Single<String>

}