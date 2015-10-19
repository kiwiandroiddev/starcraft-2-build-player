package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.support.v7.widget.RecyclerView;

public interface OnStartDragListener {

    /**
     * Called when a view is requesting a start of a drag. This is used to pass this event from
     * a list adapter to an ItemTouchHelper.
     *
     * @param viewHolder The holder of the view to drag.
     */
    void onStartDrag(RecyclerView.ViewHolder viewHolder);
}
