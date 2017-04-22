package com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase

/**
 * Copyright © 2017. Orion Health. All rights reserved.
 */

class SettingsPresenter(val resetDatabaseUseCase: ResetDatabaseUseCase,
                        val navigator: SettingsNavigator) {

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
        resetDatabaseUseCase.resetDatabase().subscribe()
    }

}
