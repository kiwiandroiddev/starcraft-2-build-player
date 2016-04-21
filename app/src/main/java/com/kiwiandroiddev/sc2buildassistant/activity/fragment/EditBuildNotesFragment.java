package com.kiwiandroiddev.sc2buildassistant.activity.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.BuildEditorTabView;
import com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys;
import com.kiwiandroiddev.sc2buildassistant.activity.dialog.InsertLinkDialog;
import com.kiwiandroiddev.sc2buildassistant.activity.dialog.PreviewNotesDialog;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build;

/**
 * Provides a UI for editing the free-form notes of a particular build order.
 * These notes can include HTML tags for formatting. Adds an action item for
 * previewing the resulting HTML in a dialog.
 * 
 * At the moment it is just one big text input widget. In future it could be
 * expanded with more editor tools such as Undo/Redo, text formatting, etc.
 * 
 * @author matt
 *
 */
public class EditBuildNotesFragment extends Fragment implements BuildEditorTabView {
	
	private static final String TAG = "EditBuildNotesFragment";
	private EditText mNotes;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);	// we want to add action bar items
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.edit_notes_menu, menu);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case R.id.menu_preview:
        		previewClicked();
        		return true;     	
        }
        return super.onOptionsItemSelected(item);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO butterknife-ify
		View v = inflater.inflate(R.layout.fragment_edit_build_notes, container, false);
		mNotes = (EditText) v.findViewById(R.id.edit_notes);
		
		Button boldButton = (Button) v.findViewById(R.id.boldButton);
		boldButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				basicTagButtonClicked("b");
			}
		});
		
		Button PButton = (Button) v.findViewById(R.id.PButton);
		PButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				basicTagButtonClicked("p");
			}
		});
		
		Button h1Button = (Button) v.findViewById(R.id.h1Button);
		h1Button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				basicTagButtonClicked("h1");
			}
		});
		
		Button h2Button = (Button) v.findViewById(R.id.h2Button);
		h2Button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				basicTagButtonClicked("h2");
			}
		});
		
		Button h3Button = (Button) v.findViewById(R.id.h3Button);
		h3Button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				basicTagButtonClicked("h3");
			}
		});
		
		Button brButton = (Button) v.findViewById(R.id.brButton);
		brButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				insertTag("<br />");
			}
		});
		
		Button aButton = (Button) v.findViewById(R.id.aButton);
		aButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addLinkButtonClicked();
			}
		});
		aButton.setText(Html.fromHtml("<u>A</u>"));
		
		// populate fields from build object passed in
		if (savedInstanceState == null) {			
			Build build = (Build) getArguments().getSerializable(IntentKeys.KEY_BUILD_OBJECT);
			mNotes.setText(build.getNotes());
		} // nothing to do in 'else' case since android saves and restores View states itself
		
		return v;
	}
	
	public String getNotes() {
		return mNotes.getText().toString();
	}
	
	private void previewClicked() {
		Intent i = new Intent(getActivity(), PreviewNotesDialog.class);
        i.putExtra(PreviewNotesDialog.KEY_NOTES_HTML, getNotes());
        startActivity(i);
	}
	
	// ========================================================================
	// Text formatting button callbacks
	// ========================================================================
	
	private void basicTagButtonClicked(String qualifier) {
		String openingTag = "<"+qualifier+">";
		String closingTag = "</"+qualifier+">";
		insertTags(openingTag, closingTag);
	}
	
	private void addLinkButtonClicked() {
		String label = "";
		String url = "";
		
		// use existing selection as label, if applicable
		int start = Math.min(mNotes.getSelectionStart(), mNotes.getSelectionEnd());
		int end = Math.max(mNotes.getSelectionStart(), mNotes.getSelectionEnd());
		
		if (start != end) {
			label = mNotes.getText().subSequence(start, end).toString();
		}
		
		Intent intent = new Intent(getActivity(), InsertLinkDialog.class);
		intent.putExtra(InsertLinkDialog.KEY_LABEL, label);
		intent.putExtra(InsertLinkDialog.KEY_URL, url);
		intent.putExtra(InsertLinkDialog.KEY_START, start);
		intent.putExtra(InsertLinkDialog.KEY_END, end);
		startActivityForResult(intent, 0);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (data.hasExtra(InsertLinkDialog.EXTRA_LINK_HTML)) {
				int start = Math.min(mNotes.getSelectionStart(), mNotes.getSelectionEnd());
				int end = Math.max(mNotes.getSelectionStart(), mNotes.getSelectionEnd());
				
				if (data.hasExtra(InsertLinkDialog.EXTRA_START)) {
					start = data.getExtras().getInt(InsertLinkDialog.EXTRA_START);
				}
				if (data.hasExtra(InsertLinkDialog.EXTRA_END)) {
					end = data.getExtras().getInt(InsertLinkDialog.EXTRA_END);
				}
				
				String html = data.getExtras().getString(InsertLinkDialog.EXTRA_LINK_HTML);
				mNotes.getText().replace(start, end, html);
//				insertTag(html);
			}
		}
	}
	
	/**
	 * Inserts XML tags into the text field at the current cursor position.
	 * If some text is currently selected it will be wrapped in the XML tags,
	 * otherwise they will appear on either side of the cursor.
	 */
	private void insertTags(String openingTag, String closingTag) {
		int start = Math.min(mNotes.getSelectionStart(), mNotes.getSelectionEnd());
		int end = Math.max(mNotes.getSelectionStart(), mNotes.getSelectionEnd());
		
		// order is important here
		mNotes.getText().replace(end, end, closingTag);
		mNotes.getText().replace(start, start, openingTag);
		
		int newStart = start + openingTag.length();
		
		if (start != end) {
			int diff = end - start;
			mNotes.setSelection(newStart + diff);
		} else {
			mNotes.setSelection(newStart);
		}
	}
	
	/**
	 * Inserts a single XML tag (or any string really) at the cursor position,
	 * then moves the cursor to the end of the newly inserted chunk.
	 * @param tag
	 */
	private void insertTag(String tag) {
		int end = Math.max(mNotes.getSelectionStart(), mNotes.getSelectionEnd());
		mNotes.getText().replace(end, end, tag);
		mNotes.setSelection(end + tag.length());
	}

	@Override
	public boolean requestsAddButton() {
		return false;
	}

	@Override
	public void onAddButtonClicked() {}
}
