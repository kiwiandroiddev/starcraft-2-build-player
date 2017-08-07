package com.kiwiandroiddev.sc2buildassistant.feature.brief.data

import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.BuildRepository
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class SqliteBuildRepository(val dbAdapter: DbAdapter) : BuildRepository {

    private val openDbAdapter: DbAdapter
        get() = dbAdapter.open()

    override fun getBuildForId(buildId: Long): Single<Build> =
            Single.just(openDbAdapter.fetchBuild(buildId))
                    .subscribeOn(Schedulers.io())

}