package com.kiwiandroiddev.sc2buildassistant.feature.brief.data

import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.TranslateByDefaultPreferenceAgent
import com.kiwiandroiddev.sc2buildassistant.feature.cache.Cache
import io.reactivex.Completable
import io.reactivex.Single

/**
 * Created by matthome on 6/08/17.
 */
class CachedTranslateByDefaultPreferenceAgent(val cache: Cache<Boolean>) : TranslateByDefaultPreferenceAgent {

    override fun shouldTranslateByDefault(buildId: Long): Single<Boolean> =
            cache.get(buildId.toString()).onErrorReturnItem(false)

    override fun setTranslateByDefaultPreference(buildId: Long): Completable =
            cache.put(buildId.toString(), true)

    override fun clearTranslateByDefaultPreference(buildId: Long): Completable =
            cache.put(buildId.toString(), false)

}