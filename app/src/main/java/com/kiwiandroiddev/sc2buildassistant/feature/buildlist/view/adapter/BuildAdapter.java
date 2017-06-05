package com.kiwiandroiddev.sc2buildassistant.feature.buildlist.view.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.feature.buildlist.presentation.model.BuildViewModel;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Matt Clarke on 5/06/17.
 */
public class BuildAdapter extends RecyclerView.Adapter<BuildViewHolder> {

    private static final int BUILD_ROW_TYPE = 0;
    private static final int FOOTER_ROW_TYPE = 1;
    private final Context context;
    private final BuildViewHolder.BuildViewHolderClickListener itemClickListener;
    private List<BuildViewModel> mBuildViewModelList;

    public BuildAdapter(Context context, Cursor cursor, BuildViewHolder.BuildViewHolderClickListener itemClickListener) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        mBuildViewModelList = new ArrayList<>();
        if (!cursor.moveToFirst()) {
            return;
        }

        do {
            mBuildViewModelList.add(getBuildViewModelAtCursor(cursor));
        } while (cursor.moveToNext());
    }

    @NonNull
    private BuildViewModel getBuildViewModelAtCursor(Cursor cursor) {
        long buildId = cursor.getLong(cursor.getColumnIndex(DbAdapter.KEY_BUILD_ORDER_ID));

        String buildName = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_NAME));

        String dateStr = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CREATED));
        String formattedDateStr = "";
        if (!TextUtils.isEmpty(dateStr)) {
            Date date;
            try {
                date = DbAdapter.DATE_FORMAT.parse(dateStr);
                DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
                formattedDateStr = df.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        int vsFactionId = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_VS_FACTION_ID));
        @StringRes int vsFactionStringId = (vsFactionId == 0) ? R.string.race_any : DbAdapter.getFactionName(vsFactionId);
        String vsRaceFormatted = context.getString(R.string.build_row_vs_race_template, context.getString(vsFactionStringId));

        return new BuildViewModel(buildId, buildName, formattedDateStr, vsRaceFormatted);
    }

    @Override
    public BuildViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        @LayoutRes int itemLayout = (viewType == BUILD_ROW_TYPE) ? R.layout.build_row : R.layout.spacer_row;
        View view = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        return new BuildViewHolder(view, itemClickListener);
    }

    @Override
    public void onBindViewHolder(BuildViewHolder viewHolder, int position) {
        switch (getItemViewType(position)) {
            case BUILD_ROW_TYPE:
                viewHolder.bindBuildViewModel(mBuildViewModelList.get(position));
                return;
            case FOOTER_ROW_TYPE:
            default:
                viewHolder.unbind();
        }
    }

    @Override
    public int getItemCount() {
        return mBuildViewModelList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position < mBuildViewModelList.size() ? BUILD_ROW_TYPE : FOOTER_ROW_TYPE;
    }

    @Override
    public String toString() {
        return "BuildAdapter{" +
                "mBuildViewModelList=" + mBuildViewModelList +
                '}';
    }
}
