package com.kiwiandroiddev.sc2buildassistant.activity;

import java.util.ArrayList;
import java.util.HashMap;

//import com.google.analytics.tracking.android.EasyTracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;

import com.kiwiandroiddev.sc2buildassistant.BuildOrderProvider;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.RaceFragment;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.view.ObservableScrollView;

/**
 * Screen for showing an explanation of the build order, including references etc.
 * From here users can play the build order by pressing the Play action item.
 */
public class BriefActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        ObservableScrollView.Callbacks {

//	private static final String TAG = "BriefActivity";
	
	private long mBuildId;
//	private long mStartTime;	// temp optimization testing
	private DbAdapter.Faction mFaction;
	private DbAdapter.Expansion mExpansion;
	private String mBuildName;
//	private String mAuthorName;
	
	private static final HashMap<DbAdapter.Faction, Integer> sRaceBgMap;
	private static final ArrayList<String> sColumns;

    private static final int STATE_ONSCREEN = 0;
    private static final int STATE_OFFSCREEN = 1;
    private static final int STATE_RETURNING = 2;

    private View mQuickReturnView;
    private View mPlaceholderView;
    private ObservableScrollView mObservableScrollView;
    private ScrollSettleHandler mScrollSettleHandler = new ScrollSettleHandler();
    private int mMinRawY = 0;
    private int mState = STATE_ONSCREEN;
    private int mQuickReturnHeight;
    private int mMaxScrollY;

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

    private Toolbar mToolbar;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
//		mStartTime = SystemClock.uptimeMillis();
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	if (!sharedPref.getBoolean(SettingsActivity.KEY_SHOW_STATUS_BAR, false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	}
		
		super.onCreate(savedInstanceState);
//		setTheme(R.style.Theme_Sherlock);
        setContentView(R.layout.activity_brief);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        Log.d(TAG, "onCreate(), mBuildId = " + mBuildId);
        
        if (savedInstanceState == null) {
        	mBuildId = getIntent().getExtras().getLong(RaceFragment.KEY_BUILD_ID);
        	mBuildName = getIntent().getExtras().getString(RaceFragment.KEY_BUILD_NAME);
        	mFaction = (DbAdapter.Faction) getIntent().getExtras().getSerializable(RaceFragment.KEY_FACTION_ENUM);
        	mExpansion = (DbAdapter.Expansion) getIntent().getExtras().getSerializable(RaceFragment.KEY_EXPANSION_ENUM);
        } else {
        	mBuildId = savedInstanceState.getLong(RaceFragment.KEY_BUILD_ID, -1);
        	mBuildName = savedInstanceState.getString(RaceFragment.KEY_BUILD_NAME);
        	mFaction = (DbAdapter.Faction) savedInstanceState.getSerializable(RaceFragment.KEY_FACTION_ENUM);
        	mExpansion = (DbAdapter.Expansion) savedInstanceState.getSerializable(RaceFragment.KEY_EXPANSION_ENUM);
        }

        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);

        mObservableScrollView = (ObservableScrollView) findViewById(R.id.scrollView1);
        mObservableScrollView.setCallbacks(this);

        mQuickReturnView = mToolbar;
        mPlaceholderView = findViewById(R.id.placeholder);

        mObservableScrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        onScrollChanged(mObservableScrollView.getScrollY());
                        mMaxScrollY = mObservableScrollView.computeVerticalScrollRange()
                                - mObservableScrollView.getHeight();
                        mQuickReturnHeight = mQuickReturnView.getHeight();
                    }
                });

        // request a cursor loader from the loader manager. This will be used to
        // fetch build order info from the database.
        getSupportLoaderManager().initLoader(0, null, this);

        // show build title, faction, expansion now
        displayBasicInfo();

        trackBriefView();

//        Log.d(TAG, "time to load = " + (SystemClock.uptimeMillis() - start) + " ms");
        
        // sc2brief
        //Debug.stopMethodTracing();
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
       inflater.inflate(R.menu.brief_menu, menu);		// add the "play build" action bar item
       inflater.inflate(R.menu.options_menu, menu);
       return true;
    }
	
    /* handle "Up" button press on title bar, navigate back to build list */
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                // This is called when the Home (Up) button is pressed
//                // in the Action Bar.
//                finish();
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// Handle Briefing screen specific menu items
    	if (item.getItemId() == R.id.menu_play_build) {
    		// starts the build player interface
            playBuild();
    	} else if (item.getItemId() == android.R.id.home) {
            // This is called when the Home (Up) button is pressed
            // in the Action Bar.
            finish();
            return true;
        }
    	
    	// use the same options menu as the main activity 
    	boolean result = BuildListActivity.OnMenuItemSelected(this, item);
    	if (!result)
    		return super.onOptionsItemSelected(item);
    	else
    		return true;
    }

    private void playBuild() {
        Intent i = new Intent(this, PlaybackActivity.class);
        i.putExtra(RaceFragment.KEY_BUILD_ID, mBuildId);
        startActivity(i);
    }

    //=========================================================================
    // Android lifecycle methods
	//=========================================================================
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(RaceFragment.KEY_BUILD_ID, mBuildId);
        outState.putString(RaceFragment.KEY_BUILD_NAME, mBuildName);
        outState.putSerializable(RaceFragment.KEY_FACTION_ENUM, mFaction);
        outState.putSerializable(RaceFragment.KEY_EXPANSION_ENUM, mExpansion);
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
        mToolbar.setTitle(mBuildName);
        mToolbar.setSubtitle(race + " - " + expansion);

		// set background graphic (stub)
		View root = this.findViewById(R.id.brief_root);
		root.setBackgroundDrawable(getResources().getDrawable(getBackgroundDrawable(mFaction)));
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
			TextView subtitleView = (TextView)this.findViewById(R.id.brief_buildSubTitle);	
			subtitleView.setText(Html.fromHtml(source));
			// makes links clickable
			subtitleView.setMovementMethod(LinkMovementMethod.getInstance());
		}
		
		if (notes != null) {
			TextView notesView = (TextView)this.findViewById(R.id.brief_buildNotes);
			notesView.setText(Html.fromHtml(notes));
			notesView.setMovementMethod(LinkMovementMethod.getInstance());
		}
		
		View authorLayout = this.findViewById(R.id.brief_author_layout);
		if (author != null) {
			authorLayout.setVisibility(View.VISIBLE);
			TextView authorView = (TextView)this.findViewById(R.id.brief_author);
			authorView.setText(author);
		} else {
			authorLayout.setVisibility(View.GONE);
		}
//        Log.d(TAG, "time to load = " + (SystemClock.uptimeMillis() - mStartTime) + " ms");
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
//    	EasyTracker.getInstance().setContext(this);
//    	EasyTracker.getTracker().sendEvent("brief_view", mExpansion.toString() + "_" + mFaction.toString(), mBuildName, null);
	}

    //=========================================================================
    // Quick return Toolbar implementation
    //=========================================================================

    @Override
    public void onScrollChanged(int scrollY) {
        scrollY = Math.min(mMaxScrollY, scrollY);

        mScrollSettleHandler.onScroll(scrollY);

        int rawY = mPlaceholderView.getTop() - scrollY;
        int translationY = 0;

        switch (mState) {
            case STATE_OFFSCREEN:
                if (rawY <= mMinRawY) {
                    mMinRawY = rawY;
                } else {
                    mState = STATE_RETURNING;
                }
                translationY = rawY;
                break;

            case STATE_ONSCREEN:
                if (rawY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                translationY = rawY;
                break;

            case STATE_RETURNING:
                translationY = (rawY - mMinRawY) - mQuickReturnHeight;
                if (translationY > 0) {
                    translationY = 0;
                    mMinRawY = rawY - mQuickReturnHeight;
                }

                if (rawY > 0) {
                    mState = STATE_ONSCREEN;
                    translationY = rawY;
                }

                if (translationY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                break;
        }
        mQuickReturnView.animate().cancel();
        mQuickReturnView.setTranslationY(translationY + scrollY);
    }

    @Override
    public void onDownMotionEvent() {
        mScrollSettleHandler.setSettleEnabled(false);
    }

    @Override
    public void onUpOrCancelMotionEvent() {
        mScrollSettleHandler.setSettleEnabled(true);
        mScrollSettleHandler.onScroll(mObservableScrollView.getScrollY());
    }

    private class ScrollSettleHandler extends Handler {
        private static final int SETTLE_DELAY_MILLIS = 100;

        private int mSettledScrollY = Integer.MIN_VALUE;
        private boolean mSettleEnabled;

        public void onScroll(int scrollY) {
            if (mSettledScrollY != scrollY) {
                // Clear any pending messages and post delayed
                removeMessages(0);
                sendEmptyMessageDelayed(0, SETTLE_DELAY_MILLIS);
                mSettledScrollY = scrollY;
            }
        }

        public void setSettleEnabled(boolean settleEnabled) {
            mSettleEnabled = settleEnabled;
        }

        @Override
        public void handleMessage(Message msg) {
            // Handle the scroll settling.
            if (STATE_RETURNING == mState && mSettleEnabled) {
                int mDestTranslationY;
                if (mSettledScrollY - mQuickReturnView.getTranslationY() > mQuickReturnHeight / 2) {
                    mState = STATE_OFFSCREEN;
                    mDestTranslationY = Math.max(
                            mSettledScrollY - mQuickReturnHeight,
                            mPlaceholderView.getTop());
                } else {
                    mDestTranslationY = mSettledScrollY;
                }

                mMinRawY = mPlaceholderView.getTop() - mQuickReturnHeight - mDestTranslationY;
                mQuickReturnView.animate().translationY(mDestTranslationY);
            }
            mSettledScrollY = Integer.MIN_VALUE; // reset
        }
    }
}
