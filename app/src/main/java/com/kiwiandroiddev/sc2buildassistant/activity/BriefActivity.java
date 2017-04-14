package com.kiwiandroiddev.sc2buildassistant.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.kiwiandroiddev.sc2buildassistant.BuildOrderProvider;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.ads.AdLoader;
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Expansion;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction;
import com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsActivity;
import com.kiwiandroiddev.sc2buildassistant.util.NoOpAnimationListener;
import com.kiwiandroiddev.sc2buildassistant.view.WindowInsetsCapturingView;
import com.kiwiandroiddev.sc2buildassistant.view.WindowInsetsCapturingView.OnCapturedWindowInsetsListener;

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
public class BriefActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final HashMap<Faction, Integer> sRaceBgMap;
    private static final ArrayList<String> sColumns;

    @InjectExtra(KEY_BUILD_ID) long mBuildId;
    @InjectExtra(KEY_FACTION_ENUM) Faction mFaction;
    @InjectExtra(KEY_EXPANSION_ENUM) Expansion mExpansion;
    @InjectExtra(KEY_BUILD_NAME) String mBuildName;

    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.brief_buildSubTitle) TextView mSubtitleView;
    @InjectView(R.id.brief_root) View mRootView;
    @InjectView(R.id.brief_buildNotes) TextView mNotesView;
    @InjectView(R.id.brief_author_layout) View mAuthorLayout;
    @InjectView(R.id.brief_author) TextView mAuthorText;
    @InjectView(R.id.activity_brief_play_action_button) FloatingActionButton mPlayButton;
    @InjectView(R.id.ad_frame) ViewGroup mAdFrame;
    @InjectView(R.id.brief_window_insets_capturing_view) WindowInsetsCapturingView mWindowInsetsCapturingView;
    @InjectView(R.id.brief_content_layout) ViewGroup mBriefContentLayout;

    // TODO temp!
    @InjectView(R.id.buildName) TextView mBuildNameText;

    static {
        sRaceBgMap = new HashMap<Faction, Integer>();
        sRaceBgMap.put(Faction.TERRAN, R.drawable.terran_icon_blur_drawable);
        sRaceBgMap.put(Faction.PROTOSS, R.drawable.protoss_icon_blur_drawable);
        sRaceBgMap.put(Faction.ZERG, R.drawable.zerg_icon_blur_drawable);

        // Columns from the build order table containing info we want to display
        sColumns = new ArrayList<String>();
        sColumns.add(DbAdapter.KEY_SOURCE);
        sColumns.add(DbAdapter.KEY_DESCRIPTION);
        sColumns.add(DbAdapter.KEY_AUTHOR);
    }

    @DrawableRes
    public static int getBackgroundDrawable(Faction race) {
        return sRaceBgMap.get(race);
    }

    /**
     * Starts a new BriefActivity for the given build details.
     * <p>
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
                            Faction faction,
                            Expansion expansion,
                            String buildName,
                            TextView sharedBuildNameTextView) {
        Intent i = new Intent(callingActivity, BriefActivity.class);
        i.putExtra(KEY_BUILD_ID, buildId);    // pass build order record ID

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
        initSystemUiVisibility();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brief);
        ButterKnife.inject(this);

        initIntentParameterFields(savedInstanceState);
        initToolbar();
        startLoadingBriefFromDb();
        displayBasicInfo();
        setAdBannerVisibilityBasedOnCurrentPreference();
        trackBriefView();
        initScrollView();
        ensureBriefContentIsNotHiddenBySystemBars();
        registerSelfAsPreferenceChangeListener();
    }

    private void registerSelfAsPreferenceChangeListener() {
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SettingsActivity.KEY_SHOW_ADS)) {
            setAdBannerVisibilityBasedOnCurrentPreference();
        }
    }

    private void setAdBannerVisibilityBasedOnCurrentPreference() {
        if (shouldShowAds()) {
            showAdBanner();
        } else {
            hideAdBanner();
        }
    }

    private boolean shouldShowAds() {
        SharedPreferences sharedPref = getSharedPreferences();
        return sharedPref.getBoolean(SettingsActivity.KEY_SHOW_ADS, true);
    }

    private void showAdBanner() {
        mAdFrame.setVisibility(View.VISIBLE);

        if (mAdFrame.getChildCount() == 0) {
            AdView adView = new AdView(this);
            mAdFrame.addView(adView);
            AdLoader.loadAdForRealUsers(adView);
            fadeInAdOnLoad(adView);
        }
    }

    private void hideAdBanner() {
        mAdFrame.setVisibility(View.GONE);
    }

    private void initSystemUiVisibility() {
        if (!getSharedPreferences().getBoolean(SettingsActivity.KEY_SHOW_STATUS_BAR, false)) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        makeNavigationBarTranslucentIfPossible();
    }

    private void initToolbar() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void initScrollView() {
        NestedScrollView nestedScrollView = (NestedScrollView) findViewById(R.id.brief_nested_scroll_view);
        nestedScrollView.setOnScrollChangeListener(new OnScrollDirectionChangedListener() {
            @Override
            public void onStartScrollingDown() {
                onScrollDownBrief();
            }

            @Override
            public void onStartScrollingUp() {
                onScrollUpBrief();
            }
        });
    }

    private void onScrollDownBrief() {
        mPlayButton.hide();
        hideToolbar();
        hideSystemNavigationBar();
    }

    private void onScrollUpBrief() {
        mPlayButton.show();
        showToolbar();
        showSystemNavigationBar();
    }

    private void hideToolbar() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_and_fade_out_to_top);
        animation.setAnimationListener(new NoOpAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mToolbar.setVisibility(View.GONE);
            }
        });
        mToolbar.startAnimation(animation);
    }

    private void showToolbar() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_and_fade_in_from_top);
        animation.setAnimationListener(new NoOpAnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mToolbar.setVisibility(View.VISIBLE);
            }
        });
        mToolbar.startAnimation(animation);
    }

    private void hideSystemNavigationBar() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void showSystemNavigationBar() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void makeNavigationBarTranslucentIfPossible() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    private void initIntentParameterFields(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Dart.inject(this);
        } else {
            Dart.inject(this, savedInstanceState);
        }
    }

    private void startLoadingBriefFromDb() {
        getSupportLoaderManager().initLoader(0, null, this);
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void fadeInAdOnLoad(final AdView adView) {
        adView.setAlpha(0.0f);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                adView.animate()
                        .alpha(1.0f)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
            }
        });
    }

    private void ensureBriefContentIsNotHiddenBySystemBars() {
        mWindowInsetsCapturingView.setOnCapturedWindowInsetsListener(new OnCapturedWindowInsetsListener() {
            @Override
            public void onCapturedWindowInsets(Rect insets) {
                mBriefContentLayout.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                mWindowInsetsCapturingView.clearOnCapturedWindowInsetsListener();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterSelfAsPreferenceChangeListener();
    }

    private void unregisterSelfAsPreferenceChangeListener() {
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.brief_menu, menu);        // add the "play build" action bar item
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_BUILD_ID, mBuildId);
        outState.putString(KEY_BUILD_NAME, mBuildName);
        outState.putSerializable(KEY_FACTION_ENUM, mFaction);
        outState.putSerializable(KEY_EXPANSION_ENUM, mExpansion);
    }

    /**
     * Immediately displays title, faction and expansion info. These data are sent from the
     * calling activity and don't need to be queried from the database
     */
    private void displayBasicInfo() {
        mBuildNameText.setText(mBuildName);
        mRootView.setBackgroundDrawable(getResources().getDrawable(getBackgroundDrawable(mFaction)));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Uri.withAppendedPath(BuildOrderProvider.BASE_URI, DbAdapter.TABLE_BUILD_ORDER),    // table URI
                sColumns.toArray(new String[sColumns.size()]),                                    // columns to return
                DbAdapter.KEY_BUILD_ORDER_ID + " = " + mBuildId,                                // select clause
                null,                                                                            // select args
                null);                                                                            // sort by
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
