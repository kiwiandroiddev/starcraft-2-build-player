package com.kiwiandroiddev.sc2buildassistant.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.f2prateek.dart.Dart;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.adapter.BuildItemAdapter;
import com.kiwiandroiddev.sc2buildassistant.data.RealCurrentTimeProvider;
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.ItemType;
import com.kiwiandroiddev.sc2buildassistant.domain.BuildPlayer;
import com.kiwiandroiddev.sc2buildassistant.domain.BuildPlayerEventListener;
import com.kiwiandroiddev.sc2buildassistant.domain.GameSpeeds;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem;
import com.kiwiandroiddev.sc2buildassistant.util.EasyTrackerUtils;
import com.kiwiandroiddev.sc2buildassistant.util.MapFormat;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Provides the UI to play back, stop, pause and seek within a build order.
 * Displays visual and audio alerts when build items are reached during playback.
 * A BuildPlayer object is used to handle the low-level playback logic; this
 * activity handles the playback UI.
 * 
 * @author matt
 *
 */
// credit for timer code: http://kristjansson.us/?p=1010
public class PlaybackActivity extends AppCompatActivity implements OnSeekBarChangeListener, OnInitListener,
														  OnSharedPreferenceChangeListener, BuildPlayerEventListener {

    private static final String KEY_BUILD_PLAYER_OBJECT = "BuildPlayer";

	public static final int TIME_STEP = 100;	// milliseconds between updates
	private Build mBuild;	// build order to play back, passed in by main activity
	private BuildPlayer mBuildPlayer;
	private DbAdapter mDb;
	private Handler mHandler = new Handler();
	private Queue<Integer> mPendingAlerts;		// values are indices of build items in the build

    private View mTimerTextContainer;
	private TextView mMaxTimeText;
	private TextView mTimerText;
    @InjectView(R.id.buildListView) ListView mBuildListView;
	@InjectView(R.id.playPauseButton) ImageButton mPlayPauseButton;
	@InjectView(R.id.stopButton) ImageButton mStopButton;
	@InjectView(R.id.seekBar) SeekBar mSeekBar;
	@InjectView(R.id.overlayIcon) ImageView mOverlayIcon;
	@InjectView(R.id.overlayContainer) View mOverlayContainer;
	@InjectView(R.id.overlayText) TextView mOverlayText;

	private boolean mUserIsSeeking = false;
	private TextToSpeech mTts;
	private boolean mTtsReady = false;
	
    private Runnable mUpdateTimeTask = new Runnable() {    	
		public void run()
		{
			mBuildPlayer.iterate();
			mHandler.postDelayed(mUpdateTimeTask, TIME_STEP);
		}
    };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        // hides status bar on Android 4.0+, on Android 2.3.x status bar is already hidden from the app theme...
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        ButterKnife.inject(this);

		//noinspection ConstantConditions
		getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        // Inflate the custom view
        mTimerTextContainer = LayoutInflater.from(this).inflate(R.layout.playback_time_text, null);

        // Attach to the action bar
        getSupportActionBar().setCustomView(mTimerTextContainer);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // always show "Media Volume" control, instead of ringer volume
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // get a reference to global DB instance to get drawable resources for build items, etc.
        MyApplication app = (MyApplication) getApplication();
        mDb = app.getDb();
        
        mPendingAlerts = new LinkedList<Integer>();

        mTimerText = (TextView)mTimerTextContainer.findViewById(R.id.timerText);
        mMaxTimeText = (TextView)mTimerTextContainer.findViewById(R.id.maxTimeText);
        mOverlayContainer.setVisibility(View.INVISIBLE);

        // Try to init TTS engine
		initTTS();

        // fetch the build object from database associated with ID we were passed
        long buildId = Dart.get(getIntent().getExtras(), IntentKeys.KEY_BUILD_ID);
        DbAdapter db = new DbAdapter(this);
        db.open();
        mBuild = db.fetchBuild(buildId);
        db.close();
        
        // create build player object from build passed in intent
        if (savedInstanceState != null && savedInstanceState.getSerializable(KEY_BUILD_PLAYER_OBJECT) != null) {
			mBuildPlayer = (BuildPlayer)savedInstanceState.getSerializable(KEY_BUILD_PLAYER_OBJECT);
//        	Log.w(this.toString(), "saved buildplayer found, player = " + savedPlayer + ", numListeners = " + savedPlayer.getNumListeners());
        } else {
        	mBuildPlayer = new BuildPlayer(new RealCurrentTimeProvider(), mBuild.getItems());
        }
        mBuildPlayer.registerListener(this);
        
        int buildDuration = mBuildPlayer.getDuration();
        mMaxTimeText.setText(String.format("%02d:%02d", buildDuration / 60, buildDuration % 60));
        
        mSeekBar.setMax(buildDuration);	// NOTE: progress bar units are seconds!
        mSeekBar.setOnSeekBarChangeListener(this);
        
        // populate list view with build items
        BuildItemAdapter bAdapter = new BuildItemAdapter(this,
        		R.layout.build_item_row, mBuild.getItems());

        // Add spacer to list so transparent seekbar doesn't obscure last build item
        mBuildListView.addFooterView(getFooterView(), null, false);     // false = not selectable

        mBuildListView.setAdapter(bAdapter);
        
        // keep screen from sleeping until the build has played through
        setKeepScreenOn(true);
        
        // Initialize spinner to choose playback speed (Slower, Slow, Normal, Fast, Faster)
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        Log.w(this.toString(), "sharedPref identity = " + sharedPref.toString());
        sharedPref.registerOnSharedPreferenceChangeListener(this);		// notify this activity when settings change
        		
		// hacky: initialize buildplayer with stored preferences
        this.onSharedPreferenceChanged(sharedPref, SettingsActivity.KEY_GAME_SPEED);
		this.onSharedPreferenceChanged(sharedPref, SettingsActivity.KEY_EARLY_WARNING);
		this.onSharedPreferenceChanged(sharedPref, SettingsActivity.KEY_START_TIME);
		
		// timer start
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 0);
        
        trackPlaybackView();
        
        // sc2play
//    	Debug.stopMethodTracing();
	}

    private View getFooterView() {
        return getLayoutInflater().inflate(R.layout.activity_playback_spacer_row, null, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.playback_menu, menu);
       return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed
                // in the Action Bar.
                finish();
        }

    	// use the same options menu as the main activity
    	if (!MainActivity.OnMenuItemSelected(this, item)) {
			return super.onOptionsItemSelected(item);
		} else {
			return true;
		}
    }

	//=========================================================================
	// Android life cycle methods
	//=========================================================================
	
    @Override
    public void onStart() {
    	super.onStart();
    	EasyTracker.getInstance(this).activityStart(this);
    }

	@Override
	public void onStop() {
//		Log.w(this.toString(), "onStop() called, mBuildPlayer = " + mBuildPlayer);
		super.onStop();
		mHandler.removeCallbacks(mUpdateTimeTask);
    	EasyTracker.getInstance(this).activityStop(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
//		Log.w(this.toString(), "onPause() called");
		mHandler.removeCallbacks(mUpdateTimeTask);
	}
	
	@Override
	public void onResume() {
//		Log.w(this.toString(), "onResume() called, mBuildPlayer = " + mBuildPlayer);
		super.onResume();
		mBuildPlayer.removeListeners();
		mBuildPlayer.registerListener(this);
		mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 0);
        
        // init preferences
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		mBuildPlayer.removeListeners();
		outState.putSerializable(KEY_BUILD_PLAYER_OBJECT, mBuildPlayer);
		super.onSaveInstanceState(outState);
//		Log.w(this.toString(), "onSaveInstanceState() called, outState = " + outState);
	}
	
	@Override
	protected void onDestroy() {
//		Timber.d("destroy", "onDestroy() called");
		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
//			Timber.d("destroy", "TTS Destroyed");
		}
		super.onDestroy();
	}
	
	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// is listener invalid?
		BuildPlayer player = (BuildPlayer)savedInstanceState.getSerializable(KEY_BUILD_PLAYER_OBJECT);
		if (player != null) {
			mBuildPlayer = player;
			mBuildPlayer.removeListeners();
			mBuildPlayer.registerListener(this);
		}
//		Log.w(this.toString(), "onRestoreInstanceState() called, player = " + player);
	}
	
	//=========================================================================
	// Callbacks from layout XML widgets
	//=========================================================================
	
	public void stopClicked(View view) {
		mBuildPlayer.stop();
		playButtonClick();
	}
	
	public void playPauseClicked(View view) {
		if (!mBuildPlayer.isPlaying())
			mBuildPlayer.play();
		else
			mBuildPlayer.pause();
		playButtonClick();
	}
	
	//=========================================================================
	// Seek bar callbacks
	//=========================================================================
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// stop timer from setting seekbar progress while user is dragging it
		mUserIsSeeking = true;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		mBuildPlayer.seekTo(seekBar.getProgress() * 1000);
		mUserIsSeeking = false;
	}

	//=========================================================================
	// Text to Speech finished initializing callback
	//=========================================================================
	
	@Override
	public void onInit(int status) {
		// text to speech engine ready
		mTtsReady = true;
		
		Locale currentLocale = Locale.getDefault();
		final String lang = currentLocale.getLanguage();
		
		// Do we currently have enough translated strings for the language?
		// TODO fix this shit... oh god.
		boolean haveTranslations = (
				lang.matches("en") ||
				lang.matches("fr") ||
				lang.matches("ru") ||
				lang.matches("pt"));

		if (haveTranslations) {
			int langAvailable = mTts.isLanguageAvailable(currentLocale); 
			if (langAvailable == TextToSpeech.LANG_AVAILABLE ||
				langAvailable == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
				langAvailable == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
				mTts.setLanguage(currentLocale);
				return;
			}
		}

		// TODO inform the user that TTS data isn't available for their language, and what
		// they can do to get it etc.
		mTts.setLanguage(Locale.US);
	}
	
	/** should be called only after TTS availability has been confirmed */
	private void initTTS() {
		mTts = new TextToSpeech(this, this);
	}

	//=========================================================================
	// User preferences callback
	//=========================================================================
	
	@Override
	/**
	 * if game speed is changed via preferences, update time multiplier
	 * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
	 */
	public void onSharedPreferenceChanged(SharedPreferences prefs,
			String key) {
		if (key.equals(SettingsActivity.KEY_GAME_SPEED)) {
			int choice = Integer.parseInt(prefs.getString(key, "4"));
			gameSpeedChanged(choice);
		} else if (key.equals(SettingsActivity.KEY_EARLY_WARNING)) {
			mBuildPlayer.setAlertOffset(prefs.getInt(key, 0)); 		// TODO: centralise default values
		} else if (key.equals(SettingsActivity.KEY_START_TIME)) {
//			Log.w(this.toString(), "start time changed");
			mBuildPlayer.setStartTime(prefs.getInt(key, 0)); 		// TODO: centralise default values
		}
	}
	
	private void gameSpeedChanged(int gameSpeedIndex) {
		mBuildPlayer.setTimeMultiplier(
				GameSpeeds.getMultiplierForGameSpeed(gameSpeedIndex, mBuild.getExpansion())
		);
		
		String speed = getResources().getStringArray(R.array.pref_game_speed_text)[gameSpeedIndex];
		Toast.makeText(this, String.format(getString(R.string.dlg_game_speed_alert), speed), Toast.LENGTH_LONG).show();
	}

	//=========================================================================
	// BuildPlayer callback methods follow
	//=========================================================================
	
	@Override
	public void onBuildThisNow(BuildItem item, int itemPos) {
		doVoiceAlert(item);
		mPendingAlerts.add(itemPos);
		if (mPendingAlerts.size() == 1) {
			handleVisualAlerts();
		}
	}
	
	private void doVoiceAlert(BuildItem item) {
		if (mTtsReady) {
			// TODO: check for custom speech attached to build item
//			String speech = "Build " + item.getCount() + " " + mDb.getNameString(item.getGameItemID()) + ".";
			String speech = this.getVoiceMessage(item);
			mTts.speak(speech, TextToSpeech.QUEUE_ADD, null);
		}
	}
	
	private void handleVisualAlerts() {
		if (mPendingAlerts.size() == 0)
			return;
		
		// get build item off the end of the queue
		int itemPos = mPendingAlerts.element();
		BuildItem item = mBuild.getItems().get(itemPos);
		
		mBuildListView.setSelection(itemPos);
		
		mOverlayText.setText(getTextMessage(item));
		mOverlayIcon.setImageResource(mDb.getLargeIcon(item.getGameItemID()));
		
        mOverlayContainer.setVisibility(View.VISIBLE);
		ObjectAnimator animation1 = ObjectAnimator.ofFloat(mOverlayContainer, "alpha", 0f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 0f);
	    animation1.setDuration(5000);

	    AnimatorSet set = new AnimatorSet();
	    set.playTogether(animation1);
//	    set.playTogether(animation1, animation2);
	    set.start();
	    set.addListener(new AnimatorListenerAdapter() {
	      @Override
	      public void onAnimationEnd(Animator animation) {
	    	  mPendingAlerts.remove();
//	          Log.w("test", "anim finished");
	          handleVisualAlerts();
	      }
	    });
	}

	@Override
	public void onBuildPlay() {
		mStopButton.setEnabled(true);
//		mPlayPauseButton.setText(PlaybackActivity.this.getString(R.string.playback_pause));
		Drawable replacer = getResources().getDrawable(R.drawable.pause_button_drawable);
		mPlayPauseButton.setImageDrawable(replacer);
		mPlayPauseButton.invalidate();
		setKeepScreenOn(true);
	}

	@Override
	public void onBuildPaused() {
//		mPlayPauseButton.setText(PlaybackActivity.this.getString(R.string.playback_play));
		Drawable replacer = getResources().getDrawable(R.drawable.play_button_drawable);
		mPlayPauseButton.setImageDrawable(replacer);
		mPlayPauseButton.invalidate();
	}

	@Override
	public void onBuildStopped() {
		mStopButton.setEnabled(false);
//		mPlayPauseButton.setText(PlaybackActivity.this.getString(R.string.playback_play));
		Drawable replacer = getResources().getDrawable(R.drawable.play_button_drawable);
		mPlayPauseButton.setImageDrawable(replacer);
		mPlayPauseButton.invalidate();
	}

	@Override
	public void onBuildResumed() {
//		mPlayPauseButton.setText(PlaybackActivity.this.getString(R.string.playback_pause));
		Drawable replacer = getResources().getDrawable(R.drawable.pause_button_drawable);
		mPlayPauseButton.setImageDrawable(replacer);
		mPlayPauseButton.invalidate();
	}

	@Override
	public void onBuildFinished() {
		// TODO: need to localise
		if (mTtsReady) {
			mTts.speak("Build order finished.", TextToSpeech.QUEUE_ADD, null);
		}
		setKeepScreenOn(false);
		trackPlaybackFinished();
	}

	@Override
	public void onIterate(long newGameTime) {
//		Log.w(this.toString(), "onIterate() called with " + newGameTime);
		long timeSec = newGameTime / 1000;
	
		// TODO: extract time formatting helper function
		mTimerText.setText(String.format("%02d:%02d", timeSec / 60, timeSec % 60));
	//	mTimerText.setText(String.valueOf(mCurrentGameTime/1000.0));
		if (!mUserIsSeeking) {
			mSeekBar.setProgress((int)timeSec);
		}
	}
	
	//=========================================================================
	// Helpers
	//=========================================================================

	private void playButtonClick() {
		MediaPlayer mp = MediaPlayer.create(this, R.raw.replay_click);
		if (mp != null)
			mp.start();
	}
	
	private void setKeepScreenOn(boolean flag) {
		if (mBuildListView != null)
			mBuildListView.setKeepScreenOn(flag);
	}
	
	/**
	 * Returns the text message that should be spoken to the user to alert them
	 * of this upcoming build item
	 */
	private String getVoiceMessage(BuildItem item) {
		return getAlertMessage(item, true);
	}
	
	/**
	 * Returns the text message that should be shown to the user to alert them
	 * of this upcoming build item
	 */
	private String getTextMessage(BuildItem item) {
		return getAlertMessage(item, false);
	}
	
	/**
	 * Gets the text message that should be shown or spoken to the user to alert them
	 * of an upcoming build item. Handles some minor differences between voice and
	 * text messages.
	 * 
	 * @param item
	 * @param voice whether the string returned should be displayed or spoken with TTS
	 * @return alert message
	 */
	private String getAlertMessage(BuildItem item, boolean voice) {
		if (!voice && item.getVoice() != null)
			return item.getVoice();
		
		// if no voice string specified, prefer to use text string
		if (item.getText() != null)
			return item.getText();
		
		// if neither text nor voice string given, generate a message
		String itemName = mDb.getNameString(item.getGameItemID());
		
		// e.g. build/train/research
		// TEMPORARY: debugging user device to get more context on a known NPE
		String verb = "";
		try {
			verb = mDb.getVerbForItem(item.getGameItemID());
		} catch (Exception e) {
			// Report this error for analysis
			String err = "getVerbForItem() in getAlertMessage() threw exception " + e.getMessage() + 
					" for item " + item + " in build " + mBuild + " playback time=" +
					mTimerText.getText().toString();

			EasyTracker.getInstance(this).send(
					MapBuilder.createException(
							new StandardExceptionParser(this, null)
									.getDescription(Thread.currentThread().getName(), e),
							false)    // False indicates a nonfatal exception
							.build());

    		return itemName;	// compromise to prevent crash: just say item name
		}
		
        Map<String, String> args = new HashMap<String, String>();
        args.put("item", itemName);
        args.put("verb", verb);
        args.put("count", ""+item.getCount() + (!voice ? "x" : ""));
        args.put("target", mDb.getNameString(item.getTarget()));
        
        String template;
        ItemType itemType = mDb.getItemType(item.getGameItemID());
        final boolean multipleItems = item.getCount() > 1;
        if (itemType == ItemType.STRUCTURE) {
        	if (item.getTarget() == null)
        		template = getString(multipleItems ? R.string.sentence_structure_plural_no_target : R.string.sentence_structure_singular_no_target);
        	else
        		template = getString(multipleItems ? R.string.sentence_structure_plural_with_target : R.string.sentence_structure_singular_with_target);
        } else if (itemType == ItemType.ABILITY) {
        	if (item.getTarget() == null)
        		template = getString(R.string.sentence_ability_no_target);
        	else
        		template = getString(R.string.sentence_ability_with_target);
        } else if (itemType == ItemType.UPGRADE) {
        	template = getString(R.string.sentence_upgrade);
        } else if (itemType == ItemType.UNIT) {
        	template = getString(multipleItems ? R.string.sentence_unit_plural : R.string.sentence_unit_singular);
        } else {
        	// probably a NOTE with no text message, fallback to item name
        	return itemName;
        }
        
        return MapFormat.format(template, args);
	}
	
	/**
	 * Send info about this playback to Google Analytics as it's of interest
	 * which builds are being viewed and which aren't
	 */
	private void trackPlaybackView() {
		EasyTrackerUtils.sendEvent(this,
				"playback_view",
				mBuild.getExpansion().toString() + "_" + mBuild.getFaction().toString(),
				mBuild.getName(),
				null
		);
	}

	private void trackPlaybackFinished() {
		EasyTrackerUtils.sendEvent(this,
				"playback_finished",
				mBuild.getExpansion().toString() + "_" + mBuild.getFaction().toString(),
				mBuild.getName(),
				null
		);
	}
}
