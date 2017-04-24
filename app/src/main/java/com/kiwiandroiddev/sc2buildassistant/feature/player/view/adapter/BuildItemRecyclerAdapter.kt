package com.kiwiandroiddev.sc2buildassistant.feature.player.view.adapter

import android.content.Context
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
class BuildItemRecyclerAdapter(val context: Context) : RecyclerView.Adapter<BuildItemRecyclerAdapter.BuildItemViewHolder>() {

    private val database: DbAdapter by lazy { (context.applicationContext as MyApplication).db!! }

    var buildItems: List<BuildItem>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = buildItems?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildItemViewHolder {
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.build_item_row, parent, false)
        return BuildItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BuildItemViewHolder, position: Int) {
        val viewModel = itemViewModelForPosition(position)
        with(holder) {
            name.text = viewModel.name
            time.text = viewModel.time
            icon.setImageResource(viewModel.iconResId)
        }
    }

    class BuildItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.unit_label) as TextView
        val time: TextView = itemView.findViewById(R.id.time_label) as TextView
        val icon: ImageView = itemView.findViewById(R.id.unit_icon) as ImageView
    }

    private fun itemViewModelForPosition(position: Int) =
            buildItems!![position].mapToViewModel()

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

