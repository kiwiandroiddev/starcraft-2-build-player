package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain

import io.reactivex.Completable

interface ResetDatabaseUseCase {
    fun resetDatabase(): Completable
}