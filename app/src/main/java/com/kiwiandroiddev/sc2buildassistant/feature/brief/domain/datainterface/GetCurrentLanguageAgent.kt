package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface

import io.reactivex.Single

/**
 * Created by matthome on 8/07/17.
 */
interface GetCurrentLanguageAgent {
    fun getLanguageCode(): Single<String>
}