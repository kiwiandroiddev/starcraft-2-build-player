package com.kiwiandroiddev.sc2buildassistant.util;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;

/**
 * Implementation of the AdListener interface that provides default (empty)
 * callback definitions for everything except onReceiveAd(). Reduces boilerplate.
 *
 * Created by matt on 27/04/15.
 */
public abstract class OnReceiveAdListener implements AdListener {
    @Override
    public abstract void onReceiveAd(Ad ad);

    @Override
    public void onFailedToReceiveAd(Ad ad, AdRequest.ErrorCode errorCode) {

    }

    @Override
    public void onPresentScreen(Ad ad) {

    }

    @Override
    public void onDismissScreen(Ad ad) {

    }

    @Override
    public void onLeaveApplication(Ad ad) {

    }
}
