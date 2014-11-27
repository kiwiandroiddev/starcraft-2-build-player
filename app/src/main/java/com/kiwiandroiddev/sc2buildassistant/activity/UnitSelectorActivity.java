package com.kiwiandroiddev.sc2buildassistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.RaceFragment;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Faction;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.ItemType;
import com.kiwiandroiddev.sc2buildassistant.adapter.ItemTypePagerAdapter;

//import com.google.analytics.tracking.android.EasyTracker;

/**
 * UI for allowing the user to select any item (unit, structure, upgrade, etc.) of a
 * a specific faction. The selected item (if any) is returned via the activity's result.
 * 
 * @author matt
 *
 */
public class UnitSelectorActivity extends ActionBarActivity {
	
	public final static int PICK_ITEM_REQUEST = 1;	// request code when getactivityforresult() is called
	public final static int RESULT_NONE = 90;			// used to say that the user selected "no item" to calling activity
	
	public static final String KEY_CALLER_ID = "CallerID";	// used to differentiate between sources in the calling activity
																// e.g. if there are 2 unit selector buttons
	public static final String KEY_DEFAULT_ITEM_TYPE = "DefaultItemType";		// initial tab to have selected (unit/structure/upgrade...)
	public static final String KEY_RESULT_ITEM_ID = "ItemID";		// key for item ID (int) selected by user using this dialog
	
	private DbAdapter.Faction mFactionFilter;				// only show units from this side in the selector dialog
	private ItemTypePagerAdapter mPagerAdapter;
	private ViewPager mPager;
	private int mCallerID = -1;							// see KEY_CALLER_ID comment
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dialog_item_selector);
		this.setTitle(R.string.dlg_select_item_title);
		
		ItemType defaultItemType = null;
        if (savedInstanceState == null) {
        	mFactionFilter = (Faction) getIntent().getExtras().getSerializable(RaceFragment.KEY_FACTION_ENUM);
        	if (getIntent().getExtras().containsKey(KEY_CALLER_ID)) {
        		mCallerID = getIntent().getExtras().getInt(KEY_CALLER_ID);
        	}
        	if (getIntent().getExtras().containsKey(KEY_DEFAULT_ITEM_TYPE)) {
        		defaultItemType = (ItemType) getIntent().getExtras().getSerializable(KEY_DEFAULT_ITEM_TYPE);
        	}
        } else {
        	mFactionFilter = (Faction) savedInstanceState.getSerializable(RaceFragment.KEY_FACTION_ENUM);
        	if (savedInstanceState.containsKey(KEY_CALLER_ID)) {
        		mCallerID = savedInstanceState.getInt(KEY_CALLER_ID);
        	}
        	if (savedInstanceState.containsKey(KEY_DEFAULT_ITEM_TYPE)) {
        		defaultItemType = (ItemType) savedInstanceState.getSerializable(KEY_DEFAULT_ITEM_TYPE);
        	}
        }
        
        Button cancelButton = (Button) findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				UnitSelectorActivity.this.finish();
			}
		});
        
        // Set up view pager
        mPager = (ViewPager) findViewById(R.id.pager);
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
//    	EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
    	super.onStop();
//    	EasyTracker.getInstance().activityStop(this);
    }
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(RaceFragment.KEY_FACTION_ENUM, mFactionFilter);
		super.onSaveInstanceState(outState);
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
