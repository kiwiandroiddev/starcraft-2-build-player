package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
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
import java.util.Collections;

/**
 * Adapter for displaying build items in an editable list (as opposed to BuildItemAdapter
 * which is for showing a read-only list of build items, e.g. on the Playback screen).
 * The list is editable in the sense that build items can added, removed and reordered.
 *
 * BUG: views not updating on move (red time text when out of place)
 *
 * Created by matt on 4/10/15.
 */
public class EditBuildItemRecyclerAdapter extends RecyclerView.Adapter<EditBuildItemViewHolder>
        implements ItemTouchEventListener {

    private final Context mContext;
    private final DbAdapter mDb;
    private final OnStartDragListener mOnStartDragListener;
    private final OnBuildItemClickedListener mOnBuildItemClickedListener;
    private final ArrayList<BuildItem> mBuildItems;

    public EditBuildItemRecyclerAdapter(Context context,
                                        OnStartDragListener onStartDragListener,
                                        OnBuildItemClickedListener onBuildItemClickedListener, ArrayList<BuildItem> buildItems) {
        mContext = context;
        mOnStartDragListener = onStartDragListener;
        mOnBuildItemClickedListener = onBuildItemClickedListener;
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
    public void onBindViewHolder(final EditBuildItemViewHolder holder, final int position) {
        final BuildItem item = mBuildItems.get(position);

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

        // stub - use whole row
        holder.icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnBuildItemClickedListener.onBuildItemClicked(item, position);
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
        Collections.swap(mBuildItems, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    /**
     * Inserts the specified object at the specified index in the array.
     *
     * @param item The build item to insert into the array.
     * @param index The index at which the object must be inserted.
     */
    public void insert(@NonNull BuildItem item, int index) {
        mBuildItems.add(index, item);
        notifyItemInserted(index);
    }

    /**
     * Attempts to insert a new build item into the correct position in the list
     * based on its time value. The existing item at that position (if any) and
     * all subsequent items will be shifted along to the right by one. Note:
     * assumes build items are already ordered by time (which they may well not
     * be). Insertion takes O(n) time.
     *
     * @param item
     *            new build item to add
     * @return index of array where item was inserted.
     */
    public int autoInsert(@NonNull BuildItem item) {
        for (int i=0; i<mBuildItems.size(); i++) {
            if (item.getTime() < mBuildItems.get(i).getTime()) {
                mBuildItems.add(i, item);
                notifyItemInserted(i);
                return i;
            }
        }

        // item had a greater time value than all others, append it to the end
        mBuildItems.add(item);
        notifyItemInserted(mBuildItems.size()-1);
        return (mBuildItems.size()-1);
    }

    /**
     * Replaces the build item at specified index with a new one,
     * if the new and existing build items aren't equivalent.
     *
     * @param item		new BuildItem
     * @param index		position in arraylist
     */
    public void replace(@NonNull BuildItem item, int index) {
        BuildItem existingItem = mBuildItems.get(index);

        if (item == existingItem || item.equals(existingItem)) {
            return;
        }

        mBuildItems.set(index, item);
        notifyItemChanged(index);
    }

    public ArrayList<BuildItem> getBuildItems() {
        return mBuildItems;
    }
}
