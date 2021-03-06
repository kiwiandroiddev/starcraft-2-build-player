package com.kiwiandroiddev.sc2buildassistant.feature.translate.data.cache

import com.kiwiandroiddev.sc2buildassistant.feature.cache.Cache
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.LanguageCode
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.SupportedLanguagesAgent
import io.reactivex.Observable

/**
 * Created by matthome on 6/08/17.
 */
class CachedSupportedLanguagesAgent(val supportedLanguagesAgent: SupportedLanguagesAgent,
                                    val cache: Cache<Array<LanguageCode>>) : SupportedLanguagesAgent {

    private val CACHE_KEY = "SUPPORTED_LANGUAGES"

    override fun supportedLanguages(): Observable<LanguageCode> =
            cache.get(CACHE_KEY)
                    .flatMapObservable { codes -> Observable.fromIterable(codes.toList()) }
                .onErrorResumeNext { e: Throwable ->
                    if (e is Cache.NoValueForKey)
                        supportedLanguagesAgent.supportedLanguages()
                                .toList()
                                .flatMap { codes ->
                                    cache.put(CACHE_KEY, codes.toTypedArray())
                                            .toSingle { codes }
                                            .onErrorReturn { codes }
                                }
                                .flatMapObservable { codes -> Observable.fromIterable(codes) }
                    else
                        Observable.error(e)
                }

}