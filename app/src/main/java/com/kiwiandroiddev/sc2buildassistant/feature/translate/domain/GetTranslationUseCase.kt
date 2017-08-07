package com.kiwiandroiddev.sc2buildassistant.feature.translate.domain

import io.reactivex.Single

interface GetTranslationUseCase {

    fun getTranslation(
            fromLanguageCode: LanguageCode,
            toLanguageCode: LanguageCode,
            sourceText: String
    ): Single<String>

}