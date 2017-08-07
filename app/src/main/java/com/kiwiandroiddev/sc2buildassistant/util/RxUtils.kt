package com.kiwiandroiddev.sc2buildassistant.util

import io.reactivex.Observable

/**
 * Created by matthome on 28/07/17.
 */
fun Observable<Boolean>.whenTrue(): Observable<Boolean> = filter { it == true }