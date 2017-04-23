package com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation

import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import io.reactivex.Scheduler
import io.reactivex.observers.DisposableCompletableObserver

class SettingsPresenter(val resetDatabaseUseCase: ResetDatabaseUseCase,
                        val navigator: SettingsNavigator,
                        val errorReporter: ErrorReporter,
                        val viewResultScheduler: Scheduler, val executionScheduler: Scheduler) {

    private var view: SettingsView? = null

    fun attachView(view: SettingsView) {
        this.view = view
    }

    fun detachView() {
        this.view = null
    }

    fun showChangelogSelected() {
        navigator.openFullChangelog()
    }

    fun rateAppSelected() {
        navigator.openPlayStoreListing()
    }

    fun translateSelected() {
        navigator.openTranslationPage()
    }

    fun resetDatabaseSelected() {
        view!!.showResetDatabaseConfirmation()
    }

    fun confirmResetDatabaseSelected() {
        resetDatabaseUseCase.resetDatabase()
                .subscribeOn(executionScheduler)
                .observeOn(viewResultScheduler)
                .subscribe(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        view?.showResetDatabaseSuccess()
                    }

                    override fun onError(error: Throwable) {
                        view?.showResetDatabaseError(error.message ?: "")
                        errorReporter.trackNonFatalError(error)
                    }
                })
    }

}
