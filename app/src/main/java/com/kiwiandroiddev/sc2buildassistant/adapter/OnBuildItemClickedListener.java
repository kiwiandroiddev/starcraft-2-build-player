package com.kiwiandroiddev.sc2buildassistant.adapter;

import com.kiwiandroiddev.sc2buildassistant.model.BuildItem;

/**
 * Created by matt on 6/10/15.
 */
public interface OnBuildItemClickedListener {
    void onBuildItemClicked(BuildItem item, int position);
}
