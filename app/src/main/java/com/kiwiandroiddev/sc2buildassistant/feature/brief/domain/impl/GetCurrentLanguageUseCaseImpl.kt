package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetCurrentLanguageUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.GetCurrentLanguageAgent
import io.reactivex.Single

class GetCurrentLanguageUseCaseImpl(val getCurrentLanguageAgent: GetCurrentLanguageAgent) : GetCurrentLanguageUseCase {

    override fun getLanguageCode(): Single<String> =
            getCurrentLanguageAgent.getLanguageCode()

}