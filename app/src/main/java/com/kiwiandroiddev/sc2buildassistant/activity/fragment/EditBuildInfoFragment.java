package com.kiwiandroiddev.sc2buildassistant.activity.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.BuildEditorTabView;
import com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Expansion;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Faction;
import com.kiwiandroiddev.sc2buildassistant.adapter.ExpansionSpinnerAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.FactionSpinnerAdapter;
import com.kiwiandroiddev.sc2buildassistant.model.Build;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Fragment for editing the basic information of a build order
 * including title, faction, expansion level, author, etc.
 * 
 * Fragment communication reference used:
 * https://developer.android.com/training/basics/fragments/communicating.html
 * 
 * @author matt
 *
 */
public class EditBuildInfoFragment extends Fragment implements BuildEditorTabView {
	
	private static final String TAG = "EditBuildInfoFragment";
	private static final String KEY_EXPANSION_SELECTION = "mExpansionSelection";
	private static final String KEY_FACTION_SELECTION = "mFactionSelection";
	private static final String KEY_VS_FACTION_SELECTION = "mVsFactionSelection";
	
	private EditBuildInfoListener mCallback;

	@InjectView(R.id.edit_expansion_spinner) Spinner mExpansionSpinner;
	@InjectView(R.id.edit_faction_spinner) Spinner mFactionSpinner;
	@InjectView(R.id.edit_vs_faction_spinner) Spinner mVsFactionSpinner;
	@InjectView(R.id.edit_title) TextView mTitle;
	@InjectView(R.id.edit_source_title) TextView mSourceTitle;
	@InjectView(R.id.edit_source_url) TextView mSourceURL;
	@InjectView(R.id.edit_author) TextView mAuthor;

	public interface EditBuildInfoListener {
		void onFactionSelectionChanged(DbAdapter.Faction selection);
		void onTitleChanged(String newTitle);
		// ..
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Timber.d(TAG, "EditBuildInfoFragment.onCreateView() called with savedInstanceState = " + savedInstanceState);
		View v = inflater.inflate(R.layout.fragment_edit_build_info, container, false);
        ButterKnife.inject(this, v);

		// add spinner items
		android.support.v7.app.ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
		mExpansionSpinner.setAdapter(new ExpansionSpinnerAdapter(actionBar.getThemedContext()));
		mFactionSpinner.setAdapter(new FactionSpinnerAdapter(actionBar.getThemedContext(), false));
		mVsFactionSpinner.setAdapter(new FactionSpinnerAdapter(actionBar.getThemedContext(), true));

		// get initial selections from fragment args or savedInstanceState
		if (savedInstanceState == null) {
			// populate fields from an existing build object
			Build build = (Build) getArguments().getSerializable(IntentKeys.KEY_BUILD_OBJECT);
			populateFields(build);
		} else {
			// restore IcsSpinner selections manually, android system handles the other Views itself
			if (savedInstanceState.containsKey(KEY_EXPANSION_SELECTION))
				mExpansionSpinner.setSelection(savedInstanceState.getInt(KEY_EXPANSION_SELECTION));

			if (savedInstanceState.containsKey(KEY_FACTION_SELECTION))
				mFactionSpinner.setSelection(savedInstanceState.getInt(KEY_FACTION_SELECTION));

			if (savedInstanceState.containsKey(KEY_VS_FACTION_SELECTION))
				mVsFactionSpinner.setSelection(savedInstanceState.getInt(KEY_VS_FACTION_SELECTION));
		}

		// tell parent activity when selections change
		mFactionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				mCallback.onFactionSelectionChanged(DbAdapter.Faction.values()[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
		mTitle.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {	}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) { }

			@Override
			public void afterTextChanged(Editable s) {
				mCallback.onTitleChanged(s.toString());
			}
		});

		return v;
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (EditBuildInfoListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement EditBuildInfoListener");
        }
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_EXPANSION_SELECTION, mExpansionSpinner.getSelectedItemPosition());
		outState.putInt(KEY_FACTION_SELECTION, mFactionSpinner.getSelectedItemPosition());
		outState.putInt(KEY_VS_FACTION_SELECTION, mVsFactionSpinner.getSelectedItemPosition());
	}

	public String getTitle() {
		// Got a user reporting a nullpointerexception here - probably from exiting editbuildactivity
		// before view for this fragment has been created
		if (mTitle != null) {
			return mTitle.getText().toString();
		} else {
			return null;
		}
	}

	public Faction getFaction() {
		return (Faction) mFactionSpinner.getSelectedItem();
	}

	public Faction getVsFaction() {
		// TODO: support "Any/All" Factions in spinner!
		return (Faction) mVsFactionSpinner.getSelectedItem();
	}

	public Expansion getExpansion() {
		return (Expansion) mExpansionSpinner.getSelectedItem();
	}

	public String getSourceTitle() {
		return mSourceTitle.getText().toString();
	}

	public String getSourceURL() {
		return mSourceURL.getText().toString();
	}

	public String getAuthor() {
		return mAuthor.getText().toString();
	}

	// ..

	private void populateFields(Build build) {
		mExpansionSpinner.setSelection(build.getExpansion().ordinal());
		mFactionSpinner.setSelection(build.getFaction().ordinal());
		mTitle.setText(build.getName());
		mSourceTitle.setText(build.getSourceTitle());
		mSourceURL.setText(build.getSourceURL());
		mAuthor.setText(build.getAuthor());

		if (build.getVsFaction() == null) {
            mVsFactionSpinner.setSelection(0);
        } else {
            mVsFactionSpinner.setSelection(build.getVsFaction().ordinal() + 1);
        }
	}

	public void setFactionSelection(Faction selection) {
		mFactionSpinner.setSelection(selection.ordinal());
	}

	@Override
	public boolean requestsAddButton() {
		return false;
	}

	@Override
	public void onAddButtonClicked() { }
}
