package com.kiwiandroiddev.sc2buildassistant.data;

import com.kiwiandroiddev.sc2buildassistant.feature.player.domain.CurrentTimeProvider;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by matt on 24/03/17.
 */
public class RealCurrentTimeProvider implements CurrentTimeProvider, Serializable {

    @Override
    public long getTime() {
        return new Date().getTime();
    }

}
