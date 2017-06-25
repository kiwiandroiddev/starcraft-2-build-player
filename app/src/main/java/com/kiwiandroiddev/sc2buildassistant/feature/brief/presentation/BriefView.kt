package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import io.reactivex.Observable
import java.io.Serializable

/**
 * Created by Matt Clarke on 28/04/17.
 */
interface BriefView {

    fun getBuildId(): Long

    fun getViewEvents(): Observable<BriefViewEvent>

    fun render(viewState: BriefViewState)

    data class BriefViewState(val showAds: Boolean,
                              val showLoadError: Boolean,
                              val briefText: String?,
                              val buildSource: String?,
                              val buildAuthor: String?) : Serializable

    sealed class BriefViewEvent {
        class PlaySelected : BriefViewEvent()
        class EditSelected : BriefViewEvent()
        class SettingsSelected : BriefViewEvent()
    }

}

