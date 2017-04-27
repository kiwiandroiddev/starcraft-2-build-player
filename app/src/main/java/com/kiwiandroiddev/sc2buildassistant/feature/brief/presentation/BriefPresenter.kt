package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

/**
 * Created by Matt Clarke on 28/04/17.
 */
class BriefPresenter(val navigator: BriefNavigator) {

    private var view: BriefView? = null
    private var buildId: Long? = null

    fun attachView(view: BriefView, buildId: Long) {
        this.view = view
        this.buildId = buildId
    }

    fun detachView() {

    }

    fun onPlayBuildSelected() {
        ensureViewAttached()

        navigator.onPlayBuild(buildId!!)
    }

    private fun ensureViewAttached() {
        if (view == null) throw IllegalStateException("no view attached")
    }

}