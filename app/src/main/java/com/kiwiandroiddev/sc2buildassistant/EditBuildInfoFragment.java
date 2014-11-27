package com.kiwiandroiddev.sc2buildassistant;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kiwiandroiddev.sc2buildassistant.DbAdapter.Expansion;
import com.kiwiandroiddev.sc2buildassistant.DbAdapter.Faction;

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
public class EditBuildInfoFragment extends Fragment {
	
	private static final String TAG = "EditBuildInfoFragment";
	private static final String KEY_EXPANSION_SELECTION = "mExpansionSelection";
	private static final String KEY_FACTION_SELECTION = "mFactionSelection";
	private static final String KEY_VS_FACTION_SELECTION = "mVsFactionSelection";
	
	private EditBuildInfoListener mCallback;
	private IcsSpinner mExpansionSpinner;
	private IcsSpinner mFactionSpinner;
	private IcsSpinner mVsFactionSpinner;
	private TextView mTitle;
	private TextView mSourceTitle;
	private TextView mSourceURL;
	private TextView mAuthor;
	
	public interface EditBuildInfoListener {
		public void onFactionSelectionChanged(DbAdapter.Faction selection);
		public void onTitleChanged(String newTitle);
		// ..
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "EditBuildInfoFragment.onCreateView() called with savedInstanceState = " + savedInstanceState);
		View v = inflater.inflate(R.layout.fragment_edit_build_info, container, false);
        
		// save references to views as they'll be needed later
		mExpansionSpinner = (IcsSpinner) v.findViewById(R.id.edit_expansion_spinner);
		mFactionSpinner = (IcsSpinner) v.findViewById(R.id.edit_faction_spinner);
		mVsFactionSpinner = (IcsSpinner) v.findViewById(R.id.edit_vs_faction_spinner);
		mTitle = (TextView) v.findViewById(R.id.edit_title);
		mSourceTitle = (TextView) v.findViewById(R.id.edit_source_title);
		mSourceURL = (TextView) v.findViewById(R.id.edit_source_url);
		mAuthor = (TextView) v.findViewById(R.id.edit_author);
		
		// add spinner items
		ActionBar actionBar = getActivity().getActionBar();
		mExpansionSpinner.setAdapter(new ExpansionSpinnerAdapter(actionBar.getThemedContext()));
		mFactionSpinner.setAdapter(new FactionSpinnerAdapter(actionBar.getThemedContext(), false));
		mVsFactionSpinner.setAdapter(new FactionSpinnerAdapter(actionBar.getThemedContext(), true));
				
		// get initial selections from fragment args or savedInstanceState
		if (savedInstanceState == null) {			
			// populate fields from an existing build object
			Build build = (Build) getArguments().getSerializable(RaceFragment.KEY_BUILD_OBJECT);
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
		mFactionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(IcsAdapterView<?> parent, View view,
					int position, long id) {
				mCallback.onFactionSelectionChanged(DbAdapter.Faction.values()[position]);
			}

			@Override
			public void onNothingSelected(IcsAdapterView<?> parent) {}	
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
		if (mTitle != null)
			return mTitle.getText().toString();
		else
			return null;
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
		
		if (build.getVsFaction() == null)
			mVsFactionSpinner.setSelection(0);
		else
			mVsFactionSpinner.setSelection(build.getVsFaction().ordinal()+1);
	}

	public void setFactionSelection(Faction selection) {
		mFactionSpinner.setSelection(selection.ordinal());
	}
}
