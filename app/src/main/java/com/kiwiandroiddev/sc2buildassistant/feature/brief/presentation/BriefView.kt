package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

/**
 * Created by Matt Clarke on 28/04/17.
 */
interface BriefView {
    fun  render(viewState: BriefViewState)

    data class BriefViewState(val showAds: Boolean,
                              val showLoadError: Boolean,
                              val briefText: String?)
}