/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.model.BuildItem;
import com.kiwiandroiddev.sc2buildassistant.util.ViewUtils;

import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class DraggableSwipeableBuildItemAdapter
        extends RecyclerView.Adapter<DraggableSwipeableBuildItemAdapter.BuildItemViewHolder>
        implements DraggableItemAdapter<DraggableSwipeableBuildItemAdapter.BuildItemViewHolder>,
        SwipeableItemAdapter<DraggableSwipeableBuildItemAdapter.BuildItemViewHolder> {
    private final List<BuildItem> mBuildItems;

    public DraggableSwipeableBuildItemAdapter(List<BuildItem> buildItems) {
        setHasStableIds(true);      // SwipeableItemAdapter requires stable IDs
        mBuildItems = buildItems;
    }

    @Override
    public BuildItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View v = inflater.inflate(R.layout.edit_build_item_row, parent, false);
        return new BuildItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(BuildItemViewHolder holder, int position) {
        BuildItem question = mBuildItems.get(position);
        holder.bindTo(question);
    }

    @Override
    public int getItemCount() {
        return mBuildItems.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int onGetSwipeReactionType(BuildItemViewHolder holder, int position, int x, int y) {
        if (onCheckCanStartDrag(holder, position, x, y)) {
            return RecyclerViewSwipeManager.REACTION_CAN_NOT_SWIPE_BOTH;
        } else {
            return RecyclerViewSwipeManager.REACTION_CAN_SWIPE_LEFT |
                    RecyclerViewSwipeManager.REACTION_CAN_SWIPE_RIGHT;
        }
    }

    @Override
    public void onSetSwipeBackground(BuildItemViewHolder holder, int position, int type) {
//        int bgRes = 0;
//        switch (type) {
//            case RecyclerViewSwipeManager.DRAWABLE_SWIPE_NEUTRAL_BACKGROUND:
//                bgRes = R.drawable.bg_swipe_item_neutral;
//                break;
//            case RecyclerViewSwipeManager.DRAWABLE_SWIPE_LEFT_BACKGROUND:
//                bgRes = R.drawable.bg_swipe_item_no;
//                break;
//            case RecyclerViewSwipeManager.DRAWABLE_SWIPE_RIGHT_BACKGROUND:
//                bgRes = R.drawable.bg_swipe_item_yes;
//                break;
//        }
//
//        holder.itemView.setBackgroundResource(bgRes);
        holder.itemView.setBackgroundResource(R.color.orange);
    }

    @Override
    public int onSwipeItem(BuildItemViewHolder questionViewHolder, int position, int result) {
        // stub
        return RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM;
    }

    @Override
    public void onPerformAfterSwipeReaction(BuildItemViewHolder buildItemViewHolder, int position, int result, int reaction) {
        if (reaction == RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM) {
            mBuildItems.remove(position);
            notifyItemRemoved(position);
        }
    }

    @Override
    public boolean onCheckCanStartDrag(BuildItemViewHolder holder, int position, int x, int y) {
        // x, y --- relative from the itemView's top-left
        final View containerView = holder.container;
        final View dragHandleView = holder.dragHandle;

        final int offsetX = containerView.getLeft() + (int) (ViewCompat.getTranslationX(containerView) + 0.5f);
        final int offsetY = containerView.getTop() + (int) (ViewCompat.getTranslationY(containerView) + 0.5f);

        return ViewUtils.hitTest(dragHandleView, x - offsetX, y - offsetY);
    }

    @Override
    public ItemDraggableRange onGetItemDraggableRange(BuildItemViewHolder holder, int position) {
        // no drag-sortable range specified
        return null;
    }

    @Override
    public void onMoveItem(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        try {
            Collections.swap(mBuildItems, fromPosition, toPosition);
        } catch (IndexOutOfBoundsException e) {
            Timber.e(e, "couldn't move item");
            return;
        }

        notifyItemMoved(fromPosition, toPosition);
    }
    public static class BuildItemViewHolder extends AbstractSwipeableItemViewHolder {
        private BuildItem mBuildItem;
        private final FrameLayout container;
        public View dragHandle;
        private TextView main;
        private TextView target;
        private TextView time;
        private TextView count;
        private ImageView icon;

        private DbAdapter mDb;      // stub

        public BuildItemViewHolder(View itemView) {
            super(itemView);
            container = (FrameLayout) itemView.findViewById(R.id.container);
            dragHandle = itemView.findViewById(R.id.drag_handle);
            main = (TextView)itemView.findViewById(R.id.main_label);
            target = (TextView)itemView.findViewById(R.id.target_label);
            time = (TextView)itemView.findViewById(R.id.time_label);
            count = (TextView)itemView.findViewById(R.id.count_label);
            icon = (ImageView)itemView.findViewById(R.id.unit_icon);
        }

        /**
         * The viewholder itself is responsible for updating its child views to reflect
         * a new bound model object (rather than the adapter).
         */
        public void bindTo(BuildItem item) {
            mBuildItem = item;

            // get a reference to the global DB instance (stub)
            MyApplication app = (MyApplication) itemView.getContext().getApplicationContext();
            this.mDb = app.getDb();

            // work out if this build item is in the wrong position based on its time
            boolean outOfPosition = false;
//            if (position > 0 && position < mBuildItems.size()) {
//                BuildItem previousItem = mBuildItems.get(position - 1);
//                if (item.getTime() < previousItem.getTime())
//                    outOfPosition = true;
//            }

//		Timber.d(this.toString(), String.format("in getView(), positon=%s, convertView=%s, parent=%s, item=%s, main=%s, target=%s, item.getText()=%s",
//				position, convertView, parent, item, main, target, item.getText()));

            // set the main label (either unit name or message text)
            if (item.getText() == null || item.getText().matches("")) {
                String s = mDb.getNameString(item.getGameItemID());
                main.setText(s);
            } else {
                main.setText(item.getText());
            }

            // display unit count (if something other than 1)
            if (item.getCount() > 1) {
                count.setText("x" + item.getCount());
                count.setVisibility(View.VISIBLE);
            } else {
                count.setVisibility(View.GONE);
            }

            // display ability target
            if (item.getTarget() == null || item.getTarget().matches("")) {
                target.setVisibility(View.GONE);
            } else {
//			Timber.d(this.toString(), "item = " + item + ", target = " + item.getTarget() + ", target label vis = " + target.getVisibility());
                String s = mDb.getNameString(item.getTarget());
                target.setText("on " + s);
                target.setVisibility(View.VISIBLE);
            }

            // find and display small icon for unit (will display a placeholder if no icon was found)
            icon.setImageResource(mDb.getSmallIcon(item.getGameItemID()));

            // show the unit's time in the build queue
            int timeSec = item.getTime();
            time.setText(String.format("%02d:%02d", timeSec / 60, timeSec % 60));
            if (outOfPosition)
                time.setTextColor(Color.RED);
            else
                time.setTextColor(itemView.getResources().getColor(android.R.color.secondary_text_dark));


            // set background resource (target view ID: container)
            // set background resource (target view ID: container)
//            final int dragState = holder.getDragStateFlags();
//            final int swipeState = holder.getSwipeStateFlags();
//
//            if (((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_UPDATED) != 0) ||
//                    ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_IS_UPDATED) != 0)) {
//                int bgResId;
//
//                if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_ACTIVE) != 0) {
//                    bgResId = R.drawable.bg_item_dragging_active_state;
//
//                    // need to clear drawable state here to get correct appearance of the dragging item.
//                    DrawableUtils.clearState(holder.mContainer.getForeground());
//                } else if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_DRAGGING) != 0) {
//                    bgResId = R.drawable.bg_item_dragging_state;
//                } else if ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_IS_ACTIVE) != 0) {
//                    bgResId = R.drawable.bg_item_swiping_active_state;
//                } else if ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_SWIPING) != 0) {
//                    bgResId = R.drawable.bg_item_swiping_state;
//                } else {
//                    bgResId = R.drawable.bg_item_normal_state;
//                }
//
//                holder.mContainer.setBackgroundResource(bgResId);
//            }
        }

        @Override
        public View getSwipeableContainerView() {
            return container;
        }
    }
}
