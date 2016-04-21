package com.kiwiandroiddev.sc2buildassistant.activity.fragment;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.EmptyPermissionListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener;
import com.kiwiandroiddev.sc2buildassistant.BuildOrderProvider;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.BriefActivity;
import com.kiwiandroiddev.sc2buildassistant.activity.EditBuildActivity;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Expansion;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction;
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build;
import com.kiwiandroiddev.sc2buildassistant.service.JsonBuildService;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hugo.weaving.DebugLog;
import timber.log.Timber;

import static android.os.Build.VERSION;
import static com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys.KEY_BUILD_ID;
import static com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys.KEY_BUILD_NAME;
import static com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys.KEY_EXPANSION_ENUM;
import static com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys.KEY_FACTION_ENUM;

/**
 * Fragment for displaying a list build orders for one of the 3 StarCraft factions (Terran,
 * Protoss or Zerg). Users can tap builds in the list once to open the detailed explanation
 * screen (BriefActivity) or long-press to get a menu of actions that can be taken on
 * that build including Edit, Export and Delete. 
 * 
 * @author matt
 *
 */
public class RaceFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	    
	private static Map<Faction, Integer> sIconByRace = new HashMap<>();

	private int mBgDrawable;
	private Expansion mCurrentExpansion;
	private Faction mFaction;
	private ViewGroup mRootView;
	private RecyclerView mRecyclerView;
    private BuildAdapter mAdapter;

	static {
		sIconByRace.put(Faction.TERRAN, R.drawable.terran_icon_drawable);
		sIconByRace.put(Faction.PROTOSS, R.drawable.protoss_icon_drawable);
		sIconByRace.put(Faction.ZERG, R.drawable.zerg_icon_drawable);
	}

	@DebugLog
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			/** Getting the arguments to the Bundle object */
			Bundle data = getArguments();
			
			/** Getting integer data of the key current_page from the bundle */
			mFaction = (Faction) data.getSerializable(KEY_FACTION_ENUM);
			
			// passed from racefragmentpageradapter when this is constructed
			mCurrentExpansion = (Expansion) data.getSerializable(KEY_EXPANSION_ENUM);
		} else {
			mFaction = (Faction) savedInstanceState.getSerializable(KEY_FACTION_ENUM);
			mCurrentExpansion = (Expansion)savedInstanceState.getSerializable(KEY_EXPANSION_ENUM);
		}
		
		mBgDrawable = sIconByRace.containsKey(mFaction) ? sIconByRace.get(mFaction) : R.drawable.not_found;
	}

    @DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// create a list view to show the builds for this race
		// and make clicks on an item start the playback activity for that build
		View v = inflater.inflate(R.layout.fragment_race_layout, container, false);
		mRecyclerView = (RecyclerView) v.findViewById(R.id.build_list);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		mRecyclerView.setHasFixedSize(true);
		mRecyclerView.setBackgroundDrawable(this.getActivity().getResources().getDrawable(mBgDrawable));

		// load list of build order names for this tab's faction and expansion
		// use race ID to differentiate the cursor from ones for other tabs
		getActivity().getSupportLoaderManager().initLoader(mFaction.ordinal(), null, this);

		mRootView = (ViewGroup) v;
		return v;
	}

    @DebugLog
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(KEY_FACTION_ENUM, mFaction);
		outState.putSerializable(KEY_EXPANSION_ENUM, mCurrentExpansion);
		super.onSaveInstanceState(outState);
	}
		
	/**
	 * list view will only show build orders for Starcraft 2
	 * expansion given as an argument
	 * @param game
	 */
	public void setExpansionFilter(Expansion game) {
		mCurrentExpansion = game;

		// mCurrentExpansion is used to build the query for a new cursor
		getActivity().getSupportLoaderManager().restartLoader(mFaction.ordinal(), null, this);
	}

	/**
	 * Loads a cursor to the list of build order names in the database for this tab's race
	 * and expansion.
	 */
    @DebugLog
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        DbAdapter db = ((MyApplication) getActivity().getApplicationContext()).getDb();
        db.open();
		
		final String whereClause = DbAdapter.KEY_FACTION_ID + " = " + DbAdapter.getFactionID(mFaction)
    			+ " and " + DbAdapter.KEY_EXPANSION_ID + " = " + DbAdapter.getExpansionID(mCurrentExpansion);

		return new CursorLoader(getActivity(),
                Uri.withAppendedPath(BuildOrderProvider.BASE_URI, DbAdapter.TABLE_BUILD_ORDER),	// table URI
                new String[]{ DbAdapter.KEY_BUILD_ORDER_ID, DbAdapter.KEY_NAME, DbAdapter.KEY_CREATED, DbAdapter.KEY_VS_FACTION_ID },	// columns to return
                whereClause,																	// select clause
                null,																			// select args
                DbAdapter.KEY_VS_FACTION_ID + ", " + DbAdapter.KEY_NAME + " asc");				// sort order
	}

    @DebugLog
	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        mAdapter = new BuildAdapter(getActivity(), cursor);
        updateListAdapter();
	}

    @DebugLog
	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
        mAdapter = null;
        updateListAdapter();
	}

    @DebugLog
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateListAdapter();
    }

    @DebugLog
    private void updateListAdapter() {
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(mAdapter);
        }
    }

    /**
     * Starts the build editor activity, passing ID of build in the database
     */
	private void editBuild(long rowId) {
		Intent i = new Intent(getActivity(), EditBuildActivity.class);
        i.putExtra(KEY_BUILD_ID, rowId);
        startActivity(i);
	}

    /**
     * Confirms deletion first with user as this is operation deletes user data
     * @param rowId
     */
	private void deleteBuild(final long rowId) {
        // TODO replace with Material Dialog (for ICS devices)
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(R.string.dlg_confirm_delete_build_title)
    		.setMessage(R.string.dlg_confirm_delete_build_message)
    		.setPositiveButton(android.R.string.yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
			        Uri buildUri = ContentUris.withAppendedId(
			        		Uri.withAppendedPath(BuildOrderProvider.BASE_URI, DbAdapter.TABLE_BUILD_ORDER), rowId);
			        getActivity().getContentResolver().delete(buildUri, null, null);
				}
			})
			.setNegativeButton(android.R.string.no, null)
			.show();
	}
	
	/**
	 * Exports Build in database with given row ID to a JSON file on the
	 * user's SD card. Presents a dialog for the user to enter a filename
	 * for the build. A default filename based on the build name is suggested.
	 * 
	 * @param rowId
	 */
	private void exportBuild(final long rowId) {
		final PermissionListener snackbarPermissionListener =
				SnackbarOnDeniedPermissionListener.Builder
						.with(mRootView, R.string.permission_popup_write_storage_denied_for_exporting)
						.withOpenSettingsButton(R.string.permission_popup_settings)
						.build();

		Dexter.checkPermission(new EmptyPermissionListener() {
			@Override
			public void onPermissionGranted(PermissionGrantedResponse response) {
				exportBuildWithPermissionsGranted(rowId);
			}

			@Override
			public void onPermissionDenied(PermissionDeniedResponse response) {
				snackbarPermissionListener.onPermissionDenied(response);
			}

			@Override
			public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
				token.continuePermissionRequest();
			}
		}, Manifest.permission.WRITE_EXTERNAL_STORAGE);
	}

	private void exportBuildWithPermissionsGranted(long rowId) {

		if (!JsonBuildService.createBuildsDirectory()) {
			Toast.makeText(getActivity(),
					String.format(getString(R.string.error_couldnt_create_builds_dir), JsonBuildService.BUILDS_DIR),
					Toast.LENGTH_LONG).show();
			return;
		}

		// get build object from DB
		DbAdapter db = ((MyApplication) getActivity().getApplicationContext()).getDb();
		final Build build = db.fetchBuild(rowId);
		if (build == null) {
			Timber.d("couldn't export build with id " + rowId + " as it doesn't exist in DB");
			return;
		}

		// create a dialog with a text input field to get filename
		final EditText input = new EditText(getActivity());
		final String buildName = build.getName();
		// TODO: prevent the user from even entering any invalid characters
		// restrict input to alphanumeric chars, dots and underscores
		input.setText(removeSpecialCharacters(buildName) + ".json");		// set a default filename
		input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

		alert.setTitle(R.string.dlg_enter_filename_title)
				.setMessage(R.string.dlg_enter_filename_message)
				.setView(input)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// TODO: confirm overwrite with another dialog if filename alreaady exists!
						final String filename = input.getText().toString();
						if (filename.matches("")) {
							Toast.makeText(getActivity(), R.string.dlg_invalid_filename, Toast.LENGTH_LONG).show();
							return;
						}

						try {
							JsonBuildService.writeBuildToJsonFile(filename, build);
						} catch (Exception e) {
							Toast.makeText(getActivity(), String.format(getString(R.string.dlg_couldnt_write_file),
									filename, e.toString()), Toast.LENGTH_LONG).show();
							e.printStackTrace();
							return;
						}
						Toast.makeText(getActivity(), String.format(getString(R.string.dlg_wrote_file_to_dir),
								filename, JsonBuildService.BUILDS_DIR), Toast.LENGTH_LONG).show();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				}).show();
	}

	/**
     * Spacer to stop Ad from obscuring the bottom of content
     * @return
     */
    private View getAdSpacerView() {
		LayoutInflater inflater = getActivity().getLayoutInflater();
        return inflater.inflate(R.layout.ad_spacer_row, null, false);
	}

	/**
	 * Replaces special characters in a string with underscores. Useful for
	 * sanitizing filenames
	 * 
	 * @param input string
	 */
	public static String removeSpecialCharacters(String input) {
		return input.replaceAll("[^\\dA-Za-z]+", "_");
	}

	private final static class BuildViewModel {
        public final long buildId;
		public final String name;
		public final String creationDate;
		public final String vsRace;

		public BuildViewModel(long buildId, String name, String creationDate, String vsRace) {
            this.buildId = buildId;
            this.name = name;
			this.creationDate = creationDate;
			this.vsRace = vsRace;
		}
	}

	private final class BuildViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {
        public TextView name;
		public TextView vsRace;
		public TextView creationDate;
        public BuildViewModel viewModel;

		public BuildViewHolder(@NonNull View itemView) {
			super(itemView);
			name = (TextView) itemView.findViewById(R.id.buildName);
			vsRace = (TextView) itemView.findViewById(R.id.buildVsRace);
			creationDate = (TextView) itemView.findViewById(R.id.buildCreationDate);
		}

		public void bindBuildViewModel(@NonNull BuildViewModel model) {
            viewModel = model;
			name.setText(model.name);
			vsRace.setText(model.vsRace);
			creationDate.setText(model.creationDate);
			itemView.setOnClickListener(this);
			itemView.setOnLongClickListener(this);
		}

		public void unbind() {
			itemView.setOnClickListener(null);
		}

        @Override
        public void onClick(View v) {
            onBuildItemClicked(this);
        }

		@Override
		public boolean onLongClick(View v) {
            onBuildItemLongPressed(this);
			return true;    // event handled
		}
	}

	private class BuildAdapter extends RecyclerView.Adapter<BuildViewHolder> {
		private static final int BUILD_ROW_TYPE = 0;
		private static final int FOOTER_ROW_TYPE = 1;
		List<BuildViewModel> mBuildViewModelList;

		public BuildAdapter(Context context, Cursor cursor) {
			mBuildViewModelList = new ArrayList<>();

            if (!cursor.moveToFirst()) {
				return;
			}

			do {
                long buildId = cursor.getLong(cursor.getColumnIndex(DbAdapter.KEY_BUILD_ORDER_ID));

				String buildName = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_NAME));

				String dateStr = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CREATED));
				String formattedDateStr = "";
				if (!TextUtils.isEmpty(dateStr)) {
					Date date;
					try {
						date = DbAdapter.DATE_FORMAT.parse(dateStr);
						DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
						formattedDateStr = df.format(date);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}

				int vsFactionId = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_VS_FACTION_ID));
				final String factionName = vsFactionId == 0 ? context.getString(R.string.race_any) :
						context.getString(DbAdapter.getFactionName(vsFactionId));
				String vsRace = "vs. " + factionName;

				BuildViewModel viewModel = new BuildViewModel(buildId, buildName, formattedDateStr, vsRace);
				mBuildViewModelList.add(viewModel);
			} while (cursor.moveToNext());
		}

		@Override
		public BuildViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == BUILD_ROW_TYPE) {
				View view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.build_row, parent, false);
				return new BuildViewHolder(view);
			} else {
				View view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.spacer_row, parent, false);
				return new BuildViewHolder(view);
			}
		}

		@Override
		public void onBindViewHolder(BuildViewHolder viewHolder, int position) {
			switch (getItemViewType(position)) {
				case BUILD_ROW_TYPE:
					viewHolder.bindBuildViewModel(mBuildViewModelList.get(position));
					return;
				case FOOTER_ROW_TYPE:
				default:
					viewHolder.unbind();
			}
		}

		@Override
		public int getItemCount() {
			return mBuildViewModelList.size() + 1;
		}

		@Override
		public int getItemViewType(int position) {
			return position < mBuildViewModelList.size() ? BUILD_ROW_TYPE : FOOTER_ROW_TYPE;
		}

		@Override
        public String toString() {
            return "BuildAdapter{" +
                    "mBuildViewModelList=" + mBuildViewModelList +
                    '}';
        }
    }

    private void onBuildItemClicked(BuildViewHolder buildViewHolder) {
        BuildViewModel model = buildViewHolder.viewModel;

        Intent i = new Intent(getActivity(), BriefActivity.class);
        i.putExtra(KEY_BUILD_ID, model.buildId);	// pass build order record ID

        // speed optimization - pass these so brief activity doesn't need to
        // requery them from the database and can display them instantly
        i.putExtra(KEY_FACTION_ENUM, mFaction);
        i.putExtra(KEY_EXPANSION_ENUM, mCurrentExpansion);
        i.putExtra(KEY_BUILD_NAME, model.name);

        if (VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // create the transition animation - the views in the layouts
            // of both activities are defined with android:transitionName="buildName"
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(getActivity(),
                            buildViewHolder.name, "buildName");
            // start the new activity
            getActivity().startActivity(i, options.toBundle());
        } else {
            getActivity().startActivity(i);
        }
    }
    
    private void onBuildItemLongPressed(final BuildViewHolder buildViewHolder) {
        Context context = getActivity();
        final MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(context);
        adapter.add(new MaterialSimpleListItem.Builder(context)
                .content(R.string.menu_edit_build)
                .build());
        adapter.add(new MaterialSimpleListItem.Builder(context)
                .content(R.string.menu_delete_build)
                .build());
        adapter.add(new MaterialSimpleListItem.Builder(context)
                .content(R.string.menu_export_build)
                .build());

        new MaterialDialog.Builder(context)
                .title(buildViewHolder.viewModel.name)
                .adapter(adapter, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                        long buildId = buildViewHolder.viewModel.buildId;
                        switch (which) {
                            case 0:
                                editBuild(buildId);
                                break;
                            case 1:
                                deleteBuild(buildId);
                                break;
                            case 2:
                                exportBuild(buildId);
                                break;
                            default:
                                Timber.e("Unknown context menu item selected, index = " + which);
                                break;
                        }
                        dialog.dismiss();
                    }
                })
                .show();
    }
}

