package com.kiwiandroiddev.sc2buildassistant.util

import android.view.View
import android.view.ViewGroup

/**
 * Created by matthome on 15/07/17.
 */
var View.visible: Boolean
    get() = visibility == View.VISIBLE
    set(value) { visibility = View.VISIBLE.takeIf { value } ?: View.GONE }

// Thanks to https://antonioleiva.com/functional-operations-viewgroup-kotlin/
val ViewGroup.views: List<View>
    get() = (0..childCount - 1).map { getChildAt(it) }