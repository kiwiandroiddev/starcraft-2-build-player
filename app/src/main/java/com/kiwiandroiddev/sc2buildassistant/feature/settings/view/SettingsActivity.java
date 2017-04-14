package com.kiwiandroiddev.sc2buildassistant.feature.settings.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;

/**
 * Created by matt on 27/11/14.
 */
public class SettingsActivity extends AppCompatActivity {

    public static final String TRANSLATE_URL = "http://www.getlocalization.com/sc2buildplayer/";

    public static final String KEY_GAME_SPEED = "pref_game_speed";
    public static final String KEY_EARLY_WARNING = "pref_early_warning";
    public static final String KEY_START_TIME = "pref_start_time";
    public static final String KEY_BUILDS_VERSION = "_builds_version";
    public static final String KEY_EXPANSION_SELECTION = "_expansion_selection";
    public static final String KEY_FACTION_SELECTION = "_faction_selection";
    public static final String KEY_ENABLE_TRACKING = "pref_tracking_enabled";
    public static final String KEY_SHOW_STATUS_BAR = "pref_show_status_bar";
    public static final String KEY_SHOW_ADS = "pref_show_ads";
    public static final String KEY_CHANGELOG = "pref_changelog";
    public static final String KEY_RATE_THIS_APP = "pref_rate_this_app";
    public static final String KEY_TRANSLATE = "pref_translate";
    public static final String KEY_RESTORE_DATABASE = "pref_restore_database";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
    	EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
    	EasyTracker.getInstance(this).activityStop(this);
    }

}
