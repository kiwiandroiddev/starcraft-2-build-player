package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * Created by matt on 28/09/15.
 */
public class SimpleItemTouchCallback extends ItemTouchHelper.Callback {

    private static int INVALID_POSITION = -1;

    private ItemTouchEventListener mListener;
    private int draggedTo = INVALID_POSITION;

    public SimpleItemTouchCallback(@NonNull ItemTouchEventListener listener) {
        super();
        mListener = listener;
    }

    /**
     * Specifies which directions of dragging and swiping are supported -
     * horizontal swiping and vertical dragging.
     * @param recyclerView
     * @param viewHolder
     * @return
     */
    @Override
    public int getMovementFlags(RecyclerView recyclerView, ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    /**
     * Drags cannot be started by long-pressing anywhere on an item -
     * they are only supported via a press down on drag handle view.
     * @return
     */
    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    /**
     * Item swipe action can be started from anywhere within the view.
     * @return
     */
    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    /**
     * If this method returns true, ItemTouchHelper assumes viewHolder has been
     * moved to the adapter position of target ViewHolder
     * @param recyclerView
     * @param viewHolder
     * @param target
     * @return
     */
    @Override
    public boolean onMove(RecyclerView recyclerView,
                          ViewHolder viewHolder,
                          ViewHolder target) {
        int toPosition = target.getAdapterPosition();
        mListener.onItemMove(viewHolder.getAdapterPosition(), toPosition);

        draggedTo = toPosition;

        return true;
    }

    @Override
    public void onSwiped(ViewHolder viewHolder, int directionFlags) {
        mListener.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void clearView(RecyclerView recyclerView, ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        if (draggedTo != INVALID_POSITION)
            mListener.onItemDropped(draggedTo);

        draggedTo = INVALID_POSITION;
    }
}
