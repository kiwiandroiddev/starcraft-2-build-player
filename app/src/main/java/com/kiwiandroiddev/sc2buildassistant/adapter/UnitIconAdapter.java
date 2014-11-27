package com.kiwiandroiddev.sc2buildassistant.adapter;

import java.util.LinkedHashMap;

import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Faction;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.ItemType;

import android.content.Context;
import android.database.Cursor;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Manages a collection of Items (units/structures/upgrades...) and allows them to be displayed in a View
 * as icons. The items managed by a UnitIconAdapter are of only one faction and itemtype (e.g. Terran buildings or Zerg units).
 * 
 * @author matt
 *
 */
public class UnitIconAdapter extends BaseAdapter {
    private Context mContext;
    
    // using linked hash to preserve key insertion order (so items appear ordered by name)
    private LinkedHashMap<Long, Integer> mItemIDToIconMap;   
    
    // TODO: allow filtering items by expansion level as well, this will mean large changes to
    // the database though
    public UnitIconAdapter(Context c, Faction factionFilter, ItemType itemTypeFilter) {
        mContext = c;
        
        // fetch IDs of items to display (stub)
        DbAdapter db = ((MyApplication) c.getApplicationContext()).getDb();
        Cursor cursor = db.fetchItemIDsMatching(factionFilter, itemTypeFilter);
        mItemIDToIconMap = new LinkedHashMap<Long, Integer>(50);
        while (cursor.moveToNext()) {
        	long row_id = cursor.getLong(0);
        	mItemIDToIconMap.put(row_id, db.getSmallIcon(db.getItemUniqueName(row_id)));
        }
        cursor.close();
    }

    public int getCount() {
    	return mItemIDToIconMap.size();
    }

    public Object getItem(int position) {
        return mItemIDToIconMap.get(getItemId(position));
    }

    public long getItemId(int position) {
        return (Long) mItemIDToIconMap.keySet().toArray()[position];
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(pxToDp(64, mContext), pxToDp(64, mContext)));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        //imageView.setImageResource(mThumbIds[position]);
        imageView.setImageResource((Integer) getItem(position));
        return imageView;
    }
    
    public static int pxToDp(int pixels, Context context) {
    	return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixels,
    			context.getResources().getDisplayMetrics());
    }
}