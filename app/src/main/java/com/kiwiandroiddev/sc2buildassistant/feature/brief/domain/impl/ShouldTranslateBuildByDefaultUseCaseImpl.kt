package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.ShouldTranslateBuildByDefaultUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.TranslateByDefaultPreferenceAgent
import io.reactivex.Completable
import io.reactivex.Single

class ShouldTranslateBuildByDefaultUseCaseImpl(val translateByDefaultPreferenceAgent: TranslateByDefaultPreferenceAgent)
    : ShouldTranslateBuildByDefaultUseCase {

    override fun shouldTranslateByDefault(buildId: Long): Single<Boolean> =
            translateByDefaultPreferenceAgent.shouldTranslateByDefault(buildId)

    override fun setTranslateByDefaultPreference(buildId: Long): Completable =
            translateByDefaultPreferenceAgent.setTranslateByDefaultPreference(buildId)

    override fun clearTranslateByDefaultPreference(buildId: Long): Completable =
            translateByDefaultPreferenceAgent.clearTranslateByDefaultPreference(buildId)

}