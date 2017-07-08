package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetCurrentLanguageUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView.BriefViewState
import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.GetSettingsUseCase
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable

/**
 * Created by Matt Clarke on 28/04/17.
 */
class BriefPresenterImpl(val getBuildUseCase: GetBuildUseCase,
                         val getSettingsUseCase: GetSettingsUseCase,
                         val getCurrentLanguageUseCase: GetCurrentLanguageUseCase,
                         val checkTranslationPossibleUseCase: CheckTranslationPossibleUseCase,
                         val navigator: BriefNavigator,
                         val errorReporter: ErrorReporter,
                         val postExecutionScheduler: Scheduler) : BriefPresenter {

    companion object {
        private val INITIAL_VIEW_STATE = BriefViewState(
                showAds = false,
                showLoadError = false,
                showTranslateOption = false,
                briefText = null,
                buildSource = null,
                buildAuthor = null
        )
    }

    private var view: BriefView? = null
    private var disposable: Disposable? = null

    override fun attachView(view: BriefView) {
        this.view = view

        setupViewStateStream(view, view.getBuildId())
    }

    private sealed class Result {
        data class ShowAdsResult(val showAds: Boolean) : Result()

        data class ShowTranslateOptionResult(val showTranslateOption: Boolean) : Result()

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
//                .doOnNext { System.out.println("result = $it") }
                .compose(this::reduceToViewState)
                .observeOn(postExecutionScheduler)
                .subscribe(view::render)
    }

    private fun getNavigationResults(viewEvents: Observable<BriefView.BriefViewEvent>): Observable<Result> =
            viewEvents.flatMap { viewEvent ->
                when (viewEvent) {
                    is BriefView.BriefViewEvent.PlaySelected -> navigateToPlayer(view?.getBuildId()!!)
                    is BriefView.BriefViewEvent.EditSelected -> navigateToEditor(view?.getBuildId()!!)
                    is BriefView.BriefViewEvent.SettingsSelected -> navigateToSettings()
                }
            }

    private fun navigateToPlayer(buildId: Long): Observable<Result.SuccessfulNavigationResult> =
            Observable.fromCallable {
                navigator.onPlayBuild(buildId)
                Result.SuccessfulNavigationResult()
            }

    private fun navigateToEditor(buildId: Long): Observable<Result.SuccessfulNavigationResult> =
            Observable.fromCallable {
                navigator.onEditBuild(buildId)
                Result.SuccessfulNavigationResult()
            }

    private fun navigateToSettings(): Observable<Result.SuccessfulNavigationResult> =
            Observable.fromCallable {
                navigator.onOpenSettings()
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

                    is Result.ShowTranslateOptionResult ->
                        lastViewState.copy(showTranslateOption = result.showTranslateOption)

                    is Result.SuccessfulNavigationResult -> lastViewState    // do nothing to view state
                }
            }

    private fun loadBuildResults(buildId: Long): Observable<Result> =
            getBuildUseCase.getBuild(buildId).toObservable()
                    .map { build ->
                        Result.LoadBuildResult.Success(build) as Result.LoadBuildResult
                    }
                    .onErrorReturn { error -> Result.LoadBuildResult.LoadFailure(error) }
                    .flatMap { loadBuildResult ->
                        when (loadBuildResult) {
                            is Result.LoadBuildResult.LoadFailure -> Observable.just(loadBuildResult)
                            is Result.LoadBuildResult.Success ->
                                Observable.merge(
                                        Observable.just(loadBuildResult as Result),
                                        showTranslateOptionResults(loadBuildResult.build)
                                )
                        }
                    }

    private fun showTranslateOptionResults(build: Build): Observable<Result> =
            getCurrentLanguageUseCase.getLanguageCode().toObservable()
                    .flatMap { currentLanguage ->
                        if (build.isoLanguageCode == null || currentLanguage == build.isoLanguageCode) {
                            Observable.just<Result>(Result.ShowTranslateOptionResult(showTranslateOption = false))
                        } else {
                            checkTranslationPossibleUseCase.canTranslateFromLanguage(
                                    fromLanguageCode = build.isoLanguageCode!!, toLanguageCode = currentLanguage)
                                    .map { canTranslateBuild ->
                                        Result.ShowTranslateOptionResult(canTranslateBuild) as Result
                                    }
                                    .toObservable()
                                    .onErrorResumeNext { _: Throwable -> Observable.empty<Result>() }
                        }
                    }.doOnError { error -> errorReporter.trackNonFatalError(error) }

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

    override fun detachView() {
        view = null
        disposable?.dispose()
    }

}