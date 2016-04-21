package com.kiwiandroiddev.sc2buildassistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.ItemType;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Dialog for editing a single build item. Lets the user select the unit/upgrade/ability,
 * count, time and optional text and voice messages.
 * 
 * @author matt
 *
 */
public class EditBuildItemActivity extends AppCompatActivity implements OnClickListener {
	
	public static final int EDIT_BUILD_ITEM_REQUEST = 0;
	
	// identify which build item is being edited for the benefit of the calling activity
	public static final String KEY_INCOMING_BUILD_ITEM_ID = "IncomingBuildItemID";
	public static final String KEY_DEFAULT_TIME = "DefaultTime";
	
	// for internal use - saving and restoring state
	private static final String KEY_MAIN_ITEM_ID = "MainItemID";
	private static final String KEY_TARGET_ITEM_ID = "TargetItemID";
	
	private static final int NO_ITEM_ICON = R.drawable.stat_notify_disabled;
	
	private Faction mFaction;		// current faction for the build order, used to limit unit selection
	
	// whether this dialog is for editing an existing build item or making a new one
	// (modifies its behaviour slightly)
	private boolean mHaveShownInitialSelector = false;	// has initial unit selector dialog been opened once?
	
	private Long mIncomingItemID;
	private String mMainItemID;			// required
	private String mTargetItemID;		// can be none/null (build item might not need a target)

    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.dlg_unit_button) ImageButton mUnitButton;
    @InjectView(R.id.dlg_target_button) ImageButton mTargetButton;
    @InjectView(R.id.dlg_clear_target_button) Button mClearTargetButton;
    @InjectView(R.id.dlg_minutes) EditText mMinutes;
    @InjectView(R.id.dlg_seconds) EditText mSeconds;
    @InjectView(R.id.dlg_amount) EditText mCount;
    @InjectView(R.id.dlg_custom_text) EditText mCustomText;
    @InjectView(R.id.dlg_custom_speech) EditText mCustomSpeech;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_edit_build_item);
        ButterKnife.inject(this);

		setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.dlg_edit_item_title);

        mUnitButton.setOnClickListener(this);
        mTargetButton.setOnClickListener(this);
        
        if (savedInstanceState == null) {
        	Bundle extras = getIntent().getExtras();
        	
        	if (extras.containsKey(KEY_INCOMING_BUILD_ITEM_ID))
        		mIncomingItemID = extras.getLong(KEY_INCOMING_BUILD_ITEM_ID);
        	
	        BuildItem item;
	        if (extras.containsKey(IntentKeys.KEY_BUILD_ITEM_OBJECT)) {
	        	item = (BuildItem) extras.getSerializable(IntentKeys.KEY_BUILD_ITEM_OBJECT);
	        } else {
	        	item = new BuildItem();
	        	item.setTime(extras.getInt(KEY_DEFAULT_TIME, 0));
	        }
	        	
	        mMinutes.setText("" + item.getTime() / 60);
	        mSeconds.setText("" + item.getTime() % 60);
	        mCount.setText("" + item.getCount());
	        mCustomText.setText(item.getText());
	        mCustomSpeech.setText(item.getVoice());
	        mMainItemID = item.getGameItemID();
	        mTargetItemID = item.getTarget();
	        
        	mFaction = (Faction) extras.getSerializable(IntentKeys.KEY_FACTION_ENUM);
        } else {
        	if (savedInstanceState.containsKey(KEY_INCOMING_BUILD_ITEM_ID))
        		mIncomingItemID = savedInstanceState.getLong(KEY_INCOMING_BUILD_ITEM_ID);
        		
        	mMainItemID = savedInstanceState.getString(KEY_MAIN_ITEM_ID);
        	mTargetItemID = savedInstanceState.getString(KEY_TARGET_ITEM_ID);
        	mFaction = (Faction) savedInstanceState.getSerializable(IntentKeys.KEY_FACTION_ENUM);
        	
        	// if we're restoring a previous instance, we must have shown the initial selector already
        	mHaveShownInitialSelector = true;
        }
        
        DbAdapter db = ((MyApplication) getApplicationContext()).getDb();
        mUnitButton.setImageResource(db.getSmallIcon(mMainItemID));
        if (mTargetItemID == null) {
        	mTargetButton.setImageResource(NO_ITEM_ICON);
        	mClearTargetButton.setEnabled(false);
        } else {
        	mTargetButton.setImageResource(db.getSmallIcon(mTargetItemID));
        }

        // Show the unit selector dialog immediately if we're creating a new item, since it's required
        if (mMainItemID == null && !mHaveShownInitialSelector) {
        	showUnitSelector(R.id.dlg_unit_button, null);
        	mHaveShownInitialSelector = true;
        }
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
	public void onSaveInstanceState(Bundle outState) {
		if (mIncomingItemID != null)
			outState.putLong(KEY_INCOMING_BUILD_ITEM_ID, mIncomingItemID);
		
		outState.putSerializable(KEY_MAIN_ITEM_ID, mMainItemID);
		outState.putSerializable(KEY_TARGET_ITEM_ID, mTargetItemID);
		outState.putSerializable(IntentKeys.KEY_FACTION_ENUM, mFaction);
		super.onSaveInstanceState(outState);
	}

    @OnClick(R.id.dlg_clear_target_button)
    public void clearButtonClicked() {
        mTargetButton.setImageResource(NO_ITEM_ICON);
        mTargetItemID = null;
        mClearTargetButton.setEnabled(false);
    }

    @OnClick(R.id.cancelButton)
    public void cancelButtonClicked() {
        finish();
    }

	/** 
	 * Tries to create and return (via activity result) a BuildItem object from the input fields of the dialog.
	 * If any required inputs are missing, or other input is invalid, the user will be
	 * alerted and the activity will remain open.
	 */
    @OnClick(R.id.okButton)
	public void buildResult() {
		BuildItem result = new BuildItem();
		
		try {
			// TODO: restrict count to positive numbers
			result.setCount(getIntFromEditText(mCount, 1));
		} catch (NumberFormatException e) {
			Toast.makeText(this, R.string.edit_build_invalid_count_message, Toast.LENGTH_SHORT).show();
			return;
		}
		
		try {
			// TODO: restrict time values to 0-60 range
			int minutes = getIntFromEditText(mMinutes, 0);
			int seconds = getIntFromEditText(mSeconds, 0);
			int time = (minutes * 60) + seconds;
			result.setTime(time);
		} catch (NumberFormatException e) {
			Toast.makeText(this, R.string.edit_build_invalid_time_message, Toast.LENGTH_SHORT).show();
			return;
		}
		
		result.setText(mCustomText.getText().toString().matches("") ? null : mCustomText.getText().toString());
		result.setVoice(mCustomSpeech.getText().toString().matches("") ? null : mCustomSpeech.getText().toString());
		result.setUnit(mMainItemID);
		result.setTarget(mTargetItemID);
		
		// finish activity and return build as result
		Intent data = new Intent();
		data.putExtra(IntentKeys.KEY_BUILD_ITEM_OBJECT, result);
		// tell the caller which build item of theirs we're returning
		if (mIncomingItemID != null)
			data.putExtra(KEY_INCOMING_BUILD_ITEM_ID, mIncomingItemID);
		setResult(RESULT_OK, data);
		finish();
	}
	
	// Helpers
	private int getIntFromEditText(EditText edit, int defaultIfNull) throws NumberFormatException {
//		Timber.d(this.toString(), "edit.getText() = " + edit.getText());
		String text = edit.getText().toString();
		if (text.matches("")) {
//			Timber.d(this.toString(), "  text is empty, returning " + defaultIfNull);
			return defaultIfNull;
		}
		
		int value = Integer.parseInt(text);
		return value;
	}

	@Override
	public void onClick(View v) {
		DbAdapter db = ((MyApplication) getApplicationContext()).getDb();
		switch (v.getId()) {
		case R.id.dlg_unit_button:
//			Timber.d(this.toString(), "unit button click");
			showUnitSelector(R.id.dlg_unit_button, db.getItemType(mMainItemID));	// stub
			break;
		case R.id.dlg_target_button:
//			Timber.d(this.toString(), "target button click");
			showUnitSelector(R.id.dlg_target_button, db.getItemType(mTargetItemID));
			break;
//		default:
//			Timber.d(this.toString(), "Error: got unknown onClick() event in BuildItemDialog, view = " + v);
		}
	}
	
	/** Starts the Unit Selector activity, allowing the user to choose a unit/upgrade/ability/etc.
	 * 
	 * @param callerId  identifies the source, in case there is more than one place where the calling
	 * 		activity that might want to use a unit selector. This id is then returned in the data bundle
	 * 		in onActivityResult()
	 */
	private void showUnitSelector(int callerId, ItemType defaultTab) {
        Intent i = new Intent(this, UnitSelectorActivity.class);
        i.putExtra(IntentKeys.KEY_FACTION_ENUM, mFaction);
        i.putExtra(UnitSelectorActivity.KEY_CALLER_ID, callerId);
        if (defaultTab != null) {
            i.putExtra(UnitSelectorActivity.KEY_DEFAULT_ITEM_TYPE, defaultTab);
        }
        startActivityForResult(i, UnitSelectorActivity.PICK_ITEM_REQUEST);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // Check which request we're responding to
	    if (requestCode == UnitSelectorActivity.PICK_ITEM_REQUEST) {	    	
	        // Make sure the request was successful
	        if (resultCode == UnitSelectorActivity.RESULT_OK) {
	        	int callerId = data.getExtras().getInt(UnitSelectorActivity.KEY_CALLER_ID);
	        	long itemId = data.getExtras().getLong(UnitSelectorActivity.KEY_RESULT_ITEM_ID);
	        	
//	        	Timber.d(this.toString(), "got RESULT_OK in BuildItemDialog.onActivityResult(), callerId = " + callerId + ", itemId = " + itemId);
	        	
	            DbAdapter db = ((MyApplication) getApplicationContext()).getDb();
	        	
	        	switch (callerId) {
	        	case R.id.dlg_unit_button:
	        		mMainItemID = db.getItemUniqueName(itemId);
	        		mUnitButton.setImageResource(db.getSmallIcon(mMainItemID));
	        		break;
	        	case R.id.dlg_target_button:
	        		mTargetItemID = db.getItemUniqueName(itemId);
	        		mTargetButton.setImageResource(db.getSmallIcon(mTargetItemID));
	        		mClearTargetButton.setEnabled(true);
	        		break;
	        	default:
	        		throw new RuntimeException("Unknown callerID in BuildItemDialog.onActivityResult()!");
	        	}
	        } else {
	        	// cancelled - do nothing
//	        	Timber.d(this.toString(), "got cancelled result in BuildItemDialog.onActivityResult()");
	        	
	        	// if no choice was made for a new build item, quit this item editor activity
	        	if (mMainItemID == null) {
	        		setResult(RESULT_CANCELED);
	        		this.finish();
	        	}
	        }
	    }
	}
}
