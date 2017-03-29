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
    private Rect insets;

    public WindowInsetsCapturingView(Context context) {
        super(context);
    }

    public WindowInsetsCapturingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WindowInsetsCapturingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        this.insets = new Rect(insets.left, insets.top, insets.right, insets.bottom);
        return super.fitSystemWindows(insets);
    }

    public Rect getInsets() {
        return insets;
    }
}
