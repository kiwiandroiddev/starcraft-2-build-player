package com.kiwiandroiddev.sc2buildassistant.domain;

/**
 * Created by matt on 23/03/17.
 */
public interface CurrentTimeProvider {

    /*
     * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
     */
    long getTime();

}
