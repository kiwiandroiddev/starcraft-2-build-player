package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain

import io.reactivex.Completable
import io.reactivex.Single

interface ShouldTranslateBuildByDefaultUseCase {

    fun shouldTranslateByDefault(buildId: Long): Single<Boolean>
    fun setTranslateByDefaultPreference(buildId: Long): Completable
    fun clearTranslateByDefaultPreference(buildId: Long): Completable

}