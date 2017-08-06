package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain

import io.reactivex.Single

interface GetCurrentLanguageUseCase {
    fun getLanguageCode(): Single<String>
}