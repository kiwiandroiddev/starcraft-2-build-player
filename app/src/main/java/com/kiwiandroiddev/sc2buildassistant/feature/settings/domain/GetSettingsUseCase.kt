package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain

import io.reactivex.Observable

interface GetSettingsUseCase {
    fun showAds(): Observable<Boolean>
}