package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

/**
 * Created by Matt Clarke on 24/06/17.
 */
interface BriefPresenter {
    fun attachView(view: BriefView)
    fun detachView()
}