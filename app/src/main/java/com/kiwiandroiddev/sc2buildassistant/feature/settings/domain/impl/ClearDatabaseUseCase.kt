package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl

import io.reactivex.Completable

interface ClearDatabaseUseCase {
    fun clear(): Completable
}