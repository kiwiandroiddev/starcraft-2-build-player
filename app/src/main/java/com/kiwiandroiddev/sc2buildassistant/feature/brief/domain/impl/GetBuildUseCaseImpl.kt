package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.BuildRepository
import io.reactivex.Single

class GetBuildUseCaseImpl(val buildRepository: BuildRepository) : GetBuildUseCase {

    override fun getBuild(buildId: Long): Single<Build> =
            buildRepository.getBuildForId(buildId)

}