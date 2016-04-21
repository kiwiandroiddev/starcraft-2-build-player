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
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Expansion;

/**
 * Used to show available StarCraft 2 expansions in a Spinner widget. Rows in the
 * spinner include the expansion's name and an icon.
 * 
 * @author matt
 *
 */
public class ExpansionSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {

	private Context mContext;
	private String[] mExpansionNames;
	private static int[] sExpansionIcons = new int[] { R.drawable.wol_icon, R.drawable.hots_icon, R.drawable.lotv_icon };
	
	public ExpansionSpinnerAdapter(Context context) {
		mContext = context;
		mExpansionNames = new String[] {
				context.getString(R.string.expansion_wol),
				context.getString(R.string.expansion_hots),
				context.getString(R.string.expansion_lotv)};
	}
	
	@Override
	public int getCount() {
		return Expansion.values().length;
	}

	@Override
	public Object getItem(int position) {
		return Expansion.values()[position];
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
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(viewResId, parent, false);
		}
		
		TextView name = (TextView)row.findViewById(R.id.icon_spinner_text1);
		ImageView icon = (ImageView)row.findViewById(R.id.icon_spinner_icon1);
		name.setText(mExpansionNames[position]);
		icon.setImageResource(sExpansionIcons[position]);
		
		return row;
	}
}
