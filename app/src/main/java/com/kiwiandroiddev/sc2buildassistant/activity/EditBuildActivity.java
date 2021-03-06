package com.kiwiandroiddev.sc2buildassistant.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback;
import com.google.analytics.tracking.android.EasyTracker;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.EditBuildInfoFragment;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.EditBuildInfoFragment.EditBuildInfoListener;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.EditBuildItemsFragment;
import com.kiwiandroiddev.sc2buildassistant.activity.fragment.EditBuildNotesFragment;
import com.kiwiandroiddev.sc2buildassistant.adapter.EditBuildPagerAdapter;
import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Expansion;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction;
import com.kiwiandroiddev.sc2buildassistant.feature.brief.view.BriefActivity;
import com.kiwiandroiddev.sc2buildassistant.service.JsonBuildService;
import com.kiwiandroiddev.sc2buildassistant.util.FragmentUtils;
import com.kiwiandroiddev.sc2buildassistant.util.NoOpAnimatorListener;

import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Activity for creating new build orders or editing existing ones. Has three fragments
 * for editing different aspects of the build order - basic information, detailed notes,
 * and the list of build items.
 * 
 * Reference for combining tabs with view pager:
 * https://developer.android.com/training/implementing-navigation/lateral.html
 *
 * @author matt
 *
 */
public class EditBuildActivity extends AppCompatActivity implements EditBuildInfoListener, EditBuildPagerAdapter.OnFragmentCreatedListener {

	private static final String KEY_NEW_BUILD_BOOL = "mCreatingNewBuild";
	private static final String KEY_WORKING_BUILD = "mWorkingBuild";
	private static final String KEY_SELECTED_TAB = "mSelectedTab";

	private boolean mCreatingNewBuild = false;
	private long mBuildId;		// id of existing build, if any
	private Build mInitialBuild;
	private EditBuildPagerAdapter mPagerAdapter;
	private Faction mCurrentFactionSelection;
    private boolean mHaveInitialisedFABVisibility = false;
    private ObjectAnimator mButtonAppearAnimation;
    private boolean mButtonIsOrWillBeVisible;

    @BindView(R.id.edit_build_activity_root) View mRootView;
    @BindView(R.id.edit_build_activity_add_button) View mAddButton;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.pager) ViewPager mPager;
	@BindView(R.id.tabs) TabLayout mTabLayout;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		// Can't have both fullscreen mode and window resizing when the soft keyboard
		// becomes visible (needed so formatting toolbar stays on screen). Known bug
		// in AOSP apparently:
		// https://code.google.com/p/android/issues/detail?id=5497

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_build);
        ButterKnife.bind(this);

        // Get arguments sent by BuildListActivity - these will determine
        // if we should create a new build or edit an existing one
        Expansion expansion = null;
        Faction faction = null;
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
        initAnimators();

		initViewPager();

        initToolbar();
	}

	private void initViewPager() {
		mPagerAdapter = new EditBuildPagerAdapter(getSupportFragmentManager(), this, mInitialBuild, this);
		mPager.setOffscreenPageLimit(3);
		mPager.setAdapter(mPagerAdapter);
		final ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // Show or hide floating Add button depending on current tab's preference
                setAddButtonVisibility(getCurrentlyVisibleEditorTab().requestsAddButton());
            }
        };
		mPager.addOnPageChangeListener(onPageChangeListener);

		mTabLayout.setupWithViewPager(mPager);
	}

	private void initToolbar() {
		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(mCreatingNewBuild ? getString(R.string.edit_build_new_title) : getString(R.string.edit_build_edit_title));
		if (!mCreatingNewBuild) {
			getSupportActionBar().setSubtitle(mInitialBuild.getName());
		}
	}

	/**
     * This is called whenever EditBuildPagerAdapter creates a new child Fragment. It's
     * used here to initialise the Floating "Add" button's visibility at the earliest possible
     * moment.
     * Note that the FAB's visibility is also updated whenever the user switches editor Fragments
     * via an OnPageChangeListener - ideally that would fire automatically when the ViewPager is
     * first populated, making this method unnecessary.
     *
     * @param newFragment
     */
    @Override
    public void onEditorFragmentCreated(Fragment newFragment) {
        if (!mHaveInitialisedFABVisibility) {
            if (!(newFragment instanceof BuildEditorTabView)) {
                throw new IllegalStateException("You should change " + newFragment + " to implement BuildEditorTabView");
            }

            BuildEditorTabView tab = (BuildEditorTabView) newFragment;
            setAddButtonVisibility(tab.requestsAddButton());
            mHaveInitialisedFABVisibility = true;
        }
    }

    private void initAnimators() {
        // init from XML (one source of truth for initial view state)
        mButtonIsOrWillBeVisible = mAddButton.getVisibility() == View.VISIBLE;

        mButtonAppearAnimation = ObjectAnimator.ofPropertyValuesHolder(mAddButton,
                PropertyValuesHolder.ofFloat("scaleX", 0.0f, 1.0f),
                PropertyValuesHolder.ofFloat("scaleY", 0.0f, 1.0f));
        mButtonAppearAnimation.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
        mButtonAppearAnimation.setInterpolator(new FastOutSlowInInterpolator());
    }

    private void setAddButtonVisibility(boolean makeVisible) {
        if (makeVisible && !mButtonIsOrWillBeVisible) {
            mAddButton.setVisibility(View.VISIBLE);
            mButtonAppearAnimation.removeAllListeners();
            mButtonAppearAnimation.start();
            mButtonIsOrWillBeVisible = true;
        } else if (!makeVisible && mButtonIsOrWillBeVisible) {
            mButtonAppearAnimation.removeAllListeners();
            mButtonAppearAnimation.addListener(new NoOpAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mAddButton.setVisibility(View.GONE);
                }
            });
            mButtonAppearAnimation.reverse();
            mButtonIsOrWillBeVisible = false;
        }
    }

    private BuildEditorTabView getCurrentlyVisibleEditorTab() {
        Fragment fragment = mPagerAdapter.getRegisteredFragment(mPager.getCurrentItem());
        if (fragment == null) {
            Timber.e("Warning: registered fragment was null in getCurrentlyVisibleEditorTab, returning default BuildEditorTabView impl.");
            return BuildEditorTabView.DefaultBuildEditorTabView;
        }
        if (fragment instanceof BuildEditorTabView) {
            Timber.d("current editor tab = " + fragment);
            return (BuildEditorTabView) fragment;
        } else {
            throw new IllegalStateException(String.format("Fragment \"%s\" from EditBuildPagerAdapter must implement BuildEditorTabView",
                    fragment));
        }
    }

    @Override
    public void onStart() {
    	super.onStart();
    	EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
    	super.onStop();
    	EasyTracker.getInstance(this).activityStop(this);
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
            	doExit();
                return true;            	
        }
        return super.onOptionsItemSelected(item);
    }

	@OnClick(R.id.edit_build_activity_add_button)
	public void onAddButtonClicked() {
		getCurrentlyVisibleEditorTab().onAddButtonClicked();
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
                        new SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                itemsEditor.setFaction(selection);
                                mCurrentFactionSelection = selection;
                                setBackgroundImage(selection);
                            }
                        },
                        // if user cancels, revert the faction selection
                        new SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                revertFactionSelectionInInfoFragment();
                            }
                        });
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
                        new SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                mPagerAdapter.setFaction(selection);
                                mCurrentFactionSelection = selection;
                                setBackgroundImage(selection);
                            }
                        },
                        // if user cancels, revert the faction selection
                        new SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                revertFactionSelectionInInfoFragment();
                            }
                        });
			} else {
				// no items in initial build
				// send new faction to pageradapter so it's passed on to items editor when created
				// thereby limiting new items to the correct faction
				mPagerAdapter.setFaction(selection);
				
				mCurrentFactionSelection = selection;
				setBackgroundImage(selection);
			}
		}
	}

    private void revertFactionSelectionInInfoFragment() {
        EditBuildInfoFragment infoFragment = findInfoFragment();
        if (infoFragment != null)
            infoFragment.setFactionSelection(mCurrentFactionSelection);
    }

    private void setBackgroundImage(Faction faction) {
		// set background graphic
		mRootView.setBackgroundDrawable(
                getResources().getDrawable(BriefActivity.Companion.getBackgroundDrawable(faction)));
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
		Build newBuild = assembleBuild();
		
		// last minute sanity checking on user's input before we write it to the database
		if (newBuild.getName() == null || newBuild.getName().matches("")) {
			// TODO generate one automatically (Untitled Build #1...)
			showMessage(R.string.edit_build_no_title_error);
			return;
		}
		if (!newBuild.isWellOrdered()) {
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
		if (assembledBuild == null) {
            assembledBuild = assembleBuild();
        }

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
		if (userMadeChanges()) {
            showSaveChangesBeforeExitPrompt();
		} else {
			finish();
		}
	}

    private void showSaveChangesBeforeExitPrompt() {
        new MaterialDialog.Builder(this)
                .title(R.string.edit_build_prompt_save_title)
                .content(R.string.edit_build_prompt_save_message)
                .positiveText(R.string.save)
                .onPositive(new SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        saveResult();
                    }
                })
                .negativeText(R.string.discard)
                .onNegative(new SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        showMessage(R.string.edit_build_discarded_changes);
                        finish();
                    }
                })
                .neutralText(R.string.cancel)
                .show();
    }

    /** may be null if the fragment hasn't been created yet (meaning the user hasn't swiped over to it so far) */
	private EditBuildInfoFragment findInfoFragment() {
		FragmentManager fm = getSupportFragmentManager();
		return (EditBuildInfoFragment) fm.findFragmentByTag(FragmentUtils.makeFragmentName(mPager.getId(), 0));
	}
	
	private EditBuildNotesFragment findNotesFragment() {
		FragmentManager fm = getSupportFragmentManager();
		return (EditBuildNotesFragment) fm.findFragmentByTag(FragmentUtils.makeFragmentName(mPager.getId(), 1));
	}
	
	private EditBuildItemsFragment findItemsFragment() {
		FragmentManager fm = getSupportFragmentManager();
		return (EditBuildItemsFragment) fm.findFragmentByTag(FragmentUtils.makeFragmentName(mPager.getId(), 2));
	}
	
	/**
	 * @return new build object from user's input
	 */
	private Build assembleBuild() {
		Build result = new Build();
		//Build result = mInitialBuild;	// copies existing build items, if any. Useful if items fragment is null
//		Build result = (Build) UnoptimizedDeepCopy.copy(mInitialBuild);

		// set creation time if needed
		if (result.getCreated() == null) {
            result.setCreated(new Date());
        }
		
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
		if (!newTitle.matches("")) {
            getSupportActionBar().setSubtitle(newTitle);
        }
	}
	
	/** Convenience function to build and show a yes/no dialog */
	public static void showConfirmationDialog(Context context,
                                              @StringRes int titleRes,
                                              @StringRes int messageRes,
                                              final SingleButtonCallback positiveAction,
                                              final SingleButtonCallback negativeAction) {
        new MaterialDialog.Builder(context)
                .title(titleRes)
                .content(messageRes)
                .positiveText(android.R.string.yes)
                .onPositive(positiveAction)
                .negativeText(android.R.string.no)
                .onNegative(negativeAction)
                .show();
	}

    /**
     * Writes a build object to the database in a background task
     * TODO: Rx-ify
     */
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
			mDlg.setMessage(getString(R.string.edit_build_save_in_progress));
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
}
