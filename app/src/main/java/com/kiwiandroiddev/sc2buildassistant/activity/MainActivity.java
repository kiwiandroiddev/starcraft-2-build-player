package com.kiwiandroiddev.sc2buildassistant.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.ads.Ad;
import com.google.ads.AdView;
import com.google.analytics.tracking.android.EasyTracker;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.dialog.FileDialog;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.RaceFragment;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.ExpansionSpinnerAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.RaceFragmentPagerAdapter;
import com.kiwiandroiddev.sc2buildassistant.service.JsonBuildService;
import com.kiwiandroiddev.sc2buildassistant.service.StandardBuildsService;
import com.kiwiandroiddev.sc2buildassistant.util.ChangeLog;
import com.kiwiandroiddev.sc2buildassistant.util.FragmentUtils;
import com.kiwiandroiddev.sc2buildassistant.util.IOUtils;
import com.kiwiandroiddev.sc2buildassistant.util.OnReceiveAdListener;
import com.kiwiandroiddev.sc2buildassistant.util.SelectionMode;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Main activity and entry point of the app. Shows available build orders for each of the three factions
 * in a view pager. Also performs a number of app initialization functions, such as
 * loading the standard builds from assets into the user's database.
 *
 * @author matt
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

	public static final int REQUEST_OPEN = 2;			// open file request code for importing builds
	private static final String KEY_EXPANSION_CHOICE = "com.kiwiandroiddev.sc2buildassistant.ExpansionChoice";
	private static final String KEY_FACTION_CHOICE = "com.kiwiandroiddev.sc2buildassistant.FactionChoice";

    private RaceFragmentPagerAdapter mPagerAdapter;
	private FragmentManager mManager;
    private int mPreviousFactionChoice;

    @InjectView(R.id.pager) ViewPager mPager;
    @InjectView(R.id.loading_panel) View mLoadingLayout;
    @InjectView(R.id.loading_spinner) ProgressBar mLoadingSpinner;
    @InjectView(R.id.loading_bar) ProgressBar mLoadingBar;
    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.toolbar_expansion_spinner) Spinner mToolbarExpansionSpinner;
    @InjectView(R.id.ad) AdView mAdView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
//    	Debug.startMethodTracing("sc2main");

    	SharedPreferences sharedPrefs = getDefaultSharedPreferences();
    	if (!sharedPrefs.getBoolean(SettingsActivity.KEY_SHOW_STATUS_BAR, false)) {
	        // hides status bar on Android 4.0+, on Android 2.3.x status bar is already hidden from the app theme...
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	}

    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // slide ad banner in from the bottom when it loads (rather than popping)
        if (mAdView != null) {
            mAdView.setAdListener(new OnReceiveAdListener() {
                @Override
                public void onReceiveAd(Ad ad) {
                    mAdView.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_in_from_bottom));
                }
            });
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // HACK: default values from XML don't work for custom preference widgets (i.e. number picker)
        // so we have to manually set default values
        if (!sharedPrefs.contains(SettingsActivity.KEY_EARLY_WARNING)) {
        	Editor ed = sharedPrefs.edit();
        	ed.putInt(SettingsActivity.KEY_EARLY_WARNING, 5);	// TODO HARD-CODED DEFAULT VALUE - SHOULD BE IN XML!
        	ed.commit();
        }
        if (!sharedPrefs.contains(SettingsActivity.KEY_START_TIME)) {
        	Editor ed = sharedPrefs.edit();
        	ed.putInt(SettingsActivity.KEY_START_TIME, 15);	// TODO HARD-CODED DEFAULT VALUE - SHOULD BE IN XML!
        	ed.commit();
        }

        loadStandardBuilds();

        initRaceFragmentPagerAndExpansionSpinner(savedInstanceState);

        // Show Changelog if appropriate
        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun()) {
            cl.getLogDialog().show();
        }

        //Debug.stopMethodTracing();	// sc2main
    }

    private SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
     * Check if builds directory has been modified since we last checked
     * if so, load all builds from JSON files into DB, showing progress feedback in the UI
     */
    private void loadStandardBuilds() {
        showLoadingAnim();
        StandardBuildsService.getLoadStandardBuildsIntoDBObservable(this, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer percent) {
                        setLoadProgress(percent);
                    }

                    @Override
                    public void onCompleted() {
                        hideLoadingAnim();
                        JsonBuildService.notifyBuildProviderObservers(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this,
                                String.format(MainActivity.this.getString(R.string.error_loading_std_builds),
                                        e.getMessage()),
                                Toast.LENGTH_LONG).show();
                        Timber.e("LoadStandardBuildsTask returned an exception: ", e);
                    }
                });
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

    /**
     * Starcraft expansion selection changed - need to pass this on to
     * race fragments so they can re-filter their list view.
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final DbAdapter.Expansion expansion = DbAdapter.Expansion.values()[position];

        // pass on current expansion to any racefragments created in future
        mPagerAdapter.setCurrentExpansion(expansion);
        saveExpansionSelection(expansion.ordinal());

        // TODO hacky but have not yet found a better way to find all child fragments
        for (int i = 0; i < 3; ++i) {
            String tag = FragmentUtils.makeFragmentName(mPager.getId(), i);
            Fragment f = mManager.findFragmentByTag(tag);
            if (f != null) {
                RaceFragment rf = (RaceFragment) f;
                rf.setExpansionFilter(expansion);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }

    /** shows the indeterminate progress spinner, hides progress bar */
    @DebugLog
	public void showLoadingAnim() {
		mLoadingLayout.setVisibility(View.VISIBLE);
		mLoadingSpinner.setVisibility(View.VISIBLE);
		mLoadingBar.setVisibility(View.GONE);
	}

    /**
     * This is called intermittently during loading of standard builds into database.
     * Hides indeterminate progress spinner and shows the progress bar with
     * the given percentage full.
     *
     * @param percent progress of load as a percentage
     */
    @DebugLog
    public void setLoadProgress(Integer percent) {
		if (mLoadingSpinner.getVisibility() == View.VISIBLE)
			mLoadingSpinner.setVisibility(View.GONE);
		if (mLoadingBar.getVisibility() == View.GONE)
			mLoadingBar.setVisibility(View.VISIBLE);

		mLoadingBar.setProgress(percent);
	}

	/** hides progress spinner and bar */
    @DebugLog
	public void hideLoadingAnim() {
		mLoadingLayout.setVisibility(View.GONE);
	}

    private void initRaceFragmentPagerAndExpansionSpinner(Bundle savedInstanceState) {
        /** Getting fragment manager */
        mManager = getSupportFragmentManager();

        /** Instantiating FragmentPagerAdapter */
        mPagerAdapter = new RaceFragmentPagerAdapter(mManager, this);

        final int previousExpansionChoice = savedInstanceState != null ?
                savedInstanceState.getInt(KEY_EXPANSION_CHOICE) :
                getSavedExpansionSelection();
        mPagerAdapter.setCurrentExpansion(DbAdapter.Expansion.values()[previousExpansionChoice]);

        /** Setting the pagerAdapter to the pager object */
        mPager.setAdapter(mPagerAdapter);

        mPreviousFactionChoice = savedInstanceState != null ?
                savedInstanceState.getInt(KEY_FACTION_CHOICE) :
                getSavedFactionSelection();

        /** Bind tabs view to pager */
        TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
        tabs.setupWithViewPager(mPager);

        // Set up expansion drop-down list on action bar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mToolbarExpansionSpinner.setAdapter(new ExpansionSpinnerAdapter(getSupportActionBar().getThemedContext()));
        mToolbarExpansionSpinner.setOnItemSelectedListener(this);
        mToolbarExpansionSpinner.setSelection(previousExpansionChoice);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Workaround for a bug with TabLayout. If this is called in onCreate(), the first tab's
        // title remains highlighted while another tab might be selected
        mPager.setCurrentItem(mPreviousFactionChoice);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.build_list_menu, menu);		// add the "new build" action bar item
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle Build List screen specific menu items
        switch (item.getItemId()) {
            case R.id.menu_new_build:
                // starts the build editor
                onNewBuildMenuClicked();
                return true;
            case R.id.menu_import_build:
                onImportBuildMenuClicked();
                return true;
        }

        if (!MainActivity.OnMenuItemSelected(this, item)) {
            return super.onOptionsItemSelected(item);
        } else {
            return true;
        }
    }

    private void onNewBuildMenuClicked() {
        Intent i = new Intent(this, EditBuildActivity.class);
        i.putExtra(IntentKeys.KEY_EXPANSION_ENUM, mPagerAdapter.getCurrentExpansion());
        i.putExtra(IntentKeys.KEY_FACTION_ENUM, DbAdapter.Faction.values()[mPager.getCurrentItem()]);
        startActivity(i);
    }

    private void onImportBuildMenuClicked() {
        Intent intent = new Intent(getBaseContext(), FileDialog.class);
        File root = Environment.getExternalStorageDirectory();
        File buildsDir = new File(root, JsonBuildService.BUILDS_DIR);
        intent.putExtra(FileDialog.START_PATH, buildsDir.getAbsolutePath());	// stub
        intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
        intent.putExtra(FileDialog.FORMAT_FILTER, new String[] { "json" });
        intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
        startActivityForResult(intent, REQUEST_OPEN);
    }

    // helper than can be used by all activities that show the same menu
    public static boolean OnMenuItemSelected(Context ctx, MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_settings:
                Intent i = new Intent(ctx, SettingsActivity.class);
                ctx.startActivity(i);
                return true;
            default:
                return false;
        }
    }

    /*
     * Gets the result when an open-file dialog completes
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    switch (requestCode) {
		// import build file dialog returned
	    case REQUEST_OPEN:
	    	if (resultCode == RESULT_OK) {
                final String filename = data.getStringExtra(FileDialog.RESULT_PATH);
                JsonBuildService.importBuildsFromJsonFileToDatabase(getApplicationContext(), filename);
	    	}
	    	break;
	    }
	}

    @Override
    protected void onPause() {
        super.onPause();
        mPreviousFactionChoice = mPager.getCurrentItem();
    }

	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_EXPANSION_CHOICE, mToolbarExpansionSpinner.getSelectedItemPosition());
        outState.putInt(KEY_FACTION_CHOICE, mPager.getCurrentItem());
        saveFactionSelection(mPager.getCurrentItem());
    }

    private void saveExpansionSelection(int index) {
		IOUtils.writeIntToSharedPrefs(this, SettingsActivity.KEY_EXPANSION_SELECTION, index);
    }

    private void saveFactionSelection(int index) {
		IOUtils.writeIntToSharedPrefs(this, SettingsActivity.KEY_FACTION_SELECTION, index);
    }

    /** Now defaults to Heart of the Swarm */
    private int getSavedExpansionSelection() {
        return getDefaultSharedPreferences()
                .getInt(SettingsActivity.KEY_EXPANSION_SELECTION, DbAdapter.Expansion.HOTS.ordinal());
    }

    private int getSavedFactionSelection() {
        return getDefaultSharedPreferences()
                .getInt(SettingsActivity.KEY_FACTION_SELECTION, DbAdapter.Faction.TERRAN.ordinal());
    }

}
