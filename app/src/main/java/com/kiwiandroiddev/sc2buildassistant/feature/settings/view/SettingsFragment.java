package com.kiwiandroiddev.sc2buildassistant.feature.settings.view;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.service.JsonBuildService;
import com.kiwiandroiddev.sc2buildassistant.service.StandardBuildsService;
import com.kiwiandroiddev.sc2buildassistant.util.ChangeLog;
import com.kiwiandroiddev.sc2buildassistant.util.EasyTrackerUtils;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsActivity.KEY_CHANGELOG;
import static com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsActivity.KEY_RATE_THIS_APP;
import static com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsActivity.KEY_RESTORE_DATABASE;
import static com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsActivity.KEY_TRANSLATE;

/**
 * Copyright © 2017. Orion Health. All rights reserved.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);

        addPreferencesFromResource(R.xml.preferences);
        initPreferenceClickListeners();
    }

    private void initPreferenceClickListeners() {
        findPreference(KEY_CHANGELOG).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                showChangelog();
                return true;
            }
        });

        findPreference(KEY_RATE_THIS_APP).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                return launchPlayStoreAppListing();
            }
        });

        findPreference(KEY_TRANSLATE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                launchTranslationPage();
                return true;
            }
        });

        findPreference(KEY_RESTORE_DATABASE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                confirmResetDatabase();
                return true;
            }
        });
    }

    private void showChangelog() {
        ChangeLog cl = new ChangeLog(getActivity());
        cl.showFullLogDialog();
    }

    private void launchTranslationPage() {
        // track this event as it's something we want the user to do (a "goal" in analytics speak)
        EasyTrackerUtils.sendEvent(getActivity(), "ui_action", "menu_select", "translate_option", null);

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(SettingsActivity.TRANSLATE_URL));
        startActivity(browserIntent);
    }

    private boolean launchPlayStoreAppListing() {
        // track this event as it's something we want the user to do (a "goal" in analytics speak)
        EasyTrackerUtils.sendEvent(getActivity(), "ui_action", "menu_select", "rate_option", null);

        return launchPlayStore(Uri.parse("market://details?id=" + getActivity().getPackageName()));
    }

    /*
     * Handles updating preference summaries when their values change
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPref, String key) {
        Preference pref = findPreference(key);

        if (key.equals(SettingsActivity.KEY_GAME_SPEED)) {
            // Set summary to be the user-description for the selected value
            String[] speedValues = this.getResources().getStringArray(R.array.pref_game_speed_text);
            int index = Integer.parseInt(sharedPref.getString(SettingsActivity.KEY_GAME_SPEED, "4"));
            String speed = speedValues[index];
            pref.setSummary(speed);
        } else if (key.equals(SettingsActivity.KEY_EARLY_WARNING)) {
            int seconds = sharedPref.getInt(key, 99);    // default here should never be used!
            String summary = String.format(this.getResources().getString(R.string.pref_early_warning_summary), seconds);
            pref.setSummary(summary);
        } else if (key.equals(SettingsActivity.KEY_START_TIME)) {
            int seconds = sharedPref.getInt(key, 99);
            String summary = String.format(this.getResources().getString(R.string.pref_start_time_summary), seconds);
            pref.setSummary(summary);
        } else if (key.equals(SettingsActivity.KEY_ENABLE_TRACKING)) {
            GoogleAnalytics myInstance = GoogleAnalytics.getInstance(getActivity());
            myInstance.setAppOptOut(!sharedPref.getBoolean(SettingsActivity.KEY_ENABLE_TRACKING, true));
        } else if (key.equals(SettingsActivity.KEY_SHOW_ADS)) {
            trackNewAdsPreferenceSelection(sharedPref);
        }
    }

    private void trackNewAdsPreferenceSelection(SharedPreferences sharedPref) {
        boolean showAds = sharedPref.getBoolean(SettingsActivity.KEY_SHOW_ADS, true);
        EasyTrackerUtils.sendEvent(getActivity(), "ui_action", "menu_select", "show_ads_option", showAds ? 1L : 0L);
    }

    /*
     * Registers the settings activity to hear preference changes when it becomes active
     * Straight from Settings doc page
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(this);

        // hack: initialize summaries - manual call to callback
        String[] keys = {SettingsActivity.KEY_GAME_SPEED, SettingsActivity.KEY_EARLY_WARNING, SettingsActivity.KEY_START_TIME};
        for (String key : keys) {
            this.onSharedPreferenceChanged(prefs, key);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Attempts to launch this app's page on the play store on the user's device
     *
     * @return whether play store launch was successful
     */
    private boolean launchPlayStore(Uri uri) {
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            this.startActivity(goToMarket);
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.dlg_no_market_error, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Handles wiping all build orders from the internal sqlite database and reloading
     * the standard build orders
     */
    private void confirmResetDatabase() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.dlg_confirm_db_reset_title)
                .content(R.string.dlg_confirm_db_reset_message)
                .positiveText(android.R.string.yes)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        doResetDatabase();
                    }
                })
                .negativeText(android.R.string.no)
                .show();
    }

    private void doResetDatabase() {
        // TODO DI candidate if I've ever seen one
        DbAdapter db = ((MyApplication) getActivity().getApplicationContext()).getDb();
        db.clear();
        final boolean forceLoad = true;

        StandardBuildsService.getLoadStandardBuildsIntoDBObservable(getActivity(), forceLoad)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer percent) {
                        Timber.d("percent = " + percent);
                    }

                    @Override
                    public void onCompleted() {
                        JsonBuildService.notifyBuildProviderObservers(getActivity());
                        Toast.makeText(getActivity(), R.string.pref_restore_database_succeeded, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getActivity(),
                                String.format(getString(R.string.error_loading_std_builds),
                                        e.getMessage()),
                                Toast.LENGTH_LONG).show();
                        Timber.e("LoadStandardBuildsTask returned an exception: ", e);

                        // Report this error for analysis
                        EasyTrackerUtils.sendNonFatalException(getActivity(), e);
                    }
                });
    }
}
