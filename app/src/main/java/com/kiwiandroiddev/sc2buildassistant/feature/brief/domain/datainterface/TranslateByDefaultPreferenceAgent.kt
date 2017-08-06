package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface

import io.reactivex.Completable
import io.reactivex.Single

/**
 * Created by matthome on 6/08/17.
 */
interface TranslateByDefaultPreferenceAgent {

    fun shouldTranslateByDefault(buildId: Long): Single<Boolean>
    fun setTranslateByDefaultPreference(buildId: Long): Completable
    fun clearTranslateByDefaultPreference(buildId: Long): Completable

}