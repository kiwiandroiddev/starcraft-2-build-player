package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl

import rx.Observable

interface ClearDatabaseUseCase {
    fun clear(): Observable<Void>
}