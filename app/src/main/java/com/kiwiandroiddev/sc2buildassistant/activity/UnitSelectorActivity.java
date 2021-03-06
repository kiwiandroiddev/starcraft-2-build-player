package com.kiwiandroiddev.sc2buildassistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.google.analytics.tracking.android.EasyTracker;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.adapter.ItemTypePagerAdapter;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.ItemType;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * UI for allowing the user to select any item (unit, structure, upgrade, etc.) of a
 * a specific faction. The selected item (if any) is returned via the activity's result.
 * 
 * @author matt
 *
 */
public class UnitSelectorActivity extends AppCompatActivity {
	
	public final static int PICK_ITEM_REQUEST = 1;	// request code when getactivityforresult() is called
	public final static int RESULT_NONE = 90;			// used to say that the user selected "no item" to calling activity
	
	public static final String KEY_CALLER_ID = "CallerID";	// used to differentiate between sources in the calling activity
																// e.g. if there are 2 unit selector buttons
	public static final String KEY_DEFAULT_ITEM_TYPE = "DefaultItemType";		// initial tab to have selected (unit/structure/upgrade...)
	public static final String KEY_RESULT_ITEM_ID = "ItemID";		// key for item ID (int) selected by user using this dialog
	
	private Faction mFactionFilter;				// only show units from this side in the selector dialog
    private int mCallerID = -1;							// see KEY_CALLER_ID comment

    private ItemTypePagerAdapter mPagerAdapter;

	@BindView(R.id.pager) ViewPager mPager;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_item_selector);
        ButterKnife.bind(this);

		setTitle(R.string.dlg_select_item_title);

		ItemType defaultItemType = null;
        if (savedInstanceState == null) {
        	mFactionFilter = (Faction) getIntent().getExtras().getSerializable(IntentKeys.KEY_FACTION_ENUM);
        	if (getIntent().getExtras().containsKey(KEY_CALLER_ID)) {
        		mCallerID = getIntent().getExtras().getInt(KEY_CALLER_ID);
        	}
        	if (getIntent().getExtras().containsKey(KEY_DEFAULT_ITEM_TYPE)) {
        		defaultItemType = (ItemType) getIntent().getExtras().getSerializable(KEY_DEFAULT_ITEM_TYPE);
        	}
        } else {
        	mFactionFilter = (Faction) savedInstanceState.getSerializable(IntentKeys.KEY_FACTION_ENUM);
        	if (savedInstanceState.containsKey(KEY_CALLER_ID)) {
        		mCallerID = savedInstanceState.getInt(KEY_CALLER_ID);
        	}
        	if (savedInstanceState.containsKey(KEY_DEFAULT_ITEM_TYPE)) {
        		defaultItemType = (ItemType) savedInstanceState.getSerializable(KEY_DEFAULT_ITEM_TYPE);
        	}
        }

        // Set up view pager
        mPagerAdapter = new ItemTypePagerAdapter(getSupportFragmentManager(), this, mFactionFilter);		// stub
        mPager.setAdapter(mPagerAdapter);
        
        // if we received a default item type, show that tab now
        if (defaultItemType != null) {
        	mPager.setCurrentItem(defaultItemType.ordinal());
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
		outState.putSerializable(IntentKeys.KEY_FACTION_ENUM, mFactionFilter);
		super.onSaveInstanceState(outState);
	}

    @OnClick(R.id.cancelButton)
    public void cancelButtonClicked() {
        finish();
    }

	/** 
	 * Fragments should use this instead of setResult() to ensure that the
	 * caller ID is returned to the calling activity
	 * 
	 * @param resultCode
	 * @param data
	 */
	public void setCustomResult(int resultCode, Intent data) {
		if (data == null)
			data = new Intent();
		
		if (!data.hasExtra(KEY_CALLER_ID) && mCallerID != -1)
			data.putExtra(KEY_CALLER_ID, mCallerID);
		
		super.setResult(resultCode, data);
	}
	
	public void setCustomResult(int resultCode) {
		setCustomResult(resultCode, new Intent());
	}
}
