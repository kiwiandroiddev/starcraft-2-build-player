package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction;

/**
 * Used to show the StarCraft 2 factions (Terran, Protoss and Zerg) in a Spinner widget.
 * Rows in the spinner will display the factions's name and icon.
 * 
 * @author matt
 *
 */
public class FactionSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {

	private Context mContext;
	private boolean mAllowAny;
	private static int[] sFactionIcons = new int[] {
		R.drawable.terran_icon_small,
		R.drawable.zerg_icon_small,
		R.drawable.protoss_icon_small };
	
	public FactionSpinnerAdapter(Context context, boolean allowAnyOption) {
		mContext = context;
		mAllowAny = allowAnyOption;
	}
	
	@Override
	public int getCount() {
		final int extra = mAllowAny ? 1 : 0; 
		return Faction.values().length + extra;
	}

	@Override
	public Object getItem(int position) {
		if (!mAllowAny)
			return Faction.values()[position];
		else {
			if (position == 0)
				return null;
			return Faction.values()[position-1];
		}
	}

	@Override
	public long getItemId(int position) {
		// use the position to uniquely identify an element
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, parent, R.layout.icon_spinner_item);
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, parent, R.layout.icon_spinner_dropdown_item);
	}
	

	private View getView(int position, View convertView, ViewGroup parent, int viewResId) {
		View row = convertView;
		if (row == null) {
//			LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(viewResId, parent, false);
		}
		
		TextView name = (TextView)row.findViewById(R.id.icon_spinner_text1);
		ImageView icon = (ImageView)row.findViewById(R.id.icon_spinner_icon1);
		
		if (!mAllowAny) {
			name.setText(DbAdapter.getFactionName(Faction.values()[position]));
			icon.setImageResource(sFactionIcons[position]);
		} else {
			if (position == 0) {
				name.setText("Any");
			} else {
				name.setText(DbAdapter.getFactionName(Faction.values()[position-1]));
				icon.setImageResource(sFactionIcons[position-1]);
			}
		}
		
		return row;
	}
}
