package com.kiwiandroiddev.sc2buildassistant.feature.translate.data.network

import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.LanguageCode
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.SupportedLanguagesAgent
import io.reactivex.Observable

/**
 * Created by matthome on 31/07/17.
 */
class NetworkSupportedLanguagesAgent(val translationApi: TranslationApi) : SupportedLanguagesAgent {

    override fun supportedLanguages(): Observable<LanguageCode> =
            translationApi
                    .languageCodes()
                    .flatMapObservable { codes ->
                        Observable.fromIterable(codes)
                    }

}