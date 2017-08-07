package com.kiwiandroiddev.sc2buildassistant.feature.brief.data

import android.content.Context
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.GetCurrentLanguageAgent
import io.reactivex.Single

/**
 * Created by matthome on 8/07/17.
 */
class GetCurrentSystemLanguageAgent(val context: Context) : GetCurrentLanguageAgent {

    override fun getLanguageCode(): Single<String> =
            Single.just(context.resources.configuration.locale.language)

}