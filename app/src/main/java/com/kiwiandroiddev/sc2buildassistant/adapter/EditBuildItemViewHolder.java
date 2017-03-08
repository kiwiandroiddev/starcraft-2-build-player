package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kiwiandroiddev.sc2buildassistant.R;

/**
 * Stores view references for a specific row within an editable build item list.
 * <p>
 * Created by matt on 4/10/15.
 */
class EditBuildItemViewHolder extends RecyclerView.ViewHolder {

    public final TextView mainLabel;
    public final TextView targetLabel;
    public final TextView time;
    public final TextView count;
    public final ImageView icon;
    public final View handle;
    public final View container;

    EditBuildItemViewHolder(View view) {
        super(view);
        mainLabel = (TextView) view.findViewById(R.id.main_label);
        targetLabel = (TextView) view.findViewById(R.id.target_label);
        time = (TextView) view.findViewById(R.id.time_label);
        count = (TextView) view.findViewById(R.id.count_label);
        icon = (ImageView) view.findViewById(R.id.unit_icon);
        handle = view.findViewById(R.id.drag_handle);
        container = view.findViewById(R.id.container);
    }

    void setOutOfOrderIndicatorVisibility(boolean visible) {
        time.setTextColor(
                visible ? getColor(R.color.build_item_out_of_position)
                        : getColor(android.R.color.secondary_text_dark));
    }

    private int getColor(int build_item_out_of_position) {
        return ContextCompat.getColor(itemView.getContext(), build_item_out_of_position);
    }

}
