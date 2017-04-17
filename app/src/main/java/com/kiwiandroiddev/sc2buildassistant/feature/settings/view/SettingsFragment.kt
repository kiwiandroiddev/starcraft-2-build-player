package com.kiwiandroiddev.sc2buildassistant.feature.settings.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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
import com.kiwiandroiddev.sc2buildassistant.service.JsonBuildService
import com.kiwiandroiddev.sc2buildassistant.service.StandardBuildsService
import com.kiwiandroiddev.sc2buildassistant.util.EasyTrackerUtils
import rx.Observer
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * Copyright © 2017. Orion Health. All rights reserved.
 */
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

//    private fun showChangelog() {
//        val cl = ChangeLog(activity)
//        cl.showFullLogDialog()
//    }
//
//    private fun launchTranslationPage() {
//        // track this event as it's something we want the user to do (a "goal" in analytics speak)
//        EasyTrackerUtils.sendEvent(activity, "ui_action", "menu_select", "translate_option", null)
//
//        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(SettingsPresenter.TRANSLATE_URL))
//        startActivity(browserIntent)
//    }
//
//    private fun launchPlayStoreAppListing(): Boolean {
//        // track this event as it's something we want the user to do (a "goal" in analytics speak)
//        EasyTrackerUtils.sendEvent(activity, "ui_action", "menu_select", "rate_option", null)
//
//        return launchPlayStore(Uri.parse("market://details?id=" + activity.packageName))
//    }

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
        val keys = arrayOf(SettingsActivity.KEY_GAME_SPEED, SettingsActivity.KEY_EARLY_WARNING, SettingsActivity.KEY_START_TIME)
        for (key in keys) {
            this.onSharedPreferenceChanged(prefs, key)
        }

        settingsPresenter.attachView(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)

        settingsPresenter.detachView()
    }

    /**
     * Attempts to launch this app's page on the play store on the user's device

     * @return whether play store launch was successful
     */
    private fun launchPlayStore(uri: Uri): Boolean {
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            this.startActivity(goToMarket)
            return true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.dlg_no_market_error, Toast.LENGTH_SHORT).show()
            return false
        }

    }

    /**
     * Handles wiping all build orders from the internal sqlite database and reloading
     * the standard build orders
     */
    private fun confirmResetDatabase() {
        MaterialDialog.Builder(activity)
                .title(R.string.dlg_confirm_db_reset_title)
                .content(R.string.dlg_confirm_db_reset_message)
                .positiveText(android.R.string.yes)
                .onPositive { _, _ -> doResetDatabase() }
                .negativeText(android.R.string.no)
                .show()
    }

    private fun doResetDatabase() {
        // TODO DI candidate if I've ever seen one
        val db = (activity.applicationContext as MyApplication).db
        db!!.clear()
        val forceLoad = true

        StandardBuildsService.getLoadStandardBuildsIntoDBObservable(activity, forceLoad)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Int> {
                    override fun onNext(percent: Int?) {
                        Timber.d("percent = " + percent!!)
                    }

                    override fun onCompleted() {
                        JsonBuildService.notifyBuildProviderObservers(activity)
                        Toast.makeText(activity, R.string.pref_restore_database_succeeded, Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(activity,
                                String.format(getString(R.string.error_loading_std_builds),
                                        e.message),
                                Toast.LENGTH_LONG).show()
                        Timber.e("LoadStandardBuildsTask returned an exception: ", e)

                        // Report this error for analysis
                        EasyTrackerUtils.sendNonFatalException(activity, e)
                    }
                })
    }

    override fun showResetDatabaseConfirmation() {
        confirmResetDatabase()     // TODO temporary
    }

}
