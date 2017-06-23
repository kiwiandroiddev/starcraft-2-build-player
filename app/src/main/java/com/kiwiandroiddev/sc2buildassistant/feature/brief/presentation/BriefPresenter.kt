package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView.BriefViewState
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.GetSettingsUseCase
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable

/**
 * Created by Matt Clarke on 28/04/17.
 */
class BriefPresenter(val getBuildUseCase: GetBuildUseCase,
                     val getSettingsUseCase: GetSettingsUseCase,
                     val navigator: BriefNavigator,
                     val postExecutionScheduler: Scheduler) {

    companion object {
        private val INITIAL_VIEW_STATE = BriefViewState(
                showAds = false,
                showLoadError = false,
                briefText = null,
                buildSource = null,
                buildAuthor = null
        )
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

        class SuccessfulNavigationResult : Result()
    }

    private fun setupViewStateStream(view: BriefView, buildId: Long) {
        val allResults: Observable<Result> = Observable.merge(
                loadBuildResults(buildId),
                showAdResults(),
                getNavigationResults(view.getViewEvents())
        )

        disposable = allResults
                .compose(this::reduceToViewState)
                .observeOn(postExecutionScheduler)
                .subscribe(view::render)
    }

    private fun getNavigationResults(viewEvents: Observable<BriefView.BriefViewEvent>): Observable<Result> =
            viewEvents.ofType(BriefView.BriefViewEvent.PlaySelected::class.java)
                    .flatMap { navigateToPlayer(buildId!!) }

    private fun navigateToPlayer(buildId: Long): Observable<Result.SuccessfulNavigationResult> =
            Observable.fromCallable {
                navigator.onPlayBuild(buildId)
                Result.SuccessfulNavigationResult()
            }

    private fun reduceToViewState(results: Observable<Result>): Observable<BriefViewState> =
            results.scan(INITIAL_VIEW_STATE) { lastViewState, result ->
                when (result) {
                    is Result.ShowAdsResult ->
                        lastViewState.copy(showAds = result.showAds)

                    is Result.LoadBuildResult.Success ->
                        updatedViewStateWithBuildInfo(lastViewState, result.build)

                    is Result.LoadBuildResult.LoadFailure ->
                        lastViewState.copy(showLoadError = true)

                    is BriefPresenter.Result.SuccessfulNavigationResult -> lastViewState    // do nothing to view state
                }
            }

    private fun loadBuildResults(buildId: Long): Observable<Result.LoadBuildResult> =
            getBuildUseCase.getBuild(buildId).toObservable()
                    .map { build -> Result.LoadBuildResult.Success(build) as Result.LoadBuildResult }
                    .onErrorReturn { error -> Result.LoadBuildResult.LoadFailure(error) }

    private fun showAdResults(): Observable<Result.ShowAdsResult> =
            getSettingsUseCase.showAds()
                    .map { showAds -> Result.ShowAdsResult(showAds) }
                    .onErrorReturn { error ->
                        error.printStackTrace()
                        Result.ShowAdsResult(showAds = false)
                    }

    private fun updatedViewStateWithBuildInfo(oldViewState: BriefViewState, build: Build): BriefViewState =
            with(build) {
                oldViewState.copy(
                        briefText = notes,
                        buildSource = source,
                        buildAuthor = author
                )
            }

    fun detachView() {
        view = null
        disposable?.dispose()
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