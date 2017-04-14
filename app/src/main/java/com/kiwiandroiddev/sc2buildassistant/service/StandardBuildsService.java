package com.kiwiandroiddev.sc2buildassistant.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.kiwiandroiddev.sc2buildassistant.util.IOUtils;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsActivity;
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import timber.log.Timber;

/**
 * Provides methods for initialising a local database with the set of stock build orders,
 * and upgrading/migrating them when changes are made to the stock selection in app updates.
 *
 * Created by matt on 17/07/15.
 */
public class StandardBuildsService {
	private static final int BUILD_FILES_VERSION = 49;	// tracks changes to build JSON files in assets/
	private static final String ASSETS_BUILDS_DIR = "builds";

	/**
	 * Returns an observable on the progress of loading stock build orders into the local SQLite DB.
	 * Should be scheduled on a worker thread.
	 *
	 * @param c context
	 * @param forceLoad if false, builds are only copied if an upgrade is required. If true,
	 *                  standard builds are always copied.
	 * @return observable on load progress (percentage)
	 */
	public static Observable<Integer> getLoadStandardBuildsIntoDBObservable(final Context c, final boolean forceLoad) {
		return Observable.create(new Observable.OnSubscribe<Integer>() {
			@Override
			public void call(final Subscriber<? super Integer> observer) {
				try {
					if (!observer.isUnsubscribed()) {
						loadStandardBuildsIntoDB(c, forceLoad, new DbAdapter.ProgressListener() {
							@Override
							public void onProgressUpdate(int percent) {
								if (!observer.isUnsubscribed()) {
									observer.onNext(percent);
								}
							}
						});
						observer.onCompleted();
					}
				} catch (Exception e) {
					observer.onError(e);
				}
			}
		}).onBackpressureBuffer();
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
	private static void loadStandardBuildsIntoDB(Context c, boolean forceLoad,
			DbAdapter.ProgressListener listener) throws IOException {
        DbAdapter db = ((MyApplication) c.getApplicationContext()).getDb();
        db.open();

        final int oldVersion = getStoredBuildsVersion(c);
        final int newVersion = BUILD_FILES_VERSION;
        final boolean outOfDate = oldVersion < newVersion;

        if (forceLoad || outOfDate) {
        	// handle updating standard builds version
        	doUpdateBuilds(c, db, oldVersion, newVersion);

        	ArrayList<Build> stdBuilds = fetchStandardBuilds(c);
        	//Timber.d(TAG, "in loadStandardBuildsIntoDB(), stdBuilds = " + stdBuilds);
        	db.addOrReplaceBuilds(stdBuilds, listener);

        	if (outOfDate) {
				updateBuildsVersion(c);
			}
        }
	}

	// TODO make private
	public static void loadStandardBuildsIntoDB(Context c, boolean forceLoad) throws IOException {
		loadStandardBuildsIntoDB(c, forceLoad, null);
	}

	/**
	 * Sets the user's saved builds version to the current builds version constant.
	 * This should be done after the standard builds on their SD card have been updated.
	 */
	private static void updateBuildsVersion(Context c) {
		IOUtils.writeIntToSharedPrefs(c, SettingsActivity.KEY_BUILDS_VERSION, BUILD_FILES_VERSION);
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
			List<String> buildsToDelete = IOUtils.getStringListFromAsset(c, "39_old_builds.json");
			if (buildsToDelete != null) {
				for (String name : buildsToDelete) {
					db.deleteBuild(name);
				}
			}
			//db.deleteBuild("YoDa's Reaper FE into Widow Mine Drop (vs. Terran)");	// stub
		}
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
		ArrayList<Build> allBuilds = new ArrayList<>();

		for (String filename : IOUtils.getAssetsWithExtension(c, ".json", ASSETS_BUILDS_DIR)) {
			InputStream inputStream = c.getAssets().open(ASSETS_BUILDS_DIR + "/" + filename);
			ArrayList<Build> builds = JsonBuildService.readBuildsFromJsonInputStream(inputStream); 	// stub
			if (builds == null) {
				Timber.e("JSON syntax error with file " + filename);
			} else {
				allBuilds.addAll(builds);
			}
		}

		return allBuilds;
	}

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
	 * @return true if user's saved builds version is different to the current builds version constant,
	 * meaning one or more of the standard build files has been updated
	 */
//    private static boolean buildsOutOfDate(Context c) {
//    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
//    	return (prefs.getInt(SettingsActivity.KEY_BUILDS_VERSION, -1) != BuildListActivity.BUILD_FILES_VERSION);
//    }
}
