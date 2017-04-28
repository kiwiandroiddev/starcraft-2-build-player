package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain

import io.reactivex.Observable

interface GetSettingsUseCase {
    fun showAds(): Observable<Boolean>
}