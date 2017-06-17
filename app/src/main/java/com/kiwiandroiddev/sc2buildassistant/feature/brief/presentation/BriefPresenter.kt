package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetSettingsUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView.BriefViewState
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

/**
 * Created by Matt Clarke on 28/04/17.
 */
class BriefPresenter(val getBuildUseCase: GetBuildUseCase,
                     val getSettingsUseCase: GetSettingsUseCase,
                     val navigator: BriefNavigator) {

    companion object {
        private val INITIAL_VIEW_STATE = BriefViewState(
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

        setupViewStateStream(view, buildId)
    }

    private sealed class Result {
        data class ShowAdsResult(val showAds: Boolean) : Result()
        sealed class LoadBuildResult : Result() {
            data class Success(val build: Build) : LoadBuildResult()
            data class LoadFailure(val cause: Throwable) : LoadBuildResult()
        }
    }

    private fun setupViewStateStream(view: BriefView, buildId: Long) {
        val allResults: Observable<Result> = Observable.merge(loadBuildResults(buildId), showAdResults())
//                .doOnNext { log("allResults onNext = $it") }

        val viewStateObservable = allResults.scan(INITIAL_VIEW_STATE) { lastViewState, result ->
            when (result) {
                is Result.ShowAdsResult ->
                    lastViewState.copy(showAds = result.showAds)

                is BriefPresenter.Result.LoadBuildResult.Success ->
                    lastViewState.copy(briefText = result.build.notes)

                is BriefPresenter.Result.LoadBuildResult.LoadFailure ->
                    lastViewState.copy(showLoadError = true)
            }
        }

        disposable = viewStateObservable
//                .doOnNext { log("onNext = $it") }
                .subscribe(view::render)
    }

    private fun loadBuildResults(buildId: Long): Observable<Result.LoadBuildResult> =
            getBuildUseCase.getBuild(buildId)
                .map { build -> Result.LoadBuildResult.Success(build) as Result.LoadBuildResult }
                .onErrorReturn { error -> Result.LoadBuildResult.LoadFailure(error) }
//                .doOnNext { log("buildResult onNext = $it") }

    private fun showAdResults(): Observable<Result.ShowAdsResult> =
            getSettingsUseCase.showAds()
                .map { showAds -> Result.ShowAdsResult(showAds) }
                .onErrorReturn { error ->
                    error.printStackTrace()
                    Result.ShowAdsResult(showAds = false)
                }
//                .doOnNext { log("showAdResult onNext = $it") }

    private fun log(msg: String) {
        System.out.println(msg)
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