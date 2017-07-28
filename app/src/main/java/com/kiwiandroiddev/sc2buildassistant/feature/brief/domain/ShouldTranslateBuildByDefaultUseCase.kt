package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain

import io.reactivex.Single

interface ShouldTranslateBuildByDefaultUseCase {

    fun shouldTranslateByDefault(buildId: Long): Single<Boolean>

}