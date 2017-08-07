package com.kiwiandroiddev.sc2buildassistant.feature.translate.data.cache

import com.kiwiandroiddev.sc2buildassistant.feature.cache.Cache
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.LanguageCode
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.TranslationAgent
import io.reactivex.Single

/**
 * Created by matthome on 6/08/17.
 */
class CachedTranslationAgent(val translationAgent: TranslationAgent,
                             val translationCache: Cache<String>) : TranslationAgent {

    override fun getTranslation(fromLanguageCode: LanguageCode,
                                toLanguageCode: LanguageCode,
                                sourceText: String): Single<String> =
            translationCache.get(buildCacheKey(fromLanguageCode, toLanguageCode, sourceText))
                    .onErrorResumeNext { e: Throwable ->
                        if (e is Cache.NoValueForKey) {
                            translationAgent.getTranslation(fromLanguageCode, toLanguageCode, sourceText)
                                    .flatMap { translatedText ->
                                        translationCache.put(
                                                buildCacheKey(fromLanguageCode, toLanguageCode, sourceText), translatedText)
                                                .andThen(Single.just(translatedText))
                                                .onErrorReturn { translatedText }
                                    }
                        } else {
                            Single.error(e)
                        }
                    }

    private fun buildCacheKey(fromLanguageCode: LanguageCode,
                              toLanguageCode: LanguageCode,
                              sourceText: String) =
            Triple(fromLanguageCode, toLanguageCode, sourceText)
                    .hashCode().toString()

}