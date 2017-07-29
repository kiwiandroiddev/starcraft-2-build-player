package com.kiwiandroiddev.sc2buildassistant.util

/**
 * Created by matthome on 29/07/17.
 */
fun noneAreTrue(vararg flags: Boolean) = !(flags.toSet().any())