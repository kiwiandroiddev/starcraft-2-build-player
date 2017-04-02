package com.kiwiandroiddev.sc2buildassistant.ads;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.kiwiandroiddev.sc2buildassistant.R;

/**
 * Created by matt on 28/02/17.
 */
public final class AdLoader {
    private AdLoader() {}

    public static void loadAdForRealUsers(AdView adView) {
        adView.setAdUnitId(adView.getContext().getString(R.string.admob_banner_ad_unit_id));
        adView.setAdSize(AdSize.SMART_BANNER);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("947D1DE4DD71747C1BDAB51AB10C6F0A")
                .addTestDevice("DE812DC8D7D780751D4A8765E49ADDEB")
                .build();
        adView.loadAd(adRequest);
    }
}
