package com.kiwiandroiddev.sc2buildassistant.util;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewTreeObserver;

import com.kiwiandroiddev.sc2buildassistant.view.ObservableScrollView;

/**
 * Created by matt on 1/01/15.
 */
public class QuickReturnHandler implements ObservableScrollView.Callbacks {
    private static final int STATE_ONSCREEN = 0;
    private static final int STATE_OFFSCREEN = 1;
    private static final int STATE_RETURNING = 2;

    private View mQuickReturnView;
    private View mPlaceholderView;
    private ObservableScrollView mObservableScrollView;

    private ScrollSettleHandler mScrollSettleHandler = new ScrollSettleHandler();
    private int mMinRawY = 0;
    private int mState = STATE_ONSCREEN;
    private int mQuickReturnHeight;
    private int mMaxScrollY;
    private ViewTreeObserver.OnGlobalLayoutListener mScrollViewGlobalLayoutListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            onScrollChanged(mObservableScrollView.getScrollY());
            mMaxScrollY = mObservableScrollView.computeVerticalScrollRange()
                    - mObservableScrollView.getHeight();
            mQuickReturnHeight = mQuickReturnView.getHeight();
        }
    };

    public QuickReturnHandler(View quickReturnView,
                              View placeholderView,
                              ObservableScrollView scrollView) {

        mQuickReturnView = quickReturnView;
        mPlaceholderView = placeholderView;
        setScrollView(scrollView);
    }

    public void setScrollView(ObservableScrollView scrollView) {
        if (mObservableScrollView != null) {
            mObservableScrollView.setCallbacks(null);
            mObservableScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(mScrollViewGlobalLayoutListener);
            mObservableScrollView = null;
        }

        mObservableScrollView = scrollView;
        mObservableScrollView.setCallbacks(this);
        mObservableScrollView.getViewTreeObserver().addOnGlobalLayoutListener(mScrollViewGlobalLayoutListener);
    }

    public ObservableScrollView getScrollView() {
        return mObservableScrollView;
    }

    @Override
    public void onScrollChanged(int scrollY) {
        scrollY = Math.min(mMaxScrollY, scrollY);

        mScrollSettleHandler.onScroll(scrollY);

        int rawY = mPlaceholderView.getTop() - scrollY;
        int translationY = 0;

        switch (mState) {
            case STATE_OFFSCREEN:
                if (rawY <= mMinRawY) {
                    mMinRawY = rawY;
                } else {
                    mState = STATE_RETURNING;
                }
                translationY = rawY;
                break;

            case STATE_ONSCREEN:
                if (rawY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                translationY = rawY;
                break;

            case STATE_RETURNING:
                translationY = (rawY - mMinRawY) - mQuickReturnHeight;
                if (translationY > 0) {
                    translationY = 0;
                    mMinRawY = rawY - mQuickReturnHeight;
                }

                if (rawY > 0) {
                    mState = STATE_ONSCREEN;
                    translationY = rawY;
                }

                if (translationY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                break;
        }
        mQuickReturnView.animate().cancel();
        mQuickReturnView.setTranslationY(translationY + scrollY);
    }

    @Override
    public void onDownMotionEvent() {
        mScrollSettleHandler.setSettleEnabled(false);
    }

    @Override
    public void onUpOrCancelMotionEvent() {
        mScrollSettleHandler.setSettleEnabled(true);
        mScrollSettleHandler.onScroll(mObservableScrollView.getScrollY());
    }

    private class ScrollSettleHandler extends Handler {
        private static final int SETTLE_DELAY_MILLIS = 100;

        private int mSettledScrollY = Integer.MIN_VALUE;
        private boolean mSettleEnabled;

        public void onScroll(int scrollY) {
            if (mSettledScrollY != scrollY) {
                // Clear any pending messages and post delayed
                removeMessages(0);
                sendEmptyMessageDelayed(0, SETTLE_DELAY_MILLIS);
                mSettledScrollY = scrollY;
            }
        }

        public void setSettleEnabled(boolean settleEnabled) {
            mSettleEnabled = settleEnabled;
        }

        @Override
        public void handleMessage(Message msg) {
            // Handle the scroll settling.
            if (STATE_RETURNING == mState && mSettleEnabled) {
                int mDestTranslationY;
                if (mSettledScrollY - mQuickReturnView.getTranslationY() > mQuickReturnHeight / 2) {
                    mState = STATE_OFFSCREEN;
                    mDestTranslationY = Math.max(
                            mSettledScrollY - mQuickReturnHeight,
                            mPlaceholderView.getTop());
                } else {
                    mDestTranslationY = mSettledScrollY;
                }

                mMinRawY = mPlaceholderView.getTop() - mQuickReturnHeight - mDestTranslationY;
                mQuickReturnView.animate().translationY(mDestTranslationY);
            }
            mSettledScrollY = Integer.MIN_VALUE; // reset
        }
    }
}
