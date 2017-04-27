package com.kiwiandroiddev.sc2buildassistant.feature.settings.data

import android.content.Context
import com.kiwiandroiddev.sc2buildassistant.di.qualifiers.ApplicationContext
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.LoadStandardBuildsIntoDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.service.JsonBuildService
import com.kiwiandroiddev.sc2buildassistant.service.StandardBuildsService
import io.reactivex.Observable

class LoadStandardBuildsIntoDatabaseUseCaseImpl(@ApplicationContext val appContext: Context) : LoadStandardBuildsIntoDatabaseUseCase {

    override fun loadBuilds(): Observable<Int> =
            StandardBuildsService.getLoadStandardBuildsIntoDBObservable(appContext, true)
                    .doOnComplete { JsonBuildService.notifyBuildProviderObservers(appContext) }

}