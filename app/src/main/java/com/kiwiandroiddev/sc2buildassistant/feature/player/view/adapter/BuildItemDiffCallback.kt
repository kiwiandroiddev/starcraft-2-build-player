package com.kiwiandroiddev.sc2buildassistant.feature.player.view.adapter

import android.support.v7.util.DiffUtil
import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem

/**
 * Created by Matt Clarke on 25/04/17.
 */
class BuildItemDiffCallback(val oldBuildItems: List<BuildItem>,
                            val newBuildItems: List<BuildItem>) : DiffUtil.Callback() {

    override fun getOldListSize() = oldBuildItems.size

    override fun getNewListSize() = newBuildItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldBuildItems[oldItemPosition] == newBuildItems[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            areItemsTheSame(oldItemPosition, newItemPosition)

}