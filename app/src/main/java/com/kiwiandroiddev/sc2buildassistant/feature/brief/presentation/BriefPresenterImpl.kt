package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.CheckTranslationPossibleUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetCurrentLanguageUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefPresenterImpl.Result.*
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.GetTranslationUseCase
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
                         val getTranslationUseCase: GetTranslationUseCase,
                         val navigator: BriefNavigator,
                         val errorReporter: ErrorReporter,
                         val postExecutionScheduler: Scheduler) : BriefPresenter {

    companion object {
        private val INITIAL_VIEW_STATE = BriefViewState(
                showAds = false,
                showLoadError = false,
                showTranslateOption = false,
                showTranslationError = false,
                translationLoading = false,
                showRevertTranslationOption = false,
                briefText = null,
                buildSource = null,
                buildAuthor = null
        )
    }

    private sealed class Result {
        data class ShowAdsResult(val showAds: Boolean) : Result()

        data class ShowTranslateOptionResult(val showTranslateOption: Boolean) : Result()

        data class RevertTranslationResult(val untranslatedBrief: String?,
                                           val reshowTranslateOption: Boolean) : Result()

        sealed class LoadBuildResult : Result() {
            data class Success(val build: Build) : LoadBuildResult()
            data class LoadFailure(val cause: Throwable) : LoadBuildResult()
        }

        class SuccessfulNavigationResult : Result()

        sealed class TranslationResult : Result() {
            class Loading : TranslationResult()
            data class Success(val translatedBrief: String) : TranslationResult()
            data class Failure(val cause: Throwable) : TranslationResult()
        }
    }

    private var view: BriefView? = null
    private var disposable: Disposable? = null

    override fun attachView(view: BriefView) {
        this.view = view

        setupViewStateStream(view, view.getBuildId())
    }

    private fun setupViewStateStream(view: BriefView, buildId: Long) {
        val allResults: Observable<Result> = Observable.merge(
                loadBuildResults(buildId),
                showAdResults(),
                getViewEventResults(view.getViewEvents())
        )

        disposable = allResults
                .compose(this::reduceToViewState)
                .observeOn(postExecutionScheduler)
                .subscribe(view::render)
    }

    private fun getViewEventResults(viewEvents: Observable<BriefView.BriefViewEvent>): Observable<Result> =
            viewEvents.flatMap { viewEvent ->
                when (viewEvent) {
                    is BriefView.BriefViewEvent.PlaySelected -> navigateToPlayer(view?.getBuildId()!!)
                    is BriefView.BriefViewEvent.EditSelected -> navigateToEditor(view?.getBuildId()!!)
                    is BriefView.BriefViewEvent.SettingsSelected -> navigateToSettings()
                    is BriefView.BriefViewEvent.TranslateSelected -> getTranslateBuildResults()
                    is BriefView.BriefViewEvent.RevertTranslationSelected -> getRevertTranslationResult()
                }
            }

    private fun navigateToPlayer(buildId: Long): Observable<Result.SuccessfulNavigationResult> =
            Observable.fromCallable {
                navigator.onPlayBuild(buildId)
                SuccessfulNavigationResult()
            }

    private fun navigateToEditor(buildId: Long): Observable<Result.SuccessfulNavigationResult> =
            Observable.fromCallable {
                navigator.onEditBuild(buildId)
                SuccessfulNavigationResult()
            }

    private fun navigateToSettings(): Observable<Result.SuccessfulNavigationResult> =
            Observable.fromCallable {
                navigator.onOpenSettings()
                SuccessfulNavigationResult()
            }

    private fun getRevertTranslationResult(): Observable<Result> {
        return getBuild().toObservable().flatMap { build ->
            isTranslationAvailableForBuild(build).toObservable()
                    .flatMap { translationAvailable ->
                        Observable.just(RevertTranslationResult(
                                untranslatedBrief = build.notes,
                                reshowTranslateOption = translationAvailable
                        ))
                    }
        }
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

                    is Result.TranslationResult.Loading ->
                        lastViewState.copy(translationLoading = true)

                    is Result.TranslationResult.Success ->
                        lastViewState.copy(
                                briefText = result.translatedBrief,
                                translationLoading = false,
                                showTranslateOption = false,
                                showRevertTranslationOption = true)

                    is Result.TranslationResult.Failure ->
                        lastViewState.copy(showTranslationError = true, translationLoading = false)

                    is Result.RevertTranslationResult -> lastViewState.copy(
                            briefText = result.untranslatedBrief,
                            showRevertTranslationOption = false,
                            showTranslateOption = result.reshowTranslateOption
                    )
                }
            }

    private fun loadBuildResults(buildId: Long): Observable<Result> =
            getBuildUseCase.getBuild(buildId).toObservable()
                    .map { build ->
                        LoadBuildResult.Success(build) as Result.LoadBuildResult
                    }
                    .onErrorReturn { error ->
                        error.printStackTrace()
                        LoadBuildResult.LoadFailure(error)
                    }
                    .flatMap { loadBuildResult ->
                        when (loadBuildResult) {
                            is Result.LoadBuildResult.LoadFailure -> Observable.just(loadBuildResult)
                            is Result.LoadBuildResult.Success ->
                                Observable.merge(
                                        Observable.just(loadBuildResult as Result),
                                        showTranslateOptionResult(loadBuildResult.build).toObservable()
                                )
                        }
                    }

    private fun showTranslateOptionResult(build: Build): Single<Result> =
            isTranslationAvailableForBuild(build)
                    .map { available -> ShowTranslateOptionResult(available) as Result }

    private fun isTranslationAvailableForBuild(build: Build): Single<Boolean> =
            build.hasLanguageCodeAndNotes()
                    .flatMap { haveBuildNotesAndLanguage ->
                        when (haveBuildNotesAndLanguage) {
                            true -> canTranslateBuildToCurrentLanguage(build)
                            false -> Single.just(false)
                        }
                    }

    private fun Build.hasLanguageCodeAndNotes() =
            Single.fromCallable { notes != null && isoLanguageCode != null }

    private fun canTranslateBuildToCurrentLanguage(build: Build): Single<Boolean> =
            getCurrentLanguage().flatMap { currentLanguage ->
                if (currentLanguage != build.isoLanguageCode) {
                    translationPossibleBetweenLanguages(
                            from = build.isoLanguageCode!!,
                            to = currentLanguage
                    )
                } else {
                    Single.just(false)
                }
            }

    private fun getCurrentLanguage(): Single<String> =
            getCurrentLanguageUseCase.getLanguageCode().doOnError { getCurrentLanguageError ->
                errorReporter.trackNonFatalError(getCurrentLanguageError)
            }

    private fun translationPossibleBetweenLanguages(from: String, to: String): Single<Boolean> =
            checkTranslationPossibleUseCase.canTranslateFromLanguage(
                    fromLanguageCode = from,
                    toLanguageCode = to)
                    .onErrorReturn { error -> error.printStackTrace(); false }

    private fun getTranslateBuildResults(): Observable<Result> =
            getBuild().toObservable().flatMap { build ->
                isTranslationAvailableForBuild(build).toObservable()
                        .flatMap { translationAvailable ->
                            when {
                                translationAvailable -> {
                                    getTranslateBuildToCurrentLanguageResults(build)
                                }
                                else -> {
                                    val error = IllegalStateException("Translate selected when translation not available or needed")
                                    errorReporter.trackNonFatalError(error)
                                    Observable.just(TranslationResult.Failure(error))
                                }
                            }
                        }
            }.map { it as Result }

    private fun getTranslateBuildToCurrentLanguageResults(build: Build): Observable<Result.TranslationResult> =
            getCurrentLanguage().toObservable()
                    .flatMap { currentLanguageCode ->
                        getTranslationUseCase.getTranslation(
                                fromLanguageCode = build.isoLanguageCode!!, // already know this won't be null from earlier but still
                                toLanguageCode = currentLanguageCode,
                                sourceText = build.notes!!)
                                .toObservable()
                                .map { translatedBrief ->
                                    TranslationResult.Success(translatedBrief) as Result.TranslationResult
                                }
                                .onErrorReturn { error ->
                                    error.printStackTrace()
                                    TranslationResult.Failure(error)
                                }
                                .startWith(TranslationResult.Loading())
                    }

    private fun getBuild(): Single<Build> =
            getBuildUseCase.getBuild(view?.getBuildId()!!)      // TODO stub

    private fun showAdResults(): Observable<Result.ShowAdsResult> =
            getSettingsUseCase.showAds()
                    .map { showAds -> ShowAdsResult(showAds) }
                    .onErrorReturn { error ->
                        error.printStackTrace()
                        ShowAdsResult(showAds = false)
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