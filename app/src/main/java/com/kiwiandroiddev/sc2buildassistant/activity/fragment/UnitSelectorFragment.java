package com.kiwiandroiddev.sc2buildassistant.activity.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys;
import com.kiwiandroiddev.sc2buildassistant.activity.UnitSelectorActivity;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.ItemType;
import com.kiwiandroiddev.sc2buildassistant.adapter.UnitIconAdapter;

/**
 * Fragment letting the user pick an item within one item type and faction, e.g. only Zerg upgrades.
 * A UnitSelectorActivity displays a separate UnitSelectorFragment for every item type within
 * a faction.
 * 
 * @author matt
 *
 */
public class UnitSelectorFragment extends Fragment {
	
	private Faction mFactionFilter;
	private ItemType mItemTypeFilter;
	private UnitIconAdapter mAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_item_selector, container, false);
		
		Bundle dataSource = savedInstanceState == null ? getArguments() : savedInstanceState; 

		mFactionFilter = (Faction) dataSource.getSerializable(IntentKeys.KEY_FACTION_ENUM);
		mItemTypeFilter = (ItemType) dataSource.getSerializable(IntentKeys.KEY_ITEM_TYPE_ENUM);

//		Timber.d(this.toString(), "in UnitSelectorFragment.onCreateView(), faction = " + mFactionFilter
//				+ ", item type = " + mItemTypeFilter);
		
		GridView gridview = (GridView) v.findViewById(R.id.gridview);
		mAdapter = new UnitIconAdapter(getActivity(), mFactionFilter, mItemTypeFilter);
		gridview.setAdapter(mAdapter);
		gridview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				// pass the selected Item row ID (in database) back to the calling activity
				Intent data = new Intent();
				data.putExtra(UnitSelectorActivity.KEY_RESULT_ITEM_ID, mAdapter.getItemId(position));
				((UnitSelectorActivity) getActivity()).setCustomResult(UnitSelectorActivity.RESULT_OK, data);
				getActivity().finish();
			}
		});
		
		return v;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(IntentKeys.KEY_FACTION_ENUM, mFactionFilter);
		outState.putSerializable(IntentKeys.KEY_ITEM_TYPE_ENUM, mItemTypeFilter);
		super.onSaveInstanceState(outState);
	}
}
