package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.EditBuildInfoFragment;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.EditBuildItemsFragment;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.EditBuildNotesFragment;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem;
import com.kiwiandroiddev.sc2buildassistant.util.UnoptimizedDeepCopy;

import java.util.ArrayList;

/**
 * Manages the 3 build editor fragments (info, notes and items) for a ViewPager
 * so they can be swiped between by the user. 
 *
 * Instantiated Fragment instances can be retrieved via the {@link #getRegisteredFragment(int)}
 * method.
 * Credit: https://stackoverflow.com/questions/8785221/retrieve-a-fragment-from-a-viewpager
 *
 * @author matt
 *
 */
public class EditBuildPagerAdapter extends FragmentPagerAdapter {

	private Context mContext;
	private OnFragmentCreatedListener mFragmentCreatedListener;
	private Build mBuildToEdit;
	private SparseArray<Fragment> registeredFragments = new SparseArray<>();

	public interface OnFragmentCreatedListener {
		void onEditorFragmentCreated(Fragment newFragment);
	}

	/**
	 * 
	 * @param fm fragment manager
	 * @param buildToEdit - null if user is creating a new build
	 */
	public EditBuildPagerAdapter(@NonNull FragmentManager fm,
								 @NonNull Context context,
								 @NonNull Build buildToEdit,
								 @NonNull OnFragmentCreatedListener fragmentCreatedListener) {
		super(fm);
		mContext = context;
		mFragmentCreatedListener = fragmentCreatedListener;
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
		data.putSerializable(IntentKeys.KEY_BUILD_OBJECT, mBuildToEdit);
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

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		Fragment fragment = (Fragment) super.instantiateItem(container, position);
		registeredFragments.put(position, fragment);
		mFragmentCreatedListener.onEditorFragmentCreated(fragment);
		return fragment;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		registeredFragments.remove(position);
		super.destroyItem(container, position, object);
	}

	public Fragment getRegisteredFragment(int position) {
		return registeredFragments.get(position);
	}

	public void setFaction(Faction selection) {
		if (mBuildToEdit.getFaction() != selection) {
			mBuildToEdit.setFaction(selection);
			// clear any build items from the old faction
			if (mBuildToEdit.getItems() != null && mBuildToEdit.getItems().size() > 0) {
				mBuildToEdit.setItems(new ArrayList<BuildItem>());
			}
		}
	}
}
