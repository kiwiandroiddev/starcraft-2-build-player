package com.kiwiandroiddev.sc2buildassistant.feature.player.domain


import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem

/** Interface used by BuildPlayer to notify high-level code of playback events  */
interface BuildPlayerEventListener {
    fun onBuildThisNow(item: BuildItem, position: Int)
    fun onBuildPlay()
    fun onBuildPaused()
    fun onBuildStopped()
    fun onBuildResumed()
    fun onBuildFinished()
    fun onBuildItemsChanged(newBuildItems: List<BuildItem>)
    fun onIterate(newGameTimeMs: Long)
}
