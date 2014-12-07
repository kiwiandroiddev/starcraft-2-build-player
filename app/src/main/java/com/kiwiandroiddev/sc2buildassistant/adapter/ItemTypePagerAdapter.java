package com.kiwiandroiddev.sc2buildassistant.adapter;

import com.kiwiandroiddev.sc2buildassistant.activity.fragment.RaceFragment;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.UnitSelectorFragment;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.ItemType;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Manages unit selector fragments for each item type (structure, unit, ability etc.)
 * 
 * @author matt
 *
 */
public class ItemTypePagerAdapter extends FragmentPagerAdapter {

	private Context mContext;
	private DbAdapter.Faction mFactionFilter;

	public ItemTypePagerAdapter(FragmentManager fm, Context context, DbAdapter.Faction factionFilter) {
		super(fm);
		mContext = context;
		mFactionFilter = factionFilter;
	}

	@Override
	public Fragment getItem(int position) {
		Fragment tab = new UnitSelectorFragment();
		DbAdapter.ItemType itemType = ItemType.values()[position];
		
		Bundle data = new Bundle();
		data.putSerializable(RaceFragment.KEY_FACTION_ENUM, mFactionFilter);
		data.putSerializable(RaceFragment.KEY_ITEM_TYPE_ENUM, itemType);
		tab.setArguments(data);
		
//		Log.d(this.toString(), "in getItem(), tab = " + tab);
		
		return tab;
	}

	@Override
	public int getCount() {
		return ItemType.values().length;
	}

	@Override
    public CharSequence getPageTitle(int position) {
		return mContext.getString(DbAdapter.getItemTypeName(ItemType.values()[position]));
    }
}
