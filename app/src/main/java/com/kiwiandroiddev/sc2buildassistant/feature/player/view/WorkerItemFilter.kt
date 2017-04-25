package com.kiwiandroiddev.sc2buildassistant.feature.player.view

import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem

/**
 * Created by Matt Clarke on 25/04/17.
 */
internal class WorkerItemFilter : Function1<BuildItem, Boolean> {
    override fun invoke(buildItem: BuildItem) =
        buildItem.gameItemID !in listOf("probe", "drone", "scv")
}
