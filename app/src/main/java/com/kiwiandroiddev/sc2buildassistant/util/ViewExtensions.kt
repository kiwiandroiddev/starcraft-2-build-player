package com.kiwiandroiddev.sc2buildassistant.util

import android.view.View

/**
 * Created by matthome on 15/07/17.
 */
var View.visible: Boolean
    get() = visibility == View.VISIBLE
    set(value) { visibility = View.VISIBLE.takeIf { value } ?: View.GONE }