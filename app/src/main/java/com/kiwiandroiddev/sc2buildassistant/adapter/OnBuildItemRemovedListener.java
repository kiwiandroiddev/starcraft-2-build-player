package com.kiwiandroiddev.sc2buildassistant.adapter;

import com.kiwiandroiddev.sc2buildassistant.model.BuildItem;

/**
 * Created by matt on 18/10/15.
 */
public interface OnBuildItemRemovedListener {
    void onBuildItemRemoved(int position, BuildItem removeditem);
}
