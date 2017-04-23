package com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation

interface SettingsView {
    fun showResetDatabaseConfirmation()
    fun showResetDatabaseSuccess()
    fun showResetDatabaseError(detailedError: String)
}
