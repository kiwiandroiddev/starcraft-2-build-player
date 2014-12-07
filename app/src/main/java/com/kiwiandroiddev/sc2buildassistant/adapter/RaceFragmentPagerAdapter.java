package com.kiwiandroiddev.sc2buildassistant.adapter;

import com.kiwiandroiddev.sc2buildassistant.activity.fragment.RaceFragment;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Expansion;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
//import android.util.Log;

/**
 * Manages fragments for each of the 3 factions so that they can be displayed
 * in a ViewPager. Each fragment shows available build orders for that faction.
 * 
 * @author matt
 *
 */
public class RaceFragmentPagerAdapter extends FragmentPagerAdapter {
	
	private DbAdapter.Expansion mCurrentExpansion;		// pass this onto RaceFragments when created
	private Context mContext;				// needed so we can get locale-independent strings
	
	public RaceFragmentPagerAdapter(FragmentManager manager, Context context) {
		super(manager);
		mContext = context;
		mCurrentExpansion = Expansion.WOL;
	}

	/** This method will be invoked when a page is requested to create */
	@Override
	public Fragment getItem(int position) {
		DbAdapter.Faction race = DbAdapter.Faction.values()[position];
		RaceFragment tab = new RaceFragment();
		
		// TODO keeping this expansion-value passing for now, would like to remove soon
		Bundle data = new Bundle();
		data.putSerializable(RaceFragment.KEY_FACTION_ENUM, race);
		data.putSerializable(RaceFragment.KEY_EXPANSION_ENUM, mCurrentExpansion);
		tab.setArguments(data);
		
		return tab;
	}

	/** Returns the number of pages */
	@Override
	public int getCount() {		
		//return DbAdapter.Faction.values().length;
		return 3;	// for performance
	}
	
	@Override
    public CharSequence getPageTitle(int position) {
		// get faction corresponding to tab position
		final DbAdapter.Faction faction = DbAdapter.Faction.values()[position];
        return mContext.getString(DbAdapter.getFactionName(faction));
    }
	
	public void setCurrentExpansion(DbAdapter.Expansion expansion) {
		mCurrentExpansion = expansion;
	}
	
	public DbAdapter.Expansion getCurrentExpansion() {
		return mCurrentExpansion;
	}
}
