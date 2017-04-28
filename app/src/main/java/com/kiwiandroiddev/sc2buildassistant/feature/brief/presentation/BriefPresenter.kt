package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetSettingsUseCase
import io.reactivex.disposables.Disposable

/**
 * Created by Matt Clarke on 28/04/17.
 */
class BriefPresenter(val getSettingsUseCase: GetSettingsUseCase,
                     val navigator: BriefNavigator) {

    private var view: BriefView? = null
    private var buildId: Long? = null
    private var disposable: Disposable? = null

    fun attachView(view: BriefView, buildId: Long) {
        this.view = view
        this.buildId = buildId

        disposable = getSettingsUseCase.showAds()
                .startWith(false)
                .map { showAds -> BriefView.BriefViewState(showAds = showAds) }
                .distinct()
                .subscribe { viewState -> view.render(viewState) }
    }

    fun detachView() {
        view = null
        disposable?.dispose()
    }

    fun onPlayBuildSelected() {
        ensureViewAttached()

        navigator.onPlayBuild(buildId!!)
    }

    fun onEditBuildSelected() {
        ensureViewAttached()

        navigator.onEditBuild(buildId!!)
    }

    fun onSettingsSelected() {
        ensureViewAttached()

        navigator.onOpenSettings()
    }

    private fun ensureViewAttached() {
        if (view == null) throw IllegalStateException("no view attached")
    }

}