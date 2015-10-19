package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.model.BuildItem;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Adapter for displaying build items in an editable list (as opposed to BuildItemAdapter
 * which is for showing a read-only list of build items, e.g. on the Playback screen).
 * 
 * @author matt
 *
 */
public class EditBuildItemAdapter extends ArrayAdapter<BuildItem> {
	private Context mContext;
	private int mLayoutResourceId;
	private ArrayList<BuildItem> mBuildItems;
	private DbAdapter mDb;
	
	public EditBuildItemAdapter(Context context, int layoutResourceId, ArrayList<BuildItem> items) {
		super(context, layoutResourceId, items);
		this.mLayoutResourceId = layoutResourceId;
		this.mContext = context;
		this.mBuildItems = items;
		
		// get a reference to the global DB instance
		MyApplication app = (MyApplication) context.getApplicationContext();
		this.mDb = app.getDb();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;

		if (row == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(mLayoutResourceId, parent, false);
		}
		
		TextView main = (TextView)row.findViewById(R.id.main_label);
		TextView target = (TextView)row.findViewById(R.id.target_label);
		TextView time = (TextView)row.findViewById(R.id.time_label);
		TextView count = (TextView)row.findViewById(R.id.count_label);
		ImageView icon = (ImageView)row.findViewById(R.id.unit_icon);
		
		BuildItem item = mBuildItems.get(position);
		
		// work out if this build item is in the wrong position based on its time
		boolean outOfPosition = false;
		if (position > 0 && position < mBuildItems.size()) {
			BuildItem previousItem = mBuildItems.get(position - 1);
			if (item.getTime() < previousItem.getTime())
				outOfPosition = true;
		}
		
//		Timber.d(this.toString(), String.format("in getView(), position=%s, convertView=%s, parent=%s, item=%s, main=%s, target=%s, item.getText()=%s",
//				position, convertView, parent, item, main, target, item.getText()));
		
		// set the main label (either unit name or message text)
		if (item.getText() == null || item.getText().matches("")) {
			String s = mDb.getNameString(item.getGameItemID());
			main.setText(s);
		} else {
			main.setText(item.getText());
		}
		
		// display unit count (if something other than 1)
		if (item.getCount() > 1) {
			count.setText("x" + item.getCount());
			count.setVisibility(View.VISIBLE);
		} else {
			count.setVisibility(View.GONE);
		}

		// display ability target
		if (item.getTarget() == null || item.getTarget().matches("")) {
			target.setVisibility(View.GONE);
		} else {
//			Timber.d(this.toString(), "item = " + item + ", target = " + item.getTarget() + ", target label vis = " + target.getVisibility());
			String s = mDb.getNameString(item.getTarget());
			target.setText("on " + s);
			target.setVisibility(View.VISIBLE);
		}
		
		// find and display small icon for unit (will display a placeholder if no icon was found)
		icon.setImageResource(mDb.getSmallIcon(item.getGameItemID()));
		
		// show the unit's time in the build queue
		int timeSec = item.getTime();
		time.setText(String.format("%02d:%02d", timeSec / 60, timeSec % 60));
		if (outOfPosition)
			time.setTextColor(Color.RED);
		else
			time.setTextColor(mContext.getResources().getColor(android.R.color.secondary_text_dark));
		
		return row;
	}
	
	/**
	 * Useful for saving and restoring the adapter in its host activity
	 * 
	 * @return
	 */
	public ArrayList<BuildItem> getArrayList() {
		return mBuildItems;
	}
	
	/**
	 * Replaces the build item at specified index with a new one,
	 * if the new and existing build items aren't equivalent.
	 * 
	 * @param item		new BuildItem
	 * @param index		position in arraylist
	 */
	public void replace(BuildItem item, int index) {
		BuildItem existingItem = getItem(index);
		
		if (item == existingItem || item.equals(existingItem))
			return;
		
		mBuildItems.set(index, item);
		notifyDataSetChanged();
	}
	
	/**
	 * Attempts to delete the build item at the given index from the internal arraylist
	 * 
	 * @param index
	 */
	public void delete(int index) {
		mBuildItems.remove(index);
		notifyDataSetChanged();
	}
	
	/**
	 * Attempts to swap two items in the arraylist.
	 * @param srcIndex
	 * @param destIndex
	 * @return true if successful, false if one of the indices was out of bounds
	 */
	public boolean swapItem(int srcIndex, int destIndex) {		
		try {
			Collections.swap(mBuildItems, srcIndex, destIndex);
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
		
		notifyDataSetChanged();
		return true;
	}
	
	/**
	 * Attempts to insert a new build item into the correct position in the list
	 * based on its time value. The existing item at that position (if any) and
	 * all subsequent items will be shifted along to the right by one. Note:
	 * assumes build items are already ordered by time (which they may well not
	 * be). Insertion takes O(n) time.
	 * 
	 * @param item
	 *            new build item to add
	 * @return index of array where item was inserted.
	 */
	public int insert(BuildItem item) {
		for (int i=0; i<mBuildItems.size(); i++) {
			if (item.getTime() < mBuildItems.get(i).getTime()) {
				mBuildItems.add(i, item);
				notifyDataSetChanged();
				return i;
			}
		}
		
		// item had a greater time value than all others, append it to the end
		mBuildItems.add(item);
		notifyDataSetChanged();
		return (mBuildItems.size()-1);
	}
}
