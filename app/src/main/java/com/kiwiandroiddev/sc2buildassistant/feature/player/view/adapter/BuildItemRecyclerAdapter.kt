package com.kiwiandroiddev.sc2buildassistant.feature.player.view.adapter

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.kiwiandroiddev.sc2buildassistant.MyApplication
import com.kiwiandroiddev.sc2buildassistant.R
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter
import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem
import com.kiwiandroiddev.sc2buildassistant.feature.player.presentation.model.BuildItemViewModel

/**
 * Created by Matt Clarke on 24/04/17.
 */
class BuildItemRecyclerAdapter(val context: Context,
                               @LayoutRes val footerLayoutId: Int) : RecyclerView.Adapter<BuildItemRecyclerAdapter.RowViewHolder>() {

    companion object {
        private val BUILD_ITEM_ROW_TYPE = 0
        private val FOOTER_ROW_TYPE = 1

        private val NUM_FOOTER_ITEMS = 1
    }

    private val database: DbAdapter by lazy { (context.applicationContext as MyApplication).db!! }

    var buildItems: List<BuildItem> = emptyList()
        set(value) {
            if (field.isEmpty()) {
                field = value
                notifyDataSetChanged()
            } else {
                val diffCallback = BuildItemDiffCallback(buildItems, value)
                val diffResult = DiffUtil.calculateDiff(diffCallback)

                field = value
                diffResult.dispatchUpdatesTo(this)
            }
        }

    override fun getItemCount() = buildItems.size + NUM_FOOTER_ITEMS

    override fun getItemViewType(position: Int) =
        if (position < buildItems.size) BUILD_ITEM_ROW_TYPE else FOOTER_ROW_TYPE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val inflater = LayoutInflater.from(context)

        if (viewType == BUILD_ITEM_ROW_TYPE) {
            val itemView = inflater.inflate(R.layout.build_item_row, parent, false)
            return RowViewHolder.BuildItemViewHolder(itemView)
        } else {
            val itemView = inflater.inflate(footerLayoutId, parent, false)
            return RowViewHolder.FooterItemViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        when(holder) {
            is RowViewHolder.BuildItemViewHolder -> {
                val viewModel = itemViewModelForPosition(position)
                with(holder) {
                    name.text = viewModel.name
                    time.text = viewModel.time
                    icon.setImageResource(viewModel.iconResId)
                }
            }
            is RowViewHolder.FooterItemViewHolder -> return
        }
    }

    sealed class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        class BuildItemViewHolder(itemView: View) : RowViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.unit_label) as TextView
            val time: TextView = itemView.findViewById(R.id.time_label) as TextView
            val icon: ImageView = itemView.findViewById(R.id.unit_icon) as ImageView
        }

        class FooterItemViewHolder(itemView: View) : RowViewHolder(itemView)

    }

    private fun itemViewModelForPosition(position: Int) =
            buildItems[position].mapToViewModel()

    private fun BuildItem.mapToViewModel(): BuildItemViewModel {
        var itemName = database.getNameString(gameItemID)
        if (count > 1)
            itemName = itemName + " x" + count        // TODO localise

        val formattedTime = String.format("%02d:%02d", time / 60, time % 60)

        // find and display small icon for unit (will display a placeholder if no icon was found)
        val iconResId = database.getSmallIcon(gameItemID)

        return BuildItemViewModel(itemName, formattedTime, iconResId)
    }

}

