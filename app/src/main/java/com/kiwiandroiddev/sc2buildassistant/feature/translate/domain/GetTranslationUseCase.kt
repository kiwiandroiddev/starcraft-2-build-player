package com.kiwiandroiddev.sc2buildassistant.feature.translate.domain

import io.reactivex.Single

interface GetTranslationUseCase {

    fun getTranslation(fromLanguageCode: String, toLanguageCode: String, sourceText: String): Single<String>

}