package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain

import io.reactivex.Single

interface CheckTranslationPossibleUseCase {
    fun canTranslateFromLanguage(fromLanguageCode: String,
                                 toLanguageCode: String): Single<Boolean>
}