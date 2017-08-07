package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import io.reactivex.Single

interface GetBuildUseCase {
    fun getBuild(buildId: Long): Single<Build>
}