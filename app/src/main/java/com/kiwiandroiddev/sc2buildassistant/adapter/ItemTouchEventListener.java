package com.kiwiandroiddev.sc2buildassistant.adapter;

/**
 * Created by matt on 28/09/15.
 */
interface ItemTouchEventListener {

    void onItemMove(int fromPosition, int toPosition);

    void onItemDismiss(int position);

    /**
     * Called when the user interaction with some element is over and it
     * completed its animation.
     */
    void onItemDropped(int atPosition);

}
