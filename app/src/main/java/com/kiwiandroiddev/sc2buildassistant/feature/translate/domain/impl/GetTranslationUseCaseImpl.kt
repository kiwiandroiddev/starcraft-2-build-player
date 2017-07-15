package com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.GetTranslationUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.TranslationAgent
import io.reactivex.Single

class GetTranslationUseCaseImpl(val translationAgent: TranslationAgent) : GetTranslationUseCase {

    override fun getTranslation(fromLanguageCode: String,
                                toLanguageCode: String,
                                sourceText: String): Single<String> =
            translationAgent.getTranslation(fromLanguageCode, toLanguageCode, sourceText)

}