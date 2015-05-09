package com.kiwiandroiddev.sc2buildassistant.activity.fragment;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.InputType;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kiwiandroiddev.sc2buildassistant.BuildOrderProvider;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.activity.BriefActivity;
import com.kiwiandroiddev.sc2buildassistant.activity.EditBuildActivity;
import com.kiwiandroiddev.sc2buildassistant.activity.MainActivity;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Expansion;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.Faction;
import com.kiwiandroiddev.sc2buildassistant.model.Build;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

import static android.os.Build.VERSION;

/**
 * Fragment for displaying a list build orders for one of the 3 StarCraft factions (Terran,
 * Protoss or Zerg). Users can tap builds in the list once to open the detailed explanation
 * screen (BriefActivity) or long-press to get a menu of actions that can be taken on
 * that build including Edit, Export and Delete. 
 * 
 * @author matt
 *
 */
public class RaceFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {
	    
	private static Map<DbAdapter.Faction, Integer> sIconByRace = new HashMap<DbAdapter.Faction, Integer>();
	
	public static final String TAG = "RaceFragment";
	
	// keys that this activity uses when passing data to other activities or fragments
	public static final String KEY_BUILD_ID = "com.kiwiandroiddev.sc2buildassistant.BuildId";
	public static final String KEY_BUILD_NAME = "com.kiwiandroiddev.sc2buildassistant.BuildName";
	public static final String KEY_EXPANSION_ENUM = "com.kiwiandroiddev.sc2buildassistant.Expansion";
	public static final String KEY_FACTION_ENUM = "com.kiwiandroiddev.sc2buildassistant.Faction";
	public static final String KEY_ITEM_TYPE_ENUM = "com.kiwiandroiddev.sc2buildassistant.ItemType";
	public static final String KEY_BUILD_OBJECT = "com.kiwiandroiddev.sc2buildassistant.model.Build";
	public static final String KEY_BUILD_ITEM_OBJECT = "com.kiwiandroiddev.sc2buildassistant.model.BuildItem";
	
	private int mBgDrawable;
	private DbAdapter.Expansion mCurrentExpansion;
	private DbAdapter.Faction mFaction;
	private SimpleCursorAdapter mAdapter;
	
	private static final String[] sProjectionFrom;
	private static final int[] sProjectionTo;
	
	static {
		sIconByRace.put(DbAdapter.Faction.TERRAN, R.drawable.terran_icon_drawable);
		sIconByRace.put(DbAdapter.Faction.PROTOSS, R.drawable.protoss_icon_drawable);
		sIconByRace.put(DbAdapter.Faction.ZERG, R.drawable.zerg_icon_drawable);
		
        sProjectionFrom = new String[] { DbAdapter.KEY_NAME, DbAdapter.KEY_CREATED, DbAdapter.KEY_VS_FACTION_ID };
        sProjectionTo = new int[] { R.id.buildName, R.id.buildCreationDate, R.id.buildVsRace };
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		mDb = ((Sc2BuildAssistantApp)getActivity().getApplicationContext()).getDB();

		if (savedInstanceState == null) {
			/** Getting the arguments to the Bundle object */
			Bundle data = getArguments();
			
			/** Getting integer data of the key current_page from the bundle */
			mFaction = (Faction) data.getSerializable(KEY_FACTION_ENUM);
			
			// passed from racefragmentpageradapter when this is constructed
			mCurrentExpansion = (Expansion) data.getSerializable(KEY_EXPANSION_ENUM);
		} else {
			mFaction = (DbAdapter.Faction) savedInstanceState.getSerializable(KEY_FACTION_ENUM);
			mCurrentExpansion = (DbAdapter.Expansion)savedInstanceState.getSerializable(KEY_EXPANSION_ENUM);
		}
		
		mBgDrawable = sIconByRace.containsKey(mFaction) ? sIconByRace.get(mFaction) : R.drawable.not_found;
		
		// onCreate() guaranteed to be called before onCreateView() according to docs
        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.build_row, null,
        		sProjectionFrom, sProjectionTo, Adapter.NO_SELECTION);
        
        // display the build creation date in a more readable format
        // display vs. faction
        mAdapter.setViewBinder(new ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int column) {
				if (column == 2) {	// creation date column
					TextView text = (TextView) view;
					String dateStr = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CREATED));
					if (dateStr != null && !dateStr.matches("")) {
						Date date;
						try {
							date = DbAdapter.DATE_FORMAT.parse(dateStr);
						} catch (ParseException e) {
							e.printStackTrace();
							return false;
						}
						DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
						text.setText(df.format(date));
						return true;
					}
				} else if (column == 3) {
					TextView text = (TextView) view;
					final int vsFactionId = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_VS_FACTION_ID));
					final String factionName = vsFactionId == 0 ? getString(R.string.race_any) :
						getString(DbAdapter.getFactionName(vsFactionId));
					text.setText("vs. " + factionName);
					return true;
				}
				return false;
			}
        });
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// create a list view to show the builds for this race
		// and make clicks on an item start the playback activity for that build
		View v = inflater.inflate(R.layout.fragment_race_layout, container, false);
		ListView list = (ListView)v.findViewById(R.id.build_list);
		list.setBackgroundDrawable(this.getActivity().getResources().getDrawable(mBgDrawable));

		// set up the "Add Build..." extra list entry at the bottom
        list.addFooterView(getFooterView(), null, true);

        // Add an unselectable spacer to the bottom to stop ads from obscuring content
        list.addFooterView(getAdSpacerView(), null, false);     // false = not selectable

		list.setAdapter(mAdapter);
		list.setOnItemClickListener(this);

		registerForContextMenu(list);
		
		// load list of build order names for this tab's faction and expansion
		// use race ID to differentiate the cursor from ones for other tabs
		getActivity().getSupportLoaderManager().initLoader(mFaction.ordinal(), null, this);
		
		return v;
	}
	
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
	public void setExpansionFilter(DbAdapter.Expansion game) {
		mCurrentExpansion = game;

		// mCurrentExpansion is used to build the query for a new cursor
		getActivity().getSupportLoaderManager().restartLoader(mFaction.ordinal(), null, this);
	}

	/**
	 * Loads a cursor to the list of build order names in the database for this tab's race
	 * and expansion.
	 */
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

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		mAdapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);
	}
	
	/**
	 * A build order in the list view was long-pressed, create and display the
	 * appropriate context menu
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

	    // disable context menu for footer ("new build item")
	    if (info.id != -1) {
		    MenuInflater inflater = getActivity().getMenuInflater();
		    inflater.inflate(R.menu.build_list_context_menu, menu);
	    }
	}
	
	/**
	 * Handle menu choices for when the user long-presses a build order
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    switch (item.getItemId()) {
	        case R.id.menu_edit_build:
	            editBuild(info.id);
	            return true;
	        case R.id.menu_delete_build:
	            deleteBuild(info.id);
	            return true;
	        case R.id.menu_export_build:
	        	exportBuild(info.id);
	        	return true;
	    }
	 	return super.onContextItemSelected(item);
	}

    /**
     * Starts the build editor activity, passing ID of build in the database
     */
	private void editBuild(long rowId) {
		Intent i = new Intent(getActivity(), EditBuildActivity.class);
        i.putExtra(RaceFragment.KEY_BUILD_ID, rowId);
        startActivity(i);
	}

    /**
     * Confirms deletion first with user as this is operation deletes user data
     * @param rowId
     */
	private void deleteBuild(final long rowId) {
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
	private void exportBuild(long rowId) {
		if (!MainActivity.createBuildsDir(getActivity())) {
			Toast.makeText(getActivity(),
					String.format(getString(R.string.error_couldnt_create_builds_dir), MainActivity.BUILDS_DIR),
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
						MainActivity.writeBuild(filename, build);
					} catch (Exception e) {
						Toast.makeText(getActivity(), String.format(getString(R.string.dlg_couldnt_write_file),
								filename, e.toString()), Toast.LENGTH_LONG).show();
						e.printStackTrace();
						return;
					}
					Toast.makeText(getActivity(), String.format(getString(R.string.dlg_wrote_file_to_dir),
							filename, MainActivity.BUILDS_DIR), Toast.LENGTH_LONG).show();
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			}).show();
	}
	
	private View getFooterView() {
		LayoutInflater inflater = getActivity().getLayoutInflater();
        return inflater.inflate(R.layout.add_build_row, null, false);
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        if (id != -1) {
            Intent i = new Intent(getActivity(), BriefActivity.class);
            i.putExtra(KEY_BUILD_ID, id);	// pass build order record ID

            // speed optimization - pass these so brief activity doesn't need to
            // requery them from the database and can display them instantly
            i.putExtra(KEY_FACTION_ENUM, mFaction);
            i.putExtra(KEY_EXPANSION_ENUM, mCurrentExpansion);

            TextView nameView = (TextView) view.findViewById(R.id.buildName);
            i.putExtra(KEY_BUILD_NAME, nameView.getText().toString());

            if (VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                // create the transition animation - the images in the layouts
                // of both activities are defined with android:transitionName="robot"
                ActivityOptions options = ActivityOptions
                        .makeSceneTransitionAnimation(getActivity(),
                                new Pair<View, String>(nameView, "buildName"),
                                new Pair<View, String>(view, "buildRowBg"));
                // start the new activity
                getActivity().startActivity(i, options.toBundle());
            } else {
                getActivity().startActivity(i);
            }

            //Debug.startMethodTracing("sc2brief");
//            startActivity(i);
        } else {    // footer
            // starts the build editor
            Intent i = new Intent(getActivity(), EditBuildActivity.class);
            i.putExtra(RaceFragment.KEY_EXPANSION_ENUM, mCurrentExpansion);
            i.putExtra(RaceFragment.KEY_FACTION_ENUM, mFaction);
            startActivity(i);
        }
    }

}

