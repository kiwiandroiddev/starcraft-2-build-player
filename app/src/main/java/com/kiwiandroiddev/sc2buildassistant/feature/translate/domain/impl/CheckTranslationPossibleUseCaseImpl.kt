package com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.CheckTranslationPossibleUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.LanguageCode
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.SupportedLanguagesAgent
import io.reactivex.Single

class CheckTranslationPossibleUseCaseImpl(val supportedLanguagesAgent: SupportedLanguagesAgent) : CheckTranslationPossibleUseCase {

    override fun canTranslateFromLanguage(fromLanguageCode: LanguageCode,
                                          toLanguageCode: LanguageCode): Single<Boolean> =
            supportedLanguagesAgent.supportedLanguages()
                    .toList()
                    .map { supportedCodes ->
                        supportedCodes.containsAll(
                                setOf(fromLanguageCode, toLanguageCode)
                        )
                    }
                    .onErrorReturn { false }

}