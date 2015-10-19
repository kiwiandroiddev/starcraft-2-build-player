package com.kiwiandroiddev.sc2buildassistant.adapter;

/**
 * Created by matt on 28/09/15.
 */
public interface ItemTouchEventListener {

    void onItemMove(int fromPosition, int toPosition);

    void onItemDismiss(int position);

}
