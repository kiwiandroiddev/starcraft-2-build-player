package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

interface BriefNavigator {
    fun onPlayBuild(buildId: Long)
    fun onEditBuild(buildId: Long)
}