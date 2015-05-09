package com.kiwiandroiddev.sc2buildassistant.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

import com.astuetz.PagerSlidingTabStrip;
import com.google.ads.Ad;
import com.google.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kiwiandroiddev.sc2buildassistant.BuildOrderProvider;
import com.kiwiandroiddev.sc2buildassistant.ChangeLog;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.SelectionMode;
import com.kiwiandroiddev.sc2buildassistant.activity.dialog.FileDialog;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.RaceFragment;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.NameNotUniqueException;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.ProgressListener;
import com.kiwiandroiddev.sc2buildassistant.adapter.ExpansionSpinnerAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.RaceFragmentPagerAdapter;
import com.kiwiandroiddev.sc2buildassistant.model.Build;
import com.kiwiandroiddev.sc2buildassistant.util.OnReceiveAdListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

//import com.google.analytics.tracking.android.EasyTracker;
//import com.google.analytics.tracking.android.Tracker;

/**
 * Main activity and entry point of the app. Shows available build orders for each of the three factions
 * in a view pager. Also contains a number of app initialization functions, such as
 * loading the standard builds from assets into the user's database.
 * 
 * @author matt
 *
 */
public class MainActivity extends ActionBarActivity implements AdapterView.OnItemSelectedListener {

	public static final int REQUEST_OPEN = 2;			// open file request code for importing builds
	public static final int BUILD_FILES_VERSION = 46;	// tracks changes to build JSON files in assets/

	// keys that this activity uses when passing data to other activities or fragments
	public static final String KEY_EXPANSION_CHOICE = "com.kiwiandroiddev.sc2buildassistant.ExpansionChoice";
	public static final String KEY_FACTION_CHOICE = "com.kiwiandroiddev.sc2buildassistant.FactionChoice";
	
	public static final String BUILDS_DIR = "sc2_builds";
	public static final String ASSETS_BUILDS_DIR = "builds";
	public static final String TAG = "BuildListActivity";
	public static final boolean DEBUG = false;
	
	private RaceFragmentPagerAdapter mPagerAdapter;
	private FragmentManager mManager;
    
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
    	
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	if (!sharedPref.getBoolean(SettingsActivity.KEY_SHOW_STATUS_BAR, false)) {
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
        if (!sharedPref.contains(SettingsActivity.KEY_EARLY_WARNING)) {
        	Editor ed = sharedPref.edit();
        	ed.putInt(SettingsActivity.KEY_EARLY_WARNING, 5);	// TODO HARD-CODED DEFAULT VALUE - SHOULD BE IN XML!
        	ed.commit();
        }
        if (!sharedPref.contains(SettingsActivity.KEY_START_TIME)) {
        	Editor ed = sharedPref.edit();
        	ed.putInt(SettingsActivity.KEY_START_TIME, 15);	// TODO HARD-CODED DEFAULT VALUE - SHOULD BE IN XML!
        	ed.commit();
        }
                
        // check if builds directory has been modified since we last checked
        // if so, load all builds from JSON files into DB
        LoadStandardBuildsTask task = new LoadStandardBuildsTask(this);
        task.execute();
        
        initRaceFragmentPagerAndExpansionSpinner(savedInstanceState);

        // Show Changelog if appropriate
        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun()) {
			cl.getLogDialog().show();
		}

        //Debug.stopMethodTracing();	// sc2main
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.build_list_menu, menu);		// add the "new build" action bar item
       inflater.inflate(R.menu.options_menu, menu);
       return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// Handle Build List screen specific menu items
    	switch (item.getItemId()) {
    	case R.id.menu_new_build:
    		// starts the build editor
    		Intent i = new Intent(this, EditBuildActivity.class);
    		// TODO: move KEY definitions into one global 'contract' class
            i.putExtra(RaceFragment.KEY_EXPANSION_ENUM, mPagerAdapter.getCurrentExpansion());
            i.putExtra(RaceFragment.KEY_FACTION_ENUM, DbAdapter.Faction.values()[mPager.getCurrentItem()]);
            startActivity(i);
            return true;
    	case R.id.menu_import_build:
    		// TODO extract method refactor
    		Intent intent = new Intent(getBaseContext(), FileDialog.class);
    		File root = Environment.getExternalStorageDirectory();
        	File buildsDir = new File(root, MainActivity.BUILDS_DIR);
            intent.putExtra(FileDialog.START_PATH, buildsDir.getAbsolutePath());	// stub
            intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
            intent.putExtra(FileDialog.FORMAT_FILTER, new String[] { "json" });
            intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
            startActivityForResult(intent, REQUEST_OPEN);
    		return true;
    	}
    	
    	if (!MainActivity.OnMenuItemSelected(this, item)) {
			return super.onOptionsItemSelected(item);
		} else {
			return true;
		}
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
        for (int i=0; i<3; ++i) {
            String tag = makeFragmentName(mPager.getId(), i);
            Fragment f = mManager.findFragmentByTag(tag);
            if (f != null) {
                RaceFragment rf = (RaceFragment)f;
                rf.setExpansionFilter(expansion);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }

	/**
	 * Loads standard build orders into the local database.
	 * 
	 * If forceLoad is false, standard builds will only be copied if the
	 * user's copy of them is out of date. This will be the case if new builds
	 * were added in an app update.
	 * 
	 * If forceLoad is true, standard builds will always be copied.
	 * 
	 * In both cases, standard builds loaded will overwrite any changes the
	 * user has made to them.
	 */
	public static void loadStandardBuildsIntoDB(Context c, boolean forceLoad,
			ProgressListener listener) throws IOException {
        DbAdapter db = ((MyApplication) c.getApplicationContext()).getDb();
        db.open();
        
        final int oldVersion = getStoredBuildsVersion(c);
        final int newVersion = BUILD_FILES_VERSION;
        final boolean outOfDate = oldVersion < newVersion;

        if (forceLoad || outOfDate) {
        	// handle updating standard builds version
        	doUpdateBuilds(c, db, oldVersion, newVersion);
        	
        	ArrayList<Build> stdBuilds = fetchStandardBuilds(c);
        	//Log.d(TAG, "in loadStandardBuildsIntoDB(), stdBuilds = " + stdBuilds);
        	db.addOrReplaceBuilds(stdBuilds, listener);
        	
        	if (outOfDate) {
				updateBuildsVersion(c);
			}
        }
	}
    
	/**
	 * If any operations other than just copying and/or overwriting builds are needed
	 * when updating the user's standard builds, they are performed here. 
	 * 
	 * @param db
	 * @param oldVersion user's current standard builds version
	 * @param newVersion latest standard builds version
	 */
	private static void doUpdateBuilds(Context c, DbAdapter db, int oldVersion, int newVersion) {
		// 38: renamed std builds removing "(vs. Race)" as this is now generated
		// programmatically
		if (oldVersion < 42) {
			List<String> buildsToDelete = getStringListFromAsset(c, "39_old_builds.json");
			if (buildsToDelete != null) {
				for (String name : buildsToDelete) {
					db.deleteBuild(name);
				}
			}
			//db.deleteBuild("YoDa's Reaper FE into Widow Mine Drop (vs. Terran)");	// stub
		}
	}
	
	public static void loadStandardBuildsIntoDB(Context c, boolean forceLoad) throws IOException {
		loadStandardBuildsIntoDB(c, forceLoad, null);
	}
	
	/** shows the indeterminate progress spinner, hides progress bar */
	public void showLoadingAnim() {
		mLoadingLayout.setVisibility(View.VISIBLE);
		mLoadingSpinner.setVisibility(View.VISIBLE);
		mLoadingBar.setVisibility(View.GONE);
	}
	
    /**
     * This is called intermittently during loading of std builds into database.
     * Hides indeterminate progress spinner and shows the progress bar with
     * the given percentage full.
     * 
     * @param percent progress of load as a percentage
     */
    public void setLoadProgress(Integer percent) {	
		if (mLoadingSpinner.getVisibility() == View.VISIBLE)
			mLoadingSpinner.setVisibility(View.GONE);
		if (mLoadingBar.getVisibility() == View.GONE)
			mLoadingBar.setVisibility(View.VISIBLE);
		
		mLoadingBar.setProgress(percent);
	}
	
	/** hides progress spinner and bar */
	public void hideLoadingAnim() {
		mLoadingLayout.setVisibility(View.GONE);
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
                importBuilds(getApplicationContext(), filename);
	    	}
	    	break;
	    }
	}
    
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_EXPANSION_CHOICE, mToolbarExpansionSpinner.getSelectedItemPosition());
        outState.putInt(KEY_FACTION_CHOICE, mPager.getCurrentItem());
        saveFactionSelection(mPager.getCurrentItem());
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

        final int previousFactionChoice = savedInstanceState != null ?
        		savedInstanceState.getInt(KEY_FACTION_CHOICE) :
        		getSavedFactionSelection();
        mPager.setCurrentItem(previousFactionChoice);

        /** Bind sliding tabs view to pager */
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setViewPager(mPager);

        // Set up expansion drop-down list on action bar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mToolbarExpansionSpinner.setAdapter(new ExpansionSpinnerAdapter(getSupportActionBar().getThemedContext()));
        mToolbarExpansionSpinner.setOnItemSelectedListener(this);
        mToolbarExpansionSpinner.setSelection(previousExpansionChoice);
	}

	/**
	 * Returns the standard builds included with the app in a list.
	 * The standard builds are compiled from JSON files in the
	 * assets/builds directory
	 * 
	 * @param c application context. Needed so this function can read from the
	 * application's assets directory
	 * @return arraylist of Build objects
	 */
	private static ArrayList<Build> fetchStandardBuilds(Context c) throws IOException {
    	// compile build objects into a list and return them
    	ArrayList<Build> all_builds = new ArrayList<Build>();

    	for (String filename : getAssetsWithExtension(c, ".json", ASSETS_BUILDS_DIR)) {
    		ArrayList<Build> builds = readBuilds(c.getAssets().open(ASSETS_BUILDS_DIR + "/" + filename)); 	// stub
    		if (builds == null) {
    			Log.e(TAG, "JSON syntax error with file " + filename);
    		} else {
    			all_builds.addAll(builds);
    		}
    	}
    	
    	return all_builds;
	}
    
    /**
     * @return true if user's saved builds version is different to the current builds version constant,
     * meaning one or more of the standard build files has been updated
     */
//    private static boolean buildsOutOfDate(Context c) {
//    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
//    	return (prefs.getInt(SettingsActivity.KEY_BUILDS_VERSION, -1) != BuildListActivity.BUILD_FILES_VERSION);
//    }
    
    /**
     * Gets the version number of the standard builds currently stored in the user's database
     * (Might be out of date).
     * Returns -1 if no standard builds have been copied yet (probably a new install).
     */
    private static int getStoredBuildsVersion(Context c) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	return prefs.getInt(SettingsActivity.KEY_BUILDS_VERSION, -1);
    }
        
    /**
     * Sets the user's saved builds version to the current builds version constant.
     * This should be done after the standard builds on their SD card have been updated.
     */
    private static void updateBuildsVersion(Context c) {
		writeIntToSharedPrefs(c, SettingsActivity.KEY_BUILDS_VERSION, MainActivity.BUILD_FILES_VERSION);
    }
    
    private void saveExpansionSelection(int index) {
		writeIntToSharedPrefs(this, SettingsActivity.KEY_EXPANSION_SELECTION, index);
    }
    
    private void saveFactionSelection(int index) {
		writeIntToSharedPrefs(this, SettingsActivity.KEY_FACTION_SELECTION, index);
    }

	private static void writeIntToSharedPrefs(Context c, String key, int value) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		prefs.edit().putInt(key, value).apply();
	}

    /** Now defaults to Heart of the Swarm */
    private int getSavedExpansionSelection() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	return prefs.getInt(SettingsActivity.KEY_EXPANSION_SELECTION, DbAdapter.Expansion.HOTS.ordinal());
    }
    
    private int getSavedFactionSelection() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	return prefs.getInt(SettingsActivity.KEY_FACTION_SELECTION, DbAdapter.Faction.TERRAN.ordinal());	
    }

    private class LoadStandardBuildsTask extends AsyncTask<Boolean, Integer, Exception> {
        private Context mContext;

        public LoadStandardBuildsTask(Context c) {
            super();
            mContext = c;
        }

        protected void onPreExecute() {
            ((MainActivity)mContext).showLoadingAnim();
        }

        protected Exception doInBackground(Boolean... notUsed) {
            try {
                MainActivity.loadStandardBuildsIntoDB(mContext, false,        // only update if necessary
                        new ProgressListener() {
                            @Override
                            public void onProgressUpdate(int percent) {
                                publishProgress(percent);
                            }
                        });
            } catch (Exception e) {
                return e;
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            ((MainActivity)mContext).setLoadProgress(progress[0]);
        }

        protected void onPostExecute(Exception e) {
            ((MainActivity)mContext).hideLoadingAnim();

            if (e == null) {
                // notify observers of buildprovider's build table that its contents have changed
                MainActivity.notifyBuildProviderObservers(mContext);
            } else {
                Toast.makeText(mContext, String.format(mContext.getString(R.string.error_loading_std_builds), e.getMessage()),
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "LoadStandardBuildsTask returned an exception: ", e);

                // Report this error for analysis
//	    		EasyTracker.getInstance().setContext(mContext);
//	    		Tracker myTracker = EasyTracker.getTracker();       // Get a reference to tracker.
//	    		myTracker.sendException(e.getMessage(), false);    // false indicates non-fatal exception.
            }
        }
    }

    // ========================================================================
    // Static Helper Functions
    // ========================================================================

    /**
     * Parses a JSON input stream, returning a list of Build objects.
     */
    public static ArrayList<Build> readBuilds(InputStream input) throws IOException, JsonSyntaxException {
    	Gson gson = new GsonBuilder()
	        .setDateFormat(DbAdapter.DATE_FORMAT.toPattern())	// use ISO-8601 date format
	        .create();
        String bufferString = "";
		int size = input.available();
        byte[] buffer = new byte[size];
        input.read(buffer);
        input.close();
        bufferString = new String(buffer);
	    
		Build[] builds = gson.fromJson(bufferString, Build[].class);
		ArrayList<Build> result = new ArrayList<Build>(Arrays.asList(builds));
		return result;
    }
    
    /**
     * Parses a JSON file, returning a list of Build objects.
     */
    public static ArrayList<Build> readBuilds(File json_file) throws IOException, JsonSyntaxException {
    	return readBuilds(new FileInputStream(json_file));
    }
    
    /**
     * Helper to write a build object as a JSON file to the builds directory on user's SD card
     * 
     * @param filename output filename without parent directory (e.g. "6pool.json")
     * @param build Build object to serialize
     */
    public static void writeBuild(String filename, Build build) throws IOException, FileNotFoundException {
    	// use GSON to serialize it to a JSON string
    	ArrayList<Build> list = new ArrayList<Build>();
    	list.add(build);
    	
    	Gson gson = new GsonBuilder()
	        .setDateFormat(DbAdapter.DATE_FORMAT.toPattern())	// use ISO-8601 date format
	        .create();
    	
		final String json = gson.toJson(list);
		
		// write JSON file to file system
		File root = Environment.getExternalStorageDirectory();
    	File file = new File(root, MainActivity.BUILDS_DIR + "/" + filename);

		file.createNewFile();
		OutputStream out = new FileOutputStream(file);
		out.write(json.getBytes());
		out.flush();
        out.close();
    }
    
    /**
     * Helper for reading in Build object(s) from a JSON file, then attempting to
     * load it/them into the database
     * 
     * @param filename
     */
    public static void importBuilds(Context c, String filename) {
    	DbAdapter db = ((MyApplication) c.getApplicationContext()).getDb();
    	ArrayList<Build> newBuilds;
    	File file = new File(filename);
    	
    	try {
    		newBuilds = readBuilds(file);
    	} catch (JsonSyntaxException e) {
			Log.e(TAG, "JSON syntax error with file " + file);
            Toast.makeText(c, "Could't load " + file.toString() + ", invalid JSON syntax", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
		} catch (IOException e) {
			// TODO make this more informative
			Log.e(TAG, "IO error with file " + file);
            Toast.makeText(c, "Could't load " + file.toString() + ", input/ouput error", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
		}
    	
//    	Log.d(TAG, "newBuilds = " + newBuilds);
    	for (Build build : newBuilds) {
    		try { db.addBuild(build); }
    		catch (NameNotUniqueException e) {
    			// TODO: move to strings.xml
    			Toast.makeText(c, "Could't import \"" + build.getName() + "\" as there is another build with that name. Please delete the old one first.", Toast.LENGTH_LONG).show();
    		}
    	}
    	MainActivity.notifyBuildProviderObservers(c);
    }
    
    public static ArrayList<String> getAssetsWithExtension(Context c, String endsWith, String dir) throws IOException {
    	String[] all_assets = c.getAssets().list(dir);
    	ArrayList<String> results = new ArrayList<String>();
    	for (String filename : all_assets) {
    		if (filename.endsWith(endsWith))
    			results.add(filename);
    	}
    	return results;
    }
    
    // credit: http://stackoverflow.com/questions/9530921/list-all-the-files-from-all-the-folder-in-a-single-list
    // recursive file search
    public static ArrayList<File> getFilesWithExtension(String endsWith, File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getFilesWithExtension(endsWith, parentDir));
            } else {
                if (file.getName().endsWith(endsWith)){
                    inFiles.add(file);
                }
            }
        }
        return inFiles;
    }
    
//    public static void copyFile(InputStream in, OutputStream out) throws IOException {
//        byte[] buffer = new byte[1024];
//        int read;
//        while((read = in.read(buffer)) != -1){
//          out.write(buffer, 0, read);
//        }
//    }

    /**
     * Notify observers of buildprovider's build table that its contents have changed
     */
    public static void notifyBuildProviderObservers(Context c) {
		Uri buildTableUri = Uri.withAppendedPath(BuildOrderProvider.BASE_URI, DbAdapter.TABLE_BUILD_ORDER);
		c.getContentResolver().notifyChange(buildTableUri, null);
    }
    
    /**
     * Creates builds directory on user's SD card if needed
     * 
     * @param c
     * @return false if the builds dir doesn't exist and couldn't be created,
     * true otherwise
     */
    public static boolean createBuildsDir(Context c) { 
    	File root = Environment.getExternalStorageDirectory();
    	File buildsDir = new File(root, BUILDS_DIR);
    	if (!buildsDir.exists()) {
    		return buildsDir.mkdirs();
    	}
    	return true;
    }
    
    /**
     * Helper for getting Fragments inside the tab pager
     */
    public static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }
    
    public static List<String> getStringListFromAsset(Context c, String assetName) {
    	// read asset file into string buffer
    	String bufferString;
		try {
	    	InputStream input;
			input = c.getAssets().open(assetName);
			int size = input.available();
	        byte[] buffer = new byte[size];
	        input.read(buffer);
	        input.close();
	        bufferString = new String(buffer);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
        
    	Gson gson = new Gson();
    	try {
    		String[] list = gson.fromJson(bufferString, String[].class);
    		return Arrays.asList(list);
    	} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return null;
    	}
    }
}
