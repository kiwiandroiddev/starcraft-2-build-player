package com.kiwiandroiddev.sc2buildassistant.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.kiwiandroiddev.sc2buildassistant.R;
import com.kiwiandroiddev.sc2buildassistant.model.Build;
import com.kiwiandroiddev.sc2buildassistant.model.BuildItem;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// TODO: add check constraints to count, order

/**
 * Simple Build Order Database access class. Defines basic CRUD operations
 * and has methods for returning cursors to different sets of data.
 * 
 * Based on NotesDbAdapter.java from Notepad sample app included with Android SDK
 * and http://www.androidhive.info/2011/11/android-sqlite-database-tutorial/
 * 
 * @author matt
 */
public class DbAdapter {
	
	public enum ItemType { UNIT, STRUCTURE, UPGRADE, ABILITY, NOTE };
	private static Map<ItemType, Long> sItemTypeToIdMap = new HashMap<ItemType, Long>();
	private static Map<Long, ItemType> sIdToItemTypeMap = new HashMap<Long, ItemType>();
	
	public enum Faction { TERRAN, ZERG, PROTOSS };
	// TODO would be better to use a proper Bidirection Map class here, but don't want the extra
	// bloat of another library (Guava)
	private static Map<Faction, Long> sFactionToIdMap = new HashMap<Faction, Long>();
	private static Map<Long, Faction> sIdToFactionMap = new HashMap<Long, Faction>();
	
	public enum Expansion { WOL, HOTS };
	private static Map<Expansion, Long> sExpansionToIdMap = new HashMap<Expansion, Long>();
	private static Map<Long, Expansion> sIdToExpansionMap = new HashMap<Long, Expansion>();

	// links item type, faction and expansion enums to locale-independent string resources
	private static Map<DbAdapter.ItemType, Integer> sItemTypeNameMap = new HashMap<DbAdapter.ItemType, Integer>();
	private static Map<DbAdapter.Faction, Integer> sFactionNameMap = new HashMap<DbAdapter.Faction, Integer>();
	private static Map<DbAdapter.Expansion, Integer> sExpansionNameMap = new HashMap<DbAdapter.Expansion, Integer>();
	
    private static final String DB_NAME = "build_order_db";
    private static final int DB_VERSION = 48;
    private static final String TAG = "DbAdapter";	// to use in log messages
    
    // used for storing build creation and modified times as text fields (SQLite doesn't have a datetime type)
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Simple interface to post progress updates back to clients for long
     * database operations.
     */
    public interface ProgressListener {
    	void onProgressUpdate(int percent);
    }
    
    /**
     * Exceptions that can be thrown by DbAdapter methods when trying to add data
     */
    public class NameNotUniqueException extends RuntimeException {
		private static final long serialVersionUID = -8430454829568227763L;
		public NameNotUniqueException(String message) {
            super(message);
        }
    }
    
    /**
     * Database creation SQL statements
     */
	
	public static final String TABLE_EXPANSION = "expansion";	// WoL, HotS, LotV
	public static final String TABLE_FACTION = "faction";		// terran, protoss or zerg
	public static final String TABLE_BUILD_ORDER = "build_order";
	public static final String TABLE_ITEM_TYPE = "item_type";	// unit, building, upgrade, etc.
	public static final String TABLE_ITEM = "item";				// SCV, hatchery, zealot, etc.
	public static final String TABLE_BUILD_ORDER_ITEM = "build_order_item";
	public static final String TABLE_FACTION_ITEM_TYPE_VERB = "faction_item_type_verb";	// build vs. morph vs. warp in
	public static final String[] ALL_TABLES = {
			TABLE_BUILD_ORDER_ITEM,
			TABLE_ITEM,
			TABLE_ITEM_TYPE,
			TABLE_BUILD_ORDER,
			TABLE_FACTION,
			TABLE_EXPANSION };
	
	// expansion table column names
	public static final String KEY_EXPANSION_ID = "expansion_id";
	// (name)
	
	// faction table column names
	public static final String KEY_FACTION_ID = "faction_id";
	// (name)
	
	// build order table column names
	public static final String KEY_BUILD_ORDER_ID = "_id";	// SimpleCursorAdapter requires id column to be named this
	public static final String KEY_NAME = "name";
	// (expansion_id, faction_id)
	public static final String KEY_VS_FACTION_ID = "vs_faction_id";
	public static final String KEY_SOURCE = "source";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_AUTHOR = "author";
	public static final String KEY_CREATED = "created";
	public static final String KEY_MODIFIED = "modified";
	
	// item_type column names
	public static final String KEY_ITEM_TYPE_ID = "item_type_id";
	
	// item column names
	public static final String KEY_ITEM_ID = "item_id";
	// (item_type_id, name, faction_id)
	
	// build_order_item column names
	// (build_order_id, item_id)
	public static final String KEY_ORDER = "_order";		// important as items might have same time value but build order creator might want to force an ordering
	public static final String KEY_SECONDS = "seconds";
	public static final String KEY_TEXT = "_text";
	public static final String KEY_VOICE = "voice";
	public static final String KEY_TARGET = "target";
	public static final String KEY_COUNT = "count";
	
	/*
	 * Default SORT BY clauses for each table
	 */
	public static final String TABLE_BUILD_ORDER_DEFAULT_SORT = (KEY_VS_FACTION_ID + ", " + KEY_NAME);
	// ..
	
	/**
	 * BuildDBAdapter Member variables
	 */
	private Context mContext;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	private boolean isOpen = false;
	
	/**
	 * Initialize static variables
	 */
	static {
		// TODO: duplication of knowledge from addStaticData() method. Can't actually guarantee these IDs. They should be queried from the DB
		sItemTypeToIdMap.put(ItemType.UNIT, (long) 1);
		sItemTypeToIdMap.put(ItemType.STRUCTURE, (long) 2);
		sItemTypeToIdMap.put(ItemType.UPGRADE, (long) 3);
		sItemTypeToIdMap.put(ItemType.ABILITY, (long) 4);
		sItemTypeToIdMap.put(ItemType.NOTE, (long) 5);
		
		sIdToItemTypeMap.put((long) 1, ItemType.UNIT);
		sIdToItemTypeMap.put((long) 2, ItemType.STRUCTURE);
		sIdToItemTypeMap.put((long) 3, ItemType.UPGRADE);
		sIdToItemTypeMap.put((long) 4, ItemType.ABILITY);
		sIdToItemTypeMap.put((long) 5, ItemType.NOTE);
		
		sFactionToIdMap.put(Faction.TERRAN, (long) 1);
		sFactionToIdMap.put(Faction.ZERG, (long) 2);
		sFactionToIdMap.put(Faction.PROTOSS, (long) 3);
		
		sIdToFactionMap.put((long) 1, Faction.TERRAN);
		sIdToFactionMap.put((long) 2, Faction.ZERG);
		sIdToFactionMap.put((long) 3, Faction.PROTOSS);
		
		sExpansionToIdMap.put(Expansion.WOL, (long) 1);
		sExpansionToIdMap.put(Expansion.HOTS, (long) 2);
		
		sIdToExpansionMap.put((long) 1, Expansion.WOL);
		sIdToExpansionMap.put((long) 2, Expansion.HOTS);
		
		sItemTypeNameMap.put(ItemType.UNIT, R.string.item_type_unit);
		sItemTypeNameMap.put(ItemType.STRUCTURE, R.string.item_type_structure);
		sItemTypeNameMap.put(ItemType.UPGRADE, R.string.item_type_upgrade);
		sItemTypeNameMap.put(ItemType.ABILITY, R.string.item_type_ability);
		sItemTypeNameMap.put(ItemType.NOTE, R.string.item_type_note);
		
		sFactionNameMap.put(DbAdapter.Faction.TERRAN, R.string.race_terran);
		sFactionNameMap.put(DbAdapter.Faction.PROTOSS, R.string.race_protoss);
		sFactionNameMap.put(DbAdapter.Faction.ZERG, R.string.race_zerg);
		
		sExpansionNameMap.put(DbAdapter.Expansion.WOL, R.string.expansion_wol);
		sExpansionNameMap.put(DbAdapter.Expansion.HOTS, R.string.expansion_hots);
	}
	
    /**
     * Static convenience methods
     */
    
	/** returns a string resource ID */
	public static int getItemTypeName(DbAdapter.ItemType type) {
		return sItemTypeNameMap.get(type);
	}
	
	/** returns a string resource ID */
	public static int getFactionName(DbAdapter.Faction race) {
		return sFactionNameMap.get(race);
	}
	
	/** returns a string resource ID */
	public static int getExpansionName(DbAdapter.Expansion game) {
		return sExpansionNameMap.get(game);
	}
	
	/** Takes care of creating/opening/upgrading an SQLite database */
    private static class DatabaseHelper extends SQLiteOpenHelper {

    	private boolean mNeedsStaticDataAdded = false;
    	private Context mContext;
    	
        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        	mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	Log.w(TAG, "onCreate() called");
        	
        	// TODO: make these static
        	final String CREATE_EXPANSION_TABLE = "create table " + TABLE_EXPANSION + " ("
        			+ KEY_EXPANSION_ID + " integer primary key autoincrement, "
        			+ KEY_NAME + " text not null unique);\n";
        	
        	final String CREATE_FACTION_TABLE = "create table " + TABLE_FACTION + " ("
        			+ KEY_FACTION_ID + " integer primary key autoincrement, "
        			+ KEY_NAME + " text not null unique);\n";
        	
        	final String CREATE_BUILD_ORDER_TABLE = "create table " + TABLE_BUILD_ORDER + " ("
        			+ KEY_BUILD_ORDER_ID + " integer primary key autoincrement, "
        	        + KEY_NAME + " text not null unique, "
        	        + KEY_EXPANSION_ID + " integer not null, "
        	        + KEY_FACTION_ID + " integer not null, "
        	        + KEY_VS_FACTION_ID + " integer, "		// null means vs. any race
        	        + KEY_SOURCE + " text, "
        	        + KEY_DESCRIPTION +" text, "
        	        + KEY_AUTHOR + " text, "
        	        + KEY_CREATED + " text, "	// ISO 8601 string (YYYY-MM-DD HH:MM:SS)
        	        + KEY_MODIFIED + " text, "	// ISO 8601 string (YYYY-MM-DD HH:MM:SS)
        	        + "foreign key(" + KEY_EXPANSION_ID + ") references " + TABLE_EXPANSION + " (" + KEY_EXPANSION_ID + ") on delete cascade, "
        	        + "foreign key(" + KEY_FACTION_ID + ") references " + TABLE_FACTION + " (" + KEY_FACTION_ID + ") on delete cascade, "
        	        + "foreign key(" + KEY_VS_FACTION_ID + ") references " + TABLE_FACTION + " (" + KEY_FACTION_ID + ") on delete cascade);\n";
        	
        	final String CREATE_ITEM_TYPE_TABLE = "create table " + TABLE_ITEM_TYPE + " ("
        			+ KEY_ITEM_TYPE_ID + " integer primary key autoincrement, "
        			+ KEY_NAME + " text not null unique);\n";
        	
        	final String CREATE_ITEM_TABLE = "create table " + TABLE_ITEM + " ("
        			+ KEY_ITEM_ID + " integer primary key autoincrement, "
        			+ KEY_ITEM_TYPE_ID + " integer not null, "
        			+ KEY_NAME + " text not null unique, "
        			+ KEY_FACTION_ID + " integer, "			// null = can be used in all faction's build orders (e.g. note)
        			+ "foreign key(" + KEY_FACTION_ID + ") references " + TABLE_FACTION + " (" + KEY_FACTION_ID + ") on delete cascade, "
        			+ "foreign key(" + KEY_ITEM_TYPE_ID + ") references " + TABLE_ITEM_TYPE + " (" + KEY_ITEM_TYPE_ID + ") on delete cascade);\n";
        	
        	final String CREATE_BUILD_ORDER_ITEM_TABLE = "create table " + TABLE_BUILD_ORDER_ITEM + " ("
        			+ KEY_BUILD_ORDER_ID + " integer, "
        			+ KEY_ITEM_ID + " integer not null, "
        			+ KEY_ORDER + " integer not null, "
        			+ KEY_SECONDS + " integer not null, "
        			+ KEY_TEXT + " text, "
        			+ KEY_VOICE + " text, "
        			+ KEY_TARGET + " text, "
        			+ KEY_COUNT + " integer not null, "
        			+ "foreign key(" + KEY_BUILD_ORDER_ID + ") references " + TABLE_BUILD_ORDER + " (" + KEY_BUILD_ORDER_ID + ") on delete cascade, "
        			+ "foreign key(" + KEY_ITEM_ID + ") references " + TABLE_ITEM + " (" + KEY_ITEM_ID + ") on delete cascade, "
        			+ "primary key(" + KEY_BUILD_ORDER_ID + ", " + KEY_ORDER + "));\n";
        			    	
        	final String[] ALL_CREATE_STATEMENTS = {
        			CREATE_EXPANSION_TABLE,
        			CREATE_FACTION_TABLE,
        			CREATE_BUILD_ORDER_TABLE,
        			CREATE_ITEM_TYPE_TABLE,
        			CREATE_ITEM_TABLE,
        			CREATE_BUILD_ORDER_ITEM_TABLE
        			// ..
        	};
             	
        	for (String sql : ALL_CREATE_STATEMENTS)
        		db.execSQL(sql);
            
        	mNeedsStaticDataAdded = true;
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion);
            
            if (oldVersion <= 46) {
            	// 48: added date created and date modified to Build table
            	db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT", TABLE_BUILD_ORDER, KEY_CREATED));
            	db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT", TABLE_BUILD_ORDER, KEY_MODIFIED));
            	
            	// 47: moved verbs from DB to strings.xml, verb table no longer needed
            	db.execSQL("DROP TABLE IF EXISTS " + TABLE_FACTION_ITEM_TYPE_VERB);
            	
//            	if (oldVersion == 43 || oldVersion == 44 || oldVersion == 45) {

            	// 44: enduring locusts upgrade added to items, need to reload items from JSON file
            	// 45: warpgate structure and transform ability added
            	// 46: removed anabolic synthesis, renamed air attacks to flyer attacks
            	loadItemDefinitions(mContext, db, false);
            	
//            	}
            } else {
	            for (String table : ALL_TABLES)
	            	db.execSQL("DROP TABLE IF EXISTS " + table);
	            onCreate(db);
            }
        }
        
        /** Adds static data like expansion and race names. This only needs to be called
         * once, after the database is first created. */
        public void addStaticData(SQLiteDatabase db) {
        	Log.w(DbAdapter.TAG, "addStaticData() called");
        	
        	ContentValues initialValues = new ContentValues();
        	
        	db.beginTransaction();
        	
        	String[] expansions = { "Wings of Liberty", "Heart of the Swarm" };
        	for (String expansion : expansions) {
        		initialValues.clear();
        		initialValues.put(KEY_NAME, expansion);
                db.insert(TABLE_EXPANSION, null, initialValues);	
        	}
            
        	String[] factions = { "Terran", "Zerg", "Protoss" };
        	for (String faction : factions) {
        		initialValues.clear();
        		initialValues.put(KEY_NAME, faction);
                db.insert(TABLE_FACTION, null, initialValues);	
        	}
        	
        	String[] item_types = { "unit", "structure", "upgrade", "ability", "note" };
        	for (String type : item_types) {
        		initialValues.clear();
        		initialValues.put(KEY_NAME, type);
                db.insert(TABLE_ITEM_TYPE, null, initialValues);	
        	}
        	        	
        	loadItemDefinitions(mContext, db, false);
        	
        	db.setTransactionSuccessful();
        	db.endTransaction();
        	
        	// should value since there are no expansion or faction records yet
        	//db.execSQL("INSERT INTO " + TABLE_BUILD_ORDER + " (name, expansion_id, faction_id) VALUES ('test build order', 0, 0);");
        }
        
        public boolean needsStaticDataAdded() {
        	return mNeedsStaticDataAdded;
        }
        
        /** tells this helper that static data has now been added to the DB */
        public void staticDataAdded() {
        	mNeedsStaticDataAdded = false;
        }
        
        // Helpers
        
        /* used only for getting item definitions from JSON files via GSON
         * Represents a "game item", like a marine, scv, etc. */
        private class Item {
        	public String mName;
        	public ItemType mType;
        	public Faction mFaction;
        	
        	public String toString() {
        		return mName + " " + mType + " " + mFaction;
        	}
        }
        
        /**
         * Attempts to load item definitions into Item table from the JSON assets file
         * 
         * @param c context
         * @param db database
         * @param overwrite If true, any conflicts will be resolved by overwriting old item
         * with new item from JSON file
         */
        private static void loadItemDefinitions(Context c, SQLiteDatabase db, boolean overwrite) {
        	ContentValues initialValues = new ContentValues();
        	
        	try {
        		// TODO externalize this filename string
	        	ArrayList<Item> items = readItems(new InputStreamReader(c.getAssets().open("items.json")));

	        	for (Item item : items) {
	        		if (!overwrite) {
	        			// skip if an item with the same name is already in the database
	        			if (DbAdapter.getItemID(item.mName, db) != -1)
	        				continue;
	        			
		        		initialValues.clear();
		        		initialValues.put(KEY_NAME, item.mName);
		        		initialValues.put(KEY_ITEM_TYPE_ID, DbAdapter.getItemTypeID(item.mType));
		        		
		        		// faction can be null for build items e.g. the "note" item
		        		if (item.mFaction != null)
		        			initialValues.put(KEY_FACTION_ID, DbAdapter.getFactionID(item.mFaction));
		        		
		                final long result = db.insert(TABLE_ITEM, null, initialValues);
//		                if (result != -1) {
//		                	Timber.d(TAG, "Inserted new item " + item + " into items table");
//		                }
	        		} else {
	        			// TODO Not yet implemented
	        		}
	        	}
        	} catch (IOException e) {
        		Log.e(TAG, e.toString(), e);
        	}
        }
        
        private static ArrayList<Item> readItems(Reader in) {
    		Gson gson = new Gson();
    		Type t = new TypeToken<ArrayList<Item>>() {}.getType();
    		JsonReader reader;
			reader = new JsonReader(in);
			ArrayList<Item> result = gson.fromJson(reader, t);
			return result;
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public DbAdapter(Context context) {
        this.mContext = context;
    }

    /**
     * Open the build order database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public synchronized DbAdapter open() throws SQLException {
    	if (!isOpen) {
//    		Timber.d(TAG, "DbAdapter.open() called, getting new writable database...");
	        mDbHelper = new DatabaseHelper(mContext);
	        mDb = mDbHelper.getWritableDatabase();
	        
	    	if (!mDb.isReadOnly()) {
	    	    // Enable foreign key constraints
	    	    mDb.execSQL("PRAGMA foreign_keys=ON;");
	    	}
	        
	    	// check if database was just created and needs static data
	    	if (mDbHelper.needsStaticDataAdded()) {
	    		mDbHelper.addStaticData(mDb);
	    		mDbHelper.staticDataAdded();
	    	}
	    	isOpen = true;
    	}
//        Log.w(TAG, "mDb = " + mDb);
        return this;
    }

    public void close() {
        mDbHelper.close();
        isOpen = false;
    }
    
    // temporary?
    public SQLiteDatabase getWritableDatabase() {
    	open();
    	return mDb;
    }
    
    /**
     * USE WITH CAUTION!
     * Deletes all user data from the database. This includes build orders and build order items only,
     * static data such as items, expansions and factions are left intact. 
     */
    public void clear() {
    	mDb.delete(TABLE_BUILD_ORDER_ITEM, null, null);
    	mDb.delete(TABLE_BUILD_ORDER, null, null);
    }
    
    /**
     * DB access helpers from here on
     */
    
    /**
     * Returns the number of unique build orders currently in the database
     */
    public int getBuildCount() {
    	Cursor cursor = null;
    	int result = 0;
    	
    	try {
    		cursor = mDb.rawQuery("select Count(*) from " + TABLE_BUILD_ORDER, null);
    		if (cursor != null) {
        		cursor.moveToFirst();
        		result = cursor.getInt(0);	// should only be one column
        	} else {
        		throw new RuntimeException("Error querying size of build table");
        	}
    	} finally {
    		if (cursor != null)
    			cursor.close();
    	}
    	
    	return result;
    }
    
    public void addBuilds(ArrayList<Build> builds) {
    	mDb.beginTransaction();
    	try {
    		for (Build build : builds) {
				addBuild(build);
			}
			mDb.setTransactionSuccessful();
    	} catch (RuntimeException e) {
    		throw e;
    	} finally {
    		mDb.endTransaction();
    	}
    }
    
    /**
     * Adds builds to the database. Existing builds with the same name
     * as any of the incoming builds will be overwritten.
     * 
     * @param builds
     * @param client optional listener to get progress updates
     */
	public void addOrReplaceBuilds(ArrayList<Build> builds, ProgressListener client) {
    	mDb.beginTransaction();
    	try {
    		
    		int i=0;
    		final int size = builds.size();
    		for (Build build : builds) {
    			long id = getBuildID(build.getName());
//    			Timber.d(this.toString(), "deciding whether to add/replace " + build.getName() + ", id = " + id);
    			if (id == -1) {
    				addBuild(build);
    			} else {
    				addOrReplaceBuild(build, id);
    			}
    			i++;
    			if (client != null)
    				client.onProgressUpdate((int)((i/(float)size)*100));
    		}
    		
			mDb.setTransactionSuccessful();
    	} catch (RuntimeException e) {
    		throw e;
    	} finally {
    		mDb.endTransaction();
    	}
	}
    
    /**
     * Adds builds to the database. Existing builds with the same name
     * as any of the incoming builds will be overwritten.
     * 
     * @param builds
     */
	public void addOrReplaceBuilds(ArrayList<Build> builds) {
		addOrReplaceBuilds(builds, null);
	}
	
    /**
     * Add a build to the database
     * 
     * @param build
     */
    public void addBuild(Build build) throws NameNotUniqueException {
    	addOrReplaceBuild(build, -1);
    }
    
    /**
     * Adds or replaces a build
     * 
     * @param build
     * @param existingRowId row id of build to replace, or -1 to add a new build
     */
    public void addOrReplaceBuild(Build build, long existingRowId) throws NameNotUniqueException {
//    	Timber.d(TAG, "addBuild() called, build hash = " + build.longHashCode());
//    	Timber.d(this.toString(), "addOrReplaceBuild() called with existing row id = " + existingRowId);
    	
    	ContentValues values = new ContentValues();
		values.put(KEY_NAME, build.getName());
		if (build.getFaction() == null)
			throw new RuntimeException("faction for build \"" + build + "\" is null");

		values.put(KEY_FACTION_ID, getFactionID(build.getFaction()));
		values.put(KEY_EXPANSION_ID, getExpansionID(build.getExpansion()));
		values.put(KEY_SOURCE, build.getSource());
		values.put(KEY_AUTHOR, build.getAuthor());
		values.put(KEY_DESCRIPTION, build.getNotes());
		if (build.getVsFaction() != null)
			values.put(KEY_VS_FACTION_ID, getFactionID(build.getVsFaction()));
		if (build.getCreated() != null)
			values.put(KEY_CREATED, DATE_FORMAT.format(build.getCreated()));
		if (build.getModified() != null)
			values.put(KEY_MODIFIED, DATE_FORMAT.format(build.getModified()));
		
		try {
			if (existingRowId == -1) {		// create new Build
				long row_id = mDb.insert(TABLE_BUILD_ORDER, null, values);
				
		        if (row_id != -1) {
		        	// if it has no build items, we're done
		        	if (build.getItems() != null)
		        		addBuildItems(row_id, build.getItems());
		        } else {
		        	throw new NameNotUniqueException("Couldn't add new BO " + build.toString() + ", name already in use");
		        }
			} else {	// overwrite existing Build data
				if (mDb.update(TABLE_BUILD_ORDER, values, KEY_BUILD_ORDER_ID + " = " + existingRowId, null) == 0) {
					throw new RuntimeException("addOrReplaceBuild() didn't update any rows - existing build ID \"" + existingRowId + "\" was invalid");
				}
				
				// drop any existing build items and add our new ones
				int dropped = dropBuildItems(existingRowId);
	//			Timber.d(this.toString(), "overwriting existing build data, num dropped rows = " + dropped);
				addBuildItems(existingRowId, build.getItems());
			}
		} catch (RuntimeException e) {
			throw new RuntimeException("Problem adding build items to build name \"" + build.getName() +
					"\" = " + e.getMessage());
		}
    }
    
    /** Convenience function - attempts to drop all build items for build order with given ID */
    public int dropBuildItems(long build_row_id) {
    	return mDb.delete(TABLE_BUILD_ORDER_ITEM, KEY_BUILD_ORDER_ID + " = " + build_row_id, null);
    }
    
    public void addBuildItems(long build_row_id, ArrayList<BuildItem> items) throws RuntimeException {
    	if (items == null)
    		return;
    	
    	ContentValues values = new ContentValues();

    	// assumes items are already ordered
    	for (int i = 0; i < items.size(); i++) {
    		BuildItem item = items.get(i);
    		
    		// try to get item ID in the database from it's unique name (e.g. "marine")
    		long itemID = getItemID(item.getGameItemID());
    		if (itemID == -1) {
    			throw new RuntimeException("couldn't insert build order item: " + item.getGameItemID() + " doesn't exist in DB");
    		}
    		
    		values.put(KEY_BUILD_ORDER_ID, build_row_id);
    		values.put(KEY_ITEM_ID, itemID);
    		values.put(KEY_SECONDS, item.getTime());
    		values.put(KEY_ORDER, i);
    		values.put(KEY_COUNT, item.getCount());
    		values.put(KEY_TEXT, item.getText());
    		values.put(KEY_TARGET, item.getTarget());
    		values.put(KEY_VOICE, item.getVoice());
    		
    		if (mDb.insert(TABLE_BUILD_ORDER_ITEM, null, values) == -1)
    			throw new RuntimeException("couldn't insert build order item for BO " + build_row_id);
    	}
    }
        
    /**
     * Deletes build with given ID from the database. Consider using ContentResolver instead
     * as it handles notifying observers.
     * 
     * @param row_id
     * @return true if build was successfully deleted, false if not. This will usually
     * 				mean there was no build with the given ID.
     */
    public boolean deleteBuild(long row_id) {
    	return (mDb.delete(TABLE_BUILD_ORDER, KEY_BUILD_ORDER_ID + " = " + row_id, null) != 0);
    }
    
    /**
     * Deletes build with given name from the database. Consider using ContentResolver instead
     * as it handles notifying observers.
     * 
     * @param name unique human-readable name given to build (e.g. "1 Rax Reaper FE")
     * @return true if build was successfully deleted, false if not. This will usually
     * 				mean there was no build with the given ID.
     */
    public boolean deleteBuild(String name){
    	return (mDb.delete(TABLE_BUILD_ORDER, KEY_NAME + " = ?", new String[]{name}) != 0);
    }
    
    /**
     * Returns build object encapsulating all data for a build order from
     * the database
     * 
     * @param row_id	ID of build order to return
     * @return Build object or null if row_id doesn't exist
     */
    public Build fetchBuild(long row_id) {
    	Cursor result = null;
    	Build newBuild = null;
    	
    	try {
    		result = mDb.query(TABLE_BUILD_ORDER,
    			new String[] { KEY_NAME, KEY_FACTION_ID, KEY_EXPANSION_ID, KEY_VS_FACTION_ID, KEY_SOURCE, KEY_DESCRIPTION, KEY_AUTHOR, KEY_CREATED, KEY_MODIFIED },
    			KEY_BUILD_ORDER_ID + " = " + row_id, null, null, null, null);
	    	if (result.moveToFirst()) {
		    	final String name = result.getString(0);
		    	final Faction faction = Faction.values()[result.getInt(1)-1];
		    	final Expansion expansion = Expansion.values()[result.getInt(2)-1];
		    	final Faction vsFaction = result.isNull(3) ? null : Faction.values()[result.getInt(3)-1];
		    	final String source = result.getString(4);
		    	final String notes = result.getString(5);
		    	final String author = result.getString(6);
		    	final String createdString = result.getString(7);
		    	final String modifiedString = result.getString(8);
		    	    	
		    	newBuild = new Build(name, faction, vsFaction, expansion, source, notes, fetchBuildItems(row_id));
		    	newBuild.setAuthor(author);
		    	
		    	if (createdString != null) {
			    	try {
			    		newBuild.setCreated(DATE_FORMAT.parse(createdString));
			    	} catch (ParseException e) {
			    		// do nothing, creation time will be NULL
			    	}
		    	}
		    	
		    	if (modifiedString != null) {
			    	try {
			    		newBuild.setModified(DATE_FORMAT.parse(modifiedString));
			    	} catch (ParseException e) {
			    		// do nothing, last modified time will be NULL
			    	}
		    	}
	    	}
    	} finally {
    		if (result != null)
    			result.close();
    	}
    	
    	return newBuild;
    }
    
    /** Returns the items in a build order given by build order id. Returns null if there
     * are no items for the specified build */
    public ArrayList<BuildItem> fetchBuildItems(long build_order_id) {   	
    	final String where = KEY_BUILD_ORDER_ID + " = " + build_order_id +
    			" AND " + TABLE_BUILD_ORDER_ITEM + "." + KEY_ITEM_ID + " = " +
    			TABLE_ITEM + "." + KEY_ITEM_ID;
    	
    	final String orderBy = KEY_ORDER + " ASC";
    	
    	final String sql = String.format(Locale.US, "select %s, %s, %s, %s, %s, %s, %s from %s, %s where %s order by %s;",
    			TABLE_ITEM + "." + KEY_NAME, KEY_ORDER, KEY_SECONDS, KEY_TEXT, KEY_VOICE, KEY_TARGET, KEY_COUNT,
    			TABLE_BUILD_ORDER_ITEM, TABLE_ITEM,
    			where,
    			orderBy);
    	
    	Cursor result = null;
    	ArrayList<BuildItem> items = null;
    	
    	try {
    		result = mDb.rawQuery(sql, null);
        	items = new ArrayList<BuildItem>();
        	while (result.moveToNext()) {
    			BuildItem buildItem = new BuildItem(result.getInt(2), result.getString(0), result.getInt(6),
    					result.getString(5), result.getString(3), result.getString(4)); 
    			items.add(buildItem);
        	}
    	} finally {
    		if (result != null)
    			result.close();
    	}
    	
    	if (items != null && items.size() > 0)
    		return items;
    	else
    		return null;
    }
    
    /**
     * Returns a build's ID given its unique name. Returns -1
     * if no build with the given name exists in DB.
     * 
     * @param uniqueName
     * @return Build table row ID
     */
    public long getBuildID(String uniqueName) {
    	Cursor cursor = null;
    	
    	try {
    		cursor = mDb.query(TABLE_BUILD_ORDER,
        			new String[] { KEY_BUILD_ORDER_ID },
        			KEY_NAME + " = ?", 
        			new String[] { uniqueName }, null, null, null);
        	if (cursor.getCount() == 0)
        		return -1;
        	
        	cursor.moveToFirst();
        	return cursor.getLong(0);	
    	} finally {
    		if (cursor != null)
    			cursor.close();
    	}
    }
    
    /**
     * Returns a cursor over Item row IDs whose faction and item type match
     * the arguments given.
     * If NOTE is given as the typeFilter, the factionFilter will not apply.
     * 
     * @param factionFilter
     * @param typeFilter
     * @return
     */
    public Cursor fetchItemIDsMatching(Faction factionFilter, ItemType typeFilter) {
    	String select = KEY_ITEM_TYPE_ID + " = " + getItemTypeID(typeFilter);
    	if (typeFilter != ItemType.NOTE)		// "special" item category (should rename it)
    		select += " and " + KEY_FACTION_ID + " = " + getFactionID(factionFilter);
    	
    	final String orderBy = KEY_NAME + " ASC";
    	
    	Cursor result = mDb.query(TABLE_ITEM,
    			new String[] { KEY_ITEM_ID },
    			select, null, null, null, orderBy);

    	return result;
    }
    
	//=========================================================================
    // Build Item database helpers (previously in GameItemDatabase module)
	//=========================================================================

    /**
     * Returns the integer ID of an item given its unique name (e.g. "marine")
     * or -1 if it doesn't exist
     */
    public long getItemID(String itemName) {
    	return getItemID(itemName, mDb);
    }
    
    /**
     * Returns the integer ID of an item given its unique name (e.g. "marine")
     * or -1 if it doesn't exist
     */
    public static long getItemID(String itemName, SQLiteDatabase db) {
    	Cursor cursor = null;
    	
    	try {
    		cursor = db.query(TABLE_ITEM,
        			new String[] { KEY_ITEM_ID },
        			KEY_NAME + " = ?", 
        			new String[] { itemName }, null, null, null);
        	if (cursor.getCount() == 0)
        		return -1;
        	
        	cursor.moveToFirst();
        	return cursor.getLong(0);	
    	} finally {
    		if (cursor != null)
    			cursor.close();
    	}
    }
    
    /**
     * Gets the unique name of an item as it exists in the database given its row ID
     * or null if it doesn't exist
     * @param row_id
     * @return
     */
    public String getItemUniqueName(long row_id) {
    	Cursor cursor = null;
    	String name = null;
    	
    	try {
    		cursor = mDb.query(TABLE_ITEM,
    			new String[] { KEY_NAME },
    			KEY_ITEM_ID + " = ?", 
    			new String[] { ""+row_id }, null, null, null);
	    	if (cursor.getCount() > 0) {
		    	cursor.moveToFirst();
		    	name = cursor.getString(0);
	    	}
    	} finally {
    		if (cursor != null)
    			cursor.close();
    	}
    	return name;
    }
    
    /**
     * Returns an appropriate action verb for a game item.
     * E.g. "build" for a Terran structure, "evolve" for a Zerg upgrade, etc.
     * Verbs returned are dependent on locale.
     * 
     * @param item_name
     * @return verb string
     */
    public String getVerbForItem(String item_name) {
    	Faction faction = getItemFaction(item_name);
    	ItemType type = getItemType(item_name);
    	return getVerb(faction, type);
	}
	
    /**
     * Returns an appropriate action verb based on an item type and faction.
     * E.g. "build" for a Terran structure, "evolve" for a Zerg upgrade, etc.
     * Verbs returned are dependent on locale.
     * 
     * This method is robust in that if the exact verb for a faction and item type
     * isn't available, the next best match will be returned. If all else fails, "build"
     * will be returned.
     * 
     * @param item_name
     * @return verb string
     */
    public String getVerb(Faction faction, ItemType type) {
    	String verbStringId = "verb_" +
    			(type == null ? ItemType.STRUCTURE.toString().toLowerCase(Locale.US) : type.toString().toLowerCase(Locale.US)) + "_" +
    			(faction == null ? "generic" : faction.toString().toLowerCase(Locale.US));
    	    			
    	int verbResourceId = mContext.getResources().getIdentifier(verbStringId, "string", mContext.getPackageName());
    	
    	// fall-back to faction-independent verb
    	if (verbResourceId == 0) {
    		if (faction != null)
    			return getVerb(null, type);
    		else if (type != null)
    			return getVerb(null, null);
    		else
    			return "build";
    	} else {
    		return mContext.getString(verbResourceId);
    	}
    }
    
	/*
	 * Gets the human-readable string resource identifier for a unit, upgrade or ability
	 * Returns 0 if item not found
	 */
	public int getName(String item_name) {
		return mContext.getResources().getIdentifier("gameitem_" + item_name, "string", mContext.getPackageName());
	}
	
	/*
	 * Gets the localized, human-readable string for a unit, upgrade or ability
	 * Returns back the item_id if item not found
	 */
	public String getNameString(String item_name) {
		int res_id = this.getName(item_name);
		return (res_id != 0) ? mContext.getString(res_id) : item_name;
	}
	
	/*
	 * Gets the type of the given game item (unit, structure, upgrade, etc).
	 */
	public ItemType getItemType(String item_name) {
		if (item_name == null || item_name.matches(""))
			return null;
		
		// query the database for the item with given name's type
		Cursor cursor = null;
		ItemType result = null;
		
		try {
			cursor = mDb.query(TABLE_ITEM,
    			new String[] { KEY_ITEM_TYPE_ID },
    			KEY_NAME + " = ?", 
    			new String[] { item_name }, null, null, null);
		
			if (cursor.getCount() == 0)
				throw new RuntimeException("couldn't get the type of item with name " + item_name + " as it doesn't exist in DB");
			
			// convert the type from a row ID to an ItemType enum value
			cursor.moveToFirst();
			result = getItemType(cursor.getLong(0));
		} finally {
    		if (cursor != null)
    			cursor.close();
    	}
		return result;
	}
	
	/**
	 * Returns the Faction to which the given game item belongs
	 * 
	 * @param item_name unique name of a game item
	 * @return item's faction
	 */
	public Faction getItemFaction(String item_name) {
		// query the database for the item with given name's type
		Cursor cursor = null;
		Faction result = null;
		
		try {
			cursor = mDb.query(TABLE_ITEM,
    			new String[] { KEY_FACTION_ID },
    			KEY_NAME + " = ?", 
    			new String[] { item_name }, null, null, null);
		
			if (cursor.getCount() == 0)
				throw new RuntimeException("couldn't get the faction of item with name " + item_name + " as it doesn't exist in DB");
			
			// convert the type from a row ID to an ItemType enum value
			cursor.moveToFirst();
			result = getFaction(cursor.getLong(0));
		} finally {
    		if (cursor != null)
    			cursor.close();
    	}
		return result;
	}
	
	/*
	 * Gets the small icon drawable ID for a unit, upgrade or ability
	 * Returns an "unknown unit" placeholder icon if no suitable icon was found
	 */
	public int getSmallIcon(String item_name) {
		int result = mContext.getResources().getIdentifier("gameitem_" + item_name + "_small", "drawable", mContext.getPackageName());
		if (result == 0)
			return R.drawable.not_found;
		else
			return result;
	}
	
	/*
	 * Gets the large icon drawable ID for a unit, upgrade or ability
	 * Returns an "unknown unit" placeholder icon if no suitable icon was found
	 */
	public int getLargeIcon(String item_name) {
		int result = mContext.getResources().getIdentifier("gameitem_" + item_name + "_large", "drawable", mContext.getPackageName());
		if (result == 0)
			// fall back to small icon if no large one exists
			result = this.getSmallIcon(item_name);

		return result;
	}
    
	//=========================================================================
    
    /**
     * Returns the row ID of the given ItemType (e.g. unit, structure...)
     * @param item type as an enum value
     * @return row ID of item type in DB
     */
    public static long getItemTypeID(ItemType type) {
    	return sItemTypeToIdMap.get(type);
    }
    
    public static long getFactionID(Faction faction) {
    	return sFactionToIdMap.get(faction);
    }
    
    public static long getExpansionID(Expansion expansion) {
    	return sExpansionToIdMap.get(expansion);
    }
    
    /** returns an enum given a row id in the database */
    public static ItemType getItemType(long item_type_row_id) {
    	return sIdToItemTypeMap.get(item_type_row_id);
    }
    
    public static Faction getFaction(long faction_row_id) {
    	return sIdToFactionMap.get(faction_row_id);
    }
    
    public static Expansion getExpansion(long expansion_row_id) {
    	return sIdToExpansionMap.get(expansion_row_id);
    }
    
	/** returns a string resource ID */
    public static int getItemTypeName(long item_type_id) {
    	return DbAdapter.getItemTypeName(getItemType(item_type_id));
    }
    
    public static int getFactionName(long faction_row_id) {
    	return DbAdapter.getFactionName(getFaction(faction_row_id));
    }
    
    public static int getExpansionName(long expansion_row_id) {
    	return DbAdapter.getExpansionName(getExpansion(expansion_row_id));
    }
    
//    /**
//     * Adds a new item definition. Items are the types of elements that could appear in a build order
//     * list, e.g. marine, stim_pack, hatchery, chrono_boost...
//     * 
//     * @param itemName unique name for this item e.g. "marine"
//     * @param itemType whether this item is a unit, structure, upgrade, ability
//     */
//    public void addItem(String itemName, ItemType itemType) {
//    	ContentValues values = new ContentValues();
//		values.put(KEY_NAME, itemName);
//		values.put(KEY_ITEM_TYPE_ID, getItemTypeID(itemType));
//		if (mDb.insert(TABLE_ITEM, null, values) == -1)
//			throw new RuntimeException("couldn't insert item with name " + itemName);
//    }
}
