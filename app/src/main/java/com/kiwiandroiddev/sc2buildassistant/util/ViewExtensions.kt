package com.kiwiandroiddev.sc2buildassistant.util

import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils

/**
 * Created by matthome on 15/07/17.
 */
var View.visible: Boolean
    get() = visibility == View.VISIBLE
    set(value) { visibility = View.VISIBLE.takeIf { value } ?: View.GONE }

// Thanks to https://antonioleiva.com/functional-operations-viewgroup-kotlin/
val ViewGroup.views: List<View>
    get() = (0..childCount - 1).map { getChildAt(it) }

fun View.startFadeInAnimation() {
    val animation = AnimationUtils.loadAnimation(this.context, android.R.anim.fade_in)
    animation.setAnimationListener(object : NoOpAnimationListener() {
        override fun onAnimationStart(animation: Animation?) {
            visible = true
        }
    })
    startAnimation(animation)
}

fun View.startFadeOutAnimation() {
    val animation = AnimationUtils.loadAnimation(this.context, android.R.anim.fade_out)
    animation.setAnimationListener(object : NoOpAnimationListener() {
        override fun onAnimationEnd(animation: Animation) {
            visibility = View.INVISIBLE
        }
    })
    startAnimation(animation)
}