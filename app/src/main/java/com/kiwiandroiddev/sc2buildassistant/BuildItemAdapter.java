package com.kiwiandroiddev.sc2buildassistant;

import java.util.ArrayList;

import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.R.id;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Adapter for displaying build items in a read-only list.
 * Each row shows the unit icon, name and scheduled build time.
 * 
 * @author matt
 *
 */
public class BuildItemAdapter extends ArrayAdapter<BuildItem> {
	private Context mContext;
	private int mLayoutResourceId;
	private ArrayList<BuildItem> mBuildItems;
	private DbAdapter mDb;
	
	public BuildItemAdapter(Context context, int layoutResourceId, ArrayList<BuildItem> items) {
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
			LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
			row = inflater.inflate(mLayoutResourceId, parent, false);
		}
		
		TextView name = (TextView)row.findViewById(R.id.unit_label);
		TextView time = (TextView)row.findViewById(R.id.time_label);
		ImageView icon = (ImageView)row.findViewById(R.id.unit_icon);
		
		BuildItem item = mBuildItems.get(position);
//		Log.w(this.toString(), item.toString());
		
		// find and display unit name in user's language
//		if (mDb.getType(item.getGameItemID()) == GameItemType.NOTE) {
//			if (item.getText() != null)
//				name.setText(item.getText());
//			else
//				name.setText(mContext.getString(R.string.error_no_build_text_message));
//		} else {
			String s = mDb.getNameString(item.getGameItemID());
			if (item.getCount() > 1)
				s = s + " x" + item.getCount();
			name.setText(s);		// stub
//		}
		
		// find and display small icon for unit (will display a placeholder if no icon was found)
		icon.setImageResource(mDb.getSmallIcon(item.getGameItemID()));
		
		// show the unit's time in the build queue
		int timeSec = item.getTime();
		time.setText(String.format("%02d:%02d", timeSec / 60, timeSec % 60));
		
		return row;
	}
}
