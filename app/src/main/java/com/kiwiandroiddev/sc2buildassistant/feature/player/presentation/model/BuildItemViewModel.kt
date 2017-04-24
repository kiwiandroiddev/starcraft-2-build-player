package com.kiwiandroiddev.sc2buildassistant.feature.player.presentation.model

import android.support.annotation.DrawableRes

data class BuildItemViewModel(val name: String,
                              val time: String,
                              @DrawableRes val iconResId: Int)