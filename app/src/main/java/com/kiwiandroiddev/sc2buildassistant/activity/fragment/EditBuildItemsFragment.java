package com.kiwiandroiddev.sc2buildassistant.activity.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.EditBuildItemActivity;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Faction;
import com.kiwiandroiddev.sc2buildassistant.adapter.EditBuildItemAdapter;
import com.kiwiandroiddev.sc2buildassistant.model.Build;
import com.kiwiandroiddev.sc2buildassistant.model.BuildItem;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Provides a UI for displaying a list of ordered build items that the user can edit.
 * Through this screen users can add, delete and rearrange build items. This screen
 * launches another activity (EditBuildItemActivity) to allow the user to modify details
 * of existing build items.
 * 
 * @author matt
 *
 */
public class EditBuildItemsFragment extends Fragment implements OnItemClickListener {
	
	public static final String TAG = "EditBuildItemsFragment";
	private static final String KEY_BUILD_ITEM_ARRAY = "buildItemArray";
	
	private EditBuildItemAdapter mAdapter;
	private DbAdapter.Faction mFaction;		// current faction of build order, used to limit unit selection
	private BuildItem mDeletedItem;			// for undo support
	private int mDeletedItemIndex;			// for undo support
    private DbAdapter mDb;

    @InjectView(R.id.edit_items_list_view) ListView mListView;
    @InjectView(R.id.undobar) View mUndoBar;
	@InjectView(R.id.undobar_message) TextView mUndoText;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		 ArrayList<BuildItem> workingList;
	        
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
			
			Build callerBuild = (Build) getArguments().getSerializable(RaceFragment.KEY_BUILD_OBJECT);
			//Build build = callerBuild == null ? null : (Build) UnoptimizedDeepCopy.copy(callerBuild);
			Build build = callerBuild;
			
			mFaction = build.getFaction();
			//Timber.d(this.toString(), "in EditBuildItemsFragment.onCreate(), build id = " + Integer.toHexString(System.identityHashCode(build)));
			
	        workingList = build.getItems() == null ? new ArrayList<BuildItem>() : build.getItems();
		} else {
			workingList = (ArrayList<BuildItem>) savedInstanceState.getSerializable(KEY_BUILD_ITEM_ARRAY);
			
			// stub: doesn't keep up to date with faction selection in Info tab!
			mFaction = (DbAdapter.Faction) savedInstanceState.getSerializable(RaceFragment.KEY_FACTION_ENUM);
		}
		
		mAdapter = new EditBuildItemAdapter(getActivity(),
        		R.layout.edit_build_item_row, workingList);
		
		// get a reference to the global DB instance
		MyApplication app = (MyApplication) getActivity().getApplicationContext();
		this.mDb = app.getDb();
	}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_edit_build_items, container, false);
        ButterKnife.inject(this, v);

		mListView.setOnItemClickListener(this);
		
        // set up the "Add item..." extra list entry at the bottom
        mListView.addFooterView(getFooterView(), null, true);
        mListView.setAdapter(mAdapter);
        
        // set up drag event listeners
        DragSortListView dragListView = (DragSortListView) mListView;
        dragListView.setDropListener(onDrop);
        dragListView.setRemoveListener(onRemove);
        //dragListView.setDragScrollProfile(ssProfile);

        // can be left open from info and notes editor fragments
        hideKeyboard();
        
		return v;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(KEY_BUILD_ITEM_ARRAY, mAdapter.getArrayList());
		outState.putSerializable(RaceFragment.KEY_FACTION_ENUM, mFaction);
	}
	
	public void setFaction(Faction selection) {
		if (selection != mFaction) {
			mFaction = selection;
			mAdapter.clear();
		}
	}
	
	public ArrayList<BuildItem> getBuildItems() {
		//Timber.d(this.toString(), "getBuildItems() called, items count = " + mAdapter.getCount());
		return mAdapter.getArrayList();
	}
	
	private View getFooterView() {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		return inflater.inflate(R.layout.edit_build_item_row_footer, null, false);
	}

	/**
	 * User clicked a build item in the list, let them edit it
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//Timber.d(this.toString(), "parent = " + parent + ", view = " + view + ", position = " + position + ", id = " + id);
		
		Intent i = new Intent(getActivity(), EditBuildItemActivity.class);
        i.putExtra(RaceFragment.KEY_FACTION_ENUM, mFaction);
        
		// -1 is the footer item ID
		if (id != -1) {
			BuildItem item = mAdapter.getItem(position);
			//Timber.d(this.toString(), "onItemClick(), sending to EditBuildItemActivity item " + item);
			i.putExtra(RaceFragment.KEY_BUILD_ITEM_OBJECT, item);
	        i.putExtra(EditBuildItemActivity.KEY_INCOMING_BUILD_ITEM_ID, id);
		} else {
			// "Add item" clicked
			i.putExtra(EditBuildItemActivity.KEY_DEFAULT_TIME, getDuration());	// stub
			// TODO pass default supply as well?
		}
		
        startActivityForResult(i, EditBuildItemActivity.EDIT_BUILD_ITEM_REQUEST);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // Check which request we're responding to
	    if (requestCode == EditBuildItemActivity.EDIT_BUILD_ITEM_REQUEST) {	    	
	        // Make sure the request was successful
	        if (resultCode == Activity.RESULT_OK) {
	        	Long id = data.getExtras().containsKey(EditBuildItemActivity.KEY_INCOMING_BUILD_ITEM_ID) ?
	        		data.getExtras().getLong(EditBuildItemActivity.KEY_INCOMING_BUILD_ITEM_ID) :
	        		null;
	        		
	        	BuildItem item = (BuildItem) data.getExtras().getSerializable(RaceFragment.KEY_BUILD_ITEM_OBJECT);
	        	//Timber.d(this.toString(), "in EditBuildItemsFragment, got item = " + item);
	        	
	        	if (id == null) {	// i.e. new build item to add	        		
	        		// slot the new build item into the correct place based on its time
	        		int position = mAdapter.insert(item);
	        		mListView.setSelection(position);
	        		
	        		invalidateUndo();
	        	} else {			// an existing one should be modified
	        		//Timber.d(this.toString(), "replacing item at index " + id  + " with " + item);
	        		((EditBuildItemAdapter)mAdapter).replace(item, id.intValue());
	        	}
	        	// temp testing
	        	//Timber.d(this.toString(), "added build item, items size now = " + mAdapter.getCount());
	        }
	    }
	}
	
	/**
	 * Basic support for undoing build item deletion. Shows a small overlay with
	 * an undo button to restore a build item that was just deleted.
	 * 
	 * @param itemIndex	  index of deleted item in underlying arraylist
	 * @param deletedItem  build item that was deleted
	 */
	private void showUndo(int itemIndex, BuildItem deletedItem) {
		mDeletedItemIndex = itemIndex;
		mDeletedItem = deletedItem;
		
		// fade in overlay...
		String name = mDb.getNameString(deletedItem.getGameItemID());
		String undoText = "Deleted <b>" + name;
		if (deletedItem.getCount() > 1)
			undoText = undoText + " x" + deletedItem.getCount();
		undoText = undoText + "</b>";
		
		mUndoText.setText(Html.fromHtml(undoText));
		mUndoBar.setVisibility(View.VISIBLE);
//	    mUndoBar.setAlpha(1);
//	    mUndoBar.animate().alpha(0.4f).setDuration(5000)
//	        .withEndAction(new Runnable() {
//
//	          @Override
//	          public void run() {
//	        	  mUndoBar.setVisibility(View.GONE);
//	          }
//	        });
	}
	
	private void invalidateUndo() {
		mDeletedItemIndex = -1;
		mDeletedItem = null;
		// fade out overlay...
		mUndoBar.setVisibility(View.GONE);
	}

    @OnClick(R.id.undobar_button)
	public void onUndoClick() {
		if (mDeletedItem != null && mDeletedItemIndex != -1) {
			mAdapter.insert(mDeletedItem, mDeletedItemIndex);
			mAdapter.notifyDataSetChanged();
			invalidateUndo();
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
		int count = ((EditBuildItemAdapter)mAdapter).getArrayList().size();
		if (count > 0) {
			return ((EditBuildItemAdapter)mAdapter).getArrayList().get(count-1).getTime();
		} else {
			return 0;
		}
	}
	
	/** Hides the on-screen keyboard if visible */
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
			      Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mUndoText.getWindowToken(), 0);
	}
	
	// ========================================================================
	// Listeners for listview drag events
	// ========================================================================
	
	private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			if (from != to) {
//				Timber.d(TAG, "drop() called with from: " + from + ", to: " + to);
				BuildItem item = mAdapter.getItem(from);
				mAdapter.remove(item);
				mAdapter.insert(item, to);
				mAdapter.notifyDataSetChanged();
				
				EditBuildItemsFragment.this.invalidateUndo();
			}
		}
	};

	private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
		@Override
		public void remove(int which) {
//			Timber.d(TAG, "remove() called with which: " + which);
			BuildItem item = mAdapter.getItem(which); 
			mAdapter.remove(item);
			
			EditBuildItemsFragment.this.showUndo(which, item);
		}
	};

//	private DragSortListView.DragScrollProfile ssProfile = new DragSortListView.DragScrollProfile() {
//		@Override
//		public float getSpeed(float w, long t) {
//			if (w > 0.8f) {
//				// Traverse all views in a millisecond
//				return ((float) t * 0.001f);
//			} else {
//				return 10.0f * w;
//			}
//		}
//	};
}
