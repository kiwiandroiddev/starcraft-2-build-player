package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.datainterface

import io.reactivex.Observable

interface SettingsRepository {
    fun showAds(): Observable<Boolean>
}