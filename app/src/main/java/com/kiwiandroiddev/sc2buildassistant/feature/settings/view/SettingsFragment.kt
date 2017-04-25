package com.kiwiandroiddev.sc2buildassistant.feature.settings.view

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.google.analytics.tracking.android.GoogleAnalytics
import com.kiwiandroiddev.sc2buildassistant.MyApplication
import com.kiwiandroiddev.sc2buildassistant.R
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsPresenter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsView
import com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsActivity.*
import com.kiwiandroiddev.sc2buildassistant.util.EasyTrackerUtils
import javax.inject.Inject

class SettingsFragment : PreferenceFragment(), SettingsView, SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject lateinit var settingsPresenter: SettingsPresenter

    override fun onCreate(paramBundle: Bundle?) {
        (activity.application as MyApplication).inject(this)

        super.onCreate(paramBundle)

        addPreferencesFromResource(R.xml.preferences)
        initPreferenceClickListeners()

        settingsPresenter.attachView(this)
    }

    private fun initPreferenceClickListeners() {
        findPreference(KEY_CHANGELOG).setOnPreferenceClickListener {
            settingsPresenter.showChangelogSelected()
            true
        }

        findPreference(KEY_RATE_THIS_APP).setOnPreferenceClickListener {
            settingsPresenter.rateAppSelected()
            true
        }

        findPreference(KEY_TRANSLATE).setOnPreferenceClickListener {
            settingsPresenter.translateSelected()
            true
        }

        findPreference(KEY_RESTORE_DATABASE).setOnPreferenceClickListener {
            settingsPresenter.resetDatabaseSelected()
            true
        }
    }

    /*
     * Handles updating preference summaries when their values change
     */
    override fun onSharedPreferenceChanged(sharedPref: SharedPreferences, key: String) {
        val pref = findPreference(key)

        if (key == SettingsActivity.KEY_GAME_SPEED) {
            // Set summary to be the user-description for the selected value
            val speedValues = this.resources.getStringArray(R.array.pref_game_speed_text)
            val index = Integer.parseInt(sharedPref.getString(SettingsActivity.KEY_GAME_SPEED, "4"))
            val speed = speedValues[index]
            pref.summary = speed
        } else if (key == SettingsActivity.KEY_EARLY_WARNING) {
            val seconds = sharedPref.getInt(key, 99)    // default here should never be used!
            val summary = String.format(this.resources.getString(R.string.pref_early_warning_summary), seconds)
            pref.summary = summary
        } else if (key == SettingsActivity.KEY_START_TIME) {
            val seconds = sharedPref.getInt(key, 99)
            val summary = String.format(this.resources.getString(R.string.pref_start_time_summary), seconds)
            pref.summary = summary
        } else if (key == SettingsActivity.KEY_ENABLE_TRACKING) {
            val myInstance = GoogleAnalytics.getInstance(activity)
            myInstance.appOptOut = !sharedPref.getBoolean(SettingsActivity.KEY_ENABLE_TRACKING, true)
        } else if (key == SettingsActivity.KEY_SHOW_ADS) {
            trackNewAdsPreferenceSelection(sharedPref)
        }
    }

    private fun trackNewAdsPreferenceSelection(sharedPref: SharedPreferences) {
        val showAds = sharedPref.getBoolean(SettingsActivity.KEY_SHOW_ADS, true)

        EasyTrackerUtils.sendEvent(activity, "ui_action", "menu_select", "show_ads_option", if (showAds) 1L else 0L)
    }

    /*
     * Registers the settings activity to hear preference changes when it becomes active
     * Straight from Settings doc page
     * @see android.app.Activity#onResume()
     */
    override fun onResume() {
        super.onResume()
        val prefs = preferenceScreen.sharedPreferences
        prefs.registerOnSharedPreferenceChangeListener(this)

        // hack: initialize summaries - manual call to callback
        listOf(SettingsActivity.KEY_GAME_SPEED,
                SettingsActivity.KEY_EARLY_WARNING,
                SettingsActivity.KEY_START_TIME)
                .forEach { key -> onSharedPreferenceChanged(prefs, key) }

        settingsPresenter.attachView(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        settingsPresenter.detachView()
    }

    override fun showResetDatabaseConfirmation() {
        MaterialDialog.Builder(activity)
                .title(R.string.dlg_confirm_db_reset_title)
                .content(R.string.dlg_confirm_db_reset_message)
                .positiveText(android.R.string.yes)
                .onPositive { _, _ -> settingsPresenter.confirmResetDatabaseSelected() }
                .negativeText(android.R.string.no)
                .show()
    }

    override fun showResetDatabaseError(detailedError: String) {
        Toast.makeText(activity,
                String.format(getString(R.string.error_loading_std_builds), detailedError),
                Toast.LENGTH_LONG).show()
    }

    override fun showResetDatabaseSuccess() {
        Toast.makeText(activity, R.string.pref_restore_database_succeeded, Toast.LENGTH_SHORT).show()
    }

}
