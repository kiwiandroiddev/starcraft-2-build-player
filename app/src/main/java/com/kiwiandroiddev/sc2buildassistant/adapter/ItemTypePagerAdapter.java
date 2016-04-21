package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.UnitSelectorFragment;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.ItemType;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction;

/**
 * Manages unit selector fragments for each item type (structure, unit, ability etc.)
 * 
 * @author matt
 *
 */
public class ItemTypePagerAdapter extends FragmentPagerAdapter {

	private Context mContext;
	private Faction mFactionFilter;

	public ItemTypePagerAdapter(FragmentManager fm, Context context, Faction factionFilter) {
		super(fm);
		mContext = context;
		mFactionFilter = factionFilter;
	}

	@Override
	public Fragment getItem(int position) {
		Fragment tab = new UnitSelectorFragment();
		ItemType itemType = ItemType.values()[position];
		
		Bundle data = new Bundle();
		data.putSerializable(IntentKeys.KEY_FACTION_ENUM, mFactionFilter);
		data.putSerializable(IntentKeys.KEY_ITEM_TYPE_ENUM, itemType);
		tab.setArguments(data);
		
//		Timber.d(this.toString(), "in getItem(), tab = " + tab);
		
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
