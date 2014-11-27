package com.kiwiandroiddev.sc2buildassistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

//import com.google.analytics.tracking.android.EasyTracker;

public class InsertLinkDialog extends Activity {
	
	// start and end cursor positions to be passed back to calling
	// activity unchanged (for convenience of replacing existing selection)
	public static final String KEY_START = "mStart";
	public static final String KEY_END = "mEnd";
	public static final String KEY_LABEL = "mLabel";
	public static final String KEY_URL = "mUrl";
	
	// resulting HTML string from this activity, if successful
	public static final String EXTRA_LINK_HTML = "com.kiwiandroiddev.extra.LINK_HTML";
	public static final String EXTRA_START = "com.kiwiandroiddev.extra.START";
	public static final String EXTRA_END = "com.kiwiandroiddev.extra.END";
	
	private EditText mLabel;
	private EditText mUrl;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dialog_insert_link);
		this.setTitle(R.string.dlg_insert_link_title);
		
		mLabel = (EditText) findViewById(R.id.labelEditText);
		mUrl = (EditText) findViewById(R.id.urlEditText);
		
		if (getIntent().getExtras().containsKey(KEY_LABEL)) {
			String label = getIntent().getStringExtra(KEY_LABEL);
			if (label != null && !label.matches("")) {
				mLabel.setText(label);
				mUrl.requestFocus();
			}
		}
		
		if (getIntent().getExtras().containsKey(KEY_URL)) {
			String url = getIntent().getStringExtra(KEY_URL);
			if (url != null && !url.matches(""))
				mUrl.setText(getIntent().getStringExtra(KEY_URL));
		}
		
		Button okBtn = (Button) findViewById(R.id.okButton);
		okBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// ..
				Intent data = new Intent();
				data.putExtra(EXTRA_LINK_HTML, buildHtml());
				
				if (getIntent().hasExtra(KEY_START))
					data.putExtra(EXTRA_START, getIntent().getExtras().getInt(KEY_START));
				if (getIntent().hasExtra(KEY_END))
					data.putExtra(EXTRA_END, getIntent().getExtras().getInt(KEY_END));
				
				setResult(Activity.RESULT_OK, data);
				InsertLinkDialog.this.finish();
			}
		});
		
		Button cancelBtn = (Button) findViewById(R.id.cancelButton);
		cancelBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				InsertLinkDialog.this.finish();
			}
		});
	}
	
	private String buildHtml() {
		String html = "<a href=\""
				+ mUrl.getText().toString()
				+ "\">"
				+ mLabel.getText().toString()
				+ "</a>";
		return html;
	}
	
    @Override
    public void onStart() {
    	super.onStart();
//    	EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
    	super.onStop();
//    	EasyTracker.getInstance().activityStop(this);
    }
}
