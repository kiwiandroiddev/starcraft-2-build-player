package com.kiwiandroiddev.sc2buildassistant.activity.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.BuildEditorTabView;
import com.kiwiandroiddev.sc2buildassistant.activity.EditBuildItemActivity;
import com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Faction;
import com.kiwiandroiddev.sc2buildassistant.adapter.EditBuildItemRecyclerAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.OnBuildItemClickedListener;
import com.kiwiandroiddev.sc2buildassistant.adapter.OnBuildItemRemovedListener;
import com.kiwiandroiddev.sc2buildassistant.adapter.OnStartDragListener;
import com.kiwiandroiddev.sc2buildassistant.adapter.SimpleItemTouchCallback;
import com.kiwiandroiddev.sc2buildassistant.model.Build;
import com.kiwiandroiddev.sc2buildassistant.model.BuildItem;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Provides a UI for displaying a list of ordered build items that the user can edit.
 * Through this screen users can add, delete and rearrange build items. This screen
 * launches another activity (EditBuildItemActivity) to allow the user to modify details
 * of existing build items.
 * 
 * @author matt
 *
 */
public class EditBuildItemsFragment extends Fragment implements OnStartDragListener, OnBuildItemClickedListener,
        OnBuildItemRemovedListener, BuildEditorTabView {
	
	public static final String TAG = "EditBuildItemsFragment";
	private static final String KEY_BUILD_ITEM_ARRAY = "buildItemArray";
	
	private DbAdapter.Faction mFaction;		// current faction of build order, used to limit unit selection
    private DbAdapter mDb;
	private EditBuildItemRecyclerAdapter mAdapter;
	private ItemTouchHelper mTouchHelper;
	private ArrayList<BuildItem> mWorkingList;
    private Snackbar mItemDeletedSnackbar;

    @InjectView(R.id.fragment_edit_build_items_list_view) RecyclerView mRecyclerView;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// populate list view with build items
		if (savedInstanceState == null) {			
	        // set up list adapter
			
			// Okay something really funky happens here... the build object we "deserialize" from the
			// intent arguments is actually a reference to the original build in the parent activity!
			// Which causes problems because the original build's items are checked against the items
			// returned from this fragment to see if the user has made changes. In actual fact
			// any changes made will affect both build item lists meaning it will appear as though the
			// user has never changed anything!
			// The solution is to deep copy the build we get, even though it was supposedly serialized
			// which should perform the same function as deep copy...
			
			Build callerBuild = (Build) getArguments().getSerializable(IntentKeys.KEY_BUILD_OBJECT);
			//Build build = callerBuild == null ? null : (Build) UnoptimizedDeepCopy.copy(callerBuild);
			Build build = callerBuild;
			
			mFaction = build.getFaction();
			//Timber.d(this.toString(), "in EditBuildItemsFragment.onCreate(), build id = " + Integer.toHexString(System.identityHashCode(build)));
			
	        mWorkingList = build.getItems() == null ? new ArrayList<BuildItem>() : build.getItems();
		} else {
			mWorkingList = (ArrayList<BuildItem>) savedInstanceState.getSerializable(KEY_BUILD_ITEM_ARRAY);
			
			// stub: doesn't keep up to date with faction selection in Info tab!
			mFaction = (DbAdapter.Faction) savedInstanceState.getSerializable(IntentKeys.KEY_FACTION_ENUM);
		}

		// get a reference to the global DB instance
		MyApplication app = (MyApplication) getActivity().getApplicationContext();
		this.mDb = app.getDb();
	}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_edit_build_items, container, false);
        ButterKnife.inject(this, v);

        // can be left open from info and notes editor fragments
        hideKeyboard();
        
		return v;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		//noinspection ConstantConditions
		mRecyclerView.setHasFixedSize(true);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

		mAdapter = new EditBuildItemRecyclerAdapter(getActivity(), this, this, this, mWorkingList);
		mRecyclerView.setAdapter(mAdapter);

		ItemTouchHelper.Callback callback = new SimpleItemTouchCallback(mAdapter);
		mTouchHelper = new ItemTouchHelper(callback);
		mTouchHelper.attachToRecyclerView(mRecyclerView);
	}

	@Override
	public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
		mTouchHelper.startDrag(viewHolder);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(KEY_BUILD_ITEM_ARRAY, mAdapter.getBuildItems());
		outState.putSerializable(IntentKeys.KEY_FACTION_ENUM, mFaction);
	}

    /**
     * Called when user changes the build's faction in the Info editor fragment.
     * When this happens, all items should be cleared from the build because they're no longer
     * available to the new faction.
     * @param selection
     */
	public void setFaction(Faction selection) {
		if (selection != mFaction) {
			mFaction = selection;
			mAdapter.clear();
		}
	}
	
	public ArrayList<BuildItem> getBuildItems() {
		return mWorkingList;
	}

    @Override
    public void onAddButtonClicked() {
        Intent i = new Intent(getActivity(), EditBuildItemActivity.class);
        i.putExtra(IntentKeys.KEY_FACTION_ENUM, mFaction);
        i.putExtra(EditBuildItemActivity.KEY_DEFAULT_TIME, getDuration());
        // TODO pass default supply as well?
        startActivityForResult(i, EditBuildItemActivity.EDIT_BUILD_ITEM_REQUEST);
    }

	@Override
	public void onBuildItemClicked(BuildItem item, int position) {
		Intent i = new Intent(getActivity(), EditBuildItemActivity.class);
		i.putExtra(IntentKeys.KEY_FACTION_ENUM, mFaction);
		i.putExtra(IntentKeys.KEY_BUILD_ITEM_OBJECT, item);
		i.putExtra(EditBuildItemActivity.KEY_INCOMING_BUILD_ITEM_ID, position);
		// TODO pass default supply as well?
		startActivityForResult(i, EditBuildItemActivity.EDIT_BUILD_ITEM_REQUEST);
	}

    /**
	 * Called with the output of the item editor dialog, from either adding a new build item
	 * to the list or selecting an existing one to edit.
	 *
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // Check which request we're responding to
	    if (requestCode == EditBuildItemActivity.EDIT_BUILD_ITEM_REQUEST) {
	        // Make sure the request was successful
	        if (resultCode == Activity.RESULT_OK) {
	        	Long id = data.getExtras().containsKey(EditBuildItemActivity.KEY_INCOMING_BUILD_ITEM_ID) ?
	        		data.getExtras().getLong(EditBuildItemActivity.KEY_INCOMING_BUILD_ITEM_ID) :
	        		null;

	        	BuildItem item = (BuildItem) data.getExtras().getSerializable(IntentKeys.KEY_BUILD_ITEM_OBJECT);
	        	//Timber.d(this.toString(), "in EditBuildItemsFragment, got item = " + item);

	        	if (id == null) {	// i.e. new build item to add
	        		// slot the new build item into the correct place based on its time
	        		int position = mAdapter.autoInsert(item);
//	        		mListView.setSelection(position);
					// TODO scroll to position of newly inserted item

	        		invalidateUndo();
	        	} else {			// an existing one should be modified
	        		//Timber.d(this.toString(), "replacing item at index " + id  + " with " + item);
	        		mAdapter.replace(item, id.intValue());
	        	}
	        	// temp testing
	        	//Timber.d(this.toString(), "added build item, items size now = " + mAdapter.getCount());
	        }
	    }
	}

    @Override
    public void onBuildItemRemoved(final int deletedItemPosition, final BuildItem deletedItem) {
        String itemName = mDb.getNameString(deletedItem.getGameItemID());
        String deletionMessage = deletedItem.getCount() > 1 ?
                            getString(R.string.deleted_items_x_N, itemName, deletedItem.getCount()) :
                            getString(R.string.deleted_item, itemName);

        mItemDeletedSnackbar = Snackbar.make(getView(), deletionMessage, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (deletedItemPosition != -1 && deletedItem != null) {
                            mAdapter.insert(deletedItem, deletedItemPosition);
                        }
                        mItemDeletedSnackbar = null;
                    }
                });
        mItemDeletedSnackbar.show();
    }

	private void invalidateUndo() {
        if (mItemDeletedSnackbar != null) {
            mItemDeletedSnackbar.dismiss();
            mItemDeletedSnackbar = null;
        }
	}

    // ========================================================================
	// Helpers
	// ========================================================================

	/**
	 * returns the time value for the last build item in the list
	 * (which should probably be the maximum time in the list)
	 * @return time in seconds
	 */
	private int getDuration() {
		int count = mAdapter.getBuildItems().size();
		if (count > 0) {
			return mAdapter.getBuildItems().get(count-1).getTime();
		} else {
			return 0;
		}
	}

	/** Hides the on-screen keyboard if visible */
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
			      Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mRecyclerView.getWindowToken(), 0);
	}

    @Override
    public boolean requestsAddButton() {
        return true;
    }
}
