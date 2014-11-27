package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.kiwiandroiddev.sc2buildassistant.model.Build;
import com.kiwiandroiddev.sc2buildassistant.model.BuildItem;
import com.kiwiandroiddev.sc2buildassistant.EditBuildInfoFragment;
import com.kiwiandroiddev.sc2buildassistant.EditBuildItemsFragment;
import com.kiwiandroiddev.sc2buildassistant.EditBuildNotesFragment;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.RaceFragment;
import com.kiwiandroiddev.sc2buildassistant.UnoptimizedDeepCopy;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Faction;

import java.util.ArrayList;

/**
 * Manages the 3 build editor fragments (info, notes and items) for a ViewPager
 * so they can be swiped between by the user. 
 * 
 * @author matt
 *
 */
public class EditBuildPagerAdapter extends FragmentPagerAdapter {

	private Context mContext;
	private Build mBuildToEdit;
	
	/**
	 * 
	 * @param fm fragment manager
	 * @param buildToEdit - null if user is creating a new build
	 * @param initialExpansion - null if user is editing an existing build
	 * @param initialFaction - null if user is editing an existing build
	 */
	public EditBuildPagerAdapter(FragmentManager fm, Context context, Build buildToEdit) {
		super(fm);
		mContext = context;
		mBuildToEdit = (Build) UnoptimizedDeepCopy.copy(buildToEdit);
	}

	@Override
	public Fragment getItem(int position) {
		Fragment tab = null;

		switch (position) {
		case 0:
			tab = new EditBuildInfoFragment();
			break;
		case 1:
			tab = new EditBuildNotesFragment();
			break;
		case 2:
			tab = new EditBuildItemsFragment();
			break;
		}
		
		Bundle data = new Bundle();
		data.putSerializable(RaceFragment.KEY_BUILD_OBJECT, mBuildToEdit);
		tab.setArguments(data);
		return tab;
	}

	@Override
	public int getCount() {
		return 3;
	}
	
	@Override
    public CharSequence getPageTitle(int position) {
		int res = 0;
		switch (position) {
		case 0:
			res = R.string.edit_build_info_title;
			break;
		case 1:
			res = R.string.edit_build_notes_title;
			break;
		case 2:
			res = R.string.edit_build_items_title;
			break;
		}
		return mContext.getString(res);
    }

	public void setFaction(Faction selection) {
		if (mBuildToEdit.getFaction() != selection) {
			mBuildToEdit.setFaction(selection);
			// clear any build items from the old faction
			if (mBuildToEdit.getItems() != null && mBuildToEdit.getItems().size() > 0)
				mBuildToEdit.setItems(new ArrayList<BuildItem>());
		}
	}
}
