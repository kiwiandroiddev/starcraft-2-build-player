package com.kiwiandroiddev.sc2buildassistant;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class NumberPickerPreference extends DialogPreference {
	
	private EditText mText;
	
    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        setDialogLayoutResource(R.layout.numberpicker_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        
        setDialogIcon(null);
    }
    
    @Override
    protected View onCreateDialogView() {
    	View root = super.onCreateDialogView();
    	mText = (EditText)root.findViewById(R.id.numberpicker_edit);
    	mText.setText(""+this.getPersistedInt(0));
    	return root;
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
        	Log.w(this.toString(), "OK clicked");
        	int value = 0;
        	try {
        		value = Integer.parseInt(mText.getText().toString());
        	} catch (NumberFormatException e) {}
            persistInt(value);
        }
    }
}