package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.ItemType;

import java.util.LinkedHashMap;

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
    private LinkedHashMap<Long, IconWithDescription> mItemIDToIconMap;

    private static class IconWithDescription {
        @DrawableRes final int iconDrawable;
        @StringRes final int contentDescription;

        private IconWithDescription(int iconDrawable, int contentDescription) {
            this.iconDrawable = iconDrawable;
            this.contentDescription = contentDescription;
        }
    }

    // TODO: allow filtering items by expansion level as well, this will mean large changes to
    // the database though
    public UnitIconAdapter(Context c, Faction factionFilter, ItemType itemTypeFilter) {
        mContext = c;
        DbAdapter db = ((MyApplication) c.getApplicationContext()).getDb();
        Cursor cursor = db.fetchItemIDsMatching(factionFilter, itemTypeFilter);
        mItemIDToIconMap = new LinkedHashMap<Long, IconWithDescription>(50);
        while (cursor.moveToNext()) {
        	long row_id = cursor.getLong(0);
            String itemUniqueName = db.getItemUniqueName(row_id);
            IconWithDescription iconWithDescription = new IconWithDescription(db.getSmallIcon(itemUniqueName), db.getName(itemUniqueName));
            mItemIDToIconMap.put(row_id, iconWithDescription);
        }
        cursor.close();
    }

    public int getCount() {
    	return mItemIDToIconMap.size();
    }

    public IconWithDescription getItem(int position) {
        return mItemIDToIconMap.get(getItemId(position));
    }

    public long getItemId(int position) {
        return (Long) mItemIDToIconMap.keySet().toArray()[position];
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(pxToDp(64, mContext), pxToDp(64, mContext)));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        IconWithDescription iconWithDescription = getItem(position);
        imageView.setImageResource(iconWithDescription.iconDrawable);
        imageView.setContentDescription(mContext.getString(iconWithDescription.contentDescription));
        return imageView;
    }
    
    private static int pxToDp(int pixels, Context context) {
    	return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixels,
    			context.getResources().getDisplayMetrics());
    }
}