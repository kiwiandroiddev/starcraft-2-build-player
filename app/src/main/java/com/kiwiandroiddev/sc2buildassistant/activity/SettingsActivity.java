package com.kiwiandroiddev.sc2buildassistant.activity;

//import com.google.analytics.tracking.android.EasyTracker;
//import com.google.analytics.tracking.android.GoogleAnalytics;
//import com.google.analytics.tracking.android.Tracker;
//import com.google.analytics.tracking.android.GoogleAnalytics.AppOptOutCallback;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
        import android.view.MenuItem;
        import android.view.WindowManager;
import android.widget.Toast;

import com.kiwiandroiddev.sc2buildassistant.ChangeLog;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;

/**
 * Displays a UI for adjusting a number of playback settings and functions
 * that the user shouldn't need too often, such as resetting the database.
 * Also contains options letting the user Rate the app and buy the pro
 * version.
 * 
 * @author matt
 *
 */
public class SettingsActivity extends PreferenceActivity
							implements OnSharedPreferenceChangeListener
{
	public static final String PRO_VERSION_PACKAGE = "com.kiwiandroiddev.sc2buildassistantpro";
	public static final String TRANSLATE_URL = "http://www.getlocalization.com/sc2buildplayer/";
	
	public static final String KEY_GAME_SPEED = "pref_game_speed";
	public static final String KEY_EARLY_WARNING = "pref_early_warning";
	public static final String KEY_START_TIME = "pref_start_time";
	public static final String KEY_BUILDS_VERSION = "_builds_version";
	public static final String KEY_EXPANSION_SELECTION = "_expansion_selection";
	public static final String KEY_FACTION_SELECTION = "_faction_selection";
	public static final String KEY_ENABLE_TRACKING = "pref_tracking_enabled";
	public static final String KEY_SHOW_STATUS_BAR = "pref_show_status_bar";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	if (!sharedPref.getBoolean(SettingsActivity.KEY_SHOW_STATUS_BAR, false)) {
	 		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
	 				WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	}
 		
    	// set dark theme
    	// NOTE: must be called BEFORE super.onCreate() for the theme to apply correctly!
 		// TODO: This only seems to apply to this activity, investigate reason.
//    	setTheme(R.style.Theme_Sherlock);	
        super.onCreate(savedInstanceState);    
        getActionBar().setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.preferences);
        
        /** Show Changelog dialog when user taps on changelog preference */
        Preference changelogPref = (Preference)findPreference("pref_changelog");
        changelogPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                     public boolean onPreferenceClick(Preference preference) {
                    	 ChangeLog cl = new ChangeLog(SettingsActivity.this);
                    	 cl.getFullLogDialog().show();
                    	 return true;
                     }
                 });
        
        /** Launch this app's listing in Play Store when user taps "Rate this App" */
        Preference ratePref = (Preference)findPreference("pref_rate_this_app");
        ratePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	// track this event as it's something we want the user to do (a "goal" in analytics speak)
//            	EasyTracker.getInstance().setContext(SettingsActivity.this);
//            	EasyTracker.getTracker().sendEvent("ui_action", "menu_select", "rate_option", null);
            	
            	return launchPlayStore(Uri.parse("market://details?id=" + SettingsActivity.this.getPackageName()));
            }
        });
        
        /** Launch Pro version's listing in Play Store when user taps "Buy Pro version" */
        Preference proPref = (Preference)findPreference("pref_upgrade_to_pro");
        proPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	// track this event as it's something we want the user to do (a "goal" in analytics speak)
//            	EasyTracker.getInstance().setContext(SettingsActivity.this);
//            	EasyTracker.getTracker().sendEvent("ui_action", "menu_select", "buy_pro_option", null);
            	
            	return launchPlayStore(Uri.parse("market://details?id=" + SettingsActivity.PRO_VERSION_PACKAGE));
            }
        });
        
        /** Launch getlocalization page in a browser window */
        Preference translatePref = (Preference)findPreference("pref_translate");
        translatePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	// track this event as it's something we want the user to do (a "goal" in analytics speak)
//            	EasyTracker.getInstance().setContext(SettingsActivity.this);
//            	EasyTracker.getTracker().sendEvent("ui_action", "menu_select", "translate_option", null);
            	
            	Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(TRANSLATE_URL));
            	startActivity(browserIntent);
            	return true;
            }
        });
        
        /** Logic for resetting the database when the user taps that menu item */
        Preference resetPref = (Preference)findPreference("pref_restore_database");
        resetPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	doResetDatabase();
            	return true;
            }
        });
    }
    
    @Override
    public void onStart() {
    	super.onStart();
//    	EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
    	super.onStop();
//    	EasyTracker.getInstance().activityStop(this);
    }
    
    /*
     * Handles updating preference summaries when their values change
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPref, String key) {
//    	Log.w(this.toString(), "SettingsActivity.onSharedPreferenceChanged() called, key = " + key);
//    	Log.w(this.toString(), "key.equals(KEY_EARLY_WARNING) = "+key.equals(KEY_EARLY_WARNING));
        Resources res = this.getResources();
    	Preference pref = findPreference(key);
    	
        if (key.equals(KEY_GAME_SPEED)) {      
            // Set summary to be the user-description for the selected value
            String[] speedValues = res.getStringArray(R.array.pref_game_speed_text);
            int index = Integer.parseInt(sharedPref.getString(SettingsActivity.KEY_GAME_SPEED, "4"));
            String speed = speedValues[index];
            pref.setSummary(speed);
        } else if (key.equals(KEY_EARLY_WARNING)) {
        	int seconds = sharedPref.getInt(key, 99);	// default here should never be used!
//        	Log.w(this.toString(), "early warning seconds value = " + seconds);
        	String summary = String.format(res.getString(R.string.pref_early_warning_summary), seconds);
        	pref.setSummary(summary);
        } else if (key.equals(KEY_START_TIME)) {
        	int seconds = sharedPref.getInt(key, 99);
        	String summary = String.format(res.getString(R.string.pref_start_time_summary), seconds);
        	pref.setSummary(summary);
        } else if (key.equals(KEY_ENABLE_TRACKING)) {
//        	GoogleAnalytics myInstance = GoogleAnalytics.getInstance(this);
//        	myInstance.setAppOptOut(!sharedPref.getBoolean(KEY_ENABLE_TRACKING, true));
        }
        // other preferences here...
    }
    
    /*
     * Registers the settings activity to hear preference changes when it becomes active
     * Straight from Settings doc page
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
//        Log.w(this.toString(), "SettingsActivity.onResume() called");
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(this);
        
        // hack: initialize summaries - manual call to callback
        String[] keys = { KEY_GAME_SPEED, KEY_EARLY_WARNING, KEY_START_TIME };
        for (String key : keys) {
        	this.onSharedPreferenceChanged(prefs, key);
        }
    }

    @Override
    protected void onPause() {
//    	Log.w(this.toString(), "SettingsActivity.onPause() called");
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
    
    /* handle "Up" button press on title bar, navigate back to build list */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed
                // in the Action Bar.
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /** 
     * Attempts to launch this app's page on the play store on the user's device
     * 
     * @return whether play store launch was successful
     */
    private boolean launchPlayStore(Uri uri) {
//		Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
		Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
		try {
		    this.startActivity(goToMarket);
		    return true;
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.dlg_no_market_error, Toast.LENGTH_SHORT).show();
			return false;
		}
    }
    
    /**
     * Handles wiping all build orders from the internal sqlite database and reloading
     * the standard build orders
     */
    private void doResetDatabase() {
    	// confirm with user as this is operation deletes user data
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.dlg_confirm_db_reset_title)
    		.setMessage(R.string.dlg_confirm_db_reset_message)
    		.setPositiveButton(android.R.string.yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO: do this in background thread as it's very slow
			    	DbAdapter db = ((MyApplication)getApplicationContext()).getDb();
			    	db.clear();
			    	final boolean forceLoad = true;
			    	try {
						BuildListActivity.loadStandardBuildsIntoDB(SettingsActivity.this, forceLoad);
						BuildListActivity.notifyBuildProviderObservers(SettingsActivity.this);
				    	Toast.makeText(SettingsActivity.this, R.string.pref_restore_database_succeeded, Toast.LENGTH_SHORT).show();
					} catch (Exception e) {
						Toast.makeText(SettingsActivity.this, String.format(getString(R.string.error_loading_std_builds), e.getMessage()),
			    				Toast.LENGTH_LONG).show();
						e.printStackTrace();
						
						// Report this error for analysis
//			    		EasyTracker.getInstance().setContext(SettingsActivity.this);
//			    		Tracker myTracker = EasyTracker.getTracker();       // Get a reference to tracker.
//			    		myTracker.sendException(e.getMessage(), false);    // false indicates non-fatal exception.
					}
				}
			})
			.setNegativeButton(android.R.string.no, null)
			.show();
    }
}