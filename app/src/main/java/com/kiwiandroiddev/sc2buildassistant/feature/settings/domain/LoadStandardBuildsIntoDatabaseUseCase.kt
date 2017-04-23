package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain

import io.reactivex.Observable

interface LoadStandardBuildsIntoDatabaseUseCase {
    fun loadBuilds(): Observable<Int>
}