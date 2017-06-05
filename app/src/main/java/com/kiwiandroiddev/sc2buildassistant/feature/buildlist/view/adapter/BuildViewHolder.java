package com.kiwiandroiddev.sc2buildassistant.feature.buildlist.view.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.feature.buildlist.presentation.model.BuildViewModel;

/**
 * Created by Matt Clarke on 5/06/17.
 */
public final class BuildViewHolder extends RecyclerView.ViewHolder {

    public interface BuildViewHolderClickListener {
        void onBuildClicked(BuildViewHolder buildViewHolder);

        void onBuildLongClicked(BuildViewHolder buildViewHolder);
    }

    public BuildViewModel viewModel;
    public final TextView nameText;
    private TextView vsRaceText;
    private TextView creationDateText;

    @NonNull
    private final BuildViewHolderClickListener clickListener;

    BuildViewHolder(@NonNull View itemView,
                    @NonNull BuildViewHolderClickListener clickListener) {
        super(itemView);
        nameText = (TextView) itemView.findViewById(R.id.buildName);
        vsRaceText = (TextView) itemView.findViewById(R.id.buildVsRace);
        creationDateText = (TextView) itemView.findViewById(R.id.buildCreationDate);
        this.clickListener = clickListener;
    }

    void bindBuildViewModel(@NonNull BuildViewModel model) {
        viewModel = model;
        nameText.setText(model.getName());
        vsRaceText.setText(model.getVsRace());
        creationDateText.setText(model.getCreationDate());
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onBuildClicked(BuildViewHolder.this);
            }
        });
        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clickListener.onBuildLongClicked(BuildViewHolder.this);
                return true;
            }
        });
    }

    public void unbind() {
        itemView.setOnClickListener(null);
        itemView.setOnLongClickListener(null);
    }

}
