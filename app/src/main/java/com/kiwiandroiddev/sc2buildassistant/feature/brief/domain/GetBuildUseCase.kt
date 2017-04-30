package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import io.reactivex.Observable

interface GetBuildUseCase {
    fun getBuild(buildId: Long): Observable<Build>
}