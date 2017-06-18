package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import java.io.Serializable

/**
 * Created by Matt Clarke on 28/04/17.
 */
interface BriefView {
    fun render(viewState: BriefViewState)

    data class BriefViewState(val showAds: Boolean,
                              val showLoadError: Boolean,
                              val briefText: String?,
                              val buildSource: String?,
                              val buildAuthor: String?) : Serializable
}