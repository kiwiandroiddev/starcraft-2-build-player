package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetSettingsUseCase
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction

/**
 * Created by Matt Clarke on 28/04/17.
 */
class BriefPresenter(val getBuildUseCase: GetBuildUseCase,
                     val getSettingsUseCase: GetSettingsUseCase,
                     val navigator: BriefNavigator) {

    companion object {
        private val INITIAL_VIEW_STATE = BriefView.BriefViewState(
                showAds = false,
                showLoadError = false,
                briefText = null)
    }

    private var view: BriefView? = null
    private var buildId: Long? = null
    private var disposable: Disposable? = null

    fun attachView(view: BriefView, buildId: Long) {
        this.view = view
        this.buildId = buildId

        val showAdsSetting = getSettingsUseCase.showAds().onErrorReturn { false }
        val getBuild = getBuildUseCase.getBuild(buildId)

        val viewStateObservable: Observable<BriefView.BriefViewState> =
                Observable.combineLatest(showAdsSetting, getBuild,
                        BiFunction { showAds, build -> BriefView.BriefViewState(showAds, false, null) })

        disposable = viewStateObservable
                .startWith(INITIAL_VIEW_STATE)
                .onErrorReturn { BriefView.BriefViewState(showAds = false, showLoadError = true, briefText = null) }
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