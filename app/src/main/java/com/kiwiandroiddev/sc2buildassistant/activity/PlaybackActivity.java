package com.kiwiandroiddev.sc2buildassistant.activity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import android.content.Intent;
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
import android.support.v7.app.ActionBarActivity;
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
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

//import com.google.analytics.tracking.android.EasyTracker;
//import com.google.analytics.tracking.android.Tracker;
import com.kiwiandroiddev.sc2buildassistant.BuildPlayer;
import com.kiwiandroiddev.sc2buildassistant.BuildPlayerEventListener;
import com.kiwiandroiddev.sc2buildassistant.MapFormat;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.RaceFragment;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.ItemType;
import com.kiwiandroiddev.sc2buildassistant.adapter.BuildItemAdapter;
import com.kiwiandroiddev.sc2buildassistant.model.Build;
import com.kiwiandroiddev.sc2buildassistant.model.BuildItem;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

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
public class PlaybackActivity extends ActionBarActivity implements OnSeekBarChangeListener, OnInitListener,
														  OnSharedPreferenceChangeListener, BuildPlayerEventListener {
	
	public static int MY_DATA_CHECK_CODE = 0;
	
	// StarCraft 2 game speed reference: http://wiki.teamliquid.net/starcraft2/Game_Speed
	public static final double SLOWER_FACTOR = 0.6;
	public static final double SLOW_FACTOR = 0.8;
	public static final double NORMAL_FACTOR = 1.0;
	public static final double FAST_FACTOR = 1.2;
	public static final double FASTER_FACTOR = 1.4;
	
	public static final int TIME_STEP = 100;	// milliseconds between updates
	
	private Build mBuild;	// build order to play back, passed in by main activity
	private BuildPlayer mBuildPlayer;
	private DbAdapter mDb;
	private Handler mHandler = new Handler();
	private Queue<Integer> mPendingAlerts;		// values are indices of build items in the build
    private double[] mIndexToMultiplier = { SLOWER_FACTOR, SLOW_FACTOR, NORMAL_FACTOR, FAST_FACTOR, FASTER_FACTOR };
	
	// references to widgets, so we don't have to look them up by ID constantly
    private ListView mBuildListView;
//	private Spinner mSpinner;
	private TextView mMaxTimeText;
	private TextView mTimerText;
	private ImageButton mPlayPauseButton;
	private ImageButton mStopButton;
	private SeekBar mSeekBar;
	private ImageView mOverlayIcon;
	private View mOverlayContainer;
	private View mTimerTextContainer;
	private TextView mOverlayText;
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
//        Log.w(this.toString(), "onCreate() called, savedInstanceState = " + savedInstanceState);
//        setTheme(android.R.style.Theme_Black_NoTitleBar_Fullscreen);
//		setTheme(R.style.Theme_Sherlock);
        setContentView(R.layout.activity_playback);
       
//        getSupportActionBar().hide();
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
        
        // find widgets, assign them to member variables
        mPlayPauseButton = (ImageButton)findViewById(R.id.playPauseButton);
        mStopButton = (ImageButton)findViewById(R.id.stopButton);
        mTimerText = (TextView)mTimerTextContainer.findViewById(R.id.timerText);
        mMaxTimeText = (TextView)mTimerTextContainer.findViewById(R.id.maxTimeText);
        mOverlayIcon = (ImageView)findViewById(R.id.overlayIcon);
        mSeekBar = (SeekBar)findViewById(R.id.seekBar);
        mOverlayContainer = findViewById(R.id.overlayContainer);
        mOverlayContainer.setVisibility(View.INVISIBLE);
        mOverlayText = (TextView)findViewById(R.id.overlayText);
        
        // check for text-to-speech availablility, try to init engine
        checkForTTS();

        // fetch the build object from database associated with ID we were passed
        long buildId = getIntent().getExtras().getLong(RaceFragment.KEY_BUILD_ID);	// stub
        DbAdapter db = new DbAdapter(this);
        db.open();
        mBuild = db.fetchBuild(buildId);
        db.close();
        
        // create build player object from build passed in intent
        if (savedInstanceState != null && savedInstanceState.getSerializable("BuildPlayer") != null) {
        	BuildPlayer savedPlayer = (BuildPlayer)savedInstanceState.getSerializable("BuildPlayer");
//        	Log.w(this.toString(), "saved buildplayer found, player = " + savedPlayer + ", numListeners = " + savedPlayer.getNumListeners());
        	mBuildPlayer = savedPlayer;
        } else {
        	mBuildPlayer = new BuildPlayer(mBuild.getItems());
        }
        mBuildPlayer.registerListener(this);
        
        int buildDuration = mBuildPlayer.getDuration();
        mMaxTimeText.setText(String.format("%02d:%02d", buildDuration / 60, buildDuration % 60));
        
        mSeekBar.setMax(buildDuration);	// NOTE: progress bar units are seconds!
        mSeekBar.setOnSeekBarChangeListener(this);
        
        // populate list view with build items
        BuildItemAdapter bAdapter = new BuildItemAdapter(this,
        		R.layout.build_item_row, mBuild.getItems());
        mBuildListView = (ListView)findViewById(R.id.buildListView);
        mBuildListView.setAdapter(bAdapter);
        
        // keep screen from sleeping until the build has played through
        setKeepScreenOn(true);
        
        // Initialize spinner to choose playback speed (Slower, Slow, Normal, Fast, Faster)
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        Log.w(this.toString(), "sharedPref identity = " + sharedPref.toString());
        sharedPref.registerOnSharedPreferenceChangeListener(this);		// notify this activity when settings change
        		
		// hacky: initialize buildplayer with early warning preference
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
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.options_menu, menu);
       return true;
    }
    
    /* handle "Up" button press on title bar, navigate back to briefing screen */
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
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed
                // in the Action Bar.
                finish();
//                return true;
        }

    	// use the same options menu as the main activity
    	boolean result = MainActivity.OnMenuItemSelected(this, item);
    	if (!result)
    		return super.onOptionsItemSelected(item);
    	else
    		return true;
    }
	
    /*
     * Gets result of checking text to speech engine availability
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == MY_DATA_CHECK_CODE) {
	        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
	            // success, create the TTS instance
	            initTTS();
	        } else {
	            // do nothing, user is prompted in main activity to install package
	        }
	    }
	}
	
	//=========================================================================
	// Android life cycle methods
	//=========================================================================
	
    @Override
    public void onStart() {
    	super.onStart();
//    	EasyTracker.getInstance().activityStart(this);
    }

	@Override
	public void onStop() {
//		Log.w(this.toString(), "onStop() called, mBuildPlayer = " + mBuildPlayer);
		super.onStop();
		mHandler.removeCallbacks(mUpdateTimeTask);
//    	EasyTracker.getInstance().activityStop(this);
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
		outState.putSerializable("BuildPlayer", mBuildPlayer);
		super.onSaveInstanceState(outState);
//		Log.w(this.toString(), "onSaveInstanceState() called, outState = " + outState);
	}
	
	@Override
	protected void onDestroy() {
//		Log.d("destroy", "onDestroy() called");
		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
//			Log.d("destroy", "TTS Destroyed");
		}
		super.onDestroy();
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// is listener invalid?
		BuildPlayer player = (BuildPlayer)savedInstanceState.getSerializable("BuildPlayer");
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
		boolean haveTranslations = (lang.matches("en") || lang.matches("fr"));
				
		if (haveTranslations) {
			int langAvailable = mTts.isLanguageAvailable(currentLocale); 
			if (langAvailable == TextToSpeech.LANG_AVAILABLE ||
				langAvailable == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
				mTts.setLanguage(currentLocale);
				return;
			}
		}
		
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
	
	private void gameSpeedChanged(int index) {
		mBuildPlayer.setTimeMultiplier(mIndexToMultiplier[index]);
		String speed = getResources().getStringArray(R.array.pref_game_speed_text)[index];
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

	// TODO: reuse method with same name in BuildListActivity
	private void checkForTTS() {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
	        Intent checkIntent = new Intent();
	        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
	        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
		} else {
			// don't check, just go ahead and init the engine which *should* already be available
			initTTS();
		}
	}
	
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
//    		EasyTracker.getInstance().setContext(this);
//    		Tracker myTracker = EasyTracker.getTracker();       // Get a reference to tracker.
//    		myTracker.sendException(err, false);    // false indicates non-fatal exception.
    		return itemName;	// compromise to prevent crash: just say item name
		}
		
        Map<String, String> args = new HashMap<String, String>();
        args.put("item", itemName);
        args.put("verb", verb);
        args.put("count", ""+item.getCount() + (!voice ? "x" : ""));
        args.put("target", mDb.getNameString(item.getTarget()));
        
        String template;
        DbAdapter.ItemType itemType = mDb.getItemType(item.getGameItemID());
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
//    	EasyTracker.getInstance().setContext(this);
//    	EasyTracker.getTracker().sendEvent("playback_view", mBuild.getExpansion().toString() + "_" + mBuild.getFaction().toString(), mBuild.getName(), null);
	}
	
	private void trackPlaybackFinished() {
//		EasyTracker.getInstance().setContext(this);
//    	EasyTracker.getTracker().sendEvent("playback_finished", mBuild.getExpansion().toString() + "_" + mBuild.getFaction().toString(), mBuild.getName(), null);
	}
}
