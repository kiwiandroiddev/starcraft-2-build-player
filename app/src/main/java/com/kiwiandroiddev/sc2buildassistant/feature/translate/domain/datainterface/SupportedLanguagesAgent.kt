package com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface

import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.LanguageCode
import io.reactivex.Observable

interface SupportedLanguagesAgent {

    fun supportedLanguages(): Observable<LanguageCode>

}