package com.kiwiandroiddev.sc2buildassistant.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by matt on 28/03/17.
 */
public class WindowInsetsCapturingView extends View {

    public interface OnCapturedWindowInsetsListener {
        void onCapturedWindowInsets(Rect insets);
    }

    private OnCapturedWindowInsetsListener listener;

    public WindowInsetsCapturingView(Context context) {
        super(context);
    }

    public WindowInsetsCapturingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WindowInsetsCapturingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnCapturedWindowInsetsListener(OnCapturedWindowInsetsListener listener) {
        this.listener = listener;
    }

    public void clearOnCapturedWindowInsetsListener() {
        this.listener = null;
    }

    @Override
    public boolean getFitsSystemWindows() {
        return true;
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        if (listener != null) {
            Rect capturedInsets = new Rect(insets.left, insets.top, insets.right, insets.bottom);
            listener.onCapturedWindowInsets(capturedInsets);
        }
        return false;
    }

}
