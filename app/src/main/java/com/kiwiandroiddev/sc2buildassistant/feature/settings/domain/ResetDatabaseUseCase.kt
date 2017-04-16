package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain

import rx.Observable

interface ResetDatabaseUseCase {
    fun resetDatabase(): Observable<Void>
}