package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.model.BuildItem;

import java.util.ArrayList;

/**
 * Adapter for displaying build items in an editable list (as opposed to BuildItemAdapter
 * which is for showing a read-only list of build items, e.g. on the Playback screen).
 * The list is editable in the sense that build items can added, removed and reordered.
 *
 * Created by matt on 4/10/15.
 */
public class EditBuildItemRecyclerAdapter extends RecyclerView.Adapter<EditBuildItemViewHolder>
        implements ItemTouchEventListener {

    private final Context mContext;
    private final DbAdapter mDb;
    private final OnStartDragListener mOnStartDragListener;
    private final ArrayList<BuildItem> mBuildItems;

    public EditBuildItemRecyclerAdapter(Context context,
                                        OnStartDragListener onStartDragListener,
                                        ArrayList<BuildItem> buildItems) {
        mContext = context;
        mOnStartDragListener = onStartDragListener;
        mBuildItems = buildItems;

        // get a reference to the global DB instance. TODO: inject this reference via DI container
        MyApplication app = (MyApplication) context.getApplicationContext();
        mDb = app.getDb();
    }

    /**
     * Create a new ViewHolder instance, not yet bound to a particular model object (BuildItem)
     *
     * @param parent
     * @param viewType
     * @return new BuildItemViewHolder
     */
    @Override
    public EditBuildItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_build_item_row, parent, false);
        return new EditBuildItemViewHolder(view);
    }

    /**
     * Update a ViewHolder's views to reflect the state of a particular BuildItem.
     *
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(final EditBuildItemViewHolder holder, int position) {
        BuildItem item = mBuildItems.get(position);

        // work out if this build item is in the wrong position based on its time
        boolean outOfPosition = false;
        if (position > 0 && position < mBuildItems.size()) {
            BuildItem previousItem = mBuildItems.get(position - 1);
            if (item.getTime() < previousItem.getTime())
                outOfPosition = true;
        }

        // set the main label (either unit name or message text)
        if (item.getText() == null || item.getText().matches("")) {
            String s = mDb.getNameString(item.getGameItemID());
            holder.mainLabel.setText(s);
        } else {
            holder.mainLabel.setText(item.getText());
        }

        // display unit count (if something other than 1)
        if (item.getCount() > 1) {
            holder.count.setText("x" + item.getCount());    // TODO localise this
            holder.count.setVisibility(View.VISIBLE);
        } else {
            holder.count.setVisibility(View.GONE);
        }

        // display ability target
        if (item.getTarget() == null || item.getTarget().matches("")) {
            holder.targetLabel.setVisibility(View.GONE);
        } else {
            String s = mDb.getNameString(item.getTarget());
            holder.targetLabel.setText("on " + s);      // TODO localise this
            holder.targetLabel.setVisibility(View.VISIBLE);
        }

        // find and display small icon for unit (will display a placeholder if no icon was found)
        holder.icon.setImageResource(mDb.getSmallIcon(item.getGameItemID()));

        // show the unit's time in the build queue
        int timeSec = item.getTime();
        holder.time.setText(String.format("%02d:%02d", timeSec / 60, timeSec % 60));
        if (outOfPosition) {
            holder.time.setTextColor(Color.RED);
        } else {
            holder.time.setTextColor(mContext.getResources().getColor(android.R.color.secondary_text_dark));
        }

        // pass touches on the drag handle up to the adapter's parent so it can take
        // appropriate action to start the drag operation
        holder.handle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) ==
                        MotionEvent.ACTION_DOWN) {
                    mOnStartDragListener.onStartDrag(holder);
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mBuildItems.size();
    }

    @Override
    public void onItemDismiss(int position) {
        mBuildItems.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        BuildItem prev = mBuildItems.remove(fromPosition);
        mBuildItems.add(toPosition > fromPosition ? toPosition - 1 : toPosition, prev);
        notifyItemMoved(fromPosition, toPosition);
    }

    public ArrayList<BuildItem> getBuildItems() {
        return mBuildItems;
    }
}
