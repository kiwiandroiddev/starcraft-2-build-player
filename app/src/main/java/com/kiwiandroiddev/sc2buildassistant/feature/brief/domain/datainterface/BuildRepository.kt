package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import io.reactivex.Single

interface BuildRepository {

    fun getBuildForId(buildId: Long): Single<Build>

}