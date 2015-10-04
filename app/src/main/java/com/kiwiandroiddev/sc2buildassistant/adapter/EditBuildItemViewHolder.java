package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kiwiandroiddev.sc2buildassistant.R;

/**
 * Stores view references for a specific row within an editable build item list.
 *
 * Created by matt on 4/10/15.
 */
public class EditBuildItemViewHolder extends RecyclerView.ViewHolder {

    public final TextView mainLabel;
    public final TextView targetLabel;
    public final TextView time;
    public final TextView count;
    public final ImageView icon;
    public final View handle;

    public EditBuildItemViewHolder(View view) {
        super(view);
        mainLabel = (TextView)view.findViewById(R.id.main_label);
        targetLabel = (TextView)view.findViewById(R.id.target_label);
        time = (TextView)view.findViewById(R.id.time_label);
        count = (TextView)view.findViewById(R.id.count_label);
        icon = (ImageView)view.findViewById(R.id.unit_icon);
        handle = view.findViewById(R.id.drag_handle);
    }

}
