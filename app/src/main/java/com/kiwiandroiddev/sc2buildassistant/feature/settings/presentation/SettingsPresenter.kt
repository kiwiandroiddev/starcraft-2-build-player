package com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase

/**
 * Copyright © 2017. Orion Health. All rights reserved.
 */

class SettingsPresenter(val resetDatabaseUseCase: ResetDatabaseUseCase,
                        val navigator: SettingsNavigator) {

    companion object {
        val TRANSLATE_URL = "http://www.getlocalization.com/sc2buildplayer/"
    }

    private var view: SettingsView? = null

    fun attachView(view: SettingsView) {
        this.view = view
    }

    fun showChangelogSelected() {
        navigator.openFullChangelog()
    }

    fun rateAppSelected() {
        navigator.openPlayStoreListing()
    }

    fun translateSelected() {
        navigator.openUrl(TRANSLATE_URL)
    }

    fun resetDatabaseSelected() {
        view!!.showResetDatabaseConfirmation()
    }

    fun confirmResetDatabaseSelected() {
        resetDatabaseUseCase.resetDatabase().subscribe()
    }

}
