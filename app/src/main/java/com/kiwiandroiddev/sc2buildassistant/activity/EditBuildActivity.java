package com.kiwiandroiddev.sc2buildassistant.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.kiwiandroiddev.sc2buildassistant.service.JsonBuildService;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.UnoptimizedDeepCopy;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.EditBuildInfoFragment;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.EditBuildInfoFragment.EditBuildInfoListener;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.EditBuildItemsFragment;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.EditBuildNotesFragment;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Expansion;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Faction;
import com.kiwiandroiddev.sc2buildassistant.adapter.EditBuildPagerAdapter;
import com.kiwiandroiddev.sc2buildassistant.model.Build;
import com.kiwiandroiddev.sc2buildassistant.model.BuildItem;

import java.util.ArrayList;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

//import com.google.analytics.tracking.android.EasyTracker;

/**
 * Activity for creating new build orders or editing existing ones. Has three fragments
 * for editing different aspects of the build order - basic information, detailed notes,
 * and the list of build items.
 * 
 * Reference for combining tabs with view pager:
 * https://developer.android.com/training/implementing-navigation/lateral.html
 *
 * TODO: use TabLayout from design support library
 *
 * @author matt
 *
 */
public class EditBuildActivity extends ActionBarActivity implements EditBuildInfoListener {

    @InjectView(android.R.id.content) View mRootView;

    /** Writes a build object to the database in a background task */
	private class WriteBuildTask extends AsyncTask<Void, Void, Boolean> {
		private DbAdapter mDb;
		private Build mBuild;
		private long mBuildId;
		private ProgressDialog mDlg;
		
		public WriteBuildTask(Build build, long buildId) {
			super();
			mDb = ((MyApplication) getApplicationContext()).getDb();
			mBuild = build;
			mBuildId = buildId;
		}
				 
		protected Boolean doInBackground(Void... unused) {
			try {
				mDb.addOrReplaceBuild(mBuild, mBuildId);
				return true;
			} catch (DbAdapter.NameNotUniqueException e) {
				return false;
			}
		}
		
		protected void onPreExecute() {
            mDlg = new ProgressDialog(EditBuildActivity.this);
            mDlg.setMessage("Saving...");       // TODO: localize
            mDlg.setCancelable(false);
            mDlg.setIndeterminate(true);
            mDlg.show();
		}
		
		/**
		 * If write was successful, displays a message, notifies content provider observers and
		 * finishes the activity.
		 * If unsuccessful, displays an error message.
		 */
		protected void onPostExecute(Boolean result) {
			mDlg.hide();
			if (result == true) {
				// notify observers of buildprovider's build table that its contents have changed
				JsonBuildService.notifyBuildProviderObservers(EditBuildActivity.this);
				showMessage(R.string.edit_build_save_successful);
				EditBuildActivity.this.finish();
			} else {
				showMessage(R.string.edit_build_title_already_taken_error);	
			}
		}
	}
	
	public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
	    private Fragment mFragment;
	    private final ActionBarActivity mActivity;
	    private final String mTag;
	    private final Class<T> mClass;
	    private final Build mBuild;

	    /** Constructor used each time a new tab is created.
	      * @param activity  The host Activity, used to instantiate the fragment
	      * @param tag  The identifier tag for the fragment
	      * @param clz  The fragment's Class, used to instantiate the fragment
	      */
	    public TabListener(ActionBarActivity activity, String tag, Class<T> clz, Build build) {
	        mActivity = activity;
	        mTag = tag;
	        mClass = clz;
	        mBuild = build;
	    }

	    /* The following are each of the ActionBar.TabListener callbacks */

	    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
	    	Fragment preInitializedFragment = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);

	        // Check if the fragment is already initialized
	        if (mFragment == null && preInitializedFragment == null) {
	        	Timber.d(mTag + ": neither fragment nor preinitialized fragment set");
	            // If not, instantiate and add it to the activity
	        	Bundle data = new Bundle();
	    		data.putSerializable(IntentKeys.KEY_BUILD_OBJECT, mBuild);
	            mFragment = Fragment.instantiate(mActivity, mClass.getName(), data);
	            ft.add(android.R.id.content, mFragment, mTag);
	        } else if (mFragment != null) {
	        	Timber.d(mTag + ": fragment already exists, reattaching");
	            // If it exists, simply attach it in order to show it
	            ft.attach(mFragment);
	        } else if (preInitializedFragment != null) {
	        	Timber.d(mTag + ": pre-initialized fragment already exists, reattaching that");
	            ft.attach(preInitializedFragment);
	            mFragment = preInitializedFragment;
	        }
	        
//	        // Check if the fragment is already initialized
//	        if (mFragment == null) {
//	            // If not, instantiate and add it to the activity
//	    		    		
//	    		Bundle data = new Bundle();
//	    		data.putSerializable(IntentKeys.KEY_BUILD_OBJECT, mBuild);
//	            mFragment = Fragment.instantiate(mActivity, mClass.getName(), data);
//	            ft.add(android.R.id.content, mFragment, mTag);
//	        } else {
//	            // If it exists, simply attach it in order to show it
//	            ft.attach(mFragment);
//	        }
	    }

	    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
	        if (mFragment != null) {
	            // Detach the fragment, because another one is being attached
	            ft.detach(mFragment);
	        }
	    }

	    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
	        // User selected the already selected tab. Usually do nothing.
	    }
    }
	
//	public static final int EDIT_BUILD_REQUEST = 128;
	
	private static final String KEY_NEW_BUILD_BOOL = "mCreatingNewBuild";
	private static final String KEY_WORKING_BUILD = "mWorkingBuild";
	private static final String KEY_SELECTED_TAB = "mSelectedTab";
	private static final String TAG = "EditBuildActivity";
	
	private boolean mCreatingNewBuild = false;
	private long mBuildId;		// id of existing build, if any
	private Build mInitialBuild;
	private Build mFragmentSharedBuild;
	private EditBuildPagerAdapter mPagerAdapter;
//	private ViewPager mPager;
	private Faction mCurrentFactionSelection;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Can't have both fullscreen mode and window resizing when the soft keyboard
		// becomes visible (needed so formatting toolbar stays on screen). Known bug
		// in AOSP apparently:
		// https://code.google.com/p/android/issues/detail?id=5497
		
//		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//    	if (!sharedPref.getBoolean(SettingsActivity.KEY_SHOW_STATUS_BAR, false)) {
//			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//					WindowManager.LayoutParams.FLAG_FULLSCREEN);
//    	}
		
		super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ButterKnife.inject(this);

        // Get arguments sent by BuildListActivity - these will determine
        // if we should create a new build or edit an existing one
        DbAdapter.Expansion expansion = null;
        DbAdapter.Faction faction = null;
        if (savedInstanceState == null) {
        	Bundle data = getIntent().getExtras(); 
        	mBuildId = data.containsKey(IntentKeys.KEY_BUILD_ID) ? data.getLong(IntentKeys.KEY_BUILD_ID) : -1;
        	expansion = data.containsKey(IntentKeys.KEY_EXPANSION_ENUM) ? (Expansion) data.getSerializable(IntentKeys.KEY_EXPANSION_ENUM) : null;
        	faction = data.containsKey(IntentKeys.KEY_FACTION_ENUM) ? (Faction) data.getSerializable(IntentKeys.KEY_FACTION_ENUM) : null;
        	
            // Load working copy of existing Build from database or create a new one
            if (mBuildId != -1) {
            	DbAdapter db = ((MyApplication) getApplication()).getDb();
            	mInitialBuild = db.fetchBuild(mBuildId);
            	mCreatingNewBuild = false;
            } else {
            	mInitialBuild = new Build();
            	mInitialBuild.setExpansion(expansion);
            	mInitialBuild.setFaction(faction);
            	mCreatingNewBuild = true;
            }
        } else {
        	// handle resuming saved state
        	mCreatingNewBuild = savedInstanceState.getBoolean(KEY_NEW_BUILD_BOOL);
        	mInitialBuild = (Build) savedInstanceState.getSerializable(KEY_WORKING_BUILD);
        	mBuildId = savedInstanceState.getLong(IntentKeys.KEY_BUILD_ID);
        }
        
        mCurrentFactionSelection = mInitialBuild.getFaction();
        setBackgroundImage(mCurrentFactionSelection);
        
//        if (mInitialBuild.getItems() != null)
//        	Timber.d(this.toString(), "in EditBuildActivity.onCreate(), mInitialBuild items count = " + mInitialBuild.getItems().size());
//        
//		Timber.d(this.toString(), "in EditBuildActivity.onCreate(), mInitialBuild id = " + Integer.toHexString(System.identityHashCode(mInitialBuild)));
        
        // setup action bar for tabs
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//        actionBar.setDisplayShowTitleEnabled(false);

    	mFragmentSharedBuild = (Build) UnoptimizedDeepCopy.copy(mInitialBuild);

    	Timber.d("onCreate() called, num tabs = " + actionBar.getTabCount());
        ActionBar.Tab tab = actionBar.newTab()
                .setText("Info")
                .setTabListener(new TabListener<EditBuildInfoFragment>(
                        this, "info", EditBuildInfoFragment.class, mFragmentSharedBuild));
        actionBar.addTab(tab);

        tab = actionBar.newTab()
            .setText("Notes")
            .setTabListener(new TabListener<EditBuildNotesFragment>(
                        this, "notes", EditBuildNotesFragment.class, mFragmentSharedBuild));
        actionBar.addTab(tab);
        
        tab = actionBar.newTab()
                .setText("Items")
                .setTabListener(new TabListener<EditBuildItemsFragment>(
                        this, "items", EditBuildItemsFragment.class, mFragmentSharedBuild));
        actionBar.addTab(tab);
        
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SELECTED_TAB)) {
        	actionBar.setSelectedNavigationItem(savedInstanceState.getInt(KEY_SELECTED_TAB));
        }
        
        // Set up view pager
//        mPager = (ViewPager) findViewById(R.id.pager);
//        mPagerAdapter = new EditBuildPagerAdapter(getSupportFragmentManager(), this, mInitialBuild);
//        mPager.setAdapter(mPagerAdapter);
        
        // set action bar title
        getSupportActionBar().setTitle(mCreatingNewBuild == true ? getString(R.string.edit_build_new_title) : getString(R.string.edit_build_edit_title));
        if (!mCreatingNewBuild)
        	getSupportActionBar().setSubtitle(mInitialBuild.getName());
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
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(KEY_NEW_BUILD_BOOL, mCreatingNewBuild);
		outState.putSerializable(KEY_WORKING_BUILD, mInitialBuild);
		outState.putLong(IntentKeys.KEY_BUILD_ID, mBuildId);
		outState.putInt(KEY_SELECTED_TAB, getSupportActionBar().getSelectedNavigationIndex());
		super.onSaveInstanceState(outState);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.edit_build_menu, menu);
       return true;
    }
    
    /** Handle "Up" button press on action bar */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case R.id.menu_save:
        		// temp profiling
//        		Debug.startMethodTracing("saveResult");
        		saveResult();
//        		Debug.stopMethodTracing();
        		return true;
            case android.R.id.home:
            	// This is called when the Home (Up) button is pressed
                // in the Action Bar.
            	// TODO: prompt user to save changes (do it in onDestroy instead?)
                //finish();
            	doExit();
                return true;            	
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onFactionSelectionChanged(final Faction selection) {
		Timber.d("onFactionSelectionChanged() called with " + selection + ", current selection = " + mCurrentFactionSelection);
		if (selection == mCurrentFactionSelection)
			return;
		
		// user has selected a different faction in the info editor
		
		// does the items editor exist yet?
		final EditBuildItemsFragment itemsEditor = findItemsFragment();
		if (itemsEditor != null) {
			// if yes, does it have any items?
			if (itemsEditor.getBuildItems() != null && itemsEditor.getBuildItems().size() > 0) {
				// yes it has items
				// confirm item deletion with user
				showConfirmationDialog(this, R.string.dlg_confirm_change_faction_title,
						R.string.dlg_confirm_change_faction_message,
						// if user confirms, change faction
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								itemsEditor.setFaction(selection);
								mCurrentFactionSelection = selection;
								setBackgroundImage(selection);
							}},
							// if user cancels, revert the faction selection
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								EditBuildInfoFragment infoFragment = findInfoFragment();
								if (infoFragment != null)
									infoFragment.setFactionSelection(mCurrentFactionSelection);
							}});
			} else {
			// no, no items
				// go ahead and change faction
				itemsEditor.setFaction(selection);
				mCurrentFactionSelection = selection;
				setBackgroundImage(selection);
			}
		
		} else {	// no, item editor fragment doesn't exist yet
			// does the initial build have any items?
			if (mInitialBuild.getItems() != null && mInitialBuild.getItems().size() > 0) {
				// yes it has items
				// show confirmation dialog
				showConfirmationDialog(this, R.string.dlg_confirm_change_faction_title,
						R.string.dlg_confirm_change_faction_message,
						// if user accepts, send new faction to pageradapter
						// When pageradapter creates items editor, it should pass it an empty build
						// item list
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
                                // TODO: bug? mPagerAdapter will be null...
								mPagerAdapter.setFaction(selection);
								mCurrentFactionSelection = selection;
								setBackgroundImage(selection);
							}},
							// if user cancels, revert the faction selection
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								EditBuildInfoFragment infoFragment = findInfoFragment();
								if (infoFragment != null)
									infoFragment.setFactionSelection(mCurrentFactionSelection);
							}});
			} else {
				// no items in initial build
				// send new faction to pageradapter so it's passed on to items editor when created
				// thereby limiting new items to the correct faction
				
				// TODO: work out how to translate this to new tab layout
//				mPagerAdapter.setFaction(selection);

				// TEMP HACK
				mFragmentSharedBuild.setFaction(selection);
				// clear any old-faction build items
				if (mFragmentSharedBuild.getItems() != null && mFragmentSharedBuild.getItems().size() > 0)
					mFragmentSharedBuild.setItems(new ArrayList<BuildItem>());
				
				mCurrentFactionSelection = selection;
				setBackgroundImage(selection);
			}
		}
	}

	private void setBackgroundImage(Faction faction) {
		// set background graphic
		mRootView.setBackgroundDrawable(
                getResources().getDrawable(BriefActivity.getBackgroundDrawable(faction)));
	}
	
	/** 
	 * Adds or updates a Build in the database based on user's input, then calls finish(). If
	 * the user was editing an existing build and no changes were made, no database transactions
	 * occur and finish() is called.
	 * 
	 * If there are problems with the build such that it can't be saved, finish() won't be called
	 * and an error message will be shown instead.
	 */
	private void saveResult() {
//		DbAdapter db = ((MyApplication) getApplicationContext()).getDb();
		Build newBuild = assembleBuild();
		
		// last minute sanity checking on user's input before we write it to the database
		if (newBuild.getName() == null || newBuild.getName().matches("")) {
			// TODO generate one automatically (Untitled Build #1...)
			showMessage(R.string.edit_build_no_title_error);
			return;
		}
		if (!newBuild.isWellOrdered()) {
			// TODO: provide real-time visual feedback of this in the items editor fragment
			showMessage(R.string.edit_build_not_well_ordered_error);
			return;
		}
		
		long buildId = -1;
		if (mCreatingNewBuild) {
			// set creation time
			Date now = new Date();
			newBuild.setCreated(now);
			newBuild.setModified(now);
		} else {
			if (userMadeChanges(newBuild)) {
				// set last modified time
				newBuild.setModified(new Date());	// current time
				
				buildId = mBuildId;
			} else {
				finish();	// no changes made, just finish
				return;
			}
		}
		
		// finishes activity if successful
		new WriteBuildTask(newBuild, buildId).execute();
	}
	
	/** did the user make changes to the build in any of the editor fragments? */
	private boolean userMadeChanges() {
		return userMadeChanges(null);
	}
	
	private boolean userMadeChanges(Build assembledBuild) {
		if (assembledBuild == null)
			assembledBuild = assembleBuild();
//		Timber.d(this.toString(), "initial build items = " + mInitialBuild.getItems() + ", assembled build items = " + assembledBuild.getItems());
//		Timber.d(this.toString(), "authors the same = " + Build.objectsEquivalent(mInitialBuild.getAuthor(), assembledBuild.getAuthor()));
//		Timber.d(this.toString(), "initial items size = " + mInitialBuild.getItems().size() + ", assembledbuild items size = " + assembledBuild.getItems().size());
//		Timber.d(this.toString(), "items the same = " + Build.objectsEquivalent(mInitialBuild.getItems(), assembledBuild.getItems()));
//		Timber.d(this.toString(), "initialbuild = " + mInitialBuild);
		return (!mInitialBuild.equals(assembledBuild));	
	}

	@Override
	public void onBackPressed() {
	    doExit();
	}
	
	/**
	 * The user is attempting to exit the activity without explicitly saving the build
	 */
	private void doExit() {
		// check if the user has made changes to the build (whether new or existing)
			// if so, prompt them to save changes
			// if not, just finish

		if (userMadeChanges()) {
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

		    alertDialog.setPositiveButton(R.string.save, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					saveResult();
				}
			}).setNegativeButton(R.string.discard, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showMessage(R.string.edit_build_discarded_changes);
					finish();
				}
			}).setNeutralButton(R.string.cancel, null)
				.setTitle(R.string.edit_build_prompt_save_title)
				.setMessage(R.string.edit_build_prompt_save_message);
		    
		    alertDialog.show();
		} else {
			finish();
		}
	}
	
	/** may be null if the fragment hasn't been created yet (meaning the user hasn't swiped over to it so far) */
	private EditBuildInfoFragment findInfoFragment() {
		FragmentManager fm = getSupportFragmentManager();
		EditBuildInfoFragment f = (EditBuildInfoFragment) fm.findFragmentByTag("info");
		Timber.d("in findInfoFragment(), f = " + f);
		return f;
//		return (EditBuildInfoFragment) fm.findFragmentByTag(BuildListActivity.makeFragmentName(mPager.getId(), 0));
	}
	
	private EditBuildNotesFragment findNotesFragment() {
		FragmentManager fm = getSupportFragmentManager();
		EditBuildNotesFragment f = (EditBuildNotesFragment) fm.findFragmentByTag("notes");
		Timber.d("in findNotesFragment(), f = " + f);
		return f;
		
//		return (EditBuildNotesFragment) fm.findFragmentByTag(BuildListActivity.makeFragmentName(mPager.getId(), 1));
	}
	
	private EditBuildItemsFragment findItemsFragment() {
		FragmentManager fm = getSupportFragmentManager();
		EditBuildItemsFragment f = (EditBuildItemsFragment) fm.findFragmentByTag("items");
		Timber.d("in findItemsFragment(), f = " + f);
		return f;
//		return (EditBuildItemsFragment) fm.findFragmentByTag(BuildListActivity.makeFragmentName(mPager.getId(), 2));
	}
	
	/**
	 * @return new build object from user's input
	 */
	private Build assembleBuild() {
		Build result = new Build();
		//Build result = mInitialBuild;	// copies existing build items, if any. Useful if items fragment is null
//		Build result = (Build) UnoptimizedDeepCopy.copy(mInitialBuild);

		// set creation time if needed
		if (result.getCreated() == null)
			result.setCreated(new Date());
		
		EditBuildInfoFragment infoFragment = findInfoFragment();
		EditBuildNotesFragment notesFragment = findNotesFragment();
		EditBuildItemsFragment itemsFragment = findItemsFragment();
		
//		Timber.d(this.toString(), String.format("info = %s, notes = %s, items = %s", infoFragment, notesFragment, itemsFragment));
		
		if (infoFragment != null) {
			// title will be null if view for info fragment hasn't been created yet
			if (infoFragment.getTitle() != null) {
				result.setName(infoFragment.getTitle().matches("") ? null : infoFragment.getTitle());
				result.setFaction(infoFragment.getFaction());
				result.setVsFaction(infoFragment.getVsFaction());
				result.setExpansion(infoFragment.getExpansion());
				result.setSource(infoFragment.getSourceTitle().matches("") ? null : infoFragment.getSourceTitle(),
						infoFragment.getSourceURL().matches("") ? null : infoFragment.getSourceURL());
				result.setAuthor(infoFragment.getAuthor().matches("") ? null : infoFragment.getAuthor());
			}
		} else {
			throw new RuntimeException("In assembleBuild(), couldn't find EditBuildInfoFragment"); 
		}
		
		if (notesFragment != null) {
			result.setNotes(notesFragment.getNotes().matches("") ? null : notesFragment.getNotes());
		} else {
			result.setNotes(mInitialBuild.getNotes());
		}
		
		if (itemsFragment != null) {
			ArrayList<BuildItem> items = itemsFragment.getBuildItems();
			// hack to make sure assembled builds with no items is equal to
			// the initial build, if no other changes have been made
			if (items.size() != 0)
				result.setItems(items);
		} else {
			result.setItems(mInitialBuild.getItems());
		}
		
		return result;
	}
	
	// Helpers
	private void showMessage(int msg_res_id) {
		Toast.makeText(this, msg_res_id, Toast.LENGTH_SHORT).show();
	}

	/**
	 * User edited the build title, update the action bar subtitle
	 */
	@Override
	public void onTitleChanged(String newTitle) {
		if (!newTitle.matches(""))
			getSupportActionBar().setSubtitle(newTitle);
	}
	
	/** Convenience function to build and show a yes/no dialog */
	public static void showConfirmationDialog(Context c, int titleRes, int messageRes,
			OnClickListener positiveAction, OnClickListener negativeAction) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(c);
    	builder.setTitle(titleRes)
    		.setMessage(messageRes)
    		.setPositiveButton(android.R.string.yes, positiveAction)
			.setNegativeButton(android.R.string.no, negativeAction)
			.show();
	}
}
