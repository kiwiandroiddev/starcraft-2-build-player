package com.kiwiandroiddev.sc2buildassistant.activity.dialog;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.kiwiandroiddev.sc2buildassistant.R;

public class PreviewNotesDialog extends Activity {
	
	public static final String KEY_NOTES_HTML = "mNotesHtml";
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dialog_preview_notes);
		this.setTitle(R.string.dlg_preview_notes_title);
		
		if (getIntent().getExtras().containsKey(KEY_NOTES_HTML)) {
			String source = getIntent().getExtras().getString(KEY_NOTES_HTML); 
			TextView notes = (TextView) findViewById(R.id.notesPreviewText);
			notes.setText(Html.fromHtml(source));
			notes.setMovementMethod(LinkMovementMethod.getInstance());	// makes links clickable
		}
		
		Button okBtn = (Button) findViewById(R.id.okButton);
		okBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PreviewNotesDialog.this.finish();
			}
		});
	}
	
    @Override
    public void onStart() {
    	super.onStart();
    	EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
    	super.onStop();
    	EasyTracker.getInstance().activityStop(this);
    }
}
