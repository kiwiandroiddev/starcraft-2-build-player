package com.kiwiandroiddev.sc2buildassistant.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import com.google.ads.Ad;
import com.google.ads.AdView;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.kiwiandroiddev.sc2buildassistant.BuildOrderProvider;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.util.OnReceiveAdListener;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import static com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys.KEY_BUILD_ID;
import static com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys.KEY_BUILD_NAME;
import static com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys.KEY_EXPANSION_ENUM;
import static com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys.KEY_FACTION_ENUM;

/**
 * Screen for showing an explanation of the build order, including references etc.
 * From here users can play the build order by pressing the Play action item.
 */
public class BriefActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final HashMap<DbAdapter.Faction, Integer> sRaceBgMap;
    private static final ArrayList<String> sColumns;

	@InjectExtra(KEY_BUILD_ID) long mBuildId;
	@InjectExtra(KEY_FACTION_ENUM) DbAdapter.Faction mFaction;
	@InjectExtra(KEY_EXPANSION_ENUM) DbAdapter.Expansion mExpansion;
	@InjectExtra(KEY_BUILD_NAME) String mBuildName;

    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.brief_buildSubTitle) TextView mSubtitleView;
    @InjectView(R.id.brief_root) View mRootView;
    @InjectView(R.id.brief_buildNotes) TextView mNotesView;
    @InjectView(R.id.brief_author_layout) View mAuthorLayout;
    @InjectView(R.id.brief_author) TextView mAuthorText;
    @InjectView(R.id.ad) AdView mAdView;

    // TODO temp!
    @InjectView(R.id.buildName) TextView mBuildNameText;

	static {
		sRaceBgMap = new HashMap<DbAdapter.Faction, Integer>();
		sRaceBgMap.put(DbAdapter.Faction.TERRAN, R.drawable.terran_icon_blur_drawable);
		sRaceBgMap.put(DbAdapter.Faction.PROTOSS, R.drawable.protoss_icon_blur_drawable);
		sRaceBgMap.put(DbAdapter.Faction.ZERG, R.drawable.zerg_icon_blur_drawable);
		
		// Columns from the build order table containing info we want to display
		sColumns = new ArrayList<String>();
		sColumns.add(DbAdapter.KEY_SOURCE);
		sColumns.add(DbAdapter.KEY_DESCRIPTION);
		sColumns.add(DbAdapter.KEY_AUTHOR);
	}

	/**
	 * Starts a new BriefActivity for the given build details.
	 *
	 * Only the buildId is strictly needed, having other build fields passed in is a speed
	 * optimization - saves the new BriefActivity from having to refetch these itself using the
	 * build ID.
	 *
	 * @param callingActivity
	 * @param buildId
	 * @param faction
	 * @param expansion
	 * @param buildName
	 */
	public static void open(Activity callingActivity,
							long buildId,
							DbAdapter.Faction faction,
							DbAdapter.Expansion expansion,
							String buildName,
							TextView sharedBuildNameTextView) {
		Intent i = new Intent(callingActivity, BriefActivity.class);
		i.putExtra(KEY_BUILD_ID, buildId);	// pass build order record ID

		// speed optimization - pass these so brief activity doesn't need to
		// requery them from the database and can display them instantly
		i.putExtra(KEY_FACTION_ENUM, faction);
		i.putExtra(KEY_EXPANSION_ENUM, expansion);
		i.putExtra(KEY_BUILD_NAME, buildName);

		if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

			// create the transition animation - the views in the layouts
			// of both activities are defined with android:transitionName="buildName"
			ActivityOptions options = ActivityOptions
					.makeSceneTransitionAnimation(callingActivity,
							sharedBuildNameTextView, "buildName");

			// start the new activity
			callingActivity.startActivity(i, options.toBundle());
		} else {
			callingActivity.startActivity(i);
		}
	}

    @Override
	protected void onCreate(Bundle savedInstanceState) {
//		mStartTime = SystemClock.uptimeMillis();
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	if (!sharedPref.getBoolean(SettingsActivity.KEY_SHOW_STATUS_BAR, false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	}
		
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brief);
        ButterKnife.inject(this);

        if (savedInstanceState == null) {
			// init member variables using Intent extras
			Dart.inject(this);
        } else {
			// init member variables using previously saved instance state bundle
			Dart.inject(this, savedInstanceState);
        }

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // request a cursor loader from the loader manager. This will be used to
        // fetch build order info from the database.
        getSupportLoaderManager().initLoader(0, null, this);

        // show build title, faction, expansion now
        displayBasicInfo();

		// fade in ad banner when the image loads rather than popping
		if (mAdView != null) {
			mAdView.setAlpha(0.0f);
			mAdView.setAdListener(new OnReceiveAdListener() {
				@Override
				public void onReceiveAd(Ad ad) {
					mAdView.animate()
							.alpha(1.0f)
							.setInterpolator(new FastOutSlowInInterpolator())
							.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
				}
			});
		}

        trackBriefView();

        // sc2brief
        //Debug.stopMethodTracing();
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
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.brief_menu, menu);		// add the "play build" action bar item
       inflater.inflate(R.menu.options_menu, menu);
       return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {



    	if (item.getItemId() == android.R.id.home) {
            finishCompat();
            return true;
        } else if (item.getItemId() == R.id.menu_edit_build) {
			Intent i = new Intent(this, EditBuildActivity.class);
			i.putExtra(KEY_BUILD_ID, mBuildId);
			startActivity(i);
			return true;
		}
    	
    	// use the same options menu as the main activity 
    	boolean result = MainActivity.OnMenuItemSelected(this, item);
    	if (!result) {
			return super.onOptionsItemSelected(item);
		} else {
			return true;
		}
    }

    @Override
    public void onBackPressed() {
        finishCompat();
    }

    private void finishCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
    }

    @OnClick(R.id.activity_brief_play_action_button)
    public void playBuild() {
        Intent i = new Intent(this, PlaybackActivity.class);
        i.putExtra(KEY_BUILD_ID, mBuildId);
        startActivity(i);
    }

    //=========================================================================
    // Android lifecycle methods
	//=========================================================================
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_BUILD_ID, mBuildId);
        outState.putString(KEY_BUILD_NAME, mBuildName);
        outState.putSerializable(KEY_FACTION_ENUM, mFaction);
        outState.putSerializable(KEY_EXPANSION_ENUM, mExpansion);
    }
    
	//=========================================================================
	// Callbacks from layout XML widgets
	//=========================================================================
	
	/* returns a string resource ID */
	public static int getBackgroundDrawable(DbAdapter.Faction race) {
		return sRaceBgMap.get(race);
	}

	/** 
	 * Immediately displays title, faction and expansion info. These data are sent from the
	 * calling activity and don't need to be queried from the database 
	 */
	private void displayBasicInfo() {
		final String race = getString(DbAdapter.getFactionName(mFaction));
		final String expansion = getString(DbAdapter.getExpansionName(mExpansion));

		// Toolbar subtitle example: "Terran - Wings of Liberty"
//        getSupportActionBar().setTitle(mBuildName);

        // TODO temp
        mBuildNameText.setText(mBuildName);

//        getSupportActionBar().setSubtitle(race + " - " + expansion);
//        mToolbar.setTitle(mBuildName);
//        mToolbar.setSubtitle(race + " - " + expansion);

		// set background graphic (stub)
		mRootView.setBackgroundDrawable(getResources().getDrawable(getBackgroundDrawable(mFaction)));
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this,
                Uri.withAppendedPath(BuildOrderProvider.BASE_URI, DbAdapter.TABLE_BUILD_ORDER),	// table URI
                sColumns.toArray(new String[sColumns.size()]),									// columns to return
                DbAdapter.KEY_BUILD_ORDER_ID + " = " + mBuildId,								// select clause
                null,																			// select args
                null);																			// sort by
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		cursor.moveToFirst();
		
		final int sourceIndex = sColumns.indexOf(DbAdapter.KEY_SOURCE);
		final int notesIndex = sColumns.indexOf(DbAdapter.KEY_DESCRIPTION);
		final int authorIndex = sColumns.indexOf(DbAdapter.KEY_AUTHOR);
		
		final String source = cursor.getString(sourceIndex);
		final String notes = cursor.getString(notesIndex);
		final String author = cursor.getString(authorIndex);
		
		// just a textview as part of the main content - not the action bar subtitle!
		if (source != null) {	
			mSubtitleView.setText(Html.fromHtml(source));
            
			// makes links clickable
			mSubtitleView.setMovementMethod(LinkMovementMethod.getInstance());
		}
		
		if (notes != null) {
			mNotesView.setText(Html.fromHtml(notes));
			mNotesView.setMovementMethod(LinkMovementMethod.getInstance());
		}

		if (author != null) {
			mAuthorLayout.setVisibility(View.VISIBLE);
			mAuthorText.setText(author);
		} else {
			mAuthorLayout.setVisibility(View.GONE);
		}
//        Timber.d(TAG, "time to load = " + (SystemClock.uptimeMillis() - mStartTime) + " ms");
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// do nothing
	}
	
	/**
	 * Send info about this brief view to Google Analytics as it's of interest
	 * which builds are being viewed and which aren't
	 */
	private void trackBriefView() {
    	EasyTracker.getInstance(this).send(
				MapBuilder.createEvent(
					"brief_view", mExpansion.toString() + "_" + mFaction.toString(), mBuildName, null)
				.build());
	}

}
